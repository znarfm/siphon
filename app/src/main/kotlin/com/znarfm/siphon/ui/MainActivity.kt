package com.znarfm.siphon.ui

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.znarfm.siphon.SiphonApp
import com.znarfm.siphon.data.db.DownloadState
import com.znarfm.siphon.ui.browser.BookmarksScreen
import com.znarfm.siphon.ui.browser.BrowserScreen
import com.znarfm.siphon.ui.browser.BrowserViewModel
import com.znarfm.siphon.ui.browser.HistoryScreen
import com.znarfm.siphon.ui.browser.MagnetDownloadDialog
import com.znarfm.siphon.ui.browser.NEW_TAB_URL
import com.znarfm.siphon.ui.browser.TabSwitcherScreen
import com.znarfm.siphon.ui.queue.QueueScreen
import com.znarfm.siphon.ui.queue.QueueViewModel
import com.znarfm.siphon.ui.settings.AppearanceSettingsScreen
import com.znarfm.siphon.ui.settings.BrowserSearchSettingsScreen
import com.znarfm.siphon.ui.settings.DownloadSettingsScreen
import com.znarfm.siphon.ui.settings.PrivacyAdblockSettingsScreen
import com.znarfm.siphon.ui.settings.SettingsHubScreen
import com.znarfm.siphon.ui.sniffer.SnifferSheet
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.SiphonTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppScreen {
    BROWSER,
    QUEUE,
    TAB_SWITCHER,
    BOOKMARKS,
    HISTORY,
    SETTINGS,
    SETTINGS_BROWSER,
    SETTINGS_APPEARANCE,
    SETTINGS_ADBLOCK,
    SETTINGS_DOWNLOADS
}

class MainActivity : ComponentActivity() {

    private val browserViewModel: BrowserViewModel by viewModels {
        (application as SiphonApp).container.viewModelFactory
    }

    private val queueViewModel: QueueViewModel by viewModels {
        (application as SiphonApp).container.viewModelFactory
    }

    private val downloadRepository by lazy {
        (application as SiphonApp).container.downloadRepository
    }

    private val _currentScreen = MutableStateFlow(AppScreen.BROWSER)
    val currentScreen = _currentScreen.asStateFlow()

    private val _pendingClipboardLink = MutableStateFlow<String?>(null)
    val pendingClipboardLink: StateFlow<String?> = _pendingClipboardLink.asStateFlow()

    private var lastPromptedClipboardLink: String? = null

