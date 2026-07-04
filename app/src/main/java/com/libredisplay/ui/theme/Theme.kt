package com.libredisplay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Colour palette ────────────────────────────────────────────────────────────

val ColorLowBackground    = Color(0xFFB71C1C)   // deep red
val ColorInRangeBackground = Color(0xFF1B5E20)  // deep green
val ColorHighBackground   = Color(0xFFE65100)   // deep orange
val ColorStaleBackground  = Color(0xFF424242)   // dark grey

val ColorOnDark = Color(0xFFFFFFFF)
val ColorSubtitle = Color(0xFFE0E0E0)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF000000),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF)
)

@Composable
fun LibreDisplayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

