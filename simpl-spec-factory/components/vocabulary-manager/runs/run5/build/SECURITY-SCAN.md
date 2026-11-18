# Security scan

Tool: Trivy 0.76.2 (`/opt/homebrew/bin/trivy`), vulnerability DB version 2 updated 2026-11-17 13:00 UTC, Java DB
updated 2026-11-17 00:59 UTC. Scans of 18 Nov 2026.

Commands:

```bash
trivy fs    --severity HIGH,CRITICAL work/
trivy image --severity HIGH,CRITICAL vm-factory-run5/backend:1.0.0
trivy image --severity HIGH,CRITICAL vm-factory-run5/ui:1.0.0
trivy image --severity HIGH,CRITICAL vm-factory-run5/fuseki:6.2.0
trivy image --severity HIGH,CRITICAL quay.io/keycloak/keycloak:26.8.2@sha256:c68e286c…
```

Raw results: `reports/trivy/*-boot4.json` (current build, Spring Boot 4.1.2) and `reports/trivy/boot3/` (the earlier
Spring Boot 3.5.16 build: `*-before.json` without overrides, `*-after.json` with the Tomcat and Jackson overrides).

## Counts (HIGH and CRITICAL)

| Target | Boot 3.5.16, no overrides | Boot 3.5.16, Tomcat and Jackson overrides (before this upgrade) | Boot 4.1.2 (after this upgrade) |
|---|---|---|---|
| `trivy fs work/` (backend/pom.xml, ui/package-lock.json) | 7 CRITICAL, 6 HIGH | 2 CRITICAL, 0 HIGH | 0 CRITICAL, 0 HIGH |
| image `vm-factory-run5/backend:1.0.0` | 7 CRITICAL, 6 HIGH | 2 CRITICAL, 0 HIGH | 0 CRITICAL, 0 HIGH |
| image `vm-factory-run5/ui:1.0.0` | 0, 0 | 0, 0 | 0, 0 |
| image `vm-factory-run5/fuseki:6.2.0` | 0, 0 | 0, 0 | 0, 0 |
| image `quay.io/keycloak/keycloak:26.8.2` (third party, used as is) | 0, 6 HIGH | 0, 6 HIGH | 0, 6 HIGH |

## Fixes made

| Finding | Package | Fix |
|---|---|---|
| CVE-2026-47884, CVE-2026-47890 (CRITICAL) | spring-webmvc 6.2.19 | Upgrade to Spring Boot 4.1.2, which manages Spring Framework 7.0.10 (fixed in 7.0.9). |
| CVE-2026-55276, CVE-2026-65182, CVE-2026-65637, CVE-2026-65905, CVE-2026-68525 (CRITICAL), CVE-2026-53404 (HIGH) | tomcat-embed-core 10.1.55 | Spring Boot 4.1.2 manages Tomcat 11.0.27 (fixed from 11.0.25). The Boot 3 override `tomcat.version` is removed. |
| CVE-2026-89407, CVE-2026-89425, CVE-2026-68497, CVE-2026-91776, CVE-2026-91777 (HIGH) | jackson-core and jackson-databind 2.21.4 | The service now uses Jackson 3.1.7 (`tools.jackson`), managed by Spring Boot 4.1.2; Jackson 2 databind is no longer on the class path (only `jackson-annotations` 2.21). The override `jackson-bom.version` is removed. |

The remaining version overrides in `backend/pom.xml` (`commons-codec` 1.22.0, `gson` 2.14.0) only align with the
versions Jena 6.2.0 is built against; they are not security fixes.

Other choices that keep the counts low: current base images pinned by digest (`eclipse-temurin:21-jre-alpine`,
`nginxinc/nginx-unprivileged:alpine`, `node:24-alpine` and `maven:3.9-eclipse-temurin-21-alpine` for the build
stages), `apk upgrade` in every runtime image, Fuseki 6.2.0 (July 2026), and Keycloak 26.8.2 (the newest release, 12
Nov 2026).

## Findings still open

All in the third-party Keycloak image, which this build uses unchanged:

| Id | Severity | Package | Why it stays |
|---|---|---|---|
| CVE-2026-91403 | HIGH | java-21-openjdk-headless 21.0.13 (Red Hat UBI 9.9) | Red Hat status `will_not_fix`, no fixed package. It is a CA distrust update (eMudhra intermediates); Keycloak here makes no outgoing TLS calls (local realm, no identity brokering). |
| CVE-2026-119373, CVE-2026-119374, CVE-2026-119379 | HIGH | sqlite-libs 3.34.1 | No fixed package in UBI 9 yet. Keycloak does not use SQLite (dev mode uses H2), and the FTS5 and session-extension paths need an attacker-supplied database file. |
| CVE-2026-119586 | HIGH | sqlite-libs 3.34.1 | No fixed package. Affects the `sqlite3` command-line shell `.recover` command, which is not used. |
| CVE-2026-102554 | HIGH | com.google.guava:guava 33.6.0-jre | Fixed in guava 33.7.2, but the jar is part of the Keycloak distribution and 26.8.2 is the newest Keycloak release; replacing jars inside the vendor image is not supported and could break it. The flaw needs Java deserialisation of attacker data into Guava collections, which Keycloak does not do on the paths this build uses. Upgrade Keycloak when a release with guava 33.7.2 or newer ships. |

No finding in code or images built by this project is open.

## Limits of the scan

- The Fuseki image holds the shaded `jena-fuseki-server-6.2.0.jar`. Trivy found no findings in it; Trivy only sees the
  libraries a shaded jar lists in its Maven metadata, so a library shaded without that metadata would not be reported.
  The Fuseki SBOM from Trivy is in `sbom/fuseki-image-sbom.cdx.json`.
- MEDIUM and LOW findings were not part of the finish line and are not listed.
- The scan is run by hand; there is no CI pipeline in this build (see DEVIATIONS.md, OWASP-12).
