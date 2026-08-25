package com.libredisplay.data.test

import com.libredisplay.data.model.*
import kotlin.random.Random
import java.time.Instant
import java.time.ZonedDateTime
import java.time.ZoneId

/**
 * Enumeration of test/QA scenarios for deterministic glucose data generation
 */
enum class GlucoseScenario {
    // Stable scenarios
    NORMAL_STABLE,
    HIGH_STABLE,
    VERY_HIGH_STABLE,
    LOW_STABLE,

    // Trending scenarios
    NORMAL_RISING,
    NORMAL_FALLING,
    HIGH_RISING,
    HIGH_FAST_RISING,
    VERY_HIGH_RISING,
    LOW_FALLING,
    LOW_FAST_FALLING,
    VERY_LOW,

    // Data quality scenarios
    NO_DATA,
    STALE_DATA,
    ONLY_1H_DATA,
    ONLY_3H_DATA,
    ONLY_6H_DATA,
    ONLY_12H_DATA,
    FULL_24H_DATA,
    FULL_3D_DATA,
    DATA_WITH_GAPS,
    DATA_CROSSING_MIDNIGHT,

    // Sensor scenarios
    SENSOR_EXPIRING,
    SENSOR_EXPIRED,
    GMI_UNAVAILABLE,
    LONG_METRIC_VALUES,

    // Backup scenarios
    BACKUP_SAMPLE_DATA
}

/**
 * Generator for deterministic glucose readings used in testing
 *
 * Never uses random() without a fixed seed to ensure reproducible tests.
 * This generator is ONLY for tests/QA, never active in production release.
 */
class GlucoseTestDataGenerator(private val scenario: GlucoseScenario = GlucoseScenario.NORMAL_STABLE) {

    // Test ranges
    companion object {
        const val MIN_GLUCOSE = 20    // mg/dL
        const val MAX_GLUCOSE = 600   // mg/dL
        const val NORMAL_LOW = 70
        const val NORMAL_HIGH = 180
        const val HIGH_THRESHOLD = 240
        const val VERY_HIGH_THRESHOLD = 300
        const val LOW_THRESHOLD = 70
        const val VERY_LOW_THRESHOLD = 55
    }

