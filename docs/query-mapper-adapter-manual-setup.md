# query-mapper-adapter — manual setup walkthrough

This walkthrough is the manual equivalent of the QMA steps inside `./start.sh`. Use it when you want to understand each step, debug a failure, or run the steps individually. For a one-shot setup, see the [main README](../README.md#quick-start).

After completing the steps below you will have:

- `query-mapper-adapter` running on `:8084` (context path `/v1`) proxying to `fc-service`
- Quick search (`GET /v1/selfDescriptions?searchString=...`) returning results with access-policy filtering

---

## 1. Clone the upstream source

```bash
mkdir -p repos
git clone https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/poc-gaia-edc.git repos/poc-gaia-edc
```

`repos/` is gitignored.

## 2. Build the JAR

```bash
cd repos/poc-gaia-edc
PROJECT_RELEASE_VERSION=local ./mvnw clean install -DskipTests
cd ../..
```

Output: `repos/poc-gaia-edc/target/adapter-local.jar`. The `PROJECT_RELEASE_VERSION` variable sets the JAR version string; any non-empty value works. Tests are skipped because they depend on a running fc-service; run with `./mvnw test` separately if needed.

## 3. Build the Docker image

```bash
cd repos/poc-gaia-edc
docker build -t query-mapper-adapter:local .
cd ../..
```

Uses the upstream's top-level `Dockerfile`. Output: `query-mapper-adapter:local`.

## 4. Start the stack

`docker-compose.yml` includes the `query-mapper-adapter` service. If the full stack is not yet up:

```bash
docker compose up -d
```

If the stack is already up and you only want to start QMA:

```bash
docker compose up -d query-mapper-adapter
```

The service depends on `fc-service` being reachable on the `simpl-cat-net` network.

## 5. Verify

```bash
# Quick search — returns access-policy-filtered results
curl -s "http://localhost:8084/v1/selfDescriptions?searchString=simpl&page=1&pageSize=5"
# → {"totalCount":N,"items":[...]}

# Advanced search — requires at least one property filter
curl -s -X POST http://localhost:8084/v1/selfDescriptions/advancedSearch \
  -H "Content-Type: application/json" \
  -d '{"filters":[{"property":"simpl:name","value":"aefaf"}]}'
# → {"totalCount":N,"items":[...]}
```

Both should return HTTP 200. An empty catalogue (`totalCount: 0`) is normal on a fresh stack — run `./seed.sh` to populate it.

---

## Seed data note

`seed.sh` patches upstream example SDs before uploading them. Two fields in `simpl:servicePolicy` contain upstream placeholder strings (`swvwe`, `wefwwfe`) that cause fc-service's `QuickSearchService` to crash with a `JsonParseException` when QMA triggers a quick search. The seed script replaces them with valid ODRL JSON granting `CONSUMER` access. See the comments in `seed.sh` for details.

---

For limitations and the broader stack see the [main README](../README.md).
