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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.res.Configuration
import com.libredisplay.diagnostics.UiAuditCaptureResult
import com.libredisplay.diagnostics.UiAuditCaptureContext
import com.libredisplay.diagnostics.UiAuditExporter
import com.libredisplay.diagnostics.UiAuditStep
import com.libredisplay.data.model.AppMode
import com.libredisplay.ui.monitoring.MonitoringScreen
import com.libredisplay.ui.analytics.DataAnalysisScreen
import com.libredisplay.ui.futures.FuturesScreen
import com.libredisplay.ui.privacy.PrivacyDataScreen
import com.libredisplay.ui.restore.StartupRestoreHost
import com.libredisplay.ui.settings.AboutScreen
import com.libredisplay.ui.settings.DiagnosticScreen
import com.libredisplay.ui.settings.PollingFrequencyScreen
import com.libredisplay.ui.settings.RetentionSettingsScreen
import com.libredisplay.ui.settings.SettingsFocusSection
import com.libredisplay.ui.settings.SettingsScreen
import com.libredisplay.ui.settings.StatisticsScreen
import com.libredisplay.ui.start.StartScreen
import com.libredisplay.ui.theme.LibreDisplayTheme
import com.libredisplay.ui.settings.AccountSettingsScreen
import com.libredisplay.ui.settings.MonitoringSettingsScreen
import com.libredisplay.ui.settings.MonitoringSettingsSection
import com.libredisplay.ui.settings.SettingsMainScreen
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDateTime

enum class AppScreen {
    Start,
    Monitoring,
    Analytics,
    Futures,
    Settings,
    SettingsAccount,
    SettingsTargetRange,
    SettingsHomeMetrics,
    SettingsHbA1c,
    Diagnostics,
    PrivacyData,
    About,
    Statistics,
    Retention,
    Polling
}

class MainActivity : androidx.fragment.app.FragmentActivity() {

    private lateinit var appLock: com.libredisplay.auth.AppLockRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val app = application as LibreDisplayApp
        appLock = com.libredisplay.auth.AppLockRepository(applicationContext)
        setContent {
            LibreDisplayTheme {
                var unlocked by remember {
                    mutableStateOf(!appLock.isEnabled || appLock.isSessionUnlocked)
                }
                if (!unlocked) {
                    com.libredisplay.ui.security.AppLockScreen(
                        onUnlocked = {
                            appLock.markUnlockedForSession()
                            unlocked = true
                        },
                        onExit = { finish() }
                    )
                    return@LibreDisplayTheme
                }
                var refreshNonce by remember { mutableIntStateOf(0) }
                var showLoginOnly by remember { mutableStateOf(false) }
                var uiAuditNonce by remember { mutableIntStateOf(0) }
                var uiAuditInProgress by remember { mutableStateOf(false) }
                var uiAuditProgressLabel by remember { mutableStateOf<String?>(null) }
                var uiAuditResultPath by remember { mutableStateOf<String?>(null) }

                fun resolveLaunchScreen(): AppScreen {
                    val target = AppLaunchResolver.resolve(app.settingsRepository.loadSettings(), app.settingsRepository.hasPersistedSessionData())
                    showLoginOnly = target == AppLaunchTarget.LOGIN
                    return when (target) {
                        AppLaunchTarget.START -> AppScreen.Start
                        AppLaunchTarget.LOGIN -> AppScreen.Settings
                        AppLaunchTarget.MONITORING -> AppScreen.Monitoring
                    }
                }

                var navigationState by remember {
                    mutableStateOf(initialNavigationState(resolveLaunchScreen(), showLoginOnly))
                }
                val currentScreen = navigationState.current
                var settingsFocusSection by remember { mutableStateOf(SettingsFocusSection.GENERAL) }
                var showExitConfirmation by remember { mutableStateOf(false) }
                var startupRestoreActive by remember { mutableStateOf(false) }
                var autoOpenRestorePicker by remember { mutableStateOf(false) }

                fun relaunchNavigation() {
                    navigationState = initialNavigationState(resolveLaunchScreen(), showLoginOnly)
                }

                fun navigateTo(screen: AppScreen) {
                    navigationState = navigationState.navigateTo(screen)
                }

                fun navigateBack() {
                    navigationState = navigationState.navigateBack()
                }

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

                LaunchedEffect(currentScreen, refreshNonce) {
                    if (currentScreen != AppScreen.Monitoring) return@LaunchedEffect
                    val settings = app.settingsRepository.loadSettings()
                    val isLive = settings.appMode == AppMode.LIVE
                    val hasSession = app.settingsRepository.hasPersistedSessionData()
                    val alreadyAcknowledged = app.settingsRepository.isRestorePromptAcknowledged()
                    if (!isLive || !hasSession || alreadyAcknowledged) return@LaunchedEffect
                    val emptyLocal = app.appDataBackupRepository.isLocalLiveDataEmpty()
                    if (emptyLocal) {
                        app.settingsRepository.setRestorePromptAcknowledged(true)
                        startupRestoreActive = true
                    }
                }

                StartupRestoreHost(
                    active = startupRestoreActive,
                    onFinished = {
                        startupRestoreActive = false
                        refreshNonce += 1
                    }
                )

                if (uiAuditInProgress && uiAuditProgressLabel != null) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Generowanie raportu UI") },
                        text = { Text(uiAuditProgressLabel.orEmpty()) },
                        confirmButton = {}
                    )
                }

