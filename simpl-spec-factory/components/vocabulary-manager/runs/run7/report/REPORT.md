# Final report: vocabulary-manager, run 7 (19 Nov 2026)

Project Simpl-Open, component vocabulary-manager, engine 6c3a721, 405 rules in scope. Sources:

- From step 70: `report/verification.md` and `verification.json` (step 70a), `scan-summary.md` and the raw Trivy files, `fuzz-summary.md` with `fuzz.json` and `fuzz/console.txt`, `load.json`, and `design-review.md` (step 70c).
- From the tests: `tests/work/COVERAGE.md` and the behave reports, and `tests/review/SUITE-REVIEW.md` (step 45 and the answers of 45b).
- From the build: `TRACEABILITY.md`, `DEVIATIONS.md`, `REUSE.md`, `QUALITY.md`, `SECURITY-SCAN.md`, `BUILD-LOG.md` and the JaCoCo report.

To explain the one failure I sent the two failing requests to the running stack with curl. I also read one own integration test line and `docs/architecture.md`.

## 1. In short

**What was built.** The code writer built the full component from the specs and 93 decisions (D1 to D81, E1 to E6, Q1 to Q6). The stack runs under Docker Compose project `vm-factory-run7` and has four parts:

- a Java 21 backend on Spring Boot 4.1.2;
- Apache Jena Fuseki 6.2.0 as the store, holding content as base64 chunks of 64 KB;
- Keycloak 26.8.2 with the realm of E1;
- a Vue 3 UI behind nginx.

The finish line was reached:

- the stack starts from a clean state in 28 s;
- smoke passes 13 of 13;
- `mvn verify` is green with JaCoCo;
- Trivy ran and the open findings are documented;
- the stack was left running.

**How it scored.**

- The independent suite has 424 scenarios: 420 passed (99.1%), 2 failed and 2 were skipped. The 2 skipped scenarios are `@wip` and cannot be provoked through the API.
- All 85 live criteria have a scenario. 84 have all their scenarios passing.
- 82 of 93 decisions have a scenario, and 80 have all their scenarios passing.
- This is the first run measured at the full Q4 design load (1000 vocabularies, 20 versions each). Every Q1 target was met.
- The independent Trivy scan gives 0 HIGH and 0 CRITICAL in the own images and dependencies. The upstream Keycloak image has 1 CRITICAL and 6 HIGH. That matches the build's own scan exactly.

**The failures.** Both failures have one cause, and it is a build bug against D67:

- The test sends a path with an encoded slash or a control character.
- Tomcat refuses that path before it reaches Spring. It answers 400 with its own HTML error page.
- D67 asks for `application/problem+json` of type `urn:problem-type:belgif:badRequest`. The status code is right but the body and media type are wrong.

**Fuzzing.** It found no server errors. The 2 findings are decided limits (D38, D37). This build words its messages differently, so the fuzz list's text match missed them.

**What it means.** The functional behaviour follows the decisions almost completely. The remaining risk lies in qualities that scenarios do not reach. The design review found:

- a role gate that fails open for any route left out of the route table;
- a store with no authentication;
- list and search queries with no bound;
- no metrics or trace propagation;
- a forced single replica (D81).

The build's own documents do not mention most of these.

## 2. Results

### Scenarios per feature

| Feature | Passed | Failed | Skipped |
|---|---|---|---|
| Role-based access control on every operation | 80 | 0 | 0 |
| Vocabulary versions via the API | 60 | 0 | 0 |
| External vocabularies via the API | 46 | 0 | 0 |
| Vocabulary list with filters, sorting and paging via the API | 45 | 0 | 0 |
| Create a new vocabulary via the API | 39 | 0 | 0 |
| Syntax and semantic validation of uploaded content | 32 | 0 | 0 |
| Search operations for consumers | 29 | 0 | 0 |
| HTTP protocol rules that hold on every operation | 23 | **2** | 0 |
| Update the vocabulary status via the API | 16 | 0 | 0 |
| Vocabulary list in the UI | 17 | 0 | 0 |
| Create and new version forms in the UI | 14 | 0 | 0 |
| Vocabulary details and version history in the UI | 10 | 0 | 0 |
| Behaviour when the store is not reachable | 7 | 0 | 0 |
| Health of the Vocabulary Manager API | 2 | 0 | 0 |
| Behaviour that cannot be provoked through the API | 0 | 0 | 2 (`@wip`) |
| **Total** | **420** | **2** | **2** |

