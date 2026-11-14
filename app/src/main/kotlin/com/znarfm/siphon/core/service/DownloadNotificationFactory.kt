package com.znarfm.siphon.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.znarfm.siphon.core.common.FormatUtils
import com.znarfm.siphon.data.db.DownloadState
import com.znarfm.siphon.ui.MainActivity

internal object DownloadNotificationFactory {

    const val CHANNEL_ACTIVE = "siphon_active_downloads"
    const val CHANNEL_STATUS = "siphon_download_status"
    private const val STATUS_NOTIFICATION_BASE_ID = 2000

    fun ensureChannels(
        notificationManager: NotificationManager,
        includeStatusChannel: Boolean = true
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val activeChannel = NotificationChannel(
            CHANNEL_ACTIVE,
            "Active Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Live progress for active downloads"
            setShowBadge(false)
        }
        if (!includeStatusChannel) {
            notificationManager.createNotificationChannel(activeChannel)
            return
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

    fun statusNotificationId(downloadId: Long): Int =
        STATUS_NOTIFICATION_BASE_ID + (downloadId.toInt() and 0x0FFFFFFF)

    fun buildProgressNotification(
        context: Context,
        items: List<TransferProgress>,
        emptyTitle: String,
        indeterminateFallbackText: String,
        configureEmptyActions: (NotificationCompat.Builder.() -> Unit)? = null,
        configureActiveActions: (NotificationCompat.Builder.(List<TransferProgress>) -> Unit)? = null
    ): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ACTIVE)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(buildMainActivityPendingIntent(context))

        if (items.isEmpty()) {
            builder
                .setContentTitle(emptyTitle)
                .setProgress(0, 0, true)
            configureEmptyActions?.invoke(builder)
            return builder.build()
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
            builder.setContentText(
                "$pct% • ${FormatUtils.formatBytes(totalDownloaded)} / ${FormatUtils.formatBytes(totalSize)}"
            )
        } else {
            builder.setProgress(0, 0, true)
            builder.setContentText(
                if (totalDownloaded > 0L) FormatUtils.formatBytes(totalDownloaded) else indeterminateFallbackText
            )
        }

        configureActiveActions?.invoke(builder, items)
        return builder.build()
    }

    fun buildResumeNotification(
        context: Context,
        displayName: String,
        isTimeout: Boolean,
        resumeIntent: PendingIntent,
        cancelIntent: PendingIntent
    ): Notification {
        val subtitle = if (isTimeout) {
            "Paused by background limit — tap Resume to continue"
        } else {
            "Download paused"
        }
        return NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(displayName)
            .setContentText(subtitle)
            .setAutoCancel(true)
            .setContentIntent(resumeIntent)
            .addAction(android.R.drawable.ic_media_play, "Resume", resumeIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()
    }

    fun buildTerminalStatusNotification(
        context: Context,
        displayName: String,
        state: DownloadState,
        retryIntent: PendingIntent
    ): Notification? {
        if (state == DownloadState.PAUSED || state == DownloadState.CANCELLED) return null
        val builder = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(displayName)
            .setAutoCancel(true)

        when (state) {
            DownloadState.COMPLETED -> builder.setContentText("Download complete")
            DownloadState.AUTH_EXPIRED -> builder.setContentText("Link expired (HTTP 403/410) — open app to refresh link")
            DownloadState.WAITING_FOR_NETWORK -> {
                builder.setContentText("Waiting for network…")
                    .addAction(android.R.drawable.ic_media_play, "Retry", retryIntent)
            }
            else -> builder.setContentText("Download failed")
        }

        return builder.build()
    }

    private fun buildMainActivityPendingIntent(context: Context): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
