import importlib.util
import io
import json
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path
import zipfile

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
	assignment_tokens = []
	labels = []
	next_issue_number = 700
	issue_assignees = {}
	assign_should_fail = False
	job_list_should_fail = False
	artifact_list_should_fail = False
	job_log_should_fail = False
	artifact_download_should_fail = False
	workflow_jobs = {}
	workflow_artifacts = {}
	artifact_payloads = {}
	job_log_payloads = {}

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
			"title": title,
			"body": body,
			"labels": labels,
		}
		self.__class__.created_issues.append(response)
		self.__class__.issue_assignees.setdefault(issue_number, [])
		return response

	def _copilot_assignee_confirmed(self, issue_number: int):
		logins = list(self.__class__.issue_assignees.get(issue_number, []))
		return "copilot-swe-agent[bot]" in logins, logins

	def assign_copilot(self, issue_number: int, base_branch: str, instructions: str) -> dict:
		if self.__class__.assign_should_fail:
			raise RuntimeError("GraphQL assignment failed")
		self.__class__.assignment_tokens.append(self.token)
		logins = set(self.__class__.issue_assignees.get(issue_number, []))
		logins.add("copilot-swe-agent[bot]")
		self.__class__.issue_assignees[issue_number] = sorted(logins)
		payload = {
			"issue_number": issue_number,
			"base_branch": base_branch,
			"instructions": instructions,
			"status": "ASSIGNED",
			"method": "GRAPHQL",
		}
		self.__class__.assignments.append(payload)
		return payload

	def list_workflow_run_jobs(self, run_id: str) -> list[dict]:
		if self.__class__.job_list_should_fail:
			raise RuntimeError("GitHub Actions workflow job retrieval failed")
		return json.loads(json.dumps(self.__class__.workflow_jobs.get(str(run_id), [])))

	def list_workflow_run_artifacts(self, run_id: str) -> list[dict]:
		if self.__class__.artifact_list_should_fail:
			raise RuntimeError("GitHub Actions artifact retrieval failed")
		return json.loads(json.dumps(self.__class__.workflow_artifacts.get(str(run_id), [])))

	def download_workflow_artifact(self, artifact_id: int) -> bytes:
		if self.__class__.artifact_download_should_fail:
			raise RuntimeError("GitHub Actions artifact download failed")
		return self.__class__.artifact_payloads.get(int(artifact_id), b"")

	def download_workflow_job_logs(self, job_id: int) -> bytes:
		if self.__class__.job_log_should_fail:
			raise RuntimeError("GitHub Actions job log download failed")
		return self.__class__.job_log_payloads.get(int(job_id), b"")


