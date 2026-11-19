# Final report: vocabulary-manager, run 6 (19 Nov 2026)

Project Simpl-Open, component vocabulary-manager, engine 2b73430, 405 rules in scope. This report draws on `report/verification.md` (step 70a), `scan-summary.md` and the raw Trivy files, `fuzz-summary.md` with `fuzz.json` and `fuzz/console.txt`, `design-review.md` (step 70c), `tests/work/COVERAGE.md` and its reports, `tests/review/SUITE-REVIEW.md` (step 45), and the build's `TRACEABILITY.md`, `DEVIATIONS.md`, `REUSE.md`, `QUALITY.md`, `SECURITY-SCAN.md` and `BUILD-LOG.md`. To explain the two API failures I also read the Fuseki and backend container logs and `application.yaml`. `load.json` exists but is a skip record (no `Q` decision).

## 1. In short

**What was built.** The code writer built the full component from the specs and 65 decisions (D1 to D61, E1 to E4). The stack has four parts, all running under Docker Compose project `vm-factory-run6`:

- a Java 21 backend on Spring Boot 4.1.2;
- Apache Jena Fuseki 6.2.0 as the store;
- Keycloak 26.8.2 with the realm of E1;
- a Vue 3 UI behind nginx.

The write step stopped once on a usage limit. It was resumed, and the second session kept the first session's code after checking it.

**How it scored.** The independent suite has 254 scenarios (432 cases). Against the running stack:

- 428 cases passed (99.1%), 3 failed and 1 was skipped (`@wip`).
- API: 383 of 385 run cases pass.
- Store down: 5 of 5.
- UI: 40 of 41, including 4 of 4 accessibility checks with 0 axe violations.
- 84 of the 85 live criteria have all their scenarios passing. The 85th, SIMPL-18162/AC4, has only a `@wip` scenario.

**Why these numbers differ from `verification.md`.** That file reports 444 scenarios, 13 skipped, 74 of 85 criteria passing and 40 of 65 decisions passing. Those figures are lower than the truth because of a counting fault in the engine, not in the build:

- `verify.py` keys each scenario by `file:line`.
- Step 60 edited two UI feature files at 03:58, after the API run at 03:49. That shifted 12 UI scenarios down by one line.
- Each of those 12 scenarios therefore appears twice: once as "skipped" at its old line (in `api.json`) and once as "passed" at its new line (in `ui.json`).

Merging the results by feature and scenario name gives the corrected figures used in this report.

**The failures.**

- Two of the three failures share one cause, which is a build bug. A 5 MB upload takes Fuseki about 20 s to store. D44 fixes the store response timeout at 10 s, so the request gets 503 instead of 201. The next write, queued behind it in the store, times out too.
- The third failure is a UI question that the decisions leave open. When the browser refuses a file over 5 MB, does the message go under the file field (D37) or above the form (D15)?

**Fuzzing.** It found no server errors. All six findings follow from decisions that the fuzz list does not know (D37, D38, D52), plus one stateful case that raises two small owner questions.

**Design review.** It found real gaps the build's own documents do not mention:

- creator email addresses go into INFO logs;
- the restore script can leave an empty store;
- the reference check sends an unbounded number of queries to the store per upload.

**What it means.** Every API behaviour the decisions fix passes, except storing a file at the size limit. The supply chain is clean for everything this project builds: 0 HIGH and 0 CRITICAL. The 7 open findings are all in the third-party Keycloak image and all have reasons. The run is ready for one fix (store writes at the 5 MB limit), one engine fix (`verify.py` result merge) and the owner questions in section 6.

## 2. Results

