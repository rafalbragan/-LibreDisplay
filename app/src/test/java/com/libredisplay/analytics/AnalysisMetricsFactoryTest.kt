package com.libredisplay.analytics

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class AnalysisMetricsFactoryTest {

    private val now: Instant = Instant.parse("2026-08-24T12:00:00Z")

    @Test
    fun calculate_returnsEmptyForNoReadings() {
        val metrics = AnalysisMetricsFactory.calculate(
            readings = emptyList(),
            periodStart = now.minus(Duration.ofHours(1)),
            periodEnd = now,
            targetLow = 80,
            targetHigh = 180,
            lowCritical = 54,
            highCritical = 250
        )

        assertEquals(PeriodMetrics.empty, metrics)
    }

    @Test
    fun calculate_countsLowAndHighEpisodesAcrossSegments() {
        val points = listOf(50, 52, 95, 260, 255, 120, 49, 110, 270).mapIndexed { index, value ->
            point(value, index.toLong() * 5)
        }

        val metrics = AnalysisMetricsFactory.calculate(
            readings = points,
            periodStart = now.minus(Duration.ofHours(2)),
            periodEnd = now,
            targetLow = 80,
            targetHigh = 180,
            lowCritical = 54,
            highCritical = 250
        )

        assertEquals(9, metrics.readingsCount)
        assertEquals(2, metrics.veryLowEpisodes)
        assertEquals(2, metrics.veryHighEpisodes)
        assertNotNull(metrics.cvPercent)
        assertNotNull(metrics.gmiPercent)
    }

    private fun point(value: Int, minuteOffset: Long): GlucoseHistoryPoint =
        GlucoseHistoryPoint(
            value = value,
            timestamp = now.minus(Duration.ofMinutes(minuteOffset)),
            trend = GlucoseTrend.FLAT
        )
}