                if (!uiAuditInProgress && uiAuditResultPath != null) {
                    AlertDialog(
                        onDismissRequest = { uiAuditResultPath = null },
                        title = { Text("Raport UI gotowy") },
                        text = { Text("Plik raportu: ${uiAuditResultPath.orEmpty()}") },
                        confirmButton = {
                            TextButton(onClick = { uiAuditResultPath = null }) {
                                Text("OK")
                            }
                        }
                    )
                }

                val uiAuditSteps = remember {
                    listOf(
                        UiAuditStep(AppScreen.Monitoring, "Główny", "Główny", listOf("czytelność aktualnej glikemii", "gęstość sekcji Home", "widoczność alertów")),
                        UiAuditStep(AppScreen.Analytics, "Główny -> Historia", "Główny -> Historia", listOf("czy wykres zaczyna się wysoko", "spójność nagłówka", "kompaktowość statystyk")),
                        UiAuditStep(AppScreen.Settings, "Główny -> Ustawienia", "Główny -> Ustawienia", listOf("hierarchia sekcji", "czy login/hasło nie są na górze", "liczba wierszy na viewport")),
                        UiAuditStep(AppScreen.SettingsTargetRange, "Ustawienia -> Monitorowanie -> Zakres", "Główny -> Ustawienia -> Monitorowanie -> Zakres", listOf("czytelność pól zakresu", "zwartość layoutu", "ergonomia edycji")),
                        UiAuditStep(AppScreen.SettingsHomeMetrics, "Ustawienia -> Monitorowanie -> Metryki", "Główny -> Ustawienia -> Monitorowanie -> Metryki", listOf("kolejność metryk", "zrozumiałość opisów", "dotyk i odstępy")),
                        UiAuditStep(AppScreen.SettingsHbA1c, "Ustawienia -> Monitorowanie -> HbA1c", "Główny -> Ustawienia -> Monitorowanie -> HbA1c", listOf("czytelność pól HbA1c", "obsługa pustych danych", "gęstość informacji")),
                        UiAuditStep(AppScreen.SettingsAccount, "Ustawienia -> LibreLinkUp", "Główny -> Ustawienia -> Konto i połączenie -> LibreLinkUp", listOf("czy hasło nie jest stale pokazane", "status konta", "akcje zmiany konta")),
                        UiAuditStep(AppScreen.PrivacyData, "Ustawienia -> Prywatność i dane", "Główny -> Ustawienia -> Dane i prywatność", listOf("jasność akcji prywatności", "ryzykowne akcje", "nazewnictwo")),
                        UiAuditStep(AppScreen.Retention, "Ustawienia -> Retencja", "Główny -> Ustawienia -> Dane i prywatność -> Retencja", listOf("konsekwencje retencji", "czytelność opcji czasu", "ostrzeżenia")),
                        UiAuditStep(AppScreen.Polling, "Ustawienia -> Synchronizacja", "Główny -> Ustawienia -> Monitorowanie -> Synchronizacja", listOf("zrozumiałość wpływu na baterię", "zakres opcji", "opisy transferu")),
                        UiAuditStep(AppScreen.About, "Ustawienia -> O aplikacji", "Główny -> Ustawienia -> Aplikacja -> O aplikacji", listOf("dane wersji", "czytelność informacji", "spójność stylu")),
                        UiAuditStep(AppScreen.Diagnostics, "Ustawienia -> Zaawansowane -> Diagnostyka", "Główny -> Ustawienia -> Zaawansowane -> Diagnostyka", listOf("narzędzia logów", "bezpieczeństwo danych", "czy akcje są czytelne")),
                        UiAuditStep(AppScreen.Statistics, "Ustawienia -> Statystyki", "Główny -> Ustawienia -> Statystyki", listOf("użyteczność metryk", "braki danych", "czytelność liczb"))
                    )
                }

