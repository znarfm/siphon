package com.znarfm.siphon.ui.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.znarfm.siphon.ui.theme.SiphonIcons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SiteInfoSheet(
    url: String,
    title: String,
    isIncognito: Boolean,
    isAdBlockEnabled: Boolean,
    adblockRuleCount: Int,
    isSiteAllowlisted: Boolean = false,
    tabBlockedCount: Int = 0,
    desktopMode: Boolean,
    webDarkMode: Boolean,
    onToggleAdBlock: () -> Unit,
    onToggleSiteAllowlist: () -> Unit = {},
    onToggleDesktopMode: () -> Unit,
    onToggleWebDarkMode: () -> Unit,
    onManageAdblock: () -> Unit,
    onCopyUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isHttps = url.startsWith("https://", ignoreCase = true)
    val isHttp = url.startsWith("http://", ignoreCase = true)

    val securityIcon = when {
        isHttps -> SiphonIcons.Lock
        isHttp -> SiphonIcons.Warning
        else -> SiphonIcons.Info
    }
    val securityLabel = when {
        isHttps -> "Connection is secure"
        isHttp -> "Connection is not secure"
        else -> "Internal page"
    }
    val securityTint = when {
        isHttps -> MaterialTheme.colorScheme.primary
        isHttp -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val securityContainer = when {
        isHttps -> MaterialTheme.colorScheme.primaryContainer
        isHttp -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val securityOnContainer = when {
        isHttps -> MaterialTheme.colorScheme.onPrimaryContainer
        isHttp -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Security badge + Incognito pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = securityContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = securityIcon,
                            contentDescription = null,
                            tint = securityTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = securityLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = securityOnContainer
                        )
                    }
                }

                if (isIncognito) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = SiphonIcons.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Incognito",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Title & Full URL + Copy button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (title.isNotBlank()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onCopyUrl(url) }) {
                        Icon(
                            imageVector = SiphonIcons.ContentCopy,
                            contentDescription = "Copy URL",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Ad Blocker Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAdBlockEnabled && !isSiteAllowlisted) SiphonIcons.Shield else SiphonIcons.Outlined.Shield,
                            contentDescription = null,
                            tint = if (isAdBlockEnabled && !isSiteAllowlisted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ad Blocker",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = when {
                                    !isAdBlockEnabled -> "Paused globally • $adblockRuleCount active rules"
                                    isSiteAllowlisted -> "Paused on this site • $adblockRuleCount active rules"
                                    else -> "$tabBlockedCount ads blocked on this page • $adblockRuleCount active rules"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAdBlockEnabled,
                            onCheckedChange = { onToggleAdBlock() }
                        )
                    }

                    if (isHttp || isHttps) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pause adblock on this site",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isSiteAllowlisted) "Site is allowlisted" else "Allow ads on this domain",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isSiteAllowlisted,
                                onCheckedChange = { onToggleSiteAllowlist() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FilledTonalButton(
                        onClick = onManageAdblock,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Manage Lists")
                    }
                }
            }

            HorizontalDivider()

            // Quick Toggles: Desktop Site & Web Dark Mode
            SiteToggleRow(
                icon = SiphonIcons.DesktopWindows,
                title = "Desktop site",
                subtitle = "Request desktop version of this site",
                checked = desktopMode,
                onCheckedChange = { onToggleDesktopMode() }
            )

            SiteToggleRow(
                icon = SiphonIcons.DarkMode,
                title = "Web dark mode",
                subtitle = "Force dark theme on web pages",
                checked = webDarkMode,
                onCheckedChange = { onToggleWebDarkMode() }
            )
        }
    }
}

@Composable
private fun SiteToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() }
        )
    }
}
