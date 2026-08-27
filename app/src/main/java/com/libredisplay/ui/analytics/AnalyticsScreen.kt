package com.libredisplay.ui.analytics

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.analytics.BarChartMode
import com.libredisplay.analytics.BarChartWindow
import com.libredisplay.analytics.FourteenDayOverlay
import com.libredisplay.analytics.PeriodMetrics
import com.libredisplay.analytics.RangeBar
import com.libredisplay.ui.monitoring.CompactPersonSwitcherBar
import com.libredisplay.ui.monitoring.DashboardNavItem
import com.libredisplay.ui.monitoring.TopLevelNavigationBar
import com.libredisplay.ui.theme.LibreCareColors
import java.time.ZoneOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataAnalysisScreen(
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
    onOpenHome: () -> Unit = {},
    onOpenFutures: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: DataAnalysisViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val exportEvent by viewModel.exportEvent.collectAsState()
    var showRangeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(exportEvent) {
        val event = exportEvent ?: return@LaunchedEffect
        runCatching {
            val file = java.io.File(event.filePath)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "LibreCare - eksport danych surowych")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(share, "Udostępnij eksport LibreCare"))
        }
        viewModel.consumeExportEvent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analiza danych") },
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
                selected = DashboardNavItem.HISTORIA,
                onOpenHome = onOpenHome,
                onOpenHistory = {},
                onOpenFutures = onOpenFutures,
                onOpenSettings = onOpenSettings
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactPersonSwitcherBar(
                persons = state.persons,
                selectedPatientId = state.selectedPatientId,
                recentPatientIds = emptyList(),
                onPersonSelected = viewModel::onPersonSelected,
                isDemoMode = false,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = LibreCareColors.Surface)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableChip("Cały dzień", !state.nightOnlyEnabled) { viewModel.onNightOnlyChanged(false) }
                SelectableChip("Tylko nocne", state.nightOnlyEnabled) { viewModel.onNightOnlyChanged(true) }
            }

            // ---- Bar chart section ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rozkład czasu w zakresie", color = LibreCareColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                SelectableChip("Dzień", state.barMode == BarChartMode.DAILY) { viewModel.onBarModeChanged(BarChartMode.DAILY) }
                if (state.monthlyAvailable) {
                    SelectableChip("Miesiąc", state.barMode == BarChartMode.MONTHLY) { viewModel.onBarModeChanged(BarChartMode.MONTHLY) }
                }
            }

            state.barWindow?.let { window ->
                Text(window.rangeLabel, color = LibreCareColors.TextSecondary, fontSize = 12.sp)
                RangeBarChart(
                    window = window,
                    selectedIndex = state.selectedBarIndex,
                    onSelect = viewModel::onBarSelected,
                    onScroll = viewModel::onBarScroll
                )
                RangeLegend()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = window.canScrollOlder,
                        onClick = { viewModel.onBarScroll(if (window.mode == BarChartMode.DAILY) 7 else 3) }
                    ) { Text("‹ Starsze") }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = window.canScrollNewer,
                        onClick = { viewModel.onBarScroll(if (window.mode == BarChartMode.DAILY) -7 else -3) }
                    ) { Text("Nowsze ›") }
                }
                window.bars.getOrNull(state.selectedBarIndex)?.let { bar ->
                    Text(barDetail(bar), color = LibreCareColors.TextPrimary, fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = LibreCareColors.Surface)

            // ---- Overlay section ----
            Text("Profil dobowy (nakładka 14 dni)", color = LibreCareColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            OverlayLineChart(state.overlay)
            OverlayLegend()

            if (state.trendObservations.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LibreCareColors.Surface.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Obserwacje", color = LibreCareColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    state.trendObservations.forEach { obs ->
                        Text("• $obs", color = LibreCareColors.TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            HorizontalDivider(color = LibreCareColors.Surface)

            Text(state.customRangeLabel, color = LibreCareColors.TextSecondary, fontSize = 12.sp)
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { showRangeDialog = true }) {
                Text("Ustaw zakres własny (od-do)")
            }

            MetricsTable(metricsByPeriod = state.metricsByPeriod)

            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.exportRawDataToExcel() }) {
                Text("Eksportuj surowe dane do Excela")
            }

            state.infoMessage?.let { Text(it, color = LibreCareColors.TextSecondary, fontSize = 12.sp) }
            Spacer(Modifier.height(8.dp))
        }

        if (showRangeDialog) {
            val startUtc = state.customStart
                ?.atZone(ZoneOffset.UTC)?.toLocalDate()?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
            val endUtc = state.customEnd
                ?.atZone(ZoneOffset.UTC)?.toLocalDate()?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
            val dateRangeState = rememberDateRangePickerState(
                initialSelectedStartDateMillis = startUtc,
                initialSelectedEndDateMillis = endUtc
            )
            DatePickerDialog(
                onDismissRequest = { showRangeDialog = false },
                confirmButton = {
                    Button(onClick = {
                        val start = dateRangeState.selectedStartDateMillis
                        val end = dateRangeState.selectedEndDateMillis
                        if (start != null && end != null) {
                            viewModel.onCustomRangeSelected(PickerUtcDateRange(start, end))
                            showRangeDialog = false
                        }
                    }) { Text("Zastosuj") }
                },
                dismissButton = { OutlinedButton(onClick = { showRangeDialog = false }) { Text("Anuluj") } },
                colors = DatePickerDefaults.colors()
            ) {
                DateRangePicker(state = dateRangeState, title = { Text("Wybierz zakres własny") }, showModeToggle = false)
            }
        }
    }
}

