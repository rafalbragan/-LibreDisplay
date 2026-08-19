package com.libredisplay.ui.monitoring

import org.junit.Assert.assertEquals
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
            gmiValue = 7.2,
            hba1cValue = null
        )

        assertEquals(listOf("Poniżej", "W zakresie", "Powyżej", "GMI", "HbA1c"), tiles.map { it.label })
        assertEquals("0m", tiles[0].primaryValue)
        assertEquals("50%", tiles[1].secondaryValue)
        assertEquals("1g 15m", tiles[1].primaryValue)
    }
}

