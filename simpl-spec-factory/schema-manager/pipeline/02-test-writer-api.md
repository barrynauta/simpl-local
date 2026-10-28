# Step 2: test writer (Schema Manager)

Given to a fresh general-purpose agent. Same method as the first candidate ([`../../pipeline/02-test-writer-api.md`](../../pipeline/02-test-writer-api.md)), adapted to the Schema Manager digest. Paths are placeholders: `{SM}` is this `schema-manager` folder, `{UPSTREAM}` the local upstream stack (`~/src/simpl-local/simpl-schema-manager`).

---

You are the **test writer** of a spec-driven software factory experiment. Write a black-box test suite for the Simpl-Open **Schema Manager** from the specs, before any implementation exists. A separate code writer is building the component at the same time and never sees your suite; you never see its build.

## Input (read-only)

`{SM}/specs/`, `{SM}/spec-digest.md` and `{SM}/findings.md`. Do not read any implementation source: not upstream (`{UPSTREAM}/repos`, `~/src/gitlab`, the web), not the generated build (`~/src/spec-factory-build*`), not the first candidate's tests. You may read `{UPSTREAM}/README.md` and `{UPSTREAM}/docs/` to operate the upstream stack, nothing else in it.

## What to write, in `{SM}/tests/`

- Python, behave 1.2.6 and requests. `features/environment.py` reads all configuration from environment variables with the names of D60 (`SM_BASE_URL`, `KEYCLOAK_URL`, `VOCABULARY_MANAGER_URL`, `VOCABULARY_NAMESPACE_BASE`, `MAIL_API_URL`, `WEBHOOK_RECEIVER_URL`, ...) plus `AUTH_MODE` (`oidc` with the D59 contract, or `none` for a stack without authentication) and `CLIENT_ID` (default `schema-tests`). Document every variable in its docstring.
- **Reference environment of the factory build** (built by the code writer from the same digest, not visible to you): API `http://localhost:8098/v1`, Keycloak `http://localhost:8185`, Vocabulary Manager stub (D60 contract) `http://localhost:8099`, Mailpit API `http://localhost:8027`. The service runs in Docker: your webhook receiver listens on the host (default port 8299, variable `WEBHOOK_LISTEN_PORT`) and the service reaches it as `http://host.docker.internal:8299` (`WEBHOOK_RECEIVER_URL`). Start the receiver in `before_all`, in a thread.
- `features/api/*.feature`: Gherkin scenarios for every in-scope API story, the API behaviour of the UI stories and the OpenAPI-only operations, following section 6 "Test writer" of the digest (validation rules, the slot rule as a state table, e-mail and webhook side effects including their absence). Tag each scenario with its story key (`@SIMPL-nnnnn`), the decisions it depends on (`@Dnn`), `@openapi-only` where no story exists, `@rbac` for role checks, `@mail`, `@webhook`, and `@search-serial` for the SEARCH tests that must run one after another (D61).
- `features/ui/*.feature`: Gherkin for the UI stories, tagged `@ui @wip`, using the `data-testid` values and texts of D45 to D57. Browser steps come in a later step.
- `features/steps/api_steps.py`: one shared step library, custom parse types for quoted values.
- `fixtures/`: small Turtle files (valid RESOURCE and SEARCH schemas with the D31 default properties, syntax errors, each structure and semantic error of the digest, references to stub vocabularies, a non-Turtle file).
- `README.md`: contents, tags, environment variables, how to run against the factory build and against upstream.

## Rules

- HTTP only (plus the webhook receiver, the stub seeding and the Mailpit API of D60), so the suite runs unchanged against any implementation.
- Every scenario creates its own data per D61 (unique names, own resourceType). Schemas cannot be deleted; clean up what can be (webhook subscriptions).
- Check status codes, Problem Details and the payload fields the specs and digest name; nothing they do not say.
- **Baseline:** the factory build does not exist yet. Run the suite against the local upstream stack: start it with `{UPSTREAM}/start.sh` and stop it with `{UPSTREAM}/stop.sh` at the end. It has no Keycloak (authentication bypassed), no stub, possibly no mail capture, and it is a May 2026 version, older than the enhancement stories. Run what can run (`AUTH_MODE=none`, skip with tags what needs missing pieces) and classify each failure: a test-writer bug (fix it), an environment difference (missing piece), or a real difference on a decision or a newer story (keep it failing and report it). Never weaken a check to make upstream pass. Also make sure every step is defined and the suite runs without errors (dry run).
- Save reports in `{SM}/tests/reports/` (progress and JSON per target).
- Do not touch other containers or ports than the upstream stack. Do not git commit.
- Plain English, short sentences, no em-dashes or en-dashes.

## Report back (under 400 words)

Scenario counts per feature and tag, the baseline result, the failures grouped by classification, the test-writer bugs you fixed, and any place where the digest was too vague to write a test.
