package com.libredisplay.ui.monitoring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.diagnostics.DiagnosticLogger
import com.libredisplay.ui.theme.LibreCareColors
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal data class ChartPointSample(
    val epochMillis: Long,
    val value: Int
)

internal data class PreparedChartData(
    val points: List<ChartPointSample>,
    val minValue: Int,
    val maxValue: Int
)

internal data class NearestHistoryPointMatch(
    val point: GlucoseHistoryPoint,
    val distancePx: Float
)

internal data class ChartArea(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

internal fun calculateChartArea(
    canvasWidth: Float,
    canvasHeight: Float,
    leftPadding: Float = 54f,
    topPadding: Float = 16f,
    rightPadding: Float = 20f,
    bottomPadding: Float = 36f
): ChartArea? {
    if (!canvasWidth.isFinite() || !canvasHeight.isFinite() || canvasWidth <= 0f || canvasHeight <= 0f) {
        return null
    }
    val area = ChartArea(
        left = leftPadding,
        top = topPadding,
        right = canvasWidth - rightPadding,
        bottom = canvasHeight - bottomPadding
    )
    return area.takeIf { it.width > 0f && it.height > 0f }
}

internal fun measureMaxYAxisLabelWidth(
    values: List<Int>,
    textMeasurer: TextMeasurer,
    style: TextStyle
): Float = values.maxOfOrNull { value ->
    textMeasurer.measure(
        text = AnnotatedString(value.toString()),
        style = style,
        softWrap = false,
        maxLines = 1
    ).size.width.toFloat()
} ?: 0f

internal fun adaptiveYAxisPadding(
    minPaddingPx: Float,
    values: List<Int>,
    textMeasurer: TextMeasurer,
    style: TextStyle
): Float = max(minPaddingPx, measureMaxYAxisLabelWidth(values, textMeasurer, style) + 16f)

internal fun clampXLabelLeft(
    preferredLeft: Float,
    labelWidth: Float,
    boundsLeft: Float,
    boundsRight: Float
): Float {
    val maxLeft = (boundsRight - labelWidth).coerceAtLeast(boundsLeft)
    return preferredLeft.coerceIn(boundsLeft, maxLeft)
}

internal fun calculateSafeTextConstraints(maxWidthPx: Float, maxHeightPx: Float): Constraints? {
    val safeMaxWidth = maxOf(0, maxWidthPx.roundToInt())
    val safeMaxHeight = maxOf(0, maxHeightPx.roundToInt())
    if (safeMaxWidth == 0 || safeMaxHeight == 0) {
        return null
    }
    return Constraints(maxWidth = safeMaxWidth, maxHeight = safeMaxHeight)
}

internal fun selectXAxisLabelIndices(pointCount: Int, maxLabels: Int = 5): List<Int> {
    if (pointCount <= 0) return emptyList()
    if (pointCount == 1) return listOf(0)

    val lastIndex = pointCount - 1
    val anchors = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
    return anchors
        .map { (it * lastIndex).roundToInt().coerceIn(0, lastIndex) }
        .distinct()
        .take(maxLabels)
}

internal fun selectXAxisFractions(maxLabels: Int = 5): List<Float> {
    if (maxLabels <= 1) return listOf(0f)
    return (0 until maxLabels).map { index ->
        index.toFloat() / (maxLabels - 1).toFloat()
    }
}

internal fun selectYAxisLabels(
    minY: Int,
    targetLow: Int,
    targetHigh: Int,
    maxY: Int,
    maxLabels: Int = 5
): List<Int> {
    if (maxLabels <= 0) return emptyList()
    if (minY >= maxY) return listOf(minY)

    val span = (maxY - minY).coerceAtLeast(1)
    val step = when {
        span <= 40 -> 10
        span <= 80 -> 20
        span <= 160 -> 40
        else -> 50
    }
    val alignedStart = (minY / step) * step
    val alignedEnd = ((maxY + step - 1) / step) * step

    val labels = mutableSetOf<Int>()
    var value = alignedStart
    while (value <= alignedEnd) {
        if (value in minY..maxY) labels += value
        value += step
    }
    labels += minY
    labels += maxY
    labels += targetLow.coerceIn(minY, maxY)
    labels += targetHigh.coerceIn(minY, maxY)

    return labels
        .toList()
        .sorted()
        .let { sorted ->
            if (sorted.size <= maxLabels) return@let sorted
            val first = sorted.first()
            val last = sorted.last()
            val middle = sorted.filter { it != first && it != last }
            val limitedMiddle = middle
                .chunked((middle.size.toFloat() / (maxLabels - 2).coerceAtLeast(1)).coerceAtLeast(1f).roundToInt())
                .mapNotNull { chunk -> chunk.firstOrNull() }
                .take((maxLabels - 2).coerceAtLeast(0))
            (listOf(first) + limitedMiddle + listOf(last)).distinct().sorted()
        }
}

internal fun isValidChartCoordinate(x: Float, y: Float): Boolean = x.isFinite() && y.isFinite()

internal fun findNearestPointIndexByX(points: List<Offset>, tapX: Float): Int? {
    if (points.isEmpty()) return null
    var bestIndex = 0
    var bestDistance = Float.MAX_VALUE
    points.forEachIndexed { index, point ->
        val distance = abs(point.x - tapX)
        if (distance < bestDistance) {
            bestDistance = distance
            bestIndex = index
        }
    }
    return bestIndex
}

internal fun downsampleHistoryPreservingExtremes(
    points: List<GlucoseHistoryPoint>,
    maxPoints: Int
): List<GlucoseHistoryPoint> {
    if (points.size <= maxPoints || maxPoints < 4) return points.sortedBy { it.timestamp }
    val sorted = points.sortedBy { it.timestamp }
    val bucketCount = (maxPoints / 4).coerceAtLeast(1)
    val bucketSize = ceil(sorted.size.toDouble() / bucketCount.toDouble()).toInt().coerceAtLeast(1)
    val reduced = mutableListOf<GlucoseHistoryPoint>()
    sorted.chunked(bucketSize).forEach { bucket ->
        reduced += listOfNotNull(
            bucket.firstOrNull(),
            bucket.minByOrNull { it.value },
            bucket.maxByOrNull { it.value },
            bucket.lastOrNull()
        )
            .distinctBy { it.timestamp to it.value }
            .sortedBy { it.timestamp }
    }
    return reduced.distinctBy { it.timestamp to it.value }.sortedBy { it.timestamp }.take(maxPoints)
}

internal fun interpolateHistoryPointsForRendering(
    points: List<GlucoseHistoryPoint>,
    stepMinutes: Long = 1,
    maxGapMinutes: Long = 20
): List<GlucoseHistoryPoint> {
    val sorted = points
        .distinctBy { it.timestamp to it.value }
        .sortedBy { it.timestamp }
    if (sorted.size < 2) return sorted

    val safeStepSeconds = (stepMinutes.coerceAtLeast(1L) * 60L)
    val safeMaxGapSeconds = (maxGapMinutes.coerceAtLeast(stepMinutes) * 60L)
    val expanded = mutableListOf<GlucoseHistoryPoint>()

    sorted.zipWithNext().forEach { (current, next) ->
        expanded += current
        val gapSeconds = java.time.Duration.between(current.timestamp, next.timestamp).seconds
        val canInterpolate = gapSeconds > safeStepSeconds && gapSeconds <= safeMaxGapSeconds
        if (!canInterpolate) return@forEach

        val steps = (gapSeconds / safeStepSeconds).toInt()
        if (steps <= 1) return@forEach
        val delta = next.value - current.value
        for (stepIndex in 1 until steps) {
            val fraction = stepIndex.toDouble() / steps.toDouble()
            expanded += GlucoseHistoryPoint(
                value = (current.value + delta * fraction).roundToInt(),
                timestamp = current.timestamp.plusSeconds(stepIndex * safeStepSeconds),
                trend = current.trend
            )
        }
    }

    expanded += sorted.last()
    return expanded.distinctBy { it.timestamp to it.value }.sortedBy { it.timestamp }
}

/**
 * Clips the series to the visible domain and splits it into continuous segments.
 *
 * Guarantees:
 *  - the line always starts at the very left edge and ends at the very right edge of the chart
 *    whenever data exists on both sides of the boundary (a synthetic, linearly interpolated
 *    boundary sample is inserted),
 *  - the line is only broken where the sensor really has no data (gap larger than [maxGapMinutes]).
 */
internal fun clipAndSegmentSeries(
    points: List<GlucoseHistoryPoint>,
    domainStartMillis: Long,
    domainEndMillis: Long,
    maxGapMinutes: Long = 20
): List<List<GlucoseHistoryPoint>> {
    if (points.isEmpty()) return emptyList()
    if (domainEndMillis <= domainStartMillis) return emptyList()

    val sorted = points
        .distinctBy { it.timestamp to it.value }
        .sortedBy { it.timestamp }
    val maxGapSeconds = maxGapMinutes.coerceAtLeast(1L) * 60L

    val inside = sorted.filter { it.timestamp.toEpochMilli() in domainStartMillis..domainEndMillis }
    val clipped = mutableListOf<GlucoseHistoryPoint>()

    val lastBefore = sorted.lastOrNull { it.timestamp.toEpochMilli() < domainStartMillis }
    val firstAfterDomain = sorted.firstOrNull { it.timestamp.toEpochMilli() > domainEndMillis }

    if (inside.isEmpty()) {
        // The whole viewport sits between two samples: draw the interpolated straight line across
        // the chart when the gap is small enough, otherwise draw nothing.
        if (lastBefore == null || firstAfterDomain == null) return emptyList()
        val gapSeconds = Duration.between(lastBefore.timestamp, firstAfterDomain.timestamp).seconds
        if (gapSeconds > maxGapSeconds) return emptyList()
        return listOf(
            listOf(
                interpolateBoundaryPoint(lastBefore, firstAfterDomain, domainStartMillis),
                interpolateBoundaryPoint(lastBefore, firstAfterDomain, domainEndMillis)
            )
        )
    }

    val firstInside = inside.firstOrNull()
    if (lastBefore != null && firstInside != null) {
        val gapSeconds = Duration.between(lastBefore.timestamp, firstInside.timestamp).seconds
        if (gapSeconds in 1..maxGapSeconds) {
            clipped += interpolateBoundaryPoint(lastBefore, firstInside, domainStartMillis)
        }
    }

    clipped += inside

    val lastInside = inside.lastOrNull()
    val firstAfter = firstAfterDomain
    if (lastInside != null && firstAfter != null) {
        val gapSeconds = Duration.between(lastInside.timestamp, firstAfter.timestamp).seconds
        if (gapSeconds in 1..maxGapSeconds) {
            clipped += interpolateBoundaryPoint(lastInside, firstAfter, domainEndMillis)
        }
    }

    if (clipped.isEmpty()) return emptyList()

    val ordered = clipped.distinctBy { it.timestamp.toEpochMilli() }.sortedBy { it.timestamp }
    val segments = mutableListOf<List<GlucoseHistoryPoint>>()
    var current = mutableListOf(ordered.first())
    ordered.zipWithNext().forEach { (previous, next) ->
        val gapSeconds = Duration.between(previous.timestamp, next.timestamp).seconds
        if (gapSeconds > maxGapSeconds) {
            segments += current
            current = mutableListOf(next)
        } else {
            current += next
        }
    }
    segments += current
    return segments.filter { it.isNotEmpty() }
}

private fun interpolateBoundaryPoint(
    from: GlucoseHistoryPoint,
    to: GlucoseHistoryPoint,
    targetMillis: Long
): GlucoseHistoryPoint {
    val fromMillis = from.timestamp.toEpochMilli()
    val toMillis = to.timestamp.toEpochMilli()
    val span = (toMillis - fromMillis).toDouble()
    val fraction = if (span <= 0.0) 0.0 else ((targetMillis - fromMillis).toDouble() / span).coerceIn(0.0, 1.0)
    val value = (from.value + (to.value - from.value) * fraction).roundToInt()
    return GlucoseHistoryPoint(
        value = value,
        timestamp = Instant.ofEpochMilli(targetMillis),
        trend = to.trend
    )
}

internal fun findNearestHistoryPoint(
    points: List<GlucoseHistoryPoint>,
    canvasWidth: Float,
    canvasHeight: Float,
    touchX: Float,
    domainStartMillis: Long? = null,
    domainEndMillis: Long? = null,
    axisLeftPaddingPx: Float = 54f,
    axisTopPaddingPx: Float = 16f,
    axisRightPaddingPx: Float = 20f,
    axisBottomPaddingPx: Float = 36f
): GlucoseHistoryPoint? = findNearestHistoryPointMatch(
    points = points,
    canvasWidth = canvasWidth,
    canvasHeight = canvasHeight,
    touchX = touchX,
    domainStartMillis = domainStartMillis,
    domainEndMillis = domainEndMillis,
    axisLeftPaddingPx = axisLeftPaddingPx,
    axisTopPaddingPx = axisTopPaddingPx,
    axisRightPaddingPx = axisRightPaddingPx,
    axisBottomPaddingPx = axisBottomPaddingPx
)?.point

internal fun findNearestHistoryPointMatch(
    points: List<GlucoseHistoryPoint>,
    canvasWidth: Float,
    canvasHeight: Float,
    touchX: Float,
    domainStartMillis: Long? = null,
    domainEndMillis: Long? = null,
    axisLeftPaddingPx: Float = 54f,
    axisTopPaddingPx: Float = 16f,
    axisRightPaddingPx: Float = 20f,
    axisBottomPaddingPx: Float = 36f
): NearestHistoryPointMatch? {
    val area = calculateChartArea(
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        leftPadding = axisLeftPaddingPx,
        topPadding = axisTopPaddingPx,
        rightPadding = axisRightPaddingPx,
        bottomPadding = axisBottomPaddingPx
    ) ?: return null
    val prepared = prepareChartData(points)
    if (prepared.points.isEmpty()) return null
    val sortedPoints = points.sortedBy { it.timestamp }
    val resolvedDomainStart = domainStartMillis?.takeIf { domainEndMillis != null && domainEndMillis > it }
        ?: prepared.points.first().epochMillis
    val resolvedDomainEnd = domainEndMillis?.takeIf { it > resolvedDomainStart }
        ?: prepared.points.last().epochMillis.coerceAtLeast(resolvedDomainStart + 1L)
    val minTime = resolvedDomainStart.toFloat()
    val maxTime = resolvedDomainEnd.toFloat().coerceAtLeast(minTime + 1f)
    val offsets = prepared.points.map { sample ->
        val fraction = (sample.epochMillis.toFloat() - minTime) / (maxTime - minTime)
        Offset(area.left + fraction * area.width, 0f)
    }
    val nearestIndex = findNearestPointIndexByX(offsets, touchX) ?: return null
    val nearestPoint = sortedPoints.getOrNull(nearestIndex) ?: return null
    val distancePx = abs(offsets[nearestIndex].x - touchX)
    return NearestHistoryPointMatch(point = nearestPoint, distancePx = distancePx)
}

internal fun placeCurrentValueLabel(point: Offset, chartLeft: Float, chartRight: Float, topY: Float): Offset {
    val rightPreferredX = point.x + 10f
    val fallbackLeftX = point.x - 76f
    val x = if (rightPreferredX + 72f <= chartRight) {
        rightPreferredX
    } else {
        fallbackLeftX.coerceAtLeast(chartLeft)
    }
    return Offset(x = x, y = topY)
}

internal fun prepareChartData(points: List<GlucoseHistoryPoint>): PreparedChartData {
    val prepared = points.mapNotNull { point ->
        val epochMillis = runCatching { point.timestamp.toEpochMilli() }
            .getOrElse {
                DiagnosticLogger.logWarning("GlucoseChart", "CHART POINT SKIPPED reason=invalid timestamp")
                return@mapNotNull null
            }
        ChartPointSample(epochMillis = epochMillis, value = point.value)
    }.filter {
        val valid = it.epochMillis.toDouble().isFinite() && it.value.toDouble().isFinite()
        if (!valid) {
            DiagnosticLogger.logWarning("GlucoseChart", "CHART POINT SKIPPED reason=non-finite value")
        }
        valid
    }.sortedBy { it.epochMillis }

    if (prepared.isEmpty()) {
        return PreparedChartData(points = emptyList(), minValue = 0, maxValue = 240)
    }

    val rawMax = prepared.maxOf { it.value }
    // Scale always starts at 0 so the chart shows full proportional context.
    // Do NOT cap maxValue at 420 – CGM readings can exceed that level.
    // Add proportional top margin so labels don't overlap data points.
    val topMargin = if (rawMax > 350) 40 else 25
    val minValue = 0
    val maxValue = rawMax + topMargin

    return PreparedChartData(points = prepared, minValue = minValue, maxValue = maxValue)
}

@Composable
fun GlucoseChart(
    points: List<GlucoseHistoryPoint>,
    targetLow: Int,
    targetHigh: Int,
    xTickInterval: Duration = Duration.ofHours(2),
    zoneId: ZoneId = DateTimeFormatterProvider.deviceZoneId(),
    domainStart: java.time.Instant? = null,
    domainEnd: java.time.Instant? = null,
    selectedPoint: GlucoseHistoryPoint? = null,
    onPointSelected: ((GlucoseHistoryPoint) -> Unit)? = null,
    onPointSelectionCleared: (() -> Unit)? = null,
    onChartTapped: (() -> Unit)? = null,
    chartHeight: Dp = 260.dp,
    maxVisiblePoints: Int = 300,
    maxYAxisLabels: Int = 6,
    maxXAxisLabels: Int = 5,
    axisLeftPaddingPx: Float = 54f,
    axisTopPaddingPx: Float = 16f,
    axisRightPaddingPx: Float = 20f,
    axisBottomPaddingPx: Float = 36f,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val compactAxisLabels = maxYAxisLabels <= 4 || maxXAxisLabels <= 4
    val labelStyle = TextStyle(color = Color(0xFFCBD5E1), fontSize = if (compactAxisLabels) 12.sp else 13.sp)
    val valueStyle = TextStyle(color = Color.White, fontSize = 13.sp)
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }
    val dynamicPointBudget = remember(canvasWidth, maxVisiblePoints) {
        val drawableWidth = (canvasWidth - 80f).roundToInt().coerceAtLeast(160)
        (drawableWidth / 3).coerceIn(80, maxVisiblePoints)
    }
    val interactionPoints = remember(points, dynamicPointBudget) {
        downsampleHistoryPreservingExtremes(points, dynamicPointBudget)
    }
    // Clip to the visible window first so the line always spans the full chart width and is only
    // broken where the sensor actually produced no data.
    val segments = remember(points, domainStart, domainEnd, dynamicPointBudget) {
        val startMillis = domainStart?.toEpochMilli() ?: points.minOfOrNull { it.timestamp.toEpochMilli() }
        val endMillis = domainEnd?.toEpochMilli() ?: points.maxOfOrNull { it.timestamp.toEpochMilli() }
        if (startMillis == null || endMillis == null) {
            emptyList()
        } else {
            clipAndSegmentSeries(
                points = points,
                domainStartMillis = startMillis,
                domainEndMillis = endMillis.coerceAtLeast(startMillis + 1L)
            ).map { segment ->
                interpolateHistoryPointsForRendering(
                    downsampleHistoryPreservingExtremes(segment, dynamicPointBudget)
                )
            }.filter { it.isNotEmpty() }
        }
    }
    val renderedPoints = remember(segments, interactionPoints) {
        if (segments.isEmpty()) {
            interpolateHistoryPointsForRendering(interactionPoints)
        } else {
            segments.flatten().sortedBy { it.timestamp }
        }
    }
    val selectionThresholdPx = 28f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .onSizeChanged {
                canvasWidth = it.width.toFloat()
                canvasHeight = it.height.toFloat()
            }
            .pointerInput(renderedPoints, canvasWidth, canvasHeight, onPointSelected, onChartTapped) {
                detectTapGestures { tapOffset ->
                    val match = findNearestHistoryPointMatch(
                        points = renderedPoints,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        touchX = tapOffset.x,
                        domainStartMillis = domainStart?.toEpochMilli(),
                        domainEndMillis = domainEnd?.toEpochMilli(),
                        axisLeftPaddingPx = axisLeftPaddingPx,
                        axisTopPaddingPx = axisTopPaddingPx,
                        axisRightPaddingPx = axisRightPaddingPx,
                        axisBottomPaddingPx = axisBottomPaddingPx
                    )
                    val selectedReading = match?.point
                    val shouldSelectPoint = when {
                        selectedReading == null || onPointSelected == null -> false
                        onChartTapped == null -> true
                        else -> match.distancePx <= selectionThresholdPx
                    }
                    if (shouldSelectPoint) {
                        val confirmedReading = selectedReading ?: return@detectTapGestures
                        val selectionCallback = onPointSelected ?: return@detectTapGestures
                        DiagnosticLogger.logInfo(
                            "GlucoseChart",
                            "CHART POINT SELECTED timestamp=${confirmedReading.timestamp} value=${confirmedReading.value}"
                        )
                        selectionCallback.invoke(confirmedReading)
                    } else {
                        onChartTapped?.invoke()
                    }
                }
            }
            .pointerInput(renderedPoints, canvasWidth, canvasHeight, onPointSelected) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val selectedReading = findNearestHistoryPoint(
                            points = renderedPoints,
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                            touchX = offset.x,
                            domainStartMillis = domainStart?.toEpochMilli(),
                            domainEndMillis = domainEnd?.toEpochMilli(),
                            axisLeftPaddingPx = axisLeftPaddingPx,
                            axisTopPaddingPx = axisTopPaddingPx,
                            axisRightPaddingPx = axisRightPaddingPx,
                            axisBottomPaddingPx = axisBottomPaddingPx
                        )
                        if (selectedReading != null) {
                            DiagnosticLogger.logInfo(
                                "GlucoseChart",
                                "CHART POINT SELECTED timestamp=${selectedReading.timestamp} value=${selectedReading.value}"
                            )
                            onPointSelected?.invoke(selectedReading)
                        }
                    },
                    onDragEnd = {
                        onPointSelectionCleared?.invoke()
                    },
                    onDragCancel = {
                        onPointSelectionCleared?.invoke()
                    }
                ) { change, _ ->
                    val selectedReading = findNearestHistoryPoint(
                        points = renderedPoints,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        touchX = change.position.x,
                        domainStartMillis = domainStart?.toEpochMilli(),
                        domainEndMillis = domainEnd?.toEpochMilli(),
                        axisLeftPaddingPx = axisLeftPaddingPx,
                        axisTopPaddingPx = axisTopPaddingPx,
                        axisRightPaddingPx = axisRightPaddingPx,
                        axisBottomPaddingPx = axisBottomPaddingPx
                    ) ?: return@detectDragGestures
                    onPointSelected?.invoke(selectedReading)
                    change.consume()
                }
            }
    ) {
        fun drawSafeTextLabel(
            text: String,
            style: TextStyle,
            preferredTopLeft: Offset,
            boundsLeft: Float,
            boundsTop: Float,
            boundsRight: Float,
            boundsBottom: Float,
            textMeasurer: TextMeasurer,
            maxLines: Int = 1,
            softWrap: Boolean = maxLines > 1
        ): Boolean {
            val clampedLeft = boundsLeft.coerceIn(0f, size.width)
            val clampedTop = boundsTop.coerceIn(0f, size.height)
            val clampedRight = boundsRight.coerceIn(clampedLeft, size.width)
            val clampedBottom = boundsBottom.coerceIn(clampedTop, size.height)

            val left = preferredTopLeft.x.coerceIn(clampedLeft, clampedRight)
            val top = preferredTopLeft.y.coerceIn(clampedTop, clampedBottom)

            val constraints = calculateSafeTextConstraints(
                maxWidthPx = clampedRight - left,
                maxHeightPx = clampedBottom - top
            )

            if (constraints == null) {
                DiagnosticLogger.logWarning("GlucoseChart", "CHART LABEL SKIPPED reason=no space")
                return false
            }

            val layout = textMeasurer.measure(
                text = AnnotatedString(text),
                style = style,
                constraints = constraints,
                softWrap = softWrap,
                maxLines = maxLines
            )

            drawText(
                textLayoutResult = layout,
                topLeft = Offset(left, top)
            )
            return true
        }

        DiagnosticLogger.logInfo(
            "GlucoseChart",
            "CHART RENDER START points=${renderedPoints.size} width=${size.width} height=${size.height}"
        )

        if (size.width <= 0f || size.height <= 0f) {
            DiagnosticLogger.logWarning("GlucoseChart", "CHART RENDER SKIPPED reason=zero canvas width=${size.width} height=${size.height}")
            return@Canvas
        }

        val prepared = prepareChartData(renderedPoints)
        if (prepared.points.isEmpty()) {
            drawSafeTextLabel(
                text = "Brak danych historycznych",
                style = valueStyle,
                preferredTopLeft = Offset(16f, size.height / 2f),
                boundsLeft = 8f,
                boundsTop = 8f,
                boundsRight = size.width - 8f,
                boundsBottom = size.height - 8f,
                textMeasurer = textMeasurer
            )
            return@Canvas
        }

        val sorted = prepared.points
        val minValue = prepared.minValue
        val maxValue = prepared.maxValue
        val yScaleMin = if (minValue == maxValue) minValue - 20 else minValue
        val yScaleMax = if (minValue == maxValue) maxValue + 20 else maxValue
        val resolvedDomainStartMillis = domainStart?.toEpochMilli()?.takeIf { domainEnd != null && domainEnd.toEpochMilli() > it }
            ?: sorted.first().epochMillis
        val resolvedDomainEndMillis = domainEnd?.toEpochMilli()?.takeIf { it > resolvedDomainStartMillis }
            ?: sorted.last().epochMillis.coerceAtLeast(resolvedDomainStartMillis + 1L)
        val visibleDuration = java.time.Duration.ofMillis((resolvedDomainEndMillis - resolvedDomainStartMillis).coerceAtLeast(1L))
        val minTime = resolvedDomainStartMillis.toFloat()
        val maxTime = resolvedDomainEndMillis.toFloat().coerceAtLeast(minTime + 1f)

        val yLabels = selectYAxisLabels(yScaleMin, targetLow, targetHigh, yScaleMax, maxLabels = maxYAxisLabels)
        val adaptiveLeftPadding = adaptiveYAxisPadding(axisLeftPaddingPx, yLabels, textMeasurer, labelStyle)
        val area = calculateChartArea(
            canvasWidth = size.width,
            canvasHeight = size.height,
            leftPadding = adaptiveLeftPadding,
            topPadding = axisTopPaddingPx,
            rightPadding = axisRightPaddingPx,
            bottomPadding = axisBottomPaddingPx
        ) ?: run {
            DiagnosticLogger.logWarning("GlucoseChart", "CHART RENDER SKIPPED reason=invalid chart area")
            return@Canvas
        }

        val chartLeft = area.left
        val chartTop = area.top
        val chartRight = area.right
        val chartBottom = area.bottom
        val chartWidth = area.width
        val chartHeight = area.height

        DiagnosticLogger.logInfo(
            "GlucoseChart",
            "CHART AREA width=$chartWidth height=$chartHeight"
        )
        DiagnosticLogger.logInfo("GlucoseChart", "CHART Y SCALE min=$yScaleMin max=$yScaleMax")

        fun yFor(value: Int): Float {
            val fraction = (value - yScaleMin).toFloat() / (yScaleMax - yScaleMin).toFloat().coerceAtLeast(1f)
            return chartBottom - (fraction * chartHeight)
        }

        fun xFor(epochMs: Float): Float {
            val fraction = (epochMs - minTime) / (maxTime - minTime)
            return chartLeft + (fraction * chartWidth)
        }

        val veryLowY = yFor(54).coerceIn(chartTop, chartBottom)
        val lowY = yFor(69).coerceIn(chartTop, chartBottom)
        val inRangeLowY = yFor(targetLow).coerceIn(chartTop, chartBottom)
        val inRangeHighY = yFor(targetHigh).coerceIn(chartTop, chartBottom)
        val highY = yFor(250).coerceIn(chartTop, chartBottom)

        fun drawBand(top: Float, bottom: Float, color: Color) {
            drawRect(
                color = color,
                topLeft = Offset(chartLeft, minOf(top, bottom)),
                size = Size(chartWidth, kotlin.math.abs(bottom - top).coerceAtLeast(0f))
            )
        }

        drawBand(chartTop, highY, LibreCareColors.VeryHighBand.copy(alpha = 0.18f))
        drawBand(highY, inRangeHighY, LibreCareColors.HighBand.copy(alpha = 0.16f))
        drawBand(inRangeHighY, inRangeLowY, LibreCareColors.InRangeBand.copy(alpha = 0.14f))
        drawBand(inRangeLowY, lowY, LibreCareColors.LowBand.copy(alpha = 0.16f))
        drawBand(lowY, chartBottom, LibreCareColors.VeryLowBand.copy(alpha = 0.18f))

        val plottedPoints = sorted.mapNotNull { point ->
            val x = xFor(point.epochMillis.toFloat())
            val y = yFor(point.value)
            if (!isValidChartCoordinate(x, y)) {
                DiagnosticLogger.logWarning("GlucoseChart", "CHART POINT SKIPPED reason=invalid coordinate")
                return@mapNotNull null
            }
            point to Offset(x, y)
        }

        if (plottedPoints.size > 1) {
            // One path per continuous segment: gaps in the sensor data stay visible as gaps,
            // everything else is drawn as an unbroken line from edge to edge.
            val drawableSegments = if (segments.isEmpty()) listOf(renderedPoints) else segments
            drawableSegments.forEach { segment ->
                val offsets = segment.mapNotNull { point ->
                    val x = xFor(point.timestamp.toEpochMilli().toFloat())
                    val y = yFor(point.value)
                    if (isValidChartCoordinate(x, y)) Offset(x, y) else null
                }
                if (offsets.size < 2) return@forEach
                val segmentPath = Path()
                offsets.forEachIndexed { index, position ->
                    if (index == 0) segmentPath.moveTo(position.x, position.y) else segmentPath.lineTo(position.x, position.y)
                }
                drawPath(path = segmentPath, color = Color(0xFF55C8F2), style = Stroke(width = 3f))
            }
        }

        if (plottedPoints.size == 1) {
            DiagnosticLogger.logInfo("GlucoseChart", "CHART SINGLE POINT mode=true")
        }

        // Marker remains only for selected point to keep the chart compact and readable.

        DiagnosticLogger.logInfo("GlucoseChart", "CHART Y AXIS labels=$yLabels")
        var renderedYLabels = 0
        yLabels.forEach { value ->
            val y = yFor(value).coerceIn(chartTop, chartBottom)
            drawLine(
                color = Color.White.copy(alpha = 0.14f),
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 1.5f
            )
            val rendered = drawSafeTextLabel(
                text = value.toString(),
                style = labelStyle,
                preferredTopLeft = Offset(8f, y - 9f),
                boundsLeft = 0f,
                boundsTop = chartTop,
                boundsRight = chartLeft - 8f,
                boundsBottom = chartBottom,
                textMeasurer = textMeasurer
            )
            if (rendered) renderedYLabels++
        }
        DiagnosticLogger.logInfo("GlucoseChart", "CHART Y LABELS count=$renderedYLabels")

        val gridIntervalMillis = xTickInterval.toMillis().coerceAtLeast(60_000L)
        val xTicks = buildList {
            add(resolvedDomainStartMillis)
            var tick = ((resolvedDomainStartMillis / gridIntervalMillis) + 1) * gridIntervalMillis
            while (tick < resolvedDomainEndMillis) {
                add(tick)
                tick += gridIntervalMillis
            }
            if (resolvedDomainEndMillis != resolvedDomainStartMillis) add(resolvedDomainEndMillis)
        }.distinct().sorted()

        xTicks.forEach { tickMillis ->
            val gridFraction = (tickMillis.toFloat() - minTime) / (maxTime - minTime)
            if (gridFraction !in 0f..1f) return@forEach
            val gridX = chartLeft + gridFraction * chartWidth
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(gridX, chartTop),
                end = Offset(gridX, chartBottom),
                strokeWidth = 1f
            )
        }

        DiagnosticLogger.logInfo("GlucoseChart", "CHART X AXIS labels=$xTicks")
        var renderedXLabels = 0
        var previousTickDate: java.time.LocalDate? = null
        val crossesMidnight = Instant.ofEpochMilli(resolvedDomainStartMillis).atZone(zoneId).toLocalDate() !=
            Instant.ofEpochMilli(resolvedDomainEndMillis).atZone(zoneId).toLocalDate()
        xTicks.forEachIndexed { index, labelEpochMillis ->
            val fraction = (labelEpochMillis.toFloat() - minTime) / (maxTime - minTime)
            val labelX = chartLeft + fraction * chartWidth
            val instant = Instant.ofEpochMilli(labelEpochMillis)
            val localDate = instant.atZone(zoneId).toLocalDate()
            val showDate = visibleDuration >= Duration.ofHours(24) && crossesMidnight && (index == 0 || previousTickDate != localDate)
            val labelText = if (showDate) {
                "${DateTimeFormatterProvider.compactDateFormatter().withZone(zoneId).format(instant)}\n${PolishDateTimeFormatter.formatTime(instant, zoneId)}"
            } else {
                PolishDateTimeFormatter.formatTime(instant, zoneId)
            }
            previousTickDate = localDate
            val xLayout = textMeasurer.measure(
                text = AnnotatedString(labelText),
                style = labelStyle,
                softWrap = true,
                maxLines = 2
            )
            val rendered = drawSafeTextLabel(
                text = labelText,
                style = labelStyle,
                preferredTopLeft = Offset(
                    clampXLabelLeft(labelX - xLayout.size.width / 2f, xLayout.size.width.toFloat(), chartLeft, chartRight),
                    chartBottom + 10f
                ),
                boundsLeft = chartLeft,
                boundsTop = chartBottom + 4f,
                boundsRight = chartRight,
                boundsBottom = size.height,
                textMeasurer = textMeasurer,
                maxLines = 2
            )
            if (rendered) renderedXLabels++
        }
        DiagnosticLogger.logInfo("GlucoseChart", "CHART X LABELS count=$renderedXLabels")

        drawSafeTextLabel(
            text = "mg/dL",
            style = labelStyle,
            preferredTopLeft = Offset(8f, chartTop - 14f),
            boundsLeft = 0f,
            boundsTop = 0f,
            boundsRight = chartLeft,
            boundsBottom = chartTop,
            textMeasurer = textMeasurer
        )

        val selectedMarker = selectedPoint?.let { selected ->
            plottedPoints.firstOrNull { (point, _) ->
                point.epochMillis == selected.timestamp.toEpochMilli() && point.value == selected.value
            }
        }

        selectedMarker?.let { (selectedChartPoint, selectedOffset) ->
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(selectedOffset.x, chartTop),
                end = Offset(selectedOffset.x, chartBottom),
                strokeWidth = 2f
            )
            drawCircle(color = Color.White, radius = 10f, center = selectedOffset)
            drawCircle(color = Color(0xFF0F172A), radius = 6f, center = selectedOffset)

            val valueText = "${selectedChartPoint.value} mg/dL"
            val timeText = PolishDateTimeFormatter.formatAbsoluteWithSeconds(
                instant = Instant.ofEpochMilli(selectedChartPoint.epochMillis),
                zoneId = zoneId
            )

            val valueLayout = textMeasurer.measure(
                text = AnnotatedString(valueText),
                style = valueStyle,
                softWrap = false,
                maxLines = 1
            )
            val timeLayout = textMeasurer.measure(
                text = AnnotatedString(timeText),
                style = labelStyle,
                softWrap = false,
                maxLines = 1
            )

            val tooltipWidth = maxOf(valueLayout.size.width, timeLayout.size.width).toFloat() + 24f
            val tooltipHeight = (valueLayout.size.height + timeLayout.size.height).toFloat() + 18f
            val preferredX = selectedOffset.x + 12f
            val tooltipX = if (preferredX + tooltipWidth <= size.width - 8f) {
                preferredX
            } else {
                (selectedOffset.x - tooltipWidth - 12f).coerceAtLeast(chartLeft + 4f)
            }
            val tooltipY = (selectedOffset.y - tooltipHeight - 10f)
                .coerceIn(chartTop + 4f, chartBottom - tooltipHeight - 4f)

            drawRoundRect(
                color = Color(0xFF1B2940),
                topLeft = Offset(tooltipX, tooltipY),
                size = Size(tooltipWidth, tooltipHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawText(valueLayout, topLeft = Offset(tooltipX + 12f, tooltipY + 6f))
            drawText(
                timeLayout,
                topLeft = Offset(tooltipX + 12f, tooltipY + 8f + valueLayout.size.height.toFloat())
            )
        }

        DiagnosticLogger.logInfo("GlucoseChart", "CHART RENDER END success=true")
    }
}
