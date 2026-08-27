package com.libredisplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationStateTest {

    @Test
    fun switchingToTopLevelDoesNotDuplicateMonitoringRoot() {
        val state = AppNavigationState(listOf(AppScreen.Monitoring))
            .navigateTo(AppScreen.Analytics)
            .navigateTo(AppScreen.Futures)
            .navigateTo(AppScreen.Settings)
            .navigateTo(AppScreen.Analytics)

        assertEquals(listOf(AppScreen.Monitoring, AppScreen.Analytics), state.stack)
    }

    @Test
    fun nestedSettingsPath_popsBackToExactParentSequence() {
        var state = AppNavigationState(listOf(AppScreen.Monitoring))
            .navigateTo(AppScreen.Settings)
            .navigateTo(AppScreen.PrivacyData)
            .navigateTo(AppScreen.Retention)

        assertEquals(AppScreen.Retention, state.current)
        state = state.navigateBack()
        assertEquals(AppScreen.PrivacyData, state.current)
        state = state.navigateBack()
        assertEquals(AppScreen.Settings, state.current)
        state = state.navigateBack()
        assertEquals(AppScreen.Monitoring, state.current)
    }

    @Test
    fun loginOnlyLaunch_preservesStartAsBackTarget() {
        val state = initialNavigationState(AppScreen.Settings, showLoginOnly = true)

        assertEquals(listOf(AppScreen.Start, AppScreen.Settings), state.stack)
        assertTrue(state.canNavigateBack())
    }

    @Test
    fun allMaxDepthRoutes_popBackExactlyOneScreenAtATime() {
        val routes = listOf(
            allMaxDepthRoutesFrom(AppScreen.Monitoring),
            allMaxDepthRoutesFrom(AppScreen.Settings)
        ).flatten().distinct().filter { route -> isAppendOnlyRoute(route) }

        assertFalse(routes.isEmpty())

        routes.forEach { route ->
            var state = AppNavigationState(listOf(route.first()))
            route.drop(1).forEach { next -> state = state.navigateTo(next) }
            assertEquals(route, state.stack)

            route.dropLast(1).asReversed().forEach { expected ->
                state = state.navigateBack()
                assertEquals(expected, state.current)
            }
        }
    }

    @Test
    fun topLevelSwitching_keepsExpectedBackTargets() {
        var state = AppNavigationState(listOf(AppScreen.Monitoring))
        state = state.navigateTo(AppScreen.Settings)
        state = state.navigateTo(AppScreen.SettingsHomeMetrics)
        assertEquals(listOf(AppScreen.Monitoring, AppScreen.Settings, AppScreen.SettingsHomeMetrics), state.stack)

        state = state.navigateTo(AppScreen.Analytics)
        assertEquals(listOf(AppScreen.Monitoring, AppScreen.Analytics), state.stack)

        state = state.navigateTo(AppScreen.Futures)
        assertEquals(listOf(AppScreen.Monitoring, AppScreen.Futures), state.stack)

        state = state.navigateBack()
        assertEquals(AppScreen.Monitoring, state.current)
    }

    @Test
    fun allMaximumDepthRoutes_canBeTraversedForwardAndBackward() {
        val routes = AppScreen.entries
            .filter { it.isTopLevelDestination() || it == AppScreen.Start }
            .flatMap { allMaxDepthRoutesFrom(it) }
            .filter { route -> isAppendOnlyRoute(route) }
            .distinct()

        assertTrue(routes.isNotEmpty())

        routes.forEach { route ->
            var state = AppNavigationState(listOf(route.first()))
            route.drop(1).forEach { destination ->
                state = state.navigateTo(destination)
                assertEquals(destination, state.current)
            }
            route.dropLast(1).asReversed().forEach { expected ->
                state = state.navigateBack()
                assertEquals(expected, state.current)
            }
        }
    }

    @Test
    fun bottomNavigationSwitchingOrders_doNotCreateUnexpectedDuplicates() {
        val sequences = listOf(
            listOf(AppScreen.Analytics, AppScreen.Futures, AppScreen.Settings, AppScreen.Monitoring),
            listOf(AppScreen.Settings, AppScreen.Analytics, AppScreen.Futures, AppScreen.Settings),
            listOf(AppScreen.Analytics, AppScreen.Monitoring, AppScreen.Futures, AppScreen.Analytics, AppScreen.Settings)
        )

        sequences.forEach { sequence ->
            var state = AppNavigationState(listOf(AppScreen.Monitoring))
            sequence.forEach { destination ->
                state = state.navigateTo(destination)
                assertEquals(destination, state.current)
                assertEquals(state.stack.distinct(), state.stack)
                val topLevelCount = state.stack.count { it.isTopLevelDestination() }
                assertTrue("stack=${state.stack}", topLevelCount <= 2)
            }
        }
    }

    /**
     * A route can be traversed forward-and-backward with a strict "pop exactly one" invariant
     * only when every forward navigation appends exactly one screen. Top-level switches that
     * reset the back stack (e.g. Monitoring always resets to a single root) are excluded here
     * because those transitions are covered by the dedicated top-level switching tests.
     */
    private fun isAppendOnlyRoute(route: List<AppScreen>): Boolean {
        var state = AppNavigationState(listOf(route.first()))
        route.drop(1).forEach { destination ->
            val next = state.navigateTo(destination)
            if (next.stack != state.stack + destination) return false
            state = next
        }
        return true
    }
}

