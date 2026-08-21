package com.libredisplay.ui.monitoring

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.BuildConfig
import com.libredisplay.R
import com.libredisplay.analytics.GlucoseMetricsCalculator
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.ui.theme.LibreCareColors
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.math.roundToLong

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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringScreen(
    refreshNonce: Int,
    onNavigateToSettings: () -> Unit,
    onNavigateToMetricSettings: () -> Unit = onNavigateToSettings,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToAnalytics: () -> Unit = onNavigateToDiagnostics,
    onSwitchToLiveMode: () -> Unit,
    onRunUiAudit: () -> Unit = {},
    viewModel: MonitoringViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var historyContext by remember { mutableStateOf<HistoryOpenContext?>(null) }
    var nfzDetailsContext by remember { mutableStateOf<NfzDetailsContext?>(null) }
    var showSwitchToLiveDialog by remember { mutableStateOf(false) }
    var recentPersonIds by rememberSaveable { mutableStateOf(listOf<String>()) }

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

    LaunchedEffect(state.selectedPatientId) {
        val selected = state.selectedPatientId ?: return@LaunchedEffect
        recentPersonIds = (listOf(selected) + recentPersonIds.filterNot { it == selected }).take(8)
    }

    val openFullScreenHistory: () -> Unit = {
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
                  lastReadingAt = state.reading?.timestamp ?: state.lastMeasurementTimestamp,
                  reading = state.reading,
                  appVersionLabel = BuildConfig.VERSION_NAME,
                  dbRangeLabel = compactDbRangeLabel(state.reading),
                  defaultDataRangeLabel = "12g",
                  onRunUiAudit = onRunUiAudit
             )
         },
         contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            TopLevelNavigationBar(
                selected = DashboardNavItem.GLOWNA,
                onOpenHome = {},
                onOpenHistory = onNavigateToAnalytics,
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
                    val configuration = LocalConfiguration.current
                    val isLandscapeHome = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                    Column(modifier = contentModifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (state.isDemoMode) {
                            DemoModeBanner(onSwitchToLiveMode = { showSwitchToLiveDialog = true })
                        }

                        // Single identity area: selected person appears only in this switcher.
                        CompactPersonSwitcherBar(
                            persons = state.availablePersons,
                            selectedPatientId = state.selectedPatientId,
                            recentPatientIds = recentPersonIds,
                            onPersonSelected = viewModel::onPersonSelected,
                            isDemoMode = state.isDemoMode
                        )

                        if (reading != null) {
                            val metrics = buildDashboardMetrics(reading, state.settings.targetLow, state.settings.targetHigh)
                            val fullTimeline = readingTimeline(reading)
                            val averageMgDl = fullTimeline.takeIf { it.isNotEmpty() }?.map { it.value }?.average()?.roundToInt()
                            val sensorActivityPercent = sensorActivityFromHistory(
                                history = fullTimeline,
                                periodStart = currentTime.minus(Duration.ofDays(14)),
                                periodEnd = currentTime
                            )?.activityPercent?.roundToInt()
                            val metricTiles = buildQuickMetricTiles(
                                belowDuration = metrics.belowDuration,
                                belowPercent = metrics.belowPercent,
                                inRangeDuration = metrics.inRangeDuration,
                                inRangePercent = metrics.inRangePercent,
                                aboveDuration = metrics.aboveDuration,
                                abovePercent = metrics.abovePercent,
                                gmiValue = metrics.gmiValue,
                                hba1cValue = metrics.hba1cValue,
                                averageValueMgDl = averageMgDl,
                                sensorActivityPercent = sensorActivityPercent
                            )
                            if (isLandscapeHome) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(0.38f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        RedesignedCurrentGlucoseCard(
                                            reading = reading,
                                            targetLow = state.settings.targetLow,
                                            targetHigh = state.settings.targetHigh,
                                            now = currentTime
                                        )
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
                                    }
                                    Column(
                                        modifier = Modifier.weight(0.62f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        GlucoseChartCard(
                                            state = state,
                                            reading = reading,
                                            onOpenHistory = openFullScreenHistory,
                                            now = currentTime,
                                            chartHeight = 220.dp
                                        )
                                    }
                                }
                                ImprovedQuickMetricsPanel(
                                    tiles = metricTiles,
                                    orderedIds = state.quickMetricsOrder,
                                    visibility = state.quickMetricsVisibility,
                                    onOrderChanged = viewModel::saveQuickMetricsOrder,
                                    onEditClick = onNavigateToMetricSettings
                                )
                            } else {
                                RedesignedCurrentGlucoseCard(
                                    reading = reading,
                                    targetLow = state.settings.targetLow,
                                    targetHigh = state.settings.targetHigh,
                                    now = currentTime
                                )
                                GlucoseChartCard(
                                    state = state,
                                    reading = reading,
                                    onOpenHistory = openFullScreenHistory,
                                    now = currentTime,
                                    chartHeight = 220.dp
                                )
                                ImprovedQuickMetricsPanel(
                                    tiles = metricTiles,
                                    orderedIds = state.quickMetricsOrder,
                                    visibility = state.quickMetricsVisibility,
                                    onOrderChanged = viewModel::saveQuickMetricsOrder,
                                    onEditClick = onNavigateToMetricSettings
                                )
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
                            }
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
    val trend = trendPresentation(
        trend = reading.trend,
        glucoseValue = reading.value,
        targetLow = targetLow,
        targetHigh = targetHigh
    )
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
private fun GlucoseChartCard(
    state: MonitoringUiState,
    reading: GlucoseReading?,
    onOpenHistory: () -> Unit,
    now: Instant = Instant.now(),
    chartHeight: Dp = 260.dp
) {
    var selectedPoint by remember(reading) { mutableStateOf<GlucoseHistoryPoint?>(null) }
    val fullHistory = remember(reading) {
        if (reading != null) readingTimeline(reading) else emptyList()
    }
    val chartPoints = remember(fullHistory) { homeChartAvailablePoints(fullHistory) }
    val zoneId = DateTimeFormatterProvider.deviceZoneId()
    val dataAvailability = remember(fullHistory, now) { buildHomeCoverageSummary(fullHistory, now) }
    val availableEnd = chartPoints.maxOfOrNull { it.timestamp }
    val navigationDomainStart = remember(availableEnd) {
        availableEnd?.minus(HomeChartRange.LAST_12_HOURS.duration)
    }
    val navigationDuration = remember(navigationDomainStart, availableEnd) {
        if (navigationDomainStart == null || availableEnd == null) Duration.ZERO
        else Duration.between(navigationDomainStart, availableEnd).coerceAtLeast(Duration.ZERO)
    }
    val databaseSpanLabel = remember(fullHistory, now) { homeDatabaseSpanLabel(fullHistory, now) }
    var homeChartRange by remember(state.selectedPatientId) { mutableStateOf(HomeChartRange.default) }
    var viewportStartMillis by remember(state.selectedPatientId, chartPoints) { mutableStateOf<Long?>(null) }
    var viewportDurationMillis by remember(state.selectedPatientId, chartPoints) { mutableStateOf(homeChartRange.duration.toMillis()) }
    var chartWidthPx by remember { mutableStateOf(1f) }
    var zoomNonce by remember { mutableStateOf(0L) }

    LaunchedEffect(state.selectedPatientId, navigationDomainStart, availableEnd) {
        viewportDurationMillis = homeChartRange.duration.toMillis()
        viewportStartMillis = if (navigationDomainStart != null && availableEnd != null) {
            (availableEnd.toEpochMilli() - viewportDurationMillis).coerceAtLeast(navigationDomainStart.toEpochMilli())
        } else {
            null
        }
    }

    LaunchedEffect(homeChartRange, navigationDomainStart, availableEnd) {
        if (navigationDomainStart == null || availableEnd == null) return@LaunchedEffect
        viewportDurationMillis = homeChartRange.duration.toMillis()
        viewportStartMillis = (availableEnd.toEpochMilli() - viewportDurationMillis).coerceAtLeast(navigationDomainStart.toEpochMilli())
    }

    LaunchedEffect(zoomNonce) {
        if (zoomNonce == 0L) return@LaunchedEffect
        delay(220L)
        val snapped = snapHomeChartRange(Duration.ofMillis(viewportDurationMillis))
        homeChartRange = snapped
        viewportDurationMillis = snapped.duration.toMillis()
    }

    val viewport = remember(navigationDomainStart, availableEnd, viewportStartMillis, viewportDurationMillis) {
        if (navigationDomainStart == null || availableEnd == null) {
            null
        } else {
            buildViewport(
                selectedRangeStart = navigationDomainStart,
                selectedRangeEnd = availableEnd,
                requestedStartMillis = viewportStartMillis,
                requestedDurationMillis = viewportDurationMillis.coerceAtLeast(1L),
                minimumDuration = Duration.ofHours(1)
            )
        }
    }
    val viewportPoints = remember(chartPoints, viewport) {
        if (viewport == null) chartPoints else chartPoints.filter { !it.timestamp.isBefore(viewport.start) && !it.timestamp.isAfter(viewport.end) }
    }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        val visibleViewport = viewport ?: return@rememberTransformableState
        if (navigationDomainStart == null || availableEnd == null || navigationDuration <= Duration.ofHours(1)) return@rememberTransformableState
        val updated = applyViewportTransform(
            currentStartMillis = visibleViewport.start.toEpochMilli(),
            currentDurationMillis = visibleViewport.duration.toMillis(),
            selectedRangeStart = navigationDomainStart,
            selectedRangeEnd = availableEnd,
            zoomChange = zoomChange,
            panXPx = 0f,
            chartWidthPx = chartWidthPx,
            minimumDuration = Duration.ofHours(1)
        )
        viewportStartMillis = updated.first
        viewportDurationMillis = updated.second
        if (kotlin.math.abs(zoomChange - 1f) > 0.01f) {
            zoomNonce = System.nanoTime()
        }
    }

    // Flat section – no Card wrapper, separated by spacing from surrounding content
     Column(
         modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(2.dp)
     ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text("Historia glikemii", color = DashboardPrimaryText, fontSize = 20.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Okno ${homeChartRange.shortLabel} · baza $databaseSpanLabel", color = DashboardSecondaryText, fontSize = 13.sp, lineHeight = 14.sp)
            }
              IconButton(onClick = onOpenHistory, modifier = Modifier.size(48.dp)) {
                  Icon(
                      Icons.AutoMirrored.Outlined.ShowChart,
                      contentDescription = "Powiększyć wykres",
                      tint = DashboardSecondaryText,
                      modifier = Modifier.size(18.dp)
                  )
            }
        }

        HomeDataAvailabilityRow(summary = dataAvailability)
        HomeChartRangeSelector(
            selectedRange = homeChartRange,
            onRangeSelected = { homeChartRange = it }
        )

        if (chartPoints.isEmpty()) {
            EmptyChartState()
        } else {
            val activeViewport = viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .transformable(state = transformState, enabled = navigationDuration > Duration.ofHours(1))
                    .pointerInput(homeChartRange, activeViewport) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (navigationDomainStart != null && availableEnd != null) {
                                    viewportDurationMillis = homeChartRange.duration.toMillis()
                                    viewportStartMillis = (availableEnd.toEpochMilli() - viewportDurationMillis).coerceAtLeast(navigationDomainStart.toEpochMilli())
                                }
                            }
                        )
                    }
            ) {
                GlucoseChart(
                    points = viewportPoints,
                    targetLow = state.settings.targetLow,
                    targetHigh = state.settings.targetHigh,
                    zoneId = zoneId,
                    domainStart = activeViewport?.start,
                    domainEnd = activeViewport?.end,
                    selectedPoint = selectedPoint,
                    onPointSelected = { point -> selectedPoint = point },
                    onPointSelectionCleared = { selectedPoint = null },
                    onChartTapped = onOpenHistory,
                    chartHeight = chartHeight,
                    maxYAxisLabels = 4,
                    maxXAxisLabels = 3,
                    axisLeftPaddingPx = 48f,
                    axisRightPaddingPx = 16f,
                    axisBottomPaddingPx = 52f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { chartWidthPx = it.width.toFloat() }
                )
            }
            activeViewport?.let { visibleViewport ->
                HomeChartNavigator(
                    viewport = visibleViewport,
                    availableStart = navigationDomainStart,
                    availableEnd = availableEnd,
                    axisLeftPaddingPx = 48f,
                    axisRightPaddingPx = 16f,
                    onFractionChanged = { fraction ->
                        if (navigationDomainStart != null && availableEnd != null) {
                            viewportStartMillis = viewportStartForFraction(fraction, visibleViewport, navigationDomainStart, availableEnd)
                        }
                    },
                    onViewportChanged = { fraction ->
                        if (navigationDomainStart != null && availableEnd != null) {
                            val totalMillis = Duration.between(navigationDomainStart, availableEnd).toMillis().coerceAtLeast(1L)
                            val windowMillis = visibleViewport.duration.toMillis().coerceAtLeast(1L)
                            val maxOffset = (totalMillis - windowMillis).coerceAtLeast(0L)
                            val startOffset = (maxOffset * fraction.coerceIn(0f, 1f)).roundToLong()
                            viewportStartMillis = navigationDomainStart.toEpochMilli() + startOffset
                        }
                    }
                )
            }
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

