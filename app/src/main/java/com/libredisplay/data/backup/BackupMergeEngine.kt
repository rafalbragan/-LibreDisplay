package com.libredisplay.data.backup

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Minimal projection of a locally stored reading, used for merge planning. */
data class LocalReadingKey(
    val patientId: String,
    val epochMillis: Long,
    val valueMgDl: Int
)

/** A single timestamp that exists both locally and in the archive but with a different value. */
data class ReadingConflict(
    val epochMillis: Long,
    val localValueMgDl: Int,
    val backupValueMgDl: Int
)

/** What should happen with readings that differ between the archive and the device. */
enum class ConflictResolution {
    /** Keep the values currently stored on the device. */
    KEEP_LOCAL,

    /** Overwrite with the values from the archive. */
    KEEP_BACKUP
}

data class PersonMergePlan(
    val patientId: String,
    val displayName: String,
    val existsLocally: Boolean,
    val addedReadings: List<BackupReadingDto>,
    val identicalReadings: Int,
    val conflicts: List<ReadingConflict>,
    val addedRangeStart: Instant?,
    val addedRangeEnd: Instant?,
    val addedDistinctDays: Int
) {
    val hasConflicts: Boolean get() = conflicts.isNotEmpty()
    val hasAnythingToRestore: Boolean get() = addedReadings.isNotEmpty() || conflicts.isNotEmpty() || !existsLocally
}

data class RestorePlan(
    val persons: List<PersonMergePlan>,
    val settingsAvailable: Boolean,
    val createdAtIso: String,
    val appVersion: String,
    val schemaVersion: Int
) {
    val hasConflicts: Boolean get() = persons.any { it.hasConflicts }
    val totalAdded: Int get() = persons.sumOf { it.addedReadings.size }
    val totalConflicts: Int get() = persons.sumOf { it.conflicts.size }
}

/**
 * Pure merge planning used by restore.
 *
 * Rules requested by the product owner:
 *  - readings with the same timestamp AND the same value are merged silently,
 *  - readings that only exist in the archive extend the local history and are reported
 *    ("dla X wczytano 3 dni między A a B"),
 *  - readings with the same timestamp but a DIFFERENT value are reported as conflicts so that
 *    the user can decide whether the current or the archived values win.
 */
object BackupMergeEngine {

    fun buildPlan(
        bundle: BackupBundle,
        localReadings: List<LocalReadingKey>,
        localPatientIds: Set<String>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): RestorePlan {
        val localByPatient = localReadings.groupBy { it.patientId }
        val backupByPatient = bundle.readingsByPatient
        val personNames = bundle.persons.associate { it.patientId to it.displayName }
        val patientIds = (bundle.persons.map { it.patientId } + backupByPatient.keys).distinct()

        val plans = patientIds.map { patientId ->
            buildPersonPlan(
                patientId = patientId,
                displayName = personNames[patientId] ?: patientId,
                existsLocally = patientId in localPatientIds,
                localReadings = localByPatient[patientId].orEmpty(),
                backupReadings = backupByPatient[patientId].orEmpty(),
                zoneId = zoneId
            )
        }
        return RestorePlan(
            persons = plans,
            settingsAvailable = bundle.settings != null,
            createdAtIso = bundle.createdAtIso,
            appVersion = bundle.appVersion,
            schemaVersion = bundle.schemaVersion
        )
    }

