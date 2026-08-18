package com.libredisplay.ui.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
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
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.ui.theme.LibreCareColors
import java.time.Duration
import java.time.Instant

private val DashboardBackground = LibreCareColors.Background
private val DashboardSurface = LibreCareColors.Surface
private val DashboardElevatedSurface = LibreCareColors.SurfaceElevated
private val DashboardPrimaryText = LibreCareColors.TextPrimary
private val DashboardSecondaryText = LibreCareColors.TextSecondary
private val DashboardMutedText = LibreCareColors.TextMuted
private val AccentGreen = LibreCareColors.AccentTeal
private val AccentWarning = LibreCareColors.AccentAmber
private val AccentRed = LibreCareColors.AccentRed
private val AccentCritical = LibreCareColors.AccentPurple

private enum class DashboardNavItem(val label: String) {
    GLOWNA("Główna"),
    HISTORIA("Historia"),
    DODAJ("Dodaj"),
    ALARMY("Alarmy"),
    WIECEJ("Więcej")
}

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
    var historyContext by remember { mutableStateOf<HistoryOpenContext?>(null) }
    var showSwitchToLiveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(refreshNonce) {
        viewModel.onScreenVisible(refreshNonce)
    }

    val openHistory: () -> Unit = {
        when (val action = chartClickAction(state)) {
            is MonitoringAction.OpenHistory -> historyContext = action.context
        }
    }

    if (historyContext != null) {
        FullScreenGlucoseChartScreen(
            onNavigateBack = { historyContext = null },
            viewModel = viewModel,
            initialRange = historyContext!!.timeRange.toHistoryTimeRange()
        )
        return
    }

    Scaffold(
        containerColor = DashboardBackground,
        topBar = {
            DashboardTopBar(
                connectionState = state.connectionState,
                isDemoMode = state.isDemoMode,
                onRefresh = viewModel::refreshNow,
                onOpenHistory = openHistory,
                onOpenSettings = onNavigateToSettings
            )
        },
        bottomBar = {
            DashboardBottomNavigation(
                onOpenHistory = openHistory,
                onOpenMore = onNavigateToSettings
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
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                    Column(modifier = contentModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.isDemoMode) {
                            DemoModeBanner(onSwitchToLiveMode = { showSwitchToLiveDialog = true })
                        }

                        // Single identity area: selected person appears only in this switcher.
                        CompactPersonSwitcherBar(
                            persons = state.availablePersons,
                            selectedPatientId = state.selectedPatientId,
                            onPersonSelected = viewModel::onPersonSelected,
                            isDemoMode = state.isDemoMode
                        )

                        TimeRangeDisplay(
                            timeRange = state.timeRange,
                            latestReadingAt = reading?.timestamp ?: state.lastMeasurementTimestamp,
                            onChangeClick = openHistory
                        )

                        if (reading != null) {
                            CurrentGlucoseHeroCard(reading, state.settings.targetLow, state.settings.targetHigh)
                            CurrentGlucoseWarningCard(reading, state.settings.targetLow, state.settings.targetHigh)
                            DashboardMetricTilesRow(state = state, reading = reading)
                            GlucoseChartCard(
                                state = state,
                                reading = reading,
                                onOpenHistory = openHistory
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    connectionState: ConnectionState,
    isDemoMode: Boolean,
    onRefresh: () -> Unit,
    onOpenHistory: () -> Unit,
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
            }
        },
        actions = {
            ConnectionStatusDot(connectionState)
            IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Odśwież dane", tint = DashboardPrimaryText)
            }
            IconButton(onClick = onOpenHistory, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ShowChart, contentDescription = "Historia glikemii", tint = DashboardPrimaryText)
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
    val now = Instant.now()
    val duration = Duration.between(reading.timestamp, now)
    val presentation = buildGlucoseStatusPresentation(
        reading = reading,
        now = now,
        config = GlucoseWarningConfig(targetLowMgDl = targetLow, targetHighMgDl = targetHigh)
    )
    val freshnessWarning = presentation.freshnessWarning
    val trend = trendPresentation(reading.trend)
    val glucoseColor = warningToneColor((presentation.medicalWarning ?: presentation.primary).tone)

    Card(
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Aktualna glikemia ${reading.value} mg na decylitr, ${trendContentDescription(reading.trend)}" }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
            val type = glucoseCardTypographyForWidth(maxWidth.value.toInt())
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Aktualna glikemia", color = DashboardSecondaryText, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = reading.value.toString(),
                            color = glucoseColor,
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
                    TrendBlock(trend.arrow, trend.label, trend.color, type)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = PolishDateTimeFormatter.formatUserFacing(reading.timestamp),
                        color = DashboardSecondaryText,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(trend.label, color = trend.color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatReadingAge(duration),
                        color = DashboardMutedText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    SensorRemainingPill()
                    if (freshnessWarning != null) DataFreshnessBadge(freshnessWarning.title)
                }
            }
        }
    }
}

