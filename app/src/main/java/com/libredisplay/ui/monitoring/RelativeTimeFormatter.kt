package com.libredisplay.ui.monitoring

import java.time.Duration
import java.time.Instant

/**
 * Formats time difference as user-facing relative text.
 *
 * Examples:
 * - < 1 min: "chwilę temu"
 * - 1-59 min: "15 min temu"
 * - 1h-24h: "1g 15m temu"
 * - 24h+: "2 dni 3g temu"
 */
internal object RelativeTimeFormatter {

    /**
     * Format a timestamp as relative time ago (e.g., "15 min temu", "1g 15m temu")
     */
    fun formatTimeAgo(
        timestamp: Instant,
        now: Instant = Instant.now()
    ): String {
        val duration = Duration.between(timestamp, now)
        return formatDurationAgo(duration)
    }

    /**
     * Format a duration as time ago
     */
    fun formatDurationAgo(duration: Duration?): String {
        val safeDuration = duration ?: return "brak danych"
        if (safeDuration.isNegative || safeDuration.isZero) return "chwilę temu"

        val totalMinutes = safeDuration.toMinutes()
        if (totalMinutes < 1) return "chwilę temu"
        if (totalMinutes < 60) return "$totalMinutes min temu"

        val totalHours = totalMinutes / 60
        val minutes = totalMinutes % 60
        if (totalHours < 24) {
            return when {
                minutes == 0L -> {
                    when (totalHours) {
                        1L -> "1g temu"
                        else -> "${totalHours}g temu"
                    }
                }
                else -> {
                    when (totalHours) {
                        1L -> "1g ${minutes}m temu"
                        else -> "${totalHours}g ${minutes}m temu"
                    }
                }
            }
        }

        val days = totalHours / 24
        val hours = totalHours % 24
        val dayLabel = when (days) {
            1L -> "1 dzień"
            else -> "$days dni"
        }

        return when {
            hours == 0L -> "$dayLabel temu"
            else -> "$dayLabel ${hours}g temu"
        }
    }
}

