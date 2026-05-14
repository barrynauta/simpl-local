# simpl-notification-service-local

A scripts-and-documentation repo for running the **Simpl-Open Notification Service** locally on a Mac
(OrbStack or Docker Desktop).
Intended for proving modularity of the notification service, for evaluation and debugging on a 16 GB Mac.

This repo holds **only orchestration scripts, configuration, and documentation**.
No upstream Simpl-Open code is committed here — sources live at `code.europa.eu` and are
cloned into `repos/` (gitignored) at build time:

- `contract-billing/notification-service` — the service itself
- `contract-billing/common_logging` — internal logging library (EU, no public Maven package)
- `contract-billing/common` — internal common library (EU, no public Maven package)

---

## Upstream component assessment — FAIL (technical review 2026-05-08)

The local stack in this repo runs and the email-dispatch path is verified end-to-end. However, **the upstream `notification-service` component itself was assessed as FAIL** in a four-persona technical review on 2026-05-08 (Solution Architect, Application Expert, Data Expert, Integration Expert — all returned FAIL with high confidence). The local stack is useful for exploring and reproducing the behaviour, but the component should not be integrated against in its current shape.

Three concerns are load-bearing for that verdict:

- **SMS channel is an unimplemented stub.** `SMSService.send()` logs one line and returns. The accompanying `SMSServiceTest` is `assertTrue(true)` with a `//TO BE IMPLEMENTED` comment — fabricated test coverage for a channel that delivers nothing. Callers sending `channel: sms` receive no delivery, no error, no indication of failure. One of two advertised channels is non-functional.
- **The email-path test exists; the SMS-path test does not.** `ConsumerTest.java` (with `@EmbeddedKafka`) is genuinely thorough for the Kafka→SMTP path. There is no real test for the SMS channel, and the placeholder test misleads coverage reports. Test coverage of the component as a whole is therefore overstated.
- **Kafka transport is architecturally disproportionate for a simple email relay.** ADR-03 (23 Jan 2025) documents the choice of Kafka, but in practice every caller must configure a Kafka producer to send what amounts to an SMTP message. No service has integrated with this component yet (per FTA: *"integration has not yet been included in the latest release"*) — meaning the integration tax has not actually been paid by any caller. A REST-based `POST /notifications` would deliver the same outcome with no infrastructure burden on callers and would provide synchronous delivery confirmation. ADR-03 is worth re-presenting to the programme before producers commit to the Kafka contract.

Additional HIGH findings flagged in the same review (full detail in the Notion review page):

- AsyncAPI spec is structurally invalid and contractually wrong — `action: send` documents a producer for what is a consumer service, and a malformed `$ref` makes the document unparseable. Any team building a producer from this spec would build the wrong integration.
- GDPR risk — `Consumer.prepareDefaultErrorNotification()` appends raw Kafka payloads (which may contain participant PII) directly into outbound error emails and logs.
- No dead-letter queue — `FixedBackOff(1000L, 1)` discards failed messages after a single 1-second retry. SMTP transient failure results in permanent message loss, defeating Kafka's primary durability benefit.

**Recommendation from the review:** replace with a REST endpoint (preferred — revisits ADR-03 at programme level), or rework with at minimum a DLQ, GDPR-safe error handling, a corrected AsyncAPI spec, an implemented or removed SMS channel, and pom.xml alignment to the parent BOM (estimated 5–8 days). The current moment — before any producer has integrated — is the right moment to make that call.

What this means for the local stack here: nothing changes mechanically — `./start.sh` still works and Mailpit still catches dispatched email. Treat the stack as a tool for confirming the verdict and for demonstrating the behaviour, not as a launchpad for building producers against the current contract.

---

## Quick start

```bash
git clone https://github.com/barrynauta/simpl-notification-service-local.git
cd simpl-notification-service-local
./start.sh
```

First run takes 8–12 minutes (Maven dependency download + Docker image build with OTEL agent download).
Subsequent starts are under 30 seconds.

When ready, a test notification is published to Kafka automatically. Check the result:

```bash
open http://localhost:8025   # Mailpit — should show 1 email
```

---

## Status

| Phase | What | Status |
|-------|------|--------|
| 1 | Clone upstream `notification-service`, `common_logging`, `common` | ✅ |
| 2 | Build Docker image — Stage 1: Maven builds all three in sequence | ✅ |
| 3 | Build Docker image — Stage 2: runtime (`eclipse-temurin:21-jdk-alpine`) | ✅ |
| 4 | Start Kafka + Zookeeper via Compose | ✅ |
| 5 | Run `notification-service` container | ✅ |
| 6 | Publish test Kafka message → verify email in Mailpit | ✅ |

Verified 2026-05-05. Spring Boot 3.5.6, Kafka 3.9.x client, Java 21.

---

## What this stack provides

- **notification-service** (Spring Boot 3.5.6 / Java 21) on `:8081` — Kafka consumer that dispatches
  emails from the `notifications` topic. Built from upstream source.
