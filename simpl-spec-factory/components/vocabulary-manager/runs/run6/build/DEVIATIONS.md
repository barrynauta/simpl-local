# Deviations

Rules from `input/rules.json` this build does not follow, or follows only in part, and decisions not implemented as written. I checked the MUST rules of step 50 one by one and the SHOULD rules where they apply to code, config or pipeline; rules that apply only to the API file, the CDM or events (this build publishes none, D45) are not listed. This list is my claim, not a proof that nothing else is missed.

## Decisions

| Decision | Deviation |
|---|---|
| D48 | Recorded deviation from API-46: 400, 404, 500 and 503 use `urn:problem-type:belgif:...` as decided. Not a deviation from the decision. |
| none other | Every decision in scope is implemented as written. |

## Rules

| Rule | Level | What is not met | Reason |
|---|---|---|---|
| API-46 | SHOULD | Belgif problem types for 400, 404, 500, 503 | Decision D48 |
| OWT-A04, OWASP-03 | MUST | No TLS inside the local stack or on the published ports; the store is not encrypted at rest | Local evaluation stack with fixed `http://localhost` URLs from the build profile. In production TLS ends at the Tier 1 gateway and the volume is encrypted by the platform. |
| MACH-07 | MUST | The realm file holds the three test users with the password `Password1!` (decision E1); Keycloak runs in `start-dev` mode with its embedded H2 database | Test users are fixed by E1 and the build profile. No other secret exists in code or images. OpenBao (catalogue `secrets-and-keys`) is not part of the stack. |
| MACH-06, ARCH-07 | MUST / SHOULD | The service holds the per-vocabulary locks in memory, so two backend replicas would not serialise each other's writes | Decision D43 says this build runs one instance and names the in-process lock. More replicas need a store-side lock or optimistic version check. |
| ARCH-07 | SHOULD | Fuseki and Keycloak run as single containers | Local stack. |
| NFR-08 | SHOULD | No trace context passed to Fuseki; no tracing backend | The correlation id is logged and sent as `X-Correlation-Id`; a tracing backend is not part of the build profile. |
| OWASP-07 | MUST | 7 HIGH or CRITICAL findings remain in the Keycloak product image | Listed with reasons in SECURITY-SCAN.md. Own images and dependencies: 0. |
| OPS-06, OPS-33, OPS-35, OPS-37 | MUST / SHOULD | No CI pipeline, SAST gate or merge gate | The factory step builds locally without a repository pipeline. Spotless (`spotless:check` is bound to `mvn verify`) and Prettier exist and pass; Trivy was run by hand. |
| CODE-03, CODE-04 | SHOULD | Not applicable | The UI is Vue, not Angular. |
| UX-01 | SHOULD | The UI does not use the Europa Component Library or another established design system | The build profile asks for a plain component library; the screens follow the decided texts and flows only. |
| OWASP-10 | MUST | Security events are logged but not sent to a monitoring system | No monitoring stack in the build profile. |
| NFR-04 | MUST | No call-rate limit in the service | Decision D46: the Tier 1 gateway applies it in production. |
| BLG-col-repres, BLG-api-matur level 3 | MUST / SHOULD | Collection items carry `href` links; no further hypermedia | The wire format is fixed by the OpenAPI file (D39). |
| SIMPL-18184 AC2 | n/a | Deprecated reference is a warning, not an error | Decision D11 overrides the criterion. |

## Not implemented

Nothing in scope is left out. Out of scope by decision: SIMPL-28680, SIMPL-393 seed data, SIMPL-395 (D1, D61); events (D45); `semanticReasoningSkipped` is reserved and never emitted (D10).
