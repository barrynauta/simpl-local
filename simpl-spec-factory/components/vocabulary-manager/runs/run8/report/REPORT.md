# Final report: vocabulary-manager, run 8 (19 Nov 2026)

Project Simpl-Open, component vocabulary-manager, engine c671dab, 405 rules in scope. Sources: `RUN.json`; `report/` (verification of 70a, `scan-summary.md` and the Trivy files, `load.json`, `fuzz-summary.md` with `fuzz.json` and `fuzz/console.txt`, `design-review.md` of 70c); `tests/work/COVERAGE.md` and the behave reports in `tests/work/reports/`; `tests/review/SUITE-REVIEW.md`; and from the build `TRACEABILITY.md`, `DEVIATIONS.md`, `REUSE.md`, `QUALITY.md`, `SECURITY-SCAN.md` and `BUILD-LOG.md`. To explain the one failure I read one line of `ui/src/views/VocabularyForm.vue`. To explain the verification numbers I read `spec-factory/pipeline/verify.py`.

## 1. In short

**What was built.** The stack runs under compose project `vm-factory-run8` and has four parts:

- a Java 21 backend on Spring Boot 4.1.2;
- Apache Jena Fuseki 6.2.0 as the store, with three store users (D83);
- Keycloak 26.8.2 with the realms of E1;
- a Vue 3 UI behind nginx.

The code writer of step 50 hit the usage limit partway through (`RUN.json`: "You've hit your 5-hour limit"). A second attempt kept the code it found and added everything that was missing: tests, docs, ops scripts and reports. It then reached the finish line:

- the stack starts;
- `smoke.sh` passes;
- `mvn verify` is green with JaCoCo;
- Trivy ran, and every open finding has a reason;
- the stack was left running.

**The step 70a figures are wrong.** `verification.md` reports **50 of 465 passed**, with 414 skipped. That is a defect in the pipeline, not in the build. `verify.py` reads every `*.json` file in `tests/work/reports/`, and that folder also holds `dry-run.json`. A behave dry run gives every scenario the status `untested`. The merge in `verify.py` lets any status other than `skipped` overwrite the earlier one (line 56). So the dry run replaced all 413 API passes with `untested`. Only the store-down and UI files, which come later in sort order, put their results back.

I recomputed the matrix with the same merge rule over `api.json`, `store-down.json` and `ui.json` only. All numbers below come from that recomputation.

**How it scored.**

- **463 of 465** scenario cases passed (99.6%). 1 failed. 1 is `@wip` and does not run.
- All 85 live criteria have a scenario. 78 have all their scenarios passing.
- 83 of 100 decisions have a scenario. 80 have all their scenarios passing.
- Every Q1 latency target was met, with 0 errors in 5575 requests. The throughput target was missed (17.9 reads and 0.70 writes per second).
- The independent Trivy scan finds 0 HIGH and 0 CRITICAL in every own target. The Keycloak vendor image has 1 CRITICAL and 6 HIGH, the same as in the build's own `SECURITY-SCAN.md`.
- axe found 0 WCAG 2.1 AA violations on the four pages checked (Q6).

**What it means.** The functional behaviour follows the decisions almost completely. The one failure is a build bug against D75: the validation report has the wrong ARIA role. The design review confirms that the five gaps of run 7 are now closed in the build (D82 to D87). The risks that remain are quality risks:

- request validation that is built by hand;
- SPARQL queries built from strings;
- upload memory that is not bounded against concurrency;
- no lint or format gates and no own UI tests (OPS-37 is the one MUST that is not met);
- no TLS.

## 2. Results

### Scenarios per feature

