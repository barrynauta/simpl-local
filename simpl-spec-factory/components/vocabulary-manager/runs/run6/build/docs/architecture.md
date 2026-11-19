# Architecture overview

## Containers

```
browser ──> ui (nginx, 4433) ──/v1──> backend (Spring Boot, 8196) ──SPARQL 1.1──> fuseki (TDB2, internal)
   │                                       │
   └──── login (OIDC, PKCE) ──> keycloak (8193) <── realm keys (JWKS, at start and on unknown kid)
```

- The UI uses the same public API as any other client (MACH-04). nginx serves the static build, a `config.json` with the Keycloak URL, and proxies `/v1/` to the backend.
- The backend validates tokens itself (D17): Nimbus JWT decoder with signature, issuer, audience `vocabulary-manager-be` and expiry checks; roles from `realm_access.roles` (E1).
- The store is reachable only on the internal compose network (MSV-02). Fuseki runs without admin endpoints.

## Backend packages (`eu.simpl.vocabularymanager`)

| Package | Role |
|---|---|
| `api` | Controllers per resource, request checks in the decided order (D51), paging and list parameters (D19 to D23, D38), representations (D25, D26) |
| `service` | `VocabularyService` (create, new version, status changes, reads, search), `ExternalVocabularyService`, `VocabularyLocks` (D43) |
| `validation` | `TurtleParser` (Jena RIOT), `SemanticRules` (S1 to S6), `ReferenceChecker` (S7 and the warnings of D11, D12, D47), `ContentValidator` (syntax, then semantic) |
| `store` | `VocabularyStore` port and `SparqlVocabularyStore` adapter; `SparqlClient` with timeouts and read retries (D44) |
| `security` | Filter chains, `RealmKeySource` (cached realm keys, fail closed, D44) |
| `problem` | Problem details (RFC 9457 shape, Belgif and Simpl types, D48) |
| `web` | Correlation id filter, 405/404 filter with a route table, error controller and Tomcat valve that answer problem+json |
| `domain` | Vocabulary, VocabularyVersion, ExternalVocabulary, VocabularyStatus, ValidationWarning, Page |

## Storage model

One RDF resource per vocabulary, per vocabulary version and per external vocabulary, in the store namespace `https://w3id.org/simpl/vocabulary-manager/store#`. The uploaded Turtle is stored as an exact literal (D29: downloads return the bytes as uploaded). The vocabulary carries its own status (D3); each version carries its own status. A lower-case name key gives case-insensitive uniqueness (D37) while paths match exactly (D54).

## Write path

1. Security filters: 401, then 403 (D17). Unknown key id with Keycloak down: 503 (D44).
2. Controller checks in the order of D51: 415, 413, 400 (path, unknown fields and parameters, field rules, version field), then the service.
3. Service takes the per-vocabulary lock (D43, 256 fair stripes, timeout counts towards the request, 503 after it), checks 404 and 409 (duplicate name, same status, identical version D46), then validates content (D9), then writes the whole change in one SPARQL update (one Fuseki transaction, D8).

## Failure behaviour

- Store: connect timeout 2 s, response timeout 10 s, reads retried twice after 200 ms and 400 ms, updates never retried; unreachable gives 503 with `Retry-After: 5`; any other store error gives 500 (D44).
- Identity provider: keys cached; unknown kid triggers one fetch (2 s timeout, 5 s cooldown); failure gives 503, never acceptance without a signature check.
- `GET /v1/health`: 503 DOWN when the store does not answer within 2 s; 200 DEGRADED when no keys are loaded and Keycloak does not answer; else 200 UP.

## UI

Vue 3 with vue-router, keycloak-js (authorization code with PKCE), and the n3 Turtle parser for the in-browser syntax check (D13). Screens: Vocabulary list (`/vocabularies`), Vocabulary details with the version history (`/vocabularies/<Name>`), create (`/vocabularies/new`) and new version (`/vocabularies/<Name>/update`). All fixed texts live in `ui/src/texts.ts`.
