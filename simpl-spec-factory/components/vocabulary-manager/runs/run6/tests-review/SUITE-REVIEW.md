# Suite review

## 1. In short

- **Reviewed:** 244 scenarios (421 cases) in 14 feature files, together with the step library, `vmtest/` helpers, fixtures, `README.md` and `COVERAGE.md`. The behave dry run finds no undefined steps. The two features it reports as "failed" are the behave quirk that the README already describes.
- **Findings:** 17 in total: 4 blocker, 4 major, 9 minor.
- **By kind:** 6 over-assertion, 5 under-assertion, 3 fragile, 1 proof that cannot work, 1 traceability, 1 gap, 0 misreading.
- **Overall:** the API part is strong. It uses its own data per run (E4), proves "nothing stored" through the list filter (D49), and checks the fixed texts of D4, D11, D47 and D55 exactly. Two API blockers sit in the external vocabulary feature. The two UI blockers will hit at step 60, when the UI steps go live.

## 2. Findings

| Id | Severity | Kind | Scenario | Decision / criterion | What is wrong | What it should check instead |
|---|---|---|---|---|---|---|
| SR-01 | blocker | proof that cannot work | external_vocabularies.feature:171, "An external vocabulary name that differs only in case is a duplicate" (line 178) | D56, D54 | The step `the external vocabulary "raw:TERMS<RUN>" is not registered` lower-cases names before comparing them. It therefore finds the original `Terms<run>` from the Given step and fails on every correct build. | Check that exactly one external vocabulary matches case-insensitively and that its name is the original `Terms<run>`. Or use GET `/external-vocabularies/TERMS<RUN>` and expect 404: the name is valid, and paths match exactly (D54). |
| SR-02 | blocker | over-assertion | external_vocabularies.feature:10, "An external vocabulary is registered" (lines 25-26) | D41, D56 | Checks `aliasUris.0` and `aliasUris.1` by position. No decision fixes the order of aliasUris, and the Fuseki store keeps multi-valued properties unordered. | Compare aliasUris as a set, after removing a trailing `#` or `/` (D30). |
| SR-03 | blocker | fragile | ui_vocabulary_list.feature:45, "A click on a sortable column header sorts and a second click reverses" (lines 53-56) | D23, D50, D58 | The three vocabularies are created with no gap, so their creation dates can fall in the same second. The tie then breaks by name and gives "Apple, Banana, Cherry", not the expected order. | Use `Given the vocabularies "Cherry, Apple, Banana" exist, created one second apart`. |
| SR-04 | blocker | over-assertion | ui_create_and_version.feature:31, "Field errors are shown before submit" | D37, D13 | Asserts that Submit is disabled. D37 only says the field errors appear under the field before submit. D13 disables Submit only for errors from the upload check. The field errors themselves are not checked at all. | Check that an error appears under Name and under Description, and that nothing is stored after a submit attempt. Drop the disabled check, or raise it as an owner question (Q5). |
| SR-05 | major | over-assertion | external_vocabularies.feature:227, outline row `namespaceUri \| not a uri` (line 240) | D56, D41 | No decision fixes URI-format validation. OpenAPI `format: uri` is an annotation by default, so a build that follows the decisions may accept the value. | Remove the row, or keep it as @wip until the owner answers Q2. |
| SR-06 | major | over-assertion | ui_vocabulary_list.feature:95, "A status change for a vocabulary that is no longer available" (line 102) | D24, D35, SIMPL-18174/AC2 | Expects "Vocabulary not found.". D35 fixes that text only for the details page. D24 only fixes the stub, and AC2 asks only that the user is "informed that the vocabulary could not be found". | Check that a not-found message is shown (for example the problem detail from the stub) and that the row stays "Active". Raise the text as Q4. |
| SR-07 | major | under-assertion | list_vocabularies.feature:123, "A creation date-time filter matches the value to the second" | D20 | Only checks that the exact value matches and that 2001-01-01 does not. A build that matches a date-time on the whole day passes. | Add a query with the same day and one second off (created + 1 s) and expect 0 items. |
| SR-08 | major | under-assertion | validation.feature:111, "Relative IRIs resolve against the content URL of the new version" | D57 | `relative_iris.ttl` resolves `<#>` to the vocabulary's own namespace. Own namespaces and self-references give no warning, so a build that uses the wrong base passes. | Add a relative prefix that resolves to another internal vocabulary only under the D57 base, for example `@prefix o: <../../Ghost<run>/versions/1#>`. Expect the S7 error naming Ghost<run>; a wrong base would give externalReferenceSkipped instead. |
| SR-09 | minor | under-assertion | validation.feature:50, "The semantic rule <rule> blocks the creation" | D9 | The location of the S1 to S5 issues is not checked at all. `COVERAGE.md` says "checked only for presence", which is not true. | Add "the issue has a non-empty location", and correct the note in COVERAGE.md. |
| SR-10 | minor | over-assertion | ui_vocabulary_list.feature:11 (columns "Description", "Creator"), lines 38-41 (filter labels "Name", "Status"), line 74 (row action "Download") | D31, D60, SIMPL-18161 | These labels are not fixed by a decision. The SIMPL-18161 note calls the row action "Download vocabulary". | Match the AC wording, or accept either label (case-insensitive, substring). |
| SR-11 | minor | fragile | ui_create_and_version.feature:6 (line 14), "I do not see the text Version" | D32, SIMPL-18169/AC1 | Matches any "Version" on the page. It can hit "Latest version" in the list behind a dialog, or a heading. | Limit the check to the form: no field labelled "Version" or "Change log". |
| SR-12 | minor | over-assertion | ui_create_and_version.feature:73 (line 78), "I see the text 5 MB" | D13, D15 | The text of the browser-side refusal is not fixed. D15 fixes the UI text only for the API 413 detail. | Check that an error is shown for the file and that Submit is disabled. |
| SR-13 | minor | under-assertion | ui_details_and_access.feature:21 (line 29), ui_details_and_access.feature:83 (lines 88-90) | D35, D18, SIMPL-18161 | "I see the text Deprecated" already passes from the status column. The viewer's "do not see Update/Deprecate" passes trivially when the actions sit in a closed three-dot menu (SIMPL-18161 note). | Check "Deprecated" inside the version panel. Open the row menu before checking which actions are hidden. |
| SR-14 | minor | fragile | list_vocabularies.feature:224, "Paging keeps the requested sort order across pages" (line 234) | D22 | Following the "next" link only works if the links keep the name filter and the sort. D22 does not say they do. If they don't, page 2 lists the whole shared store. | Raise as Q7. Meanwhile request page 2 explicitly, as the scenario already does for page 3. |
| SR-15 | minor | under-assertion | list_vocabularies.feature:192, "Sorting on status groups the statuses and breaks ties by name" | D23 | The `-status` request checks only the order of the groups. It does not check that ties are still name ascending. | Also run the tie check on the descending response. |
| SR-16 | minor | traceability | status.feature:9 (@D6), validation.feature:111 (@D57, see SR-08), create_vocabulary.feature:162 (@D54), external_vocabularies.feature:38 | D6, D54, D57, D41 | @D6 (publication) has nothing to do with a status change. @D54 is about paths, but this is a uniqueness test (D37). The step name "on every page" checks only the first page. | Drop @D6 and @D54 from these scenarios. Rename the step, or make it page through the list. |
| SR-17 | minor | gap | none | D12, D15, D19, D21, D52 | Not tested, and COVERAGE.md gives no reason: the SPARQL-style `PREFIX` declaration (D12); 415 on POST /external-vocabularies/content (D15); the 64-character name filter boundary (D19); an unknown query parameter on the content downloads and on DELETE (D21, D52). | Add one small scenario for each. |

