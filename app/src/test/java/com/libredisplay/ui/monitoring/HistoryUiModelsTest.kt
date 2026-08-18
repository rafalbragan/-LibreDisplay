package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HistoryUiModelsTest {

    @Test
    fun trendPresentation_returnsPolishLabelAndArrow() {
        val rising = trendPresentation(GlucoseTrend.RISING)
        val fallingFast = trendPresentation(GlucoseTrend.FALLING_FAST)

        assertEquals("Rośnie", rising.label)
        assertEquals("↗", rising.arrow)
        assertEquals("Szybko spada", fallingFast.label)
        assertEquals("↓", fallingFast.arrow)
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
            rangeLabel = "Ostatnie 24 godz.",
            targetLow = 70,
            targetHigh = 180
        )

        assertTrue(section.title.contains("Statystyki - Ostatnie 24 godz."))
        assertEquals(listOf("Średnia", "GMI", "CV", "Czas w zakresie"), section.cards.map { it.label })
        assertTrue(section.cards.none { it.value.isBlank() })
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

