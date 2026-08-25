# LibreCare — Module Inventory (from the real codebase)

Derived from the actual repository: `MainActivity.AppScreen` (navigation graph),
`AppNavigationState.kt` (routes/edges), Compose screens under `app/src/main/java/com/libredisplay/`,
and the main ViewModels. Package base: `com.libredisplay` (applicationId `com.libredisplay`, app
label **LibreCare**). No `UserMode` exists in production code yet (verified).

> `vary-by-mode?` = whether this module should *eventually* differ across CAREGIVER / SENIOR /
> CLINICIAN. It does **not** vary today.

## Navigation shell

- **Route enum:** `AppScreen` in `app/src/main/java/com/libredisplay/MainActivity.kt`
  (values: `Start, Monitoring, Analytics, Settings, SettingsAccount, SettingsTargetRange,
  SettingsHomeMetrics, SettingsHbA1c, Diagnostics, PrivacyData, About, Statistics, Retention,
  Polling`).
- **Navigation state / edges:** `app/src/main/java/com/libredisplay/AppNavigationState.kt`.
- **Top-level bar:** `ui/monitoring/TopLevelNavigationBar.kt` — three destinations:
  `Główna` (Home/Monitoring), `Analiza` (Analytics), `Ustawienia` (Settings).

---

## 1. Start / onboarding
- **Route:** `AppScreen.Start`
- **Files:** `ui/start/StartScreen.kt`, `AppLaunchResolver.kt`
- **Purpose:** First-run entry; choose Connect-with-LibreLinkUp (Live) or Try Demo.
- **Data displayed:** app intro / mode entry points.
- **Actions:** `Connect with LibreLinkUp`, `Try Demo Mode`.
- **vary-by-mode?** No (pre-mode entry). It is the natural place a *future* mode selector could live.

## 2. Home / Monitoring (PRIMARY)
- **Route:** `AppScreen.Monitoring`
- **Files:** `ui/monitoring/MonitoringScreen.kt`, `ui/monitoring/MonitoringViewModel.kt`,
  `RedesignedGlucoseCard.kt`, `RedesignedMetrics.kt`, `RedesignedTopBar.kt`, `GlucoseChart.kt`,
  `HomeChartModels.kt`, `DashboardComponents.kt` (`CompactPersonSwitcherBar`), `GlucoseWarningUi.kt`,
  `SensorStatusCalculator.kt`, `DataCoverageModel.kt`, `NfzAssessment.kt`.
- **Purpose:** Live status of the monitored person's CGM.
- **Data displayed:** current glucose + unit, trend arrow/description, min/max, freshness/staleness,
  time-in-range bar, quick metric tiles (below/in-range/above, GMI, CV, episodes, coverage),
  home chart with pan/zoom, NFZ eligibility card, warnings.
- **Actions:** switch monitored person, refresh, open full-screen history, open Analytics, open
  Settings, edit metrics, run UI audit, switch to Live from Demo.
- **vary-by-mode?** **Yes (high).** Caregiver: immediate status/risk; Senior: huge value + simple
  status; Clinician: clinical snapshot.

## 3. Full-screen history chart
- **Route:** overlay from Monitoring (not a distinct `AppScreen`); `historyContext` in
  `MonitoringScreen.kt`.
- **Files:** `ui/monitoring/FullScreenGlucoseChartScreen.kt`, `GlucoseChart.kt`,
  `HistoryAggregation.kt`, `HistoryUiModels.kt` (uses `MonitoringViewModel.loadDetailedHistory`).
- **Purpose:** Detailed, pannable history chart with tooltip, target band, min/max markers.
- **Data displayed:** history series, legend rows, selected-point tooltip, range selector.
- **Actions:** select range, pan/zoom, tap point, back.
- **vary-by-mode?** **Yes (medium).** Clinician: detailed analysis; Senior: simplified/omitted.

## 4. NFZ details
- **Route:** overlay from Monitoring (`nfzDetailsContext`).
- **Files:** `ui/monitoring/NfzDetailsScreen.kt`, `ui/monitoring/NfzAssessment.kt`.
- **Purpose:** Explain NFZ (reimbursement) status: reason, recommendation, evaluation period, info.
- **Data displayed:** status + reason + required vs current values + recommendation (never bare
  red/green).
