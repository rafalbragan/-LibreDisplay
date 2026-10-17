package com.libredisplay.data.repository

import com.libredisplay.data.model.LibreConnectionPerson
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectedPersonRestoreFallbackTest {

    @Test
    fun selectPersonForSnapshot_fallsBackToFirstWhenStoredSelectionIsMissing() {
        val persons = listOf(
            LibreConnectionPerson(patientId = "active-1", displayName = "Osoba Aktywna 1"),
            LibreConnectionPerson(patientId = "active-2", displayName = "Osoba Aktywna 2")
        )

        val selected = selectPersonForSnapshot(
            persons = persons,
            preferredPatientId = null,
            storedPatientId = "inactive-old-person"
        )

        assertEquals("active-1", selected.patientId)
    }
}

