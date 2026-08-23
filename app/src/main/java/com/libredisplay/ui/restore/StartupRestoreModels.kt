package com.libredisplay.ui.restore

import com.libredisplay.data.backup.BackupCoverageCalculator
import com.libredisplay.data.backup.BackupMergeEngine
import com.libredisplay.data.backup.RestorePlan
import com.libredisplay.data.repository.AppDataBackupRepository.BackupOffer
import java.time.ZoneId

/** One step of the "load my data" conversation shown right after LibreCare starts. */
sealed interface StartupRestoreStep {

    /** Nothing to ask about. */
    data object Hidden : StartupRestoreStep

    /** "Mam zapisane dane tych osób za taki okres - wczytać je?" */
    data class OfferLocalData(val offer: BackupOffer) : StartupRestoreStep

    /** A blocking operation is running. */
    data class Working(val label: String) : StartupRestoreStep

    /** Reading-by-reading comparison found real differences - the user must choose. */
    data class ResolveConflicts(val summary: ConflictSummary) : StartupRestoreStep

    /** What was actually merged. */
    data class ShowSummary(val report: String) : StartupRestoreStep

    /** "Czy chcesz dodatkowo wczytać plik z danymi?" - the picker only opens after a yes. */
    data object AskForFile : StartupRestoreStep

    data class Failure(val message: String) : StartupRestoreStep
}

/** Everything the user needs in order to pick between the device data and the archive. */
data class ConflictSummary(
    val totalConflicts: Int,
    val personLines: List<String>,
    val examples: List<String>
) {
    val hasConflicts: Boolean get() = totalConflicts > 0
}

/** Pure formatting helpers, kept out of Compose so they can be unit tested. */
object StartupRestoreFormatter {

    const val MAX_CONFLICT_EXAMPLES = 5

    /** One readable line per monitored person, e.g. "Anna · 18.08.2026 – 24.08.2026 · …". */
    fun offerLines(offer: BackupOffer, zoneId: ZoneId = ZoneId.systemDefault()): List<String> =
        offer.persons.map { coverage ->
            "${coverage.displayName} · ${BackupCoverageCalculator.describe(coverage, zoneId)}"
        }

    fun offerHeadline(offer: BackupOffer): String = when {
        !offer.exists -> "Nie znaleziono zapisanych danych na tym urządzeniu."
        offer.persons.isEmpty() -> "Zapisany plik danych nie zawiera żadnych osób."
        else -> {
            val people = offer.persons.size
            val readings = offer.totalReadings
            "Mam zapisane dane ${polishPeople(people)} (łącznie $readings odczytów). Wczytać je teraz?"
        }
    }

    fun conflictSummary(
        plan: RestorePlan,
        zoneId: ZoneId = ZoneId.systemDefault(),
        maxExamples: Int = MAX_CONFLICT_EXAMPLES
    ): ConflictSummary {
        val conflictingPersons = plan.persons.filter { it.hasConflicts }
        val personLines = conflictingPersons.map { person ->
            "${person.displayName}: ${person.conflicts.size} odczytów różni się wartością " +
                "(${person.identicalReadings} identycznych, ${person.addedReadings.size} nowych)"
        }
        val examples = conflictingPersons
            .flatMap { person -> person.conflicts.map { person.displayName to it } }
            .sortedBy { it.second.epochMillis }
            .take(maxExamples.coerceAtLeast(0))
            .map { (name, conflict) -> "$name – ${BackupMergeEngine.describeConflict(conflict, zoneId)}" }
        return ConflictSummary(
            totalConflicts = plan.totalConflicts,
            personLines = personLines,
            examples = examples
        )
    }

    fun polishPeople(count: Int): String = when {
        count <= 0 -> "0 osób"
        count == 1 -> "1 osoby"
        count in 2..4 -> "$count osób"
        else -> "$count osób"
    }
}

