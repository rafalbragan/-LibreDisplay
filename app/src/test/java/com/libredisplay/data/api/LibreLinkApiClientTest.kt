package com.libredisplay.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibreLinkApiClientTest {

    private val http = OkHttpLibreLinkUpHttp()

    @Test
    fun postLogin_hasContentType() {
        val headers = http.buildHeadersForTest(method = "POST")
        assertEquals("application/json", headers["Accept"])
        assertEquals("application/json", headers["Content-Type"])
        assertFalse(headers.containsKey("Accept-Encoding"))
        assertEquals("no-cache", headers["Cache-Control"])
        assertEquals("no-cache", headers["Pragma"])
        assertEquals("0", headers["Expires"])
        assertEquals("Keep-Alive", headers["Connection"])
        assertEquals("llu.android", headers["Product"])
        assertEquals("4.17.0", headers["Version"])
        assertEquals("LibreLinkUp/4.17.0", headers["User-Agent"])
        assertFalse(headers.containsKey("Authorization"))
        assertFalse(headers.containsKey("Account-Id"))
    }

    @Test
    fun postLogin_hasSingleAcceptValue() {
        val headers = http.buildHeadersForTest(method = "POST")
        assertEquals("application/json", headers["Accept"])
        assertFalse((headers["Accept"] ?: "").contains(","))
    }

    @Test
    fun getConnections_hasNoContentType_andHasAuthHeaders() {
        val headers = http.buildHeadersForTest(
            method = "GET",
            token = "token123",
            accountIdHash = "hash123"
        )
        assertFalse(headers.containsKey("Content-Type"))
        assertEquals("Bearer token123", headers["Authorization"])
        assertEquals("hash123", headers["Account-Id"])
        assertTrue(headers.containsKey("Product"))
        assertTrue(headers.containsKey("Version"))
    }

    @Test
    fun getGraph_hasNoContentType_andHasAuthHeaders() {
        val headers = http.buildHeadersForTest(
            method = "GET",
            token = "graphToken",
            accountIdHash = "graphHash"
        )
        assertFalse(headers.containsKey("Content-Type"))
        assertEquals("Bearer graphToken", headers["Authorization"])
        assertEquals("graphHash", headers["Account-Id"])
    }
}
