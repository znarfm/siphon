package com.znarfm.siphon.ui.browser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TabSwitcherScreen(
    tabs: List<BrowserTab>,
    activeTabId: String,
    thumbnails: Map<String, ImageBitmap>,
    lastClosedTab: BrowserTab?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onUndoCloseTab: () -> Unit,
    onDismissUndo: () -> Unit,
    onNewTab: (Boolean) -> Unit,
    onCloseAllTabs: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isIncognitoTabSelected by remember {
        mutableStateOf(tabs.firstOrNull { it.id == activeTabId }?.isIncognito ?: false)
    }
    var overflowExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val normalCount = remember(tabs) { tabs.count { !it.isIncognito } }
    val incognitoCount = remember(tabs) { tabs.count { it.isIncognito } }
    val visibleTabs = remember(tabs, isIncognitoTabSelected) {
        tabs.filter { it.isIncognito == isIncognitoTabSelected }
    }

    LaunchedEffect(lastClosedTab) {
        if (lastClosedTab != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Tab closed",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.ActionPerformed -> onUndoCloseTab()
                SnackbarResult.Dismissed -> onDismissUndo()
            }
        }
    }

    val containerBg = if (isIncognitoTabSelected) {
        Color(0xFF17181B)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = containerBg,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerBg,
                    titleContentColor = if (isIncognitoTabSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = if (isIncognitoTabSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = if (isIncognitoTabSelected) Color.White else MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Done"
                        )
                    }
                },
                title = {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = !isIncognitoTabSelected,
                            onClick = { isIncognitoTabSelected = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {}
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tab,
                                    contentDescription = "Normal tabs",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = normalCount.toString(),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        SegmentedButton(
                            selected = isIncognitoTabSelected,
                            onClick = { isIncognitoTabSelected = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {}
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VisibilityOff,
                                    contentDescription = "Incognito tabs",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = incognitoCount.toString(),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onNewTab(isIncognitoTabSelected) }) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = if (isIncognitoTabSelected) "New Incognito tab" else "New tab"
                        )
                    }
                    Box {
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isIncognitoTabSelected) "Close all Incognito tabs" else "Close all tabs"
                                    )
                                },
                                onClick = {
                                    overflowExpanded = false
                                    onCloseAllTabs(isIncognitoTabSelected)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (visibleTabs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = if (isIncognitoTabSelected) Icons.Rounded.VisibilityOff else Icons.Rounded.Tab,
                        contentDescription = null,
                        tint = if (isIncognitoTabSelected) Color(0xFF9AA0A6) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (isIncognitoTabSelected) "No Incognito tabs" else "No open tabs",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isIncognitoTabSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    FilledTonalButton(onClick = { onNewTab(isIncognitoTabSelected) }) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isIncognitoTabSelected) "New Incognito tab" else "New tab")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(visibleTabs, key = { it.id }) { tab ->
                    TabGridCard(
                        tab = tab,
                        isActive = tab.id == activeTabId,
                        thumbnail = thumbnails[tab.id],
                        onSelect = { onSelectTab(tab.id) },
                        onClose = { onCloseTab(tab.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabGridCard(
    tab: BrowserTab,
    isActive: Boolean,
    thumbnail: ImageBitmap?,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)
    val isIncognito = tab.isIncognito
    val headerBg = if (isIncognito) Color(0xFF2B2C30) else MaterialTheme.colorScheme.surfaceContainerHigh
    val bodyBg = if (isIncognito) Color(0xFF202124) else MaterialTheme.colorScheme.surfaceContainerLow
    val headerFg = if (isIncognito) Color(0xFFE8EAED) else MaterialTheme.colorScheme.onSurface
    val border = if (isActive) {
        BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, if (isIncognito) Color(0xFF3C4043) else MaterialTheme.colorScheme.outlineVariant)
    }

    val displayTitle = tab.title.ifBlank {
        if (tab.url == NEW_TAB_URL) "New Tab" else tab.url
    }

    Surface(
        shape = cardShape,
        color = bodyBg,
        border = border,
        tonalElevation = if (isActive) 4.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.76f)
            .clip(cardShape)
            .clickable(onClick = onSelect)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isIncognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Public,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary else headerFg.copy(alpha = 0.78f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = headerFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close tab",
                        tint = headerFg.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Thumbnail or clean NTP/blank preview
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bodyBg),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isIncognito) Color(0xFF303134) else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isIncognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Tab,
                                    contentDescription = null,
                                    tint = if (isIncognito) Color(0xFF9AA0A6) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = if (tab.url == NEW_TAB_URL) "New Tab" else displayTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isIncognito) Color(0xFF9AA0A6) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
