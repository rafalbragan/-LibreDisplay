package com.libredisplay.ui.monitoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class RedesignedMetricsTest {

    @Test
    fun formatDurationQuickly_formatsCompactMetricDurations() {
        assertEquals("—", formatDurationQuickly(null))
        assertEquals("0m", formatDurationQuickly(Duration.ZERO))
        assertEquals("15m", formatDurationQuickly(Duration.ofMinutes(15)))
        assertEquals("1g 5m", formatDurationQuickly(Duration.ofMinutes(65)))
        assertEquals("8g 2m", formatDurationQuickly(Duration.ofHours(8).plusMinutes(2)))
        assertEquals("1d 2g", formatDurationQuickly(Duration.ofHours(26)))
    }

    @Test
    fun buildQuickMetricTiles_preservesPolishOrderingForRangeMetrics() {
        val tiles = buildQuickMetricTiles(
            belowDuration = Duration.ofMinutes(0),
            belowPercent = 0,
            inRangeDuration = Duration.ofMinutes(75),
            inRangePercent = 50,
            aboveDuration = Duration.ofMinutes(75),
            abovePercent = 50,
            minValueMgDl = 70,
            maxValueMgDl = 220,
            gmiValue = 7.2,
            averageValueMgDl = 124,
            veryLowEpisodes = 2,
            veryHighEpisodes = 3
        )

        assertEquals(listOf("Poniżej", "W zakresie", "Powyżej", "Średnia", "Minimum", "Maksimum", "GMI", "Epizody bardzo niskie", "Epizody bardzo wysokie"), tiles.map { it.label })
        assertEquals("0m", tiles[0].primaryValue)
        assertEquals("50%", tiles[1].secondaryValue)
        assertEquals("1g 15m", tiles[1].primaryValue)
    }

    @Test
    fun quickMetricsRows_smallWidth_usesThreePlusTwoLayout() {
        val rows = quickMetricsRows(maxWidthDp = 384f, orderedTiles = sampleTiles())

        assertEquals(2, rows.size)
        assertEquals(listOf("Poniżej", "W zakresie", "Powyżej"), rows.first().map { it.label })
        assertEquals(listOf("Średnia", "Minimum"), rows.last().map { it.label })
    }

    @Test
    fun quickMetricsRows_largeWidth_keepsSingleRow() {
        val rows = quickMetricsRows(maxWidthDp = 600f, orderedTiles = sampleTiles())

        assertEquals(1, rows.size)
        assertEquals(9, rows.single().size)
    }

    @Test
    fun buildQuickMetricTiles_usesReadableFallbackTexts() {
        val tiles = buildQuickMetricTiles(
            belowDuration = Duration.ofMinutes(59),
            belowPercent = 1,
            inRangeDuration = Duration.ofHours(12).plusMinutes(45),
            inRangePercent = 56,
            aboveDuration = Duration.ofHours(9).plusMinutes(45),
            abovePercent = 43,
            minValueMgDl = null,
            maxValueMgDl = null,
            gmiValue = null,
            averageValueMgDl = null,
            veryLowEpisodes = null,
            veryHighEpisodes = null
        )

        assertEquals("Za mało danych", tiles.first { it.label == "GMI" }.secondaryValue)
        assertTrue(tiles.first { it.label == "W zakresie" }.primaryValue.contains("12g 45m"))
    }

    private fun sampleTiles() = buildQuickMetricTiles(
        belowDuration = Duration.ofMinutes(15),
        belowPercent = 1,
        inRangeDuration = Duration.ofHours(12).plusMinutes(45),
        inRangePercent = 56,
        aboveDuration = Duration.ofHours(9).plusMinutes(45),
        abovePercent = 43,
        minValueMgDl = null,
        maxValueMgDl = null,
        gmiValue = null,
        averageValueMgDl = null,
        veryLowEpisodes = null,
        veryHighEpisodes = null
    )
}

