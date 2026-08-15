package com.libredisplay.ui.monitoring

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class ReadingAgeFormatterTest {

    @Test
    fun beforeMinute_returnsPrzedChwila() {
        assertEquals("przed chwilą", formatReadingAge(Duration.ZERO))
        assertEquals("przed chwilą", formatReadingAge(Duration.ofSeconds(20)))
    }

    @Test
    fun minutes_areFormatted() {
        assertEquals("Dane sprzed 7 min", formatReadingAge(Duration.ofMinutes(7)))
    }

    @Test
    fun oneHourPlusMinutes_areFormatted() {
        assertEquals("Dane sprzed 1 godz. 5 min", formatReadingAge(Duration.ofMinutes(65)))
    }

    @Test
    fun manyHours_areFormatted() {
        assertEquals("Dane sprzed 7 godz. 7 min", formatReadingAge(Duration.ofMinutes(427)))
    }

    @Test
    fun daysHoursMinutes_areFormatted() {
        assertEquals("Dane sprzed 1 dzień 1 godz. 10 min", formatReadingAge(Duration.ofMinutes(1510)))
        assertEquals("Dane sprzed 2 dni 2 godz. 15 min", formatReadingAge(Duration.ofMinutes(3015)))
    }

    @Test
    fun nullDuration_returnsMissingText() {
        assertEquals("Brak czasu pomiaru", formatReadingAge(null))
    }
}