By run mode:

| Run mode | Passed |
|---|---|
| API | 372 of 374 |
| Store down | 7 of 7 |
| UI | 41 of 41 |
| `@wip` | 0 of 2 (skipped) |

### Scenarios per tag kind

| Tag kind | Passed | Failed | Skipped |
|---|---|---|---|
| `@negative` | 263 | 2 | 2 |
| no kind tag (positive paths) | 152 | 0 | 0 |
| `@a11y` | 3 | 0 | 0 |
| `@slow` (5 MB writes) | 2 | 0 | 0 |

The rule tags are all green: 22 rules are tested, and every scenario that carries a rule tag passes. The largest are:

| Rule | Scenarios passing |
|---|---|
| OWASP-02 | 75 |
| SEC-05 | 54 |
| API-46 | 43 |
| API-47 | 41 |
| BLG-err-problem | 41 |
| NFR-04 | 29 |
| OWASP-08 | 22 |
| ARCH-32 | 21 |

### Criteria

- **85 of 85** live criteria have a scenario.
- **84 of 85** have all their scenarios passing.
- The exception is `SIMPL-18162/AC4` (an internal error gives 500 with a problem detail). Its only scenario is `@wip`: "An internal error on the list gives 500 with the problem detail". It is not a failure. Nothing in the suite can provoke a 500.

### Decisions

- **82 of 93** decisions have a scenario. **80 of 93** have all their scenarios passing.
- Two decisions are not all green:
  - **D67**: 7 of 9 pass. The 2 failures are covered in section 3.
  - **D44**: 10 of 12 pass. 2 are `@wip`: the 500 above, and "A token with an unknown key id while the identity provider is down fails closed with 503". The build's own test `VIT.unknownSigningKeyWithIdentityProviderDownFailsClosed` covers the second.
- Eleven decisions have no scenario. COVERAGE gives a reason for each:

| Decision | Reason |
|---|---|
| D2, D39, D61 | Nothing to observe, or tested under other decisions |
| D80 | Logs are not visible through the API |
| D81 | An accepted risk |
| E3, E4, E6 | Environment choices |
| Q3, Q4, Q5 | Measured outside Gherkin (see section 5) |

## 3. Failures

### Failing scenarios

**1 and 2. "A path the firewall refuses gives 400 before the token check" (encoded slash, `/vocabularies/Abc%2FDef`) and (control char, `/vocabularies/Abc%00Def`).** `protocol.feature:108` and `:109`. Tags `@D67 @negative`.

- **Observed.** The status is `400`. The body is `Content-Type: text/html;charset=utf-8` with Tomcat's "HTTP Status 400 Bad Request" page. I reproduced this on the running stack with curl, without a token.
- **Decision.** D67 says: "a path the firewall refuses (encoded slash, control character) returns 400 problem+json, type urn:problem-type:belgif:badRequest, no issues; then the token".
- **Cause: a build bug.**
  - The build wired Spring Security's `StrictHttpFirewall` into its `EntryFilter` (`docs/architecture.md`, step 1).
  - Embedded Tomcat rejects `%2F` and `%00` at the connector, before any servlet filter runs. Tomcat's default error report valve then writes HTML.
  - So the build's firewall never sees these requests, and the problem mapping never runs.
  - The build's own test (`VocabularyApiIT.java:499`) checks only the status code, with an admin token. It could not catch this.
- **Fix.** Either let Tomcat pass these paths through to the firewall, or replace Tomcat's error page with the Belgif problem for connector-level 400s.
- **Step 45** did not flag this scenario. The test writer listed it as an assumption in COVERAGE section 5 ("A firewall-refused path ... gets 400 even without a token (D67)"). That reading matches D67.
- **History.** Run 6 raised the question as R-049. D67 now answers it clearly, so this is not a spec gap.

