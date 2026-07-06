package com.libredisplay.data.repository

import com.libredisplay.data.api.AuthCapableLibreLinkUpClient
import com.libredisplay.data.api.LibreLinkUpHttpException
import com.libredisplay.data.api.LibreResponseDecodingException
import com.libredisplay.data.api.NonRetryableLibreLinkUpException
import com.libredisplay.data.api.PersistedLibreLinkUpSession
import com.libredisplay.data.model.AppSettings
import com.libredisplay.diagnostics.DiagnosticLogger
import com.libredisplay.diagnostics.DiagnosticStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

class AuthRepository(
    private val settingsProvider: () -> AppSettings,
    private val client: AuthCapableLibreLinkUpClient,
    private val loginStateStore: LoginStateStore? = null,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() }
) {

    private val loginMutex = Mutex()
    private var lastTerminalLoginError: NonRetryableLibreLinkUpException? = null
    private var persistedSessionLoaded = false

    fun cooldownRemainingSeconds(nowMillis: Long = currentTimeMillis()): Long {
        val nextAllowed = loginStateStore?.loadNextAllowedLoginAt() ?: 0L
        val remainingMillis = (nextAllowed - nowMillis).coerceAtLeast(0L)
        return (remainingMillis + 999L) / 1000L
    }

    fun nextAllowedLoginAtMillis(): Long = loginStateStore?.loadNextAllowedLoginAt() ?: 0L

    fun clearLocalLoginCooldown() {
        loginStateStore?.clearNextAllowedLoginAt()
    }

    suspend fun ensureAuthenticated(force: Boolean = false) {
        val settings = settingsProvider()
        val snapshot = CredentialsSnapshot.fromSettings(settings)
        connectOnce(snapshot = snapshot, force = force)
    }

    suspend fun connectOnce(snapshot: CredentialsSnapshot, force: Boolean = true) {
        if (loginMutex.isLocked) {
            DiagnosticLogger.logInfo("AuthRepository", "LOGIN SINGLE-FLIGHT WAITING")
        }

        DiagnosticLogger.logInfo("AuthRepository", "LOGIN SINGLE-FLIGHT ENTER")
        loginMutex.withLock {
            try {
                if (!force && client.hasActiveSession()) {
                    DiagnosticLogger.logInfo("AuthRepository", "LOGIN SINGLE-FLIGHT SKIP - already logged in")
                    return
                }

                if (!force && !client.hasActiveSession()) {
                    tryImportPersistedSession()
                    if (client.hasActiveSession()) {
                        DiagnosticLogger.logInfo("AuthRepository", "LOGIN SINGLE-FLIGHT SKIP - reused persisted token")
                        return
                    }
                }

                val cooldownRemaining = cooldownRemainingSeconds()
                if (cooldownRemaining > 0) {
                    DiagnosticLogger.logWarning("AuthRepository", "RETRY BLOCKED reason=local cooldown remaining=${cooldownRemaining}s")
                    throw NonRetryableLibreLinkUpException(
                        "Lokalna przerwa bezpieczenstwa: aplikacja wstrzymala kolejna probe na ${formatRemaining(cooldownRemaining)}. Serwer nie potwierdzil, ze konto jest zablokowane."
                    )
                }

                if (!force && lastTerminalLoginError != null) {
                    DiagnosticLogger.logWarning("AuthRepository", "RETRY BLOCKED reason=manual retry required")
                    throw lastTerminalLoginError as NonRetryableLibreLinkUpException
                }

                if (!snapshot.isConfigured) {
                    return
                }

                DiagnosticLogger.logInfo(
                    "AuthRepository",
                    "Login attempt region=${snapshot.region} emailOriginalLength=${snapshot.emailOriginalLength} emailNormalizedLength=${snapshot.email.length} emailWasChangedByNormalization=${snapshot.emailWasChangedByNormalization} emailNormalized=${snapshot.emailNormalized} maskedNormalizedEmail=${snapshot.maskedNormalizedEmail} passwordCharCount=${snapshot.passwordCharCount} passwordCodePointCount=${snapshot.passwordCodePointCount} passwordUtf8ByteCount=${snapshot.passwordUtf8ByteCount} hasLeadingWhitespace=${snapshot.hasLeadingWhitespace} hasTrailingWhitespace=${snapshot.hasTrailingWhitespace} containsNewLine=${snapshot.containsNewLine} currentAndStoredPasswordEqual=${snapshot.currentAndStoredPasswordEqual}"
                )

                if (snapshot.email != snapshot.normalizedEmailForCheck) {
                    throw NonRetryableLibreLinkUpException("Wykryto niespojnosc danych logowania. Zadanie nie zostalo wyslane.")
                }

                try {
                    client.login(snapshot.email, snapshot.password, snapshot.region)
                    lastTerminalLoginError = null
                    loginStateStore?.clearNextAllowedLoginAt()
                    loginStateStore?.saveNormalizedEmail(snapshot.email)
                    client.exportSession()?.let { loginStateStore?.savePersistedSession(it) }
                    DiagnosticStatus.setLogin(ok = true)
                    DiagnosticStatus.setToken(true)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (throwable: Throwable) {
                    loginStateStore?.clearPersistedSession()
                    if (throwable is NonRetryableLibreLinkUpException) {
                        lastTerminalLoginError = throwable
                        DiagnosticLogger.logWarning("AuthRepository", "AUTO RETRY DISABLED")
                    }
                    val cooldownMs = cooldownForFailure(throwable)
                    if (cooldownMs > 0L) {
                        loginStateStore?.saveNextAllowedLoginAt(currentTimeMillis() + cooldownMs)
                    }
                    DiagnosticStatus.setLogin(ok = false, detail = throwable.message.orEmpty())
                    DiagnosticStatus.setLastError(throwable.message.orEmpty())
                    throw throwable
                }
            } finally {
                DiagnosticLogger.logInfo("AuthRepository", "LOGIN SINGLE-FLIGHT EXIT")
            }
        }
    }

    suspend fun ensureSessionFromStorageOnly(): Boolean {
        if (loginMutex.isLocked) {
            DiagnosticLogger.logInfo("AuthRepository", "LOGIN SINGLE-FLIGHT WAITING")
        }
        return loginMutex.withLock {
            if (client.hasActiveSession()) return@withLock true
            tryImportPersistedSession()
            client.hasActiveSession()
        }
    }

    fun clearSession() {
        client.clearSession()
        lastTerminalLoginError = null
        loginStateStore?.clearPersistedSession()
        DiagnosticStatus.setToken(false)
    }

    private fun tryImportPersistedSession() {
        if (persistedSessionLoaded) return
        persistedSessionLoaded = true
        val saved = loginStateStore?.loadPersistedSession() ?: return
        val expiresAt = saved.tokenExpiresAtEpochSeconds
        if (expiresAt != null && expiresAt <= currentTimeMillis() / 1000L) {
            loginStateStore.clearPersistedSession()
            return
        }
        client.importSession(saved)
    }

    private fun cooldownForFailure(throwable: Throwable): Long {
        if (throwable.isResponseDecodingFailure()) {
            return 0L
        }
        val baseCooldownMs = DEFAULT_LOCAL_COOLDOWN_MS
        return when (throwable) {
            is NonRetryableLibreLinkUpException -> baseCooldownMs
            is LibreLinkUpHttpException -> {
                val lockout = throwable.lockoutInfo?.lockoutSeconds ?: 0
                val retryAfter = throwable.retryAfterSeconds ?: 0
                val serverSeconds = maxOf(lockout, retryAfter)
                maxOf(baseCooldownMs, serverSeconds * 1000L)
            }
            else -> baseCooldownMs
        }
    }

    private fun formatRemaining(seconds: Long): String {
        val minutes = (seconds / 60).toInt()
        val secs = (seconds % 60).toInt()
        return "%02d:%02d".format(minutes, secs)
    }

    private companion object {
        const val DEFAULT_LOCAL_COOLDOWN_MS = 5 * 60 * 1000L
    }
}

private fun Throwable.isResponseDecodingFailure(): Boolean {
    if (this is LibreResponseDecodingException) return true
    var current: Throwable? = this
    while (current != null) {
        if (current is LibreResponseDecodingException) return true
        if (current::class.qualifiedName == "com.google.gson.stream.MalformedJsonException") return true
        current = current.cause
    }
    return false
}

data class CredentialsSnapshot(
    val email: String,
    val normalizedEmailForCheck: String,
    val password: String,
    val region: String,
    val emailOriginalLength: Int,
    val emailNormalized: Boolean,
    val emailWasChangedByNormalization: Boolean,
    val maskedNormalizedEmail: String,
    val passwordCharCount: Int,
    val passwordCodePointCount: Int,
    val passwordUtf8ByteCount: Int,
    val hasLeadingWhitespace: Boolean,
    val hasTrailingWhitespace: Boolean,
    val containsNewLine: Boolean,
    val currentAndStoredPasswordEqual: Boolean,
    val isConfigured: Boolean
) {
    companion object {
        fun fromSettings(settings: AppSettings): CredentialsSnapshot {
            val originalEmail = settings.email
            val normalizedEmail = originalEmail.trim().lowercase(Locale.ROOT)
            val password = settings.password
            return CredentialsSnapshot(
                email = normalizedEmail,
                normalizedEmailForCheck = normalizedEmail.trim().lowercase(Locale.ROOT),
                password = password,
                region = settings.loginRegionSelection().ifBlank { "EU" },
                emailOriginalLength = originalEmail.length,
                emailNormalized = normalizedEmail == normalizedEmail.trim().lowercase(Locale.ROOT),
                emailWasChangedByNormalization = originalEmail != normalizedEmail,
                maskedNormalizedEmail = maskEmail(normalizedEmail),
                passwordCharCount = password.length,
                passwordCodePointCount = password.codePointCount(0, password.length),
                passwordUtf8ByteCount = password.toByteArray(Charsets.UTF_8).size,
                hasLeadingWhitespace = password.isNotEmpty() && password.first().isWhitespace(),
                hasTrailingWhitespace = password.isNotEmpty() && password.last().isWhitespace(),
                containsNewLine = password.contains('\n') || password.contains('\r'),
                currentAndStoredPasswordEqual = true,
                isConfigured = settings.isConfigured() && !settings.useMock
            )
        }
    }
}

private fun maskEmail(email: String): String {
    if (email.isBlank() || !email.contains("@")) return "***"
    val parts = email.split("@", limit = 2)
    val local = parts[0]
    val domain = parts[1]
    val localMasked = if (local.length <= 1) "*" else "${local.first()}***"
    return "$localMasked@$domain"
}

interface LoginStateStore {
    fun saveNextAllowedLoginAt(epochMillis: Long)
    fun loadNextAllowedLoginAt(): Long
    fun clearNextAllowedLoginAt()
    fun savePersistedSession(session: PersistedLibreLinkUpSession)
    fun loadPersistedSession(): PersistedLibreLinkUpSession?
    fun clearPersistedSession()
    fun saveNormalizedEmail(email: String)
}

class SettingsLoginStateStore(
    private val settingsRepository: SettingsRepository
) : LoginStateStore {
    override fun saveNextAllowedLoginAt(epochMillis: Long) = settingsRepository.saveNextAllowedLoginAt(epochMillis)
    override fun loadNextAllowedLoginAt(): Long = settingsRepository.loadNextAllowedLoginAt()
    override fun clearNextAllowedLoginAt() = settingsRepository.clearNextAllowedLoginAt()
    override fun savePersistedSession(session: PersistedLibreLinkUpSession) = settingsRepository.savePersistedSession(session)
    override fun loadPersistedSession(): PersistedLibreLinkUpSession? = settingsRepository.loadPersistedSession()
    override fun clearPersistedSession() = settingsRepository.clearPersistedSession()
    override fun saveNormalizedEmail(email: String) = settingsRepository.saveNormalizedEmail(email)
}
