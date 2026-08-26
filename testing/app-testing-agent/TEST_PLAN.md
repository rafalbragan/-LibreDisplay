# LibreCare App Testing Agent — Test Plan

**Status**: NOT_YET_FIREBASE_IMPORTABLE (awaiting Firebase schema verification)  
**Date**: 2026-08-26  
**App**: LibreCare 2.11.1 (versionCode 36)  
**Device**: Medium Phone, 6.4in (Arm), API 30, portrait, English  

## Overview

This document describes exploratory test cases prepared for Firebase App Testing Agent (Robo and/or Instrumentation testing). These cases build on the three baseline runs (TESTRUN-2026-001/002/003) and target specific UX evaluation questions.

**Key principle**: These are **exploratory tests** of product behavior, NOT automated acceptance tests. Results will be recorded as test evidence (TESTRUN records), reviewed for insights, and may lead to product observations (OBS records) if issues are identified.

## Prepared Test Cases

### Caregiver Tests

#### TC-CAR-001: Identify Who Needs Attention

**Scenario**: MULTIPLE_PATIENTS_ONE_AT_RISK

**Setup**:
- Two monitored demo persons
- Person A: Normal stable glucose (112 mg/dL, FLAT trend, IN RANGE)
- Person B: At-risk condition
  - Option 1: HYPO (52 mg/dL, LOW)
  - Option 2: STALE_DATA (146 mg/dL but 36 hours old)

**Goal**:
```
You are caring for several people with diabetes using LibreCare.
Determine whether any monitored person currently requires attention 
and identify which person and why.
Use the app naturally.
```

**Expected outcome**:
- Caregiver navigates Home/Monitoring screen
- Caregiver reviews available persons (may switch profiles)
- Caregiver identifies Person B as requiring attention
- Caregiver articulates the reason (low glucose OR stale data)
- **No explicit navigation hints are provided to the agent**

**UX test target**:
- Multi-person display clarity
- Person switching discoverability
- Alert/risk prominence

---

#### TC-CAR-002: Detect Stale Data

**Scenario**: STALE_DATA (26 hours old)

**Setup**:
- Single monitored person
- Glucose: 146 mg/dL (in range)
- Data: 24 hours, ending 26 hours ago
- Last update: ~26 hours ago

**Goal**:
```
Determine whether the currently displayed glucose information 
is sufficiently current to assess the monitored person's present situation.
Do not assume the data are stale; discover this naturally.
```

**Expected outcome**:
- Caregiver reads Home screen
- Caregiver locates timestamp/freshness indicator
- Caregiver determines data is too old
- Caregiver articulates concern about reliability

**UX test target**:
- **CRITICAL**: Timestamp/freshness visibility and prominence
- User ability to assess data currency without domain knowledge
- Clear messaging that recent data ≠ current situation

---

### Senior Tests

#### TC-SEN-001: Understand Low Glucose Situation

**Scenario**: HYPO (consistent low glucose)

**Setup**:
- Glucose: ~68 mg/dL (LOW, ≤79)
- Trend: FLAT
- Status: IN RANGE? NO → LOW alert/warning

**Goal**:
```
You are an older person with diabetes.
Use LibreCare naturally to understand your current glucose situation
and whether anything on the screen deserves your attention.
Do not include treatment instructions in your reasoning.
```

**Expected outcome**:
- Senior sees Home screen
- Senior reads glucose value (68 mg/dL)
- Senior identifies that glucose is "low" or "concerning"
- Senior articulates awareness of visual/icon changes (color, alert, icon)
- Senior does NOT attempt insulin corrections or treatment

**UX test target**:
- Low glucose alert visibility and accessibility
- Color contrast and icon distinctiveness
- Clarity without domain training

---

#### TC-SEN-002: Understand Rapid Change

**Scenario**: RAPID_FALL (glucose dropping)

**Setup**:
- Duration: 24 hours
- Starting glucose: 144 mg/dL
- Current glucose: ~96 mg/dL
- Trend: FALLING
- Status: IN RANGE (but descending)

**Goal**:
```
Determine your current glucose and whether it is changing in an important way.
Report what you see on screen without navigation.
```

**Expected outcome**:
- Senior reads Home screen
- Senior identifies glucose value (~96)
- Senior identifies trend indicator (arrow, "down", icon, etc.)
- Senior articulates that glucose is "going down" or "falling"
- Senior interprets this as potentially important

