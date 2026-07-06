package com.libredisplay.data.api

import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginRequestWireFormatTest {

    @Test
    fun login_sendsExpectedHeadersAndSerializedJsonBody() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":0,"data":{"authTicket":{"token":"t"},"user":{"id":"u"}}}"""))
        server.start()
        try {
            val http = OkHttpLibreLinkUpHttp()
            val appHeaders = http.buildHeadersForTest(method = "POST")
            val request = LoginRequest(
                email = "rafal.b.ragan@gmail.com",
                password = "My_Test-P@ss 123!"
            )

            http.login(server.url("/").toString(), request)

            val recorded = server.takeRequest()
            assertEquals("POST", recorded.method)
            assertEquals("/llu/auth/login", recorded.path)

            assertFalse(appHeaders.containsKey("Accept-Encoding"))
            assertEquals("application/json", recorded.getHeader("Accept"))
            assertEquals("gzip", recorded.getHeader("Accept-Encoding"))
            assertEquals("application/json", recorded.getHeader("Content-Type"))
            assertEquals("no-cache", recorded.getHeader("Cache-Control"))
            assertEquals("no-cache", recorded.getHeader("Pragma"))
            assertEquals("0", recorded.getHeader("Expires"))
            assertEquals("Keep-Alive", recorded.getHeader("Connection"))
            assertEquals("llu.android", recorded.getHeader("Product"))
            assertEquals("4.17.0", recorded.getHeader("Version"))
            assertEquals("LibreLinkUp/4.17.0", recorded.getHeader("User-Agent"))

            assertEquals(1, recorded.headers.values("Accept").size)
            assertEquals(1, recorded.headers.values("Accept-Encoding").size)
            assertEquals(1, recorded.headers.values("Content-Type").size)
            assertFalse(recorded.headers.names().contains("Authorization"))
            assertFalse(recorded.headers.names().contains("Account-Id"))

            val rawBody = recorded.body.readUtf8()
            val bodyJson = JsonParser.parseString(rawBody).asJsonObject
            assertEquals("rafal.b.ragan@gmail.com", bodyJson.get("email").asString)
            assertEquals("My_Test-P@ss 123!", bodyJson.get("password").asString)
            assertTrue(rawBody.toByteArray(Charsets.UTF_8).isNotEmpty())
        } finally {
            server.shutdown()
        }
    }
}

