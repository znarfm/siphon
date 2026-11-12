package com.znarfm.siphon.core.storage

import java.util.Locale

object SmartRouting {

    private val TORRENT_EXTENSIONS = setOf("torrent")
    private val TORRENT_MIMES = setOf("application/x-bittorrent")

    private val APP_EXTENSIONS = setOf(
        "apk", "apks", "xapk", "aab",
        "exe", "msi", "dmg", "pkg",
        "deb", "rpm", "appimage", "ipa"
    )
    private val APP_MIMES = setOf(
        "application/vnd.android.package-archive",
        "application/x-msdownload",
        "application/x-msi",
        "application/x-apple-diskimage",
        "application/x-debian-package",
        "application/x-redhat-package-manager"
    )

    private val ARCHIVE_EXTENSIONS = setOf(
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst",
        "tgz", "tbz2", "txz", "iso", "cab", "lz", "lzma"
    )
    private val ARCHIVE_MIMES = setOf(
        "application/zip",
        "application/x-zip-compressed",
        "application/x-rar-compressed",
        "application/vnd.rar",
        "application/x-7z-compressed",
        "application/x-tar",
        "application/gzip",
        "application/x-gzip",
        "application/x-bzip2",
        "application/x-xz",
        "application/zstd",
        "application/x-iso9660-image",
        "application/x-compressed"
    )

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm",
        "m4v", "3gp", "3g2", "mpg", "mpeg", "ts", "m2ts",
        "mts", "vob", "ogv", "divx"
    )

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "aac", "flac", "wav", "ogg", "oga", "opus",
        "m4a", "wma", "alac", "aiff", "mid", "midi", "amr",
        "ac3", "dts"
    )

    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg",
        "ico", "tiff", "tif", "heic", "heif", "avif", "raw",
        "cr2", "nef", "psd"
    )

    private val DOCUMENT_EXTENSIONS = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "odt", "ods", "odp", "rtf", "txt", "md", "csv", "tsv",
        "epub", "mobi", "azw3", "djvu", "json", "xml", "html",
        "htm", "log", "tex"
    )
    private val DOCUMENT_MIMES = setOf(
        "application/pdf",
        "application/msword",
        "application/vnd.ms-excel",
        "application/vnd.ms-powerpoint",
        "application/rtf",
        "application/epub+zip",
        "application/json",
        "application/xml"
    )

    fun resolveCategoryFolder(displayName: String, mimeType: String): String {
        val ext = extractSimpleExtension(displayName)
        val mime = mimeType.substringBefore(';').trim().lowercase(Locale.ROOT)

        if (ext in TORRENT_EXTENSIONS || mime in TORRENT_MIMES) {
            return "Torrents"
        }

        if (ext in APP_EXTENSIONS || mime in APP_MIMES) {
            return "Apps"
        }

        if (ext in ARCHIVE_EXTENSIONS || mime in ARCHIVE_MIMES) {
            return "Archives"
        }

        if (mime.startsWith("video/")) {
            return "Video"
        }
        if (mime.startsWith("audio/")) {
            return "Audio"
        }
        if (mime.startsWith("image/")) {
            return "Images"
        }

        if (ext in VIDEO_EXTENSIONS) {
            return "Video"
        }
        if (ext in AUDIO_EXTENSIONS) {
            return "Audio"
        }
        if (ext in IMAGE_EXTENSIONS) {
            return "Images"
        }

        if (ext in DOCUMENT_EXTENSIONS ||
            mime in DOCUMENT_MIMES ||
            mime.startsWith("text/") ||
            mime.startsWith("application/vnd.openxmlformats-officedocument.") ||
            mime.startsWith("application/vnd.oasis.opendocument.")
        ) {
            return "Documents"
        }

        return "Other"
    }

    fun resolveUniqueName(desiredName: String, existingNames: Set<String>): String {
        if (existingNames.isEmpty()) return desiredName

        val lowerExisting = existingNames.mapTo(HashSet(existingNames.size)) {
            it.lowercase(Locale.ROOT)
        }
        if (desiredName.lowercase(Locale.ROOT) !in lowerExisting) {
            return desiredName
        }

        val (stem, ext) = splitStemAndExtension(desiredName)
        var counter = 1
        while (true) {
            val candidate = "$stem ($counter)$ext"
            if (candidate.lowercase(Locale.ROOT) !in lowerExisting) {
                return candidate
            }
            counter++
        }
    }

    private fun extractSimpleExtension(displayName: String): String {
        val trimmed = displayName.trim()
        val lastDot = trimmed.lastIndexOf('.')
        if (lastDot <= 0 || lastDot == trimmed.length - 1) {
            return ""
        }
        return trimmed.substring(lastDot + 1).lowercase(Locale.ROOT)
    }

    private fun splitStemAndExtension(name: String): Pair<String, String> {
        val lastDot = name.lastIndexOf('.')
        if (lastDot <= 0 || lastDot == name.length - 1) {
            return name to ""
        }

        val secondLastDot = name.lastIndexOf('.', startIndex = lastDot - 1)
        if (secondLastDot > 0) {
            val middleSegment = name.substring(secondLastDot + 1, lastDot).lowercase(Locale.ROOT)
            if (middleSegment == "tar") {
                return name.substring(0, secondLastDot) to name.substring(secondLastDot)
            }
        }

        return name.substring(0, lastDot) to name.substring(lastDot)
    }
}