private fun barDetail(bar: RangeBar): String {
    if (!bar.hasData) return "${bar.fullLabel}: brak danych"
    return "${bar.fullLabel}: w zakresie ${bar.inRangePercent}% · poniżej ${bar.belowPercent}% · powyżej ${bar.abovePercent}%" +
        " · śr ${bar.averageGlucose?.roundToInt() ?: "—"} · zakres ${bar.minGlucose}–${bar.maxGlucose}" +
        " · ep. ↓${bar.veryLowEpisodes}/↑${bar.veryHighEpisodes} (${bar.readingsCount} odczytów)"
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) LibreCareColors.AccentTeal.copy(alpha = 0.2f) else LibreCareColors.Surface.copy(alpha = 0.35f),
                RoundedCornerShape(8.dp)
            )
            .border(1.dp, if (selected) LibreCareColors.AccentTeal else LibreCareColors.Surface, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = LibreCareColors.TextPrimary, fontSize = 12.sp)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, color = LibreCareColors.TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun RangeLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendDot(LibreCareColors.AccentRed, "Poniżej")
        LegendDot(LibreCareColors.AccentTeal, "W zakresie")
        LegendDot(LibreCareColors.AccentAmber, "Powyżej")
    }
}

@Composable
private fun OverlayLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendDot(LibreCareColors.TextSecondary.copy(alpha = 0.5f), "dni (cienkie)")
        LegendDot(LibreCareColors.AccentTeal, "średnia (gruba)")
    }
}

