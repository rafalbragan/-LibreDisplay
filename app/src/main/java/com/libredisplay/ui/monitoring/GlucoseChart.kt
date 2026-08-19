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
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

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
    leftPadding: Float = 72f,
    topPadding: Float = 18f,
    rightPadding: Float = 44f,
    bottomPadding: Float = 44f
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
    val interior = sorted.drop(1).dropLast(1)
    val bucketCount = ((maxPoints - 2) / 2).coerceAtLeast(1)
    val bucketSize = ceil(interior.size.toDouble() / bucketCount.toDouble()).toInt().coerceAtLeast(1)
    val reduced = mutableListOf<GlucoseHistoryPoint>()
    reduced += sorted.first()
    interior.chunked(bucketSize).forEach { bucket ->
        bucket.minByOrNull { it.value }?.let(reduced::add)
        bucket.maxByOrNull { it.value }?.let(reduced::add)
    }
    reduced += sorted.last()
    return reduced.distinctBy { it.timestamp to it.value }.sortedBy { it.timestamp }.take(maxPoints)
}

internal fun findNearestHistoryPoint(
    points: List<GlucoseHistoryPoint>,
    canvasWidth: Float,
    canvasHeight: Float,
    touchX: Float
): GlucoseHistoryPoint? = findNearestHistoryPointMatch(
    points = points,
    canvasWidth = canvasWidth,
    canvasHeight = canvasHeight,
    touchX = touchX
)?.point

internal fun findNearestHistoryPointMatch(
    points: List<GlucoseHistoryPoint>,
    canvasWidth: Float,
    canvasHeight: Float,
    touchX: Float
): NearestHistoryPointMatch? {
    val area = calculateChartArea(
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        leftPadding = 72f,
        topPadding = 18f,
        rightPadding = 44f,
        bottomPadding = 44f
    ) ?: return null
    val prepared = prepareChartData(points)
    if (prepared.points.isEmpty()) return null
    val sortedPoints = points.sortedBy { it.timestamp }
    val minTime = prepared.points.first().epochMillis.toFloat()
    val maxTime = prepared.points.last().epochMillis.toFloat().coerceAtLeast(minTime + 1f)
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
        return PreparedChartData(points = emptyList(), minValue = 40, maxValue = 240)
    }

    val rawMin = prepared.minOf { it.value }
    val rawMax = prepared.maxOf { it.value }
    // Do NOT cap maxValue at 420 – CGM readings can exceed that level.
    // Add proportional top margin so labels don't overlap data points.
    val topMargin = if (rawMax > 350) 40 else 25
    val minValue = (rawMin - 20).coerceAtLeast(40)
    val maxValue = rawMax + topMargin

    return PreparedChartData(points = prepared, minValue = minValue, maxValue = maxValue)
}

