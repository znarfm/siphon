package com.znarfm.siphon.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

data class ProbeResult(
    val finalUrl: String,
    val totalBytes: Long,
    val isResumable: Boolean,
    val strongEtag: String?,
    val lastModified: String?,
    val mimeType: String,
    val suggestedFilename: String?
)

class MetadataProber(
    private val client: OkHttpClient = NetworkClients.defaultClient
) {

    suspend fun probe(url: String, headers: Headers = Headers.Builder().build()): ProbeResult =
        withContext(Dispatchers.IO) {
            val rangeReq = Request.Builder()
                .url(url)
                .headers(headers)
                .header("Range", "bytes=0-0")
                .get()
                .build()

            client.newCall(rangeReq).execute().use { resp ->
                if (resp.code == 405 || resp.code == 501) {
                    return@withContext fallbackHeadProbe(url, headers)
                }
                if (!resp.isSuccessful) {
                    throw HttpStatusException(resp.code, "Probe failed with HTTP ${resp.code}")
                }
                parseProbeResponse(resp)
            }
        }

    /**
     * Verifies a refreshed pre-signed CDN link matches the existing partial download on disk:
     * 1. Content-Length / totalBytes must match expectedTotalBytes.
     * 2. First up-to-4KB sample from the remote URL must byte-match local disk at offset 0.
     */
    suspend fun verifyRefreshedUrl(
        newUrl: String,
        headers: Headers,
        expectedTotalBytes: Long,
        localChannel: FileChannel,
        sampleSize: Int = 4096
    ): ProbeResult = withContext(Dispatchers.IO) {
        val checkLen = minOf(expectedTotalBytes, sampleSize.toLong(), localChannel.size()).toInt()
        val reqLen = if (checkLen > 0) checkLen else 1

        val req = Request.Builder()
            .url(newUrl)
            .headers(headers)
            .header("Range", "bytes=0-${reqLen - 1}")
            .get()
            .build()

        client.newCall(req).execute().use { resp ->
            if (resp.code != 206) {
                throw IOException("Refreshed URL did not return HTTP 206 (got ${resp.code})")
            }
            val parsed = parseProbeResponse(resp)
            if (parsed.totalBytes != expectedTotalBytes) {
                throw IOException(
                    "Size mismatch: expected $expectedTotalBytes, got ${parsed.totalBytes}"
                )
            }

            if (checkLen > 0) {
                val remoteBytes = resp.body?.bytes()
                    ?: throw IOException("Empty body on verification probe")
                if (remoteBytes.size < checkLen) {
                    throw IOException("Short read on verification probe: ${remoteBytes.size} < $checkLen")
                }

                val localBuf = ByteBuffer.allocate(checkLen)
                var pos = 0L
                while (localBuf.hasRemaining()) {
                    val r = localChannel.read(localBuf, pos)
                    if (r <= 0) break
                    pos += r
                }
                localBuf.flip()
                for (i in 0 until checkLen) {
                    if (localBuf.get(i) != remoteBytes[i]) {
                        throw IOException("Byte mismatch at offset $i; remote file differs from local copy")
                    }
                }
            }
            parsed
        }
    }

    private fun fallbackHeadProbe(url: String, headers: Headers): ProbeResult {
        val headReq = Request.Builder()
            .url(url)
            .headers(headers)
            .head()
            .build()
        client.newCall(headReq).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw HttpStatusException(resp.code, "HEAD probe failed with HTTP ${resp.code}")
            }
            return parseProbeResponse(resp)
        }
    }

    private fun parseProbeResponse(resp: Response): ProbeResult {
        val contentRange = resp.header("Content-Range")
        val acceptRanges = resp.header("Accept-Ranges")?.equals("bytes", ignoreCase = true) == true
        val totalFromRange = parseTotalFromContentRange(contentRange)
        val contentLength = resp.header("Content-Length")?.toLongOrNull() ?: -1L

        val totalBytes = when {
            totalFromRange > 0L -> totalFromRange
            resp.code != 206 && contentLength > 0L -> contentLength
            else -> -1L
        }

        val isResumable = totalBytes > 0L && (resp.code == 206 || acceptRanges)
        val rawEtag = resp.header("ETag")?.trim()
        val strongEtag = rawEtag?.takeIf { !it.startsWith("W/", ignoreCase = true) }
        val lastModified = resp.header("Last-Modified")?.trim()
        val mimeType = resp.header("Content-Type")
            ?.substringBefore(';')
            ?.trim()
            ?.ifEmpty { null }
            ?: "application/octet-stream"

        val disposition = resp.header("Content-Disposition")
        val suggestedFilename = parseFilenameFromDisposition(disposition)

        return ProbeResult(
            finalUrl = resp.request.url.toString(),
            totalBytes = totalBytes,
            isResumable = isResumable,
            strongEtag = strongEtag,
            lastModified = lastModified,
            mimeType = mimeType,
            suggestedFilename = suggestedFilename
        )
    }

    companion object {
        /**
         * Parses total length from `Content-Range: bytes start-end/total`.
         */
        fun parseTotalFromContentRange(header: String?): Long {
            if (header.isNullOrBlank()) return -1L
            val slashIdx = header.lastIndexOf('/')
            if (slashIdx == -1 || slashIdx == header.length - 1) return -1L
            val totalPart = header.substring(slashIdx + 1).trim()
            if (totalPart == "*") return -1L
            return totalPart.toLongOrNull() ?: -1L
        }

        /**
         * Parses start offset from `Content-Range: bytes start-end/total`.
         */
        fun parseStartFromContentRange(header: String?): Long {
            if (header.isNullOrBlank()) return -1L
            val rangePart = header.substringAfter("bytes", "").trim()
            val dashIdx = rangePart.indexOf('-')
            if (dashIdx <= 0) return -1L
            return rangePart.substring(0, dashIdx).trim().toLongOrNull() ?: -1L
        }

        private fun parseFilenameFromDisposition(disposition: String?): String? {
            if (disposition.isNullOrBlank()) return null
            val parts = disposition.split(';')
            for (part in parts) {
                val trimmed = part.trim()
                if (trimmed.startsWith("filename=", ignoreCase = true)) {
                    return trimmed.substringAfter('=').trim('"', '\'', ' ').ifEmpty { null }
                }
            }
            return null
        }
    }
}

class HttpStatusException(val code: Int, message: String) : IOException(message)
