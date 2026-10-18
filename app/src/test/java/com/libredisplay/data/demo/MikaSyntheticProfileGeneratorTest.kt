package com.libredisplay.data.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class MikaSyntheticProfileGeneratorTest {

    @Test
    fun generate_isDeterministicForSameAnchorDay() {
        val now = Instant.parse("2026-10-17T08:00:00Z")
        val first = MikaSyntheticProfileGenerator.generate(now)
        val second = MikaSyntheticProfileGenerator.generate(now)

        assertEquals(first.timestamp, second.timestamp)
        assertEquals(first.value, second.value)
        assertEquals(first.history.size, second.history.size)
        assertEquals(first.history.first(), second.history.first())
        assertEquals(first.history.last(), second.history.last())
    }

    @Test
    fun generate_coversRoughlyEighteenMonths() {
        val now = Instant.parse("2026-10-17T08:00:00Z")
        val reading = MikaSyntheticProfileGenerator.generate(now)
        val span = Duration.between(reading.history.first().timestamp, reading.history.last().timestamp)

        assertTrue("expected >= 500 days", span.toDays() >= 500)
        assertTrue("expected <= 560 days", span.toDays() <= 560)
        assertTrue(reading.history.size > 20_000)
    }

    @Test
    fun generate_latestPointIsNotInFuture() {
        val now = Instant.parse("2026-10-17T08:07:59Z")
        val reading = MikaSyntheticProfileGenerator.generate(now)

        assertTrue("latest timestamp must be <= now", !reading.timestamp.isAfter(now))
        assertTrue("last history point must be <= now", !reading.history.last().timestamp.isAfter(now))
    }
}

