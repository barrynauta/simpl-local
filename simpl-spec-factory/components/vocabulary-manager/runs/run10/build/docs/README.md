# Vocabulary Manager (factory build, run 10)

The Vocabulary Manager is the Simpl-Open component (business name: Vocabulary Hub) where the Governance Authority
manages vocabularies: Turtle files with versions, a vocabulary status and a version status, validation on upload,
downloads, searches for consumers such as Schema Manager, and registered copies of external vocabularies.

It is built from the specifications and decisions in `../input/` only. Terms follow `glossary.md`.

## What is in the stack

| Service | Image | What it does | Host port |
|---|---|---|---|
| `proxy` | `vm-factory-run10/ui-proxy` (nginx, unprivileged) | Serves the UI on 4433 and proxies `/v1` to the backends; serves the API on 8196 | 8196, 4433 |
| `backend` (2 replicas) | `vm-factory-run10/backend` (Java 21, Spring Boot 4.1.3) | The API under `/v1`; management port 8081 (health, Prometheus) internal only | none |
| `fuseki` | `vm-factory-run10/fuseki` (Apache Jena Fuseki 6.2.0, plain mode) | The RDF store, dataset `vocabulary`, users reader, writer, admin | none |
| `keycloak` | `quay.io/keycloak/keycloak:26.8.2` | Realms `simpl` and `simpl-other` with the clients and users of E1 and E7 | 127.0.0.1:8193 |
| `backend-store-down` (profile) | backend image | A backend with an unreachable store, for the D44 tests | 8199 |
| `store-tools` (profile) | `vm-factory-run10/store-tools` (curl on Alpine) | Backup and restore over the internal store network | none |

## How to run

```bash
./stack.sh up            # creates .env with random store and Keycloak admin passwords on the first run,
                         # then: docker compose -p vm-factory-run10 up -d --build --wait
./smoke.sh               # main flow, 401 and 403 checks
./stack.sh store-down-up # second backend with an unreachable store on http://localhost:8199/v1
./stack.sh metrics       # Prometheus metrics of each backend (management port)
./stack.sh down          # stop (data volume kept; ./stack.sh down --volumes drops it)
./stack.sh images        # every image of the stack
```

- API: `http://localhost:8196/v1` (for example `GET /v1/vocabularies`), bearer token from
  `http://localhost:8193/realms/simpl/protocol/openid-connect/token` (client `vocabulary-tests`, password grant).
- UI: `http://localhost:4433` (login through Keycloak; users `ga-admin`, `ga-viewer`, `ga-norole`, password
  `Password1!`, decision E1).
- Every setting is an environment variable (E4); see `compose.yaml` and `backend/src/main/resources/application.yml`.
  Ports: `API_PORT`, `UI_PORT`, `KEYCLOAK_PORT`, `API_STORE_DOWN_PORT`; limits: `BACKEND_CPUS`, `BACKEND_MEMORY`,
  `BACKEND_REPLICAS` and the same for the other services.

## Build and tests

```bash
cd backend && mvn verify     # unit and integration tests (embedded Fuseki, test identity provider), JaCoCo
                             # report in target/site/jacoco, Spotless (Palantir) check, coverage gate 80/60
cd ui && npm ci && npm run build   # ESLint, Prettier check, vue-tsc, Vitest component tests, Vite build
python3 scripts/make-decided-openapi.py ../input/specs/api/vocabulary_openapi-tier1.yaml docs/openapi-decided.yaml
scripts/backup-restore-test.sh     # backup and restore round trip on the running stack (Q3)
```

## Documents

- `architecture.md`: components, request flow, check order, concurrency, failure handling, logical data model.
- `api-notes.md`: how the API behaves where the published contract is silent or changed by a decision.
- `openapi-decided.yaml`: the published contract with every change of D79 applied (generated).
- `physical-model-shapes.ttl`: SHACL shapes of each named graph of the store (tested by the build).
- `backup-schedule.md`: backup, restore and the example schedule.
- `glossary.md`: copy of the decided glossary.
- In the build root: `TRACEABILITY.md`, `REUSE.md`, `QUALITY.md`, `DEVIATIONS.md`, `SECURITY-SCAN.md`,
  `BUILD-LOG.md`, `LICENSE` (EUPL 1.2), `sbom/`.