| Measure | Corrected (merged by scenario name) | As reported by `verification.md` |
|---|---|---|
| Cases passed | 428 of 432 (99.1%); 3 failed, 1 skipped (`@wip`) | 428 of 444 (96%); 3 failed, 13 skipped |
| Live criteria with a scenario | 85 of 85 (100%) | 85 of 85 |
| Live criteria with all scenarios passing | 84 of 85 (99%) | 74 of 85 (87%) |
| Decisions with a scenario | 58 of 65 (89%) | 58 of 65 |
| Decisions with all scenarios passing | 55 of 58 with a scenario (55 of 65 overall) | 40 of 65 (62%) |
| Rules tested | 27; 26 all passing, NFR-04 34 of 35 | 27 |
| Accessibility (axe, WCAG 2.1 A and AA) | 0 violations on 4 pages; 2 "incomplete" checks on the list page (color-contrast on 20 nodes, aria-prohibited-attr on 1 node) need a person | |

### Per feature

| Feature file | Passed | Failed | Skipped |
|---|---|---|---|
| access_control | 86 | 0 | 0 |
| create_vocabulary | 35 | 2 | 0 |
| cross_cutting | 13 | 0 | 0 |
| external_vocabularies | 65 | 0 | 0 |
| list_vocabularies | 49 | 0 | 1 (`@wip`) |
| search | 18 | 0 | 0 |
| status | 29 | 0 | 0 |
| store_down | 5 | 0 | 0 |
| validation | 33 | 0 | 0 |
| version_history | 28 | 0 | 0 |
| versions | 27 | 0 | 0 |
| ui_accessibility | 4 | 0 | 0 |
| ui_create_and_version | 12 | 1 | 0 |
| ui_details_and_access | 14 | 0 | 0 |
| ui_vocabulary_list | 10 | 0 | 0 |
| **Total** | **428** | **3** | **1** |

### Per tag kind

A case counts once for each kind of tag it carries.

| Tag kind | Cases | Passed | Failed | Skipped |
|---|---|---|---|---|
| Decision (D, E) | 431 | 428 | 3 | 0 |
| Criterion (AC_) | 231 | 230 | 0 | 1 |
| Negative | 266 | 265 | 1 | 0 |
| Rule | 153 | 152 | 1 (NFR-04) | 0 |
| UI | 41 | 40 | 1 | 0 |
| Accessibility | 4 | 4 | 0 | 0 |
| Store down | 5 | 5 | 0 | 0 |
| Work in progress (wip) | 1 | 0 | 0 | 1 |

### Criteria and decisions

- **Criteria with a failing scenario:** none. None of the three failing scenarios carries a criterion tag.
- **Criterion with only a skipped scenario:** SIMPL-18162/AC4 (internal error gives 500). The suite cannot cause a 500 from outside. A store outage gives 503 by D44 and is tested in `store_down.feature`.
- **Decisions with a failing scenario:**
  - D15: 11 of 13 pass;
  - D13: 2 of 3;
  - D58: 6 of 7, and this failure is a side effect of the D15 one (section 3.2).
- **Decisions with no scenario:** D2, D39, D40, D61, E1, E3 and E4. `COVERAGE.md` gives a reason for each: a working rule, scope, or run environment, none observable as behaviour.
- **Rules tested:**
  - all passing: API-12, API-36, API-38, API-44, API-45, API-46, API-47, ARCH-06, ARCH-32, ARCH-35, BLG-col-paging, BLG-err-problem, BLG-err-sanit, BLG-hlth-res, BLG-rep-struct, BLG-req-valid, BLG-stat-codes, MACH-08, MSV-07, NFR-07, NFR-10, OWA-API3, OWASP-02, OWASP-08, SEC-05, SOA-05;
  - one failure: NFR-04 (34 of 35 pass).

## 3. Failures

### 3.1 A file of exactly 5 MB is not stored in time (build bug)

- **Scenario:** create_vocabulary "A file of exactly 5 MB is accepted" (`create_vocabulary.feature:193`). Tags D15, NFR-04.
- **What happens:** the API returns 503 `serviceUnavailable`, "The vocabulary store is not available. Retry later.", after 11.2 s. The suite expects 201.
- **Cause:** the store write takes longer than the decided timeout.
  - The Fuseki log shows the update for this request starting at 02:48:49 UTC and finishing with `204 No Content (20.265 s)`.
  - D44 fixes the store response timeout at 10 s (`STORE_RESPONSE_TIMEOUT:10s` in `application.yaml`). The backend gave up after 10 s and answered 503 as D44 says.
  - The build stores the content as base64 inside one SPARQL update literal, about 7 MB for a 5 MB file (design review, finding 3). Fuseki needs about 20 s to parse and commit that.
