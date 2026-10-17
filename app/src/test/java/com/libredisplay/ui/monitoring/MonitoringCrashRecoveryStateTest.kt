package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MonitoringCrashRecoveryStateTest {

    @Test
    fun withClearedReading_resetsPersonSpecificReadingAndHistoryState() {
        val now = Instant.parse("2026-10-17T20:00:00Z")
        val reading = GlucoseReading.of(
            value = 145,
            timestamp = now.minusSeconds(60),
            trend = GlucoseTrend.RISING,
            history = emptyList()
        )
        val initial = MonitoringUiState(
            reading = reading,
            currentGlucose = "145",
            currentTimestamp = "10 min temu",
            historyStatus = HistoryStatus.Available,
            lastSuccessfulFetchAt = now,
            consecutivePollingFailures = 2,
            isLoading = true
        )

        val cleared = initial.withClearedReading(
            staleMessage = "Brak aktualnych danych",
            keepLoading = false
        )

        assertNull(cleared.reading)
        assertEquals("", cleared.currentGlucose)
        assertEquals(HistoryStatus.Empty, cleared.historyStatus)
        assertTrue(cleared.isDataStale)
        assertEquals("Brak aktualnych danych", cleared.staleInfoMessage)
    }
}

