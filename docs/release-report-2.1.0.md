# Release Report - LibreCare 2.1.0

## Summary
Release 2.1.0 consolidates settings navigation, improves chart smoothness, adds per-person DB coverage visibility, and keeps LIVE-only backup/restore boundaries.

## Architecture review
### Affected functionality status
- existing implementation: **EXISTS**
- architecture: **EXISTS**
- ViewModels: **EXISTS**
- repositories: **EXISTS**
- Room database: **EXISTS**
- migrations: **EXISTS** (no schema change in this release)
- API layer: **EXISTS**
- Demo Mode: **EXISTS**
- Privacy & Data: **EXISTS**
- statistics: **EXISTS**
- widgets: **INCOMPLETE** (no dedicated widget regression run in this cycle)
- charts: **EXISTS**
- Compose screens: **EXISTS**
- navigation: **EXISTS**

## UI changes
- Settings area now exposes split entry points for main, monitoring, and account sections.
- Monitoring/history chart rendering now interpolates sparse points to minute-level presentation for smoother movement.
- Statistics screen now shows per-person data start and fill percentages for 14/30/60/90/360 days.
- Backup/restore actions remain visible in `Privacy & Data` with explicit LIVE-only scope.
- Fixed routing flaw where `Zakres`, `Metryki` and `HbA1c` audit steps pointed to one tabbed screen; now each has its own destination.

## Database changes
- No Room schema version change detected in this release cycle.
- Backup/restore flow continues to operate on LIVE-only entities and excludes demo rows.
- Added DAO aggregation for per-person coverage windows in live data.

## Tests
- Full validation completed:
  - `./gradlew clean`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lint`
  - `./gradlew assembleDebug`
  - `./gradlew assembleRelease`
  - `./gradlew bundleRelease`
- Connected tests: **No connected device/emulator available.**
- Tooling note: `adb` command was unavailable in current PATH.
- Added/updated tests: `GlucoseChartLayoutLogicTest`, `HistoryUiModelsTest`, `DiagnosticsStatsRepositoryTest`, `GlucoseSyncRepositoryBackfillTest`.

## Navigation audit (root cause)
- Cause: all three audit routes (`Zakres`, `Metryki`, `HbA1c`) used one `AppScreen.SettingsMonitoring` destination with internal tab state.
- Fix: added dedicated destinations (`SettingsTargetRange`, `SettingsHomeMetrics`, `SettingsHbA1c`) and routed settings entries directly.
- Result: screenshots/routes are now distinct by destination, not only by tab selection.

## Artifacts
- Debug APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.0-debug.apk` (23 414 839 B)
- Release APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.0-release.apk` (3 145 248 B)
- Release AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.0-release.aab` (5 759 431 B)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.0-release.aab`

## Branding review
- user-facing: **EXISTS** (`LibreCare` in release docs/changelog sections updated for 2.1.0)
- package/applicationId: **EXISTS** (`com.libredisplay` retained as technical legacy)
- migration legacy: **EXISTS** (no rename migration executed)
- documentation: **INCOMPLETE** (historical docs still contain legacy `LibreDisplay` path/class references)
- generated output: **EXISTS** (build paths include workspace folder name `LibreDisplay`)

## Remaining risks
- Full chart touch regression still depends on connected-device instrumentation.
- Historical documentation includes legacy naming references outside this release scope.
- Coverage percentages assume expected 1-minute cadence and may differ from vendor sampling policy for specific devices.

