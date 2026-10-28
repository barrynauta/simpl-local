# Step 4: UI browser steps (Catalogue)

Given to a fresh general-purpose agent. Same method as the first candidate ([`../../pipeline/04-test-writer-ui.md`](../../pipeline/04-test-writer-ui.md)), adapted. `{CAT}` is this `catalogue` folder, `{UPSTREAM}` the local upstream stack (`~/src/simpl-local/simpl-catalogue`).

---

You are the **test writer** of a spec-driven software factory experiment. Your job: make the 26 UI scenarios in `{CAT}/tests/features/ui/*.feature` runnable with browser steps (Python, behave 1.2.6 + Playwright sync API, headless Chromium), then run them against two implementations of the Simpl-Open **Catalogue** and report.

## Black-box rule (most important)

The tests must check what the **specs and the digest** say, not what one build happens to do.
- You MAY read: `{CAT}/specs/`, `{CAT}/spec-digest.md`, `{CAT}/findings.md`, and everything under `{CAT}/tests/`. You may read `{UPSTREAM}/README.md` and `{UPSTREAM}/docs/` to operate the upstream stack.
- You MUST NOT read: `~/src/spec-factory-build-cat/`, any other `~/src/spec-factory-build*`, `~/src/gitlab/`, or the source under `{UPSTREAM}/repos/`. Do not grep them either.
- You MAY look at the rendered pages in the browser (DOM, accessibility tree, screenshots) and talk HTTP to the APIs: that is what a user sees.
- **Locators, in this order:** (1) accessible roles, labels and visible text taken from the story wording; where a story quotes a label, match it case-insensitive and whitespace tolerant. (2) Where no story fixes it, the texts and `data-testid` values that the digest defines (section 7 and the UI decisions): they are part of the shared decision list, not of one build. Tag such scenarios or steps with the decision, so a failure reads as "different decision". (3) Where neither fixes it, a tolerant pattern with a short comment naming the story. No CSS classes or ids.
- Never weaken an assertion to make one implementation pass. A deviation from a story or decision is a finding, not a test bug. Fix only genuine test-writer bugs. If you change any .feature text, list each change and why.

## Setup

- Work in `{CAT}/tests`. Existing venv `.venv`. Add `playwright` to `requirements.txt`, install it and `playwright install chromium`.
- Read `features/environment.py`, `features/support/*.py`, `features/steps/*.py` (including the existing `ui_steps.py`) first. Reuse the API Given steps and the alias mechanism for setup; do not duplicate them. Browser steps go in `features/steps/ui_steps.py`, in the style of the API steps. Browser lifecycle in environment.py, only for scenarios tagged @ui: the 459 API scenarios must stay as they are and keep passing.
- New configuration, documented in the environment.py docstring: the UI base URL (default `http://localhost:18104`, digest section 5.1), `UI_AUTH` = `oidc` (sign in on the identity provider's login page with the users and password of digest section 5.3) or `none` (the UI has no sign-in). Generic login-page locators.
- "The API fails" steps: fault injection with Playwright `page.route`, returning a Problem Details error for the matching request. Works against any implementation.
- Downloads: `page.expect_download`, compare the bytes with the API's content of that version.
- Remove `@wip` from runnable scenarios; keep `@ui`.

## Targets

1. Factory build (running now, compose project `simpl-catalogue-factory`; do not stop, restart or modify it): the default URLs of digest section 5.1, `UI_AUTH=oidc`.
2. Local upstream (May 2026 version of the earlier repositories, no Keycloak, UI at `http://localhost:4321`, catalogue backend at `http://localhost:8081`, adapter at `http://localhost:8084`, no client backend): start it with `{UPSTREAM}/start.sh`, run, and stop it with `{UPSTREAM}/stop.sh` at the end. Run with `UI_AUTH=none AUTH_MODE=none` and the upstream URLs. Expect sign-in, federation and client backend scenarios to fail or be skipped there (environment), and expect setup through the API to fail where upstream does not accept the digest's signed descriptions (see findings, step 2): classify, do not adapt the fixtures to upstream.

Run `behave features/ui` against both and save `reports/build1-ui-progress.txt`, `reports/build1-ui.json`, `reports/upstream-ui-progress.txt`, `reports/upstream-ui.json`. At the end, rerun `behave --tags=~@ui` against the build once to confirm 458 passed and 1 failed (the known D14 order deviation). Keep iterations bounded: if a step stays ambiguous after a few attempts, leave it failing with a clear message.

Update `tests/README.md` (UI running instructions, tags). Do not touch findings.md. Do not commit. Only the upstream catalogue stack may be started and stopped; ports 5432, 7474, 7687, 8081, 8084 and 4321 belong to it. Writing rule: no em-dashes and no en-dashes anywhere.

## Report back (under 400 words)

- pass counts per target (UI), and the API rerun count;
- per failing scenario: target, scenario, what the browser showed against what the story or decision says, and a classification: implementation deviation / different decision / environment / spec gap;
- test bugs you fixed, and any .feature text changes;
- story wordings too vague for a strict locator (spec findings).
