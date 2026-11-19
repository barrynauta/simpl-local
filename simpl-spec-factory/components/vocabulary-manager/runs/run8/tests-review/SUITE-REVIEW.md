# Suite review

## 1. In short

- **Scope:** 238 scenarios (198 api, 5 store-down, 34 ui, 1 wip), counting each outline once. That is 452 scenarios with the outlines expanded. A `behave --dry-run` parses all of them and finds no undefined steps.
- **Overall:** traceability to the decisions is close and careful. The "nothing stored" proofs use the name filter or the namespace search, as D49 and E4 ask. Fixed texts are checked exactly where a decision gives them, and left free where D55 leaves them free.
- **Findings by severity:** 0 blockers, 7 majors, 14 minors.
- **Findings by kind:**

| Kind | Count |
|---|---|
| Over-assertion | 4 |
| Under-assertion | 8 |
| Gaps | 4 |
| Fragile | 3 |
| Traceability | 2 |
| Misreading | 0 |
| Proof that cannot work | 0 |

## 2. Findings

| Id | Severity | Kind | Scenario | Decision / criterion | What is wrong | What to check instead |
|---|---|---|---|---|---|---|
| SR-01 | major | under-assertion | `new_version.feature:45` (line 60), "The vocabulary shows the latest version number..."; `status_change.feature:10` (line 24), "Deprecate a vocabulary..."; `status_change.feature:173` (line 182), "A version status change updates the last update date of the vocabulary" | D25 (lastUpdateDate changes on a new version and on either status change) | All three use "is not before the remembered". Times are to the second (D58), so the old value passes. A build that never updates the vocabulary's lastUpdateDate passes all three. | Wait past a second boundary before the change, then assert "is later than". For the new version, assert it equals the new version's creationDate. For a PATCH, assert it equals the PATCH response's lastUpdateDate. |
| SR-02 | major | gap / under-assertion | No scenario. Nearest: `versions_and_content.feature:133`, `search.feature:28`, `list_vocabularies.feature:17` | D5 ("latest" means the highest number, whatever its status) | In every scenario the latest version is also the latest ACTIVE version. A build that uses the latest ACTIVE version for GET /vocabularies, GET /vocabularies/{name}, GET /content/{name}, search latest=true and latestVersionHref passes. | Create 2 versions. PATCH v2 to DEPRECATED and v1 back to ACTIVE. Expect version 2, v2's description and v2's bytes in all five places. |
| SR-03 | major | under-assertion | `list_vocabularies.feature:167` (creationDate and lastUpdated rows); `versions_and_content.feature:76` (creationDate rows) | D23, D58 | Setup creates the data within about one second, so timestamps tie. The sort check then only verifies tie-breaking. A build that ignores the sort direction on dates passes. | Create the items in different seconds and assert the exact order, for example "Charlie,Alpha,Bravo" for -creationDate. |
| SR-04 | major | under-assertion | `list_vocabularies.feature:128`, "Date filters with a date-time match the value to the second" | D20 | Only the vocabulary under test carries the uid, so a build that treats a date-time as a whole day passes. | Add a second vocabulary created in a later second and expect it excluded. Or expect an empty result for the same date-time plus one second. |
| SR-05 | major | over-assertion | `search.feature:97` outline, row `{"items":[{"name":..,"latest":true}]}` | D52, D68 | Expects issue name `items[0].latest`. D52 gives "one issue per unknown name". D68 fixes indexed names only for `.name` and `.version`. A build that reports `latest` follows D52 and fails. | Accept either `latest` or `items[0].latest` until the owners decide (question Q1). |
| SR-06 | major | over-assertion | Step "the response is a problem with status N" (`api_steps.py:549`), used by every negative scenario | D17, D55, OpenAPI `Problem` | The step fails when the body's `status` member is missing. RFC 9457 makes that member optional, the OpenAPI `Problem` schema does not require it, and no decision makes it mandatory. | Check the HTTP status and the media type. Check the body `status` only when present, or get a decision (question Q3). |
| SR-07 | major | over-assertion | `ui/vocabulary_ui.feature:11`, "The vocabulary list shows each vocabulary..." (line 16) | D5, D23, D75 | It expects the headers "Name,Description,Latest version,Status,Date created,Last updated,Creator". D75 names list headers only through D23 and D5. "Description" and "Creator" are not decided labels, and an actions column header is not ruled out. If step 60 checks this list exactly, a correct build can fail. | Check that the decided headers are present. Check "Description" and "Creator" only once they are decided (question Q4). |
| SR-08 | minor | over-assertion | `list_vocabularies.feature:140`, "A filter that matches nothing..." (line 146) | D22 | Expects totalPages 0 for an empty list. D38 fixes 0 only for the searches. D22 says "correct totalPages", which leaves 0 or 1 open. | Keep it if the owners confirm 0 (question Q2); otherwise accept 0 or 1. |
| SR-09 | minor | under-assertion | `ui/vocabulary_ui.feature:331`, "A failed status change keeps the shown status" | D72, D60 | It checks only "The vocabulary status could not be updated.". D72 adds the D60 sentence and the problem detail. There is no 503 row. With an exact-match step, a correct build fails. | Check all three parts and add a 503 example. |
| SR-10 | minor | gap | none | D35 and D72 (history load error, 500 and 503), D18 (any 403 shows the message), D76 (413 shown above the form), D60 ('Cancel' button) | These are not covered, and COVERAGE.md gives no reason. | Add UI scenarios with the API stubbed to 500, 503, 403 and 413. |
| SR-11 | minor | fragile | `list_vocabularies.feature:115`, "Date filters with a date match the whole UTC day" | D20 | Alpha and Bravo can fall on different UTC days if the scenario crosses midnight. | Remember each vocabulary's day and build the expected set from those days. |
| SR-12 | minor | fragile | `external_vocabularies.feature:98`, "The external vocabulary list is paged and in name order" | D41, E4 | It walks the global external list, which grows every run. Registrations by other runs can shift pages while it walks. Order is checked within a page only. | Also compare the last name of a page with the first name of the next. Accept that the list grows. |
| SR-13 | minor | fragile | `environment.py` (RUN_ID) | E4, D37, D19 | RUN_ID from the environment is not checked against `[a-z0-9]`. A value such as "run-8" breaks every name and the name filter. | Validate or clean RUN_ID at start. |
| SR-14 | minor | under-assertion | `create_vocabulary.feature:260`, `versions_and_content.feature:182` | D63 | "No Accept header" is never sent, because requests adds `Accept: */*` by default. | Add a row that removes the Accept header. |
| SR-15 | minor | gap | none | D46, D51 (the identical-version 409 comes after the field checks and before validation) | No scenario checks this order. | Upload the same content and description with an empty changelog and expect 400, not 409. |
| SR-16 | minor | gap | none | Q2 and D37 (1000-character description and changelog accepted on a new version); D38 (query parameter on the external search); D30 (a new alias equal to an existing alias gives 409) | Small gaps at the limits and in the conflict rules. | Add one scenario each. |
| SR-17 | minor | under-assertion | `external_vocabularies.feature:253`, "A registration with a registered namespace or alias gives 409..." | D8 spirit, D30 | It does not prove that nothing was stored for `${Other}`. | Add a check that `${Other}` does not exist, for example a 404 on GET `/external-vocabularies/${Other}`. That name is valid and unique, so the 404 proves it. |
| SR-18 | minor | traceability | `access_control.feature:122` tagged `@AC_SIMPL-18559_AC1`; `status_change.feature:10` and `:29` tagged `@AC_SIMPL-18170_AC2`; `new_version.feature:11` tagged `@AC_SIMPL-18172_AC2` (a UI criterion) | as named | The tags name criteria the scenarios do not check: a UI deprecation link, creation read-back, and a UI form. | Remove or move these tags. |
| SR-19 | minor | traceability | `status_change.feature:124`, "404 comes before 409 for a version status change" | D51 | No 409 condition exists in this scenario, so it does not test the order. | Rename it to "A status change for a version that does not exist gives 404". |
| SR-20 | minor | under-assertion | `validation.feature:13`, `external_vocabularies.feature:206` | D9, D65 | "location starting with 'line 3'" also matches line 30 or 31. | Match `^line 3(,|$)`. |
| SR-21 | minor | under-assertion | `access_control.feature:47` and `:63` (invalid token) | D17 | The title 'Unauthorized' is not checked. | Add the title check. |

