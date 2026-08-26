# Product Evidence to Observation Flow

**Document**: Phase 2 Exploratory Product Review  
**Date**: 2026-08-26  
**Version**: 1.0

## Core Principle

```
TESTRUN ≠ OBSERVATION
```

A **test run** is exploratory evidence.  
An **observation** is a validated product finding.

## Evidence Flow

```
┌─────────────────────────────────────────────────────────┐
│  Firebase App Testing Agent (Manual or Automated)      │
│  [runs test, collects agent output]                    │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│  TESTRUN Record                                          │
│  ├─ id: TESTRUN-YYYY-NNN                               │
│  ├─ scenario, persona, result                          │
│  ├─ positive_evidence: ["Finding A", "Finding B"]      │
│  ├─ potential_issues: ["Hypothesis X"] (unconfirmed)   │
│  ├─ linked_observations: [] (initially empty)          │
│  └─ review_status: "unreviewed"                        │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────┐
        │  HUMAN PRODUCT REVIEW        │
        │  (not automated)             │
        │                              │
        │  "Is this a real problem?"   │
        │  "Is this a limitation?"     │
        │  "Is this expected?"         │
        │  "Is this testable again?"   │
        └──────────────┬───────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
        ▼                             ▼
   POSITIVE EVIDENCE          POTENTIAL ISSUE
   (Keep & celebrate)         (Needs confirmation)
        │                             │
        │                    ┌────────┴────────┐
        │                    │                 │
        │             ▼              ▼
        │        Confirmed        Unconfirmed
        │         Finding          Hypothesis
        │             │                 │
        │             ▼                 ▼
        │        OBSERVATION        DISCARD
        │      OBS-20260826-NNN    (no OBS)
        │             │
        │             └──────────┬──────────┘
        │                        │
        └────────────────────────┴─────────────┐
                                 │
                                 ▼
                     ┌──────────────────────┐
                     │  UPDATE TESTRUN      │
                     │  linked_observations │
                     │  = [OBS-...]        │
                     │  review_status:      │
                     │  "reviewed"          │
                     └──────────────────────┘
```

## Example: TESTRUN-2026-001 (Caregiver Baseline)

### Input

```yaml
id: TESTRUN-2026-001
test_case: "Caregiver — assess current glucose situation"
scenario: NORMAL
result: PASS
positive_evidence:
  - "Current glucose status was discoverable"
  - "Monitored-person switching was discovered without instruction"
  - "Agent assessed more than one monitored person"
  - "Test did not require navigation hints"
potential_issues:
  - "Ambiguity about which person is 'the' monitored person"
review_status: unreviewed
```

### Human Review Questions

1. **Is this positive evidence of good UX?**
   - ✓ Yes: "Current glucose discoverable without hints" = baseline working
   - ✓ Yes: "Multi-person switching not obvious, but agent discovered it"

2. **Is "discovered multi-person switching" a problem?**
   - Option A: No → this is expected UX, keep as positive evidence
   - Option B: Yes → create OBS-20260826-## for multi-person confusion

3. **Is "ambiguity about monitored person" a real issue?**
   - Option A: Defer to next test → too vague, need TESTRUN data
   - Option B: Create preliminary OBS-20260826-## to track this hypothesis

4. **Should any of these become Observations?**
   - If issue is confirmed in ≥2 independent test runs → create OBS
   - If issue is one-off interpretation → note but don't create OBS
   - If positive, no OBS needed → celebrate the finding

### Outcome (Example)

```yaml
# After review:
id: TESTRUN-2026-001
review_status: reviewed
linked_observations: []  # No confirmed issues from this run

# Alternative outcome (if ambiguity was confirmed):
linked_observations:
  - OBS-20260826-001  # Created: Ambiguous multi-person context
```

---

## Three Baseline Tests: NOT Converted to Observations

### TESTRUN-2026-001: Caregiver
- **Result**: PASS ✓
- **Finding**: Multi-person switching and glucose discovery work
- **Action**: ✓ Record positive evidence, do NOT create OBS
- **Status**: baseline validation

### TESTRUN-2026-002: Senior
- **Result**: PASS ✓
- **Finding**: Senior can read glucose + trend + status from Home screen
- **Action**: ✓ Record positive evidence, do NOT create OBS
- **Status**: baseline validation

