# LibreCare Product CLI

Deterministic tooling for the `/product` Requirements Collector. No AI/LLM calls.

```bash
python scripts/product/product_cli.py validate   # validate all product records; non-zero on error
python scripts/product/product_cli.py summary     # print counts of observations/requirements
```

## What `validate` checks

- Required fields (per `product/schema/*.schema.json`).
- Valid enum values (status, persona, mode, type, severity, frequency, confidence, source_type).
- Pattern checks (e.g. `OBS-`, `REQ-`, `DEC-` IDs).
- Pattern checks for exploratory test runs (e.g. `TEST-YYYY-NNN`).
- Array minimum sizes where configured in schemas.
- Numeric score bounds (`0..10`) for requirement scoring dimensions.
- Unique IDs across all records.
- `recommended_option` must match an existing `solution_options[].id`.
- `solution_options[].id` must be unique within a requirement.
- Requirement status and `human_decision` consistency.
- Cross-references exist:
  - test-run → `linked_observations` (observation IDs),
  - requirement → `linked_observations` (observation IDs),
  - requirement → `related_decisions` (decision IDs),
  - decision → `related_requirements` (requirement IDs),
  - observation → `linked_requirements` (requirement IDs).
- `product/CURRENT_FOCUS.yaml` is always validated (required structure, modes, and priority-weight range).
- JSON/YAML parse failures are **hard errors**. A UTF-8 BOM is tolerated for JSON.

## What `validate` scans

- `product/research/observations/*.json` (+ `.yaml` if PyYAML is installed)
- `product/research/test-runs/*.json` (+ `.yaml`)
- `product/requirements/*.json` (+ `.yaml`), excluding `*TEMPLATE*`
- `product/decisions/*.json` (+ `.yaml`), excluding `*TEMPLATE*`
- `product/examples/*` routed by filename keyword (`observation` / `test-run` / `requirement` / `decision`)

Templates (`*TEMPLATE*`) are scaffolds and are intentionally not validated as records.
YAML parsing requires PyYAML; if unavailable, validation fails.

## Summary output

`summary` validates counts separately for:

- Real backlog records (from `product/research/observations`, `product/research/test-runs`, `product/requirements`, `product/decisions`)
- Example records (from `product/examples`)

