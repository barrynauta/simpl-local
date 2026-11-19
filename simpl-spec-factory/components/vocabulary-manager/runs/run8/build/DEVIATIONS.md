# Deviations

This file lists every rule of `input/rules.json` that the build does not follow, or follows only in part, and every decision that is not implemented exactly as written. Each entry was checked against the files in `work/` (backend code, `compose.yaml`, Dockerfiles, `ui/`, `ops/`, `docs/`, `keycloak/`), not assumed.

"Partly" means the rule is met for most of its text and one part is missing; the missing part is named.

## 1. Rules not followed or followed only in part

### API contract and wire format

| Rule | What the rule asks | What the build does | Why |
|---|---|---|---|
| API-46 | Every problem `type` is a URN `urn:problem-type:simpl:...` | 400, 404, 500 and 503 use `urn:problem-type:belgif:...` (badRequest, resourceNotFound, internalServerError, serviceUnavailable). 401, 403, 405, 406, 409, 413 and 415 use `urn:problem-type:simpl:...` | Decided in D48: the published OpenAPI and D44 fix the belgif types for these codes. |
| API-24, API-26 | URI `https://host/pathPrefix/apiName/v{major}/resources`, apiName in lowerCamelCase | The API base is `http://localhost:8196/v1`: no path prefix and no apiName segment | E2 fixes the base path `/v1`, and the published OpenAPI has no apiName. Behind the Tier 1 gateway a prefix would be added by routing. |
| API-50, API-52 | The resource identifier is named `id` or `code`; identifier path parameters are `id` or `code` | Vocabularies and external vocabularies are identified by `name` (`vocabularyName`, `name` in paths); versions by `version` | D30 makes the name the identity; the published contract uses these names. |
| BLG-cst-header | Custom headers carry an organisation prefix | `X-Correlation-Id` has no organisation prefix | D86 fixes this header name. |
| BLG-trc-header | Support `BelGov-Trace-Id` on requests and responses | Not supported. The service reads and returns W3C `traceparent` and `X-Correlation-Id` | D86 fixes W3C trace context and X-Correlation-Id; the API is not exposed to Belgian government third parties. |
| BLG-doc-refdata | The OpenAPI and other API documentation are served under a `doc` resource of the API | No `doc` resource. The decided contract is in `docs/openapi-decided.yaml` | No operation for it in the contract; D84 forbids handlers without a contract operation and role rule. |
| BLG-prf-cach | Consider HTTP caching for resources fetched repeatedly | Every response has `Cache-Control: no-cache, no-store` (Spring Security default); no `ETag` or `Last-Modified`, no conditional GET | Version content never changes and could be cached, but status must be read fresh (D45). Caching was left out to keep one behaviour for all reads; recorded as a gap. |

### Security and transport

