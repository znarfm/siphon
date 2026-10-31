package com.siphon.app.core.sniffer

import android.net.Uri
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.ArrayList
import java.util.Locale

object SnifferUrlClassifier {
    val MEDIA_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "flv", "avi", "mov", "m4v",
        "m4a", "mp3", "aac", "opus", "flac", "wav", "ogg",
        "m3u8", "mpd"
    )

    val AUDIO_EXTENSIONS = setOf(
        "mp3", "m4a", "aac", "ogg", "opus", "wav", "flac"
    )

    val SUBTITLE_EXTENSIONS = setOf(
        "vtt", "srt", "ass", "ssa", "ttml", "dfxp", "sub"
    )

    val SEGMENT_EXTENSIONS = setOf("ts", "m4s")

    val BYTE_RANGE_PARAMS = setOf(
        "range", "bytes", "bytestart", "byteend", "start", "end", "offset", "part"
    )

    fun isCandidateMediaUrl(url: String, requestHeaders: Map<String, String>): Boolean {
        val secFetchDest = requestHeaders["Sec-Fetch-Dest"]?.lowercase(Locale.ROOT)
        if (secFetchDest == "video" || secFetchDest == "audio" || secFetchDest == "track") return true

        val accept = requestHeaders["Accept"]?.lowercase(Locale.ROOT)
        if (accept?.contains("video/") == true || accept?.contains("audio/") == true || accept?.contains("text/vtt") == true) return true

        val path = runCatching<String?> {
            url.toHttpUrlOrNull()?.encodedPath?.lowercase(Locale.ROOT)
        }.getOrNull() ?: url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        val ext = path.substringAfterLast('.', "")
        return ext in MEDIA_EXTENSIONS || ext in SEGMENT_EXTENSIONS || isSubtitleUrl(url)
    }

    fun isSubtitleUrl(url: String): Boolean {
        val path = runCatching<String?> {
            url.toHttpUrlOrNull()?.encodedPath?.lowercase(Locale.ROOT)
        }.getOrNull() ?: url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        val ext = path.substringAfterLast('.', "")
        if (ext in SUBTITLE_EXTENSIONS) return true
        val lowerUrl = url.lowercase(Locale.ROOT)
        return lowerUrl.contains("format=vtt") || lowerUrl.contains("fmt=vtt") || lowerUrl.contains("type=vtt") || lowerUrl.contains(".vtt?") || lowerUrl.contains(".srt?")
    }

    fun isSubtitleMime(mime: String): Boolean {
        val m = mime.lowercase(Locale.ROOT)
        return m.contains("text/vtt") || m.contains("application/x-subrip") || m.contains("text/srt") ||
            m.contains("ttml+xml") || m.contains("x-ssa") || m.contains("subtitle")
    }

    fun subtitleExtFromUrlOrMime(url: String, mime: String): String {
        val path = runCatching<String?> {
            url.toHttpUrlOrNull()?.encodedPath?.lowercase(Locale.ROOT)
        }.getOrNull() ?: url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        val ext = path.substringAfterLast('.', "")
        if (ext in SUBTITLE_EXTENSIONS) return ext
        val m = mime.lowercase(Locale.ROOT)
        return when {
            m.contains("subrip") || m.contains("srt") -> "srt"
            m.contains("ttml") -> "ttml"
            m.contains("ssa") || m.contains("ass") -> "ass"
            else -> "vtt"
        }
    }

    fun subtitleMimeForExt(ext: String): String = when (ext.lowercase(Locale.ROOT)) {
        "srt" -> "application/x-subrip"
        "ttml", "dfxp" -> "application/ttml+xml"
        "ass", "ssa" -> "text/x-ssa"
        else -> "text/vtt"
    }

    fun inferLanguageFromUrl(url: String): String? {
        val parsed = url.toHttpUrlOrNull()
        if (parsed != null) {
            for (param in listOf("lang", "hl", "locale", "language", "lng")) {
                parsed.queryParameter(param)?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        val file = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
        val parts = file.split('.', '_', '-')
        for (i in (parts.size - 2) downTo 0) {
            val candidate = parts[i].lowercase(Locale.ROOT)
            if (candidate in SUBTITLE_EXTENSIONS || candidate in setOf("sub", "subs", "subtitle", "subtitles")) {
                continue
            }
            if (candidate.length in 2..3 && candidate.all { it.isLetter() }) {
                return candidate
            }
            break
        }
        return null
    }

    fun buildSubtitleDisplayName(url: String, label: String?, lang: String?, ext: String): String {
        val rawName = extractDisplayNameFromUrl(url, "subtitle.$ext").substringBeforeLast('.')
        val cleanLang = (lang ?: label)?.trim()?.replace(Regex("[^a-zA-Z0-9_-]"), "")?.takeIf { it.isNotBlank() }
        return if (cleanLang != null && !rawName.endsWith(".$cleanLang", ignoreCase = true)) {
            "$rawName.$cleanLang.$ext"
        } else {
            "$rawName.$ext"
        }
    }

    fun isSegmentUrl(url: String): Boolean {
        val path = runCatching<String?> {
            url.toHttpUrlOrNull()?.encodedPath?.lowercase(Locale.ROOT)
        }.getOrNull() ?: url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        val ext = path.substringAfterLast('.', "")
        if (ext in SEGMENT_EXTENSIONS) return true
        if (ext in SUBTITLE_EXTENSIONS && (path.contains("/seg") || path.contains("segment") || path.contains("chunk") || path.contains("part"))) {
            return true
        }
        return path.contains("/segment") || path.contains("/seg-")
    }

    fun isHlsUrl(url: String): Boolean {
        val path = runCatching<String?> {
            url.toHttpUrlOrNull()?.encodedPath?.lowercase(Locale.ROOT)
        }.getOrNull() ?: url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        return path.endsWith(".m3u8") || path.contains(".m3u8") || url.contains(".m3u8")
    }

    fun normalizeByteRangeUrl(url: String): String {
        val parsed = url.toHttpUrlOrNull() ?: return url
        val builder = parsed.newBuilder()
        val paramNames = ArrayList(parsed.queryParameterNames)
        for (name in paramNames) {
            if (name.lowercase(Locale.ROOT) in BYTE_RANGE_PARAMS) {
                builder.removeAllQueryParameters(name)
            }
        }
        return builder.fragment(null).build().toString()
    }

    fun isAudioUrl(url: String): Boolean {
        val path = runCatching<String?> {
            url.toHttpUrlOrNull()?.encodedPath?.lowercase(Locale.ROOT)
        }.getOrNull() ?: url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        val ext = path.substringAfterLast('.', "")
        return ext in AUDIO_EXTENSIONS
    }

    fun isAudioMime(mime: String): Boolean {
        val m = mime.lowercase(Locale.ROOT)
        return m.startsWith("audio/") || m == "application/ogg" || m.contains("audio")
    }

    fun isAudioHls(content: String, url: String): Boolean {
        val lower = content.lowercase(Locale.ROOT)
        val lowerUrl = url.lowercase(Locale.ROOT)
        if (lowerUrl.contains("audio") || lowerUrl.contains("podcast") || lowerUrl.contains("radio") || lowerUrl.contains("sound")) return true
        if (lower.contains("type=audio") && !lower.contains("type=video") && !lower.contains("resolution=")) return true
        if (lower.contains(".aac") || lower.contains(".mp3") || lower.contains(".m4a")) return true
        return false
    }

    fun extractDisplayNameFromUrl(url: String, fallback: String, isAudio: Boolean = false): String {
        val path = runCatching { Uri.parse(url).path }.getOrNull()
            ?: url.substringBefore('?').substringBefore('#')
        val rawName = path.substringAfterLast('/', "").trim()
        val name = if (rawName.isNotBlank()) rawName else fallback
        return if (name.endsWith(".m3u8", ignoreCase = true)) {
            val base = name.substringBeforeLast('.')
            if (isAudio) "$base.m4a" else "$base.mp4"
        } else {
            name
        }
    }
}
