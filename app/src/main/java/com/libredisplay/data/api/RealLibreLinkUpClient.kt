package com.libredisplay.data.api

import com.libredisplay.data.model.GlucoseReading

/**
 * Real implementation placeholder.
 *
 * IMPORTANT:
 * - This class intentionally contains TODOs only.
 * - No API endpoints are hardcoded here.
 * - Add official LibreLinkUp integration details in your environment before use.
 *
 * Suggested implementation points:
 * 1) Build Retrofit + OkHttp clients.
 * 2) Implement credential login and keep auth token in-memory only.
 * 3) Fetch connection list and choose a patient.
 * 4) Fetch latest glucose reading and map to [GlucoseReading].
 * 5) Never log passwords or auth tokens.
 *
 * @param region The server region selected by the user (EU, US, DE, FR).
 */
class RealLibreLinkUpClient(private val region: String) : LibreLinkUpClient {

    // TODO: Store auth token in memory only – never persist, never log
    private var authToken: String? = null

    // TODO: Store the first patient ID after calling getConnections()
    private var patientId: String? = null

    override suspend fun login(email: String, password: String) {
        // TODO: Implement LibreLinkUp login call for selected [region]
        // On success store auth token in [authToken]
        // NEVER log [password] or [authToken]
        throw NotImplementedError(
            "RealLibreLinkUpClient.login() not implemented yet. " +
            "See class-level KDoc for implementation instructions."
        )
    }

    override suspend fun getConnections(): List<String> {
        // TODO: Implement LibreLinkUp connections request
        // Return a list of connection/patient IDs
        throw NotImplementedError(
            "RealLibreLinkUpClient.getConnections() not implemented yet. " +
            "See class-level KDoc for implementation instructions."
        )
    }

    override suspend fun getLatestReading(): GlucoseReading? {
        // TODO: Implement latest reading request for selected patient
        // Map remote payload to [GlucoseReading]
        throw NotImplementedError(
            "RealLibreLinkUpClient.getLatestReading() not implemented yet. " +
            "See class-level KDoc for implementation instructions."
        )
    }
}

