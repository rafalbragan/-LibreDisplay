package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import java.time.Duration
import java.time.Instant

/**
 * Describes how much actual data is available versus what the user requested.
 *
 * SELECTED RANGE != AVAILABLE DATA RANGE.
 *
 * This model separates:
 *   A. REQUESTED / SELECTED RANGE  – what the user wants to see (e.g. 24h)
 *   B. AVAILABLE DATA RANGE        – what we actually have (e.g. 8h 02m)
 *
 * Rules:
 * - Never label statistics or charts with the selected range when coverage is incomplete.
 * - Show available span in all section headers.
 * - Show selected range as secondary info.
 * - Show estimated time to full coverage only when hasFullCoverage == false and we have data.
 */
data class DataCoverageModel(
    /** Duration the user selected (e.g. 24h). */
    val selectedRange: Duration,
    /** Short label for selectedRange (e.g. "24 godz."). */
    val selectedRangeLabel: String,
    /** Oldest reading we have in the filtered set. Null if no data. */
    val oldestAvailableTimestamp: Instant?,
    /** Newest reading we have in the filtered set. Null if no data. */
    val newestAvailableTimestamp: Instant?,
    /** Actual time span covered by data (newest - oldest). Zero if no data. */
    val availableSpan: Duration,
    /** True when availableSpan >= selectedRange. */
    val hasFullCoverage: Boolean,
    /**
     * How long until full coverage assuming continuous recording.
     * Null when hasFullCoverage == true or no data at all.
     */
    val timeUntilFullCoverage: Duration?
) {
    /** Human-readable label for the available span (e.g. "8 godz. 02 min"). */
    val availableSpanLabel: String
        get() = PolishDateTimeFormatter.formatNaturalDuration(availableSpan)

    /** Section header for charts and statistics. */
    val sectionHeaderLabel: String
        get() = if (hasFullCoverage) {
            selectedRangeLabel
        } else {
            "${availableSpanLabel} dostępnych danych"
        }

    /** Secondary line shown when coverage is incomplete. Null when full. */
    val selectedRangeNote: String?
        get() = if (hasFullCoverage) null else "Wybrany zakres: $selectedRangeLabel"

    /** Estimate when full coverage will be available, assuming continuous recording. */
    val fullCoverageEstimate: String?
        get() {
            val remaining = timeUntilFullCoverage ?: return null
            if (remaining.isNegative || remaining.isZero) return null
            val remaining2min = Duration.ofMinutes(remaining.toMinutes() + 1) // round up
            return "Przy ciągłym zapisie pełny zakres $selectedRangeLabel będzie dostępny za ok. ${PolishDateTimeFormatter.formatNaturalDuration(remaining)}."
        }

    companion object {
        val EMPTY = DataCoverageModel(
            selectedRange = Duration.ZERO,
            selectedRangeLabel = "—",
            oldestAvailableTimestamp = null,
            newestAvailableTimestamp = null,
            availableSpan = Duration.ZERO,
            hasFullCoverage = false,
            timeUntilFullCoverage = null
        )
    }
}

/**
 * Compute a [DataCoverageModel] from a list of history points and the user's selected range.
 *
 * @param history      All history points (filtered to the selected range at most).
 * @param selectedRange Duration requested by the user.
 * @param selectedRangeLabel Polish short label for the range (e.g. "24 godz.").
 */
fun computeDataCoverage(
    history: List<GlucoseHistoryPoint>,
    selectedRange: Duration,
    selectedRangeLabel: String
): DataCoverageModel {
    if (history.isEmpty()) {
        return DataCoverageModel(
            selectedRange = selectedRange,
            selectedRangeLabel = selectedRangeLabel,
            oldestAvailableTimestamp = null,
            newestAvailableTimestamp = null,
            availableSpan = Duration.ZERO,
            hasFullCoverage = false,
            timeUntilFullCoverage = null
        )
    }

    val oldest = history.minOf { it.timestamp }
    val newest = history.maxOf { it.timestamp }
    val span = Duration.between(oldest, newest)
    val hasFullCoverage = !span.isNegative && span >= selectedRange
    val timeUntil = if (!hasFullCoverage) {
        val needed = selectedRange - span
        if (needed.isNegative || needed.isZero) null else needed
    } else null

    return DataCoverageModel(
        selectedRange = selectedRange,
        selectedRangeLabel = selectedRangeLabel,
        oldestAvailableTimestamp = oldest,
        newestAvailableTimestamp = newest,
        availableSpan = span,
        hasFullCoverage = hasFullCoverage,
        timeUntilFullCoverage = timeUntil
    )
}

/** Short label for a [TimeRange] (used in coverage model). */
internal fun TimeRange.toSelectedRangeLabel(): String = when (this) {
    TimeRange.LAST_3_HOURS -> "3 godz."
    TimeRange.LAST_6_HOURS -> "6 godz."
    TimeRange.LAST_12_HOURS -> "12 godz."
    TimeRange.LAST_24_HOURS -> "24 godz."
    TimeRange.LAST_3_DAYS -> "3 dni"
    TimeRange.LAST_7_DAYS -> "7 dni"
    TimeRange.LAST_30_DAYS -> "30 dni"
    TimeRange.LAST_90_DAYS -> "90 dni"
    TimeRange.LAST_365_DAYS -> "rok"
}

