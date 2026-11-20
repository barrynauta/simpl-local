# Suite review

## 1. In short

- **Reviewed:** all 298 scenario definitions (524 once outlines are expanded) in 13 feature files. That includes the `@ui` scenarios, which are Gherkin only, the 3 `@wip` scenarios and the store-down scenarios. I also read the step library, the fixtures, `COVERAGE.md` and the Locust load test. The behave dry run reports no undefined steps.
- **Overall:** the suite follows the decisions closely. It checks the fixed texts, the order of checks on writes (D51), the problem types (D48), paging and links (D22, D59, D78, D111) and the integer rules (D91, D112) well. It proves "nothing stored" through the name-filtered list, as D49 asks. Most findings are about sorts that cannot show a difference, and a few names or shapes the decisions leave open.
- **Findings by severity:** 1 blocker, 6 major, 9 minor (16 in total).
- **Findings by kind:**

| Kind | Count |
|---|---|
| Over-assertion | 3 |
| Under-assertion | 6 |
| Misreading | 1 |
| Proof that cannot work | 0 |
| Fragile | 2 |
| Traceability | 1 |
| Gaps | 3 |

## 2. Findings

| Id | Severity | Kind | Scenario | Decision or criterion | What is wrong | What the scenario should check instead |
|---|---|---|---|---|---|---|
| SR-01 | blocker | over-assertion | search.feature:269, "An empty URI array is rejected: <array>" | D38, D68 | It expects the issue name to be the array (`namespaceUris` or `aliasUris`). D68 says a body that matches no oneOf branch gives name `body`. An empty array fails the branch's minItems 1 (D38), so a build that follows D68 answers `body` and fails. | Assert 400, the type `badRequest` and an issue in `body` without a fixed name, until the owners decide (question 1). |
| SR-02 | major | under-assertion | create_version.feature:260, "The identical-version check runs after the field checks" | D46, D51 | The file is newly rendered (new marker) and the description is "Desc", so the request is not identical to version 1. A build that runs the identical check before the field checks also returns 400 here. | Send the file and description of version 1 with an empty changelog, and expect 400 with an issue named `changelog`, not 409. |
| SR-03 | major | under-assertion | list_vocabularies.feature:187, sort outline, rows creationDate, -creationDate, lastUpdated, -lastUpdated | D23, D58 | Three vocabularies created a few hundred ms apart usually share the same second. Ties sort by name ascending in both directions, so ascending and descending give the same order. A build that ignores these sort keys passes. | Space the creations more than 1 s apart, in an order that is not alphabetical (for example Charlie, then Alpha, then Bravo). Assert the exact name order for each sort value. |
| SR-04 | major | under-assertion | read_versions_and_content.feature:122, history sort, rows creationDate and -creationDate | D23 | The same problem: the three versions are created within about one second, ties sort by version descending in both directions, so the order is always 3, 2, 1. | Wait more than 1 s between versions and assert 1, 2, 3 for `creationDate` and 3, 2, 1 for `-creationDate`. |
| SR-05 | major | under-assertion | ui_list_and_details.feature:62, "Sort by the column <column>" (Status, Date created, Last updated) | D23, D50 | Both vocabularies are ACTIVE and created in the same second, so each column holds equal values. The ascending and descending orders are then the same, and an unsorted list passes. | Make one vocabulary DEPRECATED and space the creations and updates more than 1 s apart. State the expected row order for each click. |
| SR-06 | major | over-assertion | list_vocabularies.feature:240, rows `page=1&page=2` and `sort=name&sort=-name` | D52, D21 | D52 says only that a repeated parameter returns 400. "Exactly 1 issue in query named <param>" is fixed by D21 for unknown parameters only. | For the repeated rows, assert the 400 problem with type `badRequest` only, or get the shape decided (question 2). |
| SR-07 | major | misreading | load/locustfile.py:95, write user (1 user, `constant_throughput(1)`) | D99, Q1 | D99 defines throughput as an offered rate. One closed-loop writer cannot send 1 write per second when a write takes longer than 1 s, and Q1 allows up to 3 s p95. A build that meets Q1 can then fail `throughput_met`. | Spread the 1 write per second over several writer users (for example 3 users at 0.33 per second, below the D92 cap of 4), or use an open-loop schedule. Compare the achieved rate with the offered rate. |
| SR-08 | minor | fragile | steps/api_steps.py:50, `content_base()` probe | E4 | When `CONTENT_BASE_URL` is unset, the probe creates `<prefix>BaseProbe` under the prefix of whichever scenario first resolves `${base}`. A scenario that resolves `${base}` before a `name=${prefix}` count would see one item too many. No current scenario does this, but it depends on scenario order and tag filters. | Give the probe its own run-level prefix (for example `Vt<run>Probe`). |
| SR-09 | minor | fragile | ui_list_and_details.feature:53, "Filter by creation date" | D20, D72 | It uses "today's UTC date" taken when the step runs, so it fails when the scenario crosses midnight UTC. | Take the date from the vocabulary's `creationDate` read through the API. |
| SR-10 | minor | over-assertion | external_vocabularies.feature:11 and :182, "response has no field validationWarnings" | D30, D41 | The decisions say registration gives no warnings and returns the ExternalVocabulary body. They do not forbid an empty `validationWarnings` array. | Use "is absent or empty". |
| SR-11 | minor | under-assertion | create_vocabulary.feature:223, "more than 64 parts" | D110, Q2 | The extra parts are repeated `description` fields. A build with no part limit that rejects repeated fields also passes. There is no test at the limit, and none is possible with valid fields. | Note in `COVERAGE.md` that this limit cannot be isolated through the API, or ask the owners (question 6). |
| SR-12 | minor | gap | external_vocabularies.feature (none), status.feature:130, create_vocabulary.feature:275 | Q2, D76, D110 | Three tests at the limit are missing: an external file of exactly 5 242 880 bytes (`COVERAGE.md` claims it exists), a multipart request of exactly 6 MB, and a JSON body of exactly 1 MB. Only the cases above the limit exist. | Add the three cases at the limit. A 1 MB JSON body can be padded with whitespace so it stays valid. Otherwise, correct `COVERAGE.md`. |
| SR-13 | minor | gap | create_vocabulary.feature:50 (CPSV), validation.feature | D12 | No scenario proves that the `owl` and `xsd` namespaces give no `externalReferenceSkipped`: the CPSV file uses `owl`, but its warnings are not checked. The internal reference form "followed by nothing" is not tested either. | Assert "no validation warnings" on the CPSV scenario. Add a file that declares `xsd`. Add a reference `<base>/content/<R>/versions/1` with no separator. |
| SR-14 | minor | under-assertion | search.feature:139 and :261, unknown property in a search body | D52 | Only status and type are checked. D52 fixes one issue per unknown name, in `body`. | Assert an issue in `body`, with the name left free where D52 does not fix it. |
| SR-15 | minor | traceability | ui_list_and_details.feature:191 (@D85); store_down.feature:8 (@ARCH-35); protocol.feature:67 (@D17); access_control.feature:8 (@AC_SIMPL-18173_AC5 on all 18 rows); external_vocabularies.feature:49 (@D66) | as named | Each tag does not match what the scenario checks. D85 is about store LIMIT, not UI paging (D35). ARCH-35 is shown by the 401 and 403 scenarios at :53 and :60, not by the 503 outline. D17 does not cover 400 sanitising. SIMPL-18173/AC5 is about POST .../versions only. D66 is about 404 texts, not successful downloads. | Move or drop these tags. |
| SR-16 | minor | gap | read_versions_and_content.feature, external_vocabularies.feature | D63 | No 406 check on the external vocabulary GET operations, and no check that a JSON operation accepts a request with no Accept header. | Add one row for each. |