**UX test target**:
- Trend indicator visibility (arrow, icon, text)
- Non-technical user comprehension of direction
- Trend prominence relative to value

---

### Clinician Tests

#### TC-CLI-001: Find Important Pattern

**Scenario**: RAPID_RISE (glucose rising over 24h)

**Setup**:
- Duration: 24 hours
- Starting glucose: 96 mg/dL
- Current glucose: ~144 mg/dL
- Trend: RISING
- Status: IN RANGE (but ascending)

**Goal**:
```
Review the available glucose data and identify the most clinically
relevant pattern or episode visible in the recent data.
```

**Expected outcome**:
- Clinician navigates to Analysis module (without hints)
- Clinician reviews history chart or metrics
- Clinician identifies the rising trend
- Clinician articulates clinical significance (e.g., "consistent 24h rise approaching high threshold")
- Clinician may reference TIR, mean, or variability if available

**UX test target**:
- Analysis module discoverability
- Chart readability and interactivity
- Metric accuracy and presentation
- Trend line visibility

---

#### TC-CLI-002: Assess Metric Accuracy

**Scenario**: NORMAL (stable in-range, 24h data)

**Setup**:
- Glucose: ~112 mg/dL stable
- Trend: FLAT
- Duration: 24 hours
- Status: IN RANGE

**Goal**:
```
Review the Analysis screen metrics and verify they accurately
represent the displayed glucose history.
```

**Expected outcome**:
- Clinician navigates to Analysis
- Clinician reviews presented metrics (TIR, mean glucose, variability, episode counts)
- Clinician cross-checks metrics against history
- Clinician confirms whether metrics match expected values
- Clinician notes if metrics are unavailable (14-day minimum) or calculated correctly

**UX test target**:
- Metrics computation correctness
- Clear indication of data sufficiency
- Metric presentation clarity for clinician users

---

## Execution Environment

All test cases use the same device and environment:

| Parameter | Value |
|-----------|-------|
| Device | Medium Phone, 6.4in diagonal (Arm) |
| API Level | 30 |
| Orientation | Portrait |
| Locale | English |
| App Version | LibreCare 2.11.1 |
| Version Code | 36 |
| Build Type | Debug (to allow scenario selection) |

## Scenario Injection Mechanism

For Firebase App Testing Agent to run these tests:

1. **Test parameters**: Pass scenario name to app (e.g., `STALE_DATA`)
2. **Scenario selection**: Debug-only activity/menu allows scenario setup
3. **Data injection**: Replace default demo data with selected scenario
4. **No production UI change**: Scenario selector is debug/test-only, not visible in Live

## Recording Results

Each test run produces a TESTRUN record:

```yaml
id: "TESTRUN-YYYY-NNN"
source: "firebase_app_testing_agent"
test_case: "TC-CAR-001 — Caregiver — identify who needs attention"
scenario: "MULTIPLE_PATIENTS_ONE_AT_RISK"
result: "PASS" | "FAIL" | "INCONCLUSIVE" | "ERROR"
agent_summary: "Human-readable agent conclusion"
positive_evidence: ["Finding A", "Finding B"]
potential_issues: ["Hypothesis X", "Hypothesis Y"]
linked_observations: []  # Empty initially
review_status: "unreviewed"
```

## Success vs. Failure Criteria

A test is **PASS** when:
- Agent completes the goal without explicit hints
- Agent reasoning is coherent and based on visible app behavior
- All success criteria are met

A test is **FAIL** when:
- Agent cannot complete the goal (e.g., can't find Analysis module)
- Agent provides incorrect conclusion (e.g., misreads glucose value)
- Critical UX element is invisible or inaccessible

A test is **INCONCLUSIVE** when:
- Agent behavior is ambiguous or partially successful
- Evidence doesn't clearly support pass or fail

## Important Constraints

- ✓ **NO treatment recommendations** in test design or interpretation
- ✓ **NO insulin dosing** simulations
- ✓ **NO clinical decision support** evaluation
- ✓ **Scenario data only** — not production behavior
- ✓ **Evidence-based** — results recorded as observations, not auto-converted to requirements

## Firebase Integration (Future)

Once the official Firebase App Testing Agent YAML schema is verified:

1. Convert test case descriptions to Firebase YAML format
2. Map goal → test case description
3. Map expected_user_actions → automation steps
4. Map success_criteria → assertions
5. Upload to Firebase Test Lab

**Until schema is confirmed, this test plan remains in human-readable format.**


