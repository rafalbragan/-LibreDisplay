package com.libredisplay.ui.monitoring

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.R
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Full-screen monitoring dashboard.
 *
 * Layout intent: maximum readability for elderly users – huge numbers,
 * coloured background, single-glance status.
 */
@Composable
fun MonitoringScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: MonitoringViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {

        when (val s = state) {
            is MonitoringUiState.Loading -> LoadingContent()

            is MonitoringUiState.Success -> SuccessContent(
                state = s,
                onRefresh = viewModel::refresh,
                onSettings = onNavigateToSettings
            )

            is MonitoringUiState.Error -> ErrorContent(
                message = s.message,
                onRefresh = viewModel::refresh,
                onSettings = onNavigateToSettings
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF80CBC4), strokeWidth = 6.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.loading_connecting),
                color = Color.White,
                fontSize = 22.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Success
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SuccessContent(
    state: MonitoringUiState.Success,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    val reading = state.reading
    val bgColor = when {
        state.isStale     -> ColorStaleBackground
        reading.isLow     -> ColorLowBackground
        reading.isHigh    -> ColorHighBackground
        else              -> ColorInRangeBackground
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // ── Main reading area ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Stale data warning
            AnimatedVisibility(
                visible = state.isStale,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                StaleWarning()
            }

            // Trend arrow
            Text(
                text = reading.trend.arrow,
                color = ColorOnDark,
                fontSize = 80.sp,
                fontWeight = FontWeight.Light
            )

            // Glucose value – enormous for elderly readability
            Text(
                text = "${reading.value}",
                color = ColorOnDark,
                fontSize = 160.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 160.sp
            )

            // Unit
            Text(
                text = stringResource(R.string.unit_mgdl),
                color = ColorSubtitle,
                fontSize = 32.sp
            )

            Spacer(Modifier.height(16.dp))

            // Trend description
            Text(
                text = reading.trend.description,
                color = ColorOnDark,
                fontSize = 40.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(24.dp))

            // Timestamp & data age
            Text(
                text = stringResource(R.string.timestamp_label, state.reading.timestamp.toDisplayTime()),
                color = ColorSubtitle,
                fontSize = 20.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(
                    R.string.updated_minutes_ago,
                    state.dataAgeMin,
                    if (state.dataAgeMin == 1L) stringResource(R.string.minute_singular) else stringResource(R.string.minute_plural)
                ),
                color = ColorSubtitle,
                fontSize = 22.sp
            )

            Spacer(Modifier.height(8.dp))

            // Status
            StatusChip(reading = reading, isStale = state.isStale)

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.medical_disclaimer),
                color = Color(0xFFE0E0E0),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // Error banner (non-fatal, last reading still shown)
            state.error?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.error_warning, msg),
                    color = Color(0xFFFFCC02),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // ── Action buttons (top-right corner) ─────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.refresh),
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Error (no cached reading)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(
    message: String,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorStaleBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFFCC02),
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.cannot_retrieve_data),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = Color.LightGray,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.retry), fontSize = 20.sp)
                }
                OutlinedButton(
                    onClick = onSettings,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings), fontSize = 20.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StaleWarning() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFCC02), modifier = Modifier.size(36.dp))
        Text(
            text = stringResource(R.string.stale_data_refreshing),
            color = Color(0xFFFFCC02),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun java.time.Instant.toDisplayTime(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return formatter.format(atZone(ZoneId.systemDefault()))
}

@Composable
private fun StatusChip(reading: GlucoseReading, isStale: Boolean) {
    val (label, chipColor) = when {
        isStale      -> stringResource(R.string.status_stale)    to Color(0xFF757575)
        reading.isLow  -> stringResource(R.string.status_low)   to Color(0xFFEF5350)
        reading.isHigh -> stringResource(R.string.status_high)  to Color(0xFFFF7043)
        else           -> stringResource(R.string.status_normal)    to Color(0xFF43A047)
    }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = chipColor,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.status_label, label),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}

