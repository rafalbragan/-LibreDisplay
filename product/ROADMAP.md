# LibreCare — Roadmap

Phased plan. Phase 1 is infrastructure only; later phases require explicit human decisions before
starting. Nothing here authorizes implementation by itself.

## Phase 1 — Product-management foundation (this task)

- [x] Product strategy and three-mode direction (`PRODUCT_STRATEGY.md`, `MODE_ARCHITECTURE.md`).
- [x] Machine-readable focus and personas (`CURRENT_FOCUS.yaml`, `PERSONAS.yaml`).
- [x] Safety guardrails (`SAFETY_GUARDRAILS.md`) — autonomous insulin dosing explicitly forbidden.
- [x] Real module inventory from the codebase (`MODULE_INVENTORY.md`, `MODULE_MODE_MATRIX.md`).
- [x] Requirements Collector schemas + templates + examples.
- [x] Deterministic `product_cli.py` (`validate`, `summary`) + `product-quality` CI.
- [ ] Populate additional exploratory observations from a new review cycle when a human explicitly authorizes the next discovery step.

Exit criteria: `product validate` green; Android CI and Firebase Test Lab baseline unchanged.

## Cleanup snapshot — 2026-10-21 (separate from foundation Phase 1)

The PO authorized this focused cleanup, not implementation of the remaining proposals.
Baseline: [PR #46](https://github.com/rafalbragan/-LibreDisplay/pull/46),
`8ce8cb2bb12789bff287be353ea19d8b8ec5abb0`, LibreCare **2.17.2 / 53**.
Cleanup: [stacked PR #47](https://github.com/rafalbragan/-LibreDisplay/pull/47).
Version is unchanged; this is not a release.

- Implemented on the target feature branch (not a completeness or medical-reliability claim):
  - monitoring / glucose overview
  - chart history / full-screen history interaction
  - LibreView CSV import
  - analytics and statistics
  - monitored-person profiles
  - privacy/data settings surface
- Under review / implementation authorization not established:
  - backup and restore integrity (mechanisms exist; open integrity gaps remain)
  - full caregiver / senior / clinician mode implementation and preference persistence
  - alerts and refresh behavior (analysis/design ACCEPT; behavior implementation HOLD,
    `DEC-0009` / `DEC-0010`)
  - 12h metrics and TIR accuracy review
- No inventory entry is currently labelled IN_DEVELOPMENT or PLANNED: no evidence of
  authorized active work or an explicit accepted future implementation scope was established
  for these gaps. Accepted three-mode direction/policy is not an implementation start decision.

**Separate delivery states:** PR #46 remains open; remote master is
`5433f28b04442d5dfb2af9777801109bee730b35`. Its complete 2.17.2 delta is not merged.
Google Play distribution was not independently verified. Existing human decisions remain
unchanged; neither branch implementation nor passing tests grants PO acceptance.

Evidence, test results and limits: [cleanup report](research/CLEANUP-20261021.md).
All ten replaced Futures ideas are retained in the
[proposal archive](research/FUTURES-PROPOSALS-20261021.md); legacy prototype labels do not grant approval.

## Phase 2 — Exploratory review & backlog population (requires human go-ahead)

- Structured exploratory testing per persona/module → real observation records.
- AI clustering/deduplication of observations → candidate requirements.
- Human triage: `ACCEPT` / `HOLD` / `REJECT` with recorded decisions.

## Phase 3 — Mode architecture groundwork (requires human go-ahead)

- Introduce `UserMode` + `ModulePresentationPolicyProvider` in a presentation layer.
- CAREGIVER policy reproduces today's UI (behavior-preserving); tests stay green.

## Phase 4 — Per-module mode policies (requires human go-ahead, per module)

- SENIOR and CLINICIAN policies added module-by-module behind the provider.
- Context capture (meals/drinks/insulin/symptoms) with provenance — safety reviewed.

## Phase 5 — Mode selection & persistence (requires human go-ahead)

- User-facing mode selection and persistence, added only after policies exist.

## Explicitly deferred / guarded

- **Correction-plan / dose guidance:** separate clinician-approved safety/regulatory project only
  (see `SAFETY_GUARDRAILS.md` G1/G4). Not on this roadmap as a normal feature.

`FUTURE` items are ideas, not commitments, and never described as existing functionality.
