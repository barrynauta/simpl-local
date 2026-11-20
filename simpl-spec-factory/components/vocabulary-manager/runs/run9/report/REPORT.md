# Final report: vocabulary-manager, run 9 (19 Nov 2026)

Project Simpl-Open, component vocabulary-manager, engine 348729b, 405 rules in scope.

Sources:

- `RUN.json`.
- `report/`: `verification.md` and `verification.json` (70a), `scan-summary.md`, `load.json`, `fuzz-summary.md` and `design-review.md` (70c).
- `tests/work/COVERAGE.md` and the behave reports in `tests/work/reports/`.
- `tests/review/SUITE-REVIEW.md`.
- From the build: `TRACEABILITY.md`, `DEVIATIONS.md`, `REUSE.md`, `QUALITY.md`, `SECURITY-SCAN.md` and `BUILD-LOG.md`.

To explain the two failures I read `logs/ui_60.md` and parts of `ui/src/views/VocabularyForm.vue` and `ui/src/views/VocabularyList.vue`.

## 1. In short

**What was built.** The stack runs under compose project `vm-factory-run9` and has four parts:

- a Java 21 backend on Spring Boot 4.1.2;
- Apache Jena Fuseki 6.2.0 as the store, with three store users (D83);
- Keycloak 26.8.2 with the realms of E1;
- a Vue 3 UI behind nginx, which also proxies `/v1`.

The code writer hit the usage limit again (`RUN.json`, step `write`: "You've hit your 5-hour limit"). `BUILD-LOG.md` says the interrupted attempt had already built the code, the stack, the docs and a first Trivy scan. The second attempt checked that work against D1 to D99, E1 to E7 and Q1 to Q6, and kept it. It made one fix, for the D90 text. It then reached the finish line:

- the stack starts from a clean state;
- `smoke.sh` passes;
- `mvn verify` is green: 59 tests, JaCoCo line 92.0 %, branch 77.9 %;
- Trivy ran, and every open finding has a reason;
- the stack was left running.

**How it scored.**

- **476 of 479** scenario runs passed (99.4 %). 2 failed and 1 is `@wip` (skipped).
- All 85 live criteria have a scenario. 82 have all their scenarios passing.
- 87 of 112 decisions have a scenario. 79 have all their scenarios passing.
- **Q1 is met in full at the D99 load shape** for the first time. The seed was 1000 vocabularies with 20 versions each, loaded with 0 errors. Reads ran at 20.0 per second and writes at 1.0 per second, with 0 errors. The worst read p95 was 150 ms, the worst write p95 720 ms (1 MB files), and the 5 MB create took 0.77 s.
- Fuzzing: 0 findings and 0 server errors. All 14 failed checks are explained by decisions.
- Trivy: 0 HIGH and 0 CRITICAL in every own target. The Keycloak vendor image has 1 CRITICAL and 6 HIGH, the same as the build's own `SECURITY-SCAN.md`.
- axe: 0 WCAG 2.1 AA violations on the four pages, and every list action can be reached by keyboard (Q6).

**What it means.** The API follows the decisions completely: every API, store-down and limit scenario passes. Both failures are UI build bugs:

- the vocabulary list has no "Creator" column (SIMPL-18161/AC1);
- the create form does not show all field errors on Submit (D37, D62).

The second one probably comes from a layout shift, not from missing validation code. Both are small fixes. The design review finds no MUST rule "not met". It does find 8 SHOULD rules not met that the build does not record (section 5). The main structural gaps are:

- no circuit breaker;
- the stack cannot scale out;
- no shared exception model;
- no Simpl log format.

Some of these need a decision before a build can meet them.

## 2. Results

### Scenarios per feature

