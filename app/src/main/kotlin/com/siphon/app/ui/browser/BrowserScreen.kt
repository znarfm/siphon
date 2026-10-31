package com.siphon.app.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.siphon.app.core.sniffer.MediaSnifferClient
import com.siphon.app.data.db.DownloadEntity

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
    onNavigateUrl: (String) -> Unit,
    onToggleAdBlock: () -> Unit,
    onOpenSniffer: () -> Unit,
    onCancelRebind: () -> Unit,
    onNewTab: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onUpdateTabMeta: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val webViewMap = remember { mutableMapOf<String, WebView>() }
    var urlTextFieldValue by remember { mutableStateOf(TextFieldValue(currentUrl)) }
    var isUrlBarFocused by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableIntStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val activeWebView = webViewMap[activeTabId]

    LaunchedEffect(activeTabId, currentUrl) {
        val currentActiveUrl = activeWebView?.url ?: currentUrl
        if (!isUrlBarFocused) {
            urlTextFieldValue = TextFieldValue(currentActiveUrl)
        }
    }

    LaunchedEffect(snifferClient) {
        snifferClient.onUrlChanged = { newUrl ->
            if (!isUrlBarFocused) {
                urlTextFieldValue = TextFieldValue(newUrl)
            }
            val title = activeWebView?.title.orEmpty()
            onUpdateTabMeta(newUrl, title)
        }
        snifferClient.onNavigationStateChanged = { back, forward ->
            canGoBack = back
            canGoForward = forward
        }
    }

    BackHandler(enabled = isUrlBarFocused || canGoBack) {
        if (isUrlBarFocused) {
            focusManager.clearFocus()
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isUrlBarFocused) {
                        // Navigation controls
                        FilledTonalButton(
                            onClick = { activeWebView?.goBack() },
                            enabled = canGoBack,
                            modifier = Modifier.size(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("<", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        FilledTonalButton(
                            onClick = { activeWebView?.goForward() },
                            enabled = canGoForward,
                            modifier = Modifier.size(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text(">", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        FilledTonalButton(
                            onClick = { activeWebView?.reload() },
                            modifier = Modifier.size(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("⟳", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Address bar (expands to full width when focused)
                    OutlinedTextField(
                        value = urlTextFieldValue,
                        onValueChange = { urlTextFieldValue = it },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                isUrlBarFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    // Highlight/select all text upon focus
                                    val text = urlTextFieldValue.text
                                    urlTextFieldValue = urlTextFieldValue.copy(
                                        selection = TextRange(0, text.length)
                                    )
                                }
                            },
                        singleLine = true,
                        placeholder = { Text("Search or type URL", maxLines = 1, fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                val target = sanitizeUrl(urlTextFieldValue.text)
                                onNavigateUrl(target)
                                activeWebView?.loadUrl(target)
                            }
                        )
                    )

                    if (isUrlBarFocused) {
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                val cur = activeWebView?.url ?: currentUrl
                                urlTextFieldValue = TextFieldValue(cur)
                            }
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(6.dp))

                        // Ad-Block Toggle
                        Button(
                            onClick = onToggleAdBlock,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAdBlockEnabled) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (isAdBlockEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = if (isAdBlockEnabled) "Shield ON" else "Shield OFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Sniffer Sheet Trigger Badge
                        BadgedBox(
                            badge = {
                                if (detectedMediaCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White
                                    ) {
                                        Text(detectedMediaCount.toString())
                                    }
                                }
                            }
                        ) {
                            Button(
                                onClick = onOpenSniffer,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (detectedMediaCount > 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("↓", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

                // Horizontal Tab Bar Strip
                if (!isUrlBarFocused) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(tabs, key = { it.id }) { tab ->
                            val isSelected = tab.id == activeTabId
                            InputChip(
                                selected = isSelected,
                                onClick = { onSelectTab(tab.id) },
                                label = {
                                    Text(
                                        text = tab.title,
                                        maxLines = 1,
                                        fontSize = 11.sp,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 100.dp)
                                    )
                                },
                                trailingIcon = {
                                    Text(
                                        text = "✕",
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .padding(start = 2.dp)
                                            .clickable { onCloseTab(tab.id) }
                                    )
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }

                        item {
                            FilledTonalButton(
                                onClick = onNewTab,
                                modifier = Modifier.size(30.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Multi-Tab WebView Pool: Keeps all opened tab WebViews alive in memory
        Box(modifier = Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
                val isVisible = tab.id == activeTabId
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (isVisible) 1f else 0f)
                        .zIndex(if (isVisible) 1f else 0f)
                ) {
                    AndroidView(
                        factory = { context ->
                            val webView = WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    mediaPlaybackRequiresUserGesture = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                }
                                webViewClient = snifferClient
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        if (tab.id == activeTabId) {
                                            pageProgress = newProgress
                                            canGoBack = view?.canGoBack() == true
                                            canGoForward = view?.canGoForward() == true
                                            val vUrl = view?.url
                                            if (!vUrl.isNullOrBlank() && !isUrlBarFocused && vUrl != urlTextFieldValue.text) {
                                                urlTextFieldValue = TextFieldValue(vUrl)
                                                onNavigateUrl(vUrl)
                                            }
                                        }
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        val vUrl = view?.url ?: tab.url
                                        val vTitle = title.orEmpty()
                                        onUpdateTabMeta(vUrl, vTitle)
                                        if (tab.id == activeTabId && !vUrl.isNullOrBlank() && !isUrlBarFocused && vUrl != urlTextFieldValue.text) {
                                            urlTextFieldValue = TextFieldValue(vUrl)
                                            onNavigateUrl(vUrl)
                                        }
                                    }
                                }
                                loadUrl(tab.url)
                            }
                            webViewMap[tab.id] = webView
                            webView
                        },
                        update = { webView ->
                            webViewMap[tab.id] = webView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun sanitizeUrl(input: String): String {
    val trimmed = input.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
        else -> "https://www.google.com/search?q=" + Uri.encode(trimmed)
    }
}
