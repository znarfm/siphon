package com.znarfm.siphon.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class SmartRoutingTest {

    @Test
    fun resolveCategoryFolder_videoByExtensionAndMime() {
        assertEquals("Video", SmartRouting.resolveCategoryFolder("movie.mp4", "application/octet-stream"))
        assertEquals("Video", SmartRouting.resolveCategoryFolder("clip.MKV", ""))
        assertEquals("Video", SmartRouting.resolveCategoryFolder("stream.webm", "video/webm"))
        assertEquals("Video", SmartRouting.resolveCategoryFolder("no_extension", "video/mp4"))
        assertEquals("Video", SmartRouting.resolveCategoryFolder("recording.ts", "application/octet-stream"))
    }

    @Test
    fun resolveCategoryFolder_audioByExtensionAndMime() {
        assertEquals("Audio", SmartRouting.resolveCategoryFolder("track.mp3", "application/octet-stream"))
        assertEquals("Audio", SmartRouting.resolveCategoryFolder("lossless.FLAC", ""))
        assertEquals("Audio", SmartRouting.resolveCategoryFolder("voice.m4a", "audio/mp4"))
        assertEquals("Audio", SmartRouting.resolveCategoryFolder("podcast.webm", "audio/webm"))
        assertEquals("Audio", SmartRouting.resolveCategoryFolder("no_extension", "audio/mpeg"))
    }

    @Test
    fun resolveCategoryFolder_imagesByExtensionAndMime() {
        assertEquals("Images", SmartRouting.resolveCategoryFolder("photo.jpg", "application/octet-stream"))
        assertEquals("Images", SmartRouting.resolveCategoryFolder("diagram.PNG", ""))
        assertEquals("Images", SmartRouting.resolveCategoryFolder("vector.svg", "image/svg+xml"))
        assertEquals("Images", SmartRouting.resolveCategoryFolder("pic.heic", "application/octet-stream"))
        assertEquals("Images", SmartRouting.resolveCategoryFolder("no_extension", "image/webp"))
    }

    @Test
    fun resolveCategoryFolder_documentsByExtensionAndMime() {
        assertEquals("Documents", SmartRouting.resolveCategoryFolder("report.pdf", "application/pdf"))
        assertEquals("Documents", SmartRouting.resolveCategoryFolder("notes.txt", "application/octet-stream"))
        assertEquals("Documents", SmartRouting.resolveCategoryFolder("sheet.xlsx", "application/octet-stream"))
        assertEquals("Documents", SmartRouting.resolveCategoryFolder("book.epub", ""))
        assertEquals("Documents", SmartRouting.resolveCategoryFolder("config.json", "application/json"))
        assertEquals("Documents", SmartRouting.resolveCategoryFolder("readme", "text/plain; charset=utf-8"))
        assertEquals(
            "Documents",
            SmartRouting.resolveCategoryFolder(
                "doc_no_ext",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
        )
    }

    @Test
    fun resolveCategoryFolder_archivesByExtensionAndMime() {
        assertEquals("Archives", SmartRouting.resolveCategoryFolder("bundle.zip", "application/octet-stream"))
        assertEquals("Archives", SmartRouting.resolveCategoryFolder("backup.tar.gz", "application/gzip"))
        assertEquals("Archives", SmartRouting.resolveCategoryFolder("data.7z", ""))
        assertEquals("Archives", SmartRouting.resolveCategoryFolder("distro.iso", "application/octet-stream"))
        assertEquals("Archives", SmartRouting.resolveCategoryFolder("no_extension", "application/x-rar-compressed"))
    }

    @Test
    fun resolveCategoryFolder_appsByExtensionAndMime() {
        assertEquals("Apps", SmartRouting.resolveCategoryFolder("siphon.apk", "application/vnd.android.package-archive"))
        assertEquals("Apps", SmartRouting.resolveCategoryFolder("siphon.apk", "application/zip"))
        assertEquals("Apps", SmartRouting.resolveCategoryFolder("installer.exe", "application/octet-stream"))
        assertEquals("Apps", SmartRouting.resolveCategoryFolder("package.deb", ""))
        assertEquals("Apps", SmartRouting.resolveCategoryFolder("no_extension", "application/vnd.android.package-archive"))
    }

    @Test
    fun resolveCategoryFolder_torrentsByExtensionAndMime() {
        assertEquals("Torrents", SmartRouting.resolveCategoryFolder("ubuntu.torrent", "application/octet-stream"))
        assertEquals("Torrents", SmartRouting.resolveCategoryFolder("ubuntu.torrent", "text/plain"))
        assertEquals("Torrents", SmartRouting.resolveCategoryFolder("no_extension", "application/x-bittorrent"))
    }

    @Test
    fun resolveCategoryFolder_otherFallback() {
        assertEquals("Other", SmartRouting.resolveCategoryFolder("firmware.bin", "application/octet-stream"))
        assertEquals("Other", SmartRouting.resolveCategoryFolder("unknown", ""))
        assertEquals("Other", SmartRouting.resolveCategoryFolder(".hidden", "application/octet-stream"))
    }

    @Test
    fun resolveUniqueName_noCollisionReturnsDesiredName() {
        assertEquals("video.mp4", SmartRouting.resolveUniqueName("video.mp4", emptySet()))
        assertEquals(
            "video.mp4",
            SmartRouting.resolveUniqueName("video.mp4", setOf("other.mp4", "video.mkv"))
        )
    }

    @Test
    fun resolveUniqueName_standardExtensionCollisionsCaseInsensitive() {
        assertEquals(
            "movie (1).mp4",
            SmartRouting.resolveUniqueName("movie.mp4", setOf("movie.mp4"))
        )
        assertEquals(
            "movie (1).mp4",
            SmartRouting.resolveUniqueName("movie.mp4", setOf("MOVIE.MP4"))
        )
        assertEquals(
            "movie (3).mp4",
            SmartRouting.resolveUniqueName(
                "movie.mp4",
                setOf("movie.mp4", "Movie (1).mp4", "MOVIE (2).MP4")
            )
        )
    }

    @Test
    fun resolveUniqueName_multiDotAndCompoundExtensions() {
        assertEquals(
            "archive (1).tar.gz",
            SmartRouting.resolveUniqueName("archive.tar.gz", setOf("archive.tar.gz"))
        )
        assertEquals(
            "archive (2).tar.bz2",
            SmartRouting.resolveUniqueName(
                "archive.tar.bz2",
                setOf("ARCHIVE.TAR.BZ2", "archive (1).tar.bz2")
            )
        )
        assertEquals(
            "my.cool.file (1).txt",
            SmartRouting.resolveUniqueName("my.cool.file.txt", setOf("my.cool.file.txt"))
        )
    }

    @Test
    fun resolveUniqueName_noExtensionAndDotfiles() {
        assertEquals(
            "README (1)",
            SmartRouting.resolveUniqueName("README", setOf("readme"))
        )
        assertEquals(
            ".gitignore (1)",
            SmartRouting.resolveUniqueName(".gitignore", setOf(".gitignore"))
        )
    }
}
