# Step 2: test writer (Contract Management)

Given to a fresh general-purpose agent. Same method as the earlier candidates ([`../../catalogue/pipeline/02-test-writer-api.md`](../../catalogue/pipeline/02-test-writer-api.md)), adapted to the Contract Management digest. `{CM}` is this `contract-management` folder, `{UPSTREAM}` the local upstream stack (`~/src/simpl-local/simpl-contract`).

---

You are the **test writer** of a spec-driven software factory experiment. Write a black-box test suite for Simpl-Open **Contract Management** (contract manager backend, contract UI backend, UI, at a Provider and a Consumer) from the specs, before any implementation exists. A separate code writer is building it at the same time and never sees your suite; you never see its build.

## Input (read-only)

`{CM}/specs/` and `{CM}/spec-digest.md`. Do not read any implementation source or other test suite: not upstream (`{UPSTREAM}/repos`, `~/src/gitlab`, the web), not the generated build (`~/src/spec-factory-build*`), not the other candidates' tests or digests, not any PSO test checkout. You may read `{UPSTREAM}/README.md` and `{UPSTREAM}/docs/` to operate the upstream stack, nothing else in it.

## What to write, in `{CM}/tests/`

- Python, behave 1.2.6, requests, and a Kafka client (`confluent-kafka` or `kafka-python`) for the event checks of D51. No crypto code is needed: the stub produces valid and broken credentials (D56). `features/environment.py` reads all configuration from environment variables, with the host URLs, API keys and realms of the digest (D67, D76, D77) as defaults, plus `AUTH_MODE` (`oidc`, or `none` for a stack without Keycloak) and switches to skip the stub, Kafka or the second party. Document every variable in its docstring.
- **Reference environment of the factory build** (built by the code writer from the same digest, not visible to you): the services, ports, realms, clients, users and keys of D67, D76 and D77, compose project `cm-factory`.
- `features/api/*.feature`: Gherkin scenarios following digest section 6 "Test writer": one per acceptance criterion of the in-scope stories, one per row of the D37 state machine tables, the added operations of Appendices A and B, the stub-driven failure cases (signatures, wallet, signer, D56 to D59), the events of Appendix D, expiry (D82) and access (D66 to D68). Tag each scenario with its story key (`@SIMPL-nnnnn`) or `@bp07` or `@openapi-only`, the decisions it depends on (`@Dnn`), `@rbac`, `@events`, `@two-party` where it needs both parties, `@slow` where it polls, and anything that must run serially.
- `features/ui/*.feature`: Gherkin for the UI stories, tagged `@ui @wip`, with the hooks of D72. Browser steps come in a later step.
- `features/steps/*.py`: a shared step library with custom parse types for quoted values, and a polling helper per D79.
- `fixtures/`: request bodies, expected problem types, anything the stub control calls need.
- `README.md`: contents, tags, environment variables, how to run against the factory build and against upstream.

## Rules

- HTTP and Kafka only (plus the stub control endpoints of Appendix C.6), so the suite runs unchanged against any implementation.
- Test data per D80 and D81: own asset and negotiation ids per scenario; nothing can be deleted, so never assume an empty database.
- Check status codes, problem details, states and the payload fields the specs and digest name; nothing they do not say.
- **Baseline:** the factory build does not exist yet. Run the suite against the local upstream stack: start it with `{UPSTREAM}/start.sh` (see its README) and stop it with `{UPSTREAM}/stop.sh` at the end. It is a June 2026 version of the earlier repositories: one party (Provider mode), no Keycloak, no signer, wallet or connector, no UI backend, older than most stories. Run what can run (upstream URLs and keys by environment variable, skip by tag what needs missing pieces) and classify each failure: a test-writer bug (fix it), an environment difference (missing piece), or a real difference on a decision or a newer story (keep it failing and report it). Never weaken a check to make upstream pass. Make sure every step is defined and the suite runs without errors (dry run).
- Save reports in `{CM}/tests/reports/` (progress and JSON per target).
- Do not touch other containers or ports than the upstream contract stack. Ports 4323, 5433, 8086, 9002 and 9095 belong to it. Do not git commit.
- Plain English, short sentences, never use em-dashes or en-dashes. Roles, not names.

## Report back (under 400 words)

Scenario counts per feature and tag, the baseline result, the failures grouped by classification, the test-writer bugs you fixed, and any place where the digest was too vague to write a test.