    fun buildPersonPlan(
        patientId: String,
        displayName: String,
        existsLocally: Boolean,
        localReadings: List<LocalReadingKey>,
        backupReadings: List<BackupReadingDto>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PersonMergePlan {
        val localByTimestamp = localReadings.associateBy({ it.epochMillis }, { it.valueMgDl })
        val added = mutableListOf<BackupReadingDto>()
        val conflicts = mutableListOf<ReadingConflict>()
        var identical = 0

        backupReadings.forEach { reading ->
            val epochMillis = parseInstantOrNull(reading.timestampIso)?.toEpochMilli() ?: return@forEach
            val localValue = localByTimestamp[epochMillis]
            when {
                localValue == null -> added += reading
                localValue == reading.valueMgDl -> identical++
                else -> conflicts += ReadingConflict(
                    epochMillis = epochMillis,
                    localValueMgDl = localValue,
                    backupValueMgDl = reading.valueMgDl
                )
            }
        }

        val addedInstants = added.mapNotNull { parseInstantOrNull(it.timestampIso) }.sorted()
        val distinctDays = addedInstants.map { it.atZone(zoneId).toLocalDate() }.distinct().size

        return PersonMergePlan(
            patientId = patientId,
            displayName = displayName,
            existsLocally = existsLocally,
            addedReadings = added,
            identicalReadings = identical,
            conflicts = conflicts.sortedBy { it.epochMillis },
            addedRangeStart = addedInstants.firstOrNull(),
            addedRangeEnd = addedInstants.lastOrNull(),
            addedDistinctDays = distinctDays
        )
    }

    /**
     * Decides which readings must actually be written to the database once the user resolved
     * the conflicts.
     */
    fun readingsToWrite(
        plan: PersonMergePlan,
        backupReadings: List<BackupReadingDto>,
        resolution: ConflictResolution
    ): List<BackupReadingDto> {
        if (resolution == ConflictResolution.KEEP_BACKUP) {
            val conflictMillis = plan.conflicts.map { it.epochMillis }.toSet()
            val conflicting = backupReadings.filter {
                parseInstantOrNull(it.timestampIso)?.toEpochMilli() in conflictMillis
            }
            return plan.addedReadings + conflicting
        }
        return plan.addedReadings
    }

    // ------------------------------------------------------------------ reporting

    fun describePerson(plan: PersonMergePlan, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (plan.addedReadings.isEmpty() && plan.conflicts.isEmpty()) {
            return "${plan.displayName}: brak nowych danych do wczytania (${plan.identicalReadings} identycznych odczytów)."
        }
        val parts = mutableListOf<String>()
        if (plan.addedReadings.isNotEmpty()) {
            val start = plan.addedRangeStart
            val end = plan.addedRangeEnd
            val dayLabel = polishDays(plan.addedDistinctDays)
            parts += if (start != null && end != null) {
                "wczytano $dayLabel (${plan.addedReadings.size} odczytów) między ${formatDate(start, zoneId)} a ${formatDate(end, zoneId)}"
            } else {
                "wczytano ${plan.addedReadings.size} odczytów"
            }
        }
        if (plan.identicalReadings > 0) {
            parts += "${plan.identicalReadings} odczytów było identycznych i zostało scalonych"
        }
        if (plan.conflicts.isNotEmpty()) {
            parts += "${plan.conflicts.size} odczytów różni się wartością"
        }
        return "${plan.displayName}: ${parts.joinToString(", ")}."
    }

    fun describePlan(plan: RestorePlan, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (plan.persons.isEmpty()) return "Kopia nie zawiera danych osób."
        return plan.persons.joinToString(separator = "\n") { describePerson(it, zoneId) }
    }

    fun describeConflict(conflict: ReadingConflict, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val instant = Instant.ofEpochMilli(conflict.epochMillis)
        return "${formatDateTime(instant, zoneId)}: w aplikacji ${conflict.localValueMgDl} mg/dL, w archiwum ${conflict.backupValueMgDl} mg/dL"
    }

    // ------------------------------------------------------------------ helpers

    internal fun parseInstantOrNull(text: String?): Instant? {
        if (text.isNullOrBlank()) return null
        runCatching { return Instant.parse(text) }
        runCatching { return Instant.ofEpochMilli(text.toLong()) }
        return null
    }

    private fun polishDays(days: Int): String = when {
        days <= 0 -> "dane"
        days == 1 -> "1 dzień"
        days in 2..4 -> "$days dni"
        else -> "$days dni"
    }

    private fun formatDate(instant: Instant, zoneId: ZoneId): String =
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("pl")).withZone(zoneId).format(instant)

    private fun formatDateTime(instant: Instant, zoneId: ZoneId): String =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale("pl")).withZone(zoneId).format(instant)

    internal fun spanOf(plan: PersonMergePlan): Duration {
        val start = plan.addedRangeStart ?: return Duration.ZERO
        val end = plan.addedRangeEnd ?: return Duration.ZERO
        return Duration.between(start, end).let { if (it.isNegative) Duration.ZERO else it }
    }
}

