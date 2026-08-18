package com.libredisplay

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.libredisplay.ui.monitoring.MonitoringScreen
import com.libredisplay.ui.analytics.AnalyticsScreen
import com.libredisplay.ui.privacy.PrivacyDataScreen
import com.libredisplay.ui.settings.AboutScreen
import com.libredisplay.ui.settings.DiagnosticScreen
import com.libredisplay.ui.settings.SettingsScreen
import com.libredisplay.ui.start.StartScreen
import com.libredisplay.ui.theme.LibreDisplayTheme

enum class AppScreen {
    Start,
    Monitoring,
    Analytics,
    Settings,
    Diagnostics,
    PrivacyData,
    About
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val app = application as LibreDisplayApp
        setContent {
            LibreDisplayTheme {
                var refreshNonce by remember { mutableIntStateOf(0) }
                var showLoginOnly by remember { mutableStateOf(false) }
                fun resolveLaunchScreen(): AppScreen {
                    val target = AppLaunchResolver.resolve(app.settingsRepository.loadSettings(), app.settingsRepository.hasPersistedSessionData())
                    showLoginOnly = target == AppLaunchTarget.LOGIN
                    return when (target) {
                        AppLaunchTarget.START -> AppScreen.Start
                        AppLaunchTarget.LOGIN -> AppScreen.Settings
                        AppLaunchTarget.MONITORING -> AppScreen.Monitoring
                    }
                }

                fun resolveBackFromSettings(): AppScreen {
                    return when (AppLaunchResolver.resolveBackFromSettings(app.settingsRepository.loadSettings(), app.settingsRepository.hasPersistedSessionData())) {
                        AppLaunchTarget.MONITORING -> AppScreen.Monitoring
                        AppLaunchTarget.START,
                        AppLaunchTarget.LOGIN -> AppScreen.Start
                    }
                }

                var currentScreen by remember {
                    mutableStateOf(resolveLaunchScreen())
                }

                when (currentScreen) {
                    AppScreen.Start -> StartScreen(
                        onConnectWithLibreLinkUp = {
                            app.settingsRepository.switchToLiveMode()
                            showLoginOnly = true
                            refreshNonce += 1
                            currentScreen = resolveLaunchScreen()
                        },
                        onTryDemoMode = {
                            app.settingsRepository.switchToDemoMode()
                            showLoginOnly = false
                            refreshNonce += 1
                            currentScreen = AppScreen.Monitoring
                        }
                    )

                    AppScreen.Monitoring -> MonitoringScreen(
                        refreshNonce = refreshNonce,
                        onNavigateToSettings = { currentScreen = AppScreen.Settings },
                        onNavigateToDiagnostics = { currentScreen = AppScreen.Diagnostics },
                        onNavigateToAnalytics = { currentScreen = AppScreen.Analytics },
                        onSwitchToLiveMode = {
                            app.settingsRepository.switchToLiveMode()
                            showLoginOnly = true
                            refreshNonce += 1
                            currentScreen = resolveLaunchScreen()
                        }
                    )

                    AppScreen.Analytics -> AnalyticsScreen(
                        onNavigateBack = { currentScreen = AppScreen.Monitoring }
                    )

                    AppScreen.Settings -> SettingsScreen(
                        loginOnly = showLoginOnly,
                        onNavigateBack = { currentScreen = resolveBackFromSettings() },
                        onSaved = {
                            showLoginOnly = false
                            refreshNonce += 1
                            currentScreen = resolveLaunchScreen()
                        },
                        onNavigateToDiagnostics = { currentScreen = AppScreen.Diagnostics },
                        onNavigateToPrivacyData = { currentScreen = AppScreen.PrivacyData },
                        onNavigateToAbout = { currentScreen = AppScreen.About }
                    )

                    AppScreen.Diagnostics -> DiagnosticScreen(
                        onNavigateBack = { currentScreen = resolveBackFromSettings() }
                    )

                    AppScreen.PrivacyData -> PrivacyDataScreen(
                        onNavigateBack = {
                            currentScreen = if (resolveLaunchScreen() == AppScreen.Start) AppScreen.Start else AppScreen.Settings
                        },
                        onNavigateToStart = {
                            showLoginOnly = false
                            refreshNonce += 1
                            currentScreen = AppScreen.Start
                        },
                        onNavigateToLogin = {
                            showLoginOnly = true
                            refreshNonce += 1
                            currentScreen = AppScreen.Settings
                        }
                    )

                    AppScreen.About -> AboutScreen(
                        onNavigateBack = { currentScreen = AppScreen.Settings }
                    )
                }
            }
        }
    }
}
