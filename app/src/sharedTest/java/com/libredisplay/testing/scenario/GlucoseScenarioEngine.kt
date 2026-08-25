package com.libredisplay.testing.scenario

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseHistoryStats
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

enum class GlucoseScenario {
    NORMAL_STABLE,
    NORMAL_RISING,
    NORMAL_FALLING,
    HIGH_STABLE,
    HIGH_RISING,
    HIGH_FAST_RISING,
    VERY_HIGH_STABLE,
    VERY_HIGH_RISING,
    LOW_STABLE,
    LOW_FALLING,
    LOW_FAST_FALLING,
    VERY_LOW,
    NO_DATA,
    STALE_DATA,
    ONLY_1H_DATA,
    ONLY_3H_DATA,
    ONLY_6H_DATA,
    ONLY_12H_DATA,
    FULL_24H_DATA,
    FULL_48H_DATA,
    FULL_3D_DATA,
    DATA_WITH_GAPS,
    DATA_CROSSING_MIDNIGHT,
    SENSOR_EXPIRING,
    SENSOR_EXPIRED,
    GMI_UNAVAILABLE,
    LONG_METRIC_VALUES,
    BACKUP_SAMPLE_DATA
}

enum class CgmPattern {
    FLAT,
    GRADUAL_RISE,
    GRADUAL_FALL,
    FAST_RISE,
    FAST_FALL,
    HYPO_EPISODE,
    HYPER_EPISODE,
    IRREGULAR_INTERVALS,
    GAPS,
    CROSSING_MIDNIGHT
}

data class ScenarioDataset(
    val scenario: GlucoseScenario,
    val now: Instant,
    val points: List<GlucoseHistoryPoint>,
    val zoneId: ZoneId = GlucoseScenarioEngine.DEFAULT_ZONE_ID
) {
    val oldestTimestamp: Instant? get() = points.firstOrNull()?.timestamp
    val newestTimestamp: Instant? get() = points.lastOrNull()?.timestamp
    val availableSpan: Duration = if (points.size < 2) Duration.ZERO else Duration.between(points.first().timestamp, points.last().timestamp)

    fun asReading(targetLow: Int = 80, targetHigh: Int = 180): GlucoseReading? {
        val current = points.lastOrNull() ?: return null
        val history = points.dropLast(1)
        return GlucoseReading.of(
            value = current.value,
            timestamp = current.timestamp,
            trend = current.trend,
            history = history,
            stats = GlucoseHistoryStats.from(points, targetLow, targetHigh),
            historyHoursAvailable = availableSpan.toMinutes().toDouble() / 60.0,
            sourceHistoryPointCount = points.size
        )
    }
}

object DeterministicCgmGenerator {
    private val defaultGapSlots = setOf(12, 13, 14, 44, 45, 73, 74, 75)

    fun generate(
        startTime: Instant,
        duration: Duration,
        sampleInterval: Duration,
        startGlucose: Int,
        pattern: CgmPattern,
        zoneId: ZoneId = GlucoseScenarioEngine.DEFAULT_ZONE_ID
    ): List<GlucoseHistoryPoint> {
        require(!duration.isNegative) { "duration must be >= 0" }
        require(sampleInterval > Duration.ZERO) { "sampleInterval must be > 0" }

        val totalSteps = (duration.toMillis() / sampleInterval.toMillis()).toInt().coerceAtLeast(0)
        if (totalSteps == 0) return emptyList()

        val intervals = buildIntervals(totalSteps, sampleInterval, pattern)
        val rawPoints = mutableListOf<Pair<Instant, Int>>()
        var time = startTime

        for (index in 0 until totalSteps) {
            val value = glucoseValueFor(pattern = pattern, startGlucose = startGlucose, index = index, totalSteps = totalSteps, time = time, zoneId = zoneId)
            val shouldKeep = when (pattern) {
                CgmPattern.GAPS -> index !in defaultGapSlots
                else -> true
            }
            if (shouldKeep) {
                rawPoints += time to value
            }
            time = time.plus(intervals[index])
        }

        return rawPoints.mapIndexed { index, (timestamp, value) ->
            val previous = rawPoints.getOrNull(index - 1)
            val slope = if (previous == null) 0.0 else {
                val minutes = Duration.between(previous.first, timestamp).toMinutes().coerceAtLeast(1)
                (value - previous.second).toDouble() / minutes.toDouble()
            }
            GlucoseHistoryPoint(
                value = value.coerceAtLeast(1),
                timestamp = timestamp,
                trend = GlucoseTrend.fromSlope(slope)
            )
        }
    }

