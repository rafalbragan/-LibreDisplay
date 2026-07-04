package com.libredisplay.data.repository

import android.content.Context
import android.util.Log
import com.libredisplay.data.api.LibreLinkUpClient
import com.libredisplay.data.api.MockLibreLinkUpClient
import com.libredisplay.data.api.RealLibreLinkUpClient
import com.libredisplay.data.model.GlucoseReading

private const val TAG = "GlucoseRepository"

/**
 * Orchestrates authentication and data fetching from LibreLinkUp.
 *
 * Selects between [MockLibreLinkUpClient] and [RealLibreLinkUpClient] based on
 * the current [AppSettings.useMock] flag.
 *
 * Never logs passwords or auth tokens.
 */
class GlucoseRepository(private val settingsRepository: SettingsRepository) {

    private var client: LibreLinkUpClient? = null
    private var loggedIn = false

    /**
     * Ensures the client is authenticated.  Called automatically before
     * [fetchLatestReading] if not already authenticated.
     */
    suspend fun ensureAuthenticated() {
        val settings = settingsRepository.loadSettings()
        val newClient: LibreLinkUpClient = if (settings.useMock) {
            MockLibreLinkUpClient()
        } else {
            RealLibreLinkUpClient(settings.region)
        }
        client = newClient
        // NOTE: password intentionally not logged
        newClient.login(settings.email, settings.password)
        loggedIn = true
        Log.d(TAG, "Authentication successful (useMock=${settings.useMock})")
    }

    /**
     * Fetches the latest glucose reading.
     *
     * Re-authenticates automatically if not logged in.
     *
     * @return [GlucoseReading] or null if no data is available.
     * @throws Exception propagated from the underlying client.
     */
    suspend fun fetchLatestReading(): GlucoseReading? {
        if (!loggedIn || client == null) {
            ensureAuthenticated()
        }
        return client!!.getLatestReading()
    }

    /** Force re-authentication on the next call (e.g. after settings change). */
    fun invalidate() {
        client = null
        loggedIn = false
    }
}

