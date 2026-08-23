package com.libredisplay.data.repository

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.libredisplay.data.local.LibreDisplayDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class AppDataBackupRepositoryLegacyRestoreTest {

    private lateinit var database: LibreDisplayDatabase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var repository: AppDataBackupRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, LibreDisplayDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settingsRepository = SettingsRepository(context)
        repository = AppDataBackupRepository(
            context = context,
            database = database,
            settingsRepository = settingsRepository,
            observedPersonDao = database.observedPersonDao(),
            glucoseReadingDao = database.glucoseReadingDao(),
            patientSettingsDao = database.patientSettingsDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun restoreFromUri_supportsLegacyShortKeysBackupFormat() {
        runBlocking {
            val legacyBackup = """
            {
              "a": 1,
              "b": 1787331723872,
              "c": "2.2.2",
              "d": {
                "appMode": "LIVE",
                "backgroundPollingMinutes": 15,
                "customBaseUrl": "",
                "email": "legacy@example.com",
                "kioskMode": false,
                "password": "secret",
                "refreshInterval": 60,
                "region": "EU",
                "regionMode": "EU",
                "retentionHours": 17280,
                "selectedPatientId": "p-1",
                "showStatistics": true,
                "targetHigh": 180,
                "targetLow": 80,
                "trendWindowMinutes": 3,
                "useAuthV3": true
              },
              "e": ["in_range", "below", "above"],
              "f": {
                "in_range": true,
                "below": true,
                "above": true
              },
              "h": [
                {
                  "a": "p-1",
                  "b": "Jan",
                  "c": "Kowalski",
                  "d": "Jan Kowalski",
                  "f": true,
                  "g": "2026-08-21T17:01:15.812Z",
                  "h": "2026-08-21T17:01:15.812Z",
                  "i": "2026-08-21T17:01:15.812Z"
                }
              ],
              "i": [
                {
                  "a": "p-1:1787100677000",
                  "b": "p-1",
                  "c": "2026-08-19T00:51:17Z",
                  "d": 121,
                  "e": "→",
                  "f": "Stabilnie",
                  "g": "LibreLinkUp",
                  "h": "session-1",
                  "i": "2026-08-19T12:39:45.628Z",
                  "j": true,
                  "l": "2026-08-19T12:39:45.628Z"
                }
              ],
              "j": []
            }
            """.trimIndent()

            val tempFile = File.createTempFile("legacy-backup", ".json")
            tempFile.writeText(legacyBackup)

            val summary = repository.restoreFromUri(Uri.fromFile(tempFile), "test-pass-123")

            assertEquals(1, summary.livePersons)
            assertEquals(1, summary.liveReadings)
            assertEquals(0, summary.patientSettings)
            assertEquals(1, database.observedPersonDao().getAllLivePersons().size)
            assertEquals(1, database.glucoseReadingDao().getAllLiveReadings().size)
            assertEquals("p-1", settingsRepository.loadSettings().selectedPatientId)

            tempFile.delete()
        }
    }
}



