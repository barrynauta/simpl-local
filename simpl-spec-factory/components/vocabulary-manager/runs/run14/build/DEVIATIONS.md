# Deviations

Rules from `input/rules.json` that the build does not follow, and decisions it could not implement as written.

## Rules set aside by a decision

| Rule | Decision | What the build does |
|---|---|---|
| API-46 (problem types) for 400, 404, 500, 503 | D48 | Uses `urn:problem-type:belgif:...` for these four codes; simpl types for 401, 403, 405, 406, 409, 413, 414, 415, 431 |
| CODE-11 (depend on interfaces) | D135, D187 | No interface for a class with one implementation |
| CODE-21, CODE-52, SEC-11 (Simpl LogWrapper, log shipping) | D105 | JSON lines from Spring Boot structured logging |
| OWT-A04, OWASP-03, API-25, ARCH-17 (TLS in transit, encryption at rest) | D185, D165 | Local stack runs plain HTTP on 127.0.0.1 and internal networks; at rest encryption belongs to the platform |
| OWT-A07 (failed-token limiting), OWT-A09 and OWASP-10 (alerting) | D164, D185, D186 | Not limited in the service; security events are logged (WARN) and the metrics an alert would use are named in THREATS.md |
| D83 (store admin endpoints for the admin user only) | D193 | `/$/compact` is open to all three store users (Fuseki 6.3 cannot limit it); the store is on an internal network only |
| CODE-01, OPS-06, OPS-33, OPS-35, OPS-37 (pipeline gates, SAST gate) | D95 | No CI pipeline in the factory. Formatter, lint, tests and coverage gates run in `mvn verify` and in the UI image build; no SAST tool is run |

## Rules not followed, without a decision

| Rule | Reason |
|---|---|
| BLG-int-conlang (Content-Language header) | Responses carry English texts but no `Content-Language` header. Not decided; small gap. |
| CODE-39 (@throws in Javadoc of every public method) | Public service and repository methods document `@throws ApiException`; several small helpers and controller methods (which throw the same exception) do not. |
| DATA-26 to DATA-29 (CDM updated with every domain change) | The CDM is owned by the CDM owners; the differences are listed in docs/data-model.md and go to them (D207). |
| UX-01 (UX guidelines, Simpl Figma design system) | The Figma design and participant-fe-components are not available to the factory (D167, specs README). The UI uses PrimeVue unstyled with Tailwind and plain accessible markup. |
| API-01 (CDM delivered with the OpenAPI) | The CDM is input, not produced here; the build delivers a logical model traced to it (docs/data-model.md). |

## Decisions implemented differently from their text

| Decision | What differs | Why |
|---|---|---|
| D94 (SPARQL with ParameterizedSparqlString or the builders) | SPARQL is built from fixed templates; values are written as RDF terms by Jena's `NodeFmtLib` in one pass (`SparqlTemplate`) | `ParameterizedSparqlString` throws on any literal that contains a text equal to a variable name (for example a description with "?now"), which would turn valid input into a 500 (`SparqlInjectionTest`). Same safety: every value is escaped by Jena, never re-read as query text. |
| D133 (query parameters bound by the framework) | The raw query string is parsed by `QueryParameters`; routing, produces and consumes are Spring MVC mappings | The servlet container decodes `+` as a space, which D168 forbids. |
| D99 (seed of 1000 vocabularies with 20 versions, 5 minutes) | Measured with 300 vocabularies with 2 versions on top of the store of earlier tests, for 2 minutes | Time budget of the build step; step 70 measures with the full seed (D188). |
| D188 (index on the sort key) | Paging, filter and sort run in the store with ORDER BY, LIMIT and OFFSET on denormalised vocabulary nodes; TDB2 still sorts the matched vocabularies per call (top-N) | SPARQL on TDB2 offers no ordered index scan. The vocabulary node carries every listed field, so no join per version is needed. |
| D123 (no inline styles) | The CSP header has no 'unsafe-inline'. PrimeVue 5 injects its base `<style>` element and one style attribute even in unstyled mode; the browser blocks them and logs CSP errors in the console | PrimeVue offers no switch to skip its base styles. The UI does not rely on PrimeVue styles (Tailwind classes only), so nothing visible changes. Loosening the CSP was not chosen. |
