package com.znarfm.siphon.ui.browser

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.znarfm.siphon.core.adblock.AdblockConfig
import com.znarfm.siphon.core.adblock.FastHostFilter
import com.znarfm.siphon.ui.theme.AppColorSeed
import com.znarfm.siphon.ui.theme.AppThemeMode

@Composable
fun BrowserMenuSheet(
    isMenuOpen: Boolean,
    onDismissRequest: () -> Unit,
    adblockRuleCount: Int,
    isWebDarkMode: Boolean,
    isDesktopMode: Boolean,
    isAdBlockEnabled: Boolean,
    themeMode: AppThemeMode,
    colorSeed: AppColorSeed,
    currentUrl: String,
    clipboardManager: ClipboardManager,
    onNewIncognitoTab: () -> Unit,
    onOpenBookmarksAndHistory: () -> Unit,
    onOpenAdblockDialog: () -> Unit,
    onToggleWebDarkMode: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleAdBlock: () -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetColorSeed: (AppColorSeed) -> Unit
) {
    DropdownMenu(
        expanded = isMenuOpen,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text("🕶 New Incognito Tab", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
            onClick = {
                onDismissRequest()
                onNewIncognitoTab()
            }
        )

        DropdownMenuItem(
            text = { Text("⭐ Bookmarks & History", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
            onClick = {
                onDismissRequest()
                onOpenBookmarksAndHistory()
            }
        )

        DropdownMenuItem(
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🛡 Adblock Rules", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("$adblockRuleCount", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            onClick = {
                onDismissRequest()
                onOpenAdblockDialog()
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        DropdownMenuItem(
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌙 Web Dark Mode", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isWebDarkMode,
                        onCheckedChange = { onToggleWebDarkMode() },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            },
            onClick = onToggleWebDarkMode
        )

        DropdownMenuItem(
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🖥 Desktop Site", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isDesktopMode,
                        onCheckedChange = { onToggleDesktopMode() },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            },
            onClick = onToggleDesktopMode
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "🎨 App Theme",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            val themeModes = AppThemeMode.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeModes.forEachIndexed { index, mode ->
                    val isSelected = mode == themeMode
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onSetThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, themeModes.size),
                        label = { Text(mode.title, fontSize = 10.sp) }
                    )
                }
            }
        }

        val availableSeeds = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AppColorSeed.entries
            } else {
                AppColorSeed.entries.filter { it != AppColorSeed.DYNAMIC }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            availableSeeds.forEach { seed ->
                val effectiveSelected = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && colorSeed == AppColorSeed.DYNAMIC) {
                    seed == AppColorSeed.SAPPHIRE
                } else {
                    seed == colorSeed
                }
                if (seed == AppColorSeed.DYNAMIC) {
                    FilterChip(
                        selected = effectiveSelected,
                        onClick = { onSetColorSeed(seed) },
                        label = { Text("Monet", fontSize = 10.sp) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(seed.swatch)
                            .border(
                                width = if (effectiveSelected) 2.5.dp else 1.dp,
                                color = if (effectiveSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onSetColorSeed(seed) }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        DropdownMenuItem(
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🛡 Ad Blocker", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isAdBlockEnabled,
                        onCheckedChange = { onToggleAdBlock() },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            },
            onClick = onToggleAdBlock
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        DropdownMenuItem(
            text = { Text("📋 Copy Link", fontSize = 13.sp) },
            onClick = {
                onDismissRequest()
                clipboardManager.setText(AnnotatedString(currentUrl))
            }
        )
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
