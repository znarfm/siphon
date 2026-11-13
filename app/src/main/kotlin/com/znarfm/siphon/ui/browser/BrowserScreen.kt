package com.znarfm.siphon.ui.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.znarfm.siphon.core.adblock.AdblockConfig
import com.znarfm.siphon.core.sniffer.MediaSnifferClient
import com.znarfm.siphon.data.db.BookmarkEntity
import com.znarfm.siphon.data.db.DownloadEntity
import com.znarfm.siphon.data.db.HistoryEntity
import com.znarfm.siphon.ui.theme.AppColorSeed
import com.znarfm.siphon.ui.theme.AppThemeMode
import kotlinx.coroutines.CancellationException

private const val DARK_MODE_INJECT_JS = """
(function() {
    var parseLuma = function(bg) {
        if (!bg || bg === 'transparent' || bg === 'rgba(0, 0, 0, 0)') return null;
        var m = bg.match(/rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)(?:\s*,\s*([\d.]+))?\s*\)/);
        if (!m) return null;
        if (m[4] !== undefined && parseFloat(m[4]) < 0.15) return null;
        return (0.299 * parseInt(m[1], 10) + 0.587 * parseInt(m[2], 10) + 0.114 * parseInt(m[3], 10));
    };
    var isPageNativelyDark = function() {
        var docEl = document.documentElement;
        var body = document.body;
        if (!docEl) return false;
        var meta = document.querySelector('meta[name="color-scheme"], meta[name="supported-color-schemes"]');
        if (meta && /dark/i.test(meta.content)) return true;
        if ([docEl, body].some(function(el) {
            return el && (el.hasAttribute('dark') || /dark|night|black/i.test([el.className, el.getAttribute('data-theme'), el.getAttribute('data-color-mode'), el.getAttribute('data-bs-theme')].join(' ')));
        })) return true;
        var existingStyle = document.getElementById('__siphon_dark_mode');
        if (existingStyle) existingStyle.disabled = true;
        var darkBg = false;
        try {
            var ds = window.getComputedStyle(docEl);
            if (ds && (ds.colorScheme || '').toLowerCase().includes('dark')) darkBg = true;
            if (!darkBg) {
                var luma = parseLuma(body ? window.getComputedStyle(body).backgroundColor : null);
                if (luma === null) luma = parseLuma(ds ? ds.backgroundColor : null);
                if (luma !== null && luma < 115) darkBg = true;
            }
        } catch (e) {}
        if (existingStyle) existingStyle.disabled = false;
        return darkBg;
    };
    var applyDark = function() {
        var root = document.head || document.documentElement;
        if (!root) return;
        var existing = document.getElementById('__siphon_dark_mode');
        if (isPageNativelyDark()) {
            if (existing) existing.remove();
            return;
        }
        if (!existing) {
            var s = document.createElement('style');
            s.id = '__siphon_dark_mode';
            s.textContent = ':root { color-scheme: dark !important; } html { filter: invert(90%) hue-rotate(180deg) !important; background-color: #121212 !important; } img, video, iframe, canvas, svg, picture, [style*="background-image"] { filter: invert(100%) hue-rotate(180deg) !important; }';
            root.appendChild(s);
        }
    };
    applyDark();
    if (!window.__siphon_dark_observer && document.documentElement) {
        var timer = null;
        window.__siphon_dark_observer = new MutationObserver(function() {
            if (timer) return;
            timer = setTimeout(function() { timer = null; applyDark(); }, 120);
        });
        window.__siphon_dark_observer.observe(document.documentElement, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['class', 'style', 'data-theme', 'data-color-mode', 'data-color-scheme', 'data-bs-theme', 'dark']
        });
    }
})();
"""

private const val DARK_MODE_REMOVE_JS = """
(function() {
    if (window.__siphon_dark_observer) {
        window.__siphon_dark_observer.disconnect();
        window.__siphon_dark_observer = null;
    }
    var s = document.getElementById('__siphon_dark_mode');
    if (s) s.remove();
})();
"""

