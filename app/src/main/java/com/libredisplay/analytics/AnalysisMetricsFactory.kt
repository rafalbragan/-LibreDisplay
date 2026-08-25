package com.libredisplay.analytics

import com.libredisplay.data.model.GlucoseHistoryPoint
import java.time.Instant

data class PeriodMetrics(
    val readingsCount: Int,
    val tirPercent: Int?,
    val belowPercent: Int?,
    val abovePercent: Int?,
    val averageGlucose: Double?,
    val cvPercent: Double?,
    val gmiPercent: Double?,
    val minGlucose: Int?,
    val maxGlucose: Int?,
    val veryLowEpisodes: Int,
    val veryHighEpisodes: Int,
    val sensorActivityPercent: Double?
) {
    companion object {
        val empty = PeriodMetrics(
            readingsCount = 0,
            tirPercent = null,
            belowPercent = null,
            abovePercent = null,
            averageGlucose = null,
            cvPercent = null,
            gmiPercent = null,
            minGlucose = null,
            maxGlucose = null,
            veryLowEpisodes = 0,
            veryHighEpisodes = 0,
            sensorActivityPercent = null
        )
    }
}

object AnalysisMetricsFactory {

    fun calculate(
        readings: List<GlucoseHistoryPoint>,
        periodStart: Instant,
        periodEnd: Instant,
        targetLow: Int,
        targetHigh: Int,
        lowCritical: Int,
        highCritical: Int
    ): PeriodMetrics {
        if (periodEnd <= periodStart) return PeriodMetrics.empty
        if (readings.isEmpty()) return PeriodMetrics.empty

        val distribution = GlucoseMetricsCalculator.calculateRangeDistribution(
            readings = readings,
            targetLow = targetLow,
            targetHigh = targetHigh,
            lowCritical = lowCritical,
            highCritical = highCritical
        )
        val activity = GlucoseMetricsCalculator.calculateSensorActivity(
            readings = readings,
            periodStart = periodStart,
            periodEnd = periodEnd,
            expectedIntervalMinutes = 15
        )
        val average = GlucoseMetricsCalculator.calculateAverageGlucose(readings).takeIf { it.isFinite() }
        val cv = GlucoseMetricsCalculator.calculateCoefficientOfVariation(readings)
        val gmi = average?.let { GlucoseMetricsCalculator.calculateGmi(it) }
        val minMax = GlucoseMetricsCalculator.calculateMinMax(readings)

        return PeriodMetrics(
            readingsCount = readings.size,
            tirPercent = distribution.inRangePercent,
            belowPercent = distribution.belowRangePercent,
            abovePercent = distribution.aboveRangePercent,
            averageGlucose = average,
            cvPercent = cv,
            gmiPercent = gmi,
            minGlucose = minMax.first,
            maxGlucose = minMax.second,
            veryLowEpisodes = countEpisodes(readings) { it < lowCritical },
            veryHighEpisodes = countEpisodes(readings) { it > highCritical },
            sensorActivityPercent = activity.activityPercent
        )
    }

    private fun countEpisodes(
        points: List<GlucoseHistoryPoint>,
        predicate: (Int) -> Boolean
    ): Int {
        if (points.isEmpty()) return 0
        val sorted = points.sortedBy { it.timestamp }
        var episodes = 0
        var inEpisode = false
        sorted.forEach { point ->
            val matches = predicate(point.value)
            if (matches && !inEpisode) {
                episodes += 1
                inEpisode = true
            } else if (!matches) {
                inEpisode = false
            }
        }
        return episodes
    }
}

