package com.libredisplay.data.repository

import com.libredisplay.data.api.LibreLinkUpClient
import com.libredisplay.data.api.MockLibreLinkUpClient
import com.libredisplay.data.api.RetrofitLibreLinkUpClient
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.diagnostics.DiagnosticLogger

data class GlucoseSnapshot(
    val current: GlucoseReading,
    val history: List<com.libredisplay.data.model.GlucoseHistoryPoint>
)

class GlucoseRepository(
    private val settingsProvider: () -> AppSettings,
    private val authRepository: AuthRepository,
    private val productionClient: RetrofitLibreLinkUpClient,
    private val mockClient: LibreLinkUpClient = MockLibreLinkUpClient()
) {

    suspend fun fetchLatestReading(): GlucoseReading? {
        val settings = settingsProvider()
        return if (settings.useMock) {
            ensureMockReady(settings)
            mockClient.getLatestReading()
        } else {
            authRepository.ensureAuthenticated(force = false)
            fetchLatestReadingFromActiveSession()
        }
    }

    suspend fun fetchLatestReadingWithSnapshot(snapshot: CredentialsSnapshot): GlucoseReading {
        authRepository.connectOnce(snapshot = snapshot, force = true)
        return fetchLatestReadingFromActiveSession()
    }

    suspend fun fetchLatestReadingFromPersistedSessionOrNull(): GlucoseReading? {
        val hasSession = authRepository.ensureSessionFromStorageOnly()
        if (!hasSession) return null
        return fetchLatestReadingFromActiveSession()
    }

    suspend fun fetchLatestReadingFromActiveSession(): GlucoseReading {
        val reading = productionClient.getLatestReading()
        DiagnosticLogger.logInfo(
            "GlucoseRepository",
            "Reading value=${reading.value} trend=${reading.trend.arrow} history=${reading.history.size}"
        )
        return reading
    }

    suspend fun fetchLatestSnapshotFromActiveSession(): GlucoseSnapshot {
        val reading = fetchLatestReadingFromActiveSession()
        return GlucoseSnapshot(current = reading, history = reading.history)
    }

    fun resetSession() {
        authRepository.clearSession()
    }

    private suspend fun ensureMockReady(settings: AppSettings) {
        mockClient.login(
            email = settings.email.ifBlank { "mock@libredisplay.local" },
            password = settings.password.ifBlank { "mock" }
        )
    }
}
