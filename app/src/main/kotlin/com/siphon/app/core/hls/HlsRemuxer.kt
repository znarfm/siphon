package com.siphon.app.core.hls

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

object HlsRemuxer {
    fun remuxToMp4(trackFiles: List<File>, outputFile: File): Boolean {
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

            var maxInputSize = 4 * 1024 * 1024
            for (extractor in extractors) {
                for (t in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(t)
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        maxInputSize = maxOf(maxInputSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                    }
                }
            }

            muxer.start()
            val buffer = ByteBuffer.allocate(maxInputSize)
            val bufferInfo = MediaCodec.BufferInfo()
            val initialPts = extractors.mapNotNull { if (it.sampleTime >= 0L) it.sampleTime else null }.minOrNull() ?: 0L

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
                val rawPts = extractor.sampleTime
                if (sampleSize < 0 || rawPts < 0L) {
                    extractor.advance()
                    continue
                }

                val adjustedPts = (rawPts - initialPts).coerceAtLeast(0L)

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
}
