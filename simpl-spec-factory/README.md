# simpl-spec-factory

An experiment: how far can we get generating a complete Simpl-Open component **from its specifications alone**? Not just the code, but the full test suite and the documentation too. In other words, a software factory driven by specs.

Status: **run 1 done** (2026-10-27): the generated build passes 74 of 74 black-box API scenarios (upstream: 58 of 74). UI: 22 of 22 browser scenarios (upstream: 9 of 22). See [`spec-digest.md`](./spec-digest.md) and [`findings.md`](./findings.md). Candidate: Vocabulary Manager. Input set: [`specs/`](./specs/README.md).

**Personal test project.** This is a personal experiment, for testing purposes only. It is not commissioned by, endorsed by or speaking for the European Commission, the Simpl programme or any of its contractors. **Purpose:** see how feasible spec-driven generation is, and learn which parts of the specs could be made more precise. It is not meant to show what is wrong with Simpl. Every gap found is a pointer to where the specs can be improved.

---

## The question

Simpl-Open has user stories, acceptance criteria, functional requirements and API definitions for its components. If those specs are good enough, a generator (Claude Code, driven by a repeatable prompt pipeline) should be able to produce a working component that behaves like the upstream one. Where it cannot, the gaps tell us something about the specs.

So the experiment answers two things at once:

1. **The factory:** how good is spec-driven generation of code, tests and docs today?
2. **The specs:** are Simpl's specifications complete and precise enough to build from?

## The goal

The end result is **one service, or a small combination of services, including a UI**, that runs with a plain:

```
docker compose up
```

No manual setup steps, no clone-and-patch scripts, no external accounts. Seed data and mocks are part of the compose file.

## An honest assessment

This is an assessment, not a demo. The write-up reports what actually happened:

- what the factory got right first time, and what needed human correction (and how much);
- where the specs were missing, ambiguous or contradicted the upstream behaviour;
- what does not work at the end, stated plainly;
- no cherry-picking: every run is kept in `findings.md`, including the failed ones.

A result that shows large gaps in the specs is a valid and useful outcome, as long as the evidence shows it: it tells us where to invest.

## Inputs

| Input | Source | Notes |
|-------|--------|-------|
| User stories + acceptance criteria | Jira (SIMPL project) | Primary behavioural source |
| Functional requirements | FTA, component functional descriptions | What the component is for |
| API definitions | OpenAPI specs in the upstream repo | The contract to match |
| Existing tests | Upstream repo | **Use with care, maybe not at all.** They describe what the code does, not what the spec says, so they can carry the implementation's bugs into the generated result. If used, keep them out of generation and use them only as a check afterwards. |

The upstream **source code is not an input**. Generation works from specs only.

## Outputs

- **Code:** the backend service(s) and the UI, started together with `docker compose up`.
- **Unit tests.**
- **Functional tests in Gherkin** (Given/When/Then), derived from the acceptance criteria, with step definitions.
- **Integration tests with mocks** for every external dependency (Kafka, Keycloak, other Simpl services).
- **Documentation:** README, architecture overview, API docs, and how to run it.
- **A traceability matrix:** each acceptance criterion linked to the code and tests that cover it.

## Choosing the candidate

A good candidate is reasonably isolated and well specified:

- clear user stories with acceptance criteria;
- an OpenAPI definition;
- a UI that belongs to it;
- few dependencies on other Simpl services;
- already running locally in this repo, so the generated version can be compared side by side with upstream.

Stacks here that may fit: `simpl-vocabulary-manager` (backend + UI, one backing service: Fuseki), `simpl-schema-manager` (backend + UI, adds Kafka), `simpl-sd-tooling` (UI + two backends, no datastore, peers mocked). `simpl-notification-service` has no UI and `simpl-files` is too small to say much.

### Candidate scan (2026-10-27)

| Component | Live Jira stories | With Given/When/Then ACs | OpenAPI | Backing services | UI |
|-----------|-------------------|--------------------------|---------|-----------------|----|
| **Vocabulary Manager** | 35 (3 cancelled) | 33 | `vocabulary_openapi.yaml`, 13 paths | Fuseki | yes |
| Schema Manager | 55 (36 cancelled) | most | `schema_openapi.yaml`, 9 paths | Fuseki + Kafka | yes |
| SD Tooling | 32 (15 cancelled) | most | 4 specs across 2 backends, v1 to v3 | none, but many peers to mock | yes |
| Contract | 44 (11 cancelled) | all | `openapi3-v1.yaml` | Postgres + Kafka, signer, wallet, EDC | stub upstream |

**Pick: Vocabulary Manager.** Each feature comes as a pair of stories, one "via API" and one "via UI" (create, new version, status, validate syntax, validate semantics, history, filter and sort, RBAC). That gives the backend and the UI their own specs. The stories link to the R07.02 tech description, the BP02B functional description and a Figma design. Status: 24 "Ready for DG Acceptance", 10 "ready for implementation check" (mostly UI), so not everything is built upstream yet.

Known gaps to check: the `/external-vocabularies` endpoints seem to have no story of their own; RBAC needs a mocked identity in place of Keycloak.

Natural second step: add Schema Manager on the same Fuseki (the vocabulary stories say "use same persistence layer as Schema Management").

## Approach

**Run 1: Spring Boot + Vue 3**, the same stack as upstream, so size, complexity and behaviour can be compared directly.

Rules for the factory, to keep the result honest:

1. **Test writer and code writer are separate.** One agent writes the tests from `specs/` only; another writes the code from `specs/` only. Neither sees the other's output before the tests run. Otherwise the tests check what was built instead of what was specified.
2. **One black-box test suite** at the API and UI level (Gherkin scenarios with HTTP and browser steps), independent of the implementation language. It runs against the generated build **and** against upstream. Unit tests are per implementation.
3. **Spec decisions are logged.** Where the specs are silent or contradict each other (for example R07.02 draft API vs OpenAPI), the factory picks one, and the choice goes into `findings.md` as a spec gap.
4. **Contamination check.** Upstream is public, so the model may have seen it. After the run, compare naming and structure with upstream; strong similarity means we measured memory, not specs.

**Run 2 (optional): a second, independent stack**, built from the same `specs/` without seeing run 1, against the same black-box suite. Where both builds pass, the spec was clear; where they differ on the same criterion, the spec left room. Only worth doing if run 1 shows enough ambiguity.

## How we judge the result

- Does `docker compose up` on a clean machine give a working service and UI?
- Do the generated tests pass against the generated code?
- Do they also pass against the **upstream** component? Differences are either generator errors or spec ambiguities, and both are findings.
- How many acceptance criteria are covered, and how many could not be generated because the spec was unclear or missing?
- How much human correction was needed, and where?

## Layout (planned)

```
specs/       collected input specs, as fetched (stories, ACs, FRs, OpenAPI)
generated/   the factory output: code, tests, docs
pipeline/    prompts and scripts that drive the generation, so a run can be repeated
findings.md  what worked, what did not, spec gaps found
```
