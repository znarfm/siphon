package com.siphon.app.core.hls

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.siphon.app.core.network.HeadersCodec
import com.siphon.app.core.network.HttpStatusException
import com.siphon.app.core.network.NetworkClients
import com.siphon.app.core.storage.DownloadStorageProvider
import com.siphon.app.data.db.ChunkEntity
import com.siphon.app.data.db.ChunkState
import com.siphon.app.data.db.DownloadDao
import com.siphon.app.data.db.DownloadEntity
import com.siphon.app.data.db.DownloadState
import com.siphon.app.data.db.DownloadType
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
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class HlsQueueEngine(
    private val context: Context,
    private val dao: DownloadDao,
    private val client: OkHttpClient = NetworkClients.defaultClient,
    private val maxParallelSegments: Int = 4,
    rateBytesPerSec: Long = 0L
) {

    private val keyCache = ConcurrentHashMap<String, ByteArray>()
    val rateLimiter = com.siphon.app.core.network.TokenBucket(rateBytesPerSec)

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

        val isSubDownload = download.mimeType.startsWith("text/") ||
            download.displayName.endsWith(".vtt", ignoreCase = true) ||
            download.displayName.endsWith(".srt", ignoreCase = true)

        val isAudioDownload = download.mimeType.startsWith("audio/") ||
            download.displayName.endsWith(".mp3", ignoreCase = true) ||
            download.displayName.endsWith(".m4a", ignoreCase = true) ||
            download.displayName.endsWith(".aac", ignoreCase = true)

        val targetMime = when {
            isSubDownload -> download.mimeType.ifBlank { "text/vtt" }
            isAudioDownload -> if (download.displayName.endsWith(".mp3", ignoreCase = true)) "audio/mpeg" else "audio/mp4"
            download.type == DownloadType.HLS || download.displayName.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            else -> download.mimeType
        }
        val targetName = when {
            isAudioDownload -> {
                if (!download.displayName.endsWith(".m4a", ignoreCase = true) && !download.displayName.endsWith(".mp3", ignoreCase = true)) {
                    "${download.displayName.substringBeforeLast('.')}.m4a"
                } else download.displayName
            }
            targetMime == "video/mp4" && !download.displayName.endsWith(".mp4", ignoreCase = true) -> {
                "${download.displayName.substringBeforeLast('.')}.mp4"
            }
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

        val workDir = File(context.cacheDir, "hls_$downloadId").apply { mkdirs() }
        val headers = HeadersCodec.decode(download.headersJson)

        try {
            // 1. Ensure chunks are populated from playlist
            val existingChunks = dao.getAllChunks(downloadId)
            val segmentsByTrack: Map<Int, List<ParsedHlsSegment>> = if (existingChunks.isEmpty()) {
                val parsed = parseAndPopulateTracks(download, headers, workDir)
                val allChunks = parsed.values.flatten().map { seg ->
                    val start = seg.byteRangeOffset ?: 0L
                    val end = if (seg.byteRangeLength != null && seg.byteRangeLength > 0L) {
                        start + seg.byteRangeLength - 1L
                    } else 0L
                    ChunkEntity(
                        downloadId = downloadId,
                        trackIndex = seg.trackIndex,
                        chunkIndex = seg.chunkIndex,
                        startOffset = start,
                        currentOffset = start,
                        endOffset = end,
                        state = ChunkState.PENDING
                    )
                }
                dao.insertChunks(allChunks)
                parsed
            } else {
                recoverSegments(download, headers, workDir)
            }

            val totalSegmentsCount = segmentsByTrack.values.sumOf { it.size }
            var currentMergedCount = 0

            // 2. Process each track with sliding-window assembly
            for ((trackIndex, trackSegments) in segmentsByTrack) {
                downloadAndAssembleTrack(
                    downloadId = downloadId,
                    trackIndex = trackIndex,
                    segments = trackSegments,
                    headers = headers,
                    workDir = workDir,
                    onSegmentMerged = { bytesMerged ->
                        currentMergedCount++
                        val estTotal = if (currentMergedCount > 0 && totalSegmentsCount > 0) {
                            val projected = (bytesMerged.toDouble() / currentMergedCount * totalSegmentsCount).toLong()
                            if (download.totalBytes > 0L) {
                                maxOf(download.totalBytes, projected)
                            } else {
                                projected
                            }
                        } else {
                            download.totalBytes
                        }
                        if (estTotal > 0L) {
                            dao.updateTotalBytes(downloadId, estTotal)
                        }
                        onProgress?.invoke(bytesMerged, estTotal)
                    }
                )
            }

            // 3. Remux track files into final destination (or keep raw concatenated track for subtitle playlists)
            dao.updateDownloadState(downloadId, DownloadState.MERGING)
            onProgress?.invoke(0L, -1L)

            val trackFiles = segmentsByTrack.keys.sorted().map { trackIdx ->
                File(workDir, "track_${trackIdx}.tmp")
            }

            val singleFile = trackFiles.firstOrNull()
            val alreadyMp4 = trackFiles.size == 1 && singleFile != null && isAlreadyMp4(singleFile)

            val remuxOutputFile = File(workDir, "remux_output.mp4")
            val remuxSuccess = if (isSubDownload || alreadyMp4) false else remuxToMp4(trackFiles, remuxOutputFile)

            val finalSourceFile = if (remuxSuccess && remuxOutputFile.exists() && remuxOutputFile.length() > 0) {
                trackFiles.forEach { it.delete() }
                remuxOutputFile
            } else {
                // Fallback to copying raw track 0 (.ts or .vtt or already MP4)
                singleFile ?: throw IOException("No track files produced")
            }

            val totalBytes = finalSourceFile.length()
            storage.openWriteSession(destUri, totalBytes = totalBytes).use { session ->
                val destChannel = session.channel
                destChannel.truncate(0)
                destChannel.position(0)
                var copiedBytes = 0L
                FileInputStream(finalSourceFile).channel.use { srcChannel ->
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
            android.util.Log.e("HlsQueueEngine", "HLS download failed for id $downloadId: ${e.message}", e)
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

    private fun fetchPlaylistText(url: String, headers: Headers): String {
        val req = Request.Builder().url(url).headers(headers).get().build()
        return client.newCall(req).execute().use { resp ->
            if (resp.code == 403 || resp.code == 410) {
                throw HttpStatusException(resp.code, "Failed to fetch playlist HTTP ${resp.code}")
            }
            if (!resp.isSuccessful) throw IOException("Failed to fetch playlist HTTP ${resp.code}")
            resp.body?.string() ?: throw IOException("Empty playlist response")
        }
    }

    private suspend fun parseAndPopulateTracks(
        download: DownloadEntity,
        headers: Headers,
        workDir: File
    ): Map<Int, List<ParsedHlsSegment>> {
        val rootContent = fetchPlaylistText(download.url, headers)
        val result = mutableMapOf<Int, List<ParsedHlsSegment>>()

        if (HlsPlaylistParser.isMasterPlaylist(rootContent)) {
            val master = HlsPlaylistParser.parseMasterPlaylist(download.url, rootContent)
            val selectedVariant = master.variants.firstOrNull()
                ?: throw IOException("No variants found in master playlist")

            val videoContent = fetchPlaylistText(selectedVariant.url, headers)
            File(workDir, "media_track_0.m3u8").writeText(videoContent)
            File(workDir, "media_track_0.url").writeText(selectedVariant.url)
            result[0] = HlsPlaylistParser.parseMediaPlaylist(selectedVariant.url, videoContent, trackIndex = 0).segments

            if (selectedVariant.audioGroupId != null) {
                val audioTrack = master.audioTracks.firstOrNull { it.groupId == selectedVariant.audioGroupId && it.isDefault }
                    ?: master.audioTracks.firstOrNull { it.groupId == selectedVariant.audioGroupId }
                if (audioTrack != null && audioTrack.url.isNotBlank() && audioTrack.url != selectedVariant.url) {
                    val audioContent = fetchPlaylistText(audioTrack.url, headers)
                    File(workDir, "media_track_1.m3u8").writeText(audioContent)
                    File(workDir, "media_track_1.url").writeText(audioTrack.url)
                    result[1] = HlsPlaylistParser.parseMediaPlaylist(audioTrack.url, audioContent, trackIndex = 1).segments
                }
            }
        } else {
            File(workDir, "media_track_0.m3u8").writeText(rootContent)
            File(workDir, "media_track_0.url").writeText(download.url)
            result[0] = HlsPlaylistParser.parseMediaPlaylist(download.url, rootContent, trackIndex = 0).segments
        }

        return result
    }

    private suspend fun recoverSegments(
        download: DownloadEntity,
        headers: Headers,
        workDir: File
    ): Map<Int, List<ParsedHlsSegment>> {
        // Attempt fresh playlist fetch first to refresh expiring CDN tokens
        return try {
            parseAndPopulateTracks(download, headers, workDir)
        } catch (e: Exception) {
            android.util.Log.e("HlsQueueEngine", "HLS refresh failed for id ${download.id}: ${e.message}", e)
            val httpEx = findHttpStatusException(e)
            if (httpEx != null && (httpEx.code == 403 || httpEx.code == 410)) {
                throw e
            }
            // Fallback to cached playlist if offline
            val cachedVideo = File(workDir, "media_track_0.m3u8")
            if (cachedVideo.exists() && cachedVideo.length() > 0L) {
                val videoUrl = File(workDir, "media_track_0.url").takeIf { it.exists() }?.readText()?.trim() ?: download.url
                val result = mutableMapOf<Int, List<ParsedHlsSegment>>()
                val videoContent = cachedVideo.readText()
                result[0] = HlsPlaylistParser.parseMediaPlaylist(videoUrl, videoContent, trackIndex = 0).segments

                val cachedAudio = File(workDir, "media_track_1.m3u8")
                if (cachedAudio.exists() && cachedAudio.length() > 0L) {
                    val audioUrl = File(workDir, "media_track_1.url").takeIf { it.exists() }?.readText()?.trim() ?: download.url
                    val audioContent = cachedAudio.readText()
                    result[1] = HlsPlaylistParser.parseMediaPlaylist(audioUrl, audioContent, trackIndex = 1).segments
                }
                result
            } else {
                throw e
            }
        }
    }

    private suspend fun downloadAndAssembleTrack(
        downloadId: Long,
        trackIndex: Int,
        segments: List<ParsedHlsSegment>,
        headers: Headers,
        workDir: File,
        onSegmentMerged: suspend (cumulativeBytes: Long) -> Unit
    ) = coroutineScope {
        val trackFile = File(workDir, "track_${trackIndex}.tmp")
        val lastMergedOffset = dao.getLastMergedOffset(downloadId, trackIndex)

        val raf = RandomAccessFile(trackFile, "rw")
        val trackChannel = raf.channel

        // Crash recovery: truncate track file to last persisted MERGED chunk offset
        if (trackFile.exists() && lastMergedOffset >= 0L) {
            trackChannel.truncate(lastMergedOffset)
            trackChannel.position(lastMergedOffset)
        }

        val mergeMutex = Mutex()
        var cumulativeBytes = lastMergedOffset
        val maxMerged = dao.getAllChunks(downloadId)
            .filter { it.trackIndex == trackIndex && it.state == ChunkState.MERGED }
            .maxOfOrNull { it.chunkIndex }
        var nextMergeChunkIndex = if (maxMerged == null) 0 else maxMerged + 1

        val pendingSegments = segments.filter { it.chunkIndex >= nextMergeChunkIndex }
        if (pendingSegments.isEmpty()) {
            trackChannel.close()
            raf.close()
            return@coroutineScope
        }

        val segmentQueue = ConcurrentLinkedQueue(pendingSegments)
        val completionSignal = Channel<Unit>(Channel.CONFLATED)

        try {
            // Background Merger: wakes immediately whenever any worker finishes a segment,
            // assembly merges all contiguous finished segments in strict sequence without blocking workers.
            val mergerJob = launch(Dispatchers.IO) {
                var uncommittedSegments = 0
                while (isActive && nextMergeChunkIndex < segments.size) {
                    var mergedAny = false
                    mergeMutex.withLock {
                        while (isActive) {
                            val nextSegFile = File(workDir, "seg_${trackIndex}_${nextMergeChunkIndex}.tmp")
                            if (!nextSegFile.exists() || nextSegFile.length() == 0L) break

                            FileInputStream(nextSegFile).channel.use { inChannel ->
                                val buffer = ByteBuffer.allocateDirect(64 * 1024)
                                while (inChannel.read(buffer) > 0) {
                                    buffer.flip()
                                    cumulativeBytes += trackChannel.write(buffer)
                                    buffer.clear()
                                }
                            }

                            nextSegFile.delete()
                            nextMergeChunkIndex++
                            uncommittedSegments++
                            mergedAny = true

                            // Periodically sync OS dirty pages and update database (every 8 segments or when done)
                            if (uncommittedSegments >= 8 || nextMergeChunkIndex >= segments.size) {
                                trackChannel.force(false)
                                dao.markChunkMerged(downloadId, trackIndex, nextMergeChunkIndex - 1, cumulativeBytes)
                                dao.markChunksMergedUpTo(downloadId, trackIndex, nextMergeChunkIndex - 1)
                                uncommittedSegments = 0
                            }
                            onSegmentMerged(cumulativeBytes)
                        }
                    }

                    if (nextMergeChunkIndex >= segments.size) break
                    if (!mergedAny) {
                        // Wait for next worker completion signal
                        completionSignal.receiveCatching()
                    }
                }

                // Final commit on completion
                if (uncommittedSegments > 0) {
                    trackChannel.force(false)
                    dao.markChunkMerged(downloadId, trackIndex, nextMergeChunkIndex - 1, cumulativeBytes)
                    dao.markChunksMergedUpTo(downloadId, trackIndex, nextMergeChunkIndex - 1)
                }
            }

            // Continuous Worker Pool: Pull from shared queue without batch waiting barriers
            val concurrency = maxParallelSegments.coerceIn(4, 8)
            val workers = (0 until concurrency).map {
                launch(Dispatchers.IO) {
                    while (isActive) {
                        val segment = segmentQueue.poll() ?: break
                        dao.updateChunkProgress(downloadId, trackIndex, segment.chunkIndex, 0L, ChunkState.IN_FLIGHT)
                        val segFile = File(workDir, "seg_${trackIndex}_${segment.chunkIndex}.tmp")
                        if (!segFile.exists() || segFile.length() == 0L) {
                            downloadSegment(segment, headers, segFile)
                        }
                        val segSize = segFile.length()
                        dao.updateChunkProgress(downloadId, trackIndex, segment.chunkIndex, segSize, ChunkState.DONE)
                        completionSignal.trySend(Unit)
                    }
                }
            }

            workers.forEach { it.join() }
            completionSignal.trySend(Unit)
            mergerJob.join()
        } finally {
            trackChannel.force(false)
            runCatching { trackChannel.close() }
            runCatching { raf.close() }
        }
    }

    private suspend fun downloadSegment(
        segment: ParsedHlsSegment,
        headers: Headers,
        destinationFile: File
    ) = withContext(Dispatchers.IO) {
        val partFile = File(destinationFile.parentFile, "${destinationFile.name}.part")
        val reqBuilder = Request.Builder().url(segment.url).headers(headers)
        if (segment.byteRangeLength != null && segment.byteRangeLength > 0L) {
            val start = segment.byteRangeOffset ?: 0L
            val end = start + segment.byteRangeLength - 1L
            reqBuilder.header("Range", "bytes=$start-$end")
        }
        val req = reqBuilder.get().build()
        client.newCall(req).execute().use { resp ->
            if (resp.code == 403 || resp.code == 410) {
                throw HttpStatusException(resp.code, "Segment download failed HTTP ${resp.code}: ${segment.url}")
            }
            if (!resp.isSuccessful) throw IOException("Segment download failed HTTP ${resp.code}: ${segment.url}")
            val body = resp.body ?: throw IOException("Empty segment body")

            val cipher = if (segment.keyMethod.equals("AES-128", ignoreCase = true) && !segment.keyUrl.isNullOrBlank()) {
                val keyBytes = getOrFetchKey(segment.keyUrl, headers)
                val ivBytes = parseOrGenerateIv(segment.ivHex, segment.sequenceNumber)
                Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
                    init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
                }
            } else null

            val outStream = FileOutputStream(partFile)
            val inStream = if (cipher != null) CipherInputStream(body.byteStream(), cipher) else body.byteStream()

            outStream.use { fileOut ->
                inStream.use { rawIn ->
                    val buffer = ByteArray(64 * 1024)
                    var bytes = rawIn.read(buffer)
                    while (bytes >= 0) {
                        rateLimiter.consume(bytes)
                        fileOut.write(buffer, 0, bytes)
                        bytes = rawIn.read(buffer)
                    }
                }
            }
        }
        if (!partFile.renameTo(destinationFile)) {
            partFile.copyTo(destinationFile, overwrite = true)
            partFile.delete()
        }
    }

    private suspend fun getOrFetchKey(keyUrl: String, headers: Headers): ByteArray =
        withContext(Dispatchers.IO) {
            keyCache[keyUrl]?.let { return@withContext it }
            val req = Request.Builder().url(keyUrl).headers(headers).get().build()
            val bytes = client.newCall(req).execute().use { resp ->
                if (resp.code == 403 || resp.code == 410) {
                    throw HttpStatusException(resp.code, "Failed to fetch AES key HTTP ${resp.code}")
                }
                if (!resp.isSuccessful) throw IOException("Failed to fetch AES key HTTP ${resp.code}")
                resp.body?.bytes() ?: throw IOException("Empty key body")
            }
            if (bytes.size != 16) throw IOException("Invalid AES key length: ${bytes.size}")
            keyCache[keyUrl] = bytes
            bytes
        }

    private fun remuxToMp4(trackFiles: List<File>, outputFile: File): Boolean =
        HlsRemuxer.remuxToMp4(trackFiles, outputFile)

    private fun isAlreadyMp4(file: File): Boolean {
        if (!file.exists() || file.length() < 12L) return false
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(12)
                val read = fis.read(header)
                if (read >= 8) {
                    val boxType = String(header, 4, 4, Charsets.US_ASCII)
                    boxType == "ftyp" || boxType == "styp"
                } else false
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun cancelAndDelete(downloadId: Long, storage: DownloadStorageProvider) =
        withContext(Dispatchers.IO) {
            val download = dao.getDownload(downloadId)
            val workDir = File(context.cacheDir, "hls_$downloadId")
            workDir.deleteRecursively()

            if (download?.destinationUri != null) {
                storage.delete(download.destinationUri)
            }
            dao.deleteChunksForDownload(downloadId)
            dao.markTerminalAndPurgeCredentials(downloadId, DownloadState.CANCELLED)
        }

    companion object {
        fun parseOrGenerateIv(ivHex: String?, sequenceNumber: Long): ByteArray {
            if (!ivHex.isNullOrBlank()) {
                val clean = ivHex.trim().removePrefix("0x").removePrefix("0X")
                val padded = clean.padStart(32, '0')
                val bytes = ByteArray(16)
                for (i in 0 until 16) {
                    val byteStr = padded.substring(i * 2, i * 2 + 2)
                    bytes[i] = byteStr.toInt(16).toByte()
                }
                return bytes
            }
            // RFC 8216 Section 5.2 sequence-number big-endian 16-byte IV fallback
            return ByteBuffer.allocate(16).putLong(8, sequenceNumber).array()
        }

        fun findHttpStatusException(throwable: Throwable?): HttpStatusException? {
            var cur = throwable
            while (cur != null) {
                if (cur is HttpStatusException) return cur
                cur = cur.cause
            }
            return null
        }
    }
}
