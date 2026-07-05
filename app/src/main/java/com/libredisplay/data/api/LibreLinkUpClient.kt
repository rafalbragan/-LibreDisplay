package com.libredisplay.data.api

import com.libredisplay.data.model.GlucoseReading

/**
 * Contract for interacting with the LibreLinkUp backend.
 *
 * Two implementations exist:
 *  - [MockLibreLinkUpClient]  – random data, no network required (default / testing).
 *  - [RealLibreLinkUpClient]  – real LibreLinkUp API via Retrofit + OkHttp.
 */
interface LibreLinkUpClient {

    /**
     * Authenticate with LibreLinkUp and obtain a session token.
     *
     * @throws LibreLinkUpException if authentication fails.
     */
    suspend fun login(email: String, password: String)

    /**
     * Retrieve the list of patient connections for the logged-in account.
     *
     * @return List of patient identifiers / names.
     * @throws LibreLinkUpException if the request fails.
     */
    suspend fun getConnections(): List<String>

    /**
     * Fetch the most recent glucose reading from the first available connection.
     *
     * @return Latest [GlucoseReading], or null if no data is available.
     * @throws LibreLinkUpException if the request fails.
     */
    suspend fun getLatestReading(): GlucoseReading?
}

/** Domain-level exception thrown by any [LibreLinkUpClient] implementation. */
class LibreLinkUpException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

