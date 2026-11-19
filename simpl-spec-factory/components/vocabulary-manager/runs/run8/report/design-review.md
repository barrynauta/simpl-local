# Design review

## 1. In short

I reviewed **153 rules**: every rule with `check: review` that includes step 70.

| Level | Met | Partly | Not met | Not applicable | Total |
|---|---|---|---|---|---|
| MUST | 25 | 22 | 1 (OPS-37) | 4 | 52 |
| SHOULD | 45 | 20 | 11 | 25 | 101 |

| Prefix | Met | Partly | Not met | N/A |
|---|---|---|---|---|
| DDD | 1 | 1 | 0 | 0 |
| MACH | 6 | 0 | 0 | 0 |
| MSV | 4 | 0 | 0 | 1 |
| OWT / OWA | 2 | 5 | 0 | 0 |
| REUSE | 3 | 5 | 0 | 0 |
| NFR / SOA | 5 | 2 | 0 | 0 |
| API | 10 | 4 | 1 | 6 |
| CODE | 13 | 6 | 6 | 0 |
| ARCH | 15 | 7 | 2 | 1 |
| DATA | 7 | 6 | 1 | 14 |
| SEC | 2 | 3 | 1 | 3 |
| OPS | 2 | 0 | 1 | 3 |
| OWASP / UX | 0 | 3 | 0 | 1 |

**The five most important findings**

1. **The request validation layer is built by hand and not reported as built.** The OpenAPI is re-implemented in code across `OperationCatalogue`, `RequestShapeInterceptor`, `QueryParameters`, `JsonBody`, `MultipartForm` and `Paging`, about 900 lines. `JsonBody` says "every property can be checked against the schema by hand". No own test ties the catalogue to `docs/openapi-decided.yaml`. `HandlerRuleVerifier` only compares handlers with the catalogue, never with the contract. `REUSE.md` has no row for this capability, and no OpenAPI request validator was considered.
2. **SPARQL is built by string concatenation.** Examples: `VocabularyRepository.java` (all queries and updates) and `VocabularyFilter.toSparql`. Escaping goes through Jena `FmtUtils`, and sort values are whitelisted (`ListQuery.java:25-39`), so I found no injection. But Jena's `ParameterizedSparqlString` and `UpdateBuilder` exist for exactly this, and the code is hard to read. `TraceContext` also parses `traceparent` with its own regex, although Micrometer Tracing ships with Spring Boot.
3. **Memory is not bounded by concurrency.** Each upload buffers up to 6 MB (`MultipartForm`), then a base64 copy inside the SPARQL update string, then the full triple list (`TurtleParser.Collector`). Tomcat allows `threads.max: 100` in a 1 GB container. Validation also re-runs inside the `ConditionalWrite` retry loop (`VocabularyService.java:152-191`), so the 5 MB parse can repeat up to 6 times under contention. The 5 MB create already takes 21.9 s of its 30 s budget (`QUALITY.md`).
4. **No guideline enforcement for Java, and no UI tests.**
   - The Spotless version properties sit in `pom.xml:26-27`, but no plugin is configured.
   - The UI has Prettier but no linter.
   - There is no pipeline file.
   - `ui/tests/` is empty even though Vitest is installed.
5. **Transport and admin exposure.**
   - There is no TLS anywhere.
   - Keycloak runs `start-dev` with its admin console on host port 8193 (`compose.yaml:44-49`).
   - The management port 8081 (Prometheus, permit-all) is reachable from every container on the `front` network.
   - Backups are written unencrypted to `work/backups/` on the same host.

## 2. Verdicts

### DDD

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| DDD-03 | MUST | partly | `input/decisions/glossary.md` lists term, definition and CDM class. The build's `docs/` has no glossary or link to it, and it does not cover the build-only `Change` record. |
| DDD-14 | MUST | met | No other-context terms (EDC, Schema Manager) in `openapi-decided.yaml` or the code. |

