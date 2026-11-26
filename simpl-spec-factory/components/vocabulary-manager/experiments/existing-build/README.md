# Experiment: the run 14 test suite against the existing Simpl build

Date: 26 Nov 2026. Status: done, API part only.

## Question

How close does the existing Simpl-Open Vocabulary Manager (upstream backend `main`) come to the written
decisions, and how much of the factory's test suite measures the decisions rather than the specs?

## Method

- The run 14 API suite (582 cases, `not @ui and not @wip and not @store_down`), unchanged, in a copy of the
  test workspace, with `API_URL` pointed at the local evaluation stack of the existing build
  (`simpl-vocabulary-manager`, API on port 8086, demo seed loaded).
- Tokens came from the run 14 Keycloak (realm and users of E1). The existing backend decodes the token for the
  `email` claim but does not check it; the local stack has no gateway or Keycloak in front of it.
- The UI suite (71 cases) was not run: the existing local UI skips the Keycloak login and every UI case starts
  with that login, so all would fail on the first step and say nothing. The side by side click-through of
  23 Nov (D208 to D210) covers the UI.
- Failures were sorted by cause from the expected and actual status and the assert message, then the larger
  groups were checked by hand with curl.

## Results

246 of 582 pass (42 %); 333 fail, 3 stop with a step error.

| Cause | Cases | What it means |
|---|---|---|
| Access control expected (401, 403) | 56 | The local stack has no authorization. In production a gateway does this, so these say nothing about the backend. |
| Input the decisions refuse is accepted (400 expected, 200 or 201 given) | 91 | Paging values, registration fields, extra parameters, validation rules: the decisions are stricter than the build. |
| Right status, different error body or warning code | 90 | Belgif problem details are there, but `issues` entries, codes or texts differ from what the decisions fix. |
| Other status code | 26 | For example references in uploaded Turtle refused with 400 where the decisions accept them (12), 413, 414 and 431 limits answered with 400, 406 and 415 in another order. |
| Behaviour differs | 56 | Version history oldest first (decisions: newest first), status filter on the version instead of the vocabulary status, `createdBy` and `latestVersionHref` left out of version responses (optional in the OpenAPI), 5 MB file downloaded with different bytes, external content type with charset. |
| Server error 500 | 14 | An unknown path or a method the path does not declare (PUT, DELETE, TRACE) gives 500 instead of 404 or 405. |

### Likely real defects in the existing build

These do not depend on any factory decision:

1. **500 on unknown paths and undeclared methods** (14 cases, confirmed with curl: `PUT /v1/vocabularies` and
   `GET /v1/nothing-here` both give 500).
2. **Concurrent new versions get the same number**: four overlapping uploads of a new version all returned
   version 2.
3. **Concurrent creations of one name both succeed**: two overlapping creations give two 201s instead of 201 and
   409. Two identical overlapping status changes both give 200.
4. **Limits answered as plain 400**: a too long URL and too large headers give an HTML 400 from the servlet
   container, not 414 or 431 as problem+json.

## What it means

- **The suite mostly measures the decisions.** About 260 of the 336 failures (stricter validation, error bodies,
  status order, behaviour choices) are cases where the upstream code made a different or no choice and the
  decisions fixed one. That is the bias the cross-vendor test is meant to look at: the tests check the factory's
  own decisions, and a build from the same decisions passes them (run 14: 664 of 668).
- **Where the decisions and the build differ, the decisions are not automatically right.** Version history order,
  the status filter and the handling of references in Turtle are owner questions, not build bugs.
- **The suite does find real defects in the existing build.** The four above are worth reporting upstream once
  checked against the deployed version.

## Limits

- One run against a local evaluation stack, not a deployed Simpl environment. A gateway in front could change
  the 401, 403 and possibly the 414 and 431 results.
- The sorting into causes is mechanical with hand checks on the larger groups; a few cases may sit in the wrong
  group.
- Upstream UI not measured.
