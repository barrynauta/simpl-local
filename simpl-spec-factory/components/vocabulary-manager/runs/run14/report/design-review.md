# Design review

Build reviewed: `simpl-open-vocabulary-manager-run14/build/work` (backend, UI, proxy, Fuseki, compose, docs). I formed each verdict from the code and config first, and read `REUSE.md`, `DEVIATIONS.md`, `QUALITY.md` and `THREATS.md` afterwards.

## 1. In short

**Rules reviewed:** 153, every rule with `check: review` and step 70 in `steps`.

| Level | Met | Partly | Not met | Not applicable |
|---|---|---|---|---|
| MUST (52) | 26 | 20 | 2 | 4 |
| SHOULD (101) | 60 | 14 | 11 | 16 |

| Prefix | Met | Partly | Not met | N/A |
|---|---|---|---|---|
| DDD | 1 | 1 | 0 | 0 |
| MACH | 6 | 0 | 0 | 0 |
| MSV | 3 | 1 | 0 | 1 |
| OWT | 2 | 4 | 0 | 0 |
| OWA | 0 | 1 | 0 | 0 |
| REUSE | 5 | 3 | 0 | 0 |
| NFR | 5 | 1 | 0 | 0 |
| SOA | 1 | 0 | 0 | 0 |
| API | 10 | 4 | 3 | 4 |
| CODE | 15 | 5 | 4 | 1 |
| ARCH | 18 | 5 | 1 | 1 |
| DATA | 13 | 7 | 3 | 5 |
| SEC | 4 | 0 | 1 | 4 |
| OPS | 2 | 0 | 1 | 3 |
| OWASP | 1 | 2 | 0 | 0 |
| UX | 0 | 0 | 0 | 1 |

The two MUST rules that are not met (CODE-11 and OPS-37) were both set aside by decisions (D135 and D95). Most MUST "partly" verdicts come from items the decisions push to the platform: TLS, encryption at rest, limits on failed tokens, and alerting (D164, D185).

**The five most important findings**

1. **The HTTP front of the backend is mostly own code.** `GatekeeperFilter` (279 lines), `RouteMatcher` (114), the `Operation` table (168), `QueryParameters` with its own percent-decoder (148) and an nginx allow map together form a second router beside Spring MVC. There is no Spring Security in `pom.xml`. Three route tables are kept in line by a test. `REUSE.md` says routing is done by Spring MVC (D133) and has no row for `RouteMatcher`. (REUSE-01, REUSE-02, REUSE-07)
2. **Security events are logged unevenly.** Rejections made in the filter (firewall 400, 406, 413, 415, the upload-slot 503, and the identity-provider 503 inside the refetch window) go out through `ProblemWriter` and leave no log line. The same 4xx outcomes raised in a controller are logged at INFO by `ProblemHandler`. Only 401 and 403 are logged in both paths. (OWT-A09, OWASP-10, CODE-34)
3. **The correlation id stops at the backend.** The nginx `log_format` (`ui/nginx/nginx.conf:21`) and the Fuseki log pattern (`fuseki/log4j2.properties`) carry no correlation id and no trace id, so a request cannot be followed across the three containers. (MSV-09)
4. **The store is a single point of failure, and this is recorded nowhere.** The backend runs two replicas, but there is one Fuseki TDB2 instance and no replication. `QUALITY.md` presents Q4 scaling without saying so. The list query still sorts every matching vocabulary on each call (the recorded D188 deviation), so its cost grows with the store. (ARCH-07, ARCH-19, NFR-05)
5. **The API answers carry no security headers.** API responses (proxy `server` on port 8080, and the backend) have no `X-Content-Type-Options` and no `Cache-Control: no-store`; only the UI port sets headers. This is not recorded. TLS and token-failure limiting are deferred by decision. (OWA-API8, OWT-A04, OWT-A07)

## 2. Verdicts

### DDD

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| DDD-03 | MUST | partly | `docs/glossary.md` has a CDM column, but 17 of the 28 schemas in `openapi-decided.yaml` are missing (for example `InputValidationProblem`, `VocabularySearchRequest`, `CollectionPage`). |
| DDD-14 | MUST | met | No other context's terms appear in the API or code. Schema Manager is reached only through the searches (D45). |

