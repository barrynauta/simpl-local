# Security scan (finish line 4)

Scanner: Trivy 0.76.2 (`/opt/homebrew/bin/trivy`), vulnerability database of 19 Nov 2026. Commands:

```
trivy fs --severity HIGH,CRITICAL --scanners vuln --skip-dirs backups work/
trivy image --severity HIGH,CRITICAL --scanners vuln <image>   # for every line of ./stack.sh images
```

Full JSON of the one image with open findings: `reports/trivy-keycloak.json`.

## Counts per target

| Target | Before fixes (C / H) | After fixes (C / H) | What changed |
|---|---|---|---|
| `backend/pom.xml` (Maven, runtime scope) | 0 / 0 | 0 / 0 | Spring Boot 4.1.2, Jena 6.2.0, Nimbus 10.10 were already current |
| `ui/package-lock.json` (npm, production) | 0 / 0 | 0 / 0 | none |
| `vm-factory-run8/backend:1.0.0` (eclipse-temurin 21 JRE Alpine, `apk upgrade`) | 0 / 0 | 0 / 0 | none |
| `vm-factory-run8/fuseki:6.2.0` (same base, Fuseki 6.2.0 jar, checksum-verified) | 0 / 0 | 0 / 0 | none |
| `vm-factory-run8/ui:1.0.0` (nginx unprivileged Alpine) | 0 / 0 | 0 / 0 | none |
| `curlimages/curl:8.23.0` (helper of the backup scripts) | 0 / 32 | removed | 32 HIGH in krb5 / krb5-libs 1.22.2-r1, fixed in 1.22.3-r0. 8.23.0 is the newest curlimages tag and still ships the old krb5, so it was replaced by an own image |
| `vm-factory-run8/ops-curl:1.0.0` (new: `ops/Dockerfile`, alpine 3.24.4 pinned by digest, `apk upgrade`, curl) | n/a | 0 / 0 | replaces curlimages/curl; backup and restore round trip passes with it |
| `quay.io/keycloak/keycloak:26.8.2` | 1 / 6 | 1 / 6 | none possible, see below |

Own images and own dependencies: 0 CRITICAL, 0 HIGH (Q5 met).

## Findings still open (all in the Keycloak product image)

Keycloak 26.8.2 (digest `sha256:7e66a96e...`, tag 26.8.2-3, published 19 Nov 2026) is the newest release on quay.io. It is the identity provider product of the reuse catalogue and E1; the factory runs it as a local stand-in. Rebuilding the vendor image (swapping JDK or jars inside the Keycloak distribution) would produce an unsupported Keycloak, so the findings stay until Keycloak publishes a fixed image.

| Id | Package | Installed | Fixed | Severity | Reason it stays |
|---|---|---|---|---|---|
| CVE-2026-114938 | java-21-openjdk-headless (UBI 9) | 1:21.0.13.1.2-1.2.el9 | no fixed version | CRITICAL | no fixed package exists in the base distribution; newest Keycloak image |
| CVE-2026-91403 | java-21-openjdk-headless (UBI 9) | 1:21.0.13.1.2-1.2.el9 | no fixed version | HIGH | no fixed package exists; newest Keycloak image |
| CVE-2026-119373 | sqlite-libs (UBI 9) | 3.34.1-11.el9_8 | no fixed version | HIGH | no fixed package; Keycloak in this stack does not use SQLite (dev-file H2 store) |
| CVE-2026-119374 | sqlite-libs (UBI 9) | 3.34.1-11.el9_8 | no fixed version | HIGH | as above |
| CVE-2026-119379 | sqlite-libs (UBI 9) | 3.34.1-11.el9_8 | no fixed version | HIGH | as above |
| CVE-2026-119586 | sqlite-libs (UBI 9) | 3.34.1-11.el9_8 | no fixed version | HIGH | as above |
| CVE-2026-102554 | com.google.guava:guava (inside the Keycloak distribution) | 33.6.0-jre | 33.7.2 | HIGH | the fix needs a Keycloak release that bundles guava 33.7.2; replacing a jar inside the vendor image is not supported. Keycloak runs only on the internal `front` network plus the published test port 8193 of this local stack |

Not reachable from the internet: the stack binds to localhost ports of a developer machine. In production the Simpl-Open Tier 1 Keycloak (with its own patch process) replaces this container.
