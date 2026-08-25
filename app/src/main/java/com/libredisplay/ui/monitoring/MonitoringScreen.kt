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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
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
import com.libredisplay.data.model.QuickMetricId
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
    val homeHistory by viewModel.homeHistory.collectAsState()
    val homeDataSpan by viewModel.homeDataSpan.collectAsState()
    var historyContext by remember { mutableStateOf<HistoryOpenContext?>(null) }
    var nfzDetailsContext by remember { mutableStateOf<NfzDetailsContext?>(null) }
    var showSwitchToLiveDialog by remember { mutableStateOf(false) }
    var recentPersonIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    // Metric tiles computed by the chart card, lifted so landscape can render them full-width below
    // the [glucose | chart] row instead of inside the narrower chart column.
    var landscapeMetricTiles by remember(state.selectedPatientId) { mutableStateOf<List<QuickMetricTileUi>?>(null) }
    // Lifted range selector, so landscape can show the range chips in the left column.
    var landscapeRangeSelector by remember(state.selectedPatientId) { mutableStateOf<RangeSelectorState?>(null) }
    // Home content scroll, hoisted so landscape can auto-scroll to the chart ("Historia glikemii").
    val homeScrollState = rememberScrollState()
    var landscapeChartAnchorPx by remember { mutableStateOf(0) }

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

    val configuration = LocalConfiguration.current
    val isLandscapeHome = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // On entering landscape, scroll a little so the chart section ("Historia glikemii") starts at the
    // top; on returning to portrait, reset to the top.
    LaunchedEffect(isLandscapeHome, landscapeChartAnchorPx) {
        if (isLandscapeHome && landscapeChartAnchorPx > 0) {
            homeScrollState.animateScrollTo(landscapeChartAnchorPx)
        } else if (!isLandscapeHome) {
            homeScrollState.scrollTo(0)
        }
    }

         Scaffold(
         containerColor = DashboardBackground,
         topBar = {
             if (!isLandscapeHome) {
                 LibreTopBar(
                      lastReadingAt = state.lastSuccessfulFetchAt ?: state.reading?.timestamp ?: state.lastMeasurementTimestamp,
                      reading = state.reading,
                      appVersionLabel = BuildConfig.VERSION_NAME,
                      onRunUiAudit = onRunUiAudit
                 )
             }
         },
         contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isLandscapeHome) {
                TopLevelNavigationBar(
                    selected = DashboardNavItem.GLOWNA,
                    onOpenHome = {},
                    onOpenHistory = onNavigateToAnalytics,
                    onOpenSettings = onNavigateToSettings
                )
            }
        }
    ) { padding ->
        Surface(
            color = DashboardBackground,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when {
                !state.isConfigured -> EmptyConfigurationState(onNavigateToSettings)
                state.isLoading && state.reading == null -> LoadingState()
                else -> {
                    val reading = state.reading
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(homeScrollState)
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
                            if (isLandscapeHome) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned {
                                            landscapeChartAnchorPx = it.boundsInParent().top.roundToInt()
                                        },
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(0.38f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Landscape hides the top app bar; this compact header keeps
                                        // the LibreCare title and the last-update time on the left.
                                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.Bottom,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.app_name),
                                                    color = LibreCareColors.TextPrimary,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "v${BuildConfig.VERSION_NAME}",
                                                    color = LibreCareColors.TextSecondary,
                                                    fontSize = 12.sp,
                                                    maxLines = 1
                                                )
                                            }
                                            val landscapeLastAt = state.lastSuccessfulFetchAt
                                                ?: state.reading?.timestamp
                                                ?: state.lastMeasurementTimestamp
                                            Text(
                                                text = "Ostatnia aktualizacja: " + (landscapeLastAt?.let {
                                                    val zone = DateTimeFormatterProvider.deviceZoneId()
                                                    "${DateTimeFormatterProvider.compactDateFormatter().withZone(zone).format(it)} ${PolishDateTimeFormatter.formatTime(it, zone)}"
                                                } ?: "brak danych"),
                                                color = LibreCareColors.TextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                        RedesignedCurrentGlucoseCard(
                                            reading = reading,
                                            targetLow = state.settings.targetLow,
                                            targetHigh = state.settings.targetHigh,
                                            now = currentTime
                                        )
                                        // Range chips moved under the glucose card (above NFZ) so the
                                        // user can change the range here and see the chart + metrics
                                        // update immediately.
                                        landscapeRangeSelector?.let { rs ->
                                            HomeChartRangeSelector(
                                                options = rs.options,
                                                selectedRange = rs.selectedRange,
                                                onRangeSelected = rs.onRangeSelected
                                            )
                                        }
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
                                    }
                                    Column(
                                        modifier = Modifier.weight(0.62f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        GlucoseChartCard(
                                            state = state,
                                            reading = reading,
                                            historyPoints = homeHistory,
                                            storedDataSpan = homeDataSpan,
                                            onRangeDurationChanged = viewModel::loadHomeHistory,
                                            onOpenHistory = openFullScreenHistory,
                                            now = currentTime,
                                            chartHeight = 220.dp,
                                            showInlineMetrics = false,
                                            onMetricsComputed = { landscapeMetricTiles = it },
                                            showInlineRangeSelector = false,
                                            onRangeSelectorState = { landscapeRangeSelector = it },
                                            onQuickMetricsOrderChanged = viewModel::saveQuickMetricsOrder,
                                            onEditMetricsClick = onNavigateToMetricSettings
                                        )
                                    }
                                }
                                // Landscape: metrics span the FULL screen width (left-to-right) below
                                // the glucose/chart row, where there is plenty of horizontal space.
                                landscapeMetricTiles?.let { tiles ->
                                    ImprovedQuickMetricsPanel(
                                        tiles = tiles,
                                        orderedIds = state.quickMetricsOrder,
                                        visibility = state.quickMetricsVisibility,
                                        onOrderChanged = viewModel::saveQuickMetricsOrder,
                                        onEditClick = onNavigateToMetricSettings
                                    )
                                }
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
                                    historyPoints = homeHistory,
                                    storedDataSpan = homeDataSpan,
                                    onRangeDurationChanged = viewModel::loadHomeHistory,
                                    onOpenHistory = openFullScreenHistory,
                                    now = currentTime,
                                    chartHeight = 220.dp,
                                    onQuickMetricsOrderChanged = viewModel::saveQuickMetricsOrder,
                                    onEditMetricsClick = onNavigateToMetricSettings
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
                            }
                        } else {
                            EmptyChartState()
                        }
                        ErrorPanel(state.errorMessage, state.canRetry, state.retryCooldownSecondsRemaining, viewModel)
                    }
                }
            }
            }
            if (isLandscapeHome) {
                SideNavigationRail(
                    selected = DashboardNavItem.GLOWNA,
                    onOpenHome = {},
                    onOpenHistory = onNavigateToAnalytics,
                    onOpenSettings = onNavigateToSettings
                )
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
internal fun CurrentGlucoseHeroCard(reading: GlucoseReading, targetLow: Int, targetHigh: Int) {
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
            .testTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_CARD)
            .semantics {
                contentDescription = "Aktualna glikemia ${reading.value} mg na decylitr, ${trendContentDescription(reading.trend)}"
                stateDescription = glucoseRangeStatus(reading.value, targetLow, targetHigh)
            }
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
                            modifier = Modifier.testTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_VALUE),
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
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .testTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_UNIT),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    TrendBlock(trend.arrow, trend.label, trend.color, type)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(1.dp)
                            .testTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_SEVERITY)
                            .semantics {
                                contentDescription = glucoseRangeStatus(reading.value, targetLow, targetHigh)
                            }
                    )
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
internal fun TrendBlock(arrow: String, label: String, color: Color, typography: DashboardTypography) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
            .testTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_TREND)
            .semantics { contentDescription = "Trend: $label" }
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

