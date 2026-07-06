package com.libredisplay.data.api

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseHistoryStats
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.data.model.LibreConnectionPerson
import java.time.Instant
import kotlin.math.sin

class MockLibreLinkUpClient : LibreLinkUpClient {

    private var loggedIn = false
    private var tick = 0
    private var selectedPatientId: String? = null

    private val persons = listOf(
        LibreConnectionPerson(
            patientId = "mock-mama",
            displayName = "Mama",
            firstName = "Mama",
            lastName = null
        ),
        LibreConnectionPerson(
            patientId = "mock-tata",
            displayName = "Tata",
            firstName = "Tata",
            lastName = null
        )
    )

    override suspend fun login(email: String, password: String) {
        loggedIn = true
    }

    override suspend fun getConnections(): List<LibreConnectionPerson> {
        require(loggedIn) { "Tryb testowy wymaga aktywnej sesji" }
        return persons
    }

    override suspend fun getLatestReading(patientId: String?): GlucoseReading? {
        require(loggedIn) { "Tryb testowy wymaga aktywnej sesji" }
        val resolvedPatientId = patientId ?: selectedPatientId ?: persons.first().patientId
        selectedPatientId = resolvedPatientId
        tick += 1
        val now = Instant.now()
        val offset = if (resolvedPatientId == persons.first().patientId) 0 else 9
        val baseValue = if (resolvedPatientId == persons.first().patientId) 136.0 else 168.0
        val history = (0 until 48).map { index ->
            val reversed = 47 - index
            val timestamp = now.minusSeconds(reversed * 15L * 60L)
            val base = baseValue + (sin((tick + index + offset) / 4.0) * 24.0)
            val noise = ((index + tick + offset) % 5) - 2
            val value = (base + noise).toInt().coerceIn(75, 250)
            val delta = sin((tick + index + offset) / 5.0) * 3.5
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
            historyHoursAvailable = 12.0
        )
    }
}
