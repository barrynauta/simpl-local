# Design review

Build: `simpl-open-vocabulary-manager-run7`. Rules reviewed: the 153 rules in `rules.json` with `check: review` and step 70. I formed each verdict from the code first and read `REUSE.md`, `QUALITY.md` and `DEVIATIONS.md` only afterwards.

## 1. In short

**Verdicts per level**

| Level | met | partly | not met | not applicable |
|---|---|---|---|---|
| MUST (52) | 23 | 21 | 6 | 2 |
| SHOULD (101) | 38 | 23 | 18 | 22 |

**Verdicts per prefix** (met / partly / not met / n.a.)

DDD 2/0/0/0 · MACH 3/3/0/0 · MSV 2/2/0/1 · OWT 1/5/0/0 · OWA 0/1/0/0 · REUSE 4/4/0/0 · NFR 3/3/0/0 · SOA 0/1/0/0 · API 13/3/2/3 · CODE 15/6/4/0 · ARCH 13/7/3/2 · DATA 1/1/13/13 · SEC 2/5/0/2 · OPS 2/0/1/3 · OWASP 0/3/0/0 · UX 0/0/1/0

**The five most important findings**

1. **The role check fails open when a path is missing from the route table.**
   - Spring Security only requires an authenticated user (`SecurityConfig.java`, `apiChain`, `anyRequest().authenticated()`).
   - Roles are checked in an own `RequestGateFilter`, which skips the check when `EntryFilter` found no `RouteTable` match (`RequestGateFilter.java:47`, `EntryFilter.java:93`).
   - Today every controller mapping is in the table. But any future mapping that is left out of it would be open to a user with no role, such as `ga-norole`.
   - Nothing in the main code keeps the table and the controller mappings in step.
2. **The store has no authentication.**
   - Fuseki runs with `--update` and no credentials (`fuseki/Dockerfile`, ENTRYPOINT).
   - Any container on the compose network, and anyone on host loopback port 3141, can read or write it, admin endpoints included.
   - There is no TLS on that link. This contradicts the "Zero trust" and "Defence in depth" entries in `REUSE.md`.
3. **Store reads are unbounded.**
   - `listVocabularies`, `listVersions` and `listExternal` fetch every row with no `LIMIT` and page in memory (`FusekiVocabularyStore.java:72`, `VocabularyService.java:142`, `ExternalVocabularyService.java:88`).
   - `SearchService.java:81` and `ExternalVocabularyService.java:52` load every external vocabulary on each call.
   - `DEVIATIONS.md` does not list this.
4. **Observability is incomplete.**
   - There are no metrics: Actuator exposes health only (`application.yaml` `management.endpoints.web.exposure.include: health`).
   - There is no tracing, and the correlation id is not forwarded to Fuseki or Keycloak (`SparqlClient.java`, `HttpKeyFetcher.java`).
   - D67 expects metrics on the management port. `DEVIATIONS.md` does not list NFR-08, MSV-09 or MSV-10.
5. **The backend can only run as a single replica.**
   - The in-process `NameLocks` forces one replica (D81), so ARCH-07 (MUST) is not met and MACH-06 / MACH-09 are only partly met.
   - `REUSE.md` still names "Stateless design" as an applied pattern.
   - The build also delivers no updated OpenAPI for the D79 changes and no CDM, LDM or PDM.

## 2. Verdicts

### DDD

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| DDD-03 | MUST | met | `input/decisions/glossary.md` has a term, definition and CDM class per row. The build uses those terms. |
| DDD-14 | MUST | met | Model-to-API translation sits in one class, `api/ApiMapper.java`. |

