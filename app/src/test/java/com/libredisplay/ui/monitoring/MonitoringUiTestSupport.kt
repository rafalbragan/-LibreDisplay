package com.libredisplay.ui.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.libredisplay.analytics.GlucoseMetricsCalculator
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.QuickMetricId
import com.libredisplay.testing.scenario.GlucoseScenario
import com.libredisplay.testing.scenario.GlucoseScenarioEngine
import com.libredisplay.ui.settings.SettingsMainScreen
import com.libredisplay.ui.theme.LibreDisplayTheme
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals

internal data class ResponsiveMatrixConfig(
    val widthDp: Int,
    val fontScale: Float
)

internal val DefaultResponsiveMatrix: List<ResponsiveMatrixConfig> = listOf(
    ResponsiveMatrixConfig(widthDp = 360, fontScale = 1.0f),
    ResponsiveMatrixConfig(widthDp = 360, fontScale = 1.2f),
    ResponsiveMatrixConfig(widthDp = 384, fontScale = 1.3f),
    ResponsiveMatrixConfig(widthDp = 411, fontScale = 1.5f),
    ResponsiveMatrixConfig(widthDp = 480, fontScale = 1.0f)
)

@Composable
internal fun MonitoringHomeTestHost(
    scenario: GlucoseScenario,
    widthDp: Int,
    fontScale: Float,
    modifier: Modifier = Modifier
) {
    val dataset = remember(scenario) { GlucoseScenarioEngine.dataset(scenario) }
    val reading = dataset.asReading()
    val metricTiles = remember(scenario, reading) {
        metricTilesForScenario(scenario = scenario, reading = reading, points = dataset.points)
    }
    val rangeOptions = homeChartRangeOptions(dataset.availableSpan)
    val selectedRange = rangeOptions.lastOrNull { it.enabled }?.range ?: HomeChartRange.LAST_1_HOUR

    ResponsiveContainer(widthDp = widthDp, fontScale = fontScale, modifier = modifier) {
        Surface {
            Column(
                modifier = Modifier
                    .background(color = androidx.compose.ui.graphics.Color(0xFF07101A))
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                LibreTopBar(
                    lastReadingAt = reading?.timestamp ?: dataset.newestTimestamp,
                    reading = reading,
                    appVersionLabel = "test",
                    onRunUiAudit = {}
                )
                when {
                    reading == null -> {
                        Text("Brak danych", modifier = Modifier.padding(top = 12.dp))
                    }
                    dataset.newestTimestamp?.isBefore(dataset.now.minusSeconds(60 * 60)) == true -> {
                        Text("Dane nieaktualne", modifier = Modifier.padding(top = 12.dp))
                        CurrentGlucoseHeroCard(reading = reading, targetLow = 80, targetHigh = 180)
                    }
                    else -> {
                        CurrentGlucoseHeroCard(reading = reading, targetLow = 80, targetHigh = 180)
                    }
                }
                if (metricTiles.isNotEmpty()) {
                    ImprovedQuickMetricsPanel(
                        tiles = metricTiles,
                        orderedIds = QuickMetricId.DEFAULT_ORDER,
                        visibility = QuickMetricId.entries.associateWith { true },
                        onOrderChanged = {},
                        onEditClick = {}
                    )
                }
                HomeChartRangeSelector(
                    options = rangeOptions,
                    selectedRange = selectedRange,
                    onRangeSelected = {}
                )
                TopLevelNavigationBar(
                    selected = DashboardNavItem.GLOWNA,
                    onOpenHome = {},
                    onOpenHistory = {},
                    onOpenFutures = {},
                    onOpenSettings = {}
                )
            }
        }
    }
}

