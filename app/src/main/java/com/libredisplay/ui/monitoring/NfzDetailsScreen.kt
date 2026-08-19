package com.libredisplay.ui.monitoring

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
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
import com.libredisplay.ui.theme.LibreCareColors

internal data class NfzDetailsContext(
    val assessment: NfzAssessment,
    val summary: NfzStatusSummaryUi,
    val attentionCount: Int,
    val totalCriteriaCount: Int,
    val selectedHomeRangeDays: Double,
    val selectedHomeRangeLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NfzDetailsScreen(
    context: NfzDetailsContext,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refundacja NFZ") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Status: ${context.attentionCount} z ${context.totalCriteriaCount} kryteriów wymaga uwagi",
                color = if (context.attentionCount > 0) LibreCareColors.AccentAmber else LibreCareColors.AccentGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Aktualny zakres na ekranie głównym: ${context.selectedHomeRangeLabel}",
                color = LibreCareColors.TextSecondary,
                fontSize = 12.sp
            )

            Text("Kryteria", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

            val hasExtendedWindowCriteria = context.assessment.criteria.any {
                val minDays = it.minimumEvaluationDays ?: return@any false
                minDays > context.selectedHomeRangeDays
            }

            context.assessment.criteria.forEach { criterion ->
                NfzCriterionRow(
                    criterion = criterion,
                    selectedRangeDays = context.selectedHomeRangeDays
                )
            }

            if (hasExtendedWindowCriteria) {
                Text(
                    text = "* Część wartości obliczono z dłuższego okresu wymaganego dla konkretnego kryterium niż aktualny zakres na Home.",
                    color = LibreCareColors.TextSecondary,
                    fontSize = 11.sp
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = LibreCareColors.Surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Wyjaśnienia", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(context.summary.headline, color = LibreCareColors.TextPrimary, fontSize = 13.sp)
                    Text(context.summary.details, color = LibreCareColors.TextSecondary, fontSize = 12.sp)

                    if (context.summary.keyReasons.isNotEmpty()) {
                        Text("Główne powody", color = Color.White, fontWeight = FontWeight.SemiBold)
                        context.summary.keyReasons.forEach { reason ->
                            Text("• $reason", color = LibreCareColors.TextSecondary, fontSize = 12.sp)
                        }
                    }

                    if (context.summary.keyRecommendations.isNotEmpty()) {
                        Text("Zalecenia", color = Color.White, fontWeight = FontWeight.SemiBold)
                        context.summary.keyRecommendations.forEach { recommendation ->
                            Text("• $recommendation", color = LibreCareColors.TextSecondary, fontSize = 12.sp)
                        }
                    }

                    Text(
                        "Na podstawie dostępnych danych jest to ocena orientacyjna. Aplikacja nie zastępuje decyzji lekarza ani NFZ.",
                        color = LibreCareColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NfzCriterionRow(
    criterion: NfzCriterionEvaluation,
    selectedRangeDays: Double
) {
    val (symbol, color) = when (criterion.status) {
        NfzCriterionStatus.MET -> "✓" to LibreCareColors.AccentGreen
        NfzCriterionStatus.NOT_MET -> "!" to LibreCareColors.AccentAmber
        NfzCriterionStatus.UNKNOWN -> "—" to LibreCareColors.TextSecondary
        NfzCriterionStatus.NOT_APPLICABLE -> "—" to LibreCareColors.TextSecondary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = LibreCareColors.Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        val needsExtendedWindowMarker = (criterion.minimumEvaluationDays ?: 0) > selectedRangeDays
        val valuePrefix = if (needsExtendedWindowMarker) "* " else ""
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(symbol, color = color, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(criterion.condition, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("$valuePrefix${criterion.currentValue}", color = LibreCareColors.TextPrimary, fontSize = 13.sp)
            Text("Wymagane: ${criterion.requiredValue}", color = LibreCareColors.TextSecondary, fontSize = 12.sp)
            criterion.evaluationWindowLabel?.let {
                Text("Okres oceny kryterium: $it", color = LibreCareColors.TextSecondary, fontSize = 12.sp)
            }
            Text("Powód: ${criterion.reason}", color = LibreCareColors.TextSecondary, fontSize = 12.sp)
            criterion.recommendation?.let {
                Text("Zalecenie: $it", color = LibreCareColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}


