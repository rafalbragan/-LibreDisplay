package com.libredisplay.ui.monitoring

import androidx.compose.ui.graphics.Color

/**
 * Central display configuration for the always-on tablet glucose monitor.
 *
 * Keep visual thresholds, trend thresholds and default refresh behaviour here so
 * the UI can be tuned without changing data-fetching or API code.
 */
object DisplaySettings {
    const val DEFAULT_TREND_WINDOW_MINUTES = 3
    val TREND_WINDOW_OPTIONS_MINUTES = listOf(1, 2, 3, 5, 10, 15)

    const val MIN_REFRESH_INTERVAL_SECONDS = 15
    const val DEFAULT_REFRESH_INTERVAL_SECONDS = 60
    const val MAX_REFRESH_INTERVAL_SECONDS = 300

    const val LOW_GLUCOSE = 70
    const val TARGET_LOW = 80
    const val TARGET_HIGH = 180
    const val HIGH_WARNING = 250

    const val FAST_RISE_MGDL_PER_MIN = 3.0
    const val RISE_MGDL_PER_MIN = 1.0
    const val FALL_MGDL_PER_MIN = -1.0
    const val FAST_FALL_MGDL_PER_MIN = -3.0

    val lowColor = Color(0xFFC62828)
    val targetColor = Color(0xFF1B7F3A)
    val warningHighColor = Color(0xFFFF8F00)
    val criticalHighColor = Color(0xFF7F1010)
    val staleColor = Color(0xFF424242)
    val chartSurfaceColor = Color(0xFF0E1721)

    fun glucoseCardColor(value: Int, isStale: Boolean): Color = when {
        isStale -> staleColor
        value < LOW_GLUCOSE -> lowColor
        value <= TARGET_HIGH -> targetColor
        value < HIGH_WARNING -> warningHighColor
        else -> criticalHighColor
    }
}

