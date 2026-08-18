package com.libredisplay.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.R

private val DashboardBackground = Color(0xFF101318)
private val DashboardSurface = Color(0xFF182033)
private val DashboardElevatedSurface = Color(0xFF202A3D)
private val DashboardPrimaryText = Color(0xFFF3F6FA)
private val DashboardSecondaryText = Color(0xFFAAB3C2)

/**
 * Statistics and Information screen showing database stats and network transfer data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = DashboardBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DashboardSurface,
                    titleContentColor = DashboardPrimaryText,
                    navigationIconContentColor = DashboardPrimaryText
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val db = state.databaseStats
        val net = state.networkStats

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.error?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2024)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color = DashboardPrimaryText,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (state.isDemoMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2E18)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.demo_transfer_exclusion),
                        color = DashboardPrimaryText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Database Statistics Section
            StatisticsSection(
                title = stringResource(R.string.statistics_database),
                items = listOf(
                    stringResource(R.string.database_size) to (db?.let { viewModel.getApplication<com.libredisplay.LibreDisplayApp>().diagnosticsStatsRepository.formatBytes(it.totalBytes) } ?: "-"),
                    stringResource(R.string.database_reading_count) to (db?.readingsCount?.toString() ?: "-"),
                    stringResource(R.string.database_person_count) to (db?.peopleCount?.toString() ?: "-"),
                    stringResource(R.string.database_oldest_reading) to (db?.oldestReadingTimestamp?.let { formatInstant(it) } ?: "Brak danych"),
                    stringResource(R.string.database_newest_reading) to (db?.newestReadingTimestamp?.let { formatInstant(it) } ?: "Brak danych"),
                    stringResource(R.string.database_available_range) to (db?.availableRangeText ?: "Brak danych"),
                    stringResource(R.string.database_growth_week) to (db?.estimatedGrowthPerWeekBytes?.let { viewModel.getApplication<com.libredisplay.LibreDisplayApp>().diagnosticsStatsRepository.formatBytes(it) } ?: stringResource(R.string.insufficient_estimation_data)),
                    stringResource(R.string.database_growth_month) to (db?.estimatedGrowthPerMonthBytes?.let { viewModel.getApplication<com.libredisplay.LibreDisplayApp>().diagnosticsStatsRepository.formatBytes(it) } ?: stringResource(R.string.insufficient_estimation_data))
                )
            )

            // Network Statistics Section
            StatisticsSection(
                title = stringResource(R.string.statistics_network),
                items = listOf(
                    stringResource(R.string.network_downloaded_total) to (net?.let { viewModel.getApplication<com.libredisplay.LibreDisplayApp>().diagnosticsStatsRepository.formatBytes(it.totalDownloadedBytes) } ?: "-"),
                    stringResource(R.string.network_uploaded_total) to (net?.let { viewModel.getApplication<com.libredisplay.LibreDisplayApp>().diagnosticsStatsRepository.formatBytes(it.totalUploadedBytes) } ?: "-"),
                    stringResource(R.string.network_downloaded_day) to (net?.averageDownloadedPerDayBytes?.let { viewModel.getApplication<com.libredisplay.LibreDisplayApp>().diagnosticsStatsRepository.formatBytes(it) } ?: stringResource(R.string.insufficient_estimation_data)),
                    stringResource(R.string.network_uploaded_day) to (net?.averageUploadedPerDayBytes?.let { viewModel.getApplication<com.libredisplay.LibreDisplayApp>().diagnosticsStatsRepository.formatBytes(it) } ?: stringResource(R.string.insufficient_estimation_data)),
                    stringResource(R.string.network_downloaded_week) to (net?.averageDownloadedPerWeekBytes?.let { viewModel.getApplication<com.libredisplay.LibreDisplayApp>().diagnosticsStatsRepository.formatBytes(it) } ?: stringResource(R.string.insufficient_estimation_data)),
                    stringResource(R.string.network_uploaded_week) to (net?.averageUploadedPerWeekBytes?.let { viewModel.getApplication<com.libredisplay.LibreDisplayApp>().diagnosticsStatsRepository.formatBytes(it) } ?: stringResource(R.string.insufficient_estimation_data)),
                    stringResource(R.string.network_downloaded_month) to (net?.averageDownloadedPerMonthBytes?.let { viewModel.getApplication<com.libredisplay.LibreDisplayApp>().diagnosticsStatsRepository.formatBytes(it) } ?: stringResource(R.string.insufficient_estimation_data)),
                    stringResource(R.string.network_uploaded_month) to (net?.averageUploadedPerMonthBytes?.let { viewModel.getApplication<com.libredisplay.LibreDisplayApp>().diagnosticsStatsRepository.formatBytes(it) } ?: stringResource(R.string.insufficient_estimation_data)),
                    stringResource(R.string.network_sync_count) to ((net?.successfulSyncCount ?: 0L).toString()),
                    stringResource(R.string.network_last_sync) to (net?.lastSyncAt?.let { formatInstant(it) } ?: "Brak danych")
                )
            )

            StatisticsSection(
                title = stringResource(R.string.polling_frequency_title),
                items = listOf(
                    stringResource(R.string.polling_current_frequency) to state.pollingLabel,
                    stringResource(R.string.polling_current_usage) to state.pollingUsageCurrentLabel,
                    stringResource(R.string.polling_after_change) to state.pollingUsageEstimatedLabel,
                    stringResource(R.string.polling_battery_warning) to stringResource(R.string.polling_battery_warning)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatInstant(instant: java.time.Instant): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withZone(java.time.ZoneId.systemDefault())
    return formatter.format(instant)
}

@Composable
private fun StatisticsSection(
    title: String,
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            title,
            color = DashboardPrimaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = DashboardSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                items.forEachIndexed { index, (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            color = DashboardSecondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            value,
                            color = DashboardPrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (index < items.size - 1) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(DashboardElevatedSurface)
                        )
                    }
                }
            }
        }
    }
}

