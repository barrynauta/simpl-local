# Build report: Simpl-Open vocabulary-manager, run12 (20 Nov 2026)

## 1. In short

- **What was built:** the vocabulary-manager, from decisions D1 to D167, Q1 to Q6 and E1 to E7. The stack has two Spring Boot 4 backend replicas, Apache Jena Fuseki (TDB2), Keycloak with the E1 realms, an nginx API gateway, and a Vue 3 UI behind nginx. `stack.sh up` worked from a clean state and the smoke test passed.
- **How it scored:**
  - 466 of 470 scenarios passed (99.1 %). 3 failed and 1 was skipped (a `@wip` scenario).
  - All 54 UI scenarios passed, and so did all 6 store-down scenarios.
  - Fuzzing found 0 problems and 0 server errors.
  - Q1 was met at the full D99 seed. The slowest read p95 was 280 ms against a 300 ms target, and there were 0 errors at 20 reads/s and 1 write/s.
  - Trivy found 0 HIGH and 0 CRITICAL in the build's own images and dependencies.
- **What the failures mean:**
  - **Two build bugs.** The nginx gateway answers a very long URL with a 500 HTML page (D158, D122). A large non-file multipart part gets a generic issue instead of the field name (D52, D76).
  - **One test bug.** A setup step treats the decided 409 of D4 as a setup failure.
  - Neither build bug touches the main functions: create, version, status, search, download, access control.
- **Main gaps from the design review (70c):**
  - There is no TLS in transit, and no decision covers it (OWT-A04).
  - Token handling and trace propagation are partly own code where a library would do (REUSE-01, D94).
  - There is no threat model and no alert path.

## 2. Results

### Scenarios per feature

| Feature file | Scenarios | Passed | Failed | Skipped |
|---|---|---|---|---|
| access_control.feature | 85 | 85 | 0 | 0 |
| create_version.feature | 28 | 28 | 0 | 0 |
| create_vocabulary.feature | 51 | 50 | 1 | 0 |
| cross_cutting.feature | 6 | 4 | 1 | 1 (`@wip`) |
| external_vocabularies.feature | 43 | 43 | 0 | 0 |
| health.feature | 3 | 3 | 0 | 0 |
| list_vocabularies.feature | 50 | 50 | 0 | 0 |
| read_vocabulary.feature | 40 | 40 | 0 | 0 |
| search.feature | 33 | 33 | 0 | 0 |
| status_change.feature | 28 | 27 | 1 | 0 |
| store_down.feature | 6 | 6 | 0 | 0 |
| ui_forms_details.feature | 23 | 23 | 0 | 0 |
| ui_list.feature | 31 | 31 | 0 | 0 |
| validation.feature | 43 | 43 | 0 | 0 |
| **Total** | **470** | **466** | **3** | **1** |

### Scenarios per tag kind

A scenario counts once per kind it carries.

| Tag kind | Passed | Failed | Skipped |
|---|---|---|---|
| `@negative` | 303 | 3 | 0 |
| Positive (no `@negative`) | 163 | 0 | 1 |
| `@ui` (Playwright, step 60) | 54 | 0 | 0 |
| `@a11y` (axe) | 4 | 0 | 0 |
| `@store_down` (port 8199) | 6 | 0 | 0 |
| `@wip` | 0 | 0 | 1 |
| With a criterion tag (`@AC_`) | 216 | 0 | 1 |
| With a decision tag (`@D`, `@E`, `@Q`) | 466 | 3 | 0 |
| With a rule tag | 170 | 2 | 0 |

All three failures are negative scenarios. None carries a criterion tag.

### Criteria

- **Live criteria:** 85. SIMPL-28680 left the set by D61. SIMPL-393 and SIMPL-395 have no Given/When/Then criteria and are settled by D1.
- **With a scenario:** 85 of 85 (100 %).
- **With all scenarios passing:** 84 of 85 (98.8 %). The one left is SIMPL-18162/AC4 (internal error 500). Its only scenario is `@wip` and was skipped.

