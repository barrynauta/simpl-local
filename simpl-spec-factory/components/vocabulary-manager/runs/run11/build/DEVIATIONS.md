# Deviations

Rules of `input/rules.json` this build does not follow, decisions that override a rule, and
decisions not implemented as written. Rules that do not apply to this component (Tier 2 agent to
agent rules SEC-06, SEC-08 to SEC-10, SEC-13, SEC-15 to SEC-21; Kafka and AsyncAPI rules API-62 to
API-80, BLG-async-*, MSV-04, ARCH-27; Angular rules CODE-03, CODE-04; sagas MSV-08) are not listed.

## Rules overridden by a decision

| Rule | Decision | What the build does |
|---|---|---|
| API-46 (problem type URN `urn:problem-type:simpl:`) | D48 | 400, 404, 500 and 503 keep the Belgif types of the OpenAPI (`urn:problem-type:belgif:...`); 401, 403, 405, 406, 409, 413, 415 use `simpl` |
| CODE-21, CODE-52 (Log4J wrapper, common Simpl log format), SEC-11, CODE-50 (central log shipping) | D105 | JSON lines through Spring Boot structured logging (Logstash layout) with the fields of D96; no shipping to ELK |
| CODE-11 (depend on interfaces) | D135 | no interface for classes with one implementation |
| CODE-33 (shared exception framework) | D104 | one own exception class with the problem types as error codes |
| CODE-45, SEC-12 (authorisation at the Tier 1 gateway) | D17 | the service validates tokens and roles itself; the gateway is not part of the local stack |
| API-06, API-26 (lowerCamelCase path segments: `external-vocabularies`, `external-vocabulary/searches`) and API-50, API-52 (identifier named id or code) | D39, D30 | the published contract's paths and the name as identity are kept |
| DATA-18 (LDM attributes in snake_case) | D97 (use the CDM class and attribute names) | LDM attributes keep the CDM's lowerCamelCase names |
| DATA-22 to DATA-25 (PDM as UML class diagram with tables, keys, indexes) | D87 (physical model as SHACL shapes of the RDF store's named graphs) | `docs/physical-model.ttl` plus the table in `docs/architecture.md`; TDB2 keeps its own triple indexes |
| SOA-09 path overlap (`/vocabularies/searches` vs `/vocabularies/{vocabularyName}`) | D77 | fixed segment wins; GET on the fixed paths gives 405 |

## Rules not followed (no decision covers them)

| Rule | Why |
|---|---|
| API-25, OWT-A04, OWASP-03 (TLS, encryption at rest) | The local stack uses plain HTTP on localhost and an unencrypted Docker volume for the store. TLS termination and volume encryption belong to the deployment platform; not configured here. |
| ARCH-07 (no single point of failure) | The backend runs with two replicas, but Fuseki and Keycloak run as one container each. A replicated store is outside this build. |
| SEC-07 (Tier 1 Authentication Provider with the Simpl custom authenticator) | Plain Keycloak 26.8.2; the plugin is not in the build input (E1). |
| OWASP-07 (no known vulnerabilities above the threshold) | 6 HIGH remain in the vendor Keycloak image (see SECURITY-SCAN.md); own images and dependencies have none. |
| OPS-05 to OPS-08, OPS-12, OPS-17, OPS-23 to OPS-27, OPS-33, OPS-35, OPS-37, OWASP-12 (pipeline: SAST, SCA, image scans, Sonar, registry push, SBOM per image, merge gates, protected branches) | No CI pipeline in the factory (D95). The same checks run locally on the finish line: tests, Spotless, ESLint, Prettier, vue-tsc, Trivy, CycloneDX SBOM. No SAST tool or Sonar was run. |
| OPS-10, OPS-11, OPS-13, OPS-28 to OPS-30 (Kubernetes network policies, secrets, service accounts, Helm chart, deployment strategy) | Docker Compose only; no Helm chart. Compose uses an internal network for the store, non-root users, read-only file systems and `no-new-privileges`. |
| OPS-15 (Semantic Versioning of artefacts) | Images are tagged `local`; the jar has version 1.0.0. |
| CODE-05 (Prettier with a TypeScript configuration) | Prettier configuration is `.prettierrc.json`. |
| CODE-39 (`@throws` for every exception of a public method) | Documented on services, repositories and the store client; not on every controller method and filter (they throw only the documented `ApiException`). |
| CODE-07 (maximum method length) | No maximum is given in the input; a few controller methods are about 30 to 40 lines because they list the field checks of one request in decided order. |
| MSV-11 (backward compatibility checked by contract tests) | There is no earlier version of this API to compare with; `DecidedContractTest` checks the code against the decided contract only. |
| ARCH-22, ARCH-23 (service registry and service description) | No registry in the stack; `docs/` describes the service. |
| BLG-prf-cach, BLG-prf-lstmod (HTTP caching, Last-Modified) | No caching headers; content of a version never changes, so ETags would be possible, but nothing decides them. |
| NFR-10, Q6 (WCAG 2.1 AA measured) | The UI is built for it, but axe was not run in this build. |
| UX-01 (UX guidelines, Figma) | The Figma design is not reachable (specs README); the UI follows the decided texts and roles only. |
| NFR-03, NFR-13 (load test at the stated load) | Measured at a reduced seed (200 x 3 instead of 1000 x 20); see QUALITY.md. |

## Decisions not implemented exactly as written

| Decision | Difference |
|---|---|
| D122 | The proxy answers malformed requests with 400 problem+json and TRACE with 405 problem+json and an Allow header per declared path. A request to the proxy's internal path `/__problems/...` gets nginx's own 404 HTML page. |
| D98 / D120 | The management port 8081 is on the front network (D120 allows it); it is published on no host port and the proxy routes only to port 8080. |
| D99, Q1 | Seed reduced as above. |
