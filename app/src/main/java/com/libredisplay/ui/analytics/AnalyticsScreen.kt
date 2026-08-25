package com.libredisplay.ui.analytics

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.analytics.FourteenDayOverlay
import com.libredisplay.analytics.PeriodMetrics
import com.libredisplay.analytics.WeeklyRangeBar
import com.libredisplay.ui.monitoring.CompactPersonSwitcherBar
import com.libredisplay.ui.monitoring.DashboardNavItem
import com.libredisplay.ui.monitoring.TopLevelNavigationBar
import com.libredisplay.ui.theme.LibreCareColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataAnalysisScreen(
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
    onOpenHome: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: DataAnalysisViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val exportEvent by viewModel.exportEvent.collectAsState()

    LaunchedEffect(exportEvent) {
        val event = exportEvent ?: return@LaunchedEffect
        runCatching {
            val file = java.io.File(event.filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

            Text(
                text = state.customRangeLabel,
                color = LibreCareColors.TextSecondary,
                fontSize = 12.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    label = "Cały dzień",
                    selected = !state.nightOnlyEnabled,
                    onClick = { viewModel.onNightOnlyChanged(false) }
                )
                FilterChip(
                    label = "Tylko nocne",
                    selected = state.nightOnlyEnabled,
                    onClick = { viewModel.onNightOnlyChanged(true) }
                )
            }

            Text("Tygodniowy rozkład zakresów", color = LibreCareColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            WeeklyStackedChart(state.weeklyBars)

            Text("Nakładka 14 dni", color = LibreCareColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            FourteenDayOverlayChart(state.overlay14Days)

            MetricsTable(metricsByPeriod = state.metricsByPeriod)

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.exportRawDataToExcel() }
            ) {
                Text("Eksportuj surowe dane do Excela")
            }

            state.infoMessage?.let {
                Text(it, color = LibreCareColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun WeeklyStackedChart(bars: List<WeeklyRangeBar>) {
    val safeBars = if (bars.isEmpty()) List(7) { null } else bars.map { it }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(LibreCareColors.Surface.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        if (safeBars.isEmpty()) return@Canvas
        val slotWidth = size.width / safeBars.size
        val barWidth = slotWidth * 0.55f
        safeBars.forEachIndexed { index, bar ->
            val x = index * slotWidth + (slotWidth - barWidth) / 2f
            val totalHeight = size.height
            val belowHeight = totalHeight * (((bar?.belowPercent ?: 0).coerceIn(0, 100)) / 100f)
            val inRangeHeight = totalHeight * (((bar?.inRangePercent ?: 0).coerceIn(0, 100)) / 100f)
            val aboveHeight = totalHeight * (((bar?.abovePercent ?: 0).coerceIn(0, 100)) / 100f)
            val stackedHeight = (belowHeight + inRangeHeight + aboveHeight).coerceAtMost(totalHeight)
            var y = size.height
            drawRect(
                color = LibreCareColors.AccentRed,
                topLeft = Offset(x, y - belowHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, belowHeight)
            )
            y -= belowHeight
            drawRect(
                color = LibreCareColors.AccentTeal,
                topLeft = Offset(x, y - inRangeHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, inRangeHeight)
            )
            y -= inRangeHeight
            drawRect(
                color = LibreCareColors.AccentAmber,
                topLeft = Offset(x, y - aboveHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, aboveHeight)
            )
            if (stackedHeight < 1f) {
                drawRect(
                    color = LibreCareColors.Surface,
                    topLeft = Offset(x, size.height - 1f),
                    size = androidx.compose.ui.geometry.Size(barWidth, 1f)
                )
            }
        }
    }
}

@Composable
private fun FourteenDayOverlayChart(overlay: FourteenDayOverlay) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(LibreCareColors.Surface.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        val dayColor = LibreCareColors.TextSecondary.copy(alpha = 0.3f)
        val avgColor = LibreCareColors.AccentTeal
        val minY = 40f
        val maxY = 320f
        fun xForMinute(minute: Int): Float = (minute.coerceIn(0, 1439) / 1439f) * size.width
        fun yForValue(value: Double): Float {
            val normalized = ((value - minY) / (maxY - minY)).coerceIn(0.0, 1.0)
            return size.height - (normalized.toFloat() * size.height)
        }

        overlay.dayLines.forEach { line ->
            if (line.points.size < 2) return@forEach
            val path = Path()
            line.points.sortedBy { it.minuteOfDay }.forEachIndexed { index, point ->
                val x = xForMinute(point.minuteOfDay)
                val y = yForValue(point.valueMgDl.toDouble())
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = dayColor)
        }

        if (overlay.averageLine.size >= 2) {
            val avgPath = Path()
            overlay.averageLine.sortedBy { it.minuteOfDay }.forEachIndexed { index, point ->
                val x = xForMinute(point.minuteOfDay)
                val y = yForValue(point.averageMgDl)
                if (index == 0) avgPath.moveTo(x, y) else avgPath.lineTo(x, y)
            }
            drawPath(path = avgPath, color = avgColor)
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
