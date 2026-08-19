package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.Instant

class DataCoverageModelTest {

    private val now = Instant.parse("2026-08-19T12:00:00Z")

    @Test
    fun computeDataCoverage_emptyHistory_returnsNoData() {
        val coverage = computeDataCoverage(
            history = emptyList(),
            selectedRange = Duration.ofHours(24),
            selectedRangeLabel = "24 godz."
        )

        assertNull(coverage.oldestAvailableTimestamp)
        assertNull(coverage.newestAvailableTimestamp)
        assertTrue(coverage.availableSpan.isZero)
        assertFalse(coverage.hasFullCoverage)
        assertNull(coverage.timeUntilFullCoverage)
    }

    @Test
    fun computeDataCoverage_partialData_hasNoFullCoverage() {
        // 8 hours of data, requested 24h
        val history = historySpanning(
            from = now.minus(Duration.ofHours(8)),
            to = now,
            step = Duration.ofMinutes(15)
        )
        val coverage = computeDataCoverage(
            history = history,
            selectedRange = Duration.ofHours(24),
            selectedRangeLabel = "24 godz."
        )

        assertFalse(coverage.hasFullCoverage)
        // Available span should be approximately 8h
        val spanHours = coverage.availableSpan.toHours()
        assertTrue("Expected ~8h span, got $spanHours", spanHours in 7..9)
        // Time until full should be approximately 16h
        assertNotNull(coverage.timeUntilFullCoverage)
        val untilHours = coverage.timeUntilFullCoverage!!.toHours()
        assertTrue("Expected ~16h until full, got $untilHours", untilHours in 14..17)
    }

    @Test
    fun computeDataCoverage_fullData_hasFullCoverage() {
        // 25 hours of data, requested 24h
        val history = historySpanning(
            from = now.minus(Duration.ofHours(25)),
            to = now,
            step = Duration.ofMinutes(15)
        )
        val coverage = computeDataCoverage(
            history = history,
            selectedRange = Duration.ofHours(24),
            selectedRangeLabel = "24 godz."
        )

        assertTrue(coverage.hasFullCoverage)
        assertNull(coverage.timeUntilFullCoverage)
        assertNull(coverage.fullCoverageEstimate)
    }

    @Test
    fun sectionHeaderLabel_fullCoverage_returnsSelectedRangeLabel() {
        val coverage = DataCoverageModel(
            selectedRange = Duration.ofHours(24),
            selectedRangeLabel = "24 godz.",
            oldestAvailableTimestamp = now.minus(Duration.ofHours(25)),
            newestAvailableTimestamp = now,
            availableSpan = Duration.ofHours(25),
            hasFullCoverage = true,
            timeUntilFullCoverage = null
        )

        assertEquals("24 godz.", coverage.sectionHeaderLabel)
        assertNull(coverage.selectedRangeNote)
        assertNull(coverage.fullCoverageEstimate)
    }

    @Test
    fun sectionHeaderLabel_partialCoverage_returnsAvailableSpan() {
        val coverage = DataCoverageModel(
            selectedRange = Duration.ofHours(24),
            selectedRangeLabel = "24 godz.",
            oldestAvailableTimestamp = now.minus(Duration.ofHours(8)),
            newestAvailableTimestamp = now,
            availableSpan = Duration.ofHours(8).plusMinutes(2),
            hasFullCoverage = false,
            timeUntilFullCoverage = Duration.ofHours(15).plusMinutes(58)
        )

        assertTrue(
            "sectionHeaderLabel should contain 'dostępnych danych', got: ${coverage.sectionHeaderLabel}",
            coverage.sectionHeaderLabel.contains("dostępnych danych")
        )
        assertNotNull(coverage.selectedRangeNote)
        assertTrue(coverage.selectedRangeNote!!.contains("24 godz."))
        assertNotNull(coverage.fullCoverageEstimate)
        assertTrue(coverage.fullCoverageEstimate!!.contains("24 godz."))
    }

    @Test
    fun formatNaturalDuration_formatsCorrectly() {
        assertEquals("chwilę temu", PolishDateTimeFormatter.formatNaturalDuration(null))
        assertEquals("chwilę temu", PolishDateTimeFormatter.formatNaturalDuration(Duration.ZERO))
        assertEquals("15 min", PolishDateTimeFormatter.formatNaturalDuration(Duration.ofMinutes(15)))
        assertEquals("1 godz. 15 min", PolishDateTimeFormatter.formatNaturalDuration(Duration.ofMinutes(75)))
        assertEquals("8 godz. 02 min", PolishDateTimeFormatter.formatNaturalDuration(Duration.ofHours(8).plusMinutes(2)))
        assertEquals("24 godz.", PolishDateTimeFormatter.formatNaturalDuration(Duration.ofHours(24)))
        assertEquals("7 dni", PolishDateTimeFormatter.formatNaturalDuration(Duration.ofDays(7)))
        assertEquals("1 godz.", PolishDateTimeFormatter.formatNaturalDuration(Duration.ofHours(1)))
    }

    @Test
    fun timeRangeToSelectedRangeLabel_allValues() {
        assertEquals("3 godz.", TimeRange.LAST_3_HOURS.toSelectedRangeLabel())
        assertEquals("24 godz.", TimeRange.LAST_24_HOURS.toSelectedRangeLabel())
        assertEquals("7 dni", TimeRange.LAST_7_DAYS.toSelectedRangeLabel())
        assertEquals("rok", TimeRange.LAST_365_DAYS.toSelectedRangeLabel())
    }

    // Helper: create history points spanning a given period
    private fun historySpanning(
        from: Instant,
        to: Instant,
        step: Duration
    ): List<GlucoseHistoryPoint> {
        val points = mutableListOf<GlucoseHistoryPoint>()
        var t = from
        while (!t.isAfter(to)) {
            points.add(GlucoseHistoryPoint(value = 120, timestamp = t, trend = GlucoseTrend.FLAT))
            t = t.plus(step)
        }
        return points
    }
}

