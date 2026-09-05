import importlib.util
import json
import os
import shutil
import tempfile
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

    # Ensure deterministic reddit behavior in tests.
    monkeypatch.delenv("REDDIT_CLIENT_ID", raising=False)
    monkeypatch.delenv("REDDIT_CLIENT_SECRET", raising=False)

    yield cli, root
    temp_dir.cleanup()


@pytest.fixture()
def fixture_sources(tmp_path):
    out = tmp_path / "sources.json"
    out.write_text(FIXTURE_SOURCES.read_text(encoding="utf-8"), encoding="utf-8")
    return out


def build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM", solvability="APP"):
    payload = {"clusters": []}
    for c in clusters:
        payload["clusters"].append({
            "cluster_id": c["cluster_id"],
            "classification": classification,
            "persona": c["persona_candidate"],
            "problem_statement": c["normalized_problem"],
            "evidence_summary": "Evidence summary",
            "source_diversity_summary": "Diverse sources",
            "current_librecare_match": "None",
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
        })
    return payload


def _prepare_clusters(cli, sources_file):
    cfg = json.loads(Path(sources_file).read_text(encoding="utf-8"))
    collected = []
    for source in cfg["sources"]:
        result = cli.collect_from_source(source, max_items=20, timeout=2.0, retries=1)
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
        "source_family": "github_community",
    }
    deduped, stats = cli.dedupe_items([item, dict(item)])
    assert len(deduped) == 1
    assert stats["exact_duplicates"] == 1


def test_normalized_text_duplicate_detection(cli_env):
    cli, _ = cli_env
    items = [
        {"canonical_url": "https://a/1", "content_hash": cli.content_hash("1"), "problem_statement": "Caregiver cannot read stale data quickly", "source_family": "github_community"},
        {"canonical_url": "https://a/2", "content_hash": cli.content_hash("2"), "problem_statement": "caregiver cannot read stale data quickly!!!", "source_family": "github_community"},
    ]
    deduped, stats = cli.dedupe_items(items)
    assert len(deduped) == 1
    assert stats["normalized_duplicates"] == 1


def test_near_duplicate_clustering_and_stable_cluster_id(cli_env):
    cli, _ = cli_env
    items = [
        {"canonical_url": "https://a/1", "content_hash": cli.content_hash("1"), "problem_statement": "Caregiver cannot quickly detect stale readings", "source_family": "github_community", "persona": "caregiver", "module": "Home / Monitoring", "source_type": "community", "type": "usability", "severity": "medium", "frequency": "frequent", "confidence": "medium"},
        {"canonical_url": "https://a/2", "content_hash": cli.content_hash("2"), "problem_statement": "Caregiver struggles to detect stale readings quickly", "source_family": "reddit", "persona": "caregiver", "module": "Home / Monitoring", "source_type": "community", "type": "usability", "severity": "medium", "frequency": "frequent", "confidence": "medium"},
    ]
    deduped, _ = cli.dedupe_items(items)
    c1 = cli.cluster_items(deduped)
    c2 = cli.cluster_items(deduped)
    assert len(c1) == 1
    assert c1[0]["cluster_id"] == c2[0]["cluster_id"]


