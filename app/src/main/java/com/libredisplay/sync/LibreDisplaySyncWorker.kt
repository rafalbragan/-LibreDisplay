package com.libredisplay.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.repository.SyncReason
import com.libredisplay.data.repository.SyncStatus

class LibreDisplaySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as LibreDisplayApp
        val result = app.glucoseSyncRepository.syncAllPersons(SyncReason.PERIODIC)
        if (result.status == SyncStatus.SUCCESS) {
            // Keep the single automatic backup file aligned with the freshly synced data.
            runCatching { app.appDataBackupRepository.createAutomaticBackup(includeConfiguration = true) }
        }
        return when (result.status) {
            SyncStatus.SUCCESS,
            SyncStatus.RATE_LIMITED,
            SyncStatus.SKIPPED -> Result.success()
            SyncStatus.FAILED -> Result.retry()
        }
    }
}