- **Kafka** (`confluentinc/cp-kafka:7.5.0`) on `:9093` — single-broker message bus with
  auto-created `notifications` topic.
- **Mailpit** (`axllent/mailpit:v1.21`) on `:1025` (SMTP) / `:8025` (web UI) — catches all outbound
  email so you can inspect it without a real mail server.
- **Kafka UI** (`provectuslabs/kafka-ui:v0.7.1`) on `:9081` — browse topics and inspect messages.

For architecture diagram and per-component breakdown,
see [`docs/notification-service-architecture.md`](docs/notification-service-architecture.md).

## What this stack does NOT provide

- **Authentication.** Keycloak, Tier-1 / Tier-2 gateways are deliberately omitted. The notification
  service has no HTTP REST API, so there is no auth surface to protect.
- **ArgoCD / Helm.** The stack proves the component runs standalone. Kubernetes is an acceptable
  substrate dependency; ArgoCD is **not** an acceptable per-component installation prerequisite.
- **HashiCorp Vault / OpenBao.** Secrets are passed as plain environment variables.
- **Kafka SASL / TLS.** Production uses `SASL_SSL`; local uses `PLAINTEXT`.
- **OpenTelemetry export.** The Elastic OTel agent is loaded by the Dockerfile but disabled via
  `OTEL_SDK_DISABLED=true`.
- Production-grade HA, secrets management, or monitoring.

---

## Prerequisites

**Software:**