    private fun buildIntervals(totalSteps: Int, sampleInterval: Duration, pattern: CgmPattern): List<Duration> {
        if (pattern != CgmPattern.IRREGULAR_INTERVALS) {
            return List(totalSteps) { sampleInterval }
        }
        val offsets = listOf(-2L, 0L, 3L, -1L, 2L, 0L, 1L, -2L)
        return List(totalSteps) { index ->
            sampleInterval.plusMinutes(offsets[index % offsets.size]).coerceAtLeast(Duration.ofMinutes(3))
        }
    }

    private fun glucoseValueFor(
        pattern: CgmPattern,
        startGlucose: Int,
        index: Int,
        totalSteps: Int,
        time: Instant,
        zoneId: ZoneId
    ): Int {
        val safeTotal = totalSteps.coerceAtLeast(1)
        val progress = index.toDouble() / safeTotal.toDouble()
        val base = when (pattern) {
            CgmPattern.FLAT -> startGlucose + listOf(0, 1, -1, 2, -2, 1, 0, -1)[index % 8]
            CgmPattern.GRADUAL_RISE -> startGlucose + (progress * 48.0).roundToInt()
            CgmPattern.GRADUAL_FALL -> startGlucose - (progress * 48.0).roundToInt()
            CgmPattern.FAST_RISE -> startGlucose + (progress * 118.0).roundToInt()
            CgmPattern.FAST_FALL -> startGlucose - (progress * 118.0).roundToInt()
            CgmPattern.HYPO_EPISODE -> when {
                progress < 0.25 -> 110 - (progress * 20.0).roundToInt()
                progress < 0.45 -> 68 - ((progress - 0.25) * 110.0).roundToInt()
                progress < 0.65 -> 48 + ((progress - 0.45) * 120.0).roundToInt()
                else -> 92 + ((progress - 0.65) * 20.0).roundToInt()
            }
            CgmPattern.HYPER_EPISODE -> when {
                progress < 0.25 -> 135 + (progress * 28.0).roundToInt()
                progress < 0.55 -> 190 + ((progress - 0.25) * 230.0).roundToInt()
                progress < 0.8 -> 255 - ((progress - 0.55) * 120.0).roundToInt()
                else -> 215 - ((progress - 0.8) * 70.0).roundToInt()
            }
            CgmPattern.IRREGULAR_INTERVALS -> startGlucose + listOf(0, 5, -3, 8, -4, 6, -2, 3)[index % 8]
            CgmPattern.GAPS -> startGlucose + listOf(1, 0, -1, 2, -2, 1)[index % 6]
            CgmPattern.CROSSING_MIDNIGHT -> {
                val localHour = time.atZone(zoneId).hour
                when (localHour) {
                    22, 23 -> startGlucose + 20
                    0, 1 -> startGlucose + 32
                    else -> startGlucose + listOf(0, 2, -1, 1, -2, 0)[index % 6]
                }
            }
        }
        return base.coerceIn(1, 600)
    }
}

object GlucoseScenarioEngine {
    val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Europe/Warsaw")
    val DEFAULT_NOW: Instant = ZonedDateTime.of(2026, 8, 24, 12, 0, 0, 0, DEFAULT_ZONE_ID).toInstant()

