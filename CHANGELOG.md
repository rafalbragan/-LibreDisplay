# Changelog

All notable changes to LibreCare will be documented in this file.

## 2.10.1 - 2026-08-25

### PL

#### Added
- Ekran Analizy: dodano picker zakresu własnego `od-do` (dialog wyboru dat) i przeliczanie metryk dla wybranego zakresu.
- Ekran Analizy: dodano dotykowe wskazanie punktu/słupka (tooltip tekstowy pod wykresem) dla wykresu tygodniowego i nakładki 14 dni.

#### Changed
- Eksport `.xlsx` korzysta teraz z aktualnie wybranego zakresu własnego (jeśli został ustawiony), zamiast zawsze eksportować cały zakres lokalnych danych.

#### Fixed
- Selektor zakresu na ekranie głównym pozostaje ręcznie przewijany i nie zmienia zakresu automatycznie podczas scrollowania; dodano test regresyjny zachowania.

#### Tests
- `DataAnalysisCustomRangeTest` — mapowanie zakresu z pickera dat na granice dobowe.
- `HomeChartRangeSelectorBehaviorTest` — ręczne przewijanie bez automatycznej zmiany wyboru.

### EN

#### Added
- Analysis screen: added custom `from-to` date range picker and metrics recomputation for the selected range.
- Analysis screen: added tap-based selection feedback (text tooltip under charts) for weekly bars and 14-day overlay.

#### Changed
- `.xlsx` export now uses the currently selected custom range (when set), instead of always exporting the full local history.

#### Fixed
- Home range selector remains manually scrollable and does not auto-switch range during scrolling; regression behavior test added.

#### Tests
- `DataAnalysisCustomRangeTest` — picker range to day-boundary mapping.
- `HomeChartRangeSelectorBehaviorTest` — manual scroll without implicit selection change.

## 2.10.0 - 2026-08-25

### PL

#### Added
- Nowy ekran `Analiza danych` z tabelą metryk dla okresów `1g / 3g / 6g / 24g / 7d / 30d / Własny` (TIR, poniżej/powyżej, średnia, CV, GMI, min/max, epizody, aktywność sensora).
- Wykres tygodniowy stacked (poniżej / w zakresie / powyżej) oraz nakładka 14 dni (linie dzienne + średnia minutowa).
- Eksport surowych danych do Excela (`.xlsx`) z arkuszami `Dane surowe` i `Podsumowanie`, z możliwością udostępnienia pliku.

#### Changed
- Selektor zakresu wykresu Home (`1g`, `3g`, `...`) działa teraz w pełni manualnie: można go przewijać bez auto-przeskoków, a samo przewijanie nie zmienia automatycznie wybranego zakresu.

#### Fixed
- Poprawiono problem "uciekających" klocków zakresu w wąskich szerokościach i orientacji poziomej; większe zakresy są dostępne po ręcznym przewinięciu.

#### Tests
- `AnalysisMetricsFactoryTest` — metryki okresowe i epizody.
- `AnalysisChartFactoryTest` — tygodniowe słupki i nakładka 14 dni.
- `RawDataExcelExporterTest` — generowanie pliku `.xlsx` i struktury arkuszy.
- `HomeChartRangeSelectorBehaviorTest` — przewijanie selektora zakresu bez samoczynnej zmiany wyboru.

#### Artifacts
- `release-artifacts/LibreCare-2.10.0-debug.apk`
- `release-artifacts/LibreCare-2.10.0-release.apk`
- `release-artifacts/LibreCare-2.10.0-release.aab` (plik do Google Play)

### EN

#### Added
- New `Data analysis` screen with a metrics table for `1h / 3h / 6h / 24h / 7d / 30d / Custom` periods (TIR, below/above, average, CV, GMI, min/max, episodes, sensor activity).
- Weekly stacked range chart (below / in range / above) and 14-day overlay (daily lines + minute average).
- Raw data export to Excel (`.xlsx`) with `Dane surowe` and `Podsumowanie` sheets, including file sharing.

#### Changed
- The Home chart range selector (`1h`, `3h`, `...`) is now fully manual: users can scroll it without auto-jumps, and scrolling no longer auto-changes the selected range.

#### Fixed
- Fixed the "escaping" range chips issue on narrow widths and landscape; larger ranges are reachable via manual scrolling.

#### Tests
- `AnalysisMetricsFactoryTest` — period metrics and episodes.
- `AnalysisChartFactoryTest` — weekly stacked bars and 14-day overlay.
- `RawDataExcelExporterTest` — `.xlsx` generation and sheet structure.
- `HomeChartRangeSelectorBehaviorTest` — range selector scrolling without implicit selection change.

#### Artifacts
- `release-artifacts/LibreCare-2.10.0-debug.apk`
- `release-artifacts/LibreCare-2.10.0-release.apk`
- `release-artifacts/LibreCare-2.10.0-release.aab` (Google Play upload file)

## 2.9.1 - 2026-08-25

### PL

#### Changed
- Drag&drop metryk na ekranie głównym: **sąsiedni kafelek płynnie „ucieka" w nowe miejsce** podczas przenoszenia (strip przełączony na `LazyRow` + `animateItem`); przeciągany kafelek nadal ma animację uniesienia.

#### Tests
- `MonitoringResponsiveUiTest` — dostosowany do `LazyRow` (przewija do każdej wartości i sprawdza pełny, nieucinany tekst przez `performScrollToNode`).
- `./gradlew testDebugUnitTest` — PASS.

#### Artifacts
- `release-artifacts/LibreCare-2.9.1-debug.apk`
- `release-artifacts/LibreCare-2.9.1-release.apk`
- `release-artifacts/LibreCare-2.9.1-release.aab` (plik do Google Play)

### EN

#### Changed
- Home-screen metrics drag & drop: the **displaced neighbour now slides smoothly into place** while dragging (strip switched to `LazyRow` + `animateItem`); the dragged tile keeps its lift animation.

#### Tests
- `MonitoringResponsiveUiTest` — adapted to `LazyRow` (scrolls to each value and checks full, untruncated text via `performScrollToNode`).
- `./gradlew testDebugUnitTest` — PASS.

#### Artifacts
- `release-artifacts/LibreCare-2.9.1-debug.apk`
- `release-artifacts/LibreCare-2.9.1-release.apk`
- `release-artifacts/LibreCare-2.9.1-release.aab` (Google Play upload file)

## 2.9.0 - 2026-08-25

### PL

#### Changed
- Metryki na ekranie głównym i w Ustawieniach przestawiasz teraz **przeciąganiem** (przytrzymaj i przesuń): kafelek/wiersz się „unosi" (skala + cień), a upuszczenie zmienia kolejność. Usunięto przyciski strzałek w Ustawieniach.
- Widok poziomy: **selektor zakresu (chipy 1g/3g/…) przeniesiony do lewej kolumny** — pod wartość glikemii i zalecenia, nad kartą NFZ. Zmiana zakresu od razu odświeża wykres i metryki.
- Po obróceniu do orientacji poziomej ekran **automatycznie przewija się** tak, aby zaczynać od sekcji „Historia glikemii".

#### Fixed
- **Po wczytaniu danych z kopii aplikacja nie wymusza już ponownego logowania.** Ekran logowania pojawia się tylko wtedy, gdy naprawdę brak i zapisanej sesji, i danych logowania (`AppLaunchResolver` ujednolicony z `shouldShowLoginForm`).

#### Tests
- `AppLaunchResolverTest` — zaktualizowany (LIVE z danymi logowania → MONITORING; bez sesji i bez danych → LOGIN).
- `./gradlew testDebugUnitTest` — PASS.

