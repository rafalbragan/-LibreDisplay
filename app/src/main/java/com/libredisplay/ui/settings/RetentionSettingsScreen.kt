package com.libredisplay.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.R
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private val DashboardBackground = Color(0xFF101318)
private val DashboardSurface = Color(0xFF182033)
private val DashboardPrimaryText = Color(0xFFF3F6FA)
private val DashboardSecondaryText = Color(0xFFAAB3C2)
private val AccentGreen = Color(0xFF43C59E)

/**
 * Data Retention Settings screen.
 * Allows user to choose how long local data is kept.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetentionSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: RetentionSettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showOptionDialog by remember { mutableStateOf(false) }

    if (state.confirmRequired && state.pendingHours != null) {
        val pendingLabel = state.options.firstOrNull { it.hours == state.pendingHours }?.label ?: state.pendingHours.toString()
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmation() },
            title = { Text("Potwierdzenie zmiany retencji") },
            text = {
                Text(
                    "Zmiana retencji na $pendingLabel usunie starsze lokalne odczyty z tego urzadzenia. Nie usuwa danych z LibreLinkUp.\n\n" +
                    "Czy na pewno chcesz kontynuować?"
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.applyPendingRetention() }) {
                    Text("Potwierdz")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmation() }) {
                    Text("Anuluj")
                }
            }
        )
    }

    if (showOptionDialog) {
        AlertDialog(
            onDismissRequest = { showOptionDialog = false },
            title = { Text("Przechowuj dane przez") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    state.options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(option.label, modifier = Modifier.weight(1f))
                            if (option.hours == state.selectedHours) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreen)
                            }
                            TextButton(onClick = {
                                showOptionDialog = false
                                viewModel.requestRetentionChange(option.hours)
                            }) {
                                Text("Wybierz")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOptionDialog = false }) { Text("Zamknij") }
            }
        )
    }

    Scaffold(
        containerColor = DashboardBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.retention_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DashboardSurface,
                    titleContentColor = DashboardPrimaryText,
                    navigationIconContentColor = DashboardPrimaryText
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.retention_description),
                color = DashboardSecondaryText,
                fontSize = 13.sp
            )
            OutlinedButton(onClick = { showOptionDialog = true }, modifier = Modifier.fillMaxWidth()) {
                val selectedLabel = state.options.firstOrNull { it.hours == state.selectedHours }?.label ?: "-"
                Text("$selectedLabel")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Szacowany rozmiar bazy po zmianie: ${state.estimatedSizeLabel}",
                color = DashboardSecondaryText,
                fontSize = 12.sp
            )
            Text(
                "Szacowana liczba odczytow: ${state.estimatedReadingsLabel}",
                color = DashboardSecondaryText,
                fontSize = 12.sp
            )
        }
    }
}





