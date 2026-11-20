# Suite review

## 1. In short

- **Reviewed:** 259 scenario definitions in 15 feature files (468 runs once outline rows are expanded), plus the step library, `lib/vmtest`, the fixtures, the load test and `COVERAGE.md`. A dry run (`behave --dry-run`, nothing written) shows 0 undefined steps.
- **Overall:** The API part is strong and closely follows the decisions. The main risks are in `ui.feature` and in a few paging and number checks that the decisions leave open.
- **Findings by severity:** 2 blocker, 6 major, 11 minor.
- **Findings by kind:**

| Kind | Count |
|---|---|
| Misreading | 1 |
| Over-assertion | 6 |
| Under-assertion | 6 |
| Fragile | 2 |
| Traceability | 1 |
| Gaps | 3 |

## 2. Findings

| Id | Severity | Kind | Scenario | Decision or criterion | What is wrong | What to check instead |
|---|---|---|---|---|---|---|
| SR-01 | blocker | misreading | ui.feature:7 "The vocabulary list shows every vocabulary with its columns and actions" (header check at line 11) | D75, D23, D5, SIMPL-18161/AC1 | The scenario expects the column header "Vocabulary status". D75 says the list column headers are named as in D23, and D23 names the sortable column "Status". A UI that follows D23 and D75 fails this check. "Vocabulary status" is the filter label (D72), not the column header. | Use the header "Status". Keep "Latest version" (D5), and keep "Description" and "Creator", which come from the criterion. Raise the conflict between the criterion's wording and D23 as an owner question (Q-1). |
| SR-02 | blocker | fragile | ui.feature:56, 65, 150, 161, 195, 205, 214, 326 (download from the list, history, Update pre-fill, Update submit, deprecate and reactivate, no longer available, failed status change, any 403) | E4, D34, D23 | Each of these picks a row menu ("Actions for ${Health}") without first filtering the list. The store is never reset (E4). The list shows 20 rows per page sorted by name (D34, D23), and earlier runs leave many names that sort before "Health<run>". So the row is usually not on page 1, and a correct build fails. | Before using the row menu, filter the list on the run id or the name, as the other UI scenarios do. Or open the details page by its route (D74). |
| SR-03 | major | over-assertion | ui.feature:161 "Submitting the Update form creates the next version and confirms its number" (lines 169-170) | D72, D14, SIMPL-18172/AC2 | After submit, the scenario expects the version statuses to be visible on the page. D72 fixes the page that opens only after a new *vocabulary* is created; no decision says where the UI goes after a new *version*. A build that stays on the list or the form fails. | Keep the success message check (D14). Then open the details page of the vocabulary explicitly before checking the statuses of versions 2 and 1. |
| SR-04 | major | over-assertion | list.feature:6 (link checks at lines 14-16) and list.feature:140 outline (lines 146-147) | D22, D78 | The request carries only `name` (and a filter). The link step still requires `page=1&pageSize=20` in the self, first and last links. D78 says links keep the parameters of the request; no decision says a link must add the defaults. A link like `/vocabularies?name=x&page=1` is valid under the decisions and fails. | Require only the request's own parameters plus `page` where it changes. Allow `pageSize` at its default but do not require it. See owner question Q-3. |
| SR-05 | major | over-assertion | list.feature:30 outline, row `pageSize=2.0` | D22, D91 | D91 rejects "one that is not a whole number". 2.0 is a whole number in value, and JSON Schema treats 2.0 as an integer. A build using a schema validator may accept it, so a correct build can fail. | Drop the row, or replace it with a clearly non-integer value such as `2.5`. Raise Q-2. |
| SR-06 | major | under-assertion | versions.feature:196 "The field checks run before the identical-version check" | D46, D51 | The request sends a freshly generated file (`@valid`), so it can never match the stored content. A build that runs the identical-version check before the field checks still answers 400 and passes. | Send the content of version 1 with its description and no changelog part, then expect 400 with an issue named `changelog`, not 409. |
| SR-07 | major | gap | none; closest is status.feature:137 "Deprecating one version sets only that version and returns it" | D25 | D25 says the vocabulary's `lastUpdateDate` changes on either status change, including a version status change. Only the vocabulary PATCH case is tested (status.feature:6). `COVERAGE.md` gives no reason for the omission. | Add a scenario: remember the vocabulary, wait for the next second, PATCH version 1, then check that the vocabulary's `lastUpdateDate` differs and `creationDate` is unchanged. |
| SR-08 | major | under-assertion | load/locustfile.py:26 (`LOAD_WRITE_FILE_BYTES` default 10240) | Q1, D99 | Q1 sets p95 at most 3 s for writes "up to 1 MB". The throughput mix only writes 10 KB files, so a build that is slow at 1 MB still meets Q1 in the report. | Mix write sizes up to 1 MB (for example a share at 1 MB) and report p95 for the 1 MB writes on their own. |
| SR-09 | minor | over-assertion | ui.feature:15 outline (line 20) and ui.feature:214 (line 219) | D34, D72, D60 | D34 and D72 fix composite texts: a fixed sentence, then the D60 text and/or the problem detail. Read as an exact match, "shows an alert X" fails a UI that puts the whole text in one alert. | Use "an alert containing X" (as ui.feature:131 already does). Check the detail and the D60 text separately. |
| SR-10 | minor | over-assertion | validation.feature:117 "A valid new version is confirmed by the 201 response without error details" (line 121) | D14 | "issues is absent or null" fails a 201 that carries `"issues": []`. No decision forbids an empty list. | Accept a missing field, null, or an empty list (the step at vm_steps.py:1023 already does this). |
| SR-11 | minor | over-assertion | vm_steps.py:769 (used in read.feature:180, 188 and external.feature:38, 47) | D29, D41 | The regex allows only `attachment; filename="X"`. A header that adds `filename*=UTF-8''X` (some frameworks add it by default) fails, although D29 fixes only "attachment" and the file name. | Parse Content-Disposition: require the type `attachment` and `filename` equal to the decided name, and allow other parameters. |
| SR-12 | minor | fragile | list.feature:123 "The creationDate and lastUpdated filters with a date match the whole UTC day" | D20 | Alpha and Beta are created about 1.2 s apart. If the scenario runs across UTC midnight, they fall on different days and the check fails. | Skip or retry when the two creation dates are on different UTC days. Or check only that the filter returns every vocabulary whose date is that day. |
| SR-13 | minor | traceability | versions.feature:5 (tags `@AC_SIMPL-18172_AC2`, `@AC_SIMPL-18189_AC1`) | SIMPL-18172, SIMPL-18189 | Both are UI stories ("via the UI"). The API scenario cannot meet them, and the tags inflate their coverage in `COVERAGE.md`. | Keep these tags on the UI scenarios only. Tag the API scenario with the API criteria (SIMPL-18173/AC1, SIMPL-18190/AC1, SIMPL-18187/AC2). |
| SR-14 | minor | under-assertion | access.feature:213 "Scopes are not checked: a viewer token without extra scopes reads the list" | D16, D40 | Every token in the suite is fetched the same way (scope openid), so this scenario proves nothing beyond the viewer read matrix. | Either drop the D40 claim from `COVERAGE.md`, or state that the scope check is proven by every read with an openid-only token. |
| SR-15 | minor | under-assertion | read.feature:73 "The version history can be sorted, ties broken by version descending" (line 80) | D23 | `sort=creationDate` checks only the status code 200, not the order. | Create the versions at least one second apart and check the order (ascending by creation date, ties by version descending). |
| SR-16 | minor | under-assertion | load/run_load.py:116 (Locust run with `-r` = all users at once) | Q1 | Q1 is measured "after warm-up", but the statistics include the ramp-up and first requests. A cold-start spike can make a good build miss the p95. | Add a warm-up period that is left out of the statistics, or reset the stats after warm-up. |
| SR-17 | minor | gap | ui.feature | D35, D72, D18, D60, D34, Q6 | No UI scenario for: the history load error ('The version history could not be loaded.' plus detail, 500 and 503); a viewer opening the Update form URL directly (D18 names both forms); the date format 'YYYY-MM-DD HH:mm UTC' and the menu item 'Vocabularies' (D60); paging at 20 rows (D34); keyboard reachability (Q6). | Add one Gherkin scenario for each, or give a reason in `COVERAGE.md`. |
| SR-18 | minor | gap | API features | D52, D10, D67 | No scenario for: a query parameter on DELETE (D52); validationWarnings not returned by GET of the version (D10); a control character in the path (D67, only the encoded slash is tested). | Add a DELETE with `?x=1` (expect 400, in: query). Add a GET of a version created with a warning (expect no validationWarnings). Add a firewall case with a control character. |
| SR-19 | minor | under-assertion | ui.feature:116 "Field errors are shown under the fields and nothing is sent" | D62 | The title promises "nothing is sent", but no step checks it. | Add a check that no POST /vocabularies left the browser, or that no vocabulary with that name is stored. |

