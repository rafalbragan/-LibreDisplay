package com.libredisplay.ui.monitoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class FullScreenHistoryViewportTest {

    @Test
    fun historySelectableRanges_matchesAcceptedScope_1hTo24h() {
        assertEquals(
            listOf(
                TimeRange.LAST_1_HOUR,
                TimeRange.LAST_3_HOURS,
                TimeRange.LAST_6_HOURS,
                TimeRange.LAST_12_HOURS,
                TimeRange.LAST_24_HOURS
            ),
            historySelectableRanges()
        )
    }

    @Test
    fun minimumViewportDuration_forWeekAndLonger_isOneDay() {
        assertEquals(Duration.ofDays(1), minimumViewportDuration(TimeRange.LAST_7_DAYS))
        assertEquals(Duration.ofDays(1), minimumViewportDuration(TimeRange.LAST_30_DAYS))
    }

    @Test
    fun buildViewport_forOneHourWindowOnLongHistory_allowsPanning() {
        val historyStart = Instant.parse("2026-08-01T00:00:00Z")
        val historyEnd = historyStart.plus(Duration.ofHours(24))

        val viewport = buildViewport(
            selectedRangeStart = historyStart,
            selectedRangeEnd = historyEnd,
            requestedStartMillis = historyEnd.minus(Duration.ofHours(1)).toEpochMilli(),
            requestedDurationMillis = Duration.ofHours(1).toMillis(),
            minimumDuration = Duration.ofHours(1)
        )

        assertEquals(Duration.ofHours(1), viewport.duration)
        assertTrue(viewport.canPan)
    }

    @Test
    fun buildViewport_forOneHourWindowOnShortHistory_disablesPanning() {
        val historyStart = Instant.parse("2026-08-01T00:00:00Z")
        val historyEnd = historyStart.plus(Duration.ofMinutes(40))

        val viewport = buildViewport(
            selectedRangeStart = historyStart,
            selectedRangeEnd = historyEnd,
            requestedStartMillis = historyStart.toEpochMilli(),
            requestedDurationMillis = Duration.ofHours(1).toMillis(),
            minimumDuration = Duration.ofHours(1)
        )

        assertEquals(Duration.ofMinutes(40), viewport.duration)
        assertTrue(!viewport.canPan)
    }

    @Test
    fun buildViewport_clampsToSelectedRange() {
        val start = Instant.parse("2026-08-01T00:00:00Z")
        val end = start.plus(Duration.ofDays(7))

        val viewport = buildViewport(
            selectedRangeStart = start,
            selectedRangeEnd = end,
            requestedStartMillis = end.toEpochMilli(),
            requestedDurationMillis = Duration.ofDays(2).toMillis(),
            minimumDuration = Duration.ofDays(1)
        )

        assertEquals(Duration.ofDays(2), viewport.duration)
        assertEquals(end.minus(Duration.ofDays(2)), viewport.start)
        assertEquals(end, viewport.end)
        assertTrue(viewport.isZoomed)
        assertTrue(viewport.canPan)
    }

    @Test
    fun applyViewportTransform_zoomInAndPan_keepsViewportInsideRange() {
        val start = Instant.parse("2026-08-01T00:00:00Z")
        val end = start.plus(Duration.ofDays(7))

        val updated = applyViewportTransform(
            currentStartMillis = start.toEpochMilli(),
            currentDurationMillis = Duration.ofDays(7).toMillis(),
            selectedRangeStart = start,
            selectedRangeEnd = end,
            zoomChange = 2f,
            panXPx = -120f,
            chartWidthPx = 300f,
            minimumDuration = Duration.ofDays(1)
        )

        assertTrue(updated.second in Duration.ofDays(1).toMillis()..Duration.ofDays(7).toMillis())
        assertTrue(updated.first >= start.toEpochMilli())
        assertTrue(updated.first + updated.second <= end.toEpochMilli())
    }

    @Test
    fun viewportSliderHelpers_roundTripWithinRange() {
        val start = Instant.parse("2026-08-01T00:00:00Z")
        val end = start.plus(Duration.ofDays(7))
        val viewport = buildViewport(
            selectedRangeStart = start,
            selectedRangeEnd = end,
            requestedStartMillis = start.plus(Duration.ofDays(2)).toEpochMilli(),
            requestedDurationMillis = Duration.ofDays(1).toMillis(),
            minimumDuration = Duration.ofDays(1)
        )

        val fraction = viewportScrollFraction(viewport, start, end)
        val restoredStart = viewportStartForFraction(fraction, viewport, start, end)

        assertTrue(kotlin.math.abs(viewport.start.toEpochMilli() - restoredStart) <= 60_000L)
    }

    @Test
    fun isViewportAtLatest_detectsLatestWindowAndOlderWindow() {
        val start = Instant.parse("2026-08-01T00:00:00Z")
        val end = start.plus(Duration.ofHours(24))

        val latestViewport = buildViewport(
            selectedRangeStart = start,
            selectedRangeEnd = end,
            requestedStartMillis = end.minus(Duration.ofHours(6)).toEpochMilli(),
            requestedDurationMillis = Duration.ofHours(6).toMillis(),
            minimumDuration = Duration.ofHours(1)
        )

        val olderViewport = buildViewport(
            selectedRangeStart = start,
            selectedRangeEnd = end,
            requestedStartMillis = start.plus(Duration.ofHours(8)).toEpochMilli(),
            requestedDurationMillis = Duration.ofHours(6).toMillis(),
            minimumDuration = Duration.ofHours(1)
        )

        assertTrue(isViewportAtLatest(latestViewport, start, end))
        assertTrue(!isViewportAtLatest(olderViewport, start, end))
    }

    @Test
    fun buildViewportLabel_containsBothDates() {
        val start = Instant.parse("2026-08-01T00:00:00Z")
        val end = start.plus(Duration.ofDays(1))

        val label = buildViewportLabel(start, end, ZoneId.of("Europe/Warsaw"))

        assertTrue(label.contains("2026") || label.contains("01.08"))
        assertTrue(label.contains("–"))
    }
}



