package com.libredisplay.data.backup

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File

class LocalBackupStoreTest {

    private lateinit var root: File
    private lateinit var store: LocalBackupStore

    private fun bundle(persons: Int) = BackupBundle(
        schemaVersion = BackupBundle.CURRENT_SCHEMA_VERSION,
        createdAtIso = "2026-08-20T10:00:00Z",
        appVersion = "2.4.0",
        persons = (1..persons).map {
            BackupPersonDto(
                patientId = "p$it",
                displayName = "Osoba $it",
                lastSeenAtIso = "2026-08-20T10:00:00Z",
                createdAtIso = "2026-08-20T10:00:00Z",
                updatedAtIso = "2026-08-20T10:00:00Z"
            )
        }
    )

    @Before
    fun setUp() {
        root = File(System.getProperty("java.io.tmpdir"), "librecare-store-${System.nanoTime()}")
        root.mkdirs()
        store = LocalBackupStore(root)
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun write_createsExactlyOneBackupFile() {
        store.write(BackupCodec.encode(bundle(1)))
        store.write(BackupCodec.encode(bundle(2)))

        assertTrue(store.exists())
        assertEquals(LocalBackupStore.FILE_NAME, store.backupFile.name)

        val payloadFiles = store.directory.listFiles()
            .orEmpty()
            .filter { it.name == LocalBackupStore.FILE_NAME }
        assertEquals(1, payloadFiles.size)

        val decoded = BackupCodec.decode(store.read()!!)
        assertEquals(2, decoded.persons.size)
    }

    @Test
    fun write_keepsPreviousCopyAndSurvivesCorruption() {
        store.write(BackupCodec.encode(bundle(3)))
        store.write(BackupCodec.encode(bundle(5)))

        // Simulate a corrupted current file (interrupted write, damaged storage).
        store.backupFile.writeText("{ broken", Charsets.UTF_8)

        val recovered = store.read()
        assertNotNull(recovered)
        val decoded = BackupCodec.decode(recovered!!)
        assertEquals(3, decoded.persons.size)
    }

    @Test
    fun write_rejectsContentThatCannotBeReadBack() {
        try {
            store.write("definitely not a backup")
            fail("Expected BackupFormatException")
        } catch (error: BackupFormatException) {
            assertTrue(error.message.orEmpty().contains("weryfikacji"))
        }
        assertFalse(store.exists())
    }

    @Test
    fun deleteRemovesEverything() {
        store.write(BackupCodec.encode(bundle(1)))
        store.write(BackupCodec.encode(bundle(1)))
        store.delete()

        assertFalse(store.exists())
        assertEquals(0L, store.sizeBytes())
    }

    @Test
    fun metadataIsReported() {
        store.write(BackupCodec.encode(bundle(1)))

        assertTrue(store.sizeBytes() > 0L)
        assertNotNull(store.lastModifiedEpochMillis())
        assertEquals("librecare-backup.json", store.exportFileName())
    }
}