                LaunchedEffect(uiAuditNonce) {
                    if (uiAuditNonce == 0 || uiAuditInProgress) return@LaunchedEffect

                    uiAuditInProgress = true
                    val previousNavigationState = navigationState
                    val sessionDir = UiAuditExporter.createSessionDirectory(this@MainActivity)
                    val results = mutableListOf<UiAuditCaptureResult>()

                    fun auditStateFor(screen: AppScreen): AppNavigationState = when (screen) {
                        AppScreen.Monitoring,
                        AppScreen.Analytics,
                        AppScreen.Futures,
                        AppScreen.Settings,
                        AppScreen.Start -> AppNavigationState(listOf(screen))
                        AppScreen.SettingsTargetRange,
                        AppScreen.SettingsHomeMetrics,
                        AppScreen.SettingsHbA1c,
                        AppScreen.SettingsAccount,
                        AppScreen.Diagnostics,
                        AppScreen.PrivacyData,
                        AppScreen.About,
                        AppScreen.Statistics,
                        AppScreen.Retention,
                        AppScreen.Polling -> AppNavigationState(listOf(AppScreen.Monitoring, AppScreen.Settings, screen))
                    }

                    uiAuditSteps.forEachIndexed { index, step ->
                        uiAuditProgressLabel = "Zrzut ${index + 1}/${uiAuditSteps.size}: ${step.label}"
                        navigationState = auditStateFor(step.screen)
                        delay(900)

                        val safeName = step.label
                            .lowercase()
                            .replace(" ", "-")
                            .replace("->", "-")
                            .replace("/", "-")
                            .replace("ę", "e")
                            .replace("ó", "o")
                            .replace("ą", "a")
                            .replace("ś", "s")
                            .replace("ł", "l")
                            .replace("ż", "z")
                            .replace("ź", "z")
                            .replace("ć", "c")
                            .replace("ń", "n")
                        val screenshotFileName = String.format("%02d_%s.png", index + 1, safeName)
                        val screenshotFile = File(sessionDir, screenshotFileName)
                        val captureSuccess = UiAuditExporter.captureCurrentScreen(this@MainActivity, screenshotFile)
                        val settings = app.settingsRepository.loadSettings()
                        val metadataFileName = String.format("%02d_%s.json", index + 1, safeName)
                        val metadataFile = File(sessionDir, metadataFileName)
                        val configuration = resources.configuration
                        val metadataWritten = captureSuccess && UiAuditExporter.writeCaptureMetadata(
                            activity = this@MainActivity,
                            destinationFile = metadataFile,
                            screenshotFile = screenshotFile,
                            step = step,
                            context = UiAuditCaptureContext(
                                appVersion = BuildConfig.VERSION_NAME,
                                appMode = settings.appMode.name,
                                selectedPatientId = settings.selectedPatientId,
                                refreshIntervalSeconds = settings.refreshInterval,
                                backgroundPollingMinutes = settings.backgroundPollingMinutes,
                                retentionHours = settings.retentionHours,
                                targetLowMgDl = settings.targetLow,
                                targetHighMgDl = settings.targetHigh
                            )
                        )
                        results += UiAuditCaptureResult(
                            step = step,
                            screenshotFileName = screenshotFileName,
                            captureSuccess = captureSuccess,
                            metadataFileName = metadataFileName.takeIf { metadataWritten },
                            screenWidthDp = configuration.screenWidthDp,
                            fontScale = configuration.fontScale,
                            orientation = when (configuration.orientation) {
                                Configuration.ORIENTATION_LANDSCAPE -> "landscape"
                                Configuration.ORIENTATION_PORTRAIT -> "portrait"
                                else -> "undefined"
                            },
                            darkTheme = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                        )
                    }

                    val reportFile = File(sessionDir, "ui-audit-report.md")
                    val reportText = UiAuditExporter.buildReportContent(
                        appVersion = BuildConfig.VERSION_NAME,
                        generatedAt = LocalDateTime.now(),
                        results = results
                    )
                    reportFile.writeText(reportText, Charsets.UTF_8)

                    navigationState = previousNavigationState
                    uiAuditProgressLabel = null
                    uiAuditInProgress = false
                    uiAuditResultPath = reportFile.absolutePath
                }

