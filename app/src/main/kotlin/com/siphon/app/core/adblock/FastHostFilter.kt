package com.siphon.app.core.adblock

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

class FastHostFilter(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    @Volatile
    private var blockedHosts: HashSet<String> = HashSet(0)

    val isLoaded = AtomicBoolean(false)
    var isEnabled: Boolean = true

    fun loadFromAssets(context: Context, assetPath: String = "adblock_hosts.txt") {
        if (isLoaded.get()) return
        scope.launch {
            runCatching {
                context.assets.open(assetPath).use { stream ->
                    loadFromStream(stream)
                }
            }
        }
    }

    suspend fun loadFromStream(stream: InputStream) = withContext(Dispatchers.Default) {
        val lines = ArrayList<String>(512)
        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                val trimmed = line.trim().lowercase()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    lines.add(trimmed)
                }
                line = reader.readLine()
            }
        }
        val set = HashSet<String>(lines.size * 4 / 3 + 1)
        set.addAll(lines)
        blockedHosts = set
        isLoaded.set(true)
    }

    /**
     * Suffix-slicing matcher:
     * e.g. "sub.ads.domain.com" -> tests "sub.ads.domain.com", "ads.domain.com", "domain.com"
     * Stops before single TLD label.
     */
    fun shouldBlockHost(rawHost: String?): Boolean {
        if (!isEnabled || !isLoaded.get() || rawHost.isNullOrBlank()) return false
        val host = rawHost.trim().lowercase().trimEnd('.')
        val set = blockedHosts

        var current = host
        while (current.contains('.')) {
            if (set.contains(current)) {
                return true
            }
            val dotIndex = current.indexOf('.')
            if (dotIndex == -1) break
            current = current.substring(dotIndex + 1)
        }
        return false
    }

    fun shouldBlockUrl(url: String?): Boolean {
        if (!isEnabled || url.isNullOrBlank()) return false
        val host = runCatching {
            val uri = Uri.parse(url)
            uri.host?.lowercase()
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: runCatching {
            val withoutScheme = if (url.contains("://")) url.substringAfter("://") else url
            val authority = withoutScheme.substringBefore('/').substringBefore('?')
            val hostPort = if (authority.contains('@')) authority.substringAfterLast('@') else authority
            if (hostPort.startsWith('[')) {
                hostPort.substringBefore(']').trimStart('[')
            } else {
                hostPort.substringBefore(':')
            }
        }.getOrNull()?.trim()?.lowercase() ?: return false
        return shouldBlockHost(host)
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            200,
            "OK",
            emptyMap(),
            ByteArrayInputStream(ByteArray(0))
        )
    }
}
