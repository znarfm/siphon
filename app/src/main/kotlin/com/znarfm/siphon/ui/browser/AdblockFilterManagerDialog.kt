package com.znarfm.siphon.ui.browser

import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.TabularNumberStyle

private enum class FilterManagerTab(val label: String) {
    BUILT_IN("Built-in"),
    CUSTOM_LISTS("Custom Lists"),
    CUSTOM_RULES("Custom Rules")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdblockFilterManagerDialog(
    settings: AdblockSettings,
    activeRuleCount: Int,
    onToggleList: (id: String, enabled: Boolean) -> Unit,
    onAddCustomList: (name: String, url: String) -> Unit,
    onRemoveCustomList: (id: String) -> Unit,
    onUpdateCustomRules: (String) -> Unit,
    onRefreshSubscriptions: (onComplete: () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(FilterManagerTab.BUILT_IN) }
    var isRefreshing by remember { mutableStateOf(false) }

    var customNameInput by remember { mutableStateOf("") }
    var customUrlInput by remember { mutableStateOf("") }
    var customRulesDraft by remember(settings.customUserRules) {
        mutableStateOf(settings.customUserRules)
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

    val builtInLists = remember(settings.enabledLists) {
        settings.enabledLists.filter { it.isBuiltIn }
    }
    val customLists = remember(settings.enabledLists) {
        settings.enabledLists.filter { !it.isBuiltIn }
    }
    val enabledCount = remember(settings.enabledLists) {
        settings.enabledLists.count { it.isEnabled }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Adblock Filter Manager",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$activeRuleCount active rules • $enabledCount lists enabled",
                        style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Switcher
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

            Spacer(modifier = Modifier.height(12.dp))

            // Body Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp, max = 420.dp)
            ) {
                when (selectedTab) {
                    FilterManagerTab.BUILT_IN -> {
                        val grouped = remember(builtInLists) {
                            FilterListCategory.entries
                                .mapNotNull { category ->
                                    val items = builtInLists.filter { it.category == category }
                                    if (items.isEmpty()) null else category to items
                                }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            grouped.forEach { (category, sources) ->
                                item(key = "header_${category.name}") {
                                    Text(
                                        text = formatCategoryName(category),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp)
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
                    }

                    FilterManagerTab.CUSTOM_LISTS -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
                    }

                    FilterManagerTab.CUSTOM_RULES -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
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
                                maxLines = 12,
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

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
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
                    shape = RoundedCornerShape(14.dp)
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
