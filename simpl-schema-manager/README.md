# simpl-schema-manager-local

A scripts-and-documentation repo for running the **Simpl-Open Schema Manager** locally on a Mac
(OrbStack or Docker Desktop).
Intended for proving modularity of the schema manager, for evaluation and debugging on a 16 GB Mac.

This repo holds **only orchestration scripts, configuration, and documentation**.
No upstream Simpl-Open code is committed here — sources live at `code.europa.eu` and are
cloned into `repos/` (gitignored) at build time:

- `gaia-x-edc/simpl-schema-manager` — the service itself

The schema manager's only EU-internal Maven dependency (`simpl-schema-versioning:1.0.0-SNAPSHOT`)
is fetched anonymously from the EU GitLab Package Registry — **no GITLAB_PAT required**.

---

## Quick start

```bash
git clone https://github.com/barrynauta/simpl-schema-manager-local.git
cd simpl-schema-manager-local
./start.sh
```

First run takes 4–8 minutes (Maven dependency download + Docker image build).
Subsequent starts are under 15 seconds.

When ready, the smoke test confirms Fuseki has the four datasets the app auto-creates and that the
unauthenticated `/webhooks` probe returns `[]`.

---

## What this stack provides

- **schema-manager** (Spring Boot 3.5.x / Java 21) on `:8085` — REST service for managing JSON-LD/SHACL
  schemas, their versions, and webhook subscribers. Built from upstream source.
- **Apache Jena Fuseki** (`secoresearch/fuseki:5.3.0`) on `:3030` — RDF triplestore. The schema-manager
  auto-creates four datasets at boot: `ds_schemas`, `ds_schema_metadata`, `ds_schema_categories`,
  `ds_webhooks`.
- **Kafka** (`bitnamilegacy/kafka:3.3.2`) on `:9094` — single-broker KRaft (no Zookeeper). Used by the
  schema-manager to publish schema-change events to subscribed webhooks.
- **Kafka UI** (`provectuslabs/kafka-ui:v0.7.2`) on `:9001` — browse topics and inspect messages.

For an architecture diagram and per-component breakdown, see
[`docs/schema-manager-architecture.md`](docs/schema-manager-architecture.md).

## What this stack does NOT provide

- **Authentication.** Keycloak and Tier-1 / Tier-2 gateways are deliberately omitted. The unauthenticated
  endpoints (`/webhooks`) work directly. Endpoints under `/schemas` return a Belgif RFC-7807 400 demanding
  an `Authorization` header — that's the production JWT gate, not a stack failure.
- **ArgoCD / Helm.** The stack proves the component runs standalone. Kubernetes is an acceptable
  substrate dependency; ArgoCD is **not** an acceptable per-component installation prerequisite.
- **HashiCorp Vault / OpenBao.** Fuseki admin password is passed as a plain env var.
- **Kafka SASL / TLS.** Production uses `SASL_SSL`; local uses `PLAINTEXT`.
- **OpenTelemetry export.** Not wired in by this stack.
- Production-grade HA, secrets management, or monitoring.

---

## Prerequisites

**Software:**

| Tool | Version | Notes |
|------|---------|-------|
| Docker | 20.10+ | [OrbStack](https://orbstack.dev/) recommended on Mac |
| Docker Compose | 2.0+ | Bundled with OrbStack and Docker Desktop |
| Git | 2.30+ | |
| curl | any | Used by `start.sh` smoke test |

Java is **not** required on the host — the build runs inside the Maven builder image.

**System:**

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| RAM | 3 GB | 4 GB allocated to Docker |
| Disk | 2 GB | 4 GB (Maven cache + Docker images + repo) |

---

## What `./start.sh` actually does

The same flow run as individual steps — useful for debugging.
See [`docs/schema-manager-manual-setup.md`](docs/schema-manager-manual-setup.md).

Flags:
- `--rebuild` — force Maven re-build and Docker image rebuild even if they exist
- `--run-tests` — after the stack is up, run the Bruno smoke-test collection inside the docker network (no host install of Bruno needed)

---

## Repository structure

```
simpl-schema-manager-local/
├── README.md              This file.
├── LICENSE                EUPL-1.2 (matches upstream).
├── .gitignore             Excludes repos/, .env, .claude/.
├── docker-compose.yml     Defines the full local stack.
├── Dockerfile.local       Multi-stage Maven build + runtime, replacing the
│                          upstream single-stage Dockerfile that copies a
│                          pre-built JAR from GitLab CI.
├── start.sh               Idempotent one-shot setup (clone → build → up → smoke).
├── stop.sh                Stop containers (--full wipes volumes).
├── .env.example           Template for port/credential overrides.
├── bruno/                 Bruno HTTP smoke-test collection.
│   ├── bruno.json                              Collection metadata.
│   ├── environments/local.bru                  Bruno desktop app — hits localhost ports on the host.
│   ├── environments/docker.bru                 In-network run via ./start.sh --run-tests — uses service hostnames.
│   └── 0{1..4}-*.bru                           Individual tests with inline assertions.
└── docs/
    ├── schema-manager-architecture.md   Architecture diagram and design notes.
    └── schema-manager-manual-setup.md   Step-by-step walkthrough.
```

---

## Configuration

Defaults live in `docker-compose.yml`. Copy `.env.example` to `.env` to override:

| Variable | Default | Purpose |
|---|---|---|
| `FUSEKI_PORT` | `3030` | Host port for Fuseki |
| `FUSEKI_ADMIN_USER` | `admin` | Fuseki admin username |
| `FUSEKI_ADMIN_PASSWORD` | `admin1234` | Fuseki admin password (also passed to schema-manager) |
| `KAFKA_HOST_PORT` | `9094` | Host port for the external Kafka listener |
| `KAFKA_UI_PORT` | `9001` | Host port for Kafka UI |
| `SCHEMA_MANAGER_PORT` | `8085` | Host port for the schema-manager API |

---

## Smoke tests (Bruno)

A Bruno collection lives in `bruno/`. Each request includes inline `tests` assertions that pin
the expected schema-manager behaviour. Four checks:

1. `GET /webhooks` returns `200` with an empty array — unauthenticated liveness.
2. `GET /schemas` returns Belgif RFC-7807 `400` without an `Authorization` header — confirms the
   JWT gate is wired on the collection endpoint.
3. `GET /schemas/{name}/versions` returns the same `400` — confirms the gate covers parametric
   routes too.
4. Fuseki's `/$/datasets` admin endpoint reports the four bootstrap datasets (`ds_schemas`,
   `ds_schema_metadata`, `ds_schema_categories`, `ds_webhooks`) — confirms the schema-manager's
   Fuseki client successfully initialised the triplestore at boot.

