package com.znarfm.siphon.core.adblock

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import com.znarfm.siphon.core.network.NetworkClients
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

data class AdblockConfig(
    val baseUrl: String = FastHostFilter.HAGEZI_NORMAL_URL,
    val allowShorteners: Boolean = true,
    val enableThreatIntel: Boolean = false,
    val customUrl: String = ""
) {
    fun toBlockUrls(): List<String> = buildList {
        if (baseUrl.isNotBlank()) add(baseUrl.trim())
        if (enableThreatIntel) add(FastHostFilter.HAGEZI_THREAT_URL)
        if (customUrl.isNotBlank()) add(customUrl.trim())
    }

    fun toAllowUrls(): List<String> = buildList {
        if (allowShorteners) add(FastHostFilter.DANDELION_SHORTENERS_URL)
    }
}

class FastHostFilter(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    @Volatile
    private var blockedHosts: HashSet<String> = HashSet(0)

    @Volatile
    private var allowedHosts: HashSet<String> = HashSet(0)

    val isLoaded = AtomicBoolean(false)
    var isEnabled: Boolean = true
    val ruleCount: Int get() = blockedHosts.size

    fun loadFromAssets(context: Context, assetPath: String = "adblock_hosts.txt") {
        if (isLoaded.get()) return
        scope.launch {
            val blockFile = File(context.filesDir, "adblock_subscription.txt")
            val allowFile = File(context.filesDir, "adblock_allowlist.txt")

            if (allowFile.exists() && allowFile.length() > 0L) {
                runCatching {
                    allowFile.inputStream().use { loadAllowlistFromStream(it) }
                }
            }

            if (blockFile.exists() && blockFile.length() > 0L) {
                runCatching {
                    blockFile.inputStream().use { loadFromStream(it) }
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

    suspend fun refreshSubscriptions(
        context: Context,
        blockUrls: List<String>,
        allowUrls: List<String>
    ): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        runCatching {
            val blockFile = File(context.filesDir, "adblock_subscription.txt")
            val allowFile = File(context.filesDir, "adblock_allowlist.txt")

            val fetchedBlocks = coroutineScope {
                blockUrls.filter { it.isNotBlank() }.map { url ->
                    val cleanUrl = url.trim().replace("/refs/heads/", "/")
                    async {
                        runCatching { fetchAndParse(cleanUrl, isAllow = false) }
                            .getOrElse { emptySet() }
                    }
                }.awaitAll()
            }

            val fetchedAllows = coroutineScope {
                allowUrls.filter { it.isNotBlank() }.map { url ->
                    val cleanUrl = url.trim().replace("/refs/heads/", "/")
                    async {
                        runCatching { fetchAndParse(cleanUrl, isAllow = true) }
                            .getOrElse { emptySet() }
                    }
                }.awaitAll()
            }

            val newBlocked = fetchedBlocks.flatten().toHashSet()
            val newAllowed = fetchedAllows.flatten().toHashSet()

            if (newBlocked.isEmpty() && blockUrls.any { it.isNotBlank() }) {
                throw IOException("Failed to load rules: network unreachable or invalid blocklist URL")
            }

            // Atomic pointer swap
            blockedHosts = newBlocked
            allowedHosts = newAllowed
            isLoaded.set(true)

            // Cache to disk
            blockFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                for (host in newBlocked) {
                    writer.write(host)
                    writer.newLine()
                }
            }
            allowFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                for (host in newAllowed) {
                    writer.write(host)
                    writer.newLine()
                }
            }

            Pair(newBlocked.size, newAllowed.size)
        }
    }

    private fun fetchAndParse(url: String, isAllow: Boolean): Set<String> {
        val cleanUrl = url.trim().replace("/refs/heads/", "/")
        val result = runCatching { doNetworkFetch(cleanUrl, isAllow) }
        if (result.isSuccess) return result.getOrThrow()

        // Fallback to jsDelivr CDN if raw.githubusercontent.com is blocked
        if (cleanUrl.contains("raw.githubusercontent.com/")) {
            val cdnFallback = when (cleanUrl) {
                "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/domains/normal.txt" -> HAGEZI_NORMAL_URL
                "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/domains/light.txt" -> HAGEZI_LIGHT_URL
                "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/domains/pro.txt" -> HAGEZI_PRO_URL
                "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/domains/fake.txt" -> HAGEZI_THREAT_URL
                "https://raw.githubusercontent.com/DandelionSprout/adfilt/master/LegitimateURLShortener.txt" -> DANDELION_SHORTENERS_URL
                else -> null
            }
            if (cdnFallback != null) {
                return doNetworkFetch(cdnFallback, isAllow)
            }
        }
        throw result.exceptionOrNull() ?: IOException("Failed to load $url")
    }

    private fun doNetworkFetch(url: String, isAllow: Boolean): Set<String> {
        val req = Request.Builder().url(url).build()
        val set = HashSet<String>(1024)
        NetworkClients.defaultClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $url")
            resp.body?.byteStream()?.let { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        val host = if (isAllow) extractAllowHostFromRule(line) else extractHostFromRule(line)
                        if (host != null) {
                            set.add(host)
                        }
                        line = reader.readLine()
                    }
                }
            }
        }
        return set
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

    suspend fun loadAllowlistFromStream(stream: InputStream) = withContext(Dispatchers.Default) {
        val lines = ArrayList<String>(1024)
        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                val host = extractAllowHostFromRule(line)
                if (host != null) {
                    lines.add(host)
                }
                line = reader.readLine()
            }
        }
        val set = HashSet<String>(lines.size * 4 / 3 + 1)
        set.addAll(lines)
        allowedHosts = set
    }

    fun isAllowedHost(rawHost: String?): Boolean {
        if (!isEnabled || rawHost.isNullOrBlank()) return false
        val host = rawHost.trim().lowercase().trimEnd('.')
        val set = allowedHosts
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

    fun shouldBlockHost(rawHost: String?): Boolean {
        if (!isEnabled || !isLoaded.get() || rawHost.isNullOrBlank()) return false
        val host = rawHost.trim().lowercase().trimEnd('.')
        if (isAllowedHost(host)) return false

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

    fun shouldBlockUri(uri: Uri?): Boolean {
        if (!isEnabled || uri == null) return false
        val path = uri.path?.lowercase().orEmpty()
        if (path.endsWith("/pagead.js") || path.endsWith("/ads.js") || path.contains("/widget/ads")) {
            return true
        }
        val host = uri.host?.lowercase()
        if (!host.isNullOrBlank()) {
            return shouldBlockHost(host)
        }
        return false
    }

    fun shouldBlockUrl(url: String?): Boolean {
        if (!isEnabled || url.isNullOrBlank()) return false
        val lower = url.lowercase()
        if (lower.contains("/pagead.js") || lower.contains("/widget/ads") || lower.contains("/ads.js")) {
            return true
        }
        val host = runCatching {
            val uri = Uri.parse(url)
            uri.host?.lowercase()
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: runCatching {
            val withoutScheme = if (url.contains("://")) url.substringAfter("://") else url.trimStart('/')
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
            403,
            "Blocked",
            emptyMap(),
            ByteArrayInputStream(ByteArray(0))
        )
    }

    companion object {
        const val HAGEZI_NORMAL_URL = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/adblock/multi.txt"
        const val HAGEZI_LIGHT_URL = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/adblock/light.txt"
        const val HAGEZI_PRO_URL = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/adblock/pro.txt"
        const val HAGEZI_THREAT_URL = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/adblock/fake.txt"
        const val DANDELION_SHORTENERS_URL = "https://cdn.jsdelivr.net/gh/DandelionSprout/adfilt@master/LegitimateURLShortener.txt"

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

        fun extractAllowHostFromRule(line: String): String? {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("#") || trimmed.startsWith("[")) {
                return null
            }
            if (trimmed.contains("##") || trimmed.contains("#@#") || trimmed.contains("#?#")) {
                return null
            }

            // ABP allowlist syntax: @@||domain.com^ or @@domain.com^
            if (trimmed.startsWith("@@")) {
                val afterPrefix = trimmed.removePrefix("@@").removePrefix("||")
                val hostPart = afterPrefix.takeWhile { it != '^' && it != '/' && it != '$' && it != '?' && it != ':' }
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
}
