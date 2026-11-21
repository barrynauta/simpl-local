# Suite review

## 1. In short

- **Scope:** 306 scenario definitions (550 cases once examples are expanded) in 18 feature files. I also checked the step library (`api_steps.py`, `vmtests/`), the fixtures, `README.md`, `COVERAGE.md` and the load test. `behave --dry-run` reports 0 undefined steps and 0 errors. The dry run wrote `work/reports/dry-run.json`, which the brief allows. No other file was changed.
- **Overall:** the API suite follows the decisions closely. It covers the check order (D51, D162), the decided texts (D47, D55, D66, D147), the paging links (D110, D111), validation locations (D65, D140, D142 to D145) and the "nothing stored" proofs through the token-filtered list (D49).
- **Findings by severity:** 2 blockers, 4 majors, 12 minors.
- **Findings by kind:**

| Kind | Count |
|---|---|
| Fragile | 3 |
| Over-assertion | 3 |
| Under-assertion | 7 |
| Proof that cannot work | 1 |
| Misreading or ambiguity | 1 |
| Traceability | 1 |
| Gaps | 2 |

## 2. Findings

| Id | Severity | Kind | Scenario | Decision or criterion | What is wrong | What the scenario should check instead |
|---|---|---|---|---|---|---|
| SR-01 | blocker | fragile | `ui_list.feature:93` "A click on another sortable column sorts it ascending" | D23, D50, D58 | Bravo and Alpha are created one after the other with no wait. Both usually get the same creationDate second. D23 then breaks the tie by name ascending, so a correct build shows Alpha, Bravo. The scenario expects Bravo, Alpha. | Add "I wait one second" between the two creations, as `vocabulary_list.feature:178` does. |
| SR-02 | blocker | over-assertion | `vocabulary_read.feature:57` "A vocabulary that does not exist gives 404", line 61 | D55, D66 | Checks the detail text of the issue inside the 404 problem. D66 fixes only the problem detail and the issue's `in` and `name`. D55 says tests check only the texts a decision fixes. The OpenAPI example is not a rule. A correct build may word the issue detail differently. | Drop line 61. Keep the problem detail, exactly one issue, and in: path, name: vocabularyName. |
| SR-03 | major | over-assertion | `http_protocol.feature:107` "TRACE is refused with 405" | D122, D166 | API port 8196 most likely goes through the front proxy: D102 puts two backends behind it with no host port per backend. D122 lets the proxy answer TRACE itself with a problem+json 405, but fixes no Allow header. Only the backend's own refusal (D166) carries Allow. | Check status 405 and problem+json only, or get a decision that the proxy also sends Allow (Q1). |
| SR-04 | major | over-assertion | `http_protocol.feature:177` (414) and `:183` (431) | D122, D158 | D158 gives 414 and 431 for the backend's server. The 200,000-character header and URL may be refused first by the proxy. Nginx answers an oversize header with 400, and D122 allows that for a malformed request. The decisions do not say which layer must answer, so a correct build can fail. | Keep these sizes under the proxy limits but over the backend limits, or accept 400, 414 or 431 as problem+json until Q1 is answered. |
| SR-05 | major | misreading | `ui_forms.feature:159` "A new version is created from the update form", line 166 | D113, D163 | Expects the route `/vocabularies/{Alpha}`. D113 opens the details page "with the new version's panel open". D163 makes the route of a selected version `/vocabularies/<Name>/versions/<n>`. A build that follows D163 ends on `/versions/2`. | Accept either route, or check the open region "Version 2" and the selected row instead of the URL (Q2). |
| SR-06 | major | proof that cannot work | `ui_list.feature:26`, line 32 | D34, D72, D114 | Checks that the alert contains "Internal Server Error" as "the problem detail". The stub step gives no response body, so the detail the UI should show is not fixed. A build that correctly shows the stub's detail can fail. A build that shows only the title can pass. | Define the stub body (a problem with a known detail) in the step, and check that detail. |
| SR-07 | minor | under-assertion | `ui_details.feature:92`, `ui_list.feature:35` | D35, D72, D114 | The history and list load-error scenarios check the decided text but not the problem detail that D72 and D114 require. | Add a check for the stub's detail, once SR-06 fixes the stub body. |
| SR-08 | minor | fragile | `vocabulary_list.feature:136` "The lastUpdated filter matches the last update date" | D20 | Expects Alpha and Bravo on the same UTC day. If the run crosses midnight UTC between Bravo's creation and Alpha's new version, Bravo is left out. | Check that Alpha is listed for its own date, and Bravo only when its lastUpdateDate has the same date. |
| SR-09 | minor | fragile | `vocabulary_create.feature:123` "A name of 3 characters is accepted" | D37, E4 | The random 3-character name is checked only with an exact GET. Uniqueness is case-insensitive and the store is never reset, so a name that differs only in case from an earlier run's name gives 409. | Check candidates through the case-insensitive name filter, or retry on 409 with a new name. |
| SR-10 | minor | under-assertion | `validation.feature:268` "A rejected upload returns errors only, not its warnings" | D10 | Proves only that the warning is not an issue. It does not prove that no validationWarnings come back on the 400. | Add `validationWarnings` = `<absent>` on the 400 body. |
| SR-11 | minor | under-assertion | `external_vocabularies.feature:273` "An invalid alias is rejected" | D56, D73 | Accepts any issue name that starts with "aliasUris". D56 and D73 fix the name as `aliasUris`; the `<array>[<index>]` form of D110 is for search arrays. | Check the issue named exactly "aliasUris". |
| SR-12 | minor | under-assertion | `ui_forms.feature:172` "A description-only change creates a new version with the current content" | D33 | Checks that version 2 exists, but not that its content equals version 1's bytes. | Download version 2 and compare it with the bytes of version 1. |
| SR-13 | minor | under-assertion | `ui_list.feature:105` "The list pages on the server with 20 rows" | D34, D115 | Checks only that the controls exist. Not checked: 20 rows per page, "Page 1 of <m>", or that "Next page" loads page 2. | Count the rows (the store always holds more than 20) and click "Next page". |
| SR-14 | minor | under-assertion | `ui_list.feature:17`, `ui_details.feature:35` (dates) | D60 | Checks only " UTC". D60 fixes the format 'YYYY-MM-DD HH:mm UTC'. | Match the regex `^\d{4}-\d{2}-\d{2} \d{2}:\d{2} UTC$`. |
| SR-15 | minor | under-assertion, traceability | `ui_access.feature:82` CSP scenario | D123 | Checks 2 of the 4 directives. It does not check connect-src (self and the identity provider URL) or the absence of inline scripts and styles ('unsafe-inline'). It is also tagged @ui, so `./run.sh api` skips it, although the README says it "runs as it is". | Check all of D123, and drop @ui or fix the README. |
| SR-16 | minor | over-assertion (possible) | `vocabulary_list.feature:169` "A filter that matches nothing" | D22, D59 | Expects totalPages 0 for an empty list. D153 fixes 0 for searches only. For the list, D22 says only "correct totalPages" (it is implied by "last is omitted when total is 0"). | Keep the check, and confirm through Q4. |
| SR-17 | minor | gap | none (read order) | D162 | No scenario for 406 before 400 on a read, for example a bad Accept with pageSize=0. | Add one read scenario: Accept text/html plus `pageSize=0` gives 406. |
| SR-18 | minor | gap | none (UI) | D35, D76 | No scenario for these two UI rules: the version list fetches further pages when total is over 100, and a server 413 detail appears above the form. `COVERAGE.md` gives no reason. | Add UI scenarios with a stubbed 413, and a stubbed history with total 101. |

