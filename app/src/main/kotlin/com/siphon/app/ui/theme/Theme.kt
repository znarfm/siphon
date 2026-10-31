package com.siphon.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF161616),
    surfaceContainerHighest = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF141414)
)

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

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ExpressiveShapes,
        content = content
    )
}
