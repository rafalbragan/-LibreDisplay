package com.libredisplay.data.repository

import android.app.Application
import com.libredisplay.data.api.PersistedLibreLinkUpSession
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun clearingSessionAndCredentials_requiresExplicitLoginAgain() {
        val context = RuntimeEnvironment.getApplication()
        val repo = SettingsRepository(context)
        repo.clearAll()

        repo.saveSettings(
            AppSettings(
                email = "u@example.com",
                password = "x",
                appMode = AppMode.LIVE
            )
        )
        repo.savePersistedSession(
            PersistedLibreLinkUpSession(
                token = "token",
                userId = "user",
                accountIdHash = "account",
                region = "EU",
                baseUrl = "https://example.local"
            )
        )

        assertTrue(repo.hasStoredCredentials())
        assertTrue(repo.hasPersistedSessionData())

        repo.clearSessionData()
        repo.clearAccountCredentials()
        repo.switchToLiveMode()

        assertFalse(repo.hasStoredCredentials())
        assertFalse(repo.hasPersistedSessionData())
        assertTrue(repo.shouldShowLoginForm())
    }
}
