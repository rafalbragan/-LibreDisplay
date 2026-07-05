package com.libredisplay.data.repository

import com.libredisplay.data.api.LibreLinkApiClient
import com.libredisplay.data.api.LibreLinkUpException
import com.libredisplay.data.api.LibreLinkUpHttpException
import com.libredisplay.data.api.LoginResult
import com.libredisplay.data.model.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {

    @Test
    fun successfulLogin_storesTokenAndRegion() = runBlocking {
        val fakeApi = FakeApiClient(loginResult = LoginResult("token-123", "EU2"))
        val repo = AuthRepository(
            settingsProvider = { AppSettings(email = "a@b.pl", password = "x", region = "EU") },
            apiClient = fakeApi
        )

        repo.ensureLoggedIn()

        assertEquals("token-123", repo.currentToken())
        assertEquals("EU2", repo.currentRegion())
        assertEquals(1, fakeApi.loginCalls)
    }

    @Test
    fun login401_returnsPolishMessage() = runBlocking {
        val fakeApi = FakeApiClient(loginException = LibreLinkUpHttpException(401))
        val repo = AuthRepository(
            settingsProvider = { AppSettings(email = "bad@b.pl", password = "bad", region = "EU") },
            apiClient = fakeApi
        )

        val message = runCatching { repo.ensureLoggedIn() }.exceptionOrNull()?.message.orEmpty()
        assertTrue(message.contains("Nieprawidlowe dane logowania"))
    }

    @Test
    fun login430_setsCooldownAndReturnsPolishMessage() = runBlocking {
        val fakeApi = FakeApiClient(loginException = LibreLinkUpHttpException(430))
        val repo = AuthRepository(
            settingsProvider = { AppSettings(email = "a@b.pl", password = "x", region = "EU") },
            apiClient = fakeApi
        )

        val message = runCatching { repo.ensureLoggedIn() }.exceptionOrNull()?.message.orEmpty()
        assertTrue(message.contains("Serwer odrzucil kolejne proby logowania"))
        assertTrue(repo.cooldownRemainingMs() > 0L)
    }

    @Test
    fun ensureLoggedIn_reusesTokenWithoutSecondLogin() = runBlocking {
        val fakeApi = FakeApiClient(loginResult = LoginResult("token-1", "EU"))
        val repo = AuthRepository(
            settingsProvider = { AppSettings(email = "a@b.pl", password = "x", region = "EU") },
            apiClient = fakeApi
        )

        repo.ensureLoggedIn()
        repo.ensureLoggedIn()

        assertEquals(1, fakeApi.loginCalls)
    }

    private class FakeApiClient(
        private val loginResult: LoginResult? = null,
        private val loginException: Exception? = null
    ) : LibreLinkApiClient() {
        var loginCalls: Int = 0

        override suspend fun login(region: String, email: String, password: String): LoginResult {
            loginCalls++
            loginException?.let { throw it }
            return loginResult ?: throw LibreLinkUpException("Brak wyniku")
        }
    }
}

