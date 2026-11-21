# Vocabulary Manager (factory run 13)

The Vocabulary Manager lets the Governance Authority manage vocabularies (Turtle files) for Simpl-Open: create a vocabulary, create new versions (numbered automatically, older ACTIVE versions are deprecated), change the status of a vocabulary or a version, browse and download every version, and register external vocabularies (cached copies such as Dublin Core) that uploaded content may reference. Every upload is validated: Turtle syntax first, then the semantic rules and the reference check of decisions D9 and D12. Consumers such as the Schema Manager read vocabularies through the search and content operations.

## Run

Requirements: Docker with Compose v2, `openssl`, `bash`, free host ports 8196, 4433, 8193 (and 8199 for the store-down backend).

```bash
./stack.sh up          # builds the images, creates .env with local secrets, waits until healthy
./smoke.sh             # main flow with curl: create, new version, list, download, search, 403, 401
./stack.sh down        # stops the stack (the store volume stays)
./stack.sh images      # every image of the stack
./stack.sh store-down-up / store-down-down   # second backend with an unreachable store on 8199
```

| What | URL |
|---|---|
| API | http://localhost:8196/v1 (for example `GET /v1/vocabularies`) |
| UI | http://localhost:4433 (log in as ga-admin, ga-viewer or ga-norole, password `Password1!`) |
| Keycloak | http://localhost:8193 (realms `simpl` and `simpl-other`, bound to 127.0.0.1) |
| Token for tests | `POST http://localhost:8193/realms/simpl/protocol/openid-connect/token` with `grant_type=password`, `client_id=vocabulary-tests` |

The stack runs two backend replicas behind nginx. The store (Fuseki) and the backend management port (metrics, liveness) publish no host port; read metrics with `docker compose -p vm-factory-run13 exec backend wget -q -O - http://127.0.0.1:8081/actuator/prometheus`.

## Build and test

```bash
cd backend && mvn verify          # unit and integration tests (embedded Fuseki), Spotless check, JaCoCo (report: target/site/jacoco, copy in reports/jacoco)
cd ui && npm ci && npm run lint && npm run format:check && npm test && npm run build
python3 scripts/loadtest.py --vocabularies 1000 --versions 20 --duration 300   # Q1 load test against the stack
./scripts/backup-restore-test.sh                                                # Q3 round trip
```

## Configuration (environment)

| Variable | Default | Use |
|---|---|---|
| `VOCABULARY_CONTENT_BASE_URL` | `https://authority.example/vocabulary-manager` | base of contentHref and of internal references (E2, D12) |
| `STORE_QUERY_URL`, `STORE_UPDATE_URL` | Fuseki in the stack | SPARQL endpoints |
| `STORE_READ_USER/PASSWORD`, `STORE_UPDATE_USER/PASSWORD` | from `.env` | store users (D83) |
| `ISSUER_URI` | `http://localhost:8193/realms/simpl` | accepted token issuer (E5) |
| `JWKS_URI` | Keycloak certs inside the stack | realm keys |
| `TOKEN_AUDIENCE` | `vocabulary-manager-be` | required audience (E5) |
| `MAX_PARALLEL_UPLOADS` | 4 | D92 |
| `STORE_CONNECT_TIMEOUT`, `STORE_READ_TIMEOUT`, `STORE_UPDATE_TIMEOUT` | 2s, 10s, 45s | D44, D71 |
| `BACKEND_REPLICAS`, `*_CPUS`, `*_MEMORY`, `FUSEKI_HEAP` | see `compose.yaml` | D98, D102 |

## Documents

- [architecture.md](architecture.md): components, request path, order of checks, threat model
- [api-notes.md](api-notes.md): the API as built, differences with the published contract
- [openapi-decided.yaml](openapi-decided.yaml): published OpenAPI with the decided changes (D87)
- [logical-data-model.md](logical-data-model.md), [physical-model.md](physical-model.md), [physical-model-shapes.ttl](physical-model-shapes.ttl)
- [glossary.md](glossary.md): the decided glossary
- In the build root: `TRACEABILITY.md`, `REUSE.md`, `QUALITY.md`, `DEVIATIONS.md`, `SECURITY-SCAN.md`, `BUILD-LOG.md`, `LICENSE` (EUPL 1.2)
