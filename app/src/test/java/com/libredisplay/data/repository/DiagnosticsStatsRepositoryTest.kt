package com.libredisplay.data.repository

import android.app.Application
import androidx.room.Room
import com.libredisplay.data.local.GlucoseReadingEntity
import com.libredisplay.data.local.LibreDisplayDatabase
import com.libredisplay.data.local.ObservedPersonEntity
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.storage.SecureStorage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DiagnosticsStatsRepositoryTest {

    private lateinit var db: LibreDisplayDatabase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var secureStorage: SecureStorage
    private lateinit var repository: DiagnosticsStatsRepository

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        settingsRepository = SettingsRepository(context)
        settingsRepository.clearAll()
        secureStorage = SecureStorage(context)

        db = Room.inMemoryDatabaseBuilder(context, LibreDisplayDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = DiagnosticsStatsRepository(
            context = context,
            glucoseReadingDao = db.glucoseReadingDao(),
            observedPersonDao = db.observedPersonDao(),
            syncRunDao = db.syncRunDao(),
            settingsRepository = settingsRepository,
            secureStorage = secureStorage
        )
    }

    @After
    fun tearDown() {
        settingsRepository.clearAll()
        db.close()
    }

    @Test
    fun databaseStats_countsAndTimestamps_areComputed() = runBlocking {
        val now = Instant.parse("2026-08-18T12:00:00Z")
        val older = now.minusSeconds(3600)

        db.observedPersonDao().upsertAll(
            listOf(
                ObservedPersonEntity(
                    patientId = "p1",
                    firstName = "A",
                    lastName = "B",
                    displayName = "A B",
                    isActive = true,
                    lastSeenAt = now,
                    createdAt = now,
                    updatedAt = now
                )
            )
        )

        db.glucoseReadingDao().insertIgnore(
            listOf(
                GlucoseReadingEntity(
                    id = "p1:${older.toEpochMilli()}",
                    patientId = "p1",
                    timestamp = older,
                    valueMgDl = 110,
                    trendArrow = "→",
                    trendLabel = "Stabilnie",
                    source = "LibreLinkUp",
                    sourceAccountId = "acc",
                    receivedAt = older,
                    isValid = true,
                    rawTrendCode = null,
                    createdAt = older
                ),
                GlucoseReadingEntity(
                    id = "p1:${now.toEpochMilli()}",
                    patientId = "p1",
                    timestamp = now,
                    valueMgDl = 120,
                    trendArrow = "↗",
                    trendLabel = "Rosnie",
                    source = "LibreLinkUp",
                    sourceAccountId = "acc",
                    receivedAt = now,
                    isValid = true,
                    rawTrendCode = null,
                    createdAt = now
                )
            )
        )

        val stats = repository.loadDatabaseStats(now)

        assertEquals(2L, stats.readingsCount)
        assertEquals(1, stats.peopleCount)
        assertNotNull(stats.oldestReadingTimestamp)
        assertNotNull(stats.newestReadingTimestamp)
        assertTrue(stats.totalBytes >= 0)
    }

    @Test
    fun retentionEstimate_insufficientData_returnsNullEstimates() = runBlocking {
        val estimate = repository.estimateRetention(24)
        assertTrue(estimate.insufficientData)
        assertNull(estimate.estimatedBytes)
        assertNull(estimate.estimatedReadings)
    }

    @Test
    fun pollingEstimate_insufficientData_isHonest() = runBlocking {
        val estimate = repository.estimatePolling(30)
        assertTrue(estimate.insufficientData)
        assertNull(estimate.currentDailyBytes)
        assertNull(estimate.selectedDailyBytes)
    }

    @Test
    fun networkStats_demoMode_isExcluded() = runBlocking {
        val stats = repository.loadNetworkUsageStats(AppMode.DEMO)
        assertEquals(0L, stats.totalDownloadedBytes)
        assertEquals(0L, stats.totalUploadedBytes)
        assertNull(stats.averageDownloadedPerDayBytes)
    }
}