class AutomationCliTest(unittest.TestCase):
	def setUp(self):
		self.cli = load_cli_module()
		self.temp_dir = tempfile.TemporaryDirectory()
		self.root = Path(self.temp_dir.name)
		shutil.copytree(WORKSPACE_ROOT / "product", self.root / "product")
		(self.root / "product" / "bugs").mkdir(exist_ok=True)
		(self.root / "product" / "implementation").mkdir(exist_ok=True)
		for bug_path in (self.root / "product" / "bugs").glob("BUG-*.json"):
			bug_path.unlink()
		for impl_path in (self.root / "product" / "implementation").glob("IMP-BUG-*.json"):
			impl_path.unlink()

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
		FakeGitHubClient.assignment_tokens = []
		FakeGitHubClient.labels = []
		FakeGitHubClient.next_issue_number = 700
		FakeGitHubClient.issue_assignees = {}
		FakeGitHubClient.assign_should_fail = False
		FakeGitHubClient.job_list_should_fail = False
		FakeGitHubClient.artifact_list_should_fail = False
		FakeGitHubClient.job_log_should_fail = False
		FakeGitHubClient.artifact_download_should_fail = False
		FakeGitHubClient.workflow_jobs = {}
		FakeGitHubClient.workflow_artifacts = {}
		FakeGitHubClient.artifact_payloads = {}
		FakeGitHubClient.job_log_payloads = {}

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

	def bug_impl_paths(self) -> list[Path]:
		return sorted((self.root / "product" / "implementation").glob("IMP-BUG-*.json"))

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

	def parse_bug_handoff_args(self, repo_name: str) -> tuple[int, dict]:
		captured = {}
		original = self.cli.cmd_bug_sync_fix_handoff

		def fake_cmd(*args):
			captured.update({
				"bug_id": args[0],
				"repo_owner": args[1],
				"repo_name": args[2],
				"github_token": args[3],
				"copilot_assignment_token": args[4],
			})
			return 0

		self.cli.cmd_bug_sync_fix_handoff = fake_cmd
		try:
			rc = self.cli.main([
				"bug-sync-fix-handoff",
				"--bug-id",
				"BUG-0001",
				"--repo-owner=rafalbragan",
				f"--repo-name={repo_name}",
				"--github-token",
				"token",
					"--copilot-assignment-token",
					"user-token",
			])
		finally:
			self.cli.cmd_bug_sync_fix_handoff = original
		return rc, captured

	def workflow_run_event(self, *, workflow_name: str, conclusion: str, branch: str = "master", event_name: str = "push", run_id: str = "1001", head_sha: str = "abc123def456", pull_requests=None, run_url: str | None = None) -> dict:
		return {
			"workflow_run": {
				"name": workflow_name,
				"conclusion": conclusion,
				"event": event_name,
				"id": run_id,
				"html_url": run_url or f"https://github.com/example/repo/actions/runs/{run_id}",
				"head_sha": head_sha,
				"head_branch": branch,
				"pull_requests": pull_requests if pull_requests is not None else [],
			}
		}

	def ci_regression_metadata(self, *, workflow_name: str, branch: str = "master", conclusion: str = "failure", run_id: str = "1001", head_sha: str = "abc123def456", failed_job: str = "Fast test suite", failed_step: str = "Fast test suite: Run fast suite", log_excerpt: str = ":app:compileDebugKotlin FAILED", deterministic_work_executed: bool = True, event_name: str = "push") -> dict:
		return {
			"workflow_name": workflow_name,
			"conclusion": conclusion,
			"event": event_name,
			"branch": branch,
			"run_id": run_id,
			"run_url": f"https://github.com/example/repo/actions/runs/{run_id}",
			"head_sha": head_sha,
			"failed_job": failed_job,
			"failed_step": failed_step,
			"log_excerpt": log_excerpt,
			"deterministic_work_executed": deterministic_work_executed,
		}

	def run_ci_regression_intake(self, event: dict, metadata: dict | None = None, output_name: str = "ci-regression.json") -> dict:
		event_file = self.root / "workflow-run-event.json"
		meta_file = self.root / "ci-regression-meta.json"
		output_file = self.root / output_name
		self.write_json(event_file, event)
		metadata_arg = None
		if metadata is not None:
			self.write_json(meta_file, metadata)
			metadata_arg = str(meta_file)
		self.assertEqual(0, self.cli.cmd_ci_regression_intake(str(event_file), metadata_arg, str(output_file)))
		return self.read_json(output_file)

	def run_git(self, cwd: Path, *args: str) -> subprocess.CompletedProcess:
		return subprocess.run(
			["git", *args],
			cwd=cwd,
			capture_output=True,
			text=True,
			encoding="utf-8",
			errors="replace",
		)

	def make_bug_record(self, bug_id: str, source: str, source_reference: str, observed: str, expected: str = "Oczekiwane", reproduction: str = "1. Repro") -> dict:
		bug = self.cli.build_bug_record(
			bug_id=bug_id,
			title=f"Bug {bug_id}",
			source=source,
			source_reference=source_reference,
			source_issue_number=None,
			source_issue_url="https://example/issue",
			related_requirement_ids=[],
			related_test_ids=[],
			observed_behavior=observed,
			expected_behavior=expected,
			reproduction=reproduction,
			severity="HIGH",
			safety_impact="LOW",
		)
		bug["dedup_signature"] = self.cli.bug_signature(bug)
		bug["status"] = "TRIAGED"
		return bug

	def make_bug_review(self, bug_id: str, status: str = "TRIAGED", classification: str = "INCONCLUSIVE") -> dict:
		return {
			"bug_id": bug_id,
			"classification": classification,
			"status": status,
			"generated_at": "2026-09-03T00:00:00Z",
		}

	def init_git_origin_with_baseline_bug(self) -> tuple[Path, Path]:
		baseline_bug = self.make_bug_record(
			"BUG-0001",
			"MANUAL",
			"baseline-reference",
			"Baseline observed",
		)
		self.write_json(self.root / "product" / "bugs" / "BUG-0001.json", baseline_bug)
		self.write_json(self.root / "product" / "generated" / "bug-reviews" / "BUG-0001.json", self.make_bug_review("BUG-0001"))
		self.assertEqual(0, self.run_git(self.root, "init", "-b", "master").returncode)
		self.assertEqual(0, self.run_git(self.root, "config", "user.name", "Test User").returncode)
		self.assertEqual(0, self.run_git(self.root, "config", "user.email", "test@example.com").returncode)
		self.assertEqual(0, self.run_git(self.root, "add", ".").returncode)
		commit = self.run_git(self.root, "commit", "-m", "init")
		self.assertEqual(0, commit.returncode, commit.stderr)
		remote_dir = self.root / "remote.git"
		self.assertEqual(0, self.run_git(self.root, "init", "--bare", str(remote_dir)).returncode)
		self.assertEqual(0, self.run_git(self.root, "remote", "add", "origin", str(remote_dir)).returncode)
		push = self.run_git(self.root, "push", "-u", "origin", "master")
		self.assertEqual(0, push.returncode, push.stderr)
		return self.root, remote_dir

	def clone_origin(self, remote_dir: Path, clone_name: str) -> Path:
		clone_dir = self.root / clone_name
		clone = self.run_git(self.root, "clone", str(remote_dir), str(clone_dir))
		self.assertEqual(0, clone.returncode, clone.stderr)
		self.assertEqual(0, self.run_git(clone_dir, "config", "user.name", "Other Writer").returncode)
		self.assertEqual(0, self.run_git(clone_dir, "config", "user.email", "other@example.com").returncode)
		return clone_dir

	def persist_bug_records(self, output_name: str = "persist.json") -> dict:
		output_file = self.root / output_name
		rc = self.cli.cmd_persist_bug_records("master", "product: persist test bug", output_file=str(output_file))
		self.assertEqual(0, rc)
		return self.read_json(output_file)

	def make_zip_payload(self, files: dict[str, str]) -> bytes:
		buffer = io.BytesIO()
		with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as archive:
			for name, text in files.items():
				archive.writestr(name, text)
		return buffer.getvalue()

	def configure_workflow_failure_diagnostics(self, run_id: str, *, job_log_text: str = "", artifact_name: str = "gradle-reports", log_file: str = "gradle-debug.log", artifact_log_text: str = ""):
		FakeGitHubClient.workflow_jobs[str(run_id)] = [{
			"id": 9001,
			"name": "Build debug APK",
			"conclusion": "failure",
			"steps": [
				{"name": "Build debug APK", "conclusion": "success"},
				{"name": "Fail if debug APK was not created", "conclusion": "failure"},
			],
		}]
		if job_log_text:
			FakeGitHubClient.job_log_payloads[9001] = job_log_text.encode("utf-8")
		if artifact_log_text:
			FakeGitHubClient.workflow_artifacts[str(run_id)] = [{"id": 9901, "name": artifact_name, "expired": False}]
			FakeGitHubClient.artifact_payloads[9901] = self.make_zip_payload({log_file: artifact_log_text})

	def enrich_ci_regression_bug(self, bug_id: str, run_id: str, output_name: str = "ci-enrich.json") -> dict:
		output_file = self.root / output_name
		rc = self.cli.cmd_ci_regression_enrich_evidence(
			bug_id=bug_id,
			repo_owner="example",
			repo_name="repo",
			github_token="token",
			run_id=run_id,
			output_file=str(output_file),
		)
		self.assertEqual(0, rc)
		return self.read_json(output_file)

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
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", "user-token", output_file=str(out)))
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
		self.assertEqual([], self.bug_impl_paths())

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
		self.assertEqual([], self.bug_impl_paths())

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
		self.assertEqual([], self.bug_impl_paths())

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
		self.assertEqual([], self.bug_impl_paths())

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
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", "user-token"))
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", "user-token"))
		self.assertEqual(1, len(FakeGitHubClient.created_issues))
		self.assertEqual(1, len(FakeGitHubClient.assignments))
		impl = self.read_json(self.root / "product" / "implementation" / f"IMP-{bug_id}.json")
		self.assertIsNone(impl["source_inbox_id"])
		self.assertEqual(407, impl["source_issue_number"])

	def test_bug_assignment_error_does_not_fake_assigned_state(self):
		bug_id = self.import_bug(
			self.bug_issue_event(420, "[Blad LibreCare] Assignment fail", "ISO", "Naturalny czas", "1. Otworz dashboard")
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
		FakeGitHubClient.assign_should_fail = True
		out = self.root / "bug-handoff-fail.json"
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", "user-token", output_file=str(out)))
		summary = self.read_json(out)
		self.assertFalse(summary["copilot_real_assignee_confirmed"])
		self.assertEqual("CONFIRMED_DEFECT", summary["status"])
		bug = self.read_json(self.root / "product" / "bugs" / f"{bug_id}.json")
		self.assertNotIn("assigned_agent", bug["implementation_issue"])

	def test_bug_retry_after_failed_assignment_reuses_same_issue(self):
		bug_id = self.import_bug(
			self.bug_issue_event(421, "[Blad LibreCare] Retry assignment", "ISO", "Naturalny czas", "1. Otworz dashboard")
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
		FakeGitHubClient.assign_should_fail = True
		first_out = self.root / "bug-handoff-retry-1.json"
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", "user-token", output_file=str(first_out)))
		first = self.read_json(first_out)
		self.assertEqual(1, len(FakeGitHubClient.created_issues))

		FakeGitHubClient.assign_should_fail = False
		second_out = self.root / "bug-handoff-retry-2.json"
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", "user-token", output_file=str(second_out)))
		second = self.read_json(second_out)
		self.assertEqual(first["implementation_issue_number"], second["implementation_issue_number"])
		self.assertEqual(1, len(FakeGitHubClient.created_issues))
		self.assertEqual(1, len(FakeGitHubClient.assignments))
		self.assertTrue(second["copilot_real_assignee_confirmed"])

	def test_bug_handoff_cli_parses_repo_name_without_hyphen(self):
		rc, captured = self.parse_bug_handoff_args("LibreDisplay")
		self.assertEqual(0, rc)
		self.assertEqual("rafalbragan", captured["repo_owner"])
		self.assertEqual("LibreDisplay", captured["repo_name"])
		self.assertEqual("user-token", captured["copilot_assignment_token"])

	def test_bug_handoff_cli_parses_repo_name_with_leading_hyphen(self):
		rc, captured = self.parse_bug_handoff_args("-LibreDisplay")
		self.assertEqual(0, rc)
		self.assertEqual("rafalbragan", captured["repo_owner"])
		self.assertEqual("-LibreDisplay", captured["repo_name"])
		self.assertEqual("user-token", captured["copilot_assignment_token"])

	def test_non_confirmed_triage_removes_preexisting_bug_impl_record(self):
		bug_id = self.import_bug(
			self.bug_issue_event(412, "[Blad LibreCare] Wstepny import", "Objaw", "Oczekiwanie", "1. Repro")
		)
		# Simulate stale state from older automation runs.
		self.cli.ensure_bug_impl(self.read_json(self.root / "product" / "bugs" / f"{bug_id}.json"))
		self.assertTrue((self.root / "product" / "implementation" / f"IMP-{bug_id}.json").exists())
		self.triage_bug(
			bug_id,
			{
				"classification": "INCONCLUSIVE",
				"reasoning": "Brak wystarczajacych danych.",
				"severity": "LOW",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [],
				"recommended_related_tests": [],
			},
		)
		self.assertFalse((self.root / "product" / "implementation" / f"IMP-{bug_id}.json").exists())

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
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", "user-token"))
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

	def test_android_ci_failure_on_master_routes_to_bug_intake(self):
		event = self.workflow_run_event(workflow_name="Android CI", conclusion="failure", branch="master", run_id="2001")
		metadata = self.ci_regression_metadata(workflow_name="Android CI", run_id="2001")
		result = self.run_ci_regression_intake(event, metadata, output_name="ci-master-android-ci.json")
		self.assertTrue(result["routed"])
		self.assertEqual("CI_REGRESSION", result["source"])
		bug = self.read_json(self.root / "product" / "bugs" / f"{result['bug_id']}.json")
		self.assertEqual("CI_REGRESSION", bug["source"])
		self.assertIn("workflow=Android CI", bug["source_reference"])
		self.assertIn("run_id=2001", bug["source_reference"])
		self.assertIn("branch=master", bug["source_reference"])
		self.assertIn(":app:compileDebugKotlin FAILED", bug["observed_behavior"])
		self.assertEqual([], self.bug_impl_paths())

	def test_android_apk_build_failure_on_master_routes_to_bug_intake(self):
		event = self.workflow_run_event(workflow_name="Android APK Build", conclusion="failure", branch="master", run_id="2002")
		metadata = self.ci_regression_metadata(
			workflow_name="Android APK Build",
			run_id="2002",
			failed_job="Build debug APK",
			failed_step="Build debug APK: Fail if debug APK was not created",
			log_excerpt=":app:compileDebugKotlin FAILED",
		)
		result = self.run_ci_regression_intake(event, metadata, output_name="ci-master-apk-build.json")
		self.assertTrue(result["routed"])
		bug = self.read_json(self.root / "product" / "bugs" / f"{result['bug_id']}.json")
		self.assertEqual("CI_REGRESSION", bug["source"])
		self.assertIn("workflow=Android APK Build", bug["source_reference"])

	def test_master_android_failure_with_gradle_reports_artifact_is_inspected(self):
		result = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android APK Build", conclusion="failure", branch="master", run_id="3001"),
			self.ci_regression_metadata(
				workflow_name="Android APK Build",
				run_id="3001",
				failed_job="Build debug APK",
				failed_step="Build debug APK: Fail if debug APK was not created",
				log_excerpt="Build debug APK: Fail if debug APK was not created",
			),
			output_name="ci-artifact-source.json",
		)
		self.configure_workflow_failure_diagnostics(
			"3001",
			artifact_log_text="FAILURE:\nExecution failed for task ':app:compileDebugKotlin'.\ne: file:///app/src/main/java/com/libredisplay/ui/monitoring/TrendProjection.kt:42: Unresolved reference: trendProjection\n",
		)
		payload = self.enrich_ci_regression_bug(result["bug_id"], "3001", "ci-artifact-enriched.json")
		self.assertTrue(payload["evidence_enriched"])
		self.assertEqual("ENRICHED", payload["result"])
		self.assertEqual(["gradle-reports"], payload["artifact_names"])
		self.assertEqual("gradle-reports", payload["artifact_name"])
		self.assertEqual("gradle-debug.log", payload["log_file"])
		self.assertGreaterEqual(payload["diagnostic_lines_found"], 1)

	def test_artifact_name_is_discovered_dynamically(self):
		result = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android APK Build", conclusion="failure", branch="master", run_id="3001a"),
			self.ci_regression_metadata(workflow_name="Android APK Build", run_id="3001a", failed_job="Build debug APK", failed_step="Build debug APK: Fail if debug APK was not created", log_excerpt="Build debug APK: Fail if debug APK was not created"),
			output_name="ci-artifact-dynamic-source.json",
		)
		self.configure_workflow_failure_diagnostics(
			"3001a",
			artifact_name="librecare-fast-suite",
			artifact_log_text=":app:compileDebugKotlin FAILED\nExecution failed for task ':app:compileDebugKotlin'.\n",
		)
		payload = self.enrich_ci_regression_bug(result["bug_id"], "3001a", "ci-artifact-dynamic-enriched.json")
		self.assertTrue(payload["evidence_enriched"])
		self.assertEqual("librecare-fast-suite", payload["artifact_name"])
		self.assertIn("librecare-fast-suite", payload["artifact_names"])

	def test_retrieval_failure_returns_structured_failure_without_silent_success(self):
		result = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android APK Build", conclusion="failure", branch="master", run_id="3001b"),
			self.ci_regression_metadata(workflow_name="Android APK Build", run_id="3001b", failed_job="Build debug APK", failed_step="Build debug APK: Fail if debug APK was not created", log_excerpt="Build debug APK: Fail if debug APK was not created"),
			output_name="ci-retrieval-failure-source.json",
		)
		FakeGitHubClient.job_list_should_fail = True
		payload = self.enrich_ci_regression_bug(result["bug_id"], "3001b", "ci-retrieval-failure.json")
		self.assertEqual("RETRIEVAL_FAILED", payload["result"])
		self.assertFalse(payload["evidence_enriched"])
		self.assertFalse(payload["job_logs_fetched"])
		bug = self.read_json(self.root / "product" / "bugs" / f"{result['bug_id']}.json")
		self.assertNotIn("ci_failure_evidence", bug)

	def test_compile_debug_kotlin_failure_captures_concise_compiler_excerpt(self):
		result = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android APK Build", conclusion="failure", branch="master", run_id="3002"),
			self.ci_regression_metadata(workflow_name="Android APK Build", run_id="3002", failed_job="Build debug APK", failed_step="Build debug APK: Fail if debug APK was not created", log_excerpt="Build debug APK: Fail if debug APK was not created"),
			output_name="ci-compiler-source.json",
		)
		self.configure_workflow_failure_diagnostics(
			"3002",
			artifact_log_text="""
:app:compileDebugKotlin FAILED
FAILURE: Build failed with an exception.
Execution failed for task ':app:compileDebugKotlin'.
e: file:///app/src/main/java/com/libredisplay/ui/monitoring/MonitoringScreen.kt:101: No value passed for parameter 'trendWindowMinutes'
e: file:///app/src/main/java/com/libredisplay/ui/monitoring/TrendProjection.kt:42: Unresolved reference: trendProjection
""",
		)
		self.enrich_ci_regression_bug(result["bug_id"], "3002", "ci-compiler-enriched.json")
		bug = self.read_json(self.root / "product" / "bugs" / f"{result['bug_id']}.json")
		excerpt = bug["ci_failure_evidence"]["excerpt"]
		self.assertIn(":app:compileDebugKotlin FAILED", excerpt)
		self.assertIn("No value passed for parameter 'trendWindowMinutes'", excerpt)
		self.assertIn("Unresolved reference: trendProjection", excerpt)

	def test_missing_useful_artifact_keeps_ci_regression_safely_inconclusive(self):
		result = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android APK Build", conclusion="failure", branch="master", run_id="3003"),
			self.ci_regression_metadata(workflow_name="Android APK Build", run_id="3003", failed_job="Build debug APK", failed_step="Build debug APK: Fail if debug APK was not created", log_excerpt="Build debug APK: Fail if debug APK was not created"),
			output_name="ci-missing-artifact.json",
		)
		payload = self.enrich_ci_regression_bug(result["bug_id"], "3003", "ci-missing-artifact-enrich.json")
		self.assertFalse(payload["evidence_enriched"])
		self.assertEqual("NO_USEFUL_EVIDENCE", payload["result"])
		self.triage_bug(result["bug_id"], {
			"classification": "INCONCLUSIVE",
			"reasoning": "Brak użytecznego artefaktu lub logu.",
			"severity": "HIGH",
			"safety_impact": "LOW",
			"requires_behavior_change": False,
			"recommended_related_requirements": [],
			"recommended_related_tests": [],
		})
		bug = self.read_json(self.root / "product" / "bugs" / f"{result['bug_id']}.json")
		self.assertEqual("INCONCLUSIVE", bug["classification"])
		self.assertEqual("TRIAGED", bug["status"])
		self.assertEqual([], self.bug_impl_paths())

	def test_huge_gradle_log_is_reduced_to_bounded_excerpt(self):
		result = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android APK Build", conclusion="failure", branch="master", run_id="3004"),
			self.ci_regression_metadata(workflow_name="Android APK Build", run_id="3004", failed_job="Build debug APK", failed_step="Build debug APK: Fail if debug APK was not created", log_excerpt="Build debug APK: Fail if debug APK was not created"),
			output_name="ci-huge-log.json",
		)
		noise = "\n".join([f"noise line {idx}" for idx in range(400)])
		artifact_log = noise + "\n:app:compileDebugKotlin FAILED\nFAILURE: Build failed with an exception.\nExecution failed for task ':app:compileDebugKotlin'.\n" + "\n".join([f"e: file:///src/File{idx}.kt:{idx}: Unresolved reference: item{idx}" for idx in range(1, 80)])
		self.configure_workflow_failure_diagnostics("3004", artifact_log_text=artifact_log)
		self.enrich_ci_regression_bug(result["bug_id"], "3004", "ci-huge-log-enrich.json")
		bug = self.read_json(self.root / "product" / "bugs" / f"{result['bug_id']}.json")
		excerpt = bug["ci_failure_evidence"]["excerpt"]
		self.assertLessEqual(len(excerpt), self.cli.MAX_FAILURE_EXCERPT_CHARS)
		self.assertLessEqual(len(excerpt.splitlines()), self.cli.MAX_FAILURE_EXCERPT_LINES)

	def test_same_bug_receives_additional_evidence_without_duplicate_bug(self):
		result = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android APK Build", conclusion="failure", branch="master", run_id="3005"),
			self.ci_regression_metadata(workflow_name="Android APK Build", run_id="3005", failed_job="Build debug APK", failed_step="Build debug APK: Fail if debug APK was not created", log_excerpt="Build debug APK: Fail if debug APK was not created"),
			output_name="ci-extra-evidence.json",
		)
		bug_id = result["bug_id"]
		self.configure_workflow_failure_diagnostics("3005", artifact_log_text=":app:compileDebugKotlin FAILED\nExecution failed for task ':app:compileDebugKotlin'.\n")
		self.enrich_ci_regression_bug(bug_id, "3005", "ci-extra-evidence-enrich.json")
		self.assertEqual(1, len(list((self.root / "product" / "bugs").glob("BUG-*.json"))))
		bug = self.read_json(self.root / "product" / "bugs" / f"{bug_id}.json")
		self.assertGreaterEqual(len(bug.get("evidence", [])), 2)

	def test_enriched_existing_bug_is_retriaged(self):
		result = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android APK Build", conclusion="failure", branch="master", run_id="3006"),
			self.ci_regression_metadata(workflow_name="Android APK Build", run_id="3006", failed_job="Build debug APK", failed_step="Build debug APK: Fail if debug APK was not created", log_excerpt="Build debug APK: Fail if debug APK was not created"),
			output_name="ci-retriage.json",
		)
		self.configure_workflow_failure_diagnostics("3006", artifact_log_text=":app:compileDebugKotlin FAILED\nExecution failed for task ':app:compileDebugKotlin'.\n")
		self.enrich_ci_regression_bug(result["bug_id"], "3006", "ci-retriage-enrich.json")
		prompt_file = self.root / "bug-ai-prompt.json"
		self.cli.main(["bug-build-ai-prompt", "--bug-id", result["bug_id"], "--output-file", str(prompt_file)])
		prompt = self.read_json(prompt_file)
		bug = self.read_json(self.root / "product" / "bugs" / f"{result['bug_id']}.json")
		self.assertEqual(bug["ci_failure_evidence"]["excerpt"], prompt["ci_failure_evidence_excerpt"])
		self.triage_bug(result["bug_id"], {
			"classification": "CONFIRMED_DEFECT",
			"reasoning": "Rzeczywisty błąd kompilacji Kotlin potwierdza regresję deterministycznego CI.",
			"severity": "HIGH",
			"safety_impact": "LOW",
			"requires_behavior_change": False,
			"recommended_related_requirements": [],
			"recommended_related_tests": [],
		})
		bug = self.read_json(self.root / "product" / "bugs" / f"{result['bug_id']}.json")
		self.assertEqual("CONFIRMED_DEFECT", bug["classification"])
		self.assertEqual("CONFIRMED_DEFECT", bug["status"])

	def test_confirmed_ci_regression_handoff_uses_user_token_and_creates_one_fix_issue(self):
		result = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android APK Build", conclusion="failure", branch="master", run_id="3007"),
			self.ci_regression_metadata(workflow_name="Android APK Build", run_id="3007", failed_job="Build debug APK", failed_step="Build debug APK: Fail if debug APK was not created", log_excerpt="Build debug APK: Fail if debug APK was not created"),
			output_name="ci-confirmed-handoff.json",
		)
		self.configure_workflow_failure_diagnostics("3007", artifact_log_text=":app:compileDebugKotlin FAILED\nExecution failed for task ':app:compileDebugKotlin'.\n")
		self.enrich_ci_regression_bug(result["bug_id"], "3007", "ci-confirmed-handoff-enrich.json")
		self.triage_bug(result["bug_id"], {
			"classification": "CONFIRMED_DEFECT",
			"reasoning": "To potwierdzona regresja kompilacji.",
			"severity": "HIGH",
			"safety_impact": "LOW",
			"requires_behavior_change": False,
			"recommended_related_requirements": ["REQ-DOES-NOT-EXIST", self.accepted_req_id],
			"recommended_related_tests": [],
		})
		out = self.root / "ci-confirmed-handoff-result.json"
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(result["bug_id"], "example", "repo", "token", "user-token", output_file=str(out)))
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(result["bug_id"], "example", "repo", "token", "user-token", output_file=str(out)))
		bug = self.read_json(self.root / "product" / "bugs" / f"{result['bug_id']}.json")
		self.assertEqual([self.accepted_req_id], bug["related_requirement_ids"])
		self.assertEqual(1, len(FakeGitHubClient.created_issues))
		self.assertEqual(1, len(FakeGitHubClient.assignments))
		self.assertEqual(["user-token"], FakeGitHubClient.assignment_tokens)

	def test_enriched_prompt_contains_ci_failure_excerpt(self):
		result = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android APK Build", conclusion="failure", branch="master", run_id="3007a"),
			self.ci_regression_metadata(workflow_name="Android APK Build", run_id="3007a", failed_job="Build debug APK", failed_step="Build debug APK: Fail if debug APK was not created", log_excerpt="Build debug APK: Fail if debug APK was not created"),
			output_name="ci-prompt-source.json",
		)
		self.configure_workflow_failure_diagnostics("3007a", artifact_name="librecare-fast-suite", artifact_log_text=":app:compileDebugKotlin FAILED\nExecution failed for task ':app:compileDebugKotlin'.\n")
		self.enrich_ci_regression_bug(result["bug_id"], "3007a", "ci-prompt-enriched.json")
		prompt_file = self.root / "bug-ai-prompt.json"
		self.assertEqual(0, self.cli.main(["bug-build-ai-prompt", "--bug-id", result["bug_id"], "--output-file", str(prompt_file)]))
		prompt = self.read_json(prompt_file)
		bug = self.read_json(self.root / "product" / "bugs" / f"{result['bug_id']}.json")
		self.assertEqual(bug["ci_failure_evidence"]["excerpt"], prompt["ci_failure_evidence_excerpt"])
		self.assertIn("compileDebugKotlin", prompt["ci_failure_evidence_excerpt"])

	def test_inconclusive_ci_regression_creates_no_implementation_issue(self):
		result = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android APK Build", conclusion="failure", branch="master", run_id="3008"),
			self.ci_regression_metadata(workflow_name="Android APK Build", run_id="3008", failed_job="Build debug APK", failed_step="Build debug APK: Fail if debug APK was not created", log_excerpt="Build debug APK: Fail if debug APK was not created"),
			output_name="ci-inconclusive-flow.json",
		)
		self.triage_bug(result["bug_id"], {
			"classification": "INCONCLUSIVE",
			"reasoning": "Nadal brak wystarczających dowodów.",
			"severity": "HIGH",
			"safety_impact": "LOW",
			"requires_behavior_change": False,
			"recommended_related_requirements": [],
			"recommended_related_tests": [],
		})
		self.assertEqual(1, self.cli.cmd_bug_sync_fix_handoff(result["bug_id"], "example", "repo", "token", "user-token"))
		self.assertEqual([], self.bug_impl_paths())

	def test_related_requirement_ids_keep_only_real_canonical_ids(self):
		result = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android APK Build", conclusion="failure", branch="master", run_id="3009"),
			self.ci_regression_metadata(workflow_name="Android APK Build", run_id="3009", failed_job="Build debug APK", failed_step="Build debug APK: Fail if debug APK was not created", log_excerpt="Build debug APK: Fail if debug APK was not created"),
			output_name="ci-canonical-reqs.json",
		)
		bug_path = self.root / "product" / "bugs" / f"{result['bug_id']}.json"
		bug = self.read_json(bug_path)
		bug["related_requirement_ids"] = ["REQ-CI-001: fake text", self.accepted_req_id]
		self.write_json(bug_path, bug)
		self.triage_bug(result["bug_id"], {
			"classification": "INCONCLUSIVE",
			"reasoning": "Sanity check.",
			"severity": "HIGH",
			"safety_impact": "LOW",
			"requires_behavior_change": False,
			"recommended_related_requirements": ["REQ-UNKNOWN", self.accepted_req_id],
			"recommended_related_tests": [],
		})
		updated = self.read_json(bug_path)
		self.assertEqual([self.accepted_req_id], updated["related_requirement_ids"])

	def test_successful_master_ci_does_not_create_bug(self):
		event = self.workflow_run_event(workflow_name="Android CI", conclusion="success", branch="master", run_id="2003")
		metadata = self.ci_regression_metadata(workflow_name="Android CI", conclusion="success", run_id="2003")
		result = self.run_ci_regression_intake(event, metadata, output_name="ci-master-success.json")
		self.assertFalse(result["routed"])
		self.assertEqual("NON_FAILURE_CONCLUSION", result["reason"])
		self.assertEqual([], list((self.root / "product" / "bugs").glob("BUG-*.json")))

	def test_skipped_or_cancelled_master_ci_does_not_create_bug(self):
		for conclusion in ["skipped", "cancelled"]:
			result = self.run_ci_regression_intake(
				self.workflow_run_event(workflow_name="Android CI", conclusion=conclusion, branch="master", run_id=f"2004-{conclusion}"),
				self.ci_regression_metadata(workflow_name="Android CI", conclusion=conclusion, run_id=f"2004-{conclusion}"),
				output_name=f"ci-master-{conclusion}.json",
			)
			self.assertFalse(result["routed"])
			self.assertEqual("NON_FAILURE_CONCLUSION", result["reason"])
		self.assertEqual([], list((self.root / "product" / "bugs").glob("BUG-*.json")))

	def test_pr_ci_failure_does_not_create_unrelated_master_bug(self):
		event = self.workflow_run_event(
			workflow_name="Android CI",
			conclusion="failure",
			branch="master",
			event_name="pull_request",
			run_id="2005",
			pull_requests=[{"number": 77}],
		)
		metadata = self.ci_regression_metadata(workflow_name="Android CI", event_name="pull_request", run_id="2005")
		result = self.run_ci_regression_intake(event, metadata, output_name="ci-pr-failure.json")
		self.assertFalse(result["routed"])
		self.assertEqual("PULL_REQUEST_CI", result["reason"])
		self.assertEqual([], list((self.root / "product" / "bugs").glob("BUG-*.json")))

	def test_duplicate_workflow_run_same_conclusion_is_idempotent(self):
		event = self.workflow_run_event(workflow_name="Android CI", conclusion="failure", branch="master", run_id="2006")
		metadata = self.ci_regression_metadata(workflow_name="Android CI", run_id="2006")
		first = self.run_ci_regression_intake(event, metadata, output_name="ci-dup-1.json")
		second = self.run_ci_regression_intake(event, metadata, output_name="ci-dup-2.json")
		self.assertTrue(first["routed"])
		self.assertTrue(second["routed"])
		self.assertEqual(first["bug_id"], second["bug_id"])
		self.assertEqual("CREATED", first["action"])
		self.assertEqual("DUPLICATED_SOURCE", second["action"])
		self.assertEqual(1, len(list((self.root / "product" / "bugs").glob("BUG-*.json"))))

	def test_two_writers_start_with_bug0002_and_only_one_canonical_bug0002_survives(self):
		_root, remote_dir = self.init_git_origin_with_baseline_bug()
		event = self.workflow_run_event(workflow_name="Android CI", conclusion="failure", branch="master", run_id="33694534681")
		metadata = self.ci_regression_metadata(workflow_name="Android CI", run_id="33694534681")
		result = self.run_ci_regression_intake(event, metadata, output_name="ci-persist-dup-source.json")
		self.assertEqual("BUG-0002", result["bug_id"])
		other = self.clone_origin(remote_dir, "other-writer")
		self.write_json(other / "product" / "bugs" / "BUG-0002.json", self.read_json(self.root / "product" / "bugs" / "BUG-0002.json"))
		self.write_json(other / "product" / "generated" / "bug-reviews" / "BUG-0002.json", self.make_bug_review("BUG-0002"))
		self.assertEqual(0, self.run_git(other, "add", ".").returncode)
		self.assertEqual(0, self.run_git(other, "commit", "-m", "remote duplicate bug").returncode)
		push = self.run_git(other, "push", "origin", "master")
		self.assertEqual(0, push.returncode, push.stderr)

		persist = self.persist_bug_records("persist-dup-source.json")
		self.assertEqual("BUG-0002", persist["persisted_bugs"][0]["final_bug_id"])
		self.assertEqual("DUPLICATED_SOURCE", persist["persisted_bugs"][0]["action"])
		self.assertTrue((self.root / "product" / "bugs" / "BUG-0002.json").exists())
		self.assertFalse((self.root / "product" / "bugs" / "BUG-0003.json").exists())

	def test_same_source_reference_remote_before_persistence_reuses_remote_bug(self):
		_root, remote_dir = self.init_git_origin_with_baseline_bug()
		event = self.workflow_run_event(workflow_name="Android CI", conclusion="failure", branch="master", run_id="33694534681")
		metadata = self.ci_regression_metadata(workflow_name="Android CI", run_id="33694534681")
		self.run_ci_regression_intake(event, metadata, output_name="ci-remote-reuse.json")
		other = self.clone_origin(remote_dir, "other-reuse")
		self.write_json(other / "product" / "bugs" / "BUG-0002.json", self.read_json(self.root / "product" / "bugs" / "BUG-0002.json"))
		self.write_json(other / "product" / "generated" / "bug-reviews" / "BUG-0002.json", self.make_bug_review("BUG-0002"))
		self.assertEqual(0, self.run_git(other, "add", ".").returncode)
		self.assertEqual(0, self.run_git(other, "commit", "-m", "remote same source bug").returncode)
		self.assertEqual(0, self.run_git(other, "push", "origin", "master").returncode)

		persist = self.persist_bug_records("persist-remote-reuse.json")
		self.assertIn(persist["status"], {"NO_CHANGES", "PUSHED"})
		self.assertEqual("BUG-0002", persist["persisted_bugs"][0]["final_bug_id"])

	def test_different_remote_bug_using_planned_id_allocates_next_free_id_safely(self):
		_root, remote_dir = self.init_git_origin_with_baseline_bug()
		event = self.workflow_run_event(workflow_name="Android CI", conclusion="failure", branch="master", run_id="33694534681")
		metadata = self.ci_regression_metadata(workflow_name="Android CI", run_id="33694534681")
		self.run_ci_regression_intake(event, metadata, output_name="ci-remote-renumber.json")
		other = self.clone_origin(remote_dir, "other-renumber")
		remote_bug = self.make_bug_record(
			"BUG-0002",
			"MANUAL",
			"different-remote-bug",
			"Inny zdalny bug",
			"Inny expected",
			"1. Inny repro",
		)
		self.write_json(other / "product" / "bugs" / "BUG-0002.json", remote_bug)
		self.write_json(other / "product" / "generated" / "bug-reviews" / "BUG-0002.json", self.make_bug_review("BUG-0002", classification="CONFIRMED_DEFECT", status="CONFIRMED_DEFECT"))
		self.assertEqual(0, self.run_git(other, "add", ".").returncode)
		self.assertEqual(0, self.run_git(other, "commit", "-m", "remote unrelated bug").returncode)
		push = self.run_git(other, "push", "origin", "master")
		self.assertEqual(0, push.returncode, push.stderr)

		persist = self.persist_bug_records("persist-remote-renumber.json")
		self.assertEqual("PUSHED", persist["status"])
		self.assertEqual("BUG-0003", persist["persisted_bugs"][0]["final_bug_id"])
		self.assertTrue((self.root / "product" / "bugs" / "BUG-0003.json").exists())
		review = self.read_json(self.root / "product" / "generated" / "bug-reviews" / "BUG-0003.json")
		self.assertEqual("BUG-0003", review["bug_id"])

	def test_unrelated_canonical_conflict_stops_without_destructive_resolution(self):
		self.init_git_origin_with_baseline_bug()
		event = self.workflow_run_event(workflow_name="Android CI", conclusion="failure", branch="master", run_id="33694534681")
		metadata = self.ci_regression_metadata(workflow_name="Android CI", run_id="33694534681")
		self.run_ci_regression_intake(event, metadata, output_name="ci-unrelated-stop.json")
		(self.root / "product" / "requirements" / "REQ-0001.yaml").write_text("changed: true\n", encoding="utf-8")
		output_file = self.root / "persist-stop.json"
		rc = self.cli.cmd_persist_bug_records("master", "product: should stop", output_file=str(output_file))
		self.assertEqual(1, rc)
		self.assertFalse(output_file.exists())

	def test_same_root_cause_across_different_runs_deduplicates_bug(self):
		first = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android CI", conclusion="failure", branch="master", run_id="2007"),
			self.ci_regression_metadata(workflow_name="Android CI", run_id="2007", head_sha="sha2007"),
			output_name="ci-root-1.json",
		)
		second = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android CI", conclusion="failure", branch="master", run_id="2008"),
			self.ci_regression_metadata(workflow_name="Android CI", run_id="2008", head_sha="sha2008"),
			output_name="ci-root-2.json",
		)
		self.assertEqual(first["bug_id"], second["bug_id"])
		self.assertEqual("DUPLICATED_ROOT_CAUSE", second["action"])

	def test_ci_regression_source_can_be_confirmed_defect_without_req_ids(self):
		result = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android CI", conclusion="failure", branch="master", run_id="2009"),
			self.ci_regression_metadata(workflow_name="Android CI", run_id="2009"),
			output_name="ci-triage-source.json",
		)
		self.triage_bug(
			result["bug_id"],
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "To potwierdzona regresja deterministycznego CI na master.",
				"severity": "HIGH",
				"safety_impact": "LOW",
				"requires_behavior_change": False,
				"recommended_related_requirements": [],
				"recommended_related_tests": [],
			},
		)
		bug = self.read_json(self.root / "product" / "bugs" / f"{result['bug_id']}.json")
		self.assertEqual("CONFIRMED_DEFECT", bug["status"])
		self.assertTrue(bug["triage_traceability"]["has_ci_regression_evidence"])

	def test_ci_regression_new_behavior_or_safety_change_still_routes_to_product_decision(self):
		result = self.run_ci_regression_intake(
			self.workflow_run_event(workflow_name="Android CI", conclusion="failure", branch="master", run_id="2010"),
			self.ci_regression_metadata(workflow_name="Android CI", run_id="2010"),
			output_name="ci-triage-needs-product.json",
		)
		self.triage_bug(
			result["bug_id"],
			{
				"classification": "CONFIRMED_DEFECT",
				"reasoning": "Naprawa wymaga zmiany semantyki produktu.",
				"severity": "HIGH",
				"safety_impact": "HIGH",
				"requires_behavior_change": True,
				"recommended_related_requirements": [],
				"recommended_related_tests": [],
			},
		)
		bug = self.read_json(self.root / "product" / "bugs" / f"{result['bug_id']}.json")
		self.assertEqual("NEEDS_PRODUCT_DECISION", bug["status"])

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
		self.assertEqual(0, self.cli.cmd_bug_sync_fix_handoff(bug_id, "example", "repo", "token", "user-token"))
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

	def test_automation_cli_contains_graphql_assignment_flow(self):
		text = (WORKSPACE_ROOT / "scripts" / "product" / "automation_cli.py").read_text(encoding="utf-8")
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
		self.assertNotIn('"model": "Auto"', text)
		self.assertIn("MASTER_CI_REGRESSION_WORKFLOWS", text)
		self.assertIn("def cmd_ci_regression_intake", text)
		self.assertIn("def cmd_ci_regression_enrich_evidence", text)
		self.assertIn("download_workflow_artifact", text)
		self.assertIn("download_workflow_job_logs", text)
		self.assertIn("extract_bounded_failure_excerpt", text)
		self.assertIn("canonical_requirement_ids", text)
		self.assertIn("def cmd_persist_bug_records", text)
		self.assertIn('persist-bug-records', text)
		self.assertIn('"CI_REGRESSION"', text)


