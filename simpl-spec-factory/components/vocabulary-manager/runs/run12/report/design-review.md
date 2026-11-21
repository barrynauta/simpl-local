# Design review

## 1. In short

**Rules reviewed:** 153. These are the rules with `check: review` and step 70 in their steps. For the CDM-content rules (DATA-04 to DATA-16) I judged the input CDM together with the build's logical model.

**Verdicts by level**

| Level | met | partly | not met | not applicable |
|---|---|---|---|---|
| MUST (52) | 27 | 18 | 3 | 4 |
| SHOULD (101) | 55 | 22 | 16 | 8 |

**Verdicts by prefix** (met / partly / not met / n.a.): DDD 2/0/0/0, MACH 6/0/0/0, MSV 3/1/0/1, OWT 3/2/1/0, OWA 0/1/0/0, REUSE 4/4/0/0, NFR 5/1/0/0, SOA 1/0/0/0, API 9/6/2/4, CODE 17/3/5/0, ARCH 15/8/2/0, DATA 13/10/4/1, SEC 2/1/3/3, OPS 2/0/2/2, OWASP 0/3/0/0, UX 0/0/0/1.

The three MUST rules not met are OWT-A04, CODE-11 and OPS-37. CODE-11 and OPS-37 are set aside by decisions D135 and D95.

**Five most important findings**

1. **No TLS anywhere.** The service fetches the realm keys over plain HTTP (`docker-compose.yml:18`). The store's basic-auth credentials also go over plain HTTP (`StoreClient.java:146`). The only decision on encryption covers data at rest (D165). TLS in transit is a self-declared deviation with no decision behind it.
2. **Token handling is partly own code.** `TokenVerifier.java:152-220` fetches and caches the realm keys itself, and `BearerTokenFilter.java` (84 lines) replaces Spring Security's resource-server filter. Nimbus does the cryptography. REUSE.md gives two reasons and records only the first:
   - It says `JWKSourceBuilder` cannot meet D88 and D129. That is arguable, because it has a configurable rate limit and an outage cache.
   - It does not mention the filter at all.
   
   Also, the circuit breaker on the key fetch (`TokenVerifier.java:107-117`) never helps. It needs 10 recorded calls, and the fetch runs at most once per 10 s, so it opens only after about 100 s and then skips one fetch.
3. **Trace context is passed on by hand.** StoreClient and TokenVerifier build the `traceparent` header as a string (`StoreClient.java:156-160`, `TokenVerifier.java:210-214`). They reuse the server span id and always set the "sampled" flag. Calls through the JDK `HttpClient` are not instrumented, so there are no client spans and no per-dependency latency metrics. This contradicts D94 and REUSE.md's line "Micrometer Tracing".
4. **The ops scripts handle secrets and formats loosely.**
   - The Fuseki admin password is put in a URL (`ops/backup.sh:14`).
   - JSON is parsed with `sed` and `grep` (`backup.sh:16-20`).
   - `BACKUP_COPY_CMD` is run with `eval` (`backup.sh:36`).
   
   None of this is in DEVIATIONS.md.
5. **No threat model, no alert path, no pipeline.** `architecture.md` has a security section that lists controls, but no threats (SEC-01, OWASP-05). Security events are logged at WARN on the `security` logger, but nothing can raise an alert (OWT-A09, OWASP-10). There is no CI pipeline (OPS-37, MUST), which D95 waives.

## 2. Verdicts

### DDD
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| DDD-03 | MUST | met | `docs/glossary.md` has a definition, CDM class and API names for each term. |
| DDD-14 | MUST | met | The API uses its own terms. There are no outside-context terms such as EDC or connector. |

### MACH
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| MACH-01 | SHOULD | met | Each part has its own Dockerfile and image. The backend runs alone against its store (`backend-store-down` profile). |
| MACH-02 | MUST | met | Only the backend reaches Fuseki, on the internal `store` network (`docker-compose.yml:160-163`). |
| MACH-03 | MUST | met | `docs/openapi-decided.yaml` matches `Routes.ALL`. `OpenApiContractTest` and `RouteRuleCheck` enforce this. |
| MACH-06 | SHOULD | met | No session state. Writes are conditional store updates (`VocabularyStore.java:278-311`). The upload limiter is per instance by design (D92). |
| MACH-07 | MUST | met | Secrets come from `.env` via `stack.sh`. `application.yaml:88-107` uses environment variables, with localhost URLs as dev defaults and empty password defaults. |
| MACH-09 | SHOULD | met | `server.shutdown: graceful`, 30 s per phase, `stop_grace_period: 35s`. `BACKEND_REPLICAS` defaults to 2. |

