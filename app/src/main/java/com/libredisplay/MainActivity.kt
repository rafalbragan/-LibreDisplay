package com.libredisplay

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.libredisplay.data.repository.SettingsRepository
import com.libredisplay.service.MonitoringService
import com.libredisplay.ui.monitoring.MonitoringScreen
import com.libredisplay.ui.settings.SettingsScreen
import com.libredisplay.ui.theme.LibreDisplayTheme

/**
 * Single-activity host.
 *
 * Responsibilities:
 *  - Keep screen permanently ON (FLAG_KEEP_SCREEN_ON).
 *  - Opt into edge-to-edge / fullscreen display.
 *  - Host the Compose navigation graph.
 *  - Start the background MonitoringService.
 */
class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(applicationContext)

        // ── Keep screen permanently on ──────────────────────────────────────
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()
        applyImmersiveMode()

        // ── Start background monitoring service only when credentials exist ──
        if (settingsRepository.isConfigured()) {
            startMonitoringService()
        }

        setContent {
            LibreDisplayTheme {
                val navController = rememberNavController()
                val startDestination = if (settingsRepository.isConfigured()) "monitoring" else "settings"
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable("monitoring") {
                        MonitoringScreen(
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
        applyKioskModeIfEnabled()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
    }

    private fun startMonitoringService() {
        Intent(this, MonitoringService::class.java).also { intent ->
            startForegroundService(intent)
        }
    }

    private fun applyImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun applyKioskModeIfEnabled() {
        if (!settingsRepository.loadSettings().kioskMode) {
            if (isInLockTaskMode()) {
                runCatching { stopLockTask() }
            }
            return
        }

        runCatching { startLockTask() }
            .onFailure { Log.w("MainActivity", "Kiosk mode requested but lock task is unavailable") }
    }

    private fun isInLockTaskMode(): Boolean {
        val am = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        return am.lockTaskModeState != android.app.ActivityManager.LOCK_TASK_MODE_NONE
    }
}

