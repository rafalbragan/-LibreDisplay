package com.libredisplay.data.repository

import android.app.Application
import android.net.Uri
import androidx.room.Room
import com.libredisplay.data.local.GlucoseReadingEntity
import com.libredisplay.data.local.LibreDisplayDatabase
import com.libredisplay.data.local.ObservedPersonEntity
import com.libredisplay.data.local.PatientSettingsEntity
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppDataBackupRepositoryTest {

    private lateinit var db: LibreDisplayDatabase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var backupRepository: AppDataBackupRepository
    private lateinit var tmpFile: File

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        settingsRepository = SettingsRepository(context)
        settingsRepository.clearAll()

        db = Room.inMemoryDatabaseBuilder(context, LibreDisplayDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        backupRepository = AppDataBackupRepository(
            context = context,
            database = db,
            settingsRepository = settingsRepository,
            observedPersonDao = db.observedPersonDao(),
            glucoseReadingDao = db.glucoseReadingDao(),
            patientSettingsDao = db.patientSettingsDao()
        )
        tmpFile = File.createTempFile("librecare-backup-", ".json")
        seedData()
    }

    @After
    fun tearDown() {
        settingsRepository.clearAll()
        db.close()
        tmpFile.delete()
    }

    @Test
    fun exportBackup_excludesDemoRowsAndKeepsLiveSettings() = runBlocking {
        backupRepository.exportToUri(Uri.fromFile(tmpFile))

        val text = tmpFile.readText(Charsets.UTF_8)
        assertTrue(text.contains("real-person-a"))
        assertFalse(text.contains("demo-person-anna"))
        assertFalse(text.contains("\"source\": \"DemoMode\""))
        assertTrue(text.contains("\"refreshInterval\": 120"))
    }

    @Test
    fun restoreBackup_filtersDemoContentAndPreventsDemoMode() = runBlocking {
        val payload = """
            {
              "schemaVersion": 1,
              "generatedAtEpochMillis": 1,
              "appVersion": "test",
              "settings": {
                "email": "user@example.com",
                "password": "secret",
                "selectedPatientId": "demo-person-anna",
                "region": "EU",
                "regionMode": "EU",
                "customBaseUrl": "",
                "refreshInterval": 120,
                "targetLow": 80,
                "targetHigh": 180,
                "trendWindowMinutes": 3,
                "showStatistics": true,
                "kioskMode": false,
                "appMode": "DEMO",
                "useAuthV3": true,
                "retentionHours": 720,
                "backgroundPollingMinutes": 30
              },
              "quickMetricOrder": ["below", "in_range", "above", "gmi", "hba1c"],
              "persistedSession": null,
              "livePersons": [
                {
                  "patientId": "real-person-restored",
                  "firstName": "Real",
                  "lastName": "User",
                  "displayName": "Real User",
                  "connectionId": null,
                  "isActive": true,
                  "lastSeenAtIso": "2026-08-20T10:00:00Z",
                  "createdAtIso": "2026-08-20T10:00:00Z",
                  "updatedAtIso": "2026-08-20T10:00:00Z"
                },
                {
                  "patientId": "demo-person-anna",
                  "firstName": "Demo",
                  "lastName": "User",
                  "displayName": "Demo User",
                  "connectionId": null,
                  "isActive": true,
                  "lastSeenAtIso": "2026-08-20T10:00:00Z",
                  "createdAtIso": "2026-08-20T10:00:00Z",
                  "updatedAtIso": "2026-08-20T10:00:00Z"
                }
              ],
              "liveReadings": [
                {
                  "id": "real-person-restored:1",
                  "patientId": "real-person-restored",
                  "timestampIso": "2026-08-20T10:00:00Z",
                  "valueMgDl": 111,
                  "trendArrow": "->",
                  "trendLabel": "Stable",
                  "source": "LibreLinkUp",
                  "sourceAccountId": "acc-1",
                  "receivedAtIso": "2026-08-20T10:00:00Z",
                  "isValid": true,
                  "rawTrendCode": null,
                  "createdAtIso": "2026-08-20T10:00:00Z"
                },
                {
                  "id": "demo-person-anna:1",
                  "patientId": "demo-person-anna",
                  "timestampIso": "2026-08-20T10:00:00Z",
                  "valueMgDl": 140,
                  "trendArrow": "->",
                  "trendLabel": "Stable",
                  "source": "DemoMode",
                  "sourceAccountId": null,
                  "receivedAtIso": "2026-08-20T10:00:00Z",
                  "isValid": true,
                  "rawTrendCode": null,
                  "createdAtIso": "2026-08-20T10:00:00Z"
                }
              ],
              "livePatientSettings": [
                {
                  "patientId": "real-person-restored",
                  "lowCriticalMgDl": 54,
                  "lowMgDl": 70,
                  "targetLowMgDl": 80,
                  "targetHighMgDl": 180,
                  "highMgDl": 250,
                  "hba1cTargetPercent": 7.2,
                  "labHba1cPercent": 6.8,
                  "labHba1cDateIso": "2026-08-19",
                  "updatedAtIso": "2026-08-20T10:00:00Z"
                }
              ]
            }
        """.trimIndent()
        tmpFile.writeText(payload, Charsets.UTF_8)

        backupRepository.restoreFromUri(Uri.fromFile(tmpFile))

        assertNotNull(db.observedPersonDao().getByPatientId("real-person-restored"))
        assertNull(db.observedPersonDao().getByPatientId("demo-person-anna"))
        val restoredReadings = db.glucoseReadingDao().getAllLiveReadings()
        assertEquals(1, restoredReadings.size)
        assertEquals("real-person-restored", restoredReadings.first().patientId)
        assertEquals(AppMode.NONE, settingsRepository.loadSettings().appMode)
        assertNull(settingsRepository.loadSettings().selectedPatientId)
    }

    private fun seedData() = runBlocking {
        val now = Instant.parse("2026-08-20T10:00:00Z")

        db.observedPersonDao().upsertAll(
            listOf(
                ObservedPersonEntity(
                    patientId = "real-person-a",
                    firstName = "Real",
                    lastName = "User",
                    displayName = "Real User",
                    lastSeenAt = now,
                    createdAt = now,
                    updatedAt = now
                ),
                ObservedPersonEntity(
                    patientId = "demo-person-anna",
                    firstName = "Demo",
                    lastName = "User",
                    displayName = "Demo User",
                    lastSeenAt = now,
                    createdAt = now,
                    updatedAt = now
                )
            )
        )

        db.glucoseReadingDao().insertReplace(
            listOf(
                GlucoseReadingEntity(
                    id = "real-person-a:${now.toEpochMilli()}",
                    patientId = "real-person-a",
                    timestamp = now,
                    valueMgDl = 118,
                    trendArrow = "->",
                    trendLabel = "Stable",
                    source = "LibreLinkUp",
                    sourceAccountId = "acc-1",
                    receivedAt = now,
                    isValid = true,
                    rawTrendCode = null,
                    createdAt = now
                ),
                GlucoseReadingEntity(
                    id = "demo-person-anna:${now.toEpochMilli()}",
                    patientId = "demo-person-anna",
                    timestamp = now,
                    valueMgDl = 130,
                    trendArrow = "->",
                    trendLabel = "Stable",
                    source = "DemoMode",
                    sourceAccountId = null,
                    receivedAt = now,
                    isValid = true,
                    rawTrendCode = null,
                    createdAt = now
                )
            )
        )

        db.patientSettingsDao().upsert(
            PatientSettingsEntity(
                patientId = "real-person-a",
                lowCriticalMgDl = 54,
                lowMgDl = 70,
                targetLowMgDl = 80,
                targetHighMgDl = 180,
                highMgDl = 250,
                hba1cTargetPercent = 7.5,
                labHba1cPercent = 6.9,
                labHba1cDate = java.time.LocalDate.parse("2026-08-18"),
                updatedAt = now
            )
        )

        settingsRepository.saveSettings(
            AppSettings(
                email = "user@example.com",
                password = "secret",
                selectedPatientId = "real-person-a",
                appMode = AppMode.LIVE,
                refreshInterval = 120,
                backgroundPollingMinutes = 30
            )
        )
    }
}

