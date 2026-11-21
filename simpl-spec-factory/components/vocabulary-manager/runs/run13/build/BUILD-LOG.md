# Build log: Vocabulary Manager, run 13 (21 Nov 2026)

Code writer of step 50. Inputs: decisions D1 to D167, E1 to E7, Q1 to Q6; stories; OpenAPI 1.0.0; CDM; rules; build profile; reuse catalogue.

## How the work went

1. Read the decisions, glossary, OpenAPI, CDM, reuse catalogue, the step-50 rules and the story criteria.
2. Probed Fuseki 6.2.0 by hand for the store access of D83 (per-endpoint users, admin endpoints). Built the Fuseki image from the Maven Central jar, verified by SHA-512 (cross-checked with the published SHA-1).
3. The UI (Vue, PrimeVue unstyled, Tailwind, nginx proxy) was built by a parallel worker from the same inputs and brief; I integrated it into the stack and fixed one proxy gap (below).
4. Backend: Spring Boot 4.1.3 on Java 21, Jena 6.2.0, Nimbus 10.10, Resilience4j 2.4.0, Micrometer Tracing (Brave). 80 own tests (unit tests plus integration tests against an embedded Fuseki and a local JWKS endpoint).
5. Stack: Fuseki, Keycloak, two backend replicas, nginx proxy; `stack.sh` creates `.env` with random secrets.
6. Finish line runs: smoke, `mvn verify`, Trivy, a local Schemathesis run, backup and restore round trip, load test.

## Fix iterations

| # | Problem | Fix |
|---|---|---|
| 1 | Backend containers restarted at start: Spring Boot 4 no longer brings SnakeYAML, so `application.yaml` could not be read (tests passed because SnakeYAML is a test dependency). | Converted the configuration to `application.properties`; no YAML parser in the runtime. |
| 2 | Own test of the key cool-down expected a second fetch inside the 10 s window. | The test was wrong (D88 allows no fetch inside the window); corrected the test, not the code. |
| 3 | Keycloak 26.7.5 image: 2 CRITICAL and 18 HIGH findings. | Moved to Keycloak 26.8.2 (released 19 Nov 2026): 0 CRITICAL, 6 HIGH (see `SECURITY-SCAN.md`). |
| 4 | Schemathesis: TRACE answered by nginx with 405 but without `Allow` (D126, D139, D166 want Allow). nginx answers TRACE itself and never forwards it. | Added a `map` from path to allowed methods in the nginx config and an `Allow` header on nginx's own 405 answer. |
| 5 | Log lines about a created version lacked the `version` field (D96). | Version put in the MDC after the store write. |
| 6 | First load-test seed used 8 parallel writers through the proxy; a replica then had more than 4 uploads at once and answered 503 as D92 decides, so many creates failed. | Load test limited to 4 parallel writers (D119); the D99 seed is now bulk-loaded through the restore procedure (`scripts/seed-store.sh`), 22 s instead of about 45 minutes through the API. |
| 7 | Q1 missed on the default list: p95 329 ms (target 300 ms) at the D99 load. The query joined the latest description of every vocabulary before sorting. | Sort and page in a sub-query, join the description for the page only. Re-measured: p95 168 ms. |

## Gaps where the specs and decisions left me guessing (new findings)

