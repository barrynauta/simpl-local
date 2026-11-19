# Reuse decisions (REUSE-07)

Catalogue: `input/reuse-catalogue.yaml`. Everything the component needs that the catalogue or a mature library covers is reused; nothing security-related or format-parsing is written by hand (REUSE-01 to REUSE-03).

| Capability | Reused | Version | Notes |
|---|---|---|---|
| Identity provider, login, tokens, roles | Keycloak (catalogue `identity-provider`), realm imported at start | 26.8.2 | The Simpl-Open custom authenticator plugin is not added: the build uses plain username and password users of decision E1. |
| Token validation (signature, issuer, audience, expiry) | Spring Security OAuth2 Resource Server with Nimbus JOSE + JWT | Spring Boot 4.1.2 BOM | Only the key source is ours (`RealmKeySource`, a `JWKSource` extension point) to meet D44 (cache, refetch on unknown kid, fail closed with 503). Key parsing and signature checks stay in Nimbus. |
| Tier 1 gateway authorisation | Not reused in this build | n/a | Catalogue `api-gateway-authorisation`. D17 makes the service check roles itself so the rules are testable without a gateway; in production the gateway applies them first. |
| UI login | keycloak-js (authorization code with PKCE) | 26.2.5 | |
| RDF store | Apache Jena Fuseki (catalogue `rdf-store`), TDB2 | 6.2.0 | Server jar from Maven Central, SHA-256 checked in the Dockerfile. |
| SPARQL query building | Jena ARQ `ParameterizedSparqlString` | 6.2.0 | No string concatenation of request values (OWASP-04). |
| HTTP client to the store | JDK `java.net.http.HttpClient` with the SPARQL 1.1 protocol | Java 21 | `simpl-http-client` (catalogue) targets Tier 2 mutual TLS; the store call is internal plain HTTP inside the stack, so the JDK client is enough. |
| Turtle parsing (server) | Jena RIOT | 6.2.0 | Syntax errors with line and column come from RIOT. |
| Turtle parsing (browser) | n3 | 2.13.12 | D13 syntax check. |
| Semantic rules S1 to S6 | Built here on Jena's graph API | n/a | No library implements the decided rules; they are small graph queries. OWL reasoners were not used: D10 reserves `semanticReasoningSkipped`. |
| Problem details | Own small writer in the RFC 9457 shape with Belgif types | n/a | Spring's `ProblemDetail` cannot carry the Belgif `issues` array and `href` without extension; a 60-line writer keeps one shape for filters, controllers and the Tomcat valve. |
| Request validation, multipart | Spring MVC multipart (Tomcat), Jackson | Spring Boot 4.1.2 | Field and parameter rules (D37, D52) are explicit checks because the decided rules go beyond the OpenAPI file (D39). |
| Logging | Spring Boot structured logging, ECS format | Spring Boot 4.1.2 | |
| Metrics | Micrometer with Prometheus registry, Spring Boot Actuator | Spring Boot 4.1.2 BOM | |
| Health and probes | Spring Boot Actuator probes plus `GET /v1/health` (D44) | Spring Boot 4.1.2 | |
| Messaging | Not used | n/a | D45: pull only, no events. Kafka (catalogue) not needed. |
| Relational store, object storage, secrets store | Not used | n/a | No relational data or files outside the RDF store. Secrets: see DEVIATIONS.md (OpenBao not in the local stack). |
| UI framework | Vue 3, vue-router, Vite, TypeScript | 3.5.45, 4.6.5, 8.3.8, 5.9.3 | Plain components, no component library (build profile). |
| UI web server and API proxy | nginx (nginx-unprivileged image) | alpine, pinned by digest | |
| Formatting | Spotless with Palantir Java format; Prettier for Vue and TypeScript | 3.11.0 / 2.103.0; 3.10.1 | CODE-01, CODE-05 |
| Coverage | JaCoCo; Vitest V8 coverage | 0.8.16; 5.1.1 | |
| SBOM | CycloneDX Maven plugin; `npm sbom` | 2.9.3; npm 10 | `sbom/` |
| Test store | Embedded Fuseki (`jena-fuseki-main`, test scope) | 6.2.0 | Integration tests run against a real SPARQL server. |

## Named patterns (REUSE-06)

| Pattern | Where |
|---|---|
| Ports and adapters (repository port) | `store/VocabularyStore` (port), `store/SparqlVocabularyStore` (adapter); `support/InMemoryStore` in tests |
| Data transfer object / assembler | `api/Representations` builds the wire objects from domain records |
| Chain of responsibility (servlet filters) | `web/CorrelationIdFilter`, security chains, `web/MethodNotAllowedFilter` |
| Lock striping | `service/VocabularyLocks` (256 fair locks keyed by lower-case name, D43) |
| Retry with exponential backoff (reads only) | `store/SparqlClient` (200 ms, 400 ms, D44) |
| Timeout and fail fast | `store/SparqlClient`, `security/RealmKeySource` |
| Cache-aside with fail closed | `security/RealmKeySource` |
| Problem details (RFC 9457) | `problem/*` |
| Health check API | `api/HealthController`, Actuator probes |
| Correlation identifier | `web/CorrelationIdFilter` (`X-Correlation-Id`, in every log line through MDC) |
| Reverse proxy / same-origin API access | `ui/nginx/default.conf.template` |
| Unit of work (one transaction per request) | one SPARQL update per write in `store/SparqlVocabularyStore` |
