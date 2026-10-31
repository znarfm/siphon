package com.siphon.app.core.engine

import android.net.Uri
import com.siphon.app.core.network.HeadersCodec
import com.siphon.app.core.network.HttpStatusException
import com.siphon.app.core.network.MetadataProber
import com.siphon.app.core.network.NetworkClients
import com.siphon.app.core.storage.DownloadStorageProvider
import com.siphon.app.data.db.ChunkEntity
import com.siphon.app.data.db.ChunkSnapshot
import com.siphon.app.data.db.ChunkState
import com.siphon.app.data.db.DownloadDao
import com.siphon.app.data.db.DownloadEntity
import com.siphon.app.data.db.DownloadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

private const val MB = 1024L * 1024L
private const val BUFFER_SIZE = 64 * 1024
private const val PROGRESS_TICK_MS = 250L
private const val CHECKPOINT_TICKS = 10 // 10 * 250ms = 2500ms
private const val MAX_CHUNK_RETRIES = 3

class DynamicSemaphore(initialPermits: Int) {
    private val lock = Any()
    private var maxPermits = initialPermits.coerceIn(1, 4)
    private var availablePermits = maxPermits
    private val waiters = ArrayDeque<CancellableContinuation<Unit>>()

    suspend fun acquire() {
        suspendCancellableCoroutine<Unit> { cont ->
            synchronized(lock) {
                if (availablePermits > 0) {
                    availablePermits--
                    cont.resume(Unit)
                } else {
                    waiters.addLast(cont)
                    cont.invokeOnCancellation {
                        synchronized(lock) {
                            waiters.remove(cont)
                        }
                    }
                }
            }
        }
    }

    fun release() {
        var toResume: CancellableContinuation<Unit>? = null
        synchronized(lock) {
            while (waiters.isNotEmpty()) {
                val candidate = waiters.removeFirst()
                if (candidate.isActive) {
                    toResume = candidate
                    break
                }
            }
            if (toResume == null) {
                if (availablePermits < maxPermits) {
                    availablePermits++
                }
            }
        }
        toResume?.resume(Unit)
    }

    fun updateMaxPermits(newMax: Int) {
        val toResume = mutableListOf<CancellableContinuation<Unit>>()
        synchronized(lock) {
            val clamped = newMax.coerceIn(1, 4)
            val delta = clamped - maxPermits
            maxPermits = clamped
            if (delta > 0) {
                availablePermits += delta
                while (availablePermits > 0 && waiters.isNotEmpty()) {
                    val candidate = waiters.removeFirst()
                    if (candidate.isActive) {
                        availablePermits--
                        toResume.add(candidate)
                    }
                }
            }
        }
        for (cont in toResume) {
            cont.resume(Unit)
        }
    }

    suspend inline fun <T> withPermit(action: suspend () -> T): T {
        acquire()
        try {
            return action()
        } finally {
            release()
        }
    }
}

