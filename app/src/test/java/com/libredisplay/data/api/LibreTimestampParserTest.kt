package com.libredisplay.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class LibreTimestampParserTest {

    @Test
    fun parsesIso8601Instant() {
        val parsed = LibreTimestampParser.parse("2026-07-06T10:00:00Z")
        assertEquals(Instant.parse("2026-07-06T10:00:00Z"), parsed)
    }

    @Test
    fun parsesOffsetDateTime() {
        val parsed = LibreTimestampParser.parse("2026-07-06T12:00:00+02:00")
        assertEquals(Instant.parse("2026-07-06T10:00:00Z"), parsed)
    }

    @Test
    fun parsesEpochMillis() {
        val parsed = LibreTimestampParser.parse("1783332000000")
        assertNotNull(parsed)
    }

    @Test
    fun parsesUsAmPmFormat() {
        val parsed = LibreTimestampParser.parse("7/6/2026 10:15:00 AM")
        assertEquals(Instant.parse("2026-07-06T10:15:00Z"), parsed)
    }

    @Test
    fun parsesTwentyFourHourFormat() {
        val parsed = LibreTimestampParser.parse("2026-07-06 23:10:00")
        assertEquals(Instant.parse("2026-07-06T23:10:00Z"), parsed)
    }

    @Test
    fun invalidValue_returnsNull() {
        assertNull(LibreTimestampParser.parse("not-a-date"))
    }
}

