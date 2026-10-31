package com.siphon.app.data.repository

import android.app.Application
import android.content.Context
import android.net.Uri
import com.siphon.app.core.network.HeadersCodec
import com.siphon.app.core.network.MetadataProber
import com.siphon.app.core.service.DownloadForegroundService
import com.siphon.app.core.sniffer.SniffedMedia
import com.siphon.app.core.storage.DownloadStorageProvider
import com.siphon.app.data.db.ChunkEntity
import com.siphon.app.data.db.DownloadDao
import com.siphon.app.data.db.DownloadEntity
import com.siphon.app.data.db.DownloadState
import com.siphon.app.data.db.DownloadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.Headers
import java.io.File

data class ConcurrencySettings(
    val maxDownloads: Int = 2,
    val workers: Int = 6,
    val bandwidthLimit: Long = 0L,
    val wifiOnly: Boolean = false
)

class DownloadRepository(
    val application: Application,
    val dao: DownloadDao,
    val storage: DownloadStorageProvider,
    val metadataProber: MetadataProber = MetadataProber()
) {
    private val prefs = application.getSharedPreferences(
        DownloadForegroundService.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _concurrencySettings = MutableStateFlow(
        ConcurrencySettings(
            maxDownloads = prefs.getInt(DownloadForegroundService.PREF_MAX_CONCURRENT_DOWNLOADS, 2).coerceIn(1, 4),
            workers = prefs.getInt(DownloadForegroundService.PREF_WORKERS_PER_DOWNLOAD, 6).coerceIn(1, 8),
            bandwidthLimit = prefs.getLong(DownloadForegroundService.PREF_BANDWIDTH_LIMIT_BYTES, 0L).coerceAtLeast(0L),
            wifiOnly = prefs.getBoolean(DownloadForegroundService.PREF_WIFI_ONLY, false)
        )
    )
    val concurrencySettings: Flow<ConcurrencySettings> = _concurrencySettings.asStateFlow()

    val downloads: Flow<List<DownloadEntity>> = dao.observeDownloads()

    @OptIn(ExperimentalCoroutinesApi::class)
    val chunksMap: Flow<Map<Long, List<ChunkEntity>>> = downloads.flatMapLatest { list ->
        val activeIds = list.filter {
            it.state != DownloadState.COMPLETED && it.state != DownloadState.CANCELLED
        }.map { it.id }
        if (activeIds.isEmpty()) {
            flowOf(emptyMap())
        } else {
            val flows = activeIds.map { id ->
                dao.observeChunks(id).map { chunks -> id to chunks }
            }
            combine(flows) { pairs -> pairs.toMap() }
        }
    }

    suspend fun startDownload(
        media: SniffedMedia,
        variantUrl: String? = null,
        customFilename: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val downloadUrl = variantUrl ?: media.url
        val name = customFilename ?: media.displayName
        val headersJson = HeadersCodec.encode(media.headers)

        val entity = DownloadEntity(
            url = downloadUrl,
            displayName = name,
            destinationUri = null,
            mimeType = media.mimeType,
            totalBytes = media.totalBytes,
            etag = null,
            lastModified = null,
            isResumable = true,
            type = media.mediaType,
            state = DownloadState.QUEUED,
            sourcePageUrl = media.sourcePageUrl,
            headersJson = headersJson
        )

        val id = dao.insertDownload(entity)
        val settings = _concurrencySettings.value
        DownloadForegroundService.startOrResume(
            context = application,
            downloadId = id,
            maxConcurrent = settings.maxDownloads,
            workers = settings.workers
        )
        id
    }

    fun pauseDownload(id: Long) {
        DownloadForegroundService.pause(application, id)
    }

    suspend fun resumeDownload(id: Long): Boolean = withContext(Dispatchers.IO) {
        val download = dao.getDownload(id) ?: return@withContext false
        if (download.state == DownloadState.AUTH_EXPIRED) {
            return@withContext false
        }
        val settings = _concurrencySettings.value
        DownloadForegroundService.startOrResume(
            context = application,
            downloadId = id,
            maxConcurrent = settings.maxDownloads,
            workers = settings.workers
        )
        true
    }

    fun cancelDownload(id: Long) {
        DownloadForegroundService.cancel(application, id)
    }

    fun pauseAll() {
        DownloadForegroundService.pauseAll(application)
    }

    fun resumeAll() {
        DownloadForegroundService.resumeAll(application)
    }

    suspend fun clearCompleted() = withContext(Dispatchers.IO) {
        dao.clearCompletedDownloads()
    }

    suspend fun retryDownload(id: Long) = withContext(Dispatchers.IO) {
        val download = dao.getDownload(id) ?: return@withContext
        if (download.type == DownloadType.HLS) {
            val workDir = File(application.cacheDir, "hls_$id")
            workDir.deleteRecursively()
        }
        dao.resetChunksToStart(id)
        dao.updateDownloadState(id, DownloadState.QUEUED)
        val settings = _concurrencySettings.value
        DownloadForegroundService.startOrResume(
            context = application,
            downloadId = id,
            maxConcurrent = settings.maxDownloads,
            workers = settings.workers
        )
    }

    suspend fun renameDownload(id: Long, newName: String) = withContext(Dispatchers.IO) {
        val entity = dao.getDownload(id) ?: return@withContext
        val safeName = DownloadStorageProvider.sanitizeDisplayName(newName)
        storage.rename(entity.destinationUri, safeName)
        dao.updateDisplayName(id, safeName)
    }

    suspend fun deleteDownload(id: Long, deleteFile: Boolean = true) = withContext(Dispatchers.IO) {
        val entity = dao.getDownload(id)
        cancelDownload(id)
        if (deleteFile && entity?.destinationUri != null) {
            storage.delete(entity.destinationUri)
        }
        if (entity?.type == DownloadType.HLS) {
            val workDir = File(application.cacheDir, "hls_$id")
            workDir.deleteRecursively()
        }
        dao.deleteDownload(id)
    }

    suspend fun rebindDownload(
        target: DownloadEntity,
        media: SniffedMedia,
        variantUrl: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val candidateUrl = variantUrl ?: media.url
        val headersJson = HeadersCodec.encode(media.headers)

        if (media.mediaType == DownloadType.HLS || target.type == DownloadType.HLS) {
            val workDir = File(application.cacheDir, "hls_${target.id}")
            File(workDir, "media_track_0.m3u8").delete()
            File(workDir, "media_track_1.m3u8").delete()
            File(workDir, "media_track_0.url").delete()
            File(workDir, "media_track_1.url").delete()

            dao.rebindExpiredLink(
                id = target.id,
                newUrl = candidateUrl,
                newEtag = null,
                newHeadersJson = headersJson
            )
            val settings = _concurrencySettings.value
            DownloadForegroundService.startOrResume(
                context = application,
                downloadId = target.id,
                maxConcurrent = settings.maxDownloads,
                workers = settings.workers
            )
            true
        } else {
            val okHeadersBuilder = Headers.Builder()
            media.headers.forEach { (k, v) -> okHeadersBuilder.add(k, v) }
            val okHeaders = okHeadersBuilder.build()

            var strongEtag: String? = null
            val targetUriStr = target.destinationUri
            if (!targetUriStr.isNullOrBlank()) {
                val targetUri = Uri.parse(targetUriStr)
                val verified = runCatching {
                    storage.openReadSession(targetUri).use { session ->
                        val probeResult = metadataProber.verifyRefreshedUrl(
                            newUrl = candidateUrl,
                            headers = okHeaders,
                            expectedTotalBytes = target.totalBytes,
                            localChannel = session.channel,
                            sampleSize = 4096
                        )
                        strongEtag = probeResult.strongEtag
                        true
                    }
                }.getOrDefault(false)

                if (!verified && target.totalBytes > 0L) {
                    return@withContext false
                }
            }

            dao.rebindExpiredLink(
                id = target.id,
                newUrl = candidateUrl,
                newEtag = strongEtag,
                newHeadersJson = headersJson
            )
            val settings = _concurrencySettings.value
            DownloadForegroundService.startOrResume(
                context = application,
                downloadId = target.id,
                maxConcurrent = settings.maxDownloads,
                workers = settings.workers
            )
            true
        }
    }

    fun updateConcurrency(maxDownloads: Int, workers: Int, bandwidthLimit: Long, wifiOnly: Boolean) {
        val maxD = maxDownloads.coerceIn(1, 4)
        val w = workers.coerceIn(1, 8)
        val limit = bandwidthLimit.coerceAtLeast(0L)
        _concurrencySettings.value = ConcurrencySettings(maxD, w, limit, wifiOnly)
        prefs.edit()
            .putInt(DownloadForegroundService.PREF_MAX_CONCURRENT_DOWNLOADS, maxD)
            .putInt(DownloadForegroundService.PREF_WORKERS_PER_DOWNLOAD, w)
            .putLong(DownloadForegroundService.PREF_BANDWIDTH_LIMIT_BYTES, limit)
            .putBoolean(DownloadForegroundService.PREF_WIFI_ONLY, wifiOnly)
            .apply()
        DownloadForegroundService.updateConcurrency(application, maxD, w, limit, wifiOnly)
    }
}
