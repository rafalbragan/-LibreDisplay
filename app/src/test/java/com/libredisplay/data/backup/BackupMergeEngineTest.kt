package com.libredisplay.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class BackupMergeEngineTest {

    private val zone: ZoneId = ZoneId.of("Europe/Warsaw")
    private val base: Instant = Instant.parse("2026-08-18T00:00:00Z")

    private fun reading(offsetMinutes: Long, value: Int, patientId: String = "p1"): BackupReadingDto {
        val timestamp = base.plus(Duration.ofMinutes(offsetMinutes))
        return BackupReadingDto(
            id = "$patientId:${timestamp.toEpochMilli()}",
            patientId = patientId,
            timestampIso = timestamp.toString(),
            valueMgDl = value,
            receivedAtIso = timestamp.toString(),
            createdAtIso = timestamp.toString()
        )
    }

    private fun local(offsetMinutes: Long, value: Int, patientId: String = "p1"): LocalReadingKey =
        LocalReadingKey(
            patientId = patientId,
            epochMillis = base.plus(Duration.ofMinutes(offsetMinutes)).toEpochMilli(),
            valueMgDl = value
        )

    @Test
    fun identicalReadings_areMergedWithoutConflicts() {
        val plan = BackupMergeEngine.buildPersonPlan(
            patientId = "p1",
            displayName = "Anna",
            existsLocally = true,
            localReadings = listOf(local(0, 100), local(15, 110)),
            backupReadings = listOf(reading(0, 100), reading(15, 110)),
            zoneId = zone
        )

        assertEquals(2, plan.identicalReadings)
        assertTrue(plan.addedReadings.isEmpty())
        assertFalse(plan.hasConflicts)
    }

    @Test
    fun differentDateRange_isMergedAndSummarised() {
        // Local device knows only day 3, archive additionally covers day 1 and day 2.
        val localReadings = (0 until 4).map { local(2880 + it * 15L, 120) }
        val backupReadings = (0 until 4).map { reading(2880 + it * 15L, 120) } +
            (0 until 4).map { reading(it * 15L, 100) } +
            (0 until 4).map { reading(1440 + it * 15L, 110) }

        val plan = BackupMergeEngine.buildPersonPlan(
            patientId = "p1",
            displayName = "Anna",
            existsLocally = true,
            localReadings = localReadings,
            backupReadings = backupReadings,
            zoneId = zone
        )

        assertEquals(8, plan.addedReadings.size)
        assertEquals(4, plan.identicalReadings)
        assertFalse(plan.hasConflicts)
        assertEquals(2, plan.addedDistinctDays)

        val description = BackupMergeEngine.describePerson(plan, zone)
        assertTrue(description.contains("Anna"))
        assertTrue(description.contains("2 dni"))
        assertTrue(description.contains("scalonych"))
    }

    @Test
    fun sameTimestampDifferentValue_producesConflict() {
        val plan = BackupMergeEngine.buildPersonPlan(
            patientId = "p1",
            displayName = "Anna",
            existsLocally = true,
            localReadings = listOf(local(0, 100)),
            backupReadings = listOf(reading(0, 140)),
            zoneId = zone
        )

        assertTrue(plan.hasConflicts)
        assertEquals(1, plan.conflicts.size)
        assertEquals(100, plan.conflicts.single().localValueMgDl)
        assertEquals(140, plan.conflicts.single().backupValueMgDl)

        val text = BackupMergeEngine.describeConflict(plan.conflicts.single(), zone)
        assertTrue(text.contains("w aplikacji 100 mg/dL"))
        assertTrue(text.contains("w archiwum 140 mg/dL"))
    }

    @Test
    fun readingsToWrite_keepLocal_skipsConflictingArchiveRows() {
        val backupReadings = listOf(reading(0, 140), reading(15, 111))
        val plan = BackupMergeEngine.buildPersonPlan(
            patientId = "p1",
            displayName = "Anna",
            existsLocally = true,
            localReadings = listOf(local(0, 100)),
            backupReadings = backupReadings,
            zoneId = zone
        )

        val keepLocal = BackupMergeEngine.readingsToWrite(plan, backupReadings, ConflictResolution.KEEP_LOCAL)
        val keepBackup = BackupMergeEngine.readingsToWrite(plan, backupReadings, ConflictResolution.KEEP_BACKUP)

        assertEquals(1, keepLocal.size)
        assertEquals(111, keepLocal.single().valueMgDl)
        assertEquals(2, keepBackup.size)
        assertTrue(keepBackup.any { it.valueMgDl == 140 })
    }

    @Test
    fun buildPlan_reportsPersonsThatDoNotExistLocally() {
        val bundle = BackupBundle(
            schemaVersion = 3,
            createdAtIso = base.toString(),
            appVersion = "2.4.0",
            persons = listOf(
                BackupPersonDto(
                    patientId = "p2",
                    displayName = "Bartek",
                    lastSeenAtIso = base.toString(),
                    createdAtIso = base.toString(),
                    updatedAtIso = base.toString()
                )
            ),
            readings = listOf(reading(0, 105, "p2"))
        )

        val plan = BackupMergeEngine.buildPlan(
            bundle = bundle,
            localReadings = emptyList(),
            localPatientIds = emptySet(),
            zoneId = zone
        )

        assertEquals(1, plan.persons.size)
        assertFalse(plan.persons.single().existsLocally)
        assertEquals(1, plan.totalAdded)
        assertEquals(0, plan.totalConflicts)
    }

    @Test
    fun parseInstantOrNull_acceptsIsoAndEpochMillis() {
        assertEquals(base, BackupMergeEngine.parseInstantOrNull(base.toString()))
        assertEquals(base, BackupMergeEngine.parseInstantOrNull(base.toEpochMilli().toString()))
        assertEquals(null, BackupMergeEngine.parseInstantOrNull("not-a-date"))
        assertEquals(null, BackupMergeEngine.parseInstantOrNull(null))
    }
}

