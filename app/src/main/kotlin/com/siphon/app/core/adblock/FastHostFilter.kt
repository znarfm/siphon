package com.siphon.app.core.adblock

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import com.siphon.app.core.network.NetworkClients
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
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
    val ruleCount: Int get() = blockedHosts.size

    fun loadFromAssets(context: Context, assetPath: String = "adblock_hosts.txt") {
        if (isLoaded.get()) return
        scope.launch {
            val file = File(context.filesDir, "adblock_subscription.txt")
            if (file.exists() && file.length() > 0L) {
                runCatching {
                    file.inputStream().use { loadFromStream(it) }
                }
            }
            if (!isLoaded.get() || blockedHosts.isEmpty()) {
                runCatching {
                    context.assets.open(assetPath).use { stream ->
                        loadFromStream(stream)
                    }
                }
            }
        }
    }

    suspend fun refreshSubscription(
        context: Context,
        url: String = DEFAULT_SUBSCRIPTION_URL
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(url).build()
            val file = File(context.filesDir, "adblock_subscription.txt")
            NetworkClients.defaultClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                resp.body?.byteStream()?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
            }
            file.inputStream().use { stream ->
                loadFromStream(stream)
            }
            blockedHosts.size
        }
    }

    suspend fun loadFromStream(stream: InputStream) = withContext(Dispatchers.Default) {
        val lines = ArrayList<String>(1024)
        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                val host = extractHostFromRule(line)
                if (host != null) {
                    lines.add(host)
                }
                line = reader.readLine()
            }
        }
        val set = HashSet<String>(lines.size * 4 / 3 + 1)
        set.addAll(lines)
        blockedHosts = set
        isLoaded.set(true)
    }

    companion object {
        const val DEFAULT_SUBSCRIPTION_URL = "https://adaway.org/hosts.txt"

        fun extractHostFromRule(line: String): String? {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("#") || trimmed.startsWith("[")) {
                return null
            }
            if (trimmed.contains("##") || trimmed.contains("#@#") || trimmed.contains("#?#") || trimmed.startsWith("@@")) {
                return null
            }

            // ABP/AdGuard rule syntax: ||domain.com^ or ||domain.com/
            if (trimmed.startsWith("||")) {
                val afterBars = trimmed.substring(2)
                val hostPart = afterBars.takeWhile { it != '^' && it != '/' && it != '$' && it != '?' && it != ':' }
                val cleanHost = hostPart.trim().lowercase().trimEnd('.')
                return if (cleanHost.contains('.') && !cleanHost.contains('*')) cleanHost else null
            }

            // Standard hosts syntax: 0.0.0.0 domain.com or 127.0.0.1 domain.com
            if (trimmed.startsWith("0.0.0.0 ") || trimmed.startsWith("127.0.0.1 ")) {
                val hostPart = trimmed.substringAfter(' ').trim().substringBefore(' ').substringBefore('#').trim()
                val cleanHost = hostPart.lowercase().trimEnd('.')
                return if (cleanHost.contains('.') && cleanHost != "localhost") cleanHost else null
            }

            // Plain domain syntax: domain.com
            val plain = trimmed.substringBefore('#').substringBefore(' ').trim().lowercase().trimEnd('.')
            if (plain.contains('.') && !plain.contains('/') && !plain.contains(':') && !plain.contains('*')) {
                return plain
            }
            return null
        }
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