- **Classification: build bug.** D15 says a 5 MB file must be accepted and D44 fixes 10 s. This build's storage design cannot meet both on this stack. In run 5 the whole create feature passed on the same stack, so storing a 5 MB file within the limit is possible.
- **Gap in the build's own checks:** `QUALITY.md` checked only that a 6 MB file gives 413. It never checked that a 5 MB file is stored within the timeout.
- **Side note:** the write did commit after the 503 (Fuseki returned 204 at 02:49:10). D44 allows this ("after a 503 the store holds the whole change or none of it"), so it is not a second defect. But it means a client that ignores D44's "check with GET first" advice and resubmits gets 409.
- **Step 45:** it raised question 8 on this scenario (is the limit on the file part or on the whole request?). The test writer kept the scenario. The failure has nothing to do with that question.

### 3.2 Times are UTC to the second with a Z (side effect of 3.1)

- **Scenario:** create_vocabulary "Times are UTC to the second with a Z" (`create_vocabulary.feature:217`). Tag D58.
- **What happens:** 503 after 10.0 s instead of 201.
- **Cause:** this scenario ran right after 3.1. Fuseki (TDB2) allows one write transaction at a time, so its small update waited behind the 20 s write. Fuseki logs it as `204 No Content (10.253 s)`, committed after the backend had already answered 503.
- **Classification:** the same build bug as 3.1. D58 itself passes in the 6 other scenarios that check times.
- **Spec point (R-040):** D43 says changes to different vocabularies are not serialised against each other, but the store serialises every write. One upload at the limit blocks all other writes.

### 3.3 A file above 5 MB is refused in the browser (spec gap)

- **Scenario:** ui_create_and_version "A file above 5 MB is refused in the browser" (`ui_create_and_version.feature:75`). Tags ui, D13, D15, negative.
- **What happens:** the UI refuses the file and disables Submit, so D13 is met. But it shows the refusal above the form ("Uploaded vocabulary file is too large..."), not as a field error under "Vocabulary file".
- **Step 45 history:** finding SR-12 flagged this scenario, because its old check "I see the text 5 MB" fixed a text no decision gives. The test writer replaced that check with "field error under Vocabulary file and Submit disabled", reading D37. Step 60 kept the scenario and raised the question.
- **Why it is ambiguous:**
  - D37 says the UI shows field errors under the field before submit, and server errors (400, 409, 413) above the form.
  - D15 says that above 5 MB "the UI shows its detail text", which is the 413 text the build shows.
  - The browser check is neither a server 413 nor one of the field rules D37 lists.
- **Classification: the decision is ambiguous (spec gap).** Neither the build nor the suite clearly contradicts a decision. Question R-036 in section 6.

### 3.4 Skipped: internal error 500 (not a failure)

- **Scenario:** list_vocabularies "An internal error while listing returns 500 with the detail of the error" (`list_vocabularies.feature:299`, SIMPL-18162/AC4). It is `@wip`.
- The build has the code for it (`GlobalExceptionHandler`) and an own unit test (`SparqlClientTest.storeErrorIsAFailureNotUnavailable`).

### 3.5 Fuzz findings (Schemathesis, 20 operations, 30 examples each, seed 1)

There were 12 failed checks and 0 server errors. Six are already explained in `fuzz-known.yaml` (D9, D19, D20, D22). The six findings:

