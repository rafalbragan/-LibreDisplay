package com.libredisplay.data.repository

import com.libredisplay.data.model.LibreConnectionPerson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GlucoseRepositoryPersonSelectionTest {

    private fun person(id: String, name: String) = LibreConnectionPerson(
        patientId = id,
        displayName = name,
        firstName = name,
        lastName = null
    )

    @Test
    fun `returns preferred patient when available`() {
        val persons = listOf(
            person("PAT-1", "Anna"),
            person("PAT-2", "Piotr")
        )

        val selected = selectPersonForSnapshot(
            persons = persons,
            preferredPatientId = "PAT-2",
            storedPatientId = "PAT-1"
        )

        assertEquals("PAT-2", selected.patientId)
    }

    @Test
    fun `falls back to stored patient when preferred is blank`() {
        val persons = listOf(
            person("PAT-1", "Anna"),
            person("PAT-2", "Piotr")
        )

        val selected = selectPersonForSnapshot(
            persons = persons,
            preferredPatientId = "   ",
            storedPatientId = "PAT-2"
        )

        assertEquals("PAT-2", selected.patientId)
    }

    @Test
    fun `falls back to first person when selected id does not exist`() {
        val persons = listOf(
            person("PAT-1", "Anna"),
            person("PAT-2", "Piotr")
        )

        val selected = selectPersonForSnapshot(
            persons = persons,
            preferredPatientId = "PAT-404",
            storedPatientId = null
        )

        assertEquals("PAT-1", selected.patientId)
    }

    @Test
    fun `throws domain exception when persons list is empty`() {
        assertThrows(NoActivePersonsException::class.java) {
            selectPersonForSnapshot(
                persons = emptyList(),
                preferredPatientId = "PAT-1",
                storedPatientId = null
            )
        }
    }
}

