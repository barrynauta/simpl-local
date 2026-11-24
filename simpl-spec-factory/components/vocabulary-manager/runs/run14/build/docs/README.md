# Vocabulary Manager (Vocabulary Hub), factory run 14

The Vocabulary Manager lets the Governance Authority manage vocabularies of Simpl-Open: create a vocabulary from a
Turtle file, create new versions, deprecate or reactivate vocabularies and versions, download content, search
vocabularies and versions, and register external vocabularies (cached copies such as Dublin Core) used to resolve
references. It is built from the specs and decisions in `../input/` (BP02B, the SIMPL-181xx stories, SIMPL-18452,
SIMPL-18559 and decisions D1 to D210, E1 to E8, Q1 to Q6).

## Run

```
bash stack.sh up        # builds and starts everything, returns when the stack is usable
./smoke.sh              # main flow end to end, 401 and 403
bash stack.sh down      # stops the stack (the store volume stays)
```

`stack.sh up` writes `.env` with random local secrets on the first run (D131). It needs Docker with Compose v2.

| What | URL |
|---|---|
| API | http://localhost:8196/v1 (for example `/v1/vocabularies`) |
| UI | http://localhost:4433 (log in as ga-admin, ga-viewer or ga-norole, password `Password1!`) |
| Keycloak | http://localhost:8193 (realms `simpl` and `simpl-other`) |
| Second backend with an unreachable store | `bash stack.sh store-down-up`, then http://localhost:8199/v1 |

Token for tests: `POST http://localhost:8193/realms/simpl/protocol/openid-connect/token` with
`grant_type=password`, `client_id=vocabulary-tests`, `username=ga-admin`, `password=Password1!`.

## Build and test without Docker

```
cd backend && mvn verify         # unit and integration tests (embedded Fuseki), Spotless, JaCoCo (target/site/jacoco)
cd ui && npm ci && npm run lint && npm test && npm run build
```

## Layout

| Path | Content |
|---|---|
| `backend/` | Java 21, Spring Boot 4.1.3, Maven. The API. |
| `ui/` | Vue 3, Vite, TypeScript, PrimeVue (unstyled), Tailwind; nginx config of the proxy for both ports. |
| `fuseki/` | Apache Jena Fuseki 6.3.0 image: configuration, three users, logging without query text. |
| `keycloak/` | Realm imports of E1, E7 and E8. |
| `scripts/` | backup, restore, round trip test, compaction, load test, decided OpenAPI generator. |
| `docs/` | this README, architecture, API notes, data model, SHACL shapes, decided OpenAPI, operations, glossary. |

Other documents at the top: `TRACEABILITY.md`, `REUSE.md`, `QUALITY.md`, `DEVIATIONS.md`, `SECURITY-SCAN.md`,
`THREATS.md`, `BUILD-LOG.md`, `LICENSE` (EUPL 1.2), `sbom-backend.cdx.json`, `sbom-ui.cdx.json`.
