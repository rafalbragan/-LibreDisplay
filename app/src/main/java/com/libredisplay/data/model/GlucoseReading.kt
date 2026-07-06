package com.libredisplay.data.model

import java.time.Instant

data class GlucoseReading(
    val value: Int,
    val timestamp: Instant,
    val trend: GlucoseTrend,
    val history: List<GlucoseHistoryPoint> = emptyList(),
    val stats: GlucoseHistoryStats = GlucoseHistoryStats(),
    val historyHoursAvailable: Double = 0.0,
    val trendApiCode: Int? = null,
    val trendValidationWarning: String? = null
) {
    val isLow: Boolean get() = value < LOW_THRESHOLD
    val isHigh: Boolean get() = value > HIGH_THRESHOLD
    val sourceHistoryPointCount: Int get() = history.size

    companion object {
        const val LOW_THRESHOLD = 70
        const val HIGH_THRESHOLD = 180

        fun of(
            value: Int,
            timestamp: Instant,
            trend: GlucoseTrend,
            history: List<GlucoseHistoryPoint> = emptyList(),
            stats: GlucoseHistoryStats = GlucoseHistoryStats(),
            trendApiCode: Int? = null,
            trendValidationWarning: String? = null,
            historyHoursAvailable: Double = 0.0,
            sourceHistoryPointCount: Int = history.size
        ): GlucoseReading = GlucoseReading(
            value = value,
            timestamp = timestamp,
            trend = trend,
            history = history,
            stats = if (stats == GlucoseHistoryStats() && history.isNotEmpty()) {
                GlucoseHistoryStats.from(history, 80, 180)
            } else {
                stats
            },
            historyHoursAvailable = if (historyHoursAvailable > 0.0) historyHoursAvailable else defaultHours(history, sourceHistoryPointCount),
            trendApiCode = trendApiCode,
            trendValidationWarning = trendValidationWarning
        )

        private fun defaultHours(history: List<GlucoseHistoryPoint>, sourceHistoryPointCount: Int): Double {
            if (history.size < 2 && sourceHistoryPointCount < 2) return 0.0
            val points = if (history.size >= 2) history else history.take(sourceHistoryPointCount)
            if (points.size < 2) return 0.0
            val seconds = points.last().timestamp.epochSecond - points.first().timestamp.epochSecond
            return seconds.coerceAtLeast(0) / 3600.0
        }
    }
}
