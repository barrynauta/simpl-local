# Security scan (finish line 4)

Scanner: Trivy 0.76.2 (`/opt/homebrew/bin/trivy`), vulnerability DB updated 2026-11-21 07:02 UTC. Severity HIGH and CRITICAL. Commands:

```bash
trivy fs --scanners vuln --severity HIGH,CRITICAL --skip-dirs ui/node_modules --skip-dirs backups .
trivy image --scanners vuln --severity HIGH,CRITICAL <image>     # every image of ./stack.sh images
```

JSON reports: `reports/trivy/`. SBOMs (CycloneDX): `sbom/backend.cdx.json` (Maven plugin), `sbom/ui.cdx.json` (Trivy from package-lock).

## Counts per target (HIGH / CRITICAL)

| Target | Before fixes | After fixes | Fix applied |
|---|---|---|---|
| `work/` file system (`backend/pom.xml`, `ui/package-lock.json`) | 0 / 0 | 0 / 0 | none needed (Spring Boot 4.1.3, Jena 6.2.0, Nimbus 10.10, Resilience4j 2.4.0, current npm packages) |
| `vm-factory-run13-backend:1.0.0` (Temurin 21 JRE, Alpine 3.24) | 0 / 0 | 0 / 0 | `apk upgrade` in the image from the first build; base pinned by digest |
| `vm-factory-run13-fuseki:6.2.0` (Temurin 21 JRE, Alpine 3.24) | 0 / 0 | 0 / 0 | same; Fuseki jar verified by SHA-512 |
| `vm-factory-run13-ui:1.0.0` (nginx-unprivileged 1.29, Alpine 3.23) | 56 / 1 | 0 / 0 | `apk upgrade` in the final stage (measured by the UI build step on the probe image) |
| `quay.io/keycloak/keycloak` | 26.7.5: 18 / 2 | 26.8.2: 6 / 0 | upgraded to Keycloak 26.8.2 (released 19 Nov 2026), pinned by digest |

## Findings left open

All in the Keycloak 26.8.2 image (`quay.io/keycloak/keycloak:26.8.2@sha256:7e66a96e4739239070aeaf72e4c5decf0f7f682cfc725b9f7ea2b336eaba38b3`), a reused product (E1). The image has no package manager (UBI micro), and patching it would mean forking Keycloak's image (REUSE-09).

| Id | Package | Installed | Fixed | Severity | Reason it stays |
|---|---|---|---|---|---|
| CVE-2026-91403 | java-21-openjdk-headless | 21.0.13.1.2-1.2.el9 | none | HIGH | No fixed version published by Red Hat. It is a CA distrust change (eMudhra intermediates); Keycloak in this stack makes no outgoing TLS calls. |
| CVE-2026-119373 | sqlite-libs | 3.34.1-11.el9_8 | none | HIGH | No fixed version. Keycloak does not use SQLite (dev mode uses the embedded H2 database); FTS5 not reachable. |
| CVE-2026-119374 | sqlite-libs | 3.34.1-11.el9_8 | none | HIGH | No fixed version; not reachable, as above. |
| CVE-2026-119379 | sqlite-libs | 3.34.1-11.el9_8 | none | HIGH | No fixed version; session changesets not used. |
| CVE-2026-119586 | sqlite-libs | 3.34.1-11.el9_8 | none | HIGH | No fixed version; the sqlite3 shell `.recover` command is not used. |
| CVE-2026-102554 | com.google.guava:guava (in `/opt/keycloak/lib`) | 33.6.0-jre | 33.7.2 | HIGH | Fixed version exists, but only Keycloak can ship it; replacing a jar inside the vendor image is patching the product. Newest Keycloak (26.8.2) still bundles 33.6.0. Keycloak is published on 127.0.0.1 only (D98). |

No finding is open in the images and dependencies the build owns (Q5).