@Composable
internal fun HistoryScreenshotHost(widthDp: Int, fontScale: Float) {
    val dataset = remember { GlucoseScenarioEngine.dataset(GlucoseScenario.FULL_24H_DATA) }
    val reading = dataset.asReading()
    val points = dataset.points
    val distribution = GlucoseMetricsCalculator.calculateRangeDistribution(points, 80, 180, 54, 250)
    val activity = GlucoseMetricsCalculator.calculateSensorActivity(
        readings = points,
        periodStart = points.first().timestamp,
        periodEnd = points.last().timestamp
    )

    ResponsiveContainer(widthDp = widthDp, fontScale = fontScale) {
        LibreDisplayTheme {
            Surface {
                Column(Modifier.padding(16.dp)) {
                    Text("Historia glikemii")
                    Text("Zakres: 24h")
                    TestMetricsGrid(
                        rows = listOf(
                            "Pokrycie danych" to "${activity.activityPercent.toInt()}%",
                            "Czas w zakresie" to "${distribution.inRangePercent}%",
                            "Poniżej" to "${distribution.belowRangePercent}%",
                            "Powyżej" to "${distribution.aboveRangePercent}%",
                            "Średnia" to "${reading?.history?.map { it.value }?.average()?.toInt() ?: 0} mg/dL",
                            "GMI" to (reading?.stats?.let { "${GlucoseMetricsCalculator.calculateGmi(reading.history.map { point -> point.value }.average()).formatOneDecimal()}%" } ?: "—")
                        )
                    )
                    TopLevelNavigationBar(
                        selected = DashboardNavItem.HISTORIA,
                        onOpenHome = {},
                        onOpenHistory = {},
                        onOpenFutures = {},
                        onOpenSettings = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun TestMetricsGrid(rows: List<Pair<String, String>>) {
    val chunks = rows.chunked(2)
    Column(modifier = Modifier.padding(top = 12.dp)) {
        chunks.forEach { chunk ->
            Row {
                chunk.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f).padding(4.dp)) {
                        Text(label)
                        Text(value)
                    }
                }
                if (chunk.size == 1) {
                    Spacer(modifier = Modifier.weight(1f).height(1.dp))
                }
            }
        }
    }
}

@Composable
internal fun SettingsScreenshotHost(widthDp: Int, fontScale: Float) {
    ResponsiveContainer(widthDp = widthDp, fontScale = fontScale) {
        LibreDisplayTheme {
            SettingsMainScreen(
                showBackButton = true,
                onNavigateBack = {},
                onNavigateToMonitoring = {},
                onNavigateToMetricSettings = {},
                onNavigateToHbA1cSettings = {},
                onNavigateToAccount = {},
                onNavigateToDataPrivacy = {},
                onNavigateToStatistics = {},
                onNavigateToAppearance = {},
                onNavigateToAdvanced = {},
                onNavigateToRetention = {},
                onNavigateToPolling = {},
                onOpenFutures = {}
            )
        }
    }
}

@Composable
private fun ResponsiveContainer(
    widthDp: Int,
    fontScale: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDensity provides Density(density = LocalDensity.current.density, fontScale = fontScale)) {
        Box(modifier = modifier.width(widthDp.dp)) {
            LibreDisplayTheme {
                content()
            }
        }
    }
}

internal fun ComposeContentTestRule.assertNoOverlap(
    first: SemanticsNodeInteraction,
    second: SemanticsNodeInteraction,
    firstLabel: String,
    secondLabel: String,
    context: String
) {
    // getUnclippedBoundsInRoot() fetches the semantics node and throws if it is missing, so it
    // doubles as an existence check while returning the layout rectangle regardless of scroll.
    val firstBounds = first.getUnclippedBoundsInRoot()
    val secondBounds = second.getUnclippedBoundsInRoot()
    val intersection = firstBounds.intersection(secondBounds)
    assertEquals(
        "$firstLabel overlaps $secondLabel at $context. first=$firstBounds second=$secondBounds intersection=$intersection",
        0f,
        intersection.area,
        0.0f
    )
}

private fun DpRect.intersection(other: DpRect): OverlapArea {
    val left = max(left.value, other.left.value)
    val top = max(top.value, other.top.value)
    val right = min(right.value, other.right.value)
    val bottom = min(bottom.value, other.bottom.value)
    if (right <= left || bottom <= top) {
        return OverlapArea(0f)
    }
    return OverlapArea((right - left) * (bottom - top))
}

private data class OverlapArea(val area: Float)

private fun metricTilesForScenario(
    scenario: GlucoseScenario,
    reading: GlucoseReading?,
    points: List<GlucoseHistoryPoint>
): List<QuickMetricTileUi> {
    if (scenario == GlucoseScenario.LONG_METRIC_VALUES) {
        return buildQuickMetricTiles(
            belowDuration = java.time.Duration.ZERO,
            belowPercent = 100,
            inRangeDuration = java.time.Duration.ofMinutes(59),
            inRangePercent = 1,
            aboveDuration = java.time.Duration.ofHours(23).plusMinutes(59),
            abovePercent = 99,
            minValueMgDl = 54,
            maxValueMgDl = 312,
            gmiValue = 12.8,
            averageValueMgDl = 245,
            veryLowEpisodes = 0,
            veryHighEpisodes = 10,
            dataCoveragePercent = 0,
            dataMissingDescription = "Za mało danych do dokładnej estymacji"
        )
    }
    if (reading == null || points.size < 2) {
        return emptyList()
    }
    val distribution = rangeDistributionFromHistory(points, 80, 180)
    val average = points.map { it.value }.average().toInt()
    val gmi = GlucoseMetricsCalculator.calculateGmi(points.map { it.value }.average())
    return buildQuickMetricTiles(
        belowDuration = distribution?.belowRangeDuration,
        belowPercent = distribution?.belowRangePercent,
        inRangeDuration = distribution?.inRangeDuration,
        inRangePercent = distribution?.inRangePercent,
        aboveDuration = distribution?.aboveRangeDuration,
        abovePercent = distribution?.aboveRangePercent,
        minValueMgDl = points.minOfOrNull { it.value },
        maxValueMgDl = points.maxOfOrNull { it.value },
        gmiValue = gmi,
        averageValueMgDl = average,
        veryLowEpisodes = countEpisodes(points) { it < 54 },
        veryHighEpisodes = countEpisodes(points) { it > 250 },
        dataCoveragePercent = 100,
        dataMissingDescription = if (scenario == GlucoseScenario.DATA_WITH_GAPS) "Przerwy w danych" else "Brak przerw"
    )
}

private fun Double.formatOneDecimal(): String = "%.1f".format(this).replace('.', ',')

