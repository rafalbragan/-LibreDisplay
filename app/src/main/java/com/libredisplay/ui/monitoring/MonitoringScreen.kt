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
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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
    WIECEJ("Więcej")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringScreen(
    refreshNonce: Int,
    onNavigateToSettings: () -> Unit,
    onNavigateToMetricSettings: () -> Unit = onNavigateToSettings,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToAnalytics: () -> Unit = onNavigateToDiagnostics,
    onSwitchToLiveMode: () -> Unit,
    viewModel: MonitoringViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var historyContext by remember { mutableStateOf<HistoryOpenContext?>(null) }
    var nfzDetailsContext by remember { mutableStateOf<NfzDetailsContext?>(null) }
    var showSwitchToLiveDialog by remember { mutableStateOf(false) }

    // Local ticker: refreshes time-sensitive UI every 30 s without any network request.
    // Covers: "chwilę temu", "Sensor: X dni", coverage countdown, reading age.
    var currentTime by remember { mutableStateOf(java.time.Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            currentTime = java.time.Instant.now()
        }
    }

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

    if (nfzDetailsContext != null) {
        NfzDetailsScreen(
            context = nfzDetailsContext!!,
            onNavigateBack = { nfzDetailsContext = null }
        )
        return
    }

     Scaffold(
         containerColor = DashboardBackground,
         topBar = {
             LibreTopBar(
                 onNavigateToSettings = onNavigateToSettings
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
                    Column(modifier = contentModifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Data freshness and sensor status bar (replaces status shown in cards)
                        DataFreshnessAndSensorStatusBar(
                            lastReadingAt = reading?.timestamp ?: state.lastMeasurementTimestamp,
                            reading = reading,
                            now = currentTime,
                            modifier = Modifier.padding(horizontal = 0.dp)
                        )

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
                            // Redesigned glucose card
                            RedesignedCurrentGlucoseCard(
                                reading = reading,
                                targetLow = state.settings.targetLow,
                                targetHigh = state.settings.targetHigh,
                                now = currentTime
                            )

                            // Subtle divider before metrics
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = DashboardSurface
                            )

                            // Quick metrics panel – flat, no card wrapper
                            val metrics = buildDashboardMetrics(reading, state.settings.targetLow, state.settings.targetHigh)
                            val metricTiles = buildQuickMetricTiles(
                                belowDuration = metrics.belowDuration,
                                belowPercent = metrics.belowPercent,
                                inRangeDuration = metrics.inRangeDuration,
                                inRangePercent = metrics.inRangePercent,
                                aboveDuration = metrics.aboveDuration,
                                abovePercent = metrics.abovePercent,
                                gmiValue = metrics.gmiValue,
                                hba1cValue = metrics.hba1cValue
                            )
                            ImprovedQuickMetricsPanel(
                                tiles = metricTiles,
                                orderedIds = state.quickMetricsOrder,
                                onOrderChanged = viewModel::saveQuickMetricsOrder
                            )
                            TextButton(
                                onClick = onNavigateToMetricSettings,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Zmień metryki", color = AccentGreen)
                            }

                            // Subtle divider before chart
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = DashboardSurface
                            )

                            // History preview chart (flat section, no card wrapper)
                            GlucoseChartCard(
                                state = state,
                                reading = reading,
                                onOpenHistory = openHistory,
                                now = currentTime
                            )

                            // Subtle divider before NFZ
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = DashboardSurface
                            )

                            // NFZ Refund status
                            NfzStatusCompactCard(
                                state = state,
                                reading = reading,
                                onOpenDetails = { assessment, summary, attentionCount ->
                                    nfzDetailsContext = NfzDetailsContext(
                                        assessment = assessment,
                                        summary = summary,
                                        attentionCount = attentionCount,
                                        totalCriteriaCount = assessment.criteria.size,
                                        selectedHomeRangeDays = state.timeRange.durationDays,
                                        selectedHomeRangeLabel = compactDashboardRangeLabel(
                                            state.timeRange,
                                            state.lastMeasurementTimestamp
                                        )
                                    )
                                }
                            )
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
private fun DemoModeBanner(onSwitchToLiveMode: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3A2E18).copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
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
private fun NfzStatusCompactCard(
    state: MonitoringUiState,
    reading: GlucoseReading,
    onOpenDetails: (NfzAssessment, NfzStatusSummaryUi, Int) -> Unit
) {
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
    val attentionCount = remember(assessment) {
        assessment.criteria.count { it.status == NfzCriterionStatus.NOT_MET || it.status == NfzCriterionStatus.UNKNOWN }
    }

    val color = when (summary.status) {
        NfzStatus.GREEN -> AccentGreen
        NfzStatus.YELLOW -> AccentWarning
        NfzStatus.RED -> AccentRed
        NfzStatus.GRAY -> DashboardMutedText
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails(assessment, summary, attentionCount) }
            .semantics { contentDescription = "Status NFZ: ${assessment.headline}" }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Refundacja NFZ", color = DashboardSecondaryText, fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.Info, contentDescription = null, tint = DashboardSecondaryText)
        }
        Text(
            text = if (attentionCount > 0) {
                "⚠ $attentionCount kryteria wymagają uwagi"
            } else {
                "Brak kryteriów wymagających uwagi"
            },
            color = if (attentionCount > 0) AccentWarning else AccentGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text("Sprawdź szczegóły >", color = color, fontSize = 12.sp)
        androidx.compose.material3.HorizontalDivider(color = DashboardSurface)
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
private fun GlucoseChartCard(state: MonitoringUiState, reading: GlucoseReading?, onOpenHistory: () -> Unit, now: Instant = Instant.now()) {
    var selectedPoint by remember(reading) { mutableStateOf<GlucoseHistoryPoint?>(null) }
    val chartPoints = remember(reading) {
        if (reading != null) readingTimeline(reading) else emptyList()
    }
    val zoneId = DateTimeFormatterProvider.deviceZoneId()

    // Coverage: separate selected range from actually available data.
    // 'now' included in remember key so the countdown ticks locally every 30 s.
    val coverage = remember(chartPoints, state.timeRange, now) {
        computeDataCoverage(
            history = chartPoints,
            selectedRange = java.time.Duration.ofSeconds(state.timeRange.durationSeconds),
            selectedRangeLabel = when (state.timeRange.presetRange) {
                PresetTimeRange.LAST_12_HOURS -> "12 godz."
                PresetTimeRange.LAST_24_HOURS -> "24 godz."
                PresetTimeRange.LAST_7_DAYS -> "7 dni"
                PresetTimeRange.LAST_14_DAYS -> "14 dni"
                PresetTimeRange.LAST_30_DAYS -> "30 dni"
                PresetTimeRange.LAST_90_DAYS -> "90 dni"
                PresetTimeRange.LAST_12_MONTHS -> "12 mies."
            },
            now = now
        )
    }

    // Flat section – no Card wrapper, separated by spacing from surrounding content
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "Historia glikemii",
                    color = DashboardPrimaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                // Show actual available span, not selected range
                Text(
                    text = coverage.sectionHeaderLabel,
                    color = DashboardSecondaryText,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onOpenHistory) {
                Icon(Icons.AutoMirrored.Outlined.ShowChart, contentDescription = "Powiększyć wykres", tint = DashboardSecondaryText)
            }
        }

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

        // Coverage note and estimate
        coverage.selectedRangeNote?.let { note ->
            Text(
                text = note,
                color = DashboardMutedText,
                fontSize = 11.sp
            )
        }
        coverage.fullCoverageEstimate?.let { estimate ->
            Text(
                text = estimate,
                color = DashboardMutedText,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3A2024).copy(alpha = 0.45f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
