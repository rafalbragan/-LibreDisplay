package com.libredisplay.ui.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libredisplay.data.model.QuickMetricId
import com.libredisplay.ui.theme.LibreCareColors
import java.time.Duration

internal data class QuickMetricTileUi(
    val id: QuickMetricId,
    val label: String,
    val primaryValue: String,
    val secondaryValue: String,
    val accent: Color,
    val emphasized: Boolean = false
)

internal fun quickMetricLabel(metricId: QuickMetricId): String = when (metricId) {
    QuickMetricId.BELOW -> "Poniżej"
    QuickMetricId.IN_RANGE -> "W zakresie"
    QuickMetricId.ABOVE -> "Powyżej"
    QuickMetricId.GMI -> "GMI"
    QuickMetricId.HBA1C -> "HbA1c"
}

internal fun buildQuickMetricTiles(
    belowDuration: Duration?,
    belowPercent: Int?,
    inRangeDuration: Duration?,
    inRangePercent: Int?,
    aboveDuration: Duration?,
    abovePercent: Int?,
    gmiValue: Double?,
    hba1cValue: Double?
): List<QuickMetricTileUi> {
    return listOf(
        QuickMetricTileUi(
            id = QuickMetricId.BELOW,
            label = "Poniżej",
            primaryValue = formatDurationQuickly(belowDuration),
            secondaryValue = belowPercent?.let { "$it%" } ?: "—",
            accent = LibreCareColors.AccentRed
        ),
        QuickMetricTileUi(
            id = QuickMetricId.IN_RANGE,
            label = "W zakresie",
            primaryValue = formatDurationQuickly(inRangeDuration),
            secondaryValue = inRangePercent?.let { "$it%" } ?: "—",
            accent = LibreCareColors.AccentGreen,
            emphasized = true
        ),
        QuickMetricTileUi(
            id = QuickMetricId.ABOVE,
            label = "Powyżej",
            primaryValue = formatDurationQuickly(aboveDuration),
            secondaryValue = abovePercent?.let { "$it%" } ?: "—",
            accent = LibreCareColors.AccentAmber
        ),
        QuickMetricTileUi(
            id = QuickMetricId.GMI,
            label = "GMI",
            primaryValue = gmiValue?.let { "${"%.1f".format(it).replace('.', ',')}%" } ?: "—",
            secondaryValue = if (gmiValue == null) "Za mało danych" else "Szacunkowa",
            accent = LibreCareColors.AccentBlue
        ),
        QuickMetricTileUi(
            id = QuickMetricId.HBA1C,
            label = "HbA1c",
            primaryValue = hba1cValue?.let { "${"%.1f".format(it).replace('.', ',')}%" } ?: "—",
            secondaryValue = if (hba1cValue == null) "Brak danych lab" else "Szacunkowa",
            accent = LibreCareColors.TextPrimary
        )
    )
}

