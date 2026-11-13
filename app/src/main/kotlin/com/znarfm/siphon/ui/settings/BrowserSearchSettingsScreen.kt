package com.znarfm.siphon.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.znarfm.siphon.data.repository.BrowserPreferences
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens

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

internal fun searchEngineDisplayName(urlPrefix: String): String {
    return SEARCH_ENGINES.firstOrNull { it.urlPrefix == urlPrefix }?.name ?: "Google"
}

@Composable
fun BrowserSearchSettingsScreen(
    browserPreferences: BrowserPreferences,
    onSetSearchEngine: (String) -> Unit,
    onSetWebDarkMode: (Boolean) -> Unit,
    onSetDesktopMode: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSubPageScaffold(
        title = "Browser & Search",
        onBack = onBack,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "section_search_engine") {
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

            item(key = "section_browsing_defaults") {
                SettingsSectionHeader(
                    icon = Icons.Rounded.Tune,
                    title = "Browsing Defaults"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = ExpressiveShapeTokens.RestingCardShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsSwitchRow(
                            icon = Icons.Rounded.DarkMode,
                            title = "Web Dark Mode",
                            subtitle = "Force dark appearance on web pages",
                            checked = browserPreferences.webDarkMode,
                            onCheckedChange = onSetWebDarkMode
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        SettingsSwitchRow(
                            icon = Icons.Rounded.DesktopWindows,
                            title = "Desktop Mode",
                            subtitle = "Request desktop sites by default",
                            checked = browserPreferences.desktopMode,
                            onCheckedChange = onSetDesktopMode
                        )
                    }
                }
            }
        }
    }
}