class ChunkQueueEngine(
    private val dao: DownloadDao,
    private val chunkClient: OkHttpClient = NetworkClients.chunkClient,
    maxConcurrentDownloads: Int = 2
) {

    private val downloadSemaphore = DynamicSemaphore(maxConcurrentDownloads.coerceIn(1, 4))

    fun updateMaxConcurrent(maxConcurrent: Int) {
        downloadSemaphore.updateMaxPermits(maxConcurrent)
    }

    suspend fun <T> withDownloadPermit(action: suspend () -> T): T {
        downloadSemaphore.acquire()
        try {
            return action()
        } finally {
            downloadSemaphore.release()
        }
    }

    private class ActiveChunk(
        val downloadId: Long,
        val trackIndex: Int,
        val chunkIndex: Int,
        val startOffset: Long,
        val endOffset: Long,
        initialOffset: Long,
        initialState: ChunkState
    ) {
        val currentOffset = AtomicLong(initialOffset)
        val state = AtomicReference(initialState)

        fun snapshot(): ChunkSnapshot = ChunkSnapshot(
            downloadId = downloadId,
            trackIndex = trackIndex,
            chunkIndex = chunkIndex,
            currentOffset = currentOffset.get(),
            state = state.get()
        )
    }

    /**
     * Allocates or re-opens the target [MediaStore] / SAF URI via [storage],
     * runs `StatFs` + FAT32 pre-flight, executes the download, and flips `IS_PENDING = 0`
     * on completion.
     */
    suspend fun executeWithStorage(
        downloadId: Long,
        storage: DownloadStorageProvider,
        customTreeUri: Uri? = null,
        requestedWorkers: Int = 6,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): DownloadState = downloadSemaphore.withPermit {
        withContext(Dispatchers.IO) {
            val download = dao.getDownload(downloadId)
                ?: return@withContext DownloadState.FAILED

            val destUri = if (!download.destinationUri.isNullOrBlank()) {
                Uri.parse(download.destinationUri).also { storage.refreshPendingExpiry(it) }
            } else {
                storage.createDestination(
                    displayName = download.displayName,
                    mimeType = download.mimeType,
                    customTreeUri = customTreeUri
                ).also { created ->
                    dao.updateDestinationUri(downloadId, created.toString())
                }
            }

            val alreadyDownloaded = dao.getAllChunks(downloadId).sumOf {
                (it.currentOffset - it.startOffset).coerceAtLeast(0L)
            }

            val state = try {
                storage.openWriteSession(
                    uri = destUri,
                    totalBytes = download.totalBytes,
                    alreadyDownloadedBytes = alreadyDownloaded,
                    requestedWorkers = requestedWorkers
                ).use { session ->
                    executeInternal(
                        downloadId = downloadId,
                        channel = session.channel,
                        workerCount = session.preflight.recommendedWorkers,
                        onProgress = onProgress
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ChunkQueueEngine", "Chunk download failed for id $downloadId: ${e.message}", e)
                dao.updateDownloadState(downloadId, DownloadState.FAILED)
                DownloadState.FAILED
            }

            if (state == DownloadState.COMPLETED) {
                storage.finalizePending(destUri)
            }
            state
        }
    }

    /**
     * Cancels a download, deletes its pending/partial MediaStore or SAF file, and purges credentials.
     */
    suspend fun cancelAndDelete(
        downloadId: Long,
        storage: DownloadStorageProvider,
        removeRecord: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val download = dao.getDownload(downloadId) ?: return@withContext
        storage.delete(download.destinationUri)
        if (removeRecord) {
            dao.deleteDownload(downloadId)
        } else {
            dao.deleteChunksForDownload(downloadId)
            dao.updateDestinationUri(downloadId, null)
            dao.markTerminalAndPurgeCredentials(downloadId, DownloadState.CANCELLED)
        }
    }

    private suspend fun executeInternal(
        downloadId: Long,
        channel: FileChannel,
        workerCount: Int = 6,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): DownloadState = withContext(Dispatchers.IO) {
            val download = dao.getDownload(downloadId)
                ?: return@withContext DownloadState.FAILED
            val headers = HeadersCodec.decode(download.headersJson)
            val clampedWorkers = workerCount.coerceIn(1, 8)

            dao.updateDownloadState(downloadId, DownloadState.ACTIVE)
            dao.resetInFlightChunksForDownload(downloadId)

            if (!download.isResumable || download.totalBytes <= 0L) {
                return@withContext runSingleStream(download, headers, channel, onProgress)
            }

            var existingChunks = dao.getAllChunks(downloadId)
            if (existingChunks.isEmpty()) {
                val generated = buildChunks(
                    downloadId = downloadId,
                    totalBytes = download.totalBytes,
                    workerCount = clampedWorkers,
                    isResumable = true
                )
                dao.insertChunks(generated)
                existingChunks = generated
            }

            val previouslyDoneBytes = existingChunks
                .filter { it.state == ChunkState.DONE }
                .sumOf { (it.endOffset - it.startOffset + 1).coerceAtLeast(0L) }

            val incompleteChunks = existingChunks
                .filter { it.state != ChunkState.DONE && it.state != ChunkState.MERGED }
                .map {
                    ActiveChunk(
                        downloadId = it.downloadId,
                        trackIndex = it.trackIndex,
                        chunkIndex = it.chunkIndex,
                        startOffset = it.startOffset,
                        endOffset = it.endOffset,
                        initialOffset = it.currentOffset,
                        initialState = ChunkState.PENDING
                    )
                }

            if (incompleteChunks.isEmpty()) {
                dao.markTerminalAndPurgeCredentials(downloadId, DownloadState.COMPLETED)
                return@withContext DownloadState.COMPLETED
            }

            val queue = ConcurrentLinkedQueue(incompleteChunks)
            val fallbackToSingleStream = AtomicBoolean(false)
            val terminalErrorState = AtomicReference<DownloadState?>(null)

            var wasCancelled = false
            try {
                coroutineScope {
                    val checkpointer = launchCheckpointer(
                        channel = channel,
                        chunks = incompleteChunks,
                        previouslyDoneBytes = previouslyDoneBytes,
                        totalBytes = download.totalBytes,
                        onProgress = onProgress
                    )

                    val workers = (0 until minOf(clampedWorkers, incompleteChunks.size)).map {
                        async {
                            runWorker(
                                download = download,
                                headers = headers,
                                channel = channel,
                                queue = queue,
                                fallbackToSingleStream = fallbackToSingleStream,
                                terminalErrorState = terminalErrorState
                            )
                        }
                    }

                    workers.awaitAll()
                    checkpointer.cancelAndJoin()
                }
            } catch (ce: CancellationException) {
                wasCancelled = true
                throw ce
            } finally {
                withContext(NonCancellable) {
                    if (!fallbackToSingleStream.get()) {
                        flushAndCheckpoint(channel, incompleteChunks)
                    }
                    if (wasCancelled) {
                        dao.updateDownloadState(downloadId, DownloadState.PAUSED)
                    }
                }
            }

            if (fallbackToSingleStream.get()) {
                return@withContext runSingleStream(download, headers, channel, onProgress)
            }

            val errState = terminalErrorState.get()
            val finalState = when {
                errState != null -> errState
                incompleteChunks.all { it.state.get() == ChunkState.DONE } -> DownloadState.COMPLETED
                else -> DownloadState.PAUSED
            }

            if (finalState == DownloadState.COMPLETED || finalState == DownloadState.CANCELLED) {
                dao.markTerminalAndPurgeCredentials(downloadId, finalState)
            } else {
                dao.updateDownloadState(downloadId, finalState)
            }
            finalState
        }

    private suspend fun runWorker(
        download: DownloadEntity,
        headers: Headers,
        channel: FileChannel,
        queue: ConcurrentLinkedQueue<ActiveChunk>,
        fallbackToSingleStream: AtomicBoolean,
        terminalErrorState: AtomicReference<DownloadState?>
    ) = coroutineScope {
        val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
        val byteArray = ByteArray(BUFFER_SIZE)

        while (isActive && !fallbackToSingleStream.get() && terminalErrorState.get() == null) {
            val chunk = queue.poll() ?: break
            chunk.state.set(ChunkState.IN_FLIGHT)

            var attempt = 0
            var completed = false

            while (isActive && !completed && attempt <= MAX_CHUNK_RETRIES) {
                try {
                    val resumeOffset = chunk.currentOffset.get()
                    if (resumeOffset > chunk.endOffset) {
                        chunk.state.set(ChunkState.DONE)
                        completed = true
                        break
                    }

                    val reqBuilder = Request.Builder()
                        .url(download.url)
                        .headers(headers)
                        .header("Range", "bytes=$resumeOffset-${chunk.endOffset}")

                    if (resumeOffset > chunk.startOffset) {
                        val validator = download.etag ?: download.lastModified
                        if (!validator.isNullOrBlank()) {
                            reqBuilder.header("If-Range", validator)
                        }
                    }

                    chunkClient.newCall(reqBuilder.build()).execute().use { resp ->
                        when (resp.code) {
                            200 -> {
                                // Server ignored Range or If-Range failed because remote file changed
                                fallbackToSingleStream.set(true)
                                return@coroutineScope
                            }
                            403, 410 -> {
                                chunk.state.set(ChunkState.PENDING)
                                terminalErrorState.compareAndSet(null, DownloadState.AUTH_EXPIRED)
                                return@coroutineScope
                            }
                            429, 503 -> {
                                val retryAfterSec = resp.header("Retry-After")?.toLongOrNull()
                                throw RetryableHttpException(resp.code, retryAfterSec)
                            }
                            206 -> {
                                val startFromHeader = MetadataProber.parseStartFromContentRange(
                                    resp.header("Content-Range")
                                )
                                if (startFromHeader != resumeOffset) {
                                    throw IOException(
                                        "Content-Range offset mismatch: expected $resumeOffset, got $startFromHeader"
                                    )
                                }
                                val stream = resp.body?.byteStream()
                                    ?: throw IOException("Empty response body on HTTP 206")

                                var currentPos = resumeOffset
                                val maxRemaining = chunk.endOffset - resumeOffset + 1
                                var bytesReadTotal = 0L

                                while (isActive && bytesReadTotal < maxRemaining) {
                                    val toRead = minOf(
                                        byteArray.size.toLong(),
                                        maxRemaining - bytesReadTotal
                                    ).toInt()
                                    val read = stream.read(byteArray, 0, toRead)
                                    if (read == -1) break

                                    buffer.clear()
                                    buffer.put(byteArray, 0, read)
                                    buffer.flip()
                                    while (buffer.hasRemaining()) {
                                        currentPos += channel.write(buffer, currentPos)
                                    }
                                    bytesReadTotal += read
                                    chunk.currentOffset.set(currentPos)
                                }

                                if (chunk.currentOffset.get() == chunk.endOffset + 1) {
                                    chunk.state.set(ChunkState.DONE)
                                    completed = true
                                } else if (isActive) {
                                    throw IOException(
                                        "Unexpected EOF at ${chunk.currentOffset.get()}/${chunk.endOffset + 1}"
                                    )
                                }
                            }
                            else -> throw HttpStatusException(resp.code, "HTTP ${resp.code}")
                        }
                    }
                } catch (ce: CancellationException) {
                    if (chunk.state.get() != ChunkState.DONE) {
                        chunk.state.set(ChunkState.PENDING)
                    }
                    throw ce
                } catch (e: RetryableHttpException) {
                    attempt++
                    if (attempt > MAX_CHUNK_RETRIES) {
                        chunk.state.set(ChunkState.PENDING)
                        terminalErrorState.compareAndSet(null, DownloadState.WAITING_FOR_NETWORK)
                        return@coroutineScope
                    }
                    val waitMs = (e.retryAfterSeconds?.times(1000L))
                        ?: (1000L shl (attempt - 1))
                    delay(waitMs.coerceAtMost(30_000L))
                } catch (e: HttpStatusException) {
                    chunk.state.set(ChunkState.PENDING)
                    terminalErrorState.compareAndSet(null, DownloadState.FAILED)
                    return@coroutineScope
                } catch (e: IOException) {
                    attempt++
                    if (attempt > MAX_CHUNK_RETRIES) {
                        chunk.state.set(ChunkState.PENDING)
                        terminalErrorState.compareAndSet(null, DownloadState.WAITING_FOR_NETWORK)
                        return@coroutineScope
                    }
                    delay(1000L shl (attempt - 1))
                }
            }
        }
    }

    /**
     * Single-stream fallback for non-resumable endpoints or when server returns HTTP 200 on resume.
     * Truncates [channel] to 0 and downloads in a single stream from offset 0.
     */
    private suspend fun runSingleStream(
        download: DownloadEntity,
        headers: Headers,
        channel: FileChannel,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)?
    ): DownloadState = coroutineScope {
        channel.truncate(0L)
        dao.deleteChunksForDownload(download.id)
        dao.updateResumable(download.id, false)

        val singleChunk = ChunkEntity(
            downloadId = download.id,
            trackIndex = 0,
            chunkIndex = 0,
            startOffset = 0L,
            currentOffset = 0L,
            endOffset = if (download.totalBytes > 0) download.totalBytes - 1 else -1L,
            state = ChunkState.IN_FLIGHT
        )
        dao.insertChunks(listOf(singleChunk))

        val active = ActiveChunk(
            downloadId = download.id,
            trackIndex = 0,
            chunkIndex = 0,
            startOffset = 0L,
            endOffset = singleChunk.endOffset,
            initialOffset = 0L,
            initialState = ChunkState.IN_FLIGHT
        )

        val checkpointer = launchCheckpointer(
            channel = channel,
            chunks = listOf(active),
            previouslyDoneBytes = 0L,
            totalBytes = download.totalBytes,
            onProgress = onProgress
        )

        var finalState = DownloadState.PAUSED
        try {
            val req = Request.Builder()
                .url(download.url)
                .headers(headers)
                .get()
                .build()

            chunkClient.newCall(req).execute().use { resp ->
                if (resp.code == 403 || resp.code == 410) {
                    finalState = DownloadState.AUTH_EXPIRED
                    return@use
                }
                if (!resp.isSuccessful) {
                    finalState = DownloadState.FAILED
                    return@use
                }
                val stream = resp.body?.byteStream() ?: throw IOException("Empty body")
                val byteArray = ByteArray(BUFFER_SIZE)
                val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
                var pos = 0L

                while (isActive) {
                    val read = stream.read(byteArray)
                    if (read == -1) break
                    buffer.clear()
                    buffer.put(byteArray, 0, read)
                    buffer.flip()
                    while (buffer.hasRemaining()) {
                        pos += channel.write(buffer, pos)
                    }
                    active.currentOffset.set(pos)
                }

                if (isActive && (download.totalBytes <= 0L || pos == download.totalBytes)) {
                    active.state.set(ChunkState.DONE)
                    finalState = DownloadState.COMPLETED
                }
            }
        } catch (ce: CancellationException) {
            active.state.set(ChunkState.PENDING)
            throw ce
        } catch (_: IOException) {
            active.state.set(ChunkState.PENDING)
            finalState = DownloadState.WAITING_FOR_NETWORK
        } finally {
            withContext(NonCancellable) {
                checkpointer.cancelAndJoin()
                flushAndCheckpoint(channel, listOf(active))
                if (finalState == DownloadState.COMPLETED) {
                    dao.markTerminalAndPurgeCredentials(download.id, finalState)
                } else {
                    dao.updateDownloadState(download.id, finalState)
                }
            }
        }
        finalState
    }

    private fun CoroutineScope.launchCheckpointer(
        channel: FileChannel,
        chunks: List<ActiveChunk>,
        previouslyDoneBytes: Long,
        totalBytes: Long,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)?
    ) = launch {
        var tick = 0
        while (isActive) {
            delay(PROGRESS_TICK_MS)
            tick++
            if (tick >= CHECKPOINT_TICKS) {
                tick = 0
                flushAndCheckpoint(channel, chunks)
            }
            if (onProgress != null) {
                val activeBytes = chunks.sumOf {
                    (it.currentOffset.get() - it.startOffset).coerceAtLeast(0L)
                }
                onProgress(previouslyDoneBytes + activeBytes, totalBytes)
            }
        }
    }

    /**
     * Ordering invariant:
     * 1. Snapshot all workers' AtomicLong offsets and DONE states BEFORE channel.force(false).
     * 2. Flush OS dirty pages to storage via channel.force(false).
     * 3. Commit the pre-force snapshot to SQLite AFTER force() returns.
     */
    private suspend fun flushAndCheckpoint(
        channel: FileChannel,
        chunks: List<ActiveChunk>
    ): List<ChunkSnapshot> {
        val preForceSnapshot = chunks.map { it.snapshot() }
        if (channel.isOpen) {
            channel.force(false)
        }
        dao.commitCheckpoints(preForceSnapshot)
        return preForceSnapshot
    }

    companion object {
        /**
         * Clamped adaptive chunk sizing:
         * - Non-resumable or unknown size -> single chunk (0..-1)
         * - Files < 24MB -> max(1MB, totalBytes / workerCount)
         * - Files >= 24MB -> clamp(totalBytes / 512, min = 4MB, max = 32MB)
         */
        fun buildChunks(
            downloadId: Long,
            totalBytes: Long,
            workerCount: Int = 6,
            isResumable: Boolean = true
        ): List<ChunkEntity> {
            if (!isResumable || totalBytes <= 0L) {
                return listOf(
                    ChunkEntity(
                        downloadId = downloadId,
                        trackIndex = 0,
                        chunkIndex = 0,
                        startOffset = 0L,
                        currentOffset = 0L,
                        endOffset = if (totalBytes > 0L) totalBytes - 1 else -1L,
                        state = ChunkState.PENDING
                    )
                )
            }

            val chunkSize = if (totalBytes < 24L * MB) {
                maxOf(1L * MB, totalBytes / workerCount.coerceAtLeast(1))
            } else {
                (totalBytes / 512L).coerceIn(4L * MB, 32L * MB)
            }

            val chunks = ArrayList<ChunkEntity>()
            var offset = 0L
            var index = 0
            while (offset < totalBytes) {
                val end = minOf(offset + chunkSize - 1, totalBytes - 1)
                chunks.add(
                    ChunkEntity(
                        downloadId = downloadId,
                        trackIndex = 0,
                        chunkIndex = index++,
                        startOffset = offset,
                        currentOffset = offset,
                        endOffset = end,
                        state = ChunkState.PENDING
                    )
                )
                offset = end + 1
            }
            return chunks
        }
    }
}

private class RetryableHttpException(
    val code: Int,
    val retryAfterSeconds: Long?
) : IOException("Retryable HTTP $code")
