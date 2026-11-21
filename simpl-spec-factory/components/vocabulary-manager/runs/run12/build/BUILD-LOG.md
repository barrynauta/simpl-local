# Build log (run 12, 20 Nov 2026)

## How the build went

1. Read the decisions (D1 to D167, Q1 to Q6, E1 to E7), glossary, OpenAPI, 35 stories, BP02B, CDM, reuse catalogue and rules.
2. Built and tested the Fuseki 6.2.0 image first (Shiro users: read, update, admin; `$/ping` anonymous; admin, backup and compact endpoints checked by hand).
3. A separate worker built the UI (`ui/`, see `ui/UI-NOTES.md`) in parallel from the same decisions; I built the backend, store, Keycloak realms, gateway, compose file, scripts and documents.
4. Backend first run inside the stack, then fixes (below), then 59 own tests with an in-process Fuseki and a stand-in key server, then the scans, fuzzing, the UI walk-through, axe, a reduced load measurement and the backup and restore round trip.

## Problems and how they were solved (fix iterations: 11)

| # | Problem | Fix |
|---|---|---|
| 1 | UI image build failed: Prettier checked `UI-NOTES.md` | Notes excluded from Prettier and the Docker context |
| 2 | Backend did not start: Spring Boot 4 no longer brings SnakeYAML for `application.yaml` | SnakeYAML as a runtime dependency |
| 3 | `TokenVerifier` had two constructors | `@Autowired` on the public one |
| 4 | Liveness probe got 401: the API security chain also covered the management port | Separate security chain for the management port (matched on the local port) |
| 5 | Store answered 400 to the batch lookup: Jena 6 `setValues` writes `("a")` rows, which need `VALUES (?x)` | `VALUES (?name) { ?names }` form everywhere |
| 6 | Encoded slash got Tomcat's HTML 400 page (D158 wants problem+json) | `ProblemErrorReportValve` replaces the host error report valve |
| 7 | `+02:00` in a date-time filter was read as a space | Query strings are decoded with RFC 3986 rules ('+' stays '+') |
| 8 | Two-instance test: a request that arrived while another request fetched the realm keys got 401 | The unknown-key path is serialised, so it waits for a fetch in progress (D88 cooldown kept) |
| 9 | Restore: Fuseki did not restart (read-only config copy) and its request log printed SPARQL text with creator e-mails (against D80) | Config written fresh at start; Fuseki action log set to WARN through its own log4j2 file |
| 10 | Rule CODE-01 asks for Palantir format; first build used Google format | Spotless switched to Palantir 2.80.0, code reformatted |
| 11 | More than 64 multipart parts gave 413 instead of 400 (D110) | Container exceptions split into size (413) and shape (400) |

## Results

- `./stack.sh up` from a clean state (compose down -v, image tags removed, no .env; BuildKit layer cache kept): exit 0 in 28 s, all services healthy (two backend replicas), smoke PASSED.
- `./smoke.sh`: PASSED (create, new version, list as viewer, history, download bytes equal, search, viewer 403 on create, no token 401). Also passed after the restore.
- `mvn verify`: 59 tests green; JaCoCo 92.8 % lines, 82.4 % branches; Spotless check green.
- UI build: ESLint, Prettier, vue-tsc, 32 Vitest tests green (UI worker).
- Own fuzzing with Schemathesis 4.33 (seed 1, 30 examples) against the published OpenAPI: 1241 cases, no 5xx. With all checks: 14 findings, all decided contract changes (D19 relaxed name filter accepted; D20 invalid date, D38 empty array, D52 unknown property, D73 URI, D9 binary file, D91 integers above int32 rejected).
- Probe of decided edge cases on the running stack: 405 with Allow before 401 (PUT, QUERY, HEAD, TRACE), GET /vocabularies/searches 405, unknown path 401 or 404, /actuator 404, 406 and `application/*`, 415, 400 for bad path values, repeated and unknown parameters, encoded slash 400 problem+json, 409 texts, identical version 409, `version` field 400, 4 parallel new versions get 2 to 5 with only the newest ACTIVE, page beyond the last with prev to the last page, 413 at 5 MB + 1 and for a 6.6 MB request, external register, search, content, delete.
- Store down (`./stack.sh store-down-up`, port 8199): GET /vocabularies and POST /vocabularies 503 with Retry-After 5 and the D44 detail; GET /health 503 DOWN; container stays healthy (liveness).
- UI walk-through with Playwright (Chromium) against the stack: admin logs in at Keycloak, list with the D159 columns, New Vocabulary + form, browser check 'Syntax check passed.', submit opens the details page with 'Version 1 of <Name> was created.', Deprecate from the row menu shows 'Status of <Name> changed to Deprecated.'; viewer sees no 'New Vocabulary +' and gets 'You are not authorized to perform this action.' on /vocabularies/new. No console errors.
- axe-core 4 (WCAG 2.1 A and AA) on list, details, create and new version pages: 0 violations.
- Backup and restore round trip: backup 565 quads, a vocabulary created after the backup, restore loads 565 = 565 quads, list total back to the backup state, smoke green; compaction task accepted.
- Trivy: 0 HIGH or CRITICAL in own images and dependencies; Keycloak image 6 HIGH without a usable fix (SECURITY-SCAN.md).
- Reduced load measurement: QUALITY.md.