## 3. Questions for the decisions

1. **D52 and D68:** what issue name does an unknown property inside an array item get: `latest` or `items[0].latest`?
2. **D22:** what is totalPages for an empty GET list (/vocabularies, .../versions, /external-vocabularies): 0 or 1? D38 says 0 only for the searches.
3. **Problem body:** must every problem+json body carry the `status` member? RFC 9457 and the OpenAPI make it optional, and the suite requires it everywhere.
4. **D23, D35 and D75:** what are the header labels of the non-sortable list columns (description, creator)? Does the actions menu column have a header?
5. **D86:** must the service return a supplied X-Correlation-Id unchanged? The suite assumes yes.
6. **D72:** is the failed-status-change message one alert with three parts, or separate elements?

## Answers of the test writer (45b)

I fixed 20 of the 21 findings and rejected one (SR-08). Nothing has run against a live system yet, so none of the new checks has passed or failed.

SR-01: fixed. All three scenarios now wait for the next second before the change and assert "is later than" the remembered lastUpdateDate.
SR-02: fixed. New scenario: v2 is DEPRECATED and v1 reactivated, then the list, GET by name, latest download, search with latest true and latestVersionHref must all give version 2 (D5).
SR-03: fixed. Vocabularies and versions are now created a second apart, and every sort row also checks the exact order of names or versions.
SR-04: fixed. Bravo is created a second after Alpha, and the date-time filters must return only Alpha.
SR-05: fixed. The row now accepts the issue name `latest` or `items[0].latest`.
SR-06: fixed. The body `status` member is optional; when it is present it must equal the HTTP status.
SR-07: fixed. The step became "includes the columns" and lists only the decided headers: Name, Latest version, Status, Date created, Last updated.
SR-08: rejected, because the OpenAPI is the wire source in the decisions' source hierarchy, and its example `empty` for GET /vocabularies shows totalPages 0. D22 only asks for the "correct totalPages". The reading is recorded in COVERAGE.md.
SR-09: fixed. The scenario is now an outline with 500 and 503 rows, and it checks the D72 text, the D60 sentence and the problem detail.
SR-10: fixed. Four UI scenarios added: history load error (500 and 503), any 403 shows the authorization message, a 413 detail appears above the form, and both forms offer Submit and Cancel.
SR-11: fixed. The expected items are built from each vocabulary's own UTC day.
SR-12: fixed. The walk through the external list also checks the order across page boundaries.
SR-13: fixed. RUN_ID is cut down to lower-case letters and digits, at most 8 characters.
SR-14: fixed. `<none>` removes the Accept header; a no-Accept row was added for create and for download.
SR-15: fixed. New scenario: same content and description with a blank changelog gives 400 on changelog, not 409.
SR-16: fixed. Three scenarios added: 1000-character description and changelog accepted on a new version, a query parameter on the external search gives 400, and an alias equal to a registered alias gives 409.
SR-17: fixed. GET /external-vocabularies/${Other} must return 404 after the conflict.
SR-18: fixed. I removed the SIMPL-18559/AC1 and SIMPL-18172/AC2 tags. I kept SIMPL-18170/AC2 on the status scenarios, because its last clause says the status "can later be manually changed to DEPRECATED or back to ACTIVE", which those scenarios check.
SR-19: fixed. Renamed to "A status change for a version that does not exist gives 404".
SR-20: fixed. Syntax-error locations must now match `line N(,.*)?`.
SR-21: fixed. Both invalid-token outlines now check the title 'Unauthorized'.

The suite now has 247 scenarios, 464 with outlines expanded. COVERAGE.md is regenerated: 27 criteria have no runnable API scenario, up from 25 only because of the SR-18 tag removals, and both of those criteria are still covered by `@ui` scenarios.

Dry run: 13 features, 465 scenarios: 464 untested and 1 skipped (the `@wip` one), 0 failed, 0 undefined steps.
