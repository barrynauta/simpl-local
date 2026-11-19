# Build log, run 7 (19 Nov 2026)

## What was built

Backend (Java 21, Spring Boot 4.1.2, Maven), Fuseki 6.2.0 store image, Keycloak 26.8.2 realm, Vue 3 UI behind nginx, compose stack `vm-factory-run7`, `stack.sh`, `smoke.sh`, backup and restore scripts, load check, Playwright UI check.

## Order of work

1. Read the decisions, glossary, OpenAPI, stories, build profile, reuse catalogue and the step-50 rules of `rules.json`.
2. Backend: route table and filters for the decided order of checks, security, request parsing, validation, store, services, controllers.
3. Stack: Fuseki image, Keycloak realm, compose; manual API run with curl.
4. UI, then a Playwright check with axe.
5. Own tests, coverage, formatting, SBOM.
6. Smoke, store-down, Schemathesis fuzzing, backup and restore round trip, load check, Trivy.

## Problems and how they were solved (fix iterations: 10)

1. Fuseki 6 does not create the parent directory of a TDB2 location: the entrypoint now creates `/fuseki/databases/vocabulary` first.
2. A 5 MB upload took 23 s, 15 s of it inside Fuseki: the SPARQL update parser is very slow on one literal of several megabytes. Content is now stored as base64 chunks of 64 KB (0.6 s in Fuseki, 1.5 s end to end).
3. Jena's Turtle parser accepts a file whose last triple has no final dot. Strict mode (`RDFParser.strict(true)`) rejects it, as RDF 1.1 Turtle requires; ill-formed literals stay warnings.
4. Binary uploads produced parser messages with control characters; details are now limited to printable ASCII.
5. nginx-unprivileged could not write its generated config into a root-owned tmpfs on a read-only container: tmpfs mounted with uid 101.
6. Security headers were missing on the SPA routes because nginx does not inherit `add_header` into a location that sets its own: the headers moved into an include used in every location.
7. Test configuration did not replace the realm key bean (bean overriding order): the test bean is now `@Primary`.
8. Tomcat (and Commons FileUpload by default) drop a multipart part whose name is empty, which D70 requires to report as the field `''`: multipart is now parsed with Commons FileUpload 2 and a small subclass keeps unnamed form-data parts. Container multipart parsing is switched off.
9. FileUpload 2.0.0-M5 API differences (`setMaxSize`, `setMaxFileCount`, the count exception extends the size exception): adapted.
10. Fuzzing found 413 for a multipart body whose part headers never end (FileUpload throws a size exception for the header limit): only the total-size limit now gives 413, other size exceptions give 400. Test added.

## Fuzzing (Schemathesis 4.33.4, as step 70: max 30 examples, seed 1, 300 s, all checks)

First run: 48 068 requests, no 5xx, no response schema, status-code or content-type conformance failures; undeclared methods gave 405. 12 findings of two kinds:

- ST202 "schema-valid request rejected" (10): pageSize above 100 (D22), invalid calendar dates such as `0000-00-00` (D20), unknown JSON properties (D52), external search with an empty array or duplicate entries (D38), POST with content that is not valid Turtle (D9, D79). All decided contract changes, listed in D79.
- ST201 "schema-violating request accepted" (2): `name=000` on the list (D19 relaxes the pattern), and the 413 for broken part headers (fixed, item 10 above).

Second run after the fix: see the end of this log.

## Spec gaps the decisions leave open (new findings)

1. **405 before or after 401 for an undeclared method on a known path** (D67 covers unknown methods only, D77 covers fixed segments). Chosen: 405 first, for every client.
2. **Issue name for a bad entry inside `namespaceUris` or `aliasUris` of an external search** (D68 indexes internal search items only; D73 says "the field"). Chosen: the array name without an index.
3. **Turtle parser profile.** "Must parse as Turtle (RDF 1.1)" does not say whether lenient parsers are acceptable; Jena's default accepts a missing final dot. Chosen: strict mode.
4. **Form of the literal in S1 locations** ("the literal as written"): lexical form only, without quotes, language tag or datatype, as in the OpenAPI example.
5. **S4 language tag case**: compared case-insensitively (`en` and `EN` are one language); the location shows the tag of the first label.
6. **Stored form of description and changelog**: limits apply after trimming (D37) but storage is not decided. Chosen: stored trimmed, so D46's "trimmed description equals its description" compares like with like.
7. **Size limit of JSON bodies** (PATCH, searches, content by namespace) is not decided. Chosen: 1 MB, 413 above it.
8. **Base IRI for external vocabulary content** (D57 covers internal versions): `<contentBase>/external-vocabularies/<Name>`.
9. **Content-URL shapes not covered by D69**, for example `<contentBase>/content/<Name>/other#`: treated as external.
10. **Integers beyond 64 bits** in `page` and in a version path: schema-valid; a version that large gives 404, a page that large gives an empty page.
11. **HEAD and OPTIONS** are not declared: they give 405 (no CORS preflight is needed because the UI is same-origin).
12. **External vocabulary without description or source URI**: the properties are left out of the response.
13. **Charset of multipart text fields**: always decoded as UTF-8.
14. **D46 byte equality**: implemented as equal SHA-256 hashes (DEVIATIONS.md).

## What does not work or was not done

- UI error states that need a stubbed API (500, 503, 404 on status change) are implemented with the decided texts but are not in my own UI check.
- No CI pipeline, no SAST run, no alerting (DEVIATIONS.md).
- Keycloak image: 1 CRITICAL, 6 HIGH without a fix in the newest build (SECURITY-SCAN.md).
- Design load of 1000 vocabularies with 20 versions each was not loaded into the store; the load check used one vocabulary with about 60 versions.

## Second fuzz run (after fix 10)

53 209 requests, no 5xx, no response conformance failure. 11 findings, all decided contract changes: 10 ST202 (D20, D22, D38, D52, D79 as above) and 1 ST201 (`name` filter relaxed by D19). The stateful 413 finding is gone.

## Final clean-state run

`docker compose -p vm-factory-run7 --profile storedown down -v`, then `./stack.sh up` from a clean state, then `smoke.sh`, `ui/e2e/ui_check.py` and `ops/backup-restore-test.sh`. Results: start from clean state in 28 s, all services healthy; smoke 13 of 13 PASS; UI check 0 failures (axe clean on 4 pages); backup and restore round trip PASS. The stack is left running (store-down backend not running; start it with `./stack.sh store-down-up`).
