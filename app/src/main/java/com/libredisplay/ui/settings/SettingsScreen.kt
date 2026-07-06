package com.libredisplay.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.diagnostics.DiagnosticLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val message by viewModel.message.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message == "Ustawienia zapisane") {
            viewModel.clearMessage()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ustawienia") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Konto LibreLinkUp", fontSize = 20.sp)
                    OutlinedTextField(
                        value = settings.email,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text("E-mail") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    OutlinedTextField(
                        value = settings.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text("Hasło") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Ukryj hasło" else "Pokaż hasło"
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = settings.regionMode,
                        onValueChange = { viewModel.onRegionModeChange(it.uppercase()) },
                        label = { Text("Region logowania (EU/GLOBAL/EU2/DE/US/FR/JP/AP/CUSTOM)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (settings.regionMode.equals("CUSTOM", ignoreCase = true)) {
                        OutlinedTextField(
                            value = settings.customBaseUrl,
                            onValueChange = viewModel::onCustomBaseUrlChange,
                            label = { Text("Wlasny URL API") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Zakres docelowy", fontSize = 20.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = settings.targetLow.toString(),
                            onValueChange = viewModel::onTargetLowChange,
                            label = { Text("Dolna granica") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = settings.targetHigh.toString(),
                            onValueChange = viewModel::onTargetHighChange,
                            label = { Text("Górna granica") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tryb mock")
                            Text("Działa bez realnego API i generuje 12h historii.", fontSize = 13.sp)
                        }
                        Switch(checked = settings.useMock, onCheckedChange = viewModel::onUseMockChange)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Diagnostyka", fontSize = 20.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onNavigateToDiagnostics, modifier = Modifier.weight(1f)) {
                            Text("Pokaż log")
                        }
                        OutlinedButton(onClick = { copyLog(context) }, modifier = Modifier.weight(1f)) {
                            Text("Kopiuj")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { DiagnosticLogger.clear() }, modifier = Modifier.weight(1f)) {
                            Text("Wyczyść")
                        }
                        Button(onClick = { shareLog(context) }, modifier = Modifier.weight(1f)) {
                            Text("Udostępnij")
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = viewModel::resetSession, modifier = Modifier.weight(1f)) {
                    Text("Wyczysc zapisany token i zaloguj ponownie")
                }
                Button(onClick = { viewModel.saveSettings() }, modifier = Modifier.weight(1f)) {
                    Text("Zapisz")
                }
            }
        }
    }
}

private fun copyLog(context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("LibreDisplayLog", DiagnosticLogger.readAll()))
}

private fun shareLog(context: Context) {
    DiagnosticLogger.createShareIntent(context)?.let {
        context.startActivity(Intent.createChooser(it, "Udostępnij log"))
    }
}
