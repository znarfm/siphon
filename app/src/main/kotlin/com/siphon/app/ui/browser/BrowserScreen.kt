package com.siphon.app.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.AlertDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import com.siphon.app.data.db.BookmarkEntity
import com.siphon.app.data.db.HistoryEntity
import com.siphon.app.core.adblock.FastHostFilter
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.siphon.app.core.sniffer.MediaSnifferClient
import com.siphon.app.data.db.DownloadEntity
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

enum class QuickSearchFilter {
    ALL, BOOKMARKS, HISTORY
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
    onToggleBars: (Boolean) -> Unit = {},
    onToggleWebDarkMode: () -> Unit = {},
    onToggleDesktopMode: () -> Unit = {},
    onSetThemeMode: (AppThemeMode) -> Unit = {},
    onSetColorSeed: (AppColorSeed) -> Unit = {},
    onNavigateUrl: (String) -> Unit,
    onToggleAdBlock: () -> Unit,
    onOpenSniffer: () -> Unit,
    onCancelRebind: () -> Unit,
    onNewTab: (url: String, isIncognito: Boolean) -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onToggleBookmark: () -> Unit = {},
    onDeleteBookmark: (Long) -> Unit = {},
    onDeleteHistory: (Long) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onRefreshAdblock: (String) -> Unit = {},
    onUpdateTabMeta: (url: String, title: String) -> Unit,
    onRegisterDomScan: (((() -> Unit) -> Unit))? = null,
    modifier: Modifier = Modifier
) {
    val webViewMap = remember { mutableMapOf<String, WebView>() }
    val readyTabs = remember { mutableStateMapOf<String, Boolean>() }
    var urlTextFieldValue by remember { mutableStateOf(TextFieldValue(currentUrl)) }
    var isUrlBarFocused by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var showAdblockDialog by remember { mutableStateOf(false) }
    var searchFilter by remember { mutableStateOf(QuickSearchFilter.ALL) }
    var pageProgress by remember { mutableIntStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var pendingPopupUrl by remember { mutableStateOf<String?>(null) }
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

        // Top Browser Bar: Controls, URL Bar, Ad-Block toggle, Sniffer trigger
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isUrlBarFocused && canGoBack) {
                        IconButton(
                            onClick = { activeWebView?.goBack() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Sleek Expressive URL Bar Capsule
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onToggleAdBlock,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Text(
                                    text = "🛡",
                                    fontSize = 13.sp,
                                    color = if (isAdBlockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            BasicTextField(
                                value = urlTextFieldValue,
                                onValueChange = { newValue ->
                                    if (!isUrlBarFocused) {
                                        isUrlBarFocused = true
                                        val text = newValue.text
                                        urlTextFieldValue = newValue.copy(
                                            selection = TextRange(0, text.length)
                                        )
                                    } else {
                                        urlTextFieldValue = newValue
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { focusState ->
                                        isUrlBarFocused = focusState.isFocused
                                        if (focusState.isFocused) {
                                            onToggleBars(true)
                                            val text = urlTextFieldValue.text
                                            urlTextFieldValue = urlTextFieldValue.copy(
                                                selection = TextRange(0, text.length)
                                            )
                                        }
                                    },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        isUrlBarFocused = false
                                        focusManager.clearFocus()
                                        val target = sanitizeUrl(urlTextFieldValue.text)
                                        onNavigateUrl(target)
                                        activeWebView?.loadUrl(target)
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (urlTextFieldValue.text.isEmpty()) {
                                            Text(
                                                "Search or type URL",
                                                maxLines = 1,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            IconButton(
                                onClick = onToggleBookmark,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(
                                    text = if (isCurrentBookmarked) "★" else "☆",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }

                            if (isUrlBarFocused) {
                                IconButton(
                                    onClick = { urlTextFieldValue = TextFieldValue("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("✕", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                IconButton(
                                    onClick = { activeWebView?.reload() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("⟳", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (isUrlBarFocused) {
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = {
                                isUrlBarFocused = false
                                focusManager.clearFocus()
                                val cur = activeWebView?.url ?: currentUrl
                                urlTextFieldValue = TextFieldValue(cur)
                            }
                        ) {
                            Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(6.dp))

                        // Sniffer catch button
                        if (detectedMediaCount > 0) {
                            FilledTonalButton(
                                onClick = onOpenSniffer,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = CircleShape,
                                modifier = Modifier.height(34.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                            ) {
                                Text("↓ $detectedMediaCount", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            IconButton(
                                onClick = onOpenSniffer,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Text("↓", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }

                        // Overflow menu button
                        Box {
                            IconButton(
                                onClick = { isMenuOpen = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Text("⋮", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }

                            DropdownMenu(
                                expanded = isMenuOpen,
                                onDismissRequest = { isMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("🕶 New Incognito Tab", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        isMenuOpen = false
                                        onNewTab("https://google.com", true)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("⭐ Bookmarks & History", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        isMenuOpen = false
                                        isUrlBarFocused = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🛡 Adblock Rules", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("$adblockRuleCount", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        isMenuOpen = false
                                        showAdblockDialog = true
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🌙 Web Dark Mode", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Switch(
                                                checked = isWebDarkMode,
                                                onCheckedChange = { onToggleWebDarkMode() },
                                                modifier = Modifier.scale(0.8f)
                                            )
                                        }
                                    },
                                    onClick = onToggleWebDarkMode
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🖥 Desktop Site", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Switch(
                                                checked = isDesktopMode,
                                                onCheckedChange = { onToggleDesktopMode() },
                                                modifier = Modifier.scale(0.8f)
                                            )
                                        }
                                    },
                                    onClick = onToggleDesktopMode
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Text(
                                    text = "🎨 App Theme",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AppThemeMode.entries.forEach { mode ->
                                        val isSelected = mode == themeMode
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onSetThemeMode(mode) },
                                            label = { Text(mode.title, fontSize = 10.sp) },
                                            shape = CircleShape,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }

                                val availableSeeds = remember {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        AppColorSeed.entries
                                    } else {
                                        AppColorSeed.entries.filter { it != AppColorSeed.DYNAMIC }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    availableSeeds.forEach { seed ->
                                        val effectiveSelected = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && colorSeed == AppColorSeed.DYNAMIC) {
                                            seed == AppColorSeed.SAPPHIRE
                                        } else {
                                            seed == colorSeed
                                        }
                                        if (seed == AppColorSeed.DYNAMIC) {
                                            FilterChip(
                                                selected = effectiveSelected,
                                                onClick = { onSetColorSeed(seed) },
                                                label = { Text("Monet", fontSize = 10.sp) },
                                                shape = CircleShape,
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(seed.swatch)
                                                    .border(
                                                        width = if (effectiveSelected) 2.5.dp else 1.dp,
                                                        color = if (effectiveSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { onSetColorSeed(seed) }
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🛡 Ad Blocker", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Switch(
                                                checked = isAdBlockEnabled,
                                                onCheckedChange = { onToggleAdBlock() },
                                                modifier = Modifier.scale(0.8f)
                                            )
                                        }
                                    },
                                    onClick = onToggleAdBlock
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                DropdownMenuItem(
                                    text = { Text("📋 Copy Link", fontSize = 13.sp) },
                                    onClick = {
                                        isMenuOpen = false
                                        val url = activeWebView?.url ?: currentUrl
                                        clipboardManager.setText(AnnotatedString(url))
                                    }
                                )
                            }
                        }
                    }
                }

                if (pageProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { pageProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    )
                }

                // Fast Search & Filter Panel (Bookmarks + History)
                if (isUrlBarFocused) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val query = urlTextFieldValue.text.trim().lowercase()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = searchFilter == QuickSearchFilter.ALL,
                                    onClick = { searchFilter = QuickSearchFilter.ALL },
                                    label = { Text("All", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = searchFilter == QuickSearchFilter.BOOKMARKS,
                                    onClick = { searchFilter = QuickSearchFilter.BOOKMARKS },
                                    label = { Text("⭐ Bookmarks (${bookmarks.size})", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = searchFilter == QuickSearchFilter.HISTORY,
                                    onClick = { searchFilter = QuickSearchFilter.HISTORY },
                                    label = { Text("🕒 History (${history.size})", fontSize = 11.sp) }
                                )
                            }
                            if (searchFilter == QuickSearchFilter.HISTORY && history.isNotEmpty()) {
                                TextButton(
                                    onClick = onClearHistory,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text("Clear All", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        val matchedBookmarks = if (searchFilter != QuickSearchFilter.HISTORY) {
                            bookmarks.filter { query.isEmpty() || it.title.lowercase().contains(query) || it.url.lowercase().contains(query) }
                        } else emptyList()

                        val matchedHistory = if (searchFilter != QuickSearchFilter.BOOKMARKS) {
                            history.filter { query.isEmpty() || it.title.lowercase().contains(query) || it.url.lowercase().contains(query) }
                        } else emptyList()

                        if (matchedBookmarks.isEmpty() && matchedHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No matching bookmarks or history", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp)
                            ) {
                                if (matchedBookmarks.isNotEmpty()) {
                                    items(matchedBookmarks, key = { "bm_${it.id}" }) { bm ->
                                        SearchResultItemRow(
                                            icon = "⭐",
                                            title = bm.title,
                                            url = bm.url,
                                            onSelect = {
                                                isUrlBarFocused = false
                                                focusManager.clearFocus()
                                                urlTextFieldValue = TextFieldValue(bm.url)
                                                onNavigateUrl(bm.url)
                                                activeWebView?.loadUrl(bm.url)
                                            },
                                            onDelete = { onDeleteBookmark(bm.id) }
                                        )
                                    }
                                }

                                if (matchedHistory.isNotEmpty()) {
                                    items(matchedHistory, key = { "hi_${it.id}" }) { hi ->
                                        SearchResultItemRow(
                                            icon = "🕒",
                                            title = hi.title,
                                            url = hi.url,
                                            onSelect = {
                                                isUrlBarFocused = false
                                                focusManager.clearFocus()
                                                urlTextFieldValue = TextFieldValue(hi.url)
                                                onNavigateUrl(hi.url)
                                                activeWebView?.loadUrl(hi.url)
                                            },
                                            onDelete = { onDeleteHistory(hi.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Horizontal Tab Bar Strip (sleek, compact)
                if (!isUrlBarFocused) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(tabs, key = { it.id }) { tab ->
                            val isSelected = tab.id == activeTabId
                            val tabBg = when {
                                tab.isIncognito && isSelected -> Color(0xFF333333)
                                tab.isIncognito -> Color(0xFF222222)
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                            }
                            val tabFg = when {
                                tab.isIncognito -> Color.White
                                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                shape = CircleShape,
                                color = tabBg,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onSelectTab(tab.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = (if (tab.isIncognito) "🕶 " else "") + tab.title.ifBlank { if (tab.isIncognito) "Private" else "New Tab" },
                                        maxLines = 1,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = tabFg,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 110.dp)
                                    )
                                    if (tabs.size > 1) {
                                        Text(
                                            text = "✕",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = tabFg,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable { onCloseTab(tab.id) }
                                                .padding(2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .clickable { onNewTab("https://google.com", false) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("+", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .clickable { onNewTab("https://google.com", true) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🕶", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
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
                                                        // deltaY < 0 means finger dragging up (scrolling down)
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
                                            }
                                            if (view != null && newProgress >= 10) {
                                                if (currentWebDarkMode.value) {
                                                    applyWebDarkModeToWebView(view, true)
                                                }
                                                val domain = runCatching { Uri.parse(view.url).host }.getOrNull()
                                                view.evaluateJavascript(com.siphon.app.core.sniffer.SnifferJsInjector.buildUserscript(domain), null)
                                            }
                                        }

                                        override fun onReceivedTitle(view: WebView?, title: String?) {
                                            val vUrl = view?.url ?: tab.url
                                            val vTitle = title.orEmpty()
                                            onUpdateTabMeta(vUrl, vTitle)
                                            if (tab.id == currentActiveTabId.value && !vUrl.isNullOrBlank() && !isUrlBarFocused && vUrl != urlTextFieldValue.text) {
                                                urlTextFieldValue = TextFieldValue(vUrl)
                                                onNavigateUrl(vUrl)
                                            }
                                        }

                                        override fun onCreateWindow(
                                            view: WebView?,
                                            isDialog: Boolean,
                                            isUserGesture: Boolean,
                                            resultMsg: Message?
                                        ): Boolean {
                                            if (resultMsg == null || view == null) return false
                                            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                                            val tempWebView = WebView(view.context).apply {
                                                settings.javaScriptEnabled = true
                                                webViewClient = object : WebViewClient() {
                                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                        request?.url?.toString()?.takeIf { it.isNotBlank() }?.let {
                                                            pendingPopupUrl = it
                                                        }
                                                        post { destroy() }
                                                        return true
                                                    }

                                                    @Deprecated("Deprecated in Java")
                                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                                        url?.takeIf { it.isNotBlank() }?.let {
                                                            pendingPopupUrl = it
                                                        }
                                                        post { destroy() }
                                                        return true
                                                    }

                                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                        url?.takeIf { it.isNotBlank() }?.let {
                                                            pendingPopupUrl = it
                                                        }
                                                        post { destroy() }
                                                    }
                                                }
                                            }
                                            transport.webView = tempWebView
                                            resultMsg.sendToTarget()
                                            return true
                                        }
                                    }
                                    setOnScrollChangeListener { _, _, scrollY, _, _ ->
                                        if (tab.id == currentActiveTabId.value && scrollY <= 10 && !currentBarsVisible.value) {
                                            onToggleBars(true)
                                        }
                                    }
                                    setDownloadListener { downloadUrl, userAgent, _, _, _ ->
                                        val headers = if (!userAgent.isNullOrBlank()) mapOf("User-Agent" to userAgent) else emptyMap()
                                        snifferClient.inspectCandidate(downloadUrl, headers, forceInspect = true)
                                        onOpenSniffer()
                                    }
                                    loadUrl(tab.url)
                                }
                                webViewMap[tab.id] = webView
                                webView
                            },
                            update = { webView ->
                                webView.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
                                webView.setBackgroundColor(webViewBgArgb)
                                webViewMap[tab.id] = webView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (pendingPopupUrl != null) {
            val urlToOpen = pendingPopupUrl!!
            AlertDialog(
                onDismissRequest = { pendingPopupUrl = null },
                title = { Text("Open Popup") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("A page wants to open:")
                        Text(
                            text = urlToOpen,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onNewTab(urlToOpen, false)
                            pendingPopupUrl = null
                        }
                    ) {
                        Text("New Tab")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { pendingPopupUrl = null }) {
                            Text("Cancel")
                        }
                        FilledTonalButton(
                            onClick = {
                                activeWebView?.loadUrl(urlToOpen)
                                pendingPopupUrl = null
                            }
                        ) {
                            Text("Current Tab")
                        }
                    }
                }
            )
        }

        if (showAdblockDialog) {
            AdblockSubscriptionDialog(
                currentRuleCount = adblockRuleCount,
                onDismiss = { showAdblockDialog = false },
                onRefresh = { url ->
                    onRefreshAdblock(url)
                    showAdblockDialog = false
                }
            )
        }
    }
}

@Composable
private fun AdblockSubscriptionDialog(
    currentRuleCount: Int,
    onDismiss: () -> Unit,
    onRefresh: (String) -> Unit
) {
    val presets = listOf(
        "AdAway Hosts (Recommended)" to FastHostFilter.DEFAULT_SUBSCRIPTION_URL,
        "AdGuard Mobile Filter" to "https://filters.adtidy.org/extension/chromium/filters/11.txt",
        "EasyList Standard" to "https://easylist.to/easylist/easylist.txt"
    )
    var selectedUrl by remember { mutableStateOf(presets.first().second) }
    var customUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adblock Subscriptions", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Active Rules: $currentRuleCount hosts blocked",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Select Subscription Source:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                presets.forEach { (name, url) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedUrl = url
                                customUrl = ""
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUrl == url && customUrl.isEmpty(),
                            onClick = {
                                selectedUrl = url
                                customUrl = ""
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(name, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = {
                        customUrl = it
                        selectedUrl = it
                    },
                    label = { Text("Custom Filter / Hosts URL", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = customUrl.ifBlank { selectedUrl }
                    onRefresh(target)
                }
            ) {
                Text("Refresh & Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun sanitizeUrl(input: String): String {
    val trimmed = input.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
        else -> "https://www.google.com/search?q=" + Uri.encode(trimmed)
    }
}

@Composable
private fun SearchResultItemRow(
    icon: String,
    title: String,
    url: String,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title.ifBlank { url },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = url,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(22.dp)
        ) {
            Text("✕", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