### MSV
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| MSV-02 | MUST | met | The Fuseki dataset is owned by the backend. The ops scripts use a separate admin user. |
| MSV-04 | MUST | n.a. | No events (D45). |
| MSV-06 | MUST | met | Store timeouts 2/10/45 s, read retries 200 and 400 ms, a breaker, and 503 with Retry-After. The key fetch has a 2 s timeout and returns 503 (D44). |
| MSV-09 | MUST | partly | The backend has `CorrelationFilter`, MDC and forwarding. The nginx gateway neither creates nor logs `X-Correlation-Id` (`gateway/default.conf`), and Fuseki logs lack it. |
| MSV-10 | SHOULD | met | Logstash JSON logs (`application.yaml:79-82`) and Prometheus on the management port. |

### OWT and OWA
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| OWT-A02 | MUST | met | `include-stacktrace: never`. Actuator is on port 9090, not published or proxied. `ProblemHandler` returns a generic 500. |
| OWT-A04 | MUST | not met | Plain HTTP for the API, the key fetch and the store. The TDB2 volume is unencrypted. Standard crypto is used and no secrets are in code. |
| OWT-A07 | MUST | partly | Signature, issuer, audience and expiry are checked (`TokenVerifier.java:118-149`). Failed tokens are not limited (D164). The admin password is in a URL in `ops/backup.sh:14`. |
| OWT-A08 | MUST | met | The Fuseki jar is downloaded with a checksum (`fuseki/Dockerfile:12`). Base images are pinned by digest. Turtle is parsed by Jena RIOT into triples, never deserialised into objects. |
| OWT-A09 | MUST | partly | 401 and 403 are logged at WARN on the `security` logger with the token subject. Admin changes are logged at INFO. Nothing raises an alert. |
| OWT-A10 | MUST | met | The service fails closed with 503 when keys are missing. Each write is one store update with a receipt token. |
| OWA-API8 | SHOULD | partly | The OpenAPI lists an https server and an http local one. The UI nginx sets CSP and other headers, but the API gateway sets none. The contract states no CORS rule. |

### REUSE
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| REUSE-01 | MUST | partly | Nimbus does the cryptography. The key fetch and cache (`TokenVerifier.java:152-220`) and `BearerTokenFilter` are own code, where the Spring Security resource server would do. |
| REUSE-02 | SHOULD | partly | Own code handles standard formats in four places: the query-string splitter (`QueryParams.java:35-55`), JSON built by string concatenation (`ProblemErrorReportValve.java:32-44`), the `traceparent` string, and `sed` JSON parsing in `backup.sh`. SPARQL itself is built with `ParameterizedSparqlString`. |
| REUSE-03 | SHOULD | met | Keycloak, Fuseki and nginx are used. No own store, queue or token issuer. |
| REUSE-04 | MUST | met | OIDC bearer tokens, RFC 9457 problem details, OpenAPI 3.0. |
| REUSE-05 | SHOULD | partly | The catalogue is mapped in REUSE.md. `participant-shared-code`, `certificate-authority` and `graph-index` are not mentioned. The http-client reason ("no outgoing calls except the realm keys") is wrong. |
| REUSE-06 | SHOULD | met | Optimistic concurrency, retry, breaker, bulkhead and fail-closed are named and real. Three nits: "Aggregate root" and "Chain of responsibility" only label framework use, and the key-fetch breaker is redundant. |
| REUSE-07 | MUST | partly | REUSE.md exists but misses `BearerTokenFilter`, `QueryParams`, `ProblemErrorReportValve`, the hand-built `traceparent` and the ops JSON parsing. |
| REUSE-09 | SHOULD | met | The custom Tomcat error valve is a supported extension point. No vendored library code. |

### NFR and SOA
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| NFR-05 | SHOULD | met | Every SPARQL query has a LIMIT. Fuseki has a 10 s query timeout (`fuseki/vocabulary.ttl:18`). Tomcat has 200 threads, there are 4 upload permits, and every container has CPU and memory limits. |
| NFR-06 | MUST | met | Q3 targets are in `docs/operations.md`. The backup and restore scripts check counts. The round trip is recorded in BUILD-LOG.md (565 = 565 quads). |
| NFR-08 | SHOULD | partly | JSON logs and per-operation metrics are in place. Trace context is passed by hand, there are no client spans or store-latency metrics, and the gateway logs in nginx's plain-text format. |
| NFR-11 | SHOULD | met | Configuration only through environment variables. No host paths. |
| NFR-12 | SHOULD | met | Q4 and QUALITY.md give the design load, two replicas and the stateless backend. The store is a single instance. |
| NFR-13 | MUST | met | QUALITY.md gives measured values or the reason something was not measured: Q1 at a reduced seed, with the list p95 at 305 ms reported as above target, and availability not measured. |
| SOA-05 | SHOULD | met | Reads keep working while Keycloak is down (cached keys) and `/health` reports DEGRADED. 503 is declared on every operation. |

