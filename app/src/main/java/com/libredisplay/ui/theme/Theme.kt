package com.libredisplay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorLowBackground = LibreCareColors.AccentRed
val ColorInRangeBackground = LibreCareColors.AccentTeal
val ColorHighBackground = LibreCareColors.AccentAmber
val ColorStaleBackground = LibreCareColors.TextMuted

val ColorOnDark = LibreCareColors.TextPrimary
val ColorSubtitle = LibreCareColors.TextSecondary

private val DarkColorScheme = darkColorScheme(
    primary = LibreCareColors.AccentTeal,
    onPrimary = LibreCareColors.Background,
    secondary = LibreCareColors.AccentBlue,
    onSecondary = LibreCareColors.Background,
    background = LibreCareColors.Background,
    onBackground = LibreCareColors.TextPrimary,
    surface = LibreCareColors.Surface,
    onSurface = LibreCareColors.TextPrimary,
    surfaceVariant = LibreCareColors.SurfaceElevated,
    onSurfaceVariant = LibreCareColors.TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2F6FED),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF16865C),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF172033),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF172033),
    surfaceVariant = Color(0xFFE9EEF6),
    onSurfaceVariant = Color(0xFF667085)
)

@Composable
fun LibreDisplayTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
