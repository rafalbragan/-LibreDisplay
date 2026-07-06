package com.libredisplay.data.model

import java.time.Instant

/** Single historical glucose point used by the caregiver dashboard chart. */
data class GlucoseHistoryPoint(
    val value: Int,
    val timestamp: Instant,
    val trend: GlucoseTrend = GlucoseTrend.UNKNOWN
)

