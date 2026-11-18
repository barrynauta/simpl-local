# Scan summary, run 5 (step 70, orchestrator)

Trivy 0.76.2, HIGH and CRITICAL only, run by the orchestrator on 18 Nov 2026, independent of the build's own scan. The first build used Spring Boot 3.5.16; the build profile was changed to Spring Boot 4 and the code writer upgraded the same build to 4.1.2.

| Target | Boot 3 build (CRITICAL / HIGH) | Boot 4.1.2 build (CRITICAL / HIGH) |
|---|---|---|
| backend | 2 / 0 | 0 / 0 |
| deps | 2 / 0 | 0 / 0 |
| fuseki | 0 / 0 | 0 / 0 |
| keycloak | 0 / 6 | 0 / 6 |
| ui | 0 / 0 | 0 / 0 |
