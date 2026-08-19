package com.libredisplay.analytics

import com.libredisplay.data.model.GlucoseHistoryPoint
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class RangeDistribution(
    val belowCriticalPercent: Int,
    val belowRangePercent: Int,
    val inRangePercent: Int,
    val aboveRangePercent: Int,
    val aboveVeryHighPercent: Int,
    val belowRangeDuration: Duration,
    val inRangeDuration: Duration,
    val aboveRangeDuration: Duration
)

data class SensorActivity(
    val activityPercent: Double,
    val coveredDuration: Duration,
    val missingDuration: Duration,
    val daysWithData: Int,
    val dataQualityStatus: String
)

data class DailyMetric(
    val date: LocalDate,
    val tirPercent: Int,
    val belowRangePercent: Int,
    val aboveRangePercent: Int,
    val averageGlucose: Double,
    val gmi: Double,
    val sensorActivityPercent: Double,
    val readingsCount: Int,
    val dataQuality: String
)

object GlucoseMetricsCalculator {

    fun calculateRangeDistribution(
        readings: List<GlucoseHistoryPoint>,
        targetLow: Int,
        targetHigh: Int,
        lowCritical: Int,
        highCritical: Int,
        maxGap: Duration = Duration.ofMinutes(20)
    ): RangeDistribution {
        if (readings.size < 2) {
            return RangeDistribution(0, 0, 0, 0, 0, Duration.ZERO, Duration.ZERO, Duration.ZERO)
        }
        val sorted = readings.sortedBy { it.timestamp }
        var below = Duration.ZERO
        var inRange = Duration.ZERO
        var above = Duration.ZERO
        var belowCritical = Duration.ZERO
        var aboveVeryHigh = Duration.ZERO
        var covered = Duration.ZERO

        for (i in 0 until sorted.lastIndex) {
            val current = sorted[i]
            val next = sorted[i + 1]
            val delta = Duration.between(current.timestamp, next.timestamp)
            if (delta.isNegative || delta > maxGap) continue
            covered += delta

            when {
                current.value < lowCritical -> {
                    below += delta
                    belowCritical += delta
                }
                current.value < targetLow -> below += delta
                current.value > highCritical -> {
                    above += delta
                    aboveVeryHigh += delta
                }
                current.value > targetHigh -> above += delta
                else -> inRange += delta
            }
        }

        fun pct(duration: Duration): Int {
            if (covered.isZero) return 0
            return ((duration.toMillis().toDouble() / covered.toMillis().toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 100)
        }

        return RangeDistribution(
            belowCriticalPercent = pct(belowCritical),
            belowRangePercent = pct(below),
            inRangePercent = pct(inRange),
            aboveRangePercent = pct(above),
            aboveVeryHighPercent = pct(aboveVeryHigh),
            belowRangeDuration = below,
            inRangeDuration = inRange,
            aboveRangeDuration = above
        )
    }

    fun calculateSensorActivity(
        readings: List<GlucoseHistoryPoint>,
        periodStart: Instant,
        periodEnd: Instant,
        expectedIntervalMinutes: Long = 15,
        maxGap: Duration = Duration.ofMinutes(20)
    ): SensorActivity {
        if (periodEnd <= periodStart) {
            return SensorActivity(0.0, Duration.ZERO, Duration.ZERO, 0, "NO_DATA")
        }
        val total = Duration.between(periodStart, periodEnd)
        if (readings.size < 2) {
            return SensorActivity(0.0, Duration.ZERO, total, 0, "LOW")
        }

        val sorted = readings.sortedBy { it.timestamp }
        var covered = Duration.ZERO
        val days = sorted.map { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() }.distinct().size

        for (i in 0 until sorted.lastIndex) {
            val current = sorted[i]
            val next = sorted[i + 1]
            val delta = Duration.between(current.timestamp, next.timestamp)
            if (delta.isNegative || delta > maxGap) continue
            covered += delta
        }

        val pct = (covered.toMillis().toDouble() / total.toMillis().toDouble() * 100.0).coerceIn(0.0, 100.0)
        val status = when {
            pct >= 75.0 -> "HIGH"
            pct >= 65.0 -> "MEDIUM"
            else -> "LOW"
        }
        return SensorActivity(
            activityPercent = pct,
            coveredDuration = covered,
            missingDuration = total.minus(covered),
            daysWithData = days,
            dataQualityStatus = status
        )
    }

    fun calculateGmi(averageGlucoseMgDl: Double): Double = 3.31 + 0.02392 * averageGlucoseMgDl

    fun calculateAverageGlucose(readings: List<GlucoseHistoryPoint>): Double {
        if (readings.isEmpty()) return Double.NaN
        return readings.map { it.value }.average()
    }

    fun calculateMinMax(readings: List<GlucoseHistoryPoint>): Pair<Int?, Int?> {
        if (readings.isEmpty()) return null to null
        return readings.minOf { it.value } to readings.maxOf { it.value }
    }

    fun calculateDailyMetrics(
        readings: List<GlucoseHistoryPoint>,
        targetLow: Int,
        targetHigh: Int,
        lowCritical: Int,
        highCritical: Int
    ): List<DailyMetric> {
        val grouped = readings.groupBy { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() }
        return grouped.entries.sortedBy { it.key }.map { (date, points) ->
            val range = calculateRangeDistribution(points, targetLow, targetHigh, lowCritical, highCritical)
            val avg = calculateAverageGlucose(points)
            val gmi = if (avg.isFinite()) calculateGmi(avg) else Double.NaN
            val activity = calculateSensorActivity(
                readings = points,
                periodStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                periodEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
            DailyMetric(
                date = date,
                tirPercent = range.inRangePercent,
                belowRangePercent = range.belowRangePercent,
                aboveRangePercent = range.aboveRangePercent,
                averageGlucose = avg,
                gmi = gmi,
                sensorActivityPercent = activity.activityPercent,
                readingsCount = points.size,
                dataQuality = activity.dataQualityStatus
            )
        }
    }
}

