package com.libredisplay

import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings

enum class AppLaunchTarget {
    START,
    LOGIN,
    MONITORING
}

object AppLaunchResolver {
    fun resolve(settings: AppSettings, hasPersistedSession: Boolean): AppLaunchTarget {
        return when (settings.appMode) {
            AppMode.DEMO -> AppLaunchTarget.MONITORING
            // Only force the login screen when there is genuinely nothing to log in with: no saved
            // session AND no stored credentials. After restoring a backup that contains credentials
            // the app must go straight to monitoring (it can reconnect on its own) instead of asking
            // the user to sign in again. This mirrors SettingsRepository.shouldShowLoginForm().
            AppMode.LIVE -> if (hasPersistedSession || settings.hasCredentials()) {
                AppLaunchTarget.MONITORING
            } else {
                AppLaunchTarget.LOGIN
            }
            AppMode.NONE -> AppLaunchTarget.START
        }
    }

    fun resolveBackFromSettings(settings: AppSettings, hasPersistedSession: Boolean): AppLaunchTarget {
        return when (resolve(settings, hasPersistedSession)) {
            AppLaunchTarget.MONITORING -> AppLaunchTarget.MONITORING
            AppLaunchTarget.LOGIN,
            AppLaunchTarget.START -> AppLaunchTarget.START
        }
    }
}

