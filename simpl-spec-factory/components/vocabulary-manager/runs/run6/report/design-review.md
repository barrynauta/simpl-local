# Design review

Build: `simpl-open-vocabulary-manager-run6/build/work`. Scope: the 153 rules with `check: review` and step 70 in `steps` (52 MUST, 101 SHOULD). I formed each verdict from the code before reading `DEVIATIONS.md`, `REUSE.md` and `QUALITY.md`.

## 1. In short

**Verdicts per level**

| Level | Met | Partly | Not met | Not applicable | Total |
|---|---|---|---|---|---|
| MUST | 22 | 20 | 7 | 3 | 52 |
| SHOULD | 28 | 11 | 16 | 46 | 101 |
| **All** | 50 | 31 | 23 | 49 | 153 |

**Verdicts per prefix** (met / partly / not met / not applicable)

DDD 1/1/0/0 · MACH 3/3/0/0 · MSV 4/0/0/1 · OWT 2/3/1/0 · OWA 0/1/0/0 · REUSE 4/4/0/0 · NFR 1/4/1/0 · SOA 1/0/0/0 · API 1/0/0/20 · CODE 13/4/7/1 · ARCH 15/6/3/1 · DATA 1/0/8/19 · SEC 2/2/1/4 · OPS 2/0/1/3 · OWASP 0/3/0/0 · UX 0/0/1/0

**The five most important findings**

1. **Email addresses go into the INFO logs on every write.** The creator (the email claim, decision D27) is logged at `VocabularyService.java:108,143,173,197` and `ExternalVocabularyService.java:95,111`. This breaks CODE-32 and touches ARCH-20, OWT-A09 and OWASP-01. The build's own documents do not mention it, and `QUALITY.md` says security logging has no sensitive data.
2. **Only one instance can run.** The per-vocabulary locks live in memory (`VocabularyLocks.java`, 256 lock stripes), so ARCH-07 (MUST) and MACH-06 are not met. Decision D43 allows this, and `DEVIATIONS.md` says so honestly. A conditional SPARQL update on `vm:latestVersion` would make more replicas safe.
3. **The restore is not proven, and it can leave the store empty.** `ops/restore.sh` runs `DROP ALL` first and then loads through `/tmp`, which is a 64 MB tmpfs (`compose.yaml`, fuseki service). Content is stored as base64 inside a literal, about 7 MB per 5 MB upload, so a real dataset fails half-way and leaves an empty store. The restore test in `QUALITY.md` used 169 quads. NFR-06 and OWT-A10 are partly met.
4. **One upload can trigger an unbounded number of store calls.** `ReferenceChecker.check` (lines 71 to 84, 107 to 135) runs 2 or 3 SPARQL queries for each internal prefix declaration, with no cap and while holding the vocabulary lock. A 5 MB file can hold thousands of prefixes. The warnings list is not capped either (only errors are, `ContentValidator.java:82`). NFR-05 is partly met, while `QUALITY.md` says resources are bounded.
5. **Simpl conventions for resilience, observability and the UI are missing.**
   - There is no circuit breaker (CODE-46, ARCH-34).
   - No trace context is passed on (NFR-08).
   - Logs use Spring's ECS format, not the Simpl log wrapper or the common Simpl log format (CODE-21, CODE-52), and no log aggregation is set up (CODE-50, SEC-11).
   - The UI does not use `participant-fe-components` from the reuse catalogue (UX-01 is a MUST; REUSE-05 is also affected).

   Error routing is also over-built: `RouteTable`, `MethodNotAllowedFilter`, `RequestRejectedHandler`, `ProblemErrorReportValve` and `ProblemErrorController` give five ways to produce an error response, and `RouteTable` repeats the controller mappings by hand, so a new endpoint has to be added in two places.

## 2. Verdicts

### DDD

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| DDD-03 | MUST | partly | A glossary with CDM classes exists in `input/decisions/glossary.md`, but the build does not deliver a glossary for the component; `docs/` has none. |
| DDD-14 | MUST | met | The component consumes no other context. The mapping from domain to wire happens in one place, `api/Representations.java`. |

