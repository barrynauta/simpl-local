# simpl-files - Simpl-Files local stack

Local-evaluation stack for the **Simpl-Files component** of Simpl-Open: a stock
nginx static file server that hosts the **contract and SLA templates** a
**provider** references (by URL) when authoring a Self-Description through
[SD Tooling](../simpl-sd-tooling/README.md). Tier-2, Provider-Agent leaf.

**Status: verified standalone 2026-07-21** - one nginx container, **zero
dependencies** (no database, no OpenBao, no Kafka, no ArgoCD). `./start.sh`
builds and runs it; `curl /status` returns `{"status":"UP"}` and
`curl /static/contract/ContractTemplate1.json` returns the Gaia-X/SHACL contract
template. This is the polar opposite of the Common Components App-of-Apps: the
component is trivially modular; only its packaging and docs pretend otherwise.

## What runs

| Service | Source | Port (host) | Role |
|---|---|---|---|
| `simpl-files` | [`data1/simpl-files`](https://code.europa.eu/simpl/simpl-open/development/data1/simpl-files) (main) | 8085 | `nginxinc/nginx-unprivileged` serving static JSON + PDF from `/home/nginx/files` (baked into the image). No auth, no user input, no dynamic content, no backing services. |

Served content (all read-only, baked in): `/static/contract/ContractTemplate{1,2,3}.json`,
`/static/pdf/*.pdf` (BillingSchema1/2, SLAAgreement1/2, ContractTemplate1/2,
Dataset_Contract_Template), and `/status`.

## Quick start

```
./start.sh            # clone, docker build, run, smoke-test
./stop.sh             # remove the container
./stop.sh --full      # also remove the local image and the cloned repo
```

First run: under a minute (nginx base pull + copy the files). Smoke checks:

```
curl -s http://localhost:8085/status                                  # {"status":"UP"}
curl -s http://localhost:8085/static/contract/ContractTemplate1.json  # JSON-LD template
```

## Design decisions (matches the simpl-local conventions)

- **Docker only. No Helm needed for a local install.** The whole component is
  nginx plus a `files/` folder that is copied into the image. `docker run` is the
  complete local form. The repo also ships a Helm chart (Deployment / Service /
  Ingress), but that is the Kubernetes / agent-embedded deployment form, not a
  local requirement. We ran the Helm chart once against a cluster to confirm how
  it slots into a Provider Agent (it comes up as a single pod with no
  dependencies); for evaluation the container is enough and simpler.
- **The shipped documentation is ignored on purpose.** Both the "Installation
  Guide" and the "Deployment Guide" tell you to
  `docker run -v $(pwd)/files:/usr/share/nginx/html`, but this image serves from
  `/home/nginx/files` (per its Dockerfile and `nginx.conf`), and the files are
  already baked into the image, so that volume mount is both wrong-path and
  redundant. Neither guide walks through the Helm chart. The authoritative source
  is the Dockerfile + `nginx.conf` + `charts/`, which is what `start.sh` follows.
- **No auth, no TLS, no ingress locally.** The Helm chart's `ingress` (hardcoded
  `files.dev.simpl-europe.eu`, `clusterIssuer: dev-prod`, TLS secret) has no
  enable toggle, but the Docker path skips it entirely and serves plain HTTP on a
  local port.

## Upstream gotchas / findings

- **Chart is not self-installable.** `charts/Chart.yaml` has
  `version: ${PROJECT_RELEASE_VERSION}` (invalid semver) and `values.yaml` has
  `image.repository: ${CI_REGISTRY_IMAGE}` / `tag: ${PROJECT_RELEASE_VERSION}`.
  These are CI substitutions, so `helm install` off the raw repo fails without
  manual patching (same `${...}` family as the SD-Tooling poms).
- **Ingress has no `enabled` flag** and hardcodes a dev domain + cluster-issuer.
- **Listed as a flat "Mandatory Component"** in the installation guide, with no
  description and no link to the README that explains it, and mis-scoped: only a
  **provider** running SD-Tooling needs it. A Governance Authority or a
  consumer-only agent does not.
- **Guide taxonomy is undefined and inconsistent** across the deliverable set:
  "Deployment Guide" means local `docker run` here but cluster Helm/ArgoCD for
  Common Components, and the "Installation Guide" duplicates the docker steps and
  bolts on an unused Helm-values table.

## Architecture observation (webserver vs database, attack surface)

A static file server is the right shape for "documents addressable by stable
URL", which is what a Self-Description needs when it references a contract
template: a database would add an API, a schema, and a connection pool to serve
what is fundamentally read-only static content. So webserver over database is not
the issue.

The open question is why this is a **separate, publicly-exposed, per-provider**
service at all. The templates shipped here are generic (ContractTemplate1..3,
SLAAgreement1..2, BillingSchema1..2), not provider-specific, and SD-Tooling
already bundles its static schemas directly. Exposing an identical standard
template set on a public ingress on every provider multiplies public surface for
content that could be bundled into the tooling or centrally hosted by the
Governance Authority. The nginx itself is low-risk (static, read-only, no input),
so the concern is not the server but the deployment pattern, which sits in
tension with the architecture's own least-privilege / minimize-attack-surface
principles. The architecture documentation offers no rationale beyond "thin
planned component ... exposes JSON and PDF files via HTTP." See the audit journey
in the knowledge base for the fuller write-up.
