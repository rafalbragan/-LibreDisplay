# LibreCare 2.17.2 / 53 — API 36 release report

Implementation: 2026-10-19. Final local artifact verification: 2026-10-20.

**Local build, tests and signing: PASS. Google Play acceptance: not yet verified.**

## Identity and scope

- Previous version: **2.17.1 / 52**, compile/target SDK 35; new version: **2.17.2 / 53**, compile/target SDK **36**, minSdk **26**.
- Application ID remains **`com.libredisplay`**; user-facing brand remains **LibreCare**.
- Owner explicitly confirmed code **53** unused across uploaded bundles, tracks and drafts. This is owner confirmation, not a direct Play inventory query.
- Authorized worktree: `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018`.
- Branch: `feature/libreview-chart2-play-20261018`; existing PR: https://github.com/rafalbragan/-LibreDisplay/pull/46.
- Initial local/remote/PR HEAD verified: `9a3a0224babe3c3eab1a86235cbe9b97f0afef55`; PR open, no auto-merge, initial applicable checks successful.
- No new PR, force push, merge, auto-merge, physical Samsung operation, uninstall, app-data clear or Play publication.

## Compatibility changes / architecture review

1. Compile and target API 36; AGP **8.10.1**, checksum-pinned Gradle **8.11.1**, Robolectric **4.16.1**, **JDK 21** test runtime. Java/Kotlin app bytecode remains 17. Build-tools 35.0.0 are retained as supported by AGP 8.10.
2. Handle `MonitoringService.onTimeout(startId, fgsType)`: cancel polling, remove the foreground notification, stop the service promptly rather than exceed the system dataSync grace period. Existing periodic WorkManager synchronization remains scheduled; no quota bypass/restart loop.
3. Debug-only Compose host now uses edge-to-edge like MainActivity. New instrumentation checks real system back gestures and system/cutout insets. No production UI redesign or global predictive-back opt-out.
4. Update existing Android CI workflows and environment checks to API 36/JDK 21. New dedicated emulator runner verifies host process, AVD, serial and API before every ADB action.
5. New lint rule `CredManMissingDal` remains a visible **warning** for the previously documented external DAL prerequisite. No fictitious domain affiliation or blanket lint suppression. Optional passkeys are not claimed functional without DAL.

**EXISTS/preserved:** exact Abbott CSV parser, importer repository/ViewModels, explicit patient confirmation, transactional rollback, dedup/conflicts/idempotency, retention protection, SAF file/tree selection, Chart 2.0 and navigation. **INCOMPLETE → addressed:** API 36 tooling/coverage and FGS timeout handling. **MISSING/external:** verified Play upload-certificate registration and optional passkey DAL. Full assessment: [quality review](quality-review-report-2.17.2.md).

Room remains schema **3**; no schema or migration changes, no destructive migration. Importer, chart and schema source diff is empty. Demo Mode, Privacy & Data, API, widgets, statistics, retention/polling/storage/transfer/privacy/account-reset settings and NFZ behavior were not redesigned.

## Actual tests and build evidence

All evidence paths below are relative to the authorized worktree. Only synthetic data was used; no private export was supplied.

