package com.znarfm.siphon.core.dash

import com.znarfm.siphon.core.common.FormatUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.Locale

data class DashSegment(
    val url: String,
    val isInit: Boolean = false
)

data class DashTrack(
    val id: String,
    val contentType: String, // "video", "audio", "text"
    val mimeType: String,
    val bandwidth: Long,
    val width: Int?,
    val height: Int?,
    val codecs: String?,
    val language: String?,
    val isSingleFile: Boolean,
    val singleFileUrl: String?,
    val segments: List<DashSegment>,
    val isDrmProtected: Boolean = false
) {
    val resolution: String?
        get() = if (width != null && height != null && width > 0 && height > 0) "${width}x${height}" else null
}

data class ParsedDashManifest(
    val mpdUrl: String,
    val durationSec: Double,
    val videoTracks: List<DashTrack>,
    val audioTracks: List<DashTrack>,
    val subtitleTracks: List<DashTrack>,
    val hasDrm: Boolean
)

object DashManifestParser {

    private data class SegmentTimelineEntry(
        val t: Long?,
        val d: Long,
        val r: Int
    )

    private data class SegmentTemplateSpec(
        val initialization: String? = null,
        val media: String? = null,
        val timescale: Long = 1L,
        val duration: Long = 0L,
        val startNumber: Long = 1L,
        val timeline: List<SegmentTimelineEntry> = emptyList()
    )

    private data class SegmentListSpec(
        val initializationUrl: String? = null,
        val segmentUrls: List<String> = emptyList()
    )

    fun isDashManifest(content: String): Boolean {
        return content.contains("<MPD", ignoreCase = true)
    }

