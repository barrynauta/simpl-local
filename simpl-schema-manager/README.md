# simpl-schema-manager-local

A scripts-and-documentation repo for running the **Simpl-Open Schema Manager** locally on a Mac
(OrbStack or Docker Desktop).
Intended for proving modularity of the schema manager, for evaluation and debugging on a 16 GB Mac.

This repo holds **only orchestration scripts, configuration, and documentation**.
No upstream Simpl-Open code is committed here — sources live at `code.europa.eu` and are
cloned into `repos/` (gitignored) at build time:

- `gaia-x-edc/simpl-schema-manager` — the backend service
- `gaia-x-edc/simpl-schema-manager-ui` — the Vue 3 frontend

The schema manager's only EU-internal Maven dependency (`simpl-schema-versioning:1.0.0-SNAPSHOT`)
is fetched anonymously from the EU GitLab Package Registry — **no GITLAB_PAT required**.

---

## Quick start

```bash
git clone https://github.com/barrynauta/simpl-schema-manager-local.git
cd simpl-schema-manager-local
./start.sh
```

First run takes 3–5 minutes (Maven dependency download + UI Vite build + Docker image assembly).
Subsequent starts are under 15 seconds.

When ready:

- the smoke test confirms Fuseki has the four datasets the app auto-creates and that
  the unauthenticated `/webhooks` probe returns `[]`;
- open **http://localhost:4322** in a browser to use the schema-manager UI (no login —
  Keycloak is bypassed locally);
