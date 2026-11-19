# Vocabulary Manager (run 7)

The Vocabulary Manager is the Simpl-Open component where the Governance Authority manages vocabularies: create a vocabulary from a Turtle file, add versions, change the status of a vocabulary or a version, browse the history, download content, and register external vocabularies. Consumers such as Schema Manager read it through the search and content operations. It implements the decisions in `input/decisions/` and the Vocabulary Management API (`input/specs/api/vocabulary_openapi-tier1.yaml`, with the decided changes of D79).

## Run

Requirements: Docker with Compose v2, ports 8196, 4433, 8193, 3141 (loopback) and 8199 free.

```
./stack.sh up          # build and start; returns when every service is healthy
./smoke.sh             # main flow, 403 and 401 checks
./stack.sh down        # stop (data volume kept)
```

| Service | URL |
|---|---|
| API | http://localhost:8196/v1 (for example `GET /v1/health`) |
| UI | http://localhost:4433 (log in as ga-admin, ga-viewer or ga-norole, password `Password1!`) |
| Keycloak | http://localhost:8193, realm `simpl`; token endpoint `/realms/simpl/protocol/openid-connect/token`, public test client `vocabulary-tests` (password grant) |
| Fuseki | http://127.0.0.1:3141 (loopback only, debugging) |
| Store-down backend | http://localhost:8199/v1 after `./stack.sh store-down-up` (D44 tests); remove with `./stack.sh store-down-down` |

Get a token:

```
curl -s -X POST http://localhost:8193/realms/simpl/protocol/openid-connect/token \
  -d grant_type=password -d client_id=vocabulary-tests -d username=ga-admin --data-urlencode 'password=Password1!'
```

## Configuration (environment variables)

| Variable | Default | Meaning |
|---|---|---|
| `STORE_URL` | `http://localhost:3030/vocabulary` | Fuseki dataset URL |
| `ISSUER_URI` | `http://localhost:8180/realms/simpl` | the one accepted token issuer (E5); the stack sets `http://localhost:8193/realms/simpl` |
| `JWKS_URI` | issuer + `/protocol/openid-connect/certs` | where the realm keys are fetched |
| `TOKEN_AUDIENCE` | `vocabulary-manager-be` | required audience |
| `VOCABULARY_CONTENT_BASE_URL` | `https://authority.example/vocabulary-manager` | base of `contentHref` and of internal references (E2, D12) |
| `KEYCLOAK_PUBLIC_URL` | `http://localhost:8193` | compose: Keycloak hostname and the UI's login URL |

## Develop and test

```
cd backend && mvn verify          # unit and integration tests (embedded Fuseki), JaCoCo, Spotless check
cd ui && npm ci && npm test       # Vitest; npm run build type-checks and builds
python3 ui/e2e/ui_check.py        # Playwright UI check with axe (needs the stack and Playwright for Python)
python3 ops/load-check.py 60      # Q1 load check
ops/backup-restore-test.sh        # Q3 round trip
```

Coverage report: `docs/coverage/jacoco/index.html`.

## Documents

- `docs/architecture.md`: components, data model in the store, request pipeline.
- `docs/api-notes.md`: how the API behaves where the decisions refine the contract.
- `TRACEABILITY.md`, `REUSE.md`, `QUALITY.md`, `DEVIATIONS.md`, `SECURITY-SCAN.md`, `BUILD-LOG.md`.
