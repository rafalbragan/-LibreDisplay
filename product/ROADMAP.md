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
- [ ] (Not started) Populate real observations from an exploratory review phase.

Exit criteria: `product validate` green; Android CI and Firebase Test Lab baseline unchanged.

## Phase 2 — Exploratory review & backlog population (requires human go-ahead)

- Structured exploratory testing per persona/module → real observation records.
- AI clustering/deduplication of observations → candidate requirements.
- Human triage: `ACCEPT` / `HOLD` / `REJECT` with recorded decisions.

## Phase 3 — Mode architecture groundwork (requires human go-ahead)

- Introduce `UserMode` + `ModulePresentationPolicyProvider` in a presentation layer.
- CAREGIVER policy reproduces today's UI (behaviour-preserving); tests stay green.

## Phase 4 — Per-module mode policies (requires human go-ahead, per module)

- SENIOR and CLINICIAN policies added module-by-module behind the provider.
- Context capture (meals/drinks/insulin/symptoms) with provenance — safety reviewed.

## Phase 5 — Mode selection & persistence (requires human go-ahead)

- User-facing mode selection and persistence, added only after policies exist.

## Explicitly deferred / guarded

- **Correction-plan / dose guidance:** separate clinician-approved safety/regulatory project only
  (see `SAFETY_GUARDRAILS.md` G1/G4). Not on this roadmap as a normal feature.

`FUTURE` items are ideas, not commitments, and never described as existing functionality.

