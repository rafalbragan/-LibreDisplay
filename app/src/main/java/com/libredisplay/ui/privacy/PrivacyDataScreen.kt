package com.libredisplay.ui.privacy

import android.content.Intent
import android.net.Uri
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
    ResetAppData,
    DeleteDemoData
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDataScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStart: () -> Unit,
    viewModel: PrivacyDataViewModel = viewModel()
) {
    val context = LocalContext.current
    val event by viewModel.event.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingAction by remember { mutableStateOf<PrivacyAction?>(null) }

    LaunchedEffect(event) {
        val value = event ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(value.message)
        if (value.navigateToStart) {
            onNavigateToStart()
        }
        viewModel.consumeEvent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Data") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "LibreCare stores glucose history locally on this device.\n\n" +
                    "Stored data may include:\n" +
                    "- monitored person name\n" +
                    "- monitored person identifier\n" +
                    "- glucose readings\n" +
                    "- glucose trends\n" +
                    "- reading timestamps\n" +
                    "- selected monitored person\n" +
                    "- local app settings\n" +
                    "- authentication/session data needed to connect to LibreLinkUp\n\n" +
                    "LibreCare does not sell your data.\n\n" +
                    "LibreCare is not a medical device and does not provide medical advice, diagnosis, treatment recommendations or emergency alerts.",
                fontSize = 14.sp
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
                Text("Privacy Policy")
            }

            if (viewModel.isDemoMode) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { pendingAction = PrivacyAction.DeleteDemoData }
                ) {
                    Text("Delete demo data")
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { pendingAction = PrivacyAction.DeleteMyStoredData }
            ) { Text("Delete my stored data") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { pendingAction = PrivacyAction.DeleteLocalGlucoseHistory }
            ) { Text("Delete local glucose history") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { pendingAction = PrivacyAction.DeleteMonitoredPeople }
            ) { Text("Delete monitored people") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { pendingAction = PrivacyAction.DisconnectAccount }
            ) { Text("Disconnect LibreLinkUp account") }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { pendingAction = PrivacyAction.ClearSessionData }
            ) { Text("Clear session data") }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { pendingAction = PrivacyAction.ResetAppData }
            ) { Text("Reset app data") }
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
                        PrivacyAction.ResetAppData -> viewModel.resetAppData()
                        PrivacyAction.DeleteDemoData -> viewModel.deleteDemoData()
                    }
                }) {
                    Text(dialog.confirm)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingAction = null }) {
                    Text("Cancel")
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
            title = "Delete my stored data?",
            message = "This will delete locally stored LibreCare data from this device. This does not delete data from LibreLinkUp.",
            confirm = "Delete"
        )
        PrivacyAction.DeleteLocalGlucoseHistory -> ConfirmationDialogModel(
            title = "Delete local glucose history?",
            message = "This will remove local glucose readings from this device and keep account settings.",
            confirm = "Delete"
        )
        PrivacyAction.DeleteMonitoredPeople -> ConfirmationDialogModel(
            title = "Delete monitored people?",
            message = "This will remove locally stored monitored people and selected person.",
            confirm = "Delete"
        )
        PrivacyAction.DisconnectAccount -> ConfirmationDialogModel(
            title = "Disconnect LibreLinkUp account?",
            message = "This will clear session tokens and account connection state on this device.",
            confirm = "Disconnect"
        )
        PrivacyAction.ClearSessionData -> ConfirmationDialogModel(
            title = "Clear session data?",
            message = "This clears stored session/authentication tokens and keeps local history.",
            confirm = "Clear"
        )
        PrivacyAction.ResetAppData -> ConfirmationDialogModel(
            title = "Reset LibreCare?",
            message = "This will remove local history, monitored people, selected person, session data and app settings from this device. This action cannot be undone.",
            confirm = "Reset"
        )
        PrivacyAction.DeleteDemoData -> ConfirmationDialogModel(
            title = "Delete demo data?",
            message = "This will remove simulated demo people and demo glucose history from this device.",
            confirm = "Delete"
        )
    }
}

