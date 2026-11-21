# Final report: Simpl-Open vocabulary-manager, run 13 (21 Nov 2026)

## 1. In short

- **What was built:** the Vocabulary Manager. It has a Spring Boot 4.1.3 backend on Java 21 (two replicas), Apache Jena Fuseki 6.2.0 as the store, Keycloak 26.8.2, and a Vue 3 UI behind nginx. It was built from decisions D1 to D167, E1 to E7 and Q1 to Q6 (engine 1c52cf3, 405 rules).
- **Score:** 551 of 552 scenarios passed (99.8 %).
  - 84 of 85 live criteria have a scenario, and all of those scenarios pass.
  - The one criterion without a running scenario is SIMPL-18162/AC4. It has code, and its only scenario is marked @wip because a 500 cannot be caused from outside.
  - 136 of 180 decisions have a scenario and all of them pass. 139 have a scenario.
- **The one failure is a build bug** against D76. The build applies the 5 MB limit to every multipart part, not only to `vocabularyFile`. So a 6 MB request that the decisions allow gets 413 instead of 400.
- **Q1 was missed on one operation.** The independent load test measured `getVocabularies` at p95 346.8 ms against a 300 ms target (D136). Every other read was at or under 61.7 ms. Throughput, write times and the 5 MB writes met their targets. The build's own load test measured the same list at 168 ms, on a smaller store.
- **Fuzzing:** 0 server errors and 1 finding. The finding is not a build bug: it is a decided rule (D91) that the fuzz known-list misses.
- **Scans:** 0 CRITICAL in every target. 6 HIGH remain, all in the reused Keycloak image, and the build accepted each of them with a reason.
- **Design review (70c):** of 52 MUST rules, 30 are met, 16 partly met, 2 not met (both recorded deviations) and 4 not applicable. The main new finding is that the management port can be reached from the front network, against D98.
- **What it means:** the behaviour matches the decisions almost fully. The remaining work is one multipart fix, the paged-list query cost as the store grows, and a few spec questions (section 6).
- **Effort:** about 2 h 15 min wall clock and about 56 USD of agent cost.

## 2. Results

### Scenarios per feature

| Feature | Passed | Failed |
|---|---|---|
| Access control on every operation | 86 | 0 |
| List, filter, sort and page vocabularies | 72 | 0 |
| Register, read, search and remove external vocabularies | 60 | 0 |
| HTTP protocol rules: methods, media types, unknown paths and server limits | 45 | 0 |
| Read a vocabulary, its versions and its content | 44 | 0 |
| Create a vocabulary | 36 | **1** |
| Create a new version of a vocabulary | 31 | 0 |
| Change the vocabulary status and a version status | 26 | 0 |
| Vocabulary references in uploaded content | 26 | 0 |
| Search vocabularies by name and version | 24 | 0 |
| Syntax and semantic validation of uploaded content | 24 | 0 |
| Overlapping changes to one vocabulary | 3 | 0 |
| UI access control, accessibility and security headers (API run: CSP check) | 1 | 0 |
| UI access control, accessibility and security headers (UI run) | 12 | 0 |
| UI create and new version forms | 19 | 0 |
| UI vocabulary list | 18 | 0 |
| UI vocabulary details and version history | 10 | 0 |
| The vocabulary store is not reachable (store-down stack) | 14 | 0 |
| **Total** | **551** | **1** |

One more case (`not_runnable.feature`, SIMPL-18162/AC4) is tagged @wip and was not run. That makes 553 cases in `COVERAGE.md`.

### Scenarios per tag kind

| Tag kind | Passed | Failed |
|---|---|---|
| negative | 325 | 1 |
| no kind tag (positive paths) | 226 | 0 |
| ui (Playwright, step 60) | 59 | 0 |
| store_down | 14 | 0 |

The kinds overlap: 22 of the ui cases and 13 of the store_down cases are also tagged negative.

### Criteria and decisions

| Measure | Result |
|---|---|
| Live criteria with a scenario | 84 of 85 (99 %) |
| Live criteria with all scenarios passing | 84 of 85 (99 %) |
| Decisions with a scenario | 139 of 180 (77 %) |
| Decisions with all scenarios passing | 136 of 180 (76 %) |
| Decisions with a failing scenario | D51, D76, Q2 (all from the same scenario) |
| Rules tested | 23; only NFR-04 has a failure (58 of 59) |

