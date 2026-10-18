package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.system.measureTimeMillis

class GlucoseChartPerformanceTest {

    @Test
    fun prepareChartData_handlesEighteenMonthHistoryWithinGuardrail() {
        val now = Instant.parse("2026-10-18T00:00:00Z")
        val points = buildLongHistory(now)

        var prepared: PreparedChartData? = null
        val elapsedMs = measureTimeMillis {
            prepared = prepareChartData(points)
        }

        assertEquals(points.size, prepared!!.points.size)
        assertTrue("prepareChartData should finish within 15s, actual=${elapsedMs}ms", elapsedMs < 15_000)
    }

    private fun buildLongHistory(now: Instant): List<GlucoseHistoryPoint> {
        val totalPoints = 52_560 // 18 months at 15-minute cadence (~365*1.5 days equivalent)
        return (0 until totalPoints).map { index ->
            GlucoseHistoryPoint(
                value = 95 + (index % 90),
                timestamp = now.minusSeconds((totalPoints - index).toLong() * 15L * 60L),
                trend = GlucoseTrend.FLAT
            )
        }
    }
}

