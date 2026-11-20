# Design review

Build: `simpl-open-vocabulary-manager-run11` (Vocabulary Manager, Java 21 / Spring Boot 4.1.3, Fuseki, Keycloak, Vue UI behind nginx). I formed my verdicts from the code, configuration and docs, and read `DEVIATIONS.md`, `REUSE.md` and `QUALITY.md` only afterwards. I changed no files and started nothing.

## 1. In short

**Rules reviewed:** 153. These are all rules with `check: review` and step 70: 52 MUST and 101 SHOULD.

| Level | met | partly | not met | not applicable |
|---|---|---|---|---|
| MUST (52) | 28 | 19 | 1 | 4 |
| SHOULD (101) | 57 | 18 | 13 | 13 |
| **Total** | **85** | **37** | **14** | **17** |

| Prefix | met | partly | not met | n/a |
|---|---|---|---|---|
| DDD (2) | 2 | 0 | 0 | 0 |
| MACH (6) | 6 | 0 | 0 | 0 |
| MSV (5) | 4 | 0 | 0 | 1 |
| OWT / OWA (7) | 3 | 4 | 0 | 0 |
| REUSE (8) | 6 | 2 | 0 | 0 |
| NFR (6) | 4 | 2 | 0 | 0 |
| SOA (1) | 0 | 1 | 0 | 0 |
| API (21) | 12 | 3 | 2 | 4 |
| CODE (25) | 17 | 3 | 5 | 0 |
| ARCH (25) | 15 | 8 | 1 | 1 |
| DATA (28) | 13 | 7 | 3 | 5 |
| SEC (9) | 1 | 3 | 2 | 3 |
| OPS (6) | 2 | 1 | 1 | 2 |
| OWASP (3) | 0 | 3 | 0 | 0 |
| UX (1) | 0 | 0 | 0 | 1 |

**The five most important findings**

1. **Failed tokens are never limited (OWT-A07, MUST).** Every call is fully checked (signature, issuer, audience, expiry, not-before) through Nimbus in `security/TokenVerifier.java`. But nothing limits repeated failures: there is no gateway in the stack, no `limit_req` in `ui/nginx/nginx.conf`, and no limiter in the service. `DEVIATIONS.md` does not list OWT-A07.
2. **No threat model or security section (SEC-01, OWASP-05, MUST).** Security is decided piece by piece (D17, D44, D83, D84, D98, D46) and the code has default deny, fail closed and network isolation. But `docs/` has no threat model or security section, and `DEVIATIONS.md` is silent on it.
3. **User emails are stored and shown to every viewer (ARCH-20, OWT-A04, MUST).** `createdBy` holds the user's email (`TokenVerifier.user()`, D27). It sits unencrypted in the TDB2 volume and goes back to every viewer-role user. Logs correctly use `sub` (D80). The privacy side is not recorded anywhere.
4. **`REUSE.md` is incomplete against D93 (REUSE-07, MUST).** About 750 lines of own HTTP plumbing have no rows of their own: `web/ApiRoutes.java` (270 lines: own route matching, Allow headers), `NegotiationFilter` (158), `MethodGuardFilter`, and `Representations` (166, links built by string concatenation). Hand-written parsing also shows up: `QueryParameters.parse` splits on `&` and `=`, and `MultipartForm.translate` recognises size limits by matching exception class names.
5. **Recovery and load targets are only partly measured (NFR-06, NFR-13, MUST).** The backup and restore round trip passed, but on 68 quads, and the restore was never timed against the 30-minute recovery time objective. The Q1 load ran on 600 versions instead of the D99 seed of 20 000, about 3 % of the design data volume.