| Check | Actual result | Evidence |
|---|---|---|
| Complete JVM suite | **617 tests, 0 failures, 0 errors, 0 skipped**, 84 XML suites | `.delivery-evidence/api36-final-junit/`, `api36-jvm-lint-summary.json` |
| Parser / repository / ViewModels | **19 / 10 / 2 PASS**, included above | Exact Abbott aliases/ketones, private-metadata redaction, preview, confirmation/hash, dedup, rollback, repeat, retention, person isolation |
| DB version/registration / explorer math / new timeout tests | **7 / 6 / 2 PASS**, included above | New timeout cases run with Robolectric API 36 |
| `clean` | **PASS** in initial guarded run | `api36-full-validation-stdout.log` |
| `testDebugUnitTest`, `lint`, `assembleDebug`, `assembleDebugAndroidTest` | **PASS**, final JDK 21 run, exit 0 | `api36-jdk21-validation-{boundary,stdout,stderr,exit}.log` |
| Lint | **0 errors, 275 warnings, 9 hints** | `app/build/reports/lint-results-debug.xml` / `.html`; not warning-free |
| API 36 delivery instrumentation | **9/9 PASS**, 67.658 s | `.delivery-evidence/api36-complete-delivery-instrumentation.txt` |
| Room migration instrumentation | **2/2 PASS**, 0.558 s, migrations 1→2 and 2→3 | `.delivery-evidence/api36-migrations-instrumentation.txt` |
| Home instrumentation | **1/1 PASS**, 3.619 s | `.delivery-evidence/api36-home-instrumentation.txt` |
| `assembleRelease`, `bundleRelease` | **PASS**, exit 0, 4m 10s | `.delivery-evidence/api36-signing-build-*.log` (allowlisted output, no secrets) |
| Workflow syntax | **actionlint PASS** | `.delivery-evidence/api36-actionlint.log`; shellcheck/pyflakes not run |

Device: **`emulator-5582` / `LibreCare_API36_20261019`**, Android **36**, gesture navigation. Verified: real file/folder picker, no-write preview, explicit profile assignment, duplicates/conflicts, repeat-import zero new rows, retained history/provenance; fullscreen pinch/pan, multi-year queries, exact local-time tooltip, range preservation, person isolation, landscape, dark/large-font, both system back gestures, system-bar/cutout clearance.

Earlier failures are preserved, not counted as passes: JDK 17 cannot execute the API 36 Robolectric sandbox; the new DAL lint finding initially failed lint. Three emulator attempts were obstructed by a System UI ANR (and recorded Pixel Launcher input timeout). After recovering only the owned emulator and closing the exact System UI ANR outside the tests, one file-preview timeout remained; the isolated file test passed, followed by the full **9/9** suite above. No test/assertion was removed or weakened. See the quality report for evidence and reproducibility risk.

## Verified artifacts

| Artifact | Absolute path | Exact bytes | SHA-256 |
|---|---|---:|---|
| DEBUG APK | `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018\release-artifacts\LibreCare-2.17.2-debug.apk` | **24,110,791** | `39069CFD735111FF8B95E53A1808A0CA430CE8BFC44528401A3A16FEB927D69E` |
| RELEASE APK | `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018\release-artifacts\LibreCare-2.17.2-release.apk` | **3,618,687** | `60293A9282178E3576CAB16F1F7A4939CCA7FDF9105D33FD03E49F2D358E6EED` |
| RELEASE AAB / GOOGLE PLAY UPLOAD FILE | `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018\release-artifacts\LibreCare-2.17.2-release.aab` | **6,671,711** | `7AF8978061F0CE94196BA8ADC7E13E4BBDE88D4515FEA2A42412269D1586D0D0` |

- **Actual AAB manifest**, read by official bundletool 1.18.3: `com.libredisplay`, versionName **2.17.2**, versionCode **53**, targetSdk **36**, minSdk **26**. `bundletool validate`: PASS.
- JAR signature verified by both Java JarFile payload verification (**526 signed payload entries**) and `jarsigner -verify`. Signer SHA-256: **`14D768C2ECB104A48F239B3E9BC2518D09AD1FD2041C22BEE42E9608C90655DE`**.
- APK manifests/signatures independently checked using aapt/apksigner; release is non-debuggable and signed by the same upload certificate. Debug uses Android Debug certificate `962CB41FD3F7FF5DD9C64D71385F459EEBF7A8A2446E06CCB02E35572958BD07` and version suffix `-debug`.
- Bundle requests **PAGE_ALIGNMENT_16K**; all **8** native libraries have ELF LOAD alignment ≥16 KB. This is static compatibility verification, not a physical 16 KB device run.
- Self-signed/no-timestamp JAR certificate warnings are recorded; they do not negate the cryptographic verification and are not proof of Play registration.
- Verification: `.delivery-evidence/api36-bundle-validation-1792477287.json`, `api36-apk-verification.json`. Named AAB equals the actual Gradle output byte-for-byte.
- Signing used the already verified local upload key through process-scoped environment only. Main-checkout local configuration and keystore remained byte-identical; no secrets were printed or written to reports.
- Build ran before committing the focused changes. `.delivery-evidence/api36-signed-source-snapshot.txt` records the actual app/Gradle source-file hashes; these match the verified working tree. Initial HEAD is not misrepresented as already containing the API 36 changes. Final Git/CI verification is recorded in the PR delivery follow-up/final response.

