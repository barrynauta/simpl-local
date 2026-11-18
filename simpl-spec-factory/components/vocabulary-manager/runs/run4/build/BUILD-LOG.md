# Build log

Run 4, 17 Nov 2026. Code writer, working only from `build/input/`.

## What was built

- Backend: Java 21, Spring Boot 3.5.16, Apache Jena 5.6.0 (parser and SPARQL), Nimbus JOSE for tokens. All 13
  OpenAPI paths (16 operations) plus the decided changes.
- Store: Fuseki 5.6.0 (TDB2) built from the Maven Central jar with a pinned checksum.
- Identity: Keycloak 26.4 with the realm of E1 and the public test client `vocabulary-tests`.
- UI: Vue 3.5, Vite 6, TypeScript, keycloak-js, n3 (browser syntax check), served by nginx which proxies `/v1`.
- Own tests: 66 backend tests (unit and MockMvc integration with an in-memory Jena dataset and a local JWKS server),
  7 Vitest tests, `smoke.sh` (18 checks), `ui/e2e/ui_check.py` (Playwright, run by hand).

## Where the specs and decisions still left me guessing (new gaps)

1. **Order of 409 and 400 on POST /vocabularies.** A duplicate name with invalid content: no decision. I check the
   name first (409), then validate (400).
2. **Order of 404 and field checks.** On POST .../versions and the PATCH operations, path, body and field checks
   (400, 413, 415) run before the 404 check. D46 lists them together without an order.
3. **Unknown form fields and JSON properties.** Only `version` is decided (D7). I reject every unknown multipart field
   and JSON property with 400 (OWA-API3), also on PATCH and POST /external-vocabularies/content, whose schemas do not
   say `additionalProperties: false`.
4. **Query parameters on POST, PATCH and DELETE.** D21 speaks of GET and D38 of the searches. I return 400 for any
   query parameter on every operation, and also for a parameter given twice.
5. **S4 and labels without a language tag.** Two `skos:prefLabel` literals without a tag on one subject count as the
   same language and give S4.
6. **S7 text for a reference without version.** D12 gives only "... version <n> does not exist.". For a missing name
   referenced without version I return "Referenced internal vocabulary '<Name>' does not exist.".
7. **Deprecated reference to the latest ACTIVE version of a DEPRECATED vocabulary.** D11 read literally gives
   "... version 1 is deprecated. Use version 1, the latest ACTIVE version." I kept the literal text.
8. **Case of names in paths.** Names are unique ignoring case (D37), but lookups by path are exact: `/vocabularies/HEALTH`
   for a stored `Health` gives 404.
9. **Texts not given by the decisions:** version not found ("Version <n> of vocabulary '<Name>' was not found.",
   issue `in: path`, `name: version`), external namespace conflict, field messages, 405 and unknown path problems.
10. **External vocabularies:** name uniqueness ignoring case (as internal), an alias equal to the own namespace is
    dropped, duplicates are removed, `aliasUris` may come as repeated form fields or one JSON array (multipart encoding
    of an array is not specified), `description` up to 1000 characters. External names are not checked against
    internal names.
11. **Base IRI for relative IRIs** in uploads is not specified. I use the content URL of the new version.
12. **Dates** are stored and returned to the second (`2026-11-17T10:00:00Z`) so that sort order, ties and filters agree
    with what clients see.
13. **Name sort** is by code point (Java `String.compareTo`), so `AZ` sorts before `Ab`.
14. **`prev` link beyond the last page** points to the last page; with no results it is left out.
15. **UI details not fixed by the decisions:** menu item texts ("View vocabulary details", "History", "Download
    vocabulary", "Update", "Change status to Deprecated/Active"); the status change runs at once without a
    confirmation step; dates shown as the ISO text of the API; not-found message "Vocabulary not found. The vocabulary
    could not be found."; 5xx message "The operation failed. Please retry or contact support."; submit buttons are
    labelled "Submit"; the file field is labelled "File upload".
16. **N3 reports no column** for syntax errors in the browser. The UI shows the column of the offending token in its
    line, or the first non-blank column.
17. **E2 and E3 against the build profile:** ports, issuer URL and the "tests reset the dataset" note differ (see
    DEVIATIONS.md).

## Problems and how they were solved

| Problem | Fix |
|---|---|
| `NoSuchMethodError` in commons-lang3 at runtime: Spring Boot manages an older version than Jena 5.6 needs. | Pinned `commons-lang3` 3.19.0, `commons-codec` 1.19.0, `commons-compress` 1.28.0 in the POM. |
| MockMvc multipart `.param()` fields do not appear in `request.getParts()`. | Tests send every field as a `MockPart`, like a real client. |
| A Turtle file without foreign prefixes gave no warning in my first test. | The test was wrong: D12 skips the vocabulary's own namespace. |
| Retry test counted 2 calls instead of 3: the JDK test HTTP server handled requests one at a time. | Gave the test server a thread pool. |
| Fuseki container stopped: `--loc` directory missing on a fresh volume. | The image creates `/fuseki/databases/vocabulary`. |
| A 4.9 MB upload took 5.8 s, mostly in `ParameterizedSparqlString` scanning the Base64 literal for every parameter. | The content goes in after the command is built, with a random marker; now 3.8 s. |
| First draft logged the user's e-mail on access denials (CODE-32) and logged and rethrew on key fetch failure (CODE-42). | Logs the token subject instead; removed the log before rethrow. |
| `smoke.sh` JSON helper broke on nested quotes. | Passes file and selector as arguments to Python. |

Fix iterations: 6 (backend tests 1, compose start 1, smoke script 1, UI type check 1, upload speed 1, logging rules 1).

## Checks at hand-over

- `docker compose -p vm-factory-run4 down -v` then `docker compose -p vm-factory-run4 up -d --build --wait`: exit 0,
  four containers healthy.
- `./smoke.sh`: 18 of 18 checks pass.
- `mvn verify`: 66 tests, 0 failures; JaCoCo line coverage 94.4 %, branch 78.9 % (gate 80 % lines); Spotless clean.
- `npm test`, `npm run build`, `npm run format:check`: green.
- `ui/e2e/ui_check.py` against the stack: passes (create with syntax check, validation report, warnings panel, new
  version, deprecated version warnings, download, status change, stubbed 404 and 500, viewer restrictions).
- By hand on the stack: 6 MB and 30 MB uploads give 413; JSON to POST /vocabularies gives 415; five parallel new
  versions give 201 with versions 2 to 6 and only 6 ACTIVE; a backend started with an unreachable store gives 503
  with `Retry-After: 5` and health 503.

## What does not work or is not done

- No CI pipeline, vulnerability scan, Helm chart or TLS (see DEVIATIONS.md).
- Only one backend instance may run (D43 lock in process).
- The UI sort headers and the UI filters other than name are not covered by an automated check.
- The UI look follows no design system; the Figma mock-up was not available.
