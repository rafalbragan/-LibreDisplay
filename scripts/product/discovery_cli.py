#!/usr/bin/env python3
"""LibreCare Discovery Agent v1 (deterministic collector + bounded AI analysis)."""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import re
import subprocess
import sys
import time
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
DISCOVERY_CACHE_PATH = GENERATED_DISCOVERY_DIR / "cache" / "discovery-cache-v1.json"
DISCOVERY_CLUSTER_REGISTRY_PATH = PRODUCT / "discovery" / "cluster-registry.json"
DISCOVERY_PROPOSED_CLUSTER_REGISTRY_PATH = GENERATED_DISCOVERY_DIR / "cluster-registry.proposed.json"

CLUSTER_REGISTRY_VERSION = 1
CLUSTER_REGISTRY_MATCH_THRESHOLD = 0.72
CLUSTER_REGISTRY_AMBIGUITY_DELTA = 0.02

FORBIDDEN_CACHE_KEYS = {
    "access_token",
    "refresh_token",
    "authorization",
    "client_secret",
    "reddit_client_secret",
    "github_token",
    "password",
    "cookie",
    "set-cookie",
}

MAX_CACHED_EXCERPT_LENGTH = 220
MAX_CACHED_PROBLEM_LENGTH = 180
MAX_CACHED_TEXT_LENGTH = 500

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
    "a",
    "an",
    "the",
    "and",
    "or",
    "to",
    "for",
    "of",
    "in",
    "on",
    "is",
    "are",
    "be",
    "it",
    "that",
    "this",
    "with",
    "as",
    "at",
    "by",
    "from",
    "i",
    "we",
    "you",
    "they",
    "he",
    "she",
    "can",
    "cannot",
    "nie",
    "oraz",
    "dla",
    "jest",
    "sie",
    "się",
    "jak",
    "przy",
    "if",
    "then",
}

CANONICAL_NOISE_TOKENS = {
    "has",
    "have",
    "had",
    "doing",
    "done",
    "today",
    "now",
    "currently",
    "recently",
    "just",
}

CANONICAL_TOKEN_ALIASES = {
    "cant": "cannot",
    "struggle": "cannot",
    "struggles": "cannot",
    "struggled": "cannot",
    "trouble": "cannot",
    "troubles": "cannot",
    "troubled": "cannot",
    "unable": "cannot",
    "difficult": "cannot",
    "difficulty": "cannot",
    "detects": "detect",
    "detected": "detect",
    "detecting": "detect",
    "detection": "detect",
    "readings": "reading",
    "caregivers": "caregiver",
}


class SourceResult:
    def __init__(self, name: str, family: str, status: str, items: list[dict], detail: str = ""):
        self.name = name
        self.family = family
        self.status = status
        self.items = items
        self.detail = detail


class DiscoveryCache:
    def __init__(self, path: Path):
        self.path = path
        self._data = {"version": 1, "entries": {}}
        self._load()

    def _load(self) -> None:
        if not self.path.exists():
            return
        try:
            payload = json.loads(self.path.read_text(encoding="utf-8"))
            if isinstance(payload, dict) and isinstance(payload.get("entries"), dict):
                self._data = payload
        except Exception:
            # Invalid cache must never break run determinism.
            self._data = {"version": 1, "entries": {}}

    def get(self, key: str, max_age_seconds: int):
        entries = self._data.get("entries", {})
        row = entries.get(key)
        if not isinstance(row, dict):
            return None
        ts = row.get("cached_at")
        if not isinstance(ts, (int, float)):
            return None
        if time.time() - ts > max(1, max_age_seconds):
            return None
        value = row.get("value")
        return value

    def put(self, key: str, value) -> None:
        _validate_cache_payload(value)
        self._data.setdefault("entries", {})
        self._data["entries"][key] = {"cached_at": int(time.time()), "value": value}

    def save(self) -> None:
        _validate_cache_payload(self._data)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        serialized = json.dumps(self._data, ensure_ascii=False, indent=2)
        # Defensive redaction guard: never persist likely credentials.
        lowered = serialized.lower()
        for marker in ["reddit_client_secret", "authorization", "bearer ", "github_token", "client_secret", "access_token", "refresh_token"]:
            if marker in lowered:
                raise RuntimeError("Cache serialization blocked: credential-like marker detected")
        self.path.write_text(serialized + "\n", encoding="utf-8")


def _truncate_text(text: str, limit: int) -> str:
    cleaned = re.sub(r"\s+", " ", str(text)).strip()
    if len(cleaned) <= limit:
        return cleaned
    return cleaned[: limit - 3].rstrip() + "..."


def _validate_cache_payload(value, path: str = "cache") -> None:
    if isinstance(value, dict):
        for key, item in value.items():
            key_str = str(key).lower()
            if key_str in FORBIDDEN_CACHE_KEYS:
                raise RuntimeError(f"Cache serialization blocked: forbidden key detected at {path}.{key_str}")
            _validate_cache_payload(item, f"{path}.{key_str}")
        return
    if isinstance(value, list):
        for idx, item in enumerate(value):
            _validate_cache_payload(item, f"{path}[{idx}]")
        return
    if isinstance(value, str):
        lowered = value.lower()
        for marker in ["<html", "</html", "<body", "</body", "<script", "</script", "<style", "</style"]:
            if marker in lowered:
                raise RuntimeError(f"Cache serialization blocked: raw HTML detected at {path}")


def _normalize_cached_item(item: dict, source_name: str, source_family: str, source_type: str) -> dict | None:
    normalized = _normalize_item_fields(item, source_name, source_family, source_type)
    if not normalized:
        return None
    cached = {
        "canonical_url": normalized["canonical_url"],
        "source_name": normalized["source_name"],
        "source_family": normalized["source_family"],
        "source_identity": normalized["source_identity"],
        "source_type": normalized["source_type"],
        "retrieved_at": normalized["retrieved_at"],
        "content_hash": normalized["content_hash"],
        "excerpt": _truncate_text(normalized["problem_statement"], MAX_CACHED_EXCERPT_LENGTH),
        "problem_statement": _truncate_text(normalized["problem_statement"], MAX_CACHED_PROBLEM_LENGTH),
        "canonical_problem_key": normalized["canonical_problem_key"],
        "canonical_problem_fingerprint": normalized["canonical_problem_fingerprint"],
        "persona": normalized["persona"],
        "mode": normalized["mode"],
        "module": normalized["module"],
        "type": normalized["type"],
        "severity": normalized["severity"],
        "frequency": normalized["frequency"],
        "confidence": normalized["confidence"],
    }
    return cached


def _cache_get_items(cache: DiscoveryCache, key: str, max_age_seconds: int) -> list[dict] | None:
    value = cache.get(key, max_age_seconds)
    if not isinstance(value, list):
        return None
    items = [item for item in value if isinstance(item, dict)]
    return items or None


def _cache_put_items(cache: DiscoveryCache, key: str, items: list[dict]) -> None:
    cache.put(key, items)


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


