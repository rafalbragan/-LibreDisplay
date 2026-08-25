package com.libredisplay.ui.monitoring

import android.app.Application
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import com.libredisplay.ui.theme.LibreDisplayTheme
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HomeChartRangeSelectorBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rangeSelector_scrollDoesNotAutoChangeSelection_andAllowsManualAccessToFarRanges() {
        val options = homeChartRangeOptions(Duration.ofDays(400))
        var selectedRange by mutableStateOf(HomeChartRange.LAST_1_HOUR)

        composeRule.setContent {
            LibreDisplayTheme {
                androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.width(320.dp)) {
                    HomeChartRangeSelector(
                        options = options,
                        selectedRange = selectedRange,
                        onRangeSelected = { selectedRange = it }
                    )
                }
            }
        }

        composeRule.onNodeWithTag(LibreCareTestTags.rangeChip(HomeChartRange.LAST_1_HOUR)).assertIsSelected()

        composeRule.onNodeWithTag(LibreCareTestTags.HOME_CHART_RANGE_SELECTOR)
            .performTouchInput {
                repeat(6) { swipeLeft() }
            }

        // Scrolling should not trigger selection changes by itself.
        composeRule.onNodeWithTag(LibreCareTestTags.rangeChip(HomeChartRange.LAST_1_HOUR)).assertIsSelected()

        // User can manually scroll to long ranges (important in landscape and narrow widths).
        composeRule.onNodeWithTag(LibreCareTestTags.rangeChip(HomeChartRange.ALL_AVAILABLE)).assertIsDisplayed()
    }
}



