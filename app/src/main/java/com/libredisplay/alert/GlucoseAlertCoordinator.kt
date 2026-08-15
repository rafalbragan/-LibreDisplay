package com.libredisplay.alert

import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.GlucoseReading
import java.time.Duration
import java.time.Instant

data class GlucoseAlert(
    val level: GlucoseAlertLevel,
    val value: Int,
    val createdAt: Instant = Instant.now()
)

class GlucoseAlertCoordinator(
    private val alarmPlayer: NoOpAlarmPlayer,
    private val tts: NoOpTextToSpeechEngine,
    private val vibration: NoOpVibrationController,
    private val notifications: AndroidNotificationDispatcher
) {
    private var acknowledgedLevel: GlucoseAlertLevel = GlucoseAlertLevel.NONE

    fun onReading(settings: AppSettings, reading: GlucoseReading): GlucoseAlert? {
        val ageMinutes = Duration.between(reading.timestamp, Instant.now()).toMinutes().coerceAtLeast(0)
        if (ageMinutes > 5) {
            return null
        }

        val level = levelFor(reading.value)
        if (level == GlucoseAlertLevel.NONE) {
            acknowledgedLevel = GlucoseAlertLevel.NONE
            return null
        }

        if (level == acknowledgedLevel) {
            return null
        }

        val alert = GlucoseAlert(level = level, value = reading.value)
        alarmPlayer.play(level)
        vibration.vibrate(level)
        val message = GlucoseAlertMessaging.messageFor(level) ?: "Alert glukozy"
        tts.speak(message, 1.0f)
        notifications.dispatch(alert)
        return alert
    }

    fun acknowledge() {
        acknowledgedLevel = GlucoseAlertLevel.NONE
    }

    private fun levelFor(value: Int): GlucoseAlertLevel {
        return when {
            value <= 54 -> GlucoseAlertLevel.LOW_CRITICAL
            value <= 60 -> GlucoseAlertLevel.LOW_URGENT
            value <= 70 -> GlucoseAlertLevel.LOW_WARNING
            value >= 350 -> GlucoseAlertLevel.HIGH_CRITICAL
            value >= 240 -> GlucoseAlertLevel.HIGH_VERY_HIGH
            value >= 200 -> GlucoseAlertLevel.HIGH_WARNING
            else -> GlucoseAlertLevel.NONE
        }
    }
}