def _cluster_registry_payload(payload) -> dict:
    if not isinstance(payload, dict):
        return {"version": CLUSTER_REGISTRY_VERSION, "entries": []}
    entries = payload.get("entries")
    if not isinstance(entries, list):
        entries = []
    out = []
    for raw in entries:
        if not isinstance(raw, dict):
            continue
        cid = str(raw.get("cluster_id") or "").strip()
        key = str(raw.get("canonical_problem_key") or "").strip()
        if not cid or not key:
            continue
        out.append(
            {
                "cluster_id": cid,
                "canonical_problem_key": _truncate_text(key, 220),
                "problem_fingerprint": str(raw.get("problem_fingerprint") or "").strip(),
                "persona": _truncate_text(str(raw.get("persona") or "unknown"), 80),
                "module": _truncate_text(str(raw.get("module") or "unknown"), 120),
                "created_at": str(raw.get("created_at") or "").strip(),
                "last_seen_at": str(raw.get("last_seen_at") or "").strip(),
            }
        )
    out.sort(key=lambda x: x["cluster_id"])
    return {"version": int(payload.get("version") or CLUSTER_REGISTRY_VERSION), "entries": out}


def load_cluster_registry(path: Path = DISCOVERY_CLUSTER_REGISTRY_PATH) -> dict:
    if not path.exists():
        return {"version": CLUSTER_REGISTRY_VERSION, "entries": []}
    try:
        payload = read_json(path)
        cleaned = _cluster_registry_payload(payload)
        version = cleaned.get("version")
        if version != CLUSTER_REGISTRY_VERSION:
            raise ValueError(f"Unsupported registry version: {version}. Expected {CLUSTER_REGISTRY_VERSION}.")
        return cleaned
    except ValueError as ve:
        # Only re-raise ValueError that we explicitly raised above (version check)
        # Do NOT re-raise JSONDecodeError which is a subclass of ValueError
        if "Unsupported registry version" in str(ve):
            raise
        # All other errors (including JSONDecodeError) should be wrapped
        raise RuntimeError(f"Failed to load cluster registry from {path}: {ve}") from ve
    except Exception as e:
        raise RuntimeError(f"Failed to load cluster registry from {path}: {e}") from e


def _deterministic_cluster_id_for_key(canonical_problem_key: str, persona: str, module: str) -> str:
    material = "|".join([canonical_problem_key.strip(), persona.strip(), module.strip()])
    digest = hashlib.sha1(material.encode("utf-8")).hexdigest()[:12].upper()
    return f"DISC-{digest}"


def _registry_match_score(cluster: dict, entry: dict) -> float:
    cluster_tokens = set(_canonical_problem_tokens(str(cluster.get("canonical_problem_key") or "")))
    entry_tokens = set(_canonical_problem_tokens(str(entry.get("canonical_problem_key") or "")))
    score = jaccard_similarity(cluster_tokens, entry_tokens)
    persona = str(cluster.get("persona_candidate") or "")
    module = str(cluster.get("module_candidate") or "")
    if persona and entry.get("persona") and persona != entry.get("persona"):
        score *= 0.9
    if module and entry.get("module") and module != entry.get("module"):
        score *= 0.95
    return score


def resolve_cluster_ids_with_registry(
    clusters: list[dict],
    registry: dict,
    now_iso: str,
    observation_clusters: list[dict] | None = None,
) -> tuple[list[dict], dict, dict]:
    entries = [dict(x) for x in (registry.get("entries") or []) if isinstance(x, dict)]
    existing_ids = {str(x.get("cluster_id") or "") for x in entries}
    matched_existing = 0
    new_entries = 0
    ambiguous_matches = []

    for cluster in clusters:
        cluster_key = str(cluster.get("canonical_problem_key") or canonical_problem_key(str(cluster.get("normalized_problem") or "")))
        cluster["canonical_problem_key"] = cluster_key
        persona = str(cluster.get("persona_candidate") or "unknown")
        module = str(cluster.get("module_candidate") or "unknown")

        scored = []
        for entry in entries:
            score = _registry_match_score(cluster, entry)
            if score >= CLUSTER_REGISTRY_MATCH_THRESHOLD:
                scored.append((score, str(entry.get("cluster_id") or ""), entry))
        scored.sort(key=lambda x: (-x[0], x[1]))

        if len(scored) >= 2 and abs(scored[0][0] - scored[1][0]) <= CLUSTER_REGISTRY_AMBIGUITY_DELTA:
            cluster["identity_ambiguous"] = True
            cluster["identity_ambiguous_candidates"] = [
                {"cluster_id": scored[0][1], "score": round(scored[0][0], 3)},
                {"cluster_id": scored[1][1], "score": round(scored[1][0], 3)},
            ]
            ambiguous_matches.append(
                {
                    "provisional_cluster_id": cluster.get("cluster_id"),
                    "canonical_problem_key": cluster_key,
                    "candidates": cluster["identity_ambiguous_candidates"],
                }
            )
            continue

        if scored:
            chosen = scored[0][2]
            cluster["cluster_id"] = str(chosen.get("cluster_id") or cluster["cluster_id"])
            cluster["identity_ambiguous"] = False
            chosen["last_seen_at"] = now_iso
            matched_existing += 1
            continue

        reused_cluster_id = _reuse_existing_cluster_id(cluster, observation_clusters or [])
        if reused_cluster_id:
            cluster_id = reused_cluster_id
        else:
            cluster_id = _deterministic_cluster_id_for_key(cluster_key, persona, module)
        if cluster_id in existing_ids:
            salt = hashlib.sha1(f"{cluster_key}|{persona}|{module}|{len(entries)}".encode("utf-8")).hexdigest()[:6].upper()
            cluster_id = f"DISC-{salt}{cluster_id[-6:]}"
        cluster["cluster_id"] = cluster_id
        cluster["identity_ambiguous"] = False
        entry = {
            "cluster_id": cluster_id,
            "canonical_problem_key": _truncate_text(cluster_key, 220),
            "problem_fingerprint": str((cluster.get("fingerprints") or [""])[0]),
            "persona": _truncate_text(persona, 80),
            "module": _truncate_text(module, 120),
            "created_at": now_iso,
            "last_seen_at": now_iso,
        }
        entries.append(entry)
        existing_ids.add(cluster_id)
        new_entries += 1

    entries.sort(key=lambda x: x["cluster_id"])
    proposed = {"version": CLUSTER_REGISTRY_VERSION, "entries": entries}
    summary = {
        "existing_entries": len(registry.get("entries") or []),
        "matched_existing": matched_existing,
        "new_entries": new_entries,
        "ambiguous_matches": ambiguous_matches,
        "changed": bool(new_entries or matched_existing),
    }
    return clusters, summary, proposed


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
    return [t for t in normalize_text(text).split(" ") if t and t not in STOPWORDS and len(t) > 2]


def text_fingerprint(text: str) -> str:
    tokens = sorted(set(tokenize(text)))
    return hashlib.sha256(" ".join(tokens).encode("utf-8")).hexdigest()[:16]


def _stem_canonical_token(token: str) -> str:
    if len(token) > 6 and token.endswith("ies"):
        return token[:-3] + "y"
    if len(token) > 5 and token.endswith("ing"):
        return token[:-3]
    if len(token) > 4 and token.endswith("ed"):
        return token[:-2]
    if len(token) > 4 and token.endswith("s"):
        return token[:-1]
    return token


