package com.siphon.app.ui.queue

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siphon.app.core.network.HeadersCodec
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

enum class QueueFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Done")
}

enum class QueueSortOption(val label: String) {
    DATE_DESC("Newest"),
    DATE_ASC("Oldest"),
    SIZE_DESC("Largest"),
    SIZE_ASC("Smallest"),
    PROGRESS_DESC("Progress %"),
    NAME_ASC("Name (A–Z)")
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onClearCompleted: () -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onRetry: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onBulkDelete: (List<Long>, Boolean) -> Unit,
    onBulkCancel: (List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(QueueFilter.ALL) }
    var selectedSort by remember { mutableStateOf(QueueSortOption.DATE_DESC) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateMapOf<Long, Boolean>() }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var pendingDeleteDownload by remember { mutableStateOf<DownloadEntity?>(null) }
    var pendingRenameDownload by remember { mutableStateOf<DownloadEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var inspectorDownload by remember { mutableStateOf<DownloadEntity?>(null) }

    val activeCount = downloads.count { it.state == DownloadState.ACTIVE || it.state == DownloadState.QUEUED || it.state == DownloadState.MERGING }
    val pausedCount = downloads.count {
        it.state == DownloadState.PAUSED || it.state == DownloadState.AUTH_EXPIRED || it.state == DownloadState.WAITING_FOR_NETWORK
    }
    val completedCount = downloads.count { it.state == DownloadState.COMPLETED }

    // Filter + Search + Sort
    val filteredList = remember(downloads, selectedFilter, searchQuery, selectedSort, chunksMap) {
        val byTab = when (selectedFilter) {
            QueueFilter.ALL -> downloads
            QueueFilter.ACTIVE -> downloads.filter { it.state == DownloadState.ACTIVE || it.state == DownloadState.QUEUED || it.state == DownloadState.MERGING }
            QueueFilter.PAUSED -> downloads.filter {
                it.state == DownloadState.PAUSED || it.state == DownloadState.AUTH_EXPIRED || it.state == DownloadState.WAITING_FOR_NETWORK
            }
            QueueFilter.COMPLETED -> downloads.filter { it.state == DownloadState.COMPLETED }
        }

        val q = searchQuery.trim().lowercase()
        val bySearch = if (q.isBlank()) byTab else byTab.filter {
            it.displayName.lowercase().contains(q) ||
                it.url.lowercase().contains(q) ||
                it.sourcePageUrl?.lowercase()?.contains(q) == true
        }

        when (selectedSort) {
            QueueSortOption.DATE_DESC -> bySearch.sortedByDescending { it.createdAt }
            QueueSortOption.DATE_ASC -> bySearch.sortedBy { it.createdAt }
            QueueSortOption.SIZE_DESC -> bySearch.sortedByDescending { it.totalBytes }
            QueueSortOption.SIZE_ASC -> bySearch.sortedBy { it.totalBytes }
            QueueSortOption.PROGRESS_DESC -> bySearch.sortedByDescending { computeProgress(it, chunksMap[it.id].orEmpty()) }
            QueueSortOption.NAME_ASC -> bySearch.sortedBy { it.displayName.lowercase() }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header Surface
        Surface(
            tonalElevation = 3.dp,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (isSelectionMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectedCount = selectedIds.count { it.value }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                isSelectionMode = false
                                selectedIds.clear()
                            }) {
                                Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$selectedCount selected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = {
                                val allSelected = filteredList.all { selectedIds[it.id] == true }
                                if (allSelected) {
                                    selectedIds.clear()
                                } else {
                                    filteredList.forEach { selectedIds[it.id] = true }
                                }
                            }) {
                                Text(if (filteredList.all { selectedIds[it.id] == true }) "Deselect" else "All", fontSize = 12.sp)
                            }

                            if (selectedCount > 0) {
                                FilledTonalButton(
                                    onClick = {
                                        val ids = selectedIds.filter { it.value }.keys.toList()
                                        onBulkCancel(ids)
                                        isSelectionMode = false
                                        selectedIds.clear()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { showBulkDeleteDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                text = "$activeCount active • $maxConcurrentDownloads max • $workersPerDownload workers" +
                                    (if (bandwidthLimitBytes > 0) " • ${bandwidthLimitBytes / (1024 * 1024)} MB/s" else "") +
                                    (if (isWifiOnly) " • Wi-Fi" else ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                                Text("🔍", fontSize = 15.sp)
                            }
                            FilledTonalButton(
                                onClick = { showSettingsDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeCount > 0) {
                            FilledTonalButton(
                                onClick = onPauseAll,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("⏸ Pause All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (pausedCount > 0) {
                            FilledTonalButton(
                                onClick = onResumeAll,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("▶ Resume All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (completedCount > 0) {
                            OutlinedButton(
                                onClick = onClearCompleted,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Clear Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Search & Filter Toolbar (Item 2)
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by filename, URL, or domain…", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("✕", fontSize = 12.sp)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Text("Sort:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(QueueSortOption.entries) { opt ->
                        FilterChip(
                            selected = selectedSort == opt,
                            onClick = { selectedSort = opt },
                            shape = RoundedCornerShape(12.dp),
                            label = { Text(opt.label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        // Filter chips row
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
                    shape = RoundedCornerShape(16.dp),
                    label = { Text(filter.label, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        // Downloads List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No downloads matching '$searchQuery'" else "No downloads in queue",
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
                    val isChecked = selectedIds[item.id] == true

                    DownloadItemCard(
                        download = item,
                        chunks = chunks,
                        isSelectionMode = isSelectionMode,
                        isSelected = isChecked,
                        onToggleSelect = {
                            val next = !isChecked
                            if (next) selectedIds[item.id] = true else selectedIds.remove(item.id)
                            if (selectedIds.isEmpty()) isSelectionMode = false
                        },
                        onCardClick = {
                            if (isSelectionMode) {
                                val next = !isChecked
                                if (next) selectedIds[item.id] = true else selectedIds.remove(item.id)
                                if (selectedIds.isEmpty()) isSelectionMode = false
                            } else {
                                inspectorDownload = item
                            }
                        },
                        onCardLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedIds.clear()
                                selectedIds[item.id] = true
                            } else {
                                val next = !isChecked
                                if (next) selectedIds[item.id] = true else selectedIds.remove(item.id)
                                if (selectedIds.isEmpty()) isSelectionMode = false
                            }
                        },
                        onPause = { onPause(item.id) },
                        onResume = { onResume(item.id) },
                        onRetry = { onRetry(item.id) },
                        onCancel = { onCancel(item.id) },
                        onDelete = { pendingDeleteDownload = item },
                        onRenameClick = {
                            pendingRenameDownload = item
                            renameInput = item.displayName
                        },
                        onRebind = { onRebind(item) },
                        onShare = { shareDownloadFile(context, item) },
                        onCopyStreamUrl = { copyToClipboard(context, item.url, "Stream URL") },
                        onCopyPageUrl = { copyToClipboard(context, item.sourcePageUrl?.takeIf { it.isNotBlank() } ?: item.url, "Page URL") }
                    )
                }
            }
        }
    }

    // Media & Task Inspector Bottom Sheet (Item 5)
    if (inspectorDownload != null) {
        val target = inspectorDownload!!
        val targetChunks = chunksMap[target.id] ?: emptyList()

        TaskInspectorBottomSheet(
            download = target,
            chunks = targetChunks,
            onDismiss = { inspectorDownload = null },
            onShare = { shareDownloadFile(context, target) },
            onCopyStreamUrl = { copyToClipboard(context, target.url, "Stream URL") },
            onCopyPageUrl = { copyToClipboard(context, target.sourcePageUrl?.takeIf { it.isNotBlank() } ?: target.url, "Page URL") },
            onOpenFile = { openCompletedFile(context, target) },
            onRename = {
                inspectorDownload = null
                pendingRenameDownload = target
                renameInput = target.displayName
            }
        )
    }

    // Single item delete dialog
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

    if (pendingRenameDownload != null) {
        val target = pendingRenameDownload!!
        AlertDialog(
            onDismissRequest = { pendingRenameDownload = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Filename") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onRename(target.id, trimmed)
                        }
                        pendingRenameDownload = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRenameDownload = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBulkDeleteDialog) {
        val selectedCount = selectedIds.count { it.value }
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("Bulk Delete ($selectedCount tasks)") },
            text = {
                Text("Delete $selectedCount selected tasks? Choose to remove records only or delete downloaded files as well.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ids = selectedIds.filter { it.value }.keys.toList()
                        onBulkDelete(ids, true)
                        showBulkDeleteDialog = false
                        isSelectionMode = false
                        selectedIds.clear()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Files & Records")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showBulkDeleteDialog = false }) {
                        Text("Cancel")
                    }
                    FilledTonalButton(
                        onClick = {
                            val ids = selectedIds.filter { it.value }.keys.toList()
                            onBulkDelete(ids, false)
                            showBulkDeleteDialog = false
                            isSelectionMode = false
                            selectedIds.clear()
                        }
                    ) {
                        Text("Records Only")
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadItemCard(
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
    onCopyPageUrl: () -> Unit
) {
    val context = LocalContext.current
    var isMenuExpanded by remember { mutableStateOf(false) }
    val isCompleted = download.state == DownloadState.COMPLETED

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
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
                // Header: Display Name, State Badge & Overflow Menu
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

                    // 3-dots Menu for URL & File Sharing (Item 3)
                    Box {
                        IconButton(
                            onClick = { isMenuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("⋮", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            if (isCompleted || !download.destinationUri.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text("📤 Share File") },
                                    onClick = {
                                        isMenuExpanded = false
                                        onShare()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("🔗 Copy Stream URL") },
                                onClick = {
                                    isMenuExpanded = false
                                    onCopyStreamUrl()
                                }
                            )
                            if (!download.sourcePageUrl.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text("🌐 Copy Page URL") },
                                    onClick = {
                                        isMenuExpanded = false
                                        onCopyPageUrl()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("✏️ Rename") },
                                onClick = {
                                    isMenuExpanded = false
                                    onRenameClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("ℹ️ Inspect Details") },
                                onClick = {
                                    isMenuExpanded = false
                                    onCardClick()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val isHls = download.type == DownloadType.HLS
                val totalSegments = chunks.size
                val completedSegments = chunks.count { it.state == ChunkState.MERGED || it.state == ChunkState.DONE }
                val progressFraction = computeProgress(download, chunks)
                val progressPercent = (progressFraction * 100).toInt()
                val downloadedBytes = computeDownloadedBytes(download, chunks)

                // Subtitle: Type & Size / Progress
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isHls) "HLS Stream" else "Multi-Chunk HTTP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val progressDetail = when {
                        download.state == DownloadState.MERGING -> " • Processing & saving to disk…"
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

                Spacer(modifier = Modifier.height(8.dp))

                // Segmented Chunk Progress Bar
                if (download.state == DownloadState.MERGING) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                } else if (chunks.isNotEmpty()) {
                    SegmentedChunkBar(chunks = chunks, totalBytes = download.totalBytes, isHls = isHls)
                } else {
                    LinearProgressIndicator(
                        progress = { progressFraction },
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
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text("Pause", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        DownloadState.MERGING -> {
                            FilledTonalButton(
                                onClick = {},
                                enabled = false,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text("Merging…", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        DownloadState.PAUSED, DownloadState.WAITING_FOR_NETWORK -> {
                            Button(
                                onClick = onResume,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        // One-Tap Retry for Failed/Cancelled Tasks (Item 4)
                        DownloadState.FAILED, DownloadState.CANCELLED -> {
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        DownloadState.AUTH_EXPIRED -> {
                            Button(
                                onClick = onRebind,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text("Update Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        DownloadState.COMPLETED -> {
                            Button(
                                onClick = { openCompletedFile(context, download) },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text("Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = if (download.state == DownloadState.COMPLETED || download.state == DownloadState.CANCELLED) onDelete else onCancel,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (download.state == DownloadState.COMPLETED || download.state == DownloadState.CANCELLED) "Delete" else "Cancel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Media & Task Inspector Bottom Sheet (Item 5)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskInspectorBottomSheet(
    download: DownloadEntity,
    chunks: List<ChunkEntity>,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onCopyStreamUrl: () -> Unit,
    onCopyPageUrl: () -> Unit,
    onOpenFile: () -> Unit,
    onRename: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val headers = remember(download.headersJson) { HeadersCodec.decode(download.headersJson) }
    val isHls = download.type == DownloadType.HLS

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isHls) "HLS .m3u8 Stream" else "Progressive HTTP Download",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StateBadge(download.state)
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // Output Storage Path
            Text("Storage Location", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                text = download.destinationUri ?: "Not yet allocated",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!download.destinationUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onOpenFile,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Show in Files / Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onShare,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Share", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Technical details
            Text("Stream & Metadata", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            val completedChunks = chunks.count { it.state == ChunkState.MERGED || it.state == ChunkState.DONE }
            val metaText = if (isHls) {
                "Segments: $completedChunks / ${chunks.size} • MIME: ${download.mimeType.ifBlank { "video/mp4" }}\n" +
                    "ETag: ${download.etag ?: "None"} • Last-Modified: ${download.lastModified ?: "None"}"
            } else {
                "Size: ${formatFileSize(download.totalBytes)} • Resumable: ${if (download.isResumable) "Yes (HTTP 206)" else "No"}\n" +
                    "Chunks: ${chunks.size} positional workers • MIME: ${download.mimeType.ifBlank { "binary/octet-stream" }}"
            }
            Text(text = metaText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Captured Headers
            if (headers.size > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Captured Headers", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(8.dp)
                ) {
                    headers.forEach { (k, v) ->
                        val displayVal = if (k.equals("Cookie", ignoreCase = true)) "${v.take(24)}…" else v
                        Text("$k: $displayVal", fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRename,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Rename", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onCopyStreamUrl,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Stream URL", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onCopyPageUrl,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Page URL", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun openCompletedFile(context: Context, download: DownloadEntity) {
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

private fun shareDownloadFile(context: Context, download: DownloadEntity) {
    val uriStr = download.destinationUri
    if (!uriStr.isNullOrBlank()) {
        val uri = Uri.parse(uriStr)
        val mime = if (download.mimeType.contains("mpegurl")) "video/mp4" else download.mimeType.ifBlank { "*/*" }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share file"))
        } catch (_: Exception) {
            Toast.makeText(context, "No app available to share file", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "File location is not yet ready", Toast.LENGTH_SHORT).show()
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
        ?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}

private fun computeProgress(download: DownloadEntity, chunks: List<ChunkEntity>): Float {
    val isHls = download.type == DownloadType.HLS
    val totalSegments = chunks.size
    val completedSegments = chunks.count { it.state == ChunkState.MERGED || it.state == ChunkState.DONE }
    return when {
        download.state == DownloadState.COMPLETED -> 1f
        isHls && totalSegments > 0 -> (completedSegments.toFloat() / totalSegments).coerceIn(0f, 1f)
        !isHls && download.totalBytes > 0L -> {
            val downloaded = chunks.sumOf { (it.currentOffset - it.startOffset).coerceAtLeast(0L) }
            (downloaded.toFloat() / download.totalBytes).coerceIn(0f, 1f)
        }
        else -> 0f
    }
}

private fun computeDownloadedBytes(download: DownloadEntity, chunks: List<ChunkEntity>): Long {
    val isHls = download.type == DownloadType.HLS
    return if (isHls) {
        val totalSegments = chunks.size
        val completedSegments = chunks.count { it.state == ChunkState.MERGED || it.state == ChunkState.DONE }
        if (download.totalBytes > 0 && totalSegments > 0) {
            (download.totalBytes * (completedSegments.toDouble() / totalSegments)).toLong()
        } else 0L
    } else {
        chunks.sumOf { (it.currentOffset - it.startOffset).coerceAtLeast(0L) }
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
        val gapW = if (chunkCount > 1) 1.5.dp.toPx() else 0f
        val totalGaps = gapW * (chunkCount - 1)
        val usableW = (totalW - totalGaps).coerceAtLeast(1f)

        if (isHls || totalBytes <= 0L) {
            val segW = usableW / chunkCount
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

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "Adaptive / Unknown"
    val kb = bytes.toDouble() / 1024
    if (kb < 1000) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024
    return if (mb >= 1000) {
        String.format(Locale.US, "%.2f GB", mb / 1024)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}
