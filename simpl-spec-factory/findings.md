# Findings

Running log of what the factory got right, what needed correction, and what the specs left open. Every run is kept here, including failed ones.

## 2026-10-27: step 1, spec digest

- Input: 35 stories (32 in scope), OpenAPI `7755e1f`, BP02B v155, R07.02 v5. See [`spec-digest.md`](./spec-digest.md).
- 30 decisions recorded (D1 to D30), 6 open questions for the spec owners.
- Most stories have testable Given/When/Then criteria; the API stories and the OpenAPI line up well on the main flows (create, version, status, list, download).
- Largest gaps for buildability: the semantic and structure validation rules (D16, D17) and how a vocabulary references another vocabulary version (D18).
- The OpenAPI defines no 401 or 403 responses, while two stories require them (D4).
- External vocabularies: six operations in the OpenAPI, no story.

## 2026-10-27: step 2, test writer

- 74 API scenarios (runnable) and 22 UI scenarios (Gherkin, browser steps to do), all 32 in-scope stories covered, 21 of the 30 decisions exercised. See [`tests/`](./tests/README.md).
- **Baseline against upstream** (local stack `../simpl-vocabulary-manager`, June 2026 clone of the old `development/` location, no gateway): **58 of 74 pass**. The 16 that fail:

| Group | Scenarios | Reading |
|---|---|---|
| Role checks (`@rbac`) | 8 | Expected: upstream relies on a gateway that is not in the local stack. The service itself does not check tokens or roles. |
| Structure and semantic checks (D16, D17) | 2 | Upstream accepts a file without an ontology or concept-scheme header, and an IRI declared both as class and property. Our minimal rule set is stricter than upstream. Confirms open question 1. |
| Internal references (D17, D18) | 3 | Upstream does not recognise our reference convention, so missing, deprecated or non-existent internal references pass. Confirms open question 2: the mechanism is not in the specs. |
| New version on a deprecated vocabulary (D11) | 1 | Upstream allows it (201). Upstream answers open question 3 differently from our decision. |
| Re-activating an older version (D10) | 1 | Upstream allows a second ACTIVE version of the same vocabulary. Also open question 3. |
| Page size above 100 (D29) | 1 | Upstream has no maximum. Our decision, not a spec requirement. |

- Upstream behaviour worth noting: an **unregistered** external namespace gives a warning (as SIMPL-18183 says); a term missing from a **registered** external vocabulary is an error. The specs do not describe the second rule.
- Test fixes during the baseline (test writer errors, not findings): multipart form without a file, Accept header on downloads, external namespace normalisation (upstream drops the trailing `#`; the OpenAPI mentions normalisation without defining it).

## 2026-10-27: step 3, code writer (run 1)

- A separate agent built backend (Spring Boot 3.5, Java 21, Jena, Fuseki), UI (Vue 3, Vite, TypeScript) and a Keycloak realm from `specs/` and `spec-digest.md` only, in its own workspace. About 30 minutes, four fix iterations. Output: [`generated/run1/`](./generated/run1/), with its own [`TRACEABILITY.md`](./generated/run1/TRACEABILITY.md) and [`BUILD-LOG.md`](./generated/run1/BUILD-LOG.md).
- `docker compose up --wait` works from a clean state. Its own checks: `mvn verify` green (39 unit, 25 integration tests), smoke script 22/22, a headless UI walkthrough 11/11.
- **Black-box suite against the build: 74 of 74 pass** (upstream baseline: 58 of 74).
- **Isolation check:** the agent's tool calls show no file access outside its workspace and no web access.
- **Contamination check:** 9 of 37 class names are shared with upstream, all generic or taken from OpenAPI schema names (`VocabularyController`, `Problem`, `ExternalVocabulary`); 3 of 150 method names (`findAll`, `findVocabulary`, `main`); different package structure. Backend about half the size of upstream (2,900 vs 5,500 lines). No sign that the build reproduces upstream code.

### How to read 74 of 74

- The test writer and the code writer both used the 30 decisions of the spec digest. The result therefore shows that **specs plus a written decision list** are enough to build from. It does not show that the raw specs are: where upstream and the build differ (the 8 non-RBAC failures of the baseline), the difference sits exactly on those decisions.
- The suite covers the API only. The UI scenarios are still Gherkin without steps; the UI was only checked by the code writer's own walkthrough.
- The code writer still found **29 further spec gaps** beyond the digest (in its `BUILD-LOG.md`). Notable: the OpenAPI example treats a reference to a deprecated version as a warning, while SIMPL-18184 makes it an error; the order of error checks (400, 404, 409, 413, 415) is not specified; `VocabularyMetadata` has no creator field although SIMPL-18176 asks for one; no default sort order; the singular path `/external-vocabulary/searches` looks like a typo.

## 2026-10-27: step 4, UI browser steps

- The 22 UI scenarios now run with Playwright (headless Chromium), written by the test writer without seeing either implementation's source. Locators come from the story wording; "API fails" scenarios use request interception in the browser, so the same suite runs on both UIs. Reports: `tests/reports/build1-ui.*`, `tests/reports/upstream-ui.*`.
- **Factory build: 22 of 22.** The first run gave 21: the history lists versions newest first, and the test read "chronological" (SIMPL-18167) as oldest first. Decision (27 Oct): "chronological" allows either direction, and the "+" in "New Vocabulary +" is the usual add convention, possibly an icon, so it is not required. Both are logged as spec wording findings.
- **Local upstream: 9 of 22.** Of the 13 failures:
  - 2 are environment: the local stack bypasses sign-in, so the viewer and "asked to sign in" scenarios cannot pass there.
  - 2 are the known spec gaps D17 (class and property) and D18 (no reference mechanism, so no deprecated-reference warning).
  - 9 differ from the story text: menu labels ("Download" vs "Download vocabulary"), a "DEPRECATED" label without a warning or a link to the active version (SIMPL-18178, 18559), no validation on upload (SIMPL-18180), the update form not pre-filled (SIMPL-18172), no retry advice on error, no "validation passed" message, and the file upload not marked as required on the creation form (SIMPL-18169). With the relaxed "+" the button itself is now accepted. Several of these stories are "ready for implementation check", so upstream may simply not have reached them yet.
- **Story wording too vague for a strict test (spec findings):** "New Vocabulary +" (text or icon?), "clear warning message" (wording and placement), "chronological" (which direction), how to reach the history, the status-change action, confirmation and error texts, "hidden or disabled", how required fields are marked, and how to "select or reference a version" (D18). A Figma reference would settle most of these, but it is not in the input set (D21).
- **Feature text changed by the test writer** (all logged in its report): filter and sort scenarios made robust against a paged, shared list and an unspecified default sort; download-newest no longer repeats the label check; the duplicate-name check split into two submits; a creation notification no story asks for removed; the new-version confirmation must name version 2 (SIMPL-18189 AC3).
- **Seen but not tested:** the build reports one syntax error for a file with two; SIMPL-18180 AC7 asks for all errors.
