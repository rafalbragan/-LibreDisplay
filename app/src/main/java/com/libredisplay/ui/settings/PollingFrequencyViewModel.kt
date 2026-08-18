package com.libredisplay.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.LibreDisplayApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PollingOptionItem(
    val label: String,
    val minutes: Int
)

data class PollingUiState(
    val options: List<PollingOptionItem> = pollingOptions(),
    val selectedMinutes: Int = 60,
    val currentUsageLabel: String = "Za malo danych do dokladnej estymacji",
    val estimatedUsageLabel: String = "Za malo danych do dokladnej estymacji",
    val isSaving: Boolean = false
)

class PollingFrequencyViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LibreDisplayApp

    private val _uiState = MutableStateFlow(PollingUiState())
    val uiState: StateFlow<PollingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val settings = app.settingsRepository.loadSettings()
            _uiState.value = _uiState.value.copy(selectedMinutes = settings.backgroundPollingMinutes)
            recalc(settings.backgroundPollingMinutes)
        }
    }

    fun savePolling(minutes: Int) {
        val safeMinutes = minutes.coerceIn(15, 60)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val settings = app.settingsRepository.loadSettings()
            app.settingsRepository.saveSettings(settings.copy(backgroundPollingMinutes = safeMinutes))
            com.libredisplay.sync.LibreDisplaySyncScheduler.schedule(getApplication())
            recalc(safeMinutes)
            _uiState.value = _uiState.value.copy(selectedMinutes = safeMinutes, isSaving = false)
        }
    }

    private suspend fun recalc(minutes: Int) {
        val estimate = app.diagnosticsStatsRepository.estimatePolling(minutes)
        val current = estimate.currentDailyBytes?.let { app.diagnosticsStatsRepository.formatBytes(it) + " / dzien" }
            ?: "Za malo danych do dokladnej estymacji"
        val selected = estimate.selectedDailyBytes?.let { app.diagnosticsStatsRepository.formatBytes(it) + " / dzien" }
            ?: "Za malo danych do dokladnej estymacji"
        _uiState.value = _uiState.value.copy(
            currentUsageLabel = current,
            estimatedUsageLabel = selected
        )
    }
}

private fun pollingOptions(): List<PollingOptionItem> = listOf(
    PollingOptionItem("15 minut", 15),
    PollingOptionItem("30 minut", 30),
    PollingOptionItem("60 minut", 60)
)

