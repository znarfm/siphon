package com.znarfm.siphon.core.engine

import android.content.Context
import android.net.Uri
import com.znarfm.siphon.core.network.HttpStatusException
import com.znarfm.siphon.core.storage.DownloadStorageProvider
import com.znarfm.siphon.data.db.DownloadDao
import com.znarfm.siphon.data.db.DownloadEntity
import com.znarfm.siphon.data.db.DownloadState
import com.znarfm.siphon.data.db.DownloadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer

internal data class ResolvedTarget(
    val displayName: String,
    val mimeType: String,
    val isSubtitle: Boolean
)

/**
 * Shared helpers for segmented stream engines ([HlsQueueEngine], [DashQueueEngine]).
 */
internal object SegmentedStreamSupport {

    private const val COPY_BUFFER_BYTES = 128 * 1024

    fun resolveTarget(download: DownloadEntity): ResolvedTarget {
        val isSubDownload = download.mimeType.startsWith("text/") ||
            download.displayName.endsWith(".vtt", ignoreCase = true) ||
            download.displayName.endsWith(".srt", ignoreCase = true)

        val isAudioDownload = download.mimeType.startsWith("audio/") ||
            download.displayName.endsWith(".mp3", ignoreCase = true) ||
            download.displayName.endsWith(".m4a", ignoreCase = true) ||
            download.displayName.endsWith(".aac", ignoreCase = true)

        val targetMime = when {
            isSubDownload -> download.mimeType.ifBlank { "text/vtt" }
            isAudioDownload -> if (download.displayName.endsWith(".mp3", ignoreCase = true)) "audio/mpeg" else "audio/mp4"
            download.type == DownloadType.HLS ||
                download.type == DownloadType.DASH ||
                download.displayName.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            else -> download.mimeType
        }

        val targetName = when {
            isAudioDownload -> {
                if (!download.displayName.endsWith(".m4a", ignoreCase = true) &&
                    !download.displayName.endsWith(".mp3", ignoreCase = true)
                ) {
                    "${download.displayName.substringBeforeLast('.')}.m4a"
                } else {
                    download.displayName
                }
            }
            targetMime == "video/mp4" && !download.displayName.endsWith(".mp4", ignoreCase = true) -> {
                "${download.displayName.substringBeforeLast('.')}.mp4"
            }
            else -> download.displayName
        }

        return ResolvedTarget(
            displayName = targetName,
            mimeType = targetMime,
            isSubtitle = isSubDownload
        )
    }

    suspend fun resolveOrCreateDestination(
        download: DownloadEntity,
        target: ResolvedTarget,
        dao: DownloadDao,
        storage: DownloadStorageProvider,
        customTreeUri: Uri?
    ): Uri {
        return if (!download.destinationUri.isNullOrBlank()) {
            Uri.parse(download.destinationUri).also { storage.refreshPendingExpiry(it) }
        } else {
            storage.createDestination(
                displayName = target.displayName,
                mimeType = target.mimeType,
                customTreeUri = customTreeUri
            ).also { created ->
                dao.updateDestinationUri(download.id, created.toString())
            }
        }
    }

    suspend fun writeAndFinalizeOutput(
        downloadId: Long,
        sourceFile: File,
        destUri: Uri,
        workDir: File,
        dao: DownloadDao,
        storage: DownloadStorageProvider,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)?
    ): Long {
        val totalBytes = sourceFile.length()
        storage.openWriteSession(destUri, totalBytes = totalBytes).use { session ->
            val destChannel = session.channel
            destChannel.truncate(0)
            destChannel.position(0)
            var copiedBytes = 0L
            FileInputStream(sourceFile).channel.use { srcChannel ->
                val buffer = ByteBuffer.allocateDirect(COPY_BUFFER_BYTES)
                while (srcChannel.read(buffer) > 0) {
                    buffer.flip()
                    copiedBytes += destChannel.write(buffer)
                    buffer.clear()
                    onProgress?.invoke(copiedBytes, totalBytes)
                }
            }
            destChannel.force(true)
        }

        storage.finalizePending(destUri)
        dao.updateTotalBytes(downloadId, totalBytes)
        dao.markTerminalAndPurgeCredentials(downloadId, DownloadState.COMPLETED)
        workDir.deleteRecursively()
        return totalBytes
    }

    fun fetchText(
        client: OkHttpClient,
        url: String,
        headers: Headers,
        errorLabel: String
    ): String {
        val req = Request.Builder().url(url).headers(headers).get().build()
        return client.newCall(req).execute().use { resp ->
            if (resp.code == 403 || resp.code == 410) {
                throw HttpStatusException(resp.code, "Failed to fetch $errorLabel HTTP ${resp.code}")
            }
            if (!resp.isSuccessful) throw IOException("Failed to fetch $errorLabel HTTP ${resp.code}")
            resp.body?.string() ?: throw IOException("Empty $errorLabel response")
        }
    }

    fun findHttpStatusException(throwable: Throwable?): HttpStatusException? {
        var cur = throwable
        while (cur != null) {
            if (cur is HttpStatusException) return cur
            cur = cur.cause
        }
        return null
    }

    fun mapFailureState(throwable: Throwable?): DownloadState {
        val httpEx = findHttpStatusException(throwable)
        return if (httpEx?.code == 403 || httpEx?.code == 410) {
            DownloadState.AUTH_EXPIRED
        } else {
            DownloadState.FAILED
        }
    }

    suspend fun cancelAndDelete(
        context: Context,
        workDirPrefix: String,
        downloadId: Long,
        dao: DownloadDao,
        storage: DownloadStorageProvider
    ) = withContext(Dispatchers.IO + NonCancellable) {
        val download = dao.getDownload(downloadId)
        File(context.cacheDir, "${workDirPrefix}_$downloadId").deleteRecursively()
        if (download?.destinationUri != null) {
            storage.delete(download.destinationUri)
        }
        dao.deleteChunksForDownload(downloadId)
        dao.markTerminalAndPurgeCredentials(downloadId, DownloadState.CANCELLED)
    }
}
