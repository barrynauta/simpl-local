# Schema Manager (spec-factory build)

This folder holds a Simpl-Open Schema Manager built only from the frozen specifications in
`../../input/specs/` and the decisions D1 to D63 in `../../input/spec-digest.md`.

It has two parts:

- **Backend**: a REST API for schema concepts and their versions (Java 21, Spring Boot 3.5, Maven,
  PostgreSQL). It validates SHACL in Turtle, enforces the one-published-per-slot rule, sends
  Provider e-mail and delivers webhooks.
- **UI**: the schema pages of the Governance Authority portal (Vue 3, Vite, TypeScript), served by
  nginx. nginx also proxies the API.

## Run

Requirements: Docker with Compose v2. Ports 8098, 4334, 8185, 8099 and 8027 must be free.

```bash
cd build
docker compose up -d --build --wait     # about 3 minutes on the first build
./smoke.sh                              # curl smoke test, needs jq
```

| What | URL | Config name |
|---|---|---|
| API | http://localhost:8098/v1 | `SM_BASE_URL` |
| UI | http://localhost:4334 | `SM_UI_URL` |
| Keycloak (realm `simpl`) | http://localhost:8185 | `KEYCLOAK_URL` |
| Vocabulary Manager stub | http://localhost:8099 | `VOCABULARY_MANAGER_URL` (for tests) |
| Mailpit HTTP API and web UI | http://localhost:8027 | `MAIL_API_URL` |

Users (password `password`): `ga-admin` (GA_SCHEMA_ADMIN), `ga-viewer` (GA_SCHEMA_VIEWER),
`ga-norole` (no role). API tests get tokens with the password grant on the public client
`schema-tests`. The UI signs in with the public client `schema-ui` (authorization code with PKCE).

```bash
TOKEN=$(curl -s -X POST http://localhost:8185/realms/simpl/protocol/openid-connect/token \
  -d grant_type=password -d client_id=schema-tests -d username=ga-admin -d password=password | jq -r .access_token)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8098/v1/schemas
```

To start from an empty database: `docker compose down -v` and then `up` again.

## Tests

- `cd backend && mvn verify`: unit tests (validation, parameter and media type rules) and
  integration tests (`*IT`) against PostgreSQL in Testcontainers, with mocked JWTs, a mocked
  Vocabulary Manager client and a mocked mail sender. A local HTTP server receives webhooks.
  Needs Docker.
- `./smoke.sh`: the finish-line smoke test against the running stack.
- `cd ui-check && npm install && npx playwright install chromium && node check.mjs`: an own
  browser check of the UI against the running stack. This is not the black-box suite.

## Layout

| Path | Content |
|---|---|
| `docker-compose.yml` | The whole environment (project name `schema-factory`) |
| `backend/` | Spring Boot service, Flyway migration, tests, Dockerfile |
| `ui/` | Vue application, nginx config, Dockerfile |
| `keycloak/realm-simpl.json` | Realm imported at start-up (D59) |
| `vocabulary-stub/` | Vocabulary Manager stub (D60), Node.js, in memory |
| `ui-check/` | Own Playwright check of the UI |
| `smoke.sh` | Finish-line smoke test |
| `docs/` | This README, architecture, API notes, decision notes |
| `TRACEABILITY.md` | Stories and decisions mapped to code and tests |
| `BUILD-LOG.md` | Honest log of guesses, problems and open points |
