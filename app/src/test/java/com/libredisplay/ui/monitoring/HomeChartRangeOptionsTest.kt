package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class HomeChartRangeOptionsTest {

    @Test
    fun withoutData_onlyShortestRangeIsEnabledPlusOnePreview() {
        val options = homeChartRangeOptions(Duration.ZERO)

        assertEquals(2, options.size)
        assertEquals(HomeChartRange.LAST_1_HOUR, options[0].range)
        assertTrue(options[0].enabled)
        assertEquals(HomeChartRange.LAST_3_HOURS, options[1].range)
        assertFalse(options[1].enabled)
    }

    @Test
    fun exactlyOneGreyedOutRangeIsAlwaysOffered() {
        val options = homeChartRangeOptions(Duration.ofHours(13))

        assertEquals(listOf("1g", "3g", "6g", "9g", "12g", "24g", "3d"), options.map { it.range.shortLabel })
        assertEquals(6, options.count { it.enabled })
        assertEquals(1, options.count { !it.enabled })
        assertEquals(HomeChartRange.LAST_3_DAYS, options.last().range)
        assertFalse(options.last().enabled)
    }

    @Test
    fun moreDataThanCurrentScale_unlocksNextScaleAndGreysTheFollowing() {
        // Just over 12h of data: 24g must become selectable and 3d shown greyed-out as preview.
        val options = homeChartRangeOptions(Duration.ofHours(12).plusMinutes(1))

        val enabled = options.filter { it.enabled }.map { it.range }
        val greyed = options.filterNot { it.enabled }.map { it.range }
        assertTrue(HomeChartRange.LAST_24_HOURS in enabled)
        assertEquals(listOf(HomeChartRange.LAST_3_DAYS), greyed)
    }

    @Test
    fun fiveDaysOfData_makesSevenDaysClickableAndGreysFourteenDays() {
        val options = homeChartRangeOptions(Duration.ofDays(5))

        val enabled = options.filter { it.enabled }.map { it.range }
        val greyed = options.filterNot { it.enabled }.map { it.range }
        assertTrue(HomeChartRange.LAST_7_DAYS in enabled)
        assertEquals(listOf(HomeChartRange.LAST_14_DAYS), greyed)
        assertEquals(HomeChartRange.LAST_7_DAYS, largestSelectableHomeChartRange(Duration.ofDays(5)))
    }

    @Test
    fun longRangesBecomeAvailableAsHistoryGrows() {
        assertEquals(HomeChartRange.LAST_3_DAYS, largestSelectableHomeChartRange(Duration.ofHours(30)))
        assertEquals(HomeChartRange.LAST_7_DAYS, largestSelectableHomeChartRange(Duration.ofDays(4)))
        assertEquals(HomeChartRange.LAST_14_DAYS, largestSelectableHomeChartRange(Duration.ofDays(10)))
        assertEquals(HomeChartRange.LAST_1_MONTH, largestSelectableHomeChartRange(Duration.ofDays(20)))
        assertEquals(HomeChartRange.LAST_3_MONTHS, largestSelectableHomeChartRange(Duration.ofDays(60)))
        assertEquals(HomeChartRange.LAST_6_MONTHS, largestSelectableHomeChartRange(Duration.ofDays(120)))
        assertEquals(HomeChartRange.ALL_AVAILABLE, largestSelectableHomeChartRange(Duration.ofDays(400)))
    }

    @Test
    fun everyRangeHasAReadableTickInterval() {
        HomeChartRange.entries.forEach { range ->
            val interval = homeChartAxisTickInterval(range)
            assertTrue("range=$range", !interval.isZero && !interval.isNegative)
            assertTrue("range=$range", interval < range.duration)
        }
    }

    @Test
    fun rangeLabelsAreUniqueAndShort() {
        val labels = HomeChartRange.entries.map { it.shortLabel }
        assertEquals(labels.size, labels.distinct().size)
        assertTrue(labels.all { it.length <= 3 })
    }

    @Test
    fun allRangesAreOrderedAscending() {
        val durations = HomeChartRange.entries.map { it.duration }
        assertEquals(durations.sorted(), durations)
    }

    @Test
    fun dataSummary_explainsStoredHistoryAndNextRange() {
        val now = Instant.parse("2026-08-21T12:00:00Z")
        val points = (0 until 40).map {
            GlucoseHistoryPoint(
                value = 110,
                timestamp = now.minus(Duration.ofMinutes((39 - it) * 15L)),
                trend = GlucoseTrend.FLAT
            )
        }

        val summary = buildHomeDataSummary(points)

        assertTrue(summary.primaryText.startsWith("Zapisana historia:"))
        assertNotNull(summary.nextRangeLabel)
        assertNotNull(summary.secondaryText)
        assertTrue(summary.secondaryText!!.contains("będzie dostępny za ok."))
    }

    @Test
    fun dataSummary_withoutData_saysBrak() {
        val summary = buildHomeDataSummary(emptyList())

        assertEquals("Zapisana historia: brak", summary.primaryText)
    }

    @Test
    fun dataSummary_withFullYear_hasNoPendingRange() {
        val now = Instant.parse("2026-08-21T12:00:00Z")
        val points = listOf(
            GlucoseHistoryPoint(100, now.minus(Duration.ofDays(730)), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(100, now, GlucoseTrend.FLAT)
        )

        val summary = buildHomeDataSummary(points)

        assertNull(summary.nextRangeLabel)
        assertNull(summary.secondaryText)
    }

    // ------------------------------------------------------------------ navigator

    @Test
    fun oneFullSwipeMovesTheViewportFromLeftToRight() {
        val geometry = computeHomeNavigatorGeometry(
            totalWidthPx = 1000f,
            leftInsetPx = 0f,
            rightInsetPx = 0f,
            viewportFraction = 0f,
            windowFraction = 0.5f
        )

        // Dragging by the pannable distance must cover the whole range in a single gesture.
        val pannable = navigatorPannableWidth(geometry)
        assertEquals(500f, pannable)
        assertEquals(1f, navigatorFractionAfterDrag(0f, pannable, geometry))
        assertEquals(0f, navigatorFractionAfterDrag(1f, -pannable, geometry))
    }

    @Test
    fun smallDragMovesProportionallyNotByPixels() {
        val geometry = computeHomeNavigatorGeometry(
            totalWidthPx = 1000f,
            leftInsetPx = 0f,
            rightInsetPx = 0f,
            viewportFraction = 0f,
            windowFraction = 0.5f
        )

        // 12 h window inside 24 h of data: 50 px of finger travel must move 10% of the range.
        assertEquals(0.1f, navigatorFractionAfterDrag(0f, 50f, geometry), 0.0001f)
    }

    @Test
    fun draggingIsClampedToValidRange() {
        val geometry = computeHomeNavigatorGeometry(
            totalWidthPx = 600f,
            leftInsetPx = 0f,
            rightInsetPx = 0f,
            viewportFraction = 0.5f,
            windowFraction = 0.25f
        )

        assertEquals(1f, navigatorFractionAfterDrag(0.9f, 10_000f, geometry))
        assertEquals(0f, navigatorFractionAfterDrag(0.1f, -10_000f, geometry))
        assertEquals(0.5f, navigatorFractionAfterDrag(0.5f, Float.NaN, geometry))
    }

    @Test
    fun tappingTheTrackCentersTheViewportOnThatPosition() {
        val geometry = computeHomeNavigatorGeometry(
            totalWidthPx = 1000f,
            leftInsetPx = 0f,
            rightInsetPx = 0f,
            viewportFraction = 0f,
            windowFraction = 0.2f
        )

        assertEquals(0f, navigatorFractionForPosition(0f, geometry))
        assertEquals(1f, navigatorFractionForPosition(1000f, geometry))
        assertEquals(0.5f, navigatorFractionForPosition(500f, geometry), 0.001f)
    }

    @Test
    fun thumbNeverCollapsesToZeroWidth() {
        val geometry = computeHomeNavigatorGeometry(
            totalWidthPx = 800f,
            leftInsetPx = 0f,
            rightInsetPx = 0f,
            viewportFraction = 0.5f,
            windowFraction = 0.0f
        )

        assertTrue(geometry.viewportWidth >= 1f)
        assertTrue(navigatorPannableWidth(geometry) >= 1f)
    }
}

