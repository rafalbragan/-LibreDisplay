package com.libredisplay.ui.restore

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.backup.ConflictResolution
import com.libredisplay.data.repository.AppDataBackupRepository
import com.libredisplay.data.repository.SyncReason
import com.libredisplay.diagnostics.DiagnosticLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the startup conversation:
 *
 *  1. "Mam zapisane dane tych osób za taki okres - wczytać je?"
 *  2. reading-by-reading merge, differences are the only thing that triggers a question,
 *  3. a summary of what was actually loaded,
 *  4. only then an optional "czy chcesz dodatkowo wczytać plik z danymi?".
 */
class StartupRestoreViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LibreDisplayApp
    private val backupRepository = app.appDataBackupRepository

    private val _step = MutableStateFlow<StartupRestoreStep>(StartupRestoreStep.Hidden)
    val step: StateFlow<StartupRestoreStep> = _step.asStateFlow()

    private var staged: AppDataBackupRepository.StagedRestore? = null

    /** Loads the offer. When there is nothing stored we jump straight to the file question. */
    fun begin() {
        if (_step.value != StartupRestoreStep.Hidden) return
        _step.value = StartupRestoreStep.Working("Sprawdzam zapisane dane…")
        viewModelScope.launch {
            val offer = runCatching { backupRepository.loadAutomaticBackupOffer() }
                .getOrElse { AppDataBackupRepository.BackupOffer.EMPTY }
            _step.value = if (offer.hasData) {
                StartupRestoreStep.OfferLocalData(offer)
            } else {
                StartupRestoreStep.AskForFile
            }
        }
    }

    /** User confirmed "wczytaj moje dane". */
    fun loadStoredData() {
        _step.value = StartupRestoreStep.Working("Porównuję odczyt po odczycie…")
        viewModelScope.launch {
            runCatching { backupRepository.stageAutomaticBackupRestore() }
                .onSuccess { stagedRestore ->
                    staged = stagedRestore
                    if (stagedRestore.plan.hasConflicts) {
                        _step.value = StartupRestoreStep.ResolveConflicts(
                            StartupRestoreFormatter.conflictSummary(stagedRestore.plan)
                        )
                    } else {
                        applyStaged(ConflictResolution.KEEP_LOCAL)
                    }
                }
                .onFailure { failWith(it.message) }
        }
    }

    /** User picked a file after being asked, so we compare that archive instead. */
    fun loadFromFile(uri: Uri, password: String? = null) {
        _step.value = StartupRestoreStep.Working("Czytam wybrany plik…")
        viewModelScope.launch {
            runCatching { backupRepository.stageRestoreFromUri(uri, password) }
                .onSuccess { stagedRestore ->
                    staged = stagedRestore
                    if (stagedRestore.plan.hasConflicts) {
                        _step.value = StartupRestoreStep.ResolveConflicts(
                            StartupRestoreFormatter.conflictSummary(stagedRestore.plan)
                        )
                    } else {
                        applyStaged(ConflictResolution.KEEP_LOCAL)
                    }
                }
                .onFailure { failWith(it.message) }
        }
    }

    fun resolveConflicts(resolution: ConflictResolution) {
        _step.value = StartupRestoreStep.Working("Scalam dane…")
        viewModelScope.launch { applyStaged(resolution) }
    }

    private suspend fun applyStaged(resolution: ConflictResolution) {
        val current = staged ?: run {
            failWith("Brak przygotowanych danych do wczytania.")
            return
        }
        runCatching {
            backupRepository.applyWholeStagedRestore(
                staged = current,
                conflictResolution = resolution,
                restoreConfiguration = true
            )
        }.onSuccess { result ->
            staged = null
            DiagnosticLogger.logInfo(
                "StartupRestoreViewModel",
                "STARTUP RESTORE applied persons=${result.summary.livePersons} readings=${result.summary.liveReadings}"
            )
            val connectionNote = connectRestoredAccountSilently()
            _step.value = StartupRestoreStep.ShowSummary(
                listOfNotNull(result.report, connectionNote).joinToString("\n")
            )
        }.onFailure { failWith(it.message) }
    }

    /**
     * The restored configuration already carries verified credentials, so LibreCare connects on
     * its own instead of asking "czy połączyć z kontem LibreLinkUp?".
     */
    private suspend fun connectRestoredAccountSilently(): String? {
        val settings = app.settingsRepository.loadSettings()
        if (!settings.hasCredentials()) return null
        val authenticated = runCatching { app.authRepository.ensureAuthenticated(force = false) }
            .isSuccess
        if (!authenticated) {
            DiagnosticLogger.logWarning("StartupRestoreViewModel", "AUTO CONNECT failed")
            return "Nie udało się automatycznie połączyć z LibreLinkUp – sprawdź hasło w ustawieniach."
        }
        runCatching { app.glucoseSyncRepository.syncAllPersons(SyncReason.LOGIN) }
            .onFailure {
                DiagnosticLogger.logWarning(
                    "StartupRestoreViewModel",
                    "AUTO SYNC failed reason=${it.message}"
                )
            }
        app.appDataBackupRepository.refreshAutomaticBackupQuietly()
        return "Połączono z LibreLinkUp i pobrano bieżące dane z ostatnich 12 godzin."
    }

    /** After the summary we ask - once - about an extra file. */
    fun dismissSummary() {
        _step.value = StartupRestoreStep.AskForFile
    }

    fun skipStoredData() {
        _step.value = StartupRestoreStep.AskForFile
    }

    fun dismissFailure() {
        _step.value = StartupRestoreStep.AskForFile
    }

    fun finish() {
        staged = null
        _step.value = StartupRestoreStep.Hidden
    }

    private fun failWith(message: String?) {
        staged = null
        _step.value = StartupRestoreStep.Failure(
            message?.takeIf { it.isNotBlank() } ?: "Nie udało się wczytać danych."
        )
    }
}

