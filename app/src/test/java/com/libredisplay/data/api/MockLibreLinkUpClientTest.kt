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

    @Test
    fun mockClient_exposesTwoPersons_andSupportsPersonSpecificReadings() = runTest {
        val client = MockLibreLinkUpClient()
        client.login("demo@example.com", "secret")

        val persons = client.getConnections()
        val mamaReading = client.getLatestReading(persons[0].patientId)
        val tataReading = client.getLatestReading(persons[1].patientId)

        assertTrue(persons.size >= 2)
        assertEquals("Mama", persons[0].displayName)
        assertEquals("Tata", persons[1].displayName)
        assertTrue(mamaReading != null)
        assertTrue(tataReading != null)
        assertTrue((mamaReading?.value ?: 0) != (tataReading?.value ?: 0) || mamaReading?.trend != tataReading?.trend)
    }
}
