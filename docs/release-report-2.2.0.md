# Release Report - LibreCare 2.2.0

## Summary
Release 2.2.0 focuses on fixing Home readability regressions introduced by over-compression. The update prioritizes zero overlap, zero clipping of important content, larger readable typography, responsive layout structure, Home-only chart range/viewport controls, and stack-correct top-level navigation.

## Architecture review
### Affected functionality status
- existing implementation: **EXISTS**
- architecture: **EXISTS**
- ViewModels: **EXISTS**
- repositories: **EXISTS**
- Room database: **EXISTS**
- migrations: **NOT APPLICABLE** (no DB schema change)
- API layer: **EXISTS**
- Demo Mode: **EXISTS**
- Privacy & Data: **EXISTS**
- statistics: **EXISTS**
- widgets: **INCOMPLETE**
- charts: **EXISTS**
- Compose screens: **EXISTS**
- navigation: **EXISTS**

## UI changes
- `LibreTopBar` now respects status-bar safe area and uses larger typography.
- Data freshness / sensor state wraps to two lines on narrow widths instead of shrinking text.
- `RedesignedCurrentGlucoseCard` uses a responsive, multi-line layout for value + unit + trend.
- `ImprovedQuickMetricsPanel` uses adaptive rows and larger readable metric typography.
- Home chart gained:
  - independent selector `1h / 3h / 6h / 9h / 12h`
  - explicit `12 h / 24 h` data availability
  - navigator under the chart
  - X-axis pinch-to-zoom snap behavior
  - double tap reset to latest segment
- `AnalyticsScreen` and `SettingsMainScreen` now behave like top-level destinations without root back arrows.

## Database changes
- No Room schema/version changes.
- No migration files added.

## Tests
- Executed:
  - `./gradlew clean`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lint`
  - `./gradlew assembleDebug`
  - `./gradlew assembleRelease`
  - `./gradlew bundleRelease`
- Added/updated tests:
  - `AppNavigationStateTest`
  - `HomeChartModelsTest`
  - `RedesignedMetricsTest`
  - `GlucoseChartLayoutLogicTest`
  - `HistoryUiModelsTest`
  - `GlucoseWarningUiTest`
- Connected tests: **No connected device/emulator available.**
- Tooling note: `adb` command is not available in the current shell PATH.

## Artifacts
- Debug APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.0-debug.apk` (23 447 625 B)
- Release APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.0-release.apk` (3 161 630 B)
- Release AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.0-release.aab` (5 797 368 B)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.0-release.aab`

## Branding review
- user-facing: **EXISTS** (`LibreCare` kept in app title and docs)
- package/applicationId: **EXISTS** (`com.libredisplay` retained as technical identifier)
- migration legacy: **NOT APPLICABLE**
- documentation: **INCOMPLETE** (historical `LibreDisplay` references remain in repository path/docs)
- generated output: **EXISTS**

## Remaining risks
- Manual verification for required `fontScale` and narrow-width device classes could not be completed in this environment.
- Full screenshot/golden coverage for Home layouts is still missing.
- Existing unrelated workspace modifications remain outside the scope of this UI/UX iteration.

