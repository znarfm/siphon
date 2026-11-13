package com.znarfm.siphon.ui.browser

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.TabularNumberStyle
import kotlinx.coroutines.launch
import kotlin.math.abs

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

    val activeColorScheme = if (isIncognitoTabSelected) {
        darkColorScheme()
    } else {
        MaterialTheme.colorScheme
    }

    MaterialTheme(
        colorScheme = activeColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
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
                            onClick = onDismiss,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Done"
                            )
                        }

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.weight(1f)
                        ) {
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
                                        style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                                        fontWeight = if (!isIncognitoTabSelected) FontWeight.Bold else FontWeight.Medium
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
                                        style = MaterialTheme.typography.labelLarge.merge(TabularNumberStyle),
                                        fontWeight = if (isIncognitoTabSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        FilledTonalIconButton(
                            onClick = { onNewTab(isIncognitoTabSelected) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = if (isIncognitoTabSelected) "New Incognito tab" else "New tab"
                            )
                        }

                        Box {
                            FilledTonalIconButton(
                                onClick = { overflowExpanded = true },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
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
                }
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
                        Surface(
                            shape = ExpressiveShapeTokens.ActiveCardShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isIncognitoTabSelected) Icons.Rounded.VisibilityOff else Icons.Rounded.Tab,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isIncognitoTabSelected) "No Incognito tabs" else "No open tabs",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FilledTonalButton(
                            onClick = { onNewTab(isIncognitoTabSelected) },
                            shape = ExpressiveShapeTokens.PillShape
                        ) {
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
}

@Composable
private fun TabGridCard(
    tab: BrowserTab,
    isActive: Boolean,
    thumbnail: ImageBitmap?,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val cardShape = if (isActive) {
        ExpressiveShapeTokens.SelectedCardShape
    } else {
        ExpressiveShapeTokens.RestingCardShape
    }
    val isIncognito = tab.isIncognito
    val headerBg = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val bodyBg = MaterialTheme.colorScheme.surfaceContainerLow
    val headerFg = MaterialTheme.colorScheme.onSurface
    val border = if (isActive) {
        BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    val displayTitle = tab.title.ifBlank {
        if (tab.url == NEW_TAB_URL) "New Tab" else tab.url
    }

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val fallbackWidthPx = with(density) { 180.dp.toPx() }
    var cardWidthPx by remember(tab.id) { mutableFloatStateOf(0f) }
    val offsetX = remember(tab.id) { Animatable(0f) }
    val currentOnClose by rememberUpdatedState(onClose)

    Surface(
        shape = cardShape,
        color = bodyBg,
        border = border,
        tonalElevation = if (isActive) 4.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.76f)
            .onSizeChanged { size ->
                cardWidthPx = size.width.toFloat()
            }
            .graphicsLayer {
                val tx = offsetX.value
                val dismissDistancePx = if (cardWidthPx > 0f) cardWidthPx else fallbackWidthPx
                translationX = tx
                alpha = (1f - (abs(tx) / dismissDistancePx) * 0.6f).coerceIn(0.25f, 1f)
            }
            .pointerInput(tab.id) {
                val flingVelocityPxPerSec = with(density) { 500.dp.toPx() }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                    val touchSlop = viewConfiguration.touchSlop
                    var totalDx = 0f
                    var totalDy = 0f
                    var isHorizontalDrag = false
                    var hapticFired = false
                    var wasCancelled = false

                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Cancel) {
                            wasCancelled = true
                            break
                        }
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            break
                        }
                        if (change.isConsumed && !isHorizontalDrag) {
                            wasCancelled = true
                            break
                        }
                        val delta = change.positionChange()
                        totalDx += delta.x
                        totalDy += delta.y

                        if (!isHorizontalDrag) {
                            val absDx = abs(totalDx)
                            val absDy = abs(totalDy)
                            if (absDy > touchSlop && absDy > absDx) {
                                wasCancelled = true
                                break
                            }
                            if (absDx > touchSlop && absDx > absDy) {
                                isHorizontalDrag = true
                                velocityTracker.resetTracking()
                            }
                        }

                        if (isHorizontalDrag) {
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val newOffset = totalDx
                            scope.launch { offsetX.snapTo(newOffset) }
                            val effectiveWidth = if (cardWidthPx > 0f) cardWidthPx else fallbackWidthPx
                            val crossed = abs(newOffset) >= effectiveWidth * 0.35f
                            if (crossed && !hapticFired) {
                                hapticFired = true
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (!crossed && hapticFired) {
                                hapticFired = false
                            }
                        }
                    }

                    if (isHorizontalDrag) {
                        val effectiveWidth = if (cardWidthPx > 0f) cardWidthPx else fallbackWidthPx
                        val dismissThreshold = effectiveWidth * 0.35f
                        val vx = velocityTracker.calculateVelocity().x
                        val currentTx = offsetX.value
                        val shouldDismiss = !wasCancelled && (
                            abs(currentTx) >= dismissThreshold ||
                                (abs(vx) >= flingVelocityPxPerSec && abs(currentTx) > touchSlop)
                            )
                        if (shouldDismiss) {
                            val direction = when {
                                abs(vx) >= flingVelocityPxPerSec && vx != 0f -> if (vx > 0f) 1f else -1f
                                currentTx >= 0f -> 1f
                                else -> -1f
                            }
                            val targetX = direction * (effectiveWidth * 1.25f)
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = targetX,
                                    animationSpec = tween(durationMillis = 180)
                                )
                                currentOnClose()
                            }
                        } else {
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        }
                    }
                }
            }
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = headerFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = ExpressiveShapeTokens.PillShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(ExpressiveShapeTokens.PillShape)
                        .clickable(onClick = onClose)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close tab",
                            tint = headerFg.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
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
                            shape = ExpressiveShapeTokens.PillShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isIncognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Tab,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = if (tab.url == NEW_TAB_URL) "New Tab" else displayTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
