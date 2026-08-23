package com.libredisplay.ui.restore

import com.libredisplay.data.backup.BackupCoverageCalculator
import com.libredisplay.data.backup.BackupReadingDto
import com.libredisplay.data.backup.PersonMergePlan
import com.libredisplay.data.backup.ReadingConflict
import com.libredisplay.data.backup.RestorePlan
import com.libredisplay.data.repository.AppDataBackupRepository.BackupOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class StartupRestoreFormatterTest {

    private val zone: ZoneId = ZoneId.of("Europe/Warsaw")

    private fun offerWithTwoPeople(): BackupOffer = BackupOffer(
        exists = true,
        createdAtIso = "2026-08-24T10:00:00Z",
        appVersion = "2.4.0",
        settingsAvailable = true,
        persons = listOf(
            BackupCoverageCalculator.forPerson(
                patientId = "p1",
                displayName = "Anna",
                timestamps = (0 until 288).map {
                    Instant.parse("2026-08-18T00:00:00Z").plusSeconds(300L * it)
                },
                zoneId = zone
            ),
            BackupCoverageCalculator.forPerson(
                patientId = "p2",
                displayName = "Marek",
                timestamps = listOf(
                    Instant.parse("2026-08-20T00:00:00Z"),
                    Instant.parse("2026-08-21T00:00:00Z")
                ),
                zoneId = zone
            )
        )
    )

    @Test
    fun headlineListsPeopleAndReadings() {
        val headline = StartupRestoreFormatter.offerHeadline(offerWithTwoPeople())

        assertTrue(headline, headline.contains("2 osób"))
        assertTrue(headline, headline.contains("290 odczytów"))
    }

    @Test
    fun headlineExplainsMissingFile() {
        assertEquals(
            "Nie znaleziono zapisanych danych na tym urządzeniu.",
            StartupRestoreFormatter.offerHeadline(BackupOffer.EMPTY)
        )
    }

    @Test
    fun offerLinesShowNamePeriodAndGapInformation() {
        val lines = StartupRestoreFormatter.offerLines(offerWithTwoPeople(), zone)

        assertEquals(2, lines.size)
        assertTrue(lines[0], lines[0].startsWith("Anna · "))
        assertTrue(lines[0], lines[0].contains("18.08.2026"))
        assertTrue(lines[0], lines[0].contains("dane ciągłe"))
        assertTrue(lines[1], lines[1].startsWith("Marek · "))
        assertTrue(lines[1], lines[1].contains("z przerwami"))
        assertTrue(lines[1], lines[1].contains("%"))
    }

    @Test
    fun conflictSummaryDescribesPeopleAndLimitsExamples() {
        val conflicts = (0 until 9).map {
            ReadingConflict(
                epochMillis = Instant.parse("2026-08-20T06:00:00Z").plusSeconds(300L * it).toEpochMilli(),
                localValueMgDl = 100 + it,
                backupValueMgDl = 200 + it
            )
        }
        val plan = RestorePlan(
            persons = listOf(
                PersonMergePlan(
                    patientId = "p1",
                    displayName = "Anna",
                    existsLocally = true,
                    addedReadings = emptyList<BackupReadingDto>(),
                    identicalReadings = 42,
                    conflicts = conflicts,
                    addedRangeStart = null,
                    addedRangeEnd = null,
                    addedDistinctDays = 0
                )
            ),
            settingsAvailable = true,
            createdAtIso = "2026-08-24T10:00:00Z",
            appVersion = "2.4.0",
            schemaVersion = 3
        )

        val summary = StartupRestoreFormatter.conflictSummary(plan, zone)

        assertEquals(9, summary.totalConflicts)
        assertTrue(summary.hasConflicts)
        assertEquals(1, summary.personLines.size)
        assertTrue(summary.personLines[0], summary.personLines[0].contains("Anna"))
        assertTrue(summary.personLines[0], summary.personLines[0].contains("42 identycznych"))
        assertEquals(StartupRestoreFormatter.MAX_CONFLICT_EXAMPLES, summary.examples.size)
        assertTrue(summary.examples[0], summary.examples[0].contains("mg/dL"))
    }

    @Test
    fun conflictSummaryIsEmptyWhenNothingDiffers() {
        val plan = RestorePlan(
            persons = emptyList(),
            settingsAvailable = false,
            createdAtIso = "2026-08-24T10:00:00Z",
            appVersion = "2.4.0",
            schemaVersion = 3
        )

        val summary = StartupRestoreFormatter.conflictSummary(plan, zone)

        assertEquals(0, summary.totalConflicts)
        assertTrue(summary.personLines.isEmpty())
        assertTrue(summary.examples.isEmpty())
    }
}

