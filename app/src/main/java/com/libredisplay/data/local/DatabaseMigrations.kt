package com.libredisplay.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.libredisplay.diagnostics.DiagnosticLogger

/**
 * MIGRATION_1_2
 *
 * Change: added [GlucoseReadingEntity.sourceAccountId] (nullable TEXT) to the
 * `glucose_readings` table. SQLite ALTER TABLE allows adding a nullable column
 * without specifying DEFAULT, so existing rows simply get NULL. All other
 * multi-person tables already exist in schema version 1 and must remain intact.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        DiagnosticLogger.logInfo("DB", "Migration 1 -> 2: adding sourceAccountId to glucose_readings")
        database.execSQL("ALTER TABLE glucose_readings ADD COLUMN sourceAccountId TEXT")
        DiagnosticLogger.logInfo("DB", "Migration 1 -> 2 completed successfully")
    }
}

/**
 * All registered migrations, in order. Register every migration here so the
 * database builder can pick them all up with a single spread call.
 */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2
)

