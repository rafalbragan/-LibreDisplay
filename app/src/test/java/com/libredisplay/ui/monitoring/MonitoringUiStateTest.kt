package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.LibreConnectionPerson
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringUiStateTest {

    @Test
    fun shouldShowPersonSwitcher_isFalseForSinglePerson() {
        val state = MonitoringUiState(
            availablePersons = listOf(
                LibreConnectionPerson(patientId = "p1", displayName = "Mama")
            )
        )

        assertFalse(state.shouldShowPersonSwitcher())
    }

    @Test
    fun shouldShowPersonSwitcher_isTrueForMultiplePersons() {
        val state = MonitoringUiState(
            availablePersons = listOf(
                LibreConnectionPerson(patientId = "p1", displayName = "Mama"),
                LibreConnectionPerson(patientId = "p2", displayName = "Tata")
            )
        )

        assertTrue(state.shouldShowPersonSwitcher())
    }
}