| # | Operation | What the fuzzer did | Answer | Cause |
|---|---|---|---|---|
| F1 | POST /external-vocabularies/content | JSON body with unknown properties (`ñ` and others) | 400 | **The fuzz list misses a decision:** D52 says any property the schema does not define gives 400, even without `additionalProperties: false`. **The OpenAPI also has a contract problem:** it should declare `additionalProperties: false`. |
| F2 | POST /external-vocabulary/searches | `namespaceUris` with a repeated URI and `aliasUris: []` | 400 "Field 'aliasUris' mu..." | **The fuzz list misses a decision:** D38 says each array has at least 1 entry when present. **Contract problem:** the OpenAPI has no `minItems: 1`. |
| F3 | POST /vocabularies/{vocabularyName}/versions | a changelog value the decided rule refuses | 400 "Field 'changelog' is..." | **The fuzz list misses a decision:** D37 says changelog is 1 to 1000 characters after trimming. **Contract problem:** the OpenAPI has no length limits. The build log says the same about the D37 pattern. |
| F4 | PATCH /vocabularies/{vocabularyName} | unknown JSON properties | 400 | **The fuzz list misses a decision:** D52. **Contract problem:** `additionalProperties: false` is missing. |
| F5 | PATCH /vocabularies/{vocabularyName}/versions/{version} | unknown JSON properties | 400 | **The fuzz list misses a decision:** D52. **Contract problem:** `additionalProperties: false` is missing. |
| F6 | Stateful: POST /external-vocabularies | a multipart request with a JSON text as `description`, an empty `vocabularyFile`, and an extra part with an empty name (`name=""`) | 201 | Three separate points, below. |

The three points in F6:

- **The "object" description is not a bug.** A multipart text part is always a string, so the build is right to store it as text. This is a contract problem: the OpenAPI has no `encoding` for the multipart body, so Schemathesis treats the field as typed JSON.
- **The empty file is a spec gap (R-037).** D30 checks only Turtle syntax for external vocabularies, and an empty document is valid Turtle. For internal vocabularies, S6 rejects a file without statements. The decisions do not say whether an external vocabulary may be registered with no statements.
- **The part with an empty name may be a build bug.** D52 says any form field the schema does not define gives 400, and the build ignored this part without an error. Whether a part with no name counts as a "field" is not decided (R-038).

The build log already noted that the remaining ST201 and ST202 checks follow decided changes, and that the fuzz step needs those decisions to judge them. Adding D37, D38 and D52 to `fuzz-known.yaml` would leave only F6 as a finding.

## 4. Traceability

| Measure | Share of 85 live criteria |
|---|---|
| With a passing scenario (all its scenarios pass) | 84 (99%) |
| With a scenario at all | 85 (100%); 84 with a runnable one (SIMPL-18162/AC4 is `@wip`) |
| With code according to `build/work/TRACEABILITY.md` | 85 (100%) |

- **Criteria with nothing:** none. No live criterion lacks both code and a scenario.
- **SIMPL-18162/AC4** is the only criterion without a run scenario. It has code and an own unit test.
- **Out of the live set:** SIMPL-28680 (D61). SIMPL-393 is met by the generic upload (D1), and SIMPL-395 is replaced by the SIMPL-181xx stories.
- **Criterion numbering:** the build numbered unnumbered criteria in story order, the same convention the decisions use. `TRACEABILITY.md` groups some ids (for example "SIMPL-18180 AC2, AC3" and "SIMPL-18195 AC1 to AC3"). Read that way, every id has a code path.
- **Weak spot in the build's own evidence:** 15 UI criteria are listed with "by hand" as their only own test. Examples are SIMPL-18163/AC1, SIMPL-18167/AC1, SIMPL-18169/AC1, SIMPL-18176/AC2, SIMPL-18178/AC1 and SIMPL-18180/AC8. They pass in the independent UI suite, but the build has no automated test of its own for them.

## 5. Quality of the build

### Quality targets

There is no `Q` decision. No decision gives a response time, throughput, availability, RTO or RPO target, so `load.json` is a skip record. The table lists every target that exists, with where it comes from. For NFR-13, every target is either measured or marked as not measured.

