# Final report: vocabulary-manager, run 11 (20 Nov 2026)

Project Simpl-Open, component vocabulary-manager, engine 48020bf, 405 rules in scope. Compose project `vm-factory-run11`. The run went from 13:25 to 15:39. Agent cost before this report was about USD 53: tests USD 9.77, code USD 25.77, suite review USD 4.16, review fixes USD 10.68, design review USD 2.99.

Sources: `RUN.json`; `report/` (`verification.md` of 70a, `scan-summary.md`, `load.json`, `fuzz-summary.md`, `fuzz.json`, `fuzz/console.txt`, `design-review.md` of 70c); `tests/work/COVERAGE.md` and the behave reports in `tests/work/reports/`; `tests/review/SUITE-REVIEW.md`; and from the build `TRACEABILITY.md`, `DEVIATIONS.md`, `REUSE.md`, `QUALITY.md`, `SECURITY-SCAN.md` and `BUILD-LOG.md`. I also read four other things:

- `logs/ui_60.md` and `tests/work/run.sh`, to explain why the API suite did not run;
- the D18, D21, D51, D67, D72, D122 and D138 entries of `decisions.json`;
- the list error handler in `ui/src/views/VocabularyList.vue`;
- the run 10 report, for the comparison.

## 1. In short

**What was built.** The build has four parts:

- a Java 21 backend on Spring Boot 4.1.3, run as two replicas (D102), using Jena 6.2.0, Nimbus 10.10 and Resilience4j 2.4.0;
- Apache Jena Fuseki 6.2.0 as the store;
- Keycloak 26.8.2 with the realms of E1 and E7;
- a Vue 3 UI served by nginx, which also proxies the API.

All 19 operations of the contract are built, with the decided changes. The code writer reached the finish line:

- the stack starts from a clean state;
- `smoke.sh` passes 13 of 13 checks;
- `mvn verify` is green: 49 tests, line coverage 93.3 %, branch coverage 82.3 %;
- the build's Trivy findings are fixed, or listed with a reason;
- the stack was left running.

**How it scored.** Verification counts **71 of 549 scenarios passed, 1 failed, 477 skipped.** The 477 skipped scenarios are the whole default API suite, so the low figure says nothing about the API. The API suite never ran, because of two defects in the test harness:

- `run.sh` used the tag syntax `not @tag`, which behave 1.2.6 does not understand, so every scenario was filtered out.
- `support/vm.py` passed `timeout` to `requests.Request`, so every API call would have crashed anyway.

The UI step (step 60) found and fixed both, as `logs/ui_60.md` records. The orchestrator did not run the API suite again after that. The step 45 dry run did not catch the problem because the `dry-run` mode already used the correct `~@tag` syntax.

**What was measured:**

| Part | Result |
|---|---|
| UI scenarios | 61 of 62 pass. The one failure is a build bug against D18. |
| Accessibility (Q6) | axe found 0 WCAG 2.1 AA violations on all four pages. |
| Store-down scenarios | 10 of 10 pass. |
| Load (Q1) | Met in full, at the full D99 seed of 1000 vocabularies with 20 versions each. |
| Fuzzing | 0 server errors. 3 findings, of which 2 are one build bug and 1 is a missing fuzz exception. |
| Trivy, own images and dependencies | 0 HIGH, 0 CRITICAL |
| Trivy, vendor Keycloak image | 6 HIGH, 0 CRITICAL, all given a reason |

**What it means.** On the evidence we have, the build looks sound: UI, store-down, load, fuzz and scans are all good. We cannot say the API meets its 85 criteria until `./run.sh api` is run against the stack that is still running, and `verify.py` is run again. The same kind of defect happened in run 10: the API results were lost there too, and only a manual rerun recovered them. This is now a repeated factory defect. It is not a build finding and not a spec finding.

## 2. Results

### Per feature

