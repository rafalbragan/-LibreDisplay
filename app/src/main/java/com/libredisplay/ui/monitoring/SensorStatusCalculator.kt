package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseReading
import java.time.Duration
import java.time.Instant

/**
 * Models for sensor status display.
 *
 * The app needs to know:
 * - When the sensor was inserted
 * - How long it lasts (typically 14 days for FreeStyle)
 * - Current remaining time
 */
internal data class SensorStatus(
    val isKnown: Boolean,
    val insertedAt: Instant? = null,
    val lifespan: Duration = Duration.ofDays(14),
    val remainingDuration: Duration? = null,
    val statusMessage: String = "",
    val isWarning: Boolean = false,
    val isError: Boolean = false
)

/**
 * Calculate sensor remaining time based on available data.
 *
 * This is a placeholder implementation that infers sensor status from glucose readings.
 * In production, the app should track actual sensor insertion time from LibreLinkUp API.
 */
internal object SensorStatusCalculator {

    /**
     * Estimate sensor status from glucose reading data.
     *
     * Note: This is a simplified approach. LibreView API may provide sensor metadata
     * that should be used instead. For now, we assume a 14-day FreeStyle sensor lifecycle.
     */
    fun calculateSensorStatus(reading: GlucoseReading?, now: Instant = Instant.now()): SensorStatus {
        if (reading == null) {
            return SensorStatus(
                isKnown = false,
                statusMessage = "Brak danych sensora"
            )
        }

        // Try to infer sensor insertion time from the oldest reading in history
        // A FreeStyle Libre sensor typically runs for 14 days
        val sensorLifespan = Duration.ofDays(14)

        // Get the oldest reading timestamp from history
        val oldestReading = reading.history.minByOrNull { it.timestamp }
            ?: return SensorStatus(isKnown = false, statusMessage = "Zbyt mało danych")

        val ageFromOldestReading = Duration.between(oldestReading.timestamp, now)

        // Sensor is known if we have any history data
        val sensorInsertionEstimate = oldestReading.timestamp.minus(ageFromOldestReading.multipliedBy(2).dividedBy(3))
        val remainingDuration = sensorLifespan.minus(Duration.between(sensorInsertionEstimate, now))

        return when {
            remainingDuration.isNegative || remainingDuration.isZero -> SensorStatus(
                isKnown = true,
                remainingDuration = Duration.ZERO,
                statusMessage = "Sensor wygasł",
                isError = true
            )
            remainingDuration < Duration.ofHours(12) -> SensorStatus(
                isKnown = true,
                insertedAt = sensorInsertionEstimate,
                remainingDuration = remainingDuration,
                statusMessage = "Sensor wkrótce wygasa: ${formatSensorDuration(remainingDuration)}",
                isWarning = true
            )
            else -> SensorStatus(
                isKnown = true,
                insertedAt = sensorInsertionEstimate,
                remainingDuration = remainingDuration,
                statusMessage = "Sensor: ${formatSensorDuration(remainingDuration)}"
            )
        }
    }

    /**
     * Format sensor remaining duration in a compact form.
     * E.g., "13 dni 8 godz. 24 min" or "5g 30m"
     */
    fun formatSensorDuration(duration: Duration?): String {
        val safeDuration = duration ?: return "brak danych"
        if (safeDuration.isNegative || safeDuration.isZero) return "0"

        val totalMinutes = safeDuration.toMinutes()
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60

        return when {
            days > 0 -> {
                val dayText = when (days) {
                    1L -> "1 dzień"
                    else -> "$days dni"
                }
                when {
                    hours > 0 && minutes > 0 -> "$dayText ${hours}g ${minutes}m"
                    hours > 0 -> "$dayText ${hours}g"
                    else -> dayText
                }
            }
            hours > 0 -> {
                when {
                    minutes > 0 -> "${hours}g ${minutes}m"
                    else -> "${hours}g"
                }
            }
            else -> "${minutes}m"
        }
    }
}

