package com.libredisplay.ui.monitoring

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.api.LibreLinkUpHttpException
import com.libredisplay.data.api.LibreResponseDecodingException
import com.libredisplay.data.api.NonRetryableLibreLinkUpException
import com.libredisplay.data.repository.CredentialsSnapshot
import com.libredisplay.diagnostics.DiagnosticLogger
import com.libredisplay.diagnostics.DiagnosticStatus
import com.libredisplay.service.RefreshController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class MonitoringViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LibreDisplayApp
    private val settingsRepository = app.settingsRepository
    private val glucoseRepository = app.glucoseRepository
    private val connectMutex = Mutex()
    private val pollMutex = Mutex()
    private val attemptCounter = AtomicLong(0)
    private val backoffPolicy = PollingBackoffPolicy()
    private val connectivityProvider = AndroidConnectivityStatusProvider(application.applicationContext)

    private val _uiState = MutableStateFlow(
        MonitoringUiState(
            settings = settingsRepository.loadSettings(),
            isConfigured = settingsRepository.isConfigured()
        )
    )
    val uiState: StateFlow<MonitoringUiState> = _uiState.asStateFlow()

    private var refreshController = RefreshController(intervalMs = settingsRepository.loadSettings().refreshInterval * 1000L)
    private var pollingJob: Job? = null
    private var retryJob: Job? = null
    private var cooldownJob: Job? = null
    private var lastRefreshNonce: Int? = null
    private var networkAvailable: Boolean = true
    private var failureStartAt: Instant? = null

    init {
        connectivityProvider.start { available ->
            networkAvailable = available
            if (!available) {
                _uiState.update {
                    it.copy(
                        dataConnectionState = DataConnectionState.Offline(it.lastSuccessfulFetchAt),
                        staleInfoMessage = staleMessage(it.lastSuccessfulFetchAt),
                        isDataStale = true
                    )
                }
            } else if (_uiState.value.connectionState == ConnectionState.Connected) {
                viewModelScope.launch {
                    delay(3_000L)
                    pollOnce(force = true, source = "network-recovered")
                }
            }
        }
    }

    fun onScreenVisible(refreshNonce: Int) {
        if (lastRefreshNonce == refreshNonce) return
        lastRefreshNonce = refreshNonce
        reloadSettings()
        bootstrapUsingPersistedTokenOnly()
    }

    private fun reloadSettings() {
        val settings = settingsRepository.loadSettings()
        val targetState = if (settings.isConfigured()) ConnectionState.Disconnected else ConnectionState.Idle
        _uiState.update { current ->
            current.copy(
                settings = settings,
                isConfigured = settings.isConfigured(),
                errorMessage = null,
                canRetry = settings.isConfigured(),
                retryCooldownSecondsRemaining = authCooldownSeconds(),
                isPolling = false,
                historyStatus = if (current.reading == null) HistoryStatus.Loading else current.historyStatus,
                connectionState = targetState
            )
        }
        stopPollingInternal("settings reload")
        refreshController = RefreshController(intervalMs = settings.refreshInterval.coerceIn(30, 300) * 1000L)
    }

    private fun bootstrapUsingPersistedTokenOnly() {
        val settings = settingsRepository.loadSettings()
        if (!settings.isConfigured()) {
            transitionState(ConnectionState.Idle)
            return
        }
        viewModelScope.launch {
            runCatching {
                glucoseRepository.fetchLatestReadingFromPersistedSessionOrNull()
            }.onSuccess { readingOrNull ->
                if (readingOrNull == null) {
                    transitionState(ConnectionState.Disconnected)
                    _uiState.update { it.copy(canRetry = true, errorMessage = "Kliknij \"Polacz z LibreLinkUp\", aby wykonac pojedyncza probe logowania.") }
                    return@onSuccess
                }
                transitionState(ConnectionState.Connected)
                _uiState.update { it.withReading(readingOrNull).copy(
                    isLoading = false,
                    errorMessage = null,
                    canRetry = false,
                    lastUpdatedAt = Instant.now(),
                    retryCooldownSecondsRemaining = authCooldownSeconds(),
                    authenticationState = AuthenticationState.Authenticated,
                    dataConnectionState = DataConnectionState.Live,
                    pollingStatus = PollingStatus.Active,
                    lastSuccessfulFetchAt = Instant.now(),
                    lastMeasurementTimestamp = readingOrNull.timestamp,
                    isDataStale = false,
                    consecutivePollingFailures = 0,
                    nextPollingRetryAt = null,
                    staleInfoMessage = null
                ) }
                startPolling()
            }.onFailure { throwable ->
                glucoseRepository.resetSession()
                if (throwable is LibreLinkUpHttpException && throwable.statusCode in setOf(401, 403)) {
                    transitionState(ConnectionState.AuthenticationRequired)
                    _uiState.update {
                        it.copy(
                            errorMessage = "Zapisany token zostal odrzucony. Kliknij \"Polacz z LibreLinkUp\".",
                            canRetry = true,
                            retryCooldownSecondsRemaining = authCooldownSeconds(),
                            authenticationState = AuthenticationState.AuthenticationRequired,
                            pollingStatus = PollingStatus.AuthenticationRequired("Sesja wymaga recznego ponownego polaczenia.")
                        )
                    }
                } else {
                    transitionState(ConnectionState.Disconnected)
                    _uiState.update { it.copy(canRetry = true) }
                }
            }
        }
    }

    fun refreshNow() {
        if (_uiState.value.connectionState != ConnectionState.Connected) return
        viewModelScope.launch { pollOnce(force = true, source = "manual-refresh") }
    }

    fun retryAfterError() {
        connectManually(trigger = "manual retry")
    }

    fun connectManually(trigger: String = "manual connect") {
        val cooldown = authCooldownSeconds()
        if (cooldown > 0) {
            transitionState(ConnectionState.Cooldown(cooldown))
            _uiState.update {
                it.copy(
                errorMessage = "Kolejna probe mozna wykonac za ${formatRemaining(cooldown)}",
                retryCooldownSecondsRemaining = cooldown,
                canRetry = false
                )
            }
            startCooldownCountdown()
            return
        }
        DiagnosticLogger.logInfo("MonitoringViewModel", "USER CLICKED RETRY")
        val settings = settingsRepository.loadSettings()
        val snapshot = CredentialsSnapshot.fromSettings(settings)
        if (!snapshot.isConfigured) {
            transitionState(ConnectionState.Disconnected)
            _uiState.update {
                it.copy(errorMessage = "Wpisz email i haslo, potem kliknij Polacz z LibreLinkUp.")
            }
            return
        }
        runManualConnect(snapshot = snapshot, trigger = trigger)
    }

    fun stopPolling() {
        stopPollingInternal("manual stop")
    }

    private fun runManualConnect(snapshot: CredentialsSnapshot, trigger: String) {
        viewModelScope.launch {
            connectMutex.withLock {
                if (_uiState.value.connectionState is ConnectionState.Connecting) {
                    DiagnosticLogger.logInfo("MonitoringViewModel", "LOGIN SINGLE-FLIGHT WAITING")
                    return@withLock
                }

                transitionState(ConnectionState.Connecting)
                val attemptId = attemptCounter.incrementAndGet()
                DiagnosticLogger.logInfo("MonitoringViewModel", "LOGIN ATTEMPT START attemptId=$attemptId")
                DiagnosticLogger.logInfo("MonitoringViewModel", "Connection trigger=$trigger")

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    canRetry = false,
                    retryCooldownSecondsRemaining = authCooldownSeconds(),
                    isPolling = false
                )

                runCatching {
                    glucoseRepository.fetchLatestReadingWithSnapshot(snapshot)
                }.onSuccess { reading ->
                    DiagnosticLogger.logInfo("MonitoringViewModel", "LOGIN ATTEMPT END attemptId=$attemptId result=success")
                    transitionState(ConnectionState.Connected)
                    _uiState.update { it.withReading(reading).copy(
                        isLoading = false,
                        errorMessage = null,
                        canRetry = false,
                        lastUpdatedAt = Instant.now(),
                        authenticationState = AuthenticationState.Authenticated,
                        dataConnectionState = DataConnectionState.Live,
                        pollingStatus = PollingStatus.Active,
                        lastSuccessfulFetchAt = Instant.now(),
                        lastMeasurementTimestamp = reading.timestamp,
                        isDataStale = false,
                        consecutivePollingFailures = 0,
                        nextPollingRetryAt = null,
                        staleInfoMessage = null
                    ) }
                    startPolling()
                }.onFailure { throwable ->
                    if (throwable is CancellationException) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        return@withLock
                    }
                    DiagnosticLogger.logException("MonitoringViewModel", throwable, "Connection attempt failed")
                    DiagnosticLogger.logInfo("MonitoringViewModel", "LOGIN ATTEMPT END attemptId=$attemptId result=failure")
                    stopPollingInternal("manual retry required")
                    val cooldown = authCooldownSeconds()
                    val nextState = classifyLoginFailure(throwable)
                    transitionState(nextState)
                    _uiState.update {
                        it.copy(
                        isLoading = false,
                        errorMessage = humanReadableMessage(throwable, cooldown),
                        canRetry = cooldown <= 0,
                        retryCooldownSecondsRemaining = cooldown,
                        authenticationState = AuthenticationState.AuthenticationRequired,
                        pollingStatus = PollingStatus.AuthenticationRequired("Wymagane reczne ponowne polaczenie."),
                        isPolling = false
                        )
                    }
                    DiagnosticLogger.logWarning("MonitoringViewModel", "RETRY BLOCKED reason=manual retry required")
                    DiagnosticLogger.logWarning("MonitoringViewModel", "AUTO RETRY DISABLED")
                    startCooldownCountdown()
                }
            }
        }
    }

    private fun startPolling() {
        if (_uiState.value.connectionState != ConnectionState.Connected) return
        pollingJob?.cancel()
        retryJob?.cancel()
        refreshController.resume()
        DiagnosticLogger.logInfo("MonitoringViewModel", "POLLING START")
        DiagnosticStatus.setPolling(true, "co ${_uiState.value.settings.refreshInterval}s")
        _uiState.value = _uiState.value.copy(isPolling = true)
        pollingJob = viewModelScope.launch {
            refreshController.ticks().collectLatest {
                if (_uiState.value.connectionState == ConnectionState.Connected) {
                    pollOnce(force = false, source = "interval")
                }
            }
        }
    }

    private suspend fun pollOnce(force: Boolean, source: String) {
        if (_uiState.value.connectionState != ConnectionState.Connected) return
        if (!pollMutex.tryLock()) return
        try {
            val now = Instant.now()
            val nextRetryAt = _uiState.value.nextPollingRetryAt
            if (!force && nextRetryAt != null && now.isBefore(nextRetryAt)) {
                return
            }

            if (!networkAvailable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isDataStale = true,
                        dataConnectionState = DataConnectionState.Offline(it.lastSuccessfulFetchAt),
                        staleInfoMessage = staleMessage(it.lastSuccessfulFetchAt)
                    )
                }
                return
            }

            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching {
                glucoseRepository.fetchLatestReadingFromActiveSession()
            }.onSuccess { reading ->
                val previousFailures = _uiState.value.consecutivePollingFailures
                val downtime = failureStartAt?.let { Duration.between(it, Instant.now()).seconds.coerceAtLeast(0) } ?: 0L
                if (previousFailures > 0) {
                    DiagnosticLogger.logInfo(
                        "MonitoringViewModel",
                        "POLLING RECOVERED previousFailureCount=$previousFailures downtimeSeconds=$downtime"
                    )
                }
                failureStartAt = null
                retryJob?.cancel()
                _uiState.value = _uiState.value.withReading(reading).copy(
                    isLoading = false,
                    errorMessage = null,
                    canRetry = false,
                    lastUpdatedAt = Instant.now(),
                    isPolling = true,
                    authenticationState = AuthenticationState.Authenticated,
                    dataConnectionState = DataConnectionState.Live,
                    pollingStatus = PollingStatus.Active,
                    lastSuccessfulFetchAt = Instant.now(),
                    lastMeasurementTimestamp = reading.timestamp,
                    isDataStale = false,
                    consecutivePollingFailures = 0,
                    nextPollingRetryAt = null,
                    staleInfoMessage = null
                )
            }.onFailure { throwable ->
                if (throwable is CancellationException) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@onFailure
                }
                DiagnosticLogger.logException("MonitoringViewModel", throwable, "Polling failed source=$source")
                when (PollingFailureClassifier.classify(throwable)) {
                    PollingFailureType.AUTHENTICATION_REQUIRED -> {
                        transitionState(ConnectionState.AuthenticationRequired)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                canRetry = true,
                                errorMessage = "Sesja wygasla lub zostala odrzucona. Kliknij \"Polacz z LibreLinkUp\".",
                                authenticationState = AuthenticationState.AuthenticationRequired,
                                pollingStatus = PollingStatus.AuthenticationRequired("Wymagane reczne ponowne polaczenie."),
                                isDataStale = true,
                                staleInfoMessage = staleMessage(it.lastSuccessfulFetchAt)
                            )
                        }
                        stopPollingInternal("authentication required")
                    }

                    PollingFailureType.TRANSIENT_NETWORK,
                    PollingFailureType.SERVER_UNAVAILABLE,
                    PollingFailureType.RESPONSE_DECODING,
                    PollingFailureType.UNKNOWN -> {
                        handleTransientPollingFailure(throwable)
                    }
                }
            }
        } finally {
            if (pollMutex.isLocked) pollMutex.unlock()
        }
    }

    private fun handleTransientPollingFailure(throwable: Throwable) {
        val failures = (_uiState.value.consecutivePollingFailures + 1).coerceAtLeast(1)
        if (failureStartAt == null) failureStartAt = Instant.now()
        val retryAfter = (throwable as? LibreLinkUpHttpException)?.retryAfterSeconds?.toLong()
        val delaySeconds = backoffPolicy.nextDelaySeconds(failureCount = failures, retryAfterSeconds = retryAfter)
        val retryAt = Instant.now().plusSeconds(delaySeconds)
        val failureType = when (throwable) {
            is java.net.UnknownHostException -> "DNS"
            else -> throwable::class.java.simpleName
        }

        DiagnosticLogger.logWarning(
            "MonitoringViewModel",
            "POLLING TEMPORARY FAILURE type=$failureType exception=${throwable::class.java.simpleName} consecutiveFailures=$failures lastSuccessfulFetchAt=${_uiState.value.lastSuccessfulFetchAt ?: "n/a"} nextRetryInSeconds=$delaySeconds tokenPreserved=true historyPreserved=true automaticRelogin=false"
        )

        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                canRetry = false,
                retryCooldownSecondsRemaining = 0,
                authenticationState = AuthenticationState.Authenticated,
                dataConnectionState = it.lastSuccessfulFetchAt?.let { last -> DataConnectionState.Stale(last, failures) }
                    ?: DataConnectionState.Offline(null),
                pollingStatus = when (PollingFailureClassifier.classify(throwable)) {
                    PollingFailureType.SERVER_UNAVAILABLE -> PollingStatus.ServerUnavailable(retryAt)
                    else -> PollingStatus.TemporarilyOffline(failures, retryAt)
                },
                isDataStale = true,
                consecutivePollingFailures = failures,
                nextPollingRetryAt = retryAt,
                staleInfoMessage = "Chwilowy brak polaczenia z LibreLinkUp. Wyswietlane sa ostatnie poprawne dane. Ponowna proba nastapi automatycznie."
            )
        }

        schedulePollingRetry(delaySeconds)
    }

    private fun schedulePollingRetry(delaySeconds: Long) {
        retryJob?.cancel()
        retryJob = viewModelScope.launch {
            delay(delaySeconds * 1000L)
            if (_uiState.value.connectionState == ConnectionState.Connected) {
                pollOnce(force = true, source = "backoff-retry")
            }
        }
    }

    private fun transitionState(next: ConnectionState) {
        _uiState.update { current ->
            if (current.connectionState == next) return@update current
            DiagnosticLogger.logInfo("MonitoringViewModel", "CONNECTION STATE: ${current.connectionState} -> $next")
            current.copy(connectionState = next)
        }
    }

    private fun stopPollingInternal(reason: String) {
        val wasPolling = pollingJob != null || _uiState.value.isPolling
        pollingJob?.cancel()
        pollingJob = null
        retryJob?.cancel()
        retryJob = null
        refreshController.stop()
        if (wasPolling) {
            DiagnosticLogger.logInfo("MonitoringViewModel", "POLLING STOP reason=$reason")
            DiagnosticStatus.setPolling(false, reason)
        }
        _uiState.update { it.copy(isPolling = false) }
    }

    private fun humanReadableMessage(throwable: Throwable, cooldownSeconds: Long): String {
        return when (throwable) {
            is NonRetryableLibreLinkUpException -> {
                val msg = throwable.message.orEmpty().lowercase()
                if (msg.contains("incorrect username/password") || msg.contains("odrzucil logowanie")) {
                    buildString {
                        appendLine("Serwer odrzucil logowanie przed utworzeniem tokena. Nie jest to jednoznaczne potwierdzenie blednego hasla ani serwerowej blokady.")
                        appendLine()
                        appendLine("Email i haslo moga byc poprawne. Przyczyna moze byc:")
                        appendLine("- lokalna lub serwerowa blokada po wczesniejszych probach,")
                        appendLine("- regresja w sposobie budowania requestu,")
                        appendLine("- odrzucenie nieoficjalnego klienta,")
                        appendLine("- niezaakceptowane warunki LibreLinkUp,")
                        appendLine("- niewlasciwa wersja lub zestaw naglowkow.")
                        appendLine()
                        appendLine("Nie wykonano kolejnej proby.")
                        if (cooldownSeconds > 0) {
                            appendLine("Aplikacja wstrzymala kolejna probe na ${formatRemaining(cooldownSeconds)}, aby ograniczyc ryzyko blokady. Serwer nie potwierdzil, ze konto jest zablokowane.")
                        }
                        append("Jezeli oficjalna aplikacja LibreLinkUp rowniez nie pozwala sie zalogowac, uzyj funkcji \"Nie pamietam hasla\" lub skontaktuj sie z pomoca Abbott.")
                    }
                } else {
                    throwable.message ?: "LibreLinkUp odrzucil logowanie. Kolejna proba nie zostanie wykonana automatycznie."
                }
            }
            is LibreResponseDecodingException -> {
                throwable.message
                    ?: "Odpowiedz LibreLinkUp jest skompresowana GZIP, ale nie zostala rozpakowana przez klienta HTTP."
            }
            is LibreLinkUpHttpException -> {
                val lockoutInfo = throwable.lockoutInfo
                val serverLockout = maxOf(lockoutInfo?.lockoutSeconds ?: 0, throwable.retryAfterSeconds ?: 0)
                val lockedByMessage = lockoutInfo?.message?.contains("locked", ignoreCase = true) == true ||
                    lockoutInfo?.message?.contains("temporarily banned", ignoreCase = true) == true
                if (throwable.statusCode in setOf(429, 430) || lockoutInfo?.apiStatus == 429 || lockoutInfo?.apiCode == 60 || lockedByMessage) {
                    if (serverLockout > 0) {
                        "Serwer LibreLinkUp zablokowal kolejne logowania. Nieudane proby: ${lockoutInfo?.failures ?: "?"}, interval: ${lockoutInfo?.intervalSeconds ?: "?"} s, minimalny lockout: $serverLockout s. Aplikacja nie bedzie probowala logowac sie automatycznie."
                    } else {
                        "LibreLinkUp zablokowal logowanie, ale nie podal czasu odblokowania. Nie wykonuj kolejnych prob. Sprobuj pozniej w oficjalnej aplikacji LibreLinkUp lub uzyj resetu hasla."
                    }
                } else {
                    "LibreLinkUp zwrocil HTTP ${throwable.statusCode}. Uzyj przycisku Ponow probe."
                }
            }
            else -> if (throwable.containsMalformedJsonException()) {
                "Odpowiedz LibreLinkUp nie mogla zostac zdekodowana. Sprawdz konfiguracje klienta HTTP i kompresji."
            } else {
                throwable.message ?: "Nie udalo sie pobrac danych glukozy."
            }
        }
    }

    private fun authCooldownSeconds(): Long = app.authRepository.cooldownRemainingSeconds()

    private fun classifyLoginFailure(throwable: Throwable): ConnectionState {
        return when (throwable) {
            is NonRetryableLibreLinkUpException -> ConnectionState.AuthenticationRejected(
                apiStatus = 2,
                serverMessage = throwable.message,
                localCooldownUntil = app.authRepository.nextAllowedLoginAtMillis()
                    .takeIf { it > 0L }
                    ?.let { Instant.ofEpochMilli(it) }
            )
            is LibreResponseDecodingException -> ConnectionState.ResponseDecodingFailure(
                encoding = throwable.encoding,
                contentType = throwable.contentType,
                message = throwable.message.orEmpty()
            )
            is LibreLinkUpHttpException -> when {
                throwable.statusCode in setOf(429, 430) || throwable.lockoutInfo?.apiStatus == 429 || throwable.lockoutInfo?.apiCode == 60 -> {
                    val retryAt = throwable.retryAfterSeconds?.let { Instant.now().plusSeconds(it.toLong()) }
                    ConnectionState.Locked(retryAt)
                }
                throwable.statusCode in setOf(401, 403) -> ConnectionState.AuthenticationRequired
                else -> ConnectionState.UnknownFailure("HTTP ${throwable.statusCode}")
            }
            is java.io.IOException -> ConnectionState.NetworkFailure(throwable.message.orEmpty())
            else -> if (throwable.containsMalformedJsonException()) {
                ConnectionState.ResponseDecodingFailure(
                    encoding = null,
                    contentType = null,
                    message = "Odpowiedz LibreLinkUp nie mogla zostac zdekodowana."
                )
            } else {
                ConnectionState.UnknownFailure(throwable.message.orEmpty())
            }
        }
    }

    private fun startCooldownCountdown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            while (true) {
                val remaining = authCooldownSeconds()
                _uiState.update { it.copy(retryCooldownSecondsRemaining = remaining, canRetry = remaining <= 0) }
                if (remaining <= 0) break
                delay(1000)
            }
        }
    }

    private fun formatRemaining(seconds: Long): String {
        val minutes = (seconds / 60).toInt()
        val secs = (seconds % 60).toInt()
        return "%02d:%02d".format(minutes, secs)
    }

    private fun Throwable.containsMalformedJsonException(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current::class.qualifiedName == "com.google.gson.stream.MalformedJsonException") {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun MonitoringUiState.withReading(reading: com.libredisplay.data.model.GlucoseReading): MonitoringUiState {
        val minutes = max(0, java.time.Duration.between(reading.timestamp, Instant.now()).toMinutes().toInt())
        val minText = reading.stats.min?.toString()
        val maxText = reading.stats.max?.toString()
        val pointCount = reading.stats.min?.let { reading.history.size } ?: 0
        val status = when {
            reading.stats.min != null && reading.stats.max != null && pointCount > 0 -> HistoryStatus.Available
            reading.history.isNotEmpty() -> HistoryStatus.Available
            else -> HistoryStatus.Empty
        }

        return copy(
            reading = reading,
            currentGlucose = reading.value.toString(),
            currentTimestamp = reading.timestamp.toString(),
            minutesAgo = minutes,
            trend = reading.trend,
            min12h = minText,
            max12h = maxText,
            historyPointCount = pointCount,
            historyStatus = status
        )
    }

    private fun staleMessage(lastSuccessfulFetchAt: Instant?): String {
        if (lastSuccessfulFetchAt == null) return "Brak polaczenia - brak ostatniej udanej aktualizacji"
        val minutes = Duration.between(lastSuccessfulFetchAt, Instant.now()).toMinutes().coerceAtLeast(0)
        return "Brak polaczenia - dane z przed $minutes min"
    }

    override fun onCleared() {
        connectivityProvider.stop()
        super.onCleared()
    }
}
