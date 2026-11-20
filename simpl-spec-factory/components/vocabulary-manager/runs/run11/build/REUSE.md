# Reuse decisions (REUSE-01 to REUSE-09, D93, E6)

The reuse catalogue (`input/reuse-catalogue.yaml`) was checked first (REUSE-05).

## Per capability

| Capability | Reused (version) | Or built, and why |
|---|---|---|
| Identity provider, login, tokens | Keycloak 26.8.2 (catalogue: identity-provider), image pinned by digest | The Simpl-Open custom authenticator plugin is not in the build input; plain Keycloak with the realms of E1 and E7 |
| API gateway authorisation | not deployed (catalogue: Spring Cloud Gateway) | D17: the service checks tokens and roles itself so the rules are testable without a gateway; in production the Tier 1 gateway applies the same rules first |
| Token validation (signature, issuer, audience, expiry, not-before) | Nimbus JOSE+JWT 10.10: `DefaultJWTProcessor`, `JWSVerificationKeySelector`, `DefaultJWTClaimsVerifier`, `JWKSet` (D108, D132) | Own code only for the key fetch timing of D88 and D129 (`RealmKeys`, about 100 lines): Nimbus `JWKSourceBuilder` has its own refresh and outage rules that do not give the decided 401 / 503 outcomes |
| Authorisation rules | Spring Security 7.1 (`authorizeHttpRequests`, firewall, entry point, access denied handler) | Rules generated from one route table so each operation has exactly one rule (D84) |
| Secrets | environment variables, `.env` created by `stack.sh` (catalogue: OpenBao in production) | OpenBao is not run locally (E6) |
| RDF store | Apache Jena Fuseki 6.2.0, TDB2 (catalogue: rdf-store) | none |
| SPARQL query and update building | Jena ARQ 6.2.0 `ParameterizedSparqlString` (D94) | none |
| SPARQL result parsing | Jena `ResultSetMgr` (JSON results) | none |
| SPARQL protocol transport | JDK `java.net.http.HttpClient` (Java 21) | The catalogue's simpl-http-client is for Tier 2 mTLS calls between participants; the store is an internal dependency. Jena's own remote connection does not let the service set a separate timeout per call kind (2 s connect, 10 s read, 45 s update, 2 s health) together with per-request basic auth and trace headers, so `SparqlClient` (about 200 lines) wraps the JDK client |
| Turtle parsing (syntax check) | Jena RIOT `RDFParser` (REUSE-02) | none |
| Semantic rules S1 to S7 | own code (`SemanticRules`, `ReferenceChecker`, about 330 lines) | The rules are decided by D9 and D12 and are not OWL reasoning or SHACL constraints with these texts and locations; a reasoner (Jena OWL) would add load and still need mapping to the decided report |
| URI syntax | Jena `RFC3986` parser (jena-iri3986) | none |
| JSON | Jackson 3.1 (Spring Boot), strict duplicate detection | none |
| Multipart | Servlet container parser (Tomcat 11) through `HttpServletRequest.getParts()` (D121) | none |
| Query string | JDK `URLDecoder` | Strict checks of D21, D52 and D138 on top (`QueryParameters`, about 80 lines): Spring's binding silently accepts unknown and repeated parameters |
| Request validation against the contract (D93) | own code plus `DecidedContractTest`, which loads `docs/openapi-decided.yaml` (SnakeYAML) and compares operations, query parameters, limits and patterns with the code | A maintained OpenAPI validator (Atlassian swagger-request-validator) cannot produce the decided issue names (`items[0].version`, `body` for oneOf), the order of checks of D51, or the decided relaxations (D19); mapping its messages would be more code than the checks |
| Date and time | `java.time` | none |
| Problem details | own `ProblemWriter` and one exception class (D104) | Spring's `ProblemDetail` cannot be written from servlet filters (401, 405, 406, 415 come before Spring MVC) and has no `issues`; about 60 lines |
| Resilience (circuit breaker) | Resilience4j 2.4.0 circuit breaker and Micrometer binder (D103) | Bulkhead for uploads is a JDK `Semaphore` (`UploadLimiter`, 30 lines) |
| Retries | own: at most 2 read retries after 200 and 400 ms (D44), conditional write retries (D82) | Short and decided exactly; Resilience4j Retry would need a custom predicate per call kind |
| Metrics | Micrometer 1.17 with Prometheus registry, Spring Boot Actuator | none |
| Tracing and correlation | Micrometer Tracing 1.7 with the Brave bridge, W3C propagation (D86, D94) | `CorrelationFilter` (X-Correlation-Id) and `TraceHeaders` (forwarding), about 70 lines |
| Logging | Spring Boot structured logging (Logstash JSON layout), Logback | D105: the Simpl LogWrapper is not in the build input |
| Persistence of uploaded bytes | in the RDF store as `xsd:base64Binary` (D106) | object storage not used (D106) |
| Messaging | none (D45, pull only) | none |
| UI framework | Vue 3.5, vue-router 4.6, Vite 8, TypeScript 5.9 | catalogue: participant-fe-components (PrimeVue unstyled, Tailwind) is not available to the factory (E6); the build profile asks for a plain component library, so the components are plain Vue |
| UI login | keycloak-js 26.2 (PKCE) | none |
| UI Turtle check | N3.js 2.13 (D13) | none |
| UI tests | Vitest 5, Vue Test Utils 2.6, jsdom | none |
| Reverse proxy, static files | nginx (nginxinc/nginx-unprivileged, alpine-slim) | none |
| Backup and restore | Fuseki graph store read (N-Quads) and TDB2 `tdbloader` and `tdbquery` from the Fuseki jar | Scripts only (`ops/*.sh`) |
| Formatting and lint | Spotless with Palantir Java Format (CODE-01), ESLint, Prettier (D95) | none |
| Coverage, SBOM | JaCoCo 0.8.16, CycloneDX Maven plugin (through the Spring Boot parent), @cyclonedx/cyclonedx-npm | none |

Every reused library had a release in the last 12 months (REUSE-08).

## Named patterns (REUSE-06)

| Pattern | Where |
|---|---|
| Layered architecture (controller, service, repository) and Repository (DAO, CODE-47) | `web`, `service`, `store` packages |
| Optimistic concurrency with a conditional update (compare and set) | `VocabularyRepository.addVersion`, `setStatus`, `setVersionStatus`, create with NOT EXISTS (D82) |
| Idempotency check through a write token | creation and status write ids checked after each update (`writeApplied`) |
| Retry with backoff (bounded) | `SparqlClient.readWithRetry`, conditional write loop in `VocabularyService` |
| Circuit breaker | Resilience4j around store calls and key fetch (D103) |
| Bulkhead | `UploadLimiter` (D92) |
| Timeouts and fail fast; graceful degradation | store and key timeouts; GET /health DEGRADED; cached realm keys keep working |
| Fail closed | unknown key while keys cannot be fetched gives 503, never an unchecked token (D44) |
| Default deny | route table plus `anyRequest().authenticated()` and the start-up check `RouteRuleCheck` (D84) |
| Chain of responsibility (servlet filter chain) | method guard, firewall, authentication, authorisation, negotiation (order of checks D51) |
| Problem details (RFC 9457) with one exception type and error codes | `ApiException`, `ProblemType`, `GlobalExceptionHandler` |
| Correlation identifier, distributed trace context | `CorrelationFilter`, Micrometer Tracing |
| Stateless service, shared nothing between replicas | no session, no lock state (MACH-06, D82) |
| Backend for the UI through a reverse proxy (same origin) | nginx in the `ui` image |
| Health check endpoint | GET /v1/health, actuator probes on the management port |
