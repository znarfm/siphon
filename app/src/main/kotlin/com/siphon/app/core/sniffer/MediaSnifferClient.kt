package com.siphon.app.core.sniffer

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.siphon.app.core.adblock.FastHostFilter
import com.siphon.app.core.hls.HlsPlaylistParser
import com.siphon.app.core.network.HeadersCodec
import com.siphon.app.core.network.MetadataProber
import com.siphon.app.core.network.NetworkClients
import com.siphon.app.core.storage.DownloadStorageProvider
import com.siphon.app.data.db.DownloadDao
import com.siphon.app.data.db.DownloadEntity
import com.siphon.app.data.db.DownloadType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class MediaSnifferClient(
    val hostFilter: FastHostFilter,
    val metadataProber: MetadataProber = MetadataProber(),
    private val dao: DownloadDao? = null,
    private val storage: DownloadStorageProvider? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : WebViewClient() {

    private val _detectedMedia = MutableStateFlow<List<SniffedMedia>>(emptyList())
    val detectedMedia = _detectedMedia.asStateFlow()

    private val _rebindTarget = MutableStateFlow<DownloadEntity?>(null)
    val rebindTarget = _rebindTarget.asStateFlow()

    private val _rebindSuccessEvent = MutableSharedFlow<Long>()
    val rebindSuccessEvent = _rebindSuccessEvent.asSharedFlow()

    @Volatile
    var currentPageUrl: String = ""
        private set

    var onUrlChanged: ((String) -> Unit)? = null
    var onNavigationStateChanged: ((canGoBack: Boolean, canGoForward: Boolean) -> Unit)? = null
    var onWebViewPageStarted: ((WebView, String) -> Unit)? = null
    var onWebViewPageFinished: ((WebView, String) -> Unit)? = null

    // LRU cache for candidate URLs normalized by stripping byte-range query params
    private val lruDedup = Collections.synchronizedMap(
        object : LinkedHashMap<String, Boolean>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
                return size > 500
            }
        }
    )

    // Manifests captured per page URL to suppress .ts/.m4s segment probing
    private val pageManifests = ConcurrentHashMap<String, MutableSet<String>>()

    fun setRebindTarget(download: DownloadEntity?) {
        _rebindTarget.value = download
    }

    fun clearDetectedMedia() {
        _detectedMedia.value = emptyList()
        lruDedup.clear()
        pageManifests.clear()
    }

    fun inspectCandidate(
        url: String,
        requestHeaders: Map<String, String> = emptyMap(),
        hintLang: String? = null,
        hintLabel: String? = null,
        forceInspect: Boolean = false
    ) {
        inspectCandidateAsync(url, requestHeaders, hintLang, hintLabel, forceInspect)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (!url.isNullOrBlank()) {
            currentPageUrl = url
            pageManifests.putIfAbsent(url, Collections.newSetFromMap(ConcurrentHashMap()))
            onUrlChanged?.invoke(url)
            onNavigationStateChanged?.invoke(view?.canGoBack() == true, view?.canGoForward() == true)
            if (view != null) onWebViewPageStarted?.invoke(view, url)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (!url.isNullOrBlank()) {
            currentPageUrl = url
            onUrlChanged?.invoke(url)
            onNavigationStateChanged?.invoke(view?.canGoBack() == true, view?.canGoForward() == true)
            if (view != null) onWebViewPageFinished?.invoke(view, url)
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        if (hostFilter.shouldBlockUrl(url)) {
            return true // Drop ad redirects / popup chains
        }
        return false
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val req = request ?: return null
        val uri = req.url ?: return null
        val urlStr = uri.toString()

        // 1. Drop ad network subresources immediately
        if (hostFilter.shouldBlockUrl(urlStr)) {
            return hostFilter.createEmptyResponse()
        }

        // 2. Passive sniffer: strictly return null for all non-ad requests so Chromium loads normally
        inspectCandidateAsync(urlStr, req.requestHeaders)
        return null
    }

    private fun inspectCandidateAsync(
        url: String,
        requestHeaders: Map<String, String>,
        hintLang: String? = null,
        hintLabel: String? = null,
        forceInspect: Boolean = false
    ) {
        if (!forceInspect && !isCandidateMediaUrl(url, requestHeaders)) return

        // 3. Skip segment probing (.ts / .m4s) if a parent manifest is already captured for this page
        if (!forceInspect && isSegmentUrl(url)) {
            val manifests = pageManifests[currentPageUrl]
            if (!manifests.isNullOrEmpty()) {
                return
            }
        }

        // 4. Dedup candidate URLs normalized by stripping byte-range query params
        val dedupKey = normalizeByteRangeUrl(url)
        val alreadySeen = synchronized(lruDedup) {
            if (lruDedup.containsKey(dedupKey)) {
                true
            } else {
                lruDedup[dedupKey] = true
                false
            }
        }
        if (alreadySeen) return

        // 5. Capture cookies, Referer, and User-Agent
        val cookies = runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()
        val headersBuilder = mutableMapOf<String, String>()
        if (!cookies.isNullOrBlank()) {
            headersBuilder["Cookie"] = cookies
        }
        requestHeaders["Referer"]?.let { headersBuilder["Referer"] = it }
        requestHeaders["User-Agent"]?.let { headersBuilder["User-Agent"] = it }
        requestHeaders["Origin"]?.let { headersBuilder["Origin"] = it }

        scope.launch(Dispatchers.IO) {
            if (isHlsUrl(url)) {
                processHlsCandidate(url, headersBuilder, hintLang, hintLabel)
            } else {
                processProgressiveCandidate(url, headersBuilder, hintLang, hintLabel, forceInspect)
            }
        }
    }

    private suspend fun processHlsCandidate(
        url: String,
        headersMap: Map<String, String>,
        hintLang: String? = null,
        hintLabel: String? = null
    ) {
        val manifests = pageManifests.getOrPut(currentPageUrl) {
            Collections.newSetFromMap(ConcurrentHashMap())
        }
        manifests.add(url)

        val okHeadersBuilder = Headers.Builder()
        headersMap.forEach { (k, v) -> okHeadersBuilder.add(k, v) }
        val okHeaders = okHeadersBuilder.build()

        val req = Request.Builder()
            .url(url)
            .headers(okHeaders)
            .get()
            .build()

        val bodyText = runCatching {
            NetworkClients.defaultClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        }.getOrNull() ?: return

        val parsedMaster = if (HlsPlaylistParser.isMasterPlaylist(bodyText)) {
            HlsPlaylistParser.parseMasterPlaylist(url, bodyText)
        } else null

        if (parsedMaster != null) {
            parsedMaster.subtitleTracks.forEach { sub ->
                val lang = sub.language ?: sub.name
                val subDisplay = buildSubtitleDisplayName(sub.url, sub.name, sub.language, "vtt")
                val subMedia = SniffedMedia(
                    url = sub.url,
                    sourcePageUrl = currentPageUrl,
                    mediaType = DownloadType.HLS,
                    mimeType = "text/vtt",
                    displayName = subDisplay,
                    totalBytes = -1L,
                    headers = headersMap,
                    isSubtitle = true,
                    language = lang
                )
                _detectedMedia.update { list ->
                    if (list.none { it.url == subMedia.url }) list + subMedia else list
                }
            }
        }

        val rawVariants = parsedMaster?.variants?.map {
            HlsVariantInfo(it.url, it.bandwidth, it.resolution, it.codecs)
        } ?: emptyList()

        if (parsedMaster == null) {
            val parsedMedia = HlsPlaylistParser.parseMediaPlaylist(url, bodyText)
            val isSubPlaylist = parsedMedia.segments.isNotEmpty() && parsedMedia.segments.all {
                isSubtitleUrl(it.url)
            }
            if (isSubPlaylist) {
                val lang = hintLang ?: hintLabel ?: inferLanguageFromUrl(url)
                val subDisplay = buildSubtitleDisplayName(url, hintLabel, lang, "vtt")
                val subMedia = SniffedMedia(
                    url = url,
                    sourcePageUrl = currentPageUrl,
                    mediaType = DownloadType.HLS,
                    mimeType = "text/vtt",
                    displayName = subDisplay,
                    totalBytes = -1L,
                    headers = headersMap,
                    durationSec = parsedMedia.segments.sumOf { it.durationSec },
                    isSubtitle = true,
                    language = lang
                )
                _detectedMedia.update { list ->
                    if (list.none { it.url == subMedia.url }) list + subMedia else list
                }
                return
            }
        }

        val isAudioStream = isAudioHls(bodyText, url) ||
            (rawVariants.isNotEmpty() && rawVariants.all { it.resolution == null && it.codecs?.startsWith("mp4a") == true })
        val displayName = extractDisplayNameFromUrl(url, if (isAudioStream) "audio.m4a" else "stream.m3u8", isAudio = isAudioStream)

        var durationSec = 0.0
        val isMaster = rawVariants.isNotEmpty()
        var sampledBitrate: Long = 0L

        val targetPlaylist = if (isMaster) {
            val probeVariantUrl = rawVariants.firstOrNull()?.url
            if (!probeVariantUrl.isNullOrBlank()) {
                val variantReq = Request.Builder().url(probeVariantUrl).headers(okHeaders).get().build()
                runCatching {
                    NetworkClients.defaultClient.newCall(variantReq).execute().use { resp ->
                        if (resp.isSuccessful) Pair(probeVariantUrl, resp.body?.string().orEmpty()) else null
                    }
                }.getOrNull()
            } else null
        } else {
            Pair(url, bodyText)
        }

        if (targetPlaylist != null) {
            val parsedMedia = HlsPlaylistParser.parseMediaPlaylist(targetPlaylist.first, targetPlaylist.second)
            durationSec = parsedMedia.segments.sumOf { it.durationSec }
            val firstSeg = parsedMedia.segments.firstOrNull { it.durationSec > 0.0 }
            if (firstSeg != null) {
                val segReq = Request.Builder()
                    .url(firstSeg.url)
                    .headers(okHeaders)
                    .header("Range", "bytes=0-0")
                    .get()
                    .build()
                val sampledBytes = runCatching {
                    NetworkClients.defaultClient.newCall(segReq).execute().use { resp ->
                        val contentRange = resp.header("Content-Range")
                        val rangeTotal = contentRange?.substringAfterLast('/')?.trim()?.toLongOrNull()
                        rangeTotal ?: resp.header("Content-Length")?.toLongOrNull()
                    }
                }.getOrNull()
                if (sampledBytes != null && sampledBytes > 1000L) {
                    val rate = (sampledBytes * 8.0 / firstSeg.durationSec).toLong()
                    if (rate in 50_000L..50_000_000L) {
                        sampledBitrate = rate
                    }
                }
            }
        }

        val enrichedVariants = rawVariants.mapIndexed { idx, variant ->
            val estBytes = if (durationSec > 0.0) {
                val bw = if (idx == 0 && sampledBitrate > 0L) {
                    sampledBitrate
                } else if (variant.bandwidth > 0) {
                    val firstBw = rawVariants.firstOrNull()?.bandwidth ?: 0L
                    if (sampledBitrate > 0L && firstBw > 0L) {
                        (sampledBitrate.toDouble() * variant.bandwidth / firstBw).toLong()
                    } else {
                        variant.bandwidth
                    }
                } else {
                    if (sampledBitrate > 0L) sampledBitrate else 1_500_000L
                }
                (bw * durationSec / 8.0).toLong()
            } else -1L
            variant.copy(estimatedBytes = estBytes)
        }

        val estimatedTotalBytes = if (enrichedVariants.isNotEmpty()) {
            enrichedVariants.firstOrNull()?.estimatedBytes ?: -1L
        } else if (durationSec > 0.0) {
            val bw = if (sampledBitrate > 0L) sampledBitrate else 1_500_000L
            (bw * durationSec / 8.0).toLong()
        } else {
            -1L
        }

        val media = SniffedMedia(
            url = url,
            sourcePageUrl = currentPageUrl,
            mediaType = DownloadType.HLS,
            mimeType = if (isAudioStream) "audio/mp4" else "application/vnd.apple.mpegurl",
            displayName = displayName,
            totalBytes = estimatedTotalBytes,
            headers = headersMap,
            variants = enrichedVariants,
            durationSec = durationSec,
            isAudio = isAudioStream
        )

        _detectedMedia.update { list ->
            if (list.none { it.url == media.url }) list + media else list
        }
    }

    private suspend fun processProgressiveCandidate(
        url: String,
        headersMap: Map<String, String>,
        hintLang: String? = null,
        hintLabel: String? = null,
        forceInspect: Boolean = false
    ) {
        val okHeadersBuilder = Headers.Builder()
        headersMap.forEach { (k, v) -> okHeadersBuilder.add(k, v) }
        val okHeaders = okHeadersBuilder.build()

        val probeResult = runCatching {
            metadataProber.probe(url, okHeaders)
        }.getOrNull() ?: return

        val mime = probeResult.mimeType.lowercase(Locale.ROOT)
        if (mime.contains("mpegurl")) {
            processHlsCandidate(url, headersMap, hintLang, hintLabel)
            return
        }

        val isSub = isSubtitleUrl(probeResult.finalUrl) || isSubtitleUrl(url) || isSubtitleMime(mime)
        val isAudio = isAudioUrl(probeResult.finalUrl) || isAudioUrl(url) || isAudioMime(mime)
        val isMediaMime = mime.startsWith("video/") || mime.startsWith("audio/") ||
            mime == "application/octet-stream" || isSub || isAudio || forceInspect

        if (probeResult.totalBytes <= 0 && !isMediaMime) {
            return
        }

        val ext = subtitleExtFromUrlOrMime(probeResult.finalUrl, mime)
        val lang = if (isSub) (hintLang ?: hintLabel ?: inferLanguageFromUrl(probeResult.finalUrl)) else null
        val displayName = if (isSub) {
            probeResult.suggestedFilename ?: buildSubtitleDisplayName(probeResult.finalUrl, hintLabel, lang, ext)
        } else {
            probeResult.suggestedFilename ?: extractDisplayNameFromUrl(url, if (isAudio) "audio.mp3" else "media", isAudio = isAudio)
        }

        val resolvedMime = if (isSub && (mime == "application/octet-stream" || mime.startsWith("text/plain"))) {
            subtitleMimeForExt(ext)
        } else {
            probeResult.mimeType
        }

        val media = SniffedMedia(
            url = probeResult.finalUrl,
            sourcePageUrl = currentPageUrl,
            mediaType = DownloadType.HTTP,
            mimeType = resolvedMime,
            displayName = displayName,
            totalBytes = probeResult.totalBytes,
            headers = headersMap,
            isSubtitle = isSub,
            isAudio = isAudio,
            language = lang
        )

        // Check explicit "Update Link" rebind flow if active
        val target = _rebindTarget.value
        if (target != null && dao != null && storage != null) {
            attemptRebind(media, target, probeResult.strongEtag)
        }

        _detectedMedia.update { list ->
            if (list.none { it.url == media.url }) list + media else list
        }
    }

    private suspend fun attemptRebind(
        candidate: SniffedMedia,
        target: DownloadEntity,
        candidateEtag: String?
    ) {
        val targetUriStr = target.destinationUri ?: return
        val targetUri = Uri.parse(targetUriStr)

        // Precondition: totalBytes must match target
        if (candidate.totalBytes != target.totalBytes || target.totalBytes <= 0L) {
            return
        }

        runCatching {
            storage?.openReadSession(targetUri)?.use { session ->
                val localChannel = session.channel
                val okHeadersBuilder = Headers.Builder()
                candidate.headers.forEach { (k, v) -> okHeadersBuilder.add(k, v) }

                metadataProber.verifyRefreshedUrl(
                    newUrl = candidate.url,
                    headers = okHeadersBuilder.build(),
                    expectedTotalBytes = target.totalBytes,
                    localChannel = localChannel,
                    sampleSize = 4096
                )

                // Overlap verification verified 4KB byte match: commit rebind to Room
                dao?.rebindExpiredLink(
                    id = target.id,
                    newUrl = candidate.url,
                    newEtag = candidateEtag,
                    newHeadersJson = HeadersCodec.encode(candidate.headers)
                )

                _rebindTarget.value = null
                _rebindSuccessEvent.emit(target.id)
            }
        }
    }

    companion object {
        private val MEDIA_EXTENSIONS = setOf(
            "mp4", "mkv", "webm", "flv", "avi", "mov", "m4v",
            "m4a", "mp3", "aac", "opus", "flac", "wav", "ogg",
            "m3u8", "mpd"
        )

        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "m4a", "aac", "ogg", "opus", "wav", "flac"
        )

        private val SUBTITLE_EXTENSIONS = setOf(
            "vtt", "srt", "ass", "ssa", "ttml", "dfxp"
        )

        private val SEGMENT_EXTENSIONS = setOf("ts", "m4s")

        private val BYTE_RANGE_PARAMS = setOf(
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
            if (parts.size >= 2) {
                val candidate = parts[parts.size - 2].lowercase(Locale.ROOT)
                if (candidate.length in 2..3 && candidate.all { it.isLetter() } && candidate !in SUBTITLE_EXTENSIONS) {
                    return candidate
                }
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
}

class SiphonSnifferBridge(
    private val client: MediaSnifferClient
) {
    @android.webkit.JavascriptInterface
    fun postCandidate(url: String?, headersJson: String?, lang: String?, label: String?) {
        if (url.isNullOrBlank()) return
        val headers = if (!headersJson.isNullOrBlank()) {
            runCatching {
                val map = mutableMapOf<String, String>()
                val json = org.json.JSONObject(headersJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = json.optString(k)
                }
                map
            }.getOrDefault(emptyMap())
        } else emptyMap()
        client.inspectCandidate(url, headers, lang, label)
    }
}

object SnifferJsInjector {
    fun buildUserscript(domain: String?): String {
        val domainHook = when {
            domain?.contains("soundcloud.com") == true -> """
                try {
                    if (window.__sc_hydration) {
                        window.__sc_hydration.forEach(function(item) {
                            if (item.data && item.data.media && item.data.media.transcodings) {
                                item.data.media.transcodings.forEach(function(t) {
                                    if (t.url) notify(t.url);
                                });
                            }
                        });
                    }
                } catch(e) {}
            """.trimIndent()
            domain?.contains("twitch.tv") == true -> """
                try {
                    document.querySelectorAll('[data-a-target="video-player"]').forEach(function(el) {
                        el.style.display = 'block';
                    });
                } catch(e) {}
            """.trimIndent()
            else -> """
                try {
                    var selectors = ['.play-button', '.vjs-big-play-button', '.jw-display-icon-display', '[class*="play-btn"]', '.video-overlay'];
                    selectors.forEach(function(sel) {
                        document.querySelectorAll(sel).forEach(function(btn) {
                            if (btn && typeof btn.click === 'function' && !btn.__siphon_clicked__) {
                                btn.__siphon_clicked__ = true;
                                btn.click();
                            }
                        });
                    });
                } catch(e) {}
            """.trimIndent()
        }

        return """
            (function() {
                if (window.__siphon_injected__) return;
                window.__siphon_injected__ = true;

                try {
                    window.canRunAds = true;
                    window.isAdBlockActive = false;
                    window.adBlockDetected = false;
                    if (!window.adsbygoogle) {
                        window.adsbygoogle = { loaded: true, push: function() {} };
                    }
                    var origGetComputedStyle = window.getComputedStyle;
                    window.getComputedStyle = function(el, pseudo) {
                        var style = origGetComputedStyle.apply(this, arguments);
                        if (el && ((el.id && el.id.indexOf('ad') !== -1) || (el.className && typeof el.className === 'string' && el.className.indexOf('ad') !== -1))) {
                            return new Proxy(style, {
                                get: function(target, prop) {
                                    if (prop === 'display') return 'block';
                                    if (prop === 'visibility') return 'visible';
                                    if (prop === 'height') return target[prop] === '0px' ? '100px' : target[prop];
                                    return target[prop];
                                }
                            });
                        }
                        return style;
                    };
                } catch(e) {}

                function notify(url, headers, lang, label) {
                    if (!url || typeof url !== 'string') return;
                    if (url.indexOf('blob:') === 0 || url.indexOf('javascript:') === 0 || url.indexOf('data:') === 0) return;
                    try {
                        if (window.__siphon_sniffer__ && window.__siphon_sniffer__.postCandidate) {
                            window.__siphon_sniffer__.postCandidate(url, headers ? JSON.stringify(headers) : null, lang || null, label || null);
                        }
                    } catch(e) {}
                }

                var MEDIA_REGEX = /\.(m3u8|mpd|mp4|webm|mkv|m4v|mp3|m4a|aac|ogg|opus|wav|flac|vtt|srt)(\?[^"'\s]*)?$/i;
                var RPC_STREAM_REGEX = /https?:\/\/[^\s"'<>]+\.(?:m3u8|mpd|mp4|webm|m4v|mp3|m4a|aac|ogg|opus|flac|wav)[\w\-.~:/?#\[\]@!$&'()*+,;=]*/gi;

                function checkUrl(url) {
                    if (typeof url === 'string' && MEDIA_REGEX.test(url)) {
                        notify(url);
                    }
                }

                if (window.WebSocket) {
                    var OrigWebSocket = window.WebSocket;
                    window.WebSocket = function(url, protocols) {
                        var ws = protocols ? new OrigWebSocket(url, protocols) : new OrigWebSocket(url);
                        try {
                            ws.addEventListener('message', function(event) {
                                if (typeof event.data === 'string') {
                                    var matches = event.data.match(RPC_STREAM_REGEX);
                                    if (matches) {
                                        for (var i = 0; i < matches.length; i++) {
                                            notify(matches[i]);
                                        }
                                    }
                                }
                            });
                        } catch(e) {}
                        return ws;
                    };
                    window.WebSocket.prototype = OrigWebSocket.prototype;
                }

                if (window.fetch) {
                    var origFetch = window.fetch;
                    window.fetch = function(resource, init) {
                        var url = typeof resource === 'string' ? resource : (resource && resource.url ? resource.url : '');
                        if (url) checkUrl(url);
                        return origFetch.apply(this, arguments).then(function(response) {
                            try {
                                var ct = response.headers.get('content-type') || '';
                                if (ct.indexOf('video/') !== -1 || ct.indexOf('audio/') !== -1 || ct.indexOf('mpegurl') !== -1 || ct.indexOf('dash+xml') !== -1) {
                                    notify(response.url || url);
                                }
                            } catch(e) {}
                            return response;
                        });
                    };
                }

                function sweepMediaElements() {
                    document.querySelectorAll('video, audio').forEach(function(el) {
                        if (el.style.display === 'none') el.style.display = 'block';
                        if (el.style.visibility === 'hidden') el.style.visibility = 'visible';
                        if (el.hidden) el.hidden = false;

                        var src = el.src || el.currentSrc;
                        if (src) checkUrl(src);

                        el.querySelectorAll('source').forEach(function(s) {
                            if (s.src) checkUrl(s.src);
                        });
                        el.querySelectorAll('track').forEach(function(t) {
                            if (t.src) notify(t.src, null, t.srclang, t.label);
                        });
                    });
                    $domainHook
                }

                sweepMediaElements();
                if (document.readyState !== 'complete') {
                    window.addEventListener('DOMContentLoaded', sweepMediaElements);
                    window.addEventListener('load', sweepMediaElements);
                }
                setInterval(sweepMediaElements, 2500);
            })();
        """.trimIndent()
    }
}
