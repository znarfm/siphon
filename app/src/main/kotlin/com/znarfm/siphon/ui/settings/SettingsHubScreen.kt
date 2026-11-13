package com.znarfm.siphon.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.znarfm.siphon.data.repository.BrowserPreferences
import com.znarfm.siphon.ui.theme.AppColorSeed
import com.znarfm.siphon.ui.theme.AppThemeMode
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens

@Composable
fun SettingsHubScreen(
    browserPreferences: BrowserPreferences,
    themeMode: AppThemeMode,
    colorSeed: AppColorSeed,
    isAdBlockEnabled: Boolean,
    adblockRuleCount: Int,
    enabledListCount: Int,
    maxConcurrentDownloads: Int,
    workersPerDownload: Int,
    bandwidthLimit: Long,
    versionName: String,
    updateAvailableTag: String?,
    onOpenBrowserSearch: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenPrivacyAdblock: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAboutUpdates: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val browserSummary = buildList {
        add(searchEngineDisplayName(browserPreferences.searchEngine))
        if (browserPreferences.webDarkMode) add("Web Dark")
        if (browserPreferences.desktopMode) add("Desktop")
    }.joinToString(" • ")

    val effectiveSeedTitle = if (
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
        colorSeed == AppColorSeed.DYNAMIC
    ) {
        AppColorSeed.SAPPHIRE.title
    } else {
        colorSeed.title
    }
    val appearanceSummary = "${themeMode.title} • $effectiveSeedTitle"

    val privacySummary = if (isAdBlockEnabled) {
        "Enabled • $adblockRuleCount rules • $enabledListCount lists"
    } else {
        "Disabled"
    }

    val speedLimitLabel = if (bandwidthLimit <= 0L) {
        "Unlimited"
    } else {
        "${(bandwidthLimit / (1024L * 1024L))} MB/s"
    }
    val downloadsSummary = "$maxConcurrentDownloads concurrent • $workersPerDownload workers • $speedLimitLabel"

    SettingsSubPageScaffold(
        title = "Settings",
        onBack = onBack,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "category_browser_search") {
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsNavigationRow(
                        icon = Icons.Rounded.Public,
                        title = "Browser & Search",
                        subtitle = browserSummary,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onOpenBrowserSearch
                    )
                }
            }

            item(key = "category_appearance") {
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsNavigationRow(
                        icon = Icons.Rounded.Palette,
                        title = "Appearance",
                        subtitle = appearanceSummary,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onOpenAppearance
                    )
                }
            }

            item(key = "category_privacy_adblock") {
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsNavigationRow(
                        icon = Icons.Rounded.Shield,
                        title = "Privacy & Ad Blocker",
                        subtitle = privacySummary,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onOpenPrivacyAdblock
                    )
                }
            }

            item(key = "category_downloads_storage") {
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsNavigationRow(
                        icon = Icons.Rounded.Download,
                        title = "Downloads & Storage",
                        subtitle = downloadsSummary,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onOpenDownloads
                    )
                }
            }

            item(key = "category_about_updates") {
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsNavigationRow(
                        icon = Icons.Rounded.SystemUpdate,
                        title = "About & Updates",
                        subtitle = if (updateAvailableTag != null) {
                            "Update available: $updateAvailableTag • v$versionName"
                        } else {
                            "v$versionName • by @znarfm"
                        },
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onOpenAboutUpdates
                    )
                }
            }
        }
    }
}
