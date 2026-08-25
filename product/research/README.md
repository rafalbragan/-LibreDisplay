# LibreCare — Research / Observations

This folder holds **observations**: structured records of *problems* seen in LibreCare. An
observation captures the **problem**, not a solution. Solutions and trade-offs belong to
requirements (`product/requirements/`), which link back to observations.

## How a future AI Product Reviewer submits observations

1. **One file per observation** in `product/research/observations/`.
   - Preferred format for validated records: JSON (works with the deterministic CLI without extra
     dependencies). YAML is also accepted when a YAML parser is available.
   - Filename suggestion: `OBS-YYYYMMDD-nn.json` (e.g. `OBS-20260826-01.json`).
2. **Use a fresh, unique `id`** of the form `OBS-XXXX`. IDs must be unique across all observations.
3. **Fill the required fields** from `product/schema/observation.schema.json`:
   `id, created_at, source_type, source_reference, persona, mode, module, type, severity, frequency,
   confidence, evidence, problem_statement, status`.
4. **Record the problem, not the fix.** `proposed_solution` is optional and should usually be empty.
5. **Reference a real module** from `product/MODULE_INVENTORY.md` in `module` when known.
6. **Pick honest values.** Use `unknown`/`low` confidence rather than inventing certainty. Missing
   memory / missing context is acceptable and expected (see safety guardrail G5).
7. **Validate before submitting:**
   ```bash
   python scripts/product/product_cli.py validate
   python scripts/product/product_cli.py summary
   ```

## What the AI may and may not do

- **May:** collect, classify, deduplicate, cluster, find patterns, propose candidate requirements,
  propose options, score, and provide counterarguments.
- **Must not:** change strategy, approve requirements, approve UX, or start implementation.
  See `product/PRODUCT_STRATEGY.md` §4 and `product/SAFETY_GUARDRAILS.md` G6.

## Lifecycle

```text
OBSERVATION → PROBLEM → CANDIDATE REQUIREMENT → AI ANALYSIS →
HUMAN DECISION → DESIGN PROPOSAL → HUMAN DECISION → IMPLEMENTATION → TEST
```

An observation's `status` moves `new → triaged → linked` (once a requirement links it) → `closed`
or `duplicate`. Rejected/held ideas are preserved via `product/decisions/` so they are not
re-proposed as new.

Use `OBSERVATION_TEMPLATE.yaml` (in this folder) as a human-friendly starting point.

