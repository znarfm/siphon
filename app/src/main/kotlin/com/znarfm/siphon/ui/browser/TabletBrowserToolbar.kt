package com.znarfm.siphon.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.znarfm.siphon.ui.common.IncognitoThemeOverlay
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.SiphonIcons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TabletBrowserToolbar(
    tabs: List<BrowserTab>,
    activeTabId: String,
    url: String,
    title: String,
    isIncognito: Boolean,
    isLoading: Boolean,
    pageProgress: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isBookmarked: Boolean,
    isAdBlockEnabled: Boolean,
    detectedMediaCount: Int,
    activeDownloadCount: Int,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onOpenOmniboxSearch: () -> Unit,
    onOpenSiteInfo: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenTabSwitcher: () -> Unit,
    onOpenSniffer: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    IncognitoThemeOverlay(isIncognito) {
        val primaryContentColor = MaterialTheme.colorScheme.onSurface
        val secondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        val isNewTab = url == NEW_TAB_URL || url.isBlank()
        val displayUrl = if (isNewTab) "Search or type URL" else url

        val siteSecurity = resolveSiteSecurityVisuals(
            url = url,
            isNewTab = isNewTab,
            isIncognito = isIncognito,
            isAdBlockEnabled = isAdBlockEnabled
        )

        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                // Row 1: Compact Horizontal Tab Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(tabs, key = { it.id }) { tab ->
                            val isActive = tab.id == activeTabId
                            val tabTitle = if (tab.url == NEW_TAB_URL || tab.title.isBlank()) {
                                "New Tab"
                            } else {
                                tab.title
                            }
                            val tabContainerColor = if (isActive) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                Color.Transparent
                            }
                            val tabContentColor = if (isActive) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                secondaryContentColor
                            }

                            Surface(
                                onClick = { onSelectTab(tab.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = tabContainerColor,
                                modifier = Modifier
                                    .widthIn(min = 120.dp, max = 210.dp)
                                    .height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(start = 10.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (tab.isIncognito) SiphonIcons.PrivacyTip else SiphonIcons.Public,
                                        contentDescription = null,
                                        tint = tabContentColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = tabTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = tabContentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onCloseTab(tab.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = SiphonIcons.Close,
                                            contentDescription = "Close tab",
                                            tint = tabContentColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onNewTab,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = SiphonIcons.Add,
                            contentDescription = "New tab",
                            tint = primaryContentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Row 2: Top Omnibox & Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onGoBack,
                        enabled = canGoBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = SiphonIcons.ArrowBack,
                            contentDescription = "Back",
                            tint = if (canGoBack) primaryContentColor else secondaryContentColor.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onGoForward,
                        enabled = canGoForward,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = SiphonIcons.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (canGoForward) primaryContentColor else secondaryContentColor.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = if (isLoading) onStop else onReload,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isLoading) SiphonIcons.Close else SiphonIcons.Refresh,
                            contentDescription = if (isLoading) "Stop loading" else "Reload page",
                            tint = primaryContentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Capsule URL Pill
                    Surface(
                        shape = ExpressiveShapeTokens.PillShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
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
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) SiphonIcons.Star else SiphonIcons.Outlined.StarBorder,
                                    contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark page",
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else secondaryContentColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    // Sniffer Quick Button with detected media badge
                    OmniboxSnifferButton(
                        detectedMediaCount = detectedMediaCount,
                        onClick = onOpenSniffer
                    )

                    // Downloads Quick Button with active badge
                    OmniboxDownloadsButton(
                        activeDownloadCount = activeDownloadCount,
                        onClick = onOpenDownloads
                    )

                    // Tab Counter [N] Button
                    OmniboxTabCounterButton(
                        tabCount = tabs.size,
                        onClick = onOpenTabSwitcher
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

                AnimatedVisibility(visible = pageProgress in 1..99) {
                    LinearWavyProgressIndicator(
                        progress = { pageProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