Also worth noting:
- **Possible TRACE echo (plausible, not proven).** `InfrastructureConfig.connectorCustomizer` sets `setAllowTrace(true)`, and `MethodGuardFilter` lets TRACE through on undeclared paths. An authenticated TRACE sent straight to a backend may reach `HttpServlet.doTrace` and echo the request. nginx blocks TRACE from outside; please check this inside the network in step 70.
- **Good reuse overall.** Token checks use Nimbus, Turtle parsing uses Jena RIOT, queries use `ParameterizedSparqlString`, the circuit breaker is Resilience4j, multipart goes through the container, and JSON through Jackson.

## 2. Verdicts

### DDD
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| DDD-03 | MUST | met | `docs/glossary.md` maps each term to its CDM class and API names. |
| DDD-14 | MUST | met | No terms from other contexts in the API; Schema Manager only consumes through searches (D45). |

### MACH
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| MACH-01 | SHOULD | met | Own Dockerfile and image per part; compose is only the local stack. |
| MACH-02 | MUST | met | Only calls to Fuseki SPARQL and Keycloak JWKS over HTTP (`SparqlClient`, `RealmKeys`). |
| MACH-03 | MUST | met | `docs/openapi-decided.yaml`; `DecidedContractTest`; `RouteRuleCheck` stops startup when a handler and a rule differ. |
| MACH-06 | SHOULD | met | Stateless sessions; writes are conditional (D82); per-instance state is only the key cache and the upload semaphore. |
| MACH-07 | MUST | met | Every value comes from the environment and secrets from `.env` (`stack.sh`). Nit: `application.yaml` defaults the issuer and JWKS to `http://localhost:8193`. |
| MACH-09 | SHOULD | met | `server.shutdown: graceful` with 30 s, `stop_grace_period: 35s`, two replicas, health checks. |

### MSV
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| MSV-02 | MUST | met | Fuseki on an internal `store` network, own users (D83). |
| MSV-04 | MUST | n/a | No events (D45). |
| MSV-06 | MUST | met | Timeouts 2/10/45 s, 2 read retries, Resilience4j breaker, 503 with Retry-After (`SparqlClient`, `InfrastructureConfig`). |
| MSV-09 | MUST | met | `CorrelationFilter` puts the id in the MDC; `TraceHeaders` forwards it to the store and Keycloak; nginx fills it with `$request_id`. |
| MSV-10 | SHOULD | met | `logging.structured.format.console: logstash`; Prometheus on the management port. |

### OWT / OWA
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| OWT-A02 | MUST | met | Actuator only on unpublished port 8081, whitelabel off, `include-stacktrace: never`, no CORS. Nit: Tomcat `setAllowTrace(true)`. |
| OWT-A04 | MUST | partly | Crypto through Nimbus and no secrets in code. But plain HTTP everywhere, an `http` issuer, an unencrypted volume holding emails, and the backup password in a `docker exec` command line. |
| OWT-A07 | MUST | partly | Full claim checks on every call and no tokens in URLs. No limit on repeated failures anywhere in the stack. |
| OWT-A08 | MUST | met | Fuseki jar pinned with `ADD --checksum`, images pinned by digest, uploads parsed to triples and never deserialised to objects. |
| OWT-A09 | MUST | partly | Auth failure and access denied logged at WARN, admin changes at INFO with userId (sub). No alert path (D105, no shipping). |
| OWT-A10 | MUST | met | Fails closed with 503 when keys are unavailable; one SPARQL update request per write; sanitised 500. |
| OWA-API8 | SHOULD | partly | Security headers on the UI server and Spring Security defaults on the API. No TLS; the contract states no CORS rule. |

