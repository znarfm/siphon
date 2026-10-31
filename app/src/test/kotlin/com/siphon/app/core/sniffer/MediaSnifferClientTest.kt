package com.siphon.app.core.sniffer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSnifferClientTest {

    @Test
    fun testNormalizeByteRangeUrlStripsRangeParamsOnly() {
        val original = "https://cdn.example.com/video.mp4?range=0-1048576&bytes=1000&token=abc123xyz&sig=999#time=10"
        val normalized = MediaSnifferClient.normalizeByteRangeUrl(original)

        // Range query parameters and fragment must be removed, while token and signature are preserved
        assertFalse(normalized.contains("range="))
        assertFalse(normalized.contains("bytes="))
        assertFalse(normalized.contains("#time=10"))
        assertTrue(normalized.contains("token=abc123xyz"))
        assertTrue(normalized.contains("sig=999"))
    }

    @Test
    fun testIsCandidateMediaUrlByExtension() {
        assertTrue(MediaSnifferClient.isCandidateMediaUrl("https://example.com/movie.mp4", emptyMap()))
        assertTrue(MediaSnifferClient.isCandidateMediaUrl("https://example.com/stream.m3u8", emptyMap()))
        assertTrue(MediaSnifferClient.isCandidateMediaUrl("https://example.com/clip.webm", emptyMap()))
        assertTrue(MediaSnifferClient.isCandidateMediaUrl("https://example.com/audio.m4a", emptyMap()))
        assertTrue(MediaSnifferClient.isCandidateMediaUrl("https://example.com/track.mp3", emptyMap()))

        assertFalse(MediaSnifferClient.isCandidateMediaUrl("https://example.com/index.html", emptyMap()))
        assertFalse(MediaSnifferClient.isCandidateMediaUrl("https://example.com/styles.css", emptyMap()))
        assertFalse(MediaSnifferClient.isCandidateMediaUrl("https://example.com/bundle.js", emptyMap()))
    }

    @Test
    fun testIsCandidateMediaUrlByHeaders() {
        // Without media extension, Sec-Fetch-Dest: video or Accept: video/* marks it as a candidate
        val headersVideo = mapOf("Sec-Fetch-Dest" to "video")
        assertTrue(MediaSnifferClient.isCandidateMediaUrl("https://example.com/media_stream?id=42", headersVideo))

        val headersAudio = mapOf("Accept" to "audio/webm,audio/*;q=0.9")
        assertTrue(MediaSnifferClient.isCandidateMediaUrl("https://example.com/audio_stream?id=42", headersAudio))

        val headersHtml = mapOf("Accept" to "text/html,application/xhtml+xml")
        assertFalse(MediaSnifferClient.isCandidateMediaUrl("https://example.com/page", headersHtml))
    }

    @Test
    fun testSegmentUrlDetection() {
        assertTrue(MediaSnifferClient.isSegmentUrl("https://example.com/hls/seg-1.ts"))
        assertTrue(MediaSnifferClient.isSegmentUrl("https://example.com/hls/chunk.m4s"))
        assertTrue(MediaSnifferClient.isSegmentUrl("https://example.com/hls/segment/10?token=abc"))

        assertFalse(MediaSnifferClient.isSegmentUrl("https://example.com/video.mp4"))
        assertFalse(MediaSnifferClient.isSegmentUrl("https://example.com/stream.m3u8"))
    }

    @Test
    fun testHlsUrlDetection() {
        assertTrue(MediaSnifferClient.isHlsUrl("https://example.com/live/master.m3u8"))
        assertTrue(MediaSnifferClient.isHlsUrl("https://example.com/live/stream.m3u8?token=xyz"))
        assertFalse(MediaSnifferClient.isHlsUrl("https://example.com/live/stream.mp4"))
    }

    @Test
    fun testParseHlsMasterVariants() {
        val master = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1920x1080,CODECS="avc1.640028"
            1080p.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=1500000,RESOLUTION=1280x720,CODECS="avc1.4d401f"
            720p.m3u8
        """.trimIndent()

        val variants = MediaSnifferClient.parseHlsMasterVariants("https://example.com/hls/master.m3u8", master)
        assertEquals(2, variants.size)
        assertEquals("https://example.com/hls/1080p.m3u8", variants[0].url)
        assertEquals(3000000L, variants[0].bandwidth)
        assertEquals("1920x1080", variants[0].resolution)

        assertEquals("https://example.com/hls/720p.m3u8", variants[1].url)
        assertEquals(1500000L, variants[1].bandwidth)
        assertEquals("1280x720", variants[1].resolution)
    }

    @Test
    fun testExtractDisplayNameReplacesM3u8WithMp4() {
        assertEquals("index.mp4", MediaSnifferClient.extractDisplayNameFromUrl("https://example.com/movie/index.m3u8", "fallback"))
        assertEquals("track.mp3", MediaSnifferClient.extractDisplayNameFromUrl("https://example.com/audio/track.mp3", "fallback"))
        assertEquals("fallback", MediaSnifferClient.extractDisplayNameFromUrl("https://example.com/", "fallback"))
    }

    @Test
    fun testAacIsMediaNotSegment() {
        assertTrue(MediaSnifferClient.isCandidateMediaUrl("https://example.com/song.aac", emptyMap()))
        assertFalse(MediaSnifferClient.isSegmentUrl("https://example.com/song.aac"))
    }
}
