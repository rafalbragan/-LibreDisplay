package com.libredisplay.analytics

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RangeDistributionPrecisionRegressionTest {
    private val at = Instant.parse("2026-09-01T12:00:00.123456Z")
    private fun point(time: Instant) = GlucoseHistoryPoint(231, time, GlucoseTrend.FLAT)
    private fun distribution(points: List<GlucoseHistoryPoint>) =
        GlucoseMetricsCalculator.calculateRangeDistribution(points, 70, 180, 54, 250)

    @Test fun staleReadingAndRoomMillisecondCopy_doNotDivideZeroByZero() {
        val cached = Instant.ofEpochMilli(at.toEpochMilli())
        val result = distribution(listOf(point(cached), point(at)))
        assertEquals(0, result.aboveRangePercent)
        assertEquals(0, result.inRangePercent)
        assertEquals(0, result.belowRangePercent)
    }

    @Test fun duplicateTimestampsAndSinglePoint_haveNoMeasurableCoverage() {
        assertEquals(0, distribution(listOf(point(at), point(at))).aboveRangePercent)
        assertEquals(0, distribution(listOf(point(at))).aboveRangePercent)
    }

    @Test fun regularIntervals_keepExistingDistribution() {
        val result = distribution(listOf(point(at), point(at.plusSeconds(900))))
        assertEquals(100, result.aboveRangePercent)
        assertEquals(Duration.ofMinutes(15), result.aboveRangeDuration)
    }
}
