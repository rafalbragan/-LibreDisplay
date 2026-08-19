package com.libredisplay.ui.monitoring

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.diagnostics.DiagnosticLogger
import com.libredisplay.ui.theme.LibreCareColors
import java.time.Instant
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FullScreenGlucoseChartScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonitoringViewModel = viewModel(),
    initialRange: TimeRange = TimeRange.LAST_24_HOURS
) {
    val state by viewModel.uiState.collectAsState()
    val reading = state.reading
    val zoneId = DateTimeFormatterProvider.deviceZoneId()

    var range by remember(state.selectedPatientId) { mutableStateOf(initialRange) }
    var selectedPoint by remember { mutableStateOf<GlucoseHistoryPoint?>(null) }

    val history = reading?.let(::readingTimeline).orEmpty()
    val end = history.maxOfOrNull { it.timestamp } ?: Instant.now()
    val start = end.minus(range.duration)
    val visible = history.filter { !it.timestamp.isBefore(start) && !it.timestamp.isAfter(end) }
    val chartPoints = remember(visible, range, state.settings.targetLow, state.settings.targetHigh) {
        buildHistoryChartSeries(
            readings = visible,
            timeRange = range,
            targetLow = state.settings.targetLow,
            targetHigh = state.settings.targetHigh
        )
    }
    val extremes = remember(chartPoints) { calculateChartExtremes(chartPoints) }
    val legendRows = remember(visible, state.settings.targetLow, state.settings.targetHigh) {
        historyLegendRows(visible, state.settings.targetLow, state.settings.targetHigh)
    }
    val stats = remember(visible, range, state.settings.targetLow, state.settings.targetHigh) {
        historyStatsSection(
            history = visible,
            rangeLabel = range.label,
            targetLow = state.settings.targetLow,
            targetHigh = state.settings.targetHigh
        )
    }
    val eventItems = remember { placeholderHistoryEvents() }

    LaunchedEffect(range, state.selectedPatientId) {
        DiagnosticLogger.logInfo("FullScreenHistory", "CHART RANGE CHANGED range=${range.name}")
    }

    LaunchedEffect(chartPoints, range, state.selectedPatientId) {
        selectedPoint = chartPoints.lastOrNull()
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimeRangeSelector(range = range, onRangeSelected = { range = it })

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(range.label, color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Text("Przeciągnij po wykresie, aby podejrzeć punkt.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    if (chartPoints.isEmpty()) {
                        LocalEmptyChartState()
                    } else {
                        GlucoseChart(
                            points = chartPoints,
                            targetLow = state.settings.targetLow,
                            targetHigh = state.settings.targetHigh,
                            zoneId = zoneId,
                            selectedPoint = selectedPoint,
                            onPointSelected = { point -> selectedPoint = point },
                            onPointSelectionCleared = { selectedPoint = null },
                            chartHeight = 380.dp,
                            maxVisiblePoints = 280,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (extremes.minimumReading != null && extremes.maximumReading != null) {
                val averageValue = visible.map { it.value }.average().takeIf { it.isFinite() } ?: 0.0
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
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(label.valueText, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(label.dateTime, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(label.statusText, color = Color(0xFFCBD5E1))
                        label.trendText?.let { Text(it, color = Color(0xFFCBD5E1), fontSize = 12.sp) }
                    }
                }
            }

            // Range distribution bar
            var showLegendDetails by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = LibreCareColors.Surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLegendDetails = !showLegendDetails }
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rozkład zakresów", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            if (showLegendDetails) "▼" else "▶",
                            color = LibreCareColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Compact bar representation
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

                    // Detailed legend (shown when expanded)
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
                }
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
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(
            TimeRange.LAST_3_HOURS,
            TimeRange.LAST_6_HOURS,
            TimeRange.LAST_12_HOURS,
            TimeRange.LAST_24_HOURS,
            TimeRange.LAST_7_DAYS,
            TimeRange.LAST_30_DAYS,
            TimeRange.LAST_90_DAYS,
            TimeRange.LAST_365_DAYS
        ).forEach { candidate ->
            if (candidate == range) {
                TextButton(onClick = { onRangeSelected(candidate) }) { Text(candidate.shortLabel) }
            } else {
                OutlinedButton(onClick = { onRangeSelected(candidate) }) { Text(candidate.shortLabel) }
            }
        }
    }
}

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
    Card(
        colors = CardDefaults.cardColors(containerColor = LibreCareColors.SurfaceElevated),
        modifier = Modifier.width(160.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(card.label, color = LibreCareColors.TextSecondary, fontSize = 12.sp)
            Text(card.value, color = card.accent, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 2)
            Text(card.supportingText, color = LibreCareColors.TextMuted, fontSize = 11.sp)
        }
    }
}

private fun buildHistoryChartSeries(
    readings: List<GlucoseHistoryPoint>,
    timeRange: TimeRange,
    targetLow: Int,
    targetHigh: Int
): List<GlucoseHistoryPoint> {
    if (readings.isEmpty()) return emptyList()
    if (chartModeForRange(timeRange) == "line") {
        return downsampleHistoryPreservingExtremes(readings, maxPoints = 280)
    }
    val series = aggregateReadingsForRange(
        readings = readings,
        timeRange = timeRange,
        bucketSize = bucketSizeForRange(timeRange),
        targetLow = targetLow,
        targetHigh = targetHigh
    )
    val points = series.buckets.mapNotNull { bucket ->
        bucket.averageGlucoseMgDl?.roundToInt()?.let { value ->
            GlucoseHistoryPoint(value = value, timestamp = bucket.start, trend = GlucoseTrend.UNKNOWN)
        }
    }
    return downsampleHistoryPreservingExtremes(points, maxPoints = 280)
}