### MACH

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| MACH-01 | SHOULD | met | Own image, own compose service. The backend needs Fuseki and Keycloak only at runtime (`compose.yaml` depends_on). |
| MACH-02 | MUST | met | The only callers are the store (own dataset) and the Keycloak JWKS endpoint (`RealmKeySource`). |
| MACH-03 | MUST | met | `docs/openapi-decided.yaml` covers every catalogue operation. Drift risk noted in finding 1. |
| MACH-06 | SHOULD | met | Stateless (`SessionCreationPolicy.STATELESS`). No lock state (`ConditionalWrite`). The only cache is the public-key cache. |
| MACH-07 | MUST | met | `application.yml` reads everything from env. Passwords have no defaults (`read-password: ${STORE_READ_PASSWORD:}`). `.env` is git-ignored with mode 600. |
| MACH-09 | SHOULD | met | `server.shutdown: graceful`, `stop_grace_period: 30s`, readiness HEALTHCHECK in `backend/Dockerfile`. |

### MSV

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| MSV-02 | MUST | met | Fuseki dataset `vocabulary` is used only by this backend and the ops scripts (`fuseki/config.ttl`). |
| MSV-04 | MUST | n/a | No events (D45). |
| MSV-06 | MUST | met | `HttpSparqlStore`: 2 s connect, 10 s or 45 s timeouts, read retries of 200 ms and 400 ms, 503 with Retry-After (`ApiExceptionHandler.java:57`). The JWKS fetch has a 2 s timeout and fails closed. |
| MSV-09 | MUST | met | `CorrelationFilter` sets the MDC and the response header. `TraceContext.outgoingHeaders()` is forwarded in `HttpSparqlStore.request` and `RealmKeySource.refresh`. |
| MSV-10 | SHOULD | met | ECS structured logs (`logging.structured.format.console: ecs`) and Prometheus on 8081. |

### OWT / OWA

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| OWT-A02 | MUST | partly | Errors carry no internals (`include-stacktrace: never`, `Problems.internalServerError`). But Keycloak `start-dev` and its admin console are published on 8193, and 8081 is permit-all on the shared `front` network (`SecurityConfig.java:78-87`). |
| OWT-A04 | MUST | partly | Crypto is Nimbus and there are no secrets in code. There is no TLS anywhere, and the TDB2 volume and backups are not encrypted. |
| OWT-A07 | MUST | partly | Signature, issuer, audience, expiry and required claims are checked (`TokenValidator.java:42-53`), and the token is never in a URL. There is no limit on repeated invalid tokens in the service (left to the gateway, D46). |
| OWT-A08 | MUST | met | Fuseki jar is SHA-512 checked (`fuseki/Dockerfile`). Images are pinned by digest. JSON is read as a tree and never bound to classes. Turtle is parsed to triples. |
| OWT-A09 | MUST | partly | The `security` logger records 401, 403 and firewall refusals without personal data. There is a change-graph audit trail. No alerting. |
| OWT-A10 | MUST | met | One SPARQL update per write with a store-side condition (`insertVersion` WHERE). Fails closed on unknown key id with 503. |
| OWA-API8 | SHOULD | partly | Security headers and CSP in `nginx/default.conf.template` and `SecurityConfig`. Same-origin proxy, so CORS stays closed. Server URL is `http://` and there is no TLS. |

