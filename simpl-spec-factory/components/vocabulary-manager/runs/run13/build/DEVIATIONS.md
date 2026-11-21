# Deviations

Rules of `input/rules.json` this build does not follow, and decisions it could not implement as written. A decision that overrides a rule is listed with both.

## Rules overridden by a decision

| Rule | Level | What the build does | Decision |
|---|---|---|---|
| API-46 (problem type URN 'urn:problem-type:simpl:') | SHOULD | 400, 404, 500 and 503 keep the Belgif types `urn:problem-type:belgif:...`; 401, 403, 405, 406, 409, 413, 415 use `urn:problem-type:simpl:...`. | D48 |
| CODE-11 (depend on interfaces) | MUST | No interface for a class with one implementation; classes depend on the concrete collaborator, injected by Spring. | D135 |
| CODE-21, CODE-52 (Log4J wrapper, Simpl log format), SEC-11 (logs to central ELK) | SHOULD | JSON lines in Logstash format from Spring Boot structured logging (D96); no LogWrapper, no log shipping. | D105 |
| CODE-33 (shared exception framework) | SHOULD | One own exception base class `ApiException` with `ProblemType` as error code enum; no shared Simpl library. | D104 |
| OWT-A07 (repeated token failures limited) | MUST | Not limited in the service; token failures are logged on the `security` logger. Limiting belongs to the Tier 1 gateway. | D164 |
| API-25 (HTTPS only), OWT-A04 and OWASP-03 (TLS in transit, encryption at rest) | SHOULD / MUST | The local stack uses plain HTTP between containers and on the host ports; the store is not encrypted at rest. TLS termination and encryption at rest belong to the platform. createdBy is personal data and is marked so in the logical model. | D165 (and E2 for the local URLs) |
| SEC-12, CODE-45 (authorisation at the Spring Cloud Gateway, clients only through the gateway) | SHOULD | No gateway in the stack; the service validates tokens and roles itself. | D17 |
| DDD-04 (same value sets in CDM and API) | MUST | The warning codes are the closed set of D10 (`externalReferenceSkipped`, `deprecatedVocabularyReference`, `semanticReasoningSkipped`); the CDM still lists `syntaxError` and `semanticError`. | D10 |
| DDD-06, DDD-08 (API names equal CDM names) | MUST / SHOULD | Wire names of the published contract are kept: `VocabularyCategory` for InternalVocabulary, `VocabularyMetadata` for InternalVocabularyVersion, `version` for latestVersion. The code uses the glossary terms. | D25, D26, D39 |
| API-06, API-24, API-50, API-52, API-54 (path segment case, URI form, identifier naming) | SHOULD | Paths, the `/v1` base and the name-based identifiers are those of the published contract, which writers do not change. | D39, E2 |
| DATA-18 (LDM attributes in snake_case) | SHOULD | The logical model uses the glossary and CDM names (`creationDate`, `lastUpdateDate`). | D117 |
| DATA-22 to DATA-25 (PDM as UML class diagram with tables and keys) | SHOULD | The physical model of the RDF store is a set of SHACL shapes per named graph, checked by an own test. | D87 |
| SOA-09 (no overlapping paths) | rule of the project | `/vocabularies/searches` and `/external-vocabularies/content` overlap a path template; the fixed segment wins. | D77 |

## Rules not followed for other reasons

| Rule | Level | Gap | Reason |
|---|---|---|---|
| OPS-05, OPS-06, OPS-24, OPS-33, OPS-35, OPS-37 (SAST, Sonar, pipeline gates) | SHOULD / MUST | No CI pipeline, no SAST or Sonar run. Formatter check (Spotless) runs in `mvn verify`; ESLint, Prettier and Vitest run in the UI image build and fail it on errors. | The factory has no CI (D95 says a pipeline file is not required). No SAST tool is installed on this machine. |
| OPS-07, OPS-08, OPS-14, OPS-26 (pipeline scans, SBOM in pipeline) | SHOULD | Done by hand in the build step: Trivy on the work tree and every image, CycloneDX SBOMs in `sbom/`. | No pipeline. |
| OPS-10, OPS-11, OPS-13, OPS-27 to OPS-30, OPS-25 (Kubernetes network policies, secrets, service accounts, Helm chart, registry) | SHOULD | Docker Compose only; secrets in a generated `.env` file. | The build profile fixes a Compose stack. |
| NFR-09 (mutation score, static analysis, complexity limit) | SHOULD | Line and branch coverage measured (94.2 %, 83.4 %); no mutation testing, no static analyser, no complexity limit enforced. | No tool for it in the build profile. |
| OWASP-07, OWT-A03 (no dependency or image with known HIGH vulnerabilities) | MUST | The Keycloak 26.8.2 image keeps 6 HIGH findings (5 without a fixed version, 1 in a bundled guava). Own images: 0. | Keycloak is a reused product (E1); its image cannot be patched without forking it (REUSE-09). Details in `SECURITY-SCAN.md`. |
| OWT-A09 (security events can raise an alert) | MUST | Access denials and token rejections are logged on the `security` logger and counted in the request metrics by status; no alert rule is shipped. | No monitoring backend in the stack. |
| ARCH-07 (no single point of failure) | MUST | The backend runs as two replicas; Fuseki and Keycloak run as one instance each. | Local stack; the store is one TDB2 dataset by design (E3). |
| ARCH-22 (service registry) | SHOULD | Not registered anywhere. | No registry in the stack. |
| BLG-int-negot, BLG-int-conlang (Accept-Language, Content-Language) | MUST | Problem texts are English only; Accept-Language is ignored and no Content-Language is sent. | The decided texts are English (D47, D55, D66). |
| BLG-trc-header (BelGov-Trace-Id), BLG-cst-header (prefix custom headers) | SHOULD | W3C traceparent and `X-Correlation-Id` instead. | D86 names these headers. |
| CODE-39 (@throws on every exception of a public method) | MUST | Checked exceptions are documented; the runtime `ApiException` thrown by many methods is not listed on each one. | One handler maps every `ApiException` (D104); listing it on every method adds noise. |
| UX-01 (established UX guidelines) | MUST | The UI follows the decided labels and WCAG 2.1 AA; the Simpl Figma design system is not applied. | The Figma design and participant-fe-components are not available to the factory (specs README, D167). |
| API-21 (examples on every request and response) | SHOULD | `docs/openapi-decided.yaml` keeps the published examples and adds examples for the new problem responses only. | Contract owners maintain the published file (D39). |

## Decisions not implemented exactly as written

| Decision | What differs | Reason |
|---|---|---|
| D83 (admin endpoints off, or only for a third admin user) together with D156 (online compaction) | The online compaction endpoint `/$/compact` must be enabled for D156. Fuseki 6.2.0 applies the server-level user list to it, so all three store users (reader, writer, admin) can start a compaction; anonymous calls get 401. Compaction changes no data. All other admin endpoints are off. | Fuseki main protects admin endpoints only with the server-level allowed users (checked by hand: anonymous 401, reader 200). |
| Q6 (no axe violations) | The UI is built for it (labels, roles, keyboard actions), but axe was not run in this build step. | No browser runs in the build step; step 60 runs axe. |
| D99 (load test shape) | The seed of 1000 x 20 versions was bulk-loaded into the store with the restore procedure (data in the same physical model, checked by the SHACL shapes) instead of being created through the API; the offered load itself (20 reads/s, 1 write/s, 5 minutes) went through the API. | Creating 20 000 versions through the API takes about 45 minutes on this machine (TDB2 single writer); the seed is not what Q1 measures. |
