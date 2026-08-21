package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class HomeChartModelsTest {

    @Test
    fun homeChartAvailablePoints_limitsTimelineToLast24Hours() {
        val now = Instant.parse("2026-08-21T12:00:00Z")
        val points = samplePoints(now = now, hours = 24)

        val available = homeChartAvailablePoints(points, now = now)

        assertTrue(available.all { !it.timestamp.isBefore(now.minus(Duration.ofHours(24))) })
        assertEquals(now, available.last().timestamp)
    }

    @Test
    fun buildHomeChartViewport_defaultsToLatestSelectedWindow() {
        val now = Instant.parse("2026-08-21T12:00:00Z")
        val viewport = buildHomeChartViewport(samplePoints(now, 12), HomeChartRange.LAST_3_HOURS)!!

        assertEquals(Duration.ofHours(3), viewport.effectiveDuration)
        assertEquals(now, viewport.visibleEnd)
        assertEquals(now.minus(Duration.ofHours(3)), viewport.visibleStart)
        assertTrue(viewport.canPan)
    }

    @Test
    fun buildHomeChartViewport_whenDataShorterThanSelection_clampsToAvailable() {
        val now = Instant.parse("2026-08-21T12:00:00Z")
        val viewport = buildHomeChartViewport(samplePoints(now, 2), HomeChartRange.LAST_12_HOURS)!!

        assertTrue(viewport.effectiveDuration <= Duration.ofHours(2))
    }

    @Test
    fun viewportFraction_roundTripsWithNavigatorFraction() {
        val now = Instant.parse("2026-08-21T12:00:00Z")
        val base = buildHomeChartViewport(samplePoints(now, 12), HomeChartRange.LAST_3_HOURS)!!
        val shifted = viewportFromFraction(base, 0.5f)

        assertTrue(viewportFraction(shifted) in 0.45f..0.55f)
    }

    @Test
    fun buildHomeCoverageSummary_reports12And24HoursSeparately() {
        val now = Instant.parse("2026-08-21T12:00:00Z")
        val summary = buildHomeCoverageSummary(samplePoints(now, 24), now)

        assertEquals(listOf("12h", "3d", "7d", "30d"), summary.items.map { it.label })
        assertFalse(summary.items.any { it.statusLabel.isBlank() })
    }

    @Test
    fun homeDatabaseSpanLabel_returnsCompactRangeForStoredHistory() {
        val now = Instant.parse("2026-08-21T12:00:00Z")
        val label = homeDatabaseSpanLabel(samplePoints(now, 24 * 7), now)

        assertTrue(label.contains("7d") || label.contains("6d"))
    }

    @Test
    fun computeHomeNavigatorGeometry_forSixHours_usesHalfTrackAndEndsAtRightEdge() {
        val geometry = computeHomeNavigatorGeometry(
            totalWidthPx = 300f,
            leftInsetPx = 48f,
            rightInsetPx = 16f,
            viewportFraction = 1f,
            windowFraction = 0.5f
        )

        assertEquals(48f, geometry.trackLeft)
        assertEquals(236f, geometry.trackWidth)
        assertEquals(118f, geometry.viewportWidth)
        assertEquals(166f, geometry.viewportLeft)
        assertEquals(284f, geometry.viewportLeft + geometry.viewportWidth)
    }

    @Test
    fun snapHomeChartRange_returnsNearestOfficialViewport() {
        assertEquals(HomeChartRange.LAST_1_HOUR, snapHomeChartRange(Duration.ofMinutes(80)))
        assertEquals(HomeChartRange.LAST_3_HOURS, snapHomeChartRange(Duration.ofHours(4)))
        assertEquals(HomeChartRange.LAST_9_HOURS, snapHomeChartRange(Duration.ofHours(8)))
        assertEquals(HomeChartRange.LAST_12_HOURS, snapHomeChartRange(Duration.ofHours(11)))
    }

    private fun samplePoints(now: Instant, hours: Int): List<GlucoseHistoryPoint> {
        val total = hours * 4
        return (0 until total).map { index ->
            GlucoseHistoryPoint(
                value = 100 + (index % 30),
                timestamp = now.minus(Duration.ofMinutes(((total - 1 - index) * 15L))),
                trend = GlucoseTrend.FLAT
            )
        }
    }
}