## 3. Questions for the decisions

1. **Alias order and form (D30, D56):** must GET and POST /external-vocabularies return aliasUris in the order they were sent, and in the form they were sent or normalised? The same question applies to `namespaceUri`.
2. **URI validation (D56, D41):** must a namespaceUri, aliasUri or sourceUri that is not an absolute URI return 400? This also covers the body of POST /external-vocabularies/content.
3. **External 404 by namespace (D41):** POST /external-vocabularies/content has no name. Which detail and issue does its 404 carry?
4. **UI not-found on status change (D24, SIMPL-18174/AC2):** which text does the list show when the status PATCH returns 404?
5. **Submit button (D37 against D13):** does the Submit button stay enabled while field errors are shown?
6. **D36 against D47:** what does the UI show for a version that is ACTIVE in a DEPRECATED vocabulary? D36 would show "Latest ACTIVE version: <n>", which may point to the version itself. D47 recommends no version in this case.
7. **Paging links (D22):** do self, first, prev, next and last keep the filter and sort parameters of the request?
8. **Upload limit (D15):** is the 5 MB limit on the file part or on the whole multipart request? The "exactly 5 MB is accepted" scenarios assume the file part.
9. **List warning (D36):** is the deprecation text in the list visible text, or a tooltip on the icon?

