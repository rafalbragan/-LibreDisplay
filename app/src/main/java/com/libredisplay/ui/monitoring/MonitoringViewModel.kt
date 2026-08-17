package com.libredisplay.ui.monitoring

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.api.LibreLinkUpHttpException
import com.libredisplay.data.api.LibreResponseDecodingException
import com.libredisplay.data.api.NonRetryableLibreLinkUpException
import com.libredisplay.data.model.MonitoringSnapshot
import com.libredisplay.data.repository.CredentialsSnapshot
import com.libredisplay.diagnostics.DiagnosticLogger
import com.libredisplay.diagnostics.DiagnosticStatus
import com.libredisplay.service.RefreshController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
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
import kotlin.math.ceil
import kotlin.math.max
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class MonitoringViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LibreDisplayApp
    private val settingsRepository = app.settingsRepository
    private val glucoseRepository = app.glucoseRepository
    private val glucoseSyncRepository = app.glucoseSyncRepository
    private val connectMutex = Mutex()
    private val fetchMutex = Mutex()
    private val attemptCounter = AtomicLong(0)
    private val backoffPolicy = PollingBackoffPolicy()
    private val connectivityProvider = AndroidConnectivityStatusProvider(application.applicationContext)
    private val viewModelExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        DiagnosticLogger.logException("MonitoringViewModel", throwable, "Unhandled coroutine failure")
    }

    private val _uiState = MutableStateFlow(
        MonitoringUiState(
            settings = settingsRepository.loadSettings(),
            isConfigured = settingsRepository.isConfigured(),
            isDemoMode = settingsRepository.loadSettings().useMock
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
    private var rateLimitUntilEpochMillis: Long = settingsRepository.loadRateLimitUntilEpochMillis()

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
        reconcileRateLimitState()
        bootstrapUsingPersistedTokenOnly()
    }

    private fun reloadSettings() {
        val settings = settingsRepository.loadSettings()
        val hba1cSettings = settingsRepository.loadHbA1cSettings(settings.selectedPatientId)
        val targetState = if (settings.isConfigured()) ConnectionState.Disconnected else ConnectionState.Idle
        _uiState.update { current ->
            current.copy(
                settings = settings,
                isConfigured = settings.isConfigured(),
                isDemoMode = settings.useMock,
                errorMessage = null,
                canRetry = settings.isConfigured(),
                retryCooldownSecondsRemaining = authCooldownSeconds(),
                isPolling = false,
                historyStatus = if (current.reading == null) HistoryStatus.Loading else current.historyStatus,
                connectionState = targetState,
                labHbA1cPercent = hba1cSettings.labHbA1cPercent,
                labHbA1cDate = hba1cSettings.labHbA1cDate,
                targetHbA1cPercent = hba1cSettings.targetHbA1cPercent
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
            val localSnapshot = glucoseRepository.loadLatestMonitoringSnapshotFromLocal(settings.selectedPatientId)
            if (localSnapshot != null) {
                transitionState(ConnectionState.Connected)
                _uiState.update {
                    it.applyDashboardSnapshot(localSnapshot).copy(
                        isLoading = false,
                        errorMessage = null,
                        canRetry = false,
                        dataConnectionState = DataConnectionState.Stale(localSnapshot.reading.timestamp, 0),
                        pollingStatus = PollingStatus.Active,
                        lastSuccessfulFetchAt = localSnapshot.reading.timestamp,
                        lastMeasurementTimestamp = localSnapshot.reading.timestamp,
                        isDataStale = true,
                        staleInfoMessage = "Dane z lokalnej historii. Ostatnia synchronizacja: ${localSnapshot.reading.timestamp}"
                    )
                }
            }

            runCatching {
                glucoseRepository.fetchMonitoringSnapshotFromPersistedSessionOrNull()
            }.onSuccess { snapshotOrNull ->
                if (snapshotOrNull == null) {
                    transitionState(ConnectionState.Disconnected)
                    _uiState.update { it.copy(canRetry = true, errorMessage = "Kliknij \"Polacz z LibreLinkUp\", aby wykonac pojedyncza probe logowania.") }
                    return@onSuccess
                }
                transitionState(ConnectionState.Connected)
                _uiState.update {
                    it.applyDashboardSnapshot(snapshotOrNull).copy(
                    isLoading = false,
                    errorMessage = null,
                    canRetry = false,
                    lastUpdatedAt = Instant.now(),
                    retryCooldownSecondsRemaining = authCooldownSeconds(),
                    authenticationState = AuthenticationState.Authenticated,
                    dataConnectionState = DataConnectionState.Live,
                    pollingStatus = PollingStatus.Active,
                    lastSuccessfulFetchAt = Instant.now(),
                    lastMeasurementTimestamp = snapshotOrNull.reading.timestamp,
                    isDataStale = false,
                    consecutivePollingFailures = 0,
                    nextPollingRetryAt = null,
                    staleInfoMessage = null
                    )
                }
                startPolling()
                viewModelScope.launch(viewModelExceptionHandler) {
                    glucoseSyncRepository.syncAllPersons(com.libredisplay.data.repository.SyncReason.APP_START)
                }
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
        viewModelScope.launch(viewModelExceptionHandler) {
            pollOnce(force = true, source = "manual-refresh")
        }
    }

    fun retryAfterError() {
        connectManually(trigger = "manual retry")
    }

    fun connectManually(trigger: String = "manual connect") {
        DiagnosticLogger.logInfo("MonitoringViewModel", "USER CLICKED RETRY")
        val lockedRemaining = activeRateLimitRemainingSeconds()
        if (lockedRemaining > 0) {
            DiagnosticLogger.logWarning("RATE_LIMIT", "Rate limit detected")
            if (rateLimitUntilEpochMillis > 0L) {
                DiagnosticLogger.logInfo(
                    "RATE_LIMIT",
                    "Retry allowed at timestamp: ${Instant.ofEpochMilli(rateLimitUntilEpochMillis)}"
                )
            }
            DiagnosticLogger.logInfo("RATE_LIMIT", "Remaining seconds: $lockedRemaining")
            DiagnosticLogger.logWarning(
                "MonitoringViewModel",
                "RETRY BLOCKED reason=rate limit active remainingSeconds=$lockedRemaining"
            )
            _uiState.update {
                it.copy(
                    canRetry = false,
                    retryCooldownSecondsRemaining = max(authCooldownSeconds(), lockedRemaining),
                    errorMessage = rateLimitMessage(lockedRemaining)
                )
            }
            startCooldownCountdown()
            return
        }

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

    fun onPersonSelected(patientId: String) {
        val normalized = patientId.trim()
        if (normalized.isBlank()) return
        val previous = _uiState.value.selectedPatientId
        if (previous == normalized) return
        val selectedPerson = _uiState.value.availablePersons.firstOrNull { it.patientId == normalized }
        val selectedDisplayName = selectedPerson?.displayName ?: _uiState.value.selectedPersonName.orEmpty()
        DiagnosticLogger.logInfo("PERSON", "Switched selected person to: $selectedDisplayName / $normalized")
        DiagnosticLogger.logInfo(
            "MonitoringViewModel",
            "PERSON SWITCH old=${previous?.take(6) ?: "none"} new=${normalized.take(6)}"
        )
        settingsRepository.saveSelectedPatientId(normalized)
        _uiState.update { current ->
            val selectedName = current.availablePersons.firstOrNull { it.patientId == normalized }?.displayName
            val selectedCurrent = current.availablePersons.firstOrNull { it.patientId == normalized }
            current.copy(
                selectedPatientId = normalized,
                selectedPersonFirstName = selectedCurrent?.firstName,
                selectedPersonLastName = selectedCurrent?.lastName,
                selectedPersonFullName = selectedName ?: selectedDisplayName.ifBlank { current.selectedPersonFullName },
                selectedPersonName = selectedName ?: selectedDisplayName.ifBlank { current.selectedPersonName },
                isLoading = true,
                historyStatus = HistoryStatus.Loading
            )
        }
        viewModelScope.launch(viewModelExceptionHandler) {
            glucoseRepository.loadLatestMonitoringSnapshotFromLocal(normalized)?.let { localSnapshot ->
                _uiState.update {
                    it.applyDashboardSnapshot(localSnapshot).copy(
                        isLoading = false,
                        isDataStale = true,
                        staleInfoMessage = "Dane z lokalnej historii. Trwa odswiezanie dla wybranej osoby."
                    )
                }
            }
            if (_uiState.value.connectionState == ConnectionState.Connected) {
                pollOnce(force = true, source = "person-switch")
            }
        }
    }

    fun stopPolling() {
        stopPollingInternal("manual stop")
    }

    private fun runManualConnect(snapshot: CredentialsSnapshot, trigger: String) {
        viewModelScope.launch(viewModelExceptionHandler) {
            if (!connectMutex.tryLock()) {
                DiagnosticLogger.logInfo("MonitoringViewModel", "LOGIN SINGLE-FLIGHT WAITING")
                return@launch
            }
            DiagnosticLogger.logInfo("MonitoringViewModel", "LOGIN SINGLE-FLIGHT ENTER")
            try {
                val priorState = _uiState.value.connectionState

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
                DiagnosticLogger.logInfo("MonitoringViewModel", "FETCH START reason=$trigger")

                runCatching {
                    performSingleManualFetch(snapshot = snapshot, trigger = trigger, priorState = priorState)
                }.onSuccess { monitoringSnapshot ->
                    DiagnosticLogger.logInfo("MonitoringViewModel", "LOGIN ATTEMPT END attemptId=$attemptId result=success")
                    transitionState(ConnectionState.Connected)
                    _uiState.update {
                        it.applyDashboardSnapshot(monitoringSnapshot).copy(
                        isLoading = false,
                        errorMessage = null,
                        canRetry = false,
                        lastUpdatedAt = Instant.now(),
                        authenticationState = AuthenticationState.Authenticated,
                        dataConnectionState = DataConnectionState.Live,
                        pollingStatus = PollingStatus.Active,
                        lastSuccessfulFetchAt = Instant.now(),
                        lastMeasurementTimestamp = monitoringSnapshot.reading.timestamp,
                        isDataStale = false,
                        consecutivePollingFailures = 0,
                        nextPollingRetryAt = null,
                        staleInfoMessage = null
                        )
                    }
                    startPolling()
                }.onFailure { throwable ->
                    if (throwable is CancellationException) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        return@onFailure
                    }

                    if (throwable is FetchInProgressException) {
                        _uiState.update { it.copy(isLoading = false) }
                        return@onFailure
                    }

                    if (throwable.isRateLimit()) {
                        DiagnosticLogger.logException("MonitoringViewModel", throwable, "Connection attempt failed (rate limited)")
                        DiagnosticLogger.logInfo("MonitoringViewModel", "LOGIN ATTEMPT END attemptId=$attemptId result=failure")
                        handleRateLimitFailure(throwable)
                        return@onFailure
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
                        canRetry = cooldown <= 0 && activeRateLimitRemainingSeconds() <= 0,
                        retryCooldownSecondsRemaining = max(cooldown, activeRateLimitRemainingSeconds()),
                        authenticationState = AuthenticationState.AuthenticationRequired,
                        pollingStatus = PollingStatus.AuthenticationRequired("Wymagane reczne ponowne polaczenie."),
                        isPolling = false
                        )
                    }
                    DiagnosticLogger.logWarning("MonitoringViewModel", "RETRY BLOCKED reason=manual retry required")
                    DiagnosticLogger.logWarning("MonitoringViewModel", "AUTO RETRY DISABLED")
                    startCooldownCountdown()
                }
            } finally {
                DiagnosticLogger.logInfo("MonitoringViewModel", "LOGIN SINGLE-FLIGHT EXIT")
                DiagnosticLogger.logInfo("MonitoringViewModel", "LOGIN SINGLE-FLIGHT CLEAR")
                if (connectMutex.isLocked) connectMutex.unlock()
            }
        }
    }

    private fun startPolling() {
        if (_uiState.value.connectionState != ConnectionState.Connected) return
        pollingJob?.cancel()
        retryJob?.cancel()
        refreshController.resume()
        DiagnosticLogger.logInfo("MonitoringViewModel", "POLLING START firstTickDelayed=true")
        DiagnosticStatus.setPolling(true, "co ${_uiState.value.settings.refreshInterval}s")
        _uiState.value = _uiState.value.copy(isPolling = true)
        pollingJob = viewModelScope.launch(viewModelExceptionHandler) {
            refreshController.ticks().collectLatest {
                if (_uiState.value.connectionState == ConnectionState.Connected) {
                    pollOnce(force = false, source = "interval")
                }
            }
        }
    }

    private suspend fun pollOnce(force: Boolean, source: String) {
        if (_uiState.value.connectionState != ConnectionState.Connected) return

        val lockedRemaining = activeRateLimitRemainingSeconds()
        if (!force && lockedRemaining > 0) {
            DiagnosticLogger.logWarning(
                "MonitoringViewModel",
                "POLLING SKIPPED reason=rate limit active remainingSeconds=$lockedRemaining"
            )
            return
        }

        if (!fetchMutex.tryLock()) {
            DiagnosticLogger.logWarning("MonitoringViewModel", "FETCH SKIPPED reason=already running")
            DiagnosticLogger.logWarning("MonitoringViewModel", "POLLING SKIPPED reason=fetch already in progress")
            return
        }

        DiagnosticLogger.logInfo("MonitoringViewModel", "FETCH SINGLE-FLIGHT ENTER")
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
            val selectedId = _uiState.value.selectedPatientId
            val selectedName = _uiState.value.selectedPersonName.orEmpty()
            DiagnosticLogger.logInfo("DATA", "Refreshing glucose data for person: $selectedName / ${selectedId ?: "unknown"}")
            DiagnosticLogger.logInfo("MonitoringViewModel", "FETCH START reason=$source")
            runCatching {
                glucoseRepository.fetchMonitoringSnapshot(preferredPatientId = selectedId)
            }.onSuccess { monitoringSnapshot ->
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
                _uiState.value = _uiState.value.applyDashboardSnapshot(monitoringSnapshot).copy(
                    isLoading = false,
                    errorMessage = null,
                    canRetry = false,
                    lastUpdatedAt = Instant.now(),
                    isPolling = true,
                    authenticationState = AuthenticationState.Authenticated,
                    dataConnectionState = DataConnectionState.Live,
                    pollingStatus = PollingStatus.Active,
                    lastSuccessfulFetchAt = Instant.now(),
                    lastMeasurementTimestamp = monitoringSnapshot.reading.timestamp,
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
                if (throwable.isRateLimit()) {
                    handleRateLimitFailure(throwable)
                    return@onFailure
                }
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
            DiagnosticLogger.logInfo("MonitoringViewModel", "FETCH SINGLE-FLIGHT EXIT")
            DiagnosticLogger.logInfo("MonitoringViewModel", "FETCH SINGLE-FLIGHT CLEAR")
            if (fetchMutex.isLocked) fetchMutex.unlock()
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
        retryJob = viewModelScope.launch(viewModelExceptionHandler) {
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
                    val retryAfterSeconds = (throwable.retryAfterSeconds ?: 30).coerceAtLeast(1)
                    val retryAt = Instant.now().plusSeconds(retryAfterSeconds.toLong())
                    ConnectionState.Locked(retryAt = retryAt, retryAfterSeconds = retryAfterSeconds)
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
        cooldownJob = viewModelScope.launch(viewModelExceptionHandler) {
            while (true) {
                val authRemaining = authCooldownSeconds()
                val rateLimitRemaining = activeRateLimitRemainingSeconds()
                if (rateLimitRemaining <= 0L) {
                    clearRateLimitUntilEpochMillis()
                }
                val remaining = max(authRemaining, rateLimitRemaining)
                _uiState.update { current ->
                    val rateLimitedMessage = if (rateLimitRemaining > 0) {
                        rateLimitMessage(rateLimitRemaining)
                    } else if (current.errorMessage.isRateLimitMessage()) {
                        null
                    } else {
                        current.errorMessage
                    }
                    current.copy(
                        retryCooldownSecondsRemaining = remaining,
                        canRetry = remaining <= 0 && current.isConfigured,
                        errorMessage = rateLimitedMessage
                    )
                }
                DiagnosticLogger.logInfo("RATE_LIMIT", "Remaining seconds: $rateLimitRemaining")
                if (remaining <= 0) break
                delay(1000)
            }
        }
    }

    private suspend fun performSingleManualFetch(
        snapshot: CredentialsSnapshot,
        trigger: String,
        priorState: ConnectionState
    ): MonitoringSnapshot {
        if (!fetchMutex.tryLock()) {
            DiagnosticLogger.logWarning("MonitoringViewModel", "FETCH SKIPPED reason=already running")
            throw FetchInProgressException()
        }
        DiagnosticLogger.logInfo("MonitoringViewModel", "FETCH SINGLE-FLIGHT ENTER")
        try {
            val shouldTrySessionReuse = trigger.contains("retry", ignoreCase = true) || priorState is ConnectionState.Locked
            if (shouldTrySessionReuse) {
                val sessionReading = runCatching {
                    glucoseRepository.fetchMonitoringSnapshotFromPersistedSessionOrNull(
                        preferredPatientId = _uiState.value.selectedPatientId
                    )
                }.getOrElse { throwable ->
                    val statusCode = throwable.findHttpStatusCode()
                    if (statusCode in setOf(401, 403)) {
                        DiagnosticLogger.logInfo("MonitoringViewModel", "LOGIN REQUIRED reason=http$statusCode")
                        null
                    } else {
                        throw throwable
                    }
                }

                if (sessionReading != null) {
                    DiagnosticLogger.logInfo("MonitoringViewModel", "SESSION REUSED after rate limit")
                    return sessionReading
                }
            }

            return glucoseRepository.fetchMonitoringSnapshotWithSnapshot(
                snapshot = snapshot,
                preferredPatientId = _uiState.value.selectedPatientId
            )
        } finally {
            DiagnosticLogger.logInfo("MonitoringViewModel", "FETCH SINGLE-FLIGHT EXIT")
            DiagnosticLogger.logInfo("MonitoringViewModel", "FETCH SINGLE-FLIGHT CLEAR")
            if (fetchMutex.isLocked) fetchMutex.unlock()
        }
    }

    private fun MonitoringUiState.applyDashboardSnapshot(snapshot: MonitoringSnapshot): MonitoringUiState {
        val persons = snapshot.persons.take(3)
        DiagnosticLogger.logInfo("PERSON", "Available people loaded")
        DiagnosticLogger.logInfo("MonitoringViewModel", "PERSONS LOADED count=${persons.size}")
        val selected = persons.firstOrNull { it.patientId == snapshot.selectedPerson.patientId }
            ?: persons.firstOrNull()
            ?: snapshot.selectedPerson
        val hba1cSettings = settingsRepository.loadHbA1cSettings(selected.patientId)
        if (selected.patientId != selectedPatientId) {
            settingsRepository.saveSelectedPatientId(selected.patientId)
        }
        return withReading(snapshot.reading).copy(
            availablePersons = persons,
            selectedPatientId = selected.patientId,
            selectedPersonFirstName = selected.firstName,
            selectedPersonLastName = selected.lastName,
            selectedPersonFullName = selected.displayName,
            selectedPersonName = selected.displayName,
            labHbA1cPercent = hba1cSettings.labHbA1cPercent,
            labHbA1cDate = hba1cSettings.labHbA1cDate,
            targetHbA1cPercent = hba1cSettings.targetHbA1cPercent,
            historyStatus = if (snapshot.reading.history.isEmpty()) HistoryStatus.Empty else HistoryStatus.Available
        )
    }

    private fun handleRateLimitFailure(throwable: Throwable) {
        val httpException = throwable.findLibreHttpException()
        val retryAfterSeconds = (httpException?.retryAfterSeconds ?: 30).coerceAtLeast(1)
        val nowMillis = System.currentTimeMillis()
        val retryAtMillis = nowMillis + retryAfterSeconds * 1000L
        setRateLimitUntilEpochMillis(retryAtMillis)
        val retryAt = Instant.ofEpochMilli(retryAtMillis)
        DiagnosticLogger.logWarning("RATE_LIMIT", "Rate limit detected")
        DiagnosticLogger.logInfo("RATE_LIMIT", "Retry allowed at timestamp: $retryAt")
        transitionState(ConnectionState.Locked(retryAt = retryAt, retryAfterSeconds = retryAfterSeconds))
        stopPollingInternal("rate limited")
        DiagnosticLogger.logWarning("MonitoringViewModel", "AUTO RETRY DISABLED")

        val remaining = max(authCooldownSeconds(), activeRateLimitRemainingSeconds(nowMillis = nowMillis))
        _uiState.update {
            it.copy(
                isLoading = false,
                canRetry = false,
                retryCooldownSecondsRemaining = remaining,
                errorMessage = rateLimitMessage(remaining),
                pollingStatus = PollingStatus.ServerUnavailable(retryAt),
                nextPollingRetryAt = null
            )
        }
        startCooldownCountdown()
    }

    private fun activeRateLimitRemainingSeconds(nowMillis: Long = System.currentTimeMillis()): Long {
        val untilMillis = rateLimitUntilEpochMillis
        if (untilMillis <= 0L) return 0L
        val remainingMillis = untilMillis - nowMillis
        if (remainingMillis <= 0L) return 0L
        return ceil(remainingMillis / 1000.0).toLong().coerceAtLeast(0L)
    }

    private fun rateLimitMessage(remainingSeconds: Long): String {
        return "Too many login attempts. Try again in $remainingSeconds seconds."
    }

    private fun Throwable.isRateLimit(): Boolean {
        val http = findLibreHttpException() ?: return false
        return http.statusCode in setOf(429, 430)
    }

    private fun setRateLimitUntilEpochMillis(epochMillis: Long) {
        rateLimitUntilEpochMillis = epochMillis
        settingsRepository.saveRateLimitUntilEpochMillis(epochMillis)
    }

    private fun clearRateLimitUntilEpochMillis() {
        if (rateLimitUntilEpochMillis == 0L) return
        rateLimitUntilEpochMillis = 0L
        settingsRepository.clearRateLimitUntilEpochMillis()
    }

    private fun reconcileRateLimitState() {
        val rateLimitRemaining = activeRateLimitRemainingSeconds()
        val authRemaining = authCooldownSeconds()
        if (rateLimitRemaining <= 0L) {
            clearRateLimitUntilEpochMillis()
        }
        val remaining = max(authRemaining, rateLimitRemaining)
        _uiState.update { current ->
            current.copy(
                retryCooldownSecondsRemaining = remaining,
                canRetry = remaining <= 0 && current.isConfigured,
                errorMessage = if (rateLimitRemaining > 0L) {
                    rateLimitMessage(rateLimitRemaining)
                } else if (current.errorMessage.isRateLimitMessage()) {
                    null
                } else {
                    current.errorMessage
                }
            )
        }
        if (remaining > 0L) {
            startCooldownCountdown()
        }
    }

    private fun Throwable.findLibreHttpException(): LibreLinkUpHttpException? {
        var current: Throwable? = this
        while (current != null) {
            if (current is LibreLinkUpHttpException) return current
            current = current.cause
        }
        return null
    }

    private fun Throwable.findHttpStatusCode(): Int? = findLibreHttpException()?.statusCode

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

private fun String?.isRateLimitMessage(): Boolean {
    return this?.startsWith("Too many login attempts.", ignoreCase = true) == true
}

private class FetchInProgressException : RuntimeException("Fetch already in progress")

