package com.siphon.app.ui.queue

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
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
    bandwidthLimitBytes: Long = 0L,
    isWifiOnly: Boolean = false,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onDelete: (downloadId: Long, deleteFile: Boolean) -> Unit,
    onRebind: (DownloadEntity) -> Unit,
    onSaveSettings: (maxDownloads: Int, workers: Int, bandwidthLimit: Long, wifiOnly: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(QueueFilter.ALL) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var pendingDeleteDownload by remember { mutableStateOf<DownloadEntity?>(null) }

    val filteredList = when (selectedFilter) {
        QueueFilter.ALL -> downloads
        QueueFilter.ACTIVE -> downloads.filter { it.state == DownloadState.ACTIVE || it.state == DownloadState.QUEUED }
        QueueFilter.PAUSED -> downloads.filter { it.state == DownloadState.PAUSED || it.state == DownloadState.AUTH_EXPIRED || it.state == DownloadState.WAITING_FOR_NETWORK }
        QueueFilter.COMPLETED -> downloads.filter { it.state == DownloadState.COMPLETED }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Header
        Surface(
            tonalElevation = 3.dp,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Download Queue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${downloads.count { it.state == DownloadState.ACTIVE }} active • $maxConcurrentDownloads tasks max • $workersPerDownload workers/task" +
                            (if (bandwidthLimitBytes > 0) " • ${bandwidthLimitBytes / (1024 * 1024)} MB/s" else "") +
                            (if (isWifiOnly) " • Wi-Fi Only" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = { showSettingsDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Filters row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QueueFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    shape = RoundedCornerShape(16.dp),
                    label = { Text(filter.label, fontWeight = FontWeight.SemiBold) }
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 84.dp),
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
                        onDelete = { pendingDeleteDownload = item },
                        onRebind = { onRebind(item) }
                    )
                }
            }
        }
    }

    if (pendingDeleteDownload != null) {
        val target = pendingDeleteDownload!!
        AlertDialog(
            onDismissRequest = { pendingDeleteDownload = null },
            title = { Text("Delete Download") },
            text = {
                Text("Delete '${target.displayName}'? Choose whether to delete only the history record or also delete the downloaded file from storage.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(target.id, true)
                        pendingDeleteDownload = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete File & Record")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { pendingDeleteDownload = null }) {
                        Text("Cancel")
                    }
                    FilledTonalButton(
                        onClick = {
                            onDelete(target.id, false)
                            pendingDeleteDownload = null
                        }
                    ) {
                        Text("Record Only")
                    }
                }
            }
        )
    }

    if (showSettingsDialog) {
        ConcurrencySettingsDialog(
            currentMaxDownloads = maxConcurrentDownloads,
            currentWorkers = workersPerDownload,
            currentBandwidthLimit = bandwidthLimitBytes,
            currentWifiOnly = isWifiOnly,
            onDismiss = { showSettingsDialog = false },
            onConfirm = { maxD, w, limit, wifi ->
                onSaveSettings(maxD, w, limit, wifi)
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
    val context = LocalContext.current
    val isCompleted = download.state == DownloadState.COMPLETED

    val openFile = {
        val uriStr = download.destinationUri
        if (!uriStr.isNullOrBlank()) {
            val uri = Uri.parse(uriStr)
            val mime = if (download.mimeType.contains("mpegurl")) "video/mp4" else download.mimeType.ifBlank { "video/*" }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(Intent.createChooser(intent, "Open with"))
            } catch (_: Exception) {
                val fallback = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching { context.startActivity(Intent.createChooser(fallback, "Open with")) }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isCompleted) Modifier.clickable { openFile() } else Modifier)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Display Name & State Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = download.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                StateBadge(state = download.state)
            }

            Spacer(modifier = Modifier.height(6.dp))

            val isHls = download.type == DownloadType.HLS
            val totalSegments = chunks.size
            val completedSegments = chunks.count { it.state == ChunkState.MERGED || it.state == ChunkState.DONE }
            val progressFraction = if (isHls && totalSegments > 0) {
                completedSegments.toFloat() / totalSegments
            } else if (!isHls && download.totalBytes > 0L) {
                (chunks.sumOf { (it.currentOffset - it.startOffset).coerceAtLeast(0L) }.toFloat() / download.totalBytes).coerceIn(0f, 1f)
            } else if (download.state == DownloadState.COMPLETED) {
                1f
            } else {
                0f
            }
            val progressPercent = (progressFraction * 100).toInt()

            val downloadedBytes = if (isHls) {
                if (download.totalBytes > 0) (download.totalBytes * progressFraction).toLong() else 0L
            } else {
                chunks.sumOf { (it.currentOffset - it.startOffset).coerceAtLeast(0L) }
            }

            // Subtitle: Type & Size / Progress
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isHls) "HLS Stream" else "Multi-Chunk HTTP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val progressDetail = when {
                    download.state == DownloadState.COMPLETED -> {
                        " • ${formatFileSize(if (download.totalBytes > 0) download.totalBytes else downloadedBytes)} (100%)"
                    }
                    download.totalBytes > 0L -> {
                        " • ${formatFileSize(downloadedBytes)} / ${formatFileSize(download.totalBytes)} ($progressPercent%)"
                    }
                    isHls && totalSegments > 0 -> {
                        " • $completedSegments / $totalSegments segments ($progressPercent%)"
                    }
                    else -> {
                        " • " + formatFileSize(download.totalBytes)
                    }
                }

                Text(
                    text = progressDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Segmented Chunk Progress Bar
            if (chunks.isNotEmpty()) {
                SegmentedChunkBar(chunks = chunks, totalBytes = download.totalBytes, isHls = isHls)
            } else {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons (M3 Expressive Pill buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (download.state) {
                    DownloadState.ACTIVE, DownloadState.QUEUED -> {
                        Button(
                            onClick = onPause,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Pause", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    DownloadState.PAUSED, DownloadState.WAITING_FOR_NETWORK, DownloadState.FAILED -> {
                        Button(
                            onClick = onResume,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    DownloadState.AUTH_EXPIRED -> {
                        Button(
                            onClick = onRebind,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Update Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    DownloadState.COMPLETED -> {
                        Button(
                            onClick = openFile,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    DownloadState.CANCELLED -> {}
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = if (download.state == DownloadState.COMPLETED || download.state == DownloadState.CANCELLED) onDelete else onCancel,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (download.state == DownloadState.COMPLETED || download.state == DownloadState.CANCELLED) "Delete" else "Cancel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
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
    isHls: Boolean = false,
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

        if (!isHls && totalBytes > 0L) {
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
                    size = Size(chunkWidth.coerceAtLeast(1f), barHeight)
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
    currentBandwidthLimit: Long,
    currentWifiOnly: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (maxDownloads: Int, workers: Int, bandwidthLimit: Long, wifiOnly: Boolean) -> Unit
) {
    var maxDownloads by remember { mutableIntStateOf(currentMaxDownloads) }
    var workers by remember { mutableIntStateOf(currentWorkers) }
    var bandwidthMb by remember { mutableIntStateOf((currentBandwidthLimit / (1024 * 1024)).toInt().coerceIn(0, 50)) }
    var wifiOnly by remember { mutableStateOf(currentWifiOnly) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Settings") },
        text = {
            Column {
                Text("Max Concurrent Active Downloads: $maxDownloads (1–4)", fontSize = 13.sp)
                Slider(
                    value = maxDownloads.toFloat(),
                    onValueChange = { maxDownloads = it.toInt() },
                    valueRange = 1f..4f,
                    steps = 2
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Workers per Download: $workers (1–8)", fontSize = 13.sp)
                Slider(
                    value = workers.toFloat(),
                    onValueChange = { workers = it.toInt() },
                    valueRange = 1f..8f,
                    steps = 6
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (bandwidthMb <= 0) "Speed Limit: Unlimited" else "Speed Limit: $bandwidthMb MB/s",
                    fontSize = 13.sp
                )
                Slider(
                    value = bandwidthMb.toFloat(),
                    onValueChange = { bandwidthMb = it.toInt() },
                    valueRange = 0f..50f,
                    steps = 49
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { wifiOnly = !wifiOnly }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Wi-Fi Only (pause on metered data)", fontSize = 13.sp)
                    androidx.compose.material3.Switch(
                        checked = wifiOnly,
                        onCheckedChange = { wifiOnly = it },
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val limitBytes = if (bandwidthMb <= 0) 0L else bandwidthMb * 1024L * 1024L
                onConfirm(maxDownloads, workers, limitBytes, wifiOnly)
            }) {
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
