package com.libredisplay.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.LibreDisplayApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RetentionOptionItem(
    val label: String,
    val hours: Int
)

data class RetentionUiState(
    val options: List<RetentionOptionItem> = defaultRetentionOptions(),
    val selectedHours: Int = 24 * 30,
    val estimatedSizeLabel: String = "Za malo danych do dokladnej estymacji",
    val estimatedReadingsLabel: String = "Za malo danych do dokladnej estymacji",
    val pendingHours: Int? = null,
    val confirmRequired: Boolean = false,
    val isSaving: Boolean = false
)

class RetentionSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LibreDisplayApp

    private val _uiState = MutableStateFlow(RetentionUiState())
    val uiState: StateFlow<RetentionUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val settings = app.settingsRepository.loadSettings()
            _uiState.value = _uiState.value.copy(selectedHours = settings.retentionHours)
            recalcEstimate(settings.retentionHours)
        }
    }

    fun requestRetentionChange(hours: Int) {
        val current = _uiState.value.selectedHours
        _uiState.value = _uiState.value.copy(
            pendingHours = hours,
            confirmRequired = hours < current
        )
        if (hours >= current) {
            applyPendingRetention()
        }
    }

    fun dismissConfirmation() {
        _uiState.value = _uiState.value.copy(pendingHours = null, confirmRequired = false)
    }

    fun applyPendingRetention() {
        val pending = _uiState.value.pendingHours ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val current = app.settingsRepository.loadSettings()
            app.settingsRepository.saveSettings(current.copy(retentionHours = pending))
            app.localGlucoseHistoryRepository.deleteReadingsOlderThanHours(pending.toLong())
            recalcEstimate(pending)
            _uiState.value = _uiState.value.copy(
                selectedHours = pending,
                pendingHours = null,
                confirmRequired = false,
                isSaving = false
            )
        }
    }

    private suspend fun recalcEstimate(hours: Int) {
        val estimate = app.diagnosticsStatsRepository.estimateRetention(hours)
        val sizeLabel = estimate.estimatedBytes?.let { app.diagnosticsStatsRepository.formatBytes(it) }
            ?: "Za malo danych do dokladnej estymacji"
        val readingLabel = estimate.estimatedReadings?.toString()
            ?: "Za malo danych do dokladnej estymacji"
        _uiState.value = _uiState.value.copy(
            estimatedSizeLabel = sizeLabel,
            estimatedReadingsLabel = readingLabel
        )
    }
}

private fun defaultRetentionOptions(): List<RetentionOptionItem> = listOf(
    RetentionOptionItem("12 godzin", 12),
    RetentionOptionItem("24 godziny", 24),
    RetentionOptionItem("7 dni", 24 * 7),
    RetentionOptionItem("30 dni", 24 * 30),
    RetentionOptionItem("90 dni", 24 * 90),
    RetentionOptionItem("12 miesięcy", 24 * 30 * 12),
    RetentionOptionItem("24 miesiące", 24 * 30 * 24),
    RetentionOptionItem("36 miesięcy", 24 * 30 * 36),
    RetentionOptionItem("5 lat", 24 * 365 * 5),
    RetentionOptionItem("10 lat", 24 * 365 * 10)
)