/**
 * Lifted state of the home chart range selector, so landscape can render the range chips outside the
 * chart card (in the left column) while the card keeps owning the range state.
 */
internal data class RangeSelectorState(
    val options: List<HomeChartRangeOption>,
    val selectedRange: HomeChartRange,
    val onRangeSelected: (HomeChartRange) -> Unit
)

@Composable
private fun GlucoseChartCard(
    state: MonitoringUiState,
    reading: GlucoseReading?,
    historyPoints: List<GlucoseHistoryPoint>,
    storedDataSpan: Duration,
    onRangeDurationChanged: (Duration) -> Unit,
    onOpenHistory: () -> Unit,
    now: Instant = Instant.now(),
    chartHeight: Dp = 260.dp,
    showInlineMetrics: Boolean = true,
    onMetricsComputed: ((List<QuickMetricTileUi>) -> Unit)? = null,
    showInlineRangeSelector: Boolean = true,
    onRangeSelectorState: ((RangeSelectorState) -> Unit)? = null,
    onQuickMetricsOrderChanged: (List<QuickMetricId>) -> Unit,
    onEditMetricsClick: () -> Unit
) {
    var selectedPoint by remember(state.selectedPatientId) { mutableStateOf<GlucoseHistoryPoint?>(null) }
    val fallbackHistory = remember(reading) {
        if (reading != null) readingTimeline(reading) else emptyList()
    }
    // The dashboard chart works on the merged database history so that long ranges (3d ... 12m)
    // become available as soon as enough data has been collected.
    val chartPoints = remember(historyPoints, fallbackHistory) {
        (historyPoints + fallbackHistory)
            .distinctBy { it.timestamp to it.value }
            .sortedBy { it.timestamp }
    }
    val zoneId = DateTimeFormatterProvider.deviceZoneId()
    // Range availability is driven by the full stored span (from the database), not only by the
    // window currently loaded into the chart. This mirrors the full-screen history: as soon as the
    // collected data is longer than a scale, the next scale unlocks - even with gaps in the data.
    val loadedSpan = remember(chartPoints) { homeChartDataSpan(chartPoints) }
    val dataSpan = remember(loadedSpan, storedDataSpan) { maxOf(loadedSpan, storedDataSpan) }
    val dataSummary = remember(dataSpan) { buildHomeDataSummary(dataSpan) }
    val rangeOptions = remember(dataSpan) { homeChartRangeOptions(dataSpan) }
    val availableStart = chartPoints.minOfOrNull { it.timestamp }
    val availableEnd = chartPoints.maxOfOrNull { it.timestamp }

    var homeChartRangeName by rememberSaveable(state.selectedPatientId) {
        mutableStateOf(HomeChartRange.default.name)
    }
    // True once the user manually taps a range chip. Until then the dashboard shows the default
    // window (12h portrait / 24h landscape); after a tap the choice is kept for the session.
    var userSelectedRange by rememberSaveable(state.selectedPatientId) { mutableStateOf(false) }
    val selectedRange = remember(homeChartRangeName) {
        runCatching { HomeChartRange.valueOf(homeChartRangeName) }.getOrDefault(HomeChartRange.default)
    }
    var viewportStartMillis by remember(state.selectedPatientId) { mutableStateOf<Long?>(null) }
    var followLatest by remember(state.selectedPatientId) { mutableStateOf(true) }
    var hasNewerData by remember(state.selectedPatientId) { mutableStateOf(false) }
    var chartWidthPx by remember { mutableStateOf(1f) }

    val navigationDomainStart = availableStart
    val navigationDuration = remember(navigationDomainStart, availableEnd) {
        if (navigationDomainStart == null || availableEnd == null) Duration.ZERO
        else Duration.between(navigationDomainStart, availableEnd).coerceAtLeast(Duration.ZERO)
    }
    val viewportDurationMillis = remember(selectedRange, navigationDuration) {
        selectedRange.duration.toMillis().coerceAtMost(navigationDuration.toMillis().coerceAtLeast(1L))
    }

    // Range selection policy:
    // - Default (no manual choice yet): show 12h in portrait and 24h in landscape when the collected
    //   data can fill it; otherwise fall back to the largest range the data currently supports.
    // - After a manual choice: keep the user's range for the whole session, only correcting it if it
    //   is no longer valid for the available data.
    val chartConfiguration = LocalConfiguration.current
    val isChartLandscape = chartConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val preferredDefaultRange = if (isChartLandscape) HomeChartRange.LAST_24_HOURS else HomeChartRange.LAST_12_HOURS
    LaunchedEffect(rangeOptions, userSelectedRange, preferredDefaultRange) {
        val enabled = rangeOptions.filter { it.enabled }.map { it.range }
        if (enabled.isEmpty()) return@LaunchedEffect
        val largest = enabled.last()
        if (!userSelectedRange) {
            val target = if (preferredDefaultRange in enabled) preferredDefaultRange else largest
            if (selectedRange != target) homeChartRangeName = target.name
        } else if (selectedRange !in enabled) {
            homeChartRangeName = largest.name
        }
    }

    // Load ALL available history (not only the selected range). The dashboard chart then keeps the
    // whole collected span in memory, so the mini navigator under the chart can represent the largest
    // available range and the viewport (the selected range, e.g. 24g) can be panned across it. The
    // selected range only controls how wide the visible window is, never how much data is loaded.
    LaunchedEffect(state.selectedPatientId, dataSpan) {
        onRangeDurationChanged(largestSelectableHomeChartRange(dataSpan).duration)
    }

    // Changing the selected range only re-centres the visible window on the most recent data; it does
    // not reload, because the full history is already available for panning.
    LaunchedEffect(selectedRange, state.selectedPatientId) {
        followLatest = true
        hasNewerData = false
        viewportStartMillis = availableEnd?.toEpochMilli()?.minus(selectedRange.duration.toMillis())
    }

    // Background refresh must NOT move the user away from the window they are inspecting.
    LaunchedEffect(availableEnd, viewportDurationMillis) {
        val end = availableEnd ?: return@LaunchedEffect
        if (followLatest) {
            viewportStartMillis = end.toEpochMilli() - viewportDurationMillis
            hasNewerData = false
        } else {
            val currentEnd = (viewportStartMillis ?: 0L) + viewportDurationMillis
            if (end.toEpochMilli() > currentEnd) hasNewerData = true
        }
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
    val visibleMetrics = remember(viewportPoints, state.settings.targetLow, state.settings.targetHigh) {
        val distribution = GlucoseMetricsCalculator.calculateRangeDistribution(
            readings = viewportPoints,
            targetLow = state.settings.targetLow,
            targetHigh = state.settings.targetHigh,
            lowCritical = 54,
            highCritical = 250
        )
        val avg = viewportPoints.takeIf { it.isNotEmpty() }?.map { it.value }?.average()?.roundToInt()
        val gmi = avg?.toDouble()?.let(GlucoseMetricsCalculator::calculateGmi)
        val viewportStart = viewportPoints.minOfOrNull { it.timestamp }
        val viewportEnd = viewportPoints.maxOfOrNull { it.timestamp }
        val sensorActivityPercent = if (viewportStart != null && viewportEnd != null) {
            sensorActivityFromHistory(
                history = viewportPoints,
                periodStart = viewportStart,
                periodEnd = viewportEnd
            )?.activityPercent?.roundToInt()
        } else {
            null
        }
        Triple(distribution, gmi, avg) to sensorActivityPercent
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
            Text("Historia glikemii", color = DashboardPrimaryText, fontSize = 20.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenHistory, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.AutoMirrored.Outlined.ShowChart,
                    contentDescription = "Powiększyć wykres",
                    tint = DashboardSecondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        val onRangeSelected: (HomeChartRange) -> Unit = { range ->
            userSelectedRange = true
            homeChartRangeName = range.name
        }
        // Lift the range selector state so a caller (landscape) can render the chips elsewhere.
        LaunchedEffect(rangeOptions, selectedRange) {
            onRangeSelectorState?.invoke(RangeSelectorState(rangeOptions, selectedRange, onRangeSelected))
        }
        if (showInlineRangeSelector) {
            HomeChartRangeSelector(
                options = rangeOptions,
                selectedRange = selectedRange,
                onRangeSelected = onRangeSelected
            )
        }

        HomeDataAvailabilityRow(summary = dataSummary)

        if (hasNewerData) {
            NewDataChip(
                onShowLatest = {
                    followLatest = true
                    hasNewerData = false
                    availableEnd?.let { end ->
                        viewportStartMillis = end.toEpochMilli() - viewportDurationMillis
                    }
                }
            )
        }

        if (chartPoints.isEmpty()) {
            EmptyChartState()
        } else {
            val activeViewport = viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(selectedRange, activeViewport) {
                        detectTapGestures(
                            onDoubleTap = {
                                followLatest = true
                                hasNewerData = false
                                availableEnd?.let { end ->
                                    viewportStartMillis = end.toEpochMilli() - viewportDurationMillis
                                }
                            }
                        )
                    }
            ) {
                GlucoseChart(
                    points = chartPoints,
                    targetLow = state.settings.targetLow,
                    targetHigh = state.settings.targetHigh,
                    xTickInterval = homeChartAxisTickInterval(selectedRange),
                    zoneId = zoneId,
                    domainStart = activeViewport?.start,
                    domainEnd = activeViewport?.end,
                    selectedPoint = selectedPoint,
                    onPointSelected = { point -> selectedPoint = point },
                    onPointSelectionCleared = { selectedPoint = null },
                    onChartTapped = onOpenHistory,
                    chartHeight = chartHeight,
                    maxYAxisLabels = 4,
                    maxXAxisLabels = 5,
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
                    onViewportChanged = { fraction ->
                        if (navigationDomainStart != null && availableEnd != null) {
                            followLatest = false
                            val totalMillis = Duration.between(navigationDomainStart, availableEnd).toMillis().coerceAtLeast(1L)
                            val windowMillis = visibleViewport.duration.toMillis().coerceAtLeast(1L)
                            val maxOffset = (totalMillis - windowMillis).coerceAtLeast(0L)
                            val startOffset = (maxOffset * fraction.coerceIn(0f, 1f)).roundToLong()
                            viewportStartMillis = navigationDomainStart.toEpochMilli() + startOffset
                            if (fraction >= 0.999f) followLatest = true
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
                    text = "${label.valueText}\n${label.dateTime}",
                    modifier = Modifier.testTag(LibreCareTestTags.HOME_CHART_SELECTED_LABEL),
                    color = DashboardSecondaryText,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val distribution = visibleMetrics.first.first
            val visibleMin = viewportPoints.minOfOrNull { it.value }
            val visibleMax = viewportPoints.maxOfOrNull { it.value }
            val veryLowEpisodes = countEpisodes(viewportPoints) { it < 54 }
            val veryHighEpisodes = countEpisodes(viewportPoints) { it > 250 }

            val dataCoverageStats = calculateDataCoverage(viewportPoints, selectedRange.duration)

            CompactTimeInRangeBar(
                belowPercent = distribution.belowRangePercent,
                inRangePercent = distribution.inRangePercent,
                abovePercent = distribution.aboveRangePercent
            )

            val metricTiles = buildQuickMetricTiles(
                belowDuration = distribution.belowRangeDuration,
                belowPercent = distribution.belowRangePercent,
                inRangeDuration = distribution.inRangeDuration,
                inRangePercent = distribution.inRangePercent,
                aboveDuration = distribution.aboveRangeDuration,
                abovePercent = distribution.aboveRangePercent,
                minValueMgDl = visibleMin,
                maxValueMgDl = visibleMax,
                gmiValue = visibleMetrics.first.second,
                averageValueMgDl = visibleMetrics.first.third,
                veryLowEpisodes = veryLowEpisodes,
                veryHighEpisodes = veryHighEpisodes,
                cvPercent = com.libredisplay.analytics.GlucoseMetricsCalculator
                    .calculateCoefficientOfVariation(viewportPoints),
                dataCoveragePercent = dataCoverageStats.coveragePercent,
                dataMissingDescription = dataCoverageStats.missingDescription
            )
            // Lift the computed tiles so a caller (landscape) can render them full-width. List uses
            // structural equality, so this only fires when the tiles actually change.
            LaunchedEffect(metricTiles) { onMetricsComputed?.invoke(metricTiles) }
            if (showInlineMetrics) {
                ImprovedQuickMetricsPanel(
                    tiles = metricTiles,
                    orderedIds = state.quickMetricsOrder,
                    visibility = state.quickMetricsVisibility,
                    onOrderChanged = onQuickMetricsOrderChanged,
                    onEditClick = onEditMetricsClick
                )
            }
        }

    }
}

internal fun countEpisodes(
    points: List<GlucoseHistoryPoint>,
    predicate: (Int) -> Boolean
): Int {
    if (points.isEmpty()) return 0
    val sorted = points.sortedBy { it.timestamp }
    var episodes = 0
    var inEpisode = false
    sorted.forEach { point ->
        val matches = predicate(point.value)
        if (matches && !inEpisode) {
            episodes += 1
            inEpisode = true
        } else if (!matches) {
            inEpisode = false
        }
    }
    return episodes
}

@Composable
private fun CompactTimeInRangeBar(
    belowPercent: Int,
    inRangePercent: Int,
    abovePercent: Int
) {
    val safeBelow = belowPercent.coerceIn(0, 100)
    val safeInRange = inRangePercent.coerceIn(0, 100)
    val safeAbove = abovePercent.coerceIn(0, 100)
    val total = (safeBelow + safeInRange + safeAbove).coerceAtLeast(1)
    val belowWeight = (safeBelow.toFloat() / total.toFloat()).coerceAtLeast(0.0001f)
    val inRangeWeight = (safeInRange.toFloat() / total.toFloat()).coerceAtLeast(0.0001f)
    val aboveWeight = (safeAbove.toFloat() / total.toFloat()).coerceAtLeast(0.0001f)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Poniżej $safeBelow% | W zakresie $safeInRange% | Powyżej $safeAbove%",
            color = DashboardPrimaryText,
            fontSize = 12.sp
        )
        Row(modifier = Modifier.fillMaxWidth().height(6.dp)) {
            Box(
                modifier = Modifier
                    .weight(belowWeight)
                    .fillMaxSize()
                    .background(LibreCareColors.AccentRed)
            )
            Box(
                modifier = Modifier
                    .weight(inRangeWeight)
                    .fillMaxSize()
                    .background(LibreCareColors.AccentGreen)
            )
            Box(
                modifier = Modifier
                    .weight(aboveWeight)
                    .fillMaxSize()
                    .background(LibreCareColors.AccentAmber)
            )
        }
    }
}

@Composable
private fun NewDataChip(onShowLatest: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LibreCareColors.AccentTeal.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
            .clickable(onClick = onShowLatest)
            .heightIn(min = 44.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics { contentDescription = "Nowe dane. Dotknij, aby przejść do najnowszych odczytów." },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Pojawiły się nowe dane",
            color = DashboardPrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text("Pokaż najnowsze ›", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HomeDataAvailabilityRow(summary: HomeDataSummary) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            summary.primaryText,
            color = DashboardPrimaryText,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        summary.secondaryText?.let {
            Text(
                it,
                color = DashboardSecondaryText,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun HomeChartRangeSelector(
    options: List<HomeChartRangeOption>,
    selectedRange: HomeChartRange,
    onRangeSelected: (HomeChartRange) -> Unit
) {
    val scrollState = rememberScrollState()
    val canScrollRight = scrollState.value < scrollState.maxValue
    val canScrollLeft = scrollState.value > 0

    // Stable (scroll-independent) right edge of the currently selected chip and the viewport width,
    // used to auto-scroll the selected chip to the right when the list overflows the screen.
    var selectedRightPx by remember { mutableStateOf(0) }
    var viewportWidthPx by remember { mutableStateOf(0) }

    // When the scale list is scrollable, keep the highlighted chip aligned to the right edge (so the
    // largest available range stays in view). When it already fits on screen, do nothing.
    LaunchedEffect(selectedRange, scrollState.maxValue, viewportWidthPx, selectedRightPx) {
        if (scrollState.maxValue <= 0 || viewportWidthPx <= 0 || selectedRightPx <= 0) return@LaunchedEffect
        val target = (selectedRightPx - viewportWidthPx).coerceIn(0, scrollState.maxValue)
        scrollState.animateScrollTo(target)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canScrollLeft) {
            Text("‹", color = DashboardSecondaryText, fontSize = 18.sp, modifier = Modifier.padding(end = 2.dp))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState)
                .onSizeChanged { viewportWidthPx = it.width },
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                val range = option.range
                val isSelected = range == selectedRange
                OutlinedButton(
                    onClick = { if (option.enabled) onRangeSelected(range) },
                    enabled = option.enabled,
                    modifier = Modifier
                        .testTag(LibreCareTestTags.rangeChip(range))
                        .widthIn(min = 52.dp)
                        .heightIn(min = 48.dp)
                        .then(
                            if (isSelected) {
                                Modifier.onGloballyPositioned { coords ->
                                    // boundsInParent() is scroll-dependent; add the current scroll to
                                    // recover a stable content offset that does not move while scrolling.
                                    val stableRight = coords.boundsInParent().right + scrollState.value
                                    selectedRightPx = stableRight.roundToInt()
                                }
                            } else {
                                Modifier
                            }
                        )
                        .semantics {
                            contentDescription = if (option.enabled) {
                                range.accessibilityLabel
                            } else {
                                "${range.accessibilityLabel} - za mało danych"
                            }
                            this.selected = isSelected
                        },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            isSelected -> LibreCareColors.AccentTeal
                            option.enabled -> LibreCareColors.SurfaceMuted
                            else -> LibreCareColors.SurfaceMuted.copy(alpha = 0.4f)
                        }
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) LibreCareColors.AccentTeal else Color.Transparent,
                        contentColor = if (isSelected) Color(0xFF082F2D) else LibreCareColors.TextPrimary,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = DashboardMutedText
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(range.shortLabel, fontSize = 13.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (canScrollRight) {
            Text("›", color = AccentGreen, fontSize = 18.sp, modifier = Modifier.padding(start = 2.dp))
        }
    }
}

@Composable
private fun HomeChartNavigator(
    viewport: HistoryViewport,
    availableStart: Instant?,
    availableEnd: Instant?,
    onViewportChanged: (Float) -> Unit
) {
    val safeStart = availableStart ?: viewport.start
    val safeEnd = availableEnd ?: viewport.end
    val fraction = viewportScrollFraction(viewport, safeStart, safeEnd)
    val totalMillis = Duration.between(safeStart, safeEnd).toMillis().coerceAtLeast(1L)
    val rawWindowFraction = (viewport.duration.toMillis().toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    val windowFraction = if (viewport.canPan) rawWindowFraction.coerceAtLeast(0.08f) else 1f

    // The live fraction must not be a pointerInput key, otherwise every emitted drag event would
    // restart the gesture detector and a whole swipe would only advance a few pixels.
    val latestFraction = rememberUpdatedState(fraction)
    val canPan = viewport.canPan
    var dragFraction by remember { mutableStateOf(fraction) }

    Canvas(
        modifier = Modifier
            .testTag(LibreCareTestTags.HOME_CHART_NAVIGATOR)
            .fillMaxWidth()
            .height(20.dp)
            .pointerInput(canPan, windowFraction) {
                if (!canPan) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        val geometry = computeHomeNavigatorGeometry(
                            totalWidthPx = size.width.toFloat(),
                            leftInsetPx = 0f,
                            rightInsetPx = 0f,
                            viewportFraction = latestFraction.value,
                            windowFraction = windowFraction
                        )
                        val insideThumb = offset.x >= geometry.viewportLeft &&
                            offset.x <= geometry.viewportLeft + geometry.viewportWidth
                        dragFraction = if (insideThumb) {
                            latestFraction.value
                        } else {
                            // Tapping the track jumps straight to that position.
                            navigatorFractionForPosition(offset.x, geometry).also(onViewportChanged)
                        }
                    },
                    onDragEnd = { dragFraction = latestFraction.value },
                    onDragCancel = { dragFraction = latestFraction.value }
                ) { change, dragAmount ->
                    val geometry = computeHomeNavigatorGeometry(
                        totalWidthPx = size.width.toFloat(),
                        leftInsetPx = 0f,
                        rightInsetPx = 0f,
                        viewportFraction = dragFraction,
                        windowFraction = windowFraction
                    )
                    dragFraction = navigatorFractionAfterDrag(dragFraction, dragAmount.x, geometry)
                    onViewportChanged(dragFraction)
                    change.consume()
                }
            }
            .pointerInput(canPan, windowFraction) {
                if (!canPan) return@pointerInput
                detectTapGestures { offset ->
                    val geometry = computeHomeNavigatorGeometry(
                        totalWidthPx = size.width.toFloat(),
                        leftInsetPx = 0f,
                        rightInsetPx = 0f,
                        viewportFraction = latestFraction.value,
                        windowFraction = windowFraction
                    )
                    val target = navigatorFractionForPosition(offset.x, geometry)
                    dragFraction = target
                    onViewportChanged(target)
                }
            }
            .semantics {
                contentDescription = "Widoczny przedział czasu: ${buildViewportLabel(viewport.start, viewport.end, DateTimeFormatterProvider.deviceZoneId())}. Przeciągnij, aby przewinąć historię."
            }
    ) {
        val geometry = computeHomeNavigatorGeometry(
            totalWidthPx = size.width,
            leftInsetPx = 0f,
            rightInsetPx = 0f,
            viewportFraction = fraction,
            windowFraction = windowFraction
        )
        // Slim minimap: a thin track with a slightly taller thumb, both vertically centred.
        val trackHeight = 3f
        val trackTop = size.height / 2f - trackHeight / 2f
        val thumbHeight = 11f
        val thumbTop = size.height / 2f - thumbHeight / 2f
        drawRoundRect(
            color = LibreCareColors.SurfaceMuted,
            topLeft = androidx.compose.ui.geometry.Offset(geometry.trackLeft, trackTop),
            size = androidx.compose.ui.geometry.Size(geometry.trackWidth, trackHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f, trackHeight / 2f)
        )
        drawRoundRect(
            color = LibreCareColors.AccentTeal.copy(alpha = 0.28f),
            topLeft = androidx.compose.ui.geometry.Offset(geometry.viewportLeft, thumbTop),
            size = androidx.compose.ui.geometry.Size(geometry.viewportWidth, thumbHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(thumbHeight / 2f, thumbHeight / 2f)
        )
        drawRoundRect(
            color = LibreCareColors.AccentTeal,
            topLeft = androidx.compose.ui.geometry.Offset(geometry.viewportLeft, thumbTop),
            size = androidx.compose.ui.geometry.Size(geometry.viewportWidth, thumbHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(thumbHeight / 2f, thumbHeight / 2f),
            style = Stroke(width = 1.5f)
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
        Text("Brak danych 12 g", color = DashboardPrimaryText, fontWeight = FontWeight.SemiBold)
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


