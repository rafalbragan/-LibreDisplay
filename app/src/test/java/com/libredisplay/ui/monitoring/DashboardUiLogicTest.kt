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
import kotlin.math.roundToInt

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
    fun selectedPerson_identityAppearsOnlyOnceInDashboardModel() {
        val count = dashboardIdentityOccurrences(
            showTopBarIdentity = false,
            showHeaderIdentity = false,
            showSwitcherIdentity = true
        )
        assertEquals(1, count)
    }

    @Test
    fun personSwitcher_forTwoAndThreePersons_usesInlineChipsNotDropdown() {
        assertEquals(PersonSwitcherMode.INLINE_CHIPS, personSwitcherModeForCount(2))
        assertEquals(PersonSwitcherMode.INLINE_CHIPS, personSwitcherModeForCount(3))
    }

    @Test
    fun personSwitcher_forMoreThanThreePersons_usesScrollableChips() {
        assertEquals(PersonSwitcherMode.SCROLLABLE_CHIPS, personSwitcherModeForCount(4))
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
    fun rangeRow_exposesSelectedRangeLabel() {
        val label = TimeRangeState.fromPreset(PresetTimeRange.LAST_24_HOURS).rangeLabel()
        assertTrue(label.contains("Zakres:"))
        assertTrue(label.contains("24"))
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

        assertTrue(label.dateTime.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}, \\d{2}:\\d{2}:\\d{2}")))
        assertEquals("145 mg/dL", label.valueText)
        assertTrue(label.valueLabel.startsWith("Wartość:"))
        assertTrue(label.timeLabel.startsWith("Czas:"))
        assertEquals("W zakresie", label.statusText)
    }

    @Test
    fun glucoseValueAndUnitText_isSingleDisplayToken() {
        assertEquals("225 mg/dL", glucoseValueAndUnitText(225))
    }

    @Test
    fun glucoseCardModel_containsValueUnitTrendFreshnessAndStatus() {
        val reading = GlucoseReading.of(
            value = 225,
            timestamp = Instant.now().minus(Duration.ofHours(1)),
            trend = GlucoseTrend.FLAT,
            history = emptyList()
        )

        assertEquals("225 mg/dL", glucoseValueAndUnitText(reading.value))
        assertEquals("Glikemia stabilna", trendContentDescription(reading.trend))
        assertTrue(formatReadingAge(Duration.ofHours(1)).contains("temu"))
        assertEquals("Powyżej zakresu", glucoseRangeStatus(reading.value, 80, 180))
    }

    @Test
    fun rangeTileUi_noData_usesDashAndNoFakeZero() {
        val tile = rangeTileUi(percent = null, duration = null, hasReadings = false)

        assertEquals("—", tile.percentLabel)
        assertEquals("brak danych", tile.durationLabel)
        assertFalse(tile.hasData)
    }

    @Test
    fun rangeTileUi_existingReadingsAndZeroDuration_showsZeroMinutes() {
        val tile = rangeTileUi(percent = 0, duration = Duration.ZERO, hasReadings = true)

        assertEquals("0%", tile.percentLabel)
        assertEquals("0m", tile.durationLabel)
        assertTrue(tile.hasData)
    }

    @Test
    fun formatDurationLabel_usesCompactPolishUnits() {
        assertEquals("0m", formatDurationLabel(Duration.ZERO))
        assertEquals("45m", formatDurationLabel(Duration.ofMinutes(45)))
        assertEquals("1g", formatDurationLabel(Duration.ofMinutes(60)))
        assertEquals("1g 15m", formatDurationLabel(Duration.ofMinutes(75)))
        assertEquals("2g", formatDurationLabel(Duration.ofMinutes(120)))
        assertEquals("2g 30m", formatDurationLabel(Duration.ofMinutes(150)))
        assertEquals("1d 2g", formatDurationLabel(Duration.ofHours(26)))
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

    @Test
    fun historyContext_preservesSelectedPersonAndRange() {
        val state = MonitoringUiState(
            selectedPatientId = "p2",
            timeRange = TimeRangeState.fromPreset(PresetTimeRange.LAST_7_DAYS)
        )

        val context = buildHistoryOpenContext(state)

        assertEquals("p2", context.patientId)
        assertEquals(PresetTimeRange.LAST_7_DAYS, context.timeRange.presetRange)
    }

    @Test
    fun chartClickAction_opensHistoryWithSelectedPersonAndRange() {
        val state = MonitoringUiState(
            selectedPatientId = "p7",
            timeRange = TimeRangeState.fromPreset(PresetTimeRange.LAST_24_HOURS)
        )

        val action = chartClickAction(state)

        assertTrue(action is MonitoringAction.OpenHistory)
        val context = (action as MonitoringAction.OpenHistory).context
        assertEquals("p7", context.patientId)
        assertEquals(PresetTimeRange.LAST_24_HOURS, context.timeRange.presetRange)
    }

    @Test
    fun compactDashboardRangeLabel_isPolishAndTimezoneAware() {
        val label = compactDashboardRangeLabel(
            timeRange = TimeRangeState.fromPreset(PresetTimeRange.LAST_24_HOURS),
            latestReadingAt = Instant.parse("2026-08-18T17:07:00Z"),
            zoneId = ZoneId.of("Europe/Warsaw")
        )

        assertTrue(label.contains("Dane:"))
        assertTrue(label.contains("19:07"))
        assertTrue(label.contains("Zakres: 24h"))
    }

    @Test
    fun chartTitleForPerson_usesPolishHistoryTitle() {
        assertEquals("Historia glikemii - Halina", chartTitleForPerson("Halina"))
    }

    @Test
    fun dashboardMetricTiles_exposeFiveCompactStates() {
        val history = listOf(
            GlucoseHistoryPoint(120, Instant.parse("2026-07-27T10:00:00Z"), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(120, Instant.parse("2026-07-27T10:15:00Z"), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(220, Instant.parse("2026-07-27T10:30:00Z"), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(120, Instant.parse("2026-07-27T10:45:00Z"), GlucoseTrend.FLAT)
        )
        val reading = GlucoseReading.of(
            value = 120,
            timestamp = Instant.parse("2026-07-27T10:45:00Z"),
            trend = GlucoseTrend.FLAT,
            history = history
        )

        val tiles = buildDashboardMetricTiles(reading = reading, targetLow = 80, targetHigh = 180)

        assertEquals(5, tiles.size)
        assertEquals(listOf("Poniżej", "Zakres", "Powyżej", "GMI", "Czujnik"), tiles.map { it.label })
    }

    @Test
    fun readingTimeline_appendsCurrentReadingOnceAndSorts() {
        val reading = GlucoseReading.of(
            value = 130,
            timestamp = Instant.parse("2026-07-27T10:45:00Z"),
            trend = GlucoseTrend.FLAT,
            history = listOf(
                GlucoseHistoryPoint(120, Instant.parse("2026-07-27T10:00:00Z"), GlucoseTrend.FLAT),
                GlucoseHistoryPoint(125, Instant.parse("2026-07-27T10:15:00Z"), GlucoseTrend.FLAT),
                GlucoseHistoryPoint(130, Instant.parse("2026-07-27T10:45:00Z"), GlucoseTrend.FLAT)
            )
        )

        val timeline = readingTimeline(reading)

        assertEquals(3, timeline.size)
        assertEquals(Instant.parse("2026-07-27T10:00:00Z"), timeline.first().timestamp)
        assertEquals(Instant.parse("2026-07-27T10:45:00Z"), timeline.last().timestamp)
        assertEquals(130, timeline.last().value)
    }

    @Test
    fun keyDashboardLabels_arePolish() {
        assertEquals("W zakresie", glucoseRangeStatus(120, 80, 180))
        assertEquals("Poniżej zakresu", glucoseRangeStatus(70, 80, 180))
        assertEquals("Powyżej zakresu", glucoseRangeStatus(200, 80, 180))
    }

    @Test
    fun trendRateEstimate_forRisingTrend_returnsConsistentWindowedSlope() {
        val reading = GlucoseReading.of(
            value = 140,
            timestamp = Instant.parse("2026-07-27T10:15:00Z"),
            trend = GlucoseTrend.RISING,
            history = listOf(
                GlucoseHistoryPoint(120, Instant.parse("2026-07-27T10:00:00Z"), GlucoseTrend.FLAT)
            )
        )

        val snapshot = trendWindowSnapshot(reading, requestedWindowMinutes = 20)
        val estimate = estimateTrendRate(reading, requestedWindowMinutes = 20)

        assertTrue(snapshot != null)
        assertTrue(estimate != null)
        assertEquals(2, snapshot?.points?.size)
        assertEquals(15.0, snapshot?.spanMinutes ?: 0.0, 0.0001)
        assertEquals(20, estimate?.mgDlPer15Minutes?.roundToInt())
        assertEquals(2, estimate?.sampleCount)
        assertEquals(GlucoseTrend.RISING, estimate?.derivedTrend)
    }

    @Test
    fun formatTrendRatePerMinute_roundsToOneDecimalWithSign() {
        assertEquals("+3.1 mg/dL/min", formatTrendRatePerMinute(3.14))
        assertEquals("−2.6 mg/dL/min", formatTrendRatePerMinute(-2.64))
    }

    @Test
    fun buildTrendProjection_usesNextRelevantRisingThreshold() {
        val reading = GlucoseReading.of(
            value = 170,
            timestamp = Instant.parse("2026-07-27T10:15:00Z"),
            trend = GlucoseTrend.RISING_FAST,
            history = listOf(
                GlucoseHistoryPoint(125, Instant.parse("2026-07-27T10:00:00Z"), GlucoseTrend.RISING)
            )
        )

        val projection = buildTrendProjection(
            reading = reading,
            trendWindowMinutes = 20,
            thresholds = TrendProjectionThresholds(80, 180),
            now = Instant.parse("2026-07-27T10:16:00Z")
        )

        assertEquals(180, projection?.thresholdMgDl)
        assertEquals(2, projection?.minutesToThreshold)
    }

    @Test
    fun buildTrendProjection_usesNextRelevantFallingThreshold() {
        val reading = GlucoseReading.of(
            value = 90,
            timestamp = Instant.parse("2026-07-27T10:15:00Z"),
            trend = GlucoseTrend.FALLING_FAST,
            history = listOf(
                GlucoseHistoryPoint(135, Instant.parse("2026-07-27T10:00:00Z"), GlucoseTrend.FALLING)
            )
        )

        val projection = buildTrendProjection(
            reading = reading,
            trendWindowMinutes = 20,
            thresholds = TrendProjectionThresholds(70, 180),
            now = Instant.parse("2026-07-27T10:16:00Z")
        )

        assertEquals(70, projection?.thresholdMgDl)
        assertEquals(6, projection?.minutesToThreshold)
    }

    @Test
    fun buildTrendProjection_returnsNullWhenRemainingMinutesExpired() {
        val reading = GlucoseReading.of(
            value = 175,
            timestamp = Instant.parse("2026-07-27T10:15:00Z"),
            trend = GlucoseTrend.RISING_FAST,
            history = listOf(
                GlucoseHistoryPoint(130, Instant.parse("2026-07-27T10:00:00Z"), GlucoseTrend.RISING)
            )
        )

        val projection = buildTrendProjection(
            reading = reading,
            trendWindowMinutes = 20,
            thresholds = TrendProjectionThresholds(80, 180),
            now = Instant.parse("2026-07-27T10:18:00Z")
        )

        assertEquals(null, projection)
    }

    @Test
    fun trendWindowSnapshot_returnsNullForZeroMinuteSpan() {
        val timestamp = Instant.parse("2026-07-27T10:15:00Z")
        val reading = GlucoseReading.of(
            value = 140,
            timestamp = timestamp,
            trend = GlucoseTrend.RISING_FAST,
            history = listOf(
                GlucoseHistoryPoint(120, timestamp, GlucoseTrend.FLAT),
                GlucoseHistoryPoint(130, timestamp, GlucoseTrend.RISING)
            )
        )

        assertEquals(null, trendWindowSnapshot(reading, requestedWindowMinutes = 10))
        assertEquals(null, estimateTrendRate(reading, requestedWindowMinutes = 10))
    }
}

