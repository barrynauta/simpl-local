# Reuse decisions (REUSE-05, REUSE-06, REUSE-07, D93, D94)

## Per capability

| Capability | Reused (version) | Built here, and why |
|---|---|---|
| Identity provider, login, tokens, roles | Keycloak 26.8.2 (catalogue: identity-provider), OpenID Connect, realm roles (E1) | Realm import files only |
| Gateway authorisation | Not in this build: the Tier 1 gateway (Spring Cloud Gateway) runs in production (D17) | The service checks tokens and roles itself so the rules are testable without a gateway |
| Token validation | Nimbus JOSE+JWT 10.10: `DefaultJWTProcessor`, `JWSVerificationKeySelector`, `DefaultJWTClaimsVerifier`, `JWKSet.parse` (D108, D132) | `RealmKeyCache` (about 150 lines): only the fetch timing of D88, D129 and D174, which no library offers (Nimbus `JWKSourceBuilder` has its own refresh and outage rules that differ from D88) |
| Secrets | Environment variables locally, OpenBao in production (catalogue: secrets-and-keys) | `stack.sh` generates local random secrets |
| RDF store | Apache Jena Fuseki 6.3.0, TDB2 (catalogue: rdf-store, E3) | Image built from the Maven Central jar, checked by SHA-512 |
| SPARQL building | Apache Jena 6.3.0: `NodeFmtLib` writes every value as an escaped RDF term | `SparqlTemplate` (about 80 lines): one-pass substitution of `?variables`. Jena's `ParameterizedSparqlString` (named in D94) throws an exception for any literal that contains text like `?now` equal to a variable name, which a description may contain (proven by `SparqlInjectionTest`); that would be a 500. Jena's query builder does not cover SPARQL Update with multiple operations in one request. |
| SPARQL over HTTP | Java `HttpClient`, Jena `ResultSetMgr` for SPARQL JSON results | `StoreClient` (about 250 lines): per-call timeouts, preemptive basic auth per user, trace header forwarding, retry and breaker classification of D44, D134, D201. Jena's `QueryExecutionHTTP` was not used because the D134/D201 classification of store answers (5xx counts, 4xx does not, 401/403 is 500) needs the raw status. |
| Turtle parsing (server) | Jena RIOT (parser, tokenizer, `LabelToNode`) | Rules S1 to S7 (`SemanticRules`, `References`, `ContentValidator`): domain rules of D9 to D12, no library implements them |
| SHACL | Jena SHACL 6.3.0 (tests) | Shapes in `docs/shapes.ttl` |
| Turtle parsing (browser) | N3.js 2.13.12 | |
| Web framework, routing, multipart | Spring Boot 4.1.3, Spring MVC (mappings with produces and consumes, D133), Tomcat 11 multipart parsing (no own multipart parser, D121) | `GatekeeperFilter` (about 250 lines): the decided order of 405, firewall 400, 401/404, 403, 406, 415, 413, 503 that no framework filter gives in this order (Spring Security's order differs on 405 before 401 and on unknown paths) |
| Request validation (D93) | Choice: own code plus a build test. `RequestRules`, `QueryParameters`, `MultipartForm`, `JsonBody` (together about 450 lines) | `ContractTest` compares the operation table, the Spring handlers, the proxy Allow map, the role rules and the main limits with `openapi-decided.yaml`. An OpenAPI request validator (for example swagger-request-validator) was not used: it cannot express D21/D52/D138 (issue per unknown or repeated parameter with our names), D168 (`+` is a plus) or the order of checks. `QueryParameters` exists because the servlet container decodes `+` as a space (D168). |
| JSON | Jackson 3 (Spring Boot managed) with strict duplicate detection | |
| Problem details | RFC 9457 format (REUSE-04) with the Belgif issue extension | One exception class and one handler (D104); a Tomcat `ErrorReportValve` subclass for container errors (D158) |
| Resilience | Resilience4j 2.4.0 circuit breaker and Micrometer binding (D103) | Read retries (2 retries, 200/400 ms) and the conditional-write retry (`ConditionalWrite`, D157) are small loops |
| Metrics | Micrometer, Prometheus registry, Spring Boot Actuator on a separate port (D67, D86) | |
| Tracing and correlation | Micrometer Tracing with the Brave bridge, W3C propagation (D94) | Correlation id header handling in the gatekeeper |
| Logging | Spring Boot structured logging (logstash JSON), SLF4J and Logback (D96) | Simpl LogWrapper not used (D105) |
| Messaging | Not needed: pull only (D45) | |
| Object storage, relational store | Not needed: content in the RDF store (D106) | |
| Front proxy | nginx (nginx-unprivileged 1.31.7) | Config only |
| UI framework | Vue 3.5, vue-router 5, Vite 8, TypeScript 5.9, PrimeVue 5 unstyled (Dialog), TailwindCSS 4 (E6, D167); keycloak-js 26.2 | Views and a small accessible actions menu; participant-fe-components is not available to the factory (D167) |
| Tests | JUnit 5, AssertJ, Spring Boot test, embedded Fuseki (jena-fuseki-main), JDK HttpServer as test key server; Vitest, Vue Test Utils, jsdom | |
| Build quality | Spotless with Palantir Java Format 2.103.0, JaCoCo 0.8.16, CycloneDX Maven plugin 2.9.3; ESLint 10, Prettier 3 | |

## Named patterns used (REUSE-06, catalogue: architectural-patterns)

| Pattern | Where |
|---|---|
| Microservices, own data store | backend owns the Fuseki dataset; others use the API (MSV-02) |
| Stateless design | backend keeps no lock or session state; conditional store updates (D82) allow two replicas |
| Retries with backoff | `StoreClient.withReadRetries`, `ConditionalWrite` |
| Circuit breaker | `StoreClient`, `RealmKeyCache` (Resilience4j) |
| Graceful degradation | health DEGRADED, cached keys keep working while Keycloak is down |
| Least privilege | store users read/update/admin, non-root containers, read-only file systems, dropped capabilities |
| Defence in depth | proxy limits, service limits, store on an internal network, token checks in the service |
| Zero trust | the service validates every token itself even behind the gateway (D17) |
| Default deny | operation table, start-time check (D84) |