#### Artifacts
- `release-artifacts/LibreCare-2.9.0-debug.apk`
- `release-artifacts/LibreCare-2.9.0-release.apk`
- `release-artifacts/LibreCare-2.9.0-release.aab` (plik do Google Play)

### EN

#### Changed
- Home-screen and Settings metrics now reorder via **drag & drop** (long-press and move): the tile/row lifts (scale + shadow) and dropping changes the order. Arrow buttons removed from Settings.
- Landscape: the **range selector (1h/3h/… chips) moved into the left column** — under the glucose value/recommendations, above the NFZ card. Changing the range updates the chart and metrics immediately.
- On rotating to landscape the screen **auto-scrolls** to start at the "Historia glikemii" section.

#### Fixed
- **Restoring a backup no longer forces re-login.** The login screen only appears when there is genuinely no saved session AND no stored credentials (`AppLaunchResolver` aligned with `shouldShowLoginForm`).

#### Tests
- `AppLaunchResolverTest` — updated (LIVE with credentials → MONITORING; no session and no credentials → LOGIN).
- `./gradlew testDebugUnitTest` — PASS.

#### Artifacts
- `release-artifacts/LibreCare-2.9.0-debug.apk`
- `release-artifacts/LibreCare-2.9.0-release.apk`
- `release-artifacts/LibreCare-2.9.0-release.aab` (Google Play upload file)

## 2.8.0 - 2026-08-25

### PL

#### Added
- Widok poziomy: **metryki zajmują teraz całą szerokość ekranu** (od lewej do prawej) pod wierszem glukoza/wykres — więcej miejsca i lepsza czytelność.

#### Changed
- **Domyślny zakres wykresu na ekranie głównym**: 12 godz. w pionie i 24 godz. w poziomie (o ile zebrano wystarczająco danych; w przeciwnym razie największy dostępny zakres).
- **Wybór zakresu jest zapamiętywany w obrębie sesji**: po ręcznym wybraniu zakresu aplikacja go utrzymuje (także po obrocie ekranu), zamiast wracać do domyślnego.
- CI: workflow Firebase Test Lab uruchamia instrumentation tests na **macierzy 3 telefonów wirtualnych** (SMALL/STANDARD/LARGE) w jednym przebiegu, z walidacją model+API.

#### Tests
- `./gradlew testDebugUnitTest` — PASS.
- `./gradlew lint` — PASS.

#### Artifacts
- `release-artifacts/LibreCare-2.8.0-debug.apk`
- `release-artifacts/LibreCare-2.8.0-release.apk`
- `release-artifacts/LibreCare-2.8.0-release.aab` (plik do Google Play)

### EN

#### Added
- Landscape: **metrics now span the full screen width** (left-to-right) below the glucose/chart row — more space and better readability.

#### Changed
- **Default home chart range**: 12h in portrait and 24h in landscape (when enough data has been collected; otherwise the largest available range).
- **Range choice persists within the session**: once the user picks a range it is kept (including across rotation) instead of reverting to the default.
- CI: the Firebase Test Lab workflow runs instrumentation tests on a **3 virtual-phone matrix** (SMALL/STANDARD/LARGE) in one run, with model+API validation.

#### Tests
- `./gradlew testDebugUnitTest` — PASS.
- `./gradlew lint` — PASS.

#### Artifacts
- `release-artifacts/LibreCare-2.8.0-debug.apk`
- `release-artifacts/LibreCare-2.8.0-release.apk`
- `release-artifacts/LibreCare-2.8.0-release.aab` (Google Play upload file)

## 2.7.0 - 2026-08-25

### PL

#### Added
- **Monitorowanie w tle (usługa pierwszoplanowa)**: LibreCare może teraz pobierać dane stale w tle ze stałym powiadomieniem, co ~30 s–5 min (wg ustawionej częstotliwości), odpornie na tryb Doze — także gdy ekran jest zamknięty. Domyślnie włączone; uruchamiane, gdy monitorowanie jest skonfigurowane.
- **Pionowy pasek nawigacji po prawej** w orientacji poziomej (Główna / Historia / Ustawienia) zamiast dolnego menu.

#### Changed
- W orientacji poziomej ukryto górny i dolny pasek — więcej miejsca na treść. Nagłówek „LibreCare" i „Ostatnia aktualizacja" przeniesione do kompaktowego nagłówka w lewej kolumnie.
- Uprawnienia `FOREGROUND_SERVICE` i `FOREGROUND_SERVICE_DATA_SYNC` dodane pod usługę monitorowania (Android 14+ przekazuje typ `dataSync`).

#### Fixed
- Wcześniej usługa monitorowania (`MonitoringService`) była zaimplementowana, ale **nigdy nie uruchamiana** — w tle działał tylko WorkManager (co 15–60 min, usypiany przez Doze). Teraz usługa jest realnie startowana z widocznej aktywności i po restarcie telefonu.

#### Tests
- `./gradlew testDebugUnitTest` — PASS.
- `./gradlew lint` — PASS.

#### Artifacts
- `release-artifacts/LibreCare-2.7.0-debug.apk`
- `release-artifacts/LibreCare-2.7.0-release.apk`
- `release-artifacts/LibreCare-2.7.0-release.aab` (plik do Google Play)

### EN

#### Added
- **Always-on background monitoring (foreground service)**: LibreCare can now fetch data continuously in the background with a persistent notification, every ~30 s–5 min (per the configured interval), Doze-resistant — even when the screen is closed. Enabled by default; started once monitoring is configured.
- **Vertical navigation rail on the right** in landscape (Home / History / Settings) instead of the bottom bar.

#### Changed
- Landscape now hides the top and bottom bars for more content space. The "LibreCare" title and "last update" moved into a compact header in the left column.
- Added `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_DATA_SYNC` permissions for the monitoring service (Android 14+ passes the `dataSync` type).

#### Fixed
- Previously the monitoring service (`MonitoringService`) was implemented but **never started** — only WorkManager ran in the background (every 15–60 min, deferred by Doze). It is now actually started from a visible activity and after device reboot.

#### Tests
- `./gradlew testDebugUnitTest` — PASS.
- `./gradlew lint` — PASS.

#### Artifacts
- `release-artifacts/LibreCare-2.7.0-debug.apk`
- `release-artifacts/LibreCare-2.7.0-release.apk`
- `release-artifacts/LibreCare-2.7.0-release.aab` (Google Play upload file)

## 2.6.0 - 2026-08-25

### PL

#### Added
- Nowa metryka **CV (współczynnik zmienności)** na ekranie głównym — pokazuje stabilność glikemii z progiem klinicznym „Stabilnie (≤36%)" / „Duża zmienność". Można ją włączać/wyłączać i przestawiać jak pozostałe metryki.

#### Changed
- Wykres historii: dane rysowane gęściej (ok. 1 punkt na 2 px, zgodnie z rekomendacją próbkowania CGM co 5 min), bez nadmiernego przerzedzania.
- Wykres historii: więcej miejsca po prawej stronie, aby dało się dosunąć palec do wartości przy krawędzi.
- Wykres historii: skrajne etykiety osi czasu przeniesione niżej, aby podczas przewijania nie nachodziły na etykiety pośrednie.

#### Fixed
- **Obrót ekranu** (pionowo ↔ poziomo) nie wymusza już ponownego logowania — sesja odblokowania przeżywa zmianę orientacji, a pełne zamknięcie aplikacji nadal wymaga uwierzytelnienia.
- **Przełączanie monitorowanej osoby** nie zamyka już aplikacji — anulowanie poprzedniego zadania i odrzucanie nieaktualnych wyników eliminują wyścig korutyn.
- Wykres historii: wyraźniejsze oznaczenie zmiany dnia — pogrubiona/większa data i pionowa linia w miejscu przejścia na kolejny dzień.

