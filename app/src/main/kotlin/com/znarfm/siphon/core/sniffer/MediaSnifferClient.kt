package com.znarfm.siphon.core.sniffer

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.znarfm.siphon.core.adblock.AdblockEngine
import com.znarfm.siphon.core.common.toOkHttpHeaders
import com.znarfm.siphon.core.hls.HlsPlaylistParser
import com.znarfm.siphon.core.network.HeadersCodec
import com.znarfm.siphon.core.network.MetadataProber
import com.znarfm.siphon.core.network.NetworkClients
import com.znarfm.siphon.core.storage.DownloadStorageProvider
import com.znarfm.siphon.data.db.DownloadDao
import com.znarfm.siphon.data.db.DownloadEntity
import com.znarfm.siphon.data.db.DownloadType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class MediaSnifferClient(
    val adblockEngine: AdblockEngine,
    val metadataProber: MetadataProber = MetadataProber(),
    private val dao: DownloadDao? = null,
    private val storage: DownloadStorageProvider? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    val tabId: String = "",
    private val onAdBlocked: ((pageUrl: String, blockedUrl: String) -> Unit)? = null
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

    @Volatile
    var currentPageTitle: String? = null

    @Volatile
    var defaultUserAgent: String? = null

    @Volatile
    var isIncognito: Boolean = false

    private val pageGeneration = AtomicLong(0L)

    @Volatile
    private var lastPageStartedUrl: String? = null

    @Volatile
    private var lastBlockedMainFrameUrl: String? = null

    var onUrlChanged: ((String) -> Unit)? = null
    var onNavigationStateChanged: ((canGoBack: Boolean, canGoForward: Boolean) -> Unit)? = null
    var onWebViewPageStarted: ((WebView, String) -> Unit)? = null
    var onWebViewPageFinished: ((WebView, String) -> Unit)? = null
    var onResetAdBlockedCount: (() -> Unit)? = null
    var onMagnetLinkClicked: ((String) -> Unit)? = null

    // LRU cache for candidate URLs normalized by stripping byte-range query params
    private val lruDedup = Collections.synchronizedMap(
        object : LinkedHashMap<String, Boolean>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
                return size > 500
            }
        }
    )

    // Manifests captured per page URL to suppress .ts/.m4s/.aac segment probing
    private val pageManifests = ConcurrentHashMap<String, MutableSet<String>>()

    fun setRebindTarget(download: DownloadEntity?) {
        _rebindTarget.value = download
    }

    fun destroy() {
        scope.cancel()
    }

    fun clearDetectedMedia() {
        pageGeneration.incrementAndGet()
        _detectedMedia.value = emptyList()
        lruDedup.clear()
        pageManifests.clear()
        lastPageStartedUrl = null
    }

    fun removeDetectedMedia(id: String) {
        _detectedMedia.update { list -> list.filterNot { it.id == id } }
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
        forceInspect: Boolean = false,
        pageTitle: String? = null
    ) {
        inspectCandidateAsync(url, requestHeaders, hintLang, hintLabel, forceInspect, pageTitle)
    }

    fun notifyAdBlocked(pageUrl: String, blockedUrl: String) {
        onAdBlocked?.invoke(pageUrl, blockedUrl)
    }

    fun resolveSourceUrl(
        requestUrl: String,
        requestHeaders: Map<String, String>? = null
    ): String {
        if (isHttpUrl(currentPageUrl)) {
            return currentPageUrl
        }
        val referer = findReferer(requestHeaders ?: emptyMap())
        if (isHttpUrl(referer)) {
            return referer!!
        }
        return requestUrl
    }

    private fun isHttpUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (!url.isNullOrBlank()) {
            val wasBlockedMainFrame = (url == lastBlockedMainFrameUrl)
            lastBlockedMainFrameUrl = null
            if (!wasBlockedMainFrame) {
                onResetAdBlockedCount?.invoke()
            }
            if (lastPageStartedUrl != null && lastPageStartedUrl != url) {
                clearDetectedMedia()
            }
            lastPageStartedUrl = url
            currentPageUrl = url
            view?.settings?.userAgentString?.takeIf { it.isNotBlank() }?.let { defaultUserAgent = it }
            view?.title?.takeIf { it.isNotBlank() }?.let { currentPageTitle = it }
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
            view?.settings?.userAgentString?.takeIf { it.isNotBlank() }?.let { defaultUserAgent = it }
            view?.title?.takeIf { it.isNotBlank() }?.let { currentPageTitle = it }
            onUrlChanged?.invoke(url)
            onNavigationStateChanged?.invoke(view?.canGoBack() == true, view?.canGoForward() == true)
            if (view != null && adblockEngine.isEnabled && !adblockEngine.isUrlAllowed(url)) {
                val gen = pageGeneration.get()
                scope.launch(Dispatchers.Default) {
                    val cosmetic = adblockEngine.getCosmeticResources(url)
                    val selectorRules = cosmetic.hideSelectors
                        .asSequence()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .chunked(200)
                        .joinToString("\n") { chunk ->
                            chunk.joinToString(", ") + " { display: none !important; }"
                        }
                    val css = when {
                        selectorRules.isNotBlank() -> selectorRules
                        !cosmetic.generichide ->
                            ".adbox, .banner_ads, .adsbox, .textads, [class*='ad-banner'], [id*='ad-container'] { display: none !important; }"
                        else -> ""
                    }
                    val styleJs = if (css.isNotBlank()) {
                        val quotedCss = JSONObject.quote(css)
                        "(function() { var el = document.getElementById('__siphon_adblock_style__'); if (!el) { el = document.createElement('style'); el.id = '__siphon_adblock_style__'; (document.head || document.documentElement).appendChild(el); } el.textContent = $quotedCss; })();"
                    } else null
                    val scriptJs = if (cosmetic.injectedScript.isNotBlank()) {
                        "(function() { try { ${cosmetic.injectedScript}\n} catch(e) {} })();"
                    } else null

                    if (styleJs != null || scriptJs != null) {
                        withContext(Dispatchers.Main) {
                            if (pageGeneration.get() == gen) {
                                styleJs?.let { view.evaluateJavascript(it, null) }
                                scriptJs?.let { view.evaluateJavascript(it, null) }
                            }
                        }
                    }
                }
            }
            if (view != null) onWebViewPageFinished?.invoke(view, url)
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        if (uri.scheme.equals("magnet", ignoreCase = true)) {
            onMagnetLinkClicked?.invoke(uri.toString())
            return true
        }
        val url = uri.toString()
        val pageUrl = resolveSourceUrl(url, request.requestHeaders)
        if (adblockEngine.shouldBlockRequest(url, pageUrl, "main_frame")) {
            onAdBlocked?.invoke(pageUrl, url)
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

        if (uri.scheme.equals("magnet", ignoreCase = true)) {
            return null
        }

        val url = uri.toString()
        val sourceUrl = resolveSourceUrl(url, req.requestHeaders)
        val requestType = inferRequestType(req)

        // 1. Drop ad network subresources immediately
        if (adblockEngine.shouldBlockRequest(url, sourceUrl, requestType)) {
            if (req.isForMainFrame) {
                lastBlockedMainFrameUrl = url
            }
            onAdBlocked?.invoke(sourceUrl, url)
            return adblockEngine.createEmptyResponse()
        }

        if (req.isForMainFrame) {
            lastBlockedMainFrameUrl = null
        }

        // 2. Passive sniffer: strictly return null for all non-ad requests so Chromium loads normally
        inspectCandidateAsync(url, req.requestHeaders)
        return null
    }

    private fun inferRequestType(request: WebResourceRequest): String {
        if (request.isForMainFrame) return "main_frame"

        val accept = request.requestHeaders
            ?.let { findHeaderIgnoreCase(it, "Accept") }
            ?.lowercase(Locale.ROOT)
            .orEmpty()

        when {
            accept.contains("text/css") -> return "stylesheet"
            accept.contains("image/") -> return "image"
            accept.contains("application/javascript") || accept.contains("text/javascript") -> return "script"
            accept.contains("text/html") -> return "sub_frame"
        }

        val path = request.url?.path?.lowercase(Locale.ROOT).orEmpty()
        return when {
            path.endsWith(".js") || path.endsWith(".mjs") -> "script"
            path.endsWith(".css") -> "stylesheet"
            path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                path.endsWith(".gif") || path.endsWith(".webp") || path.endsWith(".svg") ||
                path.endsWith(".ico") -> "image"
            path.endsWith(".woff") || path.endsWith(".woff2") ||
                path.endsWith(".ttf") || path.endsWith(".otf") -> "font"
            path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".m3u8") ||
                path.endsWith(".mpd") || path.endsWith(".ts") -> "media"
            else -> "other"
        }
    }

    private fun httpClientForTab() =
        if (isIncognito) NetworkClients.incognitoDefaultClient else NetworkClients.defaultClient

    private fun findHeaderIgnoreCase(headersMap: Map<String, String>, name: String): String? {
        return headersMap[name]
            ?: headersMap.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }

    private fun registerManifestAndPruneSegments(manifestUrl: String, sourcePage: String) {
        if (currentPageUrl.isNotBlank()) {
            pageManifests.getOrPut(currentPageUrl) {
                Collections.newSetFromMap(ConcurrentHashMap())
            }.add(manifestUrl)
        }
        if (sourcePage.isNotBlank() && sourcePage != currentPageUrl) {
            pageManifests.getOrPut(sourcePage) {
                Collections.newSetFromMap(ConcurrentHashMap())
            }.add(manifestUrl)
        }
        // Retroactively prune any raw .ts/.m4s/.aac segments that raced ahead of this manifest
        _detectedMedia.update { list ->
            list.filterNot { it.mediaType == DownloadType.HTTP && isSegmentUrl(it.url) }
        }
    }

    private fun inspectCandidateAsync(
        url: String,
        requestHeaders: Map<String, String>,
        hintLang: String? = null,
        hintLabel: String? = null,
        forceInspect: Boolean = false,
        pageTitle: String? = null
    ) {
        if (url.startsWith("magnet:", ignoreCase = true) ||
            url.startsWith("blob:", ignoreCase = true) ||
            url.startsWith("data:", ignoreCase = true) ||
            url.startsWith("javascript:", ignoreCase = true)
        ) return

        if (!forceInspect && !isCandidateMediaUrl(url, requestHeaders)) return

        val refererHeader = findHeaderIgnoreCase(requestHeaders, "Referer")
        val sourcePage = refererHeader ?: currentPageUrl

        // 3. Skip segment probing (.ts / .m4s / .aac) if a parent manifest is already captured
        val isAacPath = url.substringBefore('?').substringBefore('#').endsWith(".aac", ignoreCase = true)
        if (!forceInspect && (isSegmentUrl(url) || isAacPath)) {
            val hasMainManifest = !pageManifests[currentPageUrl].isNullOrEmpty()
            val hasRefererManifest = !refererHeader.isNullOrBlank() && !pageManifests[refererHeader].isNullOrEmpty()
            if (hasMainManifest || hasRefererManifest) {
                return
            }
        }

        // Synchronously register manifest URLs before dispatching async work to prevent segment races
        val isManifestUrl = SnifferUrlClassifier.isHlsUrl(url) || SnifferUrlClassifier.isDashUrl(url)
        if (isManifestUrl) {
            registerManifestAndPruneSegments(url, sourcePage)
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
        if (alreadySeen && !forceInspect) return

        // 5. Capture cookies (non-incognito), Referer, Origin, User-Agent, and Authorization
        val headersBuilder = mutableMapOf<String, String>()
        if (!isIncognito) {
            val cookies = runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()
            if (!cookies.isNullOrBlank()) {
                headersBuilder["Cookie"] = cookies
            }
        }
        val effectiveReferer = refererHeader ?: currentPageUrl.takeIf { isHttpUrl(it) }
        effectiveReferer?.let { headersBuilder["Referer"] = it }

        val effectiveUa = findHeaderIgnoreCase(requestHeaders, "User-Agent") ?: defaultUserAgent
        effectiveUa?.takeIf { it.isNotBlank() }?.let { headersBuilder["User-Agent"] = it }

        val effectiveOrigin = findHeaderIgnoreCase(requestHeaders, "Origin")
            ?: effectiveReferer?.let { ref ->
                runCatching {
                    val u = Uri.parse(ref)
                    if (!u.scheme.isNullOrBlank() && !u.authority.isNullOrBlank()) "${u.scheme}://${u.authority}" else null
                }.getOrNull()
            }
        effectiveOrigin?.let { headersBuilder["Origin"] = it }

        findHeaderIgnoreCase(requestHeaders, "Authorization")?.let {
            headersBuilder["Authorization"] = it
        }

        val effectiveTitle = pageTitle?.takeIf { it.isNotBlank() } ?: currentPageTitle
        val gen = pageGeneration.get()

        scope.launch(Dispatchers.IO) {
            if (pageGeneration.get() != gen) return@launch
            val succeeded = when {
                isHlsUrl(url) -> processHlsCandidate(url, headersBuilder, hintLang, hintLabel, effectiveTitle, gen)
                SnifferUrlClassifier.isDashUrl(url) -> processDashCandidate(url, headersBuilder, effectiveTitle, gen)
                else -> processProgressiveCandidate(url, headersBuilder, hintLang, hintLabel, forceInspect, effectiveTitle, gen)
            }
            if (!succeeded) {
                synchronized(lruDedup) { lruDedup.remove(dedupKey) }
            }
        }
    }

    private fun findReferer(headersMap: Map<String, String>): String? {
        return findHeaderIgnoreCase(headersMap, "Referer")
    }

    private suspend fun processHlsCandidate(
        url: String,
        headersMap: Map<String, String>,
        hintLang: String? = null,
        hintLabel: String? = null,
        pageTitle: String? = null,
        gen: Long = pageGeneration.get()
    ): Boolean {
        val sourcePage = findReferer(headersMap) ?: currentPageUrl
        registerManifestAndPruneSegments(url, sourcePage)

        val okHeaders = headersMap.toOkHttpHeaders()

        val req = Request.Builder()
            .url(url)
            .headers(okHeaders)
            .get()
            .build()

        val bodyText = runCatching {
            httpClientForTab().newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        }.getOrNull() ?: return false

        if (pageGeneration.get() != gen) return true

        val parsedMaster = if (HlsPlaylistParser.isMasterPlaylist(bodyText)) {
            HlsPlaylistParser.parseMasterPlaylist(url, bodyText)
        } else null

        if (parsedMaster != null) {
            parsedMaster.subtitleTracks.forEach { sub ->
                val lang = sub.language ?: sub.name
                val subDisplay = buildSubtitleDisplayName(sub.url, sub.name, sub.language, "vtt")
                val subMedia = SniffedMedia(
                    tabId = this.tabId,
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
                if (pageGeneration.get() == gen) {
                    _detectedMedia.update { list ->
                        if (list.none { it.url == subMedia.url }) list + subMedia else list
                    }
                }
            }
        }

        val rawVariants = parsedMaster?.variants?.map { v ->
            val matchedAudio = if (!v.audioGroupId.isNullOrBlank()) {
                parsedMaster.audioTracks.firstOrNull { it.groupId == v.audioGroupId && it.isDefault }
                    ?: parsedMaster.audioTracks.firstOrNull { it.groupId == v.audioGroupId }
            } else null
            HlsVariantInfo(
                url = v.url,
                bandwidth = v.bandwidth,
                resolution = v.resolution,
                codecs = v.codecs,
                audioUrl = matchedAudio?.url?.takeIf { it.isNotBlank() && it != v.url }
            )
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
                    tabId = this.tabId,
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
                if (pageGeneration.get() == gen) {
                    _detectedMedia.update { list ->
                        if (list.none { it.url == subMedia.url }) list + subMedia else list
                    }
                }
                return true
            }
        }

        val isAudioStream = isAudioHls(bodyText, url) ||
            (rawVariants.isNotEmpty() && rawVariants.all { it.resolution == null && it.codecs?.startsWith("mp4a") == true })
        val displayName = SnifferUrlClassifier.extractDisplayNameFromUrl(
            url = url,
            fallback = if (isAudioStream) "audio.m4a" else "stream.m3u8",
            isAudio = isAudioStream,
            pageTitle = pageTitle
        )

        var durationSec = 0.0
        val isMaster = rawVariants.isNotEmpty()
        var sampledBitrate: Long = 0L

        val targetPlaylist = if (isMaster) {
            val probeVariantUrl = rawVariants.firstOrNull()?.url
            if (!probeVariantUrl.isNullOrBlank()) {
                val variantReq = Request.Builder().url(probeVariantUrl).headers(okHeaders).get().build()
                runCatching {
                    httpClientForTab().newCall(variantReq).execute().use { resp ->
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
                    httpClientForTab().newCall(segReq).execute().use { resp ->
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

        if (pageGeneration.get() != gen) return true

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
            tabId = this.tabId,
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

        if (pageGeneration.get() == gen) {
            _detectedMedia.update { list ->
                if (list.none { it.url == media.url }) list + media else list
            }
        }
        return true
    }

    private suspend fun processDashCandidate(
        url: String,
        headersMap: Map<String, String>,
        pageTitle: String? = null,
        gen: Long = pageGeneration.get()
    ): Boolean {
        val sourcePage = findReferer(headersMap) ?: currentPageUrl
        registerManifestAndPruneSegments(url, sourcePage)

        val okHeaders = headersMap.toOkHttpHeaders()

        val req = Request.Builder()
            .url(url)
            .headers(okHeaders)
            .get()
            .build()

        val bodyText = runCatching {
            httpClientForTab().newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        }.getOrNull() ?: return false

        if (pageGeneration.get() != gen) return true
        if (!com.znarfm.siphon.core.dash.DashManifestParser.isDashManifest(bodyText)) return false

        val parsed = runCatching {
            com.znarfm.siphon.core.dash.DashManifestParser.parse(url, bodyText)
        }.getOrNull() ?: return false

        val nonDrmVideos = parsed.videoTracks.filter { !it.isDrmProtected }
        val nonDrmAudios = parsed.audioTracks.filter { !it.isDrmProtected }
        if (nonDrmVideos.isEmpty() && nonDrmAudios.isEmpty()) {
            return true // DRM-only manifest; suppress segments but don't surface broken download
        }

        val isAudioOnly = nonDrmVideos.isEmpty() && nonDrmAudios.isNotEmpty()
        val durationSec = parsed.durationSec
        val variants = nonDrmVideos.map { track ->
            val estBytes = if (durationSec > 0.0 && track.bandwidth > 0L) {
                (track.bandwidth * durationSec / 8.0).toLong()
            } else -1L
            HlsVariantInfo(
                url = track.singleFileUrl ?: "$url#rep=${track.id}",
                bandwidth = track.bandwidth,
                resolution = track.resolution,
                codecs = track.codecs,
                estimatedBytes = estBytes,
                representationId = track.id
            )
        }

        val estimatedTotalBytes = variants.firstOrNull()?.estimatedBytes ?: -1L
        val displayName = SnifferUrlClassifier.extractDisplayNameFromUrl(
            url = url,
            fallback = if (isAudioOnly) "audio.m4a" else "stream.mp4",
            isAudio = isAudioOnly,
            pageTitle = pageTitle
        )

        val isSingleHttpFile = nonDrmVideos.size == 1 &&
            nonDrmVideos.first().isSingleFile &&
            nonDrmAudios.isEmpty() &&
            !nonDrmVideos.first().singleFileUrl.isNullOrBlank()

        val media = SniffedMedia(
            tabId = this.tabId,
            url = if (isSingleHttpFile) nonDrmVideos.first().singleFileUrl!! else url,
            sourcePageUrl = sourcePage,
            mediaType = if (isSingleHttpFile) DownloadType.HTTP else DownloadType.DASH,
            mimeType = if (isAudioOnly) "audio/mp4" else "video/mp4",
            displayName = displayName,
            totalBytes = estimatedTotalBytes,
            headers = headersMap,
            variants = if (isSingleHttpFile) emptyList() else variants,
            durationSec = durationSec,
            isAudio = isAudioOnly
        )

        if (pageGeneration.get() == gen) {
            _detectedMedia.update { list ->
                if (list.none { it.url == media.url }) list + media else list
            }
        }
        return true
    }

    private suspend fun processProgressiveCandidate(
        url: String,
        headersMap: Map<String, String>,
        hintLang: String? = null,
        hintLabel: String? = null,
        forceInspect: Boolean = false,
        pageTitle: String? = null,
        gen: Long = pageGeneration.get()
    ): Boolean {
        val sourcePage = findReferer(headersMap) ?: currentPageUrl
        val okHeaders = headersMap.toOkHttpHeaders()

        val probeResult = runCatching {
            metadataProber.probe(url, okHeaders)
        }.getOrNull() ?: return false

        if (pageGeneration.get() != gen) return true

        val mime = probeResult.mimeType.lowercase(Locale.ROOT)
        if (mime.contains("mpegurl")) {
            return processHlsCandidate(url, headersMap, hintLang, hintLabel, pageTitle, gen)
        }
        if (mime.contains("dash+xml")) {
            return processDashCandidate(url, headersMap, pageTitle, gen)
        }

        val isSub = isSubtitleUrl(probeResult.finalUrl) || isSubtitleUrl(url) || isSubtitleMime(mime)
        val isAudio = isAudioUrl(probeResult.finalUrl) || isAudioUrl(url) || isAudioMime(mime)
        val isMediaMime = mime.startsWith("video/") || mime.startsWith("audio/") ||
            mime == "application/octet-stream" || mime == "application/zip" ||
            mime == "application/x-zip-compressed" || isSub || isAudio || forceInspect

        if (probeResult.totalBytes <= 0 && !isMediaMime) {
            return false
        }

        val ext = subtitleExtFromUrlOrMime(probeResult.finalUrl, mime)
        val lang = if (isSub) (hintLang ?: hintLabel ?: inferLanguageFromUrl(probeResult.finalUrl)) else null
        val displayName = if (isSub) {
            probeResult.suggestedFilename ?: buildSubtitleDisplayName(probeResult.finalUrl, hintLabel, lang, ext)
        } else {
            probeResult.suggestedFilename ?: SnifferUrlClassifier.extractDisplayNameFromUrl(
                url = url,
                fallback = if (isAudio) "audio.mp3" else "media",
                isAudio = isAudio,
                pageTitle = pageTitle
            )
        }

        val resolvedMime = if (isSub && (mime == "application/octet-stream" || mime.startsWith("text/plain"))) {
            subtitleMimeForExt(ext)
        } else {
            probeResult.mimeType
        }

        val media = SniffedMedia(
            tabId = this.tabId,
            url = probeResult.finalUrl,
            sourcePageUrl = sourcePage,
            mediaType = DownloadType.HTTP,
            mimeType = resolvedMime,
            displayName = displayName,
            totalBytes = probeResult.totalBytes,
            headers = headersMap,
            isSubtitle = isSub,
            isAudio = isAudio,
            language = lang,
            strongEtag = probeResult.strongEtag,
            lastModified = probeResult.lastModified,
            isResumable = probeResult.isResumable
        )

        // Check explicit "Update Link" rebind flow if active
        val target = _rebindTarget.value
        if (target != null && dao != null && storage != null) {
            attemptRebind(media, target, probeResult.strongEtag)
        }

        if (pageGeneration.get() == gen) {
            _detectedMedia.update { list ->
                if (list.none { it.url == media.url }) list + media else list
            }
        }
        return true
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
                metadataProber.verifyRefreshedUrl(
                    newUrl = candidate.url,
                    headers = candidate.headers.toOkHttpHeaders(),
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
        postCandidateWithTitle(url, headersJson, lang, label, null)
    }

    @android.webkit.JavascriptInterface
    fun postCandidateWithTitle(
        url: String?,
        headersJson: String?,
        lang: String?,
        label: String?,
        pageTitle: String?
    ) {
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
        if (!pageTitle.isNullOrBlank()) {
            client.currentPageTitle = pageTitle
        }
        client.inspectCandidate(url, headers, lang, label, pageTitle = pageTitle)
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
                if (!document || document.__siphon_injected__) return;
                document.__siphon_injected__ = true;

                try {
                    window.canRunAds = true;
                    window.isAdBlockActive = false;
                    window.adBlockDetected = false;
                    if (!window.adsbygoogle) {
                        window.adsbygoogle = { loaded: true, push: function() {} };
                    }
                    var AD_TOKEN_REGEX = /(^|[-_\s])ads?([-_\s]|$)/i;
                    var origGetComputedStyle = window.getComputedStyle;
                    window.getComputedStyle = function(el, pseudo) {
                        var style = origGetComputedStyle.apply(this, arguments);
                        if (el && ((el.id && AD_TOKEN_REGEX.test(el.id)) || (el.className && typeof el.className === 'string' && AD_TOKEN_REGEX.test(el.className)))) {
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
                        var absUrl = new URL(url, document.baseURI || window.location.href).href;
                        var title = (document && document.title) ? document.title : null;
                        if (window.__siphon_sniffer__) {
                            if (window.__siphon_sniffer__.postCandidateWithTitle) {
                                window.__siphon_sniffer__.postCandidateWithTitle(absUrl, headers ? JSON.stringify(headers) : null, lang || null, label || null, title);
                            } else if (window.__siphon_sniffer__.postCandidate) {
                                window.__siphon_sniffer__.postCandidate(absUrl, headers ? JSON.stringify(headers) : null, lang || null, label || null);
                            }
                        }
                    } catch(e) {}
                }

                var MEDIA_REGEX = /\.(m3u8|mpd|mp4|webm|mkv|m4v|mp3|m4a|aac|ogg|opus|wav|flac|vtt|srt|zip)(\?[^"'\s]*)?$/i;
                var RPC_STREAM_REGEX = /https?:\/\/[^\s"'<>]+\.(?:m3u8|mpd|mp4|webm|m4v|mp3|m4a|aac|ogg|opus|flac|wav|zip)[\w\-.~:/?#\[\]@!$&'()*+,;=]*/gi;

                function checkUrl(url, headers) {
                    if (typeof url === 'string' && MEDIA_REGEX.test(url)) {
                        notify(url, headers);
                    }
                }

                function isMediaContentType(ct) {
                    if (!ct) return false;
                    var l = ct.toLowerCase();
                    return l.indexOf('video/') !== -1 || l.indexOf('audio/') !== -1 ||
                        l.indexOf('mpegurl') !== -1 || l.indexOf('dash+xml') !== -1 ||
                        l.indexOf('application/zip') !== -1;
                }

                if (window.WebSocket && !window.WebSocket.__siphon_wrapped__) {
                    var OrigWebSocket = window.WebSocket;
                    var WrappedWS = function(url, protocols) {
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
                    WrappedWS.prototype = OrigWebSocket.prototype;
                    WrappedWS.CONNECTING = OrigWebSocket.CONNECTING;
                    WrappedWS.OPEN = OrigWebSocket.OPEN;
                    WrappedWS.CLOSING = OrigWebSocket.CLOSING;
                    WrappedWS.CLOSED = OrigWebSocket.CLOSED;
                    WrappedWS.__siphon_wrapped__ = true;
                    window.WebSocket = WrappedWS;
                }

                if (window.fetch && !window.fetch.__siphon_wrapped__) {
                    var origFetch = window.fetch;
                    var wrappedFetch = function(resource, init) {
                        var url = typeof resource === 'string' ? resource : (resource && resource.url ? resource.url : '');
                        var reqHeaders = null;
                        try {
                            if (init && init.headers) {
                                reqHeaders = {};
                                if (typeof init.headers.forEach === 'function') {
                                    init.headers.forEach(function(v, k) { reqHeaders[k] = v; });
                                } else {
                                    Object.keys(init.headers).forEach(function(k) { reqHeaders[k] = init.headers[k]; });
                                }
                            }
                        } catch(e) {}
                        if (url) checkUrl(url, reqHeaders);
                        return origFetch.apply(this, arguments).then(function(response) {
                            try {
                                var ct = response.headers.get('content-type') || '';
                                if (isMediaContentType(ct)) {
                                    notify(response.url || url, reqHeaders);
                                }
                            } catch(e) {}
                            return response;
                        });
                    };
                    wrappedFetch.__siphon_wrapped__ = true;
                    window.fetch = wrappedFetch;
                }

                if (window.XMLHttpRequest && !window.XMLHttpRequest.prototype.__siphon_wrapped__) {
                    var origXhrOpen = window.XMLHttpRequest.prototype.open;
                    var origXhrSetHeader = window.XMLHttpRequest.prototype.setRequestHeader;
                    var origXhrSend = window.XMLHttpRequest.prototype.send;
                    window.XMLHttpRequest.prototype.__siphon_wrapped__ = true;

                    window.XMLHttpRequest.prototype.open = function(method, url) {
                        this.__siphon_url__ = url;
                        this.__siphon_headers__ = {};
                        return origXhrOpen.apply(this, arguments);
                    };

                    window.XMLHttpRequest.prototype.setRequestHeader = function(header, value) {
                        try {
                            if (this.__siphon_headers__ && header) {
                                this.__siphon_headers__[header] = value;
                            }
                        } catch(e) {}
                        return origXhrSetHeader.apply(this, arguments);
                    };

                    window.XMLHttpRequest.prototype.send = function() {
                        var xhr = this;
                        try {
                            if (xhr.__siphon_url__) {
                                checkUrl(xhr.__siphon_url__, xhr.__siphon_headers__);
                            }
                            xhr.addEventListener('load', function() {
                                try {
                                    var ct = xhr.getResponseHeader('content-type') || '';
                                    if (isMediaContentType(ct)) {
                                        notify(xhr.responseURL || xhr.__siphon_url__, xhr.__siphon_headers__);
                                    }
                                } catch(e) {}
                            });
                        } catch(e) {}
                        return origXhrSend.apply(this, arguments);
                    };
                }

                function inspectMediaElement(el) {
                    if (!el || !el.tagName) return;
                    var tag = el.tagName.toLowerCase();
                    if (tag === 'video' || tag === 'audio') {
                        if (el.style && el.style.display === 'none') el.style.display = 'block';
                        if (el.style && el.style.visibility === 'hidden') el.style.visibility = 'visible';
                        if (el.hidden) el.hidden = false;

                        var src = el.src || el.currentSrc;
                        if (src) notify(src);

                        el.querySelectorAll('source').forEach(function(s) {
                            if (s.src) notify(s.src);
                        });
                        el.querySelectorAll('track').forEach(function(t) {
                            if (t.src) notify(t.src, null, t.srclang, t.label);
                        });
                    } else if (tag === 'source' && el.src) {
                        notify(el.src);
                    } else if (tag === 'track' && el.src) {
                        notify(el.src, null, el.srclang, el.label);
                    }
                }

                function sweepMediaElements() {
                    document.querySelectorAll('video, audio').forEach(inspectMediaElement);
                    $domainHook
                }

                sweepMediaElements();
                if (document.readyState !== 'complete') {
                    window.addEventListener('DOMContentLoaded', sweepMediaElements);
                    window.addEventListener('load', sweepMediaElements);
                }

                if (window.MutationObserver && !document.__siphon_observer__) {
                    var targetNode = document.documentElement || document;
                    if (targetNode) {
                        var observer = new MutationObserver(function(mutations) {
                            for (var i = 0; i < mutations.length; i++) {
                                var m = mutations[i];
                                if (m.type === 'attributes' && m.target) {
                                    inspectMediaElement(m.target);
                                } else if (m.type === 'childList' && m.addedNodes) {
                                    for (var j = 0; j < m.addedNodes.length; j++) {
                                        var node = m.addedNodes[j];
                                        if (node && node.nodeType === 1) {
                                            inspectMediaElement(node);
                                            if (node.querySelectorAll) {
                                                node.querySelectorAll('video, audio, source, track').forEach(inspectMediaElement);
                                            }
                                        }
                                    }
                                }
                            }
                        });
                        observer.observe(targetNode, {
                            childList: true,
                            subtree: true,
                            attributes: true,
                            attributeFilter: ['src', 'currentSrc']
                        });
                        document.__siphon_observer__ = observer;
                    }
                }
            })();
        """.trimIndent()
    }
}
