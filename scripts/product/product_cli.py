#!/usr/bin/env python3
"""LibreCare Product CLI — deterministic validator + summary for /product records.

Phase 2 Requirements Collector. No AI/LLM calls. Standard-library only for the JSON path.
YAML records are validated too *if* PyYAML happens to be installed; otherwise they are skipped
with a note (the deterministic CI path uses JSON records).

Usage:
    python scripts/product/product_cli.py validate
    python scripts/product/product_cli.py summary
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from pathlib import Path

try:  # optional; not required for the deterministic JSON path
    import yaml  # type: ignore
    HAVE_YAML = True
except Exception:  # pragma: no cover - environment dependent
    HAVE_YAML = False

ROOT = Path(__file__).resolve().parents[2]
PRODUCT = ROOT / "product"
SCHEMA_DIR = PRODUCT / "schema"

CURRENT_FOCUS_FILE = PRODUCT / "CURRENT_FOCUS.yaml"
OBSERVATIONS_DIR = PRODUCT / "research" / "observations"
TEST_RUNS_DIR = PRODUCT / "research" / "test-runs"
REQUIREMENTS_DIR = PRODUCT / "requirements"
DECISIONS_DIR = PRODUCT / "decisions"
EXAMPLES_DIR = PRODUCT / "examples"

# Maximum value allowed for requirement score fields.
MAX_SCORE = 10

# ----------------------------------------------------------------------------- helpers

JSON_TYPE_CHECKS = {
    "string": lambda v: isinstance(v, str),
    "number": lambda v: isinstance(v, (int, float)) and not isinstance(v, bool),
    "integer": lambda v: isinstance(v, int) and not isinstance(v, bool),
    "boolean": lambda v: isinstance(v, bool),
    "object": lambda v: isinstance(v, dict),
    "array": lambda v: isinstance(v, list),
}


def load_schema(name: str) -> dict:
    with (SCHEMA_DIR / name).open(encoding="utf-8") as fh:
        return json.load(fh)


def load_record(path: Path):
    """Return (record_or_None, problem_or_None, is_hard_error)."""
    suffix = path.suffix.lower()
    if suffix == ".json":
        try:
            with path.open(encoding="utf-8-sig") as fh:
                return json.load(fh), None, False
        except Exception as exc:  # noqa: BLE001
            return None, f"could not parse JSON: {exc}", True
    if suffix in (".yaml", ".yml"):
        if not HAVE_YAML:
            return None, "YAML record skipped (PyYAML not installed; deterministic path uses JSON)", False
        try:
            with path.open(encoding="utf-8") as fh:
                return yaml.safe_load(fh), None, False
        except Exception as exc:  # noqa: BLE001
            return None, f"could not parse YAML: {exc}", True
    return None, "unsupported file type", False


def validate_value(schema: dict, value, path: str, errors: list) -> None:
    """Minimal but strict JSON-Schema subset: type, enum, required, properties, items, pattern."""
    expected_type = schema.get("type")

    # JSON Schema allows "type" to be a list (e.g. ["string", "null"] for nullable fields).
    if isinstance(expected_type, list):
        if value is None and "null" in expected_type:
            return  # valid nullable
        non_null_types = [t for t in expected_type if t != "null"]
        if non_null_types and not any(JSON_TYPE_CHECKS.get(t, lambda _: True)(value) for t in non_null_types):
            errors.append(f"{path}: expected type {expected_type!r}, got '{type(value).__name__}'")
            return
        expected_type = None  # skip single-type check below

    if expected_type and expected_type in JSON_TYPE_CHECKS:
        if not JSON_TYPE_CHECKS[expected_type](value):
            errors.append(f"{path}: expected type '{expected_type}', got '{type(value).__name__}'")
            return

    if "enum" in schema and value not in schema["enum"]:
        errors.append(f"{path}: value {value!r} not in allowed {schema['enum']}")

    if "pattern" in schema and isinstance(value, str):
        if not re.fullmatch(schema["pattern"], value):
            errors.append(f"{path}: value {value!r} does not match pattern {schema['pattern']!r}")

    if "minimum" in schema and isinstance(value, (int, float)):
        if value < schema["minimum"]:
            errors.append(f"{path}: value {value} is less than minimum {schema['minimum']}")

    if "maximum" in schema and isinstance(value, (int, float)):
        if value > schema["maximum"]:
            errors.append(f"{path}: value {value} is greater than maximum {schema['maximum']}")

    if "minItems" in schema and isinstance(value, list):
        if len(value) < schema["minItems"]:
            errors.append(f"{path}: array has {len(value)} items, minimum is {schema['minItems']}")

    if expected_type == "object" or isinstance(value, dict):
        if isinstance(value, dict):
            for req in schema.get("required", []):
                if req not in value:
                    errors.append(f"{path}: missing required field '{req}'")
            props = schema.get("properties", {})
            for key, subschema in props.items():
                if key in value:
                    validate_value(subschema, value[key], f"{path}.{key}", errors)

    if expected_type == "array" or isinstance(value, list):
        item_schema = schema.get("items")
        if item_schema and isinstance(value, list):
            for idx, item in enumerate(value):
                validate_value(item_schema, item, f"{path}[{idx}]", errors)


def validate_requirement_scores(record: dict, path: str, errors: list) -> None:
    """Validate that all score fields in a requirement are within [0, MAX_SCORE]."""
    scores = record.get("scores")
    if not isinstance(scores, dict):
        return
    for score_key, score_val in scores.items():
        if isinstance(score_val, (int, float)) and score_val > MAX_SCORE:
            errors.append(
                f"{path}.scores.{score_key}: value {score_val} exceeds maximum {MAX_SCORE}"
            )


def _iter_record_files(directory: Path):
    if not directory.exists():
        return
    for path in sorted(directory.iterdir()):
        if not path.is_file():
            continue
        if path.name.startswith("."):
            continue
        if "TEMPLATE" in path.name.upper():
            continue
        if path.suffix.lower() in (".json", ".yaml", ".yml"):
            yield path


def _iter_example_files(kind_keyword: str):
    if not EXAMPLES_DIR.exists():
        return
    for path in sorted(EXAMPLES_DIR.iterdir()):
        if not path.is_file():
            continue
        if kind_keyword in path.name.lower() and path.suffix.lower() in (".json", ".yaml", ".yml"):
            yield path


def collect(kind_dir: Path, example_keyword: str):
    """Return list of (path, record, problem, is_hard_error)."""
    out = []
    for path in list(_iter_record_files(kind_dir)) + list(_iter_example_files(example_keyword)):
        record, problem, is_error = load_record(path)
        out.append((path, record, problem, is_error))
    return out


def collect_split(kind_dir: Path, example_keyword: str):
    """Return (real_records, example_records) separately."""
    real = []
    for path in _iter_record_files(kind_dir):
        record, problem, is_error = load_record(path)
        real.append((path, record, problem, is_error))
    examples = []
    for path in _iter_example_files(example_keyword):
        record, problem, is_error = load_record(path)
        examples.append((path, record, problem, is_error))
    return real, examples


def rel(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT))
    except ValueError:
        return str(path)


# ----------------------------------------------------------------------------- CURRENT_FOCUS validation

def validate_current_focus(errors: list) -> None:
    """Validate CURRENT_FOCUS.yaml if it exists. Priority weights must not exceed MAX_SCORE."""
    if not CURRENT_FOCUS_FILE.exists():
        return
    if not HAVE_YAML:
        return
    try:
        with CURRENT_FOCUS_FILE.open(encoding="utf-8") as fh:
            focus = yaml.safe_load(fh)
    except Exception as exc:  # noqa: BLE001
        errors.append(f"{rel(CURRENT_FOCUS_FILE)}: could not parse: {exc}")
        return
    if not isinstance(focus, dict):
        errors.append(f"{rel(CURRENT_FOCUS_FILE)}: top-level must be a YAML object")
        return
    weights = focus.get("priority_weights")
    if isinstance(weights, dict):
        for key, val in weights.items():
            if isinstance(val, (int, float)) and val > MAX_SCORE:
                errors.append(
                    f"{rel(CURRENT_FOCUS_FILE)}: priority_weights.{key} = {val} exceeds maximum {MAX_SCORE}"
                )


# ----------------------------------------------------------------------------- commands

def cmd_validate() -> int:
    if not PRODUCT.exists():
        print("ERROR: /product directory not found", file=sys.stderr)
        return 2

    obs_schema = load_schema("observation.schema.json")
    req_schema = load_schema("requirement.schema.json")
    dec_schema = load_schema("decision.schema.json")
    test_run_schema = load_schema("test_run.schema.json")

    real_observations, example_observations = collect_split(OBSERVATIONS_DIR, "observation")
    real_requirements, example_requirements = collect_split(REQUIREMENTS_DIR, "requirement")
    real_decisions, example_decisions = collect_split(DECISIONS_DIR, "decision")
    real_test_runs, example_test_runs = collect_split(TEST_RUNS_DIR, "test-run")

    all_observations = real_observations + example_observations
    all_requirements = real_requirements + example_requirements
    all_decisions = real_decisions + example_decisions
    all_test_runs = real_test_runs + example_test_runs

    errors: list[str] = []
    notes: list[str] = []

    obs_ids, req_ids, dec_ids, test_run_ids = set(), set(), set(), set()
    all_ids: Counter = Counter()

    def process(records, schema, id_bucket):
        for path, record, problem, is_error in records:
            if record is None:
                if is_error and problem:
                    errors.append(f"{rel(path)}: {problem}")
                elif problem:
                    notes.append(f"{rel(path)}: {problem}")
                continue
            if not isinstance(record, dict):
                errors.append(f"{rel(path)}: top-level record must be an object")
                continue
            validate_value(schema, record, rel(path), errors)
            rid = record.get("id")
            if isinstance(rid, str):
                id_bucket.add(rid)
                all_ids[rid] += 1

    process(all_observations, obs_schema, obs_ids)
    process(all_requirements, req_schema, req_ids)
    process(all_decisions, dec_schema, dec_ids)
    process(all_test_runs, test_run_schema, test_run_ids)

    # Requirement score validation
    for path, record, _problem, _is_error in all_requirements:
        if isinstance(record, dict):
            validate_requirement_scores(record, rel(path), errors)

    # CURRENT_FOCUS.yaml validation
    validate_current_focus(errors)

    # unique IDs (globally)
    for rid, count in sorted(all_ids.items()):
        if count > 1:
            errors.append(f"duplicate id '{rid}' appears {count} times")

    # cross-references: requirements → observations, decisions
    for path, record, _problem, _is_error in all_requirements:
        if not isinstance(record, dict):
            continue
        for oid in record.get("linked_observations", []) or []:
            if oid not in obs_ids:
                errors.append(f"{rel(path)}: linked_observations references unknown observation '{oid}'")
        for did in record.get("related_decisions", []) or []:
            if did not in dec_ids:
                errors.append(f"{rel(path)}: related_decisions references unknown decision '{did}'")

    # cross-references: decisions → requirements
    for path, record, _problem, _is_error in all_decisions:
        if not isinstance(record, dict):
            continue
        for reqid in record.get("related_requirements", []) or []:
            if reqid not in req_ids:
                errors.append(f"{rel(path)}: related_requirements references unknown requirement '{reqid}'")

    # cross-references: observations → requirements + test_runs
    for path, record, _problem, _is_error in all_observations:
        if not isinstance(record, dict):
            continue
        for reqid in record.get("linked_requirements", []) or []:
            if reqid not in req_ids:
                errors.append(f"{rel(path)}: linked_requirements references unknown requirement '{reqid}'")
        for trid in record.get("linked_test_runs", []) or []:
            if trid not in test_run_ids:
                errors.append(
                    f"{rel(path)}: linked_test_runs references unknown test run '{trid}'"
                    " (must be TESTRUN-YYYY-NNN)"
                )

    # cross-references: test_runs → observations
    for path, record, _problem, _is_error in all_test_runs:
        if not isinstance(record, dict):
            continue
        for oid in record.get("linked_observations", []) or []:
            if oid not in obs_ids:
                errors.append(f"{rel(path)}: linked_observations references unknown observation '{oid}'")

    n_real_obs = sum(1 for _, r, _, _ in real_observations if isinstance(r, dict))
    n_real_reqs = sum(1 for _, r, _, _ in real_requirements if isinstance(r, dict))
    n_real_decs = sum(1 for _, r, _, _ in real_decisions if isinstance(r, dict))
    n_real_runs = sum(1 for _, r, _, _ in real_test_runs if isinstance(r, dict))

    for note in notes:
        print(f"note: {note}")

    if errors:
        print("\nPRODUCT VALIDATION: FAIL")
        for err in errors:
            print(f"  - {err}")
        print(f"\n{len(errors)} error(s).")
        return 1

    print("PRODUCT VALIDATION: PASS")
    print(
        f"validated records: "
        f"observations={n_real_obs} "
        f"requirements={n_real_reqs} "
        f"decisions={n_real_decs} "
        f"test_runs={n_real_runs}"
    )
    return 0


def cmd_summary() -> int:
    if not PRODUCT.exists():
        print("ERROR: /product directory not found", file=sys.stderr)
        return 2

    real_observations, example_observations = collect_split(OBSERVATIONS_DIR, "observation")
    real_requirements, example_requirements = collect_split(REQUIREMENTS_DIR, "requirement")
    real_decisions, example_decisions = collect_split(DECISIONS_DIR, "decision")
    real_test_runs, example_test_runs = collect_split(TEST_RUNS_DIR, "test-run")

    def parsed(records):
        return [r for _, r, _p, _e in records if isinstance(r, dict)]

    r_obs = parsed(real_observations)
    r_reqs = parsed(real_requirements)
    r_decs = parsed(real_decisions)
    r_runs = parsed(real_test_runs)
    e_obs = parsed(example_observations)
    e_reqs = parsed(example_requirements)
    e_decs = parsed(example_decisions)
    e_runs = parsed(example_test_runs)

    req_status = Counter(r.get("status", "unknown") for r in r_reqs)

    unresolved_high = [
        o.get("id", "?")
        for o in r_obs
        if o.get("severity") in ("high", "critical") and o.get("status") not in ("closed", "duplicate")
    ]

    print("LibreCare — Product Summary")
    print("=" * 34)
    print("Real backlog:")
    print(f"  Observations: {len(r_obs)}")
    print(f"  Test runs: {len(r_runs)}")
    print(f"  Requirements: {len(r_reqs)}")
    print(f"    CANDIDATE: {req_status.get('CANDIDATE', 0)}")
    print(f"    ACCEPTED:  {req_status.get('ACCEPTED', 0)}")
    print(f"    HOLD:      {req_status.get('HOLD', 0)}")
    print(f"    REJECTED:  {req_status.get('REJECTED', 0)}")
    print(f"  Decisions: {len(r_decs)}")
    print()
    print(f"Unresolved HIGH/CRITICAL observations: {len(unresolved_high)}")
    for oid in sorted(unresolved_high):
        print(f"    - {oid}")
    print()
    print("Examples (scaffold / reference — not product backlog):")
    print(f"  observations: {len(e_obs)}")
    print(f"  test-runs: {len(e_runs)}")
    print(f"  requirements: {len(e_reqs)}")
    print(f"  decisions: {len(e_decs)}")
    return 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(
        prog="product_cli.py",
        description="LibreCare Product Requirements Collector — validate/summary (deterministic, no AI).",
    )
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("validate", help="Validate all structured product records; non-zero exit on error.")
    sub.add_parser("summary", help="Print counts of observations/requirements.")

    args = parser.parse_args(argv)
    if args.command == "validate":
        return cmd_validate()
    if args.command == "summary":
        return cmd_summary()
    parser.print_help()
    return 2


if __name__ == "__main__":
    raise SystemExit(main())

