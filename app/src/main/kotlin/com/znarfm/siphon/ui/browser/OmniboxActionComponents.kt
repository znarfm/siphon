package com.znarfm.siphon.ui.browser

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.SiphonIcons
import com.znarfm.siphon.ui.theme.TabularNumberStyle

data class SiteSecurityVisuals(
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun resolveSiteSecurityVisuals(
    url: String,
    isNewTab: Boolean,
    isIncognito: Boolean,
    isAdBlockEnabled: Boolean
): SiteSecurityVisuals {
    val icon = when {
        isIncognito && isNewTab -> SiphonIcons.PrivacyTip
        isNewTab && isAdBlockEnabled -> SiphonIcons.Shield
        isNewTab -> SiphonIcons.Public
        url.startsWith("https://", ignoreCase = true) -> SiphonIcons.Lock
        isAdBlockEnabled -> SiphonIcons.Shield
        else -> SiphonIcons.Public
    }
    val tint = when {
        isAdBlockEnabled && !isIncognito -> MaterialTheme.colorScheme.primary
        isIncognito -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    return SiteSecurityVisuals(icon = icon, tint = tint)
}

@Composable
fun OmniboxTabCounterButton(
    tabCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    FilledTonalIconButton(
        onClick = onClick,
        shape = ExpressiveShapeTokens.ActiveCardShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = contentColor
        ),
        modifier = modifier.size(38.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(
                    width = 1.6.dp,
                    color = contentColor,
                    shape = RoundedCornerShape(7.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (tabCount > 99) "99+" else tabCount.toString(),
                style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun OmniboxDownloadsButton(
    activeDownloadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BadgedBox(
        modifier = modifier,
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
            onClick = onClick,
            shape = ExpressiveShapeTokens.PillShape,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = SiphonIcons.Download,
                contentDescription = "Downloads",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun OmniboxSnifferButton(
    detectedMediaCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BadgedBox(
        modifier = modifier,
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
            onClick = onClick,
            shape = ExpressiveShapeTokens.ActiveCardShape,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = SiphonIcons.Bolt,
                contentDescription = "Media Sniffer",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