### Decisions

- **Decisions:** 180 (D, E and Q ids, including the withdrawn D81).
- **With a scenario:** 130 of 180 (72 %).
- **With all scenarios passing:** 124 of 180 (69 %).
- **The six with a failing scenario:** D4, D48, D51, D76, D158 and Q2. D4 and D48 fail only because of the test bug in section 3.
- **The 50 decisions with no scenario** are deployment, internal, build-deliverable or operations decisions:
  - D2, D39, D40, D61, E1 to E4, E6
  - D79, D80, D81, D83, D85, D87, D88, D89
  - D92 to D99, D102 to D107
  - D117, D120, D123, D124, D129, D131, D133 to D136
  - D152, D156, D157, D164, D165, D167
  - Q3, Q4, Q5
  
  COVERAGE.md gives a reason for each. Some of them are checked outside the scenarios: Q1, Q4, D99 and D136 by the load test, D89 by the build's restore round trip.
- **Not checked anywhere in this run:**
  - D92 (a fifth parallel upload gets 503). COVERAGE.md leaves it to step 70, and step 70 did not test it.
  - D103 and D134 (breaker behaviour). Same situation.

### Rules

- **Rules with scenarios:** 23. All pass, except one scenario each under API-46 and NFR-04.

## 3. Failures

### Failing scenarios

**F1. "A URL that is too long gives 414"** (`cross_cutting.feature:27`, tags D158, negative)

- **Cause: build bug.** A GET with a 200,000-character query got `500 text/html` from nginx, not 414 with `application/problem+json`.
- `gateway/default.conf` sets `error_page 414 = @p414`, a named location that returns a problem body. nginx raises the 414 while it reads the request line, before a request exists that it could redirect. The redirect to the named location therefore fails, and nginx sends its built-in 500 page.
- **History:** step 45 flagged this scenario as SR-06. The reviewer argued that the proxy may answer 400 under D122. The test writer kept 414, on the grounds that D122 allows 400 only for a malformed request.
- **Either reading fails here:** D158 asks for 414 and D122 asks for problem+json from the proxy. A 500 HTML page meets neither.
- **Open question:** the proxy question behind SR-06 (step 45 Q-1) is still open. See gap 12.

**F2. "A multipart request of exactly 6 MB passes the size check"** (`create_vocabulary.feature:354`, tags D76, D51, NFR-04, Q2, negative)

- **Cause: build bug, with a spec gap behind it.**
- **What the test sends:** a valid create request, padded to exactly 6,291,456 bytes with one unknown non-file part, `padding`, of about 6 MB.
- **What the build answered:** 400 `belgif:badRequest`, so the D76 size check passed and there was no 413. But the single issue is named `body` ("The multipart request has too many parts or is malformed."), not `padding`.
- **Why:** Tomcat applies `max-http-form-post-size: 2MB` (in `application.yaml`) to non-file parts. That raises an `IllegalStateException` that `MultipartForm.read` does not recognise as a size limit, so it falls back to the generic shape issue.
- **Where it differs from the decisions:** D52 asks for an issue that names the unknown field. The 2 MB per-part limit comes from a framework default and is not in any decision.
- **History:** step 45 asked for this at-limit case (SR-17), and the test writer added it in 45b.
- **Open question:** what size a non-file part may have, and what a too-large one gets. See gap 11.

**F3. "Asking for the current vocabulary status gives 409 (ACTIVE)"** (`status_change.feature:65`, example 1.1, tags D4, D48, negative, API-46)

- **Cause: test bug.** The Given step "vocabulary {{Health}} has status ACTIVE" calls `set_status`, which PATCHes the status and requires 200.
- A new vocabulary is already ACTIVE. The build correctly answered 409 with the exact D4 text, "Vocabulary is already in status ACTIVE", and the setup step treated that as a failure.
- The DEPRECATED example passes.
- **Fix:** the setup step should skip the PATCH when the status already matches, or accept 409.
- **History:** step 45 did not flag this scenario.

