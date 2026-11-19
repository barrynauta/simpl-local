# Security scan

Tool: Trivy 0.76.2 (`/opt/homebrew/bin/trivy`), vulnerability scanner, database of 19 Nov 2026. Commands:

```
trivy fs --scanners vuln --severity HIGH,CRITICAL --skip-dirs ui/node_modules .
trivy image --scanners vuln --severity HIGH,CRITICAL <image>     # for every line of ./stack.sh images
```

Full outputs: `reports/trivy-*.txt`. SBOMs: `sbom/sbom-backend.json` (CycloneDX, 92 components), `sbom/sbom-ui.json` (CycloneDX, 14 runtime components).

## Counts (HIGH / CRITICAL)

| Target | Before fixes | After fixes |
|---|---|---|
| `backend/pom.xml` (fs) | 0 / 0 | 0 / 0 |
| `ui/package-lock.json` (fs) | 0 / 0 | 0 / 0 |
| `vm-factory-run6/vocabulary-manager-be:1.0.0` (eclipse-temurin 21 JRE, Alpine 3.24.4) | 0 / 0 | 0 / 0 |
| `vm-factory-run6/fuseki:6.2.0` (eclipse-temurin 21 JRE, Alpine 3.24.4) | 0 / 0 | 0 / 0 |
| `vm-factory-run6/vocabulary-manager-ui:1.0.0` (nginx-unprivileged, Alpine 3.24.2) | 12 / 1 | 0 / 0 |
| `quay.io/keycloak/keycloak` | 26.7: 14 / 2 | 26.8.2: 6 / 1 |
| Total | 26 HIGH / 3 CRITICAL | 6 HIGH / 1 CRITICAL |

## Fixes

1. UI image: the newest `nginxinc/nginx-unprivileged:alpine` digest still ships Alpine 3.24.2 packages with fixed versions available (brotli-libs, curl, libcurl, libexpat, libxslt, musl, musl-utils, zlib). The Dockerfile now runs `apk upgrade --no-cache` as root in the final stage and switches back to user 101. Result 0.
2. Keycloak: 26.7.5 replaced by 26.8.2 (pinned by digest `sha256:c68e286c...`). The realm import and the smoke test work unchanged. Result 16 to 7.

## Findings left open (all in the Keycloak product image)

| Id | Severity | Package | Installed | Reason it stays |
|---|---|---|---|---|
| CVE-2026-114938 | CRITICAL | java-21-openjdk-headless (RHEL 9) | 21.0.13.0.9-1.2.el9 | Status `affected`: Red Hat has no fixed package yet. 26.8.2 is the newest Keycloak image. |
| CVE-2026-91403 | HIGH | java-21-openjdk-headless (RHEL 9) | 21.0.13.0.9-1.2.el9 | Status `will_not_fix` by Red Hat. |
| CVE-2026-119373 | HIGH | sqlite-libs (RHEL 9) | 3.34.1-11.el9_8 | Status `affected`, no fixed package. Keycloak does not use SQLite (its store here is the embedded H2 of `start-dev`); the library comes with the OS layer. |
| CVE-2026-119374 | HIGH | sqlite-libs | 3.34.1-11.el9_8 | As above. |
| CVE-2026-119379 | HIGH | sqlite-libs | 3.34.1-11.el9_8 | As above. |
| CVE-2026-119586 | HIGH | sqlite-libs | 3.34.1-11.el9_8 | As above. |
| CVE-2026-102554 | HIGH | com.google.guava:guava (in Keycloak) | 33.6.0-jre | Fixed in 33.7.2, but only a new Keycloak release can ship it: the Keycloak image is a minimal UBI image without a package manager, and replacing jars inside a vendor product is unsupported and would be a patched fork (REUSE-09). Keycloak is the catalogue product (`identity-provider`) and is reachable only on localhost in this stack. |

The Keycloak image is a third-party product image used as is, as the reuse catalogue asks. All images built here have 0 HIGH and 0 CRITICAL.
