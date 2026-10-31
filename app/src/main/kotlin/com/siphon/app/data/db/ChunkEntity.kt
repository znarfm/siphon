package com.siphon.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

enum class ChunkState {
    PENDING,
    IN_FLIGHT,
    DONE,
    MERGED
}

@Entity(
    tableName = "download_chunks",
    primaryKeys = ["downloadId", "trackIndex", "chunkIndex"],
    foreignKeys = [
        ForeignKey(
            entity = DownloadEntity::class,
            parentColumns = ["id"],
            childColumns = ["downloadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("downloadId", "state")]
)
data class ChunkEntity(
    val downloadId: Long,
    val trackIndex: Int = 0,
    val chunkIndex: Int,
    val startOffset: Long,
    val currentOffset: Long,
    val endOffset: Long,
    val state: ChunkState = ChunkState.PENDING
)
