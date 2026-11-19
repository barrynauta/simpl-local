# Build log

## Session 1 (18 Nov 2026, interrupted)

An earlier attempt built the backend, the UI, the Keycloak realm, the Fuseki image and `compose.yaml`, ran the tests (58 backend tests green, JaCoCo 86 % lines) and left the stack running. It stopped before the scripts and documents were written. No log of that session exists; what follows for it is reconstructed from the files it left.

## Session 2 (19 Nov 2026)

Kept everything from session 1 after checking it: the stack was healthy, the tests green, and probes of the decided edge cases (order of checks, 405, unknown and repeated parameters, path patterns, same-status 409, identical version 409, 413, 415, page beyond the last, store-down 503 with `Retry-After`) gave the decided answers.

Added: `stack.sh`, `smoke.sh`, `ops/backup.sh`, `ops/restore.sh`, `docs/`, `TRACEABILITY.md`, `REUSE.md`, `QUALITY.md`, `DEVIATIONS.md`, `SECURITY-SCAN.md`, this log, `sbom/`, `reports/`.

### Problems and fixes (fix iterations: 5)

1. `smoke.sh` first run: a Python one-liner with escaped quotes inside an f-string failed. Rewritten without the f-string.
2. Schemathesis found that the HTTP method QUERY (and any non-standard method) gave 400, not 405. Cause: Spring Security's `StrictHttpFirewall` rejects unknown methods before the 405 filter runs, and the `RequestRejectedHandler` answered 400 for every rejection. Fix: the handler now answers 405 with the `Allow` header of the route (404 outside the API) when the method is not a standard one. Test added in `ApiIntegrationTest.undeclaredMethodsAndPaths`.
3. Trivy: the UI image had 12 HIGH and 1 CRITICAL in Alpine packages of the newest nginx-unprivileged digest; fixed with `apk upgrade` in the final stage. Keycloak 26.7.5 had 14 HIGH and 2 CRITICAL; moved to 26.8.2 (6 HIGH, 1 CRITICAL left, see SECURITY-SCAN.md).
4. Metrics on the management port 8081 answered 401, because the API security chain also covered the management port. Added a first security chain for the management port that opens only `GET` health and prometheus and denies the rest. Port 8081 is not published.
5. Schemathesis (second run) showed a 413 'file is too large' for a request whose file was tiny but whose description was about 70 KB. Cause: Tomcat's `max-http-form-post-size` (64 KB) also caps the non-file parts of a multipart request, and the 413 handler gave the upload text. D37 asks for 400 on a description over 1000 characters. Fix: the limit now equals the 12 MB request cap, so the field rules decide. New test `TomcatLimitsIntegrationTest` runs the real Tomcat connector (MockMvc skips it) with a stubbed JWT decoder and checks the 400, the 413 for a file over 5 MB, and 405 for QUERY.

### Where the specs and decisions still left me guessing

- Criterion ids: most stories do not number their criteria. I numbered scenarios in story order (the convention the decisions use, such as SIMPL-18173/AC6).
- D44 does not say which status the API port gives for `/actuator/...`. It answers 401 without a token (security first), as for any other path.
- D52 and the firewall: a request with an encoded slash or a control character in the path is rejected by the firewall before any route matching; it gets 400 without issues. No decision covers it; 400 seemed the closest fit.
- Non-standard methods are rejected by the firewall before authentication, so `QUERY` without a token gives 405, while `PUT` without a token gives 401 (D17 puts 401 first). The decisions do not cover non-standard methods; the finish line only asks for 405.
- NFR-06 asks for availability, RTO and RPO, which no decision gives. I stated 99.5 % office hours, RTO 30 minutes, RPO 24 hours for this single-instance build. These are my numbers, not owner numbers.
- The Schemathesis findings ST201 and ST202 that remain all follow decided changes (D19, D20, D22, D30, D52, and the D37 pattern). The fuzz harness of step 70 needs these decided changes to judge them, otherwise they look like contract violations.
- D56 limits aliases to 20; the OpenAPI file has no `maxItems`. Implemented in the controller.

### What does not work or was not done

- The UI views have no component or browser tests of their own; only the helpers (texts, errors, format, Turtle check, notifications) are unit-tested. The views were checked by serving the build and calling the proxy, not by a scripted browser run. The step 60 suite covers the UI.
- No CI pipeline (see DEVIATIONS.md).
- 7 HIGH or CRITICAL findings remain in the Keycloak product image (see SECURITY-SCAN.md).

### Final state

Stack running (`vm-factory-run6`, ports 8196, 4433, 8193); `smoke.sh` passes; `mvn verify` green (61 tests, JaCoCo 87 % lines, 67 % branches); UI tests green (14 tests, 96 % lines); Trivy 0 HIGH and 0 CRITICAL in own images and dependencies.
