# Changelog

All notable changes to LibreCare will be documented in this file.

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

