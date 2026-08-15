package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class ChartExtremes(
    val minimumReading: GlucoseHistoryPoint?,
    val maximumReading: GlucoseHistoryPoint?
)

fun findNearestReadingIndex(
    touchX: Float,
    chartWidth: Float,
    visibleReadings: List<GlucoseHistoryPoint>
): Int? {
    if (visibleReadings.isEmpty()) return null
    if (visibleReadings.size == 1) return 0

    val clampedX = touchX.coerceIn(0f, chartWidth)
    val ratio = if (chartWidth <= 0f) 0f else (clampedX / chartWidth)
    val index = (ratio * (visibleReadings.lastIndex)).roundToInt()
    return index.coerceIn(0, visibleReadings.lastIndex)
}

fun calculateChartExtremes(readings: List<GlucoseHistoryPoint>): ChartExtremes {
    if (readings.isEmpty()) return ChartExtremes(null, null)
    return ChartExtremes(
        minimumReading = readings.minByOrNull { it.value },
        maximumReading = readings.maxByOrNull { it.value }
    )
}

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

fun formatChartDateTime(point: GlucoseHistoryPoint): Pair<String, String> {
    return dateFormatter.format(point.timestamp) to timeFormatter.format(point.timestamp)
}

