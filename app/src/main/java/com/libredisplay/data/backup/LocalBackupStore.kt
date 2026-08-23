package com.libredisplay.data.backup

import java.io.File

/**
 * Single-file backup storage.
 *
 * The user never chooses a location, a file name or a password. LibreCare always keeps exactly
 * one automatic backup inside the application data directory:
 *
 *   <app files dir>/backup/librecare-backup.json          <- current backup
 *   <app files dir>/backup/librecare-backup.json.previous <- last known good backup
 *
 * The write is atomic (temp file + rename) so an interrupted write can never destroy the
 * previous backup. The directory is included in Android auto backup / Samsung Smart Switch
 * transfers, which is what makes the data survive a phone change.
 */
class LocalBackupStore(private val rootDirectory: File) {

    val directory: File
        get() = File(rootDirectory, DIRECTORY_NAME)

    val backupFile: File
        get() = File(directory, FILE_NAME)

    private val previousFile: File
        get() = File(directory, "$FILE_NAME$PREVIOUS_SUFFIX")

    private val temporaryFile: File
        get() = File(directory, "$FILE_NAME.tmp")

    fun exists(): Boolean = backupFile.exists() && backupFile.length() > 0L

    fun sizeBytes(): Long = if (exists()) backupFile.length() else 0L

    fun lastModifiedEpochMillis(): Long? = backupFile.takeIf { it.exists() }?.lastModified()

    /** Atomically replaces the single backup file. Returns the file that now holds the backup. */
    @Synchronized
    fun write(content: String): File {
        directory.mkdirs()
        val temp = temporaryFile
        temp.writeText(content, Charsets.UTF_8)

        // Verify the freshly written file before it replaces the previous good backup.
        val verification = runCatching { BackupCodec.decode(temp.readText(Charsets.UTF_8)) }
        if (verification.isFailure) {
            temp.delete()
            throw BackupFormatException(
                "Zapis kopii zapasowej nie powiódł się - plik nie przeszedł weryfikacji odczytu.",
                verification.exceptionOrNull()
            )
        }

        if (backupFile.exists()) {
            previousFile.delete()
            backupFile.copyTo(previousFile, overwrite = true)
        }
        if (backupFile.exists() && !backupFile.delete()) {
            backupFile.writeText(content, Charsets.UTF_8)
            temp.delete()
            return backupFile
        }
        if (!temp.renameTo(backupFile)) {
            backupFile.writeText(content, Charsets.UTF_8)
            temp.delete()
        }
        return backupFile
    }

    /** Reads the current backup, falling back to the previous copy when the current one is broken. */
    @Synchronized
    fun read(): String? {
        val current = backupFile.takeIf { it.exists() && it.length() > 0L }?.readText(Charsets.UTF_8)
        if (current != null && runCatching { BackupCodec.decode(current) }.isSuccess) {
            return current
        }
        val previous = previousFile.takeIf { it.exists() && it.length() > 0L }?.readText(Charsets.UTF_8)
        if (previous != null && runCatching { BackupCodec.decode(previous) }.isSuccess) {
            return previous
        }
        return current ?: previous
    }

    @Synchronized
    fun delete() {
        backupFile.delete()
        previousFile.delete()
        temporaryFile.delete()
    }

    /** File name suggested when the backup is shared / exported to another phone. */
    fun exportFileName(): String = FILE_NAME

    companion object {
        const val DIRECTORY_NAME = "backup"
        const val FILE_NAME = "librecare-backup.json"
        const val PREVIOUS_SUFFIX = ".previous"
        const val MIME_TYPE = "application/json"
    }
}

