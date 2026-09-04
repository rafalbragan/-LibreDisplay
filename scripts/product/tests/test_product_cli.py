import contextlib
import io
import importlib.util
import json
import re
import shutil
import tempfile
import unittest
from pathlib import Path

import yaml


WORKSPACE_ROOT = Path(__file__).resolve().parents[3]
CLI_PATH = WORKSPACE_ROOT / "scripts" / "product" / "product_cli.py"
WORKFLOW_PATH = WORKSPACE_ROOT / ".github" / "workflows" / "product-inbox.yml"
ISSUE_FORM_PATH = WORKSPACE_ROOT / ".github" / "ISSUE_TEMPLATE" / "product-inbox.yml"
BUG_WORKFLOW_PATH = WORKSPACE_ROOT / ".github" / "workflows" / "librecare-implementation-automation.yml"
BUG_ISSUE_FORM_PATH = WORKSPACE_ROOT / ".github" / "ISSUE_TEMPLATE" / "librecare-bug.yml"
ANDROID_CI_WORKFLOW_PATH = WORKSPACE_ROOT / ".github" / "workflows" / "android-ci.yml"
ANDROID_BUILD_WORKFLOW_PATH = WORKSPACE_ROOT / ".github" / "workflows" / "android-build.yml"
ANDROID_DEBUG_BUILD_WORKFLOW_PATH = WORKSPACE_ROOT / ".github" / "workflows" / "android-debug-build.yml"
DOWNLOAD_APP_TESTING_RESULTS_WORKFLOW_PATH = WORKSPACE_ROOT / ".github" / "workflows" / "download-app-testing-results.yml"
FIREBASE_TEST_LAB_WORKFLOW_PATH = WORKSPACE_ROOT / ".github" / "workflows" / "firebase-test-lab.yml"
PRODUCT_QUALITY_WORKFLOW_PATH = WORKSPACE_ROOT / ".github" / "workflows" / "product-quality.yml"