### API
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| API-01 | MUST | partly | The CDM sits only in `input/cdm`. The build ships the OpenAPI with an LDM, not the CDM. |
| API-02 | SHOULD | partly | The schemas are local (`VocabularyCategory` and others), as in the published contract. |
| API-03 | SHOULD | met | The OpenAPI is committed source, derived from the published file by `tools/make_openapi_decided.py`. No springdoc. |
| API-10 | SHOULD | partly | `/external-vocabulary/searches` is singular (inherited from the published contract). |
| API-11 | SHOULD | partly | `POST /external-vocabularies/content` is a read done with POST (the body carries a list of URIs). |
| API-15 | SHOULD | met | No British spellings in property names on a spot check. |
| API-19 | SHOULD | met | Names describe business meaning (`changelog`, `latestVersionHref`). |
| API-22 | SHOULD | partly | DEVIATIONS.md admits some request bodies have no example. |
| API-28 | SHOULD | met | Collections, documents and `searches` controllers. |
| API-30 | SHOULD | met | `/vocabularies/{vocabularyName}` and `/versions/{version}`. |
| API-35 | SHOULD | partly | Accept is sent on SPARQL queries only. It is missing on updates and on the key fetch (`TokenVerifier.java:203`). |
| API-49 | SHOULD | met | `info.description` states the purpose for the Governance Authority. |
| API-50 | SHOULD | not met | The identifier is `name` (D30, D31, D39). |
| API-51 | SHOULD | met | References are href fields named after the relation. |
| API-54 | SHOULD | not met | No `...Id` identifier types (same decisions). |
| API-55 | SHOULD | n.a. | No standard business identifier applies. |
| API-58 | SHOULD | met | Status and sort enums are present and tested. |
| API-60 | SHOULD | met | Path parameters are required. The contract test checks the parameters. |
| API-62, API-79, API-80 | MUST, SHOULD, SHOULD | n.a. | No Kafka. |

### CODE
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| CODE-06 | MUST | met | Methods are focused. The long controller methods keep the check order of D51. |
| CODE-09 | MUST | met | No flag-driven merged methods. |
| CODE-11 | MUST | not met | No interfaces (decision D135). |
| CODE-14, CODE-15 | MUST | met | Identifiers and comments are in English. |
| CODE-16, CODE-17 | MUST | met | Code is readable and names are clear. Nit: inline fully qualified annotations (`TokenVerifier.java:95`, `SecurityConfig.java:42`). |
| CODE-20 | MUST | met | Failures are logged with the MDC fields (operationId, vocabularyName, version, userId). |
| CODE-21 | SHOULD | not met | No LogWrapper (D105). |
| CODE-24, CODE-25 | MUST, SHOULD | met | ERROR is used only for unexpected exceptions (`ProblemHandler.java:102`). Rejections are logged at WARN, actions at INFO. |
| CODE-30, CODE-32 | SHOULD | met | MDC fields are set. Only the token subject is logged, never an email (D80). |
| CODE-33 | SHOULD | not met | Own `ApiException` (D104). |
| CODE-34, CODE-35, CODE-36 | MUST | met | One `ProblemHandler` plus `ProblemWriter`, also used by the filters. It covers framework, store and token exceptions and has a catch-all. Each problem type has a defined detail. |
| CODE-44 | SHOULD | met | The `ProblemType` enum is the error code. |
| CODE-45 | SHOULD | not met | No Spring Cloud Gateway (D17, E6). |
| CODE-46, CODE-48 | SHOULD | met | Resilience4j breakers are used, and beans are injected. |
| CODE-50 | SHOULD | partly | JSON goes to stdout. No log shipping. |
| CODE-52 | SHOULD | not met | Not the Simpl log format (D105). |
| CODE-54 | MUST | partly | Java and TypeScript are formatted and linted. The shell scripts carry the issues of finding 4. |
| CODE-55 | SHOULD | partly | Spotless, ESLint and Prettier are set up. Shell, Dockerfiles and nginx config have no linter. |

