package com.libredisplay.data.demo

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseHistoryStats
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.data.model.LibreConnectionPerson
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Deterministic synthetic glucose data generator for controlled demo scenarios.
 *
 * Pure Kotlin — no Android dependencies. Logic mirrors [GlucoseScenarioEngine] in sharedTest
 * but is self-contained so it can run inside the debug APK at runtime.
 *
 * This is an internal implementation detail consumed only by [ScenarioAwareMockLibreLinkUpClient].
 */
internal object ScenarioDataGenerator {

    /** Single patient used for all single-person scenarios. */
    private val SINGLE_PERSON = LibreConnectionPerson(
        patientId = "scenario-single",
        displayName = "Anna Kowalska",
        firstName = "Anna",
        lastName = "Kowalska"
    )

    /** The "normal" patient in the multi-patient at-risk scenario. */
    val NORMAL_PATIENT_ID = "scenario-normal"

    /** The at-risk patient in the multi-patient at-risk scenario. */
    val AT_RISK_PATIENT_ID = "scenario-at-risk"

    private val NORMAL_PERSON = LibreConnectionPerson(
        patientId = NORMAL_PATIENT_ID,
        displayName = "Jan Kowalski",
        firstName = "Jan",
        lastName = "Kowalski"
    )
    private val AT_RISK_PERSON = LibreConnectionPerson(
        patientId = AT_RISK_PATIENT_ID,
        displayName = "Anna Kowalska",
        firstName = "Anna",
        lastName = "Kowalska"
    )

    /** Returns the list of LibreConnectionPersons appropriate for [scenario]. */
    fun personsForScenario(scenario: DemoScenario): List<LibreConnectionPerson> =
        if (scenario == DemoScenario.MULTIPLE_PATIENTS_ONE_AT_RISK) {
            listOf(NORMAL_PERSON, AT_RISK_PERSON)
        } else {
            listOf(SINGLE_PERSON)
        }

    /**
     * Generate a [GlucoseReading] for [scenario].
     *
     * For [DemoScenario.MULTIPLE_PATIENTS_ONE_AT_RISK] the caller passes the [patientId] from
     * [personsForScenario] and receives person-specific data.
     *
     * Returns `null` for [DemoScenario.MISSING_DATA] to simulate no-data state.
     */
    fun generateReading(
        scenario: DemoScenario,
        patientId: String?,
        now: Instant
    ): GlucoseReading? = when (scenario) {
        DemoScenario.NORMAL ->
            buildFlat(now, glucose = 112, trend = GlucoseTrend.FLAT)

        DemoScenario.RAPID_RISE ->
            buildLinear(now, startGlucose = 118, ratePerHour = +8.0, trend = GlucoseTrend.RISING_FAST)

        DemoScenario.RAPID_FALL ->
            buildLinear(now, startGlucose = 188, ratePerHour = -8.0, trend = GlucoseTrend.FALLING_FAST)

        DemoScenario.HYPO ->
            buildFlat(now, glucose = 58, trend = GlucoseTrend.FALLING)

        DemoScenario.SEVERE_HYPO ->
            buildFlat(now, glucose = 38, trend = GlucoseTrend.FALLING_FAST)

        DemoScenario.HYPER ->
            buildFlat(now, glucose = 298, trend = GlucoseTrend.RISING)

        DemoScenario.STALE_DATA ->
            buildStale(now)

        DemoScenario.MISSING_DATA ->
            buildMissing(now)

        DemoScenario.MULTIPLE_PATIENTS_ONE_AT_RISK -> when (patientId) {
            AT_RISK_PATIENT_ID -> buildFlat(now, glucose = 48, trend = GlucoseTrend.FALLING_FAST)
            else               -> buildFlat(now, glucose = 112, trend = GlucoseTrend.FLAT)
        }
    }

    // -------------------------------------------------------------------------
    // Private builders
    // -------------------------------------------------------------------------

    private fun buildFlat(
        now: Instant,
        glucose: Int,
        trend: GlucoseTrend,
        historyHours: Long = 24L,
        ageMinutes: Long = 5L
    ): GlucoseReading {
        val currentTs = now.minusSeconds(ageMinutes * 60)
        val history = generateFlatHistory(currentTs, historyHours, glucose)
        return makeReading(glucose, currentTs, trend, history)
    }