| Target | Source | Target value | Measured | Verdict |
|---|---|---|---|---|
| Upload limit | D15, NFR-04 | 5 MB accepted, above it 413 | 413 above the limit passes; exactly 5 MB gives 503 after 10 s (store write takes 20.3 s) | **Not met** (section 3.1) |
| Store response timeout | D44 | 10 s per store call, then 503 with Retry-After 5 | 503 with Retry-After at 10 s, as decided | Met |
| Store and identity provider down | D44, SOA-05 | 503 on store loss; cached keys keep working | store_down 5 of 5; unit tests for the identity provider case | Met |
| Page and search limits | D22, D38, D56, D37 | page size 1 to 100, 100 search references, 20 aliases, 1000 characters | tested at and just above each limit; all pass | Met |
| Concurrent writes | D43 | 5 parallel versions give 2 to 6 once each | own test `parallelVersionCreationsAreSerialised`; suite scenario passes | Met (single instance only) |
| Health and graceful shutdown | NFR-07, MACH-08 | health UP, DEGRADED or DOWN; in-flight requests finish | health scenarios pass; shutdown not tested from outside | Partly measured |
| Accessibility | NFR-10 | WCAG 2.1 A and AA | 0 violations on 4 pages; 2 incomplete checks | Met, pending a manual contrast check |
| Response time | none (NFR-03) | no target | not load-tested; `QUALITY.md` gives list p50 11 ms, p95 15 ms, max 23 ms (100 calls); content p50 9 ms | Not measured against a target |
| Availability, RTO, RPO | NFR-06, set by the code writer, not the owner | 99.5% office hours, RTO 30 min, RPO 24 h | not measured; restore tested with 169 quads only | **Not measured** |
| Designed load (users, requests per second, data volume) | NFR-12 | none stated | none | Not measured; no target |
| Start time | finish line | start from clean state | 34 s until all four services are healthy | Measured, no target |

### Reuse

- **What `REUSE.md` says:** Keycloak, Nimbus (inside Spring Security), Fuseki, Jena RIOT and ARQ, keycloak-js, n3, Micrometer and Actuator are all reused. Nothing security-related or format-parsing is written by hand.
- **What step 70c found on the `REUSE-` rules:**
  - REUSE-01, REUSE-02, REUSE-04 and REUSE-09 are met.
  - REUSE-03 is partly met. The build wrote its own SPARQL HTTP client where Jena's HTTP query execution exists, its own JWK cache where Nimbus `JWKSourceBuilder` offers caching, and its own route table that repeats Spring MVC's mappings.
  - REUSE-05 and REUSE-07 are partly met. `REUSE.md` leaves out the catalogue entries `ui-components` (participant-fe-components), `participant-shared-code` and the circuit breaker pattern.
  - REUSE-06 is partly met. The circuit breaker is missing, some pattern names only label framework use, and error routing is built five ways (`RouteTable`, `MethodNotAllowedFilter`, `RequestRejectedHandler`, `ProblemErrorReportValve`, `ProblemErrorController`).

### Main design review findings (step 70c)

Verdicts on 153 review rules:

- MUST: 22 met, 20 partly, 7 not met, 3 not applicable.
- SHOULD: 28 met, 11 partly, 16 not met, 46 not applicable.

The main findings:

1. **Creator emails in INFO logs** on six write paths (`VocabularyService.java:108,143,173,197`, `ExternalVocabularyService.java:95,111`). This breaks CODE-32 and touches ARCH-20, OWT-A09 and OWASP-01. Run 5 had fixed this; run 6 brings it back.
2. **Single instance only.** The per-vocabulary locks live in memory, so ARCH-07 (MUST) and MACH-06 are not met. D43 allows this, and `DEVIATIONS.md` says so.
3. **Restore can empty the store.** `ops/restore.sh` runs `DROP ALL` and then loads through a 64 MB tmpfs. A realistic dataset (base64 content, about 7 MB per 5 MB upload) fails half-way. NFR-06 and OWT-A10 are partly met. This is the same storage choice that causes failure 3.1.
4. **Unbounded store calls per upload.** `ReferenceChecker.check` runs 2 or 3 SPARQL queries for each internal prefix, with no cap and while holding the lock. The warnings list is also uncapped. NFR-05 is partly met.
5. **Simpl conventions missing:**
   - no circuit breaker (CODE-46, ARCH-34);
   - no trace context (NFR-08);
   - ECS logs instead of the Simpl log wrapper (CODE-21, CODE-52), and no log aggregation (CODE-50, SEC-11);
   - participant-fe-components not used (UX-01, a MUST).

