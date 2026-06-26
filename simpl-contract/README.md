# simpl-contract — local evaluation stack

Local Docker stack for the **Contract** component of Simpl-Open's Contract-Billing
concern: the `contract` backend service plus the `contract-ui` front-end, wired
together in the same style as the other `simpl-local/` stacks.

> **Read this first — component status is mixed.**
> The **backend is real and runnable**; the **UI is a non-integrated shell**.
> See [Component status](#component-status) and [Known limitations](#known-limitations).

---

## What this is

| Part | Upstream | Branch | Tech | Status here |
|---|---|---|---|---|
| `contract` (backend) | `…/contract-billing/contract` | `main` | Spring Boot / Java 21 (WebFlux, JPA, Kafka, Liquibase) | **Boots & serves** — Postgres + Kafka stood up locally |
| `contract-ui` (frontend) | `…/contract-billing/contract-ui` | **`develop`** | React 19 + Vite + TypeScript | **Renders only** — not wired to the backend |

The backend *"enhances contract management between DataSpace participants —
storage, consultation and updating of signed contracts, additional negotiation
steps, and monitoring/enforcing contract-defined resource usage"* (its README).
It is the runtime service behind contract establishment (R17 family).

## Architecture

See [`docs/architecture.md`](docs/architecture.md) for the component context,
runtime dependency graph, boot-vs-functional dependencies, the Kafka topic map,
and the UI integration gap.

---

## Prerequisites

- **Docker** (Desktop or OrbStack) + `docker compose`
- **git**, **curl**
- **A code.europa.eu Personal Access Token** (scope `read_api`) *if* the backend's
  `simpl-common:3.3.0` dependency is not anonymously readable from the Simpl-Open
  GitLab Package Registry (group 4086). Put it in `.env` as `GITLAB_TOKEN=`.
  The build tries anonymously first and only needs the token if that 401s.

~16 GB RAM is comfortable; the first backend build downloads the Maven
dependency tree (several minutes).

---

## Quick start

```bash
cp .env.example .env        # then edit GITLAB_TOKEN / ports if needed
./start.sh --seed           # clone, build, run, and seed a sample agreement
                            # (--rebuild forces image rebuilds; --seed is opt-in)
```

Then:

```bash
curl http://localhost:8086/contract/v1/health        # backend liveness (expect 200, UP)
open http://localhost:4323                            # UI — read-path demo (loads the seeded agreement)
open http://localhost:9002                            # Kafka UI
```

**Read-path demo:** with data seeded (`./seed.sh`, or `start.sh --seed`), the UI
loads agreement `11111111-1111-1111-1111-111111111111` and renders its fields;
the id box loads any other agreement. The browser sends **no API key** — nginx
injects `X-Api-Key` and same-origins the call (see `nginx.conf`).

Exercise the backend API against its OpenAPI contract:
`repos/contract/openapi/openapi3-v1.yaml`.

Stop:

```bash
./stop.sh           # keep data
./stop.sh --full    # also drop volumes
```

### Ports (override in `.env`)

| Service | Default |
|---|---|
| Contract API | 8086 |
| Contract UI (shell) | 4323 |
| Postgres | 5433 |
| Kafka broker | 9095 |
| Kafka UI | 9002 |

---

## Component status

**Backend — works.** Runs the env-driven `default` Spring profile (not the
localhost-hardcoded `application-local.yaml`). Health only checks diskspace +
db, so the service reports UP once Postgres is reachable; Kafka is stood up so
the ~20 consumer/producer topics connect cleanly.

**UI — shell only.** `contract-ui` on `develop` is ~2,200 LOC but almost none of
it is contract-specific:
- it was scaffolded by copying the **Monitoring** front-end (`package.json`
  name is still `monitoring-reporting-fe`; the HTTP client still sends the
  Kibana header `kbn-xsrf`);
- the contract domain model is a single field (`Contract = { id: string }`);
- `httpClient` is defined but **never called** — there is **no contract API
  base URL** anywhere, so the UI makes **no calls to the backend**;
- upstream, the only configured backend is **Keycloak** with no auth-disable
  switch; this stack adds a configurable one via the [`ui-overlay/`](ui-overlay/)
  (**`VITE_PUBLIC_AUTH_MODE`**, default `disabled` here) so the UI renders
  without any login.

Upstream, the UI image only renders placeholders. **This stack adds a
read-path integration** (via [`ui-overlay/`](ui-overlay/)): a real
`ContractViewPage` that calls `GET /contract/v1/agreements/{id}` and renders the
agreement. With a seeded row (`./seed.sh`) you get a genuine
UI → nginx → backend → Postgres round-trip.

### Create-contract-data round-trip (`./drive-create.sh`)

A real Kafka round-trip that exercises backend logic without the full
dependency mesh. `drive-create.sh` produces a `create-contract-request` (as the
EDC connector would); the backend consumes it, calls the **catalogue stub**
(`catalogue-stub/`, WireMock), renders the human-readable contract HTML + a
SHA-256 hash, and produces a `create-contract-response` (errorCode 0) which the
script reads back. Proves consumer → `CatalogConnector` → `ContractHtmlRenderer`
→ hash → producer end to end.

**Still out of scope:** persisting/signing an agreement. That happens only in
the *sign* flow (`SignContractRequestConsumer`), which is triggered by the EDC
connector and mandatorily calls signer (`/v1/documents/sign`) + vc-issuer +
**wallet** (`/v1/wallets`) + catalogue. `wallet-service` has code only on
`develop`; the shipped `stubs` service is out of sync with the backend (its
signer path `/v1/credential` ≠ the backend's `/v1/documents/sign`, and it has no
vc/wallet stubs). So a real create/sign needs the whole subsystem + a simulated
connector — see `docs/contract-ui-stub-findings.md` context.

---

## Known limitations

1. **No front-to-back flow.** The UI has no integration code (see above). The
   nginx config already proxies `/contract/*` → backend, so the wiring is ready
   the day SC-1 adds API calls — but today nothing exercises it.
2. **UI auth is configurable (`VITE_PUBLIC_AUTH_MODE`).** This stack adds the
   switch contract-ui lacks (see [`ui-overlay/`](ui-overlay/)):
   - `disabled` (default here) → no Keycloak, static local identity, no login.
   - `keycloak` → upstream OIDC/PKCE against the `VITE_PUBLIC_AUTH_KEYCLOAK_*`
     target. Note that mode points at the **remote** dev sandbox (`participant`
     realm, `frontend-cli`), needs a sandbox account, **and** the app hardcodes
     its redirect URI to `http://localhost:3001/` — so a real login won't
     complete against this stack's `:4323` serving port without further changes.
   Change the value in `.env` and rebuild (`--rebuild`).
3. **Backend functional flows need stubs.** Boot needs only Postgres + Kafka.
   Sign / verify / negotiate / search flows call a signer, a VC-issuer, an EDC
   connector, and the catalogue — wired here to inert `stub.invalid` URLs.
   Point them at real stubs (or your `simpl-local/simpl-edc` stack for the EDC
   leg) to drive those paths.
4. **Backend build needs registry access.** `simpl-common:3.3.0` comes from the
   EU GitLab registry; see Prerequisites.
5. **Auth & Vault deliberately omitted**, consistent with the other
   `simpl-local/` stacks.

---

## Verification status

The stack files were authored from the verified upstream configuration
(`application.yaml`, `pom.xml`, `contract-ui` source) and the proven
`simpl-schema-manager` pattern. They have **not yet been run end-to-end on this
machine** (the backend build requires EU-registry network access). Treat the
first `./start.sh` as the real integration test; the backend-build registry
step is the most likely first failure point — the script prints the exact
remediation if it 401s.