@Composable
private fun RangeBarChart(
    window: BarChartWindow,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onScroll: (Int) -> Unit
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val leftInsetPx = with(density) { 30.dp.toPx() }
    val labelStyle = TextStyle(color = LibreCareColors.TextSecondary, fontSize = 9.sp)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(LibreCareColors.Surface.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(6.dp)
            .pointerInput(window.bars.size, window.mode) {
                detectTapGestures { off ->
                    val n = window.bars.size
                    if (n == 0) return@detectTapGestures
                    val slot = (size.width - leftInsetPx) / n
                    val idx = ((off.x - leftInsetPx) / slot).toInt()
                    if (idx in 0 until n) onSelect(idx)
                }
            }
            .pointerInput(window.bars.size, window.mode) {
                var acc = 0f
                detectHorizontalDragGestures(
                    onDragEnd = { acc = 0f },
                    onDragCancel = { acc = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    val n = window.bars.size
                    if (n == 0) return@detectHorizontalDragGestures
                    val slot = ((size.width - leftInsetPx) / n).coerceAtLeast(1f)
                    acc += dragAmount
                    while (acc >= slot) { onScroll(1); acc -= slot }   // drag right -> older
                    while (acc <= -slot) { onScroll(-1); acc += slot } // drag left  -> newer
                }
            }
    ) {
        val topInset = 6f
        val bottomInset = 16f
        val plotW = size.width - leftInsetPx
        val plotH = size.height - topInset - bottomInset
        val grid = LibreCareColors.SurfaceMuted

        listOf(0, 25, 50, 75, 100).forEach { p ->
            val y = topInset + plotH * (1 - p / 100f)
            drawLine(grid, Offset(leftInsetPx, y), Offset(size.width, y), 1f)
            val l = measurer.measure(AnnotatedString("$p"), labelStyle)
            drawText(l, topLeft = Offset(0f, y - l.size.height / 2f))
        }

        val n = window.bars.size
        if (n == 0) return@Canvas
        val slot = plotW / n
        val barW = slot * 0.6f
        window.bars.forEachIndexed { i, bar ->
            val cx = leftInsetPx + i * slot + slot / 2
            val x0 = cx - barW / 2
            if (bar.hasData) {
                val belowFrac = bar.belowPercent / 100f
                val aboveFrac = bar.abovePercent / 100f
                val inFrac = (1f - belowFrac - aboveFrac).coerceAtLeast(0f)
                var yBottom = topInset + plotH
                val hBelow = plotH * belowFrac
                drawRect(LibreCareColors.AccentRed, Offset(x0, yBottom - hBelow), Size(barW, hBelow))
                yBottom -= hBelow
                val hIn = plotH * inFrac
                drawRect(LibreCareColors.AccentTeal, Offset(x0, yBottom - hIn), Size(barW, hIn))
                yBottom -= hIn
                val hAbove = plotH * aboveFrac
                drawRect(LibreCareColors.AccentAmber, Offset(x0, yBottom - hAbove), Size(barW, hAbove))
            } else {
                drawLine(grid, Offset(x0, topInset + plotH), Offset(x0 + barW, topInset + plotH), 2f)
            }
            if (i == selectedIndex) {
                drawRect(
                    color = LibreCareColors.TextPrimary.copy(alpha = 0.5f),
                    topLeft = Offset(x0 - 2f, topInset),
                    size = Size(barW + 4f, plotH),
                    style = Stroke(width = 2f)
                )
            }
            val lbl = measurer.measure(AnnotatedString(bar.label), labelStyle)
            drawText(lbl, topLeft = Offset(cx - lbl.size.width / 2f, topInset + plotH + 2f))
        }
    }
}

@Composable
private fun OverlayLineChart(overlay: FourteenDayOverlay) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val leftInsetPx = with(density) { 30.dp.toPx() }
    val labelStyle = TextStyle(color = LibreCareColors.TextSecondary, fontSize = 9.sp)

    val allValues = overlay.dayLines.flatMap { line -> line.points.map { it.valueMgDl } }
    val dataMin = allValues.minOrNull() ?: 40
    val dataMax = allValues.maxOrNull() ?: 300
    val yMin = minOf(60, dataMin).coerceAtLeast(40)
    val yMax = maxOf(200, dataMax).coerceAtMost(360)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(LibreCareColors.Surface.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(6.dp)
    ) {
        val topInset = 6f
        val bottomInset = 16f
        val plotW = size.width - leftInsetPx
        val plotH = size.height - topInset - bottomInset
        val grid = LibreCareColors.SurfaceMuted

        fun yFor(v: Double): Float {
            val norm = ((v - yMin) / (yMax - yMin)).coerceIn(0.0, 1.0)
            return topInset + plotH * (1f - norm.toFloat())
        }
        fun xFor(minute: Int): Float = leftInsetPx + plotW * (minute.coerceIn(0, 1440) / 1440f)

        val bandTop = yFor(180.0)
        val bandBottom = yFor(80.0)
        drawRect(LibreCareColors.AccentTeal.copy(alpha = 0.10f), Offset(leftInsetPx, bandTop), Size(plotW, bandBottom - bandTop))

        listOf(yMin, (yMin + yMax) / 2, yMax).forEach { v ->
            val y = yFor(v.toDouble())
            drawLine(grid, Offset(leftInsetPx, y), Offset(size.width, y), 1f)
            val l = measurer.measure(AnnotatedString("$v"), labelStyle)
            drawText(l, topLeft = Offset(0f, y - l.size.height / 2f))
        }

        listOf(0, 6, 12, 18, 24).forEach { h ->
            val x = xFor(h * 60)
            drawLine(grid.copy(alpha = 0.4f), Offset(x, topInset), Offset(x, topInset + plotH), 1f)
            val l = measurer.measure(AnnotatedString("%02d".format(h % 24)), labelStyle)
            drawText(l, topLeft = Offset(x - l.size.width / 2f, topInset + plotH + 2f))
        }

        overlay.dayLines.forEach { line ->
            if (line.points.size < 2) return@forEach
            val path = Path()
            line.points.sortedBy { it.minuteOfDay }.forEachIndexed { i, p ->
                val x = xFor(p.minuteOfDay); val y = yFor(p.valueMgDl.toDouble())
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, LibreCareColors.TextSecondary.copy(alpha = 0.25f), style = Stroke(width = 1f))
        }
        if (overlay.averageLine.size >= 2) {
            val avg = Path()
            overlay.averageLine.sortedBy { it.minuteOfDay }.forEachIndexed { i, p ->
                val x = xFor(p.minuteOfDay); val y = yFor(p.averageMgDl)
                if (i == 0) avg.moveTo(x, y) else avg.lineTo(x, y)
            }
            drawPath(avg, LibreCareColors.AccentTeal, style = Stroke(width = 3f))
        }
    }
}

