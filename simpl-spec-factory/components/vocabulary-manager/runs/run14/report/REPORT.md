# Final report: Simpl-Open vocabulary-manager, run 14 (23 Nov 2026)

## 1. In short

- **What was built:** the Vocabulary Manager. The stack has a Spring Boot 4.1.3 backend on Java 21 (two replicas), Apache Jena Fuseki 6.3.0 as the store, Keycloak 26.8.2, and a Vue 3 UI (PrimeVue unstyled, Tailwind) behind nginx. It was built from 224 decisions (D1 to D210, E1 to E8, Q1 to Q6) and 405 rules, on engine 68e5ac2.
- **Score:** 664 of 668 scenario cases passed (99.4 %). That is 581 of 582 API, 15 of 15 store-down and 68 of 71 UI. One more case (`@wip`, SIMPL-18162/AC4) was not run.
  - All 85 live criteria have a scenario. 84 of them have only passing scenarios.
  - 157 of 224 decisions have a scenario. 152 have only passing scenarios.
- **The 4 failures:**
  - 3 come from one build bug. The confirmation dialog of D209 and D210 does not open with focus on "Cancel".
  - 1 is a test bug. The test client re-encodes the malformed `%ZZ` before sending it, so the D158 check never reaches the server.
- **Q1 is met for the first time on the full D99 seed** (1 000 vocabularies with 20 versions each, 5 minutes). The list p95 is 110 ms against a 300 ms target. In run 13 it was 347 ms.
- **Fuzzing:** 0 server errors. All 18 findings have one cause: `401 No valid access token`. The access token lives 300 s and the fuzz run lasted 300 s. The published OpenAPI documents no 401 at all, and `fuzz-known.yaml` does not list the 401 that D17 and D79 decide. None of the 18 is a build bug.
- **Scans:** 0 CRITICAL in every target. The only HIGH findings are 6 in the reused Keycloak image, and the build accepts each one with a reason.
- **Design review (70c):** of 52 MUST rules, 26 are met, 20 partly met, 2 not met and 4 not applicable. Both "not met" rules are set aside by decisions. The main findings:
  - the HTTP front is largely own routing code that `REUSE.md` does not record;
  - rejections are logged unevenly;
  - the proxy and the store do not log the correlation id;
  - the single Fuseki instance is not recorded as a single point of failure.
- **What it means:** the API behaviour matches the decisions. The remaining work is one UI focus fix, one test client fix, a fix to the fuzz setup, and the spec questions in section 6.
- **Effort:** about 2 h 2 min wall clock (20:33 to 22:35) and about 57.4 USD of agent cost before this report.

## 2. Results

### Scenarios per feature

| Kind | Feature | Passed | Failed |
|---|---|---|---|
| API | Role-based access control of the Vocabulary Manager API | 96 | 0 |
| API | Register, read, download and remove external vocabularies | 67 | 0 |
| API | Health of the Vocabulary Manager API | 3 | 0 |
| API | HTTP rules that hold for every operation | 6 | 1 |
| API | Vocabulary references and validation warnings | 33 | 0 |
| API | Search vocabularies and external vocabularies | 54 | 0 |
| API | Change the vocabulary status and a version status via the API | 45 | 0 |
| API | Syntax and semantic validation of uploaded vocabulary content | 28 | 0 |
| API | Create a new vocabulary via the API | 55 | 0 |
| API | Filter, sort and page the vocabulary list via the API | 56 | 0 |
| API | Read vocabularies, versions, history and content via the API | 94 | 0 |
| API | Create a new version of a vocabulary via the API | 44 | 0 |
| store-down | Behaviour when the vocabulary store is down | 15 | 0 |
| UI | Accessibility of the UI (Q6, NFR-10) | 7 | 0 |
| UI | External vocabularies screen in the UI | 4 | 1 |
| UI | Vocabulary details, version history and downloads in the UI | 13 | 0 |
| UI | Create and new version forms in the UI | 23 | 0 |
| UI | Vocabulary list in the UI | 21 | 2 |
| | **Total** | **664** | **4** |

axe found 0 violations on all 6 UI pages it checked (list, create form, details, new version form, external list, external form).

