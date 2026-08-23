package com.libredisplay.ui.privacy

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.data.backup.BackupMergeEngine
import com.libredisplay.data.backup.ConflictResolution
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

private val BACKUP_MIME_TYPES = arrayOf(
    "application/json",
    "application/octet-stream",
    "text/plain",
    "*/*"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDataScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStart: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    openRestorePickerOnEnter: Boolean = false,
    onRestorePickerConsumed: () -> Unit = {},
    viewModel: PrivacyDataViewModel = viewModel()
) {
    val context = LocalContext.current
    val event by viewModel.event.collectAsState()
    val backupInfo by viewModel.backupInfo.collectAsState()
    val staged by viewModel.staged.collectAsState()
    val restoreReport by viewModel.restoreReport.collectAsState()
    val passwordRequiredForUri by viewModel.passwordRequiredForUri.collectAsState()
    val busy by viewModel.busy.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingAction by remember { mutableStateOf<PrivacyAction?>(null) }
    var showStoredDataDetails by remember { mutableStateOf(false) }
    var legacyPassword by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.exportBackupTo(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.stageFromUri(uri) }

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

    LaunchedEffect(openRestorePickerOnEnter) {
        if (openRestorePickerOnEnter) {
            importLauncher.launch(BACKUP_MIME_TYPES)
            onRestorePickerConsumed()
        }
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
            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text("LibreCare zapisuje część danych lokalnie na tym urządzeniu.", fontSize = 13.sp)

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showStoredDataDetails = !showStoredDataDetails }
            ) {
                Text(if (showStoredDataDetails) "Jakie dane są przechowywane? ▲" else "Jakie dane są przechowywane? ▼")
            }

            if (showStoredDataDetails) {
                Text(
                    "- nazwa i identyfikator monitorowanej osoby\n" +
                        "- odczyty oraz trendy glikemii\n" +
                        "- ustawienia aplikacji\n" +
                        "- dane sesji wymagane do połączenia z LibreLinkUp",
                    fontSize = 12.sp
                )
            }

            HorizontalDivider()

            AppLockSection()

            HorizontalDivider()

            AutomaticBackupCard(info = backupInfo)

            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                enabled = !busy,
                onClick = { viewModel.createBackupNow() }
            ) { Text("Odśwież kopię teraz") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                enabled = !busy,
                onClick = { viewModel.stageFromAutomaticBackup() }
            ) { Text("Przywróć z kopii na tym urządzeniu") }

            Text("Przeniesienie na inny telefon", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                "Kopia jest automatycznie przenoszona przez systemowe narzędzia (Samsung Smart Switch, " +
                    "kopia Google, transfer przewodowy). Poniższe przyciski działają na dowolnym telefonie.",
                fontSize = 12.sp
            )

            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                enabled = !busy,
                onClick = { exportLauncher.launch("librecare-backup.json") }
            ) { Text("Zapisz plik (dysk lub chmura)") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                enabled = !busy,
                onClick = {
                    viewModel.prepareBackupForSharing {
                        runCatching {
                            val file = (context.applicationContext as com.libredisplay.LibreDisplayApp)
                                .appDataBackupRepository.automaticBackupFile()
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "LibreCare - kopia danych")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(share, "Udostępnij kopię LibreCare"))
                        }
                    }
                }
            ) { Text("Udostępnij kopię (chmura, e-mail, inny telefon)") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                enabled = !busy,
                onClick = { importLauncher.launch(BACKUP_MIME_TYPES) }
            ) { Text("Wczytaj plik kopii") }

            HorizontalDivider()

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                    }
                }
            ) { Text("Polityka prywatności") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToStatistics
            ) { Text("Informacje i statystyki") }

            if (viewModel.isDemoMode) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { pendingAction = PrivacyAction.DeleteDemoData }
                ) { Text("Usuń dane trybu demo") }
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
                }) { Text(dialog.confirm) }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingAction = null }) { Text("Anuluj") }
            }
        )
    }

    passwordRequiredForUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelPasswordPrompt() },
            title = { Text("Starsza kopia zapasowa") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Wybrany plik pochodzi ze starszej wersji LibreCare i jest zaszyfrowany. Podaj hasło użyte przy jego tworzeniu.")
                    TextField(
                        value = legacyPassword,
                        onValueChange = { legacyPassword = it },
                        singleLine = true,
                        label = { Text("Hasło kopii") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.stageFromUri(uri, legacyPassword)
                    legacyPassword = ""
                }) { Text("Wczytaj") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    legacyPassword = ""
                    viewModel.cancelPasswordPrompt()
                }) { Text("Anuluj") }
            }
        )
    }

    staged?.let { stagedRestore ->
        RestorePlanDialog(
            staged = stagedRestore,
            onCancel = { viewModel.clearStaged() },
            onConfirm = { ids, resolution, restoreConfiguration ->
                viewModel.applyStaged(ids, resolution, restoreConfiguration)
            }
        )
    }

    restoreReport?.let { report ->
        AlertDialog(
            onDismissRequest = { viewModel.clearRestoreReport() },
            title = { Text("Podsumowanie przywracania") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(report, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearRestoreReport()
                    onNavigateToStart()
                }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun AppLockSection() {
    val context = LocalContext.current
    val activity = remember(context) { context as? androidx.fragment.app.FragmentActivity }
    val scope = rememberCoroutineScope()
    val repository = remember(context) { com.libredisplay.auth.AppLockRepository(context) }
    val passkeyManager = remember(context) { com.libredisplay.auth.PasskeyManager(context) }

    var method by remember { mutableStateOf(repository.method) }
    var hasPasskey by remember { mutableStateOf(repository.hasPasskey) }
    var status by remember { mutableStateOf(repository.describeStatus()) }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val capable = remember(repository) { repository.isDeviceCapable() }

    fun refresh() {
        method = repository.method
        hasPasskey = repository.hasPasskey
        status = repository.describeStatus()
    }

    /** The fingerprint is verified FIRST; the method is switched only after a real success. */
    fun enableBiometric() {
        val host = activity ?: run {
            feedback = "Nie można uruchomić weryfikacji na tym ekranie."
            return
        }
        if (busy) return
        busy = true
        scope.launch {
            val manager = com.libredisplay.auth.BiometricAuthManager(host)
            if (!manager.canAuthenticate()) {
                feedback = "Najpierw skonfiguruj odcisk palca lub blokadę ekranu w ustawieniach telefonu."
                busy = false
                return@launch
            }
            when (val result = manager.authenticate(
                title = "Potwierdź odcisk palca",
                subtitle = "Potwierdź, aby włączyć logowanie odciskiem palca"
            )) {
                com.libredisplay.auth.BiometricResult.Success -> {
                    repository.enableBiometricUnlock()
                    feedback = "Logowanie odciskiem palca zostało włączone."
                }
                com.libredisplay.auth.BiometricResult.Cancelled ->
                    feedback = "Weryfikacja anulowana – nic nie zostało zmienione."
                com.libredisplay.auth.BiometricResult.NotAvailable ->
                    feedback = "To urządzenie nie obsługuje weryfikacji odciskiem palca."
                com.libredisplay.auth.BiometricResult.LockedOut ->
                    feedback = "Zbyt wiele prób. Spróbuj ponownie za chwilę."
                is com.libredisplay.auth.BiometricResult.Error -> feedback = result.message
            }
            refresh()
            busy = false
        }
    }

    fun createPasskey() {
        if (busy) return
        busy = true
        scope.launch {
            val settings = (context.applicationContext as com.libredisplay.LibreDisplayApp)
                .settingsRepository.loadSettings()
            val userName = settings.email.ifBlank { "LibreCare" }
            when (val result = passkeyManager.createPasskey(userId = userName, userName = userName)) {
                is com.libredisplay.auth.PasskeyResult.Created -> {
                    repository.enablePasskeyUnlock(result.credentialId)
                    feedback = "Klucz dostępu został utworzony i będzie używany do odblokowania."
                }
                com.libredisplay.auth.PasskeyResult.Cancelled ->
                    feedback = "Tworzenie klucza dostępu anulowane."
                is com.libredisplay.auth.PasskeyResult.Unsupported -> feedback = result.message
                is com.libredisplay.auth.PasskeyResult.Error -> feedback = result.message
                com.libredisplay.auth.PasskeyResult.Verified -> Unit
            }
            refresh()
            busy = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Blokada aplikacji", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = method == com.libredisplay.auth.UnlockMethod.BIOMETRIC,
                enabled = capable && !busy,
                onCheckedChange = { checked ->
                    if (checked) {
                        enableBiometric()
                    } else {
                        repository.disable()
                        feedback = "Blokada aplikacji została wyłączona."
                        refresh()
                    }
                }
            )
            Text("Odcisk palca, PIN lub blokada ekranu przy uruchomieniu", fontSize = 13.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = method == com.libredisplay.auth.UnlockMethod.PASSKEY,
                enabled = hasPasskey && !busy,
                onCheckedChange = { checked ->
                    if (checked && hasPasskey) {
                        repository.enablePasskeyUnlock(repository.passkeyId)
                        feedback = "Odblokowanie kluczem dostępu zostało włączone."
                    } else {
                        repository.disable()
                        feedback = "Blokada aplikacji została wyłączona."
                    }
                    refresh()
                }
            )
            Text("Klucz dostępu (passkey)", fontSize = 13.sp)
        }

        OutlinedButton(
            onClick = { createPasskey() },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (hasPasskey) "Utwórz nowy klucz dostępu" else "Utwórz klucz dostępu")
        }

        if (hasPasskey) {
            OutlinedButton(
                onClick = {
                    repository.forgetPasskey()
                    feedback = "Klucz dostępu został usunięty z aplikacji."
                    refresh()
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Usuń klucz dostępu")
            }
        }

        Text(status, fontSize = 11.sp)
        feedback?.let { Text(it, fontSize = 12.sp) }
    }
}

@Composable
private fun AutomaticBackupCard(info: com.libredisplay.data.repository.AppDataBackupRepository.AutomaticBackupInfo?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Automatyczna kopia zapasowa", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                "LibreCare trzyma dokładnie jeden plik kopii w danych aplikacji. Nie musisz wybierać " +
                    "miejsca, nazwy ani hasła. Kopia obejmuje osoby, które widzisz po zalogowaniu.",
                fontSize = 12.sp
            )
            if (info == null) {
                Text("Sprawdzanie stanu kopii…", fontSize = 12.sp)
                return@Column
            }
            if (!info.exists) {
                Text("Kopia nie została jeszcze utworzona.", fontSize = 12.sp)
                return@Column
            }
            Text("Osoby: ${info.persons} · odczyty: ${info.readings}", fontSize = 12.sp)
            Text("Rozmiar: ${formatBytes(info.sizeBytes)}", fontSize = 12.sp)
            Text("Ostatnia aktualizacja: ${formatTimestamp(info.lastModifiedEpochMillis)}", fontSize = 12.sp)
            Text("Plik: ${info.absolutePath}", fontSize = 11.sp)
        }
    }
}

