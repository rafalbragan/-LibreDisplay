# LibreCare Product CLI

Deterministic tooling for the `/product` Requirements Collector. No AI/LLM calls.

```bash
python scripts/product/product_cli.py validate   # validate all product records; non-zero on error
python scripts/product/product_cli.py summary     # print counts of observations/requirements
```

## What `validate` checks

- Required fields (per `product/schema/*.schema.json`).
- Valid enum values (status, persona, mode, type, severity, frequency, confidence, source_type).
- Basic types and nested `scores`/`solution_options` structure.
- Unique IDs across all records.
- Cross-references exist:
  - requirement → `linked_observations` (observation IDs),
  - requirement → `related_decisions` (decision IDs),
  - decision → `related_requirements` (requirement IDs),
  - observation → `linked_requirements` (requirement IDs).
- A JSON record that fails to parse is a **hard error**. A UTF-8 BOM is tolerated.

## What `validate` scans

- `product/research/observations/*.json` (+ `.yaml` if PyYAML is installed)
- `product/requirements/*.json` (+ `.yaml`), excluding `*TEMPLATE*`
- `product/decisions/*.json` (+ `.yaml`), excluding `*TEMPLATE*`
- `product/examples/*` routed by filename keyword (`observation` / `requirement` / `decision`)

Templates (`*TEMPLATE*`) are scaffolds and are intentionally not validated as records. YAML records
are validated only when PyYAML is available; the deterministic CI path uses JSON records and needs
only the Python standard library.

