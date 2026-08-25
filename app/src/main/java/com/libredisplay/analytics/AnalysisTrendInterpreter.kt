package com.libredisplay.analytics

import kotlin.math.roundToInt

/**
 * Turns the daily overlay into short, human-readable Polish observations about repeatable
 * time-of-day patterns (e.g. morning rise, high sugars around noon, night lows).
 */
object AnalysisTrendInterpreter {

    fun interpret(overlay: FourteenDayOverlay, targetLow: Int, targetHigh: Int): List<String> {
        val points = overlay.dayLines.flatMap { it.points }
        if (points.size < 24) {
            return listOf("Za mało danych, aby wykryć powtarzalne wzorce dobowe. Zbieraj dane przez kilka dni.")
        }
        val byHour = points.groupBy { it.minuteOfDay / 60 }
            .mapValues { entry -> entry.value.map { it.valueMgDl }.average() }
        if (byHour.size < 6) {
            return listOf("Za mało godzin z danymi, aby wykryć powtarzalne wzorce dobowe.")
        }

        val observations = mutableListOf<String>()
        val peak = byHour.maxByOrNull { it.value }!!
        val trough = byHour.minByOrNull { it.value }!!

        observations += "Najwyższe wartości pojawiają się zwykle około %02d:00 (≈%d mg/dL)."
            .format(peak.key, peak.value.roundToInt())
        observations += "Najniższe wartości pojawiają się zwykle około %02d:00 (≈%d mg/dL)."
            .format(trough.key, trough.value.roundToInt())

        val morningEarly = averageOf(byHour, 6..8)
        val morningLate = averageOf(byHour, 9..11)
        if (morningEarly != null && morningLate != null) {
            val diff = morningLate - morningEarly
            when {
                diff > 15 -> observations += "Rano (6–11) obserwowany jest wzrost glikemii (≈+%d mg/dL).".format(diff.roundToInt())
                diff < -15 -> observations += "Rano (6–11) obserwowany jest spadek glikemii (≈%d mg/dL).".format(diff.roundToInt())
            }
        }

        averageOf(byHour, 11..14)?.let { noon ->
            if (noon > targetHigh) {
                observations += "Około południa glikemia bywa podwyższona (≈%d mg/dL).".format(noon.roundToInt())
            }
        }
        averageOf(byHour, 0..4)?.let { night ->
            if (night < targetLow) {
                observations += "W nocy (0–5) wartości bywają niskie (≈%d mg/dL).".format(night.roundToInt())
            }
        }

        val amplitude = peak.value - trough.value
        if (amplitude > 60) {
            observations += "Duża zmienność dobowa – rozstęp średnich ≈%d mg/dL.".format(amplitude.roundToInt())
        }
        if (byHour.values.all { it in targetLow.toDouble()..targetHigh.toDouble() }) {
            observations += "Profil dobowy jest w większości w zakresie docelowym."
        }
        return observations.take(6)
    }

    private fun averageOf(byHour: Map<Int, Double>, range: IntRange): Double? {
        val values = range.mapNotNull { byHour[it] }
        return if (values.isEmpty()) null else values.average()
    }
}

