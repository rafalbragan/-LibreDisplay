package com.libredisplay.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesScreen(
    onNavigateBack: () -> Unit
) {
    val featureRecords = FeatureCatalog.featureRecords()
    val versions = FeatureCatalog.versionHistory()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Funkcje i historia wersji") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Status funkcji",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            featureRecords.forEach { record ->
                FeatureCard(record)
            }

            Text(
                text = "Historia wersji",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
            versions.forEach { version ->
                VersionHistoryCard(version)
            }
        }
    }
}

@Composable
private fun FeatureCard(record: FeatureRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(record.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = record.status.labelPl,
                    fontSize = 11.sp,
                    color = Color.White,
                    modifier = Modifier
                        .background(statusColor(record.status), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Text(record.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Wersja/build: ${record.targetVersion}", fontSize = 12.sp)
            Text("Powiazania: ${record.references.joinToString(", ")}", fontSize = 12.sp)
            Text("Aktualizacja: ${record.updatedAt}", fontSize = 12.sp)
        }
    }
}

@Composable
private fun VersionHistoryCard(record: VersionHistoryRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${record.versionName} (${record.versionCode}) • ${record.releaseType}",
                fontWeight = FontWeight.SemiBold
            )
            Text("Data wydania: ${record.releaseDate}", fontSize = 12.sp)
            Text("Nowe funkcje: ${record.newFeatures.joinToString("; ")}", fontSize = 12.sp)
            Text("Naprawione bledy: ${record.fixedIssues.joinToString("; ")}", fontSize = 12.sp)
            Text("Usprawnienia: ${record.improvements.joinToString("; ")}", fontSize = 12.sp)
            Text("Znane ograniczenia: ${record.knownLimitations.joinToString("; ")}", fontSize = 12.sp)
        }
    }
}

private fun statusColor(status: FeatureStatus): Color {
    return when (status) {
        FeatureStatus.PLANNED -> Color(0xFF64748B)
        FeatureStatus.IN_DEVELOPMENT -> Color(0xFF0EA5A4)
        FeatureStatus.FOR_TESTING -> Color(0xFF2563EB)
        FeatureStatus.DEPLOYED -> Color(0xFF16A34A)
        FeatureStatus.ON_HOLD -> Color(0xFFB45309)
    }
}