## 3. Questions for the decisions

1. **Q-1 (D23, D75 against SIMPL-18161/AC1):** Is the list column header "Status" (D23, D75) or "Vocabulary status" (the criterion text and the D72 filter label)? Is there a header for the actions column?
2. **Q-2 (D22, D91):** Is `2.0` (or `1e1`) in a query or path an integer? D91 says "not a whole number", while D22 says "not integers". Please say whether the textual form or the value decides.
3. **Q-3 (D22, D78):** Must self, first, prev, next and last always carry `page` and `pageSize` explicitly (as in the OpenAPI examples), even when the request did not send them?
4. **Q-4 (D72, D14):** After a successful new version from the Update form, which page does the UI show: the details page, as after a creation, or another page?
5. **Q-5 (D90):** For one error the decided text gives "1 errors found". Is the singular "1 error found" wanted?
6. **Q-6 (D34, D35, D72):** Are the load-error and status-change error texts one alert holding the sentence, the D60 text and the detail, or separate elements? The UI steps need to know whether to match exactly or by "containing".

## Answers of the test writer (45b)

SR-01: fixed
SR-02: fixed
SR-03: fixed
SR-04: fixed
SR-05: fixed
SR-06: fixed
SR-07: fixed
SR-08: fixed
SR-09: fixed
SR-10: fixed
SR-11: fixed
SR-12: fixed
SR-13: fixed
SR-14: fixed
SR-15: fixed
SR-16: fixed
SR-17: fixed
SR-18: fixed
SR-19: fixed

