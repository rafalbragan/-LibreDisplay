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
        assertEquals("line", chartModeForRange(TimeRange.LAST_12_HOURS))
        assertEquals("line", chartModeForRange(TimeRange.LAST_24_HOURS))
        assertEquals("aggregated", chartModeForRange(TimeRange.LAST_7_DAYS))
        assertEquals("bar", chartModeForRange(TimeRange.LAST_90_DAYS))
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
}