Note for step 60, not a finding: the fixtures used in the UI forms contain `{name}`, `{Ref}`, `{base}` and `{tok:lower}`. The UI fill-in steps do not set `current_name`, so step 60 must fill these placeholders itself before it uploads the file.

## 3. Questions for the decisions

1. **Q1 (D122, D158, D166): which layer answers protocol refusals on the API port when a proxy fronts it?** Does a proxy-answered TRACE carry Allow? Must an oversize header give 431 (backend) or may it give 400 (proxy, malformed request)? Does the same hold for an oversize URL (414)?
2. **Q2 (D113, D163): what is the route after a successful new version?** D113 opens the details page with the new version's panel open. Is the route `/vocabularies/<Name>` or `/vocabularies/<Name>/versions/<n>`?
3. **Q3 (D55, D66): is the issue detail inside a 404 fixed?** If not, it is free text like the 400 issues, and the OpenAPI example is only an illustration.
4. **Q4 (D22): what are totalPages and the paging links for an empty list?** Is totalPages 0 (as D153 fixes for searches) or 1?
5. **Q5 (D38, D137): what is the issue name for an empty internal search array?** For `{"items": []}`, is it "items"? D137 names only the external arrays.

## Answers of the test writer (45b)

I fixed 15 of the 18 findings and rejected 3 (SR-04 is fixed in its accept-400/414/431 form). The dry run is clean.

