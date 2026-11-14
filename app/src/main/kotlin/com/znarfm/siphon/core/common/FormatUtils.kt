package com.znarfm.siphon.core.common

import android.net.Uri
import okhttp3.Headers
import java.net.URI
import java.util.Locale

object FormatUtils {
    fun formatBytes(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        if (safe < 1024L) return "${safe}B"
        val kb = safe / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1fKB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.1fMB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2fGB", gb)
    }

    fun resolveUri(baseUri: String, relative: String): String {
        return try {
            URI.create(baseUri).resolve(relative).toString()
        } catch (_: Exception) {
            relative
        }
    }

    fun toOkHttpHeaders(headers: Map<String, String>): Headers = headers.toOkHttpHeaders()

    fun extractHostDisplay(url: String): String {
        if (url.isBlank()) return ""
        return runCatching {
            val host = URI.create(url.trim()).host
            host?.removePrefix("www.")?.ifBlank { null }
        }.getOrNull() ?: runCatching {
            Uri.parse(url).host?.removePrefix("www.")?.ifBlank { null }
        }.getOrNull() ?: ""
    }

    fun sanitizeFileName(raw: String, fallback: String = "download"): String {
        val cleaned = raw
            .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
            .trim()
            .trim('.')
        return cleaned.ifEmpty { fallback }
    }
}

fun Map<String, String>.toOkHttpHeaders(): Headers {
    val builder = Headers.Builder()
    forEach { (key, value) -> builder.add(key, value) }
    return builder.build()
}