| Feature | Passed | Failed | Skipped |
|---|---|---|---|
| Authentication and role-based access control on every operation | 112 | 0 | 0 |
| Read vocabulary versions and download vocabulary content | 48 | 0 | 0 |
| List, filter, sort and page vocabularies through the API | 47 | 0 | 1 (`@wip`) |
| Register, read and remove external vocabularies | 44 | 0 | 0 |
| Search vocabularies and external vocabularies | 36 | 0 | 0 |
| Create a vocabulary through the API | 34 | 0 | 0 |
| Create a new version of a vocabulary through the API | 27 | 0 | 0 |
| Change the vocabulary status and a version status through the API | 23 | 0 | 0 |
| Validation of vocabulary content on create and new version | 19 | 0 | 0 |
| Upload limits and the order of checks on write operations | 13 | 0 | 0 |
| References to internal vocabularies | 10 | 0 | 0 |
| The store is unreachable | 5 | 0 | 0 |
| Vocabulary management UI | 45 | **1** | 0 |
| **Total** | **463** | **1** | **1** |

### Scenarios per run mode

| Run mode | Passed |
|---|---|
| API | 413 of 413 |
| Store down | 5 of 5 |
| UI (Playwright) | 45 of 46 |
| `@wip` | 0 of 1 (skipped by design) |

### Scenarios per tag kind

| Tag kind | Passed | Failed | Skipped |
|---|---|---|---|
| `@negative` | 272 | 0 | 1 |
| no kind tag (positive paths) | 187 | 1 | 0 |
| `@a11y` | 4 | 0 | 0 |

**Rules.** 26 rules are tested, and every scenario with a rule tag passes. The largest are SEC-05 (56), API-46 (41), NFR-04 (35), OWASP-02 (30), OWASP-08 (28), BLG-req-valid (27), OWT-A07 (25), BLG-err-problem (22) and API-47 (19).

### Criteria

- 85 of 85 live criteria have a scenario.
- 78 of 85 have all their scenarios passing.
- 82 of 85 have at least one passing scenario.

These 7 criteria are not all green:

- **SIMPL-18162/AC4** (500 on an internal error). Its only scenario is `@wip`. COVERAGE.md says a black-box test cannot provoke a 500. This was raised before as R-066.
- **SIMPL-18180/AC2** and **SIMPL-18193/AC2**. Their only scenario is the failing UI scenario.
- **SIMPL-18180/AC6, SIMPL-18180/AC7, SIMPL-18182/AC1** and **SIMPL-18182/AC3**. Each has one failing scenario (the same UI scenario), and the rest pass.

### Decisions

- 83 of 100 decisions have a scenario, and 80 have all their scenarios passing.
- **D14** (6 of 7) and **D75** (3 of 4) are not all green because of the UI failure.
- **D24** (27 of 28) is not all green because of the `@wip` scenario.
- 17 decisions have no scenario: D2, D39, D61, D79, D80, D81, D83, D85, D87, E1, E2, E3, E4, E6, Q3, Q4 and Q5. These are scope, setup, contract-process, store-internal or quality decisions that a black-box scenario cannot reach. Q4 was checked by the load test, Q3 by the build's backup and restore test, Q5 by JaCoCo and Trivy, and D83, D85 and D87 by the design review and the build's own tests.

## 3. Failures

### Failing scenario

**"Semantic errors on submit appear in the validation report and the form stays open"** (`features/ui/vocabulary_ui.feature:264`; tags SIMPL-18180/AC2, AC6, AC7, SIMPL-18182/AC1, AC3, SIMPL-18193/AC2, D14, D75).

- **What happens.** The step looks for `get_by_role("region", name="Validation report")` and finds nothing. The build renders `<section aria-label="Validation report" role="alert">` (`VocabularyForm.vue:166`). The explicit role `alert` replaces the implicit role `region` of a labelled section, so no region with that name exists.
- **What is right.** The report content is correct: three items, each with the decided D9 detail and its location.
- **Cause: a build bug.** D75 says "the validation report as a region named 'Validation report'". The build differs from that.
- **A small ambiguity in D75.** The same decision says "error messages with role alert", and the report is an error message. The build probably tried to meet both rules on one element. A region that contains an alert, or a region with `aria-live`, would meet both. See R-096.
- **Step 45 did not flag this scenario.** Step 60 classified it as "build differs from D75" and kept it as written.

### Fuzz findings

