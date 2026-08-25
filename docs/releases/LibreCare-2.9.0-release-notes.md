# LibreCare 2.9.0 – Release Notes

**Date:** 2026-08-25
**versionName:** 2.9.0 · **versionCode:** 31 (poprzednio 2.8.0 / 30)

---

## PL

### Poprawki
- **Wczytanie kopii nie wymusza już ponownego logowania.** `AppLaunchResolver.resolve` idzie do MONITORING, gdy istnieje sesja **lub** zapisane dane logowania; ekran logowania pojawia się tylko przy braku obu (spójne z `SettingsRepository.shouldShowLoginForm`).

### Zmiany UI
- **Drag & drop metryk** (ekran główny + Ustawienia): przytrzymaj i przeciągnij — kafelek/wiersz się „unosi" (skala + cień + lekkie wysunięcie), upuszczenie zmienia kolejność. Usunięto strzałki w Ustawieniach.
- **Widok poziomy — selektor zakresu w lewej kolumnie**: chipy 1g/3g/… pod wartością glikemii i zaleceniami, nad kartą NFZ. Zmiana zakresu natychmiast odświeża wykres (prawa kolumna) i metryki (pełna szerokość).
- **Auto-scroll po obrocie**: wejście w orientację poziomą przewija ekran do sekcji „Historia glikemii".

### Testy
- `AppLaunchResolverTest` zaktualizowany; `./gradlew testDebugUnitTest` — PASS.

### CI
- Workflow Firebase Test Lab: macierz 3 telefonów wirtualnych (SMALL/STANDARD/LARGE) — bez zmian w tym wydaniu, potwierdzone jako gotowe.

### Znane ograniczenia
- Animacja „ucieczki" sąsiedniego kafla podczas przenoszenia jest uproszczona (natychmiastowe przełożenie), aby zachować dostępność (wszystkie metryki obecne w drzewie semantyki). Kafelek przeciągany ma pełną animację uniesienia.
- Ekran „Analiza" + eksport surowych danych do Excela — kolejne wydania.

### Artefakty
- `release-artifacts/LibreCare-2.9.0-debug.apk`
- `release-artifacts/LibreCare-2.9.0-release.apk`
- `release-artifacts/LibreCare-2.9.0-release.aab` (plik do Google Play)

---

## EN

### Fixed
- **Restoring a backup no longer forces re-login.** `AppLaunchResolver.resolve` goes to MONITORING when a session **or** stored credentials exist; the login screen only appears when both are missing (aligned with `SettingsRepository.shouldShowLoginForm`).

### UI changes
- **Drag & drop metrics** (home + Settings): long-press and drag — the tile/row lifts (scale + shadow + slight pop-out); dropping reorders. Arrow buttons removed from Settings.
- **Landscape — range selector in the left column**: 1h/3h/… chips under the glucose value/recommendations, above the NFZ card. Changing the range immediately refreshes the chart (right column) and metrics (full width).
- **Auto-scroll on rotation**: entering landscape scrolls to the "Historia glikemii" section.

### Tests
- `AppLaunchResolverTest` updated; `./gradlew testDebugUnitTest` — PASS.

### CI
- Firebase Test Lab 3 virtual-phone matrix (SMALL/STANDARD/LARGE) — unchanged this release, confirmed ready.

### Known limitations
- The neighbour "escape" animation while dragging is simplified (instant reorder) to preserve accessibility (all metric texts present in the semantics tree). The dragged tile has the full lift animation.
- The "Analysis" screen + raw-data Excel export are planned for later releases.

### Artifacts
- `release-artifacts/LibreCare-2.9.0-debug.apk`
- `release-artifacts/LibreCare-2.9.0-release.apk`
- `release-artifacts/LibreCare-2.9.0-release.aab` (Google Play upload file)

