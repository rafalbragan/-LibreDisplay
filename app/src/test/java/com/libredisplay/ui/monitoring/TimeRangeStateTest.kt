package com.libredisplay.ui.monitoring

import org.junit.Test
import org.junit.Assert.*
import java.time.Instant

class TimeRangeStateTest {

    @Test
    fun fromPreset_last24Hours_setsCorrectRange() {
        val now = Instant.parse("2026-08-18T14:00:00Z")
        val state = TimeRangeState.fromPreset(PresetTimeRange.LAST_24_HOURS, now)

        assertEquals(PresetTimeRange.LAST_24_HOURS, state.presetRange)
        assertFalse(state.isCustomRange)
        assertEquals(86400, state.durationSeconds)
        assertEquals(24.0, state.durationHours, 0.01)
    }

    @Test
    fun fromPreset_last7Days_setsCorrectRange() {
        val now = Instant.parse("2026-08-18T14:00:00Z")
        val state = TimeRangeState.fromPreset(PresetTimeRange.LAST_7_DAYS, now)

        assertEquals(PresetTimeRange.LAST_7_DAYS, state.presetRange)
        assertEquals(604800, state.durationSeconds)
        assertEquals(7.0, state.durationDays, 0.01)
    }

    @Test
    fun rangeLabel_presetRange_returnsPresetLabel() {
        val state = TimeRangeState.fromPreset(PresetTimeRange.LAST_24_HOURS)
        val label = state.rangeLabel()

        assertEquals("Zakres: Ostatnie 24 godziny", label)
    }

    @Test
    fun rangeLabel_customRange_returnsCustomLabel() {
        val start = Instant.parse("2026-08-01T00:00:00Z")
        val end = Instant.parse("2026-08-18T00:00:00Z")
        val state = TimeRangeState(
            startTimestamp = start,
            endTimestamp = end,
            isCustomRange = true
        )

        val label = state.rangeLabel()
        assertTrue(label.contains("Zakres:"))
        assertTrue(label.contains("08.2026"))
    }

    @Test
    fun availableRanges_filtersByDataAvailability() {
        val now = Instant.parse("2026-08-18T14:00:00Z")
        val dataStart = now.minusSeconds(2592000) // 30 days ago

        val available = PresetTimeRange.availableRanges(dataStart, now)

        // Should include ranges up to 30 days
        assertTrue(available.contains(PresetTimeRange.LAST_24_HOURS))
        assertTrue(available.contains(PresetTimeRange.LAST_7_DAYS))
        assertTrue(available.contains(PresetTimeRange.LAST_30_DAYS))
        // Should NOT include ranges beyond 30 days
        assertFalse(available.contains(PresetTimeRange.LAST_90_DAYS))
    }

    @Test
    fun durationHours_calculatesCorrectly() {
        val start = Instant.parse("2026-08-18T00:00:00Z")
        val end = Instant.parse("2026-08-18T12:00:00Z")
        val state = TimeRangeState(startTimestamp = start, endTimestamp = end)

        assertEquals(12.0, state.durationHours, 0.01)
    }

    @Test
    fun durationDays_calculatesCorrectly() {
        val start = Instant.parse("2026-08-11T00:00:00Z")
        val end = Instant.parse("2026-08-18T00:00:00Z")
        val state = TimeRangeState(startTimestamp = start, endTimestamp = end)

        assertEquals(7.0, state.durationDays, 0.01)
    }
}



