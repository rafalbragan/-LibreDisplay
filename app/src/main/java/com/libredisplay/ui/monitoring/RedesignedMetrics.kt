package com.libredisplay.ui.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
    QuickMetricId.AVERAGE -> "Średnia"
    QuickMetricId.MINIMUM -> "Minimum"
    QuickMetricId.MAXIMUM -> "Maksimum"
    QuickMetricId.GMI -> "GMI"
    QuickMetricId.CV -> "CV"
    QuickMetricId.VERY_LOW_EPISODES -> "Epizody bardzo niskie"
    QuickMetricId.VERY_HIGH_EPISODES -> "Epizody bardzo wysokie"
    QuickMetricId.HBA1C -> "HbA1c"
    QuickMetricId.SENSOR_ACTIVITY -> "Aktywność"
    QuickMetricId.DATA_COVERAGE -> "Pokrycie danych"
}

internal fun buildQuickMetricTiles(
    belowDuration: Duration?,
    belowPercent: Int?,
    inRangeDuration: Duration?,
    inRangePercent: Int?,
    aboveDuration: Duration?,
    abovePercent: Int?,
    minValueMgDl: Int?,
    maxValueMgDl: Int?,
    gmiValue: Double?,
    averageValueMgDl: Int?,
    veryLowEpisodes: Int?,
    veryHighEpisodes: Int?,
    cvPercent: Double? = null,
    dataCoveragePercent: Int? = null,
    dataMissingDescription: String? = null
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
            id = QuickMetricId.DATA_COVERAGE,
            label = "Pokrycie danych",
            primaryValue = dataCoveragePercent?.let { "$it%" } ?: "—",
            secondaryValue = dataMissingDescription ?: "Brak przerw",
            accent = LibreCareColors.AccentBlue
        ),
        QuickMetricTileUi(
            id = QuickMetricId.AVERAGE,
            label = "Średnia",
            primaryValue = averageValueMgDl?.let { "$it mg/dL" } ?: "—",
            secondaryValue = if (averageValueMgDl == null) "Za mało danych" else "Średnia z widocznego okna",
            accent = LibreCareColors.AccentBlue
        ),
        QuickMetricTileUi(
            id = QuickMetricId.MINIMUM,
            label = "Minimum",
            primaryValue = minValueMgDl?.let { "$it mg/dL" } ?: "—",
            secondaryValue = if (minValueMgDl == null) "Za mało danych" else "Minimum z widocznego okna",
            accent = LibreCareColors.AccentBlue
        ),
        QuickMetricTileUi(
            id = QuickMetricId.MAXIMUM,
            label = "Maksimum",
            primaryValue = maxValueMgDl?.let { "$it mg/dL" } ?: "—",
            secondaryValue = if (maxValueMgDl == null) "Za mało danych" else "Maksimum z widocznego okna",
            accent = LibreCareColors.AccentBlue
        ),
        QuickMetricTileUi(
            id = QuickMetricId.GMI,
            label = "GMI",
            primaryValue = gmiValue?.let { "${"%.1f".format(it).replace('.', ',')}%" } ?: "—",
            secondaryValue = if (gmiValue == null) "Za mało danych" else "Szacunek",
            accent = LibreCareColors.AccentTeal
        ),
        QuickMetricTileUi(
            id = QuickMetricId.CV,
            label = "CV",
            primaryValue = cvPercent?.let { "${"%.0f".format(it)}%" } ?: "—",
            secondaryValue = when {
                cvPercent == null -> "Za mało danych"
                cvPercent <= 36.0 -> "Stabilnie (≤36%)"
                else -> "Duża zmienność"
            },
            accent = LibreCareColors.AccentPurple
        ),
        QuickMetricTileUi(
            id = QuickMetricId.VERY_LOW_EPISODES,
            label = "Epizody bardzo niskie",
            primaryValue = veryLowEpisodes?.toString() ?: "—",
            secondaryValue = if (veryLowEpisodes == null) "Za mało danych" else "Ciągłe wejścia <54 mg/dL",
            accent = LibreCareColors.AccentRed
        ),
        QuickMetricTileUi(
            id = QuickMetricId.VERY_HIGH_EPISODES,
            label = "Epizody bardzo wysokie",
            primaryValue = veryHighEpisodes?.toString() ?: "—",
            secondaryValue = if (veryHighEpisodes == null) "Za mało danych" else "Ciągłe wejścia >250 mg/dL",
            accent = LibreCareColors.AccentAmber
        )
    )
}

