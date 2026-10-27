# Architecture

```
 Browser ──> nginx (ui, :4333) ──/v1──> backend (Spring Boot, :8096) ──SPARQL over HTTP──> Fuseki (TDB2)
    │                                         │
    └──── OIDC code + PKCE ──> Keycloak (:8183) <── JWKS (keycloak:8080) ┘
 Test harness ── password grant (client vocabulary-tests) ──> Keycloak
```

## Containers

| Service | Image | Role |
|---|---|---|
| `fuseki` | `secoresearch/fuseki:5.3.0` | Fuseki main server, one TDB2 dataset `/vocabulary` with query, update and graph store endpoints. Data in the `fuseki-data` volume. Not exposed |
| `keycloak` | `keycloak/keycloak:26.0` | Realm `simpl` imported from `keycloak/simpl-realm.json` |
| `backend` | built from `backend/Dockerfile` (Maven build stage, Temurin 21 JRE) | REST API |
| `ui` | built from `ui/Dockerfile` (Node build stage, nginx) | SPA plus reverse proxy for `/v1` |

Start order is enforced with healthchecks: backend waits for Fuseki and Keycloak, UI waits for backend.

## Backend packages (`eu.simpl.vocabulary`)

| Package | Responsibility |
|---|---|
| `domain` | `Vocabulary` aggregate (vocabulary metadata plus all versions), `VocabularyVersion`, `Status`, `ExternalVocabulary`, and `VocabularyLifecycle`: pure functions for create, new version, status changes. No I/O |
| `validation` | `TurtleSyntaxValidator` (Jena RIOT, multi-error recovery), `StructureValidator` (D16), `SemanticValidator` (D17, D18), `ContentValidator` (pipeline) |
| `store` | `JenaStore`: all persistence through `RDFConnection`. Also implements `ReferenceResolver` for semantic validation |
| `service` | `VocabularyService`, `ExternalVocabularyService`: use cases, upload policy (415, 413), write lock, monotonic clock |
| `web` | Controllers, request checks (400 issues), DTOs shaped as the OpenAPI schemas, `ApiExceptionHandler` |
| `problem` | Problem Details model and a writer that always emits `application/problem+json` |
| `config` | Properties, security (resource server, roles, 401/403), Jena wiring |

## Data layout in Fuseki

| Named graph | Content |
|---|---|
| `urn:simpl:vm:meta:{Name}` | Metadata of the vocabulary and all its versions (`https://vocabulary.simpl.local/ns#` terms) |
| `urn:simpl:vm:content:{Name}:{n}` | Parsed triples of version n (used to resolve referenced terms) |
| `urn:simpl:vm:raw:{Name}:{n}` | The uploaded Turtle text as one literal (downloads return the file as uploaded) |
| `urn:simpl:vm:ext-meta:{Name}`, `ext-content`, `ext-raw` | Same for cached external vocabularies |

Every change is one SPARQL Update request (drop and re-insert the metadata graph, insert the new content graphs).
Fuseki runs one request in one transaction, so a new version and the deprecation of all previous versions are
atomic (D9). Read-modify-write sequences are serialised with a lock in the service; this assumes one backend
instance.

## Request flow for "create a version"

1. Security: bearer token valid (401 otherwise) and role `GA_VOCABULARY_ADMIN` (403 otherwise).
2. Path name format (400), vocabulary exists (404).
3. Form fields: no `version` field (D8), `description`, `changelog`, `vocabularyFile` present (400, all issues at once).
4. Vocabulary not DEPRECATED (409, D11).
5. File is Turtle (415, D15), file not above the limit (413, D30).
6. Syntax validation; on errors 400 with every error and its line and column, semantic validation skipped.
7. Structure (D16) and semantic (D17) validation together; any error gives 400 with all issues.
8. Lifecycle: next number, new version ACTIVE, all previous DEPRECATED; one atomic update.
9. 201 with `Location`, the version metadata and `validationWarnings` (D20).

## UI

Vue 3 single page application with vue-router, oidc-client-ts (code flow with PKCE) and n3 (browser Turtle
parser for the pre-check on file selection). Views: list, create, details with version history, version details,
new version. Admin-only actions are hidden for viewers (D26); the API still enforces 403.
