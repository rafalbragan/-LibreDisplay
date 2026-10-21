package com.libredisplay.ui.monitoring

import com.libredisplay.data.api.LibreAuthenticationException
import com.libredisplay.data.api.LibreLinkUpHttpException
import com.libredisplay.data.api.MissingGlucoseMeasurementException
import com.libredisplay.data.api.NonRetryableLibreLinkUpException
import com.libredisplay.data.model.LibreConnectionPerson
import com.libredisplay.data.repository.SelectedPersonGraphException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class MonitoringFailureTest {
    private fun graph(cause: Throwable) = SelectedPersonGraphException(
        LibreConnectionPerson(patientId = "synthetic-person", displayName = "Osoba testowa"), cause
    )

    @Test fun authenticationRetainsOriginalChainAndHttpMetadata() {
        for (code in listOf(401, 403)) {
            val http = LibreLinkUpHttpException(code)
            val wrapped = graph(http)
            val outer = IllegalStateException("wrapper", wrapped)
            val result = MonitoringFailure.classify(outer)
            assertEquals(MonitoringFailureKind.AUTHENTICATION, result.kind)
            assertSame(outer, result.original)
            assertSame(http, result.http)
            assertSame(wrapped, result.graph)
            assertTrue(result.accountWide)
        }
    }

    @Test fun rateLimitRetainsRetryAfter() {
        for (code in listOf(429, 430)) {
            val result = MonitoringFailure.classify(graph(LibreLinkUpHttpException(code, retryAfterSeconds = 73)))
            assertEquals(MonitoringFailureKind.RATE_LIMIT, result.kind)
            assertEquals(73, result.http?.retryAfterSeconds)
            assertTrue(result.accountWide)
        }
    }

    @Test fun wrappedServerIsNotNoData() {
        for (code in listOf(500, 503)) {
            assertEquals(MonitoringFailureKind.SERVER, MonitoringFailure.classify(graph(LibreLinkUpHttpException(code))).kind)
        }
    }

    @Test fun wrappedIoIsNotNoData() {
        assertEquals(MonitoringFailureKind.NETWORK, MonitoringFailure.classify(graph(IOException())).kind)
    }

    @Test fun onlyTypedMissingMeasurementWithKnownPersonIsNoData() {
        assertEquals(MonitoringFailureKind.MISSING_MEASUREMENT, MonitoringFailure.classify(graph(MissingGlucoseMeasurementException())).kind)
        assertEquals(MonitoringFailureKind.UNEXPECTED, MonitoringFailure.classify(MissingGlucoseMeasurementException()).kind)
        assertEquals(MonitoringFailureKind.UNEXPECTED, MonitoringFailure.classify(graph(NonRetryableLibreLinkUpException("unexpected"))).kind)
    }

    @Test fun explicitAuthenticationIsNotAParsingFailure() {
        assertEquals(MonitoringFailureKind.AUTHENTICATION, MonitoringFailure.classify(LibreAuthenticationException("synthetic rejection")).kind)
    }

    @Test fun wrappedCancellationIsRethrownUnchanged() {
        val cancellation = CancellationException("synthetic cancellation")
        try {
            MonitoringFailure.classify(graph(cancellation))
            fail("Cancellation was swallowed")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    @Test fun cyclicCausesDoNotLoop() {
        val first = Exception("first")
        val second = Exception("second", first)
        first.initCause(second)
        assertEquals(MonitoringFailureKind.UNEXPECTED, MonitoringFailure.classify(first).kind)
    }
}
