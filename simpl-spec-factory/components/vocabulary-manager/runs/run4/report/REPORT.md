# Final report: vocabulary-manager, run 4 (17 Nov 2026)

Project Simpl-Open, component vocabulary-manager, prepared with engine 0cbc4c6 (verified with 608dc73), 383 rules in scope. Written by step 70b from `report/verification.md` (step 70a), the Trivy scans, the suite docs and results, and the build's own docs.

## 1. In short

The code writer built the full component from the specs and the 49 decisions: a Java 21 / Spring Boot 3.5 backend (all 13 OpenAPI paths, 16 operations, plus the decided changes), Apache Jena Fuseki as the store, Keycloak with the realm of E1, and a Vue 3 UI behind nginx. It runs as one Docker Compose stack.

The independent test suite ran 386 scenarios against the running stack. 379 passed and 7 failed (98%). 82 of the 91 live criteria have all their scenarios passing. The 7 failures come down to two questions. Neither is about a core function: creation, versioning, validation, status, search, access control and the UI flows all pass. One failure is a build bug by the letter of D23. The other five are a spec gap: what an invalid name in a path returns.

What it means: the decisions were complete enough for a build that a separate test writer could verify almost entirely without talking to the code writer. The open points are small and are listed as questions for the spec owners in sections 3 and 6. The build is not production ready on supply chain grounds: the scans find 17 CRITICAL findings across the three targets, and there is no CI, TLS or Helm chart (section 5).

## 2. Results

| Measure | Result |
|---|---|
| Scenarios passed | 379 of 386 (98%); 7 failed, 0 skipped |
| Live criteria with a scenario | 84 of 91 (92%) |
| Live criteria with all scenarios passing | 82 of 91 (90%) |
| Decisions with a scenario | 46 of 49 (94%) |
| Decisions with all scenarios passing | 41 of 49 (84%) |
| Rules tested | 16, of which 15 pass fully (API-44: 10 of 14) |
| Accessibility (axe, WCAG 2.1 A and AA) | 0 violations on 4 pages |

### Per feature

| Run | Feature | Passed | Failed |
|---|---|---|---|
| API | access_control | 82 | 0 |
| API | create_version | 24 | 0 |
| API | create_vocabulary | 27 | 4 |
| API | external_vocabularies | 29 | 1 |
| API | health_and_resilience | 1 | 0 |
| API | list_vocabularies | 48 | 0 |
| API | search | 34 | 0 |
| API | status | 20 | 0 |
| API | validation | 35 | 0 |
| API | vocabulary_details | 31 | 0 |
| UI | ui_access | 7 | 0 |
| UI | ui_accessibility | 4 | 0 |
| UI | ui_create | 12 | 0 |
| UI | ui_details | 8 | 0 |
| UI | ui_list | 6 | 2 |
| UI | ui_status | 4 | 0 |
| UI | ui_validation | 7 | 0 |
| | **Total** | **379** | **7** |

API: 331 of 336. UI: 48 of 50.

### Per tag kind

A scenario counts once for each kind of tag it carries.

| Tag kind | Scenarios | Failed |
|---|---|---|
| Decision (D, E) | 382 | 7 |
| Criterion (AC_) | 240 | 6 |
| Negative | 215 | 5 |
| Rule | 142 | 4 |
| UI | 50 | 2 |
| Accessibility (a11y) | 4 | 0 |

## 3. Failures

### 3.1 Invalid name in a path: 400 or 404 (5 scenarios, spec gap)

Scenarios:

- create_vocabulary "An invalid or incomplete create request is rejected", rows @1.2 (name starts lower case), @1.3 (name with a hyphen), @1.4 (name too short), @1.5 (name too long). Tags AC_SIMPL-18170_AC3, D37, D8, D24, API-44.
- external_vocabularies "An incomplete registration is rejected with 400", row @1.4 (name lower case). Tags D41, D37.

What happens: the create or registration itself is correctly rejected with 400, and that step passes. The follow-up step "the vocabulary 'Bad' does not exist" then does GET `/vocabularies/<invalid name>` (or `/external-vocabularies/<invalid name>`) and expects 404. The build answers 400 with a schemaViolation issue: "Path parameter 'vocabularyName' must have 3 to 64 characters and match ^[A-Z][a-zA-Z0-9]*$." So nothing was stored, which is what the step wants to prove, but the check reads the status code.

