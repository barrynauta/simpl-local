# simpl-vocabulary-manager — local evaluation stack

Runs the Simpl-Open **Vocabulary Manager** (plus its Vue 3 UI) in isolation on
a local machine, with the absolute minimum of dependencies: the Spring Boot
service, its single backing store **Apache Jena Fuseki** (RDF triplestore),
and an nginx-served UI build. Nothing else — no Keycloak, no Vault/OpenBao,
no Kafka.

> Upstream: [`simpl-vocabulary-manager`](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-vocabulary-manager) (branch `main`)
> and [`simpl-vocabulary-manager-ui`](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-vocabulary-manager-ui) (branch `release-1.0.0` —
> the UI's `main` is an empty stub; the app lives on `develop`/`release-1.0.0`).
> Both cloned into the gitignored `repos/` at start time — no upstream code is committed here.

## What the component does

Stores and serves vocabularies in Turtle format: upload internal vocabularies
(with automatic versioning), register copied external vocabularies as
validation dependencies, run semantic validation (including a bounded OWL
reasoner), and serve vocabulary content as `text/turtle`. Metadata lives in
the `ds_vocabularies` / `ds_external_vocabularies` Fuseki datasets.

For an architecture diagram (component map + upload/validation sequence) and the
production-vs-local breakdown, see
[`docs/vocabulary-manager-architecture.md`](docs/vocabulary-manager-architecture.md).

## Prerequisites

| Tool | Notes |
|------|-------|
| Docker + Compose | OrbStack or Docker Desktop |
| Git | for cloning upstream at start time |
| curl | health checks and API use |

**No Java or Maven needed on the host** — the jar is built inside a Maven
build stage (`Dockerfile.local`). The internal `simpl-semantic-validation-sdk`
dependency is pulled **anonymously** from the public code.europa.eu Maven
registry (verified working 2026-06-12); no token, no sibling checkout.

## Usage

```bash
./start.sh              # clone upstream, build image (first run ~5 min), start, smoke-test
./start.sh --seed       # same + load upstream demo vocabularies into Fuseki
./start.sh --rebuild    # force image rebuild (e.g. after upstream pull)
./stop.sh               # stop containers (add -v to wipe Fuseki data)
```

Defaults (override in `.env`):

| Service | URL | Notes |
|---------|-----|-------|
| Vocabulary Manager UI | http://localhost:4323 | Keycloak bypassed; nginx proxies `/api/` to the backend (no CORS on the backend) |
| Vocabulary Manager API | http://localhost:8086 | `GET /health`, `GET /vocabularies`, … |
| Fuseki | http://localhost:3031 | admin / admin1234 — **3031** to avoid clashing with the schema-manager stack's Fuseki on 3030 |

API surface: see `repos/simpl-vocabulary-manager/openapi/vocabulary_openapi.yaml`
and the Postman collection under `repos/simpl-vocabulary-manager/postman/`.

## Authentication — deliberately absent (by design of this stack)

As with the other simpl-local stacks, IAA is out of scope. Conveniently, the
upstream service itself performs **no signature verification**: write
endpoints require an `Authorization: Bearer <jwt>` header, from which it only
*decodes* the `email` claim (`VocabularyController`:
`JWT.decode(token).getClaim("email")` — auth0 `java-jwt`, decode ≠ verify).
Any structurally valid JWT works:

```bash
export VOCAB_TOKEN='eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6ImxvY2FsQHNpbXBsLmxvY2FsIn0.devsignature'
```

(That token is `{"alg":"HS256","typ":"JWT"}` + `{"email":"local@simpl.local"}`
with a junk signature.) In a real deployment the platform gateway/IAA sits in
front of this service; the same `JWT.decode()` pattern exists in the
schema-manager (see `../simpl-schema-manager/docs/schema-manager-bypass.md`).

### UI bypass mechanics

The UI shares the schema-manager family's `isAuthenticationEnabled()` switch
(`src/services/keycloak.ts`): empty `PUBLIC_AUTH_KEYCLOAK_*` values in
`env-config.js` make the router guard pass without any login flow. Two extra
wrinkles, both handled by our `env-config.local.js`:

1. **Role gates**: components check `GA_VOCABULARY_ADMIN` /
   `GA_VOCABULARY_VIEWER` roles read from the `token` cookie
   (`useVocabularyAccess`), and the API client forwards that cookie as the
   Bearer token. So `env-config.local.js` plants a long-lived `token` cookie
   whose payload carries `email` + `roles:["GA_VOCABULARY_ADMIN"]` (expiry
   2100-01-01).
2. **`PUBLIC_AUTH_DEV_MOCK_TOKEN` doesn't work in production builds** — it's
   behind `import.meta.env.DEV`, compiled out by Vite — hence the cookie
   approach instead.

The UI calls the API at relative `/api/`, which `nginx-ui.conf` proxies to the
backend container — necessary because the backend has no CORS configuration.

### Example: upload a vocabulary (verified working)

All four form fields are **required** (`name` PascalCase, 3–64 alphanumeric):

```bash
curl -X POST "http://localhost:8086/vocabularies" \
  -H "Authorization: Bearer $VOCAB_TOKEN" \
  -F "vocabularyFile=@samples/sample-vocabulary.ttl" \
  -F "name=SampleVocabulary" \
  -F "description=Minimal sample vocabulary for the simpl-local evaluation stack" \
  -F "changelog=Initial version"

# Roundtrip — serves the stored Turtle back:
curl http://localhost:8086/content/SampleVocabulary
```

Expect non-blocking `externalReferenceSkipped` validation warnings for the
sample's own `example.org` namespace — that's the semantic validator noting
no cached external vocabulary is registered for it, by design.

## Status

- **Verified end-to-end 2026-06-12**: `/health` UP, seed data loaded (4 demo
  vocabularies), upload with dummy JWT → HTTP 201 (`SampleVocabulary` v1
  ACTIVE), content roundtrip serves the Turtle back.
- **UI verified 2026-06-12**: renders at `:4323` with role gates open
  (admin cookie), `/api/` proxy and SPA deep links working. Gotcha fixed
  along the way: upstream production builds bake base `/vocabulary-ui/`,
  overridden to `/` in `Dockerfile.local-ui`.

## Known limitations

- **UI branch pin**: the UI's `main` branch is an empty stub upstream; this
  stack pins `release-1.0.0` (= `develop` + security dependency bumps,
  2026-06-12). When upstream finally merges to `main`, drop the branch pin in
  `start.sh`.
- **Auth**: see above — uploads attribute changes to whatever `email` claim
  the token carries (UI uploads appear as `local@simpl.local`).
- The image build pins `PROJECT_RELEASE_VERSION=0.0.1-local` (upstream reads
  the project version from the environment).
- The pinned `simpl-semantic-validation-sdk` version
  (`1.0.1-SNAPSHOT.45.1dca947d`) must remain available in the public
  registry; if upstream bumps it to a version that is not public, the build
  will fail at dependency resolution — re-check anonymity then.
