package com.siphon.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractionTest {

    @Test
    fun extractUrl_directHttpUrl() {
        val input = "https://example.com/video.mp4"
        assertEquals("https://example.com/video.mp4", MainActivity.extractUrlFromText(input))
    }

    @Test
    fun extractUrl_embeddedInShareText() {
        val input = "Check out this video: https://cdn.site.org/stream/master.m3u8 found online"
        assertEquals("https://cdn.site.org/stream/master.m3u8", MainActivity.extractUrlFromText(input))
    }

    @Test
    fun extractUrl_trailingPunctuationStripped() {
        val input = "See: https://example.com/watch?v=123."
        assertEquals("https://example.com/watch?v=123", MainActivity.extractUrlFromText(input))
    }

    @Test
    fun extractUrl_invalidTextReturnsNull() {
        assertNull(MainActivity.extractUrlFromText("Just a random text without link"))
        assertNull(MainActivity.extractUrlFromText(""))
        assertNull(MainActivity.extractUrlFromText(null))
    }
}