def _canonical_problem_tokens(text: str) -> list[str]:
    canonical = []
    for token in tokenize(text):
        mapped = CANONICAL_TOKEN_ALIASES.get(token, token)
        mapped = _stem_canonical_token(mapped)
        if not mapped or mapped in STOPWORDS or mapped in CANONICAL_NOISE_TOKENS:
            continue
        if len(mapped) > 2:
            canonical.append(mapped)
    return sorted(set(canonical))


def canonical_problem_key(text: str) -> str:
    tokens = _canonical_problem_tokens(text)
    if not tokens:
        return text_fingerprint(text)
    return " ".join(tokens)


def canonical_problem_fingerprint(text: str) -> str:
    return hashlib.sha256(canonical_problem_key(text).encode("utf-8")).hexdigest()[:16]


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


def _safe_excerpt(text: str, limit: int = 220) -> str:
    cleaned = re.sub(r"\s+", " ", text).strip()
    if len(cleaned) <= limit:
        return cleaned
    return cleaned[: limit - 3].rstrip() + "..."


def _first_sentence(text: str) -> str:
    compact = re.sub(r"\s+", " ", text).strip()
    if not compact:
        return ""
    return re.split(r"(?<=[.!?])\s+", compact, maxsplit=1)[0]


def _problem_from_text(text: str) -> str:
    sentence = _first_sentence(text)
    if len(sentence) < 24:
        sentence = text
    return _safe_excerpt(sentence, 180)


def _extract_html_text(html: str) -> str:
    text = re.sub(r"<script\b[^>]*>.*?</script>", " ", html, flags=re.IGNORECASE | re.DOTALL)
    text = re.sub(r"<style\b[^>]*>.*?</style>", " ", text, flags=re.IGNORECASE | re.DOTALL)
    text = re.sub(r"<[^>]+>", " ", text)
    return re.sub(r"\s+", " ", unescape(text)).strip()


def _extract_html_title(html: str) -> str:
    m = re.search(r"<title[^>]*>(.*?)</title>", html, flags=re.IGNORECASE | re.DOTALL)
    if not m:
        return ""
    return _safe_excerpt(unescape(re.sub(r"\s+", " ", m.group(1))).strip(), 140)


def _source_status_for_failure(name: str) -> str:
    upper = name.upper()
    if "ABBOTT" in upper:
        return "ABBOTT_SOURCE_DEGRADED"
    if "REDDIT" in upper:
        return "REDDIT_DISABLED"
    return f"{upper}_DEGRADED"


def _bounded_retry_after(resp_headers) -> float:
    value = ""
    if resp_headers:
        value = resp_headers.get("Retry-After", "")
    try:
        seconds = float(value)
    except Exception:
        seconds = 0.0
    return max(0.0, min(seconds, 3.0))


def _request_bytes(
    url: str,
    method: str,
    timeout: float,
    retries: int,
    headers: dict | None = None,
    data: bytes | None = None,
) -> bytes:
    last_exc = None
    for attempt in range(max(1, retries)):
        req = urllib_request.Request(url, headers=headers or {}, method=method, data=data)
        try:
            with urllib_request.urlopen(req, timeout=timeout) as resp:
                return resp.read()
        except urllib_error.HTTPError as exc:
            last_exc = exc
            if exc.code == 429 and attempt + 1 < max(1, retries):
                time.sleep(_bounded_retry_after(exc.headers))
                continue
            raise
        except Exception as exc:  # noqa: BLE001
            last_exc = exc
    raise RuntimeError(f"Fetch failed: {url}: {last_exc}")


def _fetch_url(url: str, timeout: float, retries: int) -> str:
    headers = {"User-Agent": "LibreCare-Discovery-Agent/1.0 (+public-source-analysis)"}
    raw = _request_bytes(url, "GET", timeout=timeout, retries=retries, headers=headers)
    return raw.decode("utf-8", errors="replace")


def _fetch_json(url: str, timeout: float, retries: int, headers: dict | None = None) -> dict | list:
    merged_headers = {"User-Agent": "LibreCare-Discovery-Agent/1.0 (+public-source-analysis)"}
    if headers:
        merged_headers.update(headers)
    raw = _request_bytes(url, "GET", timeout=timeout, retries=retries, headers=merged_headers)
    return json.loads(raw.decode("utf-8", errors="replace"))


def _post_form_json(url: str, form: dict[str, str], timeout: float, retries: int, headers: dict | None = None) -> dict:
    merged_headers = {"User-Agent": "LibreCare-Discovery-Agent/1.0 (+public-source-analysis)"}
    if headers:
        merged_headers.update(headers)
    body = urllib_parse.urlencode(form).encode("utf-8")
    raw = _request_bytes(url, "POST", timeout=timeout, retries=retries, headers=merged_headers, data=body)
    payload = json.loads(raw.decode("utf-8", errors="replace"))
    return payload if isinstance(payload, dict) else {}


def _cache_key(source_name: str, suffix: str) -> str:
    return f"{source_name}::{suffix}"


def _normalize_item_fields(item: dict, source_name: str, source_family: str, source_type: str) -> dict | None:
    url = canonicalize_url(str(item.get("url") or item.get("canonical_url") or "").strip())
    text = str(item.get("text") or "").strip()
    if not url or not text:
        return None
    problem = _problem_from_text(str(item.get("problem_statement") or text))
    canonical_key = canonical_problem_key(problem)
    return {
        "canonical_url": url,
        "source_name": source_name,
        "source_family": source_family,
        "source_identity": source_name,
        "source_type": source_type,
        "retrieved_at": utc_now(),
        "content_hash": content_hash(text),
        "excerpt": _truncate_text(_safe_excerpt(text), MAX_CACHED_TEXT_LENGTH),
        "problem_statement": _truncate_text(problem, MAX_CACHED_PROBLEM_LENGTH),
        "canonical_problem_key": canonical_key,
        "canonical_problem_fingerprint": canonical_problem_fingerprint(problem),
        "persona": item.get("persona", "caregiver"),
        "mode": item.get("mode", item.get("persona", "caregiver")),
        "module": item.get("module", "Home / Monitoring"),
        "type": item.get("type", "usability"),
        "severity": item.get("severity", "medium"),
        "frequency": item.get("frequency", "occasional"),
        "confidence": item.get("confidence", "medium"),
    }


def _collect_fixture_items(source: dict, max_items: int) -> list[dict]:
    source_name = str(source.get("name", "unknown"))
    source_family = str(source.get("family", "other_community"))
    source_type = SOURCE_TYPE_BY_FAMILY.get(source_family, "community")
    out: list[dict] = []
    for raw in (source.get("fixture_items") or [])[:max_items]:
        item = _normalize_item_fields(raw, source_name, source_family, source_type)
        if item:
            out.append(item)
    return out


