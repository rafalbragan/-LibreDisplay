package com.libredisplay.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.HbA1cSettings
import com.libredisplay.diagnostics.DiagnosticLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LibreDisplayApp
    private val settingsRepository = app.settingsRepository
    private val authRepository = app.authRepository

    private val _settings = MutableStateFlow(settingsRepository.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _hba1cSettings = MutableStateFlow(
        settingsRepository.loadHbA1cSettings(_settings.value.selectedPatientId)
    )
    val hba1cSettings: StateFlow<HbA1cSettings> = _hba1cSettings.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun onEmailChange(value: String) {
        _settings.value = _settings.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        _settings.value = _settings.value.copy(password = value)
    }

    fun onUseMockChange(value: Boolean) {
        _settings.value = _settings.value.copy(useMock = value)
    }

    fun onLabHbA1cPercentChange(value: String) {
        val normalized = value.replace(',', '.').toDoubleOrNull()
        _hba1cSettings.value = _hba1cSettings.value.copy(
            labHbA1cPercent = normalized?.coerceIn(3.5, 20.0)
        )
    }

    fun onLabHbA1cDateChange(value: String) {
        val parsed = runCatching { LocalDate.parse(value.trim()) }.getOrNull()
        _hba1cSettings.value = _hba1cSettings.value.copy(labHbA1cDate = parsed)
    }

    fun onTargetHbA1cPercentChange(value: String) {
        val parsed = value.replace(',', '.').toDoubleOrNull()?.coerceIn(5.0, 12.0) ?: 7.5
        _hba1cSettings.value = _hba1cSettings.value.copy(targetHbA1cPercent = parsed)
    }

    fun onRegionModeChange(value: String) {
        _settings.value = _settings.value.copy(regionMode = value)
    }

    fun onCustomBaseUrlChange(value: String) {
        _settings.value = _settings.value.copy(customBaseUrl = value)
    }

    fun onTargetLowChange(value: String) {
        value.toIntOrNull()?.let { parsed ->
            val low = parsed.coerceIn(40, 300)
            val high = _settings.value.targetHigh.coerceAtLeast(low + 1)
            _settings.value = _settings.value.copy(targetLow = low, targetHigh = high)
        }
    }

    fun onTargetHighChange(value: String) {
        value.toIntOrNull()?.let { parsed ->
            val high = parsed.coerceIn(60, 400)
            val low = _settings.value.targetLow.coerceAtMost(high - 1)
            _settings.value = _settings.value.copy(targetLow = low, targetHigh = high)
        }
    }

    fun saveSettings() {
        settingsRepository.saveSettings(_settings.value)
        settingsRepository.saveHbA1cSettings(
            _hba1cSettings.value.copy(patientId = _settings.value.selectedPatientId)
        )
        authRepository.clearSession()
        DiagnosticLogger.logInfo("SettingsViewModel", "Settings saved useMock=${_settings.value.useMock}")
        _message.value = "Ustawienia zapisane"
    }

    fun resetSession() {
        authRepository.clearSession()
        _message.value = "Wyczyszczono zapisany token. Zaloguj sie ponownie recznie."
    }

    fun clearMessage() {
        _message.value = null
    }
}
