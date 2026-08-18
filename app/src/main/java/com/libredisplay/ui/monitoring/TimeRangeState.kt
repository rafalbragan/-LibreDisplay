package com.libredisplay.ui.monitoring

import java.time.Instant

/**
 * Represents the time range for glucose data display and statistics.
 * Controls what time period is shown on the dashboard and in charts.
 */
data class TimeRangeState(
    val startTimestamp: Instant = Instant.now().minusSeconds(86400), // 24 hours ago
    val endTimestamp: Instant = Instant.now(),
    val presetRange: PresetTimeRange = PresetTimeRange.LAST_24_HOURS,
    val availableStartTimestamp: Instant = Instant.now().minusSeconds(2592000), // 30 days ago default
    val availableEndTimestamp: Instant = Instant.now(),
    val isCustomRange: Boolean = false
) {
    val durationSeconds: Long
        get() = endTimestamp.epochSecond - startTimestamp.epochSecond

    val durationHours: Double
        get() = durationSeconds.toDouble() / 3600.0

    val durationDays: Double
        get() = durationSeconds.toDouble() / 86400.0

    fun rangeLabel(): String {
        return when {
            isCustomRange -> {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    .withZone(java.time.ZoneId.systemDefault())
                val start = formatter.format(startTimestamp)
                val end = formatter.format(endTimestamp)
                "Zakres: $start - $end"
            }
            else -> "Zakres: ${presetRange.displayLabel}"
        }
    }

    companion object {
        fun fromPreset(preset: PresetTimeRange, now: Instant = Instant.now()): TimeRangeState {
            val (start, end) = preset.getRange(now)
            return TimeRangeState(
                startTimestamp = start,
                endTimestamp = end,
                presetRange = preset,
                isCustomRange = false
            )
        }
    }
}

enum class PresetTimeRange(val displayLabel: String, val durationSeconds: Long) {
    LAST_12_HOURS("Ostatnie 12 godzin", 43200),
    LAST_24_HOURS("Ostatnie 24 godziny", 86400),
    LAST_7_DAYS("Ostatnie 7 dni", 604800),
    LAST_14_DAYS("Ostatnie 14 dni", 1209600),
    LAST_30_DAYS("Ostatnie 30 dni", 2592000),
    LAST_90_DAYS("Ostatnie 90 dni", 7776000),
    LAST_12_MONTHS("Ostatnich 12 miesięcy", 31536000);

    fun getRange(now: Instant = Instant.now()): Pair<Instant, Instant> {
        val start = now.minusSeconds(durationSeconds)
        return Pair(start, now)
    }

    companion object {
        fun availableRanges(availableDataStart: Instant, now: Instant): List<PresetTimeRange> {
            val maxDurationSeconds = now.epochSecond - availableDataStart.epochSecond
            return values().filter { it.durationSeconds <= maxDurationSeconds }
        }
    }
}