### REUSE

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| REUSE-01 | MUST | partly | Signature verification is Nimbus `DefaultJWTProcessor`. But the JWKS fetch, cache and cooldown are built by hand in `RealmKeySource`, where Nimbus `JWKSourceBuilder` (timeouts, rate limit, outage cache) or the Spring resource server would do. `REUSE.md` calls this "only the glue". |
| REUSE-02 | SHOULD | partly | Turtle, SPARQL results, JSON and multipart use libraries. SPARQL is built by concatenation, `traceparent` by regex (`TraceContext.java:24`), and JSON schema checks by hand. |
| REUSE-03 | SHOULD | met | Fuseki, Keycloak, nginx and Actuator are configured, not built. |
| REUSE-04 | MUST | met | RFC 9457 problem+json, OIDC bearer, W3C trace context, SPARQL 1.1 protocol, OpenAPI. |
| REUSE-05 | SHOULD | partly | The catalogue was checked. `certificate-authority` and `participant-shared-code` are not addressed in `REUSE.md`. |
| REUSE-06 | SHOULD | met | Patterns are named in `docs/architecture.md` and `REUSE.md` (optimistic concurrency, repository, fail closed, cache-aside). No clear over-engineering, though finding 1 is a home-grown "contract-first request validation" that is not named. |
| REUSE-07 | MUST | partly | `REUSE.md` exists but has no rows for request-shape validation, SPARQL query building or the own JWKS cache. |
| REUSE-09 | SHOULD | partly | Uses Tomcat-internal `org.apache.tomcat.util.http.fileupload.MultipartStream` (`MultipartForm.java:16`). |

### NFR / SOA

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| NFR-05 | SHOULD | partly | Every SPARQL query has a LIMIT, pageSize is at most 100, JWKS is at most 1 MB, every container has limits. Per-request memory is not bounded against concurrency (finding 3). `LOOKUP_LIMIT = 20_000` (`VocabularyRepository.java:45`). |
| NFR-06 | MUST | partly | Q3 targets are stated, and `ops/backup-restore-test.sh` ran (`QUALITY.md`). No schedule, and the backup stays on the host. |
| NFR-08 | SHOULD | met | ECS logs with correlationId and traceId, `http_server_requests` per uri, `traceparent` forwarded. |
| NFR-11 | SHOULD | met | Configuration only through env and mounted files. No host paths in the code. |
| NFR-12 | SHOULD | met | Q4 load is stated. Scale-out is proven by `twoInstancesOnOneStoreSerialiseThroughTheStore`. |
| NFR-13 | MUST | met | `QUALITY.md` measures Q1 to Q5. Q6 is listed as not measured, with the reason. |
| SOA-05 | SHOULD | met | The only dependencies are the store and the IdP. Cached keys survive an IdP outage. 503 is on every secured operation in the decided contract. |

### API

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| API-01 | MUST | partly | No CDM is delivered next to `openapi-decided.yaml`. The LDM traces to an external snapshot. |
| API-02 | SHOULD | partly | Schemas are local, as inherited from the published contract (D39). |
| API-03 | SHOULD | met | Derived from the hand-written published file by `make-openapi-decided.py`. No springdoc. |
| API-10 | SHOULD | partly | `/external-vocabulary/searches` is singular (inherited). |
| API-11 | SHOULD | met | POST for searches is justified by the request body. |
| API-15 | SHOULD | met | No British spellings in property names. |
| API-19 | SHOULD | met | Names carry business meaning. |
| API-22 | SHOULD | met | Examples are realistic (inherited). |
| API-28 | SHOULD | met | Every path is a document, a collection or a controller. |
| API-30 | SHOULD | met | Detail paths follow `/vocabularies/{vocabularyName}`. |
| API-35 | SHOULD | partly | Accept is set on store queries and the JWKS fetch, but not on SPARQL updates (`HttpSparqlStore.update`). |
| API-49 | SHOULD | met | The business purpose is kept. The decided file appends technical notes. |
| API-50 | SHOULD | not met | The identifier is `name` (D30). Recorded deviation. |
| API-51 | SHOULD | n/a | No reference properties. |
| API-54 | SHOULD | n/a | No identifier schema types. |
| API-55 | SHOULD | n/a | No standard business identifier applies. |
| API-58 | SHOULD | met | `status` and `sort` are enums. |
| API-60 | SHOULD | met | Required flags are set. |
| API-62 | MUST | n/a | No Kafka. |
| API-79 | SHOULD | n/a | No Kafka topics. |
| API-80 | SHOULD | n/a | No Kafka topics. |

