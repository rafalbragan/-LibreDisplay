package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NfzAssessmentTest {

    @Test
    fun sensorActivity80_meetsCriterion() {
        val assessment = assessNfzRefundContinuation(
            history = denseHistory(days = 10, intervalMinutes = 15),
            targetLow = 70,
            targetHigh = 180
        )

        val criterion = assessment.criteria.first { it.condition == "Aktywność czujnika" }
        assertEquals(NfzCriterionStatus.MET, criterion.status)
    }

    @Test
    fun sensorActivity60_failsWithReason() {
        val assessment = assessNfzRefundContinuation(
            history = denseHistory(days = 10, intervalMinutes = 30),
            targetLow = 70,
            targetHigh = 180
        )

        val criterion = assessment.criteria.first { it.condition == "Aktywność czujnika" }
        assertEquals(NfzCriterionStatus.NOT_MET, criterion.status)
        assertTrue(criterion.reason.contains("niższa", ignoreCase = true) || criterion.reason.contains("niższa"))
    }

    @Test
    fun missingSensorActivity_isUnknown() {
        val assessment = assessNfzRefundContinuation(
            history = listOf(point(120, "2026-08-01T00:00:00Z")),
            targetLow = 70,
            targetHigh = 180
        )

        val criterion = assessment.criteria.first { it.condition == "Aktywność czujnika" }
        assertEquals(NfzCriterionStatus.UNKNOWN, criterion.status)
    }

    @Test
    fun tir75_meetsCriterion() {
        val assessment = assessNfzRefundContinuation(
            history = mixedHistory(inRangeRatio = 0.75),
            targetLow = 70,
            targetHigh = 180
        )

        val criterion = assessment.criteria.first { it.condition == "TIR 70-180 mg/dL" }
        assertEquals(NfzCriterionStatus.MET, criterion.status)
    }

    @Test
    fun tir64_failsWithReason() {
        val assessment = assessNfzRefundContinuation(
            history = mixedHistory(inRangeRatio = 0.64),
            targetLow = 70,
            targetHigh = 180
        )

        val criterion = assessment.criteria.first { it.condition == "TIR 70-180 mg/dL" }
        assertEquals(NfzCriterionStatus.NOT_MET, criterion.status)
        assertTrue(criterion.reason.contains("poniżej progu"))
    }

    @Test
    fun hba1c72_meetsCriterion() {
        val assessment = assessNfzRefundContinuation(
            history = denseHistory(days = 5, intervalMinutes = 15),
            targetLow = 70,
            targetHigh = 180,
            profile = NfzPatientProfile(hbA1cPercent = 7.2, profileCompleted = true)
        )

        val criterion = assessment.criteria.first { it.condition == "HbA1c / GMI" }
        assertEquals(NfzCriterionStatus.MET, criterion.status)
    }

    @Test
    fun hba1cMissingAndGmiMissing_returnsUnknown() {
        val assessment = assessNfzRefundContinuation(
            history = listOf(point(120, "2026-08-01T00:00:00Z")),
            targetLow = 70,
            targetHigh = 180,
            profile = NfzPatientProfile()
        )

        val criterion = assessment.criteria.first { it.condition == "HbA1c / GMI" }
        assertEquals(NfzCriterionStatus.UNKNOWN, criterion.status)
    }

    @Test
    fun childMonitoringAverage8_meetsCriterion() {
        val assessment = assessNfzRefundContinuation(
            history = denseHistory(days = 5, intervalMinutes = 180),
            targetLow = 70,
            targetHigh = 180,
            profile = NfzPatientProfile(patientGroup = NfzPatientGroup.CHILD, profileCompleted = true)
        )

        val criterion = assessment.criteria.first { it.condition == "Regularność monitorowania" }
        assertEquals(NfzCriterionStatus.MET, criterion.status)
    }

    @Test
    fun childMonitoringAverage5_isNotMet() {
        val assessment = assessNfzRefundContinuation(
            history = denseHistory(days = 5, intervalMinutes = 288),
            targetLow = 70,
            targetHigh = 180,
            profile = NfzPatientProfile(patientGroup = NfzPatientGroup.CHILD, profileCompleted = true)
        )

        val criterion = assessment.criteria.first { it.condition == "Regularność monitorowania" }
        assertEquals(NfzCriterionStatus.NOT_MET, criterion.status)
        assertTrue(criterion.reason.contains("zbyt niska"))
    }

    @Test
    fun longGap_addsRecommendationAboutReducingGaps() {
        val assessment = assessNfzRefundContinuation(
            history = listOf(
                point(120, "2026-08-01T00:00:00Z"),
                point(125, "2026-08-01T00:15:00Z"),
                point(130, "2026-08-05T00:15:00Z")
            ),
            targetLow = 70,
            targetHigh = 180
        )

        assertTrue(assessment.recommendations.any { it.text.contains("Unikaj długich przerw", ignoreCase = true) })
    }

    @Test
    fun missingPatientProfile_addsRecommendation() {
        val assessment = assessNfzRefundContinuation(
            history = denseHistory(days = 3, intervalMinutes = 15),
            targetLow = 70,
            targetHigh = 180,
            profile = NfzPatientProfile()
        )

        assertTrue(assessment.recommendations.any { it.text.contains("Uzupełnij brakujące informacje o pacjencie") })
    }

    @Test
    fun infoDialogContent_mentionsThresholdsAndDisclaimer() {
        val text = buildNfzInfoDialogText(NfzRefundCriteriaConfig())

        assertTrue(text.contains("75%"))
        assertTrue(text.contains("TIR 70-180 mg/dL"))
        assertTrue(text.contains("HbA1c"))
        assertTrue(text.contains("nie zastępuje lekarza", ignoreCase = true) || text.contains("nie jest oficjalnym"))
    }

    @Test
    fun recommendations_areSortedByPriority() {
        val assessment = assessNfzRefundContinuation(
            history = mixedHistory(inRangeRatio = 0.40),
            targetLow = 70,
            targetHigh = 180,
            profile = NfzPatientProfile(patientGroup = NfzPatientGroup.UNKNOWN)
        )

        val priorities = assessment.recommendations.map { it.priority }
        assertEquals(priorities.sorted(), priorities)
    }

    @Test
    fun noFinalLegalDecisionWordingIsUsed() {
        val assessment = assessNfzRefundContinuation(
            history = denseHistory(days = 3, intervalMinutes = 15),
            targetLow = 70,
            targetHigh = 180
        )

        assertFalse(assessment.headline.contains("na pewno", ignoreCase = true))
        assertFalse(assessment.details.contains("na pewno", ignoreCase = true))
    }

    private fun denseHistory(days: Int, intervalMinutes: Int): List<GlucoseHistoryPoint> {
        val start = Instant.parse("2026-08-01T00:00:00Z")
        val totalPoints = ((days * 24 * 60) / intervalMinutes)
        return (0 until totalPoints).map { index ->
            point(
                value = 120,
                instant = start.plusSeconds(index.toLong() * intervalMinutes * 60L)
            )
        }
    }

    private fun mixedHistory(inRangeRatio: Double): List<GlucoseHistoryPoint> {
        val start = Instant.parse("2026-08-01T00:00:00Z")
        return (0 until 96).map { index ->
            val inRange = index < (96 * inRangeRatio).toInt()
            point(
                value = if (inRange) 120 else 220,
                instant = start.plusSeconds(index * 900L)
            )
        }
    }

    private fun point(value: Int, iso: String): GlucoseHistoryPoint = point(value, Instant.parse(iso))

    private fun point(value: Int, instant: Instant): GlucoseHistoryPoint = GlucoseHistoryPoint(
        value = value,
        timestamp = instant,
        trend = GlucoseTrend.FLAT
    )
}

