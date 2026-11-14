package com.znarfm.siphon.ui.queue

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.znarfm.siphon.core.common.FormatUtils
import com.znarfm.siphon.data.db.ChunkEntity
import com.znarfm.siphon.data.db.ChunkState
import com.znarfm.siphon.data.db.DownloadEntity
import com.znarfm.siphon.data.db.DownloadState
import com.znarfm.siphon.data.db.DownloadType
import com.znarfm.siphon.ui.theme.StateActive
import com.znarfm.siphon.ui.theme.StateCompleted
import com.znarfm.siphon.ui.theme.StateExpired
import com.znarfm.siphon.ui.theme.StateFailed
import com.znarfm.siphon.ui.theme.StatePaused
import com.znarfm.siphon.ui.theme.StateWaiting
import java.util.Locale
import com.znarfm.siphon.ui.theme.SiphonIcons

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DownloadItemCard(
    download: DownloadEntity,
    chunks: List<ChunkEntity>,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onRenameClick: () -> Unit,
    onRebind: () -> Unit,
    onShare: () -> Unit,
    onCopyStreamUrl: () -> Unit,
    onCopyPageUrl: () -> Unit,
    onOpen: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isMenuExpanded by remember { mutableStateOf(false) }
    val isCompleted = download.state == DownloadState.COMPLETED

    // Morph the card corner radius using animateDpAsState with fastSpatialSpec based on state & isSelected
    val targetCornerRadius = when {
        isSelected -> 26.dp
        download.state == DownloadState.ACTIVE || download.state == DownloadState.MERGING -> 22.dp
        else -> 18.dp
    }
    val animatedCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "cardCornerMorph"
    )
    val cardShape = RoundedCornerShape(animatedCornerRadius)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onCardLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() }
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
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

                    Spacer(modifier = Modifier.width(6.dp))

                    StateBadge(state = download.state)
                }

                Spacer(modifier = Modifier.height(4.dp))

                val isHls = download.type == DownloadType.HLS
                val isTorrent = download.type == DownloadType.TORRENT
                val totalSegments = chunks.size
                val completedSegments = chunks.count { it.state == ChunkState.MERGED || it.state == ChunkState.DONE }
                val progressFraction = computeProgress(download, chunks)
                val animatedProgress by animateFloatAsState(
                    targetValue = progressFraction,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    label = "downloadProgress"
                )
                val progressPercent = (progressFraction * 100).toInt()
                val downloadedBytes = computeDownloadedBytes(download, chunks)

                val estTotal = if (completedSegments > 0 && totalSegments > 0 && downloadedBytes > 0L) {
                    val projected = (downloadedBytes.toDouble() / completedSegments * totalSegments).toLong()
                    if (download.totalBytes > 0L) maxOf(download.totalBytes, projected) else projected
                } else {
                    download.totalBytes
                }

                // Subtitle: Type & Size / Progress (using fontFeatureSettings = "tnum" to eliminate jitter)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isTorrent) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = SiphonIcons.Hub,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "TORRENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (isHls) "HLS Stream" else "Multi-Chunk HTTP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (download.state == DownloadState.MERGING) {
                        Spacer(modifier = Modifier.width(6.dp))
                        LoadingIndicator(modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Remuxing MP4… Saving to storage…",
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val progressDetail = when {
                            download.state == DownloadState.COMPLETED -> {
                                val finalBytes = if (download.totalBytes > 0) download.totalBytes else downloadedBytes
                                val sizeText = if (finalBytes > 0L) FormatUtils.formatBytes(finalBytes) else "Adaptive / Unknown"
                                " • $sizeText (100%)"
                            }
                            isTorrent -> {
                                when {
                                    download.totalBytes > 0L && download.state == DownloadState.ACTIVE -> {
                                        val peerLabel = if (download.peers == 1) "1 peer" else "${download.peers} peers"
                                        " • ${FormatUtils.formatBytes(downloadedBytes)} / ${FormatUtils.formatBytes(download.totalBytes)} ($progressPercent%) • ${formatSpeed(download.speedBytesPerSec)} • $peerLabel"
                                    }
                                    download.totalBytes > 0L -> {
                                        " • ${FormatUtils.formatBytes(downloadedBytes)} / ${FormatUtils.formatBytes(download.totalBytes)} ($progressPercent%)"
                                    }
                                    download.state == DownloadState.ACTIVE || download.state == DownloadState.QUEUED -> {
                                        val peerSuffix = if (download.peers > 0) " (${download.peers} peers)" else ""
                                        " • Fetching metadata…$peerSuffix"
                                    }
                                    else -> ""
                                }
                            }
                            isHls -> {
                                val totalStr = if (estTotal > 0L) " of ~${FormatUtils.formatBytes(estTotal)}" else ""
                                if (totalSegments > 0) {
                                    " • ${FormatUtils.formatBytes(downloadedBytes)}$totalStr · $completedSegments/$totalSegments segs ($progressPercent%)"
                                } else {
                                    " • ${FormatUtils.formatBytes(downloadedBytes)}$totalStr ($progressPercent%)"
                                }
                            }
                            download.totalBytes > 0L -> {
                                " • ${FormatUtils.formatBytes(downloadedBytes)} / ${FormatUtils.formatBytes(download.totalBytes)} ($progressPercent%)"
                            }
                            else -> {
                                " • Adaptive / Unknown"
                            }
                        }

                        if (progressDetail.isNotEmpty()) {
                            Text(
                                text = progressDetail,
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar: LinearWavyProgressIndicator for MERGING or ACTIVE with empty chunks (indeterminate only when torrent totalBytes <= 0)
                val isIndeterminate = download.state == DownloadState.MERGING ||
                    (download.state == DownloadState.ACTIVE && chunks.isEmpty() && (!isTorrent || download.totalBytes <= 0L))
                val progressTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)

                if (isIndeterminate) {
                    key(download.state, true) {
                        LinearWavyProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = progressTrackColor
                        )
                    }
                } else if (chunks.isNotEmpty()) {
                    SegmentedChunkBar(chunks = chunks, totalBytes = download.totalBytes, isHls = isHls)
                } else {
                    key(download.state, false) {
                        LinearWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = progressTrackColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Unified Material 3 Expressive SplitButtonLayout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val primaryAction: () -> Unit = when (download.state) {
                        DownloadState.ACTIVE, DownloadState.QUEUED -> onPause
                        DownloadState.MERGING -> ({})
                        DownloadState.PAUSED, DownloadState.WAITING_FOR_NETWORK -> onResume
                        DownloadState.FAILED, DownloadState.CANCELLED -> onRetry
                        DownloadState.AUTH_EXPIRED -> onRebind
                        DownloadState.COMPLETED -> ({ onOpen?.invoke() ?: openCompletedFile(context, download) })
                    }

                    val (primaryIcon, primaryLabel) = when (download.state) {
                        DownloadState.ACTIVE, DownloadState.QUEUED -> SiphonIcons.Pause to "Pause"
                        DownloadState.MERGING -> SiphonIcons.HourglassTop to "Merging…"
                        DownloadState.PAUSED, DownloadState.WAITING_FOR_NETWORK -> SiphonIcons.PlayArrow to "Resume"
                        DownloadState.FAILED, DownloadState.CANCELLED -> SiphonIcons.Refresh to "Retry"
                        DownloadState.AUTH_EXPIRED -> SiphonIcons.Link to "Update Link"
                        DownloadState.COMPLETED -> SiphonIcons.FolderOpen to "Open"
                    }

                    val chevronRotation by animateFloatAsState(
                        targetValue = if (isMenuExpanded) 180f else 0f,
                        label = "splitButtonChevron"
                    )

                    SplitButtonLayout(
                        leadingButton = {
                            SplitButtonDefaults.LeadingButton(
                                onClick = primaryAction,
                                enabled = download.state != DownloadState.MERGING
                            ) {
                                Icon(
                                    imageVector = primaryIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = primaryLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        trailingButton = {
                            Box {
                                SplitButtonDefaults.TrailingButton(
                                    checked = isMenuExpanded,
                                    onCheckedChange = { isMenuExpanded = it }
                                ) {
                                    Icon(
                                        imageVector = SiphonIcons.ArrowDropDown,
                                        contentDescription = "More actions",
                                        modifier = Modifier
                                            .size(18.dp)
                                            .rotate(chevronRotation)
                                    )
                                }

                                DropdownMenu(
                                    expanded = isMenuExpanded,
                                    onDismissRequest = { isMenuExpanded = false }
                                ) {
                                    val isTerminal = download.state == DownloadState.COMPLETED || download.state == DownloadState.CANCELLED
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isTerminal) SiphonIcons.DeleteOutline else SiphonIcons.Cancel,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        text = { Text(if (isTerminal) "Delete Download" else "Cancel Download") },
                                        onClick = {
                                            isMenuExpanded = false
                                            if (isTerminal) onDelete() else onCancel()
                                        }
                                    )
                                    if (isCompleted || !download.destinationUri.isNullOrBlank()) {
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = SiphonIcons.Share,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            text = { Text("Share File") },
                                            onClick = {
                                                isMenuExpanded = false
                                                onShare()
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = SiphonIcons.Link,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        text = { Text("Copy Stream URL") },
                                        onClick = {
                                            isMenuExpanded = false
                                            onCopyStreamUrl()
                                        }
                                    )
                                    if (!download.sourcePageUrl.isNullOrBlank()) {
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = SiphonIcons.Language,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            text = { Text("Copy Page URL") },
                                            onClick = {
                                                isMenuExpanded = false
                                                onCopyPageUrl()
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = SiphonIcons.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        text = { Text("Rename") },
                                        onClick = {
                                            isMenuExpanded = false
                                            onRenameClick()
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = SiphonIcons.Info,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        text = { Text("Inspect Details") },
                                        onClick = {
                                            isMenuExpanded = false
                                            onCardClick()
                                        }
                                    )
                                }
                            }
                        }
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
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        if (chunkCount == 0) {
            drawRect(color = trackBackgroundColor)
            return@Canvas
        }

        val totalW = size.width
        val rawGapW = 1.5.dp.toPx()
        val gapW = if (chunkCount in 2..60 && (rawGapW * (chunkCount - 1)) <= totalW * 0.35f) rawGapW else 0f
        val totalGaps = gapW * (chunkCount - 1)
        val usableW = (totalW - totalGaps).coerceAtLeast(1f)

        if (isHls || totalBytes <= 0L) {
            val segW = usableW / chunkCount
            if (segW < 1.5f) {
                // Segments too dense to draw individually; render continuous bar
                drawRect(color = pendingColor, size = Size(totalW, size.height))
                val completedCount = chunks.count { it.state == ChunkState.MERGED || it.state == ChunkState.DONE }
                val inFlightCount = chunks.count { it.state == ChunkState.IN_FLIGHT }
                val completedW = totalW * (completedCount.toFloat() / chunkCount).coerceIn(0f, 1f)
                val inFlightW = totalW * (inFlightCount.toFloat() / chunkCount).coerceIn(0f, (1f - completedCount.toFloat() / chunkCount).coerceAtLeast(0f))
                if (completedW > 0f) {
                    drawRect(color = doneColor, topLeft = Offset(0f, 0f), size = Size(completedW, size.height))
                }
                if (inFlightW > 0f) {
                    drawRect(color = inFlightColor, topLeft = Offset(completedW, 0f), size = Size(inFlightW, size.height))
                }
            } else {
                for (i in 0 until chunkCount) {
                    val chunk = chunks[i]
                    val color = when (chunk.state) {
                        ChunkState.MERGED, ChunkState.DONE -> doneColor
                        ChunkState.IN_FLIGHT -> inFlightColor
                        else -> pendingColor
                    }
                    val startX = i * (segW + gapW)
                    drawRect(color = color, topLeft = Offset(startX, 0f), size = Size(segW, size.height))
                }
            }
        } else {
            var currentX = 0f
            for (i in 0 until chunkCount) {
                val chunk = chunks[i]
                val chunkSpan = (chunk.endOffset - chunk.startOffset + 1).coerceAtLeast(1L)
                val chunkRatio = (chunkSpan.toDouble() / totalBytes).coerceIn(0.0, 1.0)
                val chunkW = (usableW * chunkRatio).toFloat().coerceAtLeast(2f)

                val chunkDone = (chunk.currentOffset - chunk.startOffset).coerceAtLeast(0L)
                val doneRatio = (chunkDone.toDouble() / chunkSpan).coerceIn(0.0, 1.0)
                val doneW = (chunkW * doneRatio).toFloat()

                drawRect(color = pendingColor, topLeft = Offset(currentX, 0f), size = Size(chunkW, size.height))
                if (doneW > 0f) {
                    val color = if (chunk.state == ChunkState.IN_FLIGHT) inFlightColor else doneColor
                    drawRect(color = color, topLeft = Offset(currentX, 0f), size = Size(doneW, size.height))
                }
                currentX += chunkW + gapW
            }
        }
    }
}

@Composable
fun StateBadge(state: DownloadState, modifier: Modifier = Modifier) {
    val (color, text) = when (state) {
        DownloadState.QUEUED -> Pair(StateWaiting, "Queued")
        DownloadState.ACTIVE -> Pair(StateActive, "Downloading")
        DownloadState.MERGING -> Pair(StateActive, "Merging")
        DownloadState.PAUSED -> Pair(StatePaused, "Paused")
        DownloadState.COMPLETED -> Pair(StateCompleted, "Complete")
        DownloadState.FAILED -> Pair(StateFailed, "Failed")
        DownloadState.AUTH_EXPIRED -> Pair(StateExpired, "Link Expired")
        DownloadState.WAITING_FOR_NETWORK -> Pair(StateWaiting, "Waiting Network")
        DownloadState.CANCELLED -> Pair(StateFailed, "Cancelled")
    }

    Surface(
        color = color.copy(alpha = 0.18f),
        shape = CircleShape,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

fun computeProgress(download: DownloadEntity, chunks: List<ChunkEntity>): Float {
    val isHls = download.type == DownloadType.HLS
    val isTorrent = download.type == DownloadType.TORRENT
    val totalSegments = chunks.size
    val completedSegments = chunks.count { it.state == ChunkState.MERGED || it.state == ChunkState.DONE }
    return when {
        download.state == DownloadState.COMPLETED -> 1f
        isTorrent && download.totalBytes > 0L -> {
            (download.downloadedBytes.toFloat() / download.totalBytes.toFloat()).coerceIn(0f, 1f)
        }
        isHls && totalSegments > 0 -> (completedSegments.toFloat() / totalSegments).coerceIn(0f, 1f)
        !isHls && !isTorrent && download.totalBytes > 0L -> {
            val downloaded = chunks.sumOf { (it.currentOffset - it.startOffset).coerceAtLeast(0L) }
            (downloaded.toFloat() / download.totalBytes).coerceIn(0f, 1f)
        }
        else -> 0f
    }
}

fun computeDownloadedBytes(download: DownloadEntity, chunks: List<ChunkEntity>): Long {
    val isHls = download.type == DownloadType.HLS
    val isTorrent = download.type == DownloadType.TORRENT
    return when {
        isTorrent -> {
            if (download.state == DownloadState.COMPLETED && download.downloadedBytes <= 0L) {
                download.totalBytes
            } else {
                download.downloadedBytes
            }
        }
        isHls -> {
            val mergedBytes = chunks.filter { it.state == ChunkState.MERGED }
                .groupBy { it.trackIndex }
                .values
                .sumOf { trackChunks -> trackChunks.maxOfOrNull { it.endOffset } ?: 0L }
            val doneBytes = chunks.filter { it.state == ChunkState.DONE }.sumOf { it.currentOffset }
            val actualBytes = mergedBytes + doneBytes
            if (actualBytes > 0L) {
                actualBytes
            } else {
                val totalSegments = chunks.size
                val completedSegments = chunks.count { it.state == ChunkState.MERGED || it.state == ChunkState.DONE }
                if (download.totalBytes > 0 && totalSegments > 0) {
                    (download.totalBytes * (completedSegments.toDouble() / totalSegments)).toLong()
                } else 0L
            }
        }
        else -> {
            chunks.sumOf { (it.currentOffset - it.startOffset).coerceAtLeast(0L) }
        }
    }
}

fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0L) return "0 B/s"
    val kb = bytesPerSec / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB/s", kb)
    val mb = kb / 1024.0
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.2f GB/s", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.1f MB/s", mb)
    }
}