**Where 70c disagrees with the build's documents:**

- **The build calls these met or says nothing; 70c finds them partly or not met:**
  - CODE-32, ARCH-20 and OWT-A09: `QUALITY.md` implies clean logs.
  - NFR-05: `QUALITY.md` says resources are bounded.
  - NFR-06: the restore was "tested", but at 169 quads.
  - REUSE-05 and REUSE-07: covered above.
- **Rules left out of `DEVIATIONS.md`:** CODE-21, 33, 44, 46, 50, 52; ARCH-34; NFR-12; DATA-02, 03, 26, 27, 29; SEC-11; MACH-03; SEC-01; OWASP-05; OWT-A02.
- **Wrong levels in `DEVIATIONS.md`:** UX-01 is listed as SHOULD but is MUST, and the second ARCH-07 row is listed as SHOULD but is MUST.
- **Where 70c agrees with the self-report:** OWT-A04, ARCH-07, MACH-06, MACH-07, NFR-08, OPS-37, OWASP-10 and UX-01 are honestly reported as gaps.

### Own tests

- **Backend:** 61 tests, green in `mvn verify` with Spotless. JaCoCo: **87% lines, 67% branches**. That includes `TomcatLimitsIntegrationTest` on the real Tomcat connector and integration tests against embedded Fuseki.
- **UI:** 14 Vitest tests, 96% lines, but only on helpers (texts, errors, format, Turtle check, notifications). The views have no component or browser tests of their own.
- **`smoke.sh`:** passes.
- **Gap the own tests did not catch:** no own test stores a file at the 5 MB limit against a real store, which is what failure 3.1 needed.

### Mutation testing

Not run. There is no mutation score, so the coverage figures say what code runs, not how well it is checked.

### Deviations from the rules (`DEVIATIONS.md`)

All decisions are implemented as written. The only recorded decision deviation is D48 against API-46 (Belgif problem types). The rules not met, or met in part:

- API-46 (SHOULD), because of D48.
- OWT-A04 and OWASP-03 (MUST): no TLS and no encryption at rest (local stack).
- MACH-07 (MUST): test passwords in the realm file; Keycloak runs `start-dev` with H2.
- MACH-06 and ARCH-07: in-memory locks, single instance (D43).
- ARCH-07: Fuseki and Keycloak run as single containers.
- NFR-08: no trace context.
- OWASP-07 (MUST): 7 HIGH or CRITICAL findings in the Keycloak image, with reasons.
- OPS-06, OPS-33, OPS-35, OPS-37: no CI pipeline, SAST gate or merge gate.
- UX-01: no design system (listed as SHOULD, actually MUST).
- OWASP-10 (MUST): events are logged but not monitored.
- NFR-04 (MUST): no call-rate limit in the service; D46 puts it at the gateway.
- BLG-col-repres and BLG-api-matur: the wire format is fixed by D39.
- CODE-03 and CODE-04: marked not applicable (the UI is Vue, not Angular).

### Scan results (Trivy, HIGH and CRITICAL, orchestrator's own scan, 19 Nov 04:10)

| Target | CRITICAL | HIGH | What `SECURITY-SCAN.md` says |
|---|---|---|---|
| deps (`fs`) | 0 | 0 | 0 / 0 before and after |
| backend image `vocabulary-manager-be:1.0.0` | 0 | 0 | 0 / 0 |
| fuseki image `fuseki:6.2.0` | 0 | 0 | 0 / 0 |
| ui image `vocabulary-manager-ui:1.0.0` | 0 | 0 | fixed: 12 HIGH and 1 CRITICAL in Alpine packages removed with `apk upgrade` in the final stage |
| keycloak `26.8` | 1 | 6 | accepted, all without a usable fix (below) |