SR-01: fixed
SR-02: fixed. The extra issue-detail line is gone; the problem detail, exactly one issue, and `in: path`, `name: vocabularyName` are still checked.
SR-03: fixed. TRACE now checks only a 405 problem; Allow is no longer required, because D122 lets the proxy answer.
SR-04: fixed. The 414 and 431 scenarios now accept a problem+json 400, 414 or 431, because D122 lets the proxy answer an oversize request with 400.
SR-05: fixed. Either route is accepted (`/vocabularies/{Alpha}` or `/vocabularies/{Alpha}/versions/2`), and the scenario also checks that version 2's row is selected (`aria-current`).
SR-06: fixed. The stub step now returns a problem with a known detail, and the scenario checks that detail.
SR-07: fixed. The list 503 and history 500 load-error scenarios now check the stub's detail.
SR-08: fixed. The date check now only requires Alpha; Bravo is no longer expected.
SR-09: fixed. A random 3-character name is now checked against the case-insensitive name filter before use.
SR-10: rejected, because D10 defines `validationWarnings` only for the 201 response, and nothing decides the shape of a 400 VocabularyValidationProblem beyond its issues. Under D55, tests check only what a decision fixes.
SR-11: fixed. The issue name must now be exactly `aliasUris`.
SR-12: fixed. The scenario now checks that version 2's content equals version 1's.
SR-13: fixed. The scenario creates 21 vocabularies and filters on its own token. It checks 20 rows and "Page 1 of 2", then 1 row and "Page 2 of 2" after "Next page".
SR-14: fixed. Dates must now be shown as `YYYY-MM-DD HH:mm UTC` (D60), in the list and in the version panel.
SR-15: fixed. The scenario now also checks that `connect-src` holds `'self'` and the identity provider, and that there is no `unsafe-inline`. The `@ui` tag is dropped, so it runs in `./run.sh api`, and the README now says so.
SR-16: rejected, because no change is needed. D22 asks for the correct `totalPages`, and D59 omits the `last` link when total is 0, so 0 is the only consistent value. That matches D153.
SR-17: fixed. New read scenario: `Accept: text/html` with `pageSize=0` gives 406 (D162, D63).
SR-18: fixed. Two new `@ui` scenarios: a stubbed server 413 shows its detail above the form (D76), and a stubbed history with total 101 makes the page load page 2 with `pageSize=100` (D35).

Dry run (`./run.sh dry-run`): 0 undefined steps and 0 errors, with 553 cases and 3,117 steps untested. `COVERAGE.md` has been regenerated: every criterion still has a scenario.
