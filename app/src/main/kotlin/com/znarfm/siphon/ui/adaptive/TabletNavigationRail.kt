package com.znarfm.siphon.ui.adaptive

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.znarfm.siphon.ui.AppScreen

@Composable
fun TabletNavigationRail(
    currentScreen: AppScreen,
    activeDownloadsCount: Int,
    tabCount: Int,
    onNavigate: (AppScreen) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSettingsSelected = currentScreen == AppScreen.SETTINGS ||
        currentScreen.name.startsWith("SETTINGS_")

    NavigationRail(
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        header = {
            FloatingActionButton(onClick = onNewTab) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "New tab"
                )
            }
        }
    ) {
        NavigationRailItem(
            selected = currentScreen == AppScreen.BROWSER,
            onClick = { onNavigate(AppScreen.BROWSER) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.BROWSER) {
                        Icons.Rounded.Public
                    } else {
                        Icons.Outlined.Public
                    },
                    contentDescription = "Browser"
                )
            },
            label = {
                Text(
                    text = "Browser",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )

        NavigationRailItem(
            selected = currentScreen == AppScreen.QUEUE,
            onClick = { onNavigate(AppScreen.QUEUE) },
            icon = {
                BadgedBox(
                    badge = {
                        if (activeDownloadsCount > 0) {
                            Badge {
                                Text(text = activeDownloadsCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (currentScreen == AppScreen.QUEUE) {
                            Icons.Rounded.Download
                        } else {
                            Icons.Outlined.Download
                        },
                        contentDescription = "Queue"
                    )
                }
            },
            label = {
                Text(
                    text = "Queue",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )

        NavigationRailItem(
            selected = currentScreen == AppScreen.TAB_SWITCHER,
            onClick = { onNavigate(AppScreen.TAB_SWITCHER) },
            icon = {
                BadgedBox(
                    badge = {
                        if (tabCount > 0) {
                            Badge {
                                Text(text = tabCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (currentScreen == AppScreen.TAB_SWITCHER) {
                            Icons.Rounded.Tab
                        } else {
                            Icons.Outlined.Tab
                        },
                        contentDescription = "Tabs"
                    )
                }
            },
            label = {
                Text(
                    text = "Tabs",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )

        NavigationRailItem(
            selected = currentScreen == AppScreen.BOOKMARKS,
            onClick = { onNavigate(AppScreen.BOOKMARKS) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.BOOKMARKS) {
                        Icons.Rounded.Bookmarks
                    } else {
                        Icons.Outlined.Bookmarks
                    },
                    contentDescription = "Bookmarks"
                )
            },
            label = {
                Text(
                    text = "Bookmarks",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )

        NavigationRailItem(
            selected = currentScreen == AppScreen.HISTORY,
            onClick = { onNavigate(AppScreen.HISTORY) },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = "History"
                )
            },
            label = {
                Text(
                    text = "History",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )

        NavigationRailItem(
            selected = isSettingsSelected,
            onClick = { onNavigate(AppScreen.SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (isSettingsSelected) {
                        Icons.Rounded.Settings
                    } else {
                        Icons.Outlined.Settings
                    },
                    contentDescription = "Settings"
                )
            },
            label = {
                Text(
                    text = "Settings",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )
    }
}