Cause: no decision says what an invalid path parameter returns. D24 maps "could not be found" to 404. D30 and D41 say GET on an unknown name returns 404, and D41 says every 404 on an external path uses ExternalVocabularyNotFoundResponse. D37 gives the name pattern but only for form fields and the API create. The build applies the D37 pattern to path parameters too (as BLG-req-valid suggests). Both readings are defensible. This also touches D8 (nothing stored), which in fact holds.

Question for the owner: **Does a GET, PATCH or DELETE with a name that breaks the D37 pattern return 400 (schema violation on the path) or 404 (not found)?** Whatever the answer, the suite can prove "nothing was stored" without depending on it, for example through the list with a name filter. That would make these five scenarios test only D37 and D8.

### 3.2 UI sort on the default column (2 scenarios, build bug by the letter of D23)

Scenarios: ui_list "A sortable column sorts ascending, then descending (Name)", rows @1.1 and @1.2. Tags AC_SIMPL-18163_AC2, D23.

What happens: the list opens sorted by Name ascending (the D23 default). The build toggles the current order, so the first click on Name sorts descending and the second ascending. The suite expects ascending, then descending.

Cause: D23 says "first click ascending, second click descending; default Name ascending". Read literally, the first click on Name gives ascending, so the build differs from the decision. The build log also notes the UI sort headers had no automated check of its own. We classify this as a build bug. It is close to a spec gap, though: by the letter, the first click on the default column changes nothing visible.

Question for the owner, to confirm: **Does a click on the column that is already the active sort always start at ascending, or does it reverse the current order?**

### 3.3 Test fix made in step 60 (no failure)

ui_create "The creation form reports invalid input (unsupported file)": the expected text was the server detail "Encountered invalid Turtle syntax". Under D13 the browser syntax check shows the error with its line and keeps Submit disabled, so the server is never called. Step 60 changed the expected text to "line 1", the browser report, as in the ui_validation syntax-error scenario. This was a test bug, fixed. The meaning of the scenario is unchanged.

## 4. Traceability

| Measure | Share of 91 live criteria |
|---|---|
| With a passing scenario (all scenarios pass) | 82 (90%) |
| With a scenario at all | 84 (92%) |
| With code according to `build/work/TRACEABILITY.md` | 85 (93%) |

Criteria with no scenario:

- SIMPL-28680/AC1 to AC6 (Metadata Quality Profile). Out of scope by D1, because the profile belongs to Schema Manager. **No code and no scenario.** These six are the only criteria with nothing. If D1 stands, they should leave the live set for this component.
- SIMPL-18162/AC4 (internal error gives 500 with detail). It has code (`ApiExceptionHandler.unexpected`, `HttpSparqlClient`) and an own unit test (`HttpSparqlClientTest.storeErrorThatIsNotATimeoutGives500`). The suite cannot cause a 500 against a healthy stack. Store outages (503, D44) are covered in health_and_resilience.

Criteria with a scenario that fails: SIMPL-18170/AC3 (4 of 17 fail, section 3.1) and SIMPL-18163/AC2 (2 of 2 fail, section 3.2).

Decisions with no scenario: D2 (CDM snapshot), D45 (pull only, no events) and E3 (store). None of them is observable as behaviour. The pull path D45 relies on is tested under D29 and D38. Decisions with a failing scenario: D8, D24, D37, D41 (section 3.1) and D23 (section 3.2).

## 5. Quality of the build

### Own tests

- 66 backend tests (unit plus MockMvc integration with an in-memory Jena dataset and a local JWKS server), all green in `mvn verify`.
- JaCoCo: **line coverage 94.4%, branch coverage 78.9%**. The gate is 80% on lines.
- 7 Vitest tests on the UI. `smoke.sh` passes 18 of 18. An own Playwright check (`ui/e2e/ui_check.py`) is run by hand.
- Gaps the build itself names: no automated check of the UI sort headers (which is where 3.2 slipped through), of UI filters other than name, or of the failed status update in the UI (SIMPL-18174/AC3).

