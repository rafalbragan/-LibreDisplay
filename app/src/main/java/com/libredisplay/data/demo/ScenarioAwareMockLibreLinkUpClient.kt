package com.libredisplay.data.demo

import com.libredisplay.data.api.LibreLinkUpClient
import com.libredisplay.data.api.MockLibreLinkUpClient
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.LibreConnectionPerson
import java.time.Instant

/**
 * A [LibreLinkUpClient] that intercepts calls in demo mode and returns deterministic synthetic
 * data for the active [DemoScenario].
 *
 * When no scenario is selected ([DemoScenarioController.currentScenario] == `null`), every call
 * is delegated to [MockLibreLinkUpClient] so the normal default demo behaviour is preserved.
 *
 * In release builds [DemoScenarioController.selectScenario] is a no-op, so this client always
 * falls back to [MockLibreLinkUpClient] and production/release Live behaviour is unchanged.
 */
class ScenarioAwareMockLibreLinkUpClient : LibreLinkUpClient {

    private val fallback = MockLibreLinkUpClient()

    override suspend fun login(email: String, password: String) {
        fallback.login(email, password)
    }

    override suspend fun getConnections(): List<LibreConnectionPerson> {
        val scenario = DemoScenarioController.currentScenario
            ?: return fallback.getConnections()
        return ScenarioDataGenerator.personsForScenario(scenario)
    }

    override suspend fun getLatestReading(patientId: String?): GlucoseReading? {
        val scenario = DemoScenarioController.currentScenario
            ?: return fallback.getLatestReading(patientId)
        return ScenarioDataGenerator.generateReading(scenario, patientId, Instant.now())
    }
}