@Composable
private fun MetricsTable(metricsByPeriod: Map<AnalysisPeriod, PeriodMetrics>) {
    val periods = AnalysisPeriod.entries
    val rows = listOf(
        "TIR" to { m: PeriodMetrics -> m.tirPercent?.let { "$it%" } ?: "—" },
        "Poniżej" to { m: PeriodMetrics -> m.belowPercent?.let { "$it%" } ?: "—" },
        "Powyżej" to { m: PeriodMetrics -> m.abovePercent?.let { "$it%" } ?: "—" },
        "Średnia" to { m: PeriodMetrics -> m.averageGlucose?.let { "%.0f".format(it) } ?: "—" },
        "CV" to { m: PeriodMetrics -> m.cvPercent?.let { "${"%.1f".format(it)}%" } ?: "—" },
        "GMI" to { m: PeriodMetrics -> m.gmiPercent?.let { "${"%.1f".format(it)}%" } ?: "—" },
        "Min" to { m: PeriodMetrics -> m.minGlucose?.toString() ?: "—" },
        "Max" to { m: PeriodMetrics -> m.maxGlucose?.toString() ?: "—" },
        "Ep. niskie" to { m: PeriodMetrics -> m.veryLowEpisodes.toString() },
        "Ep. wysokie" to { m: PeriodMetrics -> m.veryHighEpisodes.toString() },
        "Aktywność" to { m: PeriodMetrics -> m.sensorActivityPercent?.let { "${"%.0f".format(it)}%" } ?: "—" }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(LibreCareColors.Surface.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.width(112.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HeaderCell("Metryka")
            rows.forEach { (label, _) ->
                MetricCell(label, bold = true)
            }
        }
        periods.forEach { period ->
            val metrics = metricsByPeriod[period] ?: PeriodMetrics.empty
            Column(
                modifier = Modifier.width(82.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HeaderCell(period.label)
                rows.forEach { (_, valueResolver) ->
                    MetricCell(valueResolver(metrics), alignEnd = true)
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String) {
    Box(modifier = Modifier.height(22.dp), contentAlignment = Alignment.CenterEnd) {
        Text(
            text = text,
            color = LibreCareColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MetricCell(text: String, bold: Boolean = false, alignEnd: Boolean = false) {
    Box(
        modifier = Modifier.height(22.dp).fillMaxWidth(),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = LibreCareColors.TextPrimary,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