`COVERAGE.md` gives a reason for each of the 41 decisions without a scenario. Most are build-side or not observable through the API: D2, D61, E3, D80, D81, Q3, Q4, Q5, E6, D83, D87 to D89, D92 to D97, D99, D102 to D107, D117, D120, D124, D129, D131 to D136, D152, D156, D157, D164 and D167.

## 3. Failures

### Failing scenario

**Create a vocabulary / "A multipart request of exactly 6 MB passes the size check"** (tags D76, D51, Q2, NFR-04, negative)

- **What the test sends:** a small valid `vocabularyFile`, plus an unknown form field `padding` that brings the whole body to exactly 6 291 456 bytes.
- **What the decisions say:**
  - D76: the 5 MB limit is on the `vocabularyFile` part, and the whole request may be at most 6 MB.
  - D51: the 413 size check comes before the 400 request-shape check.
  - D52: an unknown form field gives 400.
  - So the expected answer is 400 with an issue `in: body, name: padding`.
- **What the build answered:** 413 `payloadTooLarge`, with an issue on `vocabularyFile` saying "Uploaded vocabulary file is too large", although the file is small.
- **Cause: build bug (D76).** `application.properties` sets `spring.servlet.multipart.max-file-size=5242880B`. The servlet multipart parser applies that limit to every part, form fields included, so the roughly 6 MB `padding` part trips it. `MultipartForm` then maps any size exception to the `vocabularyFile` issue. The whole-request limit (`max-request-size=6291456B`) is set correctly.
- **Fix:** take the per-part limit off the servlet configuration, or set it to 6 MB. Then enforce the 5 MB limit by part name on `vocabularyFile`.
- **Review history:** step 45 did not flag this scenario. The test writer recorded the padding design in its own log ("reaching exactly 6 MB needs an unknown padding part").
- **Open question:** D76 does not say what happens when a part other than the file goes over 5 MB inside a 6 MB request. That is spec gap 1 in section 6.

### Fuzz finding

**POST /vocabularies/searches: ST202 "Schema-valid request rejected" (400)**

- **Request:** `{"items":[{"name":"HealthcareVocabulary","version":9223372036854775808}]}`
- **Answer:** 400, with an issue on `items[0].version`.
- **Cause: a decision the fuzz list misses.** D91 makes every integer in a path, query or JSON body a 32-bit signed integer, and D112 defines an integer in a JSON body. The build follows both.
- **What to change:** `fuzz-known.yaml` cites D91 only for path and query operations. It should also cover integers in JSON bodies, here `items[].version` on the internal search.
- **Contract side:** the published OpenAPI does not give `format: int32` and a maximum for this field. D91 puts that limit only in the decided contract.

The other 13 failed fuzz checks are explained by D9, D19, D20, D22, D37, D38, D52 and D91. There were 0 server errors across 20 operations.

## 4. Traceability

| Share of the 85 live criteria | Result |
|---|---|
| With a passing scenario | 84 (98.8 %) |
| With a scenario at all (running) | 84 (98.8 %) |
| With code, according to `TRACEABILITY.md` | 85 (100 %); every story criterion is marked "done" |

- **No criterion is without anything.** SIMPL-18162/AC4 (500 on the list) has code (`ApiExceptionHandler#handleUnexpected`, `#handleStoreError`) and a @wip scenario only. A 500 cannot be caused from outside, so the suite covers the 503 path through D44 instead.
- **Out of scope by decision:** SIMPL-28680 (D1, D61). SIMPL-395 is replaced by the SIMPL-181xx stories. SIMPL-393 is met by the generic upload.

## 5. Quality of the build

### Quality targets

