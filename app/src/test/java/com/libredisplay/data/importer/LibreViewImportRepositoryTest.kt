package com.libredisplay.data.importer

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.libredisplay.data.backup.BackupBundle
import com.libredisplay.data.backup.BackupCodec
import com.libredisplay.data.backup.BackupReadingDto
import com.libredisplay.data.local.GlucoseReadingEntity
import com.libredisplay.data.local.LibreDisplayDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class LibreViewImportRepositoryTest {
    private lateinit var db: LibreDisplayDatabase
    private lateinit var repo: LibreViewImportRepository
    private lateinit var folder: File
    private val options = CsvTimeOptions(ZoneId.of("Europe/Warsaw"))
    private val header = "Dane glukozy,Profil DEMO\r\nUrządzenie,Numer seryjny,Znacznik czasu urządzenia,Typ zapisu,Historyczne stężenie glukozy mg/dL,Zeskanowane stężenie glukozy mg/dL\r\n"
    private fun file(vararg values: Int) = header + values.mapIndexed { i, v -> "Libre,synthetic,01-05-2020 10:${i.toString().padStart(2, '0')},0,$v," }.joinToString("\r\n")
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, LibreDisplayDatabase::class.java).allowMainThreadQueries().build()
        folder = File(context.cacheDir, "import-tests").apply { mkdirs() }
        repo = LibreViewImportRepository(db, folder)
    }
    @After fun close() { db.close(); folder.deleteRecursively() }
    @Test fun previewHasNoWritesConfirmIsIdempotentAndPreservesProvenance() = runBlocking {
        val target = repo.newProfile("Profil DEMO")
        repo.preview({ file(100, 120).byteInputStream() }, target, options).use { preview ->
            assertEquals(2, preview.counts.newRecords); assertEquals(0, db.glucoseReadingDao().countAllReadings())
            assertTrue(db.observedPersonDao().getAllLivePersons().isEmpty())
            assertEquals(2, repo.confirm(preview, target.name).inserted)
        }
        repo.preview({ file(100, 120).byteInputStream() }, target.copy(create = false), options).use { preview ->
            assertEquals(2, preview.counts.duplicates); assertEquals(0, repo.confirm(preview, target.name).inserted)
        }
        val rows = db.glucoseReadingDao().getAllLiveReadings()
        assertEquals(2, rows.size); assertEquals("LibreViewCSV", rows.first().source)
        assertTrue(rows.first().importProvenance!!.contains("Europe/Warsaw"))
        assertTrue(rows.first().importProvenance!!.contains("fileSha256"))
    }
    @Test fun conflictsNeverOverwriteExistingValuesAndPeopleRemainIsolated() = runBlocking {
        val a = repo.newProfile("A DEMO"); val b = repo.newProfile("B DEMO")
        repo.preview({ file(100).byteInputStream() }, a, options).use { repo.confirm(it, a.name) }
        repo.preview({ file(200, 130).byteInputStream() }, a.copy(create = false), options).use {
            assertEquals(1, it.counts.conflicts); assertEquals(1, repo.confirm(it, a.name).inserted)
        }
        repo.preview({ file(250).byteInputStream() }, b, options).use { repo.confirm(it, b.name) }
        assertEquals(listOf(100, 130), db.glucoseReadingDao().getRangeByPatient(a.id, Instant.EPOCH, Instant.now()).map { it.valueMgDl })
        assertEquals(250, db.glucoseReadingDao().getLatestByPatient(b.id)!!.valueMgDl)
    }
    @Test fun duplicatesAndConflictsWithinFileAreCounted() = runBlocking {
        val row = "Libre,synthetic,01-05-2020 10:00,0,100,"
        val text = header + row + "\r\n" + row + "\r\n" + row.replace("100,", "200,")
        repo.preview({ text.byteInputStream() }, repo.newProfile("DEMO"), options).use {
            assertEquals(ImportCounts(1, 1, 1), it.counts)
        }
    }
    @Test fun cancellationAfterBatchRollsBackRowsAndNewProfile() = runBlocking {
        val text = header + (0 until 900).joinToString("\r\n") {
            val local = java.time.LocalDateTime.of(2020, 5, 1, 0, 0).plusMinutes(it.toLong())
            "Libre,synthetic,${local.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))},0,100,"
        }
        val target = repo.newProfile("DEMO")
        repo.preview({ text.byteInputStream() }, target, options).use {
            try { repo.confirm(it, target.name) { throw CancellationException("synthetic") }; fail() }
            catch (_: CancellationException) { }
        }
        assertEquals(0L, db.glucoseReadingDao().countAllReadings())
        assertNull(db.observedPersonDao().getByPatientId(target.id))
    }
    @Test fun changedDatabaseInvalidatesPreviewWithoutPartialWrite() = runBlocking {
        val target = repo.newProfile("DEMO")
        repo.preview({ file(100, 120).byteInputStream() }, target, options).use { preview ->
            repo.preview({ file(100).byteInputStream() }, target, options).use { repo.confirm(it, target.name) }
            try { repo.confirm(preview, target.name); fail() } catch (_: IllegalStateException) { }
        }
        assertEquals(1L, db.glucoseReadingDao().countAllReadings())
    }
    @Test fun retentionDeletesLiveRowsButNotImportsAndExplicitDeleteStillWorks() = runBlocking {
        val target = repo.newProfile("DEMO")
        repo.preview({ file(100, 120).byteInputStream() }, target, options).use { repo.confirm(it, target.name) }
        val now = Instant.parse("2020-01-01T00:00:00Z")
        db.glucoseReadingDao().insertIgnore(listOf(GlucoseReadingEntity("live:0", target.id, now, 110, null, null,
            receivedAt = now, createdAt = now)))
        assertEquals(1, db.glucoseReadingDao().deleteOlderThan(Instant.now()))
        assertEquals(2L, db.glucoseReadingDao().countAllReadings())
        assertEquals(2, db.glucoseReadingDao().deleteReadingsForPerson(target.id))
    }
    @Test fun wrongNameAndChangedFileCannotWrite() = runBlocking {
        val target = repo.newProfile("DEMO")
        repo.preview({ file(100).byteInputStream() }, target, options).use {
            try { repo.confirm(it, "wrong"); fail() } catch (_: IllegalArgumentException) { }
            it.snapshot.appendText("changed")
            try { repo.confirm(it, target.name); fail() } catch (_: IllegalArgumentException) { }
        }
        assertEquals(0L, db.glucoseReadingDao().countAllReadings())
    }
    @Test fun demoCannotBeImportedIntoLiveProfile() = runBlocking {
        val target = ImportTarget("demo-person-1", "DEMO", true)
        try { repo.preview({ file(100).byteInputStream() }, target, options); fail() } catch (_: IllegalArgumentException) { }
    }
    @Test fun chartRangeQueriesAreBoundedAndKeepExtremesAndExactSelection() = runBlocking {
        val start = Instant.parse("2020-05-01T00:00:00Z")
        (0 until 10_000).chunked(400).forEach { chunk ->
            db.glucoseReadingDao().insertIgnore(chunk.map { index ->
                val time = start.plusSeconds(index * 60L)
                GlucoseReadingEntity("chart:$index", "chart-demo", time, if (index == 9999) 500 else 100,
                    null, null, receivedAt = time, createdAt = time)
            })
        }
        val end = start.plusSeconds(10_000 * 60L)
        val buckets = db.glucoseReadingDao().chartBuckets("chart-demo", start.toEpochMilli(), end.toEpochMilli(),
            (end.toEpochMilli() - start.toEpochMilli()) / 800 + 1)
        assertTrue(buckets.size <= 801)
        assertEquals(10_000, buckets.sumOf { it.count })
        assertEquals(500, buckets.maxOf { it.maximum })
        assertEquals(500, db.glucoseReadingDao().nearestInViewport("chart-demo", start.toEpochMilli(), end.toEpochMilli(), end.toEpochMilli())!!.valueMgDl)
        assertTrue(db.glucoseReadingDao().chartBuckets("other-person", start.toEpochMilli(), end.toEpochMilli(), 60_000L).isEmpty())
    }
    @Test fun backupRoundTripPreservesProvenanceAndScanSource() {
        val row = BackupReadingDto("id", "local-csv-demo", "2020-05-01T00:00:00Z", 110,
            source = "LibreViewScan", receivedAtIso = "2026-10-19T00:00:00Z", createdAtIso = "2026-10-19T00:00:00Z",
            importProvenance = "{\"record\":3,\"zoneId\":\"Europe/Warsaw\"}")
        val bundle = BackupBundle(3, "2026-10-19T00:00:00Z", "2.17.0", readings = listOf(row))
        assertEquals(row, BackupCodec.decode(BackupCodec.encode(bundle)).readings.single())
    }
}


