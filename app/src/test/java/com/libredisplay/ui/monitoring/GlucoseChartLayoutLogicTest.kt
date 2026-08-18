package com.libredisplay.ui.monitoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.geometry.Offset
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import java.time.Instant

class GlucoseChartLayoutLogicTest {

    @Test
    fun calculateChartArea_verySmallHeight_returnsNull() {
        val area = calculateChartArea(canvasWidth = 120f, canvasHeight = 20f)

        assertEquals(null, area)
    }

    @Test
    fun calculateSafeTextConstraints_negativeHeight_returnsNull() {
        val constraints = calculateSafeTextConstraints(maxWidthPx = 100f, maxHeightPx = -13f)

        assertEquals(null, constraints)
    }

    @Test
    fun calculateSafeTextConstraints_positiveValues_areClampedAndReturned() {
        val constraints = calculateSafeTextConstraints(maxWidthPx = 80.6f, maxHeightPx = 16.2f)

        assertNotNull(constraints)
        assertEquals(81, constraints?.maxWidth)
        assertEquals(16, constraints?.maxHeight)
    }

    @Test
    fun selectXAxisLabelIndices_manyPoints_limitsLabelCount() {
        val indices = selectXAxisLabelIndices(pointCount = 43)

        assertTrue(indices.size in 4..6)
        assertTrue(indices.first() == 0)
        assertTrue(indices.last() == 42)
        assertTrue(indices.size < 43)
    }

    @Test
    fun selectXAxisLabelIndices_singlePoint_returnsOneLabel() {
        val indices = selectXAxisLabelIndices(pointCount = 1)

        assertEquals(listOf(0), indices)
    }

    @Test
    fun selectYAxisLabels_returnsAtMostFiveDistinctValues() {
        val labels = selectYAxisLabels(minY = 80, targetLow = 90, targetHigh = 130, maxY = 210, maxLabels = 6)

        assertTrue(labels.size in 4..6)
        assertEquals(80, labels.first())
        assertEquals(210, labels.last())
    }

    @Test
    fun isValidChartCoordinate_skipsNanAndInfinity() {
        assertFalse(isValidChartCoordinate(Float.NaN, 10f))
        assertFalse(isValidChartCoordinate(10f, Float.POSITIVE_INFINITY))
        assertTrue(isValidChartCoordinate(10f, 20f))
    }

    @Test
    fun findNearestPointIndexByX_selectsClosestPoint() {
        val points = listOf(
            Offset(10f, 0f),
            Offset(30f, 0f),
            Offset(90f, 0f)
        )

        val index = findNearestPointIndexByX(points, tapX = 26f)

        assertEquals(1, index)
    }

    @Test
    fun placeCurrentValueLabel_movesLeftWhenNoRightSpace() {
        val topLeft = placeCurrentValueLabel(
            point = Offset(198f, 40f),
            chartLeft = 56f,
            chartRight = 210f,
            topY = 30f
        )

        assertTrue(topLeft.x < 198f)
        assertTrue(topLeft.x >= 56f)
    }

    @Test
    fun findNearestHistoryPoint_returnsNearestVisiblePoint() {
        val points = listOf(
            GlucoseHistoryPoint(100, Instant.parse("2026-08-18T10:00:00Z"), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(120, Instant.parse("2026-08-18T10:15:00Z"), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(160, Instant.parse("2026-08-18T10:30:00Z"), GlucoseTrend.FLAT)
        )

        val nearest = findNearestHistoryPoint(points, canvasWidth = 300f, canvasHeight = 260f, touchX = 140f)

        assertNotNull(nearest)
        assertEquals(120, nearest?.value)
    }

    @Test
    fun findNearestHistoryPointMatch_reportsPointAndDistance() {
        val points = listOf(
            GlucoseHistoryPoint(100, Instant.parse("2026-08-18T10:00:00Z"), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(120, Instant.parse("2026-08-18T10:15:00Z"), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(160, Instant.parse("2026-08-18T10:30:00Z"), GlucoseTrend.FLAT)
        )

        val match = findNearestHistoryPointMatch(points, canvasWidth = 300f, canvasHeight = 260f, touchX = 140f)

        assertNotNull(match)
        assertEquals(120, match?.point?.value)
        assertTrue((match?.distancePx ?: Float.MAX_VALUE) >= 0f)
    }

    @Test
    fun downsampleHistoryPreservingExtremes_keepsMinAndMax() {
        val points = (0 until 100).map { index ->
            val value = when (index) {
                17 -> 52
                82 -> 320
                else -> 100 + (index % 20)
            }
            GlucoseHistoryPoint(value, Instant.parse("2026-08-18T00:00:00Z").plusSeconds(index * 900L), GlucoseTrend.FLAT)
        }

        val reduced = downsampleHistoryPreservingExtremes(points, maxPoints = 20)

        assertTrue(reduced.size <= 20)
        assertTrue(reduced.any { it.value == 52 })
        assertTrue(reduced.any { it.value == 320 })
    }
}

