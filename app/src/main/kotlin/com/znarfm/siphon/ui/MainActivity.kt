package com.znarfm.siphon.ui

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.znarfm.siphon.SiphonApp
import com.znarfm.siphon.data.db.DownloadState
import com.znarfm.siphon.ui.browser.BookmarksScreen
import com.znarfm.siphon.ui.browser.BrowserScreen
import com.znarfm.siphon.ui.browser.BrowserSettingsScreen
import com.znarfm.siphon.ui.browser.BrowserViewModel
import com.znarfm.siphon.ui.browser.HistoryScreen
import com.znarfm.siphon.ui.browser.NEW_TAB_URL
import com.znarfm.siphon.ui.browser.TabSwitcherScreen
import com.znarfm.siphon.ui.queue.ConcurrencySettingsDialog
import com.znarfm.siphon.ui.queue.QueueScreen
import com.znarfm.siphon.ui.queue.QueueViewModel
import com.znarfm.siphon.ui.sniffer.SnifferSheet
import com.znarfm.siphon.ui.theme.SiphonTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppScreen {
    BROWSER,
    QUEUE,
    TAB_SWITCHER,
    BOOKMARKS,
    HISTORY,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val browserViewModel: BrowserViewModel by viewModels {
        (application as SiphonApp).container.viewModelFactory
    }

    private val queueViewModel: QueueViewModel by viewModels {
        (application as SiphonApp).container.viewModelFactory
    }

    private val _currentScreen = MutableStateFlow(AppScreen.BROWSER)
    val currentScreen = _currentScreen.asStateFlow()

    fun selectScreen(screen: AppScreen) {
        _currentScreen.value = screen
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
            SiphonTheme(themeMode = themeMode, colorSeed = colorSeed) {
                MainContent(
                    browserViewModel = browserViewModel,
                    queueViewModel = queueViewModel,
                    currentScreen = screen,
                    onSelectScreen = { selectScreen(it) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
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
                selectScreen(AppScreen.BROWSER)
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clipText = clipboard?.primaryClip?.let { clip ->
                    if (clip.itemCount > 0) clip.getItemAt(0).text?.toString() else null
                }
                val url = extractUrlFromText(clipText)
                if (!url.isNullOrBlank()) {
                    browserViewModel.loadUrlAndOpenSniffer(url)
                } else {
                    browserViewModel.openSnifferSheet()
                    Toast.makeText(this, "No URL in clipboard", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }

        val url = extractUrlFromIntent(intent)
        if (!url.isNullOrBlank()) {
            selectScreen(AppScreen.BROWSER)
            browserViewModel.loadUrlAndOpenSniffer(url)
        }
    }

    private fun extractUrlFromIntent(intent: Intent): String? {
        val data = intent.dataString
        if (!data.isNullOrBlank() && (data.startsWith("http://") || data.startsWith("https://"))) {
            return data
        }

        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        extractUrlFromText(text)?.let { return it }

        val clip = intent.clipData
        if (clip != null && clip.itemCount > 0) {
            for (i in 0 until clip.itemCount) {
                val item = clip.getItemAt(i)
                val uriStr = item.uri?.toString()
                if (!uriStr.isNullOrBlank() && (uriStr.startsWith("http://") || uriStr.startsWith("https://"))) {
                    return uriStr
                }
                extractUrlFromText(item.text?.toString())?.let { return it }
            }
        }

        extractUrlFromText(intent.getStringExtra(Intent.EXTRA_SUBJECT))?.let { return it }

        return null
    }

    companion object {
        private val urlRegex = Regex("""https?://[^\s<>"'{}|\\^`]+""")

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
    onSelectScreen: (AppScreen) -> Unit
) {
    val context = LocalContext.current
    var isBarsVisible by remember { mutableStateOf(true) }
    var showAdblockDialog by remember { mutableStateOf(false) }
    var showDownloadSettingsDialog by remember { mutableStateOf(false) }

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
    val adblockConfig by browserViewModel.adblockConfig.collectAsStateWithLifecycle()

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
            onSelectScreen(AppScreen.BROWSER)
        } catch (e: CancellationException) {
            backProgress = 0f
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isBrowserVisible = currentScreen == AppScreen.BROWSER

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isBrowserVisible) 1f else 0f)
                    .zIndex(if (isBrowserVisible) 1f else 0f)
            ) {
                BrowserScreen(
                    currentUrl = browserUrl,
                    tabs = browserTabs,
                    activeTabId = activeTabId,
                    snifferClient = browserViewModel.snifferClient,
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
                    adblockConfig = adblockConfig,
                    searchEngineBase = browserPrefs.searchEngine,
                    showAdblockDialog = showAdblockDialog,
                    onShowAdblockDialogChange = { showAdblockDialog = it },
                    onToggleBars = { isBarsVisible = it },
                    onToggleWebDarkMode = { browserViewModel.toggleWebDarkMode() },
                    onToggleDesktopMode = { browserViewModel.toggleDesktopMode() },
                    onSetThemeMode = { browserViewModel.setThemeMode(it) },
                    onSetColorSeed = { browserViewModel.setColorSeed(it) },
                    onNavigateUrl = { browserViewModel.setBrowserUrl(it) },
                    onToggleAdBlock = { browserViewModel.toggleAdBlock() },
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
                    onRefreshAdblock = { config ->
                        browserViewModel.refreshAdblockSubscriptions(config) { result ->
                            val msg = result.fold(
                                onSuccess = { (blocked, allowed) -> "Adblock: $blocked blocked, $allowed allowed" },
                                onFailure = { err -> "Adblock error: ${err.message ?: "Failed to update"}" }
                            )
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onUpdateTabMeta = { url, title -> browserViewModel.updateActiveTabMeta(url, title) },
                    onRegisterDomScan = { scanner -> browserViewModel.onRequestDomScan = scanner },
                    onRegisterPageReload = { reloader -> browserViewModel.onRequestPageReload = reloader },
                    onOpenTabSwitcher = { onSelectScreen(AppScreen.TAB_SWITCHER) },
                    onOpenDownloads = { onSelectScreen(AppScreen.QUEUE) },
                    onOpenBookmarks = { onSelectScreen(AppScreen.BOOKMARKS) },
                    onOpenHistory = { onSelectScreen(AppScreen.HISTORY) },
                    onOpenSettings = { onSelectScreen(AppScreen.SETTINGS) },
                    onCaptureThumbnail = { tabId, bitmap -> browserViewModel.updateTabThumbnail(tabId, bitmap) }
                )
            }

            if (currentScreen != AppScreen.BROWSER) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                                onSaveSettings = { maxD, w, limit, wifi -> queueViewModel.setConcurrency(maxD, w, limit, wifi) },
                                onClearCompleted = { queueViewModel.clearCompleted() },
                                onPauseAll = { queueViewModel.pauseAll() },
                                onResumeAll = { queueViewModel.resumeAll() },
                                onRetry = { queueViewModel.retryDownload(it) },
                                onRename = { id, name -> queueViewModel.renameDownload(id, name) },
                                onBulkDelete = { ids, deleteFiles -> queueViewModel.bulkDelete(ids, deleteFiles) },
                                onBulkCancel = { ids -> queueViewModel.bulkCancel(ids) },
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
                            BrowserSettingsScreen(
                                browserPreferences = browserPrefs,
                                themeMode = themeMode,
                                colorSeed = colorSeed,
                                isAdBlockEnabled = isAdBlockEnabled,
                                adblockRuleCount = adblockRuleCount,
                                onSetSearchEngine = { browserViewModel.setSearchEngine(it) },
                                onSetThemeMode = { browserViewModel.setThemeMode(it) },
                                onSetColorSeed = { browserViewModel.setColorSeed(it) },
                                onToggleAdBlock = { browserViewModel.toggleAdBlock() },
                                onOpenAdblockSubscriptions = { showAdblockDialog = true },
                                onOpenDownloadSettings = { showDownloadSettingsDialog = true },
                                onClearHistory = { browserViewModel.clearHistory() },
                                onBack = { onSelectScreen(AppScreen.BROWSER) }
                            )
                        }
                        AppScreen.BROWSER -> Unit
                    }
                }
            }

            if (showDownloadSettingsDialog) {
                ConcurrencySettingsDialog(
                    currentMaxDownloads = concurrencySettings.maxDownloads,
                    currentWorkers = concurrencySettings.workers,
                    currentBandwidthLimit = concurrencySettings.bandwidthLimit,
                    currentWifiOnly = concurrencySettings.wifiOnly,
                    onDismiss = { showDownloadSettingsDialog = false },
                    onConfirm = { maxD, w, limit, wifi ->
                        queueViewModel.setConcurrency(maxD, w, limit, wifi)
                        showDownloadSettingsDialog = false
                    }
                )
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
        }
    }
}
