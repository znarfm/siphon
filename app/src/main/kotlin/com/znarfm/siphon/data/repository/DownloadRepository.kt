package com.znarfm.siphon.data.repository

import android.app.Application
import android.net.Uri
import com.znarfm.siphon.core.common.toOkHttpHeaders
import com.znarfm.siphon.core.network.HeadersCodec
import com.znarfm.siphon.core.network.MetadataProber
import com.znarfm.siphon.core.service.DownloadForegroundService
import com.znarfm.siphon.core.sniffer.SniffedMedia
import com.znarfm.siphon.core.storage.DownloadStorageProvider
import com.znarfm.siphon.data.db.ChunkEntity
import com.znarfm.siphon.data.db.DownloadDao
import com.znarfm.siphon.data.db.DownloadEntity
import com.znarfm.siphon.data.db.DownloadFileEntity
import com.znarfm.siphon.data.db.DownloadState
import com.znarfm.siphon.data.db.DownloadType
import com.znarfm.siphon.data.prefs.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Immutable
data class ConcurrencySettings(
    val maxDownloads: Int = 2,
    val workers: Int = 6,
    val bandwidthLimit: Long = 0L,
    val wifiOnly: Boolean = false,
    val customDownloadUri: String? = null
)

class DownloadRepository(
    val application: Application,
    val dao: DownloadDao,
    val storage: DownloadStorageProvider,
    val metadataProber: MetadataProber = MetadataProber()
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val prefs = AppPreferences.prefs(application)

    private val _concurrencySettings = MutableStateFlow(
        ConcurrencySettings(
            maxDownloads = prefs.getInt(AppPreferences.PREF_MAX_CONCURRENT_DOWNLOADS, 2).coerceIn(1, 4),
            workers = prefs.getInt(AppPreferences.PREF_WORKERS_PER_DOWNLOAD, 6).coerceIn(1, 8),
            bandwidthLimit = prefs.getLong(AppPreferences.PREF_BANDWIDTH_LIMIT_BYTES, 0L).coerceAtLeast(0L),
            wifiOnly = prefs.getBoolean(AppPreferences.PREF_WIFI_ONLY, false),
            customDownloadUri = prefs.getString(AppPreferences.PREF_CUSTOM_DOWNLOAD_URI, null)
        )
    )
    val concurrencySettings: Flow<ConcurrencySettings> = _concurrencySettings.asStateFlow()

    val downloads: Flow<List<DownloadEntity>> = dao.observeDownloads()

    val downloadFilesMap: Flow<Map<Long, List<DownloadFileEntity>>> =
        dao.observeAllDownloadFiles().map { list -> list.groupBy { it.downloadId } }

    fun observeDownloadFiles(downloadId: Long): Flow<List<DownloadFileEntity>> =
        dao.observeDownloadFiles(downloadId)

    @OptIn(ExperimentalCoroutinesApi::class)
    val chunksMap: Flow<Map<Long, List<ChunkEntity>>> = downloads
        .map { list ->
            list.filter {
                it.state != DownloadState.COMPLETED && it.state != DownloadState.CANCELLED
            }.map { it.id }
        }
        .distinctUntilChanged()
        .flatMapLatest { activeIds ->
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
        val selectedVariant = if (variantUrl != null) {
            media.variants.firstOrNull { it.url == variantUrl }
        } else null

        val downloadUrl = when {
            media.mediaType == DownloadType.DASH -> media.url
            variantUrl != null -> variantUrl
            else -> media.url
        }
        val name = customFilename ?: media.displayName
        val mergedHeaders = media.headers.toMutableMap()
        if (media.mediaType == DownloadType.HLS && !selectedVariant?.audioUrl.isNullOrBlank()) {
            mergedHeaders[HeadersCodec.META_HLS_AUDIO_URL] = selectedVariant!!.audioUrl!!
        }
        if (media.mediaType == DownloadType.DASH) {
            mergedHeaders[HeadersCodec.META_DASH_MPD_URL] = media.url
            if (!selectedVariant?.representationId.isNullOrBlank()) {
                mergedHeaders[HeadersCodec.META_DASH_REP_ID] = selectedVariant!!.representationId!!
            }
        }
        val headersJson = HeadersCodec.encode(mergedHeaders)

        val entity = DownloadEntity(
            url = downloadUrl,
            displayName = name,
            destinationUri = null,
            mimeType = media.mimeType,
            totalBytes = media.totalBytes,
            etag = media.strongEtag,
            lastModified = media.lastModified,
            isResumable = media.isResumable,
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

    fun startMagnetDownload(magnetUri: String) {
        repoScope.launch {
            val extractedName = runCatching {
                Uri.parse(magnetUri).getQueryParameter("dn")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { DownloadStorageProvider.sanitizeDisplayName(it) }
            }.getOrNull() ?: "Magnet Download"

            val entity = DownloadEntity(
                url = magnetUri,
                displayName = extractedName,
                destinationUri = null,
                mimeType = "application/x-bittorrent",
                totalBytes = 0L,
                etag = null,
                lastModified = null,
                isResumable = true,
                type = DownloadType.TORRENT,
                state = DownloadState.QUEUED,
                sourcePageUrl = null,
                headersJson = null
            )

            val id = dao.insertDownload(entity)
            val settings = _concurrencySettings.value
            DownloadForegroundService.startOrResume(
                context = application,
                downloadId = id,
                maxConcurrent = settings.maxDownloads,
                workers = settings.workers
            )
        }
    }

    fun startTorrentFileDownload(torrentBytes: ByteArray, displayName: String) {
        repoScope.launch {
            val torrentsDir = File(application.filesDir, "torrents").apply { mkdirs() }
            val torrentFile = File(torrentsDir, "${System.currentTimeMillis()}.torrent")
            torrentFile.writeBytes(torrentBytes)

            val safeName = DownloadStorageProvider.sanitizeDisplayName(
                displayName.ifBlank { "Torrent Download" }
            )

            val entity = DownloadEntity(
                url = torrentFile.absolutePath,
                displayName = safeName,
                destinationUri = null,
                mimeType = "application/x-bittorrent",
                totalBytes = 0L,
                etag = null,
                lastModified = null,
                isResumable = true,
                type = DownloadType.TORRENT,
                state = DownloadState.QUEUED,
                sourcePageUrl = null,
                headersJson = null
            )

            val id = dao.insertDownload(entity)
            val settings = _concurrencySettings.value
            DownloadForegroundService.startOrResume(
                context = application,
                downloadId = id,
                maxConcurrent = settings.maxDownloads,
                workers = settings.workers
            )
        }
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
        } else if (download.type == DownloadType.DASH) {
            val workDir = File(application.cacheDir, "dash_$id")
            workDir.deleteRecursively()
        } else if (download.type == DownloadType.TORRENT) {
            val stagingDir = File(application.filesDir, "torrent_staging/$id")
            stagingDir.deleteRecursively()
            dao.updateTorrentProgress(id, 0L, 0L, 0)
            dao.deleteDownloadFiles(id)
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
        if (deleteFile) {
            val files = dao.getDownloadFiles(id)
            if (files.isNotEmpty()) {
                files.forEach { storage.delete(it.uri) }
            } else if (entity?.destinationUri != null) {
                storage.delete(entity.destinationUri)
            }
        }
        if (entity?.type == DownloadType.HLS) {
            val workDir = File(application.cacheDir, "hls_$id")
            workDir.deleteRecursively()
        } else if (entity?.type == DownloadType.DASH) {
            val workDir = File(application.cacheDir, "dash_$id")
            workDir.deleteRecursively()
        } else if (entity?.type == DownloadType.TORRENT) {
            val stagingDir = File(application.filesDir, "torrent_staging/$id")
            stagingDir.deleteRecursively()
            val sourceFile = File(entity.url)
            if (sourceFile.exists() && sourceFile.parentFile?.name == "torrents") {
                sourceFile.delete()
            }
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
            val okHeaders = media.headers.toOkHttpHeaders()

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

    fun updateConcurrency(
        maxDownloads: Int,
        workers: Int,
        bandwidthLimit: Long,
        wifiOnly: Boolean,
        customDownloadUri: String? = _concurrencySettings.value.customDownloadUri
    ) {
        val maxD = maxDownloads.coerceIn(1, 4)
        val w = workers.coerceIn(1, 8)
        val limit = bandwidthLimit.coerceAtLeast(0L)
        _concurrencySettings.value = ConcurrencySettings(maxD, w, limit, wifiOnly, customDownloadUri)
        prefs.edit()
            .putInt(AppPreferences.PREF_MAX_CONCURRENT_DOWNLOADS, maxD)
            .putInt(AppPreferences.PREF_WORKERS_PER_DOWNLOAD, w)
            .putLong(AppPreferences.PREF_BANDWIDTH_LIMIT_BYTES, limit)
            .putBoolean(AppPreferences.PREF_WIFI_ONLY, wifiOnly)
            .apply {
                if (customDownloadUri != null) {
                    putString(AppPreferences.PREF_CUSTOM_DOWNLOAD_URI, customDownloadUri)
                } else {
                    remove(AppPreferences.PREF_CUSTOM_DOWNLOAD_URI)
                }
            }
            .apply()
        DownloadForegroundService.updateConcurrency(application, maxD, w, limit, wifiOnly)
    }
}
