package com.libredisplay.data.api

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.GzipSink
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LibreLinkUpGzipIntegrationTest {

    @Test
    fun gzipLoginResponse_isTransparentlyDecodedAndParsed() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setHeader("Content-Encoding", "gzip")
                .setBody(gzip("""{"status":0,"data":{"authTicket":{"token":"test-token"},"user":{"id":"u1"}}}"""))
        )
        server.start()
        try {
            val http = OkHttpLibreLinkUpHttp()
            val response = http.login(
                baseUrl = server.url("/").toString(),
                request = LoginRequest(email = "user@example.com", password = "secret")
            )

            val recorded = server.takeRequest()
            assertEquals("POST", recorded.method)
            assertEquals("gzip", recorded.getHeader("Accept-Encoding"))
            assertEquals(1, recorded.headers.values("Accept-Encoding").size)

            assertTrue(response.isSuccessful)
            assertEquals(0, response.body()?.get("status")?.asInt)
            assertEquals("test-token", response.body()?.getAsJsonObject("data")
                ?.getAsJsonObject("authTicket")?.get("token")?.asString)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun gzipLogicalStatus2_isParsedAndClassifiedAsNonRetryable() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setHeader("Content-Encoding", "gzip")
                .setBody(gzip("""{"status":2,"error":{"message":"incorrect username/password"}}"""))
        )
        server.start()
        try {
            val client = RetrofitLibreLinkUpClient(
                initialRegion = "EU",
                http = OkHttpLibreLinkUpHttp(),
                apiBaseUrlOverride = server.url("/").toString()
            )

            try {
                client.login("user@example.com", "secret", "EU")
                fail("Expected NonRetryableLibreLinkUpException")
            } catch (_: NonRetryableLibreLinkUpException) {
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun gzipRedirectResponse_isParsedWithoutSecondLoginAttempt() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setHeader("Content-Encoding", "gzip")
                .setBody(gzip("""{"status":0,"data":{"redirect":true,"region":"US"}}"""))
        )
        server.start()
        try {
            val client = RetrofitLibreLinkUpClient(
                initialRegion = "EU",
                http = OkHttpLibreLinkUpHttp(),
                apiBaseUrlOverride = server.url("/").toString()
            )

            try {
                client.login("user@example.com", "secret", "EU")
                fail("Expected NonRetryableLibreLinkUpException")
            } catch (_: NonRetryableLibreLinkUpException) {
            }

            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun gzipLogical429_lockedPayload_isParsedToHttpException() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setHeader("Content-Encoding", "gzip")
                .setBody(
                    gzip(
                        """
                        {
                          "status":429,
                          "data":{
                            "code":60,
                            "message":"locked",
                            "data":{"failures":3,"interval":60,"lockout":300}
                          }
                        }
                        """.trimIndent()
                    )
                )
        )
        server.start()
        try {
            val client = RetrofitLibreLinkUpClient(
                initialRegion = "EU",
                http = OkHttpLibreLinkUpHttp(),
                apiBaseUrlOverride = server.url("/").toString()
            )

            try {
                client.login("user@example.com", "secret", "EU")
                fail("Expected LibreLinkUpHttpException")
            } catch (ex: LibreLinkUpHttpException) {
                assertEquals(429, ex.statusCode)
                assertEquals(60, ex.lockoutInfo?.apiCode)
                assertEquals(300, ex.lockoutInfo?.lockoutSeconds)
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun plainJsonResponse_stillWorks() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"status":0,"data":{"authTicket":{"token":"plain-token"},"user":{"id":"u1"}}}""")
        )
        server.start()
        try {
            val http = OkHttpLibreLinkUpHttp()
            val response = http.login(
                baseUrl = server.url("/").toString(),
                request = LoginRequest(email = "user@example.com", password = "secret")
            )
            assertTrue(response.isSuccessful)
            assertEquals("plain-token", response.body()?.getAsJsonObject("data")
                ?.getAsJsonObject("authTicket")?.get("token")?.asString)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun gzippedBytesWithoutEncodingHeader_areFallbackDecompressedBeforeGson() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gzip("""{"status":0,"data":{"authTicket":{"token":"t"},"user":{"id":"u"}}}"""))
        )
        server.start()
        try {
            val http = OkHttpLibreLinkUpHttp()
            val response = http.login(server.url("/").toString(), LoginRequest("user@example.com", "secret"))

            assertTrue(response.isSuccessful)
            assertEquals(0, response.body()?.get("status")?.asInt)
            assertEquals("t", response.body()?.getAsJsonObject("data")
                ?.getAsJsonObject("authTicket")?.get("token")?.asString)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun malformedJson_isNotSilentlyAccepted() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{" + "\"status\":0,}")
        )
        server.start()
        try {
            val http = OkHttpLibreLinkUpHttp()
            try {
                http.login(server.url("/").toString(), LoginRequest("user@example.com", "secret"))
                fail("Expected JSON parsing failure")
            } catch (_: Exception) {
                // Expected: strict Gson parsing should fail for malformed JSON.
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun emptySuccessBody_throwsPreciseDecodingError() = runTest {
        val fake = object : LibreLinkUpHttp {
            override suspend fun login(baseUrl: String, request: LoginRequest) =
                retrofit2.Response.success<com.google.gson.JsonObject>(null)

            override suspend fun getConnections(baseUrl: String, token: String, accountIdHash: String) =
                retrofit2.Response.success<com.google.gson.JsonObject>(null)

            override suspend fun getGraph(baseUrl: String, token: String, accountIdHash: String, patientId: String) =
                retrofit2.Response.success<com.google.gson.JsonObject>(null)

            override fun buildHeadersForTest(method: String, token: String?, accountIdHash: String?): Map<String, String> = emptyMap()
        }
        val client = RetrofitLibreLinkUpClient(http = fake)

        try {
            client.login("user@example.com", "secret", "EU")
            fail("Expected LibreResponseDecodingException")
        } catch (ex: LibreResponseDecodingException) {
            assertTrue(ex.message?.contains("Pusta odpowiedz JSON", ignoreCase = true) == true)
        }
    }

    private fun gzip(value: String): Buffer {
        val result = Buffer()
        GzipSink(result).buffer().use { sink ->
            sink.writeUtf8(value)
        }
        return result
    }
}





