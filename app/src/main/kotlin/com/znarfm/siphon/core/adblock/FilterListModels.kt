package com.znarfm.siphon.core.adblock

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

enum class FilterListCategory {
    ADS,
    PRIVACY,
    ANNOYANCES,
    SECURITY,
    REGIONAL,
    CUSTOM
}

@Immutable
data class FilterListSource(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val category: FilterListCategory,
    val isBuiltIn: Boolean = true,
    val isEnabled: Boolean = false,
    val ruleCount: Int = 0,
    val lastUpdatedMs: Long = 0L,
    val lastError: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("description", description)
        put("url", url)
        put("category", category.name)
        put("isBuiltIn", isBuiltIn)
        put("isEnabled", isEnabled)
        put("ruleCount", ruleCount)
        put("lastUpdatedMs", lastUpdatedMs)
        if (lastError != null) put("lastError", lastError)
    }

    companion object {
        fun fromJson(json: JSONObject): FilterListSource {
            val categoryName = json.optString("category", FilterListCategory.CUSTOM.name)
            val category = runCatching { FilterListCategory.valueOf(categoryName) }
                .getOrDefault(FilterListCategory.CUSTOM)
            val error = if (json.has("lastError") && !json.isNull("lastError")) {
                json.optString("lastError").takeIf { it.isNotBlank() }
            } else {
                null
            }
            return FilterListSource(
                id = json.getString("id"),
                name = json.optString("name", ""),
                description = json.optString("description", ""),
                url = json.optString("url", ""),
                category = category,
                isBuiltIn = json.optBoolean("isBuiltIn", false),
                isEnabled = json.optBoolean("isEnabled", false),
                ruleCount = json.optInt("ruleCount", 0),
                lastUpdatedMs = json.optLong("lastUpdatedMs", 0L),
                lastError = error
            )
        }
    }
}

@Immutable
data class AdblockSettings(
    val enabledLists: List<FilterListSource> = BuiltinFilterLists.CATALOG,
    val customUserRules: String = "",
    val allowedSites: Set<String> = emptySet(),
    val isGlobalEnabled: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("isGlobalEnabled", isGlobalEnabled)
        put("customUserRules", customUserRules)
        put("allowedSites", JSONArray(allowedSites.toList()))
        put("enabledLists", JSONArray().apply {
            enabledLists.forEach { put(it.toJson()) }
        })
    }

    companion object {
        fun fromJson(
            json: JSONObject,
            catalog: List<FilterListSource> = BuiltinFilterLists.CATALOG
        ): AdblockSettings {
            val isGlobalEnabled = json.optBoolean("isGlobalEnabled", true)
            val customUserRules = json.optString("customUserRules", "")

            val allowedSites = buildSet {
                val arr = json.optJSONArray("allowedSites")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val host = arr.optString(i).trim().lowercase().trimEnd('.')
                        if (host.isNotBlank()) add(host)
                    }
                }
            }

            val savedById = mutableMapOf<String, FilterListSource>()
            val customLists = mutableListOf<FilterListSource>()
            val listsArr = json.optJSONArray("enabledLists")
            if (listsArr != null) {
                for (i in 0 until listsArr.length()) {
                    val obj = listsArr.optJSONObject(i) ?: continue
                    val parsed = runCatching { FilterListSource.fromJson(obj) }.getOrNull() ?: continue
                    if (parsed.isBuiltIn) {
                        savedById[parsed.id] = parsed
                    } else {
                        customLists.add(parsed)
                    }
                }
            }

            val mergedBuiltIns = catalog.map { builtIn ->
                val saved = savedById[builtIn.id]
                if (saved != null) {
                    builtIn.copy(
                        isEnabled = saved.isEnabled,
                        ruleCount = saved.ruleCount,
                        lastUpdatedMs = saved.lastUpdatedMs,
                        lastError = saved.lastError
                    )
                } else {
                    builtIn
                }
            }

            return AdblockSettings(
                enabledLists = mergedBuiltIns + customLists,
                customUserRules = customUserRules,
                allowedSites = allowedSites,
                isGlobalEnabled = isGlobalEnabled
            )
        }
    }
}

object BuiltinFilterLists {
    const val ID_EASYLIST = "easylist"
    const val ID_EASYPRIVACY = "easyprivacy"
    const val ID_UBLOCK_FILTERS = "ublock_filters"
    const val ID_UBLOCK_BADWARE = "ublock_badware"
    const val ID_PETER_LOWE = "peter_lowe"
    const val ID_HAGEZI_LIGHT = "hagezi_light"
    const val ID_HAGEZI_NORMAL = "hagezi_normal"
    const val ID_HAGEZI_PRO = "hagezi_pro"
    const val ID_HAGEZI_THREAT = "hagezi_threat"
    const val ID_DANDELION_SHORTENERS = "dandelion_shorteners"
    const val ID_FANBOY_ANNOYANCE = "fanboy_annoyance"
    const val ID_FANBOY_COOKIE = "fanboy_cookie"

