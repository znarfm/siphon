package com.znarfm.siphon.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.znarfm.siphon.data.db.BookmarkEntity
import com.znarfm.siphon.data.db.HistoryEntity
import java.net.URLEncoder

private enum class OmniboxFilter(val label: String) {
    ALL("All"),
    BOOKMARKS("Bookmarks"),
    HISTORY("History")
}

fun sanitizeUrl(
    input: String,
    searchEngineBase: String = "https://www.google.com/search?q="
): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return NEW_TAB_URL
    return when {
        trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true) -> trimmed
        !trimmed.contains(" ") && (trimmed.contains(".") || trimmed.startsWith("localhost", ignoreCase = true)) ->
            "https://$trimmed"
        else -> searchEngineBase + URLEncoder.encode(trimmed, "UTF-8")
    }
}

@Composable
fun OmniboxSearchOverlay(
    urlTextFieldValue: TextFieldValue,
    onUrlChange: (TextFieldValue) -> Unit,
    bookmarks: List<BookmarkEntity>,
    history: List<HistoryEntity>,
    searchEngineBase: String,
    onSubmitUrl: (String) -> Unit,
    onSelectUrl: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDeleteHistory: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(OmniboxFilter.ALL) }
    val focusRequester = remember { FocusRequester() }
    val clipboardManager = LocalClipboardManager.current
    val clipboardText = remember(urlTextFieldValue) {
        clipboardManager.getText()?.text?.trim().orEmpty()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val query = urlTextFieldValue.text.trim().lowercase()
    val matchedBookmarks = remember(bookmarks, query, selectedFilter) {
        if (selectedFilter == OmniboxFilter.HISTORY) {
            emptyList()
        } else {
            bookmarks.filter {
                query.isEmpty() ||
                    it.title.lowercase().contains(query) ||
                    it.url.lowercase().contains(query)
            }
        }
    }
    val matchedHistory = remember(history, query, selectedFilter) {
        if (selectedFilter == OmniboxFilter.BOOKMARKS) {
            emptyList()
        } else {
            history.filter {
                query.isEmpty() ||
                    it.title.lowercase().contains(query) ||
                    it.url.lowercase().contains(query)
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Omnibox Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Close search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (urlTextFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Search or type URL",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = urlTextFieldValue,
                                onValueChange = onUrlChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        val target = sanitizeUrl(urlTextFieldValue.text, searchEngineBase)
                                        onSubmitUrl(target)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }

                        if (urlTextFieldValue.text.isNotEmpty()) {
                            IconButton(
                                onClick = { onUrlChange(TextFieldValue("")) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear URL",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Paste & Go Chip when clipboard is non-empty
            if (clipboardText.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {
                            val target = sanitizeUrl(clipboardText, searchEngineBase)
                            onSubmitUrl(target)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Paste & Go: $clipboardText",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            // Filter Segmented Buttons + Clear All History
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filters = OmniboxFilter.entries
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    filters.forEachIndexed { index, filter ->
                        val countSuffix = when (filter) {
                            OmniboxFilter.ALL -> ""
                            OmniboxFilter.BOOKMARKS -> " (${bookmarks.size})"
                            OmniboxFilter.HISTORY -> " (${history.size})"
                        }
                        SegmentedButton(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            shape = SegmentedButtonDefaults.itemShape(index, filters.size),
                            label = {
                                Text(
                                    text = filter.label + countSuffix,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                if (selectedFilter != OmniboxFilter.BOOKMARKS && history.isNotEmpty()) {
                    TextButton(
                        onClick = onClearHistory,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear All",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

            // Matching Bookmarks & History List
            if (matchedBookmarks.isEmpty() && matchedHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching bookmarks or history",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(matchedBookmarks, key = { "bm_${it.id}" }) { bookmark ->
                        OmniboxSuggestionRow(
                            icon = Icons.Rounded.Star,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = bookmark.title,
                            url = bookmark.url,
                            onSelect = { onSelectUrl(bookmark.url) },
                            onDelete = { onDeleteBookmark(bookmark.id) }
                        )
                    }

                    items(matchedHistory, key = { "hi_${it.id}" }) { entry ->
                        OmniboxSuggestionRow(
                            icon = Icons.Rounded.History,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            title = entry.title,
                            url = entry.url,
                            onSelect = { onSelectUrl(entry.url) },
                            onDelete = { onDeleteHistory(entry.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OmniboxSuggestionRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    url: String,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.ifBlank { url },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove entry",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