| Decision | Target | Measured | Verdict |
|---|---|---|---|
| Q1 reads (D136) | p95 at most 300 ms per operation at the D99 seed | `getVocabularies` p95 **346.8 ms** (p50 209.5 ms). All other reads: p95 35.8 to 61.7 ms. | **Not met** (one operation) |
| Q1 writes, 1 MB or less | p95 at most 3 s | create 376.9 ms, new version 396.3 ms | Met |
| Q1/D71 5 MB writes | p95 at most 30 s | 2.75 s (5 of 5, no errors) | Met |
| Q1 throughput (D99) | at least 95 % of 20 reads/s and 1 write/s, no errors | 19.999 reads/s, 1.0 writes/s, 0 errors | Met |
| Q2 limits (NFR-04) | every limit enforced, tested at and above | 60 of 61 Q2 scenarios pass; the 6 MB request limit fails (section 3) | **Not met** (one limit) |
| Q3 availability and recovery | 99.5 %, RTO 30 min, RPO 24 h, tested backup and restore | Round trip 16 s (112 quads); the 357 991-quad seed restored in 22 s. Availability not measured. | Recovery met. **Availability not measured** and not declared as such in `QUALITY.md` (NFR-13 partly met, 70c). |
| Q4 design load | 20 users, 1000 x 20 versions, 10 consumers | Load test ran at the D99 seed on two replicas | Met as a load shape. The text of Q4 still says "one backend instance (D81)", although D82 withdrew D81. |
| Q5 maintainability | 80 % lines, 60 % branches; 0 HIGH/CRITICAL in own targets; one store query per upload | JaCoCo 94.3 % lines (94.2 % in `DEVIATIONS.md`), 83.4 % branches; own targets 0/0 (confirmed by step 70); one query (`VocabularyRepository#referenceState`, accepted by 70c) | Met |
| Q6 accessibility | 0 axe violations on 4 pages; keyboard access | 0 violations (step 60); Tab reaches every action on the list; 5 of 5 Q6 scenarios pass | Met |

**Why Q1 was missed.**

- The build fixed this query once already (build log, fix 7). Before the fix, the list query joined every vocabulary's latest description before sorting, and p95 was 329 ms. The fixed query sorts and pages in a sub-query, and the build then measured 168 ms on about 1 300 vocabularies.
- The independent test ran on the same `LoadSeed` data. Because the store is never reset, it also held everything the 552-scenario suite had created, plus the build's earlier data. The list query still sorts every vocabulary on each call, so its cost grows with store size.
- Run 12 measured this list at 280 ms.
- So this is partly a build weakness (the cost grows with the store) and partly a spec gap: D136 does not say at what store size the p95 is measured. That is spec gap 2 in section 6.

### Reuse

`REUSE.md` goes through the catalogue. It reuses Keycloak, Fuseki/TDB2, Jena RIOT and SPARQL builders, Nimbus JOSE, Spring Security `StrictHttpFirewall`, Spring MVC, Jackson 3, Tomcat multipart, Resilience4j, Micrometer with Brave, nginx, Vue and PrimeVue. It names its own code: the request validation (D93), `SparqlStoreClient`, `WriteRetry`, the `SemanticRules` S1 to S7, and `RealmKeyProvider`.

70c verdicts on the REUSE rules:

| Rule | Verdict | Why |
|---|---|---|
| REUSE-01 (MUST) | partly | Bearer parsing, the algorithm allow-list, the key cache and refetch, and the role gate are own security code. `REUSE.md` names only the fetch timing as own code. D108 and D132 allow it. |
| REUSE-02 | partly | Scripts parse JSON with `sed`. |
| REUSE-03, -04, -05, -06, -09 | met | |
| REUSE-07 (MUST) | partly | `REUSE.md` leaves out `OperationCatalog` and `HandlerRuleCheck` (an own regex router) and the nginx method map. |

### Main 70c findings and disagreements

1. **The management port can be reached from the front network (D98).** The backend joins `[store, front]` (`compose.yaml:80`) without `management.server.address`. This affects OWT-A02 and ARCH-30. It is not in `DEVIATIONS.md`, and `QUALITY.md` says only that the port is "not published on a host port".
2. **Three routing tables are kept by hand:** Spring MVC, `OperationCatalog` and the nginx `map`. The nginx map has no check against the other two. The cause is the status-code order of D67, D139 and D63.
3. **The security gate is own code around Nimbus** (REUSE-01, see above).
4. **No pipeline (OPS-37, MUST, D95), and lint is not enforced everywhere.** Spotless is skipped in the image build (CODE-54 partly met), and the Python and shell scripts have no linter (CODE-55).
5. **Deferred platform items:** no TLS, no encryption at rest (D165), no token-failure limiting (D164), no alerting, and a single Fuseki and Keycloak (ARCH-07). Q3 availability is neither measured nor declared as not measured (NFR-13).

Other disagreements:

- Small gaps that are not in `DEVIATIONS.md`: API-10, API-35, API-49, CODE-55 and REUSE-02.
- The coverage figure differs between documents: 94.3 % against 94.2 %.
- In the other direction, the build lists UX-01 and SEC-02 as deviations, where 70c rates them not applicable.

### Own tests and deviations

**Own tests**

- Backend: 80 tests, JaCoCo 94.3 % lines and 83.4 % branches, thresholds enforced in `mvn verify`.
- UI: 45 Vitest tests, 83 % lines and 88 % branches.
- Mutation score: not measured (NFR-09 deviation, no tool in the profile).

**Rules overridden by a decision**

| Rule ids | Decision |
|---|---|
| API-46 | D48 |
| CODE-11 | D135 |
| CODE-21, CODE-52, SEC-11 | D105 |
| CODE-33 | D104 |
| OWT-A07 | D164 |
| API-25, OWT-A04, OWASP-03 | D165, E2 |
| SEC-12, CODE-45 | D17 |
| DDD-04 | D10 |
| DDD-06, DDD-08 | D25, D26, D39 |
| API-06, API-24, API-50, API-52, API-54 | D39, E2 |
| DATA-18 | D117 |
| DATA-22 to DATA-25 | D87 |
| SOA-09 | D77 |

**Rules not followed for other reasons**

- OPS-05, OPS-06, OPS-24, OPS-33, OPS-35, OPS-37, OPS-07, OPS-08, OPS-14 and OPS-26: no CI pipeline.
- OPS-10, OPS-11, OPS-13, OPS-25 and OPS-27 to OPS-30: Compose only.
- NFR-09: no mutation testing or static analysis.
- OWASP-07 and OWT-A03: Keycloak HIGH findings.
- OWT-A09: no alerting.
- ARCH-07: single store and identity provider.
- ARCH-22: no service registry.
- BLG-int-negot and BLG-int-conlang: problem texts are English only.
- BLG-trc-header and BLG-cst-header: D86 names other headers.
- CODE-39: the runtime `ApiException` is not listed on every method.
- UX-01: the Figma design system is not available.
- API-21: examples are kept from the published contract.

**Decisions not built exactly as written**

- D83 together with D156: all three store users can start a compaction, because Fuseki protects its admin endpoints only by the server-level user list.
- D99: the seed was bulk-loaded through the restore path instead of through the API.

### Scan results (step 70, Trivy, HIGH and CRITICAL)

| Target | CRITICAL | HIGH | What `SECURITY-SCAN.md` says |
|---|---|---|---|
| deps | 0 | 0 | 0/0, nothing to fix |
| backend image | 0 | 0 | 0/0 (`apk upgrade`, base pinned by digest) |
| fuseki image | 0 | 0 | 0/0 (jar verified by SHA-512) |
| ui image | 0 | 0 | Fixed from 56 HIGH and 1 CRITICAL with `apk upgrade` |
| keycloak 26.8.2 | 0 | 6 | Upgraded from 26.7.5 (2 CRITICAL, 18 HIGH). 6 HIGH accepted with reasons. |

The independent scan agrees with the build's figures.

The 6 accepted Keycloak findings:

- 5 have no fixed version: one in openjdk and four in sqlite-libs.
- 1 is in the bundled guava (CVE-2026-102554). A fixed guava exists (33.7.2), but only Keycloak can ship it, so the build accepts it under REUSE-09.

## 6. New spec gaps

These are questions for the owners that steps 20 to 30 did not raise, as far as the build log, the failures and the reviews show. Each goes back into step 30 as a finding R-nnn.