## Remaining Google Play / product checks

1. **Registered Play upload certificate remains unverified.** Compare the Play Console Upload Key SHA-256 with the signer above. It is a different role from the Play App Signing certificate; do not change Google's app-signing key.
2. Manually upload the verified AAB to Internal Testing only after owner review. Complete any current health/data-safety/foreground-service/store declarations and account requirements. No Play upload or acceptance was performed here.
3. Emulator System UI/timing instability required recovery/retries. Final suite passes, but a wider device/foldable/tablet matrix and six-hour background soak are not claimed. Background quotas and notification permissions still apply.
4. Optional passkeys require verified DAL; private real-CSV integration remains unverified. These are existing limitations, not hidden by the API upgrade.

## Documentation and final compliance checklist

- Release notes (PL/EN): `docs/releases/LibreCare-2.17.2-release-notes.md`.
- Changelog (PL/EN): `CHANGELOG.md`, 2.17.2 at top.
- Quality/architecture review: `docs/quality-review-report-2.17.2.md`.

| Mandatory rule / gate | Status |
|---|---|
| Verify existing implementation; root cause; preserve functionality; bounded scope | PASS |
| Affected architecture/ViewModels/repositories/Room/API/Demo/privacy/statistics/widgets/charts/screens/navigation review | PASS — affected paths reviewed; unrelated behavior unchanged |
| UI readability/scaling/dark/accessibility/insets/phone/landscape and chart interaction review | PASS — scoped automated evidence above; broader manual accessibility audit not claimed |
| NFZ-specific changes and new statistics/settings features | NOT APPLICABLE — unchanged |
| Room version/migration changes | NOT APPLICABLE — no schema change; existing migration tests PASS |
| New/updated tests; complete JVM, repository/ViewModel and relevant device tests | PASS |
| Required clean/test/lint/debug/release/bundle tasks and available-emulator tests | PASS — final results above; earlier failed attempts retained |
| Named APK/AAB, full paths/sizes/hashes, Google Play upload file identified | PASS |
| Version review/increase and owner confirmation of unused code | PASS |
| PL/EN release notes/changelog, release/quality reports, remaining risks | PASS |
| LibreCare branding; legacy package/database/documentation/generated references classified | PASS |
| Protected checkout/no Samsung/no uninstall or data clear/no new PR/merge/publication | PASS |
| `.gitignore` excludes builds, APK/AAB, keystores, local.properties and local evidence | PASS |
| Commit/push existing branch, remote/PR SHA and final Actions | Pending at report creation; verify in final Git/CI follow-up before declaring delivery complete |
| Google Play acceptance/publication | NOT APPLICABLE to local validation — external acceptance remains unverified; publication prohibited |

## Uzupełnienie przeglądu gotowości — 2026-11-02 (PR-y #55–#58)

Uzupełnienie dotyczy aktualnych HEAD-ów PR-ów wymienionych poniżej, a nie historycznej kompilacji lokalnej i podpisanego AAB opisanego wcześniej w tym pliku. Twierdzenia wcześniejszego raportu dotyczące Play Console, wersji i artefaktów nie zostały niezależnie zweryfikowane dla tego zestawu PR-ów. **Ogólna decyzja o gotowości wydania: HOLD.** Żaden PR nie został scalony, nie włączono auto-merge ani nie opublikowano pakietu.

