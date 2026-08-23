package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class GlucoseChartSeriesTest {

    private val base: Instant = Instant.parse("2026-08-21T00:00:00Z")

    private fun point(minutes: Long, value: Int = 120) = GlucoseHistoryPoint(
        value = value,
        timestamp = base.plus(Duration.ofMinutes(minutes)),
        trend = GlucoseTrend.FLAT
    )

    private fun millis(minutes: Long) = base.plus(Duration.ofMinutes(minutes)).toEpochMilli()

    @Test
    fun lineIsExtendedToBothChartEdgesWhenDataExistsOutside() {
        // Samples every 5 minutes from 00:00 to 02:00, viewport 00:30 - 01:30.
        val points = (0..24).map { point(it * 5L, 100 + it) }

        val segments = clipAndSegmentSeries(points, millis(30), millis(90))

        assertEquals(1, segments.size)
        val segment = segments.single()
        assertEquals(millis(30), segment.first().timestamp.toEpochMilli())
        assertEquals(millis(90), segment.last().timestamp.toEpochMilli())
    }

    @Test
    fun boundaryValuesAreLinearlyInterpolated() {
        val points = listOf(point(0, 100), point(20, 200))

        val segment = clipAndSegmentSeries(points, millis(10), millis(15)).single()

        assertEquals(150, segment.first().value)
        assertEquals(175, segment.last().value)
    }

    @Test
    fun realSensorGapBreaksTheLine() {
        val before = (0..6).map { point(it * 5L) }          // 00:00 - 00:30
        val after = (0..6).map { point(120 + it * 5L) }     // 02:00 - 02:30

        val segments = clipAndSegmentSeries(before + after, millis(0), millis(150))

        assertEquals(2, segments.size)
        assertTrue(segments[0].last().timestamp.isBefore(segments[1].first().timestamp))
    }

    @Test
    fun smallGapsDoNotBreakTheLine() {
        val points = listOf(point(0), point(10), point(25), point(40))

        val segments = clipAndSegmentSeries(points, millis(0), millis(40))

        assertEquals(1, segments.size)
        assertEquals(4, segments.single().size)
    }

    @Test
    fun emptyInputProducesNoSegments() {
        assertTrue(clipAndSegmentSeries(emptyList(), millis(0), millis(60)).isEmpty())
    }

    @Test
    fun invalidDomainProducesNoSegments() {
        val points = (0..5).map { point(it * 5L) }
        assertTrue(clipAndSegmentSeries(points, millis(60), millis(60)).isEmpty())
        assertTrue(clipAndSegmentSeries(points, millis(60), millis(30)).isEmpty())
    }

    @Test
    fun viewportInsideOneLongGapProducesNoSegments() {
        val points = listOf(point(0), point(600))

        val segments = clipAndSegmentSeries(points, millis(200), millis(300))

        assertTrue(segments.isEmpty())
    }

    @Test
    fun segmentsAreSortedAndFreeOfDuplicates() {
        val points = listOf(point(10), point(0), point(10), point(5))

        val segment = clipAndSegmentSeries(points, millis(0), millis(10)).single()

        assertEquals(segment.sortedBy { it.timestamp }, segment)
        assertEquals(segment.distinctBy { it.timestamp }.size, segment.size)
    }

    @Test
    fun clippingNeverProducesPointsOutsideDomain() {
        val points = (0..48).map { point(it * 5L, 90 + it) }

        clipAndSegmentSeries(points, millis(30), millis(90)).flatten().forEach {
            val value = it.timestamp.toEpochMilli()
            assertTrue(value >= millis(30))
            assertTrue(value <= millis(90))
        }
    }
}

