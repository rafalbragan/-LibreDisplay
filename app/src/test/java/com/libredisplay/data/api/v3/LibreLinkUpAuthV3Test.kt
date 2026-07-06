package com.libredisplay.data.api.v3

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.libredisplay.data.api.NonRetryableLibreLinkUpException
import com.libredisplay.data.model.GlucoseReading
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class LibreLinkUpAuthV3Test {

    @Test
    fun loginSuccess_status0TokenAndUserId() = runBlocking {
        val transport = FakeTransport(
            loginResponses = mutableListOf(
                Response.success(
                    json(
                        """
                        {
                          "status": 0,
                          "data": {
                            "authTicket": { "token": "token-123" },
                            "user": { "id": "user-abc" }
                          }
                        }
                        """.trimIndent()
                    )
                )
            ),
            connectionsResponse = Response.success(
                json("""{"data":[{"patientId":"PAT-1"}]}""")
            )
        )
        val auth = LibreLinkUpAuthV3(http = transport)

        val session = auth.authorize(initialRegion = "EU", email = "a@b.c", password = "secret")

        assertEquals("EU", session.region)
        assertEquals("token-123", session.token)
        assertEquals("user-abc", session.userId)
        assertEquals("PAT-1", session.patientId)
    }

    @Test
    fun loginRedirect_runsSingleRetryOnRedirectedRegion() = runBlocking {
        val transport = FakeTransport(
            loginResponses = mutableListOf(
                Response.success(json("""{"status":0,"data":{"redirect":true,"region":"US"}}""")),
                Response.success(
                    json("""{"status":0,"data":{"authTicket":{"token":"t"},"user":{"id":"u"}}}""")
                )
            ),
            connectionsResponse = Response.success(json("""{"data":[{"patientId":"PAT-US"}]}"""))
        )
        val auth = LibreLinkUpAuthV3(http = transport)

        val session = auth.authorize(initialRegion = "EU", email = "a@b.c", password = "secret")

        assertEquals(2, transport.loginCalls)
        assertEquals("https://api-us.libreview.io/", transport.lastLoginBaseUrl)
        assertEquals("US", session.region)
    }

    @Test
    fun loginStatus2_stopsFlowWithoutConnectionsAndWithoutRetry() = runBlocking {
        val transport = FakeTransport(
            loginResponses = mutableListOf(
                Response.success(json("""{"status":2,"error":{"message":"incorrect username/password"}}"""))
            )
        )
        val auth = LibreLinkUpAuthV3(http = transport)

        val error = runCatching {
            auth.authorize(initialRegion = "EU", email = "a@b.c", password = "secret")
        }.exceptionOrNull()

        assertTrue(error is NonRetryableLibreLinkUpException)
        assertEquals(1, transport.loginCalls)
        assertEquals(0, transport.connectionsCalls)
    }

    @Test
    fun accountId_isSha256OfUserId() = runBlocking {
        val transport = FakeTransport(
            loginResponses = mutableListOf(
                Response.success(
                    json("""{"status":0,"data":{"authTicket":{"token":"t"},"user":{"id":"abc"}}}""")
                )
            ),
            connectionsResponse = Response.success(json("""{"data":[{"patientId":"PAT-1"}]}"""))
        )
        val auth = LibreLinkUpAuthV3(http = transport)

        val session = auth.authorize(initialRegion = "EU", email = "a@b.c", password = "secret")

        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", session.accountIdHash)
    }

    @Test
    fun connections_usesDataZeroPatientId() = runBlocking {
        val transport = FakeTransport(
            loginResponses = mutableListOf(
                Response.success(
                    json("""{"status":0,"data":{"authTicket":{"token":"t"},"user":{"id":"abc"}}}""")
                )
            ),
            connectionsResponse = Response.success(json("""{"data":[{"patientId":"PAT-777"}]}"""))
        )
        val auth = LibreLinkUpAuthV3(http = transport)

        val session = auth.authorize(initialRegion = "EU", email = "a@b.c", password = "secret")

        assertEquals("PAT-777", session.patientId)
    }

    @Test
    fun missingPatientId_isNonRetryable() = runBlocking {
        val transport = FakeTransport(
            loginResponses = mutableListOf(
                Response.success(
                    json("""{"status":0,"data":{"authTicket":{"token":"t"},"user":{"id":"abc"}}}""")
                )
            ),
            connectionsResponse = Response.success(json("""{"data":[{"id":"NO-PATIENT"}]}"""))
        )
        val auth = LibreLinkUpAuthV3(http = transport)

        val error = runCatching {
            auth.authorize(initialRegion = "EU", email = "a@b.c", password = "secret")
        }.exceptionOrNull()

        assertTrue(error is NonRetryableLibreLinkUpException)
    }

    @Test
    fun graph_isParsedIntoGlucoseReading() = runBlocking {
        val fakeAuth = object : LibreLinkUpAuthV3Contract {
            override suspend fun authorize(initialRegion: String, email: String, password: String): LibreLinkUpSessionV3 {
                return LibreLinkUpSessionV3(
                    region = "EU",
                    baseUrl = "https://api-eu.libreview.io/",
                    token = "token-123",
                    userId = "user-1",
                    accountIdHash = "hash-1",
                    patientId = "PAT-1"
                )
            }

            override suspend fun fetchGraph(session: LibreLinkUpSessionV3): LibreLinkUpGraphV3 {
                return LibreLinkUpGraphV3(
                    patientId = session.patientId,
                    payload = json(
                        """
                        {
                          "data": {
                            "connection": {
                              "glucoseMeasurement": {
                                "Value": 149,
                                "TrendArrow": 3,
                                "FactoryTimestamp": "2026-07-05T10:15:00Z"
                              }
                            },
                            "graphData": [
                              {"Value": 140},
                              {"Value": 145}
                            ]
                          }
                        }
                        """.trimIndent()
                    )
                )
            }
        }

        val client = LibreLinkUpClientV3(initialRegion = "EU", authV3 = fakeAuth)
        client.login("a@b.c", "secret")

        val reading: GlucoseReading? = client.getLatestReading()

        assertNotNull(reading)
        assertEquals(149, reading?.value)
    }

    @Test
    fun getRequests_doNotIncludeContentTypeHeader() {
        val http = LibreLinkUpHttpV3()

        val getHeaders = http.buildHeadersForTest("GET")

        assertFalse(getHeaders.containsKey("Content-Type"))
    }

    @Test
    fun postLogin_includesContentTypeHeader() {
        val http = LibreLinkUpHttpV3()

        val postHeaders = http.buildHeadersForTest("POST")

        assertEquals("application/json", postHeaders["Content-Type"])
    }

    @Test
    fun http429_stopsFlowAndDoesNotRetryAutomatically() = runBlocking {
        val transport = FakeTransport(
            loginResponses = mutableListOf(
                Response.error(429, "{\"retry_after\":120}".toResponseBody("application/json".toMediaType()))
            )
        )
        val auth = LibreLinkUpAuthV3(http = transport)

        val error = runCatching {
            auth.authorize(initialRegion = "EU", email = "a@b.c", password = "secret")
        }.exceptionOrNull()

        assertTrue(error is NonRetryableLibreLinkUpException)
        assertEquals(1, transport.loginCalls)
        assertEquals(0, transport.connectionsCalls)
    }

    private fun json(raw: String): JsonObject = JsonParser.parseString(raw).asJsonObject

    private class FakeTransport(
        private val loginResponses: MutableList<Response<JsonObject>> = mutableListOf(),
        private val connectionsResponse: Response<JsonObject> = Response.success(jsonObject("{\"data\":[]}")),
        private val graphResponse: Response<JsonObject> = Response.success(jsonObject("{\"data\":{}}"))
    ) : LibreLinkUpTransportV3 {

        var loginCalls: Int = 0
        var connectionsCalls: Int = 0
        var graphCalls: Int = 0
        var lastLoginBaseUrl: String? = null

        override suspend fun login(baseUrl: String, request: LoginRequestV3): Response<JsonObject> {
            loginCalls++
            lastLoginBaseUrl = baseUrl
            if (loginResponses.isEmpty()) {
                throw IllegalStateException("No fake login response configured")
            }
            return loginResponses.removeAt(0)
        }

        override suspend fun getConnections(baseUrl: String, token: String, accountIdHash: String): Response<JsonObject> {
            connectionsCalls++
            return connectionsResponse
        }

        override suspend fun getGraph(baseUrl: String, token: String, accountIdHash: String, patientId: String): Response<JsonObject> {
            graphCalls++
            return graphResponse
        }

        companion object {
            private fun jsonObject(raw: String): JsonObject = JsonParser.parseString(raw).asJsonObject
        }
    }
}

