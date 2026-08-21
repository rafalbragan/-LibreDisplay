package com.libredisplay.ui.monitoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class PolishFormattersTest {

    @Test
    fun formatUserFacing_usesLocalDeviceTimezoneForToday() {
        val zoneId = ZoneId.of("Europe/Warsaw")
        val now = Instant.parse("2026-08-18T13:00:00Z")
        val timestamp = Instant.parse("2026-08-18T12:45:00Z")

        val label = PolishDateTimeFormatter.formatUserFacing(timestamp, now = now, zoneId = zoneId)

        assertEquals("Dzisiaj 14:45", label)
    }

    @Test
    fun formatUserFacing_usesYesterdayWhenApplicable() {
        val zoneId = ZoneId.of("Europe/Warsaw")
        val now = Instant.parse("2026-08-18T05:00:00Z")
        val timestamp = Instant.parse("2026-08-17T20:10:00Z")

        val label = PolishDateTimeFormatter.formatUserFacing(timestamp, now = now, zoneId = zoneId)

        assertEquals("Wczoraj 22:10", label)
    }

    @Test
    fun formatAbsolute_usesPolishLocalTime() {
        val zoneId = ZoneId.of("Europe/Warsaw")
        val timestamp = Instant.parse("2026-08-18T12:45:00Z")

        val label = PolishDateTimeFormatter.formatAbsolute(timestamp, zoneId)

        assertEquals("18.08.2026, 14:45", label)
    }

    @Test
    fun compactDuration_examplesMatchExpectedLabels() {
        assertEquals("0m", PolishDateTimeFormatter.formatCompactDuration(Duration.ZERO))
        assertEquals("5m", PolishDateTimeFormatter.formatCompactDuration(Duration.ofMinutes(5)))
        assertEquals("45m", PolishDateTimeFormatter.formatCompactDuration(Duration.ofMinutes(45)))
        assertEquals("1g", PolishDateTimeFormatter.formatCompactDuration(Duration.ofMinutes(60)))
        assertEquals("1g 15m", PolishDateTimeFormatter.formatCompactDuration(Duration.ofMinutes(75)))
        assertEquals("2g", PolishDateTimeFormatter.formatCompactDuration(Duration.ofMinutes(120)))
        assertEquals("2g 30m", PolishDateTimeFormatter.formatCompactDuration(Duration.ofMinutes(150)))
        assertEquals("1d 2g", PolishDateTimeFormatter.formatCompactDuration(Duration.ofHours(26)))
    }

    @Test
    fun chartAxisLabel_isCompactAndTimezoneAware() {
        val zoneId = ZoneId.of("Europe/Warsaw")
        val instant = Instant.parse("2026-08-18T12:45:00Z")

        assertEquals(
            "18.08\n14:45",
            PolishDateTimeFormatter.formatChartAxisLabel(instant, visibleDuration = Duration.ofHours(24), zoneId = zoneId)
        )
        assertEquals(
            "18.08\n14:45",
            PolishDateTimeFormatter.formatChartAxisLabel(instant, visibleDuration = Duration.ofDays(3), zoneId = zoneId)
        )
        assertEquals(
            "18.08",
            PolishDateTimeFormatter.formatChartAxisLabel(instant, visibleDuration = Duration.ofDays(30), zoneId = zoneId)
        )
    }

    @Test
    fun rangeTileDuration_distinguishesZeroFromNoData() {
        assertEquals("brak danych", PolishDateTimeFormatter.formatRangeTileDuration(null, hasReadings = false))
        assertEquals("0m", PolishDateTimeFormatter.formatRangeTileDuration(Duration.ZERO, hasReadings = true))
    }

    @Test
    fun calendarDaysBetween_countsInclusiveDays() {
        val days = PolishDateTimeFormatter.calendarDaysBetween(
            start = Instant.parse("2026-08-16T20:00:00Z"),
            end = Instant.parse("2026-08-18T03:00:00Z"),
            zoneId = ZoneId.of("Europe/Warsaw")
        )

        assertTrue(days >= 2)
    }

    @Test
    fun rangeLabel_usesSharedFormatter() {
        val label = PolishDateTimeFormatter.formatRangeLabel(
            start = Instant.parse("2026-08-16T20:00:00Z"),
            end = Instant.parse("2026-08-18T03:00:00Z"),
            zoneId = ZoneId.of("Europe/Warsaw")
        )

        assertEquals("Zakres: 16.08.2026 - 18.08.2026", label)
    }
}

