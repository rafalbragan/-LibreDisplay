# Changelog

All notable changes to LibreCare will be documented in this file.

## 1.7.0 - 2026-08-19

### PL

#### Dodano
- `DataCoverageModel` – centralny model oddzielający WYBRANY ZAKRES od DOSTĘPNYCH DANYCH.
- `formatNaturalDuration()` – naturalny polski format czasu trwania: „15 min", „8 godz. 02 min", „24 godz.", „7 dni".
- `TimeRange.toSelectedRangeLabel()` – krótkie polskie etykiety zakresów.
- Informacja o dostępnym spanie danych bezpośrednio przy wykresie i nagłówkach statystyk.
- Szacowany czas do pełnego zakresu: „Przy ciągłym zapisie pełny zakres 24 godz. będzie dostępny za ok. X godz. Y min."
- `DataCoverageModelTest` – 8 nowych przypadków testowych.

#### Zmieniono
- `ImprovedQuickMetricsPanel` – usunięto opakowanie w Card; metryki bezpośrednio na tle.
- `GlucoseChartCard` – usunięto opakowanie w Card; sekcja oddzielona subtelnymi separatorami.
- `TimeRangeDisplay` – usunięto opakowanie w Surface/pill; płaski wiersz z ikoną.
- Tytuł statystyk: „Statystyki · 8 godz. 02 min danych" zamiast „Statystyki - Ostatnie 24 godz."
- Sekcja „Czas w zakresach" pokazuje faktyczny span danych, nie wybrany zakres.
- Nagłówek wykresu i statystyk w HistoryScreen używa rzeczywistego span zamiast range.label.
- Zmniejszono promienie zaokrągleń: karta glukozy 20→14dp, NFZ 18→12dp, MedicalAlert 12→8dp.

#### Poprawiono
- Usunięto ogólny komunikat „Zużycie baterii może wzrosnąć" z ustawień pollingu (zastąpiony precyzyjnym opisem w kontekście opcji).
- Spójne formatowanie polskich nazw czasów trwania w całej aplikacji.

#### Testy
- `./gradlew testDebugUnitTest`: PASS (274 testów)
- `./gradlew lint`: PASS
- `./gradlew assembleDebug`: PASS
- `./gradlew assembleRelease`: PASS
- `./gradlew bundleRelease`: PASS

#### Artefakty
- `release-artifacts/LibreCare-1.7.0-debug.apk` (~22 657 KB)
- `release-artifacts/LibreCare-1.7.0-release.apk` (~2 984 KB)
- `release-artifacts/LibreCare-1.7.0-release.aab` (~5 483 KB)

### EN

#### Added
- `DataCoverageModel` – central model separating SELECTED RANGE from AVAILABLE DATA.
- `formatNaturalDuration()` – natural Polish duration formatting.
- Coverage info and estimate shown below chart and in statistics headers.

#### Changed
- `ImprovedQuickMetricsPanel` – removed Card wrapper; flat layout.
- `GlucoseChartCard` – removed Card wrapper; uses dividers.
- `TimeRangeDisplay` – removed Surface/pill; flat row.
- Statistics title uses actual available span, not selected range.
- Reduced corner radii throughout.

#### Fixed
- Removed vague battery warning from polling settings.



### PL

#### Dodano
- Ekran `NfzDetailsScreen` jako pełny ekran szczegółów refundacji NFZ (zamiast nieprzewijalnego dialogu).
- Mechanizm otwierania `Ustawień` bezpośrednio w sekcji metryk HbA1c/GMI (`SettingsFocusSection.HBA1C`).
- Personalizowaną kolejność kafelków `Quick Metrics` (long press + drag & drop na Home) z zapisem lokalnym.
- Alternatywną, dostępnościową zmianę kolejności metryk w `Ustawieniach` (przyciski góra/dół).

#### Zmieniono
- Home: karta glikemii używa przebudowanego komponentu `RedesignedCurrentGlucoseCard` z medycznym alertem inline.
- Home: `Quick Metrics` korzysta z panelu `ImprovedQuickMetricsPanel` i akcji `Zmień metryki` prowadzącej do odpowiedniej sekcji Ustawień.
- Home: dolna nawigacja pokazuje wyłącznie działające pozycje: `Główna`, `Historia`, `Więcej`.
- History: selektor zakresu jest jednoliniowy i przewijany poziomo.
- History: sekcja `Notatki i zdarzenia` została ukryta do czasu pełnej implementacji funkcji.
- History: podsumowanie `MIN / ŚREDNIA / MAX` prezentowane jako kompaktowy wiersz.
- History chart: subtelniejsze pasma zakresów, cieńsza linia, jeden marker zaznaczenia oraz tooltip bez ucinania przy prawej krawędzi.

