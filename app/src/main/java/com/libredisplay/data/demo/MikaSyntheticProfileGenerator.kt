package com.libredisplay.data.demo

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseHistoryStats
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Deterministic synthetic fixture for DEMO profile "Mika".
 * Generates 18 months of realistic-looking data for app testing only.
 */
object MikaSyntheticProfileGenerator {
    const val MIKA_PATIENT_ID = "demo-person-mika"
    private const val SAMPLE_INTERVAL_MINUTES = 30L
    private const val HISTORY_MONTHS = 18L
    private const val FIXED_SEED = 20261017L

    fun generate(now: Instant): GlucoseReading {
        val anchorDate = now.atZone(ZoneOffset.UTC).toLocalDate()
        val end = anchorDate.atTime(23, 30).toInstant(ZoneOffset.UTC)
        val start = anchorDate.minusMonths(HISTORY_MONTHS).atStartOfDay().toInstant(ZoneOffset.UTC)
        val points = buildSequence(start = start, end = end)
        val latest = points.last()
        val stats = GlucoseHistoryStats.from(points, 80, 180)
        return GlucoseReading.of(
            value = latest.value,
            timestamp = latest.timestamp,
            trend = latest.trend,
            history = points,
            stats = stats,
            historyHoursAvailable = HISTORY_MONTHS * 30.0 * 24.0
        )
    }

    private fun buildSequence(start: Instant, end: Instant): List<GlucoseHistoryPoint> {
        val points = ArrayList<GlucoseHistoryPoint>(24_000)
        var index = 0
        var ts = start
        while (!ts.isAfter(end)) {
            val minuteOfDay = ((ts.epochSecond % 86_400L) / 60.0)
            val day = index / ((24 * 60) / SAMPLE_INTERVAL_MINUTES).toInt()

            val circadian = 16.0 * sin((2.0 * PI * minuteOfDay / 1440.0) + 0.75)
            val breakfast = gaussian(minuteOfDay, 7.75 * 60.0, 80.0, 20.0)
            val lunch = gaussian(minuteOfDay, 13.1 * 60.0, 95.0, 26.0)
            val dinner = gaussian(minuteOfDay, 19.0 * 60.0, 105.0, 24.0)
            val weekly = 10.0 * sin((2.0 * PI * day / 7.0) + 0.25)
            val monthly = 6.0 * cos((2.0 * PI * day / 30.0) + 1.3)
            val stableBlock = if ((day % 17) in 10..14) -4.0 else 0.0
            val eventRise = if ((index + FIXED_SEED).mod(777L) == 0L) 20.0 else 0.0
            val eventDrop = if ((index + FIXED_SEED).mod(991L) == 0L) -16.0 else 0.0
            val jitter = (((FIXED_SEED + index * 31L) % 11L) - 5L).toDouble()

            val raw = 112.0 + circadian + breakfast + lunch + dinner + weekly + monthly + stableBlock + eventRise + eventDrop + jitter
            val value = raw.toInt().coerceIn(62, 235)
            val trendSignal =
                0.9 * cos((2.0 * PI * minuteOfDay / 1440.0) + 0.75) +
                    1.3 * gaussianSlope(minuteOfDay, 7.75 * 60.0, 80.0) +
                    1.6 * gaussianSlope(minuteOfDay, 13.1 * 60.0, 95.0) +
                    1.4 * gaussianSlope(minuteOfDay, 19.0 * 60.0, 105.0)

            points += GlucoseHistoryPoint(
                value = value,
                timestamp = ts,
                trend = GlucoseTrend.fromSlope(trendSignal * 1.8)
            )

            ts = ts.plusSeconds(SAMPLE_INTERVAL_MINUTES * 60)
            index++
        }
        return points
    }

    private fun gaussian(minuteOfDay: Double, center: Double, widthMinutes: Double, amplitude: Double): Double {
        val delta = minuteOfDay - center
        val sigma = widthMinutes / 2.8
        return amplitude * kotlin.math.exp(-(delta * delta) / (2.0 * sigma * sigma))
    }

    private fun gaussianSlope(minuteOfDay: Double, center: Double, widthMinutes: Double): Double {
        val delta = minuteOfDay - center
        val sigma = widthMinutes / 2.8
        val g = kotlin.math.exp(-(delta * delta) / (2.0 * sigma * sigma))
        return -delta / (sigma * sigma) * g
    }
}

