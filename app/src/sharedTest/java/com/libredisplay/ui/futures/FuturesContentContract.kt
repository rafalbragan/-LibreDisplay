package com.libredisplay.ui.futures

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.libredisplay.ui.monitoring.DashboardNavItem
import com.libredisplay.ui.monitoring.LibreCareTestTags
import com.libredisplay.ui.theme.LibreDisplayTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

abstract class FuturesContentContract {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun inventoryShowsLimitationsWithoutOldPrototypeOrApprovalClaims() {
        composeRule.setContent { LibreDisplayTheme { FuturesScreen(viewModel = FuturesViewModel()) } }
        composeRule.onNodeWithText("Funkcje LibreCare").assertIsDisplayed()
        composeRule.onNodeWithText("Najbliższe do wdrożenia").assertDoesNotExist()
        composeRule.onNodeWithText(FuturesStatus.IN_DEVELOPMENT.description).assertDoesNotExist()
        composeRule.onNodeWithText(FuturesStatus.PLANNED.description).assertDoesNotExist()
        val ideas = FuturesUiState().ideas
        listOf("libreview-csv-import", "backup-and-restore", "caregiver-senior-clinician-modes", "alerts-and-refresh").forEach { id ->
            composeRule.onNodeWithText(ideas.single { it.id == id }.nowValue)
                .performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun seniorFilterHidesUnmatchedCardsAndEveryoneRestoresThem() {
        composeRule.setContent { LibreDisplayTheme { FuturesScreen(viewModel = FuturesViewModel()) } }
        composeRule.onNodeWithTag(LibreCareTestTags.futuresAudience("senior"))
            .performScrollTo().performClick().assertIsSelected()
        composeRule.onNodeWithTag(LibreCareTestTags.futuresCard("person-profiles")).assertDoesNotExist()
        composeRule.onNodeWithTag(LibreCareTestTags.futuresCard("history-and-chart")).assertExists()
        composeRule.onNodeWithText("Filtr zmienia tylko tę listę, nie tryb aplikacji.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(LibreCareTestTags.futuresAudience("wszyscy"))
            .performScrollTo().performClick().assertIsSelected()
        composeRule.onNodeWithTag(LibreCareTestTags.futuresCard("person-profiles")).assertExists()
    }

    @Test
    fun expansionShowsAndHidesNextStepButKeepsLimitations() {
        val idea = FuturesUiState().ideas.single { it.id == "backup-and-restore" }
        composeRule.setContent { LibreDisplayTheme { FuturesScreen(viewModel = FuturesViewModel()) } }
        composeRule.onNodeWithText(idea.nextStep).assertDoesNotExist()
        composeRule.onNodeWithTag("futures_toggle_${idea.id}").performScrollTo().performClick()
        composeRule.onNodeWithText(idea.nextStep).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("futures_toggle_${idea.id}").performScrollTo().performClick()
        composeRule.onNodeWithText(idea.nextStep).assertDoesNotExist()
        composeRule.onNodeWithText(idea.nowValue).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun everyStatusHasItsOwnExplanationWhenPresent() {
        // Synthetic entries exercise rendering only; they do not approve real proposals.
        val example = FuturesUiState().ideas.first()
        val state = FuturesUiState(ideas = FuturesStatus.entries.map {
            example.copy(id = it.name, status = it)
        })
        composeRule.setContent { LibreDisplayTheme { FuturesContent(state, {}, {}) } }
        FuturesStatus.entries.forEach {
            composeRule.onNodeWithText(it.description).performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag(LibreCareTestTags.futuresCard(it.name)).assertExists()
        }
    }

    @Test
    fun emptyFilterShowsAnExplicitRecoveryHint() {
        composeRule.setContent {
            LibreDisplayTheme { FuturesContent(FuturesUiState(ideas = emptyList()), {}, {}) }
        }
        composeRule.onNodeWithText("Brak funkcji dla wybranej perspektywy. Wybierz filtr Wszyscy.")
            .performScrollTo().assertIsDisplayed()
        FuturesStatus.entries.forEach { composeRule.onNodeWithText(it.description).assertDoesNotExist() }
    }

    @Test
    fun existingBottomNavigationInvokesTheCorrectDestinations() {
        val opened = mutableListOf<String>()
        composeRule.setContent {
            LibreDisplayTheme {
                FuturesContent(FuturesUiState(), {}, {},
                    onOpenHome = { opened.add("home") },
                    onOpenAnalytics = { opened.add("analytics") },
                    onOpenSettings = { opened.add("settings") })
            }
        }
        listOf(DashboardNavItem.GLOWNA, DashboardNavItem.HISTORIA, DashboardNavItem.USTAWIENIA).forEach {
            composeRule.onNodeWithTag(LibreCareTestTags.bottomNav(it)).assertIsDisplayed().performClick()
        }
        assertEquals(listOf("home", "analytics", "settings"), opened)
    }

    @Test
    fun largeTextKeepsStatusDetailsAndToggleReachable() {
        val idea = FuturesUiState().ideas.single { it.id == "backup-and-restore" }
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.8f)) {
                LibreDisplayTheme { FuturesScreen(viewModel = FuturesViewModel()) }
            }
        }
        composeRule.onNodeWithTag("futures_toggle_${idea.id}").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText(idea.nextStep).performScrollTo().assertIsDisplayed()
    }
}
