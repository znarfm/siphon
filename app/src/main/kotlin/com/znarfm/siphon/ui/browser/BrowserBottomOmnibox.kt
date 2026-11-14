package com.znarfm.siphon.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.znarfm.siphon.core.common.FormatUtils
import com.znarfm.siphon.ui.common.IncognitoThemeOverlay
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.SiphonIcons
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BrowserBottomOmnibox(
    url: String,
    title: String,
    isIncognito: Boolean,
    isLoading: Boolean,
    pageProgress: Int,
    isBookmarked: Boolean,
    isAdBlockEnabled: Boolean,
    tabCount: Int,
    detectedMediaCount: Int,
    activeDownloadCount: Int,
    onOpenOmniboxSearch: () -> Unit,
    onOpenSiteInfo: () -> Unit,
    onToggleBookmark: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onOpenTabSwitcher: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSniffer: () -> Unit,
    onOpenDownloads: () -> Unit,
    onSwitchPreviousTab: () -> Unit = {},
    onSwitchNextTab: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val dockOffsetX = remember { Animatable(0f) }
    val dockOffsetY = remember { Animatable(0f) }
    val currentOnSwitchPreviousTab by rememberUpdatedState(onSwitchPreviousTab)
    val currentOnSwitchNextTab by rememberUpdatedState(onSwitchNextTab)
    val currentOnOpenTabSwitcher by rememberUpdatedState(onOpenTabSwitcher)

    IncognitoThemeOverlay(isIncognito) {
        val dockContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        val pillContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        val primaryContentColor = MaterialTheme.colorScheme.onSurface
        val secondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant

        val isNewTab = url == NEW_TAB_URL || url.isBlank()
        val displayUrl = if (isNewTab) {
            "Search or type URL"
        } else {
            FormatUtils.extractHostDisplay(url).ifBlank { url }
        }

        val siteSecurity = resolveSiteSecurityVisuals(
            url = url,
            isNewTab = isNewTab,
            isIncognito = isIncognito,
            isAdBlockEnabled = isAdBlockEnabled
        )

        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            AnimatedVisibility(visible = pageProgress in 1..99) {
                Column {
                    LinearWavyProgressIndicator(
                        progress = { pageProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(4.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Surface(
                shape = ExpressiveShapeTokens.PillShape,
                color = dockContainerColor,
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = dockOffsetX.value
                        translationY = dockOffsetY.value
                    }
                    .pointerInput(Unit) {
                        val horizontalThresholdPx = with(density) { 56.dp.toPx() }
                        val verticalThresholdPx = with(density) { 48.dp.toPx() }
                        val maxTranslationXPx = with(density) { 48.dp.toPx() }
                        val maxTranslationYPx = with(density) { 36.dp.toPx() }
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val touchSlop = viewConfiguration.touchSlop
                            var totalDx = 0f
                            var totalDy = 0f
                            var pastTouchSlop = false
                            var lockedAxis = 0 // 0 = none, 1 = horizontal, 2 = vertical upward
                            var hapticFired = false
                            var wasCancelled = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null) {
                                    wasCancelled = true
                                    break
                                }
                                if (!change.pressed) {
                                    break
                                }
                                val delta = change.positionChange()
                                totalDx += delta.x
                                totalDy += delta.y

                                if (!pastTouchSlop) {
                                    val absDx = abs(totalDx)
                                    val absDy = abs(totalDy)
                                    if (absDx > touchSlop || absDy > touchSlop) {
                                        if (absDx >= absDy) {
                                            pastTouchSlop = true
                                            lockedAxis = 1
                                        } else if (totalDy < 0f) {
                                            pastTouchSlop = true
                                            lockedAxis = 2
                                        } else {
                                            break
                                        }
                                    }
                                }

                                if (pastTouchSlop) {
                                    change.consume()
                                    if (lockedAxis == 1) {
                                        val clampedX = (totalDx * 0.45f)
                                            .coerceIn(-maxTranslationXPx, maxTranslationXPx)
                                        scope.launch { dockOffsetX.snapTo(clampedX) }
                                        val crossed = abs(totalDx) >= horizontalThresholdPx
                                        if (crossed && !hapticFired) {
                                            hapticFired = true
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else if (!crossed && hapticFired) {
                                            hapticFired = false
                                        }
                                    } else if (lockedAxis == 2) {
                                        val clampedY = (totalDy * 0.45f)
                                            .coerceIn(-maxTranslationYPx, 0f)
                                        scope.launch { dockOffsetY.snapTo(clampedY) }
                                        val crossed = totalDy <= -verticalThresholdPx
                                        if (crossed && !hapticFired) {
                                            hapticFired = true
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else if (!crossed && hapticFired) {
                                            hapticFired = false
                                        }
                                    }
                                }
                            }

                            if (pastTouchSlop) {
                                if (!wasCancelled) {
                                    if (lockedAxis == 1) {
                                        if (totalDx < -horizontalThresholdPx) {
                                            currentOnSwitchNextTab()
                                        } else if (totalDx > horizontalThresholdPx) {
                                            currentOnSwitchPreviousTab()
                                        }
                                    } else if (lockedAxis == 2 && totalDy < -verticalThresholdPx) {
                                        currentOnOpenTabSwitcher()
                                    }
                                }
                                scope.launch {
                                    dockOffsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                                scope.launch {
                                    dockOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Capsule URL Pill
                    Surface(
                        shape = ExpressiveShapeTokens.PillShape,
                        color = pillContainerColor,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onOpenSiteInfo,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = siteSecurity.icon,
                                    contentDescription = "Site info",
                                    tint = siteSecurity.tint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(ExpressiveShapeTokens.PillShape)
                                    .clickable(onClick = onOpenOmniboxSearch)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = displayUrl,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isNewTab) secondaryContentColor else primaryContentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = onToggleBookmark,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) SiphonIcons.Star else SiphonIcons.Outlined.StarBorder,
                                    contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark page",
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else secondaryContentColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = if (isLoading) onStop else onReload,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLoading) SiphonIcons.Close else SiphonIcons.Refresh,
                                    contentDescription = if (isLoading) "Stop loading" else "Reload page",
                                    tint = secondaryContentColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    // Expressive Tab Counter [N] Button
                    OmniboxTabCounterButton(
                        tabCount = tabCount,
                        onClick = onOpenTabSwitcher
                    )

                    // Downloads Quick Button with active badge
                    OmniboxDownloadsButton(
                        activeDownloadCount = activeDownloadCount,
                        onClick = onOpenDownloads
                    )

                    // Expressive Sniffer Button
                    OmniboxSnifferButton(
                        detectedMediaCount = detectedMediaCount,
                        onClick = onOpenSniffer
                    )

                    // Overflow Menu Button
                    IconButton(
                        onClick = onOpenMenu,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = SiphonIcons.MoreVert,
                            contentDescription = "More options",
                            tint = primaryContentColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
    }
}
