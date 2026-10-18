package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.LibreConnectionPerson
import com.libredisplay.data.repository.selectPersonForSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringRestartRecoveryScenarioTest {

    @Test
    fun noDataSwitchRestart_returnsToActivePersonSafely() {
        val noDataState = MonitoringUiState(
            selectedPatientId = "inactive-old-person",
            selectedPersonName = "Osoba nieaktywna"
        ).withClearedReading(
            staleMessage = "Brak aktualnych danych dla wybranej osoby",
            keepLoading = false
        )

        assertNull(noDataState.reading)
        assertEquals(HistoryStatus.Empty, noDataState.historyStatus)
        assertTrue(noDataState.isDataStale)

        val personsAfterRestart = listOf(
            LibreConnectionPerson(patientId = "active-1", displayName = "Osoba Aktywna"),
            LibreConnectionPerson(patientId = "active-2", displayName = "Osoba Aktywna 2")
        )

        val selectedAfterRestart = selectPersonForSnapshot(
            persons = personsAfterRestart,
            preferredPatientId = null,
            storedPatientId = noDataState.selectedPatientId
        )

        assertEquals("active-1", selectedAfterRestart.patientId)
    }
}

