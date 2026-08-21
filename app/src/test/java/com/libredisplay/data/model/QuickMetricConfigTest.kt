package com.libredisplay.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickMetricConfigTest {

    @Test
    fun normalizeOrder_keepsProvidedAndAppendsMissing() {
        val input = listOf(QuickMetricId.GMI, QuickMetricId.BELOW)

        val normalized = QuickMetricId.normalizeOrder(input)

        assertEquals(
            listOf(
                QuickMetricId.GMI,
                QuickMetricId.BELOW,
                QuickMetricId.IN_RANGE,
                QuickMetricId.ABOVE,
                QuickMetricId.HBA1C,
                QuickMetricId.AVERAGE,
                QuickMetricId.SENSOR_ACTIVITY
            ),
            normalized
        )
    }

    @Test
    fun fromStorageId_returnsNullForUnknownValue() {
        assertEquals(null, QuickMetricId.fromStorageId("unknown"))
    }
}

