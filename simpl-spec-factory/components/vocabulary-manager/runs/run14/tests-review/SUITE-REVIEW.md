# Suite review

## 1. In short

- **Scenarios reviewed:** 367 (663 cases). That is 295 API scenarios, 10 store-down scenarios, 1 `@wip` scenario and 61 UI scenarios (Gherkin only), plus the Locust load test. `behave --dry-run` on the non-UI scenarios finds no undefined steps.
- **Overall:** the API suite follows the decisions closely, including the later ones (D140 to D210). The problems are in the expired-token proof, in UI scenarios that assume a small store, in two titles and one route that no decision fixes, and in the load test's single writer.
- **Findings by severity:** 2 blockers, 5 majors, 9 minors (16 in total).
- **Findings by kind:**
  - Over-assertion: 3 (SR-02, SR-06, SR-10)
  - Under-assertion: 2 (SR-09, SR-12)
  - Misreading: 0
  - Proof that cannot work: 2 (SR-01, SR-08)
  - Fragile: 5 (SR-03, SR-04, SR-05, SR-07, SR-11)
  - Traceability or clarity: 2 (SR-14, SR-16)
  - Gaps: 2 (SR-13, SR-15)

## 2. Findings

| Id | Severity | Kind | Scenario | Decision or criterion | What is wrong | What the scenario should check instead |
|---|---|---|---|---|---|---|
| SR-01 | blocker | Proof that cannot work | `access_control.feature:203` "An expired token gets 401" | E8, D132, D17 | The step waits `expires_in + 3` s (about 13 s). D132 tells the build to check expiry with Nimbus `DefaultJWTClaimsVerifier` or Spring `JwtValidators`. Both allow 60 s of clock skew by default. A build that follows D132 with library defaults accepts the token and returns 200. No decision fixes the clock skew. | Wait longer than any plausible skew (for example `expires_in + 70` s), or wait for an owner decision on the skew and use it. Keep `@slow`. |
| SR-02 | blocker | Over-assertion | `ui/vocabulary_forms_ui.feature:217` "Cancel on the creation form returns to the list without a request" | D60 | D60 fixes only the label 'Cancel'. No decision or story says where Cancel goes. A correct build that goes back in browser history fails, because the test opens `/vocabularies/new` directly. | Check only that no POST was sent and that the form route was left. Or raise the target route as an owner question first. |
| SR-03 | major | Fragile | `ui/vocabulary_list_ui.feature:6` "The Vocabulary list shows every vocabulary..." | E4, D34, D22 | The scenario opens the list with no name filter, then reads the row of `$Apple` and opens its Actions menu. The shared store holds thousands of vocabularies (D99 seed, earlier runs), and the list pages by 20 in name order. `Apple<run>` is almost never on page 1. | Add `I filter "Name contains" with "$RUN"` before the row checks, as the other list scenarios do. |
| SR-04 | major | Fragile | `ui/external_vocabularies_ui.feature:5`, `:16`, `:36` | E4, D210, D41 | The external screen has no filter (D41, D210), and the store keeps every external vocabulary from earlier runs. "`$ExtA` is listed before `$ExtB`", "the row of `$Ext`" and "Remove for `$Ext`" assume the rows are on the visible page. | State in the Gherkin that the step pages with 'Next page' until the row appears, and that the order check compares positions across pages. Otherwise this needs a decision on how tests find their rows (Q6 below). |
| SR-05 | major | Fragile | `ui/vocabulary_list_ui.feature:74` "A click on a sortable column header sorts and a second click reverses" | D23, D58 | The three vocabularies are created without the one-second spacing. When they share a creation second, the 'Date created' sort falls back to name order (Apple, Banana, Cherry) instead of the expected Cherry, Apple, Banana. | Use `vocabularies "$Cherry, $Apple, $Banana" exist, created one second apart`, as the API sort scenarios do. |
| SR-06 | major | Over-assertion | `http_rules.feature:26` "Problem bodies carry the decided type, title and status", rows 404 and 400 | D55, D48 | D55 lets tests check status, type, issue `in` and `name`, and only the texts a decision fixes. Titles are decided for 401, 403 (D17), 406 (D63) and 503 (D44). 'Resource Not Found' and 'Bad Request' appear only in OpenAPI examples. | Check the title only on the 403 and 406 rows. Drop it from the 404 and 400 rows, or raise it as an owner question (Q4 below). |
| SR-07 | major | Fragile | `load/locustfile.py:93` (class `Writer`, `fixed_count = 1`) | Q1, D99, D183 | D99 asks for an offered write rate of 1 per second. With one writer in a closed loop, every write that takes more than 1 s delays the next one. Q1 allows up to 3 s at p95 for 1 MB files. A build that meets the p95 target can still fail the "achieved at least 95 % of the offered rate" verdict. | Use 2 or 3 writer users with `constant_throughput(1/N)` each. That stays below the 4 upload slots of D92 and keeps a true offered rate. |
| SR-08 | minor | Proof that cannot work, traceability | `http_rules.feature:5` (414) and `:11` (431) | D158, D178, Q2 | D158 tells the build to raise its server limits, but no decision says how far. A build with limits above 200 000 characters returns 400 (name filter) or 200, not 414 or 431. The `@Q2 @NFR-04` tags are also wrong: Q2 does not list URL or header size. | Keep the scenarios but ask the owners for the limit, then test at and above it. Drop the `@Q2 @NFR-04` tags. |
| SR-09 | minor | Under-assertion | `vocabulary_create.feature:90` outline, rows "name too short" (`Ab`) and "empty name" | SIMPL-18170/AC3, D8 | "Nothing stored" is proven with `name contains $RUN`. The name `Ab` does not contain the run id, so a wrong build that stored `Ab` passes. | For that row, also check that no item of `GET /vocabularies?name=Ab` is named exactly `Ab`. |
| SR-10 | minor | Over-assertion, traceability | `validation.feature:404` "Warnings do not appear in a failed creation" | D10, D12 | D10 says where warnings appear (the 201 response). It does not forbid `validationWarnings` in a 400 body. The scenario is tagged `@D11`, but the file has an external namespace, not a deprecated internal reference. | Keep the 400 and one-issue checks. Treat the absent field as an owner question (Q5 below). Retag `@D11` to `@D12`. |
| SR-11 | minor | Fragile | `vocabulary_list_query.feature:83` "A creation date matches every vocabulary created on that UTC day"; `ui/vocabulary_list_ui.feature:64` "today's UTC date" | D20 | Both fail if the run crosses 00:00 UTC between the two creations, or between the creation and the filter. | Read the day of each vocabulary and skip or retry when they differ. In the UI scenario, use the day from the stored `creationDate`, not today's date. |
| SR-12 | minor | Under-assertion | `access_control.feature:69` outline, rows `POST /external-vocabularies` and `DELETE /external-vocabularies/$Ext` | D16, D17 | The checks after the 403 only cover the internal vocabulary `$Alpha`. Nothing proves the external vocabulary is still registered after a refused DELETE. | Add `the external vocabulary "$Ext" is listed` (or a GET returning 200) after the 403. |
| SR-13 | minor | Gap | `ui/vocabulary_details_ui.feature:92` to `:123` | D36, D64 | D36 puts the version warning in the version list and in the panel. Only the panel is checked. The scenario at `:119` checks an ACTIVE version, which shows no warning anyway, so it does not exercise the self-link rule of D64. | Add a check of the warning in a deprecated version's row. Either remove `:119` or note that D64's self-link case cannot be reached under D47. |
| SR-14 | minor | Clarity | `ui/vocabulary_details_ui.feature:54` "the version row 1 shows the status 'Deprecated'" | D35 | "Row 1" can mean the first row, which is version 2 and Active because the list is newest first, or the row of version 1. | Write "the row of version 1". |
| SR-15 | minor | Gap | All write features | D52, D51, D195 | Not tested: a query parameter on DELETE and on `POST /external-vocabularies` (D52); 415 before 413 (D51); a form field sent twice on POST .../versions and on external registration (D195). COVERAGE.md gives no reason. | Add one scenario per point, or record why they are left out in COVERAGE.md. |
| SR-16 | minor | Traceability | `vocabulary_version_create.feature:75` "A new version for a DEPRECATED vocabulary is created..." | SIMPL-18170/AC2, D3 | SIMPL-18170/AC2 is about reading a newly created vocabulary. The scenario tests D3 on version creation. | Drop `@AC_SIMPL-18170_AC2` and keep `@D3`. |