### CODE

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| CODE-06 | MUST | met | Small methods with one action. |
| CODE-09 | MUST | met | No flag-switched methods found. |
| CODE-11 | MUST | partly | `SparqlStore` and `ReferenceLookup` are interfaces, but services depend on the concrete `VocabularyRepository` and `ExternalVocabularyRepository`. |
| CODE-14 | MUST | met | Identifiers are English. |
| CODE-15 | MUST | met | Comments are English. |
| CODE-16 | MUST | partly | Multi-line SPARQL string assembly in the repositories is hard to read and review. |
| CODE-17 | MUST | met | Names are clear. |
| CODE-20 | MUST | partly | "Vocabulary created with version 1" (`VocabularyService.java:162`) and `ApiExceptionHandler.handleApi` do not log the vocabulary name. Only the access-log path has it. |
| CODE-21 | SHOULD | not met | No Log4J wrapper (recorded). |
| CODE-24 | MUST | met | Levels fit: info for user actions, warn for refusals, error for store errors. |
| CODE-25 | SHOULD | met | `error` is used only for store errors, IdP outages and unexpected errors. |
| CODE-30 | SHOULD | partly | Correlation, trace and user ids are in the MDC. No entity id, and `MDC_OPERATION` is declared but never set (`TraceContext.java:21`). |
| CODE-32 | SHOULD | met | Only the token `sub` is logged (`TokenAuthenticationFilter.java:63`). |
| CODE-33 | SHOULD | not met | No shared base exception (recorded). |
| CODE-34 | MUST | met | One `@RestControllerAdvice` plus `ProblemWriter` in filters. |
| CODE-35 | MUST | met | Covers own, framework (`HttpMediaType*`, `NoHandlerFound`) and library exceptions (`StoreException`, catch-all). |
| CODE-36 | MUST | met | Each exception has a fixed problem body. |
| CODE-44 | SHOULD | not met | No error-code enum (recorded). |
| CODE-45 | SHOULD | not met | No gateway (decided, D17). |
| CODE-46 | SHOULD | not met | No circuit breaker (recorded). |
| CODE-48 | SHOULD | met | Beans are injected. Filters are built in `@Configuration`. |
| CODE-50 | SHOULD | met | JSON logs go to stdout. |
| CODE-52 | SHOULD | partly | ECS JSON, not the Simpl log format. |
| CODE-54 | MUST | partly | The formatting guideline is not enforced for Java. |
| CODE-55 | SHOULD | not met | Java has no formatter or linter configured (Spotless declared only). TypeScript has Prettier but no linter. Shell and Dockerfiles have none. |

### ARCH

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| ARCH-01 | SHOULD | met | No central dependency beyond the IdP. |
| ARCH-02 | MUST | met | Pull-only API (D45). Other components depend only on it. |
| ARCH-04 | SHOULD | met | Only the store and the IdP. |
| ARCH-05 | SHOULD | met | Store IRIs and graphs are not exposed on the wire. |
| ARCH-07 | MUST | partly | Several instances are proven by a test, but compose runs one of each and Fuseki is a single node. |
| ARCH-08 | SHOULD | partly | No `LICENSE` (EUPL) file in `work/`. |
| ARCH-09 | SHOULD | met | No vendor SDKs. |
| ARCH-11 | SHOULD | met | The search and content operations are generic. |
| ARCH-13 | SHOULD | met | `docs/api-notes.md` describes contracts only. |
| ARCH-14 | SHOULD | partly | Limits are hard-coded in `compose.yaml`, not overridable by variable. There is no chart. |
| ARCH-17 | MUST | partly | Access control and secrets are good. No TLS, nothing encrypted at rest. |
| ARCH-18 | MUST | met | Validation, one transaction per write, SHA-256 stored. |
| ARCH-19 | MUST | partly | Backup and restore are tested. No replicas, no off-host copy. |
| ARCH-20 | MUST | met | Logs carry `sub` only. `createdBy` email is exposed to viewers by D27 (noted). |
| ARCH-22 | SHOULD | not met | Not registered (recorded). |
| ARCH-23 | SHOULD | met | `docs/README.md` and `info.description`. |
| ARCH-24 | SHOULD | partly | Own image. No pipeline or chart. |
| ARCH-25 | SHOULD | met | Vocabulary management only. |
| ARCH-26 | SHOULD | met | The dataset is used by one service. |
| ARCH-27 | SHOULD | n/a | Pull only, by decision D45. |
| ARCH-28 | SHOULD | partly | The 5 MB write is held synchronously for up to 22 s (D71 allows it). |
| ARCH-30 | SHOULD | met | Three store users with split rights (`fuseki/config.ttl`). Two realm roles. |
| ARCH-31 | SHOULD | met | Service-side token, role and input checks, plus the internal network. |
| ARCH-33 | SHOULD | met | Reads are retried with backoff. Writes are not retried, by decision. |
| ARCH-34 | SHOULD | not met | No circuit breaker. |

