package com.libredisplay.data.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockLibreLinkUpClientTest {

    @Test
    fun mockClient_generatesCurrentReading_and12HourHistory() = runTest {
        val client = MockLibreLinkUpClient()
        client.login("demo@example.com", "secret")

        val reading = client.getLatestReading()!!

        assertEquals(48, reading.history.size)
        assertEquals(12.0, reading.historyHoursAvailable, 0.01)
        assertTrue(reading.value in 75..250)
    }
}
