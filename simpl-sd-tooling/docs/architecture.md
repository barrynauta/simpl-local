# simpl-sd-tooling — architecture

## Component view

```
                 browser
                    │  http://localhost:4324  (only same-origin calls)
                    ▼
 ┌──────────────────────────────────────────┐
 │ sd-ui (Astro SSR, Node 22, :4322)        │
 │  pages/api/* = BFF routes                │
 │  auth OFF: PUBLIC_AUTH_KEYCLOAK_* empty  │
 └──────┬──────────────────────┬────────────┘
        │ PUBLIC_CREATION_     │ PUBLIC_SIGNER_URL /
        │ WIZARD_API_URL       │ PUBLIC_ASSET_ORCHESTRATOR_API_URL
        ▼                      ▼
 ┌─────────────────────┐   ┌─────────────────────────────────────┐
 │ sdtooling-api       │   │ peer-stubs (WireMock, :8089 host)   │
 │ (Spring Boot, :8087)│──▶│  /tier1/v2/*      auth-provider     │
 │  no DB, no broker   │   │  /v1|v2/selfDescriptions/enriched   │
 │  bearer NOT required│   │                   connector-adapter │
 └──────┬──────────────┘   │  /v1/workflowDefinitions, /v1/      │
        │ VALIDATION_       │   workflows*     asset-orchestrator│
        │ SERVICE_URL       │  /v1/credentials/issue  vc-issuer  │
        ▼                  │  /adapter/*      catalogue-adapter  │
 ┌─────────────────────┐   │  /fc/*           federated-catalogue│
 │ sdtooling-validation│   │  /v1/credential  signer (UI-side)   │
 │ (Spring Boot, :8088)│   └─────────────────────────────────────┘
 │  SHACL/Jena+topbraid│
 │  in-process, no auth│        ./schemas ──volume──▶ /data/schemas
 └─────────────────────┘        (seeded from api-be's data/schemas)
```

## Schema supply path

Production: Governance Authority's **schema-manager** → **schema-sync-adapter**
(REST pull + webhook push, Quartz re-sync every 900s; requires Postgres, Kafka,
and IAA) → shared NFS volume → api-be reads files.

The api-be never calls the schema-manager; it reads flat
`<Name>.ttl` (SHACL content) + `<Name>.json` (metadata: name, version,
resourceType, status) pairs from `schema-sync-service.repository-path`
(`SCHEMA_SYNC_SERVICE_REPOSITORY_PATH`). So this stack replaces the whole sync
chain with a host directory seeded from the sample pairs the api-be repo ships
(`data/schemas/{Application,Data,Infra}Schema.{ttl,json}`). That is the same
trick the schema-sync-adapter itself uses for its preload feature.

Consequence: live publish/revoke propagation from a schema-manager is out of
scope here. The `simpl-schema-manager` stack remains the place to exercise the
GA-side schema lifecycle.

## Enrich / finalize sequence (v3, what the wizard drives)

```
UI BFF ──POST /v3/selfDescriptions/enriched?schemaId=──▶ sdtooling-api
  1. load schema metadata+content            [./schemas volume]
  2. load resource-address template schema   [classpath, baked into jar]
  3. validate resource address               [sdtooling-validation  REAL]
  4. set offeringType / sharingMethod
  5. set participantId                       [stub: GET /tier1/v2/participant]
  6. generate hashes
  7. register with connector-adapter         [stub: POST /v2/selfDescriptions/enriched
                                              — response BECOMES the SD, so the
                                              stub echoes $.sd back (templating)]
  8. register workflow                       [stub: POST /v1/workflowDefinitions]
  9. strip providerDataAddress, set metadata, @id (local:did:<uuid>),
     identifier, version
 10. validate final SD against schema        [sdtooling-validation  REAL]
finalize (POST /v1/selfDescriptions/finalized) = all of the above, then:
 11. sign via vc-issuer                      [stub: POST /v1/credentials/issue
                                              — wraps SD in fake VC]
publish (POST /v1/selfDescriptions/publications):
 12. federated catalogue create              [stub: POST /fc/self-descriptions
                                              — XFSC-shaped response]
```