def load_cli_module():
    spec = importlib.util.spec_from_file_location("product_cli_under_test", CLI_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    spec.loader.exec_module(module)
    return module


class ProductCliInboxTest(unittest.TestCase):
    def setUp(self):
        self.cli = load_cli_module()
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)
        shutil.copytree(WORKSPACE_ROOT / "product", self.root / "product")
        self.original_client_factory = self.cli.GITHUB_CLIENT_FACTORY

        class FakeGitHubClient:
            created_issues = []
            assigned = []
            assignment_tokens = []
            labels = []
            next_issue_number = 500
            issue_assignees = {}
            issue_assignee_nodes = {}
            assign_should_fail = False
            existing_issues_by_req = {}
            find_existing_calls = 0

            def __init__(self, owner: str, repo: str, token: str):
                self.owner = owner
                self.repo = repo
                self.token = token

            def ensure_label(self, name: str, color: str, description: str) -> None:
                self.__class__.labels.append((name, color, description))

            def find_existing_implementation_issue(self, req_id: str, expected_title: str):
                self.__class__.find_existing_calls += 1
                existing = self.__class__.existing_issues_by_req.get(req_id)
                if existing is not None:
                    return dict(existing)
                for issue in self.__class__.created_issues:
                    if issue["req_id"] == req_id:
                        return issue["response"]
                return None

            def create_issue(self, title: str, body: str, labels: list[str]):
                issue_number = self.__class__.next_issue_number
                self.__class__.next_issue_number += 1
                response = {
                    "number": issue_number,
                    "html_url": f"https://github.com/example/repo/issues/{issue_number}",
                    "title": title,
                }
                self.__class__.created_issues.append({
                    "title": title,
                    "body": body,
                    "labels": labels,
                    "response": response,
                    "req_id": re.search(r"REQ-[0-9A-Za-z._-]+", title).group(0),
                })
                self.__class__.issue_assignees.setdefault(issue_number, [])
                self.__class__.issue_assignee_nodes.setdefault(issue_number, [])
                return response

            def _copilot_assignee_confirmed(self, issue_number: int, expected_actor_node_id: str | None = None):
                nodes = list(self.__class__.issue_assignee_nodes.get(issue_number, []))
                if not nodes:
                    nodes = [{"login": login, "node_id": "BOT_NODE_ID"} for login in self.__class__.issue_assignees.get(issue_number, [])]
                logins = [str((node or {}).get("login")) for node in nodes if (node or {}).get("login")]
                node_ids = [str((node or {}).get("node_id") or (node or {}).get("id")) for node in nodes if (node or {}).get("node_id") or (node or {}).get("id")]
                if expected_actor_node_id:
                    return expected_actor_node_id in node_ids, logins, node_ids
                return "copilot-swe-agent[bot]" in logins or "BOT_NODE_ID" in node_ids, logins, node_ids

            def assign_copilot(self, issue_number: int, base_branch: str, instructions: str):
                if self.__class__.assign_should_fail:
                    raise RuntimeError("GraphQL assignment failed: missing feature preview")
                self.__class__.assignment_tokens.append(self.token)
                logins = set(self.__class__.issue_assignees.get(issue_number, []))
                logins.add("copilot-swe-agent[bot]")
                self.__class__.issue_assignees[issue_number] = sorted(logins)
                nodes = [node for node in self.__class__.issue_assignee_nodes.get(issue_number, []) if str((node or {}).get("node_id") or (node or {}).get("id")) != "BOT_NODE_ID"]
                nodes.append({"login": "Copilot", "node_id": "BOT_NODE_ID"})
                self.__class__.issue_assignee_nodes[issue_number] = nodes
                payload = {
                    "issue_number": issue_number,
                    "base_branch": base_branch,
                    "instructions": instructions,
                    "status": "ASSIGNED",
                    "method": "GRAPHQL",
                    "mutation": "addAssigneesToAssignable",
                    "field": "agent_assignment",
                    "actor_node_id": "BOT_NODE_ID",
                }
                self.__class__.assigned.append(payload)
                return payload

        self.fake_client = FakeGitHubClient
        self.cli.GITHUB_CLIENT_FACTORY = FakeGitHubClient
        self.fake_client.created_issues = []
        self.fake_client.assigned = []
        self.fake_client.assignment_tokens = []
        self.fake_client.labels = []
        self.fake_client.next_issue_number = 500
        self.fake_client.issue_assignees = {}
        self.fake_client.issue_assignee_nodes = {}
        self.fake_client.assign_should_fail = False
        self.fake_client.existing_issues_by_req = {}
        self.fake_client.find_existing_calls = 0

    def tearDown(self):
        self.cli.GITHUB_CLIENT_FACTORY = self.original_client_factory
        self.temp_dir.cleanup()

    @contextlib.contextmanager
    def patched_paths(self):
        original = {
            "ROOT": self.cli.ROOT,
            "PRODUCT": self.cli.PRODUCT,
            "SCHEMA_DIR": self.cli.SCHEMA_DIR,
            "CURRENT_FOCUS_FILE": self.cli.CURRENT_FOCUS_FILE,
            "OBSERVATIONS_DIR": self.cli.OBSERVATIONS_DIR,
            "TEST_RUNS_DIR": self.cli.TEST_RUNS_DIR,
            "REQUIREMENTS_DIR": self.cli.REQUIREMENTS_DIR,
            "DECISIONS_DIR": self.cli.DECISIONS_DIR,
            "EXAMPLES_DIR": self.cli.EXAMPLES_DIR,
            "INBOX_DIR": self.cli.INBOX_DIR,
            "REVIEW_DIR": self.cli.REVIEW_DIR,
            "REVIEW_QUEUE_FILE": self.cli.REVIEW_QUEUE_FILE,
            "GENERATED_DIR": self.cli.GENERATED_DIR,
            "INBOX_REVIEW_RESULTS_FILE": self.cli.INBOX_REVIEW_RESULTS_FILE,
            "INBOX_REVIEWS_DIR": self.cli.INBOX_REVIEWS_DIR,
            "IMPLEMENTATION_HANDOFFS_DIR": self.cli.IMPLEMENTATION_HANDOFFS_DIR,
            "BUG_REVIEWS_DIR": self.cli.BUG_REVIEWS_DIR,
            "TECHNICAL_VALIDATIONS_DIR": self.cli.TECHNICAL_VALIDATIONS_DIR,
            "IMPLEMENTATION_DIR": self.cli.IMPLEMENTATION_DIR,
            "BUGS_DIR": self.cli.BUGS_DIR,
        }
        try:
            self.cli.ROOT = self.root
            self.cli.PRODUCT = self.root / "product"
            self.cli.SCHEMA_DIR = self.cli.PRODUCT / "schema"
            self.cli.CURRENT_FOCUS_FILE = self.cli.PRODUCT / "CURRENT_FOCUS.yaml"
            self.cli.OBSERVATIONS_DIR = self.cli.PRODUCT / "research" / "observations"
            self.cli.TEST_RUNS_DIR = self.cli.PRODUCT / "research" / "test-runs"
            self.cli.REQUIREMENTS_DIR = self.cli.PRODUCT / "requirements"
            self.cli.DECISIONS_DIR = self.cli.PRODUCT / "decisions"
            self.cli.EXAMPLES_DIR = self.cli.PRODUCT / "examples"
            self.cli.INBOX_DIR = self.cli.PRODUCT / "inbox"
            self.cli.REVIEW_DIR = self.cli.PRODUCT / "review"
            self.cli.REVIEW_QUEUE_FILE = self.cli.REVIEW_DIR / "REVIEW_QUEUE.yaml"
            self.cli.GENERATED_DIR = self.cli.PRODUCT / "generated"
            self.cli.INBOX_REVIEW_RESULTS_FILE = self.cli.GENERATED_DIR / "INBOX_REVIEW_RESULTS.json"
            self.cli.INBOX_REVIEWS_DIR = self.cli.GENERATED_DIR / "inbox-reviews"
            self.cli.IMPLEMENTATION_HANDOFFS_DIR = self.cli.GENERATED_DIR / "implementation-handoffs"
            self.cli.BUG_REVIEWS_DIR = self.cli.GENERATED_DIR / "bug-reviews"
            self.cli.TECHNICAL_VALIDATIONS_DIR = self.cli.GENERATED_DIR / "technical-validations"
            self.cli.IMPLEMENTATION_DIR = self.cli.PRODUCT / "implementation"
            self.cli.BUGS_DIR = self.cli.PRODUCT / "bugs"
            yield
        finally:
            for key, value in original.items():
                setattr(self.cli, key, value)

    def write_json(self, path: Path, payload):
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("w", encoding="utf-8") as fh:
            json.dump(payload, fh, indent=2, ensure_ascii=False)
            fh.write("\n")

    def make_issue_event(self, number: int, issue_type: str, notice: str, labels=None, updated_at="2026-09-02T10:00:00Z"):
        labels = labels if labels is not None else [{"name": "product-inbox"}]
        body = "\n".join([
            "### Typ zgłoszenia",
            issue_type,
            "",
            "### Persona użytkownika",
            "Opiekun",
            "",
            "### Moduł",
            "Główna / Monitoring",
            "",
            "### Co zauważyłeś(-aś) / czego potrzebujesz?",
            notice,
            "",
            "### Dlaczego to ważne?",
            "Bo to ważne.",
            "",
            "### Kontekst / przykład",
            "Przykładowy kontekst.",
        ])
        return {
            "issue": {
                "number": number,
                "html_url": f"https://github.com/example/repo/issues/{number}",
                "url": f"https://api.github.com/repos/example/repo/issues/{number}",
                "created_at": "2026-09-02T09:00:00Z",
                "updated_at": updated_at,
                "title": f"[Skrzynka Produktowa] Test {number}",
                "body": body,
                "labels": labels,
                "user": {"login": "tester"},
            }
        }

    def make_comment_event(self, issue_event: dict, command: str, author: str = "repo-owner"):
        return {
            "repository": {"owner": {"login": "repo-owner"}},
            "issue": issue_event["issue"],
            "comment": {"body": command, "user": {"login": author}},
        }

    def run_import(self, event: dict) -> int:
        event_file = self.root / "event.json"
        self.write_json(event_file, event)
        with self.patched_paths():
            return self.cli.cmd_inbox_import(str(event_file))

    def run_apply_ai(self, issue_number: int, payload) -> int:
        ai_file = self.root / "ai.json"
        if isinstance(payload, str):
            ai_file.write_text(payload, encoding="utf-8")
        else:
            self.write_json(ai_file, payload)
        with self.patched_paths():
            return self.cli.cmd_inbox_apply_ai_review(issue_number, str(ai_file))

    def run_decision(self, event: dict) -> int:
        event_file = self.root / "comment-event.json"
        self.write_json(event_file, event)
        with self.patched_paths(), contextlib.redirect_stdout(io.StringIO()):
            return self.cli.cmd_inbox_handle_decision(str(event_file))

    def run_decision_with_output(self, event: dict) -> tuple[int, str]:
        event_file = self.root / "comment-event.json"
        self.write_json(event_file, event)
        stdout = io.StringIO()
        with self.patched_paths(), contextlib.redirect_stdout(stdout):
            code = self.cli.cmd_inbox_handle_decision(str(event_file))
        return code, stdout.getvalue()

    def run_handoff(self, event: dict, output_name: str = "handoff.json", copilot_assignment_token: str = "copilot-user-token", expected_code: int = 0) -> dict:
        event_file = self.root / "comment-event.json"
        output_file = self.root / output_name
        self.write_json(event_file, event)
        with self.patched_paths(), contextlib.redirect_stdout(io.StringIO()):
            result = self.cli.cmd_inbox_sync_implementation_handoff(
                event_file=str(event_file),
                repo_owner="example",
                repo_name="repo",
                github_token="token",
                copilot_assignment_token=copilot_assignment_token,
                output_file=str(output_file),
            )
        self.assertEqual(expected_code, result)
        return json.loads(output_file.read_text(encoding="utf-8"))

    def run_pr_track(self, payload: dict) -> dict:
        event_file = self.root / "pr-event.json"
        output_file = self.root / "pr-track.json"
        self.write_json(event_file, payload)
        with self.patched_paths():
            result = self.cli.cmd_track_implementation_pr(str(event_file), str(output_file))
        self.assertEqual(0, result)
        return json.loads(output_file.read_text(encoding="utf-8"))

    def run_validate(self) -> int:
        with self.patched_paths():
            return self.cli.cmd_validate()

    def parse_handoff_args(self, repo_name: str) -> tuple[int, dict]:
        captured = {}
        original = self.cli.cmd_inbox_sync_implementation_handoff

        def fake_cmd(**kwargs):
            captured.update(kwargs)
            return 0

        self.cli.cmd_inbox_sync_implementation_handoff = fake_cmd
        try:
            rc = self.cli.main([
                "inbox-sync-implementation-handoff",
                "--event-file",
                "dummy-event.json",
                "--repo-owner=rafalbragan",
                f"--repo-name={repo_name}",
                "--github-token",
                "token",
                "--copilot-assignment-token",
                "user-token",
            ])
        finally:
            self.cli.cmd_inbox_sync_implementation_handoff = original
        return rc, captured

    def req_ids(self):
        reqs = set()
        for p in (self.root / "product" / "requirements").glob("REQ-*.json"):
            reqs.add(p.stem)
        for p in (self.root / "product" / "requirements").glob("REQ-*.yaml"):
            reqs.add(p.stem)
        for p in (self.root / "product" / "requirements").glob("REQ-*.yml"):
            reqs.add(p.stem)
        return reqs

    def implementation_ids(self):
        return {p.stem for p in (self.root / "product" / "implementation").glob("IMP-*.json")}

    def implementation_record(self, implementation_id: str) -> dict:
        return json.loads((self.root / "product" / "implementation" / f"{implementation_id}.json").read_text(encoding="utf-8"))

    def ai_payload(self, issue_number: int, classification: str, decision: str = "HOLD") -> dict:
        return {
            "inbox_id": f"INBOX-GH-{issue_number:06d}",
            "classification": classification,
            "summary": "Analiza AI",
            "evidence_available": ["E1"],
            "evidence_missing": ["M1"],
            "existing_capability_overlap": [],
            "proposed_requirement": "Nowe wymaganie" if classification in {"PRODUCT_PROBLEM", "PRODUCT_OPPORTUNITY", "SAFETY_GAP"} else "",
            "user_value": "Wartość",
            "counterargument": "Kontrargument",
            "simpler_alternative": "Prostsza alternatywa",
            "safety_impact": "LOW",
            "estimated_scope": "MEDIUM",
            "proposed_priority": "P2",
            "recommended_decision": decision,
            "reasoning_summary": "Podsumowanie",
        }

    def make_bug_issue_event(self, number: int, title: str, observed: str, expected: str, repro: str, labels=None):
        labels = labels if labels is not None else [{"name": "bug"}, {"name": "librecare-bug"}]
        body = "\n".join([
            "### Zachowanie obecne",
            observed,
            "",
            "### Zachowanie oczekiwane",
            expected,
            "",
            "### Kroki reprodukcji",
            repro,
            "",
            "### Powiazane REQ",
            "REQ-0002",
        ])
        return {
            "issue": {
                "number": number,
                "html_url": f"https://github.com/example/repo/issues/{number}",
                "url": f"https://api.github.com/repos/example/repo/issues/{number}",
                "created_at": "2026-09-02T09:00:00Z",
                "updated_at": "2026-09-02T10:00:00Z",
                "title": title,
                "body": body,
                "labels": labels,
                "user": {"login": "tester"},
            }
        }

    def run_bug_import(self, event: dict) -> int:
        if not hasattr(self.cli, "cmd_bug_import"):
            self.skipTest("bug automation commands are covered in automation_cli tests")
        event_file = self.root / "bug-event.json"
        self.write_json(event_file, event)
        with self.patched_paths():
            return self.cli.cmd_bug_import(str(event_file))

    def run_bug_triage(self, bug_id: str, payload: dict) -> int:
        if not hasattr(self.cli, "cmd_bug_apply_ai_triage"):
            self.skipTest("bug automation commands are covered in automation_cli tests")
        triage_file = self.root / "bug-triage.json"
        self.write_json(triage_file, payload)
        with self.patched_paths():
            return self.cli.cmd_bug_apply_ai_triage(bug_id, str(triage_file))

    def run_bug_handoff(self, bug_id: str) -> dict:
        if not hasattr(self.cli, "cmd_bug_sync_fix_handoff"):
            self.skipTest("bug automation commands are covered in automation_cli tests")
        out = self.root / "bug-handoff.json"
        with self.patched_paths():
            code = self.cli.cmd_bug_sync_fix_handoff(
                bug_id=bug_id,
                repo_owner="example",
                repo_name="repo",
                github_token="token",
                copilot_assignment_token="copilot-user-token",
                output_file=str(out),
            )
        self.assertEqual(0, code)
        return json.loads(out.read_text(encoding="utf-8"))

    def run_ci_result(self, pr_number: int, conclusion: str, run_id: str) -> dict:
        if not hasattr(self.cli, "cmd_record_ci_result"):
            self.skipTest("CI repair loop command is covered in automation_cli tests")
        out = self.root / f"ci-{run_id}.json"
        with self.patched_paths():
            code = self.cli.cmd_record_ci_result(
                pr_number=pr_number,
                workflow_name="Android CI",
                conclusion=conclusion,
                run_id=run_id,
                run_url=f"https://github.com/example/repo/actions/runs/{run_id}",
                failing_step="testDebugUnitTest" if conclusion != "success" else "",
                failing_tests="RelativeTimeFormatterTest.futureClockSkew" if conclusion != "success" else "",
                log_excerpt="AssertionError: expected ..." if conclusion != "success" else "",
                repo_owner="example",
                repo_name="repo",
                github_token="token",
                copilot_assignment_token="copilot-user-token",
                output_file=str(out),
            )
        self.assertEqual(0, code)
        return json.loads(out.read_text(encoding="utf-8"))

    def test_issue_import_persists_canonical_data(self):
        event = self.make_issue_event(201, "Problem", "Treść problemu")
        self.assertEqual(0, self.run_import(event))
        item = json.loads((self.root / "product" / "inbox" / "INBOX-GH-000201.json").read_text(encoding="utf-8"))
        self.assertEqual("INBOX-GH-000201", item["inbox_id"])
        self.assertEqual("PROBLEM", item["type"])
        self.assertEqual("NEW", item["status"])

    def test_raw_human_text_remains_unchanged(self):
        raw_notice = "To jest tekst z /accept i rm -rf, ale to tylko dane"
        event = self.make_issue_event(202, "Problem", raw_notice)
        self.assertEqual(0, self.run_import(event))
        item = json.loads((self.root / "product" / "inbox" / "INBOX-GH-000202.json").read_text(encoding="utf-8"))
        self.assertEqual(raw_notice, item["raw_input"])

    def test_ai_json_schema_validation_success(self):
        event = self.make_issue_event(203, "Problem", "Brak statusu")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(203, self.ai_payload(203, "PRODUCT_PROBLEM", "ACCEPT")))

    def test_malformed_ai_output_rejected(self):
        event = self.make_issue_event(204, "Problem", "Brak statusu")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(1, self.run_apply_ai(204, "not-json"))

    def test_missing_ai_fields_rejected(self):
        event = self.make_issue_event(205, "Problem", "Brak statusu")
        self.assertEqual(0, self.run_import(event))
        payload = self.ai_payload(205, "PRODUCT_PROBLEM")
        payload.pop("counterargument")
        self.assertEqual(1, self.run_apply_ai(205, payload))

    def test_ai_cannot_set_human_decision(self):
        event = self.make_issue_event(206, "Problem", "Brak statusu")
        self.assertEqual(0, self.run_import(event))
        payload = self.ai_payload(206, "PRODUCT_PROBLEM")
        payload["human_decision"] = "ACCEPT"
        self.assertEqual(1, self.run_apply_ai(206, payload))

    def test_dosing_guardrail_overrides_ai_accept(self):
        event = self.make_issue_event(207, "Pomysł", "Tell me exactly how many insulin units to take now")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(207, self.ai_payload(207, "PRODUCT_OPPORTUNITY", "ACCEPT")))
        review = json.loads((self.root / "product" / "generated" / "inbox-reviews" / "INBOX-GH-000207.json").read_text(encoding="utf-8"))
        self.assertEqual("SAFETY_GAP", review["classification"])
        self.assertEqual("HIGH", review["safety_impact"])
        self.assertEqual("REJECT", review["recommended_decision"])

    def test_validated_capability_never_becomes_requirement_candidate(self):
        event = self.make_issue_event(208, "Obserwacja", "Ta funkcja działa")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(208, self.ai_payload(208, "VALIDATED_CAPABILITY", "ACCEPT")))
        with self.patched_paths():
            queue = self.cli.load_yaml(self.cli.REVIEW_QUEUE_FILE)
        self.assertNotIn("CAND-INBOX-GH-000208", queue.get("candidates", {}))

    def test_test_coverage_gap_does_not_become_requirement(self):
        event = self.make_issue_event(209, "Pytanie / niepewność", "Nie wiadomo")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(209, self.ai_payload(209, "TEST_COVERAGE_GAP", "ACCEPT")))
        with self.patched_paths():
            queue = self.cli.load_yaml(self.cli.REVIEW_QUEUE_FILE)
        self.assertNotIn("CAND-INBOX-GH-000209", queue.get("candidates", {}))

    def test_product_problem_can_enter_human_review(self):
        event = self.make_issue_event(210, "Problem", "Realny problem")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(210, self.ai_payload(210, "PRODUCT_PROBLEM", "ACCEPT")))
        with self.patched_paths():
            queue = self.cli.load_yaml(self.cli.REVIEW_QUEUE_FILE)
        self.assertIn("CAND-INBOX-GH-000210", queue.get("candidates", {}))

    def test_product_opportunity_can_enter_human_review(self):
        event = self.make_issue_event(211, "Pomysł", "Szansa")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(211, self.ai_payload(211, "PRODUCT_OPPORTUNITY", "HOLD")))
        with self.patched_paths():
            queue = self.cli.load_yaml(self.cli.REVIEW_QUEUE_FILE)
        self.assertIn("CAND-INBOX-GH-000211", queue.get("candidates", {}))

    def test_accept_creates_one_canonical_requirement_and_is_idempotent(self):
        event = self.make_issue_event(212, "Problem", "Nowe wymaganie")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(212, self.ai_payload(212, "PRODUCT_PROBLEM", "ACCEPT")))
        before = self.req_ids()
        impl_before = self.implementation_ids()
        cmd_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(cmd_event))
        first_after = self.req_ids()
        self.assertEqual(len(before) + 1, len(first_after))
        req_id = sorted(first_after - before)[0]
        req_path = self.root / "product" / "requirements" / f"{req_id}.yaml"
        self.assertTrue(req_path.exists())
        requirement = self.cli.load_yaml(req_path)
        self.assertEqual("ACCEPTED", requirement["status"])
        self.assertEqual("QUEUED", requirement["implementation"]["implementation_status"])
        self.assertEqual(f"IMP-{req_id}", requirement["implementation"]["implementation_id"])
        self.assertEqual(impl_before | {f"IMP-{req_id}"}, self.implementation_ids())
        implementation = self.implementation_record(f"IMP-{req_id}")
        self.assertEqual(req_id, implementation["requirement_id"])
        self.assertEqual("QUEUED", implementation["status"])
        self.assertEqual("PENDING", implementation["validation_state"])
        self.assertEqual(0, self.run_decision(cmd_event))
        self.assertEqual(first_after, self.req_ids())
        self.assertEqual(impl_before | {f"IMP-{req_id}"}, self.implementation_ids())

    def test_accept_retry_reuses_existing_requirement_when_queue_marker_missing(self):
        event = self.make_issue_event(230, "Problem", "Retry bez duplikatow")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(230, self.ai_payload(230, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))

        first_req_ids = self.req_ids()
        req_id = sorted([rid for rid in first_req_ids if rid.startswith("REQ-")])[-1]
        first_impl_ids = self.implementation_ids()

        with self.patched_paths():
            queue = self.cli.load_yaml(self.cli.REVIEW_QUEUE_FILE)
            queue["candidates"]["CAND-INBOX-GH-000230"].pop("applied_requirement_id", None)
            self.cli.save_yaml(self.cli.REVIEW_QUEUE_FILE, queue)

        self.assertEqual(0, self.run_decision(decision_event))
        self.assertEqual(first_req_ids, self.req_ids())
        self.assertEqual(first_impl_ids, self.implementation_ids())
        with self.patched_paths():
            queue = self.cli.load_yaml(self.cli.REVIEW_QUEUE_FILE)
        self.assertEqual(req_id, queue["candidates"]["CAND-INBOX-GH-000230"]["applied_requirement_id"])

    def test_accept_creates_one_implementation_issue_and_assignment_once(self):
        event = self.make_issue_event(217, "Problem", "Naturalny czas świeżości")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(217, self.ai_payload(217, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))

        first = self.run_handoff(decision_event)
        second = self.run_handoff(decision_event, output_name="handoff-second.json")

        self.assertFalse(first["blocked"])
        self.assertEqual(first["implementation_id"], second["implementation_id"])
        self.assertEqual(first["implementation_issue_number"], second["implementation_issue_number"])
        self.assertEqual(1, len(self.fake_client.created_issues))
        self.assertEqual(1, len(self.fake_client.assigned))
        implementation = self.implementation_record(first["implementation_id"])
        self.assertEqual("AGENT_ASSIGNED", implementation["status"])
        self.assertEqual(first["implementation_issue_number"], implementation["implementation_issue_number"])
        self.assertEqual("ASSIGNED", implementation["copilot_assignment"]["status"])

    def test_label_without_real_assignee_is_not_treated_as_assigned(self):
        event = self.make_issue_event(240, "Problem", "Weryfikacja realnego przypisania")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(240, self.ai_payload(240, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))

        req_id = sorted(self.req_ids())[-1]
        req_path = self.root / "product" / "requirements" / f"{req_id}.yaml"
        requirement = self.cli.load_yaml(req_path)
        requirement["implementation"]["implementation_issue"] = {
            "number": 7,
            "url": "https://github.com/example/repo/issues/7",
            "title": "[Implementacja] test",
            "assigned_agent": "copilot-swe-agent[bot]",
            "agent_assignment": {"status": "ASSIGNED"},
        }
        requirement["implementation"]["implementation_status"] = "AGENT_ASSIGNED"
        self.cli.save_yaml(req_path, requirement)

        self.fake_client.issue_assignees[7] = []
        self.fake_client.assign_should_fail = True
        summary = self.run_handoff(decision_event, output_name="handoff-unassigned.json", expected_code=1)

        self.assertEqual("QUEUED", summary["status"])
        self.assertFalse(summary["copilot_real_assignee_confirmed"])
        self.assertIn("LIBRECARE_COPILOT_ASSIGNMENT_STATUS", summary.get("comment_markdown", ""))
        refreshed = self.cli.load_yaml(req_path)
        self.assertEqual("QUEUED", refreshed["implementation"]["implementation_status"])
        self.assertNotIn("assigned_agent", refreshed["implementation"].get("implementation_issue", {}))

    def test_repeated_handoff_after_failed_assignment_reuses_issue(self):
        event = self.make_issue_event(241, "Problem", "Retry po błędzie assignment")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(241, self.ai_payload(241, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))

        self.fake_client.assign_should_fail = True
        first = self.run_handoff(decision_event, output_name="handoff-fail-1.json", expected_code=1)
        self.assertEqual("QUEUED", first["status"])
        issue_number = first["implementation_issue_number"]
        self.assertEqual(1, len(self.fake_client.created_issues))

        second = self.run_handoff(decision_event, output_name="handoff-fail-2.json", expected_code=1)
        self.assertEqual(issue_number, second["implementation_issue_number"])
        self.assertEqual(1, len(self.fake_client.created_issues))

    def test_repeated_successful_handoff_creates_no_duplicate_issue_or_assignment(self):
        event = self.make_issue_event(242, "Problem", "Ponowienie po sukcesie")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(242, self.ai_payload(242, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))

        first = self.run_handoff(decision_event, output_name="handoff-success-1.json")
        second = self.run_handoff(decision_event, output_name="handoff-success-2.json")
        self.assertTrue(first["copilot_real_assignee_confirmed"])
        self.assertTrue(second["copilot_real_assignee_confirmed"])
        self.assertEqual(first["implementation_issue_number"], second["implementation_issue_number"])
        self.assertEqual(1, len(self.fake_client.created_issues))
        self.assertEqual(1, len(self.fake_client.assigned))

    def test_assignment_uses_dedicated_user_token(self):
        event = self.make_issue_event(246, "Problem", "User token for Copilot assignment")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(246, self.ai_payload(246, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))

        summary = self.run_handoff(decision_event, output_name="handoff-user-token.json", copilot_assignment_token="copilot-user-token")
        self.assertEqual("AGENT_ASSIGNED", summary["status"])
        self.assertEqual(["copilot-user-token"], self.fake_client.assignment_tokens)

    def test_missing_assignment_token_keeps_queue_and_posts_polish_comment(self):
        event = self.make_issue_event(247, "Problem", "Missing Copilot user token")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(247, self.ai_payload(247, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))

        summary = self.run_handoff(decision_event, output_name="handoff-missing-user-token.json", copilot_assignment_token="", expected_code=1)
        self.assertEqual("QUEUED", summary["status"])
        self.assertFalse(summary["copilot_real_assignee_confirmed"])
        self.assertEqual("HANDOFF_FAILED", summary["handoff_result"])
        self.assertEqual([], self.fake_client.assignment_tokens)
        self.assertIn("COPILOT_AGENT_USER_TOKEN", summary.get("comment_markdown", ""))

    def test_ci_repair_uses_dedicated_user_token(self):
        event = self.make_issue_event(248, "Problem", "Retry after CI failure")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(248, self.ai_payload(248, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))

        handoff = self.run_handoff(decision_event, output_name="handoff-ci-token.json", copilot_assignment_token="copilot-user-token")
        self.run_pr_track({
            "action": "opened",
            "pull_request": {
                "number": 90,
                "html_url": "https://github.com/example/repo/pull/90",
                "title": f"{handoff['req_id']} — implementacja",
                "body": f"Closes #{handoff['implementation_issue_number']}\n{handoff['req_id']}",
                "state": "open",
                "merged": False,
                "head": {"ref": "copilot/fix-90"},
            },
        })
        self.fake_client.assignment_tokens = []
        summary = self.run_ci_result(90, "failure", "2005")
        self.assertEqual("REPAIRING", summary["status"])
        self.assertTrue(summary["repair_attempted"])
        self.assertTrue(summary["repair_assignment_verified"])
        self.assertEqual(["copilot-user-token"], self.fake_client.assignment_tokens)

    def test_existing_implementation_record_issue_is_reused_without_search_or_create(self):
        event = self.make_issue_event(243, "Problem", "Reuse issue from IMP record")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(243, self.ai_payload(243, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))

        req_id = sorted([rid for rid in self.req_ids() if rid.startswith("REQ-")])[-1]
        impl_id = f"IMP-{req_id}"
        impl_path = self.root / "product" / "implementation" / f"{impl_id}.json"
        impl = json.loads(impl_path.read_text(encoding="utf-8"))
        impl["implementation_issue_number"] = 7
        impl["implementation_issue_url"] = "https://github.com/example/repo/issues/7"
        impl["copilot_assignment"] = {}
        impl["status"] = "QUEUED"
        self.write_json(impl_path, impl)

        req_path = self.root / "product" / "requirements" / f"{req_id}.yaml"
        requirement = self.cli.load_yaml(req_path)
        requirement["implementation"]["implementation_issue"] = {}
        requirement["implementation"]["implementation_status"] = "QUEUED"
        self.cli.save_yaml(req_path, requirement)

        self.fake_client.issue_assignees[7] = []
        summary = self.run_handoff(decision_event, output_name="handoff-imp-reuse.json")
        self.assertEqual(7, summary["implementation_issue_number"])
        self.assertEqual(0, self.fake_client.find_existing_calls)
        self.assertEqual(0, len(self.fake_client.created_issues))
        self.assertEqual(1, len(self.fake_client.assigned))
        self.assertEqual(7, self.fake_client.assigned[0]["issue_number"])

    def test_existing_issue_lookup_by_req_identity_reuses_issue(self):
        event = self.make_issue_event(244, "Problem", "Lookup issue by REQ label")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(244, self.ai_payload(244, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))

        req_id = sorted([rid for rid in self.req_ids() if rid.startswith("REQ-")])[-1]
        self.fake_client.existing_issues_by_req[req_id] = {
            "number": 7,
            "html_url": "https://github.com/example/repo/issues/7",
            "title": f"[Implementacja] {req_id} — Existing",
            "labels": [{"name": "implementation"}, {"name": req_id}, {"name": "copilot"}],
            "body": f"<!-- LIBRECARE_REQUIREMENT_ID: {req_id} -->",
        }
        self.fake_client.issue_assignees[7] = []

        summary = self.run_handoff(decision_event, output_name="handoff-lookup-reuse.json")
        self.assertEqual(7, summary["implementation_issue_number"])
        self.assertEqual(1, self.fake_client.find_existing_calls)
        self.assertEqual(0, len(self.fake_client.created_issues))
        self.assertEqual(1, len(self.fake_client.assigned))

    def test_existing_issue_with_confirmed_assignee_skips_duplicate_assignment(self):
        event = self.make_issue_event(245, "Problem", "No duplicate assignment")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(245, self.ai_payload(245, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))

        req_id = sorted([rid for rid in self.req_ids() if rid.startswith("REQ-")])[-1]
        req_path = self.root / "product" / "requirements" / f"{req_id}.yaml"
        requirement = self.cli.load_yaml(req_path)
        requirement["implementation"]["implementation_issue"] = {
            "number": 7,
            "url": "https://github.com/example/repo/issues/7",
            "title": f"[Implementacja] {req_id}",
            "agent_assignment": {"status": "ASSIGNED"},
        }
        requirement["implementation"]["implementation_status"] = "QUEUED"
        self.cli.save_yaml(req_path, requirement)
        self.fake_client.issue_assignees[7] = ["copilot-swe-agent[bot]"]

        summary = self.run_handoff(decision_event, output_name="handoff-already-assigned.json")
        self.assertTrue(summary["copilot_real_assignee_confirmed"])
        self.assertEqual("AGENT_ASSIGNED", summary["status"])
        self.assertEqual(0, len(self.fake_client.assigned))
        self.assertEqual(0, len(self.fake_client.created_issues))

    def test_handoff_cli_parses_repo_name_without_hyphen(self):
        rc, captured = self.parse_handoff_args("LibreDisplay")
        self.assertEqual(0, rc)
        self.assertEqual("rafalbragan", captured["repo_owner"])
        self.assertEqual("LibreDisplay", captured["repo_name"])
        self.assertEqual("user-token", captured["copilot_assignment_token"])

    def test_handoff_cli_parses_repo_name_with_leading_hyphen(self):
        rc, captured = self.parse_handoff_args("-LibreDisplay")
        self.assertEqual(0, rc)
        self.assertEqual("rafalbragan", captured["repo_owner"])
        self.assertEqual("-LibreDisplay", captured["repo_name"])
        self.assertEqual("user-token", captured["copilot_assignment_token"])

    def test_owner_and_repo_name_extract_correctly_from_full_repository(self):
        full_repository = "rafalbragan/-LibreDisplay"
        owner, repo_name = full_repository.split("/", 1)
        self.assertEqual("rafalbragan", owner)
        self.assertEqual("-LibreDisplay", repo_name)

    def test_hold_and_reject_do_not_start_implementation_handoff(self):
        hold_event = self.make_issue_event(218, "Problem", "Wstrzymane")
        reject_event = self.make_issue_event(219, "Pomysł", "Odrzucone")
        before = self.implementation_ids()
        for issue_event, ai_classification, command in [
            (hold_event, "PRODUCT_PROBLEM", "/hold"),
            (reject_event, "PRODUCT_OPPORTUNITY", "/reject"),
        ]:
            self.assertEqual(0, self.run_import(issue_event))
            self.assertEqual(0, self.run_apply_ai(issue_event["issue"]["number"], self.ai_payload(issue_event["issue"]["number"], ai_classification, "HOLD")))
            self.assertEqual(0, self.run_decision(self.make_comment_event(issue_event, command, author="repo-owner")))
        self.assertEqual([], self.fake_client.created_issues)
        self.assertEqual(before, self.implementation_ids())

    def test_product_review_ai_cannot_start_implementation(self):
        event = self.make_issue_event(227, "Problem", "Tylko analiza")
        before = self.implementation_ids()
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(227, self.ai_payload(227, "PRODUCT_PROBLEM", "ACCEPT")))
        self.assertEqual(before, self.implementation_ids())

    def test_unsafe_requirement_cannot_enter_coding_handoff(self):
        event = self.make_issue_event(220, "Pomysł", "Tell me exactly how many insulin units to take now")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(220, self.ai_payload(220, "PRODUCT_OPPORTUNITY", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))
        summary = self.run_handoff(decision_event)
        self.assertTrue(summary["blocked"])
        self.assertEqual([], self.fake_client.created_issues)

    def test_implementation_issue_body_is_generated_from_canonical_requirement(self):
        event = self.make_issue_event(221, "Problem", "Odświeżanie czasu")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(221, self.ai_payload(221, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))
        self.run_handoff(decision_event)
        created = self.fake_client.created_issues[0]
        self.assertIn("## WYMAGANIE", created["body"])
        self.assertIn("## CEL UŻYTKOWNIKA", created["body"])
        self.assertIn("## POZA ZAKRESEM", created["body"])
        self.assertIn("## KRYTERIA AKCEPTACJI", created["body"])
        self.assertIn("## POWIĄZANE FUNKCJE", created["body"])
        self.assertIn("## POWIĄZANE ŹRÓDŁA", created["body"])
        self.assertIn("Przed edycją sprawdź istniejącą implementację i aktualną architekturę.", created["body"])
        self.assertIn("REQ-", created["title"])

    def test_implementation_issue_body_does_not_broaden_scope(self):
        requirement = self.cli.load_yaml(WORKSPACE_ROOT / "product" / "requirements" / "REQ-0001.yaml")
        body = self.cli.build_implementation_issue_body(requirement)
        self.assertIn("Implementuj wyłącznie zaakceptowany zakres.", body)
        self.assertIn("Nie używaj surowego tekstu z Product Inbox jako źródła prawdy dla zakresu implementacji.", body)
        self.assertNotIn("invent", body.lower())

    def test_pr_link_can_be_recorded_and_merge_does_not_validate(self):
        event = self.make_issue_event(222, "Problem", "PR tracking")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(222, self.ai_payload(222, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))
        handoff = self.run_handoff(decision_event)
        req_id = handoff["req_id"]

        open_summary = self.run_pr_track({
            "action": "opened",
            "pull_request": {
                "number": 27,
                "html_url": "https://github.com/example/repo/pull/27",
                "title": f"{req_id} — implementacja",
                "body": f"Closes #{handoff['implementation_issue_number']}\n{req_id}",
                "state": "open",
                "merged": False,
                "head": {"ref": "copilot/req-222"},
            }
        })
        self.assertEqual("PR_READY", open_summary["status"])
        self.assertEqual("copilot/req-222", open_summary["branch"])
        self.assertIn("Pull Request:", open_summary["comment_markdown"])
        self.assertIn("Wymagany przegląd człowieka przed scaleniem.", open_summary["comment_markdown"])

        merged_summary = self.run_pr_track({
            "action": "closed",
            "pull_request": {
                "number": 27,
                "html_url": "https://github.com/example/repo/pull/27",
                "title": f"{req_id} — implementacja",
                "body": f"Closes #{handoff['implementation_issue_number']}\n{req_id}",
                "state": "closed",
                "merged": True,
                "head": {"ref": "copilot/req-222"},
            }
        })
        self.assertEqual("MERGED", merged_summary["status"])
        with self.patched_paths():
            req_path, requirement = self.cli.load_requirement_by_id(req_id)
        self.assertIsNotNone(req_path)
        self.assertEqual("MERGED", requirement["implementation"]["implementation_status"])
        self.assertEqual("VALIDATION_PENDING", requirement["implementation"]["validation_state"])
        implementation = self.implementation_record(f"IMP-{req_id}")
        self.assertEqual("MERGED", implementation["status"])
        self.assertEqual("VALIDATION_PENDING", implementation["validation_state"])
        self.assertEqual("copilot/req-222", implementation["branch"])

    def test_requirement_pr_mentioning_bug_history_stays_requirement(self):
        event = self.make_issue_event(229, "Problem", "REQ tracking with bug mention")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(229, self.ai_payload(229, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))
        handoff = self.run_handoff(decision_event)
        summary = self.run_pr_track({
            "action": "opened",
            "pull_request": {
                "number": 35,
                "html_url": "https://github.com/example/repo/pull/35",
                "title": f"{handoff['req_id']} — implementacja z historią BUG-0003",
                "body": f"Closes #{handoff['implementation_issue_number']}\nHistoria: BUG-0003",
                "state": "open",
                "merged": False,
                "head": {"ref": "copilot/req-229"},
            }
        })
        self.assertEqual(handoff["req_id"], summary["req_id"])
        self.assertEqual("PR_READY", summary["status"])

    def test_validate_passes_with_canonical_implementation_record(self):
        event = self.make_issue_event(228, "Problem", "Walidacja implementacji")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(228, self.ai_payload(228, "PRODUCT_PROBLEM", "ACCEPT")))
        self.assertEqual(0, self.run_decision(self.make_comment_event(event, "/accept", author="repo-owner")))
        self.assertEqual(0, self.run_validate())

    def test_validate_passes_for_issue5_bug_without_implementation_record(self):
        bug_path = self.root / "product" / "bugs" / "BUG-0001.json"
        self.assertTrue(bug_path.exists())
        bug = json.loads(bug_path.read_text(encoding="utf-8"))
        self.assertEqual("GITHUB_BUG_ISSUE", bug.get("source"))
        self.assertEqual(5, bug.get("source_issue_number"))
        self.assertEqual("NEEDS_PRODUCT_DECISION", bug.get("status"))
        self.assertFalse((self.root / "product" / "implementation" / "IMP-BUG-0001.json").exists())
        self.assertEqual(0, self.run_validate())

    def test_validate_fails_for_inconclusive_bug_with_active_bug_impl(self):
        impl_path = self.root / "product" / "implementation" / "IMP-BUG-0003.json"
        self.write_json(impl_path, {
            "implementation_id": "IMP-BUG-0003",
            "requirement_id": None,
            "bug_id": "BUG-0003",
            "source_inbox_id": None,
            "source_issue_number": None,
            "implementation_issue_number": 9,
            "implementation_issue_url": "https://github.com/example/repo/issues/9",
            "copilot_assignment": {"status": "ASSIGNMENT_FAILED"},
            "branch": None,
            "pull_request_number": None,
            "pull_request_url": None,
            "status": "QUEUED",
            "created_at": "2026-09-03T00:00:00Z",
            "updated_at": "2026-09-03T00:00:00Z",
            "attempt_count": 0,
            "last_ci_result": "UNKNOWN",
            "validation_state": "PENDING",
            "acceptance_test_ids": [],
            "ci_failures": [],
        })
        self.assertEqual(1, self.run_validate())

    def test_validate_fails_when_bug_review_mismatches_canonical_bug(self):
        review_path = self.root / "product" / "generated" / "bug-reviews" / "BUG-0002.json"
        review = json.loads(review_path.read_text(encoding="utf-8"))
        review["classification"] = "CONFIRMED_DEFECT"
        self.write_json(review_path, review)
        self.assertEqual(1, self.run_validate())

    def test_req0002_bootstrap_implementation_record_is_honest(self):
        implementation = self.implementation_record("IMP-REQ-0002")
        self.assertEqual("REQ-0002", implementation["requirement_id"])
        self.assertEqual("VALIDATED", implementation["status"])
        self.assertIsNone(implementation["implementation_issue_number"])
        self.assertIsNone(implementation["pull_request_number"])
        self.assertIn("bootstrap", implementation["bootstrap_note"].lower())

    def test_confirmed_bug_can_be_auto_handed_off_once(self):
        bug_event = self.make_bug_issue_event(
            301,
            "[Blad LibreCare] Raw ISO timestamp",
            "Na karcie widoczny jest surowy ISO.",
            "Karta powinna pokazywac naturalny czas.",
            "1. Otworz Home\n2. Zobacz karte osoby",
        )
        self.assertEqual(0, self.run_bug_import(bug_event))
        bug_path = self.root / "product" / "bugs" / "BUG-0001.json"
        self.assertTrue(bug_path.exists())
        triage_payload = {
            "classification": "CONFIRMED_DEFECT",
            "reasoning": "Regresja wobec REQ-0002.",
            "severity": "HIGH",
            "safety_impact": "LOW",
            "requires_behavior_change": False,
            "recommended_related_requirements": ["REQ-0002"],
            "recommended_related_tests": ["TESTRUN-2026-006"],
        }
        self.assertEqual(0, self.run_bug_triage("BUG-0001", triage_payload))
        first = self.run_bug_handoff("BUG-0001")
        second = self.run_bug_handoff("BUG-0001")
        self.assertEqual(first["implementation_issue_number"], second["implementation_issue_number"])
        self.assertEqual(1, len(self.fake_client.created_issues))

    def test_bug_new_behavior_request_routes_to_product_decision(self):
        bug_event = self.make_bug_issue_event(
            302,
            "[Blad LibreCare] New threshold request",
            "Aplikacja nie zmienia progow medycznych.",
            "Prosba o nowy dynamiczny prog.",
            "1. Otworz ustawienia",
        )
        self.assertEqual(0, self.run_bug_import(bug_event))
        triage_payload = {
            "classification": "CONFIRMED_DEFECT",
            "reasoning": "To jest rozszerzenie zakresu.",
            "severity": "MEDIUM",
            "safety_impact": "LOW",
            "requires_behavior_change": True,
            "recommended_related_requirements": [],
            "recommended_related_tests": [],
        }
        self.assertEqual(0, self.run_bug_triage("BUG-0001", triage_payload))
        bug = json.loads((self.root / "product" / "bugs" / "BUG-0001.json").read_text(encoding="utf-8"))
        self.assertEqual("NEEDS_PRODUCT_DECISION", bug["status"])

    def test_ci_failure_triggers_bounded_repair_and_limit(self):
        event = self.make_issue_event(303, "Problem", "Napraw workflow")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(303, self.ai_payload(303, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))
        handoff = self.run_handoff(decision_event)
        self.run_pr_track({
            "action": "opened",
            "pull_request": {
                "number": 88,
                "html_url": "https://github.com/example/repo/pull/88",
                "title": f"{handoff['req_id']} — implementacja",
                "body": f"Closes #{handoff['implementation_issue_number']}\n{handoff['req_id']}",
                "state": "open",
                "merged": False,
                "head": {"ref": "copilot/fix-88"},
            },
        })

        fail1 = self.run_ci_result(88, "failure", "1001")
        self.assertEqual("REPAIRING", fail1["status"])
        self.assertFalse(fail1["auto_merge"])
        self.assertEqual(1, fail1["attempt_count"])
        fail_dup = self.run_ci_result(88, "failure", "1001")
        self.assertEqual("DEDUPLICATED", fail_dup["action"])
        self.assertEqual(1, fail_dup["attempt_count"])
        fail2 = self.run_ci_result(88, "failure", "1002")
        self.assertEqual("REPAIRING", fail2["status"])
        fail3 = self.run_ci_result(88, "failure", "1003")
        self.assertEqual("FAILED", fail3["status"])
        self.assertIn("implementacja wymaga uwagi", fail3["comment_markdown"].lower())
        fail4 = self.run_ci_result(88, "failure", "1004")
        self.assertEqual("FAILED", fail4["status"])
        self.assertEqual("STOPPED_TERMINAL", fail4["action"])

    def test_ci_failure_then_success_sets_ready_for_human_review(self):
        event = self.make_issue_event(304, "Problem", "Napraw test")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(304, self.ai_payload(304, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))
        handoff = self.run_handoff(decision_event)
        self.run_pr_track({
            "action": "opened",
            "pull_request": {
                "number": 89,
                "html_url": "https://github.com/example/repo/pull/89",
                "title": f"{handoff['req_id']} — implementacja",
                "body": f"Closes #{handoff['implementation_issue_number']}\n{handoff['req_id']}",
                "state": "open",
                "merged": False,
                "head": {"ref": "copilot/fix-89"},
            },
        })
        self.run_ci_result(89, "failure", "2001")
        success = self.run_ci_result(89, "success", "2002")
        self.assertEqual("READY_FOR_HUMAN_REVIEW", success["status"])
        self.assertFalse(success["auto_merge"])
        self.assertIn("Wymagany przegląd człowieka przed scaleniem.", success["comment_markdown"])

    def test_failed_ci_result_can_never_transition_to_merged(self):
        event = self.make_issue_event(305, "Problem", "Brak auto merge po fail CI")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(305, self.ai_payload(305, "PRODUCT_PROBLEM", "ACCEPT")))
        decision_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(decision_event))
        handoff = self.run_handoff(decision_event)
        self.run_pr_track({
            "action": "opened",
            "pull_request": {
                "number": 95,
                "html_url": "https://github.com/example/repo/pull/95",
                "title": f"{handoff['req_id']} — implementacja",
                "body": f"Closes #{handoff['implementation_issue_number']}\n{handoff['req_id']}",
                "state": "open",
                "merged": False,
                "head": {"ref": "copilot/fix-95"},
            }
        })
        failure = self.run_ci_result(95, "failure", "3005fail")
        self.assertNotEqual("MERGED", failure["status"])
        implementation = self.implementation_record(handoff["implementation_id"])
        self.assertNotEqual("MERGED", implementation["status"])

    def test_polish_presentation_does_not_change_internal_enums(self):
        review = self.ai_payload(223, "PRODUCT_PROBLEM", "ACCEPT")
        comment = self.cli.build_issue_review_comment(review)
        self.assertIn("## LibreCare — analiza produktu", comment)
        self.assertEqual("PRODUCT_PROBLEM", review["classification"])
        self.assertEqual("ACCEPT", review["recommended_decision"])

    def test_issue_review_comment_uses_required_polish_headings_and_commands(self):
        review = self.ai_payload(224, "PRODUCT_PROBLEM", "ACCEPT")
        comment = self.cli.build_issue_review_comment(review)
        for heading in [
            "## LibreCare — analiza produktu",
            "Klasyfikacja:",
            "Jak rozumiem problem:",
            "Dostępne dowody:",
            "Brakujące dowody:",
            "Powiązanie z istniejącymi funkcjami:",
            "Proponowane wymaganie:",
            "Dlaczego może być wartościowe:",
            "Kontrargument:",
            "Prostsza alternatywa:",
            "Wpływ na bezpieczeństwo:",
            "Szacowany zakres:",
            "Proponowany priorytet:",
            "Rekomendacja AI:",
            "Decyzja człowieka:",
            "Aby podjąć decyzję, wpisz dokładnie:",
        ]:
            self.assertIn(heading, comment)
        self.assertIn("/accept", comment)
        self.assertIn("/hold", comment)
        self.assertIn("/reject", comment)

    def test_decision_output_is_polish(self):
        event = self.make_issue_event(225, "Problem", "Naturalny czas")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(225, self.ai_payload(225, "PRODUCT_PROBLEM", "ACCEPT")))
        code, output = self.run_decision_with_output(self.make_comment_event(event, "/accept", author="repo-owner"))
        self.assertEqual(0, code)
        self.assertIn("## LibreCare — decyzja produktowa", output)
        self.assertIn("Decyzja:", output)
        self.assertIn("ZAAKCEPTOWANE", output)
        self.assertIn("Utworzone wymaganie:", output)
        self.assertIn("Status:", output)

    def test_non_owner_decision_skip_message_is_polish(self):
        event = self.make_issue_event(226, "Problem", "Owner only")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(226, self.ai_payload(226, "PRODUCT_PROBLEM", "ACCEPT")))
        code, output = self.run_decision_with_output(self.make_comment_event(event, "/accept", author="random-user"))
        self.assertEqual(0, code)
        self.assertIn("POMINIĘTO: komenda decyzyjna zignorowana", output)

    def test_hold_creates_no_requirement(self):
        event = self.make_issue_event(213, "Problem", "Wstrzymaj")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(213, self.ai_payload(213, "PRODUCT_PROBLEM", "HOLD")))
        before = self.req_ids()
        self.assertEqual(0, self.run_decision(self.make_comment_event(event, "/hold", author="repo-owner")))
        self.assertEqual(before, self.req_ids())

    def test_reject_creates_no_requirement(self):
        event = self.make_issue_event(214, "Pomysł", "Odrzuć")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(214, self.ai_payload(214, "PRODUCT_OPPORTUNITY", "HOLD")))
        before = self.req_ids()
        self.assertEqual(0, self.run_decision(self.make_comment_event(event, "/reject", author="repo-owner")))
        self.assertEqual(before, self.req_ids())

    def test_non_owner_decision_ignored(self):
        event = self.make_issue_event(215, "Problem", "Owner only")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(215, self.ai_payload(215, "PRODUCT_PROBLEM", "ACCEPT")))
        before = self.req_ids()
        self.assertEqual(0, self.run_decision(self.make_comment_event(event, "/accept", author="random-user")))
        self.assertEqual(before, self.req_ids())

    def test_issue_edit_updates_same_inbox_item(self):
        event_v1 = self.make_issue_event(216, "Problem", "Wersja 1", updated_at="2026-09-02T10:00:00Z")
        event_v2 = self.make_issue_event(216, "Problem", "Wersja 2", updated_at="2026-09-02T11:00:00Z")
        self.assertEqual(0, self.run_import(event_v1))
        self.assertEqual(0, self.run_import(event_v2))
        item = json.loads((self.root / "product" / "inbox" / "INBOX-GH-000216.json").read_text(encoding="utf-8"))
        self.assertEqual("Wersja 2", item["raw_input"])
        self.assertEqual("2026-09-02T11:00:00Z", item["updated_at"])


