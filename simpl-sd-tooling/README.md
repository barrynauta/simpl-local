# simpl-sd-tooling — SD Tooling local stack

Local-evaluation stack for the **SD Tooling component** of Simpl-Open: the
Creation Wizard a provider uses to author, validate, sign, and publish
**Self-Descriptions** (Resource Descriptions) for data, application, and
infrastructure offerings.

**Status: verified end-to-end 2026-07-06** — schemas served (3 seeded), UI
renders with SHACL-generated wizard forms (8 steps from `DataSchema.ttl`),
landing list + BFF wiring green, and the full API authoring loop green:
`/v3/selfDescriptions/enriched` (two real SHACL validations + stub round-trips)
→ `/v1/selfDescriptions/finalized` (stub-VC signing) →
`/v1/selfDescriptions/publications` (stub catalogue, XFSC-shaped response).
Policy builders (`/v1/policies/access`, `identityAttributes`, `actions`)
return real ODRL policies fed by the stubbed participant identity.

## What runs

| Service | Source | Port (host) | Role |
|---|---|---|---|
| `sd-ui` | [`simpl-sd-ui`](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-sd-ui) (main) | 4324 | Astro SSR wizard UI. The browser only calls same-origin `/api/*`; the Node server (BFF) forwards to the API. |
| `sdtooling-api` | [`sdtooling-api-be`](https://code.europa.eu/simpl/simpl-open/development/data1/sdtooling-api-be) (main) | 8087 | Creation Wizard API: schemas, policies, resource addresses, enrich, finalize (sign), publish. Spring Boot 3 / Java 21, no datastore. |
| `sdtooling-validation` | [`sdtooling-validation-api-be`](https://code.europa.eu/simpl/simpl-open/development/data1/sdtooling-validation-api-be) (main) | 8088 | Stateless SHACL / JSON-schema validator (Jena + TopBraid in-process). No auth, no backing services. |
| `peer-stubs` | WireMock 3.9.2 | 8089 | Stands in for every peer the API calls: authentication-provider, connector-adapter, asset-orchestrator, vc-issuer, catalogue-adapter, federated-catalogue, and the UI-side signer. |

Both backends are descendants of the Eclipse XFSC `sd-creation-wizard-api` /
`sd-validation-api` projects.

## Quick start

```
./start.sh            # clone, seed schemas, build, up, smoke-test
./start.sh --rebuild  # force image rebuilds after an upstream pull
./stop.sh             # stop, keep volumes
./stop.sh --full      # remove containers, volumes, and local images
```

First run: ~10-15 min (Maven dependency tree for two Spring Boot services +
npm install / astro build for the UI). Subsequent starts are seconds.

Open **http://localhost:4324** — no login (auth disabled, see below). The
wizard's schema picker should list the three seeded schemas (Application,
Data, Infra).

API smoke checks:

```
curl -s http://localhost:8087/v2/schemas | jq .            # 3 seeded schemas
curl -s http://localhost:8087/status                        # api liveness
curl -s http://localhost:8089/__admin/requests | jq .       # what hit the stubs
```

## Design decisions (matches the simpl-local conventions)

- **Auth deliberately omitted.**
  - API: `WEB_MVC_BEARER_TOKEN_REQUIRED=false` (upstream switch). Upstream
    never verifies the JWT signature anyway; it decodes and forwards the
    bearer to downstream services (the known Simpl pass-through pattern).
  - UI: the three `PUBLIC_AUTH_KEYCLOAK_*` vars are empty, which flips the
    upstream `isAuthenticationEnabled()` switch — no login, no redirect.
  - Validation API: has no auth at all upstream.
- **No Vault.** The only secrets (`VC_ISSUER_API_KEY`, `VC_ISSUER_CLIENT_ID`)
  are plain env vars; they must exist for the API to boot but their values
  are ignored by the WireMock vc-issuer.
- **Static schema supply, no schema-sync-adapter.** Production fills the
  schema directory via the schema-sync-adapter (which needs Postgres, Kafka,
  and IAA). The API only ever reads flat `<Name>.ttl` + `<Name>.json` pairs
  from a directory, so `start.sh` copies the sample schemas shipped in the
  api-be repo into `./schemas/`, mounted at `/data/schemas`. To test schema
  changes, drop your own pair into `./schemas/` and restart the api container.
- **All peers stubbed with one WireMock.** The enrich/finalize pipeline calls
  connector-adapter and continues on its *response*, so the register stubs
  echo the submitted SD back via response templating (`$.sdJson` for v2) and
  inject the `simpl:edcConnector` / `simpl:edcRegistration` objects the real
  adapter would add (the final SHACL validation requires them). The vc-issuer
  stub wraps the SD in a syntactically valid but unsigned VC.
- **Throwaway keypair for the tier2 clients.** The catalogue-adapter and
  federated-catalogue Feign clients bootstrap an mTLS SimplClient by fetching
  the participant's private key and certificate chain from the
  authentication-provider before every call. `gen-auth-stubs.sh` (run by
  `start.sh`) generates a self-signed RSA keypair and embeds it in a
  gitignored WireMock mapping — the material only has to parse; over the
  stack's plain-HTTP wiring it is never used in a TLS handshake.

## What works vs what is stubbed

**Real** (actual upstream code executing):
- Schema listing and content serving from the seeded TTL/JSON pairs.
- The full wizard UI: SHACL-driven form generation (`@simpl/vue-components`),
  resource-address templates and UI-schemas (baked into the api-be jar).
- SHACL validation of the authored SD against the schema, and JSON-schema
  validation of resource addresses — the whole validation service is real.
- The enrich pipeline in the api-be: offering type, sharing method,
  participant id injection, hash generation, id/identifier/version stamping,
  final SHACL validation.

**Stubbed** (WireMock, so only the protocol is exercised, not the semantics):
- Connector-adapter registration (EDC asset creation does not happen — pair
  with the `simpl-edc` stack if you want that for real).
- Workflow registration in the asset-orchestrator (see `simpl-orchestration`
  for the real Dagster stack).
- VC issuance/signing: the returned VC's `proof` is fake; nothing validates
  it downstream in this stack.
- Catalogue publish and the resource-descriptions list (the UI landing list
  shows one canned entry). Two catalogue read paths use the api-be's own
  built-in mock flags instead of WireMock.
- Participant identity (`did:web:sd-local-participant`) and identity
  attributes for the access-policy builder.

## Known limitations

- `PUBLIC_DEPLOYMENT_SCRIPT_UPLOAD_URL` is unset; the deployment-script flow
  (application offerings) will fail if exercised. No stub exists upstream or
  here.
- The UI's mock switch is baked at image build time (off); rebuilding with
  `--build-arg MOCK_APIS=true` gives a zero-backend demo UI instead. Upstream
  gotcha: any non-empty `USE_MOCK_APIS` (including the string `false`) enables
  mocks, so the Dockerfile only exports the variable when the arg is exactly
  `true`.
- Stub response shapes were derived from the Feign clients, the UI's own mock
  services, and the XFSC catalogue DTOs; if upstream reshapes a response the
  stub needs the same change. `curl http://localhost:8089/__admin/requests`
  shows near-misses (unmatched requests) when debugging.
- Upstream `main` moves fast (activity as recent as 2026-07-06); `start.sh`
  pulls on every run, so a previously green stack can break after an upstream
  change. Pin by checking out a tag inside `repos/<repo>` if you need
  stability.

## Architecture

See [`docs/architecture.md`](./docs/architecture.md) for the component
diagram, the enrich/finalize sequence, the schema supply path, and the full
stub inventory with the reasoning behind each response shape.
