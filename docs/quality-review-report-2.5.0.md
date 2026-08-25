# LibreCare 2.5.0 – raport przeglądu jakości

Data: 2026-08-24
Wersja: 2.5.0 (versionCode 27)

---

## 1. Zakres przeglądu

Przegląd obejmuje zmiany wprowadzone w 2.5.0: przepływ startowy „wczytaj moje dane”, przepływ
logowania, natychmiastowy zapis nowych odczytów do pliku danych oraz blokadę aplikacji
(odcisk palca + klucz dostępu).

---

## 2. Weryfikacja wymagań użytkownika

| # | Wymaganie | Realizacja | Status |
| --- | --- | --- | --- |
| 1 | Zamiast pytania o kopię – propozycja wczytania danych, które aplikacja ma | `StartupRestoreStep.OfferLocalData` + `StartupRestoreFormatter.offerHeadline` | PASS |
| 2 | Wylistuj kogo dane i za jaki okres | `AppDataBackupRepository.loadAutomaticBackupOffer()` zwraca `PersonDataCoverage` na osobę | PASS |
| 3 | Pokaż min i max datę | `PersonDataCoverage.firstTimestamp` / `lastTimestamp`, format `dd.MM.yyyy` w strefie lokalnej | PASS |
| 4 | Gdy brakuje danych – napisz „z przerwami” | `BackupCoverageCalculator.describe()` → `"z przerwami – brakuje ok. X% danych z całego okresu"` | PASS |
| 5 | Podaj procent brakujących danych z całego okresu | `missingPercent` = brakujące sloty / oczekiwane sloty (kadencja 5 min) | PASS |
| 6 | Zapytaj, czy dodatkowo wczytać plik | `StartupRestoreStep.AskForFile` po kroku danych lokalnych | PASS |
| 7 | Okno wyboru pliku dopiero po zgodzie | `filePicker.launch(...)` wyłącznie w `confirmButton` dialogu | PASS |
| 8 | Sprawdzaj odczyt po odczycie | `BackupMergeEngine.buildPersonPlan` porównuje po znaczniku czasu i wartości | PASS |
| 9 | Pytaj tylko przy rozbieżnościach | `if (plan.hasConflicts)` → dialog; inaczej scalanie bez pytania | PASS |
| 10 | Podaj dane pozwalające zdecydować | `ConflictSummary`: liczba różnic, wiersz na osobę, do 5 przykładów z datą i obiema wartościami | PASS |
| 11 | Po logowaniu nie pytaj o połączenie z LibreLinkUp | usunięte pytanie; `connectRestoredAccountSilently()` łączy samodzielnie | PASS |
| 12 | Najpierw sprawdź hasło, potem przywracaj | `ensureAuthenticated(force = true)` w `runCatching`, scalanie dopiero w `onSuccess` | PASS |
| 13 | Scal kopię z bieżącymi danymi | `mergeAutomaticBackupAfterLogin()` z `ConflictResolution.KEEP_LOCAL` | PASS |
| 14 | Odczytaj bieżące dane za 12 godzin | `SyncReason.LOGIN` + `LOGIN_BACKFILL_WINDOW = 12 h` | PASS |
| 15 | Otwórz ekran główny | `_message.value = "Ustawienia zapisane"` → nawigacja do Monitoring | PASS |
| 16 | Od razu dopisz nowe dane do zaszyfrowanego pliku danych | `onReadingsStored` → `refreshAutomaticBackupQuietly()` po każdej synchronizacji z wstawkami | PASS |
| 17 | Odcisk palca – zweryfikuj i przełącz logowanie | `enableBiometric()` przełącza metodę wyłącznie po `BiometricResult.Success` | PASS |
| 18 | Umożliw stworzenie klucza dostępu i logowanie nim | `PasskeyManager.createPasskey()` / `verifyPasskey()`, `UnlockMethod.PASSKEY` | PASS |

---

## 3. Przegląd architektury

- **Separacja warstw zachowana.** Cała logika obliczeniowa (`BackupCoverageCalculator`,
  `StartupRestoreFormatter`, `PasskeyRequestFactory`) jest czysta i nie zależy od Compose ani od
  Androida (poza `Base64` w fabryce WebAuthn, testowanym pod Robolectrikiem).
