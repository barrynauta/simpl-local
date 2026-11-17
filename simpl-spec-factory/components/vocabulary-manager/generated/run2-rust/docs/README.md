# Vocabulary Manager backend (Rust)

A backend for the Simpl-Open Vocabulary Manager, built only from the specifications in
`../input/` (user stories, OpenAPI, R07.02, BP02B) and the spec digest with decisions D1 to D30.

It lets the Governance Authority manage internal vocabularies (Turtle files) with automatic
versioning, status management and validation, and keep cached copies of external vocabularies
that validation can resolve against.

## What is in the box

| Service | What | Host port |
|---|---|---|
| `backend` | This Rust service (axum, tokio) | `http://localhost:8097/v1` |
| `keycloak` | Identity provider, realm `simpl` imported at start | `http://localhost:8184` |
| `fuseki` | Apache Jena Fuseki 5.3, TDB2 dataset `vocab` | not exposed |

Compose project name: `vocab-rust`.

## Run

```bash
cd build
docker compose up -d --build --wait
./smoke.sh
```

`--wait` returns when all three services report healthy. No manual steps and no external accounts.
Data is kept in the named volume `vocab-rust_fuseki-data`. `docker compose down -v` removes it.

## Tokens

Password grant on the public client `vocabulary-tests`:

```bash
curl -s -X POST http://localhost:8184/realms/simpl/protocol/openid-connect/token \
  -d grant_type=password -d client_id=vocabulary-tests \
  -d username=ga-admin -d password=password
```

| User | Password | Roles in the `roles` claim |
|---|---|---|
| `ga-admin` | `password` | `GA_VOCABULARY_ADMIN` (read and write) |
| `ga-viewer` | `password` | `GA_VOCABULARY_VIEWER` (read) |
| `ga-norole` | `password` | no vocabulary role (gets 403) |

The public client `vocabulary-ui` (authorization code with PKCE S256) has redirect URIs and web
origins for `http://localhost:4333/*` and `http://localhost:4323/*`.

### Token issuer

Keycloak runs with `KC_HOSTNAME=http://localhost:8184`. With a fixed hostname, every token has
`iss = http://localhost:8184/realms/simpl`, no matter whether it was requested from the host
(`localhost:8184`) or from inside the compose network (`keycloak:8080`).
`KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true` keeps the back-channel endpoints reachable on the internal
name. The backend accepts that one issuer (`OIDC_ISSUERS`, comma separated if you need more) and
fetches the signing keys from the internal JWKS URL `http://keycloak:8080/realms/simpl/protocol/openid-connect/certs`.
Both paths were tested.

## Configuration (environment variables)

| Variable | Default | Meaning |
|---|---|---|
| `BIND_ADDR` | `0.0.0.0:8080` | Listen address |
| `PUBLIC_BASE_URL` | `http://localhost:8097/v1` | Absolute base used in `contentHref` |
| `STORE` | `fuseki` | `fuseki` or `memory` (no persistence, for local runs) |
| `FUSEKI_URL` | `http://fuseki:3030` | Fuseki base URL |
| `FUSEKI_DATASET` | `vocab` | Dataset name |
| `FUSEKI_USER`, `FUSEKI_PASSWORD` | unset | Optional basic auth for Fuseki |
| `OIDC_ISSUERS` | `http://localhost:8184/realms/simpl` | Accepted `iss` values, comma separated |
| `OIDC_JWKS_URL` | `http://keycloak:8080/realms/simpl/protocol/openid-connect/certs` | Signing keys |
| `OIDC_AUDIENCE` | unset | If set, `aud` must contain it |
| `MAX_UPLOAD_BYTES` | `10485760` | Upload limit (D30) |
| `REFERENCE_BASE` | `https://vocabulary.simpl.local/vocabularies` | Base of internal references (D18) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4333,http://localhost:4323` | CORS origins |
| `RUST_LOG` | `info` | Log filter |

## Develop and test

```bash
cargo test            # unit tests and router-level integration tests (in-memory store, test JWT key)
cargo clippy --all-targets
STORE=memory OIDC_JWKS_URL=http://localhost:8184/realms/simpl/protocol/openid-connect/certs cargo run
```

## More

- [architecture.md](architecture.md): components, storage layout, request flow.
- [api-notes.md](api-notes.md): behaviour per endpoint, where the OpenAPI left room.
- [decisions.md](decisions.md): how D1 to D30 are implemented.
- [../TRACEABILITY.md](../TRACEABILITY.md): stories and decisions mapped to code and tests.
- [../BUILD-LOG.md](../BUILD-LOG.md): gaps, choices, problems.