    fun selectScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun dismissClipboardPrompt() {
        _pendingClipboardLink.value = null
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()
        handleIncomingIntent(intent)

        setContent {
            val themeMode by browserViewModel.themeMode.collectAsStateWithLifecycle()
            val colorSeed by browserViewModel.colorSeed.collectAsStateWithLifecycle()
            val screen by currentScreen.collectAsStateWithLifecycle()
            val clipboardLink by pendingClipboardLink.collectAsStateWithLifecycle()
            SiphonTheme(themeMode = themeMode, colorSeed = colorSeed) {
                MainContent(
                    browserViewModel = browserViewModel,
                    queueViewModel = queueViewModel,
                    currentScreen = screen,
                    onSelectScreen = { selectScreen(it) },
                    pendingClipboardLink = clipboardLink,
                    onAcceptClipboardLink = { link ->
                        dismissClipboardPrompt()
                        if (link.startsWith("magnet:", ignoreCase = true)) {
                            queueMagnetDownload(link)
                        } else {
                            selectScreen(AppScreen.BROWSER)
                            browserViewModel.loadUrlAndOpenSniffer(link)
                        }
                    },
                    onDismissClipboardLink = { dismissClipboardPrompt() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipText = clipboard?.primaryClip?.let { clip ->
                if (clip.itemCount > 0) clip.getItemAt(0).text?.toString() else null
            }
            val link = extractLinkFromText(clipText)
            if (!link.isNullOrBlank() && link != lastPromptedClipboardLink) {
                lastPromptedClipboardLink = link
                _pendingClipboardLink.value = link
            }
        }
    }

    private fun queueMagnetDownload(magnetUri: String) {
        selectScreen(AppScreen.QUEUE)
        lifecycleScope.launch {
            downloadRepository.startMagnetDownload(magnetUri)
        }
        Toast.makeText(this, "Magnet download queued", Toast.LENGTH_SHORT).show()
    }

    private fun queueTorrentUri(uri: Uri) {
        val fileName = resolveFileName(uri) ?: "download.torrent"
        lifecycleScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes != null && bytes.isNotEmpty()) {
                downloadRepository.startTorrentFileDownload(bytes, fileName)
                selectScreen(AppScreen.QUEUE)
                Toast.makeText(this@MainActivity, "Torrent queued", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Failed to read torrent file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resolveFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            runCatching {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            return cursor.getString(idx)
                        }
                    }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            "com.znarfm.siphon.action.SHORTCUT_INCOGNITO" -> {
                selectScreen(AppScreen.BROWSER)
                browserViewModel.openNewBrowserTab(NEW_TAB_URL, isIncognito = true, switchImmediately = true)
                return
            }
            "com.znarfm.siphon.action.SHORTCUT_QUEUE" -> {
                selectScreen(AppScreen.QUEUE)
                return
            }
            "com.znarfm.siphon.action.SHORTCUT_PASTE_SNIFF" -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clipText = clipboard?.primaryClip?.let { clip ->
                    if (clip.itemCount > 0) clip.getItemAt(0).text?.toString() else null
                }
                val link = extractLinkFromText(clipText)
                when {
                    !link.isNullOrBlank() && link.startsWith("magnet:", ignoreCase = true) -> {
                        queueMagnetDownload(link)
                    }
                    !link.isNullOrBlank() -> {
                        selectScreen(AppScreen.BROWSER)
                        browserViewModel.loadUrlAndOpenSniffer(link)
                    }
                    else -> {
                        selectScreen(AppScreen.BROWSER)
                        browserViewModel.openSnifferSheet()
                        Toast.makeText(this, "No URL in clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
                return
            }
        }

        val link = extractLinkFromIntent(intent) ?: return
        when {
            link.startsWith("magnet:", ignoreCase = true) -> {
                queueMagnetDownload(link)
            }
            link.startsWith("content://", ignoreCase = true) || link.startsWith("file://", ignoreCase = true) -> {
                queueTorrentUri(Uri.parse(link))
            }
            else -> {
                selectScreen(AppScreen.BROWSER)
                browserViewModel.loadUrlAndOpenSniffer(link)
            }
        }
    }

    private fun extractLinkFromIntent(intent: Intent): String? {
        val data = intent.dataString
        if (!data.isNullOrBlank()) {
            val lower = data.lowercase()
            if (lower.startsWith("magnet:") ||
                lower.startsWith("http://") ||
                lower.startsWith("https://") ||
                lower.startsWith("content://") ||
                lower.startsWith("file://")
            ) {
                return data
            }
        }

        val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        if (streamUri != null) {
            val streamStr = streamUri.toString()
            if (streamStr.startsWith("content://", ignoreCase = true) ||
                streamStr.startsWith("file://", ignoreCase = true)
            ) {
                return streamStr
            }
        }

        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        extractLinkFromText(text)?.let { return it }

        val clip = intent.clipData
        if (clip != null && clip.itemCount > 0) {
            for (i in 0 until clip.itemCount) {
                val item = clip.getItemAt(i)
                val uriStr = item.uri?.toString()
                if (!uriStr.isNullOrBlank()) {
                    val lower = uriStr.lowercase()
                    if (lower.startsWith("magnet:") ||
                        lower.startsWith("http://") ||
                        lower.startsWith("https://") ||
                        lower.startsWith("content://") ||
                        lower.startsWith("file://")
                    ) {
                        return uriStr
                    }
                }
                extractLinkFromText(item.text?.toString())?.let { return it }
            }
        }

        extractLinkFromText(intent.getStringExtra(Intent.EXTRA_SUBJECT))?.let { return it }

        return null
    }

    companion object {
        private val magnetRegex = Regex("""magnet:\?[^\s<>"']+""", RegexOption.IGNORE_CASE)
        private val urlRegex = Regex("""https?://[^\s<>"'{}|\\^`]+""", RegexOption.IGNORE_CASE)

        fun extractLinkFromText(text: String?): String? {
            if (text.isNullOrBlank()) return null
            magnetRegex.find(text)?.value?.trimEnd('.', ',', ')', ']', ';', '!')?.let { return it }
            return urlRegex.find(text)?.value?.trimEnd('.', ',', ')', ']', ';', '!')
        }

        fun extractUrlFromText(text: String?): String? {
            if (text.isNullOrBlank()) return null
            return urlRegex.find(text)?.value?.trimEnd('.', ',', ')', ']', ';', '!')
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainContent(
    browserViewModel: BrowserViewModel,
    queueViewModel: QueueViewModel,
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
    pendingClipboardLink: String? = null,
    onAcceptClipboardLink: (String) -> Unit = {},
    onDismissClipboardLink: () -> Unit = {}
) {
    val context = LocalContext.current
    var isBarsVisible by remember { mutableStateOf(true) }
    var settingsSubPageOrigin by remember { mutableStateOf(AppScreen.SETTINGS) }

    val navigateBack: () -> Unit = {
        when (currentScreen) {
            AppScreen.SETTINGS_BROWSER,
            AppScreen.SETTINGS_APPEARANCE,
            AppScreen.SETTINGS_ADBLOCK,
            AppScreen.SETTINGS_DOWNLOADS -> onSelectScreen(settingsSubPageOrigin)
            else -> onSelectScreen(AppScreen.BROWSER)
        }
    }

    val browserTabs by browserViewModel.browserTabs.collectAsStateWithLifecycle()
    val activeTabId by browserViewModel.activeTabId.collectAsStateWithLifecycle()
    val browserUrl by browserViewModel.browserUrl.collectAsStateWithLifecycle()
    val tabThumbnails by browserViewModel.tabThumbnails.collectAsStateWithLifecycle()
    val lastClosedTab by browserViewModel.lastClosedTab.collectAsStateWithLifecycle()
    val detectedMedia by browserViewModel.detectedMedia.collectAsStateWithLifecycle()
    val rebindTarget by browserViewModel.rebindTarget.collectAsStateWithLifecycle()
    val isAdBlockEnabled by browserViewModel.isAdBlockEnabled.collectAsStateWithLifecycle()
    val browserPrefs by browserViewModel.browserPreferences.collectAsStateWithLifecycle()
    val themeMode by browserViewModel.themeMode.collectAsStateWithLifecycle()
    val colorSeed by browserViewModel.colorSeed.collectAsStateWithLifecycle()
    val isSnifferSheetOpen by browserViewModel.isSnifferSheetOpen.collectAsStateWithLifecycle()
    val bookmarks by browserViewModel.bookmarks.collectAsStateWithLifecycle()
    val history by browserViewModel.history.collectAsStateWithLifecycle()
    val isCurrentBookmarked by browserViewModel.isCurrentUrlBookmarked.collectAsStateWithLifecycle()
    val adblockRuleCount by browserViewModel.adblockRuleCount.collectAsStateWithLifecycle()
    val adblockSettings by browserViewModel.adblockSettings.collectAsStateWithLifecycle()
    val blockedCountsByTab by browserViewModel.blockedCountsByTab.collectAsStateWithLifecycle()
    val pendingMagnetUri by browserViewModel.pendingMagnetUri.collectAsStateWithLifecycle()

    val downloads by queueViewModel.downloads.collectAsStateWithLifecycle()
    val chunksMap by queueViewModel.chunksMap.collectAsStateWithLifecycle()
    val concurrencySettings by queueViewModel.concurrencySettings.collectAsStateWithLifecycle()
    val queueFilter by queueViewModel.filter.collectAsStateWithLifecycle()
    val queueSortOption by queueViewModel.sortOption.collectAsStateWithLifecycle()
    val queueSearchQuery by queueViewModel.searchQuery.collectAsStateWithLifecycle()
    val queueSelectedIds by queueViewModel.selectedIds.collectAsStateWithLifecycle()
    val queueIsSelectionMode by queueViewModel.isSelectionMode.collectAsStateWithLifecycle()

    val activeDownloadsCount = downloads.count {
        it.state == DownloadState.ACTIVE || it.state == DownloadState.QUEUED || it.state == DownloadState.MERGING
    }

    var backProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler(
        enabled = currentScreen != AppScreen.BROWSER && !(currentScreen == AppScreen.QUEUE && queueIsSelectionMode)
    ) { progress ->
        try {
            progress.collect { backEvent ->
                backProgress = backEvent.progress
            }
            backProgress = 0f
            navigateBack()
        } catch (e: CancellationException) {
            backProgress = 0f
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            val isBrowserVisible = currentScreen == AppScreen.BROWSER

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isBrowserVisible) 1f else 0f)
                    .zIndex(if (isBrowserVisible) 1f else 0f)
                    .pointerInput(isBrowserVisible) {
                        if (!isBrowserVisible) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
            ) {
                BrowserScreen(
                    currentUrl = browserUrl,
                    tabs = browserTabs,
                    activeTabId = activeTabId,
                    snifferClientForTab = { tabId -> browserViewModel.getSnifferClientForTab(tabId) },
                    detectedMediaCount = detectedMedia.size,
                    activeDownloadCount = activeDownloadsCount,
                    isAdBlockEnabled = isAdBlockEnabled,
                    rebindTarget = rebindTarget,
                    isBarsVisible = isBarsVisible,
                    isWebDarkMode = browserPrefs.webDarkMode,
                    isDesktopMode = browserPrefs.desktopMode,
                    themeMode = themeMode,
                    colorSeed = colorSeed,
                    isCurrentBookmarked = isCurrentBookmarked,
                    bookmarks = bookmarks,
                    history = history,
                    adblockRuleCount = adblockRuleCount,
                    blockedCountsByTab = blockedCountsByTab,
                    isSiteAllowlisted = { url -> browserViewModel.isSiteAllowlisted(url) },
                    searchEngineBase = browserPrefs.searchEngine,
                    onToggleBars = { isBarsVisible = it },
                    onToggleWebDarkMode = { browserViewModel.toggleWebDarkMode() },
                    onToggleDesktopMode = { browserViewModel.toggleDesktopMode() },
                    onSetThemeMode = { browserViewModel.setThemeMode(it) },
                    onSetColorSeed = { browserViewModel.setColorSeed(it) },
                    onNavigateUrl = { browserViewModel.setBrowserUrl(it) },
                    onToggleAdBlock = { browserViewModel.toggleAdBlock() },
                    onToggleSiteAllowlist = { url -> browserViewModel.toggleSiteAllowlist(url) },
                    onResetTabBlockedCount = { tabId -> browserViewModel.resetTabBlockedCount(tabId) },
                    onOpenSniffer = { browserViewModel.openSnifferSheet() },
                    onCancelRebind = { browserViewModel.cancelRebindFlow() },
                    onNewTab = { url, isIncognito, inBackground ->
                        browserViewModel.openNewBrowserTab(url, isIncognito, switchImmediately = !inBackground)
                        if (inBackground) {
                            val msg = if (isIncognito) "Opened in background (Incognito)" else "Opened in background"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDownloadDirectMedia = { url ->
                        browserViewModel.downloadDirectMedia(url)
                        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                    },
                    onMagnetLinkClicked = { uri ->
                        browserViewModel.showMagnetDownloadPopup(uri)
                    },
                    onSelectTab = { browserViewModel.switchBrowserTab(it) },
                    onCloseTab = { browserViewModel.closeBrowserTab(it) },
                    onToggleBookmark = {
                        val activeTab = browserTabs.firstOrNull { it.id == activeTabId }
                        val title = activeTab?.title.orEmpty()
                        browserViewModel.toggleBookmark(browserUrl, title)
                    },
                    onDeleteBookmark = { browserViewModel.deleteBookmark(it) },
                    onDeleteHistory = { browserViewModel.deleteHistory(it) },
                    onClearHistory = { browserViewModel.clearHistory() },
                    onUpdateTabMeta = { url, title -> browserViewModel.updateActiveTabMeta(url, title) },
                    onRegisterDomScan = { scanner -> browserViewModel.onRequestDomScan = scanner },
                    onRegisterPageReload = { reloader -> browserViewModel.onRequestPageReload = reloader },
                    onOpenTabSwitcher = { onSelectScreen(AppScreen.TAB_SWITCHER) },
                    onOpenDownloads = { onSelectScreen(AppScreen.QUEUE) },
                    onOpenBookmarks = { onSelectScreen(AppScreen.BOOKMARKS) },
                    onOpenHistory = { onSelectScreen(AppScreen.HISTORY) },
                    onOpenSettings = { onSelectScreen(AppScreen.SETTINGS) },
                    onOpenAdblockSettings = {
                        settingsSubPageOrigin = AppScreen.BROWSER
                        onSelectScreen(AppScreen.SETTINGS_ADBLOCK)
                    },
                    onCaptureThumbnail = { tabId, bitmap -> browserViewModel.updateTabThumbnail(tabId, bitmap) }
                )
            }

            if (currentScreen != AppScreen.BROWSER) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                        .graphicsLayer {
                            val s = 1f - (backProgress * 0.08f)
                            scaleX = s
                            scaleY = s
                            alpha = 1f - (backProgress * 0.25f)
                        }
                        .zIndex(2f)
                ) {
                    when (currentScreen) {
                        AppScreen.QUEUE -> {
                            QueueScreen(
                                downloads = downloads,
                                chunksMap = chunksMap,
                                maxConcurrentDownloads = concurrencySettings.maxDownloads,
                                workersPerDownload = concurrencySettings.workers,
                                bandwidthLimitBytes = concurrencySettings.bandwidthLimit,
                                isWifiOnly = concurrencySettings.wifiOnly,
                                selectedFilter = queueFilter,
                                onSelectFilter = { queueViewModel.setFilter(it) },
                                selectedSort = queueSortOption,
                                onSelectSort = { queueViewModel.setSortOption(it) },
                                searchQuery = queueSearchQuery,
                                onSearchQueryChange = { queueViewModel.setSearchQuery(it) },
                                isSelectionMode = queueIsSelectionMode,
                                onSetSelectionMode = { queueViewModel.setSelectionMode(it) },
                                selectedIds = queueSelectedIds,
                                onToggleSelectId = { queueViewModel.toggleSelection(it) },
                                onSelectAllIds = { queueViewModel.selectAll(it) },
                                onClearSelection = { queueViewModel.clearSelection() },
                                onPause = { queueViewModel.pauseDownload(it) },
                                onResume = { queueViewModel.resumeDownload(it) },
                                onCancel = { queueViewModel.cancelDownload(it) },
                                onDelete = { id, deleteFile -> queueViewModel.deleteDownload(id, deleteFile) },
                                onRebind = { download ->
                                    onSelectScreen(AppScreen.BROWSER)
                                    browserViewModel.startRebindFlow(download)
                                },
                                onClearCompleted = { queueViewModel.clearCompleted() },
                                onPauseAll = { queueViewModel.pauseAll() },
                                onResumeAll = { queueViewModel.resumeAll() },
                                onRetry = { queueViewModel.retryDownload(it) },
                                onRename = { id, name -> queueViewModel.renameDownload(id, name) },
                                onBulkDelete = { ids, deleteFiles -> queueViewModel.bulkDelete(ids, deleteFiles) },
                                onBulkCancel = { ids -> queueViewModel.bulkCancel(ids) },
                                onOpenDownloadSettings = {
                                    settingsSubPageOrigin = AppScreen.QUEUE
                                    onSelectScreen(AppScreen.SETTINGS_DOWNLOADS)
                                },
                                onNavigateBack = { onSelectScreen(AppScreen.BROWSER) }
                            )
                        }
                        AppScreen.TAB_SWITCHER -> {
                            TabSwitcherScreen(
                                tabs = browserTabs,
                                activeTabId = activeTabId,
                                thumbnails = tabThumbnails,
                                lastClosedTab = lastClosedTab,
                                onSelectTab = { id ->
                                    browserViewModel.switchBrowserTab(id)
                                    onSelectScreen(AppScreen.BROWSER)
                                },
                                onCloseTab = { browserViewModel.closeBrowserTab(it) },
                                onUndoCloseTab = { browserViewModel.undoCloseTab() },
                                onDismissUndo = { browserViewModel.dismissUndoCloseTab() },
                                onNewTab = { isIncognito ->
                                    browserViewModel.openNewBrowserTab(
                                        url = NEW_TAB_URL,
                                        isIncognito = isIncognito,
                                        switchImmediately = true
                                    )
                                    onSelectScreen(AppScreen.BROWSER)
                                },
                                onCloseAllTabs = { incognitoOnly -> browserViewModel.closeAllTabs(incognitoOnly) },
                                onDismiss = { onSelectScreen(AppScreen.BROWSER) }
                            )
                        }
                        AppScreen.BOOKMARKS -> {
                            BookmarksScreen(
                                bookmarks = bookmarks,
                                onOpenUrl = { url, inNewTab, isIncognito ->
                                    if (inNewTab || isIncognito) {
                                        browserViewModel.openNewBrowserTab(url, isIncognito = isIncognito, switchImmediately = true)
                                    } else {
                                        browserViewModel.setBrowserUrl(url)
                                    }
                                    onSelectScreen(AppScreen.BROWSER)
                                },
                                onDeleteBookmark = { browserViewModel.deleteBookmark(it) },
                                onBack = { onSelectScreen(AppScreen.BROWSER) }
                            )
                        }
                        AppScreen.HISTORY -> {
                            HistoryScreen(
                                history = history,
                                onOpenUrl = { url, inNewTab, isIncognito ->
                                    if (inNewTab || isIncognito) {
                                        browserViewModel.openNewBrowserTab(url, isIncognito = isIncognito, switchImmediately = true)
                                    } else {
                                        browserViewModel.setBrowserUrl(url)
                                    }
                                    onSelectScreen(AppScreen.BROWSER)
                                },
                                onDeleteHistory = { browserViewModel.deleteHistory(it) },
                                onClearHistory = { browserViewModel.clearHistory() },
                                onBack = { onSelectScreen(AppScreen.BROWSER) }
                            )
                        }
                        AppScreen.SETTINGS -> {
                            SettingsHubScreen(
                                browserPreferences = browserPrefs,
                                themeMode = themeMode,
                                colorSeed = colorSeed,
                                isAdBlockEnabled = isAdBlockEnabled,
                                adblockRuleCount = adblockRuleCount,
                                enabledListCount = adblockSettings.enabledLists.count { it.isEnabled },
                                maxConcurrentDownloads = concurrencySettings.maxDownloads,
                                workersPerDownload = concurrencySettings.workers,
                                bandwidthLimit = concurrencySettings.bandwidthLimit,
                                onOpenBrowserSearch = {
                                    settingsSubPageOrigin = AppScreen.SETTINGS
                                    onSelectScreen(AppScreen.SETTINGS_BROWSER)
                                },
                                onOpenAppearance = {
                                    settingsSubPageOrigin = AppScreen.SETTINGS
                                    onSelectScreen(AppScreen.SETTINGS_APPEARANCE)
                                },
                                onOpenPrivacyAdblock = {
                                    settingsSubPageOrigin = AppScreen.SETTINGS
                                    onSelectScreen(AppScreen.SETTINGS_ADBLOCK)
                                },
                                onOpenDownloads = {
                                    settingsSubPageOrigin = AppScreen.SETTINGS
                                    onSelectScreen(AppScreen.SETTINGS_DOWNLOADS)
                                },
                                onBack = { onSelectScreen(AppScreen.BROWSER) }
                            )
                        }
                        AppScreen.SETTINGS_BROWSER -> {
                            BrowserSearchSettingsScreen(
                                browserPreferences = browserPrefs,
                                onSetSearchEngine = { browserViewModel.setSearchEngine(it) },
                                onSetWebDarkMode = { enabled ->
                                    if (enabled != browserPrefs.webDarkMode) {
                                        browserViewModel.toggleWebDarkMode()
                                    }
                                },
                                onSetDesktopMode = { enabled ->
                                    if (enabled != browserPrefs.desktopMode) {
                                        browserViewModel.toggleDesktopMode()
                                    }
                                },
                                onBack = navigateBack
                            )
                        }
                        AppScreen.SETTINGS_APPEARANCE -> {
                            AppearanceSettingsScreen(
                                themeMode = themeMode,
                                colorSeed = colorSeed,
                                onSetThemeMode = { browserViewModel.setThemeMode(it) },
                                onSetColorSeed = { browserViewModel.setColorSeed(it) },
                                onBack = navigateBack
                            )
                        }
                        AppScreen.SETTINGS_ADBLOCK -> {
                            PrivacyAdblockSettingsScreen(
                                isAdBlockEnabled = isAdBlockEnabled,
                                adblockSettings = adblockSettings,
                                activeRuleCount = adblockRuleCount,
                                onToggleAdBlock = { browserViewModel.toggleAdBlock() },
                                onToggleList = { id, enabled -> browserViewModel.toggleFilterList(id, enabled) },
                                onAddCustomList = { name, url -> browserViewModel.addCustomFilterList(name, url) },
                                onRemoveCustomList = { id -> browserViewModel.removeCustomFilterList(id) },
                                onUpdateCustomRules = { rules -> browserViewModel.updateCustomUserRules(rules) },
                                onRefreshSubscriptions = { onComplete ->
                                    browserViewModel.refreshAdblockSubscriptions(
                                        onError = { err ->
                                            onComplete()
                                            Toast.makeText(
                                                context,
                                                "Adblock error: ${err.message ?: "Failed to update"}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onResult = { rules, lists ->
                                            onComplete()
                                            Toast.makeText(
                                                context,
                                                "Adblock updated: $rules rules ($lists lists)",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                },
                                onClearHistory = { browserViewModel.clearHistory() },
                                onBack = navigateBack
                            )
                        }
                        AppScreen.SETTINGS_DOWNLOADS -> {
                            DownloadSettingsScreen(
                                currentMaxDownloads = concurrencySettings.maxDownloads,
                                currentWorkers = concurrencySettings.workers,
                                currentBandwidthLimit = concurrencySettings.bandwidthLimit,
                                currentWifiOnly = concurrencySettings.wifiOnly,
                                currentCustomDownloadUri = concurrencySettings.customDownloadUri,
                                onUpdateConcurrency = { maxD, w, limit, wifi, customUri ->
                                    queueViewModel.setConcurrency(maxD, w, limit, wifi, customUri)
                                },
                                onBack = navigateBack
                            )
                        }
                        AppScreen.BROWSER -> Unit
                    }
                }
            }

            val activeTabTitle = browserTabs.firstOrNull { it.id == activeTabId }?.title

            SnifferSheet(
                isOpen = isSnifferSheetOpen,
                detectedMedia = detectedMedia,
                rebindTarget = rebindTarget,
                activeTabTitle = activeTabTitle,
                activeTabUrl = browserUrl,
                onDismiss = { browserViewModel.closeSnifferSheet() },
                onClearMedia = { browserViewModel.clearDetectedMedia() },
                onRefreshMedia = { browserViewModel.refreshDetectedMedia() },
                onDownloadSelected = { requests ->
                    val target = rebindTarget
                    if (target != null) {
                        val first = requests.firstOrNull() ?: return@SnifferSheet
                        browserViewModel.rebindDownload(target, first.media, first.selectedVariantUrl)
                    } else {
                        requests.forEach { req ->
                            browserViewModel.startDownload(
                                media = req.media,
                                customDisplayName = req.customFileName,
                                selectedVariantUrl = req.selectedVariantUrl
                            )
                        }
                    }
                }
            )

            pendingMagnetUri?.let { magnetUri ->
                MagnetDownloadDialog(
                    magnetUri = magnetUri,
                    customDownloadUri = concurrencySettings.customDownloadUri,
                    onConfirm = {
                        browserViewModel.startMagnetDownload(magnetUri)
                        Toast.makeText(context, "Torrent download started", Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { browserViewModel.dismissMagnetDownloadPopup() }
                )
            }

            AnimatedVisibility(
                visible = !pendingClipboardLink.isNullOrBlank(),
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .zIndex(10f)
            ) {
                pendingClipboardLink?.let { link ->
                    ClipboardDetectionBanner(
                        link = link,
                        onAccept = { onAcceptClipboardLink(link) },
                        onDismiss = onDismissClipboardLink
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipboardDetectionBanner(
    link: String,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMagnet = link.startsWith("magnet:", ignoreCase = true)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ExpressiveShapeTokens.RestingCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMagnet) Icons.Rounded.Hub else Icons.Rounded.ContentPaste,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isMagnet) "Magnet Link Detected" else "Link in Clipboard",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = link,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    shape = ExpressiveShapeTokens.PillShape
                ) {
                    Text("Dismiss")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onAccept,
                    shape = ExpressiveShapeTokens.PillShape
                ) {
                    Text(
                        text = if (isMagnet) "Download Magnet" else "Sniff / Open",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
