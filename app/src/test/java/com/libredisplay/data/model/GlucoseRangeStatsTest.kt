package com.libredisplay.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GlucoseRangeStatsTest {

    private val now: Instant = Instant.parse("2026-07-06T12:00:00Z")

    @Test
    fun emptyHistory_returnsNullRangeWithoutFallbackToCurrent() {
        val result = calculateLast12HoursStats(
            history = emptyList(),
            current = GlucoseHistoryPoint(142, now),
            now = now
        )

        assertNull(result.minimum)
        assertNull(result.maximum)
        assertEquals(0, result.pointCount)
    }

    @Test
    fun onePoint_returnsSameMinAndMaxWithPointCountOne() {
        val result = calculateLast12HoursStats(
            history = listOf(GlucoseHistoryPoint(105, now.minusSeconds(60))),
            current = null,
            now = now
        )

        assertEquals(105, result.minimum)
        assertEquals(105, result.maximum)
        assertEquals(1, result.pointCount)
    }

    @Test
    fun multiplePoints_calculatesExpectedMinAndMax() {
        val points = listOf(
            GlucoseHistoryPoint(98, now.minusSeconds(4 * 60 * 60)),
            GlucoseHistoryPoint(105, now.minusSeconds(3 * 60 * 60)),
            GlucoseHistoryPoint(142, now.minusSeconds(2 * 60 * 60)),
            GlucoseHistoryPoint(183, now.minusSeconds(60 * 60)),
            GlucoseHistoryPoint(161, now)
        )

        val result = calculateLast12HoursStats(history = points, current = null, now = now)

        assertEquals(98, result.minimum)
        assertEquals(183, result.maximum)
        assertEquals(5, result.pointCount)
    }

    @Test
    fun olderThanTwelveHours_areFilteredOut() {
        val points = listOf(
            GlucoseHistoryPoint(88, now.minusSeconds(13 * 60 * 60)),
            GlucoseHistoryPoint(150, now)
        )

        val result = calculateLast12HoursStats(history = points, current = null, now = now)

        assertEquals(150, result.minimum)
        assertEquals(150, result.maximum)
        assertEquals(1, result.pointCount)
        assertEquals(1, result.outsideWindowCount)
    }

    @Test
    fun boundaryAtExactlyTwelveHours_isIncluded() {
        val points = listOf(
            GlucoseHistoryPoint(100, now.minusSeconds(12 * 60 * 60)),
            GlucoseHistoryPoint(140, now)
        )

        val result = calculateLast12HoursStats(history = points, current = null, now = now)

        assertEquals(100, result.minimum)
        assertEquals(140, result.maximum)
        assertEquals(2, result.pointCount)
    }

    @Test
    fun newestMeasurementAnchorsWindowInsteadOfDeviceNow() {
        val newest = now.minusSeconds(2 * 60 * 60)
        val points = listOf(
            GlucoseHistoryPoint(95, newest.minusSeconds(11 * 60 * 60)),
            GlucoseHistoryPoint(180, newest)
        )

        val result = calculateLast12HoursStats(history = points, current = null, now = now)

        assertEquals(95, result.minimum)
        assertEquals(180, result.maximum)
        assertEquals(newest, result.newestTimestamp)
    }

    @Test
    fun duplicatePoints_areRemovedByTimestampAndValue() {
        val t = now.minusSeconds(600)
        val points = listOf(
            GlucoseHistoryPoint(120, t),
            GlucoseHistoryPoint(120, t),
            GlucoseHistoryPoint(130, now)
        )

        val result = calculateLast12HoursStats(history = points, current = null, now = now)

        assertEquals(2, result.pointCount)
        assertEquals(120, result.minimum)
        assertEquals(130, result.maximum)
    }

    @Test
    fun nonPositiveValues_areIgnored() {
        val points = listOf(
            GlucoseHistoryPoint(0, now.minusSeconds(900)),
            GlucoseHistoryPoint(-5, now.minusSeconds(600)),
            GlucoseHistoryPoint(140, now)
        )

        val result = calculateLast12HoursStats(history = points, current = null, now = now)

        assertEquals(140, result.minimum)
        assertEquals(140, result.maximum)
        assertEquals(1, result.pointCount)
    }

    @Test
    fun currentMeasurementIsAddedWhenHistoryExistsAndPointIsMissing() {
        val history = listOf(GlucoseHistoryPoint(100, now.minusSeconds(300)))
        val current = GlucoseHistoryPoint(160, now)

        val result = calculateLast12HoursStats(history = history, current = current, now = now)

        assertEquals(100, result.minimum)
        assertEquals(160, result.maximum)
        assertEquals(2, result.pointCount)
    }

    @Test
    fun currentMeasurement_isNotDuplicatedWhenAlreadyInHistory() {
        val current = GlucoseHistoryPoint(140, now)
        val history = listOf(GlucoseHistoryPoint(110, now.minusSeconds(300)), current)

        val result = calculateLast12HoursStats(history = history, current = current, now = now)

        assertEquals(2, result.pointCount)
        assertEquals(110, result.minimum)
        assertEquals(140, result.maximum)
    }

    @Test
    fun pointsAreSortedChronologically() {
        val points = listOf(
            GlucoseHistoryPoint(130, now),
            GlucoseHistoryPoint(120, now.minusSeconds(600)),
            GlucoseHistoryPoint(125, now.minusSeconds(300))
        )

        val result = calculateLast12HoursStats(history = points, current = null, now = now)

        assertTrue(result.usedPoints.zipWithNext().all { (a, b) -> a.timestamp <= b.timestamp })
    }
}

