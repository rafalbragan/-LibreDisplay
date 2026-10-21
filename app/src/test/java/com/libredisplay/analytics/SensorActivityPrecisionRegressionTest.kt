package com.libredisplay.analytics

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class SensorActivityPrecisionRegressionTest {
    private val at = Instant.parse("2026-09-01T12:00:00.123456Z")
    private fun point(time: Instant) = GlucoseHistoryPoint(231, time, GlucoseTrend.FLAT)
    private fun activity(start: Instant, end: Instant) =
        GlucoseMetricsCalculator.calculateSensorActivity(listOf(point(start), point(end)), start, end)

    @Test fun staleReadingAndRoomMillisecondCopy_haveNoMeasurableActivity() {
        val cached = Instant.ofEpochMilli(at.toEpochMilli())
        val result = activity(cached, at)
        assertTrue(result.activityPercent.isFinite())
        assertEquals(0.0, result.activityPercent, 0.0)
        assertEquals(Duration.ZERO, result.coveredDuration)
        assertEquals(Duration.between(cached, at), result.missingDuration)
        assertEquals("NO_DATA", result.dataQualityStatus)
    }

    @Test fun positiveNanosecondPeriod_neverProducesNaNOrFullCoverage() {
        val result = activity(at, at.plusNanos(1))
        assertEquals(0.0, result.activityPercent, 0.0)
        assertEquals("NO_DATA", result.dataQualityStatus)
    }

    @Test fun equalAndReversedPeriods_remainNoData() {
        listOf(at, at.minusNanos(1)).forEach { end ->
            val result = activity(at, end)
            assertEquals(0.0, result.activityPercent, 0.0)
            assertEquals("NO_DATA", result.dataQualityStatus)
        }
    }

    @Test fun regularInterval_keepsExistingActivityAndQuality() {
        val result = activity(at, at.plusSeconds(900))
        assertEquals(100.0, result.activityPercent, 0.0)
        assertEquals(Duration.ofMinutes(15), result.coveredDuration)
        assertEquals(Duration.ZERO, result.missingDuration)
        assertEquals(1, result.daysWithData)
        assertEquals("HIGH", result.dataQualityStatus)
    }

    @Test fun absentOrSingleReading_keepsLowQualityForMeasurablePeriod() {
        listOf(emptyList(), listOf(point(at))).forEach { readings ->
            val result = GlucoseMetricsCalculator.calculateSensorActivity(readings, at, at.plusSeconds(900))
            assertEquals(0.0, result.activityPercent, 0.0)
            assertEquals(Duration.ofMinutes(15), result.missingDuration)
            assertEquals("LOW", result.dataQualityStatus)
        }
    }

    @Test fun duplicateTimestamps_haveZeroActivityInMeasurablePeriod() {
        val result = GlucoseMetricsCalculator.calculateSensorActivity(
            listOf(point(at), point(at)), at, at.plusSeconds(900)
        )
        assertEquals(0.0, result.activityPercent, 0.0)
        assertEquals("LOW", result.dataQualityStatus)
    }
}

