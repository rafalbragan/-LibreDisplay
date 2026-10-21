package com.libredisplay.data.importer

import androidx.room.withTransaction
import com.google.gson.JsonObject
import com.libredisplay.data.local.GlucoseReadingEntity
import com.libredisplay.data.local.LibreDisplayDatabase
import com.libredisplay.data.local.ObservedPersonEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

data class ImportTarget(val id: String, val name: String, val create: Boolean = false)
data class ImportCounts(val duplicates: Int, val conflicts: Int, val newRecords: Int)
class ImportPreview internal constructor(internal val snapshot: File, val sha256: String,
    val target: ImportTarget, val options: CsvTimeOptions, val summary: CsvSummary, val counts: ImportCounts) : AutoCloseable {
    override fun close() { snapshot.delete() }
}
data class ImportResult(val inserted: Int, val duplicates: Int, val conflicts: Int)

/** A private immutable snapshot separates SAF access from the confirmation/transaction boundary. */
class LibreViewImportRepository(private val database: LibreDisplayDatabase, private val cache: File,
    private val demoMode: () -> Boolean = { false }) {
    suspend fun targets(): List<ImportTarget> = withContext(Dispatchers.IO) {
        val persons = if (demoMode()) database.observedPersonDao().getAllDemoPersons()
            else database.observedPersonDao().getAllLivePersons()
        persons.map { ImportTarget(it.patientId, it.displayName) }
    }

    fun newProfile(name: String): ImportTarget {
        require(name.trim().length in 1..100) { "Wpisz nazwę profilu (1–100 znaków)." }
        val prefix = if (demoMode()) "demo-person-import-" else "local-csv-"
        return ImportTarget(prefix + UUID.randomUUID(), name.trim(), create = true)
    }

    suspend fun preview(open: () -> InputStream, target: ImportTarget, options: CsvTimeOptions): ImportPreview = withContext(Dispatchers.IO) {
        validateTarget(target)
        cache.mkdirs()
        val snapshot = File.createTempFile("libreview-", ".csv", cache)
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            open().use { input -> snapshot.outputStream().use { output ->
                val buffer = ByteArray(32 * 1024)
                var size = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    size += count
                    require(size <= 128L * 1024 * 1024) { "Plik przekracza limit 128 MB." }
                    digest.update(buffer, 0, count); output.write(buffer, 0, count)
                }
            } }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            val evaluated = evaluate(snapshot, hash, target, options, write = false)
            ImportPreview(snapshot, hash, target, options, evaluated.first, evaluated.second)
        } catch (error: Throwable) {
            snapshot.delete(); throw error
        }
    }

    suspend fun confirm(preview: ImportPreview, confirmedName: String,
        afterBatch: suspend (Int) -> Unit = {}): ImportResult = withContext(Dispatchers.IO) {
        require(confirmedName.trim() == preview.target.name) { "Nazwa pacjenta nie została potwierdzona." }
        validateTarget(preview.target)
        require(preview.snapshot.isFile) { "Podgląd wygasł. Wybierz plik ponownie." }
        // Hash-check immediately before parsing. No external URI is reopened after confirmation.
        val digest = MessageDigest.getInstance("SHA-256")
        preview.snapshot.inputStream().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer); if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        require(digest.digest().joinToString("") { "%02x".format(it) } == preview.sha256) { "Plik podglądu zmienił się. Utwórz nowy podgląd." }
        database.withTransaction {
            validateTarget(preview.target)
            val target = preview.target
            if (target.create && database.observedPersonDao().getByPatientId(target.id) == null) {
                val now = Instant.now()
                database.observedPersonDao().upsertAll(listOf(ObservedPersonEntity(patientId = target.id,
                    firstName = null, lastName = null, displayName = target.name, isActive = false,
                    lastSeenAt = now, createdAt = now, updatedAt = now)))
            }
            val actual = evaluate(preview.snapshot, preview.sha256, target, preview.options, write = true, afterBatch = afterBatch)
            check(actual.first == preview.summary && actual.second == preview.counts) {
                "Historia zmieniła się od podglądu. Nic nie zapisano. Utwórz nowy podgląd."
            }
            currentCoroutineContext().ensureActive()
            ImportResult(actual.second.newRecords, actual.second.duplicates, actual.second.conflicts)
        }
    }

    private suspend fun validateTarget(target: ImportTarget) {
        require(target.id.startsWith("demo-person-") == demoMode()) { "Tryb danych zmienił się. Wybierz profil ponownie." }
        if (!target.create) {
            val actual = database.observedPersonDao().getByPatientId(target.id)
            require(actual != null && actual.displayName == target.name) { "Profil zmienił się. Wybierz go ponownie." }
        } else {
            require(target.id.startsWith(if (demoMode()) "demo-person-import-" else "local-csv-")) { "Nieprawidłowy profil lokalny." }
        }
    }

    private suspend fun evaluate(file: File, hash: String, target: ImportTarget, options: CsvTimeOptions,
        write: Boolean, afterBatch: suspend (Int) -> Unit = {}): Pair<CsvSummary, ImportCounts> {
        val seen = HashMap<Long, Int>()
        val batch = ArrayList<CsvMeasurement>(400)
        var duplicates = 0; var conflicts = 0; var added = 0
        suspend fun flush() {
            if (batch.isEmpty()) return
            currentCoroutineContext().ensureActive()
            val existing = database.glucoseReadingDao().atTimestamps(target.id, batch.map { it.timestamp })
                .associateBy { it.timestamp.toEpochMilli() }
            val now = Instant.now()
            val newRows = batch.mapNotNull { row ->
                val old = existing[row.timestamp.toEpochMilli()]
                when {
                    old != null && old.valueMgDl == row.value -> { duplicates++; null }
                    old != null -> { conflicts++; null }
                    else -> GlucoseReadingEntity(id = "${target.id}:${row.timestamp.toEpochMilli()}", patientId = target.id,
                        timestamp = row.timestamp, valueMgDl = row.value, trendArrow = null, trendLabel = null,
                        source = if (row.type == 0) "LibreViewCSV" else "LibreViewScan", receivedAt = now, createdAt = now,
                        importProvenance = JsonObject().apply {
                            addProperty("fileSha256", hash); addProperty("record", row.record); addProperty("type", row.type)
                            addProperty("deviceLocalTimestamp", row.localTimestamp); addProperty("zoneId", options.zone.id)
                            addProperty("offset", row.offset); addProperty("ambiguousPolicy", options.ambiguous.name)
                        }.toString())
                }
            }
            if (write) {
                val inserted = database.glucoseReadingDao().insertIgnore(newRows).count { it != -1L }
                check(inserted == newRows.size) { "Konflikt zapisu. Nic nie zapisano. Utwórz nowy podgląd." }
                added += inserted
                afterBatch(added)
            } else added += newRows.size
            batch.clear()
        }
        val summary = file.inputStream().use { input -> LibreViewCsvParser().parse(input, options) { row ->
            val previous = seen.putIfAbsent(row.timestamp.toEpochMilli(), row.value)
            when {
                previous == row.value -> duplicates++
                previous != null -> conflicts++
                else -> batch.add(row)
            }
            if (batch.size >= 400) flush()
        } }
        flush()
        return summary to ImportCounts(duplicates, conflicts, added)
    }
}



