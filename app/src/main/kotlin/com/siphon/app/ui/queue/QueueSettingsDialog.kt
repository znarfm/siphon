package com.siphon.app.ui.queue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ConcurrencySettingsDialog(
    currentMaxDownloads: Int,
    currentWorkers: Int,
    currentBandwidthLimit: Long = 0L,
    currentWifiOnly: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (maxDownloads: Int, workers: Int, bandwidthLimit: Long, wifiOnly: Boolean) -> Unit
) {
    var maxD by remember { mutableIntStateOf(currentMaxDownloads) }
    var workers by remember { mutableIntStateOf(currentWorkers) }
    var bandwidthMb by remember { mutableIntStateOf((currentBandwidthLimit / (1024 * 1024)).toInt().coerceIn(0, 50)) }
    var wifiOnly by remember { mutableStateOf(currentWifiOnly) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Engine Configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(
                        text = "Max Concurrent Downloads: $maxD",
                        style = MaterialTheme.typography.bodyMedium,
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
                        style = MaterialTheme.typography.bodyMedium,
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
                        style = MaterialTheme.typography.bodyMedium,
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limitBytes = if (bandwidthMb <= 0) 0L else bandwidthMb * 1024L * 1024L
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
