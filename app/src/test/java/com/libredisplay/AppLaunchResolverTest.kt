package com.libredisplay

import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLaunchResolverTest {

    @Test
    fun demoMode_alwaysOpensMonitoring() {
        val target = AppLaunchResolver.resolve(
            settings = AppSettings(appMode = AppMode.DEMO),
            hasPersistedSession = false
        )

        assertEquals(AppLaunchTarget.MONITORING, target)
    }

    @Test
    fun liveMode_withoutSessionOpensLoginEvenWhenCredentialsExist() {
        val target = AppLaunchResolver.resolve(
            settings = AppSettings(
                appMode = AppMode.LIVE,
                email = "user@example.com",
                password = "secret"
            ),
            hasPersistedSession = false
        )

        assertEquals(AppLaunchTarget.LOGIN, target)
    }

    @Test
    fun liveMode_withSessionOpensMonitoring() {
        val target = AppLaunchResolver.resolve(
            settings = AppSettings(appMode = AppMode.LIVE),
            hasPersistedSession = true
        )

        assertEquals(AppLaunchTarget.MONITORING, target)
    }

    @Test
    fun noneMode_opensStart() {
        val target = AppLaunchResolver.resolve(
            settings = AppSettings(appMode = AppMode.NONE),
            hasPersistedSession = false
        )

        assertEquals(AppLaunchTarget.START, target)
    }
}

