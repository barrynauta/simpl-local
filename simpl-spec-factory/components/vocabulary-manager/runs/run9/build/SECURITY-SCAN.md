# SECURITY-SCAN.md

Tool: Trivy 0.76.2 (`/opt/homebrew/bin/trivy`), vulnerability DB of 19 Nov 2026. Severity HIGH and CRITICAL.
Raw results: `scan/*.json`.

Commands:

```
trivy fs --severity HIGH,CRITICAL --skip-dirs ui/node_modules --skip-dirs backend/target .
trivy image --severity HIGH,CRITICAL <image>      # for every line of ./stack.sh images
```

## Counts per target

"Before" is the state the interrupted earlier attempt of this step started from; it did not keep a record of
those counts, so they are unknown. The fixes it made are visible in the build files: current base images
(Alpine 3.24, Temurin 21 JRE, node 24, nginx-unprivileged 1.31), all pinned by digest, `apk upgrade` in the UI
image, Spring Boot 4.1.2 and Jena 6.2.0. "After" is the scan of this run, after the final image builds.

| Target | Before | After (CRITICAL / HIGH) |
|---|---|---|
| `work/` file system (Maven and npm dependencies, Dockerfiles, compose) | not recorded | 0 / 0 |
| `vm-factory-run9/vocabulary-manager-be:1.0.0` (own) | not recorded | 0 / 0 |
| `vm-factory-run9/vocabulary-store:1.0.0` (own, Fuseki 6.2.0) | not recorded | 0 / 0 |
| `vm-factory-run9/vocabulary-manager-ui:1.0.0` (own) | not recorded | 0 / 0 |
| `quay.io/keycloak/keycloak:26.8.2@sha256:7e66a96e...` (third party, run as is) | not recorded | 1 / 6 |

## Findings left open

All seven are in the upstream Keycloak image, the newest one published (26.8.2-3, pushed 19 Nov 2026, same
digest as `26.8.2` and `latest`). The build does not change third-party images; Keycloak is the test identity
provider of the local stack (E1, dev mode allowed by D98, port on 127.0.0.1 only). In production the platform
Keycloak of Tier 1 is used, not this image.

| Id | Severity | Package | Installed | Reason it stays |
|---|---|---|---|---|
| CVE-2026-114938 | CRITICAL | java-21-openjdk-headless (RHEL 9 RPM) | 21.0.13.1.2-1.2.el9 | No fixed version: Red Hat status "affected", no fixed RPM. No newer Keycloak image exists. |
| CVE-2026-91403 | HIGH | java-21-openjdk-headless | 21.0.13.1.2-1.2.el9 | No fixed version: Red Hat status "will_not_fix". |
| CVE-2026-119373 | HIGH | sqlite-libs | 3.34.1-11.el9_8 | No fixed version (status "affected"). Keycloak does not use SQLite; the library is part of the UBI base. |
| CVE-2026-119374 | HIGH | sqlite-libs | 3.34.1-11.el9_8 | As above. |
| CVE-2026-119379 | HIGH | sqlite-libs | 3.34.1-11.el9_8 | As above. |
| CVE-2026-119586 | HIGH | sqlite-libs | 3.34.1-11.el9_8 | As above. |
| CVE-2026-102554 | HIGH | com.google.guava:guava (in `/opt/keycloak/lib/lib/main`) | 33.6.0-jre | Fixed in 33.7.2, but no Keycloak release ships it yet. Swapping a jar inside the Keycloak distribution would mean building and maintaining an own Keycloak image whose behaviour is no longer the tested upstream one; for a local test identity provider that risk is not worth it. Revisit when Keycloak 26.8.3 or later is out. |

No finding is open in the build's own images or dependencies (Q5).
