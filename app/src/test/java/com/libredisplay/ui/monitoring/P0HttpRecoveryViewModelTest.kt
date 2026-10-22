package com.libredisplay.ui.monitoring

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.room.Room
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.api.*
import com.libredisplay.data.local.LibreDisplayDatabase
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant
import java.util.concurrent.Executor

/** No Activity/Application startup, real network, service, backup or production data involved. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class P0HttpRecoveryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var app: LibreDisplayApp
    private lateinit var db: LibreDisplayDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var http: FaultHttp
    private lateinit var client: RetrofitLibreLinkUpClient
    private lateinit var auth: AuthRepository
    private lateinit var repository: GlucoseRepository
    private lateinit var store: ViewModelStore
    private lateinit var vm: MonitoringViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        val context = RuntimeEnvironment.getApplication()
        app = LibreDisplayApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java)
            .apply { isAccessible = true }.invoke(app, context)
        settings = SettingsRepository(context)
        settings.clearAll()
        settings.saveSettings(AppSettings(appMode = AppMode.LIVE, email = "fixture@example.invalid",
            password = "synthetic", selectedPatientId = ACTIVE, refreshInterval = 300,
            backgroundServiceEnabled = false))
        db = Room.inMemoryDatabaseBuilder(context, LibreDisplayDatabase::class.java)
            .allowMainThreadQueries().setQueryExecutor(Executor { it.run() })
            .setTransactionExecutor(Executor { it.run() }).build()
        val local = LocalGlucoseHistoryRepository(db.observedPersonDao(), db.glucoseReadingDao(), db.syncRunDao())
        http = FaultHttp()
        client = RetrofitLibreLinkUpClient(http = http)
        auth = AuthRepository({ settings.loadSettings() }, client, SettingsLoginStateStore(settings))
        repository = GlucoseRepository({ settings.loadSettings() }, auth, client, localHistoryRepository = local)
        replace("settingsRepository", settings)
        replace("authRepository", auth)
        replace("glucoseRepository", repository)
        replace("glucoseSyncRepository", GlucoseSyncRepository({ settings.loadSettings() }, auth, client, local))
        newViewModel()
    }

    @After fun tearDown() {
        store.clear()
        dispatcher.scheduler.runCurrent()
        db.close()
        settings.clearAll()
        Dispatchers.resetMain()
    }

    @Test fun startup401() = verify(Entry.STARTUP, 401)
    @Test fun startup403() = verify(Entry.STARTUP, 403)
    @Test fun manual401() = verify(Entry.MANUAL, 401)
    @Test fun manual403() = verify(Entry.MANUAL, 403)
    @Test fun polling401() = verify(Entry.POLL, 401)
    @Test fun polling403() = verify(Entry.POLL, 403)
    @Test fun startup429() = verify(Entry.STARTUP, 429)
    @Test fun startup430() = verify(Entry.STARTUP, 430)
    @Test fun manual429() = verify(Entry.MANUAL, 429)
    @Test fun manual430() = verify(Entry.MANUAL, 430)
    @Test fun polling429() = verify(Entry.POLL, 429)
    @Test fun polling430() = verify(Entry.POLL, 430)
    @Test fun startup500() = verify(Entry.STARTUP, 500)
    @Test fun startup503() = verify(Entry.STARTUP, 503)
    @Test fun manual500() = verify(Entry.MANUAL, 500)
    @Test fun manual503() = verify(Entry.MANUAL, 503)
    @Test fun polling500() = verify(Entry.POLL, 500)
    @Test fun polling503() = verify(Entry.POLL, 503)
    @Test fun startupTimeout() = verify(Entry.STARTUP, error = SocketTimeoutException("synthetic"))
    @Test fun manualTimeout() = verify(Entry.MANUAL, error = SocketTimeoutException("synthetic"))
    @Test fun pollingTimeout() = verify(Entry.POLL, error = SocketTimeoutException("synthetic"))
    @Test fun startupIo() = verify(Entry.STARTUP, error = IOException("synthetic"))
    @Test fun manualIo() = verify(Entry.MANUAL, error = IOException("synthetic"))
    @Test fun pollingIo() = verify(Entry.POLL, error = IOException("synthetic"))
    @Test fun startupDecoding() = verify(Entry.STARTUP, error = LibreResponseDecodingException("synthetic"))
    @Test fun manualDecoding() = verify(Entry.MANUAL, error = LibreResponseDecodingException("synthetic"))
    @Test fun pollingDecoding() = verify(Entry.POLL, error = LibreResponseDecodingException("synthetic"))
    @Test fun startupUnexpected() = verify(Entry.STARTUP, error = IllegalStateException("synthetic"))
    @Test fun manualUnexpected() = verify(Entry.MANUAL, error = IllegalStateException("synthetic"))
    @Test fun pollingUnexpected() = verify(Entry.POLL, error = IllegalStateException("synthetic"))
    @Test fun connections401IsAccountFailure() = verify(Entry.STARTUP, 401, connections = true)
    @Test fun connections503IsNotNoData() = verify(Entry.STARTUP, 503, connections = true)
    @Test fun switch401() = verify(Entry.SWITCH, 401)
    @Test fun switch403() = verify(Entry.SWITCH, 403)
    @Test fun switch429() = verify(Entry.SWITCH, 429)
    @Test fun switch430() = verify(Entry.SWITCH, 430)
    @Test fun switch500() = verify(Entry.SWITCH, 500)
    @Test fun switch503() = verify(Entry.SWITCH, 503)
    @Test fun switchTimeout() = verify(Entry.SWITCH, error = SocketTimeoutException("synthetic"))
    @Test fun switchIo() = verify(Entry.SWITCH, error = IOException("synthetic"))
    @Test fun switchDecoding() = verify(Entry.SWITCH, error = LibreResponseDecodingException("synthetic"))
    @Test fun switchUnexpected() = verify(Entry.SWITCH, error = IllegalStateException("synthetic"))

    @Test fun startupOldMeasurementWithoutHistory() = verifySuccessfulFreshness(Entry.STARTUP, stale = true)
    @Test fun manualOldMeasurementWithoutHistory() = verifySuccessfulFreshness(Entry.MANUAL, stale = true)
    @Test fun pollingOldMeasurementWithoutHistory() = verifySuccessfulFreshness(Entry.POLL, stale = true)
    @Test fun startupOldMeasurementWithHistory() = verifySuccessfulFreshness(Entry.STARTUP, stale = true, history = true)
    @Test fun manualOldMeasurementWithHistory() = verifySuccessfulFreshness(Entry.MANUAL, stale = true, history = true)
    @Test fun pollingOldMeasurementWithHistory() = verifySuccessfulFreshness(Entry.POLL, stale = true, history = true)
    @Test fun startupFreshMeasurementWithoutHistory() = verifySuccessfulFreshness(Entry.STARTUP, stale = false)
    @Test fun manualFreshMeasurementWithoutHistory() = verifySuccessfulFreshness(Entry.MANUAL, stale = false)
    @Test fun pollingFreshMeasurementWithoutHistory() = verifySuccessfulFreshness(Entry.POLL, stale = false)
    @Test fun switchToHistoricalPersonDoesNotClaimLiveData() = verifySuccessfulFreshness(Entry.SWITCH, stale = true)
    @Test fun switchToFreshPersonRestoresLiveData() = verifySuccessfulFreshness(Entry.SWITCH, stale = false)

    private fun verifySuccessfulFreshness(entry: Entry, stale: Boolean, history: Boolean = false) = runTest(dispatcher) {
        auth.ensureAuthenticated()
        if (entry == Entry.POLL || entry == Entry.SWITCH) {
            vm.onScreenVisible(0)
            runCurrent()
        }
        // A fixed historical timestamp reproduces the defect independently of network/timing.
        val timestamp = if (stale) Instant.parse("2020-01-01T12:00:00Z")
            else Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        val points = if (history) """[{"ValueInMgPerDl":120,"Timestamp":"${timestamp.minusSeconds(300)}"}]""" else "[]"
        http.graphBody = """{"status":0,"data":{"connection":{"glucoseMeasurement":{"ValueInMgPerDl":147,"Timestamp":"$timestamp"}},"graphData":$points}}"""
        when (entry) {
            Entry.STARTUP -> vm.onScreenVisible(0)
            Entry.MANUAL -> vm.connectManually()
            Entry.POLL -> vm.refreshNow()
            Entry.SWITCH -> vm.onPersonSelected(INACTIVE)
        }
        runCurrent()
        val state = vm.uiState.value
        assertEquals(ConnectionState.Connected, state.connectionState)
        assertEquals(AuthenticationState.Authenticated, state.authenticationState)
        assertFalse(state.isLoading)
        assertEquals(if (entry == Entry.SWITCH) INACTIVE else ACTIVE, state.selectedPatientId)
        assertEquals(147, state.reading?.value)
        assertEquals(timestamp, state.reading?.timestamp)
        assertEquals(stale, state.isDataStale)
        if (stale) {
            assertTrue(state.dataConnectionState is DataConnectionState.Stale)
            assertNotNull(state.staleInfoMessage)
        } else {
            assertEquals(DataConnectionState.Live, state.dataConnectionState)
            assertNull(state.staleInfoMessage)
        }
        assertNull(state.errorMessage)
        assertNull(state.nextPollingRetryAt)
    }

    @Test fun personSwitchLocalCacheRemainsStaleWhileNetworkIsPending() = runTest(dispatcher) {
        auth.ensureAuthenticated()
        vm.onScreenVisible(0); runCurrent()
        val timestamp = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        http.graphBody = """{"status":0,"data":{"connection":{"glucoseMeasurement":{"ValueInMgPerDl":123,"Timestamp":"$timestamp"}},"graphData":[]}}"""
        repository.fetchMonitoringSnapshot(INACTIVE)
        http.gate = CompletableDeferred()
        vm.onPersonSelected(INACTIVE); runCurrent()
        assertEquals(INACTIVE, vm.uiState.value.selectedPatientId)
        assertEquals(123, vm.uiState.value.reading?.value)
        assertTrue(vm.uiState.value.isDataStale)
        assertNotEquals(DataConnectionState.Live, vm.uiState.value.dataConnectionState)
        http.gate!!.complete(Unit); runCurrent()
        assertFalse(vm.uiState.value.isDataStale)
        assertEquals(DataConnectionState.Live, vm.uiState.value.dataConnectionState)
    }

    private fun verify(entry: Entry, code: Int? = null, error: Throwable? = null, connections: Boolean = false) = runTest(dispatcher) {
        auth.ensureAuthenticated()
        if (entry == Entry.POLL || entry == Entry.SWITCH) {
            vm.onScreenVisible(0)
            runCurrent()
        }
        val before = http.graphCalls
        val logins = http.loginCalls
        http.code = code
        http.failure = error
        http.failConnections = connections
        when (entry) {
            Entry.STARTUP -> vm.onScreenVisible(0)
            Entry.MANUAL -> vm.connectManually()
            Entry.POLL -> vm.refreshNow()
            Entry.SWITCH -> vm.onPersonSelected(INACTIVE)
        }
        runCurrent()
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue("Failure must never be live", state.dataConnectionState != DataConnectionState.Live)
        assertTrue(state.isDataStale)
        assertNotEquals("Brak aktualnych danych dla wybranej osoby.", state.staleInfoMessage)
        assertEquals("Reuse valid session, no duplicate login", logins, http.loginCalls)
        assertEquals(before + if (connections) 0 else 1, http.graphCalls)
        if (entry == Entry.SWITCH) {
            assertEquals(INACTIVE, state.selectedPatientId)
            assertNull(state.reading)
            assertEquals(ACTIVE, settings.loadSettings().selectedPatientId)
            assertTrue(vm.homeHistory.value.isEmpty())
            assertTrue(vm.detailedHistory.value.isEmpty())
        }
        if (entry == Entry.POLL) {
            assertEquals(147, state.reading?.value)
            assertNotNull(db.glucoseReadingDao().getLatestByPatient(ACTIVE))
        }
        when (code) {
            401, 403 -> {
                assertEquals(ConnectionState.AuthenticationRequired, state.connectionState)
                assertEquals(AuthenticationState.AuthenticationRequired, state.authenticationState)
                assertTrue(state.pollingStatus is PollingStatus.AuthenticationRequired)
                assertFalse(settings.hasPersistedSessionData())
                assertFalse(client.hasActiveSession())
                assertNull(state.nextPollingRetryAt)
            }
            429, 430 -> {
                assertTrue(state.connectionState is ConnectionState.Locked)
                assertEquals(73, (state.connectionState as ConnectionState.Locked).retryAfterSeconds)
                assertTrue(state.retryCooldownSecondsRemaining in 72L..73L)
                assertTrue(settings.hasPersistedSessionData())
                assertTrue(client.hasActiveSession())
                assertFalse(state.canRetry)
                val requests = http.graphCalls
                vm.refreshNow(); vm.connectManually(); vm.onScreenVisible(1)
                runCurrent()
                assertEquals("Cooldown applies to every entry point", requests, http.graphCalls)
                assertTrue(vm.uiState.value.connectionState is ConnectionState.Locked)
                assertNotNull("Persisted cooldown must remain visible", vm.uiState.value.errorMessage)
            }
            else -> {
                assertTrue(settings.hasPersistedSessionData())
                assertTrue(client.hasActiveSession())
                assertNotNull("Failure must be visible with cached data too", state.errorMessage)
                if (error is LibreResponseDecodingException || error is IllegalStateException) {
                    assertNull("Unexpected data must not spin in automatic retries", state.nextPollingRetryAt)
                    assertTrue(state.canRetry)
                } else {
                    assertNotNull(state.nextPollingRetryAt)
                    assertEquals(1, state.consecutivePollingFailures)
                    if (code in listOf(500, 503)) assertTrue(state.pollingStatus is PollingStatus.ServerUnavailable)
                }
            }
        }
    }

    @Test fun expiredStoredSessionWithHistory_isOfflineAndDoesNotLogin() = runTest(dispatcher) {
        repository.fetchMonitoringSnapshot(ACTIVE)
        val saved = settings.loadPersistedSession()!!
        client.clearSession()
        settings.savePersistedSession(saved.copy(tokenExpiresAtEpochSeconds = Instant.now().epochSecond - 1))
        val logins = http.loginCalls
        val graphs = http.graphCalls
        vm.onScreenVisible(0)
        runCurrent()
        assertEquals(147, vm.uiState.value.reading?.value)
        assertEquals(ACTIVE, vm.uiState.value.selectedPatientId)
        assertEquals(AuthenticationState.AuthenticationRequired, vm.uiState.value.authenticationState)
        assertTrue(vm.uiState.value.isDataStale)
        assertNotEquals(DataConnectionState.Live, vm.uiState.value.dataConnectionState)
        assertEquals(logins, http.loginCalls)
        assertEquals(graphs, http.graphCalls)
        assertNotNull(db.glucoseReadingDao().getLatestByPatient(ACTIVE))
    }

    @Test fun missingMeasurement_keepsSelectorAndDoesNotPersistFailedChoice() = runTest(dispatcher) {
        auth.ensureAuthenticated(); vm.onScreenVisible(0); runCurrent()
        vm.onPersonSelected(INACTIVE); runCurrent()
        assertEquals(INACTIVE, vm.uiState.value.selectedPatientId)
        assertNull(vm.uiState.value.reading)
        assertEquals(2, vm.uiState.value.availablePersons.size)
        assertEquals("Brak aktualnych danych dla wybranej osoby.", vm.uiState.value.staleInfoMessage)
        assertEquals(ACTIVE, settings.loadSettings().selectedPatientId)
        vm.onPersonSelected(ACTIVE); runCurrent()
        assertEquals(147, vm.uiState.value.reading?.value)
    }

    @Test fun malformedGraphIsNotMissingMeasurement() = runTest(dispatcher) {
        auth.ensureAuthenticated()
        http.graphBody = "{}"
        vm.onScreenVisible(0); runCurrent()
        assertNotNull(vm.uiState.value.errorMessage)
        assertNotEquals("Brak aktualnych danych dla wybranej osoby.", vm.uiState.value.staleInfoMessage)
        assertNull(vm.uiState.value.nextPollingRetryAt)
    }

    @Test fun invalidTimestampIsNotFabricatedAsLiveNow() = runTest(dispatcher) {
        auth.ensureAuthenticated()
        http.graphBody = """{"status":0,"data":{"connection":{"glucoseMeasurement":{"ValueInMgPerDl":147,"Timestamp":"invalid"}},"graphData":[]}}"""
        vm.onScreenVisible(0); runCurrent()
        assertNull(vm.uiState.value.reading)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test fun stalePolling401StillInvalidatesAccount() = staleAccountFailure(401)
    @Test fun stalePolling430StillLocksAccount() = staleAccountFailure(430)
    private fun staleAccountFailure(code: Int) = runTest(dispatcher) {
        auth.ensureAuthenticated(); vm.onScreenVisible(0); runCurrent()
        http.gate = CompletableDeferred(); http.code = code
        vm.refreshNow(); runCurrent()
        val requests = http.graphCalls
        vm.onPersonSelected(INACTIVE); runCurrent()
        http.gate!!.complete(Unit); runCurrent()
        assertEquals(INACTIVE, vm.uiState.value.selectedPatientId)
        assertNull(vm.uiState.value.reading)
        assertEquals("Waiting person switch must not issue another request", requests, http.graphCalls)
        if (code == 401) assertEquals(ConnectionState.AuthenticationRequired, vm.uiState.value.connectionState)
        else assertTrue(vm.uiState.value.connectionState is ConnectionState.Locked)
    }

    @Test fun staleSuccessCannotOverwriteNewPerson() = runTest(dispatcher) {
        auth.ensureAuthenticated(); vm.onScreenVisible(0); runCurrent()
        http.gate = CompletableDeferred()
        vm.refreshNow(); runCurrent()
        vm.onPersonSelected(INACTIVE); runCurrent()
        http.gate!!.complete(Unit); runCurrent()
        assertEquals(INACTIVE, vm.uiState.value.selectedPatientId)
        assertNull(vm.uiState.value.reading)
        assertEquals(ACTIVE, settings.loadSettings().selectedPatientId)
    }

    @Test fun cancelledManualFetchDoesNotBecomeNoDataOrAuthenticationFailure() = runTest(dispatcher) {
        auth.ensureAuthenticated()
        http.failure = CancellationException("synthetic cancellation")
        vm.connectManually(); runCurrent()
        assertTrue(client.hasActiveSession())
        assertTrue(settings.hasPersistedSessionData())
        assertNull(vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.nextPollingRetryAt)
        assertNotEquals(AuthenticationState.AuthenticationRequired, vm.uiState.value.authenticationState)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun overlappingStartupManualAndRefreshRemainSingleFlight() = runTest(dispatcher) {
        auth.ensureAuthenticated()
        http.gate = CompletableDeferred()
        vm.onScreenVisible(0); runCurrent()
        val before = http.graphCalls
        vm.connectManually(); vm.refreshNow(); runCurrent()
        assertEquals(before, http.graphCalls)
        http.gate!!.complete(Unit); runCurrent()
        assertEquals(ConnectionState.Connected, vm.uiState.value.connectionState)
    }

    @Test fun cancelledPollClearsLoadingWithoutInvalidatingSession() = runTest(dispatcher) {
        auth.ensureAuthenticated(); vm.onScreenVisible(0); runCurrent()
        http.failure = CancellationException("synthetic cancellation")
        vm.refreshNow(); runCurrent()
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(client.hasActiveSession())
        assertTrue(settings.hasPersistedSessionData())
        assertEquals(147, vm.uiState.value.reading?.value)
        assertNull(vm.uiState.value.nextPollingRetryAt)
    }

    @Test fun cancelledOutstandingSwitchReleasesMutexAndPreservesNewestIdentity() = runTest(dispatcher) {
        auth.ensureAuthenticated(); vm.onScreenVisible(0); runCurrent()
        http.gate = CompletableDeferred()
        vm.onPersonSelected(INACTIVE); runCurrent()
        vm.onPersonSelected(ACTIVE); runCurrent()
        http.gate!!.complete(Unit); runCurrent()
        assertEquals(ACTIVE, vm.uiState.value.selectedPatientId)
        assertEquals(147, vm.uiState.value.reading?.value)
        assertEquals(ACTIVE, settings.loadSettings().selectedPatientId)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun startupHandoffToNewPersonKeepsPolling() = runTest(dispatcher) {
        repository.fetchMonitoringSnapshot(ACTIVE)
        http.gate = CompletableDeferred()
        vm.onScreenVisible(0); runCurrent()
        vm.onPersonSelected(INACTIVE); runCurrent()
        http.gate!!.complete(Unit); runCurrent()
        assertEquals(INACTIVE, vm.uiState.value.selectedPatientId)
        assertNull(vm.uiState.value.reading)
        assertTrue(vm.uiState.value.isPolling)
    }

    @Test fun missingMeasurementAfterServerFailureClearsPendingRetry() = runTest(dispatcher) {
        auth.ensureAuthenticated(); vm.onScreenVisible(0); runCurrent()
        http.code = 503
        vm.onPersonSelected(INACTIVE); runCurrent()
        assertNotNull(vm.uiState.value.nextPollingRetryAt)
        http.code = null
        vm.refreshNow(); runCurrent()
        assertEquals("Brak aktualnych danych dla wybranej osoby.", vm.uiState.value.staleInfoMessage)
        assertEquals(0, vm.uiState.value.consecutivePollingFailures)
        assertNull(vm.uiState.value.nextPollingRetryAt)
        val requests = http.graphCalls
        advanceTimeBy(74_000); runCurrent()
        assertEquals("Recovered no-data must not keep a server retry", requests, http.graphCalls)
    }

    @Test fun wrappedRetryAfterSchedulesExactlyOneRetryAndNoLogin() = runTest(dispatcher) {
        auth.ensureAuthenticated(); vm.onScreenVisible(0); runCurrent()
        http.code = 503
        vm.refreshNow(); runCurrent()
        val requests = http.graphCalls
        val logins = http.loginCalls
        advanceTimeBy(72_000); runCurrent()
        assertEquals(requests, http.graphCalls)
        http.code = null
        advanceTimeBy(1_000); runCurrent()
        assertEquals(requests + 1, http.graphCalls)
        assertEquals(logins, http.loginCalls)
        assertNull(vm.uiState.value.nextPollingRetryAt)
        assertEquals(0, vm.uiState.value.consecutivePollingFailures)
    }

    @Test fun locallyExpiredLoadedSessionNeverTriggersAutomaticLogin() = runTest(dispatcher) {
        auth.ensureAuthenticated(); vm.onScreenVisible(0); runCurrent()
        client.importSession(settings.loadPersistedSession()!!.copy(tokenExpiresAtEpochSeconds = 1))
        val logins = http.loginCalls
        vm.refreshNow(); runCurrent()
        assertEquals(AuthenticationState.AuthenticationRequired, vm.uiState.value.authenticationState)
        assertEquals(147, vm.uiState.value.reading?.value)
        assertEquals(logins, http.loginCalls)
        assertFalse(settings.hasPersistedSessionData())
    }

    @Test fun manualGraphFailureAfterEmailNormalizationIsNotDiscarded() = runTest(dispatcher) {
        settings.saveSettings(settings.loadSettings().copy(email = " Fixture@Example.Invalid "))
        http.code = 401
        vm.connectManually(); runCurrent()
        assertEquals(ConnectionState.AuthenticationRequired, vm.uiState.value.connectionState)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(settings.hasPersistedSessionData())
        assertEquals(1, http.loginCalls)
    }

    private fun runTest(dispatcher: TestDispatcher, body: suspend TestScope.() -> Unit) =
        kotlinx.coroutines.test.runTest(dispatcher) {
            try {
                body()
            } finally {
                // Cancel recurring ViewModel jobs before runTest drains the virtual scheduler.
                store.clear()
                runCurrent()
            }
        }

    private fun newViewModel() {
        store = ViewModelStore()
        val owner = object : ViewModelStoreOwner { override val viewModelStore = store }
        vm = ViewModelProvider(owner, ViewModelProvider.AndroidViewModelFactory(app))[MonitoringViewModel::class.java]
        // Deterministic connectivity; no Android network callbacks or real sockets needed.
        MonitoringViewModel::class.java.getDeclaredField("networkAvailable")
            .apply { isAccessible = true }.setBoolean(vm, true)
    }

    private fun replace(name: String, value: Any) {
        LibreDisplayApp::class.java.getDeclaredField(name).apply { isAccessible = true }.set(app, value)
    }

    private enum class Entry { STARTUP, MANUAL, POLL, SWITCH }
    private class FaultHttp(private val delegate: P0RecoveryFixture.FixtureHttp = P0RecoveryFixture.FixtureHttp()) : LibreLinkUpHttp by delegate {
        var code: Int? = null
        var failure: Throwable? = null
        var failConnections = false
        var graphBody: String? = null
        var gate: CompletableDeferred<Unit>? = null
        var graphCalls = 0
        var loginCalls = 0
        override suspend fun login(baseUrl: String, request: LoginRequest): Response<JsonObject> {
            loginCalls++
            return delegate.login(baseUrl, request)
        }
        override suspend fun getConnections(baseUrl: String, token: String, accountIdHash: String): Response<JsonObject> {
            if (failConnections) fault()?.let { return it }
            return delegate.getConnections(baseUrl, token, accountIdHash)
        }
        override suspend fun getGraph(baseUrl: String, token: String, accountIdHash: String, patientId: String): Response<JsonObject> {
            graphCalls++
            gate?.await()
            if (!failConnections) fault()?.let { return it }
            graphBody?.let { return Response.success(JsonParser.parseString(it).asJsonObject) }
            return delegate.getGraph(baseUrl, token, accountIdHash, patientId)
        }
        private fun fault(): Response<JsonObject>? {
            failure?.let { throw it }
            val status = code ?: return null
            val raw = okhttp3.Response.Builder().request(okhttp3.Request.Builder().url("http://fixture.invalid").build())
                .protocol(okhttp3.Protocol.HTTP_1_1).code(status).message("synthetic")
                .header("Retry-After", "73").build()
            return Response.error("{}".toResponseBody(), raw)
        }
    }

    private companion object {
        const val ACTIVE = P0RecoveryFixture.ACTIVE
        const val INACTIVE = P0RecoveryFixture.INACTIVE
    }
}
