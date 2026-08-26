# LibreCare Controlled Demo Scenarios

Controlled, deterministic glucose scenarios for Firebase App Testing Agent and exploratory QA.

## Quick Reference

| Scenario | Current | Trend | Status | Use Case |
|----------|---------|-------|--------|----------|
| NORMAL | 112 | FLAT | IN RANGE | Baseline reference |
| RAPID_RISE | 144 | RISING | IN RANGE | Trend detection |
| RAPID_FALL | 96 | FALLING | IN RANGE | Downward trend |
| HYPO | 68 | FLAT | LOW | Low glucose visibility |
| SEVERE_HYPO | 48 | FALLING_FAST | VERY LOW | Severe hypo prominence |
| HYPER | 212 | FLAT | HIGH | High glucose visibility |
| STALE_DATA | 146 | FLAT | (24h old) | Freshness detection |
| MISSING_DATA | None | None | None | No data state |
| POST_MEAL_RISE | 130 | RISING | IN RANGE | Post-meal pattern |
| MULTIPLE_PATIENTS_ONE_AT_RISK | Variable | Variable | Variable | Multi-person triage |

## Implementation Map

All scenarios built using `GlucoseScenarioEngine` with 28 preset scenarios and 10 CGM patterns.

### GlucoseScenario Enum (28 scenarios)
- NORMAL_STABLE → baseline NORMAL
- LOW_STABLE → HYPO
- VERY_LOW → SEVERE_HYPO
- HIGH_STABLE → HYPER
- HIGH_RISING → RAPID_RISE
- LOW_FALLING → RAPID_FALL
- STALE_DATA → STALE_DATA (critical test)
- NO_DATA → MISSING_DATA
- Plus: DATA_WITH_GAPS, FULL_24H/48H/3D_DATA, CROSSING_MIDNIGHT, etc.

### CgmPattern Enum (10 patterns)
FLAT, GRADUAL_RISE, GRADUAL_FALL, FAST_RISE, FAST_FALL, HYPO_EPISODE, HYPER_EPISODE, IRREGULAR_INTERVALS, GAPS, CROSSING_MIDNIGHT

## Technical Details

- **Location**: `/app/src/sharedTest/java/com/libredisplay/testing/scenario/GlucoseScenarioEngine.kt`
- **Deterministic**: Every call with same parameters = identical output
- **Base timezone**: Europe/Warsaw
- **Base time**: 2026-08-24 12:00:00
- **Sample interval**: 5 minutes
- **Target range**: 80–180 mg/dL (default, configurable)

## Scenario Details

### NORMAL - Baseline
- 24h stable data
- Glucose: ~112 mg/dL ±2 mg/dL
- Trend: FLAT
- Status: IN RANGE
- Use: Reference UI baseline

### RAPID_RISE - Trending Up
- 24h rising data
- Glucose: 96 → 144 mg/dL
- Trend: RISING
- Status: IN RANGE (near high)
- Use: Test trend visibility

### RAPID_FALL - Trending Down
- 24h falling data
- Glucose: 144 → 96 mg/dL
- Trend: FALLING
- Status: IN RANGE (near low)
- Use: Test downward trend detection

### HYPO - Low Glucose
- 24h stable low
- Glucose: ~68 mg/dL
- Trend: FLAT
- Status: LOW (≤79)
- Use: Low glucose alerting

### SEVERE_HYPO - Very Low
- ~6h HYPO_EPISODE pattern
- Glucose: 110 → 48 → 92 (dips and recovers)
- Trend: FALLING_FAST (at nadir)
- Status: VERY LOW (≤54)
- Use: Accessibility/maximum prominence

### HYPER - High Glucose
- 24h stable high
- Glucose: ~212 mg/dL
- Trend: FLAT
- Status: HIGH (≥181)
- Use: High glucose alerting

### STALE_DATA - CRITICAL TEST
- 24h data ending 26 hours ago
- Glucose: ~146 mg/dL
- Trend: FLAT
- Status: IN RANGE (but 24+ hours old)
- **CRITICAL**: Tests if user immediately detects stale timestamp
- Use: Freshness awareness; caregiver must verify currency

### MISSING_DATA - No Data
- Zero points (NO_DATA)
- Glucose: None
- Trend: None
- Status: Error state
- Use: Missing data error handling

### POST_MEAL_RISE - Pattern Context
- 6–8h FAST_RISE pattern
- Glucose: 92 → 130+ mg/dL
- Trend: RISING or FLAT (plateau)
- Status: IN RANGE
- Use: Post-meal pattern recognition (future)

### MULTIPLE_PATIENTS_ONE_AT_RISK - Triage
- Two demo persons
  - Person A: NORMAL_STABLE (good glucose)
  - Person B: HYPO or STALE_DATA (needs attention)
- Use: Caregiver multi-person triage

## Safety Compliance

✓ No insulin dosing calculations  
✓ No treatment recommendations  
✓ No dietary advice  
✓ Glucose range: 1–600 mg/dL (realistic)  
✓ Timestamps: realistic, local timezone  
✓ Compliant with SAFETY_GUARDRAILS.md

## Usage Example

```kotlin
// Get deterministic scenario
val dataset = GlucoseScenarioEngine.dataset(
    scenario = GlucoseScenario.STALE_DATA,
    now = Instant.now(),
    zoneId = ZoneId.of("Europe/Warsaw")
)

// Convert to production domain model
val reading: GlucoseReading? = dataset.asReading(
    targetLow = 80,
    targetHigh = 180
)

// In Firebase App Testing Agent:
val scenarioName = getTestParameter("scenario")
val reading = GlucoseScenarioEngine.reading(
    scenario = GlucoseScenario.valueOf(scenarioName)
)
```

## Scenario Selection Mechanism (PART F)

Currently scenarios are accessed programmatically in test code. For Firebase App Testing Agent integration:

1. Create debug-only scenario selector (not production UI)
2. Allow agent to pass scenario parameter
3. Inject into mock repo / test data provider
4. Production Live build remains unchanged

Selector location: TBD (debug menu or test build flavor)


