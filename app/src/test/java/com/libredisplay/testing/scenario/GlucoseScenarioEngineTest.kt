package com.libredisplay.testing.scenario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.ZoneId

class GlucoseScenarioEngineTest {

    @Test
    fun scenarioEnum_containsAllRequiredNamedScenarios() {
        val names = GlucoseScenario.entries.map { it.name }.toSet()

        assertEquals(
            setOf(
                "NORMAL_STABLE",
                "NORMAL_RISING",
                "NORMAL_FALLING",
                "HIGH_STABLE",
                "HIGH_RISING",
                "HIGH_FAST_RISING",
                "VERY_HIGH_STABLE",
                "VERY_HIGH_RISING",
                "LOW_STABLE",
                "LOW_FALLING",
                "LOW_FAST_FALLING",
                "VERY_LOW",
                "NO_DATA",
                "STALE_DATA",
                "ONLY_1H_DATA",
                "ONLY_3H_DATA",
                "ONLY_6H_DATA",
                "ONLY_12H_DATA",
                "FULL_24H_DATA",
                "FULL_48H_DATA",
                "FULL_3D_DATA",
                "DATA_WITH_GAPS",
                "DATA_CROSSING_MIDNIGHT",
                "SENSOR_EXPIRING",
                "SENSOR_EXPIRED",
                "GMI_UNAVAILABLE",
                "LONG_METRIC_VALUES",
                "BACKUP_SAMPLE_DATA"
            ),
            names
        )
    }

    @Test
    fun datasets_areDeterministicForRepeatedCalls() {
        GlucoseScenario.entries.forEach { scenario ->
            val first = GlucoseScenarioEngine.dataset(scenario)
            val second = GlucoseScenarioEngine.dataset(scenario)

            assertEquals("scenario=$scenario pointCount", first.points.size, second.points.size)
            assertEquals("scenario=$scenario firstPoint", first.points.firstOrNull(), second.points.firstOrNull())
            assertEquals("scenario=$scenario lastPoint", first.points.lastOrNull(), second.points.lastOrNull())
            assertEquals("scenario=$scenario reading", first.asReading(), second.asReading())
        }
    }

    @Test
    fun generator_supportsAllReusablePatterns() {
        val startTime = GlucoseScenarioEngine.DEFAULT_NOW.minus(Duration.ofHours(12))
        val zoneId = ZoneId.of("Europe/Warsaw")

        val flat = DeterministicCgmGenerator.generate(startTime, Duration.ofHours(6), Duration.ofMinutes(5), 110, CgmPattern.FLAT, zoneId)
        val gradualRise = DeterministicCgmGenerator.generate(startTime, Duration.ofHours(6), Duration.ofMinutes(5), 95, CgmPattern.GRADUAL_RISE, zoneId)
        val gradualFall = DeterministicCgmGenerator.generate(startTime, Duration.ofHours(6), Duration.ofMinutes(5), 150, CgmPattern.GRADUAL_FALL, zoneId)
        val fastRise = DeterministicCgmGenerator.generate(startTime, Duration.ofHours(6), Duration.ofMinutes(5), 180, CgmPattern.FAST_RISE, zoneId)
        val fastFall = DeterministicCgmGenerator.generate(startTime, Duration.ofHours(6), Duration.ofMinutes(5), 140, CgmPattern.FAST_FALL, zoneId)
        val hypoEpisode = DeterministicCgmGenerator.generate(startTime, Duration.ofHours(6), Duration.ofMinutes(5), 110, CgmPattern.HYPO_EPISODE, zoneId)
        val hyperEpisode = DeterministicCgmGenerator.generate(startTime, Duration.ofHours(6), Duration.ofMinutes(5), 130, CgmPattern.HYPER_EPISODE, zoneId)
        val irregular = DeterministicCgmGenerator.generate(startTime, Duration.ofHours(6), Duration.ofMinutes(5), 115, CgmPattern.IRREGULAR_INTERVALS, zoneId)
        val gaps = DeterministicCgmGenerator.generate(startTime, Duration.ofHours(12), Duration.ofMinutes(5), 117, CgmPattern.GAPS, zoneId)
        val crossingMidnight = DeterministicCgmGenerator.generate(
            startTime = GlucoseScenarioEngine.DEFAULT_NOW.atZone(zoneId).toLocalDate().minusDays(1).atTime(20, 0).atZone(zoneId).toInstant(),
            duration = Duration.ofHours(8),
            sampleInterval = Duration.ofMinutes(5),
            startGlucose = 122,
            pattern = CgmPattern.CROSSING_MIDNIGHT,
            zoneId = zoneId
        )

        assertTrue(flat.zipWithNext().all { kotlin.math.abs(it.second.value - it.first.value) <= 4 })
        assertTrue(gradualRise.last().value > gradualRise.first().value)
        assertTrue(gradualFall.last().value < gradualFall.first().value)
        assertTrue(fastRise.last().value - fastRise.first().value >= 100)
        assertTrue(fastFall.first().value - fastFall.last().value >= 100)
        assertTrue(hypoEpisode.any { it.value < 54 })
        assertTrue(hyperEpisode.any { it.value > 250 })
        assertTrue(irregular.zipWithNext().map { Duration.between(it.first.timestamp, it.second.timestamp).toMinutes() }.distinct().size > 1)
        assertTrue(gaps.size < (Duration.ofHours(12).toMinutes() / 5).toInt())
        assertTrue(crossingMidnight.map { it.timestamp.atZone(zoneId).toLocalDate() }.distinct().size >= 2)
    }

    @Test
    fun namedScenarios_haveExpectedCoverageCharacteristics() {
        val full48h = GlucoseScenarioEngine.dataset(GlucoseScenario.FULL_48H_DATA)
        val stale = GlucoseScenarioEngine.dataset(GlucoseScenario.STALE_DATA)
        val noData = GlucoseScenarioEngine.dataset(GlucoseScenario.NO_DATA)
        val oneHour = GlucoseScenarioEngine.dataset(GlucoseScenario.ONLY_1H_DATA)
        val gmiUnavailable = GlucoseScenarioEngine.dataset(GlucoseScenario.GMI_UNAVAILABLE)

        assertTrue(full48h.availableSpan >= Duration.ofHours(47).plusMinutes(50))
        assertTrue(stale.newestTimestamp!!.isBefore(GlucoseScenarioEngine.DEFAULT_NOW.minus(Duration.ofHours(24))))
        assertTrue(noData.points.isEmpty())
        assertTrue(oneHour.availableSpan in Duration.ofMinutes(55)..Duration.ofMinutes(60))
        assertTrue(gmiUnavailable.availableSpan >= Duration.ofDays(12))
        assertTrue(gmiUnavailable.availableSpan < Duration.ofDays(14))
    }
}

