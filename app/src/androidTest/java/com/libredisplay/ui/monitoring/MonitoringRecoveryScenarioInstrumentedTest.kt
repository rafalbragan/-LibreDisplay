package com.libredisplay.ui.monitoring

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.libredisplay.data.model.LibreConnectionPerson
import com.libredisplay.data.repository.selectPersonForSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MonitoringRecoveryScenarioInstrumentedTest {

    @Test
    fun noDataSwitchRestart_restoresActivePersonOnDevice() {
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

        val activePersons = listOf(
            LibreConnectionPerson(patientId = "active-1", displayName = "Osoba Aktywna"),
            LibreConnectionPerson(patientId = "active-2", displayName = "Osoba Aktywna 2")
        )

        val restored = selectPersonForSnapshot(
            persons = activePersons,
            preferredPatientId = null,
            storedPatientId = noDataState.selectedPatientId
        )

        assertEquals("active-1", restored.patientId)
    }
}

