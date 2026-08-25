package com.libredisplay.ui.monitoring

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.diagnostics.DiagnosticLogger
import com.libredisplay.ui.theme.LibreCareColors
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FullScreenGlucoseChartScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonitoringViewModel = viewModel(),
    initialRange: TimeRange = TimeRange.LAST_24_HOURS
) {
    val state by viewModel.uiState.collectAsState()
    val detailedHistory by viewModel.detailedHistory.collectAsState()
    val reading = state.reading
    val zoneId = DateTimeFormatterProvider.deviceZoneId()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var range by remember(state.selectedPatientId) { mutableStateOf(initialRange) }
    var selectedPoint by remember { mutableStateOf<GlucoseHistoryPoint?>(null) }
    var viewportStartMillis by remember(state.selectedPatientId, range) { mutableStateOf<Long?>(null) }
    var viewportDurationMillis by remember(state.selectedPatientId, range) { mutableStateOf(range.duration.toMillis()) }
    var chartWidthPx by remember { mutableStateOf(1f) }

    // Local ticker for coverage countdown – no network needed.
    var currentTime by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            currentTime = Instant.now()
        }
    }

    val history = remember(detailedHistory, reading) {
        if (detailedHistory.isNotEmpty()) detailedHistory else reading?.let(::readingTimeline).orEmpty()
    }
    val end = history.maxOfOrNull { it.timestamp } ?: Instant.now()
    val start = end.minus(range.duration)
    val visible = remember(history, start, end) {
        history.filter { !it.timestamp.isBefore(start) && !it.timestamp.isAfter(end) }
    }
    val zoomEnabled = range.duration >= Duration.ofDays(7)
    val minViewportDuration = remember(range) { minimumViewportDuration(range) }

    LaunchedEffect(range, state.selectedPatientId, state.lastMeasurementTimestamp) {
        viewModel.loadDetailedHistory(range)
    }

    LaunchedEffect(range, state.selectedPatientId, end) {
        viewportStartMillis = start.toEpochMilli()
        viewportDurationMillis = range.duration.toMillis()
    }

    val viewport = remember(start, end, viewportStartMillis, viewportDurationMillis, minViewportDuration) {
        buildViewport(
            selectedRangeStart = start,
            selectedRangeEnd = end,
            requestedStartMillis = viewportStartMillis,
            requestedDurationMillis = viewportDurationMillis,
            minimumDuration = minViewportDuration
        )
    }
    val viewportPoints = remember(visible, viewport) {
        visible.filter { !it.timestamp.isBefore(viewport.start) && !it.timestamp.isAfter(viewport.end) }
    }

    // Coverage model: separates SELECTED range from AVAILABLE data.
    // 'currentTime' in key so countdown ticks locally.
    val coverage = remember(visible, range, currentTime) {
        computeDataCoverage(
            history = visible,
            selectedRange = range.duration,
            selectedRangeLabel = range.toSelectedRangeLabel(),
            now = currentTime
        )
    }

    val chartPoints = remember(viewportPoints, range, state.settings.targetLow, state.settings.targetHigh) {
        buildHistoryChartSeries(
            readings = viewportPoints,
            timeRange = range,
            targetLow = state.settings.targetLow,
            targetHigh = state.settings.targetHigh
        )
    }
    val extremes = remember(chartPoints) { calculateChartExtremes(chartPoints) }
    val legendRows = remember(viewportPoints, state.settings.targetLow, state.settings.targetHigh) {
        historyLegendRows(viewportPoints, state.settings.targetLow, state.settings.targetHigh)
    }
    val viewportLabel = remember(viewport) {
        buildViewportLabel(viewport.start, viewport.end, zoneId)
    }
    val stats = remember(viewportPoints, viewport, range, state.settings.targetLow, state.settings.targetHigh, coverage) {
        historyStatsSection(
            history = viewportPoints,
            rangeLabel = viewportLabel,
            targetLow = state.settings.targetLow,
            targetHigh = state.settings.targetHigh,
            coverage = if (viewport.isZoomed) null else coverage
        )
    }

    LaunchedEffect(range, state.selectedPatientId) {
        DiagnosticLogger.logInfo("FullScreenHistory", "CHART RANGE CHANGED range=${range.name}")
    }

    LaunchedEffect(chartPoints, range, state.selectedPatientId, viewport) {
        selectedPoint = chartPoints.lastOrNull()
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        if (!zoomEnabled) return@rememberTransformableState
        val updated = applyViewportTransform(
            currentStartMillis = viewport.start.toEpochMilli(),
            currentDurationMillis = viewport.duration.toMillis(),
            selectedRangeStart = start,
            selectedRangeEnd = end,
            zoomChange = zoomChange,
            panXPx = panChange.x,
            chartWidthPx = chartWidthPx,
            minimumDuration = minViewportDuration
        )
        viewportStartMillis = updated.first
        viewportDurationMillis = updated.second
    }

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.selectedPersonName?.let { "Historia glikemii - $it" } ?: "Historia glikemii",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zamknij historię")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeRangeSelector(range = range, onRangeSelected = { range = it })

             Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                 Text(coverage.sectionHeaderLabel, color = Color(0xFFCBD5E1), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                 coverage.selectedRangeNote?.let { note ->
                     Text(note, color = Color(0xFF64748B), fontSize = 11.sp)
                 }
                Text(
                    if (zoomEnabled) "Przeciągnij po wykresie, aby podejrzeć punkt. Uszczypnij, aby przybliżyć, a potem przesuwaj suwak poniżej."
                    else "Przeciągnij po wykresie, aby podejrzeć punkt.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                if (viewport.isZoomed) {
                    Text(
                        "Widoczne okno: $viewportLabel",
                        color = LibreCareColors.AccentTeal,
                        fontSize = 11.sp
                    )
                }
                if (chartPoints.isEmpty()) {
                    LocalEmptyChartState()
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { chartWidthPx = it.width.toFloat() }
                            .transformable(state = transformState, enabled = zoomEnabled)
                    ) {
                        GlucoseChart(
                            points = chartPoints,
                            targetLow = state.settings.targetLow,
                            targetHigh = state.settings.targetHigh,
                            zoneId = zoneId,
                            selectedPoint = selectedPoint,
                            onPointSelected = { point -> selectedPoint = point },
                            onPointSelectionCleared = { selectedPoint = null },
                            chartHeight = if (isLandscape) 300.dp else 380.dp,
                            maxVisiblePoints = 1_500,
                            axisRightPaddingPx = 44f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (zoomEnabled) {
                        Slider(
                            value = viewportScrollFraction(viewport, start, end),
                            onValueChange = { fraction ->
                                viewportStartMillis = viewportStartForFraction(fraction, viewport, start, end)
                            },
                            enabled = viewport.canPan,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                coverage.fullCoverageEstimate?.let { estimate ->
                    Text(
                        text = estimate,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            androidx.compose.material3.HorizontalDivider(color = LibreCareColors.Surface)

            if (extremes.minimumReading != null && extremes.maximumReading != null) {
                val averageValue = viewportPoints.map { it.value }.average().takeIf { it.isFinite() } ?: 0.0
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactStatistic("MIN", "${extremes.minimumReading.value}", "mg/dL", Modifier.weight(1f))
                    CompactStatistic("ŚREDNIA", "${averageValue.toInt()}", "mg/dL", Modifier.weight(1f))
                    CompactStatistic("MAX", "${extremes.maximumReading.value}", "mg/dL", Modifier.weight(1f))
                }
            }

            selectedPoint?.let { point ->
                val label = formatChartPointLabel(
                    point = point,
                    targetLow = state.settings.targetLow,
                    targetHigh = state.settings.targetHigh,
                    zoneId = zoneId
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LibreCareColors.Surface.copy(alpha = 0.35f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(label.valueText, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(label.dateTime, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(label.statusText, color = Color(0xFFCBD5E1))
                    label.trendText?.let { Text(it, color = Color(0xFFCBD5E1), fontSize = 12.sp) }
                }
            }

            // Range distribution bar
            var showLegendDetails by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLegendDetails = !showLegendDetails }
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Czas w zakresach", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = if (coverage.hasFullCoverage) coverage.selectedRangeLabel
                                   else "${coverage.availableSpanLabel} danych",
                            color = LibreCareColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        if (showLegendDetails) "▼" else "▶",
                        color = LibreCareColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    legendRows.forEach { row ->
                        if (row.hasData && row.percent > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(row.percent.toFloat())
                                    .fillMaxHeight()
                                    .background(row.color)
                            )
                        }
                    }
                }

                if (showLegendDetails) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        legendRows.forEach { row ->
                            if (row.hasData) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("●", color = row.color, fontSize = 12.sp, modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(row.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${row.durationLabel} · ${row.percentLabel}",
                                            color = LibreCareColors.TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                androidx.compose.material3.HorizontalDivider(color = LibreCareColors.Surface)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stats.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.cards.forEach { card ->
                        HistoryStatCard(card)
                    }
                }
            }

            // Notes/Events section intentionally hidden until full functionality is implemented.
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TimeRangeSelector(range: TimeRange, onRangeSelected: (TimeRange) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        historySelectableRanges().forEach { candidate ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onRangeSelected(candidate) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = candidate.shortLabel,
                    color = if (candidate == range) LibreCareColors.TextPrimary else LibreCareColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (candidate == range) FontWeight.SemiBold else FontWeight.Normal
                )
                if (candidate == range) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(width = 22.dp, height = 2.dp)
                            .background(LibreCareColors.AccentTeal)
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
    HorizontalDivider(color = LibreCareColors.Surface, modifier = Modifier.padding(horizontal = 4.dp))
}

internal fun historySelectableRanges(): List<TimeRange> = listOf(
    TimeRange.LAST_3_HOURS,
    TimeRange.LAST_6_HOURS,
    TimeRange.LAST_12_HOURS,
    TimeRange.LAST_24_HOURS,
    TimeRange.LAST_1_DAY,
    TimeRange.LAST_3_DAYS,
    TimeRange.LAST_7_DAYS,
    TimeRange.LAST_30_DAYS,
    TimeRange.LAST_90_DAYS,
    TimeRange.LAST_365_DAYS
)

@Composable
private fun LocalEmptyChartState() {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Brak danych 12 h", color = Color.White, fontWeight = FontWeight.SemiBold)
        Text("Nie mam jeszcze lokalnej historii dla tego zakresu.", color = Color(0xFF9CA3AF), fontSize = 12.sp)
        Text("Odśwież dane albo poczekaj na synchronizację.", color = Color(0xFF9CA3AF), fontSize = 12.sp)
    }
}

@Composable
private fun CompactStatistic(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            label,
            color = LibreCareColors.TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            value,
            color = LibreCareColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            unit,
            color = LibreCareColors.TextSecondary,
            fontSize = 9.sp
        )
    }
}

@Deprecated("Używaj CompactStatistic zamiast")
@Composable
private fun ChartSummaryCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), modifier = modifier.height(90.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, color = Color(0xFFCBD5E1), fontSize = 12.sp)
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Deprecated("Używaj kompaktowego paska rozkładu zamiast")
@Composable
private fun HistoryLegendRow(row: HistoryLegendRowUi) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("●", color = row.color, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(row.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(row.threshold, color = LibreCareColors.TextSecondary, fontSize = 11.sp)
        }
        Text(row.durationLabel, color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(row.percentLabel, color = LibreCareColors.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun HistoryStatCard(card: HistoryStatCardUi) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .background(LibreCareColors.Surface.copy(alpha = 0.28f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MetricLabelWithTooltip(
            label = card.label,
            tooltipTitle = card.tooltipTitle,
            tooltipExplanation = card.tooltipExplanation,
            tooltipFormula = card.tooltipFormula,
            modifier = Modifier.fillMaxWidth()
        )
        Text(card.value, color = card.accent, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 2)
        Text(card.supportingText, color = LibreCareColors.TextMuted, fontSize = 11.sp)
    }
}

private fun buildHistoryChartSeries(
    readings: List<GlucoseHistoryPoint>,
    timeRange: TimeRange,
    targetLow: Int,
    targetHigh: Int
): List<GlucoseHistoryPoint> {
    if (readings.isEmpty()) return emptyList()
    return readings
        .distinctBy { it.timestamp to it.value }
        .sortedBy { it.timestamp }
}

internal data class HistoryViewport(
    val start: Instant,
    val end: Instant,
    val duration: Duration,
    val isZoomed: Boolean,
    val canPan: Boolean
)

internal fun minimumViewportDuration(range: TimeRange): Duration = when {
    range.duration >= Duration.ofDays(7) -> Duration.ofDays(1)
    else -> range.duration
}

internal fun buildViewport(
    selectedRangeStart: Instant,
    selectedRangeEnd: Instant,
    requestedStartMillis: Long?,
    requestedDurationMillis: Long,
    minimumDuration: Duration
): HistoryViewport {
    val fullStartMillis = selectedRangeStart.toEpochMilli()
    val fullEndMillis = selectedRangeEnd.toEpochMilli().coerceAtLeast(fullStartMillis + 1L)
    val totalDurationMillis = (fullEndMillis - fullStartMillis).coerceAtLeast(1L)
    val minDurationMillis = minimumDuration.toMillis().coerceAtMost(totalDurationMillis).coerceAtLeast(1L)
    val durationMillis = requestedDurationMillis.coerceIn(minDurationMillis, totalDurationMillis)
    val maxStartMillis = (fullEndMillis - durationMillis).coerceAtLeast(fullStartMillis)
    val startMillis = (requestedStartMillis ?: fullStartMillis).coerceIn(fullStartMillis, maxStartMillis)
    return HistoryViewport(
        start = Instant.ofEpochMilli(startMillis),
        end = Instant.ofEpochMilli(startMillis + durationMillis),
        duration = Duration.ofMillis(durationMillis),
        isZoomed = durationMillis < totalDurationMillis,
        canPan = maxStartMillis > fullStartMillis
    )
}

internal fun applyViewportTransform(
    currentStartMillis: Long,
    currentDurationMillis: Long,
    selectedRangeStart: Instant,
    selectedRangeEnd: Instant,
    zoomChange: Float,
    panXPx: Float,
    chartWidthPx: Float,
    minimumDuration: Duration
): Pair<Long, Long> {
    val fullStartMillis = selectedRangeStart.toEpochMilli()
    val fullEndMillis = selectedRangeEnd.toEpochMilli().coerceAtLeast(fullStartMillis + 1L)
    val fullDurationMillis = (fullEndMillis - fullStartMillis).coerceAtLeast(1L)
    val minDurationMillis = minimumDuration.toMillis().coerceAtMost(fullDurationMillis).coerceAtLeast(1L)
    val safeZoom = zoomChange.takeIf { it.isFinite() && it > 0f } ?: 1f
    val targetDurationMillis = (currentDurationMillis / safeZoom).roundToLong().coerceIn(minDurationMillis, fullDurationMillis)
    val centerMillis = currentStartMillis + currentDurationMillis / 2L
    val centeredStartMillis = (centerMillis - targetDurationMillis / 2L)
        .coerceIn(fullStartMillis, (fullEndMillis - targetDurationMillis).coerceAtLeast(fullStartMillis))
    val panMillis = if (chartWidthPx > 0f && panXPx.isFinite()) {
        (-(panXPx / chartWidthPx) * targetDurationMillis.toDouble()).roundToLong()
    } else {
        0L
    }
    val shiftedStartMillis = (centeredStartMillis + panMillis)
        .coerceIn(fullStartMillis, (fullEndMillis - targetDurationMillis).coerceAtLeast(fullStartMillis))
    return shiftedStartMillis to targetDurationMillis
}

internal fun viewportScrollFraction(
    viewport: HistoryViewport,
    selectedRangeStart: Instant,
    selectedRangeEnd: Instant
): Float {
    val fullStartMillis = selectedRangeStart.toEpochMilli()
    val maxStartMillis = selectedRangeEnd.toEpochMilli() - viewport.duration.toMillis()
    val denominator = (maxStartMillis - fullStartMillis).coerceAtLeast(1L)
    return ((viewport.start.toEpochMilli() - fullStartMillis).toFloat() / denominator.toFloat()).coerceIn(0f, 1f)
}

internal fun viewportStartForFraction(
    fraction: Float,
    viewport: HistoryViewport,
    selectedRangeStart: Instant,
    selectedRangeEnd: Instant
): Long {
    val fullStartMillis = selectedRangeStart.toEpochMilli()
    val maxStartMillis = (selectedRangeEnd.toEpochMilli() - viewport.duration.toMillis()).coerceAtLeast(fullStartMillis)
    return (
        fullStartMillis.toDouble() +
            (maxStartMillis - fullStartMillis).toDouble() * fraction.coerceIn(0f, 1f).toDouble()
        ).roundToLong()
}

internal fun buildViewportLabel(start: Instant, end: Instant, zoneId: java.time.ZoneId): String {
    return "${PolishDateTimeFormatter.formatAbsolute(start, zoneId)} – ${PolishDateTimeFormatter.formatAbsolute(end, zoneId)}"
}