## Answers of the test writer (45b)



SR-01: fixed. The case-duplicate check now expects exactly one external vocabulary that matches in any case, and it must carry the original name `Terms<run>`.
SR-02: fixed. `aliasUris` is compared as a set, after removing a trailing `#` or `/` (D30).
SR-03: fixed. The UI sort scenario now creates the vocabularies one second apart.
SR-04: fixed. The scenario checks for a field error under Name and under Description, then clicks Submit and checks that nothing was stored. The "Submit is disabled" check is gone.
SR-05: fixed. I removed the `not a uri` row. `COVERAGE.md` now says URI format is not tested because no decision fixes it.
SR-06: fixed. The scenario now accepts any message saying the vocabulary could not be found (for example the stub's problem detail). The row must still show "Active".
SR-07: fixed. I added a query for the creation time plus one second, which must return 0 items.
SR-08: fixed. A new scenario declares a relative prefix `<../../Ghost<run>/versions/1#>`. Under the D57 base it names Ghost, which does not exist, so the scenario expects the S7 error naming Ghost.
SR-09: fixed. The S1 to S5 outline now checks that every issue has a non-empty location, and the note in `COVERAGE.md` is corrected.
SR-10: fixed. `ui_steps.py` now documents a matching rule: labels that no decision fixes match case-insensitively as a substring, so "Download vocabulary" also matches "Download". Decided texts still match exactly.
SR-11: fixed. The checks are now "the form has no field labelled 'Change log'" and "... 'Version'", instead of a whole-page text search.
SR-12: fixed. The scenario checks for a field error under "Vocabulary file" and that Submit is disabled (D13). The "5 MB" text check is gone.
SR-13: fixed. "Deprecated" is now checked in the row of version 1 and in the version panel. The admin and viewer scenarios open the row's actions menu before checking which actions show.
SR-14: fixed. Page 2 is now requested directly. The check that the "next" link points to page 2 stays.
SR-15: fixed. The `-status` response now also gets the tie check (name ascending).
SR-16: fixed. I dropped `@D6` from the status scenario and `@D54` from the case-duplicate scenario. The external list step is renamed to "on this page". `@D57` stays on the relative-IRI scenario; SR-08 makes it meaningful.
SR-17: fixed. New scenarios:
- an upload using SPARQL-style `PREFIX` gets `externalReferenceSkipped` (D12);
- `POST /external-vocabularies/content` that is not JSON gets 415 (D15);
- a 64-character name filter is accepted (D19);
- an unknown query parameter gets 400 on both content downloads, on `GET /external-vocabularies/{name}/content` and on `DELETE /external-vocabularies/{name}` (D21, D52).

Dry run (`./run.sh dry-run`, behave 1.3.3): 250 scenarios, 428 cases (385 API, 5 store-down, 37 UI, 1 WIP), 2589 steps, 0 undefined, 0 failed, exit 0. Behave still lists "2 features failed"; that is the quirk the README already describes, and no step ran or failed. `COVERAGE.md` was regenerated: all 85 live criteria still have a scenario.
