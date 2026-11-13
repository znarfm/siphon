package com.znarfm.siphon.data.repository

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.znarfm.siphon.core.service.DownloadForegroundService
import com.znarfm.siphon.core.updater.AppUpdater
import com.znarfm.siphon.core.updater.ReleaseInfo
import com.znarfm.siphon.core.updater.UpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateRepository(
    private val application: Application,
    private val appUpdater: AppUpdater = AppUpdater()
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val prefs = application.getSharedPreferences(
        DownloadForegroundService.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val packageInfo: PackageInfo by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.packageManager.getPackageInfo(
                application.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            application.packageManager.getPackageInfo(application.packageName, 0)
        }
    }

    val currentVersionName: String
        get() = packageInfo.versionName ?: "0.0.0"

    val currentVersionCode: Long
        get() = packageInfo.longVersionCode

    private val _autoUpdateEnabled = MutableStateFlow(
        prefs.getBoolean(PREF_AUTO_UPDATE_ENABLED, true)
    )
    val autoUpdateEnabled: StateFlow<Boolean> = _autoUpdateEnabled.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _startupPromptDismissed = MutableStateFlow(false)
    val startupPromptDismissed: StateFlow<Boolean> = _startupPromptDismissed.asStateFlow()

    private var hasCheckedThisSession = false
    private var lastKnownRelease: ReleaseInfo? = null

    fun setAutoUpdateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_UPDATE_ENABLED, enabled).apply()
        _autoUpdateEnabled.value = enabled
    }

    fun dismissStartupPrompt() {
        _startupPromptDismissed.value = true
    }

    fun checkForUpdates(silent: Boolean = false) {
        val current = _updateState.value
        if (current is UpdateState.Checking ||
            current is UpdateState.Downloading ||
            current is UpdateState.Verifying
        ) {
            return
        }
        if (silent && hasCheckedThisSession) {
            return
        }
        if (!silent) {
            _updateState.value = UpdateState.Checking
        }

        repoScope.launch(Dispatchers.IO) {
            hasCheckedThisSession = true
            appUpdater.checkForUpdate(currentVersion = currentVersionName)
                .onSuccess { release ->
                    if (release != null) {
                        lastKnownRelease = release
                        _startupPromptDismissed.value = false
                        _updateState.value = UpdateState.UpdateAvailable(release)
                    } else if (!silent) {
                        _updateState.value = UpdateState.UpToDate(currentVersionName)
                    } else if (_updateState.value !is UpdateState.UpdateAvailable &&
                        _updateState.value !is UpdateState.ReadyToInstall
                    ) {
                        _updateState.value = UpdateState.Idle
                    }
                }
                .onFailure { error ->
                    if (!silent) {
                        _updateState.value = UpdateState.Error(
                            message = error.message ?: "Failed to check for updates"
                        )
                    } else if (_updateState.value !is UpdateState.UpdateAvailable &&
                        _updateState.value !is UpdateState.ReadyToInstall
                    ) {
                        _updateState.value = UpdateState.Idle
                    }
                }
        }
    }

    fun downloadAndVerifyUpdate() {
        val current = _updateState.value
        if (current is UpdateState.Downloading || current is UpdateState.Verifying) {
            return
        }
        val release = when (current) {
            is UpdateState.UpdateAvailable -> current.release
            is UpdateState.Error -> lastKnownRelease
            else -> null
        } ?: return

        lastKnownRelease = release
        repoScope.launch(Dispatchers.IO) {
            _updateState.value = UpdateState.Downloading(
                release = release,
                progressPercent = 0,
                downloadedBytes = 0L,
                totalBytes = release.apkSizeBytes
            )
            appUpdater.downloadAndVerifyApk(
                cacheDir = application.cacheDir,
                release = release,
                onProgress = { percent, downloaded, total ->
                    if (percent >= 100 && total > 0L && downloaded >= total) {
                        _updateState.value = UpdateState.Verifying(release = release)
                    } else {
                        _updateState.value = UpdateState.Downloading(
                            release = release,
                            progressPercent = percent,
                            downloadedBytes = downloaded,
                            totalBytes = total
                        )
                    }
                }
            ).onSuccess { (apkFile, verifiedSha256) ->
                _updateState.value = UpdateState.ReadyToInstall(
                    release = release,
                    apkFile = apkFile,
                    verifiedSha256 = verifiedSha256
                )
            }.onFailure { error ->
                _updateState.value = UpdateState.Error(
                    message = error.message ?: "Failed to download update"
                )
            }
        }
    }

    fun installReadyApk(context: Context): Boolean {
        val state = _updateState.value as? UpdateState.ReadyToInstall ?: return false
        if (!appUpdater.canInstallPackages(context)) {
            val permissionIntent = appUpdater.createInstallPermissionIntent(context).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(permissionIntent)
            return false
        }
        val installIntent = appUpdater.createInstallIntent(context, state.apkFile)
        context.startActivity(installIntent)
        return true
    }

    companion object {
        const val PREF_AUTO_UPDATE_ENABLED = "pref_auto_update_enabled"
    }
}
