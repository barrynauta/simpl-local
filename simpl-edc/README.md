# simpl-edc — local two-agent EDC stack

Runs **two Simpl EDC connectors** (a provider and a consumer) locally and drives
a real Dataspace-Protocol exchange between them: catalogue → contract
negotiation → **MinioS3-PUSH data transfer**. Unlike the other simpl-local
stacks (each a single isolated service), this one proves two Simpl agents
actually exchanging data.

> Upstream: [`simpl-edc`](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-edc)
> — a customised Eclipse Dataspace Connector (cloned into the gitignored `repos/`
> at start time; no upstream code committed here).

See [`docs/edc-two-agent-design.md`](docs/edc-two-agent-design.md) for the full
wiring rationale, topology, and sequence diagrams.

## What the demo does

Provider holds `example-s3.txt` in `provider-bucket` (MinIO). The consumer
discovers it via the provider's DSP catalogue, negotiates a usage contract
(auto-finalizes), and triggers a `MinioS3-PUSH` transfer; the provider's data
plane copies the object into `consumer-bucket`. `start.sh` runs this end to end
and verifies the object lands.

## Prerequisites

| Tool | Notes |
|------|-------|
| Docker + Compose | OrbStack or Docker Desktop; allow ≥6 GB (two JVM connectors + 2 Postgres + MinIO) |
| Git | clones upstream at start time |
| curl, jq | drive + parse the management-API transfer flow |

**No Java/Maven on the host** — the connector jar is built in a Maven stage
(`Dockerfile.local`). **First build is slow (~10 min)** — EDC pulls a large
dependency tree.

## Usage

```bash
./start.sh                # clone, build, start both agents, run the transfer, verify
./start.sh --no-transfer  # bring the stack up but skip the transfer demo
./start.sh --rebuild      # force connector image rebuild (e.g. after upstream pull)
./stop.sh                 # stop (add -v to wipe MinIO volume + DB state)
```

| Service | URL | Notes |
|---------|-----|-------|
| Provider management API | http://localhost:19193/management | `X-Api-Key: password` |
| Consumer management API | http://localhost:29193/management | `X-Api-Key: password` |
| Provider DSP protocol | http://localhost:19194/protocol | connector-to-connector |
| MinIO S3 API | http://localhost:9000 | minioadmin / minioadmin |
| MinIO console | http://localhost:9090 | **9090** (schema-manager's Kafka UI owns 9001) |

## No auth machinery — by design (and by upstream's own local config)

Like the other stacks, the heavy dependencies are bypassed — but here every
bypass is one the **upstream `local/` config already ships** for local dev; we
inherit them rather than inventing them:

- **IAA / Keycloak / Tier-1**: the connector reads a hardcoded base64
  `mocked.agent.identity.attributes` blob (parsed by `SimplIdentityService`)
  instead of calling a real authentication provider. The real Tier-1 URL is
  commented out upstream.
- **Contract manager**: `contractmanager.extension.enabled=false` → negotiation
  auto-finalizes after a couple of seconds. No contract-manager mock (its image
  isn't anonymously pullable anyway), no manual signing curl.
- **Vault**: not wired (the pom uses `configuration-filesystem`); EDC falls back
  to its in-memory vault. The `VAULT_*` vars in the upstream `docker/.env` are
  vestigial (helm path).
- **Orchestration (Dagster)**: `transfer.extension.enabled` is flipped to
  `false` by `start.sh` so the post-transfer Dagster callback is dropped — the
  S3→S3 transfer is unaffected, keeping this stack independent of
  `simpl-orchestration`.

`start.sh` generates `config/{provider,consumer}.properties` from the upstream
files each run, rewriting only the `localhost` references to compose service
names (and the one Dagster flag). That keeps us current with upstream config
changes while staying containerized — see
[`docs/edc-two-agent-design.md`](docs/edc-two-agent-design.md) for the exact
substitution table.

## Status

- **Verified end-to-end 2026-06-13**: both connectors boot, negotiation
  `FINALIZED`, transfer `COMPLETED`, `example-s3.txt` confirmed in
  `consumer-bucket`. Two build gotchas resolved along the way: the pom needs
  `PROJECT_RELEASE_VERSION` (set in `Dockerfile.local`), and the connector must
  run on **Java 21** — the vendored Gaia-X MinIO S3 extension is compiled for 21
  and fails on a 17 runtime with `UnsupportedClassVersionError`.

## Known limitations / notes

- **First build ~10 min, large image** — the EDC Maven tree. One-time.
- **Data plane vs control plane**: for `MinioS3-PUSH` the *bytes* go
  connector→MinIO (provider data plane reads `provider-bucket`, writes
  `consumer-bucket`); only the *control plane* (catalogue/negotiation/transfer
  request) is connector→connector over DSP. Watching that split is part of what
  this stack is for (R59 data-plane-path question).
- **Config drift**: `config/*.properties` are regenerated from upstream each run
  by `start.sh`; if upstream renames a key the sed substitutions may need
  updating. They are gitignored (generated artifacts).
- The upstream repo ships `complete-minio-transfer.sh` (env-parameterized); this
  stack's `start.sh` mirrors that flow inline with container hostnames and a
  containerized `mc` verification (the upstream script's step 9 assumes a host
  `mc`).