### MACH

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| MACH-01 | SHOULD | met | The backend has its own image and compose service and depends only on Fuseki and Keycloak. |
| MACH-02 | MUST | met | Talks only to its own Fuseki dataset over SPARQL and to Keycloak's JWKS endpoint. |
| MACH-03 | MUST | partly | The contract exists (input OpenAPI), but the D79 changes (401/403/405/406/503, pageSize, `latestVersionHref`) were made in code without updating any contract file (`DEVIATIONS.md` row D79). |
| MACH-06 | SHOULD | partly | Sessions are stateless, but `service/NameLocks.java` keeps per-name lock state in memory (D81). |
| MACH-07 | MUST | met | All settings come from the environment (`application.yaml`, `AppProperties`). There are no secrets in code or images. |
| MACH-09 | SHOULD | partly | Containers, graceful shutdown (`server.shutdown: graceful`, `stop_grace_period: 35s`) and fast start are in place. Horizontal scaling is excluded by D81. |

### MSV

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| MSV-02 | MUST | met | Only the backend uses dataset `vocabulary`. |
| MSV-04 | MUST | not applicable | No events (D45). |
| MSV-06 | MUST | met | Store: connect 2 s, read 10 s with 2 retries, write 45 s with no retry, then 503 (`SparqlClient.java:62`, `ApiExceptionHandler`). Keycloak: 2 s, fail closed with 503 (`RealmKeySource`). |
| MSV-09 | MUST | partly | The id is in MDC and the response header (`EntryFilter.java:132`), but it is not sent to Fuseki or Keycloak (`SparqlClient.selectOnce`, `HttpKeyFetcher.fetch`). |
| MSV-10 | SHOULD | partly | ECS JSON logs are in place. There is no metrics endpoint. |

### OWT and OWA

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| OWT-A02 | MUST | partly | Good: Actuator on separate port 8081 with health only, stack traces off, sanitised problems, no CORS. Gap: Fuseki admin and update endpoints are unauthenticated on `127.0.0.1:3141` (compose, Fuseki ENTRYPOINT). |
| OWT-A04 | MUST | partly | Crypto comes from Nimbus and the JDK, and there are no secrets in code or logs. There is no TLS anywhere and no encryption at rest (recorded deviation). |
| OWT-A07 | MUST | partly | Signature, issuer, audience and expiry are checked (`SecurityConfig.tokenValidator`), and there are no credentials in URLs. There is no API rate limit (deferred to the gateway, D46). |
| OWT-A08 | MUST | met | The Fuseki jar is SHA-512 checked (`fuseki/Dockerfile`), images are pinned by digest, and JSON is read as trees only (`JsonBody.java`). |
| OWT-A09 | MUST | partly | 401 and 403 are logged at WARN. Admin changes are logged without the acting user (`VocabularyService.java:76,120,247,267`, `ExternalVocabularyService.java:75,135`). Nothing raises an alert. |
| OWT-A10 | MUST | partly | Unknown keys fail closed with 503, writes run as one transaction, and unexpected errors give a sanitised 500. But the role gate skips its check when no route matches (`RequestGateFilter.java:47`). |
| OWA-API8 | SHOULD | partly | Security headers and CSP are set in nginx and the backend (`SecurityConfig` headers, `security-headers.inc.template`), with no verbose errors. TLS-only and allowed origins are not stated in the contract or docs. |

### REUSE

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| REUSE-01 | MUST | partly | Signature checking uses Spring Security and Nimbus. Key fetching and refetch logic are hand-built (`HttpKeyFetcher`, `RealmKeySource`), where Nimbus `JWKSourceBuilder` already offers refresh on unknown kid and outage tolerance. Role authorisation is hand-built outside Spring Security (`RouteTable`, `RequestGateFilter`). |
| REUSE-02 | SHOULD | partly | Jena (Turtle, SPARQL results, term escaping), Jackson and Commons FileUpload are used. The query string is parsed by hand (`QueryParameters.java`, split on `&` plus `URLDecoder`). SPARQL text is built by concatenating `NodeFmtLib` output, not with `ParameterizedSparqlString` or the query builder. |
| REUSE-03 | SHOULD | met | Fuseki and Keycloak are products. Rate limiting and authorisation are deferred to the gateway. |
| REUSE-04 | MUST | met | HTTP and OpenAPI, OIDC bearer tokens, JSON, RFC 9457 (Belgif profile, D48). |
| REUSE-05 | SHOULD | met | E6 and `REUSE.md` record catalogue use. Reasons are given for the authenticator plugin, participant-fe-components, the gateway and simpl-http-client. |
| REUSE-06 | SHOULD | partly | Patterns are named in `REUSE.md`. "Stateless design" is named but not applied (`NameLocks`). "Zero trust" is overstated because the store is unauthenticated. The route table is a home-made routing layer that duplicates Spring MVC and has no named pattern. |
| REUSE-07 | MUST | partly | `REUSE.md` exists but leaves out the own route matching and authorisation, the own query-string parser, the own JWKS HTTP fetch and the decision not to use Micrometer. |
| REUSE-09 | SHOULD | met | Extension points are used (filters, `JWKSource`, `ErrorController`, a FileUpload subclass). No framework code is copied or patched. |

