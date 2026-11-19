# Reuse decisions (REUSE-07)

Reuse catalogue checked: `input/reuse-catalogue.yaml` (REUSE-05). Choices of step 30 in E6. Versions are those pinned
in `backend/pom.xml` (Spring Boot 4.1.2 manages the Spring, Tomcat, Jackson and Micrometer versions),
`ui/package.json` and `compose.yaml`.

## Per capability

| Capability | Reused | Version | Built here, and why |
|---|---|---|---|
| Identity provider, user login, roles | Keycloak (catalogue: identity-provider), realms imported at start | 26.8.2 (image pinned by digest) | Realm files only (`keycloak/`). The Simpl-Open custom authenticator plugin is not available to the factory; plain Keycloak login is used (E1). |
| Token validation (JWT signature, issuer, audience, expiry) | Nimbus JOSE + JWT (`DefaultJWTProcessor`, `JWSVerificationKeySelector`, JWK set parsing) | 10.10 | Only the glue: `TokenValidator` configures the processor; `RealmKeySource` fetches the JWKS with the JDK HttpClient so the 2 s timeout, cache and fail-closed rule of D44 hold (REUSE-01). |
| Authorisation filter chain, firewall, security headers | Spring Security (`SecurityFilterChain`, `StrictHttpFirewall`, `AuthorizationManager`) | 7.1.2 | The rule table (`OperationCatalogue`) is ours, because the rules come from D16 per contract operation. |
| Gateway authorisation | Not reused in the build: Spring Cloud Gateway (catalogue: api-gateway-authorisation) | n/a | In production the Tier 1 gateway applies the rules first; the build checks tokens and roles itself so the rules are testable without a gateway (D17, E6). |
| RDF store | Apache Jena Fuseki (catalogue: rdf-store), TDB2 | 6.2.0 (official jar from Maven Central, SHA-512 checked, on a digest-pinned Temurin JRE) | Configuration only (`fuseki/config.ttl`, users from the environment, D83) (REUSE-03). |
| Turtle parsing and validation input | Apache Jena RIOT (`RDFParser`, Turtle, base IRI D57) | 6.2.0 | Semantic rules S1 to S7 are domain rules of D9, written over the parsed triples (REUSE-02). |
| SPARQL protocol client | JDK `java.net.http.HttpClient` plus Jena `ResultSetMgr` (SPARQL JSON results) and `FmtUtils` (term escaping) | JDK 21, Jena 6.2.0 | The thin client `HttpSparqlStore` is ours because D44 and D71 need separate connect, read and update timeouts, read-only retries and two users; Jena's `RDFConnection` does not give per-call budgets of that form. |
| JSON | Jackson 3 (`tools.jackson`) through Spring | 3.1.7 | `JsonBody` checks unknown properties and types on the Jackson tree (D52). |
| Multipart | Tomcat's bundled Apache Commons FileUpload stream parser | Tomcat 11.0.27 | `MultipartForm` drives the stream parser itself because the servlet part API drops parts with an empty name, which D70 must report. |
| URIs, dates, hashes | `java.net.URI` (RFC 3986), `java.time` (ISO 8601), `MessageDigest` SHA-256 | JDK 21 | none |
| Problem details (RFC 9457) | Problem shape of the OpenAPI and the Belgif problem types | n/a | `Problems` and `ProblemWriter` build the bodies with Jackson; Spring's `ProblemDetail` is not used because the Belgif `issues` array and the fixed types of D48 are needed in filters too (REUSE-04). |
| Web framework, HTTP server | Spring Boot (Spring MVC, embedded Tomcat), graceful shutdown | 4.1.2 / Tomcat 11.0.27 | none |
| Metrics | Micrometer with the Prometheus registry, Spring Boot Actuator | 1.17.2 | none (D86) |
| Health probes | Spring Boot Actuator liveness and readiness on the management port | 4.1.2 | `GET /v1/health` is a contract operation (D44) and is ours. |
| Logging | Spring Boot structured logging, ECS format, SLF4J and Logback | 4.1.2 | `CorrelationFilter` adds trace ids to the MDC (D86). |
| Trace context | W3C `traceparent` format and `X-Correlation-Id` | n/a | Propagation in `TraceContext` (no tracing backend in the build, D86). |
| Secrets | Environment variables from a generated `.env` (local) | n/a | OpenBao (catalogue: secrets-and-keys) is not part of this build; the deployment injects the same variables from OpenBao (E6). |
| HTTP client with Simpl TLS checks | Not reused: simpl-http-client | n/a | The only outgoing calls are to the store and to Keycloak inside the deployment network, plain HTTP in this stack; no participant TLS (E6). |
| UI framework | Vue 3, Vue Router, Vite, TypeScript | vue 3.5.45, vue-router 4.6.5, vite 8.3.8, typescript 5.9.3 | Plain components and CSS. participant-fe-components (PrimeVue unstyled, TailwindCSS) is not available to the factory (E6); the UI follows its stack where it can. |
| UI login | keycloak-js (OIDC code flow with PKCE) | 26.2.5 | none |
| Turtle syntax check in the browser | N3.js `Parser` | 2.13.12 | `turtleCheck.ts` maps its error to line and column (D13, D60). |
| Static serving and API proxy | nginx (nginx-unprivileged, alpine) | 1.31.7 | Configuration only (`ui/nginx/default.conf.template`). |
| Backup and restore | Fuseki Graph Store Protocol (dataset GET and PUT, N-Quads), curl in an own helper image | curl from Alpine 3.24.4 (digest-pinned base, `ops/Dockerfile`) | Scripts `ops/backup.sh`, `ops/restore.sh` (Q3). |
| SBOM | CycloneDX Maven plugin | 2.9.3 | none |
| Coverage | JaCoCo | 0.8.16 | none |
| Physical model checks | Apache Jena SHACL (tests) and SHACL Core shapes | 6.2.0 | Shapes in `docs/physical-model.shacl.ttl` (D87). |
| Relational store, messaging, object storage, connector, graph index, ticketing | Not needed | n/a | No relational data; pull only, no events (D45); no file exchange between participants; no catalogue index (E6). |

## Named patterns (REUSE-06)

| Pattern | Where |
|---|---|
| Microservice, stateless service | backend: no session, no in-process lock (D82) |
| Default deny, least privilege | `OperationCatalogue`, `SecurityConfig.decide`, `HandlerRuleVerifier` (D84); three store users (D83) |
| Defence in depth, zero trust | gateway in production plus own token checks; store on an internal network with authentication |
| Chain of responsibility | servlet filter chain and `RequestShapeInterceptor` in the order of D67, D51, D63 |
| Optimistic concurrency (conditional update with retry) | `ConditionalWrite`, `VocabularyRepository.insertVersion` and the status updates (D82) |
| Retry with backoff and timeouts | `HttpSparqlStore.readWithRetry` (D44) |
| Fail closed and graceful degradation | `RealmKeySource`, `TokenAuthenticationFilter` (503), health DEGRADED (D44) |
| Cache-aside | `RealmKeySource` (realm keys) |
| Repository | `VocabularyRepository`, `ExternalVocabularyRepository` |
| Problem details | `Problems`, `ProblemWriter`, `ApiExceptionHandler` |
| Correlation identifier | `CorrelationFilter`, `TraceContext` |
| Same-origin reverse proxy | nginx of `ui/` proxies `/v1` |
| Backup, verify, swap | `ops/restore.sh`: staging dataset, count check, then replace |