| Feature | Runs | Passed | Failed | Skipped |
|---|---|---|---|---|
| Access control on the Vocabulary Management API | 103 | 103 | 0 | 0 |
| Register, read and remove external vocabularies | 47 | 47 | 0 | 0 |
| Read vocabularies, versions, version history and content | 43 | 43 | 0 | 0 |
| List, filter, sort and page vocabularies | 38 | 38 | 0 | 0 |
| Search vocabularies and external vocabularies | 33 | 33 | 0 | 0 |
| Create a vocabulary through the API | 32 | 32 | 0 | 0 |
| Create a new version of a vocabulary | 29 | 29 | 0 | 0 |
| Vocabulary references in uploaded content | 25 | 25 | 0 | 0 |
| Change the vocabulary status and the version status | 23 | 23 | 0 | 0 |
| Content negotiation, problem format and trace headers | 20 | 20 | 0 | 0 |
| Validation of uploaded vocabulary content | 18 | 18 | 0 | 0 |
| Behaviour when the vocabulary store is not reachable | 9 | 9 | 0 | 0 |
| Upload limits and time budget | 9 | 9 | 0 | 0 |
| Vocabulary Management UI | 49 | 47 | 2 | 0 |
| Criteria that a black-box API run cannot trigger | 1 | 0 | 0 | 1 |
| **Total** | **479** | **476** | **2** | **1** |

The load test is not a behave scenario. Its results are in section 5 (Q1).

### Scenarios per tag kind

| Tag kind | Runs | Passed | Failed | Skipped |
|---|---|---|---|---|
| `@ui` (browser, step 60) | 49 | 47 | 2 | 0 |
| `@negative` | 296 | 294 | 1 | 1 |
| `@wip` (not runnable black box) | 1 | 0 | 0 | 1 |
| Criterion tags (`@AC_...`) | 85 criteria | 82 criteria all passing | 2 criteria with a failure | 1 criterion with only a skip |
| Decision tags (`@D..`, `@E..`, `@Q..`) | 87 decisions | 79 all passing | 6 with a failure | 2 with a skip |
| Rule tags | 25 rules | 24 all passing | 0 | 1 (API-44, one skip) |

### Criteria

- Live criteria: 85. SIMPL-28680 is out of scope (D1, D61).
- With a scenario: 85 of 85 (100 %).
- With all scenarios passing: 82 of 85 (96 %).
- Not all passing:
  - SIMPL-18161/AC1: 1 scenario, failed.
  - SIMPL-18169/AC2: 3 scenarios, 1 failed.
  - SIMPL-18162/AC4: only a `@wip` scenario, skipped.

### Decisions

- With a scenario: 87 of 112 (78 %).
- With all scenarios passing: 79 of 112 (71 %).
- With a failure: D31, D34, D37, D62, D72, D75. All of them come from the two UI scenarios.
- With a skip only: D24 and D44. Both come from the `@wip` scenario of SIMPL-18162/AC4.
- With no scenario (25): D2, D39, D40, D61, D79, D80, D81, D83, D85, D87, D88, D89, D93, D94, D95, D96, D97, D98, D99, E2, E3, E6, Q3, Q4, Q5. `COVERAGE.md` gives a reason for each one: build-side, operational, withdrawn, or tested under other decisions.

## 3. Failures

### Failing scenarios

**1. "The vocabulary list shows every vocabulary with its columns and actions"** (`ui.feature:7`; SIMPL-18161/AC1, D31, D34, D72, D75)

- The failing step is: Then the UI shows a table with the column headers "Name, Description, Latest version, Status, Date created, Last updated, Creator".
- The assertion: missing header `Creator`. The build shows Name, Status, Latest version, Description, Date created, Last updated, Actions.
- **Cause: build bug.** SIMPL-18161/AC1 asks for the creator in every row. D31 fixes the term "creator" (wire `createdBy`). D27 says the vocabulary shows the creator of version 1, and `createdBy` is on `VocabularyMetadata`. So the data is there, but `VocabularyList.vue` has no column for it. The build's `TRACEABILITY.md` row for SIMPL-18161/AC1 already leaves the creator out ("name, latest version, status, dates, actions").
- **Step 45 flagged this scenario** (SR-01), but for a different header: "Vocabulary status" should be "Status". The test writer fixed that. SR-01 also said explicitly to keep "Description" and "Creator", because they come from the criterion. The test was right to keep them.
- **Spec gap behind it.** D75 says the list headers are "named as in D23". D23 names only the sortable columns. No decision lists the full set of list columns, their order or the header of the actions column (see gap 1 in section 6).