### MACH

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| MACH-01 | SHOULD | met | Own Dockerfile and image per container (`backend/Dockerfile`). No deployment step joins it to another component. |
| MACH-02 | MUST | met | No other component's store or classes are used. Fuseki sits on the internal `store` network (`docker-compose.yml:142`). |
| MACH-03 | MUST | met | `docs/openapi-decided.yaml`. `ContractTest` checks the operation table against it. |
| MACH-06 | SHOULD | met | No session or lock state. Conditional writes with receipts (`VocabularyRepository.insertVersion`). The key cache is per instance by design. |
| MACH-07 | MUST | met | All settings come from `${...}` in `application.yml`. Secrets are in a `.env` that `stack.sh` generates and `.gitignore` lists. |
| MACH-09 | SHOULD | met | `server.shutdown: graceful`, 30 s per phase, `stop_grace_period: 40s`, `replicas: ${BACKEND_REPLICAS:-2}`. |

### MSV and SOA

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| MSV-02 | MUST | met | One dataset, used only by this backend, with three users (`fuseki/config.ttl`). |
| MSV-04 | MUST | n/a | No events (D45). |
| MSV-06 | MUST | met | Store: 2 s connect, 10 s read, 45 s update; reads retried at 200 and 400 ms; breaker gives 503 (`StoreClient.java:43-162`). Key fetch: 2 s timeout, fails closed with 503. |
| MSV-09 | MUST | partly | The backend puts the correlation id in the MDC and forwards it with `traceparent` (`TraceHeaders`). Proxy and Fuseki log lines carry neither. |
| MSV-10 | SHOULD | met | Logs use the logstash JSON format. Prometheus metrics run on management port 8081. |
| SOA-05 | SHOULD | met | Every operation needs the store, and the decided contract says so with 503. Health stays up while the store is down (D152). |

### OWT and OWA

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| OWT-A02 | MUST | partly | Actuator runs on its own port and is not proxied. Error bodies are sanitised, and no CORS is set. API responses have no nosniff or no-store headers (`nginx.conf` API server block). Keycloak runs in `start-dev` on 127.0.0.1 (D98). |
| OWT-A04 | MUST | partly | Plain HTTP and no encryption at rest (set aside by D185). Crypto comes from standard libraries (Nimbus, `MessageDigest`). No secrets in code. Logs name the store user, never the password (`StoreClient.java:232`). |
| OWT-A07 | MUST | partly | Signature, issuer, audience, expiry and nbf are checked with Nimbus, with no clock skew (`JwtAuthenticator.java:50-56`). No token in URLs. No limit on failed tokens (D164). |
| OWT-A08 | MUST | met | Turtle is parsed into triples by Jena RIOT, with no object deserialisation. Images are pinned by digest. The Fuseki jar is checked with SHA-512. |
| OWT-A09 | MUST | partly | 401 and 403 are logged at WARN, admin changes at INFO with `sub`. Filter rejections are not logged. No alert path (D185). |
| OWT-A10 | MUST | met | Each write is one SPARQL update. Missing keys fail closed with 503. Default deny through the operation table plus `HandlerRuleVerifier`. |
| OWA-API8 | SHOULD | partly | `servers` is `http://localhost`. CORS was decided (D126). Security headers exist on the UI port only. |

### REUSE

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| REUSE-01 | MUST | partly | Crypto and claims go through Nimbus `DefaultJWTProcessor` and `DefaultJWTClaimsVerifier`. Bearer extraction (`JwtAuthenticator.bearerToken`), role checks and request admission are own code in a servlet filter. No Spring Security. |
| REUSE-02 | SHOULD | partly | Own query-string parser and percent-decoder (`QueryParameters.decode`). Own `Accept` splitting on commas (`GatekeeperFilter.java:193`). `SparqlTemplate` does regex substitution of variables; the values are escaped by Jena `NodeFmtLib`. Spring's `UriUtils.decode` would have kept `+` as a plus. |
| REUSE-03 | SHOULD | met | Keycloak, Fuseki and nginx are used as products. Resilience4j provides the breakers. The small retry loops and the `UploadSlots` semaphore could have been Resilience4j Retry and Bulkhead (minor). |
| REUSE-04 | MUST | met | RFC 9457 problem+json, OIDC bearer tokens, OpenAPI, W3C trace context. `X-Correlation-Id` was decided (D86). |
| REUSE-05 | SHOULD | met | Catalogue choices are in E6 and `REUSE.md`. `participant-fe-components` is unavailable and the reason is recorded (D167). |
| REUSE-06 | SHOULD | met | Named patterns are listed (`REUSE.md`, `docs/architecture.md`). The receipt graph is optimistic concurrency, but it is not named as such. |
| REUSE-07 | MUST | partly | `REUSE.md` has no row for `RouteMatcher` plus `Operation` (about 280 lines of HTTP plumbing, D93), none for `TraceHeaders`, and none for the catalogue entries `http-client` and `participant-shared-code`. |
| REUSE-09 | SHOULD | met | No vendored code. `ProblemErrorReportValve` uses Tomcat's extension point. |

