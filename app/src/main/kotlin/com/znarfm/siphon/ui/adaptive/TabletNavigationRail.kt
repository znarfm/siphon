package com.znarfm.siphon.ui.adaptive

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.znarfm.siphon.ui.theme.SiphonIcons

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
                    imageVector = SiphonIcons.Add,
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
                        SiphonIcons.Public
                    } else {
                        SiphonIcons.Outlined.Public
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
                            SiphonIcons.Download
                        } else {
                            SiphonIcons.Outlined.Download
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
                            SiphonIcons.Tab
                        } else {
                            SiphonIcons.Outlined.Tab
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
                        SiphonIcons.Bookmarks
                    } else {
                        SiphonIcons.Outlined.Bookmarks
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
                    imageVector = SiphonIcons.History,
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
                        SiphonIcons.Settings
                    } else {
                        SiphonIcons.Outlined.Settings
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