class ProductCliGraphQLAssignmentContractTest(unittest.TestCase):
    def setUp(self):
        self.cli = load_cli_module()

    def make_client(self):
        return self.cli.GitHubIssueAutomationClient("rafalbragan", "-LibreDisplay", "token")

    def test_add_assignees_payload_uses_supported_agent_assignment_fields(self):
        client = self.make_client()
        captured = []

        client._get_repository_node_id = lambda: "R_repo"
        client._get_issue_node_id = lambda issue_number: "I_issue"
        client._get_actor_node_id = lambda login: "BOT_NODE_ID"
        client._copilot_assignee_confirmed = lambda issue_number, expected_actor_node_id=None: (True, ["Copilot"], ["BOT_NODE_ID"])

        def fake_graphql(query: str, variables: dict | None = None):
            captured.append({"query": query, "variables": json.loads(json.dumps(variables or {}))})
            return {
                "data": {
                    "addAssigneesToAssignable": {
                        "assignable": {
                            "number": 7,
                            "assignees": {"nodes": [{"login": "Copilot", "id": "BOT_NODE_ID"}]},
                        }
                    }
                }
            }

        client._graphql = fake_graphql
        result = client.assign_copilot(7, "master", "Instrukcje testowe")

        payload = captured[0]["variables"]
        agent_assignment = payload["agentAssignment"]
        self.assertIn("assigneeIds", payload)
        self.assertEqual(["BOT_NODE_ID"], payload["assigneeIds"])
        self.assertEqual("BOT_NODE_ID", result["assignee_ids"][0])
        self.assertEqual("assigneeIds", result["assignee_id_field"])
        self.assertEqual("master", agent_assignment["baseRef"])
        self.assertEqual("R_repo", agent_assignment["targetRepositoryId"])
        self.assertEqual("Instrukcje testowe", agent_assignment["customInstructions"])
        self.assertEqual("GPT-5.4 mini", agent_assignment["model"])
        self.assertTrue(agent_assignment["model"].strip())
        self.assertNotEqual("auto", agent_assignment["model"].strip().lower())
        self.assertNotIn("agentLogin", agent_assignment)
        self.assertNotIn("instructions", agent_assignment)

    def test_replace_actors_fallback_uses_actor_ids_and_supported_agent_assignment_fields(self):
        client = self.make_client()
        captured = []

        client._get_repository_node_id = lambda: "R_repo"
        client._get_issue_node_id = lambda issue_number: "I_issue"
        client._get_actor_node_id = lambda login: "BOT_NODE_ID"
        client._copilot_assignee_confirmed = lambda issue_number, expected_actor_node_id=None: (True, ["Copilot"], ["BOT_NODE_ID"])

        def fake_graphql(query: str, variables: dict | None = None):
            captured.append({"query": query, "variables": json.loads(json.dumps(variables or {}))})
            if "addAssigneesToAssignable" in query:
                return {"errors": [{"message": "preview mismatch"}]}
            return {
                "data": {
                    "replaceActorsForAssignable": {
                        "assignable": {
                            "number": 7,
                            "assignees": {"nodes": [{"login": "Copilot", "id": "BOT_NODE_ID"}]},
                        }
                    }
                }
            }

        client._graphql = fake_graphql
        result = client.assign_copilot(7, "master", "Instrukcje testowe")

        payload = captured[1]["variables"]
        agent_assignment = payload["agentAssignment"]
        self.assertIn("actorIds", payload)
        self.assertEqual(["BOT_NODE_ID"], payload["actorIds"])
        self.assertEqual("actorIds", result["assignee_id_field"])
        self.assertEqual("Instrukcje testowe", agent_assignment["customInstructions"])
        self.assertEqual("GPT-5.4 mini", agent_assignment["model"])
        self.assertTrue(agent_assignment["model"].strip())
        self.assertNotEqual("auto", agent_assignment["model"].strip().lower())
        self.assertNotIn("agentLogin", agent_assignment)
        self.assertNotIn("instructions", agent_assignment)

    def test_graphql_success_without_copilot_assignee_raises(self):
        client = self.make_client()
        client._get_repository_node_id = lambda: "R_repo"
        client._get_issue_node_id = lambda issue_number: "I_issue"
        client._get_actor_node_id = lambda login: "BOT_NODE_ID"
        client._copilot_assignee_confirmed = lambda issue_number, expected_actor_node_id=None: (False, ["Copilot"], ["OTHER_NODE_ID"])
        client._graphql = lambda query, variables=None: {
            "data": {
                "addAssigneesToAssignable": {
                    "assignable": {
                        "number": 7,
                        "assignees": {"nodes": [{"login": "Copilot", "id": "OTHER_NODE_ID"}]},
                    }
                }
            }
        }

        with self.assertRaisesRegex(RuntimeError, "Copilot assignee was not confirmed"):
            client.assign_copilot(7, "master", "Instrukcje testowe")

    def test_graphql_success_with_confirmed_copilot_assignee_returns_assigned(self):
        client = self.make_client()
        client._get_repository_node_id = lambda: "R_repo"
        client._get_issue_node_id = lambda issue_number: "I_issue"
        client._get_actor_node_id = lambda login: "BOT_NODE_ID"
        client._copilot_assignee_confirmed = lambda issue_number, expected_actor_node_id=None: (True, ["Copilot"], ["BOT_NODE_ID"])
        client._graphql = lambda query, variables=None: {
            "data": {
                "addAssigneesToAssignable": {
                    "assignable": {
                        "number": 7,
                        "assignees": {"nodes": [{"login": "Copilot", "id": "BOT_NODE_ID"}]},
                    }
                }
            }
        }

        result = client.assign_copilot(7, "master", "Instrukcje testowe")
        self.assertEqual("ASSIGNED", result["status"])
        self.assertEqual("GRAPHQL", result["method"])
        self.assertEqual(["Copilot"], result["verified_assignees"])

    def test_graphql_login_alias_is_accepted_when_node_id_matches(self):
        client = self.make_client()
        client._get_repository_node_id = lambda: "R_repo"
        client._get_issue_node_id = lambda issue_number: "I_issue"
        client._get_actor_node_id = lambda login: "BOT_NODE_ID"
        client._copilot_assignee_confirmed = lambda issue_number, expected_actor_node_id=None: (True, ["Copilot"], ["BOT_NODE_ID"])
        client._graphql = lambda query, variables=None: {
            "data": {
                "addAssigneesToAssignable": {
                    "assignable": {
                        "number": 7,
                        "assignees": {"nodes": [{"login": "Copilot", "id": "BOT_NODE_ID"}]},
                    }
                }
            }
        }
        result = client.assign_copilot(7, "master", "Instrukcje testowe")
        self.assertEqual("ASSIGNED", result["status"])
        self.assertEqual(["BOT_NODE_ID"], result["verified_assignee_node_ids"])

    def test_graphql_login_alias_is_rejected_when_node_id_differs(self):
        client = self.make_client()
        client._get_repository_node_id = lambda: "R_repo"
        client._get_issue_node_id = lambda issue_number: "I_issue"
        client._get_actor_node_id = lambda login: "BOT_NODE_ID"
        client._copilot_assignee_confirmed = lambda issue_number, expected_actor_node_id=None: (expected_actor_node_id == "OTHER_NODE_ID", ["Copilot"], ["OTHER_NODE_ID"])
        client._graphql = lambda query, variables=None: {
            "data": {
                "addAssigneesToAssignable": {
                    "assignable": {
                        "number": 7,
                        "assignees": {"nodes": [{"login": "Copilot", "id": "BOT_NODE_ID"}]},
                    }
                }
            }
        }
        with self.assertRaisesRegex(RuntimeError, "Copilot assignee was not confirmed"):
            client.assign_copilot(7, "master", "Instrukcje testowe")

    def test_explicit_model_constant_is_configured_and_not_auto(self):
        self.assertEqual("GPT-5.4 mini", self.cli.COPILOT_AGENT_MODEL)
        self.assertTrue(self.cli.COPILOT_AGENT_MODEL.strip())
        self.assertNotEqual("auto", self.cli.COPILOT_AGENT_MODEL.strip().lower())