### REUSE
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| REUSE-01 | MUST | met | Nimbus `DefaultJWTProcessor` and `DefaultJWTClaimsVerifier`; only the key fetch timing is own code (D108). |
| REUSE-02 | SHOULD | partly | RIOT, Jackson, Tomcat multipart, Jena RFC3986 and Spring `MediaType` are used. Hand-written: query-string split (`QueryParameters`), links and Content-Disposition by concatenation, the Basic auth header, and exception-name matching in `MultipartForm.translate`. |
| REUSE-03 | SHOULD | met | Fuseki, Keycloak and nginx; no own store, queue or token issuer. |
| REUSE-04 | MUST | met | OIDC bearer tokens, RFC 9457 problem+json, W3C traceparent. |
| REUSE-05 | SHOULD | met | The catalogue was checked; reasons given for the gateway, OpenBao, http-client and ui-components. |
| REUSE-06 | SHOULD | met | Patterns are named and actually used (compare-and-set, bulkhead, breaker, default deny); no over-engineering found. |
| REUSE-07 | MUST | partly | `REUSE.md` is broad, but D93 rows are missing for `ApiRoutes`, `NegotiationFilter`, `MethodGuardFilter` and `Representations`. |
| REUSE-09 | SHOULD | met | Framework extended only through customisers and filters; nothing vendored. |

### NFR / SOA
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| NFR-05 | SHOULD | met | Compose CPU and memory limits on every service, Tomcat threads at 100, LIMIT on list queries. Nit: `VocabularyRepository.references` has no LIMIT (D85 allows it). |
| NFR-06 | MUST | partly | Q3 targets, `ops/backup.sh`, `restore.sh` and a round trip that passed. The restore was not timed against the recovery time objective, and the round trip used a tiny dataset. |
| NFR-08 | SHOULD | met | JSON logs with trace and correlation ids, `http.server.requests` histograms, traceparent forwarded. |
| NFR-11 | SHOULD | met | Environment-only configuration, no host paths. |
| NFR-12 | SHOULD | met | Q4 load, D102 two replicas, and the state is named (the store). |
| NFR-13 | MUST | partly | `QUALITY.md` measures Q1 on a reduced seed (600 instead of 20 000 versions); availability and the recovery time objective were not measured. Reasons are given. |
| SOA-05 | SHOULD | partly | Cached keys keep reads working while Keycloak is down, but the contract does not say which operation depends on what. |

### API (mostly the published contract, which D39 keeps unchanged)
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| API-01 | MUST | met | `input/cdm/vocabulary-manager-be.md` plus the logical model in `docs/architecture.md`. |
| API-02 | SHOULD | partly | Schema names (`VocabularyCategory`, `VocabularyMetadata`) differ from the CDM classes; the glossary maps them. |
| API-03 | SHOULD | met | Spec written by hand; the decided copy is generated by script from it; no springdoc. |
| API-10 | SHOULD | partly | `/external-vocabulary/searches` is singular. |
| API-11 | SHOULD | met | GET, POST, PATCH and DELETE fit their actions; searches are POST controllers. |
| API-15 | SHOULD | met | No British spellings in property names. |
| API-19 | SHOULD | met | Business names (`namespaceUri`, `changelog`). |
| API-22 | SHOULD | met | Realistic examples (`HealthcareVocabulary`, Dublin Core). |
| API-28 | SHOULD | met | Collections, documents, and `/searches` controllers. |
| API-30 | SHOULD | met | `/vocabularies/{vocabularyName}`. |
| API-35 | SHOULD | met | Accept is set in `SparqlClient.post` and `RealmKeys.download`. |
| API-49 | SHOULD | partly | `info.description` mixes the business purpose with factory process text. |
| API-50 | SHOULD | not met | The identifier is `name` (D30, D39; the build records it). |
| API-51 | SHOULD | met | `latestVersionHref`, `contentHref`. |
| API-54 | SHOULD | not met | No identifier schema type such as `VocabularyName`; not recorded. |
| API-55 | SHOULD | n/a | No standard business identifier applies. |
| API-58 | SHOULD | met | `status` and `sort` declare an enum. |
| API-60 | SHOULD | met | Path parameters are `required: true`. |
| API-62 | MUST | n/a | No Kafka. |
| API-79 | SHOULD | n/a | No Kafka. |
| API-80 | SHOULD | n/a | No Kafka. |

