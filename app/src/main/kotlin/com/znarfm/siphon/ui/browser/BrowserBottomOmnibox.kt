package com.znarfm.siphon.ui.browser

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.TabularNumberStyle

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
    modifier: Modifier = Modifier
) {
    val incognitoScheme = remember { darkColorScheme() }
    val activeScheme = if (isIncognito) incognitoScheme else MaterialTheme.colorScheme

    MaterialTheme(colorScheme = activeScheme) {
        val dockContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        val pillContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        val primaryContentColor = MaterialTheme.colorScheme.onSurface
        val secondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant

        val isNewTab = url == NEW_TAB_URL || url.isBlank()
        val displayUrl = if (isNewTab) {
            "Search or type URL"
        } else {
            runCatching { Uri.parse(url).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: url
        }

        val siteIcon = when {
            isNewTab && isAdBlockEnabled -> Icons.Rounded.Shield
            isNewTab -> Icons.Rounded.Public
            url.startsWith("https://", ignoreCase = true) -> Icons.Rounded.Lock
            isAdBlockEnabled -> Icons.Rounded.Shield
            else -> Icons.Rounded.Public
        }
        val siteIconTint = when {
            isAdBlockEnabled && !isIncognito -> MaterialTheme.colorScheme.primary
            isIncognito -> MaterialTheme.colorScheme.onSurface
            else -> secondaryContentColor
        }

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
                modifier = Modifier.fillMaxWidth()
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
                                    imageVector = siteIcon,
                                    contentDescription = "Site info",
                                    tint = siteIconTint,
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
                                    imageVector = if (isBookmarked) Icons.Rounded.Star else Icons.Outlined.StarBorder,
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
                                    imageVector = if (isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                                    contentDescription = if (isLoading) "Stop loading" else "Reload page",
                                    tint = secondaryContentColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    // Expressive Tab Counter [N] Button
                    FilledTonalIconButton(
                        onClick = onOpenTabSwitcher,
                        shape = ExpressiveShapeTokens.ActiveCardShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = primaryContentColor
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .border(
                                    width = 1.6.dp,
                                    color = primaryContentColor,
                                    shape = RoundedCornerShape(7.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (tabCount > 99) "99+" else tabCount.toString(),
                                style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                                fontWeight = FontWeight.Bold,
                                color = primaryContentColor
                            )
                        }
                    }

                    // Downloads Quick Button with active badge
                    BadgedBox(
                        badge = {
                            if (activeDownloadCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text(
                                        text = activeDownloadCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    ) {
                        FilledTonalIconButton(
                            onClick = onOpenDownloads,
                            shape = ExpressiveShapeTokens.PillShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = primaryContentColor
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Downloads",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Expressive Sniffer Button
                    BadgedBox(
                        badge = {
                            if (detectedMediaCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text(
                                        text = detectedMediaCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    ) {
                        FilledTonalIconButton(
                            onClick = onOpenSniffer,
                            shape = ExpressiveShapeTokens.ActiveCardShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = "Media Sniffer",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Overflow Menu Button
                    IconButton(
                        onClick = onOpenMenu,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
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
