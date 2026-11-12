package com.znarfm.siphon.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class ChunkSnapshot(
    val downloadId: Long,
    val trackIndex: Int,
    val chunkIndex: Int,
    val currentOffset: Long,
    val state: ChunkState
)

@Dao
interface DownloadDao {

    @Insert
    suspend fun insertDownload(download: DownloadEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<ChunkEntity>)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownload(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE state = :state ORDER BY createdAt ASC")
    suspend fun getDownloadsByState(state: DownloadState): List<DownloadEntity>

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeDownloads(): Flow<List<DownloadEntity>>

    @Query(
        """
        SELECT * FROM download_chunks
        WHERE downloadId = :downloadId
        ORDER BY trackIndex ASC, chunkIndex ASC
        """
    )
    suspend fun getAllChunks(downloadId: Long): List<ChunkEntity>

    @Query(
        """
        SELECT * FROM download_chunks
        WHERE downloadId = :downloadId
        ORDER BY trackIndex ASC, chunkIndex ASC
        """
    )
    fun observeChunks(downloadId: Long): Flow<List<ChunkEntity>>

    @Query("DELETE FROM download_chunks WHERE downloadId = :downloadId")
    suspend fun deleteChunksForDownload(downloadId: Long)

    @Query("UPDATE downloads SET isResumable = :isResumable WHERE id = :id")
    suspend fun updateResumable(id: Long, isResumable: Boolean)

    @Query("UPDATE downloads SET destinationUri = :destinationUri WHERE id = :id")
    suspend fun updateDestinationUri(id: Long, destinationUri: String?)

    @Query(
        """
        UPDATE download_chunks
        SET currentOffset = :currentOffset, state = :state
        WHERE downloadId = :downloadId AND trackIndex = :trackIndex AND chunkIndex = :chunkIndex
        """
    )
    suspend fun updateChunkProgress(
        downloadId: Long,
        trackIndex: Int,
        chunkIndex: Int,
        currentOffset: Long,
        state: ChunkState
    )

    @Transaction
    suspend fun commitCheckpoints(snapshots: List<ChunkSnapshot>) {
        for (s in snapshots) {
            updateChunkProgress(s.downloadId, s.trackIndex, s.chunkIndex, s.currentOffset, s.state)
        }
    }

    @Query(
        """
        UPDATE download_chunks
        SET state = 'MERGED'
        WHERE downloadId = :downloadId AND trackIndex = :trackIndex AND chunkIndex <= :maxChunkIndex
        """
    )
    suspend fun markChunksMergedUpTo(
        downloadId: Long,
        trackIndex: Int,
        maxChunkIndex: Int
    )

    @Query(
        """
        UPDATE download_chunks
        SET endOffset = :mergedOffset, currentOffset = :mergedOffset, state = 'MERGED'
        WHERE downloadId = :downloadId AND trackIndex = :trackIndex AND chunkIndex = :chunkIndex
        """
    )
    suspend fun markChunkMerged(
        downloadId: Long,
        trackIndex: Int,
        chunkIndex: Int,
        mergedOffset: Long
    )

    @Query(
        """
        SELECT COALESCE(MAX(endOffset), 0) FROM download_chunks
        WHERE downloadId = :downloadId AND trackIndex = :trackIndex AND state = 'MERGED'
        """
    )
    suspend fun getLastMergedOffset(downloadId: Long, trackIndex: Int): Long

    @Query("UPDATE download_chunks SET state = 'PENDING' WHERE state = 'IN_FLIGHT'")
    suspend fun resetInFlightChunksOnBoot()

    @Query(
        """
        UPDATE download_chunks
        SET state = 'PENDING'
        WHERE downloadId = :downloadId AND state = 'IN_FLIGHT'
        """
    )
    suspend fun resetInFlightChunksForDownload(downloadId: Long)

    @Query("UPDATE downloads SET state = :state WHERE id = :id")
    suspend fun updateDownloadState(id: Long, state: DownloadState)

    @Query("UPDATE downloads SET totalBytes = :totalBytes WHERE id = :id")
    suspend fun updateTotalBytes(id: Long, totalBytes: Long)

    @Transaction
    suspend fun markTerminalAndPurgeCredentials(id: Long, terminalState: DownloadState) {
        updateDownloadState(id, terminalState)
        purgeCredentials(id)
    }

    @Query("UPDATE downloads SET headersJson = NULL WHERE id = :id")
    suspend fun purgeCredentials(id: Long)

    @Query(
        """
        UPDATE downloads
        SET url = :newUrl, etag = :newEtag, headersJson = :newHeadersJson, state = 'QUEUED'
        WHERE id = :id
        """
    )
    suspend fun rebindExpiredLink(
        id: Long,
        newUrl: String,
        newEtag: String?,
        newHeadersJson: String?
    )

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: Long)

    @Query("UPDATE downloads SET displayName = :newName WHERE id = :id")
    suspend fun updateDisplayName(id: Long, newName: String)

    @Query("DELETE FROM downloads WHERE state = 'COMPLETED'")
    suspend fun clearCompletedDownloads()

    @Query(
        """
        UPDATE download_chunks
        SET currentOffset = startOffset, state = 'PENDING'
        WHERE downloadId = :id
        """
    )
    suspend fun resetChunksToStart(id: Long)
}