### CODE
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| CODE-06 | MUST | met | Small focused methods (`SparqlClient`, `VocabularyService`). |
| CODE-09 | MUST | met | No flag-driven merged methods. |
| CODE-11 | MUST | not met | Concrete collaborators, no interfaces (D135; recorded). |
| CODE-14 | MUST | met | Identifiers in English. |
| CODE-15 | MUST | met | Comments in English. |
| CODE-16 | MUST | met | Readable code; decision ids cited in Javadoc. |
| CODE-17 | MUST | met | Clear names. |
| CODE-20 | MUST | met | MDC carries vocabularyName, version, userId and operationId; error paths log what failed. |
| CODE-21 | SHOULD | not met | No Log4J wrapper (D105; recorded). |
| CODE-24 | MUST | met | 401 and 403 at WARN, store 5xx and unexpected failures at ERROR, changes at INFO. |
| CODE-25 | SHOULD | met | ERROR only for store faults and unexpected exceptions. |
| CODE-30 | SHOULD | met | MDC fields (D96). |
| CODE-32 | SHOULD | met | Logs carry `sub`, never email (D80). |
| CODE-33 | SHOULD | not met | Own exception class (D104; recorded). |
| CODE-34 | MUST | met | `GlobalExceptionHandler` plus `ProblemWriter` for the filters. |
| CODE-35 | MUST | met | Own `ApiException`, Spring MVC and multipart exceptions, IO and Interrupted, and Nimbus exceptions are all mapped. |
| CODE-36 | MUST | met | The `ProblemType` enum gives status, type and title per code. |
| CODE-44 | SHOULD | met | `ProblemType` serves as the error-code enum. |
| CODE-45 | SHOULD | not met | No gateway (D17; recorded). |
| CODE-46 | SHOULD | met | Resilience4j around the store and the key fetch. |
| CODE-48 | SHOULD | met | Constructor injection; HTTP clients and breakers are beans. |
| CODE-50 | SHOULD | partly | JSON to stdout is ready for a collector; no shipping. |
| CODE-52 | SHOULD | not met | Logstash layout, not the Simpl format (D105). |
| CODE-54 | MUST | partly | Guidelines followed except CODE-11 and CODE-39. |
| CODE-55 | SHOULD | partly | Spotless, ESLint, Prettier and vue-tsc are configured. No linter for shell, Dockerfiles, nginx or the Python tools. |

### ARCH
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| ARCH-01 | SHOULD | met | No central dependency. |
| ARCH-02 | MUST | met | Only the published API is used by others. |
| ARCH-04 | SHOULD | met | Fuseki and Keycloak only. |
| ARCH-05 | SHOULD | met | No SPARQL or storage details in the schemas. |
| ARCH-07 | MUST | partly | Two backend replicas; Fuseki and Keycloak are single. |
| ARCH-08 | SHOULD | met | EUPL `LICENSE`, open spec. |
| ARCH-09 | SHOULD | met | No vendor SDK. |
| ARCH-11 | SHOULD | met | Search and content operations can be reused by other consumers. |
| ARCH-13 | SHOULD | met | `docs/api-notes.md` describes the contract only. |
| ARCH-14 | SHOULD | partly | Limits are compose variables; there is no Helm chart. |
| ARCH-17 | MUST | partly | Strong access control, store isolated; no TLS. |
| ARCH-18 | MUST | partly | Validation, single-request transactions, compare-and-set and the SHACL test are there; no integrity protection in transit. |
| ARCH-19 | MUST | partly | Backups and restore tested; the store is a single instance. |
| ARCH-20 | MUST | partly | Email stored as `createdBy`, unencrypted, and shown to every viewer. |
| ARCH-22 | SHOULD | not met | No registry (recorded). |
| ARCH-23 | SHOULD | partly | README and docs exist; `info.description` is weak. |
| ARCH-24 | SHOULD | partly | Own image; no pipeline and no chart. |
| ARCH-25 | SHOULD | met | One capability. |
| ARCH-26 | SHOULD | met | The store has one user service. |
| ARCH-27 | SHOULD | n/a | Pull only, by decision D45. |
| ARCH-28 | SHOULD | met | Synchronous publish is decided (D6); bulkhead bounds it. |
| ARCH-30 | SHOULD | met | Two roles; Fuseki reader, updater and backup users; non-root containers. |
| ARCH-31 | SHOULD | met | nginx limits, service authorisation, input checks, Fuseki auth, internal network. |
| ARCH-33 | SHOULD | met | Bounded own retries (`READ_RETRY_DELAYS_MS`, conditional loop). |
| ARCH-34 | SHOULD | met | Breaker as in D103. |

