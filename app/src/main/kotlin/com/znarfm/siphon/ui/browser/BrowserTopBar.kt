package com.znarfm.siphon.ui.browser

import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.znarfm.siphon.data.db.BookmarkEntity
import com.znarfm.siphon.data.db.HistoryEntity

enum class QuickSearchFilter {
    ALL, BOOKMARKS, HISTORY
}

fun sanitizeUrl(input: String): String {
    val trimmed = input.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
        else -> "https://www.google.com/search?q=" + Uri.encode(trimmed)
    }
}

@Composable
fun BrowserTopBar(
    canGoBack: Boolean,
    onGoBack: () -> Unit,
    onToggleAdBlock: () -> Unit,
    isAdBlockEnabled: Boolean,
    urlTextFieldValue: TextFieldValue,
    onUrlChange: (TextFieldValue) -> Unit,
    isUrlBarFocused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onCancelFocus: () -> Unit = {},
    onSubmitUrl: (String) -> Unit,
    onClearUrl: () -> Unit,
    pageProgress: Int,
    onReload: () -> Unit,
    onStop: () -> Unit,
    isCurrentBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    detectedMediaCount: Int,
    onOpenSniffer: () -> Unit,
    onOpenMenu: () -> Unit,
    bookmarks: List<BookmarkEntity>,
    history: List<HistoryEntity>,
    onSelectHistoryOrBookmark: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDeleteHistory: (Long) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
    menuContent: @Composable () -> Unit = {}
) {
    var searchFilter by remember { mutableStateOf(QuickSearchFilter.ALL) }

    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isUrlBarFocused) {
                    IconButton(
                        onClick = onCancelFocus,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Text("←", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                } else if (canGoBack) {
                    IconButton(
                        onClick = onGoBack,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Sleek Expressive URL Bar Capsule
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleAdBlock,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text(
                                text = "🛡",
                                fontSize = 13.sp,
                                color = if (isAdBlockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        BasicTextField(
                            value = urlTextFieldValue,
                            onValueChange = { newValue ->
                                if (!isUrlBarFocused) {
                                    onFocusChange(true)
                                    val text = newValue.text
                                    onUrlChange(TextFieldValue(text, TextRange(text.length)))
                                } else {
                                    onUrlChange(newValue)
                                }
                            },
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
                                    val target = sanitizeUrl(urlTextFieldValue.text)
                                    onSubmitUrl(target)
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { state ->
                                    if (state.isFocused && !isUrlBarFocused) {
                                        onFocusChange(true)
                                        val text = urlTextFieldValue.text
                                        onUrlChange(TextFieldValue(text, TextRange(0, text.length)))
                                    }
                                }
                        )

                        if (urlTextFieldValue.text.isNotEmpty() && isUrlBarFocused) {
                            IconButton(
                                onClick = onClearUrl,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else if (!isUrlBarFocused) {
                            if (pageProgress in 1..99) {
                                IconButton(
                                    onClick = onStop,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("✕", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                IconButton(
                                    onClick = onReload,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("↻", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            IconButton(
                                onClick = onToggleBookmark,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Text(
                                    text = if (isCurrentBookmarked) "★" else "☆",
                                    fontSize = 14.sp,
                                    color = if (isCurrentBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (!isUrlBarFocused) {
                    Spacer(modifier = Modifier.width(6.dp))

                    BadgedBox(
                        badge = {
                            if (detectedMediaCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text(detectedMediaCount.toString(), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onOpenSniffer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("⚡", fontSize = 17.sp)
                        }
                    }

                    Box {
                        IconButton(
                            onClick = onOpenMenu,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("⋮", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        menuContent()
                    }
                }
            }

            if (pageProgress in 1..99) {
                LinearWavyProgressIndicator(
                    progress = { pageProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }

            // Fast Search & Filter Panel (Bookmarks + History)
            if (isUrlBarFocused) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val query = urlTextFieldValue.text.trim().lowercase()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filters = QuickSearchFilter.entries
                        SingleChoiceSegmentedButtonRow {
                            filters.forEachIndexed { index, filter ->
                                val labelText = when (filter) {
                                    QuickSearchFilter.ALL -> "All"
                                    QuickSearchFilter.BOOKMARKS -> "⭐ Bookmarks (${bookmarks.size})"
                                    QuickSearchFilter.HISTORY -> "🕒 History (${history.size})"
                                }
                                SegmentedButton(
                                    selected = searchFilter == filter,
                                    onClick = { searchFilter = filter },
                                    shape = SegmentedButtonDefaults.itemShape(index, filters.size),
                                    label = { Text(labelText, fontSize = 11.sp) }
                                )
                            }
                        }
                        if (searchFilter == QuickSearchFilter.HISTORY && history.isNotEmpty()) {
                            TextButton(
                                onClick = onClearHistory,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("Clear All", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    val matchedBookmarks = if (searchFilter != QuickSearchFilter.HISTORY) {
                        bookmarks.filter { query.isEmpty() || it.title.lowercase().contains(query) || it.url.lowercase().contains(query) }
                    } else emptyList()

                    val matchedHistory = if (searchFilter != QuickSearchFilter.BOOKMARKS) {
                        history.filter { query.isEmpty() || it.title.lowercase().contains(query) || it.url.lowercase().contains(query) }
                    } else emptyList()

                    if (matchedBookmarks.isEmpty() && matchedHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No matching bookmarks or history", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                        ) {
                            if (matchedBookmarks.isNotEmpty()) {
                                items(matchedBookmarks, key = { "bm_${it.id}" }) { bm ->
                                    SearchResultItemRow(
                                        icon = "⭐",
                                        title = bm.title,
                                        url = bm.url,
                                        onSelect = { onSelectHistoryOrBookmark(bm.url) },
                                        onDelete = { onDeleteBookmark(bm.id) }
                                    )
                                }
                            }

                            if (matchedHistory.isNotEmpty()) {
                                items(matchedHistory, key = { "hi_${it.id}" }) { hi ->
                                    SearchResultItemRow(
                                        icon = "🕒",
                                        title = hi.title,
                                        url = hi.url,
                                        onSelect = { onSelectHistoryOrBookmark(hi.url) },
                                        onDelete = { onDeleteHistory(hi.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItemRow(
    icon: String,
    title: String,
    url: String,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title.ifBlank { url },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = url,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(22.dp)
        ) {
            Text("✕", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
