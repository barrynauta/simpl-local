# Final report: vocabulary-manager, run 10 (20 Nov 2026)

Project Simpl-Open, component vocabulary-manager, engine 348729b, 405 rules in scope. Compose project `vm-factory-run10`.

Sources: `RUN.json`; `report/` (`verification.md` and `verification.json` of 70a, `scan-summary.md`, `load.json`, `fuzz-summary.md`, `fuzz.json`, `fuzz/console.txt`, `design-review.md` of 70c); `tests/work/COVERAGE.md` and the behave reports in `tests/work/reports/`; `tests/review/SUITE-REVIEW.md`; from the build: `TRACEABILITY.md`, `DEVIATIONS.md`, `REUSE.md`, `QUALITY.md`, `SECURITY-SCAN.md`, `BUILD-LOG.md` and `reports/jacoco.csv`. To explain the measurement gap and the UI failure I also read `logs/ui_60.md`, `tests/work/run.sh`, the file times in `tests/work/reports/`, two lines of `ui/src/views/VocabularyDetails.vue`, and the decisions D20, D36, D64, D91, D99, D112 and Q1 to Q6 in `decisions.json`.

## 1. In short

**What was built.** The build has four parts:

- a Java 21 backend on Spring Boot 4.1.3, run as two replicas (D102);
- Apache Jena Fuseki 6.2.0 as the store;
- Keycloak 26.8.2 with the realms of E1;
- a Vue 3 UI behind nginx, which also proxies `/v1`.

It also has an Alpine-based `store-tools` image for backup and restore. The code writer finished in one session: 256 turns, 71 minutes, USD 29.75. It reached the finish line:

- the stack starts from a clean state;
- `smoke.sh` passes;
- `mvn verify` is green, with JaCoCo line coverage 96.1 % and branch coverage 86.9 %;
- the Trivy findings are fixed or given a reason;
- the stack was left running.

The whole run cost about USD 55 before this report.

**How it scored, and the main caveat.** The verification counts **72 of 73 scenarios passed**. That number covers only part of the suite. The suite has 533 scenarios. Of these:

- 49 are UI scenarios and ran: 48 passed.
- 9 are store-down scenarios and ran: 9 passed.
- 3 are `@wip` and never run.
- 472 are API scenarios. Only 15 of them have a result, all from `protocol.feature`.

The other **457 API scenarios have no recorded result**, in 10 feature files. The suite step (12:15 to 12:17) wrote `reports/api.json`. The file now has a time of 12:29, which falls inside the UI step (12:17 to 12:31), and it holds only `protocol.feature`. Most likely the UI step re-ran `./run.sh api` on one feature file, and that run replaced the full API report. `logs/ui_60.md` says "The API run and the dry run are unaffected", but the file times show otherwise. No other copy of the full result exists.

So the API outcome of this build was **not measured**. All criterion and decision figures from 70a are low because of this gap. They do not show what the build does.

**What was measured:**

- **UI:** 48 of 49 pass. The one failure is the D36 link text.
- **Store-down:** 9 of 9 pass.
- **Accessibility:** 0 WCAG 2.1 AA violations on the four pages (Q6).
- **Load at the full D99 shape:**
  - The seed was 1000 vocabularies with 20 versions each.
  - Throughput was met: 20.0 reads/s and 1.0 writes/s with 0 errors.
  - Write p95 values were met.
  - The read p95 target was **missed on one operation**: `getVocabularies` p95 was 320 ms against a target of 300 ms.
- **Fuzzing:** 0 server errors and 7 findings. All 7 are decided rejections (D91, D20) whose wording the known-failure list does not match. They are not build bugs.
- **Trivy:** 0 HIGH and 0 CRITICAL in every own target. Keycloak has 6 HIGH and 0 CRITICAL, the same as the build's own scan says.
- **Design review:** 29 of 52 MUST rules met. The main points are hand-written token claim checks (REUSE-01), a second request layer beside Spring MVC (REUSE-06 and REUSE-07), a circuit breaker that does not count store 5xx (MSV-06), and CODE-11 not met and not recorded as a deviation.

