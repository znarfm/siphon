package com.znarfm.siphon.ui

import android.content.Intent
import android.net.Uri
import android.os.Build

object IntentLinkExtractor {
    private val magnetRegex = Regex("""magnet:\?[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val urlRegex = Regex("""https?://[^\s<>"'{}|\\^`]+""", RegexOption.IGNORE_CASE)

    fun extractLinkFromIntent(intent: Intent): String? {
        val data = intent.dataString
        if (!data.isNullOrBlank()) {
            val lower = data.lowercase()
            if (lower.startsWith("magnet:") ||
                lower.startsWith("http://") ||
                lower.startsWith("https://") ||
                lower.startsWith("content://") ||
                lower.startsWith("file://")
            ) {
                return data
            }
        }

        val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        if (streamUri != null) {
            val streamStr = streamUri.toString()
            if (streamStr.startsWith("content://", ignoreCase = true) ||
                streamStr.startsWith("file://", ignoreCase = true)
            ) {
                return streamStr
            }
        }

        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        extractLinkFromText(text)?.let { return it }

        val clip = intent.clipData
        if (clip != null && clip.itemCount > 0) {
            for (i in 0 until clip.itemCount) {
                val item = clip.getItemAt(i)
                val uriStr = item.uri?.toString()
                if (!uriStr.isNullOrBlank()) {
                    val lower = uriStr.lowercase()
                    if (lower.startsWith("magnet:") ||
                        lower.startsWith("http://") ||
                        lower.startsWith("https://") ||
                        lower.startsWith("content://") ||
                        lower.startsWith("file://")
                    ) {
                        return uriStr
                    }
                }
                extractLinkFromText(item.text?.toString())?.let { return it }
            }
        }

        extractLinkFromText(intent.getStringExtra(Intent.EXTRA_SUBJECT))?.let { return it }

        return null
    }

    fun extractLinkFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        magnetRegex.find(text)?.value?.trimEnd('.', ',', ')', ']', ';', '!')?.let { return it }
        return urlRegex.find(text)?.value?.trimEnd('.', ',', ')', ']', ';', '!')
    }

    fun extractUrlFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return urlRegex.find(text)?.value?.trimEnd('.', ',', ')', ']', ';', '!')
    }
}
