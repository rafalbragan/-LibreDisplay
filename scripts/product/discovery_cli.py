#!/usr/bin/env python3
"""LibreCare Discovery Agent v1 (deterministic collector + bounded AI analysis).

The Discovery Agent collects compact evidence from configured public sources,
normalizes and deduplicates data, builds stable clusters, applies a single
bounded AI analysis pass, and produces ranked Product Inbox candidates.

The agent is advisory-only. It never accepts requirements, never starts
implementation, and never performs merge operations.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
from datetime import datetime, timezone
from html import unescape
from pathlib import Path
from typing import Callable
from urllib import error as urllib_error
from urllib import parse as urllib_parse
from urllib import request as urllib_request

try:
    import yaml  # type: ignore
    HAVE_YAML = True
except Exception:
    HAVE_YAML = False

if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

ROOT = Path(__file__).resolve().parents[2]
PRODUCT = ROOT / "product"
SCHEMA_DIR = PRODUCT / "schema"
OBSERVATIONS_DIR = PRODUCT / "research" / "observations"
REQUIREMENTS_DIR = PRODUCT / "requirements"
DECISIONS_DIR = PRODUCT / "decisions"
GENERATED_DISCOVERY_DIR = PRODUCT / "generated" / "discovery"
VALIDATED_CAPABILITIES_MD = PRODUCT / "generated" / "VALIDATED_CAPABILITIES.md"
SOURCES_CONFIG_DEFAULT = PRODUCT / "discovery" / "sources.json"

CLASSIFICATIONS = {
    "VALIDATED_CAPABILITY",
    "PRODUCT_PROBLEM",
    "PRODUCT_OPPORTUNITY",
    "TEST_COVERAGE_GAP",
    "SAFETY_GAP",
    "INCONCLUSIVE",
}

SOLVABILITY_VALUES = {"APP", "ABBOTT_LIMITATION", "EXTERNAL_ONLY", "MIXED"}
CONFIDENCE_VALUES = {"low", "medium", "high"}

ELIGIBLE_CLASSIFICATIONS = {"PRODUCT_PROBLEM", "PRODUCT_OPPORTUNITY", "SAFETY_GAP"}

SOURCE_FAMILIES = {"official_vendor", "github_community", "reddit", "other_community", "competitor"}
SOURCE_TYPE_BY_FAMILY = {
    "official_vendor": "community",
    "github_community": "community",
    "reddit": "community",
    "other_community": "community",
    "competitor": "competitor",
}

STOPWORDS = {
    "a", "an", "the", "and", "or", "to", "for", "of", "in", "on", "is", "are", "be", "it", "that",
    "this", "with", "as", "at", "by", "from", "i", "we", "you", "they", "he", "she", "can", "cannot",
    "nie", "oraz", "dla", "jest", "sie", "się", "jak", "przy", "or", "if", "then",
}


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def read_json(path: Path):
    with path.open("r", encoding="utf-8-sig") as fh:
        return json.load(fh)


def write_json(path: Path, payload: dict | list) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as fh:
        json.dump(payload, fh, ensure_ascii=False, indent=2)
        fh.write("\n")


def load_record(path: Path):
    suffix = path.suffix.lower()
    if suffix == ".json":
        return read_json(path)
    if suffix in {".yaml", ".yml"}:
        if not HAVE_YAML:
            raise RuntimeError("PyYAML is required for YAML parsing")
        with path.open("r", encoding="utf-8") as fh:
            return yaml.safe_load(fh)
    raise RuntimeError(f"Unsupported record type: {path}")


def iter_record_files(directory: Path):
    if not directory.exists():
        return
    for path in sorted(directory.iterdir()):
        if not path.is_file():
            continue
        if path.name.startswith("."):
            continue
        if "TEMPLATE" in path.name.upper():
            continue
        if path.suffix.lower() in {".json", ".yaml", ".yml"}:
            yield path


def canonicalize_url(url: str) -> str:
    parsed = urllib_parse.urlsplit(url.strip())
    scheme = (parsed.scheme or "https").lower()
    netloc = parsed.netloc.lower()
    path = re.sub(r"/+", "/", parsed.path or "/")
    if path != "/" and path.endswith("/"):
        path = path[:-1]

    q = urllib_parse.parse_qsl(parsed.query, keep_blank_values=False)
    kept = []
    for k, v in q:
        lk = k.lower()
        if lk.startswith("utm_"):
            continue
        if lk in {"fbclid", "gclid", "mc_cid", "mc_eid"}:
            continue
        kept.append((lk, v.strip()))
    kept.sort()
    query = urllib_parse.urlencode(kept)
    return urllib_parse.urlunsplit((scheme, netloc, path, query, ""))


def normalize_text(text: str) -> str:
    lowered = unescape(text).lower()
    lowered = re.sub(r"https?://\S+", " ", lowered)
    lowered = re.sub(r"[^\w\s]", " ", lowered, flags=re.UNICODE)
    lowered = re.sub(r"\s+", " ", lowered).strip()
    return lowered


def tokenize(text: str) -> list[str]:
    tokens = [t for t in normalize_text(text).split(" ") if t and t not in STOPWORDS and len(t) > 2]
    return tokens


def text_fingerprint(text: str) -> str:
    tokens = sorted(set(tokenize(text)))
    base = " ".join(tokens)
    return hashlib.sha256(base.encode("utf-8")).hexdigest()[:16]


def content_hash(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def jaccard_similarity(a_tokens: set[str], b_tokens: set[str]) -> float:
    if not a_tokens and not b_tokens:
        return 1.0
    if not a_tokens or not b_tokens:
        return 0.0
    inter = len(a_tokens & b_tokens)
    union = len(a_tokens | b_tokens)
    return inter / union if union else 0.0


def stable_cluster_id(problem_fingerprints: list[str]) -> str:
    material = "|".join(sorted(problem_fingerprints))
    digest = hashlib.sha1(material.encode("utf-8")).hexdigest()[:12].upper()
    return f"DISC-{digest}"


def _safe_excerpt(text: str, limit: int = 220) -> str:
    cleaned = re.sub(r"\s+", " ", text).strip()
    if len(cleaned) <= limit:
        return cleaned
    return cleaned[: limit - 3].rstrip() + "..."


def _first_sentence(text: str) -> str:
    compact = re.sub(r"\s+", " ", text).strip()
    if not compact:
        return ""
    match = re.split(r"(?<=[.!?])\s+", compact, maxsplit=1)
    return match[0]


def _problem_from_text(text: str) -> str:
    sentence = _first_sentence(text)
    if len(sentence) < 24:
        sentence = text
    sentence = _safe_excerpt(sentence, 180)
    return sentence


class SourceResult:
    def __init__(self, name: str, family: str, status: str, items: list[dict], detail: str = ""):
        self.name = name
        self.family = family
        self.status = status
        self.items = items
        self.detail = detail


class GitHubIssueClient:
    def __init__(self, owner: str, repo: str, token: str):
        self.owner = owner
        self.repo = repo
        self.token = token
        self.base = f"https://api.github.com/repos/{owner}/{repo}"

    def _request(self, method: str, path: str, payload: dict | None = None):
        url = f"{self.base}{path}"
        data = None
        headers = {
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {self.token}",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "LibreCare-Discovery-Agent/1.0",
        }
        if payload is not None:
            data = json.dumps(payload).encode("utf-8")
            headers["Content-Type"] = "application/json"
        req = urllib_request.Request(url, data=data, headers=headers, method=method)
        with urllib_request.urlopen(req, timeout=20) as resp:
            body = resp.read().decode("utf-8")
            return json.loads(body) if body else {}

    def find_issue_by_marker(self, marker: str) -> dict | None:
        page = 1
        while page <= 10:
            issues = self._request("GET", f"/issues?state=all&labels=product-inbox&per_page=100&page={page}")
            if not isinstance(issues, list) or not issues:
                return None
            for issue in issues:
                if "pull_request" in issue:
                    continue
                body = issue.get("body") or ""
                if marker in body:
                    return issue
            page += 1
        return None

    def create_issue(self, title: str, body: str, labels: list[str]) -> dict:
        return self._request("POST", "/issues", {"title": title, "body": body, "labels": labels})


GITHUB_CLIENT_FACTORY = GitHubIssueClient


def _fetch_url(url: str, timeout: float, retries: int) -> str:
    headers = {"User-Agent": "LibreCare-Discovery-Agent/1.0 (+public-source-analysis)"}
    last_err = None
    for _ in range(max(1, retries)):
        req = urllib_request.Request(url, headers=headers)
        try:
            with urllib_request.urlopen(req, timeout=timeout) as resp:
                raw = resp.read()
                return raw.decode("utf-8", errors="replace")
        except Exception as exc:  # noqa: BLE001
            last_err = exc
    raise RuntimeError(f"Fetch failed: {url}: {last_err}")


def _extract_html_text(html: str) -> str:
    # Compact text extraction: remove scripts/styles and tags.
    text = re.sub(r"<script\b[^>]*>.*?</script>", " ", html, flags=re.IGNORECASE | re.DOTALL)
    text = re.sub(r"<style\b[^>]*>.*?</style>", " ", text, flags=re.IGNORECASE | re.DOTALL)
    text = re.sub(r"<[^>]+>", " ", text)
    text = unescape(text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def _source_status_for_failure(name: str) -> str:
    upper = name.upper()
    if "ABBOTT" in upper:
        return "ABBOTT_SOURCE_DEGRADED"
    if "REDDIT" in upper:
        return "REDDIT_DISABLED"
    return f"{upper}_DEGRADED"


def collect_from_source(source: dict, max_items: int, timeout: float, retries: int) -> SourceResult:
    name = str(source.get("name", "unknown"))
    family = str(source.get("family", "other_community"))
    if family not in SOURCE_FAMILIES:
        return SourceResult(name=name, family=family, status="INVALID_SOURCE_FAMILY", items=[])

    if source.get("enabled") is False:
        return SourceResult(name=name, family=family, status="DISABLED", items=[])

    if family == "reddit":
        if not os.environ.get("REDDIT_CLIENT_ID") or not os.environ.get("REDDIT_CLIENT_SECRET"):
            return SourceResult(name=name, family=family, status="REDDIT_DISABLED", items=[])

    fixture_items = source.get("fixture_items") or []
    items: list[dict] = []

    for fixture in fixture_items[:max_items]:
        url = canonicalize_url(str(fixture.get("url", "")))
        text = str(fixture.get("text", "")).strip()
        if not url or not text:
            continue
        items.append({
            "canonical_url": url,
            "source_name": name,
            "source_family": family,
            "source_type": SOURCE_TYPE_BY_FAMILY.get(family, "community"),
            "retrieved_at": utc_now(),
            "content_hash": content_hash(text),
            "excerpt": _safe_excerpt(text),
            "problem_statement": _problem_from_text(fixture.get("problem_statement") or text),
            "persona": fixture.get("persona", "caregiver"),
            "mode": fixture.get("mode", fixture.get("persona", "caregiver")),
            "module": fixture.get("module", "Home / Monitoring"),
            "type": fixture.get("type", "usability"),
            "severity": fixture.get("severity", "medium"),
            "frequency": fixture.get("frequency", "occasional"),
            "confidence": fixture.get("confidence", "medium"),
        })

    urls = source.get("urls") or []
    if items:
        return SourceResult(name=name, family=family, status="OK", items=items)

    # Bounded, lightweight fetch from configured URLs when no fixture items are provided.
    fetched = 0
    for url in urls:
        if fetched >= max_items:
            break
        canon = canonicalize_url(str(url))
        try:
            body = _fetch_url(canon, timeout=timeout, retries=retries)
        except Exception as exc:  # noqa: BLE001
            return SourceResult(name=name, family=family, status=_source_status_for_failure(name), items=[], detail=str(exc))
        text = _extract_html_text(body)
        if not text:
            continue
        items.append({
            "canonical_url": canon,
            "source_name": name,
            "source_family": family,
            "source_type": SOURCE_TYPE_BY_FAMILY.get(family, "community"),
            "retrieved_at": utc_now(),
            "content_hash": content_hash(text),
            "excerpt": _safe_excerpt(text),
            "problem_statement": _problem_from_text(text),
            "persona": "caregiver",
            "mode": "caregiver",
            "module": "Home / Monitoring",
            "type": "usability",
            "severity": "low",
            "frequency": "unknown",
            "confidence": "low",
        })
        fetched += 1

    status = "OK" if items else "EMPTY"
    return SourceResult(name=name, family=family, status=status, items=items)


def dedupe_items(items: list[dict]) -> tuple[list[dict], dict]:
    exact_seen = set()
    text_seen = set()
    output = []
    stats = {"exact_duplicates": 0, "normalized_duplicates": 0}
    for item in items:
        exact_key = (item.get("canonical_url"), item.get("content_hash"))
        if exact_key in exact_seen:
            stats["exact_duplicates"] += 1
            continue
        exact_seen.add(exact_key)
        fp = text_fingerprint(item.get("problem_statement", ""))
        item["problem_fingerprint"] = fp
        if fp in text_seen:
            stats["normalized_duplicates"] += 1
            continue
        text_seen.add(fp)
        output.append(item)
    return output, stats


def cluster_items(items: list[dict], threshold: float = 0.58) -> list[dict]:
    clusters = []
    for item in sorted(items, key=lambda i: (i.get("problem_fingerprint", ""), i.get("canonical_url", ""))):
        tokens = set(tokenize(item.get("problem_statement", "")))
        placed = False
        for cluster in clusters:
            sim = jaccard_similarity(tokens, cluster["token_union"])
            if sim >= threshold:
                cluster["items"].append(item)
                cluster["token_union"] = cluster["token_union"] | tokens
                placed = True
                break
        if not placed:
            clusters.append({"items": [item], "token_union": tokens})

    out = []
    for raw in clusters:
        c_items = raw["items"]
        problem_fps = [x["problem_fingerprint"] for x in c_items]
        cluster_id = stable_cluster_id(problem_fps)
        source_families = sorted(set(x["source_family"] for x in c_items))
        evidence = [x.get("excerpt") or _safe_excerpt(x.get("problem_statement", "")) for x in c_items]
        urls = sorted(set(x["canonical_url"] for x in c_items))
        persona_counts: dict[str, int] = {}
        module_counts: dict[str, int] = {}
        for it in c_items:
            persona_counts[it.get("persona", "unknown")] = persona_counts.get(it.get("persona", "unknown"), 0) + 1
            module_counts[it.get("module", "unknown")] = module_counts.get(it.get("module", "unknown"), 0) + 1
        persona = sorted(persona_counts.items(), key=lambda kv: (-kv[1], kv[0]))[0][0]
        module = sorted(module_counts.items(), key=lambda kv: (-kv[1], kv[0]))[0][0]
        normalized_problem = sorted(c_items, key=lambda i: len(i.get("problem_statement", "")), reverse=True)[0]["problem_statement"]
        out.append({
            "cluster_id": cluster_id,
            "normalized_problem": normalized_problem,
            "persona_candidate": persona,
            "module_candidate": module,
            "evidence_items": evidence,
            "source_urls": urls,
            "source_families": source_families,
            "source_count": len(c_items),
            "independent_source_family_count": len(source_families),
            "fingerprints": sorted(set(problem_fps)),
            "raw_items": c_items,
        })
    out.sort(key=lambda c: c["cluster_id"])
    return out


def _text_similarity(a: str, b: str) -> float:
    return jaccard_similarity(set(tokenize(a)), set(tokenize(b)))


def load_foundation_index() -> dict:
    requirements = []
    decisions = []
    observations = []
    capabilities = []

    for path in iter_record_files(REQUIREMENTS_DIR) or []:
        try:
            rec = load_record(path)
        except Exception:
            continue
        requirements.append({
            "id": rec.get("id", path.stem),
            "text": str(rec.get("problem", "")),
            "status": str(rec.get("status", "")),
            "path": str(path.relative_to(ROOT)),
        })

    for path in iter_record_files(DECISIONS_DIR) or []:
        try:
            rec = load_record(path)
        except Exception:
            continue
        decisions.append({
            "id": rec.get("id", path.stem),
            "subject": str(rec.get("subject", "")),
            "decision": str(rec.get("decision", "")),
            "status": str(rec.get("status", "")),
            "path": str(path.relative_to(ROOT)),
        })

    for path in iter_record_files(OBSERVATIONS_DIR) or []:
        try:
            rec = load_record(path)
        except Exception:
            continue
        observations.append({
            "id": rec.get("id", path.stem),
            "text": str(rec.get("problem_statement", "")),
            "path": str(path.relative_to(ROOT)),
            "cluster_id": rec.get("cluster_id"),
            "problem_fingerprint": rec.get("problem_fingerprint"),
        })

    if VALIDATED_CAPABILITIES_MD.exists():
        text = VALIDATED_CAPABILITIES_MD.read_text(encoding="utf-8")
        for line in text.splitlines():
            if line.strip().startswith("**Capability:**"):
                capabilities.append(line.split("**Capability:**", 1)[1].strip())

    return {
        "requirements": requirements,
        "decisions": decisions,
        "observations": observations,
        "capabilities": capabilities,
    }


def match_cluster_to_foundation(cluster: dict, foundation: dict) -> dict:
    problem = cluster["normalized_problem"]
    best_req = (0.0, None)
    for req in foundation["requirements"]:
        score = _text_similarity(problem, req["text"])
        if score > best_req[0]:
            best_req = (score, req)

    best_cap = (0.0, None)
    for cap in foundation["capabilities"]:
        score = _text_similarity(problem, cap)
        if score > best_cap[0]:
            best_cap = (score, cap)

    best_dec = (0.0, None)
    for dec in foundation["decisions"]:
        composite = f"{dec['subject']} {dec['decision']}"
        score = _text_similarity(problem, composite)
        if score > best_dec[0]:
            best_dec = (score, dec)

    suppress_reproposal = False
    suppress_reason = ""
    linked = []

    if best_req[1] and best_req[0] >= 0.68:
        linked.append({"type": "requirement", "id": best_req[1]["id"], "score": round(best_req[0], 3), "path": best_req[1]["path"]})
    if best_cap[1] and best_cap[0] >= 0.68:
        linked.append({"type": "validated_capability", "id": best_cap[1], "score": round(best_cap[0], 3)})
    if best_dec[1] and best_dec[0] >= 0.68:
        linked.append({"type": "decision", "id": best_dec[1]["id"], "status": best_dec[1]["status"], "score": round(best_dec[0], 3), "path": best_dec[1]["path"]})
        if str(best_dec[1]["status"]).upper() in {"HOLD", "REJECT", "REJECTED"}:
            suppress_reproposal = True
            suppress_reason = f"Existing decision {best_dec[1]['id']} has status {best_dec[1]['status']}"

    return {
        "linked": linked,
        "suppress_reproposal": suppress_reproposal,
        "suppress_reason": suppress_reason,
        "best_requirement_score": best_req[0],
        "best_capability_score": best_cap[0],
    }


def build_ai_prompt(run_id: str, clusters: list[dict], model: str) -> str:
    compact = []
    for c in clusters:
        compact.append({
            "cluster_id": c["cluster_id"],
            "normalized_problem": c["normalized_problem"],
            "persona_candidate": c["persona_candidate"],
            "module_candidate": c["module_candidate"],
            "independent_source_family_count": c["independent_source_family_count"],
            "source_families": c["source_families"],
            "evidence_items": c["evidence_items"][:6],
            "source_urls": c["source_urls"][:12],
            "foundation_match": c.get("foundation_match", {}),
        })

    instruction = {
        "task": "Classify LibreCare discovery clusters. Advisory only.",
        "model": model,
        "required_output": {
            "type": "object",
            "required": ["clusters"],
            "clusters_item_required": [
                "cluster_id", "classification", "persona", "problem_statement",
                "evidence_summary", "source_diversity_summary", "current_librecare_match",
                "solvability", "impact_score", "frequency_score", "evidence_score",
                "solvability_score", "novelty_score", "effort_score", "confidence",
                "counterargument", "candidate_recommendation"
            ],
            "classification_enum": sorted(CLASSIFICATIONS),
            "solvability_enum": sorted(SOLVABILITY_VALUES),
            "confidence_enum": sorted(CONFIDENCE_VALUES),
        },
        "constraints": [
            "Return JSON only.",
            "Problem statement must describe problem, not solution.",
            "Exactly one classification per cluster.",
            "Do not accept requirements or start implementation.",
        ],
        "clusters": compact,
    }
    return json.dumps(instruction, ensure_ascii=False, indent=2)


def run_copilot_json(prompt: str, model: str) -> str:
    cmd = [
        "copilot",
        "-s",
        "--no-ask-user",
        "--disable-builtin-mcps",
        "--available-tools=view,grep,glob",
        "--allow-tool=read",
        "--deny-tool=write",
        "--deny-tool=shell",
        f"--model={model}",
    ]
    proc = subprocess.run(
        cmd,
        input=prompt,
        capture_output=True,
        text=True,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(f"Copilot CLI failed: {proc.stderr.strip()[:400]}")
    return proc.stdout


def extract_json_object(raw: str) -> dict:
    text = raw.strip()
    if text.startswith("```"):
        text = re.sub(r"^```(?:json)?", "", text).strip()
        text = re.sub(r"```$", "", text).strip()
    try:
        return json.loads(text)
    except Exception:
        match = re.search(r"\{[\s\S]*\}", text)
        if not match:
            raise
        return json.loads(match.group(0))


def validate_ai_output(payload: dict, clusters: list[dict]) -> tuple[bool, list[str]]:
    errors = []
    if not isinstance(payload, dict):
        return False, ["AI payload is not a JSON object"]
    entries = payload.get("clusters")
    if not isinstance(entries, list):
        return False, ["AI payload missing 'clusters' array"]
    expected_ids = {c["cluster_id"] for c in clusters}
    got_ids = set()
    required = {
        "cluster_id", "classification", "persona", "problem_statement", "evidence_summary",
        "source_diversity_summary", "current_librecare_match", "solvability", "impact_score",
        "frequency_score", "evidence_score", "solvability_score", "novelty_score", "effort_score",
        "confidence", "counterargument", "candidate_recommendation"
    }
    for idx, row in enumerate(entries):
        if not isinstance(row, dict):
            errors.append(f"clusters[{idx}] is not an object")
            continue
        missing = [k for k in sorted(required) if k not in row]
        if missing:
            errors.append(f"clusters[{idx}] missing fields: {missing}")
            continue
        cid = str(row["cluster_id"])
        got_ids.add(cid)
        if cid not in expected_ids:
            errors.append(f"clusters[{idx}] unknown cluster_id: {cid}")
        if row["classification"] not in CLASSIFICATIONS:
            errors.append(f"clusters[{idx}] invalid classification: {row['classification']}")
        if row["solvability"] not in SOLVABILITY_VALUES:
            errors.append(f"clusters[{idx}] invalid solvability: {row['solvability']}")
        if row["confidence"] not in CONFIDENCE_VALUES:
            errors.append(f"clusters[{idx}] invalid confidence: {row['confidence']}")
        for score_key in ["impact_score", "frequency_score", "evidence_score", "solvability_score", "novelty_score", "effort_score"]:
            val = row.get(score_key)
            if not isinstance(val, int) or val < 0 or val > 5:
                errors.append(f"clusters[{idx}] {score_key} must be int 0..5")
    missing_cluster_ids = expected_ids - got_ids
    if missing_cluster_ids:
        errors.append(f"AI output missing clusters: {sorted(missing_cluster_ids)}")
    return not errors, errors


def apply_governance(cluster: dict, ai_row: dict) -> dict:
    out = dict(ai_row)
    match = cluster.get("foundation_match", {})

    # Deterministic overrides: existing validated capability evidence suppresses proposals.
    if match.get("best_capability_score", 0.0) >= 0.68:
        out["classification"] = "VALIDATED_CAPABILITY"

    if match.get("best_requirement_score", 0.0) >= 0.75:
        out["suppressed"] = True
        out["suppressed_reason"] = "Strong match to existing requirement"

    if match.get("suppress_reproposal"):
        out["suppressed"] = True
        out["suppressed_reason"] = match.get("suppress_reason")

    if out.get("classification") not in CLASSIFICATIONS:
        out["classification"] = "INCONCLUSIVE"

    if out.get("classification") in {"VALIDATED_CAPABILITY", "TEST_COVERAGE_GAP", "INCONCLUSIVE"}:
        out["eligible_for_inbox"] = False
        out["exclusion_reason"] = f"Classification {out['classification']} is not eligible"
    elif out.get("solvability") in {"ABBOTT_LIMITATION", "EXTERNAL_ONLY"}:
        out["eligible_for_inbox"] = False
        out["exclusion_reason"] = f"Solvability {out['solvability']} not app-solvable"
    elif out.get("suppressed"):
        out["eligible_for_inbox"] = False
        out["exclusion_reason"] = out.get("suppressed_reason", "Suppressed by existing decision")
    else:
        out["eligible_for_inbox"] = out.get("classification") in ELIGIBLE_CLASSIFICATIONS

    return out


def compute_score(cluster: dict, decision: dict) -> int:
    impact = int(decision.get("impact_score", 0))
    freq = int(decision.get("frequency_score", 0))
    evidence = int(decision.get("evidence_score", 0))
    solvability = int(decision.get("solvability_score", 0))
    novelty = int(decision.get("novelty_score", 0))
    effort = int(decision.get("effort_score", 0))

    source_diversity = min(cluster.get("independent_source_family_count", 0), 3)
    diversity_component = source_diversity * 4

    strategic_fit = 5 if cluster.get("persona_candidate") == "caregiver" else 3

    raw = (
        impact * 9
        + freq * 8
        + evidence * 8
        + solvability * 9
        + novelty * 7
        + diversity_component
        + strategic_fit * 3
        - effort * 5
    )
    return max(0, min(100, int(raw)))


def next_observation_name(existing_names: set[str], date_stamp: str) -> tuple[str, str]:
    n = 1
    while True:
        obs_id = f"OBS-{date_stamp}-{n:02d}"
        filename = f"{obs_id}.json"
        if filename not in existing_names:
            return obs_id, filename
        n += 1


def load_existing_observation_fingerprints() -> tuple[set[str], set[str]]:
    fingerprints = set()
    cluster_ids = set()
    for path in iter_record_files(OBSERVATIONS_DIR) or []:
        try:
            rec = load_record(path)
        except Exception:
            continue
        fp = rec.get("problem_fingerprint")
        cid = rec.get("cluster_id")
        if isinstance(fp, str) and fp:
            fingerprints.add(fp)
        if isinstance(cid, str) and cid:
            cluster_ids.add(cid)
    return fingerprints, cluster_ids


def create_observations(clusters_ranked: list[dict], run_id: str) -> list[str]:
    created_files = []
    OBSERVATIONS_DIR.mkdir(parents=True, exist_ok=True)
    existing_files = {p.name for p in OBSERVATIONS_DIR.glob("OBS-*.json")}
    existing_fp, existing_cluster_ids = load_existing_observation_fingerprints()
    date_stamp = datetime.now(timezone.utc).strftime("%Y%m%d")

    for row in clusters_ranked:
        cls = row["governed"]["classification"]
        if cls not in ELIGIBLE_CLASSIFICATIONS:
            continue
        cid = row["cluster"]["cluster_id"]
        fp = row["cluster"]["fingerprints"][0] if row["cluster"]["fingerprints"] else ""
        if cid in existing_cluster_ids or (fp and fp in existing_fp):
            continue
        obs_id, filename = next_observation_name(existing_files, date_stamp)
        payload = {
            "id": obs_id,
            "created_at": utc_now(),
            "source_type": row["cluster"]["raw_items"][0].get("source_type", "community"),
            "source_reference": row["cluster"]["source_urls"][0],
            "persona": row["governed"].get("persona", row["cluster"].get("persona_candidate", "unknown")),
            "mode": row["cluster"].get("persona_candidate", "unknown"),
            "module": row["cluster"].get("module_candidate", "unknown"),
            "type": row["cluster"]["raw_items"][0].get("type", "usability"),
            "severity": row["cluster"]["raw_items"][0].get("severity", "medium"),
            "frequency": row["cluster"]["raw_items"][0].get("frequency", "unknown"),
            "confidence": row["cluster"]["raw_items"][0].get("confidence", "medium"),
            "evidence": row["cluster"]["evidence_items"][:3],
            "problem_statement": row["governed"]["problem_statement"],
            "status": "new",
            "cluster_id": cid,
            "problem_fingerprint": fp,
            "discovery_run_id": run_id,
        }
        path = OBSERVATIONS_DIR / filename
        write_json(path, payload)
        existing_files.add(filename)
        existing_cluster_ids.add(cid)
        if fp:
            existing_fp.add(fp)
        created_files.append(str(path.relative_to(ROOT)))

    return created_files


def build_inbox_issue_body(entry: dict, marker: str) -> str:
    cluster = entry["cluster"]
    governed = entry["governed"]
    safety_prefix = "\nWYMAGA PRZEGLĄDU BEZPIECZEŃSTWA\n" if governed["classification"] == "SAFETY_GAP" else ""
    lines = [
        "### Typ zgłoszenia",
        "Problem" if governed["classification"] in {"PRODUCT_PROBLEM", "SAFETY_GAP"} else "Pomysł",
        "",
        "### Persona użytkownika",
        cluster.get("persona_candidate", "Opiekun"),
        "",
        "### Moduł",
        cluster.get("module_candidate", "Inne / nie wiem"),
        "",
        "### Co zauważyłeś(-aś) / czego potrzebujesz?",
        governed.get("problem_statement", ""),
        "",
        "### Dlaczego to ważne?",
        f"Discovery score: {entry['score']}/100. {governed.get('candidate_recommendation', '')}" + safety_prefix,
        "",
        "### Kontekst / przykład",
        f"Na podstawie dostępnych danych: {governed.get('evidence_summary', '')}",
        "",
        "Źródła:",
    ]
    lines.extend([f"- {u}" for u in cluster.get("source_urls", [])[:8]])
    lines.extend(["", marker])
    return "\n".join(lines)


def publish_top3(top_ranked: list[dict], repo_owner: str, repo_name: str, github_token: str) -> list[dict]:
    client = GITHUB_CLIENT_FACTORY(repo_owner, repo_name, github_token)
    created = []
    for entry in top_ranked[:3]:
        if not entry["governed"].get("eligible_for_inbox"):
            continue
        marker = f"<!-- LIBRECARE_DISCOVERY_CLUSTER: {entry['cluster']['cluster_id']} -->"
        existing = client.find_issue_by_marker(marker)
        if existing:
            created.append({
                "cluster_id": entry["cluster"]["cluster_id"],
                "action": "SKIPPED_EXISTS",
                "issue_number": existing.get("number"),
                "issue_url": existing.get("html_url"),
            })
            continue

        title = f"[Skrzynka Produktowa] Discovery {entry['cluster']['cluster_id']}"
        body = build_inbox_issue_body(entry, marker)
        issue = client.create_issue(title=title, body=body, labels=["product-inbox"])
        created.append({
            "cluster_id": entry["cluster"]["cluster_id"],
            "action": "CREATED",
            "issue_number": issue.get("number"),
            "issue_url": issue.get("html_url"),
        })
    return created


def run_discovery(
    sources_file: Path,
    max_items_per_source: int,
    publish_top3_flag: bool,
    repo_owner: str,
    repo_name: str,
    github_token: str,
    ai_mode: str,
    ai_model: str,
    ai_response_file: Path | None,
    timeout: float,
    retries: int,
) -> dict:
    started_at = utc_now()
    run_stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M")
    run_id = f"DISCOVERY-{run_stamp}"

    status = "SUCCESS"
    errors = []
    ai_calls = 0

    sources_cfg = read_json(sources_file)
    sources = sources_cfg.get("sources", [])

    source_results = []
    all_items = []
    for source in sources:
        result = collect_from_source(source, max_items=max_items_per_source, timeout=timeout, retries=retries)
        source_results.append({
            "name": result.name,
            "family": result.family,
            "status": result.status,
            "detail": result.detail,
            "count": len(result.items),
        })
        all_items.extend(result.items)
        if result.status.endswith("DEGRADED") or result.status == "REDDIT_DISABLED":
            if status == "SUCCESS":
                status = "DEGRADED"

    deduped_items, dedupe_stats = dedupe_items(all_items)
    clusters = cluster_items(deduped_items)

    foundation = load_foundation_index()
    for c in clusters:
        c["foundation_match"] = match_cluster_to_foundation(c, foundation)

    if ai_response_file:
        raw = ai_response_file.read_text(encoding="utf-8")
        ai_calls = 1
    elif ai_mode == "copilot" and clusters:
        prompt = build_ai_prompt(run_id, clusters, model=ai_model)
        raw = run_copilot_json(prompt, model=ai_model)
        ai_calls = 1
    else:
        # Deterministic fallback for offline/test environments.
        payload = {"clusters": []}
        for c in clusters:
            cls = "PRODUCT_PROBLEM"
            if c["foundation_match"].get("best_capability_score", 0) >= 0.68:
                cls = "VALIDATED_CAPABILITY"
            payload["clusters"].append({
                "cluster_id": c["cluster_id"],
                "classification": cls,
                "persona": c["persona_candidate"],
                "problem_statement": c["normalized_problem"],
                "evidence_summary": _safe_excerpt("; ".join(c["evidence_items"]), 160),
                "source_diversity_summary": f"{c['source_count']} items / {c['independent_source_family_count']} families",
                "current_librecare_match": str(c.get("foundation_match", {}).get("linked", [])),
                "solvability": "APP",
                "impact_score": 3,
                "frequency_score": 3,
                "evidence_score": 3,
                "solvability_score": 3,
                "novelty_score": 3,
                "effort_score": 2,
                "confidence": "medium",
                "counterargument": "Evidence may still be incomplete.",
                "candidate_recommendation": "Needs human review.",
            })
        raw = json.dumps(payload)
        ai_calls = 0

    ai_payload = {}
    if clusters:
        try:
            ai_payload = extract_json_object(raw)
        except Exception as exc:  # noqa: BLE001
            status = "FAILED"
            errors.append(f"AI output parse failed: {exc}")
            ai_payload = {}

    governed_rows = []
    if clusters and ai_payload:
        valid, ai_errors = validate_ai_output(ai_payload, clusters)
        if not valid:
            status = "FAILED"
            errors.extend(ai_errors)
        else:
            ai_map = {x["cluster_id"]: x for x in ai_payload["clusters"]}
            for cluster in clusters:
                governed = apply_governance(cluster, ai_map[cluster["cluster_id"]])
                score = compute_score(cluster, governed)
                governed_rows.append({"cluster": cluster, "governed": governed, "score": score})

    if ai_calls > 2:
        status = "FAILED"
        errors.append(f"AI call budget exceeded: {ai_calls} > 2")

    governed_rows.sort(key=lambda r: (-r["score"], r["cluster"]["cluster_id"]))
    top10 = governed_rows[:10]

    created_observations = []
    created_issues = []
    if status != "FAILED":
        created_observations = create_observations(top10, run_id)
        if publish_top3_flag and top10:
            if not (repo_owner and repo_name and github_token):
                status = "DEGRADED"
                errors.append("publish_top3 requested but GitHub credentials/repo not provided")
            else:
                created_issues = publish_top3(top10, repo_owner=repo_owner, repo_name=repo_name, github_token=github_token)

    finished_at = utc_now()
    report = {
        "run_id": run_id,
        "started_at": started_at,
        "finished_at": finished_at,
        "status": status,
        "counts": {
            "COLLECTED": len(all_items),
            "AFTER_DEDUPE": len(deduped_items),
            "CLUSTERS": len(clusters),
            "AI_CALLS": ai_calls,
        },
        "source_status": source_results,
        "dedupe": dedupe_stats,
        "top10": [
            {
                "rank": idx + 1,
                "score": row["score"],
                "cluster_id": row["cluster"]["cluster_id"],
                "problem": row["governed"].get("problem_statement"),
                "persona": row["governed"].get("persona"),
                "classification": row["governed"].get("classification"),
                "evidence_count": len(row["cluster"].get("evidence_items", [])),
                "source_families": row["cluster"].get("source_families", []),
                "current_librecare_match": row["governed"].get("current_librecare_match"),
                "solvability": row["governed"].get("solvability"),
                "effort": row["governed"].get("effort_score"),
                "confidence": row["governed"].get("confidence"),
                "counterargument": row["governed"].get("counterargument"),
                "source_urls": row["cluster"].get("source_urls", []),
                "eligibility": bool(row["governed"].get("eligible_for_inbox")),
                "exclusion_reason": row["governed"].get("exclusion_reason", ""),
                "suppressed_reason": row["governed"].get("suppressed_reason", ""),
            }
            for idx, row in enumerate(top10)
        ],
        "created_observations": created_observations,
        "top3_issue_actions": created_issues,
        "errors": errors,
        "safety_guards": {
            "max_ai_calls": 2,
            "ai_calls_used": ai_calls,
            "no_auto_acceptance": True,
            "no_auto_implementation": True,
            "no_auto_merge": True,
        },
    }

    GENERATED_DISCOVERY_DIR.mkdir(parents=True, exist_ok=True)
    json_path = GENERATED_DISCOVERY_DIR / f"DISCOVERY-{run_stamp}.json"
    md_path = GENERATED_DISCOVERY_DIR / f"DISCOVERY-{run_stamp}.md"
    write_json(json_path, report)
    md_path.write_text(render_markdown_report(report), encoding="utf-8")

    report["json_report_path"] = str(json_path.relative_to(ROOT))
    report["md_report_path"] = str(md_path.relative_to(ROOT))
    return report


def render_markdown_report(report: dict) -> str:
    lines = [
        f"# LibreCare Discovery Report — {report['run_id']}",
        "",
        f"- Status: **{report['status']}**",
        f"- Started: {report['started_at']}",
        f"- Finished: {report['finished_at']}",
        f"- COLLECTED: {report['counts']['COLLECTED']}",
        f"- AFTER_DEDUPE: {report['counts']['AFTER_DEDUPE']}",
        f"- CLUSTERS: {report['counts']['CLUSTERS']}",
        f"- AI_CALLS: {report['counts']['AI_CALLS']}",
        "",
        "## Source Status",
        "",
    ]
    for src in report.get("source_status", []):
        lines.append(f"- {src['name']}: {src['status']} ({src['count']})")

    lines.extend(["", "## TOP CANDIDATES", ""])

    def _emit_section(title: str, predicate: Callable[[dict], bool]):
        lines.append(f"## {title}")
        lines.append("")
        emitted = 0
        for item in report.get("top10", []):
            if not predicate(item):
                continue
            emitted += 1
            lines.append(f"TOP {item['rank']} — {item['score']}/100")
            lines.append("")
            lines.append("Problem:")
            lines.append(item.get("problem", ""))
            lines.append("")
            lines.append("Persona:")
            lines.append(str(item.get("persona", "")))
            lines.append("")
            lines.append("Classification:")
            lines.append(str(item.get("classification", "")))
            lines.append("")
            lines.append("Evidence:")
            lines.append(f"{item.get('evidence_count', 0)} items / {len(item.get('source_families', []))} independent families")
            lines.append("")
            lines.append("Current coverage:")
            lines.append(str(item.get("current_librecare_match", "")))
            lines.append("")
            lines.append("Solvability:")
            lines.append(str(item.get("solvability", "")))
            lines.append("")
            lines.append("Effort:")
            lines.append(str(item.get("effort", "")))
            lines.append("")
            lines.append("Confidence:")
            lines.append(str(item.get("confidence", "")))
            lines.append("")
            lines.append("Counterargument:")
            lines.append(str(item.get("counterargument", "")))
            lines.append("")
            lines.append("Sources:")
            for url in item.get("source_urls", []):
                lines.append(f"- {url}")
            lines.append("")
        if emitted == 0:
            lines.append("Brak pozycji.")
            lines.append("")

    _emit_section("WORTH REVIEWING", lambda i: i.get("eligibility") is True)
    _emit_section("ALREADY COVERED", lambda i: i.get("classification") == "VALIDATED_CAPABILITY")
    _emit_section("ABBOTT LIMITATION / NOT LIBRECARE-SOLVABLE", lambda i: i.get("solvability") in {"ABBOTT_LIMITATION", "EXTERNAL_ONLY"})
    _emit_section("TEST / RESEARCH GAPS", lambda i: i.get("classification") == "TEST_COVERAGE_GAP")
    _emit_section("INSUFFICIENT EVIDENCE", lambda i: i.get("classification") == "INCONCLUSIVE")

    if report.get("errors"):
        lines.extend(["## Errors", ""])
        for err in report["errors"]:
            lines.append(f"- {err}")

    return "\n".join(lines).rstrip() + "\n"


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="LibreCare Discovery Agent v1")
    parser.add_argument("--sources-file", default=str(SOURCES_CONFIG_DEFAULT))
    parser.add_argument("--max-items-per-source", type=int, default=20)
    parser.add_argument("--publish-top3", action="store_true")
    parser.add_argument("--repo-owner", default="")
    parser.add_argument("--repo-name", default="")
    parser.add_argument("--github-token", default="")
    parser.add_argument("--ai-mode", choices=["copilot", "heuristic"], default="heuristic")
    parser.add_argument("--ai-model", default="gpt-5.4-mini")
    parser.add_argument("--ai-response-file", default="")
    parser.add_argument("--timeout", type=float, default=12.0)
    parser.add_argument("--retries", type=int, default=2)
    parser.add_argument("--output-file", default="")
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    sources_file = Path(args.sources_file)
    ai_response_file = Path(args.ai_response_file) if args.ai_response_file else None

    if args.max_items_per_source < 1 or args.max_items_per_source > 200:
        print("ERROR: --max-items-per-source must be 1..200", file=sys.stderr)
        return 2

    report = run_discovery(
        sources_file=sources_file,
        max_items_per_source=args.max_items_per_source,
        publish_top3_flag=bool(args.publish_top3),
        repo_owner=args.repo_owner,
        repo_name=args.repo_name,
        github_token=args.github_token,
        ai_mode=args.ai_mode,
        ai_model=args.ai_model,
        ai_response_file=ai_response_file,
        timeout=args.timeout,
        retries=args.retries,
    )

    if args.output_file:
        write_json(Path(args.output_file), report)

    print(f"DISCOVERY_RUN_ID: {report['run_id']}")
    print(f"DISCOVERY_STATUS: {report['status']}")
    print(f"DISCOVERY_REPORT_JSON: {report['json_report_path']}")
    print(f"DISCOVERY_REPORT_MD: {report['md_report_path']}")
    print(f"DISCOVERY_AI_CALLS: {report['counts']['AI_CALLS']}")
    if report.get("errors"):
        for err in report["errors"]:
            print(f"DISCOVERY_ERROR: {err}")

    return 0 if report["status"] != "FAILED" else 1


if __name__ == "__main__":
    raise SystemExit(main())

