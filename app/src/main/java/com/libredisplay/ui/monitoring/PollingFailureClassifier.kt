package com.libredisplay.ui.monitoring

import com.libredisplay.data.api.LibreLinkUpHttpException
import com.libredisplay.data.api.LibreResponseDecodingException
import com.libredisplay.data.api.NonRetryableLibreLinkUpException

object PollingFailureClassifier {
    fun classify(throwable: Throwable): PollingFailureType {
        return when (throwable) {
            is NonRetryableLibreLinkUpException -> PollingFailureType.AUTHENTICATION_REQUIRED
            is LibreResponseDecodingException -> PollingFailureType.RESPONSE_DECODING
            is java.net.UnknownHostException,
            is java.net.SocketTimeoutException,
            is java.io.IOException -> PollingFailureType.TRANSIENT_NETWORK
            is LibreLinkUpHttpException -> when {
                throwable.statusCode in setOf(401, 403) -> PollingFailureType.AUTHENTICATION_REQUIRED
                throwable.statusCode >= 500 || throwable.statusCode == 429 || throwable.statusCode == 430 -> PollingFailureType.SERVER_UNAVAILABLE
                else -> PollingFailureType.UNKNOWN
            }
            else -> PollingFailureType.UNKNOWN
        }
    }
}