| Rule | What the rule asks | What the build does | Why |
|---|---|---|---|
| SEC-12, CODE-45 | Tier 1 authorisation at the Spring Cloud Gateway; clients reach the API only through the gateway | No gateway in the stack. The service validates the token (signature, issuer, audience, expiry) and the realm roles itself, default deny per operation (`SecurityConfig`, `OperationCatalogue`, `HandlerRuleVerifier`) | Decided in D17 and E6: the build checks tokens itself so the rules can be tested without a gateway. In production the gateway applies the same rules first. |
| SEC-07 | Tier 1 authentication with OpenID Connect through the Tier 1 Keycloak, with the Simpl-Open custom authenticator plugin | Plain Keycloak 26.8.2 with an imported realm (`keycloak/realm-simpl.json`), no custom authenticator plugin | The plugin is not available to the factory; E1 fixes a plain realm with test users. |
| API-25, OWT-A04, OWASP-03, OWA-API8, ARCH-17 (partly) | TLS for all traffic; sensitive data at rest encrypted; the API states TLS only | No TLS anywhere in the local stack: backend, UI, Keycloak (`start-dev`, `sslRequired: none`) and Fuseki speak plain HTTP. The Fuseki TDB2 volume is not encrypted. Secrets are not in code or logs, and crypto uses Nimbus JOSE | Local evaluation stack on one host with fixed `http://localhost` URLs in the build profile; TLS termination belongs to the platform ingress. The store sits on an internal network with no published port (D83). |
| OPS-11, OPS-12 | Runtime secrets in Kubernetes Secrets or Vault; pipeline secrets in an external store | Secrets (store users, Keycloak admin) are random values that `stack.sh` writes once to `work/.env` (mode 600, git-ignored) and passes as environment variables | E6: environment variables locally, OpenBao in production. No OpenBao instance in the local stack. |
| OWT-A07 (partly), NFR-04 (partly) | Repeated failures are limited; limits include a call rate | Keycloak has brute-force protection on (`bruteForceProtected: true`). The service has no call-rate limit and no limit on repeated invalid tokens | D46: the platform call-rate limit belongs to the Tier 1 gateway, outside this build. All other limits of NFR-04 (file, request, page size, items, text lengths) are enforced with defined errors. |
| OWT-A09 (partly), OWASP-10 (partly) | Security events are logged and can raise an alert | Missing or invalid tokens, denied roles and firewall refusals are logged on the `security` logger, without personal data (D80). No alerting is configured | No monitoring stack in the build; alerts would be rules in the central monitoring. |
| OWASP-05 (partly), SEC-01 (partly) | A threat model before implementation | No separate threat model document. The abuse cases are decided in D46, and the security design (default deny, least privilege per store user, defence in depth, fail closed) is in `docs/architecture.md` | Time; recorded as a gap. |
| OWASP-07, REUSE-08 (partly) | No dependency or image with known vulnerabilities above the threshold; no CRITICAL without a reason | The Keycloak image `quay.io/keycloak/keycloak:26.8.2` has 1 CRITICAL and 6 HIGH findings (OpenJDK, sqlite, guava inside the Keycloak distribution). Own images and dependencies have 0 HIGH and 0 CRITICAL | 26.8.2 is the newest tag (published 19 Nov 2026). The OS findings have no fixed package. Guava has a fix (33.7.2) but sits inside the vendor distribution; patching it would mean rebuilding Keycloak. Details and reasons per finding in `SECURITY-SCAN.md`. |

### Reuse and frameworks

| Rule | What the rule asks | What the build does | Why |
|---|---|---|---|
| UX-01, UX-02 | The UI follows the Simpl UX guidelines (participant-fe-components, Figma design system); adherence is measured with usability testing | The UI is plain Vue 3 + Vue Router + Vite with own CSS (`ui/package.json`: vue, vue-router, keycloak-js, n3). No PrimeVue, no TailwindCSS, no participant-fe-components. No usability testing | E6: the library is not available to the factory. The build profile asks for a plain component library. |
| REUSE-09 (partly) | Frameworks are extended through extension points; no use of framework internals | Multipart bodies are read with `org.apache.tomcat.util.http.fileupload.MultipartStream` and `ParameterParser`, Tomcat's bundled copy of Commons FileUpload, which is an internal API of the container (`web/body/MultipartForm.java`). Spring's multipart support is switched off | Tomcat's part handling silently drops a part with an empty name, so the D70 issue (`in: body, name: ''`) could not be produced; with valid fields the request would even give 201. The bundled parser is reused rather than hand-writing one (REUSE-02). A Tomcat upgrade may move or change this class; the own tests cover it. |

### Coding standards and tests

