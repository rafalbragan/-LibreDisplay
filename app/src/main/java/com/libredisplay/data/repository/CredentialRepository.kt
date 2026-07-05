package com.libredisplay.data.repository

import android.content.Context
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import com.libredisplay.data.api.LibreLinkUpException
import com.libredisplay.data.storage.SecureStorage

class CredentialRepository(private val context: Context) {

    private val storage = SecureStorage(context)
    private val credentialManager = CredentialManager.create(context)

    suspend fun saveCredentials(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) return

        runCatching {
            credentialManager.createCredential(
                context,
                CreatePasswordRequest(id = email, password = password)
            )
        }.onFailure {
            // Fallback to encrypted storage only.
            storage.putString(SecureStorage.KEY_EMAIL, email)
            storage.putString(SecureStorage.KEY_PASSWORD, password)
            throw LibreLinkUpException("Nie udalo sie zapisac danych w managerze hasel. Uzyto bezpiecznego zapisu lokalnego.")
        }
    }

    suspend fun getCredentialsOrNull(): Pair<String, String>? {
        val result = runCatching {
            credentialManager.getCredential(
                context,
                GetCredentialRequest(listOf(GetPasswordOption()))
            )
        }

        val credential = result.getOrNull()?.credential
        if (credential is PasswordCredential) {
            return credential.id to credential.password
        }

        val fallbackEmail = storage.getString(SecureStorage.KEY_EMAIL)
        val fallbackPassword = storage.getString(SecureStorage.KEY_PASSWORD)
        return if (fallbackEmail.isNotBlank() && fallbackPassword.isNotBlank()) {
            fallbackEmail to fallbackPassword
        } else {
            null
        }
    }

    fun clearLocalFallback() {
        storage.putString(SecureStorage.KEY_EMAIL, "")
        storage.putString(SecureStorage.KEY_PASSWORD, "")
    }
}

