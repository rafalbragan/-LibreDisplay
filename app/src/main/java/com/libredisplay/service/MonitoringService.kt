package com.libredisplay.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.libredisplay.LibreDisplayApp.Companion.MONITORING_CHANNEL_ID
import com.libredisplay.MainActivity
import com.libredisplay.R
import com.libredisplay.data.repository.GlucoseRepository
import com.libredisplay.data.repository.SettingsRepository
import com.libredisplay.widget.WidgetUpdater
import kotlinx.coroutines.*

private const val TAG = "MonitoringService"
private const val NOTIFICATION_ID = 1001

/**
 * Foreground service that keeps glucose data fresh even when the activity
 * is in the background.
 *
 * The service posts a persistent notification so Android does not kill it.
 * The actual data fetch is delegated to [GlucoseRepository].
 */
class MonitoringService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val widgetUpdater by lazy { WidgetUpdater(applicationContext) }

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var glucoseRepository: GlucoseRepository

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        glucoseRepository  = GlucoseRepository(settingsRepository)
        startForeground(NOTIFICATION_ID, buildNotification())
        startPollingLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY   // Restart if killed by the OS

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun startPollingLoop() {
        scope.launch {
            while (isActive) {
                try {
                    val reading = glucoseRepository.fetchLatestReading()
                    if (reading != null) {
                        widgetUpdater.updateWithReading(
                            value = reading.value,
                            unit = getString(R.string.unit_mgdl),
                            trend = reading.trend.arrow,
                            timestamp = reading.timestamp
                        )
                    }
                    Log.d(TAG, "Background fetch successful")
                } catch (e: Exception) {
                    Log.e(TAG, "Background fetch failed: ${e.message}")
                    widgetUpdater.updateWithError(e.message ?: getString(R.string.error_check_official_app))
                }
                delay(15_000L)
            }
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, MONITORING_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_monitoring_title))
            .setContentText(getString(R.string.notification_monitoring_text))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()
    }
}

