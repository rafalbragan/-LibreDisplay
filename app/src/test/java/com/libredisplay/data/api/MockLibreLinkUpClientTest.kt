package com.libredisplay.data.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockLibreLinkUpClientTest {

    @Test
    fun mockClient_generatesCurrentReading_andLongHistory() = runTest {
        val client = MockLibreLinkUpClient()
        client.login("demo@example.com", "secret")

        val reading = client.getLatestReading("demo-person-anna")!!

        assertTrue(reading.history.size >= 5760)
        assertEquals(24.0 * 60.0, reading.historyHoursAvailable, 0.01)
        assertTrue(reading.value in 68..208)
    }

    @Test
    fun mockClient_exposesDemoPersons_andSupportsPersonSpecificReadings() = runTest {
        val client = MockLibreLinkUpClient()
        client.login("demo@example.com", "secret")

        val persons = client.getConnections()
        val anna = persons.first { it.patientId == "demo-person-anna" }
        val jan = persons.first { it.patientId == "demo-person-jan" }
        val annaReading = client.getLatestReading(anna.patientId)
        val janReading = client.getLatestReading(jan.patientId)

        assertTrue(persons.size >= 5)
        assertEquals("Anna Kowalska", anna.displayName)
        assertEquals("Jan Kowalski", jan.displayName)
        assertTrue(annaReading != null)
        assertTrue(janReading != null)
        assertTrue(annaReading!!.value in 68..208)
        assertTrue(janReading!!.value in 68..208)
        assertTrue(annaReading.history.isNotEmpty())
        assertTrue(janReading.history.isNotEmpty())
    }
}