**What it means.** On everything that was measured, the build is at least as good as run 9 and better on scans and coverage. Q1 slipped on the list read. Before this run counts as a result, the API suite has to be run again against the running stack (`./run.sh api`, about 1 to 2 minutes). Section 6 adds a factory fix so the UI step cannot overwrite the API report.

## 2. Results

### Per feature

| Feature | Planned | Ran | Passed | Failed | No result |
|---|---|---|---|---|---|
| Cross-cutting protocol rules | 15 | 15 | 15 | 0 | 0 |
| Create, new version, status change and access in the UI (ui) | 23 | 23 | 23 | 0 | 0 |
| Vocabulary list and Vocabulary details in the UI (ui) | 26 | 26 | 25 | 1 | 0 |
| Behaviour when the vocabulary store is unreachable (store_down) | 9 | 9 | 9 | 0 | 0 |
| List vocabularies with filters, sorting and paging | 63 | 0 | | | 63 |
| Role-based access control on the Vocabulary Management API | 78 | 0 | | | 78 |
| Register, read and remove external vocabularies | 57 | 0 | | | 57 |
| Search vocabularies and external vocabularies | 49 | 0 | | | 49 |
| Syntax and semantic validation of uploaded vocabulary content | 47 | 0 | | | 47 |
| Change the vocabulary status and the version status | 45 | 0 | | | 45 |
| Read vocabulary versions, the version history and the content | 45 | 0 | | | 45 |
| Create a vocabulary through the API | 44 | 0 | | | 44 |
| Create a new version of a vocabulary through the API | 29 | 0 | | | 29 |
| Criteria and targets that cannot be checked from the API (wip) | 3 | 0 | | | 3 (never run) |
| **Total** | **533** | **73** | **72** | **1** | **460** |

### Per tag kind

| Tag kind | Ran | Passed | Failed | Note |
|---|---|---|---|---|
| API (default) | 15 of 472 | 15 | 0 | 457 have no result (report overwritten) |
| `@ui` | 49 | 48 | 1 | Playwright, fresh context each |
| `@a11y` (within ui) | 4 | 4 | 0 | 0 violations (Q6) |
| `@store_down` | 9 | 9 | 0 | port 8199 |
| `@wip` | 0 of 3 | | | never run by design |
| Rule tags | 11 rules | 11 | 0 | API-44, API-75, ARCH-35, BLG-err-problem, BLG-err-sanit, BLG-hlth-res, BLG-meth-http, MACH-08, NFR-07, NFR-08, NFR-10 |

### Criteria and decisions

| Measure | Measured (70a) | By suite design (COVERAGE.md) |
|---|---|---|
| Live criteria with a scenario | 44 of 85 (52 %) | 85 of 85 (84 runnable, SIMPL-18162/AC4 only `@wip`) |
| Live criteria with all scenarios passing | 43 of 85 (51 %) | not measurable this run |
| Decisions with a scenario | 48 of 130 (37 %) | 99 of 130 (31 have a stated reason for no scenario) |
| Decisions with all scenarios passing | 46 of 130 (35 %) | not measurable this run |

The only criterion with a failing scenario is SIMPL-18559/AC1. The decisions with a failing scenario are D36 and D64, both from the same scenario.

## 3. Failures

### Failing scenario

**`ui_list_and_details.feature:154`, "A deprecated version links to the latest ACTIVE version"** (SIMPL-18559/AC1, D36, D64).

- **Step:** `Then the version row "1" shows the warning "This version is deprecated." and a link "Latest ACTIVE version: 3"`.
- **Result:** "no link named 'Latest ACTIVE version: 3' in the row; links: ['3']".
- **Cause: build bug.** D36 says the text "'Latest ACTIVE version: <n>' as a link". In `VocabularyDetails.vue` (lines 181 and 216) the words "Latest ACTIVE version:" are plain text and only the number is the link. The warning text, the link target and the D64 rule (never link to itself) are right.
- Step 45 did not flag this scenario. Step 60 kept it as written, and that is the right call.
- The decision is clear enough that the build should follow it. One owner check is still useful (gap 1 in section 6).

