# LibreCare 2.16.4 - Release Notes

## PL

### Wersja
- versionName: `2.16.4`
- versionCode: `48`
- Data: `2026-10-18`

### Nowe funkcje
- Dodano spojną polityke danych DEMO (`DEMO` + kompatybilnosc z `DemoMode`) i wspolne helpery izolacji.
- Dodano metadane manifestu `com.librecare.ENABLE_MIKA_DEMO_PROFILE` do weryfikacji flagi w podpisanym APK.

### Poprawki
- Poprawiono generator Miki: ostatni punkt zawsze ma `timestamp <= aktualny czas`.
- Zablokowano alarmy/powiadomienia kliniczne dla danych DEMO/syntetycznych.
- Wzmocniono separacje DEMO vs LIVE w backupie/statystykach/filtrowaniu zrodla danych.

### Ulepszenia
- Rozszerzono testy regresyjne o: brak przyszlych timestampow Miki, izolacje DEMO source, alarm suppression, scenariusz restartu recovery.
- Dodano test instrumentacyjny scenariusza `brak danych -> zmiana osoby -> restart -> powrot do aktywnej osoby`.

### Testy
- `./gradlew clean` - PASS
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS
- `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.libredisplay.ui.monitoring.MonitoringRecoveryScenarioInstrumentedTest` - PASS (SM-S948B)

### Artefakty
- Debug APK: `release-artifacts/LibreCare-2.16.4-debug.apk`
- Release APK: `release-artifacts/LibreCare-2.16.4-release.apk`
- Release AAB: `release-artifacts/LibreCare-2.16.4-release.aab`
- Google Play upload file: `release-artifacts/LibreCare-2.16.4-release.aab`

### Ograniczenia / Blockery
- Google Sign-In (Credential Manager) - **IN PROGRESS**.
- Google Drive backup/restore - **IN PROGRESS**.

## EN

### Version
- versionName: `2.16.4`
- versionCode: `48`
- Date: `2026-10-18`

### New Features
- Added a shared DEMO data policy (`DEMO` with backward compatibility for `DemoMode`) for stricter synthetic/live isolation.
- Added manifest metadata `com.librecare.ENABLE_MIKA_DEMO_PROFILE` so the flag can be verified directly in a signed APK.

### Fixes
- Fixed Mika generator so the latest point always has `timestamp <= current time`.
- Disabled clinical alarms/notifications for DEMO/synthetic data.
- Strengthened DEMO vs LIVE separation across backup/statistics/source filtering.

### Improvements
- Added regression coverage for: no-future Mika timestamps, DEMO-source isolation, alarm suppression, restart recovery path.
- Added instrumentation scenario test for `no data -> person switch -> restart -> return to active person`.

### Tests
- `./gradlew clean` - PASS
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS
- `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.libredisplay.ui.monitoring.MonitoringRecoveryScenarioInstrumentedTest` - PASS (SM-S948B)

### Artifacts
- Debug APK: `release-artifacts/LibreCare-2.16.4-debug.apk`
- Release APK: `release-artifacts/LibreCare-2.16.4-release.apk`
- Release AAB: `release-artifacts/LibreCare-2.16.4-release.aab`
- Google Play upload file: `release-artifacts/LibreCare-2.16.4-release.aab`

### Known Limitations / Blockers
- Google Sign-In (Credential Manager) - **IN PROGRESS**.
- Google Drive backup/restore - **IN PROGRESS**.

