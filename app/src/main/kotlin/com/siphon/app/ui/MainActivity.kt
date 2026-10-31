package com.siphon.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.siphon.app.SiphonApp
import com.siphon.app.data.db.DownloadState
import com.siphon.app.ui.browser.BrowserScreen
import com.siphon.app.ui.browser.BrowserViewModel
import com.siphon.app.ui.queue.QueueScreen
import com.siphon.app.ui.queue.QueueViewModel
import com.siphon.app.ui.sniffer.SnifferSheet
import com.siphon.app.ui.theme.SiphonTheme
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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()
        handleIncomingIntent(intent)

        setContent {
            val themeMode by browserViewModel.themeMode.collectAsState()
            val colorSeed by browserViewModel.colorSeed.collectAsState()
            val tab by currentTab.collectAsState()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    browserViewModel: BrowserViewModel,
    queueViewModel: QueueViewModel,
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit
) {
    var isBarsVisible by remember { mutableStateOf(true) }

    val browserTabs by browserViewModel.browserTabs.collectAsState()
    val activeTabId by browserViewModel.activeTabId.collectAsState()
    val browserUrl by browserViewModel.browserUrl.collectAsState()
    val detectedMedia by browserViewModel.detectedMedia.collectAsState()
    val rebindTarget by browserViewModel.rebindTarget.collectAsState()
    val isAdBlockEnabled by browserViewModel.isAdBlockEnabled.collectAsState()
    val browserPrefs by browserViewModel.browserPreferences.collectAsState()
    val themeMode by browserViewModel.themeMode.collectAsState()
    val colorSeed by browserViewModel.colorSeed.collectAsState()
    val isSnifferSheetOpen by browserViewModel.isSnifferSheetOpen.collectAsState()
    val bookmarks by browserViewModel.bookmarks.collectAsState()
    val history by browserViewModel.history.collectAsState()
    val isCurrentBookmarked by browserViewModel.isCurrentUrlBookmarked.collectAsState()
    val adblockRuleCount by browserViewModel.adblockRuleCount.collectAsState()

    val downloads by queueViewModel.downloads.collectAsState()
    val chunksMap by queueViewModel.chunksMap.collectAsState()
    val concurrencySettings by queueViewModel.concurrencySettings.collectAsState()
    val queueFilter by queueViewModel.filter.collectAsState()
    val queueSortOption by queueViewModel.sortOption.collectAsState()
    val queueSearchQuery by queueViewModel.searchQuery.collectAsState()
    val queueSelectedIds by queueViewModel.selectedIds.collectAsState()
    val queueIsSelectionMode by queueViewModel.isSelectionMode.collectAsState()

    val activeDownloadsCount = downloads.count {
        it.state == DownloadState.ACTIVE || it.state == DownloadState.QUEUED || it.state == DownloadState.MERGING
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
                    onToggleBars = { isBarsVisible = it },
                    onToggleWebDarkMode = { browserViewModel.toggleWebDarkMode() },
                    onToggleDesktopMode = { browserViewModel.toggleDesktopMode() },
                    onSetThemeMode = { browserViewModel.setThemeMode(it) },
                    onSetColorSeed = { browserViewModel.setColorSeed(it) },
                    onNavigateUrl = { browserViewModel.setBrowserUrl(it) },
                    onToggleAdBlock = { browserViewModel.toggleAdBlock() },
                    onOpenSniffer = { browserViewModel.openSnifferSheet() },
                    onCancelRebind = { browserViewModel.cancelRebindFlow() },
                    onNewTab = { url, isIncognito -> browserViewModel.openNewBrowserTab(url, isIncognito) },
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
                    onRefreshAdblock = { browserViewModel.refreshAdblockSubscription(it) },
                    onUpdateTabMeta = { url, title -> browserViewModel.updateActiveTabMeta(url, title) },
                    onRegisterDomScan = { scanner -> browserViewModel.onRequestDomScan = scanner }
                )
            }

            if (currentTab == AppTab.QUEUE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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

            // Floating Stained-Glass Pill Bottom Bar
            AnimatedVisibility(
                visible = isBarsVisible || currentTab != AppTab.BROWSER,
                enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .zIndex(3f)
            ) {
                val glassBrush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.76f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.38f),
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.76f)
                    )
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.28f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.50f)
                    )
                )

                Box(
                    modifier = Modifier
                        .shadow(elevation = 14.dp, shape = CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                        .clip(CircleShape)
                        .background(glassBrush)
                        .border(width = 1.2.dp, brush = borderBrush, shape = CircleShape)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .wrapContentWidth()
                        .height(42.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isBrowser = currentTab == AppTab.BROWSER
                        Surface(
                            color = if (isBrowser) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else Color.Transparent,
                            border = if (isBrowser) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                            shape = CircleShape,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onSelectTab(AppTab.BROWSER) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BadgedBox(badge = {
                                    if (detectedMedia.isNotEmpty()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text(detectedMedia.size.toString(), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    }
                                }) {
                                    Text("🌐", fontSize = 15.sp)
                                }
                                Text(
                                    "Browser",
                                    fontSize = 12.sp,
                                    fontWeight = if (isBrowser) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isBrowser) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val isQueue = currentTab == AppTab.QUEUE
                        Surface(
                            color = if (isQueue) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else Color.Transparent,
                            border = if (isQueue) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                            shape = CircleShape,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onSelectTab(AppTab.QUEUE) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
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
                                    color = if (isQueue) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            val activeTabTitle = browserTabs.firstOrNull { it.id == activeTabId }?.title

            SnifferSheet(
                isOpen = isSnifferSheetOpen,
                detectedMedia = detectedMedia,
                rebindTarget = rebindTarget,
                activeTabTitle = activeTabTitle,
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