Step 7 is the reason the connector-adapter stubs use WireMock response
templating rather than canned bodies: everything after it operates on the
*response*, so a static body would corrupt the SD mid-pipeline. The stub also
injects `simpl:edcConnector` and `simpl:edcRegistration` (verified required by
`DataSchema.ttl`'s `EdcConnectorShape` / `EdcRegistrationShape`) because the
real adapter adds them and step 10 rejects an SD without them.

## Tier2 client mTLS bootstrap (publish + list paths)

The two tier2 Feign clients do not simply call their URL: on every call
`Tier2ClientDefaultConfig` builds a SimplClient whose keystore comes from the
authentication-provider —

```
GET /tier1/v2/keypairs/active     → {"privateKey": "<PEM>"}   (BouncyCastle-parsed)
GET /tier1/v2/credentials/active  → {"content": "<PEM chain>"}
GET /tier1/v2/ephemeralProof      → {"proof": "..."}
```

Without these, publish/list fail with a misleading gson `MalformedJsonException`
(it tries to parse WireMock's 404 page as the keypair JSON). `gen-auth-stubs.sh`
generates a throwaway self-signed RSA keypair into a gitignored mapping. The
key material only needs to parse into a PKCS12 keystore; with `http://` tier2
URLs no TLS handshake ever uses it.

## Catalogue read paths

Three different mechanisms, deliberately:

| Call | Mechanism |
|---|---|
| list resource descriptions (`GET /adapter/participants/resourceDescriptions`) | WireMock (canned single entry) — upstream has **no** built-in mock for this one |
| versions of an RD | api-be built-in mock aspect (`CATALOGUE_ADAPTER_MOCK_GET_ALL_RESOURCE_DESCRIPTIONS_BY_VERSION=true`, upstream default) |
| revoke an RD | api-be built-in mock aspect (`CATALOGUE_ADAPTER_MOCK_REVOKE_RESOURCE_DESCRIPTION=true`, upstream default is false — flipped here so revoke never tries the real tier2 gateway) |

The tier2 gateway URLs (`catalogue-adapter.tier2-gateway.url`,
`federated-catalogue.tier2-gateway.url`) point at WireMock with the upstream
default path prefixes (`/adapter`, `/fc`) kept, so stub paths read like the
real gateway routes.

## Auth model

- **UI**: upstream OIDC code+PKCE against Keycloak, implemented in the BFF
  with HttpOnly cookies. Empty `PUBLIC_AUTH_KEYCLOAK_{SERVER_URL,REALM,CLIENT_ID}`
  → `isAuthenticationEnabled()` false → middleware short-circuits. The BFF
  then forwards `Authorization: Bearer undefined` downstream, which is fine
  because:
- **api-be**: `web.mvc.bearer-token.required=false` disables the token gate.
  Upstream never cryptographically verifies the token anyway (decode +
  forward), consistent with the other simpl-local stacks' findings.
- **validation-api**: no security configuration exists upstream at all; it
  relies on network placement. Worth noting as an upstream observation: it
  trusts whatever reaches it.

## Build notes

- Both backends: no Maven wrapper in the repos; built with the `maven:3.9`
  image. `CI_API_V4_URL` must be defined (GitLab-CI-only variable interpolated
  in the pom's registry URL) — same gotcha as the simpl-contract stack.
- `sdtooling-api-be` depends on `eu.europa.ec.simpl:simpl-schema-versioning:1.0.0-SNAPSHOT`
  — a SNAPSHOT from the group 4086 registry. If anonymous snapshot resolution
  fails, set `GITLAB_TOKEN` in `.env`.
- The validation repo's own Dockerfile expects a CI-generated
  `resolved_settings.xml` that is not in the repo; `Dockerfile.local-validation`
  replaces it.
- UI: `@simpl/vue-components` resolves from the code.europa.eu npm registry
  (anonymous works for public packages). `USE_MOCK_APIS` is inlined by Vite at
  build time — a build arg, not runtime env. The `PUBLIC_*` URLs by contrast
  are read from `process.env` per request (`src/util/getEnv.ts`), so they are
  plain compose environment.

## Port map (host)

| Port | Service |
|---|---|
| 4324 | sd-ui |
| 8087 | sdtooling-api |
| 8088 | sdtooling-validation |
| 8089 | peer-stubs (WireMock admin at `/__admin/`) |
