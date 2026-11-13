package com.znarfm.siphon.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.znarfm.siphon.core.updater.UpdateState
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.TabularNumberStyle

@Composable
fun AboutUpdatesSettingsScreen(
    versionName: String,
    versionCode: Long,
    autoUpdateEnabled: Boolean,
    updateState: UpdateState,
    onToggleAutoUpdate: (Boolean) -> Unit,
    onCheckForUpdates: () -> Unit,
    onDownloadAndVerify: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenAuthorProfile: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBusy = updateState is UpdateState.Checking ||
        updateState is UpdateState.Downloading ||
        updateState is UpdateState.Verifying

    SettingsSubPageScaffold(
        title = "About & Updates",
        onBack = onBack,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "section_app_info") {
                SettingsSectionHeader(
                    icon = Icons.Rounded.Info,
                    title = "App Info"
                )
            }

            item(key = "card_app_info") {
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Siphon",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Version v$versionName ($versionCode)",
                                style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        SettingsNavigationRow(
                            icon = Icons.Rounded.Person,
                            title = "Author: @znarfm",
                            subtitle = "github.com/znarfm • Tap to open profile",
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = onOpenAuthorProfile
                        )
                    }
                }
            }

            item(key = "section_updates") {
                SettingsSectionHeader(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "Updates"
                )
            }

            item(key = "card_updates") {
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsSwitchRow(
                            title = "Auto-check for updates",
                            subtitle = "Check GitHub Releases for new versions on startup",
                            checked = autoUpdateEnabled,
                            onCheckedChange = onToggleAutoUpdate,
                            icon = Icons.Rounded.Refresh
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Release Channel: GitHub",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = onCheckForUpdates,
                                    enabled = !isBusy,
                                    shape = ExpressiveShapeTokens.PillShape
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Check for updates")
                                }
                            }

                            UpdateStatusContent(
                                versionName = versionName,
                                updateState = updateState,
                                onCheckForUpdates = onCheckForUpdates,
                                onDownloadAndVerify = onDownloadAndVerify,
                                onInstallUpdate = onInstallUpdate
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateStatusContent(
    versionName: String,
    updateState: UpdateState,
    onCheckForUpdates: () -> Unit,
    onDownloadAndVerify: () -> Unit,
    onInstallUpdate: () -> Unit
) {
    when (updateState) {
        is UpdateState.Idle,
        is UpdateState.UpToDate -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Up to date (v$versionName)",
                    style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        is UpdateState.Checking -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp
                )
                Text(
                    text = "Checking GitHub Releases...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        is UpdateState.UpdateAvailable -> {
            val release = updateState.release
            val shaPreview = release.expectedSha256?.take(16)?.plus("...") ?: "Missing"
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Update available: ${release.tagName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (release.releaseNotes.isNotBlank()) {
                    Text(
                        text = release.releaseNotes.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "Expected SHA-256: $shaPreview",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                FilledTonalButton(
                    onClick = onDownloadAndVerify,
                    shape = ExpressiveShapeTokens.PillShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download & Verify")
                }
            }
        }

        is UpdateState.Downloading -> {
            val progressFraction = (updateState.progressPercent / 100f).coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Downloading ${updateState.release.tagName} (${updateState.progressPercent}%)",
                    style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.onSurface
                )
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        is UpdateState.Verifying -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Verifying SHA-256 checksum...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        is UpdateState.ReadyToInstall -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "SHA-256 verified: ${updateState.verifiedSha256.take(16)}...",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledTonalButton(
                    onClick = onInstallUpdate,
                    shape = ExpressiveShapeTokens.PillShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.InstallMobile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Install ${updateState.release.tagName}")
                }
            }
        }

        is UpdateState.Error -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = updateState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                FilledTonalButton(
                    onClick = onCheckForUpdates,
                    shape = ExpressiveShapeTokens.PillShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry Check")
                }
            }
        }
    }
}
