# notification-service — manual setup walkthrough

Step-by-step equivalent of `./start.sh` — useful for debugging or learning each phase independently.

## Phase 1: Clone the upstream repos

All three repos are cloned into `repos/` (gitignored — never committed to this stack).

```bash
git clone \
  https://code.europa.eu/simpl/simpl-open/development/contract-billing/notification-service.git \
  repos/notification-service

git clone \
  https://code.europa.eu/simpl/simpl-open/development/contract-billing/common_logging.git \
  repos/common_logging

git clone \
  https://code.europa.eu/simpl/simpl-open/development/contract-billing/common.git \
  repos/common
```

## Phase 2: Build the JAR

The `pom.xml` uses `${env.PROJECT_RELEASE_VERSION}` as the version — Maven fails without it.

```bash
export PROJECT_RELEASE_VERSION=local
cd repos/notification-service
./mvnw clean install -DskipTests
ls target/notification_service-local.jar   # verify
cd ../..
```

First run: 5–10 minutes (Maven downloads ~300 MB of dependencies).
Subsequent runs: under 60 seconds (local Maven cache warm).

## Phase 3: Build the Docker image

```bash
docker build -t simpl-notification-service:local repos/notification-service/
```

The Dockerfile (`eclipse-temurin:21-jdk-alpine`) downloads the Elastic OTel Java agent from Maven Central during this step — network access required. Takes ~2 minutes on first build.

## Phase 4: Start infrastructure (Kafka + Mailpit)

```bash
docker compose up -d zookeeper kafka mailpit kafka-ui
# Wait for Kafka to be ready (up to ~60s)
docker compose ps
```

Verify Kafka is healthy:
```bash
docker exec simpl-kafka kafka-broker-api-versions --bootstrap-server kafka:9093
```

## Phase 5: Start the notification service

```bash
docker compose up -d notification-service
docker compose logs -f notification-service   # Ctrl-C when "Started" appears
```

## Phase 6: Send a test notification

Publish a message directly to the `notifications` Kafka topic to trigger an email:

```bash
docker exec -i simpl-kafka kafka-console-producer \
  --broker-list kafka:9093 \
  --topic notifications <<< \
  '{"channel":"email","to":"test@example.com","subject":"Manual test","message":"Hello from manual setup."}'
```

Then open Mailpit at http://localhost:8025 — the email should appear within 1–2 seconds.

## Phase 7: Inspect the Kafka topic

Open Kafka UI at http://localhost:9081, select the `local` cluster, and browse the `notifications` topic to see consumed messages.

## Verify

```bash
# Check service is up (if actuator is on classpath)
curl http://localhost:8081/actuator/health

# Check logs
docker compose logs notification-service | tail -20

# Check email was received
curl -s http://localhost:8025/api/v1/messages | python3 -m json.tool | grep subject
```

## Teardown

```bash
# Stop containers, keep volumes
./stop.sh

# Stop and wipe all data (full reset)
./stop.sh --full
```

## See also

- [Architecture overview](notification-service-architecture.md)
- [Main README](../README.md)
