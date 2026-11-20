# Reuse decisions (REUSE-07)

## Per capability

| Capability | Reused (product, library, protocol, version) | Built ourselves, and why |
|---|---|---|
| Identity provider, login, tokens, roles | Keycloak 26.8.2 (catalogue `identity-provider`), OpenID Connect, realm roles (E1) | Realm import files only |
| Token validation (signature, claims) | Nimbus JOSE + JWT 10.10 (`SignedJWT`, `DefaultJWSVerifierFactory`, `JWKSet`) | The key fetch timing (10 s cooldown, fail closed with 503, D88, D108) is ours, as D108 decides; about 120 lines in `RealmKeyProvider`. Spring Security's resource server or Nimbus `JWKSourceBuilder` could have fetched keys, but their rate limiting and failure mapping do not give the decided 401 and 503 outcomes |
| Authorisation, HTTP firewall, security headers | Spring Security 7.1.3 (`authorizeHttpRequests` with a custom `AuthorizationManager`, `StrictHttpFirewall`, default headers) | The role rule per operation comes from our operation table (D84); in production the Tier 1 gateway (Spring Cloud Gateway, catalogue `api-gateway-authorisation`) applies the same rules first (D17) |
| Secrets | Environment variables (MACH-07); OpenBao in production (catalogue `secrets-and-keys`) | `stack.sh` generates local random passwords into `.env` |
| RDF store | Apache Jena Fuseki 6.2.0 (catalogue `rdf-store`), Fuseki Main plain mode, TDB2, basic auth with access lists | Config file and entrypoint |
| SPARQL protocol client | JDK `java.net.http.HttpClient` plus Jena 6.2.0 `ResultSetMgr` for results | About 220 lines in `SparqlStore` for timeouts per call type, read retries, breaker and trace headers. Jena's `RDFConnection`/`QueryExecHTTP` could have sent the requests, but per-request timeouts for updates, the retry policy and the 503 mapping of D44 needed control over the HTTP exchange |
| SPARQL query building | Jena `ParameterizedSparqlString` and update syntax objects (`UpdateModify`, `QuadAcc`) (D94) | Never string joins with values |
| Turtle parsing (server) | Jena RIOT 6.2.0 (`RDFParser`, `ErrorHandler`) | none |
| Semantic rules S1 to S6, reference check S7 | Jena graph API | The rules themselves (D9, D12): no library implements these decided rules (an OWL reasoner would be heavier and give other messages); about 300 lines |
| Turtle parsing (browser) | N3.js 2.13.12 (D13) | none |
| JSON | Jackson 3 (Spring Boot 4 default) | none |
| Multipart | Tomcat 11 multipart parser through the Servlet API (`getParts`) | Field rules on top; about 110 lines in `MultipartForm` |
| Request validation against the contract (D93) | own code plus `DecidedContractTest`, which loads `docs/openapi-decided.yaml` with SnakeYAML and compares every operation, role rule, query parameter, limit, pattern and form field with the code | A maintained OpenAPI request validator (for example the Atlassian swagger-request-validator) was not used: the decided order of checks (D51), issue names (D68, D110) and the problem shapes are decided precisely and a generic validator reports differently. About 350 lines across `Checks`, `QueryParameters`, `FieldRules`, `Paging` and the controllers |
| Problem details | RFC 9457 with the Belgif problem and issue types (REUSE-04) | One exception class and one handler (D104) |
| Resilience | Resilience4j 2.4.0 `circuitbreaker` and `micrometer` (D103) | Retry of reads is a 10-line loop (2 retries, fixed back-off) |
| Tracing and correlation | Micrometer Tracing 1.7.3 with the Brave bridge (W3C trace context, D86, D94) | `CorrelationFilter` for `X-Correlation-Id` (about 40 lines): no library handles this header |
| Metrics | Micrometer with the Prometheus registry, Spring Boot Actuator on the management port | none |
| Logging | Spring Boot structured logging (logstash JSON), SLF4J, Logback | none (Simpl LogWrapper out of scope, D105) |
| Concurrency control | Store-side conditional updates in one SPARQL update request (one TDB2 write transaction) | Change-token pattern in `VocabularyRepository` (see patterns) |
| Paging links | none | About 60 lines in `Paging` (the decided link shape of D59, D78, D110, D111 is specific) |
| Health | Spring Boot Actuator probes (management port) and our `GET /v1/health` with the decided statuses (D44, D108) | none |
| UI framework | Vue 3.5, vue-router 5, Vite 8, TypeScript 5.9, keycloak-js 26.2 | Plain components. The catalogue's `participant-fe-components` (PrimeVue unstyled, TailwindCSS, Simpl Figma) is not available to the factory (E6), so screens are built with plain HTML and CSS |
| UI tests, lint, format | Vitest 5, Vue Test Utils, ESLint 10 with eslint-plugin-vue and typescript-eslint, Prettier 3 | none |
| Front proxy | nginx 1.31 (nginx-unprivileged image) | Configuration only |
| Messaging | not used: pull only (D45) | n/a |
| Relational store, object storage, connector, simpl-http-client | not needed (E6, D106): no outgoing calls other than the store and the realm keys inside the deployment | n/a |
| SBOM, scanning | CycloneDX Maven plugin 2.9.3, `npm sbom`, Trivy 0.76.2 | none |
| Backup and restore | Fuseki Graph Store Protocol (N-Quads), curl | Scripts only |

## Named patterns (REUSE-06)

| Pattern | Where |
|---|---|
| Layered architecture (controller, service, repository) | `api`, `service`, `store` packages |
| Front controller with a filter chain (chain of responsibility) | `RoutingFilter`, `TokenAuthenticationFilter`, `MediaTypeFilter` in the Spring Security chain |
| Default deny, least privilege | `OperationAuthorizationManager`, Fuseki access lists (D83, D84) |
| Defence in depth | gateway in production, token check in the service, store users, internal networks |
| Optimistic concurrency control (compare and set with a change token) | `VocabularyRepository#insertVersion`, `updateVocabularyStatus`, `ExternalVocabularyRepository#insert` (D82) |
| Retry with back-off | `SparqlStore#select` (reads only) and the write retry loop in `VocabularyService` |
| Circuit breaker | `SparqlStore`, `RealmKeyProvider` (D103) |
| Bulkhead (semaphore) | `UploadLimiter` (D92) |
| Fail fast, fail closed | 503 with Retry-After when the store or the key fetch is unavailable (D44) |
| Graceful degradation | health DEGRADED, cached realm keys keep working (D44) |
| Stateless service, scale out | no session or lock state; two replicas behind the proxy (D102) |
| Correlation identifier, distributed tracing | `CorrelationFilter`, Micrometer Tracing (D86) |
| Problem details (RFC 9457) | `Problems`, `ProblemHandler` |
| Data transfer object | `api/dto/Dtos` |
| Backends for frontends not used: the UI uses the public API (MACH-04) | proxy forwards `/v1` |
