package com.znarfm.siphon.ui.browser

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.znarfm.siphon.data.db.BookmarkEntity
import com.znarfm.siphon.data.db.HistoryEntity
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.TabularNumberStyle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    onOpenUrl: (url: String, inNewTab: Boolean, isIncognito: Boolean) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = isSearching) {
        isSearching = false
        searchQuery = ""
    }

    val filteredBookmarks = remember(bookmarks, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            bookmarks
        } else {
            bookmarks.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.url.contains(query, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            SearchableTopAppBar(
                title = "Bookmarks",
                searchPlaceholder = "Search bookmarks…",
                isSearching = isSearching,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onToggleSearch = {
                    isSearching = !isSearching
                    if (!isSearching) searchQuery = ""
                },
                onBack = {
                    if (isSearching) {
                        isSearching = false
                        searchQuery = ""
                    } else {
                        onBack()
                    }
                }
            )
        }
    ) { innerPadding ->
        if (filteredBookmarks.isEmpty()) {
            EmptyStateView(
                icon = if (searchQuery.isNotBlank()) Icons.Rounded.SearchOff else Icons.Rounded.BookmarkBorder,
                title = if (searchQuery.isNotBlank()) "No matching bookmarks" else "No bookmarks yet",
                subtitle = if (searchQuery.isNotBlank()) {
                    "Try a different title or URL keyword."
                } else {
                    "Pages you star while browsing will appear here."
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredBookmarks,
                    key = { it.id }
                ) { bookmark ->
                    BookmarkItemRow(
                        bookmark = bookmark,
                        onOpen = { onOpenUrl(bookmark.url, false, false) },
                        onOpenNewTab = { onOpenUrl(bookmark.url, true, false) },
                        onOpenIncognito = { onOpenUrl(bookmark.url, true, true) },
                        onCopyUrl = {
                            clipboardManager.setText(AnnotatedString(bookmark.url))
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("URL copied to clipboard")
                            }
                        },
                        onDelete = { onDeleteBookmark(bookmark.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkItemRow(
    bookmark: BookmarkEntity,
    onOpen: () -> Unit,
    onOpenNewTab: () -> Unit,
    onOpenIncognito: () -> Unit,
    onCopyUrl: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = ExpressiveShapeTokens.RestingCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.title.ifBlank { bookmark.url },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = bookmark.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Bookmark options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open in new tab") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenNewTab()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open in incognito tab") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.VisibilityOff,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenIncognito()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy URL") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onCopyUrl()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    history: List<HistoryEntity>,
    onOpenUrl: (url: String, inNewTab: Boolean, isIncognito: Boolean) -> Unit,
    onDeleteHistory: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isSearching) {
        isSearching = false
        searchQuery = ""
    }

    val dateFormatter = remember {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    }

    val filteredHistory = remember(history, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            history
        } else {
            history.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.url.contains(query, ignoreCase = true)
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Clear browsing history?") },
            text = {
                Text("This will permanently delete all visited pages from your browsing history.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearHistory()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SearchableTopAppBar(
                title = "History",
                searchPlaceholder = "Search history…",
                isSearching = isSearching,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onToggleSearch = {
                    isSearching = !isSearching
                    if (!isSearching) searchQuery = ""
                },
                onBack = {
                    if (isSearching) {
                        isSearching = false
                        searchQuery = ""
                    } else {
                        onBack()
                    }
                },
                extraActions = {
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirmDialog = true }) {
                            Text(
                                text = "Clear",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (filteredHistory.isEmpty()) {
            EmptyStateView(
                icon = if (searchQuery.isNotBlank()) Icons.Rounded.SearchOff else Icons.Rounded.History,
                title = if (searchQuery.isNotBlank()) "No matching history" else "No browsing history",
                subtitle = if (searchQuery.isNotBlank()) {
                    "Try a different title or URL keyword."
                } else {
                    "Pages you visit will be listed here."
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredHistory,
                    key = { it.id }
                ) { entry ->
                    val formattedTime = remember(entry.visitedAt) {
                        dateFormatter.format(Date(entry.visitedAt))
                    }
                    HistoryItemRow(
                        entry = entry,
                        formattedTime = formattedTime,
                        onOpen = { onOpenUrl(entry.url, false, false) },
                        onDelete = { onDeleteHistory(entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemRow(
    entry: HistoryEntity,
    formattedTime: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = ExpressiveShapeTokens.RestingCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title.ifBlank { entry.url },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete history entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SearchableTopAppBar(
    title: String,
    searchPlaceholder: String,
    isSearching: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onBack: () -> Unit,
    extraActions: @Composable () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        title = {
            if (isSearching) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = searchPlaceholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = {
            if (isSearching) {
                IconButton(
                    onClick = {
                        if (searchQuery.isNotEmpty()) {
                            onSearchQueryChange("")
                        } else {
                            onToggleSearch()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close search"
                    )
                }
            } else {
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search"
                    )
                }
                extraActions()
            }
        }
    )
}

@Composable
private fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
