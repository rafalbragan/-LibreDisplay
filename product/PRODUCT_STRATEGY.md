# LibreCare — Product Strategy

Status: **Phase 1 foundation** (infrastructure only — no production UI change).
Owner: Human Product Owner. AI acts as researcher/analyst, never as approver.

## 1. One app, three perspectives

LibreCare is a single Android application that surfaces Libre CGM data through three perspectives
("modes") of the **same** underlying data and rules:

| Mode | Core question |
|------|----------------|
| `CAREGIVER` (primary) | "Is something happening **now** that I should pay attention to?" |
| `SENIOR` | "Is everything OK now, and is there anything I need to do per my plan?" |
| `CLINICIAN` | "What happened between visits, what patterns matter, and what evidence supports them?" |

Shared foundation across all modes:

- the same underlying CGM data (LibreLinkUp → Room history),
- the same domain model (`GlucoseReading`, `GlucoseHistoryPoint`, metrics),
- the same safety rules (see `SAFETY_GUARDRAILS.md`),
- the same stored events.

A mode may differ in: information priorities, available actions, information density, navigation
shortcuts, wording, and default ranges/summaries. A mode is **not** merely a colour/font/hide-a-card
theme.

## 2. Product intent per perspective

### CAREGIVER (primary direction)

Priorities: current glucose · trend and speed/direction · **freshness** of CGM data · important
risks · recent relevant events · easy monitored-person switching · what changed since last check ·
actionable context without information overload.

### SENIOR

Priorities: very large current glucose · very clear trend · simple status · very low cognitive load
· large touch targets · minimal steps · future glanceable widgets · simple contextual questions.

Future contextual questions (examples, not yet implemented):
"Did you eat during the last hour?", "Did you drink something sweet?", "Did you take insulin?",
"How many units should I record?" — answerable as **YES / NO / I DON'T REMEMBER**. The system must
**tolerate missing memory** rather than force an answer.

### CLINICIAN

Priorities: Time in Range / Below / Above · CV and standard CGM metrics already supported · episodes
· data completeness · timeline · meal/drink/insulin/symptom context · period comparison · reports ·
and a **clear distinction** between measured data, user-entered facts, and app-generated
observations.

## 3. Safety boundary (summary)

LibreCare may eventually detect rapid rise/fall, notice missing context, ask contextual questions,
show patterns, summarize observations, and show a previously configured care plan.

LibreCare **must not** autonomously calculate or recommend an individual insulin dose from AI, CGM
trend, or incomplete context. See `SAFETY_GUARDRAILS.md` for the binding rules. Any future
correction-plan feature must be driven by an explicitly configured, clinician-approved plan and
treated as a **separate safety/regulatory project**.

## 4. Human product control

AI **may**: collect observations, classify, deduplicate, cluster, search for patterns, propose
candidate requirements, propose several options, score candidates, and provide counterarguments.

AI **must not**: change product strategy, approve requirements, approve UX, or start implementation
automatically.

Flow: `OBSERVATION → PROBLEM → CANDIDATE REQUIREMENT → AI ANALYSIS → HUMAN DECISION → DESIGN
PROPOSAL → HUMAN DECISION → IMPLEMENTATION → TEST`. Human decisions are `ACCEPT` / `HOLD` / `REJECT`.
Rejected and HOLD ideas remain recorded (see `decisions/` and `DECISION_LOG.md`).

## 5. Prioritization philosophy

Prioritization is **multi-dimensional** (see `CURRENT_FOCUS.yaml` weights and the requirement
`scores` object). We deliberately do **not** collapse prioritization into a single opaque AI score;
each dimension (safety, caregiver/senior/clinician value, frequency, confidence, complexity,
strategy alignment) stays visible so a human can reason and override.

## 6. Non-goals for Phase 1

- No production UI redesign.
- No in-app mode switching yet.
- No new medical functionality.
- No insulin-dose recommendation logic.

