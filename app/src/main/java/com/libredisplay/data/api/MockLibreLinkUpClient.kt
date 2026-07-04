package com.libredisplay.data.api

import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.random.Random

/**
 * Mock implementation of [LibreLinkUpClient].
 *
 * Generates realistic, random glucose readings in the range 60–250 mg/dL.
 * No network requests are made. Suitable for UI development and on-device demos
 * without a real LibreLinkUp account.
 *
 * Replace this with [RealLibreLinkUpClient] once the real API integration is ready.
 */
class MockLibreLinkUpClient : LibreLinkUpClient {

    private var loggedIn = false

    // Simulated "current" glucose that drifts slowly over time
    private var simulatedGlucose: Int = 110
    private var lastReadingTime: Instant = Instant.now()

    override suspend fun login(email: String, password: String) {
        // Simulate network latency
        delay(800)
        // Accept any non-empty credentials for mock purposes
        if (email.isBlank() || password.isBlank()) {
            throw LibreLinkUpException("Mock: email and password must not be empty")
        }
        loggedIn = true
        // NOTE: password is intentionally NOT logged anywhere
    }

    override suspend fun getConnections(): List<String> {
        delay(400)
        requireLoggedIn()
        return listOf("Mock Patient – Parent")
    }

    override suspend fun getLatestReading(): GlucoseReading {
        delay(600)
        requireLoggedIn()

        // Drift the simulated glucose by a random delta each call
        val delta = Random.nextInt(-8, 9)
        simulatedGlucose = (simulatedGlucose + delta).coerceIn(60, 250)

        val trend = when {
            delta >= 4  -> GlucoseTrend.RISING_FAST
            delta >= 2  -> GlucoseTrend.RISING
            delta <= -4 -> GlucoseTrend.FALLING_FAST
            delta <= -2 -> GlucoseTrend.FALLING
            else        -> GlucoseTrend.FLAT
        }

        lastReadingTime = Instant.now()
        return GlucoseReading.of(
            value = simulatedGlucose,
            timestamp = lastReadingTime,
            trend = trend
        )
    }

    private fun requireLoggedIn() {
        if (!loggedIn) throw LibreLinkUpException("Mock: not logged in – call login() first")
    }
}

