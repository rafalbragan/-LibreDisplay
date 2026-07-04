package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseReading

/**
 * All possible states the monitoring screen can be in.
 */
sealed class MonitoringUiState {

    /** Initial state while waiting for the first reading. */
    object Loading : MonitoringUiState()

    /**
     * A reading was successfully obtained.
     *
     * @param reading     The latest glucose reading.
     * @param dataAgeMin  How many minutes ago the reading was taken.
     * @param isStale     True when dataAgeMin > 20.
     * @param isRefreshing True while a background refresh is in progress.
     * @param error       Non-null if the last refresh attempt failed but
     *                    a cached reading is still shown.
     */
    data class Success(
        val reading: GlucoseReading,
        val dataAgeMin: Long,
        val isStale: Boolean,
        val isRefreshing: Boolean = false,
        val error: String? = null
    ) : MonitoringUiState()

    /**
     * An error occurred and there is no cached reading to show.
     *
     * @param message Human-readable error message.
     */
    data class Error(val message: String) : MonitoringUiState()
}

