package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class EpisodeCountingTest {

    private val now: Instant = Instant.parse("2026-08-24T12:00:00Z")

    @Test
    fun zeroEpisodes_whenAllReadingsStayOutsidePredicate() {
        val points = points(listOf(90, 100, 110, 120, 130))

        assertEquals(0, countEpisodes(points) { it < 54 })
    }

    @Test
    fun oneEpisode_whenVeryLowValuesAreContinuous() {
        val points = points(List(10) { 50 })

        assertEquals(1, countEpisodes(points) { it < 54 })
    }

    @Test
    fun multipleEpisodes_whenContinuousSegmentsAreSeparated() {
        val points = points(listOf(49, 50, 52, 90, 95, 51, 50, 100, 48, 47))

        assertEquals(3, countEpisodes(points) { it < 54 })
    }

    @Test
    fun episodeInterruptedByReturnToNormal_isCountedAgainOnlyAfterAnotherDrop() {
        val points = points(listOf(52, 51, 80, 84, 53, 52, 90, 120))

        assertEquals(2, countEpisodes(points) { it < 54 })
    }

    private fun points(values: List<Int>): List<GlucoseHistoryPoint> = values.mapIndexed { index, value ->
        GlucoseHistoryPoint(
            value = value,
            timestamp = now.plus(Duration.ofMinutes(index.toLong() * 5L)),
            trend = GlucoseTrend.FLAT
        )
    }
}

