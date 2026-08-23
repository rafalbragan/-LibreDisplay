package com.libredisplay.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import com.libredisplay.LibreDisplayApp
import com.libredisplay.diagnostics.DiagnosticLogger
import kotlinx.coroutines.runBlocking

/**
 * Makes sure the single automatic LibreCare backup file is up to date right before Android
 * (Google One backup, Samsung Smart Switch, cable / Wi-Fi device transfer, adb backup) copies the
 * application data directory to the new device.
 *
 * Only full backup is used - the legacy key/value API is intentionally a no-op.
 */
class LibreCareBackupAgent : BackupAgent() {

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) {
        // Key/value backup is not used by LibreCare.
    }

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
        // Key/value restore is not used by LibreCare.
    }

    override fun onFullBackup(data: FullBackupDataOutput?) {
        refreshAutomaticBackup()
        super.onFullBackup(data)
    }

    override fun onRestoreFinished() {
        super.onRestoreFinished()
        DiagnosticLogger.logInfo(
            "LibreCareBackupAgent",
            "SYSTEM RESTORE FINISHED - automatic LibreCare backup file is available for import"
        )
    }

    private fun refreshAutomaticBackup() {
        runCatching {
            val app = applicationContext as? LibreDisplayApp ?: return
            runBlocking { app.appDataBackupRepository.createAutomaticBackup(includeConfiguration = true) }
            DiagnosticLogger.logInfo("LibreCareBackupAgent", "SYSTEM BACKUP prepared automatic backup file")
        }.onFailure {
            DiagnosticLogger.logWarning(
                "LibreCareBackupAgent",
                "SYSTEM BACKUP could not refresh automatic backup reason=${it.message}"
            )
        }
    }
}

