package com.znarfm.siphon.ui.browser

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.znarfm.siphon.core.adblock.AdblockEngine
import com.znarfm.siphon.core.adblock.AdblockSettings
import com.znarfm.siphon.core.common.FormatUtils
import com.znarfm.siphon.core.sniffer.MediaSnifferClient
import com.znarfm.siphon.core.sniffer.SniffedMedia
import com.znarfm.siphon.data.db.BookmarkEntity
import com.znarfm.siphon.data.db.DownloadEntity
import com.znarfm.siphon.data.db.HistoryEntity
import com.znarfm.siphon.data.prefs.AppPreferences
import com.znarfm.siphon.data.repository.BrowserPreferences
import com.znarfm.siphon.data.repository.BrowserRepository
import com.znarfm.siphon.data.repository.DownloadRepository
import com.znarfm.siphon.ui.theme.AppColorSeed
import com.znarfm.siphon.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class BrowserViewModel(
    val browserRepository: BrowserRepository,
    val downloadRepository: DownloadRepository
) : ViewModel() {

    val adblockEngine: AdblockEngine = browserRepository.adblockEngine

    private val initialTab = BrowserTab()
    private val _browserTabs = MutableStateFlow<List<BrowserTab>>(listOf(initialTab))
    val browserTabs = _browserTabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId = _activeTabId.asStateFlow()

    private val tabSnifferClients = ConcurrentHashMap<String, MediaSnifferClient>()

    private val _rebindTarget = MutableStateFlow<DownloadEntity?>(null)
    val rebindTarget: StateFlow<DownloadEntity?> = _rebindTarget.asStateFlow()

    fun getSnifferClientForTab(tabId: String): MediaSnifferClient {
        val tabIncognito = _browserTabs.value.firstOrNull { it.id == tabId }?.isIncognito == true
        return synchronized(tabSnifferClients) {
            tabSnifferClients.getOrPut(tabId) {
                MediaSnifferClient(
                    adblockEngine = adblockEngine,
                    dao = downloadRepository.dao,
                    storage = downloadRepository.storage,
                    tabId = tabId,
                    onAdBlocked = { _, _ -> recordAdBlocked(tabId) }
                ).apply {
                    isIncognito = tabIncognito
                    onMagnetLinkClicked = { uri ->
                        showMagnetDownloadPopup(uri)
                    }
                    viewModelScope.launch {
                        rebindSuccessEvent.collect { downloadId ->
                            _rebindTarget.value = null
                            downloadRepository.resumeDownload(downloadId)
                        }
                    }
                }
            }.also { it.isIncognito = tabIncognito }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val detectedMedia: StateFlow<List<SniffedMedia>> = _activeTabId
        .flatMapLatest { tabId ->
            getSnifferClientForTab(tabId).detectedMedia
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pendingMagnetUri = MutableStateFlow<String?>(null)
    val pendingMagnetUri: StateFlow<String?> = _pendingMagnetUri.asStateFlow()

    val bookmarks: StateFlow<List<BookmarkEntity>> = browserRepository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = browserRepository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val browserPreferences: StateFlow<BrowserPreferences> = browserRepository.browserPreferences

    private val _browserUrl = MutableStateFlow(NEW_TAB_URL)
    val browserUrl = _browserUrl.asStateFlow()

    private val _tabThumbnails = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val tabThumbnails: StateFlow<Map<String, ImageBitmap>> = _tabThumbnails.asStateFlow()

    private val _lastClosedTab = MutableStateFlow<BrowserTab?>(null)
    val lastClosedTab: StateFlow<BrowserTab?> = _lastClosedTab.asStateFlow()
    private var lastClosedTabIndex: Int = -1

    val isCurrentUrlBookmarked: StateFlow<Boolean> = browserRepository.isUrlBookmarked(_browserUrl)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val adblockRuleCount: StateFlow<Int> = browserRepository.adblockRuleCount
    val adblockSettings: StateFlow<AdblockSettings> = browserRepository.adblockSettings
    val isAdBlockEnabled: StateFlow<Boolean> = browserRepository.isAdBlockEnabled

    private val _blockedCountsByTab = MutableStateFlow<Map<String, Int>>(emptyMap())
    val blockedCountsByTab: StateFlow<Map<String, Int>> = _blockedCountsByTab.asStateFlow()

    private val _isSnifferSheetOpen = MutableStateFlow(false)
    val isSnifferSheetOpen = _isSnifferSheetOpen.asStateFlow()

    private val prefs = AppPreferences.prefs(browserRepository.application)

    private val _themeMode = MutableStateFlow(
        runCatching {
            AppThemeMode.valueOf(
                prefs.getString("pref_theme_mode", AppThemeMode.SYSTEM.name)
                    ?: AppThemeMode.SYSTEM.name
            )
        }.getOrDefault(AppThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _colorSeed = MutableStateFlow(
        runCatching {
            AppColorSeed.valueOf(
                prefs.getString("pref_color_seed", AppColorSeed.DYNAMIC.name)
                    ?: AppColorSeed.DYNAMIC.name
            )
        }.getOrDefault(AppColorSeed.DYNAMIC)
    )
    val colorSeed: StateFlow<AppColorSeed> = _colorSeed.asStateFlow()

    var onRequestDomScan: (() -> Unit)? = null
    var onRequestPageReload: (() -> Unit)? = null

    fun showMagnetDownloadPopup(uri: String) {
        _pendingMagnetUri.value = uri
    }

    fun dismissMagnetDownloadPopup() {
        _pendingMagnetUri.value = null
    }

    fun startMagnetDownload(magnetUri: String) {
        _pendingMagnetUri.value = null
        viewModelScope.launch {
            downloadRepository.startMagnetDownload(magnetUri)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("pref_theme_mode", mode.name).apply()
    }

    fun setColorSeed(seed: AppColorSeed) {
        _colorSeed.value = seed
        prefs.edit().putString("pref_color_seed", seed.name).apply()
    }

    fun toggleWebDarkMode() {
        val next = !browserPreferences.value.webDarkMode
        browserRepository.setWebDarkMode(next)
    }

    fun toggleDesktopMode() {
        val next = !browserPreferences.value.desktopMode
        browserRepository.setDesktopMode(next)
    }

    fun setBrowserUrl(url: String) {
        _browserUrl.value = url
        val curId = _activeTabId.value
        getSnifferClientForTab(curId).updateCurrentPageUrl(url)
        _browserTabs.value = _browserTabs.value.map {
            if (it.id == curId) it.copy(url = url) else it
        }
    }

    fun openNewBrowserTab(
        url: String = NEW_TAB_URL,
        isIncognito: Boolean = false,
        switchImmediately: Boolean = true
    ) {
        val newTab = BrowserTab(url = url, isIncognito = isIncognito)
        _browserTabs.value = _browserTabs.value + newTab
        getSnifferClientForTab(newTab.id).updateCurrentPageUrl(url)
        if (switchImmediately) {
            _activeTabId.value = newTab.id
            _browserUrl.value = url
        }
    }

    fun switchBrowserTab(tabId: String) {
        val target = _browserTabs.value.firstOrNull { it.id == tabId } ?: return
        _activeTabId.value = tabId
        _browserUrl.value = target.url
        getSnifferClientForTab(tabId).updateCurrentPageUrl(target.url)
    }

    fun switchToPreviousTab() {
        findAdjacentTabId(_browserTabs.value, _activeTabId.value, delta = -1)?.let { targetId ->
            switchBrowserTab(targetId)
        }
    }

    fun switchToNextTab() {
        findAdjacentTabId(_browserTabs.value, _activeTabId.value, delta = +1)?.let { targetId ->
            switchBrowserTab(targetId)
        }
    }

    fun updateTabThumbnail(tabId: String, bitmap: ImageBitmap) {
        _tabThumbnails.value = _tabThumbnails.value + (tabId to bitmap)
    }

    fun closeBrowserTab(tabId: String) {
        val currentList = _browserTabs.value
        val closedIndex = currentList.indexOfFirst { it.id == tabId }
        if (closedIndex == -1) return

        val closedTab = currentList[closedIndex]
        _lastClosedTab.value = closedTab
        lastClosedTabIndex = closedIndex
        _tabThumbnails.update { it - tabId }
        _blockedCountsByTab.update { it - tabId }
        synchronized(tabSnifferClients) { tabSnifferClients.remove(tabId) }?.destroy()

        if (currentList.size <= 1) {
            val resetTab = BrowserTab(url = NEW_TAB_URL, title = "New Tab")
            _browserTabs.value = listOf(resetTab)
            _activeTabId.value = resetTab.id
            _browserUrl.value = resetTab.url
            getSnifferClientForTab(resetTab.id).updateCurrentPageUrl(resetTab.url)
            return
        }
        val remaining = currentList.filter { it.id != tabId }
        _browserTabs.value = remaining
        if (_activeTabId.value == tabId) {
            val nextTab = remaining.last()
            _activeTabId.value = nextTab.id
            _browserUrl.value = nextTab.url
            getSnifferClientForTab(nextTab.id).updateCurrentPageUrl(nextTab.url)
        }
    }

    fun undoCloseTab() {
        val tabToRestore = _lastClosedTab.value ?: return
        val currentList = _browserTabs.value
        val insertIndex = lastClosedTabIndex.coerceIn(0, currentList.size)
        val updatedList = currentList.toMutableList().apply {
            add(insertIndex, tabToRestore)
        }
        _browserTabs.value = updatedList
        _activeTabId.value = tabToRestore.id
        _browserUrl.value = tabToRestore.url
        getSnifferClientForTab(tabToRestore.id).updateCurrentPageUrl(tabToRestore.url)
        _lastClosedTab.value = null
        lastClosedTabIndex = -1
    }

    fun dismissUndoCloseTab() {
        _lastClosedTab.value = null
        lastClosedTabIndex = -1
    }

    fun closeAllTabs(incognitoOnly: Boolean = false) {
        val currentList = _browserTabs.value
        if (incognitoOnly) {
            val closedIds = currentList.filter { it.isIncognito }.map { it.id }.toSet()
            _tabThumbnails.update { it - closedIds }
            _blockedCountsByTab.update { it - closedIds }
            val removedClients = synchronized(tabSnifferClients) {
                closedIds.mapNotNull { tabSnifferClients.remove(it) }
            }
            removedClients.forEach { it.destroy() }
            val remaining = currentList.filter { !it.isIncognito }
            if (remaining.isEmpty()) {
                val freshTab = BrowserTab(url = NEW_TAB_URL, title = "New Tab")
                _browserTabs.value = listOf(freshTab)
                _activeTabId.value = freshTab.id
                _browserUrl.value = freshTab.url
                getSnifferClientForTab(freshTab.id).updateCurrentPageUrl(freshTab.url)
            } else {
                _browserTabs.value = remaining
                if (remaining.none { it.id == _activeTabId.value }) {
                    val nextTab = remaining.last()
                    _activeTabId.value = nextTab.id
                    _browserUrl.value = nextTab.url
                    getSnifferClientForTab(nextTab.id).updateCurrentPageUrl(nextTab.url)
                }
            }
        } else {
            _tabThumbnails.value = emptyMap()
            _blockedCountsByTab.value = emptyMap()
            val removedClients = synchronized(tabSnifferClients) {
                tabSnifferClients.values.toList().also { tabSnifferClients.clear() }
            }
            removedClients.forEach { it.destroy() }
            val freshTab = BrowserTab(url = NEW_TAB_URL, title = "New Tab")
            _browserTabs.value = listOf(freshTab)
            _activeTabId.value = freshTab.id
            _browserUrl.value = freshTab.url
            getSnifferClientForTab(freshTab.id).updateCurrentPageUrl(freshTab.url)
        }
    }

    fun updateActiveTabMeta(url: String, title: String) {
        val curId = _activeTabId.value
        _browserUrl.value = url
        getSnifferClientForTab(curId).updateCurrentPageUrl(url)
        var currentTabIsIncognito = false
        _browserTabs.value = _browserTabs.value.map {
            if (it.id == curId) {
                currentTabIsIncognito = it.isIncognito
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

        if (!currentTabIsIncognito && url.isNotBlank() && !url.startsWith("about:") && !url.startsWith("chrome:")) {
            viewModelScope.launch(Dispatchers.IO) {
                browserRepository.addHistory(
                    url = url,
                    title = title.ifBlank { deriveTitleFromUrl(url) }
                )
            }
        }
    }

    fun toggleBookmark(url: String, title: String) {
        viewModelScope.launch {
            browserRepository.toggleBookmark(url, title)
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            browserRepository.deleteBookmark(id)
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            browserRepository.deleteHistory(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            browserRepository.clearHistory()
        }
    }

    fun recordAdBlocked(tabId: String) {
        if (tabId.isBlank()) return
        _blockedCountsByTab.update { current ->
            current + (tabId to ((current[tabId] ?: 0) + 1))
        }
    }

    fun resetTabBlockedCount(tabId: String) {
        if (tabId.isBlank()) return
        _blockedCountsByTab.update { current ->
            if (current.containsKey(tabId)) current - tabId else current
        }
    }

    fun toggleAdBlock() {
        browserRepository.toggleAdBlock()
    }

    fun toggleSiteAllowlist(urlOrHost: String) {
        val host = AdblockEngine.extractHostFromUrl(urlOrHost)
            ?: AdblockEngine.normalizeHost(urlOrHost)
            ?: return
        browserRepository.toggleSiteAllowlist(host)
    }

    fun isSiteAllowlisted(urlOrHost: String): Boolean {
        val host = AdblockEngine.extractHostFromUrl(urlOrHost)
            ?: AdblockEngine.normalizeHost(urlOrHost)
            ?: return false
        val allowed = adblockSettings.value.allowedSites
        if (allowed.isEmpty()) return false
        var current = host
        while (current.contains('.')) {
            if (allowed.contains(current)) return true
            val dotIndex = current.indexOf('.')
            if (dotIndex == -1) break
            current = current.substring(dotIndex + 1)
        }
        return allowed.contains(current)
    }

    fun toggleFilterList(id: String, enabled: Boolean) {
        browserRepository.toggleList(id, enabled)
    }

    fun addCustomFilterList(name: String, url: String) {
        browserRepository.addCustomList(name, url)
    }

    fun removeCustomFilterList(id: String) {
        browserRepository.removeCustomList(id)
    }

    fun updateCustomUserRules(rules: String) {
        browserRepository.updateCustomUserRules(rules)
    }

    fun refreshAdblockSubscriptions(
        onError: ((Throwable) -> Unit)? = null,
        onResult: (Int, Int) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val result = browserRepository.refreshAdblockSubscriptions()
            result.onSuccess { updatedSettings ->
                val enabledCount = updatedSettings.enabledLists.count { it.isEnabled }
                onResult(adblockRuleCount.value, enabledCount)
            }.onFailure { err ->
                onError?.invoke(err)
            }
        }
    }

    fun clearDetectedMedia() {
        getSnifferClientForTab(_activeTabId.value).clearDetectedMedia()
    }

    fun removeDetectedMedia(id: String) {
        getSnifferClientForTab(_activeTabId.value).removeDetectedMedia(id)
    }

    fun refreshDetectedMedia() {
        val activeClient = getSnifferClientForTab(_activeTabId.value)
        activeClient.clearDetectedMedia()
        onRequestPageReload?.invoke()
        onRequestDomScan?.invoke()
        val curUrl = _browserUrl.value
        if (curUrl.isNotBlank()) {
            activeClient.inspectCandidate(curUrl)
        }
    }

    fun openSnifferSheet() {
        _isSnifferSheetOpen.value = true
    }

    fun closeSnifferSheet() {
        _isSnifferSheetOpen.value = false
    }

    fun loadUrlAndOpenSniffer(url: String) {
        clearDetectedMedia()
        setBrowserUrl(url)
        getSnifferClientForTab(_activeTabId.value).inspectCandidate(url, forceInspect = true)
        openSnifferSheet()
    }

    fun startDownload(
        media: SniffedMedia,
        customDisplayName: String? = null,
        selectedVariantUrl: String? = null
    ) {
        viewModelScope.launch {
            downloadRepository.startDownload(
                media = media,
                variantUrl = selectedVariantUrl,
                customFilename = customDisplayName
            )
            _isSnifferSheetOpen.value = false
        }
    }

    fun downloadDirectMedia(url: String) {
        startStandardDownload(
            url = url,
            userAgent = null,
            contentDisposition = null,
            mimetype = "image/*",
            contentLength = -1L,
            refererUrl = _browserUrl.value,
            tabId = _activeTabId.value
        )
    }

    fun startStandardDownload(
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimetype: String? = null,
        contentLength: Long = -1L,
        refererUrl: String? = null,
        tabId: String = _activeTabId.value
    ) {
        if (url.isBlank() || url.startsWith("blob:", ignoreCase = true) || url.startsWith("data:", ignoreCase = true)) {
            return
        }
        viewModelScope.launch {
            val snifferClient = getSnifferClientForTab(tabId)
            val isIncognito = snifferClient.isIncognito
            val effectiveReferer = refererUrl?.takeIf { it.startsWith("http", ignoreCase = true) }
                ?: _browserUrl.value.takeIf { it.startsWith("http", ignoreCase = true) }
                ?: ""
            val effectiveUa = userAgent?.takeIf { it.isNotBlank() } ?: snifferClient.defaultUserAgent

            val headers = mutableMapOf<String, String>()
            if (effectiveReferer.isNotBlank()) {
                headers["Referer"] = effectiveReferer
            }
            if (!effectiveUa.isNullOrBlank()) {
                headers["User-Agent"] = effectiveUa
            }
            if (!isIncognito) {
                val cookies = runCatching { android.webkit.CookieManager.getInstance().getCookie(url) }.getOrNull()
                if (!cookies.isNullOrBlank()) {
                    headers["Cookie"] = cookies
                }
            }

            val parsedDispName = com.znarfm.siphon.core.network.MetadataProber.parseFilenameFromDisposition(contentDisposition)
            val guessedName = runCatching {
                android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
            }.getOrNull()?.takeIf { it.isNotBlank() && it != "downloadfile.bin" }
            val fileName = parsedDispName
                ?: guessedName
                ?: com.znarfm.siphon.core.sniffer.SnifferUrlClassifier.extractDisplayNameFromUrl(
                    url = url,
                    fallback = "download_${System.currentTimeMillis()}"
                )

            val resolvedMime = mimetype?.substringBefore(';')?.trim()?.takeIf { it.isNotBlank() }
                ?: "application/octet-stream"

            val sniffed = SniffedMedia(
                id = java.util.UUID.randomUUID().toString(),
                tabId = tabId,
                url = url,
                displayName = fileName,
                mimeType = resolvedMime,
                totalBytes = contentLength,
                mediaType = com.znarfm.siphon.data.db.DownloadType.HTTP,
                sourcePageUrl = effectiveReferer,
                headers = headers
            )
            downloadRepository.startDownload(sniffed)
        }
    }

    fun rebindDownload(
        target: DownloadEntity,
        media: SniffedMedia,
        selectedVariantUrl: String? = null
    ) {
        viewModelScope.launch {
            val success = downloadRepository.rebindDownload(target, media, selectedVariantUrl)
            if (success) {
                cancelRebindFlow()
                _isSnifferSheetOpen.value = false
            }
        }
    }

    fun setSearchEngine(engine: String) {
        browserRepository.setSearchEngine(engine)
    }

    fun startRebindFlow(download: DownloadEntity) {
        _rebindTarget.value = download
        getSnifferClientForTab(_activeTabId.value).setRebindTarget(download)
        val sourceUrl = download.sourcePageUrl?.takeIf { it.isNotBlank() } ?: "https://www.google.com"
        setBrowserUrl(sourceUrl)
    }

    fun cancelRebindFlow() {
        _rebindTarget.value = null
        tabSnifferClients.values.forEach { it.setRebindTarget(null) }
    }

    private fun deriveTitleFromUrl(url: String): String {
        return FormatUtils.extractHostDisplay(url).ifBlank { "Tab" }
    }

    override fun onCleared() {
        super.onCleared()
        val clients = synchronized(tabSnifferClients) {
            tabSnifferClients.values.toList().also { tabSnifferClients.clear() }
        }
        clients.forEach { it.destroy() }
        _tabThumbnails.value = emptyMap()
    }
}

internal fun findAdjacentTabId(
    tabs: List<BrowserTab>,
    activeTabId: String,
    delta: Int
): String? {
    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: return null
    val modeTabs = tabs.filter { it.isIncognito == activeTab.isIncognito }
    val currentIndex = modeTabs.indexOfFirst { it.id == activeTabId }
    if (currentIndex == -1) return null
    val targetIndex = currentIndex + delta
    return modeTabs.getOrNull(targetIndex)?.id
}
