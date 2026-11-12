package com.siphon.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Immutable
enum class AppColorSeed(val title: String, val swatch: Color) {
    DYNAMIC("Auto", Color(0xFF6750A4)),
    SAPPHIRE("Blue", Color(0xFF3B82F6)),
    VIOLET("Violet", Color(0xFF8B5CF6)),
    ROSE("Rose", Color(0xFFF43F5E)),
    AMBER("Amber", Color(0xFFF59E0B)),
    EMERALD("Teal", Color(0xFF10B981));

    fun toLightScheme(): ColorScheme {
        val primary = lerp(swatch, Color.Black, 0.22f)
        val container = lerp(swatch, Color.White, 0.80f)
        val onContainer = lerp(swatch, Color.Black, 0.72f)
        val tertiary = lerp(swatch, Color(0xFFEC4899), 0.35f)
        val tertiaryContainer = lerp(tertiary, Color.White, 0.82f)
        return lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = lerp(primary, Color(0xFF475569), 0.25f),
            onSecondary = Color.White,
            secondaryContainer = lerp(swatch, Color.White, 0.86f),
            onSecondaryContainer = onContainer,
            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = lerp(tertiary, Color.Black, 0.70f),
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFF475569),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF1F5F9),
            surfaceContainer = Color(0xFFEAF0F6),
            surfaceContainerHigh = Color(0xFFE2E8F0),
            surfaceContainerHighest = Color(0xFFCBD5E1),
            outline = Color(0xFF64748B),
            outlineVariant = Color(0xFFCBD5E1)
        )
    }

    fun toDarkScheme(): ColorScheme {
        val primary = lerp(swatch, Color.White, 0.28f)
        val onPrimary = lerp(swatch, Color.Black, 0.74f)
        val container = lerp(swatch, Color.Black, 0.42f)
        val onContainer = lerp(swatch, Color.White, 0.85f)
        val tertiary = lerp(swatch, Color(0xFFF472B6), 0.38f)
        val tertiaryContainer = lerp(tertiary, Color.Black, 0.48f)
        return darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = lerp(primary, Color(0xFF94A3B8), 0.25f),
            onSecondary = onPrimary,
            secondaryContainer = lerp(swatch, Color.Black, 0.56f),
            onSecondaryContainer = onContainer,
            tertiary = tertiary,
            onTertiary = lerp(tertiary, Color.Black, 0.75f),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = lerp(tertiary, Color.White, 0.85f),
            background = Color(0xFF0B0F14),
            onBackground = Color(0xFFE2E8F0),
            surface = Color(0xFF10161D),
            onSurface = Color(0xFFE2E8F0),
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = Color(0xFF94A3B8),
            surfaceContainerLowest = Color(0xFF070A0E),
            surfaceContainerLow = Color(0xFF121921),
            surfaceContainer = Color(0xFF161F29),
            surfaceContainerHigh = Color(0xFF1E2936),
            surfaceContainerHighest = Color(0xFF263445),
            outline = Color(0xFF334155),
            outlineVariant = Color(0xFF1E293B)
        )
    }
}

// State colors with refined luminance for dark and light modes
val StateActive = Color(0xFF00E676)
val StatePaused = Color(0xFFFFB300)
val StateWaiting = Color(0xFFFFD54F)
val StateExpired = Color(0xFFFF5252)
val StateCompleted = Color(0xFF00E676)
val StateFailed = Color(0xFFFF5252)

