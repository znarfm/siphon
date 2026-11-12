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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.znarfm.siphon.core.adblock.AdblockConfig
import com.znarfm.siphon.core.adblock.FastHostFilter

@Composable
fun BrowserMenuSheet(
    isMenuOpen: Boolean,
    onDismissRequest: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLoading: Boolean,
    isBookmarked: Boolean,
    adblockRuleCount: Int,
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

@Composable
fun AdblockSubscriptionDialog(
    currentRuleCount: Int,
    initialConfig: AdblockConfig = AdblockConfig(),
    onDismiss: () -> Unit,
    onApply: (AdblockConfig) -> Unit
) {
    val basePresets = listOf(
        "HaGeZi Multi Normal (Recommended)" to FastHostFilter.HAGEZI_NORMAL_URL,
        "HaGeZi Multi Light (Low RAM)" to FastHostFilter.HAGEZI_LIGHT_URL,
        "HaGeZi Multi Pro (Aggressive)" to FastHostFilter.HAGEZI_PRO_URL
    )

    var selectedBaseUrl by remember { mutableStateOf(initialConfig.baseUrl) }
    var allowShorteners by remember { mutableStateOf(initialConfig.allowShorteners) }
    var enableThreatIntel by remember { mutableStateOf(initialConfig.enableThreatIntel) }
    var customUrl by remember { mutableStateOf(initialConfig.customUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Adblock Subscriptions", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Active Rules: $currentRuleCount hosts blocked",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("Base Blocklist:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                basePresets.forEach { (name, url) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBaseUrl = url }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedBaseUrl == url,
                            onClick = { selectedBaseUrl = url }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(name, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Redirect & CDN Allowlist:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { allowShorteners = !allowShorteners }
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allowShorteners,
                        onCheckedChange = { allowShorteners = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("DandelionSprout Shorteners", fontSize = 12.sp)
                        Text(
                            "Unbreaks bit.ly, tinyurl, ad gates, and download resolvers",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Threat Intel:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { enableThreatIntel = !enableThreatIntel }
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enableThreatIntel,
                        onCheckedChange = { enableThreatIntel = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("HaGeZi Fake / Scam / Malicious", fontSize = 12.sp)
                        Text(
                            "Blocks scam, phishing, and malware domains",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    label = { Text("Custom Blocklist URL (Optional)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        AdblockConfig(
                            baseUrl = selectedBaseUrl,
                            allowShorteners = allowShorteners,
                            enableThreatIntel = enableThreatIntel,
                            customUrl = customUrl
                        )
                    )
                }
            ) {
                Text("Refresh & Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
