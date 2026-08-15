package com.libredisplay.alert

object GlucoseAlertMessaging {
    fun messageFor(level: GlucoseAlertLevel): String? {
        return when (level) {
            GlucoseAlertLevel.NONE -> null
            GlucoseAlertLevel.LOW_WARNING -> "Niska glukoza - poziom ostrzegawczy"
            GlucoseAlertLevel.LOW_URGENT -> "Niska glukoza - poziom pilny"
            GlucoseAlertLevel.LOW_CRITICAL -> "Niska glukoza - poziom krytyczny"
            GlucoseAlertLevel.HIGH_WARNING -> "Wysoka glukoza - poziom ostrzegawczy"
            GlucoseAlertLevel.HIGH_VERY_HIGH -> "Wysoka glukoza - poziom bardzo wysoki"
            GlucoseAlertLevel.HIGH_CRITICAL -> "Wysoka glukoza - poziom krytyczny"
        }
    }
}

