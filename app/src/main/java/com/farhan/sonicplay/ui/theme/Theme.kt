package com.farhan.sonicplay.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.farhan.sonicplay.data.model.ThemeMode

val SonicPrimary = Color(0xFF00E5FF)
val SonicSecondary = Color(0xFF7C4DFF)
val SonicTertiary = Color(0xFF00E676)

val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF08080A)
val AmoledCard = Color(0xFF13141B)
val AmoledCardElevated = Color(0xFF1E202B)

val DarkBackground = Color(0xFF0F1117)
val DarkSurface = Color(0xFF181B24)
val DarkCard = Color(0xFF222634)

val LightBackground = Color(0xFFF7F9FC)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFEDF2F7)

fun parseHexColor(hex: String, fallback: Color = SonicPrimary): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

@Composable
fun SonicPlayTheme(
    themeMode: ThemeMode = ThemeMode.AMOLED,
    useDynamicColor: Boolean = true,
    accentColorHex: String = "#00E5FF",
    content: @Composable () -> Unit
) {
    val isSystemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    val customAccent = parseHexColor(accentColorHex, SonicPrimary)

    val colorScheme = when {
        themeMode == ThemeMode.AMOLED -> {
            darkColorScheme(
                primary = customAccent,
                onPrimary = Color.Black,
                secondary = SonicSecondary,
                onSecondary = Color.White,
                tertiary = SonicTertiary,
                background = AmoledBackground,
                onBackground = Color(0xFFF0F0F0),
                surface = AmoledSurface,
                onSurface = Color(0xFFF0F0F0),
                surfaceVariant = AmoledCard,
                onSurfaceVariant = Color(0xFFB0B3C1)
            )
        }
        isDark -> {
            darkColorScheme(
                primary = customAccent,
                onPrimary = Color.Black,
                secondary = SonicSecondary,
                onSecondary = Color.White,
                tertiary = SonicTertiary,
                background = DarkBackground,
                onBackground = Color(0xFFE6E8EE),
                surface = DarkSurface,
                onSurface = Color(0xFFE6E8EE),
                surfaceVariant = DarkCard,
                onSurfaceVariant = Color(0xFFA0A5B5)
            )
        }
        else -> {
            lightColorScheme(
                primary = customAccent,
                onPrimary = Color.White,
                secondary = SonicSecondary,
                onSecondary = Color.White,
                tertiary = SonicTertiary,
                background = LightBackground,
                onBackground = Color(0xFF1A1C23),
                surface = LightSurface,
                onSurface = Color(0xFF1A1C23),
                surfaceVariant = LightCard,
                onSurfaceVariant = Color(0xFF5A6070)
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
