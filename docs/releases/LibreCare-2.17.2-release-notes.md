# LibreCare 2.17.2 / 53 — Android 16 (API 36)

Date: 2026-10-19. Previous version: 2.17.1 / 52.

## PL

### Poprawki i usprawnienia
- `compileSdk=36` i `targetSdk=36`: usunięcie przyczyny odrzucenia pakietu z docelowym API 35.
- Bezpieczne zakończenie usługi monitorowania po systemowym limicie czasu `dataSync`, zamiast ryzyka awarii po upływie okresu ochronnego. Istniejąca okresowa synchronizacja WorkManager pozostaje dostępna.
- AGP 8.10.1, Gradle 8.11.1 z weryfikacją sumy, Robolectric 4.16.1 i JDK 21 dla testów API 36; bytecode aplikacji pozostaje Java 17.
- Testowy host ekranów używa edge-to-edge tak jak główna aktywność. Dodano kontrole insetów i rzeczywistych gestów systemowych wstecz.

### Zachowane funkcje / nowe funkcje
Nie dodano niezwiązanych funkcji ani nie przeprojektowano UI. Zachowano importer LibreView z dokładnymi polskimi nagłówkami Abbott, kolumnami ketonów, potwierdzeniem profilu, transakcją, deduplikacją i ochroną retencji. Chart 2.0 zachowuje pinch/pan, historię wieloletnią, wybór punktu i osoby. Room pozostaje w wersji schematu 3, `applicationId=com.libredisplay`, minSdk 26.

### Testy i artefakty
Pełny lokalny zestaw JVM: **617/617**, bez pominięć. Lint: **0 błędów, 275 ostrzeżeń, 9 wskazówek**. Końcowe wyniki emulatora API 36, migracji, podpisu, manifestu oraz pełne ścieżki/rozmiary/SHA-256 APK i AAB: [raport wydania](../release-report-2.17.2.md).

Plik przewidziany do ręcznego przesłania: `release-artifacts/LibreCare-2.17.2-release.aab`; jego gotowość wynika wyłącznie z końcowej weryfikacji w raporcie. Nie wykonano publikacji ani operacji na fizycznym Samsungu.

### Znane ograniczenia
- Kod 53 został potwierdzony jako wolny przez właściciela; bez bezpośredniego dostępu do inwentarza Play.
- Podpis lokalnym kluczem upload nie zastępuje weryfikacji certyfikatu zarejestrowanego w Play ani wymaganych deklaracji sklepu.
- Opcjonalne passkeys nadal wymagają zweryfikowanych Digital Asset Links; nowa reguła lint pozostaje jawnym ostrzeżeniem, bez fikcyjnego powiązania domeny.
- Limity systemowe FGS/WorkManager i ustawienia powiadomień mogą ograniczać pracę w tle. Brak deklaracji nieprzerwanego monitorowania ani wielogodzinnego testu.
- Brak udostępnionego prywatnego eksportu; testy używają danych syntetycznych. Pełna macierz tabletów/foldables nie była przedmiotem tego wydania.

## EN

### Fixes and improvements
- Compile/target Android API 36, addressing the reported target-35 Play rejection.
- Stop the monitoring foreground service promptly on the system dataSync timeout; retain existing periodic WorkManager synchronization.
- API-36-compatible AGP 8.10.1, checksum-pinned Gradle 8.11.1, Robolectric 4.16.1 and JDK 21 test runtime. App bytecode remains Java 17.
- Match the test host's edge-to-edge behavior to MainActivity; add system-back gesture and system-inset regression tests.

### Features preserved
No unrelated feature addition or UI redesign. Exact Abbott CSV aliases, ketone handling, patient confirmation, atomic import/dedup/retention and interactive Chart 2.0 remain intact. Room schema 3, `com.libredisplay`, and minSdk 26 are unchanged.

### Tests and artifacts
Local JVM **617/617**, no skips; lint **0 errors / 275 warnings / 9 hints**. Actual API 36 device/migration results and verified APK/AAB paths, signatures, sizes and SHA-256 hashes are in the [release report](../release-report-2.17.2.md). The intended manual Play upload file is `release-artifacts/LibreCare-2.17.2-release.aab`; readiness is established by the report, not this filename.

### Known limitations
Code 53 availability is owner-confirmed, not independently queried through Play. Registered upload-certificate acceptance and Console declarations remain external. Optional passkeys still depend on verified DAL; the new lint finding remains a visible warning. Background work and notifications remain subject to system/user limits; no six-hour soak or tablet/foldable matrix is claimed. No private CSV, physical Samsung operation, merge, auto-merge or Play publication.
