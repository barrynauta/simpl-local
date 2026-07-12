# Infrastructure stack: architecture overview

An integrative view of the two SIMPL infrastructure-provisioning components in this
local stack: the `infrastructure-be` backend (Spring Boot, artifact `script-service`)
and the `infrastructure-fe` React SPA, plus the infra they depend on (Postgres and a
single-node Kafka). This is the above-the-component view: how they fit together, who
calls whom, and what crosses the wire. For per-component internals see:

- [`infrastructure-be-architecture.md`](infrastructure-be-architecture.md): backend, persistence, Kafka topics, config keys
- [`infrastructure-fe-architecture.md`](infrastructure-fe-architecture.md): SPA, nginx, auth flow, runtime config

## What the component does

The infrastructure-provisioning capability lets a Simpl Infrastructure Provider
offer cloud resources (VM, Kubernetes, PaaS) that a Consumer provisions on demand.
The backend stores **deployment scripts**, **cloud provisioner templates** and
**cloud environments**, and drives provisioning by exchanging request/response
messages over Kafka with an **external provisioner** (Crossplane/Terraform run via
ArgoCD, in the separate `infrastructure-crossplane` / `infrastructure-provisioner`
repos). The backend itself runs no Terraform: it is the requester/record side; the
provisioner is the executor side.

## At a glance

```mermaid
flowchart LR
    Browser["Browser"]
    Curl["curl / Bruno smoke tests"]

    subgraph net["Docker network: simpl-infra-net"]
        FE["infrastructure-fe (React SPA, nginx) :3001"]
        BE["infrastructure-be (Spring Boot) :8080"]
        PG[("Postgres :5433 - infra")]
        K[("Kafka :9092 - KRaft, PLAINTEXT")]
    end

    Provisioner["external provisioner\n(Crossplane/Terraform via ArgoCD)\nNOT in this stack"]

    Browser -->|HTTP :3001| FE
    Browser -.->|REST :8080 - API base from runtime config| BE
    Curl -->|REST :8080| BE
    BE -->|JDBC| PG
    BE -->|produce to-provision / to-decommission| K
    BE -->|consume provisioned / decommissioned| K
    K -.->|the provisioner would consume/produce here| Provisioner

    classDef hard fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px
    classDef ext fill:#eee,stroke:#999,stroke-dasharray:4 3
    class FE,BE,PG,K hard
    class Provisioner ext
```

All four containers run on a single bridge network (`simpl-infra-net`). The SPA is
served by nginx on the host at `:3001`; because it runs in the browser, its API
calls go to the host-mapped backend port `:8080`, not to a Docker hostname.
Postgres (`:5433`) and Kafka (`:9092`) are exposed on the host for inspection.

## Provisioning message flow (request and response)

The backend never talks to a cloud provider directly. `POST /scripts/trigger`
publishes a request and the provisioner (out of this stack) fulfils it and replies.

```mermaid
sequenceDiagram
    autonumber
    actor U as Client (SPA or API)
    participant BE as infrastructure-be :8080
    participant PG as Postgres
    participant K as Kafka
    participant P as Provisioner (external, not in stack)

    U->>BE: POST /scripts/trigger (deployment script + params)
    BE->>PG: persist ScriptTrigger (status: in-progress)
    BE->>K: produce to "to-provision"
    Note over K,P: In a full deployment the provisioner consumes<br/>"to-provision", runs Terraform/Crossplane, and replies.
    P-->>K: produce ArgoResponseDTO to "provisioned"
    K-->>BE: ProvisionedListener consumes "provisioned"
    BE->>PG: update ScriptTrigger (status from response)
    U->>BE: GET /scripts/triggerList (poll status)
    BE-->>U: current trigger status
```

In this local stack there is no provisioner, so `to-provision` messages are not
fulfilled. `seed.sh` and the Bruno collection instead inject a response directly
onto the `provisioned` topic to exercise the consume path.

## Kafka topics

| Topic | Direction (from BE) | Purpose |
|---|---|---|
| `to-provision` | produce | provisioning request to the provisioner |
| `provisioned` | consume | provisioning result (`ArgoResponseDTO`) |
| `to-decommission` | produce | decommission request |
| `decommissioned` | consume | decommission result |
| `notifications` | produce | user notifications |

## Trust boundaries and what is omitted

```mermaid
flowchart TB
    subgraph prod["Intended production shape"]
        GW["Tier-1 Gateway (JWT verify)"] --> BEp["infrastructure-be"]
        KCp["Keycloak"] -.-> FEp["infrastructure-fe (login)"]
        BEp --> Vault["Vault/OpenBao (secrets)"]
        BEp --> Gitea["Gitea (script storage)"]
    end
    subgraph local["This local stack"]
        BEl["infrastructure-be\nauth DISABLED in code\n(see be-findings F1)"]
        FEl["infrastructure-fe\nKeycloak URL points nowhere\n(no interactive login)"]
    end
```

Omitted on purpose: **Keycloak/Tier-1 gateway** (the backend disables auth in code,
so no token is needed; the SPA needs a Keycloak only for an interactive login),
**Vault/OpenBao** (`VaultServiceImpl` is lazy; dummy env satisfies bean binding),
**Gitea** (only exercised by script-content operations), **mailer**, and the
**ArgoCD/Crossplane provisioner** (the separate executor side). This is a
component-in-isolation stack: governance evidence that the backend runs without
ArgoCD as a prerequisite.

Consequence worth noting: because the backend has authentication disabled and the
SPA's role gate is client-side only, the role/authorisation model is currently not
enforced end to end (see `be-findings.md` F1 and `fe-findings.md` F-FE-3).