@Composable
fun GlucoseChart(
    points: List<GlucoseHistoryPoint>,
    targetLow: Int,
    targetHigh: Int,
    zoneId: ZoneId = DateTimeFormatterProvider.deviceZoneId(),
    selectedPoint: GlucoseHistoryPoint? = null,
    onPointSelected: ((GlucoseHistoryPoint) -> Unit)? = null,
    onPointSelectionCleared: (() -> Unit)? = null,
    onChartTapped: (() -> Unit)? = null,
    chartHeight: Dp = 260.dp,
    maxVisiblePoints: Int = 240,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color(0xFFCBD5E1), fontSize = 12.sp)
    val valueStyle = TextStyle(color = Color.White, fontSize = 13.sp)
    val visiblePoints = remember(points, maxVisiblePoints) { downsampleHistoryPreservingExtremes(points, maxVisiblePoints) }
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }
    val selectionThresholdPx = 28f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .onSizeChanged {
                canvasWidth = it.width.toFloat()
                canvasHeight = it.height.toFloat()
            }
            .pointerInput(visiblePoints, canvasWidth, canvasHeight, onPointSelected, onChartTapped) {
                detectTapGestures { tapOffset ->
                    val match = findNearestHistoryPointMatch(
                        points = visiblePoints,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        touchX = tapOffset.x
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
            .pointerInput(visiblePoints, canvasWidth, canvasHeight, onPointSelected) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val selectedReading = findNearestHistoryPoint(
                            points = visiblePoints,
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                            touchX = offset.x
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
                        points = visiblePoints,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        touchX = change.position.x
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
            textMeasurer: TextMeasurer
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
                softWrap = false,
                maxLines = 1
            )

            drawText(
                textLayoutResult = layout,
                topLeft = Offset(left, top)
            )
            return true
        }

        DiagnosticLogger.logInfo(
            "GlucoseChart",
            "CHART RENDER START points=${visiblePoints.size} width=${size.width} height=${size.height}"
        )

        if (size.width <= 0f || size.height <= 0f) {
            DiagnosticLogger.logWarning("GlucoseChart", "CHART RENDER SKIPPED reason=zero canvas width=${size.width} height=${size.height}")
            return@Canvas
        }

        val prepared = prepareChartData(visiblePoints)
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
        val visibleDuration = java.time.Duration.ofMillis((sorted.last().epochMillis - sorted.first().epochMillis).coerceAtLeast(1L))
        val minTime = sorted.first().epochMillis.toFloat()
        val maxTime = sorted.last().epochMillis.toFloat().coerceAtLeast(minTime + 1f)

        val area = calculateChartArea(
            canvasWidth = size.width,
            canvasHeight = size.height,
            leftPadding = 72f,
            topPadding = 18f,
            rightPadding = 44f,
            bottomPadding = 44f
        )
        if (area == null) {
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
            val linePath = Path()
            plottedPoints.forEachIndexed { index, (_, position) ->
                if (index == 0) linePath.moveTo(position.x, position.y) else linePath.lineTo(position.x, position.y)
            }
            drawPath(path = linePath, color = Color(0xFF55C8F2), style = Stroke(width = 3f))
        }

        if (plottedPoints.size == 1) {
            DiagnosticLogger.logInfo("GlucoseChart", "CHART SINGLE POINT mode=true")
        }

        // Marker remains only for selected point to keep the chart compact and readable.

        val yLabels = selectYAxisLabels(yScaleMin, targetLow, targetHigh, yScaleMax, maxLabels = 6)
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
                preferredTopLeft = Offset(8f, y - 8f),
                boundsLeft = 0f,
                boundsTop = chartTop,
                boundsRight = chartLeft - 8f,
                boundsBottom = chartBottom,
                textMeasurer = textMeasurer
            )
            if (rendered) renderedYLabels++
        }
        DiagnosticLogger.logInfo("GlucoseChart", "CHART Y LABELS count=$renderedYLabels")

        val xLabelIndices = selectXAxisLabelIndices(plottedPoints.size, maxLabels = 5)
        DiagnosticLogger.logInfo("GlucoseChart", "CHART X AXIS labels=$xLabelIndices")
        var renderedXLabels = 0
        xLabelIndices.forEach { index ->
            val (point, position) = plottedPoints[index]
            val rendered = drawSafeTextLabel(
                text = PolishDateTimeFormatter.formatChartAxisLabel(
                    instant = java.time.Instant.ofEpochMilli(point.epochMillis),
                    visibleDuration = visibleDuration,
                    zoneId = zoneId
                ),
                style = labelStyle,
                preferredTopLeft = Offset(position.x - 28f, chartBottom + 10f),
                boundsLeft = chartLeft,
                boundsTop = chartBottom + 4f,
                boundsRight = chartRight,
                boundsBottom = size.height,
                textMeasurer = textMeasurer
            )
            if (rendered) renderedXLabels++
        }
        DiagnosticLogger.logInfo("GlucoseChart", "CHART X LABELS count=$renderedXLabels")

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
            val timeText = PolishDateTimeFormatter.formatTime(
                instant = java.time.Instant.ofEpochMilli(selectedChartPoint.epochMillis),
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
