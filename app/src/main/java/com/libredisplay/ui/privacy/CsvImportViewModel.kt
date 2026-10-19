package com.libredisplay.ui.privacy

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.importer.*
import com.libredisplay.data.model.AppMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.ZoneId

data class CsvImportUiState(val targets: List<ImportTarget> = emptyList(), val selected: ImportTarget? = null,
    val files: List<VerifiedCsvDocument> = emptyList(), val preview: ImportPreview? = null,
    val busy: Boolean = false, val message: String? = null, val result: ImportResult? = null,
    val zone: String = ZoneId.systemDefault().id, val policy: AmbiguousTimePolicy = AmbiguousTimePolicy.SKIP)

class CsvImportViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LibreDisplayApp
    private val repository = LibreViewImportRepository(app.database, File(app.cacheDir, "libreview-import")) {
        app.settingsRepository.loadSettings().appMode == AppMode.DEMO
    }
    private val documents = LibreViewDocuments(app.contentResolver)
    private val _state = MutableStateFlow(CsvImportUiState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    init { viewModelScope.launch {
        val targets = repository.targets()
        _state.update { it.copy(targets = targets) }
    } }

    fun select(target: ImportTarget) {
        if (_state.value.busy) return
        invalidate(); _state.update { it.copy(selected = target) }
    }
    fun newProfile(name: String) {
        try { select(repository.newProfile(name)) } catch (_: IllegalArgumentException) {
            _state.update { it.copy(message = "Wpisz nazwę profilu (1–100 znaków).") }
        }
    }
    fun timeOptions(zone: String, policy: AmbiguousTimePolicy) {
        if (_state.value.busy) return
        invalidate(); _state.update { it.copy(zone = zone, policy = policy, files = emptyList()) }
    }
    private fun options() = CsvTimeOptions(ZoneId.of(_state.value.zone.trim()), _state.value.policy)
    fun discover(tree: Uri) = execute {
        _state.update { it.copy(files = emptyList()) }
        val count = documents.discover(tree, options()) { document -> _state.update { it.copy(files = it.files + document) } }
        _state.update { it.copy(message = "Sprawdzono $count plików. Zweryfikowane pliki LibreView: ${it.files.size}.") }
    }
    fun preview(uri: Uri) = execute {
        val target = requireNotNull(_state.value.selected) { "Wybierz profil przed podglądem." }
        val result = repository.preview({ app.contentResolver.openInputStream(uri) ?: error("Nie można otworzyć pliku.") }, target, options())
        _state.update { it.copy(preview = result, result = null) }
    }
    fun confirm(name: String) = execute(clearPreview = false) {
        val preview = requireNotNull(_state.value.preview)
        val result = repository.confirm(preview, name)
        preview.close()
        _state.update { it.copy(preview = null, result = result,
            selected = it.selected?.copy(create = false),
            message = "Import zakończony. Zapisano ${result.inserted} nowych pomiarów. Konflikty nie zostały nadpisane.") }
        val targets = repository.targets()
        _state.update { it.copy(targets = targets) }
    }
    fun cancel() { job?.cancel() }
    fun dismissPreview() { if (!_state.value.busy) invalidate() }
    private fun invalidate() {
        if (_state.value.busy) return
        _state.value.preview?.close()
        _state.update { it.copy(preview = null, result = null, message = null) }
    }
    private fun execute(clearPreview: Boolean = true, action: suspend () -> Unit) {
        if (job?.isActive == true) return
        if (clearPreview) invalidate()
        _state.update { it.copy(busy = true, message = null) }
        job = viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) {
                _state.update { it.copy(message = "Przerwano operację. Niezatwierdzona transakcja została wycofana.") }
                throw cancelled
            } catch (error: Exception) {
                // Do not log raw provider errors, filenames, CSV contents or patient identifiers.
                val safeMessage = when (error) {
                    is CsvFormatException -> error.message
                    is java.time.DateTimeException -> "Nieprawidłowa strefa czasowa. Użyj nazwy, np. Europe/Warsaw."
                    is java.io.IOException, is SecurityException -> "Nie można odczytać dokumentu. Wybierz plik ponownie."
                    is IllegalArgumentException, is IllegalStateException -> "Import nie został zatwierdzony. Sprawdź profil i utwórz nowy podgląd."
                    else -> "Nie udało się zaimportować danych. Utwórz nowy podgląd."
                }
                _state.update { it.copy(message = safeMessage) }
            } finally { _state.update { it.copy(busy = false) } }
        }
    }
    override fun onCleared() { _state.value.preview?.close(); super.onCleared() }
}


