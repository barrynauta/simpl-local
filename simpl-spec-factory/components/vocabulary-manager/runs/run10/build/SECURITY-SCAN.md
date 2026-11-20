# Security scan

Tool: Trivy 0.76.2 (`/opt/homebrew/bin/trivy`), vulnerability DB version 2 updated 2026-11-20 07:09 UTC.
Commands: `trivy fs --severity HIGH,CRITICAL --scanners vuln .` on `work/`, and
`trivy image --severity HIGH,CRITICAL --scanners vuln <image>` on every image printed by `./stack.sh images`.
Raw JSON results are in `security/` (`*-before.json`, `*-after.json`).

## Counts (HIGH / CRITICAL)

| Target | Before fixes | After fixes | Fix applied |
|---|---|---|---|
| `backend/pom.xml` (fs) | 0 / 0 | 0 / 0 | Current versions chosen from the start (Spring Boot 4.1.3, Jena 6.2.0, Nimbus 10.10) |
| `ui/package-lock.json` (fs) | 0 / 0 | 0 / 0 | none needed |
| `vm-factory-run10/backend:1.0.0` (eclipse-temurin 21 JRE Alpine, pinned digest, `apk upgrade`) | 0 / 0 | 0 / 0 | none needed |
| `vm-factory-run10/fuseki:1.0.0` (same base, Fuseki jar SHA-256 checked) | 0 / 0 | 0 / 0 | none needed |
| `vm-factory-run10/ui-proxy:1.0.0` (nginx-unprivileged Alpine, pinned digest, `apk upgrade`) | 0 / 0 | 0 / 0 | none needed |
| `curlimages/curl:8.19.0` (first choice for backup and restore) | 46 / 1 | removed | Replaced by the own `vm-factory-run10/store-tools:1.0.0` (Alpine 3.24.4, pinned digest, `apk upgrade`, curl from Alpine) |
| `vm-factory-run10/store-tools:1.0.0` | n/a | 0 / 0 | new image |
| `quay.io/keycloak/keycloak:26.8.2` (pinned digest) | 6 / 0 | 6 / 0 | No fix possible in this build: 26.8.2 is the newest Keycloak release and its newest image build (tags 26.8, 26.8.2 and 26.8.2-3 have the same digest) |

Build-stage images (`maven:3.9-eclipse-temurin-21-alpine`, `node:24-alpine`) are only used to build and are not part
of the running stack; they were not scanned.

## Findings left open

All in the third-party Keycloak image, which the stack runs as the product named by the reuse catalogue
(`identity-provider`). In production the platform operates the Tier 1 Keycloak; this local copy runs in dev mode on
127.0.0.1 only (D98).

| Id | Package | Installed | Fixed version | Why it stays |
|---|---|---|---|---|
| CVE-2026-91403 | java-21-openjdk-headless (RHEL 9 rpm) | 21.0.13.1.2-1.2.el9 | none | No fixed package published by the distribution; the image is the newest Keycloak build |
| CVE-2026-119373 | sqlite-libs (RHEL 9 rpm) | 3.34.1-11.el9_8 | none | No fixed package; Keycloak does not use SQLite (dev mode uses its embedded H2 database), so the library is not reachable from Keycloak |
| CVE-2026-119374 | sqlite-libs | 3.34.1-11.el9_8 | none | as above |
| CVE-2026-119379 | sqlite-libs | 3.34.1-11.el9_8 | none | as above |
| CVE-2026-119586 | sqlite-libs | 3.34.1-11.el9_8 | none | as above |
| CVE-2026-102554 | com.google.guava:guava (in `/opt/keycloak/lib/lib/main`) | 33.6.0-jre | 33.7.2 | The jar is part of the Keycloak distribution; replacing a library inside the vendor's product image would make an unsupported Keycloak build. Fixed when Keycloak ships a release with Guava 33.7.2 or later; the image digest is pinned so the upgrade is a deliberate change |

## Other checks

- Fuzzing with Schemathesis 4.33.4 (own run, `--max-examples 30 --seed 1`): published contract, 1427 cases with the
  server-error check: 0 failures; all checks: 14 findings, all decided contract changes (D19 relaxed name filter;
  D37, D38, D52, D91 stricter rules). Decided contract (`docs/openapi-decided.yaml`): 1720 cases, 4 findings, all
  decided rejections of invalid content (Turtle bytes, an out-of-range date offset). Reports in `security/fuzz/`.
- SBOMs: `sbom/sbom-backend.json` (CycloneDX 1.6, Maven), `sbom/sbom-ui.json` (CycloneDX, npm, runtime
  dependencies).
