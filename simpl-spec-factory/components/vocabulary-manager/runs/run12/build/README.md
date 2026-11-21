# Vocabulary Manager, factory run 12

Start: `./stack.sh up` (API http://localhost:8196/v1, UI http://localhost:4433, Keycloak
http://localhost:8193). Smoke test: `./smoke.sh`. Documentation: [docs/README.md](docs/README.md).

| File | Content |
|---|---|
| `stack.sh`, `docker-compose.yml`, `smoke.sh` | stack entry point, compose file, smoke test |
| `backend/` | Spring Boot 4.1.3 service (Java 21, Maven) |
| `ui/` | Vue 3 UI with nginx |
| `fuseki/`, `keycloak/`, `gateway/` | store image, realm imports, API gateway |
| `ops/` | backup, restore, compaction, schedule examples |
| `docs/` | README, architecture, API notes, decided OpenAPI, data models, operations, glossary, JaCoCo report |
| `tools/` | generator of `docs/openapi-decided.yaml` |
| `sbom/` | CycloneDX SBOMs of backend and UI |
| `TRACEABILITY.md`, `REUSE.md`, `QUALITY.md`, `DEVIATIONS.md`, `BUILD-LOG.md`, `SECURITY-SCAN.md` | build reports |
| `LICENSE` | EUPL 1.2 |