### Option 1 — `./start.sh --run-tests` (no Bruno install needed)

Brings up the stack and then runs the bruno collection inside the docker network using
`@usebruno/cli` in a one-shot container. Uses the `docker` environment
(`environments/docker.bru`) where URLs resolve to internal docker hostnames
(`http://schema-manager:8085`, `http://fuseki:3030`).

```bash
./start.sh --run-tests
```

The bruno container is launched as a one-shot `docker compose run --rm` (not `up`), so it's
recreated fresh on every invocation and removed on exit. The stack stays up afterwards so you
can re-run the tests alone with `docker compose --profile tests run --rm bruno-smoke-test`, or
tear everything down with `./stop.sh`.

### Option 2 — Bruno desktop app

Open `bruno/` in the [Bruno](https://www.usebruno.com/) app, pick the `local` environment
(`environments/local.bru`, points at `http://localhost:8085` and `http://localhost:3030`), and
run the collection. Useful when iterating on individual requests or inspecting responses
interactively.

Same shape as the [`simpl-catalogue/bruno/`](../simpl-catalogue/bruno/) collection — two
environments (`local` for the desktop app on the host, `docker` for in-container runs).

## Testing manually

The schema-manager exposes a REST API at `:8085`. Most endpoints (`/schemas`, `/schemas/{name}/versions`,
`/schemas/{name}/{version}`) require a Tier-1 JWT in the `Authorization` header — those return a
Belgif RFC-7807 400 problem if called without one. `GET /webhooks` is unauthenticated and is the
liveness probe used by `start.sh`.

### Smoke test 1 — Fuseki datasets

```bash
curl -s -u admin:admin1234 http://localhost:3030/\$/datasets | \
  python3 -c "import sys,json; print('\n'.join(d['ds.name'] for d in json.load(sys.stdin)['datasets']))"
```

Expected output (order may vary):
```
/ds_schemas
/ds_schema_metadata
/ds_schema_categories
/ds_webhooks
```

### Smoke test 2 — Unauthenticated webhooks endpoint

```bash
curl -s -w "\nHTTP %{http_code}\n" http://localhost:8085/webhooks
```

Expected output:
```
[]
HTTP 200
```

### Smoke test 3 — Authenticated endpoint without auth

```bash
curl -s -w "\nHTTP %{http_code}\n" http://localhost:8085/schemas
```

Expected output:
```
{"type":"urn:problem-type:belgif:badRequest", ... "detail":"Required request header 'Authorization' ..."}
HTTP 400
```

This is the expected behaviour without a Tier-1 JWT. It confirms the controller is alive and the
auth gate is wired.

### Watch the service logs in real time

```bash
docker logs -f simpl-schema-manager
```

A successful startup logs four `Dataset ... created` lines followed by
`Started SimplSchemaManagerApplication in <n> seconds`.

---

## Known limitations and design choices

**Maven version via env var.** `pom.xml` uses `${env.PROJECT_RELEASE_VERSION}` as the artifact version.
`Dockerfile.local` exports `PROJECT_RELEASE_VERSION=local` before running `mvnw`. Forgetting this env var
causes a Maven build failure at parse time.

**Upstream `settings.xml` is CI-only.** The repo ships a `settings.xml` whose `gitlab-maven` server uses
`${env.CI_JOB_TOKEN}` — a GitLab CI predefined variable. `Dockerfile.local` does **not** pass this file
to mvnw with `-s`; default Maven settings let the build hit
`https://code.europa.eu/api/v4/projects/1462/packages/maven` anonymously, which works for the only
internal dependency (`simpl-schema-versioning:1.0.0-SNAPSHOT`).

**Single-node Kafka (KRaft).** No HA, no replication. Local dev stack only.

**No persistent Fuseki volume.** Datasets are re-created at each boot (the schema-manager does this
automatically). To persist, mount a volume to `/fuseki` and restart.

**Upstream README is empty.** The upstream repo's `README.md` is a one-line stub, so this README and
the docs in `docs/` are the authoritative local-run reference until upstream documents the service.

**Upstream drift.** The schema-manager is under active development. If a phase stops working,
re-pull `repos/simpl-schema-manager` (`./start.sh --rebuild`) and check the upstream CHANGELOG.

---

## Service URLs

| Service | URL | Purpose |
|---------|-----|---------|
| Schema Manager API | http://localhost:8085 | REST API (`/webhooks` unauth, `/schemas` Tier-1 JWT) |
| Fuseki UI | http://localhost:3030 | Triplestore admin and dataset browser (admin / admin1234) |
| Kafka UI | http://localhost:9001 | Browse topics and messages |

---

## License

This repository: see [LICENSE](LICENSE) (EUPL-1.2).
Upstream `simpl-schema-manager` in `repos/`: EUPL-1.2 per upstream repo.
