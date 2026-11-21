# Reuse decisions (REUSE-07)

Reuse catalogue: `input/reuse-catalogue.yaml`; decisions E6, D93, D94, D104, D108, D132.

## Per capability

| Capability | Reused (version) | Built ourselves, and why |
|---|---|---|
| Identity provider | Keycloak 26.8.2 (catalogue: identity-provider), realms imported at start | Realm JSON only |
| API gateway authorisation | Not in the local stack (catalogue: Spring Cloud Gateway). The service checks tokens and roles itself (D17); the local nginx gateway only proxies | nginx config (catalogue gateway is not available to the factory) |
| Token validation | Nimbus JOSE+JWT 10.10: `JWKSet`, `DefaultJWSVerifierFactory`, `DefaultJWTClaimsVerifier` (D108, D132) | Fetch timing only (`TokenVerifier.keyFor*`, about 60 lines): D88 needs a 10 s cooldown and a 503 after a failed fetch, which `JWKSourceBuilder` does not give |
| Authorisation | Spring Security 7 (Boot 4.1.3) `authorizeHttpRequests`, `StrictHttpFirewall` | Route table `Routes` (one list read by security, the method check and the start-up check, D84) |
| Secrets | Environment variables (`.env` created by `stack.sh`); OpenBao in production (catalogue: secrets-and-keys) | none |
| RDF store | Apache Jena Fuseki 6.2.0, TDB2, Shiro basic auth (catalogue: rdf-store) | Image build (download with checksum) and shiro.ini template |
| SPARQL building | Jena `ParameterizedSparqlString` (6.2.0) with typed nodes (D94) | none |
| SPARQL transport | java.net.http `HttpClient` (JDK 21) and Jena `ResultSetMgr` (result parsing) | `StoreClient` (about 190 lines): Jena's `QueryExecHTTP`/`UpdateExecHTTP` were not used because D44 needs per-call response timeouts for both queries and updates, retry only for reads and classification of failures for the breaker; the own client is tested by every integration test |
| Turtle parsing | Jena RIOT `RDFParser` (6.2.0); n3 2.13.12 in the browser (D13) | none |
| Semantic rules S1 to S6 | none fits: the rules are decided project rules (D9), not OWL reasoning | `SemanticRules` (about 190 lines), unit tested |
| JSON | Jackson 3 (Boot 4.1.3) with strict duplicate detection | none |
| Multipart | Tomcat 11 multipart via Servlet `getParts()` (D121: no own parser) | none |
| URIs, dates | `java.net.URI`, `java.time` | none |
| Request validation (D93) | Own code plus `OpenApiContractTest`, which compares the code with `docs/openapi-decided.yaml` (routes, operationIds, query parameters, sort enums, form fields, limits, patterns) | A maintained OpenAPI request validator (for example swagger-request-validator) was not used: the decided contract needs 400 before 404, issue names per D68 and D137, 405 before 401, a fixed check order (D51) and texts the validator cannot produce |
| Error model | RFC 9457 problem details (Belgif types), one exception class with a problem-type enum (D104) | `ApiException`, `ProblemType`, `ProblemWriter` |
| Resilience | Resilience4j 2.4.0 circuit breakers (D103) | `WriteRetry` (about 40 lines): fixed backoff and budget of D157; Resilience4j Retry was not used because the retry must re-read state between attempts and stop at the D71 budget |
| Concurrency of uploads | `java.util.concurrent.Semaphore` | `UploadLimiter` (20 lines) |
| Metrics | Micrometer 1.16 + Prometheus registry, Spring Boot Actuator (separate management port) | `operationId` tag through an observation convention |
| Tracing and correlation | Micrometer Tracing with Brave, W3C propagation (D86, D94) | `CorrelationFilter` for X-Correlation-Id (no library offers it) |
| Logging | Spring Boot structured logging (logstash JSON), Logback | none (Simpl LogWrapper is not available, D105) |
| Validation of stored data | Jena SHACL (test scope) against `docs/physical-model-shapes.ttl` | shapes |
| UI framework | Vue 3.5, vue-router 5.5, PrimeVue 4.5.5 unstyled, Tailwind 4.3 (E6, D167); keycloak-js 26.2.5 | Views; the Simpl participant-fe-components library is not available to the factory (D167) |
| Reverse proxy | nginx 1.31 (nginxinc/nginx-unprivileged) | Config only |
| Backup and restore | Fuseki admin backup and compact endpoints, `tdb2.tdbloader` and `tdb2.tdbquery` | Shell scripts in `ops/` |
| Messaging, relational store, object storage, connector, http-client | Not needed (E6, D45, D106) | n/a |
| Build, format, coverage, SBOM | Maven, Spotless (Palantir 2.80.0), JaCoCo 0.8.14, CycloneDX Maven plugin 2.9.1, `npm sbom` | none |
| Test infrastructure | JUnit 5, AssertJ, Mockito, Spring Boot test, Jena Fuseki main (in-process store), JDK `HttpServer` as key endpoint | `TestIdp` (test only) |

## Named patterns (REUSE-06)

| Pattern | Where |
|---|---|
| Optimistic concurrency (conditional update with version check) | `VocabularyStore.insertVersion`, `changeVocabularyStatus`, `changeVersionStatus` |
| Idempotency token / write receipt | `createToken`, `statusChange` checked after each update |
| Retry with exponential backoff and jitter | `WriteRetry` (writes), `StoreClient.withReadRetry` (reads) |
| Circuit breaker | `StoreClient`, `TokenVerifier` (Resilience4j) |
| Timeout | `StoreClient`, `TokenVerifier` |
| Bulkhead (semaphore) | `UploadLimiter` |
| Fail closed | `TokenVerifier` (503, never accept unchecked), `RouteRuleCheck` (no start without rules) |
| Graceful degradation | cached realm keys keep working while Keycloak is down; GET /health DEGRADED |
| Repository (DAO) | `store/VocabularyStore`, `ExternalStore`, `ReferenceStore` |
| Chain of responsibility (filter chain) | `CorrelationFilter`, `RouteGuardFilter`, security chain, `NegotiationFilter` |
| Aggregate root | `InternalVocabulary` owns its versions; all version changes go through the vocabulary name |
| Stateless service, externalised state | backend replicas share only the store |
| Least privilege | store read and update users, admin only for ops; roles per operation |
| Defence in depth | gateway, firewall, token check, role rules, internal store network |
