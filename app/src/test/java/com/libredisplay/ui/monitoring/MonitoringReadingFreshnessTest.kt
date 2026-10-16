package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class MonitoringReadingFreshnessTest {

    @Test
    fun `stale reading with full history is stale but not missing`() {
        val now = Instant.parse("2026-10-16T12:00:00Z")
        val timestamp = now.minusSeconds(2 * 60 * 60)
        val staleReading = GlucoseReading.of(
            value = 123,
            timestamp = timestamp,
            trend = GlucoseTrend.FLAT,
            history = listOf(
                GlucoseHistoryPoint(value = 118, timestamp = timestamp.minusSeconds(15 * 60), trend = GlucoseTrend.FLAT),
                GlucoseHistoryPoint(value = 121, timestamp = timestamp.minusSeconds(10 * 60), trend = GlucoseTrend.FLAT),
                GlucoseHistoryPoint(value = 123, timestamp = timestamp, trend = GlucoseTrend.FLAT)
            )
        )

        assertEquals(ReadingFreshness.STALE, currentReadingFreshness(staleReading, now = now, staleMinutesThreshold = 30))
    }

    @Test
    fun `fresh reading without history is fresh`() {
        val now = Instant.parse("2026-10-16T12:00:00Z")
        val freshReading = GlucoseReading.of(
            value = 118,
            timestamp = now.minusSeconds(10 * 60),
            trend = GlucoseTrend.FLAT,
            history = emptyList()
        )

        assertEquals(ReadingFreshness.FRESH, currentReadingFreshness(freshReading, now = now, staleMinutesThreshold = 30))
    }

    @Test
    fun `missing reading is reported as missing`() {
        val now = Instant.parse("2026-10-16T12:00:00Z")

        assertEquals(ReadingFreshness.MISSING, currentReadingFreshness(reading = null, now = now, staleMinutesThreshold = 30))
    }
}

