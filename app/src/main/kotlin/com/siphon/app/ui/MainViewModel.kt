package com.siphon.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.siphon.app.core.adblock.FastHostFilter
import com.siphon.app.core.network.HeadersCodec
import com.siphon.app.core.service.DownloadForegroundService
import com.siphon.app.core.sniffer.MediaSnifferClient
import com.siphon.app.core.sniffer.SniffedMedia
import com.siphon.app.core.storage.DownloadStorageProvider
import com.siphon.app.data.db.ChunkEntity
import com.siphon.app.data.db.DownloadDao
import com.siphon.app.data.db.DownloadEntity
import com.siphon.app.data.db.DownloadState
import com.siphon.app.data.db.DownloadType
import com.siphon.app.data.db.SiphonDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.Headers
import java.io.File

enum class AppTab {
    BROWSER,
    QUEUE
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SiphonDatabase.get(application)
    val dao: DownloadDao = db.downloadDao()
    val storage = DownloadStorageProvider(application)

    val hostFilter = FastHostFilter().apply {
        loadFromAssets(application)
    }

    val snifferClient = MediaSnifferClient(
        hostFilter = hostFilter,
        dao = dao,
        storage = storage
    )

    val downloads: StateFlow<List<DownloadEntity>> = dao.observeDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val detectedMedia: StateFlow<List<SniffedMedia>> = snifferClient.detectedMedia
    val rebindTarget: StateFlow<DownloadEntity?> = snifferClient.rebindTarget

    private val _currentTab = MutableStateFlow(AppTab.BROWSER)
    val currentTab = _currentTab.asStateFlow()

    private val initialTab = com.siphon.app.ui.browser.BrowserTab()
    private val _browserTabs = MutableStateFlow<List<com.siphon.app.ui.browser.BrowserTab>>(listOf(initialTab))
    val browserTabs = _browserTabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId = _activeTabId.asStateFlow()

    private val _browserUrl = MutableStateFlow("https://en.wikipedia.org")
    val browserUrl = _browserUrl.asStateFlow()

    private val _isSnifferSheetOpen = MutableStateFlow(false)
    val isSnifferSheetOpen = _isSnifferSheetOpen.asStateFlow()

    private val _isAdBlockEnabled = MutableStateFlow(true)
    val isAdBlockEnabled = _isAdBlockEnabled.asStateFlow()

