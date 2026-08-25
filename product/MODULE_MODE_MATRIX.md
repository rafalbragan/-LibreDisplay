# LibreCare — Module × Mode Matrix

Direction each **real** module should *eventually* take per perspective. This is **direction only**,
not implemented behaviour (Phase 1 changes no production UI). Rows are derived from
`MODULE_INVENTORY.md` (real modules). Items marked `FUTURE` are ideas, not existing functionality.

Legend: — = no meaningful difference expected · `FUTURE` = not built yet.

| Module | Caregiver direction | Senior direction | Clinician direction |
|--------|---------------------|------------------|---------------------|
| Home / Monitoring | Immediate status: current value, trend/speed, **freshness**, top risk, recent changes; fast person switch. | Glanceable/simple: very large value, one clear trend, simple OK/attention status, minimal actions. | Clinical snapshot: current value plus TIR/CV context and data completeness at a glance. |
| Full-screen history chart | Investigate "what happened" around a recent event. | Simplified recent history (or omitted for low cognitive load). | Detailed analysis: pan/zoom, target band, min/max markers, precise tooltips. |
| Analiza / Analytics | "What changed / what happened" summaries. | Simplified recent overview; fewer metrics. | Deep analysis: TIR/below/above, CV, episodes, completeness, period comparison, reports/export. |
| NFZ details | Understand eligibility status and next action. | Usually hidden (low relevance to task). | Understand reason + required vs current values (evidence). |
| Settings hub | Balanced set; quick access to person/metric settings. | Very few, large entries; only essentials. | Full access incl. metrics, analytics defaults, export. |
| Monitoring settings (target/metrics/HbA1c) | Sensible caregiver defaults; editable. | Minimal, pre-set defaults; rarely edited. | Rich metric configuration and clinical inputs (HbA1c/GMI). |
| Account / LibreLinkUp | — | — | — |
| Sync / Polling | — | — | — |
| Data retention | — | — | — |
| Privacy & Data | — | Simpler wording; fewer destructive actions surfaced. | — |
| Statistics | Low prominence. | Hidden. | Prominent: storage/transfer/completeness evidence. |
| About | — | — | — |
| Diagnostics | — (support/dev) | — (support/dev) | — (support/dev) |
| App lock / security | — | Larger unlock targets. | — |
| Startup restore | — | Simpler prompts. | — |
| Home-screen widget | Status + trend at a glance. | `FUTURE`: large glanceable widget is a stated Senior priority. | `FUTURE`: compact metric widget. |

## FUTURE cross-cutting capability: context capture

Not an existing module. A `FUTURE` shared capability referenced by the roadmap and personas:

| Capability (`FUTURE`) | Caregiver | Senior | Clinician |
|------------------------|-----------|--------|-----------|
| Context capture (meal / drink / insulin / symptom) | See recent captured context alongside status. | Simple contextual questions answerable **YES / NO / I DON'T REMEMBER**; tolerate missing memory. | Structured event timeline with clear provenance (measured vs user-entered vs app-generated). |

Any dose-related capability is explicitly out of scope and guarded (see `SAFETY_GUARDRAILS.md` G1/G4).

