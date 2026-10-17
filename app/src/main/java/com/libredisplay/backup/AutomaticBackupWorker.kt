package com.libredisplay.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.libredisplay.LibreDisplayApp
import com.libredisplay.diagnostics.DiagnosticLogger
import java.util.concurrent.TimeUnit

/**
 * Keeps the single automatic LibreCare backup file fresh without any user interaction.
 *
 * The worker is deliberately cheap: it only serializes the LIVE rows that are already in the
 * local database, so it never touches the network.
 */
class AutomaticBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? LibreDisplayApp ?: return Result.success()
        return runCatching {
            val summary = app.appDataBackupRepository.createAutomaticBackup(includeConfiguration = true)
            val info = app.appDataBackupRepository.automaticBackupInfo()
            summary to info
        }
            .fold(
                onSuccess = { (summary, info) ->
                    DiagnosticLogger.logInfo(
                        "AutomaticBackupWorker",
                        "AUTO BACKUP OK persons=${summary.livePersons} readings=${summary.liveReadings} restorable=${info.restorable}"
                    )
                    if (!info.restorable) {
                        DiagnosticLogger.logWarning(
                            "AutomaticBackupWorker",
                            "AUTO BACKUP NOT RESTORABLE reason=${info.validationMessage ?: "unknown"}"
                        )
                    }
                    Result.success()
                },
                onFailure = {
                    DiagnosticLogger.logWarning(
                        "AutomaticBackupWorker",
                        "AUTO BACKUP FAILED reason=${it.message}"
                    )
                    Result.retry()
                }
            )
    }

    companion object {
        const val WORK_NAME = "LibreCareAutomaticBackup"
        private const val INTERVAL_HOURS = 6L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutomaticBackupWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}