#### Poprawiono
- Ujednolicono próg starych danych glikemii do 15 minut.
- Android Back: na Home pojawia się potwierdzenie zamknięcia aplikacji, a na pod-ekranach cofanie wraca o jeden poziom.

#### Testy
- `./gradlew compileDebugKotlin --no-configuration-cache`: PASS
- `./gradlew testDebugUnitTest --no-configuration-cache`: PASS
- `./gradlew lint --no-configuration-cache`: PASS
- `./gradlew assembleDebug --no-configuration-cache`: PASS
- `./gradlew assembleRelease --no-configuration-cache`: PASS
- `./gradlew bundleRelease --no-configuration-cache`: PASS

#### Artefakty
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (22.6 MiB)
- Release APK: `app/build/outputs/apk/release/app-release.apk` (2.9 MiB)
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (5.2 MiB)

### EN

#### Added
- New `NfzDetailsScreen` full-screen NFZ reimbursement details view (replacing the non-scrollable dialog).
- Deep-link style Settings focus for HbA1c/GMI metrics section (`SettingsFocusSection.HBA1C`).
- User-customizable `Quick Metrics` tile order (long-press drag & drop on Home) with local persistence.
- Accessibility fallback metric reordering in `Settings` (move up/down controls).

#### Changed
- Home: current glucose now uses `RedesignedCurrentGlucoseCard` with integrated inline medical alert.
- Home: quick metrics use `ImprovedQuickMetricsPanel` with a working `Change metrics` action to Settings.
- Home: bottom navigation now keeps only working destinations: `Home`, `History`, `More`.
- History: time range selector is now single-row and horizontally scrollable.
- History: `Notes and events` is hidden until the feature is fully implemented.
- History: `MIN / AVERAGE / MAX` is displayed in a compact row.
- History chart: subtler range bands, thinner line, single selected marker, and tooltip clipping fix on the right edge.

#### Fixed
- Unified stale-data threshold to 15 minutes.
- Android Back behavior: Home shows an exit confirmation, nested screens return one level up.

#### Tests
- `./gradlew compileDebugKotlin --no-configuration-cache`: PASS
- `./gradlew testDebugUnitTest --no-configuration-cache`: PASS
- `./gradlew lint --no-configuration-cache`: PASS
- `./gradlew assembleDebug --no-configuration-cache`: PASS
- `./gradlew assembleRelease --no-configuration-cache`: PASS
- `./gradlew bundleRelease --no-configuration-cache`: PASS

#### Artifacts
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (22.6 MiB)
- Release APK: `app/build/outputs/apk/release/app-release.apk` (2.9 MiB)
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (5.2 MiB)

## 1.5.0 - 2026-08-18

### PL

#### Dodano
- Mockup-based redesign glownego dashboardu LibreCare z nowa hierarchia kart i ciemna paleta medyczna
- Nowy dolny pasek nawigacji z pozycjami: Glowna, Historia, Dodaj, Alarmy, Wiecej
- Rozszerzony ekran `Historia glikemii` z chipsami zakresu, legenda zakresow, statystykami i placeholderem `Notatki i zdarzenia`
- Centralne tokeny kolorow `LibreCareColors` i wspolne modele UI dla legendy/statystyk historii

#### Zmieniono
- Przeprojektowano top bar, kompaktowy selector osob i kompaktowy wiersz zakresu czasu zgodnie z kierunkiem mockupu
- Karta aktualnej glikemii ma teraz bardziej dominujacy uklad, wyrazniejszy trend i osobna karte warningow
- KPI cards zostaly zageszczone i przeniesione do poziomego scrolleru, aby nie obcinac waznych wartosci
- Wykres dashboardu i historia pelnoekranowa korzystaja z czytelniejszych osi, pasm ryzyka i tooltipa nad wybranym punktem
- Karta NFZ na dashboardzie zostala uproszczona, a szczegoly przeniesiono do dialogu informacyjnego

#### Poprawiono
- Wszystkie glówne czasy uzytkowe korzystaja z lokalnej strefy urzadzenia przez centralne formattery
- Zachowano rozroznienie `0m` vs `brak danych` w KPI i legendzie historii
- Dotkniecie tła wykresu nadal otwiera pełny ekran historii, a przeciąganie wybiera punkt i pokazuje tooltip
- Usunieto bledy kompilacji i ostrzezenia zwiazane z nowym shellem UI

#### Testy
- `./gradlew clean`: PASS
- `./gradlew testDebugUnitTest`: PASS
- `./gradlew lint`: PASS
- `./gradlew connectedDebugAndroidTest`: NIE WYKONANO (brak podlaczonego urzadzenia/emulatora)
- `./gradlew assembleDebug`: PASS
- `./gradlew assembleRelease`: PASS
- `./gradlew bundleRelease`: PASS