### NFR

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| NFR-05 | SHOULD | partly | Every list query has LIMIT and OFFSET; lookup has LIMIT 20 000. JSON bodies are capped at 1 MB+1. Every container has limits. But the list sort still scans every vocabulary (D188 deviation). External multipart registration has no upload slot while `/tmp` is a 64 MB tmpfs and multipart parts over 1 MB spill to disk. |
| NFR-06 | MUST | met | Q3 targets are stated. `scripts/backup.sh`, `restore.sh` and `backup-restore-test.sh` exist, and two backups were made today in `backups/`. |
| NFR-08 | SHOULD | met | Timer `vocabulary.api.requests` tagged by operation and status (`GatekeeperFilter.java:97`). Brave W3C propagation. Trace headers go to the store and Keycloak. |
| NFR-11 | SHOULD | met | Configuration through environment variables only. The Fuseki config is templated at entrypoint. No host paths. |
| NFR-12 | SHOULD | met | Q4 design load is stated. `QUALITY.md` says the backends are stateless and the state lives in the store. |
| NFR-13 | MUST | met | `QUALITY.md` lists target and measured value per Q. Availability is listed as not measured, with the reason (D191). |

### API (most of these rules judge the published contract, which the build may not change, D39)

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| API-01 | MUST | partly | The CDM is input (`input/cdm`) and is not shipped in `work/docs`. The build ships a logical model instead. |
| API-02 | SHOULD | not met | The schemas redefine the business types inline. Inherited from the published file. |
| API-03 | SHOULD | met | The decided file is derived from the published file plus D79 (`scripts/make_decided_openapi.py`), not from the code. |
| API-10 | SHOULD | partly | `/external-vocabulary/searches` is singular (inherited). |
| API-11 | SHOULD | met | POST for searches and the content lookup that need a body, as decided. |
| API-15 | SHOULD | met | No British spellings in property names. |
| API-19 | SHOULD | met | Property names carry business meaning. |
| API-22 | SHOULD | met | No placeholder examples found. |
| API-28 | SHOULD | met | Collections, documents and controllers (`/searches`). |
| API-30 | SHOULD | met | `/vocabularies/{vocabularyName}`. |
| API-35 | SHOULD | partly | UI JSON calls send no `Accept` (`ui/src/lib/api.ts:107`). The store update and the key fetch send none either (`StoreClient.update`, `RealmKeyCache.download`). |
| API-49 | SHOULD | partly | `info.description` leaves out external vocabularies and adds a sentence about the factory run. |
| API-50 | SHOULD | not met | The identifier is `name` (inherited, D30). |
| API-51 | SHOULD | met | Relations use `latestVersionHref` and `contentHref`. |
| API-54 | SHOULD | not met | There is no identifier schema type (for example `VocabularyName`). |
| API-55 | SHOULD | n/a | No standard identifier applies. |
| API-58 | SHOULD | met | `status` and `sort` are enums. |
| API-60 | SHOULD | met | Path parameters are required. |
| API-62, API-79, API-80 | MUST, SHOULD, SHOULD | n/a | No Kafka (D45). |