| Feature | Kind | Passed | Failed | Not run |
|---|---|---|---|---|
| Vocabulary list in the UI | ui | 18 | 0 | 0 |
| Vocabulary details and version history in the UI | ui | 13 | 0 | 0 |
| Create, new version and validation feedback in the UI | ui | 20 | 0 | 0 |
| Status change and access control in the UI | ui | 10 | 1 | 0 |
| Behaviour when the vocabulary store is not reachable | store-down | 10 | 0 | 0 |
| Access control on the Vocabulary Management API | api | 0 | 0 | 93 |
| List, filter, sort and page vocabularies through the API | api | 0 | 0 | 63 |
| Register, read and remove external vocabularies | api | 0 | 0 | 50 |
| Read a vocabulary, its version history and one version through the API | api | 0 | 0 | 37 |
| Search vocabularies and external vocabularies | api | 0 | 0 | 37 |
| HTTP contract rules that apply to every operation | api | 0 | 0 | 36 |
| Create a vocabulary through the API | api | 0 | 0 | 30 |
| Change the status of a vocabulary or of one version through the API | api | 0 | 0 | 27 |
| Create a new version of a vocabulary through the API | api | 0 | 0 | 26 |
| Syntax and semantic validation of uploaded vocabulary content | api | 0 | 0 | 24 |
| Limits of the component | api | 0 | 0 | 22 |
| Vocabulary references in uploaded content | api | 0 | 0 | 18 |
| Download vocabulary content through the API | api | 0 | 0 | 14 |
| **Total** | | **71** | **1** | **477** |

### Per tag kind

| Tag kind | Scenarios | Passed | Failed | Not run |
|---|---|---|---|---|
| `@ui` | 62 | 61 | 1 | 0 |
| of which `@a11y` (Q6) | 4 | 4 | 0 | 0 |
| `@store_down` | 10 | 10 | 0 | 0 |
| default API, `@negative` | 318 | 0 | 0 | 318 |
| default API, positive | 159 | 0 | 0 | 159 |

The 10 store-down scenarios are also tagged `@negative`.

### Criteria (85 live)

- **84 of 85** have a scenario. The one without is SIMPL-18162/AC4.
- **44 of 85** have at least one passing scenario.
- **35 of 85** have all their scenarios passing. This is the 70a figure.
- **1** has a failure: SIMPL-18195/AC2, with 4 passed and 1 failed.
- **40** are tested only by API scenarios that did not run. Examples are SIMPL-18162/AC1 to AC3, SIMPL-18164/AC1 to AC4, SIMPL-18170, SIMPL-18173, SIMPL-18175, SIMPL-18181, SIMPL-18183, SIMPL-18194 and SIMPL-18196/AC1 to AC4.

Eight criteria pass in the UI but still wait on API scenarios: SIMPL-18169/AC2, SIMPL-18176/AC1, SIMPL-18182/AC1 to AC3, SIMPL-18186/AC1, SIMPL-18189/AC1 and SIMPL-18191/AC1.

### Decisions (154)

- **118 of 154** have a scenario.
- **23 of 154** have all their scenarios passing.
- **1** has a failure: D18, with 7 passed and 1 failed.

The decisions that pass in full are the UI and store-down ones: D13, D32, D34, D35, D36, D50, D62, D64, D72, D74, D75, D85, D90, D100, D101, D109, D113, D114, D115, D118, D123, D141 and Q6. The 36 decisions without a scenario are listed in `COVERAGE.md`, each with a reason. Examples are D2, D80, D83, D88, D92, D93 to D99, D102 to D107, Q3, Q4 and Q5.

### Rules

25 rules are tagged on scenarios. Five have a passing scenario: ARCH-06, BLG-hlth-res, MACH-08, NFR-07 and NFR-10. The other 20 are tested only through the API suite and were not measured, for example OWASP-02, SEC-05, API-46 and BLG-err-problem.

## 3. Failures

### 3.1 Failing scenario

**`ui_status_access.feature:83` "A user without a vocabulary role sees the authorisation message on the list"** (SIMPL-18195/AC2, D18): **build bug.**

- For ga-norole, the list shows "The vocabulary list could not be loaded." followed by the 403 problem detail.
- D18 says: "Any 403 from the API shows the same message", meaning "You are not authorized to perform this action."
- D72 fixes the list load error text only for 500 and 503, so the two decisions do not conflict.
- In `VocabularyList.vue`, `load()` sends every error to `TEXT.listLoadFailed`. Only `statusFailure()` maps 403 to `TEXT.notAuthorized`.
- Step 45 did not flag this scenario, and the test writer kept it as written.
- Fix: map 403 to `TEXT.notAuthorized` in `load()`, and in the history load as well.

