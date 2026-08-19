package com.libredisplay.ui.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.ui.theme.LibreCareColors
import java.time.Instant

/**
 * Redesigned Current Glucose Card
 *
 * Key changes:
 * - Removed duplicate timestamp (shown in status bar above)
 * - Removed sensor info (shown in status bar above)
 * - Separated glucose level from trend status
 * - Improved color semantics (red/amber for critical, not green-based)
 * - More prominent glucose value hierarchy
 * - Medical alert integrated naturally (not separate card)
 */
@Composable
fun RedesignedCurrentGlucoseCard(
    reading: GlucoseReading,
    targetLow: Int,
    targetHigh: Int,
    now: Instant = Instant.now(),
    modifier: Modifier = Modifier
) {
    val presentation = buildGlucoseStatusPresentation(
        reading = reading,
        now = now,
        config = GlucoseWarningConfig(targetLowMgDl = targetLow, targetHighMgDl = targetHigh)
    )
    val primaryWarning = presentation.primary
    val trend = trendPresentation(reading.trend)
    val glucoseColor = warningToneColor(primaryWarning.tone)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Aktualna glikemia",
                color = LibreCareColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = reading.value.toString(),
                        color = glucoseColor,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "mg/dL",
                        color = glucoseColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = trend.arrow,
                        color = trend.color,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = trend.label,
                        color = trend.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = primaryWarning.title,
                color = glucoseColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "·",
                color = glucoseColor,
                fontSize = 14.sp
            )

            Text(
                text = trend.label,
                color = LibreCareColors.TextSecondary,
                fontSize = 14.sp
            )
        }

        if (primaryWarning.level != GlucoseWarningLevel.IN_RANGE) {
            MedicalAlertInline(
                warning = primaryWarning,
                glucoseColor = glucoseColor
            )
        }
    }
}

/**
 * Inline medical alert within the glucose card
 */
@Composable
private fun MedicalAlertInline(
    warning: GlucoseWarning,
    glucoseColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(glucoseColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = when (warning.tone) {
                    WarningTone.CRITICAL, WarningTone.URGENT -> Icons.Default.ErrorOutline
                    WarningTone.WARNING, WarningTone.CAUTION -> Icons.Default.WarningAmber
                    else -> Icons.Default.WarningAmber
                },
                contentDescription = warning.title,
                tint = glucoseColor,
                modifier = Modifier.padding(top = 2.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "⚠ ${warning.title}",
                    color = glucoseColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = warning.message,
                    color = LibreCareColors.TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

