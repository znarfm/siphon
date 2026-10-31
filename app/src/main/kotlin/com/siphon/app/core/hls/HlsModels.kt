package com.siphon.app.core.hls

data class ParsedHlsVariant(
    val url: String,
    val bandwidth: Long,
    val resolution: String? = null,
    val codecs: String? = null,
    val audioGroupId: String? = null
)

data class ParsedHlsAudioTrack(
    val groupId: String,
    val name: String,
    val url: String,
    val isDefault: Boolean = false
)

data class ParsedHlsSubtitleTrack(
    val name: String,
    val language: String? = null,
    val url: String
)

data class ParsedHlsSegment(
    val trackIndex: Int,
    val chunkIndex: Int,
    val sequenceNumber: Long,
    val url: String,
    val durationSec: Double,
    val keyMethod: String? = null,
    val keyUrl: String? = null,
    val ivHex: String? = null
)

data class ParsedMasterPlaylist(
    val masterUrl: String,
    val variants: List<ParsedHlsVariant>,
    val audioTracks: List<ParsedHlsAudioTrack>,
    val subtitleTracks: List<ParsedHlsSubtitleTrack> = emptyList()
)

data class ParsedMediaPlaylist(
    val playlistUrl: String,
    val trackIndex: Int,
    val targetDurationSec: Double,
    val segments: List<ParsedHlsSegment>
)
