package com.znarfm.siphon.ui.queue

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.sp
import com.znarfm.siphon.ui.theme.TabularNumberStyle

@Composable
fun ConcurrencySettingsDialog(
    currentMaxDownloads: Int,
    currentWorkers: Int,
    currentBandwidthLimit: Long = 0L,
    currentWifiOnly: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (maxDownloads: Int, workers: Int, bandwidthLimit: Long, wifiOnly: Boolean) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("siphon_prefs", Context.MODE_PRIVATE) }

    var maxD by remember { mutableIntStateOf(currentMaxDownloads) }
    var workers by remember { mutableIntStateOf(currentWorkers) }
    var bandwidthMb by remember { mutableIntStateOf((currentBandwidthLimit / (1024 * 1024)).toInt().coerceIn(0, 50)) }
    var wifiOnly by remember { mutableStateOf(currentWifiOnly) }
    var customUri by remember {
        mutableStateOf(prefs.getString("pref_custom_download_uri", null))
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            customUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Engine Configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(
                        text = "Max Concurrent Downloads: $maxD",
                        style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = maxD.toFloat(),
                        onValueChange = { maxD = it.toInt() },
                        valueRange = 1f..4f,
                        steps = 2
                    )
                }

                Column {
                    Text(
                        text = "Workers Per Download: $workers",
                        style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = workers.toFloat(),
                        onValueChange = { workers = it.toInt() },
                        valueRange = 1f..8f,
                        steps = 6
                    )
                }

                Column {
                    Text(
                        text = if (bandwidthMb <= 0) "Speed Limit: Unlimited" else "Speed Limit: $bandwidthMb MB/s",
                        style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = bandwidthMb.toFloat(),
                        onValueChange = { bandwidthMb = it.toInt() },
                        valueRange = 0f..50f,
                        steps = 50
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { wifiOnly = !wifiOnly },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = wifiOnly, onCheckedChange = { wifiOnly = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Download on Wi-Fi only", style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider()

                // Download Location (SAF Folder Picker)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Download Location",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val displayFolder = remember(customUri) {
                        if (customUri.isNullOrBlank()) {
                            "Default (System Downloads)"
                        } else {
                            runCatching {
                                val parsed = Uri.parse(customUri)
                                parsed.lastPathSegment?.substringAfterLast(':') ?: parsed.path ?: customUri!!
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
                            onClick = { folderPickerLauncher.launch(null) }
                        ) {
                            Text("Choose Folder", fontSize = 12.sp)
                        }

                        if (customUri != null) {
                            TextButton(
                                onClick = { customUri = null }
                            ) {
                                Text("Reset", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limitBytes = if (bandwidthMb <= 0) 0L else bandwidthMb * 1024L * 1024L
                    prefs.edit().apply {
                        if (customUri != null) {
                            putString("pref_custom_download_uri", customUri)
                        } else {
                            remove("pref_custom_download_uri")
                        }
                    }.apply()
                    onConfirm(maxD, workers, limitBytes, wifiOnly)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

