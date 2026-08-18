package com.libredisplay.data.repository

import com.libredisplay.data.api.AuthCapableLibreLinkUpClient
import com.libredisplay.data.api.LibreResponseDecodingException
import com.libredisplay.data.api.NonRetryableLibreLinkUpException
import com.libredisplay.data.api.PersistedLibreLinkUpSession
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.AppMode
import com.google.gson.stream.MalformedJsonException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AuthRepositoryTest {

    @Test
    fun ensureAuthenticated_logsInOnlyOnceWhenSessionIsActive() = runTest {
        val fakeClient = FakeAuthClient()
        val repository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = fakeClient
        )

        repository.ensureAuthenticated()
        repository.ensureAuthenticated()

        assertEquals(1, fakeClient.loginCalls)
        assertTrue(fakeClient.active)
    }

    @Test
    fun ensureAuthenticated_trimsEmail_andTrimsPassword() = runTest {
        val fakeClient = FakeAuthClient()
        val repository = AuthRepository(
            settingsProvider = {
                AppSettings(
                    appMode = AppMode.LIVE,
                    email = " user@example.com ",
                    password = " secret ",
                    regionMode = "AUTO"
                )
            },
            client = fakeClient
        )

        repository.ensureAuthenticated(force = true)

        assertEquals("user@example.com", fakeClient.lastEmail)
        assertEquals("secret", fakeClient.lastPassword)
        assertEquals("EU", fakeClient.lastRegion)
    }

    @Test
    fun ensureAuthenticated_normalizesEmailDomainToLowercase() = runTest {
        val fakeClient = FakeAuthClient()
        val fakeStore = FakeSettingsRepository()
        val repository = AuthRepository(
            settingsProvider = {
                AppSettings(
                    appMode = AppMode.LIVE,
                    email = "  RaFal@Gmail.com  ",
                    password = "P@ss Word",
                    regionMode = "EU"
                )
            },
            client = fakeClient,
            loginStateStore = fakeStore
        )

        repository.ensureAuthenticated(force = true)

        assertEquals("rafal@gmail.com", fakeClient.lastEmail)
        assertEquals("P@ss Word", fakeClient.lastPassword)
        assertEquals("rafal@gmail.com", fakeStore.savedNormalizedEmail)
    }

    @Test
    fun nonRetryableFailure_isCachedUntilSessionReset() = runTest {
        val fakeClient = FakeAuthClient().apply { failWithNonRetryable = true }
        val repository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = fakeClient
        )

        try {
            repository.ensureAuthenticated()
            fail("Expected NonRetryableLibreLinkUpException")
        } catch (_: NonRetryableLibreLinkUpException) {
        }

        try {
            repository.ensureAuthenticated()
            fail("Expected cached NonRetryableLibreLinkUpException")
        } catch (_: NonRetryableLibreLinkUpException) {
        }

        assertEquals(1, fakeClient.loginCalls)

        fakeClient.failWithNonRetryable = false
        repository.clearSession()
        repository.ensureAuthenticated()
        assertEquals(2, fakeClient.loginCalls)
    }

    @Test
    fun clearSession_resetsClientSession() {
        val fakeClient = FakeAuthClient().apply { active = true }
        val repository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = fakeClient
        )

        repository.clearSession()

        assertFalse(fakeClient.active)
    }

    @Test
    fun parallelEnsureAuthenticated_performsOnlyOneLogin() = runTest {
        val fakeClient = FakeAuthClient()
        val repository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = fakeClient
        )

        val job1 = async { repository.ensureAuthenticated() }
        val job2 = async { repository.ensureAuthenticated() }
        val job3 = async { repository.ensureAuthenticated() }

        job1.await()
        job2.await()
        job3.await()

        assertEquals(1, fakeClient.loginCalls)
    }

    @Test
    fun nonRetryableFailure_setsCooldownAndBlocksImmediateRetry() = runTest {
        var now = 10_000L
        val fakeClient = FakeAuthClient().apply { failWithNonRetryable = true }
        val fakeSettings = FakeSettingsRepository()
        val repository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = fakeClient,
            loginStateStore = fakeSettings,
            currentTimeMillis = { now }
        )

        try {
            repository.ensureAuthenticated()
            fail("Expected NonRetryableLibreLinkUpException")
        } catch (_: NonRetryableLibreLinkUpException) {
        }

        assertTrue(fakeSettings.nextAllowedLoginAt >= now + 300_000L)

        now += 1_000L
        try {
            repository.ensureAuthenticated()
            fail("Expected cooldown blocking exception")
        } catch (_: NonRetryableLibreLinkUpException) {
        }
        assertEquals(1, fakeClient.loginCalls)
    }

    @Test
    fun cooldownRemainingSeconds_decreasesBasedOnCurrentTimestamp() {
        var now = 100_000L
        val fakeSettings = FakeSettingsRepository().apply {
            saveNextAllowedLoginAt(now + 90_000L)
        }
        val repository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = FakeAuthClient(),
            loginStateStore = fakeSettings,
            currentTimeMillis = { now }
        )

        assertEquals(90L, repository.cooldownRemainingSeconds(now))

        now += 31_000L
        assertEquals(59L, repository.cooldownRemainingSeconds(now))

        now += 59_000L
        assertEquals(0L, repository.cooldownRemainingSeconds(now))
    }

    @Test
    fun cooldownPersistsAcrossRepositoryRecreation() {
        var now = 200_000L
        val fakeSettings = FakeSettingsRepository().apply {
            saveNextAllowedLoginAt(now + 45_000L)
        }

        val firstRepository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = FakeAuthClient(),
            loginStateStore = fakeSettings,
            currentTimeMillis = { now }
        )
        assertEquals(45L, firstRepository.cooldownRemainingSeconds(now))

        now += 20_000L
        val recreatedRepository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = FakeAuthClient(),
            loginStateStore = fakeSettings,
            currentTimeMillis = { now }
        )

        assertEquals(25L, recreatedRepository.cooldownRemainingSeconds(now))

        now += 26_000L
        assertEquals(0L, recreatedRepository.cooldownRemainingSeconds(now))
    }

    @Test
    fun decodingFailure_doesNotSetCooldown_andAllowsImmediateRetry() = runTest {
        var now = 10_000L
        val fakeClient = FakeAuthClient().apply { failWithDecoding = true }
        val fakeSettings = FakeSettingsRepository()
        val repository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = fakeClient,
            loginStateStore = fakeSettings,
            currentTimeMillis = { now }
        )

        repeat(2) {
            try {
                repository.ensureAuthenticated(force = true)
                fail("Expected LibreResponseDecodingException")
            } catch (_: LibreResponseDecodingException) {
            }
            now += 1_000L
        }

        assertEquals(2, fakeClient.loginCalls)
        assertEquals(0L, fakeSettings.nextAllowedLoginAt)
        assertEquals(0L, repository.cooldownRemainingSeconds(now))
    }

    @Test
    fun malformedJsonFailure_doesNotSetCooldown() = runTest {
        val fakeClient = FakeAuthClient().apply { failWithMalformedJson = true }
        val fakeSettings = FakeSettingsRepository()
        val repository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = fakeClient,
            loginStateStore = fakeSettings
        )

        try {
            repository.ensureAuthenticated(force = true)
            fail("Expected runtime decode failure")
        } catch (_: RuntimeException) {
        }

        assertEquals(1, fakeClient.loginCalls)
        assertEquals(0L, fakeSettings.nextAllowedLoginAt)
    }

    @Test
    fun ensureSessionFromStorageOnly_withoutPersistedToken_doesNotLogin() = runTest {
        val fakeClient = FakeAuthClient()
        val fakeStore = FakeSettingsRepository()
        val repository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = fakeClient,
            loginStateStore = fakeStore
        )

        val hasSession = repository.ensureSessionFromStorageOnly()

        assertFalse(hasSession)
        assertEquals(0, fakeClient.loginCalls)
    }

    @Test
    fun ensureSessionFromStorageOnly_withPersistedToken_importsWithoutLogin() = runTest {
        val fakeClient = FakeAuthClient()
        val fakeStore = FakeSettingsRepository().apply {
            savePersistedSession(
                PersistedLibreLinkUpSession(
                    token = "t",
                    userId = "u",
                    accountIdHash = "a",
                    region = "EU",
                    baseUrl = "https://api-eu.libreview.io/",
                    tokenExpiresAtEpochSeconds = null
                )
            )
        }
        val repository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = fakeClient,
            loginStateStore = fakeStore
        )

        val hasSession = repository.ensureSessionFromStorageOnly()

        assertTrue(hasSession)
        assertEquals(0, fakeClient.loginCalls)
    }

    @Test
    fun connectOnce_withInconsistentEmailSnapshot_abortsBeforeNetwork() = runTest {
        val fakeClient = FakeAuthClient()
        val repository = AuthRepository(
            settingsProvider = { AppSettings(appMode = AppMode.LIVE, email = "user@example.com", password = "secret") },
            client = fakeClient
        )
        val badSnapshot = CredentialsSnapshot(
            email = "mixed@Gmail.com",
            normalizedEmailForCheck = "mixed@gmail.com",
            password = "secret",
            region = "EU",
            emailOriginalLength = 15,
            emailNormalized = false,
            emailWasChangedByNormalization = true,
            maskedNormalizedEmail = "m***@gmail.com",
            passwordOriginalLength = 6,
            passwordNormalizedLength = 6,
            passwordCharCount = 6,
            passwordCodePointCount = 6,
            passwordUtf8ByteCount = 6,
            hasLeadingWhitespace = false,
            hasTrailingWhitespace = false,
            containsNewLine = false,
            currentAndStoredPasswordEqual = true,
            isConfigured = true
        )

        try {
            repository.connectOnce(snapshot = badSnapshot, force = true)
            fail("Expected NonRetryableLibreLinkUpException")
        } catch (_: NonRetryableLibreLinkUpException) {
        }

        assertEquals(0, fakeClient.loginCalls)
    }

    @Test
    fun credentialsSnapshot_normalizesPasswordByTrimming() {
        val settings = AppSettings(
            appMode = AppMode.LIVE,
            email = " User@Example.com ",
            password = " myPassword123 ",
            regionMode = "EU"
        )
        val snapshot = CredentialsSnapshot.fromSettings(settings)

        assertEquals("user@example.com", snapshot.email)
        assertEquals("myPassword123", snapshot.password)
        assertEquals(15, snapshot.passwordOriginalLength)
        assertEquals(13, snapshot.passwordNormalizedLength)
        assertTrue(snapshot.hasLeadingWhitespace)
        assertTrue(snapshot.hasTrailingWhitespace)
        assertFalse(snapshot.containsNewLine)
    }

    @Test
    fun credentialsSnapshot_preservesInternalSpacesInPassword() {
        val settings = AppSettings(
            appMode = AppMode.LIVE,
            email = "user@example.com",
            password = "my Password 123",
            regionMode = "EU"
        )
        val snapshot = CredentialsSnapshot.fromSettings(settings)

        assertEquals("my Password 123", snapshot.password)
        assertEquals(15, snapshot.passwordOriginalLength)
        assertEquals(15, snapshot.passwordNormalizedLength)
        assertFalse(snapshot.hasLeadingWhitespace)
        assertFalse(snapshot.hasTrailingWhitespace)
        assertFalse(snapshot.containsNewLine)
    }

    @Test
    fun credentialsSnapshot_detecetsLeadingWhitespace() {
        val settings = AppSettings(
            appMode = AppMode.LIVE,
            email = "user@example.com",
            password = "  secret",
            regionMode = "EU"
        )
        val snapshot = CredentialsSnapshot.fromSettings(settings)

        assertEquals("secret", snapshot.password)
        assertTrue(snapshot.hasLeadingWhitespace)
        assertFalse(snapshot.hasTrailingWhitespace)
    }

    @Test
    fun credentialsSnapshot_detectsNewLineInPassword() {
        val settings = AppSettings(
            appMode = AppMode.LIVE,
            email = "user@example.com",
            password = "secret\npassword",
            regionMode = "EU"
        )
        val snapshot = CredentialsSnapshot.fromSettings(settings)

        assertTrue(snapshot.containsNewLine)
        assertEquals("secret\npassword", snapshot.password)
    }

    private class FakeAuthClient : AuthCapableLibreLinkUpClient {
        var loginCalls = 0
        var active = false
        var lastEmail: String = ""
        var lastPassword: String = ""
        var lastRegion: String = ""
        var failWithNonRetryable = false
        var failWithDecoding = false
        var failWithMalformedJson = false

        override suspend fun login(email: String, password: String, region: String) {
            loginCalls += 1
            lastEmail = email
            lastPassword = password
            lastRegion = region
            if (failWithDecoding) {
                throw LibreResponseDecodingException(
                    message = "Odpowiedz LibreLinkUp jest skompresowana GZIP, ale nie zostala rozpakowana przez klienta HTTP.",
                    encoding = "gzip",
                    contentType = "application/json"
                )
            }
            if (failWithMalformedJson) {
                throw RuntimeException("decode failed", MalformedJsonException("bad json"))
            }
            if (failWithNonRetryable) {
                throw NonRetryableLibreLinkUpException("incorrect username/password")
            }
            active = true
        }

        override fun hasActiveSession(): Boolean = active

        override fun clearSession() {
            active = false
        }

        override fun exportSession(): PersistedLibreLinkUpSession? {
            if (!active) return null
            return PersistedLibreLinkUpSession(
                token = "token",
                userId = "user-1",
                accountIdHash = "hash-1",
                region = "EU",
                baseUrl = "https://api-eu.libreview.io/",
                tokenExpiresAtEpochSeconds = null
            )
        }

        override fun importSession(session: PersistedLibreLinkUpSession) {
            active = true
        }
    }

    private class FakeSettingsRepository : LoginStateStore {
        var nextAllowedLoginAt: Long = 0L
        private var persisted: PersistedLibreLinkUpSession? = null
        var savedNormalizedEmail: String = ""

        override fun saveNextAllowedLoginAt(epochMillis: Long) {
            nextAllowedLoginAt = epochMillis
        }

        override fun loadNextAllowedLoginAt(): Long = nextAllowedLoginAt

        override fun clearNextAllowedLoginAt() {
            nextAllowedLoginAt = 0L
        }

        override fun savePersistedSession(session: PersistedLibreLinkUpSession) {
            persisted = session
        }

        override fun loadPersistedSession(): PersistedLibreLinkUpSession? = persisted

        override fun clearPersistedSession() {
            persisted = null
        }

        override fun saveNormalizedEmail(email: String) {
            savedNormalizedEmail = email
        }
    }
}
