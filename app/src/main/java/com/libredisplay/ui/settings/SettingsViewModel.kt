package com.libredisplay.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.HbA1cSettings
import com.libredisplay.diagnostics.DiagnosticLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    fun reloadFromRepository() {
        _settings.value = settingsRepository.loadSettings()
        _hba1cSettings.value = settingsRepository.loadHbA1cSettings(_settings.value.selectedPatientId)
    }

    fun onEmailChange(value: String) {
        _settings.value = _settings.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        _settings.value = _settings.value.copy(password = value)
    }

    fun onUseMockChange(value: Boolean) {
        _settings.value = _settings.value.copy(appMode = if (value) AppMode.DEMO else AppMode.LIVE)
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
        val settingsToSave = _settings.value.let { current ->
            if (current.appMode == AppMode.NONE && (current.hasCredentials() || current.useMock)) {
                current.copy(appMode = if (current.useMock) AppMode.DEMO else AppMode.LIVE)
            } else {
                current
            }
        }
        settingsRepository.saveSettings(settingsToSave)
        settingsRepository.saveHbA1cSettings(
            _hba1cSettings.value.copy(patientId = settingsToSave.selectedPatientId)
        )
        authRepository.clearSession()
        reloadFromRepository()
        DiagnosticLogger.logInfo("SettingsViewModel", "Settings saved appMode=${settingsToSave.appMode}")
        _message.value = "Ustawienia zapisane"
    }

    fun saveAndLogin() {
        val draft = _settings.value.copy(appMode = AppMode.LIVE)
        settingsRepository.saveSettings(draft)
        settingsRepository.saveHbA1cSettings(
            _hba1cSettings.value.copy(patientId = draft.selectedPatientId)
        )
        viewModelScope.launch {
            runCatching {
                authRepository.ensureAuthenticated(force = true)
            }.onSuccess {
                reloadFromRepository()
                _message.value = "Ustawienia zapisane"
            }.onFailure {
                val cooldownSeconds = authRepository.cooldownRemainingSeconds()
                _message.value = if (cooldownSeconds > 0) {
                    "Zbyt wiele prób logowania. Spróbuj ponownie za $cooldownSeconds sekund."
                } else {
                    "Nie udało się zalogować. Sprawdź email i hasło albo spróbuj ponownie później."
                }
            }
        }
    }

    fun resetSession() {
        val current = settingsRepository.loadSettings()
        authRepository.clearSession()
        if (current.isDemoPatientSelected()) {
            settingsRepository.clearSelectedPatientId()
        }
        settingsRepository.switchToLiveMode()
        reloadFromRepository()
        _message.value = "Zapisany token został usunięty. Zaloguj się ponownie."
    }

    fun clearMessage() {
        _message.value = null
    }
}
