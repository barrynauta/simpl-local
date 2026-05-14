# Simpl-Local

A monorepo of **local-evaluation stacks for Simpl-Open components**, designed to prove component modularity by running each in isolation on a single 16 GB Mac (OrbStack or Docker Desktop). Each subdirectory is a self-contained scripts-and-documentation project that clones the relevant upstream source from [`code.europa.eu/simpl`](https://code.europa.eu/simpl) at build time, builds it, and runs it under Docker Compose against only the dependencies that component genuinely needs.

No upstream Simpl-Open code is committed to this repo. Each subproject's `start.sh` clones what it needs into a gitignored `repos/` directory.

---

## Subprojects

| Folder | Component | Status | What it does |
|--------|-----------|--------|--------------|
| [`simpl-catalogue/`](./simpl-catalogue/README.md) | Federated Catalogue (`simpl-fc-service` + `simpl-catalogue-client` UI + `poc-gaia-edc` query-mapper-adapter) | QMA-backed quick search + basic advanced search verified 2026-05-03 | Local catalogue with REST API, browser UI, and access-policy-aware search via QMA. Backed by Postgres + Neo4j. Full advanced search (`xfsc-advsearch-be`), full SD lifecycle, and contract negotiation are out of scope — see subproject README. |
| [`simpl-notification-service/`](./simpl-notification-service/README.md) | Notification Service | Local stack runs (email path verified 2026-05-05) — **upstream component assessed FAIL** (technical review 2026-05-08, see subproject README) | Spring Boot Kafka consumer that dispatches emails. Comes with Kafka, Kafka UI, and Mailpit for SMTP capture. Upstream issues: SMS channel is an unimplemented stub with a fake test (`assertTrue(true)`), and the Kafka transport adds integration burden disproportionate to what is effectively a simple SMTP relay — replacement or full rework recommended before any producer integrates. |
| [`simpl-orchestration/`](./simpl-orchestration/README.md) | Orchestration Platform (Dagster) | Demonstration stack | Local Dagster-based orchestration stack with seed data and a Bruno collection for exploring the pipeline. |

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
│   └── docs/                         Per-service architecture + manual-setup docs.
├── simpl-notification-service/       Notification Service local stack.
│   ├── README.md                     Walkthrough, status, architectural observations.
│   ├── docker-compose.yml
│   ├── start.sh, stop.sh
│   └── docs/                         Architecture diagram + manual-setup walkthrough.
└── simpl-orchestration/              Orchestration Platform (Dagster) local stack.
    ├── README.md                     Purpose, quickstart, Dagster walkthrough.
    ├── docker-compose.yml
    ├── start.sh
    ├── dagster/, dagster-patches/    Dagster runtime and local patches.
    ├── seed/                         Seed data for the sample pipeline.
    ├── bruno/                        Bruno API collection for exploration.
    └── scripts/
```

Each subproject was previously a standalone repository (`simpl-catalogue-local`, `simpl-notification-service-local`, `simpl-orchestration-local`) and was absorbed into this monorepo via `git subtree add`, preserving full commit history. The original repositories on GitHub have been archived.

---

## License

EUPL-1.2 — matches upstream Simpl-Open licensing. See `LICENSE` in each subproject directory.

Upstream components cloned into each subproject's `repos/` directory at build time retain their own upstream licenses.

---

## Related

- **Simpl-Open programme:** [simpl-programme.ec.europa.eu](https://simpl-programme.ec.europa.eu/)
- **Upstream source:** [`code.europa.eu/simpl`](https://code.europa.eu/simpl)
- **Functional and Technical Architecture Specifications:** [FTA on code.europa.eu](https://code.europa.eu/simpl/simpl-open/architecture/-/blob/master/functional_and_technical_architecture_specifications/Functional-and-Technical-Architecture-Specifications.md)
