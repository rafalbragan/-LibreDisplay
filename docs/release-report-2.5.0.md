# LibreCare 2.5.0 – raport wydania

Data: 2026-08-24
Wersja: 2.5.0 (versionCode 27), poprzednio 2.4.0 (versionCode 26)
Gałąź: `master`

---

## 1. Podsumowanie

Wydanie porządkuje dwa mylące momenty w cyklu życia aplikacji:

1. **Start po instalacji.** Zamiast abstrakcyjnego pytania „czy przywrócić kopię zapasową?”
   aplikacja pokazuje konkretnie, jakie dane ma: kogo dotyczą, za jaki okres, ile dni i odczytów
   oraz czy okres jest ciągły, czy „z przerwami” z podanym procentem braków.
2. **Logowanie.** Logowanie jest jednym bezobsługowym przepływem: weryfikacja hasła → scalenie
   danych → pobranie ostatnich 12 godzin → zapis do pliku danych → ekran główny. Aplikacja nie pyta
   już o połączenie z kontem LibreLinkUp.

Dodatkowo zabezpieczenie aplikacji zyskało prawdziwą weryfikację odciskiem palca przed włączeniem
oraz opcjonalny klucz dostępu (passkey).

---

## 2. Przegląd architektury przed implementacją

| Obszar | Stan przed zmianą | Działanie |
| --- | --- | --- |
| `MainActivity` – prompt po instalacji | EXISTS (mylący `AlertDialog` „Wykryto nową instalację”) | zastąpiony `StartupRestoreHost` |
| `AppDataBackupRepository` | EXISTS (kopia, staging, merge, restore) | rozszerzone o ofertę danych i scalanie po logowaniu |
| `BackupMergeEngine` | EXISTS (porównanie odczyt po odczycie po znaczniku czasu) | bez zmian – wykorzystane bezpośrednio |
| Opis pokrycia okresu (min/max, % braków) | MISSING | nowy `BackupCoverageCalculator` |
| Przepływ startowy z dialogami | MISSING | nowy pakiet `ui/restore` |
| `SettingsViewModel.saveAndLogin` | INCOMPLETE (logował, ale nie scalał i nie pobierał danych) | rozszerzony o kroki 2–4 |
| `GlucoseSyncRepository` | INCOMPLETE (stałe okno 24 h, brak sygnału o nowych danych) | `SyncReason.LOGIN` + okno 12 h + `onReadingsStored` |
| `AppLockRepository` | INCOMPLETE (tylko flaga włącz/wyłącz) | `UnlockMethod`, passkey, `enableBiometricUnlock()` |
| Passkey / `CredentialManager` | MISSING | nowy `PasskeyManager` + `PasskeyRequestFactory` |
| `PrivacyDataScreen` → `AppLockSection` | INCOMPLETE (checkbox bez weryfikacji) | weryfikacja odciskiem przed włączeniem + klucz dostępu |
| Room / migracje | EXISTS, bez zmian schematu | **brak zmian w bazie – migracja niepotrzebna** |

---

## 3. Zmiany w kodzie

### Nowe pliki

| Plik | Rola |
| --- | --- |
| `data/backup/BackupCoverage.kt` | czysta, testowalna matematyka pokrycia: min/max, liczba dni, oczekiwana liczba odczytów, procent braków, opis po polsku |
| `ui/restore/StartupRestoreModels.kt` | maszyna stanów przepływu startowego + formatowanie treści dialogów |
| `ui/restore/StartupRestoreViewModel.kt` | orkiestracja: oferta → scalanie → rozbieżności → podsumowanie → pytanie o plik |
| `ui/restore/StartupRestoreHost.kt` | dialogi Compose, launcher pliku uruchamiany dopiero po zgodzie |
| `auth/PasskeyManager.kt` | tworzenie i weryfikacja passkey przez `CredentialManager` + testowalna fabryka JSON WebAuthn |

### Zmodyfikowane pliki

| Plik | Zmiana |
| --- | --- |
| `MainActivity.kt` | usunięty prompt „Wykryto nową instalację”, wpięty `StartupRestoreHost` |
| `data/repository/AppDataBackupRepository.kt` | `BackupOffer`, `loadAutomaticBackupOffer()`, `loadOfferFromUri()`, `applyWholeStagedRestore()`, `mergeAutomaticBackupAfterLogin()`, `refreshAutomaticBackupQuietly()` |
| `data/repository/GlucoseSyncRepository.kt` | `SyncReason.LOGIN`, okno 12 h dla logowania, callback `onReadingsStored` |
| `LibreDisplayApp.kt` | wpięcie odświeżania pliku danych po każdej udanej synchronizacji |
| `ui/settings/SettingsViewModel.kt` | `prepareDataAfterVerifiedLogin()` – kroki 2–4 przepływu logowania |
| `auth/AppLockRepository.kt` | `UnlockMethod`, `passkeyId`, `enableBiometricUnlock()`, `enablePasskeyUnlock()`, `hasEnrolledBiometrics()` |
| `ui/security/AppLockScreen.kt` | odblokowanie kluczem dostępu z bezpiecznym powrotem do keyguardu |
| `ui/privacy/PrivacyDataScreen.kt` | nowa sekcja blokady: weryfikacja odciskiem przed włączeniem, tworzenie/usuwanie klucza dostępu |

