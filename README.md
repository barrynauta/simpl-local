# Simpl Catalogue — Local

A scripts-and-documentation repo for running the **Simpl-Open Federated Catalogue** locally on a Mac 
(OrbStack or Docker Desktop). 
Intended for proving modularity of the catalogue, for evaluation and debugging on a 16 GB Mac.

This repo holds **only orchestration scripts, configuration, and documentation**. 
No upstream Simpl-Open code is committed here, the actual catalogue source lives at [https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-fc-service](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-fc-service) 
and is cloned into `repos/` (gitignored) at build time. 
Same shape as the [`simpl-orchestration-local`](https://github.com/barrynauta/simpl-orchestration-local) sister project.

---

## Quick start

```bash
git clone https://github.com/barrynauta/simpl-catalogue-local.git
cd simpl-catalogue-local
./start.sh
```

First run takes 10–20 minutes (clones upstream, builds the JAR, builds the image, pulls Neo4j + Postgres, runs n10s init). Subsequent starts are under 30 seconds. When ready you'll see service URLs printed.

Test it:
```bash
curl http://localhost:8081/self-descriptions   # → {"totalCount":0,"items":[]}
curl http://localhost:8081/schemas             # → 4 default schemas
```

(Optional) Seed it with example Gaia-X self-descriptions so you have data to query:
```bash
./seed.sh
```
Loads upstream-shipped legal-person and service-offering examples. Idempotent — re-runs are no-ops unless you pass `--force`.

Stop:
```bash
./stop.sh           # preserve data + volumes
./stop.sh --full    # wipe everything (n10s init re-runs on next start)
```

---

## Status

| Phase | What | Status |
|-------|------|--------|
| 1 | Clone upstream `simpl-fc-service` | ✅ |
| 2 | Build fc-service JAR with Maven | ✅ |
| 3 | Build fc-service Docker image | ✅ |
| 4 | Start Postgres + Neo4j via compose | ✅ |
| 5 | Run fc-service container against dependencies | ✅ |
| 6 | Verify catalogue API | ✅ |
| 7 | Add catalogue UI (`simpl-catalogue-client`) | ✅ |
| 8 | Architecture diagrams + dependency map | ⏳ |

All ✅ phases verified end-to-end against upstream `simpl-fc-service` default branch on 2026-05-03 and codified into `start.sh` + `docker-compose.yml`.

---

## What this stack provides

- **fc-service** (Federated Catalogue, Spring Boot 3.5 / Java 17, runs on Java 21 JRE) on `:8081` — REST API for self-descriptions, schemas. Built from upstream source.
- **simpl-catalogue-client** (Astro + Vue UI) on `:4321` — browser UI for the catalogue. Auth disabled (empty Keycloak vars), wired directly at fc-service. Search and contract-consumption endpoints degrade gracefully (the UI's full feature set requires xfsc-advsearch-be + contract-consumption-be which we don't run locally).
- **PostgreSQL 14** on `:5432` — relational store for fc-service.
- **Neo4j 5.14.0** on `:7474` (HTTP) / `:7687` (Bolt) with APOC + GDS + n10s plugins — graph store for fc-service's RDF/semantic operations.

For an architecture diagram + per-component breakdown of what fc-service talks to (and what it intentionally doesn't), see [`docs/fc-service-architecture.md`](docs/fc-service-architecture.md).

## What this stack does NOT provide

- Production governance / policy / quality layers (Catalogue Client Service, Query Mapper Adapter, Policy Filter, Quality Validation, Schema Registry, Management Service).
- Self-description authoring backends (`sdtooling-api-be`, `sdtooling-validation-api-be`, `simpl-sd-ui`, `simpl-signer`).
- Advanced search (`xfsc-advsearch-be`).
- Contract negotiation / data plane (`simpl-edc`, `edcconnectoradapter`, `contract-consumption-be`).
- Authentication — Keycloak, Tier-1 / Tier-2 gateways are deliberately omitted. The `/participants` and `/users` endpoints return HTTP 501 because the upstream keycloak integration was removed.
- Vault, OpenBao, Kafka, NFS, ArgoCD, Helm — none of the production platform layers.
- Production-grade scaling, HA, secrets management, monitoring.

For the full Simpl-Open architecture see [simpl-programme.ec.europa.eu](https://simpl-programme.ec.europa.eu/).

---

## Prerequisites

**Software:**

| Tool | Version | Notes |
|------|---------|-------|
| Docker | 20.10+ | [OrbStack](https://orbstack.dev/) recommended on Mac (lighter than Docker Desktop) |
| Docker Compose | 2.0+ | Bundled with both OrbStack and Docker Desktop |
| JDK | 17 or 21 | Upstream POM compiles to source/target 17. Install via SDKMAN or `brew install openjdk@21` |
| Maven | 3.5.4+ | OR just use `./mvnw` — no host install needed |
| Git | 2.30+ | |

**System:**

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| RAM | 8 GB | 12 GB allocated to Docker |
| Disk | 5 GB | 10 GB (Maven cache + Docker images + Neo4j plugins) |

**Verify your setup:**

```bash
docker --version
docker compose version
java -version       # Should report 17.x or 21.x
git --version
```

---

## What `./start.sh` actually does

The same flow run as individual steps — useful for debugging or learning what's under the hood. See [`docs/fc-service-manual-setup.md`](docs/fc-service-manual-setup.md).

---

## Repository structure

```
simpl-catalogue-local/
├── README.md              This file — the walkthrough.
├── LICENSE                EUPL-1.2 (matches upstream).
├── .gitignore             Excludes repos/, build/, plans/, discovery reports, .env, .claude/.
├── docker-compose.yml     Defines the postgres + neo4j + fc-service stack.
├── start.sh               Idempotent one-shot setup (clone + build + image + up + n10s init).
├── stop.sh                Stop containers, optionally wipe volumes (--full).
├── seed.sh                (Optional) POST upstream Gaia-X example SDs to populate the catalogue.
├── .env.example           Template for local overrides (copy to .env).
├── docs/                  Per-service walkthroughs and architecture notes.
│   ├── fc-service-manual-setup.md    Manual equivalent of ./start.sh.
│   └── fc-service-architecture.md    Diagram + dependencies + process model.
└── repos/                 GITIGNORED. Upstream code, cloned by start.sh.
    └── simpl-fc-service/    Cloned from code.europa.eu in Phase 1.
```

---

## Configuration

Defaults are in `docker-compose.yml` and `start.sh`. Override by copying `.env.example` to `.env` (gitignored) and setting any of:

- `FC_SERVICE_PORT` (default 8081)
- `POSTGRES_PORT` (default 5432)
- `NEO4J_HTTP_PORT` (default 7474), `NEO4J_BOLT_PORT` (default 7687)
- `DB_USER` / `DB_PASS` (default postgres/postgres)
- `NEO4J_USER` (default neo4j) / `NEO4J_PASSWORD` (default neo12345 — Neo4j 5.14+ refuses the literal `neo4j`)

---

## Known limitations and design choices

- **No authentication.** This is a local-development stack. The `/participants` and `/users` endpoint families return HTTP 501 (`"feature disabled due to keycloak removal"`) because the upstream removed the keycloak integration without restoring auth on top. Don't run this configuration anywhere networked.
- **No automatic data seeding.** fc-service starts with an empty database. `curl /self-descriptions` returns `{"totalCount":0,"items":[]}` on a fresh stack — this is normal. Run `./seed.sh` to populate with upstream-shipped Simpl example SDs.
- **Sandbox-permissive validation.** `docker-compose.yml` sets `FEDERATED_CATALOGUE_VERIFICATION_SCHEMA=false` and `FEDERATED_CATALOGUE_VERIFICATION_SEMANTICS=false` so example SDs from upstream test fixtures can be ingested. Current upstream validators are stricter than what those fixtures comply with (and they're stricter than what `./seed.sh`'s example payloads can satisfy without hand-construction). Both verification layers must be re-enabled for any deployment ingesting untrusted SDs. Signature verification is off by default in the upstream `application.yml` and we don't change that.
- **Only one example SD seeds successfully.** `./seed.sh` currently uploads one DataOffering. A second fixture (a service-offering VP) triggers an upstream ClassCastException and is intentionally excluded — see the comment in `seed.sh`.
- **No advanced search.** `xfsc-advsearch-be` is intentionally not part of this stack. The catalogue API serves browse and basic search via fc-service directly.
- **No `/actuator/health`.** Returns 404 in current upstream because `spring-boot-starter-actuator` is not on the classpath. Use real catalogue endpoints (e.g. `curl /self-descriptions`) for liveness checks.
- **n10s graph init step is a workaround**, not the original design. The upstream `GraphDbConfig.driver()` would have run it automatically, but the class is missing `@Configuration` so it's never registered. We use `SPRING_NEO4J_*` env vars to wire Spring Boot's auto-config Neo4j driver instead, and run the n10s init manually via cypher-shell. Once upstream fixes the missing annotation, our env vars become harmless no-ops.
- **Single-node Postgres + Neo4j.** No CloudNative-PG cluster, no Neo4j HA. Local dev stack, not production-style.
- **Upstream drift.** The upstream `simpl-fc-service` is under active development and the build occasionally regresses. If a phase that previously worked stops working, re-pull `repos/simpl-fc-service` and check the upstream `CHANGELOG.md` before re-running.

---


## Service URLs

| Service | URL | Purpose |
|---------|-----|---------|
| Catalogue UI | http://localhost:4321 | Browser UI for the catalogue — browse + view SDs |
| fc-service API | http://localhost:8081 | REST API for self-descriptions and schemas |
| Neo4j Browser | http://localhost:7474 | Graph database UI (login: `neo4j` / `neo12345`) |
| Postgres | localhost:5432 | Database (user: `postgres`, db: `fed_cat`) |

---

## License

This repository: see [LICENSE](LICENSE).
Upstream catalogue components in `repos/`: see each upstream repo's own LICENSE file.
