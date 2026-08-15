package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.data.model.LibreConnectionPerson
import java.time.Instant
import java.time.LocalDate

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
    data class Locked(val retryAt: Instant?, val retryAfterSeconds: Int = 0) : ConnectionState
    data class NetworkFailure(val message: String) : ConnectionState
    data class UnknownFailure(val message: String) : ConnectionState
}

sealed interface AuthenticationState {
    data object Authenticated : AuthenticationState
    data object AuthenticationRequired : AuthenticationState
}

sealed interface DataConnectionState {
    data object Live : DataConnectionState
    data class Stale(val lastSuccessAt: Instant, val consecutiveFailures: Int) : DataConnectionState
    data class Offline(val lastSuccessAt: Instant?) : DataConnectionState
}

sealed interface PollingStatus {
    data object Active : PollingStatus
    data class AuthenticationRequired(val message: String) : PollingStatus
    data class ServerUnavailable(val retryAt: Instant) : PollingStatus
    data class TemporarilyOffline(val failures: Int, val retryAt: Instant) : PollingStatus
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
    val connectionState: ConnectionState = ConnectionState.Idle,
    val authenticationState: AuthenticationState? = null,
    val dataConnectionState: DataConnectionState? = null,
    val pollingStatus: PollingStatus? = null,
    val isDataStale: Boolean = false,
    val consecutivePollingFailures: Int = 0,
    val nextPollingRetryAt: Instant? = null,
    val staleInfoMessage: String? = null,
    val lastSuccessfulFetchAt: Instant? = null,
    val lastMeasurementTimestamp: Instant? = null,
    val availablePersons: List<LibreConnectionPerson> = emptyList(),
    val selectedPatientId: String? = null,
    val selectedPersonName: String? = null,
    val labHbA1cPercent: Double? = null,
    val labHbA1cDate: LocalDate? = null,
    val targetHbA1cPercent: Double = 7.5
)

sealed interface HistoryStatus {
    data object Loading : HistoryStatus
    data object Available : HistoryStatus
    data object Empty : HistoryStatus
    data class Error(val message: String) : HistoryStatus
}
