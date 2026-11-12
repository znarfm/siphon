package com.znarfm.siphon.ui.sniffer

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.znarfm.siphon.core.sniffer.HlsVariantInfo
import com.znarfm.siphon.core.sniffer.SniffedMedia
import com.znarfm.siphon.data.db.DownloadEntity
import com.znarfm.siphon.data.db.DownloadType
import java.util.Locale

@Immutable
data class SelectedMediaRequest(
    val media: SniffedMedia,
    val selectedVariantUrl: String?,
    val customFileName: String
)

enum class SnifferFilter(val label: String) {

    ALL("All"),
    HLS("HLS"),
    DIRECT("Direct")
}

enum class SnifferSort(val label: String) {
    RECENCY("Recency"),
    SIZE("Size"),
    NAME("Name")
}

internal fun isCurrentTabMedia(sourceUrl: String, activeUrl: String?): Boolean {
    if (activeUrl.isNullOrBlank() || sourceUrl.isBlank()) return false
    val cleanActive = activeUrl.substringBefore('#').trimEnd('/')
    val cleanSource = sourceUrl.substringBefore('#').trimEnd('/')
    if (cleanActive.equals(cleanSource, ignoreCase = true)) return true

    val activeHost = (runCatching { java.net.URI(cleanActive).host }.getOrNull()
        ?: runCatching { Uri.parse(cleanActive).host }.getOrNull())
        ?.lowercase()?.removePrefix("www.")?.removePrefix("m.") ?: return false
    val sourceHost = (runCatching { java.net.URI(cleanSource).host }.getOrNull()
        ?: runCatching { Uri.parse(cleanSource).host }.getOrNull())
        ?.lowercase()?.removePrefix("www.")?.removePrefix("m.") ?: return false
    if (activeHost != sourceHost) return false

    val activePath = (runCatching { java.net.URI(cleanActive).path }.getOrNull()
        ?: runCatching { Uri.parse(cleanActive).path }.getOrNull() ?: "").trimEnd('/')
    val sourcePath = (runCatching { java.net.URI(cleanSource).path }.getOrNull()
        ?: runCatching { Uri.parse(cleanSource).path }.getOrNull() ?: "").trimEnd('/')
    if (activePath != sourcePath) return false

    val activeRawQuery = runCatching { java.net.URI(cleanActive).rawQuery }.getOrNull()
        ?: runCatching { Uri.parse(cleanActive).query }.getOrNull()
    val sourceRawQuery = runCatching { java.net.URI(cleanSource).rawQuery }.getOrNull()
        ?: runCatching { Uri.parse(cleanSource).query }.getOrNull()

    val activeParams = parseQueryParams(activeRawQuery)
    val sourceParams = parseQueryParams(sourceRawQuery)

    for (param in listOf("v", "id", "vid", "video_id", "clip", "item")) {
        val aVal = activeParams[param]
        val sVal = sourceParams[param]
        if (aVal != null || sVal != null) {
            return aVal == sVal
        }
    }

    return true
}

