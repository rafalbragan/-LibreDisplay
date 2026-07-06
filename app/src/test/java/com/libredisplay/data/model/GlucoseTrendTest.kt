package com.libredisplay.data.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class GlucoseTrendTest {

    @Test
    fun fromApiCode_mapsExpectedArrows() {
        assertEquals(GlucoseTrend.FALLING_FAST, GlucoseTrend.fromApiCode(1))
        assertEquals(GlucoseTrend.FALLING, GlucoseTrend.fromApiCode(2))
        assertEquals(GlucoseTrend.FLAT, GlucoseTrend.fromApiCode(3))
        assertEquals(GlucoseTrend.RISING, GlucoseTrend.fromApiCode(4))
        assertEquals(GlucoseTrend.RISING_FAST, GlucoseTrend.fromApiCode(5))
    }

    @Test
    fun stats_computeMinMaxAndTirWithoutAverage() {
        val now = Instant.parse("2026-07-06T10:00:00Z")
        val points = listOf(
            GlucoseHistoryPoint(70, now.minusSeconds(1800), GlucoseTrend.FALLING),
            GlucoseHistoryPoint(120, now.minusSeconds(900), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(200, now, GlucoseTrend.RISING)
        )

        val stats = GlucoseHistoryStats.from(points, 80, 180)

        assertEquals(70, stats.min)
        assertEquals(200, stats.max)
        assertEquals(33, stats.timeInRangePercent)
        assertEquals(33, stats.lowPercent)
        assertEquals(33, stats.highPercent)
    }
}
