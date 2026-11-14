package com.znarfm.siphon.core.engine

import android.net.Uri
import com.znarfm.siphon.core.storage.DownloadStorageProvider
import com.znarfm.siphon.data.db.DownloadState

/**
 * Common contract for segmented/stream download engines (HLS, DASH).
 */
interface DownloadEngine {

    fun updateRateLimit(bytesPerSecond: Long)

    suspend fun executeWithStorage(
        downloadId: Long,
        storage: DownloadStorageProvider,
        customTreeUri: Uri? = null,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): DownloadState

    suspend fun cancelAndDelete(downloadId: Long, storage: DownloadStorageProvider)
}
