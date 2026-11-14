package com.znarfm.siphon.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.TabularNumberStyle
import com.znarfm.siphon.ui.theme.SiphonIcons

@OptIn(ExperimentalMaterial3Api::class)
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
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Connected Expressive Quick-Action Button Group
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = ExpressiveShapeTokens.RestingCardShape,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val actionColors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )

                    FilledTonalIconButton(
                        onClick = {
                            onDismissRequest()
                            onBack()
                        },
                        enabled = canGoBack,
                        shape = ExpressiveShapeTokens.PillShape,
                        colors = actionColors,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = SiphonIcons.ArrowBack,
                            contentDescription = "Back"
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            onDismissRequest()
                            onForward()
                        },
                        enabled = canGoForward,
                        shape = ExpressiveShapeTokens.PillShape,
                        colors = actionColors,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = SiphonIcons.ArrowForward,
                            contentDescription = "Forward"
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            onDismissRequest()
                            onToggleBookmark()
                        },
                        shape = ExpressiveShapeTokens.PillShape,
                        colors = if (isBookmarked) {
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else actionColors,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) SiphonIcons.Star else SiphonIcons.Outlined.StarBorder,
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark"
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            onDismissRequest()
                            onShare()
                        },
                        shape = ExpressiveShapeTokens.PillShape,
                        colors = actionColors,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = SiphonIcons.Share,
                            contentDescription = "Share"
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            onDismissRequest()
                            onOpenPageInfo()
                        },
                        shape = ExpressiveShapeTokens.PillShape,
                        colors = actionColors,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = SiphonIcons.Info,
                            contentDescription = "Page Info"
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            onDismissRequest()
                            onReloadOrStop()
                        },
                        shape = ExpressiveShapeTokens.PillShape,
                        colors = actionColors,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = if (isLoading) SiphonIcons.Close else SiphonIcons.Refresh,
                            contentDescription = if (isLoading) "Stop" else "Reload"
                        )
                    }
                }
            }

            // Tabs Group Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = ExpressiveShapeTokens.RestingCardShape,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ExpressiveShapeTokens.RestingCardShape)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BrowserMenuItemRow(
                        icon = SiphonIcons.Add,
                        title = "New tab",
                        onClick = {
                            onDismissRequest()
                            onNewTab()
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    BrowserMenuItemRow(
                        icon = SiphonIcons.PrivacyTip,
                        title = "New incognito tab",
                        onClick = {
                            onDismissRequest()
                            onNewIncognitoTab()
                        }
                    )
                }
            }

            // Library & Tools Group Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = ExpressiveShapeTokens.RestingCardShape,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ExpressiveShapeTokens.RestingCardShape)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BrowserMenuItemRow(
                        icon = SiphonIcons.Star,
                        title = "Bookmarks",
                        onClick = {
                            onDismissRequest()
                            onOpenBookmarks()
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    BrowserMenuItemRow(
                        icon = SiphonIcons.History,
                        title = "History",
                        onClick = {
                            onDismissRequest()
                            onOpenHistory()
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    BrowserMenuItemRow(
                        icon = SiphonIcons.Download,
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
                                        style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else null
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    BrowserMenuItemRow(
                        icon = SiphonIcons.FindInPage,
                        title = "Find in page",
                        onClick = {
                            onDismissRequest()
                            onOpenFindInPage()
                        }
                    )
                }
            }

            // Page & Shield Toggles Group Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = ExpressiveShapeTokens.RestingCardShape,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ExpressiveShapeTokens.RestingCardShape)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BrowserMenuItemRow(
                        icon = SiphonIcons.DarkMode,
                        title = "Web Dark Mode",
                        onClick = onToggleWebDarkMode,
                        trailingContent = {
                            Switch(
                                checked = isWebDarkMode,
                                onCheckedChange = { onToggleWebDarkMode() }
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    BrowserMenuItemRow(
                        icon = SiphonIcons.DesktopWindows,
                        title = "Desktop site",
                        onClick = onToggleDesktopMode,
                        trailingContent = {
                            Switch(
                                checked = isDesktopMode,
                                onCheckedChange = { onToggleDesktopMode() }
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    BrowserMenuItemRow(
                        icon = SiphonIcons.Shield,
                        title = "Ad Blocker",
                        onClick = onToggleAdBlock,
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (tabBlockedCount > 0) {
                                    Surface(
                                        shape = ExpressiveShapeTokens.PillShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "$tabBlockedCount blocked",
                                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
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
                                    shape = ExpressiveShapeTokens.PillShape,
                                    label = {
                                        Text(
                                            text = "$adblockRuleCount rules",
                                            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle)
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
                }
            }

            // Settings Pill Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = ExpressiveShapeTokens.PillShape,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ExpressiveShapeTokens.PillShape)
            ) {
                BrowserMenuItemRow(
                    icon = SiphonIcons.Settings,
                    title = "Settings",
                    onClick = {
                        onDismissRequest()
                        onOpenSettings()
                    }
                )
            }
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
            .padding(horizontal = 18.dp, vertical = 13.dp),
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
