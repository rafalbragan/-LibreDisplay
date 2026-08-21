package com.libredisplay.ui.monitoring

import androidx.compose.ui.geometry.Rect
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class HistoryAggregationTest {

    @Test
    fun findNearestPoint_returnsClosestByX() {
        val start = Instant.parse("2026-07-27T10:00:00Z")
        val end = start.plusSeconds(3600)
        val points = listOf(
            GlucoseHistoryPoint(110, start.plusSeconds(0), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(120, start.plusSeconds(1800), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(130, start.plusSeconds(3600), GlucoseTrend.FLAT)
        )

        val result = findNearestPoint(
            points = points,
            touchX = 45f,
            chartBounds = Rect(0f, 0f, 100f, 100f),
            timeRangeStart = start,
            timeRangeEnd = end
        )

        assertNotNull(result)
        assertEquals(120, result?.value)
    }

    @Test
    fun findNearestPoint_emptyList_returnsNull() {
        val result = findNearestPoint(
            points = emptyList(),
            touchX = 10f,
            chartBounds = Rect(0f, 0f, 100f, 100f),
            timeRangeStart = Instant.now().minusSeconds(3600),
            timeRangeEnd = Instant.now()
        )
        assertNull(result)
    }

    @Test
    fun chartMode_matchesRangePolicy() {
        assertEquals("line", chartModeForRange(TimeRange.LAST_3_HOURS))
        assertEquals("line", chartModeForRange(TimeRange.LAST_6_HOURS))
        assertEquals("line", chartModeForRange(TimeRange.LAST_12_HOURS))
        assertEquals("line", chartModeForRange(TimeRange.LAST_24_HOURS))
        assertEquals("line", chartModeForRange(TimeRange.LAST_7_DAYS))
        assertEquals("bar", chartModeForRange(TimeRange.LAST_90_DAYS))
    }

    @Test
    fun bucketSizeForRange_supportsShortHistoryRanges() {
        assertEquals(Duration.ofMinutes(5), bucketSizeForRange(TimeRange.LAST_3_HOURS))
        assertEquals(Duration.ofMinutes(5), bucketSizeForRange(TimeRange.LAST_6_HOURS))
        assertEquals(Duration.ofMinutes(5), bucketSizeForRange(TimeRange.LAST_12_HOURS))
        assertEquals(Duration.ofMinutes(10), bucketSizeForRange(TimeRange.LAST_24_HOURS))
    }

    @Test
    fun aggregateReadingsForRange_dailyBuckets_keepNullsWhenNoData() {
        val start = Instant.parse("2026-07-20T00:00:00Z")
        val points = listOf(
            GlucoseHistoryPoint(95, start.plusSeconds(0), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(105, start.plusSeconds(900), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(160, start.plusSeconds(1800), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(170, start.plusSeconds(86400), GlucoseTrend.FLAT)
        )

        val aggregated = aggregateReadingsForRange(
            readings = points,
            timeRange = TimeRange.LAST_7_DAYS,
            bucketSize = Duration.ofHours(3)
        )

        assertTrue(aggregated.buckets.isNotEmpty())
        assertTrue(aggregated.buckets.any { it.averageGlucoseMgDl == null })
    }

    @Test
    fun toHistoryTimeRange_preservesDashboardPresetIntent() {
        assertEquals(TimeRange.LAST_12_HOURS, TimeRangeState.fromPreset(PresetTimeRange.LAST_12_HOURS).toHistoryTimeRange())
        assertEquals(TimeRange.LAST_24_HOURS, TimeRangeState.fromPreset(PresetTimeRange.LAST_24_HOURS).toHistoryTimeRange())
        assertEquals(TimeRange.LAST_7_DAYS, TimeRangeState.fromPreset(PresetTimeRange.LAST_7_DAYS).toHistoryTimeRange())
        assertEquals(TimeRange.LAST_30_DAYS, TimeRangeState.fromPreset(PresetTimeRange.LAST_14_DAYS).toHistoryTimeRange())
    }
}

