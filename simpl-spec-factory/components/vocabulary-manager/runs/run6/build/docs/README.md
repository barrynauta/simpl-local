# Vocabulary Manager (spec-factory run 6)

The Vocabulary Manager stores and versions the vocabularies (Turtle files) of the Governance Authority. It validates every upload (Turtle syntax, then the semantic rules S1 to S7 of decision D9), numbers versions automatically, deprecates older versions when a new one is created, and keeps a register of cached external vocabularies. A Vue 3 UI covers the list, details, history, download, create, new version and status change screens.

## Parts

| Part | Folder | Port on the host |
|---|---|---|
| Backend (Java 21, Spring Boot 4.1.2, API under `/v1`) | `backend/` | 8196 |
| UI (Vue 3, Vite, TypeScript, served by nginx that also proxies `/v1`) | `ui/` | 4433 |
| Identity provider (Keycloak 26.8.2, realm `simpl`, imported at start) | `keycloak/` | 8193 |
| Store (Apache Jena Fuseki 6.2.0, TDB2 dataset `vocabulary`) | `fuseki/` | not published |
| Second backend with an unreachable store (decision D44) | profile `store-down` | 8199 |

## Run

```
./stack.sh up              # docker compose -p vm-factory-run6 up -d --build --wait
./smoke.sh                 # main flow end to end, plus 401 and 403
./stack.sh store-down-up   # backend on 8199 whose store is unreachable
./stack.sh store-down-down
./stack.sh images          # every image of the stack
./stack.sh down
```

Users (password `Password1!`): `ga-admin` (GA_VOCABULARY_ADMIN), `ga-viewer` (GA_VOCABULARY_VIEWER), `ga-norole` (no role). Test token: password grant on the public client `vocabulary-tests` at `http://localhost:8193/realms/simpl/protocol/openid-connect/token`.

UI: open `http://localhost:4433`, log in through Keycloak.

## Own tests

```
cd backend && mvn verify        # unit and integration tests, JaCoCo report in target/site/jacoco
cd ui && npm ci && npm test     # Vitest with V8 coverage in ui/coverage
```

Copies of the coverage reports, the Trivy results and the Schemathesis runs are in `reports/`.

## Operations

- Backup: `ops/backup.sh [file]` dumps the whole dataset as N-Quads (online).
- Restore: `ops/restore.sh <file>` stops the backend, replaces the dataset, starts the backend.
- Health: `GET /v1/health` (UP, DEGRADED, DOWN, decision D44). Liveness, readiness and Prometheus metrics on the internal management port 8081 (`/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/prometheus`).

More: [architecture.md](architecture.md), [api-notes.md](api-notes.md).