class ProductCliLabelIdempotencyTest(unittest.TestCase):
    def setUp(self):
        self.cli = load_cli_module()

    def test_missing_requirement_label_is_created(self):
        client = self.cli.GitHubIssueAutomationClient("rafalbragan", "-LibreDisplay", "token")
        calls = []

        def fake_request(method: str, path: str, payload=None, extra_headers=None):
            calls.append((method, path))
            if method == "GET":
                raise RuntimeError('GitHub API GET failed: HTTP 404: {"message": "Not Found"}')
            if method == "POST":
                return {"name": "REQ-1234"}
            raise AssertionError(method)

        client._request = fake_request
        self.assertEqual("CREATED", client.ensure_label("REQ-1234", "0e8a16", "Śledzenie wymagania REQ-1234"))
        self.assertEqual([("GET", "/repos/rafalbragan/-LibreDisplay/labels/REQ-1234"), ("POST", "/repos/rafalbragan/-LibreDisplay/labels")], calls)

    def test_concurrent_requirement_label_creation_is_reused(self):
        client = self.cli.GitHubIssueAutomationClient("rafalbragan", "-LibreDisplay", "token")
        calls = []

        def fake_request(method: str, path: str, payload=None, extra_headers=None):
            calls.append((method, path))
            if method == "GET" and len(calls) == 1:
                raise RuntimeError('GitHub API GET failed: HTTP 404: {"message": "Not Found"}')
            if method == "POST":
                raise RuntimeError('GitHub API POST failed: HTTP 422: {"message": "already exists"}')
            if method == "GET":
                return {"name": "REQ-1234"}
            raise AssertionError(method)

        client._request = fake_request
        self.assertEqual("REUSED", client.ensure_label("REQ-1234", "0e8a16", "Śledzenie wymagania REQ-1234"))
        self.assertEqual(["GET", "POST", "GET"], [method for method, _ in calls])