## 3. Questions for the decisions

1. **D38 against D68.** For `{"namespaceUris": []}` (an empty array, which fails every oneOf branch), is the issue named after the array or `body`?
2. **D52.** For a query parameter given twice, is the answer an InputValidationProblem with exactly one issue (`in: query`, `name`: the parameter)?
3. **D84 against D77, D79 and D55.** For a known method that is not declared on a declared path (for example PUT /vocabularies), is the answer 405 or 404 with a token, and 405 or 401 without one? The suite expects 405 with a token (protocol.feature:12, :175 and :335 in search and external). D84 read literally gives 404.
4. **D43 against D92.** D43's test sends 5 parallel new versions, but D92 refuses the fifth upload on one instance with 503. The suite sends 4. Confirm that 4 is the intended test, or that D92 should queue rather than refuse.
5. **D65, rule S1.** Is "the literal as written" with its quotes and language or datatype tag, or the lexical form only, as in the OpenAPI example? The suite accepts both.
6. **Q2 and D110.** A request with 64 parts cannot be valid, so no test at the limit is possible. Is a test above the limit enough for this limit?

## Answers of the test writer (45b)

SR-01: fixed. The scenario now asserts 400, the badRequest type and an issue in `body`, with no fixed name, because D38 and D68 give different names for an empty array.
SR-02: fixed. It now sends the file and description of version 1 with an empty changelog, and expects 400 with an issue named `changelog` and still only 1 version.
SR-03: fixed. Creations and updates are now more than 1 s apart (Charlie, Alpha, Bravo, then Bravo deprecated, then a new version of Alpha). Each sort value has its exact name order.
SR-04: fixed. Versions are now more than 1 s apart and version 1 is reactivated. Each sort value has its exact version order: creationDate gives 1, 2, 3 and -creationDate gives 3, 2, 1.
SR-05: fixed. The creations are more than 1 s apart, one vocabulary is DEPRECATED, and each column states the expected row order for both clicks.
SR-06: fixed. The repeated-parameter rows moved to their own outline, which asserts only the 400 badRequest problem (D52).
SR-07: fixed. The 1 write per second is now spread over 3 writer users at 1/3 per second each, below the D92 cap of 4. The report still compares the achieved rate with the offered rate.
SR-08: fixed. The probe now uses a run-level name, `Vt<run>Probe`, outside every scenario prefix. If that name already exists (409), the probe reads it back instead.
SR-09: fixed. The date filter now uses the vocabulary's `creationDate` read through the API.
SR-10: fixed. Both scenarios now check that `validationWarnings` is absent or empty.
SR-11: fixed. `COVERAGE.md` (Q2 row) now says the 64-part limit of D110 cannot be isolated through the API and that only the case above the limit exists.
SR-12: fixed. I added three at-limit scenarios:
- an external file of exactly 5 242 880 bytes, expecting 201;
- a multipart request of exactly 6 MB, expecting the description field error and not 413. No valid request can reach 6 MB, and D51 checks size before the field rules, so a field error proves the size check passed;
- a JSON body of exactly 1 MB padded with spaces, expecting 200.
SR-13: fixed. The CPSV scenario now asserts no validation warnings. I added a scenario with a file that declares rdf, rdfs, owl, xsd and skos, and one with an internal reference that has nothing after the version number.
SR-14: fixed. Both unknown-property scenarios now assert an issue in `body`, with the name left free.
SR-15: fixed.
- Dropped @D85 from the UI paging scenario.
- Moved @ARCH-35 from the 503 outline to the 401 and 403 store-down scenarios.
- On the sanitising scenario, replaced @D17 with @D48, which fixes the problem type it checks.
- @AC_SIMPL-18173_AC5 now sits only on a separate Examples block with the POST .../versions row. `tools/coverage.py` now reads tags on Examples blocks.
- Dropped @D66 from the successful download outline.
SR-16: fixed. I added 406 rows for GET on the external vocabulary list, on one external vocabulary and on its content. I also added a JSON read with no Accept header, which expects 200.

Dry run: 533 scenarios, 3159 steps, 0 undefined steps; none failed. Behave 1.3.3 still labels 2 features "failed" in dry-run mode, which is the same scenario-outline status quirk as before, not a real failure. `COVERAGE.md` is regenerated, every gap has a reason, and no file contains an em-dash or en-dash.
