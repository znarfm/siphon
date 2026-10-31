package com.siphon.app.core.sniffer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnifferUrlClassifierTest {

    @Test
    fun testNormalizeByteRangeUrlStripsRangeParamsOnly() {
        val original = "https://cdn.example.com/video.mp4?range=0-1048576&bytes=1000&token=abc123xyz&sig=999#time=10"
        val normalized = SnifferUrlClassifier.normalizeByteRangeUrl(original)

        assertFalse(normalized.contains("range="))
        assertFalse(normalized.contains("bytes="))
        assertFalse(normalized.contains("#time=10"))
        assertTrue(normalized.contains("token=abc123xyz"))
        assertTrue(normalized.contains("sig=999"))
    }

    @Test
    fun testIsCandidateMediaUrlByExtension() {
        assertTrue(SnifferUrlClassifier.isCandidateMediaUrl("https://example.com/movie.mp4", emptyMap()))
        assertTrue(SnifferUrlClassifier.isCandidateMediaUrl("https://example.com/stream.m3u8", emptyMap()))
        assertTrue(SnifferUrlClassifier.isCandidateMediaUrl("https://example.com/clip.webm", emptyMap()))
        assertTrue(SnifferUrlClassifier.isCandidateMediaUrl("https://example.com/audio.m4a", emptyMap()))
        assertTrue(SnifferUrlClassifier.isCandidateMediaUrl("https://example.com/track.mp3", emptyMap()))
        assertTrue(SnifferUrlClassifier.isCandidateMediaUrl("https://example.com/song.aac", emptyMap()))

        assertFalse(SnifferUrlClassifier.isCandidateMediaUrl("https://example.com/index.html", emptyMap()))
        assertFalse(SnifferUrlClassifier.isCandidateMediaUrl("https://example.com/styles.css", emptyMap()))
        assertFalse(SnifferUrlClassifier.isCandidateMediaUrl("https://example.com/bundle.js", emptyMap()))
        assertFalse(SnifferUrlClassifier.isCandidateMediaUrl("https://example.com/avatar.png", emptyMap()))
    }

    @Test
    fun testIsCandidateMediaUrlByHeaders() {
        val headersVideo = mapOf("Sec-Fetch-Dest" to "video")
        assertTrue(SnifferUrlClassifier.isCandidateMediaUrl("https://example.com/media_stream?id=42", headersVideo))

        val headersAudio = mapOf("Accept" to "audio/webm,audio/*;q=0.9")
        assertTrue(SnifferUrlClassifier.isCandidateMediaUrl("https://example.com/audio_stream?id=42", headersAudio))

        val headersHtml = mapOf("Accept" to "text/html,application/xhtml+xml")
        assertFalse(SnifferUrlClassifier.isCandidateMediaUrl("https://example.com/page", headersHtml))
    }

    @Test
    fun testSegmentUrlDetection() {
        assertTrue(SnifferUrlClassifier.isSegmentUrl("https://example.com/hls/seg-1.ts"))
        assertTrue(SnifferUrlClassifier.isSegmentUrl("https://example.com/hls/chunk.m4s"))
        assertTrue(SnifferUrlClassifier.isSegmentUrl("https://example.com/hls/segment/10?token=abc"))

        assertFalse(SnifferUrlClassifier.isSegmentUrl("https://example.com/video.mp4"))
        assertFalse(SnifferUrlClassifier.isSegmentUrl("https://example.com/stream.m3u8"))
        assertFalse(SnifferUrlClassifier.isSegmentUrl("https://example.com/song.aac"))
    }

    @Test
    fun testHlsUrlDetection() {
        assertTrue(SnifferUrlClassifier.isHlsUrl("https://example.com/live/master.m3u8"))
        assertTrue(SnifferUrlClassifier.isHlsUrl("https://example.com/live/stream.m3u8?token=xyz"))
        assertFalse(SnifferUrlClassifier.isHlsUrl("https://example.com/live/stream.mp4"))
    }

    @Test
    fun testSubtitleClassification() {
        assertTrue(SnifferUrlClassifier.isSubtitleUrl("https://example.com/subtitles/en.vtt"))
        assertTrue(SnifferUrlClassifier.isSubtitleUrl("https://example.com/subtitles/movie.srt"))
        assertTrue(SnifferUrlClassifier.isSubtitleUrl("https://example.com/caption?format=vtt"))
        assertFalse(SnifferUrlClassifier.isSubtitleUrl("https://example.com/video.mp4"))

        assertTrue(SnifferUrlClassifier.isSubtitleMime("text/vtt"))
        assertTrue(SnifferUrlClassifier.isSubtitleMime("application/x-subrip"))
        assertTrue(SnifferUrlClassifier.isSubtitleMime("text/srt"))
        assertFalse(SnifferUrlClassifier.isSubtitleMime("video/mp4"))

        assertEquals("vtt", SnifferUrlClassifier.subtitleExtFromUrlOrMime("https://example.com/caption", "text/vtt"))
        assertEquals("srt", SnifferUrlClassifier.subtitleExtFromUrlOrMime("https://example.com/sub", "application/x-subrip"))
        assertEquals("text/vtt", SnifferUrlClassifier.subtitleMimeForExt("vtt"))
        assertEquals("application/x-subrip", SnifferUrlClassifier.subtitleMimeForExt("srt"))
    }

    @Test
    fun testInferLanguageFromUrl() {
        assertEquals("en", SnifferUrlClassifier.inferLanguageFromUrl("https://example.com/caption?lang=en"))
        assertEquals("es", SnifferUrlClassifier.inferLanguageFromUrl("https://example.com/subs.es.vtt"))
        assertEquals("fr", SnifferUrlClassifier.inferLanguageFromUrl("https://example.com/movie_fr_sub.srt"))
        assertNull(SnifferUrlClassifier.inferLanguageFromUrl("https://example.com/video.mp4"))
    }

    @Test
    fun testBuildSubtitleDisplayName() {
        val nameWithLang = SnifferUrlClassifier.buildSubtitleDisplayName("https://example.com/video.mp4", "English", "en", "vtt")
        assertTrue(nameWithLang.contains("en.vtt"))
    }

    @Test
    fun testAudioDetection() {
        assertTrue(SnifferUrlClassifier.isAudioUrl("https://example.com/podcast/ep1.mp3"))
        assertTrue(SnifferUrlClassifier.isAudioUrl("https://example.com/radio/stream.aac"))
        assertTrue(SnifferUrlClassifier.isAudioUrl("https://example.com/track.flac"))
        assertFalse(SnifferUrlClassifier.isAudioUrl("https://example.com/video.mp4"))

        assertTrue(SnifferUrlClassifier.isAudioMime("audio/mpeg"))
        assertTrue(SnifferUrlClassifier.isAudioMime("audio/mp4"))
        assertTrue(SnifferUrlClassifier.isAudioMime("application/ogg"))
        assertFalse(SnifferUrlClassifier.isAudioMime("video/mp4"))

        assertTrue(SnifferUrlClassifier.isAudioHls("#EXTINF:10.0,\nseg1.aac", "https://radio.example.com/live.m3u8"))
        assertTrue(SnifferUrlClassifier.isAudioHls("#EXT-X-MEDIA:TYPE=AUDIO,NAME=\"Stream\"", "https://example.com/stream.m3u8"))
        assertFalse(SnifferUrlClassifier.isAudioHls("#EXTINF:10.0,\nseg1.ts", "https://example.com/video.m3u8"))
    }

    @Test
    fun testExtractDisplayNameFromUrl() {
        assertEquals("index.mp4", SnifferUrlClassifier.extractDisplayNameFromUrl("https://example.com/movie/index.m3u8", "fallback"))
        assertEquals("podcast.m4a", SnifferUrlClassifier.extractDisplayNameFromUrl("https://example.com/audio/podcast.m3u8", "fallback", isAudio = true))
        assertEquals("track.mp3", SnifferUrlClassifier.extractDisplayNameFromUrl("https://example.com/audio/track.mp3", "fallback"))
        assertEquals("fallback", SnifferUrlClassifier.extractDisplayNameFromUrl("https://example.com/", "fallback"))
    }
}
