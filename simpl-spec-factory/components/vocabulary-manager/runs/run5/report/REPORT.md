# Final report: vocabulary-manager, run 5 (18 Nov 2026)

Project Simpl-Open, component vocabulary-manager, engine 18b54b8 (verification with an uncommitted `verify.py` fix that merges the result files), 383 rules in scope. Written by step 70b from `report/verification.md` (step 70a), `report/scan-summary.md` and the raw Trivy files, `tests/work/COVERAGE.md` and its reports, and the build's `TRACEABILITY.md`, `DEVIATIONS.md`, `SECURITY-SCAN.md` and `BUILD-LOG.md`.

## 1. In short

The code writer built the full component from the specs and the 65 decisions (D1 to D61, E1 to E4). The stack has four parts: a Java 21 backend, Apache Jena Fuseki 6.2.0 as the store, Keycloak 26.8.2 with the realm of E1, and a Vue 3 UI behind nginx. It runs as one Docker Compose stack. The first build used Spring Boot 3.5.16. During the run the owner changed the build profile to Spring Boot 4, because the two CRITICAL findings left in spring-webmvc 6.2.19 are only fixed in Spring Framework 7.0.9 and later. The code writer then upgraded the same build to Spring Boot 4.1.2. All suites were run again on the upgraded build and gave the same results as on the first build.

The independent suite ran 468 cases (254 scenarios, outline rows counted) against the running stack. 463 passed, 4 failed and 1 was skipped (`@wip`). That is 99%. API: 406 of 406 run cases pass. Store down: 4 of 4. UI: 53 of 57. 79 of the 85 live criteria have all their scenarios passing.

All four failures are in one UI feature, the creation form. One is a build bug: the browser syntax check accepts a file that the API rejects (D13). Three are a test bug, or at most a spec gap: the suite expects Submit to be disabled while field errors show, and no decision says so (D37, D13).

What it means: all of run 4's 22 questions are now closed by decisions, and both of run 4's failure causes are gone. Every API behaviour the decisions fix passes. The supply chain is clean for everything this project builds: 0 HIGH and 0 CRITICAL in the backend, the dependencies, the UI image and the Fuseki image. Only 6 HIGH remain, all in the third-party Keycloak image. The remaining questions are small and are listed in section 6.

## 2. Results

| Measure | Result |
|---|---|
| Cases passed | 463 of 468 (99%); 4 failed, 1 skipped (`@wip`) |
| API cases | 406 passed of 407 (1 skipped) |
| Store-down cases (`@store-down`) | 4 of 4 |
| UI cases (`@ui`) | 53 of 57 |
| Live criteria with a scenario | 85 of 85 (100%) |
| Live criteria with all scenarios passing | 79 of 85 (93%) |
| Decisions with a scenario | 60 of 65 (92%) |
| Decisions with all scenarios passing | 57 of 65 (88%) |
| Rules tested | 23, all passing |
| Accessibility (axe, WCAG 2.1 A and AA) | 0 violations on 4 pages; one "incomplete" color-contrast result on the list page needs a person to check |

### Per feature

| Run | Feature | Passed | Failed | Skipped |
|---|---|---|---|---|
| API | access_control | 82 | 0 | 0 |
| API | create_vocabulary | 44 | 0 | 0 |
| API | deprecated_references | 7 | 0 | 0 |
| API | errors_and_health (11 API, 4 store-down) | 15 | 0 | 0 |
| API | external_vocabularies | 38 | 0 | 0 |
| API | list | 52 | 0 | 1 |
| API | read_versions | 30 | 0 | 0 |
| API | searches | 41 | 0 | 0 |
| API | status | 32 | 0 | 0 |
| API | validation | 31 | 0 | 0 |
| API | versions | 38 | 0 | 0 |
| UI | ui_accessibility | 4 | 0 | 0 |
| UI | ui_create | 8 | 4 | 0 |
| UI | ui_details | 9 | 0 | 0 |
| UI | ui_list | 14 | 0 | 0 |
| UI | ui_status_and_access | 11 | 0 | 0 |
| UI | ui_versions | 7 | 0 | 0 |
| | **Total** | **463** | **4** | **1** |

### Per tag kind

A case counts once for each kind of tag it carries.

| Tag kind | Cases | Passed | Failed | Skipped |
|---|---|---|---|---|
| Decision (D, E) | 467 | 463 | 4 | 0 |
| Criterion (AC_) | 268 | 263 | 4 | 1 |
| Negative | 294 | 289 | 4 | 1 |
| Rule | 159 | 159 | 0 | 0 |
| UI | 57 | 53 | 4 | 0 |
| Accessibility (a11y) | 4 | 4 | 0 | 0 |
| Store down | 4 | 4 | 0 | 0 |
| Work in progress (wip) | 1 | 0 | 0 | 1 |