### 3.2 Scenarios not run (factory defect)

The 477 default API scenarios are not failures of the build or of the suite's reading of the decisions. They come from two harness defects, both now fixed in `tests/work`:

- the `not @tag` filter in `run.sh`;
- the `timeout` argument in `support/vm.py`.

The suite step ran at 14:35 and took 1 second. `reports/api.json` has the time 15:12, which is inside the UI step, and it is still all skipped. The logs do not say what rewrote it.

### 3.3 Fuzz findings (`fuzz-summary.md`)

There were 20 operations and 17 failed checks. 14 are explained by D9, D19, D20, D22, D37, D49 and D52. There were 0 server errors and 3 findings:

1. **ST105, Undocumented Content-Type, 400 `text/html`** (twice, stateful phase): **build bug.**
   - Requests: `GET /vocabularies` and `GET /external-vocabularies`, each with a very long query string. It repeats `pageSize` and `page` many times with odd percent-encoded values.
   - The answer is the servlet container's default HTML error page, not problem+json, so the request was refused by the backend's HTTP server before the service's own filters ran.
   - D138 says a repeated query parameter gives 400 with exactly one issue named after the parameter. D21 says invalid values give 400 InputValidationProblem. D48 and D122 aim at one error format for the whole component.
   - Fix: replace the container's error report with problem+json, or raise the container's parameter and decoding limits so that the service's `QueryParameters` sees the request.
   - The build log says the proxy's own errors were moved to problem+json, but this path was not covered.
   - A residual spec question is in section 6, item 17.
2. **ST201, Schema-violating request accepted, 413**: **a decision the fuzz list misses.**
   - The request body broke the schema (an array where an object is expected), but it was also over the size limit.
   - D51 puts 413 (size) before 400 (request shape), so 413 is the decided answer.
   - Fix: add D51 to `fuzz-known.yaml` for ST201 with status 413.

## 4. Traceability

| Measure | Share |
|---|---|
| Live criteria with a scenario | 84 of 85 (99 %) |
| Live criteria with at least one passing scenario | 44 of 85 (52 %) |
| Live criteria with all scenarios passing | 35 of 85 (41 %) |
| Live criteria with code, per `TRACEABILITY.md` | 85 of 85 (100 %) |

**Criteria with nothing:** none. **SIMPL-18162/AC4** (internal error 500) comes closest:

- it has no scenario, because a 500 cannot be provoked from outside (D44);
- it has code, `GlobalExceptionHandler.unexpected` and `SparqlClient.guarded`;
- no own test forces it.

Two criteria are covered only by "code review" in the build's own tests: SIMPL-18172/AC3 and SIMPL-18174/AC3. Both have passing UI scenarios.

Out of scope as decided: SIMPL-28680 (D61), SIMPL-393 and SIMPL-395 (D1).

## 5. Quality of the build

### Quality targets

| Target | Target value | Measured | Verdict |
|---|---|---|---|
| Q1 Response time and throughput | read p95 ≤ 300 ms, write p95 ≤ 3 s, 5 MB write ≤ 30 s, ≥ 95 % of 20 reads/s and 1 write/s, no errors | Step 70 (`load.json`), D99 seed 1000 × 20 plus 10 externals: worst read p95 230 ms (`GET /vocabularies`, the others 34 to 52 ms); write p95 630 ms (`POST /vocabularies`) and 600 ms (new version); 5 MB writes 5.6 to 5.7 s; 20.04 reads/s and 1.0 writes/s achieved; 0 errors in 300 s | **Met** |
| Q2 Limits (NFR-04) | the D15, D37, D38, D76, D110 and D125 limits | Black-box: the 18 `limits` and 8 `search` limit scenarios did not run. Build's own tests: `RequestRulesIT`, `ContentRulesTest`, `UploadLimiterTest`, and a probe of 6.2 MB and 8 MB uploads | **Not measured this run** (own tests only) |
| Q3 Availability and recovery | 99.5 % in office hours, RTO 30 min, RPO 24 h | Backup and restore round trip PASS on 68 quads. The restore was not timed. Availability cannot be measured without production (NFR-13) | **Partly** |
| Q4 Design load and scaling | 1000 × 20 seed, 20 read users, replicas | The seed loaded in full (483 s) and was used for Q1. Two replicas; 4 parallel new versions gave versions 2 to 5 once each. Four parallel 5 MB uploads were not measured | **Met as a load condition**; scaling not measured |
| Q5 Maintainability | coverage gate, Trivy, bounded queries | Line 93.3 %, branch 82.3 % (the gate is 80/60); Trivy 0/0 on own targets; LIMIT on list queries, but `references()` has none (D85 allows this) | **Met** |
| Q6 Accessibility | WCAG 2.1 AA | axe found 0 violations on list, details, create form and new version form; keyboard reachability passed | **Met** |

