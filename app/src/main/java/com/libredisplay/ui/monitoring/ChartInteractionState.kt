package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import java.time.Instant

data class ChartInteractionState(
    val selectedIndex: Int? = null,
    val selectedReading: GlucoseHistoryPoint? = null,
    val selectedTimestamp: Instant? = null,
    val isFullScreen: Boolean = false
)

