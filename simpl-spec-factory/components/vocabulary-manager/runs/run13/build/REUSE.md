# Reuse decisions (REUSE-07)

Checked against `input/reuse-catalogue.yaml` first (REUSE-05). Versions are the ones pinned in `backend/pom.xml`, `ui/package.json` and the Dockerfiles.

## Per capability

| Capability | Reused | Version | Built instead, and why |
|---|---|---|---|
| Identity provider (catalogue: identity-provider) | Keycloak, realms imported at start (E1, E7) | 26.8.2 (image pinned by digest) | Realm files only. The Simpl custom authenticator plugin is not available to the factory. |
| API gateway authorisation (catalogue) | Not deployed in the local stack | n/a | D17: the service checks token and roles itself so the rules are testable without a gateway. In production the Tier 1 Spring Cloud Gateway applies them first. |
| Token validation, cryptography | Nimbus JOSE + JWT: `SignedJWT`, `DefaultJWSVerifierFactory`, `JWKSet`, `DefaultResourceRetriever`, `DefaultJWTClaimsVerifier` | 10.10 | Only the fetch timing of D88 and D129 (`RealmKeyProvider`, about 150 lines) is own code, as D108 and D132 decide. Spring Security resource server was not used: its filter answers 401 for an unreachable key source and it cannot put 405 before 401 or 406 after 403 (D67, D139, D63) without as much glue code. |
| Request firewall (encoded slash, control characters) | Spring Security `StrictHttpFirewall` | 7.1.3 | Used on its own inside the request gate; no Spring Security filter chain. |
| Request routing, content negotiation, 415 | Spring MVC (`@GetMapping`, `consumes`, `produces`) | Spring Boot 4.1.3, Spring 7.0.11 | D133. The Accept rule of D63 sits before routing in `AcceptCheck` (about 50 lines) because Spring checks 415 before 406. |
| Request validation (D93 choice) | Own code: `RequestRules`, `QueryParameters`, `JsonBody`, `MultipartForm` | n/a | D93 allows own code plus a test against `docs/openapi-decided.yaml`. `ContractConformanceTest` compares every operation, role, pattern and limit. A generic OpenAPI validator (for example swagger-request-validator) could have been reused, but it does not produce the decided issue names (`items[1].name`, D68), the one-issue rules for repeated parameters (D138) or the D51 order between 413, 400 and 404, so own rules were simpler to make exact. |
| Query string parsing | Spring `UriComponentsBuilder`, `UriUtils` | Spring 7.0.11 | Only the repeated and unknown parameter rules are own code. |
| JSON | Jackson 3 (`JsonMapper`, tree model, strict duplicate detection) | 3.1.8 | None. |
| Multipart | Tomcat multipart parser through the Servlet API (`getParts`), limits from `spring.servlet.multipart.*` | Tomcat 11.0.27 | No own multipart parser (D121). |
| RDF and Turtle parsing | Apache Jena RIOT (`RDFParser`, `StreamRDF`) | 6.2.0 | None. The semantic rules S1 to S7 (`SemanticRules`, `ContentValidator`, about 300 lines) are own domain code: no library implements these decided rules. |
| SPARQL query and update building | Jena `ParameterizedSparqlString`, `UpdateModify` with quad templates, `QueryTransformOps.replaceVars`, `ElementData` for VALUES | 6.2.0 | No values are joined into query text (D94). Only fixed structure text (which filter, which sort) is chosen by code. |
| SPARQL protocol client | JDK `java.net.http.HttpClient` plus Jena `ResultSetMgr` for result parsing | JDK 21, Jena 6.2.0 | `SparqlStoreClient` (about 200 lines) is own HTTP plumbing. Jena `RDFConnectionRemote` could have been reused; it was not because the decided timeouts per call type (2 s connect, 10 s read, 45 s update, 2 s health), Basic credentials per user (D83) and trace header forwarding (D86) are explicit with the JDK client, while Jena's builder does not expose a per-request timeout for updates. |
| RDF store (catalogue: rdf-store) | Apache Jena Fuseki, TDB2 dataset 'vocabulary' (E3) | 6.2.0 (jar verified by SHA-512) | Image built from `eclipse-temurin:21-jre-alpine` because no maintained official Fuseki image exists. |
| Content storage | Fuseki literal per version (D106) | n/a | Object storage (catalogue: MinIO) not used, as D106 decides. |
| Relational store, messaging, object storage, connector, http-client (catalogue) | Not used | n/a | E6: not needed. Pull only (D45). The only outgoing calls are to the store and to the realm keys, both inside the deployment, so simpl-http-client (mTLS between participants) does not apply. |
| Circuit breaker, retry, bulkhead | Resilience4j `CircuitBreaker`, `Retry`, `Bulkhead`, `TaggedCircuitBreakerMetrics` | 2.4.0 | Conditional write retry `WriteRetry` (about 60 lines) is own code: its backoff and budget are decided (D157, D71) and it re-reads state between attempts. |
| Metrics | Micrometer with Prometheus registry, Spring Boot Actuator on a separate management port | Micrometer 1.17.3 | `OperationObservationConvention` adds the operationId tag. |
| Trace context, correlation id | Micrometer Tracing with the Brave bridge, W3C propagation (D86, D94) | 1.7.3 | `TraceHeaders` injects the current context with the Micrometer `Propagator`. The X-Correlation-Id is read or created in the request gate (a header, no library covers it). |
| Structured logs | Spring Boot structured logging, Logstash JSON format | Boot 4.1.3 | Simpl LogWrapper out of scope (D105). |
| Error format | RFC 9457 problem details, Belgif problem types (D48) | n/a | One exception base class and one handler (D104). Tomcat error valve subclass `ProblemErrorReportValve` for container answers (D158). |
| Secrets | Environment variables, generated into `.env` by `stack.sh` (D131) | n/a | OpenBao in production (E6). |
| UI framework (catalogue: ui-components) | Vue 3, PrimeVue (unstyled), TailwindCSS, keycloak-js, n3 (Turtle in the browser) | Vue 3.5.46, PrimeVue 4.5.5, Tailwind 4.3.5, keycloak-js 26.2.5, n3 2.13.12 | participant-fe-components itself is not available to the factory (D167). PrimeVue 4 rather than 5: version 5 adds a licence manager dependency. |
| Reverse proxy | nginx (unprivileged image) | 1.29 (pinned by digest) | None. |
| Formatting and lint | Spotless with Palantir Java Format; ESLint and Prettier | Spotless 3.12.0, Palantir 2.103.0 | None. |
| Test infrastructure | JUnit 5, AssertJ, Spring Boot test, embedded Fuseki (`jena-fuseki-main`), Jena SHACL, JDK HttpServer as JWKS endpoint, Vitest | as in the build files | The JWKS test server (`TestKeys`) is a few lines of test code. |
| Coverage, SBOM, scan | JaCoCo, CycloneDX Maven plugin, Trivy | 0.8.16, 2.9.3, 0.76.2 | None. |

