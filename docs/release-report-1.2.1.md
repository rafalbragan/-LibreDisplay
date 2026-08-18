# LibreCare Release Report 1.2.1

## Summary
- fixed Demo Mode / Live Mode flow
- fixed Polish localization on startup, privacy, about, and flow dialogs/messages
- fixed token clearing path and forced relogin flow
- fixed reset behavior so app no longer falls back to Demo after reset
- improved tests for launch routing and mode/session regressions
- release artifacts generated (Debug APK, Release APK, Release AAB)

## Bugs fixed
- Demo Mode had no clear switch to Live
- Live Mode could route incorrectly and skip explicit login screen when session was missing
- "Wyczyść zapisany token i zaloguj ponownie" flow did not consistently enforce LIVE + login
- reset/clear flows could leave stale mode/navigation state that reopened Demo
- several user-facing English strings remained visible in key screens

## User-facing changes
- startup screen now uses Polish copy and clear mode choices:
  - `Połącz z LibreLinkUp`
  - `Uruchom tryb demo`
- Demo banner now includes a visible switch action:
  - `Przełącz na tryb Live`
  - with Polish confirmation dialog
- Live login guidance now explicitly references LibreLink / LibreLinkUp account credentials
- Privacy & Data actions and confirmations localized to Polish
- token clearing flow now confirms and navigates to login with Polish success message
- About screen and reviewer instruction text localized to Polish

## Technical changes
- launch routing tightened in `app/src/main/java/com/libredisplay/AppLaunchResolver.kt`:
  - `AppMode.LIVE` -> `LOGIN` until persisted session exists
  - no implicit Demo fallback after reset/token clear
- startup/navigation state handling updated in `app/src/main/java/com/libredisplay/MainActivity.kt`:
  - added `showLoginOnly` gating for explicit login flow
  - propagated mode switch handling from start, monitoring, and privacy flows
- monitoring screen updated in `app/src/main/java/com/libredisplay/ui/monitoring/MonitoringScreen.kt`:
  - added Demo->Live confirmation dialog
  - added visible `Przełącz na tryb Live` action in Demo banner
- privacy logic hardened in `app/src/main/java/com/libredisplay/data/repository/PrivacyRepository.kt`:
  - explicit mode handling (`LIVE` / `NONE`) for clear/reset/disconnect flows
  - added `clearSavedTokenAndPrepareLiveLogin()`
- privacy VM/UI updated:
  - `app/src/main/java/com/libredisplay/ui/privacy/PrivacyDataViewModel.kt`
  - `app/src/main/java/com/libredisplay/ui/privacy/PrivacyDataScreen.kt`
  - added dedicated action: `Wyczyść zapisany token i zaloguj ponownie`
- settings login flow improved:
  - `app/src/main/java/com/libredisplay/ui/settings/SettingsViewModel.kt`
  - added `saveAndLogin()` and Polish error/rate-limit messages
  - `app/src/main/java/com/libredisplay/ui/settings/SettingsScreen.kt` supports login-only mode
- version bump in `app/build.gradle.kts`:
  - `versionName`: `1.2.0` -> `1.2.1`
  - `versionCode`: `3` -> `4`

## Branding verification
- application label / `app_name`: `LibreCare` (confirmed in `app/src/main/res/values/strings.xml`)
- launcher/app UI title surfaces: LibreCare (startup/top bar/about/privacy copy updated)
- remaining `LibreDisplay` references classification:
  - internal/package/applicationId (kept, reported): `com.libredisplay`, `LibreDisplayApp`, scheduler/worker/db class names
  - legacy storage/database identifiers (kept, reported): `libredisplay.db`, secure pref names
  - documentation/history references (reported): older report file `docs/release-report-1.2.0.md`
  - generated artifacts/build outputs: ignored

## Tests
- unit tests: `./gradlew testDebugUnitTest` - PASS
- ViewModel/repository tests updated:
  - `app/src/test/java/com/libredisplay/AppLaunchResolverTest.kt`
  - `app/src/test/java/com/libredisplay/data/repository/PrivacyRepositoryTest.kt`
  - `app/src/test/java/com/libredisplay/data/repository/GlucoseRepositoryTest.kt`
  - adjusted `app/src/test/java/com/libredisplay/data/repository/AuthRepositoryTest.kt` fixtures for explicit `AppMode.LIVE`
- lint: `./gradlew lint` - PASS
- connected tests: `./gradlew connectedDebugAndroidTest` - not executed (no connected device/emulator)

## Build artifacts
- Debug APK
  - path: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\apk\debug\app-debug.apk`
  - size: `22,960,342` bytes (`21.9 MiB`)
- Release APK
  - path: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\apk\release\app-release.apk`
  - size: `2,933,630` bytes (`2.8 MiB`)
- Release AAB
  - path: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\bundle\release\app-release.aab`
  - size: `5,352,329` bytes (`5.1 MiB`)
- Google Play upload artifact
  - path: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\bundle\release\app-release.aab`
  - size: `5,352,329` bytes (`5.1 MiB`)

## Manual follow-up
- test full login flow on real device with production LibreLinkUp credentials
- verify token clear -> relogin path against real API rate-limit behavior
- upload release AAB to Google Play internal testing track
- update screenshots/reviewer notes if store listing requires updated UI captures

