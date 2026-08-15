package com.libredisplay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorLowBackground = Color(0xFFB8324A)
val ColorInRangeBackground = Color(0xFF43C59E)
val ColorHighBackground = Color(0xFFF2B84B)
val ColorStaleBackground = Color(0xFF546073)

val ColorOnDark = Color(0xFFF3F6FA)
val ColorSubtitle = Color(0xFFAAB3C2)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64D2C8),
    onPrimary = Color(0xFF0A111F),
    secondary = Color(0xFF7EA5FF),
    onSecondary = Color(0xFF0A111F),
    background = Color(0xFF101318),
    onBackground = Color(0xFFF3F6FA),
    surface = Color(0xFF182033),
    onSurface = Color(0xFFF3F6FA),
    surfaceVariant = Color(0xFF202A3D),
    onSurfaceVariant = Color(0xFFAAB3C2)
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
