# Vocabulary Manager (factory run 4)

The Vocabulary Manager stores the vocabularies of the Governance Authority: internal vocabularies with numbered,
immutable versions, and registered copies of external vocabularies. It validates uploaded Turtle (syntax, then
semantic rules S1 to S7), publishes valid uploads as ACTIVE, deprecates older versions, and serves metadata, search
and content downloads to the UI and to other components such as Schema Manager.

Built from `../input/` only: stories, OpenAPI, BP02B, the decisions D1 to D46 and E1 to E3, and the build profile.

## Run

```bash
cd work
docker compose -p vm-factory-run4 up -d --build --wait
./smoke.sh
```

The command builds four images, starts them and returns when every container is healthy.

| Service | URL on the host | Notes |
|---|---|---|
| API | http://localhost:8196/v1 | for example `GET /v1/vocabularies`; `GET /v1/health` needs no token |
| UI | http://localhost:4433 | Vue 3 app served by nginx; nginx also proxies `/v1` to the backend |
| Keycloak | http://localhost:8193 | realm `simpl`; token endpoint `/realms/simpl/protocol/openid-connect/token` |
| Fuseki | not exposed | dataset `vocabulary`, internal only; add `ports: ["127.0.0.1:3141:3030"]` to the `fuseki` service to debug |

Users (password `Password1!`): `ga-admin` (GA_VOCABULARY_ADMIN), `ga-viewer` (GA_VOCABULARY_VIEWER), `ga-norole`.
Clients: `vocabulary-manager-ui` (public, code flow with PKCE), `vocabulary-tests` (public, password grant, for tests),
`vocabulary-manager-be` (bearer-only audience).

Get a token and call the API:

```bash
TOKEN=$(curl -s -d grant_type=password -d client_id=vocabulary-tests -d username=ga-admin \
  -d 'password=Password1!' http://localhost:8193/realms/simpl/protocol/openid-connect/token \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])')
curl -H "Authorization: Bearer $TOKEN" http://localhost:8196/v1/vocabularies
```

Stop: `docker compose -p vm-factory-run4 down` (add `-v` to delete the store volume).

## Configuration (environment)

| Variable | Service | Default in compose | Meaning |
|---|---|---|---|
| `VOCABULARY_STORE_URL` | backend | `http://fuseki:3030/vocabulary` | Fuseki dataset URL |
| `VOCABULARY_CONTENT_BASE_URL` | backend | `https://authority.example/vocabulary-manager` | contentBase of decision E2 |
| `KEYCLOAK_JWKS_URL` | backend | `http://keycloak:8080/realms/simpl/protocol/openid-connect/certs` | realm keys |
| `KEYCLOAK_ISSUER` | backend | `http://localhost:8193/realms/simpl` | expected `iss` |
| `KEYCLOAK_AUDIENCE` | backend | `vocabulary-manager-be` | expected `aud` |
| `API_UPSTREAM`, `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID` | ui | see compose | nginx proxy target and `/config.json` |
| `KEYCLOAK_ADMIN_USER`, `KEYCLOAK_ADMIN_PASSWORD` | keycloak | from `.env` | Keycloak bootstrap admin |

## Tests

| What | Command | Result at hand-over |
|---|---|---|
| Backend unit and integration tests, JaCoCo, Spotless | `cd backend && mvn verify` | 66 tests green; line coverage 94.4 %, branch 78.9 % |
| UI unit tests (Vitest), type check, Prettier | `cd ui && npm ci && npm test && npm run build && npm run format:check` | 7 tests green |
| Smoke test against the running stack | `./smoke.sh` | 18 checks pass |
| Own UI check against the running stack (Playwright) | `python3 ui/e2e/ui_check.py` | passes |

Reports: `reports/jacoco/index.html` (coverage), `reports/surefire/` (test results). SBOMs: `sbom/backend.cdx.json`,
`sbom/ui.cdx.json` (CycloneDX).

## More

- [architecture.md](architecture.md): components, data model in the store, checks and their order, failure handling.
- [api-notes.md](api-notes.md): how the API differs from the OpenAPI file where the decisions say so.
- `../TRACEABILITY.md`, `../DEVIATIONS.md`, `../BUILD-LOG.md`.
