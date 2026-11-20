# Vocabulary Manager (factory run 11)

The Vocabulary Manager lets the Governance Authority manage vocabularies of Simpl-Open: create a
vocabulary from a Turtle file, create new versions (the older ACTIVE versions are deprecated in the
same step), change the vocabulary or version status, browse the version history, download the
content, and register external vocabularies that uploads may reference. Consumers such as Schema
Manager read the state through the search and content operations (pull only, D45).

## Run

Requirements: Docker with Compose v2, bash, curl, python3 (for `smoke.sh`).

```
./stack.sh up          # builds and starts everything; creates local secrets in .env the first time
./smoke.sh             # main flow end to end, plus 401 and 403
./stack.sh down        # stops the stack (the store volume is kept)
```

| What | URL |
|---|---|
| API (through the proxy, two backend replicas) | http://localhost:8196/v1 |
| UI | http://localhost:4433 (log in as ga-admin, ga-viewer or ga-norole, password `Password1!`) |
| Keycloak (127.0.0.1 only) | http://localhost:8193, realms `simpl` and `simpl-other` |
| Store-down backend (tests of D44) | http://localhost:8199/v1 after `./stack.sh store-down-up` |

The RDF store (Fuseki) publishes no port (D83, D107). Health and metrics of each backend are on its
management port 8081 inside the stack (`/actuator/health`, `/actuator/prometheus`), e.g.
`docker compose -p vm-factory-run11 exec backend wget -qO- http://localhost:8081/actuator/prometheus`.

## Build and test

```
cd backend && mvn verify        # unit and integration tests, Spotless, JaCoCo (report in target/site/jacoco)
cd ui && npm ci && npm run build   # ESLint, Prettier check, vue-tsc, Vitest, Vite build
```

The integration tests start the application against an embedded Fuseki and a fake identity
provider that serves realm keys; no container is needed.

## Other documents

- [architecture.md](architecture.md): components, flows, logical and physical data model
- [api-notes.md](api-notes.md): the decided contract, order of checks, error bodies
- [operations.md](operations.md): configuration, backup and restore, limits
- [openapi-decided.yaml](openapi-decided.yaml): the published OpenAPI with the D79 changes
- [physical-model.ttl](physical-model.ttl): SHACL shapes of the store's named graphs
- [glossary.md](glossary.md): the decided glossary (copy of the step 30 glossary)
- [coverage-report/index.html](coverage-report/index.html): JaCoCo report of the last `mvn verify`
- [load-run.txt](load-run.txt): output of the own load measurement
