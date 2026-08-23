package com.libredisplay.data.local

import android.content.Context
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrate1To2_preservesPeopleAndReadings_andAddsSourceAccountId() {
        val sqliteHelper = createVersion1DbHelper()
        val db = sqliteHelper.writableDatabase
        db.apply {
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
            MIGRATION_1_2.migrate(this)
        }
        db.query("SELECT COUNT(*) FROM observed_persons").use { cursor ->
            assertCursorFirst(cursor)
            assertEquals(2, cursor.getInt(0))
        }
        db.query("SELECT patientId, valueMgDl, sourceAccountId FROM glucose_readings ORDER BY patientId ASC").use { cursor ->
            assertCursorFirst(cursor)
            assertEquals("patient-a", cursor.getString(0))
            assertEquals(115, cursor.getInt(1))
            assertNull(cursor.getString(2))

            cursor.moveToNext()
            assertEquals("patient-b", cursor.getString(0))
            assertEquals(154, cursor.getInt(1))
            assertNull(cursor.getString(2))
        }
        db.query("SELECT hba1cTargetPercent FROM patient_settings WHERE patientId='patient-a'").use { cursor ->
            assertCursorFirst(cursor)
            assertNotNull(cursor.getDouble(0))
            assertEquals(7.5, cursor.getDouble(0), 0.0)
        }

        db.close()
        sqliteHelper.close()
    }

    private fun createVersion1DbHelper(): SupportSQLiteOpenHelper {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config)
    }

    private fun assertCursorFirst(cursor: Cursor) {
        assertEquals(true, cursor.moveToFirst())
    }

    companion object {
        private const val TEST_DB = "room-migration-test.db"
    }
}