def _collect_official_pages(source: dict, max_items: int, timeout: float, retries: int, cache: DiscoveryCache, cache_max_age_seconds: int) -> list[dict]:
    out: list[dict] = []
    source_name = str(source.get("name", "unknown"))
    source_family = str(source.get("family", "official_vendor"))
    source_type = SOURCE_TYPE_BY_FAMILY.get(source_family, "community")
    for idx, raw_url in enumerate(source.get("urls") or []):
        if idx >= max_items:
            break
        canon = canonicalize_url(str(raw_url))
        key = _cache_key(source_name, f"html:{canon}")
        cached_items = _cache_get_items(cache, key, cache_max_age_seconds)
        if cached_items is not None:
            out.extend(cached_items[: max_items - len(out)])
            continue

        html = _fetch_url(canon, timeout=timeout, retries=retries)
        title = _extract_html_title(html)
        plain = _extract_html_text(html)
        if not plain:
            continue
        headline = title or _problem_from_text(plain)
        text = f"{headline}. {_safe_excerpt(plain, 500)}"
        item = _normalize_cached_item(
            {
                "url": canon,
                "text": text,
                "problem_statement": headline,
                "persona": "caregiver",
                "mode": "caregiver",
                "module": "Home / Monitoring",
                "type": "usability",
                "severity": "low",
                "frequency": "unknown",
                "confidence": "low",
            },
            source_name,
            source_family,
            source_type,
        )
        if item:
            _cache_put_items(cache, key, [item])
            out.append(item)
    return out


def _collect_github_issues(source: dict, max_items: int, timeout: float, retries: int, cache: DiscoveryCache, cache_max_age_seconds: int) -> list[dict]:
    out: list[dict] = []
    source_name = str(source.get("name", "unknown"))
    source_family = str(source.get("family", "github_community"))
    source_type = SOURCE_TYPE_BY_FAMILY.get(source_family, "community")
    repos = source.get("repos") or []
    if not repos:
        url_guess = ""
        urls = source.get("urls") or []
        if urls:
            url_guess = str(urls[0])
        match = re.search(r"github\.com/([^/]+)/([^/]+)", url_guess)
        if match:
            repos = [f"{match.group(1)}/{match.group(2)}"]

    for repo_full in repos:
        if len(out) >= max_items:
            break
        owner_repo = str(repo_full).strip().strip("/")
        if "/" not in owner_repo:
            continue
        owner, repo = owner_repo.split("/", 1)
        api_url = f"https://api.github.com/repos/{owner}/{repo}/issues?state=open&per_page={min(100, max_items)}"
        key = _cache_key(source_name, f"gh:{owner_repo}")
        cached_items = _cache_get_items(cache, key, cache_max_age_seconds)
        if cached_items is not None:
            for item in cached_items:
                if len(out) >= max_items:
                    break
                out.append(item)
            continue

        data = _fetch_json(api_url, timeout=timeout, retries=retries, headers={"Accept": "application/vnd.github+json"})
        normalized_items: list[dict] = []
        if not isinstance(data, list):
            continue
        for issue in data:
            if len(out) >= max_items:
                break
            if not isinstance(issue, dict):
                continue
            if issue.get("pull_request"):
                continue
            issue_url = canonicalize_url(str(issue.get("html_url") or ""))
            title = str(issue.get("title") or "").strip()
            body = str(issue.get("body") or "").strip()
            if not issue_url or not title:
                continue
            text = f"{title}. {_safe_excerpt(body, 260)}" if body else title
            item = _normalize_cached_item(
                {
                    "url": issue_url,
                    "text": text,
                    "problem_statement": title,
                    "persona": "caregiver",
                    "mode": "caregiver",
                    "module": "Home / Monitoring",
                    "type": "usability",
                    "severity": "medium",
                    "frequency": "unknown",
                    "confidence": "medium",
                },
                source_name,
                source_family,
                source_type,
            )
            if item:
                normalized_items.append(item)
                out.append(item)
        if normalized_items:
            _cache_put_items(cache, key, normalized_items)
    return out


def _collect_reddit_oauth(source: dict, max_items: int, timeout: float, retries: int, cache: DiscoveryCache, cache_max_age_seconds: int) -> SourceResult:
    name = str(source.get("name", "reddit"))
    family = str(source.get("family", "reddit"))
    if not os.environ.get("REDDIT_CLIENT_ID") or not os.environ.get("REDDIT_CLIENT_SECRET"):
        return SourceResult(name=name, family=family, status="REDDIT_DISABLED", items=[])

    cid = os.environ.get("REDDIT_CLIENT_ID", "")
    secret = os.environ.get("REDDIT_CLIENT_SECRET", "")
    basic = base64.b64encode(f"{cid}:{secret}".encode("utf-8")).decode("ascii")
    token_headers = {
        "Authorization": f"Basic {basic}",
        "Content-Type": "application/x-www-form-urlencoded",
    }
    token_payload = _post_form_json(
        "https://www.reddit.com/api/v1/access_token",
        {"grant_type": "client_credentials"},
        timeout=timeout,
        retries=retries,
        headers=token_headers,
    )

    access_token = ""
    if isinstance(token_payload, dict):
        access_token = str(token_payload.get("access_token") or "")
    if not access_token:
        return SourceResult(name=name, family=family, status="REDDIT_DISABLED", items=[], detail="oauth_token_missing")

    subreddits = source.get("subreddits") or ["diabetes", "Type1Diabetes"]
    out: list[dict] = []
    for sub in subreddits:
        if len(out) >= max_items:
            break
        limit = min(50, max_items - len(out))
        endpoint = f"https://oauth.reddit.com/r/{sub}/new?limit={limit}&raw_json=1"
        key = _cache_key(name, f"reddit:{sub}")
        cached_items = _cache_get_items(cache, key, cache_max_age_seconds)
        if cached_items is not None:
            out.extend(cached_items[: max_items - len(out)])
            continue

        data = _fetch_json(endpoint, timeout=timeout, retries=retries, headers={"Authorization": f"Bearer {access_token}"})
        normalized_items: list[dict] = []
        if isinstance(data, dict):
            posts = ((data.get("data") or {}).get("children") or [])
        else:
            posts = []
        for post in posts:
            if len(out) >= max_items:
                break
            if not isinstance(post, dict):
                continue
            pdata = post.get("data") or {}
            title = str(pdata.get("title") or "").strip()
            selftext = str(pdata.get("selftext") or "").strip()
            permalink = str(pdata.get("permalink") or "").strip()
            if not title or not permalink:
                continue
            url = canonicalize_url(f"https://www.reddit.com{permalink}")
            text = f"{title}. {_safe_excerpt(selftext, 240)}" if selftext else title
            item = _normalize_cached_item(
                {
                    "url": url,
                    "text": text,
                    "problem_statement": title,
                    "persona": "caregiver",
                    "mode": "caregiver",
                    "module": "Home / Monitoring",
                    "type": "usability",
                    "severity": "medium",
                    "frequency": "occasional",
                    "confidence": "low",
                },
                name,
                family,
                SOURCE_TYPE_BY_FAMILY.get(family, "community"),
            )
            if item:
                normalized_items.append(item)
                out.append(item)
        if normalized_items:
            _cache_put_items(cache, key, normalized_items)

    return SourceResult(name=name, family=family, status="OK" if out else "EMPTY", items=out)


