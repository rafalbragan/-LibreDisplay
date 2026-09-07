import importlib.util
import json
import shutil
import tempfile
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path

import pytest


WORKSPACE_ROOT = Path(__file__).resolve().parents[3]
CLI_PATH = WORKSPACE_ROOT / "scripts" / "product" / "discovery_cli.py"
FIXTURE_SOURCES = WORKSPACE_ROOT / "scripts" / "product" / "tests" / "fixtures" / "discovery" / "sources-fixture.json"


def load_cli_module():
    spec = importlib.util.spec_from_file_location("discovery_cli_under_test", CLI_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    spec.loader.exec_module(module)
    return module


@pytest.fixture()
def cli_env(monkeypatch):
    cli = load_cli_module()
    temp_dir = tempfile.TemporaryDirectory()
    root = Path(temp_dir.name)
    shutil.copytree(WORKSPACE_ROOT / "product", root / "product")

    monkeypatch.setattr(cli, "ROOT", root)
    monkeypatch.setattr(cli, "PRODUCT", root / "product")
    monkeypatch.setattr(cli, "SCHEMA_DIR", root / "product" / "schema")
    monkeypatch.setattr(cli, "OBSERVATIONS_DIR", root / "product" / "research" / "observations")
    monkeypatch.setattr(cli, "REQUIREMENTS_DIR", root / "product" / "requirements")
    monkeypatch.setattr(cli, "DECISIONS_DIR", root / "product" / "decisions")
    monkeypatch.setattr(cli, "GENERATED_DISCOVERY_DIR", root / "product" / "generated" / "discovery")
    monkeypatch.setattr(cli, "VALIDATED_CAPABILITIES_MD", root / "product" / "generated" / "VALIDATED_CAPABILITIES.md")
    monkeypatch.setattr(cli, "SOURCES_CONFIG_DEFAULT", root / "product" / "discovery" / "sources.json")
    monkeypatch.setattr(cli, "DISCOVERY_CACHE_PATH", root / "product" / "generated" / "discovery" / "cache" / "discovery-cache-v1.json")
    monkeypatch.setattr(cli, "DISCOVERY_CLUSTER_REGISTRY_PATH", root / "product" / "discovery" / "cluster-registry.json")
    monkeypatch.setattr(cli, "DISCOVERY_PROPOSED_CLUSTER_REGISTRY_PATH", root / "product" / "generated" / "discovery" / "cluster-registry.proposed.json")

    monkeypatch.delenv("REDDIT_CLIENT_ID", raising=False)
    monkeypatch.delenv("REDDIT_CLIENT_SECRET", raising=False)

    yield cli, root
    temp_dir.cleanup()


@pytest.fixture()
def fixture_sources(tmp_path):
    out = tmp_path / "sources.json"
    out.write_text(FIXTURE_SOURCES.read_text(encoding="utf-8"), encoding="utf-8")
    return out


def run_discovery_with_ai_file(cli, sources_file, ai_file, *, publish=False, repo_owner="", repo_name="", github_token=""):
    return cli.run_discovery(
        sources_file=sources_file,
        max_items_per_source=20,
        publish_top3_flag=publish,
        repo_owner=repo_owner,
        repo_name=repo_name,
        github_token=github_token,
        ai_mode="heuristic",
        ai_model="gpt-5.4-mini",
        ai_response_file=ai_file,
        timeout=2.0,
        retries=1,
        cache_max_age_seconds=3600,
    )


def build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM", solvability="APP"):
    payload = {"clusters": []}
    for c in clusters:
        payload["clusters"].append(
            {
                "cluster_id": c["cluster_id"],
                "classification": classification,
                "persona": c["persona_candidate"],
                "problem_statement": c["normalized_problem"],
                "problem_statement_pl": "Zwięzły opis problemu po polsku",
                "evidence_summary": "Evidence summary",
                "source_diversity_summary": "Diverse sources",
                "current_librecare_match": "AI summary",
                "solvability": solvability,
                "impact_score": 4,
                "frequency_score": 4,
                "evidence_score": 4,
                "solvability_score": 4,
                "novelty_score": 3,
                "effort_score": 2,
                "confidence": "high",
                "counterargument": "Counter point",
                "candidate_recommendation": "Recommend human review",
            }
        )
    return payload


def _prepare_clusters(cli, sources_file):
    cfg = json.loads(Path(sources_file).read_text(encoding="utf-8"))
    collected = []
    cache = cli.DiscoveryCache(cli.DISCOVERY_CACHE_PATH)
    for source in cfg["sources"]:
        result = cli.collect_from_source(source, max_items=20, timeout=2.0, retries=1, cache=cache, cache_max_age_seconds=3600)
        collected.extend(result.items)
    deduped, _ = cli.dedupe_items(collected)
    clusters = cli.cluster_items(deduped)
    observations = cli._existing_cluster_matches()
    registry = cli.load_cluster_registry(cli.DISCOVERY_CLUSTER_REGISTRY_PATH)
    clusters, _, _ = cli.resolve_cluster_ids_with_registry(clusters, registry, now_iso=cli.utc_now(), observation_clusters=observations)
    return clusters


def test_canonical_url_normalization(cli_env):
    cli, _ = cli_env
    url = "HTTPS://Example.COM/a/b/?utm_source=x&b=2&a=1#frag"
    assert cli.canonicalize_url(url) == "https://example.com/a/b?a=1&b=2"


def test_exact_duplicate_detection(cli_env):
    cli, _ = cli_env
    item = {
        "canonical_url": "https://a",
        "content_hash": cli.content_hash("same"),
        "problem_statement": "same problem",
        "source_name": "nightscout_github",
        "source_identity": "nightscout_github",
    }
    deduped, stats = cli.dedupe_items([item, dict(item)])
    assert len(deduped) == 1
    assert stats["exact_duplicates"] == 1


def test_normalized_text_duplicate_detection_same_source(cli_env):
    cli, _ = cli_env
    items = [
        {
            "canonical_url": "https://a/1",
            "content_hash": cli.content_hash("1"),
            "problem_statement": "Caregiver cannot read stale data quickly",
            "source_name": "nightscout_github",
            "source_identity": "nightscout_github",
        },
        {
            "canonical_url": "https://a/2",
            "content_hash": cli.content_hash("2"),
            "problem_statement": "caregiver cannot read stale data quickly!!!",
            "source_name": "nightscout_github",
            "source_identity": "nightscout_github",
        },
    ]
    deduped, stats = cli.dedupe_items(items)
    assert len(deduped) == 1
    assert stats["normalized_duplicates"] == 1


def test_cross_source_duplicate_preserves_diversity(cli_env):
    cli, _ = cli_env
    items = [
        {
            "canonical_url": "https://a/1",
            "content_hash": cli.content_hash("1"),
            "problem_statement": "Caregiver cannot read stale data quickly",
            "source_name": "nightscout_github",
            "source_identity": "nightscout_github",
            "source_family": "github_community",
            "persona": "caregiver",
            "module": "Home / Monitoring",
            "source_type": "community",
            "type": "usability",
            "severity": "medium",
            "frequency": "frequent",
            "confidence": "medium",
        },
        {
            "canonical_url": "https://a/2",
            "content_hash": cli.content_hash("2"),
            "problem_statement": "caregiver cannot read stale data quickly!!!",
            "source_name": "dexcom_official",
            "source_identity": "dexcom_official",
            "source_family": "competitor",
            "persona": "caregiver",
            "module": "Home / Monitoring",
            "source_type": "competitor",
            "type": "idea",
            "severity": "medium",
            "frequency": "frequent",
            "confidence": "medium",
        },
    ]
    deduped, _ = cli.dedupe_items(items)
    clusters = cli.cluster_items(deduped)
    assert len(clusters) == 1
    assert clusters[0]["independent_source_family_count"] == 2
    assert set(clusters[0]["source_identities"]) == {"nightscout_github", "dexcom_official"}


def test_incremental_cluster_id_stable_with_added_near_duplicate(cli_env):
    cli, _ = cli_env
    base_items = [
        {
            "canonical_url": "https://a/1",
            "content_hash": cli.content_hash("1"),
            "problem_statement": "Caregiver cannot quickly detect stale readings",
            "source_name": "nightscout_github",
            "source_identity": "nightscout_github",
            "source_family": "github_community",
            "persona": "caregiver",
            "module": "Home / Monitoring",
            "source_type": "community",
            "type": "usability",
            "severity": "medium",
            "frequency": "frequent",
            "confidence": "medium",
        },
        {
            "canonical_url": "https://a/2",
            "content_hash": cli.content_hash("2"),
            "problem_statement": "Caregiver struggles to detect stale readings quickly",
            "source_name": "xdrip_github",
            "source_identity": "xdrip_github",
            "source_family": "github_community",
            "persona": "caregiver",
            "module": "Home / Monitoring",
            "source_type": "community",
            "type": "usability",
            "severity": "medium",
            "frequency": "frequent",
            "confidence": "medium",
        },
    ]
    d1, _ = cli.dedupe_items(base_items)
    c1 = cli.cluster_items(d1)

    extended = base_items + [
        {
            "canonical_url": "https://a/3",
            "content_hash": cli.content_hash("3"),
                "problem_statement": "Caregiver has trouble detecting stale readings quickly",
            "source_name": "reddit_diabetes",
            "source_identity": "reddit_diabetes",
            "source_family": "reddit",
            "persona": "caregiver",
            "module": "Home / Monitoring",
            "source_type": "community",
            "type": "usability",
            "severity": "medium",
            "frequency": "frequent",
            "confidence": "low",
        }
    ]
    d2, _ = cli.dedupe_items(extended)
    c2 = cli.cluster_items(d2)

    assert len(c1) == 1
    assert len(c2) == 1
    assert c1[0]["cluster_id"] == c2[0]["cluster_id"]


def test_incremental_rerun_no_duplicate_observations_and_markers(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")

    r1 = run_discovery_with_ai_file(cli, fixture_sources, ai_file)
    r2 = run_discovery_with_ai_file(cli, fixture_sources, ai_file)

    assert r1["status"] in {"SUCCESS", "DEGRADED"}
    assert r2["status"] in {"SUCCESS", "DEGRADED"}

    obs_dir = root / "product" / "research" / "observations"
    obs_files = list(obs_dir.glob("OBS-*.json"))
    names = [p.name for p in obs_files]
    assert len(names) == len(set(names))


def test_incremental_rerun_no_duplicate_product_inbox_issue(cli_env, fixture_sources, monkeypatch):
    cli, root = cli_env

    class FakeClient:
        issues = []

        def __init__(self, owner, repo, token):
            self.owner = owner
            self.repo = repo
            self.token = token

        def find_issue_by_marker(self, marker):
            for issue in self.__class__.issues:
                if marker in issue["body"]:
                    return issue
            return None

        def create_issue(self, title, body, labels):
            issue = {
                "number": len(self.__class__.issues) + 1,
                "html_url": f"https://example/{len(self.__class__.issues)+1}",
                "title": title,
                "body": body,
                "labels": labels,
            }
            self.__class__.issues.append(issue)
            return issue

    monkeypatch.setattr(cli, "GITHUB_CLIENT_FACTORY", FakeClient)

    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")

    run_discovery_with_ai_file(cli, fixture_sources, ai_file, publish=True, repo_owner="o", repo_name="r", github_token="t")
    first_count = len(FakeClient.issues)
    run_discovery_with_ai_file(cli, fixture_sources, ai_file, publish=True, repo_owner="o", repo_name="r", github_token="t")
    second_count = len(FakeClient.issues)
    assert first_count == second_count


def test_existing_validated_capability_is_not_issue_candidate(cli_env, fixture_sources, monkeypatch):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")
    original_match = cli.match_cluster_to_foundation

    def forced_match(cluster, foundation):
        data = original_match(cluster, foundation)
        data["best_capability_score"] = 0.9
        return data

    monkeypatch.setattr(cli, "match_cluster_to_foundation", forced_match)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")
    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file)
    assert report["top10"] == []


def test_test_coverage_gap_report_only(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters, classification="TEST_COVERAGE_GAP")), encoding="utf-8")
    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file)
    assert all(not t["eligibility"] for t in report["top10"])


def test_safety_gap_candidate_never_auto_accept(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters, classification="SAFETY_GAP")), encoding="utf-8")
    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file)
    assert any(t["classification"] == "SAFETY_GAP" for t in report["top10"])
    assert report["safety_guards"]["no_auto_acceptance"] is True
    assert report["safety_guards"]["no_auto_implementation"] is True


def test_inconclusive_report_only(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters, classification="INCONCLUSIVE")), encoding="utf-8")
    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file)
    assert all(not t["eligibility"] for t in report["top10"])


