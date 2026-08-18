# Changelog

All notable changes to LibreCare will be documented in this file.

## 1.3.0 - 2026-08-18

### PL

#### Dodano
- Nowy model `TimeRangeState` do zarządzania zakresami czasowymi
- Enum `PresetTimeRange` z 7 predefiniowanymi zakresami (12h, 24h, 7 dni, 14 dni, 30 dni, 90 dni, 12 miesięcy)
- Komponenty Compose: `CompactPersonHeader`, `VisiblePersonSwitcher`, `CompactStatisticsGrid`, `TimeRangeDisplay`
- Ekran statystyk bazy danych i transferu sieciowego
- Ekran zarządzania retencją danych lokalnych
- Ekran konfiguracji częstotliwości pobierania danych
- Kompaktowe statystyki na dasboardzie
- Widoczny przełącznik osób jako chipy zamiast dropdown'u
- Wyświetlanie wybranego zakresu czasu
- ~45 nowych polskich etykiet

#### Zmieniono
- Przeprojektowany layout dasboardu - informacje bardziej zwarte
- `MonitoringUiState` - dodane pole `timeRange`
- Ulepszona hierarchia wizualna

#### Testy
- Nowa klasa `TimeRangeStateTest` (8 testów)
- Wszystkie 185 testów: PASS
- Lint: PASS

### EN

#### Added
- New `TimeRangeState` model for managing time ranges
- `PresetTimeRange` enum with 7 predefined ranges
- Compose components: `CompactPersonHeader`, `VisiblePersonSwitcher`, `CompactStatisticsGrid`, `TimeRangeDisplay`
- Database statistics and network transfer screen
- Data retention management screen
- Polling frequency configuration screen
- Compact statistics on dashboard
- Visible person switcher as chips instead of dropdown
- Selected time range display
- ~45 new Polish labels

#### Changed
- Redesigned dashboard layout - more compact information
- `MonitoringUiState` - added `timeRange` field
- Improved visual hierarchy

#### Tests
- New `TimeRangeStateTest` class (8 tests)
- All 185 tests: PASS
- Lint: PASS

## 1.2.1 - 2026-08-18

### PL

#### Dodano
- Widoczna akcja przełączania z trybu Demo na Live na ekranie monitorowania
- Polskie komunikaty potwierdzenia w przepływach logowania

#### Poprawiono
- Naprawiono przepływ trybu Demo i Live - Demo teraz ma widoczny przełącznik na Live
- Naprawiono nieprawidłowe routowanie trybu Live, które mogło pomijać ekran logowania
- Naprawiono procedurę "Wyczyść zapisany token i zaloguj ponownie" - teraz konsekwentnie wymusza LIVE + logowanie
- Naprawiono przepływy resetowania, które mogły zostawić stare stany trybu i nawigacji
- Naprawiono polską lokalizację na ekranie startowym i ekranach prywatności
- Usunięto pozostałe angielskie napisy z kluczowych ekranów

#### Zmieniono
- Wzmocniono routowanie startowe w AppLaunchResolver
- Zaktualizowano obsługę stanu nawigacji w MainActivity
- Poprawiono logikę prywatności w PrivacyRepository - dodano explicit mode handling
- Ulepszono obsługę logowania w ustawieniach z polskimi komunikatami błędów i rate-limit

#### Testy
- `./gradlew clean`: PASS
- `./gradlew testDebugUnitTest`: PASS
- `./gradlew lint`: PASS
- `./gradlew connectedDebugAndroidTest`: Nie uruchomiono (brak podłączonego urządzenia)
- `./gradlew assembleDebug`: PASS
- `./gradlew assembleRelease`: PASS
- `./gradlew bundleRelease`: PASS

#### Artefakty
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (21.9 MiB)
- Release APK: `app/build/outputs/apk/release/app-release.apk` (2.8 MiB)
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (5.1 MiB)
- Google Play upload artifact: `app/build/outputs/bundle/release/app-release.aab` (5.1 MiB)

### EN

#### Added
- Visible switch action from Demo to Live mode on monitoring screen
- Polish confirmation messages in login flows

#### Fixed
- Fixed Demo Mode and Live Mode flow transitions - Demo now has a visible switch to Live
- Fixed incorrect Live Mode routing that could skip the login screen
- Fixed token clearing procedure to consistently enforce LIVE + login
- Fixed reset flows that could leave stale mode and navigation states
- Fixed Polish localization on startup and privacy screens
- Removed remaining English strings from key screens

#### Changed
- Tightened launch routing in AppLaunchResolver
- Updated navigation state handling in MainActivity
- Improved privacy logic in PrivacyRepository with explicit mode handling
- Enhanced settings login handling with Polish error and rate-limit messages

#### Tests
- `./gradlew clean`: PASS
- `./gradlew testDebugUnitTest`: PASS
- `./gradlew lint`: PASS
- `./gradlew connectedDebugAndroidTest`: Not executed (no device connected)
- `./gradlew assembleDebug`: PASS
- `./gradlew assembleRelease`: PASS
- `./gradlew bundleRelease`: PASS

#### Artifacts
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (21.9 MiB)
- Release APK: `app/build/outputs/apk/release/app-release.apk` (2.8 MiB)
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (5.1 MiB)
- Google Play upload artifact: `app/build/outputs/bundle/release/app-release.aab` (5.1 MiB)

---

## 1.2.0 - 2026-08-15

### PL

#### Dodano
- Finalne sprawdzenia dla branding, versioningu, testów i artefaktów kompilacji LibreCare
- Podpisane artefakty Release APK i Release AAB

#### Poprawiono
- Naprawiono blokadę podpisywania release poprzez wygenerowanie lokalnego keystore'a
- Zaktualizowano ciąg narzędzi do tworzenia kompilacji

#### Zmieniono
- Bump wersji z 1.1.0 (2) na 1.2.0 (3)
- Zaktualizowano diagnostyke i tagi crashu na LibreCare

#### Testy
- `./gradlew clean`: PASS
- `./gradlew testDebugUnitTest`: PASS
- `./gradlew lint`: PASS
- `./gradlew connectedDebugAndroidTest`: Nie uruchomiono (brak urządzenia)

#### Artefakty
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (22.9 MiB)
- Release APK: `app/build/outputs/apk/release/app-release.apk` (2.9 MiB)
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (5.3 MiB)
- Google Play upload artifact: `app/build/outputs/bundle/release/app-release.aab` (5.3 MiB)

### EN

#### Added
- Final release-candidate checks for LibreCare branding, versioning, tests and build outputs
- Signed Release APK and Release AAB artifacts

#### Fixed
- Fixed release signing blocker by generating machine-local upload keystore
- Updated build tooling chain

#### Changed
- Version bump from 1.1.0 (2) to 1.2.0 (3)
- Updated diagnostics and crash tags to LibreCare

#### Tests
- `./gradlew clean`: PASS
- `./gradlew testDebugUnitTest`: PASS
- `./gradlew lint`: PASS
- `./gradlew connectedDebugAndroidTest`: Not executed (no device)

#### Artifacts
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (22.9 MiB)
- Release APK: `app/build/outputs/apk/release/app-release.apk` (2.9 MiB)
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (5.3 MiB)
- Google Play upload artifact: `app/build/outputs/bundle/release/app-release.aab` (5.3 MiB)

