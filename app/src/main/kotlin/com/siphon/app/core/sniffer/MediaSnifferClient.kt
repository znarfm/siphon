package com.siphon.app.core.sniffer

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.siphon.app.core.adblock.FastHostFilter
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
import java.net.URI
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

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (!url.isNullOrBlank()) {
            currentPageUrl = url
            pageManifests.putIfAbsent(url, Collections.newSetFromMap(ConcurrentHashMap()))
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

    private fun inspectCandidateAsync(url: String, requestHeaders: Map<String, String>) {
        if (!isCandidateMediaUrl(url, requestHeaders)) return

        // 3. Skip segment probing (.ts / .m4s) if a parent manifest is already captured for this page
        if (isSegmentUrl(url)) {
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
                processHlsCandidate(url, headersBuilder)
            } else {
                processProgressiveCandidate(url, headersBuilder)
            }
        }
    }

    private suspend fun processHlsCandidate(url: String, headersMap: Map<String, String>) {
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

        val variants = parseHlsMasterVariants(url, bodyText)
        val displayName = extractDisplayNameFromUrl(url, "stream.m3u8")

        val media = SniffedMedia(
            url = url,
            sourcePageUrl = currentPageUrl,
            mediaType = DownloadType.HLS,
            mimeType = "application/vnd.apple.mpegurl",
            displayName = displayName,
            totalBytes = -1L,
            headers = headersMap,
            variants = variants
        )

        _detectedMedia.update { list ->
            if (list.none { it.url == media.url }) list + media else list
        }
    }

    private suspend fun processProgressiveCandidate(url: String, headersMap: Map<String, String>) {
        val okHeadersBuilder = Headers.Builder()
        headersMap.forEach { (k, v) -> okHeadersBuilder.add(k, v) }
        val okHeaders = okHeadersBuilder.build()

        val probeResult = runCatching {
            metadataProber.probe(url, okHeaders)
        }.getOrNull() ?: return

        val mime = probeResult.mimeType.lowercase(Locale.ROOT)
        if (mime.contains("mpegurl")) {
            processHlsCandidate(url, headersMap)
            return
        }

        val isMediaMime = mime.startsWith("video/") || mime.startsWith("audio/") ||
            mime == "application/octet-stream"

        if (probeResult.totalBytes <= 0 && !isMediaMime) {
            return
        }

        val displayName = probeResult.suggestedFilename
            ?: extractDisplayNameFromUrl(url, "media")

        val media = SniffedMedia(
            url = probeResult.finalUrl,
            sourcePageUrl = currentPageUrl,
            mediaType = DownloadType.HTTP,
            mimeType = probeResult.mimeType,
            displayName = displayName,
            totalBytes = probeResult.totalBytes,
            headers = headersMap
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

        private val SEGMENT_EXTENSIONS = setOf("ts", "m4s")

        private val BYTE_RANGE_PARAMS = setOf(
            "range", "bytes", "bytestart", "byteend", "start", "end", "offset", "part"
        )

        fun isCandidateMediaUrl(url: String, requestHeaders: Map<String, String>): Boolean {
            val secFetchDest = requestHeaders["Sec-Fetch-Dest"]?.lowercase(Locale.ROOT)
            if (secFetchDest == "video" || secFetchDest == "audio") return true

            val accept = requestHeaders["Accept"]?.lowercase(Locale.ROOT)
            if (accept?.contains("video/") == true || accept?.contains("audio/") == true) return true

            val path = runCatching<String?> {
                url.toHttpUrlOrNull()?.encodedPath?.lowercase(Locale.ROOT)
            }.getOrNull() ?: url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
            val ext = path.substringAfterLast('.', "")
            return ext in MEDIA_EXTENSIONS || ext in SEGMENT_EXTENSIONS
        }

        fun isSegmentUrl(url: String): Boolean {
            val path = runCatching<String?> {
                url.toHttpUrlOrNull()?.encodedPath?.lowercase(Locale.ROOT)
            }.getOrNull() ?: url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
            val ext = path.substringAfterLast('.', "")
            if (ext in SEGMENT_EXTENSIONS) return true
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

        fun parseHlsMasterVariants(baseUrl: String, manifestText: String): List<HlsVariantInfo> {
            val variants = mutableListOf<HlsVariantInfo>()
            if (!manifestText.contains("#EXT-X-STREAM-INF")) return variants

            val lines = manifestText.lines()
            var currentBandwidth: Long = 0
            var currentResolution: String? = null
            var currentCodecs: String? = null

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("#EXT-X-STREAM-INF:")) {
                    currentBandwidth = com.siphon.app.core.hls.HlsPlaylistParser.extractAttribute(trimmed, "BANDWIDTH")?.toLongOrNull() ?: 0L
                    currentResolution = com.siphon.app.core.hls.HlsPlaylistParser.extractAttribute(trimmed, "RESOLUTION")
                    currentCodecs = com.siphon.app.core.hls.HlsPlaylistParser.extractAttribute(trimmed, "CODECS")
                } else if (!trimmed.startsWith("#") && trimmed.isNotEmpty() && currentBandwidth > 0L) {
                    val resolvedUrl = resolveUrl(baseUrl, trimmed)
                    variants.add(
                        HlsVariantInfo(
                            url = resolvedUrl,
                            bandwidth = currentBandwidth,
                            resolution = currentResolution,
                            codecs = currentCodecs
                        )
                    )
                    currentBandwidth = 0L
                    currentResolution = null
                    currentCodecs = null
                }
            }
            return variants
        }

        fun resolveUrl(baseUrl: String, relativeOrAbsolute: String): String {
            return try {
                URI.create(baseUrl).resolve(relativeOrAbsolute).toString()
            } catch (e: Exception) {
                relativeOrAbsolute
            }
        }

        fun extractDisplayNameFromUrl(url: String, fallback: String): String {
            val path = runCatching { Uri.parse(url).path }.getOrNull()
                ?: url.substringBefore('?').substringBefore('#')
            val rawName = path.substringAfterLast('/', "").trim()
            val name = if (rawName.isNotBlank()) rawName else fallback
            return if (name.endsWith(".m3u8", ignoreCase = true)) {
                name.substringBeforeLast('.') + ".mp4"
            } else {
                name
            }
        }
    }
}