### Reuse

`REUSE.md` checks the catalogue first and shows broad reuse:

- Nimbus for token checks (REUSE-01);
- Jena RIOT, ARQ `ParameterizedSparqlString` and the RFC 3986 parser;
- Resilience4j for the circuit breaker, Tomcat for multipart, Jackson for JSON;
- Micrometer, Keycloak and nginx.

Own code is explained where it exists: `RealmKeys` (D88, D129), `SparqlClient`, `SemanticRules`, `QueryParameters` and `ProblemWriter`.

70c verdicts on the REUSE rules:

| Rule | Verdict | Note |
|---|---|---|
| REUSE-01, 03, 04, 05, 06, 09 | met | |
| REUSE-02 | partly | hand-written query string split, links built by concatenation, Basic auth header, exception-name matching in `MultipartForm.translate` |
| REUSE-07 | partly | about 750 lines of own HTTP plumbing (`ApiRoutes`, `NegotiationFilter`, `MethodGuardFilter`, `Representations`) have no D93 row |

### Main 70c findings

70c reviewed 153 rules: 85 met, 37 partly, 14 not met, 17 not applicable. Only one MUST is not met, CODE-11, and D135 overrides it. The main findings:

1. **OWT-A07 (MUST):** repeated token failures are never limited, and `DEVIATIONS.md` does not list this.
2. **SEC-01 and OWASP-05 (MUST):** there is no threat model or security section in `docs/`.
3. **ARCH-20 and OWT-A04:** `createdBy` holds the user's email. It is stored unencrypted and shown to every viewer.
4. **REUSE-07 and D93:** missing rows, as above.
5. **NFR-06:** the restore was not timed against the recovery time objective.
6. **Possible TRACE echo inside the network:** Tomcat has `setAllowTrace(true)`. Plausible, not proven.

### Disagreements between 70c and the build

70c finds gaps the build does not report:

- OWT-A07;
- OWT-A09 and OWASP-10 (no alert path);
- SEC-01 and OWASP-05;
- ARCH-20;
- REUSE-07;
- NFR-06 (Q3 presented as covered by design);
- DATA-26 and DATA-29 (CDM drift is not in `DEVIATIONS.md`).

70c is more lenient than the build on OWT-A04, ARCH-07, OPS-37 and CODE-50: "partly" where the build says "not followed".

Note: 70c's NFR-13 remark about the reduced load seed applies to the build's own `QUALITY.md`. Step 70 ran the full D99 seed and met Q1.

### Own tests

- 49 backend tests with JaCoCo line 93.3 % and branch 82.3 %;
- 18 Vitest tests;
- Spotless, ESLint, Prettier and vue-tsc on the finish line.

Mutation score: not measured.

### Deviations from the rules (`DEVIATIONS.md`)

**Overridden by a decision:**

| Rules | Decision |
|---|---|
| API-46 | D48 |
| CODE-21, CODE-52, SEC-11, CODE-50 | D105 |
| CODE-11 | D135 |
| CODE-33 | D104 |
| CODE-45, SEC-12 | D17 |
| API-06, API-26, API-50, API-52 | D39, D30 |
| DATA-18 | D97 |
| DATA-22 to DATA-25 | D87 |
| SOA-09 | D77 |

**Not followed, with no decision:**

