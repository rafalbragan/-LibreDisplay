#!/usr/bin/env python3
"""LibreCare Product CLI — deterministic validator + summary for /product records.

Phase 1 Requirements Collector MVP. No AI/LLM calls. Standard-library only for the JSON path.
YAML records are validated too *if* PyYAML happens to be installed; otherwise they are skipped
with a note (the deterministic CI path uses JSON records).

Usage:
    python scripts/product/product_cli.py validate
    python scripts/product/product_cli.py summary
"""
from __future__ import annotations

import argparse
import json
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

OBSERVATIONS_DIR = PRODUCT / "research" / "observations"
REQUIREMENTS_DIR = PRODUCT / "requirements"
DECISIONS_DIR = PRODUCT / "decisions"
EXAMPLES_DIR = PRODUCT / "examples"

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
    """Return (record_or_None, problem_or_None, is_hard_error).

    - JSON parse failure is a HARD error (a record file must be valid).
    - A YAML record when PyYAML is unavailable is a soft skip (note), not an error.
    """
    suffix = path.suffix.lower()
    if suffix == ".json":
        try:
            # utf-8-sig tolerates an optional BOM.
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
    """Minimal but strict JSON-Schema subset: type, enum, required, properties, items."""
    expected_type = schema.get("type")
    if expected_type and expected_type in JSON_TYPE_CHECKS:
        if not JSON_TYPE_CHECKS[expected_type](value):
            errors.append(f"{path}: expected type '{expected_type}', got '{type(value).__name__}'")
            return  # further checks would be noise once the type is wrong

    if "enum" in schema and value not in schema["enum"]:
        errors.append(f"{path}: value {value!r} not in allowed {schema['enum']}")

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


def _iter_record_files(directory: Path):
    if not directory.exists():
        return
    for path in sorted(directory.iterdir()):
        if not path.is_file():
            continue
        if path.name.startswith("."):  # .gitkeep etc.
            continue
        if "TEMPLATE" in path.name.upper():  # scaffolds are not records
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
    """Return list of (path, record, problem, is_hard_error). record is None when not parsed."""
    out = []
    for path in list(_iter_record_files(kind_dir)) + list(_iter_example_files(example_keyword)):
        record, problem, is_error = load_record(path)
        out.append((path, record, problem, is_error))
    return out


def rel(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT))
    except ValueError:
        return str(path)


# ----------------------------------------------------------------------------- commands

def cmd_validate() -> int:
    if not PRODUCT.exists():
        print("ERROR: /product directory not found", file=sys.stderr)
        return 2

    obs_schema = load_schema("observation.schema.json")
    req_schema = load_schema("requirement.schema.json")
    dec_schema = load_schema("decision.schema.json")

    observations = collect(OBSERVATIONS_DIR, "observation")
    requirements = collect(REQUIREMENTS_DIR, "requirement")
    decisions = collect(DECISIONS_DIR, "decision")

    errors: list[str] = []
    notes: list[str] = []

    obs_ids, req_ids, dec_ids = set(), set(), set()
    all_ids = Counter()

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

    process(observations, obs_schema, obs_ids)
    process(requirements, req_schema, req_ids)
    process(decisions, dec_schema, dec_ids)

    # unique IDs (globally)
    for rid, count in sorted(all_ids.items()):
        if count > 1:
            errors.append(f"duplicate id '{rid}' appears {count} times")

    # cross-references
    for path, record, _problem, _is_error in requirements:
        if not isinstance(record, dict):
            continue
        for oid in record.get("linked_observations", []) or []:
            if oid not in obs_ids:
                errors.append(f"{rel(path)}: linked_observations references unknown observation '{oid}'")
        for did in record.get("related_decisions", []) or []:
            if did not in dec_ids:
                errors.append(f"{rel(path)}: related_decisions references unknown decision '{did}'")

    for path, record, _problem, _is_error in decisions:
        if not isinstance(record, dict):
            continue
        for reqid in record.get("related_requirements", []) or []:
            if reqid not in req_ids:
                errors.append(f"{rel(path)}: related_requirements references unknown requirement '{reqid}'")

    for path, record, _problem, _is_error in observations:
        if not isinstance(record, dict):
            continue
        for reqid in record.get("linked_requirements", []) or []:
            if reqid not in req_ids:
                errors.append(f"{rel(path)}: linked_requirements references unknown requirement '{reqid}'")

    validated = len(obs_ids) + len(req_ids) + len(dec_ids)
    for note in notes:
        print(f"note: {note}")

    if errors:
        print("\nPRODUCT VALIDATION: FAIL")
        for err in errors:
            print(f"  - {err}")
        print(f"\n{len(errors)} error(s) across {validated} validated record(s).")
        return 1

    print("PRODUCT VALIDATION: PASS")
    print(f"validated records: observations={len(obs_ids)} requirements={len(req_ids)} decisions={len(dec_ids)}")
    return 0


def cmd_summary() -> int:
    if not PRODUCT.exists():
        print("ERROR: /product directory not found", file=sys.stderr)
        return 2

    observations = [r for _, r, _p, _e in collect(OBSERVATIONS_DIR, "observation") if isinstance(r, dict)]
    requirements = [r for _, r, _p, _e in collect(REQUIREMENTS_DIR, "requirement") if isinstance(r, dict)]

    by_type = Counter(o.get("type", "unknown") for o in observations)
    by_mode = Counter(o.get("mode", "unknown") for o in observations)
    by_persona = Counter(o.get("persona", "unknown") for o in observations)
    req_status = Counter(r.get("status", "unknown") for r in requirements)

    unresolved_high = [
        o.get("id", "?")
        for o in observations
        if o.get("severity") in ("high", "critical") and o.get("status") not in ("closed", "duplicate")
    ]

    print("LibreCare — Product Summary")
    print("=" * 34)
    print(f"Observations: {len(observations)}")
    print("  by type:")
    for k, v in sorted(by_type.items()):
        print(f"    - {k}: {v}")
    print("  by mode:")
    for k, v in sorted(by_mode.items()):
        print(f"    - {k}: {v}")
    print("  by persona:")
    for k, v in sorted(by_persona.items()):
        print(f"    - {k}: {v}")
    print()
    print(f"Requirements: {len(requirements)}")
    print(f"  CANDIDATE: {req_status.get('CANDIDATE', 0)}")
    print(f"  ACCEPTED:  {req_status.get('ACCEPTED', 0)}")
    print(f"  HOLD:      {req_status.get('HOLD', 0)}")
    print(f"  REJECTED:  {req_status.get('REJECTED', 0)}")
    other = {k: v for k, v in req_status.items() if k not in ("CANDIDATE", "ACCEPTED", "HOLD", "REJECTED")}
    for k, v in sorted(other.items()):
        print(f"  {k}: {v}")
    print()
    print(f"Unresolved HIGH/CRITICAL observations: {len(unresolved_high)}")
    for oid in sorted(unresolved_high):
        print(f"    - {oid}")
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

