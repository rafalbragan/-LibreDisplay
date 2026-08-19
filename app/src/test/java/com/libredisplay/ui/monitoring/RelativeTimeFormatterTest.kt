package com.libredisplay.ui.monitoring

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RelativeTimeFormatterTest {

    @Test
    fun lessThanMinute_returnsChwileTemu() {
        assertEquals("chwilę temu", RelativeTimeFormatter.formatDurationAgo(Duration.ZERO))
        assertEquals("chwilę temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofSeconds(30)))
    }

    @Test
    fun minutes_formattedCorrectly() {
        assertEquals("1 min temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofMinutes(1)))
        assertEquals("15 min temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofMinutes(15)))
        assertEquals("59 min temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofMinutes(59)))
    }

    @Test
    fun oneHour_formatted() {
        assertEquals("1g temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofMinutes(60)))
    }

    @Test
    fun hourAndMinutes_formatted() {
        assertEquals("1g 15m temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofMinutes(75)))
        assertEquals("1g 5m temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofMinutes(65)))
    }

    @Test
    fun multipleHours_formatted() {
        assertEquals("2g temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofMinutes(120)))
        assertEquals("2g 30m temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofMinutes(150)))
        assertEquals("7g 7m temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofMinutes(427)))
    }

    @Test
    fun singleDay_formatted() {
        assertEquals("1 dzień temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofMinutes(1440)))
    }

    @Test
    fun dayAndHours_formatted() {
        assertEquals("1 dzień 1g temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofMinutes(1500)))
        assertEquals("1 dzień 8g temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofMinutes(1920)))
    }

    @Test
    fun multipleDays_formatted() {
        assertEquals("2 dni temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofMinutes(2880)))
        assertEquals("2 dni 2g temu", RelativeTimeFormatter.formatDurationAgo(Duration.ofMinutes(3000)))
    }

    @Test
    fun nullDuration_returnsMissingText() {
        assertEquals("brak danych", RelativeTimeFormatter.formatDurationAgo(null))
    }

    @Test
    fun formatTimeAgo_usesInstantDifference() {
        val now = Instant.parse("2026-08-19T12:00:00Z")
        val then = Instant.parse("2026-08-19T11:45:00Z")
        assertEquals("15 min temu", RelativeTimeFormatter.formatTimeAgo(then, now))
    }
}

