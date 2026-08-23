package com.libredisplay.auth

import android.content.Context
import androidx.biometric.BiometricManager
import com.libredisplay.data.storage.SecureStorage

/** How LibreCare verifies the owner before medical data becomes visible. */
enum class UnlockMethod(val storageId: String) {
    NONE("none"),
    BIOMETRIC("biometric"),
    PASSKEY("passkey");

    companion object {
        fun fromStorageId(id: String?): UnlockMethod =
            entries.firstOrNull { it.storageId == id } ?: NONE
    }
}

/**
 * Stores whether LibreCare must be unlocked with a biometric factor, the device PIN / pattern /
 * password before the medical data becomes visible.
 *
 * The actual credential is never stored by LibreCare - verification is delegated to the Android
 * keyguard through [BiometricAuthManager], which also covers fingerprint, face unlock and the
 * device passkey/screen lock.
 */
class AppLockRepository(context: Context) {

    private val appContext = context.applicationContext
    private val storage = SecureStorage(appContext)

    var isEnabled: Boolean
        get() = storage.getBoolean(KEY_APP_LOCK_ENABLED, false)
        set(value) = storage.putBoolean(KEY_APP_LOCK_ENABLED, value)

    /** Currently selected unlock method. */
    var method: UnlockMethod
        get() {
            if (!isEnabled) return UnlockMethod.NONE
            return UnlockMethod.fromStorageId(
                storage.getString(KEY_APP_LOCK_METHOD, UnlockMethod.BIOMETRIC.storageId)
            )
        }
        set(value) {
            storage.putString(KEY_APP_LOCK_METHOD, value.storageId)
            isEnabled = value != UnlockMethod.NONE
        }

    /** Identifier of the registered passkey, empty when no passkey was created. */
    var passkeyId: String
        get() = storage.getString(KEY_APP_LOCK_PASSKEY_ID, "")
        set(value) = storage.putString(KEY_APP_LOCK_PASSKEY_ID, value)

    val hasPasskey: Boolean get() = passkeyId.isNotBlank()

    /** True when the device can verify the user (biometrics OR device credential). */
    fun isDeviceCapable(): Boolean = BiometricManager.from(appContext).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    ) == BiometricManager.BIOMETRIC_SUCCESS

    /** True when a real biometric sensor is enrolled (fingerprint / face). */
    fun hasEnrolledBiometrics(): Boolean = BiometricManager.from(appContext).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    ) == BiometricManager.BIOMETRIC_SUCCESS

    /** Lock can only be armed when the device is actually able to verify the user. */
    fun canEnable(): Boolean = isDeviceCapable()

    /** Called only after a successful fingerprint check. */
    fun enableBiometricUnlock() {
        method = UnlockMethod.BIOMETRIC
    }

    /** Called after the system credential manager created the passkey. */
    fun enablePasskeyUnlock(credentialId: String) {
        passkeyId = credentialId
        method = UnlockMethod.PASSKEY
    }

    fun disable() {
        method = UnlockMethod.NONE
    }

    fun forgetPasskey() {
        passkeyId = ""
        if (method == UnlockMethod.PASSKEY) method = UnlockMethod.NONE
    }

    fun describeStatus(): String = when {
        !isDeviceCapable() ->
            "Brak skonfigurowanego zabezpieczenia ekranu. Ustaw PIN, wzór lub odcisk palca w ustawieniach telefonu."
        method == UnlockMethod.PASSKEY -> "Aplikacja jest zabezpieczona kluczem dostępu (passkey)."
        method == UnlockMethod.BIOMETRIC ->
            "Aplikacja jest zabezpieczona odciskiem palca, PIN-em lub blokadą ekranu."
        else -> "Aplikacja nie jest dodatkowo zabezpieczona."
    }

    private companion object {
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val KEY_APP_LOCK_METHOD = "app_lock_method"
        const val KEY_APP_LOCK_PASSKEY_ID = "app_lock_passkey_id"
    }
}
