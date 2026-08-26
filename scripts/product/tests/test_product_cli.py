import contextlib
import importlib.util
import io
import json
import shutil
import tempfile
import unittest
from pathlib import Path


WORKSPACE_ROOT = Path(__file__).resolve().parents[3]
CLI_PATH = WORKSPACE_ROOT / "scripts" / "product" / "product_cli.py"


def load_cli_module():
    spec = importlib.util.spec_from_file_location("product_cli_under_test", CLI_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    spec.loader.exec_module(module)
    return module


class ProductCliTest(unittest.TestCase):
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
            yield
        finally:
            for key, value in original.items():
                setattr(self.cli, key, value)

    def validate(self):
        with self.patched_paths():
            return self.cli.cmd_validate()

    def summary_text(self):
        buffer = io.StringIO()
        with self.patched_paths(), contextlib.redirect_stdout(buffer):
            code = self.cli.cmd_summary()
        self.assertEqual(0, code)
        return buffer.getvalue()

    def write_json(self, path: Path, payload: dict):
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("w", encoding="utf-8") as fh:
            json.dump(payload, fh, indent=2)
            fh.write("\n")

    def valid_observation(self, obs_id: str = "OBS-TEST-0001"):
        return {
            "id": obs_id,
            "created_at": "2026-08-26",
            "source_type": "manual",
            "source_reference": "unit-test",
            "persona": "caregiver",
            "mode": "caregiver",
            "module": "Home / Monitoring",
            "type": "bug",
            "severity": "low",
            "frequency": "rare",
            "confidence": "high",
            "evidence": ["test evidence"],
            "problem_statement": "test problem",
            "status": "new",
            "linked_requirements": [],
        }

    def valid_requirement(self, req_id: str = "REQ-TEST-0001"):
        return {
            "id": req_id,
            "status": "CANDIDATE",
            "problem": "test requirement problem",
            "target_personas": ["caregiver"],
            "target_modes": ["caregiver"],
            "target_modules": ["Home / Monitoring"],
            "linked_observations": ["OBS-EXAMPLE-0001"],
            "user_outcome": "test user outcome",
            "solution_options": [
                {"id": "opt-a", "summary": "A", "pros": ["p"], "cons": ["c"]},
                {"id": "opt-b", "summary": "B", "pros": ["p"], "cons": ["c"]},
            ],
            "recommended_option": "opt-a",
            "counterargument": "test counterargument",
            "scores": {
                "safety": 5,
                "caregiver_value": 5,
                "senior_value": 5,
                "clinician_value": 5,
                "frequency": 5,
                "confidence": 5,
                "complexity": 5,
                "strategy_alignment": 5,
            },
            "safety_implications": "test safety",
            "acceptance_criteria": ["criterion"],
            "test_plan": ["test"],
            "human_decision": "PENDING",
            "related_decisions": ["DEC-0001"],
        }

    def valid_test_run(self, run_id: str = "TESTRUN-2026-901"):
        return {
            "id": run_id,
            "created_at": "2026-08-26",
            "source": "firebase_app_testing_agent",
            "test_case": "Caregiver — assess current glucose situation",
            "persona": "caregiver",
            "mode": "caregiver",
            "module": "Home / Monitoring",
            "result": "PASS",
            "environment": {
                "device": "Medium Phone, 6.4in/16cm (Arm)",
                "api": 30,
                "orientation": "portrait",
                "locale": "English",
                "app_version": "2.11.1",
                "version_code": 36
            },
            "scenario": "NORMAL",
            "actions": ["Launched demo mode."],
            "agent_summary": "No urgent issues.",
            "positive_evidence": ["Current glucose is discoverable."],
            "potential_issues": ["May over-rely on in-range status."],
            "linked_observations": [],
            "review_status": "unreviewed"
        }

    def test_valid_product_data_passes(self):
        self.assertEqual(0, self.validate())

    def test_malformed_observation_id_fails(self):
        bad = self.valid_observation(obs_id="BAD ID WITH SPACES")
        self.write_json(self.root / "product" / "research" / "observations" / "obs-bad-id.json", bad)
        self.assertEqual(1, self.validate())

    def test_invalid_enum_fails(self):
        bad = self.valid_observation()
        bad["source_type"] = "bad-enum"
        self.write_json(self.root / "product" / "research" / "observations" / "obs-bad-enum.json", bad)
        self.assertEqual(1, self.validate())

    def test_duplicate_id_fails(self):
        duplicate = {
            "id": "DEC-0001",
            "date": "2026-08-26",
            "subject": "duplicate",
            "status": "ACCEPTED",
            "decision": "duplicate",
            "reason": "duplicate",
            "evidence": ["duplicate"],
            "counterargument": "duplicate",
            "revisit_condition": "duplicate",
            "related_requirements": [],
        }
        self.write_json(self.root / "product" / "decisions" / "dec-duplicate.json", duplicate)
        self.assertEqual(1, self.validate())

    def test_broken_observation_reference_fails(self):
        req = self.valid_requirement()
        req["linked_observations"] = ["OBS-MISSING-123"]
        self.write_json(self.root / "product" / "requirements" / "req-bad-observation-ref.json", req)
        self.assertEqual(1, self.validate())

    def test_broken_requirement_reference_fails(self):
        decision = {
            "id": "DEC-TEST-0001",
            "date": "2026-08-26",
            "subject": "bad requirement reference",
            "status": "ACCEPTED",
            "decision": "reference test",
            "reason": "reference test",
            "evidence": ["reference test"],
            "counterargument": "reference test",
            "revisit_condition": "reference test",
            "related_requirements": ["REQ-MISSING-123"],
        }
        self.write_json(self.root / "product" / "decisions" / "dec-bad-requirement-ref.json", decision)
        self.assertEqual(1, self.validate())

    def test_invalid_current_focus_fails(self):
        current_focus = self.root / "product" / "CURRENT_FOCUS.yaml"
        current_focus.write_text(
            "version: 1\n"
            "phase: phase-1\n"
            "primary_mode: caregiver\n"
            "priority_weights:\n"
            "  safety: 11\n"
            "modes:\n"
            "  - caregiver\n"
            "  - senior\n"
            "human_approval: {}\n"
            "safety_boundary: {}\n"
            "flow:\n"
            "  - OBSERVATION\n",
            encoding="utf-8",
        )
        self.assertEqual(1, self.validate())

    def test_example_records_do_not_count_in_real_summary(self):
        text = self.summary_text()
        self.assertIn("Real backlog", text)
        self.assertIn("Observations: 0", text)   # real OBS removed (phase 2 baseline is clean)
        self.assertIn("Test runs: 3", text)
        self.assertIn("Requirements: 0", text)
        self.assertIn("Decisions: 5", text)
        self.assertIn("Examples", text)
        self.assertIn("observations: 2", text)
        self.assertIn("test-runs: 0", text)
        self.assertIn("requirements: 1", text)
        self.assertIn("decisions: 1", text)

    def test_score_outside_range_fails(self):
        req = self.valid_requirement(req_id="REQ-TEST-0002")
        req["scores"]["safety"] = 11
        self.write_json(self.root / "product" / "requirements" / "req-score-oob.json", req)
        self.assertEqual(1, self.validate())

    def test_score_outside_range_fails(self):
        req = self.valid_requirement(req_id="REQ-TEST-0002")
        req["scores"]["safety"] = 11
        self.write_json(self.root / "product" / "requirements" / "req-score-oob.json", req)
        self.assertEqual(1, self.validate())

    def test_malformed_test_run_id_fails(self):
        bad_run = self.valid_test_run(run_id="BAD ID")
        self.write_json(self.root / "product" / "research" / "test-runs" / "test-bad-id.json", bad_run)
        self.assertEqual(1, self.validate())

    def test_invalid_test_run_enum_fails(self):
        bad_run = self.valid_test_run()
        bad_run["result"] = "UNKNOWN"
        self.write_json(self.root / "product" / "research" / "test-runs" / "test-bad-enum.json", bad_run)
        self.assertEqual(1, self.validate())

    def test_test_run_broken_observation_reference_fails(self):
        bad_run = self.valid_test_run()
        bad_run["linked_observations"] = ["OBS-MISSING-123"]
        self.write_json(self.root / "product" / "research" / "test-runs" / "test-bad-link.json", bad_run)
        self.assertEqual(1, self.validate())

    def test_example_test_run_does_not_count_as_real_backlog(self):
        example = self.valid_test_run(run_id="TESTRUN-2026-999")
        self.write_json(self.root / "product" / "examples" / "test-run-example.json", example)
        text = self.summary_text()
        self.assertIn("Test runs: 3", text)
        self.assertIn("test-runs: 1", text)


if __name__ == "__main__":
    unittest.main()

