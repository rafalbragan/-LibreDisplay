package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MonitoringPersonSwitchGuardTest {

    @Test
    fun `result for previous person is ignored after rapid switching`() {
        assertFalse(shouldApplyPersonSwitchResult(expectedPatientId = "person-A", currentSelectedPatientId = "person-B"))
    }

    @Test
    fun `result for currently selected person is applied`() {
        assertTrue(shouldApplyPersonSwitchResult(expectedPatientId = "person-B", currentSelectedPatientId = "person-B"))
    }

    @Test
    fun `inactive person switch keeps current selection and marks stale reading deterministically`() {
        val now = Instant.parse("2026-10-17T12:00:00Z")
        val staleInactiveReading = GlucoseReading.of(
            value = 112,
            timestamp = now.minusSeconds(2 * 60 * 60),
            trend = GlucoseTrend.FLAT,
            history = emptyList()
        )

        // Path model: stale async result for old selection must be ignored after user switches person.
        assertFalse(shouldApplyPersonSwitchResult(expectedPatientId = "active-person", currentSelectedPatientId = "inactive-person"))
        assertTrue(shouldApplyPersonSwitchResult(expectedPatientId = "inactive-person", currentSelectedPatientId = "inactive-person"))
        assertEquals(ReadingFreshness.STALE, currentReadingFreshness(staleInactiveReading, now = now, staleMinutesThreshold = 30))
    }
}

