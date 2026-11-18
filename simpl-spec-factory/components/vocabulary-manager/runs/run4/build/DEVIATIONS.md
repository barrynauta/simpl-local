# Deviations

Rules from `input/rules.json` that this build does not follow, or follows only in part, and decisions not
implemented as written. Rules that only apply to the OpenAPI file (which the writers may not edit, D39), to events
(none in this build, D45) or to Angular are listed once at the end as not applicable.

## Decisions

All decisions D1 to D46 and E1 to E3 are implemented. Points where the build had to read them a certain way:

| Decision | What differs | Why |
|---|---|---|
| E2 | UI is on host port 4433 and Keycloak on 8193 (not 4200 and 8180); the API is on host port 8196, the backend listens on 8080 inside its container. The token issuer is `http://localhost:8193/realms/simpl`. | The build profile fixes the host ports and wins for this run. |
| E3 | No reset of the dataset is offered to tests. | The build profile says tests may not reset the store and must use unique names. |
| D43 | The lock per vocabulary lives in the backend process, so only one backend instance may run. | As the decision allows ("This build runs one backend instance"). |

## Rules (MUST)

| Rule | Status | Reason |
|---|---|---|
| API-01, DATA-26 to DATA-29, MACH-03 | Not met | No CDM was in the input and the OpenAPI file may not be edited (D39). The decided changes (createdBy, latestVersionHref, 401/403/503, limits) are documented in `docs/api-notes.md` but not in a contract file. |
| OWT-A03, OPS-06, OPS-33, OPS-35, OPS-37 | Partly | Dependencies are pinned and listed in CycloneDX SBOMs (`sbom/`); Spotless and Prettier checks exist. There is no CI pipeline, no vulnerability scan and no merge gate in this build. |
| OWT-A04, ARCH-17 | Not met | Local stack: no TLS between containers or to the host, Fuseki data at rest is not encrypted. Secrets are not in code or logs. |
| OWT-A07 | Partly | Tokens are validated on every call (signature, issuer, audience, expiry). The service does not limit repeated failed calls; Keycloak brute force protection is on for logins. |
| OWT-A09, OWASP-10 | Partly | Login failures (in Keycloak), invalid tokens, access denials, input validation failures and administrative changes are logged (structured, without tokens or e-mail addresses). No alerting is set up. |
| OWT-A02, OWASP-06 | Partly | Secure defaults in the service (no debug endpoints, no CORS, sanitised errors, security headers). Keycloak runs in `start-dev` mode, and the test users have the known password `Password1!` required by the build profile; the Keycloak admin password is in `work/.env`. |
| OWT-A08 | Partly | The Fuseki jar is checked against a pinned SHA-512. Base images are pulled by tag, not by digest. |
| MACH-08 | Partly | One endpoint `GET /v1/health` (UP, DEGRADED, DOWN) serves as readiness and Docker health check; there is no separate liveness endpoint. |
| ARCH-07, ARCH-19 | Not met | One backend (D43) and one Fuseki instance; no replication. |
| MSV-09 | Partly | The correlation id is taken from or added to every request, logged and returned. Calls to Fuseki and Keycloak do not forward it. |
| CODE-05 | Partly | Prettier formats the Vue code, but its configuration is `.prettierrc.json`, not a TypeScript configuration file. |
| CODE-39 | Partly | Service, repository-facing and helper methods document `@throws`; the controller methods do not list the unchecked `VocabularyManagerException`, which the exception handler maps. |
| CODE-43 (SHOULD, noted here) | Partly | One system-wide exception class with error code enums. Two private exceptions (`HttpSparqlClient.StoreCallFailure`, `RealmKeys.KeyFetchFailure`) are used inside one class for retry control and never leave it. |
| ARCH-20 | Partly | `createdBy` returns the creator's e-mail address to every reader (D27 decides this). Logs use the token subject instead. |
| UX-01 | Not met | The Figma design was not reachable; the UI uses plain components and the texts of the decisions. |
| BLG-int-conlang, BLG-int-negot | Not applicable as built | Responses are in English only; no Content-Language header and no Accept-Language handling. |

## Rules (SHOULD) not followed

| Rule | Reason |
|---|---|
| API-25, SEC-08 to SEC-10, SEC-15 | No TLS or mTLS in the local stack. |
| API-46 | Belgif problem types (`urn:problem-type:belgif:...`) are used where the OpenAPI names them; simpl types where it names those. |
| CODE-21, CODE-52, CODE-50, SEC-11 | Logging is Spring Boot Logback with ECS JSON lines on stdout; the Log4J wrapper and the central log aggregation are not available here. |
| CODE-33 | No shared exception-handling framework was available; the component has its own (one exception class, error code enums). |
| CODE-45, SEC-12, SEC-13 | There is no gateway in this build; the service enforces the access rules itself (D17). |
| CODE-46, MSV-07, ARCH-34 | No circuit breaker. Timeouts and bounded retries (D44) limit the impact; a breaker was left out to keep the decided behaviour exact. |
| MSV-10 | Structured logs, but no metrics endpoint. |
| MACH-06, ARCH-15, ARCH-16 | Stateless between requests except the in-process lock of D43, which prevents horizontal scaling. |
| OPS-10, OPS-11, OPS-13, OPS-28 to OPS-30, OPS-15 (charts) | Delivered as Docker Compose only: no Helm chart, network policies, service accounts or Kubernetes secrets. Artefacts are versioned 1.0.0. |
| CODE-07 | A few controller methods (registration and search request parsing) are about 40 lines. |

## Not applicable in this build

Event and Kafka rules (API-62 to API-80, MSV-04, BLG-async-*, ARCH-27, ARCH-28), saga rules (MSV-08), Angular rules
(CODE-03, CODE-04), Tier 2 and agent-to-agent rules (SEC-06, SEC-13, SEC-16 to SEC-22), and the rules on the
content of the OpenAPI file (BLG-openapi-*, BLG-oas-*, API-34, API-59, API-61 and the other `api-spec` only rules),
which the writers may not edit.
