package com.libredisplay

internal data class AppNavigationState(
    val stack: List<AppScreen>
) {
    val current: AppScreen get() = stack.last()
}

internal fun initialNavigationState(launchScreen: AppScreen, showLoginOnly: Boolean): AppNavigationState {
    val stack = if (launchScreen == AppScreen.Settings && showLoginOnly) {
        listOf(AppScreen.Start, AppScreen.Settings)
    } else {
        listOf(launchScreen)
    }
    return AppNavigationState(stack)
}

internal fun AppScreen.isTopLevelDestination(): Boolean = when (this) {
    AppScreen.Monitoring,
    AppScreen.Analytics,
    AppScreen.Futures,
    AppScreen.Settings -> true
    else -> false
}

internal fun AppNavigationState.navigateTo(screen: AppScreen): AppNavigationState {
    if (screen == current) return this
    if (screen.isTopLevelDestination()) {
        return copy(stack = switchToTopLevel(stack, screen))
    }
    return copy(stack = stack + screen)
}

internal fun AppNavigationState.navigateBack(): AppNavigationState {
    if (stack.size <= 1) return this
    return copy(stack = stack.dropLast(1))
}

internal fun AppNavigationState.canNavigateBack(): Boolean = stack.size > 1

internal fun switchToTopLevel(currentStack: List<AppScreen>, destination: AppScreen): List<AppScreen> {
    if (destination == AppScreen.Monitoring) {
        return listOf(AppScreen.Monitoring)
    }

    val root = currentStack.firstOrNull()
    return when {
        root == AppScreen.Start && currentStack.contains(AppScreen.Settings) && destination == AppScreen.Settings -> {
            listOf(AppScreen.Start, AppScreen.Settings)
        }
        root == AppScreen.Start -> listOf(AppScreen.Start, destination)
        else -> listOf(AppScreen.Monitoring, destination)
    }
}

internal fun navigationGraphEdges(): Map<AppScreen, List<AppScreen>> = mapOf(
    AppScreen.Monitoring to listOf(AppScreen.Analytics, AppScreen.Futures, AppScreen.Settings, AppScreen.SettingsHomeMetrics),
    AppScreen.Analytics to listOf(AppScreen.Futures),
    AppScreen.Futures to emptyList(),
    AppScreen.Settings to listOf(
        AppScreen.SettingsTargetRange,
        AppScreen.SettingsHomeMetrics,
        AppScreen.SettingsHbA1c,
        AppScreen.SettingsAccount,
        AppScreen.PrivacyData,
        AppScreen.Statistics,
        AppScreen.About,
        AppScreen.Diagnostics,
        AppScreen.Retention,
        AppScreen.Polling
    ),
    AppScreen.PrivacyData to listOf(AppScreen.Statistics, AppScreen.Retention),
    AppScreen.SettingsTargetRange to emptyList(),
    AppScreen.SettingsHomeMetrics to emptyList(),
    AppScreen.SettingsHbA1c to emptyList(),
    AppScreen.SettingsAccount to emptyList(),
    AppScreen.Statistics to emptyList(),
    AppScreen.About to listOf(AppScreen.Statistics),
    AppScreen.Diagnostics to emptyList(),
    AppScreen.Retention to emptyList(),
    AppScreen.Polling to emptyList(),
    AppScreen.Start to listOf(AppScreen.Settings, AppScreen.Monitoring)
)

internal fun allMaxDepthRoutesFrom(topLevel: AppScreen): List<List<AppScreen>> {
    val edges = navigationGraphEdges()
    val routes = mutableListOf<List<AppScreen>>()

    fun dfs(route: List<AppScreen>) {
        val next = edges[route.last()].orEmpty()
        if (next.isEmpty()) {
            routes += route
            return
        }
        next.forEach { dfs(route + it) }
    }

    dfs(listOf(topLevel))
    return routes.distinct()
}

