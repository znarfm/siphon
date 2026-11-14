package com.znarfm.siphon.core.dash

import android.content.Context
import android.net.Uri
import com.znarfm.siphon.core.hls.HlsRemuxer
import com.znarfm.siphon.core.network.HeadersCodec
import com.znarfm.siphon.core.network.HttpStatusException
import com.znarfm.siphon.core.network.NetworkClients
import com.znarfm.siphon.core.network.TokenBucket
import com.znarfm.siphon.core.storage.DownloadStorageProvider
import com.znarfm.siphon.data.db.DownloadDao
import com.znarfm.siphon.data.db.DownloadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class DashQueueEngine(
    private val context: Context,
    private val dao: DownloadDao,
    private val client: OkHttpClient = NetworkClients.defaultClient,
    private val maxParallelSegments: Int = 4,
    rateBytesPerSec: Long = 0L
) {
    val rateLimiter = TokenBucket(rateBytesPerSec)

    fun updateRateLimit(bytesPerSecond: Long) {
        rateLimiter.updateRate(bytesPerSecond)
    }

    suspend fun executeWithStorage(
        downloadId: Long,
        storage: DownloadStorageProvider,
        customTreeUri: Uri? = null,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): DownloadState = withContext(Dispatchers.IO) {
        val download = dao.getDownload(downloadId)
            ?: return@withContext DownloadState.FAILED

        val isAudioOnly = download.mimeType.startsWith("audio/") ||
            download.displayName.endsWith(".m4a", ignoreCase = true)
        val targetMime = if (isAudioOnly) "audio/mp4" else "video/mp4"
        val targetName = when {
            isAudioOnly && !download.displayName.endsWith(".m4a", ignoreCase = true) ->
                "${download.displayName.substringBeforeLast('.')}.m4a"
            !isAudioOnly && !download.displayName.endsWith(".mp4", ignoreCase = true) ->
                "${download.displayName.substringBeforeLast('.')}.mp4"
            else -> download.displayName
        }

        val destUri = if (!download.destinationUri.isNullOrBlank()) {
            Uri.parse(download.destinationUri).also { storage.refreshPendingExpiry(it) }
        } else {
            storage.createDestination(
                displayName = targetName,
                mimeType = targetMime,
                customTreeUri = customTreeUri
            ).also { created ->
                dao.updateDestinationUri(downloadId, created.toString())
            }
        }

        dao.updateDownloadState(downloadId, DownloadState.ACTIVE)

        val workDir = File(context.cacheDir, "dash_$downloadId").apply { mkdirs() }
        val headers = HeadersCodec.decode(download.headersJson)

        try {
            val mpdUrl = HeadersCodec.decodeMeta(download.headersJson, HeadersCodec.META_DASH_MPD_URL) ?: download.url
            val requestedRepId = HeadersCodec.decodeMeta(download.headersJson, HeadersCodec.META_DASH_REP_ID)

            val mpdXml = fetchText(mpdUrl, headers)
            val parsed = DashManifestParser.parse(mpdUrl, mpdXml)

            val videoTrack = parsed.videoTracks
                .firstOrNull { !it.isDrmProtected && (requestedRepId == null || it.id == requestedRepId) }
                ?: parsed.videoTracks.firstOrNull { !it.isDrmProtected }

            val audioTrack = parsed.audioTracks.firstOrNull { !it.isDrmProtected }

            val selectedTracks = listOfNotNull(videoTrack, audioTrack)
            if (selectedTracks.isEmpty()) {
                throw IOException(
                    if (parsed.hasDrm) "DASH stream is DRM-protected and cannot be downloaded"
                    else "No downloadable tracks found in DASH manifest"
                )
            }

            val segmentsByTrack = mutableMapOf<Int, List<DashSegment>>()
            selectedTracks.forEachIndexed { idx, track ->
                val segs = if (track.isSingleFile && !track.singleFileUrl.isNullOrBlank()) {
                    listOf(DashSegment(track.singleFileUrl))
                } else {
                    track.segments
                }
                if (segs.isNotEmpty()) {
                    segmentsByTrack[idx] = segs
                }
            }

            if (segmentsByTrack.isEmpty()) {
                throw IOException("No segments found in DASH tracks")
            }

            val totalSegments = segmentsByTrack.values.sumOf { it.size }
            var completedSegments = 0
            var cumulativeBytes = 0L

            for ((trackIdx, segs) in segmentsByTrack) {
                downloadTrackSegments(
                    trackIndex = trackIdx,
                    segments = segs,
                    headers = headers,
                    workDir = workDir,
                    onSegmentDone = { segBytes ->
                        completedSegments++
                        cumulativeBytes += segBytes
                        val estTotal = if (completedSegments > 0 && totalSegments > 0) {
                            val proj = (cumulativeBytes.toDouble() / completedSegments * totalSegments).toLong()
                            maxOf(download.totalBytes, proj)
                        } else {
                            download.totalBytes
                        }
                        if (estTotal > 0L) {
                            dao.updateTotalBytes(downloadId, estTotal)
                        }
                        onProgress?.invoke(cumulativeBytes, estTotal)
                    }
                )
            }

            dao.updateDownloadState(downloadId, DownloadState.MERGING)
            onProgress?.invoke(0L, -1L)

            val trackFiles = segmentsByTrack.keys.sorted().map { File(workDir, "track_$it.tmp") }
            val remuxOutput = File(workDir, "remux_output.mp4")
            val finalSource = if (trackFiles.size > 1 && HlsRemuxer.remuxToMp4(trackFiles, remuxOutput) && remuxOutput.length() > 0) {
                trackFiles.forEach { it.delete() }
                remuxOutput
            } else {
                trackFiles.firstOrNull() ?: throw IOException("No DASH track output produced")
            }

            val totalBytes = finalSource.length()
            storage.openWriteSession(destUri, totalBytes = totalBytes).use { session ->
                val destChannel = session.channel
                destChannel.truncate(0)
                destChannel.position(0)
                var copiedBytes = 0L
                FileInputStream(finalSource).channel.use { srcChannel ->
                    val buffer = ByteBuffer.allocateDirect(128 * 1024)
                    while (srcChannel.read(buffer) > 0) {
                        buffer.flip()
                        copiedBytes += destChannel.write(buffer)
                        buffer.clear()
                        onProgress?.invoke(copiedBytes, totalBytes)
                    }
                }
                destChannel.force(true)
            }

            storage.finalizePending(destUri)
            dao.updateTotalBytes(downloadId, totalBytes)
            dao.markTerminalAndPurgeCredentials(downloadId, DownloadState.COMPLETED)
            workDir.deleteRecursively()
            DownloadState.COMPLETED
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                dao.updateDownloadState(downloadId, DownloadState.PAUSED)
            }
            DownloadState.PAUSED
        } catch (e: Exception) {
            android.util.Log.e("DashQueueEngine", "DASH download failed for id $downloadId: ${e.message}", e)
            val httpEx = findHttpStatusException(e)
            val targetState = if (httpEx?.code == 403 || httpEx?.code == 410) {
                DownloadState.AUTH_EXPIRED
            } else {
                DownloadState.FAILED
            }
            withContext(NonCancellable) {
                dao.updateDownloadState(downloadId, targetState)
            }
            targetState
        }
    }

    private suspend fun downloadTrackSegments(
        trackIndex: Int,
        segments: List<DashSegment>,
        headers: Headers,
        workDir: File,
        onSegmentDone: suspend (Long) -> Unit
    ) {
        val trackFile = File(workDir, "track_${trackIndex}.tmp")
        val queue = Channel<Pair<Int, DashSegment>>(Channel.UNLIMITED)
        segments.forEachIndexed { idx, seg -> queue.trySend(idx to seg) }
        queue.close()

        val mergeMutex = Mutex()
        var nextToMerge = 0
        val completed = mutableMapOf<Int, File>()

        coroutineScope {
            repeat(maxParallelSegments.coerceAtMost(segments.size)) {
                launch {
                    for ((idx, seg) in queue) {
                        if (!isActive) break
                        val segFile = File(workDir, "seg_${trackIndex}_$idx.part")
                        if (!segFile.exists() || segFile.length() == 0L) {
                            downloadSegment(seg.url, headers, segFile)
                        }
                        val segSize = segFile.length()
                        onSegmentDone(segSize)

                        mergeMutex.withLock {
                            completed[idx] = segFile
                            while (completed.containsKey(nextToMerge)) {
                                val f = completed.remove(nextToMerge)!!
                                FileOutputStream(trackFile, true).use { out ->
                                    FileInputStream(f).use { input ->
                                        input.copyTo(out)
                                    }
                                }
                                f.delete()
                                nextToMerge++
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun downloadSegment(url: String, headers: Headers, target: File) {
        val req = Request.Builder().url(url).headers(headers).get().build()
        client.newCall(req).execute().use { resp ->
            if (resp.code == 403 || resp.code == 410) {
                throw HttpStatusException(resp.code, "Segment HTTP ${resp.code}")
            }
            if (!resp.isSuccessful) {
                throw IOException("Segment HTTP ${resp.code}")
            }
            val body = resp.body ?: throw IOException("Empty segment body")
            FileOutputStream(target).use { out ->
                val buf = ByteArray(32 * 1024)
                val stream = body.byteStream()
                while (true) {
                    val read = stream.read(buf)
                    if (read == -1) break
                    rateLimiter.consume(read)
                    out.write(buf, 0, read)
                }
            }
        }
    }

    private fun fetchText(url: String, headers: Headers): String {
        val req = Request.Builder().url(url).headers(headers).get().build()
        return client.newCall(req).execute().use { resp ->
            if (resp.code == 403 || resp.code == 410) {
                throw HttpStatusException(resp.code, "Manifest HTTP ${resp.code}")
            }
            if (!resp.isSuccessful) throw IOException("Manifest HTTP ${resp.code}")
            resp.body?.string() ?: throw IOException("Empty manifest body")
        }
    }

    suspend fun cancelAndDelete(downloadId: Long, storage: DownloadStorageProvider) {
        withContext(Dispatchers.IO + NonCancellable) {
            val entity = dao.getDownload(downloadId)
            File(context.cacheDir, "dash_$downloadId").deleteRecursively()
            if (entity?.destinationUri != null) {
                storage.delete(entity.destinationUri)
            }
            dao.deleteChunksForDownload(downloadId)
            dao.markTerminalAndPurgeCredentials(downloadId, DownloadState.CANCELLED)
        }
    }

    private fun findHttpStatusException(e: Throwable?): HttpStatusException? {
        var cur = e
        while (cur != null) {
            if (cur is HttpStatusException) return cur
            cur = cur.cause
        }
        return null
    }
}
