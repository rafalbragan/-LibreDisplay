package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.data.model.HbA1cSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class HbA1cGmiCalculatorTest {

    @Test
    fun gmi_forAverage154_isAbout699() {
        val result = calculateGmi(154.0)
        assertEquals(6.99, result, 0.02)
    }

    @Test
    fun noData_showsGmiAsMissing() {
        val model = buildHbA1cKpiModel(
            hba1cSettings = HbA1cSettings(patientId = "p1"),
            historyPoints = emptyList()
        )

        assertEquals(HbA1cKpiMode.GMI_INSUFFICIENT, model.mode)
        assertNull(model.valuePercent)
    }

    @Test
    fun lessThan14Days_marksInsufficientData() {
        val history = (0 until 48).map { index ->
            GlucoseHistoryPoint(
                value = 120,
                timestamp = Instant.parse("2026-07-27T00:00:00Z").plusSeconds(index * 900L),
                trend = GlucoseTrend.FLAT
            )
        }

        val model = buildHbA1cKpiModel(
            hba1cSettings = HbA1cSettings(patientId = "p1"),
            historyPoints = history
        )

        assertEquals(HbA1cKpiMode.GMI_INSUFFICIENT, model.mode)
        assertTrue(model.insufficientData)
    }

    @Test
    fun laboratoryHbA1c_hasPriorityOverGmi() {
        val history = fourteenDaysHistory()
        val model = buildHbA1cKpiModel(
            hba1cSettings = HbA1cSettings(
                patientId = "p1",
                labHbA1cPercent = 7.2,
                labHbA1cDate = LocalDate.parse("2026-07-15"),
                targetHbA1cPercent = 7.5
            ),
            historyPoints = history
        )

        assertEquals(HbA1cKpiMode.LAB_HBA1C, model.mode)
        assertEquals(7.2, model.valuePercent ?: 0.0, 0.001)
    }

    @Test
    fun hba1c74_withTarget75_isBelowTarget() {
        assertEquals(HbA1cStatus.BELOW_TARGET, evaluateHbA1cStatus(7.4, 7.5))
    }

    @Test
    fun hba1c75_withTarget75_isNotBelowTarget() {
        val status = evaluateHbA1cStatus(7.5, 7.5)
        assertTrue(status == HbA1cStatus.NEAR_TARGET || status == HbA1cStatus.ABOVE_TARGET)
    }

    @Test
    fun gmi_mode_isNotLabeledAsLabHbA1c() {
        val model = buildHbA1cKpiModel(
            hba1cSettings = HbA1cSettings(patientId = "p1"),
            historyPoints = fourteenDaysHistory()
        )

        assertEquals(HbA1cKpiMode.GMI_ESTIMATED, model.mode)
    }

    @Test
    fun dashboardCard_usesSelectedPersonContext() {
        val reading = GlucoseReading.of(
            value = 130,
            timestamp = Instant.parse("2026-07-27T12:00:00Z"),
            trend = GlucoseTrend.FLAT,
            history = fourteenDaysHistory()
        )
        val state = MonitoringUiState(
            settings = AppSettings(targetLow = 80, targetHigh = 180),
            selectedPatientId = "patient-2",
            selectedPersonName = "Halina",
            reading = reading,
            trend = reading.trend,
            labHbA1cPercent = 7.1,
            labHbA1cDate = LocalDate.parse("2026-07-15"),
            targetHbA1cPercent = 7.5
        )

        val dashboard = state.toDashboardUiState(now = Instant.parse("2026-07-27T12:05:00Z"))

        assertEquals("patient-2", dashboard.selectedPersonHbA1cSettings.patientId)
        assertEquals(HbA1cKpiMode.LAB_HBA1C, dashboard.hba1cKpi.mode)
    }

    private fun fourteenDaysHistory(): List<GlucoseHistoryPoint> {
        val start = Instant.parse("2026-07-01T00:00:00Z")
        return (0..56).map { day ->
            GlucoseHistoryPoint(
                value = 120 + (day % 10),
                timestamp = start.plusSeconds(day * 6L * 3600L),
                trend = GlucoseTrend.FLAT
            )
        }
    }
}

