package com.znarfm.siphon.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
enum class AppThemeMode(val title: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED")
}

private fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF0E0E0E),
    surfaceContainerHigh = Color(0xFF151515),
    surfaceContainerHighest = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF141414)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SiphonTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    colorSeed: AppColorSeed = AppColorSeed.DYNAMIC,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
    }

    val context = LocalContext.current
    val useDynamic = colorSeed == AppColorSeed.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val baseScheme = if (useDynamic) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val effectiveSeed = if (colorSeed == AppColorSeed.DYNAMIC) AppColorSeed.SAPPHIRE else colorSeed
        if (isDark) effectiveSeed.toDarkScheme() else effectiveSeed.toLightScheme()
    }

    val colorScheme = if (themeMode == AppThemeMode.AMOLED) {
        baseScheme.toAmoled()
    } else {
        baseScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ExpressiveShapes,
        typography = ExpressiveTypography,
        content = content
    )
}

