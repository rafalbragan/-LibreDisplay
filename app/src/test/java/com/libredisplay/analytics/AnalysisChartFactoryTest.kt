package com.libredisplay.analytics

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class AnalysisChartFactoryTest {

    private val zone: ZoneId = ZoneId.of("Europe/Warsaw")
    private val now: Instant = Instant.parse("2026-08-24T21:00:00Z")

    @Test
    fun weeklyStackedBars_returnsSevenBars() {
        val readings = listOf(
            point("2026-08-24T00:10:00Z", 70),
            point("2026-08-24T04:10:00Z", 120),
            point("2026-08-24T08:10:00Z", 230),
            point("2026-08-23T10:10:00Z", 100)
        )

        val bars = AnalysisChartFactory.weeklyStackedBars(
            readings = readings,
            now = now,
            targetLow = 80,
            targetHigh = 180,
            lowCritical = 54,
            highCritical = 250,
            zoneId = zone
        )

        assertEquals(7, bars.size)
        assertTrue(bars.any { it.readingsCount > 0 })
    }

    @Test
    fun weeklyStackedBars_nightOnlyFiltersOutDayReadings() {
        val readings = listOf(
            point("2026-08-24T01:10:00Z", 90),
            point("2026-08-24T12:10:00Z", 220)
        )

        val allDay = AnalysisChartFactory.weeklyStackedBars(
            readings = readings,
            now = now,
            targetLow = 80,
            targetHigh = 180,
            lowCritical = 54,
            highCritical = 250,
            zoneId = zone,
            nightOnly = false
        )
        val nightOnly = AnalysisChartFactory.weeklyStackedBars(
            readings = readings,
            now = now,
            targetLow = 80,
            targetHigh = 180,
            lowCritical = 54,
            highCritical = 250,
            zoneId = zone,
            nightOnly = true
        )

        val allDayCount = allDay.sumOf { it.readingsCount }
        val nightCount = nightOnly.sumOf { it.readingsCount }
        assertTrue(nightCount < allDayCount)
    }

    @Test
    fun fourteenDayOverlay_buildsAverageMinuteLine() {
        val readings = listOf(
            point("2026-08-20T05:30:00Z", 100),
            point("2026-08-21T05:30:00Z", 140),
            point("2026-08-21T12:10:00Z", 180)
        )

        val overlay = AnalysisChartFactory.fourteenDayOverlay(
            readings = readings,
            now = now,
            zoneId = zone
        )

        assertTrue(overlay.dayLines.isNotEmpty())
        val minute = overlay.averageLine.firstOrNull { it.sampleCount == 2 }
        assertTrue(minute != null)
        assertEquals(120.0, minute!!.averageMgDl, 0.01)
    }

    private fun point(timestamp: String, value: Int): GlucoseHistoryPoint {
        return GlucoseHistoryPoint(
            value = value,
            timestamp = Instant.parse(timestamp).plus(Duration.ZERO),
            trend = GlucoseTrend.FLAT
        )
    }
}

