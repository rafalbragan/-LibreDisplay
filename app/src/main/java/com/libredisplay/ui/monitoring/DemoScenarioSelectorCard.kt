package com.libredisplay.ui.monitoring

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libredisplay.BuildConfig
import com.libredisplay.data.demo.DemoScenario
import com.libredisplay.ui.theme.LibreCareColors

/**
 * TEST / DEMO SCENARIO selector — visible only in DEBUG builds running in Demo mode.
 *
 * In release builds [BuildConfig.DEBUG] is `false`, so the entire call-site is
 * dead code eliminated by R8/ProGuard. The selector is NEVER present in release APKs.
 *
 * The Firebase App Testing Agent selects a scenario by tapping the expand button and
 * then tapping the scenario name chip. After selection the ViewModel triggers a fresh
 * data fetch and the Home / Analysis screens reflect the new scenario data.
 */
@Composable
internal fun DemoScenarioSelectorCard(
    currentScenario: DemoScenario?,
    onScenarioSelected: (DemoScenario?) -> Unit
) {
    // Hard compile-time guard: this composable does nothing in release builds.
    if (!BuildConfig.DEBUG) return

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LibreCareTestTags.DEMO_SCENARIO_SELECTOR),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = "TEST / DEMO SCENARIO",
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(LibreCareTestTags.DEMO_SCENARIO_EXPAND_BUTTON)
            ) {
                Text(
                    text = currentScenario?.displayName ?: "Domyślne demo",
                    color = Color(0xFFE2E8F0)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                // "Default" option restores normal mock demo behaviour
                TextButton(
                    onClick = {
                        onScenarioSelected(null)
                        expanded = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(LibreCareTestTags.demoScenarioOption("DEFAULT"))
                ) {
                    Text(
                        text = "Domyślne demo",
                        color = if (currentScenario == null) Color(0xFF38BDF8) else LibreCareColors.TextSecondary,
                        fontWeight = if (currentScenario == null) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                DemoScenario.entries.forEach { scenario ->
                    TextButton(
                        onClick = {
                            onScenarioSelected(scenario)
                            expanded = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(LibreCareTestTags.demoScenarioOption(scenario.name))
                    ) {
                        Text(
                            text = scenario.displayName,
                            color = if (scenario == currentScenario) Color(0xFF38BDF8) else LibreCareColors.TextPrimary,
                            fontWeight = if (scenario == currentScenario) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

