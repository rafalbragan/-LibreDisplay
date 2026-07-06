package com.libredisplay.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiagnosticSnapshot(
    val loginStatus: String = "Nieznany",
    val tokenStatus: String = "Nieznany",
    val getConnectionsStatus: String = "Nieznany",
    val getLatestGraphStatus: String = "Nieznany",
    val lastEndpoint: String = "Brak",
    val lastMethod: String = "Brak",
    val lastHttpCode: Int? = null,
    val lastError: String = "Brak",
    val pollingStatus: String = "Zatrzymane",
    val lastWarning: String = "Brak"
)

object DiagnosticStatus {
    private val state = MutableStateFlow(DiagnosticSnapshot())

    fun flow(): StateFlow<DiagnosticSnapshot> = state.asStateFlow()

    fun snapshot(): DiagnosticSnapshot = state.value

    fun reset() {
        state.value = DiagnosticSnapshot()
    }

    fun setLogin(ok: Boolean, detail: String = "") {
        update { copy(loginStatus = if (ok) "OK" else "Błąd${detail.prefix()}" ) }
    }

    fun setToken(present: Boolean) {
        update { copy(tokenStatus = if (present) "OK" else "Brak") }
    }

    fun setGetConnections(status: String) {
        update { copy(getConnectionsStatus = status) }
    }

    fun setGetLatestGraph(status: String) {
        update { copy(getLatestGraphStatus = status) }
    }

    fun setLastEndpoint(endpoint: String) {
        update { copy(lastEndpoint = endpoint) }
    }

    fun setLastError(error: String) {
        update { copy(lastError = error.ifBlank { "Brak" }) }
    }

    fun setPolling(active: Boolean, detail: String = "") {
        update { copy(pollingStatus = if (active) "Aktywne${detail.prefix()}" else "Zatrzymane${detail.prefix()}" ) }
    }

    fun setWarning(warning: String) {
        update { copy(lastWarning = warning.ifBlank { "Brak" }) }
    }

    fun setRequest(step: String, url: String, method: String) {
        update {
            copy(
                lastEndpoint = "$step • $url",
                lastMethod = method,
                getConnectionsStatus = if (step == "CONNECTIONS") "REQUEST" else getConnectionsStatus,
                getLatestGraphStatus = if (step == "GRAPH") "REQUEST" else getLatestGraphStatus
            )
        }
    }

    fun setResponse(step: String, httpCode: Int, responseBody: String) {
        update {
            copy(
                lastHttpCode = httpCode,
                lastError = if (httpCode >= 400) responseBody.ifBlank { "HTTP $httpCode" } else lastError,
                getConnectionsStatus = if (step == "CONNECTIONS") "HTTP $httpCode" else getConnectionsStatus,
                getLatestGraphStatus = if (step == "GRAPH") "HTTP $httpCode" else getLatestGraphStatus
            )
        }
    }

    private fun update(block: DiagnosticSnapshot.() -> DiagnosticSnapshot) {
        state.value = state.value.block()
    }

    private fun String.prefix(): String = if (isBlank()) "" else ": $this"
}
