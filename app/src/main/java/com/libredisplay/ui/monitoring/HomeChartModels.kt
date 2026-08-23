package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToLong

internal enum class HomeChartRange(val duration: Duration, val shortLabel: String, val accessibilityLabel: String) {
    LAST_1_HOUR(Duration.ofHours(1), "1g", "1 godzina"),
    LAST_3_HOURS(Duration.ofHours(3), "3g", "3 godziny"),
    LAST_6_HOURS(Duration.ofHours(6), "6g", "6 godzin"),
    LAST_9_HOURS(Duration.ofHours(9), "9g", "9 godzin"),
    LAST_12_HOURS(Duration.ofHours(12), "12g", "12 godzin"),
    LAST_24_HOURS(Duration.ofHours(24), "24g", "24 godziny"),
    LAST_3_DAYS(Duration.ofDays(3), "3d", "3 dni"),
    LAST_7_DAYS(Duration.ofDays(7), "7d", "7 dni"),
    LAST_14_DAYS(Duration.ofDays(14), "14d", "14 dni"),
    LAST_1_MONTH(Duration.ofDays(30), "1m", "1 miesiąc"),
    LAST_3_MONTHS(Duration.ofDays(90), "3m", "3 miesiące"),
    LAST_6_MONTHS(Duration.ofDays(182), "6m", "6 miesięcy"),
    LAST_12_MONTHS(Duration.ofDays(365), "12m", "12 miesięcy");

    companion object {
        val default = LAST_12_HOURS
    }
}

/**
 * One entry of the range selector.
 *
 * [enabled] is false for the single "next" range offered as a preview: the user can see that more
 * history will become available, but cannot select it yet.
 */
internal data class HomeChartRangeOption(
    val range: HomeChartRange,
    val enabled: Boolean
)

/**
 * Builds the visible range chips: every range that already fits into the collected data plus
 * exactly ONE additional (greyed out) range.
 */
internal fun homeChartRangeOptions(dataSpan: Duration): List<HomeChartRangeOption> {
    val ranges = homeChartRanges()
    val span = if (dataSpan.isNegative) Duration.ZERO else dataSpan
    val enabledCount = ranges.count { it.duration <= span }.coerceAtLeast(1)
    val visibleCount = (enabledCount + 1).coerceAtMost(ranges.size)
    return ranges.take(visibleCount).mapIndexed { index, range ->
        HomeChartRangeOption(range = range, enabled = index < enabledCount)
    }
}

/** Largest range that can actually be displayed with the data currently stored. */
internal fun largestSelectableHomeChartRange(dataSpan: Duration): HomeChartRange =
    homeChartRangeOptions(dataSpan).lastOrNull { it.enabled }?.range ?: HomeChartRange.LAST_1_HOUR