The Keycloak findings, all accepted with reasons in `SECURITY-SCAN.md`:

- CVE-2026-114938 (CRITICAL) and CVE-2026-91403 (HIGH) in OpenJDK: no Red Hat fix yet for the first, and Red Hat will not fix the second.
- CVE-2026-119373, 119374, 119379 and 119586 (HIGH) in sqlite-libs: no fixed package, and Keycloak does not use SQLite.
- CVE-2026-102554 (HIGH) in guava: fixed only in a later Keycloak release.

The build upgraded Keycloak from 26.7.5 to 26.8.2 (16 findings down to 7). The orchestrator's counts match the build's own scan for every target. Nothing is open in code or images built by this project.

## 6. New spec gaps

These are questions for the owners that steps 20 to 30 did not raise. Numbering continues after run 5's R-035, because `decisions.json` closes findings only up to R-022. Three questions from run 5 come back in this run without a decision:

- R-023 (Submit with field errors): step 45 raised it again as question 5. This run avoided it by changing the test (SR-04).
- R-025 (D36 against D47): raised again as step 45 question 6.
- R-030 (404 detail of POST /external-vocabularies/content): raised again as step 45 question 3.

### From the failures and the fuzzing

- **R-036. Browser size refusal (D13, D15, D37).** When the UI refuses a file over 5 MB before submit, is that a field error under "Vocabulary file" (D37), or the 413 detail text shown above the form (D15)? (Section 3.3.)
- **R-037. Empty external vocabulary.** May POST /external-vocabularies register a file with no statements? D30 checks syntax only. For internal vocabularies, S6 rejects an empty file. (F6.)
- **R-038. Multipart part without a name (D52).** Does a form part with an empty name count as an unknown field (400), or may it be ignored? (F6.)
- **R-039. Time budget for a maximum-size write (D15, D44, NFR-03).** D44 gives 10 s per store call, but no decision says how long a 5 MB create or new version may take end to end. Should a `Q` decision fix a response time for an upload at the limit, so that the storage design is chosen to meet it? (Section 3.1.)
- **R-040. Writes to different vocabularies (D43).** D43 says they are not serialised against each other, but the store allows one write at a time, so a large upload delays every other write. Is that acceptable, and how long may the delay be? (Section 3.2.)

### From the suite review (step 45)

- **R-041. Alias order and form (D30, D56).** Must GET and POST /external-vocabularies return `aliasUris` in the order sent, and in the form sent or normalised? The same question applies to `namespaceUri`.
- **R-042. URI validation (D41, D56).** Must a `namespaceUri`, `aliasUri` or `sourceUri` that is not an absolute URI give 400? This includes the body of POST /external-vocabularies/content.
- **R-043. UI text for a 404 on status change (D24, SIMPL-18174/AC2).** Which text does the list show when the status PATCH returns 404? D35 fixes "Vocabulary not found." only for the details page.
- **R-044. Paging links (D22).** Do the self, first, prev, next and last links keep the filter and sort parameters of the request?
- **R-045. Where the 5 MB limit applies (D15).** Is it on the file part or on the whole multipart request? The build applies it to the file part and caps the request at 12 MB.
- **R-046. List deprecation warning (D36).** Is the text visible in the list, or a tooltip on the icon?

### From the UI test writer (step 60)

- **R-047. Routes of the forms (D31).** D31 names the list and details routes only. Should it name the routes of the create and new version forms? The D18 "open the form URL directly" check depends on them.

### From the build log

