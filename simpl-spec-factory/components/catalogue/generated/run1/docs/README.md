# Simpl-Open Catalogue: spec factory build

This folder holds a build of the Simpl-Open Catalogue made only from the frozen input specifications
(`../input/specs/`) and the shared digest (`../input/spec-digest.md`). It contains:

| Part | Folder | Stack |
|---|---|---|
| catalogue-be (`cat`) | `backend/catalogue-be` | Java 21, Spring Boot 3.5, JDBC on PostgreSQL, Apache Jena SHACL, Titanium JSON-LD, Bouncy Castle |
| query mapper adapter (`qma`) | `backend/query-mapper-adapter` | Java 21, Spring Boot 3.5 |
| catalogue client backend (`cbe`) | `backend/catalogue-client-be` | Java 21, Spring Boot 3.5 |
| shared code | `backend/common` | Problem handling, JWT validation, paging, policy rule, advanced query |
| search UI (`ui`) | `ui` | Vue 3, Vite, TypeScript, keycloak-js, n3 (Turtle parser), nginx |
| Schema Manager stand-in | `stubs/schema-stub` | Node.js, no dependencies |
| Quality Scoring stand-in | `stubs/quality-stub` | Node.js, no dependencies |
| Environment | `docker-compose.yml`, `env/` | Keycloak 26.2, PostgreSQL 16 |

## Run

```bash
cd build
docker compose -p simpl-catalogue-factory up -d --build --wait
./smoke.sh
```

`--wait` returns when every service reports healthy on `/health` (Keycloak on its readiness endpoint).
The first build downloads Maven and npm dependencies inside Docker and takes a few minutes.

Host ports (digest section 5.1):

| Service | URL |
|---|---|
| Keycloak | http://localhost:18080 (admin console user `admin`, password `admin`) |
| cat-a | http://localhost:18101 |
| qma-a | http://localhost:18102/v1 |
| cbe-a | http://localhost:18103/v1 and /v2 |
| ui-a | http://localhost:18104 (sign in as `consumer-a` / `Passw0rd!`) |
| cat-b | http://localhost:18201 |
| qma-b | http://localhost:18202/v1 |
| qma-c | http://localhost:18302/v1 |
| schema-stub | http://localhost:18401 |
| quality-stub | http://localhost:18402 |

The database (`catalogue-db`, one PostgreSQL with databases `cat_a` and `cat_b`) has no host port.

Stop with `docker compose -p simpl-catalogue-factory down` (add `-v` to drop the data).

## Tokens

Users and clients are imported at Keycloak start-up from `env/keycloak/*.json` (digest 5.2, 5.3).
Get a user token by password grant:

```bash
curl -s -d grant_type=password -d client_id=test-client -d username=consumer-a -d 'password=Passw0rd!' \
  http://localhost:18080/realms/dataspace-a/protocol/openid-connect/token | jq -r .access_token
```

### How the token issuer works for host and network

Keycloak runs with `--hostname=http://localhost:18080` and `--hostname-backchannel-dynamic=true`.
Every token therefore carries `iss = http://localhost:18080/realms/<realm>`, also when a container asks
for it at `http://keycloak:8080` (as qma-a does for its federation token). The backends validate `iss`
against the public URL and fetch the signing keys from the internal URL
`http://keycloak:8080/realms/<realm>/protocol/openid-connect/certs`. No container ever needs to reach
`localhost:18080`, and the browser never needs to reach `keycloak:8080`.

## Signing test resource descriptions

`tools/sign-rd.mjs` signs an RD with the fixed keys of digest D22 and D23 (Node.js, no dependencies):

```bash
node tools/sign-rd.mjs --did provider-a          # the did:jwk to put in "issuer"
node tools/sign-rd.mjs provider-a < rd.json > signed.json
```

## Own tests

```bash
cd build
mvn verify
```

37 JUnit tests: unit tests of the domain logic (signature, JCS, SHACL, quality thresholds, policy rule,
advanced query, paging, attribute mapping) and Spring Boot integration tests per backend with an H2
database, mocked JWTs and MockWebServer for every downstream service.

## Further documents

- [architecture.md](architecture.md): component map and runtime flows
- [api-notes.md](api-notes.md): the wire contract as built, per service
- [decisions.md](decisions.md): how each digest decision is implemented
- [../TRACEABILITY.md](../TRACEABILITY.md): stories, BP steps and decisions to code and tests
- [../BUILD-LOG.md](../BUILD-LOG.md): guesses, problems, open points