Checked and found correct: the S1 to S7 rules and their locations, D47 texts, the internal reference shapes (D69, D146, D192), external search order and oneOf handling, paging links (D78, D110, D111), check order on reads and writes (D51, D162), 405, 406 and 415 handling, D43 with 4 parallel writes, and the store-down 503 body.

## 3. Questions for the decisions

1. **Token clock skew (E8, D132).** How much clock skew may the service allow on `exp` and `nbf`? The library defaults are 60 s, but E8 expects a 10 s token to give 401 soon after it expires.
2. **URL and header limits (D158, D178).** At what URL length must the answer be 414, and at what header size 431? D158 tells the build to raise its limits but gives no figure.
3. **Cancel on the forms (D60).** Where does Cancel go: always to `/vocabularies` (or `/external-vocabularies`), or back to the previous page?
4. **Problem titles for 400, 404 and 500 (D55, D48).** Are the OpenAPI example titles ('Bad Request', 'Resource Not Found', 'Internal Server Error') binding, or free text like the other texts D55 leaves open?
5. **Warnings in a 400 (D10).** May a 400 VocabularyValidationProblem carry `validationWarnings`, or must the field be absent?
6. **External vocabularies screen on a shared store (D210, E4).** The screen has no filter and the store is never reset. Should it get a name filter, or is paging through the list the expected way for tests to find their rows?

