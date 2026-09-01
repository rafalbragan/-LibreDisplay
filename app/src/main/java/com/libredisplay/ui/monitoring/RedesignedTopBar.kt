package com.libredisplay.ui.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libredisplay.BuildConfig
import com.libredisplay.R
import com.libredisplay.data.model.GlucoseReading
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
@Composable
fun LibreTopBar(
    lastReadingAt: Instant?,
    reading: GlucoseReading?,
    appVersionLabel: String,
    now: Instant = Instant.now(),
    onRunUiAudit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val topUpdateText = lastReadingAt?.let {
        val zoneId = DateTimeFormatterProvider.deviceZoneId()
        val readingDate = it.atZone(zoneId).toLocalDate()
        val nowDate = now.atZone(zoneId).toLocalDate()
        val timeLabel = PolishDateTimeFormatter.formatTime(it, zoneId)
        when (readingDate) {
            nowDate -> "dziś $timeLabel"
            nowDate.minusDays(1) -> "wczoraj $timeLabel"
            else -> "${DateTimeFormatterProvider.compactDateFormatter().withZone(zoneId).format(it)} $timeLabel"
        }
    } ?: "brak danych"

    val dataAgeText = reading?.let { r ->
        val dataAge = Duration.between(r.timestamp, now)
        formatReadingAge(dataAge)
    } ?: "brak danych"

    Surface(
        color = LibreCareColors.Background,
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            val compactLayout = this.maxWidth < 392.dp
            if (compactLayout) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                modifier = Modifier.testTag(LibreCareTestTags.TOP_BAR_TITLE),
                                fontWeight = FontWeight.SemiBold,
                                color = LibreCareColors.TextPrimary,
                                fontSize = 25.sp,
                                lineHeight = 27.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "v$appVersionLabel",
                                modifier = Modifier
                                    .testTag(LibreCareTestTags.TOP_BAR_VERSION)
                                    .padding(bottom = 2.dp),
                                color = LibreCareColors.TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                maxLines = 1
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Ostatnia aktualizacja: $topUpdateText",
                                modifier = Modifier.testTag(LibreCareTestTags.TOP_BAR_LAST_UPDATE),
                                color = LibreCareColors.TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 12.sp,
                                maxLines = 1,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "Wiek danych: $dataAgeText",
                                modifier = Modifier.testTag("top_bar_data_age"),
                                color = LibreCareColors.TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 12.sp,
                                maxLines = 1,
                                textAlign = TextAlign.End
                            )
                            if (onRunUiAudit != null && BuildConfig.DEBUG) {
                                IconButton(onClick = onRunUiAudit, modifier = Modifier.width(28.dp).height(28.dp).testTag(LibreCareTestTags.TOP_BAR_UI_AUDIT)) {
                                    Icon(
                                        Icons.Default.PhotoCamera,
                                        contentDescription = "Raport UI",
                                        tint = LibreCareColors.TextPrimary,
                                        modifier = Modifier.width(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            modifier = Modifier.testTag(LibreCareTestTags.TOP_BAR_TITLE),
                            fontWeight = FontWeight.SemiBold,
                            color = LibreCareColors.TextPrimary,
                            fontSize = 27.sp,
                            lineHeight = 29.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "v$appVersionLabel",
                            modifier = Modifier
                                .testTag(LibreCareTestTags.TOP_BAR_VERSION)
                                .padding(bottom = 3.dp),
                            color = LibreCareColors.TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            maxLines = 1
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.heightIn(min = 60.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(20.dp)
                        ) {
                            Text(
                                text = "Ostatnia aktualizacja: $topUpdateText",
                                modifier = Modifier.testTag(LibreCareTestTags.TOP_BAR_LAST_UPDATE),
                                color = LibreCareColors.TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 13.sp,
                                maxLines = 1,
                                textAlign = TextAlign.End
                            )
                        }
                        Text(
                            text = "Wiek danych: $dataAgeText",
                            modifier = Modifier.testTag("top_bar_data_age"),
                            color = LibreCareColors.TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            maxLines = 1,
                            textAlign = TextAlign.End
                        )
                        if (onRunUiAudit != null && BuildConfig.DEBUG) {
                            IconButton(onClick = onRunUiAudit, modifier = Modifier.width(36.dp).height(36.dp).testTag(LibreCareTestTags.TOP_BAR_UI_AUDIT)) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = "Raport UI",
                                    tint = LibreCareColors.TextPrimary,
                                    modifier = Modifier.width(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTopBarMetaLine(
    primary: String,
    secondary: String,
    secondaryColor: Color,
    trailing: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        CompactTopBarText(primary)
        CompactTopBarText(secondary, color = secondaryColor)
        CompactTopBarText(trailing)
    }
}

@Composable
private fun CompactTopBarText(
    text: String,
    color: Color = LibreCareColors.TextSecondary
) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        lineHeight = 12.sp,
        maxLines = 1
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

    val freshnessText = freshnessDuration?.let {
        RelativeTimeFormatter.formatDurationAgo(it)
    } ?: "brak danych"

    val sensorColor = if (sensorStatus.isError) LibreCareColors.AccentRed
    else if (sensorStatus.isWarning) LibreCareColors.AccentAmber
    else LibreCareColors.TextSecondary

    val readingText = if (isStale) {
        "Odczyt: ${lastReadingAt?.let { PolishDateTimeFormatter.formatTime(it) } ?: "brak"}"
    } else {
        "Odczyt: $freshnessText"
    }
    val sensorText = sensorStatus.statusMessage

    Surface(color = Color.Transparent) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
        ) {
            val stacked = this.maxWidth < 360.dp
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = readingText,
                        color = if (isStale) LibreCareColors.AccentRed else LibreCareColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (isStale) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 2
                    )
                    Text(
                        text = sensorText,
                        color = sensorColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 2
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = readingText,
                        color = if (isStale) LibreCareColors.AccentRed else LibreCareColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (isStale) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                    Text(
                        text = "·",
                        color = LibreCareColors.TextMuted,
                        fontSize = 14.sp
                    )
                    Text(
                        text = sensorText,
                        color = sensorColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
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
