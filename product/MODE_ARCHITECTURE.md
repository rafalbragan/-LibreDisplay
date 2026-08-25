# LibreCare — Mode Architecture (future concept, documentation only)

> **Phase 1 note:** This document describes the *intended* future architecture for the three
> perspectives. It is **not** implemented in production Android code in this task. No `UserMode` type
> is introduced into the app yet.

## 1. The future `UserMode`

```kotlin
enum class UserMode {
    CAREGIVER,
    SENIOR,
    CLINICIAN
}
```

`CAREGIVER` is the primary/default perspective.

## 2. Architectural rule: centralized mode-aware presentation policy

The three perspectives must eventually influence **every major module**. The wrong way to do this is
to scatter conditionals across Compose screens:

```kotlin
// ANTI-PATTERN — do NOT do this throughout the UI
if (mode == UserMode.SENIOR) { ... } else if (mode == UserMode.CLINICIAN) { ... }
```

Scattered `if (mode == ...)` branches are hard to test, hard to keep consistent, and make it easy
for one mode to silently diverge from the shared safety rules.

**Rule:** Mode differences must be expressed through **centralized, mode-aware presentation
policies** that a screen reads from, rather than ad-hoc branching inside each composable.

### 2.1 Suggested shape (future)

A single source of truth maps `(UserMode, module) → presentation policy`:

```kotlin
// FUTURE — illustrative only, not implemented in this task
data class ModulePresentationPolicy(
    val visibleSections: List<SectionId>,
    val sectionOrder: List<SectionId>,
    val density: Density,              // COMPACT | COMFORTABLE | LARGE
    val primaryActions: List<ActionId>,
    val defaultRange: RangePreset,
    val wording: WordingSet,           // per-mode strings
)

interface ModePresentationPolicyProvider {
    fun policyFor(mode: UserMode, module: ModuleId): ModulePresentationPolicy
}
```

Screens/ViewModels then render from a `ModulePresentationPolicy` they are given. A composable does
not ask "which mode am I?"; it asks "what does my policy say to show, in what order, at what
density, with what wording and default range?".

### 2.2 What stays shared regardless of mode

- CGM data source and domain model (`GlucoseReading`, `GlucoseHistoryPoint`, metrics).
- Safety rules and guardrails (`SAFETY_GUARDRAILS.md`). A mode may **not** relax a safety rule.
- Stored events (meals/drinks/insulin/symptoms — future) and their provenance
  (measured vs user-entered vs app-generated).

### 2.3 What a policy may change per mode

- Which sections are visible and in what order (information priorities).
- Information density and typography scale.
- Which actions are primary vs secondary vs hidden.
- Navigation shortcuts / entry points.
- Wording (labels, questions, summaries).
- Default time ranges and default summaries.

## 3. Provenance requirement (especially for CLINICIAN)

The data model must eventually distinguish, and the UI must be able to display:

- **measured** — CGM values and derived metrics,
- **user-entered facts** — meals, drinks, insulin, symptoms the user recorded,
- **app-generated observations** — patterns/summaries LibreCare computed.

This distinction is a shared model concern, not a per-screen cosmetic choice.

## 4. Migration approach (future phases, not now)

1. Introduce `UserMode` in a domain/presentation layer (no UI wiring).
2. Introduce `ModulePresentationPolicyProvider` with a CAREGIVER policy identical to today's UI
   (behaviour-preserving).
3. Add SENIOR and CLINICIAN policies module-by-module behind the provider.
4. Add mode selection/persistence last, once policies exist.

Each step must keep existing tests green and must not weaken safety rules.