### MACH

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| MACH-01 | SHOULD | met | The backend has its own Dockerfile and image. Its only runtime dependencies are Fuseki and Keycloak. |
| MACH-02 | MUST | met | Fuseki is reachable only on the internal network (`compose.yaml`, no published port). The UI goes through `/v1`. |
| MACH-03 | MUST | partly | The OpenAPI file exists, but the behaviour decided in D39 (401/403, 503, createdBy, limits) is in prose only (`docs/api-notes.md`). No updated contract is shipped. |
| MACH-06 | SHOULD | partly | The service is stateless apart from the in-memory locks (`VocabularyLocks.java`) and the cached realm keys. |
| MACH-07 | MUST | partly | Every backend value comes from the environment (`application.yaml`). The realm file holds test passwords, and Keycloak runs `start-dev` with an embedded H2 database. |
| MACH-09 | SHOULD | met | `server.shutdown: graceful`, a 20 s shutdown phase and `stop_grace_period: 30s`. The stack starts in 34 s. |

### MSV

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| MSV-02 | MUST | met | The service owns its Fuseki dataset; no other service uses it. |
| MSV-04 | MUST | not applicable | No events (D45). |
| MSV-06 | MUST | met | `SparqlClient.java`: connect and response timeouts, 2 read retries, no write retries, 503 with Retry-After. `RealmKeySource.java`: fails closed with 503. |
| MSV-09 | MUST | met | `CorrelationIdFilter.java` puts the id in the MDC, so it reaches every log line. `SparqlClient.java:142-145` passes it to the store. |
| MSV-10 | SHOULD | met | ECS structured logs; Prometheus metrics on the management port. |

### OWT and OWA

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| OWT-A02 | MUST | partly | The backend is clean: CORS off, no stack traces, actuator on internal port 8081. Keycloak runs `start-dev` on published port 8193. |
| OWT-A04 | MUST | not met | HTTP everywhere and `sslRequired: none` in the realm. No encryption at rest. Self-reported. |
| OWT-A07 | MUST | met | Nimbus checks signature, issuer, audience and expiry (`SecurityConfig.java` `jwtDecoder`). Keycloak brute-force protection is on. The API rate limit is left to the gateway by D46. |
| OWT-A08 | MUST | met | The Fuseki jar is checked with `ADD --checksum` and images are pinned by digest. Turtle is parsed into a graph, and Jackson builds a tree, not typed objects. |
| OWT-A09 | MUST | partly | The `security` logger records 401, 403 and firewall events. Keycloak `eventsEnabled` is not set, so login failures are not recorded. No alerting. Emails go into the logs (finding 1). |
| OWT-A10 | MUST | partly | Fails closed (`RealmKeySource.get`), and each write is one SPARQL update. `ops/restore.sh` can leave an empty store (finding 3). |
| OWA-API8 | SHOULD | partly | nginx sets CSP, nosniff and frame headers, and Spring Security adds its default headers. The API does not state "TLS only" anywhere. |

