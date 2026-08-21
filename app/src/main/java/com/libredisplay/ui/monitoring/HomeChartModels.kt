package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToLong

internal enum class HomeChartRange(val duration: Duration, val shortLabel: String, val accessibilityLabel: String) {
    LAST_1_HOUR(Duration.ofHours(1), "1h", "1 godzina"),
    LAST_3_HOURS(Duration.ofHours(3), "3h", "3 godziny"),
    LAST_6_HOURS(Duration.ofHours(6), "6h", "6 godzin"),
    LAST_9_HOURS(Duration.ofHours(9), "9h", "9 godzin"),
    LAST_12_HOURS(Duration.ofHours(12), "12h", "12 godzin");

    companion object {
        val default = LAST_6_HOURS
    }
}

internal data class HomeChartViewport(
    val availableStart: Instant,
    val availableEnd: Instant,
    val visibleStart: Instant,
    val visibleEnd: Instant,
    val requestedRange: HomeChartRange,
    val effectiveDuration: Duration,
    val canPan: Boolean
) {
    val visibleDuration: Duration get() = Duration.between(visibleStart, visibleEnd)
}

internal data class HomeCoverageItem(
    val label: String,
    val statusLabel: String,
    val isFull: Boolean
)

internal data class HomeCoverageSummary(
    val title: String,
    val items: List<HomeCoverageItem>,
    val usesCompleteness: Boolean
)

internal fun homeChartRanges(): List<HomeChartRange> = HomeChartRange.entries

internal fun homeChartAvailablePoints(
    points: List<GlucoseHistoryPoint>,
    now: Instant = points.maxOfOrNull { it.timestamp } ?: Instant.now(),
    maxWindow: Duration = Duration.ofHours(12)
): List<GlucoseHistoryPoint> {
    if (points.isEmpty()) return emptyList()
    val start = now.minus(maxWindow)
    return points
        .distinctBy { it.timestamp to it.value }
        .sortedBy { it.timestamp }
        .filter { !it.timestamp.isBefore(start) && !it.timestamp.isAfter(now) }
}

internal fun buildHomeChartViewport(
    points: List<GlucoseHistoryPoint>,
    requestedRange: HomeChartRange,
    visibleEndOverride: Instant? = null,
    maxWindow: Duration = Duration.ofHours(12)
): HomeChartViewport? {
    val availablePoints = homeChartAvailablePoints(points, maxWindow = maxWindow)
    if (availablePoints.isEmpty()) return null
    val availableStart = availablePoints.first().timestamp
    val availableEnd = availablePoints.last().timestamp
    val availableDuration = Duration.between(availableStart, availableEnd).coerceAtLeast(Duration.ZERO)
    val effectiveDuration = minDuration(requestedRange.duration, availableDuration.takeIf { !it.isZero } ?: requestedRange.duration)
    val visibleEnd = (visibleEndOverride ?: availableEnd).coerceInInstant(availableStart.plus(effectiveDuration), availableEnd)
    val visibleStart = visibleEnd.minus(effectiveDuration).coerceAtLeastInstant(availableStart)
    return HomeChartViewport(
        availableStart = availableStart,
        availableEnd = availableEnd,
        visibleStart = visibleStart,
        visibleEnd = visibleStart.plus(effectiveDuration).coerceAtMostInstant(availableEnd),
        requestedRange = requestedRange,
        effectiveDuration = effectiveDuration,
        canPan = availableDuration > effectiveDuration
    )
}

internal fun applyHomeChartPan(
    viewport: HomeChartViewport,
    panXPx: Float,
    chartWidthPx: Float
): HomeChartViewport {
    if (!viewport.canPan || !panXPx.isFinite() || chartWidthPx <= 0f) return viewport
    val visibleMillis = viewport.effectiveDuration.toMillis().coerceAtLeast(1L)
    val shiftMillis = (-(panXPx / chartWidthPx) * visibleMillis.toDouble()).roundToLong()
    val targetEnd = viewport.visibleEnd.plusMillis(shiftMillis)
    return buildHomeChartViewportFromEnd(viewport, targetEnd)
}

internal fun applyHomeChartZoom(
    viewport: HomeChartViewport,
    zoomChange: Float
): HomeChartViewport {
    val safeZoom = zoomChange.takeIf { it.isFinite() && it > 0f } ?: return viewport
    val availableDuration = Duration.between(viewport.availableStart, viewport.availableEnd).coerceAtLeast(Duration.ZERO)
    val currentMillis = viewport.effectiveDuration.toMillis().coerceAtLeast(1L)
    val targetMillis = (currentMillis / safeZoom).roundToLong()
        .coerceIn(HomeChartRange.LAST_1_HOUR.duration.toMillis(), availableDuration.toMillis().coerceAtLeast(currentMillis))
    val snappedEnd = viewport.visibleStart.plusMillis(currentMillis / 2L + targetMillis / 2L)
    val snappedRange = nearestHomeChartRange(Duration.ofMillis(targetMillis))
    return buildHomeChartViewport(
        points = listOf(
            GlucoseHistoryPoint(0, viewport.availableStart, com.libredisplay.data.model.GlucoseTrend.UNKNOWN),
            GlucoseHistoryPoint(0, viewport.availableEnd, com.libredisplay.data.model.GlucoseTrend.UNKNOWN)
        ),
        requestedRange = snappedRange,
        visibleEndOverride = snappedEnd,
        maxWindow = Duration.between(viewport.availableStart, viewport.availableEnd)
    ) ?: viewport
}