    private fun buildLinear(
        now: Instant,
        startGlucose: Int,
        ratePerHour: Double,
        trend: GlucoseTrend,
        historyHours: Long = 24L,
        ageMinutes: Long = 5L
    ): GlucoseReading {
        val currentTs = now.minusSeconds(ageMinutes * 60)
        val currentGlucose = (startGlucose + ratePerHour * historyHours).roundToInt().coerceIn(1, 600)
        val history = generateLinearHistory(currentTs, historyHours, startGlucose, ratePerHour)
        return makeReading(currentGlucose, currentTs, trend, history)
    }

    /**
     * Stale data: last reading is ~26 hours old. The UI should display a stale-data warning
     * because the reading timestamp is far in the past.
     */
    private fun buildStale(now: Instant): GlucoseReading {
        val latestTs = now.minus(Duration.ofHours(26))
        val historyStart = latestTs.minus(Duration.ofHours(24))
        val history = generateFlatHistoryBetween(historyStart, latestTs, glucose = 146)
        return makeReading(146, latestTs, GlucoseTrend.FLAT, history)
    }

    /**
     * Missing data: last reading is ~72 hours old (sensor expired / sensor removed).
     * Returns a reading rather than null so that the ViewModel can still render a meaningful
     * stale/missing-data state without throwing a SelectedPersonGraphException.
     */
    private fun buildMissing(now: Instant): GlucoseReading {
        val latestTs = now.minus(Duration.ofHours(72))
        val historyStart = latestTs.minus(Duration.ofHours(24))
        val history = generateFlatHistoryBetween(historyStart, latestTs, glucose = 118)
        return makeReading(118, latestTs, GlucoseTrend.FLAT, history)
    }

    // -------------------------------------------------------------------------
    // History generators
    // -------------------------------------------------------------------------

    private fun generateFlatHistory(
        latestTs: Instant,
        historyHours: Long,
        glucose: Int
    ): List<GlucoseHistoryPoint> {
        val start = latestTs.minus(Duration.ofHours(historyHours))
        return generateFlatHistoryBetween(start, latestTs.minusSeconds(5 * 60), glucose)
    }

    private fun generateFlatHistoryBetween(
        start: Instant,
        end: Instant,
        glucose: Int
    ): List<GlucoseHistoryPoint> {
        val intervalSec = 5L * 60L
        val steps = ((end.epochSecond - start.epochSecond) / intervalSec).toInt().coerceAtLeast(0)
        val noisePattern = intArrayOf(0, 1, -1, 2, -2, 1, 0, -1)
        return (0 until steps).map { i ->
            val noise = noisePattern[i % noisePattern.size]
            GlucoseHistoryPoint(
                value = (glucose + noise).coerceIn(1, 600),
                timestamp = start.plusSeconds(i * intervalSec),
                trend = GlucoseTrend.FLAT
            )
        }
    }

    private fun generateLinearHistory(
        latestTs: Instant,
        historyHours: Long,
        startGlucose: Int,
        ratePerHour: Double
    ): List<GlucoseHistoryPoint> {
        val start = latestTs.minus(Duration.ofHours(historyHours))
        val end = latestTs.minusSeconds(5 * 60)
        val intervalSec = 5L * 60L
        val steps = ((end.epochSecond - start.epochSecond) / intervalSec).toInt().coerceAtLeast(0)
        val intervalHours = intervalSec / 3600.0
        return (0 until steps).mapIndexed { i, _ ->
            val elapsedHours = i * intervalHours
            val value = (startGlucose + ratePerHour * elapsedHours).roundToInt().coerceIn(1, 600)
            val slope = ratePerHour / 60.0  // mg/dL per minute
            GlucoseHistoryPoint(
                value = value,
                timestamp = start.plusSeconds(i * intervalSec),
                trend = GlucoseTrend.fromSlope(slope)
            )
        }
    }

    // -------------------------------------------------------------------------
    // Reading factory
    // -------------------------------------------------------------------------

    private fun makeReading(
        value: Int,
        timestamp: Instant,
        trend: GlucoseTrend,
        history: List<GlucoseHistoryPoint>
    ): GlucoseReading {
        val stats = GlucoseHistoryStats.from(history, 80, 180)
        return GlucoseReading.of(
            value = value,
            timestamp = timestamp,
            trend = trend,
            history = history,
            stats = stats,
            historyHoursAvailable = 24.0
        )
    }
}