private fun applyWebDarkModeToWebView(webView: WebView, enabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        @Suppress("DEPRECATION")
        webView.settings.forceDark = if (enabled) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        webView.settings.isAlgorithmicDarkeningAllowed = enabled
    }
    webView.evaluateJavascript(if (enabled) DARK_MODE_INJECT_JS else DARK_MODE_REMOVE_JS, null)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    currentUrl: String,
    tabs: List<BrowserTab>,
    activeTabId: String,
    snifferClientForTab: (String) -> MediaSnifferClient,
    detectedMediaCount: Int,
    activeDownloadCount: Int = 0,
    isAdBlockEnabled: Boolean,
    rebindTarget: DownloadEntity?,
    isBarsVisible: Boolean = true,
    isWebDarkMode: Boolean = false,
    isDesktopMode: Boolean = false,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    colorSeed: AppColorSeed = AppColorSeed.DYNAMIC,
    isCurrentBookmarked: Boolean = false,
    bookmarks: List<BookmarkEntity> = emptyList(),
    history: List<HistoryEntity> = emptyList(),
    adblockRuleCount: Int = 0,
    adblockConfig: AdblockConfig = AdblockConfig(),
    searchEngineBase: String = "https://www.google.com/search?q=",
    showAdblockDialog: Boolean = false,
    onShowAdblockDialogChange: (Boolean) -> Unit = {},
    onToggleBars: (Boolean) -> Unit = {},
    onToggleWebDarkMode: () -> Unit = {},
    onToggleDesktopMode: () -> Unit = {},
    onSetThemeMode: (AppThemeMode) -> Unit = {},
    onSetColorSeed: (AppColorSeed) -> Unit = {},
    onNavigateUrl: (String) -> Unit,
    onToggleAdBlock: () -> Unit,
    onOpenSniffer: () -> Unit,
    onCancelRebind: () -> Unit,
    onNewTab: (url: String, isIncognito: Boolean, inBackground: Boolean) -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onToggleBookmark: () -> Unit = {},
    onDeleteBookmark: (Long) -> Unit = {},
    onDeleteHistory: (Long) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onRefreshAdblock: (AdblockConfig) -> Unit = {},
    onUpdateTabMeta: (url: String, title: String) -> Unit,
    onRegisterDomScan: (((() -> Unit) -> Unit))? = null,
    onRegisterPageReload: (((() -> Unit) -> Unit))? = null,
    onDownloadDirectMedia: ((url: String) -> Unit)? = null,
    onMagnetLinkClicked: (String) -> Unit = {},
    onOpenTabSwitcher: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onCaptureThumbnail: (tabId: String, bitmap: ImageBitmap) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webViewMap = remember { mutableMapOf<String, WebView>() }
    val readyTabs = remember { mutableStateMapOf<String, Boolean>() }
    var urlTextFieldValue by remember { mutableStateOf(TextFieldValue(if (currentUrl == NEW_TAB_URL) "" else currentUrl)) }
    var isOmniboxFocused by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var isSiteInfoOpen by remember { mutableStateOf(false) }
    var isFindInPageOpen by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findActiveMatchOrdinal by remember { mutableIntStateOf(0) }
    var findTotalMatches by remember { mutableIntStateOf(0) }
    var pageProgress by remember { mutableIntStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var pendingPopupUrl by remember { mutableStateOf<String?>(null) }
    var contextMenuTarget by remember { mutableStateOf<ContextMenuTarget?>(null) }
    var filePathCallback by remember { mutableStateOf<android.webkit.ValueCallback<Array<Uri>>?>(null) }
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val currentActiveTabId = rememberUpdatedState(activeTabId)
    val currentBarsVisible = rememberUpdatedState(isBarsVisible)
    val currentWebDarkMode = rememberUpdatedState(isWebDarkMode)
    val currentOnMagnetLinkClicked = rememberUpdatedState(onMagnetLinkClicked)
    val currentOnUpdateTabMeta = rememberUpdatedState(onUpdateTabMeta)
    val currentIsOmniboxFocused = rememberUpdatedState(isOmniboxFocused)
    val defaultUserAgent = remember { mutableStateOf<String?>(null) }
    val desktopUserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    LaunchedEffect(isDesktopMode) {
        webViewMap.values.forEach { webView ->
            if (defaultUserAgent.value == null) {
                defaultUserAgent.value = webView.settings.userAgentString
            }
            webView.settings.userAgentString = if (isDesktopMode) desktopUserAgent else defaultUserAgent.value
            webView.settings.useWideViewPort = isDesktopMode
            webView.settings.loadWithOverviewMode = isDesktopMode
        }
    }

    val isSystemDark = isSystemInDarkTheme()
    val isAppDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
    }
    val surfaceColorArgb = MaterialTheme.colorScheme.background.toArgb()
    val webViewBgArgb = if (isWebDarkMode || isAppDark) surfaceColorArgb else android.graphics.Color.WHITE

    LaunchedEffect(isWebDarkMode, webViewBgArgb) {
        webViewMap.values.forEach { webView ->
            webView.setBackgroundColor(webViewBgArgb)
            applyWebDarkModeToWebView(webView, isWebDarkMode)
        }
    }

    LaunchedEffect(activeTabId, tabs.size) {
        onToggleBars(true)
    }

    val activeWebView = webViewMap[activeTabId]

    val domScanAction: (WebView, MediaSnifferClient) -> Unit = { webView, tabSnifferClient ->
        val js = """
            (function() {
                var items = [];
                document.querySelectorAll('video, audio, source').forEach(function(el) {
                    if (el.src) items.push({url: el.src});
                    if (el.currentSrc) items.push({url: el.currentSrc});
                });
                document.querySelectorAll('track').forEach(function(el) {
                    if (el.src) items.push({url: el.src, lang: el.srclang || '', label: el.label || ''});
                });
                if (window.performance && window.performance.getEntriesByType) {
                    window.performance.getEntriesByType('resource').forEach(function(r) {
                        if (r.name && (r.name.includes('.m3u8') || r.name.includes('.mp4') || r.name.includes('.vtt') || r.name.includes('.srt') || r.name.includes('.ass') || r.name.includes('.ttml') || r.initiatorType === 'video' || r.initiatorType === 'audio' || r.initiatorType === 'track')) {
                            items.push({url: r.name});
                        }
                    });
                }
                return JSON.stringify(items);
            })()
        """.trimIndent()
        webView.evaluateJavascript(js) { json ->
            if (!json.isNullOrBlank() && json != "null" && json != "[]") {
                val cleanJson = if (json.startsWith("\"") && json.endsWith("\"")) {
                    org.json.JSONTokener(json).nextValue().toString()
                } else json
                runCatching {
                    val array = org.json.JSONArray(cleanJson)
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i)
                        if (obj != null) {
                            val mediaUrl = obj.optString("url")
                            val lang = obj.optString("lang").takeIf { it.isNotBlank() }
                            val label = obj.optString("label").takeIf { it.isNotBlank() }
                            if (mediaUrl.isNotBlank()) {
                                tabSnifferClient.inspectCandidate(mediaUrl, emptyMap(), lang, label)
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(activeTabId, activeWebView) {
        onRegisterDomScan?.invoke {
            val webView = webViewMap[activeTabId] ?: return@invoke
            domScanAction(webView, snifferClientForTab(activeTabId))
        }
        onRegisterPageReload?.invoke {
            val webView = webViewMap[activeTabId] ?: return@invoke
            webView.reload()
        }
    }

    LaunchedEffect(activeTabId) {
        isFindInPageOpen = false
        findQuery = ""
        findActiveMatchOrdinal = 0
        findTotalMatches = 0
    }

    LaunchedEffect(activeWebView) {
        if (activeWebView == null) {
            canGoBack = false
            canGoForward = false
            pageProgress = 100
        } else {
            activeWebView.setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                findActiveMatchOrdinal = activeMatchOrdinal
                findTotalMatches = numberOfMatches
            }
        }
    }

    LaunchedEffect(activeTabId, currentUrl) {
        val currentActiveUrl = activeWebView?.url ?: currentUrl
        if (!isOmniboxFocused) {
            urlTextFieldValue = TextFieldValue(
                if (currentActiveUrl == NEW_TAB_URL) "" else currentActiveUrl
            )
        }
    }

    val handleCancelFocus = {
        isOmniboxFocused = false
        focusManager.clearFocus()
        val cur = activeWebView?.url ?: currentUrl
        urlTextFieldValue = TextFieldValue(if (cur == NEW_TAB_URL) "" else cur)
    }

    val captureActiveThumbnail: () -> Unit = {
        val webView = webViewMap[activeTabId]
        val tab = tabs.firstOrNull { it.id == activeTabId }
        if (webView != null && tab != null && tab.url != NEW_TAB_URL && webView.width > 0 && webView.height > 0) {
            runCatching {
                val scale = 0.25f
                val w = (webView.width * scale).toInt().coerceAtLeast(1)
                val h = (webView.height * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.scale(scale, scale)
                webView.draw(canvas)
                onCaptureThumbnail(tab.id, bitmap.asImageBitmap())
            }
        }
    }

    val sharePage: () -> Unit = {
        val activeTab = tabs.firstOrNull { it.id == activeTabId }
        val shareUrl = activeWebView?.url ?: activeTab?.url ?: currentUrl
        if (shareUrl.isBlank() || shareUrl == NEW_TAB_URL) {
            Toast.makeText(context, "Nothing to share", Toast.LENGTH_SHORT).show()
        } else {
            val shareTitle = activeWebView?.title?.takeIf { it.isNotBlank() } ?: activeTab?.title
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareUrl)
                if (!shareTitle.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                }
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share page"))
        }
    }

    BackHandler(enabled = customVideoView != null) {
        customViewCallback?.onCustomViewHidden()
        customVideoView = null
        customViewCallback = null
    }

    var backGestureProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler(enabled = isOmniboxFocused || isFindInPageOpen || canGoBack) { progress ->
        try {
            progress.collect { backEvent ->
                backGestureProgress = backEvent.progress
            }
            backGestureProgress = 0f
            when {
                isOmniboxFocused -> handleCancelFocus()
                isFindInPageOpen -> {
                    isFindInPageOpen = false
                    findQuery = ""
                    findActiveMatchOrdinal = 0
                    findTotalMatches = 0
                    activeWebView?.clearMatches()
                }
                else -> activeWebView?.goBack()
            }
        } catch (e: CancellationException) {
            backGestureProgress = 0f
        }
    }

    LaunchedEffect(currentUrl, activeTabId) {
        if (currentUrl != NEW_TAB_URL && activeWebView != null) {
            val currentWebUrl = activeWebView.url
            if (currentWebUrl == null || (currentWebUrl != currentUrl && !currentUrl.startsWith(currentWebUrl))) {
                activeWebView.loadUrl(currentUrl)
            }
        }
    }

    val activeTab = tabs.firstOrNull { it.id == activeTabId }
    val activeTabUrl = activeTab?.url ?: currentUrl
    val isNewTab = activeTabUrl == NEW_TAB_URL

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // "Update Link" 403 Rebind Banner
            if (rebindTarget != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(2f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Update Link Mode (403 Rebind)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = rebindTarget.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(onClick = onCancelRebind) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel rebind",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isFindInPageOpen && !isNewTab,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier.zIndex(2f)
            ) {
                FindInPageBar(
                    query = findQuery,
                    activeMatchOrdinal = findActiveMatchOrdinal,
                    totalMatches = findTotalMatches,
                    onQueryChange = { q ->
                        findQuery = q
                        if (q.isEmpty()) {
                            activeWebView?.clearMatches()
                            findActiveMatchOrdinal = 0
                            findTotalMatches = 0
                        } else {
                            activeWebView?.findAllAsync(q)
                        }
                    },
                    onFindNext = { activeWebView?.findNext(true) },
                    onFindPrev = { activeWebView?.findNext(false) },
                    onClose = {
                        isFindInPageOpen = false
                        findQuery = ""
                        findActiveMatchOrdinal = 0
                        findTotalMatches = 0
                        activeWebView?.clearMatches()
                    }
                )
            }

            // Multi-Tab WebView / NewTabPage Pool: Keeps all opened tab WebViews alive in memory
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(webViewBgArgb))
                    .graphicsLayer {
                        val s = 1f - (backGestureProgress * 0.04f)
                        scaleX = s
                        scaleY = s
                    }
            ) {
                tabs.forEach { tab ->
                    key(tab.id) {
                        val isVisible = tab.id == activeTabId
                        if (tab.url == NEW_TAB_URL) {
                            if (isVisible) {
                                NewTabPage(
                                    isIncognito = tab.isIncognito,
                                    bookmarks = bookmarks,
                                    activeDownloadCount = activeDownloadCount,
                                    onSearchClick = {
                                        urlTextFieldValue = TextFieldValue("")
                                        isOmniboxFocused = true
                                    },
                                    onOpenUrl = { url ->
                                        onNavigateUrl(url)
                                    },
                                    onOpenDownloads = onOpenDownloads,
                                    onOpenIncognito = {
                                        captureActiveThumbnail()
                                        onNewTab(NEW_TAB_URL, true, false)
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(1f)
                                )
                            }
                        } else {
                            val isPageReady = readyTabs[tab.id] == true
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(if (isVisible && isPageReady) 1f else 0f)
                                    .zIndex(if (isVisible) 1f else 0f)
                            ) {
                                AndroidView(
                                    factory = { ctx ->
                                        val density = ctx.resources.displayMetrics.density
                                        val minScrollablePx = (140f * density).toInt()
                                        val toggleSlopPx = 56f * density
                                        var touchAccumDy = 0f
                                        var lastTouchY = 0f
                                        var isTrackingTouch = false

                                        val webView = object : WebView(ctx) {
                                            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                                                if (tab.id != currentActiveTabId.value) return false
                                                when (ev.actionMasked) {
                                                    MotionEvent.ACTION_DOWN -> {
                                                        lastTouchY = ev.rawY
                                                        touchAccumDy = 0f
                                                        isTrackingTouch = true
                                                    }
                                                    MotionEvent.ACTION_MOVE -> {
                                                        if (isTrackingTouch) {
                                                            val deltaY = ev.rawY - lastTouchY
                                                            lastTouchY = ev.rawY
                                                            val maxScroll = (contentHeight * density).toInt() - height
                                                            if (maxScroll > minScrollablePx) {
                                                                if ((deltaY < 0 && touchAccumDy > 0) || (deltaY > 0 && touchAccumDy < 0)) {
                                                                    touchAccumDy = 0f
                                                                }
                                                                touchAccumDy += deltaY
                                                                if (touchAccumDy < -toggleSlopPx && scrollY > (48 * density).toInt()) {
                                                                    if (currentBarsVisible.value) onToggleBars(false)
                                                                    touchAccumDy = 0f
                                                                } else if (touchAccumDy > toggleSlopPx) {
                                                                    if (!currentBarsVisible.value) onToggleBars(true)
                                                                    touchAccumDy = 0f
                                                                }
                                                            } else if (!currentBarsVisible.value) {
                                                                onToggleBars(true)
                                                            }
                                                        }
                                                    }
                                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                                        isTrackingTouch = false
                                                        touchAccumDy = 0f
                                                    }
                                                }
                                                return super.dispatchTouchEvent(ev)
                                            }
                                        }.apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
                                            setBackgroundColor(webViewBgArgb)
                                            settings.apply {
                                                javaScriptEnabled = true
                                                domStorageEnabled = true
                                                useWideViewPort = isDesktopMode
                                                loadWithOverviewMode = isDesktopMode
                                                mediaPlaybackRequiresUserGesture = false
                                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                                setSupportMultipleWindows(true)
                                                javaScriptCanOpenWindowsAutomatically = true
                                                if (defaultUserAgent.value == null) {
                                                    defaultUserAgent.value = userAgentString
                                                }
                                                userAgentString = if (isDesktopMode) desktopUserAgent else defaultUserAgent.value
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    @Suppress("DEPRECATION")
                                                    forceDark = if (currentWebDarkMode.value) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
                                                }
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    isAlgorithmicDarkeningAllowed = currentWebDarkMode.value
                                                }
                                                if (tab.isIncognito) {
                                                    cacheMode = WebSettings.LOAD_NO_CACHE
                                                }
                                            }
                                            if (tab.isIncognito) {
                                                clearCache(true)
                                                clearHistory()
                                            }
                                            setOnLongClickListener { v ->
                                                val wv = v as? WebView ?: return@setOnLongClickListener false
                                                val hit = wv.hitTestResult
                                                when (hit.type) {
                                                    WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                                                        val url = hit.extra
                                                        if (!url.isNullOrBlank()) {
                                                            contextMenuTarget = ContextMenuTarget(linkUrl = url)
                                                            true
                                                        } else false
                                                    }
                                                    WebView.HitTestResult.IMAGE_TYPE -> {
                                                        val imgUrl = hit.extra
                                                        if (!imgUrl.isNullOrBlank()) {
                                                            contextMenuTarget = ContextMenuTarget(imageUrl = imgUrl)
                                                            true
                                                        } else false
                                                    }
                                                    WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                                                        val imgUrl = hit.extra
                                                        val handler = object : Handler(Looper.getMainLooper()) {
                                                            override fun handleMessage(msg: Message) {
                                                                val linkUrl = msg.data?.getString("url")
                                                                val src = msg.data?.getString("src") ?: imgUrl
                                                                if (!linkUrl.isNullOrBlank() || !src.isNullOrBlank()) {
                                                                    contextMenuTarget = ContextMenuTarget(linkUrl = linkUrl, imageUrl = src)
                                                                }
                                                            }
                                                        }
                                                        val msg = handler.obtainMessage()
                                                        wv.requestFocusNodeHref(msg)
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            }
                                            val tabSnifferClient = snifferClientForTab(tab.id).apply {
                                                onUrlChanged = { newUrl ->
                                                    if (tab.id == currentActiveTabId.value) {
                                                        if (!currentIsOmniboxFocused.value) {
                                                            urlTextFieldValue = TextFieldValue(if (newUrl == NEW_TAB_URL) "" else newUrl)
                                                        }
                                                        val title = webViewMap[tab.id]?.title.orEmpty()
                                                        if (title.isNotBlank()) {
                                                            currentOnUpdateTabMeta.value(newUrl, title)
                                                        }
                                                    }
                                                }
                                                onNavigationStateChanged = { back, forward ->
                                                    if (tab.id == currentActiveTabId.value) {
                                                        canGoBack = back
                                                        canGoForward = forward
                                                    }
                                                }
                                                onWebViewPageStarted = { view, url ->
                                                    if (currentWebDarkMode.value) {
                                                        applyWebDarkModeToWebView(view, true)
                                                    }
                                                    val domain = runCatching { Uri.parse(url).host }.getOrNull()
                                                    view.evaluateJavascript(
                                                        com.znarfm.siphon.core.sniffer.SnifferJsInjector.buildUserscript(domain),
                                                        null
                                                    )
                                                }
                                                onWebViewPageFinished = { view, url ->
                                                    if (currentWebDarkMode.value) {
                                                        applyWebDarkModeToWebView(view, true)
                                                    }
                                                    val domain = runCatching { Uri.parse(url).host }.getOrNull()
                                                    view.evaluateJavascript(
                                                        com.znarfm.siphon.core.sniffer.SnifferJsInjector.buildUserscript(domain),
                                                        null
                                                    )
                                                    domScanAction(view, this)
                                                }
                                            }
                                            addJavascriptInterface(
                                                com.znarfm.siphon.core.sniffer.SiphonSnifferBridge(tabSnifferClient),
                                                "__siphon_sniffer__"
                                            )
                                            webViewClient = tabSnifferClient
                                            webChromeClient = object : WebChromeClient() {
                                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                    if (newProgress >= 35) {
                                                        readyTabs[tab.id] = true
                                                    }
                                                    if (tab.id == currentActiveTabId.value) {
                                                        pageProgress = newProgress
                                                        canGoBack = view?.canGoBack() == true
                                                        canGoForward = view?.canGoForward() == true
                                                        val vUrl = view?.url
                                                        if (!vUrl.isNullOrBlank() && !isOmniboxFocused && vUrl != urlTextFieldValue.text) {
                                                            urlTextFieldValue = TextFieldValue(vUrl)
                                                            onNavigateUrl(vUrl)
                                                        }
                                                        val vTitle = view?.title.orEmpty()
                                                        if (!vUrl.isNullOrBlank()) {
                                                            onUpdateTabMeta(vUrl, vTitle)
                                                        }
                                                    }
                                                }

                                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                                    if (view == null) {
                                                        callback?.onCustomViewHidden()
                                                        return
                                                    }
                                                    customViewCallback?.onCustomViewHidden()
                                                    customVideoView = view
                                                    customViewCallback = callback
                                                }

                                                override fun onHideCustomView() {
                                                    customViewCallback?.onCustomViewHidden()
                                                    customVideoView = null
                                                    customViewCallback = null
                                                }

                                                override fun onCreateWindow(
                                                    view: WebView?,
                                                    isDialog: Boolean,
                                                    isUserGesture: Boolean,
                                                    resultMsg: Message?
                                                ): Boolean {
                                                    if (resultMsg == null) return false
                                                    val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                                                    val popupWebView = WebView(view?.context ?: return false).apply {
                                                        webViewClient = object : WebViewClient() {
                                                            override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                                                                val popupUrl = request?.url?.toString()
                                                                if (!popupUrl.isNullOrBlank()) {
                                                                    if (popupUrl.startsWith("magnet:", ignoreCase = true)) {
                                                                        currentOnMagnetLinkClicked.value(popupUrl)
                                                                        destroy()
                                                                        return true
                                                                    }
                                                                    pendingPopupUrl = popupUrl
                                                                    destroy()
                                                                    return true
                                                                }
                                                                return false
                                                            }
                                                        }
                                                    }
                                                    transport.webView = popupWebView
                                                    resultMsg.sendToTarget()
                                                    return true
                                                }

                                                override fun onShowFileChooser(
                                                    webView: WebView?,
                                                    callback: android.webkit.ValueCallback<Array<Uri>>?,
                                                    fileChooserParams: FileChooserParams?
                                                ): Boolean {
                                                    filePathCallback?.onReceiveValue(null)
                                                    filePathCallback = callback

                                                    val acceptTypes = fileChooserParams?.acceptTypes?.filter { it.isNotBlank() }.orEmpty()
                                                    return try {
                                                        if (acceptTypes.isNotEmpty() && acceptTypes.all { it.startsWith("image/") || it.startsWith("video/") }) {
                                                            runCatching {
                                                                photoPickerLauncher.launch(
                                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                                                )
                                                            }.onFailure {
                                                                fileChooserLauncher.launch(arrayOf("*/*"))
                                                            }
                                                        } else {
                                                            fileChooserLauncher.launch(
                                                                if (acceptTypes.isNotEmpty()) acceptTypes.toTypedArray() else arrayOf("*/*")
                                                            )
                                                        }
                                                        true
                                                    } catch (e: Exception) {
                                                        filePathCallback?.onReceiveValue(null)
                                                        filePathCallback = null
                                                        false
                                                    }
                                                }
                                            }
                                        }
                                        webViewMap[tab.id] = webView
                                        if (tab.url != NEW_TAB_URL) {
                                            webView.loadUrl(tab.url)
                                        }
                                        webView
                                    },
                                    update = { webView ->
                                        webViewMap[tab.id] = webView
                                        webView.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
                                        if (isVisible && isPageReady) {
                                            canGoBack = webView.canGoBack()
                                            canGoForward = webView.canGoForward()
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Always-Bottom Chrome Omnibox Dock
        AnimatedVisibility(
            visible = (isBarsVisible || isNewTab) && !isOmniboxFocused && customVideoView == null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(5f)
        ) {
            val currentDisplayUrl = activeWebView?.url ?: activeTabUrl
            val currentDisplayTitle = activeWebView?.title?.takeIf { it.isNotBlank() } ?: activeTab?.title.orEmpty()
            BrowserBottomOmnibox(
                url = currentDisplayUrl,
                title = currentDisplayTitle,
                isIncognito = activeTab?.isIncognito ?: false,
                isLoading = !isNewTab && pageProgress in 1..99,
                pageProgress = if (isNewTab) 100 else pageProgress,
                isBookmarked = isCurrentBookmarked,
                isAdBlockEnabled = isAdBlockEnabled,
                tabCount = tabs.size,
                detectedMediaCount = detectedMediaCount,
                activeDownloadCount = activeDownloadCount,
                onOpenOmniboxSearch = {
                    val cur = if (isNewTab) "" else (activeWebView?.url ?: activeTabUrl)
                    urlTextFieldValue = TextFieldValue(cur, TextRange(0, cur.length))
                    isOmniboxFocused = true
                },
                onOpenSiteInfo = { isSiteInfoOpen = true },
                onToggleBookmark = onToggleBookmark,
                onReload = { activeWebView?.reload() },
                onStop = { activeWebView?.stopLoading() },
                onOpenTabSwitcher = {
                    captureActiveThumbnail()
                    onOpenTabSwitcher()
                },
                onOpenMenu = { isMenuOpen = true },
                onOpenSniffer = onOpenSniffer,
                onOpenDownloads = onOpenDownloads
            )
        }

        // HTML5 Fullscreen Video Overlay
        customVideoView?.let { videoView ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .zIndex(20f)
            ) {
                AndroidView(
                    factory = {
                        (videoView.parent as? ViewGroup)?.removeView(videoView)
                        videoView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Fullscreen Omnibox Search Overlay
        if (isOmniboxFocused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(30f)
            ) {
                OmniboxSearchOverlay(
                    urlTextFieldValue = urlTextFieldValue,
                    onUrlChange = { urlTextFieldValue = it },
                    bookmarks = bookmarks,
                    history = history,
                    searchEngineBase = searchEngineBase,
                    onSubmitUrl = { input ->
                        val target = sanitizeUrl(input, searchEngineBase)
                        isOmniboxFocused = false
                        focusManager.clearFocus()
                        urlTextFieldValue = TextFieldValue(if (target == NEW_TAB_URL) "" else target)
                        onNavigateUrl(target)
                        if (target != NEW_TAB_URL) {
                            activeWebView?.loadUrl(target)
                        }
                    },
                    onSelectUrl = { url ->
                        isOmniboxFocused = false
                        focusManager.clearFocus()
                        urlTextFieldValue = TextFieldValue(url)
                        onNavigateUrl(url)
                        activeWebView?.loadUrl(url)
                    },
                    onDeleteBookmark = onDeleteBookmark,
                    onDeleteHistory = onDeleteHistory,
                    onClearHistory = onClearHistory,
                    onDismiss = handleCancelFocus
                )
            }
        }

        if (isSiteInfoOpen) {
            val displayUrl = activeWebView?.url ?: activeTabUrl
            val displayTitle = activeWebView?.title?.takeIf { it.isNotBlank() } ?: activeTab?.title.orEmpty()
            SiteInfoSheet(
                url = displayUrl,
                title = displayTitle,
                isIncognito = activeTab?.isIncognito ?: false,
                isAdBlockEnabled = isAdBlockEnabled,
                adblockRuleCount = adblockRuleCount,
                desktopMode = isDesktopMode,
                webDarkMode = isWebDarkMode,
                onToggleAdBlock = onToggleAdBlock,
                onToggleDesktopMode = onToggleDesktopMode,
                onToggleWebDarkMode = onToggleWebDarkMode,
                onManageAdblock = {
                    isSiteInfoOpen = false
                    onShowAdblockDialogChange(true)
                },
                onCopyUrl = { urlToCopy ->
                    clipboardManager.setText(AnnotatedString(urlToCopy))
                    Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { isSiteInfoOpen = false }
            )
        }

        BrowserMenuSheet(
            isMenuOpen = isMenuOpen,
            onDismissRequest = { isMenuOpen = false },
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            isLoading = !isNewTab && pageProgress in 1..99,
            isBookmarked = isCurrentBookmarked,
            adblockRuleCount = adblockRuleCount,
            activeDownloadCount = activeDownloadCount,
            isWebDarkMode = isWebDarkMode,
            isDesktopMode = isDesktopMode,
            isAdBlockEnabled = isAdBlockEnabled,
            onBack = { activeWebView?.goBack() },
            onForward = { activeWebView?.goForward() },
            onToggleBookmark = onToggleBookmark,
            onShare = sharePage,
            onOpenPageInfo = { isSiteInfoOpen = true },
            onReloadOrStop = {
                if (pageProgress in 1..99) {
                    activeWebView?.stopLoading()
                } else {
                    activeWebView?.reload()
                }
            },
            onNewTab = {
                captureActiveThumbnail()
                onNewTab(NEW_TAB_URL, false, false)
            },
            onNewIncognitoTab = {
                captureActiveThumbnail()
                onNewTab(NEW_TAB_URL, true, false)
            },
            onOpenBookmarks = onOpenBookmarks,
            onOpenHistory = onOpenHistory,
            onOpenDownloads = onOpenDownloads,
            onOpenFindInPage = {
                if (!isNewTab) {
                    isFindInPageOpen = true
                }
            },
            onToggleWebDarkMode = onToggleWebDarkMode,
            onToggleDesktopMode = onToggleDesktopMode,
            onToggleAdBlock = onToggleAdBlock,
            onOpenAdblockDialog = { onShowAdblockDialogChange(true) },
            onOpenSettings = onOpenSettings
        )

        if (pendingPopupUrl != null) {
            val popup = pendingPopupUrl ?: ""
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { pendingPopupUrl = null },
                title = { Text("Open Link in New Tab?") },
                text = { Text(popup, maxLines = 3, overflow = TextOverflow.Ellipsis) },
                confirmButton = {
                    TextButton(onClick = {
                        val activeIsIncognito = tabs.firstOrNull { it.id == activeTabId }?.isIncognito ?: false
                        onNewTab(popup, activeIsIncognito, false)
                        pendingPopupUrl = null
                    }) {
                        Text("Open")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingPopupUrl = null }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        if (showAdblockDialog) {
            AdblockSubscriptionDialog(
                currentRuleCount = adblockRuleCount,
                initialConfig = adblockConfig,
                onDismiss = { onShowAdblockDialogChange(false) },
                onApply = { config ->
                    onRefreshAdblock(config)
                    onShowAdblockDialogChange(false)
                }
            )
        }

        contextMenuTarget?.let { target ->
            LinkContextMenuSheet(
                target = target,
                onDismiss = { contextMenuTarget = null },
                onOpenInNewTab = { url ->
                    onNewTab(url, false, true)
                },
                onOpenInIncognitoTab = { url ->
                    onNewTab(url, true, true)
                },
                onDownloadImage = onDownloadDirectMedia
            )
        }
    }
}
