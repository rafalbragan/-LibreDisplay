package com.libredisplay.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analiza") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Wybrana osoba: ${state.selectedPersonName ?: "-"}", fontWeight = FontWeight.SemiBold)
            if (state.persons.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    state.persons.take(3).forEach { person ->
                        val selected = person.patientId == state.selectedPatientId
                        if (selected) {
                            Button(onClick = { viewModel.onPersonSelected(person.patientId) }, modifier = Modifier.weight(1f)) {
                                Text(person.displayName)
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.onPersonSelected(person.patientId) }, modifier = Modifier.weight(1f)) {
                                Text(person.displayName)
                            }
                        }
                    }
                }
            }

            Text("Zakres czasu", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AnalyticsRange.entries.forEach { range ->
                    val selected = range == state.selectedRange
                    if (selected) {
                        Button(onClick = { viewModel.onRangeSelected(range) }, modifier = Modifier.weight(1f)) {
                            Text("${range.days}d")
                        }
                    } else {
                        OutlinedButton(onClick = { viewModel.onRangeSelected(range) }, modifier = Modifier.weight(1f)) {
                            Text("${range.days}d")
                        }
                    }
                }
            }

            val activity = state.sensorActivity
            MetricCard("Aktywność czujnika", activity?.activityPercent?.let { "${"%.1f".format(it)}%" } ?: "-")
            MetricCard("Czas w zakresie", state.rangeDistribution?.inRangePercent?.let { "$it%" } ?: "-")
            MetricCard("Poniżej zakresu", state.rangeDistribution?.belowRangePercent?.let { "$it%" } ?: "-")
            MetricCard("Powyżej zakresu", state.rangeDistribution?.aboveRangePercent?.let { "$it%" } ?: "-")
            MetricCard("Średnia glukoza", state.averageGlucose?.let { "${"%.0f".format(it)} mg/dL" } ?: "-")
            MetricCard("GMI", state.gmi?.let { "${"%.2f".format(it)}%" } ?: "-")

            val nfzInfoColor = when {
                activity == null -> Color(0xFF94A3B8)
                activity.activityPercent >= 75.0 && (state.rangeDistribution?.inRangePercent ?: 0) > 70 -> Color(0xFF16A34A)
                activity.activityPercent >= 65.0 -> Color(0xFFF59E0B)
                else -> Color(0xFFDC2626)
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Kryteria / refundacja (informacyjnie)", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = when {
                            activity == null -> "Nie można ocenić"
                            activity.activityPercent >= 75.0 && (state.rangeDistribution?.inRangePercent ?: 0) > 70 -> "Warunki widoczne w aplikacji wyglądają na spełnione"
                            activity.activityPercent >= 65.0 -> "Blisko spełnienia albo za mało danych"
                            else -> "Warunki widoczne w aplikacji nie są spełnione"
                        },
                        color = nfzInfoColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Status ma charakter informacyjny. Ostatecznej oceny dokonuje lekarz zgodnie z aktualnymi zasadami refundacji.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            state.infoMessage?.let {
                Text(it, color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = Color(0xFF9CA3AF), fontSize = 12.sp)
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

