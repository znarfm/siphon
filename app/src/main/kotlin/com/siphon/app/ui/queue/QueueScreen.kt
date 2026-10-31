package com.siphon.app.ui.queue

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siphon.app.data.db.ChunkEntity
import com.siphon.app.data.db.ChunkState
import com.siphon.app.data.db.DownloadEntity
import com.siphon.app.data.db.DownloadState
import com.siphon.app.data.db.DownloadType
import com.siphon.app.ui.theme.StateActive
import com.siphon.app.ui.theme.StateCompleted
import com.siphon.app.ui.theme.StateExpired
import com.siphon.app.ui.theme.StateFailed
import com.siphon.app.ui.theme.StatePaused
import com.siphon.app.ui.theme.StateWaiting
import java.util.Locale

@Composable
fun QueueScreen(
    downloads: List<DownloadEntity>,
    chunksMap: Map<Long, List<ChunkEntity>>,
    maxConcurrentDownloads: Int,
    workersPerDownload: Int,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRebind: (DownloadEntity) -> Unit,
    onSaveConcurrency: (maxDownloads: Int, workers: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(QueueFilter.ALL) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val filteredList = when (selectedFilter) {
        QueueFilter.ALL -> downloads
        QueueFilter.ACTIVE -> downloads.filter { it.state == DownloadState.ACTIVE || it.state == DownloadState.QUEUED }
        QueueFilter.PAUSED -> downloads.filter { it.state == DownloadState.PAUSED || it.state == DownloadState.AUTH_EXPIRED || it.state == DownloadState.WAITING_FOR_NETWORK }
        QueueFilter.COMPLETED -> downloads.filter { it.state == DownloadState.COMPLETED }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Header
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Download Queue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${downloads.count { it.state == DownloadState.ACTIVE }} active • $maxConcurrentDownloads tasks max • $workersPerDownload workers/task",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = { showSettingsDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) {
                    Text("Settings", fontSize = 12.sp)
                }
            }
        }

        // Filters row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QueueFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label) }
                )
            }
        }

        // List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No downloads in queue",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    val chunks = chunksMap[item.id] ?: emptyList()
                    DownloadItemCard(
                        download = item,
                        chunks = chunks,
                        onPause = { onPause(item.id) },
                        onResume = { onResume(item.id) },
                        onCancel = { onCancel(item.id) },
                        onDelete = { onDelete(item.id) },
                        onRebind = { onRebind(item) }
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        ConcurrencySettingsDialog(
            currentMaxDownloads = maxConcurrentDownloads,
            currentWorkers = workersPerDownload,
            onDismiss = { showSettingsDialog = false },
            onConfirm = { maxD, w ->
                onSaveConcurrency(maxD, w)
                showSettingsDialog = false
            }
        )
    }
}

enum class QueueFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Done")
}

