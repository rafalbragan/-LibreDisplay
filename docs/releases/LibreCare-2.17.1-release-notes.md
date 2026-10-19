# LibreCare 2.17.1 — release notes / informacje o wydaniu

Date / data: 2026-10-19. Version: **2.17.0 / 51 → 2.17.1 / 52**. Application ID: `com.libredisplay`. Existing PR: **#46**, branch `feature/libreview-chart2-play-20261018`.

## PL

### Poprawki
- Obsługa dokładnych nagłówków polskiego eksportu Abbott: `Urządzenie`, `Numer seryjny`, `Znacznik czasu w urządzeniu`, `Rodzaj zapisu`, `Historyczne stężenia glukozy mg/dL`, `Stężenie glukozy na podstawie skanowania mg/dL`.
- Kolumna ketonów w mmol/L nie blokuje poprawnych kolumn glukozy mg/dL. Inne jednostki glukozy nadal są odrzucane; wartości nie są przeliczane, uzupełniane ani zgadywane.
- Metadane pacjenta pozostają wyłącznie w prywatnym podglądzie, nie w automatycznym tekście diagnostycznym podsumowania. Błędy parsera nie ujawniają treści CSV.

### Funkcje i ulepszenia
- Bez nowego projektu UI. Zachowane: Chart 2.0, strumieniowy odczyt, dwie początkowe linie metadanych/nagłówka, cytowanie, czas lokalny, raportowanie DST i anulowanie.
- Typ 0 to historia, typ 1 to skan; typ 6 nie jest pomiarem glukozy. Niepoprawne wartości są jawnie liczone jako odrzucone.
- Zachowane: podgląd bez zapisu, potwierdzony wybór profilu, obsługa duplikatów/konfliktów, atomowe wycofanie i ochrona importowanej historii przed rutynową retencją.

### Testy i artefakty
- RED: 19 testów parsera, 6 oczekiwanych niepowodzeń; GREEN: 19/19. Wyłącznie dane syntetyczne.
- Fixture rzeczywistego selektora dokumentów emulatora używa teraz dokładnego formatu Abbott z ketonami mmol/L, a nie starej wersji z pamięci podręcznej.
- Pełne wyniki JVM, lint, migracji i gestów emulatora oraz rozmiar/SHA-256 APK: [raport wydania](../release-report-2.17.1.md).
- Debug: `release-artifacts/LibreCare-2.17.1-debug.apk`. Release APK/AAB i plik do Google Play: **HOLD**.

### Ograniczenia
- Nie podano autoryzowanej ścieżki prywatnego eksportu; integracja z prywatnym plikiem nie została zweryfikowana.
- Brak czterech właściwości podpisu release. Właściciel musi sprawdzić certyfikat **Upload Key**, nie certyfikat App Signing, oraz najwyższy użyty versionCode we wszystkich ścieżkach Play.
- Bez instalacji/zmian na Samsung SM-S948B, odinstalowania aplikacji, publikacji, scalania lub auto-merge.

## EN

### Fixes
- Support the exact Polish Abbott timestamp, record-type, historical-glucose and scan-glucose headers while preserving previous aliases.
- Validate mg/dL on required glucose columns only. Unrelated mmol/L ketone columns no longer invalidate an export. Unsupported glucose units still fail closed; invalid values are counted, never fabricated.
- Redact patient metadata from automatic summary diagnostics while retaining the private preview; sanitized parser errors contain no raw CSV.

### Features and improvements
- No UI redesign, chart replacement or database schema change. Streaming, metadata/header rows, quoted CSV, timestamps, DST reporting and cancellation retained.
- Types 0/1 remain historical/scan readings; type 6 remains excluded. Existing confirmed assignment, no-write preview, transactional rollback, duplicate handling, repeat-import idempotency and retention protection retained.

### Tests and artifacts
- RED: 19 parser tests with 6 expected failures; GREEN: 19/19. Synthetic inputs only.
- Emulator picker fixture now exercises exact Abbott headers and ketones, refreshing its synthetic cached file.
- Full JVM/lint/migration/retention/chart-gesture results, APK size/hash and compliance checklist: [release report](../release-report-2.17.1.md) and [quality report](../quality-review-report-2.17.1.md).
- Debug: `release-artifacts/LibreCare-2.17.1-debug.apk`. Signed release APK/AAB and Google Play upload file: **HOLD**.

### Known limitations
- No authorized private export path supplied: private-file integration remains unverified.
- Release credentials, registered Play Upload Key compatibility and the highest uploaded Play versionCode remain unverified/unconfigured. App Signing and Upload Key certificates have different roles.
- No physical Samsung changes, uninstall, Play publication, merge or auto-merge.
