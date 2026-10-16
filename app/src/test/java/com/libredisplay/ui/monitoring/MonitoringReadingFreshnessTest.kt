package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MonitoringReadingFreshnessTest {

    @Test
    fun `stale reading without history is treated as no data`() {
        val now = Instant.parse("2026-10-16T12:00:00Z")
        val staleReading = GlucoseReading.of(
            value = 123,
            timestamp = now.minusSeconds(2 * 60 * 60),
            trend = GlucoseTrend.FLAT,
            history = emptyList()
        )

        assertTrue(shouldDisplayReadingAsNoData(staleReading, now = now, staleMinutesThreshold = 30))
    }

    @Test
    fun `fresh reading without history remains visible`() {
        val now = Instant.parse("2026-10-16T12:00:00Z")
        val freshReading = GlucoseReading.of(
            value = 118,
            timestamp = now.minusSeconds(10 * 60),
            trend = GlucoseTrend.FLAT,
            history = emptyList()
        )

        assertFalse(shouldDisplayReadingAsNoData(freshReading, now = now, staleMinutesThreshold = 30))
    }

    @Test
    fun `stale reading with real history remains visible`() {
        val now = Instant.parse("2026-10-16T12:00:00Z")
        val timestamp = now.minusSeconds(2 * 60 * 60)
        val historical = listOf(
            GlucoseHistoryPoint(value = 120, timestamp = timestamp.minusSeconds(15 * 60), trend = GlucoseTrend.FLAT),
            GlucoseHistoryPoint(value = 123, timestamp = timestamp, trend = GlucoseTrend.FLAT)
        )
        val staleWithHistory = GlucoseReading.of(
            value = 123,
            timestamp = timestamp,
            trend = GlucoseTrend.FLAT,
            history = historical
        )

        assertFalse(shouldDisplayReadingAsNoData(staleWithHistory, now = now, staleMinutesThreshold = 30))
    }
}

