# Schema Manager — local stack architecture

## Component diagram

```
                        ┌──────────────────────────────┐
   curl / Bruno  ─────▶ │ schema-manager (Spring Boot) │
   (port 8085)          │  /webhooks   /schemas/...    │
                        └────┬──────────────┬──────────┘
                             │              │
              SPARQL (HTTP)  │              │ producer
                             ▼              ▼
                       ┌───────────┐   ┌──────────┐
                       │  Fuseki   │   │  Kafka   │
                       │  :3030    │   │  :9094   │
                       └───────────┘   └────┬─────┘
                                            │
                                            ▼
                                       ┌──────────┐
                                       │ Kafka UI │
                                       │  :9001   │
                                       └──────────┘
```

All four containers run on a single Docker bridge network (`simpl-schema-manager-net`).
No external auth/identity components are wired in — see "What this stack does NOT provide"
in the top-level README.

## Datasets

At boot the schema-manager calls Fuseki's admin API to create four named datasets:

| Dataset | Purpose |
|---|---|
| `ds_schemas` | Canonical schema graphs (SHACL shapes, JSON-LD contexts) |
| `ds_schema_metadata` | Schema descriptors (name, version, category, owner) |
| `ds_schema_categories` | Category taxonomy |
| `ds_webhooks` | Webhook subscriber registrations |

Confirming these exist in Fuseki is the easiest liveness check after a boot.

## Endpoints

| Path | Auth | Notes |
|---|---|---|
| `GET /webhooks` | none | Returns `[]` when no subscribers registered. Used by `start.sh` smoke test. |
| `POST /webhooks` | Tier-1 JWT | Register a webhook for schema-change events. |
| `GET /schemas` | Tier-1 JWT | List schemas. Without `Authorization` header returns Belgif RFC-7807 400. |
| `GET /schemas/{name}` | Tier-1 JWT | Fetch a single schema. |
| `GET /schemas/{name}/versions` | Tier-1 JWT | List versions of a schema. |
| `GET /schemas/{name}/{version}` | Tier-1 JWT | Fetch a specific version. |

## Production vs. local

| Concern | Production | Local |
|---|---|---|
| Auth | Keycloak → Tier-1 → Tier-2 → schema-manager | Bypassed; auth-gated endpoints return 400 |
| Fuseki credentials | OpenBao secret | Plain env var (`admin1234`) |
| Kafka transport | `SASL_SSL` | `PLAINTEXT` |
| Fuseki persistence | PVC | Ephemeral (datasets re-created at each boot) |
| Image source | Pre-built JAR from GitLab CI, single-stage `Dockerfile` | Source-built JAR via multi-stage `Dockerfile.local` |
| Version | Set by GitLab CI pipeline | `PROJECT_RELEASE_VERSION=local` |
