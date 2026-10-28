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

## 2026-10-27: step 5, swap with the existing implementation

Can the generated build replace upstream? Tried locally, by configuration only (no code changes on either side). Everything to repeat it is in [`experiments/swap/`](./experiments/swap/README.md).

![Swap test: existing and generated UI and back-end, crossed](./experiments/swap/swap-overview.svg)

| Experiment | Result |
|---|---|
| 1. Upstream's own Postman collection | Same on both: 11 of 11 requests give the same status, 6 of 7 assertions pass on both. The one failure is in the collection itself (it deprecates the vocabulary first, so the version PATCH gets 409 on both). The collection does not test external vocabularies, `/content`, search or health. |
| 2. Existing UI on the new backend | Works. Sign-in through the build's Keycloak by configuration (a runtime client, a top-level `roles` claim, an nginx mapping for the legacy `/auth` prefix). Our UI suite: 13 of 22, against 9 for the existing UI on its own backend. The 9 failures are the existing UI's own wording and features; no failure is caused by the swap. |
| 3. New UI on the existing backend | Works. 21 of 22; the one failure is D17 (upstream accepts an IRI that is both class and property). Note: the viewer scenario passes only because the UI hides the actions; the existing backend would accept a viewer's write, because it relies on the gateway for role checks. |
| 4. Existing data | Not readable as is (0 of 306 vocabularies): different graph names and predicates, two datasets instead of one, values as plain strings, no stored original file text. One SPARQL update plus a small script migrates it: 306 of 306 vocabularies and 412 of 412 versions then match field for field, and downloads and new versions work on migrated data. Most of this data was left behind by our own test runs, so it tests the shape, not real production content. 4 vocabularies have two ACTIVE versions, which the build's D10 rule does not allow; not tested further. |
| 5. Deployment fit | Close but not ready: different container port (8080 vs 8086), UI configuration file and base path, no Fuseki credentials and no dataset creation at boot in the build, images running as root, roles read from a top-level claim only (Tier 1 Keycloak puts them in `realm_access.roles`). |

Field differences found on the way: the build also returns `createdBy` and `latestVersionHref`, which the OpenAPI defines and upstream leaves out; `createdBy` is the username on the build and the email on upstream; the external `namespaceUri` keeps a trailing `/` on the build, upstream strips it ("normalized" is not defined).

### Conclusions

- **The OpenAPI works as a real contract.** Either UI runs on either backend, and upstream's own checks give the same result on both. That is a strong point for the specs and for the upstream team that wrote them.
- **Where the two differ, the specs leave room:** `createdBy`, namespace normalisation, D10 and D17. Same message as before: write the decisions down.
- **What the specs do not cover is what a replacement costs:** the data layout in Fuseki, the identity contract (which claim holds the roles, which one is the creator) and the deployment contract (port, base path, configuration keys, health, credentials). None of these is a user story, but each one decides whether two implementations can be swapped.
- **A replacement is realistic,** with a defined amount of work: a Helm chart, Fuseki credentials, non-root images, the roles claim mapping, and a tested migration job (`migrate.py` is a working start).

## 2026-10-28: step 3b, code writer in Rust (run 2, backend only)

Same input as run 1 (specs and spec digest), backend in Rust instead of Java: axum, oxttl/oxrdf for Turtle, Fuseki and Keycloak as before, no UI. Prompt: [`pipeline/03b-code-writer-rust.md`](./pipeline/03b-code-writer-rust.md). The question: do the specs carry over to another stack?

- **Build:** all 13 OpenAPI paths, 48 own tests green, clippy clean, a 102 MB image that does not run as root. About 23 minutes, five fix iterations.
- **Isolation:** no file access outside its workspace, no web access apart from crates and images.
- **Black-box suite, first pass: 33 of 74.** All 41 failures have one cause. Creating a vocabulary named X with its terms in `{base}/X#` returns 400, "referenced internal vocabulary X does not exist": the build reads the vocabulary's own namespace as a reference to itself. Most scenarios create a vocabulary first, so they fail in setup.
- **The cause is in the digest, not only in the code.** D18 says `{base}/{Name}#` refers to vocabulary Name, and does not say that a vocabulary's own namespace is not a reference. The Java writer filled that in without noticing; the Rust writer read the rule literally. Both readings follow the text. D18 is now clarified in the digest. This is the clearest example so far of why two independent builds find gaps that one build hides.
- **26 further spec gaps** in its build log, some new: no meaning given for the date filters, no rule for which namespaces get a warning (this decides whether a clean file comes back with warnings), whether unknown query parameters are an error, which claim fills `createdBy`, and `POST /external-vocabularies/content` being admin-only if D3 is read literally, although it is a read. It also found a conflict the digest missed: the OpenAPI example treats a reference to a deprecated version as a warning, D17 as an error.
- **Java or Jena in the specs:** the digest itself names Spring Boot, R07.02 asks for "the same versioning library as schema management", the named-graph layout, a semantic error example that reads like Jena output, and the 5 MB upload limit that looks like a Spring default. None of these blocked the Rust build.
- **After the D18 clarification:** the same code writer got the clarified decision only (no test details), changed two files, added two tests, rebuilt. **Black-box suite: 74 of 74**, the same as the Java build. Output: [`generated/run2-rust/`](./generated/run2-rust/).
- **Open point it raised:** under the clarified D18, any version number in a vocabulary's own namespace counts as its own (`{base}/Foo/9#` while creating version 2 of Foo). The clarification does not say whether the number must match.

### Swap: both UIs on the Rust backend

Configuration only, no source change ([`experiments/swap-rust/`](./experiments/swap-rust/)):

| Check | Result |
|---|---|
| Upstream's own Postman collection | Same as on the Java build: 11 of 11 requests give the same status, the same 6 of 7 assertions pass (the one failure is in the collection itself). |
| Generated Vue UI on the Rust backend | 22 of 22 UI scenarios. |
| Existing (upstream) UI on the Rust backend | 13 of 22, the same 9 failures as on the Java build and on its own backend: the existing UI's own wording and features. No failure is caused by the swap. |
| Memory at rest | Rust backend about 11 MiB, Fuseki about 464 MiB, Keycloak about 1 GiB. The Java build used 644 MiB at rest for backend and Fuseki together. |

### Conclusions

- **The specs carry over to another language.** With the same decisions, a Rust backend reaches the same 74 of 74 as the Java one, and both UIs run on it unchanged. The OpenAPI is a real, language-neutral contract.
- **A second, independent build is a cheap spec review.** The Rust writer read one rule more literally than the Java writer and exposed a gap in D18 that 74 green tests on the Java build had hidden. It also listed 26 open points, several of them new.
- **What still ties the specs to one stack** is small and lives outside the API: the named-graph layout in Fuseki, a reference to "the same versioning library as schema management", and the 5 MB upload limit. Worth stating in the specs as requirements, not as library choices.