- **R-048. Actuator paths on the API port (D44).** Which status should `/actuator/...` give on the API port? The build gives 401 without a token, as for any path.
- **R-049. Paths rejected by the firewall.** A path with an encoded slash or a control character is rejected before routing with 400 and no issues. Confirm, or fix the answer.
- **R-050. Non-standard methods (D17).** `QUERY` without a token gives 405, while `PUT` without a token gives 401, because the firewall runs before authentication. Should D17's "401 first" also cover non-standard methods?
- **R-051. Availability, RTO and RPO (NFR-06).** No decision gives them. The code writer chose 99.5% in office hours, RTO 30 minutes and RPO 24 hours. Owners should set or confirm these numbers.
- **R-052. Contract limits in the OpenAPI (D37, D38, D52, D56).** The decided limits (20 aliases, at least 1 entry per search array, field lengths, no unknown properties) are not in the OpenAPI file, which D39 does not let the writer edit. Should the contract be updated, so that fuzzing and clients see them? (F1 to F5.)

### From the design review (step 70c)

- **R-053. Creator in logs (D27).** Should logs carry the token subject instead of the email that D27 stores as `createdBy`?
- **R-054. Single instance (D43 against ARCH-07).** Should D43 require a store-side check, such as a conditional update on the latest version, so that more than one backend instance is safe?
- **R-055. UI component library (UX-01).** The build profile asks for a "plain component library". UX-01 (MUST) and the reuse catalogue name participant-fe-components. Which one wins? If it is the profile, a decision should waive UX-01.
- **R-056. Circuit breaker and log format.** Is a circuit breaker expected on the store and identity provider calls (CODE-46, ARCH-34)? Must the Simpl log wrapper be used, or are CODE-21 and CODE-52 waived for factory builds?
- **R-057. Keycloak mode.** Should the build profile turn on realm events (`eventsEnabled`, for OWT-A09), and run Keycloak in production mode or record dev mode as a decision?

## 7. Compared with earlier runs

| Run | Date | Suite | Result | Supply chain (own targets) |
|---|---|---|---|---|
| run1 | 27 Oct 2026 | its own suite | 74 of 74 API, 22 of 22 UI | n/a |
| run4 | 17 Nov 2026 | independent | 379 of 386 | 17 CRITICAL |
| run5 | 18 Nov 2026 | independent | 463 of 468 (99%); 4 UI failures | 0 CRITICAL, 0 HIGH |
| run6 | 19 Nov 2026 | independent, reviewed in step 45, plus fuzzing and design review | 428 of 432 (99.1%); 2 API and 1 UI failure | 0 CRITICAL, 0 HIGH |

What changed between run 5 and run 6:

**Fixed since run 5**

- Run 5's build bug is gone: the browser Turtle check now rejects N3-only syntax. "A syntax error is shown with its line when the file is chosen" passes.
- Run 5's test bug (expecting Submit disabled on field errors, R-023) was removed in step 45 (SR-04).

**New in run 6**

- **Upload at the limit.** The two API failures come from a regression in storing a 5 MB file within D44's 10 s. In run 5 all API cases passed.
- **Log privacy.** Run 5 logged the token subject. Run 6 logs creator emails again (design review, finding 1).

**Own tests are weaker than in run 5**

| Measure | Run 5 | Run 6 |
|---|---|---|
| Backend tests | 69 | 61 |
| JaCoCo line coverage | 91.5% | 87% |
| JaCoCo branch coverage | 78.2% | 67% |
| UI tests | 19 | 14 |

**Supply chain**

- Own targets stay at 0 HIGH and 0 CRITICAL.
- Keycloak 26.8.2 now shows 1 CRITICAL (CVE-2026-114938, OpenJDK, not fixed by Red Hat yet) next to the same 6 HIGH as run 5.

**New checks in this run**

- The step 45 suite review found 17 suite issues before the run (4 blockers), and all were fixed.
- Fuzzing and the design review were added. Together they surfaced issues the suite alone did not:
  - the uncapped reference fan-out;
  - the fragile restore;
  - the email logging;
  - three decisions missing from the fuzz list.

**Engine**

- `verify.py` needs to merge results by feature and scenario name, or rerun all features after step 60 edits. Otherwise feature file edits in step 60 produce phantom skipped scenarios and understate the criteria and decision scores, as they did in this run.
