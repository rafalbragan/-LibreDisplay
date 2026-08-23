package com.libredisplay.auth

import android.content.Context
import androidx.biometric.BiometricManager
import com.libredisplay.data.storage.SecureStorage

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

    /** True when the device can verify the user (biometrics OR device credential). */
    fun isDeviceCapable(): Boolean = BiometricManager.from(appContext).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    ) == BiometricManager.BIOMETRIC_SUCCESS

    /** Lock can only be armed when the device is actually able to verify the user. */
    fun canEnable(): Boolean = isDeviceCapable()

    fun describeStatus(): String = when {
        !isDeviceCapable() -> "Brak skonfigurowanego zabezpieczenia ekranu. Ustaw PIN, wzór lub odcisk palca w ustawieniach telefonu."
        isEnabled -> "Aplikacja jest zabezpieczona odciskiem palca, PIN-em lub blokadą ekranu."
        else -> "Aplikacja nie jest dodatkowo zabezpieczona."
    }

    private companion object {
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    }
}

