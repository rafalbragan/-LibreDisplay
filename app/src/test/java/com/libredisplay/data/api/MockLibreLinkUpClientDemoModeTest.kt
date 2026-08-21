package com.libredisplay.data.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class MockLibreLinkUpClientDemoModeTest {

    private val client = MockLibreLinkUpClient()

    @Test
    fun demoMode_startsWithoutAuthentication() = runTest {
        val persons = client.getConnections()
        val snapshot = client.getLatestReading(persons.first().patientId)

        assertTrue(persons.isNotEmpty())
        assertNotNull(snapshot)
    }

    @Test
    fun demoMode_loadsStableDemoPeople() = runTest {
        val persons = client.getConnections()

        assertEquals(5, persons.size)
        assertTrue(persons.any { it.patientId == "demo-person-anna" && it.displayName == "Anna Kowalska" })
        assertTrue(persons.any { it.patientId == "demo-person-jan" && it.displayName == "Jan Kowalski" })
        assertTrue(persons.any { it.patientId == "demo-person-maria" && it.displayName == "Maria Nowak" })
        assertTrue(persons.any { it.patientId == "demo-person-piotr" && it.displayName == "Piotr Zielinski" })
        assertTrue(persons.any { it.patientId == "demo-person-zofia" && it.displayName == "Zofia Wisniewska" })
    }

    @Test
    fun demoMode_generatesAboutTwoMonthsOfHistoryPerPerson() = runTest {
        val persons = client.getConnections()
        val anna = client.getLatestReading(persons.first { it.patientId == "demo-person-anna" }.patientId)!!
        val jan = client.getLatestReading(persons.first { it.patientId == "demo-person-jan" }.patientId)!!

        assertTrue(anna.history.size >= 5_700)
        assertTrue(jan.history.size >= 5_700)

        val annaDurationDays = Duration.between(anna.history.first().timestamp, anna.history.last().timestamp).toDays()
        val janDurationDays = Duration.between(jan.history.first().timestamp, jan.history.last().timestamp).toDays()

        assertTrue(annaDurationDays >= 59)
        assertTrue(janDurationDays >= 59)
        assertTrue(anna.history.any { it.value != jan.history.first().value })
    }
}