### Per tag kind

| Tag kind | Passed | Failed |
|---|---|---|
| `@negative` | 405 | 1 |
| not negative | 259 | 3 |
| criterion tag (`@AC_...`) | 273 | 2 |
| decision tag (`@D...`) | 664 | 4 |
| environment tag (`@E...`) | 39 | 0 |
| quality tag (`@Q1`, `@Q2`, `@Q6`) | 71 | 0 |
| rule tag (API, OWASP, SEC, NFR, BLG, ARCH, MACH, OWT) | 217 | 0 |

### Criteria and decisions

- **Criteria:** 85 of 85 live criteria have a scenario. 84 of 85 have only passing scenarios. The exception is SIMPL-18174/AC1: both of its scenarios fail on the dialog focus.
- **Decisions:** 157 of 224 have a scenario, and 152 of 224 have only passing scenarios. The 5 decisions with a failing scenario are D3, D60, D158, D209 and D210.
  - `COVERAGE.md` gives a reason for each of the 66 decisions without a scenario. Most are build-side or cannot be observed over HTTP: process, logs, store layout, library choice and platform items.
  - Verification counts 67, because D179 has only the `@wip` scenario.
- **Rules tested by scenarios:** 20 rules, all passing. Among them are API-46 (59 cases), OWASP-02 (71), BLG-err-problem (64), NFR-04 (64) and SEC-05 (54).

## 3. Failures

### Failing scenarios

**1. HTTP rules that hold for every operation: "A query with bad percent-encoding gets 400 badRequest without issues"** (`http_rules.feature:18`, tags D158, negative)

- **Expected:** `GET /vocabularies?name=%ZZ` gives 400, type `urn:problem-type:belgif:badRequest`, with no issues (D158).
- **Received:** 400 `badRequest` with one issue `schemaViolation` on query parameter `name` ("must have 1 to 64 letters or digits").
- **Cause: a test bug.** Python `requests` re-quotes an invalid percent escape when it prepares the URL. `%ZZ` goes out as `%25ZZ`, which is a well-formed encoding of the text `%ZZ`. I checked this: `requests.Request('GET', '...?name=%ZZ').prepare().url` ends in `name=%25ZZ`. So the server received a valid encoding, and it correctly rejected the value under D19 with one issue. The build's decoder (`QueryParameters.decode`) does return 400 `badRequest` with no issues for a raw `%ZZ`. The scenario did not test what it means to test.
- **Step 45 did not flag this scenario.**
- **Fix:** send the request with a prepared request whose URL is set after preparation, or with `http.client`, so the bytes `%ZZ` reach the server unchanged.

**2. External vocabularies screen in the UI: "The admin removes an external vocabulary after confirming"** (`ui/external_vocabularies_ui.feature:39`, tags ui, D210, D209)

**3. Vocabulary list in the UI: "The admin deprecates a vocabulary after confirming"** (`ui/vocabulary_list_ui.feature:155`, tags ui, AC_SIMPL-18174_AC1, D60, D209, D3)

**4. Vocabulary list in the UI: "The admin activates a DEPRECATED vocabulary"** (`ui/vocabulary_list_ui.feature:168`, tags ui, AC_SIMPL-18174_AC1, D60, D209)

- **Expected:** the dialog opens as a modal with focus on "Cancel" (D209; D210 reuses the D209 dialog).
- **Received:** the dialog, its title, text and Cancel button are all there, but Cancel is not focused. Playwright saw "inactive" 14 times over 5 s.
- **Cause: a build bug against D209.** `ConfirmDialog.vue` focuses Cancel once, on `nextTick` after `visible` turns true. The PrimeVue `Dialog` it wraps runs its own focus handling when its enter transition ends.
  - The probable result is that focus moves away from Cancel after the component set it. This is inferred from the code and not proven.
  - `QUALITY.md` reports the opposite ("The dialog opens with focus on Cancel"). Its own check ran with the page CSP bypassed, so timing in the CSP-enforced UI may differ. D123 notes that the CSP blocks PrimeVue's base `<style>`.
