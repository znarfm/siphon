package com.znarfm.siphon.core.adblock

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class AdblockEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeNativeAdblockBridge(
        override var isLoaded: Boolean = true
    ) : NativeAdblockBridge {
        var nextHandle = 1L
        val createdFromRules = mutableListOf<String>()
        val destroyedHandles = mutableListOf<Long>()
        val activeHandles = mutableSetOf<Long>()
        var blockPredicate: (url: String, sourceUrl: String, requestType: String) -> Boolean = { url, _, _ ->
            url.contains("doubleclick.net") || url.contains("adservice.google.com")
        }
        var cosmeticResourcesResult = CosmeticResources(
            hideSelectors = listOf(".ad-banner", "#sponsored"),
            injectedScript = "console.log('cosmetic');",
            generichide = false,
            exceptions = listOf(".allowed-ad")
        )

        override fun createFromRules(rules: String): Long {
            createdFromRules.add(rules)
            val handle = nextHandle++
            activeHandles.add(handle)
            return handle
        }

        override fun createFromDat(data: ByteArray): Long {
            val handle = nextHandle++
            activeHandles.add(handle)
            return handle
        }

        override fun serialize(handle: Long): ByteArray? {
            return if (activeHandles.contains(handle)) "FAKE_DAT_$handle".toByteArray() else null
        }

        override fun checkNetwork(
            handle: Long,
            url: String,
            sourceUrl: String,
            requestType: String
        ): Boolean {
            if (!activeHandles.contains(handle)) return false
            return blockPredicate(url, sourceUrl, requestType)
        }

        override fun cosmeticResources(handle: Long, url: String): CosmeticResources {
            if (!activeHandles.contains(handle)) return CosmeticResources()
            return cosmeticResourcesResult
        }

        override fun hiddenClassIdSelectors(
            handle: Long,
            classes: Array<String>,
            ids: Array<String>,
            exceptions: Array<String>
        ): Array<String> {
            if (!activeHandles.contains(handle)) return emptyArray()
            return classes.map { ".$it" }.toTypedArray()
        }

        override fun destroy(handle: Long) {
            activeHandles.remove(handle)
            destroyedHandles.add(handle)
        }
    }

    @Test
    fun testBuiltinCatalogContainsCuratedListsAndDefaults() {
        val catalog = BuiltinFilterLists.CATALOG
        assertTrue(catalog.size >= 10)
        assertTrue(catalog.none { it.url.contains("refs/heads") })

        val hageziNormal = catalog.first { it.id == BuiltinFilterLists.ID_HAGEZI_NORMAL }
        assertTrue(hageziNormal.isEnabled)
        assertTrue(hageziNormal.isBuiltIn)
        assertEquals(FilterListCategory.ADS, hageziNormal.category)

        val expectedIds = listOf(
            BuiltinFilterLists.ID_EASYLIST,
            BuiltinFilterLists.ID_EASYPRIVACY,
            BuiltinFilterLists.ID_UBLOCK_FILTERS,
            BuiltinFilterLists.ID_UBLOCK_BADWARE,
            BuiltinFilterLists.ID_PETER_LOWE,
            BuiltinFilterLists.ID_HAGEZI_LIGHT,
            BuiltinFilterLists.ID_HAGEZI_NORMAL,
            BuiltinFilterLists.ID_HAGEZI_PRO,
            BuiltinFilterLists.ID_HAGEZI_THREAT,
            BuiltinFilterLists.ID_DANDELION_SHORTENERS,
            BuiltinFilterLists.ID_FANBOY_ANNOYANCE,
            BuiltinFilterLists.ID_FANBOY_COOKIE
        )
        val catalogIds = catalog.map { it.id }.toSet()
        expectedIds.forEach { id ->
            assertTrue("Missing catalog entry $id", catalogIds.contains(id))
        }
    }

    @Test
    fun testNativeEngineNetworkAndCosmeticBlocking() {
        val fakeBridge = FakeNativeAdblockBridge(isLoaded = true)
        val engine = AdblockEngine(bridge = fakeBridge)
        engine.loadRulesForTesting("||doubleclick.net^\n||adservice.google.com^")

        assertTrue(engine.isLoaded.get())
        assertTrue(engine.shouldBlockRequest("https://doubleclick.net/ad.js", "https://example.com", "script"))
        assertFalse(engine.shouldBlockRequest("https://example.com/app.js", "https://example.com", "script"))

        val cosmetic = engine.getCosmeticResources("https://example.com/article")
        assertEquals(listOf(".ad-banner", "#sponsored"), cosmetic.hideSelectors)
        assertEquals("console.log('cosmetic');", cosmetic.injectedScript)

        val selectors = engine.hiddenClassIdSelectors(arrayOf("ad-box"), arrayOf("banner"), emptyArray())
        assertArrayEquals(arrayOf(".ad-box"), selectors)

        engine.destroy()
        assertFalse(engine.isLoaded.get())
        assertTrue(fakeBridge.destroyedHandles.contains(1L))
    }

    @Test
    fun testGlobalToggleAndPerSiteAllowlisting() {
        val fakeBridge = FakeNativeAdblockBridge(isLoaded = true)
        val engine = AdblockEngine(bridge = fakeBridge)
        engine.loadRulesForTesting("||doubleclick.net^")

        // Allowlist example.com -> requests from example.com or its subdomains are not blocked
        engine.updateSettings(
            AdblockSettings(
                allowedSites = setOf("example.com"),
                isGlobalEnabled = true
            )
        )
        assertFalse(engine.shouldBlockRequest("https://doubleclick.net/ad.js", "https://example.com/page", "script"))
        assertFalse(engine.shouldBlockRequest("https://doubleclick.net/ad.js", "https://sub.example.com/page", "script"))
        assertEquals(CosmeticResources(), engine.getCosmeticResources("https://sub.example.com/page"))

        // Non-allowlisted site is still blocked
        assertTrue(engine.shouldBlockRequest("https://doubleclick.net/ad.js", "https://other.org/page", "script"))

        // Global disable turns off blocking everywhere
        engine.updateSettings(AdblockSettings(allowedSites = emptySet(), isGlobalEnabled = false))
        assertFalse(engine.shouldBlockRequest("https://doubleclick.net/ad.js", "https://other.org/page", "script"))
        assertEquals(CosmeticResources(), engine.getCosmeticResources("https://other.org/page"))
        assertArrayEquals(emptyArray(), engine.hiddenClassIdSelectors(arrayOf("ad-box"), emptyArray(), emptyArray()))
    }

    @Test
    fun testFallbackHostMatchingWhenNativeUnavailable() {
        val fakeBridge = FakeNativeAdblockBridge(isLoaded = false)
        val engine = AdblockEngine(bridge = fakeBridge)
        val rules = """
            # Test blocklist
            doubleclick.net
            ||googleadservices.com^
            0.0.0.0 ads.example.com
            popads.net
            @@||popads.net^
        """.trimIndent()
        engine.loadRulesForTesting(rules)

        // Exact & subdomain suffix slicing
        assertTrue(engine.shouldBlockRequest("https://doubleclick.net/ad.js"))
        assertTrue(engine.shouldBlockRequest("https://sub.doubleclick.net/ad.js"))
        assertTrue(engine.shouldBlockRequest("https://tracker.ads.example.com/pixel"))
        assertTrue(engine.shouldBlockRequest("http://user:password@googleadservices.com:8080/ad.js"))

        // Domain boundary safety
        assertFalse(engine.shouldBlockRequest("https://notdoubleclick.net/good.js"))
        assertFalse(engine.shouldBlockRequest("https://wikipedia.org/wiki/Main_Page"))

        // Allowlist rule precedence
        assertFalse(engine.shouldBlockRequest("https://popads.net/script.js"))
        assertFalse(engine.shouldBlockRequest("https://sub.popads.net/script.js"))

        // Script path heuristics
        assertTrue(engine.shouldBlockRequest("https://test.com/pagead.js"))
        assertTrue(engine.shouldBlockRequest("https://test.com/widget/ads"))
    }

    @Test
    fun testExtractHostAndAllowHostHelpers() {
        assertEquals("adservice.google.com", AdblockEngine.extractHostFromRule("||adservice.google.com^"))
        assertEquals("tracking.domain.net", AdblockEngine.extractHostFromRule("||tracking.domain.net^${'$'}third-party"))
        assertEquals("ads.example.com", AdblockEngine.extractHostFromRule("0.0.0.0 ads.example.com"))
        assertEquals("ads.example.com", AdblockEngine.extractHostFromRule("127.0.0.1 ads.example.com # comment"))
        assertEquals("popunder.org", AdblockEngine.extractHostFromRule("popunder.org"))

        assertNull(AdblockEngine.extractHostFromRule("! Title: EasyList"))
        assertNull(AdblockEngine.extractHostFromRule("##.ad-banner"))
        assertNull(AdblockEngine.extractHostFromRule("@@||goodsite.com^"))

        assertEquals("bit.ly", AdblockEngine.extractAllowHostFromRule("@@||bit.ly^"))
        assertEquals("tinyurl.com", AdblockEngine.extractAllowHostFromRule("@@||tinyurl.com^${'$'}document"))
        assertNull(AdblockEngine.extractAllowHostFromRule("##.ad-banner"))
    }

    @Test
    fun testRefreshSubscriptionsWritesDatAndRulesAndSwapsHandle() = runBlocking {
        val fakeBridge = FakeNativeAdblockBridge(isLoaded = true)
        val fetchedUrls = mutableListOf<String>()

        val engine = AdblockEngine(
            bridge = fakeBridge,
            urlFetcher = { url ->
                fetchedUrls.add(url)
                when {
                    url.contains("raw.githubusercontent.com") -> throw IOException("Blocked")
                    url.contains("cdn.jsdelivr.net") -> "||tracker.example.com^\n||ads.example.net^\n"
                    else -> "||doubleclick.net^\n"
                }
            }
        )

        // Pre-load initial handle (handle 1)
        engine.loadRulesForTesting("||initial.com^")
        assertTrue(fakeBridge.activeHandles.contains(1L))

        val settings = AdblockSettings(
            enabledLists = listOf(
                FilterListSource(
                    id = "list1",
                    name = "List 1",
                    description = "Test",
                    url = "https://raw.githubusercontent.com/owner/repo/main/list.txt",
                    category = FilterListCategory.ADS,
                    isBuiltIn = false,
                    isEnabled = true
                ),
                FilterListSource(
                    id = "list2",
                    name = "Disabled List",
                    description = "Disabled",
                    url = "https://example.com/disabled.txt",
                    category = FilterListCategory.PRIVACY,
                    isBuiltIn = false,
                    isEnabled = false
                )
            ),
            customUserRules = "||custom-ad.org^",
            allowedSites = setOf("trusted.com"),
            isGlobalEnabled = true
        )

        val filesDir = tempFolder.newFolder("files")
        val result = engine.refreshSubscriptionsToDir(filesDir, settings)
        assertTrue(result.isSuccess)

        val updated = result.getOrThrow()
        val list1 = updated.enabledLists.first { it.id == "list1" }
        assertEquals(2, list1.ruleCount)
        assertNull(list1.lastError)
        assertTrue(list1.lastUpdatedMs > 0L)

        // Disabled list should not have been fetched
        assertFalse(fetchedUrls.any { it.contains("disabled.txt") })
        // Fallback to jsDelivr should have occurred
        assertTrue(fetchedUrls.any { it.contains("cdn.jsdelivr.net/gh/owner/repo@main/list.txt") })

        // Verify .dat and rules files written
        val datFile = File(filesDir, AdblockEngine.DAT_FILE_NAME)
        val rulesFile = File(filesDir, AdblockEngine.RULES_FILE_NAME)
        assertTrue(datFile.exists() && datFile.length() > 0L)
        assertTrue(rulesFile.exists())
        val savedRules = rulesFile.readText()
        assertTrue(savedRules.contains("||tracker.example.com^"))
        assertTrue(savedRules.contains("||custom-ad.org^"))
        assertTrue(savedRules.contains("@@||trusted.com^${'$'}document"))

        // Old handle 1 destroyed, new handle 2 active
        assertTrue(fakeBridge.destroyedHandles.contains(1L))
        assertTrue(fakeBridge.activeHandles.contains(2L))
    }
}