- **Actions:** read explanation, back.
- **vary-by-mode?** **Yes (low/medium).** Caregiver/Clinician value; likely hidden for Senior.

## 5. Analiza / Analytics
- **Route:** `AppScreen.Analytics`
- **Files:** `ui/analytics/AnalyticsScreen.kt` (`DataAnalysisScreen`),
  `ui/analytics/AnalyticsViewModel.kt` (`DataAnalysisViewModel`),
  `analytics/AnalysisChartFactory.kt`, `analytics/AnalysisTrendInterpreter.kt`,
  `analytics/AnalysisMetricsFactory.kt`, `analytics/GlucoseMetricsCalculator.kt`,
  `analytics/RawDataExcelExporter.kt`.
- **Purpose:** Retrospective analysis of stored CGM history.
- **Data displayed:** time-in-range bar chart (14 daily bars / 12 monthly bars), 14-day day-profile
  overlay (thin daily lines + thick average), automatic trend observations, per-period metrics table
  (TIR/below/above, average, CV, GMI, min/max, episodes, sensor activity), custom date range.
- **Actions:** switch person, `Cały dzień`/`Tylko nocne` filter, scroll bars in time, `Dzień`/
  `Miesiąc` mode, custom range picker, export raw data to `.xlsx`.
- **vary-by-mode?** **Yes (high).** Clinician: deep analysis + reports; Caregiver: "what happened";
  Senior: simplified recent history.

## 6. Settings hub
- **Route:** `AppScreen.Settings`
- **Files:** `ui/settings/SettingsMainScreen.kt`, `ui/settings/SettingsScreen.kt` (login form when
  `loginOnly`).
- **Purpose:** Central settings navigation (and first-run LibreLinkUp login form).
- **Data displayed:** grouped settings entries.
- **Actions:** navigate to Account, Monitoring (target range), Metrics, HbA1c, Data & Privacy,
  Statistics, Appearance/About, Advanced/Diagnostics, Retention, Sync/Polling.
- **vary-by-mode?** **Yes (medium).** Which entries are prominent/hidden differs by mode.

## 7. Account / LibreLinkUp connection
- **Route:** `AppScreen.SettingsAccount`
- **Files:** `ui/settings/AccountSettingsScreen.kt`, `data/repository/AuthRepository.kt`,
  `data/repository/CredentialRepository.kt`.
- **Purpose:** Manage LibreLinkUp credentials/connection and account status.
- **Actions:** enter/change credentials, connect, disconnect.
- **vary-by-mode?** No (shared account/connection concern).

## 8. Monitoring settings — Target range / Home metrics / HbA1c
- **Routes:** `AppScreen.SettingsTargetRange`, `AppScreen.SettingsHomeMetrics`,
  `AppScreen.SettingsHbA1c`
- **Files:** `ui/settings/MonitoringSettingsScreen.kt` (`MonitoringSettingsSection.TARGET_RANGE /
  HOME_METRICS / HBA1C`), `data/model/QuickMetricConfig.kt`, `data/model/HbA1cSettings.kt`.
- **Purpose:** Configure target range, which quick metrics show + order, HbA1c/GMI inputs.
- **Actions:** edit target low/high, reorder/toggle metrics (drag & drop), enter lab HbA1c/target.
- **vary-by-mode?** **Yes (medium).** Default metric sets/order could differ per mode.

## 9. Sync / Polling frequency
- **Route:** `AppScreen.Polling`
- **Files:** `ui/settings/PollingFrequencyScreen.kt`, `ui/settings/PollingFrequencyViewModel.kt`,
  `service/RefreshController.kt`, `sync/LibreDisplaySyncScheduler.kt`.
- **Purpose:** Configure polling/refresh frequency and transfer expectations.
- **Actions:** choose interval; see battery/transfer impact.
- **vary-by-mode?** No (shared system setting).

## 10. Data retention
- **Route:** `AppScreen.Retention`
- **Files:** `ui/settings/RetentionSettingsScreen.kt`, `ui/settings/RetentionSettingsViewModel.kt`.
- **Purpose:** Configure how long local history is kept.
- **Actions:** choose retention window.
- **vary-by-mode?** No (shared system setting).

