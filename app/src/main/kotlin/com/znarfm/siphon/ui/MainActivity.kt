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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.znarfm.siphon.SiphonApp
import com.znarfm.siphon.data.db.DownloadState
import com.znarfm.siphon.ui.browser.BrowserScreen
import com.znarfm.siphon.ui.browser.BrowserViewModel
import com.znarfm.siphon.ui.queue.QueueScreen
import com.znarfm.siphon.ui.queue.QueueViewModel
import com.znarfm.siphon.ui.sniffer.SnifferSheet
import com.znarfm.siphon.ui.theme.SiphonTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTab {
    BROWSER,
    QUEUE
}

class MainActivity : ComponentActivity() {

    private val browserViewModel: BrowserViewModel by viewModels {
        (application as SiphonApp).container.viewModelFactory
    }

    private val queueViewModel: QueueViewModel by viewModels {
        (application as SiphonApp).container.viewModelFactory
    }

    private val _currentTab = MutableStateFlow(AppTab.BROWSER)
    val currentTab = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
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
            val tab by currentTab.collectAsStateWithLifecycle()
            SiphonTheme(themeMode = themeMode, colorSeed = colorSeed) {
                MainContent(
                    browserViewModel = browserViewModel,
                    queueViewModel = queueViewModel,
                    currentTab = tab,
                    onSelectTab = { selectTab(it) }
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
                selectTab(AppTab.BROWSER)
                browserViewModel.openNewBrowserTab("https://google.com", isIncognito = true, switchImmediately = true)
                return
            }
            "com.znarfm.siphon.action.SHORTCUT_QUEUE" -> {
                selectTab(AppTab.QUEUE)
                return
            }
            "com.znarfm.siphon.action.SHORTCUT_PASTE_SNIFF" -> {
                selectTab(AppTab.BROWSER)
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
            selectTab(AppTab.BROWSER)
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
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit
) {
    val context = LocalContext.current
    var isBarsVisible by remember { mutableStateOf(true) }

    val browserTabs by browserViewModel.browserTabs.collectAsStateWithLifecycle()
    val activeTabId by browserViewModel.activeTabId.collectAsStateWithLifecycle()
    val browserUrl by browserViewModel.browserUrl.collectAsStateWithLifecycle()
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

    PredictiveBackHandler(enabled = currentTab == AppTab.QUEUE && !queueIsSelectionMode) { progress ->
        try {
            progress.collect { backEvent ->
                backProgress = backEvent.progress
            }
            backProgress = 0f
            onSelectTab(AppTab.BROWSER)
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
            val isBrowserVisible = currentTab == AppTab.BROWSER

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
                    onRegisterPageReload = { reloader -> browserViewModel.onRequestPageReload = reloader }
                )
            }

            if (currentTab == AppTab.QUEUE) {
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
                            onSelectTab(AppTab.BROWSER)
                            browserViewModel.startRebindFlow(download)
                        },
                        onSaveSettings = { maxD, w, limit, wifi -> queueViewModel.setConcurrency(maxD, w, limit, wifi) },
                        onClearCompleted = { queueViewModel.clearCompleted() },
                        onPauseAll = { queueViewModel.pauseAll() },
                        onResumeAll = { queueViewModel.resumeAll() },
                        onRetry = { queueViewModel.retryDownload(it) },
                        onRename = { id, name -> queueViewModel.renameDownload(id, name) },
                        onBulkDelete = { ids, deleteFiles -> queueViewModel.bulkDelete(ids, deleteFiles) },
                        onBulkCancel = { ids -> queueViewModel.bulkCancel(ids) }
                    )
                }
            }

            // Material 3 Expressive HorizontalFloatingToolbar Bottom Bar
            AnimatedVisibility(
                visible = isBarsVisible || currentTab != AppTab.BROWSER,
                enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .zIndex(3f)
            ) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    floatingActionButton = {
                        FloatingToolbarDefaults.VibrantFloatingActionButton(
                            onClick = { browserViewModel.openSnifferSheet() }
                        ) {
                            BadgedBox(badge = {
                                if (detectedMedia.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text(detectedMedia.size.toString(), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }) {
                                Text("⚡", fontSize = 18.sp)
                            }
                        }
                    },
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    content = {
                        val isBrowser = currentTab == AppTab.BROWSER
                        val browserCornerRadius by animateDpAsState(
                            targetValue = if (isBrowser) 14.dp else 22.dp,
                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                        )
                        val browserBgColor by animateColorAsState(
                            targetValue = if (isBrowser) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                        )
                        val browserFgColor by animateColorAsState(
                            targetValue = if (isBrowser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                        )

                        Surface(
                            color = browserBgColor,
                            shape = RoundedCornerShape(browserCornerRadius),
                            modifier = Modifier
                                .clip(RoundedCornerShape(browserCornerRadius))
                                .clickable { onSelectTab(AppTab.BROWSER) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🌐", fontSize = 15.sp)
                                Text(
                                    "Browser",
                                    fontSize = 12.sp,
                                    fontWeight = if (isBrowser) FontWeight.Bold else FontWeight.Medium,
                                    color = browserFgColor
                                )
                            }
                        }

                        val isQueue = currentTab == AppTab.QUEUE
                        val queueCornerRadius by animateDpAsState(
                            targetValue = if (isQueue) 14.dp else 22.dp,
                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                        )
                        val queueBgColor by animateColorAsState(
                            targetValue = if (isQueue) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                        )
                        val queueFgColor by animateColorAsState(
                            targetValue = if (isQueue) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                        )

                        Surface(
                            color = queueBgColor,
                            shape = RoundedCornerShape(queueCornerRadius),
                            modifier = Modifier
                                .clip(RoundedCornerShape(queueCornerRadius))
                                .clickable { onSelectTab(AppTab.QUEUE) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BadgedBox(badge = {
                                    if (activeDownloadsCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.tertiary,
                                            contentColor = MaterialTheme.colorScheme.onTertiary
                                        ) {
                                            Text(activeDownloadsCount.toString(), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    }
                                }) {
                                    Text("📥", fontSize = 15.sp)
                                }
                                Text(
                                    "Downloads",
                                    fontSize = 12.sp,
                                    fontWeight = if (isQueue) FontWeight.Bold else FontWeight.Medium,
                                    color = queueFgColor
                                )
                            }
                        }
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
