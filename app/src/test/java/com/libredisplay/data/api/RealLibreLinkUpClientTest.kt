package com.libredisplay.data.api

import com.google.gson.JsonObject
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response

class RealLibreLinkUpClientTest {

    @Test
    fun loginSuccess_readsExactTokenAndUserIdPath() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"authTicket":{"token":"abc"},"user":{"id":"user-1"}}}
            """))))
        )
        val client = RetrofitLibreLinkUpClient(http = http)

        client.login("a@b.com", "secret", "EU")

        assertEquals(1, http.loginCalls)
    }

    @Test
    fun loginMissingToken_isNonRetryable() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"user":{"id":"user-1"}}}
            """))))
        )
        val client = RetrofitLibreLinkUpClient(http = http)

        try {
            client.login("a@b.com", "secret", "EU")
            fail("Expected NonRetryableLibreLinkUpException")
        } catch (_: NonRetryableLibreLinkUpException) {
        }
        assertEquals(1, http.loginCalls)
    }

    @Test
    fun loginStatus2_isNonRetryable_andDoesNotFetchConnections() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":2,"error":{"message":"incorrect username/password"}}
            """))))
        )
        val client = RetrofitLibreLinkUpClient(http = http)

        try {
            client.login("a@b.com", "secret", "EU")
            fail("Expected NonRetryableLibreLinkUpException")
        } catch (_: NonRetryableLibreLinkUpException) {
        }
        assertEquals(1, http.loginCalls)
        assertEquals(0, http.connectionsCalls)
    }

    @Test
    fun redirect_isNonRetryable_andDoesNotPerformSecondLogin() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(
                listOf(
                    Response.success(json("""
                        {"status":0,"data":{"redirect":true,"region":"US"}}
                    """))
                )
            )
        )
        val client = RetrofitLibreLinkUpClient(http = http)

        try {
            client.login("a@b.com", "secret", "AUTO")
            fail("Expected NonRetryableLibreLinkUpException")
        } catch (_: NonRetryableLibreLinkUpException) {
        }

        assertEquals(1, http.loginCalls)
        assertEquals(listOf("https://api-eu.libreview.io/"), http.loginBaseUrls)
    }

    @Test
    fun logical429_withCode60_parsesLockoutPayload() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {
                  "status":429,
                  "data":{
                    "code":60,
                    "message":"locked",
                    "data":{"failures":3,"interval":60,"lockout":300}
                  }
                }
            """))))
        )
        val client = RetrofitLibreLinkUpClient(http = http)

        try {
            client.login("a@b.com", "secret", "EU")
            fail("Expected LibreLinkUpHttpException")
        } catch (ex: LibreLinkUpHttpException) {
            assertEquals(429, ex.statusCode)
            assertEquals(60, ex.lockoutInfo?.apiCode)
            assertEquals(3, ex.lockoutInfo?.failures)
            assertEquals(60, ex.lockoutInfo?.intervalSeconds)
            assertEquals(300, ex.lockoutInfo?.lockoutSeconds)
        }
    }

    @Test
    fun loginStatus4_isNonRetryable() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":4,"error":{"message":"terms not accepted"}}
            """))))
        )
        val client = RetrofitLibreLinkUpClient(http = http)

        try {
            client.login("a@b.com", "secret", "AUTO")
            fail("Expected NonRetryableLibreLinkUpException")
        } catch (_: NonRetryableLibreLinkUpException) {
        }
        assertEquals(1, http.loginCalls)
    }

    @Test
    fun http429_doesNotRetryConnections_andDoesNotRelogin() = runTest {
        val error = """{"status":429,"retry_after":1}"""
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"authTicket":{"token":"abc"},"user":{"id":"user-1"}}}
            """)))),
            connectionsResponses = ArrayDeque(
                listOf(
                    Response.error(429, error.toResponseBody("application/json".toMediaType()))
                )
            )
        )
        val client = RetrofitLibreLinkUpClient(http = http)
        client.login("a@b.com", "secret", "EU")

        try {
            client.getConnections()
            fail("Expected LibreLinkUpHttpException")
        } catch (_: LibreLinkUpHttpException) {
        }

        assertEquals(1, http.loginCalls)
        assertEquals(1, http.connectionsCalls)
    }

    @Test
    fun missingOrMalformedJwt_doesNotRelogin_orRetryRequest() = runTest {
        val malformedJwt = """{"error":{"message":"missing or malformed jwt"}}"""
        val http = FakeHttp(
            loginResponses = ArrayDeque(
                listOf(
                    Response.success(json("""
                        {"status":0,"data":{"authTicket":{"token":"token-1"},"user":{"id":"user-1"}}}
                    """))
                )
            ),
            connectionsResponses = ArrayDeque(
                listOf(
                    Response.error(401, malformedJwt.toResponseBody("application/json".toMediaType()))
                )
            )
        )
        val client = RetrofitLibreLinkUpClient(http = http)
        client.login("a@b.com", "secret", "EU")

        try {
            client.getConnections()
            fail("Expected LibreLinkUpHttpException")
        } catch (_: LibreLinkUpHttpException) {
        }

        assertEquals(1, http.loginCalls)
        assertEquals(1, http.connectionsCalls)
    }

    @Test
    fun connections_emptyList_throwsNonRetryable() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"authTicket":{"token":"abc"},"user":{"id":"user-1"}}}
            """)))),
            connectionsResponses = ArrayDeque(listOf(Response.success(json("""
                {"data":[]}
            """))))
        )
        val client = RetrofitLibreLinkUpClient(http = http)
        client.login("a@b.com", "secret", "EU")

        try {
            client.getConnections()
            fail("Expected NonRetryableLibreLinkUpException")
        } catch (_: NonRetryableLibreLinkUpException) {
        }
    }

    @Test
    fun graph_changedStructure_isStillParsed() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"authTicket":{"token":"abc"},"user":{"id":"user-1"}}}
            """)))),
            connectionsResponses = ArrayDeque(listOf(Response.success(json("""
                {"data":[{"patientId":"patient-1"}]}
            """)))),
            graphResponses = ArrayDeque(listOf(Response.success(json("""
                {
                  "data": {
                    "glucoseMeasurement": {
                      "value": 140,
                      "trendArrow": 3,
                      "timestamp": "2026-07-06T10:00:00Z",
                      "MeasurementUnit": "mg/dL"
                    }
                  },
                  "graphData": [
                    {"value": 120, "trendArrow": 3, "timestamp": "2026-07-06T09:45:00Z"},
                    {"value": 140, "trendArrow": 3, "timestamp": "2026-07-06T10:00:00Z"}
                  ]
                }
            """))))
        )
        val client = RetrofitLibreLinkUpClient(http = http)
        client.login("a@b.com", "secret", "EU")

        val reading = client.getLatestReading()

        assertNotNull(reading)
        assertEquals(140, reading?.value)
        assertEquals(2, reading?.history?.size)
    }

    @Test
    fun graph_parsesCurrentReading_andGraphData() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"authTicket":{"token":"abc"},"user":{"id":"user-1"}}}
            """)))),
            connectionsResponses = ArrayDeque(listOf(Response.success(json("""
                {"data":[{"patientId":"patient-1"}]}
            """)))),
            graphResponses = ArrayDeque(listOf(Response.success(json("""
                {
                  "data": {
                    "connection": {
                      "glucoseMeasurement": {
                        "Value": 123,
                        "TrendArrow": 4,
                        "FactoryTimestamp": "2026-07-06T10:00:00Z"
                      }
                    },
                    "graphData": [
                      {"Value": 120, "TrendArrow": 3, "FactoryTimestamp": "2026-07-06T09:45:00Z"},
                      {"Value": 123, "TrendArrow": 4, "FactoryTimestamp": "2026-07-06T10:00:00Z"}
                    ]
                  }
                }
            """))))
        )
        val client = RetrofitLibreLinkUpClient(http = http)
        client.login("a@b.com", "secret", "EU")

        val reading = client.getLatestReading()

        assertNotNull(reading)
        assertEquals(123, reading?.value)
        assertEquals(2, reading?.history?.size)
        assertEquals("↗", reading?.trend?.arrow)
        assertTrue((reading?.historyHoursAvailable ?: 0.0) >= 0.24)
    }

    @Test
    fun emptyGraphData_doesNotFallbackMinMaxToCurrent() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"authTicket":{"token":"abc"},"user":{"id":"user-1"}}}
            """)))),
            connectionsResponses = ArrayDeque(listOf(Response.success(json("""
                {"data":[{"patientId":"patient-1"}]}
            """)))),
            graphResponses = ArrayDeque(listOf(Response.success(json("""
                {
                  "status": 0,
                  "data": {
                    "connection": {
                      "glucoseMeasurement": {
                        "ValueInMgPerDl": 142,
                        "TrendArrow": 3,
                        "Timestamp": "2026-07-06T10:00:00Z"
                      }
                    },
                    "graphData": []
                  }
                }
            """))))
        )
        val client = RetrofitLibreLinkUpClient(http = http)
        client.login("a@b.com", "secret", "EU")

        val reading = client.getLatestReading()

        assertNotNull(reading)
        assertEquals(142, reading?.value)
        assertEquals(0, reading?.history?.size)
        assertNull(reading?.stats?.min)
        assertNull(reading?.stats?.max)
    }

    @Test
    fun graphDataValues_driveMinAndMaxNotCurrentOnly() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"authTicket":{"token":"abc"},"user":{"id":"user-1"}}}
            """)))),
            connectionsResponses = ArrayDeque(listOf(Response.success(json("""
                {"data":[{"patientId":"patient-1"}]}
            """)))),
            graphResponses = ArrayDeque(listOf(Response.success(json("""
                {
                  "status": 0,
                  "data": {
                    "connection": {
                      "glucoseMeasurement": {
                        "ValueInMgPerDl": 142,
                        "TrendArrow": 3,
                        "Timestamp": "2026-07-06T10:00:00Z"
                      }
                    },
                    "graphData": [
                      {"ValueInMgPerDl": 98, "Timestamp": "2026-07-06T08:00:00Z"},
                      {"ValueInMgPerDl": 183, "Timestamp": "2026-07-06T09:00:00Z"},
                      {"ValueInMgPerDl": 161, "Timestamp": "2026-07-06T09:30:00Z"}
                    ]
                  }
                }
            """))))
        )
        val client = RetrofitLibreLinkUpClient(http = http)
        client.login("a@b.com", "secret", "EU")

        val reading = client.getLatestReading()

        assertEquals(98, reading?.stats?.min)
        assertEquals(183, reading?.stats?.max)
        assertEquals(3, reading?.history?.size)
    }

    @Test
    fun getLatestReading_reusesCachedPatientIdWithoutExtraConnectionsCall() = runTest {
        val http = FakeHttp(
            loginResponses = ArrayDeque(listOf(Response.success(json("""
                {"status":0,"data":{"authTicket":{"token":"abc"},"user":{"id":"user-1"}}}
            """)))),
            connectionsResponses = ArrayDeque(listOf(Response.success(json("""
                {"data":[{"patientId":"patient-1"}]}
            """)))),
            graphResponses = ArrayDeque(
                listOf(
                    Response.success(json("""
                        {
                          "status": 0,
                          "data": {
                            "connection": {"glucoseMeasurement": {"ValueInMgPerDl": 120, "Timestamp": "2026-07-06T10:00:00Z"}},
                            "graphData": [{"ValueInMgPerDl": 100, "Timestamp": "2026-07-06T09:50:00Z"}]
                          }
                        }
                    """)),
                    Response.success(json("""
                        {
                          "status": 0,
                          "data": {
                            "connection": {"glucoseMeasurement": {"ValueInMgPerDl": 121, "Timestamp": "2026-07-06T10:05:00Z"}},
                            "graphData": [{"ValueInMgPerDl": 101, "Timestamp": "2026-07-06T09:55:00Z"}]
                          }
                        }
                    """))
                )
            )
        )
        val client = RetrofitLibreLinkUpClient(http = http)
        client.login("a@b.com", "secret", "EU")

        client.getLatestReading()
        client.getLatestReading()

        assertEquals(1, http.connectionsCalls)
        assertEquals(2, http.graphCalls)
        assertEquals(listOf("patient-1", "patient-1"), http.graphPatientIds)
    }

    private class FakeHttp(
        val loginResponses: ArrayDeque<Response<JsonObject>> = ArrayDeque(),
        val connectionsResponses: ArrayDeque<Response<JsonObject>> = ArrayDeque(),
        val graphResponses: ArrayDeque<Response<JsonObject>> = ArrayDeque()
    ) : LibreLinkUpHttp {
        var loginCalls = 0
        var connectionsCalls = 0
        var graphCalls = 0
        val loginBaseUrls = mutableListOf<String>()
        val graphTokens = mutableListOf<String>()
        val graphAccountIds = mutableListOf<String>()
        val graphPatientIds = mutableListOf<String>()

        override suspend fun login(baseUrl: String, request: LoginRequest): Response<JsonObject> {
            loginCalls += 1
            loginBaseUrls += baseUrl
            return loginResponses.removeFirst()
        }

        override suspend fun getConnections(baseUrl: String, token: String, accountIdHash: String): Response<JsonObject> {
            connectionsCalls += 1
            return connectionsResponses.removeFirst()
        }

        override suspend fun getGraph(baseUrl: String, token: String, accountIdHash: String, patientId: String): Response<JsonObject> {
            graphCalls += 1
            graphTokens += token
            graphAccountIds += accountIdHash
            graphPatientIds += patientId
            return graphResponses.removeFirst()
        }

        override fun buildHeadersForTest(method: String, token: String?, accountIdHash: String?): Map<String, String> = emptyMap()
    }

    private fun json(raw: String): JsonObject = com.google.gson.JsonParser.parseString(raw).asJsonObject
}