/** X axis grid interval that keeps the labels readable for the given range. */
internal fun homeChartAxisTickInterval(range: HomeChartRange): Duration = when (range) {
    HomeChartRange.LAST_1_HOUR -> Duration.ofMinutes(10)
    HomeChartRange.LAST_3_HOURS -> Duration.ofMinutes(30)
    HomeChartRange.LAST_6_HOURS -> Duration.ofHours(1)
    HomeChartRange.LAST_9_HOURS -> Duration.ofMinutes(90)
    HomeChartRange.LAST_12_HOURS -> Duration.ofHours(2)
    HomeChartRange.LAST_24_HOURS -> Duration.ofHours(4)
    HomeChartRange.LAST_3_DAYS -> Duration.ofHours(12)
    HomeChartRange.LAST_7_DAYS -> Duration.ofDays(1)
    HomeChartRange.LAST_14_DAYS -> Duration.ofDays(2)
    HomeChartRange.LAST_1_MONTH -> Duration.ofDays(5)
    HomeChartRange.LAST_3_MONTHS -> Duration.ofDays(15)
    HomeChartRange.LAST_6_MONTHS -> Duration.ofDays(30)
    HomeChartRange.LAST_12_MONTHS -> Duration.ofDays(60)
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

internal data class HomeNavigatorGeometry(
    val trackLeft: Float,
    val trackWidth: Float,
    val viewportLeft: Float,
    val viewportWidth: Float
)

internal fun homeChartRanges(): List<HomeChartRange> = HomeChartRange.entries

internal fun homeChartAvailablePoints(
    points: List<GlucoseHistoryPoint>,
    now: Instant = points.maxOfOrNull { it.timestamp } ?: Instant.now(),
    maxWindow: Duration = Duration.ofHours(24)
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
    maxWindow: Duration = Duration.ofHours(24)
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

internal fun computeHomeNavigatorGeometry(
    totalWidthPx: Float,
    leftInsetPx: Float,
    rightInsetPx: Float,
    viewportFraction: Float,
    windowFraction: Float
): HomeNavigatorGeometry {
    val safeWidth = totalWidthPx.coerceAtLeast(1f)
    val trackLeft = leftInsetPx.coerceAtLeast(0f).coerceAtMost(safeWidth)
    val trackRight = (safeWidth - rightInsetPx.coerceAtLeast(0f)).coerceAtLeast(trackLeft + 1f)
    val trackWidth = (trackRight - trackLeft).coerceAtLeast(1f)
    val clampedWindowFraction = windowFraction.coerceIn(0f, 1f)
    val viewportWidth = (trackWidth * clampedWindowFraction).coerceIn(1f, trackWidth)
    val maxViewportLeft = trackRight - viewportWidth
    val viewportLeft = trackLeft + (maxViewportLeft - trackLeft) * viewportFraction.coerceIn(0f, 1f)
    return HomeNavigatorGeometry(
        trackLeft = trackLeft,
        trackWidth = trackWidth,
        viewportLeft = viewportLeft,
        viewportWidth = viewportWidth
    )
}

/**
 * Distance the navigator thumb can actually travel.
 *
 * The thumb - not the track - is what the finger drags, therefore a 1:1 finger movement must be
 * mapped onto (trackWidth - thumbWidth). Using the full track width made a single swipe move the
 * viewport only by a few pixels, which was the reported bug.
 */
internal fun navigatorPannableWidth(geometry: HomeNavigatorGeometry): Float =
    (geometry.trackWidth - geometry.viewportWidth).coerceAtLeast(1f)

/** Fraction after dragging the thumb by [dragDeltaPx] pixels. */
internal fun navigatorFractionAfterDrag(
    currentFraction: Float,
    dragDeltaPx: Float,
    geometry: HomeNavigatorGeometry
): Float {
    if (!dragDeltaPx.isFinite()) return currentFraction.coerceIn(0f, 1f)
    return (currentFraction + dragDeltaPx / navigatorPannableWidth(geometry)).coerceIn(0f, 1f)
}

/** Fraction for a direct tap on the navigator track (jump to position). */
internal fun navigatorFractionForPosition(
    touchXPx: Float,
    geometry: HomeNavigatorGeometry
): Float {
    if (!touchXPx.isFinite()) return 0f
    val desiredLeft = touchXPx - geometry.viewportWidth / 2f - geometry.trackLeft
    return (desiredLeft / navigatorPannableWidth(geometry)).coerceIn(0f, 1f)
}

internal fun buildHomeCoverageSummary(
    points: List<GlucoseHistoryPoint>,
    now: Instant = points.maxOfOrNull { it.timestamp } ?: Instant.now()
): HomeCoverageSummary {
    val items = listOf(
        Duration.ofHours(12) to "12g",
        Duration.ofHours(24) to "24g"
    ).map { (window, label) ->
        buildHomeCoverageItem(points, now, window, label)
    }
    return HomeCoverageSummary(
        title = "Dane",
        items = items,
        usesCompleteness = true
    )
}

internal fun homeDatabaseSpanLabel(
    points: List<GlucoseHistoryPoint>,
    now: Instant = points.maxOfOrNull { it.timestamp } ?: Instant.now()
): String {
    if (points.size < 2) return "brak"
    val oldest = points.minOf { it.timestamp }
    val newest = points.maxOf { it.timestamp }
    val span = Duration.between(oldest, newest).coerceAtLeast(Duration.ZERO)
    val anchor = if (now.isBefore(oldest)) newest else now
    val anchoredSpan = Duration.between(oldest, anchor).coerceAtLeast(span)
    return PolishDateTimeFormatter.formatCompactDuration(anchoredSpan)
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
        return HomeCoverageItem(label = label, statusLabel = "brak", isFull = false)
    }

    val span = Duration.between(filtered.first().timestamp, filtered.last().timestamp).coerceAtLeast(Duration.ZERO)
    val activity = sensorActivityFromHistory(filtered, now.minus(window), now)?.activityPercent

    if (activity == null) {
        return HomeCoverageItem(label = label, statusLabel = PolishDateTimeFormatter.formatCompactDuration(span), isFull = false)
    }

    val covered = Duration.ofMillis((window.toMillis() * (activity / 100.0)).roundToLong()).coerceAtMost(window)
    val reachesStart = !filtered.first().timestamp.isAfter(now.minus(window).plus(Duration.ofMinutes(20)))
    val isFull = reachesStart && activity >= 98.0 && span >= window.minus(Duration.ofMinutes(20))
    return HomeCoverageItem(
        label = label,
        statusLabel = if (isFull) "✓" else PolishDateTimeFormatter.formatCompactDuration(covered),
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

/**
 * Human readable description of how much history LibreCare already stores.
 * Replaces the previous cryptic "Dane: 12g · 24g 16g 54m" line.
 */
internal data class HomeDataSummary(
    val storedSpan: Duration,
    val storedSpanLabel: String,
    val nextRangeLabel: String?,
    val missingForNextRangeLabel: String?
) {
    val primaryText: String get() = "Zapisana historia: $storedSpanLabel"

    val secondaryText: String?
        get() = if (nextRangeLabel != null && missingForNextRangeLabel != null) {
            "Zakres $nextRangeLabel będzie dostępny za ok. $missingForNextRangeLabel"
        } else {
            null
        }
}

internal fun homeChartDataSpan(points: List<GlucoseHistoryPoint>): Duration {
    if (points.size < 2) return Duration.ZERO
    val oldest = points.minOf { it.timestamp }
    val newest = points.maxOf { it.timestamp }
    return Duration.between(oldest, newest).coerceAtLeast(Duration.ZERO)
}

internal fun buildHomeDataSummary(points: List<GlucoseHistoryPoint>): HomeDataSummary {
    val span = homeChartDataSpan(points)
    val options = homeChartRangeOptions(span)
    val pending = options.lastOrNull { !it.enabled }?.range
    val missing = pending?.let { (it.duration - span).coerceAtLeast(Duration.ZERO) }
    return HomeDataSummary(
        storedSpan = span,
        storedSpanLabel = if (span.isZero) "brak" else PolishDateTimeFormatter.formatCompactDuration(span),
        nextRangeLabel = pending?.shortLabel,
        missingForNextRangeLabel = missing
            ?.takeIf { !it.isZero }
            ?.let { PolishDateTimeFormatter.formatCompactDuration(it) }
    )
}

