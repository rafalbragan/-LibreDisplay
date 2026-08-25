package com.libredisplay.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.libredisplay.data.local.GlucoseReadingEntity
import com.libredisplay.data.local.InMemoryRoomTestDatabase
import com.libredisplay.data.local.LibreDisplayDatabase
import com.libredisplay.data.local.ObservedPersonEntity
import com.libredisplay.data.model.LibreConnectionPerson
import com.libredisplay.testing.scenario.GlucoseScenario
import com.libredisplay.testing.scenario.GlucoseScenarioEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LocalGlucoseHistoryRepositoryTest {

    private lateinit var repository: LocalGlucoseHistoryRepository
    private lateinit var db: com.libredisplay.data.local.LibreDisplayDatabase

    @Before
    fun setUp() {
        db = InMemoryRoomTestDatabase.create()
        repository = LocalGlucoseHistoryRepository(
            observedPersonDao = db.observedPersonDao(),
            glucoseReadingDao = db.glucoseReadingDao(),
            syncRunDao = db.syncRunDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun loadLatestMonitoringSnapshot_includesFull24HourWindow() = runBlocking {
        val latest = Instant.parse("2026-08-20T12:00:00Z")
        val patientId = "patient-a"
        db.observedPersonDao().upsertAll(
            listOf(
                ObservedPersonEntity(
                    patientId = patientId,
                    firstName = "Anna",
                    lastName = "Nowak",
                    displayName = "Anna Nowak",
                    isActive = true,
                    lastSeenAt = latest,
                    createdAt = latest,
                    updatedAt = latest
                )
            )
        )
        db.glucoseReadingDao().insertReplace(
            listOf(
                reading(patientId, latest.minusSeconds(23 * 60 * 60), 101),
                reading(patientId, latest.minusSeconds(13 * 60 * 60), 88),
                reading(patientId, latest.minusSeconds(25 * 60 * 60), 77),
                reading(patientId, latest, 115)
            )
        )

        val snapshot = repository.loadLatestMonitoringSnapshot(patientId)

        assertNotNull(snapshot)
        val history = snapshot!!.reading.history
        assertEquals(3, history.size)
        assertTrue(history.any { it.value == 101 })
        assertTrue(history.any { it.value == 88 })
        assertTrue(history.none { it.value == 77 })
    }

    @Test
    fun deleteReadingsOlderThanHours_respectsRetentionCutoff() = runBlocking {
        val now = Instant.parse("2026-08-20T12:00:00Z")
        val patientId = "patient-a"
        db.glucoseReadingDao().insertReplace(
            listOf(
                reading(patientId, now.minusSeconds(30 * 60 * 60), 90),
                reading(patientId, now.minusSeconds(23 * 60 * 60), 100),
                reading(patientId, now.minusSeconds(2 * 60 * 60), 110)
            )
        )

        val deleted = repository.deleteReadingsOlderThanHours(hours = 24, now = now)
        val remaining = db.glucoseReadingDao().getAllLiveReadings().sortedBy { it.timestamp }

        assertEquals(1, deleted)
        assertEquals(2, remaining.size)
        assertEquals(100, remaining.first().valueMgDl)
        assertEquals(110, remaining.last().valueMgDl)
    }

    @Test
    fun persisted48Hours_remainAvailableAfterDatabaseReopen() = runBlocking {
        val scenario = GlucoseScenarioEngine.dataset(GlucoseScenario.FULL_48H_DATA)
        val dbFile = File.createTempFile("librecare-history-48h-", ".db")
        db.close()

        try {
            var persistentDb = openPersistentDb(dbFile)
            var persistentRepository = LocalGlucoseHistoryRepository(
                observedPersonDao = persistentDb.observedPersonDao(),
                glucoseReadingDao = persistentDb.glucoseReadingDao(),
                syncRunDao = persistentDb.syncRunDao()
            )
            persistentRepository.upsertObservedPersons(
                persons = listOf(LibreConnectionPerson(patientId = "patient-a", displayName = "Anna Nowak", firstName = "Anna", lastName = "Nowak")),
                now = scenario.now
            )
            persistentRepository.insertReadings("patient-a", scenario.points, now = scenario.now)
            persistentDb.close()

            persistentDb = openPersistentDb(dbFile)
            val reopenedDao = persistentDb.glucoseReadingDao()
            val reopenedPoints = reopenedDao.getRangeByPatient("patient-a", scenario.oldestTimestamp!!, scenario.newestTimestamp!!)
            val oldest = reopenedDao.oldestLiveReadingTimestamp()
            val newest = reopenedDao.newestLiveReadingTimestamp()
            val span = Duration.between(oldest, newest)

            assertEquals(scenario.points.size, reopenedPoints.size)
            assertEquals(scenario.oldestTimestamp, oldest)
            assertEquals(scenario.newestTimestamp, newest)
            assertTrue("span=$span", span > Duration.ofHours(24))

            persistentDb.close()
        } finally {
            dbFile.delete()
        }
    }

    @Test
    fun multiProfileData_survivesReopenAndCleanupKeepsBothProfiles() = runBlocking {
        val patientA = GlucoseScenarioEngine.dataset(GlucoseScenario.FULL_48H_DATA)
        val patientB = GlucoseScenarioEngine.dataset(GlucoseScenario.FULL_24H_DATA)
        val dbFile = File.createTempFile("librecare-history-multi-", ".db")
        db.close()

        try {
            var persistentDb = openPersistentDb(dbFile)
            var persistentRepository = LocalGlucoseHistoryRepository(
                observedPersonDao = persistentDb.observedPersonDao(),
                glucoseReadingDao = persistentDb.glucoseReadingDao(),
                syncRunDao = persistentDb.syncRunDao()
            )
            persistentRepository.upsertObservedPersons(
                persons = listOf(
                    LibreConnectionPerson(patientId = "patient-a", displayName = "Anna Nowak", firstName = "Anna", lastName = "Nowak"),
                    LibreConnectionPerson(patientId = "patient-b", displayName = "Bartek Kowalski", firstName = "Bartek", lastName = "Kowalski")
                ),
                now = patientA.now
            )
            persistentRepository.insertReadings("patient-a", patientA.points, now = patientA.now)
            persistentRepository.insertReadings("patient-b", patientB.points, now = patientB.now)
            persistentDb.close()

            persistentDb = openPersistentDb(dbFile)
            persistentRepository = LocalGlucoseHistoryRepository(
                observedPersonDao = persistentDb.observedPersonDao(),
                glucoseReadingDao = persistentDb.glucoseReadingDao(),
                syncRunDao = persistentDb.syncRunDao()
            )
            val deleted = persistentRepository.deleteReadingsOlderThanHours(hours = 24, now = patientA.now)
            val remainingA = persistentDb.glucoseReadingDao().getRangeByPatient("patient-a", patientA.now.minus(Duration.ofDays(3)), patientA.now)
            val remainingB = persistentDb.glucoseReadingDao().getRangeByPatient("patient-b", patientB.now.minus(Duration.ofDays(3)), patientB.now)

            assertTrue(deleted > 0)
            assertTrue(remainingA.isNotEmpty())
            assertTrue(remainingB.isNotEmpty())
            assertTrue(Duration.between(remainingA.first().timestamp, remainingA.last().timestamp) >= Duration.ofHours(23).plusMinutes(55))
            assertTrue(Duration.between(remainingB.first().timestamp, remainingB.last().timestamp) >= Duration.ofHours(23).plusMinutes(55))

            persistentDb.close()
        } finally {
            dbFile.delete()
        }
    }

    @Test
    fun loadStoredRange_reportsFullSpanEvenWithGapsAndBeyondLoadedWindow() = runBlocking {
        val newest = Instant.parse("2026-08-20T12:00:00Z")
        val oldest = newest.minus(Duration.ofDays(5))
        val patientId = "patient-a"
        db.glucoseReadingDao().insertReplace(
            listOf(
                reading(patientId, oldest, 90),
                // Large gap in the middle - the span must still be measured end-to-end.
                reading(patientId, newest.minus(Duration.ofDays(2)), 120),
                reading(patientId, newest, 110)
            )
        )

        val range = repository.loadStoredRange(patientId)

        assertNotNull(range)
        assertEquals(oldest, range!!.oldest)
        assertEquals(newest, range.newest)
        assertEquals(Duration.ofDays(5), Duration.between(range.oldest, range.newest))
    }

    @Test
    fun loadStoredRange_withoutData_isNull() = runBlocking {
        assertEquals(null, repository.loadStoredRange("patient-missing"))
    }

    private fun openPersistentDb(dbFile: File): LibreDisplayDatabase {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return Room.databaseBuilder(context, LibreDisplayDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
    }

    private fun reading(patientId: String, timestamp: Instant, value: Int): GlucoseReadingEntity {
        return GlucoseReadingEntity(
            id = "$patientId:${timestamp.toEpochMilli()}",
            patientId = patientId,
            timestamp = timestamp,
            valueMgDl = value,
            trendArrow = "→",
            trendLabel = "Stable",
            source = "LibreLinkUp",
            sourceAccountId = null,
            receivedAt = timestamp,
            isValid = true,
            rawTrendCode = null,
            createdAt = timestamp
        )
    }
}


