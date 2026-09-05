import importlib.util
import json
import shutil
import tempfile
import unittest
from pathlib import Path

WORKSPACE_ROOT = Path(__file__).resolve().parents[3]
CLI_PATH = WORKSPACE_ROOT / "scripts" / "product" / "automation_cli.py"


def load_cli_module():
	spec = importlib.util.spec_from_file_location("automation_cli_under_test", CLI_PATH)
	module = importlib.util.module_from_spec(spec)
	assert spec and spec.loader
	spec.loader.exec_module(module)
	return module


class FakeGitHubClient:
	created_issues = []
	assignments = []
	activations = []
	activation_should_fail = False
	activation_error = "GraphQL error"
	labels = []
	issues = {}
	close_calls = []
	next_issue_number = 700

	def __init__(self, owner: str, repo: str, token: str):
		self.owner = owner
		self.repo = repo
		self.token = token

	def ensure_label(self, name: str, color: str, description: str):
		self.__class__.labels.append((name, color, description))

	def create_issue(self, title: str, body: str, labels: list[str]) -> dict:
		issue_number = self.__class__.next_issue_number
		self.__class__.next_issue_number += 1
		response = {
			"number": issue_number,
			"html_url": f"https://example/issues/{issue_number}",
			"url": f"https://api.example/issues/{issue_number}",
			"state": "open",
			"title": title,
			"body": body,
			"labels": labels,
		}
		self.__class__.created_issues.append(response)
		self.__class__.issues[issue_number] = dict(response)
		return response

	def assign_copilot(self, issue_number: int, base_branch: str, instructions: str) -> dict:
		payload = {
			"issue_number": issue_number,
			"base_branch": base_branch,
			"instructions": instructions,
			"status": "ASSIGNED",
			"assignment_state": "ASSIGNED",
		}
		self.__class__.assignments.append(payload)
		return payload

	def start_copilot_agent(self, issue_number: int, base_branch: str, instructions: str, copilot_agent_user_token: str | None) -> dict:
		if not copilot_agent_user_token:
			return {
				"status": "START_FAILED",
				"start_state": "START_FAILED",
				"reason": "COPILOT_AGENT_USER_TOKEN_MISSING",
			}
		if self.__class__.activation_should_fail:
			return {
				"status": "START_FAILED",
				"start_state": "START_FAILED",
				"reason": "ACTIVATION_FAILED",
				"error": self.__class__.activation_error,
			}
		payload = {
			"status": "STARTED",
			"start_state": "STARTED",
			"activation_operation": "replaceActorsForAssignable",
			"model": "gpt-5.4-mini",
			"issue_number": issue_number,
		}
		self.__class__.activations.append(payload)
		return payload

	def assign_and_start_copilot(self, issue_number: int, base_branch: str, instructions: str, copilot_agent_user_token: str | None) -> dict:
		assignment = self.assign_copilot(issue_number, base_branch, instructions)
		activation = self.start_copilot_agent(issue_number, base_branch, instructions, copilot_agent_user_token)
		return {
			"status": "STARTED" if activation.get("status") == "STARTED" else "START_FAILED",
			"assignment_state": assignment.get("assignment_state", "ASSIGNED"),
			"start_state": activation.get("start_state", "START_FAILED"),
			"assignment": assignment,
			"activation": activation,
			"model": "gpt-5.4-mini",
		}

	def get_issue(self, issue_number: int) -> dict:
		return dict(self.__class__.issues.get(int(issue_number), {"number": issue_number, "state": "open"}))

	def close_issue(self, issue_number: int) -> dict:
		self.__class__.close_calls.append(int(issue_number))
		record = self.__class__.issues.get(int(issue_number), {"number": int(issue_number)})
		record["state"] = "closed"
		self.__class__.issues[int(issue_number)] = record
		return dict(record)