### NFR and SOA

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| NFR-05 | SHOULD | partly | Every container has `mem_limit` and `cpus`, and request and part sizes are capped. List queries are unbounded and paged in memory (`FusekiVocabularyStore.java:72`). With `spring.threads.virtual.enabled: true`, `server.tomcat.threads.max` probably no longer bounds concurrency. |
| NFR-06 | MUST | met | Q3 states availability, RTO and RPO. `ops/backup.sh` and `ops/restore.sh` (staged, count-checked) exist, and `ops/backup-restore-test.sh` passed. |
| NFR-08 | SHOULD | partly | Structured logs with correlation id are in place. There are no per-operation metrics and no trace context is passed on. |
| NFR-11 | SHOULD | met | Images plus environment variables, no host paths. The backend root filesystem is read-only. |
| NFR-12 | SHOULD | met | Q4 and `QUALITY.md` give the design load, scale-up only, and say Fuseki and the lock hold state. |
| NFR-13 | MUST | partly | Q1 is measured (`ops/load-check.py`). The JSON 1 MB limit and the 200-part limit are verified by "code review" only (`QUALITY.md` Q2), which is neither a test nor a measurement. Q4 volume (1000 × 20) is not measured, and that is stated. |
| SOA-05 | SHOULD | partly | Cached keys keep the API working while Keycloak is down. The store dependency (503) is not in the contract because the OpenAPI was not updated. |

### API

Most API rules judge the input contract `vocabulary_openapi-tier1.yaml`, which the build did not write.

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| API-01 | MUST | not met | No CDM is delivered (recorded deviation, D2). |
| API-02 | SHOULD | not met | Schemas are local (`VocabularyMetadata` and others) and do not reference a CDM. |
| API-03 | SHOULD | met | The OpenAPI is a hand-written input. No generator is in the build. |
| API-10 | SHOULD | partly | `/external-vocabulary/searches` is singular, while the siblings are plural. |
| API-11 | SHOULD | met | GET reads, POST creates or triggers a search, PATCH changes status, DELETE removes. |
| API-15 | SHOULD | met | No British spellings found in property names. |
| API-19 | SHOULD | met | Business names (`changelog`, `namespaceUri`, `aliasUris`). |
| API-22 | SHOULD | met | No `string` or `foo` placeholder examples found. |
| API-28 | SHOULD | met | Collections, documents, and the `searches` and `content` controllers. |
| API-30 | SHOULD | met | `/vocabularies/{vocabularyName}`, `/external-vocabularies/{name}`. |
| API-35 | SHOULD | met | The UI sets `Accept` on JSON calls and `text/turtle` on downloads (`ui/src/lib/api.ts`). |
| API-49 | SHOULD | met | info.description: "allows the Governance Authority to manage internal vocabularies". |
| API-50 | SHOULD | partly | The resource identifier is `name`, not `id` or `code`. This is decided and is a business key. |
| API-51 | SHOULD | met | No `...Id` reference properties. |
| API-54 | SHOULD | partly | No identifier schema type ending in `Id` or `Code`. |
| API-55 | SHOULD | met | No standard business identifier applies. |
| API-58 | SHOULD | met | `VocabularyStatus` and the sort enums are declared. |
| API-60 | SHOULD | met | Path parameters have `required: true`. |
| API-62, API-79, API-80 | MUST, SHOULD, SHOULD | not applicable | No Kafka. |

