# Security scan (finish line 4)

Scanner: Trivy 0.76.2 (`/opt/homebrew/bin/trivy`), vulnerability DB version 2 updated 2026-11-23 19:02 UTC.
Commands: `trivy fs --severity HIGH,CRITICAL work/` and `trivy image --severity HIGH,CRITICAL <image>` for every
image of `./stack.sh images`. Final scan on 23 Nov 2026 after the clean rebuild.

## Counts (HIGH / CRITICAL)

| Target | Before fixes | After fixes |
|---|---|---|
| fs: backend/pom.xml (Maven) | 0 / 0 | 0 / 0 |
| fs: ui/package-lock.json (npm) | 0 / 0 | 0 / 0 |
| image vm-factory-run14/backend:1.0.0 (own) | 0 / 0 | 0 / 0 |
| image vm-factory-run14/fuseki:6.3.0 (own) | 0 / 0 | 0 / 0 |
| image vm-factory-run14/proxy-ui:1.0.0 (own) | 1 / 0 | 0 / 0 |
| image quay.io/keycloak/keycloak:26.8.2 (third party, used unchanged) | 6 / 0 | 6 / 0 |

Fix applied: the proxy image had CVE-2026-86144 (libxml2 2.13.9-r2, fixed in r3, not yet in the Alpine 3.24
repository at build time). libxml2 was only pulled in by nginx dynamic modules the proxy does not use (xslt, njs,
image-filter, geoip); the image now removes those modules and libxml2/libxslt. All own base images are the newest
slim tags pinned by digest (eclipse-temurin 21 JRE Alpine, nginx-unprivileged Alpine, node 24 Alpine and Maven
build stages) and run `apk upgrade` at build.

Q5 (D175): 0 HIGH and 0 CRITICAL in own images and dependencies. Keycloak 26.8.2 is the newest patch release
(quay.io tags on 23 Nov 2026) and has no CRITICAL.

## Findings left open (all in the third-party Keycloak image)

| Id | Package | Installed | Fixed | Reason it stays |
|---|---|---|---|---|
| CVE-2026-102554 | com.google.guava:guava (in /opt/keycloak/lib) | 33.6.0-jre | 33.7.2 | Inside the Keycloak distribution; the image is used unchanged (D175) and 26.8.2 is the newest release. Replacing a jar inside Keycloak is not supported by the vendor. Keycloak is on 127.0.0.1 only in the local stack (D98). |
| CVE-2026-91403 | java-21-openjdk-headless (RHEL 9) | 21.0.13.1.2-1.2.el9 | none | No fixed version published. Concerns distrust of an intermediate CA; the local stack makes no outgoing TLS calls from Keycloak. |
| CVE-2026-119373 | sqlite-libs (RHEL 9) | 3.34.1-11.el9_8 | none | No fixed version. Keycloak in this stack uses its dev-mode H2 database; the FTS5 code of the OS sqlite library is not reachable through Keycloak. |
| CVE-2026-119374 | sqlite-libs | 3.34.1-11.el9_8 | none | Same as above (FTS5 index), not reachable. |
| CVE-2026-119379 | sqlite-libs | 3.34.1-11.el9_8 | none | Same as above (session changesets), not reachable. |
| CVE-2026-119586 | sqlite-libs | 3.34.1-11.el9_8 | none | Same as above (sqlite3 shell `.recover`), the shell is not used. |

SBOMs: `sbom-backend.cdx.json` (CycloneDX 1.6, Maven plugin, 98 components) and `sbom-ui.cdx.json` (npm sbom,
CycloneDX, runtime dependencies).
