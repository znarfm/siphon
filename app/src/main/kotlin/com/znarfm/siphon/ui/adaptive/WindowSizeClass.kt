package com.znarfm.siphon.ui.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.window.core.layout.WindowHeightSizeClass as CoreWindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass as CoreWindowWidthSizeClass

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
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    return remember(windowSizeClass) {
        val width = when (windowSizeClass.windowWidthSizeClass) {
            CoreWindowWidthSizeClass.COMPACT -> WindowWidthSizeClass.COMPACT
            CoreWindowWidthSizeClass.MEDIUM -> WindowWidthSizeClass.MEDIUM
            CoreWindowWidthSizeClass.EXPANDED -> WindowWidthSizeClass.EXPANDED
            else -> WindowWidthSizeClass.EXPANDED
        }
        val height = when (windowSizeClass.windowHeightSizeClass) {
            CoreWindowHeightSizeClass.COMPACT -> WindowHeightSizeClass.COMPACT
            CoreWindowHeightSizeClass.MEDIUM -> WindowHeightSizeClass.MEDIUM
            CoreWindowHeightSizeClass.EXPANDED -> WindowHeightSizeClass.EXPANDED
            else -> WindowHeightSizeClass.EXPANDED
        }
        SiphonWindowSizeClass(width = width, height = height)
    }
}