#### Tests
- `GlucoseMetricsCalculatorStatisticsTest` — 3 nowe testy CV (za mało danych → null, płaskie odczyty → 0%, znane odchylenie standardowe).
- `QuickMetricConfigTest`, `RedesignedMetricsTest` — zaktualizowane o kafelek CV.
- `./gradlew testDebugUnitTest` — PASS.

#### Artifacts
- `release-artifacts/LibreCare-2.6.0-debug.apk`
- `release-artifacts/LibreCare-2.6.0-release.apk`
- `release-artifacts/LibreCare-2.6.0-release.aab` (plik do Google Play)

### EN

#### Added
- New **CV (Coefficient of Variation)** metric on the home screen — a glucose stability marker with the clinical threshold "Stable (≤36%)" / "High variability". Toggleable and reorderable like the other metrics.

#### Changed
- History chart: denser rendering (~1 point per 2 px, aligned with the 5-minute CGM sampling recommendation) without over-decimation.
- History chart: more room on the right so a finger can reach edge values.
- History chart: the two edge time-axis labels sit on a lower baseline so they no longer collide with intermediate labels while scrolling.

#### Fixed
- **Screen rotation** (portrait ↔ landscape) no longer forces re-authentication — the unlocked session survives configuration changes, while a genuine app exit still requires unlocking.
- **Switching the monitored person** no longer crashes the app — cancelling the previous job and discarding stale results removes the coroutine race.
- History chart: clearer day-change indicator — bold/larger date and a vertical line at the day boundary.

#### Tests
- `GlucoseMetricsCalculatorStatisticsTest` — 3 new CV tests (not enough data → null, flat readings → 0%, known standard deviation).
- `QuickMetricConfigTest`, `RedesignedMetricsTest` — updated for the CV tile.
- `./gradlew testDebugUnitTest` — PASS.

#### Artifacts
- `release-artifacts/LibreCare-2.6.0-debug.apk`
- `release-artifacts/LibreCare-2.6.0-release.apk`
- `release-artifacts/LibreCare-2.6.0-release.aab` (Google Play upload file)

## 2.5.0 - 2026-08-24

### PL

#### Added
- Zamiast mylącego pytania „przywrócić kopię zapasową?” aplikacja proponuje wczytanie danych, które faktycznie ma, i wypisuje: kto, od kiedy do kiedy, ile dni i ile odczytów.
- Dla każdej osoby pokazywana jest jakość okresu: „dane ciągłe” albo „z przerwami – brakuje ok. X% danych z całego okresu”.
- Pytanie o dodatkowy plik pojawia się dopiero po wczytaniu (lub pominięciu) danych z urządzenia, a okno wyboru pliku otwiera się wyłącznie po wyraźnej zgodzie użytkownika.
- Logowanie odciskiem palca wymaga teraz faktycznego potwierdzenia odciskiem – dopiero po udanej weryfikacji metoda odblokowania zostaje przełączona.
- Możliwość utworzenia klucza dostępu (passkey) przez systemowego menedżera poświadczeń i odblokowania nim aplikacji, z bezpiecznym powrotem do odcisku palca / PIN-u.
- Dodano infrastrukturę testową dla chmurowego uruchamiania jakości: Codespaces, szybki CI, scaffold Firebase Test Lab i dokumentację `docs/testing/`.

#### Changed
- Logowanie jest jednym, bezobsługowym przepływem: sprawdzenie hasła → scalenie zapisanych danych → natychmiastowe pobranie ostatnich 12 godzin → dopisanie świeżych odczytów do pliku danych → ekran główny. Aplikacja nie pyta już, czy połączyć się z kontem LibreLinkUp.
- Po wczytaniu archiwum zawierającego konfigurację aplikacja łączy się z LibreLinkUp samodzielnie i dopisuje wynik do podsumowania.
- Każda synchronizacja, która zapisała nowe odczyty, natychmiast odświeża jeden automatyczny plik danych.
- Scalanie po zalogowaniu jest nieniszczące: identyczne odczyty są scalane, nowe dopisywane, a bieżące wartości mają pierwszeństwo do czasu decyzji użytkownika.

#### Fixed
- Uszkodzony plik danych nie wywraca już startu aplikacji – użytkownik dostaje czytelny komunikat po polsku.
- Pytanie o dane nie pojawia się po ręcznym zalogowaniu, bo dane zostały już scalone w trakcie logowania.

#### Tests
- `BackupCoverageCalculatorTest` – 6 testów pokrycia okresu, procentu braków i opisów po polsku.
- `StartupRestoreFormatterTest` – 5 testów treści okna oferty i podsumowania rozbieżności.
- `PasskeyRequestFactoryTest` – 7 testów kontraktu WebAuthn.
- `AppDataBackupRepositoryTest` – 8 nowych testów oferty danych i scalania po zalogowaniu.
- `./gradlew testDebugUnitTest` – PASS (402 testy, 0 błędów)
- `./gradlew lint` – PASS
- `./gradlew assembleDebug` / `assembleRelease` / `bundleRelease` – PASS

#### Artifacts
- `release-artifacts/LibreCare-2.5.0-debug.apk` – 23 678 597 B
- `release-artifacts/LibreCare-2.5.0-release.apk` – 3 490 239 B
- `release-artifacts/LibreCare-2.5.0-release.aab` – 6 325 791 B (plik do Google Play)

### EN

#### Added
- The startup question now offers the data LibreCare actually holds and lists who, from when to when, how many days and how many readings.
- Per person quality is shown: continuous data or "with gaps - about X% of the period is missing".
- The extra-file question is asked only after the stored data step, and the file picker opens only after an explicit yes.
- Enabling fingerprint unlock now requires a real fingerprint check first.
- Passkeys can be created through the system credential manager and used to unlock the app, with a safe fallback to fingerprint / PIN.

#### Changed
- Login is one non-interactive flow: verify password, merge stored data, download the last 12 hours, write the fresh readings back into the data file, open Home. The app no longer asks whether to connect to LibreLinkUp.
- Every sync that stored new readings refreshes the single automatic data file immediately.

#### Fixed
- A corrupted data file no longer breaks startup; a clear Polish message is shown instead.

## 2.4.0 - 2026-08-23

### PL

#### Added
- Jedna automatyczna kopia zapasowa w katalogu danych aplikacji (`files/backup/librecare-backup.json`). Użytkownik nie wybiera miejsca, nazwy ani hasła.
- Kopia obejmuje wyłącznie osoby widoczne po zalogowaniu (dane LIVE) oraz konfigurację aplikacji i sesję, dzięki czemu można przenieść całe ustawienie na inny telefon.
- Integracja z systemowym przenoszeniem danych: `allowBackup`, reguły `backup_rules.xml` / `data_extraction_rules.xml` oraz `LibreCareBackupAgent`. Działa z Samsung Smart Switch, kopią Google One i transferem przewodowym — uniwersalnie na dowolnym telefonie z Androidem.
- Zapis kopii w chmurze i udostępnianie pliku (SAF `CREATE_DOCUMENT` + systemowy arkusz udostępniania przez FileProvider).
- Scalanie przy wczytywaniu: identyczne odczyty są scalane po cichu, nowe zakresy dat są dopisywane, a podsumowanie mówi wprost np. „Anna: wczytano 3 dni (288 odczytów) między 18.08.2026 a 20.08.2026”.
- Pytanie o rozstrzygnięcie różnic: gdy ten sam znacznik czasu ma inną wartość, aplikacja pyta, czy zachować dane bieżące, czy z archiwum.
- Obsługa menedżerów haseł (Google, Samsung Pass i inne) na ekranie logowania — autofill dla pola e-mail i hasła oraz propozycja zapisania hasła po zalogowaniu.
- Blokada aplikacji odciskiem palca, PIN-em, wzorem lub hasłem ekranu blokady (ekran `Prywatność i dane`).
- Dynamiczne zakresy wykresu: 1g, 3g, 6g, 9g, 12g, 24g, 3d, 7d, 14d, 1m, 3m, 6m, 12m — widoczne są zakresy możliwe do wyświetlenia plus dokładnie jeden wyszarzony „następny”.
- Informacja „Pojawiły się nowe dane · Pokaż najnowsze” zamiast automatycznego przewijania wykresu.