                when (currentScreen) {
                    AppScreen.Start -> StartScreen(
                        onConnectWithLibreLinkUp = {
                            app.settingsRepository.switchToLiveMode()
                            showLoginOnly = true
                            refreshNonce += 1
                            relaunchNavigation()
                        },
                        onTryDemoMode = {
                            app.settingsRepository.switchToDemoMode()
                            showLoginOnly = false
                            refreshNonce += 1
                            navigationState = AppNavigationState(listOf(AppScreen.Monitoring))
                        }
                    )

                    AppScreen.Monitoring -> {
                        BackHandler { showExitConfirmation = true }
                        MonitoringScreen(
                            refreshNonce = refreshNonce,
                            onNavigateToSettings = {
                                settingsFocusSection = SettingsFocusSection.GENERAL
                                navigateTo(AppScreen.Settings)
                            },
                            onNavigateToMetricSettings = {
                                navigateTo(AppScreen.SettingsHomeMetrics)
                            },
                            onNavigateToDiagnostics = { navigateTo(AppScreen.Diagnostics) },
                            onNavigateToAnalytics = { navigateTo(AppScreen.Analytics) },
                            onNavigateToFutures = { navigateTo(AppScreen.Futures) },
                            onSwitchToLiveMode = {
                                app.settingsRepository.switchToLiveMode()
                                showLoginOnly = true
                                refreshNonce += 1
                                relaunchNavigation()
                            },
                            onRunUiAudit = {
                                if (!uiAuditInProgress) {
                                    uiAuditNonce += 1
                                }
                            }
                        )
                    }

                    AppScreen.Analytics -> {
                        BackHandler { navigateBack() }
                        DataAnalysisScreen(
                            showBackButton = navigationState.stack.size > 2,
                            onNavigateBack = { navigateBack() },
                            onOpenHome = { navigateTo(AppScreen.Monitoring) },
                            onOpenFutures = { navigateTo(AppScreen.Futures) },
                            onOpenSettings = { navigateTo(AppScreen.Settings) }
                        )
                    }

                    AppScreen.Futures -> {
                        BackHandler { navigateBack() }
                        FuturesScreen(
                            onOpenHome = { navigateTo(AppScreen.Monitoring) },
                            onOpenAnalytics = { navigateTo(AppScreen.Analytics) },
                            onOpenSettings = { navigateTo(AppScreen.Settings) }
                        )
                    }

                    AppScreen.Settings -> {
                        BackHandler { navigateBack() }
                        if (showLoginOnly) {
                            SettingsScreen(
                                loginOnly = true,
                                focusSection = settingsFocusSection,
                                onNavigateBack = { navigateBack() },
                                onSaved = {
                                    showLoginOnly = false
                                    refreshNonce += 1
                                    relaunchNavigation()
                                },
                                onNavigateToDiagnostics = { navigateTo(AppScreen.Diagnostics) },
                                onNavigateToPrivacyData = { navigateTo(AppScreen.PrivacyData) },
                                onNavigateToAbout = { navigateTo(AppScreen.About) },
                                onNavigateToStatistics = { navigateTo(AppScreen.Statistics) },
                                onNavigateToRetention = { navigateTo(AppScreen.Retention) },
                                onNavigateToPolling = { navigateTo(AppScreen.Polling) }
                            )
                        } else {
                            SettingsMainScreen(
                                showBackButton = navigationState.stack.size > 2,
                                onNavigateBack = { navigateBack() },
                                onOpenHome = { navigateTo(AppScreen.Monitoring) },
                                onOpenHistory = { navigateTo(AppScreen.Analytics) },
                                onOpenFutures = { navigateTo(AppScreen.Futures) },
                                onNavigateToMonitoring = {
                                    navigateTo(AppScreen.SettingsTargetRange)
                                },
                                onNavigateToMetricSettings = {
                                    navigateTo(AppScreen.SettingsHomeMetrics)
                                },
                                onNavigateToHbA1cSettings = {
                                    navigateTo(AppScreen.SettingsHbA1c)
                                },
                                onNavigateToAccount = { navigateTo(AppScreen.SettingsAccount) },
                                onNavigateToDataPrivacy = { navigateTo(AppScreen.PrivacyData) },
                                onNavigateToStatistics = { navigateTo(AppScreen.Statistics) },
                                onNavigateToAppearance = { navigateTo(AppScreen.About) },
                                onNavigateToAdvanced = { navigateTo(AppScreen.Diagnostics) },
                                onNavigateToRetention = { navigateTo(AppScreen.Retention) },
                                onNavigateToPolling = { navigateTo(AppScreen.Polling) }
                            )
                        }
                    }

                    AppScreen.SettingsTargetRange -> {
                        BackHandler { navigateBack() }
                        MonitoringSettingsScreen(
                            section = MonitoringSettingsSection.TARGET_RANGE,
                            onNavigateBack = { navigateBack() },
                            onSaved = {
                                refreshNonce += 1
                                navigateBack()
                            }
                        )
                    }

                    AppScreen.SettingsHomeMetrics -> {
                        BackHandler { navigateBack() }
                        MonitoringSettingsScreen(
                            section = MonitoringSettingsSection.HOME_METRICS,
                            onNavigateBack = { navigateBack() },
                            onSaved = {
                                refreshNonce += 1
                                navigateBack()
                            }
                        )
                    }

                    AppScreen.SettingsHbA1c -> {
                        BackHandler { navigateBack() }
                        MonitoringSettingsScreen(
                            section = MonitoringSettingsSection.HBA1C,
                            onNavigateBack = { navigateBack() },
                            onSaved = {
                                refreshNonce += 1
                                navigateBack()
                            }
                        )
                    }

                    AppScreen.SettingsAccount -> {
                        BackHandler { navigateBack() }
                        AccountSettingsScreen(
                            onNavigateBack = { navigateBack() },
                            onSaved = {
                                showLoginOnly = false
                                refreshNonce += 1
                                navigateBack()
                            }
                        )
                    }

                    AppScreen.Diagnostics -> {
                        BackHandler { navigateBack() }
                        DiagnosticScreen(
                            onNavigateBack = { navigateBack() }
                        )
                    }

                    AppScreen.PrivacyData -> {
                        BackHandler { navigateBack() }
                        PrivacyDataScreen(
                            onNavigateBack = { navigateBack() },
                            onNavigateToStart = {
                                showLoginOnly = false
                                refreshNonce += 1
                                navigationState = AppNavigationState(listOf(AppScreen.Start))
                            },
                            onNavigateToLogin = {
                                showLoginOnly = true
                                refreshNonce += 1
                                relaunchNavigation()
                            },
                            onNavigateToStatistics = { navigateTo(AppScreen.Statistics) },
                            openRestorePickerOnEnter = autoOpenRestorePicker,
                            onRestorePickerConsumed = { autoOpenRestorePicker = false }
                        )
                    }

                    AppScreen.About -> {
                        BackHandler { navigateBack() }
                        AboutScreen(
                            onNavigateBack = { navigateBack() },
                            onNavigateToStatistics = { navigateTo(AppScreen.Statistics) }
                        )
                    }

                    AppScreen.Statistics -> {
                        BackHandler { navigateBack() }
                        StatisticsScreen(
                            onNavigateBack = { navigateBack() }
                        )
                    }

                    AppScreen.Retention -> {
                        BackHandler { navigateBack() }
                        RetentionSettingsScreen(
                            onNavigateBack = { navigateBack() }
                        )
                    }

                    AppScreen.Polling -> {
                        BackHandler { navigateBack() }
                        PollingFrequencyScreen(
                            onNavigateBack = { navigateBack() }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Keep the session unlocked across configuration changes (e.g. rotation), but require a
        // fresh unlock when the app is genuinely sent to the background or closed.
        if (!isChangingConfigurations && ::appLock.isInitialized) {
            appLock.clearSession()
        }
    }

    override fun onStart() {
        super.onStart()
        // Start the always-on foreground monitoring service (if the user kept it enabled and
        // monitoring is configured). Starting from a visible Activity satisfies Android 12+ FGS
        // launch restrictions, and the service keeps running after the UI is closed.
        com.libredisplay.service.MonitoringServiceController.startIfEnabled(this)
    }
}



