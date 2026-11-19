# Suite review

## 1. In short

- **Scenarios reviewed:** 410, counting each outline row. That is 362 API, 7 store-down, 39 UI (Gherkin only) and 2 wip. I also reviewed the load test (`load/locustfile.py`), the step library, the client and the fixtures. `behave --dry-run` parses all 15 features and has no undefined steps.
- **Overall:** the API scenarios follow the decisions closely. Problem types, detail texts, issue `in` and `name`, check order (D51, D63, D67), warning texts (D47, D65) and "nothing stored" through name filters (D49) all match. The real problems are in one UI sort scenario, the load test, two issue-name checks the decisions leave open, and a few gaps.
- **Findings by severity:** 2 blocker, 5 major, 15 minor (22 in total).
- **Findings by kind:**

| Kind | Count |
|---|---|
| Over-assertion | 6 |
| Under-assertion | 3 |
| Misreading | 0 |
| Proof that cannot work | 1 |
| Fragile | 3 |
| Traceability | 1 |
| Gaps | 8 |

## 2. Findings

| Id | Severity | Kind | Scenario | Decision / criterion | What is wrong | What to check instead |
|---|---|---|---|---|---|---|
| SR-01 | blocker | Fragile | `ui_list.feature:56` "A click on a sortable column sorts it; a click on the active sort column reverses it" | D23, D50, D58 | Alpha, Beta and Gamma are created back to back. Times are kept to the second (D58), so their creation dates often tie. Ties are broken by name ascending in both directions (D23). The second click on "Date created" would then give "Alpha, Beta, Gamma", not "Gamma, Beta, Alpha". A correct build fails. | Use `vocabularies "Alpha, Beta, Gamma" exist, created one second apart`, as the API sort outline does. |
| SR-02 | blocker | Over-assertion | `load/locustfile.py`, `Writer` and `meets_write_throughput` | Q1, D71 | There is one blocking writer user with `constant_throughput(1)`. Throughput then depends on latency: a build whose 1 MB writes take 1.5 s (inside the 3 s p95 target) cannot reach 0.95 writes/s. The three 5 MB writes may each take up to 30 s (D71), which stalls the only writer for up to 90 s of a 300 s run. `meets_all_targets` can be false for a correct build. | Drive writes with an open arrival rate, for example several writer users or a scheduler that starts one write per second whatever the latency. Leave the 5 MB writes out of the throughput count, or run them on their own user. |
| SR-03 | major | Gap | Load test seed: 40 vocabularies with 3 versions each | Q1, Q4 | Q1 is to be measured "at the design load of Q4", which is 1000 vocabularies with 20 versions each. The seed is about 4 % of that. The reason in COVERAGE (seeding at the 1 write/s design rate would take five hours) does not hold: the seed is setup, not measured load, and can run in parallel or in bulk. Read latency on a small store does not show Q1 at Q4. | Seed the full Q4 volume in parallel before the run (or reuse a seeded dataset). Otherwise report Q1 as "not measured at Q4 volume", not as met. |
| SR-04 | major | Over-assertion | `search.feature:97`, outline row "unknown item property" | D52, D68 | Expects the issue name `items[0].latest`. D68 fixes `items[<i>].name` and `items[<i>].version` only. D52 says only "one issue per unknown name (in: body)". A build that names the issue `latest` follows the decisions and fails. | For this row, check only `in: body`, or accept `latest` and `items[0].latest`. Raise the naming as an owner question. |
| SR-05 | major | Proof that cannot work | `search.feature:207` and `:208`, rows "empty namespace array" and "empty alias array" | D38, D68 | D38 adds "at least 1 entry when present". With that rule, `{"namespaceUris": []}` fits no oneOf branch, and D68 then gives `name: body`. Reading it as an array limit gives `name: namespaceUris`. Both are defensible, so the expected name cannot be fixed from the decisions. | Check status 400 and `in: body` only, until the owner decides (question 2). |
| SR-06 | major | Over-assertion | `ui_list.feature:11` "the table has the columns 'Name, Description, Latest version, Status, Date created, Last updated'" | SIMPL-18161/AC1, D5, D23, D72, D75 | AC1 requires a creator in each row and an actions menu, and line 15 of the same scenario checks the creator email. The column list leaves out both a creator column and an actions column. If step 60 implements "has the columns" as an exact match, a build that follows AC1 fails. No decision gives the creator column header. | Make the step "includes the columns" and check the creator through the row text, or wait for the label to be decided (question 3). |
| SR-07 | major | Gap | No scenario | D25 | D25 fixes the vocabulary's `lastUpdateDate` to change on "either status change", which includes `PATCH .../versions/{version}`. Only the vocabulary PATCH and the new-version paths are tested. A build that does not update the vocabulary on a version PATCH passes. | Add: save the vocabulary's `lastUpdateDate`, wait one second, PATCH version 1, then check the vocabulary's `lastUpdateDate` is later. |
| SR-08 | minor | Over-assertion | `vocabulary_list.feature:85` "A filter that matches nothing..." expects `totalPages` 0 | D22, D38 | D38 fixes "0 when total is 0" for the searches only. For lists, D22 says only "correct totalPages". 0 is the likely reading (no `last` link when total is 0), but it is not fixed. | Keep the check, but confirm it with the owner (question 1). Or accept 0 or 1. |
| SR-09 | minor | Over-assertion | `versions.feature:368`, version `lastUpdateDate` later after its own PATCH | D4, D25 | D4 fixes a version's `lastUpdateDate` only for automatic deprecation. No decision says a manual version PATCH updates that version's `lastUpdateDate`. It is a likely reading, not a fixed one. | Keep it, and raise question 4. Or move the check to the vocabulary's `lastUpdateDate` (see SR-07). |
| SR-10 | minor | Under-assertion | `validation.feature:6` "A syntax error is rejected with one issue..." | D9 | D9 says semantic validation does not run after a syntax error. `syntax_error.ttl` holds no semantic error, so a build that runs both steps still returns exactly one issue and passes. | Add a fixture with an S1 or S3 violation before the syntax error, and expect exactly one issue: the syntax one. |
| SR-11 | minor | Gap | No scenario | D12, D11 | References declared with SPARQL-style `PREFIX` are never used. There is also no file with two deprecated references, which D11 says give one warning each. | Add one scenario with a `PREFIX` external reference (expect `externalReferenceSkipped`), and one with two deprecated references (expect two warnings). |
| SR-12 | minor | Gap | No scenario | D46, D51 | The identical-version check must run after the field checks and the 404, and before validation. That order is not tested. | Resubmit identical content with an empty changelog (expect 400 with the changelog issue). Resubmit identical content to an unknown name (expect 404). |
| SR-13 | minor | Under-assertion | `access_control.feature:55` (403 outline) and `vocabulary_list.feature:27` | SIMPL-18162/AC2, AC3 | The criteria say "the response body shall contain the detail of the error" and "metadata shouldn't be returned". The 403 outline checks neither. The 400 outline checks `items` absent but not that a detail is present. | Add "the response field 'detail' is present" to both, and "the response field 'items' is absent" to the 403 rows. |
| SR-14 | minor | Traceability | `vocabulary_list.feature:210` "An unknown query parameter on a GET of one vocabulary is rejected" | D21, D49 | Tagged `@D49` (invalid path values), but it checks D21 (unknown query parameter). | Replace `@D49` with `@D21` alone. |
| SR-15 | minor | Over-assertion | `ui_details.feature:56`, clicking "Latest ACTIVE version: 2" expects the region "Version 2" | D35, D36 | D36 says the line is "a link to that version". It does not fix whether the link opens the version panel, navigates elsewhere or scrolls. | Check only that the link exists and points to version 2. Raise the target as question 6 if the panel matters. |
| SR-16 | minor | Fragile | Load test, `download_latest` | Q1 | The writer adds 5 MB versions to seeded vocabularies, so `GET /content/{name}` sometimes downloads 5 MB. The Q1 read target covers downloads under 1 MB only. Today the share is small (about 2 %), but more 5 MB writes would push the p95 over. | Send the 5 MB writes to vocabularies the readers never download. |
| SR-17 | minor | Gap | No scenario; COVERAGE maps E5 to the tampered tokens | E5 | A token without the audience `vocabulary-manager-be`, or from another issuer, must get 401. Tampered tokens fail on the signature, so they do not show the audience or issuer check. COVERAGE gives no reason for the gap. | Add a scenario with a token from a second test client without the audience mapper, or write in COVERAGE why it cannot be tested. |
| SR-18 | minor | Gap | No UI scenario | D18, D60 | Missing: "any 403 from the API shows the same message", the menu item 'Vocabularies', and the date format 'YYYY-MM-DD HH:mm UTC' (only the text "UTC" is checked). | Add a stubbed 403 on a write showing 'You are not authorized to perform this action.'; check the menu item; check the date format with a pattern. |
| SR-19 | minor | Fragile | `t_ext_list_contains` and `t_ext_not_stored` (step library), and `external_vocabularies.feature:37` | E4, D41 | The store is never reset and every run registers new external vocabularies. The page loops stop at 199 pages: past that, `t_ext_not_stored` passes without looking further. The order check reads page 1 only. Not urgent, but it grows with every run. | Fail loudly when the page limit is reached, rather than pass. |
| SR-20 | minor | Under-assertion | `ui_list.feature:28` "The list pages on the server with 20 rows and shows an empty state" | D34 | The title promises paging with 20 rows, but only the empty state is checked. | Add a check of 20 rows and the page controls (data with more than 20 names under the run id), or rename the scenario. |
| SR-21 | minor | Under-assertion | `ui_list.feature:25` expects the text "Internal Server Error" | D34, D72 | D72 asks for the problem detail. The expected text depends on a stub body that the scenario does not define, and "Internal Server Error" reads like a title, not a detail. | Have the stub return a known detail and check that exact text. |
| SR-22 | minor | Gap | No scenario | D49, D52, D41 | Missing: an invalid name on PATCH and DELETE paths (only GET is tested); a query parameter on DELETE and on the external POST operations; the external list default `pageSize` 20; the full body of `GET /external-vocabularies/{name}`. | Add one outline row each. |

