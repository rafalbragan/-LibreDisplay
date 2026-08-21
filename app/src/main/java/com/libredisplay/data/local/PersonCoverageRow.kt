package com.libredisplay.data.local

import java.time.Instant

/**
 * Aggregated per-person coverage counters for selected rolling windows.
 */
data class PersonCoverageRow(
    val patientId: String,
    val displayName: String,
    val firstTimestamp: Instant,
    val lastTimestamp: Instant,
    val readings14d: Long,
    val readings30d: Long,
    val readings60d: Long,
    val readings90d: Long,
    val readings360d: Long
)