Schemathesis ran 20 operations, with 30 examples per operation and seed 1. It found 0 server errors and 14 failed checks. 11 of those are explained by `fuzz-known.yaml`, and 3 remain as findings.

1. **POST /external-vocabulary/searches, `aliasUris: []` gives 400 ("aliasUris must be an ...").**
   - **Cause: a decision the fuzz list misses.** D38 says "each array with at least 1 entry when present". `fuzz-known.yaml` has an entry for this case, but it matches the text `'aliasUris' must`, with quotes, which is the run 6 and run 7 wording. This build writes the field name without quotes.
   - The published OpenAPI also has no `minItems`. That is one of the D79 contract changes.
   - It is not a build bug. Run 7 had the same problem.
2. **POST /vocabularies/searches, `version: 9223372036854775808` (2^63) gives 400 ("version must be an integer ...").**
   - **Cause: a contract problem in the OpenAPI.** The schema is `type: integer, minimum: 1`, with no `format` and no `maximum`. So Schemathesis may send any size of integer.
   - No decision sets an upper bound. R-077 of run 7 asked this for `page` and the version path, not for request bodies.
   - The 400 is a sensible answer and not a build bug. See R-097.
3. **POST /vocabularies/{vocabularyName}/versions, a long changelog of repeated "a" gives 400 ("changelog must have ...").**
   - **Cause: a decision the fuzz list misses.** D37 allows 1 to 1000 characters after trimming. The known entry matches `'changelog'` with quotes, and this build words it without.
   - My assumption: the generated text is longer than 1000 characters. The console shows a long run of "a", and the message wording matches the D37 limit.
   - It is not a build bug.

**Factory note.** `fuzz-known.yaml` matches on message wording, which changes with every build. Matching on the issue `name` and `in` fields of the problem body would hold across builds.

## 4. Traceability

| Measure | Share |
|---|---|
| Live criteria with a scenario | 85 of 85 (100%) |
| Live criteria with a passing scenario | 82 of 85 (96%) |
| Live criteria with all scenarios passing | 78 of 85 (92%) |
| Live criteria with code, according to `TRACEABILITY.md` | 85 of 85 (100%) |
| Live criteria with an own automated build test | 41 of 85 (server behaviour); the 44 UI criteria have none |

**Criteria with nothing:** none. Every live criterion has code and at least one independent scenario. The weakest is **SIMPL-18162/AC4**, whose only scenario is `@wip`.

The build's own UI evidence is thin. `TRACEABILITY.md` lists manual Playwright checks for 13 of the 44 UI criteria and none for the other 31. The independent UI suite of step 60 covers that gap: 45 of 46 cases pass.

## 5. Quality of the build

### Quality targets

| Decision | Target | Measured (step 70 `load.json` unless stated) | Verdict |
|---|---|---|---|
| Q1 reads p95 | at most 300 ms | worst 240 ms (`getVocabularies`); all other reads 21 to 38 ms | met |
| Q1 writes up to 1 MB, p95 | at most 3 s | 390 ms (create), 380 ms (new version) | met |
| Q1 write at 5 MB (D71) | at most 30 s | p95 14 s over 6 requests; the build measured 21.9 s | met, with less margin in the build's own run |
| Q1 errors | 0 | 0 of 5575 | met |
| Q1 throughput | 20 reads and 1 write per second | 17.9 reads and 0.70 writes per second | **not met as measured.** Likely cause: the single writer is held up by the 5 MB writes (6 writes of 10 to 14 s, about 70 of the 299 s), and readers slow down at the same time. The question of whether this is an offered rate is still open (R-083). |
| Q2 limits (NFR-04) | the D15, D22, D37, D38, D56 and D76 limits with defined errors | 35 NFR-04 scenarios pass; `QUALITY.md` lists each limit with its own test | met |
| Q3 backup and restore (NFR-06, NFR-07) | daily backup off the host; staged, count-checked restore | round trip 532 to 532 quads (`ops/backup-restore-test.sh`); no schedule and no off-host copy | partly; not measured by step 70 |
| Q4 design load | 1000 vocabularies, 20 versions | seed of 1000 vocabularies plus one vocabulary with 20 versions; the build itself used 2 versions per vocabulary | partly (shape of the seed; see R-105) |
| Q5 maintainability | coverage 80% line and 60% branch; 0 HIGH and CRITICAL in own targets | 94.5% line, 83.7% branch; Trivy 0 and 0 in all own targets | met |
| Q6 accessibility | 0 axe violations (WCAG 2.1 AA) on the list, details and both forms | 0 violations on 4 pages, 4 `@a11y` scenarios pass, every action reachable with Tab (step 60) | met; the build itself did not measure it and records that (NFR-13) |