### ARCH
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| ARCH-01, ARCH-02, ARCH-04, ARCH-05 | SHOULD, MUST, SHOULD, SHOULD | met | The only dependencies are Keycloak and its own Fuseki. The API hides RDF details. |
| ARCH-07 | MUST | partly | The backend runs 2 replicas with conditional writes. Fuseki and Keycloak are single instances. |
| ARCH-08, ARCH-09, ARCH-11, ARCH-13 | SHOULD | met | EUPL LICENSE. No vendor SDKs. Generic search and content APIs. Docs describe the protocols. |
| ARCH-14 | SHOULD | partly | CPU and memory are variables in the compose file. No Helm chart. |
| ARCH-17 | MUST | partly | Access control and secret handling are good. No TLS. |
| ARCH-18 | MUST | partly | Validation, single-transaction writes and SHA-256 are in place. No integrity protection in transit. |
| ARCH-19 | MUST | partly | Backend replicas and a tested restore. The store is a single point of failure. |
| ARCH-20 | MUST | partly | Emails are not logged, but they are stored unencrypted and shown to viewers (D165). |
| ARCH-22 | SHOULD | not met | No registry. |
| ARCH-23, ARCH-25, ARCH-26 | SHOULD | met | Service description present, one capability, own store. |
| ARCH-24 | SHOULD | partly | Own image, but no pipeline and no Helm chart. |
| ARCH-27 | SHOULD | not met | Pull only (D45). |
| ARCH-28 | SHOULD | partly | A 5 MB upload is validated synchronously, up to 30 s (D71). |
| ARCH-30, ARCH-31, ARCH-33, ARCH-34 | SHOULD | met | Separate store read, update and admin users, and a role per route. Checks at the network, firewall, token and role layers. Retries with backoff. Breakers. |

### DATA
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| DATA-01, DATA-02 | SHOULD | met | Input CDM present. `docs/logical-data-model.md` present. |
| DATA-03 | SHOULD | partly | The physical model is SHACL shapes, not UML (D87). |
| DATA-04, DATA-05, DATA-08, DATA-09, DATA-11, DATA-13 | SHOULD | met | Business nouns, no UUIDs, associations labelled with verbs, multiplicities shown. |
| DATA-06 | SHOULD | partly | The CDM has `latestVersion` as an attribute next to the "has versions" association. |
| DATA-07, DATA-10 | SHOULD | partly | The CDM has untyped attributes and no status enumeration. The LDM fixes both. |
| DATA-12 | SHOULD | partly | `lastUpdateDate` is a generic timestamp. |
| DATA-14, DATA-15 | SHOULD | partly | The CDM has `VocabularySearch` and `ExternalVocabularySearch` (API mechanics). The LDM drops them. |
| DATA-16 | SHOULD | partly | `description` appears on both vocabulary classes in the CDM. |
| DATA-17, DATA-19, DATA-20 | SHOULD | met | The LDM is a class diagram with types, keys and multiplicities. |
| DATA-21 | SHOULD | n.a. | No many-to-many association with its own data. |
| DATA-22 to DATA-25 | SHOULD | not met | Physical model is SHACL, not UML (D87). |
| DATA-26 | MUST | partly | `createdBy` was added without a CDM update. It is listed as an owner question. |
| DATA-27, DATA-28 | MUST | met | The OpenAPI, glossary and LDM are updated. |
| DATA-29 | MUST | partly | The CDM is not versioned in the build tree together with the API. |

### SEC, OPS, OWASP and UX
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| SEC-01 | MUST | partly | `architecture.md` has a security section listing controls. No threats are named. |
| SEC-02 | MUST | n.a. | The Secure Development Guideline is not available. |
| SEC-07 | SHOULD | met | The `KeycloakOidc` scheme is declared and the service validates Keycloak tokens. |
| SEC-11, SEC-12 | SHOULD | not met | D105 and D17. |
| SEC-13, SEC-15 | SHOULD | n.a. | No Tier 2 traffic. |
| SEC-22 | SHOULD | not met | Keycloak uses a bootstrap admin from `.env`. |
| SEC-23 | SHOULD | met | Rights go through realm roles. |
| OPS-01, OPS-02 | SHOULD | met | No managed services. Test data is synthetic. |
| OPS-03, OPS-17 | SHOULD | n.a. | No production data. No GitLab settings to check. |
| OPS-29 | SHOULD | not met | No chart. |
| OPS-37 | MUST | not met | No pipeline (D95). The checks live only in `mvn verify` and `npm run build`, and the backend Dockerfile skips Spotless. |
| OWASP-01 | MUST | partly | See the OWT and SEC rows. |
| OWASP-05 | MUST | partly | Controls and the abuse cases of D46 exist. No threat model. |
| OWASP-10 | MUST | partly | Security events are logged but not monitored. |
| UX-01 | MUST | n.a. | Guidelines not available (D167). |