---

## 4. Zmiany UI

- Dialog oferty danych: lista punktowana, maks. 320 dp wysokości ze scrollem, typografia
  `bodyMedium` / `bodySmall`, wszystkie napisy po polsku.
- Dialog rozbieżności: liczba różnic, wiersz na osobę, do 5 konkretnych przykładów
  z datą, wartością w aplikacji i w archiwum. Przyciski: „Zachowaj bieżące” / „Użyj z archiwum”.
- Dialog pytania o plik: jasne „Wybierz plik” / „Nie, dziękuję”.
- Sekcja „Blokada aplikacji”: dwa niezależne wskaźniki metody (odcisk / klucz dostępu), przycisk
  tworzenia i usuwania klucza, status oraz komunikat zwrotny po każdej akcji.
- Weryfikacja: wszystkie dialogi używają `MaterialTheme.typography`, więc skalują się z ustawieniem
  rozmiaru czcionki i działają w trybie ciemnym. Treść przewija się na małych ekranach.
- Ekran główny, wykres, historia i NFZ nie były modyfikowane – brak ryzyka regresji w tych obszarach.

---

## 5. Zmiany w bazie danych

**Brak.** Schemat Room, wersja bazy i migracje pozostają bez zmian. Nowe funkcje operują wyłącznie na
istniejących encjach (`observed_persons`, `glucose_readings`, `patient_settings`) oraz na pliku
kopii w katalogu danych aplikacji. Migracja nie jest wymagana i nie została dodana.

---

## 6. Testy

| Zestaw | Nowe testy | Wynik |
| --- | --- | --- |
| `BackupCoverageCalculatorTest` | 6 | PASS |
| `StartupRestoreFormatterTest` | 5 | PASS |
| `PasskeyRequestFactoryTest` | 7 | PASS |
| `AppDataBackupRepositoryTest` | 8 (rozszerzenie istniejącego zestawu) | PASS |
| **Razem** | **26** | — |

Polecenia:

- `./gradlew testDebugUnitTest` – PASS, 402 testy, 0 błędów
- `./gradlew lint` – PASS
- `./gradlew assembleDebug` – PASS
- `./gradlew assembleRelease` – PASS
- `./gradlew bundleRelease` – PASS

Testy instrumentalne: `./gradlew connectedDebugAndroidTest` – PASS na dwóch urządzeniach
(`SM-S948B` – Android 16, `LibreCare_API35(AVD)` – Android 15). Schemat bazy nie uległ zmianie,
więc test migracji Room pozostaje aktualny i przechodzi.

---

## 7. Artefakty

| Artefakt | Ścieżka | Rozmiar |
| --- | --- | --- |
| DEBUG APK | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.5.0-debug.apk` | 23 678 597 B |
| RELEASE APK | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.5.0-release.apk` | 3 490 239 B |
| RELEASE AAB | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.5.0-release.aab` | 6 325 791 B |

**GOOGLE PLAY UPLOAD FILE:** `release-artifacts/LibreCare-2.5.0-release.aab` (6 325 791 B)

---

## 8. Branding

Wszystkie nowe napisy użytkownika mówią „LibreCare”. Pozostałe wystąpienia `libredisplay` to
wyłącznie `applicationId` / nazwy pakietów i katalogów źródłowych — zgodnie z dotychczasową
klasyfikacją nie są zmieniane, bo zmiana `applicationId` zerwałaby aktualizacje w Google Play.

---

## 9. Pozostałe ryzyka

1. **Passkey wymaga Digital Asset Links.** `PasskeyRequestFactory.RELYING_PARTY_ID` wskazuje na
   `librecare.app`. Dopóki domena nie opublikuje `assetlinks.json` powiązanego z podpisem aplikacji,
   systemowy menedżer poświadczeń odrzuci tworzenie klucza. Aplikacja obsługuje to komunikatem i
   pozostaje przy odcisku palca — ryzyko funkcjonalne, nie awaryjne.
2. **Procent braków zakłada kadencję 5 minut.** Dla innych czujników wartość jest orientacyjna.
3. **Automatyczne łączenie po wczytaniu archiwum** zadziała tylko, gdy archiwum niesie poprawne dane
   logowania; w przeciwnym razie użytkownik zobaczy komunikat i musi zalogować się ręcznie.
4. **Odświeżanie pliku danych po każdej synchronizacji z wstawkami** zwiększa liczbę zapisów na
   dysk. Zapis jest atomowy i tani (serializacja wierszy LIVE), ale przy bardzo dużej historii warto
   obserwować czas zapisu.
5. **Brak testów UI Compose** dla nowych dialogów — logika i treści są pokryte testami jednostkowymi,
   ale sam render nie jest testowany automatycznie.


