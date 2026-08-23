package com.libredisplay.data.backup

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Describes how much history LibreCare actually holds for one monitored person.
 *
 * The user must be able to decide - before anything is written - whose data is offered,
 * for which period, and whether that period is continuous or full of gaps.
 */
data class PersonDataCoverage(
    val patientId: String,
    val displayName: String,
    val readingsCount: Int,
    val firstTimestamp: Instant?,
    val lastTimestamp: Instant?,
    val expectedReadings: Int,
    val missingPercent: Int,
    val distinctDays: Int
) {
    /** True when a meaningful part of the period is not covered by readings. */
    val hasGaps: Boolean get() = missingPercent >= BackupCoverageCalculator.GAP_THRESHOLD_PERCENT

    val isEmpty: Boolean get() = readingsCount == 0
}

/**
 * Pure, unit-testable coverage math.
 *
 * LibreLinkUp publishes one value roughly every 5 minutes, so the expected number of readings for
 * a period is `period / 5 min`. Everything missing from that ideal count is reported as a gap.
 */
object BackupCoverageCalculator {

    const val GAP_THRESHOLD_PERCENT = 3

    val DEFAULT_CADENCE: Duration = Duration.ofMinutes(5)

    fun forPerson(
        patientId: String,
        displayName: String,
        timestamps: List<Instant>,
        cadence: Duration = DEFAULT_CADENCE,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PersonDataCoverage {
        val sorted = timestamps.distinct().sorted()
        if (sorted.isEmpty()) {
            return PersonDataCoverage(
                patientId = patientId,
                displayName = displayName,
                readingsCount = 0,
                firstTimestamp = null,
                lastTimestamp = null,
                expectedReadings = 0,
                missingPercent = 0,
                distinctDays = 0
            )
        }

        val first = sorted.first()
        val last = sorted.last()
        val cadenceMillis = cadence.toMillis().coerceAtLeast(1L)
        val spanMillis = Duration.between(first, last).toMillis().coerceAtLeast(0L)
        val expected = (spanMillis / cadenceMillis).toInt() + 1
        val missingPercent = if (expected <= 0) {
            0
        } else {
            val missing = (expected - sorted.size).coerceAtLeast(0)
            ((missing * 100.0) / expected).toInt().coerceIn(0, 100)
        }

        return PersonDataCoverage(
            patientId = patientId,
            displayName = displayName,
            readingsCount = sorted.size,
            firstTimestamp = first,
            lastTimestamp = last,
            expectedReadings = expected,
            missingPercent = missingPercent,
            distinctDays = sorted.map { it.atZone(zoneId).toLocalDate() }.distinct().size
        )
    }

    /** One-line, user facing Polish description shown in the "load my data" dialog. */
    fun describe(coverage: PersonDataCoverage, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (coverage.isEmpty) return "brak zapisanych odczytów"
        val start = coverage.firstTimestamp
        val end = coverage.lastTimestamp
        val period = if (start != null && end != null) {
            "${formatDate(start, zoneId)} – ${formatDate(end, zoneId)}"
        } else {
            "nieznany okres"
        }
        val quality = if (coverage.hasGaps) {
            "z przerwami – brakuje ok. ${coverage.missingPercent}% danych z całego okresu"
        } else {
            "dane ciągłe"
        }
        return "$period · ${polishDays(coverage.distinctDays)} · ${coverage.readingsCount} odczytów · $quality"
    }

    fun polishDays(days: Int): String = when {
        days <= 0 -> "brak dni"
        days == 1 -> "1 dzień"
        else -> "$days dni"
    }

    private fun formatDate(instant: Instant, zoneId: ZoneId): String =
        DATE_FORMATTER.withZone(zoneId).format(instant)

    private val DATE_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("pl"))
}