### CODE

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| CODE-06 | MUST | met | The longest method is about 52 lines (`SearchController.externalUris`), with one purpose. |
| CODE-09 | MUST | met | No merged look-alike blocks seen. |
| CODE-11 | MUST | partly | Services depend on the `VocabularyStore` port. Controllers depend on concrete service classes, and the store adapter on the concrete `SparqlClient`. |
| CODE-14, CODE-15 | MUST | met | English throughout. |
| CODE-16, CODE-17 | MUST | met | Small classes, Javadoc, clear names. |
| CODE-20 | MUST | met | Store failures are logged with their cause, unexpected errors with a stack trace, and every line carries a correlation id. |
| CODE-21 | SHOULD | not met | Uses Logback ECS (`logging.structured.format.console: ecs`), not the Log4J wrapper. Not in DEVIATIONS. |
| CODE-24, CODE-25 | MUST, SHOULD | met | ERROR is used only for store errors and unexpected failures. |
| CODE-30 | SHOULD | met | MDC correlation id, entity names in messages, service name from ECS. |
| CODE-32 | SHOULD | met | Logs use the token subject (`Roles.userId`, D80), never the email. |
| CODE-33 | SHOULD | not met | Own `ApiException` hierarchy, not the shared framework. Not in DEVIATIONS. |
| CODE-34, CODE-35, CODE-36 | MUST | met | One `@RestControllerAdvice`, plus `ProblemErrorController` and `EntryFilter` fallbacks. Framework, Jackson, FileUpload and store exceptions are mapped. Each `Problems` factory sets the user message. |
| CODE-44 | SHOULD | partly | Error types are string constants inside `Problems` factories, with no enum per category. |
| CODE-45 | SHOULD | partly | No gateway in the stack. The service checks tokens itself (D17, recorded). |
| CODE-46 | SHOULD | not met | No circuit breaker. The reason is in `REUSE.md`. |
| CODE-48 | SHOULD | met | Spring beans in `ServiceConfig` and `WebConfig`. |
| CODE-50 | SHOULD | partly | ECS JSON goes to stdout and the stack has no aggregation. |
| CODE-52 | SHOULD | not met | ECS, not the Simpl-Open common log format. Not in DEVIATIONS. |
| CODE-54 | MUST | partly | Formatting is enforced (Spotless, Prettier). CODE-21, CODE-33 and CODE-52 are not followed. |
| CODE-55 | SHOULD | partly | Formatting rules exist for Java and TS. There is no lint for the UI (no ESLint in `package.json`). |

### ARCH

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| ARCH-01 | SHOULD | met | No central ownership is assumed. |
| ARCH-02 | MUST | met | Only a pull API. No other component changes. |
| ARCH-04, ARCH-05 | SHOULD | met | Two dependencies. The API is business-shaped and hides RDF chunk storage. |
| ARCH-07 | MUST | not met | One replica forced by the in-process lock (D81, accepted risk). |
| ARCH-08, ARCH-09 | SHOULD | met | Open source products and standard SPARQL and OIDC. |
| ARCH-11 | SHOULD | met | Search and content operations are composable by consumers. |
| ARCH-13 | SHOULD | partly | The README covers consumers briefly. Nothing on participant interoperation. |
| ARCH-14 | SHOULD | met | `MaxRAMPercentage`, configurable limits and timeouts. |
| ARCH-17 | MUST | partly | Tokens and roles are required on the API. The store is open and there is no TLS (finding 2). |
| ARCH-18 | MUST | partly | Atomic single-request updates and SHA-256 per version. The store is writable without credentials. |
| ARCH-19 | MUST | partly | Backup and restore are tested. Single instance, no replica. |
| ARCH-20 | MUST | met | Logs use the subject only. `createdBy` email is exposed by decision D27. |
| ARCH-22 | SHOULD | not met | No registry or catalogue entry. |
| ARCH-23 | SHOULD | partly | `docs/README.md` plus the input OpenAPI, which is not updated. |
| ARCH-24, ARCH-25, ARCH-26 | SHOULD | met | Own build and deploy, one capability, API only. |
| ARCH-27, ARCH-28 | SHOULD | not applicable | Pull only (D45). Synchronous writes are decided (D71). |
| ARCH-30 | SHOULD | partly | Two realm roles. The backend has unlimited store rights and the store has no accounts. |
| ARCH-31 | SHOULD | partly | Service, firewall and CSP layers exist. There is no network segmentation and no gateway. |
| ARCH-33 | SHOULD | met | Read retries after 200 ms and 400 ms (`AppProperties.Store.retryDelays`). |
| ARCH-34 | SHOULD | not met | No circuit breaker. |

