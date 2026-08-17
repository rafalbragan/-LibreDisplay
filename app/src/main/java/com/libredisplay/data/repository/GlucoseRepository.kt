package com.libredisplay.data.repository

import com.libredisplay.data.api.LibreLinkUpClient
import com.libredisplay.data.api.MockLibreLinkUpClient
import com.libredisplay.data.api.RetrofitLibreLinkUpClient
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.LibreConnectionPerson
import com.libredisplay.data.model.MonitoringSnapshot
import com.libredisplay.diagnostics.DiagnosticLogger

data class GlucoseSnapshot(
    val current: GlucoseReading,
    val history: List<com.libredisplay.data.model.GlucoseHistoryPoint>
)

class GlucoseRepository(
    private val settingsProvider: () -> AppSettings,
    private val authRepository: AuthRepository,
    private val productionClient: RetrofitLibreLinkUpClient,
    private val mockClient: LibreLinkUpClient = MockLibreLinkUpClient(),
    private val localHistoryRepository: LocalGlucoseHistoryRepository? = null
) {

    suspend fun fetchLatestReading(): GlucoseReading? {
        return runCatching { fetchMonitoringSnapshot().reading }
            .getOrElse {
                val localSnapshot = loadLatestMonitoringSnapshotFromLocal()
                localSnapshot?.reading
            }
    }

    suspend fun fetchLatestReadingWithSnapshot(snapshot: CredentialsSnapshot): GlucoseReading {
        return fetchMonitoringSnapshotWithSnapshot(snapshot).reading
    }

    suspend fun fetchLatestReadingFromPersistedSessionOrNull(): GlucoseReading? {
        return fetchMonitoringSnapshotFromPersistedSessionOrNull()?.reading
    }

    suspend fun fetchLatestReadingFromActiveSession(): GlucoseReading {
        val reading = productionClient.getLatestReading()
            ?: throw NoActivePersonsException("Nie znaleziono aktywnych osób w LibreLinkUp.")
        DiagnosticLogger.logInfo(
            "GlucoseRepository",
            "Reading value=${reading.value} trend=${reading.trend.arrow} history=${reading.history.size}"
        )
        return reading
    }

    suspend fun fetchMonitoringSnapshotFromActiveSession(preferredPatientId: String? = null): MonitoringSnapshot {
        val settings = settingsProvider()
        return fetchMonitoringSnapshotFromClient(
            client = productionClient,
            preferredPatientId = preferredPatientId,
            storedPatientId = settings.selectedPatientId
        )
    }

    suspend fun fetchMonitoringSnapshot(preferredPatientId: String? = null): MonitoringSnapshot {
        val settings = settingsProvider()
        return if (settings.useMock) {
            ensureMockReady(settings)
            fetchMonitoringSnapshotFromClient(mockClient, preferredPatientId, settings.selectedPatientId)
        } else {
            authRepository.ensureAuthenticated(force = false)
            fetchMonitoringSnapshotFromClient(productionClient, preferredPatientId, settings.selectedPatientId)
        }
    }

    suspend fun fetchMonitoringSnapshotFromPersistedSessionOrNull(preferredPatientId: String? = null): MonitoringSnapshot? {
        val settings = settingsProvider()
        if (settings.useMock) {
            ensureMockReady(settings)
            return fetchMonitoringSnapshotFromClient(mockClient, preferredPatientId, settings.selectedPatientId)
        }
        val hasSession = authRepository.ensureSessionFromStorageOnly()
        if (!hasSession) {
            return loadLatestMonitoringSnapshotFromLocal(preferredPatientId ?: settings.selectedPatientId)
        }
        return fetchMonitoringSnapshotFromClient(productionClient, preferredPatientId, settings.selectedPatientId)
    }

    suspend fun loadLatestMonitoringSnapshotFromLocal(preferredPatientId: String? = null): MonitoringSnapshot? {
        return localHistoryRepository?.loadLatestMonitoringSnapshot(preferredPatientId)
    }

    suspend fun fetchMonitoringSnapshotWithSnapshot(
        snapshot: CredentialsSnapshot,
        preferredPatientId: String? = null
    ): MonitoringSnapshot {
        authRepository.connectOnce(snapshot = snapshot, force = true)
        return fetchMonitoringSnapshot(preferredPatientId = preferredPatientId)
    }

    suspend fun fetchLatestSnapshotFromActiveSession(): GlucoseSnapshot {
        val reading = fetchLatestReadingFromActiveSession()
        return GlucoseSnapshot(current = reading, history = reading.history)
    }

    fun resetSession() {
        authRepository.clearSession()
    }

    private suspend fun fetchMonitoringSnapshotFromClient(
        client: LibreLinkUpClient,
        preferredPatientId: String?,
        storedPatientId: String?
    ): MonitoringSnapshot {
        val persons = runCatching { client.getConnections() }
            .getOrElse { throwable ->
                DiagnosticLogger.logException("GlucoseRepository", throwable, "Connections fetch failed")
                throw throwable
            }
        if (persons.isEmpty()) {
            throw NoActivePersonsException("Nie znaleziono aktywnych osób w LibreLinkUp.")
        }

        val selectedPerson = resolveSelectedPerson(persons, preferredPatientId, storedPatientId)
        DiagnosticLogger.logInfo(
            "DATA",
            "Refreshing glucose data for person: ${selectedPerson.displayName} / ${selectedPerson.patientId}"
        )
        DiagnosticLogger.logInfo(
            "GlucoseRepository",
            "GRAPH REQUEST FOR SELECTED PERSON name=${selectedPerson.displayName} patientIdPrefix=${selectedPerson.patientId.take(6)}"
        )

        val reading = runCatching { client.getLatestReading(selectedPerson.patientId) }
            .getOrElse { throwable ->
                DiagnosticLogger.logException("GlucoseRepository", throwable, "Graph fetch failed for selected person")
                throw SelectedPersonGraphException(selectedPerson, throwable)
            }
            ?: throw SelectedPersonGraphException(selectedPerson, NoSuchElementException("Brak danych dla wybranej osoby"))

        DiagnosticLogger.logInfo(
            "GlucoseRepository",
            "Reading value=${reading.value} trend=${reading.trend.arrow} history=${reading.history.size}"
        )
        val snapshot = MonitoringSnapshot(persons = persons, selectedPerson = selectedPerson, reading = reading)
        localHistoryRepository?.upsertObservedPersons(persons, java.time.Instant.now())
        localHistoryRepository?.insertReadings(
            patientId = selectedPerson.patientId,
            source = if (client === mockClient) "DemoMode" else "LibreLinkUp",
            sourceAccountId = authRepository.currentAccountIdHash(),
            points = (reading.history + listOf(
                com.libredisplay.data.model.GlucoseHistoryPoint(
                    value = reading.value,
                    timestamp = reading.timestamp,
                    trend = reading.trend
                )
            )).distinctBy { it.timestamp }
        )
        return snapshot
    }

    private fun resolveSelectedPerson(
        persons: List<LibreConnectionPerson>,
        preferredPatientId: String?,
        storedPatientId: String?
    ): LibreConnectionPerson {
        val requestedPatientId = preferredPatientId?.trim().takeIf { !it.isNullOrBlank() }
            ?: storedPatientId?.trim().takeIf { !it.isNullOrBlank() }
        val selectedFromSettings = requestedPatientId?.let { requested -> persons.firstOrNull { it.patientId == requested } }
        return if (selectedFromSettings != null) {
            DiagnosticLogger.logInfo(
                "GlucoseRepository",
                "SELECTED PERSON RESTORED FROM SETTINGS patientIdPrefix=${selectedFromSettings.patientId.take(6)} name=${selectedFromSettings.displayName}"
            )
            selectedFromSettings.also {
                DiagnosticLogger.logInfo(
                    "GlucoseRepository",
                    "SELECTED PERSON patientIdPrefix=${it.patientId.take(6)} name=${it.displayName}"
                )
            }
        } else {
            val fallback = persons.first()
            DiagnosticLogger.logInfo(
                "GlucoseRepository",
                if (requestedPatientId != null) {
                    "SELECTED PERSON FALLBACK TO FIRST patientIdPrefix=${fallback.patientId.take(6)} name=${fallback.displayName}"
                } else {
                    "SELECTED PERSON FALLBACK TO FIRST patientIdPrefix=${fallback.patientId.take(6)} name=${fallback.displayName}"
                }
            )
            fallback.also {
                DiagnosticLogger.logInfo(
                    "GlucoseRepository",
                    "SELECTED PERSON patientIdPrefix=${it.patientId.take(6)} name=${it.displayName}"
                )
            }
        }
    }

    private suspend fun ensureMockReady(settings: AppSettings) {
        mockClient.login(
            email = settings.email.ifBlank { "mock@libredisplay.local" },
            password = settings.password.ifBlank { "mock" }
        )
    }
}

class SelectedPersonGraphException(
    val selectedPerson: LibreConnectionPerson,
    cause: Throwable
) : RuntimeException("Nie udało się pobrać danych dla: ${selectedPerson.displayName}", cause)

class NoActivePersonsException(message: String) : RuntimeException(message)