### 1. Stan PR

Zweryfikowana baza: `origin/master` `5538083662bdf42b4703bac0ca94ff5e73888363`. Wszystkie cztery PR-y są oparte na tej bazie.

| PR | HEAD podczas przeglądu | Zakres / CI | Instrumentacja i blokada |
|---|---|---|---|
| #55 | `6e01ced00b26b3877ba5e7a90744d0bc8cade026` | Konfiguracja Test Lab dla API 36, deterministyczne sprawdzenie trybu nawigacji gestami, zbieranie wyników testów i uporządkowanie faz testów odzyskiwania procesu. Szybki zestaw testów PASS (`41454757132`); kompilacja APK i APK androidTest PASS w `41454781723`. | Testy BackHandler oraz gestów krawędziowych/insets PASS na obu modelach API 36 i API 35. Pełna macierz FAIL: trzy istniejące testy odzyskiwania procesu nie przechodzą na żadnym urządzeniu, ponieważ PID aplikacji pozostaje bez zmian; zob. tabelę testów urządzeniowych. |
| #56 | `0a4468345859db7c32e3361a766405ca15b2d282` | Ciągłość linii wykresu pełnoekranowej historii i testy regresji agregacji. Szybki zestaw testów, debug APK i kontrole kompilacji PASS (`41444874726`, `41444874656`, `41444874746`). | Nie uruchomiono instrumentacji ani nie przeprowadzono kontroli wizualnej na tym HEAD-zie PR. Zrzuty z #55 nie weryfikują zachowania linii i agregacji wprowadzonych w tym PR-ze. |
| #57 | `b7e70af61b82bf3e4733d22186d96bc69b5b8986` | Nawigacja po oknach historii dashboardu i obsługa wyłącznie poziomego przeciągania. Szybki zestaw testów, debug APK, opcjonalna kompilacja release APK i kontrole kompilacji PASS (`41444203964`, `41444203958`, `41444203954`). | Dodany test interakcji Compose nie został uruchomiony na tym HEAD-zie PR. Szybki zestaw testów nie obejmuje jego źródeł AndroidTest. |
| #58 | `fb9f9dee0a2b9b393791d80585850575058def4e` | Zabezpieczony, ręcznie uruchamiany workflow podpisanego AAB i checklista wydania w Play. Składnia YAML została poprawnie sparsowana; kontrole CI PR PASS (`41426572193`, `41426572019`, `41426572383`). | Podpisany AAB nie został zbudowany; sekrety podpisujące, historia wersji w Play Console i zgodność certyfikatu uploadu nie zostały zweryfikowane. |

Kontrola drzewa scalania PR56, a następnie PR57, względem zweryfikowanej bazy nie wykazała konfliktów. Osobna kontrola uwzględniająca PR55 przed PR57 wykazała konflikt treści w `app/src/androidTest/java/com/libredisplay/delivery/DeliveryInstrumentedTest.kt`; przed scaleniem PR57 należy uzgodnić z ostatecznym stanem PR55. Były to wyłącznie próbne kontrole drzewa, a nie scalenia na GitHubie.

### 2. Testy urządzeniowe i zrzuty

