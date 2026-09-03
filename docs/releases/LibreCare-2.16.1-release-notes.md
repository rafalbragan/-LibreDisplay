# LibreCare 2.16.1 - Release Notes

**Data wydania / Release date**: 2026-09-02  
**versionCode**: 45  
**versionName**: 2.16.1  
**Poprzednia wersja / Previous version**: 2.16.0 (versionCode 44)

---

## Polski

### Nowe funkcje
- Dodano dodatkowe testy `RelativeTimeFormatter` dla kanonicznych przykładów z `REQ-0002` (`2 min temu`, `5 min temu`) oraz dla uczciwej obsługi nieprawidłowego ISO.
- Dodano ukierunkowane testy Product Inbox weryfikujące polskie nagłówki komentarza analizy produktu, polskie komunikaty decyzji oraz polski fallback komentarza workflow.
- Dodano regresyjne testy bug automation dla wykrywania formularza błędu LibreCare bez etykiety `librecare-bug`, wykluczeń dla Product Inbox i issue implementacyjnych oraz deduplikacji po `reopened`.

### Ulepszenia
- Wspólny formatter świeżości danych pozostał centralnym punktem prezentacji dla kart opiekuna i zyskał testowalną ścieżkę `formatIsoTimeAgo(...)` do bezpiecznej obsługi brakujących i nieprawidłowych danych wejściowych.
- Human-facing komunikacja Product Inbox w GitHub jest spójnie po polsku dla komentarza analizy produktu, wyniku decyzji i fallbacku workflow.
- Zachowano wewnętrzne wartości techniczne bez tłumaczenia: `PRODUCT_PROBLEM`, `PRODUCT_OPPORTUNITY`, `SAFETY_GAP`, `VALIDATED_CAPABILITY`, `TEST_COVERAGE_GAP`, `INCONCLUSIVE`, `ACCEPT`, `HOLD`, `REJECT`, `/accept`, `/hold`, `/reject`, `REQ-*`, `INBOX-*`, `CAND-*`.
- Workflow `librecare-implementation-automation` i CLI automatyzacji rozpoznają kanoniczny formularz błędu po prefiksie tytułu oraz wymaganych sekcjach zgłoszenia, więc intake nie zależy wyłącznie od etykiety GitHub.

### Poprawki
- Naprawiono regresję kompilacji Android CI (BUG-0004), przywracając typy projekcji trendu i przekazywanie okna trendu do karty glikemii.
- Zakres `REQ-0002` jest domknięty: główne karty opiekuna w sekcjach `Wymaga uwagi` i `Pozostali` pokazują naturalną polską świeżość danych zamiast surowego ISO-8601.
- Future clock skew nadal nie pokazuje czasu ujemnego (`przed chwilą`), bez zmiany klasyfikacji stale/fresh, progów ani semantyki medycznej.
- Naprawiono bootstrap bug automation dla zgłoszeń w kształcie Issue #5: poprawny formularz błędu LibreCare jest importowany także wtedy, gdy brakuje etykiety `librecare-bug`.

### Testy
- `python -m unittest scripts.product.tests.test_automation_cli` - PASS
- `python scripts/product/product_cli.py validate` - PASS
- `python -m unittest scripts.product.tests.test_product_cli.ProductCliInboxTest.test_polish_presentation_does_not_change_internal_enums scripts.product.tests.test_product_cli.ProductCliInboxTest.test_issue_review_comment_uses_required_polish_headings_and_commands scripts.product.tests.test_product_cli.ProductCliInboxTest.test_decision_output_is_polish scripts.product.tests.test_product_cli.ProductCliInboxTest.test_non_owner_decision_skip_message_is_polish scripts.product.tests.test_product_cli.ProductInboxWorkflowStaticTest.test_issue_form_has_product_inbox_prefix scripts.product.tests.test_product_cli.ProductInboxWorkflowStaticTest.test_workflow_bootstrap_routing_supports_body_signature scripts.product.tests.test_product_cli.ProductInboxWorkflowStaticTest.test_workflow_has_copilot_handoff_and_pr_tracking scripts.product.tests.test_product_cli.ProductInboxWorkflowStaticTest.test_workflow_decision_fallback_comment_is_polish` - PASS
- `./gradlew clean` - PASS
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS
- Firebase: NIE URUCHAMIANO (zgodnie z zakresem zadania).
- No connected device/emulator available.