**Skipped: "An internal error answers 500 with a problem body and no metadata"** (`cross_cutting.feature:42`, `@wip`, SIMPL-18162/AC4)

- No black-box way exists to cause a 500, because a store outage gives 503 by D44.
- COVERAGE.md left it for fault injection in step 70, and step 70 did not do that.
- **Not a build defect.** It is a test-basis gap (gap 14).

### Fuzz findings (`fuzz-summary.md`)

- **Totals:** 20 operations, 14 failed checks. All 14 are explained by a decision. There are 0 findings and 0 server errors.
- **The 14 explained checks are contract differences, not build bugs.** In each, the published OpenAPI (`vocabulary_openapi-tier1.yaml`) is looser or stricter than what was decided:
  - D9: file typed as binary, but valid Turtle required.
  - D19: relaxed name pattern.
  - D20: impossible dates refused.
  - D22: paging limits.
  - D37: changelog length.
  - D38: search arrays.
  - D49: version in the path must be a positive integer.
  - D52: unknown properties refused.
  - D91: 32-bit integers.
- **Action:** carry these into the published OpenAPI under D79, so later fuzz runs need no known-failure list.
- The build's own Schemathesis run (BUILD-LOG) found the same 14.

## 4. Traceability

| Measure | Share |
|---|---|
| Live criteria with a passing scenario | 84 of 85 (98.8 %) |
| Live criteria with a scenario at all | 85 of 85 (100 %) |
| Live criteria with code, per the build's TRACEABILITY.md | 85 of 85 (100 %) |

**Criteria with nothing:** none.

**Weakest criterion:** SIMPL-18162/AC4. It has code (`ProblemHandler.other`) and only unit tests for store failures, but no executed acceptance scenario.

Step 45 noted two traceability tags that inflated coverage: SIMPL-18175/AC2 on a GET, and SIMPL-18187/AC1 on the CPSV upload (SR-12). The test writer reports them fixed.

## 5. Quality of the build

### Quality targets (NFR-13)

| Target | Measured value | Verdict |
|---|---|---|
| **Q1, reads:** p95 at most 300 ms per operation (D99, D136), full seed of 1000 vocabularies by 20 versions | Seeded with 0 errors. `getVocabularies` p95 280 ms; other reads 35 to 51 ms. | Met. The list is at 93 % of the target. |
| **Q1, writes:** p95 at most 3000 ms up to 1 MB | `createVocabulary` 260 ms, `createVocabularyVersion` 530 ms | Met |
| **Q1, 5 MB write:** at most 30 s (D71) | Slowest of 3 requests: 1472 ms | Met |
| **Q1, throughput:** 20 reads/s and 1 write/s, at least 95 % achieved, no errors | 20.0 reads/s and 1.0 write/s over 300 s, 0 failures | Met |
| **Q2, limits:** at the limit and one above | 46 of 47 Q2 scenarios pass; F2 fails. Not proven: the 3-character name accepted (cannot hold a run id), the D110 64-part limit on its own, and D92. | Partly met |
| **Q3, availability and recovery** | Not measured in step 70. The build's backup and restore round trip passed (565 = 565 quads, smoke green). Availability was not measured, because the factory has no long run. | Not measured |
| **Q4, design load and scaling** | Used as the load shape: 1000 by 20 seed, two replicas. Q1 held at that load. | Met as load shape |
| **Q5, maintainability** | JaCoCo 92.8 % lines and 82.4 % branches (gate 80 and 60). 59 backend tests, 32 Vitest tests. Trivy 0 HIGH and 0 CRITICAL in own targets. Every query has a LIMIT. No mutation score or Sonar run. | Met for what was measured |
| **Q6, accessibility** | axe (WCAG 2.1 A and AA) found 0 violations on 4 pages. All 8 Q6 scenarios pass, including the keyboard scenarios. | Met |

