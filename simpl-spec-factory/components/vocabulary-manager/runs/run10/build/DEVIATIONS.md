# Deviations

Rules of `input/rules.json` this build does not follow, and decisions it could not implement as written.

## Decisions

| Decision | What is not as written | Reason |
|---|---|---|
| D70, D52 (part with an empty name) | A multipart part with `name=""` does not give 400 `in: body, name: ''`; the request is handled as if the part were absent | Tomcat's multipart parser drops parts without a field name before the Servlet API returns the parts. Detecting it would need a hand-written multipart parser (REUSE-02) |
| D99, Q1 | The throughput test did not run at the decided shape (seed 1000 x 20, 20 reads/s for 5 minutes) | Time; measured at 300 x 3, about 16 reads/s, 90 s (QUALITY.md). Not a failure of the target, but not proven |
| D98 | The management port (8081) listens on every network of the backend container, also the `front` network shared with the proxy | Docker gives no per-port network binding for replicas without fixed addresses. Mitigation: the port is published nowhere and the proxy never forwards to it; on the API port `/actuator` gives 401 or 404 (D67) |
| D48 | Problem types of 400, 404, 500 and 503 use `urn:problem-type:belgif:...` | As decided; recorded here as the deviation from API-46 that D48 asks for |

## Rules

| Rule | Level | Not followed | Reason |
|---|---|---|---|
| API-46 | SHOULD | belgif problem types for 400, 404, 500, 503 | D48 |
| CODE-21, CODE-52, SEC-11 | SHOULD | Simpl log format (LogWrapper) and shipping to ELK | D105: library not in the build input; JSON lines of D96 instead |
| CODE-39 | MUST | Not every public method documents its unchecked `ProblemException` with `@throws`; the service, store, validation and request classes do, controllers and small helpers do not | Time; the exception model is one class with one handler (D104), documented in `Problems` and `ProblemHandler` |
| OPS-06, OPS-33, OPS-35, OPS-37 | MUST | No pipeline, merge gate or SAST quality gate | No CI in the factory (D95). Formatter and lint checks run in `mvn verify` and in the UI build; Trivy as the scan. No SAST tool was run |
| OWT-A04, OWASP-03 | MUST | The local stack uses HTTP, also between the services; the store data volume is not encrypted | Local evaluation stack; in production TLS is terminated by the Tier 1 gateway and the platform encrypts volumes. Secrets are never in code, images or logs |
| OWT-A07 (repeated failures limited) | MUST | The service does not rate-limit failed token attempts | D46: call-rate limits are applied by the Tier 1 gateway in production. Keycloak brute-force protection is on for logins |
| UX-01 | MUST | The UI does not use the Simpl Figma design system or participant-fe-components | Not available to the factory (E6, specs README); plain accessible components instead |
| SEC-12 | SHOULD | Tier 1 authorisation is also enforced in the service, not only at the gateway | D17: the service checks tokens and roles itself so the rules can be tested without a gateway |
| ARCH-07 | MUST | The store (one Fuseki) is a single point of failure | Build profile fixes one Fuseki; backends run as two replicas |

Rules about Kafka, AsyncAPI and CloudEvents (API-62 to API-74) and Angular (CODE-03, CODE-04) do not apply: the
component publishes no events (D45) and the UI is Vue.
