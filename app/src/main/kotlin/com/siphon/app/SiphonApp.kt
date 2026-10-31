package com.siphon.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.siphon.app.core.adblock.FastHostFilter
import com.siphon.app.core.storage.DownloadStorageProvider
import com.siphon.app.data.db.SiphonDatabase
import com.siphon.app.data.repository.BrowserRepository
import com.siphon.app.data.repository.DownloadRepository
import com.siphon.app.ui.browser.BrowserViewModel
import com.siphon.app.ui.queue.QueueViewModel

class AppContainer(val application: Application) {
    val database: SiphonDatabase by lazy {
        SiphonDatabase.get(application)
    }

    val downloadStorageProvider: DownloadStorageProvider by lazy {
        DownloadStorageProvider(application)
    }

    val fastHostFilter: FastHostFilter by lazy {
        FastHostFilter().apply {
            loadFromAssets(application)
        }
    }

    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(
            application = application,
            dao = database.downloadDao(),
            storage = downloadStorageProvider
        )
    }

    val browserRepository: BrowserRepository by lazy {
        BrowserRepository(
            application = application,
            browserDao = database.browserDao(),
            hostFilter = fastHostFilter
        )
    }

    val viewModelFactory: ViewModelProvider.Factory by lazy {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(BrowserViewModel::class.java) -> {
                        BrowserViewModel(
                            browserRepository = browserRepository,
                            downloadRepository = downloadRepository
                        ) as T
                    }
                    modelClass.isAssignableFrom(QueueViewModel::class.java) -> {
                        QueueViewModel(
                            downloadRepository = downloadRepository
                        ) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

class SiphonApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val activeChannel = NotificationChannel(
                "siphon_active_downloads",
                "Active Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live progress for active downloads"
                setShowBadge(false)
            }
            val statusChannel = NotificationChannel(
                "siphon_download_status",
                "Download Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Completed, paused, and failed download notifications"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannels(listOf(activeChannel, statusChannel))
        }
    }

    companion object {
        lateinit var instance: SiphonApp
            private set
    }
}
