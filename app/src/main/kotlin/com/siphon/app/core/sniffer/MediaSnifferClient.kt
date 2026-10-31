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

    fun updateCurrentPageUrl(url: String) {
        if (url.isNotBlank()) {
            currentPageUrl = url
        }
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
            if (hostFilter.isEnabled) {
                val css = ".adbox, .banner_ads, .adsbox, .textads, [class*='ad-banner'], [id*='ad-container'] { display: none !important; }"
                val js = "(function() { var el = document.getElementById('__siphon_adblock_style__'); if (!el) { el = document.createElement('style'); el.id = '__siphon_adblock_style__'; el.textContent = '$css'; (document.head || document.documentElement).appendChild(el); } })();"
                view?.evaluateJavascript(js, null)
            }
            if (view != null) onWebViewPageFinished?.invoke(view, url)
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        if (hostFilter.shouldBlockUri(uri)) {
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

        // 1. Drop ad network subresources immediately
        if (hostFilter.shouldBlockUri(uri)) {
            return hostFilter.createEmptyResponse()
        }

        // 2. Passive sniffer: strictly return null for all non-ad requests so Chromium loads normally
        inspectCandidateAsync(uri.toString(), req.requestHeaders)
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

    private fun findReferer(headersMap: Map<String, String>): String? {
        return headersMap["Referer"]
            ?: headersMap["referer"]
            ?: headersMap.entries.firstOrNull { it.key.equals("referer", ignoreCase = true) }?.value
    }

    private suspend fun processHlsCandidate(
        url: String,
        headersMap: Map<String, String>,
        hintLang: String? = null,
        hintLabel: String? = null
    ) {
        val sourcePage = findReferer(headersMap) ?: currentPageUrl
        val manifests = pageManifests.getOrPut(sourcePage) {
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
                    sourcePageUrl = sourcePage,
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
                    sourcePageUrl = sourcePage,
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
            sourcePageUrl = sourcePage,
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
        val sourcePage = findReferer(headersMap) ?: currentPageUrl
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
            sourcePageUrl = sourcePage,
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
        val MEDIA_EXTENSIONS get() = SnifferUrlClassifier.MEDIA_EXTENSIONS
        val AUDIO_EXTENSIONS get() = SnifferUrlClassifier.AUDIO_EXTENSIONS
        val SUBTITLE_EXTENSIONS get() = SnifferUrlClassifier.SUBTITLE_EXTENSIONS
        val SEGMENT_EXTENSIONS get() = SnifferUrlClassifier.SEGMENT_EXTENSIONS
        val BYTE_RANGE_PARAMS get() = SnifferUrlClassifier.BYTE_RANGE_PARAMS

        fun isCandidateMediaUrl(url: String, requestHeaders: Map<String, String>): Boolean =
            SnifferUrlClassifier.isCandidateMediaUrl(url, requestHeaders)

        fun isSubtitleUrl(url: String): Boolean =
            SnifferUrlClassifier.isSubtitleUrl(url)

        fun isSubtitleMime(mime: String): Boolean =
            SnifferUrlClassifier.isSubtitleMime(mime)

        fun subtitleExtFromUrlOrMime(url: String, mime: String): String =
            SnifferUrlClassifier.subtitleExtFromUrlOrMime(url, mime)

        fun subtitleMimeForExt(ext: String): String =
            SnifferUrlClassifier.subtitleMimeForExt(ext)

        fun inferLanguageFromUrl(url: String): String? =
            SnifferUrlClassifier.inferLanguageFromUrl(url)

        fun buildSubtitleDisplayName(url: String, label: String?, lang: String?, ext: String): String =
            SnifferUrlClassifier.buildSubtitleDisplayName(url, label, lang, ext)

        fun isSegmentUrl(url: String): Boolean =
            SnifferUrlClassifier.isSegmentUrl(url)

        fun isHlsUrl(url: String): Boolean =
            SnifferUrlClassifier.isHlsUrl(url)

        fun normalizeByteRangeUrl(url: String): String =
            SnifferUrlClassifier.normalizeByteRangeUrl(url)

        fun isAudioUrl(url: String): Boolean =
            SnifferUrlClassifier.isAudioUrl(url)

        fun isAudioMime(mime: String): Boolean =
            SnifferUrlClassifier.isAudioMime(mime)

        fun isAudioHls(content: String, url: String): Boolean =
            SnifferUrlClassifier.isAudioHls(content, url)

        fun extractDisplayNameFromUrl(url: String, fallback: String, isAudio: Boolean = false): String =
            SnifferUrlClassifier.extractDisplayNameFromUrl(url, fallback, isAudio)
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
