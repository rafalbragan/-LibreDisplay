package com.libredisplay.ui.monitoring

enum class PollingFailureType {
    AUTHENTICATION_REQUIRED,
    TRANSIENT_NETWORK,
    SERVER_UNAVAILABLE,
    RESPONSE_DECODING,
    UNKNOWN
}

