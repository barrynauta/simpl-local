# Suite review

## 1. In short

- **Scenarios reviewed:** all 456 after outline expansion. That is 400 API, 6 store-down and 50 UI scenarios, plus `load/run_load.py`. A dry run (`behave --dry-run`) parses every feature and finds no undefined steps.
- **Overall:** the API scenarios are strong. They follow the decisions closely, including the late ones (D140 to D166), and check exact texts only where a decision fixes them. They prove "nothing stored" through the run-id filter as E4 and D49 ask. The serious problems are in the UI scenarios and the load test, not in the API scenarios.
- **Findings by severity:** 2 blockers, 5 majors, 13 minors (20 in total).
- **Findings by kind:**

| Kind | Count |
|---|---|
| Over-assertion | 4 |
| Under-assertion | 8 |
| Misreading | 1 |
| Proof that cannot work | 2 |
| Fragile | 2 |
| Traceability | 1 |
| Gaps | 2 |

## 2. Findings

| Id | Severity | Kind | Scenario | Decision or criterion | What is wrong | What the scenario should check instead |
|---|---|---|---|---|---|---|
| SR-01 | blocker | fragile | ui_list.feature:5, :16, :90, :99, :108, :116, :129, :139, :155, :163; ui_forms_details.feature:101, :164 | E4, D34, D23 | These scenarios open `/vocabularies` without a filter and look for the row of `{{Health}}`. The list is sorted by name with 20 rows per page. The store is never reset, and every API run adds many names that sort before "Health" (`Aa…`, `AZ…`, `Alpha…`, `Base…`, `Beta…`, `Cpsv…`). So "Health<run>" will not be on page 1, and a correct build fails. | First type the run id in "Name contains", as ui_list.feature:50 already does. Or open `/vocabularies/{{Health}}` directly when the scenario is not about the list. |
| SR-02 | blocker | proof that cannot work | load/run_load.py, `seed()` with `LOAD_SEED_THREADS` default 8 | D92, D99, Q1 | D92 lets one instance handle at most 4 uploads at a time; a fifth gets 503. The seed sends up to 8 uploads in parallel and does not retry. A correct build therefore refuses part of the seed. The read mix then hits missing names or versions and gets 404s, so `failures == 0` fails and Q1 is reported as not met. | Seed with at most 4 threads per backend instance. Retry a 503 after its Retry-After. Check that the seed is complete (1000 by 20) before the mix starts, and stop with a clear error if it is not. |
| SR-03 | major | misreading | load/run_load.py, `Writer` (one user, `constant_throughput(1)`) | D99, Q1 | D99 asks for an offered rate. One closed-loop writer cannot offer 1 write/s once a write takes more than 1 s. Q1 allows writes up to a p95 of 3 s, so a build that meets Q1 can still fail the 95 % throughput check. | Use an open model: start one write per second whatever the latency, for example with 3 to 4 writer users. Keep the in-flight uploads within D92's limit of 4. |
| SR-04 | major | under-assertion | status_change.feature:39 "A status change updates the last update date of the vocabulary" | D25, D160 | It checks only that `lastUpdateDate` is "not earlier than" the value before the PATCH. A build that never updates `lastUpdateDate` returns the same value and passes. | After the 1.2 s wait, check that `lastUpdateDate` is strictly later than "before", or equals the time of the PATCH to the second. |
| SR-05 | major | over-assertion | access_control.feature:293, TRACE example | D122, D166, D126 | D122 lets the front proxy answer TRACE itself with a 405 problem+json, and does not require an Allow header. D166 is about TRACE that reaches the backend inside the network. Since D102 (two backends), the API port is likely the proxy, so a correct build may answer without Allow. | For TRACE, check 405 and problem+json only. Keep the Allow check for HEAD and OPTIONS (D126). See question Q-1. |
| SR-06 | major | over-assertion | cross_cutting.feature:26 (414), :33 (431) | D158, D122 | D158 fixes 414 and 431 for the backend's own HTTP server. When a proxy sits in front, D122 lets it answer a malformed or oversized request with 400 problem+json, and nginx answers oversized headers with 400. A correct build behind a proxy fails the 431 check. | Accept 431, or 400 problem+json with the D48 type, for oversized headers; or skip the scenario when the proxy answers. See question Q-1. |
| SR-07 | major | proof that cannot work | ui_list.feature:163, example `ga-norole` | D18, D16 | ga-norole gets 403 on `GET /vocabularies`, so the UI shows "You are not authorized to perform this action." and no list (D18). The steps "the actions menu of {{Health}} does not offer …" need a row that cannot exist. They fail or pass by accident, depending on how step 60 implements them. | For ga-norole, check the authorisation message and that "New Vocabulary +" is absent. Keep the row-menu checks for ga-viewer only. |
| SR-08 | minor | over-assertion | external_vocabularies.feature:63 | D30, D41 | D30 says only "no warnings". A build that returns `validationWarnings: []` follows the decision but fails the "is absent" check. | Accept the field being absent or an empty array. |
| SR-09 | minor | under-assertion | create_vocabulary.feature:208, more than 64 parts | D110, D52 | The 61 filler parts are unknown fields, so D52 already gives 400. A build without the 64-part limit passes, so the scenario does not prove D110. | This cannot be told apart in a black-box test as decided. Tag the scenario D52 as well and record in COVERAGE that the D110 limit is not proven on its own. See question Q-3. |
| SR-10 | minor | under-assertion | access_control.feature:34, kinds `expired` and `tampered` | D132, D17 | Both tokens are built by editing the claims, which breaks the signature. The 401 comes from the signature check, so the expiry check is never tested even though the scenario says it is. | Rename them as signature cases, and either add a real expired token (a test client with a short token lifespan) or list the expiry check as a gap in COVERAGE. |
| SR-11 | minor | fragile | list_vocabularies.feature:131, :139 | D20, D58 | `{{today}}` and `{{tomorrow}}` are worked out when the step runs. A scenario that runs across midnight UTC gets the wrong day. | Take the day from the stored `creationDate` (remember the field and use its date part), and the next day from that. |
| SR-12 | minor | traceability | read_vocabulary.feature:152 (`@AC_SIMPL-18175_AC2` on a GET); create_vocabulary.feature:63 (`@AC_SIMPL-18187_AC1` on the CPSV upload) | SIMPL-18175/AC2, SIMPL-18187/AC1 | SIMPL-18175/AC2 is about the status-change PATCH, not GET. The CPSV scenario is the D1 proof for SIMPL-393, not the persistence criterion. Both tags inflate the coverage figures. | Remove both criterion tags. Keep `@D24 @D66` and `@D1`. |
| SR-13 | minor | under-assertion | ui_list.feature:23; ui_forms_details.feature:234 | D72, D114 | Load errors must show the D34 or D35 text plus the problem detail in one alert, and not the D60 text. The scenarios check only the first text. | Also check that the alert contains the stubbed problem detail, that there is exactly one alert, and that "Something went wrong" is absent. |
| SR-14 | minor | under-assertion | ui_forms_details.feature:113, "the form has no field Version" | D7 | The check runs after the UI has moved to the details page, so it is always true. | Move the check to the Update form, before Submit. |
| SR-15 | minor | under-assertion | ui_forms_details.feature:176 | SIMPL-18176/AC3, D35 | The version panel must show the version, status, date created, creator, description and change log. Date created and description are not checked. | Also check that "Version 1" contains the date in the D60 format and the description. |
| SR-16 | minor | under-assertion | ui_list.feature:42; ui_list.feature:16 | D34, D60 | The first does not check 20 rows per page. The second checks only the substring "UTC", not the 'YYYY-MM-DD HH:mm UTC' format. | Count the rows on page 1 when total is above 20, and match the date against the D60 pattern. |
| SR-17 | minor | gaps | Q2 limit pairs | Q2, D37, D56, D76, D150 | Q2 asks for one test at each limit and one above it. These "at the limit" cases are missing: a 3-character name accepted, a JSON body of exactly 1 MB, a multipart request of exactly 6 MB, an external description of 1000 characters, an external file of exactly 5 MB. COVERAGE says Q2 is covered without listing them. | Add the at-limit scenarios, or list each missing one in COVERAGE with a reason. |
| SR-18 | minor | gaps | (none) | D121, D63, D70, D15, Q6 | 1. COVERAGE says a part with an empty name "cannot be sent reliably", but `requests` can send `name=""`. 2. No scenario sends a JSON request with no Accept header (D63). 3. The 'a,b' alias case of D70 is missing. 4. There is no 415 on the external search. 5. Q6 keyboard reachability has no UI scenario. | Add short scenarios for these, or give valid reasons in COVERAGE. |
| SR-19 | minor | over-assertion | ui_forms_details.feature:113, "I am on the page /vocabularies/{{Health}}/versions/2" | D113, D163, D31 | D113 says the UI "opens the details page" (route `/vocabularies/<Name>`, D31) with the new version's panel open. A build that keeps `/vocabularies/<Name>` and opens the panel fails. | Check that the panel "Version 2" is shown and selected (`aria-current`), and accept either route. See question Q-2. |
| SR-20 | minor | under-assertion | create_version.feature:178 (415 on a new version) | D8 | It checks the version count only, not that version 1 stays ACTIVE ("no status change of older versions"). The 413 scenario does check this. | Add "version 1 of vocabulary … has status ACTIVE". |