### DATA

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| DATA-01 | SHOULD | not met | No CDM in the build (D2 says it is an input under review). |
| DATA-02, DATA-03 | SHOULD | not met | The component persists data. `docs/architecture.md` describes the named graphs in prose only. There is no LDM or PDM. |
| DATA-04 to DATA-16 | SHOULD | not applicable | No CDM was delivered to judge. |
| DATA-17, DATA-19 to DATA-21 | SHOULD | not met | No LDM. |
| DATA-22 to DATA-25 | SHOULD | not met | No PDM. For an RDF store this would be a graph or shape model. |
| DATA-26 | MUST | not met | No CDM update (recorded). |
| DATA-27 | MUST | partly | The API changed (D79) and the contract file did not. |
| DATA-28 | MUST | met | `docs/api-notes.md` and `docs/architecture.md` describe the changes. |
| DATA-29 | MUST | not met | No common source of truth for the CDM and the API. |

### SEC, OPS, OWASP, UX

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| SEC-01 | MUST | partly | The decisions cover access, failure and limits. No threat model document. |
| SEC-02 | MUST | partly | Secure coding is visible (strict firewall, strict JSON, escaping). Store hardening and alerting are missing. |
| SEC-07 | SHOULD | met | Keycloak OIDC, code flow with PKCE (`ui/src/lib/auth.ts`). |
| SEC-11 | SHOULD | partly | ECS logs can feed ELK. No shipping is configured. |
| SEC-12, SEC-13 | SHOULD | partly | Enforced in the service, not at a gateway (recorded). |
| SEC-15, SEC-22 | SHOULD | not applicable | No agent-to-agent calls and no agent administration. |
| SEC-23 | SHOULD | met | Realm roles are assigned to users in `keycloak/realm-simpl.json`. |
| OPS-01, OPS-02 | SHOULD | met | No OVH services. Tests use synthetic data. |
| OPS-03, OPS-17, OPS-29 | SHOULD | not applicable | No production data, branches or Helm chart in the factory. |
| OPS-37 | MUST | not met | No pipeline (recorded). |
| OWASP-01 | MUST | partly | Most A0x are covered. A05 (store), A09 (alerting) and A07 (rate limit) are partial. |
| OWASP-05 | MUST | partly | Threats are handled per decision. There is no explicit threat model. |
| OWASP-10 | MUST | partly | Logged, not monitored. |
| UX-01 | MUST | not met | Plain Vue, not participant-fe-components (recorded). |

## 3. Disagreements with the build's own documents

**Where the build's documents say met, or say nothing, and I find not met or only partly met:**

