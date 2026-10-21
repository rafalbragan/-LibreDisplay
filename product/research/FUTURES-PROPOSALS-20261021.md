# LibreCare — preserved Futures proposals / zachowane propozycje

Date: 2026-10-21. Scope: [cleanup PR #47](https://github.com/rafalbragan/-LibreDisplay/pull/47).

## Provenance and status

All **10** original idea IDs were removed from the visible inventory by cleanup commit
`78302bf7906ce35c01625c66ce1a6cec78ebe8c8`. This record preserves their intent and next questions.
The exact original text, audiences and bullet lists remain available in
[the immutable PR #46 source](https://github.com/rafalbragan/-LibreDisplay/blob/8ce8cb2bb12789bff287be353ea19d8b8ec5abb0/app/src/main/java/com/libredisplay/ui/futures/FuturesViewModel.kt).

**PL:** To archiwum propozycji, nie decyzja o wdrożeniu. Dawne etykiety „Prototyp”,
„Gotowe do testu”, „Wymaga danych” i „Wymaga backendu” nie stanowią akceptacji PO.
Nie uznajemy sugestii ani częściowo podobnej funkcji za pełne wdrożenie pomysłu.

**EN:** These are UNDER_REVIEW proposals, not accepted implementation requirements. Related
existing code is not proof that the full proposal was delivered. No canonical requirement,
decision, issue status or research-result record is changed by this archive.

| Original ID / label | Preserved proposal (PL) | Remaining question / boundary | Relationship to current inventory |
|---|---|---|---|
| `analysis-prototype` / GOTOWE_DO_TESTU | Analiza+: większe wnioski; dowolny zakres 1–90 dni; przesuwanie o dzień/tydzień/miesiąc; mocniejsza linia średniej; przypięta kolumna opisów metryk. | Which exact layout changes remain useful after reviewing today's analysis screen? PO must approve a bounded UI scope; the old label did not approve implementation. | Analytics exists; this complete UI package is not claimed delivered. |
| `spike-explanations` / PROTOTYP | Wyjaśnienia skoków: możliwe powiązania z posiłkiem, stresem, aktywnością, chorobą i hormonami; kontekst zdarzenia; przyszłe wpisy posiłków/aktywności; wsparcie rozmowy pacjent–opiekun–lekarz. | Missing reliable context and evidence for causal attribution. Research wording, uncertainty and provenance; no inferred diagnosis or treatment advice. | Not represented as an available causal-explanation engine. |
| `hypo-risk` / PROTOTYP | Ryzyko hipoglikemii: pory występowania, kierunek zmiany ryzyka i wyjaśnienie sygnału; prosty komunikat dla seniora/opiekuna bez nadmiernego niepokoju. | Historical patterns are not validated forecasts. Separate safety review and PO decision required; no dosing or alert-policy change. | Monitoring exists, not a claim that this predictive proposal is validated. |
| `variability-patterns` / GOTOWE_DO_TESTU | Wzorce zmienności: rano/dzień/wieczór/noc, dni robocze/weekend; opis po polsku obok CV; ewentualne proste reguły przed bardziej złożoną interpretacją. | Verify coverage, time windows and interpretability before approving rule-based observations. | Metrics exist; complete narrative/pattern proposal remains unapproved. |
| `meal-response` / WYMAGA_DANYCH | Reakcja na posiłki: szczyt i czas powrotu, porównanie posiłków, materiał do konsultacji. | Minimum usable meal-entry model, source quality and confounding factors are unresolved. Do not rank foods as individualized medical advice. | Not delivered as a meal-context subsystem. |
| `weekly-report` / GOTOWE_DO_TESTU | Tygodniowa karta: podsumowanie, najlepsze dni/trudniejsze momenty, krótki komentarz i 2–4 wskazówki; potencjalnie osobna karta dla seniora/opiekuna. | Test whether a summary is useful and non-judgmental; any suggestions must remain non-treatment information. PO acceptance required. | Existing analytics is not this complete weekly narrative card. |
| `sensor-wear` / PROTOTYP | Aktywność sensora i luki: udział dostępnych danych, większe przerwy, kontekst nocy/długości/nietypowości; możliwe połączenie ze świeżością na ekranie głównym. | Gaps cannot establish whether a sensor was worn or a medical event occurred. Validate data coverage and wording first. | Freshness/coverage foundations exist; the full gap-classification view is not claimed available. |
| `achievements` / PROTOTYP | Osiągnięcia i serie: motywacja, serie związane z TIR/nocnymi spadkami, możliwość ukrycia, brak oceniania i presji. | Research opt-in desirability and potential pressure/shame or unsafe incentives before any implementation. | Not part of current capability inventory. |
| `share-doctor` / WYMAGA_BACKENDU | Udostępnienie lekarzowi: skrócony raport z okresu, komentarz do wizyty, możliwy QR lub czasowy link. | Minimum shared data, consent, expiry, access control and revocation need privacy review. Existing export does not establish a secure sharing service. | Analytics/export foundations do not deliver the complete proposal. |
| `role-screens` / PROTOTYP | Senior: większy tekst i prostota; Opiekun: status, świeżość i kontakt; Lekarz: trendy, zmienność, eksport i przygotowanie wizyty. Wspólna aplikacja, bez zmiany identyfikatora. | Three-mode direction and centralized policy are accepted (`D-0002`, `D-0003`), but full implementation/persistence needs a separately approved scope. See [issue #3](https://github.com/rafalbragan/-LibreDisplay/issues/3). | Retained visibly as `caregiver-senior-clinician-modes`, UNDER_REVIEW for implementation. |

## Proposed next work, not a new roadmap or approval

Keep the next sprint small: **backup integrity tests and alert-analysis evidence first**.
Do not start Discovery merely to rediscover already reported technical gaps.

| Priority / recommendation | Evidence / problem | Unresolved question and PO decision needed |
|---|---|---|
| P0 — TEST, then bounded FIX if approved | [PR #43](https://github.com/rafalbragan/-LibreDisplay/pull/43), `AppDataBackupRepository`, codec/merge/store tests. Backup mechanisms are not proof of full restore integrity. | Demonstrate non-empty preservation, malformed/partial backup handling, rollback and round-trip fidelity before deciding a separate fix scope. Keep PR #43 on HOLD; no merge recommendation from its title alone. |
| P0 — TEST / analysis, behavior FIX on HOLD | [Issue #28](https://github.com/rafalbragan/-LibreDisplay/issues/28); DEC-0009 accepts analysis/design; DEC-0010 holds alarm-behavior changes. | Collect refresh/delivery/stale-data scenarios and safety review. PO must separately ACCEPT any change to triggering, escalation or notification behavior. |
| P1 — TEST, then FIX only for reproduced gaps | [Issue #30](https://github.com/rafalbragan/-LibreDisplay/issues/30), [PR #36](https://github.com/rafalbragan/-LibreDisplay/pull/36): duplicate requests / 429 pressure. | Confirm current request counts and backoff under concurrency; decide only residual fix scope, not blind integration of an older branch. |
| P1 — TEST | [Issues #27](https://github.com/rafalbragan/-LibreDisplay/issues/27), [#2](https://github.com/rafalbragan/-LibreDisplay/issues/2), [#25](https://github.com/rafalbragan/-LibreDisplay/issues/25), PR #46 chart evidence. | Reproduce Home responsiveness, stale presentation and Home/history consistency with person/range changes. PO selects any remaining UX fix; chart code is unchanged here. |
| P1 — TEST | [Issue #26](https://github.com/rafalbragan/-LibreDisplay/issues/26), [PR #39](https://github.com/rafalbragan/-LibreDisplay/pull/39), `TESTRUN-2026-003`: 12h metrics/TIR and benign-demo coverage. | Verify sparse/boundary/low/high data against source values; decide fix only if discrepancy reproduced. No new user research required for arithmetic checks. |
| P2 — TEST / PO scope decision | [PR #42](https://github.com/rafalbragan/-LibreDisplay/pull/42): Mika DEMO proposal. | Determine remaining independence and synthetic coverage needs relative to PR #46 before selecting work. No new DEMO behavior in cleanup. |
| P2 — DISCOVERY proposal only | [Issue #3](https://github.com/rafalbragan/-LibreDisplay/issues/3), `MODE_ARCHITECTURE.md`, `role-screens`. | Which role priorities, switching expectations and preference persistence actually help users? PO must authorize research and later implementation separately. |

Discovery readiness: a **narrow, separately authorized** study of role priorities/preferences and
comprehension of uncertain summaries may be worthwhile. Backup corruption, request pressure,
metric arithmetic and known chart regressions need deterministic tests first, not a broad new
Discovery run. No Discovery or Product Review automation was executed for this cleanup.
