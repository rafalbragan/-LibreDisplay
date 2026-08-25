# LibreCare — Safety Guardrails (binding product rules)

These rules are **binding** on every mode, every module, and every future feature. A perspective
(CAREGIVER / SENIOR / CLINICIAN) may change presentation, but **must not** relax any rule here.

## G1 — No autonomous insulin-dose recommendation (HARD PROHIBITION)

LibreCare **MUST NOT** autonomously calculate or recommend an individual insulin dose based on AI,
CGM trend, or incomplete context.

**Forbidden example (must never be produced):**

> "Glucose is rising rapidly. Take 3 units."

**Acceptable type of message:**

> "Glucose is rising rapidly. Check whether a meal or insulin was recorded and follow the
> established care plan."

This prohibition is explicit and non-negotiable in Phase 1 and remains in force until, and unless, a
future clinician-approved correction-plan project (see G4) is separately designed, reviewed, and
approved as a safety/regulatory effort.

## G2 — What LibreCare MAY do

LibreCare may eventually:

- detect rapid glucose rise/fall,
- notice missing context,
- ask contextual questions (answerable YES / NO / I DON'T REMEMBER),
- show patterns,
- summarize observations,
- show a previously configured care plan.

None of these may cross into telling the user a specific dose to take.

## G3 — Provenance and honesty

The system must be able to distinguish and clearly present:

- **measured data** (CGM values, derived metrics),
- **user-entered facts** (meals/drinks/insulin/symptoms the user recorded),
- **app-generated observations** (patterns/summaries LibreCare computed).

App-generated observations must never be presented as medical instructions.

## G4 — Future correction-plan feature (guarded)

If a future correction-plan feature is considered, its rules **must** come from an explicitly
configured, **clinician-approved** plan. It must be treated as a **separate safety/regulatory
project** with its own review, and it must not be introduced as a side effect of analytics or AI
research.

## G5 — Missing-memory tolerance

Contextual questions must tolerate "I DON'T REMEMBER". The product must never coerce a definitive
answer, and must never fabricate context to fill a gap.

## G6 — AI boundary

AI may analyze and propose (see `PRODUCT_STRATEGY.md` §4) but must not approve requirements, approve
UX, change strategy, or start implementation. Safety-relevant proposals still require explicit human
decision and, where clinical, a separate regulated process.

## G7 — Data reliability is a safety property

Freshness/staleness of CGM data and data completeness are treated as **safety-relevant** signals,
not cosmetic details. Any mode must be able to make staleness obvious rather than hide it.

---

Related requirement records must fill `safety_implications` and, where relevant, reference G1–G7.
The example decision `decisions/` / `examples/decision-no-autonomous-dosing.example.json` records
the standing prohibition so it is not "rediscovered" later.

