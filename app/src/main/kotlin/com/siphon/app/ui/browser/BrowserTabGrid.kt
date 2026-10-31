package com.siphon.app.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrowserTabGrid(
    tabs: List<BrowserTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: (url: String, isIncognito: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tabs, key = { it.id }) { tab ->
            val isSelected = tab.id == activeTabId
            val tabBg = when {
                tab.isIncognito && isSelected -> Color(0xFF333333)
                tab.isIncognito -> Color(0xFF222222)
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
            }
            val tabFg = when {
                tab.isIncognito -> Color.White
                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Surface(
                shape = CircleShape,
                color = tabBg,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSelectTab(tab.id) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = (if (tab.isIncognito) "🕶 " else "") + tab.title.ifBlank { if (tab.isIncognito) "Private" else "New Tab" },
                        maxLines = 1,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = tabFg,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 110.dp)
                    )
                    if (tabs.size > 1) {
                        Text(
                            text = "✕",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = tabFg,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onCloseTab(tab.id) }
                                .padding(2.dp)
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable { onNewTab("https://google.com", false) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable { onNewTab("https://google.com", true) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🕶", fontSize = 11.sp)
                }
            }
        }
    }
}
