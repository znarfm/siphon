package com.siphon.app.ui.sniffer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnifferTabDetectionTest {

    @Test
    fun testSameUrlMatches() {
        assertTrue(isCurrentTabMedia("https://example.com/video.mp4", "https://example.com/video.mp4"))
        assertTrue(isCurrentTabMedia("https://example.com/watch?v=123#time=10", "https://example.com/watch?v=123"))
    }

    @Test
    fun testDifferentHostsDoNotMatch() {
        assertFalse(isCurrentTabMedia("https://site-a.com/video", "https://site-b.com/video"))
    }

    @Test
    fun testSameHostDifferentPathsDoNotMatch() {
        assertFalse(isCurrentTabMedia("https://example.com/movie/matrix", "https://example.com/movie/avatar"))
    }

    @Test
    fun testSameHostDifferentVideoIdsDoNotMatch() {
        assertFalse(isCurrentTabMedia("https://www.youtube.com/watch?v=videoA", "https://m.youtube.com/watch?v=videoB"))
        assertTrue(isCurrentTabMedia("https://www.youtube.com/watch?v=videoA&feature=share", "https://m.youtube.com/watch?v=videoA"))
    }

    @Test
    fun testUnencodedCharactersDoNotCrash() {
        assertTrue(isCurrentTabMedia("https://example.com/watch?q=hello world&v=abc", "https://example.com/watch?q=hello world&v=abc"))
        assertFalse(isCurrentTabMedia("https://example.com/watch?q=hello world&v=abc", "https://example.com/watch?q=hello world&v=def"))
    }

    @Test
    fun testBlankOrNullUrlsDoNotMatch() {
        assertFalse(isCurrentTabMedia("", "https://example.com/"))
        assertFalse(isCurrentTabMedia("https://example.com/", null))
        assertFalse(isCurrentTabMedia("https://example.com/", ""))
    }
}
