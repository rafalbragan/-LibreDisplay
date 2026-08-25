# LibreCare 2.7.0 – Release Notes

**Date:** 2026-08-25
**versionName:** 2.7.0 · **versionCode:** 29 (poprzednio 2.6.0 / 28)

---

## PL

### Nowości
- **Monitorowanie w tle (usługa pierwszoplanowa)** — dane pobierane stale w tle ze stałym powiadomieniem, co ~30 s–5 min (wg ustawionej częstotliwości), odpornie na Doze, także przy zamkniętym ekranie. Domyślnie włączone; startuje, gdy monitorowanie jest skonfigurowane, oraz po restarcie telefonu.
- **Pionowy pasek nawigacji po prawej** w orientacji poziomej zamiast dolnego menu.

### Poprawki
- Usługa `MonitoringService` była zaimplementowana, ale wcześniej **nigdy nie uruchamiana** — w tle działał tylko WorkManager (co 15–60 min, usypiany przez Doze). Teraz usługa jest realnie startowana.

### Zmiany UI (poziomo)
- Ukryty górny i dolny pasek → więcej miejsca na wykres i treść.
- „LibreCare" + wersja oraz „Ostatnia aktualizacja" przeniesione do kompaktowego nagłówka w lewej kolumnie.
- Menu jako pionowy pasek po prawej stronie.

### Częstotliwość pobierania danych (wyjaśnienie)
- **Aplikacja otwarta:** co `refreshInterval` (domyślnie 60 s, zakres 30–300 s).
- **Usługa w tle (nowość, domyślnie włączona):** co 30 s–5 min, stałe powiadomienie, odporna na Doze.
- **WorkManager (zapas):** co 15–60 min (twardy limit Androida to 15 min), zależny od Doze i optymalizacji baterii.

### Testy
- `./gradlew testDebugUnitTest` — PASS.
- `./gradlew lint` — PASS.

### Znane ograniczenia
- Wydzielenie metryk do lewej kolumny w poziomie oraz pełny ekran „Analiza" i eksport Excel — planowane w kolejnych wydaniach.
- Na Androidzie 13+ do wyświetlenia powiadomienia usługi wymagana jest zgoda `POST_NOTIFICATIONS` (usługa działa również bez widocznego powiadomienia).

### Artefakty
- `release-artifacts/LibreCare-2.7.0-debug.apk`
- `release-artifacts/LibreCare-2.7.0-release.apk`
- `release-artifacts/LibreCare-2.7.0-release.aab` (plik do Google Play)

---

## EN

### New
- **Always-on background monitoring (foreground service)** — continuous background fetching with a persistent notification, every ~30 s–5 min (per configured interval), Doze-resistant, even with the screen closed. Enabled by default; starts when monitoring is configured and after reboot.
- **Vertical navigation rail on the right** in landscape instead of the bottom bar.

### Fixed
- `MonitoringService` was implemented but previously **never started** — only WorkManager ran in the background (every 15–60 min, deferred by Doze). It is now actually started.

### UI changes (landscape)
- Top and bottom bars hidden → more room for the chart and content.
- "LibreCare" + version and "last update" moved into a compact header in the left column.
- Menu rendered as a vertical rail on the right.

### Data fetch frequency (clarification)
- **App open:** every `refreshInterval` (default 60 s, range 30–300 s).
- **Background service (new, default on):** every 30 s–5 min, persistent notification, Doze-resistant.
- **WorkManager (fallback):** every 15–60 min (Android hard minimum is 15 min), subject to Doze and battery optimization.

### Tests
- `./gradlew testDebugUnitTest` — PASS.
- `./gradlew lint` — PASS.

### Known limitations
- Extracting metrics into the left landscape column, plus the full "Analysis" screen and Excel export, are planned for later releases.
- On Android 13+ the service notification requires `POST_NOTIFICATIONS` consent (the service still runs without a visible notification).

### Artifacts
- `release-artifacts/LibreCare-2.7.0-debug.apk`
- `release-artifacts/LibreCare-2.7.0-release.apk`
- `release-artifacts/LibreCare-2.7.0-release.aab` (Google Play upload file)

