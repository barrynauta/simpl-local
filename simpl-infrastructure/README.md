# simpl-infrastructure (local)

Local-evaluation Docker stack for the two SIMPL infrastructure-provisioning
components: **`infrastructure-be`** (Spring Boot backend, artifact `script-service`)
and **`infrastructure-fe`** (React SPA), plus the infra they need (Postgres and a
single-node Kafka). Built and verified 2026-07-12.

The backend manages deployment scripts, cloud provisioner templates and cloud
environments, and drives provisioning by exchanging Kafka messages with an external
provisioner (Crossplane/Terraform via ArgoCD, not part of this stack). The frontend
is its management UI. This stack runs both in isolation to prove they are
self-contained. See [`docs/infrastructure-architecture.md`](docs/infrastructure-architecture.md)
for the integrative view.

## Quick start

```bash
cp .env.example .env          # points INFRA_BE_REPO / INFRA_FE_REPO at the checkouts
./start.sh                    # builds the jar + fe image, brings the stack up
# or, once the backend jar exists:  docker compose up -d --build
```

Then:
- Backend status: http://localhost:8080/api/infrastructureProvisioning/v1/status
- Swagger UI: http://localhost:8080/swagger-ui.html
- Frontend SPA: http://localhost:3001

Stop: `./stop.sh` (`./stop.sh --clean` also drops the Postgres volume).

## Components and ports

| Service | Image | Host port | Role |
|---|---|---|---|
| infrastructure-be | built from the repo `Dockerfile` | 8080 | provisioning backend under test |
| infrastructure-fe | built from the repo `Dockerfile` (nginx) | 3001 | React management SPA |
| postgres | postgres:16-alpine | 5433 | backend DB (Flyway owns the schema) |
| kafka | confluentinc/cp-kafka:7.7.1 | 9092 | single-node KRaft, PLAINTEXT |
| bruno-smoke-test | node:20-alpine | (none) | API smoke tests, `--profile tests` only |

## Tests

Two ways to run the API smoke tests, plus the component suite for the SPA:

```bash
# 1. Shell smoke test (REST + a Kafka round-trip on the 'provisioned' topic)
./seed.sh

# 2. Bruno collection in a container (no local install needed)
docker compose --profile tests up bruno-smoke-test

# 3. Bruno collection from the host (needs Node)
cd bruno && npx @usebruno/cli run --env local -r

# 4. Frontend component tests (in the fe checkout)
cd "$INFRA_FE_REPO" && npm ci && npx cypress run --component --config-file config/cypress.config.ts
```

The Bruno collection (`bruno/`) has five checks: status, unauthenticated cloud-provider
read, seeded script-type list, unauthenticated script-type create, and OpenAPI served.
Verified green on 2026-07-12 (5 requests, 6/6 tests, 4/4 assertions). Two of the
checks deliberately send no token to demonstrate finding F1.

## What this stack does NOT provide (intentionally omitted)

| Omitted | Why | Consequence |
|---|---|---|
| Keycloak / Tier-1 gateway | backend disables auth in code; SPA needs Keycloak only for interactive login | API is open; SPA renders but cannot complete login |
| Vault / OpenBao | `VaultServiceImpl` is lazy | dummy env satisfies bean binding; secret-store calls fail if exercised |
| Gitea | only used by script-content operations | dummy env; those operations fail |
| SMTP mailer | notification e-mails only | dummy env; sending fails |
| ArgoCD / Crossplane provisioner | separate executor system | `to-provision` messages are published but nothing fulfils them |
| Zookeeper, Kafka SASL | Kafka runs KRaft PLAINTEXT, app runs SASL off | simpler, lighter |

This is a component-in-isolation stack (governance evidence that the backend runs
without ArgoCD as a prerequisite).

## Deviations needed to boot (upstream defects)

The upstream backend ships a `docker-compose.yml` and a `local` Spring profile, but
the `local` profile does not start as shipped. Two overrides are applied in this
stack's compose (details in `docs/be-findings.md`):

1. **Flyway/JPA circular depends-on:** the `local` profile sets
   `defer-datasource-initialization=true`, which with the unconditional custom Flyway
   bean forms a circular `depends-on` and the context fails to refresh. This stack
   mirrors the working `docker` profile: Flyway owns the schema, `ddl-auto=none`, no
   deferred init.
2. **OTEL agent:** the Dockerfile pins an Elastic OTEL java-agent; the compose runs
   the plain jar with `OTEL_SDK_DISABLED=true`.

## Documentation

- [`docs/infrastructure-architecture.md`](docs/infrastructure-architecture.md): integrative view, message flow, topics, trust boundaries (Mermaid).
- [`docs/infrastructure-be-architecture.md`](docs/infrastructure-be-architecture.md): backend internals, persistence, config keys.
- [`docs/infrastructure-fe-architecture.md`](docs/infrastructure-fe-architecture.md): SPA, nginx, auth flow, runtime config.
- [`docs/be-findings.md`](docs/be-findings.md): backend findings (F1 auth disabled, F2 local profile DOA, F3 Kafka no DLT, F4 dead input filter).
- [`docs/fe-findings.md`](docs/fe-findings.md): frontend findings (token storage, missing CSP, client-side roles, test suite).

## Verification (2026-07-12)

- Backend: `mvn package` green; `mvn test` 246 tests, 0 failures; 50 Flyway migrations on Postgres 16; boot ~3.4s. Unauthenticated GET/POST succeed; Kafka `provisioned` message consumed.
- Frontend: `npm ci` + `vite build` green; serves 200 with SPA fallback; Cypress component suite 71/80 passing (6 failures likely Node-version skew, see fe-findings F-FE-4).
- Bruno: 5 requests, 6/6 tests, 4/4 assertions green.

## License

Upstream is EUPL-1.2. This stack adds only local orchestration.