### DATA (CDM rules judged on the input CDM; the build's logical model corrects most points)
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| DATA-01 | SHOULD | met | Input CDM present. |
| DATA-02 | SHOULD | met | Logical model in `docs/architecture.md`. |
| DATA-03 | SHOULD | partly | SHACL shapes in `physical-model.ttl` instead of a diagram (D87). |
| DATA-04 | SHOULD | partly | The CDM has the `VocabularySearch` classes. |
| DATA-05 | SHOULD | met | No UUIDs or audit fields. |
| DATA-06 | SHOULD | met | Relations shown as associations. |
| DATA-07 | SHOULD | not met | CDM status and warningCode have no values listed; the logical model lists them. |
| DATA-08 | SHOULD | met | Verb labels. |
| DATA-09 | SHOULD | met | Mermaid UML class diagram. |
| DATA-10 | SHOULD | not met | CDM attributes untyped; the logical model types them (D117). |
| DATA-11 | SHOULD | met | No boolean flags. |
| DATA-12 | SHOULD | partly | `creationDate` and `lastUpdateDate` are generic names (glossary decided). |
| DATA-13 | SHOULD | met | Multiplicities at both ends. |
| DATA-14 | SHOULD | not met | Search request classes in the CDM. |
| DATA-15 | SHOULD | partly | Same cause as DATA-14. |
| DATA-16 | SHOULD | partly | `description` appears on both the vocabulary and the version (D28). |
| DATA-17 | SHOULD | met | Logical model is a classDiagram. |
| DATA-19 | SHOULD | met | Types and keys marked. |
| DATA-20 | SHOULD | met | Multiplicities shown. |
| DATA-21 | SHOULD | n/a | No many-to-many links with data. |
| DATA-22 | SHOULD | n/a | RDF store, no tables (D87). |
| DATA-23 | SHOULD | n/a | Same. |
| DATA-24 | SHOULD | n/a | Same. |
| DATA-25 | SHOULD | n/a | Same. |
| DATA-26 | MUST | partly | `createdBy` was added to the API (D27) but not to the CDM; listed only as an owner question. |
| DATA-27 | MUST | met | `openapi-decided.yaml` carries the D79 changes. |
| DATA-28 | MUST | met | Docs updated. |
| DATA-29 | MUST | partly | CDM, published API and decided API live in three places with no shared version. |

### SEC / OPS / OWASP / UX
| Rule | Level | Verdict | Evidence |
|---|---|---|---|
| SEC-01 | MUST | partly | Security is in the decisions; no security section in the design docs. |
| SEC-02 | MUST | n/a | The guideline document is not available. |
| SEC-07 | SHOULD | partly | OIDC through Keycloak, but without the custom authenticator. |
| SEC-11 | SHOULD | not met | No ELK shipping (D105). |
| SEC-12 | SHOULD | not met | No gateway (D17). |
| SEC-13 | SHOULD | n/a | No Tier 2. |
| SEC-15 | SHOULD | n/a | No agent-to-agent calls. |
| SEC-22 | SHOULD | partly | Technical store users exist; Keycloak bootstrap user is `admin` in start-dev. |
| SEC-23 | SHOULD | met | Realm roles assigned to users. |
| OPS-01 | SHOULD | met | No OVH services. |
| OPS-02 | SHOULD | met | Synthetic fixtures. |
| OPS-03 | SHOULD | n/a | No production data. |
| OPS-17 | SHOULD | n/a | No GitLab repository in the factory. |
| OPS-29 | SHOULD | not met | No chart. |
| OPS-37 | MUST | partly | Spotless runs in `mvn verify` and ESLint/Prettier in `npm run build`; no pipeline (D95). |
| OWASP-01 | MUST | partly | Gaps in TLS, rate limiting and threat model. |
| OWASP-05 | MUST | partly | No threat model. |
| OWASP-10 | MUST | partly | Events are logged; no monitoring or alerting. |
| UX-01 | MUST | n/a | Guidelines (Figma) not available. |

