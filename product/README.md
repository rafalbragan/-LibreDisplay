# LibreCare — Product Foundation

This `/product` directory is the **product-management infrastructure** for LibreCare. It is
intentionally separate from the Android application code. Nothing here changes production UI or
behaviour — it is documentation, schemas, backlog structure, and lightweight validation tooling.

## What LibreCare is

LibreCare is **one** Android application that reads Libre CGM data (via LibreLinkUp) and presents it
through **three perspectives** of the same underlying data, domain model, safety rules, and stored
events:

- `CAREGIVER` — primary perspective
- `SENIOR`
- `CLINICIAN`

The three perspectives are **not** just colours, font sizes, or hiding a card. Each may differ in
information priorities, actions, density, navigation shortcuts, wording, and default ranges — while
sharing the same CGM data and safety guarantees.

## Directory map

```text
product/
  README.md                  # this file
  PRODUCT_STRATEGY.md        # strategy, three-mode architecture, intent per persona
  CURRENT_FOCUS.yaml         # machine-readable priorities + modes + approval rules
  MODE_ARCHITECTURE.md       # how modes should be implemented (future, centralized policy)
  PERSONAS.yaml              # machine-readable persona definitions
  SAFETY_GUARDRAILS.md       # explicit product safety rules (no autonomous dosing)
  ROADMAP.md                 # phased roadmap
  DECISION_LOG.md            # human-readable log of meaningful product decisions
  MODULE_INVENTORY.md        # REAL modules discovered from the codebase
  MODULE_MODE_MATRIX.md      # per-module direction for each mode
  schema/                    # JSON Schemas (Draft 2020-12)
    observation.schema.json
    requirement.schema.json
    decision.schema.json
  research/
    README.md                # how a future AI Product Reviewer submits observations
    OBSERVATION_TEMPLATE.yaml
    observations/            # real observation records go here (one file each)
  requirements/
    REQUIREMENT_TEMPLATE.yaml
  decisions/
    DECISION_TEMPLATE.yaml
  examples/                  # a few clearly-marked example records
```

Tooling lives in `scripts/product/product_cli.py`.

## The required flow (human stays in control)

```text
OBSERVATION → PROBLEM → CANDIDATE REQUIREMENT → AI ANALYSIS →
HUMAN DECISION → DESIGN PROPOSAL → HUMAN DECISION → IMPLEMENTATION → TEST
```

AI may collect, classify, deduplicate, cluster, find patterns, propose candidate requirements,
propose options, score them, and give counterarguments. **AI must not** change strategy, approve
requirements, approve UX, or start implementation. Human decisions are `ACCEPT`, `HOLD`, `REJECT`,
and rejected/held ideas stay recorded so they are not rediscovered as "new".

## Validation

```bash
python scripts/product/product_cli.py validate
python scripts/product/product_cli.py summary
```

`validate` fails (non-zero exit) if any structured product record is invalid. `product-quality`
GitHub Actions runs `validate` on changes under `product/**` and `scripts/product/**`.

