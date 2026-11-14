package com.znarfm.siphon.core.adblock

import android.content.Context
import android.webkit.WebResourceResponse
import com.znarfm.siphon.core.network.NetworkClients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class AdblockEngine(
    private val bridge: NativeAdblockBridge = NativeAdblockJni,
    private val urlFetcher: (String) -> String = ::defaultFetchUrl
) {
    private val lock = ReentrantReadWriteLock()
    private val buildMutex = Mutex()

    @Volatile
    private var engineHandle: Long = 0L

    @Volatile
    private var fallbackBlockedHosts: HashSet<String> = HashSet(0)

    @Volatile
    private var fallbackAllowedHosts: HashSet<String> = HashSet(0)

    @Volatile
    private var allowedSites: Set<String> = emptySet()

    val isLoaded = AtomicBoolean(false)

    @Volatile
    var isEnabled: Boolean = true

    @Volatile
    var ruleCount: Int = 0
        private set

    fun hasCachedRules(context: Context): Boolean {
        val rulesFile = File(context.filesDir, RULES_FILE_NAME)
        return rulesFile.exists() && rulesFile.length() > 0L
    }

    fun deleteCachedList(context: Context, listId: String) {
        runCatching {
            val file = listCacheFile(context.filesDir, listId)
            if (file.exists()) file.delete()
        }
    }

    suspend fun loadCachedOrAssets(
        context: Context,
        settings: AdblockSettings = AdblockSettings()
    ) = withContext(Dispatchers.IO) {
        buildMutex.withLock {
            updateSettings(settings)

            val datFile = File(context.filesDir, DAT_FILE_NAME)
            val rulesFile = File(context.filesDir, RULES_FILE_NAME)
            val countFile = File(context.filesDir, RULE_COUNT_FILE_NAME)

            if (datFile.exists() && datFile.length() > 0L && bridge.isLoaded) {
                val loadedFromDat = runCatching {
                    val bytes = datFile.readBytes()
                    val handle = bridge.createFromDat(bytes)
                    if (handle != 0L) {
                        val count = readCachedRuleCount(countFile)
                            ?: settings.enabledLists.filter { it.isEnabled }.sumOf { it.ruleCount }
                        swapEngineHandle(handle, HashSet(0), HashSet(0), count)
                        true
                    } else {
                        false
                    }
                }.getOrDefault(false)

                if (loadedFromDat) return@withLock
            }

            if (rulesFile.exists() && rulesFile.length() > 0L) {
                val loadedFromRules = runCatching {
                    val rulesText = rulesFile.readText(Charsets.UTF_8)
                    loadRulesInternal(rulesText, datFileToWrite = datFile)
                    true
                }.getOrDefault(false)

                if (loadedFromRules) return@withLock
            }

            runCatching {
                val assetRules = context.assets.open(ASSETS_FILE_NAME).bufferedReader(Charsets.UTF_8).use {
                    it.readText()
                }
                val merged = mergeRulesWithUserConfig(
                    listContents = listOf(assetRules),
                    customUserRules = settings.customUserRules,
                    allowedSites = settings.allowedSites
                )
                loadRulesInternal(merged, datFileToWrite = null)
            }
        }
    }

    fun loadRulesForTesting(rulesText: String, settings: AdblockSettings = AdblockSettings()) {
        updateSettings(settings)
        val merged = mergeRulesWithUserConfig(
            listContents = listOf(rulesText),
            customUserRules = settings.customUserRules,
            allowedSites = settings.allowedSites
        )
        loadRulesInternal(merged, datFileToWrite = null)
    }

    private fun loadRulesInternal(rulesText: String, datFileToWrite: File?) {
        val count = countValidRules(rulesText)
        val newHandle = if (bridge.isLoaded) {
            bridge.createFromRules(rulesText)
        } else {
            0L
        }
        val (blocked, allowed) = if (bridge.isLoaded && newHandle != 0L) {
            HashSet<String>(0) to HashSet<String>(0)
        } else {
            parseFallbackHostSets(rulesText)
        }

        if (newHandle != 0L && datFileToWrite != null) {
            runCatching {
                val snapshot = bridge.serialize(newHandle)
                if (snapshot != null && snapshot.isNotEmpty()) {
                    datFileToWrite.writeBytes(snapshot)
                    datFileToWrite.parentFile?.let { dir ->
                        File(dir, RULE_COUNT_FILE_NAME).writeText(count.toString(), Charsets.UTF_8)
                    }
                }
            }
        }

        swapEngineHandle(newHandle, blocked, allowed, count)
    }

    suspend fun refreshSubscriptions(
        context: Context,
        settings: AdblockSettings,
        onListProgress: ((listId: String, status: String) -> Unit)? = null
    ): Result<AdblockSettings> = refreshSubscriptionsToDir(context.filesDir, settings, onListProgress)

    suspend fun rebuildRules(
        context: Context,
        settings: AdblockSettings
    ): Result<AdblockSettings> = rebuildRulesToDir(context.filesDir, settings)

    internal suspend fun rebuildRulesToDir(
        filesDir: File,
        settings: AdblockSettings
    ): Result<AdblockSettings> = syncAndCompileLists(
        filesDir = filesDir,
        settings = settings,
        forceRefreshAll = false
    )

    internal suspend fun refreshSubscriptionsToDir(
        filesDir: File,
        settings: AdblockSettings,
        onListProgress: ((listId: String, status: String) -> Unit)? = null
    ): Result<AdblockSettings> = syncAndCompileLists(
        filesDir = filesDir,
        settings = settings,
        forceRefreshAll = true,
        onListProgress = onListProgress
    )

    private data class FetchOutcome(
        val id: String,
        val content: String?,
        val ruleCount: Int,
        val error: String?
    )

    private suspend fun syncAndCompileLists(
        filesDir: File,
        settings: AdblockSettings,
        forceRefreshAll: Boolean,
        onListProgress: ((listId: String, status: String) -> Unit)? = null
    ): Result<AdblockSettings> = withContext(Dispatchers.IO) {
        buildMutex.withLock {
            runCatching {
                val enabledSources = settings.enabledLists.filter { it.isEnabled && it.url.isNotBlank() }
                val sourcesToFetch = if (forceRefreshAll) {
                    enabledSources
                } else {
                    enabledSources.filter { source ->
                        val cacheFile = listCacheFile(filesDir, source.id)
                        !cacheFile.exists() || cacheFile.length() == 0L || source.lastUpdatedMs == 0L
                    }
                }

                val outcomes = if (sourcesToFetch.isNotEmpty()) {
                    coroutineScope {
                        sourcesToFetch.map { source ->
                            async {
                                onListProgress?.invoke(source.id, "Fetching ${source.name}...")
                                runCatching {
                                    val text = fetchWithFallback(source.url)
                                    val count = countValidRules(text)
                                    val cacheFile = listCacheFile(filesDir, source.id)
                                    cacheFile.parentFile?.mkdirs()
                                    cacheFile.writeText(text, Charsets.UTF_8)
                                    onListProgress?.invoke(source.id, "Loaded $count rules")
                                    FetchOutcome(source.id, text, count, null)
                                }.getOrElse { err ->
                                    val msg = err.message ?: "Failed to fetch"
                                    onListProgress?.invoke(source.id, "Error: $msg")
                                    FetchOutcome(source.id, null, 0, msg)
                                }
                            }
                        }.awaitAll()
                    }
                } else {
                    emptyList()
                }

                val outcomesById = outcomes.associateBy { it.id }
                val listContents = if (forceRefreshAll) {
                    outcomes.mapNotNull { it.content }
                } else {
                    enabledSources.mapNotNull { source ->
                        outcomesById[source.id]?.content ?: run {
                            val cacheFile = listCacheFile(filesDir, source.id)
                            if (cacheFile.exists() && cacheFile.length() > 0L) {
                                runCatching { cacheFile.readText(Charsets.UTF_8) }.getOrNull()
                            } else null
                        }
                    }
                }

                if (enabledSources.isNotEmpty() && listContents.isEmpty()) {
                    val firstError = outcomes.firstNotNullOfOrNull { it.error } ?: "Network unreachable"
                    throw IOException("Failed to load filter lists: $firstError")
                }

                val now = System.currentTimeMillis()
                val updatedLists = settings.enabledLists.map { source ->
                    val outcome = outcomesById[source.id]
                    when {
                        outcome == null -> source
                        outcome.content != null -> source.copy(
                            ruleCount = outcome.ruleCount,
                            lastUpdatedMs = now,
                            lastError = null
                        )
                        else -> source.copy(lastError = outcome.error)
                    }
                }

                val updatedSettings = settings.copy(enabledLists = updatedLists)
                val mergedRules = mergeRulesWithUserConfig(
                    listContents = listContents,
                    customUserRules = updatedSettings.customUserRules,
                    allowedSites = updatedSettings.allowedSites
                )

                compileAndPersistRules(filesDir, mergedRules, updatedSettings)
                updatedSettings
            }
        }
    }

    private fun compileAndPersistRules(
        filesDir: File,
        mergedRules: String,
        updatedSettings: AdblockSettings
    ) {
        val datFile = File(filesDir, DAT_FILE_NAME)
        val rulesFile = File(filesDir, RULES_FILE_NAME)
        val countFile = File(filesDir, RULE_COUNT_FILE_NAME)

        val totalCount = countValidRules(mergedRules)
        val newHandle = if (bridge.isLoaded) {
            bridge.createFromRules(mergedRules)
        } else {
            0L
        }
        val (blocked, allowed) = if (bridge.isLoaded && newHandle != 0L) {
            HashSet<String>(0) to HashSet<String>(0)
        } else {
            parseFallbackHostSets(mergedRules)
        }

        if (newHandle != 0L && mergedRules.isNotBlank()) {
            runCatching {
                val snapshot = bridge.serialize(newHandle)
                if (snapshot != null && snapshot.isNotEmpty()) {
                    datFile.writeBytes(snapshot)
                    countFile.writeText(totalCount.toString(), Charsets.UTF_8)
                }
            }
        } else if (mergedRules.isBlank()) {
            if (datFile.exists()) runCatching { datFile.delete() }
            if (countFile.exists()) runCatching { countFile.delete() }
        }

        rulesFile.writeText(mergedRules, Charsets.UTF_8)

        updateSettings(updatedSettings)
        swapEngineHandle(newHandle, blocked, allowed, totalCount)
    }

    private fun readCachedRuleCount(countFile: File): Int? {
        if (!countFile.exists() || countFile.length() == 0L) return null
        return runCatching { countFile.readText(Charsets.UTF_8).trim().toIntOrNull() }.getOrNull()
    }

    private fun listCacheFile(filesDir: File, listId: String): File {
        val safeId = listId.replace(SAFE_LIST_ID_REGEX, "_")
        return File(File(filesDir, LIST_CACHE_DIR), "$safeId.txt")
    }

    fun updateSettings(settings: AdblockSettings) {
        isEnabled = settings.isGlobalEnabled
        allowedSites = settings.allowedSites
            .mapNotNull { normalizeHost(it) }
            .toSet()
    }

    fun shouldBlockRequest(
        url: String,
        sourceUrl: String = "",
        requestType: String = "other"
    ): Boolean {
        if (!isEnabled || url.isBlank()) return false

        val sourceHost = extractHostFromUrl(sourceUrl)
        if (isSiteAllowed(sourceHost)) return false

        val urlHost = extractHostFromUrl(url)
        if ((requestType == "main_frame" || sourceUrl.isBlank()) && isSiteAllowed(urlHost)) {
            return false
        }

        lock.read {
            val handle = engineHandle
            if (bridge.isLoaded && handle != 0L) {
                return bridge.checkNetwork(handle, url, sourceUrl, requestType)
            }
        }

        return shouldBlockFallback(url, urlHost)
    }

    fun isUrlAllowed(url: String): Boolean {
        val host = extractHostFromUrl(url)
        return !host.isNullOrEmpty() && isSiteAllowed(host)
    }

    fun getCosmeticResources(url: String): CosmeticResources {
        if (!isEnabled || url.isBlank()) return CosmeticResources()
        val host = extractHostFromUrl(url)
        if (isSiteAllowed(host)) return CosmeticResources()

        lock.read {
            val handle = engineHandle
            if (bridge.isLoaded && handle != 0L) {
                return bridge.cosmeticResources(handle, url)
            }
        }
        return CosmeticResources()
    }

    fun hiddenClassIdSelectors(
        classes: Array<String>,
        ids: Array<String>,
        exceptions: Array<String>
    ): Array<String> {
        if (!isEnabled) return emptyArray()

        lock.read {
            val handle = engineHandle
            if (bridge.isLoaded && handle != 0L) {
                return bridge.hiddenClassIdSelectors(handle, classes, ids, exceptions)
            }
        }
        return emptyArray()
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

    fun destroy() {
        var oldHandle: Long
        lock.write {
            oldHandle = engineHandle
            engineHandle = 0L
            fallbackBlockedHosts = HashSet(0)
            fallbackAllowedHosts = HashSet(0)
            isLoaded.set(false)
        }
        if (oldHandle != 0L) {
            bridge.destroy(oldHandle)
        }
    }

    private fun swapEngineHandle(
        newHandle: Long,
        blocked: HashSet<String>,
        allowed: HashSet<String>,
        newRuleCount: Int
    ) {
        var oldHandle: Long
        lock.write {
            oldHandle = engineHandle
            engineHandle = newHandle
            fallbackBlockedHosts = blocked
            fallbackAllowedHosts = allowed
            ruleCount = newRuleCount
            isLoaded.set(true)
        }
        if (oldHandle != 0L && oldHandle != newHandle) {
            bridge.destroy(oldHandle)
        }
    }

    private fun isSiteAllowed(host: String?): Boolean = isHostInSet(host, allowedSites)

    private fun shouldBlockFallback(url: String, urlHost: String?): Boolean {
        if (!isLoaded.get()) return false
        if (isHostInSet(urlHost, fallbackAllowedHosts)) {
            return false
        }
        val lower = url.lowercase()
        if (lower.contains("/pagead.js") || lower.contains("/widget/ads") || lower.contains("/ads.js")) {
            return true
        }
        return isHostInSet(urlHost, fallbackBlockedHosts)
    }

    private fun isHostInSet(host: String?, set: Set<String>): Boolean {
        if (host.isNullOrBlank() || set.isEmpty()) return false
        var current: String = host
        while (true) {
            if (set.contains(current)) return true
            val dotIndex = current.indexOf('.')
            if (dotIndex == -1) break
            current = current.substring(dotIndex + 1)
        }
        return false
    }

    private fun fetchWithFallback(rawUrl: String): String {
        val cleanUrl = rawUrl.trim().replace("/refs/heads/", "/")
        val primary = runCatching { urlFetcher(cleanUrl) }
        if (primary.isSuccess) return primary.getOrThrow()

        if (cleanUrl.contains("raw.githubusercontent.com/")) {
            val cdnFallback = toJsDelivrUrl(cleanUrl)
            if (cdnFallback != null) {
                return urlFetcher(cdnFallback)
            }
        }
        throw primary.exceptionOrNull() ?: IOException("Failed to fetch $rawUrl")
    }

    companion object {
        const val DAT_FILE_NAME = "adblock_engine.dat"
        const val RULES_FILE_NAME = "adblock_rules.txt"
        const val RULE_COUNT_FILE_NAME = "adblock_rule_count.txt"
        const val ASSETS_FILE_NAME = "adblock_hosts.txt"
        const val LIST_CACHE_DIR = "adblock_lists"
        private val SAFE_LIST_ID_REGEX = Regex("[^a-zA-Z0-9._-]")

        private fun defaultFetchUrl(url: String): String {
            val req = Request.Builder().url(url).build()
            NetworkClients.defaultClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $url")
                return resp.body?.string() ?: throw IOException("Empty response for $url")
            }
        }

        fun toJsDelivrUrl(rawGithubUrl: String): String? {
            val trimmed = rawGithubUrl.trim().replace("/refs/heads/", "/")
            val prefix = "https://raw.githubusercontent.com/"
            if (!trimmed.startsWith(prefix)) return null
            val rest = trimmed.removePrefix(prefix)
            val parts = rest.split("/", limit = 4)
            if (parts.size < 4) return null
            val owner = parts[0]
            val repo = parts[1]
            val branch = parts[2]
            val path = parts[3]
            return "https://cdn.jsdelivr.net/gh/$owner/$repo@$branch/$path"
        }

        fun normalizeHost(rawHost: String?): String? {
            if (rawHost.isNullOrBlank()) return null
            val cleaned = rawHost.trim().lowercase().trimEnd('.')
            return cleaned.takeIf { it.isNotBlank() && it != "localhost" }
        }

        fun extractHostFromUrl(url: String?): String? {
            if (url.isNullOrBlank()) return null
            return runCatching {
                val withoutScheme = if (url.contains("://")) {
                    url.substringAfter("://")
                } else if (url.startsWith("//")) {
                    url.substring(2)
                } else {
                    url
                }
                val authority = withoutScheme.substringBefore('/').substringBefore('?').substringBefore('#')
                val hostPort = if (authority.contains('@')) authority.substringAfterLast('@') else authority
                val rawHost = if (hostPort.startsWith('[')) {
                    hostPort.substringBefore(']').trimStart('[')
                } else {
                    hostPort.substringBefore(':')
                }
                normalizeHost(rawHost)
            }.getOrNull()
        }

        fun mergeRulesWithUserConfig(
            listContents: List<String>,
            customUserRules: String,
            allowedSites: Set<String>
        ): String = buildString {
            for (content in listContents) {
                if (content.isNotBlank()) {
                    append(content)
                    if (!content.endsWith('\n')) append('\n')
                }
            }
            if (customUserRules.isNotBlank()) {
                append("! Custom user rules\n")
                append(customUserRules)
                if (!customUserRules.endsWith('\n')) append('\n')
            }
            if (allowedSites.isNotEmpty()) {
                append("! Allowed sites\n")
                for (site in allowedSites) {
                    val host = normalizeHost(site) ?: continue
                    append("@@||").append(host).append("^${'$'}document\n")
                }
            }
        }

        fun countValidRules(rulesText: String): Int {
            var count = 0
            rulesText.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() &&
                    !trimmed.startsWith("!") &&
                    !trimmed.startsWith("# ") &&
                    trimmed != "#" &&
                    !trimmed.startsWith("[Adblock", ignoreCase = true)
                ) {
                    count++
                }
            }
            return count
        }

        fun parseFallbackHostSets(rulesText: String): Pair<HashSet<String>, HashSet<String>> {
            val blocked = HashSet<String>(1024)
            val allowed = HashSet<String>(64)
            rulesText.lineSequence().forEach { line ->
                val allowHost = extractAllowHostFromRule(line)
                if (allowHost != null) {
                    allowed.add(allowHost)
                } else {
                    val blockHost = extractHostFromRule(line)
                    if (blockHost != null) {
                        blocked.add(blockHost)
                    }
                }
            }
            return blocked to allowed
        }

        fun extractHostFromRule(line: String): String? {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("#") || trimmed.startsWith("[")) {
                return null
            }
            if (trimmed.contains("##") || trimmed.contains("#@#") || trimmed.contains("#?#") || trimmed.startsWith("@@")) {
                return null
            }

            if (trimmed.startsWith("||")) {
                val afterBars = trimmed.substring(2)
                val hostPart = afterBars.takeWhile { it != '^' && it != '/' && it != '$' && it != '?' && it != ':' }
                val cleanHost = hostPart.trim().lowercase().trimEnd('.')
                return if (cleanHost.contains('.') && !cleanHost.contains('*')) cleanHost else null
            }

            if (trimmed.startsWith("0.0.0.0 ") || trimmed.startsWith("127.0.0.1 ")) {
                val hostPart = trimmed.substringAfter(' ').trim().substringBefore(' ').substringBefore('#').trim()
                val cleanHost = hostPart.lowercase().trimEnd('.')
                return if (cleanHost.contains('.') && cleanHost != "localhost") cleanHost else null
            }

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

            if (trimmed.startsWith("@@")) {
                val afterPrefix = trimmed.removePrefix("@@").removePrefix("||")
                val hostPart = afterPrefix.takeWhile { it != '^' && it != '/' && it != '$' && it != '?' && it != ':' }
                val cleanHost = hostPart.trim().lowercase().trimEnd('.')
                return if (cleanHost.contains('.') && !cleanHost.contains('*')) cleanHost else null
            }
            return null
        }
    }
}
