package com.libredisplay.ui.monitoring

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.data.repository.GlucoseRepository
import com.libredisplay.data.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val TAG = "MonitoringViewModel"
private const val STALE_THRESHOLD_MINUTES = 20L

/**
 * ViewModel for the monitoring dashboard.
 *
 * Responsibilities:
 *  - Start an automatic polling loop with the interval from [SettingsRepository].
 *  - Expose [uiState] to the UI.
 *  - Handle errors gracefully (keep last reading + error banner).
 *  - Never log passwords or auth tokens.
 */
class MonitoringViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val glucoseRepository  = GlucoseRepository(settingsRepository)

    private val _uiState = MutableStateFlow<MonitoringUiState>(MonitoringUiState.Loading)
    val uiState: StateFlow<MonitoringUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Manually trigger an immediate refresh. */
    fun refresh() {
        viewModelScope.launch { fetchReading() }
    }

    /** Call when settings change so the client is re-authenticated. */
    fun onSettingsChanged() {
        glucoseRepository.invalidate()
        restartPolling()
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun startPolling() {
        pollingJob = viewModelScope.launch {
            while (isActive) {
                fetchReading()
                val intervalMs = settingsRepository.loadSettings().refreshInterval * 60_000L
                delay(intervalMs)
            }
        }
    }

    private fun restartPolling() {
        pollingJob?.cancel()
        startPolling()
    }

    private suspend fun fetchReading() {
        // Mark as refreshing without losing the last reading
        val current = _uiState.value
        if (current is MonitoringUiState.Success) {
            _uiState.value = current.copy(isRefreshing = true, error = null)
        }

        try {
            val reading = glucoseRepository.fetchLatestReading()
            if (reading == null) {
                _uiState.value = MonitoringUiState.Error("No glucose data available from LibreLinkUp")
                return
            }

            val ageMin = ChronoUnit.MINUTES.between(reading.timestamp, Instant.now())
            _uiState.value = MonitoringUiState.Success(
                reading = reading,
                dataAgeMin = ageMin,
                isStale = ageMin >= STALE_THRESHOLD_MINUTES,
                isRefreshing = false,
                error = null
            )
            Log.d(TAG, "Reading updated: ${reading.value} mg/dL, age=${ageMin}min")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch reading: ${e.message}")
            val errorMsg = "Error: ${e.message ?: "Unknown error"}"
            val previous = _uiState.value
            _uiState.value = when (previous) {
                is MonitoringUiState.Success -> previous.copy(
                    isRefreshing = false,
                    error = errorMsg
                )
                else -> MonitoringUiState.Error(errorMsg)
            }
        }
    }
}

