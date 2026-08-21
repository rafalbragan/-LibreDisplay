# Release Report - LibreCare 2.2.1

## Summary
Release 2.2.1 finishes the Home readability and interaction polish work. The update restores the UI audit entry point, adds a textual UI score report, improves the Home header and monitored-person switcher, moves metrics below the chart, and refines the Home chart navigator, data-span messaging, and X-axis readability.

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
- `LibreTopBar` now shows app version, compact DB span, last reading age, estimated sensor end, data range, and the restored camera icon for UI audit.
- `CompactPersonSwitcherBar` shows up to two wide monitored-person chips and an expandable `+N` overflow list ordered by recent selection.
- `RedesignedCurrentGlucoseCard` keeps glucose and trend in a single horizontal row and uses tighter recommendation padding.
- `ImprovedQuickMetricsPanel` is now placed below the Home chart and rendered as one horizontally scrollable strip.
- Home metrics settings support show/hide per metric and include additional metrics such as average glucose and sensor activity.
- Home chart now shows:
  - compact DB coverage summary (`12h / 3d / 7d / 30d`)
  - header summary `Okno Xh · baza Yd`
  - a single proportional viewport navigator without the extra system slider
  - X-axis labels with date + time for hourly windows
- UI audit report now contains a textual UX score and heuristic risk flags.

## Database changes
- No Room schema/version changes.
- No migration files added.
- Settings storage gained metric-visibility persistence only; no schema migration required.

## Tests
- Executed:
  - `./gradlew clean`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lint`
  - `./gradlew assembleDebug`
  - `./gradlew assembleRelease`
  - `./gradlew bundleRelease`
- Additionally executed focused regressions:
  - `UiAuditExporterTest`
  - `HomeChartModelsTest`
  - `PolishFormattersTest`
  - `DataCoverageModelTest`
  - `RedesignedMetricsTest`
- Connected tests: **No connected device/emulator available.**
- Tooling note: `adb` command is not available in the current shell PATH.

## Artifacts
- Debug APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.1-debug.apk` (23 464 012 B)
- Release APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.1-release.apk` (3 178 011 B)
- Release AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.1-release.aab` (5 808 931 B)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.1-release.aab` (5 808 931 B)

## Branding review
- user-facing: **EXISTS** (`LibreCare` retained in Home, reports, and release notes)
- package/applicationId: **EXISTS** (`com.libredisplay` retained as technical identifier)
- migration legacy: **NOT APPLICABLE**
- documentation: **INCOMPLETE** (historical `LibreDisplay` references remain in repository path and older docs)
- generated output: **EXISTS**

## Remaining risks
- Manual validation on physical device/emulator is still missing for gesture feel (`pinch`, navigator drag, chart tap) and scaling across target width/fontScale combinations.
- Widget-specific regressions were not explicitly audited in this release.
- The textual UX score is heuristic and should be treated as supportive diagnostics, not as a replacement for manual UI review.