class ProductInboxWorkflowStaticTest(unittest.TestCase):

    def workflow_push_paths(self, workflow_path: Path) -> list[str]:
        data = yaml.load(workflow_path.read_text(encoding="utf-8"), Loader=yaml.BaseLoader)
        return list((((data or {}).get("on") or {}).get("push") or {}).get("paths") or [])

    def workflow_jobs(self, workflow_path: Path) -> dict:
        data = yaml.load(workflow_path.read_text(encoding="utf-8"), Loader=yaml.BaseLoader)
        return (data or {}).get("jobs") or {}

    def jobs_invoking_copilot(self, workflow_path: Path) -> dict:
        jobs = self.workflow_jobs(workflow_path)
        result = {}
        for job_name, job in jobs.items():
            steps = (job or {}).get("steps") or []
            copilot_steps = [step for step in steps if "copilot" in str((step or {}).get("run") or "").lower()]
            if copilot_steps:
                result[job_name] = {"job": job or {}, "steps": steps, "copilot_steps": copilot_steps}
        return result

    def matches_workflow_paths(self, changed_paths: list[str], workflow_paths: list[str]) -> bool:
        import fnmatch
        return any(any(fnmatch.fnmatch(path, pattern) for pattern in workflow_paths) for path in changed_paths)

    def test_android_ci_fast_suite_captures_gradle_log_and_uploads_it(self):
        text = ANDROID_CI_WORKFLOW_PATH.read_text(encoding="utf-8")
        script_text = (WORKSPACE_ROOT / "scripts" / "test-fast.sh").read_text(encoding="utf-8")
        self.assertIn("- name: Run fast suite", text)
        self.assertIn("bash ./scripts/test-fast.sh", text)
        self.assertIn("ci-artifacts/**", text)
        self.assertIn('LOG_FILE="${LOG_DIR}/gradle-fast-suite.log"', script_text)
        self.assertIn("set -euo pipefail", script_text)
        self.assertIn("tee -a \"${LOG_FILE}\"", script_text)
        self.assertIn("PIPESTATUS[0]", script_text)
        self.assertIn("if: always()", text)

    def test_workflow_has_permissions_and_copilot_install(self):
        text = WORKFLOW_PATH.read_text(encoding="utf-8")
        self.assertIn("copilot-requests: write", text)
        self.assertIn("contents: write", text)
        self.assertIn("issues: write", text)
        self.assertIn("npm install -g @github/copilot@latest", text)
        self.assertIn("workflow_run:", text)

    def test_workflow_ai_invocation_is_read_only(self):
        text = WORKFLOW_PATH.read_text(encoding="utf-8")
        self.assertIn("copilot --version", text)
        self.assertIn("copilot --help >/dev/null", text)
        self.assertIn("--available-tools='view,grep,glob'", text)
        self.assertIn("--allow-tool='read'", text)
        self.assertIn("--deny-tool='write'", text)
        self.assertIn("--deny-tool='shell'", text)
        self.assertIn("--disable-builtin-mcps", text)
        self.assertIn("--no-ask-user", text)
        self.assertIn("-s", text)
        self.assertNotIn("copilot chat", text)
        self.assertNotIn("--input-file", text)
        self.assertNotIn("--yolo", text)
        self.assertNotIn("--allow-all", text)

    def test_workflow_has_persistence_and_concurrency(self):
        text = WORKFLOW_PATH.read_text(encoding="utf-8")
        bug_text = BUG_WORKFLOW_PATH.read_text(encoding="utf-8")
        self.assertIn("concurrency:", text)
        self.assertIn("group: product-foundation-state", text)
        self.assertIn("group: product-foundation-state", bug_text)
        self.assertIn("git add product/inbox product/generated product/review product/research product/requirements product/decisions product/implementation", text)
        self.assertIn("product: process inbox issue", text)
        self.assertIn("product: apply inbox decision issue", text)

    def test_workflows_are_valid_yaml(self):
        for workflow_path in [WORKFLOW_PATH, BUG_WORKFLOW_PATH, PRODUCT_QUALITY_WORKFLOW_PATH, ANDROID_CI_WORKFLOW_PATH, ANDROID_BUILD_WORKFLOW_PATH, ANDROID_DEBUG_BUILD_WORKFLOW_PATH, DOWNLOAD_APP_TESTING_RESULTS_WORKFLOW_PATH, FIREBASE_TEST_LAB_WORKFLOW_PATH]:
            loaded = yaml.safe_load(workflow_path.read_text(encoding="utf-8"))
            self.assertIsInstance(loaded, dict)

    def test_shellcheck_fixes_are_present_in_result_download_and_firebase_workflows(self):
        download_text = DOWNLOAD_APP_TESTING_RESULTS_WORKFLOW_PATH.read_text(encoding="utf-8")
        firebase_text = FIREBASE_TEST_LAB_WORKFLOW_PATH.read_text(encoding="utf-8")

        self.assertIn('missing="${missing% }"', download_text)
        self.assertIn('missing="${missing% }"', firebase_text)
        self.assertNotIn("sed 's/[[:space:]]*$//'", download_text)
        self.assertNotIn("sed 's/[[:space:]]*$//'", firebase_text)

        self.assertIn('} > "${MANIFEST}"', download_text)
        self.assertIn('} >> "$GITHUB_OUTPUT"', download_text)

    def test_android_debug_build_uploads_test_reports_without_masking_gradle_failures(self):
        data = yaml.safe_load(ANDROID_DEBUG_BUILD_WORKFLOW_PATH.read_text(encoding="utf-8"))
        steps = (((data or {}).get("jobs") or {}).get("build") or {}).get("steps") or []
        unit_test_step = next((step for step in steps if (step or {}).get("name") == "Run unit tests"), None)
        self.assertIsNotNone(unit_test_step)
        self.assertNotIn("continue-on-error", unit_test_step)
        self.assertEqual("./gradlew testDebugUnitTest --stacktrace", str(unit_test_step.get("run") or "").strip())
        self.assertNotIn("./gradlew test --stacktrace", str(unit_test_step.get("run") or "").strip())

        upload_step = next((step for step in steps if (step or {}).get("name") == "Upload test reports"), None)
        self.assertIsNotNone(upload_step)
        self.assertEqual("always()", upload_step.get("if"))
        self.assertEqual("actions/upload-artifact@v4", upload_step.get("uses"))
        with_block = upload_step.get("with") or {}
        self.assertEqual("android-debug-test-reports", with_block.get("name"))
        self.assertEqual("warn", with_block.get("if-no-files-found"))
        self.assertEqual(7, int(with_block.get("retention-days")))
        path_block = str(with_block.get("path") or "")
        for expected in [
            "app/build/test-results/testDebugUnitTest/**",
            "app/build/reports/tests/testDebugUnitTest/**",
            "app/build/outputs/roborazzi/**",
            "app/build/intermediates/roborazzi/**",
        ]:
            self.assertIn(expected, path_block)
        self.assertNotIn("testReleaseUnitTest", path_block)

        build_debug_step = next((step for step in steps if (step or {}).get("name") == "Build debug APK"), None)
        self.assertIsNotNone(build_debug_step)
        self.assertNotIn("continue-on-error", build_debug_step)

    def test_android_push_workflows_ignore_product_only_commits(self):
        android_ci_paths = self.workflow_push_paths(ANDROID_CI_WORKFLOW_PATH)
        android_build_paths = self.workflow_push_paths(ANDROID_BUILD_WORKFLOW_PATH)
        product_only_commit = [
            "product/bugs/BUG-0002.json",
            "product/generated/bug-reviews/BUG-0002.json",
            "scripts/product/automation_cli.py",
            ".github/workflows/librecare-implementation-automation.yml",
        ]
        android_change_commit = ["app/src/main/java/com/libredisplay/ui/monitoring/MonitoringScreen.kt"]
        self.assertFalse(self.matches_workflow_paths(product_only_commit, android_ci_paths))
        self.assertFalse(self.matches_workflow_paths(product_only_commit, android_build_paths))
        self.assertTrue(self.matches_workflow_paths(android_change_commit, android_ci_paths))
        self.assertTrue(self.matches_workflow_paths(android_change_commit, android_build_paths))

    def test_android_push_workflows_contain_expected_allowlist(self):
        for workflow_path in [ANDROID_CI_WORKFLOW_PATH, ANDROID_BUILD_WORKFLOW_PATH]:
            paths = self.workflow_push_paths(workflow_path)
            expected = {
                "app/**",
                "gradle/**",
                "gradlew",
                "gradlew.bat",
                "build.gradle.kts",
                "settings.gradle.kts",
                "gradle.properties",
                "scripts/test-fast.sh",
                "scripts/verify-environment.sh",
            }
            self.assertTrue(expected.issubset(set(paths)))
        self.assertIn(".github/workflows/android-ci.yml", self.workflow_push_paths(ANDROID_CI_WORKFLOW_PATH))
        self.assertIn(".github/workflows/android-build.yml", self.workflow_push_paths(ANDROID_BUILD_WORKFLOW_PATH))

    def test_workflow_permission_keys_are_supported(self):
        allowed = {
            "actions",
            "artifact-metadata",
            "attestations",
            "checks",
            "copilot-requests",
            "contents",
            "deployments",
            "discussions",
            "id-token",
            "issues",
            "models",
            "packages",
            "pages",
            "pull-requests",
            "repository-projects",
            "security-events",
            "statuses",
            "read-all",
            "write-all",
        }
        for workflow_path in [WORKFLOW_PATH, BUG_WORKFLOW_PATH, PRODUCT_QUALITY_WORKFLOW_PATH]:
            data = yaml.safe_load(workflow_path.read_text(encoding="utf-8"))
            permissions = data.get("permissions") or {}
            self.assertTrue(set(permissions).issubset(allowed), f"Unsupported top-level permissions in {workflow_path}: {set(permissions) - allowed}")
            for job_name, job in (data.get("jobs") or {}).items():
                job_permissions = (job or {}).get("permissions") or {}
                self.assertTrue(set(job_permissions).issubset(allowed), f"Unsupported job permissions in {workflow_path}:{job_name}: {set(job_permissions) - allowed}")

            def test_copilot_cli_jobs_have_official_permission_and_builtin_github_token(self):
                audited = {
                    str(WORKFLOW_PATH): self.jobs_invoking_copilot(WORKFLOW_PATH),
                    str(BUG_WORKFLOW_PATH): self.jobs_invoking_copilot(BUG_WORKFLOW_PATH),
                }
                self.assertEqual(
                    {
                        str(WORKFLOW_PATH): {"inbox-review"},
                        str(BUG_WORKFLOW_PATH): {"bug-intake-triage", "master-ci-regression-intake"},
                    },
                    {path: set(jobs.keys()) for path, jobs in audited.items()},
                )
                for workflow_path, jobs in audited.items():
                    for job_name, payload in jobs.items():
                        permissions = (payload["job"].get("permissions") or {})
                        self.assertEqual("write", permissions.get("copilot-requests"), f"{workflow_path}:{job_name}")
                        self.assertNotIn("models", permissions, f"{workflow_path}:{job_name}")
                        install_steps = [step for step in payload["steps"] if "npm install -g @github/copilot@latest" in str((step or {}).get("run") or "")]
                        self.assertTrue(install_steps, f"{workflow_path}:{job_name} missing current Copilot install")
                        analysis_steps = [step for step in payload["steps"] if "run copilot" in str((step or {}).get("name") or "").lower()]
                        self.assertTrue(analysis_steps, f"{workflow_path}:{job_name} missing Copilot analysis step")
                        for step in analysis_steps:
                            env = (step or {}).get("env") or {}
                            self.assertEqual("${{ github.token }}", env.get("GITHUB_TOKEN"), f"{workflow_path}:{job_name}:{step.get('name')}")
                            self.assertNotIn("COPILOT_AGENT_USER_TOKEN", env, f"{workflow_path}:{job_name}:{step.get('name')}")

    def test_copilot_agent_user_token_remains_assignment_only(self):
        allowed_commands = ["bug-sync-fix-handoff", "inbox-sync-implementation-handoff", "record-ci-result"]
        for workflow_path in [WORKFLOW_PATH, BUG_WORKFLOW_PATH]:
            for job_name, job in self.workflow_jobs(workflow_path).items():
                for step in (job or {}).get("steps") or []:
                    env = (step or {}).get("env") or {}
                    if "COPILOT_AGENT_USER_TOKEN" not in env:
                        continue
                    run = str((step or {}).get("run") or "")
                    self.assertTrue(any(command in run for command in allowed_commands), f"{workflow_path}:{job_name}:{step.get('name')}")
                    self.assertNotIn("| copilot", run)
                    self.assertNotIn("< inbox-ai-prompt.md", run)
                    self.assertNotIn("cat bug-ai-prompt.json | copilot", run)

    def test_models_read_is_not_treated_as_copilot_authentication(self):
        self.assertNotIn("models: read", WORKFLOW_PATH.read_text(encoding="utf-8"))
        self.assertNotIn("models: read", BUG_WORKFLOW_PATH.read_text(encoding="utf-8"))

    def test_product_quality_workflow_scopes_actionlint_ignore_to_copilot_requests_false_positive(self):
        text = PRODUCT_QUALITY_WORKFLOW_PATH.read_text(encoding="utf-8")
        self.assertIn("ACTIONLINT_VERSION: 1.7.7", text)
        self.assertIn("copilot-requests", text)
        self.assertIn("./actionlint -ignore 'unknown permission scope .*copilot-requests.*'", text)
        self.assertNotIn("--no-checks", text)
        self.assertNotIn("permissions: {}", text)

    def test_issue_form_has_product_inbox_prefix(self):
        text = ISSUE_FORM_PATH.read_text(encoding="utf-8")
        self.assertIn("title: \"[Skrzynka Produktowa] \"", text)

    def test_workflow_bootstrap_routing_supports_body_signature(self):
        text = WORKFLOW_PATH.read_text(encoding="utf-8")
        # CASE A: no label/no prefix can still process via canonical form headings.
        self.assertIn("const hasFormSignature", text)
        self.assertIn("'### typ zgłoszenia'", text)
        self.assertIn("'### persona użytkownika'", text)
        self.assertIn("'### co zauważyłeś(-aś) / czego potrzebujesz?'", text)
        self.assertIn("const shouldProcess = hasLabel || hasPrefix || hasFormSignature;", text)
        # CASE B/C: explicit label or title prefix should still process.
        self.assertIn("const hasLabel", text)
        self.assertIn("const hasPrefix", text)
        # CASE D: ordinary issue path is ignored.
        self.assertIn("if (!shouldProcess)", text)
        self.assertIn("core.setOutput('process', 'false');", text)

    def test_workflow_has_copilot_handoff_and_pr_tracking(self):
        text = WORKFLOW_PATH.read_text(encoding="utf-8")
        self.assertIn("inbox-sync-implementation-handoff", text)
        self.assertIn("track-implementation-pr", text)
        self.assertIn("record-ci-result", text)
        self.assertIn("copilot-swe-agent[bot]", text)
        self.assertRegex(text, r"agent_assignment|agentAssignment")
        self.assertIn("product/implementation", text)
        self.assertNotIn("gh pr merge", text)
        self.assertNotIn("pulls.merge", text)

    def test_pr_tracking_workflows_checkout_base_ref_before_pushing(self):
        bug_text = BUG_WORKFLOW_PATH.read_text(encoding="utf-8")
        inbox_text = WORKFLOW_PATH.read_text(encoding="utf-8")
        self.assertIn('track-work-pr:', bug_text)
        self.assertIn('ref: ${{ github.event.pull_request.base.ref }}', bug_text)
        self.assertIn('git push origin "HEAD:${{ github.event.pull_request.base.ref }}"', bug_text)
        self.assertIn('implementation-pr-tracking:', inbox_text)
        self.assertIn('ref: ${{ github.event.pull_request.base.ref }}', inbox_text)
        self.assertIn('git push origin "HEAD:$BRANCH_NAME"', inbox_text)
        self.assertIn('BRANCH_NAME: ${{ github.event.pull_request.base.ref }}', inbox_text)

    def test_runtime_automation_contains_no_merge_or_automerge_operations(self):
        runtime_files = [
            BUG_WORKFLOW_PATH,
            WORKFLOW_PATH,
            WORKSPACE_ROOT / ".github" / "workflows" / "product-quality.yml",
            WORKSPACE_ROOT / "scripts" / "product" / "automation_cli.py",
            WORKSPACE_ROOT / "scripts" / "product" / "product_cli.py",
        ]
        forbidden = [
            "gh pr merge",
            "enablePullRequestAutoMerge",
            "mergePullRequest",
            "pulls.merge",
            "createReview({",
            "event: \"APPROVE\"",
            "event:'APPROVE'",
            "auto-merge",
        ]
        for path in runtime_files:
            text = path.read_text(encoding="utf-8")
            for marker in forbidden:
                self.assertNotIn(marker, text, f"forbidden merge capability marker {marker!r} found in {path}")

    def test_product_cli_uses_graphql_assignment_mutations(self):
        text = CLI_PATH.read_text(encoding="utf-8")
        self.assertIn("def find_existing_implementation_issue", text)
        self.assertIn("addAssigneesToAssignable", text)
        self.assertIn("replaceActorsForAssignable", text)
        self.assertIn("GraphQL-Features", text)
        self.assertIn("targetRepositoryId", text)
        self.assertIn("baseRef", text)
        self.assertIn("customInstructions", text)
        self.assertIn('"model": model_name', text)
        self.assertIn('COPILOT_AGENT_MODEL = "GPT-5.4 mini"', text)
        self.assertIn('"issues_copilot_assignment_api_support"', text)
        self.assertIn('"coding_agent_model_selection"', text)
        self.assertNotIn('"agentLogin": COPILOT_AGENT_LOGIN', text)
        self.assertNotIn('"instructions": instructions', text)
        self.assertIn("copilot_assignment_token", text)
        self.assertNotIn('"model": "Auto"', text)

    def test_workflows_use_safe_repo_argument_shape_for_leading_hyphen_names(self):
        inbox_text = WORKFLOW_PATH.read_text(encoding="utf-8")
        bug_text = BUG_WORKFLOW_PATH.read_text(encoding="utf-8")

        self.assertIn('--repo-name="${{ github.event.repository.name }}"', inbox_text)
        self.assertIn("'--repo-name=${{ github.event.repository.name }}'", inbox_text)
        self.assertIn('--copilot-assignment-token "$COPILOT_AGENT_USER_TOKEN"', inbox_text)
        self.assertNotIn('--repo-name "${{ github.event.repository.name }}"', inbox_text)
        self.assertNotIn("'--repo-name','${{ github.event.repository.name }}'", inbox_text)

        self.assertIn('--repo-name="${{ github.event.repository.name }}"', bug_text)
        self.assertIn("'--repo-name=${{ github.event.repository.name }}'", bug_text)
        self.assertNotIn('--repo-name "${{ github.event.repository.name }}"', bug_text)
        self.assertNotIn("'--repo-name','${{ github.event.repository.name }}'", bug_text)

    def test_workflow_uses_user_token_for_agent_assignment(self):
        text = WORKFLOW_PATH.read_text(encoding="utf-8")
        self.assertIn("COPILOT_AGENT_USER_TOKEN: ${{ secrets.COPILOT_AGENT_USER_TOKEN }}", text)
        self.assertIn('--copilot-assignment-token "$COPILOT_AGENT_USER_TOKEN"', text)
        self.assertNotIn('--copilot-assignment-token "$GITHUB_TOKEN"', text)

    def test_workflow_decision_fallback_comment_is_polish(self):
        text = WORKFLOW_PATH.read_text(encoding="utf-8")
        self.assertIn("Brak wyniku komendy decyzyjnej.", text)

    def test_bug_issue_form_exists_and_is_polish(self):
        text = BUG_ISSUE_FORM_PATH.read_text(encoding="utf-8")
        self.assertIn("name: LibreCare", text)
        self.assertIn("labels:", text)
        self.assertIn("librecare-bug", text)
        self.assertIn("Co się stało?", text)
        self.assertIn("Jakiego zachowania oczekiwałeś?", text)

    def test_bug_automation_workflow_has_bounded_repair(self):
        text = BUG_WORKFLOW_PATH.read_text(encoding="utf-8")
        self.assertIn("workflow_dispatch:", text)
        self.assertIn("resume-confirmed-bug-handoff:", text)
        self.assertIn("bug_id:", text)
        self.assertIn("workflow_run", text)
        self.assertIn("record-ci-result", text)
        self.assertIn("bug-sync-fix-handoff", text)
        self.assertIn("persist-bug-records", text)
        self.assertIn("ci-regression-enrich-evidence", text)
        self.assertIn("Report CI evidence enrichment summary", text)
        self.assertIn("GITHUB_TOKEN_PRESENT", text)
        self.assertIn("JOB_LOG_HTTP_STATUS", text)
        self.assertIn("ARTIFACT_DOWNLOAD_STATUS", text)
        self.assertIn("ARTIFACT_EXTRACT_STATUS", text)
        self.assertIn("TEXT_FILES_SCANNED", text)
        self.assertIn("CANDIDATE_FILES_FOUND", text)
        self.assertIn("Determine if CI regression auto-fix is allowed", text)
        self.assertIn("Attempt bug fix handoff for confirmed CI regressions", text)
        self.assertIn("Verify enriched evidence reached the triage prompt", text)
        self.assertIn("COPILOT_AGENT_USER_TOKEN: ${{ secrets.COPILOT_AGENT_USER_TOKEN }}", text)
        self.assertIn('--copilot-assignment-token "$COPILOT_AGENT_USER_TOKEN"', text)
        self.assertIn('workflows: ["Android CI"]', text)
        self.assertIn("master-ci-regression-intake", text)
        self.assertIn("ci-regression-intake", text)
        self.assertIn("github.event.workflow_run.event != 'pull_request'", text)
        self.assertIn("fetch-depth: 0", text)
        automation_cli_text = (WORKSPACE_ROOT / "scripts" / "product" / "automation_cli.py").read_text(encoding="utf-8")
        self.assertIn("COPILOT_AGENT_LOGIN = \"copilot-swe-agent[bot]\"", automation_cli_text)
        self.assertIn("MASTER_CI_REGRESSION_WORKFLOWS = {\"Android CI\", \"Android APK Build\"}", automation_cli_text)
        self.assertIn("group: product-foundation-state", text)
        self.assertIn("copilot-requests: write", text)
        self.assertNotIn("models: read", text)
        self.assertIn("continue-on-error: true", text)
        self.assertIn("BUG_PRODUCT_DECISION_BRIDGE", text)
        self.assertNotIn("ci-regression-evidence.json || true", text)
        self.assertNotIn("gh pr merge", text)
        self.assertNotIn("pulls.merge", text)

    def test_product_quality_workflow_runs_actionlint_for_workflow_changes(self):
        text = PRODUCT_QUALITY_WORKFLOW_PATH.read_text(encoding="utf-8")
        self.assertIn('".github/workflows/**"', text)
        self.assertIn('".github/ISSUE_TEMPLATE/**"', text)
        self.assertIn("ACTIONLINT_VERSION: 1.7.7", text)
        self.assertIn("https://github.com/rhysd/actionlint/releases/download/v${ACTIONLINT_VERSION}/actionlint_${ACTIONLINT_VERSION}_linux_amd64.tar.gz", text)
        self.assertIn("./actionlint -ignore 'unknown permission scope .*copilot-requests.*'", text)
        self.assertNotIn("uses: rhysd/actionlint@v1", text)
        self.assertIn("copilot-requests", text)

    def test_android_ci_workflow_uses_current_fast_suite_artifact_name(self):
        text = ANDROID_CI_WORKFLOW_PATH.read_text(encoding="utf-8")
        self.assertIn("name: librecare-fast-suite", text)


if __name__ == "__main__":
    unittest.main()
