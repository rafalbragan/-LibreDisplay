package com.libredisplay.ui.monitoring

import android.content.res.Configuration
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.data.model.LibreConnectionPerson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

class DashboardUiLogicTest {

    @Test
    fun connectedState_hidesRetryActionCard() {
        assertFalse(shouldShowRetryAction(ConnectionState.Connected))
    }

    @Test
    fun disconnectedState_showsRetryActionCard() {
        assertTrue(shouldShowRetryAction(ConnectionState.Disconnected))
    }

    @Test
    fun patientSwitcher_supportsTwoPersons() {
        val persons = listOf(
            LibreConnectionPerson("p1", "Ryszard"),
            LibreConnectionPerson("p2", "Halina")
        )

        val mapped = mapSwitcherPersons(persons)

        assertEquals(2, mapped.size)
        assertEquals("Ryszard", mapped[0].displayName)
        assertEquals("Halina", mapped[1].displayName)
    }

    @Test
    fun selectedPerson_isReflectedInTopAndChartTitles() {
        assertEquals("Osoba: Ryszard", topBarPersonSubtitle("Ryszard"))
        assertEquals("Historia glukozy - Ryszard", chartTitleForPerson("Ryszard"))
    }

    @Test
    fun personSelection_dispatchesCallbackWithPatientId() {
        val selected = AtomicReference<String?>(null)

        dispatchPersonSelection("p2") { patientId ->
            selected.set(patientId)
        }

        assertEquals("p2", selected.get())
    }

    @Test
    fun chartReceivesHistoryFromSelectedState() {
        val history = listOf(
            GlucoseHistoryPoint(value = 120, timestamp = Instant.parse("2026-07-27T10:00:00Z"), trend = GlucoseTrend.FLAT),
            GlucoseHistoryPoint(value = 124, timestamp = Instant.parse("2026-07-27T10:15:00Z"), trend = GlucoseTrend.RISING)
        )
        val reading = GlucoseReading.of(
            value = 124,
            timestamp = Instant.parse("2026-07-27T10:15:00Z"),
            trend = GlucoseTrend.RISING,
            history = history
        )

        val dashboard = MonitoringUiState(
            reading = reading,
            trend = reading.trend,
            connectionState = ConnectionState.Connected
        ).toDashboardUiState(now = Instant.parse("2026-07-27T10:16:00Z"))

        assertEquals(history, dashboard.historyPoints)
        assertEquals(1L, dashboard.readingAgeMinutes)
    }

    @Test
    fun portraitAndLandscapeLayoutModes_areDetected() {
        assertTrue(isLandscapeDashboard(Configuration.ORIENTATION_LANDSCAPE))
        assertFalse(isLandscapeDashboard(Configuration.ORIENTATION_PORTRAIT))
    }

    @Test
    fun trendArrowTypography_isComparableToGlucoseValue() {
        val typography = glucoseCardTypographyForWidth(screenWidthDp = 411)

        assertTrue(typography.trendArrowSp >= 72)
        assertEquals(typography.glucoseValueSp, typography.trendArrowSp)
        assertTrue(typography.trendDescriptionSp >= 20)
    }

    @Test
    fun trendContentDescription_isHumanReadable() {
        assertEquals("Glikemia stabilna", trendContentDescription(GlucoseTrend.FLAT))
        assertEquals("Glikemia rośnie", trendContentDescription(GlucoseTrend.RISING))
    }

    @Test
    fun formatChartPointLabel_formatsPolishDateTimeAndStatus() {
        val point = GlucoseHistoryPoint(
            value = 145,
            timestamp = Instant.parse("2026-07-27T16:10:00Z"),
            trend = GlucoseTrend.FLAT
        )

        val label = formatChartPointLabel(
            point = point,
            targetLow = 80,
            targetHigh = 180,
            zoneId = ZoneId.of("Europe/Warsaw")
        )

        assertTrue(label.dateTime.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}")))
        assertEquals("145 mg/dL", label.valueText)
        assertEquals("W zakresie", label.statusText)
    }

    @Test
    fun glucoseValueAndUnitText_isSingleDisplayToken() {
        assertEquals("225 mg/dL", glucoseValueAndUnitText(225))
    }

    @Test
    fun rangeTileUi_noData_usesDashAndNoFakeZero() {
        val tile = rangeTileUi(percent = null, duration = null)

        assertEquals("—", tile.percentLabel)
        assertEquals("brak danych", tile.durationLabel)
        assertFalse(tile.hasData)
    }

    @Test
    fun formatReadingAge_forOldData_mentionsDays() {
        val label = formatReadingAge(Duration.ofMinutes(3015))
        assertTrue(label.contains("dni"))
    }

    @Test
    fun evaluateNfzStatus_mapsGreenYellowRedGray() {
        assertEquals(NfzStatus.GRAY, evaluateNfzStatus(null, null, null).status)
        assertEquals(NfzStatus.GREEN, evaluateNfzStatus(80.0, 75, 7.4).status)
        assertEquals(NfzStatus.YELLOW, evaluateNfzStatus(70.0, 60, null).status)
        assertEquals(NfzStatus.RED, evaluateNfzStatus(40.0, 30, null).status)
    }
}


