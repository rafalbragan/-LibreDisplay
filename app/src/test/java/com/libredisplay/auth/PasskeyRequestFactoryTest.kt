package com.libredisplay.auth

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.SecureRandom

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class PasskeyRequestFactoryTest {

    @Test
    fun createRequestContainsRelyingPartyAndUser() {
        val json = JSONObject(
            PasskeyRequestFactory.createRequestJson(
                userId = "anna@example.com",
                userName = "anna@example.com"
            )
        )

        assertEquals(
            PasskeyRequestFactory.RELYING_PARTY_ID,
            json.getJSONObject("rp").getString("id")
        )
        assertEquals(
            PasskeyRequestFactory.RELYING_PARTY_NAME,
            json.getJSONObject("rp").getString("name")
        )
        assertEquals("anna@example.com", json.getJSONObject("user").getString("name"))
        assertTrue(json.getString("challenge").isNotBlank())
    }

    @Test
    fun createRequestRequiresPlatformAuthenticatorAndUserVerification() {
        val json = JSONObject(PasskeyRequestFactory.createRequestJson("id", "name"))
        val selection = json.getJSONObject("authenticatorSelection")

        assertEquals("platform", selection.getString("authenticatorAttachment"))
        assertEquals("required", selection.getString("userVerification"))
        assertEquals("required", selection.getString("residentKey"))
        assertEquals(2, json.getJSONArray("pubKeyCredParams").length())
    }

    @Test
    fun getRequestRestrictsToTheRegisteredCredential() {
        val json = JSONObject(PasskeyRequestFactory.getRequestJson("credential-123"))

        assertEquals(PasskeyRequestFactory.RELYING_PARTY_ID, json.getString("rpId"))
        val allowed = json.getJSONArray("allowCredentials")
        assertEquals(1, allowed.length())
        assertEquals("credential-123", allowed.getJSONObject(0).getString("id"))
    }

    @Test
    fun getRequestOmitsAllowListWhenNoCredentialIsKnown() {
        val json = JSONObject(PasskeyRequestFactory.getRequestJson(""))

        assertTrue(json.isNull("allowCredentials") || !json.has("allowCredentials"))
    }

    @Test
    fun challengesAreRandomAndUrlSafe() {
        val first = PasskeyRequestFactory.randomChallenge(SecureRandom())
        val second = PasskeyRequestFactory.randomChallenge(SecureRandom())

        assertTrue(first != second)
        assertTrue(first, !first.contains("+"))
        assertTrue(first, !first.contains("/"))
        assertTrue(first, !first.contains("="))
    }

    @Test
    fun credentialIdIsReadFromResponse() {
        val response = """{"rawId":"abc123","id":"abc123","type":"public-key"}"""

        assertEquals("abc123", PasskeyRequestFactory.credentialIdFrom(response))
    }

    @Test
    fun credentialIdIsNullForBrokenResponse() {
        assertNull(PasskeyRequestFactory.credentialIdFrom("not json"))
    }
}