internal fun quickMetricsRows(maxWidthDp: Float, orderedTiles: List<QuickMetricTileUi>): List<List<QuickMetricTileUi>> {
    if (orderedTiles.isEmpty()) return emptyList()
    return when {
        maxWidthDp <= 400f -> listOf(orderedTiles.take(3), orderedTiles.drop(3).take(2))
        maxWidthDp < 560f -> listOf(orderedTiles.take(3), orderedTiles.drop(3))
        else -> listOf(orderedTiles)
    }.filter { it.isNotEmpty() }
}

@Composable
internal fun ImprovedQuickMetricsPanel(
    tiles: List<QuickMetricTileUi>,
    orderedIds: List<QuickMetricId>,
    visibility: Map<QuickMetricId, Boolean>,
    onOrderChanged: (List<QuickMetricId>) -> Unit,
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tileById = remember(tiles) { tiles.associateBy { it.id } }
    val homeMetricIds = QuickMetricId.DEFAULT_ORDER
    val visibleOrder = orderedIds.filter { it in homeMetricIds && tileById.containsKey(it) && (visibility[it] ?: true) }
    var orderedVisibleIds by remember(visibleOrder) { mutableStateOf(visibleOrder.ifEmpty { homeMetricIds.filter { tileById.containsKey(it) && (visibility[it] ?: true) } }) }
    val orderedTiles = orderedVisibleIds.mapNotNull { tileById[it] }
    val scrollState = rememberScrollState()
    val canScrollRight = scrollState.value < scrollState.maxValue
    val canScrollLeft = scrollState.value > 0
    var draggingId by remember { mutableStateOf<QuickMetricId?>(null) }
    var dragDx by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Metryki",
                color = LibreCareColors.TextPrimary,
                fontSize = 17.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag(LibreCareTestTags.METRICS_HEADER)
            )
            if (canScrollRight) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "przesuń w bok, aby zobaczyć więcej ›",
                    color = LibreCareColors.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            onEditClick?.let {
                Text(
                    text = "Edytuj >",
                    color = LibreCareColors.AccentGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier
                        .testTag(LibreCareTestTags.METRICS_EDIT)
                        .clickable(onClick = it)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }

        if (orderedTiles.isEmpty()) {
            Text(
                text = "Brak aktywnych metryk. Wybierz je w Edytuj.",
                color = LibreCareColors.TextSecondary,
                fontSize = 13.sp
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canScrollLeft) {
                    Text("‹", color = LibreCareColors.TextSecondary, fontSize = 18.sp)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Long-press lifts the tile (scale + shadow, pops out slightly); dragging past a
                    // threshold reorders. All tiles stay composed for accessibility.
                    orderedVisibleIds.forEach { id ->
                        val tile = tileById[id] ?: return@forEach
                        val lifted = draggingId == id
                        QuickMetricTile(
                            tile = tile,
                            reorderMode = true,
                            isDragging = lifted,
                            modifier = Modifier
                                .zIndex(if (lifted) 1f else 0f)
                                .graphicsLayer {
                                    if (lifted) {
                                        translationX = dragDx
                                        translationY = -10f
                                        scaleX = 1.06f
                                        scaleY = 1.06f
                                        shadowElevation = 12f
                                        alpha = 0.96f
                                    }
                                }
                                .widthIn(min = 96.dp)
                                .width(112.dp)
                                .pointerInput(orderedVisibleIds) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggingId = id; dragDx = 0f },
                                        onDragEnd = { draggingId = null; dragDx = 0f },
                                        onDragCancel = { draggingId = null; dragDx = 0f }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        dragDx += dragAmount.x
                                        val threshold = 60f
                                        val currentIndex = orderedVisibleIds.indexOf(id)
                                        when {
                                            dragDx > threshold && currentIndex < orderedVisibleIds.lastIndex -> {
                                                val updated = orderedVisibleIds.toMutableList().also {
                                                    it.add(currentIndex + 1, it.removeAt(currentIndex))
                                                }
                                                orderedVisibleIds = updated
                                                val hidden = orderedIds.filterNot { it in updated }
                                                onOrderChanged(QuickMetricId.normalizeOrder(updated + hidden))
                                                dragDx -= threshold
                                            }
                                            dragDx < -threshold && currentIndex > 0 -> {
                                                val updated = orderedVisibleIds.toMutableList().also {
                                                    it.add(currentIndex - 1, it.removeAt(currentIndex))
                                                }
                                                orderedVisibleIds = updated
                                                val hidden = orderedIds.filterNot { it in updated }
                                                onOrderChanged(QuickMetricId.normalizeOrder(updated + hidden))
                                                dragDx += threshold
                                            }
                                        }
                                    }
                                }
                        )
                    }
                }
                if (canScrollRight) {
                    Text("›", color = LibreCareColors.AccentGreen, fontSize = 18.sp)
                }
            }
        }
        HorizontalDivider(color = LibreCareColors.Surface, modifier = Modifier.padding(top = 2.dp))
    }
}