## 11. Privacy & Data
- **Route:** `AppScreen.PrivacyData`
- **Files:** `ui/privacy/PrivacyDataScreen.kt`, `ui/privacy/PrivacyDataViewModel.kt`,
  `ui/privacy/PrivacyConstants.kt`, `data/repository/PrivacyRepository.kt`,
  `data/repository/AppDataBackupRepository.kt`, `data/backup/*`.
- **Purpose:** Local backup/restore, delete data, app-lock (biometric/passkey), privacy policy.
- **Actions:** create/refresh backup, export/share/import backup, restore, delete history/people/
  account, reset app, enable app lock.
- **vary-by-mode?** No/low (shared privacy controls).

## 12. Statistics / storage & transfer
- **Route:** `AppScreen.Statistics`
- **Files:** `ui/settings/StatisticsScreen.kt`, `ui/settings/StatisticsViewModel.kt`,
  `data/repository/DiagnosticsStatsRepository.kt`, `data/repository/NetworkUsageTracker.kt`.
- **Purpose:** Show database size/growth, reading/person counts, transfer up/down, sync counts,
  storage/transfer estimates ("Za mało danych do dokładnej estymacji" when insufficient).
- **Actions:** read stats.
- **vary-by-mode?** **Yes (low).** Clinician cares most; Senior likely hidden.

## 13. About
- **Route:** `AppScreen.About`
- **Files:** `ui/settings/AboutScreen.kt`
- **Purpose:** Version/build info, links.
- **vary-by-mode?** No.

## 14. Diagnostics
- **Route:** `AppScreen.Diagnostics`
- **Files:** `ui/settings/DiagnosticScreen.kt`, `diagnostics/DiagnosticLogger.kt`,
  `diagnostics/UiAuditExporter.kt`, `diagnostics/DiagnosticStatus.kt`.
- **Purpose:** Logs, share log, UI-audit capture (developer/support).
- **vary-by-mode?** No (developer/support tool).

## 15. App lock / security
- **Route:** shown by `MainActivity` before content when enabled.
- **Files:** `ui/security/AppLockScreen.kt`, `auth/AppLockRepository.kt`,
  `auth/BiometricAuthManager.kt`, `auth/PasskeyManager.kt`.
- **Purpose:** Unlock the app via biometric/passkey.
- **vary-by-mode?** No (security gate).

## 16. Startup restore
- **Route:** host overlay from `MainActivity`.
- **Files:** `ui/restore/StartupRestoreHost.kt`, `ui/restore/StartupRestoreViewModel.kt`,
  `ui/restore/StartupRestoreModels.kt`, `ui/restore/StartupRestoreFormatter` (test-covered).
- **Purpose:** Offer restoring local data on first launch when local live data is empty.
- **vary-by-mode?** No.

## 17. Home-screen widget
- **Files:** `widget/LibreDisplayWidgetProvider.kt`, `widget/WidgetUpdater.kt`.
- **Purpose:** Glanceable widget (last reading).
- **vary-by-mode?** **Yes (FUTURE).** Senior "glanceable widgets" are a stated future priority.

## 18. Background monitoring service (non-UI)
- **Files:** `service/MonitoringService.kt`, `service/MonitoringServiceController.kt`,
  `service/RefreshController.kt`, `sync/LibreDisplaySyncWorker.kt`,
  `backup/AutomaticBackupWorker.kt`, `receiver/BootReceiver.kt`.
- **Purpose:** Always-on foreground polling + scheduled sync + automatic backup.
- **vary-by-mode?** No (shared infrastructure).

---

## Supporting domain/data packages (not screens)

- `data/api/**` — LibreLinkUp clients (v2/v3), HTTP, models, timestamp parsing, mock client.
- `data/local/**` — Room database, DAOs, entities, migrations, converters.
- `data/repository/**` — glucose sync/history, settings, auth, privacy, backup, diagnostics stats.
- `data/backup/**` — backup codec/merge/coverage/models.
- `data/model/**` — `GlucoseReading`, `GlucoseHistoryPoint`, `GlucoseTrend`, settings, metric config.
- `analytics/**` — metrics/calculators, chart factory, trend interpreter, xlsx exporter.
- `alert/**` — glucose alert coordinator/messaging/dispatcher.

These are shared across all future modes by design (see `MODE_ARCHITECTURE.md`).

