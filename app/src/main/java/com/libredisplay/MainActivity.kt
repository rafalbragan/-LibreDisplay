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
import com.libredisplay.data.model.AppSettings
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
                var currentScreen by remember {
                    mutableStateOf(
                        if (app.settingsRepository.isConfigured()) AppScreen.Monitoring else AppScreen.Start
                    )
                }

                when (currentScreen) {
                    AppScreen.Start -> StartScreen(
                        onConnectWithLibreLinkUp = { currentScreen = AppScreen.Settings },
                        onTryDemoMode = {
                            val current = app.settingsRepository.loadSettings()
                            app.settingsRepository.saveSettings(
                                AppSettings(
                                    email = "",
                                    password = "",
                                    selectedPatientId = "demo-person-anna",
                                    region = current.region,
                                    regionMode = current.regionMode,
                                    customBaseUrl = current.customBaseUrl,
                                    refreshInterval = current.refreshInterval,
                                    targetLow = current.targetLow,
                                    targetHigh = current.targetHigh,
                                    trendWindowMinutes = current.trendWindowMinutes,
                                    showStatistics = current.showStatistics,
                                    kioskMode = current.kioskMode,
                                    useMock = true,
                                    useAuthV3 = current.useAuthV3
                                )
                            )
                            app.authRepository.clearSession()
                            refreshNonce += 1
                            currentScreen = AppScreen.Monitoring
                        }
                    )

                    AppScreen.Monitoring -> MonitoringScreen(
                        refreshNonce = refreshNonce,
                        onNavigateToSettings = { currentScreen = AppScreen.Settings },
                        onNavigateToDiagnostics = { currentScreen = AppScreen.Diagnostics },
                        onNavigateToAnalytics = { currentScreen = AppScreen.Analytics }
                    )

                    AppScreen.Analytics -> AnalyticsScreen(
                        onNavigateBack = { currentScreen = AppScreen.Monitoring }
                    )

                    AppScreen.Settings -> SettingsScreen(
                        onNavigateBack = {
                            currentScreen = if (app.settingsRepository.isConfigured()) AppScreen.Monitoring else AppScreen.Start
                        },
                        onSaved = {
                            refreshNonce += 1
                            currentScreen = AppScreen.Monitoring
                        },
                        onNavigateToDiagnostics = { currentScreen = AppScreen.Diagnostics },
                        onNavigateToPrivacyData = { currentScreen = AppScreen.PrivacyData },
                        onNavigateToAbout = { currentScreen = AppScreen.About }
                    )

                    AppScreen.Diagnostics -> DiagnosticScreen(
                        onNavigateBack = {
                            currentScreen = if (app.settingsRepository.isConfigured()) AppScreen.Monitoring else AppScreen.Start
                        }
                    )

                    AppScreen.PrivacyData -> PrivacyDataScreen(
                        onNavigateBack = {
                            currentScreen = if (app.settingsRepository.isConfigured()) AppScreen.Settings else AppScreen.Start
                        },
                        onNavigateToStart = {
                            refreshNonce += 1
                            currentScreen = AppScreen.Start
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
