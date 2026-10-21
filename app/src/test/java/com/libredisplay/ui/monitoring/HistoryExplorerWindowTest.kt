package com.libredisplay.ui.monitoring

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class HistoryExplorerWindowTest {
    private val start = Instant.parse("2020-05-01T00:00:00Z").toEpochMilli()
    private val end = Instant.parse("2026-10-19T00:00:00Z").toEpochMilli()
    private val full = ExplorerWindow(start, end)
    @Test fun pinchActuallyChangesDurationAndPreservesAnchor() {
        val window = ExplorerWindow(end - 86_400_000L, end)
        val zoomed = window.transform(2f, 0f, 0.5f, full)
        assertEquals(window.duration / 2, zoomed.duration)
        assertEquals(window.at(0.5f), zoomed.at(0.5f))
    }
    @Test fun panMovesHorizontallyAndStaysInsidePersonRange() {
        val window = ExplorerWindow(end - 86_400_000L, end)
        val panned = window.transform(1f, 0.5f, 0.5f, full)
        assertTrue(panned.start < window.start)
        assertEquals(window.duration, panned.duration)
        assertEquals(full.start, window.transform(1f, 1e9f, 0.5f, full).start)
    }
    @Test fun invalidGestureNeverProducesNanCoordinates() {
        for (zoom in listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -1f)) {
            assertEquals(full, full.transform(zoom, 0f, 0f, full))
        }
        assertEquals(full, full.transform(1f, Float.NaN, 0f, full))
    }
    @Test fun relativeEpochRetainsMinutePrecisionAcrossSixYears() {
        val short = ExplorerWindow(end - 60_000L, end)
        assertEquals(0f, short.fraction(end - 60_000L), 0f)
        assertEquals(0.5f, short.fraction(end - 30_000L), 0.00001f)
        assertEquals(1f, full.fraction(end), 0f)
    }
    @Test fun hugeGapsAndReversedPointsNeverGetConnected() {
        assertFalse(canJoinHistorySamples(start, end))
        assertFalse(canJoinHistorySamples(end, start))
        assertFalse(canJoinHistorySamples(start, start))
        assertTrue(canJoinHistorySamples(start, start + 15 * 60_000L))
        assertFalse(canJoinHistorySamples(start, start + 21 * 60_000L))
    }
    @Test fun zoomBoundsHandleSingleReadingSpan() {
        val tiny = ExplorerWindow(start, start + 60_000)
        assertEquals(tiny, tiny.transform(100f, 0f, 0.5f, tiny))
    }
}

