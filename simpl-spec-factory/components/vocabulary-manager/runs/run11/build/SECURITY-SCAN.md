# Security scan

Tool: **Trivy 0.76.2** (`/opt/homebrew/bin/trivy`), vulnerability scanner, severities HIGH and
CRITICAL, scanned on 20 Nov 2026. Raw JSON reports are in `scans/` (`before-*` and `after-*`).

Commands:

```
trivy fs --scanners vuln --severity HIGH,CRITICAL .            # in work/: backend/pom.xml and ui/package-lock.json
trivy image --scanners vuln --severity HIGH,CRITICAL <image>    # for every image of ./stack.sh images
```

## Counts per target

| Target | Before fixes (HIGH / CRITICAL) | After fixes (HIGH / CRITICAL) |
|---|---|---|
| `trivy fs` on work/ (Maven and npm dependencies) | 0 / 0 (see note) | 0 / 0 |
| vm-factory-run11/backend:local (eclipse-temurin 21 JRE Alpine + application jar) | 0 / 0 | 0 / 0 |
| vm-factory-run11/fuseki:local (eclipse-temurin 21 JRE Alpine + Fuseki 6.2.0 jar) | 0 / 0 | 0 / 0 |
| vm-factory-run11/ui:local (nginx-unprivileged alpine-slim + UI files) | 3 / 0 | 0 / 0 |
| quay.io/keycloak/keycloak:26.8.2 (vendor image, pinned by digest) | 6 / 0 | 6 / 0 |

Note on the file system scan: the first `trivy fs` attempt was started with two path arguments and
did not run; the scan was run once on `work/` after the image fixes. No dependency changed between
the two points, so the before count equals the after count.

## Fixes

| Finding | Package | Fix |
|---|---|---|
| CVE-2026-107891 | musl, musl-utils 1.2.6-r2 (UI image) | `apk upgrade musl musl-utils zlib` in `ui/Dockerfile`, now 1.2.6-r3 |
| CVE-2026-85091 | zlib 1.3.2-r0 (UI image) | same, now 1.3.2-r1 |

Base images were chosen slim and current (Alpine-based Temurin 21 JRE, nginx-unprivileged
alpine-slim) and pinned by digest; library versions are pinned in `pom.xml` and `package-lock.json`.

## Findings still open

All in the vendor Keycloak image `quay.io/keycloak/keycloak:26.8.2@sha256:7e66a96e...` (the latest
Keycloak release on 20 Nov 2026; the build uses the product as published, E1):

| Id | Package | Installed | Fixed | Reason it stays |
|---|---|---|---|---|
| CVE-2026-91403 | java-21-openjdk-headless (UBI 9) | 1:21.0.13.1.2-1.2.el9 | none | No fixed version exists in the Red Hat repositories yet. |
| CVE-2026-119373 | sqlite-libs (UBI 9) | 3.34.1-11.el9_8 | none | No fixed version exists. Keycloak in this stack uses its dev-mode H2 database, not SQLite. |
| CVE-2026-119374 | sqlite-libs | 3.34.1-11.el9_8 | none | as above |
| CVE-2026-119379 | sqlite-libs | 3.34.1-11.el9_8 | none | as above |
| CVE-2026-119586 | sqlite-libs | 3.34.1-11.el9_8 | none | as above |
| CVE-2026-102554 | com.google.guava:guava (inside the Keycloak distribution) | 33.6.0-jre | 33.7.2 | The fix needs a Keycloak release that ships guava 33.7.2; replacing a jar inside the vendor image would make an untested Keycloak build. The local Keycloak listens on 127.0.0.1 only (D98). To be closed by upgrading Keycloak when a release with the fix exists. |

No HIGH or CRITICAL finding is open in the images and dependencies this build owns.
