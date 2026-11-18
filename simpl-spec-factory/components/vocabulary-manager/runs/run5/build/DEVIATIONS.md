# Deviations

Rules of `input/rules.json` this build does not follow, fully or partly, and decisions not implemented as written. Rules
that do not apply to this component (for example Angular rules or event rules, while D45 says there are no events) are
listed at the end, so the list is complete.

## Decisions

All decisions D1 to D61 and E1 to E4 are implemented. Points where I had to interpret a decision are in BUILD-LOG.md
(gaps), not here.

## Rules not followed or partly followed

| Rule | Level | What the build does | Why |
|---|---|---|---|
| API-46 | SHOULD | 400, 404, 500 and 503 use `urn:problem-type:belgif:…` types | Decision D48 keeps the belgif types of the OpenAPI and D44; recorded deviation. The simpl types are used for 401, 403, 405, 406, 409, 413 and 415. |
| API-01, DATA-26 to DATA-29 | MUST | No CDM file delivered and the OpenAPI file not updated | No CDM is in the input; D39 says the writers do not edit the OpenAPI file. The differences are documented in `docs/API.md`. |
| API-03, API-21 | SHOULD | The OpenAPI file is the input file, unchanged | D39. |
| API-25, OWT-A04, OWASP-03, OWA-API8 | SHOULD / MUST | Plain HTTP on localhost; Fuseki data on a Docker volume without encryption at rest | Local build profile with fixed http URLs. In production TLS ends at the Tier 1 gateway and the platform encrypts volumes. |
| OWASP-06 (secure defaults), SEC-02 | MUST | Keycloak runs `start-dev` (H2 file database, HTTP). The bootstrap admin password is in `.env`. | Local test identity provider of E1. Production uses the platform Keycloak. The admin password comes from the environment, not from code or image. |
| Fuseki access control (OWASP-02 for the store, MSV-02) | MUST | Fuseki has no authentication | It has no host port and is only reachable on the compose network by the backend. |
| ARCH-07, MACH-06 | MUST / SHOULD | One backend instance; the per-vocabulary lock is in memory | D43 allows this for the build. Several replicas would need a lock in the store (for example a conditional update on the latest version number). |
| MSV-07 | SHOULD | No circuit breaker or bulkhead | D44 defines timeouts, retries and 503; a circuit breaker was not asked for. |
| MSV-10 | SHOULD | Structured logs and health, no metrics endpoint | Not needed for the finish line; adding Micrometer and Actuator would add an exposed endpoint. |
| MSV-09 | MUST | Correlation id on every log line, returned in `X-Correlation-Id` and sent to Fuseki | Partly: the UI does not send one, so the backend creates it. Keycloak calls (JWKS) do not carry it. |
| OPS-06, OPS-33, OPS-35, OPS-37, OWASP-12, CODE-13, CODE-54 | MUST / SHOULD | No CI pipeline. Spotless check runs in `mvn verify`, Prettier check in `npm run format:check`, Trivy by hand. | The build is a local stack; there is no repository pipeline in scope. |
| CODE-21 | SHOULD | Logback through Spring Boot with ECS JSON, not the Log4J wrapper | The wrapper library is not available to this build. |
| CODE-33 | SHOULD | Own exception types (`ApiException`, `StoreException`, …) | The shared exception framework is not available to this build. |
| CODE-39 | MUST | `@throws` is documented on the main public methods, not on every public method that can throw an unchecked `ApiException` | Partly followed; time. |
| CODE-04, CODE-03 | MUST | n/a for Vue; ESLint is not configured for the Vue code, only Prettier (CODE-05) | No Vue lint rule set is given in the rules. |
| OWT-A07 (repeated failures limited) | MUST | Keycloak brute force protection is on; the API has no call-rate limit | D46: the Tier 1 gateway applies the rate limit. |
| OWT-A09, OWASP-10 (alerting) | MUST | Access denials, validation failures and administrative changes are logged; no alerting | No monitoring stack in the build profile. |
| OWASP-07 | MUST | Six HIGH findings in the third-party Keycloak image stay open; nothing open in the images and dependencies this project builds | No fixed Keycloak release; reasons per finding in SECURITY-SCAN.md. |
| BLG-int-conlang, BLG-int-negot | MUST | No `Content-Language` header, no `Accept-Language` handling | All texts are English only. |
| UX-01, UX-02 | MUST / SHOULD | Plain own styling | The Figma design and the UX guidelines are not in the input (specs README). |
| SEC-01, OWASP-05 | MUST | A short threat model in `docs/ARCHITECTURE.md`, no separate threat-modelling document | Time. |
| API-50, API-52, API-54 | SHOULD | Resources are identified by `name` and `version`, not `id` or `code` | The OpenAPI paths and D30 make the name the identity. |
| API-39 | MUST | No `Vary: Accept` | Each resource serves one media type; there is no negotiation between types. |

## Rules that do not apply

API-62 to API-80, BLG-async-*, MSV-04, MSV-08 and the CloudEvents rules: the component publishes and consumes no events
(D45). CODE-03 and CODE-04: no Angular code. OPS rules on Helm, Argo CD and Kubernetes: the build profile is Docker
Compose. BLG-lng-task: there are no long-running tasks.
