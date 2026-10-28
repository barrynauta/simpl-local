# Architecture overview

## Containers

```
 browser ──> ui (nginx :4334) ──/v1──> backend (:8098) ──> postgres (internal only)
    │                                      │  ├──> vocabulary-stub (:8099)   GET /vocabularies/{name}/content
    └──> keycloak (:8185) <── JWKS ────────┘  ├──> mailpit SMTP (:1025, internal; HTTP API :8027)
                                              └──> webhook receivers (any http/https URL, host.docker.internal too)
```

| Service | Image or build | Role |
|---|---|---|
| `postgres` | postgres:16-alpine | Durable store for concepts, versions and webhooks (D58). Not published on the host |
| `keycloak` | quay.io/keycloak/keycloak:26.4, `start-dev --import-realm` | Realm `simpl` (D59) |
| `mailpit` | axllent/mailpit | SMTP sink with HTTP API (D60) |
| `vocabulary-stub` | `./vocabulary-stub` (Node.js) | The D60 contract, in memory |
| `backend` | `./backend` (eclipse-temurin 21 JRE, uid 10001) | The API |
| `ui` | `./ui` (nginx 1.27) | The single page application and the `/v1` proxy |

All services have healthchecks, so `docker compose up --wait` returns when the system is usable.
The backend waits for postgres, keycloak, mailpit and the stub; the UI waits for the backend.

### Token issuer

Keycloak runs with `KC_HOSTNAME=http://localhost:8185` and `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true`.
Every token therefore carries `iss=http://localhost:8185/realms/simpl`, whether it was fetched from
the host (`localhost:8185`) or from inside the network (`keycloak:8080`). The backend accepts
exactly that issuer (`{KEYCLOAK_URL}/realms/simpl`, `KEYCLOAK_URL=http://localhost:8185`) and reads
the signing keys from `KEYCLOAK_JWKS_URL=http://keycloak:8080/realms/simpl/protocol/openid-connect/certs`,
because `localhost:8185` is not reachable from inside the backend container. Both cases were
checked: a token fetched from the host and one fetched inside the network are accepted.

## Backend

Package `eu.simpl.schemamanager`:

| Package | Classes | Responsibility |
|---|---|---|
| `security` | `SecurityConfig` | Resource server, issuer and JWKS, `roles` claim to authorities, access matrix (D5), 401 and 403 as problem JSON |
| `web` | `SchemaController`, `WebhookController`, `ErrorAdvice`, `Negotiation`, `Params`, `Problems`, `Json`, `ApiException`, `Issue` | HTTP layer: D23 processing order, Accept and Content-Type rules (D8), parameter rules (D12, D13, D26, D27), problem bodies (D63) |
| `service` | `SchemaService` | Create, new version, status change, reads; slot rule (D16 to D18) inside one transaction per change |
| `validation` | `SchemaValidator`, `VocabularyClient`, `Rdf` | Syntax (Apache Jena Turtle parser), default properties, semantic checks; Vocabulary Manager client |
| `notify` | `SchemaEvent`, `ProviderMailNotifier`, `WebhookNotifier` | After-commit, asynchronous e-mail and webhook delivery |
| `persistence` | `SchemaRepository`, `WebhookRepository` | JDBC on PostgreSQL; Flyway migration `V1__init.sql` |
| `config` | `Settings`, `AppConfig` | Settings from the D60 names; HTTP client; retry scheduler |

### Data model

- `schema_concept`: one row per concept. Unique `(schema_type, name)` (D11, D12). `slot_key` is
  `RESOURCE:<resourceType>` or `SEARCH`. A partial unique index on `slot_key where status = 'PUBLISHED'`
  makes two published concepts in one slot impossible at database level.
- `schema_version`: `(concept_id, version)` with the raw bytes in `content` (`bytea`) and the
  changelog. Content is returned byte for byte.
- `slot_lock`: one row per slot. Every create, new version and status change locks its slot row
  with `SELECT ... FOR UPDATE` first, so slot checks and transitions are serialised (D16).
- `webhook`: subscriptions, with a canonical sorted event key for the duplicate check (D41).

Version status is not stored. It is derived: the highest version has the concept status, lower
versions are REVOKED (D16).

### Request flow for create and new version (D23)

1. Spring Security: 401 (no or bad token), 403 (role).
2. Controller: 406 (Accept), 415 (not multipart), 400 (fields and parameters).
3. Service: 404 (new version only), 409 pre-check (create only).
4. Validator: syntax, default properties (RESOURCE only), semantic. First failing stage stops.
   The Vocabulary Manager is called only for local references, at most once per vocabulary.
5. Transaction: lock the slot, re-check conflicts, insert, publish a `SchemaEvent` when the result
   is PUBLISHED.
6. After commit: `ProviderMailNotifier` and `WebhookNotifier` run on the async executor.

### Notifications

- E-mail: RESOURCE only, subjects and body lines of D40, sent to `PROVIDER_NOTIFICATION_EMAILS`
  from `NOTIFICATION_FROM` over `SMTP_HOST:SMTP_PORT`. A send failure is logged.
- Webhooks: both schema types. One POST per matching subscription with `X-Simpl-Event` and the
  D43 body. A non-2xx answer, connection error or 5 s timeout is retried after 1 s and then after
  5 s more, then dropped and logged.

## UI

Vue 3 with `vue-router` and `keycloak-js`. No third-party component library: native HTML
controls (select, input, file input, button) with plain CSS, so every `data-testid` sits on the
element a test interacts with.

| Route | View | Stories |
|---|---|---|
| `/schemas` | `SchemaList.vue` | SIMPL-15837, 32790, 13361, 20530, 20067 |
| `/schemas/new` | `CreateSchema.vue` | SIMPL-13355, 32800, 24172 to 24174 |
| `/schemas/{name}?schemaType=` | `SchemaDetails.vue` | SIMPL-13346, 32787, 13365, 32797 |
| `/schemas/{name}/versions/new?schemaType=` | `NewVersion.vue` | SIMPL-13358, 32808, 20770 |

The runtime configuration (`config.js`) is written at container start from `KEYCLOAK_URL` and
`KEYCLOAK_REALM`. The API base is `/v1` on the same origin, proxied by nginx to the backend, so
the browser never needs CORS. The backend still allows `CORS_ALLOWED_ORIGINS` for direct calls.
