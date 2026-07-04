package com.libredisplay.data.model

import java.time.Instant

/**
 * Immutable snapshot of a single glucose measurement returned by LibreLinkUp.
 *
 * @param value      Glucose value in mg/dL.
 * @param timestamp  When this reading was taken (UTC).
 * @param trend      Direction of glucose change.
 * @param isLow      True when value is below the low threshold (typically 70 mg/dL).
 * @param isHigh     True when value is above the high threshold (typically 180 mg/dL).
 */
data class GlucoseReading(
    val value: Int,
    val timestamp: Instant,
    val trend: GlucoseTrend,
    val isLow: Boolean,
    val isHigh: Boolean
) {
    companion object {
        const val LOW_THRESHOLD = 70
        const val HIGH_THRESHOLD = 180

        /** Convenience factory – computes isLow / isHigh automatically. */
        fun of(value: Int, timestamp: Instant, trend: GlucoseTrend): GlucoseReading =
            GlucoseReading(
                value = value,
                timestamp = timestamp,
                trend = trend,
                isLow = value < LOW_THRESHOLD,
                isHigh = value > HIGH_THRESHOLD
            )
    }
}

