package com.libredisplay.data.repository

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GlucoseSyncRepositoryBackfillTest {

    @Test
    fun mergeBackfillWindowPoints_keepsDelayedPointsWithin24Hours() {
        val now = Instant.parse("2026-08-20T12:00:00Z")
        val reading = GlucoseReading(
            value = 145,
            timestamp = now,
            trend = GlucoseTrend.FLAT,
            history = listOf(
                GlucoseHistoryPoint(120, now.minusSeconds(25 * 3600), GlucoseTrend.FLAT),
                GlucoseHistoryPoint(125, now.minusSeconds(23 * 3600), GlucoseTrend.FLAT),
                GlucoseHistoryPoint(130, now.minusSeconds(11 * 3600), GlucoseTrend.RISING),
                GlucoseHistoryPoint(140, now.minusSeconds(60), GlucoseTrend.RISING)
            )
        )

        val merged = mergeBackfillWindowPoints(reading, backfillFrom = now.minusSeconds(24 * 3600))

        assertTrue(merged.none { it.timestamp == now.minusSeconds(25 * 3600) })
        assertTrue(merged.any { it.timestamp == now.minusSeconds(23 * 3600) })
        assertTrue(merged.any { it.timestamp == now.minusSeconds(11 * 3600) })
        assertTrue(merged.any { it.timestamp == now })
    }

    @Test
    fun mergeBackfillWindowPoints_deduplicatesByTimestamp() {
        val now = Instant.parse("2026-08-20T12:00:00Z")
        val duplicateTimestamp = now.minusSeconds(300)
        val reading = GlucoseReading(
            value = 150,
            timestamp = now,
            trend = GlucoseTrend.RISING,
            history = listOf(
                GlucoseHistoryPoint(140, duplicateTimestamp, GlucoseTrend.FLAT),
                GlucoseHistoryPoint(141, duplicateTimestamp, GlucoseTrend.RISING)
            )
        )

        val merged = mergeBackfillWindowPoints(reading, backfillFrom = now.minusSeconds(12 * 3600))

        assertEquals(2, merged.size)
        assertEquals(duplicateTimestamp, merged.first().timestamp)
        assertEquals(now, merged.last().timestamp)
    }
}