    /**
     * Generate readings for the given scenario
     *
     * @param duration Number of milliseconds of data to generate
     * @param interval Milliseconds between readings (default 5 minutes)
     * @param startGlucose Initial glucose value (mg/dL)
     * @param endTime End time for data generation (default: now)
     * @return List of glucose readings, sorted chronologically
     */
    fun generateReadings(
        duration: Long = 24 * 60 * 60 * 1000,  // 24 hours
        interval: Long = 5 * 60 * 1000,        // 5 minutes
        startGlucose: Int = 100,
        endTime: Long = System.currentTimeMillis(),
        seed: Long = 42L  // Fixed seed for reproducibility
    ): List<GlucoseReading> {

        val random = Random(seed)
        val readings = mutableListOf<GlucoseReading>()
        val startTime = endTime - duration
        var currentTime = startTime
        var currentGlucose = startGlucose

        when (scenario) {
            GlucoseScenario.NORMAL_STABLE -> {
                while (currentTime <= endTime) {
                    currentGlucose = (120 + random.nextInt(-10, 11)).coerceIn(MIN_GLUCOSE, MAX_GLUCOSE)
                    readings.add(createReading(currentGlucose, currentTime, GlucoseTrend.FLAT))
                    currentTime += interval
                }
            }

            GlucoseScenario.HIGH_STABLE -> {
                while (currentTime <= endTime) {
                    currentGlucose = (250 + random.nextInt(-15, 16)).coerceIn(MIN_GLUCOSE, MAX_GLUCOSE)
                    readings.add(createReading(currentGlucose, currentTime, GlucoseTrend.FLAT))
                    currentTime += interval
                }
            }

            GlucoseScenario.VERY_HIGH_STABLE -> {
                while (currentTime <= endTime) {
                    currentGlucose = (350 + random.nextInt(-20, 21)).coerceIn(MIN_GLUCOSE, MAX_GLUCOSE)
                    readings.add(createReading(currentGlucose, currentTime, GlucoseTrend.FLAT))
                    currentTime += interval
                }
            }

            GlucoseScenario.LOW_STABLE -> {
                while (currentTime <= endTime) {
                    currentGlucose = (85 + random.nextInt(-8, 9)).coerceIn(MIN_GLUCOSE, MAX_GLUCOSE)
                    readings.add(createReading(currentGlucose, currentTime, GlucoseTrend.FLAT))
                    currentTime += interval
                }
            }

            GlucoseScenario.NORMAL_RISING -> {
                currentGlucose = 100
                while (currentTime <= endTime) {
                    currentGlucose = (currentGlucose + random.nextInt(1, 5)).coerceIn(MIN_GLUCOSE, MAX_GLUCOSE)
                    readings.add(createReading(currentGlucose, currentTime, GlucoseTrend.UP))
                    currentTime += interval
                }
            }

            GlucoseScenario.NORMAL_FALLING -> {
                currentGlucose = 150
                while (currentTime <= endTime) {
                    currentGlucose = (currentGlucose - random.nextInt(1, 5)).coerceIn(MIN_GLUCOSE, MAX_GLUCOSE)
                    readings.add(createReading(currentGlucose, currentTime, GlucoseTrend.DOWN))
                    currentTime += interval
                }
            }

            GlucoseScenario.HIGH_RISING -> {
                currentGlucose = 200
                while (currentTime <= endTime) {
                    currentGlucose = (currentGlucose + random.nextInt(2, 6)).coerceIn(MIN_GLUCOSE, MAX_GLUCOSE)
                    readings.add(createReading(currentGlucose, currentTime, GlucoseTrend.UP))
                    currentTime += interval
                }
            }

            GlucoseScenario.HIGH_FAST_RISING -> {
                currentGlucose = 180
                while (currentTime <= endTime) {
                    currentGlucose = (currentGlucose + random.nextInt(5, 15)).coerceIn(MIN_GLUCOSE, MAX_GLUCOSE)
                    readings.add(createReading(currentGlucose, currentTime, GlucoseTrend.UP_UP))
                    currentTime += interval
                }
            }

            GlucoseScenario.VERY_HIGH_RISING -> {
                currentGlucose = 300
                while (currentTime <= endTime) {
                    currentGlucose = (currentGlucose + random.nextInt(3, 8)).coerceIn(MIN_GLUCOSE, MAX_GLUCOSE)
                    readings.add(createReading(currentGlucose, currentTime, GlucoseTrend.UP))
                    currentTime += interval
                }
            }

            GlucoseScenario.LOW_FALLING -> {
                currentGlucose = 100
                while (currentTime <= endTime) {
                    currentGlucose = (currentGlucose - random.nextInt(2, 6)).coerceIn(MIN_GLUCOSE, MAX_GLUCOSE)
                    readings.add(createReading(currentGlucose, currentTime, GlucoseTrend.DOWN))
                    currentTime += interval
                }
            }

            GlucoseScenario.LOW_FAST_FALLING -> {
                currentGlucose = 120
                while (currentTime <= endTime) {
                    currentGlucose = (currentGlucose - random.nextInt(5, 15)).coerceIn(MIN_GLUCOSE, MAX_GLUCOSE)
                    readings.add(createReading(currentGlucose, currentTime, GlucoseTrend.DOWN_DOWN))
                    currentTime += interval
                }
            }

            GlucoseScenario.VERY_LOW -> {
                while (currentTime <= endTime) {
                    currentGlucose = (45 + random.nextInt(-10, 11)).coerceIn(MIN_GLUCOSE, MAX_GLUCOSE)
                    readings.add(createReading(currentGlucose, currentTime, GlucoseTrend.DOWN))
                    currentTime += interval
                }
            }

            GlucoseScenario.NO_DATA -> {
                // Empty list
            }

            GlucoseScenario.STALE_DATA -> {
                // Single reading from 24+ hours ago
                val staleTime = endTime - (25 * 60 * 60 * 1000)
                readings.add(createReading(150, staleTime, GlucoseTrend.FLAT))
            }

            GlucoseScenario.ONLY_1H_DATA -> {
                val onlyOneHour = 60 * 60 * 1000
                var time = endTime - onlyOneHour
                while (time <= endTime) {
                    readings.add(createReading(110 + random.nextInt(-5, 6), time, GlucoseTrend.FLAT))
                    time += interval
                }
            }

            GlucoseScenario.ONLY_3H_DATA -> {
                val onlyThreeHours = 3 * 60 * 60 * 1000
                var time = endTime - onlyThreeHours
                while (time <= endTime) {
                    readings.add(createReading(110 + random.nextInt(-5, 6), time, GlucoseTrend.FLAT))
                    time += interval
                }
            }

            GlucoseScenario.ONLY_6H_DATA -> {
                val onlySixHours = 6 * 60 * 60 * 1000
                var time = endTime - onlySixHours
                while (time <= endTime) {
                    readings.add(createReading(110 + random.nextInt(-5, 6), time, GlucoseTrend.FLAT))
                    time += interval
                }
            }

            GlucoseScenario.ONLY_12H_DATA -> {
                val onlyTwelveHours = 12 * 60 * 60 * 1000
                var time = endTime - onlyTwelveHours
                while (time <= endTime) {
                    readings.add(createReading(110 + random.nextInt(-5, 6), time, GlucoseTrend.FLAT))
                    time += interval
                }
            }

            GlucoseScenario.FULL_24H_DATA -> {
                val twentyFourHours = 24 * 60 * 60 * 1000
                var time = endTime - twentyFourHours
                while (time <= endTime) {
                    readings.add(createReading(110 + random.nextInt(-5, 6), time, GlucoseTrend.FLAT))
                    time += interval
                }
            }

            GlucoseScenario.FULL_3D_DATA -> {
                val threeDays = 3 * 24 * 60 * 60 * 1000
                var time = endTime - threeDays
                while (time <= endTime) {
                    readings.add(createReading(110 + random.nextInt(-5, 6), time, GlucoseTrend.FLAT))
                    time += interval
                }
            }

            GlucoseScenario.DATA_WITH_GAPS -> {
                var time = startTime
                var gapSeed = 0
                while (time <= endTime) {
                    // Add 2 hours of data, then 30-minute gap
                    val gapInterval = 2 * 60 * 60 * 1000
                    val gapDuration = 30 * 60 * 1000

                    if (gapSeed % 5 < 4) {
                        readings.add(createReading(110 + random.nextInt(-5, 6), time, GlucoseTrend.FLAT))
                    }

                    time += interval
                    gapSeed++
                }
            }

            GlucoseScenario.DATA_CROSSING_MIDNIGHT -> {
                val midnightZone = ZoneId.systemDefault()
                var time = startTime
                while (time <= endTime) {
                    val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), midnightZone)
                    // Create a small discontinuity around midnight for testing
                    val glucose = if (zdt.hour == 0 || zdt.hour == 23) {
                        140 + random.nextInt(-10, 11)
                    } else {
                        110 + random.nextInt(-5, 6)
                    }
                    readings.add(createReading(glucose.coerceIn(MIN_GLUCOSE, MAX_GLUCOSE), time, GlucoseTrend.FLAT))
                    time += interval
                }
            }

