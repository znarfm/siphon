package com.znarfm.siphon.ui.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TabNavigationHelperTest {

    private val mixedTabs = listOf(
        BrowserTab(id = "n1", url = "https://one.example", isIncognito = false),
        BrowserTab(id = "i1", url = "https://incog1.example", isIncognito = true),
        BrowserTab(id = "n2", url = "https://two.example", isIncognito = false),
        BrowserTab(id = "i2", url = "https://incog2.example", isIncognito = true),
        BrowserTab(id = "n3", url = "https://three.example", isIncognito = false),
        BrowserTab(id = "i3", url = "https://incog3.example", isIncognito = true)
    )

    @Test
    fun normalTabsMoveWithinNormalTabsOnlyAndStopAtEnds() {
        assertNull(findAdjacentTabId(mixedTabs, "n1", delta = -1))
        assertEquals("n2", findAdjacentTabId(mixedTabs, "n1", delta = 1))

        assertEquals("n1", findAdjacentTabId(mixedTabs, "n2", delta = -1))
        assertEquals("n3", findAdjacentTabId(mixedTabs, "n2", delta = 1))

        assertEquals("n2", findAdjacentTabId(mixedTabs, "n3", delta = -1))
        assertNull(findAdjacentTabId(mixedTabs, "n3", delta = 1))
    }

    @Test
    fun incognitoTabsMoveWithinIncognitoTabsOnlyAndStopAtEnds() {
        assertNull(findAdjacentTabId(mixedTabs, "i1", delta = -1))
        assertEquals("i2", findAdjacentTabId(mixedTabs, "i1", delta = 1))

        assertEquals("i1", findAdjacentTabId(mixedTabs, "i2", delta = -1))
        assertEquals("i3", findAdjacentTabId(mixedTabs, "i2", delta = 1))

        assertEquals("i2", findAdjacentTabId(mixedTabs, "i3", delta = -1))
        assertNull(findAdjacentTabId(mixedTabs, "i3", delta = 1))
    }

    @Test
    fun singleTabReturnsNullForBothDirections() {
        val singleNormal = listOf(BrowserTab(id = "only", isIncognito = false))
        assertNull(findAdjacentTabId(singleNormal, "only", delta = -1))
        assertNull(findAdjacentTabId(singleNormal, "only", delta = 1))

        val singleInMode = listOf(
            BrowserTab(id = "n1", isIncognito = false),
            BrowserTab(id = "i1", isIncognito = true)
        )
        assertNull(findAdjacentTabId(singleInMode, "n1", delta = -1))
        assertNull(findAdjacentTabId(singleInMode, "n1", delta = 1))
        assertNull(findAdjacentTabId(singleInMode, "i1", delta = -1))
        assertNull(findAdjacentTabId(singleInMode, "i1", delta = 1))
    }

    @Test
    fun missingActiveTabIdOrEmptyListReturnsNull() {
        assertNull(findAdjacentTabId(mixedTabs, "missing", delta = -1))
        assertNull(findAdjacentTabId(mixedTabs, "missing", delta = 1))
        assertNull(findAdjacentTabId(emptyList(), "n1", delta = 1))
    }
}
