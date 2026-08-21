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
        ).flatten().distinct()

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
}