internal fun snapHomeChartRange(duration: Duration): HomeChartRange = nearestHomeChartRange(duration)

internal fun nearestHomeChartRange(duration: Duration): HomeChartRange {
    val targetMillis = duration.toMillis()
    return homeChartRanges().minByOrNull { abs(it.duration.toMillis() - targetMillis) } ?: HomeChartRange.default
}

internal fun viewportFraction(viewport: HomeChartViewport): Float {
    val totalMillis = Duration.between(viewport.availableStart, viewport.availableEnd).toMillis().coerceAtLeast(1L)
    val windowMillis = viewport.effectiveDuration.toMillis().coerceAtLeast(1L)
    val maxOffset = (totalMillis - windowMillis).coerceAtLeast(1L)
    val currentOffset = Duration.between(viewport.availableStart, viewport.visibleStart).toMillis().coerceAtLeast(0L)
    return (currentOffset.toFloat() / maxOffset.toFloat()).coerceIn(0f, 1f)
}

internal fun viewportFromFraction(viewport: HomeChartViewport, fraction: Float): HomeChartViewport {
    if (!viewport.canPan) return viewport
    val totalMillis = Duration.between(viewport.availableStart, viewport.availableEnd).toMillis().coerceAtLeast(1L)
    val windowMillis = viewport.effectiveDuration.toMillis().coerceAtLeast(1L)
    val maxOffset = (totalMillis - windowMillis).coerceAtLeast(0L)
    val offset = (maxOffset * fraction.coerceIn(0f, 1f)).roundToLong()
    val targetEnd = viewport.availableStart.plusMillis(offset + windowMillis)
    return buildHomeChartViewportFromEnd(viewport, targetEnd)
}

internal fun buildHomeCoverageSummary(
    points: List<GlucoseHistoryPoint>,
    now: Instant = points.maxOfOrNull { it.timestamp } ?: Instant.now()
): HomeCoverageSummary {
    val items = listOf(Duration.ofHours(12) to "12 h", Duration.ofHours(24) to "24 h").map { (window, label) ->
        buildHomeCoverageItem(points, now, window, label)
    }
    return HomeCoverageSummary(
        title = if (items.any { it.statusLabel.contains("Zakres danych", ignoreCase = true) }) "Zakres danych" else "Dostępność danych",
        items = items,
        usesCompleteness = items.none { it.statusLabel.contains("Zakres danych", ignoreCase = true) }
    )
}

private fun buildHomeCoverageItem(
    points: List<GlucoseHistoryPoint>,
    now: Instant,
    window: Duration,
    label: String
): HomeCoverageItem {
    val filtered = points
        .distinctBy { it.timestamp to it.value }
        .sortedBy { it.timestamp }
        .filter { !it.timestamp.isBefore(now.minus(window)) && !it.timestamp.isAfter(now) }

    if (filtered.size < 2) {
        return HomeCoverageItem(label = label, statusLabel = "Zakres danych", isFull = false)
    }

    val span = Duration.between(filtered.first().timestamp, filtered.last().timestamp).coerceAtLeast(Duration.ZERO)
    val activity = sensorActivityFromHistory(filtered, now.minus(window), now)?.activityPercent

    if (activity == null) {
        return HomeCoverageItem(label = label, statusLabel = "Zakres danych", isFull = false)
    }

    val covered = Duration.ofMillis((window.toMillis() * (activity / 100.0)).roundToLong()).coerceAtMost(window)
    val reachesStart = !filtered.first().timestamp.isAfter(now.minus(window).plus(Duration.ofMinutes(20)))
    val isFull = reachesStart && activity >= 98.0 && span >= window.minus(Duration.ofMinutes(20))
    return HomeCoverageItem(
        label = label,
        statusLabel = if (isFull) "pełne" else PolishDateTimeFormatter.formatCompactDuration(covered),
        isFull = isFull
    )
}

private fun buildHomeChartViewportFromEnd(viewport: HomeChartViewport, targetEnd: Instant): HomeChartViewport {
    val end = targetEnd.coerceInInstant(viewport.availableStart.plus(viewport.effectiveDuration), viewport.availableEnd)
    val start = end.minus(viewport.effectiveDuration).coerceAtLeastInstant(viewport.availableStart)
    return viewport.copy(
        visibleStart = start,
        visibleEnd = start.plus(viewport.effectiveDuration).coerceAtMostInstant(viewport.availableEnd)
    )
}

private fun minDuration(first: Duration, second: Duration): Duration = if (first <= second) first else second

private fun Instant.coerceInInstant(min: Instant, max: Instant): Instant = when {
    this.isBefore(min) -> min
    this.isAfter(max) -> max
    else -> this
}

private fun Instant.coerceAtLeastInstant(min: Instant): Instant = if (this.isBefore(min)) min else this
private fun Instant.coerceAtMostInstant(max: Instant): Instant = if (this.isAfter(max)) max else this

