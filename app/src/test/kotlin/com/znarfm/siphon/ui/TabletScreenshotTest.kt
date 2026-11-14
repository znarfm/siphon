package com.znarfm.siphon.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.znarfm.siphon.ui.adaptive.TabletNavigationRail
import com.znarfm.siphon.ui.browser.BrowserTab
import com.znarfm.siphon.ui.browser.TabletBrowserToolbar
import com.znarfm.siphon.ui.theme.AppColorSeed
import com.znarfm.siphon.ui.theme.AppThemeMode
import com.znarfm.siphon.ui.theme.SiphonTheme
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class TabletScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_C
    )

    @Before
    fun checkSnapshots() {
        Assume.assumeTrue(
            "Snapshots not recorded yet; run ./gradlew recordPaparazziDebug",
            File("src/test/snapshots").exists() || java.lang.Boolean.getBoolean("paparazzi.test.record")
        )
    }

    @Test
    fun tabletNavigationRail() {
        paparazzi.snapshot {
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
    }

    @Test
    fun tabletBrowserToolbar() {
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

        paparazzi.snapshot {
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
    }
}
