# LibreCare 2.13.0 — Release Notes

**Data wydania / Release date**: 2026-08-27  
**versionCode**: 39  
**versionName**: 2.13.0  
**Poprzednia wersja / Previous version**: 2.12.0 (versionCode 38)

---

## 🇵🇱 Polski

### Nowe funkcje

- **Regulowany zakres analizy (1–90 dni)** — Sekcja overlay wykresu dobowego ma nową kontrolkę pozwalającą wpisać dowolną liczbę dni (1–90). Domyślnie 14 dni jak dotychczas.
- **Nawigacja okna nakładki** — Pięć przycisków (◀◀ Miesiąc, ◀ Tydzień, Dzisiaj, Tydzień ▶, Miesiąc ▶▶) pozwalają przesuwać okno nakładki w czasie niezależnie od okna wykresu słupkowego.
- **Wyświetlanie zakresu nakładki** — Nad wykresem pojawia się etykieta z aktualnym zakresem dat i liczbą dni, np. "Zakres: 24.08 — 06.09 (14 dni)".
- **Zachowanie pozycji przewijania tabeli metryk** — Pozioma pozycja tabeli metryk jest zapamiętywana podczas nawigacji między ekranami.

### Zmiany UI

- Sekcja obserwacji: nagłówek zmieniony na "⭐ WNIOSKI & OBSERWACJE", czcionka zwiększona z 12sp do 14sp, odstęp między liniami 20sp — lepsza czytelność dla opiekunów i lekarzy.
- Tytuł wykresu dobowego zmieniony na dynamiczny: "Profil dobowy (nakładka N dni)".
- Tabela metryk ma teraz stałą lewą kolumnę z nazwami metryk — przy poziomym przewijaniu nazwy pozostają widoczne.

### Testy

- Nowe testy w `AnalysisChartFactoryTest`:
  - `overlayForWindow_7days_limitsToSevenDayLines`
  - `overlayForWindow_30days_acceptsFullRange`
  - `overlayForWindow_emptyReadings_returnsEmpty`
  - `maxDailyOffset_withShortPeriod_returnsLargerMax`
- 491 testów, 0 błędów.

### Artefakty

- `release-artifacts/LibreCare-2.13.0-debug.apk` (22,77 MB)
- `release-artifacts/LibreCare-2.13.0-release.apk` (3,52 MB)
- `release-artifacts/LibreCare-2.13.0-release.aab` (6,39 MB)

### Znane ograniczenia

- Testy connected (na urządzeniu/emulatorze) nie zostały uruchomione — brak podłączonego urządzenia/emulatora.
- Widok stickyness kolumny metryk działa poprawnie na ekranach kompaktowych; na bardzo wąskich ekranach (< 320dp) szerokość 112dp może być ciasna.

---

## 🇬🇧 English

### New Features

- **Variable analysis period (1–90 days)** — The overlay section now has an input field for selecting any number of days from 1 to 90. Default is 14 days (unchanged from previous behaviour).
- **Overlay window navigation** — Five navigation buttons (◀◀ Month, ◀ Week, Today, Week ▶, Month ▶▶) allow shifting the overlay window in time independently of the bar chart window.
- **Overlay range label** — A label showing the current date range and day count appears above the overlay chart, e.g. "Zakres: 24.08 — 06.09 (14 dni)".
- **Metrics table scroll position preservation** — The horizontal scroll position of the metrics table is now saved and restored when navigating back to the Analytics screen.

### UI Changes

- Observations section: header updated to "⭐ WNIOSKI & OBSERWACJE", font size increased from 12sp to 14sp with 20sp line height — improved readability for caregivers and clinicians.
- Daily profile chart title is now dynamic: "Profil dobowy (nakładka N dni)".
- Metrics table now uses a sticky left column — metric labels stay visible while scrolling right.

### Tests

- New tests added to `AnalysisChartFactoryTest`:
  - `overlayForWindow_7days_limitsToSevenDayLines`
  - `overlayForWindow_30days_acceptsFullRange`
  - `overlayForWindow_emptyReadings_returnsEmpty`
  - `maxDailyOffset_withShortPeriod_returnsLargerMax`
- 491 tests, 0 failures.

### Artifacts

- `release-artifacts/LibreCare-2.13.0-debug.apk` (22.77 MB)
- `release-artifacts/LibreCare-2.13.0-release.apk` (3.52 MB)
- `release-artifacts/LibreCare-2.13.0-release.aab` (6.39 MB)

### Known Limitations

- Connected tests (device/emulator) were not run — no connected device/emulator available.
- Metrics table sticky column works correctly on compact screens; on very narrow screens (< 320dp) the 112dp fixed width may feel tight.