    fun dataset(
        scenario: GlucoseScenario,
        now: Instant = DEFAULT_NOW,
        zoneId: ZoneId = DEFAULT_ZONE_ID
    ): ScenarioDataset {
        fun generate(hours: Long, startGlucose: Int, pattern: CgmPattern, intervalMinutes: Long = 5): List<GlucoseHistoryPoint> {
            val duration = Duration.ofHours(hours)
            return DeterministicCgmGenerator.generate(
                startTime = now.minus(duration),
                duration = duration,
                sampleInterval = Duration.ofMinutes(intervalMinutes),
                startGlucose = startGlucose,
                pattern = pattern,
                zoneId = zoneId
            )
        }

        val points = when (scenario) {
            GlucoseScenario.NORMAL_STABLE -> generate(24, 112, CgmPattern.FLAT)
            GlucoseScenario.NORMAL_RISING -> generate(24, 96, CgmPattern.GRADUAL_RISE)
            GlucoseScenario.NORMAL_FALLING -> generate(24, 144, CgmPattern.GRADUAL_FALL)
            GlucoseScenario.HIGH_STABLE -> generate(24, 212, CgmPattern.FLAT)
            GlucoseScenario.HIGH_RISING -> generate(24, 188, CgmPattern.GRADUAL_RISE)
            GlucoseScenario.HIGH_FAST_RISING -> generate(24, 182, CgmPattern.FAST_RISE)
            GlucoseScenario.VERY_HIGH_STABLE -> generate(24, 276, CgmPattern.FLAT)
            GlucoseScenario.VERY_HIGH_RISING -> generate(24, 252, CgmPattern.GRADUAL_RISE)
            GlucoseScenario.LOW_STABLE -> generate(24, 68, CgmPattern.FLAT)
            GlucoseScenario.LOW_FALLING -> generate(24, 88, CgmPattern.GRADUAL_FALL)
            GlucoseScenario.LOW_FAST_FALLING -> generate(24, 96, CgmPattern.FAST_FALL)
            GlucoseScenario.VERY_LOW -> generate(24, 52, CgmPattern.HYPO_EPISODE)
            GlucoseScenario.NO_DATA -> emptyList()
            GlucoseScenario.STALE_DATA -> DeterministicCgmGenerator.generate(
                startTime = now.minus(Duration.ofHours(50)),
                duration = Duration.ofHours(24),
                sampleInterval = Duration.ofMinutes(5),
                startGlucose = 146,
                pattern = CgmPattern.FLAT,
                zoneId = zoneId
            )
            GlucoseScenario.ONLY_1H_DATA -> generate(1, 108, CgmPattern.FLAT)
            GlucoseScenario.ONLY_3H_DATA -> generate(3, 110, CgmPattern.FLAT)
            GlucoseScenario.ONLY_6H_DATA -> generate(6, 112, CgmPattern.FLAT)
            GlucoseScenario.ONLY_12H_DATA -> generate(12, 114, CgmPattern.FLAT)
            GlucoseScenario.FULL_24H_DATA -> generate(24, 116, CgmPattern.FLAT)
            GlucoseScenario.FULL_48H_DATA -> generate(48, 118, CgmPattern.FLAT)
            GlucoseScenario.FULL_3D_DATA -> generate(72, 120, CgmPattern.FLAT)
            GlucoseScenario.DATA_WITH_GAPS -> generate(24, 117, CgmPattern.GAPS)
            GlucoseScenario.DATA_CROSSING_MIDNIGHT -> DeterministicCgmGenerator.generate(
                startTime = ZonedDateTime.of(2026, 8, 23, 18, 30, 0, 0, zoneId).toInstant(),
                duration = Duration.ofHours(12),
                sampleInterval = Duration.ofMinutes(5),
                startGlucose = 124,
                pattern = CgmPattern.CROSSING_MIDNIGHT,
                zoneId = zoneId
            )
            GlucoseScenario.SENSOR_EXPIRING -> generate(24, 116, CgmPattern.IRREGULAR_INTERVALS).dropLast(6)
            GlucoseScenario.SENSOR_EXPIRED -> DeterministicCgmGenerator.generate(
                startTime = now.minus(Duration.ofHours(60)),
                duration = Duration.ofHours(36),
                sampleInterval = Duration.ofMinutes(5),
                startGlucose = 118,
                pattern = CgmPattern.FLAT,
                zoneId = zoneId
            )
            GlucoseScenario.GMI_UNAVAILABLE -> generate(13 * 24L, 118, CgmPattern.FLAT)
            GlucoseScenario.LONG_METRIC_VALUES -> generate(48, 96, CgmPattern.HYPO_EPISODE)
            GlucoseScenario.BACKUP_SAMPLE_DATA -> generate(36, 122, CgmPattern.HYPER_EPISODE)
        }
        return ScenarioDataset(scenario = scenario, now = now, points = points.sortedBy { it.timestamp }, zoneId = zoneId)
    }

    fun reading(scenario: GlucoseScenario, now: Instant = DEFAULT_NOW, zoneId: ZoneId = DEFAULT_ZONE_ID): GlucoseReading? {
        return dataset(scenario = scenario, now = now, zoneId = zoneId).asReading()
    }
}