## 3. Disagreements with the build's own documents

**Where I find gaps the build does not report:**
- **OWT-A07:** repeated token failures are not limited. Not in `DEVIATIONS.md`.
- **OWT-A09 and OWASP-10:** there is no alert path. `DEVIATIONS.md` covers only log shipping (SEC-11, CODE-50), not alerting on security events.
- **SEC-01 and OWASP-05:** there is no threat model. Not recorded.
- **ARCH-20:** email as `createdBy` (stored unencrypted, visible to all viewers) is not treated as a privacy question.
- **REUSE-07 and D93:** `REUSE.md` claims full coverage, but four own HTTP plumbing classes have no row. Hand-written query-string and link building is missing from the REUSE-02 view.
- **NFR-06:** `QUALITY.md` presents Q3 as covered by design. The restore time was never measured, and the round trip used 68 quads.
- **DATA-26 and DATA-29:** CDM drift (`createdBy`, the closed warning code set) is in `architecture.md` as owner questions, but not in `DEVIATIONS.md`.
- **`QUALITY.md` Q5** says the D85 LIMITs hold. True for list queries, but `references()` runs without a LIMIT. D85 allows this, so it is minor.

**Where the build is stricter than me:**
- **OWT-A04:** the build says not followed; I say partly, because crypto and secret handling are right.
- **ARCH-07:** not followed per the build; partly in my view, since the backend runs two replicas.
- **OPS-37 and CODE-50:** not followed per the build; partly in my view, because lint runs on the finish line and the logs are ready for a collector.

**Where we agree:** the overrides by decision (D104, D105, D135, D17, D48, D87, D39) are recorded correctly. The D120 note on the management port is accurate.

## 4. Findings for the next run

1. **Require a short security section in `docs/`** (threats, controls, what is left to the platform) on the finish line. That makes SEC-01 and OWASP-05 checkable.
2. **Decide how failed tokens are limited.** Either nginx `limit_req` in the local stack, or an explicit decision that the gateway owns it, and then make the build list OWT-A07.
3. **Decide on personal data.** Choose `createdBy` = email or `sub`, mark it as personal data in the CDM, and decide whether viewers may see it (ARCH-20, CODE-32).
4. **Check D93 mechanically.** A step-70 script should list main classes over 100 lines in `web/`, `store/` and `security/` and require a `REUSE.md` row for each.
5. **Settle E6 against `build.yaml`** on the UI component stack: PrimeVue and Tailwind against "plain components".
6. **Give the load test a bulk seeder** (a `tdbloader` dataset) so the D99 seed fits the run. Add a timed restore to measure the recovery time objective.
7. **Set Tomcat `allowTrace` to false**, or add a decision on TRACE handling inside the network, and have step 70 test TRACE directly on a backend.
8. **Adjust `rules.json`:**
   - Mark DATA-22 to DATA-25 as relational only.
   - Split the CDM rules (DATA-04 to 16) into input-CDM findings for the CDM owner and build logical-model checks.
   - Tag the API naming rules on a fixed published contract (API-10, 50, 54) as contract-owner findings, so they do not count against the build.
9. **Ask for linters on shell scripts and Dockerfiles** (shellcheck, hadolint) to close CODE-55.
