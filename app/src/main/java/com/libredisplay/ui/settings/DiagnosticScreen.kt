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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.libredisplay.data.repository.SettingsRepository
import com.libredisplay.diagnostics.DiagnosticLogger
import com.libredisplay.diagnostics.DiagnosticStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val snapshot by DiagnosticStatus.flow().collectAsState()
    val tokenDiagnostics = remember { SettingsRepository(context).tokenDiagnostics() }
    var logText by remember { mutableStateOf("") }

    fun refresh() {
        logText = DiagnosticLogger.readAll()
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostyka") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Wstecz") }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Login: ${snapshot.loginStatus}")
                    Text("Token: ${snapshot.tokenStatus}")
                    Text("Connections: ${snapshot.getConnectionsStatus}")
                    Text("Graph: ${snapshot.getLatestGraphStatus}")
                    Text("Polling: ${snapshot.pollingStatus}")
                    Text("HTTP: ${snapshot.lastHttpCode ?: "—"}")
                    Text("Ostrzeżenie: ${snapshot.lastWarning}")
                    Text("Ostatni endpoint: ${snapshot.lastEndpoint}")
                    Text("Ostatni błąd: ${snapshot.lastError}")
                    Text("Znaleziono zapisany token: ${if (tokenDiagnostics.tokenPresent) "tak" else "nie"}")
                    Text("Źródło tokena: ${tokenDiagnostics.source}")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { refresh() }, modifier = Modifier.weight(1f)) { Text("Pokaż log") }
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("LibreDisplayLog", logText))
                }, modifier = Modifier.weight(1f)) { Text("Kopiuj") }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    DiagnosticLogger.clear()
                    refresh()
                }, modifier = Modifier.weight(1f)) { Text("Wyczyść") }
                Button(onClick = {
                    DiagnosticLogger.createShareIntent(context)?.let {
                        context.startActivity(Intent.createChooser(it, "Udostępnij log"))
                    }
                }, modifier = Modifier.weight(1f)) { Text("Udostępnij") }
            }

            Text("Plik logu: ${DiagnosticLogger.logFilePath()}")
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (logText.isBlank()) "Brak wpisów diagnostycznych" else logText,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
