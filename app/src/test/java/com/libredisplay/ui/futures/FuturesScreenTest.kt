package com.libredisplay.ui.futures

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.libredisplay.ui.monitoring.LibreCareTestTags
import com.libredisplay.ui.theme.LibreDisplayTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FuturesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun futuresScreen_showsPrototypeContentAndFiltersByAudience() {
        composeRule.setContent {
            LibreDisplayTheme {
                FuturesScreen(viewModel = FuturesViewModel())
            }
        }

        composeRule.onNodeWithTag(LibreCareTestTags.FUTURES_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Filtr perspektywy").assertIsDisplayed()
        composeRule.onNodeWithText("Najbliższe do wdrożenia").assertIsDisplayed()
        composeRule.onNodeWithTag(LibreCareTestTags.futuresAudience("lekarz")).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(LibreCareTestTags.futuresAudience("lekarz")).assertIsDisplayed()
    }
}

