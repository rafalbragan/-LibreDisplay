package com.libredisplay.ui.monitoring

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringPersonSwitchGuardTest {

    @Test
    fun `result for previous person is ignored after rapid switching`() {
        assertFalse(shouldApplyPersonSwitchResult(expectedPatientId = "person-A", currentSelectedPatientId = "person-B"))
    }

    @Test
    fun `result for currently selected person is applied`() {
        assertTrue(shouldApplyPersonSwitchResult(expectedPatientId = "person-B", currentSelectedPatientId = "person-B"))
    }
}