| Rule | What the rule asks | What the build does | Why |
|---|---|---|---|
| CODE-01, CODE-02, CODE-13 | Java formatted with Spotless (Palantir Java format), checked in the Maven build | `pom.xml` declares `spotless.version` and `palantir-java-format.version` properties, but the Spotless plugin is not configured, so formatting is neither applied nor checked | Left unfinished by the interrupted earlier attempt and not added in this run. The code follows a Palantir-like layout by hand, unverified. |
| CODE-05 (partly) | Vue code formatted with Prettier using a TypeScript configuration | Prettier 3.10.1 with `format` and `format:check` scripts, configured in `ui/.prettierrc.json` (JSON, not a TypeScript config file) | Prettier is used; only the config file format differs. |
| CODE-10 (partly) | Code is unit-testable and reaches high unit-test coverage | Backend: 46 own tests, 94.5 % line and 83.7 % branch coverage (JaCoCo, `reports/jacoco`). UI: Vitest is configured but `ui/tests/` is empty, so the UI has no own unit tests | Time. The UI was checked end to end by hand with Playwright (login, list, create form, browser syntax check, creation notification, version table). |
| NFR-09 (partly) | Coverage target, a mutation score, static analysis without HIGH findings, a stated method complexity limit | Coverage target met and enforced by the JaCoCo check in `mvn verify`. No mutation testing, no static analysis tool (Sonar, SpotBugs, PMD), no stated complexity limit | Not part of the build profile's finish line; recorded as a gap. |
| CODE-21, CODE-52 | Logging through the Simpl Log4J wrapper, in the common Simpl-Open log format | Spring Boot structured logging in ECS JSON (`logging.structured.format.console: ecs`) with correlation id, trace id and user id (token subject) in every line | The Log4J wrapper library is not available to the factory. ECS JSON is machine-parseable and meets CODE-26 to CODE-30. |
| CODE-33, CODE-43, CODE-44 | The shared exception framework's hierarchy; one system-wide exception class; error codes as enums | `ApiException` (carries the problem body, built by the `Problems` factory), plus `StoreException`, `StoreUnavailableException`, `IdentityProviderUnavailableException`, `TurtleSyntaxException`. No error code enum | The shared exception framework is not available to the factory. Every exception has one defined handling in `ApiExceptionHandler` (CODE-34 to CODE-36 are met). |

### Resilience and availability

| Rule | What the rule asks | What the build does | Why |
|---|---|---|---|
| MSV-07, CODE-46, ARCH-34 | Calls to other services go through a circuit breaker or bulkhead | Store and key set calls have connect and response timeouts, bounded retries for reads only (D44), and fail with 503. No circuit breaker | D44 decides timeouts and retries and nothing more; the store is the component's own data store, not another service. |
| ARCH-07 (partly), ARCH-15 (partly) | No single point of failure; horizontal scaling | The backend can run as several instances on one store (D82, proven by the own test `twoInstancesOnOneStoreSerialiseThroughTheStore`), but the compose stack runs one backend, one Fuseki and one Keycloak | A local stack on one host. Fuseki with TDB2 has no replication; high availability of the store is a platform matter. |

### Operations and pipeline

| Rule | What the rule asks | What the build does | Why |
|---|---|---|---|
| OPS-05, OPS-06, OPS-07, OPS-08, OPS-17, OPS-23, OPS-24, OPS-25, OPS-26, OPS-33, OPS-35, OPS-37, OWASP-12, OWT-A03 (partly) | A CI pipeline with SAST, SCA, image scans, unit tests, Sonar, image push, SBOM, lint and format gates that block the merge; protected branches | No pipeline file (no `.gitlab-ci.yml`) and no repository (the brief forbids git actions). Dependencies and base images are pinned, SBOMs are produced locally (`sbom/sbom-backend.json` by the CycloneDX Maven plugin, `sbom/sbom-ui.json` by `npm sbom`), and Trivy was run by hand on `work/` and on every image (`SECURITY-SCAN.md`). Tests run in `mvn verify` | The factory step builds and checks locally; the pipeline belongs to the project repository. |
| OPS-10, OPS-13, OPS-27, OPS-28, OPS-29, OPS-30 | Kubernetes network policies, service accounts, a Helm chart released per component, overridable values, a deployment strategy | Docker Compose only. Network separation is done with an internal `store-net` network; every container drops all capabilities, runs with `no-new-privileges`, non-root (except Keycloak, vendor image) and read-only where possible, with memory and CPU limits | The build profile fixes Docker Compose as the run environment. No Helm chart was written. |
| ARCH-22 | Every deployed service is registered in the service registry or catalogue | Not registered | No registry in a local factory build. |

### Data models

| Rule | What the rule asks | What the build does | Why |
|---|---|---|---|
| DATA-17, DATA-18 | The LDM is a UML class diagram with snake_case attributes | `docs/logical-data-model.md` has attribute tables and one Mermaid `erDiagram`; attributes are camelCase, matching the API and CDM names | camelCase keeps one name per concept across CDM, LDM and API (DDD-08, DDD-09). Mermaid has no full UML class notation for keys. |
| DATA-22, DATA-23, DATA-24 | The PDM is a UML class diagram with one class per table, physical types, primary and foreign keys | The store is RDF (Fuseki); the PDM is SHACL shapes per named graph (`docs/physical-model.shacl.ttl`) with datatypes and cardinalities, checked against stored data by `PhysicalModelShaclTest` | D87 decides SHACL shapes as the physical model. An RDF store has no tables or foreign keys. |

