# Architecture

## Components

```
            browser UI (4333/4323)          test harness / scripts
                    |  CORS                         |
                    v                               v
  +-------------------------------- backend (axum) ---------------------------------+
  | CORS layer -> problem mapper -> /v1 router                                       |
  |   /health (public)                                                                |
  |   everything else: require_auth (JWT, JWKS, roles claim) -> handler              |
  |                                                                                   |
  | api::vocabularies / api::external   request parsing, 400/406/413/415, JSON views  |
  | service::VocabularyService          versioning, status rules, validation pipeline |
  | validation                          Turtle syntax, structure, semantics (pure)    |
  | store::Store (trait)                FusekiStore (SPARQL over HTTP) | MemoryStore  |
  +-----------------------------------------------------------------------------------+
           | JWKS (http://keycloak:8080)                 | SPARQL query/update
           v                                             v
       Keycloak 26 (realm simpl)                 Fuseki 5.3 (TDB2 dataset vocab)
```

Source layout (`src/`):

| Module | Role |
|---|---|
| `main.rs`, `config.rs` | Start-up, environment configuration, `healthcheck` sub-command for the container |
| `api/mod.rs` | Router, CORS, mapping of framework errors to Problem Details, health |
| `api/vocabularies.rs` | Internal vocabulary endpoints and JSON views |
| `api/external.rs` | External vocabulary endpoints |
| `api/support.rs` | Query parsing, paging links, content negotiation, multipart reading, file type check |
| `auth.rs` | Bearer token validation, JWKS cache, role rules (D3) |
| `problem.rs` | `ApiError` and the RFC 9457 `Problem` body |
| `service.rs` | Business rules (D7 to D11, D13), validation pipeline, write lock |
| `validation.rs` | Syntax (oxttl), structure (D16), semantics (D17, D18) |
| `model.rs` | Domain types: `Vocabulary` aggregate with `Version`s, `ExternalVocabulary` |
| `store/` | `Store` trait, Fuseki adapter, in-memory adapter, RDF mapping |

## Domain model

A `Vocabulary` is an aggregate: name, creator, creation date and the list of `Version`s. Each
version has a number (1, 2, 3, ...), description, changelog, status, creator, creation and last
update date. The vocabulary status is derived: ACTIVE when at least one version is ACTIVE, else
DEPRECATED. Its description and version number in list views come from the latest version.

## Storage in Fuseki

All data lives in named graphs of the TDB2 dataset `vocab`:

| Graph | Content |
|---|---|
| `urn:simpl:vocabulary-manager:meta:{Name}` | Vocabulary and version metadata (own small vocabulary, namespace `https://simpl-open.eu/ns/vocabulary-manager#`) |
| `urn:simpl:vocabulary-manager:source:{Name}:{n}` | One triple with the original Turtle text of version n (downloads return the exact file) |
| `urn:simpl:vocabulary-manager:content:{Name}:{n}` | The parsed triples of version n |
| `urn:simpl:vocabulary-manager:external-meta:{Name}` and `external-source`, `external-content` | Same for external vocabularies |

Every write is one SPARQL Update request with several operations (`DROP SILENT GRAPH`,
`INSERT DATA`). Fuseki runs one request in one transaction, so creating a version and
deprecating all older versions is atomic (D9). Reads use `CONSTRUCT` queries returning
N-Triples, parsed with oxttl and mapped back to the aggregate (`store/rdf_mapping.rs`).

A process-wide async mutex serialises read-modify-write cycles (create, new version, status
changes). This is safe for one backend instance; several replicas would need optimistic
locking in the store (not built).

## Request flow for a new version

1. CORS and routing. `require_auth` checks the token (401) and the role (403).
2. `Accept` check (406), multipart check (415), streaming read with size limit (413).
3. Field checks: description, changelog, file present, no `version` field (400, D8).
4. File type check: Turtle only (415, D15).
5. Service: vocabulary exists (404), not DEPRECATED (409, D11).
6. Validation pipeline, each stage blocks the next: syntax (all errors with line and column),
   structure, semantics (all errors). Errors give 400 with one issue per finding.
7. All older versions DEPRECATED, new version ACTIVE, stored in one update (D9).
8. 201 with `Location`, metadata and `validationWarnings` when there are warnings (D20).

## Security

- JWT signature checked against the JWKS of the issuer (RS/PS/ES algorithms only, `none` and
  HMAC rejected), `exp` and `iss` checked, 30 s leeway. Keys are cached and refreshed on an
  unknown `kid` (at most every 10 s).
- Roles only from the top-level `roles` array claim (D6).
- Runtime image: `debian:bookworm-slim`, user 10001, no shell tools needed for the health check
  (the binary has a `healthcheck` sub-command).
