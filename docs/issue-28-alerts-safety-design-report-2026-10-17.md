# LibreCare - Issue #28 - raport projektowy alarmow glikemii (2026-10-17)

## 1. Stan bazowy i autoryzacja

- Repo bazowe: `origin/master` @ `ec32f1eeb53e35ec208a5f72d30db2dc1bb74323`.
- PR #33: scalony do `master` (REQ-0004).
- Issue #28: OPEN, etykieta `product-inbox`, charakter `SAFETY_GAP candidate`.
- Decyzja PO sformalizowana:
  - ACCEPT: analiza techniczna + projekt + wymagania bezpieczenstwa + plan testow.
  - HOLD: implementacja zmian zachowania alarmow do osobnej zgody PO.

## 2. Zapis decyzji w Product Foundation

- Inbox: `product/inbox/INBOX-GH-000028.json` (status: `HOLD`, z opisem zakresu ACCEPT/HOLD).
- Review queue: `product/review/REVIEW_QUEUE.yaml` -> `CAND-INBOX-GH-000028`.
- Decyzje strukturalne:
  - `product/decisions/DEC-0009-accept-issue28-alert-analysis-design.json` (ACCEPTED).
  - `product/decisions/DEC-0010-hold-issue28-alert-behavior-implementation.json` (HOLD).
- Log narracyjny:
  - `product/DECISION_LOG.md` (D-0006 i D-0007).

## 3. Audyt aktualnej implementacji alarmow

### 3.1 Podlaczenie `GlucoseAlertCoordinator` do przeplywu danych

**Potwierdzone**
- `GlucoseAlertCoordinator` istnieje (`app/src/main/java/com/libredisplay/alert/GlucoseAlertCoordinator.kt`).
- W repo nie ma instancjonowania ani wywolania `GlucoseAlertCoordinator(...)` poza definicja klasy.
- W efekcie pipeline alarmowy nie jest podlaczony do `MonitoringService`, `LibreDisplaySyncWorker` ani `MonitoringViewModel`.

**Wniosek**
- To potwierdzony defekt architektoniczny: komponent alarmowy jest obecny, ale nieaktywny w przeplywie runtime.

### 3.2 Kanaly notyfikacji i POST_NOTIFICATIONS

**Potwierdzone**
- Manifest deklaruje `android.permission.POST_NOTIFICATIONS`.
- Istnieja dwa kanaly:
  - `glucose_monitoring` (IMPORTANCE_LOW) tworzone w `LibreDisplayApp.createNotificationChannel()`.
  - `glucose_alerts` (IMPORTANCE_HIGH) tworzone przez `AndroidNotificationDispatcher.ensureChannels(...)`.
- `AndroidNotificationDispatcher.ensureChannels(...)` nie jest wywolywane nigdzie w aplikacji.
- Nie znaleziono runtime requestu `POST_NOTIFICATIONS` (Android 13+), ani jawnego checku `areNotificationsEnabled()`.

**Wniosek**
- Potwierdzony defekt: kanal alarmowy nie ma gwarantowanej inicjalizacji.
- Potwierdzony defekt: brak obslugi runtime permission dla notyfikacji.

### 3.3 Tlo, opoznienia, utrata polaczenia

**Potwierdzone**
- Foreground `MonitoringService` startuje z `MainActivity.onStart()` przez `MonitoringServiceController.startIfEnabled(...)`.
- `MonitoringService` odpyta `glucoseRepository.fetchLatestReading()` co `refreshInterval` (30..300s) i aktualizuje widget.
- WorkManager (`LibreDisplaySyncWorker`) dziala okresowo co `backgroundPollingMinutes` (15..60 min), przy `NetworkType.CONNECTED` i `batteryNotLow`.
- `BootReceiver` uruchamia `MainActivity` po reboocie; nie uruchamia bezposrednio pipeline alarmowego.
- Brak kodu, ktory w trybie background/worker wywoluje alarm coordinator po nowych odczytach.

**Wniosek**
- Potwierdzony brak alarmowego toru end-to-end dla danych w tle.
- Potwierdzone ryzyko opoznienia: fallback WorkManager ma interwal minimum 15 min, co moze byc za wolne dla alarmu safety-critical.

## 4. Potwierdzone defekty vs hipotezy

## Potwierdzone defekty
1. `GlucoseAlertCoordinator` nie jest podlaczony do zadnego przeplywu danych runtime.
2. `AndroidNotificationDispatcher.ensureChannels()` nie jest wywolywany.
3. Brak runtime obslugi `POST_NOTIFICATIONS` dla Android 13+.
4. Brak testow alarmowych unit/integration/device dla obecnej implementacji.
5. No-op implementacje audio/TTS/wibracji (`NoOp*`) oznaczaja brak faktycznej sygnalizacji alarmu nawet przy wyzwoleniu coordinatora.

## Hipotezy (wymagaja testow urzadzeniowych)
1. Na czesci urzadzen alarm moze byc nieslyszalny przez ustawienia kanalu/DND/OS battery policy.
2. Opoznienia pobierania danych (429, utrata sieci, Doze) moga powodowac brak timely alert.
3. Autostart po BOOT przez uruchamianie Activity moze byc nieskuteczny na wybranych OEM/ROM.

## 5. Docelowa architektura (propozycja do osobnej decyzji implementacyjnej)

## Cel
Zapewnic deterministyczny pipeline: *new reading -> alert evaluation -> notification/audio/vibration -> telemetry*.