### Reuse

`REUSE.md` lists 27 capabilities and 13 named patterns. The main reused products are Keycloak, Nimbus JOSE, Spring Security, Fuseki, Jena RIOT and SHACL, Jackson 3, Micrometer, N3.js and nginx.

The 70c verdicts on the REUSE rules: REUSE-03, REUSE-04 and REUSE-06 are met. REUSE-01, REUSE-02, REUSE-05, REUSE-07 and REUSE-09 are only partly met.

- **REUSE-01.** `RealmKeySource` is an own JWKS fetch, cache and cooldown. Nimbus `JWKSourceBuilder` or the Spring resource server would do this. `REUSE.md` calls it "only the glue".
- **REUSE-02.** SPARQL is built by concatenating strings. `traceparent` is parsed with a regex. JSON schema checks are written by hand.
- **REUSE-05 and REUSE-07.** `certificate-authority` and `participant-shared-code` from the catalogue are not addressed. Three built capabilities have no row: request-shape validation (about 900 lines), SPARQL building and the JWKS cache.
- **REUSE-09.** The build uses Tomcat's internal `MultipartStream`. It is recorded, with its reason (D70).

### Main 70c findings

The design review checked 153 rules:

- MUST: 25 met, 22 partly, 1 not met (**OPS-37**).
- SHOULD: 45 met, 20 partly, 11 not met.

The main findings:

1. Request validation is a hand-built copy of the OpenAPI (`OperationCatalogue`, `RequestShapeInterceptor`, `JsonBody`, `MultipartForm`, `Paging`). No test ties it to `openapi-decided.yaml`.
2. SPARQL is built from strings. Escaping goes through `FmtUtils`, and 70c found no injection.
3. Memory is not bounded against concurrency. One upload holds up to 6 MB, plus a base64 copy, plus the triple list, with 100 Tomcat threads in 1 GB. Validation also runs again inside the `ConditionalWrite` retry loop, up to 6 times.
4. No Java formatter (Spotless is declared but not configured), no UI linter, no pipeline, and `ui/tests/` is empty (CODE-54, CODE-55, OPS-37).
5. No TLS. Keycloak runs in `start-dev` with its admin console on 8193. The permit-all management port 8081 is reachable on the `front` network (OWT-A02, OWT-A04, ARCH-17).

**Where 70c is stricter than the build's documents:** REUSE-01, REUSE-02, REUSE-05, REUSE-07, NFR-05, CODE-11, CODE-16, CODE-20, CODE-30, CODE-55, OWT-A02, API-01, DATA-25, DATA-26, DATA-29, ARCH-08 and ARCH-14. The build's documents either do not mention these or rate them higher.

**No disagreement where the build is stricter.**

70c confirmed these claims of `QUALITY.md` from the code:

- every query has a LIMIT;
- there is no lock state;
- containers have limits;
- shutdown is graceful;
- the two store users are split by purpose.

### Own tests

- 46 JUnit tests, 94.5% line and 83.7% branch coverage. `mvn verify` fails the build below 80% line or 60% branch.
- Mutation score: not measured (NFR-09 partly).
- No static analysis tool.
- No own UI tests.

### Deviations from the rules

`DEVIATIONS.md` reviewed 405 rules: 272 followed, 58 not applicable, and 75 not followed or only partly followed (30 entries). The main ones:

- **Belgif problem types** for 400, 404, 500 and 503: API-46 (D48).
- **URL shape and identifiers:** API-24, API-26, API-50 and API-52 (E2, D30).
- **Headers:** BLG-cst-header and BLG-trc-header (D86).
- **No gateway:** SEC-12 and CODE-45 (D17).
- **No Simpl authenticator plugin:** SEC-07.
- **No TLS:** API-25, OWT-A04, OWASP-03, OWA-API8 and ARCH-17.
- **Secrets from environment variables:** OPS-11 and OPS-12 (E6).
- **UI component library and usability testing:** UX-01 and UX-02 (E6).
- **Framework internals:** REUSE-09 (D70).
- **Formatting and tests:** CODE-01, CODE-02, CODE-13, CODE-10 (UI) and NFR-09.
- **Logging and exceptions:** CODE-21, CODE-52, CODE-33, CODE-43 and CODE-44.
- **No circuit breaker:** MSV-07, CODE-46 and ARCH-34.
- **No high availability:** ARCH-07 and ARCH-15.
- **No pipeline:** OPS-05 to OPS-08, OPS-17, OPS-23 to OPS-26, OPS-33, OPS-35, OPS-37, OWASP-12 and OWT-A03.
- **No Kubernetes or chart:** OPS-10, OPS-13 and OPS-27 to OPS-30.
- **Not in the registry:** ARCH-22.
- **Data model notation:** DATA-17, DATA-18 and DATA-22 to DATA-24 (D87).
- **No events:** ARCH-27 and ARCH-28 (D45).

### Scan results

The independent scan of step 70 (Trivy, HIGH and CRITICAL), set against the build's `SECURITY-SCAN.md`:

| Target | CRITICAL | HIGH | Build's `SECURITY-SCAN.md` |
|---|---|---|---|
| deps | 0 | 0 | 0 and 0 |
| backend | 0 | 0 | 0 and 0 |
| fuseki | 0 | 0 | 0 and 0 |
| ui | 0 | 0 | 0 and 0 |
| ops-curl | 0 | 0 | new own image; it replaced `curlimages/curl:8.23.0` (32 HIGH in krb5), fixed |
| keycloak 26.8.2 | 1 | 6 | accepted with a reason per CVE: OpenJDK and sqlite in UBI 9 have no fixed package; guava 33.6.0 sits inside the vendor distribution and needs a Keycloak release |

The two scans agree exactly. OWASP-07 and REUSE-08 are recorded as partly met because of the Keycloak image.

## 6. New spec gaps

Run 7 raised many owner questions that are still open, for example R-066, R-068, R-070, R-071, R-073, R-077, R-082, R-083 and R-085. They are not repeated here. The questions below are new. Numbering continues after R-093.

