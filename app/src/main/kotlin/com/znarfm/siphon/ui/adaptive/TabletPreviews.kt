package com.znarfm.siphon.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.znarfm.siphon.ui.AppScreen
import com.znarfm.siphon.ui.browser.BrowserTab
import com.znarfm.siphon.ui.browser.TabletBrowserToolbar
import com.znarfm.siphon.ui.theme.AppColorSeed
import com.znarfm.siphon.ui.theme.AppThemeMode
import com.znarfm.siphon.ui.theme.SiphonTheme

@PreviewScreenSizes
@Composable
private fun TabletNavigationRailPreview() {
    SiphonTheme(
        themeMode = AppThemeMode.DARK,
        colorSeed = AppColorSeed.SAPPHIRE
    ) {
        TabletNavigationRail(
            currentScreen = AppScreen.BROWSER,
            activeDownloadsCount = 3,
            tabCount = 4,
            onNavigate = {},
            onNewTab = {}
        )
    }
}

@PreviewScreenSizes
@Composable
private fun TabletBrowserToolbarPreview() {
    val sampleTabs = listOf(
        BrowserTab(
            id = "tab-1",
            url = "https://developer.android.com/jetpack/compose",
            title = "Jetpack Compose"
        ),
        BrowserTab(
            id = "tab-2",
            url = "https://github.com/znarfm/siphon",
            title = "znarfm/siphon"
        ),
        BrowserTab(
            id = "tab-3",
            url = "https://kotlinlang.org",
            title = "Kotlin Programming Language",
            isIncognito = true
        )
    )

    SiphonTheme(
        themeMode = AppThemeMode.DARK,
        colorSeed = AppColorSeed.SAPPHIRE
    ) {
        TabletBrowserToolbar(
            tabs = sampleTabs,
            activeTabId = "tab-1",
            url = "https://developer.android.com/jetpack/compose",
            title = "Jetpack Compose",
            isIncognito = false,
            isLoading = false,
            pageProgress = 100,
            canGoBack = true,
            canGoForward = false,
            isBookmarked = true,
            isAdBlockEnabled = true,
            detectedMediaCount = 2,
            activeDownloadCount = 1,
            onSelectTab = {},
            onCloseTab = {},
            onNewTab = {},
            onGoBack = {},
            onGoForward = {},
            onReload = {},
            onStop = {},
            onOpenOmniboxSearch = {},
            onOpenSiteInfo = {},
            onToggleBookmark = {},
            onOpenTabSwitcher = {},
            onOpenSniffer = {},
            onOpenDownloads = {},
            onOpenMenu = {}
        )
    }
}