### Skipped scenarios

The 2 `@wip` scenarios in `not_runnable.feature` (see section 2) cannot be provoked through the API, so they are not failures. They leave `SIMPL-18162/AC4` without any automated proof. The build's TRACEABILITY says "covered by handler design", which is not a test. See gap R-066.

### Fuzz findings

Schemathesis ran 30 examples per operation, seed 1, over 20 operations. Result: 11 failed checks, 9 explained by `fuzz-known.yaml`, 2 findings, 0 server errors.

| Operation | Check | Server answer (full) | Cause |
|---|---|---|---|
| POST /external-vocabulary/searches | ST202 | 400, issue `in: body`, `name: aliasUris`, "'aliasUris' must be an array of at least 1 URI." The request had `"aliasUris": []`. | **A decision the fuzz list misses.** D38 says "each array with at least 1 entry when present". `fuzz-known.yaml` matches only the run 6 wording "Field 'aliasUris'". Not a build bug and not a contract problem. The OpenAPI could still add `minItems: 1` in the next contract version (D79). |
| POST /vocabularies/{vocabularyName}/versions | ST202 | 400, issue "'changelog' must be ..." for a changelog of several thousand characters | **A decision the fuzz list misses.** D37 limits changelog to 1 to 1000 characters after trimming. The list matches "Field 'changelog'" and "Form field '", not this wording. The OpenAPI has no `maxLength`, which D79 already lists as a contract change. |

Two notes on the fuzz results:

- I checked one more thing. Duplicate entries in `namespaceUris` are accepted (200). That fits D38, which says "an external vocabulary matched by several URIs appears once". BUILD-LOG says "duplicate entries (D38)" are rejected in the external search. That statement is wrong, but the behaviour is right.
- This is a factory point, not a spec point. Matching known failures on free message text breaks with every build that words its messages differently. Run 6 hit the same problem. Matching on the issue `name` and the decision would be stable.

## 4. Traceability

| Measure | Share |
|---|---|
| Live criteria with a passing scenario | 84 of 85 (98.8%) |
| Live criteria with a scenario at all | 85 of 85 (100%) |
| Live criteria with code, according to the build's TRACEABILITY | 85 of 85 (100%) |
| Criteria with nothing | none |

There are two caveats on the build's TRACEABILITY:

- It groups criteria in ranges. Examples: `SIMPL-18173 AC2-AC6`, `SIMPL-18180 AC5-AC7`, `SIMPL-18196 AC1-AC4`. 22 of the 85 criteria are traced only through such a range. The build also numbers criteria "in the order of the scenarios in each story file". That may not match the step 12 ids one to one.
- Some criteria have code but no own test:
  - `SIMPL-18162/AC4`: "covered by handler design".
  - `SIMPL-18161/AC2` and `SIMPL-18172/AC3`: "not automated (needs an API stub)".

  The independent suite covers the last two with passing UI scenarios.

## 5. Quality of the build

### Quality targets

