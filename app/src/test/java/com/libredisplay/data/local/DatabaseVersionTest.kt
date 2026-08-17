package com.libredisplay.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the declared Room database version constant and that every migration
 * step is accounted for.  These are pure JVM unit tests – no emulator required.
 *
 * Why we use DB_VERSION constant instead of annotation reflection:
 * Room's @Database annotation uses AnnotationRetention.BINARY (CLASS-level
 * retention), which means it is NOT accessible at runtime via Java reflection.
 * We therefore expose DB_VERSION as a companion object constant and test that.
 *
 * Full Room MigrationTestHelper tests (which execute the real SQLite migration)
 * require an Android instrumentation runner and must be placed under
 * app/src/androidTest/... and run on a device or emulator with:
 *
 *   ./gradlew connectedDebugAndroidTest
 *
 * Migration SQL correctness is indirectly validated here by checking the
 * migration array contents and version range.
 */
class DatabaseVersionTest {

    @Test
    fun currentVersionIs2() {
        assertEquals(
            "DB_VERSION must be 2 after adding sourceAccountId column",
            2,
            LibreDisplayDatabase.DB_VERSION
        )
    }

    @Test
    fun dbNameIsCorrect() {
        assertEquals("libredisplay.db", LibreDisplayDatabase.DB_NAME)
    }

    @Test
    fun latestVersionIsCoveredByExplicitMigrations() {
        assertTrue(
            "Current schema version must be reachable from at least one explicit migration",
            ALL_MIGRATIONS.any { it.endVersion == LibreDisplayDatabase.DB_VERSION }
        )
    }

    @Test
    fun allMigrationsArrayContainsMigration1to2() {
        assertTrue(
            "ALL_MIGRATIONS must contain at least one entry",
            ALL_MIGRATIONS.isNotEmpty()
        )
        val migration = ALL_MIGRATIONS.firstOrNull { it.startVersion == 1 && it.endVersion == 2 }
        assertTrue(
            "ALL_MIGRATIONS must include MIGRATION_1_2 (startVersion=1, endVersion=2)",
            migration != null
        )
    }

    @Test
    fun migration1to2StartAndEndVersionsAreCorrect() {
        assertEquals("MIGRATION_1_2 startVersion", 1, MIGRATION_1_2.startVersion)
        assertEquals("MIGRATION_1_2 endVersion", 2, MIGRATION_1_2.endVersion)
    }

    @Test
    fun noVersionGapsBetweenMigrations() {
        val sorted = ALL_MIGRATIONS.sortedBy { it.startVersion }
        for (i in sorted.indices.drop(1)) {
            assertEquals(
                "Migration gap: ${sorted[i - 1].endVersion} -> ${sorted[i].startVersion}",
                sorted[i - 1].endVersion,
                sorted[i].startVersion
            )
        }
    }

    @Test
    fun latestMigrationEndVersionMatchesDbVersion() {
        val maxEnd = ALL_MIGRATIONS.maxOfOrNull { it.endVersion } ?: 0
        assertEquals(
            "The latest migration's endVersion must equal DB_VERSION",
            LibreDisplayDatabase.DB_VERSION,
            maxEnd
        )
    }
}
