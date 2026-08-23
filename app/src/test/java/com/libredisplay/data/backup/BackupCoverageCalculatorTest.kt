package com.libredisplay.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class BackupCoverageCalculatorTest {

    private val zone: ZoneId = ZoneId.of("Europe/Warsaw")
    private val start: Instant = Instant.parse("2026-08-18T00:00:00Z")

    private fun continuousTimestamps(count: Int): List<Instant> =
        (0 until count).map { start.plus(Duration.ofMinutes(5L * it)) }

    @Test
    fun emptyHistoryIsReportedAsEmpty() {
        val coverage = BackupCoverageCalculator.forPerson("p1", "Anna", emptyList(), zoneId = zone)

        assertTrue(coverage.isEmpty)
        assertEquals(0, coverage.readingsCount)
        assertEquals(0, coverage.missingPercent)
        assertFalse(coverage.hasGaps)
        assertEquals("brak zapisanych odczytów", BackupCoverageCalculator.describe(coverage, zone))
    }

    @Test
    fun continuousHistoryHasNoGaps() {
        val coverage = BackupCoverageCalculator.forPerson(
            patientId = "p1",
            displayName = "Anna",
            timestamps = continuousTimestamps(288),
            zoneId = zone
        )

        assertEquals(288, coverage.readingsCount)
        assertEquals(288, coverage.expectedReadings)
        assertEquals(0, coverage.missingPercent)
        assertFalse(coverage.hasGaps)
        assertTrue(BackupCoverageCalculator.describe(coverage, zone).contains("dane ciągłe"))
    }

    @Test
    fun halfOfThePeriodMissingIsReportedAsFiftyPercent() {
        // Keep every second reading of a 100 slot window -> half of the expected values are gone.
        val timestamps = continuousTimestamps(100).filterIndexed { index, _ -> index % 2 == 0 }

        val coverage = BackupCoverageCalculator.forPerson("p1", "Anna", timestamps, zoneId = zone)

        assertEquals(50, coverage.readingsCount)
        assertEquals(99, coverage.expectedReadings)
        assertEquals(49, coverage.missingPercent)
        assertTrue(coverage.hasGaps)
        val description = BackupCoverageCalculator.describe(coverage, zone)
        assertTrue(description, description.contains("z przerwami"))
        assertTrue(description, description.contains("49%"))
    }

    @Test
    fun descriptionContainsMinAndMaxDate() {
        val timestamps = listOf(
            Instant.parse("2026-08-18T06:00:00Z"),
            Instant.parse("2026-08-20T18:00:00Z")
        )

        val coverage = BackupCoverageCalculator.forPerson("p1", "Anna", timestamps, zoneId = zone)
        val description = BackupCoverageCalculator.describe(coverage, zone)

        assertEquals(Instant.parse("2026-08-18T06:00:00Z"), coverage.firstTimestamp)
        assertEquals(Instant.parse("2026-08-20T18:00:00Z"), coverage.lastTimestamp)
        assertTrue(description, description.contains("18.08.2026"))
        assertTrue(description, description.contains("20.08.2026"))
        // 18.08 08:00 and 20.08 20:00 local time -> two calendar days carry data.
        assertEquals(2, coverage.distinctDays)
    }

    @Test
    fun duplicatedTimestampsAreCountedOnce() {
        val instant = Instant.parse("2026-08-18T06:00:00Z")

        val coverage = BackupCoverageCalculator.forPerson(
            patientId = "p1",
            displayName = "Anna",
            timestamps = listOf(instant, instant, instant),
            zoneId = zone
        )

        assertEquals(1, coverage.readingsCount)
        assertEquals(1, coverage.expectedReadings)
        assertEquals(0, coverage.missingPercent)
    }

    @Test
    fun polishDayLabelsAreCorrect() {
        assertEquals("brak dni", BackupCoverageCalculator.polishDays(0))
        assertEquals("1 dzień", BackupCoverageCalculator.polishDays(1))
        assertEquals("3 dni", BackupCoverageCalculator.polishDays(3))
    }
}

