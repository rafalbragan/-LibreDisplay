package com.libredisplay.data.repository

import android.app.Application
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsRepositoryDiagnosticsTest {

    @Test
    fun saveAndLoad_retentionAndPolling_arePersisted() {
        val context = RuntimeEnvironment.getApplication()
        val repo = SettingsRepository(context)
        repo.clearAll()

        repo.saveSettings(
            AppSettings(
                email = "u@example.com",
                password = "x",
                appMode = AppMode.LIVE,
                retentionHours = 24 * 90,
                backgroundPollingMinutes = 30
            )
        )

        val loaded = repo.loadSettings()
        assertEquals(24 * 90, loaded.retentionHours)
        assertEquals(30, loaded.backgroundPollingMinutes)
    }

    @Test
    fun saveAndLoad_invalidRetentionAndPolling_areClamped() {
        val context = RuntimeEnvironment.getApplication()
        val repo = SettingsRepository(context)
        repo.clearAll()

        repo.saveSettings(
            AppSettings(
                email = "u@example.com",
                password = "x",
                appMode = AppMode.LIVE,
                retentionHours = 1,
                backgroundPollingMinutes = 999
            )
        )

        val loaded = repo.loadSettings()
        assertEquals(12, loaded.retentionHours)
        assertEquals(60, loaded.backgroundPollingMinutes)
    }

    @Test
    fun saveAndLoad_largeRetention_isClampedToExpandedMaximum() {
        val context = RuntimeEnvironment.getApplication()
        val repo = SettingsRepository(context)
        repo.clearAll()

        repo.saveSettings(
            AppSettings(
                email = "u@example.com",
                password = "x",
                appMode = AppMode.LIVE,
                retentionHours = AppSettings.MAX_RETENTION_HOURS + 24
            )
        )

        val loaded = repo.loadSettings()
        assertEquals(AppSettings.MAX_RETENTION_HOURS, loaded.retentionHours)
    }
}

