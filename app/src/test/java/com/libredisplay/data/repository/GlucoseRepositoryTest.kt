package com.libredisplay.data.repository

import com.google.gson.JsonObject
import com.libredisplay.data.api.AuthCapableLibreLinkUpClient
import com.libredisplay.data.api.LibreLinkUpClient
import com.libredisplay.data.api.LibreLinkUpHttp
import com.libredisplay.data.api.LoginRequest
import com.libredisplay.data.api.PersistedLibreLinkUpSession
import com.libredisplay.data.api.RetrofitLibreLinkUpClient
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.data.model.LibreConnectionPerson
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.time.Instant

class GlucoseRepositoryTest {

    @Test
    fun fetchLatestReading_usesMockClientWhenEnabled() = runTest {
        val mockClient = FakeReadableClient()
        val http = FakeHttp()
        val authRepository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.DEMO) },
            client = FakeAuthClient()
        )
        val repository = GlucoseRepository(
            settingsProvider = { AppSettings(appMode = AppMode.DEMO) },
            authRepository = authRepository,
            productionClient = RetrofitLibreLinkUpClient(http = http),
            mockClient = mockClient
        )

        val reading = repository.fetchLatestReading()

        assertEquals(111, reading?.value)
        assertEquals(1, mockClient.loginCalls)
        assertTrue(http.graphPatientIds.isEmpty())
    }

    @Test
    fun fetchMonitoringSnapshot_liveModeDoesNotUseMockClient() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"authTicket":{"token":"abc"},"user":{"id":"user-1"}}}
            """)))),
            connectionsResponses = ArrayDeque(listOf(Response.success(json("""
                {"data":[{"patientId":"patient-live","firstName":"Live"}]}
            """)))),
            graphResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"connection":{"glucoseMeasurement":{"ValueInMgPerDl":121,"Timestamp":"2026-07-06T10:00:00Z"}},"graphData":[{"ValueInMgPerDl":121,"Timestamp":"2026-07-06T10:00:00Z"}]}}
            """))))
        )
        val mockClient = FakeReadableClient()
        val client = RetrofitLibreLinkUpClient(http = http)
        val authRepository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = client
        )
        val repository = GlucoseRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            authRepository = authRepository,
            productionClient = client,
            mockClient = mockClient
        )

        val snapshot = repository.fetchMonitoringSnapshot()

        assertEquals("patient-live", snapshot.selectedPerson.patientId)
        assertEquals(0, mockClient.loginCalls)
    }

    @Test
    fun mockClient_supportsMultiplePersonsAndSwitching() = runTest {
        val mockClient = com.libredisplay.data.api.MockLibreLinkUpClient()
        mockClient.login("mock@libredisplay.local", "mock")

        val persons = mockClient.getConnections()
        val anna = persons.first { it.displayName == "Anna Kowalska" }
        val jan = persons.first { it.displayName == "Jan Kowalski" }

        val annaReading = mockClient.getLatestReading(anna.patientId)
        val janReading = mockClient.getLatestReading(jan.patientId)

        assertEquals(5, persons.size)
        assertTrue(annaReading != null)
        assertTrue(janReading != null)
        assertTrue(annaReading!!.value in 68..208)
        assertTrue(janReading!!.value in 68..208)
        assertTrue(annaReading.history.isNotEmpty())
        assertTrue(janReading.history.isNotEmpty())
    }

    @Test
    fun fetchMonitoringSnapshot_usesStoredSelectionWhenAvailable() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"authTicket":{"token":"abc"},"user":{"id":"user-1"}}}
            """)))),
            connectionsResponses = ArrayDeque(listOf(Response.success(json("""
                {"data":[
                  {"patientId":"patient-mama","firstName":"Mama"},
                  {"patientId":"patient-tata","firstName":"Tata"}
                ]}
            """)))),
            graphResponses = ArrayDeque(listOf(Response.success(json("""
                {
                  "status":0,
                  "data": {
                    "connection": {"glucoseMeasurement": {"ValueInMgPerDl": 122, "Timestamp": "2026-07-06T10:00:00Z"}},
                    "graphData": [{"ValueInMgPerDl": 122, "Timestamp": "2026-07-06T10:00:00Z"}]
                  }
                }
            """))))
        )
        val client = RetrofitLibreLinkUpClient(http = http)
        val authRepository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret", selectedPatientId = "patient-tata") },
            client = client
        )
        val repository = GlucoseRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret", selectedPatientId = "patient-tata") },
            authRepository = authRepository,
            productionClient = client
        )

        val snapshot = repository.fetchMonitoringSnapshot()

        assertEquals("patient-tata", snapshot.selectedPerson.patientId)
        assertEquals("Tata", snapshot.selectedPerson.displayName)
        assertEquals(listOf("patient-tata"), http.graphPatientIds)
    }

    @Test
    fun fetchMonitoringSnapshot_fallsBackToFirstWhenStoredSelectionMissing() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"authTicket":{"token":"abc"},"user":{"id":"user-1"}}}
            """)))),
            connectionsResponses = ArrayDeque(listOf(Response.success(json("""
                {"data":[
                  {"patientId":"patient-mama","firstName":"Mama"},
                  {"patientId":"patient-tata","firstName":"Tata"}
                ]}
            """)))),
            graphResponses = ArrayDeque(listOf(Response.success(json("""
                {
                  "status":0,
                  "data": {
                    "connection": {"glucoseMeasurement": {"ValueInMgPerDl": 130, "Timestamp": "2026-07-06T10:00:00Z"}},
                    "graphData": [{"ValueInMgPerDl": 130, "Timestamp": "2026-07-06T10:00:00Z"}]
                  }
                }
            """))))
        )
        val client = RetrofitLibreLinkUpClient(http = http)
        val authRepository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret", selectedPatientId = "missing") },
            client = client
        )
        val repository = GlucoseRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret", selectedPatientId = "missing") },
            authRepository = authRepository,
            productionClient = client
        )

        val snapshot = repository.fetchMonitoringSnapshot()

        assertEquals("patient-mama", snapshot.selectedPerson.patientId)
        assertEquals(listOf("patient-mama"), http.graphPatientIds)
    }

    @Test
    fun fetchMonitoringSnapshot_usesExplicitPatientIdForGraph() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"authTicket":{"token":"abc"},"user":{"id":"user-1"}}}
            """)))),
            connectionsResponses = ArrayDeque(listOf(Response.success(json("""
                {"data":[
                  {"patientId":"patient-mama","firstName":"Mama"},
                  {"patientId":"patient-tata","firstName":"Tata"}
                ]}
            """)))),
            graphResponses = ArrayDeque(listOf(Response.success(json("""
                {
                  "status":0,
                  "data": {
                    "connection": {"glucoseMeasurement": {"ValueInMgPerDl": 140, "Timestamp": "2026-07-06T10:00:00Z"}},
                    "graphData": [{"ValueInMgPerDl": 140, "Timestamp": "2026-07-06T10:00:00Z"}]
                  }
                }
            """))))
        )
        val client = RetrofitLibreLinkUpClient(http = http)
        val authRepository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = client
        )
        val repository = GlucoseRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            authRepository = authRepository,
            productionClient = client
        )

        val snapshot = repository.fetchMonitoringSnapshot(preferredPatientId = "patient-tata")

        assertEquals("patient-tata", snapshot.selectedPerson.patientId)
        assertEquals(listOf("patient-tata"), http.graphPatientIds)
    }

    @Test
    fun fetchMonitoringSnapshot_whenSelectedPersonHasNoData_throwsSelectedPersonGraphException() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"authTicket":{"token":"abc"},"user":{"id":"user-1"}}}
            """)))),
            connectionsResponses = ArrayDeque(listOf(Response.success(json("""
                {"data":[
                  {"patientId":"patient-mama","firstName":"Mama"},
                  {"patientId":"patient-tata","firstName":"Tata"}
                ]}
            """)))),
            graphResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"connection":{},"graphData":[]}}
            """))))
        )
        val client = RetrofitLibreLinkUpClient(http = http)
        val authRepository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = client
        )
        val repository = GlucoseRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            authRepository = authRepository,
            productionClient = client
        )

        val exception = runCatching {
            repository.fetchMonitoringSnapshot(preferredPatientId = "patient-tata")
        }.exceptionOrNull()

        assertTrue(exception is SelectedPersonGraphException)
        val selectedPersonException = exception as SelectedPersonGraphException
        assertEquals("patient-tata", selectedPersonException.selectedPerson.patientId)
        assertNotNull(exception.cause)
        assertEquals(listOf("patient-tata"), http.graphPatientIds)
    }

    private class FakeReadableClient : LibreLinkUpClient {
        var loginCalls = 0
        override suspend fun login(email: String, password: String) {
            loginCalls += 1
        }
        override suspend fun getConnections(): List<LibreConnectionPerson> = listOf(
            LibreConnectionPerson(patientId = "mock", displayName = "Mock")
        )
        override suspend fun getLatestReading(patientId: String?): GlucoseReading? =
            GlucoseReading.of(111, Instant.now(), GlucoseTrend.FLAT)
    }

    private class FakeAuthClient : AuthCapableLibreLinkUpClient {
        override suspend fun login(email: String, password: String, region: String) = Unit
        override fun hasActiveSession(): Boolean = true
        override fun clearSession() = Unit
        override fun exportSession(): PersistedLibreLinkUpSession? = null
        override fun importSession(session: PersistedLibreLinkUpSession) = Unit
    }

    private class FakeHttp(
        private val loginResponses: ArrayDeque<Response<JsonObject>> = ArrayDeque(),
        private val connectionsResponses: ArrayDeque<Response<JsonObject>> = ArrayDeque(),
        private val graphResponses: ArrayDeque<Response<JsonObject>> = ArrayDeque()
    ) : LibreLinkUpHttp {
        val graphPatientIds = mutableListOf<String>()

        override suspend fun login(baseUrl: String, request: LoginRequest): Response<JsonObject> =
            loginResponses.removeFirstOrNull() ?: Response.success(JsonObject())

        override suspend fun getConnections(baseUrl: String, token: String, accountIdHash: String): Response<JsonObject> =
            connectionsResponses.removeFirstOrNull() ?: Response.success(JsonObject())

        override suspend fun getGraph(baseUrl: String, token: String, accountIdHash: String, patientId: String): Response<JsonObject> {
            graphPatientIds += patientId
            return graphResponses.removeFirstOrNull() ?: Response.success(JsonObject())
        }

        override fun buildHeadersForTest(method: String, token: String?, accountIdHash: String?): Map<String, String> = emptyMap()
    }

    private fun json(raw: String): JsonObject = com.google.gson.JsonParser.parseString(raw).asJsonObject
}