### TESTRUN-2026-003: Clinician
- **Result**: PASS ✓
- **Finding**: Analysis module discovery works, metrics visible
- **Action**: ✓ Record positive evidence, do NOT create OBS
- **Status**: baseline validation

**Important**: These PASS results are **proof that baseline UX works**. They are NOT problems. They are evidence that requirements are being met.

---

## When to Create an Observation from a Test Run

An observation should be created when:

1. **Multiple independent runs** show the same issue (reproducibility)
2. **Issue is blocking** (user cannot complete task, incorrect value shown, etc.)
3. **Issue is testable** (can be verified by re-running or fixed and validated)
4. **Issue is not a design choice** (not intentional behavior)

An observation should NOT be created when:

1. Test is PASS and baseline is healthy
2. Issue is speculative ("might confuse users")
3. Issue is based on single exploratory run
4. Issue is product feedback, not a bug (e.g., "I wish it showed X")
5. Issue conflicts with active product design decision

---

## Three-Phase Evidence Lifecycle

### Phase 1: Exploratory (Initial Test Run)

```
TESTRUN-2026-001 (PASS)
├─ positive_evidence: ["thing works"]
├─ potential_issues: ["hypothesis only"]
└─ linked_observations: []
```

**Status**: Unreviewed evidence. No OBS created yet.

### Phase 2: Targeted Follow-up (Specific Test)

```
TESTRUN-2026-004 (FAIL) ← Targets the hypothesis
├─ test_case: "Verify multi-person context"
├─ positive_evidence: []
├─ potential_issues: ["Users confused about context"]
└─ linked_observations: []
```

**Status**: Hypothesis confirmed. Now create OBS.

```
OBS-20260826-001
├─ problem: "Multi-person context ambiguous in Home screen"
├─ severity: medium
├─ linked_test_runs: [TESTRUN-2026-001, TESTRUN-2026-004]
└─ status: new
```

### Phase 3: Resolution (Test After Fix)

```
OBS-20260826-001 (in progress)
├─ fix: [code change]
└─ validation_testrun: TESTRUN-2026-005 (PASS, uses same context scenario)
```

**Status**: Regression prevented by validation test.

---

## Roles & Approval

### Exploratory Test Agent (Firebase)
- Runs test
- Records TESTRUN with factual findings
- Does NOT interpret as problem
- Provides agent summary and evidence

### Product Reviewer (Human)
- Reads TESTRUN
- Asks: "Is this a real product limitation?"
- Decides: Create OBS or keep as positive evidence
- Updates TESTRUN.review_status → "reviewed"
- Optionally links to OBS

### Product Manager
- Prioritizes confirmed OBS
- Decides if OBS → Requirement or Backlog
- Approves requirement status (CANDIDATE → ACCEPTED)

---

## Glossary

| Term | Definition |
|------|-----------|
| **TESTRUN** | Exploratory test execution record (never auto-converted to OBS) |
| **Positive Evidence** | Confirmed working behavior; celebrated, no OBS needed |
| **Potential Issue** | Hypothesis needing confirmation; may require follow-up test |
| **OBSERVATION (OBS)** | Confirmed product finding (bug, limitation, or note) |
| **Test PASS** | Agent completed goal without blocker |
| **Test FAIL** | Agent could not complete goal due to app limitation |
| **Test INCONCLUSIVE** | Agent behavior ambiguous; needs follow-up |
| **Reproducibility** | Issue appears in ≥2 independent runs (signals real bug) |
| **Deterministic Test** | Same input produces same output every time (GlucoseScenarioEngine) |

---

## Phase 2 Guardrail

✓ **Zero OBS created from three baseline PASS runs**

The baseline tests validated that core functionality works. No problems discovered. This is expected and healthy.

If a later test run (TC-CAR-002: STALE_DATA, TC-SEN-001: HYPO, etc.) reveals a legitimate blocker, that will be documented as OBS only after human review confirms the issue.

---

## Related Documents

- `/product/research/test_runs/TESTRUN-2026-*.yaml` — Baseline test records
- `/testing/app-testing-agent/TEST_PLAN.md` — Prepared follow-up test cases
- `/testing/demo/CONTROLLED_DEMO_SCENARIOS.md` — Scenario definitions
- `/product/CURRENT_FOCUS.yaml` — Product decision context