### Artefakty
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.16.1-debug.apk` (`23,924,698 B`)
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.16.1-release.apk` (`3,712,135 B`)
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.16.1-release.aab` (`6,733,328 B`)
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\reports\lint-results-debug.html`

### Znane ograniczenia
- Nie uruchamiano Firebase ani testów connected, ponieważ w środowisku nie było dostępnego urządzenia/emulatora.
- Repozytorium zawiera wiele niezależnych, lokalnych zmian w toku; ta aktualizacja dokumentuje wyłącznie zweryfikowany zakres automatyzacji i bieżące artefakty 2.16.1.

---

## English

### New Features
- Added extra `RelativeTimeFormatter` tests for canonical `REQ-0002` examples (`2 min temu`, `5 min temu`) and honest invalid-ISO handling.
- Added focused Product Inbox tests verifying Polish product-review headings, Polish decision output, and the Polish workflow fallback comment.
- Added regression tests for bug automation covering LibreCare bug-form detection without the `librecare-bug` label, exclusions for Product Inbox and implementation issues, and reopened-issue deduplication.

### Improvements
- The shared freshness formatter remains the central presentation point for caregiver cards and now has a testable `formatIsoTimeAgo(...)` path for safe handling of missing and invalid input.
- Human-facing Product Inbox GitHub communication is consistently Polish for the product review comment, decision result, and workflow fallback.
- Internal machine-readable values remain unchanged: `PRODUCT_PROBLEM`, `PRODUCT_OPPORTUNITY`, `SAFETY_GAP`, `VALIDATED_CAPABILITY`, `TEST_COVERAGE_GAP`, `INCONCLUSIVE`, `ACCEPT`, `HOLD`, `REJECT`, `/accept`, `/hold`, `/reject`, `REQ-*`, `INBOX-*`, `CAND-*`.
- The `librecare-implementation-automation` workflow and automation CLI now recognize the canonical LibreCare bug form by title prefix plus required sections, so intake no longer depends only on a GitHub label.

### Fixes
- Fixed the Android CI compilation regression (BUG-0004) by restoring trend projection types and passing the configured trend window to the glucose card.
- `REQ-0002` scope is closed: primary caregiver cards in `Wymaga uwagi` and `Pozostali` show natural Polish freshness text instead of raw ISO-8601.
- Future clock skew still never renders negative time (`przed chwilą`) without changing stale/fresh classification, thresholds, or medical semantics.
- Fixed the bug-automation bootstrap for Issue #5-shaped reports: a valid LibreCare bug form is imported even when the `librecare-bug` label is missing.

### Tests
- `python -m unittest scripts.product.tests.test_automation_cli` - PASS
- `python scripts/product/product_cli.py validate` - PASS
- `python -m unittest scripts.product.tests.test_product_cli.ProductCliInboxTest.test_polish_presentation_does_not_change_internal_enums scripts.product.tests.test_product_cli.ProductCliInboxTest.test_issue_review_comment_uses_required_polish_headings_and_commands scripts.product.tests.test_product_cli.ProductCliInboxTest.test_decision_output_is_polish scripts.product.tests.test_product_cli.ProductCliInboxTest.test_non_owner_decision_skip_message_is_polish scripts.product.tests.test_product_cli.ProductInboxWorkflowStaticTest.test_issue_form_has_product_inbox_prefix scripts.product.tests.test_product_cli.ProductInboxWorkflowStaticTest.test_workflow_bootstrap_routing_supports_body_signature scripts.product.tests.test_product_cli.ProductInboxWorkflowStaticTest.test_workflow_has_copilot_handoff_and_pr_tracking scripts.product.tests.test_product_cli.ProductInboxWorkflowStaticTest.test_workflow_decision_fallback_comment_is_polish` - PASS
- `./gradlew clean` - PASS
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS
- Firebase: NOT RUN (per task scope).
- No connected device/emulator available.

### Artifacts
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.16.1-debug.apk` (`23,924,698 B`)
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.16.1-release.apk` (`3,712,135 B`)
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.16.1-release.aab` (`6,733,328 B`)
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\reports\lint-results-debug.html`

### Known limitations
- Firebase and connected-device tests were not run because no device/emulator was available in this environment.
- The repository contains many unrelated in-progress local modifications; this update documents only the verified automation scope and current 2.16.1 artifacts.