### Architecture

| Rule | What the rule asks | What the build does | Why |
|---|---|---|---|
| ARCH-27, ARCH-28 | Components communicate through events; non-blocking interactions where no immediate answer is needed | No events. Consumers pull state through the search and content operations | Decided in D45 (pull only). |

## 2. Decisions not implemented exactly as written, or with a caveat

| Decision | What is decided | What the build does | Why |
|---|---|---|---|
| D39, D79, D87 | The writers build the decided behaviour; the published OpenAPI stays unchanged; the build delivers `openapi-decided.yaml` | Implemented as decided. The running API differs from `specs/api/vocabulary_openapi-tier1.yaml` on every D79 point (stricter: 401/403 everywhere, pageSize max 100, name rules, text limits, unknown fields and parameters, version field, search sizes, aliases, absolute URIs, valid non-empty Turtle, 400 on invalid path values, 406, 405, 6 MB cap; wider: relaxed name filter, createdBy, validationWarnings always present, latestVersionHref always filled). Schemathesis against the published file reports these as "schema-valid request rejected" (13) and one "schema-violating request accepted" (the D19 name filter); no 5xx | Listed here because a reader of the published contract will see these as differences. `docs/openapi-decided.yaml` has them all. |
| D65 | S1 and S2 locations: statement `'<subject> <predicate> <object>'` with full IRIs and "the literal as written" | The literal is written as its lexical form without quotes or datatype, for example `statement 'https://ex.org/s https://ex.org/p literal'` | "As written" can mean the source text (`"literal"`) or the value. The build follows the OpenAPI example `statement 'http://example.org/subject http://example.org/predicate invalid-literal'`, which has no quotes. Owner question. |
| D44 (caveat) | The key set is fetched again when a token names an unknown key id | After a successful fetch, an unknown key id within the refresh cooldown (10 s, `vocabulary-manager.identity.refresh-cooldown`) is rejected with 401 without a new fetch. Outside the cooldown it fetches again, and when Keycloak is down that gives 503 as decided | The cooldown stops a stream of tokens with random key ids from turning every request into a call to Keycloak. A real key rotation is picked up at most 10 s later. |
| D70 (caveat) | A part with an empty name is an unknown field: 400, `name: ''` | Implemented, but through Tomcat's internal multipart parser (see REUSE-09 above) | The container's own part handling drops such parts. |
| D17, E6 | Gateway, OpenBao and participant-fe-components are production choices; the build checks tokens itself, uses environment variables and builds its own UI | As decided; see SEC-12, OPS-11 and UX-01 above | Decided. |
| Q3 (partly) | Daily backup of the whole store, kept outside the host; restore loads into a new dataset, checks the count, then replaces | `ops/backup.sh` and `ops/restore.sh` implement the backup and the staged, count-checked restore, and `ops/backup-restore-test.sh` passes on the live stack. No daily schedule is installed and the backup lands in `work/backups/` on the same host; moving it off the host is left to the operator. A restore replaces the live dataset with the backup, so writes made between backup and restore are lost unless the restore runs while the system is quiet | A scheduler and off-host storage are platform services outside the local stack. |
| Q6 (not verified) | WCAG 2.1 AA: no axe violations on the list, details and both forms | The UI uses labelled fields, roles `status` and `alert`, named regions and keyboard-reachable actions (D75), but no axe run is part of the build's own tests | Not measured in this build. |
| Run 7 open questions | 21 small wire details are undecided; the build's own choice is the default | The build makes its own choices on these points (for example free problem `type` and detail on 405 and unknown paths, D55) | As the decisions allow. |

## Summary of the review

405 rules reviewed (399 tagged for step 50, 6 for other steps): 272 judged followed, 58 not applicable to this component (Kafka and AsyncAPI rules API-62 to API-80, the asynchronous BLG rules, Angular rules, Tier 2 and agent-to-agent rules, sagas, long-running tasks, multi-language content and similar), 75 not followed or followed only in part (listed above, 30 entries).
