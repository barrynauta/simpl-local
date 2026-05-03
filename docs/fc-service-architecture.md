# fc-service — architecture overview

A short reference for what `fc-service` is, what it talks to in our local stack, and what it would talk to in a fuller deployment.

## At a glance

```mermaid
flowchart LR
    Client["HTTP client<br/>(curl / browser / SDK)"] -->|REST :8081| FC

    subgraph net["Docker network: simpl-cat-net"]
        FC["fc-service<br/>Spring Boot 3.5<br/>Java 17 (21 JRE)<br/>:8081"]
        PG[("postgres:14<br/>db: fed_cat<br/>:5432")]
        N4J[("neo4j:5.14<br/>+ apoc, gds, n10s<br/>:7474 HTTP, :7687 Bolt")]
        FC -->|JDBC<br/>SPRING_DATASOURCE_*| PG
        FC -->|Bolt<br/>SPRING_NEO4J_*| N4J
    end

    FC -.->|disabled<br/>SCHEMA_MANAGER_SUBSCRIPTION_ENABLED=false| SM["schema-manager<br/>(not deployed locally)"]
    FC -.->|on-demand<br/>during SD verification| DID["uniresolver.io<br/>(external DID resolver)"]
    FC -.->|scheduled fetch<br/>(fails gracefully)| GTA["registry.lab.gaia-x.eu<br/>(external trust anchor)"]

    classDef hard fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px
    classDef soft fill:#fff3e0,stroke:#ef6c00,stroke-dasharray:5 5
    class PG,N4J hard
    class SM,DID,GTA soft
```

Solid arrows = hard dependencies (must be reachable for fc-service to function). Dashed arrows = soft / optional / external (fc-service starts and serves without them, with reduced behaviour).

## What runs in our local stack

| Component | Image | Port(s) | Purpose |
|---|---|---|---|
| `fc-service` | `simpl-fc-service:local` (built from upstream source) | `8081` | REST API for self-descriptions and schemas. Spring Boot 3.5, compiled to Java 17 bytecode, runs on Java 21 JRE in the container. |
| `postgres` | `postgres:14` | `5432` | Relational store. Holds SD payloads (`sdfiles`), schema files (`schemafiles`), validator caches (`validatorcache`), Liquibase changesets, and other internal state. |
| `neo4j` | `neo4j:5.14.0` with APOC + Graph Data Science + neosemantics (n10s) plugins | `7474` (HTTP browser) / `7687` (Bolt protocol) | Graph store. Holds the RDF graph parsed from each self-description. n10s handles the TTL/RDF ↔ property-graph conversion. GDS provides graph algorithms. APOC provides general-purpose procedures. |

All three live on a single bridge network `simpl-cat-net`. fc-service reaches its dependencies via container hostnames (`postgres:5432`, `neo4j:7687`).

## Why two databases?

| Concern | Where it lives | Why |
|---|---|---|
| SD payloads + binary content | PostgreSQL | Append-mostly, transactional, easy to back up. fc-service uses Hibernate/JPA + Liquibase migrations on top. |
| Parsed semantic graph of each SD | Neo4j | Self-descriptions are RDF graphs by design (Gaia-X uses SHACL shapes + Turtle ontologies). Storing them as a property graph makes graph queries (relationships between providers, services, claims) cheap. Storing them as relational rows would mean re-implementing graph traversal in SQL. |

Postgres is the source of truth for the document; Neo4j is the queryable view of its semantic content. They're populated together when an SD is published.

## What's intentionally NOT here

These are referenced by the `application.yml` defaults and would be wired in for a richer deployment, but our local stack disables or ignores them:

| Component | Status here | What it would do |
|---|---|---|
| `schema-manager` | Disabled via `SCHEMA_MANAGER_SUBSCRIPTION_ENABLED=false` | Push schema lifecycle events to fc-service so the catalogue picks up new/updated SHACL shapes without a restart. |
| External trust anchor (`registry.lab.gaia-x.eu`) | Fetched on schedule, fails gracefully | Provides the Gaia-X X.509 chain used to validate SD signatures. fc-service falls back to local defaults when unreachable — this causes the 8 noisy Jena ERROR log lines at startup. |
| DID resolver (`dev.uniresolver.io`) | Contacted on-demand during SD signature verification | Resolves DIDs to DID Documents to fetch the verification key. Only needed if you publish signed SDs. |
| CES publisher / subscriber | Both `impl: none` (default) | A NATS-backed event stream for federation-wide SD propagation between catalogues. Off in single-node setups. |
| Authentication (Keycloak, Tier-1 / Tier-2 gateways) | Not deployed | In production, fc-service sits behind a gateway that does AuthN/AuthZ. fc-service itself runs unauthenticated in the current upstream — see the Authentication note in the main README. |

## Configuration that matters in our stack

The `fc-service` container needs four groups of env vars (set by `docker-compose.yml`):

1. **Datasource** — `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`. Standard Spring Boot wiring to `postgres:5432`.
2. **Graph store** — `SPRING_NEO4J_URI`, `SPRING_NEO4J_AUTHENTICATION_USERNAME`, `SPRING_NEO4J_AUTHENTICATION_PASSWORD`. Wires Spring Boot's auto-config Neo4j driver to `neo4j:7687`. (`GRAPHSTORE_*` variants of the same values are also set; they kick in once the upstream wiring bug is patched, see the n10s init step in the manual setup walkthrough.)
3. **Subscription off-switch** — `SCHEMA_MANAGER_SUBSCRIPTION_ENABLED=false`. Prevents fc-service from trying to subscribe to a schema-manager at a Kubernetes-style hostname that doesn't exist in our network.
4. **Telemetry off-switch** — `OTEL_SDK_DISABLED=true`. fc-service's OpenTelemetry SDK defaults to off, but we set it explicitly so log noise is bounded.

## Process model

fc-service is a single Spring Boot process (Tomcat embedded, default 8081). On startup it:

1. Connects to PostgreSQL via Hikari pool, runs Liquibase migrations (11 changesets on a fresh DB), idles.
2. Hibernate / Spring Data JPA initialises (no JPA repos defined; no Spring Data Neo4j repos either — graph access is via the Neo4j Java driver directly).
3. Tomcat starts on `:8081`.
4. ~10 seconds later, a scheduled task loads the 4 default schemas from the JAR's `defaultschema/ontology/` and `defaultschema/shacl/` resources into PostgreSQL. (This is what produces the noisy Jena ERROR lines — see the manual setup walkthrough for the explanation.)
5. Tomcat serves requests.

When an SD is published (`POST /self-descriptions`), the SD payload is stored in PostgreSQL AND parsed into the Neo4j graph in the same transaction. Both stores stay in sync.

## See also

- [Manual setup walkthrough](fc-service-manual-setup.md) — step-by-step build and run.
- [Main README](../README.md) — quick-start, status, and limitations.
