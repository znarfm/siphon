package com.znarfm.siphon.core.sniffer

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
    fun testExtractDisplayNameReplacesM3u8WithMp4() {
        assertEquals("index.mp4", MediaSnifferClient.extractDisplayNameFromUrl("https://example.com/movie/index.m3u8", "fallback"))
        assertEquals("podcast.m4a", MediaSnifferClient.extractDisplayNameFromUrl("https://example.com/audio/podcast.m3u8", "fallback", isAudio = true))
        assertEquals("track.mp3", MediaSnifferClient.extractDisplayNameFromUrl("https://example.com/audio/track.mp3", "fallback"))
        assertEquals("fallback", MediaSnifferClient.extractDisplayNameFromUrl("https://example.com/", "fallback"))
    }

    @Test
    fun testAudioDetection() {
        assertTrue(MediaSnifferClient.isAudioUrl("https://example.com/podcast/ep1.mp3"))
        assertTrue(MediaSnifferClient.isAudioUrl("https://example.com/radio/stream.aac"))
        assertTrue(MediaSnifferClient.isAudioUrl("https://example.com/track.flac"))
        assertFalse(MediaSnifferClient.isAudioUrl("https://example.com/video.mp4"))

        assertTrue(MediaSnifferClient.isAudioMime("audio/mpeg"))
        assertTrue(MediaSnifferClient.isAudioMime("audio/mp4"))
        assertTrue(MediaSnifferClient.isAudioMime("application/ogg"))
        assertFalse(MediaSnifferClient.isAudioMime("video/mp4"))

        assertTrue(MediaSnifferClient.isAudioHls("#EXTINF:10.0,\nseg1.aac", "https://radio.example.com/live.m3u8"))
        assertTrue(MediaSnifferClient.isAudioHls("#EXT-X-MEDIA:TYPE=AUDIO,NAME=\"Stream\"", "https://example.com/stream.m3u8"))
        assertFalse(MediaSnifferClient.isAudioHls("#EXTINF:10.0,\nseg1.ts", "https://example.com/video.m3u8"))
    }

    @Test
    fun testSnifferJsInjectorContainsBypassAndInterception() {
        val js = SnifferJsInjector.buildUserscript("soundcloud.com")
        assertTrue(js.contains("__siphon_sniffer__"))
        assertTrue(js.contains("WebSocket"))
        assertTrue(js.contains("fetch"))
        assertTrue(js.contains("canRunAds"))
        assertTrue(js.contains("__sc_hydration"))
    }

    @Test
    fun testAacIsMediaNotSegment() {
        assertTrue(MediaSnifferClient.isCandidateMediaUrl("https://example.com/song.aac", emptyMap()))
        assertFalse(MediaSnifferClient.isSegmentUrl("https://example.com/song.aac"))
    }
}