## Named patterns (REUSE-06)

| Pattern | Where |
|---|---|
| Layered architecture (controller, service, repository) | `api/`, `service/`, `store/` packages |
| Repository (DAO, CODE-47) | `VocabularyRepository`, `ExternalVocabularyRepository` |
| Optimistic concurrency with conditional update (compare and set) | `VocabularyRepository#insertVersion`, `#setStatus`, `#insertVocabulary` (D82) |
| Idempotency token to detect the own write | `writeToken`, `statusToken` checked after each conditional update |
| Retry with exponential backoff and jitter | `WriteRetry` (D157), Resilience4j read retry (D44) |
| Circuit breaker | Resilience4j around store calls and key fetches (D103) |
| Bulkhead | Resilience4j semaphore bulkhead on uploads (D92) |
| Fail closed | `TokenVerifier` (503 when keys cannot be fetched, never unsigned), default deny in `OperationCatalog` and `HandlerRuleCheck` (D84) |
| Front controller / intercepting filter | `RequestGateFilter` (405, firewall, 401, 403, 406 in the decided order) |
| Health check (liveness and readiness) | Actuator liveness on the management port (container), `GET /v1/health` readiness (D44, D152) |
| Stateless service, scale out | No session state; two backend replicas behind nginx (D102) |
| Problem details (RFC 9457) | `error/` package |
| Correlation identifier | `X-Correlation-Id` plus W3C traceparent (D86) |
| Graceful degradation | Cached realm keys keep working while Keycloak is down (D44) |
| Defence in depth, least privilege | Store users per access (reader, writer, admin), store on an internal network, Keycloak on 127.0.0.1, containers as non-root |