### DATA

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| DATA-01 | SHOULD | partly | The CDM is referenced (snapshot 70f951f) but not in the input or the build. |
| DATA-02 | SHOULD | met | `docs/logical-data-model.md`. |
| DATA-03 | SHOULD | met | `physical-model.shacl.ttl` (D87), tested by `PhysicalModelShaclTest`. |
| DATA-04 to DATA-16 | SHOULD | n/a (13 rules) | The CDM is not in the build or its inputs, so it cannot be judged here. |
| DATA-17 | SHOULD | partly | Mermaid `erDiagram`, not UML. |
| DATA-19 | SHOULD | met | Types and PK/UK are declared. |
| DATA-20 | SHOULD | met | Multiplicities are shown. |
| DATA-21 | SHOULD | n/a | No many-to-many with own data. |
| DATA-22 | SHOULD | partly | SHACL per graph, not a class per table (decided). |
| DATA-23 | SHOULD | met | xsd datatypes in the shapes. |
| DATA-24 | SHOULD | partly | Keys are implicit in the IRIs, not declared. |
| DATA-25 | SHOULD | not met | No access structures declared (TDB2 default indexes, `vm:nameKey` and `vm:lookupKey` lookups undocumented). |
| DATA-26 | MUST | partly | The CDM changes of D3, D10 and D25 to D28 are only owner questions. |
| DATA-27 | MUST | met | The decided contract has every D79 change. |
| DATA-28 | MUST | met | The docs reference the decisions. |
| DATA-29 | MUST | partly | The CDM is not versioned with `openapi-decided.yaml`. |

### SEC / OPS / OWASP / UX

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| SEC-01 | MUST | partly | Security design is in `architecture.md`. No threat model. |
| SEC-02 | MUST | n/a | The guideline document is not available. |
| SEC-07 | SHOULD | partly | Plain Keycloak OIDC, without the plugin. |
| SEC-11 | SHOULD | partly | Logs go to stdout. No shipping to ELK. |
| SEC-12 | SHOULD | not met | No gateway (decided). |
| SEC-13 | SHOULD | n/a | No Tier 2. |
| SEC-15 | SHOULD | n/a | No agent-to-agent calls. |
| SEC-22 | SHOULD | met | Technical accounts for the store and the Keycloak bootstrap admin. |
| SEC-23 | SHOULD | met | Realm roles assigned to users (`realm-simpl.json`). |
| OPS-01 | SHOULD | met | No OVH services. |
| OPS-02 | SHOULD | met | Synthetic test data. |
| OPS-03 | SHOULD | n/a | No production data. |
| OPS-17 | SHOULD | n/a | No repository. |
| OPS-29 | SHOULD | n/a | No chart. |
| OPS-37 | MUST | not met | No pipeline. Lint and format are not wired even locally. |
| OWASP-01 | MUST | partly | Follows from OWT-A02, A04, A07 and A09. |
| OWASP-05 | MUST | partly | No threat model. |
| OWASP-10 | MUST | partly | Events are logged. No monitoring or alerting. |
| UX-01 | MUST | n/a | Cannot be checked yet. Note that `participant-fe-components` was not used. |