def test_incremental_rerun_no_duplicate_observations(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")

    r1 = cli.run_discovery(
        sources_file=fixture_sources,
        max_items_per_source=20,
        publish_top3_flag=False,
        repo_owner="",
        repo_name="",
        github_token="",
        ai_mode="heuristic",
        ai_model="gpt-5.4-mini",
        ai_response_file=ai_file,
        timeout=2.0,
        retries=1,
    )
    r2 = cli.run_discovery(
        sources_file=fixture_sources,
        max_items_per_source=20,
        publish_top3_flag=False,
        repo_owner="",
        repo_name="",
        github_token="",
        ai_mode="heuristic",
        ai_model="gpt-5.4-mini",
        ai_response_file=ai_file,
        timeout=2.0,
        retries=1,
    )

    assert r1["status"] in {"SUCCESS", "DEGRADED"}
    assert r2["status"] in {"SUCCESS", "DEGRADED"}
    obs_files = list((root / "product" / "research" / "observations").glob("OBS-*.json"))
    assert len(obs_files) == len(set(p.name for p in obs_files))


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
            issue = {"number": len(self.__class__.issues) + 1, "html_url": f"https://example/{len(self.__class__.issues)+1}", "title": title, "body": body, "labels": labels}
            self.__class__.issues.append(issue)
            return issue

    monkeypatch.setattr(cli, "GITHUB_CLIENT_FACTORY", FakeClient)

    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")

    cli.run_discovery(fixture_sources, 20, True, "o", "r", "t", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    first_count = len(FakeClient.issues)
    cli.run_discovery(fixture_sources, 20, True, "o", "r", "t", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
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
    report = cli.run_discovery(fixture_sources, 20, False, "", "", "", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    # At least one entry should be VALIDATED_CAPABILITY and ineligible.
    assert any(t["classification"] == "VALIDATED_CAPABILITY" and not t["eligibility"] for t in report["top10"])


def test_test_coverage_gap_report_only(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters, classification="TEST_COVERAGE_GAP")
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")
    report = cli.run_discovery(fixture_sources, 20, False, "", "", "", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    assert all(not t["eligibility"] for t in report["top10"])


def test_safety_gap_can_be_candidate_but_never_auto_accept(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters, classification="SAFETY_GAP")
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")
    report = cli.run_discovery(fixture_sources, 20, False, "", "", "", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    assert any(t["classification"] == "SAFETY_GAP" for t in report["top10"])
    assert report["safety_guards"]["no_auto_acceptance"] is True
    assert report["safety_guards"]["no_auto_implementation"] is True


def test_inconclusive_report_only(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters, classification="INCONCLUSIVE")
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")
    report = cli.run_discovery(fixture_sources, 20, False, "", "", "", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    assert all(not t["eligibility"] for t in report["top10"])


def test_product_problem_and_opportunity_eligible(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    problem_payload = build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")
    ai_problem = root / "ai-problem.json"
    ai_problem.write_text(json.dumps(problem_payload), encoding="utf-8")
    r_problem = cli.run_discovery(fixture_sources, 20, False, "", "", "", "heuristic", "gpt-5.4-mini", ai_problem, 2.0, 1)
    assert any(t["classification"] == "PRODUCT_PROBLEM" and t["eligibility"] for t in r_problem["top10"])

    opp_payload = build_ai_payload_for_clusters(clusters, classification="PRODUCT_OPPORTUNITY")
    ai_opp = root / "ai-opp.json"
    ai_opp.write_text(json.dumps(opp_payload), encoding="utf-8")
    r_opp = cli.run_discovery(fixture_sources, 20, False, "", "", "", "heuristic", "gpt-5.4-mini", ai_opp, 2.0, 1)
    assert any(t["classification"] == "PRODUCT_OPPORTUNITY" and t["eligibility"] for t in r_opp["top10"])


def test_source_failure_degraded_no_fabricated_evidence(cli_env, tmp_path, monkeypatch):
    cli, _ = cli_env
    sources = {
        "sources": [{"name": "abbott_official", "family": "official_vendor", "enabled": True, "urls": ["https://example.invalid"]}]
    }
    path = tmp_path / "sources.json"
    path.write_text(json.dumps(sources), encoding="utf-8")

    def fail_fetch(_url, timeout, retries):
        raise RuntimeError("network blocked")

    monkeypatch.setattr(cli, "_fetch_url", fail_fetch)
    report = cli.run_discovery(path, 5, False, "", "", "", "heuristic", "gpt-5.4-mini", None, 1.0, 1)
    assert report["status"] == "DEGRADED"
    assert report["counts"]["COLLECTED"] == 0


def test_reddit_without_credentials_disabled_and_continues(cli_env, tmp_path):
    cli, _ = cli_env
    sources = {"sources": [{"name": "reddit_diabetes", "family": "reddit", "enabled": True}]}
    path = tmp_path / "sources.json"
    path.write_text(json.dumps(sources), encoding="utf-8")
    report = cli.run_discovery(path, 5, False, "", "", "", "heuristic", "gpt-5.4-mini", None, 1.0, 1)
    assert report["status"] == "DEGRADED"
    assert any(s["status"] == "REDDIT_DISABLED" for s in report["source_status"])


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
    report = cli.run_discovery(fixture_sources, 20, True, "o", "r", "t", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    assert report["status"] == "FAILED"
    assert FakeClient.created == 0


def test_ai_call_cap_and_model(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")
    report = cli.run_discovery(fixture_sources, 20, False, "", "", "", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    assert report["counts"]["AI_CALLS"] <= 2


def test_top_candidate_cap_max_three(cli_env, fixture_sources, monkeypatch):
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
    payload = build_ai_payload_for_clusters(clusters)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")
    cli.run_discovery(fixture_sources, 20, True, "o", "r", "t", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    assert len(FakeClient.created) <= 3


def test_marker_idempotency_open_and_closed_issue(cli_env, fixture_sources, monkeypatch):
    cli, root = cli_env

    class FakeClient:
        store = [{"number": 88, "html_url": "https://example/88", "body": "<!-- LIBRECARE_DISCOVERY_CLUSTER: DISC-EXISTS -->", "state": "closed"}]
        def __init__(self, owner, repo, token):
            pass
        def find_issue_by_marker(self, marker):
            for issue in self.__class__.store:
                if marker in issue["body"]:
                    return issue
            return None
        def create_issue(self, title, body, labels):
            self.__class__.store.append({"number": 99, "html_url": "https://example/99", "body": body, "state": "open"})
            return self.__class__.store[-1]

    monkeypatch.setattr(cli, "GITHUB_CLIENT_FACTORY", FakeClient)

    cfg = json.loads(Path(fixture_sources).read_text(encoding="utf-8"))
    first = cfg["sources"][0]["fixture_items"][0]
    sources = {
        "sources": [{"name": "a", "family": "github_community", "enabled": True, "fixture_items": [first]}]
    }
    sf = root / "sources-single.json"
    sf.write_text(json.dumps(sources), encoding="utf-8")
    clusters = _prepare_clusters(cli, sf)
    clusters[0]["cluster_id"] = "DISC-EXISTS"
    payload = build_ai_payload_for_clusters(clusters)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")

    # Patch cluster creation to keep deterministic synthetic ID for this test.
    original_cluster_items = cli.cluster_items
    def fake_cluster_items(_items, threshold=0.58):
        return clusters
    monkeypatch.setattr(cli, "cluster_items", fake_cluster_items)

    report = cli.run_discovery(sf, 20, True, "o", "r", "t", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    assert any(a["action"] == "SKIPPED_EXISTS" for a in report["top3_issue_actions"])
    monkeypatch.setattr(cli, "cluster_items", original_cluster_items)


def test_requirement_and_hold_reject_match_suppresses_reproposal(cli_env, fixture_sources):
    cli, root = cli_env
    # Add synthetic rejected decision matching a known problem.
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
        "related_requirements": []
    }
    (root / "product" / "decisions" / "DEC-9999.json").write_text(json.dumps(decision), encoding="utf-8")

    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")
    report = cli.run_discovery(fixture_sources, 20, False, "", "", "", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    assert any((not i["eligibility"]) and ("suppressed" in (i.get("exclusion_reason") or "").lower() or i.get("suppressed_reason")) for i in report["top10"])


def test_no_direct_requirement_acceptance_or_implementation_creation(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")
    before_req = set((root / "product" / "requirements").glob("REQ-*"))
    before_impl = set((root / "product" / "implementation").glob("IMP-*"))
    report = cli.run_discovery(fixture_sources, 20, False, "", "", "", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    after_req = set((root / "product" / "requirements").glob("REQ-*"))
    after_impl = set((root / "product" / "implementation").glob("IMP-*"))
    assert report["safety_guards"]["no_auto_merge"] is True
    assert before_req == after_req
    assert before_impl == after_impl


def test_repo_name_starting_dash_regression(cli_env):
    cli, _ = cli_env
    parser = cli.build_parser()
    args = parser.parse_args(["--repo-name=-LibreDisplay"])
    assert args.repo_name == "-LibreDisplay"


def test_privacy_no_username_or_full_post_persisted(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters, classification="PRODUCT_PROBLEM")
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")
    cli.run_discovery(fixture_sources, 20, False, "", "", "", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    obs_files = list((root / "product" / "research" / "observations").glob("OBS-*.json"))
    assert obs_files
    for p in obs_files:
        data = json.loads(p.read_text(encoding="utf-8"))
        txt = json.dumps(data)
        assert "username" not in txt.lower()
        for evidence in data.get("evidence", []):
            assert len(evidence) <= 220


def test_source_family_diversity_counts_independent_families(cli_env):
    cli, _ = cli_env
    items = [
        {"canonical_url": "https://g/1", "content_hash": cli.content_hash("1"), "problem_statement": "same stale problem in caregiver dashboard", "source_family": "github_community", "persona": "caregiver", "module": "Home / Monitoring", "source_type": "community", "type": "usability", "severity": "medium", "frequency": "frequent", "confidence": "medium"},
        {"canonical_url": "https://g/2", "content_hash": cli.content_hash("2"), "problem_statement": "same stale problem in caregiver dashboard with extra words", "source_family": "github_community", "persona": "caregiver", "module": "Home / Monitoring", "source_type": "community", "type": "usability", "severity": "medium", "frequency": "frequent", "confidence": "medium"},
    ]
    deduped, _ = cli.dedupe_items(items)
    clusters = cli.cluster_items(deduped)
    assert clusters[0]["independent_source_family_count"] == 1


def test_score_deterministic_for_fixed_input(cli_env, fixture_sources):
    cli, root = cli_env
    clusters = _prepare_clusters(cli, fixture_sources)
    payload = build_ai_payload_for_clusters(clusters)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(payload), encoding="utf-8")
    r1 = cli.run_discovery(fixture_sources, 20, False, "", "", "", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    r2 = cli.run_discovery(fixture_sources, 20, False, "", "", "", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    s1 = [(x["cluster_id"], x["score"]) for x in r1["top10"]]
    s2 = [(x["cluster_id"], x["score"]) for x in r2["top10"]]
    assert s1 == s2


def test_workflow_defaults_and_no_schedule_and_no_auto_merge_path():
    workflow_path = WORKSPACE_ROOT / ".github" / "workflows" / "librecare-discovery.yml"
    text = workflow_path.read_text(encoding="utf-8")
    assert "workflow_dispatch:" in text
    assert "schedule:" not in text
    assert "default: false" in text
    assert "--repo-name \"${{ github.event.repository.name }}\"" in text
    assert "gh pr merge" not in text
    assert "pulls.merge" not in text


def test_no_android_app_code_modified_by_discovery_runtime(cli_env, fixture_sources):
    cli, root = cli_env
    app_dir = root / "app"
    app_dir.mkdir(exist_ok=True)
    marker = app_dir / "marker.txt"
    marker.write_text("unchanged", encoding="utf-8")
    clusters = _prepare_clusters(cli, fixture_sources)
    ai_file = root / "ai.json"
    ai_file.write_text(json.dumps(build_ai_payload_for_clusters(clusters)), encoding="utf-8")
    cli.run_discovery(fixture_sources, 20, False, "", "", "", "heuristic", "gpt-5.4-mini", ai_file, 2.0, 1)
    assert marker.read_text(encoding="utf-8") == "unchanged"


def test_preserve_corrected_discovery_logic_file_exists():
    target = WORKSPACE_ROOT / "scripts" / "product" / "tests" / "test_corrected_discovery_logic.py"
    assert target.exists()


