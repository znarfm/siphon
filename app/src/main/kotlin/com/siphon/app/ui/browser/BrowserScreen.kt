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
import androidx.compose.ui.Alignment
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
    snifferClient: MediaSnifferClient,
    detectedMediaCount: Int,
    isAdBlockEnabled: Boolean,
    rebindTarget: DownloadEntity?,
    onNavigateUrl: (String) -> Unit,
    onToggleAdBlock: () -> Unit,
    onOpenSniffer: () -> Unit,
    onCancelRebind: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var urlInput by remember { mutableStateOf(currentUrl) }
    var pageProgress by remember { mutableIntStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(snifferClient) {
        snifferClient.onUrlChanged = { newUrl ->
            urlInput = newUrl
            onNavigateUrl(newUrl)
        }
        snifferClient.onNavigationStateChanged = { back, forward ->
            canGoBack = back
            canGoForward = forward
        }
    }

    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }

    LaunchedEffect(currentUrl) {
        if (webViewInstance != null) {
            val currentWebUrl = webViewInstance?.url
            if (currentWebUrl == null || (currentWebUrl != currentUrl && !currentUrl.startsWith(currentWebUrl))) {
                webViewInstance?.loadUrl(currentUrl)
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
                    // Navigation controls
                    FilledTonalButton(
                        onClick = { webViewInstance?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.size(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("<", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    FilledTonalButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.size(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(">", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    FilledTonalButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier.size(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("⟳", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Address bar
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Search or type URL", maxLines = 1, fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                val target = sanitizeUrl(urlInput)
                                onNavigateUrl(target)
                                webViewInstance?.loadUrl(target)
                            }
                        )
                    )

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

                if (pageProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { pageProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    )
                }
            }
        }

        // WebView Container
        AndroidView(
            factory = { context ->
                WebView(context).apply {
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
                            pageProgress = newProgress
                            canGoBack = view?.canGoBack() == true
                            canGoForward = view?.canGoForward() == true
                            val vUrl = view?.url
                            if (!vUrl.isNullOrBlank() && vUrl != urlInput) {
                                urlInput = vUrl
                                onNavigateUrl(vUrl)
                            }
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            val vUrl = view?.url
                            if (!vUrl.isNullOrBlank() && vUrl != urlInput) {
                                urlInput = vUrl
                                onNavigateUrl(vUrl)
                            }
                        }
                    }
                    loadUrl(currentUrl)
                    webViewInstance = this
                }
            },
            update = { webView ->
                webViewInstance = webView
            },
            modifier = Modifier.fillMaxSize()
        )
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
