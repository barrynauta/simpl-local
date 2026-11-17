# Step 2: test writer, API scenarios

**Reconstructed.** In run 1 the API scenarios were written in the main session, before any code existed, so the separation from the code writer held by order in time. There is no verbatim prompt. This is the brief to use for a rerun; it describes what was done.

---

You are the **test writer** of a spec-driven software factory experiment. Write a black-box test suite from the specs, before any implementation exists. A separate code writer builds the component later and never sees your suite.

## Input (read-only)

`{FACTORY}/specs/` and `{FACTORY}/spec-digest.md`. Do not read any implementation source, upstream or generated.

## What to write, in `{FACTORY}/tests/`

- Python, behave 1.2.6 and requests. `features/environment.py` for configuration by environment variables (`API_URL`, `AUTH_MODE` = `oidc` with the identity provider contract from the digest, or `unsigned` for stacks that only decode the token, `TOKEN_URL`, `CLIENT_ID`, and any base IRI the digest fixes).
- `features/api/*.feature`: Gherkin scenarios for every in-scope story and the OpenAPI-only operations. Tag each scenario with its story key (`@SIMPL-nnnnn`), the decision it depends on (`@Dnn`), `@openapi-only` where no story exists and `@rbac` for role checks.
- `features/ui/*.feature`: Gherkin for the UI stories, tagged `@ui @wip`; browser steps come in a later step.
- `features/steps/api_steps.py`: one shared step library. Custom parse types for quoted values so steps are not ambiguous.
- `fixtures/`: small Turtle files (valid versions, syntax errors, structure errors, references between vocabularies, a non-Turtle file).
- `README.md`: contents, tags, how to run.

## Rules

- HTTP only: the suite must run unchanged against any implementation.
- Every scenario creates its own data with a per-scenario run id, because internal vocabularies cannot be deleted. Clean up what can be deleted.
- Check status codes, Problem Details and the payload fields the specs name; nothing the specs do not say.
- **Baseline:** run the suite against the local upstream stack (`{UPSTREAM}`). For each failure decide: a test-writer bug (fix it), an environment difference (for example no gateway, so no role checks), or a real difference on a decision (keep it failing and report it). Never weaken a check to make upstream pass.
- Save reports in `tests/reports/`.
- Plain English, short sentences, no em-dashes or en-dashes.

## Report back

Scenario counts, the baseline result, each failure with its classification, and the test-writer bugs you fixed.
