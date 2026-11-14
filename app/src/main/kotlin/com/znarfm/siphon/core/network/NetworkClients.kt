package com.znarfm.siphon.core.network

import android.webkit.CookieManager
import okhttp3.ConnectionPool
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Dispatcher
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object CookieManagerCookieJar : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        runCatching {
            val cm = CookieManager.getInstance()
            val urlStr = url.toString()
            for (cookie in cookies) {
                cm.setCookie(urlStr, cookie.toString())
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val raw = runCatching { CookieManager.getInstance().getCookie(url.toString()) }.getOrNull()
        if (raw.isNullOrBlank()) return emptyList()
        val result = mutableListOf<Cookie>()
        for (pair in raw.split(';')) {
            val trimmed = pair.trim()
            if (trimmed.isEmpty()) continue
            Cookie.parse(url, trimmed)?.let { result.add(it) }
        }
        return result
    }
}

object NetworkClients {

    private val sharedPool = ConnectionPool(16, 5, TimeUnit.MINUTES)

    /**
     * HTTP/1.1 enforced client for chunked byte-range workers.
     * Forces separate TCP connections per worker to bypass per-socket server rate limits.
     */
    val chunkClient: OkHttpClient by lazy {
        val dispatcher = Dispatcher().apply {
            maxRequests = 32
            maxRequestsPerHost = 8
        }
        OkHttpClient.Builder()
            .connectionPool(sharedPool)
            .dispatcher(dispatcher)
            .cookieJar(CookieManagerCookieJar)
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(false)
            .build()
    }

    /**
     * Default HTTP/2 + HTTP/1.1 client for metadata probes, m3u8 playlists, and HLS/DASH segments.
     */
    val defaultClient: OkHttpClient by lazy {
        val dispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 16
        }
        OkHttpClient.Builder()
            .connectionPool(sharedPool)
            .dispatcher(dispatcher)
            .cookieJar(CookieManagerCookieJar)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Cookie-less HTTP/2 + HTTP/1.1 client for incognito tabs.
     */
    val incognitoDefaultClient: OkHttpClient by lazy {
        val dispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 16
        }
        OkHttpClient.Builder()
            .connectionPool(sharedPool)
            .dispatcher(dispatcher)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}

object HeadersCodec {
    const val META_HLS_AUDIO_URL = "__siphon_hls_audio_url"
    const val META_DASH_MPD_URL = "__siphon_dash_mpd_url"
    const val META_DASH_REP_ID = "__siphon_dash_rep_id"

    fun encode(headers: Map<String, String>): String? {
        if (headers.isEmpty()) return null
        val json = JSONObject()
        for ((k, v) in headers) {
            if (k.isNotBlank() && v.isNotBlank()) json.put(k, v)
        }
        return if (json.length() == 0) null else json.toString()
    }

    fun decode(jsonStr: String?): Headers {
        val builder = Headers.Builder()
        if (jsonStr.isNullOrBlank()) return builder.build()
        val json = JSONObject(jsonStr)
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key.startsWith("__siphon_")) continue
            val value = json.optString(key, "")
            if (value.isNotEmpty()) builder.add(key, value)
        }
        return builder.build()
    }

    fun decodeMeta(jsonStr: String?, key: String): String? {
        if (jsonStr.isNullOrBlank()) return null
        return runCatching {
            JSONObject(jsonStr).optString(key, "").takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
