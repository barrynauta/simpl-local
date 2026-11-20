# DEVIATIONS.md

Rules of `input/rules.json` the build does not follow, or follows only in part, and decisions not implemented as
written. The list is checked against the MUST rules for code, config, pipeline and data model, and the SHOULD rules
a decision names. It is not a line-by-line audit of all 405 rules.

## Rules

| Rule | Level | What the build does instead | Reason |
|---|---|---|---|
| API-46 (problem type URN `urn:problem-type:simpl:`) | SHOULD | 400, 404, 500 and 503 use `urn:problem-type:belgif:...` | Required by D48 (recorded deviation). 401, 403, 405, 406, 409, 413 and 415 use the simpl types. |
| SOA-09 (no overlapping paths) | SHOULD | `/vocabularies/searches` and `/external-vocabularies/content` overlap a name path; a fixed segment wins | D77; the published contract defines these paths. |
| OWT-A04, OWASP-03 (TLS in transit, encryption at rest) | MUST | Plain HTTP inside the local stack and to localhost; Fuseki TDB2 files are not encrypted | Local evaluation stack. In production TLS ends at the Tier 1 gateway and the platform provides encrypted volumes. The store is on an internal network with no host port (D83). Secrets are never in code or logs. |
| OWT-A07 ("repeated failures are limited") | MUST | Tokens are validated on every call; there is no rate limit on failed calls in the service | D46: the call-rate limit is applied by the Tier 1 gateway in production, outside this build. |
| OWT-A09, OWASP-10 ("can raise an alert") | MUST | 401, 403, firewall rejections, input validation failures and every write are logged (JSON, subject id only) and counted in the Prometheus metrics by status | No alerting backend is part of the build (D86 says no tracing backend; the same holds for alerting). Alert rules on the 401/403 counters are an operations task. |
| OPS-06, OPS-33, OPS-35, OPS-37 (pipeline gates) | MUST | Formatter, lint, tests, coverage and SBOM run in `mvn verify` and in the UI image build; Trivy runs by hand | D95: no CI pipeline in the factory. |
| OWASP-05 (threat model before implementation) | MUST | A short threat model is in `docs/architecture.md`; it was written during the build, not before it | The step starts from the decisions; no earlier threat model exists in the input. |
| CODE-39 (`@throws` on every public method) | MUST | Checked exceptions are documented; the unchecked domain and problem exceptions (`ProblemException`, `VocabularyNotFoundException`, ...) are documented on the classes and in `docs/api-notes.md`, not with `@throws` on every public method | Time. The exceptions are mapped in one place (`ApiExceptionHandler`), which CODE-34 to CODE-36 ask for. |
| OWASP-07 (no image with known vulnerabilities above the threshold) | MUST | The third-party Keycloak image has 1 CRITICAL and 6 HIGH with no usable fix | See SECURITY-SCAN.md. Own images and dependencies: 0. |
| OWT-A02 (no debug endpoints) | MUST | Keycloak runs in dev mode (`start-dev`) | Allowed by D98 for the local stack; its port is bound to 127.0.0.1 only. |
| CODE-03, CODE-04 (Angular ESLint) | MUST | Not applicable: the UI is Vue 3 (build profile). ESLint with the Vue plugin and Prettier are used (CODE-05). | Build profile stack. |
| E6 UI stack (PrimeVue unstyled, TailwindCSS) | choice | Plain Vue components and own CSS | The build profile asks for a plain component library; the shared library is not available to the factory. |

## Decisions

| Decision | What is different | Reason |
|---|---|---|
| D99 (load test shape) | The own measurement ran 60 s at the offered rate on a small store, not 5 minutes on the 20 000-version seed | Time in this step. The results and the limit of the measurement are in QUALITY.md. Step 70 runs the load test of the factory. |
| D10 `semanticReasoningSkipped` | Never emitted | As decided: reserved code, not emitted in this build. |

Every other decision is implemented as written (TRACEABILITY.md).
