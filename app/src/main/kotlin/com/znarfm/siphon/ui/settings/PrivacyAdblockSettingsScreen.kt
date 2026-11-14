package com.znarfm.siphon.ui.settings

import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.znarfm.siphon.core.adblock.AdblockSettings
import com.znarfm.siphon.core.adblock.FilterListCategory
import com.znarfm.siphon.core.adblock.FilterListSource
import com.znarfm.siphon.core.adblock.NativeAdblockJni
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.TabularNumberStyle

private enum class FilterManagerTab(val label: String) {
    BUILT_IN("Built-in"),
    CUSTOM_LISTS("Custom Lists"),
    CUSTOM_RULES("Custom Rules")
}

@Composable
fun PrivacyAdblockSettingsScreen(
    isAdBlockEnabled: Boolean,
    adblockSettings: AdblockSettings,
    activeRuleCount: Int,
    onToggleAdBlock: () -> Unit,
    onToggleList: (id: String, enabled: Boolean) -> Unit,
    onAddCustomList: (name: String, url: String) -> Unit,
    onRemoveCustomList: (id: String) -> Unit,
    onUpdateCustomRules: (String) -> Unit,
    onRefreshSubscriptions: (onComplete: () -> Unit) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(FilterManagerTab.BUILT_IN) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    var customNameInput by remember { mutableStateOf("") }
    var customUrlInput by remember { mutableStateOf("") }
    var customRulesDraft by remember(adblockSettings.customUserRules) {
        mutableStateOf(adblockSettings.customUserRules)
    }

    val importRulesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val imported = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                reader.readLines().joinToString("\n")
            }
        }.getOrNull()
        if (!imported.isNullOrBlank()) {
            val merged = if (customRulesDraft.isBlank()) {
                imported
            } else {
                "${customRulesDraft.trimEnd()}\n$imported"
            }
            customRulesDraft = merged
            onUpdateCustomRules(merged)
            Toast.makeText(context, "Imported rules from file", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Could not read rules from file", Toast.LENGTH_SHORT).show()
        }
    }

    val builtInLists = remember(adblockSettings.enabledLists) {
        adblockSettings.enabledLists.filter { it.isBuiltIn }
    }
    val customLists = remember(adblockSettings.enabledLists) {
        adblockSettings.enabledLists.filter { !it.isBuiltIn }
    }
    val enabledCount = remember(adblockSettings.enabledLists) {
        adblockSettings.enabledLists.count { it.isEnabled }
    }
    val groupedBuiltInLists = remember(builtInLists) {
        FilterListCategory.entries.mapNotNull { category ->
            val items = builtInLists.filter { it.category == category }
            if (items.isEmpty()) null else category to items
        }
    }

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

    SettingsSubPageScaffold(
        title = "Privacy & Ad Blocker",
        onBack = onBack,
        showBackButton = showBackButton,
        modifier = modifier
    ) { innerPadding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Global Ad Blocker Switch Card
            item(key = "adblock_global_toggle") {
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Shield,
                        title = "Ad Blocker",
                        subtitle = if (isAdBlockEnabled) "Blocking ads and trackers" else "Disabled",
                        checked = isAdBlockEnabled,
                        onCheckedChange = { onToggleAdBlock() }
                    )
                }
            }

            if (!NativeAdblockJni.isLoaded) {
                item(key = "native_jni_missing_warning") {
                    Surface(
                        shape = ExpressiveShapeTokens.RestingCardShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Native adblock engine not loaded",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "libadblock_jni.so is unavailable on this build. Only basic host fallback is active; cosmetic filtering and advanced filter rules are disabled.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Filter Lists & Rules Header + Update & Apply + Segmented Tabs
            item(key = "filter_lists_header") {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader(
                    icon = Icons.Rounded.Tune,
                    title = "Filter Lists & Rules"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$activeRuleCount active rules • $enabledCount lists enabled",
                        style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            isRefreshing = true
                            onUpdateCustomRules(customRulesDraft)
                            onRefreshSubscriptions {
                                isRefreshing = false
                            }
                        },
                        enabled = !isRefreshing,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Updating...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update & Apply")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val tabs = FilterManagerTab.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { index, tab ->
                        SegmentedButton(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = tabs.size
                            ),
                            label = {
                                Text(
                                    text = tab.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }
            }

            // Active Tab Content (rendered directly into the parent LazyColumn)
            when (selectedTab) {
                FilterManagerTab.BUILT_IN -> {
                    groupedBuiltInLists.forEach { (category, sources) ->
                        item(key = "header_${category.name}") {
                            Text(
                                text = formatCategoryName(category),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
                            )
                        }
                        items(sources, key = { it.id }) { source ->
                            BuiltInFilterListItem(
                                source = source,
                                onToggle = { enabled -> onToggleList(source.id, enabled) }
                            )
                        }
                    }
                }

                FilterManagerTab.CUSTOM_LISTS -> {
                    item(key = "add_custom_list_form") {
                        Surface(
                            shape = ExpressiveShapeTokens.RestingCardShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Add Filter List URL",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                OutlinedTextField(
                                    value = customNameInput,
                                    onValueChange = { customNameInput = it },
                                    label = { Text("Name (optional)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = customUrlInput,
                                    onValueChange = { customUrlInput = it },
                                    label = { Text("Filter list URL") },
                                    placeholder = { Text("https://example.com/filters.txt") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                val isUrlValid = customUrlInput.trim().let {
                                    it.startsWith("https://", ignoreCase = true) ||
                                        it.startsWith("http://", ignoreCase = true)
                                }
                                FilledTonalButton(
                                    onClick = {
                                        onAddCustomList(customNameInput.trim(), customUrlInput.trim())
                                        customNameInput = ""
                                        customUrlInput = ""
                                    },
                                    enabled = isUrlValid,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Custom List")
                                }
                            }
                        }
                    }

                    if (customLists.isEmpty()) {
                        item(key = "empty_custom_lists") {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No custom filter lists added yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(customLists, key = { it.id }) { source ->
                            CustomFilterListItem(
                                source = source,
                                onToggle = { enabled -> onToggleList(source.id, enabled) },
                                onRemove = { onRemoveCustomList(source.id) }
                            )
                        }
                    }
                }

                FilterManagerTab.CUSTOM_RULES -> {
                    item(key = "custom_rules_editor") {
                        Surface(
                            shape = ExpressiveShapeTokens.RestingCardShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Add ABP, uBlock Origin, or hosts rules (one per line):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = customRulesDraft,
                                    onValueChange = { updated ->
                                        customRulesDraft = updated
                                        onUpdateCustomRules(updated)
                                    },
                                    placeholder = {
                                        Text("! Example:\n||ads.example.com^\n@@||trusted.example.com^\nexample.com##.ad-banner")
                                    },
                                    minLines = 7,
                                    maxLines = Int.MAX_VALUE,
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            importRulesLauncher.launch(arrayOf("text/plain", "text/*"))
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FileOpen,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Import .txt")
                                    }
                                    if (customRulesDraft.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                customRulesDraft = ""
                                                onUpdateCustomRules("")
                                            }
                                        ) {
                                            Text("Clear")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Browsing Data Section
            item(key = "section_browsing_data") {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader(
                    icon = Icons.Rounded.DeleteSweep,
                    title = "Browsing Data"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
    }
}

@Composable
private fun BuiltInFilterListItem(
    source: FilterListSource,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!source.isEnabled) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = source.isEnabled,
                onCheckedChange = onToggle
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = ExpressiveShapeTokens.PillShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = formatCategoryName(source.category),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                if (source.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = source.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FilterSourceMetaLine(source = source)
            }
        }
    }
}

@Composable
private fun CustomFilterListItem(
    source: FilterListSource,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = source.isEnabled,
                onCheckedChange = onToggle
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name.ifBlank { "Custom List" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = source.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FilterSourceMetaLine(source = source)
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Remove custom list",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun FilterSourceMetaLine(source: FilterListSource) {
    val metaParts = buildList {
        if (source.ruleCount > 0) {
            add("${source.ruleCount} rules")
        }
        if (source.lastUpdatedMs > 0L) {
            add("Updated ${formatRelativeUpdatedTime(source.lastUpdatedMs)}")
        }
    }
    if (metaParts.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = metaParts.joinToString(" • "),
            style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
            color = MaterialTheme.colorScheme.primary
        )
    }
    if (!source.lastError.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Error: ${source.lastError}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatCategoryName(category: FilterListCategory): String {
    return category.name.lowercase().replaceFirstChar { it.uppercase() }
}

private fun formatRelativeUpdatedTime(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    if (now - timestampMs < DateUtils.MINUTE_IN_MILLIS) return "just now"
    return DateUtils.getRelativeTimeSpanString(
        timestampMs,
        now,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}
