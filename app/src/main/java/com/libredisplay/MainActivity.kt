package com.libredisplay

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.libredisplay.ui.settings.PollingFrequencyScreen
import com.libredisplay.ui.settings.RetentionSettingsScreen
import com.libredisplay.ui.settings.SettingsFocusSection
import com.libredisplay.ui.settings.SettingsScreen
import com.libredisplay.ui.settings.StatisticsScreen
import com.libredisplay.ui.start.StartScreen
import com.libredisplay.ui.theme.LibreDisplayTheme

enum class AppScreen {
    Start,
    Monitoring,
    Analytics,
    Settings,
    Diagnostics,
    PrivacyData,
    About,
    Statistics,
    Retention,
    Polling
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
                var settingsFocusSection by remember { mutableStateOf(SettingsFocusSection.GENERAL) }
                var showExitConfirmation by remember { mutableStateOf(false) }

                if (showExitConfirmation) {
                    AlertDialog(
                        onDismissRequest = { showExitConfirmation = false },
                        title = { Text("Czy chcesz zamknąć LibreCare?") },
                        text = { Text("Możesz wrócić do aplikacji wybierając Anuluj.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showExitConfirmation = false
                                finish()
                            }) {
                                Text("Zamknij")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExitConfirmation = false }) {
                                Text("Anuluj")
                            }
                        }
                    )
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

                    AppScreen.Monitoring -> {
                        BackHandler { showExitConfirmation = true }
                        MonitoringScreen(
                            refreshNonce = refreshNonce,
                            onNavigateToSettings = {
                                settingsFocusSection = SettingsFocusSection.GENERAL
                                currentScreen = AppScreen.Settings
                            },
                            onNavigateToMetricSettings = {
                                settingsFocusSection = SettingsFocusSection.HBA1C
                                currentScreen = AppScreen.Settings
                            },
                            onNavigateToDiagnostics = { currentScreen = AppScreen.Diagnostics },
                            onNavigateToAnalytics = { currentScreen = AppScreen.Analytics },
                            onSwitchToLiveMode = {
                                app.settingsRepository.switchToLiveMode()
                                showLoginOnly = true
                                refreshNonce += 1
                                currentScreen = resolveLaunchScreen()
                            }
                        )
                    }

                    AppScreen.Analytics -> {
                        BackHandler { currentScreen = AppScreen.Monitoring }
                        AnalyticsScreen(onNavigateBack = { currentScreen = AppScreen.Monitoring })
                    }

                    AppScreen.Settings -> {
                        BackHandler { currentScreen = resolveBackFromSettings() }
                        SettingsScreen(
                            loginOnly = showLoginOnly,
                            focusSection = settingsFocusSection,
                            onNavigateBack = { currentScreen = resolveBackFromSettings() },
                            onSaved = {
                                showLoginOnly = false
                                refreshNonce += 1
                                currentScreen = resolveLaunchScreen()
                            },
                            onNavigateToDiagnostics = { currentScreen = AppScreen.Diagnostics },
                            onNavigateToPrivacyData = { currentScreen = AppScreen.PrivacyData },
                            onNavigateToAbout = { currentScreen = AppScreen.About },
                            onNavigateToStatistics = { currentScreen = AppScreen.Statistics },
                            onNavigateToRetention = { currentScreen = AppScreen.Retention },
                            onNavigateToPolling = { currentScreen = AppScreen.Polling }
                        )
                    }

                    AppScreen.Diagnostics -> {
                        BackHandler { currentScreen = resolveBackFromSettings() }
                        DiagnosticScreen(
                            onNavigateBack = { currentScreen = resolveBackFromSettings() }
                        )
                    }

                    AppScreen.PrivacyData -> {
                        BackHandler {
                            currentScreen = if (resolveLaunchScreen() == AppScreen.Start) AppScreen.Start else AppScreen.Settings
                        }
                        PrivacyDataScreen(
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
                            },
                            onNavigateToStatistics = { currentScreen = AppScreen.Statistics }
                        )
                    }

                    AppScreen.About -> {
                        BackHandler { currentScreen = AppScreen.Settings }
                        AboutScreen(
                            onNavigateBack = { currentScreen = AppScreen.Settings },
                            onNavigateToStatistics = { currentScreen = AppScreen.Statistics }
                        )
                    }

                    AppScreen.Statistics -> {
                        BackHandler { currentScreen = AppScreen.Settings }
                        StatisticsScreen(
                            onNavigateBack = { currentScreen = AppScreen.Settings }
                        )
                    }

                    AppScreen.Retention -> {
                        BackHandler { currentScreen = AppScreen.Settings }
                        RetentionSettingsScreen(
                            onNavigateBack = { currentScreen = AppScreen.Settings }
                        )
                    }

                    AppScreen.Polling -> {
                        BackHandler { currentScreen = AppScreen.Settings }
                        PollingFrequencyScreen(
                            onNavigateBack = { currentScreen = AppScreen.Settings }
                        )
                    }
                }
            }
        }
    }
}
