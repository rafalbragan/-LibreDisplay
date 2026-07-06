package com.libredisplay.ui.monitoring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.diagnostics.DiagnosticLogger
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    leftPadding: Float = 64f,
    topPadding: Float = 16f,
    rightPadding: Float = 12f,
    bottomPadding: Float = 32f
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
    return listOf(minY, targetLow, targetHigh, maxY)
        .distinct()
        .sorted()
        .take(maxLabels)
}

internal fun isValidChartCoordinate(x: Float, y: Float): Boolean = x.isFinite() && y.isFinite()

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
    val (minValue, maxValue) = if (rawMin == rawMax) {
        ((rawMin - 20).coerceAtLeast(40)) to ((rawMax + 20).coerceAtMost(420))
    } else {
        ((rawMin - 20).coerceAtLeast(40)) to ((rawMax + 20).coerceAtMost(420))
    }

    return PreparedChartData(points = prepared, minValue = minValue, maxValue = maxValue)
}

@Composable
fun GlucoseChart(
    points: List<GlucoseHistoryPoint>,
    targetLow: Int,
    targetHigh: Int,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color(0xFFCBD5E1), fontSize = 11.sp)
    val valueStyle = TextStyle(color = Color.White, fontSize = 12.sp)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
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
                constraints = constraints
            )

            drawText(
                textLayoutResult = layout,
                topLeft = Offset(left, top)
            )
            return true
        }

        DiagnosticLogger.logInfo(
            "GlucoseChart",
            "CHART RENDER START points=${points.size} width=${size.width} height=${size.height}"
        )

        if (size.width <= 0f || size.height <= 0f) {
            DiagnosticLogger.logWarning("GlucoseChart", "CHART RENDER SKIPPED reason=zero canvas width=${size.width} height=${size.height}")
            return@Canvas
        }

        val prepared = prepareChartData(points)
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
        val minTime = sorted.first().epochMillis.toFloat()
        val maxTime = sorted.last().epochMillis.toFloat().coerceAtLeast(minTime + 1f)

        val area = calculateChartArea(size.width, size.height)
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

        val targetHighY = yFor(targetHigh).coerceIn(chartTop, chartBottom)
        val targetLowY = yFor(targetLow).coerceIn(chartTop, chartBottom)
        val targetTop = minOf(targetHighY, targetLowY)
        val targetBottom = maxOf(targetHighY, targetLowY)

        drawRect(
            color = Color(0x18EF4444),
            topLeft = Offset(chartLeft, chartTop),
            size = Size(chartWidth, (targetTop - chartTop).coerceAtLeast(0f))
        )
        drawRect(
            color = Color(0x184CAF50),
            topLeft = Offset(chartLeft, targetTop),
            size = Size(chartWidth, (targetBottom - targetTop).coerceAtLeast(0f))
        )
        drawRect(
            color = Color(0x18EF4444),
            topLeft = Offset(chartLeft, targetBottom),
            size = Size(chartWidth, (chartBottom - targetBottom).coerceAtLeast(0f))
        )

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
            drawPath(path = linePath, color = Color(0xFF7DD3FC), style = Stroke(width = 5f))
        }

        if (plottedPoints.size == 1) {
            DiagnosticLogger.logInfo("GlucoseChart", "CHART SINGLE POINT mode=true")
        }

        plottedPoints.forEach { (_, position) ->
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = position
            )
        }

        val yLabels = selectYAxisLabels(yScaleMin, targetLow, targetHigh, yScaleMax)
        var renderedYLabels = 0
        yLabels.forEach { value ->
            val y = yFor(value).coerceIn(chartTop, chartBottom)
            drawLine(
                color = Color.White.copy(alpha = 0.14f),
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 2f
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

        val xLabelIndices = selectXAxisLabelIndices(plottedPoints.size)
        var renderedXLabels = 0
        xLabelIndices.forEach { index ->
            val (point, position) = plottedPoints[index]
            val rendered = drawSafeTextLabel(
                text = timeFormatter.format(java.time.Instant.ofEpochMilli(point.epochMillis)),
                style = labelStyle,
                preferredTopLeft = Offset(position.x - 18f, chartBottom + 4f),
                boundsLeft = chartLeft,
                boundsTop = chartBottom + 4f,
                boundsRight = chartRight,
                boundsBottom = size.height,
                textMeasurer = textMeasurer
            )
            if (rendered) renderedXLabels++
        }
        DiagnosticLogger.logInfo("GlucoseChart", "CHART X LABELS count=$renderedXLabels")

        plottedPoints.lastOrNull()?.let { (latestPoint, latestOffset) ->
            drawSafeTextLabel(
                text = "${latestPoint.value} mg/dL",
                style = valueStyle,
                preferredTopLeft = Offset(latestOffset.x - 24f, latestOffset.y - 24f),
                boundsLeft = chartLeft,
                boundsTop = chartTop,
                boundsRight = chartRight,
                boundsBottom = chartBottom,
                textMeasurer = textMeasurer
            )
        }

        DiagnosticLogger.logInfo("GlucoseChart", "CHART RENDER END success=true")
    }
}