**2. "Field errors are shown under the fields and nothing is sent"** (`ui.feature:118`; SIMPL-18169/AC2, D37, D62)

- The failing step is: And the UI shows a field error under "Description". The assertion: no error under 'Description'.
- Step 60 also checked by hand. No error appears under 'Vocabulary file', and the focus stays on Submit instead of moving to Name. Nothing is sent, which is correct.
- **Cause: build bug against D37 and D62.** D62 says a click on Submit with field errors sends nothing, keeps the errors under the fields and moves the focus to the first field with an error. D37 makes Description and the file required.
- **Likely mechanism.** I inferred this from the code; it is not proven by a test.
  - The `submit()` handler in `VocabularyForm.vue` does check all four fields and focuses the first error. If it ran, the scenario would pass.
  - What step 60 saw (only the Name error, focus on Submit) matches the handler never running.
  - The Name field validates on blur. When the mouse button goes down on Submit, Name loses focus and its error paragraph is inserted. That pushes the Submit button down before the mouse button comes up, so the browser fires no click on the button.
  - A real user who clicks with the mouse would see the same thing.
- **Fix:** keep the button in place when an error appears (for example, reserve the space for field errors), or validate only on submit.
- **Step 45 flagged this scenario** (SR-19), but because it did not check "nothing is sent". The test writer added that check, and it passes. The failing check was never questioned. Step 60 asked whether every empty required field must show its error on Submit (Q-UI-3) and read D37 and D62 that way. I agree with that reading.

**Skipped:** "An internal error while listing returns 500 with a detail and no metadata" (`wip.feature:7`; SIMPL-18162/AC4, D24, D44, API-44). A 500 needs a store fault that is not a timeout, and that needs fault injection. The build's own `RequestParsingTest.problemsHaveDecidedTypes` covers the path. It is the same `@wip` as in run 8.

### Fuzz findings

Schemathesis, 20 operations, 30 examples each, seed 1: 14 failed checks, 14 explained by a decision, **0 findings, 0 server errors**. There is no build bug and no decision missing from the fuzz list.

All 14 are **contract problems in the OpenAPI**: the published `vocabulary_openapi-tier1.yaml` is looser or different from the decisions. D39 and D79 already list these points for the owners.

| Operation(s) | Check | Decision | What the OpenAPI lacks |
|---|---|---|---|
| POST /vocabularies, POST /external-vocabularies | ST202 | D9 (D70, D79) | The file is typed as any binary; the decisions require valid, non-empty Turtle |
| POST /vocabularies/searches, POST /external-vocabulary/searches, POST /external-vocabularies/content, PATCH /vocabularies/{vocabularyName} | ST202 | D52 (D38) | `additionalProperties` is allowed; the decisions reject unknown fields and limit the search arrays |
| POST /vocabularies/{vocabularyName}/versions | ST202 | D37, D7 | Length limits of 1 to 1000 after trimming; the version form field is refused |
| GET /content/{vocabularyName}/versions/{version}, GET and PATCH /vocabularies/{vocabularyName}/versions/{version} | ST202 | D49 | The version must be a positive integer |
| GET /external-vocabularies, GET /vocabularies/{vocabularyName}/versions | ST202 | D22 | page at least 1, pageSize 1 to 100 |
| GET /vocabularies | ST201 | D19 | The name filter pattern is relaxed on purpose (wider than the contract) |
| GET /vocabularies | ST202 | D20 | A creationDate that matches the pattern but is not a real date (`format: date` is missing) |

The `fuzz-summary.md` reason text for D52 also cites "D38 limits on search bodies" for PATCH, where it does not apply. That is a wording slip in `fuzz-known.yaml`, not a finding.

## 4. Traceability

| Measure | Share |
|---|---|
| Live criteria with all scenarios passing | 82 of 85 (96 %) |
| Live criteria with at least one passing scenario | 83 of 85 (98 %) |
| Live criteria with a scenario at all | 85 of 85 (100 %) |
| Live criteria with code, by the build's `TRACEABILITY.md` | 85 of 85 (100 %) |

