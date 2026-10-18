# LibreCare 2.16.6 — P0 candidate (versionCode 50)

2026-10-18 — **NOT RELEASE READY / kandydat niewydany**

## PL

### Poprawki
- Zabezpieczono trzy odtworzone na prawdziwej Activity emulatora ścieżki wyjątku `IllegalArgumentException: Cannot round NaN value`: mianowniki rozkładu zakresów i aktywności czujnika oraz współrzędne osi wykresu. Różnica czasu API/Room może być mniejsza niż milisekunda; konwersja pełnego timestampu do `Float` dodatkowo gubi krótkie przedziały. Nie zmieniono progów medycznych ani alarmów.
- Podczas wyboru osoby czyszczone są poprzednie odczyty, historia i powiązane pola. Spóźnione wyniki wcześniejszego żądania nie zastępują danych nowej osoby.
- Nieudany wybór nie zastępuje ostatniego poprawnie zapisanego wyboru. Stary zapis nieaktywnej osoby można obsłużyć po restarcie bez kasowania sesji, konta lub historii.
- Przełącznik osób pozostaje dostępny podczas ładowania i w stanie bez danych; komunikaty są po polsku.

### Nowe funkcje / usprawnienia
- Brak nowych funkcji produktu: zakres wyłącznie P0.
- Dodano powtarzalny scenariusz emulatora z syntetycznymi odpowiedziami HTTP, rzeczywistą Activity/ViewModel, Room i szyfrowanymi ustawieniami.
- Dodano niezależne audyty GitHub, podpisu i ciągłości danych. Audyty podpisu i historii nie wdrażają zmian.

### Testy
- Testy regresyjne precyzji czasu, parsera, anulowania i izolacji danych.
- Interakcja z prawdziwym UI: osoba aktywna → brak danych/stary odczyt → powrót; wyścig przełączeń podczas pobierania.
- Oddzielne procesy dla ostatniego poprawnego wyboru, starego błędnego zapisu i starego odczytu. PID jest sprawdzany; nie zastąpiono restartu procesu odtworzeniem Activity.
- Wyniki wykonanych poleceń i dowody: [raport wydania](../release-report-2.16.6.md), [raport jakości](../quality-review-report-2.16.6.md).

### Artefakty i ograniczenia
- Pliki lokalne i ich rozmiary wymienia raport wydania. Nie opublikowano ani automatycznie nie zainstalowano wydania na telefonie.
- Budowanie release APK/AAB jest zablokowane przez brak konfiguracji podpisu w izolowanym worktree; nie importowano kluczy ani haseł. Brak pliku do przesłania do Google Play.
- Test procesu tymczasowo wyłącza BootReceiver wyłącznie na emulatorze i przywraca stan domyślny. Nie dowodzi poprawności autostartu po restarcie urządzenia, uruchomienia bez instrumentacji, usług tła ani rzeczywistego konta/sieci.
- Brak zweryfikowanego stack trace konkretnej awarii Samsunga; odtworzona ścieżka nie dowodzi tożsamości obu awarii.
- Zgodność z certyfikatem podpisu aplikacji Google Play i dostępność kodu 50 niepotwierdzone. Brak autoryzacji do zmiany podpisu lub publikacji.
- Historia i kopie zapasowe pozostają niezmienione; żadnej kopii użytkownika nie odtwarzano.
- Baza master: 2.16.2/46; zaobserwowane lokalne artefakty: do 2.16.5/49; nowy kandydat: 2.16.6/50.

## EN

### Fixes
- Guarded three emulator real-Activity `IllegalArgumentException: Cannot round NaN value` paths: range-distribution and sensor-activity denominators, and chart-axis coordinates. API/Room differences can be sub-millisecond; converting absolute timestamps to Float additionally collapses short domains. No medical thresholds or alarm behavior changed.
- Clear previous-person readings/history during selection and reject obsolete asynchronous results.
- Persist selections only after successful data retrieval. Recover a legacy inactive selection without clearing the account, session or historical readings.
- Keep the person selector available while loading and without data; new user-facing messages are Polish.

### New features / improvements
- No new product features: P0-only repair.
- Reproducible synthetic-HTTP emulator tests use the real Activity, ViewModel, Room and encrypted settings.
- Independent GitHub and read-only signing/history audits; no signing or history-recovery implementation.

### Tests
- Precision, parser exception, cancellation and identity-isolation regressions.
- Real UI active/inactive/stale transitions and switching while a previous poll is in flight.
- Three separate-process restoration paths with persisted PID verification, not Activity recreation.
- Actual commands, results and evidence: [release report](../release-report-2.16.6.md), [quality report](../quality-review-report-2.16.6.md).

### Artifacts / known limitations
- Exact local artifact paths and sizes are in the release report. No artifact was published and no physical-device installation occurred.
- Release APK/AAB tasks are blocked by unavailable signing configuration in the isolated worktree; no keys/passwords were imported. No Google Play upload file is available.
- Process tests temporarily disable BootReceiver only on the emulator and restore its default state. They do not prove reboot autostart, uninstrumented launch, background-service or real-account/network behavior.
- No verified stack trace for the exact Samsung incident; matching that incident to the reproduced crash remains open.
- Google Play app-signing compatibility and version-code availability remain unverified. Signing changes/publication are not authorized.
- No historical data or user backup was restored or modified.
- Master baseline 2.16.2/46; observed local artifacts through 2.16.5/49; candidate 2.16.6/50.

