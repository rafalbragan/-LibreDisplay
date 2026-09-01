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
        assertEquals("7 min temu", formatReadingAge(Duration.ofMinutes(7)))
    }

    @Test
    fun oneHourPlusMinutes_areFormatted() {
        assertEquals("1 godz. 5 min temu", formatReadingAge(Duration.ofMinutes(65)))
    }

    @Test
    fun manyHours_areFormatted() {
        assertEquals("7 godz. 7 min temu", formatReadingAge(Duration.ofMinutes(427)))
    }

    @Test
    fun daysHoursMinutes_areFormatted() {
        assertEquals("1 dzień 1 godz. temu", formatReadingAge(Duration.ofMinutes(1510)))
        assertEquals("2 dni 2 godz. temu", formatReadingAge(Duration.ofMinutes(3015)))
    }

    @Test
    fun nullDuration_returnsMissingText() {
        assertEquals("brak czasu pomiaru", formatReadingAge(null))
    }
}