@Composable
private fun HomeDataAvailabilityRow(summary: HomeCoverageSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(summary.title, color = DashboardSecondaryText, fontSize = 13.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
        Text(
            summary.items.joinToString(" · ") { item -> "${item.label}: ${item.statusLabel}" },
            color = DashboardPrimaryText,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Clip,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HomeChartRangeSelector(
    selectedRange: HomeChartRange,
    onRangeSelected: (HomeChartRange) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        homeChartRanges().forEach { range ->
            val selected = range == selectedRange
            OutlinedButton(
                onClick = { onRangeSelected(range) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .semantics {
                        contentDescription = range.accessibilityLabel
                        this.selected = selected
                    },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (selected) LibreCareColors.AccentTeal else LibreCareColors.SurfaceMuted),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selected) LibreCareColors.AccentTeal else Color.Transparent,
                    contentColor = if (selected) Color(0xFF082F2D) else LibreCareColors.TextPrimary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 0.dp)
            ) {
                Text(range.shortLabel, fontSize = 13.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HomeChartNavigator(
    viewport: HistoryViewport,
    availableStart: Instant?,
    availableEnd: Instant?,
    axisLeftPaddingPx: Float,
    axisRightPaddingPx: Float,
    onFractionChanged: (Float) -> Unit,
    onViewportChanged: (Float) -> Unit
) {
    val safeStart = availableStart ?: viewport.start
    val safeEnd = availableEnd ?: viewport.end
    val fraction = viewportScrollFraction(viewport, safeStart, safeEnd)
    val totalMillis = Duration.between(safeStart, safeEnd).toMillis().coerceAtLeast(1L)
    val windowFraction = (viewport.duration.toMillis().toFloat() / totalMillis.toFloat()).coerceIn(0.08f, 1f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(viewport) {
                detectTapGestures { offset ->
                    val geometry = computeHomeNavigatorGeometry(
                        totalWidthPx = size.width.toFloat(),
                        leftInsetPx = axisLeftPaddingPx,
                        rightInsetPx = axisRightPaddingPx,
                        viewportFraction = fraction,
                        windowFraction = windowFraction
                    )
                    val trackRelativeX = (offset.x - geometry.trackLeft).coerceIn(0f, geometry.trackWidth)
                    val viewportLeftFraction = if (geometry.trackWidth <= geometry.viewportWidth) {
                        0f
                    } else {
                        (trackRelativeX - geometry.viewportWidth / 2f)
                            .coerceIn(0f, geometry.trackWidth - geometry.viewportWidth) /
                            (geometry.trackWidth - geometry.viewportWidth)
                    }
                    onFractionChanged(viewportLeftFraction)
                }
            }
            .pointerInput(viewport) {
                detectDragGestures { change, dragAmount ->
                    val geometry = computeHomeNavigatorGeometry(
                        totalWidthPx = size.width.toFloat(),
                        leftInsetPx = axisLeftPaddingPx,
                        rightInsetPx = axisRightPaddingPx,
                        viewportFraction = fraction,
                        windowFraction = windowFraction
                    )
                    val movableWidth = (geometry.trackWidth - geometry.viewportWidth).coerceAtLeast(1f)
                    val deltaFraction = dragAmount.x / movableWidth
                    onViewportChanged((fraction + deltaFraction).coerceIn(0f, 1f))
                    change.consume()
                }
            }
            .semantics {
                contentDescription = "Widoczny przedział czasu: ${buildViewportLabel(viewport.start, viewport.end, DateTimeFormatterProvider.deviceZoneId())}"
            }
    ) {
        val geometry = computeHomeNavigatorGeometry(
            totalWidthPx = size.width,
            leftInsetPx = axisLeftPaddingPx,
            rightInsetPx = axisRightPaddingPx,
            viewportFraction = fraction,
            windowFraction = windowFraction
        )
        val trackTop = size.height / 2f - 4f
        val trackHeight = 8f
        val viewportLeft = geometry.viewportLeft
        val viewportWidth = geometry.viewportWidth
        drawRoundRect(
            color = LibreCareColors.SurfaceMuted,
            topLeft = androidx.compose.ui.geometry.Offset(geometry.trackLeft, trackTop),
            size = androidx.compose.ui.geometry.Size(geometry.trackWidth, trackHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = LibreCareColors.AccentTeal.copy(alpha = 0.85f),
            topLeft = androidx.compose.ui.geometry.Offset(viewportLeft, 4f),
            size = androidx.compose.ui.geometry.Size(viewportWidth, size.height - 8f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
            style = Stroke(width = 2f)
        )
        drawRoundRect(
            color = LibreCareColors.AccentTeal.copy(alpha = 0.22f),
            topLeft = androidx.compose.ui.geometry.Offset(viewportLeft, 4f),
            size = androidx.compose.ui.geometry.Size(viewportWidth, size.height - 8f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
        )
        if (viewport.canPan) {
            val gripCenterX = viewportLeft + viewportWidth / 2f
            repeat(3) { index ->
                val gripX = gripCenterX + (index - 1) * 6f
                drawLine(
                    color = Color.White.copy(alpha = 0.65f),
                    start = androidx.compose.ui.geometry.Offset(gripX, 9f),
                    end = androidx.compose.ui.geometry.Offset(gripX, size.height - 9f),
                    strokeWidth = 1.5f
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

private fun compactDbRangeLabel(reading: GlucoseReading?): String {
    val history = reading?.history ?: return "brak"
    if (history.size < 2) return "<1h"
    val start = history.minOfOrNull { it.timestamp } ?: return "brak"
    val end = history.maxOfOrNull { it.timestamp } ?: return "brak"
    val duration = Duration.between(start, end).coerceAtLeast(Duration.ZERO)
    val hours = duration.toHours()
    val days = hours / 24
    return when {
        days > 0 -> "${days}d"
        hours > 0 -> "${hours}g"
        else -> "${duration.toMinutes().coerceAtLeast(1)}m"
    }
}