/**
 * Single quick metric tile.
 *
 * Uses the same rounded outlined container as the time range chips (1g / 3g / 6g ...) so the
 * dashboard has one consistent visual language.
 */
@Composable
private fun QuickMetricTile(
    tile: QuickMetricTileUi,
    reorderMode: Boolean,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    val isRangeTile = tile.id == QuickMetricId.BELOW || tile.id == QuickMetricId.IN_RANGE || tile.id == QuickMetricId.ABOVE
    Box(
        modifier = modifier
            .testTag(LibreCareTestTags.metricTile(tile.id))
            .requiredHeightIn(min = 84.dp)
            .background(
                if (isDragging) LibreCareColors.SurfaceElevated else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = if (tile.emphasized) tile.accent.copy(alpha = 0.6f) else LibreCareColors.SurfaceMuted,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = tile.label,
                color = LibreCareColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Clip
            )
            if (isRangeTile && !reorderMode) {
                Text(
                    text = tile.secondaryValue,
                    color = tile.accent,
                    fontSize = 23.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = tile.primaryValue,
                    color = LibreCareColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            } else {
                Text(
                    text = tile.primaryValue,
                    color = tile.accent,
                    fontSize = 22.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = tile.secondaryValue,
                    color = LibreCareColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Clip
                )
            }
            if (tile.emphasized && !reorderMode && !isDragging) {
                Spacer(modifier = Modifier.height(1.dp))
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(2.dp)
                        .background(tile.accent)
                )
            }
        }
    }
}

internal fun formatDurationQuickly(duration: Duration?): String {
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

/**
 * Calculates data coverage statistics based on available data points and time range.
 * Detects gaps in data and returns coverage percentage and description.
 */
internal data class DataCoverageStats(
    val coveragePercent: Int,
    val missingDescription: String,
    val gaps: List<DataGap>
)

internal data class DataGap(
    val startTime: java.time.Instant,
    val endTime: java.time.Instant,
    val durationMinutes: Long
)

internal fun calculateDataCoverage(
    points: List<com.libredisplay.data.model.GlucoseHistoryPoint>,
    timeRange: java.time.Duration
): DataCoverageStats {
    if (points.isEmpty()) {
        return DataCoverageStats(
            coveragePercent = 0,
            missingDescription = "Brak danych",
            gaps = emptyList()
        )
    }

    val sorted = points.sortedBy { it.timestamp }
    val firstPoint = sorted.first().timestamp
    val lastPoint = sorted.last().timestamp
    val actualDuration = java.time.Duration.between(firstPoint, lastPoint)

    // Maximum acceptable gap between readings (15 minutes)
    val maxGapMillis = 15 * 60 * 1000L

    val gaps = mutableListOf<DataGap>()
    for (i in 0 until sorted.size - 1) {
        val gap = java.time.Duration.between(sorted[i].timestamp, sorted[i + 1].timestamp)
        if (gap.toMillis() > maxGapMillis) {
            gaps.add(
                DataGap(
                    startTime = sorted[i].timestamp,
                    endTime = sorted[i + 1].timestamp,
                    durationMinutes = gap.toMinutes()
                )
            )
        }
    }

    val totalGapMillis = gaps.fold(0L) { acc, gap ->
        acc + java.time.Duration.between(gap.startTime, gap.endTime).toMillis()
    }
    val coverageMillis = actualDuration.toMillis() - totalGapMillis
    val rangeMillis = timeRange.toMillis()

    val coveragePercent = if (rangeMillis > 0) {
        ((coverageMillis.toDouble() / rangeMillis.toDouble()) * 100).toInt().coerceIn(0, 100)
    } else {
        100
    }

    val missingDescription = when {
        gaps.isEmpty() -> "Brak przerw"
        gaps.size == 1 -> {
            val gap = gaps.first()
            val minutes = gap.durationMinutes
            when {
                minutes < 60 -> "Przerwa ${minutes}m"
                minutes < 1440 -> "Przerwa ${minutes / 60}h"
                else -> "Przerwa ${minutes / 1440}d"
            }
        }
        else -> {
            val totalMissing = gaps.sumOf { it.durationMinutes }
            when {
                totalMissing < 60 -> "${gaps.size} przerwy (${totalMissing}m)"
                totalMissing < 1440 -> "${gaps.size} przerwy (${totalMissing / 60}h)"
                else -> "${gaps.size} przerwy (${totalMissing / 1440}d)"
            }
        }
    }

    return DataCoverageStats(
        coveragePercent = coveragePercent,
        missingDescription = missingDescription,
        gaps = gaps
    )
}
