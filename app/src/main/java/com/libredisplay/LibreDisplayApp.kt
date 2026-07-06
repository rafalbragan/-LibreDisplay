package com.libredisplay

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.libredisplay.data.api.RetrofitLibreLinkUpClient
import com.libredisplay.data.repository.AuthRepository
import com.libredisplay.data.repository.GlucoseRepository
import com.libredisplay.data.repository.SettingsLoginStateStore
import com.libredisplay.data.repository.SettingsRepository
import com.libredisplay.diagnostics.DiagnosticLogger

class LibreDisplayApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var productionClient: RetrofitLibreLinkUpClient
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var glucoseRepository: GlucoseRepository
        private set

    override fun onCreate() {
        super.onCreate()
        DiagnosticLogger.init(this)
        DiagnosticLogger.startNewSession(this)
        settingsRepository = SettingsRepository(applicationContext)
        val initialSettings = settingsRepository.loadSettings()
        productionClient = RetrofitLibreLinkUpClient(initialRegion = initialSettings.loginRegionSelection())
        authRepository = AuthRepository(
            settingsProvider = { settingsRepository.loadSettings() },
            client = productionClient,
            loginStateStore = SettingsLoginStateStore(settingsRepository)
        )
        glucoseRepository = GlucoseRepository(
            settingsProvider = { settingsRepository.loadSettings() },
            authRepository = authRepository,
            productionClient = productionClient
        )
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MONITORING_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val MONITORING_CHANNEL_ID = "glucose_monitoring"
    }
}
