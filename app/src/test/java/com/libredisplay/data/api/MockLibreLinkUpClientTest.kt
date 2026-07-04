package com.libredisplay.data.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class MockLibreLinkUpClientTest {

    @Test
    fun latestReading_isWithinExpectedRange() = runBlocking {
        val client = MockLibreLinkUpClient()
        client.login("caregiver@example.com", "secret")
        val reading = client.getLatestReading()

        assertTrue("value should be >= 60", reading.value >= 60)
        assertTrue("value should be <= 250", reading.value <= 250)
    }
}