- API-25, OWT-A04, OWASP-03: no TLS and no encryption at rest;
- ARCH-07: single Fuseki and single Keycloak;
- SEC-07: no Simpl authenticator plugin;
- OWASP-07: Keycloak HIGH findings;
- the OPS pipeline rules OPS-05 to 08, 12, 17, 23 to 27, 33, 35, 37 and OWASP-12: no CI, SAST or Sonar;
- OPS-10, 11, 13, 28 to 30: no Helm chart;
- OPS-15, CODE-05, CODE-39, CODE-07, MSV-11, ARCH-22, ARCH-23, BLG-prf-cach, BLG-prf-lstmod, UX-01;
- NFR-10 and Q6: since measured by step 60, which found 0 violations;
- NFR-03 and NFR-13: since met by step 70 at the full seed.

**Decisions not implemented exactly as written:**

- D122: `/__problems/...` gives an HTML 404;
- D98 and D120: management port on the front network;
- D99: reduced seed in the build's own measurement.

### Scans

| Target | Step 70 CRITICAL | Step 70 HIGH | Build `SECURITY-SCAN.md` |
|---|---|---|---|
| deps (Maven, npm) | 0 | 0 | 0/0 |
| backend image | 0 | 0 | 0/0 |
| fuseki image | 0 | 0 | 0/0 |
| ui image | 0 | 0 | 3 HIGH fixed (musl CVE-2026-107891, zlib CVE-2026-85091) |
| keycloak 26.8.2 (vendor) | 0 | 6 | 6 HIGH accepted with reasons |

The six accepted Keycloak findings are:

- five with no fixed version: openjdk CVE-2026-91403, and sqlite-libs CVE-2026-119373, 119374, 119379 and 119586;
- guava CVE-2026-102554, which needs a newer Keycloak release.

The independent scan agrees exactly with the build's report.

## 6. New spec gaps

These are questions for the owners that steps 20 to 30 did not raise. They go back into step 30 as findings `R-nnn`, numbered on from the last R id there.

1. **S4 language tags:** do `@EN` and `@en` count as the same language in the duplicate-label rule, and how is the tag written in the location? (Build: lower case, one language.)
2. **S5 granularity:** does a resource typed with three pairwise disjoint classes give one issue or three? D65 fixes the location only.
3. **S3 position:** is the issue placed at the first or the later of the two declarations?
4. **Prefix declared twice:** does each declaration give its own warning or S7 error?
5. **Namespaces like `<contentBase>/content/<Name>/other#`:** these are neither `/versions/<v>` (D12) nor the D69 shapes. Are they external (a warning) or an error?
6. **External vocabulary name conflict:** what is the 409 detail text, and in what order are name, namespace and alias conflicts checked?
7. **Optional external description that is blank after trimming:** treated as absent, or 400?
8. **GET /health with a query parameter:** does D21 apply, so it gives 400?
9. **JSON body over 1 MB:** what are the 413 detail text and the issue (`in`, `name`)?
10. **`Accept: application/problem+json` on a JSON operation:** is it accepted, and what Content-Type does the 200 carry?
11. **Health of the store-down backend:** D44 makes GET /health answer 503 when the store is down. Which probe should the container health check use?
12. **Search results:** confirm there are no paging links, and that `totalPages` is 0 for an empty result.
13. **Version list `sort=name`:** all rows share the name. Is the tie-break (version descending) the decided order for both `name` and `-name`?
14. **Proxy internal paths** such as `/__problems/...`: must they answer problem+json as well (D122)?
15. **TDB2 storage growth:** who runs compaction, when, and with what downtime?
16. **Conditional write retries (D82, D92):** what backoff should be used? May the content skip validation again on retry?
17. **Rejections by the backend's HTTP server** (bad percent encoding, too many parameters, URL or header too long): must they also be problem+json, and with which status? (See fuzz finding 1.)
18. **Latest version column (step 45 Q-1, D5 against D100, SIMPL-18161/AC1):** is there a "Latest version" column, and where does it sit?
19. **Version `lastUpdateDate` (step 45 Q-2, D4 and D25):** does a manual PATCH of a version's status update it?
20. **409 on the forms (step 45 Q-3, D37 and D46):** does the alert show the API's problem detail, or a UI text, and if so which text?
21. **Order of checks on reads (step 45 Q-4, D51):** do 400 come before 503 and 404, and 403 before 404, on GET operations?
22. **UI selection and sort markers (step 60):** how is a version selected on the details page, and how is a sortable column shown?
23. **Failed-token limiting (70c, OWT-A07):** is it owned by the Tier 1 gateway, or must the local stack limit it, for example with nginx `limit_req`?
24. **Personal data (70c, ARCH-20, CODE-32):** should `createdBy` be the email or `sub`, should it be marked as personal data in the CDM, and may viewers see it?
25. **TRACE inside the network (70c):** must the backend refuse TRACE itself, not only the proxy?
26. **UI component library (70c, E6):** should the UI use the catalogue's PrimeVue and Tailwind components, or the "plain components" that `build.yaml` asks for?

