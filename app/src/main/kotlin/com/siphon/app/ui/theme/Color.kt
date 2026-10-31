package com.siphon.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

enum class AppColorSeed(val title: String, val swatch: Color) {
    DYNAMIC("Auto", Color(0xFF6750A4)),
    SAPPHIRE("Blue", Color(0xFF3B82F6)),
    VIOLET("Violet", Color(0xFF8B5CF6)),
    ROSE("Rose", Color(0xFFF43F5E)),
    AMBER("Amber", Color(0xFFF59E0B)),
    EMERALD("Teal", Color(0xFF10B981));

    fun toLightScheme(): ColorScheme {
        val primary = lerp(swatch, Color.Black, 0.25f)
        val container = lerp(swatch, Color.White, 0.82f)
        val onContainer = lerp(swatch, Color.Black, 0.70f)
        return lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = primary.copy(alpha = 0.85f),
            onSecondary = Color.White,
            secondaryContainer = container.copy(alpha = 0.7f),
            onSecondaryContainer = onContainer,
            tertiary = primary,
            onTertiary = Color.White,
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFF64748B)
        )
    }

    fun toDarkScheme(): ColorScheme {
        val primary = lerp(swatch, Color.White, 0.28f)
        val onPrimary = lerp(swatch, Color.Black, 0.72f)
        val container = lerp(swatch, Color.Black, 0.45f)
        val onContainer = lerp(swatch, Color.White, 0.82f)
        return darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = primary.copy(alpha = 0.85f),
            onSecondary = onPrimary,
            secondaryContainer = container.copy(alpha = 0.65f),
            onSecondaryContainer = onContainer,
            tertiary = primary,
            onTertiary = onPrimary,
            background = Color(0xFF0F141A),
            onBackground = Color(0xFFE2E8F0),
            surface = Color(0xFF131A22),
            onSurface = Color(0xFFE2E8F0),
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF334155)
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
