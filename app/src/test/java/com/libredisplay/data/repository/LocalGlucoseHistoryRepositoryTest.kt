package com.libredisplay.data.repository

import android.app.Application
import com.libredisplay.data.local.GlucoseReadingEntity
import com.libredisplay.data.local.InMemoryRoomTestDatabase
import com.libredisplay.data.local.ObservedPersonEntity
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


