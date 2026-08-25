# LibreCare — Decision Log (human-readable)

This is the narrative log of meaningful product decisions. Structured, machine-readable decision
records live in `product/decisions/` and `product/examples/` and are validated by the product CLI.
Rejected and HOLD decisions are kept here on purpose so ideas are not "rediscovered" as new.

Decision statuses: `ACCEPTED` · `HOLD` · `REJECTED` · `SUPERSEDED`.

---

## D-0001 — Autonomous insulin-dose recommendation is prohibited
- **Date:** 2026-08-25
- **Status:** ACCEPTED (standing prohibition)
- **Decision:** LibreCare will not autonomously calculate or recommend an individual insulin dose
  from AI, CGM trend, or incomplete context.
- **Reason:** Patient safety and regulatory boundary. A wrong dose suggestion can cause harm.
- **Counterargument considered:** "Trend-based nudges could help." Rejected because dose specificity
  crosses into clinical decision-making without a clinician-approved plan.
- **Revisit condition:** Only within a separate, clinician-approved correction-plan
  safety/regulatory project (see `SAFETY_GUARDRAILS.md` G4).
- **Structured record:** `examples/decision-no-autonomous-dosing.example.json`.

## D-0002 — LibreCare is one app with three perspectives (Caregiver primary)
- **Date:** 2026-08-25
- **Status:** ACCEPTED
- **Decision:** Adopt CAREGIVER / SENIOR / CLINICIAN as three perspectives over shared data, with
  CAREGIVER as the primary direction.
- **Reason:** Distinct core questions and cognitive-load needs, but shared CGM data/safety.
- **Revisit condition:** If usage evidence shows a fourth perspective is warranted.

## D-0003 — Mode differences via centralized presentation policy, not scattered `if (mode == ...)`
- **Date:** 2026-08-25
- **Status:** ACCEPTED (architectural rule)
- **Decision:** Future mode behaviour is expressed through a centralized mode-aware presentation
  policy provider; screens render from a policy rather than branching on mode.
- **Reason:** Testability, consistency, and keeping shared safety rules from diverging per mode.
- **Structured detail:** `MODE_ARCHITECTURE.md`.

## D-0004 — Phase 1 does not change production UI
- **Date:** 2026-08-25
- **Status:** ACCEPTED
- **Decision:** Phase 1 delivers product infrastructure only; no production Kotlin/Compose change,
  no in-app mode switching, no new medical functionality.
- **Reason:** Keep Android CI and Firebase Test Lab baseline green; separate infra from product change.
- **Revisit condition:** Phase 3 begins only on explicit human go-ahead.

---

Add new decisions above this line in reverse-chronological order, and mirror meaningful ones as
structured records in `product/decisions/`.