**Note on Q1:**

- BUILD-LOG and QUALITY.md report a list p95 of 305 ms. That came from a reduced, closed-loop run of the build's own.
- The design review (70c) quotes that figure.
- The step 70 load test at the full seed with the offered rate supersedes it.

### Reuse

**REUSE.md** maps every catalogue capability.

**70c verdicts on the REUSE rules:**

| Verdict | Rules |
|---|---|
| Met | REUSE-03, REUSE-04, REUSE-06, REUSE-09 |
| Partly met | REUSE-01, REUSE-02, REUSE-05, REUSE-07 |

**Where 70c disagrees with REUSE.md:**

1. **Token handling.** REUSE.md lists about 60 lines of own key-fetch timing. 70c finds about 100 lines in `TokenVerifier.java:152-220`, plus an unlisted 84-line `BearerTokenFilter` that replaces the Spring Security resource server. 70c also finds the claim that `JWKSourceBuilder` cannot meet D88 and D129 arguable.
2. **Tracing.** "Micrometer Tracing" is claimed, but the `traceparent` header is built by hand in `StoreClient` and `TokenVerifier`. The JDK `HttpClient` calls produce no client spans, which contradicts D94.
3. **"http-client not needed" is wrong,** because every store call is an outgoing HTTP call.
4. **Own code for standard formats is not listed:**
   - `QueryParams` (query string parsing)
   - `ProblemErrorReportValve` (JSON by string concatenation)
   - `backup.sh` (JSON parsed with `sed`)
5. **The key-fetch circuit breaker never helps.** It needs 10 calls, and the fetch runs at most once per 10 s, so 70c counts it as over-engineering.

### Main 70c findings

- **Verdicts:** 153 rules reviewed.
  - MUST: 27 met, 18 partly, 3 not met, 4 not applicable.
  - SHOULD: 55 met, 22 partly, 16 not met, 8 not applicable.
- **MUST rules not met:**
  - OWT-A04: no TLS for the API, the key fetch or the store. No decision covers it.
  - CODE-11: set aside by D135.
  - OPS-37: no pipeline, waived by D95.
- **Other findings:**
  - No threat model (SEC-01, OWASP-05).
  - No alert path for security events (OWT-A09, OWASP-10).
  - The gateway does not create or log `X-Correlation-Id` (MSV-09).
  - The Fuseki admin password sits in a URL and `eval` is used (`ops/backup.sh`). This is not in DEVIATIONS.md (OWT-A07).
  - The CDM is not shipped next to the OpenAPI (API-01, DATA-29).
- **Disagreements with DEVIATIONS.md:**
  - It is silent on SEC-01, OWASP-05, OWT-A09, OWASP-10, CODE-55, API-01 and DATA-29.
  - It calls ARCH-07 "not followed", where 70c finds it partly met.

### Own tests

- **Backend:** 59 tests. JaCoCo 92.8 % lines (2117 of 2282) and 82.4 % branches (916 of 1112). Spotless (Palantir) check green.
- **UI:** 32 Vitest tests. ESLint, Prettier and vue-tsc green.
- **Mutation score:** not measured. This is a declared deviation from NFR-09.

### Deviations from the rules (DEVIATIONS.md)

**Overridden by a decision:**

| Rules | Decision |
|---|---|
| API-46 | D48 |
| CODE-11 | D135 |
| CODE-21, CODE-52, SEC-11, CODE-50 | D105 |
| CODE-33 | D104 |
| CODE-45, SEC-12 | D17, E6 |
| OWT-A07 | D164 |
| OWA-API4 | D46 |
| BLG-addi-prop, BLG-req-valid | D52 |
| API-50, API-52, API-54, BLG-id-name | D30, D31, D39 |
| DATA-22 to DATA-24 | D87 |
| DATA-18 | D117 |
| UX-01 | D167 |

