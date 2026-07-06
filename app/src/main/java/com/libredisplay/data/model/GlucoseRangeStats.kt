package com.libredisplay.data.model

import java.time.Instant

data class GlucoseRangeStats(
    val minimum: Int? = null,
    val maximum: Int? = null,
    val pointCount: Int = 0,
    val oldestTimestamp: Instant? = null,
    val newestTimestamp: Instant? = null,
    val outsideWindowCount: Int = 0,
    val usedPoints: List<GlucoseHistoryPoint> = emptyList()
)

fun calculateLast12HoursStats(
    history: List<GlucoseHistoryPoint>,
    current: GlucoseHistoryPoint?,
    now: Instant
): GlucoseRangeStats {
    val historyCandidates = history
        .asSequence()
        .filter { it.value > 0 }
        .distinctBy { it.timestamp to it.value }
        .toList()

    val includeCurrent = current != null &&
        current.value > 0 &&
        historyCandidates.isNotEmpty() &&
        historyCandidates.none { it.timestamp == current.timestamp && it.value == current.value }

    val candidates = if (includeCurrent) historyCandidates + current else historyCandidates
    if (candidates.isEmpty()) {
        return GlucoseRangeStats()
    }

    val newest = candidates.maxByOrNull { it.timestamp }?.timestamp ?: now
    val oldestAllowed = newest.minusSeconds(12 * 60 * 60)
    val windowed = candidates
        .asSequence()
        .filter { it.timestamp >= oldestAllowed && it.timestamp <= newest }
        .sortedBy { it.timestamp }
        .toList()

    if (windowed.isEmpty()) {
        return GlucoseRangeStats(outsideWindowCount = candidates.size)
    }

    return GlucoseRangeStats(
        minimum = windowed.minOf { it.value },
        maximum = windowed.maxOf { it.value },
        pointCount = windowed.size,
        oldestTimestamp = windowed.first().timestamp,
        newestTimestamp = windowed.last().timestamp,
        outsideWindowCount = candidates.size - windowed.size,
        usedPoints = windowed
    )
}

