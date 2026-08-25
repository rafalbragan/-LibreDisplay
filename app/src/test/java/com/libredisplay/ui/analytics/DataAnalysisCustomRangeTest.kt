package com.libredisplay.ui.analytics

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class DataAnalysisCustomRangeTest {

    @Test
    fun pickerRangeToInstants_mapsUtcPickerDaysToLocalDayBounds() {
        val startUtcMillis = Instant.parse("2026-08-01T00:00:00Z").toEpochMilli()
        val endUtcMillis = Instant.parse("2026-08-03T00:00:00Z").toEpochMilli()

        val (start, end) = pickerRangeToInstants(
            PickerUtcDateRange(startUtcMillis, endUtcMillis),
            ZoneId.of("Europe/Warsaw")
        )

        val startDate = start.atZone(ZoneId.of("Europe/Warsaw")).toLocalDate()
        val endDate = end.atZone(ZoneId.of("Europe/Warsaw")).toLocalDate()

        assertEquals("2026-08-01", startDate.toString())
        assertEquals("2026-08-03", endDate.toString())
        assertEquals(ZoneOffset.UTC.totalSeconds, Instant.ofEpochMilli(startUtcMillis).atOffset(ZoneOffset.UTC).offset.totalSeconds)
    }
}

