package com.znarfm.siphon.ui.browser

import androidx.activity.compose.PredictiveBackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.znarfm.siphon.data.db.BookmarkEntity
import com.znarfm.siphon.data.db.HistoryEntity
import com.znarfm.siphon.ui.adaptive.LocalWindowSizeClass
import com.znarfm.siphon.ui.common.ClearHistoryDialog
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.TabularNumberStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.znarfm.siphon.ui.theme.SiphonIcons

@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    onOpenUrl: (url: String, inNewTab: Boolean, isIncognito: Boolean) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    SearchableListGridScaffold(
        title = "Bookmarks",
        searchPlaceholder = "Search bookmarks…",
        items = bookmarks,
        key = { it.id },
        matchesSearch = { bookmark, query ->
            bookmark.title.contains(query, ignoreCase = true) ||
                bookmark.url.contains(query, ignoreCase = true)
        },
        emptyIcon = SiphonIcons.BookmarkBorder,
        emptyTitle = "No bookmarks yet",
        emptySubtitle = "Pages you star while browsing will appear here.",
        emptySearchTitle = "No matching bookmarks",
        onBack = onBack,
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
            .clip(ExpressiveShapeTokens.RestingCardShape)
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = ExpressiveShapeTokens.PillShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = SiphonIcons.Star,
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
                        imageVector = SiphonIcons.MoreVert,
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
                                imageVector = SiphonIcons.OpenInNew,
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
                                imageVector = SiphonIcons.VisibilityOff,
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
                                imageVector = SiphonIcons.ContentCopy,
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
                                imageVector = SiphonIcons.Outlined.Delete,
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
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    }

    if (showClearConfirmDialog) {
        ClearHistoryDialog(
            onConfirm = {
                showClearConfirmDialog = false
                onClearHistory()
            },
            onDismiss = { showClearConfirmDialog = false }
        )
    }

    SearchableListGridScaffold(
        title = "History",
        searchPlaceholder = "Search history…",
        items = history,
        key = { it.id },
        matchesSearch = { entry, query ->
            entry.title.contains(query, ignoreCase = true) ||
                entry.url.contains(query, ignoreCase = true)
        },
        emptyIcon = SiphonIcons.History,
        emptyTitle = "No browsing history",
        emptySubtitle = "Pages you visit will be listed here.",
        emptySearchTitle = "No matching history",
        onBack = onBack,
        modifier = modifier,
        extraActions = {
            if (history.isNotEmpty()) {
                FilledTonalButton(
                    onClick = { showClearConfirmDialog = true },
                    shape = ExpressiveShapeTokens.PillShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
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

@Composable
private fun <T> SearchableListGridScaffold(
    title: String,
    searchPlaceholder: String,
    items: List<T>,
    key: (T) -> Any,
    matchesSearch: (T, String) -> Boolean,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptySubtitle: String,
    emptySearchTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    emptySearchSubtitle: String = "Try a different title or URL keyword.",
    snackbarHost: @Composable () -> Unit = {},
    extraActions: @Composable () -> Unit = {},
    itemContent: @Composable (T) -> Unit
) {
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchBackProgress by remember { mutableFloatStateOf(0f) }
    val isTablet = LocalWindowSizeClass.current.isTablet

    PredictiveBackHandler(enabled = isSearching) { progress ->
        try {
            progress.collect { backEvent ->
                searchBackProgress = backEvent.progress
            }
            searchBackProgress = 0f
            isSearching = false
            searchQuery = ""
        } catch (e: CancellationException) {
            searchBackProgress = 0f
        }
    }

    val filteredItems = remember(items, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            items
        } else {
            items.filter { matchesSearch(it, query) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = snackbarHost,
        topBar = {
            SearchableTopAppBar(
                title = title,
                searchPlaceholder = searchPlaceholder,
                isSearching = isSearching,
                searchQuery = searchQuery,
                searchBackProgress = searchBackProgress,
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
                extraActions = extraActions
            )
        }
    ) { innerPadding ->
        if (filteredItems.isEmpty()) {
            EmptyStateView(
                icon = if (searchQuery.isNotBlank()) SiphonIcons.SearchOff else emptyIcon,
                title = if (searchQuery.isNotBlank()) emptySearchTitle else emptyTitle,
                subtitle = if (searchQuery.isNotBlank()) emptySearchSubtitle else emptySubtitle,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else if (isTablet) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 320.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                gridItems(
                    items = filteredItems,
                    key = key
                ) { item ->
                    itemContent(item)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredItems,
                    key = key
                ) { item ->
                    itemContent(item)
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
            .clip(ExpressiveShapeTokens.RestingCardShape)
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = ExpressiveShapeTokens.PillShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = SiphonIcons.History,
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
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = ExpressiveShapeTokens.PillShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = SiphonIcons.Outlined.Delete,
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
    searchBackProgress: Float = 0f,
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

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp,
        shape = ExpressiveShapeTokens.HeaderSheetShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = SiphonIcons.ArrowBack,
                    contentDescription = "Back"
                )
            }

            if (isSearching) {
                Surface(
                    shape = ExpressiveShapeTokens.PillShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .graphicsLayer {
                            val s = 1f - (searchBackProgress * 0.08f)
                            scaleX = s
                            scaleY = s
                            alpha = 1f - (searchBackProgress * 0.85f)
                            translationX = searchBackProgress * 16.dp.toPx()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = SiphonIcons.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = searchPlaceholder,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = {
                        if (searchQuery.isNotEmpty()) {
                            onSearchQueryChange("")
                        } else {
                            onToggleSearch()
                        }
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.graphicsLayer {
                        val s = 1f - (searchBackProgress * 0.2f)
                        scaleX = s
                        scaleY = s
                        alpha = 1f - searchBackProgress
                    }
                ) {
                    Icon(
                        imageVector = SiphonIcons.Close,
                        contentDescription = "Close search"
                    )
                }
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )

                FilledTonalIconButton(
                    onClick = onToggleSearch,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = SiphonIcons.Search,
                        contentDescription = "Search"
                    )
                }

                extraActions()
            }
        }
    }
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
            shape = ExpressiveShapeTokens.ActiveCardShape,
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
