package com.libredisplay.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.ui.monitoring.DashboardNavItem
import com.libredisplay.ui.monitoring.TopLevelNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
    onOpenHome: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onNavigateToMonitoring: () -> Unit,
    onNavigateToMetricSettings: () -> Unit,
    onNavigateToHbA1cSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToDataPrivacy: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToRetention: () -> Unit,
    onNavigateToPolling: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val hba1c by viewModel.hba1cSettings.collectAsState()
    val quickOrder by viewModel.quickMetricsOrder.collectAsState()

    val accountSummary = settings.email.takeIf { it.isNotBlank() }?.let { email ->
        val parts = email.split("@")
        if (parts.size == 2 && parts[0].length >= 3) "Połączono · ${parts[0].take(3)}***@${parts[1]}" else "Połączono"
    } ?: "Brak połączenia"
    val hba1cSummary = hba1c.labHbA1cPercent?.let { value ->
        val date = hba1c.labHbA1cDate?.toString() ?: "brak daty"
        "${"%.1f".format(value).replace('.', ',')}% · $date"
    } ?: "Brak wyniku laboratoryjnego"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ustawienia") },
                navigationIcon = if (showBackButton) {
                    {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                        }
                    }
                } else {
                    {}
                }
            )
        },
        bottomBar = {
            TopLevelNavigationBar(
                selected = DashboardNavItem.USTAWIENIA,
                onOpenHome = onOpenHome,
                onOpenHistory = onOpenHistory,
                onOpenSettings = {}
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSectionHeader("MONITOROWANIE")
            SettingsRow(
                title = "Zakres docelowy",
                subtitle = "${settings.targetLow}-${settings.targetHigh} mg/dL",
                onClick = onNavigateToMonitoring
            )
            SettingsRow(
                title = "Metryki ekranu głównego",
                subtitle = "${quickOrder.size} wybranych",
                onClick = onNavigateToMetricSettings
            )
            SettingsRow(
                title = "Synchronizacja",
                subtitle = "Co ${settings.backgroundPollingMinutes} min",
                onClick = onNavigateToPolling
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionHeader("DANE ZDROWOTNE")
            SettingsRow(
                title = "HbA1c",
                subtitle = hba1cSummary,
                onClick = onNavigateToHbA1cSettings
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionHeader("KONTO I POŁĄCZENIE")
            SettingsRow(
                title = "LibreLinkUp",
                subtitle = accountSummary,
                onClick = onNavigateToAccount
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionHeader("DANE I PRYWATNOŚĆ")
            SettingsRow(
                title = "Prywatność i dane",
                subtitle = "Eksport i usuwanie danych",
                onClick = onNavigateToDataPrivacy
            )
            SettingsRow(
                title = "Informacje i statystyki",
                subtitle = "Zakres danych i wypelnienie osob",
                onClick = onNavigateToStatistics
            )
            SettingsRow(
                title = "Retencja danych",
                subtitle = formatRetentionSummary(settings.retentionHours),
                onClick = onNavigateToRetention
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionHeader("APLIKACJA")
            SettingsRow(
                title = "O aplikacji",
                subtitle = "Wersja i informacje",
                onClick = onNavigateToAppearance
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionHeader("ZAAWANSOWANE")
            SettingsRow(
                title = "Diagnostyka",
                subtitle = "Logi i narzędzia",
                onClick = onNavigateToAdvanced
            )
        }
    }
}

private fun formatRetentionSummary(retentionHours: Int): String {
    val days = retentionHours / 24
    return when {
        days >= 365 -> {
            val years = days / 365
            when (years) {
                1 -> "1 rok"
                in 2..4 -> "$years lata"
                else -> "$years lat"
            }
        }
        days >= 30 -> "${days / 30} mies."
        else -> "$days dni"
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF0EA5A4)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = Color.White)
            Text(subtitle, fontSize = 13.sp, color = Color(0xFF94A3B8))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B))
    }
}
