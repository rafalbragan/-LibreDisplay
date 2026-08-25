package com.libredisplay.analytics

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class AnalysisChartFactoryTest {

    private val zone: ZoneId = ZoneId.of("Europe/Warsaw")
    private val now: Instant = Instant.parse("2026-08-24T21:00:00Z")

    @Test
    fun dailyWindow_returns14Bars() {
        val window = AnalysisChartFactory.dailyWindow(
            readings = listOf(point("2026-08-24T10:10:00Z", 120), point("2026-08-20T10:10:00Z", 90)),
            now = now, offsetDays = 0, count = 14,
            targetLow = 80, targetHigh = 180, lowCritical = 54, highCritical = 250, zoneId = zone
        )
        assertEquals(14, window.bars.size)
        assertTrue(window.bars.any { it.readingsCount > 0 })
    }

    @Test
    fun dailyWindow_nightOnlyReducesReadings() {
        val readings = listOf(point("2026-08-24T01:10:00Z", 90), point("2026-08-24T12:10:00Z", 220))
        val all = AnalysisChartFactory.dailyWindow(readings, now, 0, 14, 80, 180, 54, 250, false, zone)
        val night = AnalysisChartFactory.dailyWindow(readings, now, 0, 14, 80, 180, 54, 250, true, zone)
        assertTrue(night.bars.sumOf { it.readingsCount } < all.bars.sumOf { it.readingsCount })
    }

    @Test
    fun dailyWindow_offsetShiftsWindowOlder() {
        val readings = listOf(point("2026-07-29T10:10:00Z", 100))
        val current = AnalysisChartFactory.dailyWindow(readings, now, 0, 14, 80, 180, 54, 250, false, zone)
        val older = AnalysisChartFactory.dailyWindow(readings, now, 14, 14, 80, 180, 54, 250, false, zone)
        assertEquals(0, current.bars.sumOf { it.readingsCount })
        assertTrue(older.bars.sumOf { it.readingsCount } > 0)
        assertTrue(older.canScrollNewer)
    }

    @Test
    fun monthlyWindow_returns12Bars() {
        val readings = listOf(point("2026-08-10T10:00:00Z", 120), point("2026-07-10T10:00:00Z", 150))
        val window = AnalysisChartFactory.monthlyWindow(readings, now, 0, 12, 80, 180, 54, 250, false, zone)
        assertEquals(12, window.bars.size)
        assertTrue(window.bars.any { it.readingsCount > 0 })
    }

    @Test
    fun overlayForWindow_buildsAverage() {
        val readings = listOf(point("2026-08-23T05:30:00Z", 100), point("2026-08-24T05:30:00Z", 140))
        val start = Instant.parse("2026-08-22T22:00:00Z")
        val end = Instant.parse("2026-08-25T22:00:00Z")
        val overlay = AnalysisChartFactory.overlayForWindow(readings, start, end, zone, 14)
        assertTrue(overlay.dayLines.isNotEmpty())
        assertTrue(overlay.averageLine.any { it.sampleCount == 2 && it.averageMgDl == 120.0 })
    }

    @Test
    fun maxDailyOffset_reflectsData() {
        val readings = listOf(point("2026-07-15T10:00:00Z", 100)) // ~40 days before now
        val max = AnalysisChartFactory.maxDailyOffset(readings, now, 14, zone)
        assertTrue(max in 20..30)
    }

    private fun point(timestamp: String, value: Int): GlucoseHistoryPoint =
        GlucoseHistoryPoint(value = value, timestamp = Instant.parse(timestamp), trend = GlucoseTrend.FLAT)
}

