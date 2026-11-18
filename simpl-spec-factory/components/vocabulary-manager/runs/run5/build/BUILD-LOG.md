# Build log (run 5, 18 Nov 2026)

## What was built

Backend (Java 21, Spring Boot 4.1.2 since the upgrade below, first built on 3.5.16; Jena 6.2.0, Nimbus JOSE), UI (Vue 3.5, Vite 8, TypeScript 5.9, keycloak-js 26,
N3.js), Fuseki 6.2.0 image, Keycloak 26.8.2 with the realm of E1, one compose file, smoke test, own tests, docs and
reports. All operations of the OpenAPI that the decisions keep are implemented, including the external vocabulary
operations and the version status change (D41).

## Finish line

| Step | Result |
|---|---|
| `docker compose -p vm-factory-run5 up -d --build --wait` from a clean state (`down -v`, `build --no-cache`) | passes; all four containers healthy |
| `./smoke.sh` | 19 checks pass (create, new version, older version deprecated, list, download latest and version 1 byte-equal, search, viewer 403 on create, no token 401, no role 403) |
| `mvn verify` (backend) | 69 tests green, Spotless check green, JaCoCo line coverage 91.5 %, branch 78.2 % (gate 75 % lines; on Boot 3 it was 90.7 % and 77.1 %) |
| `npm test` (UI) | 19 Vitest tests green; Prettier check green |
| `ui/e2e/ui_smoke.py` (own Playwright check, optional) | passes against the running stack |
| Trivy | Boot 3 without overrides: 7 CRITICAL and 6 HIGH in the backend (and the same in `fs`); Boot 3 with overrides: 2 CRITICAL (spring-webmvc); Boot 4.1.2: 0 in the backend, UI, Fuseki and `fs`. 6 HIGH stay in the Keycloak vendor image. See SECURITY-SCAN.md. |
| Stack left running | yes |

## Gaps: where the specs and decisions still left me guessing

New findings, beyond the decisions:

1. **S4 location with two duplicated languages.** One subject with duplicate labels in two languages gives two issues
   with the same detail and the same location `resource '<s>'`. The decisions fix the detail text and say "the
   offending resource" as location; the language is not part of it. I kept the plain location.
2. **Version panel link for a DEPRECATED vocabulary.** D36 (UI) shows "Latest ACTIVE version: n" when an ACTIVE version
   exists; D47 (API warning) recommends no version when the vocabulary is DEPRECATED. I followed D36 in the UI and D47
   in the API, and never link a version to itself.
3. **406.** The OpenAPI lists 406 everywhere but no decision places it in the order of D51. I check `Accept` right
   after authentication, before 415.
4. **Unknown paths without a token.** The service checks the token for every path under `/v1` before routing, so an
   unknown path gives 401 without a token and 404 with one.
5. **Search request issue names.** D38 names `items`, `namespaceUris`, `aliasUris` for limit errors. For an invalid
   item I used `items[i].name` and `items[i].version`; for a body that matches no or several `oneOf` branches I used
   `name: body`. When `namespaceUris` plus `aliasUris` exceed 100, the issue names `aliasUris` when present.
6. **404 of POST /external-vocabularies/content.** D41 fixes the 404 text "External vocabulary with name '<Name>' was not
   found." with an issue `in: path`, but this operation has no path name. I used the OpenAPI example text "External
   vocabulary with namespaceUri '<uri>' was not found." without an issue.
7. **Internal reference shapes.** D12 says "followed by nothing, '#' or '/'". A namespace like
   `<contentBase>/content/Name/versions/abc` is read as a reference to the latest version of `Name` (the `/` after the
   name matches). A namespace `<contentBase>/content/` with no name is treated as external.
8. **Self-reference.** "References to the vocabulary being created or versioned are ignored": I compare names exactly
   (D54).
9. **`Content-Type` of downloads.** Exactly `text/turtle`, no charset parameter; the bytes are returned as uploaded,
   whatever their encoding.
10. **Empty file.** A `vocabularyFile` part with zero bytes passes the field check and fails validation with S6 (400),
    not with a "required" field error.
11. **aliasUris form shape.** D56 says one part per URI. A single part with several URIs separated by commas is read as
    one (invalid) URI.
12. **Keycloak issuer.** With the issuer check of OWT-A07, a token is accepted only when it was issued for
    `http://localhost:<KEYCLOAK_PORT>/realms/simpl`. Keycloak runs with that fixed hostname, so the tests and the UI get
    matching tokens. A client that reaches Keycloak by another host name still gets the same issuer.
13. **Audience.** E1 names `vocabulary-manager-be` as the bearer-only audience but no decision says how tokens get it.
    I added an audience mapper to the clients `vocabulary-manager-ui` and `vocabulary-tests`; tokens without that
    audience get 401.
14. **D44 lock wait.** The lock wait limit is 10 s (the store response timeout); D44 does not give a separate number.
15. **UI filters.** D23 and the story name the filters but not their labels. I used "Name contains", "Vocabulary
    status" and "Created on". The list actions menu holds "View vocabulary details", "History", "Download vocabulary",
    and for administrators "Update" and "Deprecate"/"Activate".
16. **After a successful creation the UI opens the details page** of the vocabulary and shows the notification and the
    warnings panel there; D14 does not say where the user lands.
