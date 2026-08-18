package com.libredisplay.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.repository.DatabaseStats
import com.libredisplay.data.repository.NetworkUsageStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val databaseStats: DatabaseStats? = null,
    val networkStats: NetworkUsageStats? = null,
    val pollingLabel: String = "-",
    val pollingUsageCurrentLabel: String = "-",
    val pollingUsageEstimatedLabel: String = "-",
    val isDemoMode: Boolean = false,
    val error: String? = null
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LibreDisplayApp

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                val settings = app.settingsRepository.loadSettings()
                val dbStats = app.diagnosticsStatsRepository.loadDatabaseStats()
                val networkStats = app.diagnosticsStatsRepository.loadNetworkUsageStats(settings.appMode)
                val pollingEstimate = app.diagnosticsStatsRepository.estimatePolling(settings.backgroundPollingMinutes)

                val currentUsage = pollingEstimate.currentDailyBytes?.let { app.diagnosticsStatsRepository.formatBytes(it) + " / dzien" }
                    ?: "Za malo danych do dokladnej estymacji"
                val estimatedUsage = pollingEstimate.selectedDailyBytes?.let { app.diagnosticsStatsRepository.formatBytes(it) + " / dzien" }
                    ?: "Za malo danych do dokladnej estymacji"

                StatisticsUiState(
                    isLoading = false,
                    databaseStats = dbStats,
                    networkStats = networkStats,
                    pollingLabel = "${settings.backgroundPollingMinutes} min",
                    pollingUsageCurrentLabel = currentUsage,
                    pollingUsageEstimatedLabel = estimatedUsage,
                    isDemoMode = settings.appMode == AppMode.DEMO,
                    error = null
                )
            }.onSuccess {
                _uiState.value = it
            }.onFailure { throwable ->
                _uiState.value = StatisticsUiState(
                    isLoading = false,
                    error = throwable.message ?: "Nieznany blad"
                )
            }
        }
    }
}