@Composable
private fun CurrentGlucoseWarningCard(reading: GlucoseReading, targetLow: Int, targetHigh: Int) {
    val presentation = buildGlucoseStatusPresentation(
        reading = reading,
        now = Instant.now(),
        config = GlucoseWarningConfig(targetLowMgDl = targetLow, targetHighMgDl = targetHigh)
    )
    val primary = presentation.primary
    if (primary.level == GlucoseWarningLevel.IN_RANGE) return

    Card(
        colors = CardDefaults.cardColors(containerColor = warningToneColor(primary.tone).copy(alpha = 0.15f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(warningIcon(primary.iconName), contentDescription = primary.title, tint = warningToneColor(primary.tone))
                Text(primary.title, color = warningToneColor(primary.tone), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text(primary.message, color = DashboardPrimaryText, fontSize = 13.sp)
        }
    }
}

@Composable
private fun TrendBlock(arrow: String, label: String, color: Color, typography: DashboardTypography) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.semantics { contentDescription = "Trend: $label" }
    ) {
        Text(arrow, color = color, fontSize = typography.trendArrowSp.sp, fontWeight = FontWeight.Bold)
        Text(label, color = color, fontSize = typography.trendDescriptionSp.sp, maxLines = 1)
    }
}

@Composable
private fun SensorRemainingPill() {
    Surface(color = DashboardElevatedSurface, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = "Sensor: 14 dni",
            color = DashboardSecondaryText,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun DashboardMetricTilesRow(state: MonitoringUiState, reading: GlucoseReading) {
    val tiles = buildDashboardMetricTiles(
        reading = reading,
        targetLow = state.settings.targetLow,
        targetHigh = state.settings.targetHigh
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tiles.forEachIndexed { index, tile ->
            CompactMetricTile(
                tile = tile,
                accent = when (tile.label) {
                    "Poniżej" -> AccentRed
                    "Zakres" -> AccentGreen
                    "Powyżej" -> AccentWarning
                    "Czujnik" -> AccentGreen
                    else -> DashboardPrimaryText
                },
                modifier = Modifier.width(108.dp),
                emphasized = index == 1
            )
        }
    }
}

@Composable
private fun CompactMetricTile(
    tile: DashboardMetricTile,
    accent: Color,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    Card(
        modifier = modifier.height(88.dp),
        colors = CardDefaults.cardColors(containerColor = if (emphasized) DashboardElevatedSurface else DashboardSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(tile.label, color = DashboardSecondaryText, fontSize = 10.sp, maxLines = 1)
            Text(tile.value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(tile.supportingText, color = DashboardMutedText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

    Card(
        modifier = modifier.height(96.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            when (kpi.mode) {
                HbA1cKpiMode.LAB_HBA1C -> {
                    Text("HbA1c", color = DashboardSecondaryText, fontSize = 11.sp)
                    Text("${"%.1f".format(kpi.valuePercent)}%", color = DashboardPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Lab · ${state.labHbA1cDate ?: "brak daty"}", color = DashboardMutedText, fontSize = 11.sp, maxLines = 1)
                    Text("Cel < ${state.targetHbA1cPercent}%", color = DashboardSecondaryText, fontSize = 10.sp)
                }
                HbA1cKpiMode.GMI_ESTIMATED -> {
                    Text("GMI", color = DashboardSecondaryText, fontSize = 11.sp)
                    Text("${"%.1f".format(kpi.valuePercent)}%", color = DashboardPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Szacowane z sensora", color = DashboardMutedText, fontSize = 11.sp)
                    Text("Nie zastępuje HbA1c", color = DashboardMutedText, fontSize = 10.sp)
                }
                HbA1cKpiMode.GMI_INSUFFICIENT -> {
                    Text("GMI", color = DashboardSecondaryText, fontSize = 11.sp)
                    Text("—", color = DashboardPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Za mało danych", color = DashboardMutedText, fontSize = 11.sp)
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

    Card(
        modifier = modifier.height(96.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Aktywność", color = DashboardSecondaryText, fontSize = 11.sp)
            Text(value?.let { "${"%.0f".format(it)}%" } ?: "—", color = DashboardPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(if (value == null) "Za mało danych" else "Cel: >= 75%", color = color, fontSize = 11.sp)
            Text("14 dni", color = DashboardMutedText, fontSize = 10.sp)
        }
    }
}

@Composable
private fun NfzStatusCompactCard(state: MonitoringUiState, reading: GlucoseReading) {
    var showInfo by remember { mutableStateOf(false) }
    val profile = remember(state.labHbA1cPercent) {
        NfzPatientProfile(
            patientGroup = NfzPatientGroup.UNKNOWN,
            hbA1cPercent = state.labHbA1cPercent,
            profileCompleted = state.labHbA1cPercent != null
        )
    }
    val historyTimeline = remember(reading) { readingTimeline(reading) }
    val assessment = remember(historyTimeline, state.settings.targetLow, state.settings.targetHigh, state.labHbA1cPercent) {
        assessNfzRefundContinuation(
            history = historyTimeline,
            targetLow = state.settings.targetLow,
            targetHigh = state.settings.targetHigh,
            profile = profile
        )
    }
    val summary = remember(assessment) { assessment.toStatusSummaryUi() }

    val color = when (summary.status) {
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
            .semantics { contentDescription = "Status NFZ: ${assessment.headline}" }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Refundacja NFZ", color = DashboardSecondaryText, fontSize = 12.sp)
                Spacer(modifier = Modifier.weight(1f))
                MetricStatusBadge(
                    text = when (summary.status) {
                        NfzStatus.GREEN -> "Status dobry"
                        NfzStatus.YELLOW -> "Wymaga uwagi"
                        NfzStatus.RED -> "Niespełnione"
                        NfzStatus.GRAY -> "Za mało danych"
                    },
                    color = color
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { showInfo = true },
                    modifier = Modifier.semantics { contentDescription = "Informacje o warunkach refundacji NFZ" }
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = DashboardSecondaryText)
                }
            }
            Text(summary.headline, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Szczegóły kryteriów i okres oceny znajdziesz pod ikoną informacji.", color = DashboardMutedText, fontSize = 12.sp)
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Warunki refundacji NFZ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status", fontWeight = FontWeight.Bold)
                    Text(summary.headline)
                    Text("Okres oceny", fontWeight = FontWeight.Bold)
                    Text(summary.details)
                    Text("Kryteria", fontWeight = FontWeight.Bold)
                    assessment.criteria.forEach { criterion ->
                        Text("• ${criterion.condition}: ${nfzStatusLabel(criterion.status)}")
                    }
                    Text("Dlaczego niespełnione", fontWeight = FontWeight.Bold)
                    if (summary.keyReasons.isEmpty()) {
                        Text("Brak głównych powodów do wyświetlenia.")
                    } else {
                        summary.keyReasons.forEach { Text("• $it") }
                    }
                    Text("Zalecenia", fontWeight = FontWeight.Bold)
                    summary.keyRecommendations.forEach { Text("• $it") }
                    Text("Zastrzeżenie", fontWeight = FontWeight.Bold)
                    Text("Aplikacja nie zastępuje decyzji lekarza ani NFZ.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("Rozumiem")
                }
            }
        )
    }
}

@Composable
private fun CriterionCard(criterion: NfzCriterionEvaluation) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DashboardElevatedSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(criterion.condition, color = DashboardPrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Aktualnie: ${criterion.currentValue}", color = DashboardSecondaryText, fontSize = 12.sp)
            Text("Wymagane: ${criterion.requiredValue}", color = DashboardSecondaryText, fontSize = 12.sp)
            Text(nfzStatusLabel(criterion.status), color = when (criterion.status) {
                NfzCriterionStatus.MET -> AccentGreen
                NfzCriterionStatus.NOT_MET -> AccentRed
                NfzCriterionStatus.UNKNOWN -> AccentWarning
                NfzCriterionStatus.NOT_APPLICABLE -> DashboardMutedText
            }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("Powód: ${criterion.reason}", color = DashboardPrimaryText, fontSize = 12.sp)
            criterion.recommendation?.let {
                Text("Zalecenie: $it", color = DashboardSecondaryText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GlucoseChartCard(state: MonitoringUiState, reading: GlucoseReading?, onOpenHistory: () -> Unit) {
    var selectedPoint by remember(reading) { mutableStateOf<GlucoseHistoryPoint?>(null) }
    val chartPoints = remember(reading) {
        if (reading != null) readingTimeline(reading) else emptyList()
    }
    val zoneId = DateTimeFormatterProvider.deviceZoneId()
    Card(
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Wykres historii glukozy. Dotknij punkt lub przeciągnij, aby podejrzeć pomiar. Dotknij tła wykresu, aby powiększyć." }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Historia glikemii", color = DashboardPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenHistory) {
                    Icon(Icons.AutoMirrored.Outlined.ShowChart, contentDescription = "Powiększyć wykres", tint = DashboardSecondaryText)
                }
            }
            Text("Dotknij wykresu, aby otworzyć pełny ekran.", color = DashboardMutedText, fontSize = 12.sp)
            if (chartPoints.isEmpty()) {
                EmptyChartState()
            } else {
                GlucoseChart(
                    points = chartPoints,
                    targetLow = state.settings.targetLow,
                    targetHigh = state.settings.targetHigh,
                    zoneId = zoneId,
                    selectedPoint = selectedPoint,
                    onPointSelected = { point -> selectedPoint = point },
                    onPointSelectionCleared = { selectedPoint = null },
                    onChartTapped = onOpenHistory,
                    modifier = Modifier.fillMaxWidth()
                )
                selectedPoint?.let { point ->
                    val label = formatChartPointLabel(
                        point = point,
                        targetLow = state.settings.targetLow,
                        targetHigh = state.settings.targetHigh,
                        zoneId = zoneId
                    )
                    Text(
                        text = "${label.valueText} • ${label.dateTime} • ${label.statusText}",
                        color = DashboardSecondaryText,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardBottomNavigation(
    onOpenHistory: () -> Unit,
    onOpenMore: () -> Unit
) {
    NavigationBar(
        containerColor = DashboardElevatedSurface,
        tonalElevation = 0.dp,
        modifier = Modifier.heightIn(min = 68.dp)
    ) {
        DashboardBottomNavItem(true, DashboardNavItem.GLOWNA.label, Icons.Default.Home) {}
        DashboardBottomNavItem(false, DashboardNavItem.HISTORIA.label, Icons.AutoMirrored.Outlined.ShowChart, onOpenHistory)
        DashboardBottomNavItem(false, DashboardNavItem.DODAJ.label, Icons.Default.AddCircle) {}
        DashboardBottomNavItem(false, DashboardNavItem.ALARMY.label, Icons.Default.NotificationsNone) {}
        DashboardBottomNavItem(false, DashboardNavItem.WIECEJ.label, Icons.Default.Settings, onOpenMore)
    }
}

@Composable
private fun RowScope.DashboardBottomNavItem(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) AccentGreen else DashboardSecondaryText
        )
        Text(
            label,
            fontSize = 11.sp,
            color = if (selected) DashboardPrimaryText else DashboardSecondaryText,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
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
        Icon(Icons.AutoMirrored.Outlined.ShowChart, contentDescription = null, tint = DashboardMutedText)
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
        "Ostatnia synchronizacja: ${PolishDateTimeFormatter.formatUserFacing(it)}"
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
