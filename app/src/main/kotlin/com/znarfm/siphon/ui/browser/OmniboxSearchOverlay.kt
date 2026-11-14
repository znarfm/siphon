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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.TabularNumberStyle
import java.net.URLEncoder
import com.znarfm.siphon.ui.theme.SiphonIcons

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
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Expressive Omnibox Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalIconButton(
                    onClick = onDismiss,
                    shape = ExpressiveShapeTokens.PillShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = SiphonIcons.ArrowBack,
                        contentDescription = "Close search"
                    )
                }

                Surface(
                    shape = ExpressiveShapeTokens.PillShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = SiphonIcons.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (urlTextFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Search or type URL",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = urlTextFieldValue,
                                onValueChange = onUrlChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
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
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = SiphonIcons.Close,
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
                        shape = ExpressiveShapeTokens.PillShape,
                        leadingIcon = {
                            Icon(
                                imageVector = SiphonIcons.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Paste & Go: $clipboardText",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                    style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
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
                            imageVector = SiphonIcons.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matchedBookmarks, key = { "bm_${it.id}" }) { bookmark ->
                        OmniboxSuggestionCard(
                            icon = SiphonIcons.Star,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            shape = ExpressiveShapeTokens.ActiveCardShape,
                            title = bookmark.title,
                            url = bookmark.url,
                            onSelect = { onSelectUrl(bookmark.url) },
                            onDelete = { onDeleteBookmark(bookmark.id) }
                        )
                    }

                    items(matchedHistory, key = { "hi_${it.id}" }) { entry ->
                        OmniboxSuggestionCard(
                            icon = SiphonIcons.History,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = ExpressiveShapeTokens.RestingCardShape,
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
private fun OmniboxSuggestionCard(
    icon: ImageVector,
    iconTint: Color,
    iconContainerColor: Color,
    shape: Shape,
    title: String,
    url: String,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = ExpressiveShapeTokens.ActiveCardShape,
                color = iconContainerColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { url },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                    imageVector = SiphonIcons.Close,
                    contentDescription = "Remove entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
