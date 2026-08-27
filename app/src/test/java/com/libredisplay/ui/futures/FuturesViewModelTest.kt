package com.libredisplay.ui.futures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuturesViewModelTest {

    @Test
    fun initialState_expandsAnalysisPrototypeAndShowsIdeas() {
        val viewModel = FuturesViewModel()

        val state = viewModel.uiState.value
        assertEquals(FuturesAudience.WSZYSCY, state.selectedAudience)
        assertTrue("analysis-prototype" in state.expandedIdeaIds)
        assertTrue(state.visibleIdeas.isNotEmpty())
    }

    @Test
    fun selectingAudience_filtersVisibleIdeas() {
        val viewModel = FuturesViewModel()

        viewModel.selectAudience(FuturesAudience.LEKARZ)

        val state = viewModel.uiState.value
        assertEquals(FuturesAudience.LEKARZ, state.selectedAudience)
        assertTrue(state.visibleIdeas.isNotEmpty())
        assertTrue(state.visibleIdeas.all { FuturesAudience.LEKARZ in it.audiences })
    }

    @Test
    fun togglingIdea_addsAndRemovesExpandedState() {
        val viewModel = FuturesViewModel()

        viewModel.toggleIdea("hypo-risk")
        assertTrue("hypo-risk" in viewModel.uiState.value.expandedIdeaIds)

        viewModel.toggleIdea("hypo-risk")
        assertFalse("hypo-risk" in viewModel.uiState.value.expandedIdeaIds)
    }
}

