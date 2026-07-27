package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import java.time.Instant

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState
    data object AuthenticationRequired : ConnectionState
    data class Cooldown(val remainingSeconds: Long) : ConnectionState
    data class AuthenticationRejected(
        val apiStatus: Int?,
        val serverMessage: String?,
        val localCooldownUntil: Instant?
    ) : ConnectionState
    data class ResponseDecodingFailure(
        val encoding: String?,
        val contentType: String?,
        val message: String
    ) : ConnectionState
    data class Locked(val retryAt: Instant?) : ConnectionState
    data class NetworkFailure(val message: String) : ConnectionState
    data class UnknownFailure(val message: String) : ConnectionState
}

data class MonitoringUiState(
    val settings: AppSettings = AppSettings(),
    val isConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val reading: GlucoseReading? = null,
    val currentGlucose: String = "",
    val currentTimestamp: String = "",
    val minutesAgo: Int? = null,
    val trend: GlucoseTrend? = null,
    val min12h: String? = null,
    val max12h: String? = null,
    val historyPointCount: Int = 0,
    val historyStatus: HistoryStatus = HistoryStatus.Loading,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
    val retryCooldownSecondsRemaining: Long = 0,
    val isPolling: Boolean = false,
    val lastUpdatedAt: Instant? = null,
    val connectionState: ConnectionState = ConnectionState.Idle
)

sealed interface HistoryStatus {
    data object Loading : HistoryStatus
    data object Available : HistoryStatus
    data object Empty : HistoryStatus
    data class Error(val message: String) : HistoryStatus
}