1. **REUSE.md, "Stateless design: state lives in the store".** Not true. `NameLocks` holds lock state in memory, which is why D81 forces one replica.
2. **REUSE.md, "Zero trust: every call is authenticated" and "Defence in depth".** The backend-to-Fuseki link has no authentication. Fuseki accepts SPARQL Update from anyone on the network and on host loopback.
3. **REUSE.md, token validation row, "RealmKeySource only caches the key set".** It also refetches, and `HttpKeyFetcher` is an own JWKS HTTP client. Nimbus `JWKSourceBuilder` offers the D44 behaviour; the record should say why it was not used.
4. **REUSE.md is incomplete** (REUSE-07). It has no rows for:
   - the own route matching and role authorisation (`RouteTable`, `RequestGateFilter`), which replace Spring Security request matchers or method security;
   - the own query-string parser (`QueryParameters`);
   - the decision not to use Micrometer metrics.
5. **QUALITY.md, section "NFR-05 Bounded resources".** It lists only limits and timeouts. Every list and search loads all rows into memory, and DEVIATIONS does not list it. Q4 admits it only for the vocabulary list.
6. **QUALITY.md, Q2 "each limit has a test at and above".** Two limits are "code review" only. This should be listed as not measured under NFR-13.
7. **DEVIATIONS.md omits rules I find not met or only partly met:**
   - NFR-08, MSV-09, MSV-10: no metrics or tracing, correlation id not forwarded. D67 also expects metrics on the management port.
   - CODE-21, CODE-52: log format.
   - CODE-33: shared exception framework.
   - DATA-02, DATA-03: LDM and PDM.
   - OWT-A02: unauthenticated Fuseki admin on loopback.
   - OWT-A10: the fail-open role gate on unmatched routes.
   - CODE-55: no UI lint.
   - MACH-06: in-process state.

**The other way round:**

8. DEVIATIONS lists NFR-06 because scheduling is not automated. I find NFR-06 met: the targets are stated and the restore is tested. Scheduling is an operations item, not a rule breach.
9. DEVIATIONS groups DATA-28 with DATA-26 to DATA-29 as not followed. I find DATA-28 met, because the docs that refer to the model were updated.

## 4. Findings for the next run

1. **Decision:** replace the in-process lock (D43, D81) with a store-side conditional update on the latest version number. That makes ARCH-07, MACH-06 and MACH-09 reachable and removes the stateless contradiction.
2. **Brief, authorisation:** enforce roles in Spring Security (`authorizeHttpRequests` matchers or `@PreAuthorize`) and deny any request that matches no declared operation. Keep the route table only for the 405, 406 and 415 order, or add a startup check that compares controller mappings with `RouteTable`.
3. **Brief, store hardening:** run Fuseki with authentication (Shiro, or a read-only plus update user), on an internal-only compose network, and without an unauthenticated `--update`. Treat the loopback debug port as read-only or drop it.
4. **Brief, bounded queries:** push filtering, `ORDER BY`, `LIMIT` / `OFFSET` and `COUNT` into SPARQL, and cap search candidate sets. Add NFR-05 to the step 50 self-check with "no `select` without `LIMIT`".
5. **Brief, observability:** require Micrometer and Prometheus on the management port, OpenTelemetry trace propagation, and forwarding of `X-Correlation-ID` or `traceparent` to outgoing calls. Put this in the build profile so it is not optional.
6. **Rules:** tag the API-* naming rules (API-10, API-50, API-54 and others) and the DATA CDM-content rules as spec-review checks (step 20 or 30), not step 70 code review. Or require the code writer to deliver an updated OpenAPI that reflects D79 (MACH-03, DATA-27, SOA-05).
7. **Decisions:** state whether the code writer must produce an LDM and PDM for an RDF store (named-graph model or SHACL shapes), and give the CDM source path, so DATA-02, DATA-03 and DATA-29 can be judged.
8. **REUSE.md template:** require one row for every own class that parses a standard format, routes requests, authorises, fetches keys or acts as an HTTP client. Require each named pattern to point to the code that applies it.
9. **Environment choice E6:** record that the Simpl LogWrapper, the shared exception framework and the gateway are unavailable to the factory, so CODE-21, CODE-33, CODE-52 and CODE-45 appear in DEVIATIONS automatically.
10. **Add a UI linter (ESLint) and a minimal pipeline stub** so CODE-55 and OPS-37 can move from "not met" to "met".
