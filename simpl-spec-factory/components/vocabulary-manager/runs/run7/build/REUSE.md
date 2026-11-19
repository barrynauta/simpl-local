# Reuse record (REUSE-07)

One row per capability the component needs. Reuse comes first (REUSE-01 to REUSE-05); where something is built, the reason is given.

## Capabilities

| Capability | Reused (with version) | Built here, and why |
|---|---|---|
| Identity provider, login, tokens, roles | Keycloak 26.8.2 (catalogue: identity-provider), realm imported at start (E1) | Only the realm file. The Simpl-Open custom authenticator plugin is not used: not available to the factory and not needed for password and code flows. |
| Token validation (signature, issuer, audience, expiry) | Spring Security 7.1.2 OAuth2 resource server with Nimbus JOSE + JWT 10.9.1 | `RealmKeySource` only caches the key set and decides when to refetch (D44: cached keys while Keycloak is down, fail closed on an unknown key). No cryptography is written here (REUSE-01). |
| API gateway authorisation | Not run: the Tier 1 gateway (Spring Cloud Gateway) is outside this build | The service checks roles itself per operation (D17), from one route table. |
| Secrets | Environment variables (MACH-07); OpenBao in production (E6) | Nothing built; the stack holds no secrets other than the test users of E1. |
| Web framework, HTTP server | Spring Boot 4.1.2, Spring MVC 7.0.10, Tomcat 11.0.27 | Controllers, filters and the problem mapping. |
| RDF store | Apache Jena Fuseki 6.2.0 (catalogue: rdf-store), TDB2 dataset 'vocabulary' (E3) | Docker image built from the official Maven Central jar, checksum pinned. |
| SPARQL protocol client | JDK `java.net.http.HttpClient` (Java 21) plus Jena 6.2.0 `ResultSetMgr` for SPARQL JSON results | A thin client (`SparqlClient`) because D44 and D71 need exact per-call timeouts and a retry policy that differs for reads and writes. |
| Query building, injection safety | Jena `NodeFmtLib` formats every IRI and literal written into SPARQL (OWT-A05) | No string concatenation of request values; names are also pattern-checked before reaching the store. |
| Turtle parsing (server) | Apache Jena RIOT 6.2.0, strict mode | Only the rules S1 to S7 (D9, D12) are code of this component. |
| Turtle parsing (browser) | N3.js 2.13.12 (D13) | None. |
| JSON | Jackson 3.1.7 (from Spring Boot) with strict duplicate detection | Request bodies are read as trees and checked field by field, never bound to objects (OWT-A08). |
| Multipart parsing | Apache Commons FileUpload 2.0.0-M5 (streaming API) | A small subclass keeps parts with an empty name, which D70 requires to be reported; Tomcat and FileUpload both drop them by default. FileUpload 2 has no GA release yet; M5 is the current release and has no known HIGH or CRITICAL finding. |
| Problem details | RFC 9457 format in the Belgif profile; own `Problem` record | Built as a record because the Belgif `issues` and `href` members and the decided type URNs (D48) are not covered by Spring's `ProblemDetail` without the same amount of code. |
| Health, liveness, readiness | Spring Boot Actuator on a separate management port (D67) | `GET /v1/health` with UP, DEGRADED, DOWN (D44) is a small controller because the contract fixes its body. |
| Logging | Logback 1.5.38 with Spring Boot structured logging (ECS JSON) | Correlation id filter (MSV-09). |
| Firewall for odd requests | Spring Security `StrictHttpFirewall` | Wired before authentication to meet the order of D67. |
| Concurrency control | `java.util.concurrent.locks.ReentrantLock` (fair) | `NameLocks`: one lock per vocabulary name with bounded wait (D43, D71). |
| Messaging | Not needed: pull only (D45) | None. |
| Relational store, object storage, connector, http-client library | Not needed (E6) | None. |
| UI framework | Vue 3.5.45, Vue Router 4.6.5, Vite 8.3.8, TypeScript 5.9.3 | The catalogue's participant-fe-components (PrimeVue unstyled, TailwindCSS) is not available to the factory (E6); plain semantic HTML and one CSS file instead. See DEVIATIONS.md. |
| UI login | keycloak-js 26.2.5, authorization code flow with PKCE | None. |
| Web server and API proxy for the UI | nginx (nginx-unprivileged, Alpine) | Configuration only, security headers and CSP. |
| Accessibility check | axe-core 4.14.1 (dev only) | None. |
| Backup and restore | Fuseki Graph Store Protocol (dataset GET and PUT, N-Quads) | Shell scripts for the procedure of Q3. |
| SBOM | CycloneDX Maven plugin (via Spring Boot parent), `npm sbom` | None. |
| Code formatting | Spotless 3.11.0 with Palantir Java Format 2.103.0 (CODE-01); Prettier 3.10.1 (CODE-05) | None. |

## Named patterns used (REUSE-06)

| Pattern | Where |
|---|---|
| Microservice with its own store (MSV-02) | The backend is the only client of its Fuseki dataset. |
| Stateless design | No server sessions; every request carries its bearer token; state lives in the store. |
| Least privilege | Two realm roles; the role per operation is in `RouteTable`; containers run as non-root with all capabilities dropped and read-only file systems where possible. |
| Defence in depth | Gateway in production, plus token and role checks in the service, firewall, input validation, CSP in the UI. |
| Zero trust | Every call is authenticated and authorised in the service, also behind the gateway (D17). |
| Retries (with backoff) | Store reads: two retries after 200 ms and 400 ms (`SparqlClient`, D44). Writes are not retried. |
| Graceful degradation | Cached realm keys keep the API working while Keycloak is down; health reports DEGRADED (D44). |
| Circuit breaker | Not used: the bounded timeouts and the small retry budget of D44 already bound the cost of a down store; a breaker would change the decided behaviour (every call answers 503 after its own timeout). |
| Ports and adapters | `VocabularyStore` interface with the Fuseki adapter; services depend on the interface (CODE-11). |
| Single-writer lock per aggregate | `NameLocks` per vocabulary name (D43). |
