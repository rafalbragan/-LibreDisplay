package com.libredisplay.ui.monitoring

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.data.model.GlucoseHistoryStats
import com.libredisplay.data.model.GlucoseReading
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringScreen(
    refreshNonce: Int,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    viewModel: MonitoringViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(refreshNonce) {
        viewModel.onScreenVisible(refreshNonce)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LibreDisplay") },
                actions = {
                    IconButton(onClick = { viewModel.refreshNow() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Odśwież")
                    }
                    IconButton(onClick = onNavigateToDiagnostics) {
                        Icon(Icons.Default.BugReport, contentDescription = "Diagnostyka")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ustawienia")
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !state.isConfigured -> EmptyConfigurationState(onNavigateToSettings)
                state.isLoading && state.reading == null -> LoadingState()
                isLandscape -> MonitoringLandscape(state = state, viewModel = viewModel)
                else -> MonitoringPortrait(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun MonitoringPortrait(
    state: MonitoringUiState,
    viewModel: MonitoringViewModel
) {
    val reading = state.reading
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (reading != null) {
            CurrentGlucoseCard(reading, state.settings.targetLow, state.settings.targetHigh)
            FreshnessCard(reading.timestamp)
            StatsRow(
                stats = reading.stats,
                min12h = state.min12h,
                max12h = state.max12h,
                historyStatus = state.historyStatus
            )
            HistorySection(reading = reading, targetLow = state.settings.targetLow, targetHigh = state.settings.targetHigh)
        }
        ErrorPanel(
            errorMessage = state.errorMessage,
            canRetry = state.canRetry,
            cooldownSeconds = state.retryCooldownSecondsRemaining,
            viewModel = viewModel
        )
    }
}

@Composable
private fun MonitoringLandscape(
    state: MonitoringUiState,
    viewModel: MonitoringViewModel
) {
    val reading = state.reading
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (reading != null) {
                CurrentGlucoseCard(reading, state.settings.targetLow, state.settings.targetHigh)
                FreshnessCard(reading.timestamp)
                StatsColumn(
                    stats = reading.stats,
                    min12h = state.min12h,
                    max12h = state.max12h,
                    historyStatus = state.historyStatus
                )
            }
            ErrorPanel(
                errorMessage = state.errorMessage,
                canRetry = state.canRetry,
                cooldownSeconds = state.retryCooldownSecondsRemaining,
                viewModel = viewModel
            )
        }
        if (reading != null) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HistoryHeader(reading)
                    GlucoseChart(
                        points = reading.history,
                        targetLow = state.settings.targetLow,
                        targetHigh = state.settings.targetHigh,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentGlucoseCard(reading: GlucoseReading, targetLow: Int, targetHigh: Int) {
    val cardColor = when {
        reading.value < targetLow -> Color(0xFF7F1D1D)
        reading.value > targetHigh -> Color(0xFF7C2D12)
        else -> Color(0xFF14532D)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Aktualna glukoza", color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp)
            Text(
                text = "${reading.value} mg/dL",
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${reading.trend.arrow} ${reading.trend.description}",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FreshnessCard(timestamp: Instant) {
    val minutes = Duration.between(timestamp, Instant.now()).toMinutes().coerceAtLeast(0)
    val color = when {
        minutes <= 5 -> Color(0xFF166534)
        minutes <= 10 -> Color(0xFF9A3412)
        else -> Color(0xFF991B1B)
    }
    Card(colors = CardDefaults.cardColors(containerColor = color), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Dane sprzed $minutes min",
            modifier = Modifier.padding(18.dp),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatsRow(
    stats: GlucoseHistoryStats,
    min12h: String?,
    max12h: String?,
    historyStatus: HistoryStatus
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("MIN 12 h", min12h?.plus(" mg/dL") ?: "—")
            StatTile("MAX 12 h", max12h?.plus(" mg/dL") ?: "—")
        }
        StatTile("TIR", "${stats.timeInRangePercent}%")
        HistoryStatusText(historyStatus)
    }
}

@Composable
private fun StatsColumn(
    stats: GlucoseHistoryStats,
    min12h: String?,
    max12h: String?,
    historyStatus: HistoryStatus
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile("MIN 12 h", min12h?.plus(" mg/dL") ?: "—")
        StatTile("MAX 12 h", max12h?.plus(" mg/dL") ?: "—")
        StatTile("TIR", "${stats.timeInRangePercent}%")
        HistoryStatusText(historyStatus)
    }
}

@Composable
private fun HistoryStatusText(historyStatus: HistoryStatus) {
    when (historyStatus) {
        HistoryStatus.Loading -> Text("Wczytywanie historii 12 h...", color = Color(0xFFCBD5E1), fontSize = 14.sp)
        HistoryStatus.Available -> Unit
        HistoryStatus.Empty -> Text("Brak historii 12 h", color = Color(0xFFCBD5E1), fontSize = 14.sp)
        is HistoryStatus.Error -> Text(historyStatus.message, color = Color(0xFFFCA5A5), fontSize = 14.sp)
    }
}

@Composable
private fun StatTile(title: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Color(0xFF9CA3AF), fontSize = 16.sp)
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HistorySection(reading: GlucoseReading, targetLow: Int, targetHigh: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HistoryHeader(reading)
            if (reading.history.isNotEmpty()) {
                GlucoseChart(
                    points = reading.history,
                    targetLow = targetLow,
                    targetHigh = targetHigh,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text("Brak danych historycznych z endpointu graph.", color = Color.White)
            }
        }
    }
}

@Composable
private fun HistoryHeader(reading: GlucoseReading) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Historia glukozy - 12 godzin", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (reading.historyHoursAvailable in 0.1..11.9) {
            Text(
                "Dostępne dane: ${reading.historyHoursAvailable.roundToInt()} godzin",
                color = Color(0xFFCBD5E1),
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun ErrorPanel(errorMessage: String?, canRetry: Boolean, cooldownSeconds: Long, viewModel: MonitoringViewModel) {
    if (errorMessage == null) return
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3F1D1D)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(errorMessage, color = Color.White, fontSize = 18.sp)
            if (cooldownSeconds > 0) {
                Text(
                    "Kolejna probe mozna wykonac za ${formatCooldown(cooldownSeconds)}",
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (canRetry) {
                    Button(onClick = { viewModel.connectManually() }, enabled = cooldownSeconds <= 0) {
                        Text("Połącz z LibreLinkUp")
                    }
                }
                OutlinedButton(onClick = { viewModel.stopPolling() }) {
                    Text("Zatrzymaj")
                }
            }
            Text(
                "Aplikacja wykona tylko jedną próbę. Nie będzie automatycznie ponawiać logowania.",
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

private fun formatCooldown(seconds: Long): String {
    val minutes = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return "%02d:%02d".format(minutes, secs)
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text("Łączenie z LibreLinkUp…", fontSize = 20.sp)
        }
    }
}

@Composable
private fun EmptyConfigurationState(onNavigateToSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.padding(24.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Wprowadź dane LibreLinkUp albo włącz tryb mock.",
                    textAlign = TextAlign.Center,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Button(onClick = onNavigateToSettings) {
                    Text("Otwórz ustawienia")
                }
            }
        }
    }
}
