package com.libredisplay.ui.monitoring

import org.junit.Assert.assertEquals
import org.junit.Test

class HistorySelectorModelTest {

    @Test
    fun historySelectableRanges_preservesExpectedOrder() {
        assertEquals(
            listOf(
                TimeRange.LAST_3_HOURS,
                TimeRange.LAST_6_HOURS,
                TimeRange.LAST_12_HOURS,
                TimeRange.LAST_24_HOURS,
                TimeRange.LAST_7_DAYS,
                TimeRange.LAST_30_DAYS,
                TimeRange.LAST_90_DAYS,
                TimeRange.LAST_365_DAYS
            ),
            historySelectableRanges()
        )
    }
}

