package com.libredisplay.data.repository

import android.app.Application
import android.net.Uri
import androidx.room.Room
import com.libredisplay.data.backup.BackupCodec
import com.libredisplay.data.backup.ConflictResolution
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
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppDataBackupRepositoryTest {

    private lateinit var db: LibreDisplayDatabase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var backupRepository: AppDataBackupRepository
    private lateinit var backupRoot: File
    private lateinit var tmpFile: File

    private val now: Instant = Instant.parse("2026-08-20T10:00:00Z")

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        settingsRepository = SettingsRepository(context)
        settingsRepository.clearAll()

        db = Room.inMemoryDatabaseBuilder(context, LibreDisplayDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        backupRoot = File(System.getProperty("java.io.tmpdir"), "librecare-backup-test-${System.nanoTime()}")
        backupRoot.mkdirs()

        backupRepository = AppDataBackupRepository(
            context = context,
            database = db,
            settingsRepository = settingsRepository,
            observedPersonDao = db.observedPersonDao(),
            glucoseReadingDao = db.glucoseReadingDao(),
            patientSettingsDao = db.patientSettingsDao(),
            backupRootDirectory = backupRoot
        )
        tmpFile = File.createTempFile("librecare-backup-", ".json")
        seedData()
    }

    @After
    fun tearDown() {
        settingsRepository.clearAll()
        db.close()
        tmpFile.delete()
        backupRoot.deleteRecursively()
    }

    // ------------------------------------------------------------------ automatic backup

    @Test
    fun automaticBackup_createsExactlyOneFileWithoutUserInput() = runBlocking {
        val summary = backupRepository.createAutomaticBackup()

        assertEquals(1, summary.livePersons)
        assertEquals(1, summary.liveReadings)

        val info = backupRepository.automaticBackupInfo()
        assertTrue(info.exists)
        assertTrue(info.sizeBytes > 0L)
        assertTrue(info.absolutePath.endsWith("librecare-backup.json"))
        assertEquals(1, info.persons)
    }

    @Test
    fun automaticBackup_containsOnlyPeopleVisibleAfterLogin() = runBlocking {
        backupRepository.createAutomaticBackup()

        val text = backupRepository.automaticBackupFile().readText(Charsets.UTF_8)
        assertTrue(text.contains("real-person-a"))
        assertFalse(text.contains("demo-person-anna"))
        assertFalse(text.contains("DemoMode"))
    }

    @Test
    fun automaticBackup_isPlainAndNeedsNoPassword() = runBlocking {
        backupRepository.createAutomaticBackup()

        val text = backupRepository.automaticBackupFile().readText(Charsets.UTF_8)
        assertFalse(text.contains("cipherText"))
        assertFalse(BackupCodec.requiresPassword(text))

        val staged = backupRepository.stageAutomaticBackupRestore()
        assertEquals(1, staged.plan.persons.size)
    }

    @Test
    fun automaticBackup_carriesConfigurationForPhoneTransfer() = runBlocking {
        backupRepository.createAutomaticBackup()

        val bundle = BackupCodec.decode(backupRepository.automaticBackupFile().readText(Charsets.UTF_8))
        assertNotNull(bundle.settings)
        assertEquals(120, bundle.settings?.refreshInterval)
        assertEquals(30, bundle.settings?.backgroundPollingMinutes)
        assertTrue(bundle.quickMetricOrder.isNotEmpty())
    }

    // ------------------------------------------------------------------ export / import

    @Test
    fun exportAutomaticBackupTo_writesReadableFile() = runBlocking {
        backupRepository.exportAutomaticBackupTo(Uri.fromFile(tmpFile))

        val decoded = BackupCodec.decode(tmpFile.readText(Charsets.UTF_8))
        assertEquals(1, decoded.persons.size)
        assertEquals("real-person-a", decoded.persons.single().patientId)
    }

    @Test
    fun exportedFileCanBeStagedBackOnAnotherDevice() = runBlocking {
        backupRepository.exportAutomaticBackupTo(Uri.fromFile(tmpFile))

        // Simulate a fresh phone.
        db.glucoseReadingDao().deleteAllReadings()
        db.observedPersonDao().deleteAllPeople()

        val staged = backupRepository.stageRestoreFromUri(Uri.fromFile(tmpFile))
        assertEquals(1, staged.plan.persons.size)
        assertFalse(staged.plan.persons.single().existsLocally)
        assertEquals(1, staged.plan.totalAdded)

        val result = backupRepository.applyRestorePlan(
            staged = staged,
            selectedPatientIds = setOf("real-person-a"),
            conflictResolution = ConflictResolution.KEEP_BACKUP,
            restoreConfiguration = true
        )

        assertEquals(1, result.summary.livePersons)
        assertNotNull(db.observedPersonDao().getByPatientId("real-person-a"))
        assertTrue(result.report.contains("Przywracanie zakończone"))
    }

    // ------------------------------------------------------------------ merge behaviour

    @Test
    fun restore_mergesDifferentDateRangeAndReportsIt() = runBlocking {
        // Archive contains three extra days that the device does not know about.
        val archive = buildArchiveWithExtraDays()
        tmpFile.writeText(archive, Charsets.UTF_8)

        val staged = backupRepository.stageRestoreFromUri(Uri.fromFile(tmpFile))
        val plan = staged.plan.persons.single { it.patientId == "real-person-a" }

        assertEquals(3, plan.addedDistinctDays)
        assertFalse(plan.hasConflicts)

        val result = backupRepository.applyRestorePlan(
            staged = staged,
            selectedPatientIds = setOf("real-person-a"),
            conflictResolution = ConflictResolution.KEEP_LOCAL,
            restoreConfiguration = false
        )

        assertTrue(result.report.contains("3 dni"))
        assertEquals(4, db.glucoseReadingDao().getAllLiveReadings().size)
    }

    @Test
    fun restore_keepLocal_doesNotOverwriteDifferingValues() = runBlocking {
        tmpFile.writeText(buildArchiveWithConflict(200), Charsets.UTF_8)

        val staged = backupRepository.stageRestoreFromUri(Uri.fromFile(tmpFile))
        assertTrue(staged.plan.hasConflicts)

        backupRepository.applyRestorePlan(
            staged = staged,
            selectedPatientIds = setOf("real-person-a"),
            conflictResolution = ConflictResolution.KEEP_LOCAL,
            restoreConfiguration = false
        )

        val stored = db.glucoseReadingDao().getAllLiveReadings().single { it.patientId == "real-person-a" }
        assertEquals(118, stored.valueMgDl)
    }

    @Test
    fun restore_keepBackup_overwritesDifferingValues() = runBlocking {
        tmpFile.writeText(buildArchiveWithConflict(200), Charsets.UTF_8)

        val staged = backupRepository.stageRestoreFromUri(Uri.fromFile(tmpFile))

        backupRepository.applyRestorePlan(
            staged = staged,
            selectedPatientIds = setOf("real-person-a"),
            conflictResolution = ConflictResolution.KEEP_BACKUP,
            restoreConfiguration = false
        )

        val stored = db.glucoseReadingDao().getAllLiveReadings().single { it.patientId == "real-person-a" }
        assertEquals(200, stored.valueMgDl)
    }

    // ------------------------------------------------------------------ legacy formats

    @Test
    fun restore_readsLegacyVersion1BackupAndFiltersDemoRows() = runBlocking {
        tmpFile.writeText(LEGACY_V1_PAYLOAD, Charsets.UTF_8)

        backupRepository.restoreFromUri(Uri.fromFile(tmpFile))

        assertNotNull(db.observedPersonDao().getByPatientId("real-person-restored"))
        assertNull(db.observedPersonDao().getByPatientId("demo-person-anna"))
        val restoredReadings = db.glucoseReadingDao().getAllLiveReadings()
        assertTrue(restoredReadings.any { it.patientId == "real-person-restored" })
        assertTrue(restoredReadings.any { it.patientId == "real-person-a" })
        assertEquals(AppMode.NONE, settingsRepository.loadSettings().appMode)
        assertNull(settingsRepository.loadSettings().selectedPatientId)
    }

    @Test
    fun restore_readsLegacyEncryptedBackupWhenPasswordIsGiven() = runBlocking {
        val body = """
            {
              "schemaVersion": 2,
              "createdAt": "2026-08-20T10:00:00Z",
              "appVersion": "2.2.3",
              "profiles": [ { "patientId": "enc-person", "displayName": "Enc Person", "isActive": true,
                              "lastSeenAtIso": "2026-08-20T10:00:00Z", "createdAtIso": "2026-08-20T10:00:00Z",
                              "updatedAtIso": "2026-08-20T10:00:00Z" } ],
              "readings": [ { "id": "enc-person:1", "patientId": "enc-person",
                              "timestampIso": "2026-08-20T10:00:00Z", "valueMgDl": 145, "source": "LibreLinkUp",
                              "receivedAtIso": "2026-08-20T10:00:00Z", "isValid": true,
                              "createdAtIso": "2026-08-20T10:00:00Z" } ]
            }
        """.trimIndent()
        tmpFile.writeText(BackupCodec.encryptForLegacyFormat(body, "old-pass-123"), Charsets.UTF_8)

        backupRepository.restoreFromUri(Uri.fromFile(tmpFile), password = "old-pass-123")

        assertNotNull(db.observedPersonDao().getByPatientId("enc-person"))
    }

    @Test
    fun restore_malformedFile_failsWithReadablePolishMessageAndNoDataLoss() = runBlocking {
        val before = db.glucoseReadingDao().countLiveReadings()
        tmpFile.writeText("{ malformed-json", Charsets.UTF_8)

        try {
            backupRepository.restoreFromUri(Uri.fromFile(tmpFile))
            fail("Expected IllegalArgumentException for malformed backup JSON")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains("Nie można odczytać kopii zapasowej"))
        }

        assertEquals(before, db.glucoseReadingDao().countLiveReadings())
    }

    @Test
    fun restore_wrongPasswordForLegacyFile_failsWithoutDataLoss() = runBlocking {
        tmpFile.writeText(
            BackupCodec.encryptForLegacyFormat("""{"schemaVersion":2,"profiles":[],"readings":[]}""", "correct-pass"),
            Charsets.UTF_8
        )
        val before = db.glucoseReadingDao().countLiveReadings()

        try {
            backupRepository.restoreFromUri(Uri.fromFile(tmpFile), password = "wrong-pass")
            fail("Expected IllegalArgumentException for wrong password")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains("hasło", ignoreCase = true))
        }

        assertEquals(before, db.glucoseReadingDao().countLiveReadings())
    }

    @Test
    fun restore_replaceMode_overwritesOnlySelectedPerson() = runBlocking {
        backupRepository.exportAutomaticBackupTo(Uri.fromFile(tmpFile))

        val summary = backupRepository.restoreFromUri(
            uri = Uri.fromFile(tmpFile),
            selection = AppDataBackupRepository.RestoreSelection(
                people = listOf(
                    AppDataBackupRepository.PersonRestoreSelection(
                        patientId = "real-person-a",
                        mode = AppDataBackupRepository.RestoreMode.REPLACE
                    )
                ),
                restoreSettings = false
            )
        )

        assertEquals(1, summary.livePersons)
        assertTrue(db.glucoseReadingDao().getAllLiveReadings().any { it.patientId == "real-person-a" })
    }

    @Test
    fun isLocalLiveDataEmpty_reflectsDatabaseState() = runBlocking {
        assertFalse(backupRepository.isLocalLiveDataEmpty())

        db.glucoseReadingDao().deleteAllReadings()
        db.observedPersonDao().deleteAllPeople()

        assertTrue(backupRepository.isLocalLiveDataEmpty())
    }

    // ------------------------------------------------------------------ helpers

    private fun buildArchiveWithExtraDays(): String {
        val readings = (1..3).joinToString(",") { day ->
            val timestamp = now.minus(Duration.ofDays(day.toLong()))
            """
            {
              "id": "real-person-a:${timestamp.toEpochMilli()}",
              "patientId": "real-person-a",
              "timestampIso": "$timestamp",
              "valueMgDl": ${100 + day},
              "source": "LibreLinkUp",
              "receivedAtIso": "$timestamp",
              "isValid": true,
              "createdAtIso": "$timestamp"
            }
            """.trimIndent()
        }
        return """
            {
              "format": "librecare-backup",
              "schemaVersion": 3,
              "createdAt": "$now",
              "appVersion": "2.4.0",
              "persons": [ { "patientId": "real-person-a", "displayName": "Real User", "isActive": true,
                             "lastSeenAtIso": "$now", "createdAtIso": "$now", "updatedAtIso": "$now" } ],
              "readings": [ $readings,
                            { "id": "real-person-a:${now.toEpochMilli()}", "patientId": "real-person-a",
                              "timestampIso": "$now", "valueMgDl": 118, "source": "LibreLinkUp",
                              "receivedAtIso": "$now", "isValid": true, "createdAtIso": "$now" } ],
              "patientSettings": []
            }
        """.trimIndent()
    }

    private fun buildArchiveWithConflict(value: Int): String = """
        {
          "format": "librecare-backup",
          "schemaVersion": 3,
          "createdAt": "$now",
          "appVersion": "2.4.0",
          "persons": [ { "patientId": "real-person-a", "displayName": "Real User", "isActive": true,
                         "lastSeenAtIso": "$now", "createdAtIso": "$now", "updatedAtIso": "$now" } ],
          "readings": [ { "id": "real-person-a:${now.toEpochMilli()}", "patientId": "real-person-a",
                          "timestampIso": "$now", "valueMgDl": $value, "source": "LibreLinkUp",
                          "receivedAtIso": "$now", "isValid": true, "createdAtIso": "$now" } ],
          "patientSettings": []
        }
    """.trimIndent()

    private fun seedData() = runBlocking {
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

    private companion object {
        val LEGACY_V1_PAYLOAD = """
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
    }
}