17. **Status change error texts in the UI.** A 404 shows "Vocabulary not found." plus a sentence that the vocabulary could
    not be found and no change was applied; a 500 shows "The vocabulary status could not be updated." plus the D60
    text and the detail. Neither text is fixed by a decision.

## Problems and how I solved them

1. Fuseki 6 does not create the TDB2 directory: the container exited with "Directory does not exist". The entrypoint
   now creates it.
2. The Spring Boot BOM pins commons-lang3 3.17, but Jena 6.2 needs 3.20 (`BasicThreadFactory.builder()`); every query
   in the embedded Fuseki failed with `NoSuchMethodError`. I set `commons-lang3.version`, `commons-codec.version` and
   `gson.version` to the versions Jena is built against, and `jetty.version` 12.1.11 for the test-only embedded
   Fuseki.
3. Surefire ran only `*Test` classes, so the integration tests did not run and coverage was 40 %. Surefire now also
   includes `*IT`.
4. Trivy found CRITICAL Tomcat and HIGH Jackson findings in Spring Boot 3.5.16's defaults; fixed by `tomcat.version`
   10.1.62 and `jackson-bom.version` 2.21.8.
5. The Spring Boot parent configures the CycloneDX plugin; the SBOM is written to
   `target/classes/META-INF/sbom/application.cdx.json`, copied to `sbom/`.
6. A typo in `smoke.sh` (comparison of a tuple) and a multi-catch compile error (`DateTimeParseException` is a subclass
   of `DateTimeException`) were fixed on the first run.
7. Log lines first carried the creator's e-mail (CODE-32, personal data); they now carry the token subject only.

Fix iterations: 7 (the problems above); the first full stack start after the Fuseki fix worked, and the smoke test
passed from then on.

## What does not work or is not done at the end

- Six HIGH findings in the Keycloak vendor image stay open (no fixed Keycloak release).
- One backend instance only (in-memory locks, D43).
- No CI pipeline; checks run locally.
- The Playwright UI check is my own and optional; it needs `pip install playwright` and a browser, which the stack does
  not provide.
- List filtering and sorting run in the service on all vocabularies read from the store. Fine for hundreds or thousands
  of vocabularies; a very large store would need SPARQL-side paging.

## Upgrade to Spring Boot 4 (18 Nov 2026)

Reason: the build profile changed to Spring Boot 4 (4.1.2 or a newer 4.x patch), because CVE-2026-47884 and
CVE-2026-47890 in spring-webmvc are only fixed in Spring Framework 7.0.9 and later.

### Versions now

Spring Boot 4.1.2 (newest 4.x on Maven Central), Spring Framework 7.0.10, Tomcat 11.0.27, Jackson 3.1.7
(`tools.jackson`, with `jackson-annotations` 2.21), JUnit 6.0.3, Logback 1.5.38. Jena 6.2.0, Nimbus JOSE 10.10, JaCoCo
0.8.16, Spotless 3.11.0 and CycloneDX 2.9.3 are unchanged. UI, Keycloak and Fuseki are unchanged.

### What changed

- `pom.xml`: parent 4.1.2; `spring-boot-starter-web` became `spring-boot-starter-webmvc`; test starter
  `spring-boot-starter-webmvc-test` added. The overrides `tomcat.version`, `jackson-bom.version`, `jetty.version` and
  `commons-lang3.version` are removed, because Boot 4.1.2 manages fixed or matching versions (Tomcat 11.0.27, Jackson 3,
  Jetty 12.1.14, commons-lang3 3.20.0). `commons-codec` 1.22.0 and `gson` 2.14.0 stay to match Jena.
- Jackson 3: imports moved from `com.fasterxml.jackson.databind` and `.core` to `tools.jackson.databind` and
  `tools.jackson.core`; `JsonProcessingException` became `JacksonException` (unchecked, so the `IOException` catch
  blocks around `readTree` were removed); `fieldNames()` became `propertyNames()`, `fields()` became `forEachEntry`,
  `isTextual()`/`asText()` became `isString()`/`asString()`. The annotations (`@JsonInclude`, `@JsonProperty`) keep their
  `com.fasterxml.jackson.annotation` package.
- `ErrorController` moved to `org.springframework.boot.webmvc.error`.
- `application.yml`: `spring.jackson.serialization.write-dates-as-timestamps` removed. Jackson 3 has no such
  `SerializationFeature` and the application failed to start with it; the service serialises no `java.time` values (all
  times are ISO strings), so the setting was not needed.
- No Spring Security was in use, so the security changes of Boot 4 did not apply; the token filter is unchanged.

### Problems and fix iterations

1. Compile errors after the parent change (stale classes hid them until `mvn clean`): Jackson 3 packages and method
   names, `ErrorController` package. One round of mechanical edits.
2. `IOException is never thrown` in two catch blocks (Jackson 3 exceptions are unchecked). Fixed.
3. Application context failed to start in the integration tests on `write-dates-as-timestamps`. Property removed.

Fix iterations: 3. After that, all 69 tests passed without changing a test assertion.

### Behaviour check

No intended change in behaviour. Checked again from a clean state: `smoke.sh` (19 checks), my API probe of every status
code, problem type, detail text and validation report of the earlier build (same results), the Playwright UI check, and
`mvn verify`. Trivy now reports 0 HIGH and 0 CRITICAL for the backend.