## 3. Questions for the decisions

1. **D22 Empty list.** Is `totalPages` 0 or 1 when `total` is 0 on `GET /vocabularies`, the versions list and the external list? D38 fixes 0 only for the searches.
2. **D52, D68, D38 Issue names.** What is the issue `name` for an unknown property inside a search item (`latest` or `items[0].latest`)? Is an empty `namespaceUris` or `aliasUris` array a "no oneOf branch" error (`name: body`) or an array limit error (`name: namespaceUris` / `aliasUris`)?
3. **SIMPL-18161/AC1 against D23, D72 and D75.** Which columns does the vocabulary list have, and what is the creator column's header? AC1 asks for the creator in each row; no decision names the column.
4. **D4 and D25 Version last update date.** Does a manual `PATCH .../versions/{n}` set that version's own `lastUpdateDate`?
5. **Q1 and Q4 Load test.** Is "1 write per second sustained" an offered rate that may be served concurrently, or does one client have to complete writes back to back? Must Q1 be measured with the full Q4 data volume (1000 vocabularies with 20 versions each)?
6. **D36 Link target.** What does the 'Latest ACTIVE version: <n>' link open: the version panel on the same page, or another route?

## Answers of the test writer (45b)

SR-01: fixed
SR-02: fixed
SR-03: fixed
SR-04: fixed
SR-05: fixed
SR-06: fixed
SR-07: fixed
SR-08: rejected, because the OpenAPI is the source for the wire format (source hierarchy item 3). Its `empty` example for `GET /vocabularies` returns `totalPages: 0` with `total: 0`. D22 agrees: it leaves out `last` when total is 0, so there is no page to point at.
SR-09: rejected, because the OpenAPI example for `PATCH .../versions/{version}` returns the version with `lastUpdateDate` later than its `creationDate` (2024-03-02 against 2024-03-01). So the version's own last update date changes on a manual PATCH. Under D25 that also changes the vocabulary's date, which SR-07 now tests.
SR-10: fixed
SR-11: fixed
SR-12: fixed
SR-13: fixed
SR-14: fixed
SR-15: fixed
SR-16: fixed
SR-17: fixed (documented). The build profile has only the test client `vocabulary-tests`, which carries the audience mapper (E5), so the suite cannot get a validly signed token without the audience or from another issuer. COVERAGE now gives this reason.
SR-18: fixed
SR-19: fixed
SR-20: fixed
SR-21: fixed
SR-22: fixed

Dry run: `./run.sh dry-run` parses all 15 features: 424 scenarios, 2155 steps, no undefined steps and no failed features. By run mode that is 374 API, 7 store-down, 41 UI (Gherkin only) and 2 wip.

The fixes add 14 scenarios, two API steps for open issue names, and new UI placeholder steps. Two fixes reshape the load test:
- **SR-02 and SR-16:** one write now starts every second whether or not the previous write has finished. Writes go to vocabularies created for that run, which readers never read. The 5 MB writes run on their own user and are left out of the throughput count.
- **SR-03:** the load test now seeds the full Q4 volume (1000 vocabularies with 20 versions each) before the measurement. It seeds in parallel, and later runs reuse the seed. If the seed is smaller than Q4, `meets_all_targets` comes out false.

The seed adds 20,000 versions to the shared store on the first load run. `LOAD_DURATION` now starts after seeding, so `./run.sh load` takes longer overall.
