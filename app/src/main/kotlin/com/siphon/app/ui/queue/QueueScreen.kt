package com.siphon.app.ui.queue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.siphon.app.data.db.ChunkEntity
import com.siphon.app.data.db.DownloadEntity
import com.siphon.app.data.db.DownloadState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    downloads: List<DownloadEntity>,
    chunksMap: Map<Long, List<ChunkEntity>>,
    maxConcurrentDownloads: Int,
    workersPerDownload: Int,
    bandwidthLimitBytes: Long = 0L,
    isWifiOnly: Boolean = false,
    selectedFilter: QueueFilter = QueueFilter.ALL,
    onSelectFilter: (QueueFilter) -> Unit = {},
    selectedSort: QueueSortOption = QueueSortOption.DATE_DESC,
    onSelectSort: (QueueSortOption) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    isSelectionMode: Boolean = false,
    onSetSelectionMode: (Boolean) -> Unit = {},
    selectedIds: Set<Long> = emptySet(),
    onToggleSelectId: (Long) -> Unit = {},
    onSelectAllIds: (List<Long>) -> Unit = {},
    onClearSelection: () -> Unit = {},
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
    var isSearchExpanded by remember { mutableStateOf(searchQuery.isNotBlank()) }

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
                        val selectedCount = selectedIds.size
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                onSetSelectionMode(false)
                                onClearSelection()
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
                            val allSelected = filteredList.isNotEmpty() && filteredList.all { selectedIds.contains(it.id) }
                            TextButton(onClick = {
                                if (allSelected) {
                                    onClearSelection()
                                } else {
                                    onSelectAllIds(filteredList.map { it.id })
                                }
                            }) {
                                Text(if (allSelected) "Deselect" else "All", fontSize = 12.sp)
                            }

                            if (selectedCount > 0) {
                                FilledTonalButton(
                                    onClick = {
                                        onBulkCancel(selectedIds.toList())
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
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search by filename, URL, or domain…", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
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
                            onClick = { onSelectSort(opt) },
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
                    onClick = { onSelectFilter(filter) },
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
                    val isChecked = selectedIds.contains(item.id)

                    DownloadItemCard(
                        download = item,
                        chunks = chunks,
                        isSelectionMode = isSelectionMode,
                        isSelected = isChecked,
                        onToggleSelect = {
                            onToggleSelectId(item.id)
                        },
                        onCardClick = {
                            if (isSelectionMode) {
                                onToggleSelectId(item.id)
                            } else {
                                inspectorDownload = item
                            }
                        },
                        onCardLongClick = {
                            if (!isSelectionMode) {
                                onSetSelectionMode(true)
                                onToggleSelectId(item.id)
                            } else {
                                onToggleSelectId(item.id)
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
        val selectedCount = selectedIds.size
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("Bulk Delete ($selectedCount tasks)") },
            text = {
                Text("Delete $selectedCount selected tasks? Choose to remove records only or delete downloaded files as well.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ids = selectedIds.toList()
                        onBulkDelete(ids, true)
                        showBulkDeleteDialog = false
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
                            val ids = selectedIds.toList()
                            onBulkDelete(ids, false)
                            showBulkDeleteDialog = false
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