- three valid sample SHACL files are staged in `samples/` and ready for the UI's
  "Upload schema" form — see [Uploading a sample schema](#uploading-a-sample-schema).

---

## What this stack provides

- **schema-manager** (Spring Boot 3.5.x / Java 21) on `:8085` — REST service for managing JSON-LD/SHACL
  schemas, their versions, and webhook subscribers. Built from upstream source.
- **schema-manager-ui** (Vue 3 + Vite + PrimeVue, served by nginx-unprivileged) on `:4322` — the
  upstream Vue frontend, served against the local backend with Keycloak deliberately bypassed (see
  [`docs/schema-manager-bypass.md`](docs/schema-manager-bypass.md)).
- **Apache Jena Fuseki** (`secoresearch/fuseki:5.3.0`) on `:3030` — RDF triplestore. The schema-manager
  auto-creates four datasets at boot: `ds_schemas`, `ds_schema_metadata`, `ds_schema_categories`,
  `ds_webhooks`.
- **Kafka** (`bitnamilegacy/kafka:3.3.2`) on `:9094` — single-broker KRaft (no Zookeeper). Used by the
  schema-manager to publish schema-change events to subscribed webhooks.
- **Kafka UI** (`provectuslabs/kafka-ui:v0.7.2`) on `:9001` — browse topics and inspect messages.

For an architecture diagram and per-component breakdown, see
[`docs/schema-manager-architecture.md`](docs/schema-manager-architecture.md).

## What this stack does NOT provide

- **Real authentication.** Keycloak and Tier-1 / Tier-2 gateways are deliberately omitted. The UI's
  Keycloak guard is short-circuited by setting the three `PUBLIC_AUTH_KEYCLOAK_*` env vars to empty
  strings — the UI has a built-in `isAuthenticationEnabled()` switch that bypasses the redirect when
  any of those is empty. UI → backend requests pass through an nginx reverse proxy that injects a
  fake `Authorization: Bearer <JWT>` header. The backend's `RoleUtil` uses auth0
  `JWT.decode()` which performs no signature, expiry, or issuer validation — only reads
  `realm_access.roles` — so a hand-crafted JWT with `GA_SCHEMA_ADMIN` is sufficient. Full mechanics
  in [`docs/schema-manager-bypass.md`](docs/schema-manager-bypass.md). **Do not** deploy this
  configuration anywhere other than a developer's laptop.
- **ArgoCD / Helm.** The stack proves the component runs standalone. Kubernetes is an acceptable
  substrate dependency; ArgoCD is **not** an acceptable per-component installation prerequisite.
- **HashiCorp Vault / OpenBao.** Fuseki admin password is passed as a plain env var.
- **Kafka SASL / TLS.** Production uses `SASL_SSL`; local uses `PLAINTEXT`.
- **OpenTelemetry export.** Not wired in by this stack.
- Production-grade HA, secrets management, or monitoring.

---

## Uploading a sample schema

The fastest way to confirm the full UI ↔ proxy ↔ backend ↔ Fuseki ↔ SHACL-validator loop:

1. Open **http://localhost:4322** — the schema list is empty.
2. Click **Upload schema**.
3. Fill in the form:

   | Field | Value |
   |---|---|
   | Schema file | `samples/sample-data-offering.ttl` |
   | Name | `SimplDataOffering` (PascalCase, 3–64 chars) |
   | Title | `Simpl Data Offering Schema` |
   | Description | `Canonical data offering schema (local stack sample).` |
   | Resource Type | `Data` (one of `Application` / `Data` / `Infrastructure`, case-insensitive) |

4. Submit. The schema appears in the list and `GET http://localhost:4322/v1/schemas` returns it.

The three sample TTLs (`sample-data-offering.ttl`, `sample-application-offering.ttl`,
`sample-infrastructure-offering.ttl`) are copies of the upstream
`ShaclValidationServiceTest` "valid" fixtures, staged by `start.sh` into `samples/`
after the upstream clone — see [`samples/README.md`](samples/README.md). The files
are `.gitignore`d; only `samples/README.md` is tracked.

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
- `--rebuild` — force Maven re-build and Docker image rebuild even if they exist.
- `--run-tests` — after the stack is up, seed a schema (so the Kafka producer fires) and run
  the Bruno smoke-test collection inside the docker network (no host install of Bruno needed).
- `--with-notifications` — additionally bring up Mailpit and `simpl-notification-service`
  consuming the `notifications` topic, so the email side-channel is observable at
  `http://localhost:8025`. Requires the `simpl-notification-service:local` image to be
  built once first via the sibling stack (`cd ../simpl-notification-service && ./start.sh --rebuild`).

---

## Repository structure

```
simpl-schema-manager-local/
├── README.md              This file.
├── LICENSE                EUPL-1.2 (matches upstream).
├── .gitignore             Excludes repos/, .env.
├── docker-compose.yml     Defines the full local stack.
├── docker-compose.notifications.yml
│                          Optional overlay: Mailpit + simpl-notification-service consuming
│                          the notifications Kafka topic. Enabled with --with-notifications.
├── Dockerfile.local       Multi-stage Maven build + runtime for the backend,
│                          replacing the upstream single-stage Dockerfile that
│                          copies a pre-built JAR from GitLab CI.
├── Dockerfile.local-ui    Multi-stage Vite build + nginx for the UI. The
│                          nginx config bakes in the auth-bypass proxy.
├── nginx.conf             UI nginx config — SPA serving + /v1 reverse proxy
│                          with Authorization header injection.
├── env-config.local.js    Runtime UI env — empty Keycloak (disables auth
│                          guard), API URL = /v1 (same-origin via proxy).
├── start.sh               Idempotent one-shot setup (clone → build → up → smoke).
├── stop.sh                Stop containers (--full wipes volumes).
├── .env.example           Template for port/credential overrides.
├── bruno/                 Bruno HTTP smoke-test collection.
│   ├── bruno.json                              Collection metadata.
│   ├── environments/local.bru                  Bruno desktop app — hits localhost ports on the host.
│   ├── environments/docker.bru                 In-network run via ./start.sh --run-tests — uses service hostnames.
│   └── 0{1..7}-*.bru                           Individual tests with inline assertions.
├── samples/               Sample SHACL files ready for the UI's upload form.
│   ├── README.md                               Form-field reference and manual-copy commands.
│   └── *.ttl                                   (gitignored) Staged by start.sh from the upstream test fixtures.
└── docs/
    ├── schema-manager-architecture.md   Architecture diagram and design notes.
    ├── schema-manager-bypass.md         How the UI ↔ backend auth bypass works.
    ├── schema-manager-manual-setup.md   Step-by-step walkthrough.
    └── upstream-issues.md               Findings worth reporting upstream (e.g. SSM-001).
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
| `UI_PORT` | `4322` | Host port for the schema-manager UI |
| `MAILPIT_SMTP_PORT` | `1026` | (with `--with-notifications`) Host port for Mailpit SMTP |
| `MAILPIT_UI_PORT` | `8025` | (with `--with-notifications`) Host port for Mailpit Web UI |

---

## Smoke tests (Bruno)

A Bruno collection lives in `bruno/`. Each request includes inline `tests` assertions that pin
the expected schema-manager behaviour. Seven checks:

1. `GET /webhooks` returns `200` with an empty array — unauthenticated liveness.
2. `GET /schemas` returns Belgif RFC-7807 `400` without an `Authorization` header — confirms the
   JWT gate is wired on the collection endpoint.
3. `GET /schemas/{name}/versions` returns the same `400` — confirms the gate covers parametric
   routes too.
4. Fuseki's `/$/datasets` admin endpoint reports the four bootstrap datasets (`ds_schemas`,
   `ds_schema_metadata`, `ds_schema_categories`, `ds_webhooks`) — confirms the schema-manager's
   Fuseki client successfully initialised the triplestore at boot.
5. `GET http://schema-manager-ui:8080/` returns the SPA's `index.html` with the
   `env-config.js` script tag — confirms the UI build landed in the nginx image.
6. `GET http://schema-manager-ui:8080/v1/schemas` returns `200` (not `400`) — confirms the
   UI's nginx proxy rewrites `/v1/*` → `/*` and injects the fake `Authorization` header,
   and that the backend's `JWT.decode()` accepts the hand-crafted claim. This is the
   load-bearing assertion for the auth bypass.
7. Kafka-UI's `/api/clusters/{name}/topics/notifications` reports a non-empty message count
   — confirms the schema-manager fired its Kafka producer when `start.sh --run-tests` seeded
   a schema. The producer-side half of [SSM-001](docs/upstream-issues.md), observed live.

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

## Kafka usage

The schema-manager uses Kafka for **exactly one purpose**: producing email-notification messages
on schema lifecycle events. Nothing else in the service touches a broker.

The whole producer surface is one class:

```java
// kafka/service/NotificationService.java
public class NotificationService {
    private static final String NOTIFICATION_TOPIC = "notifications";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendEmailNotification(SendEmailRequest emailRequest) {
        kafkaTemplate.send(NOTIFICATION_TOPIC, new ObjectMapper().writeValueAsString(emailRequest));
    }
}
```

…invoked from exactly two call sites in `SchemaService`:

- `sendSchemaCreatedOrUpdatedEmailNotification()` — after schema create / new version upload.
- `sendSchemaStatusChangeEmailNotification()` — when a schema status flips between `PUBLISHED` and `REVOKED`.

The topic is consumed by **simpl-notification-service** (also a Simpl-Open component), which
dispatches the SMTP email. The message envelope (`channel`, `to`, `cc`, `subject`, `message`)
matches that consumer's expected schema. To trace an email end-to-end across both stacks, run
[`simpl-notification-service/`](../simpl-notification-service/) alongside this one against the
same Kafka broker and watch Mailpit catch the dispatched email.

### What is NOT on Kafka

**Webhooks.** `EventService.notifyWebhooksOnSchemaChanges()` POSTs the event JSON directly via
Spring `RestTemplate` (wrapped in `RetryTemplate` for retries) to subscriber URLs registered in
the `ds_webhooks` Fuseki dataset. Pure HTTP, synchronous, no broker involved. If you want to know
about schema changes, register a webhook (`POST /webhooks`); the Kafka path is reserved for the
email side-channel only.

### Sharp edges worth knowing

- **The email recipient is hardcoded — and the default is a public Mailinator inbox.**
  `email.address` (default `simpl123@mailinator.com`) is a single fixed address. *Every*
  schema-lifecycle event in the entire deployment notifies one inbox. No per-tenant routing,
  no admin lookup, no role-based fan-out. The default is **publicly readable at
  [mailinator.com/v4/public/inboxes.jsp?to=simpl123](https://www.mailinator.com/v4/public/inboxes.jsp?to=simpl123)**
  — and the upstream Helm chart does not override `EMAIL_ADDRESS`, so a vanilla
  `helm install` runs with this default active. Full write-up:
  [`docs/upstream-issues.md` → SSM-001](docs/upstream-issues.md#ssm-001--emailaddress-defaults-to-a-public-mailinator-inbox-helm-chart-does-not-override).
- **The `kafka.enabled` flag in `application.properties` is misleading.** It exists, but
  `KafkaTemplate` is unconditionally autowired and `NotificationService.sendEmailNotification()`
  has no guard around `kafkaTemplate.send(...)`. Setting `kafka.enabled=false` does not disable
  the call — it just produces records into a broker that may not exist, and Kafka client retries
  will hang the request thread.
- **This is the integration tax flagged in the notification-service assessment.** The
  notification-service local stack's README documents the verdict: *"Kafka transport is
  architecturally disproportionate for a simple email relay … every caller must configure a
  Kafka producer to send what amounts to an SMTP message. No service has integrated with this
  component yet."* The schema-manager is now the first concrete caller — paying that tax to
  send one event → one email. A REST `POST /notifications` would deliver the same outcome with
  no Kafka producer, no client-id config, no SASL/SSL parameters, and synchronous delivery
  confirmation.

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
| Schema Manager UI | http://localhost:4322 | Vue 3 frontend, Keycloak bypassed — open in a browser |
| Schema Manager API | http://localhost:8085 | REST API directly (`/webhooks` unauth, `/schemas` JWT-gated) |
| Schema Manager API via UI proxy | http://localhost:4322/v1 | Same API, but with auth header injected — `/v1/schemas` returns 200 |
| Fuseki UI | http://localhost:3030 | Triplestore admin and dataset browser (admin / admin1234) |
| Kafka UI | http://localhost:9001 | Browse topics and messages |
| Mailpit Web UI (with `--with-notifications` only) | http://localhost:8025 | Captured email — confirms what address the schema-manager addresses notifications to (see SSM-001) |

---

## License

This repository: see [LICENSE](LICENSE) (EUPL-1.2).
Upstream `simpl-schema-manager` in `repos/`: EUPL-1.2 per upstream repo.
