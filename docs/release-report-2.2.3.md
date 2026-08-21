# Release Report – LibreCare 2.2.3

Data: 2026-08-21

## Podsumowanie

Wydanie 2.2.3 dostarcza poprawki krytyczne dla przywracania kopii zapasowej oraz stabilizuje interakcje wykresu Home (domena 24h, czytelniejsza oś czasu). Zmiana funkcjonalna zostala wydana z podbiciem wersji do `2.2.3 (23)`.

## Przeglad architektury

- **ViewModel (EXISTS):** `MonitoringViewModel` i `PrivacyDataViewModel` istnieja; zmiana nie wymaga modyfikacji ich kontraktow.
- **Repozytoria (INCOMPLETE -> poprawione):** `AppDataBackupRepository` mial brak odpornosci na uszkodzone payloady; dodano walidacje i obsluge bledow.
- **Room DB / migracje (EXISTS):** brak zmian schematu, brak nowych migracji.
- **API layer (EXISTS):** bez zmian.
- **Demo Mode (EXISTS):** filtrowanie danych demo w backup restore utrzymane.
- **Privacy & Data (INCOMPLETE -> poprawione):** przywracanie backupu ma teraz czytelne komunikaty i bezpieczne pomijanie uszkodzonych rekordow.
- **Statistics/widgets/charts/navigation/Compose (EXISTS):** zmiany ograniczone do chart UX i formatu osi czasu na Home.

## Zmiany UI

- Home chart pracuje na stalej domenie 24h, dzieki czemu viewport 12h = 50% toru suwaka.
- Zwiekszono gestosc etykiet osi X na Home (`maxXAxisLabels = 5`) z formatem `HH:mm`.
- Utrzymano uruchamianie historii fullscreen przez ikone i tap.

## Zmiany bazy danych

- Brak zmian schematu Room.
- Migracje: nie wymagane.

## Testy

| Test | Status |
|------|--------|
| `clean` | PASS |
| `testDebugUnitTest` | PASS |
| `lint` | PASS |
| `assembleDebug` | PASS |
| `assembleRelease` | PASS |
| `bundleRelease` | PASS |
| Testy instrumentowane | No connected device/emulator available. |

Dodatkowo uruchomiono regresje:
- `AppDataBackupRepositoryTest`
- `HomeChartModelsTest`

## Artefakty

| Artefakt | Sciezka | Rozmiar |
|----------|---------|---------|
| Debug APK | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.3-debug.apk` | 23 480 386 B |
| Release APK | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.3-release.apk` | 3 178 014 B |
| Release AAB | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.3-release.aab` | 5 814 564 B |
| Google Play Upload | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.3-release.aab` | 5 814 564 B |

## Wersjonowanie

- Poprzednia wersja: `2.2.2 (22)`
- Nowa wersja: `2.2.3 (23)`

## Branding

- User-facing: `LibreCare` (PASS)
- Package/applicationId legacy: `com.libredisplay` (PASS, legacy techniczne)
- Dokumentacja: istnieja historyczne odniesienia do `LibreDisplay` (PASS, kontekst techniczny)

## Pozostale ryzyka

- Brak testow instrumentowanych i manualnej walidacji na fizycznym urzadzeniu.
- MonitoringScreen nadal nie ma dedykowanych testow Compose UI dla gestow suwaka.

