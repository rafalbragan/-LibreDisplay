package com.libredisplay.analytics

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class GlucoseMetricsCalculatorStatisticsTest {

    private val now: Instant = Instant.parse("2026-08-24T12:00:00Z")

    @Test
    fun zeroReadings_returnsEmptyStatistics() {
        val minMax = GlucoseMetricsCalculator.calculateMinMax(emptyList())
        val average = GlucoseMetricsCalculator.calculateAverageGlucose(emptyList())
        val range = GlucoseMetricsCalculator.calculateRangeDistribution(emptyList(), 80, 180, 54, 250)

        assertEquals(null to null, minMax)
        assertTrue(average.isNaN())
        assertEquals(0, range.belowRangePercent)
        assertEquals(0, range.inRangePercent)
        assertEquals(0, range.aboveRangePercent)
    }

    @Test
    fun oneReading_returnsExactMinMaxAndNoCoveredDistribution() {
        val reading = listOf(point(110, 0))

        val minMax = GlucoseMetricsCalculator.calculateMinMax(reading)
        val average = GlucoseMetricsCalculator.calculateAverageGlucose(reading)
        val range = GlucoseMetricsCalculator.calculateRangeDistribution(reading, 80, 180, 54, 250)

        assertEquals(110 to 110, minMax)
        assertEquals(110.0, average, 0.0)
        assertEquals(0, range.belowRangePercent + range.inRangePercent + range.aboveRangePercent)
    }

    @Test
    fun allLowReadings_areCountedBelowRange() {
        val readings = listOf(point(60, 0), point(62, 5), point(64, 10), point(66, 15))

        assertDistributionInvariants(readings)
        val range = GlucoseMetricsCalculator.calculateRangeDistribution(readings, 80, 180, 54, 250)

        assertEquals(100, range.belowRangePercent)
        assertEquals(0, range.inRangePercent)
        assertEquals(0, range.aboveRangePercent)
    }

    @Test
    fun allHighReadings_areCountedAboveRange() {
        val readings = listOf(point(210, 0), point(220, 5), point(215, 10), point(205, 15))

        assertDistributionInvariants(readings)
        val range = GlucoseMetricsCalculator.calculateRangeDistribution(readings, 80, 180, 54, 250)

        assertEquals(0, range.belowRangePercent)
        assertEquals(0, range.inRangePercent)
        assertEquals(100, range.aboveRangePercent)
    }

    @Test
    fun mixedReadings_keepMinAverageMaxInvariantAndValidRoundedPercentages() {
        val readings = listOf(
            point(70, 0),
            point(95, 5),
            point(122, 10),
            point(185, 15),
            point(210, 20),
            point(160, 25)
        )

        assertDistributionInvariants(readings)
        val min = GlucoseMetricsCalculator.calculateMinMax(readings).first!!
        val max = GlucoseMetricsCalculator.calculateMinMax(readings).second!!
        val average = GlucoseMetricsCalculator.calculateAverageGlucose(readings)
        val gmi = GlucoseMetricsCalculator.calculateGmi(average)

        assertTrue(min <= average)
        assertTrue(average <= max)
        assertTrue(gmi > 0.0)
    }

    @Test
    fun gapsDoNotArtificiallyInflateCoveragePercentages() {
        val readings = listOf(
            point(90, 0),
            point(110, 5),
            point(205, 60),
            point(195, 65),
            point(100, 120)
        )

        val range = GlucoseMetricsCalculator.calculateRangeDistribution(
            readings = readings,
            targetLow = 80,
            targetHigh = 180,
            lowCritical = 54,
            highCritical = 250,
            maxGap = Duration.ofMinutes(20)
        )

        assertEquals(0, range.belowRangePercent)
        assertTrue(range.inRangeDuration < Duration.ofMinutes(30))
        assertTrue(range.aboveRangeDuration < Duration.ofMinutes(30))
        assertTrue(range.inRangePercent + range.aboveRangePercent in 99..101)
    }

    private fun assertDistributionInvariants(readings: List<GlucoseHistoryPoint>) {
        val min = GlucoseMetricsCalculator.calculateMinMax(readings).first
        val max = GlucoseMetricsCalculator.calculateMinMax(readings).second
        val average = GlucoseMetricsCalculator.calculateAverageGlucose(readings)
        val distribution = GlucoseMetricsCalculator.calculateRangeDistribution(readings, 80, 180, 54, 250)

        if (min != null && max != null && average.isFinite()) {
            assertTrue("min=$min average=$average max=$max", min <= average && average <= max)
        }
        val total = distribution.belowRangePercent + distribution.inRangePercent + distribution.aboveRangePercent
        assertTrue("rounded distribution total=$total", total in 99..101)
    }

    @Test
    fun coefficientOfVariation_returnsNullWhenNotEnoughData() {
        assertEquals(null, GlucoseMetricsCalculator.calculateCoefficientOfVariation(emptyList()))
        assertEquals(null, GlucoseMetricsCalculator.calculateCoefficientOfVariation(listOf(point(120, 0))))
    }

    @Test
    fun coefficientOfVariation_isZeroForFlatReadings() {
        val readings = (0 until 5).map { point(100, it.toLong() * 5) }

        val cv = GlucoseMetricsCalculator.calculateCoefficientOfVariation(readings)

        assertEquals(0.0, cv!!, 0.0001)
    }

    @Test
    fun coefficientOfVariation_matchesKnownStandardDeviation() {
        // values 100 and 140 -> mean 120, population sd 20, CV = 20 / 120 * 100 = 16.666...
        val readings = listOf(point(100, 0), point(140, 5))

        val cv = GlucoseMetricsCalculator.calculateCoefficientOfVariation(readings)

        assertEquals(16.6667, cv!!, 0.001)
    }

    private fun point(value: Int, minuteOffset: Long): GlucoseHistoryPoint =
        GlucoseHistoryPoint(
            value = value,
            timestamp = now.plus(Duration.ofMinutes(minuteOffset)),
            trend = GlucoseTrend.FLAT
        )
}

