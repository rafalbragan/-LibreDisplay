package com.libredisplay.data.repository

import com.google.gson.JsonObject
import com.libredisplay.data.api.AuthCapableLibreLinkUpClient
import com.libredisplay.data.api.LibreLinkUpClient
import com.libredisplay.data.api.LibreLinkUpHttp
import com.libredisplay.data.api.LoginRequest
import com.libredisplay.data.api.PersistedLibreLinkUpSession
import com.libredisplay.data.api.RetrofitLibreLinkUpClient
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.time.Instant

class GlucoseRepositoryTest {

    @Test
    fun fetchLatestReading_usesMockClientWhenEnabled() = runTest {
        val mockClient = FakeReadableClient()
        val authRepository = AuthRepository(
            settingsProvider = { AppSettings(useMock = true) },
            client = FakeAuthClient()
        )
        val repository = GlucoseRepository(
            settingsProvider = { AppSettings(useMock = true) },
            authRepository = authRepository,
            productionClient = RetrofitLibreLinkUpClient(http = FakeHttp()),
            mockClient = mockClient
        )

        val reading = repository.fetchLatestReading()

        assertEquals(111, reading?.value)
        assertEquals(1, mockClient.loginCalls)
    }

    private class FakeReadableClient : LibreLinkUpClient {
        var loginCalls = 0
        override suspend fun login(email: String, password: String) {
            loginCalls += 1
        }
        override suspend fun getConnections(): List<String> = listOf("mock")
        override suspend fun getLatestReading(): GlucoseReading =
            GlucoseReading.of(111, Instant.now(), GlucoseTrend.FLAT)
    }

    private class FakeAuthClient : AuthCapableLibreLinkUpClient {
        override suspend fun login(email: String, password: String, region: String) = Unit
        override fun hasActiveSession(): Boolean = true
        override fun clearSession() = Unit
        override fun exportSession(): PersistedLibreLinkUpSession? = null
        override fun importSession(session: PersistedLibreLinkUpSession) = Unit
    }

    private class FakeHttp : LibreLinkUpHttp {
        override suspend fun login(baseUrl: String, request: LoginRequest): Response<JsonObject> = Response.success(JsonObject())
        override suspend fun getConnections(baseUrl: String, token: String, accountIdHash: String): Response<JsonObject> = Response.success(JsonObject())
        override suspend fun getGraph(baseUrl: String, token: String, accountIdHash: String, patientId: String): Response<JsonObject> = Response.success(JsonObject())
        override fun buildHeadersForTest(method: String, token: String?, accountIdHash: String?): Map<String, String> = emptyMap()
    }
}