#### Changed
- Format kopii podniesiony do wersji 3 (czytelny JSON bez hasła, z sumą kontrolną). Odczyt starszych formatów (v1, v2 zaszyfrowany hasłem, warianty ze skróconymi kluczami) pozostaje wspierany.
- Zapis kopii jest atomowy (plik tymczasowy + weryfikacja odczytu + kopia poprzednia), więc przerwany zapis nie niszczy poprzedniej kopii.
- Suwak pod wykresem: jedno przesunięcie palcem przewija cały zakres; dotknięcie ścieżki przeskakuje do wskazanego miejsca; wysokość zwiększona do 40 dp.
- Napisy „Aktualna glikemia” i „Trend” są w jednej linii.
- Metryki mają takie same zaokrąglone obwoluty jak przyciski zakresów oraz podpowiedź „przesuń w bok, aby zobaczyć więcej ›”.
- Nieczytelny wiersz `Dane: 12g · 24g 16g 54m` zastąpiony opisem „Zapisana historia: …” i „Zakres 24g będzie dostępny za ok. …”.
- Wykres domowy korzysta z pełnej historii z bazy, a nie tylko z historii bieżącego odczytu.

#### Fixed
- Naprawiono wczytywanie kopii zapasowych — zarówno starych, jak i nowych. Dekoder jest odporny na uszkodzone wiersze, brakujące pola i błędne typy, a wszystkie komunikaty błędów są po polsku.
- Odświeżanie w tle nie przenosi już użytkownika do najnowszych danych.
- Linia wykresu biegnie od lewej do prawej krawędzi (interpolacja na granicy okna) i jest przerywana wyłącznie tam, gdzie faktycznie brakuje danych.
- Suwak wykresu nie resetuje już gestu przy każdym zdarzeniu (przyczyna „skoku o kilka pikseli”).

#### Tests
- `./gradlew clean` — PASS
- `./gradlew testDebugUnitTest` — PASS (376 testów)
- `./gradlew lint` — PASS
- `./gradlew assembleDebug` — PASS
- `./gradlew assembleRelease` — PASS
- `./gradlew bundleRelease` — PASS
- Testy podłączone: `No connected device/emulator available.`

#### Artifacts
- `release-artifacts/LibreCare-2.4.0-debug.apk` (23 629 423 B)
- `release-artifacts/LibreCare-2.4.0-release.apk` (3 420 808 B)
- `release-artifacts/LibreCare-2.4.0-release.aab` (6 171 933 B)

### EN

#### Added
- One automatic backup file inside the app data directory (`files/backup/librecare-backup.json`). No location, file name or password is requested from the user.
- The backup only contains the people visible after login (LIVE data) plus the app configuration and session, so a whole setup can be moved to another phone.
- System transfer integration: `allowBackup`, `backup_rules.xml` / `data_extraction_rules.xml` and `LibreCareBackupAgent`. Works with Samsung Smart Switch, Google One backup and cable transfer - universally on any Android phone.
- Cloud save and share (SAF `CREATE_DOCUMENT` plus the system share sheet through FileProvider).
- Merge on restore: identical readings are merged silently, new date ranges are appended and the report states exactly what was loaded.
- Conflict question: when the same timestamp holds a different value, the user chooses current or archived data.
- Password manager support on the login screen (autofill for e-mail and password, save prompt after login).
- App lock with fingerprint, PIN, pattern or screen lock password.
- Dynamic chart ranges: 1h ... 12 months, with exactly one greyed out preview range.
- "New data available - show latest" indicator instead of auto scrolling.

#### Changed
- Backup format raised to version 3 (plain, checksummed JSON, no password). Reading legacy v1/v2 files remains supported.
- Backup writing is atomic and verified, keeping the previous good copy.
- Navigator slider scrolls the whole range in a single swipe, supports tap-to-jump and is 40 dp high.
- "Aktualna glikemia" and "Trend" captions share one line.
- Metric tiles use the same rounded chip container as the range buttons and show a scroll affordance.
- The cryptic data coverage line was replaced by a readable sentence.
- The dashboard chart now uses the full database history.

#### Fixed
- Restoring old and new backups works again; the decoder tolerates broken rows and reports Polish error messages.
- Background refresh no longer moves the user to the newest data.
- The chart line spans the full chart width and only breaks on real sensor gaps.
- The navigator gesture is no longer restarted on every drag event.

#### Artifacts
- `release-artifacts/LibreCare-2.4.0-debug.apk` (23 629 423 B)
- `release-artifacts/LibreCare-2.4.0-release.apk` (3 420 808 B)
- `release-artifacts/LibreCare-2.4.0-release.aab` (6 171 933 B)

## 2.3.0 - 2026-08-23

### PL

#### Added
- Dodano etap podgladu kopii przed przywroceniem: lista osob, liczba odczytow, wybor przywrocenia ustawien oraz tryb `Polacz dane` / `Zastap lokalne`.
- Dodano selektywny eksport kopii: wybor osob i ustawien przed uruchomieniem systemowego zapisu pliku.
- Dodano pasek `Time in Range` pod wykresem Home (`Poniżej | W zakresie | Powyżej`) z cienkim paskiem segmentowym.

#### Changed
- Przywracanie kopii nie usuwa globalnie danych LIVE; operuje selektywnie na wybranych osobach i trybie merge/replace.
- Metryki Home sa liczone z aktualnie widocznego viewportu wykresu (po przesunieciu okna).
- Dodano long-press drag metryk Home do zmiany kolejnosci i zapisu lokalnego.

#### Fixed
- Ograniczono restore do bezpiecznych danych oraz usunieto tylko dane demo podczas przywracania.
- Zachowano kompatybilnosc ze starszymi kopiami legacy przy jednoczesnym szyfrowaniu nowego formatu.

#### Tests
- `./gradlew clean` — PASS
- `./gradlew testDebugUnitTest` — PASS
- `./gradlew lint` — PASS
- `./gradlew assembleDebug` — PASS
- `./gradlew assembleRelease` — PASS
- `./gradlew bundleRelease` — PASS
- Testy podlaczone: `No connected device/emulator available.`

#### Artifacts
- `release-artifacts/LibreCare-2.3.0-debug.apk` (23 545 943 B)
- `release-artifacts/LibreCare-2.3.0-release.apk` (3 194 389 B)
- `release-artifacts/LibreCare-2.3.0-release.aab` (5 895 399 B)

### EN

#### Added
- Added restore preview before commit: person list, reading counts, settings toggle, and per-person `Merge` / `Replace` mode.
- Added selective backup export flow: choose people/settings before launching system file save.
- Added a compact `Time in Range` row and stacked bar under the Home chart.

#### Changed
- Backup restore no longer wipes all LIVE data; it applies selected people with merge/replace semantics.
- Home metrics now use the currently visible chart viewport window.
- Added long-press drag reordering for Home metrics with local persistence.

#### Fixed
- Restore pipeline now removes demo rows only, preserving unrelated LIVE data.
- Kept legacy backup compatibility while maintaining encrypted secure backup format.