**Not followed, with no decision behind them:**

- API-25, OWT-A04, OWASP-03 (TLS)
- ARCH-07
- BLG-trc-header, BLG-int-conlang, BLG-int-negot, BLG-prf-cach, BLG-prf-lstmod
- API-21, API-22, API-57
- CODE-39, CODE-07
- NFR-09 (mutation testing, Sonar)
- NFR-03 (load run left to step 70, which then met it)
- OPS-05 to OPS-08, OPS-14, OPS-23 to OPS-30, OPS-33, OPS-35, OPS-37 (under D95)
- OPS-10, OPS-11, OPS-13
- SEC-22, SEC-23
- UX-02, MSV-11, ARCH-22

### Scan results

**Step 70 Trivy (`scan-summary.md`), HIGH and CRITICAL:**

| Target | CRITICAL | HIGH |
|---|---|---|
| deps | 0 | 0 |
| backend | 0 | 0 |
| fuseki | 0 | 0 |
| gateway | 0 | 0 |
| ui | 0 | 0 |
| quay.io/keycloak/keycloak:26.8.2 | 0 | 6 |

**Set against SECURITY-SCAN.md:** the step 70 scan matches the build's own scan.

- The build fixed 3 HIGH in each of the gateway and UI images with `apk upgrade` (musl and zlib).
- It accepted the 6 Keycloak HIGH, each with a reason:
  - CVE-2026-91403 (OpenJDK in UBI 9.9): no fixed package.
  - Four sqlite-libs CVEs (CVE-2026-119373, CVE-2026-119374, CVE-2026-119379, CVE-2026-119586): no fixed version, and Keycloak does not use SQLite.
  - CVE-2026-102554 (guava 33.6.0, fixed in 33.7.2): patching it means forking the image (REUSE-09).
- Keycloak is a third-party local stand-in. Whether it counts under Q5 is gap 8.

## 6. New spec gaps

These are questions for the owners that steps 20 to 30 did not raise. They go back into step 30 as findings R-nnn.

**From the build log:**

1. **'+' in query strings (D20, D130, D19).** Does '+' in a query mean a plus sign (RFC 3986) or a space (HTML forms)? The build chose plus.
2. **Over 6 MB and over 64 parts at once (D51, D76, D110).** Which comes first, 413 or 400? The build answers 413.
3. **The D92 upload-limit 503 in the D51 order.** Where does it sit? The build checks it after 401, 403, 406, 415 and the Content-Length 413, and before the body is read.
4. **Base IRI on a retried write (D57 with D82 and D92).** When a concurrent write pushes the stored number from n+1 to n+2, must validation run again, or may S1 to S5 locations name the base of n+1?
5. **HEAD, OPTIONS and unknown methods on an unknown path (D126 covers declared paths only).** Should these get 405, 404 or 401?
6. **Scope of D80.** Does it cover the logs of every container, including the store's own request log, which prints SPARQL with creator e-mails?
7. **First key fetch under parallel requests (D88, D108).** What does a request see while another request is fetching the realm keys? The build makes it wait.
8. **Third-party images under Q5.** Do HIGH findings in a third-party image that runs in the stack (Keycloak) count against Q5?
9. **D82 status-change receipts.** The build keeps one receipt token per status change on the node, and they accumulate. Is a cleanup allowed or required?
10. **UI texts not decided:** field error texts, the browser syntax error text, headings, the version row menu, the header status text, the version-not-found and download-failure texts, and the page counter with 0 vocabularies. See `ui/UI-NOTES.md`.

**From the failures:**

11. **Non-file multipart parts (D52, D76, D150; failure F2).** What is the size limit of a non-file part? What does an over-size unknown part get: an issue named after the part, a generic body issue, or 413?
12. **Proxy answers (D102, D122, D158, D166, D126; failure F1, step 45 Q-1).** With two backends behind one port, the API port is a proxy. Must it:
    - answer an over-long URL with 414 problem+json?
    - answer oversized headers with 431 rather than 400?
    - send an Allow header on its own 405 answers?

