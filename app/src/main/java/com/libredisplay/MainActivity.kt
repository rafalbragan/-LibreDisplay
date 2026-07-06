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
import com.libredisplay.ui.settings.DiagnosticScreen
import com.libredisplay.ui.settings.SettingsScreen
import com.libredisplay.ui.theme.LibreDisplayTheme

enum class AppScreen {
    Monitoring,
    Settings,
    Diagnostics
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
                        if (app.settingsRepository.isConfigured()) AppScreen.Monitoring else AppScreen.Settings
                    )
                }

                when (currentScreen) {
                    AppScreen.Monitoring -> MonitoringScreen(
                        refreshNonce = refreshNonce,
                        onNavigateToSettings = { currentScreen = AppScreen.Settings },
                        onNavigateToDiagnostics = { currentScreen = AppScreen.Diagnostics }
                    )

                    AppScreen.Settings -> SettingsScreen(
                        onNavigateBack = {
                            currentScreen = if (app.settingsRepository.isConfigured()) AppScreen.Monitoring else AppScreen.Settings
                        },
                        onSaved = {
                            refreshNonce += 1
                            currentScreen = AppScreen.Monitoring
                        },
                        onNavigateToDiagnostics = { currentScreen = AppScreen.Diagnostics }
                    )

                    AppScreen.Diagnostics -> DiagnosticScreen(
                        onNavigateBack = {
                            currentScreen = if (app.settingsRepository.isConfigured()) AppScreen.Monitoring else AppScreen.Settings
                        }
                    )
                }
            }
        }
    }
}
