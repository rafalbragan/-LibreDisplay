package com.libredisplay.auth

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.libredisplay.diagnostics.DiagnosticLogger
import org.json.JSONObject
import java.security.SecureRandom
import android.util.Base64

/** Outcome of a passkey (FIDO2 / WebAuthn) operation. */
sealed class PasskeyResult {
    data class Created(val credentialId: String) : PasskeyResult()
    data object Verified : PasskeyResult()
    data object Cancelled : PasskeyResult()
    data class Unsupported(val message: String) : PasskeyResult()
    data class Error(val message: String) : PasskeyResult()
}

/**
 * Builds the WebAuthn payloads LibreCare sends to the system credential manager.
 *
 * Kept separate from Android APIs so the JSON contract can be unit tested.
 */
object PasskeyRequestFactory {

    const val RELYING_PARTY_ID = "librecare.app"
    const val RELYING_PARTY_NAME = "LibreCare"
    const val CHALLENGE_BYTES = 32

    fun randomChallenge(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(CHALLENGE_BYTES)
        random.nextBytes(bytes)
        return encode(bytes)
    }

    fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    fun createRequestJson(
        userId: String,
        userName: String,
        displayName: String = userName,
        challenge: String = randomChallenge()
    ): String = JSONObject().apply {
        put("challenge", challenge)
        put("rp", JSONObject().apply {
            put("id", RELYING_PARTY_ID)
            put("name", RELYING_PARTY_NAME)
        })
        put("user", JSONObject().apply {
            put("id", encode(userId.toByteArray(Charsets.UTF_8)))
            put("name", userName)
            put("displayName", displayName)
        })
        put("pubKeyCredParams", org.json.JSONArray().apply {
            put(JSONObject().apply { put("type", "public-key"); put("alg", -7) })
            put(JSONObject().apply { put("type", "public-key"); put("alg", -257) })
        })
        put("timeout", 120_000)
        put("attestation", "none")
        put("authenticatorSelection", JSONObject().apply {
            put("authenticatorAttachment", "platform")
            put("residentKey", "required")
            put("userVerification", "required")
        })
    }.toString()

    fun getRequestJson(
        credentialId: String,
        challenge: String = randomChallenge()
    ): String = JSONObject().apply {
        put("challenge", challenge)
        put("rpId", RELYING_PARTY_ID)
        put("timeout", 120_000)
        put("userVerification", "required")
        if (credentialId.isNotBlank()) {
            put("allowCredentials", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "public-key")
                    put("id", credentialId)
                })
            })
        }
    }.toString()

    /** Reads the credential id out of the response returned by the credential manager. */
    fun credentialIdFrom(responseJson: String): String? = runCatching {
        JSONObject(responseJson).optString("rawId").takeIf { it.isNotBlank() }
            ?: JSONObject(responseJson).optString("id").takeIf { it.isNotBlank() }
    }.getOrNull()
}

/**
 * Thin wrapper around [CredentialManager] used to register and verify a LibreCare passkey.
 *
 * When the platform cannot create a passkey (no Google/Samsung passkey provider, or no verified
 * digital asset link), the user is told in plain Polish and can keep using the biometric lock.
 */
class PasskeyManager(private val context: Context) {

    private val credentialManager by lazy { CredentialManager.create(context) }

    suspend fun createPasskey(userId: String, userName: String): PasskeyResult = try {
        val request = CreatePublicKeyCredentialRequest(
            requestJson = PasskeyRequestFactory.createRequestJson(userId = userId, userName = userName)
        )
        val response = credentialManager.createCredential(context, request)
        val json = response.data.getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON")
        val credentialId = json?.let(PasskeyRequestFactory::credentialIdFrom)
        if (credentialId.isNullOrBlank()) {
            PasskeyResult.Error("Nie udało się odczytać identyfikatora klucza dostępu.")
        } else {
            PasskeyResult.Created(credentialId)
        }
    } catch (cancelled: CreateCredentialCancellationException) {
        PasskeyResult.Cancelled
    } catch (failure: CreateCredentialException) {
        DiagnosticLogger.logWarning("PasskeyManager", "CREATE PASSKEY failed reason=${failure.message}")
        PasskeyResult.Unsupported(
            "To urządzenie nie może teraz utworzyć klucza dostępu. Użyj odcisku palca lub PIN-u."
        )
    } catch (throwable: Throwable) {
        DiagnosticLogger.logWarning("PasskeyManager", "CREATE PASSKEY error reason=${throwable.message}")
        PasskeyResult.Error(throwable.message ?: "Nie udało się utworzyć klucza dostępu.")
    }

    suspend fun verifyPasskey(credentialId: String): PasskeyResult = try {
        val option = GetPublicKeyCredentialOption(
            requestJson = PasskeyRequestFactory.getRequestJson(credentialId)
        )
        val response = credentialManager.getCredential(
            context = context,
            request = GetCredentialRequest(listOf(option))
        )
        if (response.credential is PublicKeyCredential) {
            PasskeyResult.Verified
        } else {
            PasskeyResult.Error("Otrzymano nieoczekiwany typ poświadczenia.")
        }
    } catch (cancelled: GetCredentialCancellationException) {
        PasskeyResult.Cancelled
    } catch (failure: GetCredentialException) {
        DiagnosticLogger.logWarning("PasskeyManager", "GET PASSKEY failed reason=${failure.message}")
        PasskeyResult.Unsupported(
            "Nie udało się użyć klucza dostępu. Skorzystaj z odcisku palca lub PIN-u."
        )
    } catch (throwable: Throwable) {
        DiagnosticLogger.logWarning("PasskeyManager", "GET PASSKEY error reason=${throwable.message}")
        PasskeyResult.Error(throwable.message ?: "Nie udało się użyć klucza dostępu.")
    }
}

