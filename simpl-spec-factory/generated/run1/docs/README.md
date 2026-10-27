# Vocabulary Manager (spec factory run 1)

A Simpl-Open Vocabulary Manager built only from the frozen input specs (`../../input/specs/`) and the spec digest
(`../../input/spec-digest.md`). The Governance Authority uses it to manage internal vocabularies (Turtle files):
list, filter and sort, view details and version history, download, create, create new versions, change status,
and get a validation report. It also manages cached external vocabularies (API only).

## What is in `build/`

| Path | Content |
|---|---|
| `backend/` | Java 21, Spring Boot 3.5, Maven. REST API under `/v1`, JWT resource server, Apache Jena for parsing, validation and Fuseki access |
| `ui/` | Vue 3, Vite, TypeScript, Pico CSS. Served by nginx, which also proxies `/v1` to the backend |
| `keycloak/simpl-realm.json` | Realm `simpl`, imported by Keycloak at start-up (users, roles, clients) |
| `docker-compose.yml` | Fuseki, Keycloak, backend, UI |
| `smoke.sh` | curl smoke check against the running stack |
| `docs/` | This README, architecture, API notes, decisions D1 to D30, build copy of the OpenAPI (`openapi.yaml`) |
| `TRACEABILITY.md` | Stories and decisions mapped to code and tests |
| `BUILD-LOG.md` | Honest log: gaps, choices, problems, what does not work |

## Run it

Requirements: Docker with Compose v2. Nothing else (the Maven and npm builds run inside Docker).

```bash
cd build
docker compose up -d --build --wait
./smoke.sh
```

`--wait` returns when every container is healthy. A first build takes a few minutes (Maven and npm downloads).

| Service | URL |
|---|---|
| API | http://localhost:8096/v1 (health: http://localhost:8096/v1/health) |
| UI | http://localhost:4333 |
| Keycloak | http://localhost:8183 (admin console user `admin` / `admin`) |
| Token endpoint | http://localhost:8183/realms/simpl/protocol/openid-connect/token |
| Fuseki | not exposed to the host |

Stop with `docker compose down`. Add `-v` to also delete the Fuseki data volume.

## Users and tokens (D6)

| User | Password | Role |
|---|---|---|
| `ga-admin` | `password` | `GA_VOCABULARY_ADMIN` (read and write) |
| `ga-viewer` | `password` | `GA_VOCABULARY_VIEWER` (read) |
| `ga-norole` | `password` | none |

Test harness token (password grant, public client `vocabulary-tests`):

```bash
curl -s -X POST http://localhost:8183/realms/simpl/protocol/openid-connect/token \
  -d grant_type=password -d client_id=vocabulary-tests -d username=ga-admin -d password=password
```

Roles are in a top-level `roles` array claim of the access token. The UI uses the public client `vocabulary-ui`
(authorization code with PKCE S256, redirect `http://localhost:4333/*`).

### Token issuer: host and network give the same `iss`

Keycloak runs with `KC_HOSTNAME=http://localhost:8183` and `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true`. The issuer is
therefore always `http://localhost:8183/realms/simpl`, whether a token is fetched from the host
(`localhost:8183`) or from inside the compose network (`keycloak:8080`). The backend fetches the signing keys over
the network (`APP_JWK_SET_URI=http://keycloak:8080/...`) and accepts the issuers listed in `APP_ISSUERS`
(comma separated, default `http://localhost:8183/realms/simpl`). This was checked: a token fetched with
`docker compose exec backend curl http://keycloak:8080/...` carries the localhost issuer and is accepted.

## Example calls

```bash
TOKEN=$(curl -s -X POST http://localhost:8183/realms/simpl/protocol/openid-connect/token \
  -d grant_type=password -d client_id=vocabulary-tests -d username=ga-admin -d password=password | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" -X POST http://localhost:8096/v1/vocabularies \
  -F name=HealthcareVocabulary -F description="Healthcare terms" -F changelog="Initial version" \
  -F "vocabularyFile=@health.ttl;type=text/turtle"

curl -H "Authorization: Bearer $TOKEN" "http://localhost:8096/v1/vocabularies?name=Health&sort=-creationDate"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8096/v1/content/HealthcareVocabulary
```

## Configuration (backend environment variables)

| Variable | Default | Meaning |
|---|---|---|
| `APP_FUSEKI_URL` | `http://localhost:3030/vocabulary` | Fuseki dataset URL; `mem` uses an in-memory dataset |
| `APP_JWK_SET_URI` | `http://localhost:8183/realms/simpl/protocol/openid-connect/certs` | JWKS of the issuer |
| `APP_ISSUERS` | `http://localhost:8183/realms/simpl` | Accepted `iss` values, comma separated |
| `APP_PUBLIC_BASE_URL` | `http://localhost:8096/v1` | Base for absolute `contentHref` values |
| `APP_REFERENCE_BASE` | `https://vocabulary.simpl.local/vocabularies` | D18 namespace base for internal references |
| `APP_MAX_UPLOAD_BYTES` | `10485760` | D30 upload limit (10 MB) |

The UI reads `/config.json` at start-up (authority, client id, API base, reference base).

## Developer workflow

```bash
cd build/backend && mvn verify        # unit tests (surefire) and API integration tests (failsafe, *IT)
cd build/ui && npm install && npm run dev   # UI on :4333, proxies /v1 to localhost:8096
```