### Missing results (not a failure of the build)

The 457 API scenarios without a result are a factory defect, not a build or spec finding (see section 1). They must be re-run before any API pass rate is reported.

### Fuzz findings

Schemathesis ran with seed 1 and 30 examples per operation against the published contract. There were 20 operations, 14 failed checks, 7 explained by the known list, 7 findings and **0 server errors**.

| Operation | Request (reproduction) | Build answer | Cause |
|---|---|---|---|
| POST /vocabularies/searches | `items[0].version` = 9223372036854775808 | 400 "'version' must be a ..." | Decision the fuzz list misses: D91 (32-bit, 1 to 2147483647) |
| GET /content/{vocabularyName}/versions/{version} | version 9223372036854775808 | 400 | D91, D112, same |
| GET /vocabularies/{vocabularyName}/versions/{version} | version 9223372036854775808 | 400 | D91, same |
| PATCH /vocabularies/{vocabularyName}/versions/{version} | version 9223372036854775808 | 400 | D91, same |
| GET /external-vocabularies | `pageSize=9223372036854775808` | 400 "'pageSize' must be a ..." | D91 and D22, same |
| GET /vocabularies/{vocabularyName}/versions | `pageSize=9223372036854775808` | 400 | D91 and D22, same |
| GET /vocabularies | `creationDate=0000-00-00` | 400 "'creationDate' is no(t a valid date)" | Decision the fuzz list misses: D20 ("a value matching the pattern but not a valid date returns 400") |

**What lies behind these findings.**

- The build behaves as decided in all seven cases.
- `fuzz-known.yaml` matches on the server's wording. Its D91 entry expects "must be an integer", and its D22, D49 and D20 entries expect "Query parameter 'pag", "Path parameter 'vers" and "Query parameter 'cre". This build words the issues differently: "'version' must be a ...", "'creationDate' is no...". Run 6 hit the same problem.
- The root cause is a **contract problem in the published OpenAPI** (`vocabulary_openapi-tier1.yaml`). It gives integers no `format: int32` and no maximum, and its date pattern accepts `0000-00-00`. The build's `openapi-decided.yaml` adds these, as D91 and D116 require.
- The fuzz run therefore tests against a contract that the decisions have already tightened.
- These are not scenarios, so step 45 had nothing to flag.

## 4. Traceability

| Measure | Value |
|---|---|
| Live criteria with a passing scenario (measured) | 43 of 85 (51 %) |
| Live criteria with a scenario at all | 44 of 85 measured (52 %); 85 of 85 by suite design |
| Live criteria with code according to `TRACEABILITY.md` | 85 of 85 (100 %); SIMPL-395 and SIMPL-28680 are out of the live set (D1, D61) |

**Criteria with nothing.** No criterion has neither code nor a scenario.

- **SIMPL-18162/AC4** (internal error gives 500) has only a `@wip` scenario. The build covers it with `ProblemHandler` and its own `SparqlStoreTest` and `StoreDownApiTest`.
- **SIMPL-18184/AC4** has only a UI scenario. That scenario passed.

**Criteria with code and a planned scenario but no measured result** (all in the lost API report), 41 in total:

- SIMPL-18162/AC1 to AC4
- SIMPL-18164/AC1 to AC4
- SIMPL-18166/AC1, AC2
- SIMPL-18168/AC1, AC2
- SIMPL-18170/AC1 to AC3
- SIMPL-18173/AC1 to AC6
- SIMPL-18175/AC1 to AC3
- SIMPL-18181/AC1, AC2
- SIMPL-18183/AC1, AC2
- SIMPL-18184/AC1 to AC3
- SIMPL-18187/AC1, AC2
- SIMPL-18190/AC1, AC2
- SIMPL-18194/AC1, AC2
- SIMPL-18196/AC1 to AC4

