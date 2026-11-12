package com.siphon.app.core.service

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.siphon.app.data.db.DownloadDao
import com.siphon.app.data.db.DownloadState
import com.siphon.app.data.db.SiphonDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SiphonTileService : TileService() {

    private var serviceScope: CoroutineScope? = null
    private var listeningJob: Job? = null
    private lateinit var dao: DownloadDao

    override fun onCreate() {
        super.onCreate()
        dao = SiphonDatabase.get(this).downloadDao()
    }

    override fun onStartListening() {
        super.onStartListening()
        val scope = CoroutineScope(Dispatchers.Main.immediate)
        serviceScope = scope

        listeningJob = scope.launch {
            dao.observeDownloads().collect { downloads ->
                val active = downloads.count {
                    it.state == DownloadState.ACTIVE ||
                        it.state == DownloadState.QUEUED ||
                        it.state == DownloadState.MERGING
                }
                val paused = downloads.count {
                    it.state == DownloadState.PAUSED ||
                        it.state == DownloadState.WAITING_FOR_NETWORK
                }

                val tile = qsTile ?: return@collect
                if (active > 0) {
                    tile.state = Tile.STATE_ACTIVE
                    tile.label = "Siphon ($active active)"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tile.subtitle = "Tap to pause all"
                    }
                    tile.updateTile()
                } else if (paused > 0) {
                    tile.state = Tile.STATE_INACTIVE
                    tile.label = "Siphon ($paused paused)"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tile.subtitle = "Tap to resume all"
                    }
                    tile.updateTile()
                } else {
                    tile.state = Tile.STATE_INACTIVE
                    tile.label = "Siphon Downloads"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tile.subtitle = "Idle"
                    }
                    tile.updateTile()
                }
            }
        }
    }

    override fun onStopListening() {
        listeningJob?.cancel()
        listeningJob = null
        serviceScope?.cancel()
        serviceScope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val scope = serviceScope ?: CoroutineScope(Dispatchers.Main.immediate)
        scope.launch(Dispatchers.IO) {
            val downloads = dao.observeDownloads().first()
            val hasActive = downloads.any {
                it.state == DownloadState.ACTIVE ||
                    it.state == DownloadState.QUEUED ||
                    it.state == DownloadState.MERGING
            }
            if (hasActive) {
                DownloadForegroundService.pauseAll(this@SiphonTileService)
            } else {
                val hasPaused = downloads.any {
                    it.state == DownloadState.PAUSED ||
                        it.state == DownloadState.QUEUED ||
                        it.state == DownloadState.WAITING_FOR_NETWORK
                }
                if (hasPaused) {
                    DownloadForegroundService.resumeAll(this@SiphonTileService)
                }
            }
        }
    }

    companion object {
        fun requestTileUpdate(context: Context) {
            runCatching {
                requestListeningState(context, ComponentName(context, SiphonTileService::class.java))
            }
        }
    }
}
