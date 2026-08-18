package com.libredisplay.ui.monitoring

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Badge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.R
import com.libredisplay.analytics.GlucoseMetricsCalculator
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.LibreConnectionPerson
import java.time.Duration
import java.time.Instant

private val DashboardBackground = Color(0xFF101318)
private val DashboardSurface = Color(0xFF182033)
private val DashboardElevatedSurface = Color(0xFF202A3D)
private val DashboardPrimaryText = Color(0xFFF3F6FA)
private val DashboardSecondaryText = Color(0xFFAAB3C2)
private val DashboardMutedText = Color(0xFF7F8A9A)
private val AccentGreen = Color(0xFF43C59E)
private val AccentWarning = Color(0xFFF2B84B)
private val AccentRed = Color(0xFFE05A6A)
private val AccentCritical = Color(0xFFB8324A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringScreen(
    refreshNonce: Int,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToAnalytics: () -> Unit = onNavigateToDiagnostics,
    onSwitchToLiveMode: () -> Unit,
    viewModel: MonitoringViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var showFullScreenHistory by remember { mutableStateOf(false) }
    var showSwitchToLiveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(refreshNonce) {
        viewModel.onScreenVisible(refreshNonce)
    }

    if (showFullScreenHistory) {
        FullScreenGlucoseChartScreen(
            onNavigateBack = { showFullScreenHistory = false },
            viewModel = viewModel
        )
        return
    }

    Scaffold(
        containerColor = DashboardBackground,
        topBar = {
            DashboardTopBar(
                persons = state.availablePersons,
                selectedPatientId = state.selectedPatientId,
                selectedName = state.selectedPersonName,
                connectionState = state.connectionState,
                isDemoMode = state.isDemoMode,
                onSelectPatient = viewModel::onPersonSelected,
                onRefresh = viewModel::refreshNow,
                onOpenAnalytics = onNavigateToAnalytics,
                onOpenSettings = onNavigateToSettings
            )
        }
    ) { padding ->
        Surface(
            color = DashboardBackground,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !state.isConfigured -> EmptyConfigurationState(onNavigateToSettings)
                state.isLoading && state.reading == null -> LoadingState()
                else -> {
                    val reading = state.reading
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                    if (isLandscape) {
                        Row(
                            modifier = contentModifier,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (state.isDemoMode) {
                                    DemoModeBanner(onSwitchToLiveMode = { showSwitchToLiveDialog = true })
                                }
                                SelectedPersonHeader(state.selectedPersonFullName ?: state.selectedPersonName)
                                if (reading != null) {
                                    CurrentGlucoseHeroCard(reading, state.settings.targetLow, state.settings.targetHigh)
                                    RangeTimeSummaryRow(reading, state.settings.targetLow, state.settings.targetHigh)
                                    HbA1cGmiAndSensorRow(state = state, reading = reading)
                                    NfzStatusCompactCard(state = state, reading = reading)
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                GlucoseChartCard(
                                    state = state,
                                    reading = reading,
                                    onOpenHistory = { showFullScreenHistory = true }
                                )
                                LastSyncFooter(state.lastSuccessfulFetchAt)
                                ErrorPanel(state.errorMessage, state.canRetry, state.retryCooldownSecondsRemaining, viewModel)
                            }
                        }
                    } else {
                        Column(modifier = contentModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.isDemoMode) {
                                DemoModeBanner(onSwitchToLiveMode = { showSwitchToLiveDialog = true })
                            }
                            // New compact header + person switcher
                            CompactPersonHeader(state.selectedPersonFirstName, state.selectedPersonLastName, state.isDemoMode)
                            VisiblePersonSwitcher(
                                persons = state.availablePersons,
                                selectedPatientId = state.selectedPatientId,
                                onPersonSelected = viewModel::onPersonSelected
                            )
                            TimeRangeDisplay(state.timeRange)

                            if (reading != null) {
                                CurrentGlucoseHeroCard(reading, state.settings.targetLow, state.settings.targetHigh)
                                RangeTimeSummaryRow(reading, state.settings.targetLow, state.settings.targetHigh)
                                HbA1cGmiAndSensorRow(state = state, reading = reading)
                                GlucoseChartCard(
                                    state = state,
                                    reading = reading,
                                    onOpenHistory = { showFullScreenHistory = true }
                                )
                                NfzStatusCompactCard(state = state, reading = reading)
                                LastSyncFooter(state.lastSuccessfulFetchAt)
                            } else {
                                EmptyChartState()
                            }
                            ErrorPanel(state.errorMessage, state.canRetry, state.retryCooldownSecondsRemaining, viewModel)
                        }
                    }
                }
            }
        }
    }

    if (showSwitchToLiveDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchToLiveDialog = false },
            title = { Text("Przejść do trybu Live?") },
            text = {
                Text(
                    "Tryb Live połączy aplikację z kontem LibreLinkUp. Jeśli nie masz zapisanych danych logowania, poprosimy o ich wprowadzenie."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSwitchToLiveDialog = false
                    onSwitchToLiveMode()
                }) {
                    Text("Przejdź do Live")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchToLiveDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
private fun SelectedPersonHeader(selectedName: String?) {
    val fullName = selectedName?.trim().takeIf { !it.isNullOrBlank() } ?: return
    Card(
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text("Monitoring", color = DashboardSecondaryText, fontSize = 12.sp)
            Text(
                text = fullName,
                color = DashboardPrimaryText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    persons: List<LibreConnectionPerson>,
    selectedPatientId: String?,
    selectedName: String?,
    connectionState: ConnectionState,
    isDemoMode: Boolean,
    onSelectPatient: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold, color = DashboardPrimaryText)
                if (isDemoMode) {
                    Badge(containerColor = AccentWarning.copy(alpha = 0.24f), contentColor = AccentWarning) {
                        Text("DEMO", fontSize = 10.sp)
                    }
                }
                PatientSelectorChip(
                    persons = persons,
                    selectedPatientId = selectedPatientId,
                    selectedName = selectedName,
                    onSelected = onSelectPatient
                )
            }
        },
        actions = {
            ConnectionStatusDot(connectionState)
            IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Odśwież dane", tint = DashboardPrimaryText)
            }
            IconButton(onClick = onOpenAnalytics, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Analytics, contentDescription = "Analiza", tint = DashboardPrimaryText)
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Ustawienia", tint = DashboardPrimaryText)
            }
        }
    )
}

