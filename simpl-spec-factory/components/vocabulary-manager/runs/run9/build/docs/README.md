# Vocabulary Manager (factory run 9)

The Vocabulary Manager is the Simpl-Open component where the Governance Authority manages vocabularies: Turtle
definitions with numbered, immutable versions, a vocabulary status and a version status, validation on upload, and
registered copies of external vocabularies. Schema Manager reads it through the search and content operations.

## What is in this folder

| Path | What |
|---|---|
| `backend/` | Java 21, Spring Boot 4.1.2, Maven. The API of `docs/openapi-decided.yaml` under `/v1`. |
| `ui/` | Vue 3, Vite, TypeScript, served by nginx, which also proxies `/v1` to the backend. |
| `fuseki/` | The store: Apache Jena Fuseki Main 6.2.0, dataset `vocabulary`, users per access level. |
| `keycloak/` | Realm imports: realm `simpl` (E1, E7) and realm `simpl-other` (E7). |
| `ops/` | Backup, restore, round-trip test, schedule examples. |
| `perf/measure.py` | Own measurement of the time targets. |
| `compose.yaml`, `stack.sh` | The stack and its one entry point. |
| `smoke.sh` | The finish-line smoke test. |

## Run

```
./stack.sh up          # builds and starts everything; returns when the stack is usable
./smoke.sh             # main flow, 401 and 403
./stack.sh down        # stops it (the store volume is kept)
```

`stack.sh up` creates `.env` with random store and Keycloak admin passwords on first use. Then the plain profile
command `docker compose -p vm-factory-run9 up -d --build --wait` works as well.

| What | URL |
|---|---|
| API | http://localhost:8196/v1 (health: `/v1/health`, no token) |
| UI | http://localhost:4433 (log in as ga-admin, ga-viewer or ga-norole, password `Password1!`) |
| Keycloak | http://localhost:8193 (bound to 127.0.0.1 only, D98) |
| Token | `POST http://localhost:8193/realms/simpl/protocol/openid-connect/token` with `grant_type=password`, `client_id=vocabulary-tests` |
| Store | not published to the host (D83); reachable only on the internal network |
| Metrics | management port 8081 of the backend, internal network only (D67, D98) |

`./stack.sh store-down-up` starts a second backend on port 8199 whose store URL points at a port where nothing
listens (D44); `./stack.sh store-down-down` removes it.

## Build and test without Docker

```
cd backend && mvn verify      # unit and integration tests (embedded Fuseki), Spotless check, JaCoCo report
cd ui && npm ci && npm run build   # ESLint, Prettier check, vue-tsc, Vitest, Vite build
```

JaCoCo report: `backend/target/site/jacoco/index.html` (a copy of the summary is in `QUALITY.md`).

## More

- [architecture.md](architecture.md): components, flows, patterns.
- [api-notes.md](api-notes.md): how the build reads the contract, order of checks, problem types.
- [logical-data-model.md](logical-data-model.md) and [physical-model.md](physical-model.md) (D87).
- [openapi-decided.yaml](openapi-decided.yaml): the published contract with the changes of D79 and D91.
- [operations.md](operations.md): configuration, backup and restore, schedule, logs and metrics.
- [glossary.md](glossary.md): the decided glossary (D97).
