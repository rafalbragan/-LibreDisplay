# LibreCare Release Report 1.2.0

## Summary
- Finalized release-candidate checks for LibreCare branding, versioning, tests, and build outputs.
- Bumped app version to `1.2.0 (3)` for release progression.
- Resolved the release signing blocker by generating a machine-local upload keystore and configuring ignored local signing properties.
- Successfully generated signed release APK and signed release AAB artifacts.

## Branding
- App name: `LibreCare` (`app/src/main/res/values/strings.xml`, `app_name`)
- Project name: `LibreCare` (`settings.gradle.kts`, `rootProject.name`)
- ApplicationId: `com.libredisplay` (unchanged)
- Package/namespace: `com.libredisplay` (unchanged)

Remaining `LibreDisplay` references and classification:
- Package/applicationId reference:
  - `app/build.gradle.kts` (`namespace`, `applicationId`)
  - `build.gradle.kts` (`appId`)
- Internal technical name (safe temporarily):
  - `app/src/main/java/com/libredisplay/LibreDisplayApp.kt` (class name)
  - `app/src/main/java/com/libredisplay/ui/theme/Theme.kt` (`LibreDisplayTheme`)
  - `app/src/main/java/com/libredisplay/sync/LibreDisplaySyncScheduler.kt`
  - `app/src/main/java/com/libredisplay/widget/LibreDisplayWidgetProvider.kt`
- Database name / legacy technical:
  - `app/src/main/java/com/libredisplay/data/local/LibreDisplayDatabase.kt` (`LibreDisplayDatabase`, `libredisplay.db`)
- Documentation/history reference:
  - `RELEASE_PREPARATION_REPORT.md`
- Generated/build output:
  - `build/**`, `app/build/**`, `app/schemas/com.libredisplay.data.local.LibreDisplayDatabase/**`
- User-facing references renamed now:
  - Updated diagnostic clipboard label to `LibreCareLog` in `app/src/main/java/com/libredisplay/ui/settings/DiagnosticScreen.kt`
  - Updated diagnostic crash tags to `LibreCareApp` in `app/src/main/java/com/libredisplay/LibreDisplayApp.kt`

## Versioning
- Previous versionName: `1.1.0`
- Previous versionCode: `2`
- New versionName: `1.2.0`
- New versionCode: `3`

## Build artifacts
- Debug APK path and size:
  - `C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\apk\debug\app-debug.apk`
  - `22,911,519 bytes`
- Release APK path and size:
-   - `C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\apk\release\app-release.apk`
-   - `2,933,614 bytes`
-   - Signed: yes (`apksigner verify --print-certs`, signer `CN=LibreCare Developer, O=LibreCare, C=PL`)
- Release AAB path and size:
-   - `C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\bundle\release\app-release.aab`
-   - `5,338,848 bytes`
-   - Signed: yes (`jarsigner -verify` exit code `0`, self-signed upload certificate)
- Google Play upload artifact path and size:
-   - `C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\bundle\release\app-release.aab`
-   - `5,338,848 bytes`

## Tests
- `clean`: PASS (`./gradlew clean`)
- `testDebugUnitTest`: PASS (`./gradlew testDebugUnitTest`)
- `lint`: PASS (`./gradlew lint`)
- `connectedDebugAndroidTest`: NOT RUN (no connected device/emulator: `No connected devices!`)

## Google Play readiness
- Demo Mode status: Present (`StartScreen`, `MonitoringScreen`, mock client flow)
- Privacy & Data screen status: Present (`PrivacyDataScreen`)
- Data deletion buttons status: Present
  - Delete My Stored Data
  - Delete Local Glucose History
  - Delete Monitored People
  - Disconnect Account
  - Clear Session Data
  - Reset App Data
  - Delete Demo Data (shown in demo mode)
- Room migration status: Registered (`ALL_MIGRATIONS`, `MIGRATION_1_2`) and applied via `.addMigrations(*ALL_MIGRATIONS)`
- Release signing status: PASS for local RC build using a machine-local generated upload keystore in ignored files (`local.properties`, `*.jks` not committed)
- Icon status: Launcher and adaptive icons configured (`@mipmap/ic_launcher`, `@mipmap/ic_launcher_round`, adaptive XML uses `ic_launcher_foreground_librecare`)

## Manual steps still required
- upload release AAB to Google Play Console
- store the generated upload keystore and passwords in secure backup / secret storage before publishing updates
- complete Data Safety form
- complete Health Apps declaration
- provide Privacy Policy URL
- add screenshots
- add feature graphic
- complete internal testing

