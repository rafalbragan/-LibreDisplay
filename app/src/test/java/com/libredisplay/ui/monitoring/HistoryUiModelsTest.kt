package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.ui.theme.LibreCareColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HistoryUiModelsTest {

    @Test
    fun trendPresentation_returnsPolishLabelAndArrow() {
        val rising = trendPresentation(GlucoseTrend.RISING)
        val flat = trendPresentation(GlucoseTrend.FLAT)
        val fallingFast = trendPresentation(GlucoseTrend.FALLING_FAST)

        assertEquals("Rośnie", rising.label)
        assertEquals("↗", rising.arrow)
        assertEquals("Bez zmian", flat.label)
        assertEquals("Szybko spada", fallingFast.label)
        assertEquals("↓", fallingFast.arrow)
    }

    @Test
    fun trendPresentation_allSupportedStates_haveVisiblePolishLabels() {
        val labels = GlucoseTrend.entries.associateWith { trendPresentation(it).label }

        assertEquals("Szybko rośnie", labels[GlucoseTrend.RISING_FAST])
        assertEquals("Rośnie", labels[GlucoseTrend.RISING])
        assertEquals("Bez zmian", labels[GlucoseTrend.FLAT])
        assertEquals("Spada", labels[GlucoseTrend.FALLING])
        assertEquals("Szybko spada", labels[GlucoseTrend.FALLING_FAST])
        assertEquals("Nieznany", labels[GlucoseTrend.UNKNOWN])
    }

    @Test
    fun trendPresentation_highGlucose_fallingTrendIsGreen() {
        val trend = trendPresentation(
            trend = GlucoseTrend.FALLING,
            glucoseValue = 240,
            targetLow = 70,
            targetHigh = 180
        )

        assertEquals(LibreCareColors.AccentGreen, trend.color)
    }

    @Test
    fun trendPresentation_highGlucose_risingFastIsRed() {
        val trend = trendPresentation(
            trend = GlucoseTrend.RISING_FAST,
            glucoseValue = 240,
            targetLow = 70,
            targetHigh = 180
        )

        assertEquals(warningToneColor(WarningTone.CRITICAL), trend.color)
    }

    @Test
    fun historyLegendRows_returnReadablePolishRows() {
        val rows = historyLegendRows(sampleHistory(), targetLow = 70, targetHigh = 180)

        assertEquals(5, rows.size)
        assertEquals("Bardzo wysoka", rows.first().label)
        assertEquals("W zakresie", rows[2].label)
        assertTrue(rows.all { it.threshold.contains("mg/dL") })
        assertTrue(rows.all { it.durationLabel.isNotBlank() })
    }

    @Test
    fun historyLegendRows_withNoData_returnsBrakDanych() {
        val rows = historyLegendRows(emptyList(), targetLow = 70, targetHigh = 180)

        assertTrue(rows.all { it.durationLabel == "brak danych" })
        assertTrue(rows.all { !it.hasData })
    }

    @Test
    fun historyStatsSection_returnsCompactCards() {
        val section = historyStatsSection(
            history = sampleHistory(),
            rangeLabel = "24 godz.",
            targetLow = 70,
            targetHigh = 180
        )

        // When coverage is not provided (null), title uses rangeLabel
        assertTrue(section.title.contains("Statystyki · 24 godz."))
        assertEquals(listOf("GMI", "CV", "Czas w zakresie"), section.cards.map { it.label })
        assertTrue(section.cards.none { it.value.isBlank() })
    }

    @Test
    fun historyStatsSection_withPartialCoverage_usesAvailableSpanInTitle() {
        val history = sampleHistory() // spans 1 hour
        val coverage = computeDataCoverage(
            history = history,
            selectedRange = java.time.Duration.ofHours(24),
            selectedRangeLabel = "24 godz.",
            now = history.maxOf { it.timestamp }
        )
        val section = historyStatsSection(
            history = history,
            rangeLabel = "24 godz.",
            targetLow = 70,
            targetHigh = 180,
            coverage = coverage
        )

        // Should show actual data span, not selected range
        assertTrue("Title should contain 'danych', got: ${section.title}", section.title.contains("danych"))
        assertTrue("Title should NOT contain '24 godz.' as primary label, got: ${section.title}",
            !section.title.startsWith("Statystyki · 24 godz."))
    }

    @Test
    fun historyStatsSection_withFullCoverage_usesSelectedRangeLabel() {
        val history = sampleHistory() // spans 1 hour
        val coverage = computeDataCoverage(
            history = history,
            selectedRange = java.time.Duration.ofMinutes(30), // less than 1h
            selectedRangeLabel = "30 min",
            now = history.maxOf { it.timestamp }
        )
        val section = historyStatsSection(
            history = history,
            rangeLabel = "30 min",
            targetLow = 70,
            targetHigh = 180,
            coverage = coverage
        )

        // Full coverage → show selected range label
        assertTrue("Title should use selected range label, got: ${section.title}",
            section.title.contains("30 min"))
    }

    @Test
    fun placeholderHistoryEvents_arePolishAndPresent() {
        val items = placeholderHistoryEvents()

        assertFalse(items.isEmpty())
        assertTrue(items.any { it.category == "Wysoka glikemia" })
        assertTrue(items.any { it.category == "Lekarz" })
        assertTrue(items.any { it.category == "Posiłek" })
    }

    private fun sampleHistory(): List<GlucoseHistoryPoint> {
        val start = Instant.parse("2026-08-18T10:00:00Z")
        return listOf(
            GlucoseHistoryPoint(52, start, GlucoseTrend.FLAT),
            GlucoseHistoryPoint(60, start.plusSeconds(900), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(120, start.plusSeconds(1800), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(220, start.plusSeconds(2700), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(280, start.plusSeconds(3600), GlucoseTrend.FLAT)
        )
    }
}