class AutomationCliTest(unittest.TestCase):
	def setUp(self):
		self.cli = load_cli_module()
		self.temp_dir = tempfile.TemporaryDirectory()
		self.root = Path(self.temp_dir.name)
		shutil.copytree(WORKSPACE_ROOT / "product", self.root / "product")
		(self.root / "product" / "bugs").mkdir(exist_ok=True)
		(self.root / "product" / "implementation").mkdir(exist_ok=True)
		for bug_file in (self.root / "product" / "bugs").glob("BUG-*.json"):
			bug_file.unlink()
		for impl_bug_file in (self.root / "product" / "implementation").glob("IMP-BUG-*.json"):
			impl_bug_file.unlink()

		self.cli.ROOT = self.root
		self.cli.PRODUCT = self.root / "product"
		self.cli.REQUIREMENTS_DIR = self.cli.PRODUCT / "requirements"
		self.cli.BUGS_DIR = self.cli.PRODUCT / "bugs"
		self.cli.IMPLEMENTATION_DIR = self.cli.PRODUCT / "implementation"
		self.cli.GENERATED_DIR = self.cli.PRODUCT / "generated"
		self.cli.BUG_REVIEWS_DIR = self.cli.GENERATED_DIR / "bug-reviews"
		self.cli.TECH_VALIDATIONS_DIR = self.cli.GENERATED_DIR / "technical-validations"
		self.cli.GITHUB_CLIENT_FACTORY = FakeGitHubClient

		FakeGitHubClient.created_issues = []
		FakeGitHubClient.assignments = []
		FakeGitHubClient.activations = []
		FakeGitHubClient.activation_should_fail = False
		FakeGitHubClient.activation_error = "GraphQL error"
		FakeGitHubClient.labels = []
		FakeGitHubClient.issues = {}
		FakeGitHubClient.close_calls = []
		FakeGitHubClient.next_issue_number = 700

		self.accepted_req_id = "REQ-9999"
		self.write_json(
			self.root / "product" / "requirements" / f"{self.accepted_req_id}.json",
			{"id": self.accepted_req_id, "status": "ACCEPTED", "acceptance_test_ids": ["TESTRUN-REQ-9999"]},
		)

	def tearDown(self):
		self.temp_dir.cleanup()

	def write_json(self, path: Path, payload):
		path.parent.mkdir(parents=True, exist_ok=True)
		path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")

	def read_json(self, path: Path) -> dict:
		return json.loads(path.read_text(encoding="utf-8"))

	def bug_issue_event(self, number: int, title: str, observed: str, expected: str, reproduction: str) -> dict:
		body = "\n".join([
			"### Co się stało?",
			observed,
			"",
			"### Jakiego zachowania oczekiwałeś?",
			expected,
			"",
			"### Jak odtworzyć problem?",
			reproduction,
			"",
			"### Gdzie występuje?",
			"Ekran glowny",
			"",
			"### Czy problem występował wcześniej?",
			"Nie",
			"",
			"### Dodatkowy kontekst",
			f"Powiazanie z {self.accepted_req_id} i TESTRUN-0001",
		])
		return {
			"issue": {
				"number": number,
				"title": title,
				"body": body,
				"labels": [{"name": "bug"}, {"name": "librecare-bug"}],
				"html_url": f"https://example/issues/{number}",
				"url": f"https://api.example/issues/{number}",
			}
		}

	def issue_event(self, number: int, title: str, body: str, labels=None, action: str = "opened") -> dict:
		return {
			"action": action,
			"issue": {
				"number": number,
				"title": title,
				"body": body,
				"labels": labels if labels is not None else [{"name": "bug"}, {"name": "librecare-bug"}],
				"html_url": f"https://example/issues/{number}",
				"url": f"https://api.example/issues/{number}",
			},
		}

	def import_bug(self, event: dict) -> str:
		event_file = self.root / "bug-event.json"
		self.write_json(event_file, event)
		self.assertEqual(0, self.cli.cmd_bug_import(str(event_file)))
		bug_paths = sorted((self.root / "product" / "bugs").glob("BUG-*.json"))
		self.assertTrue(bug_paths)
		return bug_paths[-1].stem

	def triage_bug(self, bug_id: str, payload: dict):
		triage_file = self.root / "triage.json"
		self.write_json(triage_file, payload)
		self.assertEqual(0, self.cli.cmd_bug_apply_ai_triage(bug_id, str(triage_file)))

	def close_eligible_bug_issue(self, bug_id: str, product_validation_pass: bool = True) -> dict:
		out = self.root / f"close-{bug_id}.json"
		code = self.cli.cmd_validate_and_close_issue_if_eligible(
			bug_id=bug_id,
			repo_owner="example",
			repo_name="repo",
			github_token="token",
			product_validation_pass=product_validation_pass,
			output_file=str(out),
		)
		self.assertEqual(0, code)
		return self.read_json(out)

	def bug_form_body(self, observed: str = "Widoczny objaw", expected: str = "Oczekiwany objaw", reproduction: str = "1. Otworz dashboard") -> str:
		return "\n".join([
			"### Co się stało?",
			observed,
			"",
			"### Jakiego zachowania oczekiwałeś?",
			expected,
			"",
			"### Jak odtworzyć problem?",
			reproduction,
			"",
			"### Gdzie występuje?",
			"Główna / Monitoring",
			"",
			"### Czy problem występował wcześniej?",
			"Nie",
			"",
			"### Dodatkowy kontekst",
			"Brak",
		])

	def product_inbox_body(self) -> str:
		return "\n".join([
			"### Typ zgłoszenia",
			"Problem",
			"",
			"### Persona użytkownika",
			"Opiekun",
			"",
			"### Moduł",
			"Główna / Monitoring",
			"",
			"### Co zauważyłeś(-aś) / czego potrzebujesz?",
			"Opis obserwacji",
		])

	def implementation_issue_body(self) -> str:
		return "\n".join([
			"## OGRANICZENIA_AUTOMATYZACJI",
			"To jest kanoniczny rekord bledu.",
			"",
			"## BUG",
			"BUG-0123",
			"",
			"## OPIS",
			"Naprawa implementacji",
		])

	def test_regression_against_requirement_queues_fix(self):
		bug_id = self.import_bug(
			self.bug_issue_event(401, "[Blad LibreCare] Czas wzgledny", "Widoczny surowy ISO", "Naturalny czas", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja wobec zaakceptowanego REQ.",
				"severity": "HIGH",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0001"],
			},
		)
		out = self.root / "handoff.json"
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", output_file=str(out)))
		bug = self.read_json(self.root / "product" / "bugs" / f"{bug_id}.json")
		self.assertEqual("CONFIRMED_DEFECT", bug["classification"])
		self.assertEqual("IN_PROGRESS", bug["status"])
		self.assertEqual(1, len(FakeGitHubClient.created_issues))

	def test_issue_five_shape_routes_as_bug_even_without_label(self):
		event = self.issue_event(
			5,
			"[Błąd LibreCare] Product Review ignoruje istniejące dowody z repozytorium",
			self.bug_form_body(
				observed="Product Review nie bierze pod uwagę dowodów.",
				expected="Powinien uwzględniać istniejące dowody.",
				reproduction="1. Otworz Product Review\n2. Sprawdz dowody",
			),
			labels=[],
		)
		self.assertTrue(self.cli.is_librecare_bug_issue(event["issue"]))
		bug_id = self.import_bug(event)
		bug = self.read_json(self.root / "product" / "bugs" / f"{bug_id}.json")
		self.assertEqual("GITHUB_BUG_ISSUE", bug["source"])
		self.assertEqual(5, bug["source_issue_number"])

	def test_product_inbox_issue_is_not_routed_as_bug(self):
		event = self.issue_event(
			6,
			"[Skrzynka Produktowa] Product Review ignoruje istniejące dowody z repozytorium",
			self.product_inbox_body(),
			labels=[{"name": "product-inbox"}],
		)
		self.assertFalse(self.cli.is_librecare_bug_issue(event["issue"]))
		event_file = self.root / "product-inbox-event.json"
		self.write_json(event_file, event)
		self.assertEqual(0, self.cli.cmd_bug_import(str(event_file)))
		self.assertEqual(0, len(list((self.root / "product" / "bugs").glob("BUG-*.json"))))

	def test_implementation_issue_is_not_routed_as_bug(self):
		event = self.issue_event(
			77,
			"[Naprawa] BUG-0123 — Product Review",
			self.implementation_issue_body(),
			labels=[{"name": "bugfix"}, {"name": "BUG-0123"}],
		)
		self.assertFalse(self.cli.is_librecare_bug_issue(event["issue"]))
		event_file = self.root / "implementation-event.json"
		self.write_json(event_file, event)
		self.assertEqual(0, self.cli.cmd_bug_import(str(event_file)))
		self.assertEqual(0, len(list((self.root / "product" / "bugs").glob("BUG-*.json"))))

	def test_unrelated_issue_is_not_routed_as_bug(self):
		event = self.issue_event(
			88,
			"Pytanie o UI",
			"Zwykly tekst bez formy zgloszenia błędu.",
			labels=[],
		)
		self.assertFalse(self.cli.is_librecare_bug_issue(event["issue"]))
		event_file = self.root / "unrelated-event.json"
		self.write_json(event_file, event)
		self.assertEqual(0, self.cli.cmd_bug_import(str(event_file)))
		self.assertEqual(0, len(list((self.root / "product" / "bugs").glob("BUG-*.json"))))

	def test_reopened_bug_issue_deduplicates_source_record(self):
		opened = self.issue_event(
			91,
			"[Blad LibreCare] Duplikat po wznowieniu",
			self.bug_form_body(observed="Ten sam objaw", expected="Ten sam wynik", reproduction="1. Otworz dashboard"),
			labels=[],
			action="opened",
		)
		reopened = self.issue_event(
			91,
			"[Blad LibreCare] Duplikat po wznowieniu",
			self.bug_form_body(observed="Ten sam objaw", expected="Ten sam wynik", reproduction="1. Otworz dashboard"),
			labels=[],
			action="reopened",
		)
		first_id = self.import_bug(opened)
		second_id = self.import_bug(reopened)
		self.assertEqual(first_id, second_id)
		bug_files = sorted((self.root / "product" / "bugs").glob("BUG-*.json"))
		self.assertEqual(1, len(bug_files))

	def test_new_behavior_routes_to_product_decision_and_no_fix(self):
		bug_id = self.import_bug(
			self.bug_issue_event(402, "[Blad LibreCare] Nowy prog", "Brak dynamicznego progu", "Nowy algorytm", "1. Otworz ustawienia")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "To zmiana zakresu produktu.",
				"severity": "MEDIUM",
				"safety_impact": "LOW",
				"requires_behavior_change": True,
				"recommended_related_requirements": [],
				"recommended_related_tests": [],
			},
		)
		self.assertEqual(1, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token"))
		bug = self.read_json(self.root / "product" / "bugs" / f"{bug_id}.json")
		self.assertEqual("NEEDS_PRODUCT_DECISION", bug["status"])
		self.assertEqual(0, len(FakeGitHubClient.created_issues))

	def test_safety_semantics_change_routes_to_product_decision(self):
		bug_id = self.import_bug(
			self.bug_issue_event(403, "[Blad LibreCare] Semantyka alarmu", "Inny alarm", "Zmiana znaczenia alarmu", "1. Otworz alerty")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Zmienia semantyke medyczna.",
				"severity": "HIGH",
				"safety_impact": "HIGH",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0003"],
			},
		)
		bug = self.read_json(self.root / "product" / "bugs" / f"{bug_id}.json")
		self.assertEqual("NEEDS_PRODUCT_DECISION", bug["status"])

	def test_inconclusive_report_has_no_auto_fix(self):
		bug_id = self.import_bug(
			self.bug_issue_event(404, "[Blad LibreCare] Niewiadomy", "Czasami dziala", "Nie wiem", "Brak stabilnych krokow")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "INCONCLUSIVE",
				"reasoning": "Brak deterministycznej reprodukcji.",
				"severity": "LOW",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [],
				"recommended_related_tests": [],
			},
		)
		self.assertEqual(1, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token"))
		bug = self.read_json(self.root / "product" / "bugs" / f"{bug_id}.json")
		self.assertEqual("TRIAGED", bug["status"])

	def test_duplicate_bug_does_not_create_duplicate_fix_or_record(self):
		first = self.import_bug(
			self.bug_issue_event(405, "[Blad LibreCare] Duplikat", "Ten sam objaw", "Ten sam wynik", "1. Otworz dashboard")
		)
		second_event = self.bug_issue_event(406, "[Blad LibreCare] Duplikat2", "Ten sam objaw", "Ten sam wynik", "1. Otworz dashboard")
		second = self.import_bug(second_event)
		self.assertEqual(first, second)
		bug_files = sorted((self.root / "product" / "bugs").glob("BUG-*.json"))
		self.assertEqual(1, len(bug_files))

	def test_confirmed_bug_assigns_copilot_once(self):
		bug_id = self.import_bug(
			self.bug_issue_event(407, "[Blad LibreCare] Jeden assignment", "ISO", "Naturalny czas", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "MEDIUM",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0007"],
			},
		)
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token"))
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token"))
		self.assertEqual(1, len(FakeGitHubClient.created_issues))
		self.assertEqual(1, len(FakeGitHubClient.assignments))

	def test_assignment_success_but_activation_fails_is_not_started(self):
		FakeGitHubClient.activation_should_fail = True
		FakeGitHubClient.activation_error = "GraphQL mutation failed"
		bug_id = self.import_bug(
			self.bug_issue_event(412, "[Blad LibreCare] Start fail", "Regresja", "Naprawa", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "MEDIUM",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0012"],
			},
		)
		out = self.root / "handoff-start-failed.json"
		self.assertEqual(
			0,
			self.cli.cmd_bug_sync_fix_handoff(
				bug_id,
				"example",
				"repo",
				"token",
				output_file=str(out),
				copilot_agent_user_token="copilot-user-token",
			),
		)
		summary = self.read_json(out)
		self.assertEqual("ASSIGNED", summary["copilot_assignment_state"])
		self.assertEqual("START_FAILED", summary["copilot_start_state"])
		impl = self.read_json(self.root / "product" / "implementation" / f"IMP-{bug_id}.json")
		self.assertEqual("START_FAILED", impl["copilot_assignment"]["start_state"])

	def test_activation_success_records_started_state(self):
		bug_id = self.import_bug(
			self.bug_issue_event(413, "[Blad LibreCare] Start ok", "Regresja", "Naprawa", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "MEDIUM",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0013"],
			},
		)
		out = self.root / "handoff-started.json"
		self.assertEqual(
			0,
			self.cli.cmd_bug_sync_fix_handoff(
				bug_id,
				"example",
				"repo",
				"token",
				output_file=str(out),
				copilot_agent_user_token="copilot-user-token",
			),
		)
		summary = self.read_json(out)
		self.assertEqual("ASSIGNED", summary["copilot_assignment_state"])
		self.assertEqual("STARTED", summary["copilot_start_state"])
		self.assertEqual(1, len(FakeGitHubClient.activations))

	def test_activation_failure_error_is_recorded_without_token_leak(self):
		FakeGitHubClient.activation_should_fail = True
		FakeGitHubClient.activation_error = "GraphQL error: forbidden"
		bug_id = self.import_bug(
			self.bug_issue_event(414, "[Blad LibreCare] Start gql fail", "Regresja", "Naprawa", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "MEDIUM",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0014"],
			},
		)
		self.assertEqual(
			0,
			self.cli.cmd_bug_sync_fix_handoff(
				bug_id,
				"example",
				"repo",
				"token",
				copilot_agent_user_token="super-secret-token",
			),
		)
		impl = self.read_json(self.root / "product" / "implementation" / f"IMP-{bug_id}.json")
		self.assertEqual("START_FAILED", impl["copilot_assignment"]["start_state"])
		self.assertIn("ACTIVATION_FAILED", impl["copilot_assignment"]["activation"]["reason"])
		self.assertNotIn("super-secret-token", json.dumps(impl, ensure_ascii=False))

	def test_existing_assigned_issue_without_started_state_is_not_treated_as_started(self):
		FakeGitHubClient.activation_should_fail = True
		bug_id = self.import_bug(
			self.bug_issue_event(422, "[Blad LibreCare] Existing assigned", "Regresja", "Naprawa", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "MEDIUM",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0022"],
			},
		)
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token"))
		out = self.root / "handoff-existing-assigned.json"
		self.assertEqual(
			0,
			self.cli.cmd_bug_sync_fix_handoff(
				bug_id,
				"example",
				"repo",
				"token",
				output_file=str(out),
				copilot_agent_user_token="copilot-user-token",
			),
		)
		summary = self.read_json(out)
		self.assertEqual("START_FAILED", summary["copilot_start_state"])

	def test_validated_bug_and_impl_with_product_pass_is_eligible_for_closure(self):
		bug_id = self.import_bug(
			self.bug_issue_event(415, "[Blad LibreCare] Close eligible", "Regresja", "Naprawa", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "HIGH",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0015"],
			},
		)
		self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", copilot_agent_user_token="copilot-user-token")
		bug_path = self.root / "product" / "bugs" / f"{bug_id}.json"
		bug = self.read_json(bug_path)
		bug["status"] = "VALIDATED"
		bug["validation_state"] = "VALIDATED"
		self.write_json(bug_path, bug)
		impl_path = self.root / "product" / "implementation" / f"IMP-{bug_id}.json"
		impl = self.read_json(impl_path)
		impl["status"] = "VALIDATED"
		impl["validation_state"] = "VALIDATED"
		self.write_json(impl_path, impl)
		result = self.close_eligible_bug_issue(bug_id, product_validation_pass=True)
		self.assertTrue(result["eligible"])
		self.assertIn(result["action"], {"CLOSED", "ALREADY_CLOSED"})
		self.assertIn(int(bug["implementation_issue_number"]), FakeGitHubClient.close_calls)

	def test_issue_remains_open_when_bug_or_impl_not_validated(self):
		bug_id = self.import_bug(
			self.bug_issue_event(416, "[Blad LibreCare] Not validated", "Regresja", "Naprawa", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "HIGH",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0016"],
			},
		)
		self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", copilot_agent_user_token="copilot-user-token")
		result = self.close_eligible_bug_issue(bug_id, product_validation_pass=True)
		self.assertFalse(result["eligible"])
		self.assertEqual("SKIPPED", result["action"])
		self.assertEqual([], FakeGitHubClient.close_calls)

	def test_issue_remains_open_when_product_validation_fails(self):
		bug_id = self.import_bug(
			self.bug_issue_event(417, "[Blad LibreCare] Product fail", "Regresja", "Naprawa", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "HIGH",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0017"],
			},
		)
		self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", copilot_agent_user_token="copilot-user-token")
		bug_path = self.root / "product" / "bugs" / f"{bug_id}.json"
		bug = self.read_json(bug_path)
		bug["status"] = "VALIDATED"
		bug["validation_state"] = "VALIDATED"
		self.write_json(bug_path, bug)
		impl_path = self.root / "product" / "implementation" / f"IMP-{bug_id}.json"
		impl = self.read_json(impl_path)
		impl["status"] = "VALIDATED"
		impl["validation_state"] = "VALIDATED"
		self.write_json(impl_path, impl)
		result = self.close_eligible_bug_issue(bug_id, product_validation_pass=False)
		self.assertFalse(result["eligible"])
		self.assertEqual("PRODUCT_VALIDATION_FAILED", result["reason"])
		self.assertEqual([], FakeGitHubClient.close_calls)

	def test_issue_closure_is_idempotent_when_already_closed(self):
		bug_id = self.import_bug(
			self.bug_issue_event(418, "[Blad LibreCare] Already closed", "Regresja", "Naprawa", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "HIGH",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0018"],
			},
		)
		handoff_code = self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", copilot_agent_user_token="copilot-user-token")
		self.assertEqual(0, handoff_code)
		bug_path = self.root / "product" / "bugs" / f"{bug_id}.json"
		bug = self.read_json(bug_path)
		impl_path = self.root / "product" / "implementation" / f"IMP-{bug_id}.json"
		impl = self.read_json(impl_path)
		bug["status"] = "VALIDATED"
		bug["validation_state"] = "VALIDATED"
		impl["status"] = "VALIDATED"
		impl["validation_state"] = "VALIDATED"
		self.write_json(bug_path, bug)
		self.write_json(impl_path, impl)
		FakeGitHubClient.issues[int(bug["implementation_issue_number"])]["state"] = "closed"
		result = self.close_eligible_bug_issue(bug_id, product_validation_pass=True)
		self.assertTrue(result["eligible"])
		self.assertEqual("ALREADY_CLOSED", result["action"])
		self.assertEqual([], FakeGitHubClient.close_calls)

	def test_unrelated_issue_is_never_closed(self):
		bug_id = self.import_bug(
			self.bug_issue_event(419, "[Blad LibreCare] Unrelated issue", "Regresja", "Naprawa", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "HIGH",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0019"],
			},
		)
		self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", copilot_agent_user_token="copilot-user-token")
		bug_path = self.root / "product" / "bugs" / f"{bug_id}.json"
		bug = self.read_json(bug_path)
		impl_path = self.root / "product" / "implementation" / f"IMP-{bug_id}.json"
		impl = self.read_json(impl_path)
		bug["status"] = "VALIDATED"
		bug["validation_state"] = "VALIDATED"
		impl["status"] = "VALIDATED"
		impl["validation_state"] = "VALIDATED"
		self.write_json(bug_path, bug)
		self.write_json(impl_path, impl)
		FakeGitHubClient.issues[999] = {"number": 999, "state": "open"}
		self.close_eligible_bug_issue(bug_id, product_validation_pass=True)
		self.assertNotIn(999, FakeGitHubClient.close_calls)

	def test_bulk_issue_closure_only_touches_eligible_bug_issue_numbers(self):
		bug_id = self.import_bug(
			self.bug_issue_event(421, "[Blad LibreCare] Bulk close", "Regresja", "Naprawa", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "HIGH",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0021"],
			},
		)
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", copilot_agent_user_token="copilot-user-token"))
		bug_path = self.root / "product" / "bugs" / f"{bug_id}.json"
		impl_path = self.root / "product" / "implementation" / f"IMP-{bug_id}.json"
		bug = self.read_json(bug_path)
		impl = self.read_json(impl_path)
		bug["status"] = "VALIDATED"
		bug["validation_state"] = "VALIDATED"
		impl["status"] = "VALIDATED"
		impl["validation_state"] = "VALIDATED"
		self.write_json(bug_path, bug)
		self.write_json(impl_path, impl)
		FakeGitHubClient.issues[999] = {"number": 999, "state": "open"}
		out = self.root / "close-all.json"
		self.assertEqual(
			0,
			self.cli.cmd_validate_and_close_all_eligible_issues(
				repo_owner="example",
				repo_name="repo",
				github_token="token",
				product_validation_pass=True,
				output_file=str(out),
			),
		)
		payload = self.read_json(out)
		self.assertEqual(1, payload["processed"])
		self.assertNotIn(999, FakeGitHubClient.close_calls)

	def test_bug_pr_ci_failure_uses_bounded_repair_loop(self):
		bug_id = self.import_bug(
			self.bug_issue_event(408, "[Blad LibreCare] CI loop", "Regresja", "Naprawa", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "HIGH",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0008"],
			},
		)
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token"))
		bug_path = self.root / "product" / "bugs" / f"{bug_id}.json"
		bug = self.read_json(bug_path)
		bug["pull_request"] = {"number": 77, "url": "https://example/pr/77", "branch": "copilot/bug"}
		self.write_json(bug_path, bug)
		self.cli.ensure_bug_impl(bug)

		out = self.root / "ci.json"
		self.assertEqual(
			0,
			self.cli.cmd_record_ci_result(
				77,
				"Android CI",
				"failure",
				"run-1",
				"https://run/1",
				repo_owner="example",
				repo_name="repo",
				github_token="token",
				output_file=str(out),
			),
		)
		first = self.read_json(out)
		self.assertEqual("REPAIRING", first["status"])
		self.assertTrue(first["repair_attempted"])

		self.assertEqual(
			0,
			self.cli.cmd_record_ci_result(77, "Android CI", "failure", "run-1", "https://run/1", output_file=str(out)),
		)
		duplicate = self.read_json(out)
		self.assertEqual("DEDUPLICATED", duplicate["action"])

	def test_three_unique_ci_failures_stop_at_failed(self):
		bug_id = self.import_bug(
			self.bug_issue_event(409, "[Blad LibreCare] Limit", "Regresja", "Naprawa", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "HIGH",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0009"],
			},
		)
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token"))
		bug_path = self.root / "product" / "bugs" / f"{bug_id}.json"
		bug = self.read_json(bug_path)
		bug["pull_request"] = {"number": 78, "url": "https://example/pr/78", "branch": "copilot/bug"}
		self.write_json(bug_path, bug)
		self.cli.ensure_bug_impl(bug)

		out = self.root / "ci-limit.json"
		self.assertEqual(0, self.cli.cmd_record_ci_result(78, "Android CI", "failure", "run-a", "https://run/a", output_file=str(out)))
		self.assertEqual("REPAIRING", self.read_json(out)["status"])
		self.assertEqual(0, self.cli.cmd_record_ci_result(78, "Android CI", "failure", "run-b", "https://run/b", output_file=str(out)))
		self.assertEqual("REPAIRING", self.read_json(out)["status"])
		self.assertEqual(0, self.cli.cmd_record_ci_result(78, "Android CI", "failure", "run-c", "https://run/c", output_file=str(out)))
		third = self.read_json(out)
		self.assertEqual("FAILED", third["status"])
		self.assertFalse(third["repair_attempted"])
		impl = self.read_json(self.root / "product" / "implementation" / f"IMP-{bug_id}.json")
		self.assertEqual(3, impl["attempt_count"])

	def test_ci_dedup_is_case_insensitive_for_conclusion(self):
		bug_id = self.import_bug(
			self.bug_issue_event(410, "[Blad LibreCare] Dedup", "Regresja", "Naprawa", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "MEDIUM",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0010"],
			},
		)
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token"))
		bug_path = self.root / "product" / "bugs" / f"{bug_id}.json"
		bug = self.read_json(bug_path)
		bug["pull_request"] = {"number": 79, "url": "https://example/pr/79", "branch": "copilot/bug"}
		self.write_json(bug_path, bug)
		self.cli.ensure_bug_impl(bug)

		out = self.root / "ci-dedup.json"
		self.assertEqual(0, self.cli.cmd_record_ci_result(79, "Android CI", "failure", "run-x", "https://run/x", output_file=str(out)))
		self.assertEqual(0, self.cli.cmd_record_ci_result(79, "Android CI", "FAILURE", "run-x", "https://run/x", output_file=str(out)))
		dup = self.read_json(out)
		self.assertEqual("DEDUPLICATED", dup["action"])

	def test_successful_ci_result_is_not_persisted_in_ci_failures(self):
		bug_id = self.import_bug(
			self.bug_issue_event(420, "[Blad LibreCare] ci failures semantics", "Regresja", "Naprawa", "1. Otworz dashboard")
		)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "MEDIUM",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0020"],
			},
		)
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", copilot_agent_user_token="copilot-user-token"))
		bug_path = self.root / "product" / "bugs" / f"{bug_id}.json"
		bug = self.read_json(bug_path)
		bug["pull_request"] = {"number": 90, "url": "https://example/pr/90", "branch": "copilot/bug"}
		self.write_json(bug_path, bug)
		self.cli.ensure_bug_impl(bug)
		out = self.root / "ci-success.json"
		self.assertEqual(0, self.cli.cmd_record_ci_result(90, "Android CI", "failure", "run-fail", "https://run/fail", output_file=str(out)))
		self.assertEqual(0, self.cli.cmd_record_ci_result(90, "Android CI", "success", "run-pass", "https://run/pass", output_file=str(out)))
		impl = self.read_json(self.root / "product" / "implementation" / f"IMP-{bug_id}.json")
		self.assertIn("run-fail:failure", impl["ci_failures"])
		self.assertFalse(any(str(x).lower().endswith(":success") for x in impl["ci_failures"]))

	def test_handoff_sanitizes_user_text_before_creating_issue(self):
		event = self.bug_issue_event(
			411,
			"[Blad LibreCare] ```rm -rf```",
			"<!-- inject --> surowy ISO",
			"naturalny czas",
			"1. Otworz dashboard",
		)
		bug_id = self.import_bug(event)
		self.triage_bug(
			bug_id,
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Regresja potwierdzona.",
				"severity": "MEDIUM",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [self.accepted_req_id],
				"recommended_related_tests": ["TESTRUN-0011"],
			},
		)
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token"))
		created = FakeGitHubClient.created_issues[-1]
		self.assertIn("OGRANICZENIA_AUTOMATYZACJI", created["body"])
		self.assertNotIn("```", created["body"])
		self.assertNotIn("<!-- inject -->", created["body"])

	def test_bug_pr_merge_sets_validation_pending(self):
		code = self.cli.cmd_bug_create(
			source="MANUAL",
			source_reference="MANUAL-001",
			title="Merge status",
			observed_behavior="x",
			expected_behavior="y",
			reproduction="z",
			related_requirement_ids=[self.accepted_req_id],
			related_test_ids=["TESTRUN-009"],
		)
		self.assertEqual(0, code)
		bug_id = "BUG-0001"
		bug_path = self.root / "product" / "bugs" / f"{bug_id}.json"
		bug = self.read_json(bug_path)
		bug["status"] = "PR_READY"
		self.write_json(bug_path, bug)

		pr_event = {
			"pull_request": {
				"number": 91,
				"html_url": "https://example/pr/91",
				"title": f"{bug_id} - fix",
				"body": f"Closes #1\n{bug_id}",
				"state": "closed",
				"merged": True,
				"head": {"ref": "copilot/bug-91"},
			}
		}
		event_file = self.root / "pr-event.json"
		self.write_json(event_file, pr_event)
		self.assertEqual(0, self.cli.cmd_track_pr(str(event_file)))
		updated = self.read_json(bug_path)
		self.assertEqual("VALIDATION_PENDING", updated["status"])
		self.assertEqual(91, updated["pull_request_number"])

	def test_canonical_bug_intake_sources_are_supported(self):
		for idx, source in enumerate(
			["FAILED_ACCEPTANCE_TESTRUN", "IMPLEMENTATION_ACCEPTANCE_FAILURE", "CI_REGRESSION", "MANUAL"],
			start=1,
		):
			self.assertEqual(
				0,
				self.cli.cmd_bug_create(
					source=source,
					source_reference=f"{source}-REF-{idx}",
					title=f"Source {source}",
					observed_behavior=f"Observed {idx}",
					expected_behavior="Expected",
					reproduction="Repro",
					related_requirement_ids=[self.accepted_req_id],
					related_test_ids=[f"TESTRUN-{idx:04d}"],
				),
			)
		bug_files = sorted((self.root / "product" / "bugs").glob("BUG-*.json"))
		self.assertEqual(4, len(bug_files))


if __name__ == "__main__":
	unittest.main()