| Test / dowód | Status | Konfiguracja / wynik |
|---|---|---|
| `historyBackHandlerRespondsToDeterministicSystemBack` | **PASS** | Uruchomienia Firebase Test Lab `41448329224` i `41454781723`: każdy test wykonano na `MediumPhone.arm` API 36, `MediumPhone_ps16k_backcompat.arm` API 36 i `SmallPhone.arm` API 35. W dwóch macierzach z raportami XML zaliczone zostały po cztery wykonania każdego testu na API 36. |
| `historyBackGestureAndSystemInsetsRemainCorrect` | **PASS** | Te same macierze i wyniki. Przed gestem od krawędzi test ustawia i sprawdza bezpieczne ustawienie `navigation_mode=2`; nie odnotowano błędu BackHandler, gestu systemowego ani insetów. |
| Pełny zestaw testów instrumentacyjnych | **FAIL** | W obu macierzach raportowanych przez XML: 24/27 testów zaliczonych na każdym z trzech urządzeń. Testy `test02_afterProcessRestart_legacyInactiveSelectionIsRecoverable`, `test04_afterProcessRestart_lastGoodSelectionStarts` i `test06_afterProcessRestart_staleSelectionDoesNotCrash` kończą się błędem w `verifyNewProcess`: zapisany i bieżący PID są identyczne. Zmiana kolejności testów ujawniła, że scenariusze te faktycznie nie kończą ani nie uruchamiają ponownie procesu aplikacji. Asercje pozostawiono bez zmian; wada projektu tych testów nadal uniemożliwia uzyskanie zielonej macierzy. |
| Generowanie APK / APK androidTest | **PASS** | Zakończone na hostowanym runnerze dla najnowszej macierzy; nie oznacza to, że testy przeszły. |
| Zrzuty ekranu wykresów | **CZĘŚCIOWO / HOLD** | Uruchomienie `41454781723`, artefakt `librecare-firebase-testlab-log`, zawiera 21 zrzutów z danymi syntetycznymi (po 7 na urządzenie) oraz XML/logcat dla każdego urządzenia. Dostępne są zrzuty widoku dnia, całej historii, roku w orientacji poziomej oraz motywu ciemnego/dużego tekstu. Zrzut dnia ze standardowego urządzenia API 36 pokazuje **0 odczytów i pusty wykres**, podczas gdy drugi model API 36 i urządzenie API 35 pokazują 97 odczytów dla tego samego scenariusza testowego. Zrzut całej historii jest wizualnie przeładowany. Rozbieżność nie została wyjaśniona; ponadto obrazy pokazują wykres z PR55/bazowej wersji, a nie zmieniony wykres z PR56. Nie wykonano zrzutów tygodnia ani miesiąca, a roku nie uchwycono w obu orientacjach. |
| Instrumentacja interakcji gestów dashboardu PR57 | **NOT RUN** | Nowego testu interakcji nie ma w wynikach uruchomienia PR55 i nie wykonano go na HEAD-zie PR57. |
| Lokalnie podłączone urządzenie/emulator | **NOT RUN** | Lokalnie nie było Android SDK, `adb`, emulatora ani podłączonego urządzenia. Wszystkie wykonane testy urządzeniowe przeprowadzono na wirtualnych urządzeniach Firebase; nie użyto fizycznego Samsunga. |

Wcześniejsza próba Firebase `41445759468` zatrzymała się na konfiguracji Android SDK (`sdkmanager` nie znalazł pakietu `tools`) przed uruchomieniem testów. PR55 jawnie instaluje teraz `platform-tools`, a późniejsze kompilacje dotarły do wszystkich trzech urządzeń wirtualnych. Najnowsza macierz nadal nie przechodzi z powodu opisanych wyżej testów odzyskiwania procesu, a nie z powodu któregokolwiek z dwóch wymaganych testów Back.

### 3. Gotowość Google Play

