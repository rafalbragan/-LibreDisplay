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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun StatisticsScreen(onNavigateBack: () -> Unit) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Database Statistics Section
            StatisticsSection(
                title = stringResource(R.string.statistics_database),
                items = listOf(
                    "Liczba odczytów" to "123 456 odczytów",
                    "Liczba osób" to "3 osoby",
                    "Rozmiar bazy" to "~45 MB",
                    "Najstarszy odczyt" to "01.07.2026",
                    "Najnowszy odczyt" to "18.08.2026",
                    "Przyrost średni" to "~2.1 MB / tydzień"
                )
            )

            // Network Statistics Section
            StatisticsSection(
                title = stringResource(R.string.statistics_network),
                items = listOf(
                    "Pobrane dane" to "~1.2 GB",
                    "Wysłane dane" to "~15 MB",
                    "Średnia na dzień" to "~2.1 MB",
                    "Ostatnia sync" to "przed 3 minutami",
                    "Szacowanie" to "Za mało danych do dokładnej estymacji"
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
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

