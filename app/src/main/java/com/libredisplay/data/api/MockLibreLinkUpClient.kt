package com.libredisplay.data.api

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseHistoryStats
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.data.model.LibreConnectionPerson
import java.time.Instant
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MockLibreLinkUpClient : LibreLinkUpClient {

    private var loggedIn = false
    private var selectedPatientId: String? = null

    private val persons = listOf(
        LibreConnectionPerson(
            patientId = "demo-person-anna",
            displayName = "Anna Kowalska",
            firstName = "Anna",
            lastName = "Kowalska"
        ),
        LibreConnectionPerson(
            patientId = "demo-person-jan",
            displayName = "Jan Kowalski",
            firstName = "Jan",
            lastName = "Kowalski"
        ),
        LibreConnectionPerson(
            patientId = "demo-person-maria",
            displayName = "Maria Nowak",
            firstName = "Maria",
            lastName = "Nowak"
        )
    )

    override suspend fun login(email: String, password: String) {
        loggedIn = true
    }

    override suspend fun getConnections(): List<LibreConnectionPerson> {
        if (!loggedIn) {
            // Keep demo mode accessible even before explicit login calls.
            loggedIn = true
        }
        return persons
    }

    override suspend fun getLatestReading(patientId: String?): GlucoseReading? {
        if (!loggedIn) {
            loggedIn = true
        }
        val resolvedPatientId = patientId ?: selectedPatientId ?: persons.first().patientId
        selectedPatientId = resolvedPatientId
        val now = Instant.now().minusSeconds(5 * 60L)
        val pointsPerDay = 96 // 15-minute interval
        val totalPoints = 60 * pointsPerDay
        val seed = resolvedPatientId.hashCode().toLong()
        val personOffset = when (resolvedPatientId) {
            "demo-person-anna" -> 0.0
            "demo-person-jan" -> 0.7
            else -> 1.4
        }

        val history = (0 until totalPoints).map { index ->
            val reversed = totalPoints - 1 - index
            val timestamp = now.minusSeconds(reversed * 15L * 60L)
            val minuteOfDay = (timestamp.epochSecond % (24 * 3600)) / 60.0
            val dayIndex = index / pointsPerDay

            // Daily circadian rhythm + meal-like peaks + gentle deterministic noise.
            val circadian = 18.0 * sin((2.0 * PI * minuteOfDay / (24.0 * 60.0)) + personOffset)
            val breakfast = mealBump(minuteOfDay, centerMinute = 8.0 * 60.0, widthMinutes = 90.0, amplitude = 22.0)
            val lunch = mealBump(minuteOfDay, centerMinute = 13.0 * 60.0, widthMinutes = 105.0, amplitude = 28.0)
            val dinner = mealBump(minuteOfDay, centerMinute = 19.0 * 60.0, widthMinutes = 110.0, amplitude = 24.0)
            val slowWave = 7.0 * sin((2.0 * PI * dayIndex / 7.0) + personOffset)
            val deterministicNoise = (((seed + index * 37L) % 9L) - 4L).toDouble()
            val occasionalShift = if ((index + seed).mod(401L) == 0L) 24.0 else if ((index + seed).mod(613L) == 0L) -16.0 else 0.0

            val baseline = 112.0 + when (resolvedPatientId) {
                "demo-person-anna" -> 4.0
                "demo-person-jan" -> 10.0
                else -> 1.0
            }

            val value = (baseline + circadian + breakfast + lunch + dinner + slowWave + deterministicNoise + occasionalShift)
                .toInt()
                .coerceIn(68, 208)

            val slopeSignal =
                0.9 * cos((2.0 * PI * minuteOfDay / (24.0 * 60.0)) + personOffset) +
                    1.4 * mealSlope(minuteOfDay, 8.0 * 60.0, 90.0) +
                    1.7 * mealSlope(minuteOfDay, 13.0 * 60.0, 105.0) +
                    1.5 * mealSlope(minuteOfDay, 19.0 * 60.0, 110.0)

            val delta = slopeSignal * 2.4
            GlucoseHistoryPoint(
                value = value,
                timestamp = timestamp,
                trend = GlucoseTrend.fromSlope(delta)
            )
        }
        val current = history.last()
        val stats = GlucoseHistoryStats.from(history, 80, 180)
        return GlucoseReading.of(
            value = current.value,
            timestamp = current.timestamp,
            trend = current.trend,
            history = history,
            stats = stats,
            historyHoursAvailable = 24.0 * 60.0
        )
    }

    private fun mealBump(minuteOfDay: Double, centerMinute: Double, widthMinutes: Double, amplitude: Double): Double {
        val delta = minuteOfDay - centerMinute
        val sigma = widthMinutes / 2.8
        return amplitude * kotlin.math.exp(-(delta * delta) / (2.0 * sigma * sigma))
    }

    private fun mealSlope(minuteOfDay: Double, centerMinute: Double, widthMinutes: Double): Double {
        val delta = minuteOfDay - centerMinute
        val sigma = widthMinutes / 2.8
        val gaussian = kotlin.math.exp(-(delta * delta) / (2.0 * sigma * sigma))
        return -delta / (sigma * sigma) * gaussian
    }
}
