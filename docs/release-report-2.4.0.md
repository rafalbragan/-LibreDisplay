# LibreCare 2.4.0 – Release Report

- Wersja: **2.4.0** (versionCode **26**), poprzednio 2.3.0 (versionCode 25)
- Data: 2026-08-23
- Gałąź: `master`

## 1. Podsumowanie

Wydanie skupia się na trzech obszarach:

1. **Kopia zapasowa i przenoszenie danych** – jeden plik tworzony automatycznie, bez hasła
   i bez wyboru lokalizacji przez użytkownika; naprawiony odczyt starych i nowych kopii;
   scalanie z podsumowaniem i rozstrzyganiem konfliktów; eksport/udostępnianie i integracja
   z systemowym transferem (Smart Switch / Google One).
2. **Bezpieczeństwo i logowanie** – obsługa menedżerów haseł (autofill) oraz blokada aplikacji
   biometrią / PIN-em / wzorem / hasłem urządzenia.
3. **Wykres Home** – ciągła linia, stabilny suwak (jedno przesunięcie = cały zakres), dynamiczne
   zakresy 1g…12m, brak przeskoku podczas odświeżania w tle, czytelne metryki i etykiety.

## 2. Przegląd architektury

| Obszar | Status | Uwagi |
| --- | --- | --- |
| Warstwa kopii zapasowej | NOWA | `data/backup/BackupModels.kt`, `BackupCodec.kt`, `BackupMergeEngine.kt`, `LocalBackupStore.kt` |
| Agent systemowego backupu | NOWY | `backup/LibreCareBackupAgent.kt`, `backup/AutomaticBackupWorker.kt` |
| Repozytorium backupu | ZMIENIONE | `data/repository/AppDataBackupRepository.kt` – automatyczny plik, preview, scalanie, konflikty |
| Ustawienia / storage | ZMIENIONE | `SettingsRepository.kt`, `SecureStorage.kt` – klucze blokady i potwierdzeń |
| Blokada aplikacji | NOWA | `auth/AppLockRepository.kt`, `ui/security/AppLockScreen.kt` |
| Autofill | NOWY | `ui/common/AutofillSupport.kt` |
| Wykres i viewport | ZMIENIONE | `GlucoseChart.kt`, `HomeChartModels.kt`, `MonitoringScreen.kt`, `MonitoringViewModel.kt` |
| Metryki | ZMIENIONE | `RedesignedMetrics.kt`, `QuickMetricConfig.kt` |
| Room / schemat | BEZ ZMIAN | Wersja bazy niezmieniona, migracja 1→2 nadal testowana |

## 3. Zmiany UI

- „Aktualna glikemia” i „Trend” w jednej linii (`RedesignedGlucoseCard.kt`).
- Zakresy wykresu jako przewijalny rząd chipów z jednym wyszarzonym „następnym” zakresem.
- Metryki w tych samych obwolutach co chipy zakresów, z podpowiedzią przewijania.
- Suwak nawigatora podwyższony do 40 dp, obsługuje przeciąganie i dotknięcie ścieżki.
- Wiersz dostępności danych zastąpiony czytelnym „Zapisana historia: …”.
- Sprawdzono: skalowanie czcionek, tryb ciemny, ekrany telefonu, orientacja pozioma historii,
  brak nachodzenia etykiet osi, tooltipa i znaczników min/max.

## 4. Zmiany w bazie danych

Brak zmian schematu Room w tej wersji – wersja bazy, encje i DAO pozostały bez zmian.
Test migracji `RoomMigrationTest` (androidTest) nadal działa i przechodzi na obu urządzeniach.

## 5. Testy

| Zadanie | Wynik |
| --- | --- |
| `./gradlew testDebugUnitTest` | PASS – 376 testów, 0 błędów |
| `./gradlew connectedDebugAndroidTest` | PASS – `SM-S948B - 16`, `LibreCare_API35(AVD) - 15` |
| `./gradlew lint` | PASS |
| `./gradlew assembleDebug` | PASS |
| `./gradlew assembleRelease` | PASS |
| `./gradlew bundleRelease` | PASS |

Nowe/zaktualizowane zestawy testów:

- `BackupCodecTest` – kodowanie/dekodowanie v3, odczyt v1/v2 i skróconych kluczy, odporność na uszkodzone dane.
- `BackupMergeEngineTest` – scalanie identycznych odczytów, dopisywanie nowych zakresów, wykrywanie konfliktów.
- `LocalBackupStoreTest` – atomowy zapis, weryfikacja odczytu, zachowanie poprzedniej kopii.
- `AppDataBackupRepositoryTest`, `AppDataBackupRepositoryLegacyRestoreTest` – pełne round-tripy.
- `GlucoseChartSeriesTest` – ciągłość linii i przerwy tylko dla realnych luk.
- `HomeChartRangeOptionsTest` – dynamiczne zakresy i jeden wyszarzony „następny”.
- `RedesignedMetricsTest`, `QuickMetricConfigTest`, `AppNavigationStateTest` – aktualizacje.

## 6. Artefakty

| Artefakt | Ścieżka | Rozmiar |
| --- | --- | --- |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` | 23 629 423 B |
| Release APK | `app/build/outputs/apk/release/app-release.apk` | 3 420 808 B |
| Release AAB | `app/build/outputs/bundle/release/app-release.aab` | 6 171 933 B |
| AndroidTest APK | `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | 392 699 B |

Kopie nazwane: `release-artifacts/LibreCare-2.4.0-{debug.apk,release.apk,release.aab}`.
Plik do Google Play: `release-artifacts/LibreCare-2.4.0-release.aab`.

## 7. Pozostałe ryzyka

1. Brak publicznego API Samsung Smart Switch – poleganie na standardowym Android Backup/Transfer.
2. Chmura realizowana przez zewnętrznych dostawców (SAF/udostępnianie), bez własnego backendu.
3. Passkeys ograniczone do blokady urządzenia; pełne FIDO2 zależy od dostawcy LibreLinkUp.
4. Bardzo duże historie (12 m) nie były testowane na urządzeniach o małej ilości RAM.
5. Testy Compose UI działają jako testy logiki układu (JVM); brak pełnego zestawu testów
   `createAndroidComposeRule` na urządzeniu.

