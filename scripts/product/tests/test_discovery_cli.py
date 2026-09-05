import importlib.util
import json
import shutil
import tempfile
import time
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
    return cli.cluster_items(deduped)


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
            "problem_statement": "Caregiver cannot detect stale readings quickly at a glance",
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
    assert any(t["classification"] == "VALIDATED_CAPABILITY" and not t["eligibility"] for t in report["top10"])


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
    assert "GITHUB BODY SECRET" not in cache_text
    assert "REDDIT SELFTEXT SECRET" not in cache_text
    assert "HTML SECRET MARKER" not in cache_text
    assert "<html" not in cache_text.lower()
    assert "<body" not in cache_text.lower()

    cache_json = json.loads(cache_path.read_text(encoding="utf-8"))
    _assert_cache_minimized(cache_json)

    obs_files = list((root / "product" / "research" / "observations").glob("OBS-*.json"))
    assert obs_files
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


def test_incremental_cluster_id_and_idempotent_observation_and_issue(cli_env, tmp_path, monkeypatch):
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

    def write_sources(path: Path, texts: list[str]):
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

    def ai_file_for(report_root: Path, clusters):
        payload = {"clusters": []}
        for cluster in clusters:
            payload["clusters"].append(
                {
                    "cluster_id": cluster["cluster_id"],
                    "classification": "PRODUCT_PROBLEM",
                    "persona": cluster["persona_candidate"],
                    "problem_statement": cluster["normalized_problem"],
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
        path = report_root / "ai.json"
        path.write_text(json.dumps(payload), encoding="utf-8")
        return path

    def run_with_sources(sources_path: Path):
        clusters = _prepare_clusters(cli, sources_path)
        ai_file = ai_file_for(root, clusters)
        return run_discovery_with_ai_file(cli, sources_path, ai_file, publish=True, repo_owner="o", repo_name="r", github_token="t")

    source_v1 = tmp_path / "sources-v1.json"
    source_v2 = tmp_path / "sources-v2.json"
    write_sources(
        source_v1,
        [
            "Caregiver cannot quickly detect stale readings",
            "Caregiver struggles to detect stale readings quickly",
        ],
    )
    write_sources(
        source_v2,
        [
            "Caregiver cannot quickly detect stale readings",
            "Caregiver struggles to detect stale readings quickly",
            "Caregiver struggles to detect stale readings quickly today",
        ],
    )

    report1 = run_with_sources(source_v1)
    obs_count1 = len(list((root / "product" / "research" / "observations").glob("OBS-*.json")))
    issue_count1 = len(FakeClient.created)
    report2 = run_with_sources(source_v2)
    obs_count2 = len(list((root / "product" / "research" / "observations").glob("OBS-*.json")))
    issue_count2 = len(FakeClient.created)

    assert report1["top10"][0]["cluster_id"] == report2["top10"][0]["cluster_id"]
    assert obs_count2 == obs_count1
    assert issue_count2 == issue_count1
    assert any(action["action"] == "SKIPPED_EXISTS" for action in report2.get("top3_issue_actions", []))


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


def test_requirement_and_hold_reject_match_suppresses_reproposal(cli_env, fixture_sources):
    cli, root = cli_env
    decision = {
        "id": "DEC-9999",
        "date": "2026-09-05",
        "subject": "Caregiver dashboard refresh feels delayed while switching monitored persons",
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
    assert any((not i["eligibility"]) and (i.get("suppressed_reason") or "existing" in (i.get("exclusion_reason") or "").lower()) for i in report["top10"])


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


