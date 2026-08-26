package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.QuickMetricId

internal object LibreCareTestTags {
    const val HOME_CURRENT_GLUCOSE_CARD = "home.current.card"
    const val HOME_CURRENT_GLUCOSE_VALUE = "home.current.value"
    const val HOME_CURRENT_GLUCOSE_UNIT = "home.current.unit"
    const val HOME_CURRENT_GLUCOSE_TREND = "home.current.trend"
    const val HOME_CURRENT_GLUCOSE_SEVERITY = "home.current.severity"
    const val TOP_BAR_TITLE = "topBar.title"
    const val TOP_BAR_VERSION = "topBar.version"
    const val TOP_BAR_LAST_UPDATE = "topBar.lastUpdate"
    const val TOP_BAR_UI_AUDIT = "topBar.uiAudit"
    const val METRICS_HEADER = "home.metrics.header"
    const val METRICS_EDIT = "home.metrics.edit"
    const val METRICS_STRIP = "home.metrics.strip"
    const val HOME_CHART_SELECTED_LABEL = "home.chart.selectedLabel"
    const val HOME_CHART_NAVIGATOR = "home.chart.navigator"
    const val HOME_CHART_RANGE_SELECTOR = "home.chart.rangeSelector"

    // Debug-only: TEST / DEMO SCENARIO selector
    const val DEMO_SCENARIO_SELECTOR = "debug.demo.scenarioSelector"
    const val DEMO_SCENARIO_EXPAND_BUTTON = "debug.demo.scenarioExpandButton"
    fun demoScenarioOption(name: String) = "debug.demo.scenario.$name"

    internal fun bottomNav(item: DashboardNavItem): String = "bottomNav.${item.name.lowercase()}"

    internal fun metricTile(metricId: QuickMetricId): String = "home.metric.${metricId.name.lowercase()}"

    internal fun rangeChip(range: HomeChartRange): String = "home.range.${range.name.lowercase()}"
}



