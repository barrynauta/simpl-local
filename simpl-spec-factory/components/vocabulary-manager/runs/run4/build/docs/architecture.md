# Architecture

## Components

```
browser ──> ui (nginx :8080, host 4433) ──/v1──> backend (Spring Boot :8080, host 8196) ──SPARQL──> fuseki (:3030)
   │                                                 │
   └──── login (code flow + PKCE) ──> keycloak (:8080, host 8193) <── realm keys (JWKS) ──┘
```

- **ui**: Vue 3 + Vite + TypeScript, plain components, `keycloak-js` for login. It calls only the public API through
  nginx (same origin, MACH-04). Runtime settings come from `/config.json`, which nginx renders from the environment.
- **backend**: Java 21, Spring Boot 3.5, one Maven module. Packages:
  - `api`: controllers, request checks (query, path, multipart, JSON), wire records, problem mapping.
  - `security`: bearer token filter, token validation with Nimbus JOSE, realm key cache, access rules.
  - `service`: use cases, per-vocabulary locks, list filters, sort and paging.
  - `validation`: Turtle syntax (Jena RIOT), semantic rules S1 to S6, reference rules S7 and warnings.
  - `store`: SPARQL client with timeouts and retries, repositories (the only classes that know SPARQL).
  - `error`: one exception class with error code enums per category, problem writer.
- **fuseki**: Apache Jena Fuseki 5.6.0, TDB2 dataset `vocabulary` on a named volume, not exposed on the host.
- **keycloak**: Keycloak 26.4, realm `simpl` imported at start (decision E1).

## Store model

All data is RDF in the Fuseki dataset. Two named graphs:

| Graph | Subjects | Properties (prefix `vm:`) |
|---|---|---|
| `urn:simpl:vocabulary-manager:graph:internal` | `urn:simpl:vocabulary-manager:vocabulary:<Name>` (`vm:Vocabulary`) | name, nameKey (lower case), status, createdBy, creationDate, lastUpdateDate, latestVersion |
| same | `...:vocabulary:<Name>:version:<n>` (`vm:VocabularyVersion`) | ofVocabulary, version, description, changelog, status, createdBy, creationDate, lastUpdateDate, content |
| `urn:simpl:vocabulary-manager:graph:external` | `urn:simpl:vocabulary-manager:external:<Name>` (`vm:ExternalVocabulary`) | name, nameKey, namespaceUri, aliasUris, sourceUri, description, createdBy, creationDate, content |

`content` holds the uploaded bytes as Base64, so downloads return exactly what was uploaded (D29). Dates are stored
and returned to the second, so sort order and the returned values always agree.

Every write is one SPARQL Update request, which Fuseki runs in one transaction (D8, D43, D44). A new version is three
operations in that request: deprecate every older ACTIVE version, move `latestVersion` and `lastUpdateDate`, insert
the version.

## Order of checks on a request

1. Correlation id (header `X-Correlation-ID`, made when missing, logged with every line, returned).
2. Bearer token (401, or 503 when the realm keys cannot be fetched for an unknown key id), then the role (403) (D17).
3. Content type (415) and Accept (406).
4. Path parameters, unknown query parameters, query values (400) (D21).
5. Body fields: unknown fields and the `version` field, required fields, formats and lengths (400), upload size (413).
6. Existence (404), then conflicts: duplicate name, same status, identical version (409) (D4, D15, D46).
7. Validation: syntax (first error only), then all semantic rules S1 to S7 (400 with every error) (D9).
8. Store write; the response carries the warnings (D10).

Changes to one vocabulary run one after another under a lock per lower-case name (D43). Waiting for the lock counts
towards the request; after 10 s the request gets 503 and nothing is stored.

## Failure handling (D44, MSV-06)

| Dependency | Timeout | Retries | What the caller sees |
|---|---|---|---|
| Fuseki read | connect 2 s, response 10 s | 2, after 200 ms and 400 ms | 503 `serviceUnavailable`, `Retry-After: 5` |
| Fuseki write | connect 2 s, response 10 s | none | 503 as above; any other store error 500 |
| Keycloak keys | 2 s | none inside a request | cached keys keep working; unknown key id with Keycloak down: 503 |
| Health | store 2 s | none | 503 DOWN, 200 DEGRADED (store up, no keys, Keycloak down), 200 UP |

No circuit breaker is used (see DEVIATIONS.md).

## Security

- Tokens are validated in the service: signature against the realm keys (RS/PS/ES algorithms only), issuer, audience
  `vocabulary-manager-be`, expiry and not-before with 30 s skew. Roles come from `realm_access.roles`.
- Failures are logged at WARN without the token (OWT-A09); access denials name method, path and user.
- Errors never carry stack traces or store details (500 says only "Internal Server Error").
- No CORS: the UI is same-origin through nginx. Security headers on the API (Spring Security defaults plus CSP) and on
  the UI (CSP, frame options, nosniff, referrer policy).
- SPARQL is built with Jena `ParameterizedSparqlString`; user text is always bound as a literal (OWASP-04).
- Containers run as non-root users; the Fuseki jar is checked against a pinned SHA-512.
