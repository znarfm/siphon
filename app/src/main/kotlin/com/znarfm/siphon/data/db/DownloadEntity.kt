package com.znarfm.siphon.data.db

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DownloadType {
    HTTP,
    HLS
}

enum class DownloadState {
    QUEUED,
    ACTIVE,
    MERGING,
    PAUSED,
    WAITING_FOR_NETWORK,
    AUTH_EXPIRED,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Immutable
@Entity(
    tableName = "downloads",
    indices = [Index("state"), Index("sourcePageUrl")]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val displayName: String,
    val destinationUri: String?,
    val mimeType: String,
    val totalBytes: Long,
    val etag: String?,
    val lastModified: String?,
    val isResumable: Boolean,
    val type: DownloadType,
    val state: DownloadState,
    val sourcePageUrl: String?,
    val headersJson: String?,
    val createdAt: Long = System.currentTimeMillis()
)
