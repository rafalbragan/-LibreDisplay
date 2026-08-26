package com.libredisplay.data.demo

import com.libredisplay.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global singleton controlling which [DemoScenario] is currently active in the debug demo mode
 * scenario selector.
 *
 * In release builds [selectScenario] is a strict no-op so [currentScenario] always returns `null`
 * and the [ScenarioAwareMockLibreLinkUpClient] falls back to the normal [MockLibreLinkUpClient]
 * behaviour. This guarantees production/release Live behaviour is unchanged.
 *
 * In debug builds the Firebase App Testing Agent selects a scenario through the on-screen
 * TEST / DEMO SCENARIO selector and the client returns deterministic synthetic data.
 */
object DemoScenarioController {

    private val _currentScenario = MutableStateFlow<DemoScenario?>(null)

    /** Observable current scenario. Emits `null` when the default demo data is active. */
    val currentScenarioFlow: StateFlow<DemoScenario?> = _currentScenario.asStateFlow()

    /** Snapshot of the current scenario. Thread-safe. */
    val currentScenario: DemoScenario? get() = _currentScenario.value

    /**
     * Select a scenario. Silently ignored in release builds.
     * Pass `null` to revert to the default [MockLibreLinkUpClient] behaviour.
     */
    fun selectScenario(scenario: DemoScenario?) {
        if (!BuildConfig.DEBUG) return   // hard guard – release builds cannot activate a scenario
        _currentScenario.value = scenario
    }

    /** Reset to default demo data (no scenario override). */
    fun reset() {
        if (!BuildConfig.DEBUG) return
        _currentScenario.value = null
    }
}

