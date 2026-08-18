# LibreCare Release Report 1.4.0

Data: 2026-08-18  
Wersja: 1.4.0 (versionCode 6)

## Co istnialo przed wdrozeniem
- Ekrany statystyk/retencji/odpytywania istnialy, ale pokazywaly stale, testowe wartosci.
- Brak realnego zliczania transferu upload/download.
- Brak estymacji opartych na rzeczywistych danych pomiarowych.
- WorkManager sync byl ustawiony na stale 6h.
- Retencja danych nie byla zapisywana ani stosowana jako konfiguracja globalna.

## Czego brakowalo
- Widocznego rozmiaru bazy opartego o realny plik DB (+WAL/+SHM).
- Licznikow odczytow/osob i zakresu danych z DAO.
- Szacowanego przyrostu bazy przy wystarczajacej ilosci danych.
- Agregowanych licznikow transferu sieciowego i srednich.
- Wykluczenia transferu demo z licznikow live.
- Trwalej konfiguracji retencji i odpytywania.

## Co zaimplementowano
1. **Repozytorium diagnostyczne**
   - `DiagnosticsStatsRepository` oblicza:
     - rozmiar bazy (`db`, `db-wal`, `db-shm`),
     - liczbe odczytow live,
     - liczbe osob live,
     - najstarszy/najnowszy odczyt,
     - zakres danych,
     - estymacje przyrostu,
     - estymacje retencji,
     - estymacje transferu dla czestotliwosci odpytywania.

2. **Transfer tracking**
   - `NetworkUsageTracker` + interceptor w `OkHttpLibreLinkUpHttp`:
     - zliczanie upload/download,
     - liczenie requestow,
     - sukces/failure sync,
     - okna czasowe pomiaru.
   - Tryb demo nie aktualizuje licznikow live.

3. **Retencja danych**
   - Ustawienie `retentionHours` (12h - 24 miesiace) zapisane w `SettingsRepository`.
   - Ekran retencji z estymacjami i potwierdzeniem przy skracaniu.
   - Cleanup retencji wykonywany przy synchronizacji (`deleteReadingsOlderThanHours`).

4. **Czestotliwosc odpytywania**
   - Ustawienie `backgroundPollingMinutes` (15-60 min) zapisane w `SettingsRepository`.
   - Ekran odpytywania pokazuje obecne/szacowane zuzycie danych.
   - `LibreDisplaySyncScheduler` uzywa ustawionego interwalu WorkManager.

5. **Integracja UI**
   - Ekran `Informacje i statystyki` podlaczony do realnych danych.
   - Dostep do statystyk z Ustawien, Prywatnosci i O aplikacji.

## Weryfikacja
- Testy jednostkowe: PASS
- Lint: PASS
- Connected tests: zalezne od podlaczonego urzadzenia/emulatora

## Dodane testy
- `DiagnosticsStatsRepositoryTest`
- `NetworkUsageTrackerTest`
- `SettingsRepositoryDiagnosticsTest`

## Artefakty
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Google Play upload: release AAB

## Ograniczenia
- Estymacje pojawiaja sie dopiero po zebraniu wystarczajacej ilosci danych.
- Komunikat uczciwy przy braku podstaw estymacji: "Za malo danych do dokladnej estymacji".

## Branding
- Nazwa aplikacji i etykiety uzytkownika: LibreCare.
- `com.libredisplay` pozostaje identyfikatorem technicznym (wewnetrzny).

