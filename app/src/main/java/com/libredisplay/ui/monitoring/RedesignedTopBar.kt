package com.libredisplay.ui.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.libredisplay.ui.theme.LibreCareColors
import java.time.Duration
import java.time.Instant

/**
 * Redesigned LibreTopBar - simplified top app bar.
 *
 * Removed:
 * - Green connection status dot (status available in data freshness bar)
 * - Manual refresh icon (data auto-syncs)
 * - History icon (available in bottom nav)
 *
 * Kept:
 * - App title
 * - Settings icon (only action-able icon)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibreTopBar(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                stringResource(R.string.app_name),
                fontWeight = FontWeight.SemiBold,
                color = LibreCareColors.TextPrimary,
                fontSize = 18.sp
            )
        },
        actions = {
            IconButton(onClick = onNavigateToSettings, modifier = Modifier.width(48.dp)) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Ustawienia",
                    tint = LibreCareColors.TextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = LibreCareColors.Background
        ),
        modifier = modifier
    )
}

/**
 * Data Freshness and Sensor Status Bar
 *
 * Shows:
 * - Data freshness in relative time format (e.g., "chwilę temu", "15 min temu")
 * - Sensor remaining time (e.g., "Sensor: 13 dni 8 godz. 24 min")
 *
 * If data is stale (> 15 min), shows prominent alert instead.
 */
@Composable
fun DataFreshnessAndSensorStatusBar(
    lastReadingAt: Instant?,
    reading: com.libredisplay.data.model.GlucoseReading?,
    now: Instant = Instant.now(),
    modifier: Modifier = Modifier,
    staleDataThresholdMinutes: Long = 15
) {
    val freshnessDuration = lastReadingAt?.let { Duration.between(it, now) }
    val isStale = freshnessDuration?.let { it > Duration.ofMinutes(staleDataThresholdMinutes) } ?: false
    val sensorStatus = SensorStatusCalculator.calculateSensorStatus(reading, now)

    if (isStale) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Brak aktualnych danych",
                color = LibreCareColors.AccentRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            lastReadingAt?.let {
                Text(
                    text = "Ostatni odczyt: ${PolishDateTimeFormatter.formatAbsolute(it)}",
                    color = LibreCareColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            val freshnessText = freshnessDuration?.let {
                RelativeTimeFormatter.formatDurationAgo(it)
            } ?: "brak danych"

            Text(
                text = "Odczyt: $freshnessText",
                color = LibreCareColors.TextSecondary,
                fontSize = 11.sp
            )
            Text(
                text = sensorStatus.statusMessage,
                color = if (sensorStatus.isError) LibreCareColors.AccentRed
                       else if (sensorStatus.isWarning) LibreCareColors.AccentAmber
                       else LibreCareColors.TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Displays whether connection is available (used for API sync status)
 * This is a simplified indicator that complements the data freshness bar
 */
@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (connectionState) {
        ConnectionState.Connected -> "Połączono" to LibreCareColors.AccentGreen
        ConnectionState.Connecting -> "Łączenie" to LibreCareColors.AccentAmber
        else -> "Offline" to LibreCareColors.AccentRed
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(8.dp)
                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Text(
            text = label,
            color = color,
            fontSize = 10.sp
        )
    }
}