## Proponowane komponenty
1. `AlertEvaluationService` (warstwa domenowa, bez Android API)
   - wejscie: `GlucoseReading`, ustawienia alertow, kontekst swiezosci,
   - wyjscie: `AlertDecision` (NONE/TRIGGER/SUPPRESS + reason).
2. `AlertOrchestrator` (warstwa aplikacyjna)
   - podpiety do miejsc, gdzie pojawia sie nowy odczyt (`MonitoringService`, sync, manual refresh),
   - zapewnia deduplikacje/ack/escalation cooldown.
3. `AlertDeliveryGateway` (Android)
   - kanal `glucose_alerts`, notyfikacja heads-up,
   - audio/wibracja/TTS z fallback i logowaniem rezultatu.
4. `AlertPermissionGuard`
   - check + flow prosby o `POST_NOTIFICATIONS`,
   - stan uprawnien raportowany do UI i diagnostyki.
5. `AlertTelemetryStore`
   - zapis: evaluation reason, delivery status, permission state, latency end-to-end.

## Integracja proponowana
- `MonitoringService`: po kazdym nowym odczycie wywolanie `AlertOrchestrator`.
- `GlucoseSyncRepository`: po zapisie nowych odczytow wyzwolenie oceny alarmu dla najnowszego punktu per osoba.
- `MainActivity`: startup guard dla permission i statusu kanalow.
- `LibreDisplayApp`: jawna inicjalizacja `glucose_alerts` przy starcie.

## 6. Kryteria akceptacji (dla osobnej decyzji implementacyjnej)

1. Alarm ocenia tylko swieze dane (zasada freshness jawnie testowana).
2. Dla kazdego triggera istnieje audytowalny `AlertDecision` i status dostarczenia.
3. Kanal `glucose_alerts` jest utworzony i zweryfikowany przy starcie aplikacji.
4. Dla Android 13+ aplikacja obsluguje `POST_NOTIFICATIONS` i komunikuje stan odmowy.
5. Brak podwojnych alarmow dla tego samego poziomu bez nowego crossing event.
6. Zachowanie przy braku sieci/429 jest jawne: alert moze byc suppressed z powodowymi kodami.
7. Brak zmian progow medycznych bez odrebnej decyzji PO.

## 7. Plan testow

## Unit tests
- Mapowanie poziomow alertu i crossing logic.
- Freshness gate (np. >5 min -> suppress z reason).
- Deduplikacja i cooldown/ack.
- Permission guard state machine.

## Integration tests (JVM/Robolectric)
- `MonitoringService` -> `AlertOrchestrator` po nowym odczycie.
- `GlucoseSyncRepository` -> alert evaluation po sync.
- Tworzenie i konfiguracja kanału `glucose_alerts`.
- Zachowanie przy 429 i `retry_after` (suppress reason + telemetry).

## Device tests
- Android 13/14/15: flow `POST_NOTIFICATIONS` (allow/deny).
- DND, battery optimization, OEM background restrictions.
- Alarm przy foreground i background (screen on/off).
- Scenariusze utraty lacznosci i reconnect.
- Opoznienie end-to-end: pobranie -> alarm (SLO).

## 8. Analiza ryzyka

- **R1 (wysokie):** false-negative alarm (brak triggera mimo crossing).
- **R2 (wysokie):** false-positive / duplikacja alarmow (alarm fatigue).
- **R3 (wysokie):** brak notyfikacji przez permission/channel mismatch.
- **R4 (srednie):** opoznienie danych (429/offline/Doze) z opoznionym alarmem.
- **R5 (srednie):** niespojnosc miedzy foreground service i worker.

## Mitigacje
- jawna telemetria decision/delivery,
- test matrix urzadzeniowy,
- staged rollout + kill switch dla warstwy delivery,
- brak zmian progow medycznych w tym zakresie.

## 9. Propozycja implementacyjna (do osobnej zgody PO)

Etap A (foundation bez zmiany progow):
1. Podlaczyc `AlertOrchestrator` do przeplywu nowych odczytow.
2. Wlaczyc realny delivery layer (notification + audio/vibration/TTS) z fallback.
3. Dodac permission flow i kanal `glucose_alerts` init/check.
4. Dodac telemetry + dashboard diagnostyczny alertow.

Etap B (twarde scenariusze background):
1. Ujednolicic trigger point pomiedzy service i worker.
2. Dodac explicit connection-loss handling policy dla alarmow.
3. Zweryfikowac latencje i niezawodnosc na urzadzeniach referencyjnych.

Etap C (hardening):
1. Regresje safety + chaos tests (offline/429/reboot).
2. Kryteria go/no-go dla produkcji.

## 10. Pytania decyzyjne do Product Ownera

1. Czy PO akceptuje wdrozenie Etapu A (podlaczenie orchestratora + permission/channel + telemetry) bez zmiany progow medycznych?
2. Czy wymagane jest minimum SLO dla opoznienia alarmu (np. p95 <= X sekund od nowego odczytu)?
3. Czy w przypadku braku `POST_NOTIFICATIONS` aplikacja ma wymagac jawnego banera safety na Home/Monitoring?
4. Czy dopuszczamy rollout etapowy (np. feature flag) dla warstwy audio/wibracji/TTS?
5. Czy policy dla braku lacznosci ma zawierac osobny "alarm braku swiezych danych" czy tylko status informacyjny?

---
Raport nie zawiera zmian produkcyjnego zachowania alarmow. Zakres implementacji pozostaje HOLD.

