import contextlib
import importlib.util
import json
import shutil
import tempfile
import unittest
from pathlib import Path


WORKSPACE_ROOT = Path(__file__).resolve().parents[3]
CLI_PATH = WORKSPACE_ROOT / "scripts" / "product" / "product_cli.py"
WORKFLOW_PATH = WORKSPACE_ROOT / ".github" / "workflows" / "product-inbox.yml"
ISSUE_FORM_PATH = WORKSPACE_ROOT / ".github" / "ISSUE_TEMPLATE" / "product-inbox.yml"


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

    def tearDown(self):
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
            "### TYPE",
            issue_type,
            "",
            "### PERSONA",
            "Opiekun",
            "",
            "### MODULE",
            "Główna / Monitoring",
            "",
            "### WHAT DID YOU NOTICE / WHAT WOULD YOU LIKE?",
            notice,
            "",
            "### WHY DOES IT MATTER?",
            "Bo to ważne.",
            "",
            "### CONTEXT / EXAMPLE",
            "Przykładowy kontekst.",
        ])
        return {
            "issue": {
                "number": number,
                "html_url": f"https://github.com/example/repo/issues/{number}",
                "url": f"https://api.github.com/repos/example/repo/issues/{number}",
                "created_at": "2026-09-02T09:00:00Z",
                "updated_at": updated_at,
                "title": f"[Product Inbox] Test {number}",
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

    def req_ids(self):
        reqs = set()
        for p in (self.root / "product" / "requirements").glob("REQ-*.json"):
            reqs.add(p.stem)
        return reqs

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
        cmd_event = self.make_comment_event(event, "/accept", author="repo-owner")
        self.assertEqual(0, self.run_decision(cmd_event))
        first_after = self.req_ids()
        self.assertEqual(len(before) + 1, len(first_after))
        self.assertEqual(0, self.run_decision(cmd_event))
        self.assertEqual(first_after, self.req_ids())

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
        self.assertIn("group: product-inbox-state", text)
        self.assertIn("git add product/inbox product/generated product/review product/research", text)
        self.assertIn("product: process inbox issue", text)
        self.assertIn("product: apply inbox decision issue", text)

    def test_issue_form_has_product_inbox_prefix(self):
        text = ISSUE_FORM_PATH.read_text(encoding="utf-8")
        self.assertIn("title: \"[Product Inbox] \"", text)

    def test_workflow_bootstrap_routing_supports_body_signature(self):
        text = WORKFLOW_PATH.read_text(encoding="utf-8")
        # CASE A: no label/no prefix can still process via canonical form headings.
        self.assertIn("const hasFormSignature", text)
        self.assertIn("'### type'", text)
        self.assertIn("'### persona'", text)
        self.assertIn("'### what did you notice / what would you like?'", text)
        self.assertIn("const shouldProcess = hasLabel || hasPrefix || hasFormSignature;", text)
        # CASE B/C: explicit label or title prefix should still process.
        self.assertIn("const hasLabel", text)
        self.assertIn("const hasPrefix", text)
        # CASE D: ordinary issue path is ignored.
        self.assertIn("if (!shouldProcess)", text)
        self.assertIn("core.setOutput('process', 'false');", text)


if __name__ == "__main__":
    unittest.main()
