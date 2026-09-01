package com.libredisplay.ui.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.ui.monitoring.GlucoseWarningLevel
import com.libredisplay.ui.theme.LibreCareColors
import java.time.Duration
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
    trendWindowMinutes: Int,
    now: Instant = Instant.now(),
    modifier: Modifier = Modifier
) {
    val projectionThresholds = remember(targetLow, targetHigh) {
        TrendProjectionThresholds(
            lowThresholdMgDl = targetLow,
            highThresholdMgDl = targetHigh
        )
    }
    val trendProjection = remember(reading, trendWindowMinutes, projectionThresholds, now) {
        buildTrendProjection(
            reading = reading,
            trendWindowMinutes = trendWindowMinutes,
            thresholds = projectionThresholds,
            now = now
        )
    }
    val trendRateLabel = trendProjection?.let { formatTrendRatePerMinute(it.rateMgDlPerMinute) }
    val projectionMessage = trendProjection?.let(::formatTrendProjectionMessage)

    val presentation = buildGlucoseStatusPresentation(
        reading = reading,
        now = now,
        config = GlucoseWarningConfig(targetLowMgDl = targetLow, targetHighMgDl = targetHigh)
    )
    val primaryWarning = presentation.primary
    val trend = trendPresentation(
        trend = reading.trend,
        glucoseValue = reading.value,
        targetLow = targetLow,
        targetHigh = targetHigh
    )
    val glucoseColor = warningToneColor(primaryWarning.tone)
    val trendColor = if (primaryWarning.level == GlucoseWarningLevel.IN_RANGE) trend.color else glucoseColor
    val statusText = primaryWarning.title
    val trendText = trend.label.takeIf {
        val normalizedTrend = trend.label.trim().lowercase()
        val normalizedStatus = statusText.trim().lowercase()
        normalizedTrend.isNotBlank() && normalizedTrend != normalizedStatus
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("Aktualna glikemia: ${reading.value} miligramów na decylitr. ${primaryWarning.title}. Trend: ${trend.label}.")
                    trendRateLabel?.let { append(" Tempo zmian: $it.") }
                    projectionMessage?.let { append(" $it") }
                }
            }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Both captions share one row so "Trend" is always on the same baseline as
        // "Aktualna glikemia", independent of the value/arrow heights below.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aktualna glikemia",
                color = LibreCareColors.TextSecondary,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Trend",
                color = LibreCareColors.TextSecondary,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(min = 64.dp)
                ) {
                    Text(
                        text = reading.value.toString(),
                        color = glucoseColor,
                        fontSize = 52.sp,
                        lineHeight = 54.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "mg/dL",
                        color = glucoseColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            TrendSummary(
                arrow = trend.arrow,
                label = trendText ?: trend.label,
                ratePerMinuteLabel = trendRateLabel,
                status = statusText,
                color = trendColor,
                modifier = Modifier.weight(1f)
            )
        }

        projectionMessage?.let { message ->
            Text(
                text = message,
                color = LibreCareColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.fillMaxWidth()
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

@Composable
private fun TrendSummary(
    arrow: String,
    label: String,
    ratePerMinuteLabel: String?,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 64.dp)
            .semantics {
                contentDescription = buildString {
                    append("Trend: $label, status: $status")
                    ratePerMinuteLabel?.let { append(", tempo: $it") }
                }
            }
    ) {
        Text(
            text = arrow,
            color = color,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                color = color,
                fontSize = 15.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ratePerMinuteLabel?.let { rateLabel ->
                Text(
                    text = rateLabel,
                    color = LibreCareColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = status,
                color = color,
                fontSize = 15.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
            .padding(horizontal = 9.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "⚠ Zalecenie",
                    color = glucoseColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = warning.message,
                    color = LibreCareColors.TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
