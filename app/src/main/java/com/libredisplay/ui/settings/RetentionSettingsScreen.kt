package com.libredisplay.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.R

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.retention_description),
                color = DashboardSecondaryText,
                fontSize = 13.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                state.options.forEach { option ->
                    RetentionOption(
                        label = option.label,
                        isSelected = option.hours == state.selectedHours,
                        onSelect = {
                            viewModel.requestRetentionChange(option.hours)
                        }
                    )
                }
            }

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

@Composable
private fun RetentionOption(
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) DashboardSurface.copy(alpha = 0.8f) else DashboardSurface,
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect,
                role = Role.RadioButton
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AccentGreen,
                    unselectedColor = DashboardSecondaryText
                )
            )
            Text(
                label,
                color = DashboardPrimaryText,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}