@Composable
internal fun ImprovedQuickMetricsPanel(
    tiles: List<QuickMetricTileUi>,
    orderedIds: List<QuickMetricId>,
    onOrderChanged: (List<QuickMetricId>) -> Unit,
    modifier: Modifier = Modifier
) {
    var reorderMode by remember { mutableStateOf(false) }
    var draggingId by remember { mutableStateOf<QuickMetricId?>(null) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val orderState = remember { mutableStateListOf<QuickMetricId>() }

    LaunchedEffect(orderedIds) {
        orderState.clear()
        orderState.addAll(orderedIds)
    }

    val tileById = remember(tiles) { tiles.associateBy { it.id } }
    val visibleOrder = orderState.filter { tileById.containsKey(it) }
    val tileWidth = 112.dp
    val slotStepPx = 120f

    Card(
        colors = CardDefaults.cardColors(containerColor = LibreCareColors.Surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Szybkie metryki",
                    color = LibreCareColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Box(modifier = Modifier.weight(1f))
                TextButton(onClick = { reorderMode = !reorderMode }) {
                    Text(if (reorderMode) "Zakończ" else "Zmień kolejność", fontSize = 12.sp)
                }
            }

            if (reorderMode) {
                Text(
                    text = "Przytrzymaj kafelek i przeciągnij, aby zmienić kolejność.",
                    color = LibreCareColors.TextSecondary,
                    fontSize = 11.sp
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleOrder, key = { it.storageId }) { metricId ->
                    val tile = tileById[metricId] ?: return@items
                    QuickMetricTile(
                        tile = tile,
                        reorderMode = reorderMode,
                        isDragging = draggingId == metricId,
                        modifier = Modifier
                            .width(tileWidth)
                            .pointerInput(reorderMode, visibleOrder, metricId) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        if (!reorderMode) reorderMode = true
                                        draggingId = metricId
                                        dragDistance = 0f
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragDistance = 0f
                                    },
                                    onDragEnd = {
                                        draggingId = null
                                        dragDistance = 0f
                                        onOrderChanged(orderState.toList())
                                    }
                                ) { change, dragAmount ->
                                    if (draggingId != metricId) return@detectDragGesturesAfterLongPress
                                    dragDistance += dragAmount.x
                                    val shift = (dragDistance / slotStepPx).toInt()
                                    if (shift != 0) {
                                        val from = orderState.indexOf(metricId)
                                        if (from != -1) {
                                            val to = (from + shift).coerceIn(0, orderState.lastIndex)
                                            if (to != from) {
                                                orderState.removeAt(from)
                                                orderState.add(to, metricId)
                                                dragDistance -= shift * slotStepPx
                                            }
                                        }
                                    }
                                    change.consume()
                                }
                            }
                    )
                }
            }
        }
    }
}

/**
 * Single quick metric tile
 */
@Composable
private fun QuickMetricTile(
    tile: QuickMetricTileUi,
    reorderMode: Boolean,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                if (tile.emphasized || isDragging) LibreCareColors.SurfaceElevated else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = tile.label,
                color = LibreCareColors.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = tile.primaryValue,
                color = tile.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Text(
                text = tile.secondaryValue,
                color = if (reorderMode) LibreCareColors.TextSecondary else tile.accent,
                fontSize = if (reorderMode) 12.sp else 16.sp,
                fontWeight = if (reorderMode) FontWeight.Normal else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatDurationQuickly(duration: Duration?): String {
    val safeDuration = duration ?: return "—"
    if (safeDuration.isNegative || safeDuration.isZero) return "0m"

    val totalMinutes = safeDuration.toMinutes()
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60

    return when {
        days > 0 -> {
            when {
                hours > 0 -> {
                    val dayText = if (days == 1L) "1d" else "${days}d"
                    val hourText = if (hours == 1L) "1g" else "${hours}g"
                    "$dayText $hourText"
                }
                else -> {
                    val dayText = if (days == 1L) "1 dzień" else "$days dni"
                    dayText
                }
            }
        }
        hours > 0 -> {
            when {
                minutes > 0 -> {
                    val hourText = if (hours == 1L) "1g" else "${hours}g"
                    "$hourText ${minutes}m"
                }
                else -> {
                    val hourText = if (hours == 1L) "1g" else "${hours}g"
                    hourText
                }
            }
        }
        else -> "${minutes}m"
    }
}

/**
 * Compact NFZ Status Card
 *
 * Shows:
 * - Title
 * - Alert if there are criteria requiring attention
 * - Link to details
 */
@Composable
fun CompactNfzStatusCard(
    warningCount: Int,
    totalCriteria: Int = 5,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                warningCount > 2 -> LibreCareColors.AccentRed.copy(alpha = 0.1f)
                warningCount > 0 -> LibreCareColors.AccentAmber.copy(alpha = 0.1f)
                else -> LibreCareColors.Surface
            }
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetails)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Refundacja NFZ",
                    color = LibreCareColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (warningCount > 0) {
                    Text(
                        text = "⚠ $warningCount kryteria wymagają uwagi",
                        color = if (warningCount > 2) LibreCareColors.AccentRed else LibreCareColors.AccentAmber,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "Wszystkie kryteria spełnione",
                        color = LibreCareColors.AccentGreen,
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = ">",
                color = LibreCareColors.TextSecondary,
                fontSize = 18.sp
            )
        }
    }
}