    val CATALOG: List<FilterListSource> = listOf(
        FilterListSource(
            id = ID_HAGEZI_NORMAL,
            name = "HaGeZi Multi Normal",
            description = "Recommended all-round protection against ads, trackers, and telemetry.",
            url = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/adblock/multi.txt",
            category = FilterListCategory.ADS,
            isBuiltIn = true,
            isEnabled = true
        ),
        FilterListSource(
            id = ID_HAGEZI_LIGHT,
            name = "HaGeZi Multi Light",
            description = "Lightweight protection with minimal memory usage and low false positives.",
            url = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/adblock/light.txt",
            category = FilterListCategory.ADS,
            isBuiltIn = true,
            isEnabled = false
        ),
        FilterListSource(
            id = ID_HAGEZI_PRO,
            name = "HaGeZi Multi Pro",
            description = "Aggressive blocking for ads, trackers, metrics, and referral domains.",
            url = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/adblock/pro.txt",
            category = FilterListCategory.ADS,
            isBuiltIn = true,
            isEnabled = false
        ),
        FilterListSource(
            id = ID_HAGEZI_THREAT,
            name = "HaGeZi Threat Intelligence",
            description = "Blocks malware, phishing, scam, and cryptojacking domains.",
            url = "https://cdn.jsdelivr.net/gh/hagezi/dns-blocklists@latest/adblock/fake.txt",
            category = FilterListCategory.SECURITY,
            isBuiltIn = true,
            isEnabled = false
        ),
        FilterListSource(
            id = ID_EASYLIST,
            name = "EasyList",
            description = "Primary filter list removing most adverts from international webpages.",
            url = "https://easylist.to/easylist/easylist.txt",
            category = FilterListCategory.ADS,
            isBuiltIn = true,
            isEnabled = false
        ),
        FilterListSource(
            id = ID_EASYPRIVACY,
            name = "EasyPrivacy",
            description = "Removes tracking scripts, web bugs, and analytics collectors.",
            url = "https://easylist.to/easylist/easyprivacy.txt",
            category = FilterListCategory.PRIVACY,
            isBuiltIn = true,
            isEnabled = false
        ),
        FilterListSource(
            id = ID_UBLOCK_FILTERS,
            name = "uBlock Filters – Ads",
            description = "uBlock Origin's curated ad and cosmetic filtering rules.",
            url = "https://cdn.jsdelivr.net/gh/uBlockOrigin/uAssets@master/filters/filters.txt",
            category = FilterListCategory.ADS,
            isBuiltIn = true,
            isEnabled = false
        ),
        FilterListSource(
            id = ID_UBLOCK_BADWARE,
            name = "uBlock Filters – Badware",
            description = "Protects against malicious sites, PUPs, and tech support scams.",
            url = "https://cdn.jsdelivr.net/gh/uBlockOrigin/uAssets@master/filters/badware.txt",
            category = FilterListCategory.SECURITY,
            isBuiltIn = true,
            isEnabled = false
        ),
        FilterListSource(
            id = ID_PETER_LOWE,
            name = "Peter Lowe's Ad & Tracking",
            description = "Compact, long-standing list of ad and tracking servers.",
            url = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=adblockplus&showintro=0&mimetype=plaintext",
            category = FilterListCategory.ADS,
            isBuiltIn = true,
            isEnabled = false
        ),
        FilterListSource(
            id = ID_DANDELION_SHORTENERS,
            name = "DandelionSprout URL Shorteners",
            description = "Exception rules to keep legitimate URL shorteners working.",
            url = "https://cdn.jsdelivr.net/gh/DandelionSprout/adfilt@master/LegitimateURLShortener.txt",
            category = FilterListCategory.PRIVACY,
            isBuiltIn = true,
            isEnabled = false
        ),
        FilterListSource(
            id = ID_FANBOY_ANNOYANCE,
            name = "Fanboy's Annoyance List",
            description = "Blocks social widgets, in-page popups, and other web annoyances.",
            url = "https://easylist.to/easylist/fanboy-annoyance.txt",
            category = FilterListCategory.ANNOYANCES,
            isBuiltIn = true,
            isEnabled = false
        ),
        FilterListSource(
            id = ID_FANBOY_COOKIE,
            name = "Fanboy's Cookie List",
            description = "Hides cookie consent banners and GDPR overlay notices.",
            url = "https://secure.fanboy.co.nz/fanboy-cookiemonster.txt",
            category = FilterListCategory.ANNOYANCES,
            isBuiltIn = true,
            isEnabled = false
        )
    )
}
