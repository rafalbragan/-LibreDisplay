package com.libredisplay.ui.privacy

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.data.importer.AmbiguousTimePolicy
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportScreen(onBack: () -> Unit, onHistory: (String) -> Unit, model: CsvImportViewModel = viewModel()) {
    val state by model.state.collectAsState()
    var profileName by rememberSaveable { mutableStateOf("") }
    var confirmedName by rememberSaveable(state.preview?.sha256, state.selected?.id) { mutableStateOf("") }
    val single = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(model::preview) }
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { it?.let(model::discover) }
    val date: (Instant?) -> String = { instant -> instant?.let {
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm XXX").withZone(runCatching { ZoneId.of(state.zone) }.getOrDefault(ZoneId.systemDefault())).format(it)
    } ?: "brak" }
    BackHandler { if (state.busy) model.cancel() else { model.dismissPreview(); onBack() } }
    Scaffold(topBar = { TopAppBar(title = { Text("Import LibreView CSV") }, navigationIcon = {
        TextButton(onClick = { if (state.busy) model.cancel() else { model.dismissPreview(); onBack() } }) { Text("Wstecz") }
    }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).testTag("csv-import-screen"),
            verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Text("Import lokalnej historii · mg/dL", style = MaterialTheme.typography.titleMedium)
                Text("Dane nie są wysyłane do LibreLinkUp. Nazwa w pliku nigdy nie jest używana do odgadywania identyfikatora osoby.", style = MaterialTheme.typography.bodySmall)
            }
            item { Text("1. Wybierz osobę lub utwórz osobny profil", style = MaterialTheme.typography.titleSmall) }
            items(state.targets, key = { it.id }) { target ->
                OutlinedButton(onClick = { model.select(target) }, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().testTag("csv-profile-${target.id}")) {
                    Text((if (state.selected?.id == target.id) "✓ " else "") + target.name)
                }
            }
            item {
                OutlinedTextField(profileName, { profileName = it.take(100) }, label = { Text("Nazwa nowego profilu lokalnego") },
                    enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("csv-profile-name"), singleLine = true)
                TextButton(onClick = { model.newProfile(profileName) }, enabled = !state.busy && profileName.isNotBlank(),
                    modifier = Modifier.testTag("csv-create-profile")) { Text("Wybierz nowy profil") }
                state.selected?.let { Text("Wybrano: ${it.name}", style = MaterialTheme.typography.titleSmall) }
            }
            item {
                Text("2. Interpretacja czasu urządzenia", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(state.zone, { model.timeOptions(it, state.policy) }, label = { Text("Strefa IANA, np. Europe/Warsaw") },
                    enabled = !state.busy, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("csv-zone"))
                Text("Plik nie zawiera strefy. Jeśli urządzenie zmieniało strefę, importuj osobne pliki. Czas nieistniejący przy zmianie DST jest pomijany.", style = MaterialTheme.typography.bodySmall)
                AmbiguousTimePolicy.entries.forEach { policy ->
                    Row {
                        RadioButton(selected = state.policy == policy, onClick = { model.timeOptions(state.zone, policy) }, enabled = !state.busy)
                        Text(when (policy) {
                            AmbiguousTimePolicy.SKIP -> "Pomiń dwuznaczne godziny DST (bezpiecznie)"
                            AmbiguousTimePolicy.EARLIER_OFFSET -> "DST: wybierz wcześniejsze wystąpienie"
                            AmbiguousTimePolicy.LATER_OFFSET -> "DST: wybierz późniejsze wystąpienie"
                        }, modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Text("3. Wybierz eksport LibreView", style = MaterialTheme.typography.titleSmall)
                Button(onClick = { single.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream")) },
                    enabled = !state.busy && state.selected != null, modifier = Modifier.fillMaxWidth().testTag("csv-select-file")) { Text("Wybierz plik CSV") }
                OutlinedButton(onClick = { folder.launch(null) }, enabled = !state.busy && state.selected != null,
                    modifier = Modifier.fillMaxWidth().testTag("csv-select-folder")) { Text("Wybierz folder") }
                Text("Folder: tylko pliki bezpośrednio w wybranym katalogu, bez podfolderów. Lista pokazuje wyłącznie pliki ze zweryfikowanym nagłówkiem LibreView.", style = MaterialTheme.typography.bodySmall)
            }
            if (state.busy) item {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                OutlinedButton(onClick = model::cancel, modifier = Modifier.testTag("csv-cancel")) { Text("Anuluj operację") }
            }
            state.message?.let { message -> item { Text(message, modifier = Modifier.testTag("csv-message"), color = MaterialTheme.colorScheme.primary) } }
            items(state.files, key = { it.uri.toString() }) { file ->
                OutlinedButton(onClick = { model.preview(file.uri) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Column { Text(file.name); Text("${file.summary.supported} pomiarów · ${date(file.summary.earliest)} — ${date(file.summary.latest)}", style = MaterialTheme.typography.bodySmall) }
                }
            }
            state.preview?.let { preview -> item {
                Card(Modifier.fillMaxWidth().testTag("csv-preview")) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Podgląd — jeszcze nic nie zapisano", style = MaterialTheme.typography.titleMedium)
                    Text("Profil: ${preview.target.name}")
                    Text("Nagłówek pliku: ${preview.summary.metadata}", style = MaterialTheme.typography.bodySmall)
                    Text("Obsługiwane: ${preview.summary.supported}\nPominięte: ${preview.summary.skipped} (błędne: ${preview.summary.invalid})\nDuplikaty: ${preview.counts.duplicates}\nKonflikty: ${preview.counts.conflicts}\nNowe: ${preview.counts.newRecords}", modifier = Modifier.testTag("csv-counts"))
                    Text("Od: ${date(preview.summary.earliest)}\nDo: ${date(preview.summary.latest)}")
                    Text("Strefa: ${preview.options.zone.id} · DST: dwuznaczne ${preview.summary.ambiguous}, nieistniejące ${preview.summary.nonexistent}")
                    Text("Typy 0 i 1 to pomiary; typ 6 jest pomijany. Istniejące wartości nie zostaną nadpisane. W pliku pierwszy poprawny pomiar danego czasu ma pierwszeństwo. Importowana historia nie podlega automatycznej retencji.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(confirmedName, { confirmedName = it }, label = { Text("Potwierdź nazwę pacjenta / profilu") },
                        enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("csv-confirm-name"), singleLine = true)
                    Text("Sprawdź samodzielnie, czy dane w pliku należą do wybranej osoby.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { model.confirm(confirmedName) }, enabled = !state.busy && confirmedName.trim() == preview.target.name && preview.summary.supported > 0,
                        modifier = Modifier.fillMaxWidth().testTag("csv-confirm")) { Text("Potwierdzam — importuj") }
                } }
            } }
            if (state.result != null) item {
                OutlinedButton(onClick = { state.selected?.id?.let(onHistory) }, modifier = Modifier.fillMaxWidth().testTag("csv-show-history")) { Text("Pokaż zaimportowaną historię") }
            }
        }
    }
}

