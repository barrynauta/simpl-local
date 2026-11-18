# Architecture

## Containers

```
browser ──> ui (nginx :8080, host 4433) ──/v1──> backend (Spring Boot :8080, host 8196) ──SPARQL──> fuseki (:3030, internal)
   │                                                     │
   └──── login (OIDC code + PKCE) ──> keycloak (:8080, host 8193) <── realm keys (JWKS) ──┘
```

- **ui**: static Vue build plus nginx. nginx proxies `/v1` to the backend, so the UI uses the same public API as every
  other client (MACH-04). `/config.json` is generated from the environment at start (E4).
- **backend**: one Spring Boot service. No Spring Security: a servlet filter validates the bearer token and the role
  (D17), so the order of checks of D51 is under the service's control.
- **fuseki**: Apache Jena Fuseki 6.2.0, TDB2 dataset `vocabulary` on a Docker volume. Only the backend talks to it
  (MSV-02). Not published on a host port.
- **keycloak**: Keycloak 26.8.2 in development mode with the realm of E1 imported at start.

Every container has a health check, so `docker compose up --wait` returns when the system is usable. The backend waits
for healthy Fuseki and Keycloak; the UI waits for a healthy backend.

## Backend packages

| Package | Role |
|---|---|
| `web` | Controllers, request checks in the order of D51 (`RequestInspector`, `FormFields`, `PathValues`, `Paging`, `DateFilter`), wire representations (`Representations`, `CollectionPage`), problem responses (`ApiException`, `ApiExceptionHandler`, `ProblemWriter`), correlation id and security headers. |
| `security` | `AuthenticationFilter` (401, 403, 503 before anything else), `TokenValidator` (signature, issuer, audience, expiry), `RealmKeys` (key cache, D44), `AccessRules` (D16). |
| `service` | Use cases: `VocabularyService` (create, new version, status changes, search), `ExternalVocabularyService`, `NameLocks` (D43). |
| `validation` | `TurtleParser` (syntax, D9), `SemanticRules` (S1 to S6), `ReferenceChecker` (S7 and warnings, D11, D12, D47), `ContentValidator`. |
| `store` | `VocabularyStore` interface, `FusekiVocabularyStore` (parameterised SPARQL), `SparqlClient` (timeouts and retries of D44). |
| `domain` | `Vocabulary`, `VocabularyVersion`, `ExternalVocabulary`, `Status`, name rules. |

### Store model

All data is in the default graph of the dataset `vocabulary`, under the namespace `urn:simpl:vocabulary-manager:`.

- Vocabulary `…:vocabulary:<Name>`: name, lower-case name key (uniqueness, D37), vocabulary status, creator, creation
  date, last update date, latest version number.
- Vocabulary version `…:vocabulary:<Name>:version:<n>`: version number, version status, description, changelog,
  creator, dates and the content as base64 (the stored bytes come back exactly as uploaded, D29).
- External vocabulary `…:external:<Name>`: name, namespace URI, alias URIs (JSON list, keeps the order), source URI,
  description, creator, creation date, content.

Every write is one SPARQL update request, which Fuseki runs as one transaction: a new version, the deprecation of the
older ACTIVE versions and the new latest version number are stored together or not at all (D4, D8, D43).

### Request flow of a write (D51)

1. `AuthenticationFilter`: no or invalid token 401; unknown key with Keycloak down 503; missing role 403.
2. Controller: 406 when `Accept` excludes the response type; 415 media type; multipart read and 413 when the file is
   larger than 5 242 880 bytes; 400 for query parameters, path values, unknown or repeated fields, field rules and the
   `version` field. All 400 issues of the request are returned together.
3. Service, under the lock of the vocabulary name: 404; 409 (duplicate name, same status, identical version); content
   validation (syntax, then semantic); one store update.

### Concurrency and failure (D43, D44)

- `NameLocks` holds one fair lock per lower-case vocabulary name; waiting longer than 10 s gives 503.
- Store calls: connect timeout 2 s, response timeout 10 s; read queries retried twice (200 ms, 400 ms) on connection
  failure or timeout; updates never retried. Timeouts and connection failures give 503 with `Retry-After: 5`; other
  store errors give 500.
- Realm keys are fetched at start and when a token names an unknown key id (at most once per 10 s after a successful
  fetch). Cached keys keep working while Keycloak is down. An unknown key with Keycloak down gives 503 (fail closed).
- `GET /v1/health`: 503 DOWN when the store does not answer within 2 s; 200 DEGRADED when no realm keys are loaded and
  Keycloak does not answer; otherwise 200 UP.

### Logging

Spring Boot structured logging in ECS JSON (`LOG_FORMAT`, default `ecs`). Every line of a request carries its
correlation id (`X-Correlation-Id`, taken from the request when it is safe, else generated, returned in the response
and passed to Fuseki). Access denials are logged at WARN with the user id (token subject), never the token or the
e-mail address; administrative changes at INFO.

## UI

Routes: `/vocabularies` (Vocabulary list), `/vocabularies/<Name>` (Vocabulary details with the version history and the
version panel, `?version=<n>` selects a version), `/vocabularies/new` (create form) and `/vocabularies/<Name>/update`
(new version form). keycloak-js does the login (`login-required`, PKCE S256); actions for administrators are hidden
for other users (D18). The browser syntax check uses N3.js (D13). Texts are those of D14, D18, D34 to D36, D46 and
D60 (`src/lib/messages.ts`).

## Threat model (short)

| Threat | Control |
|---|---|
| Forged or replayed tokens | Signature with realm keys only (asymmetric algorithms), issuer, audience, expiry checked on every call; short token lifetime (15 min). |
| Privilege escalation | Role check on the server for every operation before any input is read (D16, D17). |
| Injection into SPARQL | All values bound through Jena `ParameterizedSparqlString`; names and versions also checked against strict patterns. |
| Malicious uploads | 5 MB limit, Turtle parsed by Jena RIOT into memory, never executed; no URL in the content is fetched (no SSRF, E3). |
| Denial of service | Upload and JSON body limits, page size limit 100, search limit 100, lock wait and store timeouts; call-rate limits belong to the Tier 1 gateway (D46). |
| Information leaks | Problem responses never carry stack traces or store messages; security headers on every response; CSP on the UI. |
| Store tampering | Fuseki only on the internal Docker network, no host port. |
