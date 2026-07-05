package com.libredisplay.data.repository

import android.util.Log
import com.libredisplay.data.api.LibreLinkApiClient
import com.libredisplay.data.api.LibreLinkUpException
import com.libredisplay.data.api.LibreLinkUpHttpException
import com.libredisplay.data.model.AppSettings

private const val TAG = "AuthRepository"
private const val COOLDOWN_MS = 2 * 60_000L

class AuthRepository(
    settingsRepository: SettingsRepository? = null,
    private val settingsProvider: (() -> AppSettings)? = null,
    private val apiClient: LibreLinkApiClient = LibreLinkApiClient()
) {

    private var token: String? = null
    private var resolvedRegion: String? = null
    private var cooldownUntilMs: Long = 0L
    private var lastLoginMs: Long = 0L

    fun currentToken(): String? = token

    fun currentRegion(): String = resolvedRegion ?: loadSettings().region

    fun cooldownRemainingMs(now: Long = System.currentTimeMillis()): Long {
        return (cooldownUntilMs - now).coerceAtLeast(0L)
    }

    suspend fun ensureLoggedIn(force: Boolean = false) {
        if (!force && token != null) return
        if (cooldownRemainingMs() > 0L) {
            throw LibreLinkUpException("Zbyt wiele prob polaczenia. Sprobuj pozniej lub sprawdz dane logowania.")
        }

        val settings = loadSettings()
        if (settings.email.isBlank() || settings.password.isBlank()) {
            throw LibreLinkUpException("Wpisz dane konta LibreLinkUp, na ktorym widzisz odczyty glukozy obserwowanej osoby.")
        }

        try {
            val result = apiClient.login(
                region = settings.region,
                email = settings.email,
                password = settings.password
            )
            token = result.token
            resolvedRegion = result.region
            lastLoginMs = System.currentTimeMillis()
            cooldownUntilMs = 0L
            Log.i(TAG, "Authenticated region=$resolvedRegion")
        } catch (e: LibreLinkUpHttpException) {
            val responseLower = e.responseBody.lowercase()
            if (responseLower.contains("terms") || responseLower.contains("policy") || responseLower.contains("consent")) {
                throw LibreLinkUpException("Wymagana jest akceptacja warunkow uslugi lub polityki prywatnosci w oficjalnej aplikacji LibreLinkUp.")
            }
            if (e.statusCode == 429 || e.statusCode == 430) {
                cooldownUntilMs = System.currentTimeMillis() + COOLDOWN_MS
                throw LibreLinkUpException("Serwer odrzucil kolejne proby logowania. Mozliwe zbyt czeste proby, zly region API albo brak wymaganych naglowkow. Odczekaj i sprobuj ponownie.")
            }
            if (e.statusCode == 401) {
                throw LibreLinkUpException("Nieprawidlowe dane logowania, niezaakceptowane warunki uslugi albo konto nie ma aktywnego polaczenia LibreLinkUp.")
            }
            throw LibreLinkUpException("Blad logowania do LibreLinkUp (HTTP ${e.statusCode})")
        }
    }

    fun clearSession() {
        token = null
        resolvedRegion = null
        lastLoginMs = 0L
        cooldownUntilMs = 0L
    }

    fun activateCooldown() {
        cooldownUntilMs = System.currentTimeMillis() + COOLDOWN_MS
    }

    private val settingsLoader: () -> AppSettings =
        settingsProvider ?: { settingsRepository?.loadSettings() ?: AppSettings() }

    private fun loadSettings(): AppSettings = settingsLoader()
}


