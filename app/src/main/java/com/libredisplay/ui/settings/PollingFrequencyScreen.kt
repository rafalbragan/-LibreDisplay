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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libredisplay.R

private val DashboardBackground = Color(0xFF101318)
private val DashboardSurface = Color(0xFF182033)
private val DashboardPrimaryText = Color(0xFFF3F6FA)
private val DashboardSecondaryText = Color(0xFFAAB3C2)
private val AccentGreen = Color(0xFF43C59E)

/**
 * Polling Frequency Settings screen.
 * Allows user to configure background polling interval.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollingFrequencyScreen(onNavigateBack: () -> Unit) {
    val pollingOptions = listOf(
        "5 minut" to "~10.5 MB / dzień",
        "10 minut" to "~5.2 MB / dzień",
        "15 minut" to "~3.5 MB / dzień",
        "30 minut" to "~1.7 MB / dzień",
        "60 minut" to "~0.9 MB / dzień"
    )

    val (selectedPolling, setSelectedPolling) = remember { mutableStateOf("15 minut") }

    Scaffold(
        containerColor = DashboardBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.polling_frequency_title)) },
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
                stringResource(R.string.polling_frequency_description),
                color = DashboardSecondaryText,
                fontSize = 13.sp
            )

            // Current Usage
            Card(
                colors = CardDefaults.cardColors(containerColor = DashboardSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.polling_current_usage),
                        color = DashboardSecondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "~2.1 MB / dzień",
                        color = DashboardPrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                "Opcje częstotliwości:",
                color = DashboardPrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                pollingOptions.forEach { (frequency, dataUsage) ->
                    PollingOption(
                        frequency = frequency,
                        dataUsage = dataUsage,
                        isSelected = frequency == selectedPolling,
                        onSelect = { setSelectedPolling(frequency) }
                    )
                }
            }

            // Warning
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2E18)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.polling_battery_warning),
                    color = Color(0xFFF8FAFC),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Text(
                "Szacunkowe zużycie po zmianie: ~2.8 MB / dzień",
                color = DashboardSecondaryText,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PollingOption(
    frequency: String,
    dataUsage: String,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    frequency,
                    color = DashboardPrimaryText,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    dataUsage,
                    color = DashboardSecondaryText,
                    fontSize = 11.sp
                )
            }
        }
    }
}


