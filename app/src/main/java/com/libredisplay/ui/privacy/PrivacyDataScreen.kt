package com.libredisplay.ui.privacy

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

enum class PrivacyAction {
    DeleteMyStoredData,
    DeleteLocalGlucoseHistory,
    DeleteMonitoredPeople,
    DisconnectAccount,
    ClearSessionData,
    ClearSavedTokenAndLoginAgain,
    ResetAppData,
    DeleteDemoData
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDataScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStart: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    viewModel: PrivacyDataViewModel = viewModel()
) {
    val context = LocalContext.current
    val event by viewModel.event.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingAction by remember { mutableStateOf<PrivacyAction?>(null) }
    val backupCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val backupOpenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.restoreBackup(uri)
    }
    var showStoredDataDetails by remember { mutableStateOf(false) }

    LaunchedEffect(event) {
        val value = event ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(value.message)
        if (value.navigateToStart) {
            onNavigateToStart()
        } else if (value.navigateToLogin) {
            onNavigateToLogin()
        }
        viewModel.consumeEvent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prywatność i dane") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "LibreCare zapisuje część danych lokalnie na tym urządzeniu.",
                fontSize = 13.sp
            )

            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { showStoredDataDetails = !showStoredDataDetails }) {
                Text(if (showStoredDataDetails) "Jakie dane są przechowywane? ▲" else "Jakie dane są przechowywane? ▼")
            }

            if (showStoredDataDetails) {
                Text(
                    "- nazwa i identyfikator monitorowanej osoby\n" +
                        "- odczyty oraz trendy glikemii\n" +
                        "- ustawienia aplikacji\n" +
                        "- dane sesji wymagane do polaczenia z LibreLinkUp\n\n" +
                        "Przed odinstalowaniem utworz kopie danych i ustawien.",
                    fontSize = 12.sp
                )
            }

            Text("Kopia danych", fontSize = 13.sp)

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    backupCreateLauncher.launch("LibreCare-live-backup-${System.currentTimeMillis()}.json")
                }
            ) {
                Text("Utworz kopie danych i ustawien")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    backupOpenLauncher.launch(arrayOf("application/json", "text/plain"))
                }
            ) {
                Text("Przywroc kopie")
            }

            Text(
                "Plik kopii moze zawierac prywatne dane medyczne - przechowuj go bezpiecznie.",
                fontSize = 12.sp,
                color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
            )

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
                        context.startActivity(intent)
                    }
                }
            ) {
                Text("Polityka prywatności")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToStatistics
            ) {
                Text("Informacje i statystyki")
            }

            if (viewModel.isDemoMode) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { pendingAction = PrivacyAction.DeleteDemoData }
                ) {
                    Text("Usuń dane trybu demo")
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { pendingAction = PrivacyAction.DeleteMyStoredData }
            ) { Text("Usuń moje zapisane dane") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { pendingAction = PrivacyAction.DeleteLocalGlucoseHistory }
            ) { Text("Usuń lokalną historię glikemii") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { pendingAction = PrivacyAction.DeleteMonitoredPeople }
            ) { Text("Usuń monitorowane osoby") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { pendingAction = PrivacyAction.DisconnectAccount }
            ) { Text("Odłącz konto LibreLinkUp") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { pendingAction = PrivacyAction.ClearSessionData }
            ) { Text("Wyczyść dane sesji") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { pendingAction = PrivacyAction.ClearSavedTokenAndLoginAgain }
            ) { Text("Wyczyść zapisany token i zaloguj ponownie") }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { pendingAction = PrivacyAction.ResetAppData }
            ) { Text("Zresetuj aplikację") }
        }
    }

    pendingAction?.let { action ->
        val dialog = confirmationDialogModel(action)
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) },
            confirmButton = {
                Button(onClick = {
                    pendingAction = null
                    when (action) {
                        PrivacyAction.DeleteMyStoredData -> viewModel.deleteMyStoredData()
                        PrivacyAction.DeleteLocalGlucoseHistory -> viewModel.deleteLocalGlucoseHistory()
                        PrivacyAction.DeleteMonitoredPeople -> viewModel.deleteMonitoredPeople()
                        PrivacyAction.DisconnectAccount -> viewModel.disconnectLibreLinkUpAccount()
                        PrivacyAction.ClearSessionData -> viewModel.clearSessionData()
                        PrivacyAction.ClearSavedTokenAndLoginAgain -> viewModel.clearSavedTokenAndLoginAgain()
                        PrivacyAction.ResetAppData -> viewModel.resetAppData()
                        PrivacyAction.DeleteDemoData -> viewModel.deleteDemoData()
                    }
                }) {
                    Text(dialog.confirm)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingAction = null }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

private data class ConfirmationDialogModel(
    val title: String,
    val message: String,
    val confirm: String
)

private fun confirmationDialogModel(action: PrivacyAction): ConfirmationDialogModel {
    return when (action) {
        PrivacyAction.DeleteMyStoredData -> ConfirmationDialogModel(
            title = "Usunąć moje zapisane dane?",
            message = "To usunie lokalnie zapisane dane LibreCare z tego urządzenia. Nie usuwa danych z konta LibreLinkUp.",
            confirm = "Usuń"
        )
        PrivacyAction.DeleteLocalGlucoseHistory -> ConfirmationDialogModel(
            title = "Usunąć lokalną historię glikemii?",
            message = "To usunie lokalną historię glikemii z tego urządzenia i pozostawi ustawienia konta.",
            confirm = "Usuń"
        )
        PrivacyAction.DeleteMonitoredPeople -> ConfirmationDialogModel(
            title = "Usunąć monitorowane osoby?",
            message = "To usunie lokalnie zapisane monitorowane osoby oraz aktualny wybór osoby.",
            confirm = "Usuń"
        )
        PrivacyAction.DisconnectAccount -> ConfirmationDialogModel(
            title = "Odłączyć konto LibreLinkUp?",
            message = "To usunie lokalną sesję i stan połączenia konta na tym urządzeniu.",
            confirm = "Odłącz"
        )
        PrivacyAction.ClearSessionData -> ConfirmationDialogModel(
            title = "Wyczyścić dane sesji?",
            message = "To wyczyści dane sesji i zapisane tokeny logowania. Lokalna historia glikemii pozostanie bez zmian.",
            confirm = "Wyczyść"
        )
        PrivacyAction.ClearSavedTokenAndLoginAgain -> ConfirmationDialogModel(
            title = "Wyczyścić zapisany token?",
            message = "To usunie zapisaną sesję logowania. Po tej operacji konieczne będzie ponowne zalogowanie do LibreLinkUp.",
            confirm = "Wyczyść i zaloguj ponownie"
        )
        PrivacyAction.ResetAppData -> ConfirmationDialogModel(
            title = "Zresetować aplikację LibreCare?",
            message = "To usunie lokalną historię, monitorowane osoby, wybór osoby, dane sesji i ustawienia aplikacji z tego urządzenia. Tej operacji nie można cofnąć.",
            confirm = "Zresetuj"
        )
        PrivacyAction.DeleteDemoData -> ConfirmationDialogModel(
            title = "Usunąć dane trybu demo?",
            message = "To usunie przykładowe osoby i historię glikemii trybu demo z tego urządzenia.",
            confirm = "Usuń"
        )
    }
}

