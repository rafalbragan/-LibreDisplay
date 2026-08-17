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

        val reading = client.getLatestReading()!!

        assertEquals(5760, reading.history.size)
        assertEquals(24.0 * 60.0, reading.historyHoursAvailable, 0.01)
        assertTrue(reading.value in 68..208)
    }

    @Test
    fun mockClient_exposesDemoPersons_andSupportsPersonSpecificReadings() = runTest {
        val client = MockLibreLinkUpClient()
        client.login("demo@example.com", "secret")

        val persons = client.getConnections()
        val annaReading = client.getLatestReading(persons[0].patientId)
        val janReading = client.getLatestReading(persons[1].patientId)

        assertEquals(3, persons.size)
        assertEquals("Anna Kowalska", persons[0].displayName)
        assertEquals("Jan Kowalski", persons[1].displayName)
        assertTrue(annaReading != null)
        assertTrue(janReading != null)
        assertTrue((annaReading?.value ?: 0) != (janReading?.value ?: 0) || annaReading?.trend != janReading?.trend)
    }
}
