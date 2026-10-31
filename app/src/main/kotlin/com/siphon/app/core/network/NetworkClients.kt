package com.siphon.app.core.network

import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(false)
            .build()
    }

    /**
     * Default HTTP/2 + HTTP/1.1 client for metadata probes, m3u8 playlists, and HLS segments.
     */
    val defaultClient: OkHttpClient by lazy {
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
            val value = json.optString(key, "")
            if (value.isNotEmpty()) builder.add(key, value)
        }
        return builder.build()
    }
}
