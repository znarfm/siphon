package com.siphon.app.ui.browser

import android.annotation.SuppressLint
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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.siphon.app.core.adblock.AdblockConfig
import androidx.compose.ui.zIndex
import com.siphon.app.core.sniffer.MediaSnifferClient
import com.siphon.app.data.db.BookmarkEntity
import com.siphon.app.data.db.DownloadEntity
import com.siphon.app.data.db.HistoryEntity
import com.siphon.app.ui.theme.AppColorSeed
import com.siphon.app.ui.theme.AppThemeMode

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
    snifferClient: MediaSnifferClient,
    detectedMediaCount: Int,
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
    onDownloadDirectMedia: ((url: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val webViewMap = remember { mutableMapOf<String, WebView>() }
    val readyTabs = remember { mutableStateMapOf<String, Boolean>() }
    var urlTextFieldValue by remember { mutableStateOf(TextFieldValue(currentUrl)) }
    var isUrlBarFocused by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var showAdblockDialog by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableIntStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var pendingPopupUrl by remember { mutableStateOf<String?>(null) }
    var contextMenuTarget by remember { mutableStateOf<ContextMenuTarget?>(null) }
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val currentActiveTabId = rememberUpdatedState(activeTabId)
    val currentBarsVisible = rememberUpdatedState(isBarsVisible)
    val currentWebDarkMode = rememberUpdatedState(isWebDarkMode)
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

    val domScanAction: (WebView) -> Unit = { webView ->
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
                                snifferClient.inspectCandidate(mediaUrl, emptyMap(), lang, label)
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
            domScanAction(webView)
        }
    }

    LaunchedEffect(activeTabId, currentUrl) {
        val currentActiveUrl = activeWebView?.url ?: currentUrl
        if (!isUrlBarFocused) {
            urlTextFieldValue = TextFieldValue(currentActiveUrl)
        }
    }

    LaunchedEffect(snifferClient, activeTabId) {
        snifferClient.onUrlChanged = { newUrl ->
            if (!isUrlBarFocused) {
                urlTextFieldValue = TextFieldValue(newUrl)
            }
            val title = activeWebView?.title.orEmpty()
            if (title.isNotBlank()) {
                onUpdateTabMeta(newUrl, title)
            }
        }
        snifferClient.onNavigationStateChanged = { back, forward ->
            canGoBack = back
            canGoForward = forward
        }
        snifferClient.onWebViewPageStarted = { view, url ->
            if (currentWebDarkMode.value) {
                applyWebDarkModeToWebView(view, true)
            }
            val domain = runCatching { Uri.parse(url).host }.getOrNull()
            view.evaluateJavascript(com.siphon.app.core.sniffer.SnifferJsInjector.buildUserscript(domain), null)
        }
        snifferClient.onWebViewPageFinished = { view, url ->
            if (currentWebDarkMode.value) {
                applyWebDarkModeToWebView(view, true)
            }
            val domain = runCatching { Uri.parse(url).host }.getOrNull()
            view.evaluateJavascript(com.siphon.app.core.sniffer.SnifferJsInjector.buildUserscript(domain), null)
            domScanAction(view)
        }
    }

    BackHandler(enabled = isUrlBarFocused || canGoBack) {
        if (isUrlBarFocused) {
            isUrlBarFocused = false
            focusManager.clearFocus()
            val cur = activeWebView?.url ?: currentUrl
            urlTextFieldValue = TextFieldValue(cur)
        } else {
            activeWebView?.goBack()
        }
    }

    LaunchedEffect(currentUrl, activeTabId) {
        if (activeWebView != null) {
            val currentWebUrl = activeWebView.url
            if (currentWebUrl == null || (currentWebUrl != currentUrl && !currentUrl.startsWith(currentWebUrl))) {
                activeWebView.loadUrl(currentUrl)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isBarsVisible || isUrlBarFocused,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier.zIndex(2f)
        ) {
            Column {
                // "Update Link" 403 Rebind Banner
                if (rebindTarget != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                            TextButton(onClick = onCancelRebind) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                BrowserTopBar(
                    canGoBack = canGoBack,
                    onGoBack = { activeWebView?.goBack() },
                    onToggleAdBlock = onToggleAdBlock,
                    isAdBlockEnabled = isAdBlockEnabled,
                    urlTextFieldValue = urlTextFieldValue,
                    onUrlChange = { urlTextFieldValue = it },
                    isUrlBarFocused = isUrlBarFocused,
                    onFocusChange = { isUrlBarFocused = it },
                    onSubmitUrl = { target ->
                        isUrlBarFocused = false
                        focusManager.clearFocus()
                        urlTextFieldValue = TextFieldValue(target)
                        onNavigateUrl(target)
                        activeWebView?.loadUrl(target)
                    },
                    onClearUrl = {
                        urlTextFieldValue = TextFieldValue("")
                    },
                    pageProgress = pageProgress,
                    onReload = { activeWebView?.reload() },
                    onStop = { activeWebView?.stopLoading() },
                    isCurrentBookmarked = isCurrentBookmarked,
                    onToggleBookmark = onToggleBookmark,
                    detectedMediaCount = detectedMediaCount,
                    onOpenSniffer = onOpenSniffer,
                    onOpenMenu = { isMenuOpen = true },
                    bookmarks = bookmarks,
                    history = history,
                    onSelectHistoryOrBookmark = { url ->
                        isUrlBarFocused = false
                        focusManager.clearFocus()
                        urlTextFieldValue = TextFieldValue(url)
                        onNavigateUrl(url)
                        activeWebView?.loadUrl(url)
                    },
                    onDeleteBookmark = onDeleteBookmark,
                    onDeleteHistory = onDeleteHistory,
                    onClearHistory = onClearHistory,
                    menuContent = {
                        BrowserMenuSheet(
                            isMenuOpen = isMenuOpen,
                            onDismissRequest = { isMenuOpen = false },
                            adblockRuleCount = adblockRuleCount,
                            isWebDarkMode = isWebDarkMode,
                            isDesktopMode = isDesktopMode,
                            isAdBlockEnabled = isAdBlockEnabled,
                            themeMode = themeMode,
                            colorSeed = colorSeed,
                            currentUrl = activeWebView?.url ?: currentUrl,
                            clipboardManager = clipboardManager,
                            onNewIncognitoTab = { onNewTab("https://google.com", true, false) },
                            onOpenBookmarksAndHistory = { isUrlBarFocused = true },
                            onOpenAdblockDialog = { showAdblockDialog = true },
                            onToggleWebDarkMode = onToggleWebDarkMode,
                            onToggleDesktopMode = onToggleDesktopMode,
                            onToggleAdBlock = onToggleAdBlock,
                            onSetThemeMode = onSetThemeMode,
                            onSetColorSeed = onSetColorSeed
                        )
                    }
                )

                if (!isUrlBarFocused) {
                    BrowserTabGrid(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onSelectTab = onSelectTab,
                        onCloseTab = onCloseTab,
                        onNewTab = { url, incog -> onNewTab(url, incog, false) }
                    )
                }
            }
        }

        // Multi-Tab WebView Pool: Keeps all opened tab WebViews alive in memory
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(webViewBgArgb))
        ) {
            tabs.forEach { tab ->
                key(tab.id) {
                    val isVisible = tab.id == activeTabId
                    val isPageReady = readyTabs[tab.id] == true
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (isVisible && isPageReady) 1f else 0f)
                            .zIndex(if (isVisible) 1f else 0f)
                    ) {
                        AndroidView(
                            factory = { context ->
                                val density = context.resources.displayMetrics.density
                                val minScrollablePx = (140f * density).toInt()
                                val toggleSlopPx = 56f * density
                                var touchAccumDy = 0f
                                var lastTouchY = 0f
                                var isTrackingTouch = false

                                val webView = object : WebView(context) {
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
                                    addJavascriptInterface(
                                        com.siphon.app.core.sniffer.SiphonSnifferBridge(snifferClient),
                                        "__siphon_sniffer__"
                                    )
                                    webViewClient = snifferClient
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
                                                if (!vUrl.isNullOrBlank() && !isUrlBarFocused && vUrl != urlTextFieldValue.text) {
                                                    urlTextFieldValue = TextFieldValue(vUrl)
                                                    onNavigateUrl(vUrl)
                                                }
                                                val vTitle = view?.title.orEmpty()
                                                if (!vUrl.isNullOrBlank()) {
                                                    onUpdateTabMeta(vUrl, vTitle)
                                                }
                                            }
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
                                    }
                                }
                                webViewMap[tab.id] = webView
                                webView.loadUrl(tab.url)
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
                onDismiss = { showAdblockDialog = false },
                onApply = { config ->
                    onRefreshAdblock(config)
                    showAdblockDialog = false
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