### CODE

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| CODE-06 | MUST | met | Methods are short and focused. `GatekeeperFilter.admit` is one ordered check chain. |
| CODE-09 | MUST | met | No flag-driven merged methods found. |
| CODE-11 | MUST | not met | No interfaces (D135, recorded). |
| CODE-14 | MUST | met | Identifiers are in English. |
| CODE-15 | MUST | met | Comments are in English. |
| CODE-16 | MUST | met | Readable code with Javadoc that cites decisions. |
| CODE-17 | MUST | met | Names are clear. Minor: repositories still call `SparqlTemplate` objects `pss` (`VocabularyRepository.java:77`). |
| CODE-20 | MUST | partly | The MDC carries operation, vocabulary and version. 4xx logs carry only the title. Filter rejections are not logged. |
| CODE-21 | SHOULD | not met | LogWrapper is not used (D105, recorded). |
| CODE-24 | MUST | met | 401 and 403 at WARN, user actions at INFO, 5xx at ERROR. |
| CODE-25 | SHOULD | met | ERROR is used only for 5xx and for store 401/403 (D201). |
| CODE-30 | SHOULD | met | MDC fields `correlationId`, `operationId`, `userId`, `vocabularyName` and `version`. |
| CODE-32 | SHOULD | met | Only `sub` is logged. `createdBy` is never logged. |
| CODE-33 | SHOULD | not met | No shared base hierarchy (D104). Not listed in `DEVIATIONS.md`. |
| CODE-34 | MUST | partly | There are three rendering paths: the filter, `ProblemHandler` and the Tomcat valve. Only `ProblemHandler` logs. |
| CODE-35 | MUST | met | Own `ApiException`, framework exceptions, a catch-all `Exception`, store and IO classification, and the Tomcat valve. |
| CODE-36 | MUST | met | `ProblemType` maps each type to status, title and default detail. |
| CODE-44 | SHOULD | met | The `ProblemType` enum is the error code (D104). |
| CODE-45 | SHOULD | n/a | No gateway in the factory. Production gateway per D17. |
| CODE-46 | SHOULD | met | Resilience4j breakers on the store and the key fetch (`AppConfig.java:36`). |
| CODE-48 | SHOULD | partly | Spring DI is used, but `TraceHeaders` assigns a static `instance` in its constructor, and the beans `StoreClient` and `RealmKeyCache` read it statically instead of injecting it. |
| CODE-50 | SHOULD | met | JSON logs to stdout. |
| CODE-52 | SHOULD | not met | Not the Simpl log format (D105, recorded). |
| CODE-54 | MUST | partly | Gaps in CODE-39 Javadoc (recorded), plus the points above. |
| CODE-55 | SHOULD | partly | Spotless for Java, ESLint and Prettier for TS/Vue. No linter for the shell scripts, Python scripts, Dockerfiles or nginx config. |

### ARCH

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| ARCH-01 | SHOULD | met | No central dependency beyond the identity provider. |
| ARCH-02 | MUST | met | Other components use only the API. |
| ARCH-04 | SHOULD | met | Runtime dependencies are Keycloak and Fuseki only. |
| ARCH-05 | SHOULD | met | No store or framework details in the schemas. |
| ARCH-07 | MUST | partly | Two backend replicas, but a single Fuseki and a single Keycloak. |
| ARCH-08 | SHOULD | met | EUPL `LICENSE`, open OpenAPI. |
| ARCH-09 | SHOULD | met | No vendor SDKs. |
| ARCH-11 | SHOULD | met | Search and content operations are generic. |
| ARCH-13 | SHOULD | met | Docs describe the contracts. |
| ARCH-14 | SHOULD | met | CPU and memory are compose variables (D202). |
| ARCH-17 | MUST | partly | Access control is done. TLS and encryption at rest are deferred (D185). |
| ARCH-18 | MUST | partly | Validation, one transaction per write, SHA-256 of content. No integrity protection in transit. |
| ARCH-19 | MUST | partly | Backup and restore are tested. The store is not replicated. |
| ARCH-20 | MUST | met | Email is not logged and is marked as personal data (D165). |
| ARCH-22 | SHOULD | not met | No registration in a catalogue or service registry. |
| ARCH-23 | SHOULD | met | `info.description` and `docs/README.md`. |
| ARCH-24 | SHOULD | partly | Own image, but no pipeline and no Helm chart. |
| ARCH-25 | SHOULD | met | One capability. |
| ARCH-26 | SHOULD | met | The store is used by one service. |
| ARCH-27 | SHOULD | n/a | Pull only (D45). |
| ARCH-28 | SHOULD | met | The 201 must carry the validation outcome, so writes stay synchronous. 5 MB writes measured at about 5 s. |
| ARCH-30 | SHOULD | met | Read, update and admin store users. Non-root, read-only, `cap_drop: ALL`. |
| ARCH-31 | SHOULD | met | Limits at the proxy, token and role checks in the service, store on an internal network with users. |
| ARCH-33 | SHOULD | met | Read retries with backoff. Writes are not retried (as decided). |
| ARCH-34 | SHOULD | met | Same breakers as CODE-46. |