## Answers of the test writer (45b)

SR-01: fixed. The expired-token step now waits `expires_in + 70` seconds, which is past the 60 s default clock skew of the libraries D132 names. Tags and meaning are unchanged.
SR-02: fixed. The Cancel scenario now checks only that the form route was left and that no POST was sent.
SR-03: fixed. Added `I filter "Name contains" with "$RUN"` before the row checks.
SR-04: fixed. The feature text and the steps now say that rows are found by pressing "Next page", and that the order check compares positions across pages.
SR-05: fixed. The three vocabularies are now created one second apart.
SR-06: fixed. The outline is split in two: titles are checked only for 403 (D17) and 406 (D63). The 404 and 400 rows check status and type only (D55).
SR-07: fixed. The load test now uses 3 writers at 1/3 write per second each (`LOAD_WRITE_USERS`). That stays below the 4 upload slots of D92.
SR-08: fixed in part. I removed the `@Q2 @NFR-04` tags. I kept both scenarios at 200 000 characters, because D178 says "the suite tests the exact codes" (414, 431). The size limit itself stays an owner question.
SR-09: fixed. Added `no vocabulary is named exactly "<name>"`, which checks names the D19 filter accepts, such as `Ab`.
SR-10: fixed. Dropped the check that `validationWarnings` is absent, renamed the scenario, and retagged `@D11` to `@D12`.
SR-11: fixed. The API scenario is skipped when the two creations fall on different UTC days. The UI scenario now uses the stored creationDate day instead of today's date.
SR-12: fixed. Added `the external vocabulary "$Ext" is registered` (a GET that must return 200) after the 403.
SR-13: fixed. Added the warning check in the deprecated version's row. I removed the self-link scenario and noted in the feature why D64's self-link case cannot be reached under D47.
SR-14: fixed. The step now says "the row of version 1".
SR-15: fixed. Added five scenarios:
- a query parameter on DELETE
- a query parameter on `POST /external-vocabularies`
- a form field sent twice on POST .../versions
- a form field sent twice on external registration
- 415 before 413 on create
SR-16: fixed. Removed `@AC_SIMPL-18170_AC2`; `@D3` stays.

While fixing this I also made two other changes:
- **Dry-run check:** a first rerun showed that the dry-run check could pass on an old report when a feature file failed to parse. It now deletes the old report first and fails on a parse error.
- **README:** updated for the three writers and the longer token wait.

Dry run: all 18 feature files parse. The non-UI run (598 scenarios: API, store-down and `@wip`) has 0 undefined or ambiguous steps. The 69 `@ui` scenarios have 399 undefined steps, as intended, because step 60 adds the browser steps. `COVERAGE.md` is regenerated: 372 scenarios, every live criterion covered, and 66 decisions without a scenario, each with a reason.
