package com.siphon.app.core.hls

import java.net.URI

object HlsPlaylistParser {

    fun isMasterPlaylist(content: String): Boolean {
        return content.contains("#EXT-X-STREAM-INF")
    }

    fun parseMasterPlaylist(baseUrl: String, content: String): ParsedMasterPlaylist {
        val variants = mutableListOf<ParsedHlsVariant>()
        val audioTracks = mutableListOf<ParsedHlsAudioTrack>()

        val lines = content.lines()
        var currentBandwidth: Long = 0
        var currentResolution: String? = null
        var currentCodecs: String? = null
        var currentAudioGroup: String? = null

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXT-X-MEDIA:")) {
                val type = extractAttribute(line, "TYPE")
                if (type.equals("AUDIO", ignoreCase = true)) {
                    val groupId = extractAttribute(line, "GROUP-ID") ?: "default"
                    val name = extractAttribute(line, "NAME") ?: "Audio"
                    val uri = extractAttribute(line, "URI")
                    val isDefault = extractAttribute(line, "DEFAULT")?.equals("YES", ignoreCase = true) == true

                    if (!uri.isNullOrBlank()) {
                        audioTracks.add(
                            ParsedHlsAudioTrack(
                                groupId = groupId,
                                name = name,
                                url = resolveUri(baseUrl, uri),
                                isDefault = isDefault
                            )
                        )
                    }
                }
            } else if (line.startsWith("#EXT-X-STREAM-INF:")) {
                currentBandwidth = extractAttribute(line, "BANDWIDTH")?.toLongOrNull() ?: 0L
                currentResolution = extractAttribute(line, "RESOLUTION")
                currentCodecs = extractAttribute(line, "CODECS")
                currentAudioGroup = extractAttribute(line, "AUDIO")
            } else if (!line.startsWith("#") && currentBandwidth > 0L) {
                variants.add(
                    ParsedHlsVariant(
                        url = resolveUri(baseUrl, line),
                        bandwidth = currentBandwidth,
                        resolution = currentResolution,
                        codecs = currentCodecs,
                        audioGroupId = currentAudioGroup
                    )
                )
                currentBandwidth = 0L
                currentResolution = null
                currentCodecs = null
                currentAudioGroup = null
            }
        }

        return ParsedMasterPlaylist(
            masterUrl = baseUrl,
            variants = variants.sortedByDescending { it.bandwidth },
            audioTracks = audioTracks
        )
    }

    fun parseMediaPlaylist(
        playlistUrl: String,
        content: String,
        trackIndex: Int = 0
    ): ParsedMediaPlaylist {
        val segments = mutableListOf<ParsedHlsSegment>()
        var targetDuration = 10.0
        var currentSeqNum = 0L

        var currentKeyMethod: String? = null
        var currentKeyUrl: String? = null
        var currentIvHex: String? = null

        var nextDuration = 0.0
        var chunkIndex = 0

        val lines = content.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXT-X-TARGETDURATION:") -> {
                    targetDuration = line.substringAfter(':').trim().toDoubleOrNull() ?: 10.0
                }
                line.startsWith("#EXT-X-MEDIA-SEQUENCE:") -> {
                    currentSeqNum = line.substringAfter(':').trim().toLongOrNull() ?: 0L
                }
                line.startsWith("#EXT-X-KEY:") -> {
                    val method = extractAttribute(line, "METHOD") ?: "NONE"
                    if (method.equals("NONE", ignoreCase = true)) {
                        currentKeyMethod = null
                        currentKeyUrl = null
                        currentIvHex = null
                    } else {
                        currentKeyMethod = method
                        val rawUri = extractAttribute(line, "URI")
                        currentKeyUrl = if (!rawUri.isNullOrBlank()) resolveUri(playlistUrl, rawUri) else null
                        currentIvHex = extractAttribute(line, "IV")
                    }
                }
                line.startsWith("#EXT-X-MAP:") -> {
                    val rawUri = extractAttribute(line, "URI")
                    if (!rawUri.isNullOrBlank()) {
                        val initUrl = resolveUri(playlistUrl, rawUri)
                        segments.add(
                            ParsedHlsSegment(
                                trackIndex = trackIndex,
                                chunkIndex = chunkIndex,
                                sequenceNumber = currentSeqNum,
                                url = initUrl,
                                durationSec = 0.0,
                                keyMethod = currentKeyMethod,
                                keyUrl = currentKeyUrl,
                                ivHex = currentIvHex
                            )
                        )
                        chunkIndex++
                    }
                }
                line.startsWith("#EXTINF:") -> {
                    val durStr = line.substringAfter(':').substringBefore(',').trim()
                    nextDuration = durStr.toDoubleOrNull() ?: 0.0
                }
                !line.startsWith("#") -> {
                    val segmentUrl = resolveUri(playlistUrl, line)
                    segments.add(
                        ParsedHlsSegment(
                            trackIndex = trackIndex,
                            chunkIndex = chunkIndex,
                            sequenceNumber = currentSeqNum,
                            url = segmentUrl,
                            durationSec = nextDuration,
                            keyMethod = currentKeyMethod,
                            keyUrl = currentKeyUrl,
                            ivHex = currentIvHex
                        )
                    )
                    chunkIndex++
                    currentSeqNum++
                    nextDuration = 0.0
                }
            }
        }

        return ParsedMediaPlaylist(
            playlistUrl = playlistUrl,
            trackIndex = trackIndex,
            targetDurationSec = targetDuration,
            segments = segments
        )
    }

    fun extractAttribute(line: String, attributeName: String): String? {
        val regex = Regex("""(?:^|[:,\\s])${Regex.escape(attributeName)}=(?:"([^"]*)"|([^,\s]+))""")
        val match = regex.find(line) ?: return null
        return match.groups[1]?.value ?: match.groups[2]?.value?.trim()
    }

    fun resolveUri(baseUri: String, relativeOrAbsolute: String): String {
        return try {
            URI.create(baseUri).resolve(relativeOrAbsolute).toString()
        } catch (e: Exception) {
            relativeOrAbsolute
        }
    }
}
