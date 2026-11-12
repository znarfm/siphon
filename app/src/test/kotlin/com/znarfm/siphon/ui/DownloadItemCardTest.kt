package com.znarfm.siphon.ui

import com.znarfm.siphon.data.db.ChunkEntity
import com.znarfm.siphon.data.db.ChunkState
import com.znarfm.siphon.data.db.DownloadEntity
import com.znarfm.siphon.data.db.DownloadState
import com.znarfm.siphon.data.db.DownloadType
import com.znarfm.siphon.ui.queue.computeDownloadedBytes
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadItemCardTest {

    @Test
    fun testHlsDownloadedBytesDoesNotSumCumulativeOffsets() {
        val download = DownloadEntity(
            id = 1L,
            url = "https://example.com/stream.m3u8",
            displayName = "stream.mp4",
            destinationUri = "content://downloads/1",
            mimeType = "video/mp4",
            totalBytes = 300_000_000L,
            etag = null,
            lastModified = null,
            isResumable = true,
            type = DownloadType.HLS,
            state = DownloadState.ACTIVE,
            sourcePageUrl = null,
            headersJson = null
        )

        // Simulate 10 segments of 30MB each (total 300MB)
        // With markChunkMerged, currentOffset/endOffset are cumulative file offsets
        val chunks = (0 until 10).map { i ->
            val cumulativeOffset = (i + 1) * 30_000_000L
            ChunkEntity(
                downloadId = 1L,
                trackIndex = 0,
                chunkIndex = i,
                startOffset = 0L,
                currentOffset = cumulativeOffset,
                endOffset = cumulativeOffset,
                state = ChunkState.MERGED
            )
        }

        val downloadedBytes = computeDownloadedBytes(download, chunks)
        // Should be 300MB, NOT 1.65GB (sum of cumulative offsets)
        assertEquals(300_000_000L, downloadedBytes)
    }
}