**Decisions with no scenario on purpose.** There are 31, each with a reason in `COVERAGE.md`. Examples:

- D2 and D117: model only.
- D80, D96 and D105: logs, which the API cannot observe.
- D83, D98 and D107: deployment.
- D93, D94, D95 and D97: review only.
- Q1, Q4 and D99: the load test.
- Q5: build metrics.

## 5. Quality of the build

### Quality targets (NFR-13)

| Target | Value | Measured | Verdict |
|---|---|---|---|
| Q1 read p95 (per operation) | at most 300 ms | `getVocabularies` 320 ms (p99 760 ms); every other read 32 to 40 ms | **not met** (one operation) |
| Q1 write p95, files up to 1 MB | at most 3 s | create 560 ms, new version 770 ms (p99 2.4 s) | met |
| Q1 5 MB write (D71, run apart, D99) | at most 30 s | p95 2.25 s (5 requests) | met |
| Q1 throughput (D99, offered rate, 5 min) | 20 reads/s and 1 write/s, at least 95 % achieved, 0 errors | 20.0 and 1.0, 0 errors in 7 301 requests | met |
| Q4 design load | 1000 vocabularies by 20 versions, 2 replicas (D102) | seeded by the load step (499 s), 2 replicas | met as a load shape |
| Q2 limits at and above each limit | each limit enforced with a defined error | build: own tests per limit (`QUALITY.md`); the suite's at-limit scenarios (added after SR-11, SR-12) are in the lost API report | **not measured** this run |
| Q3 backup and restore round trip | tested every build; RTO 30 min, RPO 24 h | build: `backup-restore-test.sh` passed (141 quads); availability not measurable on a local stack | met by the build's own evidence, not checked again in step 70 |
| Q5 line and branch coverage | at least 80 % and 60 % | 96.1 % and 86.9 % (`jacoco.csv`) | met |
| Q5 no HIGH or CRITICAL in own targets | 0 | 0 in deps, backend, fuseki, ui-proxy, store-tools | met |
| Q5 reference check, one store query per upload | 1 | one UNION query (`VocabularyRepository#referenceData`), but with `LIMIT 200000` (70c NFR-05) | met, with a caveat |
| Q6 WCAG 2.1 AA, keyboard | 0 axe violations on 4 pages | 0 violations; all actions reachable by keyboard | met (note: the Change log field has no "required" markup, which axe does not flag) |
| Mutation score | | not run | not measured |

The build's own `QUALITY.md` reports Q1 as "not fully shown", from a smaller load: 300 by 3 seed, 16 reads/s, 90 s, read p95 158 ms. The orchestrator's load step has now run the D99 shape in full. Under the full seed the paged list is the one read that goes over target.

### Reuse

`REUSE.md` covers every catalogue entry with a reason. The 70c verdicts were 4 met (REUSE-03, 04, 05, 09) and 4 partly met:

- **REUSE-01 (MUST), partly.** Nimbus checks only the signature. `TokenValidator.checkClaims` (issuer, audience, expiry, nbf, subject) and `RealmKeyProvider` (key fetch and cache) are own code. D108 exempts only the fetch timing. `REUSE.md` wrongly says the claim checks come from Nimbus.
- **REUSE-02, partly.** `QueryParameters.parse` splits the raw query string by hand. `MultipartForm.classify` matches exception class names as strings.
- **REUSE-06, partly.** About 800 lines (`ApiOperation`, `RoutingFilter`, `MediaTypeFilter`, `QueryParameters`) duplicate Spring MVC routing and content negotiation. The "retry with back-off" listed for the write loop has no back-off.
- **REUSE-07 (MUST), partly.** There is no `REUSE.md` row for routing, the 404, 405, 406 and 415 checks, or content negotiation, although D93 asks for one above 100 lines.

### Main 70c findings and disagreements

