# Deviations

Rules of `input/rules.json` this build does not follow, and decisions that override a rule. Rules
about technologies the component does not use (Kafka and AsyncAPI: API-62 to API-80, CODE-51,
BLG-async-*; Angular: CODE-03, CODE-04; Tier 2 agent-to-agent traffic: SEC-06, SEC-08 to SEC-10,
SEC-13, SEC-15 to SEC-21; event consumers MSV-04, sagas MSV-08) are not applicable and not listed.
Every decision in `input/decisions/` is implemented as written; the points where an
implementation detail could be read two ways are in BUILD-LOG.md.

## Decisions that override a rule

| Rule | Decision | What the build does |
|---|---|---|
| API-46 (problem type URN `urn:problem-type:simpl:...` for every problem) | D48 | belgif types for 400, 404, 500 and 503; simpl types for 401, 403, 405, 406, 409, 413, 415 |
| CODE-11 (depend on interfaces) | D135 | No interface for classes with one implementation |
| CODE-21, CODE-52, SEC-11, CODE-50 (Simpl LogWrapper format, central log shipping) | D105 | JSON lines through Spring Boot structured logging; no log shipping in the local stack |
| CODE-33 (shared exception framework) | D104 | One own exception base class (`ApiException`) with a problem-type enum |
| CODE-45, SEC-12 (authorisation at the Spring Cloud Gateway) | D17, E6 | The service checks tokens and roles itself; the local nginx gateway only proxies |
| OWT-A07 (limit repeated token failures) | D164 | Not limited in the service; the Tier 1 gateway does it in production |
| OWA-API4 (call-rate limit) | D46 | No call-rate limit in the service; upload concurrency is limited (D92) |
| BLG-addi-prop, BLG-req-valid | D52 | Unknown properties are refused (400) also where the schema does not say `additionalProperties: false` |
| API-50, API-52, API-54, BLG-id-name (identifier named `id` or `code`) | D30, D31, D39 | The name is the identity (`name`, `vocabularyName` path parameter) as in the published contract |
| DATA-22 to DATA-24 (PDM as UML class per table with keys) | D87 | The store is RDF; the physical model is SHACL shapes per named graph |
| DATA-18 (LDM attributes in snake_case) | D117 (glossary names) | LDM uses the glossary's lowerCamelCase names, which are also the API names |
| UX-01 (Simpl UX guidelines, Figma) | D167 | The Figma design and participant-fe-components are not available to the factory; PrimeVue unstyled + Tailwind |

## Rules not followed (no decision)

| Rule | What is missing | Reason |
|---|---|---|
| API-25, OWT-A04, OWASP-03 (HTTPS, TLS in transit, encryption at rest) | The local stack uses plain HTTP and an unencrypted TDB2 volume | Local evaluation stack; TLS termination and storage encryption belong to the platform (D165 records encryption at rest as the platform's) |
| ARCH-07 (no single point of failure) | Fuseki and Keycloak run as one instance | The backend runs two replicas; a replicated store is out of scope of the local stack (Fuseki has no built-in clustering) |
| BLG-trc-header (`BelGov-Trace-Id`) | Not supported | W3C traceparent and X-Correlation-Id are used (D86); BelGov header is for third-party APIs |
| BLG-int-conlang, BLG-int-negot (Content-Language, Accept-Language) | Not set or honoured | All texts are English only; no internationalised content |
| BLG-prf-cach, BLG-prf-lstmod (HTTP caching, Last-Modified) | No caching headers on metadata or content | Not decided; content of a version never changes, so ETag caching could be added later |
| API-21, API-22 (example on every request body and response in the OpenAPI) | `openapi-decided.yaml` keeps the published examples; new responses (401, 403, 503) have examples, some request bodies do not | The published contract is the owner's; only decided changes were added |
| API-57 (every parameter has a description) | Inherited from the published contract, where most parameters have one | Not changed beyond D79 |
| CODE-39 (@throws in Javadoc for every exception of a public method) | Unchecked `ApiException` is not listed in every Javadoc | The exception model is one unchecked class documented in `ApiException` and `api-notes.md` |
| CODE-07 (method line limit) | Some controller and store methods are longer than 40 lines (`ExternalController.register`, `VocabularyStore.insertVersion`) | Kept together so the order of checks (D51) and the single store transaction stay readable |
| NFR-09 (mutation score, static analysis without HIGH findings) | No mutation testing, no Sonar run | Not available offline in the factory; coverage is measured (92.8 % lines, 82.4 % branches) |
| NFR-03 (load test at the stated load per operation) | Reduced load measured (QUALITY.md), not the full D99 seed and 5-minute run | Time; step 70 runs the full load test |
| OPS-05 to OPS-08, OPS-14, OPS-23 to OPS-30, OPS-33, OPS-35, OPS-37 (pipeline, SAST, Helm chart, registry) | No CI pipeline, no Helm chart, no image push | D95: no pipeline file is required in the factory; the checks run in `mvn verify`, `npm run build` and the Trivy scan of this step |
| OPS-10, OPS-13 (Kubernetes network policies, service accounts) | Compose stack only | Local stack; the compose networks separate the store (internal network) |
| OPS-11 (secrets in Kubernetes Secrets or Vault) | `.env` file created by `stack.sh` | Local stack (E6: OpenBao in production) |
| SEC-23, SEC-22 | Keycloak admin is a bootstrap user from `.env` | Local stand-in identity provider |
| UX-02 (usability testing) | Not done | No users in the factory |
| MSV-11 (consumer contract tests) | No consumer-driven contract tests | No consumer (Schema Manager) in the build input |
| ARCH-22 (service registry) | Not registered | No registry in the local stack |