### Criteria and decisions

- Criteria with a failing scenario: SIMPL-18169/AC2 (3 of 6 fail), SIMPL-18180/AC1, SIMPL-18180/AC5, SIMPL-18180/AC8 and SIMPL-18193/AC2 (1 each).
- Criterion with only a skipped scenario: SIMPL-18162/AC4 (internal error 500). The suite cannot cause a 500 against a healthy stack.
- Decisions with a failing scenario: D37 (39 of 42 pass), D13 (2 of 3), D60 (10 of 11).
- Decisions with no scenario: D2 (CDM snapshot), D39 (decisions win over the OpenAPI file, no behaviour of its own), D61 (SIMPL-28680 out of the live set), E3 (store technology) and E4 (run environment, applied by the harness itself). None of them is observable as behaviour.
- Rules tested, all passing: API-12, API-36, API-38, API-44, API-46, API-47, ARCH-06, ARCH-32, BLG-col-paging, BLG-err-problem, BLG-err-sanit, BLG-hlth-res, BLG-req-valid, MACH-08, OWA-API3, OWA-API8, OWASP-02, OWASP-08, OWT-A02, OWT-A05, OWT-A07, OWT-A10, SEC-05.

## 3. Failures

### 3.1 Browser syntax check passes a broken file (1 case, build bug)

Scenario: ui_create "Choosing a file with a syntax error shows its line and blocks submit" (`ui_create.feature:75`). Tags AC_SIMPL-18180_AC1, AC_SIMPL-18180_AC5, AC_SIMPL-18180_AC8, AC_SIMPL-18193_AC2, D13, D60.

What happens: the fixture `syntax-error.ttl` has a term after a literal with no `;` or `,` on line 6 (`skos:prefLabel "Broken"@en ex:Oops .`). The API rejects this file with 400. The UI shows "Syntax check passed." and keeps Submit enabled. The suite expects "line 6" and a disabled Submit.

