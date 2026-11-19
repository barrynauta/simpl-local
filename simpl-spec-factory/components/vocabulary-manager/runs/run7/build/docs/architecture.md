# Architecture overview

## Containers

```
 browser ──> ui (nginx, :4433) ──/v1──> backend (Spring Boot, :8196 -> 8080) ──SPARQL──> fuseki (TDB2 'vocabulary')
    │                                        │
    └──── login (code flow + PKCE) ──> keycloak (:8193, realm simpl) <── realm keys (JWKS) ──┘
```

| Container | Image | Role |
|---|---|---|
| `ui` | `vm-factory-run7/ui` (nginx-unprivileged Alpine) | Serves the Vue app, `/config.json` from environment, proxies `/v1` to the backend (MACH-04: the UI uses the public API only). |
| `backend` | `vm-factory-run7/backend` (Temurin 21 JRE Alpine) | The Vocabulary Management API. Management port 8081 (health only) is not published. |
| `backend-store-down` | same image, profile `storedown` | Same backend with the store URL on a closed port, API on 8199 (D44 tests). |
| `fuseki` | `vm-factory-run7/fuseki` (Fuseki 6.2.0) | The store, dataset `vocabulary`, TDB2 on the volume `fuseki-data`. |
| `keycloak` | `quay.io/keycloak/keycloak:26.8.2` | Realm `simpl` imported at start (E1). |

## Backend layers

| Package | Content |
|---|---|
| `api` | `RouteTable` (every operation with its role and media types), filters, controllers, request parsing (`QueryParameters`, `MultipartForm`, `JsonBody`, `FieldRules`, `QueryRules`), problem responses, DTOs and `ApiMapper`. |
| `security` | Token validation (Spring Security + Nimbus), `RealmKeySource` (key cache of D44), `Roles`. |
| `service` | `VocabularyService` (lifecycle), `ExternalVocabularyService`, `SearchService`, `ContentValidation`, `NameLocks`. |
| `domain` | Model records and validation: `TurtleValidator` (syntax, S1 to S6), `ReferenceAnalyzer` (D12, S7, warnings). |
| `store` | `VocabularyStore` port and the Fuseki adapter with `SparqlClient`. |

## Request pipeline (order of checks, D51, D63, D67)

1. `EntryFilter`: correlation id; unknown HTTP method gives 405; Spring Security `StrictHttpFirewall` gives 400; known path with an undeclared method gives 405 with `Allow`.
2. Spring Security: bearer token (signature, issuer, audience, expiry) gives 401; unknown key while Keycloak is down gives 503. `GET /v1/health` is public.
3. `RequestGateFilter`: role of the operation gives 403; Accept gives 406; Content-Type gives 415; declared size gives 413.
4. Controller: multipart or JSON body read (413 for the 5 MB file part), then all request-shape issues in one 400 (path values, query parameters, unknown fields, field rules, version field).
5. Service, under the per-vocabulary lock: 404, then 409 (duplicate name, same status, identical version), then content validation (syntax, then semantic and references), then one store update.

Unknown paths reach Spring MVC and give 404 (after authentication, so 401 without a token).

## Data in the store

Named graph `urn:simpl:vocabulary-manager:graph:metadata` holds:

- a vocabulary node per name: name, lower-case name key (uniqueness), vocabulary status, creator, creation date, last update date, latest version number;
- a version node per version: vocabulary link, number, description, changelog, version status, creator, dates, SHA-256 of the content, content graph link;
- an external vocabulary node per name: name, name key, namespace URI, alias URIs (JSON array, order kept), lookup keys (normalised namespace and aliases), source URI, description, creator, creation date.

Each version's content and each external vocabulary's content lives in its own named graph as base64 chunks of 64 KB with an index, so the bytes come back exactly as uploaded (D29) and a 5 MB write stays fast (D71).

Every write is one SPARQL update request, which Fuseki runs as one transaction (D8, D43). A new version deprecates the older ACTIVE versions, moves the latest version number and inserts the new version in that same request.

## Validation

- Syntax: Jena RIOT Turtle parser in strict mode, base IRI `<contentBase>/content/<Name>/versions/<n>` (D57). First error only, with line and column.
- Semantic: S1 to S6 over the parsed graph, all rules, all errors.
- References: each `@prefix`/`PREFIX` namespace is classified (internal, external, W3C, own, self). Internal ones and registered external keys are loaded in one store query; missing internal references are S7 errors; deprecated ones and unregistered external namespaces are warnings.

## UI

Vue 3 single-page app: Vocabulary list (`/vocabularies`), Vocabulary details with history (`/vocabularies/<Name>`), create form (`/vocabularies/new`), new version form (`/vocabularies/<Name>/versions/new`). keycloak-js logs the user in; write actions are shown to `GA_VOCABULARY_ADMIN` only; the server checks every call anyway.
