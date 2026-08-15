package com.libredisplay

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import com.libredisplay.data.local.LibreDisplayDatabase
import com.libredisplay.data.api.RetrofitLibreLinkUpClient
import com.libredisplay.data.repository.AuthRepository
import com.libredisplay.data.repository.GlucoseSyncRepository
import com.libredisplay.data.repository.GlucoseRepository
import com.libredisplay.data.repository.LocalGlucoseHistoryRepository
import com.libredisplay.data.repository.SettingsLoginStateStore
import com.libredisplay.data.repository.SettingsRepository
import com.libredisplay.diagnostics.DiagnosticLogger
import com.libredisplay.sync.LibreDisplaySyncScheduler

class LibreDisplayApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var productionClient: RetrofitLibreLinkUpClient
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var glucoseRepository: GlucoseRepository
        private set
    lateinit var database: LibreDisplayDatabase
        private set
    lateinit var localGlucoseHistoryRepository: LocalGlucoseHistoryRepository
        private set
    lateinit var glucoseSyncRepository: GlucoseSyncRepository
        private set

    override fun onCreate() {
        super.onCreate()
        DiagnosticLogger.init(this)
        DiagnosticLogger.startNewSession(this)
        installGlobalCrashHandler()
        settingsRepository = SettingsRepository(applicationContext)
        val initialSettings = settingsRepository.loadSettings()
        productionClient = RetrofitLibreLinkUpClient(initialRegion = initialSettings.loginRegionSelection())
        authRepository = AuthRepository(
            settingsProvider = { settingsRepository.loadSettings() },
            client = productionClient,
            loginStateStore = SettingsLoginStateStore(settingsRepository)
        )

        database = Room.databaseBuilder(
            applicationContext,
            LibreDisplayDatabase::class.java,
            "libredisplay.db"
        ).fallbackToDestructiveMigration().build()

        localGlucoseHistoryRepository = LocalGlucoseHistoryRepository(
            observedPersonDao = database.observedPersonDao(),
            glucoseReadingDao = database.glucoseReadingDao(),
            syncRunDao = database.syncRunDao()
        )

        glucoseRepository = GlucoseRepository(
            settingsProvider = { settingsRepository.loadSettings() },
            authRepository = authRepository,
            productionClient = productionClient,
            localHistoryRepository = localGlucoseHistoryRepository
        )

        glucoseSyncRepository = GlucoseSyncRepository(
            settingsProvider = { settingsRepository.loadSettings() },
            authRepository = authRepository,
            productionClient = productionClient,
            localRepository = localGlucoseHistoryRepository
        )

        LibreDisplaySyncScheduler.schedule(this)
        createNotificationChannel()
    }

    private fun installGlobalCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                DiagnosticLogger.logError(
                    "LibreDisplayApp",
                    "FATAL APP CRASH thread=${thread.name} exceptionClass=${throwable::class.java.name} message=${throwable.message.orEmpty()}"
                )
                DiagnosticLogger.logException("LibreDisplayApp", throwable, "FATAL APP CRASH stacktrace")
            }
            previous?.uncaughtException(thread, throwable)
        }
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
