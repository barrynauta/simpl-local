# Vocabulary Manager (factory run 12)

The Vocabulary Manager lets the Governance Authority manage vocabularies (business process BP02B):
create a vocabulary from a Turtle file, add versions, deprecate or reactivate vocabularies and
versions, list, search and download them, and register cached copies of external vocabularies.
Every upload is validated (Turtle syntax, then semantic rules S1 to S7) and gets a validation
report or validation warnings. It is built from the stories, the OpenAPI and the decisions in
`../input/`.

## What runs

| Service | What | Host port |
|---|---|---|
| `gateway` | nginx, API entry point, load-balances the backend replicas | 127.0.0.1:8196 (`/v1`) |
| `backend` (2 replicas) | Spring Boot 4.1.3, Java 21 | none (management port 9090 inside the stack only) |
| `fuseki` | Apache Jena Fuseki 6.2.0, TDB2 dataset `vocabulary` | none (internal network) |
| `keycloak` | Keycloak 26.8.2, realms `simpl` and `simpl-other` | 127.0.0.1:8193 |
| `ui` | Vue 3 + PrimeVue (unstyled) + Tailwind, served by nginx, proxies `/v1` | 127.0.0.1:4433 |
| `backend-store-down` | optional backend whose store is unreachable (`./stack.sh store-down-up`) | 127.0.0.1:8199 |

## How to run

```bash
./stack.sh up            # creates .env with local secrets on first run, builds, waits until healthy
./smoke.sh               # main flow: create, new version, list, download, search, 403, 401
./stack.sh images        # images of the stack
./stack.sh store-down-up # backend with an unreachable store on 8199; ./stack.sh store-down-down removes it
./stack.sh down          # stop (volumes are kept)
```

Test users (password `Password1!`): `ga-admin` (GA_VOCABULARY_ADMIN), `ga-viewer`
(GA_VOCABULARY_VIEWER), `ga-norole`. Token for scripts:

```bash
curl -s -X POST http://localhost:8193/realms/simpl/protocol/openid-connect/token \
  -d grant_type=password -d client_id=vocabulary-tests -d username=ga-admin -d 'password=Password1!'
```

UI: open http://localhost:4433 and log in at Keycloak.

## Build and test

```bash
cd backend && mvn verify      # Spotless (Palantir) check, 59 tests, JaCoCo report and 80/60 % gate
cd ui && npm ci && npm run build   # ESLint, Prettier, vue-tsc, 32 Vitest tests, Vite build
```

The JaCoCo report of the last run is in `docs/jacoco-report/index.html`.

## Documents

- [architecture.md](architecture.md): components, request pipeline, store model, concurrency, resilience.
- [api-notes.md](api-notes.md): the decided contract, error model, order of checks.
- [openapi-decided.yaml](openapi-decided.yaml): the published OpenAPI with every decided change (D87).
- [logical-data-model.md](logical-data-model.md): logical model traced to the CDM, with differences.
- [physical-model-shapes.ttl](physical-model-shapes.ttl): SHACL shapes per named graph (physical model).
- [operations.md](operations.md): backup, restore, compaction, schedules, metrics, logs.
- [glossary.md](glossary.md): the decided glossary (copy of `input/decisions/glossary.md`).
- `../ui/UI-NOTES.md`: UI build, decisions per view, UI tests.
