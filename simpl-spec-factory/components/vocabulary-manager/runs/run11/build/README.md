# Vocabulary Manager (spec factory, run 11)

```
./stack.sh up      # start (builds images, creates .env with local secrets), returns when usable
./smoke.sh         # main flow, 401 and 403
./stack.sh down
```

API http://localhost:8196/v1, UI http://localhost:4433, Keycloak http://localhost:8193.

| Folder or file | Content |
|---|---|
| `backend/` | Spring Boot service (Maven; `mvn verify` runs tests, Spotless, JaCoCo) |
| `ui/` | Vue 3 UI and the nginx proxy configuration |
| `fuseki/`, `keycloak/` | store image and realm imports |
| `ops/` | backup, restore, round-trip test, example schedule |
| `tools/` | decided OpenAPI generator, load measurement, parallel write check |
| `docs/` | README, architecture with the logical and physical model, API notes, operations, decided OpenAPI, glossary, coverage report |
| `scans/` | Trivy reports |
| `sbom.cdx.json`, `sbom-ui.cdx.json` | CycloneDX SBOMs of the backend and the UI |
| TRACEABILITY.md, REUSE.md, QUALITY.md, DEVIATIONS.md, SECURITY-SCAN.md, BUILD-LOG.md | build reports |

Licence: EUPL 1.2 (`LICENSE`).