- MUST rules: 29 met, 17 partly, 4 not met, 2 not applicable. SHOULD rules: 60 met, 17 partly, 11 not met, 13 not applicable.
- **Resilience (MSV-06, ARCH-34, CODE-46).**
  - The breaker ignores `StoreFailureException`. An overloaded Fuseki that answers 5xx never opens it, so callers get 500 instead of 503.
  - `GET /v1/health` is public and calls `keys.refresh()` outside the D88 10 s cooldown, so anyone can trigger calls to Keycloak.
- **CODE-11 (MUST) not met and not recorded.** No class depends on an abstraction.
- **Not met MUST rules the build does record:** OWT-A04 (no TLS, no encryption at rest), UX-01, OPS-37 and ARCH-07.
- **Logging and alerting (OWT-A09, OWASP-10).** A 401 for a missing token is not logged, and nothing raises an alert.
- **CODE-24.** WARN is logged on a read that is retried and then succeeds, and store 5xx is logged twice.
- **Where 70c disagrees with the build's own documents:**
  - the `REUSE.md` token row;
  - the missing D93 row;
  - the "back-off" pattern row;
  - eight rules missing from `DEVIATIONS.md` (see below);
  - the D88 cooldown as presented in `architecture.md` and `QUALITY.md`;
  - "bounded resources": `store-tools` has no limits and the reference query allows 200 000 rows.
- **Where 70c agrees with the build:** Q1, D98, Keycloak brute-force protection, OWT-A04, UX-01, OPS-37 and ARCH-07.

### Own tests

- Backend: `mvn verify` green, JaCoCo line 96.1 % (2 097 of 2 181) and branch 86.9 % (692 of 796), with a gate at 80 and 60.
- UI: Vitest, ESLint, Prettier and vue-tsc run in `npm run build`.
- Spotless runs in `mvn verify`.
- No mutation testing.

### Deviations recorded by the build (`DEVIATIONS.md`)

- **Decisions:**
  - D70 and D52: a part with an empty name is dropped by Tomcat, so it does not give the decided 400.
  - D99 and Q1: the build's own load test was smaller than the decided shape.
  - D98: the management port is also on the `front` network.
  - D48 against API-46: Belgif problem types, as decided.
- **Rules:**
  - API-46
  - CODE-21, CODE-52, SEC-11 (D105)
  - CODE-39 (incomplete `@throws`)
  - OPS-06, OPS-33, OPS-35, OPS-37 (no pipeline, no SAST)
  - OWT-A04, OWASP-03 (HTTP only, no encryption at rest)
  - OWT-A07 (no failed-token rate limit in the service, D46)
  - UX-01 (no Simpl design system, E6)
  - SEC-12 (D17)
  - ARCH-07 (single Fuseki)
- **Missing according to 70c:**
  - CODE-11 (MUST)
  - CODE-33, CODE-44 (overridden by D104)
  - SEC-13 (D17)
  - API-02
  - API-35 (the UI sends no Accept header)
  - ARCH-27 (D45)
  - the alerting half of OWT-A09

### Scan results

| Target | CRITICAL | HIGH | Build's `SECURITY-SCAN.md` |
|---|---|---|---|
| deps (`pom.xml`, `package-lock.json`) | 0 | 0 | 0 / 0, current versions chosen from the start |
| backend image | 0 | 0 | 0 / 0 |
| fuseki image | 0 | 0 | 0 / 0, jar SHA-256 checked |
| ui-proxy image | 0 | 0 | 0 / 0 |
| store-tools image | 0 | 0 | new image, 0 / 0; replaced `curlimages/curl:8.19.0` (46 HIGH, 1 CRITICAL) |
| quay.io/keycloak/keycloak:26.8.2 | 0 | 6 | 6 / 0, accepted with reasons: openjdk CVE-2026-91403 and four sqlite-libs CVEs have no fixed package (sqlite not used); Guava CVE-2026-102554 sits inside the vendor product, waits for a Keycloak release |

The independent scan and the build's scan agree on every target. Build-stage images were not scanned, which is acceptable because they do not run in the stack.

## 6. New spec gaps

