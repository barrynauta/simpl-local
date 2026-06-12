# Simpl-Local

A monorepo of **local-evaluation stacks for Simpl-Open components**, designed to prove component modularity by running each in isolation on a single 16 GB Mac (OrbStack or Docker Desktop). Each subdirectory is a self-contained scripts-and-documentation project that clones the relevant upstream source from [`code.europa.eu/simpl`](https://code.europa.eu/simpl) at build time, builds it, and runs it under Docker Compose against only the dependencies that component genuinely needs.

No upstream Simpl-Open code is committed to this repo. Each subproject's `start.sh` clones what it needs into a gitignored `repos/` directory.

---

## Subprojects

| Folder | Component | Status | What it does |
|--------|-----------|--------|--------------|
| [`simpl-catalogue/`](./simpl-catalogue/README.md) | Federated Catalogue (`simpl-fc-service` + `simpl-catalogue-client` UI + `poc-gaia-edc` query-mapper-adapter) | QMA-backed quick search + basic advanced search verified 2026-05-03 | Local catalogue with REST API, browser UI, and access-policy-aware search via QMA. Backed by Postgres + Neo4j. Full advanced search (`xfsc-advsearch-be`), full SD lifecycle, and contract negotiation are out of scope — see subproject README. |
| [`simpl-notification-service/`](./simpl-notification-service/README.md) | Notification Service | Process runs; **email path non-functional (re-verified empty 2026-05-15)** — the consumer config hardcodes `SASL_PLAINTEXT`/`PLAIN` and crashes against any plain broker. Upstream component **assessed FAIL** (SMS-channel stub, Kafka transport disproportionate to the use case, hardcoded-SASL consumer; see subproject README). | Spring Boot Kafka consumer that *intends* to dispatch emails but cannot in any documented local configuration. Comes with Kafka, Kafka UI, and Mailpit for SMTP capture — Mailpit stays empty because the consumer crashes at startup. To watch the email path end-to-end including the schema-manager Mailinator-leak demonstration, use [`simpl-schema-manager/`](./simpl-schema-manager/README.md) with `./start.sh --with-notifications` (its broker is SASL-configured to satisfy the hardcoded consumer expectations). |
| [`simpl-orchestration/`](./simpl-orchestration/README.md) | Orchestration Platform (Dagster) | Demonstration stack | Local Dagster-based orchestration stack with seed data and a Bruno collection for exploring the pipeline. |
| [`simpl-vocabulary-manager/`](./simpl-vocabulary-manager/README.md) | Vocabulary Manager (`simpl-vocabulary-manager`) | Verified end-to-end 2026-06-12 (health, seed, JWT-free upload 201, content roundtrip) | Spring Boot REST service managing Turtle vocabularies (versioned uploads, external-vocabulary registration, semantic validation incl. bounded OWL reasoning). Single backing service: Apache Jena Fuseki on host port 3031 (clash-free next to the schema-manager stack). **No auth machinery needed** — upstream write endpoints `JWT.decode()` the Bearer token without signature verification, so a static dummy token suffices. Upstream UI repo is an empty stub, so API-only. The internal `simpl-semantic-validation-sdk` Maven dependency resolves anonymously from the public code.europa.eu registry. |
| [`simpl-schema-manager/`](./simpl-schema-manager/README.md) | Schema Manager + UI (`simpl-schema-manager`, `simpl-schema-manager-ui`) | Local stack + UI verified 2026-05-15; bruno 12/12 ✓ — **upstream notification recipient defaults to a public Mailinator inbox; every schema lifecycle event leaks audit trail there** (see subproject README) | Spring Boot REST service + Vue 3 UI managing JSON-LD/SHACL schemas, versions, and webhook subscribers. Comes with Apache Jena Fuseki (RDF triplestore), Kafka, and Kafka UI. Auto-creates four Fuseki datasets at boot. **Keycloak deliberately bypassed** via empty UI env vars (built-in `isAuthenticationEnabled()` switch) + nginx auth-header injection of a hand-crafted JWT (backend uses `JWT.decode()`, no signature verification). Mechanics in [`docs/schema-manager-bypass.md`](./simpl-schema-manager/docs/schema-manager-bypass.md). |

*Status dates are per-stack verification dates against upstream `main` at that time. Matrix last reviewed: 2026-06-12.*

---

## What each component is for

Short summaries drawn from the [Simpl-Open Functional and Technical Architecture
Specifications](https://code.europa.eu/simpl/simpl-open/architecture/-/blob/master/functional_and_technical_architecture_specifications/Functional-and-Technical-Architecture-Specifications.md)
(FTA) so this index page also serves as a quick reference for *what each upstream
component is supposed to do* — not just what the local stack reproduces.

### `simpl-catalogue` — Federated Catalogue Service

The Governance Authority's central publication point for signed **Self-Descriptions**
— structured metadata documents describing every dataset, application, or
infrastructure offering in the data space. Providers publish a Self-Description; the
catalogue indexes it, validates it against the active schemas, and makes it searchable
to consumers.

Internally the Catalogue is several services: a database of persisted
Self-Descriptions, a **Search Engine** that indexes them, a **Vocabulary Datastore**
loaded with ontologies and schemas, a **Management Service** for lifecycle operations
(e.g. revocation), and three validation layers (Syntax, Semantic/SHACL, and Quality
Rule). Searches go through the **Query Mapper Adapter**, which embeds policy-based
filtering via the **Policy Filter Service** so users only ever see results their
permissions allow.

The local stack runs the `simpl-fc-service` backend, the `simpl-catalogue-client` UI,
and the `poc-gaia-edc` query-mapper-adapter against Postgres + Neo4j. Schemas the
catalogue validates against come from the Schema Management Service (see below).

### `simpl-schema-manager` — Schema Management Service

The **Metadata Description building block** of the Simpl architecture. The Governance
Authority uses it to define *the structure that every Self-Description in the data
space must conform to*: which properties exist, what data types they take, what
constraints apply, which controlled vocabularies are valid.

The schemas are **serialised as Turtle (`.ttl`) files** containing **SHACL shapes**
and **ontology fragments** in RDF. These TTL schemas drive three things across the
federation:

1. **UI form generation** — the SD Tooling and the catalogue's advanced-search UI
   automatically generate fields from the published schemas, so a participant filling
   in a Self-Description sees exactly the fields the schema demands.
2. **Validation** — the catalogue's Semantic Validation Service uses the same SHACL
   shapes to validate Self-Descriptions before publication; anything that doesn't
   conform is rejected.
3. **Federation sync** — the Schema Synch Service propagates updates from the
   Governance Authority's schema-manager to every Provider Node, so every participant
   always works against the current schema standards.

The schema-manager backend persists the TTL graphs into an Apache Jena Fuseki RDF
triplestore (four datasets: `ds_schemas`, `ds_schema_metadata`, `ds_schema_categories`,
`ds_webhooks`) and broadcasts schema lifecycle events (create / new version / publish
/ revoke) over **Kafka** to subscribed webhook consumers and to the email
notification-service. The TTL files in `simpl-schema-manager/samples/` are copies of
the upstream test suite's canonical valid SHACL shapes — drop one into the UI's
"Upload schema" form to exercise the full validate-and-publish loop.

### `simpl-orchestration` — Data Orchestration Service

The **Orchestration Platform** executes **Data Workflows** — multi-step pipelines
that process or pre-process data using custom or built-in services (data
anonymisation, transformation, validation, etc.). When a consumer acquires a data
offering, the workflow associated with that offering is what actually produces the
data the consumer receives.

The platform is built on **Dagster** with a few Simpl-specific wrappers:

- **Orchestration Engine** (Dagster Daemons) — schedules, sensors, run queueing;
  long-running daemons that coordinate workflow execution.
- **Orchestration Run Worker** (K8sRunLauncher) — launches each workflow run as a
  Kubernetes job.
- **Orchestration Management UI** (Dagit) — Dagster's browser console for inspecting
  pipelines, runs, logs, and asset materialisations.
- **Orchestration Engine API** — Dagster's GraphQL API; the programmatic interface
  used by the Asset Orchestrator and other components.
- **Asset Orchestrator** — a Simpl-developed component on top that bridges the
  catalogue's data/application offerings to Dagster workflows.
- **Repository (Gitea)** — version-controlled workflow source code, so every change
  to a job graph, op, resource config, or schedule is captured as a commit with
  author, timestamp, and diff.
- **Auth Proxy** — sidecar that integrates the platform with the IAA stack without
  coupling Dagster directly to Keycloak.

The local stack runs Dagster + Asset Orchestrator + seed pipelines so the workflow
loop is explorable without the IAA + Gitea environment.

---

## Why this exists

Simpl-Open is delivered as an integrated bundle (`common-deployer`) that brings up dozens of components, Kafka, Keycloak, Vault, ArgoCD, and Helm in a single orchestrated start. That bundle is appropriate for full agent deployment but unhelpful when the question is *"does component X work in isolation?"* Each subproject here answers that question for one component, with the smallest dependency footprint that makes the component functional.

The design principles each subproject follows:

- **No ArgoCD, no Helm, no Vault as per-component prerequisites.** Kubernetes is an acceptable substrate; the production orchestration layer is not.
- **Authentication is deliberately omitted** when the component does not require it. Keycloak and Tier-1/Tier-2 gateways are out of scope for modularity proofs.
- **Secrets are plain environment variables.** No HashiCorp Vault, no OpenBao.
- **Defaults match upstream where possible**; deviations are documented in each subproject's "Known limitations" section.

---

## Quick start

Each subproject has its own `start.sh`. Pick the component you want to evaluate:

```
git clone https://github.com/barrynauta/simpl-local.git
cd simpl-local

cd simpl-catalogue && ./start.sh             # Federated Catalogue stack
# or
cd simpl-notification-service && ./start.sh  # Notification Service stack
# or
cd simpl-orchestration && ./start.sh         # Dagster orchestration stack
# or
cd simpl-schema-manager && ./start.sh        # Schema Manager (Fuseki + Kafka) stack
```

First runs take 8–20 minutes per component (Maven dependency download + Docker image builds). Subsequent starts are under 30 seconds. Each subproject's README has component-specific verification steps.

To stop:

```
cd <subproject> && ./stop.sh          # preserve volumes
cd <subproject> && ./stop.sh --full   # wipe everything
```

---

## Prerequisites

Common to all subprojects:

| Tool | Version | Notes |
|------|---------|-------|
| Docker | 20.10+ | [OrbStack](https://orbstack.dev/) recommended on Mac |
| Docker Compose | 2.0+ | Bundled with OrbStack and Docker Desktop |
| Java JDK | 17 or 21 | Per subproject — `simpl-catalogue` compiles to 17, `simpl-notification-service` requires 21 |
| Git | 2.30+ | |

System minimums vary per subproject. Allocate at least 8 GB to Docker if running `simpl-catalogue`; 4 GB suffices for `simpl-notification-service`. See each subproject's README for exact recommendations.

---

## Repository structure

```
simpl-local/
├── README.md                         This file — umbrella index.
├── simpl-catalogue/                  Federated Catalogue local stack.
│   ├── README.md                     Walkthrough, status, architecture observations.
│   ├── docker-compose.yml
│   ├── start.sh, stop.sh, seed.sh
│   ├── docs/                         Per-service architecture + manual-setup docs.
│   └── bruno/                        Bruno HTTP smoke-test collection.
├── simpl-notification-service/       Notification Service local stack.
│   ├── README.md                     Walkthrough, status, architectural observations.
│   ├── docker-compose.yml
│   ├── start.sh, stop.sh
│   └── docs/                         Architecture diagram + manual-setup walkthrough.
├── simpl-orchestration/              Orchestration Platform (Dagster) local stack.
│   ├── README.md                     Purpose, quickstart, Dagster walkthrough.
│   ├── docker-compose.yml
│   ├── start.sh
│   ├── dagster/, dagster-patches/    Dagster runtime and local patches.
│   ├── seed/                         Seed data for the sample pipeline.
│   ├── bruno/                        Bruno API collection for exploration.
│   └── scripts/
└── simpl-schema-manager/             Schema Manager local stack.
    ├── README.md                     Walkthrough, status, smoke test.
    ├── docker-compose.yml, Dockerfile.local
    ├── start.sh, stop.sh
    └── docs/                         Architecture diagram + manual-setup walkthrough.
```

Each subproject was previously a standalone repository (`simpl-catalogue-local`, `simpl-notification-service-local`, `simpl-orchestration-local`) and was absorbed into this monorepo via `git subtree add`, preserving full commit history. The original repositories on GitHub have been archived. `simpl-schema-manager/` was added directly into the monorepo (no prior standalone repo).

---

## License

EUPL-1.2 — matches upstream Simpl-Open licensing. See `LICENSE` in each subproject directory.

Upstream components cloned into each subproject's `repos/` directory at build time retain their own upstream licenses.

---

## Related

- **Simpl-Open programme:** [simpl-programme.ec.europa.eu](https://simpl-programme.ec.europa.eu/)
- **Upstream source:** [`code.europa.eu/simpl`](https://code.europa.eu/simpl)
- **Architecture (canonical since June 2026):** [`foundations/architecture` on code.europa.eu](https://code.europa.eu/simpl/simpl-open/foundations/architecture/-/blob/main/functional_and_technical_architecture_specifications/Functional-and-Technical-Architecture-Specifications.md) — the FTA document is being sunset; this repo is becoming the main source for architectural information. (Old `simpl/simpl-open/architecture` project is being archived.)
