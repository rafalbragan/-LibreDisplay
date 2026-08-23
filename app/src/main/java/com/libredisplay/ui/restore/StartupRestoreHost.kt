package com.libredisplay.ui.restore

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.data.backup.ConflictResolution

/**
 * Hosts the startup "load my data" conversation.
 *
 * Nothing is written before the user agrees, and the file picker is only opened when the user
 * explicitly answers yes to the extra-file question.
 */
@Composable
fun StartupRestoreHost(
    active: Boolean,
    onFinished: () -> Unit,
    viewModel: StartupRestoreViewModel = viewModel()
) {
    val step by viewModel.step.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            viewModel.finish()
            onFinished()
        } else {
            viewModel.loadFromFile(uri)
        }
    }

    LaunchedEffect(active) {
        if (active) viewModel.begin()
    }

    fun close() {
        viewModel.finish()
        onFinished()
    }

    when (val current = step) {
        StartupRestoreStep.Hidden -> Unit

        is StartupRestoreStep.Working -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Dane LibreCare") },
            text = { Text(current.label) },
            confirmButton = {}
        )

        is StartupRestoreStep.OfferLocalData -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Wczytać zapisane dane?") },
            text = {
                ScrollableDialogContent {
                    Text(
                        text = StartupRestoreFormatter.offerHeadline(current.offer),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    StartupRestoreFormatter.offerLines(current.offer).forEach { line ->
                        Text(text = "• $line", style = MaterialTheme.typography.bodySmall)
                    }
                    if (current.offer.settingsAvailable) {
                        Text(
                            text = "Plik zawiera także konfigurację aplikacji.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.loadStoredData() }) { Text("Wczytaj dane") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.skipStoredData() }) { Text("Nie teraz") }
            }
        )

        is StartupRestoreStep.ResolveConflicts -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Dane różnią się") },
            text = {
                ScrollableDialogContent {
                    Text(
                        text = "Porównanie odczyt po odczycie wykryło ${current.summary.totalConflicts} " +
                            "różnic. Wybierz, które wartości zachować.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    current.summary.personLines.forEach { line ->
                        Text(text = "• $line", style = MaterialTheme.typography.bodySmall)
                    }
                    if (current.summary.examples.isNotEmpty()) {
                        Text(text = "Przykłady:", style = MaterialTheme.typography.bodySmall)
                        current.summary.examples.forEach { example ->
                            Text(text = "– $example", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.resolveConflicts(ConflictResolution.KEEP_LOCAL) }) {
                    Text("Zachowaj bieżące")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resolveConflicts(ConflictResolution.KEEP_BACKUP) }) {
                    Text("Użyj z archiwum")
                }
            }
        )

        is StartupRestoreStep.ShowSummary -> AlertDialog(
            onDismissRequest = { viewModel.dismissSummary() },
            title = { Text("Dane wczytane") },
            text = {
                ScrollableDialogContent {
                    Text(text = current.report, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSummary() }) { Text("Dalej") }
            }
        )

        StartupRestoreStep.AskForFile -> AlertDialog(
            onDismissRequest = { close() },
            title = { Text("Wczytać dodatkowy plik?") },
            text = {
                Text(
                    "Jeżeli masz plik z danymi z innego telefonu, możesz go teraz wskazać. " +
                        "Jeśli nie – aplikacja po prostu ruszy dalej."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    filePicker.launch(
                        arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")
                    )
                }) { Text("Wybierz plik") }
            },
            dismissButton = {
                TextButton(onClick = { close() }) { Text("Nie, dziękuję") }
            }
        )

        is StartupRestoreStep.Failure -> AlertDialog(
            onDismissRequest = { viewModel.dismissFailure() },
            title = { Text("Nie udało się wczytać danych") },
            text = { Text(current.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissFailure() }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun ScrollableDialogContent(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        content()
    }
}