def collect_from_source(
    source: dict,
    max_items: int,
    timeout: float,
    retries: int,
    cache: DiscoveryCache | None = None,
    cache_max_age_seconds: int = 21600,
) -> SourceResult:
    name = str(source.get("name", "unknown"))
    family = str(source.get("family", "other_community"))
    if family not in SOURCE_FAMILIES:
        return SourceResult(name=name, family=family, status="INVALID_SOURCE_FAMILY", items=[])
    if source.get("enabled") is False:
        return SourceResult(name=name, family=family, status="DISABLED", items=[])

    fixture_items = _collect_fixture_items(source, max_items=max_items)
    if fixture_items:
        return SourceResult(name=name, family=family, status="OK", items=fixture_items)

    local_cache = cache or DiscoveryCache(DISCOVERY_CACHE_PATH)

    kind = str(source.get("kind") or "").strip().lower()
    if not kind:
        if family == "reddit":
            kind = "reddit_oauth"
        elif "github" in name:
            kind = "github_issues"
        else:
            kind = "official_pages"

    try:
        if kind == "reddit_oauth":
            return _collect_reddit_oauth(source, max_items, timeout, retries, local_cache, cache_max_age_seconds)
        if kind == "github_issues":
            items = _collect_github_issues(source, max_items, timeout, retries, local_cache, cache_max_age_seconds)
            return SourceResult(name=name, family=family, status="OK" if items else "EMPTY", items=items)
        if kind == "official_pages":
            items = _collect_official_pages(source, max_items, timeout, retries, local_cache, cache_max_age_seconds)
            return SourceResult(name=name, family=family, status="OK" if items else "EMPTY", items=items)
        return SourceResult(name=name, family=family, status="INVALID_SOURCE_KIND", items=[])
    except Exception as exc:  # noqa: BLE001
        return SourceResult(name=name, family=family, status=_source_status_for_failure(name), items=[], detail=str(exc))


def dedupe_items(items: list[dict]) -> tuple[list[dict], dict]:
    exact_seen = set()
    normalized_seen_in_source = set()
    out = []
    stats = {"exact_duplicates": 0, "normalized_duplicates": 0}
    for item in items:
        source_identity = str(item.get("source_identity") or item.get("source_name") or "unknown")
        exact_key = (source_identity, item.get("canonical_url"), item.get("content_hash"))
        if exact_key in exact_seen:
            stats["exact_duplicates"] += 1
            continue
        exact_seen.add(exact_key)

        problem_text = str(item.get("problem_statement") or "")
        fp = text_fingerprint(problem_text)
        item["problem_fingerprint"] = fp
        item["canonical_problem_key"] = canonical_problem_key(problem_text)
        item["canonical_problem_fingerprint"] = canonical_problem_fingerprint(problem_text)
        dup_key = (source_identity, fp)
        if dup_key in normalized_seen_in_source:
            stats["normalized_duplicates"] += 1
            continue
        normalized_seen_in_source.add(dup_key)
        out.append(item)
    return out, stats


def _cluster_representative_item(items: list[dict]) -> dict:
    return sorted(
        items,
        key=lambda item: (
            str(item.get("canonical_problem_fingerprint") or item.get("problem_fingerprint") or ""),
            str(item.get("canonical_url") or ""),
            str(item.get("problem_statement") or ""),
        ),
    )[0]


def _stable_cluster_id_from_items(items: list[dict]) -> str:
    material = _cluster_canonical_problem_key(items)
    if not material:
        representative = _cluster_representative_item(items)
        material = "|".join(
            [
                str(representative.get("canonical_problem_key") or canonical_problem_key(str(representative.get("problem_statement") or ""))),
                str(representative.get("canonical_url") or ""),
                str(representative.get("source_family") or ""),
            ]
        )
    digest = hashlib.sha1(material.encode("utf-8")).hexdigest()[:12].upper()
    return f"DISC-{digest}"