- Aktualna tożsamość Gradle: `applicationId=com.libredisplay`, `versionCode=53`, `versionName=2.17.2`, `targetSdk=36`; minifikacja release i zmniejszanie zasobów są włączone. Poprzednia wersja w źródłach to 2.17.1/52, ale podczas tego przeglądu nie sprawdzano Play Console.
- Manifest deklaruje uprawnienia sieciowe i stanu, powiadomień, startu urządzenia oraz usługi pierwszoplanowej synchronizacji danych. W PR-ach #55–#58 nie zmieniono manifestu ani zachowania dotyczącego danych medycznych. Schemat Room i migracje pozostały bez zmian; istniejące testy instrumentacyjne migracji przeszły w macierzach, choć cała macierz nie uzyskała wyniku pozytywnego.
- Workflow/YAML PR58 można uruchomić ręcznie wyłącznie z `master`; używa uprawnień repozytorium tylko do odczytu, sprawdza obecność nazw czterech sekretów podpisujących bez wypisywania ich wartości, zapisuje keystore w pliku tymczasowym usuwanym przez procedurę porządkowania, buduje `:app:bundleRelease`, weryfikuje podpis AAB, zapisuje SHA-256 i commit źródłowy, a następnie przesyła wyłącznie artefakt. Nie publikuje ani nie promuje wydania w Play. Są to kontrole statyczne/CI, a nie test podpisanego pakietu.
- **AAB: ZABLOKOWANY / NIE ZBUDOWANO.** Nie uruchomiono poprawnej konfiguracji podpisu na chronionym `master`; w ramach tego przeglądu nie utworzono AAB ani pliku do przesłania do Play. Zaliczone opcjonalne zadanie CI budujące release APK nie stanowi dowodu zbudowania podpisanego AAB dla Play.
- Dostęp do Play Console był niedostępny. Nie zweryfikowano najwyższego dotychczas przesłanego `versionCode`, certyfikatu klucza uploadu, relacji z Play App Signing, deklaracji Data safety/danych zdrowotnych, strony sklepu ani stanu polityki prywatności. Nie należy wyciągać tych wniosków z tego ani z wcześniejszego raportu. Nie utworzono PR-a zwiększającego wersję ani nie wygenerowano czy nie zastąpiono klucza podpisującego.
- Test instalacji/aktualizacji z poprzedniej wersji z zachowaniem danych: **NOT RUN**. Nie zbudowano ani nie zainstalowano kandydata wydania. Nie potwierdzono deklaracji prawnych/prywatności w Play Console.

### 4. Wykonane zmiany

- Commity PR55: `7bf6e0d` (konfiguracja SDK API 36 i jawny tryb gestów), `30976bf` (zachowanie diagnostyki XML/logcat z Test Lab), `6e01ced` (deterministyczna kolejność faz testów odzyskiwania procesu i zrzuty syntetycznych wykresów). Ostatnia zmiana nie naprawiła bazowego błędu testu śmierci procesu; uczyniła go powtarzalnym i jawnym.
- Commity PR56: `92ad988`, `0a44683` (pokrycie granic agregacji i poprawka API).
- Commit PR57: `b7e70af` (obsługa poziomego przeciągania oraz testy regresji przesuwania i interakcji).
- W tym uzupełnieniu nie zmieniono kodu workflow ani checklisty PR58. Ten raport jest jedyną proponowaną zmianą uzupełniającą dla PR58.
- Nie zmieniono schematu Room, obliczeń medycznych, progów glikemii, dawkowania insuliny, danych pacjentów ani integracji LIVE.

### 5. Co pozostaje i właściciel

1. **Osoba odpowiedzialna za testy Android:** przeprojektować trzy testy P0 odzyskiwania procesu tak, aby każdy z nich wykonywał i weryfikował rzeczywiste zakończenie procesu, bez polegania na kolejności metod; ponownie uruchomić pełną macierz API 36. Nie usuwać asercji PID ani nie określać odtworzenia aktywności jako śmierci procesu.
2. **Osoba odpowiedzialna za wykres:** odtworzyć pusty wykres dnia na `MediumPhone.arm` API 36; wykonać zrzuty dnia/tygodnia/miesiąca/roku w orientacji pionowej i poziomej na HEAD-zie PR56; ocenić na nich czas agregacji, luki, oznaczenia skanów/min/max, skalowanie etykiet i wybór punktu. Obecne zrzuty nie potwierdzają poprawności wizualnej PR56.
3. **Osoba odpowiedzialna za dashboard:** uruchomić testy interakcji AndroidTest na HEAD-zie PR57, obejmujące kierunki nawigacji i granice, wybór punktu, otwarcie pełnego ekranu, przewijanie pionowe i powrót do najnowszych danych. Po ustaleniu kolejności scalania uzgodnić konflikt we wspólnym pliku testowym.
4. **Product Owner / administrator Play Console:** potwierdzić najwyższy przesłany `versionCode` i odcisk certyfikatu uploadu, następnie zatwierdzić wyższą wersję i skonfigurować wymagane sekrety podpisujące GitHub bez ujawniania ich wartości.
5. **Osoba odpowiedzialna za wydanie:** po ręcznym scaleniu PR-ów i spełnieniu wcześniejszych bramek uruchomić workflow podpisanego AAB z chronionego `master`, sprawdzić artefakt/podpis/SHA, wykonać niedestrukcyjny test aktualizacji i potwierdzić deklaracje Play. Raport nie upoważnia do przesłania ani promowania wydania.

