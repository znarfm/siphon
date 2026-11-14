package com.znarfm.siphon.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration

enum class WindowWidthSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

enum class WindowHeightSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

data class SiphonWindowSizeClass(
    val width: WindowWidthSizeClass,
    val height: WindowHeightSizeClass
) {
    val isTablet: Boolean
        get() = width != WindowWidthSizeClass.COMPACT

    val isExpanded: Boolean
        get() = width == WindowWidthSizeClass.EXPANDED
}

val LocalWindowSizeClass = staticCompositionLocalOf {
    SiphonWindowSizeClass(
        width = WindowWidthSizeClass.COMPACT,
        height = WindowHeightSizeClass.MEDIUM
    )
}

@Composable
fun rememberWindowSizeClass(): SiphonWindowSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    return remember(screenWidthDp, screenHeightDp) {
        val width = when {
            screenWidthDp < 600 -> WindowWidthSizeClass.COMPACT
            screenWidthDp < 840 -> WindowWidthSizeClass.MEDIUM
            else -> WindowWidthSizeClass.EXPANDED
        }
        val height = when {
            screenHeightDp < 480 -> WindowHeightSizeClass.COMPACT
            screenHeightDp < 900 -> WindowHeightSizeClass.MEDIUM
            else -> WindowHeightSizeClass.EXPANDED
        }
        SiphonWindowSizeClass(width = width, height = height)
    }
}