- **Criteria with nothing:** none.
- **Weakest criteria:**
  - SIMPL-18162/AC4 has only a `@wip` scenario.
  - SIMPL-18161/AC1 has only a failing scenario. Its traceability row leaves out the creator column the criterion asks for.
- **UI criteria:** the 37 UI-only criteria and the 7 UI-story criteria are covered only by `ui.feature`. The 7 UI-story tags were moved there after SR-13.
- **Criterion overridden by a decision:** SIMPL-18184/AC2 asks for a blocking error, and D11 with D47 make it a warning. The suite tests the warning, as decided.

## 5. Quality of the build

### Quality targets

| Target | Measured | Verdict |
|---|---|---|
| **Q1** reads p95 at most 300 ms | Worst is getVocabularies at 150 ms; all others 11 to 24 ms (`load.json`, 5 min after a 30 s warm-up, seed 1000 x 20 versions) | met |
| **Q1** writes up to 1 MB, p95 at most 3 s | createVocabulary 420 ms, createVocabularyVersion 540 ms, createVocabularyVersion1MB 720 ms | met |
| **Q1** 5 MB create at most 30 s (D71) | 765 ms p95, 3 requests | met |
| **Q1** throughput 20 reads/s and 1 write/s, at least 95 % achieved, no errors (D99) | 20.01 reads/s and 1.00 writes/s; 0 failures in about 6,600 requests; seed errors 0 | met |
| **Q2** limits (NFR-04) | 21 Q2-tagged scenarios pass, each at and above its limit | met. The build did not run 4 parallel 6 MB uploads; the suite's 6 parallel 3 MB uploads get 201 or the decided 503 |
| **Q3** availability 99.5 %, RTO 30 min, RPO 24 h | Not measured by step 70 (NFR-13). The build's backup and restore round trip passed on a small store (38 quads, about 2 min including the corrupt-backup check) | not measured at design load; the restore procedure is shown to work |
| **Q4** design load | Used as the seed of the load test. One backend instance, as Q4 says | met as a profile. See gap 3: Q4 still cites the withdrawn D81 |
| **Q5** line coverage at least 80 %, branch at least 60 % | 92.0 % line, 77.9 % branch (build's JaCoCo) | met |
| **Q5** no HIGH or CRITICAL in own images and dependencies | 0 and 0 in all four own targets (step 70 Trivy) | met |
| **Q5** one store query per reference check | The build says one query (`ReferenceAnalyzer`); not measured independently | met by the build's claim |
| **Q6** WCAG 2.1 AA, keyboard | axe through Playwright: 0 violations on the list, details and both form pages; keyboard scenario passes | met |

**Mutation score:** not measured.

**Own tests:** 59 backend tests in `mvn verify` and 14 Vitest UI tests in 4 files, all green. There are gates for Spotless, ESLint, Prettier and vue-tsc.

### Reuse

`REUSE.md` has a row for every major capability. It reuses products throughout: Keycloak, Fuseki, Spring Security with Nimbus, Jena RIOT and ARQ `ParameterizedSparqlString`, Micrometer, keycloak-js and N3.js. It justifies its own code:

- `RealmKeySource`: D88 timing that Nimbus `JWKSourceBuilder` cannot give;
- `FusekiSparqlStore`: D44 timeouts and retries;
- `MultipartForm`: D15, D70 and D76;
- the hand-written request validation: allowed by D93, and checked against `openapi-decided.yaml` by `ContractConformanceTest`.

70c verdicts on REUSE rules: 5 met, 3 partly.

| Rule | Verdict | Why |
|---|---|---|
| REUSE-01, 03, 04, 06, 09 | met | REUSE-06 has a naming nit: "Backend for frontend" for a plain nginx proxy. REUSE-09 notes that `MultipartForm` uses Tomcat-internal `MultipartStream`, which may break on Tomcat upgrades |
| REUSE-02 | partly | The query string is split by hand (`RequestChecks.query`), paging links are built by hand (`Representations.link`), and the Basic auth header is built by hand |
| REUSE-05 | partly | Catalogue items not covered: `api-gateway-authorisation`, `http-client`, `participant-shared-code`, and `object-storage` against the base64 `ContentChunks` in Fuseki |
| REUSE-07 | partly | No rows for `ContentChunks`, the circuit-breaker choice, or the query and link code |

### Design review (70c)

There are 153 review rules:

- MUST (52): 34 met, 14 partly, 0 not met, 4 not applicable.
- SHOULD (101): 59 met, 25 partly, 8 not met, 9 not applicable.

Main findings:

1. **No circuit breaker** on the store or the key endpoint (CODE-46, ARCH-34). With a slow store, each call still waits up to 10 s (reads) or 45 s (writes), and the 64 Tomcat threads can fill up.
2. **The code can scale out, the deployment cannot** (ARCH-07, MACH-09). `compose.yaml` pins the backend IP and the management address. `QUALITY.md` Q4 says several instances "may run", which is true of the code only.
3. **No shared exception base and no error-code enum** (CODE-33, CODE-44). The single `ApiExceptionHandler` meets CODE-34 to CODE-36.
4. **The logs are logstash JSON, not the Simpl log format** (CODE-21, CODE-52), and nothing ships them (SEC-11). D96 itself is met.
5. **`REUSE.md` is incomplete** (REUSE-05, REUSE-07), as in the table above.

Disagreements with the build's own documents:

- Not met, and not recorded in `DEVIATIONS.md`: CODE-46, ARCH-34, CODE-33, CODE-44, CODE-21, CODE-52, ARCH-22, ARCH-27. ARCH-27 is covered by D45 but should still be listed.
- The build says met, and 70c finds partly:
  - scaling (Q4 and `operations.md`);
  - D98: the management port 8081 is bound to 0.0.0.0 on the front network in the `backend-store-down` profile.
- Small inaccuracies:
  - `QUALITY.md` names `vocabulary-manager.limits`; the key is `vm.limits`.
  - The `RouteTable` javadoc cites a `RouteTableTest` that does not exist.
  - D44 says keys are fetched "at start"; `RealmKeySource` fetches them lazily.

70c agrees with the build on OWT-A04, OWT-A07, OWT-A09, OWASP-05, OPS-37, the Keycloak dev mode and the PrimeVue gap. It also says `QUALITY.md` was honest that the build's own Q1 run was not at design load.

### Recorded deviations (`DEVIATIONS.md`)

| Rule(s) | What the build does instead |
|---|---|
| API-46 | Belgif problem types for 400, 404, 500 and 503, as D48 requires |
| SOA-09 | Overlapping paths; a fixed segment wins (D77) |
| OWT-A04, OWASP-03 | Plain HTTP and no encryption at rest in the local stack |
| OWT-A07 | No rate limit on failed calls in the service; the gateway does it (D46) |
| OWT-A09, OWASP-10 | Denials are logged and counted, but no alerting |
| OPS-06, OPS-33, OPS-35, OPS-37 | No CI pipeline (D95); the gates run in `mvn verify` and the UI build |
| OWASP-05 | The threat model was written during the build, not before it |
| CODE-39 | `@throws` only on checked exceptions |
| OWASP-07 | The Keycloak image has findings with no fix |
| OWT-A02 | Keycloak runs in dev mode (allowed by D98; port on 127.0.0.1 only) |
| CODE-03, CODE-04 | Not applicable: the UI is Vue |
| E6 | Plain Vue components instead of PrimeVue and Tailwind |

Decisions: for D99 the build's own load run was short, and step 70 ran the full one. D10 `semanticReasoningSkipped` is reserved and never emitted.

### Scan results

| Target | Step 70 CRITICAL | Step 70 HIGH | Build's `SECURITY-SCAN.md` |
|---|---|---|---|
| deps (`work/` file system) | 0 | 0 | 0 / 0 |
| vocabulary-manager-be:1.0.0 | 0 | 0 | 0 / 0 |
| vocabulary-manager-ui:1.0.0 | 0 | 0 | 0 / 0 |
| vocabulary-store:1.0.0 (Fuseki) | 0 | 0 | 0 / 0 |
| quay.io/keycloak/keycloak:26.8.2 | 1 | 6 | 1 / 6, all accepted with a reason |

The independent scan agrees with the build's scan exactly.

The seven open Keycloak findings are:

- CVE-2026-114938 (CRITICAL) and CVE-2026-91403 in the OpenJDK RPM;
- four sqlite-libs CVEs;
- CVE-2026-102554 in guava 33.6.0. It is fixed in 33.7.2, but no Keycloak release ships it yet.

The build does not patch the vendor image, and Keycloak is only the test identity provider. That is a reasonable call; it is recorded under OWASP-07.

The counts before the fixes are not known, because the interrupted attempt did not keep them.

### Notes for the factory, not the owners (from 70c)

- Make `REUSE.md` list every catalogue entry.
- Make `DEVIATIONS.md` cover every MUST and SHOULD rule in the 70c review set.
- Add `docker compose up --scale backend=2` to the finish line.
- Mark the delivery-model rules (ARCH-14, ARCH-24, OPS-29, CODE-45, SEC-12) not applicable for compose-only builds, or add a minimal chart to the brief.
- The verify.py defect of run 8 (`dry-run.json` overwrote the results) did not recur. `dry-run.json` is still in `reports/`, but the 70a numbers match my own recount.

## 6. New spec gaps

These are questions for the owners that steps 20 to 30 did not raise. Step 30 can take them in as findings R-nnn.

1. **List columns (D23, D75, SIMPL-18161/AC1, D27).** Which columns does the vocabulary list have, in which order, and with which headers? Is "Creator" one of them, and does it show `createdBy` of version 1? Does the actions column have a header? D75 points to D23 for header names, but D23 names only the sortable columns.
2. **Field errors on Submit (D37, D62).** Must every empty required field (Name, Description, Vocabulary file) show its error on Submit, even before the user has touched it or chosen a file? Must a mouse click on Submit always go through when a blur error appears at the same moment?
3. **Q4 cites a withdrawn decision.** Q4 says "one backend instance (D81); scale up, not out, until a store-side check exists". D81 is withdrawn by D82, and D82 provides that store-side check. Must the delivered stack run more than one backend instance?
4. **Circuit breaking (CODE-46, ARCH-34).** For the store and the key endpoint: thresholds, half-open probe, and what the caller sees while the breaker is open. Is Resilience4j the default?
5. **Exception model (CODE-33, CODE-44).** Is there a shared exception base and an error-code enum? Or do the Belgif and simpl problem types of D48 replace them?
6. **Logging contract (CODE-21, CODE-52, SEC-11).** Is the Simpl log format (LogWrapper) and log shipping in scope for this component? If yes, the build input needs the library or its spec.
7. **Where file bytes live (REUSE-05).** Is storing uploads as base64 chunks in Fuseki the intended design, or should content go to the catalogue's object storage (MinIO)?
8. **Fuseki host port (D83 against the build profile).** `RUN.json` lists a Fuseki port 3141, and D83 forbids a host port for the store. Which one wins for the local stack?
9. **D94 against D88.** D94 names Nimbus `JWKSourceBuilder`, but it cannot give the refetch outcomes of D88. Can D94 be changed to "Nimbus `JWKSource` with own fetch timing"?
10. **Key fetch time (D44).** D44 says keys are fetched "at start". Is a lazy fetch on the first token or the first health call acceptable?
11. **Singular texts (D90).** For one error the decided text reads "1 errors found". Is "1 error found" wanted? What is the decided text of the warnings line ("<n> warnings found", role status), which D90 does not fix?
12. **Choices the decided contract leaves open** (`BUILD-LOG.md` gap 3, `docs/api-notes.md`). Please confirm or decide each of these:
    - JSON body limit of 1 MB, with 413;
    - at most 64 multipart parts, with 400;
    - date-times without an offset read as UTC;
    - `createdBy` and `latestVersionHref` also on the 201 body;
    - `aliasUris` always present;
    - the issue name for a bad element of a search array;
    - the order of parameters in paging links.
13. **Paging links (D22, D78).** Must self, first, prev, next and last always carry `page` and `pageSize`, even when the request did not send them? (Step 45, Q-3.)
14. **Integers in the query and path (D22, D91).** Does the textual form or the value decide whether `2.0` or `1e1` is an integer? (Step 45, Q-2.)
15. **Page after a new version (D72, D14).** Which page does the UI show after a successful new version from the Update form? (Step 45, Q-4.)
16. **Composite alerts (D34, D35, D72).** Are the load-error and status-change texts one alert, or separate elements? (Step 45, Q-6.)
17. **Names and roles in the UI (D34, D36).** What are the names of the page controls ("Next page", "Previous page")? Which role does the deprecation banner on the details page have? (Step 60, Q-UI-1 and Q-UI-2.)
18. **Contract formats (D20, D9, D79).** When the next contract version takes in D79, please add these too, so that fuzzing can tell decided rejections from defects:
    - `format: date` and `format: date-time` on the date filters;
    - the Turtle media type on `vocabularyFile`;
    - a minimum of 1 on the version in paths.
19. **CDM findings for the CDM owner** (70c DATA-04, DATA-06, DATA-10, DATA-12, DATA-14, DATA-16):
    - search request classes are in the domain model;
    - the attributes have no types;
    - `latestVersion` and `description` are duplicated;
    - the timestamp names are generic.

## 7. Compared with earlier runs

| Run | Result | Main points |
|---|---|---|
| run1 (27 Oct) | 74 of 74 API and 22 of 22 UI, own suite | Not comparable: the build wrote its own suite |
| run4 (17 Nov) | 379 of 386 | Trivy 17 CRITICAL |
| run5 (18 Nov) | 463 of 468 | Spring Boot 4.1.2; 0 CRITICAL in own targets |
| run6 (19 Nov) | 428 of 432 | First orchestrated run; failures at the 5 MB store write (D71 now) |
| run7 (19 Nov) | 420 of 424 | Q1 met at Q4 design load; design gaps later decided as D82 to D87 |
| run8 (19 Nov) | 463 of 465 | 1 UI failure, the report role (D90 now); Q1 throughput missed (D99 now); 70a figures wrong (verify.py) |
| **run9 (19 Nov)** | **476 of 479** | 2 UI failures (Creator column, field errors on Submit); Q1 fully met at the D99 shape; 0 fuzz findings |

What changed against run 8:

- **Scale of the suite and the decisions:** 479 runs against 465, and 112 decisions against 100 (D88 to D99 added). Decision coverage is 87 against 83, all passing 79 against 80. The drop comes from the two new UI failures, which touch 6 decisions.
- **Fixed:** the run 8 failure (D90 report role) now passes. Q1 throughput, missed in run 8, is met with the offered-rate method of D99.
- **New failures:** both are UI checks that the suite review and step 60 added or kept. They are not regressions of earlier passing behaviour.
- **Design risks named in run 8:**
  - now addressed: lint and format gates (Spotless, ESLint, Prettier), own UI tests (14 Vitest), parameterised SPARQL (`ParameterizedSparqlString`), and an upload bulkhead (`UploadSlots`, D92);
  - still open: hand-written request validation, now sanctioned by D93 and checked by `ContractConformanceTest`;
  - new from 70c: no circuit breaker, no scale-out of the stack, the exception model, and the log format.
- **Scans:** unchanged. 0 in own targets, and 1 CRITICAL and 6 HIGH in the Keycloak vendor image.
- **Process:** the code writer hit the 5-hour usage limit in both run 8 and run 9. In both, the second attempt kept the earlier code and did not record the scan counts before the fixes.

Agent cost recorded in `RUN.json` for this run: about 33.5 USD. That is test writing 9.25, code writing 4.07, suite review 4.00, test fixes 13.21 and design review 2.93.
