# Build log (run 10, 20 Nov 2026)

## What was built

Backend (Java 21, Spring Boot 4.1.3, Maven), Fuseki 6.2.0 store, Keycloak 26.8.2 realms, Vue 3 UI with an nginx
proxy, store tools for backup and restore. Started with `./stack.sh up` or the raw start command of the build
profile. The stack is left running with two backend replicas.

## Where the specs and decisions still left me guessing (new gaps)

1. **D43 test against D92.** D43's test sends 5 parallel new versions and expects five 201; D92 says one instance
   handles at most 4 uploads and a fifth gets 503. On one instance the two decisions conflict when all five overlap.
   The build runs two replicas behind the proxy (D102), which spreads the requests: 3 rounds of 5 parallel requests
   through port 8196 all gave 201. On a single replica the fifth may get 503. Owner question: should the D43 test
   use at most 4 parallel requests per instance, or should D92 queue briefly instead of refusing?
2. **D98 and replicas.** "The management port is on an internal network and not on the front network" cannot be
   met per port with Docker networks when replicas have no fixed addresses; a container port listens on all its
   networks. Recorded in DEVIATIONS.md.
3. **D70 empty part name.** The servlet container drops a part with `name=""` before the application sees it, so
   the decided 400 is not produced without a hand-written multipart parser (which REUSE-02 forbids). DEVIATIONS.md.
4. **D67 and the proxy.** nginx answers TRACE itself (405, HTML, no Allow). The proxy now maps the path to the
   allowed methods and returns a problem body with `Allow`; the decisions do not mention that the front proxy, not
   the service, sees some requests first (also 400 for malformed requests, 413 above the proxy's own body limit).
5. **UI test tooling and CSP.** The UI sends a strict Content-Security-Policy (no inline script). A test that injects
   axe-core with a script tag is blocked; injecting through `page.evaluate` (as axe-playwright-python does) works.
   Owner question: should the CSP be decided, or should UI tests be told to use evaluate or `bypass_csp`?
6. **Base IRI for external registration.** D57 fixes the base for internal uploads only; for external vocabularies
   the build resolves relative IRIs against the namespace URI.
7. **Size of the validation report.** "All errors are returned" has no bound; a 5 MB file can produce many
   thousands of issues. The build returns at most 1000.
8. **HEAD and OPTIONS.** D67 decides unknown methods (405); HEAD and OPTIONS are known HTTP methods that the contract
   does not declare. The build gives 405 with `Allow` for both.
9. **Accept ranges.** D63 lists `application/json`, `application/problem+json`, `*/*`; `application/*` is not listed
   and gets 406 in this build.
10. **Status sort order.** `sort=status` sorts the stored values alphabetically (ACTIVE before DEPRECATED); no
    decision fixes the order of the two values.
11. **Key refetch window after a failed fetch.** D88 says an unknown key id inside the 10 s window gets 401; D44 says
    503 when the keys cannot be fetched. The build gives 503 inside the window when the last fetch failed, 401 when
    it succeeded but lacked the key id.
12. **Date-time offsets beyond 18 hours** (for example `-18:04`) match the filter pattern but Java rejects them; the
    build answers 400 (D20 "not a valid date").
13. **Secrets for the raw start command.** The compose file needs the store and Keycloak admin passwords; `stack.sh`
    generates them into `work/.env` (git-ignored) on the first run, and the raw `docker compose ... up` uses that file.
    A copy of `work/` without `.env` must run `./stack.sh up` once.

## Problems and how they were solved

- Jena's `ParameterizedSparqlString.setValues` writes `("a") ("b")` rows, which need `VALUES (?x) {...}` with
  parentheses around the variable; found by the integration tests (500 from the store), fixed in all queries.
- Jena vocabulary constants (`OWL`, `RDF`, `SKOS`) were null when the validator class loaded before Jena's
  initialisation in a plain unit test; the validator now builds its nodes from IRIs.
- The combined `jena-fuseki-server` jar opens its admin endpoints (`/$/server`, `/$/datasets`) without
  authentication. The image runs the plain Fuseki Main command instead (no UI, no admin; only `/$/ping`).
- The management port shared the API's security chain, so health probes and metrics got 401 and the store-down
  backend never became healthy. A separate security chain now covers the management port only.
- The first backup tool image (`curlimages/curl:8.19.0`) had 46 HIGH and 1 CRITICAL findings; replaced by an own
  Alpine-based tools image with 0.
- A Vitest `beforeEach(() => mock.mockClear())` returned the mock, which Vitest ran as a teardown and so called the
  failing mock; fixed by a block body.
- The JDK test HTTP server is single-threaded, which hid retries in the timeout test; the test server now uses a
  thread pool.
- `SpringApplicationBuilder.properties()` sets defaults that `application.yml` overrides; the second test instance
  now gets command-line arguments.

Fix iterations on the running stack: 4 (management port security chain, proxy 405 handling, tools image, log
fields and Prettier TypeScript configuration in one rebuild).

## What was checked by hand on the stack

- E7: a token of client `vocabulary-tests-noaud` and a token of realm `simpl-other` both get 401.
- Store-down backend on 8199: list and create give 503 with Retry-After 5 and the decided detail; health 503 DOWN.
- `/actuator/prometheus` on the API port: 404 with a token; metrics readable with `./stack.sh metrics`.
- Logs: JSON lines with traceId, correlationId, operationId, vocabularyName and version; no email address.
- `scripts/ui-check.py`: login, create, details, new version, deprecate, viewer restrictions, axe on four pages.

## What does not work or is not done

- D70 empty part name (above). Q1 throughput not shown at the full design load (QUALITY.md).
- CODE-39 `@throws` documentation is incomplete; no SAST tool and no pipeline (DEVIATIONS.md).
- The Keycloak image keeps 6 HIGH findings without a fix in this build (SECURITY-SCAN.md).
