package com.znarfm.siphon.ui.settings

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.znarfm.siphon.ui.theme.AppColorSeed
import com.znarfm.siphon.ui.theme.AppThemeMode
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens
import com.znarfm.siphon.ui.theme.SiphonIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceSettingsScreen(
    themeMode: AppThemeMode,
    colorSeed: AppColorSeed,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetColorSeed: (AppColorSeed) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true
) {
    SettingsSubPageScaffold(
        title = "Appearance",
        onBack = onBack,
        showBackButton = showBackButton,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "section_appearance") {
                SettingsSectionHeader(
                    icon = SiphonIcons.Palette,
                    title = "Theme & Color"
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
                                                        imageVector = SiphonIcons.Check,
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
                                                    imageVector = SiphonIcons.Check,
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
        }
    }
}
