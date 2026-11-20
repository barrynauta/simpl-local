# BUILD-LOG.md

Run 9, step 40 (code writer), 19 Nov 2026.

## Course of the step

1. An earlier attempt of this step built the backend, store, realm files, UI, ops scripts, docs, smoke script,
   SBOMs and a first Trivy scan, and was then interrupted. It left no build log, no TRACEABILITY, REUSE, QUALITY,
   DEVIATIONS or SECURITY-SCAN file, and no record of the scan counts before its fixes.
2. This attempt checked what was there against the decisions (D1 to D99, E1 to E7, Q1 to Q6) and kept it: the
   code covers the decisions, `mvn verify` was green, the stack was running and healthy.
3. Fix in this attempt (1 iteration): the validation report said '1 error found' for one issue. D90 fixes the
   text as '<n> errors found', so the UI now always says 'errors' ('1 errors found'). UI test updated, image
   rebuilt.
4. Finish line run from a clean state (`down -v`, then `./stack.sh up`), smoke test, store-down check, own fuzz
   check, backup and restore round trip, own load measurement, Trivy on `work/` and on all four images.

## Finish line results

| Item | Result |
|---|---|
| Start from a clean state | `./stack.sh up` returned healthy (images from the build cache, about 30 s; the UI image was rebuilt later after the fix) |
| `smoke.sh` | passed: 401 without token, 403 for ga-viewer, create, new version, list, history, download, search |
| `./stack.sh store-down-up` | backend on 8199: `GET /v1/health` 503, `GET /v1/vocabularies` 503 with Retry-After 5 |
| `mvn verify` | 59 tests green, Spotless check, JaCoCo line 92.0 %, branch 77.9 % |
| UI build | ESLint, Prettier, vue-tsc, 14 Vitest tests green |
| Trivy | own images and dependencies 0 HIGH/CRITICAL; Keycloak 1 CRITICAL, 6 HIGH without a usable fix (SECURITY-SCAN.md) |
| Own fuzz check | Schemathesis 4.33.4 (temporary venv in /tmp, not installed system-wide), published contract, `not_a_server_error`, 30 examples, seed 1: 1241 cases, 0 server errors. With all checks against `docs/openapi-decided.yaml`: 1394 cases, 3 failures, all decided behaviour (below) |
| 405 on undeclared methods | PUT, DELETE and QUERY on `/vocabularies`, GET on `/vocabularies/searches`: 405 |
| Stack left running | yes |

## Gaps where the specs and decisions still left a choice

1. D90 fixes '<n> errors found' also for n = 1, which reads badly. The build follows the text literally. The owner
   may want '1 error found'.
2. Fuzzing finds three "schema-valid request rejected" cases that are decided behaviour but not expressed in
   either contract file: a date that matches the pattern but is not a calendar date (`2705-03-61`, D20), and a
   binary file in `vocabularyFile` (D9, D70, D79). The decided contract could state `format: date` and
   `format: date-time` on the filters, and that the file must be Turtle, so the fuzzer can tell; step 70 needs
   these in its known list.
3. Decided-contract choices the decisions do not fix, kept from the earlier attempt and listed in
   `docs/api-notes.md`: order of parameters in paging links (page and pageSize first); issue name for a bad
   element of a search array (the array name); `aliasUris` always present; `createdBy` and `latestVersionHref`
   also on the 201 body; JSON body limit 1 MB with 413; at most 64 multipart parts (400); date-times without an
   offset read as UTC; `GET /health` applies the Accept and unknown-parameter checks.
4. The warnings panel text ('<n> warnings found', role status) is not fixed by D90; the build uses the same form
   as the report line.
5. D94 names Nimbus `JWKSourceBuilder`; its refresh rules cannot give the exact D88 outcomes, so the fetch timing
   is own code on Nimbus' `JWKSource` interface (REUSE.md).
6. D99 asks for a 5 minute load test on a 20 000-version seed. Seeding that through the API at the decided write
   rate takes hours; a direct store load was not built. The own measurement is shorter and on a small store.

## Problems and how they were solved

- Earlier interruption: no record of what was done. Solved by checking the code against each decision and
  running the full finish line again.
- In the service unit tests, MDC fields set by one test show up in log lines of the next test on the same
  thread. In the running service the request filter clears them after every request, so this is test-only.

## What does not work at the end

- Keycloak image findings stay open (no fixed upstream image).
- No load test at the D99 seed size; restore time at design load is not measured.
- Fix iterations in this attempt: 1. The number of iterations of the interrupted attempt is not known.