### 6. Rekomendacja doradcza dla Product Ownera

| PR | Rekomendacja | Uzasadnienie |
|---|---|---|
| #55 | **HOLD** | Dwa testy Back przechodzą wielokrotnie, ale pełna macierz urządzeniowa nie zalicza trzech testów odzyskiwania procesu. |
| #56 | **HOLD** | Kontrole JVM/CI przechodzą; brakuje wymaganej walidacji wizualnej/urządzeniowej na zmienionym HEAD-zie PR. |
| #57 | **HOLD** | Kontrole JVM/CI przechodzą; nie uruchomiono instrumentacji interakcji, a integracja wspólnego pliku testowego wymaga uzgodnienia. |
| #58 | **ACCEPT wyłącznie dla infrastruktury bramek wydania** | Statyczne kontrole workflow/checklisty przechodzą i nie ma automatycznej ścieżki publikacji. Nie oznacza to akceptacji AAB, wersji ani wydania w Play. |

Są to wyłącznie rekomendacje; tylko Product Owner może podjąć decyzję ACCEPT/HOLD/REJECT.

### 7. Kolejność ręcznego merge

Nie rekomenduje się scalania przed rozwiązaniem wskazanych wyżej blokad HOLD. Jeśli Product Owner zaakceptuje później odpowiednie zmiany: można niezależnie scalić #58; następnie scalić #56; zaktualizować #57 względem nowego `master` i uzgodnić stan wspólnego `DeliveryInstrumentedTest.kt`; ponownie uruchomić wymagane kontrole, po czym ręcznie scalić #57. Przed scaleniem #55 uzgodnić jego wspólny plik instrumentacyjny z ostatecznym stanem #57 i ponownie uruchomić macierz API 36. Nie scalać automatycznie.

### Kontrola obowiązkowa — stan tej finalizacji

| Reguła | Status |
|---|---|
| Weryfikacja implementacji, PR HEAD i bazowego `master` | PASS |
| Czyste, osobne worktree; checkout użytkownika nietknięty | PASS |
| Nie zmieniono `master`, ochrony gałęzi ani nie użyto auto-merge/scalania | PASS |
| Testy jednostkowe/lint/debug CI dla #56/#57/#58 | PASS |
| Instrumentacja BackHandler i gestów API 36 | PASS |
| Pełna instrumentacja urządzeniowa | FAIL — trzy testy procesu odzyskiwania |
| Pełna wizualna kontrola zakresów dzień/tydzień/miesiąc/rok w obu orientacjach na PR56 | FAIL — dowody niepełne, jeden obraz dnia API 36 pusty |
| Test interakcji PR57 na jego HEAD | NOT RUN |
| Migracje Room | NOT APPLICABLE — schemat bez zmian; istniejące testy migracji uruchomiono |
| Podpisany i zweryfikowany AAB; plik do przesłania do Google Play | FAIL — nie zbudowano; Play/klucz/versionCode niepotwierdzone |
| Brak sekretów wydania w raporcie/logach; brak fizycznego urządzenia Samsung | PASS |
| Pełna gotowość do wydania Google Play | FAIL — status HOLD |
