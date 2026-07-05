package com.libredisplay.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.repository.CredentialRepository
import com.libredisplay.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen.
 *
 * Exposes the current [AppSettings] as a [StateFlow] and persists changes
 * via [SettingsRepository].
 *
 * Passwords are never logged anywhere in this class.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val credentialRepository = CredentialRepository(application)

    private val _settings = MutableStateFlow(settingsRepository.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    fun onEmailChange(value: String) {
        _settings.value = _settings.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        // Password is updated in-memory only – never logged
        _settings.value = _settings.value.copy(password = value)
    }

    fun onRegionChange(value: String) {
        _settings.value = _settings.value.copy(region = value)
    }

    fun onRefreshIntervalChange(value: Int) {
        _settings.value = _settings.value.copy(refreshInterval = value.coerceIn(1, 60))
    }

    fun onKioskModeChange(value: Boolean) {
        _settings.value = _settings.value.copy(kioskMode = value)
    }

    fun onUseMockChange(value: Boolean) {
        _settings.value = _settings.value.copy(useMock = value)
    }

    fun saveSettings() {
        settingsRepository.saveSettings(_settings.value)
        _saveSuccess.value = true
    }

    fun saveCredentialsInManager() {
        val current = _settings.value
        viewModelScope.launch {
            runCatching {
                credentialRepository.saveCredentials(current.email, current.password)
            }.onSuccess {
                _messages.emit("Dane logowania zapisano w managerze hasel")
            }.onFailure {
                _messages.emit(it.message ?: "Nie udalo sie zapisac danych logowania")
            }
        }
    }

    fun loadCredentialsFromManager() {
        viewModelScope.launch {
            val pair = credentialRepository.getCredentialsOrNull()
            if (pair == null) {
                _messages.emit("Brak zapisanych danych logowania")
                return@launch
            }
            _settings.value = _settings.value.copy(email = pair.first, password = pair.second)
            _messages.emit("Wczytano zapisane dane logowania")
        }
    }

    fun logoutAndClearLocalData() {
        _settings.value = AppSettings()
        settingsRepository.saveSettings(_settings.value)
        credentialRepository.clearLocalFallback()
        viewModelScope.launch {
            _messages.emit("Wylogowano i usunieto zapisane dane")
        }
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }
}