@Composable
private fun DemoModeBanner(onSwitchToLiveMode: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2E18)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Tryb demo", color = Color(0xFFFDE68A), fontWeight = FontWeight.Bold)
            Text(
                "Tryb demo używa przykładowych danych. Nie używaj ich do podejmowania decyzji medycznych.",
                color = Color(0xFFF8FAFC),
                fontSize = 12.sp
            )
            OutlinedButton(onClick = onSwitchToLiveMode, modifier = Modifier.fillMaxWidth()) {
                Text("Przełącz na tryb Live")
            }
        }
    }
}

@Composable
private fun PatientSelectorChip(
    persons: List<LibreConnectionPerson>,
    selectedPatientId: String?,
    selectedName: String?,
    onSelected: (String) -> Unit
) {
    val visible = persons.take(3)
    if (visible.isEmpty()) return
    val selected = visible.firstOrNull { it.patientId == selectedPatientId } ?: visible.first()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.semantics { contentDescription = "Wybrana osoba: ${selected.displayName}" }) {
        OutlinedButton(onClick = { if (visible.size > 1) expanded = true }, shape = RoundedCornerShape(14.dp)) {
            Text(
                text = selectedName?.takeIf { it.isNotBlank() } ?: selected.displayName,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (visible.size > 1) {
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Rozwiń listę pacjentów")
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            visible.forEach { person ->
                DropdownMenuItem(
                    text = {
                        val mark = if (person.patientId == selected.patientId) "✓ " else ""
                        Text(mark + person.displayName)
                    },
                    onClick = {
                        expanded = false
                        if (person.patientId != selected.patientId) onSelected(person.patientId)
                    }
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusDot(connectionState: ConnectionState) {
    val (label, color) = when (connectionState) {
        ConnectionState.Connected -> "Połączono" to AccentGreen
        ConnectionState.Connecting -> "Łączenie" to AccentWarning
        else -> "Offline" to AccentRed
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 4.dp).semantics { contentDescription = "Status połączenia: $label" }
    ) {
        Text("●", color = color, fontSize = 12.sp)
    }
}

@Composable
private fun CurrentGlucoseHeroCard(reading: GlucoseReading, targetLow: Int, targetHigh: Int) {
    val duration = Duration.between(reading.timestamp, Instant.now())
    val stale = duration.toMinutes() > 30
    val statusText = glucoseRangeStatus(reading.value, targetLow, targetHigh)

    Card(
        colors = CardDefaults.cardColors(containerColor = if (stale) DashboardElevatedSurface else DashboardSurface),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Aktualna glikemia ${reading.value} mg na decylitr, ${trendContentDescription(reading.trend)}" }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            val type = glucoseCardTypographyForWidth(maxWidth.value.toInt())
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Aktualna glikemia", color = DashboardSecondaryText, fontSize = 18.sp)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = reading.value.toString(),
                            color = DashboardPrimaryText,
                            fontSize = type.glucoseValueSp.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "mg/dL",
                            color = DashboardPrimaryText,
                            fontSize = type.glucoseUnitSp.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    TrendBlock(reading.trend.arrow, reading.trend.description, type)
                }
                Text(
                    text = formatReadingAge(duration),
                    color = DashboardSecondaryText,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    MetricStatusBadge(
                        text = statusText,
                        color = when (statusText) {
                            "W zakresie" -> AccentGreen
                            "Poniżej zakresu" -> AccentRed
                            "Krytycznie nisko" -> AccentCritical
                            "Bardzo wysoko" -> AccentCritical
                            else -> AccentWarning
                        }
                    )
                    if (stale) {
                        DataFreshnessBadge(text = "Dane nieaktualne")
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendBlock(arrow: String, label: String, typography: DashboardTypography) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.semantics { contentDescription = "Trend: $label" }
    ) {
        Text(arrow, color = DashboardPrimaryText, fontSize = typography.trendArrowSp.sp, fontWeight = FontWeight.Bold)
        Text(label, color = DashboardSecondaryText, fontSize = typography.trendDescriptionSp.sp, maxLines = 1)
    }
}

@Composable
private fun RangeTimeSummaryRow(reading: GlucoseReading, targetLow: Int, targetHigh: Int) {
    val history = reading.history + listOf(
        com.libredisplay.data.model.GlucoseHistoryPoint(
            value = reading.value,
            timestamp = reading.timestamp,
            trend = reading.trend
        )
    )
    val distribution = rangeDistributionFromHistory(history, targetLow, targetHigh)
    val below = rangeTileUi(distribution?.belowRangePercent, distribution?.belowRangeDuration)
    val inRange = rangeTileUi(distribution?.inRangePercent, distribution?.inRangeDuration)
    val above = rangeTileUi(distribution?.aboveRangePercent, distribution?.aboveRangeDuration)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RangeTimeCard("Poniżej", below, AccentRed, Modifier.weight(1f))
        RangeTimeCard("W zakresie", inRange, AccentGreen, Modifier.weight(1f), emphasized = true)
        RangeTimeCard("Powyżej", above, AccentWarning, Modifier.weight(1f))
    }
}

@Composable
private fun RangeTimeCard(
    title: String,
    data: RangeTileUi,
    accent: Color,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (emphasized) DashboardElevatedSurface else DashboardSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = DashboardSecondaryText, fontSize = 12.sp)
            Text(data.percentLabel, color = DashboardPrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(data.durationLabel, color = accent, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HbA1cGmiAndSensorRow(state: MonitoringUiState, reading: GlucoseReading) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HbA1cGmiCard(state, reading, Modifier.weight(1f))
        SensorActivityCard(reading, Modifier.weight(1f))
    }
}

@Composable
private fun HbA1cGmiCard(state: MonitoringUiState, reading: GlucoseReading, modifier: Modifier = Modifier) {
    val kpi = buildHbA1cKpiModel(
        hba1cSettings = com.libredisplay.data.model.HbA1cSettings(
            patientId = state.selectedPatientId,
            labHbA1cPercent = state.labHbA1cPercent,
            labHbA1cDate = state.labHbA1cDate,
            targetHbA1cPercent = state.targetHbA1cPercent
        ),
        historyPoints = reading.history
    )

    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = DashboardSurface), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (kpi.mode) {
                HbA1cKpiMode.LAB_HBA1C -> {
                    Text("HbA1c", color = DashboardSecondaryText, fontSize = 12.sp)
                    Text("${"%.1f".format(kpi.valuePercent)}%", color = DashboardPrimaryText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Laboratoryjne · ${state.labHbA1cDate ?: "brak daty"}", color = DashboardMutedText, fontSize = 12.sp)
                    Text("Cel: < ${state.targetHbA1cPercent}%", color = DashboardSecondaryText, fontSize = 12.sp)
                }
                HbA1cKpiMode.GMI_ESTIMATED -> {
                    Text("GMI", color = DashboardSecondaryText, fontSize = 12.sp)
                    Text("${"%.1f".format(kpi.valuePercent)}%", color = DashboardPrimaryText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Szacowane z sensora", color = DashboardMutedText, fontSize = 12.sp)
                    Text("Nie zastępuje HbA1c", color = DashboardMutedText, fontSize = 12.sp)
                }
                HbA1cKpiMode.GMI_INSUFFICIENT -> {
                    Text("GMI", color = DashboardSecondaryText, fontSize = 12.sp)
                    Text("—", color = DashboardPrimaryText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Za mało danych", color = DashboardMutedText, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SensorActivityCard(reading: GlucoseReading, modifier: Modifier = Modifier) {
    val end = Instant.now()
    val start = end.minus(Duration.ofDays(14))
    val activity = sensorActivityFromHistory(reading.history, start, end)
    val value = activity?.activityPercent

    val color = when {
        value == null -> DashboardMutedText
        value >= 75.0 -> AccentGreen
        value >= 65.0 -> AccentWarning
        else -> AccentRed
    }

    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = DashboardSurface), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Aktywność czujnika", color = DashboardSecondaryText, fontSize = 12.sp)
            Text(value?.let { "${"%.0f".format(it)}%" } ?: "—", color = DashboardPrimaryText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(if (value == null) "Za mało danych" else "Cel: ≥ 75%", color = color, fontSize = 12.sp)
            Text("Ostatnie 14 dni", color = DashboardMutedText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NfzStatusCompactCard(state: MonitoringUiState, reading: GlucoseReading) {
    val distribution = rangeDistributionFromHistory(reading.history, state.settings.targetLow, state.settings.targetHigh)
    val activity = sensorActivityFromHistory(reading.history, Instant.now().minus(Duration.ofDays(14)), Instant.now())
    val nfz = evaluateNfzStatus(
        sensorActivityPercent = activity?.activityPercent,
        tirPercent = distribution?.inRangePercent,
        labHbA1cPercent = state.labHbA1cPercent
    )

    val color = when (nfz.status) {
        NfzStatus.GREEN -> AccentGreen
        NfzStatus.YELLOW -> AccentWarning
        NfzStatus.RED -> AccentRed
        NfzStatus.GRAY -> DashboardMutedText
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Status NFZ: ${nfz.headline}" }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Refundacja NFZ", color = DashboardSecondaryText, fontSize = 12.sp)
            Text(nfz.headline, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Status informacyjny", color = DashboardMutedText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GlucoseChartCard(state: MonitoringUiState, reading: GlucoseReading?, onOpenHistory: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenHistory() }
            .semantics { contentDescription = "Wykres historii glukozy. Dotknij aby powiększyć" }
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Historia glukozy", color = DashboardPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ShowChart, contentDescription = "Powiększyć wykres", tint = DashboardSecondaryText)
            }
            Text("Dotknij, aby powiększyć", color = DashboardMutedText, fontSize = 12.sp)
            if (reading == null || reading.history.isEmpty()) {
                EmptyChartState()
            } else {
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

@Composable
private fun EmptyChartState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(DashboardElevatedSurface, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.ShowChart, contentDescription = null, tint = DashboardMutedText)
        Spacer(Modifier.height(8.dp))
        Text("Brak danych 12 h", color = DashboardPrimaryText, fontWeight = FontWeight.SemiBold)
        Text("Nie mam jeszcze lokalnej historii dla tego zakresu.", color = DashboardSecondaryText, fontSize = 12.sp)
        Text("Odśwież dane albo poczekaj na synchronizację.", color = DashboardSecondaryText, fontSize = 12.sp)
    }
}

@Composable
private fun MetricStatusBadge(text: String, color: Color) {
    Badge(containerColor = color.copy(alpha = 0.18f), contentColor = color) {
        Text(text, fontSize = 10.sp)
    }
}

@Composable
private fun DataFreshnessBadge(text: String) {
    Badge(containerColor = AccentRed.copy(alpha = 0.24f), contentColor = AccentRed) {
        Text(text, fontSize = 10.sp)
    }
}

@Composable
private fun LastSyncFooter(lastSuccessfulFetchAt: Instant?) {
    val label = lastSuccessfulFetchAt?.let {
        "Ostatnia synchronizacja: ${DateTimeLabel.format(it)}"
    } ?: "Brak informacji o synchronizacji"

    Text(
        text = label,
        color = DashboardMutedText,
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }
    )
}

@Composable
private fun ErrorPanel(errorMessage: String?, canRetry: Boolean, cooldownSeconds: Long, viewModel: MonitoringViewModel) {
    if (errorMessage == null) return
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2024)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(errorMessage, color = DashboardPrimaryText, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.connectManually() }, enabled = canRetry && cooldownSeconds <= 0) {
                    Text("Połącz")
                }
                OutlinedButton(onClick = { viewModel.stopPolling() }) {
                    Text("Zatrzymaj")
                }
            }
        }
    }
}

private object DateTimeLabel {
    private val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    fun format(instant: Instant): String = formatter.withZone(java.time.ZoneId.systemDefault()).format(instant)
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text("Łączenie z LibreLinkUp…", fontSize = 18.sp, color = DashboardPrimaryText)
        }
    }
}

@Composable
private fun EmptyConfigurationState(onNavigateToSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.padding(24.dp), colors = CardDefaults.cardColors(containerColor = DashboardSurface)) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Wprowadź dane LibreLinkUp albo uruchom tryb demo.", color = DashboardPrimaryText)
                OutlinedButton(onClick = onNavigateToSettings) {
                    Text("Otwórz ustawienia")
                }
            }
        }
    }
}