def test_product_problem_and_opportunity_eligible(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)

    ai_problem = root / "ai-problem.json"
    ai_problem.write_text(json.dumps(build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")), encoding="utf-8")
    r_problem = run_discovery_with_ai_file(cli, fixture_sources, ai_problem)
    assert any(t["classification"] == "PRODUCT_PROBLEM" and t["eligibility"] for t in r_problem["top10"])

    ai_opp = root / "ai-opp.json"
    ai_opp.write_text(json.dumps(build_ai_payload_for_clusters(clusters, classification="PRODUCT_OPPORTUNITY")), encoding="utf-8")
    r_opp = run_discovery_with_ai_file(cli, fixture_sources, ai_opp)
    assert any(t["classification"] == "PRODUCT_OPPORTUNITY" and t["eligibility"] for t in r_opp["top10"])


def test_source_failure_degraded_no_fabricated_evidence(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    sources = {
        "sources": [
            {
                "name": "abbott_official",
                "family": "official_vendor",
                "kind": "official_pages",
                "enabled": True,
                "urls": ["https://example.invalid"],
            }
        ]
    }
    path = tmp_path / "sources.json"
    path.write_text(json.dumps(sources), encoding="utf-8")

    def fail_fetch(_url, timeout, retries):
        raise RuntimeError("network blocked")

    monkeypatch.setattr(cli, "_fetch_url", fail_fetch)
    report = cli.run_discovery(path, 5, False, "", "", "", "heuristic", "gpt-5.4-mini", None, 1.0, 1, 3600)
    assert report["status"] == "DEGRADED"
    assert report["counts"]["COLLECTED"] == 0


def test_reddit_without_credentials_disabled_and_continues(cli_env, tmp_path):
    cli, _ = cli_env
    sources = {"sources": [{"name": "reddit_diabetes", "family": "reddit", "kind": "reddit_oauth", "enabled": True}]}
    path = tmp_path / "sources.json"
    path.write_text(json.dumps(sources), encoding="utf-8")
    report = cli.run_discovery(path, 5, False, "", "", "", "heuristic", "gpt-5.4-mini", None, 1.0, 1, 3600)
    assert report["status"] == "DEGRADED"
    assert any(s["status"] == "REDDIT_DISABLED" for s in report["source_status"])


def test_reddit_oauth_with_credentials_collects_items(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    monkeypatch.setenv("REDDIT_CLIENT_ID", "id")
    monkeypatch.setenv("REDDIT_CLIENT_SECRET", "sec")

    def fake_post_form_json(url, form, timeout, retries, headers=None):
        assert "access_token" in url
        return {"access_token": "token123"}

    def fake_fetch_json(url, timeout, retries, headers=None):
        assert headers and headers.get("Authorization", "").startswith("Bearer ")
        return {
            "data": {
                "children": [
                    {
                        "data": {
                            "title": "Stale readings are hard to interpret",
                            "selftext": "Need clearer state for caregivers",
                            "permalink": "/r/diabetes/comments/abc123/stale/",
                            "created_utc": time.time(),
                            "author": "should_not_be_saved",
                        }
                    }
                ]
            }
        }

    monkeypatch.setattr(cli, "_post_form_json", fake_post_form_json)
    monkeypatch.setattr(cli, "_fetch_json", fake_fetch_json)

    sources = {
        "sources": [
            {
                "name": "reddit_diabetes",
                "family": "reddit",
                "kind": "reddit_oauth",
                "enabled": True,
                "subreddits": ["diabetes"],
            }
        ]
    }
    path = tmp_path / "sources.json"
    path.write_text(json.dumps(sources), encoding="utf-8")
    report = cli.run_discovery(path, 5, False, "", "", "", "heuristic", "gpt-5.4-mini", None, 1.0, 1, 3600)
    src = report["source_status"][0]
    assert src["status"] == "OK"
    assert src["count"] > 0


def _collect_cache_text(root: Path) -> str:
    cache_path = root / "product" / "generated" / "discovery" / "cache" / "discovery-cache-v1.json"
    return cache_path.read_text(encoding="utf-8")


def _assert_cache_minimized(cache_payload):
    forbidden_keys = {
        "access_token",
        "refresh_token",
        "authorization",
        "client_secret",
        "reddit_client_secret",
        "github_token",
        "password",
        "cookie",
        "set-cookie",
        "author",
        "username",
        "user",
        "body",
        "selftext",
    }
    if isinstance(cache_payload, dict):
        for key, value in cache_payload.items():
            assert str(key).lower() not in forbidden_keys
            _assert_cache_minimized(value)
    elif isinstance(cache_payload, list):
        for item in cache_payload:
            _assert_cache_minimized(item)


def test_cache_sanitizes_raw_upstream_payloads_and_bounded_observations(cli_env, tmp_path, monkeypatch):
    cli, root = cli_env
    monkeypatch.setenv("REDDIT_CLIENT_ID", "id")
    monkeypatch.setenv("REDDIT_CLIENT_SECRET", "sec")

    long_github_body = "GITHUB BODY SECRET " + ("alpha beta gamma delta epsilon " * 30)
    long_reddit_body = "REDDIT SELFTEXT SECRET " + ("one two three four five " * 30)
    long_html = "<html><head><title>Official Page</title></head><body>" + ("<p>HTML SECRET MARKER</p>" * 80) + "</body></html>"

    def fake_post_form_json(url, form, timeout, retries, headers=None):
        assert "access_token" in url
        return {"access_token": "token123", "refresh_token": "token456", "token_type": "bearer"}

    def fake_fetch_json(url, timeout, retries, headers=None):
        if "github.com" in url or "/repos/" in url:
            return [
                {
                    "html_url": "https://github.com/example/repo/issues/77",
                    "title": "GitHub issue exposes stale readings",
                    "body": long_github_body,
                    "updated_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
                    "user": {"login": "should_not_be_saved", "avatar_url": "https://example/avatar.png"},
                }
            ]
        assert headers and headers.get("Authorization", "").startswith("Bearer ")
        return {
            "data": {
                "children": [
                    {
                        "data": {
                            "title": "Reddit caregivers need clearer stale data context",
                            "selftext": long_reddit_body,
                            "permalink": "/r/diabetes/comments/abc123/stale-context/",
                            "created_utc": time.time(),
                            "author": "should_not_be_saved",
                            "user": {"login": "should_not_be_saved"},
                        }
                    }
                ]
            }
        }

    def fake_fetch_url(url, timeout, retries):
        assert "example.com" in url
        return long_html

    monkeypatch.setattr(cli, "_post_form_json", fake_post_form_json)
    monkeypatch.setattr(cli, "_fetch_json", fake_fetch_json)
    monkeypatch.setattr(cli, "_fetch_url", fake_fetch_url)

    sources = {
        "sources": [
            {
                "name": "official_help",
                "family": "official_vendor",
                "kind": "official_pages",
                "enabled": True,
                "urls": ["https://example.com/help/stale"],
            },
            {
                "name": "nightscout_github",
                "family": "github_community",
                "kind": "github_issues",
                "enabled": True,
                "repos": ["example/repo"],
            },
            {
                "name": "reddit_diabetes",
                "family": "reddit",
                "kind": "reddit_oauth",
                "enabled": True,
                "subreddits": ["diabetes"],
            },
        ]
    }
    path = tmp_path / "sources.json"
    path.write_text(json.dumps(sources), encoding="utf-8")

    report = cli.run_discovery(path, 5, False, "", "", "", "heuristic", "gpt-5.4-mini", None, 1.0, 1, 3600)
    assert report["status"] in {"SUCCESS", "DEGRADED"}

    cache_path = root / "product" / "generated" / "discovery" / "cache" / "discovery-cache-v1.json"
    cache_text = cache_path.read_text(encoding="utf-8")
    assert "token123" not in cache_text
    assert "token456" not in cache_text
    assert "access_token" not in cache_text
    assert "should_not_be_saved" not in cache_text
    assert long_github_body not in cache_text
    assert long_reddit_body not in cache_text
    assert cli._extract_html_text(long_html) not in cache_text
    assert "REDDIT SELFTEXT SECRET" in cache_text
    assert "<html" not in cache_text.lower()
    assert "<body" not in cache_text.lower()

    cache_json = json.loads(cache_path.read_text(encoding="utf-8"))
    _assert_cache_minimized(cache_json)
    cached_items = []
    for entry in cache_json["entries"].values():
        value = entry["value"]
        cached_items.extend(value.get("items", []) if isinstance(value, dict) else value)
    assert all(len(item.get("excerpt", "")) <= cli.MAX_CACHED_EXCERPT_LENGTH for item in cached_items)

    obs_files = list((root / "product" / "research" / "observations").glob("OBS-*.json"))
    assert obs_files == []  # Single-source WEAK evidence is cacheable but cannot create observations.
    for obs_file in obs_files:
        data = json.loads(obs_file.read_text(encoding="utf-8"))
        text = json.dumps(data).lower()
        assert "should_not_be_saved" not in text
        assert "token123" not in text
        assert "token456" not in text
        assert "author" not in text
        assert "username" not in text
        assert "<html" not in text
        for evidence in data.get("evidence", []):
            assert len(evidence) <= 220
        assert len(data.get("problem_statement", "")) <= 180


def test_cluster_id_stable_for_input_order_permutation(cli_env):
    cli, _ = cli_env
    a = {
        "canonical_url": "https://example.com/a",
        "content_hash": cli.content_hash("A"),
        "problem_statement": "Caregiver cannot quickly detect stale readings",
        "source_name": "source_a",
        "source_identity": "source_a",
        "source_family": "github_community",
        "persona": "caregiver",
        "module": "Home / Monitoring",
        "source_type": "community",
        "type": "usability",
        "severity": "medium",
        "frequency": "frequent",
        "confidence": "medium",
    }
    b = {
        "canonical_url": "https://example.com/b",
        "content_hash": cli.content_hash("B"),
        "problem_statement": "Caregiver struggles to detect stale readings quickly",
        "source_name": "source_b",
        "source_identity": "source_b",
        "source_family": "reddit",
        "persona": "caregiver",
        "module": "Home / Monitoring",
        "source_type": "community",
        "type": "usability",
        "severity": "medium",
        "frequency": "frequent",
        "confidence": "medium",
    }
    c = {
        "canonical_url": "https://example.com/c",
        "content_hash": cli.content_hash("C"),
        "problem_statement": "Caregiver struggles to detect stale readings quickly today",
        "source_name": "source_c",
        "source_identity": "source_c",
        "source_family": "official_vendor",
        "persona": "caregiver",
        "module": "Home / Monitoring",
        "source_type": "community",
        "type": "usability",
        "severity": "medium",
        "frequency": "frequent",
        "confidence": "medium",
    }

    def cluster_id_for(items):
        deduped, _ = cli.dedupe_items(items)
        return cli.cluster_items(deduped)[0]["cluster_id"]

    assert cluster_id_for([a, b, c]) == cluster_id_for([c, b, a])


def _configure_cli_root(cli, root: Path, monkeypatch):
    monkeypatch.setattr(cli, "ROOT", root)
    monkeypatch.setattr(cli, "PRODUCT", root / "product")
    monkeypatch.setattr(cli, "SCHEMA_DIR", root / "product" / "schema")
    monkeypatch.setattr(cli, "OBSERVATIONS_DIR", root / "product" / "research" / "observations")
    monkeypatch.setattr(cli, "REQUIREMENTS_DIR", root / "product" / "requirements")
    monkeypatch.setattr(cli, "DECISIONS_DIR", root / "product" / "decisions")
    monkeypatch.setattr(cli, "GENERATED_DISCOVERY_DIR", root / "product" / "generated" / "discovery")
    monkeypatch.setattr(cli, "VALIDATED_CAPABILITIES_MD", root / "product" / "generated" / "VALIDATED_CAPABILITIES.md")
    monkeypatch.setattr(cli, "SOURCES_CONFIG_DEFAULT", root / "product" / "discovery" / "sources.json")
    monkeypatch.setattr(cli, "DISCOVERY_CACHE_PATH", root / "product" / "generated" / "discovery" / "cache" / "discovery-cache-v1.json")
    monkeypatch.setattr(cli, "DISCOVERY_CLUSTER_REGISTRY_PATH", root / "product" / "discovery" / "cluster-registry.json")
    monkeypatch.setattr(cli, "DISCOVERY_PROPOSED_CLUSTER_REGISTRY_PATH", root / "product" / "generated" / "discovery" / "cluster-registry.proposed.json")


def _make_isolated_cli(monkeypatch):
    cli = load_cli_module()
    temp_dir = tempfile.TemporaryDirectory()
    root = Path(temp_dir.name)
    shutil.copytree(WORKSPACE_ROOT / "product", root / "product")
    _configure_cli_root(cli, root, monkeypatch)
    monkeypatch.delenv("REDDIT_CLIENT_ID", raising=False)
    monkeypatch.delenv("REDDIT_CLIENT_SECRET", raising=False)
    return cli, root, temp_dir


def _write_custom_sources(path: Path, texts: list[str]) -> None:
    payload = {
        "sources": [
            {
                "name": "nightscout_github",
                "family": "github_community",
                "kind": "github_issues",
                "enabled": True,
                "fixture_items": [
                    {
                        "url": f"https://example.com/{idx}",
                        "source_identity": f"nightscout_fixture_{idx}",
                        "text": text,
                        "problem_statement": text,
                        "persona": "caregiver",
                        "mode": "caregiver",
                        "module": "Home / Monitoring",
                        "type": "usability",
                        "severity": "medium",
                        "frequency": "frequent",
                        "confidence": "medium",
                    }
                    for idx, text in enumerate(texts, start=1)
                ],
            }
        ]
    }
    path.write_text(json.dumps(payload), encoding="utf-8")


def _write_ai_payload(root: Path, clusters):
    payload = {"clusters": []}
    for cluster in clusters:
        payload["clusters"].append(
            {
                "cluster_id": cluster["cluster_id"],
                "classification": "PRODUCT_PROBLEM",
                "persona": cluster["persona_candidate"],
                "problem_statement": cluster["normalized_problem"],
                "problem_statement_pl": "Zwięzły opis problemu po polsku",
                "evidence_summary": "Evidence summary",
                "source_diversity_summary": "Diverse sources",
                "current_librecare_match": "AI summary",
                "solvability": "APP",
                "impact_score": 4,
                "frequency_score": 4,
                "evidence_score": 4,
                "solvability_score": 4,
                "novelty_score": 3,
                "effort_score": 2,
                "confidence": "high",
                "counterargument": "Counter point",
                "candidate_recommendation": "Recommend human review",
            }
        )
    path = root / "ai.json"
    path.write_text(json.dumps(payload), encoding="utf-8")
    return path


def _seed_registry(root: Path, entries):
    path = root / "product" / "discovery" / "cluster-registry.json"
    path.write_text(json.dumps({"version": 1, "entries": entries}, indent=2), encoding="utf-8")


def _run_custom_discovery(cli, root: Path, tmp_path: Path, texts: list[str], *, publish: bool = False):
    src_path = tmp_path / f"sources-{len(texts)}.json"
    _write_custom_sources(src_path, texts)
    clusters = _prepare_clusters(cli, src_path)
    ai_file = _write_ai_payload(root, clusters)
    return run_discovery_with_ai_file(cli, src_path, ai_file, publish=publish, repo_owner="o", repo_name="r", github_token="t")


def test_cluster_id_stable_across_clean_runs_without_prior_observations(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    cli1, root1, temp1 = _make_isolated_cli(monkeypatch)
    cli2, root2, temp2 = _make_isolated_cli(monkeypatch)
    try:
        source_a = tmp_path / "sources-a.json"
        source_b = tmp_path / "sources-b.json"
        _write_custom_sources(source_a, [
            "Caregiver cannot quickly detect stale readings",
            "Caregiver struggles to detect stale readings quickly",
        ])
        _write_custom_sources(source_b, [
            "Caregiver cannot quickly detect stale readings",
            "Caregiver struggles to detect stale readings quickly",
            "Caregiver has trouble detecting stale readings quickly",
        ])

        clusters_a = _prepare_clusters(cli1, source_a)
        report_a = run_discovery_with_ai_file(cli1, source_a, _write_ai_payload(root1, clusters_a))

        clusters_b = _prepare_clusters(cli2, source_b)
        report_b = run_discovery_with_ai_file(cli2, source_b, _write_ai_payload(root2, clusters_b))

        assert report_a["top10"][0]["cluster_id"] == report_b["top10"][0]["cluster_id"]
    finally:
        temp1.cleanup()
        temp2.cleanup()


def test_cluster_id_stable_for_input_order_permutation(cli_env):
    cli, _ = cli_env
    a = {
        "canonical_url": "https://example.com/a",
        "content_hash": cli.content_hash("A"),
        "problem_statement": "Caregiver cannot quickly detect stale readings",
        "source_name": "source_a",
        "source_identity": "source_a",
        "source_family": "github_community",
        "persona": "caregiver",
        "module": "Home / Monitoring",
        "source_type": "community",
        "type": "usability",
        "severity": "medium",
        "frequency": "frequent",
        "confidence": "medium",
    }
    b = {
        "canonical_url": "https://example.com/b",
        "content_hash": cli.content_hash("B"),
        "problem_statement": "Caregiver struggles to detect stale readings quickly",
        "source_name": "source_b",
        "source_identity": "source_b",
        "source_family": "reddit",
        "persona": "caregiver",
        "module": "Home / Monitoring",
        "source_type": "community",
        "type": "usability",
        "severity": "medium",
        "frequency": "frequent",
        "confidence": "medium",
    }
    c = {
        "canonical_url": "https://example.com/c",
        "content_hash": cli.content_hash("C"),
        "problem_statement": "Caregiver has trouble detecting stale readings quickly",
        "source_name": "source_c",
        "source_identity": "source_c",
        "source_family": "official_vendor",
        "persona": "caregiver",
        "module": "Home / Monitoring",
        "source_type": "community",
        "type": "usability",
        "severity": "medium",
        "frequency": "frequent",
        "confidence": "medium",
    }

    def cluster_id_for(items):
        deduped, _ = cli.dedupe_items(items)
        return cli.cluster_items(deduped)[0]["cluster_id"]

    assert cluster_id_for([a, b, c]) == cluster_id_for([c, b, a])


def test_persisted_observation_backward_compatibility_keeps_cluster_id(cli_env, tmp_path, monkeypatch):
    cli, root = cli_env

    class FakeClient:
        created = []

        def __init__(self, owner, repo, token):
            self.owner = owner
            self.repo = repo
            self.token = token

        def find_issue_by_marker(self, marker):
            for issue in self.__class__.created:
                if marker in issue["body"]:
                    return issue
            return None

        def create_issue(self, title, body, labels):
            issue = {
                "number": len(self.__class__.created) + 1,
                "html_url": f"https://example/issues/{len(self.__class__.created)+1}",
                "title": title,
                "body": body,
                "labels": labels,
            }
            self.__class__.created.append(issue)
            return issue

    monkeypatch.setattr(cli, "GITHUB_CLIENT_FACTORY", FakeClient)

    source_a = tmp_path / "sources-a.json"
    source_b = tmp_path / "sources-b.json"
    _write_custom_sources(source_a, [
        "Caregiver cannot quickly detect stale readings",
        "Caregiver struggles to detect stale readings quickly",
    ])
    _write_custom_sources(source_b, [
        "Caregiver cannot quickly detect stale readings",
        "Caregiver struggles to detect stale readings quickly",
        "Caregiver has trouble detecting stale readings quickly",
    ])

    clusters_a = _prepare_clusters(cli, source_a)
    report_a = run_discovery_with_ai_file(cli, source_a, _write_ai_payload(root, clusters_a), publish=True, repo_owner="o", repo_name="r", github_token="t")
    cluster_id_a = report_a["top10"][0]["cluster_id"]

    obs_dir = root / "product" / "research" / "observations"
    copied_obs = list(obs_dir.glob("OBS-*.json"))
    assert copied_obs

    fresh_cli, fresh_root, fresh_temp = _make_isolated_cli(monkeypatch)
    try:
        monkeypatch.setattr(fresh_cli, "GITHUB_CLIENT_FACTORY", FakeClient)
        fresh_obs = fresh_root / "product" / "research" / "observations"
        for src in copied_obs:
            shutil.copy2(src, fresh_obs / src.name)
        clusters_b = _prepare_clusters(fresh_cli, source_b)
        report_b = run_discovery_with_ai_file(fresh_cli, source_b, _write_ai_payload(fresh_root, clusters_b), publish=True, repo_owner="o", repo_name="r", github_token="t")
        assert report_b["top10"][0]["cluster_id"] == cluster_id_a
    finally:
        fresh_temp.cleanup()


def test_incremental_rerun_no_duplicate_observations_and_markers(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")

    class FakeClient:
        created = []

        def __init__(self, owner, repo, token):
            self.owner = owner
            self.repo = repo
            self.token = token

        def find_issue_by_marker(self, marker):
            for issue in self.__class__.created:
                if marker in issue["body"]:
                    return issue
            return None

        def create_issue(self, title, body, labels):
            issue = {
                "number": len(self.__class__.created) + 1,
                "html_url": f"https://example/issues/{len(self.__class__.created)+1}",
                "title": title,
                "body": body,
                "labels": labels,
            }
            self.__class__.created.append(issue)
            return issue

    monkeypatch = pytest.MonkeyPatch()
    monkeypatch.setattr(cli, "GITHUB_CLIENT_FACTORY", FakeClient)
    try:
        r1 = run_discovery_with_ai_file(cli, fixture_sources, ai_file, publish=True, repo_owner="o", repo_name="r", github_token="t")
        r2 = run_discovery_with_ai_file(cli, fixture_sources, ai_file, publish=True, repo_owner="o", repo_name="r", github_token="t")
    finally:
        monkeypatch.undo()

    assert r1["status"] in {"SUCCESS", "DEGRADED"}
    assert r2["status"] in {"SUCCESS", "DEGRADED"}
    obs_files = list((root / "product" / "research" / "observations").glob("OBS-*.json"))
    assert len(obs_files) == len({p.name for p in obs_files})
    assert len(FakeClient.created) == len({issue["body"] for issue in FakeClient.created})
    assert any(action["action"] == "SKIPPED_EXISTS" for action in r2.get("top3_issue_actions", []))


def test_registry_initial_creation_and_report_fields(cli_env, tmp_path):
    cli, root = cli_env
    _seed_registry(root, [])
    report = _run_custom_discovery(
        cli,
        root,
        tmp_path,
        [
            "Caregiver cannot quickly detect stale readings",
            "Caregiver struggles to detect stale readings quickly",
        ],
        publish=False,
    )

    reg = report.get("cluster_registry") or {}
    assert reg.get("existing_entries") == 0
    assert reg.get("new_entries") == 1
    assert reg.get("matched_existing") == 0
    assert reg.get("ambiguous_matches") == []
    assert reg.get("proposed_registry_path")

    proposed = root / reg["proposed_registry_path"]
    data = json.loads(proposed.read_text(encoding="utf-8"))
    assert len(data.get("entries", [])) == 1
    assert data["entries"][0]["cluster_id"] == report["top10"][0]["cluster_id"]


def test_registry_incremental_reuse_no_duplicate_observation_or_marker(cli_env, tmp_path, monkeypatch):
    cli, root = cli_env

    class FakeClient:
        created = []

        def __init__(self, owner, repo, token):
            self.owner = owner
            self.repo = repo
            self.token = token

        def find_issue_by_marker(self, marker):
            for issue in self.__class__.created:
                if marker in issue["body"]:
                    return issue
            return None

        def create_issue(self, title, body, labels):
            issue = {
                "number": len(self.__class__.created) + 1,
                "html_url": f"https://example/issues/{len(self.__class__.created)+1}",
                "title": title,
                "body": body,
                "labels": labels,
            }
            self.__class__.created.append(issue)
            return issue

    monkeypatch.setattr(cli, "GITHUB_CLIENT_FACTORY", FakeClient)
    _seed_registry(root, [])

    report1 = _run_custom_discovery(
        cli,
        root,
        tmp_path,
        [
            "Caregiver cannot quickly detect stale readings",
            "Caregiver struggles to detect stale readings quickly",
        ],
        publish=True,
    )
    cluster_id_1 = report1["top10"][0]["cluster_id"]
    obs_count_1 = len(list((root / "product" / "research" / "observations").glob("OBS-*.json")))

    # Simulate explicit human-reviewed persistence of proposed registry.
    reg_path = root / "product" / "discovery" / "cluster-registry.json"
    proposed1 = root / report1["cluster_registry"]["proposed_registry_path"]
    shutil.copy2(proposed1, reg_path)

    report2 = _run_custom_discovery(
        cli,
        root,
        tmp_path,
        [
            "Caregiver cannot quickly detect stale readings",
            "Caregiver struggles to detect stale readings quickly",
            "Caregiver cannot detect stale readings quickly because display is confusing",
        ],
        publish=True,
    )
    cluster_id_2 = report2["top10"][0]["cluster_id"]
    obs_count_2 = len(list((root / "product" / "research" / "observations").glob("OBS-*.json")))

    proposed2 = root / report2["cluster_registry"]["proposed_registry_path"]
    reg2 = json.loads(proposed2.read_text(encoding="utf-8"))
    ids = [x["cluster_id"] for x in reg2.get("entries", [])]

    assert cluster_id_1 == cluster_id_2
    assert obs_count_2 == obs_count_1
    assert ids.count(cluster_id_1) == 1
    assert [action["action"] for action in report2.get("top3_issue_actions", [])] == ["CREATED"]


def test_registry_unclassified_problem_fails_closed(cli_env, tmp_path):
    cli, root = cli_env
    _seed_registry(root, [])

    report1 = _run_custom_discovery(
        cli,
        root,
        tmp_path,
        [
            "Caregiver cannot quickly detect stale readings",
            "Caregiver struggles to detect stale readings quickly",
        ],
    )
    proposed1 = root / report1["cluster_registry"]["proposed_registry_path"]
    shutil.copy2(proposed1, root / "product" / "discovery" / "cluster-registry.json")

    report2 = _run_custom_discovery(
        cli,
        root,
        tmp_path,
        [
            "Application startup crashes when opening settings",
            "Users report startup crash before dashboard appears",
        ],
    )
    assert report2["top10"] == []
    assert report2["created_observations"] == []
    assert report2["cluster_registry"]["new_entries"] == 0


def test_registry_ambiguous_match_is_safe_and_not_published(cli_env, tmp_path, monkeypatch):
    cli, root = cli_env

    class FakeClient:
        created = 0

        def __init__(self, owner, repo, token):
            self.owner = owner
            self.repo = repo
            self.token = token

        def find_issue_by_marker(self, marker):
            return None

        def create_issue(self, title, body, labels):
            self.__class__.created += 1
            return {"number": self.__class__.created, "html_url": "https://example/issue"}

    monkeypatch.setattr(cli, "GITHUB_CLIENT_FACTORY", FakeClient)

    _seed_registry(
        root,
        [
            {
                "cluster_id": "DISC-AAAA1111AAAA",
                "canonical_problem_key": "concept:glucose_sharing_delay_or_failure|topic:caregiver|facets:stale_data",
                "problem_fingerprint": "a1",
                "persona": "caregiver",
                "module": "Home / Monitoring",
                "created_at": "2026-09-01T10:00:00Z",
                "last_seen_at": "2026-09-01T10:00:00Z",
            },
            {
                "cluster_id": "DISC-BBBB2222BBBB",
                "canonical_problem_key": "concept:glucose_sharing_delay_or_failure|topic:caregiver|facets:stale_data",
                "problem_fingerprint": "b2",
                "persona": "caregiver",
                "module": "Home / Monitoring",
                "created_at": "2026-09-01T10:00:00Z",
                "last_seen_at": "2026-09-01T10:00:00Z",
            },
        ],
    )

    report = _run_custom_discovery(
        cli,
        root,
        tmp_path,
        ["Caregiver cannot quickly detect stale readings"],
        publish=True,
    )
    reg = report.get("cluster_registry") or {}
    assert reg.get("ambiguous_matches")
    assert FakeClient.created == 0
    assert all(not item.get("eligibility") for item in report.get("top10", []))


def test_clean_workflow_simulation_with_persisted_registry_only(cli_env, tmp_path, monkeypatch):
    cli1, root1, temp1 = _make_isolated_cli(monkeypatch)
    cli2, root2, temp2 = _make_isolated_cli(monkeypatch)
    try:
        _seed_registry(root1, [])
        report1 = _run_custom_discovery(
            cli1,
            root1,
            tmp_path,
            [
                "Caregiver cannot quickly detect stale readings",
                "Caregiver struggles to detect stale readings quickly",
            ],
        )
        cluster_id_1 = report1["top10"][0]["cluster_id"]
        proposed1 = root1 / report1["cluster_registry"]["proposed_registry_path"]

        # Fresh checkout simulation: do not copy observations, copy only persisted registry state.
        shutil.copy2(proposed1, root2 / "product" / "discovery" / "cluster-registry.json")
        for obs in (root2 / "product" / "research" / "observations").glob("OBS-*.json"):
            obs.unlink()

        report2 = _run_custom_discovery(
            cli2,
            root2,
            tmp_path,
            [
                "Caregiver cannot quickly detect stale readings",
                "Caregiver struggles to detect stale readings quickly",
                "Caregiver cannot detect stale readings quickly because display is confusing",
            ],
        )
        assert report2["top10"][0]["cluster_id"] == cluster_id_1
    finally:
        temp1.cleanup()
        temp2.cleanup()


def test_malformed_ai_output_safe_failure_no_issue_creation(cli_env, fixture_sources, monkeypatch):
    cli, root = cli_env

    class FakeClient:
        created = 0

        def __init__(self, owner, repo, token):
            pass

        def find_issue_by_marker(self, marker):
            return None

        def create_issue(self, title, body, labels):
            self.__class__.created += 1
            return {"number": 1, "html_url": "https://example/1"}

    monkeypatch.setattr(cli, "GITHUB_CLIENT_FACTORY", FakeClient)

    ai_file = root / "bad-ai.txt"
    ai_file.write_text("not-json", encoding="utf-8")
    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file, publish=True, repo_owner="o", repo_name="r", github_token="t")
    assert report["status"] == "FAILED"
    assert FakeClient.created == 0


def test_ai_call_cap_and_model(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters)), encoding="utf-8")
    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file)
    assert report["counts"]["AI_CALLS"] <= 2


def test_copilot_programmatic_invocation_and_json_parse(cli_env, monkeypatch):
    cli, _ = cli_env

    class FakeResult:
        returncode = 0
        stdout = '{"clusters": []}'
        stderr = ""

    seen = {}

    def fake_run(cmd, capture_output, text, check):
        seen["cmd"] = cmd
        return FakeResult()

    monkeypatch.setattr(cli.subprocess, "run", fake_run)
    out = cli.run_copilot_json('{"prompt":"x"}', model="gpt-5.4-mini")
    parsed = cli.extract_json_object(out)

    cmd = seen["cmd"]
    assert "-p" in cmd
    assert "--model=gpt-5.4-mini" in cmd
    assert "--deny-tool=write" in cmd
    assert "--deny-tool=shell" in cmd
    assert parsed == {"clusters": []}


def test_top3_eligible_selection_filters_before_limit(cli_env, fixture_sources, monkeypatch):
    cli, root = cli_env

    class FakeClient:
        created = []

        def __init__(self, owner, repo, token):
            pass

        def find_issue_by_marker(self, marker):
            return None

        def create_issue(self, title, body, labels):
            issue = {"number": len(self.__class__.created) + 1, "html_url": "https://example/issue"}
            self.__class__.created.append(issue)
            return issue

    monkeypatch.setattr(cli, "GITHUB_CLIENT_FACTORY", FakeClient)

    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")
    if len(payload["clusters"]) < 6:
        for idx in range(6 - len(payload["clusters"])):
            payload["clusters"].append({
                **payload["clusters"][0],
                "cluster_id": f"DISC-TEST-{idx}",
                "problem_statement": f"Problem {idx}",
            })

    payload["clusters"][0]["classification"] = "INCONCLUSIVE"
    payload["clusters"][1]["classification"] = "TEST_COVERAGE_GAP"
    payload["clusters"][2]["classification"] = "PRODUCT_PROBLEM"
    payload["clusters"][3]["classification"] = "PRODUCT_OPPORTUNITY"
    payload["clusters"][4]["classification"] = "SAFETY_GAP"

    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")
    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file, publish=True, repo_owner="o", repo_name="r", github_token="t")

    created_or_skipped = [x for x in report["top3_issue_actions"] if x["action"] in {"CREATED", "SKIPPED_EXISTS"}]
    assert len(created_or_skipped) <= 3


def test_canonical_hold_reject_match_suppresses_reproposal(cli_env, fixture_sources):
    cli, root = cli_env
    decision = {
        "id": "DEC-9999",
        "date": "2026-09-05",
        "subject": "Shared glucose readings can be delayed, missing, or unavailable.",
        "status": "REJECTED",
        "decision": "Not now",
        "reason": "Already covered",
        "evidence": ["E"],
        "counterargument": "C",
        "revisit_condition": "Never",
        "related_requirements": [],
    }
    (root / "product" / "decisions" / "DEC-9999.json").write_text(json.dumps(decision), encoding="utf-8")

    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")), encoding="utf-8")
    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file)
    assert report["top10"] == []


