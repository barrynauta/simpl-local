# Catalogue: third factory candidate

Status: **run 1 built** (2026-10-28). The generated build passes 458 of 459 black-box API scenarios. The local upstream stack (May 2026): 55 of 459, with federation, client backend, quality and UI skipped. UI browser steps and swap tests are not done yet. Details: [`findings.md`](./findings.md).

## Why this candidate

Chosen as the biggest one: the whole catalogue, with three backends (catalogue-be, query mapper adapter, catalogue client backend), a UI and federated search across data spaces. Its core has no user stories; the 33 live stories describe 2026 features.

## Layout

| Path | What |
|---|---|
| [`specs/`](./specs/README.md) | Frozen input set and story index |
| [`spec-digest.md`](./spec-digest.md) | Step 1: component map, scope, decisions D1 to D83, notes N1 to N13, environment contract |
| [`tests/`](./tests/README.md) | Step 2: 459 API and 26 UI scenarios (behave, requests), signing helper, fixtures, reports |
| [`pipeline/`](./pipeline/) | The briefs for steps 1 to 3 as used |
| [`findings.md`](./findings.md) | Findings per step |

The build itself lives outside the repository for now (`spec-factory-build-cat`, compose project `simpl-catalogue-factory`), with its own `TRACEABILITY.md` and `BUILD-LOG.md`.
