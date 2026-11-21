# Security scan (finish line item 4)

Tool: Trivy 0.76.2 (`/opt/homebrew/bin/trivy`), vulnerability DB of 20 Nov 2026. Commands:

```bash
trivy fs --severity HIGH,CRITICAL work/
for i in $(./stack.sh images); do trivy image --severity HIGH,CRITICAL "$i"; done
```

Additionally: `npm audit --omit=dev` in `ui/` (0 vulnerabilities; dev dependencies have 5
moderate findings that are not shipped in the image).

## Counts (HIGH / CRITICAL)

| Target | Before fixes | After fixes | Fix applied |
|---|---|---|---|
| `trivy fs work/`: backend/pom.xml (Maven) | 0 / 0 | 0 / 0 | current versions from the start: Spring Boot 4.1.3, Jena 6.2.0, Nimbus 10.10, Resilience4j 2.4.0 |
| `trivy fs work/`: ui/package-lock.json (npm) | 0 / 0 | 0 / 0 | none needed |
| vm-factory-run12/backend:1.0.0 (eclipse-temurin:21-jre-alpine, digest pinned) | 0 / 0 | 0 / 0 | `apk upgrade` in the image; base image already clean |
| vm-factory-run12/fuseki:1.0.0 (eclipse-temurin:21-jre-alpine + Fuseki 6.2.0 jar) | 0 / 0 | 0 / 0 | `apk upgrade`; jar download checked with SHA-256 |
| vm-factory-run12/gateway:1.0.0 (nginx-unprivileged:alpine-slim) | 3 / 0 | 0 / 0 | `apk upgrade` fixes musl, musl-utils (CVE-2026-107891) and zlib (CVE-2026-85091) of the base image |
| vm-factory-run12/ui:1.0.0 (nginx-unprivileged:alpine-slim) | 3 / 0 | 0 / 0 | same `apk upgrade` (UI worker, see ui/UI-NOTES.md) |
| quay.io/keycloak/keycloak:26.8.2 (third-party, run as is) | 6 / 0 | 6 / 0 | none possible: 26.8.2 is the newest Keycloak release (26.8.3 and 26.9 do not exist on quay.io on 20 Nov 2026) |

## Findings left open

All are in the third-party Keycloak image of the local stack. In production the Simpl Tier 1
Keycloak is used, not this container.

| Id | Package | Installed | Fixed version | Reason it stays |
|---|---|---|---|---|
| CVE-2026-91403 | java-21-openjdk-headless (Red Hat UBI 9.9 in Keycloak) | 1:21.0.13.1.2-1.2.el9 | none | No fixed package in the distribution; only Keycloak can ship it |
| CVE-2026-119373 | sqlite-libs (UBI 9.9) | 3.34.1-11.el9_8 | none | No fixed version; Keycloak does not use SQLite (dev mode uses the H2 file database) |
| CVE-2026-119374 | sqlite-libs | 3.34.1-11.el9_8 | none | as above |
| CVE-2026-119379 | sqlite-libs | 3.34.1-11.el9_8 | none | as above |
| CVE-2026-119586 | sqlite-libs | 3.34.1-11.el9_8 | none | as above |
| CVE-2026-102554 | com.google.guava:guava (inside the Keycloak distribution) | 33.6.0-jre | 33.7.2 | Fixed version exists but the jar is part of the Keycloak server; patching it means forking the image (REUSE-09). Waits for the next Keycloak release; Keycloak is only a local stand-in for the Tier 1 identity provider and listens on 127.0.0.1 only |

## Other supply-chain measures

- Every base image pinned by digest (Dockerfiles, compose); Fuseki jar downloaded with `ADD --checksum=sha256:...`.
- Library versions pinned in `backend/pom.xml` (Spring Boot parent 4.1.3 manages the rest) and `ui/package-lock.json`.
- SBOMs (CycloneDX JSON): `sbom/backend.cdx.json` (Maven, runtime scope), `sbom/ui.cdx.json` (npm, production dependencies).
