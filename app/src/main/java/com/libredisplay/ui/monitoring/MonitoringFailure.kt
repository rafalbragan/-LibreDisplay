package com.libredisplay.ui.monitoring

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.libredisplay.data.api.LibreAuthenticationException
import com.libredisplay.data.api.LibreLinkUpHttpException
import com.libredisplay.data.api.LibreResponseDecodingException
import com.libredisplay.data.api.MissingGlucoseMeasurementException
import com.libredisplay.data.repository.SelectedPersonGraphException
import kotlinx.coroutines.CancellationException
import java.io.IOException

internal enum class MonitoringFailureKind {
    AUTHENTICATION, RATE_LIMIT, SERVER, NETWORK, MISSING_MEASUREMENT, DECODING, UNEXPECTED
}

internal data class MonitoringFailure(
    val kind: MonitoringFailureKind,
    val original: Throwable,
    val http: LibreLinkUpHttpException?,
    val graph: SelectedPersonGraphException?
) {
    val accountWide: Boolean
        get() = kind == MonitoringFailureKind.AUTHENTICATION || kind == MonitoringFailureKind.RATE_LIMIT

    companion object {
        fun classify(error: Throwable): MonitoringFailure {
            val chain = mutableListOf<Throwable>()
            val seen = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Throwable, Boolean>())
            var current: Throwable? = error
            while (current != null && seen.add(current)) {
                chain += current
                current = current.cause
            }
            chain.filterIsInstance<CancellationException>().firstOrNull()?.let { throw it }
            val http = chain.filterIsInstance<LibreLinkUpHttpException>().firstOrNull()
            val graph = chain.filterIsInstance<SelectedPersonGraphException>().firstOrNull()
            val kind = when {
                http?.statusCode in setOf(401, 403) || chain.any { it is LibreAuthenticationException } -> MonitoringFailureKind.AUTHENTICATION
                http?.statusCode in setOf(429, 430) || http?.lockoutInfo?.apiStatus == 429 ||
                    http?.lockoutInfo?.apiCode == 60 -> MonitoringFailureKind.RATE_LIMIT
                http != null && http.statusCode in 500..599 -> MonitoringFailureKind.SERVER
                http != null -> MonitoringFailureKind.UNEXPECTED
                chain.any { it is LibreResponseDecodingException || it is JsonParseException || it is MalformedJsonException } -> MonitoringFailureKind.DECODING
                chain.any { it is IOException } -> MonitoringFailureKind.NETWORK
                graph != null && chain.any { it is MissingGlucoseMeasurementException } -> MonitoringFailureKind.MISSING_MEASUREMENT
                else -> MonitoringFailureKind.UNEXPECTED
            }
            return MonitoringFailure(kind, error, http, graph)
        }
    }
}
