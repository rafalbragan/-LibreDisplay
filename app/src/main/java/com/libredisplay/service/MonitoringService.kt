package com.libredisplay.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.libredisplay.LibreDisplayApp
import com.libredisplay.LibreDisplayApp.Companion.MONITORING_CHANNEL_ID
import com.libredisplay.MainActivity
import com.libredisplay.R
import com.libredisplay.data.api.stopsAutomaticPolling
import com.libredisplay.diagnostics.DiagnosticLogger
import com.libredisplay.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "MonitoringService"
private const val NOTIFICATION_ID = 1001

class MonitoringService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val widgetUpdater by lazy { WidgetUpdater(applicationContext) }
    private val app by lazy { application as LibreDisplayApp }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        startPollingLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startPollingLoop() {
        scope.launch {
            while (isActive) {
                try {
                    val reading = app.glucoseRepository.fetchLatestReading()
                    if (reading != null) {
                        widgetUpdater.updateWithReading(
                            value = reading.value,
                            unit = getString(R.string.unit_mgdl),
                            trend = reading.trend.arrow,
                            timestamp = reading.timestamp
                        )
                    }
                } catch (e: Exception) {
                    DiagnosticLogger.logError(TAG, "Background fetch failed: ${e.message}")
                    DiagnosticLogger.logException(TAG, e, "MonitoringService background fetch exception")
                    widgetUpdater.updateWithError(e.message ?: getString(R.string.error_check_official_app))
                    if (e.stopsAutomaticPolling()) {
                        DiagnosticLogger.logError(TAG, "MonitoringService polling stopped after critical error. Waiting for manual retry.")
                        stopSelf()
                        break
                    }
                }
                delay(app.settingsRepository.loadSettings().refreshInterval * 1000L)
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
