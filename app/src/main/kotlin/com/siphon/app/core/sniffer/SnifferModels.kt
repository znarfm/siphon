package com.siphon.app.core.sniffer

import com.siphon.app.data.db.DownloadType
import java.util.UUID

data class HlsVariantInfo(
    val url: String,
    val bandwidth: Long = 0,
    val resolution: String? = null,
    val codecs: String? = null,
    val estimatedBytes: Long = -1L
)

data class SniffedMedia(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val sourcePageUrl: String,
    val mediaType: DownloadType,
    val mimeType: String,
    val displayName: String,
    val totalBytes: Long, // -1 if unknown or master playlist
    val headers: Map<String, String>,
    val resolution: String? = null,
    val variants: List<HlsVariantInfo> = emptyList(),
    val detectedAt: Long = System.currentTimeMillis(),
    val durationSec: Double = 0.0
)