    fun parse(mpdUrl: String, xmlContent: String): ParsedDashManifest {
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlContent))

        var durationSec = 0.0
        var mpdBaseUrl = mpdUrl
        var periodBaseUrl = mpdUrl
        var adaptBaseUrl = mpdUrl

        var inPeriod = false
        var inAdaptationSet = false
        var inRepresentation = false
        var inSegmentTemplate = false
        var inSegmentList = false

        var adaptContentType = ""
        var adaptMimeType = ""
        var adaptCodecs: String? = null
        var adaptLang: String? = null
        var adaptWidth: Int? = null
        var adaptHeight: Int? = null
        var adaptDrm = false
        var adaptTemplate: SegmentTemplateSpec? = null
        var adaptList: SegmentListSpec? = null

        var repId = ""
        var repBandwidth = 0L
        var repWidth: Int? = null
        var repHeight: Int? = null
        var repCodecs: String? = null
        var repMimeType: String? = null
        var repBaseUrl: String? = null
        var repDrm = false
        var repTemplate: SegmentTemplateSpec? = null
        var repList: SegmentListSpec? = null

        var currentTemplate: SegmentTemplateSpec? = null
        var currentTimeline = mutableListOf<SegmentTimelineEntry>()
        var currentListInit: String? = null
        var currentListUrls = mutableListOf<String>()

        val videoTracks = mutableListOf<DashTrack>()
        val audioTracks = mutableListOf<DashTrack>()
        val subtitleTracks = mutableListOf<DashTrack>()
        var anyDrm = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name
                    when {
                        tag.equals("MPD", ignoreCase = true) -> {
                            val durAttr = parser.getAttributeValue(null, "mediaPresentationDuration")
                            durationSec = parseIsoDuration(durAttr)
                        }
                        tag.equals("Period", ignoreCase = true) -> {
                            inPeriod = true
                            periodBaseUrl = mpdBaseUrl
                        }
                        tag.equals("AdaptationSet", ignoreCase = true) -> {
                            inAdaptationSet = true
                            adaptBaseUrl = periodBaseUrl
                            adaptContentType = parser.getAttributeValue(null, "contentType")?.lowercase(Locale.ROOT).orEmpty()
                            adaptMimeType = parser.getAttributeValue(null, "mimeType")?.lowercase(Locale.ROOT).orEmpty()
                            adaptCodecs = parser.getAttributeValue(null, "codecs")
                            adaptLang = parser.getAttributeValue(null, "lang")
                            adaptWidth = parser.getAttributeValue(null, "width")?.toIntOrNull()
                            adaptHeight = parser.getAttributeValue(null, "height")?.toIntOrNull()
                            adaptDrm = false
                            adaptTemplate = null
                            adaptList = null
                        }
                        tag.equals("Representation", ignoreCase = true) -> {
                            inRepresentation = true
                            repId = parser.getAttributeValue(null, "id") ?: "rep_${videoTracks.size + audioTracks.size}"
                            repBandwidth = parser.getAttributeValue(null, "bandwidth")?.toLongOrNull() ?: 0L
                            repWidth = parser.getAttributeValue(null, "width")?.toIntOrNull() ?: adaptWidth
                            repHeight = parser.getAttributeValue(null, "height")?.toIntOrNull() ?: adaptHeight
                            repCodecs = parser.getAttributeValue(null, "codecs") ?: adaptCodecs
                            repMimeType = parser.getAttributeValue(null, "mimeType")?.lowercase(Locale.ROOT) ?: adaptMimeType
                            repBaseUrl = adaptBaseUrl
                            repDrm = adaptDrm
                            repTemplate = null
                            repList = null
                        }
                        tag.equals("BaseURL", ignoreCase = true) -> {
                            val text = parser.nextText().trim()
                            if (text.isNotEmpty()) {
                                when {
                                    inRepresentation -> repBaseUrl = FormatUtils.resolveUri(repBaseUrl ?: adaptBaseUrl, text)
                                    inAdaptationSet -> adaptBaseUrl = FormatUtils.resolveUri(adaptBaseUrl, text)
                                    inPeriod -> periodBaseUrl = FormatUtils.resolveUri(periodBaseUrl, text)
                                    else -> mpdBaseUrl = FormatUtils.resolveUri(mpdBaseUrl, text)
                                }
                            }
                        }
                        tag.equals("ContentProtection", ignoreCase = true) -> {
                            val scheme = parser.getAttributeValue(null, "schemeIdUri").orEmpty().lowercase(Locale.ROOT)
                            if (scheme.isNotBlank() && !scheme.contains("mp4protection") && !scheme.contains("clearkey")) {
                                anyDrm = true
                                if (inRepresentation) repDrm = true else if (inAdaptationSet) adaptDrm = true
                            }
                        }
                        tag.equals("SegmentTemplate", ignoreCase = true) -> {
                            inSegmentTemplate = true
                            currentTimeline = mutableListOf()
                            currentTemplate = SegmentTemplateSpec(
                                initialization = parser.getAttributeValue(null, "initialization"),
                                media = parser.getAttributeValue(null, "media"),
                                timescale = parser.getAttributeValue(null, "timescale")?.toLongOrNull()?.takeIf { it > 0L } ?: 1L,
                                duration = parser.getAttributeValue(null, "duration")?.toLongOrNull() ?: 0L,
                                startNumber = parser.getAttributeValue(null, "startNumber")?.toLongOrNull() ?: 1L
                            )
                        }
                        tag.equals("S", ignoreCase = true) && inSegmentTemplate -> {
                            val t = parser.getAttributeValue(null, "t")?.toLongOrNull()
                            val d = parser.getAttributeValue(null, "d")?.toLongOrNull() ?: 0L
                            val r = parser.getAttributeValue(null, "r")?.toIntOrNull() ?: 0
                            if (d > 0L) {
                                currentTimeline.add(SegmentTimelineEntry(t, d, r))
                            }
                        }
                        tag.equals("SegmentList", ignoreCase = true) -> {
                            inSegmentList = true
                            currentListInit = null
                            currentListUrls = mutableListOf()
                        }
                        tag.equals("Initialization", ignoreCase = true) && inSegmentList -> {
                            currentListInit = parser.getAttributeValue(null, "sourceURL")
                        }
                        tag.equals("SegmentURL", ignoreCase = true) && inSegmentList -> {
                            parser.getAttributeValue(null, "media")?.let { currentListUrls.add(it) }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name
                    when {
                        tag.equals("SegmentTemplate", ignoreCase = true) -> {
                            val spec = currentTemplate?.copy(timeline = currentTimeline.toList())
                            if (inRepresentation) repTemplate = spec else if (inAdaptationSet) adaptTemplate = spec
                            inSegmentTemplate = false
                            currentTemplate = null
                        }
                        tag.equals("SegmentList", ignoreCase = true) -> {
                            val spec = SegmentListSpec(currentListInit, currentListUrls.toList())
                            if (inRepresentation) repList = spec else if (inAdaptationSet) adaptList = spec
                            inSegmentList = false
                        }
                        tag.equals("Representation", ignoreCase = true) -> {
                            val effMime = repMimeType?.takeIf { it.isNotBlank() } ?: adaptMimeType
                            val effType = when {
                                adaptContentType.isNotBlank() -> adaptContentType
                                effMime.startsWith("video/") -> "video"
                                effMime.startsWith("audio/") -> "audio"
                                effMime.startsWith("text/") || effMime.contains("vtt") || effMime.contains("ttml") -> "text"
                                else -> "video"
                            }
                            val base = repBaseUrl ?: adaptBaseUrl
                            val tmpl = repTemplate ?: adaptTemplate
                            val list = repList ?: adaptList

                            val segments = when {
                                tmpl != null -> expandSegmentTemplate(base, tmpl, repId, repBandwidth, durationSec)
                                list != null -> expandSegmentList(base, list)
                                else -> emptyList()
                            }

                            val isSingleFile = segments.isEmpty() && base != mpdUrl
                            val track = DashTrack(
                                id = repId,
                                contentType = effType,
                                mimeType = effMime.ifBlank { if (effType == "audio") "audio/mp4" else "video/mp4" },
                                bandwidth = repBandwidth,
                                width = repWidth,
                                height = repHeight,
                                codecs = repCodecs,
                                language = adaptLang,
                                isSingleFile = isSingleFile,
                                singleFileUrl = if (isSingleFile) base else null,
                                segments = segments,
                                isDrmProtected = repDrm
                            )

                            when (effType) {
                                "video" -> videoTracks.add(track)
                                "audio" -> audioTracks.add(track)
                                "text" -> subtitleTracks.add(track)
                            }
                            inRepresentation = false
                        }
                        tag.equals("AdaptationSet", ignoreCase = true) -> {
                            inAdaptationSet = false
                        }
                        tag.equals("Period", ignoreCase = true) -> {
                            inPeriod = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return ParsedDashManifest(
            mpdUrl = mpdUrl,
            durationSec = durationSec,
            videoTracks = videoTracks.sortedByDescending { it.bandwidth },
            audioTracks = audioTracks.sortedByDescending { it.bandwidth },
            subtitleTracks = subtitleTracks,
            hasDrm = anyDrm
        )
    }

    private fun expandSegmentTemplate(
        baseUrl: String,
        spec: SegmentTemplateSpec,
        repId: String,
        bandwidth: Long,
        durationSec: Double
    ): List<DashSegment> {
        val result = mutableListOf<DashSegment>()
        spec.initialization?.let { initTmpl ->
            val resolved = substituteVars(initTmpl, repId, spec.startNumber, 0L, bandwidth)
            result.add(DashSegment(url = FormatUtils.resolveUri(baseUrl, resolved), isInit = true))
        }
        val mediaTmpl = spec.media ?: return result

        if (spec.timeline.isNotEmpty()) {
            var currentTime = spec.timeline.first().t ?: 0L
            var currentNumber = spec.startNumber
            for (entry in spec.timeline) {
                if (entry.t != null) currentTime = entry.t
                val repeatCount = if (entry.r >= 0) entry.r else 0
                for (i in 0..repeatCount) {
                    val resolved = substituteVars(mediaTmpl, repId, currentNumber, currentTime, bandwidth)
                    result.add(DashSegment(url = FormatUtils.resolveUri(baseUrl, resolved)))
                    currentTime += entry.d
                    currentNumber++
                }
            }
        } else if (spec.duration > 0L && durationSec > 0.0) {
            val totalTicks = (durationSec * spec.timescale).toLong()
            val count = ((totalTicks + spec.duration - 1) / spec.duration).coerceIn(1L, 10_000L)
            for (i in 0 until count) {
                val num = spec.startNumber + i
                val time = i * spec.duration
                val resolved = substituteVars(mediaTmpl, repId, num, time, bandwidth)
                result.add(DashSegment(url = FormatUtils.resolveUri(baseUrl, resolved)))
            }
        }
        return result
    }

    private fun expandSegmentList(baseUrl: String, spec: SegmentListSpec): List<DashSegment> {
        val result = mutableListOf<DashSegment>()
        spec.initializationUrl?.let {
            result.add(DashSegment(url = FormatUtils.resolveUri(baseUrl, it), isInit = true))
        }
        for (seg in spec.segmentUrls) {
            result.add(DashSegment(url = FormatUtils.resolveUri(baseUrl, seg)))
        }
        return result
    }

    private fun substituteVars(
        template: String,
        repId: String,
        number: Long,
        time: Long,
        bandwidth: Long
    ): String {
        var out = template
            .replace("\$RepresentationID\$", repId)
            .replace("\$Bandwidth\$", bandwidth.toString())
            .replace("\$Number\$", number.toString())
            .replace("\$Time\$", time.toString())

        val formattedNumberRegex = Regex("\\\$Number%0(\\d+)d\\\$")
        out = formattedNumberRegex.replace(out) { m ->
            val width = m.groupValues[1].toIntOrNull() ?: 1
            number.toString().padStart(width, '0')
        }
        val formattedTimeRegex = Regex("\\\$Time%0(\\d+)d\\\$")
        out = formattedTimeRegex.replace(out) { m ->
            val width = m.groupValues[1].toIntOrNull() ?: 1
            time.toString().padStart(width, '0')
        }
        return out
    }

    fun parseIsoDuration(iso: String?): Double {
        if (iso.isNullOrBlank()) return 0.0
        val regex = Regex("""^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?)?$""")
        val m = regex.find(iso.trim()) ?: return 0.0
        val days = m.groupValues[1].toDoubleOrNull() ?: 0.0
        val hours = m.groupValues[2].toDoubleOrNull() ?: 0.0
        val mins = m.groupValues[3].toDoubleOrNull() ?: 0.0
        val secs = m.groupValues[4].toDoubleOrNull() ?: 0.0
        return days * 86400.0 + hours * 3600.0 + mins * 60.0 + secs
    }
}
