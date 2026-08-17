package com.libredisplay.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
        LibreDisplayDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrate1To2_preservesPeopleAndReadings_andAddsSourceAccountId() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `observed_persons` (
                    `patientId` TEXT NOT NULL,
                    `firstName` TEXT,
                    `lastName` TEXT,
                    `displayName` TEXT NOT NULL,
                    `connectionId` TEXT,
                    `isActive` INTEGER NOT NULL,
                    `lastSeenAt` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`patientId`)
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `glucose_readings` (
                    `id` TEXT NOT NULL,
                    `patientId` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `valueMgDl` INTEGER NOT NULL,
                    `trendArrow` TEXT,
                    `trendLabel` TEXT,
                    `source` TEXT NOT NULL,
                    `receivedAt` INTEGER NOT NULL,
                    `isValid` INTEGER NOT NULL,
                    `rawTrendCode` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_glucose_readings_patientId_timestamp` ON `glucose_readings` (`patientId`, `timestamp`)")
            execSQL("CREATE INDEX IF NOT EXISTS `index_glucose_readings_patientId` ON `glucose_readings` (`patientId`)")
            execSQL("CREATE INDEX IF NOT EXISTS `index_glucose_readings_timestamp` ON `glucose_readings` (`timestamp`)")
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sync_runs` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `startedAt` INTEGER NOT NULL,
                    `finishedAt` INTEGER,
                    `status` TEXT NOT NULL,
                    `reason` TEXT NOT NULL,
                    `personsCount` INTEGER NOT NULL,
                    `readingsInserted` INTEGER NOT NULL,
                    `readingsSkippedDuplicate` INTEGER NOT NULL,
                    `errorMessage` TEXT,
                    `httpStatus` INTEGER,
                    `retryAfterSeconds` INTEGER
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `patient_settings` (
                    `patientId` TEXT NOT NULL,
                    `lowCriticalMgDl` INTEGER NOT NULL,
                    `lowMgDl` INTEGER NOT NULL,
                    `targetLowMgDl` INTEGER NOT NULL,
                    `targetHighMgDl` INTEGER NOT NULL,
                    `highMgDl` INTEGER NOT NULL,
                    `hba1cTargetPercent` REAL NOT NULL,
                    `labHba1cPercent` REAL,
                    `labHba1cDate` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`patientId`)
                )
                """.trimIndent()
            )

            val now = Instant.parse("2026-08-17T10:00:00Z").toEpochMilli()
            execSQL("INSERT INTO observed_persons (patientId, firstName, lastName, displayName, connectionId, isActive, lastSeenAt, createdAt, updatedAt) VALUES ('patient-a', 'Anna', 'Nowak', 'Anna Nowak', NULL, 1, $now, $now, $now)")
            execSQL("INSERT INTO observed_persons (patientId, firstName, lastName, displayName, connectionId, isActive, lastSeenAt, createdAt, updatedAt) VALUES ('patient-b', 'Bartek', 'Kowalski', 'Bartek Kowalski', NULL, 1, $now, $now, $now)")
            execSQL("INSERT INTO glucose_readings (id, patientId, timestamp, valueMgDl, trendArrow, trendLabel, source, receivedAt, isValid, rawTrendCode, createdAt) VALUES ('patient-a:1', 'patient-a', $now, 115, '→', 'Stable', 'LibreLinkUp', $now, 1, NULL, $now)")
            execSQL("INSERT INTO glucose_readings (id, patientId, timestamp, valueMgDl, trendArrow, trendLabel, source, receivedAt, isValid, rawTrendCode, createdAt) VALUES ('patient-b:1', 'patient-b', $now, 154, '↗', 'Rising', 'LibreLinkUp', $now, 1, NULL, $now)")
            execSQL("INSERT INTO patient_settings (patientId, lowCriticalMgDl, lowMgDl, targetLowMgDl, targetHighMgDl, highMgDl, hba1cTargetPercent, labHba1cPercent, labHba1cDate, updatedAt) VALUES ('patient-a', 54, 70, 80, 180, 250, 7.5, NULL, NULL, $now)")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        val db = Room.databaseBuilder(context, LibreDisplayDatabase::class.java, TEST_DB)
            .addMigrations(MIGRATION_1_2)
            .build()

        runBlocking {
            val persons = db.observedPersonDao().getActivePersons()
            assertEquals(2, persons.size)
            assertNotNull(persons.firstOrNull { it.patientId == "patient-a" && it.displayName == "Anna Nowak" })
            assertNotNull(persons.firstOrNull { it.patientId == "patient-b" && it.displayName == "Bartek Kowalski" })

            val from = Instant.parse("2026-08-17T00:00:00Z")
            val to = Instant.parse("2026-08-18T00:00:00Z")
            val patientAReadings = db.glucoseReadingDao().getRangeByPatient("patient-a", from, to)
            val patientBReadings = db.glucoseReadingDao().getRangeByPatient("patient-b", from, to)

            assertEquals(1, patientAReadings.size)
            assertEquals("patient-a", patientAReadings.single().patientId)
            assertEquals(115, patientAReadings.single().valueMgDl)
            assertNull(patientAReadings.single().sourceAccountId)

            assertEquals(1, patientBReadings.size)
            assertEquals("patient-b", patientBReadings.single().patientId)
            assertEquals(154, patientBReadings.single().valueMgDl)
            assertNull(patientBReadings.single().sourceAccountId)

            val patientSettings = db.patientSettingsDao().getByPatientId("patient-a")
            assertNotNull(patientSettings)
            assertEquals(7.5, patientSettings?.hba1cTargetPercent ?: 0.0, 0.0)
        }

        db.close()
    }

    companion object {
        private const val TEST_DB = "room-migration-test.db"
    }
}

