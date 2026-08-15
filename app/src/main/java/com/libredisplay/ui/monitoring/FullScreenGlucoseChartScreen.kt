package com.libredisplay.ui.monitoring

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.diagnostics.DiagnosticLogger
import java.time.Instant
import java.time.ZoneId

private enum class HistoryMetricMode(val label: String) {
    TIR("TIR"),
    BELOW("Poniżej"),
    ABOVE("Powyżej"),
    ACTIVITY("Aktywność"),
    AVG("Średnia"),
    GMI("GMI")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenGlucoseChartScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonitoringViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val reading = state.reading

    var range by remember { mutableStateOf(TimeRange.LAST_24_HOURS) }
    var metricMode by remember { mutableStateOf(HistoryMetricMode.TIR) }
    var selectedPoint by remember { mutableStateOf<GlucoseHistoryPoint?>(null) }

    val history = reading?.history.orEmpty()
    val end = history.maxOfOrNull { it.timestamp } ?: Instant.now()
    val start = end.minus(range.duration)
    val visible = history.filter { !it.timestamp.isBefore(start) && !it.timestamp.isAfter(end) }

    val chartMode = chartModeForRange(range)

    LaunchedEffect(range, state.selectedPatientId, chartMode) {
        DiagnosticLogger.logInfo("FullScreenHistory", "CHART RANGE CHANGED range=${range.name}")
        DiagnosticLogger.logInfo("FullScreenHistory", "CHART MODE $chartMode")
    }

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.selectedPersonName?.let { "Historia - $it" } ?: "Historia glukozy",
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

            if (chartMode == "bar") {
                MetricModeSelector(mode = metricMode, onSelected = { metricMode = it })
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(range.label, color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    when {
                        visible.isEmpty() -> {
                            LocalEmptyChartState()
                        }
                        chartMode == "line" -> {
                            GlucoseChart(
                                points = visible,
                                targetLow = state.settings.targetLow,
                                targetHigh = state.settings.targetHigh,
                                selectedPoint = selectedPoint,
                                onPointSelected = { point -> selectedPoint = point },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        chartMode == "aggregated" -> {
                            val series = aggregateReadingsForRange(
                                readings = visible,
                                timeRange = range,
                                bucketSize = bucketSizeForRange(range),
                                targetLow = state.settings.targetLow,
                                targetHigh = state.settings.targetHigh
                            )
                            Text("Tryb agregowany (${series.buckets.size} bucketów)", color = Color(0xFFCBD5E1))
                            series.buckets.take(10).forEach { bucket ->
                                Text(
                                    "${LocalDateTimeLabel.format(bucket.start)} avg=${bucket.averageGlucoseMgDl?.let { "%.0f".format(it) } ?: "—"}",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        else -> {
                            val series = aggregateReadingsForRange(
                                readings = visible,
                                timeRange = range,
                                bucketSize = bucketSizeForRange(range),
                                targetLow = state.settings.targetLow,
                                targetHigh = state.settings.targetHigh
                            )
                            Text("Tryb statystyczny: ${metricMode.label}", color = Color(0xFFCBD5E1))
                            series.buckets.take(10).forEach { bucket ->
                                val value = when (metricMode) {
                                    HistoryMetricMode.TIR -> bucket.inRangePercent
                                    HistoryMetricMode.BELOW -> bucket.belowRangePercent
                                    HistoryMetricMode.ABOVE -> bucket.aboveRangePercent
                                    HistoryMetricMode.ACTIVITY -> bucket.sensorActivityPercent
                                    HistoryMetricMode.AVG -> bucket.averageGlucoseMgDl
                                    HistoryMetricMode.GMI -> bucket.gmiPercent
                                }
                                Text(
                                    "${LocalDateTimeLabel.format(bucket.start)}: ${value?.let { "%.1f".format(it) } ?: "—"}",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            selectedPoint?.let { point ->
                val label = formatChartPointLabel(
                    point = point,
                    targetLow = state.settings.targetLow,
                    targetHigh = state.settings.targetHigh,
                    zoneId = ZoneId.systemDefault()
                )
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(label.dateTime, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(label.valueText, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(label.statusText, color = Color(0xFFCBD5E1))
                        label.trendText?.let { Text(it, color = Color(0xFFCBD5E1), fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TimeRangeSelector(range: TimeRange, onRangeSelected: (TimeRange) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TimeRange.entries.forEach { candidate ->
            if (candidate == range) {
                TextButton(onClick = { onRangeSelected(candidate) }) { Text(candidate.shortLabel) }
            } else {
                OutlinedButton(onClick = { onRangeSelected(candidate) }) { Text(candidate.shortLabel) }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MetricModeSelector(mode: HistoryMetricMode, onSelected: (HistoryMetricMode) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HistoryMetricMode.entries.forEach { candidate ->
            if (candidate == mode) {
                TextButton(onClick = { onSelected(candidate) }) { Text(candidate.label) }
            } else {
                OutlinedButton(onClick = { onSelected(candidate) }) { Text(candidate.label) }
            }
        }
    }
}

private object LocalDateTimeLabel {
    private val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    fun format(instant: Instant): String = formatter.withZone(ZoneId.systemDefault()).format(instant)
}

@Composable
private fun LocalEmptyChartState() {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Brak danych 12 h", color = Color.White, fontWeight = FontWeight.SemiBold)
        Text("Nie mam jeszcze lokalnej historii dla tego zakresu.", color = Color(0xFF9CA3AF), fontSize = 12.sp)
        Text("Odśwież dane albo poczekaj na synchronizację.", color = Color(0xFF9CA3AF), fontSize = 12.sp)
    }
}
