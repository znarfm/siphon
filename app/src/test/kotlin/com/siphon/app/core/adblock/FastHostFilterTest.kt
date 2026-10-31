package com.siphon.app.core.adblock

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class FastHostFilterTest {

    private lateinit var filter: FastHostFilter

    @Before
    fun setup() {
        filter = FastHostFilter()
        val hostsContent = """
            # Test blocklist
            doubleclick.net
            googleadservices.com
            adservice.google.com
            ads.example.com
            popads.net
        """.trimIndent()
        runBlocking {
            filter.loadFromStream(ByteArrayInputStream(hostsContent.toByteArray()))
        }
    }

    @Test
    fun testExactDomainMatches() {
        assertTrue(filter.shouldBlockHost("doubleclick.net"))
        assertTrue(filter.shouldBlockHost("popads.net"))
        assertTrue(filter.shouldBlockHost("googleadservices.com"))
    }

    @Test
    fun testSubdomainSuffixSlicing() {
        // Any subdomains of blocked domain should match
        assertTrue(filter.shouldBlockHost("sub.doubleclick.net"))
        assertTrue(filter.shouldBlockHost("deep.nested.sub.doubleclick.net"))
        assertTrue(filter.shouldBlockHost("tracker.ads.example.com"))
    }

    @Test
    fun testNonBlockedDomains() {
        assertFalse(filter.shouldBlockHost("example.com"))
        assertFalse(filter.shouldBlockHost("google.com"))
        assertFalse(filter.shouldBlockHost("wikipedia.org"))
    }

    @Test
    fun testDomainBoundarySafety() {
        // notdoubleclick.net ends with "doubleclick.net" as a substring, but NOT as a subdomain suffix!
        assertFalse(filter.shouldBlockHost("notdoubleclick.net"))
        assertFalse(filter.shouldBlockHost("fakepopads.net"))
    }

    @Test
    fun testCaseInsensitiveAndWhitespace() {
        assertTrue(filter.shouldBlockHost("DOUBLECLICK.NET"))
        assertTrue(filter.shouldBlockHost("  Sub.DoubleClick.Net.  "))
    }

    @Test
    fun testToggleDisabled() {
        filter.isEnabled = false
        assertFalse(filter.shouldBlockHost("doubleclick.net"))
        assertFalse(filter.shouldBlockHost("sub.doubleclick.net"))
    }

    @Test
    fun testShouldBlockUrlWithUserinfoAndPort() {
        assertTrue(filter.shouldBlockUrl("http://user:password@doubleclick.net/ad.js"))
        assertTrue(filter.shouldBlockUrl("https://doubleclick.net:8080/path?query=1"))
        assertTrue(filter.shouldBlockUrl("https://user@sub.doubleclick.net:443/"))
        assertFalse(filter.shouldBlockUrl("https://user:password@example.com:8080/good.js"))
    }

    @Test
    fun testExtractHostFromRule() {
        assertEquals("adservice.google.com", FastHostFilter.extractHostFromRule("||adservice.google.com^"))
        assertEquals("tracking.domain.net", FastHostFilter.extractHostFromRule("||tracking.domain.net^${'$'}third-party"))
        assertEquals("ads.example.com", FastHostFilter.extractHostFromRule("0.0.0.0 ads.example.com"))
        assertEquals("ads.example.com", FastHostFilter.extractHostFromRule("127.0.0.1 ads.example.com # comment"))
        assertEquals("popunder.org", FastHostFilter.extractHostFromRule("popunder.org"))

        assertNull(FastHostFilter.extractHostFromRule("! Title: EasyList"))
        assertNull(FastHostFilter.extractHostFromRule("# Comment"))
        assertNull(FastHostFilter.extractHostFromRule("##.ad-banner"))
        assertNull(FastHostFilter.extractHostFromRule("@@||goodsite.com^"))
        assertNull(FastHostFilter.extractHostFromRule("[Adblock Plus 2.0]"))
    }

    @Test
    fun testEdgeCases() {
        assertFalse(filter.shouldBlockHost(""))
        assertFalse(filter.shouldBlockHost(null))
        assertFalse(filter.shouldBlockHost("com"))
        assertFalse(filter.shouldBlockHost("localhost"))
    }

    @Test
    fun testExtractAllowHostFromRule() {
        assertEquals("bit.ly", FastHostFilter.extractAllowHostFromRule("@@||bit.ly^"))
        assertEquals("tinyurl.com", FastHostFilter.extractAllowHostFromRule("@@||tinyurl.com^${'$'}document"))
        assertEquals("linktr.ee", FastHostFilter.extractAllowHostFromRule("@@||linktr.ee/"))
        assertEquals("directsite.org", FastHostFilter.extractAllowHostFromRule("@@directsite.org^"))
        assertEquals("plainshort.io", FastHostFilter.extractAllowHostFromRule("plainshort.io"))
        assertNull(FastHostFilter.extractAllowHostFromRule("! Comment"))
        assertNull(FastHostFilter.extractAllowHostFromRule("# Comment"))
        assertNull(FastHostFilter.extractAllowHostFromRule("##.ad-banner"))
        assertNull(FastHostFilter.extractAllowHostFromRule("@@||bad*domain.com^"))
    }

    @Test
    fun testAllowlistPrecedenceOverBlocklist() {
        // "popads.net" is in blocklist from setup
        assertTrue(filter.shouldBlockHost("popads.net"))

        val allowContent = """
            @@||popads.net^
            @@||redirect.service.com^
        """.trimIndent()
        runBlocking {
            filter.loadAllowlistFromStream(ByteArrayInputStream(allowContent.toByteArray()))
        }

        // Now popads.net and its subdomains should NOT be blocked
        assertFalse(filter.shouldBlockHost("popads.net"))
        assertFalse(filter.shouldBlockHost("sub.popads.net"))
        assertTrue(filter.isAllowedHost("popads.net"))
        assertTrue(filter.isAllowedHost("sub.popads.net"))

        // Unallowed blocked hosts still blocked
        assertTrue(filter.shouldBlockHost("doubleclick.net"))
    }

    @Test
    fun testValidSubscriptionUrls() {
        assertFalse(FastHostFilter.DANDELION_SHORTENERS_URL.contains("refs/heads"))
        assertTrue(FastHostFilter.DANDELION_SHORTENERS_URL.contains("jsdelivr.net"))
        val config = AdblockConfig()
        assertTrue(config.toBlockUrls().contains(FastHostFilter.HAGEZI_NORMAL_URL))
        assertTrue(config.toAllowUrls().contains(FastHostFilter.DANDELION_SHORTENERS_URL))
    }
}
