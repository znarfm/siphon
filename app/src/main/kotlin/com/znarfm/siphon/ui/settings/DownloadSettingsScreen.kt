package com.znarfm.siphon.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.TabularNumberStyle
import kotlin.math.roundToInt

@Composable
fun DownloadSettingsScreen(
    currentMaxDownloads: Int,
    currentWorkers: Int,
    currentBandwidthLimit: Long = 0L,
    currentWifiOnly: Boolean = false,
    currentCustomDownloadUri: String? = null,
    onUpdateConcurrency: (
        maxDownloads: Int,
        workers: Int,
        bandwidthLimit: Long,
        wifiOnly: Boolean,
        customDownloadUri: String?
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("siphon_prefs", Context.MODE_PRIVATE) }

    var maxD by remember(currentMaxDownloads) { mutableIntStateOf(currentMaxDownloads) }
    var workers by remember(currentWorkers) { mutableIntStateOf(currentWorkers) }
    var bandwidthMb by remember(currentBandwidthLimit) {
        mutableIntStateOf((currentBandwidthLimit / (1024 * 1024)).toInt().coerceIn(0, 50))
    }
    var wifiOnly by remember(currentWifiOnly) { mutableStateOf(currentWifiOnly) }
    var customUri by remember(currentCustomDownloadUri) {
        mutableStateOf(currentCustomDownloadUri ?: prefs.getString("pref_custom_download_uri", null))
    }

    fun applyChanges(
        nextMaxD: Int = maxD,
        nextWorkers: Int = workers,
        nextBandwidthMb: Int = bandwidthMb,
        nextWifiOnly: Boolean = wifiOnly,
        nextCustomUri: String? = customUri
    ) {
        val limitBytes = if (nextBandwidthMb <= 0) 0L else nextBandwidthMb * 1024L * 1024L
        onUpdateConcurrency(nextMaxD, nextWorkers, limitBytes, nextWifiOnly, nextCustomUri)
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            val uriString = uri.toString()
            customUri = uriString
            applyChanges(nextCustomUri = uriString)
        }
    }

    SettingsSubPageScaffold(
        title = "Downloads & Storage",
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
            // Concurrency Card
            item(key = "section_concurrency") {
                SettingsSectionHeader(
                    icon = Icons.Rounded.Download,
                    title = "Concurrency"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Max Concurrent Downloads: $maxD",
                                style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Slider(
                                value = maxD.toFloat(),
                                onValueChange = { value ->
                                    val next = value.roundToInt().coerceIn(1, 4)
                                    if (next != maxD) {
                                        maxD = next
                                        applyChanges(nextMaxD = next)
                                    }
                                },
                                valueRange = 1f..4f,
                                steps = 2
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column {
                            Text(
                                text = "Workers Per Download: $workers",
                                style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Slider(
                                value = workers.toFloat(),
                                onValueChange = { value ->
                                    val next = value.roundToInt().coerceIn(1, 8)
                                    if (next != workers) {
                                        workers = next
                                        applyChanges(nextWorkers = next)
                                    }
                                },
                                valueRange = 1f..8f,
                                steps = 6
                            )
                        }
                    }
                }
            }

            // Network & Speed Card
            item(key = "section_network_speed") {
                SettingsSectionHeader(
                    icon = Icons.Rounded.Speed,
                    title = "Network & Speed"
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = if (bandwidthMb <= 0) {
                                    "Speed Limit: Unlimited"
                                } else {
                                    "Speed Limit: $bandwidthMb MB/s"
                                },
                                style = MaterialTheme.typography.titleSmall.merge(TabularNumberStyle),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Slider(
                                value = bandwidthMb.toFloat(),
                                onValueChange = { value ->
                                    val next = value.roundToInt().coerceIn(0, 50)
                                    if (next != bandwidthMb) {
                                        bandwidthMb = next
                                        applyChanges(nextBandwidthMb = next)
                                    }
                                },
                                valueRange = 0f..50f,
                                steps = 49
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsSwitchRow(
                            icon = Icons.Rounded.Wifi,
                            title = "Download on Wi-Fi only",
                            subtitle = "Pause or defer downloads on cellular networks",
                            checked = wifiOnly,
                            onCheckedChange = { checked ->
                                wifiOnly = checked
                                applyChanges(nextWifiOnly = checked)
                            }
                        )
                    }
                }
            }

            // Storage Location Card
            item(key = "section_storage_location") {
                SettingsSectionHeader(
                    icon = Icons.Rounded.Folder,
                    title = "Storage Location"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Download Folder",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val displayFolder = remember(customUri) {
                            if (customUri.isNullOrBlank()) {
                                "Default (System Downloads)"
                            } else {
                                runCatching {
                                    val parsed = Uri.parse(customUri)
                                    parsed.lastPathSegment?.substringAfterLast(':')
                                        ?: parsed.path
                                        ?: customUri!!
                                }.getOrDefault(customUri!!)
                            }
                        }

                        Text(
                            text = displayFolder,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { folderPickerLauncher.launch(null) },
                                shape = ExpressiveShapeTokens.PillShape
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose Folder")
                            }

                            if (customUri != null) {
                                TextButton(
                                    onClick = {
                                        customUri = null
                                        applyChanges(nextCustomUri = null)
                                    }
                                ) {
                                    Text("Reset")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