#### Tests
- `./gradlew clean` — PASS
- `./gradlew testDebugUnitTest` — PASS
- `./gradlew lint` — PASS
- `./gradlew assembleDebug` — PASS
- `./gradlew assembleRelease` — PASS
- `./gradlew bundleRelease` — PASS
- Connected tests: `No connected device/emulator available.`

#### Artifacts
- `release-artifacts/LibreCare-2.3.0-debug.apk` (23,545,943 B)
- `release-artifacts/LibreCare-2.3.0-release.apk` (3,194,389 B)
- `release-artifacts/LibreCare-2.3.0-release.aab` (5,895,399 B)

## 2.2.3 - 2026-08-21

### PL

#### Added
- Dodano testy regresyjne dla przywracania kopii zapasowej z uszkodzonym JSON i niezgodnym typem pol.

#### Changed
- Ustawiono domene nawigatora wykresu Home na stale 24h, aby okno 12h zajmowalo dokladnie polowe suwaka.
- Zageszczono opisy osi czasu na Home (5 etykiet) przy zachowaniu formatu `godz:min`.
- Rozszerzono domyslne okno danych Home do 24h, aby przelacznik `24h` mial kompletne dane zrodlowe.

#### Fixed
- Naprawiono blad przywracania kopii zapasowej typu `cannot be cast to k3` przez obsluge bledow parsowania JSON i niezgodnych typow.
- Dodano bezpieczne pomijanie uszkodzonych rekordow backupu (osoby/odczyty/ustawienia pacjenta) z logowaniem diagnostycznym.

#### Tests
- `./gradlew clean` — PASS
- `./gradlew testDebugUnitTest` — PASS
- `./gradlew lint` — PASS
- `./gradlew assembleDebug` — PASS
- `./gradlew assembleRelease` — PASS
- `./gradlew bundleRelease` — PASS
- Testy podlaczone: brak urzadzenia/emulatora (`adb` niedostepne w PATH).

#### Artifacts
- `release-artifacts/LibreCare-2.2.3-debug.apk` (23 480 386 B)
- `release-artifacts/LibreCare-2.2.3-release.apk` (3 178 014 B)
- `release-artifacts/LibreCare-2.2.3-release.aab` (5 814 564 B)

### EN

#### Added
- Added regression tests for backup restore with malformed JSON and mismatched field types.

#### Changed
- Set Home chart navigator domain to a fixed 24h window so the 12h viewport equals half of the track.
- Increased Home time-axis density (5 labels) while keeping `HH:mm` formatting.
- Expanded the default Home source window to 24h so the `24h` selector uses complete source data.

#### Fixed
- Fixed backup restore `cannot be cast to k3` failures by handling invalid JSON and incompatible payload shapes.
- Added safe skip behavior for corrupted backup rows (people/readings/patient settings) with diagnostic logs.

#### Tests
- `./gradlew clean` — PASS
- `./gradlew testDebugUnitTest` — PASS
- `./gradlew lint` — PASS
- `./gradlew assembleDebug` — PASS
- `./gradlew assembleRelease` — PASS
- `./gradlew bundleRelease` — PASS
- Connected tests: no device/emulator available (`adb` not present in PATH).

#### Artifacts
- `release-artifacts/LibreCare-2.2.3-debug.apk` (23,480,386 B)
- `release-artifacts/LibreCare-2.2.3-release.apk` (3,178,014 B)
- `release-artifacts/LibreCare-2.2.3-release.aab` (5,814,564 B)

## 2.2.2 - 2026-08-21

### PL

#### Changed
- Wydanie stabilizacyjne – brak zmian funkcjonalnych.

#### Tests
- `./gradlew testDebugUnitTest` — PASS
- `./gradlew lint` — PASS
- `./gradlew assembleDebug` — PASS
- `./gradlew assembleRelease` — PASS
- `./gradlew bundleRelease` — PASS
- Testy podłączone: brak urządzenia/emulatora.

#### Artifacts
- `release-artifacts/LibreCare-2.2.2-debug.apk` (22,39 MB)
- `release-artifacts/LibreCare-2.2.2-release.apk` (3,03 MB)
- `release-artifacts/LibreCare-2.2.2-release.aab` (5,55 MB)

### EN

#### Changed
- Stability release – no functional changes.

#### Tests
- `./gradlew testDebugUnitTest` — PASS
- `./gradlew lint` — PASS
- `./gradlew assembleDebug` — PASS
- `./gradlew assembleRelease` — PASS
- `./gradlew bundleRelease` — PASS
- Connected tests: no device/emulator available.

#### Artifacts
- `release-artifacts/LibreCare-2.2.2-debug.apk` (22.39 MB)
- `release-artifacts/LibreCare-2.2.2-release.apk` (3.03 MB)
- `release-artifacts/LibreCare-2.2.2-release.aab` (5.55 MB)

---

## 2.2.1 - 2026-08-21

### PL

#### Added
- Dodano tekstowy score UX do raportu `ui-audit-report.md`, wraz ze zbiorczą oceną i flagami ryzyka dla wąskich viewportów oraz większego `fontScale`.
- Rozszerzono Demo Mode do 5 osób, aby testować przełącznik monitorowanych osób i scenariusze overflow `+N`.

#### Changed
- Przebudowano nagłówek Home: po lewej `LibreCare` z wersją i skrótem bazy danych, po prawej `Ostatni odczyt`, `Koniec działania sensora za` oraz `Zakres danych`.
- Sekcja `Historia glikemii` pokazuje teraz skrót typu `Okno 3h · baza 7d` zamiast mylącego opisu sugerującego, że baza kończy się na ~12h.
- Wiersz dostępności danych pod wykresem Home rozszerzono do zakresów `12h / 3d / 7d / 30d`.
- Metryki Home przeniesiono niżej pod wykres i zmieniono na jedną przewijaną linię, z konfiguracją widoczności `show/hide` w ustawieniach.
- Przełącznik osób na Home pokazuje teraz do 2 szerokich boksów, a kolejne osoby grupuje w `+N` z rozwijaną przewijaną listą ostatnio używanych osób.

#### Fixed
- Usunięto dodatkowy dolny `Slider` pod wykresem Home; pozostawiono tylko właściwy navigator viewportu.
- Naprawiono geometrię navigatora Home: szerokość uchwytu odpowiada wybranemu oknu `1h / 3h / 6h / 9h / 12h`, startuje przy prawej krawędzi i przesuwa się płynnie w obrębie 12 godzin.
- Poprawiono etykiety osi X na Home, aby dla zakresów godzinowych pokazywały dzień i godzinę, a nie samą godzinę bez kontekstu dnia.
- Zmniejszono ryzyko clippingu etykiet czasu pod wykresem przez większy dolny padding osi i mniejszą liczbę etykiet na Home.

#### Tests
- Zweryfikowano: `./gradlew clean`, `./gradlew testDebugUnitTest`, `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`.
- Dodatkowo uruchomiono regresje dla: `UiAuditExporterTest`, `HomeChartModelsTest`, `PolishFormattersTest`, `DataCoverageModelTest`, `RedesignedMetricsTest`.
- Testy podłączone: brak urządzenia/emulatora (`adb` niedostępne w PATH).

#### Artifacts
- `release-artifacts/LibreCare-2.2.1-debug.apk` (23 464 012 B)
- `release-artifacts/LibreCare-2.2.1-release.apk` (3 178 011 B)
- `release-artifacts/LibreCare-2.2.1-release.aab` (5 808 931 B)

### EN

#### Added
- Added a textual UX score to `ui-audit-report.md`, including a global summary and risk flags for narrow viewports and larger `fontScale` values.
- Expanded Demo Mode to 5 people so the monitored-person switcher and `+N` overflow can be exercised properly.

