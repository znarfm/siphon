package com.znarfm.siphon.core.service

import android.app.Notification
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.znarfm.siphon.SiphonApp
import com.znarfm.siphon.core.dash.DashQueueEngine
import com.znarfm.siphon.core.engine.ChunkQueueEngine
import com.znarfm.siphon.core.hls.HlsQueueEngine
import com.znarfm.siphon.core.storage.DownloadStorageProvider
import com.znarfm.siphon.data.db.DownloadDao
import com.znarfm.siphon.data.db.DownloadEntity
import com.znarfm.siphon.data.db.DownloadState
import com.znarfm.siphon.data.db.DownloadType
import com.znarfm.siphon.data.db.SiphonDatabase
import com.znarfm.siphon.data.prefs.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@OptIn(FlowPreview::class)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class DownloadUidtJobService : JobService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val progressState = MutableStateFlow<Map<Long, TransferProgress>>(emptyMap())

    private lateinit var dao: DownloadDao
    private lateinit var storage: DownloadStorageProvider
    private lateinit var engine: ChunkQueueEngine
    private lateinit var hlsEngine: HlsQueueEngine
    private lateinit var dashEngine: DashQueueEngine
    private lateinit var notificationManager: NotificationManager

    private var executionJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val container = (application as? SiphonApp)?.container
        dao = container?.downloadDao ?: SiphonDatabase.get(this).downloadDao()
        storage = container?.storageProvider ?: DownloadStorageProvider(this)

        val prefs = getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        val maxConcurrent = prefs.getInt(AppPreferences.PREF_MAX_CONCURRENT_DOWNLOADS, 2).coerceIn(1, 4)
        val bandwidthLimit = prefs.getLong(AppPreferences.PREF_BANDWIDTH_LIMIT_BYTES, 0L).coerceAtLeast(0L)

        engine = ChunkQueueEngine(
            dao = dao,
            maxConcurrentDownloads = maxConcurrent,
            rateBytesPerSec = bandwidthLimit
        )
        hlsEngine = HlsQueueEngine(
            context = this,
            dao = dao,
            rateBytesPerSec = bandwidthLimit
        )
        dashEngine = DashQueueEngine(
            context = this,
            dao = dao,
            rateBytesPerSec = bandwidthLimit
        )
        notificationManager = getSystemService(NotificationManager::class.java)
        DownloadNotificationFactory.ensureChannels(notificationManager, includeStatusChannel = false)
    }

    override fun onStartJob(params: JobParameters): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val initialNotification = buildNotification(emptyList())
            setNotification(params, UIDT_NOTIFICATION_ID, initialNotification, JOB_END_NOTIFICATION_POLICY_REMOVE)
        }

        val prefs = getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        val workers = prefs.getInt(AppPreferences.PREF_WORKERS_PER_DOWNLOAD, 6).coerceIn(1, 8)

        // Progress notification updater
        serviceScope.launch {
            progressState
                .sample(250L)
                .collect { activeMap ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        setNotification(
                            params,
                            UIDT_NOTIFICATION_ID,
                            buildNotification(activeMap.values.toList()),
                            JOB_END_NOTIFICATION_POLICY_REMOVE
                        )
                    }
                }
        }

        executionJob = serviceScope.launch(Dispatchers.IO) {
            try {
                val pendingDownloads = dao.getDownloadsByState(DownloadState.PAUSED) +
                    dao.getDownloadsByState(DownloadState.QUEUED) +
                    dao.getDownloadsByState(DownloadState.WAITING_FOR_NETWORK)

                if (pendingDownloads.isEmpty()) {
                    jobFinished(params, false)
                    return@launch
                }

                for (entity in pendingDownloads) {
                    executeDownload(entity, workers)
                }

                jobFinished(params, false)
            } catch (t: Throwable) {
                jobFinished(params, true)
            }
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        executionJob?.cancel()
        val runningIds = activeJobs.keys.toList()
        for (job in activeJobs.values) {
            job.cancel()
        }
        serviceScope.launch(Dispatchers.IO) {
            for (id in runningIds) {
                dao.updateDownloadState(id, DownloadState.PAUSED)
            }
        }
        return true
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun executeDownload(entity: DownloadEntity, workers: Int) {
        val downloadId = entity.id
        progressState.update { map ->
            map + (downloadId to TransferProgress(downloadId, entity.displayName, 0L, entity.totalBytes))
        }

        val job = serviceScope.launch(Dispatchers.IO) {
            try {
                if (entity.type == DownloadType.HLS) {
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
                } else if (entity.type == DownloadType.DASH) {
                    engine.withDownloadPermit {
                        dashEngine.executeWithStorage(
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
                        requestedWorkers = workers,
                        onProgress = { downloaded, total ->
                            progressState.update { map ->
                                val current = map[downloadId] ?: return@update map
                                map + (downloadId to current.copy(downloadedBytes = downloaded, totalBytes = total))
                            }
                        }
                    )
                }
            } finally {
                withContext(NonCancellable) {
                    activeJobs.remove(downloadId)
                    progressState.update { it - downloadId }
                }
            }
        }

        activeJobs[downloadId] = job
        job.join()
    }

    private fun buildNotification(items: List<TransferProgress>): Notification {
        return DownloadNotificationFactory.buildProgressNotification(
            context = this,
            items = items,
            emptyTitle = "Transferring downloads…",
            indeterminateFallbackText = "Downloading…"
        )
    }

    companion object {
        const val JOB_ID = 4100
        private const val UIDT_NOTIFICATION_ID = 4101

        fun scheduleFallback(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatching {
                    val jobScheduler = context.getSystemService(JobScheduler::class.java)
                    val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, DownloadUidtJobService::class.java))
                        .setUserInitiated(true)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .build()
                    jobScheduler.schedule(jobInfo)
                }
            }
        }
    }
}
