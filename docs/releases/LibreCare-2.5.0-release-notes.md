# LibreCare 2.5.0 – notatki wydania

Data: 2026-08-24
versionName: 2.5.0
versionCode: 27
Poprzednia wersja: 2.4.0 (versionCode 26)

---

## PL

### Nowe funkcje

- **Zrozumiała propozycja wczytania danych zamiast pytania o „kopię zapasową”.**
  Po instalacji aplikacja nie pyta już „czy przywrócić kopię?”. Zamiast tego pokazuje, jakie dane
  faktycznie ma i proponuje ich wczytanie. Dla każdej osoby wypisuje:
  - imię/nazwę osoby monitorowanej,
  - datę najstarszego i najnowszego odczytu (`18.08.2026 – 24.08.2026`),
  - liczbę dni i liczbę odczytów,
  - jakość okresu: `dane ciągłe` albo `z przerwami – brakuje ok. 12% danych z całego okresu`.
- **Pytanie o dodatkowy plik dopiero po decyzji o danych z urządzenia.**
  Okno wyboru pliku otwiera się wyłącznie wtedy, gdy użytkownik wyraźnie odpowie „Wybierz plik”.
  Odpowiedź „Nie, dziękuję” po prostu zamyka dialog i aplikacja rusza dalej.
- **Logowanie odciskiem palca wymaga rzeczywistej weryfikacji.**
  Zaznaczenie opcji uruchamia systemowy prompt; metoda odblokowania zostaje przełączona dopiero po
  udanym potwierdzeniu odciskiem palca / PIN-em.
- **Klucz dostępu (passkey).** Można utworzyć klucz przez systemowego menedżera poświadczeń i używać
  go do odblokowania aplikacji. Gdy klucz nie zadziała lub urządzenie go nie obsługuje, aplikacja
  wraca do odcisku palca / PIN-u i nigdy nie blokuje dostępu do własnych danych medycznych.
- **Infrastruktura testowa chmurowa.** Repozytorium zawiera teraz Codespaces, szybki CI, scaffold
  Firebase Test Lab i dokumentację `docs/testing/`. To warstwa techniczna — bez zmian w wyglądzie lub
  zachowaniu aplikacji produkcyjnej.

### Zmiany

- **Logowanie to jeden bezobsługowy przepływ:**
  1. sprawdzenie poprawności hasła w LibreLinkUp,
  2. dopiero potem scalenie zapisanych danych z bieżącymi,
  3. natychmiastowe pobranie odczytów z ostatnich 12 godzin,
  4. dopisanie świeżych danych do bieżącego pliku danych aplikacji,
  5. otwarcie ekranu głównego.
  Aplikacja nie pyta już „czy połączyć z kontem LibreLinkUp?”.
- Po wczytaniu archiwum zawierającego konfigurację aplikacja łączy się z LibreLinkUp samodzielnie
  i dopisuje wynik do podsumowania („Połączono z LibreLinkUp i pobrano bieżące dane…”).
- Każda synchronizacja, która zapisała nowe odczyty, natychmiast odświeża jeden automatyczny plik
  danych — nowe dane nie czekają na okresowego workera.
- Scalanie po zalogowaniu jest nieniszczące: identyczne odczyty są scalane, nowe dopisywane,
  a wartości bieżące mają pierwszeństwo do czasu decyzji użytkownika.
- Porównanie odbywa się odczyt po odczycie (po znaczniku czasu). Pytanie pojawia się wyłącznie tam,
  gdzie ten sam znacznik czasu ma inną wartość — i pokazuje konkretne przykłady
  („24.08.2026 06:15: w aplikacji 118 mg/dL, w archiwum 122 mg/dL”).

### Poprawki

- Uszkodzony plik danych nie wywraca już startu aplikacji — użytkownik dostaje komunikat
  „Zapisany plik danych jest uszkodzony i nie można go odczytać.”.
- Pytanie o dane nie pojawia się po ręcznym zalogowaniu, bo dane zostały scalone w trakcie logowania.

### Testy

| Zestaw | Zakres | Wynik |
| --- | --- | --- |
| `BackupCoverageCalculatorTest` | 6 testów: pusty okres, okres ciągły, 49% braków, min/max data, duplikaty, odmiana „dni” | PASS |
| `StartupRestoreFormatterTest` | 5 testów: nagłówek oferty, brak pliku, linie z okresem i przerwami, podsumowanie rozbieżności, brak rozbieżności | PASS |
| `PasskeyRequestFactoryTest` | 7 testów kontraktu WebAuthn (rp, user, platform authenticator, allowCredentials, losowość challenge, odczyt id) | PASS |
| `AppDataBackupRepositoryTest` | +8 testów: pusta oferta, lista osób bez treści demo, procent braków, uszkodzony plik, scalanie po logowaniu, konflikt, brak archiwum, odświeżenie pliku | PASS |
| `./gradlew testDebugUnitTest` | całość | PASS (402 testy, 0 błędów) |
| `./gradlew lint` | całość | PASS |

### Artefakty

| Artefakt | Ścieżka | Rozmiar |
| --- | --- | --- |
| Debug APK | `release-artifacts/LibreCare-2.5.0-debug.apk` | 23 678 597 B (22,58 MB) |
| Release APK | `release-artifacts/LibreCare-2.5.0-release.apk` | 3 490 239 B (3,33 MB) |
| Release AAB (Google Play) | `release-artifacts/LibreCare-2.5.0-release.aab` | 6 325 791 B (6,03 MB) |

### Znane ograniczenia

- Klucz dostępu (passkey) wymaga dostawcy passkey w systemie oraz zweryfikowanego powiązania
  domeny (Digital Asset Links) dla `librecare.app`. Bez tego system odrzuci utworzenie klucza —
  aplikacja pokazuje wtedy jasny komunikat i pozostaje przy odcisku palca / PIN-ie.
- Procent braków liczony jest przy założeniu kadencji LibreLinkUp co 5 minut. Dla czujników o innej
  kadencji wartość jest orientacyjna.
- Automatyczne połączenie po wczytaniu archiwum działa tylko, jeśli archiwum zawiera konfigurację
  z poprawnymi danymi logowania.

---

## EN

### Added

- The startup question now offers the data LibreCare actually holds, listing per person the display
  name, the oldest and newest reading date, the number of days and readings, and whether the period
  is continuous or "with gaps – about X% of the whole period is missing".
- The extra-file question is asked only after the stored-data step, and the file picker opens only
  after an explicit "yes".
- Enabling fingerprint unlock now requires a real fingerprint check before the method is switched.
- Passkeys can be created through the system credential manager and used to unlock the app, with a
  safe fallback to fingerprint / PIN.

### Changed

- Login is one non-interactive flow: verify password → merge stored data → download the last
  12 hours → write fresh readings back into the data file → open Home. The app no longer asks
  whether to connect to LibreLinkUp.
- Every sync that stored new readings refreshes the single automatic data file immediately.
- Readings are compared one by one; the user is only asked about real value differences and is shown
  concrete examples.

### Fixed

- A corrupted data file no longer breaks startup; a clear Polish message is shown instead.
- The data question no longer appears after a manual sign-in.

### Tests

402 unit tests pass, including 26 new tests covering coverage math, dialog wording, the WebAuthn
request contract and the login-time merge.

### Artifacts

See the table in the Polish section. Google Play upload file: `release-artifacts/LibreCare-2.5.0-release.aab`.

### Known limitations

Passkey creation depends on a system passkey provider and verified Digital Asset Links; gap
percentage assumes a 5-minute LibreLinkUp cadence; auto-connect after restore requires valid
credentials inside the archive.