#### Artefakty
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (23.1 MiB)
- Release APK: `app/build/outputs/apk/release/app-release.apk` (2.9 MiB)
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (5.2 MiB)

### EN

#### Added
- Mockup-based redesign of the main LibreCare dashboard with a darker medical dashboard hierarchy
- New bottom navigation shell with Home, History, Add, Alarms, and More entries
- Expanded `Glucose history` screen with range chips, range legend, statistics, and a `Notes and events` placeholder
- Centralized `LibreCareColors` design tokens and shared history legend/stat UI models

#### Changed
- Redesigned the top bar, compact person selector, and compact time range row to follow the provided mockup direction
- The current glucose card now has a stronger visual hierarchy, clearer trend state, and a dedicated warning card
- KPI cards were compacted into a horizontal scroller to avoid clipping important values
- The dashboard chart and full-screen history now use clearer axes, risk bands, and a tooltip offset above the selected point
- The dashboard NFZ card was simplified, with details moved into an informational dialog

#### Fixed
- Main user-facing timestamps now consistently use the device local timezone through shared formatters
- Preserved the `0m` vs `no data` distinction in KPIs and the history legend
- Kept background-tap navigation to full-screen history while drag/touch selects points and shows a tooltip
- Resolved compile issues and UI shell regressions introduced during the redesign work

#### Tests
- `./gradlew clean`: PASS
- `./gradlew testDebugUnitTest`: PASS
- `./gradlew lint`: PASS
- `./gradlew connectedDebugAndroidTest`: NOT EXECUTED (no connected device/emulator)
- `./gradlew assembleDebug`: PASS
- `./gradlew assembleRelease`: PASS
- `./gradlew bundleRelease`: PASS

#### Artifacts
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (23.1 MiB)
- Release APK: `app/build/outputs/apk/release/app-release.apk` (2.9 MiB)
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (5.2 MiB)

## 1.4.1 - 2026-08-18

### PL

#### Dodano
- Centralne formattery czasu i duration dla dashboardu, wykresu i historii pelnoekranowej
- Rozszerzone podsumowanie NFZ z powodami, wskaznikami i najwazniejszymi zaleceniami
- Tooltip punktu wykresu podczas tap/drag na dashboardzie i w pelnym ekranie historii

#### Zmieniono
- Wykres i historia korzystaja ze wspolnej osi czasu opartej o lokalna strefe urzadzenia
- Kompaktowe etykiety czasu na osi wykresu sa dopasowywane do widocznego zakresu (godzina / data+godzina / data)
- Karta ostrzezen glikemii pokazuje jednoczesnie ryzyko kliniczne oraz swiezosc danych bez ukrywania pilnych stanow
- Dashboardowa karta NFZ korzysta z centralnego modelu statusu zamiast lokalnego skladania komunikatow

#### Poprawiono
- Rozroznienie `0m` vs `brak danych` w kafelkach zakresu pozostaje spójne w calej aplikacji
- Tap poza punktem na wykresie nadal otwiera pelny ekran historii, a tap blisko punktu wybiera pomiar
- Pelny ekran historii startuje na najnowszym punkcie i pokazuje czasy w lokalnej strefie urzadzenia
- Usunieto ostrzezenia kompilacji zwiazane z przestarzalymi ikonami wykresu

#### Testy
- `./gradlew testDebugUnitTest`: PASS
- `./gradlew lint`: PASS
- `./gradlew assembleDebug`: PASS
- `./gradlew assembleRelease`: PASS
- `./gradlew bundleRelease`: PASS

#### Artefakty
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (23.5 MiB)
- Release APK: `app/build/outputs/apk/release/app-release.apk` (2.9 MiB)
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (5.2 MiB)

### EN

#### Added
- Centralized time and duration formatters for the dashboard, chart, and full-screen history
- Extended NFZ summary model with reasons, metrics, and top recommendations
- Point tooltip support on tap/drag for the dashboard chart and full-screen history

#### Changed
- Chart and history now use a shared timeline and the device-local time zone
- Compact chart axis labels adapt to the visible range (time / date+time / date)
- Glucose warning UI now surfaces both clinical severity and data freshness without hiding urgent states
- The dashboard NFZ card now uses a central status summary model instead of composing messages ad hoc

#### Fixed
- Preserved the distinction between `0m` and `no data` across range tiles
- Kept background-tap navigation to full-screen history while allowing near-point selection on the chart
- Full-screen history now defaults to the latest point and displays local-device timestamps consistently
- Removed deprecated chart icon build warnings

#### Tests
- `./gradlew testDebugUnitTest`: PASS
- `./gradlew lint`: PASS
- `./gradlew assembleDebug`: PASS
- `./gradlew assembleRelease`: PASS
- `./gradlew bundleRelease`: PASS