#### Changed
- Reworked the Home header: left side shows `LibreCare` with version and database span, right side shows `Last reading`, `Sensor ends in`, and `Data range`.
- `Glucose history` on Home now shows a compact summary such as `Window 3h · DB 7d` instead of implying that local history only goes back ~12h.
- Expanded the Home data-availability row to `12h / 3d / 7d / 30d`.
- Moved Home metrics below the chart and rebuilt them into one horizontally scrollable row with `show/hide` configuration in settings.
- The Home person switcher now shows up to 2 wide boxes and groups additional people under a `+N` expandable, horizontally scrollable list ordered by recent use.

#### Fixed
- Removed the extra bottom `Slider` under the Home chart and kept only the actual viewport navigator.
- Fixed Home navigator geometry so its width matches the selected `1h / 3h / 6h / 9h / 12h` window, starts at the right edge, and pans smoothly within the 12-hour domain.
- Improved Home X-axis labels so hourly windows show both day and time rather than time alone.
- Reduced the risk of time-label clipping below the Home chart with larger bottom axis padding and fewer labels.

#### Tests
- Verified: `./gradlew clean`, `./gradlew testDebugUnitTest`, `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`.
- Additionally ran regressions for: `UiAuditExporterTest`, `HomeChartModelsTest`, `PolishFormattersTest`, `DataCoverageModelTest`, `RedesignedMetricsTest`.
- Connected tests: no device/emulator available (`adb` not present in PATH).

#### Artifacts
- `release-artifacts/LibreCare-2.2.1-debug.apk` (23,464,012 B)
- `release-artifacts/LibreCare-2.2.1-release.apk` (3,178,011 B)
- `release-artifacts/LibreCare-2.2.1-release.aab` (5,808,931 B)

## 2.2.0 - 2026-08-21

### PL

#### Added
- Dodano niezależny dla Home selektor wykresu `1h / 3h / 6h / 9h / 12h`.
- Dodano Home navigator czasu pod wykresem, umożliwiający przesuwanie widocznego okna w obrębie dostępnych 12 godzin.
- Dodano testy `AppNavigationStateTest` i `HomeChartModelsTest` dla back stacku oraz logiki viewportu Home.

#### Changed
- Przebudowano Home pod kątem czytelności zamiast dalszej kompresji: większa typografia, bezpieczne zawijanie wierszy i większe sekcje tam, gdzie content tego wymaga.
- Zmieniono blok aktualnej glikemii na responsywny układ, który przenosi trend do kolejnej linii zamiast zmniejszać font lub ściskać elementy.
- Przebudowano metryki do układu adaptacyjnego: na węższych szerokościach `Poniżej / W zakresie / Powyżej` w pierwszym rzędzie oraz `GMI / HbA1c` w drugim.
- Sekcja wykresu Home pokazuje teraz oddzielnie dostępność danych `12 h / 24 h`, niezależnie od zakresu `Historia`.
- Rooty `Główna / Historia / Ustawienia` działają jak top-level destinations, z ujednoliconą dolną nawigacją i bez back arrow na ekranach root.

#### Fixed
- Usunięto overlapy i clipping w Home spowodowane przez zbyt sztywne `Row`, zbyt małe fonty i zbyt gęste upychanie metryk w jednym wierszu.
- Naprawiono clipping osi Y/X wykresu dzięki adaptacyjnemu gutterowi, większym etykietom osi i bezpiecznemu pozycjonowaniu labeli czasu.
- Zmieniono neutralną etykietę trendu `FLAT` z `Stabilnie` na `Bez zmian`.
- Naprawiono back stack tak, aby `navigateBack()` wracał do rzeczywistego parent screen zamiast do hardcoded route.

#### Tests
- Zweryfikowano: `./gradlew clean`, `./gradlew testDebugUnitTest`, `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`.
- Dodatkowo uruchomiono wybrane testy regresji dla: nawigacji, viewportu Home, metryk, osi wykresu, severity i trendów.
- Testy podłączone: brak urządzenia/emulatora (`adb` niedostępne w PATH).

#### Artifacts
- `release-artifacts/LibreCare-2.2.0-debug.apk` (23 447 625 B)
- `release-artifacts/LibreCare-2.2.0-release.apk` (3 161 630 B)
- `release-artifacts/LibreCare-2.2.0-release.aab` (5 797 368 B)

### EN

#### Added
- Added a Home-only `1h / 3h / 6h / 9h / 12h` chart range selector.
- Added a small Home time navigator below the chart so the visible window can be panned within the available 12-hour timeline.
- Added `AppNavigationStateTest` and `HomeChartModelsTest` for back-stack and Home viewport logic.

#### Changed
- Reworked Home for readability rather than further compression: larger typography, wrapping rows, and taller sections where the content needs it.
- Rebuilt the current-glucose block into a responsive layout that moves trend information to the next line instead of shrinking fonts or squeezing content.
- Rebuilt quick metrics into an adaptive layout: on narrow widths `Below / In range / Above` on row one and `GMI / HbA1c` on row two.
- Home chart now shows separate `12 h / 24 h` data availability, independent from the `History` range.
- `Home / History / Settings` behave as top-level destinations with unified bottom navigation and no root-level back arrow.

#### Fixed
- Removed Home overlaps and clipping caused by rigid `Row` layouts, undersized fonts, and too many metrics forced into a single line.
- Fixed chart Y/X-axis clipping via adaptive gutter sizing, larger axis labels, and safer time-label placement.
- Replaced the positive-sounding flat-trend label `Stable` with neutral `No change` semantics in Polish (`Bez zmian`).
- Fixed back stack behavior so back navigation returns to the actual parent screen instead of a hardcoded route.

#### Tests
- Verified: `./gradlew clean`, `./gradlew testDebugUnitTest`, `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`.
- Additionally ran focused regression tests for navigation, Home viewport logic, metrics, chart axes, severity, and trends.
- Connected tests: no device/emulator available (`adb` not present in PATH).

#### Artifacts
- `release-artifacts/LibreCare-2.2.0-debug.apk` (23,447,625 B)
- `release-artifacts/LibreCare-2.2.0-release.apk` (3,161,630 B)
- `release-artifacts/LibreCare-2.2.0-release.aab` (5,797,368 B)

## 2.1.1 - 2026-08-20

### PL

#### Added
- Dodano kompaktowy wiersz zakresu na Home: `24h · dane do HH:mm` + akcja `Historia >`.

#### Changed
- Przebudowano WYŁĄCZNIE layout Home: bardziej zwarty nagłówek, gęstsza hierarchia glikemii, szybszy start sekcji historii.
- Powiązano strzałkę trendu bezpośrednio z wartością glikemii i usunięto duplikację znaczenia status/trend.
- Spłaszczono metryki (4 kolumny, fallback 2x2 dla wąskich ekranów), a `Edytuj` przeniesiono inline.
- Zmniejszono narzut osi wykresu na Home (mniej etykiet, ciaśniejsze paddingi osi) bez zmiany danych/progów.

#### Fixed
- Ograniczono clipping kluczowych wartości metryk przy szerokości ~384dp.
- Podniesiono pozycję startu sekcji `Historia glikemii`, aby większa część wykresu była widoczna na pierwszym ekranie.

#### Tests
- Zweryfikowano: `./gradlew clean`, `./gradlew testDebugUnitTest`, `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`.
- Testy podłączone: brak urządzenia/emulatora (`adb` niedostępne w PATH).

#### Artifacts
- `release-artifacts/LibreCare-2.1.1-debug.apk` (23 414 859 B)
- `release-artifacts/LibreCare-2.1.1-release.apk` (3 161 633 B)
- `release-artifacts/LibreCare-2.1.1-release.aab` (5 765 417 B)

