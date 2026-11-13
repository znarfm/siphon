package com.znarfm.siphon.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrowserMenuSheet(
    isMenuOpen: Boolean,
    onDismissRequest: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLoading: Boolean,
    isBookmarked: Boolean,
    adblockRuleCount: Int,
    tabBlockedCount: Int = 0,
    activeDownloadCount: Int,
    isWebDarkMode: Boolean,
    isDesktopMode: Boolean,
    isAdBlockEnabled: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    onOpenPageInfo: () -> Unit,
    onReloadOrStop: () -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenFindInPage: () -> Unit,
    onToggleWebDarkMode: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleAdBlock: () -> Unit,
    onOpenAdblockDialog: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (!isMenuOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            // Top Quick-Action Icon Row
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onDismissRequest()
                            onBack()
                        },
                        enabled = canGoBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }

                    IconButton(
                        onClick = {
                            onDismissRequest()
                            onForward()
                        },
                        enabled = canGoForward
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Forward"
                        )
                    }

                    IconButton(
                        onClick = {
                            onDismissRequest()
                            onToggleBookmark()
                        }
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Rounded.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            onDismissRequest()
                            onShare()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share"
                        )
                    }

                    IconButton(
                        onClick = {
                            onDismissRequest()
                            onOpenPageInfo()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Page Info"
                        )
                    }

                    IconButton(
                        onClick = {
                            onDismissRequest()
                            onReloadOrStop()
                        }
                    ) {
                        Icon(
                            imageVector = if (isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                            contentDescription = if (isLoading) "Stop" else "Reload"
                        )
                    }
                }
            }

            // Tab & Navigation Items
            BrowserMenuItemRow(
                icon = Icons.Rounded.Add,
                title = "New tab",
                onClick = {
                    onDismissRequest()
                    onNewTab()
                }
            )

            BrowserMenuItemRow(
                icon = Icons.Rounded.PrivacyTip,
                title = "New incognito tab",
                onClick = {
                    onDismissRequest()
                    onNewIncognitoTab()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            BrowserMenuItemRow(
                icon = Icons.Rounded.Star,
                title = "Bookmarks",
                onClick = {
                    onDismissRequest()
                    onOpenBookmarks()
                }
            )

            BrowserMenuItemRow(
                icon = Icons.Rounded.History,
                title = "History",
                onClick = {
                    onDismissRequest()
                    onOpenHistory()
                }
            )

            BrowserMenuItemRow(
                icon = Icons.Rounded.Download,
                title = "Downloads",
                onClick = {
                    onDismissRequest()
                    onOpenDownloads()
                },
                trailingContent = if (activeDownloadCount > 0) {
                    {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = activeDownloadCount.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else null
            )

            BrowserMenuItemRow(
                icon = Icons.Rounded.FindInPage,
                title = "Find in page",
                onClick = {
                    onDismissRequest()
                    onOpenFindInPage()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Toggles
            BrowserMenuItemRow(
                icon = Icons.Rounded.DarkMode,
                title = "Web Dark Mode",
                onClick = onToggleWebDarkMode,
                trailingContent = {
                    Switch(
                        checked = isWebDarkMode,
                        onCheckedChange = { onToggleWebDarkMode() }
                    )
                }
            )

            BrowserMenuItemRow(
                icon = Icons.Rounded.DesktopWindows,
                title = "Desktop site",
                onClick = onToggleDesktopMode,
                trailingContent = {
                    Switch(
                        checked = isDesktopMode,
                        onCheckedChange = { onToggleDesktopMode() }
                    )
                }
            )

            BrowserMenuItemRow(
                icon = Icons.Rounded.Shield,
                title = "Ad Blocker",
                onClick = onToggleAdBlock,
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (tabBlockedCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "$tabBlockedCount blocked",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        AssistChip(
                            onClick = {
                                onDismissRequest()
                                onOpenAdblockDialog()
                            },
                            label = {
                                Text(
                                    text = "$adblockRuleCount rules",
                                    fontSize = 11.sp
                                )
                            }
                        )
                        Switch(
                            checked = isAdBlockEnabled,
                            onCheckedChange = { onToggleAdBlock() }
                        )
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            BrowserMenuItemRow(
                icon = Icons.Rounded.Settings,
                title = "Settings",
                onClick = {
                    onDismissRequest()
                    onOpenSettings()
                }
            )
        }
    }
}

@Composable
private fun BrowserMenuItemRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailingContent()
        }
    }
}