### Mutation testing and static scan

**Not run.** No mutation score (PIT) and no Semgrep or other static scan exist for this run. The coverage figures above say which code the own tests execute, not how well they check it.

### Deviations from the rules (from `build/work/DEVIATIONS.md`)

All 49 decisions are implemented. Three are read in a specific way: E2 (host ports 8196, 4433, 8193 and issuer from the build profile), E3 (no dataset reset for tests) and D43 (in-process lock, so only one backend instance).

MUST rules not met:

- API-01, DATA-26 to DATA-29, MACH-03: no CDM in the input, and the OpenAPI file may not be edited (D39). The decided changes are in `docs/api-notes.md`, not in a contract file.
- OWT-A04, ARCH-17: no TLS, no encryption at rest (local stack).
- ARCH-07, ARCH-19: one backend (D43) and one Fuseki, no replication.
- UX-01: the Figma design was not reachable, so the UI uses plain components.

MUST rules met in part: OWT-A03, OPS-06, OPS-33, OPS-35, OPS-37 (SBOMs and formatters, but no CI, scan or merge gate); OWT-A07 (no limit on repeated failed calls in the service); OWT-A09, OWASP-10 (logging, no alerting); OWT-A02, OWASP-06 (Keycloak in start-dev, known test passwords); OWT-A08 (base images by tag, not digest); MACH-08 (one health endpoint, no separate liveness); MSV-09 (correlation id not forwarded to Fuseki and Keycloak); CODE-05, CODE-39, CODE-43; ARCH-20 (createdBy shows the e-mail address, as D27 decides). BLG-int-conlang and BLG-int-negot are not applied (English only).

SHOULD rules not followed: API-25, SEC-08 to SEC-10, SEC-15 (TLS); API-46 (belgif problem types where the OpenAPI names them); CODE-21, CODE-50, CODE-52, SEC-11 (logging stack); CODE-33; CODE-45, SEC-12, SEC-13 (no gateway); CODE-46, MSV-07, ARCH-34 (no circuit breaker); MSV-10 (no metrics); MACH-06, ARCH-15, ARCH-16 (no horizontal scaling); OPS-10, OPS-11, OPS-13, OPS-15, OPS-28 to OPS-30 (Compose only, no Helm); CODE-07 (a few 40-line methods).

Most of these come from the scope of a local factory build (no CI, TLS, gateway or Kubernetes), not from choices against a decision.

### Scan results (Trivy, HIGH and CRITICAL only)

| Target | HIGH | CRITICAL | Main sources |
|---|---|---|---|
| Build dependencies (`trivy-fs.json`) | 8 | 7 | tomcat-embed-core 10.1.55 (6), jackson-databind and jackson-core 2.21.4 (5), libthrift 0.22.0 (2), spring-webmvc 6.2.19 (2) |
| Backend image (`trivy-backend.json`) | 10 | 7 | The same Java libraries, plus OS packages such as apt |
| UI image (`trivy-ui.json`) | 65 | 3 | Alpine OS packages of the nginx base: libexpat 2.7.0, libcrypto3 and libssl3 3.3.3, libpng 1.6.47, curl and libcurl 8.12.1 |

The Java findings come from the pinned Spring Boot 3.5.16 dependency set and from Jena's transitive libthrift. The UI findings come almost entirely from an outdated base image, not from the UI code. Both are fixed by version bumps and a newer, digest-pinned base image (OWT-A08). This confirms the OWT-A03 deviation: without a scan in the build, these findings went unnoticed.

### Accessibility

axe-core with the WCAG 2.1 A and AA rules on the vocabulary list, the details page (with a version panel open), the creation form and the new version form: **0 violations** (`tests/work/reports/a11y-*.json`).

## 6. New spec gaps

These are questions for the spec owners that steps 20 to 30 did not raise. They come from the failures and from `build/work/BUILD-LOG.md`.

### Found independently by the test writer (step 40) and the code writer (step 50)

