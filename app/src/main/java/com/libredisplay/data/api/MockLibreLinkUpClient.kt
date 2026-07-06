package com.libredisplay.data.api

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseHistoryStats
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import java.time.Instant
import kotlin.math.sin
import kotlin.random.Random

class MockLibreLinkUpClient : LibreLinkUpClient {

    private var loggedIn = false
    private var tick = 0

    override suspend fun login(email: String, password: String) {
        loggedIn = true
    }

    override suspend fun getConnections(): List<String> {
        require(loggedIn) { "Tryb testowy wymaga aktywnej sesji" }
        return listOf("mock-patient")
    }

    override suspend fun getLatestReading(): GlucoseReading {
        require(loggedIn) { "Tryb testowy wymaga aktywnej sesji" }
        tick += 1
        val now = Instant.now()
        val history = (0 until 48).map { index ->
            val reversed = 47 - index
            val timestamp = now.minusSeconds(reversed * 15L * 60L)
            val base = 145.0 + (sin((tick + index) / 4.0) * 28.0)
            val noise = ((index + tick) % 5) - 2
            val value = (base + noise).toInt().coerceIn(75, 250)
            val delta = sin((tick + index) / 5.0) * 3.5
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