### REUSE

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| REUSE-01 | MUST | met | Nimbus does the JOSE work. `RealmKeySource` only plugs in through the `JWKSource` extension point. SHA-256 comes from the JDK. |
| REUSE-02 | SHOULD | met | Jena RIOT (Turtle), Jackson (JSON), servlet multipart, `UriComponentsBuilder` (query strings). One minor exception: aliases are stored as a newline-joined literal (`SparqlVocabularyStore.java` `createExternal`). |
| REUSE-03 | SHOULD | partly | Products are reused (Fuseki, Keycloak). At library level the build writes its own SPARQL protocol client (`SparqlClient.java`, where Jena's HTTP query execution exists), its own JWK cache (Nimbus `JWKSourceBuilder` offers caching and outage tolerance) and its own route table that repeats Spring MVC's. |
| REUSE-04 | MUST | met | HTTP with the OpenAPI file, OIDC with PKCE, JSON, RFC 9457 problem shape. |
| REUSE-05 | SHOULD | partly | `REUSE.md` does not mention the catalogue entries `ui-components` (participant-fe-components), `participant-shared-code`, or the circuit breaker in `architectural-patterns`. |
| REUSE-06 | SHOULD | partly | Patterns are named in `REUSE.md`. The circuit breaker, a recurring problem the catalogue names, is missing. Some names only label framework use ("chain of responsibility", "DTO"). The five-way error routing is over-built. |
| REUSE-07 | MUST | partly | `REUSE.md` exists, but the omissions under REUSE-05 make it incomplete. |
| REUSE-09 | SHOULD | met | No framework code is copied or patched. Extensions go through `JWKSource`, `ObjectPostProcessor` and a Tomcat valve. |

### NFR and SOA

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| NFR-05 | SHOULD | partly | Memory and CPU limits on every container; pages capped at 100. Unbounded per-upload store calls and an uncapped warnings list (finding 4). |
| NFR-06 | MUST | partly | Targets are stated (`QUALITY.md`, invented by the writer). The restore was tested with 169 quads, and the script is fragile (finding 3). |
| NFR-08 | SHOULD | partly | Correlation id and per-URI metrics are present. No trace context (self-reported). |
| NFR-11 | SHOULD | met | Configuration comes only from environment variables. Read-only root filesystems. |
| NFR-12 | SHOULD | not met | No designed load (users, requests per second, data volume) is stated anywhere. Only latencies were measured. |
| NFR-13 | MUST | partly | The availability target and the RTO are neither measured nor listed as "not measured". |
| SOA-05 | SHOULD | met | Cached keys keep requests working while Keycloak is down. The 503 on store loss is documented (`api-notes.md`). |

### API

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| API-35 | SHOULD | met | `ui/src/api.ts:71`, `SparqlClient.java:140` and `RealmKeySource.java` all send Accept. |
| API-01, API-62 | MUST | not applicable | The OpenAPI file and CDM are inputs the writer may not edit (D39, D2). No Kafka (D45). |
| API-02, 03, 10, 11, 15, 19, 22, 28, 30, 49, 50, 51, 54, 55, 58, 60 | SHOULD | not applicable | These rules judge the input OpenAPI file, which the build may not change (D39). |
| API-79, API-80 | SHOULD | not applicable | No topics. |

### CODE

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| CODE-06 | MUST | partly | Controller methods parse, check about 10 rules and build the response (`VocabularyController.create` and others). Service methods are focused. |
| CODE-09 | MUST | met | Similar checks stay separate (for example `checkFileSize` and `checkNames`). |
| CODE-11 | MUST | partly | The service depends on the `VocabularyStore` port. The validator, locks, `SparqlClient` and the services are concrete classes. |
| CODE-14, 15, 16, 17 | MUST | met | Identifiers and comments are in English, with clear names and Javadoc throughout. |
| CODE-20 | MUST | met | Correlation id, operation, entity name and failure cause are logged. |
| CODE-21 | SHOULD | not met | Logback ECS, not the Log4J wrapper. |
| CODE-24, CODE-25 | MUST / SHOULD | met | ERROR only for store failures and unexpected errors (`GlobalExceptionHandler.java`). 4xx at INFO. |
| CODE-30 | SHOULD | met | Correlation id in the MDC; entity names in messages. |
| CODE-32 | SHOULD | not met | Emails in INFO logs (finding 1). |
| CODE-33 | SHOULD | not met | Own `ProblemException` hierarchy instead of the shared Simpl framework. |
| CODE-34, 35, 36 | MUST | met | One `@RestControllerAdvice` maps the component's own, Java and Spring exceptions to defined problems, with a catch-all 500. |
| CODE-44 | SHOULD | not met | No error-code enum. Problems are built by factory methods with free-text details (`Problems.java`). |
| CODE-45 | SHOULD | not applicable | No gateway in the local stack; D17 makes the service enforce access. |
| CODE-46 | SHOULD | not met | No circuit breaker on the Fuseki or Keycloak calls. |
| CODE-48 | SHOULD | met | Constructor injection throughout. |
| CODE-50, CODE-52 | SHOULD | not met | No log aggregation; not the common Simpl format. |
| CODE-54 | MUST | partly | Spotless (Palantir) and Prettier are present. The Dockerfile skips the Spotless check, and there is no pipeline that enforces it. |
| CODE-55 | SHOULD | partly | Formatting rules exist. No lint rules (no ESLint, no Checkstyle or PMD). |

### ARCH

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| ARCH-01, 02, 04 | S / M / S | met | No central dependencies; the only dependencies are Fuseki and Keycloak. |
| ARCH-05 | SHOULD | met | The storage model (store namespace, base64 literals) never appears on the wire. |
| ARCH-07 | MUST | not met | Single instance because of the in-memory locks (finding 2). Self-reported. |
| ARCH-08, 09, 11 | SHOULD | met | EUPL licence, open-source stack, search operations that other services can compose. |
| ARCH-13 | SHOULD | partly | `api-notes.md` describes the wire format, not how participants interoperate. |
| ARCH-14 | SHOULD | met | `MaxRAMPercentage` plus container limits. |
| ARCH-17 | MUST | partly | Access control is good. No TLS, no encryption at rest, and Fuseki accepts unauthenticated `--update` from anything on the stack network. |
| ARCH-18 | MUST | met | Atomic SPARQL updates, SHA-256 identity check, validation before storing. |
| ARCH-19 | MUST | partly | Single instance; fragile restore. |
| ARCH-20 | MUST | partly | Emails in logs. The email shown to viewers as `createdBy` is decided (D27). |
| ARCH-22 | SHOULD | not applicable | No registry in a local build. |
| ARCH-23, 24, 25, 26 | SHOULD | met | `docs/README.md`; separate image; one capability; REST only. |
| ARCH-27 | SHOULD | not met | No events, by decision D45. |
| ARCH-28 | SHOULD | met | Uploads need an immediate validation answer. |
| ARCH-30 | SHOULD | partly | Two roles (D16). Fuseki has no credentials at all. |
| ARCH-31 | SHOULD | partly | Network isolation and service checks. No gateway; no store authentication. |
| ARCH-33 | SHOULD | met | Reads retried after 200 ms and 400 ms (`SparqlClient.java:94-108`). |
| ARCH-34 | SHOULD | not met | No circuit breaker. |

### DATA

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| DATA-01, 04 to 16 | SHOULD | not applicable | The CDM is an upstream input (D2), not a build artefact. |
| DATA-02, DATA-03 | SHOULD | not met | The storage model is one prose paragraph (`architecture.md`). No logical or physical model of the RDF store. |
| DATA-17, 19, 20 | SHOULD | not met | No logical data model. |
| DATA-21 to 25 | SHOULD | not applicable | There are no tables or many-to-many associations in a triple store. |
| DATA-26, DATA-27 | MUST | not met | Domain changes (createdBy, description on the version, warnings) are not in the CDM or the OpenAPI file. D2 and D39 forbid the edit. |
| DATA-28 | MUST | met | `api-notes.md` and `architecture.md` describe the changes. |
| DATA-29 | MUST | not met | There is no single source for the CDM and the API. |

### SEC

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| SEC-01 | MUST | partly | Abuse cases are in D46 and failure modes in D44. The build has no threat model. |
| SEC-02 | MUST | partly | Good practice is visible (parameterised SPARQL, size limits, strict JSON). No SAST, and the guideline document cannot be checked. |
| SEC-07 | SHOULD | met | OIDC through Keycloak; keycloak-js with PKCE. |
| SEC-11 | SHOULD | not met | No ELK feed. |
| SEC-12, 13, 15, 22 | SHOULD | not applicable | No gateway, no agent calls, no agent administration in this component. |
| SEC-23 | SHOULD | met | Rights belong to realm roles, and roles are given to users. |

### OPS, OWASP and UX

| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| OPS-01, OPS-02 | SHOULD | met | No OVH managed services; the tests use synthetic data. |
| OPS-03, 17, 29 | SHOULD | not applicable | No production data, no repository, no Helm chart. |
| OPS-37 | MUST | not met | No pipeline. Self-reported. |
| OWASP-01 | MUST | partly | The gaps are A02 (no TLS), A05 (Keycloak dev mode) and A09 (logging, PII). |
| OWASP-05 | MUST | partly | Decisions cover abuse cases. No threat model document. |
| OWASP-10 | MUST | partly | Events are logged but not monitored; Keycloak login events are off. |
| UX-01 | MUST | not met | Plain components; the catalogue's `participant-fe-components` is not used. |

## 3. Disagreements with the build's own documents

**The build reports these as met, or says nothing; I find them partly or not met**

- **CODE-32, ARCH-20, OWT-A09:** emails go into INFO logs on six write paths. `DEVIATIONS.md` does not mention it, and `QUALITY.md` says security events are logged "without token contents", which suggests the logs are clean.
- **NFR-05:** `QUALITY.md` says resources are bounded. The reference check fans out to the store with no cap, and the warnings list is uncapped.
- **NFR-06:** `QUALITY.md` says the restore was tested. It was tested with 169 quads. The 64 MB tmpfs and the `DROP ALL`-then-load order make it fail on a realistic dataset and leave the store empty.
- **REUSE-05, REUSE-07:** `REUSE.md` says everything the catalogue covers is reused. It skips `ui-components`, `participant-shared-code` and the circuit breaker pattern. It also does not mention its own JWK cache next to Nimbus `JWKSourceBuilder`, or its own SPARQL HTTP client next to Jena's.
- **Rules `DEVIATIONS.md` leaves out:** CODE-21, 33, 44, 46, 50 and 52; ARCH-34; NFR-12; DATA-02, 03, 26, 27 and 29; SEC-11; MACH-03 (contract drift beyond D39); SEC-01 and OWASP-05 (no threat model); OWT-A02 (Keycloak `start-dev` on a published port).

**Levels written wrongly in `DEVIATIONS.md`**

- UX-01 is listed as SHOULD; `rules.json` says MUST.
- ARCH-07 is listed as SHOULD in the second row; it is MUST.

**Where I agree with the self-report**

OWT-A04, ARCH-07, MACH-06, MACH-07, NFR-08, OPS-37, OWASP-10 and UX-01 are honestly reported as gaps.

**Where the build is stricter than needed**

- Listing ARCH-07 for single Fuseki and Keycloak containers is fair for a local stack, but it is not a defect of the component.

## 4. Findings for the next run

1. **Build profile against the rules.** `build.yaml` asks for "plain component library", which conflicts with UX-01 (MUST) and the catalogue's `ui-components`. Either name participant-fe-components in the profile, or add a decision that waives UX-01 with a reason.
2. **D43 and ARCH-07.** D43 makes a single instance acceptable, and that guarantees a MUST failure. Replace it with a store-side optimistic check (a conditional update on `vm:latestVersion`, where an empty update means retry or 409) so that replicas are safe.
3. **Log privacy.** Add a decision for D27: log the token subject, not the email. Add a review hint to CODE-32: "grep log calls for creator or user values".
4. **Rule scope for step 70.** About 40 rules (API-02 to API-60, DATA-01 to DATA-16) judge input artefacts that the writer may not change (D2, D39). Tag them `applies_to: contract` and review them at steps 21 and 23, not here.
5. **NFR targets.** Decisions should give load, availability, RTO and RPO (NFR-06, NFR-12). This build invented its own. NFR-13 should require that each target is either measured or listed as not measured.
6. **Bound the work per request.** Add to NFR-05: "per-request outbound calls are bounded" (reference fan-out, search at 2 times 100 sequential queries).
7. **Restore test at size.** The restore test should use a dataset at the upload limit, and the restore should load into a new dataset before it swaps, never `DROP ALL` first.
8. **Generate `DEVIATIONS.md` levels.** Copy the level from `rules.json` by tool, and require one row per MUST that is partly or not met.
9. **Make `REUSE.md` checkable.** Require one row per catalogue capability, including UI, shared libraries and patterns.
10. **Resilience and logging conventions.** Decide whether a circuit breaker (Resilience4j) is expected for the store and the identity provider. Point to the Simpl log wrapper, or waive CODE-21 and CODE-52 for factory builds.
11. **Keycloak settings.** The profile should turn on realm events (`eventsEnabled`) and either use production mode or record dev mode as a decision.
12. **Pipeline.** The factory could ship a minimal CI template, so that OPS-37 and CODE-54 can be met.