1. **E3 against the build profile.** E3 says tests reset the dataset; the build profile forbids it and asks for unique run ids. Should E3 drop the reset, or should the profile offer one?
2. **E2 against the build profile.** E2's ports (4200, 8180) and issuer URL differ from the profile's. Should E2 fix ports at all, or leave them to the environment?
3. **D11 with D3: a deprecated reference to the latest ACTIVE version of a DEPRECATED vocabulary.** Read literally, the warning says "... version 1 is deprecated. Use version 1, the latest ACTIVE version." What should the text say when the recommended version is the referenced one?
4. **D12, warning S7, for a reference without a version.** D12 gives only "... version <n> does not exist." The build returns "Referenced internal vocabulary '<Name>' does not exist." What is the text when there is no version to name?
5. **API-46 against the belgif problem types.** The OpenAPI and D44 fix `urn:problem-type:belgif:...` for 400, 404, 500 and 503, which API-46 does not allow. Which one wins? (The suite checks API-46 only on 401, 403, 409, 413 and 415.)

### From the failures

6. **Invalid path parameter** (section 3.1): 400 or 404?
7. **Click on the active sort column** (section 3.2): start at ascending, or reverse?

### From the build log only

8. **Order of 409 and 400** on POST /vocabularies: a duplicate name with invalid content. The build checks the name first (409).
9. **Order of 404 and the field checks** (400, 413, 415) on POST .../versions and the PATCH operations. D46 lists them together without an order. The build runs the field checks first.
10. **Unknown form fields and JSON properties.** Only `version` is decided (D7). The build rejects every unknown field with 400 (OWA-API3), also where the schema does not say `additionalProperties: false`.
11. **Query parameters on POST, PATCH and DELETE**, and a parameter given twice. D21 speaks of GET. The build returns 400.
12. **Rule S4 and labels without a language tag.** Do two untagged `skos:prefLabel` literals on one subject count as the same language?
13. **Case of names in paths.** Names are unique ignoring case (D37), but the build looks up paths exactly: `/vocabularies/HEALTH` for a stored `Health` gives 404. Is that intended?
14. **Texts the decisions do not give:** version not found, external namespace conflict, field messages, 405 and unknown path problems.
15. **External vocabularies:** case-insensitive name uniqueness, an alias equal to the own namespace, duplicates, how `aliasUris` is encoded in multipart, `description` length, and whether external names may clash with internal names.
16. **Base IRI for relative IRIs** in uploaded Turtle. The build uses the content URL of the new version.
17. **Time precision.** The build stores and returns dates to the second so that sort, ties and filters agree with what clients see.
18. **Name sort collation.** The build sorts by code point, so `AZ` comes before `Ab`. Is a case-insensitive or locale order wanted?
19. **`prev` link beyond the last page.** The build points it to the last page.
20. **UI texts and steps not fixed:** menu item labels, no confirmation on a status change, date display format, the not-found and 5xx messages, button and field labels. The test writer also had to accept several labels for the status-change action (SIMPL-18174, D4, D18) and for the D35 link back to the list.
21. **Syntax error column in the browser.** N3 reports no column; the build shows the column of the offending token. Does D13 require a column?
22. **SIMPL-28680 in the live criteria** while D1 puts it out of scope: should its six criteria leave this component's set?

## 7. Compared with earlier runs

`RUN.json` names run 1 (27 Oct 2026, `components/vocabulary-manager/generated/run1`). Run 1 passed 74 of 74 API and 22 of 22 UI scenarios of its own suite: 96 of 96.

Run 4 passes 379 of 386 against a suite written independently from the build: 331 of 336 API and 48 of 50 UI. Both runs work against the same component. The two figures are not like for like:

- Run 1's suite was its own, with no separation between code writer and test writer. Run 4 separates them, which is why it finds questions run 1 could not see.
- Run 4's suite is four times larger (386 against 96 scenarios). It adds external vocabularies, search, health and resilience, the D43 concurrency check, and an axe accessibility check on four pages.
- Run 4 traces every scenario to criteria, decisions and rules (84 of 91 criteria and 46 of 49 decisions with a scenario) and adds Trivy scans. Run 1 has no such figures to compare.

The 7 failures in run 4 are not regressions against run 1. They come from questions that run 1's suite did not ask.
