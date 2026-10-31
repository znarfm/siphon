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
    private val maxParallelSegments: Int = 4
) {

    private val keyCache = ConcurrentHashMap<String, ByteArray>()

    suspend fun executeWithStorage(
        downloadId: Long,
        storage: DownloadStorageProvider,
        customTreeUri: Uri? = null,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): DownloadState = withContext(Dispatchers.IO) {
        val download = dao.getDownload(downloadId)
            ?: return@withContext DownloadState.FAILED

        val destUri = if (!download.destinationUri.isNullOrBlank()) {
            Uri.parse(download.destinationUri).also { storage.refreshPendingExpiry(it) }
        } else {
            storage.createDestination(
                displayName = download.displayName,
                mimeType = download.mimeType,
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
                    ChunkEntity(
                        downloadId = downloadId,
                        trackIndex = seg.trackIndex,
                        chunkIndex = seg.chunkIndex,
                        startOffset = 0L,
                        currentOffset = 0L,
                        endOffset = 0L,
                        state = ChunkState.PENDING
                    )
                }
                dao.insertChunks(allChunks)
                parsed
            } else {
                recoverSegments(download, headers, workDir)
            }

            val totalSegmentsCount = segmentsByTrack.values.sumOf { it.size }

            // 2. Process each track with sliding-window assembly
            for ((trackIndex, trackSegments) in segmentsByTrack) {
                downloadAndAssembleTrack(
                    downloadId = downloadId,
                    trackIndex = trackIndex,
                    segments = trackSegments,
                    headers = headers,
                    workDir = workDir,
                    onSegmentMerged = {
                        val merged = runCatching {
                            dao.getAllChunks(downloadId).count { it.state == ChunkState.MERGED }
                        }.getOrDefault(0)
                        onProgress?.invoke(merged.toLong(), totalSegmentsCount.toLong())
                    }
                )
            }

            // 3. Remux track files into final destination
            val trackFiles = segmentsByTrack.keys.sorted().map { trackIdx ->
                File(workDir, "track_${trackIdx}.tmp")
            }

            val remuxOutputFile = File(workDir, "remux_output.mp4")
            val remuxSuccess = remuxToMp4(trackFiles, remuxOutputFile)

            val finalSourceFile = if (remuxSuccess && remuxOutputFile.exists() && remuxOutputFile.length() > 0) {
                remuxOutputFile
            } else {
                // Fallback to copying raw track 0 (.ts)
                trackFiles.firstOrNull() ?: throw IOException("No track files produced")
            }

            // Copy to target destination URI
            storage.openWriteSession(destUri, totalBytes = finalSourceFile.length()).use { session ->
                val destChannel = session.channel
                destChannel.truncate(0)
                destChannel.position(0)
                FileInputStream(finalSourceFile).channel.use { srcChannel ->
                    val size = srcChannel.size()
                    var transferred = 0L
                    while (transferred < size) {
                        val count = srcChannel.transferTo(transferred, size - transferred, destChannel)
                        if (count <= 0) break
                        transferred += count
                    }
                }
                destChannel.force(true)
            }

            storage.finalizePending(destUri)
            dao.markTerminalAndPurgeCredentials(downloadId, DownloadState.COMPLETED)
            workDir.deleteRecursively()
            DownloadState.COMPLETED
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                dao.updateDownloadState(downloadId, DownloadState.PAUSED)
            }
            DownloadState.PAUSED
        } catch (e: Exception) {
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

    private suspend fun parseAndPopulateTracks(
        download: DownloadEntity,
        headers: Headers,
        workDir: File
    ): Map<Int, List<ParsedHlsSegment>> {
        val rootReq = Request.Builder().url(download.url).headers(headers).get().build()
        val rootContent = client.newCall(rootReq).execute().use { resp ->
            if (resp.code == 403 || resp.code == 410) {
                throw HttpStatusException(resp.code, "Failed to fetch playlist: HTTP ${resp.code}")
            }
            if (!resp.isSuccessful) throw IOException("Failed to fetch playlist: HTTP ${resp.code}")
            resp.body?.string() ?: throw IOException("Empty playlist response")
        }

        val result = mutableMapOf<Int, List<ParsedHlsSegment>>()

        if (HlsPlaylistParser.isMasterPlaylist(rootContent)) {
            val master = HlsPlaylistParser.parseMasterPlaylist(download.url, rootContent)
            val selectedVariant = master.variants.firstOrNull()
                ?: throw IOException("No variants found in master playlist")

            // Fetch video/main media playlist
            val videoReq = Request.Builder().url(selectedVariant.url).headers(headers).get().build()
            val videoContent = client.newCall(videoReq).execute().use { resp ->
                if (resp.code == 403 || resp.code == 410) {
                    throw HttpStatusException(resp.code, "Failed to fetch media playlist HTTP ${resp.code}")
                }
                if (!resp.isSuccessful) throw IOException("Failed to fetch media playlist HTTP ${resp.code}")
                resp.body?.string().orEmpty()
            }
            File(workDir, "media_track_0.m3u8").writeText(videoContent)
            File(workDir, "media_track_0.url").writeText(selectedVariant.url)
            val videoMedia = HlsPlaylistParser.parseMediaPlaylist(selectedVariant.url, videoContent, trackIndex = 0)
            result[0] = videoMedia.segments

            // Check if there is a separate audio track
            if (selectedVariant.audioGroupId != null) {
                val audioTrack = master.audioTracks.firstOrNull { it.groupId == selectedVariant.audioGroupId }
                if (audioTrack != null && audioTrack.url.isNotBlank() && audioTrack.url != selectedVariant.url) {
                    val audioReq = Request.Builder().url(audioTrack.url).headers(headers).get().build()
                    val audioContent = client.newCall(audioReq).execute().use { resp ->
                        if (resp.code == 403 || resp.code == 410) {
                            throw HttpStatusException(resp.code, "Failed to fetch audio playlist HTTP ${resp.code}")
                        }
                        if (!resp.isSuccessful) throw IOException("Failed to fetch audio playlist HTTP ${resp.code}")
                        resp.body?.string().orEmpty()
                    }
                    File(workDir, "media_track_1.m3u8").writeText(audioContent)
                    File(workDir, "media_track_1.url").writeText(audioTrack.url)
                    val audioMedia = HlsPlaylistParser.parseMediaPlaylist(audioTrack.url, audioContent, trackIndex = 1)
                    result[1] = audioMedia.segments
                }
            }
        } else {
            File(workDir, "media_track_0.m3u8").writeText(rootContent)
            File(workDir, "media_track_0.url").writeText(download.url)
            val media = HlsPlaylistParser.parseMediaPlaylist(download.url, rootContent, trackIndex = 0)
            result[0] = media.segments
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
        onSegmentMerged: suspend () -> Unit
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
                                val segSize = inChannel.size()
                                var pos = 0L
                                while (pos < segSize) {
                                    val transferred = inChannel.transferTo(pos, segSize - pos, trackChannel)
                                    if (transferred <= 0) break
                                    pos += transferred
                                }
                                cumulativeBytes += segSize
                            }

                            nextSegFile.delete()
                            nextMergeChunkIndex++
                            uncommittedSegments++
                            mergedAny = true

                            // Periodically sync OS dirty pages and update database (every 8 segments or when done)
                            if (uncommittedSegments >= 8 || nextMergeChunkIndex >= segments.size) {
                                trackChannel.force(false)
                                dao.markChunkMerged(downloadId, trackIndex, nextMergeChunkIndex - 1, cumulativeBytes)
                                uncommittedSegments = 0
                            }
                            onSegmentMerged()
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
                }
            }

            // Continuous Worker Pool: Pull from shared queue without batch waiting barriers
            val concurrency = maxParallelSegments.coerceIn(4, 8)
            val workers = (0 until concurrency).map {
                launch(Dispatchers.IO) {
                    while (isActive) {
                        val segment = segmentQueue.poll() ?: break
                        val segFile = File(workDir, "seg_${trackIndex}_${segment.chunkIndex}.tmp")
                        if (!segFile.exists() || segFile.length() == 0L) {
                            downloadSegment(segment, headers, segFile)
                        }
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
        val req = Request.Builder().url(segment.url).headers(headers).get().build()
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

    private fun remuxToMp4(trackFiles: List<File>, outputFile: File): Boolean {
        val validTrackFiles = trackFiles.filter { it.exists() && it.length() > 0L }
        if (validTrackFiles.isEmpty()) return false

        val extractors = mutableListOf<MediaExtractor>()
        var muxer: MediaMuxer? = null

        return try {
            for (file in validTrackFiles) {
                val extractor = MediaExtractor().apply { setDataSource(file.absolutePath) }
                extractors.add(extractor)
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackMap = mutableMapOf<Pair<Int, Int>, Int>() // (extractorIdx, trackIdx) -> muxerTrackIndex

            for ((extIdx, extractor) in extractors.withIndex()) {
                for (t in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(t)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                        val muxerIdx = muxer.addTrack(format)
                        trackMap[Pair(extIdx, t)] = muxerIdx
                        extractor.selectTrack(t)
                    }
                }
            }

            if (trackMap.isEmpty()) return false

            muxer.start()
            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            val lastPtsMap = mutableMapOf<Int, Long>()

            while (true) {
                var bestExtIdx = -1
                var minSampleTime = Long.MAX_VALUE

                for ((extIdx, extractor) in extractors.withIndex()) {
                    val sampleTime = extractor.sampleTime
                    if (sampleTime != -1L && sampleTime < minSampleTime) {
                        minSampleTime = sampleTime
                        bestExtIdx = extIdx
                    }
                }

                if (bestExtIdx == -1) break

                val extractor = extractors[bestExtIdx]
                val trackIdx = extractor.sampleTrackIndex
                val muxerTrack = trackMap[Pair(bestExtIdx, trackIdx)]
                if (muxerTrack == null) {
                    extractor.advance()
                    continue
                }

                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val rawPts = extractor.sampleTime
                val lastPts = lastPtsMap[muxerTrack] ?: -1L
                val adjustedPts = if (rawPts <= lastPts) lastPts + 1L else rawPts
                lastPtsMap[muxerTrack] = adjustedPts

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = adjustedPts
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                extractor.advance()
            }

            muxer.stop()
            true
        } catch (e: Exception) {
            outputFile.delete()
            false
        } finally {
            for (extractor in extractors) {
                try { extractor.release() } catch (_: Throwable) {}
            }
            try { muxer?.release() } catch (_: Throwable) {}
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