            GlucoseScenario.SENSOR_EXPIRING -> {
                // Data quality degrades, then sensor expires
                val expiryPoint = (duration * 0.8).toLong()
                var time = startTime
                while (time <= endTime) {
                    if (time < startTime + expiryPoint) {
                        readings.add(createReading(110 + random.nextInt(-5, 6), time, GlucoseTrend.FLAT))
                    }
                    // After expiry point, no readings
                    time += interval
                }
            }

            GlucoseScenario.SENSOR_EXPIRED -> {
                // No recent readings at all
                val twoHoursAgo = endTime - (2 * 60 * 60 * 1000)
                var time = startTime
                while (time <= twoHoursAgo) {
                    readings.add(createReading(110 + random.nextInt(-5, 6), time, GlucoseTrend.FLAT))
                    time += interval
                }
            }

            GlucoseScenario.GMI_UNAVAILABLE -> {
                // Full data but some metrics missing
                while (currentTime <= endTime) {
                    readings.add(createReading(120 + random.nextInt(-10, 11), currentTime, GlucoseTrend.FLAT))
                    currentTime += interval
                }
            }

            GlucoseScenario.LONG_METRIC_VALUES -> {
                // Just normal data; metrics themselves might be long when displayed
                while (currentTime <= endTime) {
                    readings.add(createReading(120 + random.nextInt(-10, 11), currentTime, GlucoseTrend.FLAT))
                    currentTime += interval
                }
            }

            GlucoseScenario.BACKUP_SAMPLE_DATA -> {
                // Representative sample for backup testing
                val twelveHours = 12 * 60 * 60 * 1000
                var time = endTime - twelveHours
                while (time <= endTime) {
                    currentGlucose = (120 + random.nextInt(-20, 21)).coerceIn(MIN_GLUCOSE, MAX_GLUCOSE)
                    readings.add(createReading(currentGlucose, time, GlucoseTrend.FLAT))
                    time += interval
                }
            }
        }

        return readings.sortedBy { it.timestamp }
    }

    private fun createReading(glucose: Int, timestamp: Long, trend: GlucoseTrend): GlucoseReading {
        return GlucoseReading(
            glucose = glucose,
            timestamp = timestamp,
            trend = trend,
            isAlarm = false,
            trendArrow = trend.symbol
        )
    }
}

/**
 * Enumeration of glucose trend directions
 */
enum class GlucoseTrend(val symbol: String) {
    DOWN_DOWN("↓↓"),
    DOWN("↓"),
    FLAT("→"),
    UP("↑"),
    UP_UP("↑↑");
}

/**
 * Data class representing a single glucose reading
 */
data class GlucoseReading(
    val glucose: Int,
    val timestamp: Long,
    val trend: GlucoseTrend,
    val isAlarm: Boolean,
    val trendArrow: String
)

