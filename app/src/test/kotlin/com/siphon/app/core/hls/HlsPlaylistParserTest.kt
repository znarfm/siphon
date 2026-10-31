package com.siphon.app.core.hls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HlsPlaylistParserTest {

    @Test
    fun testParseMasterPlaylistWithVariantsAndAudio() {
        val masterContent = """
            #EXTM3U
            #EXT-X-VERSION:4
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio-aac",NAME="English",DEFAULT=YES,URI="audio/eng.m3u8"
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio-aac",NAME="Spanish",DEFAULT=NO,URI="audio/spa.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,CODECS="avc1.64002a,mp4a.40.2",AUDIO="audio-aac"
            video/1080p/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720,CODECS="avc1.4d401f,mp4a.40.2",AUDIO="audio-aac"
            video/720p/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360,CODECS="avc1.4d401e,mp4a.40.2"
            video/360p/index.m3u8
        """.trimIndent()

        assertTrue(HlsPlaylistParser.isMasterPlaylist(masterContent))

        val master = HlsPlaylistParser.parseMasterPlaylist(
            baseUrl = "https://cdn.example.com/stream/master.m3u8",
            content = masterContent
        )

        assertEquals(3, master.variants.size)
        // Check sorted by bandwidth descending
        assertEquals(5000000L, master.variants[0].bandwidth)
        assertEquals("1920x1080", master.variants[0].resolution)
        assertEquals("https://cdn.example.com/stream/video/1080p/index.m3u8", master.variants[0].url)
        assertEquals("audio-aac", master.variants[0].audioGroupId)

        assertEquals(2500000L, master.variants[1].bandwidth)
        assertEquals("1280x720", master.variants[1].resolution)

        assertEquals(2, master.audioTracks.size)
        assertEquals("English", master.audioTracks[0].name)
        assertTrue(master.audioTracks[0].isDefault)
        assertEquals("https://cdn.example.com/stream/audio/eng.m3u8", master.audioTracks[0].url)
    }

    @Test
    fun testParseMediaPlaylistWithAes128AndSegments() {
        val mediaContent = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-TARGETDURATION:6
            #EXT-X-MEDIA-SEQUENCE:100
            #EXT-X-KEY:METHOD=AES-128,URI="https://cdn.example.com/enc.key",IV=0x0123456789ABCDEF0123456789ABCDEF
            #EXTINF:6.0,
            seg_100.ts
            #EXTINF:5.5,
            seg_101.ts
            #EXT-X-KEY:METHOD=NONE
            #EXTINF:6.0,
            seg_102.ts
            #EXT-X-ENDLIST
        """.trimIndent()

        assertFalse(HlsPlaylistParser.isMasterPlaylist(mediaContent))

        val media = HlsPlaylistParser.parseMediaPlaylist(
            playlistUrl = "https://cdn.example.com/stream/video.m3u8",
            content = mediaContent,
            trackIndex = 0
        )

        assertEquals(6.0, media.targetDurationSec, 0.001)
        assertEquals(3, media.segments.size)

        // Segment 0
        val seg0 = media.segments[0]
        assertEquals(0, seg0.chunkIndex)
        assertEquals(100L, seg0.sequenceNumber)
        assertEquals("https://cdn.example.com/stream/seg_100.ts", seg0.url)
        assertEquals("AES-128", seg0.keyMethod)
        assertEquals("https://cdn.example.com/enc.key", seg0.keyUrl)
        assertEquals("0x0123456789ABCDEF0123456789ABCDEF", seg0.ivHex)
        assertEquals(6.0, seg0.durationSec, 0.001)

        // Segment 1 (inherits AES-128 key and IV)
        val seg1 = media.segments[1]
        assertEquals(1, seg1.chunkIndex)
        assertEquals(101L, seg1.sequenceNumber)
        assertEquals("AES-128", seg1.keyMethod)

        // Segment 2 (METHOD=NONE resets encryption)
        val seg2 = media.segments[2]
        assertEquals(2, seg2.chunkIndex)
        assertEquals(102L, seg2.sequenceNumber)
        assertNull(seg2.keyMethod)
        assertNull(seg2.keyUrl)
    }

    @Test
    fun testRelativeUrlResolution() {
        val base = "https://example.com/hls/v1/playlist.m3u8"
        assertEquals("https://example.com/hls/v1/seg.ts", HlsPlaylistParser.resolveUri(base, "seg.ts"))
        assertEquals("https://example.com/hls/seg.ts", HlsPlaylistParser.resolveUri(base, "../seg.ts"))
        assertEquals("https://example.com/root/seg.ts", HlsPlaylistParser.resolveUri(base, "/root/seg.ts"))
        assertEquals("https://other.com/seg.ts", HlsPlaylistParser.resolveUri(base, "https://other.com/seg.ts"))
    }

    @Test
    fun testExtractAttributeHyphenatedPrefixDisambiguation() {
        val line = """#EXT-X-STREAM-INF:AVERAGE-BANDWIDTH=1500000,BANDWIDTH=3000000,RESOLUTION=1920x1080,CODECS="avc1.64002a,mp4a.40.2",AUDIO="audio-aac""""
        // Must NOT match AVERAGE-BANDWIDTH when searching for BANDWIDTH
        assertEquals("3000000", HlsPlaylistParser.extractAttribute(line, "BANDWIDTH"))
        assertEquals("1500000", HlsPlaylistParser.extractAttribute(line, "AVERAGE-BANDWIDTH"))
        assertEquals("1920x1080", HlsPlaylistParser.extractAttribute(line, "RESOLUTION"))
        assertEquals("avc1.64002a,mp4a.40.2", HlsPlaylistParser.extractAttribute(line, "CODECS"))
        assertEquals("audio-aac", HlsPlaylistParser.extractAttribute(line, "AUDIO"))
        assertNull(HlsPlaylistParser.extractAttribute(line, "ID"))

        val mediaLine = """#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio-aac",NAME="English",DEFAULT=YES,URI="audio/eng.m3u8""""
        assertEquals("audio-aac", HlsPlaylistParser.extractAttribute(mediaLine, "GROUP-ID"))
        assertNull(HlsPlaylistParser.extractAttribute(mediaLine, "ID"))
    }

    @Test
    fun testParseMediaPlaylistWithExtXMapInitializationSegment() {
        val fmp4Content = """
            #EXTM3U
            #EXT-X-VERSION:7
            #EXT-X-TARGETDURATION:4
            #EXT-X-MEDIA-SEQUENCE:50
            #EXT-X-KEY:METHOD=AES-128,URI="https://cdn.example.com/fmp4.key",IV=0x11223344556677889900AABBCCDDEEFF
            #EXT-X-MAP:URI="init.mp4"
            #EXTINF:4.0,
            segment_50.m4s
            #EXTINF:4.0,
            segment_51.m4s
            #EXT-X-ENDLIST
        """.trimIndent()

        val parsed = HlsPlaylistParser.parseMediaPlaylist(
            playlistUrl = "https://cdn.example.com/stream/index.m3u8",
            content = fmp4Content,
            trackIndex = 0
        )

        assertEquals(3, parsed.segments.size)

        // Init segment must be chunkIndex 0 before media segments
        val initSeg = parsed.segments[0]
        assertEquals(0, initSeg.chunkIndex)
        assertEquals(50L, initSeg.sequenceNumber)
        assertEquals("https://cdn.example.com/stream/init.mp4", initSeg.url)
        assertEquals(0.0, initSeg.durationSec, 0.001)
        assertEquals("AES-128", initSeg.keyMethod)
        assertEquals("https://cdn.example.com/fmp4.key", initSeg.keyUrl)
        assertEquals("0x11223344556677889900AABBCCDDEEFF", initSeg.ivHex)

        // Subsequent media segment
        val seg0 = parsed.segments[1]
        assertEquals(1, seg0.chunkIndex)
        assertEquals(50L, seg0.sequenceNumber)
        assertEquals("https://cdn.example.com/stream/segment_50.m4s", seg0.url)
        assertEquals(4.0, seg0.durationSec, 0.001)

        val seg1 = parsed.segments[2]
        assertEquals(2, seg1.chunkIndex)
        assertEquals(51L, seg1.sequenceNumber)
        assertEquals("https://cdn.example.com/stream/segment_51.m4s", seg1.url)
        assertEquals(4.0, seg1.durationSec, 0.001)
    }

    @Test
    fun testParseMediaPlaylistWithExtXMapUnencrypted() {
        val fmp4Content = """
            #EXTM3U
            #EXT-X-VERSION:7
            #EXT-X-TARGETDURATION:4
            #EXT-X-MEDIA-SEQUENCE:1
            #EXT-X-MAP:URI="init.mp4"
            #EXTINF:4.0,
            seg_1.m4s
            #EXT-X-ENDLIST
        """.trimIndent()

        val parsed = HlsPlaylistParser.parseMediaPlaylist(
            playlistUrl = "https://cdn.example.com/stream/index.m3u8",
            content = fmp4Content,
            trackIndex = 0
        )

        assertEquals(2, parsed.segments.size)
        val initSeg = parsed.segments[0]
        assertEquals(0, initSeg.chunkIndex)
        assertEquals("https://cdn.example.com/stream/init.mp4", initSeg.url)
        assertNull(initSeg.keyMethod)
        assertNull(initSeg.keyUrl)
        assertNull(initSeg.ivHex)
    }
}
