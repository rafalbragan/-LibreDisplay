package com.libredisplay.ui.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libredisplay.R
import com.libredisplay.ui.theme.LibreCareColors

/**
 * A metric label with a clickable info icon that shows a tooltip/explanation
 */
@Composable
internal fun MetricLabelWithTooltip(
    label: String,
    tooltipTitle: String?,
    tooltipExplanation: String?,
    tooltipFormula: String? = null,
    modifier: Modifier = Modifier
) {
    var showTooltip by remember { mutableStateOf(false) }

    if (tooltipTitle == null || tooltipExplanation == null) {
        // No tooltip available, just show label
        Text(
            text = label,
            color = LibreCareColors.TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            modifier = modifier
        )
    } else {
        Row(
            modifier = modifier
                .clickable { showTooltip = true },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = LibreCareColors.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Wyjaśnienie: $label",
                tint = LibreCareColors.TextSecondary,
                modifier = Modifier.size(12.dp)
            )
        }

        if (showTooltip) {
            MetricTooltipBottomSheet(
                title = tooltipTitle,
                explanation = tooltipExplanation,
                formula = tooltipFormula,
                onClose = { showTooltip = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricTooltipBottomSheet(
    title: String,
    explanation: String,
    formula: String? = null,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = LibreCareColors.Surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = LibreCareColors.TextPrimary
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Wyjaśnienie",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LibreCareColors.TextPrimary
                )
                Text(
                    text = explanation,
                    fontSize = 13.sp,
                    color = LibreCareColors.TextSecondary,
                    lineHeight = 18.sp
                )
            }

            if (formula != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            LibreCareColors.SurfaceElevated,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Wzór",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LibreCareColors.TextSecondary
                    )
                    Text(
                        text = formula,
                        fontSize = 12.sp,
                        color = LibreCareColors.TextPrimary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 8.dp)
            ) {
                Text("Zamknij")
            }
        }
    }
}


