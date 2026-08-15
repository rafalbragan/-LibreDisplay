package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GlucoseChartDataTest {

    @Test
    fun prepareChartData_emptyHistory_isSafe() {
        val prepared = prepareChartData(emptyList())

        assertTrue(prepared.points.isEmpty())
        assertTrue(prepared.minValue < prepared.maxValue)
    }

    @Test
    fun prepareChartData_singlePoint_isSafe() {
        val prepared = prepareChartData(
            listOf(
                GlucoseHistoryPoint(
                    value = 145,
                    timestamp = Instant.parse("2026-07-26T22:00:00Z"),
                    trend = GlucoseTrend.FLAT
                )
            )
        )

        assertEquals(1, prepared.points.size)
        assertTrue(prepared.minValue < prepared.maxValue)
    }

    @Test
    fun prepareChartData_flatHistory_addsYAxisMarginWhenMinEqualsMax() {
        val points = listOf(
            GlucoseHistoryPoint(150, Instant.parse("2026-07-26T21:00:00Z"), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(150, Instant.parse("2026-07-26T21:15:00Z"), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(150, Instant.parse("2026-07-26T21:30:00Z"), GlucoseTrend.FLAT)
        )

        val prepared = prepareChartData(points)

        assertEquals(3, prepared.points.size)
        assertTrue(prepared.maxValue - prepared.minValue >= 40)
    }

    @Test
    fun prepareChartData_skipsInvalidTimestampAndKeepsValidPoints() {
        val points = listOf(
            GlucoseHistoryPoint(120, Instant.MAX, GlucoseTrend.FLAT),
            GlucoseHistoryPoint(145, Instant.parse("2026-07-26T22:00:00Z"), GlucoseTrend.FLAT)
        )

        val prepared = prepareChartData(points)

        assertEquals(1, prepared.points.size)
        assertEquals(145, prepared.points.first().value)
    }
}