| # | Gap | What I chose |
|---|---|---|
| G1 | D12, D69, D146: a namespace `<contentBase>/content/<Name>/versions/` (with nothing after `versions/`) is not covered: is `<v>` empty, so S7 under D69, or is it "any other path", so the latest version under D146? | Treated as a reference to the latest version (D146): only a non-empty segment after `/versions/` selects a version. |
| G2 | D46 says the uploaded bytes "equal the stored content bytes". | Compared through a stored SHA-256 of the content. Equal in practice; a collision is not a realistic case. |
| G3 | D83 wants admin endpoints off or only for an admin user; D156 wants the online `/$/compact`. Fuseki main protects admin endpoints only by the server-level user list, so the reader and writer users can also start a compaction. | Accepted and recorded in `DEVIATIONS.md`. Anonymous access is refused everywhere. |
| G4 | Duplicate single-valued form fields (for example two `name` parts) are not decided. | 400 with one issue (in: body, name: the field). |
| G5 | D110 "more than 64 multipart parts give 400" does not say the issue name. | One issue `in: body`, `name: body`. |
| G6 | Version list sort `status` and `creationDate`: tie-break is version descending (D23), but the direction of the primary key for `-status` is not stated beyond "prefix with -". | `-status` sorts DEPRECATED first, ties version descending. |
| G7 | The 401 detail text and 403 detail text are not fixed. | 401 'A valid bearer token is required.' with `WWW-Authenticate: Bearer`; 403 'You are not authorized to perform this action.' (the UI text of D18). |
| G8 | Which namespaces count as "W3C core" (D12): by prefix name or by IRI? | By IRI, after removing a trailing '#' or '/': rdf, rdfs, owl, xsd, skos namespaces. A file that binds `rdf:` to another IRI gets the normal external check. |
| G9 | S1 and S2 locations for a blank node subject or object. | `_:<label>` with the parser's label. |
| G10 | The browser syntax check (D13) uses n3.js, the server uses Jena RIOT: the two parsers can disagree on edge cases (for example some IRI forms). | Server stays authoritative (D13); the browser check only blocks Submit on its own errors. |
| G11 | D44 "a store error that is not a timeout or a connection failure stays 500": a store 401 or 403 (wrong store password) is also 500. | 500, and it does not count as a breaker failure (D134). |
| G12 | D98 asks for CPU and memory limits as variables; values are not decided. | backend 1.5 CPU and 1 GB each, Fuseki 2 CPU and 2 GB (heap 1 GB), Keycloak 2 CPU and 1 GB, proxy 0.5 CPU and 128 MB. |
| G13 | Schemathesis flags `name=000` on GET /vocabularies as "schema-violating request accepted" against the published contract. | D19 relaxes the pattern on purpose; it is in `openapi-decided.yaml` and should be in the fuzz-known list of step 70. |
| G14 | Seeding the D99 load (20 000 versions) through the API is slow on this machine: Fuseki commits one write transaction at a time (TDB2 has a single writer), about 7 writes per second with 8 parallel clients. | Accepted; it is a seed cost, not a Q1 target. Measured numbers in `QUALITY.md`. |

## Checks run (results)

- `./stack.sh up` from a clean state: all services healthy (Fuseki, Keycloak, 2 backends, proxy).
- `./smoke.sh`: 14 of 14 checks passed (also after the Keycloak upgrade and after the restore).
- `mvn verify`: 80 tests, 0 failures; Spotless check passed; JaCoCo 94.3 % lines, 83.4 % branches (thresholds 80 % and 60 %).
- UI image build: ESLint, Prettier check, 45 Vitest tests (83 % lines) and the production build pass inside the Docker build.
- Live probes: E7 tokens (no audience, other realm) 401; norole 403; HEAD, OPTIONS, TRACE, QUERY 405 with Allow; encoded slash 400; `/actuator` 401 without and 404 with a token; 6 MB file 413 from the service; 8 MB body 413 from the proxy; 40 000-character URI 414; store-down backend: 503 with Retry-After 5, health 503 DOWN.
- 5 MB create: 3.6 s; 5 MB new version: 3.3 s; download of 5 MB: 0.13 s (D71: 30 s).
- Schemathesis 4.33.4 (seed 1, 30 examples, all phases except stateful): 1241 generated cases with no 5xx. All remaining check failures are decided contract changes (G13).
- Load test (D99 seed, 20 reads/s plus 1 write/s, 300 s, two replicas): 100 % of the offered rate, 0 errors, every read p95 at most 168 ms, write p95 123 ms (`QUALITY.md`).
- Backup and restore round trip: 112 quads backed up, loaded into a new TDB2 directory, count matched, swapped, list total equal before and after; 16 s including stopping and starting the backends.
- Trivy: see `SECURITY-SCAN.md`.

## What does not work or was not done

- axe (Q6) was not run in this step (no browser); the UI was built for WCAG 2.1 AA.
- No SAST, mutation testing or CI pipeline (see `DEVIATIONS.md`).
- Keycloak keeps 6 HIGH image findings that the build cannot fix.
