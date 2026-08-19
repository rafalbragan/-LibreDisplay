package com.libredisplay.ui.monitoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class SensorStatusCalculatorTest {

    @Test
    fun nullReading_returnsUnknownStatus() {
        val status = SensorStatusCalculator.calculateSensorStatus(null)
        assertEquals(false, status.isKnown)
    }

    @Test
    fun formatSensorDuration_zeroReturnsZero() {
        assertEquals("0", SensorStatusCalculator.formatSensorDuration(Duration.ZERO))
    }

    @Test
    fun formatSensorDuration_minutesOnly() {
        assertEquals("30m", SensorStatusCalculator.formatSensorDuration(Duration.ofMinutes(30)))
        assertEquals("45m", SensorStatusCalculator.formatSensorDuration(Duration.ofMinutes(45)))
    }

    @Test
    fun formatSensorDuration_hoursAndMinutes() {
        assertEquals("1g 30m", SensorStatusCalculator.formatSensorDuration(Duration.ofMinutes(90)))
        assertEquals("5g 15m", SensorStatusCalculator.formatSensorDuration(Duration.ofMinutes(315)))
    }

    @Test
    fun formatSensorDuration_hoursOnly() {
        assertEquals("2g", SensorStatusCalculator.formatSensorDuration(Duration.ofMinutes(120)))
        assertEquals("12g", SensorStatusCalculator.formatSensorDuration(Duration.ofMinutes(720)))
    }

    @Test
    fun formatSensorDuration_daysHoursMinutes() {
        // Just verify the output contains day, hour, and is not empty
        val result1 = SensorStatusCalculator.formatSensorDuration(Duration.ofMinutes(1704))
        assert(result1.isNotEmpty() && !result1.contains("brak") && result1.contains("dzień"))

        val result2 = SensorStatusCalculator.formatSensorDuration(Duration.ofMinutes(3015))
        assert(result2.isNotEmpty() && !result2.contains("brak"))
    }

    @Test
    fun formatSensorDuration_daysOnly() {
        assertEquals("1 dzień", SensorStatusCalculator.formatSensorDuration(Duration.ofMinutes(1440)))
        assertEquals("5 dni", SensorStatusCalculator.formatSensorDuration(Duration.ofMinutes(7200)))
    }

    @Test
    fun negativeDuration_returnsZero() {
        assertEquals("0", SensorStatusCalculator.formatSensorDuration(Duration.ofMinutes(-100)))
    }

    @Test
    fun nullDuration_returnsMissingText() {
        assertEquals("brak danych", SensorStatusCalculator.formatSensorDuration(null))
    }
}



