package com.znarfm.siphon.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.znarfm.siphon.data.repository.BrowserPreferences
import com.znarfm.siphon.ui.AppScreen
import com.znarfm.siphon.ui.common.effectiveForDevice
import com.znarfm.siphon.ui.theme.AppColorSeed
import com.znarfm.siphon.ui.theme.AppThemeMode
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.SiphonIcons

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
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    selectedCategory: AppScreen? = null
) {
    val browserSummary = buildList {
        add(searchEngineDisplayName(browserPreferences.searchEngine))
        if (browserPreferences.webDarkMode) add("Web Dark")
        if (browserPreferences.desktopMode) add("Desktop")
    }.joinToString(" • ")

    val effectiveSeedTitle = colorSeed.effectiveForDevice().title
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
        showBackButton = showBackButton,
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
                SettingsCategoryCard(
                    icon = SiphonIcons.Public,
                    title = "Browser & Search",
                    subtitle = browserSummary,
                    selected = selectedCategory == AppScreen.SETTINGS_BROWSER,
                    onClick = onOpenBrowserSearch
                )
            }

            item(key = "category_appearance") {
                SettingsCategoryCard(
                    icon = SiphonIcons.Palette,
                    title = "Appearance",
                    subtitle = appearanceSummary,
                    selected = selectedCategory == AppScreen.SETTINGS_APPEARANCE,
                    onClick = onOpenAppearance
                )
            }

            item(key = "category_privacy_adblock") {
                SettingsCategoryCard(
                    icon = SiphonIcons.Shield,
                    title = "Privacy & Ad Blocker",
                    subtitle = privacySummary,
                    selected = selectedCategory == AppScreen.SETTINGS_ADBLOCK,
                    onClick = onOpenPrivacyAdblock
                )
            }

            item(key = "category_downloads_storage") {
                SettingsCategoryCard(
                    icon = SiphonIcons.Download,
                    title = "Downloads & Storage",
                    subtitle = downloadsSummary,
                    selected = selectedCategory == AppScreen.SETTINGS_DOWNLOADS,
                    onClick = onOpenDownloads
                )
            }

            item(key = "category_about_updates") {
                SettingsCategoryCard(
                    icon = SiphonIcons.SystemUpdate,
                    title = "About & Updates",
                    subtitle = if (updateAvailableTag != null) {
                        "Update available: $updateAvailableTag • v$versionName"
                    } else {
                        "v$versionName • by @znarfm"
                    },
                    selected = selectedCategory == AppScreen.SETTINGS_ABOUT,
                    onClick = onOpenAboutUpdates
                )
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = ExpressiveShapeTokens.RestingCardShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        tonalElevation = if (selected) 3.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        SettingsNavigationRow(
            icon = icon,
            title = title,
            subtitle = subtitle,
            titleColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            iconTint = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            },
            onClick = onClick
        )
    }
}
