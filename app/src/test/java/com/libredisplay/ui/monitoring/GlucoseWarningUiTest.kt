package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GlucoseWarningUiTest {

    @Test
    fun valueInTargetRange_mapsToSafeStyle() {
        val warning = buildGlucoseStatusPresentation(readingAt(120)).primary

        assertEquals(GlucoseWarningLevel.IN_RANGE, warning.level)
        assertEquals(WarningTone.SAFE, warning.tone)
        assertEquals("Glikemia w zakresie.", warning.title)
    }

    @Test
    fun below70_mapsToLowWarning() {
        val warning = buildGlucoseStatusPresentation(readingAt(65)).primary

        assertEquals(GlucoseWarningLevel.LOW, warning.level)
        assertEquals("Niska glikemia", warning.title)
    }

    @Test
    fun below54_mapsToCriticalLowWarning() {
        val warning = buildGlucoseStatusPresentation(readingAt(50)).primary

        assertEquals(GlucoseWarningLevel.VERY_LOW, warning.level)
        assertEquals(WarningTone.CRITICAL, warning.tone)
    }

    @Test
    fun above200_mapsToHighWarning() {
        val warning = buildGlucoseStatusPresentation(readingAt(225)).primary

        assertEquals(GlucoseWarningLevel.HIGH, warning.level)
        assertEquals(WarningTone.WARNING, warning.tone)
        assertEquals("Wysoka glikemia", warning.title)
    }

    @Test
    fun above300_mapsToVeryHighWarning() {
        val warning = buildGlucoseStatusPresentation(readingAt(325)).primary

        assertEquals(GlucoseWarningLevel.VERY_HIGH, warning.level)
        assertEquals(WarningTone.URGENT, warning.tone)
    }

    @Test
    fun above400_mapsToCriticalHighWarning() {
        val warning = buildGlucoseStatusPresentation(readingAt(425)).primary

        assertEquals(GlucoseWarningLevel.EXTREME_HIGH, warning.level)
        assertEquals(WarningTone.CRITICAL, warning.tone)
    }

    @Test
    fun thresholdBoundaries_preserveExistingMedicalBuckets() {
        assertEquals(GlucoseWarningLevel.VERY_LOW, buildGlucoseStatusPresentation(readingAt(53)).primary.level)
        assertEquals(GlucoseWarningLevel.LOW, buildGlucoseStatusPresentation(readingAt(54)).primary.level)
        assertEquals(GlucoseWarningLevel.IN_RANGE, buildGlucoseStatusPresentation(readingAt(70)).primary.level)
        assertEquals(GlucoseWarningLevel.IN_RANGE, buildGlucoseStatusPresentation(readingAt(180)).primary.level)
        assertEquals(GlucoseWarningLevel.HIGH, buildGlucoseStatusPresentation(readingAt(181)).primary.level)
        assertEquals(GlucoseWarningLevel.HIGH, buildGlucoseStatusPresentation(readingAt(200)).primary.level)
        assertEquals(GlucoseWarningLevel.VERY_HIGH, buildGlucoseStatusPresentation(readingAt(301)).primary.level)
        assertEquals(GlucoseWarningLevel.EXTREME_HIGH, buildGlucoseStatusPresentation(readingAt(401)).primary.level)
    }

    @Test
    fun staleData_outranksNormalHigh() {
        val presentation = buildGlucoseStatusPresentation(
            reading = readingAt(225, timestamp = Instant.parse("2026-08-10T10:00:00Z")),
            now = Instant.parse("2026-08-18T10:00:00Z")
        )

        assertEquals(GlucoseWarningLevel.STALE_DATA, presentation.primary.level)
        assertTrue(presentation.secondary.any { it.level == GlucoseWarningLevel.HIGH })
    }

    @Test
    fun staleData_doesNotHideCriticalLow() {
        val presentation = buildGlucoseStatusPresentation(
            reading = readingAt(50, timestamp = Instant.parse("2026-08-10T10:00:00Z")),
            now = Instant.parse("2026-08-18T10:00:00Z")
        )

        assertEquals(GlucoseWarningLevel.VERY_LOW, presentation.primary.level)
        assertEquals(GlucoseWarningLevel.STALE_DATA, presentation.freshnessWarning?.level)
        assertTrue(presentation.secondary.any { it.level == GlucoseWarningLevel.STALE_DATA })
    }

    @Test
    fun veryLow_outranksAllNonStaleStatusesWhenCurrent() {
        val primary = buildGlucoseStatusPresentation(readingAt(40)).primary

        assertEquals(GlucoseWarningLevel.VERY_LOW, primary.level)
    }

    @Test
    fun noSkullOrDeathIconIsUsed() {
        val icons = listOf(
            buildGlucoseStatusPresentation(readingAt(120)).primary.iconName,
            buildGlucoseStatusPresentation(readingAt(50)).primary.iconName,
            buildGlucoseStatusPresentation(readingAt(425)).primary.iconName
        )

        assertFalse(icons.any { it.contains("skull", ignoreCase = true) || it.contains("death", ignoreCase = true) })
    }

    @Test
    fun warningTextsArePolish() {
        val warnings = listOf(
            buildGlucoseStatusPresentation(readingAt(120)).primary,
            buildGlucoseStatusPresentation(readingAt(65)).primary,
            buildGlucoseStatusPresentation(readingAt(325)).primary
        )

        assertTrue(warnings.all { it.message.contains("glikemia", ignoreCase = true) || it.title.contains("Dane nieaktualne") })
    }

    private fun readingAt(value: Int, timestamp: Instant = Instant.now()): GlucoseReading {
        return GlucoseReading.of(
            value = value,
            timestamp = timestamp,
            trend = GlucoseTrend.FLAT,
            history = emptyList()
        )
    }
}

