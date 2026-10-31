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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import com.siphon.app.data.db.DownloadState
import com.siphon.app.ui.browser.BrowserScreen
import com.siphon.app.ui.queue.QueueScreen
import com.siphon.app.ui.sniffer.SnifferSheet
import com.siphon.app.ui.theme.SiphonTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()
        handleIncomingIntent(intent)

        setContent {
            SiphonTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val url = intent.dataString
            if (!url.isNullOrBlank()) {
                viewModel.setBrowserUrl(url)
                viewModel.selectTab(AppTab.BROWSER)
            }
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
fun MainContent(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val browserTabs by viewModel.browserTabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val browserUrl by viewModel.browserUrl.collectAsState()
    val detectedMedia by viewModel.detectedMedia.collectAsState()
    val rebindTarget by viewModel.rebindTarget.collectAsState()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsState()
    val isSnifferSheetOpen by viewModel.isSnifferSheetOpen.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val chunksMap by viewModel.chunksMap.collectAsState()
    val maxConcurrentDownloads by viewModel.maxConcurrentDownloads.collectAsState()
    val workersPerDownload by viewModel.workersPerDownload.collectAsState()

    val activeDownloadsCount = downloads.count {
        it.state == DownloadState.ACTIVE || it.state == DownloadState.QUEUED
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 6.dp,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.BROWSER,
                    onClick = { viewModel.selectTab(AppTab.BROWSER) },
                    icon = {
                        BadgedBox(badge = {
                            if (detectedMedia.isNotEmpty()) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text(detectedMedia.size.toString(), fontWeight = FontWeight.Bold)
                                }
                            }
                        }) {
                            Text("🌐", fontSize = 20.sp)
                        }
                    },
                    label = { Text("Browser", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.QUEUE,
                    onClick = { viewModel.selectTab(AppTab.QUEUE) },
                    icon = {
                        BadgedBox(badge = {
                            if (activeDownloadsCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                ) {
                                    Text(activeDownloadsCount.toString(), fontWeight = FontWeight.Bold)
                                }
                            }
                        }) {
                            Text("📥", fontSize = 20.sp)
                        }
                    },
                    label = { Text("Downloads", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }
        }
    ) { innerPadding ->
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
                    snifferClient = viewModel.snifferClient,
                    detectedMediaCount = detectedMedia.size,
                    isAdBlockEnabled = isAdBlockEnabled,
                    rebindTarget = rebindTarget,
                    onNavigateUrl = { viewModel.setBrowserUrl(it) },
                    onToggleAdBlock = { viewModel.toggleAdBlock() },
                    onOpenSniffer = { viewModel.openSnifferSheet() },
                    onCancelRebind = { viewModel.cancelRebindFlow() },
                    onNewTab = { viewModel.openNewBrowserTab(it) },
                    onSelectTab = { viewModel.switchBrowserTab(it) },
                    onCloseTab = { viewModel.closeBrowserTab(it) },
                    onUpdateTabMeta = { url, title -> viewModel.updateActiveTabMeta(url, title) },
                    onRegisterDomScan = { scanner -> viewModel.onRequestDomScan = scanner }
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
                        maxConcurrentDownloads = maxConcurrentDownloads,
                        workersPerDownload = workersPerDownload,
                        onPause = { viewModel.pauseDownload(it) },
                        onResume = { viewModel.resumeDownload(it) },
                        onCancel = { viewModel.cancelDownload(it) },
                        onDelete = { viewModel.deleteDownload(it) },
                        onRebind = { viewModel.startRebindFlow(it) },
                        onSaveConcurrency = { maxD, w -> viewModel.setConcurrency(maxD, w) }
                    )
                }
            }

            SnifferSheet(
                isOpen = isSnifferSheetOpen,
                detectedMedia = detectedMedia,
                rebindTarget = rebindTarget,
                onDismiss = { viewModel.closeSnifferSheet() },
                onClearMedia = { viewModel.clearDetectedMedia() },
                onRefreshMedia = { viewModel.refreshDetectedMedia() },
                onDownloadMedia = { media, variantUrl ->
                    val target = rebindTarget
                    if (target != null) {
                        viewModel.rebindDownload(target, media, variantUrl)
                    } else {
                        viewModel.startDownload(media = media, selectedVariantUrl = variantUrl)
                    }
                },
                onRebindMedia = { target, media, variantUrl ->
                    viewModel.rebindDownload(target, media, variantUrl)
                }
            )
        }
    }
}
