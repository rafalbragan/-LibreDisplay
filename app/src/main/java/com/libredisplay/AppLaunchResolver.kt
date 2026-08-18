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
            AppMode.LIVE -> if (hasPersistedSession) {
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