### EN

#### Added
- Added a compact Home range row: `24h · data until HH:mm` + `History >` action.

#### Changed
- Reworked ONLY the Home layout: denser header, tighter glucose hierarchy, earlier history section start.
- Bound trend arrow directly to current glucose value and removed duplicated status/trend meaning.
- Flattened quick metrics (4 columns, 2x2 fallback on narrow widths) and moved `Edit` inline.
- Reduced Home chart axis overhead (fewer labels, tighter axis paddings) without changing data/threshold logic.

#### Fixed
- Reduced clipping risk for key metric values around ~384dp width.
- Moved `Glucose history` section higher so a larger chart portion is visible on first viewport.

#### Tests
- Verified: `./gradlew clean`, `./gradlew testDebugUnitTest`, `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`.
- Connected tests: no device/emulator available (`adb` not present in PATH).

#### Artifacts
- `release-artifacts/LibreCare-2.1.1-debug.apk` (23,414,859 B)
- `release-artifacts/LibreCare-2.1.1-release.apk` (3,161,633 B)
- `release-artifacts/LibreCare-2.1.1-release.aab` (5,765,417 B)

## 2.1.0 - 2026-08-20

### PL

#### Added
- Dodano rozdzielone ekrany ustawien: `SettingsMainScreen`, `MonitoringSettingsScreen`, `AccountSettingsScreen`.
- Dodano testy regresji dla backupu i historii: `LocalGlucoseHistoryRepositoryTest`, `FullScreenHistoryViewportTest`.
- Dodano sekcje pokrycia danych per osoba na ekranie `Informacje i statystyki` (14/30/60/90/360 dni).

#### Changed
- Zwiekszono dojrzalosc przeplywu backup/przywracanie danych LIVE + ustawienia w `Prywatnosc i dane`.
- Wykres renderuje sie plynniej przy rzadszych punktach dzieki interpolacji minutowej na warstwie prezentacji.
- Uporzadkowano nawigacje i separacje sekcji ustawien dla monitoringu i konta.
- Przebudowano UX ustawien monitoringu: `Zakres docelowy`, `Metryki ekranu glownego` i `HbA1c` to osobne destination bez wspolnych tabow.
- Zageszczono Home i Analize/Historia: mniej duplikacji, mniejsze pionowe bloki, bardziej kompaktowy uklad metryk.

#### Fixed
- Utrzymano filtracje rekordow demo podczas eksportu/importu backupu.
- Poprawiono spojnosc danych historii lokalnej po zmianach zakresu i viewportu.
- Dodano jawne dociaganie/scala nieopoznionych punktow z okna 12h podczas synchronizacji.
- Poprawiono kolor trendu na ekranie glownym: przy wysokiej glikemii trend spadkowy jest zielony, wzrostowy ostrzegawczy/krytyczny.

#### Tests
- Zweryfikowano: `./gradlew clean`, `./gradlew testDebugUnitTest`, `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`.
- Testy podlaczone: brak urzadzenia/emulatora (`adb` niedostepne w PATH).

#### Artifacts
- `release-artifacts/LibreCare-2.1.0-debug.apk` (23 414 839 B)
- `release-artifacts/LibreCare-2.1.0-release.apk` (3 145 248 B)
- `release-artifacts/LibreCare-2.1.0-release.aab` (5 759 431 B)

### EN

#### Added
- Added split settings surfaces: `SettingsMainScreen`, `MonitoringSettingsScreen`, `AccountSettingsScreen`.
- Added backup/history regression tests: `LocalGlucoseHistoryRepositoryTest`, `FullScreenHistoryViewportTest`.
- Added per-person data coverage section in `Statistics` (14/30/60/90/360 day windows).

#### Changed
- Increased maturity of the LIVE + settings backup/restore flow in `Privacy & Data`.
- Improved chart smoothness for sparse streams by adding minute-level interpolation in the presentation layer.
- Refined navigation and separation of monitoring/account settings sections.
- Reworked monitoring settings UX: `Target range`, `Home metrics`, and `HbA1c` are now separate destinations without shared tabs.
- Increased Home and History density by reducing duplicated blocks and moving to compact metric layouts.

#### Fixed
- Kept demo-record filtering enforced during backup export/import.
- Improved local history consistency after time-range and viewport updates.
- Added explicit delayed-point backfill merge for the rolling 12-hour sync window.
- Fixed Home trend color semantics: high glucose + falling trend now uses favorable (green) color.

#### Tests
- Verified: `./gradlew clean`, `./gradlew testDebugUnitTest`, `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`.
- Connected tests: no device/emulator available (`adb` unavailable in PATH).

#### Artifacts
- `release-artifacts/LibreCare-2.1.0-debug.apk` (23 414 839 B)
- `release-artifacts/LibreCare-2.1.0-release.apk` (3 145 248 B)
- `release-artifacts/LibreCare-2.1.0-release.aab` (5 759 431 B)

## 2.0.1 - 2026-08-20

### PL

#### Added
- Dodano kopie zapasowe LIVE + ustawienia (bez danych demo) oraz przywracanie z pliku w `Prywatnosc i dane`.

#### Changed
- Zwiekszono granulacje wykresu na ekranie glownym: limit punktow jest teraz dynamiczny i dopasowany do szerokosci wykresu.
- Wprowadzono hybrydowa ciaglosc danych debug/release: debug i release korzystaja z jednego `applicationId`, a debug podpisuje sie kluczem release, gdy jest skonfigurowany.

#### Fixed
- Przywracanie kopii ignoruje rekordy demo i nie pozwala odtworzyc trybu `DEMO` z backupu.

#### Tests
- Dodano `AppDataBackupRepositoryTest`.
- Rozszerzono `GlucoseChartLayoutLogicTest` o przypadek braku downsamplingu przy duzym budzecie punktow.
- Zweryfikowano: `./gradlew clean`, `./gradlew testDebugUnitTest`, `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`.
- Testy podlaczone: brak urzadzenia/emulatora (`adb` niedostepne, `Phone connected (status=device): false`).

#### Artifacts
- `release-artifacts/LibreCare-2.0.1-debug.apk` (23 365 701 B)
- `release-artifacts/LibreCare-2.0.1-release.apk` (3 128 979 B)
- `release-artifacts/LibreCare-2.0.1-release.aab` (5 692 920 B)

### EN

#### Added
- Added LIVE + settings backup (excluding demo data) and restore from file in `Privacy & Data`.

#### Changed
- Increased Home chart granularity: point budget is now dynamic and follows chart width.
- Implemented hybrid debug/release continuity: debug and release now share one `applicationId`, and debug uses release signing when configured.

#### Fixed
- Restore flow now filters out demo records and prevents restoring `DEMO` mode from backup.

#### Tests
- Added `AppDataBackupRepositoryTest`.
- Extended `GlucoseChartLayoutLogicTest` with a high-budget no-downsampling scenario.
- Verified: `./gradlew clean`, `./gradlew testDebugUnitTest`, `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`.
- Connected tests: no device/emulator available (`adb` unavailable, `Phone connected (status=device): false`).

#### Artifacts
- `release-artifacts/LibreCare-2.0.1-debug.apk` (23 365 701 B)
- `release-artifacts/LibreCare-2.0.1-release.apk` (3 128 979 B)
- `release-artifacts/LibreCare-2.0.1-release.aab` (5 692 920 B)

## 1.9.1 - 2026-08-19

### PL