@Composable
private fun DownloadItemCard(
    download: DownloadEntity,
    chunks: List<ChunkEntity>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onRebind: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Display Name & State Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = download.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                StateBadge(state = download.state)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle: Type & Size
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (download.type == DownloadType.HLS) "HLS Stream" else "Multi-Chunk HTTP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = " • " + formatFileSize(download.totalBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Segmented Chunk Progress Bar
            if (chunks.isNotEmpty()) {
                SegmentedChunkBar(chunks = chunks, totalBytes = download.totalBytes)
            } else {
                LinearProgressIndicator(
                    progress = { if (download.state == DownloadState.COMPLETED) 1f else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (download.state) {
                    DownloadState.ACTIVE, DownloadState.QUEUED -> {
                        Button(
                            onClick = onPause,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Pause", fontSize = 12.sp)
                        }
                    }
                    DownloadState.PAUSED, DownloadState.WAITING_FOR_NETWORK, DownloadState.FAILED -> {
                        Button(
                            onClick = onResume,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Resume", fontSize = 12.sp)
                        }
                    }
                    DownloadState.AUTH_EXPIRED -> {
                        Button(
                            onClick = onRebind,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Update Link", fontSize = 12.sp)
                        }
                    }
                    DownloadState.COMPLETED, DownloadState.CANCELLED -> {}
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = if (download.state == DownloadState.COMPLETED || download.state == DownloadState.CANCELLED) onDelete else onCancel,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (download.state == DownloadState.COMPLETED || download.state == DownloadState.CANCELLED) "Delete" else "Cancel",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SegmentedChunkBar(
    chunks: List<ChunkEntity>,
    totalBytes: Long,
    modifier: Modifier = Modifier
) {
    val chunkCount = chunks.size
    val trackBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val pendingColor = Color.DarkGray
    val inFlightColor = StateWaiting
    val doneColor = StateActive

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(trackBackgroundColor)
    ) {
        val totalWidth = size.width
        val barHeight = size.height

        if (totalBytes > 0L) {
            // Proportional byte chunks
            for (chunk in chunks) {
                val startX = (chunk.startOffset.toFloat() / totalBytes) * totalWidth
                val targetEnd = if (chunk.endOffset > 0) chunk.endOffset else totalBytes
                val slotWidth = ((targetEnd - chunk.startOffset).toFloat() / totalBytes) * totalWidth
                val progressWidth = ((chunk.currentOffset - chunk.startOffset).coerceAtLeast(0L).toFloat() / totalBytes) * totalWidth

                // Background slot (pending)
                drawRect(
                    color = pendingColor,
                    topLeft = Offset(startX, 0f),
                    size = Size((slotWidth - 1f).coerceAtLeast(1f), barHeight)
                )

                // Downloaded portion or full chunk
                when (chunk.state) {
                    ChunkState.DONE, ChunkState.MERGED -> {
                        drawRect(
                            color = doneColor,
                            topLeft = Offset(startX, 0f),
                            size = Size((slotWidth - 1f).coerceAtLeast(1f), barHeight)
                        )
                    }
                    ChunkState.IN_FLIGHT -> {
                        drawRect(
                            color = inFlightColor,
                            topLeft = Offset(startX, 0f),
                            size = Size((progressWidth - 1f).coerceAtLeast(1f), barHeight)
                        )
                    }
                    ChunkState.PENDING -> {
                        if (progressWidth > 0f) {
                            drawRect(
                                color = doneColor.copy(alpha = 0.6f),
                                topLeft = Offset(startX, 0f),
                                size = Size((progressWidth - 1f).coerceAtLeast(1f), barHeight)
                            )
                        }
                    }
                }
            }
        } else {
            // Equal sized chunks (e.g. HLS segments)
            val chunkWidth = totalWidth / chunkCount.coerceAtLeast(1)
            for ((idx, chunk) in chunks.withIndex()) {
                val color = when (chunk.state) {
                    ChunkState.DONE, ChunkState.MERGED -> doneColor
                    ChunkState.IN_FLIGHT -> inFlightColor
                    ChunkState.PENDING -> if (chunk.currentOffset > chunk.startOffset) doneColor.copy(alpha = 0.6f) else pendingColor
                }
                drawRect(
                    color = color,
                    topLeft = Offset(idx * chunkWidth, 0f),
                    size = Size((chunkWidth - 1f).coerceAtLeast(1f), barHeight)
                )
            }
        }
    }
}

@Composable
private fun StateBadge(state: DownloadState) {
    val (label, bg, fg) = when (state) {
        DownloadState.QUEUED -> Triple("QUEUED", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        DownloadState.ACTIVE -> Triple("ACTIVE", StateActive.copy(alpha = 0.2f), StateActive)
        DownloadState.PAUSED -> Triple("PAUSED", StatePaused.copy(alpha = 0.2f), StatePaused)
        DownloadState.WAITING_FOR_NETWORK -> Triple("WAITING", StateWaiting.copy(alpha = 0.2f), StateWaiting)
        DownloadState.AUTH_EXPIRED -> Triple("403 EXPIRED", StateExpired.copy(alpha = 0.2f), StateExpired)
        DownloadState.COMPLETED -> Triple("DONE", StateCompleted.copy(alpha = 0.2f), StateCompleted)
        DownloadState.FAILED -> Triple("FAILED", StateFailed.copy(alpha = 0.2f), StateFailed)
        DownloadState.CANCELLED -> Triple("CANCELLED", Color.Gray.copy(alpha = 0.2f), Color.LightGray)
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
private fun ConcurrencySettingsDialog(
    currentMaxDownloads: Int,
    currentWorkers: Int,
    onDismiss: () -> Unit,
    onConfirm: (maxDownloads: Int, workers: Int) -> Unit
) {
    var maxDownloads by remember { mutableIntStateOf(currentMaxDownloads) }
    var workers by remember { mutableIntStateOf(currentWorkers) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Concurrency Settings") },
        text = {
            Column {
                Text("Max Concurrent Active Downloads: $maxDownloads (1–4)")
                Slider(
                    value = maxDownloads.toFloat(),
                    onValueChange = { maxDownloads = it.toInt() },
                    valueRange = 1f..4f,
                    steps = 2
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Workers per Download: $workers (1–8)")
                Slider(
                    value = workers.toFloat(),
                    onValueChange = { workers = it.toInt() },
                    valueRange = 1f..8f,
                    steps = 6
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(maxDownloads, workers) }) {
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

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "Adaptive / Unknown"
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb >= 1000) {
        String.format(Locale.US, "%.2f GB", mb / 1024)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}
