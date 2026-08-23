package com.libredisplay.ui.privacy

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.backup.ConflictResolution
import com.libredisplay.data.repository.AppDataBackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrivacyActionEvent(
    val message: String,
    val navigateToStart: Boolean = false,
    val navigateToLogin: Boolean = false
)

class PrivacyDataViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LibreDisplayApp
    private val privacyRepository = app.privacyRepository
    private val backupRepository = app.appDataBackupRepository

    private val _event = MutableStateFlow<PrivacyActionEvent?>(null)
    val event: StateFlow<PrivacyActionEvent?> = _event.asStateFlow()

    private val _backupInfo = MutableStateFlow<AppDataBackupRepository.AutomaticBackupInfo?>(null)
    val backupInfo: StateFlow<AppDataBackupRepository.AutomaticBackupInfo?> = _backupInfo.asStateFlow()

    private val _staged = MutableStateFlow<AppDataBackupRepository.StagedRestore?>(null)
    val staged: StateFlow<AppDataBackupRepository.StagedRestore?> = _staged.asStateFlow()

    private val _restoreReport = MutableStateFlow<String?>(null)
    val restoreReport: StateFlow<String?> = _restoreReport.asStateFlow()

    /** Set when a legacy (encrypted) archive was selected and needs the original password. */
    private val _passwordRequiredForUri = MutableStateFlow<Uri?>(null)
    val passwordRequiredForUri: StateFlow<Uri?> = _passwordRequiredForUri.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val isDemoMode: Boolean
        get() = app.settingsRepository.loadSettings().useMock

    init {
        refreshBackupInfo()
    }

    // ------------------------------------------------------------------ backup

    fun refreshBackupInfo() {
        viewModelScope.launch {
            runCatching { backupRepository.automaticBackupInfo() }
                .onSuccess { _backupInfo.value = it }
        }
    }

    fun createBackupNow() {
        viewModelScope.launch {
            _busy.value = true
            runCatching { backupRepository.createAutomaticBackup(includeConfiguration = true) }
                .onSuccess { summary ->
                    _event.value = PrivacyActionEvent(
                        "Kopia zapisana automatycznie. Osoby: ${summary.livePersons}, odczyty: ${summary.liveReadings}."
                    )
                    refreshBackupInfo()
                }
                .onFailure { _event.value = PrivacyActionEvent("Nie udało się zapisać kopii: ${it.message.orEmpty()}") }
            _busy.value = false
        }
    }

    fun exportBackupTo(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { backupRepository.exportAutomaticBackupTo(uri) }
                .onSuccess { summary ->
                    _event.value = PrivacyActionEvent(
                        "Plik przeniesienia zapisany. Osoby: ${summary.livePersons}, odczyty: ${summary.liveReadings}."
                    )
                    refreshBackupInfo()
                }
                .onFailure { _event.value = PrivacyActionEvent("Nie udało się zapisać pliku: ${it.message.orEmpty()}") }
            _busy.value = false
        }
    }

    /** Ensures the file exists before it is shared through the system share sheet. */
    fun prepareBackupForSharing(onReady: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { backupRepository.createAutomaticBackup(includeConfiguration = true) }
                .onSuccess {
                    refreshBackupInfo()
                    onReady()
                }
                .onFailure { _event.value = PrivacyActionEvent("Nie udało się przygotować pliku: ${it.message.orEmpty()}") }
            _busy.value = false
        }
    }

    // ------------------------------------------------------------------ restore

    fun stageFromAutomaticBackup() {
        viewModelScope.launch {
            _busy.value = true
            runCatching { backupRepository.stageAutomaticBackupRestore() }
                .onSuccess { _staged.value = it }
                .onFailure { _event.value = PrivacyActionEvent("Nie udało się odczytać kopii: ${it.message.orEmpty()}") }
            _busy.value = false
        }
    }

    fun stageFromUri(uri: Uri, password: String? = null) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { backupRepository.stageRestoreFromUri(uri, password) }
                .onSuccess {
                    _passwordRequiredForUri.value = null
                    _staged.value = it
                }
                .onFailure { error ->
                    val message = error.message.orEmpty()
                    if (message.contains("zaszyfrowany") || message.contains("hasło")) {
                        _passwordRequiredForUri.value = uri
                    } else {
                        _event.value = PrivacyActionEvent("Nie udało się odczytać kopii: $message")
                    }
                }
            _busy.value = false
        }
    }

    fun cancelPasswordPrompt() {
        _passwordRequiredForUri.value = null
    }

    fun clearStaged() {
        _staged.value = null
    }

    fun clearRestoreReport() {
        _restoreReport.value = null
    }

    fun applyStaged(
        selectedPatientIds: Set<String>,
        conflictResolution: ConflictResolution,
        restoreConfiguration: Boolean
    ) {
        val staged = _staged.value ?: return
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                backupRepository.applyRestorePlan(
                    staged = staged,
                    selectedPatientIds = selectedPatientIds,
                    conflictResolution = conflictResolution,
                    restoreConfiguration = restoreConfiguration
                )
            }
                .onSuccess { result ->
                    _restoreReport.value = result.report
                    _staged.value = null
                    refreshBackupInfo()
                }
                .onFailure {
                    _event.value = PrivacyActionEvent("Nie udało się przywrócić kopii: ${it.message.orEmpty()}")
                }
            _busy.value = false
        }
    }

    // ------------------------------------------------------------------ privacy actions

    fun deleteMyStoredData() {
        viewModelScope.launch {
            privacyRepository.deleteMyStoredData()
            _event.value = PrivacyActionEvent("Dane zostały usunięte z tego urządzenia.", navigateToStart = true)
        }
    }

    fun deleteLocalGlucoseHistory() {
        viewModelScope.launch {
            privacyRepository.deleteLocalGlucoseHistory()
            _event.value = PrivacyActionEvent("Lokalna historia glikemii została usunięta.")
        }
    }

    fun deleteMonitoredPeople() {
        viewModelScope.launch {
            privacyRepository.deleteObservedPeople()
            _event.value = PrivacyActionEvent("Monitorowane osoby i wybór osoby zostały usunięte.", navigateToStart = true)
        }
    }

    fun disconnectLibreLinkUpAccount() {
        privacyRepository.disconnectAccount()
        _event.value = PrivacyActionEvent("Konto LibreLinkUp zostało odłączone.", navigateToStart = true)
    }

    fun clearSessionData() {
        privacyRepository.clearSessionData()
        _event.value = PrivacyActionEvent("Dane sesji zostały wyczyszczone.", navigateToLogin = true)
    }

    fun clearSavedTokenAndLoginAgain() {
        privacyRepository.clearSavedTokenAndPrepareLiveLogin()
        _event.value = PrivacyActionEvent(
            "Zapisany token został usunięty. Zaloguj się ponownie.",
            navigateToLogin = true
        )
    }

    fun resetAppData() {
        viewModelScope.launch {
            privacyRepository.resetAppData()
            _event.value = PrivacyActionEvent("Aplikacja została zresetowana.", navigateToStart = true)
        }
    }

    fun deleteDemoData() {
        viewModelScope.launch {
            privacyRepository.deleteDemoData()
            _event.value = PrivacyActionEvent("Dane trybu demo zostały usunięte.", navigateToStart = true)
        }
    }

    fun consumeEvent() {
        _event.value = null
    }
}

