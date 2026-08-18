# LibreCare Release Notes v1.4.0

**Version:** 1.4.0 (versionCode 6)  
**Release Date:** 2026-08-18  
**Branch:** master  
**Git Commit:** 877b999

---

## PL

### Najwazniejsze zmiany
- Dodano ekran **Informacje i statystyki** z realnymi danymi lokalnymi i synchronizacyjnymi.
- Dodano rzeczywiste zliczanie transferu sieciowego (pobrane/wyslane bajty, liczba zapytan, sukcesy/bledy).
- Dodano ustawienie **Retencja danych** (12 godzin - 24 miesiace) z estymacja i potwierdzeniem usuwania starszych danych.
- Dodano ustawienie **Czestotliwosc odpytywania** (15/30/60 min) z estymacja transferu.
- Tryb demo nie jest liczony jako realny transfer live.

### Funkcje
- **Statystyki bazy danych:**
  - Rozmiar bazy danych (z uwzglednieniem `db`, `-wal`, `-shm`)
  - Liczba odczytow
  - Liczba monitorowanych osob
  - Najstarszy / najnowszy odczyt
  - Dostepny zakres danych
  - Szacowany przyrost na tydzien / miesiac (jesli dane wystarczajace)
- **Statystyki transferu:**
  - Dane pobrane lacznie
  - Dane wyslane lacznie
  - Srednie dzienne / tygodniowe / miesieczne (jesli dane wystarczajace)
  - Ostatnia synchronizacja
  - Liczba synchronizacji
- **Retencja danych:**
  - Opcje: 12h, 24h, 7d, 30d, 90d, 12m, 24m
  - Estymacja liczby odczytow i rozmiaru bazy po zmianie
  - Potwierdzenie przed skroceniem retencji
  - Komunikat: "Nie usuwa danych z LibreLinkUp"
- **Czestotliwosc odpytywania:**
  - Opcje zgodne z architektura WorkManager: 15/30/60 min
  - Obecne i szacowane zuzycie danych
  - Ostrzezenie o baterii

### Zmiany techniczne
- Nowe klasy:
  - `DiagnosticsStatsRepository`
  - `NetworkUsageTracker`
  - `StatisticsViewModel`
  - `RetentionSettingsViewModel`
  - `PollingFrequencyViewModel`
- Rozszerzono `AppSettings` o:
  - `retentionHours`
  - `backgroundPollingMinutes`
- Rozszerzono klucze `SecureStorage` o liczniki transferu i ustawienia retencji/odpytywania.
- `OkHttpLibreLinkUpHttp` zlicza upload/download w interceptorze (bez logowania danych wrazliwych).
- Cleanup retencji w synchronizacji oparty o godziny retencji zamiast stalej 365 dni.
- Scheduler WorkManager korzysta z zapisanej czestotliwosci (15-60 min).

### Testy
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew connectedDebugAndroidTest` - jesli brak urzadzenia/emulatora: nie wykonano

Dodane testy:
- `DiagnosticsStatsRepositoryTest`
- `NetworkUsageTrackerTest`
- `SettingsRepositoryDiagnosticsTest`

### Artefakty
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (**plik do Google Play**)

### Ograniczenia
- Estymacje transferu i przyrostu sa pokazywane tylko przy wystarczajacej ilosci danych pomiarowych.
- Przy niedostatku danych aplikacja pokazuje: **"Za malo danych do dokladnej estymacji"**.
- Brak connected tests bez podlaczonego urzadzenia/emulatora.

### Rekomendowane kroki manualne
1. Otworz Ustawienia -> Informacje i statystyki i zweryfikuj dane bazy/transferu.
2. Otworz Ustawienia -> Retencja danych i zmien retencje na krotsza (powinno pojawic sie potwierdzenie).
3. Otworz Ustawienia -> Czestotliwosc odpytywania i zweryfikuj estymacje.
4. W trybie demo potwierdz komunikat o wykluczeniu transferu demo.

---

## EN

### Highlights
- Added **Information and statistics** screen backed by real local/sync data.
- Added real network transfer tracking (download/upload bytes, request count, success/failure).
- Added **Data retention** setting (12 hours - 24 months) with estimates and confirmation for shortening.
- Added **Polling frequency** setting (15/30/60 min) with transfer estimates.
- Demo mode is excluded from live transfer counters.

### New capabilities
- Database storage metrics (db + wal + shm), reading/person counts, oldest/newest reading, data range.
- Growth estimates per week/month when enough data is available.
- Network totals and averages per day/week/month when enough data is available.
- Retention cleanup bound to configured retention hours.
- WorkManager periodic sync interval based on persisted polling settings.

### Technical changes
- Added `DiagnosticsStatsRepository`, `NetworkUsageTracker`, and view models for stats/retention/polling.
- Extended `AppSettings` with `retentionHours` and `backgroundPollingMinutes`.
- Extended secure storage keys for diagnostics counters.
- Added DAO aggregate queries for statistics and sync run counters.

### Tests
- Unit tests: PASS
- Lint: PASS
- Connected tests: not executed if no device/emulator is attached

### Artifacts
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (**Google Play upload file**)

### Known limitations
- Estimates are shown only when measurement window is sufficient; otherwise app shows:
  **"Za malo danych do dokladnej estymacji"**.


