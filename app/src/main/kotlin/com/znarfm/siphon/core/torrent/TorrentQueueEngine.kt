package com.znarfm.siphon.core.torrent

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.znarfm.siphon.core.storage.DownloadStorageProvider
import com.znarfm.siphon.data.db.DownloadDao
import com.znarfm.siphon.data.db.DownloadFileEntity
import com.znarfm.siphon.data.db.DownloadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.libtorrent4j.AlertListener
import org.libtorrent4j.Priority
import org.libtorrent4j.SessionManager
import org.libtorrent4j.Sha1Hash
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.TorrentStatus
import org.libtorrent4j.alerts.AddTorrentAlert
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.alerts.MetadataReceivedAlert
import org.libtorrent4j.alerts.TorrentErrorAlert
import org.libtorrent4j.alerts.TorrentFinishedAlert
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long = 0L,
    val peers: Int = 0
)

class TorrentQueueEngine(
    private val context: Context,
    private val dao: DownloadDao,
    private val storageProvider: DownloadStorageProvider
) {
    private val sessionLock = Any()
    @Volatile
    private var sessionManager: SessionManager? = null
    @Volatile
    private var currentRateLimit: Int = 0

    private val activeHashes = ConcurrentHashMap<Long, Sha1Hash>()
    private val hexToDownloadId = ConcurrentHashMap<String, Long>()
    private val pausedIds = ConcurrentHashMap.newKeySet<Long>()
    private val cancelledIds = ConcurrentHashMap.newKeySet<Long>()
    private val finishedIds = ConcurrentHashMap.newKeySet<Long>()
    private val metadataUpdates = ConcurrentHashMap<Long, Pair<String, Long>>()
    private val errorMessages = ConcurrentHashMap<Long, String>()

    private val alertListener = object : AlertListener {
        override fun types(): IntArray = intArrayOf(
            AlertType.ADD_TORRENT.swig(),
            AlertType.METADATA_RECEIVED.swig(),
            AlertType.TORRENT_FINISHED.swig(),
            AlertType.TORRENT_ERROR.swig()
        )

        override fun alert(alert: Alert<*>) {
            when (alert.type()) {
                AlertType.ADD_TORRENT -> {
                    val addAlert = alert as? AddTorrentAlert ?: return
                    val handle = addAlert.handle() ?: return
                    if (handle.isValid) {
                        val hex = handle.infoHash().toHex()
                        val downloadId = hexToDownloadId[hex] ?: return
                        activeHashes[downloadId] = handle.infoHash()
                    }
                }
                AlertType.METADATA_RECEIVED -> {
                    val metaAlert = alert as? MetadataReceivedAlert ?: return
                    val handle = metaAlert.handle() ?: return
                    if (handle.isValid) {
                        val hex = handle.infoHash().toHex()
                        val downloadId = hexToDownloadId[hex] ?: return
                        val ti = handle.torrentFile()
                        if (ti != null && ti.isValid) {
                            metadataUpdates[downloadId] = ti.name() to ti.totalSize()
                        }
                    }
                }
                AlertType.TORRENT_FINISHED -> {
                    val finAlert = alert as? TorrentFinishedAlert ?: return
                    val handle = finAlert.handle() ?: return
                    if (handle.isValid) {
                        val hex = handle.infoHash().toHex()
                        val downloadId = hexToDownloadId[hex] ?: return
                        finishedIds.add(downloadId)
                    }
                }
                AlertType.TORRENT_ERROR -> {
                    val errAlert = alert as? TorrentErrorAlert ?: return
                    val handle = errAlert.handle() ?: return
                    if (handle.isValid) {
                        val hex = handle.infoHash().toHex()
                        val downloadId = hexToDownloadId[hex] ?: return
                        errorMessages.put(downloadId, errAlert.message() ?: "Torrent error")
                    }
                }
                else -> {}
            }
        }
    }

    private fun ensureSessionStarted(): SessionManager {
        synchronized(sessionLock) {
            val existing = sessionManager
            if (existing != null) {
                if (!existing.isRunning) {
                    existing.start()
                }
                return existing
            }
            val sm = SessionManager()
            sm.addListener(alertListener)
            sm.start()
            if (currentRateLimit > 0) {
                sm.downloadRateLimit(currentRateLimit)
            }
            sessionManager = sm
            return sm
        }
    }

    suspend fun execute(
        downloadId: Long,
        magnetOrUrl: String,
        torrentBytes: ByteArray?,
        onProgress: (DownloadProgress) -> Unit
    ): DownloadState = withContext(Dispatchers.IO) {
        pausedIds.remove(downloadId)
        cancelledIds.remove(downloadId)
        finishedIds.remove(downloadId)
        metadataUpdates.remove(downloadId)
        errorMessages.remove(downloadId)

        dao.updateDownloadState(downloadId, DownloadState.ACTIVE)

        val stagingDir = File(context.filesDir, "torrent_staging/$downloadId").apply { mkdirs() }
        val sm = ensureSessionStarted()

        var infoHash: Sha1Hash? = null
        var metadataApplied = false
        var torrentName = dao.getDownload(downloadId)?.displayName ?: "Torrent"
        var lastDbProgressMs = 0L
        var lastKnownDone = 0L
        var lastKnownTotal = 0L

        try {
            if (torrentBytes != null) {
                val ti = TorrentInfo.bdecode(torrentBytes)
                val hash = ti.infoHash()
                infoHash = hash
                activeHashes[downloadId] = hash
                hexToDownloadId[hash.toHex()] = downloadId

                val sanitizedName = DownloadStorageProvider.sanitizeDisplayName(ti.name())
                torrentName = sanitizedName
                dao.updateDisplayName(downloadId, sanitizedName)
                if (ti.totalSize() > 0L) {
                    lastKnownTotal = ti.totalSize()
                    dao.updateTotalBytes(downloadId, ti.totalSize())
                }
                metadataApplied = true

                val existing = sm.find(hash)
                if (existing != null && existing.isValid) {
                    existing.resume()
                } else {
                    sm.download(ti, stagingDir)
                }
            } else if (magnetOrUrl.startsWith("magnet:", ignoreCase = true)) {
                val magnetBytes = sm.fetchMagnet(magnetOrUrl, 60, stagingDir)
                    ?: run {
                        dao.updateDownloadState(downloadId, DownloadState.FAILED)
                        return@withContext DownloadState.FAILED
                    }
                val ti = TorrentInfo.bdecode(magnetBytes)
                val hash = ti.infoHash()
                infoHash = hash
                activeHashes[downloadId] = hash
                hexToDownloadId[hash.toHex()] = downloadId

                val sanitizedName = DownloadStorageProvider.sanitizeDisplayName(ti.name())
                torrentName = sanitizedName
                dao.updateDisplayName(downloadId, sanitizedName)
                if (ti.totalSize() > 0L) {
                    lastKnownTotal = ti.totalSize()
                    dao.updateTotalBytes(downloadId, ti.totalSize())
                }
                metadataApplied = true

                val existing = sm.find(hash)
                if (existing != null && existing.isValid) {
                    existing.resume()
                } else {
                    sm.download(ti, stagingDir)
                }
            } else {
                dao.updateDownloadState(downloadId, DownloadState.FAILED)
                return@withContext DownloadState.FAILED
            }

            val trackedHash = infoHash!!

            while (currentCoroutineContext().isActive) {
                if (cancelledIds.contains(downloadId)) {
                    val handle = sm.find(trackedHash)
                    if (handle != null && handle.isValid) {
                        sm.remove(handle)
                    }
                    stagingDir.deleteRecursively()
                    dao.updateDownloadState(downloadId, DownloadState.CANCELLED)
                    return@withContext DownloadState.CANCELLED
                }

                if (pausedIds.contains(downloadId)) {
                    val handle = sm.find(trackedHash)
                    if (handle != null && handle.isValid) {
                        handle.pause()
                    }
                    dao.updateTorrentProgress(downloadId, lastKnownDone, 0L, 0)
                    dao.updateDownloadState(downloadId, DownloadState.PAUSED)
                    return@withContext DownloadState.PAUSED
                }

                val err = errorMessages.remove(downloadId)
                if (err != null) {
                    val handle = sm.find(trackedHash)
                    if (handle != null && handle.isValid) {
                        sm.remove(handle)
                    }
                    dao.updateDownloadState(downloadId, DownloadState.FAILED)
                    return@withContext DownloadState.FAILED
                }

                val metaUpdate = metadataUpdates.remove(downloadId)
                if (metaUpdate != null) {
                    val (name, totalSize) = metaUpdate
                    if (name.isNotBlank()) {
                        val sanitized = DownloadStorageProvider.sanitizeDisplayName(name)
                        torrentName = sanitized
                        dao.updateDisplayName(downloadId, sanitized)
                    }
                    if (totalSize > 0L) {
                        lastKnownTotal = totalSize
                        dao.updateTotalBytes(downloadId, totalSize)
                    }
                    metadataApplied = true
                }

                val handle: TorrentHandle? = sm.find(trackedHash)
                if (handle != null && handle.isValid) {
                    if (!metadataApplied) {
                        val ti = handle.torrentFile()
                        if (ti != null && ti.isValid) {
                            val name = ti.name()
                            if (name.isNotBlank()) {
                                val sanitized = DownloadStorageProvider.sanitizeDisplayName(name)
                                torrentName = sanitized
                                dao.updateDisplayName(downloadId, sanitized)
                            }
                            if (ti.totalSize() > 0L) {
                                lastKnownTotal = ti.totalSize()
                                dao.updateTotalBytes(downloadId, ti.totalSize())
                            }
                            if (ti.numFiles() > 0) {
                                handle.prioritizeFiles(Array(ti.numFiles()) { Priority.DEFAULT })
                            }
                            metadataApplied = true
                        }
                    }

                    val status: TorrentStatus = handle.status()
                    val totalWanted = status.totalWanted().coerceAtLeast(0L)
                    val totalDone = status.totalDone().coerceAtLeast(0L)
                    val speedBytesPerSec = status.downloadPayloadRate().toLong().coerceAtLeast(0L)
                    val peers = status.numPeers().coerceAtLeast(0)

                    lastKnownDone = totalDone
                    if (totalWanted > 0L) {
                        lastKnownTotal = totalWanted
                    }

                    val nowMs = System.currentTimeMillis()
                    if (nowMs - lastDbProgressMs >= 500L) {
                        dao.updateTorrentProgress(downloadId, totalDone, speedBytesPerSec, peers)
                        lastDbProgressMs = nowMs
                    }

                    onProgress(
                        DownloadProgress(
                            downloadedBytes = totalDone,
                            totalBytes = totalWanted,
                            speedBytesPerSec = speedBytesPerSec,
                            peers = peers
                        )
                    )

                    if (finishedIds.contains(downloadId) || status.isFinished || status.isSeeding) {
                        break
                    }
                }

                delay(250L)
            }

            if (!currentCoroutineContext().isActive) {
                throw CancellationException("Torrent job cancelled")
            }

            val finalDone = if (lastKnownTotal > 0L) maxOf(lastKnownDone, lastKnownTotal) else lastKnownDone
            dao.updateTorrentProgress(downloadId, finalDone, 0L, 0)
            dao.updateDownloadState(downloadId, DownloadState.MERGING)

            val finishedHandle = sm.find(trackedHash)
            if (finishedHandle != null && finishedHandle.isValid) {
                finishedHandle.pause()
                sm.remove(finishedHandle)
            }

            val exportedFiles = exportStagingFiles(downloadId, torrentName, stagingDir)
            if (exportedFiles.isEmpty()) {
                dao.updateDownloadState(downloadId, DownloadState.FAILED)
                return@withContext DownloadState.FAILED
            }

            dao.deleteDownloadFiles(downloadId)
            dao.insertDownloadFiles(exportedFiles)
            val primaryFile = exportedFiles.first()
            dao.updateCompletedMeta(downloadId, primaryFile.uri, primaryFile.mimeType)
            dao.updateDownloadState(downloadId, DownloadState.COMPLETED)
            stagingDir.deleteRecursively()
            DownloadState.COMPLETED
        } catch (ce: CancellationException) {
            withContext(NonCancellable) {
                val trackedHash = infoHash
                val handle = if (trackedHash != null) sm.find(trackedHash) else null
                if (cancelledIds.contains(downloadId)) {
                    if (handle != null && handle.isValid) {
                        sm.remove(handle)
                    }
                    stagingDir.deleteRecursively()
                    dao.updateDownloadState(downloadId, DownloadState.CANCELLED)
                } else {
                    if (handle != null && handle.isValid) {
                        handle.pause()
                    }
                    dao.updateTorrentProgress(downloadId, lastKnownDone, 0L, 0)
                    dao.updateDownloadState(downloadId, DownloadState.PAUSED)
                }
            }
            throw ce
        } catch (_: Throwable) {
            withContext(NonCancellable) {
                val trackedHash = infoHash
                val handle = if (trackedHash != null) sm.find(trackedHash) else null
                if (handle != null && handle.isValid) {
                    sm.remove(handle)
                }
                dao.updateTorrentProgress(downloadId, lastKnownDone, 0L, 0)
                dao.updateDownloadState(downloadId, DownloadState.FAILED)
            }
            DownloadState.FAILED
        } finally {
            withContext(NonCancellable) {
                activeHashes.remove(downloadId)?.let { hash ->
                    hexToDownloadId.remove(hash.toHex())
                }
                finishedIds.remove(downloadId)
                metadataUpdates.remove(downloadId)
                errorMessages.remove(downloadId)
            }
        }
    }

    private fun exportStagingFiles(
        downloadId: Long,
        torrentName: String,
        stagingDir: File
    ): List<DownloadFileEntity> {
        val files = stagingDir.walkTopDown()
            .filter { it.isFile }
            .sortedBy { it.relativeTo(stagingDir).invariantSeparatorsPath }
            .toList()
        if (files.isEmpty()) return emptyList()

        val isMultiFile = files.size > 1
        val safeTorrentName = DownloadStorageProvider.sanitizeDisplayName(torrentName)
        val exported = ArrayList<DownloadFileEntity>(files.size)

        for (file in files) {
            val ext = file.extension.lowercase()
            val mimeType = if (ext.isNotEmpty()) {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
            } else {
                "application/octet-stream"
            }
            val relFile = file.relativeTo(stagingDir)
            val relPath = relFile.invariantSeparatorsPath
            val subfolder = if (!isMultiFile) {
                null
            } else {
                val relParent = relFile.parent
                    ?.replace('\\', '/')
                    ?.trim('/')
                    ?.takeIf { it.isNotBlank() }
                when {
                    relParent == null -> safeTorrentName
                    relParent == safeTorrentName || relParent.startsWith("$safeTorrentName/") -> relParent
                    else -> "$safeTorrentName/$relParent"
                }
            }

            val displayName = DownloadStorageProvider.sanitizeDisplayName(file.name)
            val destUri = storageProvider.createDestination(
                displayName = displayName,
                mimeType = mimeType,
                subfolder = subfolder
            )

            storageProvider.openWriteSession(
                uri = destUri,
                totalBytes = file.length(),
                alreadyDownloadedBytes = 0L,
                requestedWorkers = 1
            ).use { session ->
                FileInputStream(file).channel.use { src ->
                    var pos = 0L
                    val size = src.size()
                    while (pos < size) {
                        val transferred = src.transferTo(pos, size - pos, session.channel)
                        if (transferred <= 0L) break
                        pos += transferred
                    }
                }
                session.channel.force(false)
            }
            storageProvider.finalizePending(destUri)

            exported.add(
                DownloadFileEntity(
                    downloadId = downloadId,
                    name = displayName,
                    relativePath = relPath,
                    sizeBytes = file.length(),
                    mimeType = mimeType,
                    uri = destUri.toString()
                )
            )
        }
        return exported
    }

    fun pauseTorrent(downloadId: Long) {
        pausedIds.add(downloadId)
        val hash = activeHashes[downloadId] ?: return
        val sm = sessionManager ?: return
        val handle = sm.find(hash)
        if (handle != null && handle.isValid) {
            handle.pause()
        }
    }

    fun cancelTorrent(downloadId: Long) {
        cancelledIds.add(downloadId)
        val hash = activeHashes.remove(downloadId)
        if (hash != null) {
            hexToDownloadId.remove(hash.toHex())
            val sm = sessionManager
            val handle = sm?.find(hash)
            if (handle != null && handle.isValid) {
                sm.remove(handle)
            }
        }
        File(context.filesDir, "torrent_staging/$downloadId").deleteRecursively()
    }

    fun setBandwidthLimit(bytesPerSec: Long) {
        val clamped = bytesPerSec.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        currentRateLimit = clamped
        sessionManager?.let { sm ->
            if (sm.isRunning) {
                sm.downloadRateLimit(clamped)
            }
        }
    }
}