@Composable
private fun RestorePlanDialog(
    staged: com.libredisplay.data.repository.AppDataBackupRepository.StagedRestore,
    onCancel: () -> Unit,
    onConfirm: (Set<String>, ConflictResolution, Boolean) -> Unit
) {
    val plan = staged.plan
    var selected by remember(plan) {
        mutableStateOf(plan.persons.map { it.patientId }.toSet())
    }
    var restoreConfiguration by remember(plan) { mutableStateOf(plan.settingsAvailable) }
    var resolution by remember(plan) { mutableStateOf(ConflictResolution.KEEP_LOCAL) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Co zostanie wczytane") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (plan.persons.isEmpty()) {
                    Text("Kopia nie zawiera danych osób.")
                }
                plan.persons.forEach { person ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = person.patientId in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + person.patientId else selected - person.patientId
                                }
                            )
                            Text(person.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Text(BackupMergeEngine.describePerson(person), fontSize = 12.sp)
                    }
                }

                if (plan.hasConflicts) {
                    HorizontalDivider()
                    Text(
                        "Część odczytów ma tę samą datę, ale inną wartość (${plan.totalConflicts}). " +
                            "Które dane zachować?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    plan.persons.flatMap { it.conflicts }.take(5).forEach { conflict ->
                        Text("• ${BackupMergeEngine.describeConflict(conflict)}", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = resolution == ConflictResolution.KEEP_LOCAL,
                            onClick = { resolution = ConflictResolution.KEEP_LOCAL }
                        )
                        Text("Zachowaj dane bieżące (z aplikacji)", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = resolution == ConflictResolution.KEEP_BACKUP,
                            onClick = { resolution = ConflictResolution.KEEP_BACKUP }
                        )
                        Text("Zachowaj dane z archiwum", fontSize = 13.sp)
                    }
                }

                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = restoreConfiguration,
                        onCheckedChange = { restoreConfiguration = it },
                        enabled = plan.settingsAvailable
                    )
                    Text("Przywróć także konfigurację aplikacji", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selected.isNotEmpty() || restoreConfiguration,
                onClick = { onConfirm(selected, resolution, restoreConfiguration) }
            ) { Text("Wczytaj") }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) { Text("Anuluj") }
        }
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} kB"
    else -> String.format(Locale("pl"), "%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun formatTimestamp(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis <= 0L) return "brak"
    return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale("pl"))
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))
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

