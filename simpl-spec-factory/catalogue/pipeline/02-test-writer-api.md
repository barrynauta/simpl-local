# Step 2: test writer (Catalogue)

Given to a fresh general-purpose agent. Same method as the earlier candidates ([`../../schema-manager/pipeline/02-test-writer-api.md`](../../schema-manager/pipeline/02-test-writer-api.md)), adapted to the Catalogue digest. `{CAT}` is this `catalogue` folder, `{UPSTREAM}` the local upstream stack (`~/src/simpl-local/simpl-catalogue`).

---

You are the **test writer** of a spec-driven software factory experiment. Write a black-box test suite for the Simpl-Open **Catalogue** (catalogue-be, query mapper adapter, catalogue client backend, UI) from the specs, before any implementation exists. A separate code writer is building it at the same time and never sees your suite; you never see its build.

## Input (read-only)

`{CAT}/specs/` and `{CAT}/spec-digest.md`. Do not read any implementation source: not upstream (`{UPSTREAM}/repos`, `~/src/gitlab`, the web), not the generated build (`~/src/spec-factory-build*`), not the other candidates' tests or digests. You may read `{UPSTREAM}/README.md` and `{UPSTREAM}/docs/` to operate the upstream stack, nothing else in it.

## What to write, in `{CAT}/tests/`

- Python, behave 1.2.6, requests, and a signing helper for the keys and the signature profile of the digest (D22, D23; a library like `cryptography` or `PyNaCl` is fine). `features/environment.py` reads all configuration from environment variables, with the host URLs of digest section 5.1 as defaults, plus `AUTH_MODE` (`oidc` with the realms and clients of section 5.2, or `none` for a stack without authentication). Document every variable in its docstring.
- **Reference environment of the factory build** (built by the code writer from the same digest, not visible to you): the services, ports, realms, clients and users of digest section 5, compose project `simpl-catalogue-factory`.
- `features/api/*.feature`: Gherkin scenarios per backend and across backends, following digest section 9 "Test writer": the resource description lifecycle on catalogue-be (create, versions, revoke, delete, ownership), verification and signatures, quick and advanced search, the adapter and client backend operations, federated search across data spaces A, B and C including the slow and the inactive catalogue, the quality gate through the quality stub, schemas through the schema stub, and access per digest section 5.4. Tag each scenario with its story key (`@SIMPL-nnnnn`) or `@bp05b`/`@bp06` or `@openapi-only`, the decisions it depends on (`@Dnn`), `@rbac`, `@federation`, `@quality`, `@slow` where it waits on a timeout, and anything that must run serially.
- `features/ui/*.feature`: Gherkin for the UI stories and the UI parts of BP06, tagged `@ui @wip`, with the selectors of digest section 7. Browser steps come in a later step.
- `features/steps/*.py`: a shared step library with custom parse types for quoted values.
- `fixtures/`: resource descriptions (valid and broken, per resource type), the test signing keys of the digest, query bodies, anything the seeds need.
- `README.md`: contents, tags, environment variables, how to run against the factory build and against upstream.

## Rules

- HTTP only (plus stub control endpoints the digest defines), so the suite runs unchanged against any implementation.
- Test data per D80: unique ids per run and scenario; clean up what can be cleaned up.
- Check status codes, problem details and the payload fields the specs and digest name; nothing they do not say.
- **Baseline:** the factory build does not exist yet. Run the suite against the local upstream stack: start it with `{UPSTREAM}/start.sh` (or `docker compose up -d --wait` in that folder, see its README) and stop it with `{UPSTREAM}/stop.sh` at the end. It is a May 2026 version of the earlier repositories: one data space, no Keycloak, no stubs, no client backend, older than almost every story. Run what can run (`AUTH_MODE=none`, upstream URLs by environment variable, skip by tag what needs missing pieces) and classify each failure: a test-writer bug (fix it), an environment difference (missing piece), or a real difference on a decision or a newer story (keep it failing and report it). Never weaken a check to make upstream pass. Make sure every step is defined and the suite runs without errors (dry run).
- Save reports in `{CAT}/tests/reports/` (progress and JSON per target).
- Do not touch other containers or ports than the upstream catalogue stack. Ports 5432, 7474, 7687, 8081, 8084 and 4321 belong to it. Do not git commit.
- Plain English, short sentences, never use em-dashes or en-dashes. Roles, not names.

## Report back (under 400 words)

Scenario counts per feature and tag, the baseline result, the failures grouped by classification, the test-writer bugs you fixed, and any place where the digest was too vague to write a test.