## 3. Disagreements with the build's own documents

**I am stricter than the build:**
- **REUSE-01.** `REUSE.md` calls `RealmKeySource` "only the glue". It is an own JWKS fetch, cache and refresh-cooldown implementation where Nimbus `JWKSourceBuilder` or Spring's OAuth2 resource server exist. I rate it partly; the build treats it as met.
- **REUSE-02.** It is not listed in DEVIATIONS. SPARQL concatenation, the `traceparent` regex and the hand-written JSON schema checks make it partly.
- **REUSE-05 and REUSE-07.** `REUSE.md` claims the catalogue was checked and the list is complete. Two catalogue entries are not addressed, and three built capabilities have no row: request validation, SPARQL building and the JWKS cache.
- **NFR-05.** `QUALITY.md` presents the limits as complete. Per-request memory against thread count is not bounded, and validation repeats inside the retry loop.
- **CODE-20 and CODE-30.** DEVIATIONS says ECS "meets CODE-26 to CODE-30". The logs lack entity ids, and `MDC_OPERATION` is never set.
- **CODE-11 and CODE-16.** Not mentioned. Services depend on concrete repositories, and the SPARQL strings hurt readability.
- **CODE-55.** Only CODE-01, CODE-02 and CODE-13 are listed. There is no linter for TypeScript and no formatter for Java.
- **OWT-A02.** Not mentioned. Keycloak dev mode with its admin console is published, and the permit-all management port is reachable from the UI container's network.
- **API-01, DATA-25, DATA-26, DATA-29, ARCH-08 and ARCH-14.** Not mentioned. CDM delivery, index declarations, the licence file and configurable limits are missing.

**The build is stricter than me:** none of weight. I agree with its partly or not-met entries for OWT-A04, OWT-A07, OWT-A09, REUSE-09, ARCH-07, CODE-46 and NFR-06 / Q3.

**Claims in `QUALITY.md` I could confirm from the code:**
- Every query has a LIMIT.
- No lock state.
- Container limits.
- Graceful shutdown.
- Two store users split by purpose.

## 4. Findings for the next run

1. **Decide on request validation.** Either require a maintained OpenAPI request validator that loads `openapi-decided.yaml` at start, or require an own test that diffs `OperationCatalogue` with that file. Make `REUSE-07` say that every hand-built component over about 100 lines needs a `REUSE.md` row.
2. **Name the SPARQL building tool in the decisions.** Add to E6: "build queries with Jena `ParameterizedSparqlString` or `UpdateBuilder`, no string concatenation of terms".
3. **Name the libraries for JWKS and trace context.** Use Nimbus `JWKSourceBuilder` (or the Spring resource server with a custom timeout), and Micrometer Tracing with the W3C propagator, unless a decision says why not.
4. **Add an upload concurrency bound to Q2/NFR-05.** For example, at most N parallel uploads, then 503. Move validation out of the `ConditionalWrite` retry loop, since the content does not change between attempts.
5. **Put lint and format gates on the finish line.** Require Spotless wired in `mvn verify`, ESLint plus Prettier check in the UI build, and at least one UI unit test per view.
6. **Add a logging rule to the brief.** Every log line about an entity carries the vocabulary name and version as structured fields, and `operationId` is set in the MDC.
7. **Close the CDM gap.** Either put the CDM snapshot in `input/specs`, or state that DATA-04 to DATA-16 and API-01 are out of scope for builds. Have the build ship a glossary link and a `LICENSE` (EUPL).
8. **Compose hardening in the build profile.** Run Keycloak in production mode, or keep dev mode but not publish its admin console. Bind the management port to a separate internal network. Make container limits variables.