#### Changed
- Dodatkowo skompaktowano górną część Home: top bar ma teraz wspólne tło ekranu, a pasek świeżości danych i status sensora jest bardziej tekstowy i mniej blokowy.
- `TimeRangeDisplay` na Home jest jeszcze lżejszy: tekst „Zakres”, jedna linia informacji i akcja „Historia”.
- Selektor zakresu czasu na ekranie historii nie używa już przycisków / pills; został zastąpiony płaskim selektorem tekstowym z subtelnym underline dla aktywnej opcji.

#### Fixed
- Odzyskano dodatkowe pionowe miejsce w górnej części ekranu.
- Usunięto kolejny obszar, który nadal wizualnie przypominał zestaw chipów/przycisków.

#### Tests
- Dodano `HistorySelectorModelTest`.
- Zweryfikowano pełny build/test/lint dla wersji 1.9.1.

#### Artifacts
- `LibreCare-1.9.1-debug.apk`
- `LibreCare-1.9.1-release.apk`
- `LibreCare-1.9.1-release.aab`

### EN

#### Changed
- Further compacted the Home top area: the top bar now uses the shared screen background, and the freshness/sensor block is more text-first and less block-like.
- `TimeRangeDisplay` on Home is lighter: “Zakres” label, one information line, and a “Historia” action.
- The history time-range selector no longer uses buttons/pills; it is now a flat text selector with a subtle underline for the active option.

#### Fixed
- Recovered additional vertical space in the upper part of the screen.
- Removed another area that still visually resembled a set of chips/buttons.

#### Tests
- Added `HistorySelectorModelTest`.
- Verified full build/test/lint for version 1.9.1.

#### Artifacts
- `LibreCare-1.9.1-debug.apk`
- `LibreCare-1.9.1-release.apk`
- `LibreCare-1.9.1-release.aab`

## 1.9.0 - 2026-08-19

### PL

#### Changed
- Przebudowano layout Home i History tak, aby ekran przestał wyglądać jak stos dużych kart jedna pod drugą.
- Usunięto większość pełnoekranowych `Card` / `Surface` wrapperów z sekcji glikemii, historii i NFZ.
- Quick metrics są teraz prezentowane jako płaski, kompaktowy pas z subtelnymi separatorami zamiast osobnych dużych boxów.
- Sekcja glikemii na ekranie głównym działa bez zewnętrznej karty; hierarchia została oparta o typografię i odstępy.
- Ekran historii używa płaskich sekcji dla wykresu, selected point, zakresów i statystyk.
- Person switcher przestał być zestawem dużych pills; wybrana osoba jest oznaczana bardziej lekko, linią pod nazwą.

#### Fixed
- Odzyskano miejsce pionowe na ekranie przez zmniejszenie liczby dużych kontenerów i redukcję warstw tła.

#### Tests
- Dodano `RedesignedMetricsTest`.
- Zweryfikowano pełny build/test/lint dla wersji 1.9.0.

#### Artifacts
- `LibreCare-1.9.0-debug.apk`
- `LibreCare-1.9.0-release.apk`
- `LibreCare-1.9.0-release.aab`

### EN

#### Changed
- Rebuilt Home and History layout so the app no longer looks like a stack of large rounded cards.
- Removed most full-width `Card` / `Surface` wrappers from glucose, history, and NFZ sections.
- Quick metrics now use a flat compact strip with subtle separators instead of large individual boxes.
- The main glucose section on Home no longer relies on an outer card; hierarchy now comes from typography and spacing.
- History screen now uses flat sections for chart, selected point, range distribution, and stats.
- Person switcher is no longer a row of large pills; the selected person uses a lighter underline treatment.

#### Fixed
- Recovered vertical screen space by reducing large containers and stacked background layers.

#### Tests
- Added `RedesignedMetricsTest`.
- Verified full build/test/lint for version 1.9.0.

#### Artifacts
- `LibreCare-1.9.0-debug.apk`
- `LibreCare-1.9.0-release.apk`
- `LibreCare-1.9.0-release.aab`

## 1.8.1 - 2026-08-19

### PL

#### Fixed
- Naprawiono interpretację znaczników czasu Libre bez jawnej strefy czasowej: są teraz odczytywane w strefie czasowej telefonu, zamiast być wymuszane jako UTC.
- W efekcie wykres, tooltipy i etykiety czasu pokazują lokalny czas telefonu także dla rekordów typu `2026-08-19T12:10:00` lub `2026-07-06 23:10:00`.
- Dodano testy potwierdzające poprawną konwersję dla strefy `Europe/Warsaw`.

#### Tests
- `LibreTimestampParserTest`: rozszerzony o przypadki dla timestampów bez offsetu.
- `PolishFormattersTest`: utrzymane testy formatowania czasu lokalnego.

### EN

#### Fixed
- Fixed Libre timestamps without an explicit timezone: they are now interpreted in the phone timezone instead of being forced to UTC.
- As a result, the chart, tooltips, and user-facing time labels now show the phone's local time for records such as `2026-08-19T12:10:00` and `2026-07-06 23:10:00`.
- Added tests covering `Europe/Warsaw` conversion for naive timestamps.

#### Tests
- `LibreTimestampParserTest`: expanded with naive timestamp scenarios.
- `PolishFormattersTest`: local timezone formatting tests kept green.

## 1.8.0 - 2026-08-19

### PL

#### Naprawiono
- **Wykres – przepełnienie osi Y**: dane > 420 mg/dL wychodziły poza obszar wykresu. Usunięto twarde ograniczenie `coerceAtMost(420)`.
- **Statystyki – 0% w zakresie**: `toInt()` ucinał wartości < 1%; zmieniono na `roundToInt()`.
- **Brak lokalnego odliczania pokrycia**: `computeDataCoverage` używało `newest - oldest` zamiast `now - oldest`; licznik teraz biegnie bez sieci.
- **Brak auto-odświeżania czasu**: „chwilę temu", „Sensor: X dni" nie aktualizowały się między synchronizacjami.

#### Dodano
- Lokalny ticker 30 s w `MonitoringScreen` i `FullScreenGlucoseChartScreen` (bez żądań sieciowych).
- Parametr `now: Instant` w `computeDataCoverage`, `RedesignedCurrentGlucoseCard`, `GlucoseChartCard`.

#### Testy
- Zaktualizowano `DataCoverageModelTest` – przekazywanie `now` wprost dla determinizmu.
- Wszystkie 274 testy jednostkowe: PASS.

#### Artefakty
- `LibreCare-1.8.0-debug.apk` (~22,1 MB)
- `LibreCare-1.8.0-release.apk` (~2,9 MB)
- `LibreCare-1.8.0-release.aab` (~5,4 MB)

---

### EN

#### Fixed
- **Chart Y-axis overflow**: readings > 420 mg/dL were plotted outside chart bounds. Removed `coerceAtMost(420)` hard cap.
- **Statistics – 0% in-range**: `toInt()` truncated sub-1% values; changed to `roundToInt()`.
- **Coverage countdown not ticking**: `computeDataCoverage` used `newest - oldest` instead of `now - oldest`; countdown now runs locally.
- **No local time refresh**: "chwilę temu", sensor remaining time did not update between network syncs.

#### Added
- 30-second local ticker in `MonitoringScreen` and `FullScreenGlucoseChartScreen` (no network requests).
- `now: Instant` parameter in `computeDataCoverage`, `RedesignedCurrentGlucoseCard`, `GlucoseChartCard`.

#### Tests
- Updated `DataCoverageModelTest` – now passes `now` explicitly for determinism.
- All 274 unit tests: PASS.

#### Artifacts
- `LibreCare-1.8.0-debug.apk` (~22.1 MB)
- `LibreCare-1.8.0-release.apk` (~2.9 MB)
- `LibreCare-1.8.0-release.aab` (~5.4 MB)

---


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

