package com.libredisplay.ui.monitoring

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.api.LibreLinkUpHttp
import com.libredisplay.data.api.LoginRequest
import com.libredisplay.data.api.RetrofitLibreLinkUpClient
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.repository.AuthRepository
import com.libredisplay.data.repository.GlucoseRepository
import com.libredisplay.data.repository.GlucoseSyncRepository
import com.libredisplay.data.repository.SettingsLoginStateStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import retrofit2.Response
import java.time.Instant

/** Test-only transport injection; Activity, ViewModel, settings, parser and Room remain real. */
class P0RecoveryFixture(val app: LibreDisplayApp) {
    val http = FixtureHttp()
    private val client = RetrofitLibreLinkUpClient(http = http)
    private val auth = AuthRepository(
        settingsProvider = { app.settingsRepository.loadSettings() },
        client = client,
        loginStateStore = SettingsLoginStateStore(app.settingsRepository)
    )

    init {
        replace("productionClient", client)
        replace("authRepository", auth)
        replace("glucoseRepository", GlucoseRepository(
            settingsProvider = { app.settingsRepository.loadSettings() },
            authRepository = auth,
            productionClient = client,
            localHistoryRepository = app.localGlucoseHistoryRepository
        ))
        replace("glucoseSyncRepository", GlucoseSyncRepository(
            settingsProvider = { app.settingsRepository.loadSettings() },
            authRepository = auth,
            productionClient = client,
            localRepository = app.localGlucoseHistoryRepository
        ))
    }

    /** Only called by the first phase; restart phases MUST keep the stored session/selection/DB. */
    fun seedActiveSession() {
        app.settingsRepository.saveSettings(AppSettings(
            appMode = AppMode.LIVE,
            email = "fixture@example.invalid",
            password = "synthetic-not-a-credential",
            regionMode = "CUSTOM",
            customBaseUrl = "http://127.0.0.1:9",
            selectedPatientId = ACTIVE,
            backgroundServiceEnabled = false,
            refreshInterval = 300
        ))
        app.settingsRepository.clearRateLimitState()
        app.settingsRepository.setRestorePromptAcknowledged(true)
        runBlocking {
            // Only synthetic inactive rows, only in initial phases; never during process restore.
            app.database.glucoseReadingDao().deleteReadingsForPerson(INACTIVE)
            auth.ensureAuthenticated()
        }
        check(app.settingsRepository.hasPersistedSessionData()) {
            "Synthetic login must persist a session before Activity launch"
        }
    }

    private fun replace(name: String, value: Any) {
        LibreDisplayApp::class.java.getDeclaredField(name).apply { isAccessible = true }.set(app, value)
    }

    class FixtureHttp : LibreLinkUpHttp {
        var inactiveHasStaleReading = false
        var graphGate: CompletableDeferred<Unit>? = null
        var gatedPatient: String? = null
        val graphRequests = java.util.concurrent.CopyOnWriteArrayList<String>()
        private val now = Instant.now()
        // Stable across process restarts, with API precision finer than Room's milliseconds.
        private val staleAt = now.minusSeconds(42L * 86400)
            .truncatedTo(java.time.temporal.ChronoUnit.DAYS).plusNanos(456_789)

        override suspend fun login(baseUrl: String, request: LoginRequest) = json(
            """{"status":0,"data":{"authTicket":{"token":"synthetic-session"},"user":{"id":"fixture-user"}}}"""
        )

        override suspend fun getConnections(baseUrl: String, token: String, accountIdHash: String) = json(
            """{"status":0,"data":[
                {"patientId":"$ACTIVE","firstName":"Aktywna","lastName":"Testowa"},
                {"patientId":"$INACTIVE","firstName":"Nieaktywna","lastName":"Testowa",
                 "glucoseMeasurement":{"ValueInMgPerDl":231,"Timestamp":"$staleAt"}}
            ]}"""
        )

        override suspend fun getGraph(baseUrl: String, token: String, accountIdHash: String, patientId: String): Response<JsonObject> {
            graphRequests += patientId
            if (gatedPatient == patientId) graphGate?.await()
            if (patientId == INACTIVE) {
                return if (inactiveHasStaleReading) json(
                    """{"status":0,"data":{"connection":{"glucoseMeasurement":${point(231, staleAt)}},"graphData":[]}}"""
                ) else json("""{"status":0,"data":{"connection":{},"graphData":[]}}""")
            }
            check(patientId == ACTIVE) { "Unexpected fixture patient" }
            val history = (48 downTo 0).joinToString(",") { point(147, now.minusSeconds(it * 900L)) }
            return json("""{"status":0,"data":{"connection":{"glucoseMeasurement":${point(147, now)}},"graphData":[$history]}}""")
        }

        override fun buildHeadersForTest(method: String, token: String?, accountIdHash: String?) = emptyMap<String, String>()
        private fun point(value: Int, at: Instant) = """{"ValueInMgPerDl":$value,"Timestamp":"$at","TrendArrow":3}"""
        private fun json(body: String) = Response.success(JsonParser.parseString(body).asJsonObject)
    }

    companion object {
        const val ACTIVE = "p0-active-fixture"
        const val INACTIVE = "p0-inactive-fixture"
    }
}