## 3. Disagreements with the build's own documents

1. **REUSE.md, "Token validation":** it says only the fetch timing is own code, about 60 lines.
   - The fetch and cache are about 100 lines (`TokenVerifier.java:152-220`), and `BearerTokenFilter` (84 lines) replaces the Spring Security resource server. The filter is not listed.
   - The claim that `JWKSourceBuilder` cannot meet D88 and D129 is arguable. Its rate limit and outage handling raise distinct exception types that could map to 401 and 503.
2. **REUSE.md, "Tracing and correlation":** it says Micrometer Tracing is used (D94). Outgoing propagation is built by hand in two classes, and store calls produce no client observations.
3. **REUSE.md, http-client "not needed (no outgoing calls except the realm keys)":** every store call is an outgoing HTTP call.
4. **REUSE.md says "none" built for JSON and URIs.** Own code still handles standard formats in four places: `ProblemErrorReportValve` concatenates JSON, `QueryParams` parses the query string, `backup.sh` parses JSON with `sed`, and the `traceparent` header is a hand-built string.
5. **DEVIATIONS.md, OWT-A07:** it records only the missing rate limit. It misses the admin password in a URL (`backup.sh:14`).
6. **DEVIATIONS.md is silent on several gaps I find:** SEC-01 and OWASP-05 (no threat model), OWT-A09 and OWASP-10 (no alert path), CODE-55 (shell, Dockerfiles and nginx not linted), API-01 and DATA-29 (CDM not shipped with the API).
7. **DEVIATIONS.md, ARCH-07 "not followed":** I find it partly met. The backend is redundant; only the store and the identity provider are single instances.
8. **REUSE.md, pattern table:** I count the key-fetch circuit breaker as over-engineering, not a working pattern (finding 2). "Aggregate root" and "Chain of responsibility" describe framework use, not solved problems.
9. **QUALITY.md, Observability:** "traceparent forwarded" is true, but the sampled flag is always set and the server's span id is reused, so no store call shows up as its own span.

Where I agree: MSV-04 and the Kafka rules are not applicable. NFR-06 is backed by a recorded round trip. QUALITY.md reports the Q1 list p95 of 305 ms honestly as above target.

## 4. Findings for the next run

1. **TLS:** add a decision for TLS in transit in the local stack. At minimum, the key fetch should use HTTPS or a pinned key. Otherwise record an explicit accepted risk, so OWT-A04 becomes a decided deviation instead of a silent one.
2. **D88 and D129:** rewrite them so the Spring Security resource server or `JWKSourceBuilder` can meet them. For example: map a rate-limit exception to 401 and a key-source failure to 503. Then make the resource server the default in D94, and require a recorded reason for any own `OncePerRequestFilter` that handles tokens.
3. **D94:** require an instrumented HTTP client (for example Spring `RestClient` with the observation registry) for store and identity-provider calls. Forbid building `traceparent` by hand.
4. **REUSE.md template:** add a mandatory row for every class that reads or writes a standard format or protocol (query string, JSON, HTTP headers, JWT), and for ops scripts.
5. **Briefs:** require a short threat model (STRIDE table per entry point) as a deliverable. That makes SEC-01 and OWASP-05 checkable.
6. **Alerting:** decide a minimal alert path, such as a Prometheus rule on the 401/403 rate and on breaker-open state. This makes OWT-A09 and OWASP-10 meetable in a local stack.
7. **Ops scripts:** require `shellcheck`, `jq` for JSON, credentials from a file and never in a URL, and no `eval`.
8. **Gateway:** the nginx gateway should create and pass on `X-Correlation-Id` and log JSON (MSV-09).
9. **CDM:** decide whether the build ships a pinned copy of the CDM next to the OpenAPI (API-01, DATA-29), and who updates the CDM when a decision adds an attribute such as `createdBy` (DATA-26).
10. **Rules hygiene:**
    - CODE-11 (MUST) is overridden by a provisional decision (D135), and OPS-37 (MUST) by D95. Either downgrade or rescope these rules, or make the decisions final.
    - DATA-04 to DATA-16 judge the input CDM, not the build. Move them to the step that authors the CDM, so step 70 does not review what the build cannot change.
11. **Over-engineering check:** ask the code writer to justify each resilience mechanism against its actual call rate. The key-fetch breaker would not survive that test.
