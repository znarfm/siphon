package com.znarfm.siphon.ui.browser

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.znarfm.siphon.data.repository.BrowserPreferences
import com.znarfm.siphon.ui.theme.AppColorSeed
import com.znarfm.siphon.ui.theme.AppThemeMode
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.TabularNumberStyle

private data class SearchEngineOption(
    val name: String,
    val urlPrefix: String
)

private val SEARCH_ENGINES = listOf(
    SearchEngineOption("Google", "https://www.google.com/search?q="),
    SearchEngineOption("DuckDuckGo", "https://duckduckgo.com/?q="),
    SearchEngineOption("Bing", "https://www.bing.com/search?q="),
    SearchEngineOption("Brave", "https://search.brave.com/search?q="),
    SearchEngineOption("Startpage", "https://www.startpage.com/do/search?query=")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowserSettingsScreen(
    browserPreferences: BrowserPreferences,
    themeMode: AppThemeMode,
    colorSeed: AppColorSeed,
    isAdBlockEnabled: Boolean,
    adblockRuleCount: Int,
    enabledListCount: Int = 0,
    onSetSearchEngine: (String) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetColorSeed: (AppColorSeed) -> Unit,
    onToggleAdBlock: () -> Unit,
    onOpenAdblockSubscriptions: () -> Unit,
    onOpenDownloadSettings: () -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Clear browsing history?") },
            text = {
                Text("This will permanently delete all visited pages from your browsing history.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        onClearHistory()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Engine
            item {
                SettingsSectionHeader(
                    icon = Icons.Rounded.Public,
                    title = "Search Engine"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SEARCH_ENGINES.forEachIndexed { index, engine ->
                            val isSelected = browserPreferences.searchEngine == engine.urlPrefix
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSetSearchEngine(engine.urlPrefix) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = engine.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (index < SEARCH_ENGINES.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }

            // Appearance
            item {
                SettingsSectionHeader(
                    icon = Icons.Rounded.Palette,
                    title = "Appearance"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Theme Mode",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val themeModes = AppThemeMode.entries
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                themeModes.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        selected = mode == themeMode,
                                        onClick = { onSetThemeMode(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = themeModes.size
                                        ),
                                        label = {
                                            Text(
                                                text = mode.title,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Color Accent",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val availableSeeds = remember {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    AppColorSeed.entries
                                } else {
                                    AppColorSeed.entries.filter { it != AppColorSeed.DYNAMIC }
                                }
                            }
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                itemVerticalAlignment = Alignment.CenterVertically
                            ) {
                                availableSeeds.forEach { seed ->
                                    val isSelected = if (
                                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                                        colorSeed == AppColorSeed.DYNAMIC
                                    ) {
                                        seed == AppColorSeed.SAPPHIRE
                                    } else {
                                        seed == colorSeed
                                    }

                                    if (seed == AppColorSeed.DYNAMIC) {
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onSetColorSeed(seed) },
                                            label = { Text("Monet") },
                                            leadingIcon = if (isSelected) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            } else {
                                                null
                                            },
                                            shape = ExpressiveShapeTokens.PillShape,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        )
                                    } else {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(seed.swatch)
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onSurface
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                    shape = CircleShape
                                                )
                                                .clickable { onSetColorSeed(seed) }
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = seed.title,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Privacy & Ad Blocker
            item {
                SettingsSectionHeader(
                    icon = Icons.Rounded.Shield,
                    title = "Privacy & Ad Blocker"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onToggleAdBlock)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ad Blocker",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isAdBlockEnabled) "Blocking ads and trackers" else "Disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isAdBlockEnabled,
                                onCheckedChange = { onToggleAdBlock() }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsNavigationRow(
                            icon = Icons.Rounded.Tune,
                            title = "Manage Adblock Filter Lists",
                            subtitle = "$adblockRuleCount active rules • $enabledListCount lists enabled",
                            onClick = onOpenAdblockSubscriptions
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsNavigationRow(
                            icon = Icons.Rounded.DeleteSweep,
                            title = "Clear Browsing History",
                            subtitle = "Remove all visited pages",
                            titleColor = MaterialTheme.colorScheme.error,
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = { showClearHistoryDialog = true }
                        )
                    }
                }
            }

            // Downloads
            item {
                SettingsSectionHeader(
                    icon = Icons.Rounded.Download,
                    title = "Downloads"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsNavigationRow(
                        icon = Icons.Rounded.Download,
                        title = "Download Concurrency & Storage Folder",
                        subtitle = "Configure workers, speed limit, Wi-Fi only, and folder",
                        onClick = onOpenDownloadSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
