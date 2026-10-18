package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ChartTimestampPrecisionRegressionTest {
    private val start = Instant.parse("2026-09-01T12:00:00.123Z")
    private fun point(at: Instant, value: Int = 231) = GlucoseHistoryPoint(value, at, GlucoseTrend.FLAT)

    @Test fun apiAndRoomCopies_haveFiniteHitCoordinates() {
        val match = findNearestHistoryPointMatch(
            listOf(point(start), point(start.plusNanos(456_789))),
            canvasWidth = 400f, canvasHeight = 240f, touchX = 54f
        )
        assertNotNull(match)
        assertTrue(match!!.distancePx.isFinite())
        assertEquals(0f, match.distancePx, 0f)
    }

    @Test fun oneMillisecondDomain_keepsDistinctEndpoints() {
        val end = start.plusMillis(1)
        val last = point(end, 235)
        val match = findNearestHistoryPointMatch(
            listOf(point(start), last),
            canvasWidth = 400f, canvasHeight = 240f, touchX = 380f,
            domainStartMillis = start.toEpochMilli(), domainEndMillis = end.toEpochMilli()
        )!!
        assertEquals(last, match.point)
        assertEquals(0f, match.distancePx, 0f)
    }

    @Test fun regularDomain_preservesNearestPoint() {
        val middle = point(start.plusSeconds(1800), 147)
        val match = findNearestHistoryPointMatch(
            listOf(point(start), middle, point(start.plusSeconds(3600))),
            canvasWidth = 400f, canvasHeight = 240f, touchX = 217f
        )!!
        assertEquals(middle, match.point)
        assertTrue(match.distancePx.isFinite())
    }
}