1. **R-094. Key refetch rate (D44).** D44 says the keys are fetched again when a token names an unknown key id, but gives no rate. The build waits 10 s after a fetch, and an unknown key id inside that window gets 401 without a fetch. Is a cooldown allowed, and how long? What answer should a token get inside the window?
2. **R-095. Writes between backup and restore (Q3).** A restore replaces the store, so writes made after the backup are lost. Is that acceptable? Must writes be stopped first, or must the restore keep or merge later changes? Who installs the daily schedule and the off-host copy?
3. **R-096. Role of the validation report (D75).** D75 asks for a region named 'Validation report' and for error messages with role alert. Must the report be a region that contains an alert (or uses `aria-live`), so both hold? This came from the only failing scenario.
4. **R-097. Integer range in request bodies.** The search `version` (and every other integer in a JSON body) has no `format` and no `maximum` in the OpenAPI. Should the decided contract say `format: int64` or a stated maximum, with 400 above it? This extends R-077 from paths and query parameters to bodies.
5. **R-098. Concurrent uploads (Q2, NFR-05).** How many uploads may run in parallel before the service answers 503? Must content validation run once, outside the conditional-write retry loop?
6. **R-099. Request validation against the contract (MACH-03, REUSE-07).** Must the build use a maintained OpenAPI request validator that loads `openapi-decided.yaml`? If not, must an own test diff the hand-built catalogue against that file? Must every hand-built component over about 100 lines have a `REUSE.md` row?
7. **R-100. Library choices for E6.** Should E6 name Jena `ParameterizedSparqlString` or `UpdateBuilder` for queries, Nimbus `JWKSourceBuilder` (or the Spring resource server) for keys, and Micrometer Tracing for W3C trace context, unless a decision says why not?
8. **R-101. Lint, format and UI tests on the finish line (OPS-37, CODE-54, CODE-55, CODE-10).** Must `mvn verify` run Spotless, the UI build run ESLint and a Prettier check, and each view have at least one UI unit test?
9. **R-102. Structured log fields (CODE-20, CODE-30).** Must every log line about an entity carry the vocabulary name and version as fields, with `operationId` in the MDC?
10. **R-103. The CDM in the inputs (API-01, DATA-04 to DATA-16, DATA-26, DATA-29).** Is the CDM snapshot part of the build input, so these rules can be judged? Or are they out of scope for builds? Must the build ship a glossary link and an EUPL `LICENSE` (DDD-03, ARCH-08)?
11. **R-104. Compose hardening (OWT-A02, ARCH-14).** May the local stack run Keycloak in dev mode with its admin console published? Must the management port sit on its own internal network? Must container limits be variables?
12. **R-105. Shape of the Q4 seed.** Does Q4 mean 20 versions on every one of the 1000 vocabularies (run 7), or 1000 vocabularies plus one deep vocabulary (this run's load test)? Must the Q1 throughput be measured with the 5 MB writes running alongside, or with a separate writer?

**Factory defects (for the pipeline, not for step 30):**

- `verify.py` reads `dry-run.json`, and `untested` overwrites `passed`. Fix: skip `dry-run.json`, or treat every status other than `passed` and `failed` as `skipped`.
- `fuzz-known.yaml` matches message wording, which changes with each build. Fix: match on the issue `name` and `in` fields.

## 7. Compared with earlier runs

| Run | Result | Notable |
|---|---|---|
| run1 (27 Oct) | 74 of 74 API, 22 of 22 UI (own suite) | own suite, not independent |
| run4 (17 Nov) | 379 of 386 | Trivy 17 CRITICAL |
| run5 (18 Nov) | 463 of 468 | Spring Boot 4.1.2, 0 CRITICAL in own targets |
| run6 (19 Nov) | 428 of 432 | first orchestrated run; failures at the 5 MB write (now D71) |
| run7 (19 Nov) | 420 of 424 | Q1 met at the full Q4 load (19.92 reads per second); 2 failures on the encoded-slash HTML body (D67) |
| **run8 (19 Nov)** | **463 of 465** (1 failed, 1 `@wip`) | 46 UI cases (run 7 had 41); D67 encoded slash fixed; 1 UI failure (D75) |

**Better than run 7:**

- The D67 encoded-slash failure is fixed (BUILD-LOG fix 2).
- The five 70c gaps of run 7 are closed by the new decisions, and 70c confirms each one:
  - the store has credentials and no published port (D83, R-089);
  - access is denied by default and `HandlerRuleVerifier` checks it (D84, R-090);
  - every query has ORDER BY, LIMIT and COUNT in the store (D85, R-091);
  - metrics and trace propagation exist (D86, R-092);
  - two instances on one store are tested (D82, R-088);
  - SHACL shapes serve as the physical model (D87, R-093).
- The suite has 41 more cases, and step 45 found 0 misreadings.

**The same as run 7:**

- Keycloak 1 CRITICAL and 6 HIGH; own targets 0 and 0.
- The fuzz text-match misses on D37 and D38.

**Worse than run 7:**

- Q1 throughput fell to 17.9 reads and 0.70 writes per second.
- The Q4 seed used one deep vocabulary instead of 20 versions each.
- The build has no own UI tests and no lint gate.
- The step 70a summary is wrong this run because of the dry-run merge defect.