| Tool | Version | Notes |
|------|---------|-------|
| Docker | 20.10+ | [OrbStack](https://orbstack.dev/) recommended on Mac |
| Docker Compose | 2.0+ | Bundled with OrbStack and Docker Desktop |
| Java JDK | 21+ | Install via `brew install openjdk@21` or [SDKMAN](https://sdkman.io/) |
| Git | 2.30+ | |

**System:**

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| RAM | 4 GB | 6 GB allocated to Docker |
| Disk | 3 GB | 5 GB (Maven cache + Docker images + repo) |

---

## What `./start.sh` actually does

The same flow run as individual steps — useful for debugging.
See [`docs/notification-service-manual-setup.md`](docs/notification-service-manual-setup.md).

Flags:
- `--rebuild` — force Maven re-build and Docker image rebuild even if they exist

---

## Repository structure

```
simpl-notification-service-local/
├── README.md              This file.
├── LICENSE                EUPL-1.2 (matches upstream).
├── .gitignore             Excludes repos/, .env, .claude/.
├── docker-compose.yml     Defines the full local stack.
├── start.sh               Idempotent one-shot setup.
├── stop.sh                Stop containers (--full wipes volumes).
├── .env.example           Template for port/credential overrides.
└── docs/
    ├── notification-service-architecture.md   Architecture diagram and design notes.
    └── notification-service-manual-setup.md   Step-by-step walkthrough.
```

---

## Configuration

Defaults live in `docker-compose.yml`. Copy `.env.example` to `.env` to override:

| Variable | Default | Purpose |
|---|---|---|
| `KAFKA_HOST_PORT` | `9093` | Host port for Kafka broker |
| `KAFKA_UI_PORT` | `9081` | Host port for Kafka UI |
| `MAILPIT_SMTP_PORT` | `1025` | Host port for Mailpit SMTP |
| `MAILPIT_UI_PORT` | `8025` | Host port for Mailpit web UI |
| `NOTIFICATION_SERVICE_PORT` | `8081` | Host port for notification-service management |
| `DEFAULT_EMAIL_RECEIVER` | `test@example.com` | Default recipient for test notifications |
| `API_KEY` | `local-dev-api-key` | Value for `spring.api-key` |

---

## Testing

The notification service has no HTTP API. The only way to trigger it is to publish a JSON message to the `notifications` Kafka topic. Mailpit captures the resulting email so you can inspect it without a real mail server.

**Prerequisites:** stack is running (`./start.sh` completed) and Mailpit is open at http://localhost:8025.

### Message schema

Every message published to the `notifications` topic must be a JSON object with these fields:

| Field | Type | Required | Description |
|---|---|---|---|
| `channel` | `"email"` | Yes | Notification channel — only `email` is implemented |
| `to` | string | Yes | Primary recipient address |
| `cc` | array of strings | No | Additional recipients |
| `subject` | string | Yes | Email subject line |
| `message` | string | Yes | Email body |

### Test 1 — single recipient

**Input** (publish to Kafka):

```bash
docker exec -i simpl-kafka kafka-console-producer \
  --broker-list kafka:9093 \
  --topic notifications <<< \
  '{"channel":"email","to":"alice@example.com","subject":"Onboarding approved","message":"Your onboarding request has been approved. You may now access the data space."}'
```

**Expected output in Mailpit** (http://localhost:8025):

- 1 new message appears within ~1 second
- **To:** `alice@example.com`
- **Subject:** `Onboarding approved`
- **Body:** `Your onboarding request has been approved. You may now access the data space.`
- No Cc field

**Validate:**

```bash
curl -s http://localhost:8025/api/v1/messages | python3 -m json.tool | grep -E '"to"|"subject"'
```

### Test 2 — multiple recipients

**Input** (publish to Kafka):

```bash
docker exec -i simpl-kafka kafka-console-producer \
  --broker-list kafka:9093 \
  --topic notifications <<< \
  '{"channel":"email","to":"alice@example.com","cc":["bob@example.com","carol@example.com"],"subject":"Contract closed","message":"Contract #C-2026-001 has been closed. All parties have been notified."}'
```

**Expected output in Mailpit** (http://localhost:8025):

- 1 new message appears within ~1 second
- **To:** `alice@example.com`
- **Cc:** `bob@example.com`, `carol@example.com`
- **Subject:** `Contract closed`
- **Body:** `Contract #C-2026-001 has been closed. All parties have been notified.`

**Validate:**

```bash
curl -s http://localhost:8025/api/v1/messages | python3 -m json.tool | grep -E '"to"|"cc"|"subject"'
```

### Inspect the Kafka topic

To see all messages consumed so far (useful for debugging):

```bash
docker exec simpl-kafka kafka-console-consumer \
  --bootstrap-server kafka:9093 \
  --topic notifications \
  --from-beginning \
  --max-messages 10
```

### Watch the service logs in real time

```bash
docker logs -f simpl-notification-service
```

A successful dispatch logs a line containing the recipient address. A failed dispatch (e.g. SMTP unreachable) logs a `NotificationException`.

---

## Architectural observations

**The Kafka topic is east-west, not north-south.** The `notifications` Kafka topic connects platform-internal services only — Onboarding Manager, Contract Manager, Schema Manager, and the Notification Service all run in the same `common` namespace on the same cluster. No data space boundary is crossed by the Kafka message. The only north-south moment in the entire flow is the SMTP email dispatched at the end, when a message leaves the platform to reach a human recipient's inbox.

```
[Onboarding Manager] ──kafka──▶ [Notification Service] ──SMTP──▶ inbox@participant.eu
        ↑                               ↑                               ↑
   east-west                       east-west                       north-south
 (platform internal)           (platform internal)              (leaves the platform)
```

This raises a design question: if all producers and the consumer are owned by the same platform operator, a synchronous internal call (REST or Spring application event) would achieve the same result with significantly less infrastructure. The Kafka layer adds Zookeeper, a broker, topic management, and consumer group offsets for what is a fire-and-forget internal signal. ADR-03 justified this with fault tolerance and loose coupling, but those arguments apply most strongly at data space boundaries — less so for intra-platform traffic.

The Triggering Module (Infrastructure Provisioning) uses Spring Mail directly today, bypassing the Notification Service entirely. The architecture docs flag this as technical debt to be resolved, but it may also be the more architecturally honest approach for internal platform notifications.

---

## Known limitations and design choices

**SMTP port and TLS hardcoded in upstream.** `application.properties` has `spring.mail.port=587`,
`spring.mail.properties.mail.smtp.starttls.enable=true`, and `spring.mail.properties.mail.smtp.auth=true`
as literals — not env var references. These are overridden via Spring Boot's environment variable binding
(`SPRING_MAIL_PORT`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_*`). A production deployment would parameterise
these properly.

**Maven version via env var.** `pom.xml` uses `${env.PROJECT_RELEASE_VERSION}` as the artifact version.
`start.sh` exports `PROJECT_RELEASE_VERSION=local` before running `mvnw`. Forgetting this env var causes
a Maven build failure.

**OTEL agent downloaded at Docker build time.** The Dockerfile fetches the Elastic OTel Java agent from
Maven Central using `ADD https://...`. This requires network access during `docker build`. The agent is
then disabled at runtime via `OTEL_SDK_DISABLED=true`.

**Version inflation.** The upstream CHANGELOG shows versions 0.0.3 → 2.0.0 with "No changes" — a
programme-level semver violation. The actual functional code has not changed significantly since v0.0.3
(March 2025). This is documented separately in the Simpl governance evidence trail.

**Kafka broker version mismatch.** The `kafka-clients` dependency in `pom.xml` is `3.9.2`, while
`confluentinc/cp-kafka:7.5.0` ships Kafka 3.5.x. The Kafka protocol is backward-compatible and this
works for local dev. A closer match would be `cp-kafka:7.9.x` (Kafka 3.9.x) if it becomes available.

**Single-node Kafka.** No HA or replication (`KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1`). Local dev stack only.

**Upstream drift.** The notification-service is under active development. If a phase stops working,
re-pull `repos/notification-service` and check the upstream CHANGELOG before re-running.

---

## Service URLs

| Service | URL | Purpose |
|---------|-----|---------|
| Mailpit Web UI | http://localhost:8025 | Inspect emails dispatched by the notification service |
| Kafka UI | http://localhost:9082 | Browse Kafka topics and messages |
| Notification Service | http://localhost:8082/health | Health check (if actuator present) |

---

## License

This repository: see [LICENSE](LICENSE) (EUPL-1.2).
Upstream `notification-service` in `repos/`: EUPL-1.2 per upstream repo.