#### Artifacts
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (23.5 MiB)
- Release APK: `app/build/outputs/apk/release/app-release.apk` (2.9 MiB)
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (5.2 MiB)

## 1.4.0 - 2026-08-18

### PL

#### Dodano
- Ekran `Informacje i statystyki` z realnymi danymi lokalnymi (baza, odczyty, zakres danych, synchronizacje)
- Agregowane liczniki transferu sieciowego (pobrane/wyslane bajty, liczba zapytan, sukcesy/bledy)
- Modele i logika diagnostyczna: `DatabaseStats`, `NetworkUsageStats`, estymacje retencji i odpytywania
- Ustawienie `Retencja danych` (12 godzin do 24 miesiecy) z estymacja i potwierdzeniem skrocenia
- Ustawienie `Czestotliwosc odpytywania` (15/30/60 min) z estymacja transferu
- Dostep do statystyk z Ustawien, Prywatnosci i O aplikacji

#### Zmieniono
- Synchronizator WorkManager korzysta z ustawionej czestotliwosci odpytywania (bezpieczny zakres 15-60 min)
- Cleanup retencji korzysta z konfiguracji godzinowej zamiast stalej wartosci 365 dni
- Tryb demo nie zasila licznikow realnego transferu
- Rozszerzono `AppSettings` o `retentionHours` i `backgroundPollingMinutes`

#### Poprawiono
- Usunieto ekranowe, sztuczne wartosci statystyk i zastapiono je danymi z repozytorium diagnostycznego
- Ujednolicono komunikat dla niewystarczajacych danych: "Za malo danych do dokladnej estymacji"

#### Testy
- Dodano testy: `DiagnosticsStatsRepositoryTest`, `NetworkUsageTrackerTest`, `SettingsRepositoryDiagnosticsTest`
- Testy jednostkowe: PASS

### EN

#### Added
- `Informacje i statystyki` screen with real local stats (database, readings, data range, sync info)
- Aggregated network usage counters (downloaded/uploaded bytes, request count, success/failure)
- Diagnostics domain models: `DatabaseStats`, `NetworkUsageStats`, retention/polling estimators
- `Data retention` setting (12 hours to 24 months) with estimate and confirmation on shortening
- `Polling frequency` setting (15/30/60 min) with transfer estimate
- Statistics access from Settings, Privacy and About screens

#### Changed
- WorkManager periodic sync now uses configured polling frequency (safe bounds 15-60 min)
- Retention cleanup uses configured hourly retention instead of hardcoded 365 days
- Demo mode traffic excluded from live network counters
- `AppSettings` extended with `retentionHours` and `backgroundPollingMinutes`

#### Fixed
- Replaced mocked statistics values with diagnostics-repository driven values
- Unified insufficient-data messaging: "Za malo danych do dokladnej estymacji"

#### Tests
- Added tests: `DiagnosticsStatsRepositoryTest`, `NetworkUsageTrackerTest`, `SettingsRepositoryDiagnosticsTest`
- Unit tests: PASS

## 1.3.0 - 2026-08-18

### PL

#### Dodano
- Nowy model `TimeRangeState` do zarządzania zakresami czasowymi
- Enum `PresetTimeRange` z 7 predefiniowanymi zakresami (12h, 24h, 7 dni, 14 dni, 30 dni, 90 dni, 12 miesięcy)
- Komponenty Compose: `CompactPersonSwitcherBar`, `VisiblePersonSwitcher`, `TimeRangeDisplay`
- Ekran statystyk bazy danych i transferu sieciowego
- Ekran zarządzania retencją danych lokalnych
- Ekran konfiguracji częstotliwości pobierania danych
- Kompaktowe statystyki na dasboardzie
- Widoczny przełącznik osób jako chipy zamiast dropdown'u
- Wyświetlanie wybranego zakresu czasu
- ~45 nowych polskich etykiet

#### Zmieniono
- Przeprojektowany layout dasboardu - informacje bardziej zwarte
- Usunięto zduplikowaną prezentację monitorowanej osoby
- Usunięto dropdown osoby z top bara dla 2-3 osób
- Usunięto etykietę "Osoba monitorowana" z głównego układu
- Wprowadzono kompaktowy przełącznik osób jako jedyne miejsce tożsamości
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
- Compose components: `CompactPersonSwitcherBar`, `VisiblePersonSwitcher`, `TimeRangeDisplay`
- Database statistics and network transfer screen
- Data retention management screen
- Polling frequency configuration screen
- Compact statistics on dashboard
- Visible person switcher as chips instead of dropdown
- Selected time range display
- ~45 new Polish labels

#### Changed
- Redesigned dashboard layout - more compact information
- Removed duplicated monitored-person identity blocks
- Removed top-bar person dropdown for 2-3 users
- Removed "Monitored person" label from the main dashboard flow
- Added compact person switcher as the only monitored-person identity area
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