What changed, by finding:
- **SR-01:** the list header is now "Status" (D23, D75).
- **SR-02:** each of the 8 scenarios that opens a row's actions menu now filters the list on the name first.
- **SR-03:** after the Update form is submitted, the scenario opens the details page before it checks the version statuses.
- **SR-04:** a paging link may now leave out `page=1` and `pageSize=20` when the request did not send them.
- **SR-05:** the `pageSize=2.0` row is now `pageSize=2.5`.
- **SR-06:** the scenario now sends version 1's own content and description with no changelog, and expects 400 on `changelog` with no new version stored.
- **SR-07:** new scenario. A version PATCH changes the vocabulary's `lastUpdateDate` and leaves its `creationDate` and status unchanged.
- **SR-08:** the load test now writes a share of 1 MB versions and reports them as their own operation, `createVocabularyVersion1MB`.
- **SR-09:** the composite UI error texts are now checked with "an alert containing", and the problem detail is checked separately.
- **SR-10:** a 201 may now carry `issues` as an empty list.
- **SR-11:** `Content-Disposition` is now parsed. It must be `attachment` with the decided `filename`; other parameters are allowed.
- **SR-12:** the whole-day date filter scenario skips itself if the two vocabularies were created on different UTC days.
- **SR-13:** criteria of UI stories (SIMPL-18172, 18189, 18191, 18186, 18182/AC2, 18180/AC7) are removed from API scenarios. `COVERAGE.md` is updated to match.
- **SR-14:** the scenario no longer claims D40. `COVERAGE.md` now says every token in the suite asks only for the openid scope, which shows the read and write scopes are not required.
- **SR-15:** the versions are now created one second apart, and the order is checked for `sort=creationDate` and `sort=-creationDate`.
- **SR-16:** the load test adds a 30 s warm-up (`LOAD_WARMUP_S`) whose statistics are dropped.
- **SR-17:** five new UI scenarios:
  - history load error (500 and 503);
  - a viewer opening the new version form URL;
  - the UTC date format and the 'Vocabularies' menu item;
  - paging at 20 rows;
  - keyboard reachability of every action.
- **SR-18:** three new API checks:
  - a query parameter on DELETE gives 400 and removes nothing;
  - `validationWarnings` is not returned when the version is read later;
  - control characters `%0A` and `%00` join the encoded slash in the firewall outline.
- **SR-19:** the scenario now checks that no POST left the browser and that nothing was stored.

Dry run (`./run.sh dry-run`): 479 scenarios and 2,388 steps, all untested, 0 undefined and 0 unmatched steps. behave 1.3.3 still marks 2 features as failed even though no step or scenario fails; this happens when a plain scenario follows an outline. No em or en dashes in `work/`.
