package com.siphon.app.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.siphon.app.core.engine.ChunkQueueEngine
import com.siphon.app.core.hls.HlsQueueEngine
import com.siphon.app.core.storage.DownloadStorageProvider
import com.siphon.app.data.db.DownloadDao
import com.siphon.app.data.db.DownloadState
import com.siphon.app.data.db.DownloadType
import com.siphon.app.data.db.SiphonDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

data class TransferProgress(
    val downloadId: Long,
    val title: String,
    val downloadedBytes: Long,
    val totalBytes: Long
)

@OptIn(FlowPreview::class)
class DownloadForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val progressState = MutableStateFlow<Map<Long, TransferProgress>>(emptyMap())

    private lateinit var dao: DownloadDao
    private lateinit var storage: DownloadStorageProvider
    private lateinit var engine: ChunkQueueEngine
    private lateinit var hlsEngine: HlsQueueEngine
    private lateinit var notificationManager: NotificationManager
    private lateinit var connectivityManager: ConnectivityManager
    private var currentWorkers: Int = 6

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                serviceScope.launch(Dispatchers.IO) {
                    val waiting = dao.getDownloadsByState(DownloadState.WAITING_FOR_NETWORK)
                    for (item in waiting) {
                        startOrResumeDownload(item.id)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val initialMaxConcurrent = prefs.getInt(PREF_MAX_CONCURRENT_DOWNLOADS, 2).coerceIn(1, 4)
        currentWorkers = prefs.getInt(PREF_WORKERS_PER_DOWNLOAD, 6).coerceIn(1, 8)

        dao = SiphonDatabase.get(this).downloadDao()
        storage = DownloadStorageProvider(this)
        engine = ChunkQueueEngine(dao = dao, maxConcurrentDownloads = initialMaxConcurrent)
        hlsEngine = HlsQueueEngine(context = this, dao = dao)
        notificationManager = getSystemService(NotificationManager::class.java)
        connectivityManager = getSystemService(ConnectivityManager::class.java)

        ensureNotificationChannels()
        runCatching { connectivityManager.registerDefaultNetworkCallback(networkCallback) }

        serviceScope.launch(Dispatchers.IO) {
            dao.resetInFlightChunksOnBoot()
        }

        serviceScope.launch {
            progressState
                .sample(250L)
                .collect { activeMap ->
                    if (activeMap.isNotEmpty()) {
                        notificationManager.notify(
                            FOREGROUND_NOTIFICATION_ID,
                            buildForegroundNotification(activeMap.values.toList())
                        )
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val downloadId = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1L) ?: -1L

        if (intent?.hasExtra(EXTRA_MAX_CONCURRENT) == true) {
            val maxD = intent.getIntExtra(EXTRA_MAX_CONCURRENT, 2).coerceIn(1, 4)
            engine.updateMaxConcurrent(maxD)
        }
        if (intent?.hasExtra(EXTRA_WORKERS) == true) {
            currentWorkers = intent.getIntExtra(EXTRA_WORKERS, 6).coerceIn(1, 8)
        }

        when (action) {
            ACTION_UPDATE_CONCURRENCY -> {
                // Settings applied via extras above
            }
            ACTION_START_OR_RESUME -> {
                promoteToForeground()
                if (downloadId > 0L) {
                    startOrResumeDownload(downloadId)
                }
            }
            ACTION_PAUSE -> {
                if (downloadId > 0L) {
                    pauseDownload(downloadId)
                }
            }
            ACTION_CANCEL -> {
                if (downloadId > 0L) {
                    cancelDownload(downloadId)
                }
            }
            ACTION_PAUSE_ALL -> {
                pauseAllDownloads(postTimeoutResumeNotif = false, stopStartId = startId)
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Android 14 (API 34) FGS timeout callback.
     */
    override fun onTimeout(startId: Int) {
        pauseAllDownloads(postTimeoutResumeNotif = true, stopStartId = startId)
    }

    /**
     * Android 15 (API 35) FGS timeout callback (6-hour dataSync limit).
     * Flushes dirty OS pages via `channel.force(false)`, commits chunk offsets to Room,
     * marks tasks PAUSED, and posts a tap-to-resume notification before stopping.
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        pauseAllDownloads(postTimeoutResumeNotif = true, stopStartId = startId)
    }

    override fun onDestroy() {
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun promoteToForeground() {
        val notification = buildForegroundNotification(progressState.value.values.toList())
        ServiceCompat.startForeground(
            this,
            FOREGROUND_NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )
    }

    private fun startOrResumeDownload(downloadId: Long) {
        if (activeJobs.containsKey(downloadId)) return

        val job = serviceScope.launch(Dispatchers.IO) {
            val entity = dao.getDownload(downloadId) ?: return@launch
            progressState.update { map ->
                map + (downloadId to TransferProgress(downloadId, entity.displayName, 0L, entity.totalBytes))
            }

            try {
                val finalState = if (entity.type == DownloadType.HLS) {
                    engine.withDownloadPermit {
                        hlsEngine.executeWithStorage(
                            downloadId = downloadId,
                            storage = storage,
                            onProgress = { downloaded, total ->
                                progressState.update { map ->
                                    val current = map[downloadId] ?: return@update map
                                    map + (downloadId to current.copy(downloadedBytes = downloaded, totalBytes = total))
                                }
                            }
                        )
                    }
                } else {
                    engine.executeWithStorage(
                        downloadId = downloadId,
                        storage = storage,
                        requestedWorkers = currentWorkers,
                        onProgress = { downloaded, total ->
                            progressState.update { map ->
                                val current = map[downloadId] ?: return@update map
                                map + (downloadId to current.copy(downloadedBytes = downloaded, totalBytes = total))
                            }
                        }
                    )
                }
                postTerminalStatusNotification(entity.id, entity.displayName, finalState)
            } finally {
                withContext(NonCancellable) {
                    activeJobs.remove(downloadId)
                    progressState.update { it - downloadId }
                    stopForegroundIfIdle()
                }
            }
        }
        activeJobs[downloadId] = job
    }

    private fun pauseDownload(downloadId: Long) {
        serviceScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                activeJobs.remove(downloadId)?.cancelAndJoin()
                dao.updateDownloadState(downloadId, DownloadState.PAUSED)
                val entity = dao.getDownload(downloadId)
                if (entity != null) {
                    postResumeNotification(entity.id, entity.displayName, isTimeout = false)
                }
                stopForegroundIfIdle()
            }
        }
    }

    private fun cancelDownload(downloadId: Long) {
        serviceScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                activeJobs.remove(downloadId)?.cancelAndJoin()
                val entity = dao.getDownload(downloadId)
                if (entity?.type == DownloadType.HLS) {
                    hlsEngine.cancelAndDelete(downloadId, storage)
                } else {
                    engine.cancelAndDelete(downloadId, storage)
                }
                notificationManager.cancel(statusNotificationId(downloadId))
                stopForegroundIfIdle()
            }
        }
    }

    private fun pauseAllDownloads(postTimeoutResumeNotif: Boolean, stopStartId: Int) {
        serviceScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                val runningIds = activeJobs.keys.toList()
                val jobs = runningIds.mapNotNull { activeJobs.remove(it) }

                withTimeoutOrNull(2500L) {
                    for (job in jobs) {
                        job.cancelAndJoin()
                    }
                }

                for (id in runningIds) {
                    dao.updateDownloadState(id, DownloadState.PAUSED)
                    if (postTimeoutResumeNotif) {
                        val entity = dao.getDownload(id)
                        if (entity != null) {
                            postResumeNotification(entity.id, entity.displayName, isTimeout = true)
                        }
                    }
                }
                progressState.value = emptyMap()
                ServiceCompat.stopForeground(this@DownloadForegroundService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf(stopStartId)
            }
        }
    }

    private fun stopForegroundIfIdle() {
        if (activeJobs.isEmpty()) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildForegroundNotification(items: List<TransferProgress>): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ACTIVE)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        if (items.isEmpty()) {
            return builder
                .setContentTitle("Preparing download…")
                .setProgress(0, 0, true)
                .build()
        }

        val primary = items.first()
        val totalDownloaded = items.sumOf { it.downloadedBytes }
        val totalSize = items.sumOf { it.totalBytes.coerceAtLeast(0L) }
        val title = if (items.size == 1) {
            primary.title
        } else {
            "${items.size} active downloads"
        }

        builder.setContentTitle(title)
        if (totalSize > 0L) {
            val pct = ((totalDownloaded * 100L) / totalSize).toInt().coerceIn(0, 100)
            builder.setProgress(100, pct, false)
            builder.setContentText("$pct% • ${formatBytes(totalDownloaded)} / ${formatBytes(totalSize)}")
        } else {
            builder.setProgress(0, 0, true)
            builder.setContentText(formatBytes(totalDownloaded))
        }

        if (items.size == 1) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                buildServicePendingIntent(ACTION_PAUSE, primary.downloadId, reqOffset = 1)
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                buildServicePendingIntent(ACTION_CANCEL, primary.downloadId, reqOffset = 2)
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Pause All",
                buildServicePendingIntent(ACTION_PAUSE_ALL, 0L, reqOffset = 3)
            )
        }

        return builder.build()
    }

    private fun postResumeNotification(downloadId: Long, displayName: String, isTimeout: Boolean) {
        val resumeIntent = buildServicePendingIntent(ACTION_START_OR_RESUME, downloadId, reqOffset = 4)
        val cancelIntent = buildServicePendingIntent(ACTION_CANCEL, downloadId, reqOffset = 5)
        val subtitle = if (isTimeout) {
            "Paused by background limit — tap Resume to continue"
        } else {
            "Download paused"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(displayName)
            .setContentText(subtitle)
            .setAutoCancel(true)
            .setContentIntent(resumeIntent)
            .addAction(android.R.drawable.ic_media_play, "Resume", resumeIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()

        notificationManager.notify(statusNotificationId(downloadId), notification)
    }

    private fun postTerminalStatusNotification(
        downloadId: Long,
        displayName: String,
        state: DownloadState
    ) {
        if (state == DownloadState.PAUSED || state == DownloadState.CANCELLED) return
        val builder = NotificationCompat.Builder(this, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(displayName)
            .setAutoCancel(true)

        when (state) {
            DownloadState.COMPLETED -> builder.setContentText("Download complete")
            DownloadState.AUTH_EXPIRED -> builder.setContentText("Link expired (HTTP 403/410) — open app to refresh link")
            DownloadState.WAITING_FOR_NETWORK -> {
                val resumeIntent = buildServicePendingIntent(ACTION_START_OR_RESUME, downloadId, reqOffset = 4)
                builder.setContentText("Waiting for network…")
                    .addAction(android.R.drawable.ic_media_play, "Retry", resumeIntent)
            }
            else -> builder.setContentText("Download failed")
        }

        notificationManager.notify(statusNotificationId(downloadId), builder.build())
    }

    private fun buildServicePendingIntent(
        action: String,
        downloadId: Long,
        reqOffset: Int
    ): PendingIntent {
        val intent = Intent(this, DownloadForegroundService::class.java).apply {
            this.action = action
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        val requestCode = ((downloadId.hashCode() * 31) + reqOffset) and 0x7FFFFFFF
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (action == ACTION_START_OR_RESUME && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, requestCode, intent, flags)
        } else {
            PendingIntent.getService(this, requestCode, intent, flags)
        }
    }

    private fun ensureNotificationChannels() {
        val activeChannel = NotificationChannel(
            CHANNEL_ACTIVE,
            "Active Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Live progress for active downloads"
            setShowBadge(false)
        }
        val statusChannel = NotificationChannel(
            CHANNEL_STATUS,
            "Download Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Completed, paused, and failed download notifications"
        }
        notificationManager.createNotificationChannels(listOf(activeChannel, statusChannel))
    }

    private fun statusNotificationId(downloadId: Long): Int =
        STATUS_NOTIFICATION_BASE_ID + (downloadId.toInt() and 0x0FFFFFFF)

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "${bytes}B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format("%.1fKB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format("%.1fMB", mb)
        val gb = mb / 1024.0
        return String.format("%.2fGB", gb)
    }

    companion object {
        const val PREFS_NAME = "siphon_prefs"
        const val PREF_MAX_CONCURRENT_DOWNLOADS = "max_concurrent_downloads"
        const val PREF_WORKERS_PER_DOWNLOAD = "workers_per_download"

        const val ACTION_START_OR_RESUME = "com.siphon.app.action.START_OR_RESUME"
        const val ACTION_PAUSE = "com.siphon.app.action.PAUSE"
        const val ACTION_CANCEL = "com.siphon.app.action.CANCEL"
        const val ACTION_PAUSE_ALL = "com.siphon.app.action.PAUSE_ALL"
        const val ACTION_UPDATE_CONCURRENCY = "com.siphon.app.action.UPDATE_CONCURRENCY"

        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
        const val EXTRA_MAX_CONCURRENT = "extra_max_concurrent"
        const val EXTRA_WORKERS = "extra_workers"

        private const val CHANNEL_ACTIVE = "siphon_active_downloads"
        private const val CHANNEL_STATUS = "siphon_download_status"
        private const val FOREGROUND_NOTIFICATION_ID = 1001
        private const val STATUS_NOTIFICATION_BASE_ID = 2000

        fun updateConcurrency(context: Context, maxConcurrent: Int, workers: Int) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_UPDATE_CONCURRENCY
                putExtra(EXTRA_MAX_CONCURRENT, maxConcurrent.coerceIn(1, 4))
                putExtra(EXTRA_WORKERS, workers.coerceIn(1, 8))
            }
            context.startService(intent)
        }

        fun startOrResume(
            context: Context,
            downloadId: Long,
            maxConcurrent: Int? = null,
            workers: Int? = null
        ) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_START_OR_RESUME
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                if (maxConcurrent != null) putExtra(EXTRA_MAX_CONCURRENT, maxConcurrent.coerceIn(1, 4))
                if (workers != null) putExtra(EXTRA_WORKERS, workers.coerceIn(1, 8))
            }
            context.startForegroundService(intent)
        }

        fun pause(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }

        fun cancel(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }
    }
}
