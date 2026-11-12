package com.znarfm.siphon.ui.browser

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.znarfm.siphon.data.db.BookmarkEntity

private data class QuickShortcut(
    val title: String,
    val url: String
)

private val DefaultShortcuts = listOf(
    QuickShortcut("Google", "https://www.google.com"),
    QuickShortcut("Wikipedia", "https://en.wikipedia.org"),
    QuickShortcut("YouTube", "https://www.youtube.com"),
    QuickShortcut("GitHub", "https://github.com")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewTabPage(
    isIncognito: Boolean,
    bookmarks: List<BookmarkEntity>,
    activeDownloadCount: Int,
    onSearchClick: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenIncognito: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isIncognito) {
        IncognitoNewTabPage(
            onSearchClick = onSearchClick,
            modifier = modifier
        )
    } else {
        StandardNewTabPage(
            bookmarks = bookmarks,
            activeDownloadCount = activeDownloadCount,
            onSearchClick = onSearchClick,
            onOpenUrl = onOpenUrl,
            onOpenDownloads = onOpenDownloads,
            onOpenIncognito = onOpenIncognito,
            modifier = modifier
        )
    }
}

@Composable
private fun IncognitoNewTabPage(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = Color(0xFF202124)
    val cardColor = Color(0xFF2B2C30)
    val pillColor = Color(0xFF303134)
    val primaryText = Color(0xFFE8EAED)
    val secondaryText = Color(0xFF9AA0A6)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 36.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = CircleShape,
                color = pillColor,
                modifier = Modifier.size(84.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = "Incognito",
                        tint = primaryText,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "You've gone Incognito",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = primaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Pages you view in incognito tabs won't stick around in your browser's history, cookies, or search history after you've closed all your incognito tabs.",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryText,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                shape = RoundedCornerShape(32.dp),
                color = pillColor,
                border = BorderStroke(1.dp, Color(0xFF3C4043)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .clickable(onClick = onSearchClick)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = secondaryText,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Search or type URL",
                        style = MaterialTheme.typography.bodyLarge,
                        color = secondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PrivacyTip,
                        contentDescription = null,
                        tint = secondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Downloads and bookmarks you save will still be visible outside Incognito. Your activity may still be visible to websites you visit or your network.",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryText,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StandardNewTabPage(
    bookmarks: List<BookmarkEntity>,
    activeDownloadCount: Int,
    onSearchClick: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenIncognito: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shortcuts = remember(bookmarks) {
        if (bookmarks.isNotEmpty()) {
            bookmarks.take(8).map { bookmark ->
                val displayTitle = bookmark.title.ifBlank {
                    Uri.parse(bookmark.url).host?.removePrefix("www.") ?: bookmark.url
                }
                QuickShortcut(title = displayTitle, url = bookmark.url)
            }
        } else {
            DefaultShortcuts
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Brand header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Text(
                    text = "Siphon",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Large M3 Expressive Search Pill
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .clickable(onClick = onSearchClick)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Search or type URL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Action Chips (Downloads + Incognito)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onOpenDownloads,
                    shape = RoundedCornerShape(50),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Downloads",
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (activeDownloadCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text(text = activeDownloadCount.toString())
                                }
                            }
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        leadingIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )

                AssistChip(
                    onClick = onOpenIncognito,
                    shape = RoundedCornerShape(50),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Incognito",
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Quick Shortcuts Grid (4 columns, up to 8 items)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val rows = shortcuts.chunked(4)
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { shortcut ->
                            ShortcutTile(
                                shortcut = shortcut,
                                onClick = { onOpenUrl(shortcut.url) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutTile(
    shortcut: QuickShortcut,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initial = remember(shortcut.title, shortcut.url) {
        val cleaned = shortcut.title.trim()
        if (cleaned.isNotEmpty()) {
            cleaned.first().uppercaseChar().toString()
        } else {
            "?"
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(PaddingValues(vertical = 8.dp, horizontal = 4.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = shortcut.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