Cause: `ui/src/lib/validation.ts` creates the N3.js `Parser` without a `format` option. N3.js then parses its lenient N3 superset and accepts the line. With `format: 'text/turtle'` the same file fails with "Expected punctuation to follow ""Broken"@en" on line 6." (checked with the build's own `node_modules/n3`). D13 requires a Turtle check that shows syntax errors with their line. The build differs from D13, so this is a build bug. The fix is one option on the parser.

### 3.2 Submit stays enabled while field errors show (3 cases, test bug; owner may confirm)

Scenario outline: ui_create "Field errors are shown under the field before submit", rows @1.1 (name in lower case), @1.2 (name too short), @1.3 (description missing) (`ui_create.feature:41` to `43`). Tags AC_SIMPL-18169_AC2, D37.

What happens: the step "the 'Submit' button is disabled" fails. The button stays enabled. The step that checks nothing is stored is not reached.

Cause: D37 says "the UI shows field errors under the field before submit". It does not say Submit is disabled. D13 disables Submit only "while the upload check shows errors", which is about the file, not the name or description. The build shows field errors and keeps Submit enabled. That meets both decisions as written. The suite reads more into D37 than it says, so this is a test bug. The suite should check the field error text under the field instead. If the owner wants Submit disabled with field errors, that is a new decision (question R-023 in section 6).

### 3.3 Skipped: internal error 500 (not a failure)

list "An internal error gives 500 with details and no metadata" (`list.feature:67`, SIMPL-18162/AC4) is `@wip`. The build has code for it (`ApiExceptionHandler`) and a unit test (`ApiExceptionHandlerTest`). D44 covers the store-down case with 503, and the 4 store-down cases pass.

### 3.4 Test fixes made in step 60 (no failure)

The UI test writer fixed four test bugs before the final run: a missing wait for downloads, a form that was checked before its pre-fill arrived, a timing race in the "Last updated" sort example (status change in the same second as the creation, while D58 keeps times to the second), and an error step that now accepts role alert or status (no decision fixes the role). None of them changes what a scenario checks.

## 4. Traceability

| Measure | Share of 85 live criteria |
|---|---|
| With a passing scenario (all scenarios pass) | 79 (93%) |
| With a scenario at all | 85 (100%); 84 with a runnable one (SIMPL-18162/AC4 is `@wip`) |
| With code according to `build/work/TRACEABILITY.md` | 85 (100%) |

No live criterion is without both code and a scenario. SIMPL-18162/AC4 is the only criterion without a run scenario; it has code and an own unit test. SIMPL-28680 is out of the live set (D61), so its six criteria no longer count. SIMPL-393 is met by the generic upload (D1, one scenario tagged D1) and SIMPL-395 is replaced by the SIMPL-181xx stories.

Criteria with a failing scenario: SIMPL-18169/AC2 (section 3.2), and SIMPL-18180/AC1, SIMPL-18180/AC5, SIMPL-18180/AC8 and SIMPL-18193/AC2 (section 3.1). Once the parser format and the suite's field error step are fixed, all 84 runnable criteria are expected to pass.

## 5. Quality of the build

### Own tests

- Backend: 69 tests (unit tests, integration tests with real Tomcat and embedded Fuseki, and a store-down test), all green in `mvn verify`, with Spotless.
- JaCoCo on Spring Boot 4.1.2: **line coverage 91.5%, branch coverage 78.2%** (gate 75% on lines). On Spring Boot 3 it was 90.7% and 77.1%.
- UI: 19 Vitest tests and a Prettier check. `smoke.sh` passes 19 checks. An own Playwright check (`ui/e2e/ui_smoke.py`) is run by hand.
- The upgrade to Spring Boot 4 needed 3 fix iterations (Jackson 3 packages, `ErrorController` package, a Jackson setting that no longer exists). All 69 tests passed after that without changing an assertion.
- The own UI tests did not catch the parser format of section 3.1. The Vitest tests cover the validation helpers, but no test feeds them a file that is valid N3 and invalid Turtle.

### Mutation testing

**Not run.** There is no mutation score (PIT) for this run. The coverage figures say which code the own tests execute, not how well they check it.

### Deviations from the rules (from `build/work/DEVIATIONS.md`)

All decisions D1 to D61 and E1 to E4 are implemented. Points of interpretation are in the build log (section 6).

MUST rules not met or met in part:

- API-01, DATA-26 to DATA-29: no CDM in the input, and D39 says the OpenAPI file is not edited. The differences are in `docs/API.md`.
- OWT-A04, OWASP-03, OWA-API8: plain HTTP on localhost, no encryption at rest (local profile).
- OWASP-06, SEC-02: Keycloak in `start-dev`; the bootstrap admin password is in `.env`.
- OWASP-02, MSV-02 for the store: Fuseki has no authentication. It has no host port and is reachable only on the compose network.
- ARCH-07: one backend instance with an in-memory lock (allowed by D43).
- MSV-09 (partly): correlation id on every log line and sent to Fuseki, but not on Keycloak key fetches.
- OPS-06, OPS-33, OPS-35, OPS-37, OWASP-12, CODE-13: no CI pipeline; formatters and Trivy run by hand.
- CODE-39 (partly): `@throws` documented on the main public methods only.
- CODE-04, CODE-03: no ESLint for the Vue code, only Prettier.
- OWT-A07 (partly): Keycloak brute force protection is on; no call-rate limit in the API (D46 puts it at the gateway).
- OWT-A09, OWASP-10: logging, no alerting.
- OWASP-07: 6 HIGH open in the Keycloak vendor image, with reasons.
- BLG-int-conlang, BLG-int-negot: English only.
- UX-01: no Figma design in the input; plain own styling.
- SEC-01, OWASP-05: a short threat model in `docs/ARCHITECTURE.md`, no separate document.
- API-39: no `Vary: Accept`.

SHOULD rules not followed: API-46 for 400, 404, 500 and 503 (D48 keeps the belgif types, a recorded deviation), API-03, API-21 (D39), API-25, MACH-06, MSV-07, MSV-10, CODE-21, CODE-33, CODE-54, UX-02, API-50, API-52, API-54.

Not applicable: API-62 to API-80, BLG-async-*, MSV-04, MSV-08 and the CloudEvents rules (D45, no events); OPS rules on Helm, Argo CD and Kubernetes (Compose profile); BLG-lng-task.

Most of these come from the scope of a local factory build (no CI, TLS, gateway or Kubernetes). Two points from run 4 are now met: base images are pinned by digest (OWT-A08), and log lines carry the token subject instead of the creator's e-mail (CODE-32).

### Scan results (Trivy 0.76.2, HIGH and CRITICAL only, orchestrator's own scan)

| Target | Boot 3.5.16 build (CRITICAL / HIGH) | Boot 4.1.2 build (CRITICAL / HIGH) | What `SECURITY-SCAN.md` says |
|---|---|---|---|
| backend image | 2 / 0 | 0 / 0 | spring-webmvc CVE-2026-47884 and CVE-2026-47890 fixed by the upgrade (Spring Framework 7.0.10). The Boot 3 overrides for Tomcat and Jackson are no longer needed: Boot 4.1.2 brings Tomcat 11.0.27 and Jackson 3.1.7. |
| deps (`fs`) | 2 / 0 | 0 / 0 | Same as backend. |
| fuseki image | 0 / 0 | 0 / 0 | Nothing open. Trivy may not see libraries shaded without Maven metadata. |
| keycloak image | 0 / 6 | 0 / 6 | Accepted: CVE-2026-91403 (OpenJDK, Red Hat will not fix), CVE-2026-119373, CVE-2026-119374, CVE-2026-119379, CVE-2026-119586 (sqlite-libs, no fixed package, not used by Keycloak), CVE-2026-102554 (guava, fixed only in a later Keycloak release). |
| ui image | 0 / 0 | 0 / 0 | Nothing open. |

The orchestrator's counts match the build's own scan for every target. Nothing is open in code or images built by this project. Before any version override, the Boot 3 build had 7 CRITICAL and 6 HIGH in the backend; the build's own overrides brought that to 2 CRITICAL, and the upgrade to 0. For context: the upstream Simpl vocabulary-manager-be (local checkout v1.1.0, 29 Sep 2026) uses Spring Boot 4.1.0, which carries the same two spring-webmvc CVEs plus five CRITICAL findings in tomcat-embed-core 11.0.22.

### Accessibility

axe-core with WCAG 2.1 A and AA on the list, the details page, the creation form and the new version form: **0 violations**. The color-contrast check returned "incomplete" on the list page, which axe cannot decide by itself; a person should check it.

## 6. New spec gaps

Questions for the owners that steps 20 to 30 did not raise. Suggested ids continue after run 4's R-022.

### From the failures

- **R-023. Submit with field errors (D37, D13).** Must the create and new version forms disable Submit while a field error shows (name, description, changelog)? Or do they only show the error and let the server reject the request? (Section 3.2.)

### Found by both the test writer (step 40) and the code writer (step 50)

- **R-024. 406 Not Acceptable.** The OpenAPI lists 406 on every operation, but no decision places it in the check order of D51 or fixes its body. The build checks `Accept` right after authentication, before 415. The test writer dropped an API-37 scenario for 406 because nothing fixes it.
- **R-025. Version panel link for a DEPRECATED vocabulary (D36 against D47).** D36 shows "Latest ACTIVE version: n" when a version has status ACTIVE. D47 recommends no version when the vocabulary is DEPRECATED, because D3 makes all its versions effectively deprecated. Should the UI also show no link in that case? May a version ever link to itself? The build follows D36 in the UI and D47 in the API, and never links a version to itself.

### From the test writer (step 40)

- **R-026. Location of warning externalReferenceSkipped.** Its location text is fixed only in the glossary. Should a decision fix it?
- **R-027. Location of rules S1 to S5.** Fixed only as "the offending statement or resource as in the OpenAPI example". What is the exact location text per rule?
- **R-028. 404 detail for internal vocabularies.** The text is fixed only by an OpenAPI example. Should a decision fix it, as D55 does for the version text?
- **R-029. '<uri>' in the D55 conflict text.** Is it the URI as the client gave it, or after normalisation (for example without a trailing '#' or '/')?

### From the build log only

- **R-030. 404 of POST /external-vocabularies/content.** D41 fixes "External vocabulary with name '<Name>' was not found." with an issue `in: path`, but this operation has no name in the path. The build uses the OpenAPI example text "External vocabulary with namespaceUri '<uri>' was not found." without an issue. Which is right?
- **R-031. S4 location with duplicates in two languages.** One subject with duplicate labels in two languages gives two issues with the same detail and the same location `resource '<s>'`. Should the location or detail name the language?
- **R-032. Unknown path without a token.** The build checks the token for every path under `/v1` before routing, so an unknown path gives 401 without a token and 404 with one. Is that intended?
- **R-033. Issue names in search requests (D38).** For an invalid item the build uses `items[i].name` and `items[i].version`; for a body that matches no or several `oneOf` branches it uses `name: body`; when `namespaceUris` plus `aliasUris` exceed 100 it names `aliasUris`. Should these be fixed?
- **R-034. Internal reference shapes (D12).** A namespace `<contentBase>/content/Name/versions/abc` is read as a reference to the latest version of `Name`. A namespace `<contentBase>/content/` with no name is treated as external. Confirm both.
- **R-035. Content-Type of downloads.** The build returns exactly `text/turtle` without a charset and the bytes as uploaded. Confirm.
- **R-036. Empty file.** A `vocabularyFile` part with zero bytes passes the field check and fails validation with S6 (400), not with a "required" field error. Confirm.
- **R-037. Several URIs in one aliasUris part (D56).** D56 says one part per URI. A single part with comma-separated URIs is read as one invalid URI. Confirm.
- **R-038. Token issuer and audience (E1).** E1 names `vocabulary-manager-be` as the audience but not how tokens get it. The build adds an audience mapper to `vocabulary-manager-ui` and `vocabulary-tests` and rejects tokens without it (401). It also accepts only the fixed issuer `http://localhost:<KEYCLOAK_PORT>/realms/simpl`. Should E1 fix both?
- **R-039. Lock wait limit (D44, D43).** The build waits at most 10 s for the per-vocabulary lock (the store response timeout). Should D44 give its own number?
- **R-040. UI filter and menu labels (D23).** The build uses "Name contains", "Vocabulary status" and "Created on", and the actions "View vocabulary details", "History", "Download vocabulary", "Update", "Deprecate" or "Activate". Confirm or supply the Figma labels.
- **R-041. Page after a successful creation (D14).** The build opens the details page of the new vocabulary and shows the notification and warnings there. Confirm.
- **R-042. UI texts for a failed status change (D60).** A 404 shows "Vocabulary not found." plus a sentence that no change was applied; a 500 shows "The vocabulary status could not be updated." plus the D60 text and the detail. Neither text is fixed. Confirm or supply.

### Still open from earlier steps (not new)

- D11 against SIMPL-18184/AC2: D11 makes a deprecated reference a warning, AC2 makes it an error. The test writer flagged it again. This is D11's existing owner question; SIMPL-18184/AC2 still needs rewording.
- All decisions are still `provisional`; their owner questions stand.

## 7. Compared with earlier runs

`RUN.json` names run 4 (17 Nov 2026) and run 1 (27 Oct 2026).

| Measure | Run 4 | Run 5 |
|---|---|---|
| Decisions | 49 | 65 (D47 to D61 and E4 added) |
| Live criteria | 91 | 85 (SIMPL-28680 left by D61) |
| Cases passed | 379 of 386 (98%) | 463 of 468 (99%), 1 skipped |
| API / UI | 331 of 336 / 48 of 50 | 406 of 406 run / 53 of 57 |
| Criteria with a scenario | 84 of 91 | 85 of 85 |
| Criteria with all scenarios passing | 82 of 91 | 79 of 85 |
| Decisions with all scenarios passing | 41 of 49 | 57 of 65 |
| Rules tested | 16 (15 fully passing) | 23 (all passing) |
| JaCoCo line / branch | 94.4% / 78.9% (gate 80%) | 91.5% / 78.2% (gate 75%) |
| Trivy CRITICAL in own targets | 17 | 0 |
| Trivy HIGH in own targets | 83 | 0 (6 in the Keycloak vendor image, which run 4 did not scan) |

**Run 4 failures, now resolved.**

- Invalid name in a path, 5 cases (run 4 section 3.1, R-006): D49 decides 400 with one schemaViolation issue. The build kept its behaviour and the suite now proves "nothing stored" without a GET on the invalid name. D49: 14 of 14 pass.
- Click on the active sort column, 2 cases (run 4 section 3.2, R-007): D50 decides that a click on the active column reverses it, which is what the run 4 build did. D50: 4 of 4 pass.

**Run 4 questions, now closed.** All 22 are closed by decisions: E4 (R-001, R-002), D47 (R-003, R-004), D48 (R-005), D49 (R-006), D50 (R-007), D51 (R-008, R-009), D52 (R-010, R-011), D53 (R-012), D54 (R-013), D55 (R-014), D56 (R-015), D57 (R-016), D58 (R-017, R-018), D59 (R-019), D60 (R-020, R-021), D61 (R-022). Their scenarios all pass, except one D60 case that fails on the D13 parser bug (section 3.1). D61 and E4 have no scenario because they add no behaviour. Two new questions touch closed ones: R-025 (the D36 UI link next to D47) and R-029 (the '<uri>' of D55).

**Supply chain.** Run 4 had 17 CRITICAL, mostly from Spring Boot 3.5.16 defaults (Tomcat, Jackson, spring-webmvc), Jena's libthrift and an outdated nginx base image. Run 5 has none in anything it builds, after current base images pinned by digest, `apk upgrade`, Fuseki 6.2.0 and the move to Spring Boot 4.1.2.

**Run 1** passed 74 of 74 API and 22 of 22 UI scenarios of its own suite, written without separating code writer and test writer. Run 5's suite is independent and about five times larger, so the figures are not like for like. Run 5's four failures are not regressions against run 1 or run 4. They come from a UI check that earlier suites did not make in this form.
