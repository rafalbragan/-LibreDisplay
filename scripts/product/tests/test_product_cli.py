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
            labels = []
            next_issue_number = 500

            def __init__(self, owner: str, repo: str, token: str):
                self.owner = owner
                self.repo = repo
                self.token = token

            def ensure_label(self, name: str, color: str, description: str) -> None:
                self.__class__.labels.append((name, color, description))

            def find_existing_implementation_issue(self, req_id: str, expected_title: str):
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
                return response

            def assign_copilot(self, issue_number: int, base_branch: str, instructions: str):
                payload = {
                    "issue_number": issue_number,
                    "base_branch": base_branch,
                    "instructions": instructions,
                    "status": "ASSIGNED",
                    "field": "agent_assignment",
                }
                self.__class__.assigned.append(payload)
                return payload

        self.fake_client = FakeGitHubClient
        self.cli.GITHUB_CLIENT_FACTORY = FakeGitHubClient

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
        with self.patched_paths():
            return self.cli.cmd_inbox_handle_decision(str(event_file))

    def run_decision_with_output(self, event: dict) -> tuple[int, str]:
        event_file = self.root / "comment-event.json"
        self.write_json(event_file, event)
        stdout = io.StringIO()
        with self.patched_paths(), contextlib.redirect_stdout(stdout):
            code = self.cli.cmd_inbox_handle_decision(str(event_file))
        return code, stdout.getvalue()

    def run_handoff(self, event: dict, output_name: str = "handoff.json") -> dict:
        event_file = self.root / "comment-event.json"
        output_file = self.root / output_name
        self.write_json(event_file, event)
        with self.patched_paths():
            result = self.cli.cmd_inbox_sync_implementation_handoff(
                event_file=str(event_file),
                repo_owner="example",
                repo_name="repo",
                github_token="token",
                output_file=str(output_file),
            )
        self.assertEqual(0, result)
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

    def test_validate_passes_with_canonical_implementation_record(self):
        event = self.make_issue_event(228, "Problem", "Walidacja implementacji")
        self.assertEqual(0, self.run_import(event))
        self.assertEqual(0, self.run_apply_ai(228, self.ai_payload(228, "PRODUCT_PROBLEM", "ACCEPT")))
        self.assertEqual(0, self.run_decision(self.make_comment_event(event, "/accept", author="repo-owner")))
        self.assertEqual(0, self.run_validate())

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


class ProductInboxWorkflowStaticTest(unittest.TestCase):
    def test_workflow_has_permissions_and_copilot_install(self):
        text = WORKFLOW_PATH.read_text(encoding="utf-8")
        self.assertIn("copilot-requests: write", text)
        self.assertIn("contents: write", text)
        self.assertIn("issues: write", text)
        self.assertIn("npm install -g @github/copilot", text)
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
        self.assertIn("concurrency:", text)
        self.assertIn("group: product-foundation-state", text)
        self.assertIn("git add product/inbox product/generated product/review product/research product/requirements product/decisions product/implementation", text)
        self.assertIn("product: process inbox issue", text)
        self.assertIn("product: apply inbox decision issue", text)

    def test_workflows_are_valid_yaml(self):
        for workflow_path in [WORKFLOW_PATH, BUG_WORKFLOW_PATH]:
            loaded = yaml.safe_load(workflow_path.read_text(encoding="utf-8"))
            self.assertIsInstance(loaded, dict)

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
        self.assertIn("workflow_run", text)
        self.assertIn("record-ci-result", text)
        self.assertIn("bug-sync-fix-handoff", text)
        automation_cli_text = (WORKSPACE_ROOT / "scripts" / "product" / "automation_cli.py").read_text(encoding="utf-8")
        self.assertIn("COPILOT_AGENT_LOGIN = \"copilot-swe-agent[bot]\"", automation_cli_text)
        self.assertIn("group: product-foundation-state", text)
        self.assertIn("copilot-requests: write", text)
        self.assertNotIn("gh pr merge", text)
        self.assertNotIn("pulls.merge", text)


if __name__ == "__main__":
    unittest.main()
