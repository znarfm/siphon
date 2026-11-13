package com.znarfm.siphon.data.repository

import android.app.Application
import android.content.Context
import android.net.Uri
import com.znarfm.siphon.core.adblock.AdblockEngine
import com.znarfm.siphon.core.adblock.AdblockSettings
import com.znarfm.siphon.core.adblock.BuiltinFilterLists
import com.znarfm.siphon.core.adblock.FilterListCategory
import com.znarfm.siphon.core.adblock.FilterListSource
import com.znarfm.siphon.core.service.DownloadForegroundService
import com.znarfm.siphon.data.db.BookmarkEntity
import com.znarfm.siphon.data.db.BrowserDao
import com.znarfm.siphon.data.db.HistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Immutable
data class BrowserPreferences(
    val desktopMode: Boolean = false,
    val webDarkMode: Boolean = false,
    val searchEngine: String = "https://www.google.com/search?q="
)

class BrowserRepository(
    val application: Application,
    val browserDao: BrowserDao,
    val adblockEngine: AdblockEngine
) {
    private val prefs = application.getSharedPreferences(
        DownloadForegroundService.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var adblockRebuildJob: Job? = null

    val bookmarks: Flow<List<BookmarkEntity>> = browserDao.observeBookmarks()
    val history: Flow<List<HistoryEntity>> = browserDao.observeHistory()

    private val _adblockSettings = MutableStateFlow(loadAdblockSettings())
    val adblockSettings: StateFlow<AdblockSettings> = _adblockSettings.asStateFlow()

    private val _isAdBlockEnabled = MutableStateFlow(_adblockSettings.value.isGlobalEnabled)
    val isAdBlockEnabled: StateFlow<Boolean> = _isAdBlockEnabled.asStateFlow()

    private val _adblockRuleCount = MutableStateFlow(adblockEngine.ruleCount)
    val adblockRuleCount: StateFlow<Int> = _adblockRuleCount.asStateFlow()

    private val _browserPreferences = MutableStateFlow(
        BrowserPreferences(
            desktopMode = prefs.getBoolean("pref_desktop_mode", false),
            webDarkMode = prefs.getBoolean("pref_web_dark_mode", false),
            searchEngine = prefs.getString("pref_search_engine", "https://www.google.com/search?q=") ?: "https://www.google.com/search?q="
        )
    )
    val browserPreferences: StateFlow<BrowserPreferences> = _browserPreferences.asStateFlow()

    init {
        repoScope.launch {
            val initialSettings = _adblockSettings.value
            adblockEngine.loadCachedOrAssets(application, initialSettings)
            _adblockRuleCount.value = adblockEngine.ruleCount

            val enabledLists = initialSettings.enabledLists.filter { it.isEnabled && it.url.isNotBlank() }
            val needsInitialFetch = initialSettings.isGlobalEnabled &&
                enabledLists.isNotEmpty() &&
                (!adblockEngine.hasCachedRules(application) || enabledLists.any { it.lastUpdatedMs == 0L })
            if (needsInitialFetch) {
                refreshAdblockSubscriptions()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun isUrlBookmarked(url: Flow<String>): Flow<Boolean> =
        url.flatMapLatest { browserDao.observeIsBookmarked(it) }

    suspend fun toggleBookmark(url: String, title: String) = withContext(Dispatchers.IO) {
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("chrome:")) return@withContext
        val isBookmarked = browserDao.observeIsBookmarked(url).first()
        if (isBookmarked) {
            browserDao.deleteBookmarkByUrl(url)
        } else {
            browserDao.insertBookmark(
                BookmarkEntity(
                    title = title.ifBlank { deriveTitleFromUrl(url) },
                    url = url
                )
            )
        }
    }

    suspend fun deleteBookmark(id: Long) = withContext(Dispatchers.IO) {
        browserDao.deleteBookmarkById(id)
    }

    suspend fun addHistory(url: String, title: String) = withContext(Dispatchers.IO) {
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("chrome:")) return@withContext
        browserDao.deleteHistoryByUrl(url)
        browserDao.insertHistory(
            HistoryEntity(
                title = title.ifBlank { deriveTitleFromUrl(url) },
                url = url,
                visitedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteHistory(id: Long) = withContext(Dispatchers.IO) {
        browserDao.deleteHistoryById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        browserDao.clearHistory()
    }

    fun toggleAdBlock() {
        val current = _adblockSettings.value
        val updated = current.copy(isGlobalEnabled = !current.isGlobalEnabled)
        applyAndPersistAdblockSettings(updated)
    }

    fun toggleSiteAllowlist(host: String) {
        val normalized = AdblockEngine.normalizeHost(host) ?: return
        val current = _adblockSettings.value
        val nextAllowed = if (current.allowedSites.contains(normalized)) {
            current.allowedSites - normalized
        } else {
            current.allowedSites + normalized
        }
        applyAndPersistAdblockSettings(current.copy(allowedSites = nextAllowed))
    }

    fun addCustomList(name: String, url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return
        val cleanName = name.trim().ifBlank { deriveTitleFromUrl(cleanUrl) }
        val current = _adblockSettings.value
        if (current.enabledLists.any { it.url.equals(cleanUrl, ignoreCase = true) }) return

        val newSource = FilterListSource(
            id = "custom_${System.currentTimeMillis()}_${cleanUrl.hashCode().toUInt()}",
            name = cleanName,
            description = "Custom filter list",
            url = cleanUrl,
            category = FilterListCategory.CUSTOM,
            isBuiltIn = false,
            isEnabled = true
        )
        applyAndPersistAdblockSettings(
            current.copy(enabledLists = current.enabledLists + newSource)
        )
        scheduleAdblockRebuild()
    }

    fun removeCustomList(id: String) {
        val current = _adblockSettings.value
        val updated = current.enabledLists.filterNot { it.id == id && !it.isBuiltIn }
        if (updated.size != current.enabledLists.size) {
            applyAndPersistAdblockSettings(current.copy(enabledLists = updated))
            adblockEngine.deleteCachedList(application, id)
            scheduleAdblockRebuild()
        }
    }

    fun toggleList(id: String, enabled: Boolean) {
        val current = _adblockSettings.value
        var changed = false
        val updated = current.enabledLists.map { source ->
            if (source.id == id && source.isEnabled != enabled) {
                changed = true
                source.copy(isEnabled = enabled)
            } else {
                source
            }
        }
        if (changed) {
            applyAndPersistAdblockSettings(current.copy(enabledLists = updated))
            scheduleAdblockRebuild()
        }
    }

    fun updateCustomUserRules(rules: String) {
        val current = _adblockSettings.value
        if (current.customUserRules == rules) return
        applyAndPersistAdblockSettings(current.copy(customUserRules = rules))
        scheduleAdblockRebuild(debounceMs = 350L)
    }

    private fun scheduleAdblockRebuild(debounceMs: Long = 0L) {
        adblockRebuildJob?.cancel()
        adblockRebuildJob = repoScope.launch {
            if (debounceMs > 0L) {
                delay(debounceMs)
            }
            val result = adblockEngine.rebuildRules(
                context = application,
                settings = _adblockSettings.value
            )
            result.onSuccess { updatedSettings ->
                val latest = _adblockSettings.value
                val metaById = updatedSettings.enabledLists.associateBy { it.id }
                val mergedSettings = latest.copy(
                    enabledLists = latest.enabledLists.map { cur ->
                        val meta = metaById[cur.id]
                        if (meta != null) {
                            cur.copy(
                                ruleCount = meta.ruleCount,
                                lastUpdatedMs = meta.lastUpdatedMs,
                                lastError = meta.lastError
                            )
                        } else {
                            cur
                        }
                    }
                )
                _adblockSettings.value = mergedSettings
                _isAdBlockEnabled.value = mergedSettings.isGlobalEnabled
                _adblockRuleCount.value = adblockEngine.ruleCount
                saveAdblockSettings(mergedSettings)
            }
        }
    }

    suspend fun refreshAdblockSubscriptions(
        onResult: ((Result<AdblockSettings>) -> Unit)? = null
    ): Result<AdblockSettings> = withContext(Dispatchers.IO) {
        adblockRebuildJob?.cancel()
        val current = _adblockSettings.value
        val result = adblockEngine.refreshSubscriptions(
            context = application,
            settings = current
        )
        result.onSuccess { updatedSettings ->
            _adblockSettings.value = updatedSettings
            _isAdBlockEnabled.value = updatedSettings.isGlobalEnabled
            _adblockRuleCount.value = adblockEngine.ruleCount
            saveAdblockSettings(updatedSettings)
        }
        onResult?.invoke(result)
        result
    }

    private fun applyAndPersistAdblockSettings(updated: AdblockSettings) {
        _adblockSettings.value = updated
        _isAdBlockEnabled.value = updated.isGlobalEnabled
        adblockEngine.updateSettings(updated)
        saveAdblockSettings(updated)
    }

    private fun saveAdblockSettings(settings: AdblockSettings) {
        prefs.edit()
            .putString(PREF_ADBLOCK_SETTINGS_JSON, settings.toJson().toString())
            .apply()
    }

    private fun loadAdblockSettings(): AdblockSettings {
        val rawJson = prefs.getString(PREF_ADBLOCK_SETTINGS_JSON, null)
        if (!rawJson.isNullOrBlank()) {
            val parsed = runCatching { AdblockSettings.fromJson(JSONObject(rawJson)) }.getOrNull()
            if (parsed != null) return parsed
        }

        // Migrate legacy pref_adblock_* keys if present
        val hasLegacy = prefs.contains(LEGACY_PREF_BASE_URL) ||
            prefs.contains(LEGACY_PREF_ALLOW_SHORTENERS) ||
            prefs.contains(LEGACY_PREF_THREAT_INTEL) ||
            prefs.contains(LEGACY_PREF_CUSTOM_URL)

        if (!hasLegacy) {
            return AdblockSettings()
        }

        val legacyBase = prefs.getString(LEGACY_PREF_BASE_URL, "") ?: ""
        val legacyShorteners = prefs.getBoolean(LEGACY_PREF_ALLOW_SHORTENERS, true)
        val legacyThreat = prefs.getBoolean(LEGACY_PREF_THREAT_INTEL, false)
        val legacyCustom = (prefs.getString(LEGACY_PREF_CUSTOM_URL, "") ?: "").trim()

        val selectedBaseId = when {
            legacyBase.contains("light", ignoreCase = true) -> BuiltinFilterLists.ID_HAGEZI_LIGHT
            legacyBase.contains("pro", ignoreCase = true) -> BuiltinFilterLists.ID_HAGEZI_PRO
            else -> BuiltinFilterLists.ID_HAGEZI_NORMAL
        }

        val migratedLists = BuiltinFilterLists.CATALOG.map { source ->
            when (source.id) {
                selectedBaseId -> source.copy(isEnabled = true)
                BuiltinFilterLists.ID_HAGEZI_NORMAL -> source.copy(isEnabled = selectedBaseId == BuiltinFilterLists.ID_HAGEZI_NORMAL)
                BuiltinFilterLists.ID_DANDELION_SHORTENERS -> source.copy(isEnabled = legacyShorteners)
                BuiltinFilterLists.ID_HAGEZI_THREAT -> source.copy(isEnabled = legacyThreat)
                else -> source
            }
        }.let { list ->
            if (legacyCustom.isNotBlank()) {
                list + FilterListSource(
                    id = "legacy_custom",
                    name = "Custom List",
                    description = "Migrated custom filter list",
                    url = legacyCustom,
                    category = FilterListCategory.CUSTOM,
                    isBuiltIn = false,
                    isEnabled = true
                )
            } else {
                list
            }
        }

        val migrated = AdblockSettings(enabledLists = migratedLists)
        saveAdblockSettings(migrated)
        prefs.edit()
            .remove(LEGACY_PREF_BASE_URL)
            .remove(LEGACY_PREF_ALLOW_SHORTENERS)
            .remove(LEGACY_PREF_THREAT_INTEL)
            .remove(LEGACY_PREF_CUSTOM_URL)
            .apply()
        return migrated
    }

    fun setDesktopMode(enabled: Boolean) {
        _browserPreferences.value = _browserPreferences.value.copy(desktopMode = enabled)
        prefs.edit().putBoolean("pref_desktop_mode", enabled).apply()
    }

    fun setWebDarkMode(enabled: Boolean) {
        _browserPreferences.value = _browserPreferences.value.copy(webDarkMode = enabled)
        prefs.edit().putBoolean("pref_web_dark_mode", enabled).apply()
    }

    fun setSearchEngine(engine: String) {
        _browserPreferences.value = _browserPreferences.value.copy(searchEngine = engine)
        prefs.edit().putString("pref_search_engine", engine).apply()
    }

    private fun deriveTitleFromUrl(url: String): String {
        return runCatching {
            val uri = Uri.parse(url)
            uri.host?.removePrefix("www.")?.ifBlank { null }
        }.getOrNull() ?: "Tab"
    }

    companion object {
        private const val PREF_ADBLOCK_SETTINGS_JSON = "pref_adblock_settings_json"
        private const val LEGACY_PREF_BASE_URL = "pref_adblock_base_url"
        private const val LEGACY_PREF_ALLOW_SHORTENERS = "pref_adblock_allow_shorteners"
        private const val LEGACY_PREF_THREAT_INTEL = "pref_adblock_threat_intel"
        private const val LEGACY_PREF_CUSTOM_URL = "pref_adblock_custom_url"
    }
}