def _cluster_canonical_problem_key(items: list[dict]) -> str:
    token_counts: dict[str, int] = {}
    item_count = 0
    for item in items:
        tokens = set(_canonical_problem_tokens(str(item.get("problem_statement") or "")))
        if not tokens:
            continue
        item_count += 1
        for token in tokens:
            token_counts[token] = token_counts.get(token, 0) + 1
    if not token_counts:
        return ""
    threshold = max(1, (item_count + 1) // 2)
    anchors = sorted([token for token, count in token_counts.items() if count >= threshold])
    if not anchors:
        anchors = sorted(token_counts.keys())
    return " ".join(anchors)


def _existing_cluster_matches() -> list[dict]:
    matches: list[dict] = []
    for path in iter_record_files(OBSERVATIONS_DIR) or []:
        try:
            rec = load_record(path)
        except Exception:
            continue
        text = str(rec.get("problem_statement") or rec.get("text") or "").strip()
        cid = str(rec.get("cluster_id") or "").strip()
        if not text or not cid:
            continue
        matches.append(
            {
                "cluster_id": cid,
                "problem_statement": text,
                "problem_fingerprint": str(rec.get("problem_fingerprint") or "").strip(),
                "persona": str(rec.get("persona") or "").strip(),
                "module": str(rec.get("module") or "").strip(),
                "source_type": str(rec.get("source_type") or "").strip(),
            }
        )
    return matches


def _reuse_existing_cluster_id(cluster: dict, existing_clusters: list[dict]) -> str | None:
    if not existing_clusters:
        return None
    problem = str(cluster.get("normalized_problem") or "").strip()
    if not problem:
        return None
    persona = str(cluster.get("persona_candidate") or "").strip()
    module = str(cluster.get("module_candidate") or "").strip()
    best_score = 0.0
    best_cluster_id = None
    for existing in existing_clusters:
        score = _text_similarity(problem, existing["problem_statement"])
        if persona and existing.get("persona") and persona != existing.get("persona"):
            score *= 0.9
        if module and existing.get("module") and module != existing.get("module"):
            score *= 0.95
        if score > best_score:
            best_score = score
            best_cluster_id = existing["cluster_id"]
    return best_cluster_id if best_score >= 0.70 else None


def cluster_items(items: list[dict], threshold: float = 0.40, existing_clusters: list[dict] | None = None) -> list[dict]:
    clusters: list[dict] = []
    for item in sorted(items, key=lambda i: (i.get("problem_fingerprint", ""), i.get("canonical_url", ""))):
        tokens = set(tokenize(str(item.get("problem_statement") or "")))
        placed = False
        for cluster in clusters:
            best_sim = 0.0
            for existing in cluster["items"]:
                existing_tokens = set(tokenize(str(existing.get("problem_statement") or "")))
                best_sim = max(best_sim, jaccard_similarity(tokens, existing_tokens))
            if best_sim >= threshold:
                cluster["items"].append(item)
                cluster["token_union"] = cluster["token_union"] | tokens
                placed = True
                break
        if not placed:
            clusters.append({"items": [item], "token_union": tokens})

    out = []
    for raw in clusters:
        c_items = raw["items"]
        representative = _cluster_representative_item(c_items)
        problem_fps = sorted(
            set(
                str(item.get("canonical_problem_fingerprint") or item.get("problem_fingerprint") or "")
                for item in c_items
                if str(item.get("problem_statement") or "").strip()
            )
        )
        cluster_id = _stable_cluster_id_from_items(c_items)
        if existing_clusters:
            reused_cluster_id = _reuse_existing_cluster_id(
                {
                    "normalized_problem": representative.get("problem_statement", ""),
                    "persona_candidate": representative.get("persona", ""),
                    "module_candidate": representative.get("module", ""),
                },
                existing_clusters,
            )
            if reused_cluster_id:
                cluster_id = reused_cluster_id

        source_identities = sorted(set(str(x.get("source_identity") or x.get("source_name") or "unknown") for x in c_items))
        source_families = sorted(set(str(x.get("source_family") or "unknown") for x in c_items))
        evidence = [
            _truncate_text(x.get("excerpt") or _safe_excerpt(str(x.get("problem_statement") or "")), MAX_CACHED_EXCERPT_LENGTH)
            for x in c_items
        ]
        urls = sorted(set(str(x.get("canonical_url") or "") for x in c_items if x.get("canonical_url")))

        persona_counts: dict[str, int] = {}
        module_counts: dict[str, int] = {}
        for it in c_items:
            persona = str(it.get("persona", "unknown"))
            module = str(it.get("module", "unknown"))
            persona_counts[persona] = persona_counts.get(persona, 0) + 1
            module_counts[module] = module_counts.get(module, 0) + 1

        persona = sorted(persona_counts.items(), key=lambda kv: (-kv[1], kv[0]))[0][0]
        module = sorted(module_counts.items(), key=lambda kv: (-kv[1], kv[0]))[0][0]
        normalized_problem = _truncate_text(str(representative.get("problem_statement") or ""), MAX_CACHED_PROBLEM_LENGTH)
        cluster_problem_key = _cluster_canonical_problem_key(c_items) or canonical_problem_key(normalized_problem)

        out.append(
            {
                "cluster_id": cluster_id,
                "normalized_problem": normalized_problem,
                "canonical_problem_key": cluster_problem_key,
                "persona_candidate": persona,
                "module_candidate": module,
                "evidence_items": evidence,
                "source_urls": urls,
                "source_families": source_families,
                "source_identities": source_identities,
                "source_count": len(c_items),
                "independent_source_family_count": len(source_identities),
                "fingerprints": problem_fps,
                "raw_items": c_items,
            }
        )

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
        requirements.append(
            {
                "id": rec.get("id", path.stem),
                "text": str(rec.get("problem", "")),
                "status": str(rec.get("status", "")),
                "path": str(path.relative_to(ROOT)),
            }
        )

    for path in iter_record_files(DECISIONS_DIR) or []:
        try:
            rec = load_record(path)
        except Exception:
            continue
        decisions.append(
            {
                "id": rec.get("id", path.stem),
                "subject": str(rec.get("subject", "")),
                "decision": str(rec.get("decision", "")),
                "status": str(rec.get("status", "")),
                "path": str(path.relative_to(ROOT)),
            }
        )

    for path in iter_record_files(OBSERVATIONS_DIR) or []:
        try:
            rec = load_record(path)
        except Exception:
            continue
        observations.append(
            {
                "id": rec.get("id", path.stem),
                "text": str(rec.get("problem_statement", "")),
                "path": str(path.relative_to(ROOT)),
                "cluster_id": rec.get("cluster_id"),
                "problem_fingerprint": rec.get("problem_fingerprint"),
            }
        )

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
    problem = str(cluster.get("normalized_problem") or "")
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
        score = _text_similarity(problem, f"{dec['subject']} {dec['decision']}")
        if score > best_dec[0]:
            best_dec = (score, dec)

    linked = []
    suppress_reproposal = False
    suppress_reason = ""

    if best_req[1] and best_req[0] >= 0.68:
        linked.append({"type": "requirement", "id": best_req[1]["id"], "score": round(best_req[0], 3), "path": best_req[1]["path"]})

    if best_cap[1] and best_cap[0] >= 0.68:
        linked.append({"type": "validated_capability", "id": best_cap[1], "score": round(best_cap[0], 3)})

    if best_dec[1] and best_dec[0] >= 0.68:
        linked.append(
            {
                "type": "decision",
                "id": best_dec[1]["id"],
                "status": best_dec[1]["status"],
                "score": round(best_dec[0], 3),
                "path": best_dec[1]["path"],
            }
        )
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


def _foundation_match_summary(match: dict) -> str:
    linked = match.get("linked") or []
    if not linked:
        return "Brak silnego dopasowania do Product Foundation"
    parts = []
    for row in linked:
        rtype = row.get("type")
        rid = row.get("id")
        score = row.get("score")
        if rtype == "decision" and row.get("status"):
            parts.append(f"decision:{rid}({row.get('status')}, score={score})")
        else:
            parts.append(f"{rtype}:{rid}(score={score})")
    return "; ".join(parts)


def build_ai_prompt(run_id: str, clusters: list[dict], model: str) -> str:
    compact = []
    for c in clusters:
        compact.append(
            {
                "cluster_id": c["cluster_id"],
                "normalized_problem": c["normalized_problem"],
                "persona_candidate": c["persona_candidate"],
                "module_candidate": c["module_candidate"],
                "independent_source_family_count": c["independent_source_family_count"],
                "source_families": c["source_families"],
                "source_identities": c.get("source_identities", []),
                "evidence_items": c["evidence_items"][:6],
                "source_urls": c["source_urls"][:12],
                "foundation_match": c.get("foundation_match", {}),
            }
        )

    payload = {
        "task": "Classify LibreCare discovery clusters. Advisory only.",
        "run_id": run_id,
        "model": model,
        "required_output": {
            "type": "object",
            "required": ["clusters"],
            "clusters_item_required": [
                "cluster_id",
                "classification",
                "persona",
                "problem_statement",
                "evidence_summary",
                "source_diversity_summary",
                "current_librecare_match",
                "solvability",
                "impact_score",
                "frequency_score",
                "evidence_score",
                "solvability_score",
                "novelty_score",
                "effort_score",
                "confidence",
                "counterargument",
                "candidate_recommendation",
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
    return json.dumps(payload, ensure_ascii=False, indent=2)


def run_copilot_json(prompt: str, model: str) -> str:
    if model != "gpt-5.4-mini":
        raise RuntimeError(f"Discovery requires model gpt-5.4-mini, got: {model}")

    cmd = [
        "copilot",
        "-s",
        "--no-ask-user",
        "--disable-builtin-mcps",
        "--available-tools=view,grep,glob",
        "--allow-tool=read",
        "--deny-tool=write",
        "--deny-tool=shell",
        "--model=gpt-5.4-mini",
        "-p",
        prompt,
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True, check=False)
    if proc.returncode != 0:
        stderr = (proc.stderr or "").strip()
        if "model" in stderr.lower() and "gpt-5.4-mini" in stderr.lower():
            raise RuntimeError("Copilot CLI failed: gpt-5.4-mini is unavailable in this environment")
        raise RuntimeError(f"Copilot CLI failed: {stderr[:400]}")
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
        "cluster_id",
        "classification",
        "persona",
        "problem_statement",
        "evidence_summary",
        "source_diversity_summary",
        "current_librecare_match",
        "solvability",
        "impact_score",
        "frequency_score",
        "evidence_score",
        "solvability_score",
        "novelty_score",
        "effort_score",
        "confidence",
        "counterargument",
        "candidate_recommendation",
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
        for score_key in [
            "impact_score",
            "frequency_score",
            "evidence_score",
            "solvability_score",
            "novelty_score",
            "effort_score",
        ]:
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
    source_diversity = min(int(cluster.get("independent_source_family_count", 0)), 3)

    strategic_fit = 5 if cluster.get("persona_candidate") == "caregiver" else 3
    raw = (
        impact * 9
        + freq * 8
        + evidence * 8
        + solvability * 9
        + novelty * 7
        + source_diversity * 4
        + strategic_fit * 3
        - effort * 5
    )
    return max(0, min(100, int(raw)))


def next_observation_name(existing_names: set[str], date_stamp: str) -> tuple[str, str]:
    idx = 1
    while True:
        obs_id = f"OBS-{date_stamp}-{idx:02d}"
        filename = f"{obs_id}.json"
        if filename not in existing_names:
            return obs_id, filename
        idx += 1


def load_existing_observation_fingerprints() -> tuple[set[str], set[str]]:
    fps = set()
    clusters = set()
    for path in iter_record_files(OBSERVATIONS_DIR) or []:
        try:
            rec = load_record(path)
        except Exception:
            continue
        fp = rec.get("problem_fingerprint")
        cid = rec.get("cluster_id")
        if isinstance(fp, str) and fp:
            fps.add(fp)
        if isinstance(cid, str) and cid:
            clusters.add(cid)
    return fps, clusters


def create_observations_from_clusters(clusters: list[dict], run_id: str) -> list[str]:
    created = []
    OBSERVATIONS_DIR.mkdir(parents=True, exist_ok=True)
    existing_files = {p.name for p in OBSERVATIONS_DIR.glob("OBS-*.json")}
    existing_fp, existing_cluster_ids = load_existing_observation_fingerprints()
    date_stamp = datetime.now(timezone.utc).strftime("%Y%m%d")

    for cluster in clusters:
        if cluster.get("identity_ambiguous"):
            continue
        match = cluster.get("foundation_match", {})
        if match.get("best_capability_score", 0.0) >= 0.80:
            continue

        cid = cluster["cluster_id"]
        fp = cluster["fingerprints"][0] if cluster.get("fingerprints") else ""
        if cid in existing_cluster_ids or (fp and fp in existing_fp):
            continue

        first = cluster["raw_items"][0]
        obs_id, filename = next_observation_name(existing_files, date_stamp)
        payload = {
            "id": obs_id,
            "created_at": utc_now(),
            "source_type": first.get("source_type", "community"),
            "source_reference": cluster["source_urls"][0] if cluster.get("source_urls") else "",
            "persona": cluster.get("persona_candidate", "unknown"),
            "mode": first.get("mode", cluster.get("persona_candidate", "unknown")),
            "module": cluster.get("module_candidate", "unknown"),
            "type": first.get("type", "usability"),
            "severity": first.get("severity", "medium"),
            "frequency": first.get("frequency", "unknown"),
            "confidence": first.get("confidence", "medium"),
            "evidence": cluster.get("evidence_items", [])[:3] or [cluster.get("normalized_problem", "")],
            "problem_statement": cluster.get("normalized_problem", ""),
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
        created.append(str(path.relative_to(ROOT)))

    return created


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
    eligible_rows = [row for row in top_ranked if row["governed"].get("eligible_for_inbox")]
    for entry in eligible_rows[:3]:
        marker = f"<!-- LIBRECARE_DISCOVERY_CLUSTER: {entry['cluster']['cluster_id']} -->"
        existing = client.find_issue_by_marker(marker)
        if existing:
            created.append(
                {
                    "cluster_id": entry["cluster"]["cluster_id"],
                    "action": "SKIPPED_EXISTS",
                    "issue_number": existing.get("number"),
                    "issue_url": existing.get("html_url"),
                }
            )
            continue

        issue = client.create_issue(
            title=f"[Skrzynka Produktowa] Discovery {entry['cluster']['cluster_id']}",
            body=build_inbox_issue_body(entry, marker),
            labels=["product-inbox"],
        )
        created.append(
            {
                "cluster_id": entry["cluster"]["cluster_id"],
                "action": "CREATED",
                "issue_number": issue.get("number"),
                "issue_url": issue.get("html_url"),
            }
        )
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
    cache_max_age_seconds: int,
) -> dict:
    started_at = utc_now()
    run_stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M")
    run_id = f"DISCOVERY-{run_stamp}"

    status = "SUCCESS"
    errors: list[str] = []
    ai_calls = 0

    cache = DiscoveryCache(DISCOVERY_CACHE_PATH)

    sources_cfg = read_json(sources_file)
    sources = sources_cfg.get("sources", [])

    source_results = []
    all_items = []
    for source in sources:
        result = collect_from_source(
            source,
            max_items=max_items_per_source,
            timeout=timeout,
            retries=retries,
            cache=cache,
            cache_max_age_seconds=cache_max_age_seconds,
        )
        source_results.append(
            {
                "name": result.name,
                "family": result.family,
                "status": result.status,
                "detail": result.detail,
                "count": len(result.items),
            }
        )
        all_items.extend(result.items)
        if result.status.endswith("DEGRADED") or result.status == "REDDIT_DISABLED":
            if status == "SUCCESS":
                status = "DEGRADED"

    deduped_items, dedupe_stats = dedupe_items(all_items)
    # Build logical clusters without identity reuse first; stateful registry assigns final stable IDs.
    clusters = cluster_items(deduped_items)
    observation_clusters = _existing_cluster_matches()

    cluster_registry = None
    registry_summary = {
        "existing_entries": 0,
        "matched_existing": 0,
        "new_entries": 0,
        "ambiguous_matches": [],
        "changed": False,
    }
    proposed_registry = {"version": CLUSTER_REGISTRY_VERSION, "entries": []}

    try:
        cluster_registry = load_cluster_registry(DISCOVERY_CLUSTER_REGISTRY_PATH)
    except RuntimeError as exc:
        status = "FAILED"
        errors.append(f"Registry load failed (fail-closed): {exc}")
        cluster_registry = None

    if cluster_registry is not None:
        clusters, registry_summary, proposed_registry = resolve_cluster_ids_with_registry(
            clusters,
            cluster_registry,
            now_iso=utc_now(),
            observation_clusters=observation_clusters,
        )
    else:
        # Registry is unavailable; prevent inbox publication
        for cluster in clusters:
            cluster["identity_ambiguous"] = True
            cluster["identity_ambiguous_candidates"] = [
                {"cluster_id": cluster["cluster_id"], "score": 0.0},
            ]

    foundation = load_foundation_index()
    for c in clusters:
        c["foundation_match"] = match_cluster_to_foundation(c, foundation)

    created_observations = []
    if status != "FAILED":
        created_observations = create_observations_from_clusters(clusters, run_id)

    if ai_mode == "copilot" and ai_model != "gpt-5.4-mini":
        status = "FAILED"
        errors.append("Copilot mode requires exact model gpt-5.4-mini")

    if ai_response_file:
        raw = ai_response_file.read_text(encoding="utf-8")
        ai_calls = 1
    elif ai_mode == "copilot" and clusters and status != "FAILED":
        prompt = build_ai_prompt(run_id, clusters, model=ai_model)
        try:
            raw = run_copilot_json(prompt, model=ai_model)
        except Exception as exc:  # noqa: BLE001
            status = "FAILED"
            errors.append(str(exc))
            raw = "{}"
        ai_calls = 1
    else:
        payload = {"clusters": []}
        for c in clusters:
            cls = "PRODUCT_PROBLEM"
            if c["foundation_match"].get("best_capability_score", 0) >= 0.68:
                cls = "VALIDATED_CAPABILITY"
            payload["clusters"].append(
                {
                    "cluster_id": c["cluster_id"],
                    "classification": cls,
                    "persona": c["persona_candidate"],
                    "problem_statement": c["normalized_problem"],
                    "evidence_summary": _safe_excerpt("; ".join(c["evidence_items"]), 160),
                    "source_diversity_summary": f"{c['source_count']} items / {c['independent_source_family_count']} independent sources",
                    "current_librecare_match": _foundation_match_summary(c.get("foundation_match", {})),
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
                }
            )
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
                if cluster.get("identity_ambiguous"):
                    governed["eligible_for_inbox"] = False
                    governed["exclusion_reason"] = "Cluster identity ambiguous; requires human registry review"
                score = compute_score(cluster, governed)
                governed_rows.append({"cluster": cluster, "governed": governed, "score": score})

    if ai_calls > 2:
        status = "FAILED"
        errors.append(f"AI call budget exceeded: {ai_calls} > 2")

    governed_rows.sort(key=lambda r: (-r["score"], r["cluster"]["cluster_id"]))
    top10 = governed_rows[:10]

    created_issues = []
    if status != "FAILED" and publish_top3_flag and top10:
        if not (repo_owner and repo_name and github_token):
            status = "DEGRADED"
            errors.append("publish_top3 requested but GitHub credentials/repo not provided")
        else:
            created_issues = publish_top3(top10, repo_owner=repo_owner, repo_name=repo_name, github_token=github_token)

    finished_at = utc_now()

    report_top10 = []
    for idx, row in enumerate(top10):
        deterministic_match = row["cluster"].get("foundation_match", {})
        report_top10.append(
            {
                "rank": idx + 1,
                "score": row["score"],
                "cluster_id": row["cluster"]["cluster_id"],
                "problem": row["governed"].get("problem_statement"),
                "persona": row["governed"].get("persona"),
                "classification": row["governed"].get("classification"),
                "evidence_count": len(row["cluster"].get("evidence_items", [])),
                "source_families": row["cluster"].get("source_families", []),
                "source_identities": row["cluster"].get("source_identities", []),
                "current_librecare_match": _foundation_match_summary(deterministic_match),
                "foundation_match_links": deterministic_match.get("linked", []),
                "ai_current_librecare_match": row["governed"].get("current_librecare_match"),
                "solvability": row["governed"].get("solvability"),
                "effort": row["governed"].get("effort_score"),
                "confidence": row["governed"].get("confidence"),
                "counterargument": row["governed"].get("counterargument"),
                "source_urls": row["cluster"].get("source_urls", []),
                "eligibility": bool(row["governed"].get("eligible_for_inbox")),
                "exclusion_reason": row["governed"].get("exclusion_reason", ""),
                "suppressed_reason": row["governed"].get("suppressed_reason", ""),
            }
        )

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
        "top10": report_top10,
        "created_observations": created_observations,
        "top3_issue_actions": created_issues,
        "errors": errors,
        "cluster_registry": {
            "existing_entries": registry_summary.get("existing_entries", 0),
            "matched_existing": registry_summary.get("matched_existing", 0),
            "new_entries": registry_summary.get("new_entries", 0),
            "ambiguous_matches": registry_summary.get("ambiguous_matches", []),
            "proposed_registry_path": "",
        },
        "safety_guards": {
            "max_ai_calls": 2,
            "ai_calls_used": ai_calls,
            "no_auto_acceptance": True,
            "no_auto_implementation": True,
            "no_auto_merge": True,
        },
    }

    GENERATED_DISCOVERY_DIR.mkdir(parents=True, exist_ok=True)
    proposed_registry_path = DISCOVERY_PROPOSED_CLUSTER_REGISTRY_PATH
    if registry_summary.get("changed"):
        write_json(proposed_registry_path, proposed_registry)
        report["cluster_registry"]["proposed_registry_path"] = str(proposed_registry_path.relative_to(ROOT))

    json_path = GENERATED_DISCOVERY_DIR / f"DISCOVERY-{run_stamp}.json"
    md_path = GENERATED_DISCOVERY_DIR / f"DISCOVERY-{run_stamp}.md"
    write_json(json_path, report)
    md_path.write_text(render_markdown_report(report), encoding="utf-8")

    try:
        cache.save()
    except Exception as exc:  # noqa: BLE001
        report["status"] = "DEGRADED" if report["status"] == "SUCCESS" else report["status"]
        report["errors"].append(f"Cache save degraded: {exc}")

    report["json_report_path"] = str(json_path.relative_to(ROOT))
    report["md_report_path"] = str(md_path.relative_to(ROOT))
    return report


def _report_section_for_item(item: dict) -> str:
    if item.get("eligibility"):
        return "TOP CANDIDATES"
    if item.get("classification") == "TEST_COVERAGE_GAP":
        return "TEST / RESEARCH GAPS"
    if item.get("classification") == "INCONCLUSIVE":
        return "INSUFFICIENT EVIDENCE"
    if item.get("solvability") in {"ABBOTT_LIMITATION", "EXTERNAL_ONLY"}:
        return "ABBOTT LIMITATION / NOT LIBRECARE-SOLVABLE"
    if item.get("classification") == "VALIDATED_CAPABILITY" or item.get("suppressed_reason") or (
        "existing requirement" in str(item.get("exclusion_reason", "")).lower()
    ):
        return "ALREADY COVERED"
    return "WORTH REVIEWING"


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

    reg = report.get("cluster_registry") or {}
    lines.extend(
        [
            "",
            "## Cluster Registry",
            "",
            f"- Existing entries: {reg.get('existing_entries', 0)}",
            f"- Matched existing: {reg.get('matched_existing', 0)}",
            f"- New entries: {reg.get('new_entries', 0)}",
            f"- Ambiguous matches: {len(reg.get('ambiguous_matches', []))}",
        ]
    )
    if reg.get("proposed_registry_path"):
        lines.append(f"- Proposed registry artifact: {reg.get('proposed_registry_path')}")

    sections = [
        "TOP CANDIDATES",
        "WORTH REVIEWING",
        "ALREADY COVERED",
        "ABBOTT LIMITATION / NOT LIBRECARE-SOLVABLE",
        "TEST / RESEARCH GAPS",
        "INSUFFICIENT EVIDENCE",
    ]

    bucket: dict[str, list[dict]] = {name: [] for name in sections}
    for item in report.get("top10", []):
        bucket[_report_section_for_item(item)].append(item)

    for section in sections:
        lines.extend(["", f"## {section}", ""])
        rows = bucket[section]
        if not rows:
            lines.append("Brak pozycji.")
            lines.append("")
            continue
        for item in rows:
            lines.append(f"TOP {item['rank']} — {item['score']}/100")
            lines.append("")
            lines.append("Problem:")
            lines.append(str(item.get("problem", "")))
            lines.append("")
            lines.append("Persona:")
            lines.append(str(item.get("persona", "")))
            lines.append("")
            lines.append("Classification:")
            lines.append(str(item.get("classification", "")))
            lines.append("")
            lines.append("Evidence:")
            lines.append(
                f"{item.get('evidence_count', 0)} items / {len(item.get('source_identities', []))} independent sources"
            )
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
    parser.add_argument("--cache-max-age-seconds", type=int, default=21600)
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
        cache_max_age_seconds=max(1, int(args.cache_max_age_seconds)),
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
