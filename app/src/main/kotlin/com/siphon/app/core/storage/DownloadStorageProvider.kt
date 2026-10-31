package com.siphon.app.core.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.Closeable
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel

private const val PENDING_EXPIRY_SECONDS = 30L * 24L * 60L * 60L // 30 days

class StorageSession(
    val uri: Uri,
    val preflight: PreflightReport,
    val channel: FileChannel,
    private val streamCloseable: Closeable,
    private val pfd: ParcelFileDescriptor
) : Closeable {
    override fun close() {
        runCatching { channel.close() }
        runCatching { streamCloseable.close() }
        runCatching { pfd.close() }
    }
}

class ReadChannelSession(
    val channel: FileChannel,
    private val fis: FileInputStream,
    private val pfd: ParcelFileDescriptor
) : Closeable {
    override fun close() {
        runCatching { channel.close() }
        runCatching { fis.close() }
        runCatching { pfd.close() }
    }
}

class DownloadStorageProvider(context: Context) {

    private val appContext: Context = context.applicationContext
    private val resolver: ContentResolver = appContext.contentResolver

    /**
     * Creates a new destination URI in either [MediaStore.Downloads] (with `IS_PENDING = 1`
     * and `+30d` `DATE_EXPIRES`) or a user-selected SAF document tree ([customTreeUri]).
     */
    fun createDestination(
        displayName: String,
        mimeType: String,
        customTreeUri: Uri? = null
    ): Uri {
        val safeName = sanitizeDisplayName(displayName)
        val safeMime = mimeType.ifBlank { "application/octet-stream" }

        if (customTreeUri != null) {
            return createSafDocument(customTreeUri, safeName, safeMime)
        }

        val nowSeconds = System.currentTimeMillis() / 1000L
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, safeName)
            put(MediaStore.Downloads.MIME_TYPE, safeMime)
            put(MediaStore.Downloads.IS_PENDING, 1)
            put(MediaStore.Downloads.DATE_EXPIRES, nowSeconds + PENDING_EXPIRY_SECONDS)
        }

        return resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Failed to insert pending row into MediaStore.Downloads")
    }

    /**
     * Opens a positional-writeable [FileChannel] for [uri] and runs [FilesystemPreflight.check]
     * against the underlying file descriptor before returning.
     */
    fun openWriteSession(
        uri: Uri,
        totalBytes: Long,
        alreadyDownloadedBytes: Long = 0L,
        requestedWorkers: Int = 6
    ): StorageSession {
        val pfd = resolver.openFileDescriptor(uri, "rw")
            ?: throw IOException("Unable to open rw file descriptor for $uri")

        try {
            val preflight = FilesystemPreflight.check(
                pfd = pfd,
                totalBytes = totalBytes,
                alreadyDownloadedBytes = alreadyDownloadedBytes,
                requestedWorkers = requestedWorkers
            )
            val fos = FileOutputStream(pfd.fileDescriptor)
            return StorageSession(
                uri = uri,
                preflight = preflight,
                channel = fos.channel,
                streamCloseable = fos,
                pfd = pfd
            )
        } catch (t: Throwable) {
            runCatching { pfd.close() }
            throw t
        }
    }

    /**
     * Opens a read-only [FileChannel] for [uri] (e.g. for 4KB overlap verification when
     * rebinding an expired pre-signed URL).
     */
    fun openReadSession(uri: Uri): ReadChannelSession {
        val pfd = resolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Unable to open read file descriptor for $uri")
        try {
            val fis = FileInputStream(pfd.fileDescriptor)
            return ReadChannelSession(
                channel = fis.channel,
                fis = fis,
                pfd = pfd
            )
        } catch (t: Throwable) {
            runCatching { pfd.close() }
            throw t
        }
    }

    /**
     * Publishes a completed download by flipping `IS_PENDING = 0` on MediaStore URIs.
     * No-op for SAF document URIs.
     */
    fun finalizePending(uri: Uri) {
        if (!isMediaStoreUri(uri)) return
        val values = ContentValues().apply {
            put(MediaStore.Downloads.IS_PENDING, 0)
            putNull(MediaStore.Downloads.DATE_EXPIRES)
        }
        resolver.update(uri, values, null, null)
    }

    /**
     * Extends `DATE_EXPIRES` by another 30 days on resume so the OS never garbage-collects
     * long-paused `IS_PENDING = 1` downloads.
     */
    fun refreshPendingExpiry(uri: Uri) {
        if (!isMediaStoreUri(uri)) return
        val nowSeconds = System.currentTimeMillis() / 1000L
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DATE_EXPIRES, nowSeconds + PENDING_EXPIRY_SECONDS)
        }
        runCatching { resolver.update(uri, values, null, null) }
    }

    /**
     * Deletes the target file or orphan `IS_PENDING = 1` row on cancel/delete.
     */
    fun delete(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        delete(Uri.parse(uriString))
    }

    fun delete(uri: Uri) {
        runCatching {
            if (!isMediaStoreUri(uri) && DocumentsContract.isDocumentUri(appContext, uri)) {
                if (runCatching { DocumentsContract.deleteDocument(resolver, uri) }.getOrDefault(false)) {
                    return
                }
            }
            resolver.delete(uri, null, null)
        }
    }

    private fun createSafDocument(treeUri: Uri, displayName: String, mimeType: String): Uri {
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        return DocumentsContract.createDocument(resolver, parentDocUri, mimeType, displayName)
            ?: throw IOException("Failed to create SAF document '$displayName' in $treeUri")
    }

    private fun isMediaStoreUri(uri: Uri): Boolean =
        uri.authority == MediaStore.AUTHORITY

    companion object {
        fun sanitizeDisplayName(raw: String): String {
            val cleaned = raw
                .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
                .trim()
                .trim('.')
            return cleaned.ifEmpty { "download_${System.currentTimeMillis()}" }
        }
    }
}