def test_no_direct_requirement_acceptance_or_implementation_creation(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")), encoding="utf-8")
    before_req = set((root / "product" / "requirements").glob("REQ-*"))
    before_impl = set((root / "product" / "implementation").glob("IMP-*"))
    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file)
    after_req = set((root / "product" / "requirements").glob("REQ-*"))
    after_impl = set((root / "product" / "implementation").glob("IMP-*"))
    assert report["safety_guards"]["no_auto_merge"] is True
    assert before_req == after_req
    assert before_impl == after_impl


def test_repo_name_starting_dash_regression_parser(cli_env):
    cli, _ = cli_env
    parser = cli.build_parser()
    args = parser.parse_args(["--repo-name=-LibreDisplay"])
    assert args.repo_name == "-LibreDisplay"


def test_privacy_no_username_or_full_post_persisted(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")), encoding="utf-8")
    run_discovery_with_ai_file(cli, fixture_sources, ai_file)

    obs_files = list((root / "product" / "research" / "observations").glob("OBS-*.json"))
    assert obs_files
    for p in obs_files:
        data = json.loads(p.read_text(encoding="utf-8"))
        txt = json.dumps(data).lower()
        assert "username" not in txt
        assert "author" not in txt
        for evidence in data.get("evidence", []):
            assert len(evidence) <= 220


def test_score_deterministic_for_fixed_input(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters)), encoding="utf-8")
    r1 = run_discovery_with_ai_file(cli, fixture_sources, ai_file)
    r2 = run_discovery_with_ai_file(cli, fixture_sources, ai_file)
    s1 = [(x["cluster_id"], x["score"]) for x in r1["top10"]]
    s2 = [(x["cluster_id"], x["score"]) for x in r2["top10"]]
    assert s1 == s2


def test_report_sections_cover_all_top10_and_show_top_candidates(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")), encoding="utf-8")
    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file)

    md = Path(root / report["md_report_path"]).read_text(encoding="utf-8")
    assert "## TOP CANDIDATES" in md
    assert "## WORTH REVIEWING" in md
    assert "## ALREADY COVERED" in md
    assert "## ABBOTT LIMITATION / NOT LIBRECARE-SOLVABLE" in md
    assert "## TEST / RESEARCH GAPS" in md
    assert "## INSUFFICIENT EVIDENCE" in md

    top_lines = [line for line in md.splitlines() if line.startswith("TOP ")]
    assert len(top_lines) == len(report["top10"])


def test_product_foundation_match_is_deterministic_source_of_truth(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    ai_payload = build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")
    for row in ai_payload["clusters"]:
        row["current_librecare_match"] = "AI CONTRADICTION"
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(ai_payload), encoding="utf-8")

    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file)
    assert any("AI CONTRADICTION" != x["current_librecare_match"] for x in report["top10"])


def test_cache_hit_miss_stale_invalid_and_no_credentials_written(cli_env, tmp_path):
    cli, _ = cli_env
    cache_path = tmp_path / "cache.json"
    cache = cli.DiscoveryCache(cache_path)

    assert cache.get("k", 10) is None
    cache.put("k", {"value": {"x": 1}})
    assert cache.get("k", 10) is not None

    stale = {"version": 1, "entries": {"old": {"cached_at": int(time.time()) - 9999, "value": {"value": {"x": 2}}}}}
    cache_path.write_text(json.dumps(stale), encoding="utf-8")
    stale_cache = cli.DiscoveryCache(cache_path)
    assert stale_cache.get("old", 5) is None

    cache_path.write_text("{broken-json", encoding="utf-8")
    invalid_cache = cli.DiscoveryCache(cache_path)
    assert invalid_cache.get("anything", 5) is None

    clean_cache = cli.DiscoveryCache(cache_path)
    clean_cache.put("safe", {"value": {"hello": "world"}})
    clean_cache.save()
    txt = cache_path.read_text(encoding="utf-8").lower()
    assert "github_token" not in txt
    assert "authorization" not in txt
    assert "client_secret" not in txt


def test_workflow_regressions_no_write_paths_and_dash_safe_repo_name():
    workflow_path = WORKSPACE_ROOT / ".github" / "workflows" / "librecare-discovery.yml"
    text = workflow_path.read_text(encoding="utf-8")

    assert "workflow_dispatch:" in text
    assert "schedule:" not in text
    assert "default: false" in text

    assert "contents: read" in text
    assert "contents: write" not in text

    assert "--repo-name=\"${{ github.event.repository.name }}\"" in text

    forbidden = ["git push", "git commit", "git pull", "gh pr merge", "pulls.merge", "auto-merge", "enable_auto_merge"]
    lowered = text.lower()
    for token in forbidden:
        assert token.lower() not in lowered


def test_no_android_app_code_modified_by_discovery_runtime(cli_env, fixture_sources):
    cli, root = cli_env
    app_dir = root / "app"
    app_dir.mkdir(exist_ok=True)
    marker = app_dir / "marker.txt"
    marker.write_text("unchanged", encoding="utf-8")

    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters)), encoding="utf-8")
    run_discovery_with_ai_file(cli, fixture_sources, ai_file)
    assert marker.read_text(encoding="utf-8") == "unchanged"


def test_preserve_corrected_discovery_logic_file_exists():
    target = WORKSPACE_ROOT / "scripts" / "product" / "tests" / "test_corrected_discovery_logic.py"
    assert target.exists()


def test_missing_registry_bootstrap_is_valid(cli_env, tmp_path):
    """Empty registry file (not present) should bootstrap cleanly."""
    cli, root = cli_env
    # Use a fresh path that doesn't exist
    registry_path = tmp_path / "cluster-registry.json"
    assert not registry_path.exists()

    # load_cluster_registry should return empty registry on missing file
    registry = cli.load_cluster_registry(registry_path)
    assert registry == {"version": 1, "entries": []}


def test_malformed_json_registry_fails_closed(cli_env):
    """Registry with malformed JSON should fail closed (raise exception)."""
    cli, root = cli_env
    registry_path = root / "product" / "discovery" / "cluster-registry.json"
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    registry_path.write_text("{broken json content", encoding="utf-8")

    # load_cluster_registry should raise an exception, not silently return empty
    with pytest.raises((RuntimeError, json.JSONDecodeError)):
        cli.load_cluster_registry(registry_path)


def test_malformed_json_registry_fails_closed(cli_env):
    """Registry with malformed JSON should fail closed (raise RuntimeError)."""
    cli, root = cli_env
    registry_path = root / "product" / "discovery" / "cluster-registry.json"
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    registry_path.write_text("{broken json content", encoding="utf-8")

    # load_cluster_registry should raise RuntimeError, not return empty
    with pytest.raises(RuntimeError):
        cli.load_cluster_registry(registry_path)


def test_top_level_list_registry_fails_closed(cli_env):
    """Registry with top-level list (not object) should fail closed."""
    cli, root = cli_env
    registry_path = root / "product" / "discovery" / "cluster-registry.json"
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    registry_path.write_text(json.dumps([1, 2, 3]), encoding="utf-8")

    # Should fail closed, not silently convert to empty
    with pytest.raises(RuntimeError, match="is not a JSON object"):
        cli.load_cluster_registry(registry_path)


def test_missing_entries_field_registry_fails_closed(cli_env):
    """Registry with missing entries field should fail closed."""
    cli, root = cli_env
    registry_path = root / "product" / "discovery" / "cluster-registry.json"
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    registry_path.write_text(json.dumps({"version": 1}), encoding="utf-8")

    # Should fail closed, not silently add empty entries
    with pytest.raises(RuntimeError, match="missing required 'entries' field"):
        cli.load_cluster_registry(registry_path)


def test_entries_not_list_registry_fails_closed(cli_env):
    """Registry with entries not a list should fail closed."""
    cli, root = cli_env
    registry_path = root / "product" / "discovery" / "cluster-registry.json"
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    registry_path.write_text(json.dumps({"version": 1, "entries": "not a list"}), encoding="utf-8")

    # Should fail closed
    with pytest.raises(RuntimeError, match="'entries' field is not a list"):
        cli.load_cluster_registry(registry_path)


def test_invalid_entry_missing_fields_registry_fails_closed(cli_env):
    """Registry with entry missing required fields should fail closed."""
    cli, root = cli_env
    registry_path = root / "product" / "discovery" / "cluster-registry.json"
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    # Entry missing cluster_id
    registry_path.write_text(json.dumps({
        "version": 1,
        "entries": [
            {"canonical_problem_key": "test", "persona": "caregiver"}
        ]
    }), encoding="utf-8")

    # Should fail closed
    with pytest.raises(RuntimeError, match="missing required fields"):
        cli.load_cluster_registry(registry_path)


def test_invalid_entry_not_object_registry_fails_closed(cli_env):
    """Registry with entry not an object should fail closed."""
    cli, root = cli_env
    registry_path = root / "product" / "discovery" / "cluster-registry.json"
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    registry_path.write_text(json.dumps({
        "version": 1,
        "entries": [
            "not an object"
        ]
    }), encoding="utf-8")

    # Should fail closed
    with pytest.raises(RuntimeError, match="is not an object"):
        cli.load_cluster_registry(registry_path)


def test_invalid_registry_structure_fails_closed(cli_env):
    """Registry with structural issues should fail closed."""
    cli, root = cli_env
    registry_path = root / "product" / "discovery" / "cluster-registry.json"
    registry_path.parent.mkdir(parents=True, exist_ok=True)

    # Valid registry should still work
    valid_registry = {
        "version": 1,
        "entries": [
            {
                "cluster_id": "DISC-TEST001",
                "canonical_problem_key": "caregiver read glucose",
                "problem_fingerprint": "abc123",
                "persona": "caregiver",
                "module": "monitoring"
            }
        ]
    }
    registry_path.write_text(json.dumps(valid_registry), encoding="utf-8")
    registry = cli.load_cluster_registry(registry_path)
    assert len(registry["entries"]) == 1
    assert registry["entries"][0]["cluster_id"] == "DISC-TEST001"


def test_unsupported_registry_version_fails_closed(cli_env):
    """Registry with unsupported version should fail closed with RuntimeError."""
    cli, root = cli_env
    registry_path = root / "product" / "discovery" / "cluster-registry.json"
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    registry_path.write_text(json.dumps({"version": 999, "entries": []}), encoding="utf-8")

    # Should fail closed with RuntimeError (not ValueError)
    with pytest.raises(RuntimeError, match="Unsupported registry version"):
        cli.load_cluster_registry(registry_path)


def test_malformed_registry_prevents_inbox_publication(cli_env, fixture_sources):
    """When registry fails to load, publication should be blocked."""
    cli, root = cli_env

    # Prepare clusters while registry is valid
    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters)), encoding="utf-8")

    # NOW break the registry
    registry_path = root / "product" / "discovery" / "cluster-registry.json"
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    registry_path.write_text("{broken json", encoding="utf-8")

    # Run discovery with broken registry
    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file)

    # Should fail due to registry issue
    assert report["status"] == "FAILED"
    assert any("Registry load failed" in err for err in report["errors"])

    # No observations should be created
    assert len(report["created_observations"]) == 0

    # No issues should be created
    assert len(report["top3_issue_actions"]) == 0


def test_missing_entries_field_prevents_creation(cli_env, fixture_sources):
    """Registry with missing entries field must block observation/issue creation."""
    cli, root = cli_env

    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters)), encoding="utf-8")

    # Break with missing entries
    registry_path = root / "product" / "discovery" / "cluster-registry.json"
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    registry_path.write_text(json.dumps({"version": 1}), encoding="utf-8")

    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file, publish=True)

    assert report["status"] == "FAILED"
    assert len(report["created_observations"]) == 0
    assert len(report["top3_issue_actions"]) == 0


def test_top_level_list_prevents_creation(cli_env, fixture_sources):
    """Registry with top-level list must block observation/issue creation."""
    cli, root = cli_env

    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters)), encoding="utf-8")

    # Break with top-level list
    registry_path = root / "product" / "discovery" / "cluster-registry.json"
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    registry_path.write_text(json.dumps([1, 2, 3]), encoding="utf-8")

    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file, publish=True)

    assert report["status"] == "FAILED"
    assert len(report["created_observations"]) == 0
    assert len(report["top3_issue_actions"]) == 0


def test_entries_not_list_prevents_creation(cli_env, fixture_sources):
    """Registry with entries not a list must block observation/issue creation."""
    cli, root = cli_env

    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters)), encoding="utf-8")

    # Break with entries not a list
    registry_path = root / "product" / "discovery" / "cluster-registry.json"
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    registry_path.write_text(json.dumps({"version": 1, "entries": "not a list"}), encoding="utf-8")

    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file, publish=True)

    assert report["status"] == "FAILED"
    assert len(report["created_observations"]) == 0
    assert len(report["top3_issue_actions"]) == 0


def test_invalid_entry_prevents_creation(cli_env, fixture_sources):
    """Registry with invalid entry must block observation/issue creation."""
    cli, root = cli_env

    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters)), encoding="utf-8")

    # Break with invalid entry
    registry_path = root / "product" / "discovery" / "cluster-registry.json"
    registry_path.parent.mkdir(parents=True, exist_ok=True)
    registry_path.write_text(json.dumps({
        "version": 1,
        "entries": [
            {"canonical_problem_key": "test"}  # missing cluster_id
        ]
    }), encoding="utf-8")

    report = run_discovery_with_ai_file(cli, fixture_sources, ai_file, publish=True)

    assert report["status"] == "FAILED"
    assert len(report["created_observations"]) == 0
    assert len(report["top3_issue_actions"]) == 0

# Score normalization tests
def test_normalize_ai_score_int_accepted(cli_env):
    """JSON integer 0-5 should be accepted."""
    cli, _ = cli_env
    for val in [0, 1, 2, 3, 4, 5]:
        assert cli.normalize_ai_score(val) == val
    assert cli.normalize_ai_score(-1) is None
    assert cli.normalize_ai_score(6) is None


def test_normalize_ai_score_integral_float_normalized(cli_env):
    """Integral float (4.0) should normalize to int 4."""
    cli, _ = cli_env
    assert cli.normalize_ai_score(4.0) == 4
    assert cli.normalize_ai_score(0.0) == 0
    assert cli.normalize_ai_score(5.0) == 5


def test_normalize_ai_score_numeric_string_normalized(cli_env):
    """Numeric string "4" should normalize to int 4."""
    cli, _ = cli_env
    assert cli.normalize_ai_score("4") == 4
    assert cli.normalize_ai_score("0") == 0
    assert cli.normalize_ai_score("5") == 5


def test_normalize_ai_score_bool_rejected(cli_env):
    """Bool True/False should be rejected."""
    cli, _ = cli_env
    assert cli.normalize_ai_score(True) is None
    assert cli.normalize_ai_score(False) is None


def test_normalize_ai_score_decimal_rejected(cli_env):
    """Decimal scores should be rejected."""
    cli, _ = cli_env
    assert cli.normalize_ai_score(4.5) is None
    assert cli.normalize_ai_score(3.14) is None


def test_normalize_ai_score_string_decimal_rejected(cli_env):
    """String decimal "4.5" should be rejected."""
    cli, _ = cli_env
    assert cli.normalize_ai_score("4.5") is None


def test_normalize_ai_score_fraction_rejected(cli_env):
    """Fraction "4/5" should be rejected."""
    cli, _ = cli_env
    assert cli.normalize_ai_score("4/5") is None


def test_normalize_ai_score_text_label_rejected(cli_env):
    """Text labels like "high" should be rejected."""
    cli, _ = cli_env
    assert cli.normalize_ai_score("high") is None
    assert cli.normalize_ai_score("medium") is None
    assert cli.normalize_ai_score("low") is None


def test_normalize_ai_score_out_of_range_rejected(cli_env):
    """Out-of-range values should be rejected."""
    cli, _ = cli_env
    assert cli.normalize_ai_score(-1) is None
    assert cli.normalize_ai_score(6) is None
    assert cli.normalize_ai_score("6") is None
    assert cli.normalize_ai_score("-1") is None


def test_normalize_ai_score_missing_rejected(cli_env):
    """Missing/None should be rejected."""
    cli, _ = cli_env
    assert cli.normalize_ai_score(None) is None