These are questions for the owners that steps 20 to 30 did not raise. The last finding id in the decisions is R-124, so these could become R-125 to R-148.

1. **D36 link extent.** Is the whole text "Latest ACTIVE version: <n>" the link, or may only the number be the link? The suite reads the whole text. (Source: UI failure.)
2. **D43 against D92.** D43's test sends 5 parallel new versions, but D92 refuses the fifth upload on one instance with 503. Should the test use at most 4 per instance, or should D92 queue briefly? (Build log 1, suite review question 4.)
3. **D98 with replicas.** Docker cannot keep a container port off one network. Is the management port on the front network acceptable when it is published nowhere? If not, which alternative is wanted (for example `management.server.address`)? (Build log 2, 70c finding 10.)
4. **D70, part with an empty name.** The servlet container drops it before the application sees it. Should the 400 be dropped from D70, or is a custom multipart parser acceptable, overriding REUSE-02? (Build log 3.)
5. **Front proxy responses.** nginx answers some requests before the service does: TRACE, malformed requests, and bodies above the proxy limit. Must the proxy return the decided problem bodies (D67, D48), and what is the proxy body limit relative to D76? (Build log 4.)
6. **Content-Security-Policy.** Should the UI CSP be decided? Should UI tests inject axe through `page.evaluate` or with `bypass_csp`? (Build log 5.)
7. **Base IRI for external vocabularies.** D57 covers internal uploads only. Is the namespace URI the base for relative IRIs in an external file? (Build log 6.)
8. **Size of the validation report.** "All errors are returned" has no bound. Is a cap of 1000 issues acceptable, and how is the cut signalled? (Build log 7.)
9. **HEAD and OPTIONS** on declared paths. The build answers 405 with `Allow`. Confirm, or decide 200 and 204. (Build log 8.)
10. **Accept ranges (D63).** Should `application/*` be accepted like `*/*`? (Build log 9.)
11. **Status sort order (D23).** Is ACTIVE before DEPRECATED in alphabetical order the intended order, or a business order? (Build log 10.)
12. **D88 against D44 after a failed key fetch.** Inside the 10 s window after a failed fetch, does an unknown key id give 401 or 503? (Build log 11.)
13. **Date-time offsets (D20).** Offsets beyond plus or minus 18:00 match the pattern but are not valid. Confirm 400. (Build log 12.)
14. **Secrets for the raw start command.** The build profile's start command needs passwords that `stack.sh` generates. Should the profile name `./stack.sh up` as the start command, or name the `.env` file? (Build log 13.)
15. **D108 scope.** Should claim checks (issuer, audience, expiry, nbf) be required to use Nimbus `DefaultJWTClaimsVerifier` or Spring `JwtValidators`, with own code only for the fetch timing? (70c finding 1.)
16. **D93, routing and content negotiation.** Must the build use Spring MVC mappings with produces and consumes, plus an ordering test for 404, 405, 406 and 415? If not, must `REUSE.md` name the framework alternative not taken? Must query parameters be bound by the framework? (70c finding 2.)
17. **D103 failure classes.** Does a store 5xx count as a circuit-breaker failure (503 to the caller)? May an unauthenticated endpoint such as `/v1/health` trigger outbound calls outside the D88 cooldown? (70c finding 3.)
18. **CODE-11 for this stack.** Should the store and repository ports have interfaces, or is CODE-11 a recorded deviation? (70c finding 5.)
19. **Q1 read p95: per operation or across the read mix?** The paged list misses at 320 ms at the full seed; every other read is at most 40 ms. Is the list held to the same 300 ms at 20 000 versions? (Load result.)
20. **D38 against D68.** For an empty `namespaceUris` or `aliasUris` array, is the issue named after the array or `body`? (Suite review question 1.)
21. **D52, repeated query parameter.** Is the answer exactly one issue with `in: query` and the parameter name? (Suite review question 2.)
22. **D84 against D77, D79 and D55.** For a known method not declared on a declared path (for example PUT /vocabularies), is the answer 405 or 404 with a token, and 405 or 401 without one? (Suite review question 3.)
23. **D65 rule S1.** Is "the literal as written" with quotes and the language or datatype tag, or the lexical form only? (Suite review question 5.)
24. **Required markers in the UI.** Must the Change log field (and other required fields) carry required markup for screen readers, beyond Q6's axe check? (Step 60 observation.)

