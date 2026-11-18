# Vocabulary Manager (factory run 5)

The Vocabulary Manager lets a Governance Authority user create vocabularies (Turtle files), create new versions, change the
vocabulary status and the version status, browse the version history, download content and search vocabularies. It also
keeps registered copies of external vocabularies, so references to them can be resolved during validation. The
component is built from the stories, the OpenAPI file and the decisions in `../../input/`.

## What is in this folder

| Path | What |
|---|---|
| `backend/` | Java 21, Spring Boot 4.1.2 (Spring Framework 7.0.10, Tomcat 11, Jackson 3), Maven. The API under `/v1`. Own tests in `src/test`. |
| `ui/` | Vue 3, Vite, TypeScript. Served by nginx, which also proxies `/v1` to the backend. |
| `fuseki/` | Dockerfile of Apache Jena Fuseki 6.2.0 with the dataset `vocabulary` (store, decision E3). |
| `keycloak/simpl-realm.json` | Realm `simpl`, clients and test users of decision E1, imported at start. |
| `compose.yaml`, `.env` | The stack. Ports and URLs from the environment (decision E4). |
| `smoke.sh` | Finish-line smoke test (curl). |
| `ui/e2e/ui_smoke.py` | Optional own UI check with Playwright for Python. |
| `reports/jacoco/` | JaCoCo coverage report of the backend tests. |
| `reports/trivy/` | Trivy results before and after the fixes. |
| `sbom/` | CycloneDX SBOMs: backend (Maven), UI (npm), Fuseki image (Trivy). |
| `docs/` | This file, `ARCHITECTURE.md`, `API.md`. |
| `TRACEABILITY.md`, `DEVIATIONS.md`, `BUILD-LOG.md`, `SECURITY-SCAN.md` | Reports of this build. |

## Run

Requirements: Docker with Compose v2. Nothing else; all builds run in containers.

```bash
cd work
docker compose -p vm-factory-run5 up -d --build --wait
./smoke.sh
```

`--wait` returns when every container is healthy. The first build downloads Maven and npm packages and takes a few
minutes.

| Service | URL (host) |
|---|---|
| API | http://localhost:8196/v1 (health: `/v1/health`) |
| UI | http://localhost:4433 |
| Keycloak | http://localhost:8193 (token endpoint `/realms/simpl/protocol/openid-connect/token`) |
| Fuseki | not exposed on the host; reachable as `fuseki:3030` inside the network |

Test users (password `Password1!`): `ga-admin` (GA_VOCABULARY_ADMIN), `ga-viewer` (GA_VOCABULARY_VIEWER), `ga-norole`
(no role). The public client `vocabulary-tests` allows the password grant:

```bash
curl -s -X POST http://localhost:8193/realms/simpl/protocol/openid-connect/token \
  -d grant_type=password -d client_id=vocabulary-tests -d username=ga-admin -d 'password=Password1!'
```

Stop with `docker compose -p vm-factory-run5 down` (add `-v` to delete the stored vocabularies).

### Configuration

| Variable | Default | Used by |
|---|---|---|
| `API_PORT`, `UI_PORT`, `KEYCLOAK_PORT` | 8196, 4433, 8193 | compose |
| `KEYCLOAK_ADMIN_USER`, `KEYCLOAK_ADMIN_PASSWORD` | `admin`, value in `.env` | Keycloak bootstrap admin |
| `VOCABULARY_CONTENT_BASE_URL` | `https://authority.example/vocabulary-manager` | backend: contentHref and internal references (E2, D12) |
| `STORE_URL` | `http://fuseki:3030/vocabulary` | backend |
| `OIDC_ISSUER` | `http://localhost:8193/realms/simpl` | backend: accepted token issuer |
| `OIDC_JWKS_URL` | `http://keycloak:8080/realms/simpl/protocol/openid-connect/certs` | backend: realm keys |
| `OIDC_AUDIENCE` | `vocabulary-manager-be` | backend: required token audience |
| `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID` | localhost:8193, simpl, vocabulary-manager-ui | UI runtime config (`/config.json`) |

Tokens must be requested through `http://localhost:${KEYCLOAK_PORT}`: Keycloak runs with a fixed hostname, so the
issuer in every token is that URL, whichever way the token was requested.

## Own tests

```bash
cd backend && mvn verify      # unit and integration tests, JaCoCo report, Spotless check, SBOM
cd ui && npm ci && npm test   # unit tests of the UI helpers (Vitest)
cd ui && npm run format:check # Prettier
```

The backend integration tests start an embedded Fuseki (in memory) and a JWKS endpoint with a test key; they need no
Docker. The JaCoCo report is in `backend/target/site/jacoco/` (copied to `reports/jacoco/`). The build fails under 75 %
line coverage.
