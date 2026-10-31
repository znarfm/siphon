package com.siphon.app.data.repository

import android.app.Application
import android.content.Context
import android.net.Uri
import com.siphon.app.core.adblock.AdblockConfig
import com.siphon.app.core.adblock.FastHostFilter
import com.siphon.app.core.service.DownloadForegroundService
import com.siphon.app.data.db.BookmarkEntity
import com.siphon.app.data.db.BrowserDao
import com.siphon.app.data.db.HistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BrowserPreferences(
    val desktopMode: Boolean = false,
    val webDarkMode: Boolean = false,
    val searchEngine: String = "https://www.google.com/search?q="
)

class BrowserRepository(
    val application: Application,
    val browserDao: BrowserDao,
    val hostFilter: FastHostFilter
) {
    private val prefs = application.getSharedPreferences(
        DownloadForegroundService.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    val bookmarks: Flow<List<BookmarkEntity>> = browserDao.observeBookmarks()
    val history: Flow<List<HistoryEntity>> = browserDao.observeHistory()

    private val _adblockRuleCount = MutableStateFlow(hostFilter.ruleCount)
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
        CoroutineScope(Dispatchers.Default).launch {
            while (!hostFilter.isLoaded.get()) {
                delay(100)
            }
            _adblockRuleCount.value = hostFilter.ruleCount
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

    fun getActiveAdblockConfig(): AdblockConfig {
        var base = prefs.getString("pref_adblock_base_url", FastHostFilter.HAGEZI_NORMAL_URL)
            ?: FastHostFilter.HAGEZI_NORMAL_URL
        if (base.contains("raw.githubusercontent.com")) {
            base = FastHostFilter.HAGEZI_NORMAL_URL
        }
        return AdblockConfig(
            baseUrl = base,
            allowShorteners = prefs.getBoolean("pref_adblock_allow_shorteners", true),
            enableThreatIntel = prefs.getBoolean("pref_adblock_threat_intel", false),
            customUrl = prefs.getString("pref_adblock_custom_url", "") ?: ""
        )
    }

    fun saveAdblockConfig(config: AdblockConfig) {
        prefs.edit()
            .putString("pref_adblock_base_url", config.baseUrl)
            .putBoolean("pref_adblock_allow_shorteners", config.allowShorteners)
            .putBoolean("pref_adblock_threat_intel", config.enableThreatIntel)
            .putString("pref_adblock_custom_url", config.customUrl)
            .apply()
    }

    suspend fun refreshAdblockSubscriptions(
        config: AdblockConfig
    ): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        saveAdblockConfig(config)
        val result = hostFilter.refreshSubscriptions(
            context = application,
            blockUrls = config.toBlockUrls(),
            allowUrls = config.toAllowUrls()
        )
        if (result.isSuccess) {
            _adblockRuleCount.value = hostFilter.ruleCount
        }
        result
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
}