def test_normalize_ai_output_normalizes_scores(cli_env):
    """normalize_ai_output should normalize all score fields."""
    cli, _ = cli_env
    payload = {
        "clusters": [
            {
                "cluster_id": "DISC-ABC123",
                "impact_score": 4.0,  # integral float -> should become 4
                "frequency_score": "3",  # numeric string -> should become 3
                "evidence_score": 4,
                "solvability_score": 4.5,  # decimal, should stay invalid
                "novelty_score": 2,
                "effort_score": 2,
            }
        ]
    }
    normalized = cli.normalize_ai_output(payload)
    assert normalized["clusters"][0]["impact_score"] == 4
    assert normalized["clusters"][0]["frequency_score"] == 3
    assert normalized["clusters"][0]["evidence_score"] == 4
    assert normalized["clusters"][0]["solvability_score"] == 4.5  # Should stay invalid for validation to catch
    assert normalized["clusters"][0]["novelty_score"] == 2
    assert normalized["clusters"][0]["effort_score"] == 2


def test_validate_ai_output_rejects_bool_score_in_complete_payload(cli_env, fixture_sources):
    """Final validator must reject bool score values even in a complete payload."""
    cli, _ = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters)
    payload["clusters"][0]["impact_score"] = True

    valid, errors = cli.validate_ai_output(payload, clusters)
    assert valid is False
    assert any("impact_score must be int 0..5" in err for err in errors)


def test_validator_rejects_each_authoritative_required_field_when_missing(cli_env, fixture_sources):
    cli, _ = cli_env
    cluster = _prepare_clusters(cli, fixture_sources)[0]
    complete_row = build_ai_payload_for_clusters([cluster])["clusters"][0]

    for field in cli.AI_CLUSTER_REQUIRED_FIELDS:
        candidate = dict(complete_row)
        del candidate[field]
        valid, errors = cli.validate_ai_output({"clusters": [candidate]}, [cluster])
        assert valid is False, field
        assert any("missing fields" in error and field in error for error in errors), field


def _run_discovery_with_two_ai_payloads(
    cli, fixture_sources, monkeypatch, first_payload: dict, second_payload: dict, prompt_sink: list | None = None,
):
    ai_responses = [json.dumps(first_payload), json.dumps(second_payload)]
    call_count = [0]

    def mock_run_copilot(prompt: str, model: str) -> str:
        nonlocal call_count
        if prompt_sink is not None:
            prompt_sink.append(json.loads(prompt))
        call_count[0] += 1
        return ai_responses[min(call_count[0] - 1, 1)]

    monkeypatch.setattr(cli, "run_copilot_json", mock_run_copilot)
    report = cli.run_discovery(
        sources_file=fixture_sources,
        max_items_per_source=20,
        publish_top3_flag=False,
        repo_owner="",
        repo_name="",
        github_token="",
        ai_mode="copilot",
        ai_model="gpt-5.4-mini",
        ai_response_file=None,
        timeout=2.0,
        retries=1,
        cache_max_age_seconds=3600,
    )
    return report


def _authoritative_repair_clusters():
    return [
        {
            "cluster_id": cluster_id,
            "normalized_problem": f"Deterministic problem {cluster_id}",
            "canonical_problem_statement": f"Canonical problem {cluster_id}",
            "canonical_problem_statement_pl": f"Kanoniczny problem {cluster_id}",
            "persona_candidate": "unknown",
            "module_candidate": "unknown",
            "concept_id": "stale_or_missing_readings",
            "shared_intent_facets": ["missing_data"],
            "raw_items": [{
                "source_identity": "bounded/source",
                "source_family": "github_community",
                "problem_statement": "Bounded evidence",
                "excerpt": "Bounded excerpt",
            }],
        }
        for cluster_id in ("DISC-AAAA", "DISC-BBBB", "DISC-CCCC")
    ]


def _multi_cluster_repair_sources(tmp_path):
    sources = {
        "sources": [{
            "name": "repair_fixture",
            "family": "github_community",
            "evidence_role": "developer_community",
            "fixture_items": [
                {
                    "url": "https://example.com/missing",
                    "text": "Glucose readings are missing and do not arrive.",
                    "problem_statement": "Glucose readings are missing",
                    "source_identity": "repo/missing", "language": "en",
                    "concept_id": "stale_or_missing_readings", "topic_id": "data_freshness",
                    "persona": "unknown", "mode": "unknown", "module": "unknown",
                },
                {
                    "url": "https://example.com/alerts",
                    "text": "Glucose alerts do not activate when expected.",
                    "problem_statement": "Glucose alerts do not activate",
                    "source_identity": "repo/alerts", "language": "en",
                    "concept_id": "alerts_not_firing", "topic_id": "alerts",
                    "persona": "unknown", "mode": "unknown", "module": "unknown",
                },
                {
                    "url": "https://example.com/disconnect",
                    "text": "The sensor connection is lost and the app disconnects.",
                    "problem_statement": "The sensor connection is lost",
                    "source_identity": "repo/disconnect", "language": "en",
                    "concept_id": "signal_loss_disconnect", "topic_id": "connectivity",
                    "persona": "unknown", "mode": "unknown", "module": "unknown",
                },
            ],
        }],
    }
    path = tmp_path / "authoritative-repair-sources.json"
    path.write_text(json.dumps(sources), encoding="utf-8")
    return path


def _six_cluster_repair_sources(tmp_path):
    concepts = [
        ("stale_or_missing_readings", "data_freshness", "Glucose readings are missing and do not arrive."),
        ("alerts_not_firing", "alert_reliability", "Glucose alerts do not activate when expected."),
        ("signal_loss_disconnect", "connectivity", "The sensor connection is lost and the app disconnects."),
        ("caregiver_remote_monitoring", "caregiver", "A caregiver cannot remotely monitor glucose."),
        ("sensor_expiry_notification", "sensor_lifecycle", "The sensor expiry notification is missing."),
        ("watch_widget_glanceability", "glanceability", "The glucose watch widget is hard to read."),
    ]
    fixture_items = []
    for index, (concept_id, topic_id, problem) in enumerate(concepts):
        fixture_items.append({
            "url": f"https://example.com/partial-repair/{index}",
            "text": problem,
            "problem_statement": problem,
            "source_identity": f"repo/partial-repair-{index}",
            "language": "en",
            "concept_id": concept_id,
            "topic_id": topic_id,
            "persona": "unknown",
            "mode": "unknown",
            "module": "unknown",
        })
    path = tmp_path / "partial-repair-six-sources.json"
    path.write_text(json.dumps({"sources": [{
        "name": "partial_repair_fixture",
        "family": "github_community",
        "evidence_role": "developer_community",
        "fixture_items": fixture_items,
    }]}), encoding="utf-8")
    return path


def _payload_for_cluster_ids(clusters, cluster_ids):
    wanted = set(cluster_ids)
    payload = build_ai_payload_for_clusters(clusters)
    payload["clusters"] = [row for row in payload["clusters"] if row["cluster_id"] in wanted]
    return payload


def test_ai_repair_prompt_uses_only_authoritative_deterministic_cluster_ids(cli_env):
    cli, _ = cli_env
    clusters = _authoritative_repair_clusters()

    prompt = json.loads(cli.build_ai_repair_prompt(
        "RUN-9-REGRESSION", [clusters[1]], ["DISC-AAAA", "DISC-CCCC"], "gpt-5.4-mini",
    ))
    constraints = " ".join(prompt["constraints"])

    assert prompt["repair_cluster_ids"] == ["DISC-BBBB"]
    assert prompt["preserved_cluster_ids"] == ["DISC-AAAA", "DISC-CCCC"]
    assert [cluster["cluster_id"] for cluster in prompt["clusters"]] == ["DISC-BBBB"]
    assert "previous ai cluster_id values are untrusted" in prompt["instruction"].lower()
    assert "only authoritative identity source" in constraints.lower()
    assert "exactly one row per repair_cluster_id" in constraints
    assert "validation_errors" not in prompt
    assert "previous_valid_rows_advisory" not in prompt


def test_initial_repair_and_validator_share_authoritative_contract(cli_env, fixture_sources):
    cli, _ = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    initial = json.loads(cli.build_ai_prompt("CONTRACT-PARITY", clusters, "gpt-5.4-mini"))
    repair = json.loads(cli.build_ai_repair_prompt(
        "CONTRACT-PARITY", [clusters[0]], [], "gpt-5.4-mini",
    ))
    authoritative_fields = list(cli.AI_CLUSTER_REQUIRED_FIELDS)
    initial_contract = initial["required_output"]
    repair_contract = repair["required_output"]

    assert initial_contract["clusters_item_required"] == authoritative_fields
    assert repair_contract["clusters_item_required"] == authoritative_fields
    assert initial_contract == repair_contract == cli._ai_output_contract()
    assert initial_contract["classification_enum"] == repair_contract["classification_enum"] == sorted(cli.CLASSIFICATIONS)
    assert initial_contract["solvability_enum"] == repair_contract["solvability_enum"] == sorted(cli.SOLVABILITY_VALUES)
    assert initial_contract["confidence_enum"] == repair_contract["confidence_enum"] == sorted(cli.CONFIDENCE_VALUES)
    assert list(initial_contract["score_fields"]) == list(repair_contract["score_fields"]) == list(cli.AI_SCORE_FIELDS)
    assert [row["cluster_id"] for row in initial["output_rows_template"]] == [
        cluster["cluster_id"] for cluster in clusters
    ]
    assert all(set(row) == set(cli.AI_CLUSTER_REQUIRED_FIELDS) for row in initial["output_rows_template"])
    assert len(repair["repair_rows_template"]) == 1
    row_template = repair["repair_rows_template"][0]
    assert list(row_template) == authoritative_fields
    assert row_template["cluster_id"] == clusters[0]["cluster_id"]
    assert all(row_template[field] == 0 for field in cli.AI_SCORE_FIELDS)


