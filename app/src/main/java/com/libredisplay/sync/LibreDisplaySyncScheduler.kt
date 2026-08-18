package com.libredisplay.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.libredisplay.data.repository.SettingsRepository
import java.util.concurrent.TimeUnit

object LibreDisplaySyncScheduler {

    const val WORK_NAME = "LibreDisplayPeriodicSync"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val pollingMinutes = SettingsRepository(context).loadSettings().backgroundPollingMinutes.coerceIn(15, 60)
        val request = PeriodicWorkRequestBuilder<LibreDisplaySyncWorker>(pollingMinutes.toLong(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