    private val prefs = application.getSharedPreferences(
        DownloadForegroundService.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _maxConcurrentDownloads = MutableStateFlow(
        prefs.getInt(DownloadForegroundService.PREF_MAX_CONCURRENT_DOWNLOADS, 2).coerceIn(1, 4)
    )
    val maxConcurrentDownloads = _maxConcurrentDownloads.asStateFlow()

    private val _workersPerDownload = MutableStateFlow(
        prefs.getInt(DownloadForegroundService.PREF_WORKERS_PER_DOWNLOAD, 6).coerceIn(1, 8)
    )
    val workersPerDownload = _workersPerDownload.asStateFlow()

    private val _chunksMap = MutableStateFlow<Map<Long, List<ChunkEntity>>>(emptyMap())
    val chunksMap = _chunksMap.asStateFlow()

    init {
        viewModelScope.launch {
            snifferClient.rebindSuccessEvent.collect { downloadId ->
                // Automatically resume download when 403 link successfully rebinds
                resumeDownload(downloadId)
            }
        }

        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            downloads.flatMapLatest { list ->
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
            }.collect { map ->
                _chunksMap.value = map
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setBrowserUrl(url: String) {
        _browserUrl.value = url
        val curId = _activeTabId.value
        _browserTabs.value = _browserTabs.value.map {
            if (it.id == curId) it.copy(url = url) else it
        }
    }

    fun openNewBrowserTab(url: String = "https://en.wikipedia.org") {
        val newTab = com.siphon.app.ui.browser.BrowserTab(url = url)
        _browserTabs.value = _browserTabs.value + newTab
        _activeTabId.value = newTab.id
        _browserUrl.value = url
        _currentTab.value = AppTab.BROWSER
    }

    fun switchBrowserTab(tabId: String) {
        val target = _browserTabs.value.firstOrNull { it.id == tabId } ?: return
        _activeTabId.value = tabId
        _browserUrl.value = target.url
        _currentTab.value = AppTab.BROWSER
    }

    fun closeBrowserTab(tabId: String) {
        val currentList = _browserTabs.value
        if (currentList.size <= 1) {
            // Reset the only tab to default home
            val resetTab = com.siphon.app.ui.browser.BrowserTab(id = tabId, url = "https://en.wikipedia.org", title = "New Tab")
            _browserTabs.value = listOf(resetTab)
            _activeTabId.value = tabId
            _browserUrl.value = resetTab.url
            return
        }
        val remaining = currentList.filter { it.id != tabId }
        _browserTabs.value = remaining
        if (_activeTabId.value == tabId) {
            val nextTab = remaining.last()
            _activeTabId.value = nextTab.id
            _browserUrl.value = nextTab.url
        }
    }

    fun updateActiveTabMeta(url: String, title: String) {
        val curId = _activeTabId.value
        _browserUrl.value = url
        _browserTabs.value = _browserTabs.value.map {
            if (it.id == curId) {
                // Only replace title if the new title is non-blank; otherwise preserve existing title
                val cleanTitle = title.trim()
                val finalTitle = if (cleanTitle.isNotEmpty()) {
                    cleanTitle
                } else if (it.title.isNotEmpty() && it.title != "New Tab") {
                    it.title
                } else {
                    deriveTitleFromUrl(url)
                }
                it.copy(url = url, title = finalTitle)
            } else it
        }
    }

    private fun deriveTitleFromUrl(url: String): String {
        return runCatching {
            val uri = Uri.parse(url)
            uri.host?.removePrefix("www.")?.ifBlank { null }
        }.getOrNull() ?: "Tab"
    }

    fun toggleAdBlock() {
        val next = !_isAdBlockEnabled.value
        _isAdBlockEnabled.value = next
        hostFilter.isEnabled = next
    }

    fun openSnifferSheet() {
        _isSnifferSheetOpen.value = true
    }

    fun closeSnifferSheet() {
        _isSnifferSheetOpen.value = false
    }

    fun setConcurrency(maxDownloads: Int, workers: Int) {
        val maxD = maxDownloads.coerceIn(1, 4)
        val w = workers.coerceIn(1, 8)
        _maxConcurrentDownloads.value = maxD
        _workersPerDownload.value = w
        prefs.edit()
            .putInt(DownloadForegroundService.PREF_MAX_CONCURRENT_DOWNLOADS, maxD)
            .putInt(DownloadForegroundService.PREF_WORKERS_PER_DOWNLOAD, w)
            .apply()
        DownloadForegroundService.updateConcurrency(getApplication(), maxD, w)
    }

    fun startDownload(
        media: SniffedMedia,
        customDisplayName: String? = null,
        selectedVariantUrl: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadUrl = selectedVariantUrl ?: media.url
            val name = customDisplayName ?: media.displayName
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
            DownloadForegroundService.startOrResume(
                context = getApplication(),
                downloadId = id,
                maxConcurrent = _maxConcurrentDownloads.value,
                workers = _workersPerDownload.value
            )
            _isSnifferSheetOpen.value = false
        }
    }

    fun resumeDownload(downloadId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val download = dao.getDownload(downloadId) ?: return@launch
            if (download.state == DownloadState.AUTH_EXPIRED) {
                startRebindFlow(download)
            } else {
                DownloadForegroundService.startOrResume(
                    context = getApplication(),
                    downloadId = downloadId,
                    maxConcurrent = _maxConcurrentDownloads.value,
                    workers = _workersPerDownload.value
                )
            }
        }
    }

    fun rebindDownload(
        target: DownloadEntity,
        media: SniffedMedia,
        selectedVariantUrl: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val candidateUrl = selectedVariantUrl ?: media.url
            val headersJson = HeadersCodec.encode(media.headers)

            if (media.mediaType == DownloadType.HLS || target.type == DownloadType.HLS) {
                // Invalidate cached playlists and variant URLs in workDir so fresh playlist is fetched on resume
                val workDir = File(getApplication<Application>().cacheDir, "hls_${target.id}")
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
                cancelRebindFlow()
                _isSnifferSheetOpen.value = false
                DownloadForegroundService.startOrResume(
                    context = getApplication(),
                    downloadId = target.id,
                    maxConcurrent = _maxConcurrentDownloads.value,
                    workers = _workersPerDownload.value
                )
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
                            val probeResult = snifferClient.metadataProber.verifyRefreshedUrl(
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
                        return@launch
                    }
                }

                dao.rebindExpiredLink(
                    id = target.id,
                    newUrl = candidateUrl,
                    newEtag = strongEtag,
                    newHeadersJson = headersJson
                )
                cancelRebindFlow()
                _isSnifferSheetOpen.value = false
                DownloadForegroundService.startOrResume(
                    context = getApplication(),
                    downloadId = target.id,
                    maxConcurrent = _maxConcurrentDownloads.value,
                    workers = _workersPerDownload.value
                )
            }
        }
    }

    fun pauseDownload(downloadId: Long) {
        DownloadForegroundService.pause(getApplication(), downloadId)
    }

    fun cancelDownload(downloadId: Long) {
        DownloadForegroundService.cancel(getApplication(), downloadId)
    }

    fun deleteDownload(downloadId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = dao.getDownload(downloadId)
            cancelDownload(downloadId)
            if (entity?.destinationUri != null) {
                storage.delete(entity.destinationUri)
            }
            if (entity?.type == DownloadType.HLS) {
                val workDir = File(getApplication<Application>().cacheDir, "hls_$downloadId")
                workDir.deleteRecursively()
            }
            dao.deleteDownload(downloadId)
        }
    }

    fun startRebindFlow(download: DownloadEntity) {
        snifferClient.setRebindTarget(download)
        val sourceUrl = download.sourcePageUrl?.takeIf { it.isNotBlank() } ?: "https://www.google.com"
        _browserUrl.value = sourceUrl
        _currentTab.value = AppTab.BROWSER
    }

    fun cancelRebindFlow() {
        snifferClient.setRebindTarget(null)
    }
}
