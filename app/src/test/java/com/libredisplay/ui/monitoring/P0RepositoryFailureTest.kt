package com.libredisplay.ui.monitoring

import com.libredisplay.data.api.NonRetryableLibreLinkUpException
import com.libredisplay.data.api.RetrofitLibreLinkUpClient
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.repository.AuthRepository
import com.libredisplay.data.repository.GlucoseRepository
import com.libredisplay.data.repository.SelectedPersonGraphException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class P0RepositoryFailureTest {
    private val http = P0RecoveryFixture.FixtureHttp()
    private val client = RetrofitLibreLinkUpClient(http = http)
    private val settings = AppSettings(appMode = AppMode.LIVE, email = "fixture@example.invalid", password = "synthetic")
    private val auth = AuthRepository(settingsProvider = { settings }, client = client)
    private val repository = GlucoseRepository(settingsProvider = { settings }, authRepository = auth, productionClient = client)

    @Test fun missingGraph_preservesPeopleAndExactParserCauseForStartupRecovery() = runTest {
        val error = runCatching { repository.fetchMonitoringSnapshot(P0RecoveryFixture.INACTIVE) }.exceptionOrNull()
        assertTrue(error is SelectedPersonGraphException)
        val graphError = error as SelectedPersonGraphException
        assertEquals(P0RecoveryFixture.INACTIVE, graphError.selectedPerson.patientId)
        assertEquals(listOf(P0RecoveryFixture.ACTIVE, P0RecoveryFixture.INACTIVE), graphError.persons.map { it.patientId })
        assertTrue(graphError.cause is NonRetryableLibreLinkUpException)
        assertEquals("Brak aktualnego pomiaru glukozy w odpowiedzi graph", graphError.cause?.message)
        assertTrue(client.hasActiveSession())
    }

    @Test fun cancelledGraph_isNotConvertedToAnInactivePersonFailure() = runTest {
        http.gatedPatient = P0RecoveryFixture.INACTIVE
        http.graphGate = CompletableDeferred<Unit>().apply { cancel() }
        val error = runCatching { repository.fetchMonitoringSnapshot(P0RecoveryFixture.INACTIVE) }.exceptionOrNull()
        assertTrue(error is CancellationException)
        assertFalse(error is SelectedPersonGraphException)
    }

    @Test fun stalePerson_retainsOnlyItsOwnReadingAndTimestamp() = runTest {
        http.inactiveHasStaleReading = true
        val snapshot = repository.fetchMonitoringSnapshot(P0RecoveryFixture.INACTIVE)
        assertEquals(P0RecoveryFixture.INACTIVE, snapshot.selectedPerson.patientId)
        assertEquals(231, snapshot.reading.value)
        assertTrue(snapshot.reading.history.all { it.value == 231 })
        assertTrue(java.time.Duration.between(snapshot.reading.timestamp, java.time.Instant.now()).toDays() >= 41)
    }
}
