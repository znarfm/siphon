package com.znarfm.siphon.ui.queue

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.znarfm.siphon.data.db.ChunkEntity
import com.znarfm.siphon.data.db.DownloadEntity
import com.znarfm.siphon.data.db.DownloadFileEntity
import com.znarfm.siphon.data.db.DownloadState
import com.znarfm.siphon.data.db.SiphonDatabase
import com.znarfm.siphon.ui.adaptive.LocalWindowSizeClass
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.TabularNumberStyle
import kotlinx.coroutines.flow.map
import com.znarfm.siphon.ui.theme.SiphonIcons


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    onClearCompleted: () -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onRetry: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onBulkDelete: (List<Long>, Boolean) -> Unit,
    onBulkCancel: (List<Long>) -> Unit,
    onOpenDownloadSettings: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    downloadFilesMap: Map<Long, List<DownloadFileEntity>>? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val observedFilesMap by remember(context) {
        SiphonDatabase.get(context).downloadDao().observeAllDownloadFiles()
            .map { list -> list.groupBy { it.downloadId } }
    }.collectAsState(initial = emptyMap())
    val effectiveFilesMap = downloadFilesMap ?: observedFilesMap
    var isSearchExpanded by remember { mutableStateOf(searchQuery.isNotBlank()) }

    var selectionBackProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = isSelectionMode) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                selectionBackProgress = backEvent.progress
            }
            onSetSelectionMode(false)
            onClearSelection()
        } finally {
            selectionBackProgress = 0f
        }
    }


    val isExpanded = LocalWindowSizeClass.current.isExpanded
    var pendingDeleteDownload by remember { mutableStateOf<DownloadEntity?>(null) }
    var pendingRenameDownload by remember { mutableStateOf<DownloadEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var inspectorDownloadId by remember { mutableStateOf<Long?>(null) }
    var showTorrentFilesInPane by remember { mutableStateOf(false) }
    var torrentFilesDownload by remember { mutableStateOf<DownloadEntity?>(null) }

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

    val listPane: @Composable (Modifier) -> Unit = { paneModifier ->
        Column(modifier = paneModifier) {
            // Header Surface (Chrome Android Downloads TopAppBar + M3 Expressive styling)
            Surface(
            tonalElevation = 3.dp,
            shape = ExpressiveShapeTokens.HeaderSheetShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
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
                                Icon(
                                    imageVector = SiphonIcons.Close,
                                    contentDescription = "Exit selection mode"
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$selectedCount selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
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
                                    shape = ExpressiveShapeTokens.PillShape,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { showBulkDeleteDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = ExpressiveShapeTokens.PillShape,
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = SiphonIcons.ArrowBack,
                                contentDescription = "Back"
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Downloads",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$activeCount active • $maxConcurrentDownloads max • $workersPerDownload workers" +
                                    (if (bandwidthLimitBytes > 0) " • ${bandwidthLimitBytes / (1024 * 1024)} MB/s" else "") +
                                    (if (isWifiOnly) " • Wi-Fi" else ""),
                                style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                            Icon(
                                imageVector = SiphonIcons.Search,
                                contentDescription = "Search downloads"
                            )
                        }
                        IconButton(onClick = onOpenDownloadSettings) {
                            Icon(
                                imageVector = SiphonIcons.Settings,
                                contentDescription = "Download settings"
                            )
                        }
                    }

                    if (activeCount > 0 || pausedCount > 0 || completedCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeCount > 0) {
                                FilledTonalButton(
                                    onClick = onPauseAll,
                                    shape = ExpressiveShapeTokens.PillShape,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = SiphonIcons.Pause,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pause All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (pausedCount > 0) {
                                FilledTonalButton(
                                    onClick = onResumeAll,
                                    shape = ExpressiveShapeTokens.PillShape,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = SiphonIcons.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (completedCount > 0) {
                                OutlinedButton(
                                    onClick = onClearCompleted,
                                    shape = ExpressiveShapeTokens.PillShape,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = SiphonIcons.DeleteSweep,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search & Filter Toolbar (Item 2) with MaterialTheme.motionScheme.defaultSpatialSpec()
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = expandVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
            exit = shrinkVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut()
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
                                Icon(
                                    imageVector = SiphonIcons.Close,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(18.dp)
                                )
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
                            shape = ExpressiveShapeTokens.PillShape,
                            label = { Text(opt.label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        // Filter row upgraded to Material 3 Expressive SingleChoiceSegmentedButtonRow with counts
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            QueueFilter.entries.forEachIndexed { index, filter ->
                val count = when (filter) {
                    QueueFilter.ALL -> downloads.size
                    QueueFilter.ACTIVE -> activeCount
                    QueueFilter.PAUSED -> pausedCount
                    QueueFilter.COMPLETED -> completedCount
                }
                SegmentedButton(
                    selected = selectedFilter == filter,
                    onClick = { onSelectFilter(filter) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = QueueFilter.entries.size),
                    label = {
                        Text(
                            text = "${filter.label} ($count)",
                            fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            style = TabularNumberStyle
                        )
                    }
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
                                inspectorDownloadId = item.id
                                showTorrentFilesInPane = false
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
                        onCopyPageUrl = { copyToClipboard(context, item.sourcePageUrl?.takeIf { it.isNotBlank() } ?: item.url, "Page URL") },
                        onOpen = {
                            val itemFiles = effectiveFilesMap[item.id].orEmpty()
                            if (itemFiles.size > 1) {
                                if (isExpanded) {
                                    inspectorDownloadId = item.id
                                    showTorrentFilesInPane = true
                                } else {
                                    torrentFilesDownload = item
                                }
                            } else {
                                openCompletedFile(context, item)
                            }
                        }
                    )
                }
            }
        }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        if (isExpanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                listPane(
                    Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                )
                VerticalDivider()
                Box(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    val activeInspectorDownload = downloads.find { it.id == inspectorDownloadId } ?: downloads.firstOrNull()
                    if (activeInspectorDownload == null) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = SiphonIcons.Download,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Select a download",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap a task on the left to inspect its chunks, URLs, and actions.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val targetFiles = effectiveFilesMap[activeInspectorDownload.id].orEmpty()
                        if (showTorrentFilesInPane) {
                            TorrentFilesContent(
                                download = activeInspectorDownload,
                                files = targetFiles,
                                onBack = { showTorrentFilesInPane = false },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val targetChunks = chunksMap[activeInspectorDownload.id] ?: emptyList()
                            TaskInspectorContent(
                                download = activeInspectorDownload,
                                chunks = targetChunks,
                                onShare = { shareDownloadFile(context, activeInspectorDownload) },
                                onCopyStreamUrl = { copyToClipboard(context, activeInspectorDownload.url, "Stream URL") },
                                onCopyPageUrl = {
                                    copyToClipboard(
                                        context,
                                        activeInspectorDownload.sourcePageUrl?.takeIf { it.isNotBlank() } ?: activeInspectorDownload.url,
                                        "Page URL"
                                    )
                                },
                                onOpenFile = {
                                    if (targetFiles.size > 1) {
                                        showTorrentFilesInPane = true
                                    } else {
                                        openCompletedFile(context, activeInspectorDownload)
                                    }
                                },
                                onRename = {
                                    pendingRenameDownload = activeInspectorDownload
                                    renameInput = activeInspectorDownload.displayName
                                },
                                files = targetFiles,
                                onViewFiles = if (targetFiles.size > 1) {
                                    { showTorrentFilesInPane = true }
                                } else {
                                    null
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        } else {
            listPane(Modifier.fillMaxSize())
        }
    }

    // Media & Task Inspector Bottom Sheet (Item 5)
    val sheetInspectorDownload = if (!isExpanded) downloads.find { it.id == inspectorDownloadId } else null
    if (sheetInspectorDownload != null) {
        val targetChunks = chunksMap[sheetInspectorDownload.id] ?: emptyList()
        val targetFiles = effectiveFilesMap[sheetInspectorDownload.id].orEmpty()

        TaskInspectorBottomSheet(
            download = sheetInspectorDownload,
            chunks = targetChunks,
            onDismiss = { inspectorDownloadId = null },
            onShare = { shareDownloadFile(context, sheetInspectorDownload) },
            onCopyStreamUrl = { copyToClipboard(context, sheetInspectorDownload.url, "Stream URL") },
            onCopyPageUrl = {
                copyToClipboard(
                    context,
                    sheetInspectorDownload.sourcePageUrl?.takeIf { it.isNotBlank() } ?: sheetInspectorDownload.url,
                    "Page URL"
                )
            },
            onOpenFile = {
                if (targetFiles.size > 1) {
                    inspectorDownloadId = null
                    torrentFilesDownload = sheetInspectorDownload
                } else {
                    openCompletedFile(context, sheetInspectorDownload)
                }
            },
            onRename = {
                inspectorDownloadId = null
                pendingRenameDownload = sheetInspectorDownload
                renameInput = sheetInspectorDownload.displayName
            },
            files = targetFiles,
            onViewFiles = if (targetFiles.size > 1) {
                {
                    inspectorDownloadId = null
                    torrentFilesDownload = sheetInspectorDownload
                }
            } else {
                null
            }
        )
    }

    if (!isExpanded && torrentFilesDownload != null) {
        val target = torrentFilesDownload!!
        val targetFiles = effectiveFilesMap[target.id].orEmpty()
        TorrentFilesSheet(
            download = target,
            files = targetFiles,
            onDismiss = { torrentFilesDownload = null }
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
}