### DATA (DATA-04 to DATA-16 judge the input CDM; the build's logical model fixes several of them)

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| DATA-01 | SHOULD | met | `input/cdm/vocabulary-manager-be.md`. |
| DATA-02 | SHOULD | met | `docs/data-model.md`, logical model diagram and table. |
| DATA-03 | SHOULD | partly | The physical model is a named-graph table plus `docs/shapes.ttl`, not a diagram. |
| DATA-04 | SHOULD | partly | The CDM has `VocabularySearch` classes. |
| DATA-05 | SHOULD | met | No UUIDs or audit fields. |
| DATA-06 | SHOULD | partly | The `latestVersion` attribute duplicates the has-versions association. |
| DATA-07 | SHOULD | met | Status values are listed. |
| DATA-08 | SHOULD | met | Associations carry verb labels. |
| DATA-09 | SHOULD | met | UML class diagram with noun names. |
| DATA-10 | SHOULD | not met | CDM attributes are untyped. The build's logical model types them. |
| DATA-11 | SHOULD | met | No boolean flags. |
| DATA-12 | SHOULD | not met | Generic `creationDate` and `lastUpdateDate` (kept by D207). |
| DATA-13 | SHOULD | met | Multiplicities on both ends. |
| DATA-14 | SHOULD | not met | `VocabularySearch` and `ExternalVocabularySearch` model API calls. |
| DATA-15 | SHOULD | partly | Same cause as DATA-04 and DATA-14. |
| DATA-16 | SHOULD | partly | `description` sits on both the vocabulary and the version (D28). |
| DATA-17 | SHOULD | met | The logical model is a mermaid class diagram. |
| DATA-19 | SHOULD | met | Types and `[id]` and unique keys are given. |
| DATA-20 | SHOULD | met | Multiplicities are given. |
| DATA-21 | SHOULD | n/a | No many-to-many association with data. |
| DATA-22 to DATA-25 | SHOULD | n/a | RDF store, no tables. SHACL plays the role of the physical model. |
| DATA-26 | MUST | partly | The domain changed (`createdBy`, `contentSha256`, description moved) without a CDM update. The differences are listed for the owners. |
| DATA-27 | MUST | met | `openapi-decided.yaml` follows the changes. |
| DATA-28 | MUST | met | `docs/data-model.md` and the glossary are updated. |
| DATA-29 | MUST | partly | The CDM, the published API and the decided API live in different places with different versions. |

### SEC, OPS, OWASP, UX

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| SEC-01 | MUST | met | `THREATS.md`, STRIDE per operation group. |
| SEC-02 | MUST | n/a | The guideline document is not available. |
| SEC-07 | SHOULD | met | `openIdConnect` scheme. Keycloak tokens are validated in the service. |
| SEC-11 | SHOULD | not met | No log shipping (D105, recorded). |
| SEC-12, SEC-13 | SHOULD | n/a | No gateway in the factory. |
| SEC-15 | SHOULD | n/a | No calls from agent to agent. |
| SEC-22 | SHOULD | met | Technical store users and a generated Keycloak bootstrap admin. |
| SEC-23 | SHOULD | met | Rights are given through realm roles. |
| OPS-01 | SHOULD | met | No OVH services. |
| OPS-02 | SHOULD | met | Synthetic users at `example.com`, unique run data. |
| OPS-03, OPS-17, OPS-29 | SHOULD | n/a | No production data, no GitLab, no Helm. |
| OPS-37 | MUST | not met | No pipeline (D95, recorded). Gates run in `mvn verify` and the UI build. |
| OWASP-01 | MUST | partly | Rolls up the points above. |
| OWASP-05 | MUST | met | `THREATS.md`. |
| OWASP-10 | MUST | partly | Input validation failures are logged only when they are raised in a controller. Firewall, 406, 413 and 415 rejections leave no log line. No monitoring (D185). |
| UX-01 | MUST | n/a | The UX guidelines are not available (recorded). |

## 3. Disagreements with the build's own documents

