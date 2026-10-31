package com.siphon.app.ui.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siphon.app.data.db.ChunkEntity
import com.siphon.app.data.db.DownloadEntity
import com.siphon.app.data.repository.ConcurrencySettings
import com.siphon.app.data.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

class QueueViewModel(
    val downloadRepository: DownloadRepository
) : ViewModel() {

    val downloads: StateFlow<List<DownloadEntity>> = downloadRepository.downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chunksMap: StateFlow<Map<Long, List<ChunkEntity>>> = downloadRepository.chunksMap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val concurrencySettings: StateFlow<ConcurrencySettings> = downloadRepository.concurrencySettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConcurrencySettings())

    private val _filter = MutableStateFlow(QueueFilter.ALL)
    val filter: StateFlow<QueueFilter> = _filter.asStateFlow()

    private val _sortOption = MutableStateFlow(QueueSortOption.DATE_DESC)
    val sortOption: StateFlow<QueueSortOption> = _sortOption.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    fun setFilter(filter: QueueFilter) {
        _filter.value = filter
    }

    fun setSortOption(sort: QueueSortOption) {
        _sortOption.value = sort
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedIds.value = emptySet()
        }
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value
        val next = if (current.contains(id)) current - id else current + id
        _selectedIds.value = next
        _isSelectionMode.value = next.isNotEmpty()
    }

    fun selectAll(ids: List<Long>) {
        _selectedIds.value = ids.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun pauseDownload(id: Long) {
        downloadRepository.pauseDownload(id)
    }

    fun resumeDownload(id: Long) {
        viewModelScope.launch {
            downloadRepository.resumeDownload(id)
        }
    }

    fun cancelDownload(id: Long) {
        downloadRepository.cancelDownload(id)
    }

    fun pauseAll() {
        downloadRepository.pauseAll()
    }

    fun resumeAll() {
        downloadRepository.resumeAll()
    }

    fun clearCompleted() {
        viewModelScope.launch {
            downloadRepository.clearCompleted()
        }
    }

    fun retryDownload(id: Long) {
        viewModelScope.launch {
            downloadRepository.retryDownload(id)
        }
    }

    fun renameDownload(id: Long, newName: String) {
        viewModelScope.launch {
            downloadRepository.renameDownload(id, newName)
        }
    }

    fun deleteDownload(id: Long, deleteFile: Boolean = true) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(id, deleteFile)
        }
    }

    fun bulkDelete(ids: List<Long>, deleteFiles: Boolean) {
        viewModelScope.launch {
            ids.forEach { downloadRepository.deleteDownload(it, deleteFiles) }
            clearSelection()
            setSelectionMode(false)
        }
    }

    fun bulkCancel(ids: List<Long>) {
        ids.forEach { downloadRepository.cancelDownload(it) }
        clearSelection()
        setSelectionMode(false)
    }

    fun setConcurrency(maxDownloads: Int, workers: Int, bandwidthLimit: Long, wifiOnly: Boolean) {
        downloadRepository.updateConcurrency(maxDownloads, workers, bandwidthLimit, wifiOnly)
    }
}
