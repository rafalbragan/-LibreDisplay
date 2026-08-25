package com.libredisplay.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.libredisplay.LibreDisplayApp
import com.libredisplay.diagnostics.DiagnosticLogger

/**
 * Central start/stop for the always-on foreground [MonitoringService].
 *
 * The service keeps LibreCare fetching glucose data at the configured refresh interval even when
 * the UI is closed and is exempt from Doze (unlike the periodic WorkManager fallback). It is only
 * started when monitoring is configured and the user has kept the background service enabled.
 */
object MonitoringServiceController {

    fun startIfEnabled(context: Context) {
        val app = context.applicationContext as? LibreDisplayApp ?: return
        val settings = app.settingsRepository.loadSettings()
        if (!settings.isConfigured() || !settings.backgroundServiceEnabled) {
            return
        }
        runCatching {
            val intent = Intent(context.applicationContext, MonitoringService::class.java)
            ContextCompat.startForegroundService(context.applicationContext, intent)
        }.onFailure {
            DiagnosticLogger.logException("MonitoringServiceController", it, "startForegroundService failed")
        }
    }

    fun stop(context: Context) {
        runCatching {
            context.applicationContext.stopService(
                Intent(context.applicationContext, MonitoringService::class.java)
            )
        }.onFailure {
            DiagnosticLogger.logException("MonitoringServiceController", it, "stopService failed")
        }
    }
}