## 3. Questions for the decisions

- **Q-1 (D102, D122, D158, D166, D126): which server answers at `API_URL`?** With two backends behind one port, the API port is a proxy. Decide whether that proxy must also:
  - send an Allow header on its own 405 answers (TRACE);
  - answer oversized headers with 431 rather than 400;
  - answer an over-long URL with 414 as problem+json.
- **Q-2 (D113, D163): the route after a new version.** Should the URL be `/vocabularies/<Name>/versions/<n>` or `/vocabularies/<Name>` with the panel open?
- **Q-3 (D110, D52, D51): the 64-part limit.** For a request with more than 64 parts, which issue is expected (name, `in`), and does the part-count check run before the unknown-field check? As written, the limit cannot be seen apart from D52.
- **Q-4 (D65, D140): IRIs in S1 and S2 statement locations.** D140 asks for N-Triples form for literals, but in N-Triples an IRI sits in angle brackets. Are IRIs written bare (as in the OpenAPI example and the suite) or as `<IRI>`? Should a plain literal be written with `^^xsd:string`?
- **Q-5 (D68, D110): issue names for internal search items.** D68 names them `items[i].name` or `items[i].version`. D110 names a bad array element `<array>[<index>]`. Confirm that D110 applies only to the external URI arrays.
- **Q-6 (D92, D99, Q4): concurrent uploads in the load test.** State whether the load test must keep to 4 uploads in flight per instance, or whether a D92 503 that is retried does not count as an error for Q1.
- **Q-7 (D26, D38): the href of a search result with `latest: true`.** Should it be `/vocabularies/<Name>` (as in the OpenAPI example and the suite) or the href of the latest version?

## Answers of the test writer (45b)

SR-01: fixed
SR-02: fixed
SR-03: fixed
SR-04: fixed
SR-05: fixed
SR-06: fixed (the 431 scenario now accepts 431 or 400, both as problem+json. I kept 414 as it was: D122 lets the proxy answer 400 only for a malformed request, and a long URL is not malformed.)
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
SR-17: fixed, except one case: I did not add "a 3-character name is accepted", because a 3-character name cannot hold the run id. Names are unique case-insensitively (D37) and the store is never reset (E4), so a second run would get 409. COVERAGE.md now lists this, and the 2-character rejection is still tested. The other four at-limit cases are added: JSON body of exactly 1 MB, multipart request of exactly 6 MB, external description of 1000 characters, external file of exactly 5 MB.
SR-18: fixed (empty-name part, no Accept header on a JSON and a content read, the "a,b" alias, 415 on the external search, and a Q6 keyboard scenario)
SR-19: fixed
SR-20: fixed

Dry run: `behave --dry-run` parses 470 scenarios and 2635 steps, with 0 undefined steps. `tools/coverage.py` reports no gaps without a reason.