- **The decision is clear, so this is not a spec gap.** D209 is new on 23 Nov 2026, so this run is the first to test it. Step 45 did not flag these scenarios.
- **Fix:** focus Cancel after the dialog has finished opening (PrimeVue's `@show` or `@after-show` event), or turn off PrimeVue's own auto-focus.

### Fuzz findings (`fuzz-summary.md`)

There are 18 findings, all `ST103 Undocumented status code`, all `401 No valid access token`. They cover:
- 13 operations: `POST /external-vocabularies/content`, `POST /vocabularies/searches`, `POST /vocabularies/{vocabularyName}/versions`, `GET /content/{vocabularyName}`, `GET /content/{vocabularyName}/versions/{version}`, `GET /vocabularies/{vocabularyName}/versions`, `GET /external-vocabularies/{name}`, `GET /vocabularies/{vocabularyName}`, `GET /vocabularies/{vocabularyName}/versions/{version}`, `PATCH /vocabularies/{vocabularyName}`, `PATCH /vocabularies/{vocabularyName}/versions/{version}`, `GET /external-vocabularies/{name}/content` and `DELETE /external-vocabularies/{name}`;
- 5 stateful sequences.

| Cause | Explanation |
|---|---|
| Trigger: fuzz setup | Keycloak `accessTokenLifespan` is 300 s (`realm-simpl.json`). The fuzz run lasted 300.02 s with one token. Once the token expired, every request got 401. The build answered correctly under D17. The fuzz cases after expiry tell nothing about the other checks. |
| Contract problem in the OpenAPI | `vocabulary_openapi-tier1.yaml` documents no 401 on any operation. The decided contract (`openapi-decided.yaml`) has it. |
| A decision the fuzz list misses | D79 lists "401 and 403 on every secured operation (D17)" as expected behaviour that fuzzing must treat as known. `fuzz-known.yaml` has no ST103 entry for 401. |

**Verdict:** this is not a build bug. Two factory fixes are needed:
- refresh the token during fuzzing, or set the fuzz `max_time` below the token lifetime;
- add D17 and D79 (ST103 401) to `fuzz-known.yaml`.

Server errors: 0.

The other 14 failed checks are explained by decisions D9, D19, D20, D22, D37, D49 and D52. One label is wrong: the D52 rows for the PATCH and content lookup operations cite "D38 limits on search bodies", which does not apply to them.

## 4. Traceability

| Measure | Share |
|---|---|
| Live criteria with a passing scenario (all scenarios pass) | 84 of 85 (99 %) |
| Live criteria with a scenario | 85 of 85 (100 %) |
| Live criteria with code, according to `TRACEABILITY.md` | 85 of 85 (100 %): every story in scope (SIMPL-18161 to SIMPL-18196, SIMPL-18452, SIMPL-18559) has implementation and own tests named per criterion |

- **Criteria with nothing:** none.
- **Criterion with no passing scenario:** SIMPL-18174/AC1. It has code (`ListView`/`DetailsView` with `ConfirmDialog`, D209) but fails on the focus bug above.
- **SIMPL-18162/AC4 (500 with details):** passes through `store_down.feature:15`. Its other scenario is `@wip`, because a 500 cannot be caused from outside (D179).
- **Minor:** `TRACEABILITY.md` lists SIMPL-18183 twice.

## 5. Quality of the build

### Quality targets (NFR-13)

| Q | Target | Measured | Verdict |
|---|---|---|---|
| Q1 | Reads p95 ≤ 300 ms; new version p95 ≤ 3 000 ms; 5 MB write ≤ 30 s (D71); ≥ 95 % of 20 reads/s and 1 write/s (D99); no errors | `load.json`, full D99 seed of 1 000 × 20, 300 s after a 30 s warm-up. Read p95: list 110 ms, details 18 ms, versions 22 ms, version 20 ms, search 22 ms, downloads 25 and 21 ms, external list 18 ms. New version p95 430 ms. Three 5 MB writes, p95 4.7 s. Achieved 19.97 reads/s and 1.0 write/s, 0 errors | **Met** (all 11 checks) |
| Q2 | Every limit has a defined error (NFR-04) | 64 Q2-tagged cases pass. The 4 parallel uploads of D92 are tested only by the build's own test (`fifthUploadGets503BeforeTheBodyIsRead`) | **Met**, except the D92 slot count from outside (not tested, timing-dependent) |
| Q3 | 99.5 % availability; RTO 30 min; RPO 24 h; tested backup and restore | The build's round trip passed: 316 quads, restore 16 s, API back about 3 s later. Daily backup gives RPO 24 h | **Recovery met. Availability not measured** (D191; needs production monitoring; stated in `QUALITY.md`). No independent step 70 restore result is in `report/` |
| Q4 | Design load; scaling | The D99 load ran on two backend replicas with 0 errors. 8 parallel versions through two instances never reuse a number | **Met for the backend.** 70c: the single Fuseki is a single point of failure and is not recorded (ARCH-07, ARCH-19) |
| Q5 | Line ≥ 80 %, branch ≥ 60 %; 0 HIGH/CRITICAL in own targets; one store query per upload | JaCoCo 94.5 % lines, 86.3 % branches (`QUALITY.md` says 86.2 %). Step 70 Trivy: own targets 0/0 | **Met** |
| Q6 | Accessibility (NFR-10) | axe 0 violations on 6 pages; 7 of 7 accessibility cases pass. The D209 "focus on Cancel" keyboard rule fails in 3 cases | **Partly met** |

- **Mutation score:** not measured.
- **Own tests:** 39 backend tests, plus 14 Vitest component tests in the UI.
- **Own fuzz run:** the build's own run passed 1 438 of 1 438 cases. Step 70's independent run is reported in section 3.

### Reuse

- **`REUSE.md` records the reused products and libraries:** Keycloak, Fuseki/TDB2, Jena RIOT and SHACL, Nimbus JOSE+JWT, Spring MVC with Tomcat multipart, Jackson 3, Resilience4j, Micrometer with Brave, nginx, Vue/PrimeVue/Tailwind, and N3.js.
- **Own code it declares:** `RealmKeyCache` (D88, D129, D174), `SparqlTemplate` (D94 deviation), `StoreClient`, `GatekeeperFilter`, and the request validation classes (D93).

70c verdicts on the REUSE rules:

| Rule | Verdict | Reason |
|---|---|---|
| REUSE-01 (MUST) | partly | Bearer extraction, role checks and admission are own code in a servlet filter. There is no Spring Security. |
| REUSE-02 | partly | Own query parser and percent-decoder. Spring `UriUtils.decode` would keep `+` as a plus, which undermines the stated reason. |
| REUSE-03 | met | Minor: retry loops and `UploadSlots` could be Resilience4j Retry and Bulkhead. |
| REUSE-04 (MUST) | met | |
| REUSE-05 | met | |
| REUSE-06 | met | |
| REUSE-07 (MUST) | partly | No row for `RouteMatcher` plus `Operation` (about 280 lines, D93), `TraceHeaders`, or the catalogue entries `http-client` and `participant-shared-code`. |
| REUSE-09 | met | |

### Main 70c findings and disagreements

1. **A second router beside Spring MVC.** `GatekeeperFilter`, `RouteMatcher`, `Operation` and `QueryParameters`, plus an nginx allow map, make three route tables, kept equal only by `ContractTest`. `REUSE.md` says routing is done by Spring MVC (D133). This is a disagreement with the build.
2. **Rejections are logged unevenly** (OWT-A09, OWASP-10, CODE-34). The filter's 400, 406, 413 and 415, the upload-slot 503 and the key-refetch 503 leave no log line. `DEVIATIONS.md` claims "security events are logged (WARN)", which holds only for 401 and 403.
3. **The correlation id stops at the backend** (MSV-09). The nginx and Fuseki logs carry no correlation or trace id. `QUALITY.md` implies the opposite.
4. **The single Fuseki and the list sort cost** (ARCH-07, ARCH-19, NFR-05). The list sort still sorts every match per call (D188 deviation). Q1 holds today at the D99 seed.
5. **No security headers on API responses** (OWA-API8, OWT-A02). There is no `X-Content-Type-Options` and no `Cache-Control: no-store`. This is not recorded.

70c agrees with the recorded deviations for D94, D188, D99, D123 and the D185 platform items. It also proposes moving the API and DATA rules that judge inherited input (the published OpenAPI and the CDM) out of step 70.

### Deviations from the rules (`DEVIATIONS.md`)

- **Set aside by a decision:**
  - API-46 for 400, 404, 500 and 503 (D48)
  - CODE-11 (D135, D187)
  - CODE-21, CODE-52 and SEC-11 (D105)
  - OWT-A04, OWASP-03, API-25 and ARCH-17 (D185, D165)
  - OWT-A07, OWT-A09 and OWASP-10 alerting (D164, D185, D186)
  - D83 store admin (D193)
  - CODE-01, OPS-06, OPS-33, OPS-35 and OPS-37 (D95)
- **Not followed, without a decision:** BLG-int-conlang, CODE-39, DATA-26 to DATA-29, UX-01 and API-01.
- **Decisions implemented differently:** D94, D133, D99 (build-side load only; step 70 used the full seed), D188 and D123.
- **Missing rows according to 70c:** CODE-33 (D104), OWA-API8, ARCH-22, DDD-03, ARCH-07 and ARCH-19.

### Scan results

| Target | CRITICAL | HIGH | What `SECURITY-SCAN.md` says |
|---|---|---|---|
| deps (Maven, npm) | 0 | 0 | 0/0 before and after |
| backend image | 0 | 0 | 0/0 |
| fuseki 6.3.0 image | 0 | 0 | 0/0 |
| proxy-ui image | 0 | 0 | Fixed from 1 HIGH (CVE-2026-86144, libxml2) by removing the unused nginx modules and libxml2 |
| keycloak 26.8.2 (third party) | 0 | 6 | 6 HIGH accepted with reasons: CVE-2026-102554 (guava inside the distribution), CVE-2026-91403 (OpenJDK, no fix), CVE-2026-119373, -119374, -119379 and -119586 (sqlite-libs, no fix, not reachable). 26.8.2 is the newest release |

The step 70 scan confirms the build's own counts exactly.

## 6. New spec gaps

These are questions for the owners that steps 20 to 30 did not raise. Number them as `R-nnn` in step 30.

1. **D94, SPARQL building.** `ParameterizedSparqlString` throws on a literal that contains a variable name (for example "see ?now"). Is "values escaped by Jena's `NodeFmtLib` in fixed templates" an accepted way to meet D94?
2. **D133 and D168 conflict.** The servlet container decodes `+` as a space, so query parameters cannot be bound by the framework and keep `+` a plus. Which decision wins? If D168 wins, may the build use Spring `UriUtils` on the raw query instead of an own parser (70c, REUSE-02)?
3. **D133, D178 and D206, one route table.** Today there are three: the `Operation` enum, Spring MVC and the nginx map. Should one source (for example `openapi-decided.yaml`) generate the others? Or may the build use Spring Security with a custom filter order?
4. **D65 and D181, location text.** For rules S3, S4 and S5, does `resource '<property IRI>'` mean literal angle brackets or a placeholder? The build writes no brackets.
5. **D67, unknown path with a token.** Is 404 right for any valid token, including a token with no role? Or is it 403 when the role is missing?
6. **D63, Accept on DELETE.** DELETE answers 204 without a body. Do the JSON Accept rules apply, so that `Accept: text/html` gives 406?
7. **D92, upload slots for external registration.** Must `POST /external-vocabularies` also take an upload slot? Otherwise, how large must `/tmp` be? 70c computes that 11 parallel 6 MB registrations exceed the 64 MB tmpfs.
8. **D51 and D169, order on POST .../versions.** Must a file-part 413 come before a 400 for an invalid name in the path? The parts must be read to know the size.
9. **D21, several invalid known parameters.** Report only the first failing one (the build) or one issue each, as for unknown parameters?
10. **D83 and D193, Fuseki admin endpoints.** The default Fuseki command opens `/$/datasets` and `/$/backup` without login. Should a decision require "Fuseki main" with `fuseki:allowedUsers` at server level, and say which users may call `/$/compact` and `/$/ping`?
11. **D123, CSP and PrimeVue.** PrimeVue unstyled still injects a base `<style>`, which the CSP blocks, with console errors. Accept the errors, allow a hash, or change the component library?
12. **D89, restore window.** After a restore the API answers 503 for a few seconds while nginx re-resolves the backends. Is that acceptable within the RTO, and must `restore.sh` wait for the API?
13. **E8 and D132, token clock skew** (step 45, question 1). How much skew may the service allow on `exp` and `nbf`? The suite now waits `expires_in + 70` s to fit the 60 s library default.
14. **D158 and D178, size limits** (step 45, question 2; run 13 gap 19). At what URL length must the answer be 414, and at what header size 431?
15. **D60, Cancel target** (step 45, question 3). Does Cancel always go to the list route, or back in history?
16. **D55 and D48, problem titles** (step 45, question 4). Are 'Bad Request', 'Resource Not Found' and 'Internal Server Error' binding or free?
17. **D10, warnings in a 400** (step 45, question 5). May a 400 `VocabularyValidationProblem` carry `validationWarnings`?
18. **D210 and E4, finding rows on the external screen** (step 45, question 6). Should the screen get a name filter, given that the store is never reset?
19. **Logging of rejections (70c).** Should every rejection write one log line at a fixed level through one path, wherever it is raised? Should D173 be extended so that the proxy and the store log the correlation id and trace id?
20. **API response headers (70c).** Decide `X-Content-Type-Options: nosniff` and `Cache-Control: no-store` on authenticated API reads, and whether to add `Content-Language` (BLG-int-conlang).
21. **Single store instance (70c).** Is one Fuseki an accepted risk under ARCH-07 and ARCH-19? D188 asks for an ordered index that TDB2 cannot provide. Should it be rewritten, for example as top-N sorting plus a store size up to which Q1 must hold?
22. **D79, published contract.** Put 401 and 403 on every secured operation in the next OpenAPI version. The published file still has none, which produced all 18 fuzz findings once the token expired.
23. **DDD-03, glossary scope (70c).** Must the glossary cover every schema of `openapi-decided.yaml` (17 of 28 are missing)? Or may wrapper schemas point to their business term?

Factory notes, which are not owner questions:
- the test client must send malformed percent escapes raw;
- the fuzz run must outlive or refresh its token, and `fuzz-known.yaml` must list the D17 401;
- the D52 rows in `fuzz-known.yaml` cite D38 where it does not apply.

## 7. Compared with earlier runs

| Run | Cases passed | Q1 list p95 | Build bugs | Notes |
|---|---|---|---|---|
| run 14 | 664 of 668 (99.4 %) | 110 ms, full D99 seed, met | 1 (D209 dialog focus, 3 cases) | 1 test bug (D158 harness). 18 fuzz findings, one cause. 70c MUST 26/20/2/4 |
| run 13 | 551 of 552 (99.8 %) | 347 ms, missed | 1 (D76, now D169) | 1 fuzz finding (D91). 70c MUST 30/16/2/4 |
| run 12 | 466 of 470 | 280 ms, met | 2 (D158, D52/D76) | 1 test bug |
| run 11 | 547 of 549 | met | 1 (D18) | harness tag defect |
| run 4 | 379 of 386 | | | Trivy 17 CRITICAL |

- **Better:**
  - Q1 now holds on the full D99 seed, with the list p95 down from 347 ms to 110 ms. This follows the denormalised vocabulary nodes (D188 deviation).
  - The D76 multipart bug of run 13 is fixed: all 7 D169 cases pass.
  - The suite grew from 552 to 668 cases, and the decisions from 180 to 224 (D168 to D210, E8).
  - Decisions with a scenario went from 139 to 157.
- **New:** D209 and D210 came from the side-by-side click-through of 23 Nov 2026. This run is the first to build them, and all 3 UI failures sit there.
- **Worse:**
  - The fuzz findings went from 1 to 18. This is a setup effect (token lifetime), not a build regression.
  - 70c rates 4 fewer MUST rules as met (26 against 30), mainly from findings in logging, reuse and availability.
- **Steady:**
  - axe 0 violations, as in runs 11 to 13.
  - 0 CRITICAL in every target and 6 accepted Keycloak HIGH, as in run 13.
  - The D158 percent-encoding scenario fails again, as in run 12. This time the cause is the test client, not the build.