1. **REUSE.md, "Web framework, routing":** it says routing is done by Spring MVC mappings (D133). In fact `GatekeeperFilter` routes every request through `RouteMatcher` and the `Operation` enum before Spring MVC sees it. Spring MVC only dispatches what the gatekeeper has admitted. `RouteMatcher` (about 280 lines with the table) has no row, as D93 requires. The nginx `map $uri $allow_methods` is a third copy of the route table.
2. **REUSE.md, "Request validation":** it justifies `QueryParameters` because the container decodes `+` as a space. Spring's `UriUtils.decode` would have kept `+` without an own decoder. So I rate REUSE-02 partly, not met.
3. **REUSE.md completeness:** there are no rows for the catalogue entries `http-client` and `participant-shared-code` (E6 explains them, but REUSE-07 asks for them in `REUSE.md`). `TraceHeaders` and its static access are not mentioned either.
4. **ProblemHandler Javadoc ("The one exception handler of the component"):** errors are rendered in three places, and only `ProblemHandler` logs. CODE-34 is partly, not met as the build implies.
5. **DEVIATIONS.md, OWT-A09/OWASP-10 ("security events are logged (WARN)"):** this holds only for 401 and 403. Other rejections in the filter, the upload-slot 503 and the 503 for keys in the refetch window are not logged at all.
6. **QUALITY.md, Observability:** it lists `correlationId` in the JSON log lines. That is true for the backend only. Proxy and store logs carry no correlation or trace id (MSV-09 partly).
7. **QUALITY.md, Q4 (scaling):** two backend replicas are presented as scaling, but the single Fuseki is not called a single point of failure. ARCH-07 and ARCH-19 are not in `DEVIATIONS.md`.
8. **DEVIATIONS.md, missing rows:** CODE-33 (set aside by D104), OWA-API8 (no security headers on API responses), ARCH-22 (no registration) and DDD-03 (17 schemas missing from the glossary) are not recorded.
9. **I agree with the build** on its recorded deviations: D94 (SparqlTemplate, values escaped by Jena), D188 (TDB2 cannot do an ordered index scan), D99 (reduced seed), D123 (PrimeVue base styles blocked by the CSP), and the D185 platform items.

## 4. Findings for the next run

1. **Rules:** many API and DATA rules judge the published OpenAPI and the input CDM, which D39 forbids the build to change. Either move them out of step 70 (to steps 21 and 23), or add an "inherited from input" verdict so they stop showing as build gaps.
2. **Decisions, routing:** D133, D178 and D206 together produce three route tables (enum, Spring MVC, nginx). One decision should name a single source, for example generating the nginx map and the `Operation` table from `openapi-decided.yaml` at build time. Or it should allow Spring Security with a custom entry point and filter order, and accept the gatekeeper as its own row in `REUSE.md`.
3. **Decisions, logging:** add a decision that every rejection, wherever it is raised, writes one log line at a fixed level (INFO for 4xx, WARN for 401 and 403 and the firewall 400, ERROR for 5xx) through one path. Extend D173 so the proxy and the store log the correlation id and trace id.
4. **Decisions, API headers:** decide the headers on API responses: `X-Content-Type-Options: nosniff`, `Cache-Control: no-store` on authenticated reads, with or without a `Content-Language` header.
5. **Decisions, availability:** state whether a single Fuseki is an accepted risk (ARCH-07, ARCH-19) so the build records it. Rewrite D188, which TDB2 cannot meet. For example, accept top-N sorting and set a store size up to which Q1 must hold.
6. **Decisions, uploads:** decide whether external registration also needs upload slots, or size `/tmp` for the parts that spill to disk. Today 11 parallel 6 MB registrations on one instance would exceed the 64 MB tmpfs. This follows from the config and was not tested.
7. **Brief for the code writer:** require a `REUSE.md` row for every entry in `reuse-catalogue.yaml`, plus every own class over about 100 lines doing HTTP, parsing, routing or query building. Require `DEVIATIONS.md` to list every rule a decision sets aside (CODE-33 was missed).
8. **Brief for the code writer:** inject collaborators instead of static singletons (`TraceHeaders`), and set `Accept` on every outgoing call, UI included (API-35).
9. **Glossary:** extend the decided glossary to cover every schema of `openapi-decided.yaml`, or let DDD-03 accept that request and response wrappers point to their business term.
