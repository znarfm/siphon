package com.znarfm.siphon.ui.browser

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val dockContainerColor = if (isIncognito) {
        Color(0xFF1C1C1E)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val pillContainerColor = if (isIncognito) {
        Color(0xFF2B2B2E)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val primaryContentColor = if (isIncognito) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryContentColor = if (isIncognito) {
        Color.White.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

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
        isIncognito -> Color.White.copy(alpha = 0.85f)
        isAdBlockEnabled -> MaterialTheme.colorScheme.primary
        else -> secondaryContentColor
    }

    Surface(
        tonalElevation = 3.dp,
        color = dockContainerColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            if (pageProgress in 1..99) {
                LinearWavyProgressIndicator(
                    progress = { pageProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Capsule URL Pill
                Surface(
                    shape = CircleShape,
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

                // Tab Counter [N] Button
                IconButton(
                    onClick = onOpenTabSwitcher,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .border(
                                width = 1.8.dp,
                                color = primaryContentColor,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tabCount > 99) "99+" else tabCount.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            color = primaryContentColor
                        )
                    }
                }

                // Downloads Quick Button with active badge
                IconButton(
                    onClick = onOpenDownloads,
                    modifier = Modifier.size(38.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (activeDownloadCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text(
                                        text = activeDownloadCount.toString(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Downloads",
                            tint = primaryContentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Attached M3 Expressive Sniffer FAB
                BadgedBox(
                    badge = {
                        if (detectedMediaCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    text = detectedMediaCount.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    SmallFloatingActionButton(
                        onClick = onOpenSniffer,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
