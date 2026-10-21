package com.libredisplay.ui.futures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuturesViewModelTest {

    @Test
    fun initialState_expandsHistoryAndShowsAllCapabilities() {
        val viewModel = FuturesViewModel()

        val state = viewModel.uiState.value
        assertEquals(FuturesAudience.WSZYSCY, state.selectedAudience)
        assertEquals(setOf("history-and-chart"), state.expandedIdeaIds)
        assertEquals(state.ideas, state.visibleIdeas)
        assertEquals(10, state.ideas.size)
    }

    @Test
    fun everyAudience_filtersExactlyAndEveryoneRestoresAll() {
        val viewModel = FuturesViewModel()
        val original = viewModel.uiState.value
        FuturesAudience.entries.forEach { audience ->
            viewModel.selectAudience(audience)
            val state = viewModel.uiState.value
            assertEquals(audience, state.selectedAudience)
            assertEquals(original.ideas.filter {
                audience == FuturesAudience.WSZYSCY || audience in it.audiences
            }, state.visibleIdeas)
            assertEquals(original.expandedIdeaIds, state.expandedIdeaIds)
        }
        viewModel.selectAudience(FuturesAudience.WSZYSCY)
        assertEquals(original.ideas, viewModel.uiState.value.visibleIdeas)
    }

    @Test
    fun togglingIdea_addsAndRemovesExpandedState() {
        val viewModel = FuturesViewModel()

        viewModel.toggleIdea("backup-and-restore")
        assertTrue("backup-and-restore" in viewModel.uiState.value.expandedIdeaIds)

        viewModel.toggleIdea("backup-and-restore")
        assertFalse("backup-and-restore" in viewModel.uiState.value.expandedIdeaIds)
        assertTrue("history-and-chart" in viewModel.uiState.value.expandedIdeaIds)
    }

    @Test
    fun statuses_matchVerifiedImplementationAndUnapprovedGaps() {
        val expected = mapOf(
            "monitoring-glucose" to FuturesStatus.AVAILABLE,
            "history-and-chart" to FuturesStatus.AVAILABLE,
            "libreview-csv-import" to FuturesStatus.AVAILABLE,
            "analytics-and-statistics" to FuturesStatus.AVAILABLE,
            "person-profiles" to FuturesStatus.AVAILABLE,
            "privacy-and-data" to FuturesStatus.AVAILABLE,
            "backup-and-restore" to FuturesStatus.UNDER_REVIEW,
            "caregiver-senior-clinician-modes" to FuturesStatus.UNDER_REVIEW,
            "alerts-and-refresh" to FuturesStatus.UNDER_REVIEW,
            "tir-and-metrics-accuracy" to FuturesStatus.UNDER_REVIEW
        )
        val ideas = FuturesUiState().ideas
        assertEquals(expected, ideas.associate { it.id to it.status })
        assertEquals(ideas.size, ideas.map { it.id }.distinct().size)
        assertFalse(ideas.any { it.status in setOf(FuturesStatus.PLANNED, FuturesStatus.IN_DEVELOPMENT) })
    }

    @Test
    fun everyEntryHasSummaryLimitationsDetailsAndNextStep() {
        FuturesUiState().ideas.forEach { idea ->
            assertTrue(idea.id, listOf(idea.title, idea.summary, idea.nowValue, idea.nextStep).all { it.isNotBlank() })
            assertTrue(idea.id, idea.highlights.isNotEmpty() && idea.highlights.all { it.isNotBlank() })
            assertTrue(idea.id, idea.audiences.isNotEmpty())
        }
        assertTrue(FuturesStatus.entries.all { it.label.isNotBlank() && it.description.isNotBlank() })
    }

    @Test
    fun audienceWithNoMatchesProducesAnEmptyList() {
        val state = FuturesUiState(selectedAudience = FuturesAudience.SENIOR,
            ideas = listOf(FuturesUiState().ideas.first().copy(audiences = setOf(FuturesAudience.LEKARZ))))
        assertTrue(state.visibleIdeas.isEmpty())
    }
}

