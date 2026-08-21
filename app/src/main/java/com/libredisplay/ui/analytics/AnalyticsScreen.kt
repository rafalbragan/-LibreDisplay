package com.libredisplay.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.ui.monitoring.CompactPersonSwitcherBar
import com.libredisplay.ui.monitoring.DashboardNavItem
import com.libredisplay.ui.monitoring.TopLevelNavigationBar
import com.libredisplay.ui.theme.LibreCareColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
    onOpenHome: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: AnalyticsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historia glikemii") },
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsRange.entries.forEach { range ->
                    val selected = range == state.selectedRange
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { viewModel.onRangeSelected(range) }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = if (range == AnalyticsRange.DAYS_365) "1r" else "${range.days}d",
                            color = if (selected) LibreCareColors.TextPrimary else LibreCareColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (selected) {
                            Spacer(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(2.dp)
                                    .background(LibreCareColors.AccentTeal)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }

            HorizontalDivider(color = LibreCareColors.Surface)

            val activity = state.sensorActivity?.activityPercent?.let { "${"%.0f".format(it)}%" } ?: "—"
            val tir = state.rangeDistribution?.inRangePercent?.let { "$it%" } ?: "—"
            val below = state.rangeDistribution?.belowRangePercent?.let { "$it%" } ?: "—"
            val above = state.rangeDistribution?.aboveRangePercent?.let { "$it%" } ?: "—"
            val avg = state.averageGlucose?.let { "${"%.0f".format(it)} mg/dL" } ?: "—"
            val gmi = state.gmi?.let { "${"%.1f".format(it)}%" } ?: "—"

            MetricsGrid(
                rows = listOf(
                    "Pokrycie danych" to activity,
                    "Czas w zakresie" to tir,
                    "Poniżej" to below,
                    "Powyżej" to above,
                    "Średnia" to avg,
                    "GMI" to gmi
                )
            )

            state.infoMessage?.let {
                Text(it, color = LibreCareColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MetricsGrid(rows: List<Pair<String, String>>) {
    val chunks = rows.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        chunks.forEach { chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                chunk.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(LibreCareColors.Surface.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(label, color = LibreCareColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
                        Text(
                            value,
                            color = LibreCareColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (chunk.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
