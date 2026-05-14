# fc-service — manual setup walkthrough

This walkthrough is the manual equivalent of `./start.sh`. Use it when you want to understand each step, debug a failure mid-way, or run the steps individually. For a one-shot setup, see the [main README](../README.md#quick-start).

After completing the steps below you will have:

- `fc-service` running on `:8081` against `postgres` (`:5432`) and `neo4j` (`:7474` / `:7687`)
- 4 default schemas loaded
- An empty self-descriptions list ready to populate

---

## 1. Clone the upstream catalogue source

```bash
mkdir -p repos
git clone https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-fc-service.git repos/simpl-fc-service
```

`repos/` is gitignored, so the cloned upstream code is never committed back to this repo.

## 2. Build the fc-service JAR

```bash
cd repos/simpl-fc-service
./mvnw clean install -DskipTests
cd ../..
```

First run: 5–15 minutes (downloads ~300 MB of Maven dependencies). Subsequent builds: under a minute. Output: `repos/simpl-fc-service/fc-service-server/target/fc-service-server-1.3.0-SNAPSHOT.jar`. Tests skipped because they spin up an embedded Postgres + Neo4j harness; run separately with `./mvnw test` from the upstream repo if you want them.

## 3. Build the fc-service Docker image

```bash
cd repos/simpl-fc-service
docker build -t simpl-fc-service:local .
cd ../..
```

Uses the upstream's top-level `Dockerfile`. ~1–2 minutes (mostly the `eclipse-temurin:21-jre` base image pull on first run).

## 4. Start the stack

```bash
docker compose up -d
```

Brings up `postgres`, `neo4j`, and `fc-service` per this repo's `docker-compose.yml`. Postgres is healthy in seconds; Neo4j takes ~40s on first run while it downloads the apoc + graph-data-science + n10s plugins.

## 5. Initialise n10s in Neo4j (workaround for an upstream wiring bug)

```bash
docker exec simpl-cat-neo4j cypher-shell -u neo4j -p neo12345 \
  "CALL n10s.graphconfig.init({handleVocabUris:'MAP',handleMultival:'ARRAY',multivalPropList:['http://w3id.org/gaia-x/service#claimsGraphUri']});"

docker exec simpl-cat-neo4j cypher-shell -u neo4j -p neo12345 \
  "CREATE CONSTRAINT n10s_unique_uri IF NOT EXISTS FOR (r:Resource) REQUIRE r.uri IS UNIQUE;"
```

Both commands are idempotent — re-running is safe. The n10s configuration is persisted in the Neo4j data volume, so it survives container restarts. Only re-run after `./stop.sh --full`.

## 6. Verify

```bash
curl http://localhost:8081/self-descriptions   # → {"totalCount":0,"items":[]}
curl http://localhost:8081/schemas             # → 4 default schemas (3 ontologies + 1 SHACL shape)
```

Both should return HTTP 200 with the bodies above on a fresh stack. Empty self-descriptions list is expected — no data is seeded.

---

For limitations of this stack and the next phases (catalogue UI, advanced search, architecture diagrams) see the [main README](../README.md).