## Where the specs and decisions still left a choice (new findings)

1. **'+' in query strings.** D20 and D130 allow offsets such as `+02:00`, but no decision says whether '+' in a query means plus (RFC 3986) or space (HTML forms). Chosen: plus. A client that sends a space as '+' in the name filter gets a 400 (D19 pattern), which is safe.
2. **Size check versus part count.** D51 puts 413 before 400 and D110 says more than 64 parts is 400. A request that is both over 6 MB and over 64 parts gets 413 (size first).
3. **Place of the upload limiter 503 (D92) in the order of D51.** Chosen: after 401, 403, 406, 415 and the Content-Length 413, before the multipart body is read (so a fifth upload costs no memory).
4. **Base IRI on a retried write (D57 with D82, D92).** Validation runs once with version n+1 of the first read; when a concurrent write makes the stored number n+2, relative IRIs were resolved against n+1. The stored bytes are unaffected (D29); only S1 to S5 locations could name the other base.
5. **HEAD, OPTIONS and unknown methods on an unknown path.** D126 covers declared paths only. Chosen: 405 everywhere for methods the server never serves; known methods (GET, POST, PUT, PATCH, DELETE) on an unknown path get 401 or 404.
6. **The store's own logs and D80.** D80 covers the service's logs; Fuseki's request log prints SPARQL text with creator e-mails. Chosen: the store request log at WARN. A decision should say that D80 covers every container of the deployment.
7. **First key fetch under parallel requests (D88, D108).** The decisions do not say what a request sees while another request is fetching the keys. The race gave a 401 for a valid token; chosen: wait for the fetch in progress.
8. **Third-party images and Q5.** Q5 asks for no HIGH or CRITICAL in "own images". The Keycloak image is not own but runs in the stack and has 6 HIGH with no newer release; a decision should say whether it counts.
9. **Status-change receipts.** D82's conditional status change needs a way to know whether this request's change was applied; the build keeps one small `statusChange` token per change on the node (they accumulate). A later decision could allow a cleanup.
10. **UI texts not decided** (from the UI worker): field error texts, browser syntax error text, headings, version row menu, details header status text, version-not-found and download-failure texts, page counter with 0 vocabularies ('Page 1 of 1'), filter debounce. See `ui/UI-NOTES.md`, "Gaps and guesses".

## Assumptions

- Store descriptions and change logs are stored trimmed (D37 counts after trimming).
- A repeated single-value form field (for example two `name` parts) is 400 with an issue for that field (D52 spirit).
- The internal search item with `latest: true` carries href `/vocabularies/<Name>` and contentHref `<base>/content/<Name>` (OpenAPI example); with a version, the version paths.
- External search: every URI is matched against both namespace URIs and aliases, whatever array it came in (D38 "looked up against the namespace URI and the alias URIs").
- Content chunks of 1 MB raw bytes (6 chunks at most); a 0-byte file is stored as one empty chunk (but never stored, because S6 rejects it).

## What does not work or was not done

- Full Q1 load test at the D99 seed (1000 x 20 versions) and the 5-minute offered-rate run were not run; a reduced measurement is in QUALITY.md (list p95 305 ms at 20 closed-loop clients).
- The Keycloak image keeps 6 HIGH findings (no fixed release).
- No CI pipeline, Helm chart, mutation testing or Sonar run (DEVIATIONS.md).
- The UI was checked with one Playwright walk-through and axe, not with a full UI suite; keyboard-only use was not tested separately.