| Target | Measured | Verdict |
|---|---|---|
| **Q1** reads p95 at most 300 ms, at Q4 load | Step 70 load test, seeded at 1000 x 20 (`measured_at_q4_volume: true`). `GET /vocabularies` p95 **270 ms**. Every other read p95 is 41 to 46 ms. 0 errors. | Met. The list has only 10% margin. That fits the 70c finding that lists load all rows and page in memory. |
| **Q1** create or new version up to 1 MB, p95 at most 3 s | Create p95 69 ms. New version p95 360 ms. 0 errors. | Met |
| **Q1** 5 MB file within 30 s (D71) | 3 writes, p95 960 ms. The `@slow` scenarios pass. | Met. This was run 6's failure. |
| **Q1** 20 reads/s and 1 write/s sustained, no errors | 19.92 reads/s, 1.0 writes/s, 0 errors | Met according to `load.json`. 19.92 is just under 20; the tool applies a tolerance that Q1 does not state (R-083). |
| **Q2** limits enforced, tested at and above (NFR-04) | 29 scenarios tagged NFR-04 pass. Two limits are the build's own and not decided: JSON body 1 MB and 200 multipart parts. They are checked by "code review" only. | Met for the decided limits. Partly met for the build's own limits. |
| **Q3** availability 99.5%, RTO 30 min, RPO 24 h, daily off-host backup, tested restore | Not measured (NFR-13). Availability cannot be measured inside a build. The backup and restore round trip (`ops/backup-restore-test.sh`) passed. Scheduling and the off-host copy are not automated (DEVIATIONS, NFR-06). | Partly. Restore is shown, the rest is not measured. |
| **Q4** design load: 20 users, 1000 x 20 versions, 10 consuming services, one instance | Volume measured in step 70. The build's QUALITY.md still says "not load-tested at 1000"; that is now out of date. `load.json` reports `"users": 0`, which looks like a reporting fault (COVERAGE describes 18 readers plus writers). The 10 consuming services are not modelled separately. | Met for volume and rates. Concurrency is not shown in the report. |
| **Q5** line coverage at least 80%, branch at least 60%; no HIGH or CRITICAL in own targets; at most one store query per upload | JaCoCo: line **93.0%** (2008 of 2158), branch **78.9%** (696 of 882). Trivy own targets 0/0. One query per upload is claimed by the build (`VocabularyStore.referenceFacts`) and was not measured independently. | Met. The query bound is claimed, not proven. |
| **Q6** WCAG 2.1 AA, keyboard reach | 3 `@a11y` scenarios pass, and "The details page meets WCAG 2.1 AA" passes. The build's axe run shows 0 violations on 4 pages. Keyboard reach is only partly automated. | Met for axe. Keyboard reach is partly shown. |

Mutation testing was not measured.

### Reuse

`REUSE.md` lists reuse for every capability: Keycloak, Spring Security with Nimbus, Jena RIOT, Commons FileUpload 2, Jackson, N3.js, keycloak-js and others. It gives reasons for the parts that were built. The 70c verdicts on the REUSE rules:

| Verdict | Rules |
|---|---|
| Met | REUSE-03, REUSE-04, REUSE-05, REUSE-09 |
| Partly | REUSE-01, REUSE-02, REUSE-06, REUSE-07 |

- **REUSE-01:** the build has its own JWKS fetch and refetch (`HttpKeyFetcher`, `RealmKeySource`), although Nimbus `JWKSourceBuilder` already offers this. Role authorisation is built by hand outside Spring Security (`RouteTable`, `RequestGateFilter`).
- **REUSE-02:** the build has its own query-string parser (`QueryParameters`). SPARQL is built by concatenating `NodeFmtLib` output instead of using `ParameterizedSparqlString`.
- **REUSE-06:** "Stateless design" is named but not applied (`NameLocks`). "Zero trust" is overstated, because the store is unauthenticated.
- **REUSE-07:** `REUSE.md` has no rows for the own routing and authorisation, the query parser, the JWKS client, or the choice not to use Micrometer.

### Main 70c findings

| Level | Met | Partly | Not met | Not applicable |
|---|---|---|---|---|
| MUST (52) | 23 | 21 | 6 | 2 |
| SHOULD (101) | 38 | 23 | 18 | 22 |

The six MUST rules not met are API-01, DATA-26 and DATA-29 (no CDM), ARCH-07 (single replica), OPS-37 (no pipeline) and UX-01 (no participant-fe-components). All six are recorded deviations.

The five most important findings:

1. **The role gate fails open** for any path missing from `RouteTable` (`RequestGateFilter.java:47`). Spring Security only requires an authenticated user. Today every mapping is in the table, but nothing keeps them in step (OWT-A10).
2. **The store has no authentication.** Fuseki runs with `--update` and no credentials, and loopback port 3141 is published (OWT-A02, ARCH-17, ARCH-18).
3. **Store reads have no bound.** List and search queries fetch every row and page in memory (NFR-05). The Q1 list latency of 270 ms is the visible effect.
4. **No metrics and no tracing.** The correlation id is not forwarded to Fuseki or Keycloak (NFR-08, MSV-09, MSV-10). D67 itself expects metrics on the management port.
5. **The backend can run only as a single replica**, because of the in-process `NameLocks` (D81, ARCH-07, MACH-06, MACH-09). The build also delivers no updated OpenAPI for D79 (MACH-03, DATA-27, SOA-05).

**Disagreements with the build's documents.**

- `REUSE.md` claims a stateless design, zero trust and defence in depth, and says `RealmKeySource` "only caches". 70c finds each claim wrong or overstated.
- `QUALITY.md` lists bounded resources without mentioning the unbounded queries. It also counts "code review" as a limit test.
- `DEVIATIONS.md` leaves out rules that 70c finds not met or only partly met:
  - NFR-08, MSV-09, MSV-10 (metrics, tracing, correlation id);
  - CODE-21 and CODE-52 (log format);
  - CODE-33 (shared exception framework);
  - DATA-02 and DATA-03 (LDM and PDM);
  - OWT-A02 (open Fuseki admin on loopback);
  - OWT-A10 (fail-open role gate);
  - CODE-55 (no UI lint);
  - MACH-06 (in-process state).
- The other way round: 70c finds NFR-06 and DATA-28 met, while DEVIATIONS lists them as not followed.

### Own tests

- 59 backend tests: unit tests, and integration tests against an embedded Fuseki with signed test tokens.
- 6 Vitest UI tests, a Playwright and axe check, and `smoke.sh`.
- JaCoCo: line 93.0%, branch 78.9%. `mvn verify` enforces 80% and 60%.
- No mutation score.

### Deviations from the rules (DEVIATIONS.md)

| Rules | What is not followed | Reason given |
|---|---|---|
| API-46 | Belgif problem types | D48 |
| D79 contract rules and BLG-oas-* | The service is stricter or wider than the OpenAPI, and the file was not updated | Decided (D39, D79) |
| OWT-A04, OWASP-03, ARCH-17 | No TLS, no encryption at rest | Local evaluation stack |
| ARCH-07 | Single replica | Accepted risk D81 |
| OPS-06, OPS-33, OPS-35, OPS-37 | No pipeline, no SAST | No pipeline for this component |
| OWT-A09, OWASP-10 | No alerting | No alerting stack |
| OWT-A07 | No rate limit | D46: the gateway applies it |
| OWASP-07, OWT-A03 | Keycloak image findings left open | No fixed upstream version |
| UX-01 and E6 | Plain Vue, not participant-fe-components | Library not available to the factory |
| CODE-39 | Incomplete `@throws` | Unchecked problem exceptions |
| BLG-int-conlang | No `Content-Language` | English only |
| API-01, DATA-26 to DATA-29 | No CDM | D2: the CDM is an input in review |
| NFR-06 | Backup not scheduled, no off-host copy | Operations task |
| REUSE-05 | Plain Keycloak, no authenticator plugin | Plugin not available |

Decisions implemented with a difference:

- **D46:** an identical version is detected by SHA-256, not by byte comparison.
- **D67:** an undeclared method on a known path gets 405 before 401.
- **D38 and D73:** an array issue name has no index.
- **D13:** the browser check uses a fixed base IRI.

### Scan results

| Target | CRITICAL | HIGH | Build's SECURITY-SCAN.md |
|---|---|---|---|
| deps (Maven, npm) | 0 | 0 | 0 / 0, nothing to fix |
| vm-factory-run7-backend-1.0.0 | 0 | 0 | 0 / 0 |
| vm-factory-run7-fuseki-6.2.0 | 0 | 0 | 0 / 0 |
| vm-factory-run7-ui-1.0.0 | 0 | 0 | 0 / 0 |
| quay.io/keycloak/keycloak:26.8.2 | 1 | 6 | 1 / 6, accepted with reasons |

