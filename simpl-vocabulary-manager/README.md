# simpl-vocabulary-manager — local evaluation stack

Runs the Simpl-Open **Vocabulary Manager** in isolation on a local machine,
with the absolute minimum of dependencies: the Spring Boot service plus its
single backing store, **Apache Jena Fuseki** (RDF triplestore). Nothing else —
no Keycloak, no Vault/OpenBao, no Kafka, no UI.

> Upstream: [`simpl/simpl-open/development/gaia-x-edc/simpl-vocabulary-manager`](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-vocabulary-manager)
> (cloned into the gitignored `repos/` at start time — no upstream code is committed here).

## What the component does

Stores and serves vocabularies in Turtle format: upload internal vocabularies
(with automatic versioning), register copied external vocabularies as
validation dependencies, run semantic validation (including a bounded OWL
reasoner), and serve vocabulary content as `text/turtle`. Metadata lives in
the `ds_vocabularies` / `ds_external_vocabularies` Fuseki datasets.

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

## Known limitations

- **UI**: the upstream `simpl-vocabulary-manager-ui` repository is an empty
  stub (LICENSE + README only) — there is no UI to run; this stack is
  API-only by necessity, not by choice.
- **Auth**: see above — uploads attribute changes to whatever `email` claim
  you put in the token.
- The image build pins `PROJECT_RELEASE_VERSION=0.0.1-local` (upstream reads
  the project version from the environment).
- The pinned `simpl-semantic-validation-sdk` version
  (`1.0.1-SNAPSHOT.45.1dca947d`) must remain available in the public
  registry; if upstream bumps it to a version that is not public, the build
  will fail at dependency resolution — re-check anonymity then.
