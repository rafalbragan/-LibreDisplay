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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libredisplay.data.model.GlucoseHistoryPoint
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        if (points.isEmpty()) return@Canvas

        val sorted = points.sortedBy { it.timestamp }
        val minValue = (sorted.minOf { it.value } - 20).coerceAtLeast(40)
        val maxValue = (sorted.maxOf { it.value } + 20).coerceAtMost(420)
        val minTime = sorted.first().timestamp.toEpochMilli().toFloat()
        val maxTime = sorted.last().timestamp.toEpochMilli().toFloat().coerceAtLeast(minTime + 1f)

        val chartLeft = 64f
        val chartTop = 16f
        val chartRight = size.width - 12f
        val chartBottom = size.height - 32f
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        fun yFor(value: Int): Float {
            val fraction = (value - minValue).toFloat() / (maxValue - minValue).toFloat().coerceAtLeast(1f)
            return chartBottom - (fraction * chartHeight)
        }

        fun xFor(epochMs: Float): Float {
            val fraction = (epochMs - minTime) / (maxTime - minTime)
            return chartLeft + (fraction * chartWidth)
        }

        drawRect(
            color = Color(0x18EF4444),
            topLeft = Offset(chartLeft, chartTop),
            size = Size(chartWidth, (yFor(targetHigh) - chartTop).coerceAtLeast(0f))
        )
        drawRect(
            color = Color(0x184CAF50),
            topLeft = Offset(chartLeft, yFor(targetHigh)),
            size = Size(chartWidth, (yFor(targetLow) - yFor(targetHigh)).coerceAtLeast(0f))
        )
        drawRect(
            color = Color(0x18EF4444),
            topLeft = Offset(chartLeft, yFor(targetLow)),
            size = Size(chartWidth, (chartBottom - yFor(targetLow)).coerceAtLeast(0f))
        )

        val linePath = Path()
        sorted.forEachIndexed { index, point ->
            val x = xFor(point.timestamp.toEpochMilli().toFloat())
            val y = yFor(point.value)
            if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        drawPath(path = linePath, color = Color(0xFF7DD3FC), style = Stroke(width = 5f))

        sorted.forEach { point ->
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset(xFor(point.timestamp.toEpochMilli().toFloat()), yFor(point.value))
            )
        }

        listOf(minValue, targetLow, targetHigh, maxValue).distinct().sorted().forEach { value ->
            val y = yFor(value)
            drawLine(
                color = Color.White.copy(alpha = 0.14f),
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 2f
            )
            drawText(
                textMeasurer = textMeasurer,
                text = AnnotatedString(value.toString()),
                topLeft = Offset(8f, y - 8f),
                style = labelStyle
            )
        }

        val labels = listOf(sorted.first(), sorted[sorted.size / 2], sorted.last())
        labels.forEach { point ->
            val x = xFor(point.timestamp.toEpochMilli().toFloat())
            drawText(
                textMeasurer = textMeasurer,
                text = AnnotatedString(timeFormatter.format(point.timestamp)),
                topLeft = Offset((x - 18f).coerceIn(chartLeft, chartRight - 40f), chartBottom + 4f),
                style = labelStyle
            )
        }

        val latest = sorted.last()
        drawText(
            textMeasurer = textMeasurer,
            text = AnnotatedString("${latest.value} mg/dL"),
            topLeft = Offset(
                (xFor(latest.timestamp.toEpochMilli().toFloat()) - 24f).coerceAtMost(chartRight - 60f),
                (yFor(latest.value) - 24f).coerceAtLeast(chartTop)
            ),
            style = valueStyle
        )
    }
}