The independent scan matches the build's scan exactly. The open Keycloak findings, and why they stay:

- **CVE-2026-114938 (CRITICAL) and CVE-2026-91403 (HIGH)**, in `java-21-openjdk-headless`. No fixed RPM exists, and the UBI micro image has no package manager.
- **Four sqlite-libs HIGHs.** No fix exists, and Keycloak does not use SQLite.
- **CVE-2026-102554 (HIGH)**, in Guava inside Keycloak. A fix exists (33.7.2), but replacing the library breaks Keycloak's augmentation, so it needs an upstream Keycloak release.

## 6. New spec gaps

These questions for the owners were not raised in steps 20 to 30. They come from the build log, the suite review (step 45), the design review (step 70c) and the failures. The numbers continue after R-065, the highest id in the current decisions; step 30 may renumber them.

1. **R-066. Proving a 500 and a key fetch outage.** How must SIMPL-18162/AC4 (500 on an internal error) and D44 (unknown kid while the identity provider is down) be shown? Through a fault-injection switch in the test profile, or through the build's own tests only? Today neither has an independent scenario.
2. **R-067. Undeclared method on a known path (D67, D17).** Is the answer 405 before 401 for every client, or 401 first without a token? D67 orders only unknown methods. Run 6 raised a related question (R-050), and the build chose 405 first.
3. **R-068. Issue names inside search bodies (D38, D52, D68, D73).** Three questions:
   - What is the issue `name` for a bad entry inside `namespaceUris` or `aliasUris`?
   - What is it for an unknown property inside a search item (`latest` or `items[0].latest`)?
   - Is an empty array an array error (`aliasUris`, the build's choice) or a "no oneOf branch" error (`body`)?
4. **R-069. Turtle parser strictness (D9).** Must a file whose last triple has no final dot be rejected, as RDF 1.1 requires? Jena's default parser accepts it, so the outcome depends on the parser. The build chose strict mode.
5. **R-070. Size limit of JSON bodies.** PATCH, both searches and content by namespace have no decided limit. The build chose 1 MB with 413. It should join Q2 with a tested limit.
6. **R-071. Number of multipart parts.** There is no decided limit. The build chose 200, with 400 above it.
7. **R-072. Stored form of description and changelog.** Are they stored trimmed or as sent? D37 applies its limits after trimming. D46 compares "trimmed description equals its description".
8. **R-073. Literal in S1 locations (D65).** Does "the literal as written" mean the lexical form only, or with quotes, language tag and datatype?
9. **R-074. Language tag case in S4 (D53).** Are `en` and `EN` one language? The build compares them case-insensitively.
10. **R-075. Base IRI for external vocabulary content.** D57 covers internal versions only. The build chose `<contentBase>/external-vocabularies/<Name>`.
11. **R-076. Content URLs that D69 does not cover**, for example `<contentBase>/content/<Name>/other#`. Are they internal, external or invalid? The build treats them as external.
12. **R-077. Integers beyond 64 bits** in `page` and in a version path. Should they give 400, or 404 and an empty page as the build answers now?
13. **R-078. HEAD and OPTIONS.** They are not declared, and the build answers 405. Confirm, given that the UI is same-origin and needs no CORS preflight.
14. **R-079. Optional properties of an external vocabulary** (description, sourceUri). Are they left out when absent, or sent as `null`?
15. **R-080. Charset of multipart text fields.** Is UTF-8 always assumed, or is a part `charset` honoured?
16. **R-081. D46 identity by hash.** Is SHA-256 equality acceptable as "byte equality"?
17. **R-082. Empty lists (D22).** Is `totalPages` 0 or 1 when `total` is 0 on the three list operations? D38 fixes 0 for searches only. The test writer relied on the OpenAPI example.
18. **R-083. Q1 throughput tolerance and meaning.** Is "20 reads per second sustained" met at 19.92? Is "1 write per second" an offered rate that may run concurrently?
19. **R-084. Version `lastUpdateDate` on a manual PATCH (D4, D25).** Does a manual PATCH of a version change that version's own `lastUpdateDate`? The test writer relied on an OpenAPI example.
20. **R-085. List columns (SIMPL-18161/AC1 against D23, D72, D75).** Which columns does the list have, and what is the header of the creator column?
21. **R-086. Target of the "Latest ACTIVE version" link (D36).** Does it open the version panel on the same page or another route?
22. **R-087. Audience and issuer checks (E5).** The build profile has one test client, which already carries the audience mapper. Add a second client, without the mapper, so that the 401 for a wrong audience or issuer can be tested?
23. **R-088. Concurrency control (D43, D81).** Should the lock move to a store-side conditional update on the latest version number? That would allow more than one replica (ARCH-07, MACH-06).
24. **R-089. Store access (E3).** Must Fuseki require credentials and sit on an internal-only network, with no unauthenticated update endpoint and no published port? The decisions are silent.
25. **R-090. Authorisation default (D16, D17).** Must a request that matches no declared operation be denied by default? Today it passes the role gate.
26. **R-091. Bounded store queries (NFR-05, Q4).** Must filtering, sorting and paging run in the store (`LIMIT`, `OFFSET`, `COUNT`), with a cap on search candidates?
27. **R-092. Observability (D67, NFR-08).** Are metrics required, in which format and on which port? Must the correlation id or `traceparent` be forwarded to the store and the identity provider?
28. **R-093. Model deliverables (D2, DATA-02, DATA-03).** Must the code writer deliver an LDM and PDM for an RDF store (a named-graph model or SHACL shapes)? Must it deliver an updated OpenAPI that reflects D79 (MACH-03, DATA-27)?

## 7. Compared with earlier runs

| Run | Date | Suite | Result | Fuzz findings | 70c MUST (met / partly / not met) | Own targets, Trivy | Keycloak, Trivy |
|---|---|---|---|---|---|---|---|
| run1 | 27 Oct 2026 | its own suite | 74 of 74 API, 22 of 22 UI | n/a | n/a | n/a | n/a |
| run4 | 17 Nov 2026 | independent | 379 of 386 | n/a | n/a | 17 CRITICAL | n/a |
| run5 | 18 Nov 2026 | independent | 463 of 468 | n/a | n/a | 0 / 0 | n/a |
| run6 | 19 Nov 2026 | independent, step 45 review | 428 of 432 (99.1%), 3 failed, 1 skipped | 6 | 22 / 20 / 7 | 0 / 0 | 1 C / 6 H |
| **run7** | 19 Nov 2026 | independent, step 45 review, load at Q4 | **420 of 424 (99.1%), 2 failed, 2 skipped** | **2** | **23 / 21 / 6** | 0 / 0 | 1 C / 6 H |

**Fixed since run 6.**

- The 5 MB store write that timed out against D44 now takes under 1 s. Content is stored in 64 KB base64 chunks. Both D71 scenarios pass, and the load test's 5 MB p95 is 960 ms.
- The UI question about where the browser shows a refused file over 5 MB was settled in the decisions (D62, D76). "A file over 5 MB is refused in the browser" passes.
- `verification.md` counts correctly this time. It merges scenarios by name, so the run 6 double count is gone.

**New in run 7.**

- The decisions grew from 65 to 93, including Q1 to Q6 and D62 to D81.
- The Q1 load test was measured for the first time, at the full Q4 volume, and met all targets.
- The D67 firewall answer is now decided as problem+json. This build does not deliver it, because Tomcat rejects the path before Spring does. Run 6 only raised the question (R-049).

**Unchanged.**

- The Keycloak image findings.
- The fuzz list's dependence on message wording.
- The design-review themes: single replica, no CDM, no pipeline, plain Vue UI, and an OpenAPI that does not reflect D79.

**Cost of this run's model steps:**

| Step | Cost |
|---|---|
| Write tests | $9.56 |
| Write code | $21.36 |
| Review 45a | $3.60 |
| Fix 45b | $11.09 |
| Design 70c | $2.65 |
| **Total** | **about $48.26** |