**Factory fixes (not for the owners):**

- Write UI-step and ad hoc runs to separate report files, or make `run.sh` refuse to overwrite `reports/api.json` with a filtered run.
- Re-run `./run.sh api` for run 10 before the result is used.
- Match `fuzz-known.yaml` on the issue `name` and the decision, not on the server's wording. Or fuzz against `openapi-decided.yaml` as well.
- Add 70c finding 4 to the writer brief: every decision that overrides a rule (D104, D17, D45) gets a `DEVIATIONS.md` row.

## 7. Compared with earlier runs

| Run | Scenarios | Q1 | Trivy (own targets; Keycloak) | Coverage line / branch | Notes |
|---|---|---|---|---|---|
| run 10 (20 Nov) | 72 of 73 ran (533 planned; 457 API results lost) | throughput met; `getVocabularies` p95 320 ms, so read p95 missed | 0 / 0; Keycloak 0 CRITICAL, 6 HIGH | 96.1 % / 86.9 % | 1 UI failure (D36 link); 7 fuzz findings (wording only) |
| run 9 (19 Nov) | 476 of 479 | met in full at D99 shape, worst read p95 150 ms | 0 / 0; Keycloak 1 CRITICAL, 6 HIGH | 92.0 % / 77.9 % | 2 UI failures (now D100, D101); 0 fuzz findings |
| run 8 (19 Nov) | 463 of 465 | throughput missed (now D99) | | | 1 UI failure on the report role (now D90) |
| run 7 (19 Nov) | 420 of 424 | met at Q4 design load | | | design review gaps decided as D82 to D87 |
| run 6 (19 Nov) | 428 of 432 | | | | first orchestrated run; 5 MB store write failures (now D71) |
| run 5 (18 Nov) | 463 of 468 | | 0 CRITICAL in own targets | | Spring Boot 4.1.2 |
| run 4 (17 Nov) | 379 of 386 | | 17 CRITICAL | | |
| run 1 (27 Oct) | 74 of 74 API, 22 of 22 UI (own suite) | | | | |

**What changed since run 9:**

- **Better:**
  - The two run 9 UI failures are fixed: D100 and D101 each now have a passing scenario.
  - The Keycloak CRITICAL is gone.
  - Own coverage is up by about 4 points (line) and 9 points (branch).
  - The code writer finished without hitting the usage limit.
- **Worse:**
  - The list read p95 went from at most 150 ms to 320 ms at the same seed. That is enough to miss Q1.
  - The API suite result was lost, so this run cannot be compared with run 9 on API behaviour.
- **Unchanged in kind:**
  - The fuzz "findings" are a wording mismatch with `fuzz-known.yaml`, the same class of problem as in run 6.
  - The decision set grew from 112 (up to D99) to 130 (up to D117). The suite still covers 99 of the 130 decisions with scenarios by design.

## Note added after the run (20 Nov 2026)

Section 1 is right that the UI step replaced the full API report. The API suite was run again by hand against the same running stack (`./run.sh api`, after the load test and fuzzing), and `verify.py` was run again. Result: 470 of 472 API scenarios pass; with UI and store-down, **527 of 530** (99 %); 83 of 85 live criteria and 90 of 130 decisions have all their scenarios passing. The two API failures are build bugs:

- "A multipart part with an empty name is an unknown field" (D70, D52): the build answers 201 instead of 400.
- "A multipart request of exactly 6 MB is not refused for its size" (D76): the build answers 400 "The multipart request body could not be read." instead of reading the request and reporting the field issue.

The engine now restores the suite step's reports after the UI writer, so this cannot happen again (factory fix).