**From the suite review and COVERAGE.md (raised in step 45, not in steps 20 to 30):**

13. **Token expiry (D132, OWASP-08).** Should E1 or E7 add a test client with a short token lifespan? Without one, the expiry check cannot be tested separately from the signature check.
14. **SIMPL-18162/AC4 (500 with details).** Is fault injection expected for this criterion, or is it satisfied by D44's 503 and unit tests?
15. **Route after a new version (D113, D163; step 45 Q-2).** Should it be `/vocabularies/<Name>/versions/<n>`, or `/vocabularies/<Name>` with the panel open?
16. **The 64-part limit (D110, D52, D51; step 45 Q-3).** Which issue (name and `in`) does it give, and does it run before the unknown-field check?
17. **IRIs in S1 and S2 locations (D65, D140; step 45 Q-4).** Are IRIs written bare or in angle brackets? Do plain literals carry `^^xsd:string`?
18. **D110 array-element names (D68; step 45 Q-5).** Does the D110 naming apply only to the external URI arrays?
19. **Uploads in the load test (D92, D99, Q4; step 45 Q-6).** Must the load test keep 4 uploads in flight per instance, or does a retried D92 503 not count as an error?
20. **Href of a search result with `latest: true` (D26, D38; step 45 Q-7).** Is it `/vocabularies/<Name>`, or the href of the latest version?

**From the design review (70c), with no decision behind them:**

21. **TLS in transit.** Decide TLS for the API, the key fetch and the store in the local stack, or record an accepted risk (OWT-A04, API-25, ARCH-17).
22. **Threat model and alert path.** Should a short threat model and a minimal alert path be deliverables (SEC-01, OWASP-05, OWT-A09, OWASP-10)?
23. **MUST rules set aside by provisional decisions.** CODE-11 is set aside by D135 and OPS-37 by D95. Should the decisions be made final, or the rules downgraded?

## 7. Compared with earlier runs

| Run | Scenarios passed | Build bugs and notes |
|---|---|---|
| run1 (27 Oct) | 74 of 74 API, 22 of 22 UI | Its own small suite |
| run4 (17 Nov) | 379 of 386 | Trivy 17 CRITICAL |
| run5 (18 Nov) | 463 of 468 | 0 CRITICAL in own targets |
| run6 (19 Nov) | 428 of 432 | Failures at the 5 MB store write (D71 since) |
| run7 (19 Nov) | 420 of 424 | Q1 met at Q4 design load |
| run8 (19 Nov) | 463 of 465 | Q1 throughput missed (D99 since) |
| run9 (19 Nov) | 476 of 479 | 2 UI failures (D100, D101 since) |
| run10 (20 Nov) | 527 of 530 after the API rerun | 3 build bugs |
| run11 (20 Nov) | 547 of 549 after the API rerun | 1 build bug (D18); harness tag defect |
| **run12 (20 Nov)** | **466 of 470** | **2 build bugs (gateway 414, non-file multipart part); 1 test bug; 1 `@wip` skip** |

- **Pass rate:** 99.1 %, against 99.6 % in run11. The suite is smaller (470 against 549 scenarios), because each run writes its own suite.
- **UI:** clean for the third run in a row. All 54 UI scenarios pass and axe finds 0 violations, as in run11.
- **Q1:** met again at the full D99 seed, as in run9 and run11.
- **Supply chain:** CRITICAL counts stay at 0, against 17 in run4. The only HIGH are in the third-party Keycloak image.
- **New this run:**
  - Both build bugs sit at the edges of the request pipeline: the proxy error mapping and the container multipart limits. The domain logic is not involved.
  - The fuzz list now explains every contract difference. There are 0 findings.