- **ViewModel nie zawiera UI.** `StartupRestoreViewModel` wystawia jeden `StateFlow<StartupRestoreStep>`;
  `StartupRestoreHost` tylko renderuje.
- **Repozytorium pozostaje jedynym właścicielem operacji na danych.** UI nigdy nie dotyka DAO.
- **Brak nowych zależności.** `androidx.credentials` i `androidx.biometric` były już w projekcie.

---

## 4. Bezpieczeństwo i prywatność

- Hasło jest weryfikowane przed jakąkolwiek modyfikacją bazy — nieudane logowanie nie zmienia danych.
- `mergeAutomaticBackupAfterLogin()` używa `restoreConfiguration = false`, więc świeżo zweryfikowane
  poświadczenia nie są nadpisywane przez starsze z archiwum.
- Klucz dostępu: LibreCare przechowuje wyłącznie identyfikator poświadczenia, nigdy klucza
  prywatnego. Weryfikacja jest delegowana do systemowego menedżera poświadczeń.
- Blokada aplikacji nigdy nie zamyka dostępu na stałe: gdy passkey lub biometria zawiodą, następuje
  powrót do keyguardu, a gdy urządzenie nie potrafi zweryfikować użytkownika — aplikacja się otwiera.
- Treść dialogów nie ujawnia adresu e-mail ani hasła; w podsumowaniach pojawiają się tylko nazwy
  osób monitorowanych, daty i wartości glikemii.

---

## 5. Ustawienia, statystyki, retencja

Bez zmian i bez regresji: retencja, częstotliwość odpytywania, statystyki, informacje o pamięci
i transferze, kontrola prywatności oraz reset konta pozostają dostępne na dotychczasowych ekranach.
`refreshAutomaticBackupQuietly()` uruchamiane jest po zastosowaniu retencji, więc plik danych
odzwierciedla faktyczną zawartość bazy.

---

## 6. Wykres i NFZ

Kod wykresu oraz kod NFZ nie był modyfikowany w tym wydaniu. Interakcje wykresu (tap → pełny ekran,
tooltip, strefa docelowa, znaczniki min/max, zachowanie wybranej osoby i zakresu) pozostają w stanie
z 2.4.0.

---

## 7. Jakość testów

| Kryterium | Ocena |
| --- | --- |
| Testy jednostkowe logiki | PASS – 26 nowych testów, w tym przypadki brzegowe (pusty okres, duplikaty, uszkodzony plik) |
| Testy ViewModel | CZĘŚCIOWO – logika i formatowanie pokryte; sam `StartupRestoreViewModel` nie ma testu z podstawionym repozytorium |
| Testy repozytorium | PASS – 8 testów na `AppDataBackupRepository` z realną bazą Room (Robolectric) |
| Testy migracji | NIE DOTYCZY – schemat bazy bez zmian |
| Testy UI Compose | BRAK dla nowych dialogów |

---

## 8. Infrastruktura testowa

Repozytorium zostało uzupełnione o warstwę techniczną dla automatycznego testowania w chmurze:

- Codespaces: `.devcontainer/devcontainer.json`
- walidacja środowiska: `scripts/verify-environment.sh`
- szybki CI: `.github/workflows/android-ci.yml`
- Firebase Test Lab: `.github/workflows/firebase-test-lab.yml`
- dokumentacja operacyjna: `docs/testing/`

Ta warstwa jest już zweryfikowana w aktualnym stanie repozytorium i nie wymaga zmian w logice
produkcyjnej.

Zmiana nie dotyka UI ani logiki produkcyjnej.

## 8. Pozostałe ryzyka

1. Passkey wymaga zweryfikowanego powiązania domeny `librecare.app` (Digital Asset Links).
2. Procent braków zakłada kadencję 5 minut.
3. Brak testu integracyjnego dla `StartupRestoreViewModel` (wymagałby wstrzykiwania repozytorium).
4. Brak testów UI Compose dla nowych dialogów.
5. Częstsze zapisy pliku danych po synchronizacji — do obserwacji przy bardzo długiej historii.