class AutomationCliGraphQLAssignmentContractTest(unittest.TestCase):
	def setUp(self):
		self.cli = load_cli_module()

	def make_client(self):
		return self.cli.GitHubClient("rafalbragan", "-LibreDisplay", "token")

	def test_add_assignees_payload_uses_supported_agent_assignment_fields(self):
		client = self.make_client()
		captured = []

		client._get_repository_node_id = lambda: "R_repo"
		client._get_issue_node_id = lambda issue_number: "I_issue"
		client._get_actor_node_id = lambda login: "BOT_NODE_ID"
		client._copilot_assignee_confirmed = lambda issue_number: (True, [self.cli.COPILOT_AGENT_LOGIN])

		def fake_graphql(query: str, variables: dict | None = None):
			captured.append({"query": query, "variables": json.loads(json.dumps(variables or {}))})
			return {
				"data": {
					"addAssigneesToAssignable": {
						"assignable": {
							"number": 7,
							"assignees": {"nodes": [{"login": self.cli.COPILOT_AGENT_LOGIN, "id": "BOT_NODE_ID"}]},
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
		self.assertEqual("assigneeIds", result["assignee_id_field"])
		self.assertEqual("Instrukcje testowe", agent_assignment["customInstructions"])
		self.assertEqual("GPT-5.4 mini", agent_assignment["model"])
		self.assertTrue(agent_assignment["model"].strip())
		self.assertNotEqual("auto", agent_assignment["model"].strip().lower())
		self.assertNotIn("agentLogin", agent_assignment)
		self.assertNotIn("instructions", agent_assignment)

	def test_replace_actors_fallback_uses_actor_ids(self):
		client = self.make_client()
		captured = []
		client._get_repository_node_id = lambda: "R_repo"
		client._get_issue_node_id = lambda issue_number: "I_issue"
		client._get_actor_node_id = lambda login: "BOT_NODE_ID"
		client._copilot_assignee_confirmed = lambda issue_number: (True, [self.cli.COPILOT_AGENT_LOGIN])

		def fake_graphql(query: str, variables: dict | None = None):
			captured.append({"query": query, "variables": json.loads(json.dumps(variables or {}))})
			if "addAssigneesToAssignable" in query:
				return {"errors": [{"message": "preview mismatch"}]}
			return {
				"data": {
					"replaceActorsForAssignable": {
						"assignable": {
							"number": 7,
							"assignees": {"nodes": [{"login": self.cli.COPILOT_AGENT_LOGIN, "id": "BOT_NODE_ID"}]},
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
		self.assertEqual("GPT-5.4 mini", agent_assignment["model"])
		self.assertTrue(agent_assignment["model"].strip())
		self.assertNotEqual("auto", agent_assignment["model"].strip().lower())

	def test_graphql_success_without_copilot_assignee_raises(self):
		client = self.make_client()
		client._get_repository_node_id = lambda: "R_repo"
		client._get_issue_node_id = lambda issue_number: "I_issue"
		client._get_actor_node_id = lambda login: "BOT_NODE_ID"
		client._copilot_assignee_confirmed = lambda issue_number: (False, ["someone-else"])
		client._graphql = lambda query, variables=None: {
			"data": {
				"addAssigneesToAssignable": {
					"assignable": {
						"number": 7,
						"assignees": {"nodes": [{"login": "someone-else", "id": "OTHER"}]},
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


if __name__ == "__main__":
	unittest.main()

