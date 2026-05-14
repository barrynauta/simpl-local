# Schema Manager — manual setup walkthrough

This document walks through the same steps that `start.sh` performs. Use it when something fails
and you need to isolate which step broke.

## 0. Prerequisites

```bash
docker --version          # 20.10+
docker compose version    # 2.0+
git --version             # 2.30+
```

Java is **not** required on the host — the build runs inside the Maven builder image.

## 1. Clone the upstream source

```bash
mkdir -p repos
git clone https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-schema-manager.git \
  repos/simpl-schema-manager
```

The schema-manager has one internal Maven dependency
(`simpl-schema-versioning:1.0.0-SNAPSHOT`) which Maven pulls anonymously from
`https://code.europa.eu/api/v4/projects/1462/packages/maven` at build time. No PAT, no sibling clones.

## 2. Build the Docker image

```bash
docker build -f Dockerfile.local -t simpl-schema-manager:local .
```

This is a multi-stage build:

1. **Builder** (`maven:3.9-eclipse-temurin-21-alpine`): runs
   `PROJECT_RELEASE_VERSION=local ./mvnw -DskipTests clean package` against the upstream source.
   First run downloads ~300 MB of Maven dependencies; subsequent runs use the Docker layer cache.
2. **Runtime** (`eclipse-temurin:21-jdk-alpine`): copies the resulting JAR; uses the same base image
   as the upstream `Dockerfile`.

Two gotchas the upstream empty README does not mention:

- The upstream `pom.xml` uses `<version>${env.PROJECT_RELEASE_VERSION}</version>` — without that env
  var, Maven fails at POM parse time.
- The upstream `settings.xml` declares `${env.CI_JOB_TOKEN}` as the password for the `gitlab-maven`
  server. Passing it with `-s settings.xml` locally makes the build try to authenticate as a missing
  CI token. The `Dockerfile.local` deliberately uses default Maven settings (anonymous), which works
  because the package registry serves the snapshot dep without auth.

## 3. Start the supporting stack and the service

```bash
docker compose up -d
```

Containers:

| Container | Image | Host port |
|---|---|---|
| `simpl-sm-fuseki` | `secoresearch/fuseki:5.3.0` | 3030 |
| `simpl-sm-kafka` | `bitnamilegacy/kafka:3.3.2` | 9094 |
| `simpl-sm-kafka-ui` | `provectuslabs/kafka-ui:v0.7.2` | 9001 |
| `simpl-schema-manager` | `simpl-schema-manager:local` | 8085 |

`schema-manager` waits for `fuseki` to be healthy (Fuseki has a `wget` healthcheck on `:3030/`)
before starting.

## 4. Verify Fuseki

```bash
curl -s -u admin:admin1234 http://localhost:3030/\$/datasets | python3 -m json.tool | head -20
```

You should see four `ds.name` entries: `/ds_schemas`, `/ds_schema_metadata`, `/ds_schema_categories`,
`/ds_webhooks`. The schema-manager creates these at boot via Fuseki's admin API. If you only see
some of them, the schema-manager hit an error mid-bootstrap — check `docker logs simpl-schema-manager`.

## 5. Verify the schema-manager

```bash
curl -s -w "\nHTTP %{http_code}\n" http://localhost:8085/webhooks
```

Expected: empty array `[]` with HTTP 200. This endpoint is unauthenticated, so a non-200 response
means the service is not reachable.

```bash
curl -s -w "\nHTTP %{http_code}\n" http://localhost:8085/schemas
```

Expected: Belgif RFC-7807 problem JSON with HTTP 400 and `"detail":"Required request header
'Authorization' ..."`. This proves the controller is alive and the JWT gate is wired — it is **not**
a stack failure; it is the production auth behaviour.

## 6. Watch the logs

```bash
docker logs -f simpl-schema-manager
```

A clean startup logs the four `Dataset ... created` lines and `Started SimplSchemaManagerApplication`
within ~2 seconds.

## 7. Tear down

```bash
./stop.sh           # stop containers, keep state
./stop.sh --full    # stop containers, remove volumes
```

## Troubleshooting

### Build fails with `Could not transfer artifact eu.europa.ec.simpl:simpl-schema-versioning`

The EU GitLab Package Registry is temporarily unreachable, or the snapshot was deleted upstream.
Try again later. If persistent, browse
[https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-schema-versioning/-/packages](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-schema-versioning/-/packages)
to confirm the artifact still exists.

### Build fails with `Project version is missing` or similar

`PROJECT_RELEASE_VERSION` was not set. The `Dockerfile.local` exports it inside the builder stage; if
you're building outside Docker (`./mvnw clean package` directly against the upstream repo), prefix
the command with `PROJECT_RELEASE_VERSION=local`.

### `/webhooks` returns 503 or refuses connection

The schema-manager hasn't finished starting. Wait ~3 seconds and retry, or `docker logs
simpl-schema-manager` to see the boot output.

### Fuseki datasets are missing

The schema-manager's Fuseki client could not reach `http://fuseki:3030` from inside the container.
Check `docker logs simpl-schema-manager` for connection errors. Re-creating the stack with
`./stop.sh --full && ./start.sh` resets both containers.