## 7. Compared with earlier runs

| Run | Scenarios | Q1 | Trivy own / Keycloak | Coverage line / branch | Notes |
|---|---|---|---|---|---|
| run 11 (20 Nov) | 71 of 72 that ran (549 planned; 477 API not run) | met in full at the D99 seed, worst read p95 230 ms | 0/0; 0 CRITICAL, 6 HIGH | 93.3 % / 82.3 % | 1 UI failure (D18 on load); 3 fuzz findings |
| run 10 (20 Nov) | 527 of 530 after a manual API rerun (72 of 73 before) | throughput met; list read p95 320 ms missed | 0/0; 0 CRITICAL, 6 HIGH | 96.1 % / 86.9 % | 3 build bugs; API report lost in the UI step |
| run 9 (19 Nov) | 476 of 479 | met at the D99 load, worst read p95 150 ms | 0/0; 1 CRITICAL, 6 HIGH | 92.0 % / 77.9 % | 2 UI failures (now D100, D101) |
| run 8 (19 Nov) | 463 of 465 | throughput missed (now D99) | | | 1 UI failure on the report role (now D90) |
| run 7 (19 Nov) | 420 of 424 | met at the Q4 design load | | | review gaps decided as D82 to D87 |
| run 6 (19 Nov) | 428 of 432 | | | | first orchestrated run; 5 MB store write (now D71) |
| run 5 (18 Nov) | 463 of 468 | | 0 CRITICAL own | | Spring Boot 4.1.2 |
| run 4 (17 Nov) | 379 of 386 | | 17 CRITICAL | | |
| run 1 (27 Oct) | 74 of 74 API, 22 of 22 UI (own suite) | | | | |

**Better than run 10:**

- Q1 is met again, including the list read: p95 230 ms, against 320 ms in run 10.
- Accessibility is now measured, with 0 violations.
- There are fewer fuzz findings: 3 against 7.
- The step 45 blocker (D5 against D100) was handled by the test writer without a failure.

**Worse than run 10:**

- Own coverage is lower: by 2.8 points on lines and 4.6 on branches.
- The API suite again has no result. In run 10 the report was overwritten. Here the suite never ran, because of the tag and `timeout` defects. Two runs in a row lost the API measurement.

**Unchanged:**

- the same 6 Keycloak HIGH findings;
- 0 HIGH and 0 CRITICAL in own targets;
- one UI failure per run on an error-message decision (D36 in run 10, D18 now).

**Action for the factory:** run `./run.sh api` and `verify.py` against the running run 11 stack before using these figures. Make the orchestrator check that `api.json` holds non-skipped results before step 70.

## Note added after the run (20 Nov 2026)

The API suite was run again by hand against the same running stack (`./run.sh api` with the harness as the UI step left it, after the load test and fuzzing), and `verify.py` was run again. Result: 476 of 477 API scenarios pass; with UI and store-down, **547 of 549** (99.6 %). 82 of 85 live criteria and 116 of 154 decisions have all their scenarios passing.

The two failures:

- The UI failure of section 3.1 (D18, 403 on the list load): build bug.
- "A request that is not multipart/form-data gets 415: application/x-www-form-urlencoded" (D15): test bug. The step builds the form body with `urlencode` over fields that include the file part, and Python raises `TypeError` before the request is sent. The other two content types of the same outline pass.

So the build has one known bug in this run. The API suite was lost for the second run in a row, now by a harness defect the step 45 dry run cannot see. The factory fix is in section 7: check that `api.json` holds results that are not all skipped before step 70.