private fun parseQueryParams(query: String?): Map<String, String> {
    if (query.isNullOrBlank()) return emptyMap()
    return query.split('&').mapNotNull { pair ->
        val parts = pair.split('=', limit = 2)
        if (parts.isNotEmpty() && parts[0].isNotBlank()) {
            parts[0] to (parts.getOrNull(1) ?: "")
        } else null
    }.toMap()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SnifferSheet(
    isOpen: Boolean,
    detectedMedia: List<SniffedMedia>,
    rebindTarget: DownloadEntity?,
    activeTabTitle: String? = null,
    activeTabUrl: String? = null,
    onDismiss: () -> Unit,
    onDownloadSelected: (List<SelectedMediaRequest>) -> Unit,
    onClearMedia: () -> Unit,
    onRefreshMedia: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (!isOpen) return

    var useTabTitleAsFilename by remember { mutableStateOf(true) }
    var activeFilter by remember { mutableStateOf(SnifferFilter.ALL) }
    var activeSort by remember { mutableStateOf(SnifferSort.RECENCY) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }
    var batchSplitMenuExpanded by remember { mutableStateOf(false) }

    val selectedIds = remember { mutableStateMapOf<String, Boolean>() }
    val selectedVariants = remember { mutableStateMapOf<String, HlsVariantInfo?>() }
    val customNames = remember { mutableStateMapOf<String, String>() }

    // Primary video base name for syncing subtitle defaults
    val primaryVideoBaseName = remember(detectedMedia, selectedIds.toMap(), useTabTitleAsFilename, activeTabTitle) {
        val cleanTab = activeTabTitle?.trim()?.takeIf { it.isNotBlank() && it != "New Tab" && it != "Tab" }
            ?.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")?.trim()
        if (useTabTitleAsFilename && cleanTab != null) {
            cleanTab
        } else {
            val selectedVideo = detectedMedia.firstOrNull { !it.isSubtitle && !it.isAudio && selectedIds[it.id] == true }
                ?: detectedMedia.firstOrNull { !it.isSubtitle && !it.isAudio }
                ?: detectedMedia.firstOrNull { !it.isSubtitle }
            selectedVideo?.displayName?.substringBeforeLast('.')
        }
    }

    LaunchedEffect(detectedMedia, useTabTitleAsFilename, activeTabTitle, primaryVideoBaseName) {
        detectedMedia.forEach { media ->
            if (!selectedVariants.containsKey(media.id)) {
                selectedVariants[media.id] = media.variants.firstOrNull()
            }
            val defaultName = buildDefaultFileName(
                media = media,
                useTabTitle = useTabTitleAsFilename,
                activeTabTitle = activeTabTitle,
                videoBaseName = primaryVideoBaseName
            )
            customNames[media.id] = defaultName
        }
    }

    val filteredAndSortedMedia = remember(detectedMedia, activeFilter, activeSort) {
        val filtered = when (activeFilter) {
            SnifferFilter.ALL -> detectedMedia
            SnifferFilter.HLS -> detectedMedia.filter { it.mediaType == DownloadType.HLS }
            SnifferFilter.DIRECT -> detectedMedia.filter { it.mediaType == DownloadType.HTTP }
        }
        when (activeSort) {
            SnifferSort.RECENCY -> filtered.sortedByDescending { it.detectedAt }
            SnifferSort.SIZE -> filtered.sortedByDescending { it.totalBytes }
            SnifferSort.NAME -> filtered.sortedBy { it.displayName.lowercase() }
        }
    }

    val hasMultipleSources = remember(filteredAndSortedMedia, activeTabUrl) {
        val currentCount = filteredAndSortedMedia.count { isCurrentTabMedia(it.sourcePageUrl, activeTabUrl) }
        currentCount in 1 until filteredAndSortedMedia.size
    }

    val selectedMediaList = detectedMedia.filter { selectedIds[it.id] == true }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detected Media (${detectedMedia.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (filteredAndSortedMedia.size > 1 && rebindTarget == null) {
                        val allSelected = selectedMediaList.size == filteredAndSortedMedia.size
                        TextButton(
                            onClick = {
                                if (allSelected) {
                                    selectedIds.clear()
                                } else {
                                    filteredAndSortedMedia.forEach { selectedIds[it.id] = true }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(if (allSelected) "Deselect All" else "Select All", fontSize = 12.sp)
                        }
                    }
                    if (detectedMedia.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                selectedIds.clear()
                                onClearMedia()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Clear", fontSize = 12.sp)
                        }
                    }
                    TextButton(
                        onClick = onRefreshMedia,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Refresh", fontSize = 12.sp)
                    }
                    if (rebindTarget != null) {
                        SuggestionChip(
                            onClick = { },
                            label = { Text("Rebind", fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { useTabTitleAsFilename = !useTabTitleAsFilename }
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "Tab title as filename",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = useTabTitleAsFilename,
                    onCheckedChange = { useTabTitleAsFilename = it },
                    modifier = Modifier.scale(0.8f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    SnifferFilter.values().forEachIndexed { index, filter ->
                        SegmentedButton(
                            selected = activeFilter == filter,
                            onClick = { activeFilter = filter },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = SnifferFilter.values().size),
                            label = { Text(filter.label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    OutlinedButton(
                        onClick = { sortDropdownExpanded = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Sort: ${activeSort.label}", fontSize = 11.sp)
                    }
                    DropdownMenu(
                        expanded = sortDropdownExpanded,
                        onDismissRequest = { sortDropdownExpanded = false }
                    ) {
                        SnifferSort.values().forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort.label, fontSize = 12.sp) },
                                onClick = {
                                    activeSort = sort
                                    sortDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (filteredAndSortedMedia.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (detectedMedia.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ContainedLoadingIndicator(modifier = Modifier.size(42.dp))
                            Text(
                                text = "Listening for media streams…\nPlay media in the browser or tap Refresh to sniff.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = "No media tracks match the selected filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAndSortedMedia, key = { it.id }) { media ->
                        val isSelected = selectedIds[media.id] == true
                        val currentVariant = selectedVariants[media.id] ?: media.variants.firstOrNull()
                        val currentFileName = customNames[media.id] ?: media.displayName
                        val isCurrentTab = hasMultipleSources && isCurrentTabMedia(media.sourcePageUrl, activeTabUrl)

                        SniffedMediaItemCard(
                            media = media,
                            isSelected = isSelected,
                            isCurrentTab = isCurrentTab,
                            selectedVariant = currentVariant,
                            fileName = currentFileName,
                            onToggleSelect = {
                                if (rebindTarget != null) {
                                    val next = !isSelected
                                    selectedIds.clear()
                                    if (next) selectedIds[media.id] = true
                                } else {
                                    selectedIds[media.id] = !isSelected
                                }
                            },
                            onSelectVariant = { selectedVariants[media.id] = it },
                            onFileNameChange = { customNames[media.id] = it }
                        )
                    }
                }

                AnimatedVisibility(visible = selectedMediaList.isNotEmpty()) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))

                        val primaryLabel = if (rebindTarget != null) {
                            "Rebind to Paused Task"
                        } else if (selectedMediaList.size == 1) {
                            "Download Selected (1)"
                        } else {
                            "Download Selected (${selectedMediaList.size})"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SplitButtonLayout(
                                leadingButton = {
                                    SplitButtonDefaults.LeadingButton(
                                        onClick = {
                                            val requests = selectedMediaList.map { media ->
                                                SelectedMediaRequest(
                                                    media = media,
                                                    selectedVariantUrl = selectedVariants[media.id]?.url,
                                                    customFileName = customNames[media.id]?.ifBlank { media.displayName } ?: media.displayName
                                                )
                                            }
                                            onDownloadSelected(requests)
                                        }
                                    ) {
                                        Text(
                                            text = primaryLabel,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                    }
                                },
                                trailingButton = {
                                    Box {
                                        SplitButtonDefaults.TrailingButton(
                                            checked = batchSplitMenuExpanded,
                                            onCheckedChange = { batchSplitMenuExpanded = it }
                                        ) {
                                            Text("▾", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }

                                        DropdownMenu(
                                            expanded = batchSplitMenuExpanded,
                                            onDismissRequest = { batchSplitMenuExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Download All Detected (${filteredAndSortedMedia.size})") },
                                                onClick = {
                                                    batchSplitMenuExpanded = false
                                                    val requests = filteredAndSortedMedia.map { media ->
                                                        SelectedMediaRequest(
                                                            media = media,
                                                            selectedVariantUrl = selectedVariants[media.id]?.url ?: media.variants.firstOrNull()?.url,
                                                            customFileName = customNames[media.id]?.ifBlank { media.displayName } ?: media.displayName
                                                        )
                                                    }
                                                    onDownloadSelected(requests)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(if (useTabTitleAsFilename) "Disable Tab Title as Filename" else "Toggle Tab Title as Filename") },
                                                onClick = {
                                                    batchSplitMenuExpanded = false
                                                    useTabTitleAsFilename = !useTabTitleAsFilename
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Clear Selection") },
                                                onClick = {
                                                    batchSplitMenuExpanded = false
                                                    selectedIds.clear()
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}


private fun buildDefaultFileName(
    media: SniffedMedia,
    useTabTitle: Boolean,
    activeTabTitle: String?,
    videoBaseName: String?
): String {
    val defaultExt = when {
        media.isSubtitle -> media.displayName.substringAfterLast('.', "vtt")
        media.isAudio -> media.displayName.substringAfterLast('.', if (media.mediaType == DownloadType.HLS) "m4a" else "mp3")
        media.mediaType == DownloadType.HLS -> "mp4"
        else -> media.displayName.substringAfterLast('.', "mp4")
    }
    val cleanTab = activeTabTitle?.trim()?.takeIf { it.isNotBlank() && it != "New Tab" && it != "Tab" }
        ?.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")?.trim()

    if (media.isSubtitle) {
        val base = cleanTab?.takeIf { useTabTitle } ?: videoBaseName
        if (!base.isNullOrBlank()) {
            val langSuffix = media.language?.trim()?.replace(Regex("[^a-zA-Z0-9_-]"), "")?.takeIf { it.isNotBlank() }
            return if (langSuffix != null) {
                "$base.$langSuffix.$defaultExt"
            } else {
                "$base.$defaultExt"
            }
        }
        return media.displayName
    }

    return if (useTabTitle && cleanTab != null) {
        "$cleanTab.$defaultExt"
    } else {
        media.displayName
    }
}

@Composable
private fun SniffedMediaItemCard(
    media: SniffedMedia,
    isSelected: Boolean,
    isCurrentTab: Boolean = false,
    selectedVariant: HlsVariantInfo?,
    fileName: String,
    onToggleSelect: () -> Unit,
    onSelectVariant: (HlsVariantInfo) -> Unit,
    onFileNameChange: (String) -> Unit
) {
    var variantDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelect() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() }
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = (if (media.isAudio) "🎵 " else if (media.isSubtitle) "💬 " else "🎬 ") + media.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isCurrentTab) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Current",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    val currentEstimatedBytes = selectedVariant?.estimatedBytes?.takeIf { it > 0 } ?: media.totalBytes
                    val durationLabel = if (media.durationSec > 0.0) {
                        val totalSec = media.durationSec.toInt()
                        val mins = totalSec / 60
                        val secs = totalSec % 60
                        String.format(Locale.US, " • %02d:%02d", mins, secs)
                    } else ""
                    val langLabel = if (!media.language.isNullOrBlank()) " • [${media.language}]" else ""

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatFileSize(
                                currentEstimatedBytes,
                                isEstimate = media.mediaType == DownloadType.HLS,
                                isSubtitle = media.isSubtitle
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$durationLabel$langLabel • " + when {
                                media.isSubtitle -> media.mimeType
                                media.isAudio && media.mediaType == DownloadType.HLS -> "audio/mp4"
                                media.mediaType == DownloadType.HLS -> "video/mp4"
                                else -> media.mimeType
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                val (badgeText, badgeBg, badgeFg) = when {
                    media.isSubtitle -> Triple(
                        "SUB",
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    media.isAudio -> Triple(
                        "AUDIO",
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    media.mediaType == DownloadType.HLS -> Triple(
                        "HLS",
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    else -> Triple(
                        "DIRECT",
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg
                    )
                }
            }

            // HLS Variant Selector & Deferred Rename Input (shown only when selected)
            AnimatedVisibility(visible = isSelected) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (media.variants.isNotEmpty()) {
                        Box {
                            OutlinedButton(
                                onClick = { variantDropdownExpanded = true },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                val variantSize = selectedVariant?.estimatedBytes?.takeIf { it > 0 }
                                val sizeSuffix = if (variantSize != null) " (${formatFileSize(variantSize, true)})" else ""
                                Text(
                                    text = "Quality: " + (selectedVariant?.resolution ?: "${(selectedVariant?.bandwidth ?: 0) / 1000} kbps") + sizeSuffix,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            DropdownMenu(
                                expanded = variantDropdownExpanded,
                                onDismissRequest = { variantDropdownExpanded = false }
                            ) {
                                media.variants.forEach { variant ->
                                    val vSize = variant.estimatedBytes.takeIf { it > 0 }
                                    val label = (variant.resolution ?: "${variant.bandwidth / 1000} kbps") +
                                        (if (vSize != null) " (${formatFileSize(vSize, true)})" else "")
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            onSelectVariant(variant)
                                            variantDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = fileName,
                        onValueChange = onFileNameChange,
                        label = { Text("Save filename", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long, isEstimate: Boolean = false, isSubtitle: Boolean = false): String {
    if (bytes <= 0L) return if (isSubtitle) "Subtitle" else "Stream (Adaptive)"
    val prefix = if (isEstimate) "~" else ""
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb >= 1000) {
        String.format(Locale.US, "%s%.2f GB", prefix, mb / 1024)
    } else if (mb < 0.1) {
        val kb = (bytes.toDouble() / 1024).coerceAtLeast(1.0)
        String.format(Locale.US, "%s%.0f KB", prefix, kb)
    } else {
        String.format(Locale.US, "%s%.1f MB", prefix, mb)
    }
}