def test_ai_repair_recovers_unknown_first_response_from_authoritative_ids(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    sources = _multi_cluster_repair_sources(tmp_path)
    clusters = _prepare_clusters(cli, sources)
    assert len(clusters) == 3
    expected_ids = [cluster["cluster_id"] for cluster in clusters]
    first_invalid = build_ai_payload_for_clusters(clusters)
    first_invalid["clusters"][0]["cluster_id"] = "DISC-WRONG"
    second_valid = _payload_for_cluster_ids(clusters, [expected_ids[0]])
    prompts = []
    governed_pairs = []
    original_apply_governance = cli.apply_governance

    def capture_governance(cluster, ai_row):
        governed_pairs.append((cluster["cluster_id"], ai_row["cluster_id"]))
        return original_apply_governance(cluster, ai_row)

    monkeypatch.setattr(cli, "apply_governance", capture_governance)
    report = _run_discovery_with_two_ai_payloads(
        cli, sources, monkeypatch, first_invalid, second_valid, prompts,
    )

    assert report["status"] in {"SUCCESS", "DEGRADED"}
    assert report["counts"]["AI_CALLS"] == 2
    assert prompts[1]["repair_cluster_ids"] == [expected_ids[0]]
    assert prompts[1]["preserved_cluster_ids"] == expected_ids[1:]
    assert [row["cluster_id"] for row in prompts[1]["clusters"]] == [expected_ids[0]]
    assert governed_pairs == [(cluster_id, cluster_id) for cluster_id in expected_ids]
    assert "DISC-WRONG" not in json.dumps(report)


def test_ai_repair_recovers_duplicate_first_response_from_authoritative_ids(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    sources = _multi_cluster_repair_sources(tmp_path)
    clusters = _prepare_clusters(cli, sources)
    expected_ids = [cluster["cluster_id"] for cluster in clusters]
    assert len(expected_ids) == 3
    first_invalid = build_ai_payload_for_clusters(clusters)
    first_invalid["clusters"][1]["cluster_id"] = expected_ids[0]
    second_valid = _payload_for_cluster_ids(clusters, expected_ids[:2])
    prompts = []

    report = _run_discovery_with_two_ai_payloads(
        cli, sources, monkeypatch, first_invalid, second_valid, prompts,
    )

    assert report["status"] in {"SUCCESS", "DEGRADED"}
    assert report["counts"]["AI_CALLS"] == 2
    assert prompts[1]["repair_cluster_ids"] == expected_ids[:2]
    assert prompts[1]["preserved_cluster_ids"] == [expected_ids[2]]


def test_ai_repair_regenerates_missing_first_response_from_authoritative_context(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    sources = _multi_cluster_repair_sources(tmp_path)
    clusters = _prepare_clusters(cli, sources)
    expected_ids = [cluster["cluster_id"] for cluster in clusters]
    assert len(expected_ids) == 3
    first_invalid = build_ai_payload_for_clusters(clusters)
    missing_id = first_invalid["clusters"].pop()["cluster_id"]
    second_valid = _payload_for_cluster_ids(clusters, [missing_id])
    prompts = []

    report = _run_discovery_with_two_ai_payloads(
        cli, sources, monkeypatch, first_invalid, second_valid, prompts,
    )

    assert report["status"] in {"SUCCESS", "DEGRADED"}
    assert report["counts"]["AI_CALLS"] == 2
    assert prompts[1]["repair_cluster_ids"] == [missing_id]
    assert prompts[1]["preserved_cluster_ids"] == expected_ids[:-1]
    assert missing_id in {cluster["cluster_id"] for cluster in prompts[1]["clusters"]}


def test_run9_shape_repairs_only_missing_sixth_row_in_authoritative_order(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    sources = _six_cluster_repair_sources(tmp_path)
    clusters = _prepare_clusters(cli, sources)
    expected_ids = [cluster["cluster_id"] for cluster in clusters]
    assert len(expected_ids) == 6
    first = _payload_for_cluster_ids(clusters, expected_ids[:5])
    second = _payload_for_cluster_ids(clusters, [expected_ids[5]])
    prompts = []
    governed_ids = []
    original_apply_governance = cli.apply_governance

    def capture_governance(cluster, ai_row):
        governed_ids.append(ai_row["cluster_id"])
        return original_apply_governance(cluster, ai_row)

    monkeypatch.setattr(cli, "apply_governance", capture_governance)
    report = _run_discovery_with_two_ai_payloads(cli, sources, monkeypatch, first, second, prompts)

    assert report["status"] in {"SUCCESS", "DEGRADED"}
    assert report["counts"]["AI_CALLS"] == 2
    assert prompts[1]["repair_cluster_ids"] == [expected_ids[5]]
    assert prompts[1]["preserved_cluster_ids"] == expected_ids[:5]
    assert [cluster["cluster_id"] for cluster in prompts[1]["clusters"]] == [expected_ids[5]]
    assert governed_ids == expected_ids


def test_run10_style_missing_fields_in_repair_fails_closed(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    sources = _six_cluster_repair_sources(tmp_path)
    clusters = _prepare_clusters(cli, sources)
    first = build_ai_payload_for_clusters(clusters)
    first["clusters"][0]["impact_score"] = 4.5
    first["clusters"][1]["impact_score"] = 4.5
    repair_ids = [clusters[0]["cluster_id"], clusters[1]["cluster_id"]]
    incomplete_repair = _payload_for_cluster_ids(clusters, repair_ids)
    missing_fields = {
        "candidate_recommendation", "classification", "confidence", "counterargument",
        "current_librecare_match", "solvability", "source_diversity_summary",
    }
    for row in incomplete_repair["clusters"]:
        for field in missing_fields:
            del row[field]

    report = _run_discovery_with_two_ai_payloads(
        cli, sources, monkeypatch, first, incomplete_repair,
    )

    assert report["status"] == "FAILED"
    assert report["counts"]["AI_CALLS"] == 2
    assert report["top10"] == []
    assert report["counts"]["OBSERVATIONS_CREATED"] == 0
    assert report["counts"]["PRODUCT_INBOX_ACTIONS"] == 0
    assert sum("missing fields" in error for error in report["errors"]) == 2
    assert all(any(field in error for error in report["errors"]) for field in missing_fields)


def test_run10_style_complete_repair_succeeds(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    sources = _six_cluster_repair_sources(tmp_path)
    clusters = _prepare_clusters(cli, sources)
    expected_ids = [cluster["cluster_id"] for cluster in clusters]
    first = build_ai_payload_for_clusters(clusters)
    first["clusters"][0]["impact_score"] = 4.5
    first["clusters"][1]["impact_score"] = 4.5
    prompts = []

    report = _run_discovery_with_two_ai_payloads(
        cli, sources, monkeypatch, first,
        _payload_for_cluster_ids(clusters, expected_ids[:2]), prompts,
    )

    assert report["status"] in {"SUCCESS", "DEGRADED"}
    assert report["counts"]["AI_CALLS"] == 2
    assert prompts[1]["repair_cluster_ids"] == expected_ids[:2]
    assert set(prompts[1]["required_output"]["clusters_item_required"]) == set(cli.AI_CLUSTER_REQUIRED_FIELDS)
    assert [row["cluster_id"] for row in prompts[1]["repair_rows_template"]] == expected_ids[:2]
    assert all(set(row) == set(cli.AI_CLUSTER_REQUIRED_FIELDS) for row in prompts[1]["repair_rows_template"])


def test_partial_repair_replaces_only_invalid_field_row(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    sources = _six_cluster_repair_sources(tmp_path)
    clusters = _prepare_clusters(cli, sources)
    expected_ids = [cluster["cluster_id"] for cluster in clusters]
    first = build_ai_payload_for_clusters(clusters)
    first["clusters"][3]["impact_score"] = 4.5
    second = _payload_for_cluster_ids(clusters, [expected_ids[3]])
    prompts = []

    report = _run_discovery_with_two_ai_payloads(cli, sources, monkeypatch, first, second, prompts)

    assert report["status"] in {"SUCCESS", "DEGRADED"}
    assert report["counts"]["AI_CALLS"] == 2
    assert prompts[1]["repair_cluster_ids"] == [expected_ids[3]]
    assert prompts[1]["preserved_cluster_ids"] == expected_ids[:3] + expected_ids[4:]


def test_extra_unknown_row_is_discarded_without_second_ai_call(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    sources = _multi_cluster_repair_sources(tmp_path)
    clusters = _prepare_clusters(cli, sources)
    expected_ids = [cluster["cluster_id"] for cluster in clusters]
    first = build_ai_payload_for_clusters(clusters)
    first["clusters"][0]["impact_score"] = "4"
    unknown = dict(first["clusters"][0])
    unknown["cluster_id"] = "DISC-WRONG"
    first["clusters"].append(unknown)
    calls = []

    def mock_run_copilot(prompt: str, model: str) -> str:
        calls.append(json.loads(prompt))
        return json.dumps(first)

    governed_rows = []
    original_apply_governance = cli.apply_governance
    monkeypatch.setattr(cli, "run_copilot_json", mock_run_copilot)
    monkeypatch.setattr(cli, "apply_governance", lambda cluster, row: (
        governed_rows.append((row["cluster_id"], row["impact_score"])), original_apply_governance(cluster, row)
    )[1])
    report = cli.run_discovery(
        sources_file=sources, max_items_per_source=20, publish_top3_flag=False,
        repo_owner="", repo_name="", github_token="", ai_mode="copilot",
        ai_model="gpt-5.4-mini", ai_response_file=None, timeout=2.0, retries=1,
        cache_max_age_seconds=3600,
    )

    assert report["status"] in {"SUCCESS", "DEGRADED"}
    assert report["counts"]["AI_CALLS"] == 1
    assert len(calls) == 1
    assert [cluster_id for cluster_id, _ in governed_rows] == expected_ids
    assert governed_rows[0][1] == 4
    assert isinstance(governed_rows[0][1], int)
    assert "DISC-WRONG" not in json.dumps(report)


def test_second_partial_response_returning_preserved_id_fails_closed(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    sources = _six_cluster_repair_sources(tmp_path)
    clusters = _prepare_clusters(cli, sources)
    expected_ids = [cluster["cluster_id"] for cluster in clusters]
    first = _payload_for_cluster_ids(clusters, expected_ids[:5])
    second = _payload_for_cluster_ids(clusters, [expected_ids[5], expected_ids[0]])

    report = _run_discovery_with_two_ai_payloads(cli, sources, monkeypatch, first, second)

    assert report["status"] == "FAILED"
    assert report["counts"]["AI_CALLS"] == 2
    assert report["top10"] == []
    assert report["counts"]["OBSERVATIONS_CREATED"] == 0
    assert report["counts"]["PRODUCT_INBOX_ACTIONS"] == 0
    assert any(f"unknown cluster_id: {expected_ids[0]}" in error for error in report["errors"])


def test_second_partial_response_missing_one_of_two_targets_fails_closed(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    sources = _six_cluster_repair_sources(tmp_path)
    clusters = _prepare_clusters(cli, sources)
    expected_ids = [cluster["cluster_id"] for cluster in clusters]
    first = _payload_for_cluster_ids(clusters, [
        expected_ids[0], expected_ids[1], expected_ids[2], expected_ids[4],
    ])
    second = _payload_for_cluster_ids(clusters, [expected_ids[3]])
    prompts = []

    report = _run_discovery_with_two_ai_payloads(cli, sources, monkeypatch, first, second, prompts)

    assert prompts[1]["repair_cluster_ids"] == [expected_ids[3], expected_ids[5]]
    assert report["status"] == "FAILED"
    assert report["counts"]["AI_CALLS"] == 2
    assert report["top10"] == []
    assert report["counts"]["OBSERVATIONS_CREATED"] == 0
    assert report["counts"]["PRODUCT_INBOX_ACTIONS"] == 0
    assert any("AI payload cluster count mismatch: expected 2, got 1" in error for error in report["errors"])
    assert any(expected_ids[5] in error and "missing clusters" in error for error in report["errors"])


def test_all_invalid_first_rows_repairs_all_clusters(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    sources = _multi_cluster_repair_sources(tmp_path)
    clusters = _prepare_clusters(cli, sources)
    expected_ids = [cluster["cluster_id"] for cluster in clusters]
    first = build_ai_payload_for_clusters(clusters)
    for row in first["clusters"]:
        row["confidence"] = "invalid"
    prompts = []

    report = _run_discovery_with_two_ai_payloads(
        cli, sources, monkeypatch, first, build_ai_payload_for_clusters(clusters), prompts,
    )

    assert report["status"] in {"SUCCESS", "DEGRADED"}
    assert report["counts"]["AI_CALLS"] == 2
    assert prompts[1]["repair_cluster_ids"] == expected_ids
    assert prompts[1]["preserved_cluster_ids"] == []


def test_ai_retry_on_first_validation_failure(cli_env, fixture_sources, monkeypatch):
    """If first AI payload is invalid, should retry once with repair prompt."""
    cli, root = cli_env

    clusters = _prepare_clusters(cli, fixture_sources)

    # First response: invalid (4.5 for impact_score)
    first_ai_payload = build_ai_payload_for_clusters(clusters)
    first_ai_payload["clusters"][0]["impact_score"] = 4.5  # Invalid

    # Second response: repaired
    second_ai_payload = _payload_for_cluster_ids(clusters, [clusters[0]["cluster_id"]])
    second_ai_payload["clusters"][0]["impact_score"] = 4  # Valid

    ai_responses = [
        json.dumps(first_ai_payload),
        json.dumps(second_ai_payload),
    ]

    ai_file = root / "ai-responses.txt"
    ai_file.write_text("\n".join(ai_responses), encoding="utf-8")

    # Mock multiple AI calls
    call_count = [0]

    def mock_run_copilot(prompt: str, model: str) -> str:
        nonlocal call_count
        call_count[0] += 1
        if call_count[0] == 1:
            return ai_responses[0]
        else:
            return ai_responses[1]

    # Monkey patch for this test
    monkeypatch.setattr(cli, "run_copilot_json", mock_run_copilot)

    # Need to use copilot mode to trigger retry
    report = cli.run_discovery(
        sources_file=fixture_sources,
        max_items_per_source=20,
        publish_top3_flag=False,
        repo_owner="",
        repo_name="",
        github_token="",
        ai_mode="copilot",
        ai_model="gpt-5.4-mini",
        ai_response_file=None,
        timeout=2.0,
        retries=1,
        cache_max_age_seconds=3600,
    )
    # Should succeed after retry
    assert report["status"] in {"SUCCESS", "DEGRADED"}
    # Should have made 2 AI calls
    assert report["counts"]["AI_CALLS"] == 2


def test_ai_fails_after_two_invalid_attempts(cli_env, fixture_sources, monkeypatch):
    """If both AI attempts are invalid, should fail closed."""
    cli, root = cli_env

    clusters = _prepare_clusters(cli, fixture_sources)

    # Both responses invalid
    invalid_payload = build_ai_payload_for_clusters(clusters)
    invalid_payload["clusters"][0]["impact_score"] = 4.5  # Invalid
    invalid_repair = _payload_for_cluster_ids(clusters, [clusters[0]["cluster_id"]])
    invalid_repair["clusters"][0]["impact_score"] = 4.5

    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(invalid_payload), encoding="utf-8")

    call_count = [0]

    def mock_run_copilot(prompt: str, model: str) -> str:
        nonlocal call_count
        call_count[0] += 1
        return json.dumps(invalid_payload if call_count[0] == 1 else invalid_repair)

    # Monkey patch
    monkeypatch.setattr(cli, "run_copilot_json", mock_run_copilot)

    # Use copilot mode
    report = cli.run_discovery(
        sources_file=fixture_sources,
        max_items_per_source=20,
        publish_top3_flag=False,
        repo_owner="",
        repo_name="",
        github_token="",
        ai_mode="copilot",
        ai_model="gpt-5.4-mini",
        ai_response_file=None,
        timeout=2.0,
        retries=1,
        cache_max_age_seconds=3600,
    )
    # Should fail
    assert report["status"] == "FAILED"
    # Should have exactly 2 AI calls
    assert report["counts"]["AI_CALLS"] == 2
    # Fail-closed contract: no observations/issues after invalid AI retries
    assert len(report["created_observations"]) == 0
    assert len(report["top3_issue_actions"]) == 0
    # Top10 should be empty or not eligible due to failed AI validation
    assert len(report["top10"]) == 0 or all(not item.get("eligibility") for item in report["top10"])


def test_ai_repair_preserves_cluster_ids(cli_env, fixture_sources, monkeypatch):
    """AI repair should not change cluster IDs."""
    cli, root = cli_env

    clusters = _prepare_clusters(cli, fixture_sources)

    first_invalid = build_ai_payload_for_clusters(clusters)
    first_invalid["clusters"][0]["impact_score"] = 4.5

    second_repaired = _payload_for_cluster_ids(clusters, [clusters[0]["cluster_id"]])

    ai_responses = [json.dumps(first_invalid), json.dumps(second_repaired)]

    ai_file = root / "ai.json"
    ai_file.write_text(ai_responses[0], encoding="utf-8")

    call_count = [0]

    def mock_run_copilot(prompt: str, model: str) -> str:
        nonlocal call_count
        call_count[0] += 1
        return ai_responses[min(call_count[0] - 1, 1)]

    # Monkey patch
    monkeypatch.setattr(cli, "run_copilot_json", mock_run_copilot)

    # Use copilot mode
    report = cli.run_discovery(
        sources_file=fixture_sources,
        max_items_per_source=20,
        publish_top3_flag=False,
        repo_owner="",
        repo_name="",
        github_token="",
        ai_mode="copilot",
        ai_model="gpt-5.4-mini",
        ai_response_file=None,
        timeout=2.0,
        retries=1,
        cache_max_age_seconds=3600,
    )
    # Original cluster IDs preserved
    for row in report["top10"]:
        assert row["cluster_id"].startswith("DISC-")


def test_ai_repair_with_unknown_cluster_id_fails_closed(cli_env, fixture_sources, monkeypatch):
    """Repair response with unknown cluster_id must fail closed."""
    cli, _ = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)

    first_invalid = build_ai_payload_for_clusters(clusters)
    first_invalid["clusters"][0]["cluster_id"] = "DISC-FIRST-WRONG"

    second_bad = _payload_for_cluster_ids(clusters, [clusters[0]["cluster_id"]])
    second_bad["clusters"][0]["cluster_id"] = "DISC-UNKNOWN-ID"

    report = _run_discovery_with_two_ai_payloads(cli, fixture_sources, monkeypatch, first_invalid, second_bad)
    assert report["status"] == "FAILED"
    assert report["counts"]["AI_CALLS"] == 2
    assert report["top10"] == []
    assert any("unknown cluster_id: DISC-UNKNOWN-ID" in error for error in report["errors"])
    assert len(report["created_observations"]) == 0
    assert len(report["top3_issue_actions"]) == 0


def test_ai_repair_with_missing_cluster_fails_closed(cli_env, fixture_sources, monkeypatch):
    """Repair response missing one expected cluster must fail closed."""
    cli, _ = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)

    first_invalid = build_ai_payload_for_clusters(clusters)
    first_invalid["clusters"][0]["impact_score"] = 4.5

    second_bad = {"clusters": []}

    report = _run_discovery_with_two_ai_payloads(cli, fixture_sources, monkeypatch, first_invalid, second_bad)
    assert report["status"] == "FAILED"
    assert report["counts"]["AI_CALLS"] == 2
    assert report["top10"] == []
    assert len(report["created_observations"]) == 0
    assert len(report["top3_issue_actions"]) == 0


def test_ai_repair_with_duplicate_additional_cluster_fails_closed(cli_env, fixture_sources, monkeypatch):
    """Repair response with duplicated/additional cluster row must fail closed."""
    cli, _ = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)

    first_invalid = build_ai_payload_for_clusters(clusters)
    first_invalid["clusters"][0]["impact_score"] = 4.5

    second_bad = _payload_for_cluster_ids(clusters, [clusters[0]["cluster_id"]])
    second_bad["clusters"].append(dict(second_bad["clusters"][0]))

    report = _run_discovery_with_two_ai_payloads(cli, fixture_sources, monkeypatch, first_invalid, second_bad)
    assert report["status"] == "FAILED"
    assert report["counts"]["AI_CALLS"] == 2
    assert report["top10"] == []
    assert len(report["created_observations"]) == 0
    assert len(report["top3_issue_actions"]) == 0


def test_workflow_artifact_upload_with_if_always():
    """Verify workflow uses if: always() for artifact upload."""
    workflow_path = WORKSPACE_ROOT / ".github" / "workflows" / "librecare-discovery.yml"
    text = workflow_path.read_text(encoding="utf-8")

    # Must have always() condition
    assert "if: always()" in text
    # Must have artifact upload step
    assert "Upload discovery artifacts" in text
    # Artifact upload should come after discovery
    artifact_idx = text.find("Upload discovery artifacts")
    discovery_idx = text.find("Run Discovery Agent")
    assert discovery_idx < artifact_idx


def test_workflow_preserves_bounded_paths_only():
    """Verify workflow only uploads bounded discovery paths."""
    workflow_path = WORKSPACE_ROOT / ".github" / "workflows" / "librecare-discovery.yml"
    text = workflow_path.read_text(encoding="utf-8")

    # Should upload generated discovery
    assert "product/generated/discovery/" in text
    # Should upload observations
    assert "product/research/observations/OBS-*.json" in text
    # Should not upload raw payloads, secrets, etc
    assert "REDDIT_CLIENT" not in text or "secrets.REDDIT_CLIENT" in text
    assert "raw" not in text.lower() or "raw_deploy" not in text.lower()


def test_workflow_dispatch_uses_selected_ref_and_blocks_branch_publication():
    workflow_path = WORKSPACE_ROOT / ".github" / "workflows" / "librecare-discovery.yml"
    text = workflow_path.read_text(encoding="utf-8")

    assert "ref: ${{ github.sha }}" in text
    assert not any(line.strip() == "ref: master" for line in text.splitlines())
    assert "github.ref_name != 'master' && inputs.publish_top3" in text
    assert "publish_top3=true is forbidden for non-master" in text
    assert "github.ref_name == 'master' && inputs.publish_top3" in text


def _v15_item(cli, text, source, family="github_community", language="en", concept="stale_or_missing_readings", role="developer_community"):
    return cli._normalize_item_fields(
        {
            "url": f"https://example.com/{source}", "text": text, "problem_statement": text,
            "source_identity": source, "language": language, "concept_id": concept,
            "topic_id": "data_freshness", "query_id": f"{concept}:{language}:1", "evidence_role": role,
        }, source, family, "community",
    )


def test_v15_official_marketing_titles_are_reference_noise(cli_env):
    cli, _ = cli_env
    for title in ["Dexcom Continuous Glucose Monitoring", "Dexcom Help Center", "Sugarmate"]:
        item = cli._normalize_item_fields({"url": "https://example.com/help", "text": title, "problem_statement": title,
            "evidence_role": "official_reference", "language": "en"}, "official", "official_vendor", "community")
        assert item["evidence_role"] == "official_reference"
        assert item["marketing_only"] is True
        assert item["eligible_for_clustering"] is False
        assert cli.cluster_items([item]) == []


def test_v15_github_user_impact_and_technical_only_filters(cli_env):
    cli, _ = cli_env
    positive = [
        "Missed readings should block alerts because users receive no warning",
        "Android update causes Libre alarms to stop working for users",
    ]
    negative = [
        "manifest receivers double-deliver OOP2 broadcast implementation internals",
        "SDK dependency maintenance and CI lint update",
    ]
    assert all(cli.assess_problem_signal(text, text, "developer_community")["user_facing"] for text in positive)
    assert all(cli.assess_problem_signal(text, text, "developer_community")["technical_only"] for text in negative)
    assert all(not cli.assess_problem_signal(text, text, "developer_community")["eligible_for_clustering"] for text in negative)


def test_v15_polish_and_english_language_concepts(cli_env):
    cli, _ = cli_env
    cases = [
        ("Po aktualizacji Androida przestały działać alarmy Libre", "pl", "alerts_not_firing"),
        ("LibreLinkUp nie pokazuje nowych danych opiekunowi", "pl", "glucose_sharing_delay_or_failure"),
        ("LibreLinkUp readings are delayed for caregiver", "en", "glucose_sharing_delay_or_failure"),
    ]
    for text, language, concept in cases:
        assert cli.detect_language(text) == language
        assert cli.infer_concept(text)[0] == concept


def test_v15_cross_language_concept_corroboration_and_counts(cli_env):
    cli, _ = cli_env
    pl = _v15_item(cli, "LibreLinkUp nie pokazuje nowych danych opiekunowi", "reddit_Polska", "reddit", "pl", "glucose_sharing_delay_or_failure", "user_community")
    en = _v15_item(cli, "LibreLinkUp readings are delayed for caregiver", "librelinkup_github", "github_community", "en", "glucose_sharing_delay_or_failure")
    clusters = cli.cluster_items(cli.dedupe_items([pl, en])[0])
    assert len(clusters) == 1
    cluster = clusters[0]
    assert cluster["language_count"] == 2
    assert cluster["independent_source_identity_count"] == 2
    assert cluster["independent_source_family_count"] == 2
    assert cluster["evidence_tier"] == "CORROBORATED"


def test_v15_source_identity_and_family_counts_are_independent(cli_env):
    cli, _ = cli_env
    one = _v15_item(cli, "Caregiver sees missing glucose readings and stale data", "xdrip_a")
    two = _v15_item(cli, "Caregiver has stale glucose data and missing readings", "juggluco_b")
    cluster = cli.cluster_items(cli.dedupe_items([one, two])[0])[0]
    assert cluster["independent_source_identity_count"] == 2
    assert cluster["independent_source_family_count"] == 1
    reddit = _v15_item(cli, "Caregiver reports stale glucose and missing readings", "reddit_libre", "reddit", role="user_community")
    cluster = cli.cluster_items(cli.dedupe_items([one, reddit])[0])[0]
    assert cluster["independent_source_identity_count"] == 2
    assert cluster["independent_source_family_count"] == 2


def test_v15_evidence_tiers_and_score_caps(cli_env):
    cli, _ = cli_env
    decision = {key: 5 for key in ["impact_score", "frequency_score", "evidence_score", "solvability_score", "novelty_score"]}
    decision["effort_score"] = 0
    base = {"persona_candidate": "caregiver", "independent_source_family_count": 1}
    for tier, cap in cli.EVIDENCE_SCORE_CAPS.items():
        assert cli.compute_score({**base, "evidence_tier": tier}, decision) <= cap
    assert cli.compute_score({**base, "evidence_tier": "WEAK"}, decision) == 49


def test_v15_top10_not_padded_and_weak_is_watchlist_only(cli_env):
    cli, _ = cli_env
    rows = []
    for idx in range(4):
        rows.append({"score": 70, "cluster": {"cluster_id": f"S{idx}", "quality_gate_passes": True, "evidence_tier": "SUPPORTED"}, "governed": {"classification": "PRODUCT_PROBLEM", "solvability": "APP"}})
    rows.append({"score": 49, "cluster": {"cluster_id": "W", "quality_gate_passes": True, "evidence_tier": "WEAK"}, "governed": {"classification": "PRODUCT_PROBLEM", "solvability": "APP", "eligible_for_inbox": False}})
    top, watch = cli.select_top10_and_watchlist(rows)
    assert len(top) == 4
    assert [row["cluster"]["cluster_id"] for row in watch] == ["W"]


def test_v15_observation_and_registry_quality_gates(cli_env):
    cli, root = cli_env
    canonical_key = "concept:stale_or_missing_readings|topic:data_freshness|facets:missing_data"
    canonical_fp = cli.hashlib.sha256(canonical_key.encode("utf-8")).hexdigest()[:16]
    weak = {"cluster_id": "DISC-W", "identity_ambiguous": False, "quality_gate_passes": True, "evidence_tier": "WEAK", "foundation_match": {},
        "fingerprints": ["w"], "raw_items": [{"source_type": "community"}], "source_urls": ["https://example/w"], "persona_candidate": "caregiver",
        "mode_candidate": "caregiver", "module_candidate": "Home", "evidence_items": ["weak"],
        "normalized_problem": "Glucose readings may be missing or fail to arrive.",
        "canonical_problem_statement": "Glucose readings may be missing or fail to arrive.",
        "canonical_problem_statement_pl": "Odczyty glukozy mogą być niedostępne lub nie docierać.",
        "canonical_grounding_safe": True, "canonical_problem_key": canonical_key,
        "canonical_problem_fingerprint": canonical_fp}
    supported = {**weak, "cluster_id": "DISC-S", "evidence_tier": "SUPPORTED", "fingerprints": ["s"]}
    assert cli.create_observations_from_clusters([weak], "RUN") == []
    assert len(cli.create_observations_from_clusters([supported], "RUN")) == 1
    _, summary, proposed = cli.resolve_cluster_ids_with_registry([dict(weak)], {"version": 1, "entries": []}, cli.utc_now())
    assert summary["new_entries"] == 0 and proposed["entries"] == []
    _, summary, proposed = cli.resolve_cluster_ids_with_registry([dict(supported)], {"version": 1, "entries": []}, cli.utc_now())
    assert summary["new_entries"] == 1 and len(proposed["entries"]) == 1


def test_v15_official_and_technical_clusters_cannot_create_observations(cli_env):
    cli, _ = cli_env
    for quality in [
        {"quality_gate_passes": False, "evidence_tier": "STRONG", "evidence_roles": ["official_reference"]},
        {"quality_gate_passes": False, "evidence_tier": "STRONG", "technical_only": True},
    ]:
        cluster = {"cluster_id": "DISC-N", "identity_ambiguous": False, "foundation_match": {}, "fingerprints": ["n"],
            "raw_items": [{"source_type": "community"}], "source_urls": ["https://example/n"], "persona_candidate": "caregiver", "module_candidate": "Home",
            "evidence_items": ["noise"], "normalized_problem": "noise", **quality}
        assert cli.create_observations_from_clusters([cluster], "RUN") == []


def test_v15_product_inbox_requires_correlated_quality(cli_env):
    cli, _ = cli_env
    ai = {"classification": "PRODUCT_PROBLEM", "solvability": "APP"}
    weak = cli.apply_governance({"foundation_match": {}, "quality_gate_passes": True, "evidence_tier": "WEAK"}, ai)
    supported = cli.apply_governance({"foundation_match": {}, "quality_gate_passes": True, "evidence_tier": "SUPPORTED"}, ai)
    corroborated = cli.apply_governance({"foundation_match": {}, "quality_gate_passes": True, "evidence_tier": "CORROBORATED"}, ai)
    assert weak["eligible_for_inbox"] is False
    assert supported["eligible_for_inbox"] is False
    assert corroborated["eligible_for_inbox"] is True


def test_v15_privacy_redaction(cli_env):
    cli, _ = cli_env
    text = "mail a@b.com IPv4 192.168.1.10 IPv6 2001:db8::1 @person +48 123 456 789 sensor SN-ABCDEF123456 token=abcdefghijklmnop Authorization: secretvalue123 Cookie: session123456"
    cleaned, redacted = cli.sanitize_text(text)
    assert redacted is True
    for secret in ["a@b.com", "192.168.1.10", "2001:db8::1", "@person", "123 456 789", "ABCDEF123456", "abcdefghijklmnop", "secretvalue123", "session123456"]:
        assert secret not in cleaned

    natural, natural_redacted = cli.sanitize_text("Sensor connection failed")
    assert natural == "Sensor connection failed"
    assert natural_redacted is False
    identifiers, identifiers_redacted = cli.sanitize_text("Sensor ID ABCDEF123456, device 12345678, and order ABCDEFGHIJKL")
    assert identifiers_redacted is True
    assert "ABCDEF123456" not in identifiers
    assert "12345678" not in identifiers
    assert "ABCDEFGHIJKL" not in identifiers


def test_v15_bounded_query_schedule_covers_all_concepts(cli_env):
    cli, _ = cli_env
    packs = cli.load_query_packs(WORKSPACE_ROOT / "product" / "discovery" / "query-packs.json")
    rows = list(cli.iter_queries(packs, ["pl", "en", "de", "fr", "es"], ["pl", "en"], max_queries=48))
    assert {row["concept_id"] for row in rows} == {concept["concept_id"] for concept in packs["concepts"]}
    primary_count = sum(row["language"] in {"pl", "en"} for row in rows)
    secondary_count = len(rows) - primary_count
    assert primary_count > secondary_count


def test_v15_reddit_query_search_carries_language_and_concept(cli_env, monkeypatch):
    cli, _ = cli_env
    monkeypatch.setenv("REDDIT_CLIENT_ID", "id")
    monkeypatch.setenv("REDDIT_CLIENT_SECRET", "secret")
    monkeypatch.setattr(cli, "_post_form_json", lambda *a, **k: {"access_token": "temporary"})
    urls = []
    def fake_fetch(url, timeout, retries, headers=None):
        urls.append(url)
        return {"data": {"children": [{"data": {"title": "LibreLinkUp delayed readings for caregiver", "selftext": "Missing current data", "permalink": "/r/diabetes/comments/x/y", "created_utc": time.time()}}]}}
    monkeypatch.setattr(cli, "_fetch_json", fake_fetch)
    packs = {"concepts": [{"concept_id": "glucose_sharing_delay_or_failure", "topic_id": "caregiver", "queries": {"en": ["LibreLinkUp delayed readings"]}}]}
    result = cli._collect_reddit_oauth({"name": "reddit", "family": "reddit", "subreddits": ["diabetes"], "max_queries": 1}, 1, 1, 1, cli.DiscoveryCache(Path("unused.json")), 1, packs, ["en"], 365)
    assert "/search?" in urls[0] and "/new?" not in urls[0]
    assert result.items[0]["language"] == "en"
    assert result.items[0]["concept_id"] == "glucose_sharing_delay_or_failure"


def test_v15_github_search_bounds_and_no_pull_requests(cli_env, monkeypatch):
    cli, _ = cli_env
    urls = []
    def fake_fetch(url, timeout, retries, headers=None):
        urls.append(url)
        return [{"html_url": f"https://github.com/o/r/pull/{idx}", "title": "Libre no readings", "body": "user missing data",
                 "updated_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"), "pull_request": {}} for idx in range(100)]
    monkeypatch.setattr(cli, "_fetch_json", fake_fetch)
    packs = {"concepts": [{"concept_id": f"concept_{idx}", "topic_id": "topic", "queries": {"en": [f"query {idx}"]}} for idx in range(40)]}
    items = cli._collect_github_issue_search({"name": "gh", "family": "github_community", "repos": ["o/r"], "max_queries": 48, "max_pages": 9}, 30, 1, 1, cli.DiscoveryCache(Path("unused.json")), 1, packs, ["en"], 365)
    assert len(urls) == 2
    assert all("/repos/o/r/issues?" in url and "/search/issues" not in url for url in urls)
    assert items == []


def test_v15_github_per_concept_and_source_limits(cli_env, monkeypatch):
    cli, _ = cli_env
    def fake_fetch(url, timeout, retries, headers=None):
        return [{"html_url": f"https://github.com/o/r/issues/{idx}", "title": f"Libre missing readings {idx}", "body": "Users have missing glucose data",
                 "updated_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")} for idx in range(20)]
    monkeypatch.setattr(cli, "_fetch_json", fake_fetch)
    packs = {"languages": {"en": {"priority": 1.0}}, "concepts": [{"concept_id": "stale_or_missing_readings", "topic_id": "data_freshness", "queries": {"en": ["Libre no readings"]}}]}
    items = cli._collect_github_issue_search({"name": "gh", "family": "github_community", "repos": ["o/r"], "max_queries": 1, "max_pages": 2, "max_results_per_query": 10}, 7, 1, 1, cli.DiscoveryCache(Path("unused.json")), 1, packs, ["en"], 365)
    assert len(items) <= 7


def test_v15_total_run_budget_enforced(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    monkeypatch.setattr(cli, "MAX_TOTAL_COLLECTED", 5)
    sources = {"sources": []}
    for source_idx in range(2):
        sources["sources"].append({"name": f"source_{source_idx}", "family": "reddit", "evidence_role": "user_community", "fixture_items": [
            {"url": f"https://example.com/{source_idx}/{idx}", "text": f"Caregiver missing glucose readings number {idx}", "problem_statement": f"Caregiver missing glucose readings number {idx}", "source_identity": f"s{source_idx}-{idx}"}
            for idx in range(10)
        ]})
    path = tmp_path / "bounded-sources.json"
    path.write_text(json.dumps(sources), encoding="utf-8")
    report = cli.run_discovery(path, 30, False, "", "", "", "heuristic", "gpt-5.4-mini", None, 1, 1, 1)
    assert report["counts"]["COLLECTED"] <= 5


def test_v15_query_pack_and_ai_safety_contract(cli_env):
    cli, _ = cli_env
    packs = cli.load_query_packs(WORKSPACE_ROOT / "product" / "discovery" / "query-packs.json")
    assert len(packs["concepts"]) >= 12
    serialized = json.dumps(packs).lower()
    for forbidden in ["bolus calculator", "basal recommendation", "insulin ratio", "dosing"]:
        assert forbidden not in serialized
    cluster = {"cluster_id": "DISC-X", "normalized_problem": "Missing readings", "persona_candidate": "caregiver", "module_candidate": "Home",
        "independent_source_family_count": 2, "independent_source_identity_count": 2, "evidence_item_count": 2, "languages": ["pl", "en"],
        "evidence_tier": "CORROBORATED", "concept_id": "stale_or_missing_readings", "topic_id": "data_freshness", "source_families": ["reddit", "github_community"],
        "source_identities": ["a", "b"], "evidence_items": ["x"], "source_urls": ["https://example/x"], "foundation_match": {}}
    prompt = cli.build_ai_prompt("RUN", [cluster], "gpt-5.4-mini")
    assert "problem_statement_pl" in prompt
    assert "Do not accept requirements" in prompt
    assert "Never recommend insulin" in prompt


def test_v15_ai_requires_polish_problem_statement(cli_env):
    cli, _ = cli_env
    clusters = [{"cluster_id": "DISC-X"}]
    payload = {"clusters": [{"cluster_id": "DISC-X", "classification": "PRODUCT_PROBLEM", "persona": "caregiver", "problem_statement": "Missing readings",
        "evidence_summary": "e", "source_diversity_summary": "s", "current_librecare_match": "m", "solvability": "APP",
        "impact_score": 1, "frequency_score": 1, "evidence_score": 1, "solvability_score": 1, "novelty_score": 1, "effort_score": 1,
        "confidence": "medium", "counterargument": "c", "candidate_recommendation": "review"}]}
    valid, errors = cli.validate_ai_output(payload, clusters)
    assert valid is False
    assert any("problem_statement_pl" in error for error in errors)


def test_v15_sources_have_explicit_roles_and_no_unapproved_sites():
    config = json.loads((WORKSPACE_ROOT / "product" / "discovery" / "sources.json").read_text(encoding="utf-8"))
    assert all(source.get("evidence_role") in {"user_community", "developer_community", "official_reference"} for source in config["sources"])
    serialized = json.dumps(config).lower()
    assert "mojacukrzyca" not in serialized and "facebook" not in serialized
    assert all(source.get("kind") != "rss_atom" or source.get("allowlisted") is True for source in config["sources"])


def test_discovery_github_source_expansion_contract():
    config = json.loads((WORKSPACE_ROOT / "product" / "discovery" / "sources.json").read_text(encoding="utf-8"))
    sources = config["sources"]
    by_name = {source["name"]: source for source in sources}
    assert len(by_name) == len(sources)

    existing = {
        "nightscout_github": "nightscout/cgm-remote-monitor",
        "xdrip_github": "NightscoutFoundation/xDrip",
        "juggluco_github": "j-kaltes/Juggluco",
        "librelinkup_github": "timoschlueter/nightscout-librelink-up",
    }
    approved = {
        "librelinkup_desktop_github": "Crazy-Marvin/LibreLinkUpDesktop",
        "glucosedirect_github": "creepymonster/GlucoseDirect",
        "glucodataauto_github": "pachi81/GlucoDataAuto",
    }
    assert {name: by_name[name]["repos"][0] for name in existing} == existing
    assert {name: by_name[name]["repos"][0] for name in approved} == approved

    github_sources = [source for source in sources if source.get("family") == "github_community"]
    assert len(github_sources) == 7
    assert sum(len(source["repos"]) * source["max_pages"] for source in github_sources) == 14
    for name, repo in approved.items():
        source = by_name[name]
        assert source["family"] == "github_community"
        assert source["kind"] == "github_issue_search"
        assert source["evidence_role"] == "developer_community"
        assert source["enabled"] is True
        assert source["repos"] == [repo]
        assert source["max_queries"] == 24
        assert source["max_pages"] <= 2
        assert source["max_results_per_query"] == 10
        assert source["urls"] == [f"https://github.com/{repo}/issues"]

    assert "gillesvs/librelink" not in {repo for source in github_sources for repo in source["repos"]}
    assert by_name["reddit_cgm_communities"] == {
        "name": "reddit_cgm_communities",
        "family": "reddit",
        "kind": "reddit_oauth",
        "evidence_role": "user_community",
        "enabled": True,
        "max_queries": 24,
        "subreddits": ["diabetes", "Type1Diabetes", "Freestylelibre", "dexcom", "Polska", "poland"],
        "urls": [],
    }

    packs = json.loads((WORKSPACE_ROOT / "product" / "discovery" / "query-packs.json").read_text(encoding="utf-8"))
    assert {concept["concept_id"] for concept in packs["concepts"]} == {
        "stale_or_missing_readings", "alerts_not_firing", "false_or_repeated_alerts", "signal_loss_disconnect",
        "caregiver_remote_monitoring", "glucose_sharing_delay_or_failure", "sensor_activation_connection_failure",
        "sensor_expiry_notification", "phone_os_compatibility", "watch_widget_glanceability",
        "history_reports_statistics", "notification_customization",
    }
    serialized_packs = json.dumps(packs).lower()
    for forbidden in ["dose", "dosing", "bolus recommendation", "basal recommendation", "insulin calculation", "insulin therapy recommendation"]:
        assert forbidden not in serialized_packs


def test_v15_cached_github_excerpt_preserves_sanitized_bounded_body(cli_env):
    cli, _ = cli_env
    title = "Libre alarms stop after Android update"
    body = "Caregiver receives no warning at user@example.com from 2001:db8::1. " + ("Alarm impact continues. " * 30)
    item = cli._normalize_cached_item(
        {"url": "https://github.com/o/r/issues/1", "text": f"{title}. {body}", "problem_statement": title,
         "language": "en", "concept_id": "alerts_not_firing", "updated_at": cli.utc_now()},
        "gh", "github_community", "community",
    )
    assert item["problem_statement"] == title
    assert "Caregiver receives no warning" in item["excerpt"]
    assert item["excerpt"] != title
    assert len(item["excerpt"]) <= cli.MAX_CACHED_EXCERPT_LENGTH
    assert "user@example.com" not in item["excerpt"]
    assert "2001:db8::1" not in item["excerpt"]
    assert item["privacy_redacted"] is True


def test_v15_cached_reddit_excerpt_preserves_selftext_without_identity_or_secrets(cli_env):
    cli, _ = cli_env
    title = "LibreLinkUp readings are delayed for caregiver"
    selftext = "My caregiver sees readings twenty minutes late. @private_user Cookie: session-secret-value"
    item = cli._normalize_cached_item(
        {"url": "https://reddit.com/r/diabetes/comments/1/x", "text": f"{title}. {selftext}", "problem_statement": title,
         "language": "en", "concept_id": "glucose_sharing_delay_or_failure", "updated_at": time.time()},
        "reddit", "reddit", "community",
    )
    assert "twenty minutes late" in item["excerpt"]
    assert "@private_user" not in item["excerpt"]
    assert "session-secret-value" not in item["excerpt"]
    assert "selftext" not in item and "author" not in item and "username" not in item
    assert len(item["excerpt"]) <= cli.MAX_CACHED_EXCERPT_LENGTH


def test_v15_cross_language_requires_shared_problem_intent(cli_env):
    cli, _ = cli_env
    compatible_pl = _v15_item(cli, "LibreLinkUp nie pokazuje nowych danych opiekunowi", "reddit_Polska", "reddit", "pl", "glucose_sharing_delay_or_failure", "user_community")
    compatible_en = _v15_item(cli, "LibreLinkUp readings are delayed for caregiver", "github_en", "github_community", "en", "glucose_sharing_delay_or_failure")
    compatible = cli.cluster_items(cli.dedupe_items([compatible_pl, compatible_en])[0])
    assert len(compatible) == 1
    assert "caregiver_visibility" in cli.problem_intent_facets(compatible_pl["problem_statement"], "pl", compatible_pl["concept_id"])

    unrelated_pl = _v15_item(cli, "Chcę łatwiej dodać kolejnego opiekuna", "reddit_other", "reddit", "pl", "glucose_sharing_delay_or_failure", "user_community")
    separated = cli.cluster_items(cli.dedupe_items([unrelated_pl, compatible_en])[0])
    assert len(separated) == 2
    assert all(cluster["independent_source_identity_count"] == 1 for cluster in separated)
    assert all(cluster["independent_source_family_count"] == 1 for cluster in separated)
    assert all(cluster["language_count"] == 1 for cluster in separated)
    assert all(cluster["evidence_tier"] == "WEAK" for cluster in separated)


def test_v15_different_languages_same_concept_without_facets_stay_separate(cli_env):
    cli, _ = cli_env
    pl = _v15_item(cli, "Wygodniejszy ekran dla rodziny", "pl_source", "reddit", "pl", "glucose_sharing_delay_or_failure", "user_community")
    en = _v15_item(cli, "Simpler invitation flow for relatives", "en_source", "github_community", "en", "glucose_sharing_delay_or_failure")
    assert len(cli.cluster_items(cli.dedupe_items([pl, en])[0])) == 2


def _single_query_pack():
    return {"languages": {"en": {"priority": 1.0}}, "concepts": [{"concept_id": "stale_or_missing_readings", "topic_id": "data_freshness", "queries": {"en": ["Libre missing readings"]}}]}


def test_v15_github_cache_is_window_aware_and_rechecks_cached_timestamp(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    cache = cli.DiscoveryCache(tmp_path / "github-cache.json")
    stale_at = (datetime.now(timezone.utc) - timedelta(days=120)).isoformat().replace("+00:00", "Z")
    fresh_at = (datetime.now(timezone.utc) - timedelta(days=5)).isoformat().replace("+00:00", "Z")
    response_time = [stale_at]
    monkeypatch.setattr(cli, "_fetch_json", lambda *a, **k: [{"html_url": "https://github.com/o/r/issues/1", "title": "Libre missing readings", "body": "Caregiver has no current data", "updated_at": response_time[0]}])
    source = {"name": "gh", "family": "github_community", "repos": ["o/r"], "max_queries": 1, "max_pages": 1}
    old_window = cli._collect_github_issue_search(source, 5, 1, 1, cache, 21600, _single_query_pack(), ["en"], 365, primary_languages=["en"])
    assert len(old_window) == 1
    old_key = next(iter(cache._data["entries"]))
    current_key = old_key.replace("lookback-days:365", "lookback-days:30")
    cache._data["entries"][current_key] = cache._data["entries"][old_key]
    monkeypatch.setattr(cli, "_fetch_json", lambda *a, **k: pytest.fail("cached hit must not fetch"))
    assert cli._collect_github_issue_search(source, 5, 1, 1, cache, 21600, _single_query_pack(), ["en"], 30, primary_languages=["en"]) == []
    assert "token" not in old_key.lower() and "authorization" not in old_key.lower()

    response_time[0] = fresh_at
    fresh_cache = cli.DiscoveryCache(tmp_path / "github-fresh-cache.json")
    monkeypatch.setattr(cli, "_fetch_json", lambda *a, **k: [{"html_url": "https://github.com/o/r/issues/2", "title": "Libre missing readings", "body": "Caregiver has no current data", "updated_at": fresh_at}])
    assert len(cli._collect_github_issue_search(source, 5, 1, 1, fresh_cache, 21600, _single_query_pack(), ["en"], 30, primary_languages=["en"])) == 1
    monkeypatch.setattr(cli, "_fetch_json", lambda *a, **k: pytest.fail("fresh cached hit must not fetch"))
    assert len(cli._collect_github_issue_search(source, 5, 1, 1, fresh_cache, 21600, _single_query_pack(), ["en"], 30, primary_languages=["en"])) == 1
    assert cli._timestamp_at_or_after("", datetime.now(timezone.utc) - timedelta(days=30)) is False


def test_v15_reddit_cache_is_window_aware_and_rechecks_cached_timestamp(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    monkeypatch.setenv("REDDIT_CLIENT_ID", "id")
    monkeypatch.setenv("REDDIT_CLIENT_SECRET", "secret")
    monkeypatch.setattr(cli, "_post_form_json", lambda *a, **k: {"access_token": "temporary-token"})
    cache = cli.DiscoveryCache(tmp_path / "reddit-cache.json")
    created = [time.time() - 120 * 86400]
    monkeypatch.setattr(cli, "_fetch_json", lambda *a, **k: {"data": {"children": [{"data": {"title": "Libre missing readings", "selftext": "Caregiver has no current data", "permalink": "/r/diabetes/comments/1/x", "created_utc": created[0]}}]}})
    source = {"name": "reddit", "family": "reddit", "subreddits": ["diabetes"], "max_queries": 1}
    old_window = cli._collect_reddit_oauth(source, 5, 1, 1, cache, 21600, _single_query_pack(), ["en"], 365, ["en"])
    assert len(old_window.items) == 1
    old_key = next(iter(cache._data["entries"]))
    current_key = old_key.replace("lookback-days:365", "lookback-days:30")
    cache._data["entries"][current_key] = cache._data["entries"][old_key]
    monkeypatch.setattr(cli, "_fetch_json", lambda *a, **k: pytest.fail("cached hit must not fetch"))
    assert cli._collect_reddit_oauth(source, 5, 1, 1, cache, 21600, _single_query_pack(), ["en"], 30, ["en"]).items == []
    assert "token" not in old_key.lower() and "authorization" not in old_key.lower()

    created[0] = time.time() - 5 * 86400
    fresh_cache = cli.DiscoveryCache(tmp_path / "reddit-fresh-cache.json")
    monkeypatch.setattr(cli, "_fetch_json", lambda *a, **k: {"data": {"children": [{"data": {"title": "Libre missing readings", "selftext": "Caregiver has no current data", "permalink": "/r/diabetes/comments/2/y", "created_utc": created[0]}}]}})
    assert len(cli._collect_reddit_oauth(source, 5, 1, 1, fresh_cache, 21600, _single_query_pack(), ["en"], 30, ["en"]).items) == 1
    monkeypatch.setattr(cli, "_fetch_json", lambda *a, **k: pytest.fail("fresh cached hit must not fetch"))
    assert len(cli._collect_reddit_oauth(source, 5, 1, 1, fresh_cache, 21600, _single_query_pack(), ["en"], 30, ["en"]).items) == 1


def test_v15_runtime_primary_languages_are_authoritative_and_bounded(cli_env):
    cli, _ = cli_env
    packs = cli.load_query_packs(WORKSPACE_ROOT / "product" / "discovery" / "query-packs.json")
    default_rows = list(cli.iter_queries(packs, ["pl", "en", "de"], ["pl", "en"], 48))
    assert sum(row["language"] in {"pl", "en"} for row in default_rows) > sum(row["language"] == "de" for row in default_rows)
    pl_primary = list(cli.iter_queries(packs, ["pl", "en", "de"], ["pl"], 48))
    assert sum(row["language"] == "pl" for row in pl_primary) > sum(row["language"] == "en" for row in pl_primary)
    assert len([row for row in pl_primary if row["language"] != "pl"]) <= 48 // 4
    invalid_ignored = list(cli.iter_queries(packs, ["pl", "en", "de"], ["pl", "xx"], 48))
    assert all(row["language"] in {"pl", "en", "de"} for row in invalid_ignored)
    no_primary = list(cli.iter_queries(packs, ["pl", "en", "de"], [], 48))
    assert len(no_primary) <= 48 // 4
    implicit_default = list(cli.iter_queries(packs, ["pl", "en", "de"], max_queries=48))
    assert sum(row["language"] in {"pl", "en"} for row in implicit_default) > sum(row["language"] == "de" for row in implicit_default)


def test_v15_source_reporting_separates_requested_and_observed_languages(cli_env, tmp_path):
    cli, root = cli_env
    sources = {"sources": [
        {"name": "reddit_disabled", "family": "reddit", "kind": "reddit_oauth", "enabled": True},
        {"name": "polish_fixture", "family": "reddit", "kind": "reddit_oauth", "enabled": True, "fixture_items": [
            {"url": "https://example.com/pl", "text": "Brak danych dla opiekuna", "problem_statement": "Brak danych dla opiekuna", "language": "pl"}
        ]},
        {"name": "github_empty", "family": "github_community", "kind": "github_issue_search", "enabled": False},
    ]}
    path = tmp_path / "language-sources.json"
    path.write_text(json.dumps(sources), encoding="utf-8")
    report = cli.run_discovery(path, 5, False, "", "", "", "heuristic", "gpt-5.4-mini", None, 1, 1, 3600,
                               languages=["pl", "en", "de"], primary_languages=["pl", "en"])
    by_name = {row["name"]: row for row in report["source_status"]}
    assert by_name["reddit_disabled"]["requested_languages"] == ["pl", "en", "de"]
    assert by_name["reddit_disabled"]["observed_languages"] == []
    assert by_name["polish_fixture"]["observed_languages"] == ["pl"]
    assert by_name["github_empty"]["requested_languages"] == ["pl", "en", "de"]
    assert by_name["github_empty"]["observed_languages"] == []
    markdown = (root / report["md_report_path"]).read_text(encoding="utf-8")
    assert "requested_languages=pl,en,de" in markdown
    assert "observed_languages=-" in markdown


def test_github_public_collection_never_attaches_workflow_token(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    seen_headers = []

    def fake_fetch(url, timeout, retries, headers=None):
        seen_headers.append(dict(headers or {}))
        return []

    monkeypatch.setattr(cli, "_fetch_json", fake_fetch)
    result = cli.collect_from_source(
        {"name": "external_github", "family": "github_community", "kind": "github_issue_search", "repos": ["other/public"]},
        10, 1, 1, cache=cli.DiscoveryCache(tmp_path / "cache.json"), query_packs=_single_query_pack(), languages=["en"],
        lookback_days=30, github_token="workflow-secret-token", primary_languages=["en"],
    )
    assert result.status == "EMPTY"
    assert seen_headers
    assert all("Authorization" not in headers for headers in seen_headers)
    assert "workflow-secret-token" not in json.dumps(seen_headers)


def test_github_local_matching_assigns_one_best_concept_for_real_phrasing(cli_env):
    cli, _ = cli_env
    packs = cli.load_query_packs(WORKSPACE_ROOT / "product" / "discovery" / "query-packs.json")
    rows = cli.bounded_query_rows(packs, ["pl", "en", "de", "fr", "es"], 48, ["pl", "en"])
    cases = [
        ("LibreLinkUp readings missing for caregiver", "glucose_sharing_delay_or_failure"),
        ("Fsl2 PL Motorola is loosing connection with sensor", "signal_loss_disconnect"),
    ]
    for idx, (title, expected_concept) in enumerate(cases, start=1):
        candidate = cli._github_issue_candidate(
            {"html_url": f"https://github.com/o/r/issues/{idx}", "title": title, "body": "The problem repeats for users.", "updated_at": cli.utc_now()},
            "o/r",
        )
        match = cli._local_github_query_match(candidate, rows)
        assert match is not None
        assert match["concept_id"] == expected_concept


def test_github_local_matching_recalls_real_discovery_run_5_issues(cli_env):
    cli, _ = cli_env
    packs = cli.load_query_packs(WORKSPACE_ROOT / "product" / "discovery" / "query-packs.json")
    rows = cli.bounded_query_rows(packs, ["pl", "en", "de", "fr", "es"], 48, ["pl", "en"])
    cases = [
        ("Crazy-Marvin/LibreLinkUpDesktop", "No Error message if connection to Sensor is lost", "signal_loss_disconnect"),
        ("pachi81/GlucoDataAuto", "Blood glucose values aren't updating when listening to a song", "stale_or_missing_readings"),
        ("pachi81/GlucoDataAuto", "Not receiving glucose values", "stale_or_missing_readings"),
        ("creepymonster/GlucoseDirect", "Unsupported sensor - Libre 2", "sensor_activation_connection_failure"),
    ]
    for idx, (repo, title, expected_concept) in enumerate(cases, start=10):
        candidate = cli._github_issue_candidate(
            {"html_url": f"https://github.com/{repo}/issues/{idx}", "title": title, "body": "", "updated_at": cli.utc_now()},
            repo,
        )
        match = cli._local_github_query_match(candidate, rows)
        assert match is not None, title
        assert match["concept_id"] == expected_concept, title
        assert match["match_score"] >= cli.LOCAL_QUERY_MATCH_THRESHOLD
        signal = cli.assess_problem_signal(candidate["excerpt"], candidate["problem_statement"], "developer_community")
        assert signal["user_facing"] is True, title
        assert signal["eligible_for_clustering"] is True, title


def test_github_local_matching_requires_concept_context_for_generic_phrases(cli_env):
    cli, _ = cli_env
    packs = cli.load_query_packs(WORKSPACE_ROOT / "product" / "discovery" / "query-packs.json")
    rows = cli.bounded_query_rows(packs, ["pl", "en", "de", "fr", "es"], 48, ["pl", "en"])
    unmatched = [
        "not updating",
        "missing data",
        "connection",
        "Android Auto display widget broken",
    ]
    for idx, title in enumerate(unmatched, start=40):
        candidate = cli._github_issue_candidate(
            {"html_url": f"https://github.com/o/r/issues/{idx}", "title": title, "body": "", "updated_at": cli.utc_now()},
            "o/r",
        )
        assert cli._local_github_query_match(candidate, rows) is None, title

    candidate = cli._github_issue_candidate(
        {"html_url": "https://github.com/o/r/issues/50", "title": "Blood glucose values stopped updating", "body": "", "updated_at": cli.utc_now()},
        "o/r",
    )
    match = cli._local_github_query_match(candidate, rows)
    assert match is not None
    assert match["concept_id"] == "stale_or_missing_readings"
    assert match["concept_id"] != "glucose_sharing_delay_or_failure"


def test_github_local_matching_keeps_technical_filter_and_ambiguity_guard(cli_env, monkeypatch):
    cli, _ = cli_env
    packs = cli.load_query_packs(WORKSPACE_ROOT / "product" / "discovery" / "query-packs.json")
    rows = cli.bounded_query_rows(packs, ["pl", "en"], 48, ["pl", "en"])
    for idx, title in enumerate(["Enhancement: Git Actions for Browser Build", "Refactor API deployment pipeline"], start=60):
        candidate = cli._github_issue_candidate(
            {"html_url": f"https://github.com/o/r/issues/{idx}", "title": title, "body": "", "updated_at": cli.utc_now()},
            "o/r",
        )
        assert cli._local_github_query_match(candidate, rows) is None, title

    monkeypatch.setattr(cli, "CONCEPT_INTENT_FACETS", {**cli.CONCEPT_INTENT_FACETS, "test_a": {"missing_data"}, "test_b": {"missing_data"}})
    ambiguous_rows = [
        {"concept_id": "test_a", "topic_id": "test", "query_id": "test-a:en:1", "query": "Libre missing readings", "language": "en"},
        {"concept_id": "test_b", "topic_id": "test", "query_id": "test-b:en:1", "query": "Libre missing readings", "language": "en"},
    ]
    ambiguous = cli._github_issue_candidate(
        {"html_url": "https://github.com/o/r/issues/70", "title": "Libre missing readings", "body": "", "updated_at": cli.utc_now()},
        "o/r",
    )
    assert cli._local_github_query_match(ambiguous, ambiguous_rows) is None


def test_github_local_matching_supports_required_problem_intent_variants(cli_env):
    cli, _ = cli_env
    packs = cli.load_query_packs(WORKSPACE_ROOT / "product" / "discovery" / "query-packs.json")
    rows = cli.bounded_query_rows(packs, ["pl", "en"], 48, ["pl", "en"])
    cases = [
        ("Libre is losing connection", "signal_loss_disconnect"),
        ("Libre is loosing connection", "signal_loss_disconnect"),
        ("Libre lost connection", "signal_loss_disconnect"),
        ("Libre connection to sensor is lost", "signal_loss_disconnect"),
        ("Libre connection drops", "signal_loss_disconnect"),
        ("Libre disconnects", "signal_loss_disconnect"),
        ("Libre missing readings", "stale_or_missing_readings"),
        ("Libre no readings", "stale_or_missing_readings"),
        ("Libre delayed readings", "stale_or_missing_readings"),
        ("Libre glucose values not updating", "stale_or_missing_readings"),
        ("Libre glucose values isn't updating", "stale_or_missing_readings"),
        ("Libre readings aren't updating", "stale_or_missing_readings"),
        ("Libre readings stop updating", "stale_or_missing_readings"),
        ("Libre readings stopped updating", "stale_or_missing_readings"),
        ("Libre not receiving glucose values", "stale_or_missing_readings"),
        ("Libre no new value", "stale_or_missing_readings"),
        ("Libre no new reading", "stale_or_missing_readings"),
        ("Libre frozen reading", "stale_or_missing_readings"),
        ("Libre last received value remains", "stale_or_missing_readings"),
        ("Unsupported sensor Libre 2", "sensor_activation_connection_failure"),
        ("Sensor not recognized", "sensor_activation_connection_failure"),
        ("Unrecognized sensor", "sensor_activation_connection_failure"),
        ("Sensor connection failed", "sensor_activation_connection_failure"),
        ("Sensor cannot connect", "sensor_activation_connection_failure"),
        ("Unable to connect sensor", "sensor_activation_connection_failure"),
        ("Libre alarm stopped working", "alerts_not_firing"),
        ("Libre false alarm", "false_or_repeated_alerts"),
    ]
    for idx, (title, expected_concept) in enumerate(cases, start=20):
        candidate = cli._github_issue_candidate(
            {"html_url": f"https://github.com/o/r/issues/{idx}", "title": title, "body": "User-facing problem.", "updated_at": cli.utc_now()},
            "o/r",
        )
        match = cli._local_github_query_match(candidate, rows)
        assert match is not None, title
        assert match["concept_id"] == expected_concept, title


def test_github_local_matching_skips_technical_and_ambiguous_noise(cli_env):
    cli, _ = cli_env
    packs = cli.load_query_packs(WORKSPACE_ROOT / "product" / "discovery" / "query-packs.json")
    rows = cli.bounded_query_rows(packs, ["pl", "en"], 48, ["pl", "en"])
    candidate = cli._github_issue_candidate(
        {"html_url": "https://github.com/o/r/issues/9", "title": "Update Gradle plugin and CI manifest", "body": "Refactor build dependencies.", "updated_at": cli.utc_now()},
        "o/r",
    )
    assert cli._local_github_query_match(candidate, rows) is None


def test_github_issue_is_emitted_once_with_bounded_redacted_body(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    updated_at = (datetime.now(timezone.utc) - timedelta(days=5)).isoformat().replace("+00:00", "Z")
    long_body = "Caregiver has no readings. Email user@example.com IPv6 2001:db8::1 Cookie: private-cookie. " + ("Impact continues. " * 80)
    monkeypatch.setattr(cli, "_fetch_json", lambda *a, **k: [{
        "html_url": "https://github.com/o/r/issues/10", "title": "LibreLinkUp readings missing for caregiver",
        "body": long_body, "updated_at": updated_at, "user": {"login": "never-persist"},
    }])
    packs = cli.load_query_packs(WORKSPACE_ROOT / "product" / "discovery" / "query-packs.json")
    items = cli._collect_github_issue_search(
        {"name": "gh", "family": "github_community", "repos": ["o/r"], "max_pages": 1}, 30, 1, 1,
        cli.DiscoveryCache(tmp_path / "cache.json"), 21600, packs, ["pl", "en", "de", "fr", "es"], 365, ["pl", "en"],
    )
    assert len(items) == 1
    item = items[0]
    assert item["concept_id"] == "glucose_sharing_delay_or_failure"
    assert item["source_identity"] == "o/r"
    assert item["problem_statement"] == "LibreLinkUp readings missing for caregiver"
    assert len(item["excerpt"]) <= cli.MAX_CACHED_EXCERPT_LENGTH
    assert "user@example.com" not in item["excerpt"]
    assert "2001:db8::1" not in item["excerpt"]
    assert "private-cookie" not in item["excerpt"]
    assert "never-persist" not in json.dumps(items)


def test_github_403_degrades_only_failed_source_with_sanitized_detail(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env

    def forbidden(*args, **kwargs):
        raise cli.urllib_error.HTTPError("https://api.github.com/repos/o/r/issues", 403, "Forbidden secret-value", {}, None)

    monkeypatch.setattr(cli, "_fetch_json", forbidden)
    sources = {"sources": [
        {"name": "external_github", "family": "github_community", "kind": "github_issue_search", "enabled": True,
         "evidence_role": "developer_community", "repos": ["o/r"]},
        {"name": "safe_fixture", "family": "reddit", "enabled": True, "evidence_role": "user_community", "fixture_items": [
            {"url": "https://example.com/1", "text": "Caregiver has missing glucose readings", "problem_statement": "Caregiver has missing glucose readings", "language": "en"}
        ]},
    ]}
    path = tmp_path / "sources.json"
    path.write_text(json.dumps(sources), encoding="utf-8")
    report = cli.run_discovery(path, 10, False, "", "", "workflow-token", "heuristic", "gpt-5.4-mini", None, 1, 3, 21600)
    by_name = {row["name"]: row for row in report["source_status"]}
    assert report["status"] == "DEGRADED"
    assert by_name["external_github"]["status"] == "EXTERNAL_GITHUB_DEGRADED"
    assert by_name["external_github"]["detail"] == "HTTP 403: authorization/access problem"
    assert "secret-value" not in by_name["external_github"]["detail"]
    assert by_name["safe_fixture"]["status"] == "OK"
    assert report["counts"]["COLLECTED"] == 1


def test_github_http_diagnostics_are_sanitized_and_403_is_not_retried(cli_env, monkeypatch):
    cli, _ = cli_env
    assert cli._sanitized_failure_detail(cli.urllib_error.HTTPError("https://example", 401, "secret", {}, None)) == "HTTP 401: authorization/access problem"
    assert cli._sanitized_failure_detail(cli.urllib_error.HTTPError("https://example", 403, "secret", {}, None)) == "HTTP 403: authorization/access problem"
    assert cli._sanitized_failure_detail(cli.urllib_error.HTTPError("https://example", 429, "secret", {}, None)) == "HTTP 429: rate limit"
    assert cli._sanitized_failure_detail(cli.urllib_error.HTTPError("https://example", 500, "secret", {}, None)) == "HTTP 500: upstream request failed"

    attempts = []
    def forbidden(*args, **kwargs):
        attempts.append(1)
        raise cli.urllib_error.HTTPError("https://example", 403, "secret", {}, None)
    monkeypatch.setattr(cli.urllib_request, "urlopen", forbidden)
    with pytest.raises(cli.urllib_error.HTTPError):
        cli._request_bytes("https://example", "GET", timeout=1, retries=3)
    assert len(attempts) == 1


def test_discovery_safety_defaults_remain_bounded(cli_env):
    cli, _ = cli_env
    args = cli.build_parser().parse_args([])
    assert args.publish_top3 is False
    assert args.ai_model == "gpt-5.4-mini"
    assert cli.CROSS_LANGUAGE_SIMILARITY_THRESHOLD == 0.60
    assert cli.LOCAL_QUERY_MATCH_THRESHOLD == 0.60
    assert cli.LOCAL_QUERY_AMBIGUITY_DELTA == 0.05
    assert cli.MAX_GITHUB_REPO_PAGES == 2
    assert cli.EVIDENCE_SCORE_CAPS == {"WEAK": 49, "SUPPORTED": 74, "CORROBORATED": 89, "STRONG": 100}


@pytest.mark.parametrize(
    "title",
    [
        "Not receiving glucose values",
        "Blood glucose values aren't updating when listening to a song",
    ],
)
def test_generic_github_evidence_uses_neutral_metadata(cli_env, tmp_path, monkeypatch, title):
    cli, _ = cli_env
    monkeypatch.setattr(cli, "_fetch_json", lambda *a, **k: [{
        "html_url": "https://github.com/pachi81/GlucoDataAuto/issues/1",
        "title": title,
        "body": "No new values arrive.",
        "updated_at": cli.utc_now(),
    }])
    packs = cli.load_query_packs(WORKSPACE_ROOT / "product" / "discovery" / "query-packs.json")
    items = cli._collect_github_issue_search(
        {"name": "glucodataauto_github", "family": "github_community", "repos": ["pachi81/GlucoDataAuto"], "max_pages": 1},
        10, 1, 1, cli.DiscoveryCache(tmp_path / "cache.json"), 3600, packs, ["en"], 365, ["en"],
    )
    assert len(items) == 1
    assert items[0]["persona"] == "unknown"
    assert items[0]["mode"] == "unknown"
    assert items[0]["module"] == "unknown"
    assert items[0]["persona"] not in {"caregiver", "senior", "clinician"}


def _run6_grounding_cluster(cli):
    evidence_a = cli._normalize_item_fields(
        {
            "url": "https://github.com/Crazy-Marvin/LibreLinkUpDesktop/issues/529",
            "text": "No Error message if connection to Sensor is lost. The last received value stays visible and no disconnect warning appears.",
            "problem_statement": "No Error message if connection to Sensor is lost",
            "source_identity": "Crazy-Marvin/LibreLinkUpDesktop",
            "language": "en", "concept_id": "signal_loss_disconnect", "topic_id": "connectivity",
            "query_id": "signal_loss_disconnect:en:1", "evidence_role": "developer_community",
            "persona": "unknown", "mode": "unknown", "module": "unknown",
        },
        "librelinkup_desktop_github", "github_community", "community",
    )
    evidence_b = cli._normalize_item_fields(
        {
            "url": "https://github.com/j-kaltes/Juggluco/issues/441",
            "text": "Fsl2 PL motorola edge 50 neo loosing connection with sensor. Bluetooth reconnects unreliably after Android 16.",
            "problem_statement": "Fsl2 PL motorola edge 50 neo loosing connection with sensor",
            "source_identity": "j-kaltes/Juggluco",
            "language": "en", "concept_id": "signal_loss_disconnect", "topic_id": "connectivity",
            "query_id": "signal_loss_disconnect:en:2", "evidence_role": "developer_community",
            "persona": "unknown", "mode": "unknown", "module": "unknown",
        },
        "juggluco_github", "github_community", "community",
    )
    clusters = cli.cluster_items([evidence_a, evidence_b])
    assert len(clusters) == 1
    return clusters[0]


def _foundation_fixture(*, requirement="", capability="", decision="", decision_status="HOLD"):
    return {
        "requirements": ([{
            "id": "REQ-TEST", "text": requirement, "status": "ACCEPTED", "path": "product/requirements/REQ-TEST.yaml",
        }] if requirement else []),
        "capabilities": [capability] if capability else [],
        "decisions": ([{
            "id": "DEC-TEST", "subject": decision, "decision": "", "status": decision_status,
            "path": "product/decisions/DEC-TEST.yaml",
        }] if decision else []),
        "observations": [],
    }


def _product_problem_ai_row():
    return {"classification": "PRODUCT_PROBLEM", "solvability": "APP"}


def test_foundation_match_is_independent_of_raw_source_noise(cli_env):
    cli, _ = cli_env
    canonical = "The sensor/app connection can be lost or become unstable."
    generic = {
        "canonical_problem_statement": canonical,
        "canonical_grounding_safe": True,
        "identity_ambiguous": False,
        "raw_items": [{"problem_statement": "Generic connection-loss evidence"}],
    }
    noisy = {
        **generic,
        "raw_items": [{
            "problem_statement": (
                "Missing disconnect notification and notification customization on "
                "Motorola Edge 50 Neo after Android 16"
            ),
            "excerpt": "Source-specific warning behavior and device context",
        }],
    }
    foundation = _foundation_fixture(
        requirement="Missing disconnect notification and notification customization",
        capability="Motorola Edge 50 Neo notification customization after Android 16",
        decision="Source-specific warning behavior and device context",
    )

    generic_match = cli.match_cluster_to_foundation(generic, foundation)
    noisy_match = cli.match_cluster_to_foundation(noisy, foundation)

    for key in (
        "best_requirement_score", "best_capability_score", "linked",
        "suppress_reproposal", "suppress_reason",
    ):
        assert generic_match[key] == noisy_match[key]


def test_raw_evidence_cannot_cause_requirement_suppression(cli_env):
    cli, _ = cli_env
    cluster = {
        "canonical_problem_statement": "The sensor/app connection can be lost or become unstable.",
        "canonical_grounding_safe": True,
        "raw_items": [{"problem_statement": "Missing disconnect notification customization"}],
        "foundation_match": {},
    }
    match = cli.match_cluster_to_foundation(
        cluster,
        _foundation_fixture(requirement="Missing disconnect notification customization"),
    )
    cluster["foundation_match"] = match

    assert match["best_requirement_score"] < 0.75
    assert not any(row["type"] == "requirement" for row in match["linked"])
    assert cli.apply_governance(cluster, _product_problem_ai_row()).get("suppressed", False) is False


def test_raw_evidence_cannot_cause_validated_capability_match(cli_env):
    cli, _ = cli_env
    cluster = {
        "canonical_problem_statement": "The sensor/app connection can be lost or become unstable.",
        "canonical_grounding_safe": True,
        "raw_items": [{"problem_statement": "Custom disconnect notifications are available"}],
        "foundation_match": {},
    }
    match = cli.match_cluster_to_foundation(
        cluster,
        _foundation_fixture(capability="Custom disconnect notifications are available"),
    )
    cluster["foundation_match"] = match

    assert match["best_capability_score"] < 0.68
    assert cli.apply_governance(cluster, _product_problem_ai_row())["classification"] == "PRODUCT_PROBLEM"


def test_raw_evidence_cannot_cause_decision_suppression(cli_env):
    cli, _ = cli_env
    cluster = {
        "canonical_problem_statement": "The sensor/app connection can be lost or become unstable.",
        "canonical_grounding_safe": True,
        "raw_items": [{"problem_statement": "Hold notification customization for Android 16"}],
        "foundation_match": {},
    }
    match = cli.match_cluster_to_foundation(
        cluster,
        _foundation_fixture(decision="Hold notification customization for Android 16"),
    )
    cluster["foundation_match"] = match

    assert match["suppress_reproposal"] is False
    assert not any(row["type"] == "decision" for row in match["linked"])
    assert cli.apply_governance(cluster, _product_problem_ai_row()).get("suppressed", False) is False


@pytest.mark.parametrize(
    "cluster",
    [
        {"canonical_grounding_safe": False, "normalized_problem": "Source-specific notification requirement"},
        {"identity_ambiguous": True, "normalized_problem": "Source-specific notification requirement"},
        {"raw_items": [{"problem_statement": "Source-specific notification requirement"}]},
    ],
)
def test_foundation_match_fails_closed_without_safe_cluster_problem(cli_env, cluster):
    cli, _ = cli_env
    cluster["raw_items"] = [{"problem_statement": "Source-specific notification requirement"}]
    match = cli.match_cluster_to_foundation(
        cluster,
        _foundation_fixture(
            requirement="Source-specific notification requirement",
            capability="Source-specific notification requirement",
            decision="Source-specific notification requirement",
        ),
    )

    assert match == {
        "linked": [],
        "suppress_reproposal": False,
        "suppress_reason": "",
        "best_requirement_score": 0.0,
        "best_capability_score": 0.0,
    }


def test_normalized_problem_is_compatible_foundation_fallback(cli_env):
    cli, _ = cli_env
    problem = "Caregivers cannot distinguish stale glucose readings."
    match = cli.match_cluster_to_foundation(
        {"normalized_problem": problem, "raw_items": []},
        _foundation_fixture(requirement=problem),
    )

    assert match["best_requirement_score"] == 1.0
    assert [row["type"] for row in match["linked"]] == ["requirement"]


def test_positive_canonical_foundation_matching_remains_intact(cli_env):
    cli, _ = cli_env
    problem = "The sensor/app connection can be lost or become unstable."
    cluster = {
        "canonical_problem_statement": problem,
        "canonical_grounding_safe": True,
        "raw_items": [],
        "foundation_match": {},
    }
    match = cli.match_cluster_to_foundation(
        cluster,
        _foundation_fixture(requirement=problem, capability=problem, decision=problem),
    )
    cluster["foundation_match"] = match
    governed = cli.apply_governance(cluster, _product_problem_ai_row())

    assert match["best_requirement_score"] == 1.0
    assert match["best_capability_score"] == 1.0
    assert {row["type"] for row in match["linked"]} == {"requirement", "validated_capability", "decision"}
    assert match["suppress_reproposal"] is True
    assert governed["classification"] == "VALIDATED_CAPABILITY"
    assert governed["suppressed"] is True


def test_unknown_github_cluster_preserves_unknown_persona(cli_env):
    cli, _ = cli_env
    cluster = _run6_grounding_cluster(cli)
    assert cluster["persona_candidate"] == "unknown"
    assert cluster["module_candidate"] == "unknown"


def test_ai_persona_cannot_override_unknown_evidence(cli_env):
    cli, _ = cli_env
    cluster = _run6_grounding_cluster(cli)
    payload = build_ai_payload_for_clusters([cluster])
    payload["clusters"][0]["persona"] = "caregiver"
    valid, errors = cli.validate_ai_output(payload, [cluster])
    assert valid is False
    assert any("persona must equal persona_candidate" in error for error in errors)


def test_ai_persona_override_uses_bounded_repair(cli_env, tmp_path, monkeypatch):
    cli, root = cli_env
    sources = {"sources": [{
        "name": "github_fixture", "family": "github_community", "evidence_role": "developer_community",
        "fixture_items": [
            {"url": "https://example.com/a", "text": "Glucose readings are missing", "problem_statement": "Glucose readings are missing",
             "source_identity": "repo/a", "persona": "unknown", "mode": "unknown", "module": "unknown"},
            {"url": "https://example.com/b", "text": "No glucose readings arrive", "problem_statement": "No glucose readings arrive",
             "source_identity": "repo/b", "persona": "unknown", "mode": "unknown", "module": "unknown"},
        ],
    }]}
    source_path = tmp_path / "sources.json"
    source_path.write_text(json.dumps(sources), encoding="utf-8")
    clusters = _prepare_clusters(cli, source_path)
    invalid = build_ai_payload_for_clusters(clusters)
    invalid["clusters"][0]["persona"] = "caregiver"
    repaired = build_ai_payload_for_clusters(clusters)
    responses = iter([json.dumps(invalid), json.dumps(repaired)])
    prompts = []

    def fake_copilot(prompt, model):
        prompts.append(json.loads(prompt))
        return next(responses)

    monkeypatch.setattr(cli, "run_copilot_json", fake_copilot)
    report = cli.run_discovery(
        source_path, 20, False, "", "", "", "copilot", "gpt-5.4-mini", None, 2.0, 1, 3600,
    )
    assert report["status"] in {"SUCCESS", "DEGRADED"}
    assert report["counts"]["AI_CALLS"] == 2
    assert prompts[1]["repair_cluster_ids"] == [clusters[0]["cluster_id"]]
    assert prompts[1]["preserved_cluster_ids"] == []
    assert "validation_errors" not in prompts[1]
    assert prompts[1]["clusters"][0]["persona_candidate"] == "unknown"


def test_run6_context_separates_sources_and_exposes_shared_disconnect(cli_env):
    cli, _ = cli_env
    cluster = _run6_grounding_cluster(cli)
    assert cluster["shared_intent_facets"] == ["disconnect"]

    prompt = json.loads(cli.build_ai_prompt("RUN-6-REGRESSION", [cluster], "gpt-5.4-mini"))
    context = prompt["clusters"][0]
    evidence = context["structured_evidence_items"]
    assert len(evidence) == 2
    assert {item["source_identity"] for item in evidence} == {
        "Crazy-Marvin/LibreLinkUpDesktop", "j-kaltes/Juggluco",
    }
    assert all(set(item) == {"source_identity", "source_family", "problem_statement", "excerpt"} for item in evidence)
    assert "last received value" in evidence[0]["excerpt"]
    assert "Android 16" in evidence[1]["excerpt"]
    assert "last received value" not in evidence[1]["excerpt"]
    assert "Android 16" not in evidence[0]["excerpt"]
    assert context["shared_intent_facets"] == ["disconnect"]

    constraints = " ".join(prompt["constraints"])
    assert "common denominator supported by every evidence item" in constraints
    assert "source in evidence_summary" in constraints
    assert "persona MUST exactly equal persona_candidate" in constraints
    assert "problem_statement_pl MUST express exactly the same grounded meaning" in constraints


def test_unknown_cluster_rejects_unsupported_caregiver_statement(cli_env):
    cli, _ = cli_env
    item = cli._normalize_item_fields(
        {
            "url": "https://github.com/pachi81/GlucoDataAuto/issues/2",
            "text": "Not receiving glucose values. No new readings arrive.",
            "problem_statement": "Not receiving glucose values",
            "source_identity": "pachi81/GlucoDataAuto", "language": "en",
            "concept_id": "stale_or_missing_readings", "topic_id": "data_freshness",
            "query_id": "stale_or_missing_readings:en:1", "evidence_role": "developer_community",
            "persona": "unknown", "mode": "unknown", "module": "unknown",
        },
        "glucodataauto_github", "github_community", "community",
    )
    cluster = cli.cluster_items([item])[0]
    payload = build_ai_payload_for_clusters([cluster])
    payload["clusters"][0].update({
        "persona": "unknown",
        "problem_statement": "Caregiver does not receive current glucose values",
        "problem_statement_pl": "Opiekun nie otrzymuje aktualnych wartości glukozy",
    })
    valid, errors = cli.validate_ai_output(payload, [cluster])
    assert valid is False
    assert any("unsupported caregiver claim" in error for error in errors)


def _canonical_identity(cluster):
    return {
        key: cluster[key]
        for key in (
            "canonical_problem_statement",
            "canonical_problem_statement_pl",
            "canonical_problem_key",
            "canonical_problem_fingerprint",
            "shared_intent_facets",
        )
    }


def test_canonical_statement_layer_covers_current_twelve_concepts(cli_env):
    cli, _ = cli_env
    packs = cli.load_query_packs(WORKSPACE_ROOT / "product" / "discovery" / "query-packs.json")
    configured = {row["concept_id"] for row in packs["concepts"]}
    assert len(configured) == 12
    assert set(cli.CANONICAL_CONCEPT_STATEMENTS) == configured
    for concept_id, (statement_en, statement_pl) in cli.CANONICAL_CONCEPT_STATEMENTS.items():
        assert statement_en and statement_en.endswith(".")
        assert statement_pl and statement_pl.endswith(".")
        assert concept_id in cli.CONCEPT_INTENT_FACETS


def test_run7_canonical_grounding_is_order_and_source_noise_independent(cli_env):
    cli, _ = cli_env
    baseline = _run6_grounding_cluster(cli)
    reversed_cluster = cli.cluster_items(list(reversed(baseline["raw_items"])))[0]
    noisy_items = [dict(item) for item in baseline["raw_items"]]
    noisy_items[1]["excerpt"] += " Motorola Edge 50 Neo Android 16 Windows 11 missing disconnect warning."
    noisy_cluster = cli.cluster_items(noisy_items)[0]

    generic_raw_cluster = dict(baseline)
    generic_raw_cluster["raw_items"] = [
        {**baseline["raw_items"][0], "problem_statement": "The sensor connection is lost"},
        {**baseline["raw_items"][1], "problem_statement": "The sensor connection becomes unstable"},
    ]
    source_noisy_cluster = dict(baseline)
    source_noisy_cluster["raw_items"] = [
        {
            **baseline["raw_items"][0],
            "problem_statement": "Missing disconnect notification warning and notification customization",
        },
        {
            **baseline["raw_items"][1],
            "problem_statement": "Motorola Edge 50 Neo reconnect behavior after Android 16",
        },
    ]
    source_specific_foundation = _foundation_fixture(
        requirement="Missing disconnect notification warning and notification customization",
        capability="Motorola Edge 50 Neo reconnect behavior after Android 16",
        decision="Motorola Edge 50 Neo reconnect behavior after Android 16",
    )

    assert _canonical_identity(baseline) == _canonical_identity(reversed_cluster)
    assert _canonical_identity(baseline) == _canonical_identity(noisy_cluster)
    assert cli.match_cluster_to_foundation(
        generic_raw_cluster, source_specific_foundation,
    ) == cli.match_cluster_to_foundation(source_noisy_cluster, source_specific_foundation)
    assert baseline["canonical_problem_statement"] == "The sensor/app connection can be lost or become unstable."
    assert baseline["canonical_problem_statement_pl"] == "Połączenie sensora z aplikacją może zostać utracone lub stać się niestabilne."
    assert baseline["canonical_problem_key"] == "concept:signal_loss_disconnect|topic:connectivity|facets:disconnect"
    assert baseline["canonical_problem_fingerprint"] == cli.hashlib.sha256(
        baseline["canonical_problem_key"].encode("utf-8")
    ).hexdigest()[:16]
    contaminated = " ".join(str(value) for value in _canonical_identity(baseline).values()).lower()
    for token in ("notification", "warning", "motorola", "edge", "neo", "android", "librelinkupdesktop", "juggluco"):
        assert token not in contaminated


def test_cross_language_disconnect_grounding_is_stable(cli_env):
    cli, _ = cli_env
    en = cli._normalize_item_fields({
        "url": "https://example.com/en", "text": "The connection to the sensor is lost.",
        "problem_statement": "Connection to the sensor is lost", "source_identity": "repo/en",
        "language": "en", "concept_id": "signal_loss_disconnect", "topic_id": "connectivity",
        "persona": "unknown", "mode": "unknown", "module": "unknown",
    }, "en_source", "github_community", "community")
    pl = cli._normalize_item_fields({
        "url": "https://example.com/pl", "text": "Aplikacja rozłącza się z sensorem.",
        "problem_statement": "Aplikacja rozłącza się z sensorem", "source_identity": "repo/pl",
        "language": "pl", "concept_id": "signal_loss_disconnect", "topic_id": "connectivity",
        "persona": "unknown", "mode": "unknown", "module": "unknown",
    }, "pl_source", "github_community", "community")
    forward = cli.cluster_items([en, pl])[0]
    reverse = cli.cluster_items([pl, en])[0]
    assert forward["shared_intent_facets"] == ["disconnect"]
    assert _canonical_identity(forward) == _canonical_identity(reverse)


def test_distinct_intents_in_same_concept_fail_closed_for_persistence(cli_env):
    cli, _ = cli_env
    missing = cli._normalize_item_fields({
        "url": "https://example.com/missing", "text": "No readings are available.",
        "problem_statement": "No readings are available", "source_identity": "repo/missing",
        "language": "en", "concept_id": "stale_or_missing_readings", "topic_id": "data_freshness",
    }, "missing", "github_community", "community")
    delayed = cli._normalize_item_fields({
        "url": "https://example.com/delay", "text": "Glucose readings are delayed.",
        "problem_statement": "Glucose readings are delayed", "source_identity": "repo/delay",
        "language": "en", "concept_id": "stale_or_missing_readings", "topic_id": "data_freshness",
    }, "delay", "github_community", "community")
    cluster = cli.cluster_items([missing, delayed])[0]
    assert cluster["canonical_grounding_safe"] is False
    assert cluster["canonical_problem_key"] == ""
    resolved, summary, proposed = cli.resolve_cluster_ids_with_registry(
        [cluster], {"version": 1, "entries": []}, cli.utc_now()
    )
    assert resolved[0]["identity_ambiguous"] is True
    assert summary["new_entries"] == 0
    assert proposed["entries"] == []
    assert cli.create_observations_from_clusters(resolved, "RUN") == []


def test_ai_overclaim_cannot_redefine_report_observation_registry_or_inbox(cli_env):
    cli, root = cli_env
    cluster = _run6_grounding_cluster(cli)
    cluster["foundation_match"] = {}
    ai = build_ai_payload_for_clusters([cluster])["clusters"][0]
    ai["problem_statement"] = "The connection between the sensor and the app is lost without clear notification."
    ai["problem_statement_pl"] = "Połączenie jest tracone bez jasnego powiadomienia."
    governed = cli.apply_governance(cluster, ai)
    assert governed["problem_statement"] == cluster["canonical_problem_statement"]
    assert governed["problem_statement_pl"] == cluster["canonical_problem_statement_pl"]

    resolved, summary, proposed = cli.resolve_cluster_ids_with_registry(
        [cluster], {"version": 1, "entries": []}, cli.utc_now()
    )
    assert summary["new_entries"] == 1
    entry = proposed["entries"][0]
    assert entry["canonical_problem_key"] == cluster["canonical_problem_key"]
    assert entry["problem_fingerprint"] == cluster["canonical_problem_fingerprint"]

    created = cli.create_observations_from_clusters(resolved, "RUN-7-REPLAY")
    assert len(created) == 1
    observation = json.loads((root / created[0]).read_text(encoding="utf-8"))
    assert observation["problem_statement"] == cluster["canonical_problem_statement"]
    assert observation["problem_fingerprint"] == cluster["canonical_problem_fingerprint"]
    assert (observation["persona"], observation["mode"], observation["module"]) == ("unknown", "unknown", "unknown")

    inbox = cli.build_inbox_issue_body({"cluster": cluster, "governed": governed, "score": 74}, "<!-- marker -->")
    assert cluster["canonical_problem_statement_pl"] in inbox
    assert "bez jasnego powiadomienia" not in inbox


def test_run7_ai_overclaim_is_canonicalized_end_to_end(cli_env, tmp_path):
    cli, root = cli_env
    sources = {"sources": [{
        "name": "run7_artifact", "family": "github_community", "evidence_role": "developer_community",
        "fixture_items": [
            {
                "url": "https://github.com/Crazy-Marvin/LibreLinkUpDesktop/issues/529",
                "text": "No Error message if connection to Sensor is lost. The last received value stays visible.",
                "problem_statement": "No Error message if connection to Sensor is lost",
                "source_identity": "Crazy-Marvin/LibreLinkUpDesktop", "language": "en",
                "concept_id": "signal_loss_disconnect", "topic_id": "connectivity",
                "persona": "unknown", "mode": "unknown", "module": "unknown",
            },
            {
                "url": "https://github.com/j-kaltes/Juggluco/issues/441",
                "text": "Fsl2 PL motorola edge 50 neo loosing connection with sensor after Android 16.",
                "problem_statement": "Fsl2 PL motorola edge 50 neo loosing connection with sensor",
                "source_identity": "j-kaltes/Juggluco", "language": "en",
                "concept_id": "signal_loss_disconnect", "topic_id": "connectivity",
                "persona": "unknown", "mode": "unknown", "module": "unknown",
            },
        ],
    }]}
    source_path = tmp_path / "run7-sources.json"
    source_path.write_text(json.dumps(sources), encoding="utf-8")
    cluster = _prepare_clusters(cli, source_path)[0]
    payload = build_ai_payload_for_clusters([cluster])
    payload["clusters"][0]["problem_statement"] = "The connection between the sensor and the app is lost without clear notification."
    payload["clusters"][0]["problem_statement_pl"] = "Połączenie jest tracone bez jasnego powiadomienia."
    ai_path = tmp_path / "run7-ai.json"
    ai_path.write_text(json.dumps(payload), encoding="utf-8")

    report = run_discovery_with_ai_file(cli, source_path, ai_path)
    expected_en = "The sensor/app connection can be lost or become unstable."
    expected_pl = "Połączenie sensora z aplikacją może zostać utracone lub stać się niestabilne."
    assert report["top10"][0]["problem"] == expected_en
    assert report["top10"][0]["problem_statement"] == expected_en
    assert report["top10"][0]["problem_statement_pl"] == expected_pl

    observation = json.loads((root / report["created_observations"][0]).read_text(encoding="utf-8"))
    proposed = json.loads((root / report["cluster_registry"]["proposed_registry_path"]).read_text(encoding="utf-8"))["entries"][0]
    assert observation["problem_statement"] == expected_en
    assert observation["problem_fingerprint"] == proposed["problem_fingerprint"] == cluster["canonical_problem_fingerprint"]
    assert proposed["canonical_problem_key"] == cluster["canonical_problem_key"]
    serialized = " ".join([
        report["top10"][0]["problem_statement"],
        report["top10"][0]["problem_statement_pl"],
        observation["problem_statement"],
        proposed["canonical_problem_key"],
    ]).lower()
    for token in ("without clear notification", "motorola", "edge 50", "android 16"):
        assert token not in serialized

# End of evidence-grounding regressions.
