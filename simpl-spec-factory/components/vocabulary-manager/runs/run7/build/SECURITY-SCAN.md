# Security scan

- Tool: Trivy 0.76.2 (`/opt/homebrew/bin/trivy`), vulnerability database as downloaded on 19 Nov 2026.
- Commands: `trivy fs --severity HIGH,CRITICAL .` in `work/` (scans `backend/pom.xml` and `ui/package-lock.json`), and `trivy image --severity HIGH,CRITICAL <image>` for every image printed by `./stack.sh images`.
- Raw JSON results: `security-scan/`.

## Counts (HIGH / CRITICAL)

| Target | Before fixes | After fixes |
|---|---|---|
| `backend/pom.xml` (Maven dependencies) | 0 / 0 | 0 / 0 |
| `ui/package-lock.json` (npm dependencies) | 0 / 0 | 0 / 0 |
| `vm-factory-run7/backend:1.0.0` (Temurin 21 JRE Alpine, pinned digest) | 0 / 0 | 0 / 0 |
| `vm-factory-run7/fuseki:6.2.0` (Temurin 21 JRE Alpine, Fuseki 6.2.0) | 0 / 0 | 0 / 0 |
| `vm-factory-run7/ui:1.0.0` (nginx-unprivileged Alpine) | 0 / 0 | 0 / 0 |
| `quay.io/keycloak/keycloak:26.8.2` (pinned digest) | 6 / 1 | 6 / 1 |

"Before" and "after" are the same for the own targets: they were built from current base images with `apk upgrade` and the newest library versions from the start (Spring Boot 4.1.2, Jena 6.2.0, Fuseki 6.2.0), so the first scan found nothing to fix. The store-down backend uses the backend image.

## Findings left open

All in the third-party Keycloak image. The pinned digest is the newest Keycloak build (tags `26.8.2`, `26.8.2-3`, `26.8` and `latest` all point to it on 19 Nov 2026).

| Id | Severity | Package | Installed | Fixed | Why it stays |
|---|---|---|---|---|---|
| CVE-2026-114938 | CRITICAL | java-21-openjdk-headless (RPM) | 1:21.0.13.1.2-1.2.el9 | none | No fixed RPM exists. The Keycloak image is UBI 9 micro without a package manager, so the package cannot be upgraded in a derived image; it needs a new upstream Keycloak build. |
| CVE-2026-91403 | HIGH | java-21-openjdk-headless (RPM) | 1:21.0.13.1.2-1.2.el9 | none | Same as above. |
| CVE-2026-119373 | HIGH | sqlite-libs (RPM) | 3.34.1-11.el9_8 | none | No fixed RPM exists. Keycloak runs with `--db=dev-file` (H2), SQLite is not used by it. |
| CVE-2026-119374 | HIGH | sqlite-libs (RPM) | 3.34.1-11.el9_8 | none | Same as above. |
| CVE-2026-119379 | HIGH | sqlite-libs (RPM) | 3.34.1-11.el9_8 | none | Same as above. |
| CVE-2026-119586 | HIGH | sqlite-libs (RPM) | 3.34.1-11.el9_8 | none | Same as above. |
| CVE-2026-102554 | HIGH | com.google.guava:guava (inside Keycloak) | 33.6.0-jre | 33.7.2 | A fixed version exists, but replacing a library inside the Keycloak distribution breaks its build-time augmentation and is not supported; it needs a new upstream Keycloak release. Keycloak is not reachable from outside the stack except its login and token endpoints on localhost. |

## Other checks

- Fuseki jar verified against its SHA-512 (pinned in `fuseki/Dockerfile`; matches the Maven Central SHA-1).
- All base images pinned by digest; Maven and npm versions pinned (`pom.xml`, `package-lock.json`).
- SBOMs: `sbom/backend-bom.cyclonedx.json` (CycloneDX 1.6, 86 components), `sbom/ui-bom.cyclonedx.json`.
- Fuzzing (Schemathesis 4.33.4, 48 068 generated requests, seed 1, 300 s): no 5xx, no response that breaks the contract schema, undeclared methods give 405. See BUILD-LOG.md.
