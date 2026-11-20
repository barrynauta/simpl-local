# REUSE.md: what the build reuses and what it builds itself

Reuse first (REUSE-01 to REUSE-07, E6, D93, D94). Versions are the pinned ones of `backend/pom.xml`,
`ui/package.json` and the Dockerfiles.

## Capabilities

| Capability | Reused (product, library, protocol, version) | Built here, and why |
|---|---|---|
| Identity provider, login | Keycloak 26.8.2 (reuse catalogue, E1), OpenID Connect, realm import at start | Nothing. Realm files only (`keycloak/`). |
| Token validation (signature, issuer, audience, expiry) | Spring Security OAuth2 resource server (Spring Boot 4.1.2), Nimbus JOSE + JWT (through Spring Security) | `AudienceValidator` (about 20 lines) plugs the audience rule of E5 into Spring's `OAuth2TokenValidator` chain. |
| Realm key fetch and cache | Nimbus `JWKSource`, `JWKSet` parsing and `JWKSelector` | `RealmKeySource` (122 lines) implements Nimbus' `JWKSource` interface. Nimbus `JWKSourceBuilder` (D94) was checked and not used: its refresh and outage behaviour (rate limit, refresh-ahead, outage cache) cannot give the exact outcomes of D44 and D88 (one fetch per 10 s, unknown key id inside the window is 401, no keys at all is 503 fail closed). Parsing, selection and signature checks stay in Nimbus; only the fetch timing is own code, with its own test `RealmKeySourceTest`. |
| Roles and access rules | Spring Security authorisation (`AuthorizationFilter`), realm roles from `realm_access.roles` | `RouteTable` (one role rule per operation, D84) and `RouteFilter` (405 before security, D67). Spring's matchers are used for the rules; the table exists so that `DefaultDenyTest` can prove every handler has a rule. |
| Persistence | Apache Jena Fuseki 6.2.0 (reuse catalogue, E3), dataset `vocabulary`, TDB2; SPARQL 1.1 Query and Update over HTTP | Repositories only. |
| SPARQL query building | Jena ARQ 6.2.0 `ParameterizedSparqlString` and the update model (D94) | No string joining of values. |
| Store HTTP client | JDK `java.net.http.HttpClient` (SPARQL 1.1 Protocol over HTTP POST, basic auth); results parsed with Jena `ResultSetMgr` (SPARQL JSON results) | `FusekiSparqlStore` (179 lines) adds the timeouts and retries of D44 and D71 and the trace headers of D86. The JDK client is used directly so the connect and response timeouts of D44 and D71 and the outbound headers are set per call; Jena has no retry policy with these exact rules; a retry library (Resilience4j) for two retries was judged heavier than the 20 lines it would replace. |
| Turtle parsing | Jena RIOT (RDF 1.1 Turtle) | `TurtleParser` (143 lines) wraps RIOT's error handler to get the first error with line and column (D9) and the declared prefixes (D12). No own parser. |
| Content validation rules S1 to S7 | Jena model API | `SemanticRules`, `ReferenceAnalysis`: these are the domain rules of D9 to D12, not infrastructure. SHACL (jena-shacl) was considered for S1 to S5; plain model queries give the decided detail and location texts directly, so SHACL is used only for the physical model (D87). |
| Physical model check | jena-shacl 6.2.0 (test scope) | Shapes in `docs/shapes/`. |
| Multipart parsing | Tomcat's `MultipartStream` parser (through Spring Boot) | `MultipartForm` (221 lines) adds the limits of D15 and D76 and keeps parts with an empty name (D70), which the container's own part handling drops. |
| Request validation against the contract (D93) | swagger-parser 2.1.49 (test scope) loads `docs/openapi-decided.yaml` | Choice of D93: own validation code (`RequestChecks`, `QueryParsers`, `FieldRules`, about 500 lines) plus `ContractConformanceTest`, which compares every operation, parameter, limit and pattern of the code with `openapi-decided.yaml`. A runtime OpenAPI validator (for example Atlassian swagger-request-validator) was not used because it cannot give the order of checks of D51 (413 before 400, 404 before 409) or the issue names of D52, D68 and D70. |
| JSON | Jackson 3 (Spring Boot 4.1.2) | None. |
| Problem details (RFC 9457, Belgif) | Spring `ProblemDetail` types and `application/problem+json` | `ProblemWriter`, `Problems` (197 lines): the decided types and texts of D48, D55, D66. Belgif publishes no Java library for its problem types. |
| Metrics | Micrometer with the Prometheus registry, Spring Boot Actuator on management port 8081 (D86) | None. |
| Trace context, correlation id | Micrometer Tracing with the Brave bridge (W3C `traceparent`, D94) | `CorrelationFilter` (X-Correlation-Id, which W3C trace context does not cover) and `OutboundHeaders`. |
| Structured logs | Spring Boot structured logging, logstash JSON format (D96) | None. |
| Health | Spring Boot Actuator liveness and readiness (MACH-08) | `HealthController` for the decided `GET /v1/health` body (D44). |
| Upload bulkhead (D92) | JDK `java.util.concurrent.Semaphore` | `UploadSlots` (54 lines). |
| Backup and restore | Fuseki / Jena `RDFDataMgr` (N-Quads) | `fuseki/ops/StoreOps.java` and `ops/*.sh`: dump, load into a new dataset, count check, swap (Q3). |
| SBOM | CycloneDX Maven plugin 2.9.3 (`sbom-backend.cdx.json`); cyclonedx-npm (`sbom-ui.cdx.json`) | None. |
| Formatting and lint | Spotless 3.11.0 with Palantir Java Format 2.103.0 (CODE-01); ESLint, Prettier, vue-tsc (CODE-05, D95) | None. |
| UI framework | Vue 3.5.45, Vue Router 4.6.5, Vite, TypeScript | Plain components. PrimeVue unstyled and TailwindCSS (participant-fe-components stack, E6) are not used: the build profile asks for a plain component library and the shared library is not available to the factory. |
| UI login | keycloak-js 26.2.5 (OIDC code flow with PKCE) | None. |
| Browser Turtle check (D13) | N3.js 2.13.12 | `turtleCheck.ts` maps N3's error to line and column. |
| UI web server and API proxy | nginx-unprivileged 1.31 | Config template only. |
| Accessibility check (Q6) | axe-core 4.14.1 run inside Vitest (jsdom) | None. |
| Messaging | Not needed (pull only, D45). | |
| Secrets | Environment variables from `.env` (MACH-07), OpenBao in production (E6) | `stack.sh` creates `.env` with random values. |

## Named patterns (REUSE-06)

| Pattern | Where |
|---|---|
| Optimistic concurrency with conditional updates (D82) | `FusekiVocabularyRepository` write token check, `VocabularyService` retry loop |
| Retry with backoff (reads only, D44) | `FusekiSparqlStore` |
| Timeout and fail closed / graceful degradation to 503 (D44, D88) | `FusekiSparqlStore`, `RealmKeySource`, `ApiExceptionHandler` |
| Bulkhead (D92) | `UploadSlots` |
| Default deny (D84) | `RouteTable`, `SecurityConfig`, `DefaultDenyTest` |
| Defence in depth | Tier 1 gateway in production plus own token and role checks (D17) |
| Least privilege | Three store users: read, update, admin (D83) |
| Stateless service | No in-process lock or session (D82) |
| Repository | `store` package behind `VocabularyRepository`, `ExternalVocabularyRepository` interfaces |
| Problem details | `ProblemWriter`, `Problems` |
| Backend for frontend proxy | nginx proxies `/v1` for the UI |