1. **D76, size of non-file parts.** Does the 5 MB limit apply only to `vocabularyFile`, or to every part? A non-file part over 5 MB inside a request of 6 MB or less: should it get 413, or 400 as an unknown or invalid field (D52)? And when the whole request is over 6 MB, which issue name does the 413 carry when `vocabularyFile` itself is small?
2. **D136 and D99, store size for Q1.** Is the list p95 measured on the D99 seed alone, or on the seed plus whatever earlier suites left (the store is never reset)? Must the list target still hold when the store grows past the Q4 design load, for example to 2 000 or 3 000 vocabularies?
3. **Q4 against D82 and D102.** Q4 still says "One backend instance (D81); scale up, not out". Align it with D82, which withdrew D81, and with D102.
4. **D98, the management port.** Should the decision state how the management port is kept off the front network (a separate network or `management.server.address`), and require a check that the front network cannot reach port 8081?
5. **Q3 and NFR-13, availability.** How should a factory build show the 99.5 % availability target? Is "not measured, needs production monitoring" an accepted entry?
6. **D12, D69 and D146 (build G1).** A namespace that ends in `/content/<Name>/versions/` with nothing after it: is that rule S7 (an empty version), or the latest version?
7. **D83 against D156 (build G3).** Online compaction cannot be limited to the admin store user in Fuseki 6.2.0. Accept that the reader and writer users can start a compaction, or require another guard, such as a proxy in front of the admin endpoints?
8. **D46 (build G2).** Is checking byte equality through a stored SHA-256 acceptable?
9. **Form fields (build G4).** What happens when a single-valued form field such as `name` comes twice?
10. **D110 (build G5).** What is the issue name for more than 64 multipart parts?
11. **D23 (build G6).** For the `-status` sort on versions, which status comes first, and what breaks ties?
12. **401 and 403 texts (build G7).** Which detail texts do the 401 and 403 problems carry?
13. **D12 (build G8).** Is a "W3C core" namespace recognised by prefix or by IRI?
14. **S1 and S2 (build G9).** How is the location of a blank-node subject or object given?
15. **D13 (build G10).** The browser check (n3.js) and the server check (Jena RIOT) can disagree on edge cases. Is the rule "server is authoritative, browser blocks only on its own errors" right?
16. **D44 and D134 (build G11).** Should a store 401 or 403, for example a wrong store password, stay 500 and not count towards the breaker?
17. **D98 (build G12).** What CPU and memory values should each service get?
18. **D91, published contract.** Should the published OpenAPI carry `format: int32` and the maximum for every integer, including `items[].version` in the internal search, so that fuzzing against it does not flag decided behaviour?
19. **D122, D158 and D166 (step 45, question 1).** When the proxy fronts the API, which layer answers TRACE, an oversize header and an oversize URL? Must a proxy 405 carry Allow? Is 400 allowed instead of 414 or 431? The suite now accepts all three.
20. **D113 and D163 (step 45, question 2).** After a successful new version, is the route `/vocabularies/<Name>` or `/vocabularies/<Name>/versions/<n>`?
21. **D55 and D66 (step 45, question 3).** Is the issue detail inside a 404 fixed text, or free text?
22. **D38 and D137 (step 45, question 5).** What is the issue name for an empty internal search array (`{"items": []}`)?
23. **D67, D139 and D63, status-code order.** This order is why the build has its own security gate and three hand-kept routing tables. Keep it, accept the order Spring Security gives, or require one generated source for the operation table?
24. **Platform-deferred MUST items.** Who closes TLS (D165), encryption at rest, token-failure limiting (D164) and alerting (OWT-A09) for production?
25. **CDM owners.** The CDM uses generic dates (DATA-12), keeps search classes that the LDM drops (DATA-14) and has untyped attributes (DATA-10). It also still lists the warning codes `syntaxError` and `semanticError`, which D10 replaced (DDD-04).

## 7. Compared with earlier runs

| Run | Result | Q1 | Build bugs |
|---|---|---|---|
| run13 (this run) | 551 of 552 | **missed** on the list (p95 346.8 ms) | 1 (D76) |
| run12 | 466 of 470 | met (list p95 280 ms) | 2 (D158, D52/D76), plus 1 test bug |
| run11 | 547 of 549 | met | 1 (D18) |
| run10 | 527 of 530 | | 3 |
| run9 | 476 of 479 | | 2 UI failures |

- **The pass rate is the highest so far.** The D158 failure of run 12 is gone, but partly because step 45 (SR-04) widened that scenario to accept 400, 414 or 431 until spec gap 19 is answered.
- **The multipart limit bug looks like the run 12 "D52/D76" bug coming back.** If so, it has survived two runs, and the D76 wording (gap 1) is the likely root.
- **Q1 went backwards:** 280 ms in run 12, 346.8 ms now on the list. This is the first Q1 miss since run 8. The independent measurement differs from the build's own (168 ms), so the measurement conditions need to be fixed (gap 2).
- **Steady:** axe 0 violations as in runs 11 and 12. No CRITICAL findings in any target, against 17 CRITICAL in run 4.
- **Test bugs:** none failed this run. Step 45 fixed 15 of 18 findings before execution, and step 60 fixed 4 more in the UI steps.
